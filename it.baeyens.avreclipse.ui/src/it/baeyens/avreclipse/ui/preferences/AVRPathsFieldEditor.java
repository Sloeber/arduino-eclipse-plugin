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
 * $Id: AVRPathsFieldEditor.java 851 2010-08-07 19:37:00Z innot $
 *     
 *******************************************************************************/

package it.baeyens.avreclipse.ui.preferences;

import it.baeyens.avreclipse.core.paths.AVRPath;
import it.baeyens.avreclipse.core.paths.AVRPathManager;

import org.eclipse.jface.preference.FieldEditor;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;


/**
 * A custom field editor to edit all plugin paths.
 * 
 * This can be used on a FieldEditorPreferencePage to manage the the current path settings.
 * 
 * @author Thomas Holland
 * 
 */
public class AVRPathsFieldEditor extends FieldEditor {

	// GUI Widgets
	private Table				fTable;
	private Composite			fButtons;
	private Button				fEditButton;
	private Button				fRescanButton;
	private PreferencePage 		m_PrefPage; 

	private boolean				fValid		= true;

	// The three columns
	private static final int	COLUMN_NAME	= 0;
	private static final int	COLUMN_TYPE	= 1;
	private static final int	COLUMN_PATH	= 2;

	// fonts and colors used
	final Font					fBoldFont	= JFaceResources.getFontRegistry().getBold(
													JFaceResources.DIALOG_FONT);
	final Font					fDialogFont	= JFaceResources.getFontRegistry().get(
													JFaceResources.DIALOG_FONT);

	/**
	 * Handle selection events from the edit button.
	 * 
	 * It will open a PathSettingDialog, where the currently selected path can be modified.
	 */
	private class ButtonSelectionListener implements SelectionListener {

		@Override
		public void widgetDefaultSelected(SelectionEvent e) {
			widgetSelected(e);
		}

		@Override
		public void widgetSelected(SelectionEvent e) {
			// Get the selected item and get the associated path.
			// open a PathSettingDialog to modify the path.
			TableItem selected = fTable.getSelection()[0];
			AVRPathManager path = (AVRPathManager) selected.getData();

			if (e.getSource() == fEditButton) {
				PathSettingDialog dialog = new PathSettingDialog(fTable.getShell(), path);
				if (dialog.open() == Window.OK) {
					// OK Button selected:
					// get the modified Path, keep it and update this Editor
					path = dialog.getResult();
				}
			} else if (e.getSource() == fRescanButton) {
				// force a search for the current system path.
				// This may take a while so we display a Wait cursor
				final AVRPathManager finalpath = path;
				BusyIndicator.showWhile(fTable.getDisplay(), new Runnable() {
					@Override
					public void run() {
						finalpath.getSystemPath(true);
					}
				});
			}
			selected.setData(path);
			updateTableItem(selected);
			refreshValidState();
		}
	}

	/**
	 * Handle selection events from the table.
	 * 
	 * Once a TableItem has been selected, the Edit button is enabled
	 * 
	 */
	private class TableSelectionListener implements SelectionListener {

		@Override
		public void widgetDefaultSelected(SelectionEvent e) {
			widgetSelected(e);
		}

		@Override
		public void widgetSelected(SelectionEvent e) {
			// Enable the Edit button
			fEditButton.setEnabled(true);

			// Enable the rescan button of a system path item has been selected
			TableItem selected = fTable.getSelection()[0];
			AVRPathManager path = (AVRPathManager) selected.getData();
			switch (path.getSourceType()) {
				case System:
					fRescanButton.setEnabled(true);
					break;
				default:
					fRescanButton.setEnabled(false);
			}

		}

	}

