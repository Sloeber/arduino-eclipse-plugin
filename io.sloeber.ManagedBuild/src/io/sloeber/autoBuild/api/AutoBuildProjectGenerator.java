
package io.sloeber.autoBuild.api;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;

import org.eclipse.cdt.core.CCorePlugin;
import org.eclipse.cdt.core.settings.model.ICProjectDescription;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.ICoreRunnable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.tools.templates.core.IGenerator;

import io.sloeber.autoBuild.Internal.ManagedBuildManager;
import io.sloeber.autoBuild.Internal.ManagedCProjectNature;

public class AutoBuildProjectGenerator implements IGenerator {
	private URI myProjectURI = null;
	private String myProjectName = null;

	public AutoBuildProjectGenerator() {

	}

	@Override
	public void generate(IProgressMonitor monitor) throws CoreException {
		IGenerator.super.generate(monitor);
		final IWorkspace workspace = ResourcesPlugin.getWorkspace();
		IWorkspaceRoot root = workspace.getRoot();
		ICoreRunnable runnable = new ICoreRunnable() {
			@Override
			public void run(IProgressMonitor internalMonitor) throws CoreException {

				final IWorkspace workspace = ResourcesPlugin.getWorkspace();
				IWorkspaceRoot root = workspace.getRoot();
				IProject newProject = root.getProject(myProjectName);
				// IndexerController.doNotIndex(newProjectHandle);

				// create a eclipse project
				IProjectDescription description = workspace.newProjectDescription(myProjectName);
				if (myProjectURI != null) {
					description.setLocationURI(myProjectURI);
				}

				// Set<String> prevNatures = new
				// HashSet<String>(Arrays.asList(description.getNatureIds()));
				// prevNatures.add("org.eclipse.cdt.core.ccnature");
				// //the line below makes it a CDT project
				// prevNatures.add("org.eclipse.cdt.managedbuilder.core.ScannerConfigNature");
				// //the line below adds the environment vars to the project properties
				// prevNatures.add("org.eclipse.cdt.managedbuilder.core.managedBuildNature");
				//
				// String[] newNatures = prevNatures.toArray(new String[0]);
				// description.setNatureIds(newNatures);

				newProject.create(description, monitor);
				newProject.open(monitor);
				IFolder srcFolder = newProject.getFolder("src");
				srcFolder.create(true, true, monitor);
				IFile mainFile = srcFolder.getFile("main.cpp");
				InputStream stream = new ByteArrayInputStream(
						"intentionally empty for now".getBytes(StandardCharsets.UTF_8));
				mainFile.create(stream, true, monitor);

				// // Add the sketch code
				// Map<String, IPath> librariesToAdd = codeDesc.createFiles(newProjectHandle,
				// internalMonitor);
				//
				// // Add the arduino code folders
				// List<IPath> addToIncludePath =
				// Helpers.addArduinoCodeToProject(newProjectHandle, boardDescriptor);
				//

				// make the eclipse project a cdt project
				CCorePlugin.getDefault().createCProject(description, newProject, new NullProgressMonitor(),
						 "org.eclipse.cdt.managedbuilder.core.managedMake");
						//"org.eclipse.cdt.core.cbuilder");
				// CCorePlugin.getDefault().createCDTProject(description, newProject, null, new
				// NullProgressMonitor());
				// ManagedBuilderCorePlugin.MANAGED_MAKE_PROJECT_ID
				// "org.eclipse.cdt.managedbuilder.core.managedMake");
				// "io.sloeber.autoBuild.Project.ID"
				
				// add the required natures
				ManagedCProjectNature.addManagedNature(newProject, monitor);
				ManagedCProjectNature.addManagedBuilder(newProject, monitor);
				ManagedCProjectNature.addNature(newProject, "org.eclipse.cdt.core.ccnature", monitor); //$NON-NLS-1$

				// make the cdt project a managed build project
				try {
					IProjectType sloeberProjType = ManagedBuildManager.getProjectType("io.sloeber.core.sketch"); //$NON-NLS-1$
					ManagedBuildManager.createBuildInfo(newProject);
					IManagedProject newManagedProject;

					newManagedProject = ManagedBuildManager.createManagedProject(newProject, sloeberProjType);

					ManagedBuildManager.setNewProjectVersion(newProject);
					// Copy over the Sloeber configs
					IConfiguration defaultConfig = null;
					IConfiguration[] configs = sloeberProjType.getConfigurations();
					for (int i = 0; i < configs.length; ++i) {
						IConfiguration curConfig = newManagedProject.createConfiguration(configs[i],
								sloeberProjType.getId() + "." + i); //$NON-NLS-1$
						curConfig.setArtifactName(newManagedProject.getDefaultArtifactName());
						// Make the first configuration the default
						if (i == 0) {
							defaultConfig = curConfig;
						}
					}
					ManagedBuildManager.setDefaultConfiguration(newProject, defaultConfig);
				} catch (BuildException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

				CCorePlugin cCorePlugin = CCorePlugin.getDefault();
				ICProjectDescription prjCDesc = cCorePlugin.getProjectDescription(newProject, true);
				cCorePlugin.setProjectDescription(newProject, prjCDesc, true, null);
				// Set<String> configKeys = GetConfigKeysFromProjectDescription(prjCDesc);
				//
				// for (String curConfigKey : configKeys) {
				//
				//// arduinoProjDesc.myCompileDescriptions.put(curConfigKey, compileDescriptor);
				//// arduinoProjDesc.myBoardDescriptions.put(curConfigKey, boardDescriptor);
				//// arduinoProjDesc.myOtherDescriptions.put(curConfigKey, otherDesc);
				//// ICConfigurationDescription curConfigDesc =
				// prjCDesc.getConfigurationByName(curConfigKey);
				//// Libraries.adjustProjectDescription(curConfigDesc, pathMods);
				//// Helpers.addIncludeFolder(curConfigDesc, addToIncludePath, true);
				//
				// }

				// arduinoProjDesc.createSloeberConfigFiles();
				// arduinoProjDesc.setAllEnvironmentVars();
				// arduinoProjDesc.myIsInMemory = true;
			}
		};
		try {
			workspace.run(runnable, root, IWorkspace.AVOID_UPDATE, monitor);
		} catch (Exception e) {
			// ignore for now
			// TOFIX
		}
		monitor.done();
	}

	@Override
	public IFile[] getFilesToOpen() {
		// TODO Auto-generated method stub
		return IGenerator.super.getFilesToOpen();
	}

	public void setProjectName(String projectName) {
		myProjectName = projectName;

	}

	public void setLocationURI(URI locationURI) {
		// TODO Auto-generated method stub
		myProjectURI = locationURI;
	}

}
