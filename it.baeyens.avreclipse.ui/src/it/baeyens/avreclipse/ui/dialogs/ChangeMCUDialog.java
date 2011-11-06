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
 * $Id: ChangeMCUDialog.java 851 2010-08-07 19:37:00Z innot $
 *     
 *******************************************************************************/
package it.baeyens.avreclipse.ui.dialogs;

import it.baeyens.avreclipse.core.toolinfo.fuses.ByteValues;
import it.baeyens.avreclipse.core.toolinfo.fuses.Fuses;
import it.baeyens.avreclipse.core.util.AVRMCUidConverter;

import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Set;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;


/**
 * Dialog to select a MCU.
 * <p>
 * The dialog offers the list of MCUs to the user. If the MCU selected by the user is not compatible
 * with the MCU given to the dialog at instantiation time then a warning is shown and the OK button
 * changes to "Convert".
 * </p>
 * 
 * @author Thomas Holland
 * @since 2.3
 * 
 */
public class ChangeMCUDialog extends TitleAreaDialog {

	/** The current ByteValues to check the compatibility with the selected MCU. */
	private final ByteValues	fSourceValues;

	/** The name of the file for the dialog title. */
	private final String		fFileName;

	/** Stores the id of the selected MCU. */
	private String				fNewMCU;

	// The GUI elements
	private Combo				fMCUCombo;
	private Button				fOKButton;

	private final static int	OK_ID			= IDialogConstants.OK_ID;
	private final static int	CANCEL_ID		= IDialogConstants.CANCEL_ID;

	private final static String	TEXT_OK			= IDialogConstants.OK_LABEL;
	private final static String	TEXT_CONVERT	= "Convert";

	/**
	 * Create a new MCU change dialog.
	 * <p>
	 * The dialog is not shown until {@link #open()} is called.
	 * </p>
	 * 
	 * @param shell
	 *            <code>Shell</code> to associate this Dialog with, so that it always stays on top
	 *            of the given Shell.
	 * @param sourcevalues
	 *            A source <code>ByteValues</code> object. Used to preselect the current MCU and
	 *            to test the compatibility with the selected MCU.
	 * @param filename
	 *            The name of the source file which will appear in the dialog title.
	 */
	public ChangeMCUDialog(Shell shell, ByteValues sourcevalues, String filename) {
		super(shell);

		fSourceValues = sourcevalues;
		fNewMCU = sourcevalues.getMCUId();
		fFileName = filename;

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.dialogs.TitleAreaDialog#createDialogArea(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	protected Control createDialogArea(Composite parent) {

		String title = MessageFormat.format("Change MCU for file {0}", fFileName);
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

		// The real content
		Composite body = new Composite(composite, SWT.NONE);
		body.setLayout(new GridLayout(2, false));

		Label label = new Label(body, SWT.NONE);
		label.setText("Select new MCU:");

		Set<String> allmcusset = getMCUList();
		final String[] allmcuids = allmcusset.toArray(new String[allmcusset.size()]);
		Arrays.sort(allmcuids);

		final String[] allmcunames = new String[allmcuids.length];

		int oldmcuindex = -1;
		for (int i = 0; i < allmcuids.length; i++) {
			allmcunames[i] = AVRMCUidConverter.id2name(allmcuids[i]);
			if (fSourceValues.getMCUId().equals(allmcuids[i])) {
				oldmcuindex = i;
			}
		}

		fMCUCombo = new Combo(body, SWT.READ_ONLY);
		fMCUCombo.setItems(allmcunames);
		fMCUCombo.setVisibleItemCount(Math.min(allmcunames.length, 25));
		fMCUCombo.select(oldmcuindex);
		fMCUCombo.addSelectionListener(new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				fNewMCU = allmcuids[fMCUCombo.getSelectionIndex()];
				boolean isCompatible = fSourceValues.isCompatibleWith(fNewMCU);
				fOKButton.setText(isCompatible ? TEXT_OK : TEXT_CONVERT);
				if (!isCompatible) {
					setMessage("Selected MCU is not compatible with current MCU.",
							IMessageProvider.WARNING);
				} else {
					setMessage(null);
				}
			}
		});

		return composite;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.dialogs.Dialog#createButtonsForButtonBar(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		// create OK and Cancel buttons by default
		fOKButton = createButton(parent, OK_ID, IDialogConstants.OK_LABEL, true);
		createButton(parent, CANCEL_ID, IDialogConstants.CANCEL_LABEL, false);
	}

	/**
	 * @return The MCU selected by the user.
	 */
	public String getResult() {
		return fNewMCU;
	}

	/**
	 * Get a set with all supported MCU id values.
	 * <p>
	 * The default implementation will get the list from the <code>Fuses</code> class. Subclasses
	 * may use a different supplier.
	 * </p>
	 * 
	 * @return <code>Set</code> with all MCUs that should be shown to the user.
	 */
	protected Set<String> getMCUList() {
		return Fuses.getDefault().getMCUList();
	}
}
