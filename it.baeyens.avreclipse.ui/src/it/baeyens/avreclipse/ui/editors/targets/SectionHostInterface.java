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
 * $Id: SectionHostInterface.java 851 2010-08-07 19:37:00Z innot $
 *     
 *******************************************************************************/

package it.baeyens.avreclipse.ui.editors.targets;

import it.baeyens.avreclipse.core.targets.HostInterface;
import it.baeyens.avreclipse.core.targets.ITargetConfigConstants;
import it.baeyens.avreclipse.core.targets.ITargetConfigurationWorkingCopy;

import java.text.MessageFormat;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.events.VerifyEvent;
import org.eclipse.swt.events.VerifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Hyperlink;
import org.eclipse.ui.forms.widgets.Section;
import org.eclipse.ui.forms.widgets.TableWrapData;
import org.eclipse.ui.forms.widgets.TableWrapLayout;


/**
 * FormPart to edit all settings for the current host interface.
 * 
 * @author Thomas Holland
 * @since 2.4
 * 
 */
public class SectionHostInterface extends AbstractTCSectionPart implements ITargetConfigConstants {

	/** Used as the default string for Port / Baudrate settings. */
	private final static String		DEFAULT			= "default";

	/** List of common baudrates incl. DEFAULT */
	private final static String[]	BAUDRATES		= new String[] { //
													//
			DEFAULT, //
			"1200", //
			"2400", //
			"4800", //
			"9600", //
			"19200", //
			"38400", //
			"57600", //
			"115200", //
			"230400"								};

	/** The list of target configuration attributes that this part manages. */
	private final static String[]	PART_ATTRS		= new String[] { //
													//
			ATTR_PROGRAMMER_PORT, //
			ATTR_PROGRAMMER_BAUD, //
			ATTR_BITBANGDELAY, //
			ATTR_PAR_EXITSPEC, //
			ATTR_USB_DELAY							};

	/** The list of target configuration attributes that cause this part to refresh. */
	private final static String[]	PART_DEPENDS	= new String[] { ATTR_HOSTINTERFACE };

	/** the client area of the Section created by the superclass. */
	private Composite				fSectionClient;

	/** The current exit spec for the /Reset line. */
	private String					fExitSpecReset;

	/** The current exit spec for the Vcc line. */
	private String					fExitSpecVcc;

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
	 * @see  it.baeyens.avreclipse.ui.editors.targets.AbstractTargetConfigurationEditorPart#getDescription()
	 */
	@Override
	protected String getDescription() {
		return null; // TODO: add a description
	}

