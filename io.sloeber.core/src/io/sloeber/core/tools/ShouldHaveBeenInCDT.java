package io.sloeber.core.tools;

import java.net.URI;

import org.eclipse.cdt.core.CCorePlugin;
import org.eclipse.cdt.managedbuilder.core.BuildException;
import org.eclipse.cdt.managedbuilder.core.IConfiguration;
import org.eclipse.cdt.managedbuilder.core.IManagedProject;
import org.eclipse.cdt.managedbuilder.core.IProjectType;
import org.eclipse.cdt.managedbuilder.core.ManagedBuildManager;
import org.eclipse.cdt.managedbuilder.core.ManagedBuilderCorePlugin;
import org.eclipse.cdt.managedbuilder.core.ManagedCProjectNature;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceDescription;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;

@SuppressWarnings("nls")
public class ShouldHaveBeenInCDT {
	/**
	 * Copied from test scripts from CDT
	 * This method creates the .cProject file in your project. it is intended to be
	 * used with newly created projects. Using this method with project that have
	 * existed for some time is unknown
	 *
	 *
	 * @param newProjectHandle The newly created project that needs a .cproject
	 *                         file.
	 * @param newProjectHandle Where the project will be located
	 * @param projectTypeId    The ID from your plugin.xml extension
	 *                         org.eclipse.cdt.managedbuilder.core.buildDefinitions
	 *                         projectType
	 * @throws CoreException
	 * @throws BuildException
	 *
	 */
	static public IProject createNewManagedProject(IProject newProjectHandle, final URI location,
			final String projectTypeId) throws Exception {
		final IWorkspace workspace = ResourcesPlugin.getWorkspace();
		final IProject project = newProjectHandle;
		CCorePlugin cCorePlugin =CCorePlugin.getDefault();

		// Create the base project
		IWorkspaceDescription workspaceDesc = workspace.getDescription();
		workspaceDesc.setAutoBuilding(false);
		workspace.setDescription(workspaceDesc);
		IProjectDescription description = workspace.newProjectDescription(project.getName());
		if (location != null) {
			description.setLocationURI(location);
		}
		cCorePlugin.createCProject(description, project, new NullProgressMonitor(),
				ManagedBuilderCorePlugin.MANAGED_MAKE_PROJECT_ID);


		ManagedBuildManager.createBuildInfo(project);

		ManagedCProjectNature.addManagedNature(project, new NullProgressMonitor());
		ManagedCProjectNature.addManagedBuilder(project, new NullProgressMonitor());

		// Find the base project type definition
		IProjectType projType = ManagedBuildManager.getProjectType(projectTypeId);
		IManagedProject newManagedProject = ManagedBuildManager.createManagedProject(project, projType);

		ManagedBuildManager.setNewProjectVersion(project);
		// Copy over the configurations
		
		IConfiguration defaultConfig = null;
		IConfiguration[] configs = projType.getConfigurations();
		for (int i = 0; i < configs.length; ++i) {
			// Make the first configuration the default
			if (i == 0) {
				defaultConfig = newManagedProject.createConfiguration(configs[i], projType.getId() + "." + i);
			} else {
				newManagedProject.createConfiguration(configs[i], projType.getId() + "." + i);
			}
		}
		ManagedBuildManager.setDefaultConfiguration(project, defaultConfig);


		IConfiguration cfgs[] = newManagedProject.getConfigurations();
		for (int i = 0; i < cfgs.length; i++) {
			cfgs[i].setArtifactName(newManagedProject.getDefaultArtifactName());
		}

	

		// Initialize the path entry container
		IStatus initResult = ManagedBuildManager.initBuildInfoContainer(project);
		if (initResult.getCode() != IStatus.OK) {
			// Assert.fail("Initializing build information failed for: " + project.getName()
			// + " because: " + initResult.getMessage());
		}
		return project;
	}

}
