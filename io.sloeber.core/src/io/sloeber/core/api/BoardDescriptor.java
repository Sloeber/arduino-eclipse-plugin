package io.sloeber.core.api;

import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

import javax.swing.event.ChangeListener;

import org.eclipse.cdt.core.CCorePlugin;
import org.eclipse.cdt.core.ICDescriptor;
import org.eclipse.cdt.core.dom.IPDOMManager;
import org.eclipse.cdt.core.envvar.IContributedEnvironment;
import org.eclipse.cdt.core.envvar.IEnvironmentVariableManager;
import org.eclipse.cdt.core.language.settings.providers.ILanguageSettingsProvider;
import org.eclipse.cdt.core.language.settings.providers.ILanguageSettingsProvidersKeeper;
import org.eclipse.cdt.core.language.settings.providers.LanguageSettingsManager;
import org.eclipse.cdt.core.language.settings.providers.ScannerDiscoveryLegacySupport;
import org.eclipse.cdt.core.model.CoreModel;
import org.eclipse.cdt.core.settings.model.CSourceEntry;
import org.eclipse.cdt.core.settings.model.ICConfigurationDescription;
import org.eclipse.cdt.core.settings.model.ICExclusionPatternPathEntry;
import org.eclipse.cdt.core.settings.model.ICProjectDescription;
import org.eclipse.cdt.core.settings.model.ICProjectDescriptionManager;
import org.eclipse.cdt.core.settings.model.ICSettingEntry;
import org.eclipse.cdt.core.settings.model.ICSourceEntry;
import org.eclipse.cdt.core.settings.model.extension.CConfigurationData;
import org.eclipse.cdt.internal.core.pdom.indexer.IndexerPreferences;
import org.eclipse.cdt.managedbuilder.core.IBuilder;
import org.eclipse.cdt.managedbuilder.core.IConfiguration;
import org.eclipse.cdt.managedbuilder.core.IManagedBuildInfo;
import org.eclipse.cdt.managedbuilder.core.IManagedProject;
import org.eclipse.cdt.managedbuilder.core.IProjectType;
import org.eclipse.cdt.managedbuilder.core.ManagedBuildManager;
import org.eclipse.cdt.managedbuilder.core.ManagedCProjectNature;
import org.eclipse.cdt.managedbuilder.internal.core.Configuration;
import org.eclipse.cdt.managedbuilder.internal.core.ManagedBuildInfo;
import org.eclipse.cdt.managedbuilder.internal.core.ManagedProject;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceDescription;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;

import io.sloeber.core.Activator;
import io.sloeber.core.InternalBoardDescriptor;
import io.sloeber.core.Messages;
import io.sloeber.core.common.Common;
import io.sloeber.core.common.ConfigurationPreferences;
import io.sloeber.core.common.Const;
import io.sloeber.core.managers.InternalPackageManager;
import io.sloeber.core.tools.Helpers;
import io.sloeber.core.tools.KeyValue;
import io.sloeber.core.tools.Libraries;
import io.sloeber.core.tools.Programmers;
import io.sloeber.core.tools.TxtFile;

@SuppressWarnings({ "nls" })
public class BoardDescriptor {

    /*
     * Some constants
     */
    private static final String KEY_LAST_USED_BOARD = "Last used Board";
    private static final String KEY_LAST_USED_UPLOAD_PORT = "Last Used Upload port";
    private static final String KEY_LAST_USED_UPLOAD_PROTOCOL = "last Used upload Protocol";
    private static final String KEY_LAST_USED_BOARDS_FILE = "Last used Boards file";
    private static final String KEY_LAST_USED_BOARD_MENU_OPTIONS = "last used Board custom option selections";
    private static final String ENV_KEY_JANTJE_MENU_SELECTION = Const.ENV_KEY_JANTJE_START + "MENU";
    private static final String ENV_KEY_JANTJE_UPLOAD_PORT = Const.ENV_KEY_JANTJE_START + "COM_PORT";
    private static final String ENV_KEY_JANTJE_BOARD_NAME = Const.ENV_KEY_JANTJE_START + "BOARD_NAME";
    private static final String ENV_KEY_JANTJE_PROJECT_NAME = Const.ENV_KEY_JANTJE_START + "PROJECT_NAME";
    private static final String ENV_KEY_JANTJE_OS = Const.ENV_KEY_JANTJE_START + "OS_NAME";
    private static final String ENV_KEY_JANTJE_WORKSPACE_LOCATION = Const.ENV_KEY_JANTJE_START + "WORKSPACE_LOCATION";
    private static final String ENV_KEY_JANTJE_ECLIPSE_LOCATION = Const.ENV_KEY_JANTJE_START + "ECLIPSE_LOCATION";

    public static final String ENV_KEY_JANTJE_BOARDS_FILE = Const.ENV_KEY_JANTJE_START + "BOARDS_FILE";

    public static final String ENV_KEY_JANTJE_PACKAGE_ID = Const.ENV_KEY_JANTJE_START + "PACKAGE_ID";
    public static final String ENV_KEY_JANTJE_ARCITECTURE_ID = Const.ENV_KEY_JANTJE_START + "ARCHITECTURE_ID";
    public static final String ENV_KEY_JANTJE_BOARD_ID = Const.ENV_KEY_JANTJE_START + "BOARD_ID";
    public static final String ENV_KEY_SERIAL_PORT = Const.ERASE_START + "SERIAL.PORT";
    public static final String ENV_KEY_SERIAL_PORT_FILE = Const.ERASE_START + "SERIAL.PORT.FILE";
    private static final String ENV_KEY_BUILD_VARIANT_PATH = Const.ERASE_START + "BUILD.VARIANT.PATH";
    private static final String ENV_KEY_BUILD_ACTUAL_CORE_PATH = Const.ERASE_START + "BUILD.CORE.PATH";

    private static final String ENV_KEY_REFERENCED_CORE_PLATFORM_PATH = Const.ERASE_START + "REFERENCED.CORE.PATH";
    private static final String ENV_KEY_REFERENCED_VARIANT_PLATFORM_PATH = Const.ERASE_START
            + "REFERENCED.VARIANT.PATH";
    private static final String ENV_KEY_REFERENCED_UPLOAD_PLATFORM_PATH = Const.ERASE_START + "REFERENCED.UPLOAD.PATH";

    // preference nodes
    private static final String NODE_ARDUINO = Activator.NODE_ARDUINO;
    private static final String LIBRARY_PATH_SUFFIX = "libraries";
    private static final String JANTJE_ACTION_UPLOAD = "JANTJE.UPLOAD"; // this is actually the programmer
    private static final IEclipsePreferences myStorageNode = InstanceScope.INSTANCE.getNode(NODE_ARDUINO);
    private static final String TOOL = Messages.TOOL;
    private static final String BOARD = Messages.BOARD;
    private static final String FILE = Messages.FILE;

    /*
     * This is the basic info contained in the descriptor
     */
    private String myUploadPort;
    private String myProgrammer;
    private String myBoardID;
    private Map<String, String> myOptions;

    /*
     * the following data is stored to detect changes that will make the equal fail
     * so os changes, workspace changes, eclipse install changes will force a update
     * on the stored data
     */
    private String myProjectName = new String();
    private String myOSName = Platform.getOS();
    private String myWorkSpaceLocation = Common.getWorkspaceRoot().toString();
    private String myWorkEclipseLocation = ConfigurationPreferences.getEclipseHome().toString();

