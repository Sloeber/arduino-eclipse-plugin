package io.sloeber.autoBuild.helpers;

import org.eclipse.cdt.core.CCProjectNature;

import io.sloeber.autoBuild.integration.AutoBuildManager;
import io.sloeber.schema.api.IProjectType;

@SuppressWarnings("nls")
public class Defaults {

    static public final String defaultExtensionPointID = "io.sloeber.autoBuild.buildDefinitions";
    static public final String defaultExtensionID = "io.sloeber.autoBuild";
    static public final String defaultProjectTypeID = "io.sloeber.autoBuild.projectType.exe";
    static public final IProjectType defaultProjectType= AutoBuildManager.getProjectType(defaultExtensionPointID, defaultExtensionID, defaultProjectTypeID, true);
    static public final String defaultNatureID = CCProjectNature.CC_NATURE_ID;

}
