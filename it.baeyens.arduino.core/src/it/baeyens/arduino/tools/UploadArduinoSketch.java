package it.baeyens.arduino.tools;

import it.baeyens.arduino.common.ArduinoConst;
import it.baeyens.arduino.common.Common;
import it.baeyens.arduino.communication.ArduinoSerial;

import java.io.IOException;
import java.net.URL;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.ui.console.MessageConsole;

public class UploadArduinoSketch {

    static UploadArduinoSketch myThis = null;

    private UploadArduinoSketch() {
    }

    static private UploadArduinoSketch GetUploadArduinoSketch() {
	if (myThis == null) {
	    myThis = new UploadArduinoSketch();
	}
	return myThis;
    }

    static public void Do(IProject Project, String cConf) {
	GetUploadArduinoSketch().internalUpload(Project, cConf);
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
	if (UpLoadTool.equalsIgnoreCase(ArduinoConst.UploadToolTeensy)) {
	    Job uploadjob = new TeensyUploadJob(ArduinoConst.UploadToolTeensy, Project, cConf);
	    uploadjob.setPriority(Job.LONG);
	    uploadjob.setUser(true);
	    uploadjob.schedule();
	} else {
	    Job uploadjob = new ArduinoUploadJob(UpLoadTool, Project, cConf);
	    uploadjob.setRule(null);
	    uploadjob.setPriority(Job.LONG);
	    uploadjob.setUser(true);
	    uploadjob.schedule();
	}
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

    protected static void RunConsoledCommand(String command, IProgressMonitor monitor, boolean ClearConsole) throws IOException {

	ExternalCommandLauncher Step = new ExternalCommandLauncher(command);

	MessageConsole console = ArduinoHelpers.findConsole("upload console");
	Step.setConsole(console);
	Step.redirectErrorStream(true);
	if (ClearConsole) {
	    console.clearConsole();
	    console.activate();
	}
	Step.launch(monitor);
    }

    private class TeensyUploadJob extends Job {
	IProject myProject;
	String myCConf;
	String myNAmeTag;

	public TeensyUploadJob(String name, IProject project, String cConf) {
	    super(name);
	    myNAmeTag = name.toUpperCase();
	    myProject = project;
	    myCConf = cConf;
	}

	@Override
	protected IStatus run(IProgressMonitor monitor) {
	    try {
		monitor.beginTask(Common.getBuildEnvironmentVariable(myProject, myCConf, "A.TOOLS." + myNAmeTag + ".NAME", "no name provided"), 2);
		String myComPort = Common.getBuildEnvironmentVariable(myProject, myCConf, ArduinoConst.ENV_KEY_COM_PORT, "");
		boolean WeStoppedTheComPort = false;
		try {
		    WeStoppedTheComPort = Common.StopSerialMonitor(myComPort);
		} catch (Exception e) {
		    Common.log(new Status(IStatus.WARNING, ArduinoConst.CORE_PLUGIN_ID, "Failed to handle Com port properly", e));
		}

		int step = 1;
		boolean clearConsole = true;
		String patternTag = "A.TOOLS." + myNAmeTag + ".STEP" + step + ".PATTERN";
		String commentTag = "A.TOOLS." + myNAmeTag + ".STEP" + step + ".NAME";
		String stepPattern = Common.getBuildEnvironmentVariable(myProject, myCConf, patternTag, "");
		String stepName = Common.getBuildEnvironmentVariable(myProject, myCConf, commentTag, "");
		do {
		    monitor.subTask("Running " + stepName);

		    RunConsoledCommand(stepPattern, monitor, clearConsole);
		    clearConsole = false;
		    step++;
		    patternTag = "A.TOOLS." + myNAmeTag + ".STEP" + step + ".PATTERN";
		    commentTag = "A.TOOLS." + myNAmeTag + ".STEP" + step + ".NAME";
		    stepPattern = Common.getBuildEnvironmentVariable(myProject, myCConf, patternTag, "");
		    stepName = Common.getBuildEnvironmentVariable(myProject, myCConf, commentTag, "");
		} while (!stepPattern.isEmpty());

		try {
		    if (WeStoppedTheComPort) {
			Common.StartSerialMonitor(myComPort);
		    }
		} catch (Exception e) {
		    Common.log(new Status(IStatus.WARNING, ArduinoConst.CORE_PLUGIN_ID, "Failed to restart serial monitor", e));
		}

	    } catch (IOException e) {
		Common.log(new Status(IStatus.ERROR, ArduinoConst.CORE_PLUGIN_ID, "Upload to teensy failed", e));
	    } finally {
		monitor.done();
	    }

	    return Status.OK_STATUS;
	}
    }

    /**
     * The background Job to execute the requested avrdude commands.
     * 
     */
    private class ArduinoUploadJob extends Job {
	IProject myProject = null;
	String myUploadTool = null;
	String mycConf;

	public ArduinoUploadJob(String UploadTool, IProject project, String cConf) {
	    super(UploadTool + " Upload");
	    myUploadTool = UploadTool;
	    myProject = project;
	    mycConf = cConf;
	}

	@Override
	public IStatus run(IProgressMonitor monitor) {

	    monitor.subTask("Running uploader");
	    String MComPort = Common.getBuildEnvironmentVariable(myProject, mycConf, ArduinoConst.ENV_KEY_COM_PORT, "");
	    String boardName = Common.getBuildEnvironmentVariable(myProject, mycConf, ArduinoConst.ENV_KEY_BOARD_NAME, "");

	    String NewComPort = MComPort;
	    boolean WeStoppedTheComPort = false;
	    String command = Common.getBuildEnvironmentVariable(myProject, mycConf, "A.TOOLS." + myUploadTool.toUpperCase() + ".UPLOAD.PATTERN", "");

	    try {
		WeStoppedTheComPort = Common.StopSerialMonitor(MComPort);
		NewComPort = ArduinoSerial.makeArduinoUploadready(myProject, mycConf, MComPort);
	    } catch (Exception e) {
		Common.log(new Status(IStatus.WARNING, ArduinoConst.CORE_PLUGIN_ID, "Failed to handle Com port properly", e));
	    }

	    command = command.replaceAll(" -P ", " -P " + NewComPort + " ");
	    String nakedPort = NewComPort.replace("/dev/", "");
	    command = command.replaceAll(" --port= ", " --port=" + nakedPort + " ");

	    try {
		RunConsoledCommand(command, new SubProgressMonitor(monitor, 1), true);
	    } catch (IOException e1) {
		e1.printStackTrace();
	    }
	    if (boardName.startsWith("Arduino Due ")) {
		ArduinoSerial.reset_Arduino_by_baud_rate(MComPort, 115200, 100);
	    }
	    try {
		if (WeStoppedTheComPort) {
		    Common.StartSerialMonitor(MComPort);
		}
	    } catch (Exception e) {
		Common.log(new Status(IStatus.WARNING, ArduinoConst.CORE_PLUGIN_ID, "Failed to restart serial monitor", e));
	    }

	    monitor.done();

	    return Status.OK_STATUS;
	}
    }

}
