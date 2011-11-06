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
 * $Id: ProgConfigListFieldEditor.java 851 2010-08-07 19:37:00Z innot $
 *     
 *******************************************************************************/
package it.baeyens.avreclipse.ui.preferences;

import it.baeyens.arduino.globals.ArduinoConst;
import it.baeyens.avreclipse.AVRPlugin;
import it.baeyens.avreclipse.core.avrdude.ProgrammerConfig;
import it.baeyens.avreclipse.core.avrdude.ProgrammerConfigManager;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.preference.FieldEditor;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Widget;
import org.osgi.service.prefs.BackingStoreException;


/**
 * A special Field Editor to edit the list of AVRDude programmer configurations.
 * <p>
 * This editor has a Table of all Programmer Configurations, which can be edited, removed and added.
 * </p>
 * <p>
 * It does not work on a PreferenceStore, because the list of all configurations can only be
 * gathered directly from the Preferences. It does however support the Apply, Cancel and partially
 * the Defaults actions of a FieldEditorPreferencePage.
 * </p>
 * <p>
 * All modifications of programmer configurations are only persisted when the OK or Apply actions
 * occur.
 * </p>
 * 
 * @author Thomas Holland
 * @since 2.2
 * 
 */
public class ProgConfigListFieldEditor extends FieldEditor {

	/** The Table Control */
	private Table							fTableControl;

	/** The button box Composite containing the Add, Remove and Edit buttons */
	private Composite						fButtonComposite;

	// The GUI Widgets
	private Button							fAddButton;
	private Button							fRemoveButton;
	private Button							fEditButton;

	/**
	 * The list of removed configurations. They will be removed in the {@link #doStore()} method
	 */
	private List<ProgrammerConfig>			fRemovedConfigs;

	private final ProgrammerConfigManager	fCfgManager	= ProgrammerConfigManager.getDefault();

