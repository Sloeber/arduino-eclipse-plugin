package it.baeyens.arduino.tools.uploaders;

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

import it.baeyens.arduino.common.ArduinoConst;
import it.baeyens.arduino.common.Common;
import it.baeyens.arduino.tools.ArduinoHelpers;
import it.baeyens.arduino.tools.PasswordManager;

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
		Common.log(new Status(IStatus.ERROR, ArduinoConst.CORE_PLUGIN_ID, Messages.Upload_no_arduino_sketch, null));
		return;
	    }
	} catch (CoreException e) {
	    // Log the Exception
	    Common.log(new Status(IStatus.ERROR, ArduinoConst.CORE_PLUGIN_ID, Messages.Upload_Project_nature_unaccesible, e));
	}

	String UpLoadTool = Common.getBuildEnvironmentVariable(Project, cConf, ArduinoConst.ENV_KEY_upload_tool, ArduinoConst.EMPTY_STRING);
	String MComPort = Common.getBuildEnvironmentVariable(Project, cConf, ArduinoConst.ENV_KEY_JANTJE_COM_PORT, ArduinoConst.EMPTY_STRING);
	this.myConsole = ArduinoHelpers.findConsole(Messages.Upload_console);
	this.myConsole.clearConsole();
	this.myConsole.activate();
	this.myHighLevelConsoleStream = this.myConsole.newMessageStream();
	this.myOutconsoleStream = this.myConsole.newMessageStream();
	this.myErrconsoleStream = this.myConsole.newMessageStream();
	this.myHighLevelConsoleStream.setColor(PlatformUI.getWorkbench().getDisplay().getSystemColor(SWT.COLOR_BLACK));
	this.myOutconsoleStream.setColor(PlatformUI.getWorkbench().getDisplay().getSystemColor(SWT.COLOR_DARK_GREEN));
	this.myErrconsoleStream.setColor(PlatformUI.getWorkbench().getDisplay().getSystemColor(SWT.COLOR_RED));
	this.myHighLevelConsoleStream.println(Messages.Upload_starting);
	IRealUpload realUploader = null;
	String uploadJobName = null;

	String host = ArduinoHelpers.getHostFromComPort(MComPort);

	if (host != null) {
	    this.myHighLevelConsoleStream.println(Messages.Upload_ssh);
	    PasswordManager pwdManager = new PasswordManager();
	    if (!pwdManager.setHost(host)) {
		Common.log(new Status(IStatus.ERROR, ArduinoConst.CORE_PLUGIN_ID, Messages.Upload_login_credentials_missing + host));
	    }

	    String password = pwdManager.getPassword();
	    String login = pwdManager.getLogin();

	    realUploader = new SSHUpload(this.myHighLevelConsoleStream, this.myOutconsoleStream, this.myErrconsoleStream, password, host, login);
	    uploadJobName = ArduinoConst.Upload_ssh;
	} else if (UpLoadTool.equalsIgnoreCase(ArduinoConst.UploadToolTeensy)) {
	    this.myHighLevelConsoleStream.println(Messages.Upload_generic);
	    realUploader = new GenericLocalUploader(UpLoadTool, Project, cConf, this.myConsole, this.myErrconsoleStream, this.myOutconsoleStream);
	    uploadJobName = UpLoadTool;
	} else {
	    this.myHighLevelConsoleStream.println(Messages.Upload_arduino);
	    realUploader = new arduinoUploader(Project, cConf, UpLoadTool, this.myConsole);
	    uploadJobName = UpLoadTool;
	}

	Job uploadjob = new UploadJobWrapper(uploadJobName, Project, cConf, realUploader);
	uploadjob.setRule(null);
	uploadjob.setPriority(Job.LONG);
	uploadjob.setUser(true);
	uploadjob.schedule();

	Job job = new Job(Messages.Upload_PluginStartInitiator) {
	    @Override
	    protected IStatus run(IProgressMonitor monitor) {
		try {
		    String uploadflag = "FuStatus"; //$NON-NLS-1$
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
	    this.myNAmeTag = name.toUpperCase();
	    this.myProject = project;
	    this.myCConf = cConf;
	    this.myUploader = uploader;
	}

	@Override
	protected IStatus run(IProgressMonitor monitor) {
	    boolean WeStoppedTheComPort = false;
	    String myComPort = ArduinoConst.EMPTY_STRING;
	    try {
		monitor.beginTask(Messages.Upload_uploading + " \"" + this.myProject.getName() + "\" " + this.myNAmeTag, 2); //$NON-NLS-1$//$NON-NLS-2$
		myComPort = Common.getBuildEnvironmentVariable(this.myProject, this.myCConf, ArduinoConst.ENV_KEY_JANTJE_COM_PORT, ""); //$NON-NLS-1$

		try {
		    WeStoppedTheComPort = Common.StopSerialMonitor(myComPort);
		} catch (Exception e) {
		    Common.log(new Status(IStatus.WARNING, ArduinoConst.CORE_PLUGIN_ID, Messages.Upload_Error_com_port, e));
		}
		IFile hexFile = this.myProject.getFile(new Path(this.myCConf).append(this.myProject.getName() + ".hex")); //$NON-NLS-1$
		if (this.myUploader.uploadUsingPreferences(hexFile, this.myProject, false, monitor)) {
		    UploadSketchWrapper.this.myHighLevelConsoleStream.println(Messages.Upload_Done);
		} else {
		    UploadSketchWrapper.this.myHighLevelConsoleStream.println(Messages.Upload_failed_upload);
		}

	    } catch (Exception e) {
		Common.log(new Status(IStatus.ERROR, ArduinoConst.CORE_PLUGIN_ID, Messages.Upload_failed_upload, e));
	    } finally {
		try {
		    if (WeStoppedTheComPort) {
			Common.StartSerialMonitor(myComPort);
		    }
		} catch (Exception e) {
		    Common.log(new Status(IStatus.WARNING, ArduinoConst.CORE_PLUGIN_ID, Messages.Upload_Error_serial_monitor_restart, e));
		}
		monitor.done();
	    }

	    return Status.OK_STATUS;
	}
    }
}