	/*
	 * (non-Javadoc)
	 * @see  it.baeyens.avreclipse.ui.editors.targets.AbstractTargetConfigurationEditorPart#getPartAttributes
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
	 * @see it.baeyens.avreclipse.ui.editors.targets.AbstractTargetConfigurationEditorPart#getSectionStyle
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
		layout.numColumns = 2;
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
		String newHItxt = tcwc.getAttribute(ATTR_HOSTINTERFACE);
		HostInterface newHI = HostInterface.valueOf(newHItxt);

		//
		// Clear the old section content
		//

		// remove all previous controls from the section
		Control[] children = fSectionClient.getChildren();
		for (Control child : children) {
			child.dispose();
		}
		fSectionClient.layout(true, true);

		// Finally reflow the form. Otherwise layout artifacts may remain behind.
		getManagedForm().reflow(true);

		//
		// redraw the complete section.
		//

		String title = MessageFormat.format("{0} Settings", newHI.toString());
		getControl().setText(title);

		// rebuild the content
		FormToolkit toolkit = getManagedForm().getToolkit();
		Composite portCompo = toolkit.createComposite(fSectionClient);
		portCompo
				.setLayoutData(new TableWrapData(TableWrapData.FILL_GRAB, TableWrapData.TOP, 1, 2));
		portCompo.setLayout(new GridLayout(2, false));

		// All host interfaces have a port name.
		addPortCombo(portCompo, toolkit, newHI);

		Control section;

		// Add the host interface specific sections
		switch (newHI) {
			case SERIAL:
				addBaudRateCombo(portCompo, toolkit);
				break;

			case SERIAL_BB:
				section = addBitBangDelaySection(fSectionClient, toolkit);
				section.setLayoutData(new TableWrapData(TableWrapData.FILL_GRAB, TableWrapData.TOP,
						1, 2));
				break;

			case PARALLEL:
				section = addBitBangDelaySection(fSectionClient, toolkit);
				section.setLayoutData(new TableWrapData(TableWrapData.FILL_GRAB, TableWrapData.TOP,
						1, 2));
				section = addExitSpecsSection(fSectionClient, toolkit);
				section.setLayoutData(new TableWrapData(TableWrapData.FILL_GRAB, TableWrapData.TOP,
						1, 2));
				break;

			case USB:
				section = addUSBDelaySection(fSectionClient, toolkit);
				section
						.setLayoutData(new TableWrapData(TableWrapData.FILL, TableWrapData.TOP, 1,
								2));
				break;
		}

	}

	/**
	 * Add the port name settings controls to the parent.
	 * <p>
	 * This consists of a Label and a Text control. After creation the control is set to the current
	 * port name.
	 * </p>
	 * <p>
	 * The parent is expected to have a <code>GridLayout</code> with 2 columns.
	 * </p>
	 * 
	 * @param parent
	 *            Composite to which the port name settings controls are added.
	 * @param toolkit
	 *            FormToolkit to use for the new controls.
	 * @param hi
	 *            The current {@link HostInterface}.
	 */
	private void addPortCombo(Composite parent, FormToolkit toolkit, HostInterface hi) {

		final ITargetConfigurationWorkingCopy tcwc = getTargetConfiguration();

		//
		// The Label
		//
		Label label = toolkit.createLabel(parent, "Portname:");
		GridData gd = new GridData(SWT.BEGINNING, SWT.CENTER, false, false);
		gd.widthHint = calcTextWidth(label, "Baudrate:");
		label.setLayoutData(gd);

		// 
		// The Text control
		//
		// TODO: replace this with a combo that has some preset values.
		// For the PAR and SER ports this should be a list of previously entered names.
		// For the USB port this could be a list of autodetected devices.
		final Text combo = new Text(parent, SWT.BORDER);
		toolkit.adapt(combo, true, true);
		gd = new GridData(SWT.FILL, SWT.NONE, false, false);
		gd.widthHint = 200;
		combo.setLayoutData(gd);
		combo
				.setToolTipText("The host system port the programmer is attached to, e.g. '/dev/ttyS0' or 'com1'.\n"
						+ "Leave empty to use the default port (may not work for usb devices)");

		combo.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				String port = combo.getText();
				if (DEFAULT.equals(port)) {
					port = "";
				}
				tcwc.setAttribute(ATTR_PROGRAMMER_PORT, port);
				getManagedForm().dirtyStateChanged();
			}
		});

		// Set the current value for the port.
		// We check if the current value makes sense for the port.
		// If yes, then we leave the value like it is.
		// If no, then replace the current value with a default value
		// ("default" for SER/PAR ports, "usb" for USB ports)
		String port = tcwc.getAttribute(ATTR_PROGRAMMER_PORT);
		combo.setText(port);

		switch (hi) {
			case SERIAL:
			case SERIAL_BB:
				if (port.contains("com") // Windows
						|| port.contains("cua") // FreeBSD
						|| port.contains("tty") // Linux & MacOSX
						|| port.contains("term") // Solaris)
				) {
					// leave as is
					break;
				}
				// the previous setting probably does not represent a serial port
				combo.setText(DEFAULT);
				tcwc.setAttribute(ATTR_PROGRAMMER_PORT, "");
				break;

			case PARALLEL:
				if (port.contains("lpt") // Windows
						|| port.contains("ppi") // FreeBSD
						|| port.contains("parport") // Linux & MacOSX
						|| port.contains("printers") // Solaris)
				) {
					// leave as is
					break;
				}
				// the previous setting probablydoes not represent a parallel port
				combo.setText(DEFAULT);
				tcwc.setAttribute(ATTR_PROGRAMMER_PORT, "");
				break;

			case USB:
				if (port.contains("usb")) {
					// leave as is
					break;
				}

				// the previous setting probably does not represent a usb port
				combo.setText("usb");
				tcwc.setAttribute(ATTR_PROGRAMMER_PORT, "usb");
				break;

			default:
				// Unsupported HostInterface -- ignore (this is here to make the FindBugs happy)
				combo.setText("unknown");
		}
	}

	/**
	 * Add the baud rate settings controls to the parent.
	 * <p>
	 * This consists of a Label and a Combo control. The combo has a list of common baudrates as
	 * well as a "default" setting. After creation the control is set to the current baud rate
	 * value.
	 * </p>
	 * <p>
	 * The parent is expected to have a <code>GridLayout</code> with 2 columns.
	 * </p>
	 * 
	 * @param parent
	 *            Composite to which the setting controls are added.
	 * @param toolkit
	 *            FormToolkit to use for the new controls.
	 */
	private void addBaudRateCombo(Composite parent, FormToolkit toolkit) {

		final ITargetConfigurationWorkingCopy tcwc = getTargetConfiguration();

		//
		// The Label
		//
		Label label = toolkit.createLabel(parent, "Baudrate:");
		GridData gd = new GridData(SWT.BEGINNING, SWT.CENTER, false, false);
		gd.widthHint = calcTextWidth(label, "Baudrate:");
		label.setLayoutData(gd);

		// 
		// The Combo
		//
		final Combo combo = new Combo(parent, SWT.NONE);
		toolkit.adapt(combo, true, true);
		gd = new GridData(SWT.BEGINNING, SWT.CENTER, false, false);
		combo.setLayoutData(gd);
		combo
				.setToolTipText("Override the RS-232 connection baud rate specified in the respective programmer's entry of the configuration file.\nLeave empty to use the default");
		combo.setItems(BAUDRATES);
		combo.setVisibleItemCount(BAUDRATES.length);

		combo.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				String baudrate = combo.getText();
				if (DEFAULT.equals(baudrate)) {
					baudrate = "";
				}
				tcwc.setAttribute(ATTR_PROGRAMMER_BAUD, baudrate);
				getManagedForm().dirtyStateChanged();
			}
		});

		// The verify listener to restrict the input to integers
		combo.addVerifyListener(new VerifyListener() {
			@Override
			public void verifyText(VerifyEvent event) {
				String text = event.text;
				if (!text.matches("[0-9]*") && !text.equals(DEFAULT)) {
					event.doit = false;
				}
			}
		});

		// Set the current value for the port
		String baudrate = tcwc.getAttribute(ATTR_PROGRAMMER_BAUD);
		if (baudrate.length() == 0) {
			baudrate = DEFAULT;
		}
		combo.setText(baudrate);
	}

	/**
	 * Add the bit bang delay setting section to the parent.
	 * <p>
	 * The Section contains the controls for the ATTR_BITBANGDELAY attribute.
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
	private Section addBitBangDelaySection(Composite parent, FormToolkit toolkit) {

		final ITargetConfigurationWorkingCopy tcwc = getTargetConfiguration();

		//
		// The Section
		//
		Section section = toolkit.createSection(parent, Section.TWISTIE | Section.CLIENT_INDENT);

		section.setText("Bitbang delay");
		final String desc = "For bitbang-type programmers, delay for the set number of microseconds between each bit state change. \n"
				+ "If the host system is very fast, or the target runs off a slow clock "
				+ "(like a 32 kHz crystal, or the 128 kHz internal RC oscillator), this "
				+ "can become necessary to satisfy the requirement that the ISP clock "
				+ "frequency must not be higher than 1/4 of the CPU clock frequency.";

		String delay = tcwc.getAttribute(ATTR_BITBANGDELAY);
		if (delay.length() == 0) {
			// If there has been no value then collapse this section
			section.setExpanded(false);
		} else {
			section.setExpanded(true);
		}

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
		// The actual controls, wrapped in a Composite with a 3 column GridLayout
		//
		Composite content = toolkit.createComposite(sectionClient);
		content.setLayoutData(new TableWrapData(TableWrapData.FILL_GRAB));
		content.setLayout(new GridLayout(3, false));

		toolkit.createLabel(content, "delay:");

		//
		// The Text control
		//
		final Text text = toolkit.createText(content, delay, SWT.RIGHT);
		GridData gd = new GridData(SWT.FILL, SWT.NONE, false, false);
		gd.widthHint = calcTextWidth(text, "8888888888");
		text.setLayoutData(gd);

		text.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				String port = text.getText();
				tcwc.setAttribute(ATTR_BITBANGDELAY, port);
				getManagedForm().dirtyStateChanged();
			}
		});

		// The verify listener to restrict the input to integers
		text.addVerifyListener(new VerifyListener() {
			@Override
			public void verifyText(VerifyEvent event) {
				String text = event.text;
				if (!text.matches("[0-9]*")) {
					event.doit = false;
				}
			}
		});

		toolkit.createLabel(content, "µs");

		section.setClient(sectionClient);

		return section;
	}

	/**
	 * Add the bit bang delay setting section to the parent.
	 * <p>
	 * The Section contains the controls for the ATTR_PAR_EXITSPEC attribute.
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
	private Section addExitSpecsSection(Composite parent, FormToolkit toolkit) {

		final ITargetConfigurationWorkingCopy tcwc = getTargetConfiguration();

		//
		// The Section
		//
		Section section = toolkit.createSection(parent, Section.TWISTIE | Section.CLIENT_INDENT);

		section.setText("Parallel Port Exit Specs");
		final String desc = "By default, AVRDUDE leaves the parallel port "
				+ "in the same state on exit as it has been found at startup. "
				+ "These options modify the state of the `/RESET' and `Vcc' lines "
				+ "the parallel port is left at.\nSee the avrdude manual for more details.";

		String exitspecs = tcwc.getAttribute(ATTR_PAR_EXITSPEC);
		if (exitspecs.length() == 0) {
			// If there has been no value then collapse this section
			section.setExpanded(false);
		} else {
			section.setExpanded(true);
		}

		// Parse the exitspec string and set the
		// fExitSpecReset and fExitSpecVcc globals.
		// Not very elegant but robust.
		if (exitspecs.contains("noreset")) {
			fExitSpecReset = "noreset";
		} else if (exitspecs.contains("reset")) {
			fExitSpecReset = "reset";
		} else {
			fExitSpecReset = "";
		}

		if (exitspecs.contains("novcc")) {
			fExitSpecVcc = "novcc";
		} else if (exitspecs.contains("vcc")) {
			fExitSpecVcc = "vcc";
		} else {
			fExitSpecVcc = "";
		}

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
		content.setLayout(new GridLayout(2, true));

		//
		// The /Reset group
		//
		Group resetGroup = new Group(content, SWT.NONE);
		toolkit.adapt(resetGroup);
		resetGroup.setText("/Reset Line");
		resetGroup.setLayout(new RowLayout(SWT.VERTICAL));

		SelectionListener resetlistener = new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				String value = (String) e.widget.getData();
				fExitSpecReset = value;
				updateExitSpec();
			}
		};

		// Create the 3 buttons in the reset group
		Button button = toolkit.createButton(resetGroup, "Restore to previous state", SWT.RADIO);
		button.setData("");
		button.addSelectionListener(resetlistener);
		if ("".equals(fExitSpecReset)) {
			button.setSelection(true);
		}

		button = toolkit.createButton(resetGroup, "Activate (low) on exit", SWT.RADIO);
		button.setData("reset");
		button.addSelectionListener(resetlistener);
		if ("reset".equals(fExitSpecReset)) {
			button.setSelection(true);
		}

		button = toolkit.createButton(resetGroup, "Deactivate (high) on exit", SWT.RADIO);
		button.setData("noreset");
		button.addSelectionListener(resetlistener);
		if ("noreset".equals(fExitSpecReset)) {
			button.setSelection(true);
		}

		//
		// The Vcc group
		//
		Group vccGroup = new Group(content, SWT.NONE);
		toolkit.adapt(vccGroup);
		vccGroup.setText("Vcc Lines");
		vccGroup.setLayout(new RowLayout(SWT.VERTICAL));

		SelectionListener vcclistener = new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				String value = (String) e.widget.getData();
				fExitSpecVcc = value;
				updateExitSpec();
			}
		};

		// Create the 3 buttons in the vcc group
		button = toolkit.createButton(vccGroup, "Restore to previous state", SWT.RADIO);
		button.setData("");
		button.addSelectionListener(vcclistener);
		if ("".equals(fExitSpecVcc)) {
			button.setSelection(true);
		}

		button = toolkit.createButton(vccGroup, "Activate (high) on exit", SWT.RADIO);
		button.setData("vcc");
		button.addSelectionListener(vcclistener);
		if ("vcc".equals(fExitSpecVcc)) {
			button.setSelection(true);
		}

		button = toolkit.createButton(vccGroup, "Deactivate (low) on exit", SWT.RADIO);
		button.setData("novcc");
		button.addSelectionListener(vcclistener);
		if ("novcc".equals(fExitSpecVcc)) {
			button.setSelection(true);
		}

		section.setClient(sectionClient);

		return section;
	}

	/**
	 * Update the exit spec attribute in the target configuration.
	 * <p>
	 * This method takes the values from {@link #fExitSpecReset} and {@link #fExitSpecVcc}, combines
	 * them into a string (usable for the AVRDude -E option) and sets the string in the target
	 * configuration.
	 * </p>
	 */
	private void updateExitSpec() {
		StringBuilder sb = new StringBuilder(16);
		if (fExitSpecReset.length() != 0) {
			sb.append(fExitSpecReset);
			if (fExitSpecVcc.length() != 0) {
				sb.append(",");
			}
		}
		if (fExitSpecVcc.length() != 0) {
			sb.append(fExitSpecVcc);
		}

		getTargetConfiguration().setAttribute(ATTR_PAR_EXITSPEC, sb.toString());
		getManagedForm().dirtyStateChanged();
	}

	/**
	 * Add the bit bang delay setting section to the parent.
	 * <p>
	 * The Section contains the controls for the ATTR_USB_DELAY attribute.
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
	private Section addUSBDelaySection(Composite parent, FormToolkit toolkit) {

		//
		// The Section
		//
		Section section = toolkit.createSection(parent, Section.TWISTIE | Section.CLIENT_INDENT);

		section.setText("USB access delay");
		final String desc = "Some USB devices need a certain time to release the USB bus. "
				+ "As the AVR plugin sometimes accesses these devices in short succession "
				+ "a delay can be specified between two accesses. Set the delay when you "
				+ "get error messages that the selected usb port can't be opened.";

		Composite sectionClient = toolkit.createComposite(section);
		sectionClient.setLayout(new TableWrapLayout());

		Label description = toolkit.createLabel(sectionClient, desc, SWT.WRAP);
		description.setLayoutData(new TableWrapData(TableWrapData.FILL_GRAB));

		String delay = getTargetConfiguration().getAttribute(ATTR_USB_DELAY);
		if (delay.length() == 0) {
			// If there has been no value then collapse this section
			section.setExpanded(false);
		} else {
			section.setExpanded(true);
		}

		//
		// The Section content
		//
		Composite content = toolkit.createComposite(sectionClient);
		content.setLayoutData(new TableWrapData(TableWrapData.FILL_GRAB));
		content.setLayout(new GridLayout(4, false));

		//
		// The actual controls, wrapped in a Composite with a 3 column GridLayout
		//
		toolkit.createLabel(content, "delay:");

		final Text text = toolkit.createText(content, delay, SWT.RIGHT);
		GridData gd = new GridData(SWT.FILL, SWT.NONE, false, false);
		gd.widthHint = calcTextWidth(text, "8888888888");
		text.setLayoutData(gd);

		text.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				String port = text.getText();
				getTargetConfiguration().setAttribute(ATTR_USB_DELAY, port);
				getManagedForm().dirtyStateChanged();
			}
		});

		// The verify listener to restrict the input to integers
		text.addVerifyListener(new VerifyListener() {
			@Override
			public void verifyText(VerifyEvent event) {
				String text = event.text;
				if (!text.matches("[0-9]*")) {
					event.doit = false;
				}
			}
		});

		toolkit.createLabel(content, "ms");

		//
		// The "Test" button
		//
		Hyperlink testlink = toolkit.createHyperlink(content, "Test delay", SWT.NONE);
		testlink.setUnderlined(true);
		testlink.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, true, false));
		// TODO: implement the actual test

		section.setClient(sectionClient);

		return section;
	}

}
