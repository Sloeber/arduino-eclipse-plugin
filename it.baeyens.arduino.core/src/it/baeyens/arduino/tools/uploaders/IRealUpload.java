package it.baeyens.arduino.tools.uploaders;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IProgressMonitor;

public abstract interface IRealUpload {
    abstract public boolean uploadUsingPreferences(IFile hexFile, boolean usingProgrammer, IProgressMonitor monitor);
}
