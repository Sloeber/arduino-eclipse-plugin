package io.sloeber.core.tools.uploaders;

import java.io.IOException;

import org.eclipse.cdt.core.settings.model.ICConfigurationDescription;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.ui.console.MessageConsoleStream;

import io.sloeber.core.api.BoardDescriptor;
import io.sloeber.core.common.Common;
import io.sloeber.core.common.Const;
import io.sloeber.core.tools.ExternalCommandLauncher;

class GenericLocalUploader implements IRealUpload {

	private String myNAmeTag;
	private ICConfigurationDescription myConDesc;

	GenericLocalUploader(String NAmeTag, ICConfigurationDescription CConf) {
		myNAmeTag = NAmeTag.toUpperCase();
		myConDesc = CConf;
	}

	@Override
	public boolean uploadUsingPreferences(IFile hexFile, BoardDescriptor boardDescriptor, IProgressMonitor monitor,
			MessageConsoleStream highStream, MessageConsoleStream outStream, MessageConsoleStream errStream) {
		final String STEP = Const.STEP;
		final String DOT = Const.DOT;
		final String A_TOOLS = Const.A_TOOLS;
		int step = 1;
		String patternTag = A_TOOLS + myNAmeTag + DOT + STEP + step + DOT + Const.PATTERN;
		String commentTag = A_TOOLS + myNAmeTag + DOT + STEP + step + DOT + Const.NAME;
		String stepPattern = Common.getBuildEnvironmentVariable(myConDesc, patternTag, new String());
		String stepName = Common.getBuildEnvironmentVariable(myConDesc, commentTag, new String());
		do {
			monitor.subTask("Running " + stepName); //$NON-NLS-1$
			outStream.println(stepPattern);
			try {
				ExternalCommandLauncher launchStep = new ExternalCommandLauncher(stepPattern);
				launchStep.launch(monitor, highStream, outStream, errStream);

			} catch (IOException e) {
				errStream.print("Error: " + e.getMessage()); //$NON-NLS-1$
				return false;
			}
			step++;
			patternTag = A_TOOLS + myNAmeTag + DOT + STEP + step + DOT + Const.PATTERN;
			commentTag = A_TOOLS + myNAmeTag + DOT + STEP + step + DOT + Const.NAME;
			stepPattern = Common.getBuildEnvironmentVariable(myConDesc, patternTag, ""); //$NON-NLS-1$
			stepName = Common.getBuildEnvironmentVariable(myConDesc, commentTag, ""); //$NON-NLS-1$
		} while (!stepPattern.isEmpty());

		return true;
	}

}