    /*
     * Stuff to make things work
     */
    private File myreferencingBoardsFile;
    protected TxtFile myTxtFile;
    private ChangeListener myChangeListener = null;

    private String myBoardsVariant;
    private IPath myReferencedBoardVariantPlatformPath;
    private String myBoardsCore;
    private IPath myReferencedCorePlatformPath;
    private IPath myReferencedUploadToolPlatformPath;
    private String myUploadTool;

    @Override
    public String toString() {
        return getReferencingBoardsFile() + " \"" + getBoardName() + "\" " + getUploadPort(); //$NON-NLS-2$
    }

    /**
     * Compare 2 descriptors and return true is they are equal. This method detects
     * - OS changes - project name changes - moves of workspace - changed runtine
     * eclipse install
     *
     * @param obj
     * @return true if equal otherwise false
     */
    public boolean equals(BoardDescriptor otherBoardDescriptor) {
        if (!this.getUploadPort().equals(otherBoardDescriptor.getUploadPort())) {
            return false;
        }
        if (!this.getProgrammer().equals(otherBoardDescriptor.getProgrammer())) {
            return false;
        }
        return !needsSettingDirty(otherBoardDescriptor);
    }

    /**
     * compare 2 board descriptors and return true if replacing one board descriptor
     * with the other implies that a rebuild is needed
     *
     * @param otherBoardDescriptor
     * @return
     */
    public boolean needsSettingDirty(BoardDescriptor otherBoardDescriptor) {

        if (!this.getBoardID().equals(otherBoardDescriptor.getBoardID())) {
            return true;
        }
        if (!this.getReferencingBoardsFile().equals(otherBoardDescriptor.getReferencingBoardsFile())) {
            return true;
        }
        if (!this.getOptions().equals(otherBoardDescriptor.getOptions())) {
            return true;
        }
        if (!this.getProjectName().equals(otherBoardDescriptor.getProjectName())) {
            return true;
        }
        if (!this.getMyOSName().equals(otherBoardDescriptor.getMyOSName())) {
            return true;
        }
        if (!this.getMyWorkEclipseLocation().equals(otherBoardDescriptor.getMyWorkEclipseLocation())) {
            return true;
        }
        if (!this.getMyWorkSpaceLocation().equals(otherBoardDescriptor.getMyWorkSpaceLocation())) {
            return true;
        }
        return false;
    }

    /*
     * Create a sketchProject. This class does not really create a sketch object.
     * Nor does it look for existing (mapping) sketch projects This class represents
     * the data passed between the UI and the core This class does contain a create
     * to create the project When confdesc is null the data will be taken from the
     * "last used " otherwise the data is taken from the project the confdesc
     * belongs to
     *
     */
    public static BoardDescriptor makeBoardDescriptor(ICConfigurationDescription confdesc) {
        return new InternalBoardDescriptor(confdesc);
    }

    /**
     * after construction data needs to be derived from other data. This method
     * derives all other data and puts it in fields
     */
    private void calculateDerivedFields() {

        myReferencedCorePlatformPath = getreferencingPlatformPath();
        myBoardsCore = null;
        myReferencedBoardVariantPlatformPath = myReferencedCorePlatformPath;
        myBoardsVariant = null;
        myReferencedUploadToolPlatformPath = myReferencedCorePlatformPath;
        myUploadTool = null;
        setDefaultOptions();
        // search in the board info
        Map<String, String> boardInfo = myTxtFile.getSection(getBoardID());
        ParseSection(boardInfo);

    }

    private void ParseSection(Map<String, String> boardInfo) {
        final String COLON = ":";
        if (boardInfo == null) {
            return; // there is a problem with the board ID
        }
        String core = boardInfo.get("build.core");
        String variant = boardInfo.get("build.variant");
        String upload = boardInfo.get("upload.tool");
        // also search the options
        for (Entry<String, String> curOption : this.myOptions.entrySet()) {
            String keyPrefix = "menu." + curOption.getKey() + '.' + curOption.getValue();
            String coreOption = boardInfo.get(keyPrefix + ".build.core");
            String variantOption = boardInfo.get(keyPrefix + ".build.variant");
            String uploadOption = boardInfo.get(keyPrefix + ".upload.tool");
            if (coreOption != null) {
                core = coreOption;
            }
            if (variantOption != null) {
                variant = variantOption;
            }
            if (uploadOption != null) {
                upload = uploadOption;
            }
        }
        String architecture = getArchitecture();
        if (core != null) {
            String valueSplit[] = core.split(":");
            if (valueSplit.length == 2) {
                String refVendor = valueSplit[0];
                String actualValue = valueSplit[1];
                myBoardsCore = actualValue;
                myReferencedCorePlatformPath = InternalPackageManager.getPlatformInstallPath(refVendor, architecture);
                if (this.myReferencedCorePlatformPath == null) {
                    Common.log(new Status(IStatus.ERROR, Const.CORE_PLUGIN_ID,
                            Messages.Helpers_tool_reference_missing.replaceAll(TOOL, core)
                                    .replaceAll(FILE, getReferencingBoardsFile().toString())
                                    .replaceAll(BOARD, getBoardID())));
                    return;
                }
            } else if (valueSplit.length == 4) {
                String refVendor = valueSplit[0];
                String refArchitecture = valueSplit[1];
                String refVersion = valueSplit[2];
                String actualValue = valueSplit[3];
                myBoardsCore = actualValue;
                myReferencedCorePlatformPath = InternalPackageManager.getPlatformInstallPath(refVendor,refArchitecture, refVersion);
                if (this.myReferencedCorePlatformPath == null) {
                    Common.log(new Status(IStatus.ERROR, Const.CORE_PLUGIN_ID,
                            Messages.Helpers_tool_reference_missing.replaceAll(TOOL, core)
                                    .replaceAll(FILE, getReferencingBoardsFile().toString())
                                    .replaceAll(BOARD, getBoardID())));
                    return;
                }
            } else {
                this.myBoardsCore = core;
            }
        }
        if (variant != null) {
            String valueSplit[] = variant.split(COLON);
            if (valueSplit.length == 2) {
                String refVendor = valueSplit[0];
                String actualValue = valueSplit[1];
                this.myBoardsVariant = actualValue;
                this.myReferencedBoardVariantPlatformPath = InternalPackageManager.getPlatformInstallPath(refVendor, architecture);
                if (this.myReferencedBoardVariantPlatformPath == null) {
                    Common.log(new Status(IStatus.ERROR, Const.CORE_PLUGIN_ID,
                            Messages.Helpers_tool_reference_missing.replaceAll(TOOL, variant)
                                    .replaceAll(FILE, getReferencingBoardsFile().toString())
                                    .replaceAll(BOARD, getBoardID())));
                    return;
                }
            } else if (valueSplit.length == 4) {
                String refVendor = valueSplit[0];
                String refArchitecture = valueSplit[1];
                String refVersion = valueSplit[2];
                String actualValue = valueSplit[3];
                this.myBoardsVariant = actualValue;
                if ("*".equals(refVersion)) {
                    this.myReferencedBoardVariantPlatformPath = InternalPackageManager.getPlatformInstallPath(refVendor, refArchitecture);
                } else {
                    this.myReferencedBoardVariantPlatformPath = InternalPackageManager.getPlatformInstallPath(refVendor, refArchitecture,
                            refVersion);
                }
                if (this.myReferencedBoardVariantPlatformPath == null) {
                    Common.log(new Status(IStatus.ERROR, Const.CORE_PLUGIN_ID,
                            Messages.Helpers_tool_reference_missing.replaceAll(TOOL, variant)
                                    .replaceAll(FILE, getReferencingBoardsFile().toString())
                                    .replaceAll(BOARD, getBoardID())));
                    return;
                }
            } else {
                myBoardsVariant = variant;
            }
        }
        if (upload != null) {
            String valueSplit[] = upload.split(COLON);
            if (valueSplit.length == 2) {
                String refVendor = valueSplit[0];
                String actualValue = valueSplit[1];
                this.myUploadTool = actualValue;
                this.myReferencedUploadToolPlatformPath = InternalPackageManager.getPlatformInstallPath(refVendor, architecture);
                if (this.myReferencedUploadToolPlatformPath == null) {
                    Common.log(new Status(IStatus.ERROR, Const.CORE_PLUGIN_ID,
                            Messages.Helpers_tool_reference_missing.replaceAll(TOOL, upload)
                                    .replaceAll(FILE, getReferencingBoardsFile().toString())
                                    .replaceAll(BOARD, getBoardID())));
                    return;
                }
            } else if (valueSplit.length == 4) {
                String refVendor = valueSplit[0];
                String refArchitecture = valueSplit[1];
                String refVersion = valueSplit[2];
                String actualValue = valueSplit[3];
                this.myUploadTool = actualValue;
                this.myReferencedUploadToolPlatformPath = InternalPackageManager.getPlatformInstallPath(refVendor, refArchitecture,
                        refVersion);
                if (this.myReferencedUploadToolPlatformPath == null) {
                    Common.log(new Status(IStatus.ERROR, Const.CORE_PLUGIN_ID,
                            Messages.Helpers_tool_reference_missing.replaceAll(TOOL, upload)
                                    .replaceAll(FILE, getReferencingBoardsFile().toString())
                                    .replaceAll(BOARD, getBoardID())));
                    return;
                }
            } else {
                myUploadTool = upload;
            }
        }

    }

