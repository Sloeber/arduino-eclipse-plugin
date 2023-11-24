package io.sloeber.autoBuild.helpers;

import org.eclipse.cdt.core.CCProjectNature;

@SuppressWarnings("nls")
public class Defaults {

    static public final String defaultExtensionPointID = "io.sloeber.autoBuild.buildDefinitions";
    static public final String defaultExtensionID = "cdt.cross.gnu";
    static public final String defaultProjectTypeID = "cdt.managedbuild.target.gnu.cross.exe";
    static public final String defaultNatureID = CCProjectNature.CC_NATURE_ID;

}
