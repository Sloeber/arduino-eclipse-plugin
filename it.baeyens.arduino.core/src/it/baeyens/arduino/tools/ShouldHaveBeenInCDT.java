package it.baeyens.arduino.tools;

import org.eclipse.cdt.core.model.CoreModel;
import org.eclipse.cdt.core.settings.model.ICConfigurationDescription;
import org.eclipse.cdt.core.settings.model.ICProjectDescription;
import org.eclipse.cdt.core.settings.model.ICProjectDescriptionManager;
import org.eclipse.cdt.core.settings.model.extension.CConfigurationData;
import org.eclipse.cdt.managedbuilder.core.IBuilder;
import org.eclipse.cdt.managedbuilder.core.IToolChain;
import org.eclipse.cdt.managedbuilder.core.ManagedBuildManager;
import org.eclipse.cdt.managedbuilder.internal.core.Configuration;
import org.eclipse.cdt.managedbuilder.internal.core.ManagedBuildInfo;
import org.eclipse.cdt.managedbuilder.internal.core.ManagedProject;
import org.eclipse.cdt.managedbuilder.internal.core.ToolChain;
import org.eclipse.cdt.managedbuilder.internal.dataprovider.ConfigurationDataProvider;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import it.baeyens.arduino.ui.BuildConfigurationsPage.ConfigurationDescriptor;
import java.util.ArrayList;

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
    public static void setCProjectDescription(IProject project,
    	ArrayList<ConfigurationDescriptor> alCfgs,
    	boolean isManagedBuild, IProgressMonitor monitor) throws CoreException {

	ICProjectDescriptionManager mngr = CoreModel.getDefault().getProjectDescriptionManager();
	ICProjectDescription des = mngr.createProjectDescription(project, false, false);
	ManagedBuildInfo info = ManagedBuildManager.createBuildInfo(project);
	ManagedProject mProj = new ManagedProject(des);
	info.setManagedProject(mProj);
	monitor.worked(20);

	// String s = "it.baeyens.arduino.core.toolChain.release";
	// IToolChain tcs = ManagedBuildManager.getExtensionToolChain(s);
	//
	// Configuration cfg = new Configuration(mProj, (ToolChain) tcs, ManagedBuildManager.calculateChildId(s, null), "Release");
	// IBuilder bld = cfg.getEditableBuilder();
	// if (bld != null) {
	// if (bld.isInternalBuilder()) {
	// IConfiguration prefCfg = ManagedBuildManager.getPreferenceConfiguration(false);
	// IBuilder prefBuilder = prefCfg.getBuilder();
	// cfg.changeBuilder(prefBuilder, ManagedBuildManager.calculateChildId(cfg.getId(), null), prefBuilder.getName());
	// bld = cfg.getEditableBuilder();
	// bld.setBuildPath(null);
	// }
	// bld.setManagedBuildOn(isManagedBuild);
	// } else {
	// System.out.println("Messages.StdProjectTypeHandler_3");
	// }
	// cfg.setArtifactName(mProj.getDefaultArtifactName());
	// CConfigurationData data = cfg.getConfigurationData();
	// ICConfigurationDescription cfgDes = des.createConfiguration(ManagedBuildManager.CFG_DATA_PROVIDER_ID, data);
	//
	// ConfigurationDataProvider.setDefaultLanguageSettingsProviders(project, cfg, cfgDes);

	// Iterate across the configurations
	for (int i = 0; i < alCfgs.size(); i++) {
	    IToolChain tcs = ManagedBuildManager.getExtensionToolChain(alCfgs.get(i).ToolchainID);

	    Configuration cfg = new Configuration(mProj, (ToolChain) tcs, ManagedBuildManager.calculateChildId(alCfgs.get(i).ToolchainID, null), alCfgs.get(i).Name);
	    IBuilder bld = cfg.getEditableBuilder();
	    if (bld != null) {
		// if (bld.isInternalBuilder()) {
		// IConfiguration prefCfg = ManagedBuildManager.getPreferenceConfiguration(false);
		// IBuilder prefBuilder = prefCfg.getBuilder();
		// String name = prefBuilder.getName();
		// cfg.changeBuilder(prefBuilder, ManagedBuildManager.calculateChildId(cfg.getId(), null), name);
		// bld = cfg.getEditableBuilder();
		// bld.setBuildPath(null);
		// }
		bld.setManagedBuildOn(isManagedBuild);
		cfg.setArtifactName("${ProjName}");
	    } else {
		System.out.println("Messages.StdProjectTypeHandler_3");
	    }
	    CConfigurationData data = cfg.getConfigurationData();
	    ICConfigurationDescription cfgDes = des.createConfiguration(ManagedBuildManager.CFG_DATA_PROVIDER_ID, data);
	    ConfigurationDataProvider.setDefaultLanguageSettingsProviders(project, cfg, cfgDes);
	}
	monitor.worked(50);
	mngr.setProjectDescription(project, des);

    }

}
