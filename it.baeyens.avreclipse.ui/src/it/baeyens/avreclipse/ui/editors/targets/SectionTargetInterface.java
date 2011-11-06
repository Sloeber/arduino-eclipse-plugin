/*******************************************************************************
 * 
 * Copyright (c) 2009, 2010 Thomas Holland (thomas@innot.de) and others
 * 
 * This program and the accompanying materials are made
 * available under the terms of the GNU Public License v3
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 * 
 * Contributors:
 *     Thomas Holland - initial API and implementation
 *     
 * $Id: SectionTargetInterface.java 851 2010-08-07 19:37:00Z innot $
 *     
 *******************************************************************************/

package it.baeyens.avreclipse.ui.editors.targets;

import it.baeyens.avreclipse.core.targets.AVRHardwareConfigValidator;
import it.baeyens.avreclipse.core.targets.IProgrammer;
import it.baeyens.avreclipse.core.targets.ITargetConfigConstants;
import it.baeyens.avreclipse.core.targets.ITargetConfiguration;
import it.baeyens.avreclipse.core.targets.ITargetConfigurationWorkingCopy;
import it.baeyens.avreclipse.core.targets.TargetInterface;

import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.VerifyEvent;
import org.eclipse.swt.events.VerifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Scale;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.IMessageManager;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;
import org.eclipse.ui.forms.widgets.TableWrapData;
import org.eclipse.ui.forms.widgets.TableWrapLayout;


/**
 * FormPart to edit all settings for the current target interface.
 * <p>
 * This part is implemented as a section.
 * </p>
 * 
 * @author Thomas Holland
 * @since 2.4
 * 
 */
public class SectionTargetInterface extends AbstractTCSectionPart implements ITargetConfigConstants {

	/** The list of target configuration attributes that this part manages. */
	private final static String[]	PART_ATTRS			= new String[] { //
														//
			ATTR_JTAG_CLOCK, //
			ATTR_DAISYCHAIN_ENABLE, //
			ATTR_DAISYCHAIN_UB, //
			ATTR_DAISYCHAIN_UA, //
			ATTR_DAISYCHAIN_BB, //
			ATTR_DAISYCHAIN_BA							};

	/** The list of target configuration attributes that cause this part to refresh. */
	private final static String[]	PART_DEPENDS		= new String[] { //
														//
			ATTR_PROGRAMMER_ID, //
			ATTR_FCPU									};

	/** the client area of the Section created by the superclass. */
	private Composite				fSectionClient;

	private Section					fFreqSection;
	private Scale					fFreqScale;
	private Label					fFreqText;

	private Section					fDaisyChainSection;
	/** The composite that contains the four daisy chain setting controls. */
	private Composite				fDaisyChainCompo;

	/** Map of the daisy chain attributes to their respective text controls. */
	private Map<String, Text>		fDaisyChainTexts	= new HashMap<String, Text>(4);

	/** the array of possible clock frequencies for the current programmer. */
	private int[]					fClockValues;

	/*
	 * (non-Javadoc)
	 * @see it.baeyens.avreclipse.ui.editors.targets.AbstractTargetConfigurationEditorPart#getTitle()
	 */
	@Override
	protected String getTitle() {
		// This is just a placeholder dummy.
		// The real name will be set in the refreshSectionContent() method.
		return "Host Interface";
	}

	/*
	 * (non-Javadoc)
	 * @see	 * it.baeyens.avreclipse.ui.editors.targets.AbstractTargetConfigurationEditorPart#getDescription()
	 */
	@Override
	protected String getDescription() {
		return null; // TODO: add a description
	}

	/*
	 * (non-Javadoc)
	 * @see	 * it.baeyens.avreclipse.ui.editors.targets.AbstractTargetConfigurationEditorPart#getPartAttributes
	 * ()
	 */
	@Override
	public String[] getPartAttributes() {
		return PART_ATTRS;
	}

	/*
	 * (non-Javadoc)
	 * @seeit.baeyens.avreclipse.ui.editors.targets.AbstractTargetConfigurationEditorPart#
	 * getDependentAttributes()
	 */
	@Override
	protected String[] getDependentAttributes() {
		return PART_DEPENDS;
	}

