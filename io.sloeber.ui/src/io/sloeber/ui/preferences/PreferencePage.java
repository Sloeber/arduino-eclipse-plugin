package io.sloeber.ui.preferences;

import static io.sloeber.ui.Activator.*;

import java.io.File;
import java.util.ArrayList;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.ComboFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.PathEditor;
import org.eclipse.jface.resource.FontRegistry;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.preferences.ScopedPreferenceStore;
import org.eclipse.ui.themes.ITheme;
import org.eclipse.ui.themes.IThemeManager;

import io.sloeber.arduinoFramework.api.BoardsManager;
import io.sloeber.arduinoFramework.api.LibraryManager;
import io.sloeber.core.api.Defaults;
import io.sloeber.core.api.Preferences;
import io.sloeber.ui.Messages;
import io.sloeber.ui.helpers.MyPreferences;

/**
 * ArduinoPreferencePage is the class that is behind the preference page of
 * arduino that allows you to select the arduino path and the library path and a
 * option to use disable RXTX <br/>
 * Note that this class uses 2 technologies to change values (the flag and the
 * path). <br/>
 *
 *
 * @author Jan Baeyens
 *
 */
public class PreferencePage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {

	private static final String TRUE = "TRUE"; //$NON-NLS-1$
	private static final String FALSE = "FALSE"; //$NON-NLS-1$
	private static final String KEY_AUTO_IMPORT_LIBRARIES = "Gui entry for import libraries"; //$NON-NLS-1$
	private static final String KEY_PRAGMA_ONCE_HEADERS = "Gui entry for add pragma once"; //$NON-NLS-1$
	private static final String KEY_PRIVATE_HARDWARE_PATHS = "Gui entry for private hardware paths"; //$NON-NLS-1$
	private static final String KEY_PRIVATE_LIBRARY_PATHS = "Gui entry for private library paths"; //$NON-NLS-1$
	private static final String KEY_TOOLCHAIN_SELECTION = "Gui entry for toolchain selection"; //$NON-NLS-1$
	private static final String KEY_USE_BONJOUR = "Gui entry for usage of bonjour"; //$NON-NLS-1$

	private PathEditor arduinoPrivateLibPathPathEditor;
	private PathEditor arduinoPrivateHardwarePathPathEditor;
	private ComboFieldEditor buildBeforeUploadOptionEditor;
	private BooleanFieldEditor openSerialMonitorOpensSerialsOptionEditor;
	private BooleanFieldEditor automaticallyImportLibrariesOptionEditor;
	private BooleanFieldEditor automaticallyInstallLibrariesOptionEditor;
	private BooleanFieldEditor useArduinoToolchainSelectionEditor;
	private BooleanFieldEditor pragmaOnceHeaderOptionEditor;
	private BooleanFieldEditor cleanSerialMonitorAfterUploadEditor;
	private BooleanFieldEditor switchToSerialMonitorAfterUploadEditor;
	private BooleanFieldEditor enableParallelBuildForNewProjects;
	private BooleanFieldEditor enableBonjour;

	public PreferencePage() {
		super(org.eclipse.jface.preference.FieldEditorPreferencePage.GRID);
		setDescription(Messages.ui_workspace_settings);

		ScopedPreferenceStore preferences = new ScopedPreferenceStore(InstanceScope.INSTANCE,
				MyPreferences.NODE_ARDUINO);
		preferences.setDefault(MyPreferences.KEY_OPEN_SERIAL_WITH_MONITOR,
				MyPreferences.DEFAULT_OPEN_SERIAL_WITH_MONITOR);
		preferences.setDefault(KEY_AUTO_IMPORT_LIBRARIES, true);
		preferences.setDefault(KEY_PRAGMA_ONCE_HEADERS, true);
		preferences.setDefault(KEY_USE_BONJOUR, Defaults.useBonjour);
		preferences.setDefault(KEY_PRIVATE_HARDWARE_PATHS, Defaults.getPrivateHardwarePath());
		preferences.setDefault(KEY_PRIVATE_LIBRARY_PATHS, Defaults.getPrivateLibraryPath());
		preferences.setDefault(KEY_TOOLCHAIN_SELECTION, Defaults.useArduinoToolSelection);
		preferences.setDefault(MyPreferences.KEY_AUTO_INSTALL_LIBRARIES, Defaults.autoInstallLibraries);

		setPreferenceStore(preferences);
	}

