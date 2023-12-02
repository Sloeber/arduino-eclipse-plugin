package io.sloeber.autoBuild.api;

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

public interface ICodeProvider {
    /**
     * Method that will create file in the project during project creation
     * 
     * @param srcFolder
     *            the project that will contain the files
     * @param monitor
     *            the monitor to monitor progress
     * @return
     * @throws CoreException
     */
    boolean createFiles(IFolder srcFolder, IProgressMonitor monitor);

}
