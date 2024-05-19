package io.sloeber.autoBuild.api;

import org.eclipse.core.resources.IContainer;
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
    boolean createFiles(IContainer srcContainer, IProgressMonitor monitor);

    /**
     * Check whether this codeProvider provides code compatible with the buildArtifactType
     *
     * @param buildArtifactType a string that represents a buildArtifactType; should not be null
     *
     * @return true if this buildArtifactType can be build based from this code
     */
	boolean supports(String buildArtifactType);

	/**
	 * Check whether this codeProvider provides code compatible with the buildArtifactType and nature
	 *
	 * @param buildArtifactType a string that represents a buildArtifactType; should not be null
	 * @param natureID a string that represents a natureID; should not be null
	 * @return
	 */
	boolean supports(String buildArtifactType, String natureID);

	String getName();

	String getDescription();

	String getID();

	boolean getContainsCppCode();

	String getCodeFolder();

	void setCodeFolder(String codeFolder);
}
