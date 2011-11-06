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
 * $Id: TabAVRDudeProgrammer.java 851 2010-08-07 19:37:00Z innot $
 *     
 *******************************************************************************/
package it.baeyens.avreclipse.ui.propertypages;

import it.baeyens.avreclipse.core.avrdude.ProgrammerConfig;
import it.baeyens.avreclipse.core.properties.AVRDudeProperties;
import it.baeyens.avreclipse.ui.preferences.AVRDudeConfigEditor;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.VerifyEvent;
import org.eclipse.swt.events.VerifyListener;
import org.eclipse.swt.graphics.FontMetrics;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;


/**
 * The main / general AVRDude options tab.
 * <p>
 * On this tab, the following properties are edited:
 * <ul>
 * <li>Avrdude Programmer Configuration, incl. buttons to edit the current
 * config or add a new config</li>
 * <li>The JTAG BitClock</li>
 * <li>The BitBanger bit change delay</li>
 * </ul>
 * </p>
 * 
 * @author Thomas Holland
 * @since 2.2
 * 
 */
public class TabAVRDudeProgrammer extends AbstractAVRDudePropertyTab {

	// The GUI texts
	// Programmer config selection group
	private final static String GROUP_PROGCONFIG = "Programmer configuration";
	private final static String TEXT_EDITBUTTON = "Edit...";
	private final static String TEXT_NEWBUTTON = "New...";
	private final static String LABEL_CONFIG_WARNING = "The Programmer configuration previously associated with this project/configuration\n"
	        + "does not exist anymore. Please select a different one.";
	private final static String LABEL_NOCONFIG = "Please select a Programmer Configuration to enable avrdude functions";

	// JTAG Bitclock group
	private final static String GROUP_BITCLOCK = "JTAG ICE BitClock";
	private final static String LABEL_BITCLOCK = "Specify the bit clock period in microseconds for the JTAG interface or the ISP clock (JTAG ICE only).\n"
	        + "Set this to > 1.0 for target MCUs running with less than 4MHz on a JTAG ICE.\n"
	        + "Leave the field empty to use the preset bit clock period of the selected Programmer.";
	private final static String TEXT_BITCLOCK = "JTAG ICE bitclock";
	private final static String LABEL_BITCLOCK_UNIT = "µs";

	// BitBang delay group
	private final static String GROUP_DELAY = "BitBang Programmer Bit State Change Delay";
	private final static String LABEL_DELAY = "Specify the delay in microseconds for each bit change on bitbang-type programmers.\n"
	        + "Set this when the the host system is very fast, or the target runs off a slow clock\n"
	        + "Leave the field empty to run the ISP connection at max speed.";
	private final static String TEXT_DELAY = "Bit state change delay";
	private final static String LABEL_DELAY_UNIT = "µs";

	// The GUI widgets
	private Combo fProgrammerCombo;
	private Label fConfigWarningIcon;
	private Label fConfigWarningMessage;

	private Text fBitClockText;

	private Text fBitBangDelayText;

	/** The Properties that this page works with */
	private AVRDudeProperties fTargetProps;

	/** Warning image used for invalid Programmer Config values */
	private static final Image IMG_WARN = PlatformUI.getWorkbench().getSharedImages().getImage(
	        ISharedImages.IMG_OBJS_WARN_TSK);

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.cdt.ui.newui.AbstractCPropertyTab#createControls(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	public void createControls(Composite parent) {

		parent.setLayout(new GridLayout(1, false));

		addProgrammerConfigSection(parent);

		addBitClockSection(parent);