    protected BoardDescriptor(ICConfigurationDescription confdesc) {
        if (confdesc == null) {
            this.myreferencingBoardsFile = new File(myStorageNode.get(KEY_LAST_USED_BOARDS_FILE, ""));
            this.myTxtFile = new TxtFile(this.myreferencingBoardsFile,true);
            this.myBoardID = myStorageNode.get(KEY_LAST_USED_BOARD, "");
            this.myUploadPort = myStorageNode.get(KEY_LAST_USED_UPLOAD_PORT, "");
            this.myProgrammer = myStorageNode.get(KEY_LAST_USED_UPLOAD_PROTOCOL, Defaults.getDefaultUploadProtocol());
            this.myOptions = KeyValue.makeMap(myStorageNode.get(KEY_LAST_USED_BOARD_MENU_OPTIONS, new String()));

        } else {
            this.myUploadPort = Common.getBuildEnvironmentVariable(confdesc, ENV_KEY_JANTJE_UPLOAD_PORT, "");
            this.myProgrammer = Common.getBuildEnvironmentVariable(confdesc, JANTJE_ACTION_UPLOAD, "");
            this.myreferencingBoardsFile = new File(
                    Common.getBuildEnvironmentVariable(confdesc, ENV_KEY_JANTJE_BOARDS_FILE, ""));
            this.myBoardID = Common.getBuildEnvironmentVariable(confdesc, ENV_KEY_JANTJE_BOARD_ID, "");
            this.myProjectName = Common.getBuildEnvironmentVariable(confdesc, ENV_KEY_JANTJE_PROJECT_NAME, "");
            this.myTxtFile = new TxtFile(this.myreferencingBoardsFile,true);
            this.myOSName = Common.getBuildEnvironmentVariable(confdesc, ENV_KEY_JANTJE_OS, "");
            this.myWorkSpaceLocation = Common.getBuildEnvironmentVariable(confdesc, ENV_KEY_JANTJE_WORKSPACE_LOCATION,
                    "");
            this.myWorkEclipseLocation = Common.getBuildEnvironmentVariable(confdesc, ENV_KEY_JANTJE_ECLIPSE_LOCATION,
                    "");
            String optinconcat = Common.getBuildEnvironmentVariable(confdesc, ENV_KEY_JANTJE_MENU_SELECTION, "");
            this.myOptions = KeyValue.makeMap(optinconcat);
        }
        calculateDerivedFields();
    }

    public static BoardDescriptor makeBoardDescriptor(File boardsFile, String boardID, Map<String, String> options) {
        return new InternalBoardDescriptor(boardsFile, boardID, options);
    }

    /**
     * make a board descriptor for each board in the board.txt file with the default
     * options
     *
     * @param boardFile
     * @return a list of board descriptors
     */
    public static List<BoardDescriptor> makeBoardDescriptors(File boardFile, Map<String, String> options) {
        TxtFile txtFile = new TxtFile(boardFile,true);
        List<BoardDescriptor> boards = new ArrayList<>();
        for (String curboardName : txtFile.getAllNames()) {
            Map<String, String> boardSection = txtFile.getSection(txtFile.getBoardIDFromBoardName(curboardName));
            if (boardSection != null) {
                if (!"true".equalsIgnoreCase(boardSection.get("hide"))) {
                    boards.add(makeBoardDescriptor(boardFile, txtFile.getBoardIDFromBoardName(curboardName), options));
                }
            }
        }
        return boards;
    }

    /**
     * create a board descriptor
     *
     * @param boardsFile
     * @param boardID
     * @param options
     *            if null default options are taken
     */
    protected BoardDescriptor(File boardsFile, String boardID, Map<String, String> options) {
        this.myUploadPort = new String();
        this.myProgrammer = Defaults.getDefaultUploadProtocol();
        this.myBoardID = boardID;
        this.myOptions = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        this.myreferencingBoardsFile = boardsFile;
        this.myTxtFile = new TxtFile(this.myreferencingBoardsFile,true);
        setDefaultOptions();
        if (options != null) {
            this.myOptions.putAll(options);
        }
        calculateDerivedFields();

    }

    protected BoardDescriptor(TxtFile txtFile, String boardID) {
        this.myUploadPort = new String();
        this.myProgrammer = Defaults.getDefaultUploadProtocol();
        this.myBoardID = boardID;
        this.myOptions = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        this.myreferencingBoardsFile = txtFile.getTxtFile();
        this.myTxtFile = txtFile;
        setDefaultOptions();

        calculateDerivedFields();
    }

    public static BoardDescriptor makeBoardDescriptor(BoardDescriptor sourceBoardDescriptor) {
        return new InternalBoardDescriptor(sourceBoardDescriptor);
    }

    protected BoardDescriptor(BoardDescriptor sourceBoardDescriptor) {

        this.myUploadPort = sourceBoardDescriptor.getUploadPort();
        this.myProgrammer = sourceBoardDescriptor.getProgrammer();
        this.myBoardID = sourceBoardDescriptor.getBoardID();
        this.myOptions = sourceBoardDescriptor.getOptions();
        this.myProjectName = sourceBoardDescriptor.getProjectName();
        this.myreferencingBoardsFile = sourceBoardDescriptor.getReferencingBoardsFile();
        this.myTxtFile = sourceBoardDescriptor.myTxtFile;
        this.myChangeListener = null;

        this.myBoardsVariant = sourceBoardDescriptor.getBoardVariant();
        this.myReferencedBoardVariantPlatformPath = sourceBoardDescriptor.getReferencedVariantPlatformPath();
        this.myBoardsCore = sourceBoardDescriptor.getBoardsCore();
        this.myReferencedCorePlatformPath = sourceBoardDescriptor.getReferencedCorePlatformPath();
        this.myReferencedUploadToolPlatformPath = sourceBoardDescriptor.getReferencedUploadPlatformPath();
        this.myUploadTool = sourceBoardDescriptor.getuploadTool();
    }

