package io.sloeber.core.managers;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {
	private static final String BUNDLE_NAME = "io.sloeber.core.managers.messages"; //$NON-NLS-1$
	public static String Manager_Cant_create_folder;
	public static String Manager_Cant_extract_file;
	public static String Manager_Failed_to_download;
	public static String Manager_Failed_to_extract;
	public static String Manager_File_already_exists;
	public static String Manager_File_exists;
	public static String Manager_Format_not_supported;
	public static String Manager_is_outside;
	public static String Manager_links_to_absolute_path;
	public static String Manager_no_single_root_folder;
	public static String Manager_no_single_root_folder_while_file;
	public static String Manager_Warning_file;
	public static String Platform_loading_boards;
	public static String Platform_loading_platform;
	public static String Tool_no_valid_system;
	public static String ToolDependency_Tool_not_found;

	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	private Messages() {
	}
}