	/*
	 * (non-Javadoc)
	 * @see	 * it.baeyens.avreclipse.ui.editors.targets.AbstractTargetConfigurationEditorPart#getSectionStyle
	 * ()
	 */
	@Override
	protected int getSectionStyle() {
		return Section.TWISTIE | Section.SHORT_TITLE_BAR | Section.EXPANDED | Section.CLIENT_INDENT;
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.forms.AbstractFormPart#initialize(org.eclipse.ui.forms.IManagedForm)
	 */
	@Override
	public void createSectionContent(Composite parent, FormToolkit toolkit) {

		TableWrapLayout layout = new TableWrapLayout();
		layout.horizontalSpacing = 12;
		parent.setLayout(layout);

		fSectionClient = parent;

	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.forms.AbstractFormPart#refresh()
	 */
	@Override
	public void refreshSectionContent() {

		final ITargetConfigurationWorkingCopy tcwc = getTargetConfiguration();

		// Get the required information from the target configuration
		String programmerid = tcwc.getAttribute(ATTR_PROGRAMMER_ID);
		IProgrammer programmer = tcwc.getProgrammer(programmerid);
		TargetInterface newTI = programmer.getTargetInterface();
		fClockValues = programmer.getTargetInterfaceClockFrequencies();

		//
		// Clear the old section content
		//

		// First remove all old errors/warnings.
		// The MessageManager does not like disposed controls, so we have to remove
		// all messages first.
		// The warnings which are still valid are regenerated when the respective sections are
		// generated.
		IMessageManager mmngr = getMessageManager();
		if (fFreqScale != null && !fFreqScale.isDisposed()) {
			mmngr.removeMessages(fFreqScale);
		}
		for (Control textcontrol : fDaisyChainTexts.values()) {
			if (!textcontrol.isDisposed()) {
				mmngr.removeMessages(textcontrol);
			}
		}

		// then remove all old controls from the section
		Control[] children = fSectionClient.getChildren();
		for (Control child : children) {
			child.dispose();
		}

		// Finally reflow the form. Otherwise layout artifacts may remain behind.
		getManagedForm().reflow(true);

		//
		// redraw the complete section.
		//

		String title = MessageFormat.format("{0} Settings", newTI.toString());
		getControl().setText(title);

		// And rebuild the content
		FormToolkit toolkit = getManagedForm().getToolkit();

		Section section = null;

		// Add the BitClock section if the target configuration has some bitclock values.
		// The target configuration knows which programmers have a settable bitclock and
		// which have not.
		if (fClockValues.length != 0) {
			section = addClockSection(fSectionClient, toolkit);
			section.setLayoutData(new TableWrapData(TableWrapData.FILL_GRAB));
			fFreqSection = section;
		} else {
			fFreqSection = null;
		}

		// Add the Daisy Chain section if the target interface is capable of daisy chaining.
		if (programmer.isDaisyChainCapable()) {
			section = addJTAGDaisyChainSection(fSectionClient, toolkit);
			section.setLayoutData(new TableWrapData(TableWrapData.FILL_GRAB));
			fDaisyChainSection = section;
		} else {
			fDaisyChainSection = null;
		}

		// If the target interface has neither settable clocks nor is daisy chain capable, then add
		// a small dummy text telling the user that there is nothing to set.
		if (section == null) {
			// the selected target interface has no options
			Label label = toolkit.createLabel(fSectionClient,
					"The selected progrmmer has no user changeable settings for the "
							+ newTI.toString() + " target interface", SWT.WRAP);
			label.setLayoutData(new TableWrapData(TableWrapData.FILL_GRAB));

		}
	}

	/*
	 * (non-Javadoc)
	 * @see	 * it.baeyens.avreclipse.ui.editors.targets.AbstractTargetConfigurationEditorPart#refreshWarnings
	 * ()
	 */
	@Override
	public void refreshMessages() {
		validateBitClock();
		validateDaisyChain();
	}

	/**
	 * Add the bit bang delay setting section to the parent.
	 * <p>
	 * The Section contains the controls for the ATTR_JTAGCLOCK attribute.
	 * </p>
	 * <p>
	 * It is up to the caller to set the appropriate layout data on the returned
	 * <code>Section</code> control.
	 * </p>
	 * 
	 * @param parent
	 *            Composite to which the section is added.
	 * @param toolkit
	 *            FormToolkit to use for the new controls.
	 */
	private Section addClockSection(Composite parent, FormToolkit toolkit) {

		//
		// The Section
		//

		Section section = toolkit.createSection(parent, Section.TWISTIE | Section.CLIENT_INDENT);

		section.setText("Clock Frequency");
		String desc = "The clock frequency must not be higher that 1/4 of "
				+ "the target MCU clock frequency. The default value depends on the "
				+ "selected tool, but is usually 1 MHz, suitable for target MCUs running "
				+ "at 4 MHz or above.";

		int jtagclock = getTargetConfiguration().getIntegerAttribute(ATTR_JTAG_CLOCK);
		// Collapse the section if the current value is 0 (= default) to reduce clutter
		section.setExpanded(jtagclock != 0);

		//
		// The Section content
		//
		Composite sectionClient = toolkit.createComposite(section);
		sectionClient.setLayout(new TableWrapLayout());

		//
		// The description Label
		//
		Label description = toolkit.createLabel(sectionClient, desc, SWT.WRAP);
		description.setLayoutData(new TableWrapData(TableWrapData.FILL_GRAB));

		// 
		// The actual controls, wrapped in a Composite with a 2 column GridLayout
		//
		Composite content = toolkit.createComposite(sectionClient);
		content.setLayoutData(new TableWrapData(TableWrapData.FILL_GRAB));
		GridLayout gl = new GridLayout(2, false);
		gl.horizontalSpacing = 12;
		content.setLayout(gl);

		fFreqScale = new Scale(content, SWT.HORIZONTAL);
		toolkit.adapt(fFreqScale, true, true);
		fFreqScale.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

		fFreqScale.addSelectionListener(new SelectionAdapter() {

			/*
			 * (non-Javadoc)
			 * @seeorg.eclipse.swt.events.SelectionAdapter#widgetSelected(org.eclipse.swt.events.
			 * SelectionEvent)
			 */
			@Override
			public void widgetSelected(SelectionEvent e) {
				int index = fFreqScale.getSelection();
				int value = fClockValues[index];
				updateBitClockValue(value);
			}

		});

		// Set the scale properties.
		// we use an indirection: The scale does not set the Hz value directly.
		// instead it just selects the value from the fClockValues array.
		// The pageIncrements determine the number of ticks on the scale.
		// For up to 100 values in the bitclocks array we use 1 tick for each value.
		// For more than 100 values we use 1 tick for every 2 values.
		int units = fClockValues.length;
		fFreqScale.setMaximum(units - 1);
		fFreqScale.setMinimum(0);
		fFreqScale.setIncrement(1);
		fFreqScale.setPageIncrement(units < 100 ? 1 : 2);

		//
		// The frequency display.
		//
		fFreqText = toolkit.createLabel(content, "default", SWT.RIGHT);

		GridData gd = new GridData(SWT.FILL, SWT.CENTER, false, false);
		gd.widthHint = calcTextWidth(fFreqText, "88.888 MHz");
		fFreqText.setLayoutData(gd);

		// Finally set the scale and the label to the current setting (or the next lower if a
		// different ClockValues table is used)
		int value = getTargetConfiguration().getIntegerAttribute(ATTR_JTAG_CLOCK);

		// find next lower value
		int lastv = 0;
		int index = 0;
		for (; index < fClockValues.length; index++) {
			if (fClockValues[index] <= value) {
				lastv = fClockValues[index];
			} else {
				break;
			}
		}

		fFreqScale.setSelection(index - 1);

		// Update the value. This will in turn set the bitclock warning if required.
		updateBitClockValue(lastv);

		// Now tell the section about its content.
		section.setClient(sectionClient);

		return section;
	}

	/**
	 * Updates the bitclock attribute in the target configuration and validates it.
	 * 
	 * @param value
	 *            The new bitclock frequency.
	 */
	private void updateBitClockValue(int value) {

		// Set the attribute
		getTargetConfiguration().setIntegerAttribute(ATTR_JTAG_CLOCK, value);
		getManagedForm().dirtyStateChanged();

		// update the frequency display label

		fFreqText.setText(convertFrequencyToString(value));

		validateBitClock();

	}

	/**
	 * Convert a integer Hz value to a String.
	 * <p>
	 * The result has the unit appended:
	 * <ul>
	 * <li><code>Hz</code> for values below 1KHZ</li>
	 * <li><code>KHz</code> for values between 1 and 1000 KHz</li>
	 * <li><code>MHz</code> for values above 1000 KHz</li>
	 * </ul>
	 * As a special case the value <code>0</code> will result in "default".
	 * </p>
	 * 
	 * @param value
	 *            integer Hz value
	 * @return
	 */
	private String convertFrequencyToString(int value) {
		String text;
		if (value == 0) {
			text = "default";
		} else if (value < 1000) {
			text = value + " Hz";
		} else if (value < 1000000) {
			float newvalue = value / 1000.0F;
			text = newvalue + " KHz";
		} else {
			float newvalue = value / 1000000.0F;
			text = newvalue + " MHz";
		}
		return text;
	}

	/**
	 * Show or hide the 1/4th MCU frequency warning.
	 * 
	 * @see AVRHardwareConfigValidator#checkJTAGClock(ITargetConfiguration)
	 */
	private void validateBitClock() {

		validate(ATTR_JTAG_CLOCK, fFreqScale);

	}

	/**
	 * Add the JTAG daisy chain settings section to the parent.
	 * <p>
	 * The Section contains the controls for the ATTR_DAISYCHAIN_ENABLE and the four DAISYCHAIN_xx
	 * attributes.
	 * </p>
	 * <p>
	 * It is up to the caller to set the appropriate layout data on the returned
	 * <code>Section</code> control.
	 * </p>
	 * 
	 * @param parent
	 *            Composite to which the section is added.
	 * @param toolkit
	 *            FormToolkit to use for the new controls.
	 */
	private Section addJTAGDaisyChainSection(Composite parent, FormToolkit toolkit) {

		//
		// The Section
		//
		Section section = toolkit.createSection(parent, Section.TWISTIE | Section.CLIENT_INDENT);
		section.setText("Daisy Chain");
		String desc = "These settings are required if the target MCU is part of a JTAG daisy chain.\n"
				+ "Set the number of devices before and after the target MCU in the chain "
				+ "and the accumulated number of instruction bits they use. AVR devices use "
				+ "4 instruction bits, but other JTAG devices may differ. \n"
				+ "Note: JTAG daisy chains are only supported by some Programmers.";

		String enabledtext = getTargetConfiguration().getAttribute(ATTR_DAISYCHAIN_ENABLE);
		boolean enabled = Boolean.parseBoolean(enabledtext);

		// Collapse the section if Daisy chain is not enables to avoid clutter
		section.setExpanded(enabled);

		Composite sectionClient = toolkit.createComposite(section);
		sectionClient.setLayout(new TableWrapLayout());

		//
		// The section description label
		//
		Label description = toolkit.createLabel(sectionClient, desc, SWT.WRAP);
		description.setLayoutData(new TableWrapData(TableWrapData.FILL_GRAB));

		//
		// The Daisy Chain enable check button
		//
		boolean useDaisyChain = getTargetConfiguration()
				.getBooleanAttribute(ATTR_DAISYCHAIN_ENABLE);
		final Button enableButton = toolkit.createButton(sectionClient, "Enable daisy chain",
				SWT.CHECK);
		enableButton.setLayoutData(new TableWrapData(TableWrapData.FILL_GRAB));
		enableButton.setSelection(useDaisyChain);
		enableButton.addSelectionListener(new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				boolean isEnabled = enableButton.getSelection();
				setEnabled(fDaisyChainCompo, isEnabled);
				getTargetConfiguration().setBooleanAttribute(ATTR_DAISYCHAIN_ENABLE, isEnabled);
				getManagedForm().dirtyStateChanged();
				validateDaisyChain();
			}
		});

		// 
		// The actual daisy chain controls, wrapped in a Composite with a 4 column GridLayout
		//

		fDaisyChainCompo = toolkit.createComposite(sectionClient);
		fDaisyChainCompo.setLayoutData(new TableWrapData(TableWrapData.FILL));
		GridLayout layout = new GridLayout(4, false);
		layout.horizontalSpacing = 12;
		fDaisyChainCompo.setLayout(layout);

		createDCTextField(fDaisyChainCompo, "Devices before:", ATTR_DAISYCHAIN_UB);
		createDCTextField(fDaisyChainCompo, "Instruction bits before:", ATTR_DAISYCHAIN_BB);

		createDCTextField(fDaisyChainCompo, "Devices after:", ATTR_DAISYCHAIN_UA);
		createDCTextField(fDaisyChainCompo, "Instruction bits after:", ATTR_DAISYCHAIN_BA);

		setEnabled(fDaisyChainCompo, useDaisyChain);

		section.setClient(sectionClient);

		// Once we have created the controls we can validate the target configuration to set any
		// problem markers.
		validateDaisyChain();

		return section;
	}

