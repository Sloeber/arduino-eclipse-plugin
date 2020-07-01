package io.sloeber.core.tools;

import java.net.URI;

import org.eclipse.cdt.core.CCorePlugin;
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
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;

@SuppressWarnings("nls")
public class ShouldHaveBeenInCDT {
	/*
	 * Copied from test scripts from CDT
	 */
	/**
	 * This method creates the .cProject file in your project. it is intended to be
	 * used with newly created projects. Using this method with project that have
	 * existed for some time is unknown
	 *
	 *
	 * @param newProjectHandle        The newly created project that needs a .cproject file.
	 * @param newProjectHandle        Where the project will be located
     * @param projectTypeId 		The ID from your plugin.xml extension org.eclipse.cdt.managedbuilder.core.buildDefinitions  projectType
	 *
	 */
	static public IProject createNewManagedProject(IProject newProjectHandle, final URI location,
			final String projectTypeId) {
		final IWorkspace workspace = ResourcesPlugin.getWorkspace();
		IWorkspaceRoot root = workspace.getRoot();
		final IProject project = newProjectHandle;
		IWorkspaceRunnable runnable = new IWorkspaceRunnable() {

			
			@Override
			public void run(IProgressMonitor monitor) throws CoreException {
				// Create the base project
				IWorkspaceDescription workspaceDesc = workspace.getDescription();
				workspaceDesc.setAutoBuilding(false);
				workspace.setDescription(workspaceDesc);
				IProjectDescription description = workspace.newProjectDescription(project.getName());
				if (location != null) {
					description.setLocationURI(location);
				}
				CCorePlugin.getDefault().createCProject(description, project, new NullProgressMonitor(), ManagedBuilderCorePlugin.MANAGED_MAKE_PROJECT_ID);
				// Add the managed build nature and builder
				addManagedBuildNature(project);

				// Find the base project type definition
				IProjectType projType = ManagedBuildManager.getProjectType(projectTypeId);

				// Assert.assertNotNull(projType);

				// Create the managed-project (.cdtbuild) for our project that builds an
				// executable.
				IManagedProject newProject = null;
				try {
					newProject = ManagedBuildManager.createManagedProject(project, projType);
				} catch (Exception e) {
					// Assert.fail("Failed to create managed project for: " + project.getName());
					return;
				}
//				Assert.assertEquals(newProject.getName(), projType.getName());
//				Assert.assertFalse(newProject.equals(projType));
				ManagedBuildManager.setNewProjectVersion(project);
				// Copy over the configs
				IConfiguration defaultConfig = null;
				IConfiguration[] configs = projType.getConfigurations();
				for (int i = 0; i < configs.length; ++i) {
					// Make the first configuration the default
					if (i == 0) {
						defaultConfig = newProject.createConfiguration(configs[i], projType.getId() + "." + i);
					} else {
						newProject.createConfiguration(configs[i], projType.getId() + "." + i);
					}
				}
				ManagedBuildManager.setDefaultConfiguration(project, defaultConfig);

				IConfiguration cfgs[] = newProject.getConfigurations();
				for (int i = 0; i < cfgs.length; i++) {
					cfgs[i].setArtifactName(newProject.getDefaultArtifactName());
				}

				ManagedBuildManager.getBuildInfo(project).setValid(true);
			}
		};
		NullProgressMonitor monitor = new NullProgressMonitor();
		try {
			workspace.run(runnable, root, IWorkspace.AVOID_UPDATE, monitor);
		} catch (@SuppressWarnings("unused") CoreException e2) {
			// Assert.fail(e2.getLocalizedMessage());
		}
//		// CDT opens the Project with BACKGROUND_REFRESH enabled which causes the
//		// refresh manager to refresh the project 200ms later.  This Job interferes
//		// with the resource change handler firing see: bug 271264
//		try {
//			// CDT opens the Project with BACKGROUND_REFRESH enabled which causes the
//			// refresh manager to refresh the project 200ms later.  This Job interferes
//			// with the resource change handler firing see: bug 271264
//			Job.getJobManager().join(ResourcesPlugin.FAMILY_AUTO_REFRESH, null);
//		} catch (Exception e) {
//			// Ignore
//		}

		// Initialize the path entry container
		IStatus initResult = ManagedBuildManager.initBuildInfoContainer(project);
		if (initResult.getCode() != IStatus.OK) {
			// Assert.fail("Initializing build information failed for: " + project.getName()
			// + " because: " + initResult.getMessage());
		}
		return project;
	}

	static public void addManagedBuildNature(IProject project) {
		// Create the buildinformation object for the project
		 ManagedBuildManager.createBuildInfo(project);


		// Add the managed build nature
		try {
			ManagedCProjectNature.addManagedNature(project, new NullProgressMonitor());
			ManagedCProjectNature.addManagedBuilder(project, new NullProgressMonitor());

		} catch (@SuppressWarnings("unused") CoreException e) {
			// Assert.fail("Test failed on adding managed build nature or builder: " +
			// e.getLocalizedMessage());
		}


	}
}
