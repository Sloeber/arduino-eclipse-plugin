/*******************************************************************************
 * 
 * Copyright (c) 2008, 2010 Thomas Holland (thomas@innot.de) and others
 * 
 * This program and the accompanying materials are made
 * available under the terms of the GNU Public License v3
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 * 
 * Contributors:
 *     Thomas Holland - initial API and implementation
 *     
 * $Id: TabAVRDudeFlashEEPROM.java 851 2010-08-07 19:37:00Z innot $
 *     
 *******************************************************************************/
package it.baeyens.avreclipse.ui.propertypages;

import it.baeyens.arduino.ArduinoConst;
import it.baeyens.avreclipse.AVRPlugin;
import it.baeyens.avreclipse.PluginIDs;
import it.baeyens.avreclipse.core.properties.AVRDudeProperties;

import org.eclipse.cdt.core.settings.model.ICConfigurationDescription;
import org.eclipse.cdt.managedbuilder.core.BuildException;
import org.eclipse.cdt.managedbuilder.core.IConfiguration;
import org.eclipse.cdt.managedbuilder.core.IOption;
import org.eclipse.cdt.managedbuilder.core.IToolChain;
import org.eclipse.cdt.managedbuilder.core.ManagedBuildManager;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;


/**
 * The AVRDude Flash and EEPROM Tab page.
 * <p>
 * On this tab, the following properties are edited:
 * <ul>
 * <li>Upload of a Flash image</li>
 * <li>Upload of a EEPROM imagey</li>
 * </ul>
 * The hex image files are either taken from the current Build Configuration (this is internally
 * handled as an empty filename), or from a user selectable image file.
 * </p>
 * 
 * @author Thomas Holland
 * @since 2.2
 * 
 */
public class TabAVRDudeFlashEEPROM extends AbstractAVRDudePropertyTab {

	// The GUI texts
	private final static String		GROUP_FLASH				= "Upload Flash Memory Image";
	private final static String		TEXT_FLASH_NOUPLOAD		= "do not upload flash memory image";
	private final static String		TEXT_FLASH_FROMCONFIG	= "from build";
	private final static String		TEXT_FLASH_FROMFILE		= "from Flash memory image file:";

	private final static String		GROUP_EEPROM			= "Upload EEPROM Image";
	private final static String		TEXT_EEPROM_NOUPLOAD	= "do not upload eeprom image";
	private final static String		TEXT_EEPROM_FROMCONFIG	= "from build";
	private final static String		TEXT_EEPROM_FROMFILE	= "from EEPROM image file:";

	// The GUI widgets
	private Button					fFlashNoUploadButton;
	private Button					fFlashUploadConfigButton;
	private Button					fFlashUploadFileButton;
	private Text					fFlashFileText;
	private Button					fFlashWorkplaceButton;
	private Button					fFlashFilesystemButton;
	private Button					fFlashVariableButton;

	private Button					fEEPROMNoUploadButton;
	private Button					fEEPROMUploadConfigButton;
	private Button					fEEPROMUploadFileButton;
	private Text					fEEPROMFileText;
	private Button					fEEPROMWorkplaceButton;
	private Button					fEEPROMFilesystemButton;
	private Button					fEEPROMVariableButton;

	/** The Properties that this page works with */
	private AVRDudeProperties		fTargetProps;

	/** The file extensions for image files. Used by the file selector. */
	private final static String[]	IMAGE_EXTS				= new String[] { "*.hex", "*.eep",
			"*.bin", "*.srec"								};

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.cdt.ui.newui.AbstractCPropertyTab#createControls(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	public void createControls(Composite parent) {
		parent.setLayout(new GridLayout(1, false));

		addFlashSection(parent);

