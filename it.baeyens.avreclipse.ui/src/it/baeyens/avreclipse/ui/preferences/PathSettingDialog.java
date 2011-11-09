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
 * $Id: PathSettingDialog.java 851 2010-08-07 19:37:00Z innot $
 *     
 *******************************************************************************/

package it.baeyens.avreclipse.ui.preferences;

import it.baeyens.arduino.ArduinoConst;
import it.baeyens.avreclipse.core.paths.AVRPathManager;
import it.baeyens.avreclipse.core.paths.AVRPathManager.SourceType;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.StatusDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.part.PageBook;


/**
 * Custom dialog to modify a plugin path.
 * 
 * @author Thomas Holland
 * 
 */
public class PathSettingDialog extends StatusDialog {

	// The GUI Widgets
	private Combo			fTypeCombo		= null;
	private Combo			fBundleCombo	= null;
	private Text			fPathText		= null;
	private Button			fFolderButton	= null;

	private PageBook		fPageBook		= null;
	private Composite		fSystemPage		= null;
	private Composite		fBundlePage		= null;
	private Composite		fCustomPage		= null;

	// Internal path storage
	private AVRPathManager	fPathManager	= null;

	/**
	 * Constructor for a new PathSettingDialog.
	 * 
	 * The passed IPathManager is copied and any changes to the path are only written back to it
	 * when the {@link #getResult()} method is called.
	 * 
	 * @param parent
	 *            Parent <code>Shell</code>
	 * @param pathmanager
	 *            IPathManager with the path to edit
	 */
	public PathSettingDialog(Shell parent, AVRPathManager pathmanager) {
		super(parent);

		// make a copy of the given IPathManager
		fPathManager = new AVRPathManager(pathmanager);

		setTitle("Change Path for " + pathmanager.getName());

		// Allow this dialog to be resizeable
		setShellStyle(getShellStyle() | SWT.RESIZE);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.dialogs.Dialog#createDialogArea(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	protected Control createDialogArea(Composite parent) {
		Composite composite = (Composite) super.createDialogArea(parent);

		// The list of supported Source types
		String[] types = { AVRPathManager.SourceType.System.toString(),
				AVRPathManager.SourceType.Bundled.toString(),
				AVRPathManager.SourceType.Custom.toString() };

		// TODO: try to determine the size dynamically
		getShell().setMinimumSize(400, 220);

		// The description Label
		Label description = new Label(composite, SWT.NONE);
		description.setText(fPathManager.getDescription());

		// The Top part contains the Source Type Combo
		Composite top = new Composite(composite, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		layout.numColumns = 2;
		top.setLayout(layout);

		// The Source Type combo
		Label typelabel = new Label(top, SWT.NONE);
		typelabel.setText("Path source:");
		fTypeCombo = new Combo(top, SWT.READ_ONLY | SWT.DROP_DOWN);
		fTypeCombo.setItems(types);
		fTypeCombo.addListener(SWT.Selection, new Listener() {
			@Override
			public void handleEvent(Event e) {
				String value = fTypeCombo.getText();
				changeSourceType(value);
			}
		});

		// Separator Label as eye candy
		Label separator = new Label(composite, SWT.SEPARATOR | SWT.HORIZONTAL);
		separator.setLayoutData(new GridData(SWT.FILL, SWT.NONE, true, false));

		// The path editors for the different source types are organized as
		// pages of a PageBook, so only one is shown at a time
		fPageBook = new PageBook(composite, SWT.NONE);
		fPageBook.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		fSystemPage = addSystemPage(fPageBook);
		fBundlePage = addBundlePage(fPageBook);
		fCustomPage = addCustomPage(fPageBook);

		// show the current path / source values
		String currenttype = fPathManager.getSourceType().toString();
		fTypeCombo.select(fTypeCombo.indexOf(currenttype));
		changeSourceType(currenttype);

		return composite;

	}

	/**
	 * Add an editor page for System source type.
	 * 
	 * This only shows the system value read-only
	 * 
	 * @param parent
	 *            PageBook
	 * @return Composite containing the System source editor
	 */
	private Composite addSystemPage(Composite parent) {
		Composite page = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		layout.numColumns = 3;
		page.setLayout(layout);

		Label label = new Label(page, SWT.NONE);
		label.setText("System value:");

		final Text text = new Text(page, SWT.BORDER | SWT.READ_ONLY);
		text.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		// text.setBackground(page.getDisplay().getSystemColor(SWT.COLOR_INFO_BACKGROUND));
		text.setText(fPathManager.getSystemPath(false).toOSString());

		Button rescanbutton = new Button(page, SWT.NONE);
		rescanbutton.setText("Rescan");
		rescanbutton.addSelectionListener(new SelectionAdapter() {

			/*
			 * (non-Javadoc)
			 * 
			 * @see org.eclipse.swt.events.SelectionAdapter#widgetSelected(org.eclipse.swt.events.SelectionEvent)
			 */
			@Override
			public void widgetSelected(SelectionEvent e) {
				BusyIndicator.showWhile(fPageBook.getDisplay(), new Runnable() {
					@Override
					public void run() {
						fPathManager.getSystemPath(true);
					}
				});
				// Get the updated system path from the cache
				text.setText(fPathManager.getSystemPath(false).toOSString());
			}
		});

		return page;
	}

	/**
	 * Add an editor page for Bundle source type.
	 * 
	 * Bundle sources are currently not implemented.
	 * 
	 * @param parent
	 *            PageBook
	 * @return Composite containing the Bundle source editor
	 */
	private Composite addBundlePage(Composite parent) {
		Composite page = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		layout.numColumns = 2;
		page.setLayout(layout);

		Label label = new Label(page, SWT.NONE);
		label.setText("Select AVR-GCC Bundle");

		// TODO: Bundle: Add choices here
		fBundleCombo = new Combo(page, SWT.READ_ONLY | SWT.DROP_DOWN);
		fBundleCombo.addListener(SWT.Selection, new Listener() {
			@Override
			public void handleEvent(Event e) {
				String value = fTypeCombo.getText();
				System.out.println(value);
				// TODO: Bundle: do something
			}
		});

		return page;
	}

	/**
	 * Add an editor page for Custom source type.
	 * 
	 * @param parent
	 *            PageBook
	 * @return Composite containing the Custom source editor
	 */
	private Composite addCustomPage(Composite parent) {
		Composite page = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		layout.numColumns = 3;
		page.setLayout(layout);

		Label label = new Label(page, SWT.NONE);
		label.setText("Custom value:");
		// label.setLayoutData(new GridData(SWT.NONE, SWT.NONE, false, false));

		fPathText = new Text(page, SWT.SINGLE | SWT.BORDER);
		fPathText.setText(fPathManager.getPath().toOSString());
		fPathText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		fPathText.addListener(SWT.Modify, new Listener() {
			@Override
			public void handleEvent(Event event) {
				// test the path on all modifications
				String newpath = fPathText.getText();
				fPathManager.setPath(newpath, SourceType.Custom);
				testStatus();
			}
		});

		fFolderButton = new Button(page, SWT.NONE);
		fFolderButton.setText("Browse...");
		// fFolderButton.setLayoutData(new GridData(SWT.NONE, SWT.NONE, false,
		// false));
		fFolderButton.addListener(SWT.Selection, new Listener() {
			@Override
			public void handleEvent(Event event) {
				// open directory dialog
				String newpath = getDirectory(fPathText.getText());
				if (newpath != null) {
					fPathText.setText(newpath);
					fPathManager.setPath(newpath, SourceType.Custom);
				}
				testStatus();
			}
		});

		return page;

	}

	/**
	 * Get the results from this dialog.
	 * <p>
	 * It will return a IPathManager with the selected path.
	 * </p>
	 * <p>
	 * This should only be called when <code>open()</code> returned <code>OK</code> (OK Button
	 * clicked). Otherwise canceled changes will be returned.
	 * </p>
	 * 
	 * @return The IPathManager with the modified path.
	 */
	public AVRPathManager getResult() {
		return fPathManager;
	}

	/**
	 * Change the source type and show the associated editor page.
	 * 
	 * This method will also change the internally stored path to the selected source type.
	 * <p>
	 * The sourcetype is passed as a <code>String</code> (iso <code>SourceType</code>) as it
	 * comes directly from the Source Type combo widget.
	 * </p>
	 * 
	 * @param type
	 */
	private void changeSourceType(String type) {

		if (type.equals(SourceType.System.toString())) {
			// System source
			fPageBook.showPage(fSystemPage);
			fPathManager.setPath(fPathManager.getSystemPath(false).toOSString(), SourceType.System);
		}
		if (type.equals(SourceType.Bundled.toString())) {
			// Bundle source
			fPageBook.showPage(fBundlePage);
			// TODO: Bundle: load bundle id
			updateStatus(new Status(IStatus.ERROR, ArduinoConst.CORE_PLUGIN_ID,
					"Bundled toolchains not yet supported"));
			return;

		}
		if (type.equals(SourceType.Custom.toString())) {
			// Custom source
			fPageBook.showPage(fCustomPage);
			fPathManager.setPath(fPathText.getText(), SourceType.Custom);
		}

		// Update the status line for the new values
		testStatus();
	}

	/**
	 * Update the status line for the current path / source.
	 * 
	 * It shows
	 * <ul>
	 * <li>nothing if the path is valid and not empty </li>
	 * <li>a Warning if the path is empty, but still valid (optional paths)</li>
	 * <li>an Error if the path is invalid</li>
	 * </ul>
	 */
	private void testStatus() {
		IStatus status = Status.OK_STATUS;

		boolean empty = "".equals(fPathManager.getPath().toString());
		boolean valid = fPathManager.isValid();

		if (empty && valid) {
			// warning only
			status = new Status(IStatus.WARNING, ArduinoConst.CORE_PLUGIN_ID, "Optional path is empty");
		}
		if (!valid) {
			// error
			status = new Status(IStatus.ERROR,ArduinoConst.CORE_PLUGIN_ID, "Path is invalid");
		}

		super.updateStatus(status);
	}

	/**
	 * Helper that opens the directory chooser dialog.
	 * 
	 * @param startingDirectory
	 *            The directory the dialog will open in.
	 * @return File File or <code>null</code>.
	 * 
	 */
	private String getDirectory(String startingDirectory) {

		DirectoryDialog fileDialog = new DirectoryDialog(getShell(), SWT.OPEN);
		if (startingDirectory != null) {
			fileDialog.setFilterPath(startingDirectory);
		}
		String dir = fileDialog.open();
		if (dir != null) {
			dir = dir.trim();
			if (dir.length() > 0) {
				return dir;
			}
		}

		return null;
	}

}
