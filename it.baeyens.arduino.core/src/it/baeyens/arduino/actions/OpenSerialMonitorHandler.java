package it.baeyens.arduino.actions;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.eclipse.cdt.core.CCorePlugin;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPFunction;
import org.eclipse.cdt.core.index.IIndex;
import org.eclipse.cdt.core.index.IIndexBinding;
import org.eclipse.cdt.core.index.IIndexName;
import org.eclipse.cdt.core.index.IndexFilter;
import org.eclipse.cdt.core.model.CoreModel;
import org.eclipse.cdt.core.model.ICProject;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;

import it.baeyens.arduino.common.Common;
import it.baeyens.arduino.common.Const;
import it.baeyens.arduino.common.InstancePreferences;
import it.baeyens.arduino.listeners.ProjectExplorerListener;

/**
 * This is a handler to connect the plugin.xml to the code for opening the serial monitor
 * 
 * 
 * The code looks for all selected projects for the com port and the baudrate and connects if they both are found
 * 
 * @author jan
 * 
 */
public class OpenSerialMonitorHandler extends AbstractHandler {

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
	try {

	    PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().showView("it.baeyens.arduino.monitor.views.SerialMonitor"); //$NON-NLS-1$
	    // find all projects
	    IProject SelectedProjects[] = ProjectExplorerListener.getSelectedProjects();
	    // if there are project selected and the autoConnectScope feature is
	    // on
	    if ((SelectedProjects.length > 0) && (InstancePreferences.getOpenSerialWithMonitor() == true)) {
		for (IProject curproject : SelectedProjects) {
		    int baud = getBaudRate(curproject);
		    if (baud > 0) {
			String comPort = Common.getBuildEnvironmentVariable(curproject, Const.ENV_KEY_JANTJE_COM_PORT, Const.EMPTY_STRING);
			if (!comPort.isEmpty()) {
			    it.baeyens.arduino.monitor.SerialConnection.add(comPort, baud);
			}
		    }
		}
	    }
	} catch (PartInitException e) {
	    e.printStackTrace();
	}
	return null;
    }

    /**
     * given a project look in the source code for the line of code that sets the baud rate on the board Serial.begin([baudRate]);
     * 
     * 
     * 
     * return the integer value of [baudrate] or in case of error a negative value
     * 
     * @param iProject
     * @return
     */
    private static int getBaudRate(IProject iProject) {
	String setupFunctionName = "setup"; //$NON-NLS-1$
	String serialVariable = "Serial.begin"; //$NON-NLS-1$

	ICProject curProject = CoreModel.getDefault().getCModel().getCProject(iProject.getName());

	IIndex index = null;
	try {
	    index = CCorePlugin.getIndexManager().getIndex(curProject);
	    index.acquireReadLock();
	    // find bindings for name
	    IIndexBinding[] bindings = index.findBindings(setupFunctionName.toCharArray(), IndexFilter.ALL_DECLARED, new NullProgressMonitor());
	    ICPPFunction setupFunc = null;
	    for (IIndexBinding curbinding : bindings) {
		if (curbinding instanceof ICPPFunction) {
		    setupFunc = (ICPPFunction) curbinding;
		}

	    }

	    if (setupFunc == null) {
		return -2;// that on found binding must be a function
	    }

	    IIndexName[] names = index.findNames(setupFunc, org.eclipse.cdt.core.index.IIndex.FIND_DEFINITIONS);
	    if (names.length != 1) {
		return -3;
	    }

	    String SetupFileName = names[0].getFileLocation().getFileName();
	    String SetupFileContent = FileUtils.readFileToString(new File(SetupFileName));
	    int serialBeginStart = SetupFileContent.indexOf(serialVariable);
	    if (serialBeginStart != -1) {
		int serialBeginStartbraket = SetupFileContent.indexOf("(", serialBeginStart); //$NON-NLS-1$
		if (serialBeginStartbraket != -1) {
		    int serialBeginCloseBraket = SetupFileContent.indexOf(")", serialBeginStartbraket); //$NON-NLS-1$
		    if (serialBeginCloseBraket != -1) {
			String baudrate = SetupFileContent.substring(serialBeginStartbraket + 1, serialBeginCloseBraket).trim();
			return Integer.parseInt(baudrate);
		    }
		}
	    }

	} catch (CoreException | InterruptedException | IOException e) {
	    e.printStackTrace();
	} finally {
	    if (index != null) {
		index.releaseReadLock();
	    }
	}

	return -4;
    }

}