		addEEPROMSection(parent);

	}

	/**
	 * Add the flash image selection group.
	 * <p>
	 * This group has three radio buttons:
	 * <ul>
	 * <li>No upload</li>
	 * <li>Upload the file generated in the build configuration</li>
	 * <li>Upload a user selectable file.</li>
	 * </ul>
	 * The last option has a Text control to enter a filename and three buttons to select the
	 * filename from the workplace, the filesystem or from a build variable.
	 * </p>
	 * 
	 * @param parent
	 *            Parent <code>Composite</code>
	 */
	private void addFlashSection(Composite parent) {

		// Group Setup
		Group group = new Group(parent, SWT.NONE);
		group.setText(GROUP_FLASH);
		group.setLayout(new GridLayout(2, false));
		group.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1));

		// No Upload Button
		fFlashNoUploadButton = new Button(group, SWT.RADIO);
		fFlashNoUploadButton.setText(TEXT_FLASH_NOUPLOAD);
		fFlashNoUploadButton.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false, 2, 1));
		fFlashNoUploadButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				fTargetProps.setWriteFlash(false);
				enableFlashFileGroup(false);
				updateAVRDudePreview(fTargetProps);
			}
		});

		// Upload from Config Button
		fFlashUploadConfigButton = new Button(group, SWT.RADIO);
		fFlashUploadConfigButton.setText(TEXT_FLASH_FROMCONFIG);
		fFlashUploadConfigButton
				.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false, 2, 1));
		fFlashUploadConfigButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				fTargetProps.setWriteFlash(true);
				fTargetProps.setFlashFromConfig(true);
				// Set the corresponding "Generate Flash Image" in the current
				// toolchain(s).
				setBuildConfigGenerateFlag(PluginIDs.PLUGIN_TOOLCHAIN_OPTION_GENERATEFLASH);
				enableFlashFileGroup(false);
				updateAVRDudePreview(fTargetProps);
			}
		});

		// Upload from file Button
		fFlashUploadFileButton = new Button(group, SWT.RADIO);
		fFlashUploadFileButton.setText(TEXT_FLASH_FROMFILE);
		fFlashUploadFileButton.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		fFlashUploadFileButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				fTargetProps.setWriteFlash(true);
				fTargetProps.setFlashFromConfig(false);
				fFlashFileText.setText(fTargetProps.getFlashFile());
				enableFlashFileGroup(true);
				updateAVRDudePreview(fTargetProps);
			}
		});

		// The image file Text Control
		fFlashFileText = new Text(group, SWT.BORDER);
		fFlashFileText.setLayoutData(new GridData(SWT.FILL, SWT.NONE, true, false));
		fFlashFileText.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				String newpath = fFlashFileText.getText();
				fTargetProps.setFlashFile(newpath);
				updateAVRDudePreview(fTargetProps);
			}
		});

		// The three File Dialog Buttons (and a alignment/filler Label),
		// all wrapped in a composite.
		Composite compo = new Composite(group, SWT.NONE);
		compo.setBackgroundMode(SWT.INHERIT_FORCE);
		compo.setLayoutData(new GridData(SWT.FILL, SWT.NONE, true, false, 2, 1));
		compo.setLayout(new GridLayout(4, false));

		Label label = new Label(compo, SWT.NONE); // Filler
		label.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));

		fFlashWorkplaceButton = setupWorkplaceButton(compo, fFlashFileText);
		fFlashFilesystemButton = setupFilesystemButton(compo, fFlashFileText, IMAGE_EXTS);
		fFlashVariableButton = setupVariableButton(compo, fFlashFileText);

	}

	/**
	 * Enable / Disable the Flash file selector Controls
	 * 
	 * @param enabled
	 *            <code>true</code> to enable them, <code>false</code> to disable.
	 */
	private void enableFlashFileGroup(boolean enabled) {
		fFlashFileText.setEnabled(enabled);
		fFlashWorkplaceButton.setEnabled(enabled);
		fFlashFilesystemButton.setEnabled(enabled);
		fFlashVariableButton.setEnabled(enabled);
	}

	/**
	 * Add the eeprom image selection group.
	 * <p>
	 * This group has three radio buttons:
	 * <ul>
	 * <li>No upload</li>
	 * <li>Upload the file generated in the build configuration</li>
	 * <li>Upload a user selectable file.</li>
	 * </ul>
	 * The last option has a Text control to enter a filename and three buttons to select the
	 * filename from the workplace, the filesystem or from a build variable.
	 * </p>
	 * 
	 * @param parent
	 *            Parent <code>Composite</code>
	 */
	private void addEEPROMSection(Composite parent) {

		// Group Setup
		Group group = new Group(parent, SWT.NONE);
		group.setText(GROUP_EEPROM);
		group.setLayout(new GridLayout(2, false));
		group.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1));

		// No Upload Button
		fEEPROMNoUploadButton = new Button(group, SWT.RADIO);
		fEEPROMNoUploadButton.setText(TEXT_EEPROM_NOUPLOAD);
		fEEPROMNoUploadButton.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false, 2, 1));
		fEEPROMNoUploadButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				fTargetProps.setWriteEEPROM(false);
				enableEEPROMFileGroup(false);
				updateAVRDudePreview(fTargetProps);
			}
		});

		// Upload from Config Button
		fEEPROMUploadConfigButton = new Button(group, SWT.RADIO);
		fEEPROMUploadConfigButton.setText(TEXT_EEPROM_FROMCONFIG);
		fEEPROMUploadConfigButton
				.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false, 2, 1));
		fEEPROMUploadConfigButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				fTargetProps.setWriteEEPROM(true);
				fTargetProps.setEEPROMFromConfig(true);
				enableEEPROMFileGroup(false);
				// Set the corresponding "Generate EEPROM Image" in the current
				// toolchain(s).
				setBuildConfigGenerateFlag(PluginIDs.PLUGIN_TOOLCHAIN_OPTION_GENERATEEEPROM);
				updateAVRDudePreview(fTargetProps);
			}
		});

		// Upload from file Button
		fEEPROMUploadFileButton = new Button(group, SWT.RADIO);
		fEEPROMUploadFileButton.setText(TEXT_EEPROM_FROMFILE);
		fEEPROMUploadFileButton.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		fEEPROMUploadFileButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				fTargetProps.setWriteEEPROM(true);
				fTargetProps.setEEPROMFromConfig(false);
				fEEPROMFileText.setText(fTargetProps.getEEPROMFile());
				enableEEPROMFileGroup(true);
				updateAVRDudePreview(fTargetProps);
			}
		});

		// The image file Text Control
		fEEPROMFileText = new Text(group, SWT.BORDER);
		fEEPROMFileText.setLayoutData(new GridData(SWT.FILL, SWT.NONE, true, false));
		fEEPROMFileText.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				String newpath = fEEPROMFileText.getText();
				fTargetProps.setEEPROMFile(newpath);
				updateAVRDudePreview(fTargetProps);
			}
		});

		// The three File Dialog Buttons (and a alignment/filler Label),
		// all wrapped in a composite.
		Composite compo = new Composite(group, SWT.NONE);
		compo.setBackgroundMode(SWT.INHERIT_FORCE);
		compo.setLayoutData(new GridData(SWT.FILL, SWT.NONE, true, false, 2, 1));
		compo.setLayout(new GridLayout(4, false));

		Label label = new Label(compo, SWT.NONE); // Filler
		label.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));

		fEEPROMWorkplaceButton = setupWorkplaceButton(compo, fEEPROMFileText);
		fEEPROMFilesystemButton = setupFilesystemButton(compo, fEEPROMFileText, IMAGE_EXTS);
		fEEPROMVariableButton = setupVariableButton(compo, fEEPROMFileText);

	}

	/**
	 * Enable / Disable the EEPROM file selector Controls
	 * 
	 * @param enabled
	 *            <code>true</code> to enable them, <code>false</code> to disable.
	 */
	private void enableEEPROMFileGroup(boolean enabled) {
		fEEPROMFileText.setEnabled(enabled);
		fEEPROMWorkplaceButton.setEnabled(enabled);
		fEEPROMFilesystemButton.setEnabled(enabled);
		fEEPROMVariableButton.setEnabled(enabled);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see it.baeyens.avreclipse.ui.propertypages.AbstractAVRPropertyTab#performApply(it.baeyens.avreclipse.core.preferences.AVRProjectProperties)
	 */
	@Override
	protected void performApply(AVRDudeProperties dstprops) {

		if (fTargetProps == null) {
			// updataData() has not been called and this tab has no (modified)
			// settings yet.
			return;
		}

		// Copy the currently selected values of this tab to the given, fresh
		// Properties.
		// The caller of this method will handle the actual saving
		dstprops.setWriteFlash(fTargetProps.getWriteFlash());
		dstprops.setFlashFromConfig(fTargetProps.getFlashFromConfig());
		dstprops.setFlashFile(fTargetProps.getFlashFile());

		dstprops.setWriteEEPROM(fTargetProps.getWriteEEPROM());
		dstprops.setEEPROMFromConfig(fTargetProps.getEEPROMFromConfig());
		dstprops.setEEPROMFile(fTargetProps.getEEPROMFile());

		// Update the "Generate xxx images" options of
		// the current toolchain / all toolchains as required
		if (fTargetProps.getWriteFlash() && !fTargetProps.getFlashFromConfig()) {
			setBuildConfigGenerateFlag(PluginIDs.PLUGIN_TOOLCHAIN_OPTION_GENERATEFLASH);
		}
		if (fTargetProps.getWriteEEPROM() && !fTargetProps.getEEPROMFromConfig()) {
			setBuildConfigGenerateFlag(PluginIDs.PLUGIN_TOOLCHAIN_OPTION_GENERATEEEPROM);
		}

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see it.baeyens.avreclipse.ui.propertypages.AbstractAVRPropertyTab#performDefaults(it.baeyens.avreclipse.core.preferences.AVRProjectProperties)
	 */
	@Override
	protected void performCopy(AVRDudeProperties srcprops) {

		// Reload the items on this page
		fTargetProps.setWriteFlash(srcprops.getWriteFlash());
		fTargetProps.setFlashFromConfig(srcprops.getFlashFromConfig());
		fTargetProps.setFlashFile(srcprops.getFlashFile());

		fTargetProps.setWriteEEPROM(srcprops.getWriteEEPROM());
		fTargetProps.setEEPROMFromConfig(srcprops.getEEPROMFromConfig());
		fTargetProps.setEEPROMFile(srcprops.getEEPROMFile());
		updateData(fTargetProps);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see it.baeyens.avreclipse.ui.propertypages.AbstractAVRPropertyTab#updateData(it.baeyens.avreclipse.core.preferences.AVRProjectProperties)
	 */
	@Override
	protected void updateData(AVRDudeProperties props) {

		fTargetProps = props;

		// Update the flash group

		// There are three possibilities:
		// a) No upload wanted: WriteFlash == false
		// b) Upload from config: WriteFlash == true && FromConfig == true
		// c) Upload from file: WriteFlash == true && FromConfig == false
		if (!fTargetProps.getWriteFlash()) {
			// a) No upload wanted
			fFlashNoUploadButton.setSelection(true);
			fFlashUploadConfigButton.setSelection(false);
			fFlashUploadFileButton.setSelection(false);
			enableFlashFileGroup(false);
		} else {
			// write flash
			fFlashNoUploadButton.setSelection(false);
			if (fTargetProps.getFlashFromConfig()) {
				// b) write flash - use build config filename
				fFlashUploadConfigButton.setSelection(true);
				fFlashUploadFileButton.setSelection(false);
				enableFlashFileGroup(false);
			} else {
				// c) write flash - user supplied filename
				fFlashUploadConfigButton.setSelection(false);
				fFlashUploadFileButton.setSelection(true);
				enableFlashFileGroup(true);
			}
		}
		fFlashFileText.setText(fTargetProps.getFlashFile());

		// Update the eeprom group

		// There are three possibilities:
		// a) No upload wanted: WriteEEPROM == false
		// b) Upload from config: WriteEEPROM == true && FromConfig == true
		// c) Upload from file: WriteEEPROM == true && FromConfig == false
		if (!fTargetProps.getWriteEEPROM()) {
			// a) don't write eeprom
			fEEPROMNoUploadButton.setSelection(true);
			fEEPROMUploadConfigButton.setSelection(false);
			fEEPROMUploadFileButton.setSelection(false);
			enableEEPROMFileGroup(false);
		} else {
			// write eeprom
			fEEPROMNoUploadButton.setSelection(false);
			if (fTargetProps.getEEPROMFromConfig()) {
				// b) write eeprom - use build config filename
				fEEPROMUploadConfigButton.setSelection(true);
				fEEPROMUploadFileButton.setSelection(false);
				enableEEPROMFileGroup(false);
			} else {
				// c) write eeprom - user supplied filename
				fEEPROMUploadConfigButton.setSelection(false);
				fEEPROMUploadFileButton.setSelection(true);
				enableEEPROMFileGroup(true);
			}
		}
		fEEPROMFileText.setText(fTargetProps.getEEPROMFile());
	}

	/**
	 * Set the value of the given toolchain option to <code>true</code>, if the "avrdude" option
	 * is <code>true</code>.
	 * <p>
	 * If the "per Config" flasg is set for the project, only the current config is modified. If
	 * "per config" is not set, then all known build configurations of the project have the
	 * specified toolchain option set.
	 * </p>
	 * <p>
	 * This is supposed to be a convenience for the user, as we assume that if he wants to upload an
	 * image to the MCU, than he also wants the image file to be created during the build.
	 * </p>
	 * 
	 * @param optionid
	 *            <code>String</code> with the id of the boolean option.
	 */
	private void setBuildConfigGenerateFlag(String optionid) {

		// Test if we are perProject or perConfig.
		// If per Project, we update the "Generate xxx image" for all
		// configurations, not just the selected one
		if (isPerConfig()) {
			IConfiguration buildcfg = getCfg();
			setBuildConfigGenerateFlag(buildcfg, optionid);
		} else {
			// per Project settings.
			// Get all Configurations of this page and set the generate image
			// flag as required.
			ICConfigurationDescription[] allconfigdesc = page.getCfgsEditable();
			for (ICConfigurationDescription cfgdesc : allconfigdesc) {
				IConfiguration buildcfg = ManagedBuildManager
						.getConfigurationForDescription(cfgdesc);
				setBuildConfigGenerateFlag(buildcfg, optionid);
			}
		}
	}

	/**
	 * Set the value of a toolchain option of the given build configuration to <code>true</code>,
	 * if the "avrdude" option is <code>true</code>.
	 * <p>
	 * 
	 * @param buildcfg
	 *            <code>IConfiguration</code> for which the option value is to be set.
	 * @param optionid
	 *            <code>String</code> with the id of the boolean option.
	 */
	private void setBuildConfigGenerateFlag(IConfiguration buildcfg, String optionid) {

		IToolChain toolchain = buildcfg.getToolChain();

		// get the avrdude option.
		// The requested option will only be set to true, if the avrdude option
		// is true as well.
		IOption avrdudeoption = toolchain
				.getOptionBySuperClassId("it.baeyens.avreclipse.toolchain.options.toolchain.avrdude");

		IOption option = toolchain.getOptionBySuperClassId(optionid);

		try {
			if (avrdudeoption.getBooleanValue()) {
				ManagedBuildManager.setOption(buildcfg, toolchain, option, true);
			}
		} catch (BuildException e) {
			IStatus status = new Status(Status.ERROR, ArduinoConst.CORE_PLUGIN_ID,
					"Internal Error: Toolchain Option " + optionid + " is not of type Boolean", e);
			AVRPlugin.getDefault().log(status);
		}
	}

}
