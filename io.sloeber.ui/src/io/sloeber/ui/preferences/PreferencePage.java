package io.sloeber.ui.preferences;

import java.io.File;

import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.ComboFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.PathEditor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.preferences.ScopedPreferenceStore;

import io.sloeber.core.api.BoardsManager;
import io.sloeber.core.api.Defaults;
import io.sloeber.core.api.LibraryManager;
import io.sloeber.core.api.Other;
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
    private static final String KEY_PRIVATE_HARDWARE_PATHS = "Gui entry for private hardware paths"; //$NON-NLS-1$
    private static final String KEY_PRIVATE_LIBRARY_PATHS = "Gui entry for private library paths"; //$NON-NLS-1$

    private PathEditor arduinoPrivateLibPath;
    private PathEditor arduinoPrivateHardwarePath;
    private ComboFieldEditor buildBeforeUploadOption;
    private BooleanFieldEditor openSerialMonitorOpensSerialsOption;
    private BooleanFieldEditor automaticallyImportLibrariesOption;
    private BooleanFieldEditor cleanSerialMonitorAfterUpload;

    public PreferencePage() {
	super(org.eclipse.jface.preference.FieldEditorPreferencePage.GRID);
	setDescription(Messages.ui_workspace_settings);

	ScopedPreferenceStore preferences = new ScopedPreferenceStore(InstanceScope.INSTANCE,
		MyPreferences.NODE_ARDUINO);
	preferences.setDefault(MyPreferences.KEY_OPEN_SERIAL_WITH_MONITOR,
		MyPreferences.DEFAULT_OPEN_SERIAL_WITH_MONITOR);
	preferences.setDefault(KEY_AUTO_IMPORT_LIBRARIES, true);
	preferences.setDefault(KEY_PRIVATE_HARDWARE_PATHS, Defaults.getPrivateHardwarePath());
	preferences.setDefault(KEY_PRIVATE_LIBRARY_PATHS, Defaults.getPrivateLibraryPath());
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
	BoardsManager.setAutoImportLibraries(this.automaticallyImportLibrariesOption.getBooleanValue());
	BoardsManager.setPrivateHardwarePaths(hardWarePaths);
	BoardsManager.setPrivateLibraryPaths(libraryPaths);
	return ret;
    }

    @Override
    public void init(IWorkbench workbench) {
	String hardWarePaths = BoardsManager.getPrivateHardwarePathsString();
	String libraryPaths = LibraryManager.getPrivateLibraryPathsString();
	boolean autoImport = BoardsManager.getAutoImportLibraries();

	getPreferenceStore().setValue(KEY_AUTO_IMPORT_LIBRARIES, autoImport);
	getPreferenceStore().setValue(KEY_PRIVATE_HARDWARE_PATHS, hardWarePaths);
	getPreferenceStore().setValue(KEY_PRIVATE_LIBRARY_PATHS, libraryPaths);
    }

    /**
     * createFieldEditors creates the fields to edit. <br/>
     * 
     * @author Jan Baeyens
     */
    @Override
    protected void createFieldEditors() {
	final Composite parent = getFieldEditorParent();

	this.arduinoPrivateLibPath = new PathEditor(KEY_PRIVATE_LIBRARY_PATHS, Messages.ui_private_lib_path,
		Messages.ui_private_lib_path_help, parent);
	addField(this.arduinoPrivateLibPath);

	this.arduinoPrivateHardwarePath = new PathEditor(KEY_PRIVATE_HARDWARE_PATHS, Messages.ui_private_hardware_path,
		Messages.ui_private_hardware_path_help, parent);
	addField(this.arduinoPrivateHardwarePath);

	Dialog.applyDialogFont(parent);
	createLine(parent, 4);
	String[][] YesNoAskOptions = new String[][] { { Messages.ui_ask_every_upload, "ASK" }, //$NON-NLS-1$
		{ "Yes", TRUE }, { "No", FALSE } }; //$NON-NLS-1$ //$NON-NLS-2$
	this.buildBeforeUploadOption = new ComboFieldEditor(MyPreferences.KEY_BUILD_BEFORE_UPLOAD_OPTION,
		Messages.ui_build_before_upload, YesNoAskOptions, parent);
	addField(this.buildBeforeUploadOption);
	createLine(parent, 4);

	this.openSerialMonitorOpensSerialsOption = new BooleanFieldEditor(MyPreferences.KEY_OPEN_SERIAL_WITH_MONITOR,
		Messages.ui_open_serial_with_monitor, BooleanFieldEditor.DEFAULT, parent);
	addField(this.openSerialMonitorOpensSerialsOption);
	createLine(parent, 4);

	this.automaticallyImportLibrariesOption = new BooleanFieldEditor(KEY_AUTO_IMPORT_LIBRARIES,
		Messages.ui_auto_import_libraries, BooleanFieldEditor.DEFAULT, parent);
	addField(this.automaticallyImportLibrariesOption);

	this.cleanSerialMonitorAfterUpload = new BooleanFieldEditor(MyPreferences.getCleanSerialMonitorAfterUploadKey(),
		Messages.ui_Clean_Serial_Monitor_After_Upload, BooleanFieldEditor.DEFAULT, parent);
	addField(this.cleanSerialMonitorAfterUpload);

	createLine(parent, 4);
	Label label = new Label(parent, SWT.LEFT);
	label.setText("Your HashKey: " + Other.getSystemHash()); //$NON-NLS-1$
	label.setLayoutData(new GridData(SWT.BEGINNING, SWT.BEGINNING, false, false, 1, 2));

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

    private static void createLine(Composite parent, int ncol) {
	Label line = new Label(parent, SWT.SEPARATOR | SWT.HORIZONTAL | SWT.BOLD);
	GridData gridData = new GridData(GridData.FILL_HORIZONTAL);
	gridData.horizontalSpan = ncol;
	line.setLayoutData(gridData);
    }

}