    public String getuploadTool() {
        return this.myUploadTool;
    }

    private String getBoardsCore() {
        return this.myBoardsCore;
    }

    /*
	 * Sets default options as follows
	 * If no option is specified take the first one
     * if a option is specified but the value is invalid take the first one
     *
	 * this is so because I want to provide a list of options
	 * but if the options are incomplete or invalid
	 * this method still returns a complete and valid set.
     */
    private void setDefaultOptions() {

        TreeMap<String, String> allMenuIDs = this.myTxtFile.getMenus();
        for (Map.Entry<String, String> curMenuID : allMenuIDs.entrySet()) {
            String providedMenuValue = this.myOptions.get(curMenuID.getKey());
            ArrayList<String> menuOptions = this.myTxtFile.getMenuItemIDsFromMenuID(curMenuID.getKey(), getBoardID());
            if (menuOptions.size() > 0) {
              if (providedMenuValue == null) {
        
                    this.myOptions.put(curMenuID.getKey(), menuOptions.get(0));
                }
            else if (!menuOptions.contains(providedMenuValue)) {
                    this.myOptions.put(curMenuID.getKey(), menuOptions.get(0));
                }
            }
        }
    }

    /**
     * tries to set the project to the boarddescriptor
     *
     * @param project
     * @param monitor
     * @return true if success false if failed
     */
    public boolean configureProject(IProject project, IProgressMonitor monitor) {
        ICProjectDescription prjCDesc = CoreModel.getDefault().getProjectDescription(project);
        ICConfigurationDescription configurationDescription = prjCDesc.getActiveConfiguration();
        try {
            save(configurationDescription);
            // prjCDesc.setActiveConfiguration(configurationDescription);
            CoreModel.getDefault().getProjectDescriptionManager().setProjectDescription(project, prjCDesc, true, null);
        } catch (Exception e) {
            e.printStackTrace();
            Common.log(new Status(IStatus.ERROR, io.sloeber.core.Activator.getId(), "failed to save the board settings",
                    e));
            return false;
        }
        return true;
    }
    
	private static void setDefaultLanguageSettingsProviders(IProject project, ConfigurationDescriptor cfgDes,
			IConfiguration cfg, ICConfigurationDescription cfgDescription) {
		// propagate the preference to project properties
		boolean isPreferenceEnabled = ScannerDiscoveryLegacySupport
				.isLanguageSettingsProvidersFunctionalityEnabled(null);
		ScannerDiscoveryLegacySupport.setLanguageSettingsProvidersFunctionalityEnabled(project, isPreferenceEnabled);

		if (cfgDescription instanceof ILanguageSettingsProvidersKeeper) {
			ILanguageSettingsProvidersKeeper lspk = (ILanguageSettingsProvidersKeeper) cfgDescription;

			lspk.setDefaultLanguageSettingsProvidersIds(new String[] { cfgDes.ToolchainID });

			List<ILanguageSettingsProvider> providers = getDefaultLanguageSettingsProviders(cfg, cfgDescription);
			lspk.setLanguageSettingProviders(providers);
		}
	}
	private static List<ILanguageSettingsProvider> getDefaultLanguageSettingsProviders(IConfiguration cfg,
			ICConfigurationDescription cfgDescription) {
		List<ILanguageSettingsProvider> providers = new ArrayList<>();
		String[] ids = cfg != null ? cfg.getDefaultLanguageSettingsProviderIds() : null;

		if (ids == null) {
			// Try with legacy providers
			ids = ScannerDiscoveryLegacySupport.getDefaultProviderIdsLegacy(cfgDescription);
		}

		if (ids != null) {
			for (String id : ids) {
				ILanguageSettingsProvider provider = null;
				if (!LanguageSettingsManager.isPreferShared(id)) {
					provider = LanguageSettingsManager.getExtensionProviderCopy(id, false);
				}
				if (provider == null) {
					provider = LanguageSettingsManager.getWorkspaceProvider(id);
				}
				providers.add(provider);
			}
		}

		return providers;
	}
	
	////////////////////////////////////copied in from cdt testts
	private final static IProgressMonitor NULL_MONITOR = new NullProgressMonitor();
	private static void waitForProjectRefreshToFinish() {
		try {
			// CDT opens the Project with BACKGROUND_REFRESH enabled which causes the
			// refresh manager to refresh the project 200ms later.  This Job interferes
			// with the resource change handler firing see: bug 271264
			Job.getJobManager().join(ResourcesPlugin.FAMILY_AUTO_REFRESH, null);
		} catch (Exception e) {
			// Ignore
		}
	}


	/**
	 * Creates CDT project in a specific path in workspace adding specified configurations and opens it.
	 *
	 * @param projectName - project name.
	 * @param pathInWorkspace - path relative to workspace root.
	 * @param configurationIds - array of configuration IDs.
	 * @return - new {@link IProject}.
	 * @throws CoreException - if the project can't be created.
	 * @throws OperationCanceledException...
	 */
	public static IProject createCDTProject(String projectName, String pathInWorkspace, String[] configurationIds) throws OperationCanceledException, CoreException {
		CCorePlugin cdtCorePlugin = CCorePlugin.getDefault();
		IWorkspace workspace = ResourcesPlugin.getWorkspace();
		IWorkspaceRoot root = workspace.getRoot();

		IProject project = root.getProject(projectName);
		IndexerPreferences.set(project, IndexerPreferences.KEY_INDEXER_ID, IPDOMManager.ID_NO_INDEXER);

		IProjectDescription prjDescription = workspace.newProjectDescription(projectName);
		if(pathInWorkspace != null) {
			IPath absoluteLocation = root.getLocation().append(pathInWorkspace);
			prjDescription.setLocation(absoluteLocation);
		}

		if (configurationIds != null && configurationIds.length > 0) {
			ICProjectDescriptionManager prjDescManager = cdtCorePlugin.getProjectDescriptionManager();

			project.create(NULL_MONITOR);
			project.open(NULL_MONITOR);

			ICProjectDescription icPrjDescription = prjDescManager.createProjectDescription(project, false);
			ICConfigurationDescription baseConfiguration = cdtCorePlugin.getPreferenceConfiguration("");//TestCfgDataProvider.PROVIDER_ID);

			for (String cfgId : configurationIds) {
				icPrjDescription.createConfiguration(cfgId, cfgId + " Name", baseConfiguration);
			}
			prjDescManager.setProjectDescription(project, icPrjDescription);
		}
		project = cdtCorePlugin.createCDTProject(prjDescription, project, NULL_MONITOR);
		waitForProjectRefreshToFinish();


		project.open(null);


		return project;
	}
	public static IProject  tt(String projectName) throws Exception{
		CoreModel coreModel = CoreModel.getDefault();
		// Create model project and accompanied descriptions
		IProject project = BuildSystemTestHelper_createProject(projectName,(IPath)null);
		ICProjectDescription des = coreModel.createProjectDescription(project, false);
		//Assert.assertNotNull("createDescription returned null!", des);

		{
			ManagedBuildInfo info = ManagedBuildManager.createBuildInfo(project);
			IProjectType type = ManagedBuildManager.getProjectType("");//pluginProjectTypeId);
			//Assert.assertNotNull("project type not found", type);

			ManagedProject mProj = new ManagedProject(project, type);
			info.setManagedProject(mProj);

			IConfiguration cfgs[] = type.getConfigurations();
			//Assert.assertNotNull("configurations not found", cfgs);
			//Assert.assertTrue("no configurations found in the project type",cfgs.length>0);

			for (IConfiguration configuration : cfgs) {
				String id = ManagedBuildManager.calculateChildId(configuration.getId(), null);
				Configuration config = new Configuration(mProj, (Configuration)configuration, id, false, true, false);
				CConfigurationData data = config.getConfigurationData();
				//Assert.assertNotNull("data is null for created configuration", data);
				ICConfigurationDescription cfgDes = des.createConfiguration(ManagedBuildManager.CFG_DATA_PROVIDER_ID, data);
			}
			//Assert.assertEquals(2, des.getConfigurations().length);
		}

		// Persist the project
		coreModel.setProjectDescription(project, des);
		//Jaba removed line ResourceHelper.joinIndexerBeforeCleanup(getName());
		project.close(null);
		return project;
	}

