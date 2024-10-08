
package io.sloeber.autoBuild.integration;

import java.net.URI;
import java.util.HashSet;
import java.util.List;

import org.eclipse.cdt.core.CCProjectNature;
import org.eclipse.cdt.core.CCorePlugin;
import org.eclipse.cdt.core.CProjectNature;
import org.eclipse.cdt.core.language.settings.providers.ILanguageSettingsProvider;
import org.eclipse.cdt.core.language.settings.providers.ILanguageSettingsProvidersKeeper;
import org.eclipse.cdt.core.language.settings.providers.LanguageSettingsManager;
import org.eclipse.cdt.core.model.CoreModel;
import org.eclipse.cdt.core.settings.model.ICConfigurationDescription;
import org.eclipse.cdt.core.settings.model.ICProjectDescription;
import org.eclipse.cdt.core.settings.model.ICProjectDescriptionManager;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.ICoreRunnable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.tools.templates.core.IGenerator;

import io.sloeber.autoBuild.api.AutoBuildCommon;
import io.sloeber.autoBuild.api.ICodeProvider;
import io.sloeber.autoBuild.buildTools.api.IBuildTools;
import io.sloeber.autoBuild.core.Activator;
import io.sloeber.autoBuild.schema.api.IConfiguration;
import io.sloeber.autoBuild.schema.api.IProjectType;

public class AutoBuildProjectGenerator implements IGenerator {
	private URI myLocationURI = null;
	private String myProjectName = null;
	private IProject myProject = null;
	private IProjectType myProjectType = null;
	private ICodeProvider myCodeProvider = null;
	private String myNatureID = null;
	private String myBuilderID = null;
	private boolean myNeedsMoreWork = false;
	private IBuildTools myBuldTools = null;
	private String myCodeRootFolder = null;

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

				ICProjectDescriptionManager mngr = CoreModel.getDefault().getProjectDescriptionManager();
				IProjectType autoBuildProjType = AutoBuildManager.getProjectType(myProjectType.getExtensionPointID(),
						myProjectType.getExtensionID(), myProjectType.getId(), true);
				if (autoBuildProjType == null) {
					// project type not found can not continue
					IStatus status = new Status(IStatus.ERROR, Activator.getId(),
							"Did not find the projectType with " + myProjectType.getId() + " for extension ID " //$NON-NLS-1$ //$NON-NLS-2$
									+ myProjectType.getExtensionID() + " in extensionpointID " //$NON-NLS-1$
									+ myProjectType.getExtensionPointID());
					CoreException exception = new CoreException(status);
					throw (exception);
				}
				HashSet<String> configNames = new HashSet<>();
				myProject = root.getProject(myProjectName);
				if (myProject.exists()) {
					// Update the existing configurations
					ICProjectDescription orgdesc = mngr.getProjectDescription(myProject, false);
					for (ICConfigurationDescription curConfig : orgdesc.getConfigurations()) {
						configNames.add(curConfig.getName());
					}
					myLocationURI=myProject.getLocationURI();
					//CDT reads the .cproject file when setting the configuration for making the delta
					//and old stuff stays in it causing problems
					//ICProject cProject = CoreModel.getDefault().getCModel().getCProject(myProject.getName());
					IFile CdtDotFile = myProject.getFile(".cproject"); //$NON-NLS-1$
					if(CdtDotFile.exists()) {
						CdtDotFile.delete(true, monitor);
					}
					myProject.close(monitor);
					myProject.delete(false, true, monitor);

				} else {
					// Create new configurations based on the model configurations from the
					// plugin.xml
					IConfiguration[] modelConfigs = autoBuildProjType.getConfigurations();
					for (IConfiguration iConfig : modelConfigs) {
						configNames.add(iConfig.getName());
					}
				}



				IProjectDescription description = workspace.newProjectDescription(myProjectName);
				if (myLocationURI != null) {
					description.setLocationURI(myLocationURI);
				}

