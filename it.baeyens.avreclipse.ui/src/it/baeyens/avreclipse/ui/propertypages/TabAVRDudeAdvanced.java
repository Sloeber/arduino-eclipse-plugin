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
 * $Id: TabAVRDudeAdvanced.java 851 2010-08-07 19:37:00Z innot $
 *     
 *******************************************************************************/
package it.baeyens.avreclipse.ui.propertypages;

import it.baeyens.avreclipse.core.properties.AVRDudeProperties;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;


/**
 * The AVRDude Advanced options Tab page.
 * <p>
 * On this tab, the following properties are edited:
 * <ul>
 * <li>The automatic verify check</li>
 * <li>The Signature check</li>
 * <li>Enable the no-Write / Simulation mode</li>
 * <li>Inhibit the auto flash erase</li>
 * </ul>
 * </p>
 * 
 * @author Thomas Holland
 * @since 2.2
 * 
 */
public class TabAVRDudeAdvanced extends AbstractAVRDudePropertyTab {

	// The GUI texts

	// No verify group
	private final static String	GROUP_NOVERIFY		= "Verify Check (-V)";
	private final static String	LABEL_NOVERIFY		= "Disabling the automatic verify check will improve upload time at the risk of unnoticed upload errors.";
	private final static String	TEXT_NOVERIFY		= "Disable automatic verify check";

	// No Signature check group
	private final static String	GROUP_NOSIGCHECK	= "Device Signature Check (-F)";
	private final static String	LABEL_NOSIGCHECK	= "Enable this if the target MCU has a broken (erased or overwritten) device signature\n"
															+ "but is otherwise operating normally.";
	private final static String	TEXT_NOSIGCHECK		= "Disable device signature check";

	// No write / simulation group
	private final static String	GROUP_NOWRITE		= "Simulation Mode (-n)";
	private final static String	LABEL_NOWRITE		= "Note: Even with this option set, AVRDude might still perform a chip erase.";
	private final static String	TEXT_NOWRITE		= "Simulation mode (no data is actually written to the device)";

	// no chip erase cylce group
	private final static String	GROUP_NOCHIPERASE	= "Auto Chip Erase Cycle (-D)";
	private final static String	LABEL_NOCHIPERASE	= "Normally a chip erase cycle is performed for each flash memory upload. \n"
															+ "Enable this to inhibit the auto chip erase.";
	private final static String	TEXT_NOCHIPERASE	= "Inhibit auto chip erase";

	// The GUI widgets
	private Button				fNoVerifyButton;

	private Button				fNoSigCheckButton;

	private Button				fNoWriteCheck;

	private Button				fNoChipEraseCheck;

	/** The Properties that this page works with */
	private AVRDudeProperties	fTargetProps;

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.cdt.ui.newui.AbstractCPropertyTab#createControls(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	public void createControls(Composite parent) {
		parent.setLayout(new GridLayout(1, false));

		addNoVerifySection(parent);

		addNoSignatureSection(parent);

		addNoWriteSection(parent);

