package io.sloeber.autoBuild.core;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {
    private static final String BUNDLE_NAME = "io.sloeber.autoBuild.core.messages"; //$NON-NLS-1$

    public static String ManagedMakeBuilder_message_starting;
    public static String ManagedMakeBuilder_message_rebuild_makefiles;
    public static String ManagedMakeBuilder_message_update_makefiles;
    public static String ManagedMakeBuilder_message_incremental;
    public static String ManagedMakeBuilder_message_updating;
    public static String ManagedMakeBuilder_message_make;
    public static String ManagedMakeBuilder_message_internal_builder;
    public static String ManagedMakeBuilder_message_regen_deps;
    public static String ManagedMakeBuilder_message_updating_deps;
    public static String ManagedMakeBuilder_message_creating_markers;
    public static String ManagedMakeBuilder_message_console_header;
    public static String ManagedMakeBuilder_message_internal_builder_header_note;
    public static String ManagedMakeBuilder_message_no_build;
    public static String ManagedMakeBuilder_message_error;
    public static String ManagedMakeBuilder_message_error_build;
    public static String ManagedMakeBuilder_message_undefined_build_command;
    public static String ManagedMakeBuilder_message_finished;
    public static String ManagedMakeBuilder_message_cancelled;
    public static String ManagedMakeBuilder_message_finished_with_errs;
    public static String ManagedMakeBuilder_message_internal_builder_error;
    public static String ManagedMakeBuilder_message_stopped_error;
    public static String ManagedMakeBuilder_message_clean_deleting_output;
    public static String ManagedMakeBuilder_message_clean_build_clean;
    public static String ManagedMakeBuilder_message_program_not_in_path;
    public static String ManagedMakeBuilder_message_build_finished;
    public static String ManagedMakeBuilder_type_clean;
    public static String ManagedMakeBuider_type_incremental;
    public static String ManagedMakeBuider_type_rebuild;
    public static String ManagedMakeBuilder_warning_unsupported_configuration;
    public static String ManagedMakeBuilder_error_prefix;

    public static String Option_error_bad_value_type;
    public static String ManagedBuildManager_error_owner_not_null;
    public static String ManagedBuildManager_error_null_owner;
    public static String ManagedBuildManager_error_owner_not_project;
    public static String ManagedBuildManager_error_manifest_load_failed_title;
    public static String ManagedBuildManager_error_manifest_version_error;
    public static String ManagedBuildManager_error_manifest_header;
    public static String ManagedBuildManager_error_manifest_resolving;
    public static String ManagedBuildManager_error_manifest_duplicate;
    public static String ManagedBuildManager_error_manifest_icon;
    public static String ManagedBuildManager_error_manifest_option_category;
    public static String ManagedBuildManager_error_manifest_option_filter;
    public static String ManagedBuildManager_error_manifest_option_valuehandler;
    public static String ManagedBuildManager_error_open_failed_title;
    public static String ManagedBuildManager_error_open_failed;
    public static String ManagedBuildManager_error_write_failed_title;
    public static String ManagedBuildManager_error_write_failed;
    public static String ManagedBuildManager_error_read_only;
    public static String ManagedBuildManager_error_project_version_error;
    public static String ManagedBuildManager_error_id_nomatch;
    public static String ManagedBuildManager_error_project_file_missing;
    public static String MakefileGenerator_message_start_file;
    public static String MakefileGenerator_message_finish_file;
    public static String MakefileGenerator_message_start_build;
    public static String MakefileGenerator_message_finish_build;
    public static String MakefileGenerator_message_start_dependency;
    public static String MakefileGenerator_message_no_target;
    public static String MakefileGenerator_message_adding_source_folder;
    public static String MakefileGenerator_message_gen_source_makefile;
    public static String MakefileGenerator_message_calc_delta;
    public static String MakefileGenerator_message_finding_sources;
    public static String MakefileGenerator_comment_module_list;
    public static String MakefileGenerator_comment_module_variables;
    public static String MakefileGenerator_comment_source_list;
    public static String MakefileGenerator_comment_build_rule;
    public static String MakefileGenerator_comment_build_toprules;
    public static String MakefileGenerator_comment_build_alltarget;
    public static String MakefileGenerator_comment_build_mainbuildtarget;
    public static String MakefileGenerator_comment_build_toptargets;
    public static String MakefileGenerator_comment_module_make_includes;
    public static String MakefileGenerator_comment_module_dep_includes;
    public static String MakefileGenerator_comment_autodeps;
    public static String MakefileGenerator_comment_header;
    public static String MakefileGenerator_error_spaces;
    public static String MakeBuilder_Invoking_Make_Builder;
    public static String MakeBuilder_Invoking_Command;
    public static String MakeBuilder_Updating_project;
    public static String MakeBuilder_Creating_Markers;
    public static String MakefileGenerator_warning_no_source;
    public static String MakefileGenerator_error_no_nameprovider;
    public static String ManagedBuildInfo_message_job_init;
    public static String ManagedBuildInfo_message_init_ok;
    public static String GnuMakefileGenerator_message_postproc_dep_file;
    public static String Configuration_orphaned;
    public static String Tool_default_announcement;
    public static String Tool_Problem_Discovering_Args_For_Option;
    public static String StorableEnvironmentLoader_storeOutputStream_wrong_arguments;
    public static String UserDefinedMacroSupplier_storeOutputStream_wrong_arguments;
    public static String BuildMacroStatus_status_macro_undefined;
    public static String GeneratedMakefileBuilder_buildingSelectedFiles;
    public static String BuildDescriptionGnuMakefileGenerator_0;
    public static String BuildDescriptionGnuMakefileGenerator_1;
    public static String BuildMacroStatus_status_reference_eachother;
    public static String BuildMacroStatus_status_reference_incorrect;
    public static String BuildMacroStatus_status_macro_not_string;
    public static String BuildMacroStatus_status_macro_not_stringlist;
    public static String BuildMacroStatus_status_error;
    public static String BuildMacroStatus_value_undefined;
    public static String BuildInfoFactory_Missing_Builder;
    public static String ResourceChangeHandler_buildInfoSerializationJob;
    public static String GeneratedMakefileBuilder_buildingProject;
    public static String GeneratedMakefileBuilder_cleaningProject;
    public static String GeneratedMakefileBuilder_removingResourceMarkers;
    public static String GeneratedMakefileBuilder_refreshingArtifacts;
    public static String GeneratedMakefileBuilder_fileDeleted;
    public static String GeneratedMakefileBuilder_nothingToClean;
    public static String GenerateMakefileWithBuildDescription_0;
    public static String GenerateMakefileWithBuildDescription_1;
    public static String ManagedBuilderCorePlugin_resourceChangeHandlingInitializationJob;
    public static String InternalBuilder_msg_header;
    public static String InternalBuilder_nothing_todo;
    public static String CfgScannerConfigUtil_ErrorNotSupported;
    public static String GeneratedMakefileBuilder_cleanSelectedFiles;
    public static String FolderInfo_4;
    public static String GnuLinkOutputNameProvider_0;
    public static String CommonBuilder_2;
    public static String CommonBuilder_6;
    public static String CommonBuilder_7;
    public static String CommonBuilder_0;
    public static String CommonBuilder_16;
    public static String CommonBuilder_12;
    public static String CommonBuilder_13;
    public static String CommonBuilder_22;
    public static String CommonBuilder_23;
    public static String CommonBuilder_24;
    public static String CommonBuilder_circular_dependency;
    public static String ParallelBuilder_missingOutDir;
    public static String MakeBuilder_buildError;
    public static String MultiResourceInfo_MultiResourceInfo_UnhandledIHoldsOptionsType;
    public static String ResourceChangeHandler2_0;
    public static String ToolInfo_0;
    public static String ToolInfo_1;
    public static String AbstractBuiltinSpecsDetector_ClearingMarkers;
    public static String AbstractBuiltinSpecsDetector_DiscoverBuiltInSettingsJobName;
    public static String AbstractBuiltinSpecsDetector_ScannerDiscoveryTaskTitle;
    public static String AbstractBuiltinSpecsDetector_SerializingResults;
    public static String ExternalBuilderName;
    public static String InternalBuilderName;

	public static String InternalBuildRunner_NoNeedToRun;

	public static String ScannerDiscoveryMarkerLocationPreferences;

	public static String ScannerDiscoveryMarkerLocationProperties;

	public static String AddScannerDiscoveryMarkers;

	public static String RunningScannerDiscovery;
    static {
        // initialize resource bundle
        NLS.initializeMessages(BUNDLE_NAME, Messages.class);
    }

    private Messages() {
    }
}
