package io.sloeber.core;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {
    private static final String BUNDLE_NAME = "io.sloeber.core.messages"; //$NON-NLS-1$
    public static final String PORT_TAG = "{PORT}"; //$NON-NLS-1$
    public static final String MENUITEMID_TAG = "{MENUITEMID}"; //$NON-NLS-1$
    public static final String MENUID_TAG = "{MENUID}"; //$NON-NLS-1$
    public static final String BOARDID_TAG = "{BOARDID}"; //$NON-NLS-1$
    public static final String MENUITEMNAME_TAG = "{MENUITEMNAME}"; //$NON-NLS-1$
    public static final String LIB_TAG = "{LIB}"; //$NON-NLS-1$
    public static final String PROJECT_TAG = "{PROJECT}"; //$NON-NLS-1$
    public static final String UPLOADER_TAG = "{UPLOADER}"; //$NON-NLS-1$
    public static final String MS_TAG = "{MS}"; //$NON-NLS-1$
    public static final String NAME_TAG = "{NAME}"; //$NON-NLS-1$
    public static final String ID_TAG = "{ID}"; //$NON-NLS-1$
    public static final String COMMAND_TAG = "{COMMAND}"; //$NON-NLS-1$
    public static final String TOOL_TAG = "{TOOL}"; //$NON-NLS-1$
    public static final String FILE_TAG = "{FILE}"; //$NON-NLS-1$
    public static final String BOARD_TAG = "{BOARD}"; //$NON-NLS-1$
    public static final String CONFIG_TAG = "{CONFIG}"; //$NON-NLS-1$
    public static final String KEY_TAG = "{KEY}"; //$NON-NLS-1$
    public static final String FOLDER_TAG = "{FOLDER}"; //$NON-NLS-1$
    public static final String VERSION_TAG = "{VERSION}"; //$NON-NLS-1$
    public static final String HOST_TAG = "{HOST}"; //$NON-NLS-1$

    public static String ArduinoSerial_Comport_Appeared_and_disappeared;
    public static String ArduinoSerial_Comport_is_not_behaving_as_expected;
    public static String ArduinoSerial_comport_not_found;
    public static String ArduinoSerial_Comport_reset_took;
    public static String ArduinoSerial_Continuing_to_use;
    public static String ArduinoSerial_Ending_reset;
    public static String ArduinoSerial_exception_while_opening_seral_port;
    public static String ArduinoSerial_port_reappeared;
    public static String ArduinoSerial_port_still_missing;
    public static String ArduinoSerial_reset_dtr_toggle;
    public static String ArduinoSerial_reset_failed;
    public static String ArduinoSerial_unable_to_open_serial_port;
    public static String ArduinoSerial_Using_1200bps_touch;
    public static String ArduinoSerial_Using_comport;
    public static String BoardDescription_0;
	public static String Boards_Failed_to_read_boards;
    public static String Boards_Get_menu_item_name_from_id_did_not_find;
    public static String Boards_menu_ID_not_found;
    public static String Boards_menu_name_not_found;
    public static String command_aborted;
    public static String command_finished;
    public static String command_interupted;
    public static String command_io;
    public static String command_launching;
    public static String command_output;
    public static String EmptyLibFolder;
    public static String Failed_To_Add_Libraries;
    public static String failed_to_remove_lib;
    public static String Boards_Get_menu_item_id_from_name_failed;
    public static String Helpers_Create_folder_failed;
    public static String Helpers_delete_folder_failed;
    public static String Helpers_error_boards_TXT;
    public static String Helpers_Error_File_does_not_exists;
    public static String Helpers_error_link_folder_is_empty;
    public static String Helpers_Error_parsing_IO_exception;
    public static String Helpers_Error_The_folder_is_empty;
    public static String Helpers_No_boards_txt_found;
    public static String Helpers_tool_reference_missing;
    public static String import_lib_failed;
    public static String Manager_archive_error_root_folder_name_mismatch;
    public static String Manager_archive_error_symbolic_link_to_absolute_path;
    public static String Manager_archiver_eror_single_root_folder_required;
    public static String Manager_Cant_create_folder;
    public static String Manager_Cant_create_folder_exists;
    public static String Manager_Cant_extract_file_exist;
    public static String Manager_Failed_to_download;
    public static String Manager_Failed_to_extract;
    public static String Manager_Failed_to_parse;
    public static String Manager_Format_not_supported;
    public static String MultipleVersionsOfLib;
    public static String Platform_loading_boards;
    public static String Platform_loading_platform;
    public static String security_login;
    public static String security_password;
    public static String Tool_no_valid_system;
    public static String ToolDependency_Tool_not_found;
    public static String Upload_console_name;
    public static String Upload_error_auth_fail;
    public static String Upload_Error_com_port;
    public static String Upload_error_connection_refused;
    public static String Upload_error_network;
    public static String Upload_Error_serial_monitor_restart;
    public static String Upload_failed;
    public static String Upload_failed_upload_file;
    public static String Upload_login_credentials_missing;
    public static String Upload_no_arduino_sketch;
    public static String Upload_Project_nature_unaccesible;
    public static String Upload_sending_sketch;
    public static String Upload_sketch_on_yun;
    public static String Upload_starting;
    public static String Upload_uploading;
    public static String uploader_Failed_to_get_upload_recipe;
    public static String uploader_no_reset_using_network;
    public static String uploader_no_reset_using_programmer;
    public static String BoardsManagerIsBussy;
    public static String No_Platform_available;
    public static String decorator_no_platform;
    public static String decorator_no_port;
    public static String projectNotFoundInGUI;
    public static String sizeReportSketch;
    public static String sizeReportData;
	public static String CompileDescription_CustomDebugLevel;
	public static String CompileDescription_OptimizedForDebug;
	public static String CompileDescription_OptimizedForRelease;
	public static String CompileDescription_WarningsAll;
	public static String CompileDescription_WarningsCustom;
	public static String CompileDescription_WarningsDefault;
	public static String CompileDescription_WarningsMore;
	public static String CompileDescription_WarningsNone;
	public static String SloeberConfiguration_Failed_Modify_config_rename;
	public static String SloeberProject_Project_is_null;

    static {
        // initialize resource bundle
        NLS.initializeMessages(BUNDLE_NAME, Messages.class);
    }

    private Messages() {
    }
}