	static public IProject BuildSystemTestHelper_createProject(
			final String name,
			final IPath location) throws CoreException{
		IWorkspace workspace = ResourcesPlugin.getWorkspace();
		IWorkspaceRoot root = workspace.getRoot();
		final IProject newProjectHandle = root.getProject(name);
		IProject project = null;

		if (!newProjectHandle.exists()) {
			IWorkspaceDescription workspaceDesc = workspace.getDescription();
			workspaceDesc.setAutoBuilding(false);
			workspace.setDescription(workspaceDesc);
			IProjectDescription description = workspace.newProjectDescription(newProjectHandle.getName());
			if(location != null)
				description.setLocation(location);
			//description.setLocation(root.getLocation());
			project = CCorePlugin.getDefault().createCDTProject(description, newProjectHandle, new NullProgressMonitor());
		} else {
			IWorkspaceRunnable runnable = new IWorkspaceRunnable() {
				@Override
				public void run(IProgressMonitor monitor) throws CoreException {
					newProjectHandle.refreshLocal(IResource.DEPTH_INFINITE, monitor);
				}
			};
			NullProgressMonitor monitor = new NullProgressMonitor();
			workspace.run(runnable, root, IWorkspace.AVOID_UPDATE, monitor);
			project = newProjectHandle;
		}

		// Open the project if we have to
		if (!project.isOpen()) {
			project.open(new NullProgressMonitor());
		}

		return project;
	}
	static public IProject createNewManagedProject(IProject newProjectHandle,
			final String name,
			final IPath location,
			final String projectId,
			final String projectTypeId) throws CoreException {
		final IWorkspace workspace = ResourcesPlugin.getWorkspace();
		IWorkspaceRoot root = workspace.getRoot();
		final IProject project = newProjectHandle;
		IWorkspaceRunnable runnable = new IWorkspaceRunnable() {
			@Override
			public void run(IProgressMonitor monitor) throws CoreException {
				// Create the base project
				IWorkspaceDescription workspaceDesc = workspace.getDescription();
				workspaceDesc.setAutoBuilding(false);
				workspace.setDescription(workspaceDesc);
				IProjectDescription description = workspace.newProjectDescription(project.getName());
				if (location != null) {
					description.setLocation(location);
				}
				CCorePlugin.getDefault().createCProject(description, project, new NullProgressMonitor(), projectId);
				// Add the managed build nature and builder
				addManagedBuildNature(project);

				// Find the base project type definition
				IProjectType projType = ManagedBuildManager.getProjectType(projectTypeId);
				//Assert.assertNotNull(projType);

				// Create the managed-project (.cdtbuild) for our project that builds an executable.
				IManagedProject newProject = null;
				try {
					newProject = ManagedBuildManager.createManagedProject(project, projType);
				} catch (Exception e) {
					//Assert.fail("Failed to create managed project for: " + project.getName());
					return;
				}
//				Assert.assertEquals(newProject.getName(), projType.getName());
//				Assert.assertFalse(newProject.equals(projType));
				ManagedBuildManager.setNewProjectVersion(project);
				// Copy over the configs
				IConfiguration defaultConfig = null;
				IConfiguration[] configs = projType.getConfigurations();
				for (int i = 0; i < configs.length; ++i) {
					// Make the first configuration the default
					if (i == 0) {
						defaultConfig = newProject.createConfiguration(configs[i], projType.getId() + "." + i);
					} else {
						newProject.createConfiguration(configs[i], projType.getId() + "." + i);
					}
				}
				ManagedBuildManager.setDefaultConfiguration(project, defaultConfig);

				IConfiguration cfgs[] = newProject.getConfigurations();
				for(int i = 0; i < cfgs.length; i++){
					cfgs[i].setArtifactName(newProject.getDefaultArtifactName());
				}

				ManagedBuildManager.getBuildInfo(project).setValid(true);
			}
		};
		NullProgressMonitor monitor = new NullProgressMonitor();
		try {
			workspace.run(runnable, root, IWorkspace.AVOID_UPDATE, monitor);
		} catch (CoreException e2) {
			//Assert.fail(e2.getLocalizedMessage());
		}
		// CDT opens the Project with BACKGROUND_REFRESH enabled which causes the
		// refresh manager to refresh the project 200ms later.  This Job interferes
		// with the resource change handler firing see: bug 271264
		try {
			// CDT opens the Project with BACKGROUND_REFRESH enabled which causes the
			// refresh manager to refresh the project 200ms later.  This Job interferes
			// with the resource change handler firing see: bug 271264
			Job.getJobManager().join(ResourcesPlugin.FAMILY_AUTO_REFRESH, null);
		} catch (Exception e) {
			// Ignore
		}

		// Initialize the path entry container
		IStatus initResult = ManagedBuildManager.initBuildInfoContainer(project);
		if (initResult.getCode() != IStatus.OK) {
			//Assert.fail("Initializing build information failed for: " + project.getName() + " because: " + initResult.getMessage());
		}
		return project;
	}
	static public void addManagedBuildNature (IProject project) {
		// Create the buildinformation object for the project
		IManagedBuildInfo info = ManagedBuildManager.createBuildInfo(project);
		//Assert.assertNotNull(info);
//		info.setValid(true);

		// Add the managed build nature
		try {
			ManagedCProjectNature.addManagedNature(project, new NullProgressMonitor());
			ManagedCProjectNature.addManagedBuilder(project, new NullProgressMonitor());
		} catch (CoreException e) {
			//Assert.fail("Test failed on adding managed build nature or builder: " + e.getLocalizedMessage());
		}

		// Associate the project with the managed builder so the clients can get proper information
		ICDescriptor desc = null;
		try {
			desc = CCorePlugin.getDefault().getCProjectDescription(project, true);
			desc.remove(CCorePlugin.BUILD_SCANNER_INFO_UNIQ_ID);
			desc.create(CCorePlugin.BUILD_SCANNER_INFO_UNIQ_ID, ManagedBuildManager.INTERFACE_IDENTITY);
		} catch (CoreException e) {
			//Assert.fail("Test failed on adding managed builder as scanner info provider: " + e.getLocalizedMessage());
			return;
		}
		try {
			desc.saveProjectData();
		} catch (CoreException e) {
			//Assert.fail("Test failed on saving the ICDescriptor data: " + e.getLocalizedMessage());		}
	}
		}


///end of copied in from cdt tests
	

