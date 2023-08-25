
package io.sloeber.autoBuild.integration;

import java.net.URI;
import java.util.List;

import org.eclipse.cdt.core.CCProjectNature;
import org.eclipse.cdt.core.CCorePlugin;
import org.eclipse.cdt.core.CProjectNature;
import org.eclipse.cdt.core.language.settings.providers.ILanguageSettingsProvider;
import org.eclipse.cdt.core.language.settings.providers.ILanguageSettingsProvidersKeeper;
import org.eclipse.cdt.core.language.settings.providers.LanguageSettingsManager;
import org.eclipse.cdt.core.language.settings.providers.ScannerDiscoveryLegacySupport;
import org.eclipse.cdt.core.model.CoreModel;
import org.eclipse.cdt.core.settings.model.ICConfigurationDescription;
import org.eclipse.cdt.core.settings.model.ICProjectDescription;
import org.eclipse.cdt.core.settings.model.ICProjectDescriptionManager;
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
import org.eclipse.tools.templates.core.IGenerator;

import io.sloeber.autoBuild.api.ICodeProvider;
import io.sloeber.autoBuild.core.Activator;
import io.sloeber.schema.api.IConfiguration;
import io.sloeber.schema.api.IProjectType;
import io.sloeber.schema.internal.Configuration;

public class AutoBuildProjectGenerator implements IGenerator {
    private URI myProjectURI = null;
    private String myProjectName = null;
    private IProject myProject = null;
    private String myExtensionPointID = null;
    private String myExtensionID = null;
    private String myProjectTypeID = null;
    private ICodeProvider myCodeProvider = null;
    private String myNatureID;

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
                IProjectDescription description = workspace.newProjectDescription(myProjectName);
                if (myProjectURI != null) {
                    description.setLocationURI(myProjectURI);
                }
                myProject = root.getProject(myProjectName);
                myProject.create(description, monitor);
                myProject.open(monitor);
                CProjectNature.addCNature(myProject, monitor);
                if (CCProjectNature.CC_NATURE_ID.equals(myNatureID)) {
                    CCProjectNature.addCCNature(myProject, monitor);
                }
                AutoBuildNature.addNature(myProject, monitor);

                IFolder srcFolder = myProject.getFolder("src"); //$NON-NLS-1$
                if (!srcFolder.exists()) {
                    srcFolder.create(true, true, monitor);
                }
                if (myCodeProvider != null) {
                    myCodeProvider.createFiles(srcFolder, monitor);
                }

                myProject = CCorePlugin.getDefault().createCDTProject(description, myProject, monitor);

                ICProjectDescriptionManager mngr = CoreModel.getDefault().getProjectDescriptionManager();
                ICProjectDescription des = mngr.createProjectDescription(myProject, false, true);

                IProjectType sloeberProjType = AutoBuildManager.getProjectType(myExtensionPointID, myExtensionID,
                        myProjectTypeID, true);
                IConfiguration[] modelConfigs = sloeberProjType.getConfigurations();
                Configuration cf = (Configuration) modelConfigs[0];
                for (IConfiguration iConfig : modelConfigs) {
                    //        for (CfgHolder cfg : cfgs) {
                    Configuration config = (Configuration) iConfig;
                    AutoBuildConfigurationDescription data = new AutoBuildConfigurationDescription(config, myProject);
                    assert (data != null);
                    ICConfigurationDescription cdtCfgDes = des
                            .createConfiguration(AutoBuildConfigurationDescriptionProvider.CFG_DATA_PROVIDER_ID, data);
                    data.setCdtConfigurationDescription(cdtCfgDes);

                    //Set the language Settings
                    String[] defaultIds = iConfig.getDefaultLanguageSettingsProviderIds().toArray(new String[0]);
                    List<ILanguageSettingsProvider> providers = LanguageSettingsManager
                            .createLanguageSettingsProviders(defaultIds);
                    if (cdtCfgDes instanceof ILanguageSettingsProvidersKeeper) {
                        ((ILanguageSettingsProvidersKeeper) cdtCfgDes)
                                .setDefaultLanguageSettingsProvidersIds(defaultIds);
                        ((ILanguageSettingsProvidersKeeper) cdtCfgDes).setLanguageSettingProviders(providers);
                    }
                }
                des.setCdtProjectCreated();
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
        // TODO Auto-generated method stub
        myProjectURI = locationURI;
    }

    public IProject getProject() {
        return myProject;
    }

    public void setExtentionPointID(String extensionPointID) {
        myExtensionPointID = extensionPointID;
    }

    public void setExtentionID(String extensionID) {
        myExtensionID = extensionID;
    }

    public void setProjectTypeID(String projectTypeID) {
        myProjectTypeID = projectTypeID;

    }

    public void setCodeProvider(ICodeProvider codeProvider) {
        myCodeProvider = codeProvider;

    }

    public void setNatureID(String natureID) {
        myNatureID = natureID;

    }

}
