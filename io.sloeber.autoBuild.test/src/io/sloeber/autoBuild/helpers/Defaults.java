package io.sloeber.autoBuild.helpers;

import org.eclipse.cdt.core.CCProjectNature;
import org.eclipse.core.runtime.Platform;
import org.osgi.framework.Bundle;

import io.sloeber.autoBuild.api.AutoBuildNewProjectCodeManager;
import io.sloeber.autoBuild.api.ICodeProvider;
import io.sloeber.autoBuild.integration.AutoBuildManager;
import io.sloeber.autoBuild.schema.api.IProjectType;

@SuppressWarnings("nls")
public class Defaults {

    static public final String defaultExtensionPointID = "io.sloeber.autoBuild.buildDefinitions";
    static public final String defaultExtensionID = "io.sloeber.autoBuild";
    static public final String defaultProjectTypeID = "io.sloeber.autoBuild.projectType.exe";
    static public final IProjectType defaultProjectType= AutoBuildManager.getProjectType(defaultExtensionPointID, defaultExtensionID, defaultProjectTypeID, true);
    static public final String defaultNatureID = CCProjectNature.CC_NATURE_ID;
    static public final Bundle thisBundle = Platform.getBundle("io.sloeber.autoBuild.test"); //$NON-NLS-1$
    public static final ICodeProvider c_exeCodeProvider = AutoBuildNewProjectCodeManager.getDefault().getCodeProvider("io.sloeber.autoBuild.templateCode.c.exe");
    public static final ICodeProvider c_LibProvider = AutoBuildNewProjectCodeManager.getDefault().getCodeProvider("io.sloeber.autoBuild.templateCode.c.lib");
    public static final ICodeProvider cpp_exeCodeProvider = AutoBuildNewProjectCodeManager.getDefault().getCodeProvider("io.sloeber.autoBuild.templateCode.cpp.exe");
    public static final ICodeProvider cpp_LibProvider = AutoBuildNewProjectCodeManager.getDefault().getCodeProvider("io.sloeber.autoBuild.templateCode.cpp.lib");
    public static final ICodeProvider compoundProvider = AutoBuildNewProjectCodeManager.getDefault().getCodeProvider("io.sloeber.autoBuild.templateCode.compound");
    

}