    /*
     * Method to create a project based on the board
     */
    public IProject createProject(String projectName, URI projectURI,
            CodeDescriptor codeDescription, CompileOptions compileOptions, IProgressMonitor monitor) throws Exception {

    	CCorePlugin cCorePlugin=CCorePlugin.getDefault();
    	IWorkspace workspace = ResourcesPlugin.getWorkspace();
        IProject projectHandle = workspace.getRoot().getProject(Common.MakeNameCompileSafe(projectName));
        
//        
//        
//        ICProjectDescriptionManager mngr = CoreModel.getDefault().getProjectDescriptionManager();
//		ICProjectDescription des = mngr.createProjectDescription(projectHandle, false, true);
//		ManagedBuildInfo info = ManagedBuildManager.createBuildInfo(projectHandle);
//		ManagedProject mProj = new ManagedProject(des);
//		info.setManagedProject(mProj);
////		monitor.worked(20);
//
//		// Iterate across the configurations
//		ArrayList<ConfigurationDescriptor> cfgNamesAndTCIds=ConfigurationDescriptor.getDefaultDescriptors();
//		for (ConfigurationDescriptor curConfDesc : cfgNamesAndTCIds) {
//			IToolChain tcs = ManagedBuildManager.getExtensionToolChain(curConfDesc.ToolchainID);
//
//			Configuration cfg = new Configuration(mProj, tcs,
//					ManagedBuildManager.calculateChildId(curConfDesc.ToolchainID, null), curConfDesc.configName);
//
//				cfg.setParallelDef(compileOptions.isParallelBuildEnabled());
//
//			IBuilder bld = cfg.getEditableBuilder();
//			if (bld != null) {
//				bld.setManagedBuildOn(true);
//				cfg.setArtifactName("${ProjName}"); //$NON-NLS-1$
//			} else {
//				System.out.println("Messages.StdProjectTypeHandler_3"); //$NON-NLS-1$
//			}
//			CConfigurationData data = cfg.getConfigurationData();
//			ICConfigurationDescription cfgDes = des.createConfiguration(ManagedBuildManager.CFG_DATA_PROVIDER_ID, data);
//
//			setDefaultLanguageSettingsProviders(projectHandle, curConfDesc, cfg, cfgDes);
//		}
//
//		

        
        final IProjectDescription desc = workspace.newProjectDescription(projectHandle.getName());
        desc.setLocationURI(projectURI);
        

//        projectHandle.create(desc, monitor);
//
//        if (monitor.isCanceled()) {
//            throw new OperationCanceledException();
//        }

        

// 
//        ;
//        ICProjectDescription prjCDesc = ShouldHaveBeenInCDT.setCProjectDescription(projectHandle, cfgNamesAndTCIds,
//                 compileOptions.isParallelBuildEnabled(), monitor);

        // Add the C C++ AVR and other needed Natures to the project
        Helpers.addTheNatures(desc);
        projectHandle=cCorePlugin.createCDTProject(desc, projectHandle, monitor);
        ICProjectDescription prjCDesc=cCorePlugin.getProjectDescription(projectHandle);
//    	ConfigurationDescriptor cfgTCidPair = new ConfigurationDescriptor("Release", //$NON-NLS-1$
//    			"io.sloeber.core.toolChain.release", false); //$NON-NLS-1$
  
        IConfiguration. .createToolChain();
        CConfigurationData data= new CConfigurationData();
        prjCDesc.createConfiguration("io.sloeber.core.toolChain.release", data);
//        IBuilder arduinoBuilder = ManagedBuildManager.getExtensionBuilder("io.sloeber.core.toolChain.release");
//        prjCDesc.createConfiguration(arduinoBuilder, data)

        // Add the Arduino folder

        Helpers.createNewFolder(projectHandle, Const.ARDUINO_CODE_FOLDER_NAME, null);

        for (ICConfigurationDescription curConfig : prjCDesc.getConfigurations()) {
            compileOptions.save(curConfig);
            save(curConfig);
        }

        // Set the path variables
        // ArduinoHelpers.setProjectPathVariables(prjCDesc.getActiveConfiguration());

        // Intermediately save or the adding code will fail
        // Release is the active config (as that is the "IDE" Arduino
        // type....)
//        ICConfigurationDescription defaultConfigDescription = prjCDesc
//                .getConfigurationByName(cfgNamesAndTCIds.get(0).configName);        
//        ICResourceDescription cfgd = defaultConfigDescription.getResourceDescription(new Path(new String()), true);
        ICConfigurationDescription activeConfig = prjCDesc.getActiveConfiguration();
 

        ICExclusionPatternPathEntry[] entries = activeConfig.getSourceEntries();
        if (entries.length == 1) {
            Path exclusionPath[] = new Path[6];
            exclusionPath[0] = new Path(LIBRARY_PATH_SUFFIX + "/?*/**/?xamples/**");
            exclusionPath[1] = new Path(LIBRARY_PATH_SUFFIX + "/?*/**/?xtras/**");
            exclusionPath[2] = new Path(LIBRARY_PATH_SUFFIX + "/?*/**/test*/**");
            exclusionPath[3] = new Path(LIBRARY_PATH_SUFFIX + "/?*/**/third-party/**");
            exclusionPath[4] = new Path(LIBRARY_PATH_SUFFIX + "/**/._*");
            exclusionPath[5] = new Path(LIBRARY_PATH_SUFFIX + "/?*/utility/*/*");

            ICExclusionPatternPathEntry newSourceEntry = new CSourceEntry(entries[0].getFullPath(), exclusionPath,
                    ICSettingEntry.VALUE_WORKSPACE_PATH);
            ICSourceEntry[] out = null;
            out = new ICSourceEntry[1];
            out[0] = (ICSourceEntry) newSourceEntry;
            try {
            	activeConfig.setSourceEntries(out);
            } catch (@SuppressWarnings("unused") CoreException e) {
                // ignore
            }

        } else {
            // this should not happen
        }
        Set<String> librariesToInstall = codeDescription.createFiles(projectHandle, monitor);
        IPath linkedPath = codeDescription.getLinkedExamplePath();
        if (linkedPath != null) {
            Helpers.addIncludeFolder(activeConfig.getRootFolderDescription(), linkedPath, false);
        }
        Libraries.addLibrariesToProject(projectHandle, activeConfig, librariesToInstall);

        prjCDesc.setCdtProjectCreated();
        CoreModel.getDefault().getProjectDescriptionManager().setProjectDescription(projectHandle, prjCDesc, true,
                null);
  //      projectHandle.setDescription(desc, new NullProgressMonitor());
        projectHandle.open(IResource.BACKGROUND_REFRESH, monitor);
        projectHandle.refreshLocal(IResource.DEPTH_INFINITE, null);
        monitor.done();
        return projectHandle;
    }

    public void save(ICConfigurationDescription confdesc) throws Exception {
        boolean needsSettingDirty = saveConfiguration(confdesc, null);

        if (confdesc != null) {
            IProject project = confdesc.getProjectDescription().getProject();

            Helpers.setTheEnvironmentVariables(project, confdesc, (InternalBoardDescriptor) this);

            Helpers.addArduinoCodeToProject(this, project, confdesc);

            needsSettingDirty = needsSettingDirty || Helpers.removeInvalidIncludeFolders(confdesc);
            if (needsSettingDirty) {
                Helpers.setDirtyFlag(project, confdesc);
            } else {
                Common.log(new Status(IStatus.INFO, io.sloeber.core.Activator.getId(),
                        "Ignoring update; clean may be required: " + project.getName()));
            }
        }
    }

