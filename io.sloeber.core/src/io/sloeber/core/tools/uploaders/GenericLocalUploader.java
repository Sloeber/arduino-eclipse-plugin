package io.sloeber.core.tools.uploaders;

import java.io.IOException;

import org.eclipse.cdt.core.settings.model.ICConfigurationDescription;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.ui.console.MessageConsoleStream;

import io.sloeber.core.api.BoardDescriptor;
import io.sloeber.core.common.Common;
import io.sloeber.core.tools.ExternalCommandLauncher;

public class GenericLocalUploader implements IRealUpload {

	private String myNAmeTag;
	private ICConfigurationDescription myConDesc;

	private MessageConsoleStream myErrStream;
	private MessageConsoleStream myOutStream;
	private MessageConsoleStream myHighConsole;

	GenericLocalUploader(String NAmeTag,  ICConfigurationDescription CConf, MessageConsoleStream highConsole,
			MessageConsoleStream Errconsole, MessageConsoleStream Outconsole) {
		myNAmeTag = NAmeTag.toUpperCase();
		myConDesc = CConf;
		myErrStream = Errconsole;
		myOutStream = Outconsole;
		myHighConsole=highConsole;
	}



	@Override
	public boolean uploadUsingPreferences(IFile hexFile, BoardDescriptor boardDescriptor, IProgressMonitor monitor) {
		int step = 1;
		String patternTag = "A.TOOLS." + this.myNAmeTag + ".STEP" + step + ".PATTERN"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		String commentTag = "A.TOOLS." + this.myNAmeTag + ".STEP" + step + ".NAME"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		String stepPattern = Common.getBuildEnvironmentVariable( myConDesc, patternTag, new String()); 
		String stepName = Common.getBuildEnvironmentVariable( myConDesc, commentTag, new String()); 
		do {
			monitor.subTask("Running " + stepName); //$NON-NLS-1$
			this.myOutStream.println(stepPattern);
			try {
				ExternalCommandLauncher launchStep = new ExternalCommandLauncher(stepPattern);
				launchStep.launch(monitor, myHighConsole, myOutStream, myErrStream);			

			} catch (IOException e) {
				this.myErrStream.print("Error: " + e.getMessage()); //$NON-NLS-1$
				return false;
			}
			step++;
			patternTag = "A.TOOLS." + this.myNAmeTag + ".STEP" + step + ".PATTERN"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			commentTag = "A.TOOLS." + this.myNAmeTag + ".STEP" + step + ".NAME"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			stepPattern = Common.getBuildEnvironmentVariable(myConDesc, patternTag, ""); //$NON-NLS-1$
			stepName = Common.getBuildEnvironmentVariable(myConDesc, commentTag, ""); //$NON-NLS-1$
		} while (!stepPattern.isEmpty());

		return true;
	}

}