	/**
	 * Create a single daisy chain settings text control with a label.
	 * <p>
	 * The created text control is added to the {@link #fDaisyChainTexts} map with the given
	 * attribute as the key.
	 * </p>
	 * 
	 * @param parent
	 *            The parent composite (with a GridLayout)
	 * @param labeltext
	 *            The text for the label
	 * @param attribute
	 *            The target configuration attribute.
	 */
	private void createDCTextField(Composite parent, String labeltext, String attribute) {

		FormToolkit toolkit = getManagedForm().getToolkit();

		final ModifyListener modifylistener = new ModifyListener() {

			@Override
			public void modifyText(ModifyEvent e) {
				// Get the Attribute of the text field and its value
				String attr = (String) e.widget.getData();
				String value = ((Text) e.widget).getText();
				if (value.length() == 0) {
					value = "0";
				}

				int intvalue = Integer.parseInt(value);
				getTargetConfiguration().setIntegerAttribute(attr, intvalue);
				getManagedForm().dirtyStateChanged();
				validateDaisyChain();
			}
		};

		// The verify listener to restrict the input to integers
		final VerifyListener verifylistener = new VerifyListener() {
			@Override
			public void verifyText(VerifyEvent event) {
				String text = event.text;
				if (!text.matches("[0-9]*")) {
					event.doit = false;
				}
			}
		};

		toolkit.createLabel(parent, labeltext);

		int currvalue = getTargetConfiguration().getIntegerAttribute(attribute);
		String currvaluestring = Integer.toString(currvalue);
		Text text = toolkit.createText(parent, currvaluestring, SWT.RIGHT);
		GridData gd = new GridData(SWT.FILL, SWT.NONE, false, false);
		gd.widthHint = calcTextWidth(text, "8888");
		text.setLayoutData(gd);
		text.setTextLimit(3);
		text.setData(attribute); // set the attribute for the modify listener
		text.addModifyListener(modifylistener);
		text.addVerifyListener(verifylistener);

		fDaisyChainTexts.put(attribute, text);

	}

