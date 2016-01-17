package it.baeyens.arduino.ui;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {
    private static final String BUNDLE_NAME = "it.baeyens.arduino.ui.messages"; //$NON-NLS-1$
    public static String error_adding_arduino_code;
    public static String error_no_platform_files_found;
    public static String error_make_finder_failed;
    public static String error_make_is_not_found;
    public static String error_no_Arduino_project_selected;
    public static String error_no_host_name;
    public static String error_failed_to_import_library_in_project;

    public static String ui_build_config;
    public static String ui_debug_binaries;
    public static String ui_dont_touch;
    public static String ui_some_bla_bla;
    public static String ui_Adopting_arduino_libraries;
    public static String ui_Alternative_size;
    public static String ui_Apend_c_cpp;
    public static String ui_adopting_platforms;
    public static String ui_append_c;
    public static String ui_append_c_cpp_text;
    public static String ui_append_c_text;
    public static String ui_append_cpp;
    public static String ui_append_cpp_text;
    public static String ui_ask_every_upload;
    public static String ui_build_before_upload;
    public static String ui_category;

    public static String ui_import_arduino_libraries_in_project;
    public static String ui_import_arduino_libraries_in_project_help;
    public static String ui_import_no_arduino_project_help;
    public static String ui_import_source_folder;
    public static String ui_import_source_folder_help;
    public static String ui_import_subfolder_to_import_to;
    public static String ui_installing_arduino_libraries;
    public static String ui_installing_platforms;
    public static String ui_looking_for_make;

    public static String ui_make_is_found;

    public static String ui_name;
    public static String ui_new_sketch_arduino_information;
    public static String ui_new_sketch_arduino_information_help;
    public static String ui_new_sketch_build_configurations;
    public static String ui_new_sketch_custom_template;
    public static String ui_new_sketch_custom_template_location;
    public static String ui_new_sketch_default_cpp;
    public static String ui_new_sketch_default_ino;
    public static String ui_new_sketch_error_failed_to_create_project;
    public static String ui_new_sketch_error_folder_must_contain_sketch_cpp;
    public static String ui_new_sketch_link_to_sample_code;
    public static String ui_new_sketch_sample_sketch;
    public static String ui_new_sketch_Select_additional_configurations;
    public static String ui_new_sketch_Select_additional_configurations_help;
    public static String ui_new_sketch_select_example_code;
    public static String ui_new_sketch_selecy_code;
    public static String ui_new_sketch_sketch_template_folder;
    public static String ui_new_sketch_sketch_template_location;
    public static String ui_new_sketch_these_settings_cn_be_changed_later;
    public static String ui_new_sketch_title;
    public static String ui_new_sketch_title_help;
    public static String ui_new_sketch_use_current_settings_as_default;
    public static String ui_port;
    public static String ui_private_hardware_path;
    public static String ui_private_hardware_path_help;
    public static String ui_private_lib_path;
    public static String ui_private_lib_path_help;
    public static String ui_remove;
    public static String ui_sec_login;
    public static String ui_sec_login_and_password;
    public static String ui_sec_password;
    public static String ui_select;
    public static String ui_select_Arduino_libraries;
    public static String ui_select_folder;
    public static String ui_show_all_warnings;
    public static String ui_url_for_package_index_file;
    public static String ui_version;
    public static String ui_workspace_settings;
    public static String ui_make_what;
    public static String ui_make_test;
    public static String ui_success;
    public static String ui_warning;
    public static String ui_create_a;
    public static String ui_error_select_arduino_project;
    public static String error;

    static {
	// initialize resource bundle
	NLS.initializeMessages(BUNDLE_NAME, Messages.class);
    }

    private Messages() {
    }
}
