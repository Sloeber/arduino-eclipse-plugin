package it.baeyens.arduino.tools.uploaders;

import java.io.IOException;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.ui.console.MessageConsole;
import org.eclipse.ui.console.MessageConsoleStream;

import it.baeyens.arduino.common.Common;
import it.baeyens.arduino.tools.ExternalCommandLauncher;

public class GenericLocalUploader implements IRealUpload {

    private String myNAmeTag;
    private IProject myProject;
    private String myCConf;
    private MessageConsole myConsole;
    private MessageConsoleStream myErrconsole;
    private MessageConsoleStream myOutconsole;

    GenericLocalUploader(String NAmeTag, IProject Project, String CConf, MessageConsole Console, MessageConsoleStream Errconsole,
	    MessageConsoleStream Outconsole) {
	this.myNAmeTag = NAmeTag.toUpperCase();
	this.myProject = Project;
	this.myCConf = CConf;

	this.myConsole = Console;
	this.myErrconsole = Errconsole;
	this.myOutconsole = Outconsole;
    }

    protected static void RunConsoledCommand(MessageConsole console, String command, IProgressMonitor monitor) throws IOException {

	ExternalCommandLauncher Step = new ExternalCommandLauncher(command);

	Step.setConsole(console);
	Step.redirectErrorStream(true);
	Step.launch(monitor);
    }

    @Override
    public boolean uploadUsingPreferences(IFile hexFile, boolean usingProgrammer, IProgressMonitor monitor) {
	int step = 1;
	String patternTag = "A.TOOLS." + this.myNAmeTag + ".STEP" + step + ".PATTERN"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	String commentTag = "A.TOOLS." + this.myNAmeTag + ".STEP" + step + ".NAME"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	String stepPattern = Common.getBuildEnvironmentVariable(this.myProject, this.myCConf, patternTag, ""); //$NON-NLS-1$
	String stepName = Common.getBuildEnvironmentVariable(this.myProject, this.myCConf, commentTag, ""); //$NON-NLS-1$
	do {
	    monitor.subTask("Running " + stepName); //$NON-NLS-1$
	    this.myOutconsole.println(stepPattern);
	    try {
		RunConsoledCommand(this.myConsole, stepPattern, monitor);
	    } catch (IOException e) {
		this.myErrconsole.print("Error: " + e.getMessage()); //$NON-NLS-1$
		return false;
	    }
	    step++;
	    patternTag = "A.TOOLS." + this.myNAmeTag + ".STEP" + step + ".PATTERN"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	    commentTag = "A.TOOLS." + this.myNAmeTag + ".STEP" + step + ".NAME"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	    stepPattern = Common.getBuildEnvironmentVariable(this.myProject, this.myCConf, patternTag, ""); //$NON-NLS-1$
	    stepName = Common.getBuildEnvironmentVariable(this.myProject, this.myCConf, commentTag, ""); //$NON-NLS-1$
	} while (!stepPattern.isEmpty());

	return true;
    }

}
