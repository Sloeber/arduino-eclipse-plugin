package io.sloeber.core.tools;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {
	private static final String BUNDLE_NAME = "io.sloeber.core.tools.messages"; //$NON-NLS-1$
	public static String Boards_Failed_to_read_boards;
	public static String Boards_Get_menu_item_name_from_id_did_not_find;
	public static String Boards_name;
	public static String Boards_not_found;
	public static String Helpers_boards_file;
	public static String Helpers_Boards_id;
	public static String Helpers_Create_folder_failed;
	public static String Helpers_delete_folder_failed;
	public static String Helpers_Error_parsing;
	public static String Helpers_File_does_not_exists;
	public static String Helpers_Invalid_boards_config;
	public static String Helpers_IO_exception;
	public static String Helpers_menu;
	public static String Helpers_No_boards_txt_found;
	public static String Helpers_The_project;
	public static String Helpers_tool_reference_missing;
	public static String Helpers_link_folder;
	public static String Helpers_is_empty;
	public static String Helpers_The_folder;
	public static String Helpers_No_command_for;
	public static String Helpers_ProblemInProgrammerFie;
	public static String command_aborted;
	public static String command_finished;
	public static String command_interupted;
	public static String command_io;
	public static String command_launching;
	public static String command_output;
	public static String failed_to_remove_lib;
	public static String import_lib_failed;
	public static String security_login;
	public static String security_password;
	public static String getMenuItemIDFromMenuItemName;

	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	private Messages() {
	}
}