	/**
	 * Creates a AVRDude Programmers Configuration List field editor.
	 * <p>
	 * Because this field editor does not work on a PreferenceStore, it does not need to be passed
	 * to the constructor.
	 * </p>
	 * 
	 * @param labelText
	 *            the label text of the field editor
	 * @param parent
	 *            the parent of the field editor's control
	 */
	public ProgConfigListFieldEditor(String label, Composite parent) {
		super();
		super.setLabelText(label);
		createControl(parent);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.preference.FieldEditor#adjustForNumColumns(int)
	 */
	@Override
	protected void adjustForNumColumns(int numColumns) {
		// The Table gets all but one column, the ButtonBox one column.
		Control control = getLabelControl();
		((GridData) control.getLayoutData()).horizontalSpan = numColumns;
		((GridData) fTableControl.getLayoutData()).horizontalSpan = numColumns - 1;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.preference.FieldEditor#doFillIntoGrid(org.eclipse.swt.widgets.Composite,
	 *      int)
	 */
	@Override
	protected void doFillIntoGrid(Composite parent, int numColumns) {
		// Add the three controls:
		// Label, Table and Buttonbox
		Control control = getLabelControl(parent);
		GridData gd = new GridData();
		gd.horizontalSpan = numColumns;
		control.setLayoutData(gd);

		fTableControl = getTableControl(parent);
		gd = new GridData(SWT.FILL, SWT.FILL, true, true, numColumns - 1, 1);
		fTableControl.setLayoutData(gd);

		fButtonComposite = getButtonBoxComposite(parent);
		gd = new GridData();
		gd.verticalAlignment = GridData.BEGINNING;
		fButtonComposite.setLayoutData(gd);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.preference.FieldEditor#doLoad()
	 */
	@Override
	protected void doLoad() {
		if (fTableControl != null) {
			Set<String> allconfigids = fCfgManager.getAllConfigIDs();
			for (String configid : allconfigids) {
				if (configid.length() > 0) {
					ProgrammerConfig config = fCfgManager.getConfig(configid);
					TableItem item = new TableItem(fTableControl, SWT.NONE);
					item.setText(new String[] { config.getName(), config.getDescription() });
					item.setData(config);
				}
			}
		}
		// init the list of removed configurations
		fRemovedConfigs = new ArrayList<ProgrammerConfig>();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.preference.FieldEditor#doLoadDefault()
	 */
	@Override
	protected void doLoadDefault() {
		// No defaults supported for the List of Programmer Configurations
		// however we just reload the existing configurations. (loosing all
		// modifications)
		fTableControl.removeAll();
		doLoad();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.preference.FieldEditor#doStore()
	 */
	@Override
	protected void doStore() {
		// Save all configs still in the table, then remove all configs marked
		// for removal

		TableItem[] allitems = fTableControl.getItems();

		// save all current configs to the persistent store
		for (TableItem item : allitems) {
			ProgrammerConfig config = (ProgrammerConfig) item.getData();
			try {
				fCfgManager.saveConfig(config);
			} catch (BackingStoreException e) {
				IStatus status = new Status(Status.ERROR, ArduinoConst.CORE_PLUGIN_ID,
						"Can't save Programmer Configuration [" + config.getName()
								+ "] to the preference storage area", e);
				AVRPlugin.getDefault().log(status);
				ErrorDialog.openError(fTableControl.getShell(), "Programmer Configuration Error",
						null, status);
			}
		}

		// now delete the Configs marked as removed
		for (ProgrammerConfig config : fRemovedConfigs) {
			try {
				fCfgManager.deleteConfig(config);
			} catch (BackingStoreException e) {
				IStatus status = new Status(Status.ERROR, ArduinoConst.CORE_PLUGIN_ID,
						"Can't delete Programmer Configuration [" + config.getName()
								+ "] from the preference storage area", e);
				AVRPlugin.getDefault().log(status);
				ErrorDialog.openError(fTableControl.getShell(), "Programmer Configuration Error",
						null, status);
			}
		}
		fRemovedConfigs.clear();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.preference.FieldEditor#getNumberOfControls()
	 */
	@Override
	public int getNumberOfControls() {
		// Two: List and Buttons Composite
		return 2;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.preference.FieldEditor#setFocus()
	 */
	@Override
	public void setFocus() {
		if (fTableControl != null) {
			fTableControl.setFocus();
		}
	}

	/**
	 * Returns this field editors Table control.
	 * 
	 * @param parent
	 *            the parent control
	 * @return the list control
	 */
	public Table getTableControl(Composite parent) {
		if (fTableControl == null) {
			// Create the Table control, add two columns (name and description)
			// and set up the required listeners
			fTableControl = new Table(parent, SWT.BORDER | SWT.SINGLE | SWT.FULL_SELECTION
					| SWT.V_SCROLL | SWT.H_SCROLL);
			fTableControl.setFont(parent.getFont());
			fTableControl.setLinesVisible(true);
			fTableControl.setHeaderVisible(true);

			fTableControl.addSelectionListener(new SelectionAdapter() {
				/*
				 * (non-Javadoc)
				 * 
				 * @see org.eclipse.swt.events.SelectionAdapter#widgetSelected(org.eclipse.swt.events.SelectionEvent)
				 */
				@Override
				public void widgetSelected(SelectionEvent e) {
					Widget widget = e.widget;
					if (widget == fTableControl) {
						selectionChanged();
					}
				}
			});

			fTableControl.addDisposeListener(new DisposeListener() {
				/*
				 * (non-Javadoc)
				 * 
				 * @see org.eclipse.swt.events.DisposeListener#widgetDisposed(org.eclipse.swt.events.DisposeEvent)
				 */
				@Override
				public void widgetDisposed(DisposeEvent event) {
					fTableControl = null;
				}
			});

			TableColumn column = new TableColumn(fTableControl, SWT.NONE);
			column.setText("Configuration");
			column.setWidth(100);
			column = new TableColumn(fTableControl, SWT.NONE);
			column.setText("Description");
			column.setWidth(200);

		} else { // fTableControl != null
			checkParent(fTableControl, parent);
		}
		return fTableControl;
	}

	/**
	 * Returns this field editor's button box containing the Add, Remove and Edit buttons.
	 * 
	 * @param parent
	 *            the parent control
	 * @return the button box
	 */
	public Composite getButtonBoxComposite(Composite parent) {
		if (fButtonComposite == null) {
			fButtonComposite = new Composite(parent, SWT.NULL);
			GridLayout layout = new GridLayout(1, false);
			layout.marginWidth = 0;
			fButtonComposite.setLayout(layout);
			createButtons(fButtonComposite);
			fButtonComposite.addDisposeListener(new DisposeListener() {
				/*
				 * (non-Javadoc)
				 * 
				 * @see org.eclipse.swt.events.DisposeListener#widgetDisposed(org.eclipse.swt.events.DisposeEvent)
				 */
				@Override
				public void widgetDisposed(DisposeEvent event) {
					fAddButton = null;
					fRemoveButton = null;
					fEditButton = null;
				}
			});

		} else {
			checkParent(fButtonComposite, parent);
		}

		selectionChanged();
		return fButtonComposite;
	}

	/**
	 * Creates the Add, Remove and Edit buttons in the given button box.
	 * 
	 * @param box
	 *            the box for the buttons
	 */
	private void createButtons(Composite box) {
		fAddButton = createPushButton(box, "Add...");
		fEditButton = createPushButton(box, "Edit...");
		fRemoveButton = createPushButton(box, "Remove");
	}

	/**
	 * Helper method to create a push button.
	 * 
	 * @param parent
	 *            the parent control
	 * @param label
	 *            the button's label text
	 * @return Button
	 */
	private Button createPushButton(Composite parent, String label) {
		Button button = new Button(parent, SWT.PUSH);
		button.setText(label);
		button.setFont(parent.getFont());
		GridData data = new GridData(GridData.FILL_HORIZONTAL);
		int widthHint = convertHorizontalDLUsToPixels(button, IDialogConstants.BUTTON_WIDTH);
		data.widthHint = Math.max(widthHint, button.computeSize(SWT.DEFAULT, SWT.DEFAULT, true).x);
		button.setLayoutData(data);
		button.addSelectionListener(new SelectionAdapter() {
			/*
			 * (non-Javadoc)
			 * 
			 * @see org.eclipse.swt.events.SelectionAdapter#widgetSelected(org.eclipse.swt.events.SelectionEvent)
			 */
			@Override
			public void widgetSelected(SelectionEvent e) {
				Widget widget = e.widget;
				if (widget == fAddButton) {
					editButtonAction(true);
				} else if (widget == fRemoveButton) {
					removeButtonAction();
				} else if (widget == fEditButton) {
					editButtonAction(false);
				}
			}
		});
		return button;
	}

	/**
	 * Enable / Disable Buttons as required.
	 * <p>
	 * The Remove and Edit Buttons are only enabled, when an item is selected in the Table.
	 * </p>
	 * <p>
	 * Called after each change of the Table.
	 * </p>
	 */
	private void selectionChanged() {

		int index = fTableControl.getSelectionIndex();

		fRemoveButton.setEnabled(index >= 0);
		fEditButton.setEnabled(index >= 0);

		// TODO Is this necessary?
		fTableControl.redraw();
	}

	/**
	 * Remove the selected configuration.
	 * <p>
	 * The config is stored in the <code>fRemovedConfigs</code> list. All removed are only
	 * physically removed in the {@link #doStore()} method.
	 * </p>
	 * <p>
	 * Called when the remove button has been clicked.
	 * </p>
	 */
	private void removeButtonAction() {
		setPresentsDefaultValue(false);
		int index = fTableControl.getSelectionIndex();
		if (index >= 0) {
			TableItem ti = fTableControl.getItem(index);
			fRemovedConfigs.add((ProgrammerConfig) ti.getData());
			fTableControl.remove(index);
			selectionChanged();
		}
	}

	/**
	 * Adds a new configuration or edit the currently selected config.
	 * <p>
	 * Called when either the add or the edit button has been clicked.
	 * </p>
	 */
	private void editButtonAction(boolean createnew) {
		setPresentsDefaultValue(false);
		ProgrammerConfig config = null;
		TableItem ti = null;

		// Create a list of all currently available configurations
		// This is used by the editor to avoid name clashes
		// (a configuration name needs to be unique)
		Set<String> allconfigs = new HashSet<String>();
		TableItem[] allitems = fTableControl.getItems();
		for (TableItem item : allitems) {
			allconfigs.add(item.getText(0));
		}

		if (createnew) { // new config
			// Create a new configuration with a default name
			// (with a trailing running number if required),
			String basename = "New Configuration";
			String defaultname = basename;
			int i = 1;
			while (allconfigs.contains(defaultname)) {
				defaultname = basename + " (" + i++ + ")";
			}
			config = fCfgManager.createNewConfig();
			config.setName(defaultname);
		} else { // edit existing config
			// Get the ProgrammerConfig from the selected TableItem
			ti = fTableControl.getItem(fTableControl.getSelectionIndex());
			config = (ProgrammerConfig) ti.getData();
		}

		// Open the Config Editor.
		// If the OK Button was selected, the modified Config is fetched from
		// the Dialog and the relevant TableItem is updated.
		AVRDudeConfigEditor dialog = new AVRDudeConfigEditor(fTableControl.getShell(), config,
				allconfigs);
		if (dialog.open() == Window.OK) {
			// OK Button selected:
			ProgrammerConfig newconfig = dialog.getResult();

			if (createnew) {
				ti = new TableItem(fTableControl, SWT.NONE);
			}

			// Change the TableItem
			if (ti != null) {
				ti.setText(new String[] { newconfig.getName(), newconfig.getDescription() });
				ti.setData(newconfig);
			}
			selectionChanged();
		}
	}

}