	/**
	 * Add or remove the error messages for the daisy chain settings.
	 * 
	 * @see AVRHardwareConfigValidator#checkJTAGDaisyChainUnitsBefore(ITargetConfiguration)
	 * @see AVRHardwareConfigValidator#checkJTAGDaisyChainUnitsAfter(ITargetConfiguration)
	 * @see AVRHardwareConfigValidator#checkJTAGDaisyChainBitsBefore(ITargetConfiguration)
	 * @see AVRHardwareConfigValidator#checkJTAGDaisyChainBitsAfter(ITargetConfiguration)
	 */
	private void validateDaisyChain() {

		validate(ATTR_DAISYCHAIN_BB, fDaisyChainTexts.get(ATTR_DAISYCHAIN_BB));
		validate(ATTR_DAISYCHAIN_BA, fDaisyChainTexts.get(ATTR_DAISYCHAIN_BA));
		validate(ATTR_DAISYCHAIN_UB, fDaisyChainTexts.get(ATTR_DAISYCHAIN_UB));
		validate(ATTR_DAISYCHAIN_UA, fDaisyChainTexts.get(ATTR_DAISYCHAIN_UA));

	}

	/*
	 * (non-Javadoc)
	 * @see it.baeyens.avreclipse.ui.editors.targets.AbstractTCSectionPart#setFocus(java.lang.String)
	 */
	@Override
	public boolean setFocus(String attribute) {
		if (attribute.equals(ATTR_JTAG_CLOCK)) {
			if (fFreqScale != null && !fFreqScale.isDisposed()) {
				fFreqScale.setFocus();
			}
			if (fFreqSection != null && !fFreqSection.isDisposed()) {
				fFreqSection.setExpanded(true);
			}
			return true;
		}

		if (fDaisyChainTexts.containsKey(attribute)) {
			Text textcontrol = fDaisyChainTexts.get(attribute);
			if (textcontrol != null && !textcontrol.isDisposed()) {
				textcontrol.setFocus();
			}
			if (fDaisyChainSection != null && !fDaisyChainSection.isDisposed()) {
				fDaisyChainSection.setExpanded(true);
			}
			return true;
		}

		return false;
	}
}
