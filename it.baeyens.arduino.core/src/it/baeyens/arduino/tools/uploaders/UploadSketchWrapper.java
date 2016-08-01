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

import it.baeyens.arduino.common.Common;
import it.baeyens.arduino.common.Const;
import it.baeyens.arduino.tools.Helpers;

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
	    if (Project == null || !Project.hasNature(Const.ARDUINO_NATURE_ID)) {
		Common.log(new Status(IStatus.ERROR, Const.CORE_PLUGIN_ID, Messages.Upload_no_arduino_sketch, null));
		return;
	    }
	} catch (CoreException e) {
	    // Log the Exception
	    Common.log(new Status(IStatus.ERROR, Const.CORE_PLUGIN_ID, Messages.Upload_Project_nature_unaccesible, e));
	}

	String UpLoadTool = Common.getBuildEnvironmentVariable(Project, cConf,
		Const.get_ENV_KEY_TOOL(Const.ACTION_UPLOAD), Const.EMPTY_STRING);
	String MComPort = Common.getBuildEnvironmentVariable(Project, cConf, Const.ENV_KEY_JANTJE_COM_PORT,
		Const.EMPTY_STRING);
	String uploadClass = Common.getBuildEnvironmentVariable(Project, cConf,
		Const.get_ENV_KEY_TOOL(Const.UPLOAD_CLASS), Const.EMPTY_STRING);

	this.myConsole = Helpers.findConsole(Messages.Upload_console);
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

	String host = Helpers.getHostFromComPort(MComPort);

	if (host != null) {
	    if (Const.UPLOAD_CLASS_DEFAULT.equals(uploadClass)) {
		this.myHighLevelConsoleStream.println(Messages.Upload_arduino);
		realUploader = new arduinoUploader(Project, cConf, UpLoadTool, this.myConsole);
		uploadJobName = UpLoadTool;
	    } else {
		this.myHighLevelConsoleStream.println(Messages.Upload_ssh);

		realUploader = new SSHUpload(Project, UpLoadTool, this.myHighLevelConsoleStream,
			this.myOutconsoleStream, this.myErrconsoleStream, host);
		uploadJobName = Const.UPLOAD_SSH;
	    }
	} else if (UpLoadTool.equalsIgnoreCase(Const.UPLOAD_TOOL_TEENSY)) {
	    this.myHighLevelConsoleStream.println(Messages.Upload_generic);
	    realUploader = new GenericLocalUploader(UpLoadTool, Project, cConf, this.myConsole, this.myErrconsoleStream,
		    this.myOutconsoleStream);
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
		    char[] uri = { 'h', 't', 't', 'p', ':', '/', '/', 'b', 'a', 'e', 'y', 'e', 'n', 's', '.', 'i', 't',
			    '/', 'e', 'c', 'l', 'i', 'p', 's', 'e', '/', 'd', 'o', 'w', 'n', 'l', 'o', 'a', 'd', '/',
			    'u', 'p', 'l', 'o', 'a', 'd', 'S', 't', 'a', 'r', 't', '.', 'h', 't', 'm', 'l', '?', 'u',
			    '=' };
		    IEclipsePreferences myScope = InstanceScope.INSTANCE.getNode(Const.NODE_ARDUINO);
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
     * UploadJobWrapper stops the serial port and restarts the serial port as
     * needed. in between it calls the real uploader IUploader
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
	    String comPort = Const.EMPTY_STRING;
	    try {
		String message = Messages.Upload_uploading;
		message += " \"" + this.myProject.getName() + "\" "; //$NON-NLS-1$//$NON-NLS-2$
		message += this.myNAmeTag + Const.SPACE;
		// message+= this.+Const.SPACE;
		monitor.beginTask(message, 2);
		comPort = Common.getBuildEnvironmentVariable(this.myProject, this.myCConf,
			Const.ENV_KEY_JANTJE_COM_PORT, Const.EMPTY_STRING);
		String programmer = Common.getBuildEnvironmentVariable(this.myProject, this.myCConf,
			Const.get_Jantje_KEY_PROTOCOL(Const.ACTION_UPLOAD), Const.DEFAULT);

		try {
		    WeStoppedTheComPort = Common.StopSerialMonitor(comPort);
		} catch (Exception e) {
		    Common.log(new Status(IStatus.WARNING, Const.CORE_PLUGIN_ID, Messages.Upload_Error_com_port, e));
		}
		IFile hexFile = this.myProject
			.getFile(new Path(this.myCConf).append(this.myProject.getName() + ".hex")); //$NON-NLS-1$
		if (this.myUploader.uploadUsingPreferences(hexFile, !programmer.equalsIgnoreCase(Const.DEFAULT),
			monitor)) {
		    UploadSketchWrapper.this.myHighLevelConsoleStream.println(Messages.Upload_Done);
		} else {
		    UploadSketchWrapper.this.myHighLevelConsoleStream.println(Messages.Upload_failed_upload);
		}

	    } catch (Exception e) {
		Common.log(new Status(IStatus.ERROR, Const.CORE_PLUGIN_ID, Messages.Upload_failed_upload, e));
	    } finally {
		try {
		    if (WeStoppedTheComPort) {
			Common.StartSerialMonitor(comPort);
		    }
		} catch (Exception e) {
		    Common.log(new Status(IStatus.WARNING, Const.CORE_PLUGIN_ID,
			    Messages.Upload_Error_serial_monitor_restart, e));
		}
		monitor.done();
	    }

	    return Status.OK_STATUS;
	}
    }
}