	/**
	 * Constructor for the AVRPathsFieldEditor.
	 * 
	 * Sets the preference name (unused) and the labeltext (also unused) to fixed values.
	 */
	public AVRPathsFieldEditor(Composite parent, PreferencePage PrefPage) {
		
		super("avrpaths", "AVR Paths:", parent);
		m_PrefPage=PrefPage;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.preference.FieldEditor#adjustForNumColumns(int)
	 */
	@Override
	protected void adjustForNumColumns(int numColumns) {
		// Adjust the Layout for the given number of columns.
		// The button will get one column, the table all other columns

		GridData buttonsData = (GridData) fButtons.getLayoutData();
		buttonsData.horizontalSpan = 1;

		GridData tableData = (GridData) fTable.getLayoutData();
		tableData.horizontalSpan = numColumns - 1;

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.preference.FieldEditor#doFillIntoGrid(org.eclipse.swt.widgets.Composite,
	 *      int)
	 */
	@Override
	protected void doFillIntoGrid(Composite parent, int numColumns) {

		// Create the Table for all paths

		fTable = new Table(parent, SWT.SINGLE | SWT.BORDER | SWT.FULL_SELECTION);
		GridData tableData = new GridData(GridData.FILL_BOTH);
		tableData.horizontalSpan = numColumns - 1;
		fTable.setLayoutData(tableData);
		fTable.addSelectionListener(new TableSelectionListener());

		TableColumn nameColumn = new TableColumn(fTable, SWT.LEFT, COLUMN_NAME);
		TableColumn typeColumn = new TableColumn(fTable, SWT.LEFT, COLUMN_TYPE);
		TableColumn pathColumn = new TableColumn(fTable, SWT.LEFT, COLUMN_PATH);

		nameColumn.setText("Path to");
		typeColumn.setText("Source");
		pathColumn.setText("Current value");
		fTable.setHeaderVisible(true);

		// Creates the composite containing the button(s)

		fButtons = new Composite(parent, SWT.NO_FOCUS);

		GridData buttonsData = new GridData(GridData.END);
		buttonsData.horizontalSpan = 1;
		buttonsData.horizontalAlignment = SWT.FILL;
		fButtons.setLayoutData(buttonsData);

		FillLayout buttonsLayout = new FillLayout(SWT.VERTICAL);
		buttonsLayout.spacing = 5;
		fButtons.setLayout(buttonsLayout);

		// Create the edit Button

		fEditButton = new Button(fButtons, SWT.PUSH);

		fEditButton.setText("Edit...");
		fEditButton.addSelectionListener(new ButtonSelectionListener());
		fEditButton.setEnabled(false);

		// Create the rescan Button

		fRescanButton = new Button(fButtons, SWT.PUSH);

		fRescanButton.setText("Rescan");
		fRescanButton.addSelectionListener(new ButtonSelectionListener());
		fRescanButton.setEnabled(false);

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.preference.FieldEditor#doLoad()
	 */
	@Override
	protected void doLoad() {

		// Get the list of all supported paths
		AVRPath[] allpaths = AVRPath.values();

		for (AVRPath current : allpaths) {
			// Create a IPathManager for each path and store it
			// within a new TableItem
			AVRPathManager item = new AVRPathManager(current);

			TableItem ti = new TableItem(fTable, 0);
			ti.setData(item);
			updateTableItem(ti);

		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.preference.FieldEditor#doLoadDefault()
	 */
	@Override
	protected void doLoadDefault() {

		// Get all TableItems
		TableItem[] allitems = fTable.getItems();

		for (TableItem tableitem : allitems) {
			// get the IPathManager for the item and set it to the default value
			AVRPathManager path = (AVRPathManager) tableitem.getData();
			path.setToDefault();
			updateTableItem(tableitem);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.preference.FieldEditor#doStore()
	 */
	@Override
	protected void doStore() {

		// Get all TableItems
		TableItem[] allitems = fTable.getItems();

		for (TableItem tableitem : allitems) {
			// Get the IPathManager of the item and store its value to the
			// persistent storage
			AVRPathManager path = (AVRPathManager) tableitem.getData();
			path.store();
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.preference.FieldEditor#store()
	 */
	@Override
	public void store() {
		// We need to override this method, because we don't access the
		// PreferenceStore directly, but rather indirectly via the IPathManager
		// interface. super.store() tried to use setToDefault on the
		// PreferenceStore, which does not work here.
		if (getPreferenceStore() == null) {
			return;
		}
		doStore();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.preference.FieldEditor#getNumberOfControls()
	 */
	@Override
	public int getNumberOfControls() {
		// Table and Buttons
		return 2;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.preference.FieldEditor#isValid()
	 */
	@Override
	public boolean isValid() {
		return fValid;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.preference.FieldEditor#refreshValidState()
	 */
	@Override
	protected void refreshValidState() {
		super.refreshValidState();

		// Get all TableItems, extract their IPathManager and check
		// if it is valid and not optional.
		// If any one IPathManager is invalid, the
		// valid boolean is set to false and an error message indicating
		// the culprit is shown.

		TableItem[] allitems = fTable.getItems();
		boolean oldValid = fValid;
		boolean newValid = true;
		String invalidPath = null;

		for (TableItem ti : allitems) {
			AVRPathManager pathitem = (AVRPathManager) ti.getData();
			if (!pathitem.isValid() && !pathitem.isOptional()) {
				newValid = false;
				invalidPath = pathitem.getName();
			}
		}

		// Updates validity and error message.
		fValid = newValid;
		if (fValid == false) {
			showErrorMessage("Path for '" + invalidPath + "' is not valid");
		} else {
			clearErrorMessage();
		}

		// Send some notifications.
		if (newValid != oldValid) {
			fireStateChanged(IS_VALID, oldValid, newValid);
			m_PrefPage.setValid(newValid);
		}

	}

	/**
	 * Updates the visual of the given TableItem according to its IPathManager.
	 * 
	 * @param item
	 *            TableItem whose visual needs to be updated
	 */
	private void updateTableItem(TableItem item) {

		AVRPathManager path = (AVRPathManager) item.getData();

		// add warn / error icons if path is empty / invalid
		boolean valid = path.isValid();
		boolean optional = path.isOptional();
		boolean empty = (path.getPath().isEmpty());
		
		if (valid && !empty) {
			// valid path
			item.setImage((Image) null);
		} else if ((valid && empty) || (!valid && optional)) {
			// valid but empty path or invalid and optional path (use for optional paths)
			item.setImage(PlatformUI.getWorkbench().getSharedImages().getImage(
					ISharedImages.IMG_OBJS_WARN_TSK));
		} else {
			// Path is invalid
			item.setImage(PlatformUI.getWorkbench().getSharedImages().getImage(
					ISharedImages.IMG_OBJS_ERROR_TSK));
		}

		item.setText(COLUMN_NAME, path.getName());
		item.setText(COLUMN_TYPE, path.getSourceType().toString());
		item.setText(COLUMN_PATH, path.getPath().toOSString());

		// Adjust color/font according to source type
		switch (path.getSourceType()) {
			case System:
				item.setFont(COLUMN_TYPE, fDialogFont);
				item.setFont(COLUMN_PATH, fDialogFont);
				item.setForeground(COLUMN_PATH, fTable.getDisplay().getSystemColor(
						SWT.COLOR_DARK_GRAY));
				break;
			case Bundled:
				item.setFont(COLUMN_TYPE, fDialogFont);
				item.setFont(COLUMN_PATH, fDialogFont);
				item.setForeground(COLUMN_PATH, fTable.getDisplay().getSystemColor(
						SWT.COLOR_DARK_GRAY));
				break;
			case Custom:
				item.setFont(COLUMN_TYPE, fBoldFont);
				item.setFont(COLUMN_PATH, fBoldFont);
				item
						.setForeground(COLUMN_PATH, fTable.getDisplay().getSystemColor(
								SWT.COLOR_BLACK));
		}

		// Updates the table layout.
		fTable.getColumn(COLUMN_NAME).pack();
		fTable.getColumn(COLUMN_TYPE).pack();
		fTable.getColumn(COLUMN_PATH).pack();
		fTable.layout();

	}
}
