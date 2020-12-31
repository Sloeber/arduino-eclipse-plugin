package io.sloeber.core.tools.uploaders;

import static io.sloeber.core.common.Common.*;
import static io.sloeber.core.common.Const.*;

import java.io.IOException;

import org.eclipse.cdt.core.settings.model.ICConfigurationDescription;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.ui.console.MessageConsoleStream;

import io.sloeber.core.api.BoardDescription;
import io.sloeber.core.tools.ExternalCommandLauncher;

class GenericLocalUploader implements IRealUpload {

	private String myNAmeTag;
	private ICConfigurationDescription myConDesc;

	GenericLocalUploader(String NAmeTag, ICConfigurationDescription CConf) {
		myNAmeTag = NAmeTag.toUpperCase();
		myConDesc = CConf;
	}

	@Override
	public boolean uploadUsingPreferences(IFile hexFile, BoardDescription boardDescriptor, IProgressMonitor monitor,
			MessageConsoleStream highStream, MessageConsoleStream outStream, MessageConsoleStream errStream) {

		int step = 1;
        String patternTag = TOOLS + myNAmeTag + DOT + STEP + step + DOT + PATTERN;
        String commentTag = TOOLS + myNAmeTag + DOT + STEP + step + DOT + NAME;
        String stepPattern = getBuildEnvironmentVariable(myConDesc, patternTag, new String());
        String stepName = getBuildEnvironmentVariable(myConDesc, commentTag, new String());
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
            patternTag = TOOLS + myNAmeTag + DOT + STEP + step + DOT + PATTERN;
            commentTag = TOOLS + myNAmeTag + DOT + STEP + step + DOT + NAME;
            stepPattern = getBuildEnvironmentVariable(myConDesc, patternTag, ""); //$NON-NLS-1$
            stepName = getBuildEnvironmentVariable(myConDesc, commentTag, ""); //$NON-NLS-1$
		} while (!stepPattern.isEmpty());

		return true;
	}

}
