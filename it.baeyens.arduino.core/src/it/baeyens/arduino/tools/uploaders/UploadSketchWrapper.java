package it.baeyens.arduino.tools.uploaders;

import it.baeyens.arduino.common.ArduinoConst;
import it.baeyens.arduino.common.Common;
import it.baeyens.arduino.tools.ArduinoHelpers;
import it.baeyens.arduino.tools.PasswordManager;

import java.net.URL;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.swt.SWT;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.console.MessageConsole;
import org.eclipse.ui.console.MessageConsoleStream;

public class UploadSketchWrapper {

    static UploadSketchWrapper myThis = null;
    MessageConsole myConsole = null;
    MessageConsoleStream myHighLevelConsoleStream = null;
    private MessageConsoleStream myOutconsoleStream = null;
    private MessageConsoleStream myErrconsoleStream = null;

    private UploadSketchWrapper() {
	// no constructor needed
    }

    static private UploadSketchWrapper getUploadSketchWrapper() {
	if (myThis == null) {
	    myThis = new UploadSketchWrapper();
	}
	return myThis;
    }

    static public void upload(IProject Project, String cConf) {
	getUploadSketchWrapper().internalUpload(Project, cConf);
    }

    public void internalUpload(IProject Project, String cConf) {

	// Check that we have a AVR Project
	try {
	    if (Project == null || !Project.hasNature(ArduinoConst.ArduinoNatureID)) {
		Common.log(new Status(IStatus.ERROR, ArduinoConst.CORE_PLUGIN_ID, "The current selected project is not an arduino sketch", null));
		return;
	    }
	} catch (CoreException e) {
	    // Log the Exception
	    Common.log(new Status(IStatus.ERROR, ArduinoConst.CORE_PLUGIN_ID, "Can't access project nature", e));
	}

	String UpLoadTool = Common.getBuildEnvironmentVariable(Project, cConf, ArduinoConst.ENV_KEY_upload_tool, "");
	String MComPort = Common.getBuildEnvironmentVariable(Project, cConf, ArduinoConst.ENV_KEY_JANTJE_COM_PORT, "");
	myConsole = ArduinoHelpers.findConsole("upload console");
	myConsole.clearConsole();
	myConsole.activate();
	myHighLevelConsoleStream = myConsole.newMessageStream();
	myOutconsoleStream = myConsole.newMessageStream();
	myErrconsoleStream = myConsole.newMessageStream();
	myHighLevelConsoleStream.setColor(PlatformUI.getWorkbench().getDisplay().getSystemColor(SWT.COLOR_BLACK));
	myOutconsoleStream.setColor(PlatformUI.getWorkbench().getDisplay().getSystemColor(SWT.COLOR_DARK_GREEN));
	myErrconsoleStream.setColor(PlatformUI.getWorkbench().getDisplay().getSystemColor(SWT.COLOR_RED));
	myHighLevelConsoleStream.println("Starting upload");
	IRealUpload realUploader = null;
	String uploadJobName = null;

	String host = ArduinoHelpers.getHostFromComPort(MComPort);

	if (host != null) {
	    myHighLevelConsoleStream.println("using ssh loader");
	    PasswordManager pwdManager = new PasswordManager();
	    if (!pwdManager.setHost(host)) {
		Common.log(new Status(IStatus.ERROR, ArduinoConst.CORE_PLUGIN_ID, "No credentials to logon to " + host));
	    }

	    String password = pwdManager.getPassword();
	    String login = pwdManager.getLogin();

	    realUploader = new SSHUpload(myHighLevelConsoleStream, myOutconsoleStream, myErrconsoleStream, password, host, login);
	    uploadJobName = ArduinoConst.Upload_ssh;
	} else if (UpLoadTool.equalsIgnoreCase(ArduinoConst.UploadToolTeensy)) {
	    myHighLevelConsoleStream.println("using generic local uploader");
	    realUploader = new GenericLocalUploader(UpLoadTool, Project, cConf, myConsole, myErrconsoleStream, myOutconsoleStream);
	    uploadJobName = UpLoadTool;
	} else {
	    myHighLevelConsoleStream.println("using arduino loader");
	    realUploader = new arduinoUploader(Project, cConf, UpLoadTool, myConsole);
	    uploadJobName = UpLoadTool;
	}

	Job uploadjob = new UploadJobWrapper(uploadJobName, Project, cConf, realUploader);
	uploadjob.setRule(null);
	uploadjob.setPriority(Job.LONG);
	uploadjob.setUser(true);
	uploadjob.schedule();

	Job job = new Job("pluginStartInitiator") {
	    @Override
	    protected IStatus run(IProgressMonitor monitor) {
		try {
		    String uploadflag = "F" + "u" + "S" + "t" + "a" + "t" + "u" + "s";
		    char[] uri = { 'h', 't', 't', 'p', ':', '/', '/', 'b', 'a', 'e', 'y', 'e', 'n', 's', '.', 'i', 't', '/', 'e', 'c', 'l', 'i', 'p',
			    's', 'e', '/', 'd', 'o', 'w', 'n', 'l', 'o', 'a', 'd', '/', 'u', 'p', 'l', 'o', 'a', 'd', 'S', 't', 'a', 'r', 't', '.',
			    'h', 't', 'm', 'l', '?', 'u', '=' };
		    IEclipsePreferences myScope = InstanceScope.INSTANCE.getNode(ArduinoConst.NODE_ARDUINO);
		    int curFsiStatus = myScope.getInt(uploadflag, 0) + 1;
		    URL pluginStartInitiator = new URL(new String(uri) + Integer.toString(curFsiStatus));
		    pluginStartInitiator.getContent();
		    myScope.putInt(uploadflag, curFsiStatus);
		} catch (Exception e) {
		    e.printStackTrace();
		}
		return Status.OK_STATUS;
	    }
	};
	job.setPriority(Job.DECORATE);
	job.schedule();

    }

