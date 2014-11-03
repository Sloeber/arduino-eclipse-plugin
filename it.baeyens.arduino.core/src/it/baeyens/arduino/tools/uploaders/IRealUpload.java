package it.baeyens.arduino.tools.uploaders;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IProgressMonitor;

public abstract interface IRealUpload {
    abstract public boolean uploadUsingPreferences(IFile hexFile, IProject project, boolean usingProgrammer, IProgressMonitor monitor);
}
