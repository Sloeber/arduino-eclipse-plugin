package it.baeyens.arduino.tools;

import it.baeyens.arduino.ui.BuildConfigurationsPage.ConfigurationDescriptor;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.cdt.core.language.settings.providers.ILanguageSettingsProvider;
import org.eclipse.cdt.core.language.settings.providers.ILanguageSettingsProvidersKeeper;
import org.eclipse.cdt.core.language.settings.providers.LanguageSettingsManager;
import org.eclipse.cdt.core.language.settings.providers.ScannerDiscoveryLegacySupport;
import org.eclipse.cdt.core.model.CoreModel;
import org.eclipse.cdt.core.settings.model.ICConfigurationDescription;
import org.eclipse.cdt.core.settings.model.ICProjectDescription;
import org.eclipse.cdt.core.settings.model.ICProjectDescriptionManager;
import org.eclipse.cdt.core.settings.model.extension.CConfigurationData;
import org.eclipse.cdt.managedbuilder.core.IBuilder;
import org.eclipse.cdt.managedbuilder.core.IConfiguration;
import org.eclipse.cdt.managedbuilder.core.IToolChain;
import org.eclipse.cdt.managedbuilder.core.ManagedBuildManager;
import org.eclipse.cdt.managedbuilder.internal.core.Configuration;
import org.eclipse.cdt.managedbuilder.internal.core.ManagedBuildInfo;
import org.eclipse.cdt.managedbuilder.internal.core.ManagedProject;
import org.eclipse.cdt.managedbuilder.internal.core.ToolChain;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

@SuppressWarnings("restriction")
// TOFIX Get this code in CDT so I should not have to do this
public class ShouldHaveBeenInCDT {
    /*
     * Copied from wizard STDWizardHandler package package org.eclipse.cdt.managedbuilder.ui.wizards;; This method creates the .cProject file in your
     * project.
     * 
     * BK: modified this and made it work for multiple configs.
     */
    /**
     * This method creates the .cProject file in your project. it is intended to be used with newly created projects. Using this method with project
     * that have existed for some time is unknown
     * 
     * 
     * @param project
     *            The newly created project that needs a .cproject file.
     * @param alCfgs
     *            An array-list of configuration descriptors (names, toolchain IDs) to be used with this project
     * @param isManagedBuild
     *            When true the project is managed build. Else the project is not (read you have to maintain the makefiles yourself)
     * @param monitor
     *            The monitor to follow the process
     * @throws CoreException
     */
    public static ICProjectDescription setCProjectDescription(IProject project, ArrayList<ConfigurationDescriptor> alCfgs, boolean isManagedBuild,
	    IProgressMonitor monitor) throws CoreException {

	ICProjectDescriptionManager mngr = CoreModel.getDefault().getProjectDescriptionManager();
	ICProjectDescription des = mngr.createProjectDescription(project, false, true);
	ManagedBuildInfo info = ManagedBuildManager.createBuildInfo(project);
	ManagedProject mProj = new ManagedProject(des);
	info.setManagedProject(mProj);
	monitor.worked(20);

	// Iterate across the configurations
	for (int i = 0; i < alCfgs.size(); i++) {
	    IToolChain tcs = ManagedBuildManager.getExtensionToolChain(alCfgs.get(i).ToolchainID);

	    Configuration cfg = new Configuration(mProj, (ToolChain) tcs, ManagedBuildManager.calculateChildId(alCfgs.get(i).ToolchainID, null),
		    alCfgs.get(i).Name);
	    IBuilder bld = cfg.getEditableBuilder();
	    if (bld != null) {
		bld.setManagedBuildOn(isManagedBuild);
		cfg.setArtifactName("${ProjName}");
	    } else {
		System.out.println("Messages.StdProjectTypeHandler_3");
	    }
	    CConfigurationData data = cfg.getConfigurationData();
	    ICConfigurationDescription cfgDes = des.createConfiguration(ManagedBuildManager.CFG_DATA_PROVIDER_ID, data);

	    setDefaultLanguageSettingsProviders(project, alCfgs.get(i), cfg, cfgDes);
	}
	monitor.worked(50);
	return des;

    }

    private static void setDefaultLanguageSettingsProviders(IProject project, ConfigurationDescriptor cfgDes, IConfiguration cfg,
	    ICConfigurationDescription cfgDescription) {
	// propagate the preference to project properties
	boolean isPreferenceEnabled = ScannerDiscoveryLegacySupport.isLanguageSettingsProvidersFunctionalityEnabled(null);
	ScannerDiscoveryLegacySupport.setLanguageSettingsProvidersFunctionalityEnabled(project, isPreferenceEnabled);

	if (cfgDescription instanceof ILanguageSettingsProvidersKeeper) {
	    ILanguageSettingsProvidersKeeper lspk = (ILanguageSettingsProvidersKeeper) cfgDescription;

	    lspk.setDefaultLanguageSettingsProvidersIds(new String[] { cfgDes.ToolchainID });

	    List<ILanguageSettingsProvider> providers = getDefaultLanguageSettingsProviders(cfg, cfgDescription);
	    lspk.setLanguageSettingProviders(providers);
	}
    }

    private static List<ILanguageSettingsProvider> getDefaultLanguageSettingsProviders(IConfiguration cfg, ICConfigurationDescription cfgDescription) {
	List<ILanguageSettingsProvider> providers = new ArrayList<ILanguageSettingsProvider>();
	String[] ids = cfg != null ? cfg.getDefaultLanguageSettingsProviderIds() : null;

	if (ids == null) {
	    // Try with legacy providers
	    ids = ScannerDiscoveryLegacySupport.getDefaultProviderIdsLegacy(cfgDescription);
	}

	if (ids != null) {
	    for (String id : ids) {
		ILanguageSettingsProvider provider = null;
		if (!LanguageSettingsManager.isPreferShared(id)) {
		    provider = LanguageSettingsManager.getExtensionProviderCopy(id, false);
		}
		if (provider == null) {
		    provider = LanguageSettingsManager.getWorkspaceProvider(id);
		}
		providers.add(provider);
	    }
	}

	return providers;
    }

}
