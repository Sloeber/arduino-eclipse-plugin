package it.baeyens.arduino.managers;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {
    private static final String BUNDLE_NAME = "it.baeyens.arduino.managers.messages"; //$NON-NLS-1$
    public static String ArduinoManager_Cant_create_folder;
    public static String ArduinoManager_Cant_extract_file;
    public static String ArduinoManager_Downloading_make_exe;
    public static String ArduinoManager_Failed_to_download;
    public static String ArduinoManager_Failed_to_extract;
    public static String ArduinoManager_File_already_exists;
    public static String ArduinoManager_File_exists;
    public static String ArduinoManager_Format_not_supported;
    public static String ArduinoManager_is_outside;
    public static String ArduinoManager_links_to_absolute_path;
    public static String ArduinoManager_no_single_root_folder;
    public static String ArduinoManager_no_single_root_folder_while_file;
    public static String ArduinoManager_unable_to_create_file;
    public static String ArduinoManager_unable_to_write_to_file;
    public static String ArduinoManager_Warning_file;
    public static String ArduinoManager_will_not_work;
    public static String ArduinoPlatform_loading_boards;
    public static String ArduinoPlatform_loading_platform;
    public static String ArduinoTool_no_valid_system;
    public static String ToolDependency_Tool_not_found;

    static {
	// initialize resource bundle
	NLS.initializeMessages(BUNDLE_NAME, Messages.class);
    }

    private Messages() {
    }
}
