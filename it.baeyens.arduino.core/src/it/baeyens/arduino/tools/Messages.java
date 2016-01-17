package it.baeyens.arduino.tools;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {
    private static final String BUNDLE_NAME = "it.baeyens.arduino.tools.messages"; //$NON-NLS-1$
    public static String And_Configuration;
    public static String ArduinoBoards_Failed_to_read_boards;
    public static String ArduinoBoards_Get_menu_item_name_from_id_did_not_find;
    public static String ArduinoBoards_name;
    public static String ArduinoBoards_not_found;

    public static String ArduinoHelpers__in_boards_invalid;
    public static String ArduinoHelpers_boards_file;
    public static String ArduinoHelpers_Boards_id;
    public static String ArduinoHelpers_Core_refference_missing;
    public static String ArduinoHelpers_Create_folder_failed;
    public static String ArduinoHelpers_delete_folder_failed;
    public static String ArduinoHelpers_Error_parsing;
    public static String ArduinoHelpers_File_does_not_exists;
    public static String ArduinoHelpers_File_missing;
    public static String ArduinoHelpers_Invalid_boards_config;
    public static String ArduinoHelpers_IO_exception;
    public static String ArduinoHelpers_menu;
    public static String ArduinoHelpers_No_boards_txt_found;
    public static String ArduinoHelpers_The_project;
    public static String ArduinoHelpers_Value_for_key;
    public static String ArduinoHelpers_Variant_reference_missing;
    public static String ArduinoHelpers_link_folder;
    public static String ArduinoHelpers_is_empty;
    public static String ArduinoHelpers_Could_not_add_folder;
    public static String ArduinoHelpers_To_include_path;
    public static String ArduinoHelpers_The_folder;
    public static String ArduinoHelpers_No_command_for;
    public static String command_aborted;
    public static String command_finished;
    public static String command_interupted;
    public static String command_io;
    public static String command_launching;
    public static String command_output;
    public static String failed_to_remove_lib;
    public static String import_lib_failed;
    public static String Is_Not_valid_for_project;
    public static String security_login;
    public static String security_password;
    public static String The_lib;

    static {
	// initialize resource bundle
	NLS.initializeMessages(BUNDLE_NAME, Messages.class);
    }

    private Messages() {
    }
}