		addNoChipEraseSection(parent);

	}

	/**
	 * Add the No Verify check button.
	 * 
	 * @param parent
	 *            <code>Composite</code>
	 */
	private void addNoVerifySection(Composite parent) {
		Group group = new Group(parent, SWT.NONE);
		group.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		group.setLayout(new GridLayout(1, false));
		group.setText(GROUP_NOVERIFY);

		Label label = new Label(group, SWT.WRAP);
		label.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		label.setText(LABEL_NOVERIFY);
		fNoVerifyButton = setupCheck(group, TEXT_NOVERIFY, 1, SWT.FILL);
	}

	/**
	 * Add the No Signature Check check button.
	 * 
	 * @param parent
	 *            <code>Composite</code>
	 */
	private void addNoSignatureSection(Composite parent) {
		Group group = new Group(parent, SWT.NONE);
		group.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		group.setLayout(new GridLayout(1, false));
		group.setText(GROUP_NOSIGCHECK);

		Label label = new Label(group, SWT.WRAP);
		label.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		label.setText(LABEL_NOSIGCHECK);
		fNoSigCheckButton = setupCheck(group, TEXT_NOSIGCHECK, 1, SWT.FILL);
	}

	/**
	 * Add the No Write / Simulate check button.
	 * 
	 * @param parent
	 *            <code>Composite</code>
	 */
	private void addNoWriteSection(Composite parent) {

		Group group = setupGroup(parent, GROUP_NOWRITE, 1, SWT.NONE);
		group.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1));

		setupLabel(group, LABEL_NOWRITE, 1, SWT.NONE);
		fNoWriteCheck = setupCheck(group, TEXT_NOWRITE, 1, SWT.CHECK);
	}

	/**
	 * Add the No Chip Erase check button.
	 * 
	 * @param parent
	 *            <code>Composite</code>
	 */
	private void addNoChipEraseSection(Composite parent) {

		Group group = setupGroup(parent, GROUP_NOCHIPERASE, 1, SWT.NONE);
		group.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1));

		setupLabel(group, LABEL_NOCHIPERASE, 1, SWT.NONE);
		fNoChipEraseCheck = setupCheck(group, TEXT_NOCHIPERASE, 1, SWT.CHECK);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.cdt.ui.newui.AbstractCPropertyTab#checkPressed(org.eclipse.swt.events.SelectionEvent)
	 */
	@Override
	protected void checkPressed(SelectionEvent e) {
		// This is called for all checkbuttons / tributtons which have been set
		// up with the setupXXX() calls

		Control source = (Control) e.widget;

		if (source.equals(fNoVerifyButton)) {
			// No Verify checkbox selected
			boolean noverify = fNoVerifyButton.getSelection();
			fTargetProps.setNoVerify(noverify);

		} else if (source.equals(fNoSigCheckButton)) {
			// No Signature checkbox selected
			boolean nosigcheck = fNoSigCheckButton.getSelection();
			fTargetProps.setNoSigCheck(nosigcheck);

		} else if (source.equals(fNoWriteCheck)) {
			// No Write = Simulation Checkbox has been selected
			// Write the new value to the target properties
			boolean newvalue = fNoWriteCheck.getSelection();
			fTargetProps.setNoWrite(newvalue);

		} else if (source.equals(fNoChipEraseCheck)) {
			// "No Chip Erase" checkbox selected
			boolean newvalue = fNoChipEraseCheck.getSelection();
			fTargetProps.setNoChipErase(newvalue);

		}

		updateAVRDudePreview(fTargetProps);
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
		dstprops.setNoVerify(fTargetProps.getNoVerify());
		dstprops.setNoSigCheck(fTargetProps.getNoSigCheck());
		dstprops.setNoWrite(fTargetProps.getNoWrite());
		dstprops.setNoChipErase(fTargetProps.getNoChipErase());
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see it.baeyens.avreclipse.ui.propertypages.AbstractAVRPropertyTab#performDefaults(it.baeyens.avreclipse.core.preferences.AVRProjectProperties)
	 */
	@Override
	protected void performCopy(AVRDudeProperties srcprops) {

		// Reload the items on this page
		fTargetProps.setNoVerify(srcprops.getNoVerify());
		fTargetProps.setNoSigCheck(srcprops.getNoSigCheck());
		fTargetProps.setNoWrite(srcprops.getNoWrite());
		fTargetProps.setNoChipErase(srcprops.getNoChipErase());
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

		// Update the GUI widgets on this Tab.
		fNoVerifyButton.setSelection(fTargetProps.getNoVerify());
		fNoSigCheckButton.setSelection(fTargetProps.getNoSigCheck());
		fNoWriteCheck.setSelection(fTargetProps.getNoWrite());
		fNoChipEraseCheck.setSelection(fTargetProps.getNoChipErase());
	}

}