    public void saveConfiguration() {
        saveConfiguration(null, null);
    }

    public boolean saveConfiguration(ICConfigurationDescription confDesc, IContributedEnvironment contribEnvIn) {
        boolean needsSettingDirty = false;
        if (confDesc != null) {
            if (getActualCoreCodePath() == null) {
                // don't change stuff if the setup is wonky
                return false;
            }
            BoardDescriptor curBoardDesCriptor = makeBoardDescriptor(confDesc);
            needsSettingDirty = curBoardDesCriptor.needsSettingDirty(this);
            IContributedEnvironment contribEnv = contribEnvIn;
            if (contribEnv == null) {
                IEnvironmentVariableManager envManager = CCorePlugin.getDefault().getBuildEnvironmentManager();
                contribEnv = envManager.getContributedEnvironment();
            }
            Common.setBuildEnvironmentVariable(contribEnv, confDesc, "JANTJE.SELECTED.PLATFORM",
                    getreferencingPlatformPath().toString());
            Common.setBuildEnvironmentVariable(contribEnv, confDesc, ENV_KEY_JANTJE_BOARD_NAME, getBoardName());
            Common.setBuildEnvironmentVariable(contribEnv, confDesc, ENV_KEY_JANTJE_BOARDS_FILE,
                    getReferencingBoardsFile().toString());
            Common.setBuildEnvironmentVariable(contribEnv, confDesc, ENV_KEY_JANTJE_BOARD_ID, this.myBoardID);
            Common.setBuildEnvironmentVariable(contribEnv, confDesc, ENV_KEY_JANTJE_ARCITECTURE_ID, getArchitecture());
            Common.setBuildEnvironmentVariable(contribEnv, confDesc, ENV_KEY_JANTJE_PACKAGE_ID, getPackage());
            Common.setBuildEnvironmentVariable(contribEnv, confDesc, ENV_KEY_JANTJE_UPLOAD_PORT, this.myUploadPort);
            Common.setBuildEnvironmentVariable(contribEnv, confDesc, ENV_KEY_JANTJE_PROJECT_NAME,
                    confDesc.getProjectDescription().getProject().getName());
            Common.setBuildEnvironmentVariable(contribEnv, confDesc, ENV_KEY_JANTJE_OS, this.myOSName);
            Common.setBuildEnvironmentVariable(contribEnv, confDesc, ENV_KEY_JANTJE_WORKSPACE_LOCATION,
                    this.myWorkSpaceLocation);
            Common.setBuildEnvironmentVariable(contribEnv, confDesc, ENV_KEY_JANTJE_ECLIPSE_LOCATION,
                    this.myWorkEclipseLocation);
            Common.setBuildEnvironmentVariable(confDesc, JANTJE_ACTION_UPLOAD, this.myProgrammer);
            String value = KeyValue.makeString(this.myOptions);
            Common.setBuildEnvironmentVariable(contribEnv, confDesc, ENV_KEY_JANTJE_MENU_SELECTION, value);

            Common.setBuildEnvironmentVariable(contribEnv, confDesc, ENV_KEY_SERIAL_PORT, getActualUploadPort());
            Common.setBuildEnvironmentVariable(contribEnv, confDesc, ENV_KEY_SERIAL_PORT_FILE,
                    getActualUploadPort().replace("/dev/", new String()));
			Common.setBuildEnvironmentVariable(contribEnv, confDesc, ENV_KEY_BUILD_ACTUAL_CORE_PATH, getActualCoreCodePath().toOSString());
            IPath variantPath = getActualVariantPath();
            if (variantPath != null) {
                Common.setBuildEnvironmentVariable(contribEnv, confDesc, ENV_KEY_BUILD_VARIANT_PATH,
                        variantPath.toOSString());
            } else {// teensy does not use variants
                Common.setBuildEnvironmentVariable(contribEnv, confDesc, ENV_KEY_BUILD_VARIANT_PATH, new String());
            }

            // the entries below are only saved for special platforms that heavily rely on
            // referencing such as jantjes hardware

            Common.setBuildEnvironmentVariable(contribEnv, confDesc, ENV_KEY_REFERENCED_CORE_PLATFORM_PATH,
                    getReferencedCorePlatformPath().toOSString());
            Common.setBuildEnvironmentVariable(contribEnv, confDesc, ENV_KEY_REFERENCED_VARIANT_PLATFORM_PATH,
                    getReferencedVariantPlatformPath().toOSString());
            Common.setBuildEnvironmentVariable(contribEnv, confDesc, ENV_KEY_REFERENCED_UPLOAD_PLATFORM_PATH,
                    getReferencedUploadPlatformPath().toOSString());

        }

        // Also save last used values
        myStorageNode.put(KEY_LAST_USED_BOARDS_FILE, getReferencingBoardsFile().toString());
        myStorageNode.put(KEY_LAST_USED_BOARD, this.myBoardID);
        myStorageNode.put(KEY_LAST_USED_UPLOAD_PORT, this.myUploadPort);
        myStorageNode.put(KEY_LAST_USED_UPLOAD_PROTOCOL, this.myProgrammer);
        myStorageNode.put(KEY_LAST_USED_BOARD_MENU_OPTIONS, KeyValue.makeString(this.myOptions));
        return needsSettingDirty;
    }

    public String getPackage() {
        return this.myTxtFile.getPackage();
    }

    public String getArchitecture() {
        return this.myTxtFile.getArchitecture();
    }

    public File getReferencingBoardsFile() {
        return this.myreferencingBoardsFile;
    }

    public String getBoardName() {
        return this.myTxtFile.getNameFromID(this.myBoardID);
    }

    public String getUploadPort() {
        return this.myUploadPort;
    }

    /**
	 * return the actual adress en,coded in the upload port
	 * example
	 * uploadport com4 returns com4
	 * uploadport = arduino.local at 199.25.25.1 returns arduino.local
     * 
     * @return
     */
    public String getActualUploadPort() {
        return myUploadPort.split(" ")[0];
    }

    public String getProgrammer() {
        return this.myProgrammer;
    }

    /**
	 * Set the upload port like in the gui.
	 * The upload port can be a comport or a networkadress space and something else
	 * note that getuploadport returns the before space part of this method
     * @param newUploadPort
     */
    public void setUploadPort(String newUploadPort) {
        this.myUploadPort = newUploadPort;
    }

    public void setUploadProtocol(String newUploadProtocol) {
        this.myProgrammer = newUploadProtocol;

    }

    public void setBoardID(String boardID) {
        if (!boardID.equals(this.myBoardID)) {
            this.myBoardID = boardID;
            informChangeListeners();
        }
    }

    public void setBoardName(String boardName) {
        String newBoardID = this.myTxtFile.getBoardIDFromBoardName(boardName);
        if ((newBoardID == null || this.myBoardID.equals(newBoardID))) {
            return;
        }

        this.myBoardID = newBoardID;
        informChangeListeners();

    }

    public void setreferencingBoardsFile(File boardsFile) {
        if (boardsFile == null) {
            return;// ignore
        }
        if (this.myreferencingBoardsFile.equals(boardsFile)) {
            return;
        }

        this.myreferencingBoardsFile = boardsFile;
        this.myTxtFile = new TxtFile(this.myreferencingBoardsFile,true);
        informChangeListeners();
    }

    public void setOptions(Map<String, String> options) {
        if (options == null) {
            return;
        }
        this.myOptions.putAll(options);
        calculateDerivedFields();
    }