	@Override
	public boolean isValid() {
		return testStatus();
	}

	@Override
	public boolean okToLeave() {
		return testStatus();
	}

	/**
	 * PerformOK is done when the end users presses OK on a preference page. The
	 * order of the execution of the performOK is undefined. This method saves
	 * the path variables based on the settings and removes the last used
	 * setting.<br/>
	 *
	 * @see propertyChange
	 *
	 * @see createFieldEditors
	 *
	 * @author Jan Baeyens
	 *
	 */
	@Override
	public boolean performOk() {
		if (!testStatus()) {
			return false;
		}
		boolean ret = super.performOk();
		String hardWarePaths[] = getPreferenceStore().getString(KEY_PRIVATE_HARDWARE_PATHS).split(File.pathSeparator);
		String libraryPaths[] = getPreferenceStore().getString(KEY_PRIVATE_LIBRARY_PATHS).split(File.pathSeparator);

		String filteredList[] =filterUnwantedPaths(hardWarePaths);
		if (filteredList!=null) {
		    hardWarePaths=filteredList;
			log(new Status(IStatus.ERROR, PLUGIN_ID, Messages.Invalid_Private_Hardware_folder));
		}
		filteredList=filterUnwantedPaths(libraryPaths);
        if (filteredList!=null) {
            libraryPaths=filteredList;
			log(new Status(IStatus.ERROR, PLUGIN_ID, Messages.Invalid_Private_Library_folder));
        }
        Preferences.setUseArduinoToolSelection(this.useArduinoToolchainSelectionEditor.getBooleanValue());
		Preferences.setAutoImportLibraries(this.automaticallyImportLibrariesOptionEditor.getBooleanValue());
		Preferences.setPragmaOnceHeaders(this.pragmaOnceHeaderOptionEditor.getBooleanValue());
		Preferences.setUseBonjour(enableBonjour.getBooleanValue());
		BoardsManager.setPrivateHardwarePaths(hardWarePaths);
		LibraryManager.setPrivateLibraryPaths(libraryPaths);
		return ret;
	}

	/**
	 * Given a set of paths modify the set removing the unwanted paths
	 * Current implementation filters all path that are the arduinoPlugin folder or subfolders of it
	 * @param paths a list of path strings that need filtering
	 * @return null if no filtering is needed else filtered list
	 */
	private static String[] filterUnwantedPaths(String[] paths) {
	    IPath unWanted=BoardsManager.getInstallationPath();
	    ArrayList<String> filteredList = new ArrayList<>();
	    boolean filtered=false;
	    for (int i = 0; i < paths.length; i++) {
            Path curPath = new Path(paths[i]);
            if(unWanted.isPrefixOf(curPath)) {
                filtered=true;
            }else {
                filteredList.add(paths[i]);
            }
        }
        if(filtered) {
            return filteredList.toArray(new String[0]);
        }

        return null;
    }

    @Override
	public void init(IWorkbench workbench) {
		String hardWarePaths = BoardsManager.getPrivateHardwarePathsString();
		String libraryPaths = LibraryManager.getPrivateLibraryPathsString();
		boolean autoImport = Preferences.getAutoImportLibraries();
		boolean pragmaOnceHeaders = Preferences.getPragmaOnceHeaders();
		boolean useArduinoToolchainSelection =Preferences.getUseArduinoToolSelection();
		boolean useBonjour=Preferences.useBonjour();


		getPreferenceStore().setValue(KEY_AUTO_IMPORT_LIBRARIES, autoImport);
		getPreferenceStore().setValue(KEY_PRAGMA_ONCE_HEADERS, pragmaOnceHeaders);
		getPreferenceStore().setValue(KEY_PRIVATE_HARDWARE_PATHS, hardWarePaths);
		getPreferenceStore().setValue(KEY_PRIVATE_LIBRARY_PATHS, libraryPaths);
		getPreferenceStore().setValue(KEY_TOOLCHAIN_SELECTION, useArduinoToolchainSelection);
		getPreferenceStore().setValue(KEY_USE_BONJOUR, useBonjour);


	}

