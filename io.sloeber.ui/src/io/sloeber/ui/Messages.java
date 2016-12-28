package io.sloeber.ui;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {
    private static final String BUNDLE_NAME = "io.sloeber.ui.messages"; //$NON-NLS-1$

    public static String platformSelectionTip;

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
    public static String ui_open_serial_with_monitor;
    public static String ui_build_before_upload;
    public static String ui_auto_import_libraries;
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

    public static String json_browser_fail;

    public static String json_find;

    public static String json_maintain;

    public static String json_update;

    public static String LibraryPreferencePage_add_remove;

    public static String SampleSelector_num_selected;

    public static String ui_Clean_Serial_Monitor_After_Upload;
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
    public static String UploadProjectHandler_build_failed;
    public static String pleaseWaitForInstallerJob;
    public static String buildBeforeUpload;
    public static String doYouWantToBuildBeforeUpload;

    public static String packageTooltip;

    static {
	// initialize resource bundle
	NLS.initializeMessages(BUNDLE_NAME, Messages.class);
    }

    private Messages() {
    }
}