				myProject.create(description, monitor);
				myProject.open(monitor);

				CProjectNature.addCNature(myProject, monitor);
				if (CCProjectNature.CC_NATURE_ID.equals(myNatureID)) {
					CCProjectNature.addCCNature(myProject, monitor);
				}
				AutoBuildNature.addNature(myProject, monitor);

				IContainer srcFolder = myProject;
				if (myCodeRootFolder != null && !myCodeRootFolder.isBlank()) {
					IFolder actualFolder = myProject.getFolder(myCodeRootFolder);
					srcFolder = actualFolder;
					if (!srcFolder.exists()) {
						actualFolder.create(true, true, monitor);
					}
				}
				if (myCodeProvider != null) {
					myCodeProvider.createFiles(srcFolder, monitor);
				}

				myProject = CCorePlugin.getDefault().createCDTProject(description, myProject, monitor);


				ICProjectDescription des = mngr.createProjectDescription(myProject, false, true);

				IConfiguration defaultConfig = autoBuildProjType.getConfigurations()[0];

				for (String curConfigName : configNames) {
					IConfiguration config = autoBuildProjType.getConfiguration(curConfigName);
					if (config == null) {
						config = defaultConfig;
					}
					AutoBuildConfigurationDescription data = new AutoBuildConfigurationDescription(config, myProject,
							myBuldTools, myCodeRootFolder);
					assert (data != null);
					data.setBuilder(data.getBuilder(myBuilderID));
					ICConfigurationDescription cdtCfgDes = des
							.createConfiguration(AutoBuildConfigurationDescriptionProvider.CFG_DATA_PROVIDER_ID, data);
					cdtCfgDes.setName(curConfigName);
					data.setCdtConfigurationDescription(cdtCfgDes);

					// Set the language Settings
					String[] defaultIds = config.getDefaultLanguageSettingsProviderIds().toArray(new String[0]);
					List<ILanguageSettingsProvider> providers = LanguageSettingsManager
							.createLanguageSettingsProviders(defaultIds);
					ILanguageSettingsProvidersKeeper languageKeeper = (ILanguageSettingsProvidersKeeper) cdtCfgDes;
					if (cdtCfgDes instanceof ILanguageSettingsProvidersKeeper) {
						languageKeeper.setLanguageSettingProviders(providers);
					}
					AutoBuildCommon.createFolder(data.getBuildFolder());
				}
				if (!myNeedsMoreWork) {
					des.setCdtProjectCreated();
				}
				des.setActiveConfiguration(des.getConfigurations()[0]);
				des.setDefaultSettingConfiguration(des.getConfigurations()[0]);
				mngr.setProjectDescription(myProject, des);

			}
		};
		try {
			workspace.run(runnable, root, IWorkspace.AVOID_UPDATE, monitor);
		} catch (Exception e) {
			Activator.log(e);
		}
		monitor.done();
	}

	@Override
	public IFile[] getFilesToOpen() {
		return IGenerator.super.getFilesToOpen();
	}

	public void setProjectName(String projectName) {
		myProjectName = projectName;

	}

	public void setLocationURI(URI locationURI) {
		myLocationURI = locationURI;
	}

	public IProject getProject() {
		return myProject;
	}

	public void setCodeProvider(ICodeProvider codeProvider) {
		myCodeProvider = codeProvider;
	}

	public void setNatureID(String natureID) {
		myNatureID = natureID;
	}

	public void setNeedsMoreWork(boolean needsMoreWork) {
		myNeedsMoreWork = needsMoreWork;
	}

	public void setBuilderName(String builderName) {
		myBuilderID = builderName;
	}

	public void setBuildTools(IBuildTools buildTool) {
		myBuldTools = buildTool;
	}

	public void setCodeRootFolder(String codeRootFolder) {
		myCodeRootFolder = codeRootFolder;

	}

	public void setProjectType(IProjectType projectType) {
		myProjectType = projectType;

	}

}