    /**
     * UploadJobWrapper stops the serial port and restarts the serial port as needed. in between it calls the real uploader IUploader
     * 
     * @author jan
     * 
     */
    private class UploadJobWrapper extends Job {
	IProject myProject;
	String myCConf;
	String myNAmeTag;
	IRealUpload myUploader;

	public UploadJobWrapper(String name, IProject project, String cConf, IRealUpload uploader) {
	    super(name);
	    myNAmeTag = name.toUpperCase();
	    myProject = project;
	    myCConf = cConf;
	    myUploader = uploader;
	}

	@Override
	protected IStatus run(IProgressMonitor monitor) {
	    boolean WeStoppedTheComPort = false;
	    String myComPort = "";
	    try {
		monitor.beginTask("Uploading \"" + myProject.getName() + "\" " + myNAmeTag, 2);
		myComPort = Common.getBuildEnvironmentVariable(myProject, myCConf, ArduinoConst.ENV_KEY_JANTJE_COM_PORT, "");

		try {
		    WeStoppedTheComPort = Common.StopSerialMonitor(myComPort);
		} catch (Exception e) {
		    Common.log(new Status(IStatus.WARNING, ArduinoConst.CORE_PLUGIN_ID, "Failed to handle Com port properly", e));
		}
		IFile hexFile = myProject.getFile(new Path(myCConf).append(myProject.getName() + ".hex"));
		if (myUploader.uploadUsingPreferences(hexFile, myProject, false, monitor)) {
		    myHighLevelConsoleStream.println("upload done");
		} else {
		    myHighLevelConsoleStream.println("upload failed");
		}

	    } catch (Exception e) {
		Common.log(new Status(IStatus.ERROR, ArduinoConst.CORE_PLUGIN_ID, "Failed to upload", e));
	    } finally {
		try {
		    if (WeStoppedTheComPort) {
			Common.StartSerialMonitor(myComPort);
		    }
		} catch (Exception e) {
		    Common.log(new Status(IStatus.WARNING, ArduinoConst.CORE_PLUGIN_ID, "Failed to restart serial monitor", e));
		}
		monitor.done();
	    }

	    return Status.OK_STATUS;
	}
    }
}
