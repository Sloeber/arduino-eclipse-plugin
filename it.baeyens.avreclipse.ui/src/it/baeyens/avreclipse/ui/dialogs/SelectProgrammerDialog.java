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
 * $Id: SelectProgrammerDialog.java 851 2010-08-07 19:37:00Z innot $
 *     
 *******************************************************************************/
package it.baeyens.avreclipse.ui.dialogs;

import it.baeyens.avreclipse.core.avrdude.ProgrammerConfig;
import it.baeyens.avreclipse.core.avrdude.ProgrammerConfigManager;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;


/**
 * Dialog to select a Programmer.
 * <p>
 * The dialog offers the list of Programmers to the user.
 * </p>
 * 
 * @author Thomas Holland
 * @since 2.3
 * 
 */
public class SelectProgrammerDialog extends TitleAreaDialog {

	/** The ID of the programmer selected by the user. */
	private String				fSelectedID;

	private Combo				fProgrammerCombo;

	private Map<String, String>	fIdToNameMap;
	private Map<String, String>	fNameToIdMap;

	private String[]			fAllNames;

	/** Contains the name of the programmer to pre-select when the dialog is opened. */
	private String				fPreselectName;

	/**
	 * Create a new Programmer selection dialog.
	 * <p>
	 * The dialog is not shown until {@link #open()} is called.
	 * </p>
	 * 
	 * @param shell
	 *            <code>Shell</code> to associate this Dialog with, so that it always stays on top
	 *            of the given Shell.
	 * @param preselect
	 *            A <code>ProgrammerConfig</code> which will be preselected when the dialog is
	 *            opened. May be <code>null</code>, in which case the first programmer in the
	 *            list will be preselected.
	 */
	public SelectProgrammerDialog(Shell shell, ProgrammerConfig preselect) {
		super(shell);

		initProgrammerList();

		if (preselect != null) {
			fPreselectName = preselect.getName();
		}

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.dialogs.TitleAreaDialog#createDialogArea(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	protected Control createDialogArea(Composite parent) {

		String title = "Select Programmer to read the MCU";
		setTitle(title);

		// create the top level composite for the dialog area
		Composite composite = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		layout.verticalSpacing = 0;
		layout.horizontalSpacing = 0;
		composite.setLayout(layout);
		composite.setLayoutData(new GridData(GridData.FILL_BOTH));
		composite.setFont(parent.getFont());

		// Build a separator line. This is just eye candy.
		Label titleBarSeparator = new Label(composite, SWT.HORIZONTAL | SWT.SEPARATOR);
		titleBarSeparator.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1));

		Composite body = new Composite(composite, SWT.NONE);
		body.setLayout(new GridLayout(2, false));

		if (fAllNames.length == 0) {
			// Show an error message when no programmers are available
			setErrorMessage("No programmer configurations available.");

			Label errorlabel = new Label(body, SWT.WRAP);
			errorlabel
					.setText("Please go to the AVRDude preferences and add at least one programmer configuration.\n\n"
							+ "(Window > Preferences... > AVR > AVRDude).");
			errorlabel.setLayoutData(new GridData(SWT.BEGINNING, SWT.TOP, true, true, 2, 1));

			return composite;
		}

		// at least one programmer is available -> continue normally

		Label label = new Label(body, SWT.NONE);
		label.setText("Select programmer:");

		fProgrammerCombo = new Combo(body, SWT.READ_ONLY);
		fProgrammerCombo.setItems(fAllNames);
		fProgrammerCombo.setVisibleItemCount(Math.min(fAllNames.length, 25));
		fProgrammerCombo.addSelectionListener(new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				String name = fProgrammerCombo.getText();
				fSelectedID = fNameToIdMap.get(name);
			}
		});

		if (fPreselectName != null) {
			int index = fProgrammerCombo.indexOf(fPreselectName);
			fProgrammerCombo.select(index);
			fSelectedID = fNameToIdMap.get(fPreselectName);
		} else {
			fProgrammerCombo.select(0);
			fSelectedID = fNameToIdMap.get(fAllNames[0]);
		}

		return composite;
	}

	/**
	 * @return The <code>ProgrammerConfig</code> for the configuration selected by the user
	 */
	public ProgrammerConfig getResult() {

		return ProgrammerConfigManager.getDefault().getConfig(fSelectedID);
	}

	/**
	 * Initialize the array of Programmer names and the map to the ProgrammerConfig id values.
	 */
	protected void initProgrammerList() {
		ProgrammerConfigManager pcmgr = ProgrammerConfigManager.getDefault();

		fIdToNameMap = pcmgr.getAllConfigNames();
		fNameToIdMap = new HashMap<String, String>();
		fAllNames = new String[fIdToNameMap.size()];
		int i = 0;
		for (String id : fIdToNameMap.keySet()) {
			String name = fIdToNameMap.get(id);
			fNameToIdMap.put(name, id);
			fAllNames[i++] = name;
		}

		Arrays.sort(fAllNames);

		return;
	}

}
