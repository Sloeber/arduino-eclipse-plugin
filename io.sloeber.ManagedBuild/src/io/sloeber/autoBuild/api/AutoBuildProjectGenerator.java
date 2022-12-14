
package io.sloeber.autoBuild.api;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.cdt.core.CCorePlugin;
import org.eclipse.cdt.core.model.CoreModel;
import org.eclipse.cdt.core.model.IPathEntry;
import org.eclipse.cdt.core.settings.model.ICConfigurationDescription;
import org.eclipse.cdt.core.settings.model.ICProjectDescription;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.ICoreRunnable;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.tools.templates.core.IGenerator;
//import org.eclipse.tools.templates.freemarker.SourceRoot;
//import org.eclipse.tools.templates.freemarker.TemplateManifest;

public class AutoBuildProjectGenerator implements IGenerator {
	private URI myProjectURI=null;
	private String myProjectName= null;

	public AutoBuildProjectGenerator() {

	}

	@Override
	public void generate(IProgressMonitor monitor) throws CoreException {
		IGenerator.super.generate(monitor);

		final IWorkspace workspace = ResourcesPlugin.getWorkspace();
		IWorkspaceRoot root = workspace.getRoot();
		IProject newProject = root.getProject(myProjectName);
		// IndexerController.doNotIndex(newProjectHandle);

		// create a eclipse project
		IProjectDescription description = workspace.newProjectDescription(myProjectName);
		if (myProjectURI != null) {
			description.setLocationURI(myProjectURI);
		}
		newProject.create(description, monitor);
		newProject.open(monitor);
		IFolder srcFolder= newProject.getFolder("src");
		srcFolder.create(true, true, monitor);
		IFile mainFile=srcFolder.getFile("main.cpp");
		InputStream stream = new ByteArrayInputStream("intentionally empty for now".getBytes(StandardCharsets.UTF_8));
		mainFile.create(stream, true, monitor);

//                        // Add the sketch code
//                        Map<String, IPath> librariesToAdd = codeDesc.createFiles(newProjectHandle, internalMonitor);
//
//                        // Add the arduino code folders
//                        List<IPath> addToIncludePath = Helpers.addArduinoCodeToProject(newProjectHandle, boardDescriptor);
//

		// make the eclipse project a cdt project
		CCorePlugin.getDefault().createCDTProject(description, newProject,"io.sloeber.autoBuild.Project.ID", new NullProgressMonitor());
		// ManagedBuilderCorePlugin.MANAGED_MAKE_PROJECT_ID);

		// add the required natures
//                        ManagedCProjectNature.addManagedNature(newProjectHandle, monitor);
//                        ManagedCProjectNature.addManagedBuilder(newProjectHandle, monitor);
//                        ManagedCProjectNature.addNature(newProjectHandle, "org.eclipse.cdt.core.ccnature", monitor); //$NON-NLS-1$
//                        ManagedCProjectNature.addNature(newProjectHandle, Const.ARDUINO_NATURE_ID, monitor);

		// make the cdt project a managed build project
//		IProjectType sloeberProjType = ManagedBuildManager.getProjectType("io.sloeber.core.sketch"); //$NON-NLS-1$
//		ManagedBuildManager.createBuildInfo(newProjectHandle);
//		IManagedProject newProject = ManagedBuildManager.createManagedProject(newProjectHandle, sloeberProjType);
//		ManagedBuildManager.setNewProjectVersion(newProjectHandle);
//		// Copy over the Sloeber configs
//		IConfiguration defaultConfig = null;
//		IConfiguration[] configs = sloeberProjType.getConfigurations();
//		for (int i = 0; i < configs.length; ++i) {
//			IConfiguration curConfig = newProject.createConfiguration(configs[i], sloeberProjType.getId() + "." + i); //$NON-NLS-1$
//			curConfig.setArtifactName(newProject.getDefaultArtifactName());
//			curConfig.getEditableBuilder().setParallelBuildOn(compileDescriptor.isParallelBuildEnabled());
//			// Make the first configuration the default
//			if (i == 0) {
//				defaultConfig = curConfig;
//			}
//		}

		// create a sloeber project
		// SloeberProject arduinoProjDesc = new SloeberProject(newProjectHandle);
		// arduinoProjDesc.myIsInMemory = true;
		// the line below will trigger environment var requests causing loops if called
		// to early
//		ManagedBuildManager.setDefaultConfiguration(newProjectHandle, defaultConfig);

		CCorePlugin cCorePlugin = CCorePlugin.getDefault();
//                        ICProjectDescription prjCDesc = cCorePlugin.getProjectDescription(newProjectHandle, true);
//                        Set<String> configKeys = GetConfigKeysFromProjectDescription(prjCDesc);
//
//                        for (String curConfigKey : configKeys) {
//
////                            arduinoProjDesc.myCompileDescriptions.put(curConfigKey, compileDescriptor);
////                            arduinoProjDesc.myBoardDescriptions.put(curConfigKey, boardDescriptor);
////                            arduinoProjDesc.myOtherDescriptions.put(curConfigKey, otherDesc);
////                            ICConfigurationDescription curConfigDesc = prjCDesc.getConfigurationByName(curConfigKey);
////                            Libraries.adjustProjectDescription(curConfigDesc, pathMods);
////                            Helpers.addIncludeFolder(curConfigDesc, addToIncludePath, true);
//
//                        }

//                        arduinoProjDesc.createSloeberConfigFiles();
//                        arduinoProjDesc.setAllEnvironmentVars();
//                        arduinoProjDesc.myIsInMemory = true;

		monitor.done();
	}



	@Override
	public IFile[] getFilesToOpen() {
		// TODO Auto-generated method stub
		return IGenerator.super.getFilesToOpen();
	}


	public void setProjectName(String projectName) {
		myProjectName=projectName;

	}

	public void setLocationURI(URI locationURI) {
		// TODO Auto-generated method stub
		myProjectURI=locationURI;
	}

}