		addBitBangDelaySection(parent);

	}

	/**
	 * Add the Programmer Configuration selection <code>Combo</code> and the
	 * "Edit", "New" Buttons.
	 * 
	 * @param parent
	 *            <code>Composite</code>
	 */
	private void addProgrammerConfigSection(Composite parent) {

		Group configgroup = setupGroup(parent, GROUP_PROGCONFIG, 3, SWT.NONE);
		configgroup.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1));

		fProgrammerCombo = new Combo(configgroup, SWT.READ_ONLY);
		fProgrammerCombo.setLayoutData(new GridData(SWT.FILL, SWT.NONE, true, false));
		fProgrammerCombo.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				String selectedname = fProgrammerCombo
				        .getItem(fProgrammerCombo.getSelectionIndex());
				String selectedid = getProgrammerConfigId(selectedname);
				fTargetProps.setProgrammerId(selectedid);
				showProgrammerWarning("", false);
				updateAVRDudePreview(fTargetProps);
			}
		});
		// Init the combo with the list of available programmer configurations
		loadProgrammerConfigs();

		// Edit... Button
		Button editButton = setupButton(configgroup, TEXT_EDITBUTTON, 1, SWT.NONE);
		editButton.setBackground(parent.getBackground());
		editButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				editButtonAction(false);
			}
		});

		// New... Button
		Button newButton = setupButton(configgroup, TEXT_NEWBUTTON, 1, SWT.NONE);
		newButton.setBackground(parent.getBackground());
		newButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				editButtonAction(true);
			}
		});

		// The Warning icon / message composite
		Composite warningComposite = new Composite(configgroup, SWT.NONE);
		warningComposite.setLayoutData(new GridData(SWT.FILL, SWT.NONE, true, false, 3, 1));
		GridLayout gl = new GridLayout(2, false);
		gl.marginHeight = 0;
		gl.marginWidth = 0;
		gl.verticalSpacing = 0;
		gl.horizontalSpacing = 0;
		warningComposite.setLayout(gl);

		fConfigWarningIcon = new Label(warningComposite, SWT.LEFT);
		fConfigWarningIcon.setLayoutData(new GridData(SWT.BEGINNING, SWT.BEGINNING, false, false));
		fConfigWarningIcon.setImage(IMG_WARN);

		fConfigWarningMessage = new Label(warningComposite, SWT.LEFT);
		fConfigWarningMessage.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		fConfigWarningMessage.setText("two-line\ndummy");

		// By default make the warning invisible
		// updateData() will make it visible when required
		fConfigWarningIcon.setVisible(false);
		fConfigWarningMessage.setVisible(false);

	}

	/**
	 * The JTAG bitclock section.
	 * <p>
	 * The primary control in this section is a text field, that accepts only
	 * floating point numbers.
	 * </p>
	 * 
	 * @param parent
	 *            <code>Composite</code>
	 */
	private void addBitClockSection(Composite parent) {

		// TODO this could be replaced by a combo to select standard values.
		// Also this could be implemented as a frequency selector like in AVR
		// Studio
		// However, investigations into the avrdude source code indicate, that
		// the different programmer backends interpret this number differently.
		// Especially the stk500v2 backend interprets this number relative to the
		// clock frequency of the stk500 programmer.

		Group group = new Group(parent, SWT.NONE);
		group.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		group.setLayout(new GridLayout(3, false));
		group.setText(GROUP_BITCLOCK);

		Label label = new Label(group, SWT.WRAP);
		label.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 3, 1));
		label.setText(LABEL_BITCLOCK);

		setupLabel(group, TEXT_BITCLOCK, 1, SWT.NONE);

		fBitClockText = new Text(group, SWT.BORDER | SWT.RIGHT);
		GridData gd = new GridData(SWT.FILL, SWT.FILL, false, false);
		FontMetrics fm = getFontMetrics(parent);
		gd.widthHint = Dialog.convertWidthInCharsToPixels(fm, 12);
		fBitClockText.setLayoutData(gd);
		fBitClockText.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				fTargetProps.setBitclock(fBitClockText.getText());
				updateAVRDudePreview(fTargetProps);
			}
		});
		fBitClockText.addVerifyListener(new VerifyListener() {
			@Override
			public void verifyText(VerifyEvent event) {
				// Accept only digits and -at most- one dot '.'
				int dotcount = 0;
				if (fBitClockText.getText().contains(".")) {
					dotcount++;
				}
				String text = event.text;
				for (int i = 0; i < text.length(); i++) {
					char ch = text.charAt(i);
					if (ch == '.') {
						dotcount++;
						if (dotcount > 1) {
							event.doit = false;
							return;
						}
					} else if (!('0' <= ch && ch <= '9')) {
						event.doit = false;
						return;
					}
				}
			}
		});

		// Label with the units (microseconds)
		setupLabel(group, LABEL_BITCLOCK_UNIT, 1, SWT.FILL);
	}

	/**
	 * The BitBang bit change delay section.
	 * <p>
	 * The primary control in this section is a text field, that accepts only
	 * integers.
	 * </p>
	 * 
	 * @param parent
	 *            <code>Composite</code>
	 */
	private void addBitBangDelaySection(Composite parent) {

		Group group = new Group(parent, SWT.NONE);
		group.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		group.setLayout(new GridLayout(3, false));
		group.setText(GROUP_DELAY);

		Label label = new Label(group, SWT.WRAP);
		label.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 3, 1));
		label.setText(LABEL_DELAY);

		setupLabel(group, TEXT_DELAY, 1, SWT.NONE);

		fBitBangDelayText = new Text(group, SWT.BORDER | SWT.RIGHT);
		GridData gd = new GridData(SWT.FILL, SWT.FILL, false, false);
		FontMetrics fm = getFontMetrics(parent);
		gd.widthHint = Dialog.convertWidthInCharsToPixels(fm, 12);
		fBitBangDelayText.setLayoutData(gd);
		fBitBangDelayText.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				fTargetProps.setBitBangDelay(fBitBangDelayText.getText());
				updateAVRDudePreview(fTargetProps);
			}
		});
		fBitBangDelayText.addVerifyListener(new VerifyListener() {
			@Override
			public void verifyText(VerifyEvent event) {
				// Accept only digits
				String text = event.text;
				for (int i = 0; i < text.length(); i++) {
					char ch = text.charAt(i);
					if (!('0' <= ch && ch <= '9')) {
						event.doit = false;
						return;
					}
				}
			}
		});

		// Label with the units (microseconds)
		setupLabel(group, LABEL_DELAY_UNIT, 1, SWT.FILL);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see it.baeyens.avreclipse.ui.propertypages.AbstractAVRPropertyTab#performApply(it.baeyens.avreclipse.core.preferences.AVRProjectProperties)
	 */
	@Override
	protected void performApply(AVRDudeProperties dstprops) {

		// Save all new / modified programmer configurations
		saveProgrammerConfigs();

		// Copy the currently selected values of this tab to the given, fresh
		// Properties.
		// The caller of this method will handle the actual saving
		dstprops.setProgrammerId(fTargetProps.getProgrammerId());
		dstprops.setBitclock(fTargetProps.getBitclock());
		dstprops.setBitBangDelay(fTargetProps.getBitBangDelay());
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see it.baeyens.avreclipse.ui.propertypages.AbstractAVRPropertyTab#performDefaults()
	 */
	@Override
	protected void performDefaults() {

		// Reset the list of Programmer Configurations
		loadProgrammerConfigs();

		// The other defaults related stuff is done in the performCopy() method,
		// which is called later by the superclass.
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see it.baeyens.avreclipse.ui.propertypages.AbstractAVRPropertyTab#performDefaults(it.baeyens.avreclipse.core.preferences.AVRProjectProperties)
	 */
	@Override
	protected void performCopy(AVRDudeProperties srcprops) {

		// Reload the items on this page
		fTargetProps.setProgrammerId(srcprops.getProgrammerId());
		fTargetProps.setBitclock(srcprops.getBitclock());
		fTargetProps.setBitBangDelay(srcprops.getBitBangDelay());
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

		// Set the selection of the Programmercombo
		// If the programmerid of the target properties does not exist,
		// show a warning and select the first item (without copying it into the
		// Target Properties)
		String programmerid = fTargetProps.getProgrammerId();
		if (programmerid.length() == 0) {
			// No Programmer has been set yet
			// Deselect the combo and show a Message
			fProgrammerCombo.deselect(fProgrammerCombo.getSelectionIndex());
			showProgrammerWarning(LABEL_NOCONFIG, false);
		} else {
			// Programmer id exists. Now test if it is still valid
			if (!isValidId(programmerid)) {
				// id is not valid. Deselect Combo and show a Warning
				fProgrammerCombo.deselect(fProgrammerCombo.getSelectionIndex());
				showProgrammerWarning(LABEL_CONFIG_WARNING, true);
			} else {
				// everything is good. Select the id in the combo
				String programmername = getProgrammerConfigName(programmerid);
				int index = fProgrammerCombo.indexOf(programmername);
				fProgrammerCombo.select(index);
				showProgrammerWarning("", false);
			}
		}

		fBitClockText.setText(fTargetProps.getBitclock());
		fBitBangDelayText.setText(fTargetProps.getBitBangDelay());

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see it.baeyens.avreclipse.ui.propertypages.AbstractAVRDudePropertyTab#doProgConfigsChanged(java.lang.String[],
	 *      int)
	 */
	@Override
	protected void doProgConfigsChanged(String[] configs, int newindex) {

		fProgrammerCombo.setItems(configs);

		// make the combo show all available items (no scrollbar)
		fProgrammerCombo.setVisibleItemCount(configs.length);

		if (newindex != -1) {
			fProgrammerCombo.select(newindex);
		} else {
			fProgrammerCombo.deselect(fProgrammerCombo.getSelectionIndex());
		}
	};

	/**
	 * Adds a new configuration or edits the currently selected Programmer
	 * Configuration.
	 * <p>
	 * Called when either the new or the edit button has been clicked.
	 * </p>
	 * 
	 * @see AVRDudeConfigEditor
	 */
	private void editButtonAction(boolean createnew) {
		ProgrammerConfig oldconfig = null;

		// Create a list of all currently available configurations
		// This is used by the editor to avoid name clashes
		// (a configuration name needs to be unique)
		String[] allcfgs = fProgrammerCombo.getItems();
		Set<String> allconfignames = new HashSet<String>(allcfgs.length);
		for (String cfg : allcfgs) {
			allconfignames.add(cfg);
		}

		if (createnew) { // new config
			// Create a new configuration with a default name
			// (with a trailing running number if required),
			// a sample Description text and stk500v2 as programmer
			// (because I happen to have one)
			// All other options remain at the default (empty)
			String basename = "New Configuration";
			String defaultname = basename;
			int i = 1;
			while (allconfignames.contains(defaultname)) {
				defaultname = basename + " (" + i++ + ")";
			}
			oldconfig = fCfgManager.createNewConfig();
			oldconfig.setName(defaultname);
		} else { // edit existing config
			// Get the ProgrammerConfig from the Combo
			String configname = allcfgs[fProgrammerCombo.getSelectionIndex()];
			String configid = getProgrammerConfigId(configname);
			oldconfig = getProgrammerConfig(configid);
		}

		// Open the Config Editor.
		// If the OK Button was selected, the modified Config is fetched from
		// the Dialog and the the superclass is informed about the addition /
		// modification.
		AVRDudeConfigEditor dialog = new AVRDudeConfigEditor(fProgrammerCombo.getShell(),
		        oldconfig, allconfignames);
		if (dialog.open() == Window.OK) {
			// OK Button selected:
			ProgrammerConfig newconfig = dialog.getResult();
			fTargetProps.setProgrammer(newconfig);

			addProgrammerConfig(newconfig);
			updateData(fTargetProps.getParent());
		}
	}

	/**
	 * Show the supplied Warning in the Programmer config group.
	 * 
	 * @param text
	 *            Message to display.
	 * @param warning
	 *            <code>true</code> to make the warning visible,
	 *            <code>false</code> to hide it.
	 */
	private void showProgrammerWarning(String text, boolean warning) {
		fConfigWarningIcon.setVisible(warning);
		fConfigWarningMessage.setText(text);
		fConfigWarningMessage.pack();
		fConfigWarningMessage.setVisible(true);
	}

}