	/**
	 * createFieldEditors creates the fields to edit. <br/>
	 *
	 * @author Jan Baeyens
	 */
	@Override
	protected void createFieldEditors() {
		final Composite rootParent = getFieldEditorParent();
		Dialog.applyDialogFont(rootParent);
		IThemeManager themeManager = PlatformUI.getWorkbench().getThemeManager();
		ITheme currentTheme = themeManager.getCurrentTheme();
		FontRegistry fontRegistry = currentTheme.getFontRegistry();
		Font headerFont =fontRegistry.get(JFaceResources.HEADER_FONT);

		arduinoPrivateLibPathPathEditor = new PathEditor(KEY_PRIVATE_LIBRARY_PATHS, Messages.ui_private_lib_path,
				Messages.ui_private_lib_path_help, rootParent);
		addField(arduinoPrivateLibPathPathEditor);

		arduinoPrivateHardwarePathPathEditor = new PathEditor(KEY_PRIVATE_HARDWARE_PATHS, Messages.ui_private_hardware_path,
				Messages.ui_private_hardware_path_help, rootParent);
		addField(arduinoPrivateHardwarePathPathEditor);


		Composite parent = new Composite(rootParent, SWT.NONE);
		GridData gd1=new GridData(SWT.BEGINNING,SWT.CENTER,false,false);
		//gd1.horizontalSpan=2;
		parent.setLayoutData(gd1);
		parent.setLayout(new GridLayout(1,false));


		Group UIboxparent= new Group(parent, SWT.BORDER_SOLID );
		UIboxparent.setText(Messages.PreferencePage_UI_Behaviour_Group_Title);
		UIboxparent.setFont(headerFont);
		UIboxparent.setLayout(new GridLayout(2,true));
		GridData gd11=new GridData(SWT.FILL,SWT.TOP,true,false);
		//gd11.horizontalSpan=4;
		UIboxparent.setLayoutData(gd11);

		Composite UIbox = new Composite(UIboxparent, SWT.NONE);
		GridData gd12=new GridData(SWT.FILL,SWT.TOP,true,false);
		gd12.horizontalSpan=2;
		UIbox.setLayoutData(gd12);
		UIbox.setLayout(new GridLayout(2,false));


		String[][] YesNoAskOptions = new String[][] { { Messages.ui_ask_every_upload, "ASK" }, //$NON-NLS-1$
				{ Messages.yes, TRUE }, { Messages.no, FALSE } };
		buildBeforeUploadOptionEditor = new ComboFieldEditor(MyPreferences.KEY_BUILD_BEFORE_UPLOAD_OPTION,
				Messages.ui_build_before_upload, YesNoAskOptions, UIbox);
		addField(buildBeforeUploadOptionEditor);

		Composite UIbox2 = new Composite(UIboxparent, SWT.NONE);
		GridData gd13 = new GridData(SWT.FILL, SWT.TOP, true, false);
		gd13.horizontalSpan = 2;
		UIbox2.setLayoutData(gd13);
		UIbox2.setLayout(new GridLayout(2, false));
		Dialog.applyDialogFont(UIbox2);

		openSerialMonitorOpensSerialsOptionEditor = new BooleanFieldEditor(MyPreferences.KEY_OPEN_SERIAL_WITH_MONITOR,
				Messages.ui_open_serial_with_monitor, BooleanFieldEditor.DEFAULT, UIbox2);
		addField(openSerialMonitorOpensSerialsOptionEditor);

		automaticallyImportLibrariesOptionEditor = new BooleanFieldEditor(KEY_AUTO_IMPORT_LIBRARIES,
				Messages.ui_auto_import_libraries, BooleanFieldEditor.DEFAULT, UIbox2);
		addField(automaticallyImportLibrariesOptionEditor);

		cleanSerialMonitorAfterUploadEditor = new BooleanFieldEditor(MyPreferences.KEY_CLEAN_MONITOR_AFTER_UPLOAD,
				Messages.ui_clean_serial_monitor_after_upload, BooleanFieldEditor.DEFAULT, UIbox2);
		addField(cleanSerialMonitorAfterUploadEditor);

		switchToSerialMonitorAfterUploadEditor = new BooleanFieldEditor(MyPreferences.SWITCH_TO_MONITOR_AFTER_UPLOAD,
				Messages.ui_switch_to_serial_monitor_after_upload, BooleanFieldEditor.DEFAULT, UIbox2);
		addField(switchToSerialMonitorAfterUploadEditor);


		Group internalBehaviourGroup= new Group(parent, SWT.BORDER_SOLID);
		internalBehaviourGroup.setText(Messages.PreferencePage_Internal_Behaviour_Group_Title);
		GridData gd2=new GridData(SWT.FILL,SWT.TOP,true,false);
		//gd2.horizontalSpan=2;
		internalBehaviourGroup.setLayout(new GridLayout(2,true));
		internalBehaviourGroup.setLayoutData(gd2);
		internalBehaviourGroup.setFont(headerFont);

		Composite internalBehaviourbox = new Composite(internalBehaviourGroup, SWT.NONE);
		GridData gd14 = new GridData(SWT.FILL, SWT.TOP, true, false);
		gd14.horizontalSpan = 2;
		internalBehaviourbox.setLayoutData(gd14);
		internalBehaviourbox.setLayout(new GridLayout(2, false));
		Dialog.applyDialogFont(internalBehaviourbox);

//		internalBehaviourGroup.setBackground(display.getSystemColor(SWT.COLOR_GREEN));
//		internalBehaviourbox.setBackground(display.getSystemColor(SWT.COLOR_RED));

		useArduinoToolchainSelectionEditor = new BooleanFieldEditor(KEY_TOOLCHAIN_SELECTION,
				Messages.ui_use_arduino_toolchain_selection, BooleanFieldEditor.DEFAULT, internalBehaviourbox);
		addField(useArduinoToolchainSelectionEditor);

		pragmaOnceHeaderOptionEditor = new BooleanFieldEditor(KEY_PRAGMA_ONCE_HEADERS, Messages.ui_pragma_once_headers,
				BooleanFieldEditor.DEFAULT, internalBehaviourbox);
		addField(pragmaOnceHeaderOptionEditor);

		enableParallelBuildForNewProjects = new BooleanFieldEditor(MyPreferences.KEY_ENABLE_PARALLEL_BUILD_FOR_NEW_PROJECTS,
				Messages.ui_enable_parallel_build_for_new_projects, BooleanFieldEditor.DEFAULT, internalBehaviourbox);
		addField(enableParallelBuildForNewProjects);

		Group netWorkGroup= new Group(parent, SWT.BORDER_SOLID);
		netWorkGroup.setText(Messages.PreferencePage_Network_Group_Title);
		GridData gd3=new GridData(SWT.FILL,SWT.TOP,true,false);
		//gd3.horizontalSpan=2;
		netWorkGroup.setLayoutData(gd3);
		netWorkGroup.setFont(headerFont);
		netWorkGroup.setLayout(new GridLayout(2,true));

		Composite netWorkbox = new Composite(netWorkGroup, SWT.NONE);
		GridData gd15 = new GridData(SWT.FILL, SWT.TOP, true, false);
		gd15.horizontalSpan = 2;
		netWorkbox.setLayoutData(gd15);
		netWorkbox.setLayout(new GridLayout(2, false));
		Dialog.applyDialogFont(netWorkbox);

		automaticallyInstallLibrariesOptionEditor = new BooleanFieldEditor(MyPreferences.KEY_AUTO_INSTALL_LIBRARIES,
				Messages.ui_auto_install_libraries, BooleanFieldEditor.DEFAULT, netWorkbox);
		addField(automaticallyInstallLibrariesOptionEditor);

		enableBonjour = new BooleanFieldEditor(KEY_USE_BONJOUR,
				Messages.ui_enable_bonjour, BooleanFieldEditor.DEFAULT, netWorkbox);
		addField(enableBonjour);

	}

	/**
	 * testStatus test whether the provided information is OK. Here the code
	 * checks whether there is a hardware\arduino\board.txt file under the
	 * provide path.
	 *
	 * @return true if the provided info is OK; False if the provided info is
	 *         not OK
	 *
	 * @author Jan Baeyens
	 *
	 */
	boolean testStatus() {

		setErrorMessage(null);
		setValid(true);
		return true;
	}

	@Override
	protected void performApply() {
		super.performApply();
	}


}