    /**
     * Returns the options for this board This reflects the options selected through
     * the menu functionality in the boards.txt
     *
     * @return a map of case insensitive ordered key value pairs
     */
    public Map<String, String> getOptions() {
        return this.myOptions;
    }

    public String getBoardID() {
        return this.myBoardID;
    }

    public String[] getCompatibleBoards() {
        return this.myTxtFile.getAllNames();
    }

    public String[] getUploadProtocols() {

        return Programmers.getUploadProtocols(this);

    }

    public String[] getMenuItemNamesFromMenuID(String menuID) {
        return this.myTxtFile.getMenuItemNamesFromMenuID(menuID, this.myBoardID);
    }

    public Set<String> getAllMenuNames() {
        return this.myTxtFile.getMenuNames();
    }

    public TreeMap<String, IPath> getAllExamples() {
        return LibraryManager.getAllExamples(this);
    }

    public void addChangeListener(ChangeListener l) {
        this.myChangeListener = l;
    }

    public void removeChangeListener() {
        this.myChangeListener = null;
    }

    private void informChangeListeners() {
        calculateDerivedFields();
        if (this.myChangeListener != null) {
            this.myChangeListener.stateChanged(null);
        }
    }

    public String getMenuIdFromMenuName(String menuName) {
        return this.myTxtFile.getMenuIDFromMenuName(menuName);
    }

    public String getMenuNameFromMenuID(String id) {
        return this.myTxtFile.getMenuNameFromID(id);
    }

    public String getMenuItemNamedFromMenuItemID(String menuItemID, String menuID) {
        return this.myTxtFile.getMenuItemNameFromMenuItemID(this.myBoardID, menuID, menuItemID);
    }

    public String getMenuItemIDFromMenuItemName(String menuItemName, String menuID) {
        return this.myTxtFile.getMenuItemIDFromMenuItemName(this.myBoardID, menuID, menuItemName);
    }

    public static String getUploadPort(IProject project) {
        return Common.getBuildEnvironmentVariable(project, ENV_KEY_JANTJE_UPLOAD_PORT, new String());
    }

    private String getMyOSName() {
        return this.myOSName;
    }

    private String getMyWorkSpaceLocation() {
        return this.myWorkSpaceLocation;
    }

    private String getMyWorkEclipseLocation() {
        return this.myWorkEclipseLocation;
    }

    public String getProjectName() {
        return this.myProjectName;
    }

    /**
     * provide the actual path to the variant. Use this method if you want to know
     * where the variant is
     *
     * @return the path to the variant; null if no variant is needed
     */
    public IPath getActualVariantPath() {
        if (getBoardVariant() == null) {
            return null;
        }
        IPath retPath = getReferencedVariantPlatformPath();
        if (retPath == null) {
            retPath = getreferencingPlatformPath();
        }
        return retPath.append(Const.VARIANTS_FOLDER_NAME).append(getBoardVariant());
    }

    private String getBoardVariant() {
        return this.myBoardsVariant;
    }

    public IPath getActualCoreCodePath() {
        IPath retPath = getReferencedCorePlatformPath();
        if (retPath == null) {
            retPath = getreferencingPlatformPath();
        }
        if (this.myBoardsCore == null) {
            return null;
        }
        return retPath.append("cores").append(this.myBoardsCore);
    }

    /**
	 * provide the actual path to the variant. Use this method if you want to know
     * where the variant is
     *
	 * @return the path to the variant
     */
    public IPath getReferencedCorePlatformPath() {
        if (myReferencedCorePlatformPath != null) {
            return myReferencedCorePlatformPath;
        }
        return getreferencingPlatformPath();
    }

    public IPath getReferencedUploadPlatformPath() {
        if (myReferencedUploadToolPlatformPath != null) {
            return myReferencedUploadToolPlatformPath;
        }
        return getreferencingPlatformPath();
    }

    public IPath getReferencedVariantPlatformPath() {
        if (myReferencedBoardVariantPlatformPath != null) {
            return myReferencedBoardVariantPlatformPath;
        }
        return getreferencingPlatformPath();
    }

    public File getReferencingPlatformFile() {
        return getreferencingPlatformPath().append(Const.PLATFORM_FILE_NAME).toFile();
    }

    public Path getreferencingPlatformPath() {
        try {
            return new Path(this.myreferencingBoardsFile.getParent());
        } catch (@SuppressWarnings("unused") Exception e) {
            return new Path(new String());
        }
    }

    public File getreferencedPlatformFile() {
        if (this.myReferencedCorePlatformPath == null) {
            return null;
        }
        return this.myReferencedCorePlatformPath.append(Const.PLATFORM_FILE_NAME).toFile();
    }

    public IPath getReferencedLibraryPath() {
        if (this.myReferencedCorePlatformPath == null) {
            return null;
        }
        return this.myReferencedCorePlatformPath.append(LIBRARY_PATH_SUFFIX);
    }

    public IPath getReferencingLibraryPath() {
        return this.getreferencingPlatformPath().append(LIBRARY_PATH_SUFFIX);
    }

    public String getUploadCommand(ICConfigurationDescription confdesc) {
        String upLoadTool = getActualUploadTool(confdesc);
        String action = "UPLOAD";
        if (usesProgrammer()) {
            action = "PROGRAM";
        }
        String networkPrefix = "";
        if (isNetworkUpload()) {
            networkPrefix = "NETWORK_";
        }
		String key = "A.TOOLS." + upLoadTool.toUpperCase() + "."
				+ action + "." + networkPrefix+"PATTERN";
        String ret = Common.getBuildEnvironmentVariable(confdesc, key, "");
        if (ret.isEmpty()) {
			Common.log(new Status(IStatus.ERROR, Const.CORE_PLUGIN_ID,
					key + " : not found in the platform.txt file"));
        }
        return ret;
    }

    public String getActualUploadTool(ICConfigurationDescription confdesc) {
        if (confdesc == null) {
            Common.log(new Status(IStatus.ERROR, Const.CORE_PLUGIN_ID, "Confdesc null is not alowed here"));
            return this.myUploadTool;
        }
        if (usesProgrammer()) {
            return Common.getBuildEnvironmentVariable(confdesc, "A.PROGRAM.TOOL",
                    "Program tool not properly configured");
        }

        if (this.myUploadTool == null) {
            return Common.getBuildEnvironmentVariable(confdesc, "A.UPLOAD.TOOL", "upload tool not properly configured");
        }

        return this.myUploadTool;
    }

    public boolean usesProgrammer() {
        return !this.myProgrammer.equals(Defaults.getDefaultUploadProtocol());
    }

    public IPath getreferencedHardwarePath() {
        IPath platformPath = getReferencedCorePlatformPath();
        return platformPath.removeLastSegments(1);
    }

    /*
	 * get the latest installed arduino platform with the same architecture.
	 * This is the platform to use the programmers.txt if no other programmers.txt
	 * are found.
     */
    public IPath getArduinoPlatformPath() {
        return InternalPackageManager.getPlatformInstallPath("arduino", getArchitecture());
    }

    /**
     * If the upload port contains a space everything before the first space is
     * considered to be a dns name or ip adress.
     *
     * @param mComPort
	 * @return null if no space in uploadport
	 *         return dns name op ipadress
     */
    public String getHost() {
        String host = myUploadPort.split(" ")[0];
        if (host.equals(myUploadPort))
            return null;
        return host;
    }

    /**
     * true if this board needs a networkUpload else false
     * 
     * @return
     */
    public boolean isNetworkUpload() {
        return getHost() != null;
    }

}
