package io.sloeber.core.tools.uploaders;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.ui.console.MessageConsoleStream;

import io.sloeber.core.api.BoardDescriptor;

public abstract interface IRealUpload {
	abstract public boolean uploadUsingPreferences(IFile hexFile, BoardDescriptor boardDescriptor,
			IProgressMonitor monitor, 
			MessageConsoleStream myHighStream,
			MessageConsoleStream myOutStream,
			MessageConsoleStream myErrStream);
}
