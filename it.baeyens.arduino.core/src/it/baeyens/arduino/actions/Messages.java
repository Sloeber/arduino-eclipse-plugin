package it.baeyens.arduino.actions;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {
    private static final String BUNDLE_NAME = "it.baeyens.arduino.actions.messages"; //$NON-NLS-1$
    public static String ArduinoUploadProjectHandler_Build_failed;
    public static String ArduinoUploadProjectHandler_Build_failed_so_no_upload;
    public static String ArduinoUploadProjectHandler_Multiple_projects_found;
    public static String ArduinoUploadProjectHandler_No_project_found;
    public static String ArduinoUploadProjectHandler_The_Names_Are;
    public static String ArduinoUploadProjectHandler_Upload_for_project;
    public static String BuildHandler_Build_Code_of_project;
    public static String BuildHandler_Failed_to_build;
    public static String BuildHandler_No_Project_found;
    public static String BuildHandler_Start_Build_Activator;
    public static String ReattachLibraries_no_project_found;

    static {
	// initialize resource bundle
	NLS.initializeMessages(BUNDLE_NAME, Messages.class);
    }

    private Messages() {
    }
}
