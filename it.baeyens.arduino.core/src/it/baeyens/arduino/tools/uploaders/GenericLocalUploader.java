package it.baeyens.arduino.tools.uploaders;

import it.baeyens.arduino.common.Common;
import it.baeyens.arduino.tools.ExternalCommandLauncher;

import java.io.IOException;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.ui.console.MessageConsole;
import org.eclipse.ui.console.MessageConsoleStream;

public class GenericLocalUploader implements IRealUpload {

    private String myNAmeTag;
    private IProject myProject;
    private String myCConf;
    private MessageConsole myConsole;
    private MessageConsoleStream myErrconsole;
    private MessageConsoleStream myOutconsole;

    GenericLocalUploader(String NAmeTag, IProject Project, String CConf, MessageConsole Console, MessageConsoleStream Errconsole,
	    MessageConsoleStream Outconsole) {
	myNAmeTag = NAmeTag.toUpperCase();
	myProject = Project;
	myCConf = CConf;

	myConsole = Console;
	myErrconsole = Errconsole;
	myOutconsole = Outconsole;
    }

    protected static void RunConsoledCommand(MessageConsole console, String command, IProgressMonitor monitor) throws IOException {

	ExternalCommandLauncher Step = new ExternalCommandLauncher(command);

	Step.setConsole(console);
	Step.redirectErrorStream(true);
	Step.launch(monitor);
    }

    @Override
    public boolean uploadUsingPreferences(IFile hexFile, IProject project, boolean usingProgrammer, IProgressMonitor monitor) {
	int step = 1;
	String patternTag = "A.TOOLS." + myNAmeTag + ".STEP" + step + ".PATTERN";
	String commentTag = "A.TOOLS." + myNAmeTag + ".STEP" + step + ".NAME";
	String stepPattern = Common.getBuildEnvironmentVariable(myProject, myCConf, patternTag, "");
	String stepName = Common.getBuildEnvironmentVariable(myProject, myCConf, commentTag, "");
	do {
	    monitor.subTask("Running " + stepName);
	    myOutconsole.println(stepPattern);
	    try {
		RunConsoledCommand(myConsole, stepPattern, monitor);
	    } catch (IOException e) {
		myErrconsole.print("Error: " + e.getMessage());
		return false;
	    }
	    step++;
	    patternTag = "A.TOOLS." + myNAmeTag + ".STEP" + step + ".PATTERN";
	    commentTag = "A.TOOLS." + myNAmeTag + ".STEP" + step + ".NAME";
	    stepPattern = Common.getBuildEnvironmentVariable(myProject, myCConf, patternTag, "");
	    stepName = Common.getBuildEnvironmentVariable(myProject, myCConf, commentTag, "");
	} while (!stepPattern.isEmpty());

	return true;
    }

}
