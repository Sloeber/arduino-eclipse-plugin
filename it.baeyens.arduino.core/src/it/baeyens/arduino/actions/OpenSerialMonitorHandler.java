package it.baeyens.arduino.actions;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IProject;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;

import it.baeyens.arduino.common.Common;
import it.baeyens.arduino.common.Const;
import it.baeyens.arduino.common.IndexHelper;
import it.baeyens.arduino.common.InstancePreferences;
import it.baeyens.arduino.listeners.ProjectExplorerListener;

/**
 * This is a handler to connect the plugin.xml to the code for opening the
 * serial monitor
 * 
 * 
 * The code looks for all selected projects for the com port and the baudrate
 * and connects if they both are found
 * 
 * @author jan
 * 
 */
public class OpenSerialMonitorHandler extends AbstractHandler {

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
	try {

	    PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage()
		    .showView("it.baeyens.arduino.monitor.views.SerialMonitor"); //$NON-NLS-1$
	    // find all projects
	    IProject SelectedProjects[] = ProjectExplorerListener.getSelectedProjects();
	    // if there are project selected and the autoConnectScope feature is
	    // on
	    if ((SelectedProjects.length > 0) && (InstancePreferences.getOpenSerialWithMonitor() == true)) {
		for (IProject curproject : SelectedProjects) {
		    int baud = getBaudRate(curproject);
		    if (baud > 0) {
			String comPort = Common.getBuildEnvironmentVariable(curproject, Const.ENV_KEY_JANTJE_COM_PORT,
				Const.EMPTY_STRING);
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
     * given a project look in the source code for the line of code that sets
     * the baud rate on the board Serial.begin([baudRate]);
     * 
     * 
     * 
     * return the integer value of [baudrate] or in case of error a negative
     * value
     * 
     * @param iProject
     * @return
     */
    private static int getBaudRate(IProject iProject) {
	String parentFunc = "setup"; //$NON-NLS-1$
	String childFunc = "Serial.begin"; //$NON-NLS-1$
	String baudRate = IndexHelper.findParameterInFunction(iProject, parentFunc, childFunc, null);
	if (baudRate == null) {
	    return -1;
	}
	return Integer.parseInt(baudRate);

    }

}
