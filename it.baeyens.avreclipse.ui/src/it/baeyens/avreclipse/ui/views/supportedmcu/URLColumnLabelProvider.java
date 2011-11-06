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
 * $Id: URLColumnLabelProvider.java 851 2010-08-07 19:37:00Z innot $
 *     
 *******************************************************************************/
package it.baeyens.avreclipse.ui.views.supportedmcu;

import it.baeyens.arduino.globals.ArduinoConst;
import it.baeyens.avreclipse.AVRPlugin;
import it.baeyens.avreclipse.core.IMCUProvider;
import it.baeyens.avreclipse.core.toolinfo.Datasheets;
import it.baeyens.avreclipse.core.toolinfo.MCUNames;
import it.baeyens.avreclipse.core.util.AVRMCUidConverter;
import it.baeyens.avreclipse.util.URLDownloadException;
import it.baeyens.avreclipse.util.URLDownloadManager;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.TableEditor;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.events.IHyperlinkListener;
import org.eclipse.ui.forms.widgets.Hyperlink;
import org.eclipse.ui.ide.IDE;


/**
 * This is an extended ColumnLabelProvider that handles URL hyperlinks.
 * <p>
 * This Class needs two {@link IMCUProvider}s, one for the Label text and one for the URL. As
 * implemented in the View these are {@link MCUNames} and {@link Datasheets} respectively.
 * </p>
 * <p>
 * As TableViewers do not support custom controls or actually anything clickable, this class is
 * implemented by adding TableEditors on top of the TableItems in this Column. The
 * {@link #updateColumn(TableViewer, TableViewerColumn)} method needs to be called to set up the
 * TableEditors. This method may only be called after the table has been filled with values (after
 * the TableViewer.setInput(model)) method has been called.
 * </p>
 * <p>
 * The TableEditors are not used as Editors, but contain an Hyperlink control each, which can be
 * clicked to download and open the URL from the given linkprovider.
 * </p>
 * 
 * @author Thomas Holland
 * @since 2.2
 */
public class URLColumnLabelProvider extends ColumnLabelProvider implements
		ISelectionChangedListener {

	/** The MCUProvider that provides the text to be shown in the cell */
	private final IMCUProvider					fNameProvider;

	/** The IMCUProvider that provides the url to be opene */
	private final IMCUProvider					fLinkProvider;

	private TableViewer							fTableViewer;

	/** The last TableEditor selected. Required to de-select */
	private TableEditor							fLastEditor;

	/**
	 * List of all TableEditors of this column. Required to update the TableEditors manually on a
	 * {@link SelectionChangedEvent}
	 */
	private final Map<TableItem, TableEditor>	fTableEditors			= new HashMap<TableItem, TableEditor>();

	/** The text color for links not yet downloaded. Value: SWT.COLOR_DARK_BLUE */
	private static Color						LINK_COLOR				= PlatformUI
																				.getWorkbench()
																				.getDisplay()
																				.getSystemColor(
																						SWT.COLOR_DARK_BLUE);

	/** The text color for links already in the cache. Value: SWT.COLOR_MAGETA */
	private static Color						LINK_IN_CACHE_COLOR		= PlatformUI
																				.getWorkbench()
																				.getDisplay()
																				.getSystemColor(
																						SWT.COLOR_MAGENTA);

	/** The text color for malformed links. Value: SWT.COLOR_RED */
	private static Color						LINK_MALFORMED_COLOR	= PlatformUI
																				.getWorkbench()
																				.getDisplay()
																				.getSystemColor(
																						SWT.COLOR_RED);

	/**
	 * @param nameprovider
	 *            The <code>IMCUProvider</code> that returns a User readable name for a given MCU
	 *            id
	 * @param linkProvider
	 *            The <code>IMCUProvider</code> that returns the URL (as <code>String</code>)
	 *            for the datasheet for the given MCU id
	 */
	public URLColumnLabelProvider(IMCUProvider nameprovider, IMCUProvider linkProvider) {
		fNameProvider = nameprovider;
		fLinkProvider = linkProvider;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.viewers.ColumnLabelProvider#getText(java.lang.Object)
	 */
	@Override
	public String getText(Object element) {

		// returns the name of the given MCU id
		String mcuid = (String) element;
		String info = fNameProvider.getMCUInfo(mcuid);

		// If MCUNames is used as a provider, info will never be null. But this
		// might change...
		return info != null ? info : "n/a";
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.viewers.BaseLabelProvider#dispose()
	 */
	@Override
	public void dispose() {
		// Not sure if this is really necessary, as this ColumnLabelProvider
		// will only be disposed when the whole View (incl. the TableViewer) is
		// closed.
		if (fTableViewer != null)
			fTableViewer.removeSelectionChangedListener(this);
	}

	/**
	 * Set up this column for URL table cells.
	 * <p>
	 * This needs to be called <strong>after</strong> the table has been filled with rows. It will
	 * add Hyperlink Widgets on top of all cells in the column, that actually contain URLs. This is
	 * done via TableEditors for those cells.
	 * </p>
	 * <p>
	 * This also adds itself as <code>SelectionChangeListener</code> and as
	 * <code>FocusListener</code> to the given TableViewer.
	 * </p>
	 * <p>
	 * Both parameters need to refer to the same column as this ColumnLabelProvider. Passing other
	 * TableViewers or TableViewerColumns will result in undefined results.
	 * </p>
	 * 
	 * @param tableviewer
	 *            The TableViewer which contains this Column.
	 * @param viewercolumn
	 *            The TableViewerColumn for this ColumnLabelProvider
	 */
	public void updateColumn(TableViewer tableviewer, TableViewerColumn viewercolumn) {

		// get the table from the Column and find the index of the given column
		// this is needed later on for the TableEditor
		fTableViewer = tableviewer;
		TableColumn column = viewercolumn.getColumn();
		Table table = column.getParent();

		int index = getColumnIndex(column);

		// Now go through all TableItems (=Rows) of the Table.
		// For each TableItem a new TableEditor with a Hyperlink Control is
		// generated (if the MCU id of the row has a Datasheet associated with
		// it).
		TableItem[] allitems = table.getItems();
		for (TableItem item : allitems) {
			// get the mcuid for this row
			String mcuname = item.getText();
			String mcuid = AVRMCUidConverter.name2id(mcuname);

			// Test if there is a datasheet available. If yes, add a TableEditor
			// with a Hyperlink in it. If no, do nothing (will show the text
			// from #getText())
			if (fLinkProvider.hasMCU(mcuid)) {
				final URL url;

				final Hyperlink link = new Hyperlink(table, SWT.NONE);
				link.setText(mcuname);

				try {
					// Create an URL object for the Datasheet URL and set the
					// Hyperlink Control to look like a Browser link.
					url = new URL(fLinkProvider.getMCUInfo(mcuid));
					link.setUnderlined(true);
					link.setHref(url);
					link.setToolTipText(url.toExternalForm());
					// Simlate standard Browser behaviour
					if (URLDownloadManager.inCache(url)) {
						link.setData(LINK_IN_CACHE_COLOR);
					} else {
						link.setData(LINK_COLOR);
					}
				} catch (MalformedURLException e1) {
					// unlikely, as this should be covered by the Datasheet
					// Preferences. Nevertheless I leave this here, if a user
					// tries to mess with the datasheet property files.
					link.setUnderlined(false);
					link.setData(LINK_MALFORMED_COLOR);
					link.setToolTipText("Malformed Datasheet URL: "
							+ fLinkProvider.getMCUInfo(mcuid));
				}

				// The HyperlinkListener is taking care of opening the URL when
				// clicked.
				link.addHyperlinkListener(new IHyperlinkListener() {
					/*
					 * (non-Javadoc)
					 * 
					 * @see org.eclipse.ui.forms.events.IHyperlinkListener#linkActivated(org.eclipse.ui.forms.events.HyperlinkEvent)
					 */
					@Override
					public void linkActivated(HyperlinkEvent event) {
						URL url = (URL) event.getHref();
						if (url != null) {
							// Start the (downloading and) opening of the
							// references URL
							openURL(url);
						}
					}

					@Override
					public void linkEntered(HyperlinkEvent event) {
						// Do nothing
					}

					@Override
					public void linkExited(HyperlinkEvent event) {
						// Do nothing
					}
				});

				// finally create a TableEditor for our Hyperlink and keep a
				// reference to it, so manual layout() method calls can be made
				// when the Table selection has changed.
				TableEditor editor = new TableEditor(table);
				editor.grabHorizontal = true;
				editor.setEditor(link, item, index);
				fTableEditors.put(item, editor);

				// finally set the colors (not selected, not focused)
				setEditorColors(editor, false, false);
			}

		}
		// Sometimes the TableEditors are a bit off when opening the viewer.
		// Re-layout the TableEditors in the background
		Display display = table.getDisplay();
		display.asyncExec(updateEditors);

		// Add this to the table PostSelectionChangeListener
		fTableViewer.addSelectionChangedListener(this);

		// Now add a FocusListener to set the colors of the selected TableEditor
		// whenever the focus changes for the Table
		fTableViewer.getTable().addFocusListener(new FocusListener() {

			/*
			 * (non-Javadoc)
			 * 
			 * @see org.eclipse.swt.events.FocusListener#focusGained(org.eclipse.swt.events.FocusEvent)
			 */
			@Override
			public void focusGained(FocusEvent e) {
				Table table = (Table) e.getSource();
				int index = table.getSelectionIndex();
				if (index != -1) {
					// some item is selected. Get it, find the associated
					// TableEditor (if any), and change the colors of the
					// associated Hyperlink.
					TableItem selected = table.getItem(index);
					TableEditor editor = fTableEditors.get(selected);
					if (editor != null) {
						setEditorColors(editor, true, true);
					}
				}
			}

			/*
			 * (non-Javadoc)
			 * 
			 * @see org.eclipse.swt.events.FocusListener#focusLost(org.eclipse.swt.events.FocusEvent)
			 */
			@Override
			public void focusLost(FocusEvent e) {
				Table table = (Table) e.getSource();
				int index = table.getSelectionIndex();
				if (index != -1) {
					// some item is selected. Get it, find the associated
					// TableEditor (if any), and change the colors of the
					// associated Hyperlink.
					TableItem selected = table.getItem(index);
					TableEditor editor = fTableEditors.get(selected);
					if (editor != null) {
						setEditorColors(editor, true, false);
					}
				}
			}
		});

	}

	/**
	 * Sets the colors of a Hyperlink control (via the associated TableEditor).
	 * <p>
	 * A (Windows) SWT Table Cell can have three color states. These three states are covered in
	 * this method:
	 * </p>
	 * <ul>
	 * <li>
	 * <p>
	 * Item selected: <code>true</code>, Table has Focus: <code>true</code><br>
	 * Background: <code>SWT.COLOR_LIST_SELECTION</code><br>
	 * Foreground: <code>SWT.COLOR_LIST_SELECTION_TEXT</code>
	 * </p>
	 * </li>
	 * <li>
	 * <p>
	 * Item selected: <code>true</code>, Table has Focus: <code>false</code><br>
	 * Background: <code>SWT.COLOR_WIDGET_BACKGROUND</code><br>
	 * Foreground: Link color provided by the Hyperlink Control
	 * </p>
	 * </li>
	 * <li>
	 * <p>
	 * Item selected: <code>false</code>, Table has Focus: not required<br>
	 * Background: <code>SWT.COLOR_LIST_BACKGROUND</code><br>
	 * Foreground: Link color provided by the Hyperlink Control
	 * </p>
	 * </li>
	 * </ul>
	 * 
	 * @param editor
	 *            TableEdior to set the colors for.
	 * @param isselected
	 *            <code>true</code> if the editor is in a currently selected table row.
	 * @param hasfocus
	 *            <code>true</code> if the table has the focus. Not required if isselected is
	 *            <code>false</code>
	 */
	private void setEditorColors(TableEditor editor, boolean isselected, boolean hasfocus) {
		final Color background, foreground;
		final Control link = editor.getEditor();
		final Display display = link.getDisplay();
		if (isselected) {
			if (hasfocus) {
				background = display.getSystemColor(SWT.COLOR_LIST_SELECTION);
				foreground = display.getSystemColor(SWT.COLOR_LIST_SELECTION_TEXT);
			} else {
				// no focus
				background = display.getSystemColor(SWT.COLOR_WIDGET_BACKGROUND);
				foreground = (Color) link.getData();
			}
		} else {
			// not selected
			background = display.getSystemColor(SWT.COLOR_LIST_BACKGROUND);
			foreground = (Color) link.getData();
		}
		link.setBackground(background);
		link.setForeground(foreground);
	}

	/**
	 * Gets the index of the given TableColumn in the table
	 * 
	 * @param column
	 * @return int with Table column index
	 */
	private static int getColumnIndex(TableColumn column) {
		Table table = column.getParent();
		TableColumn[] allcolumns = table.getColumns();
		int index = -1;
		for (int i = 0; i < allcolumns.length; i++) {
			if (allcolumns[i] == column) {
				index = i;
				break;
			}
		}
		return index;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.viewers.ISelectionChangedListener#selectionChanged(org.eclipse.jface.viewers.SelectionChangedEvent)
	 */
	@Override
	public void selectionChanged(SelectionChangedEvent event) {
		// Stupid - at least on Windows an external Selection change via the
		// TableViewer.setSelection() method does not cause a Selection event in
		// the underlying Table, so we have to with the SelectionChangeEvent of
		// the TableViewer.

		// When the selection has changed, first a previously selected
		// TableEditor (which still has its "selected" colors) needs to be
		// changed to the unselected colors.
		//
		// Then all TableEditors of this column need to recalculate their
		// layout,
		// because the (programmatic) selection change might change the visible
		// part of the Table, which the TableEditors won't notice (again stupid)

		TableViewer source = (TableViewer) event.getSource();
		Table table = source.getTable();
		TableItem item = table.getItem(table.getSelectionIndex());

		// Restore the previously selected link
		if (fLastEditor != null) {
			setEditorColors(fLastEditor, false, false);
		}

		TableEditor editor = fTableEditors.get(item);
		if (editor != null) {
			setEditorColors(editor, true, item.getParent().isFocusControl());
			fLastEditor = editor;
		}

		// Update all Editors. This is called twice, because at - least on
		// windows - clicking on a partially visible row will cause the table to
		// scroll *after* the selection has been made (which -again- the
		// TableEditors will not be aware of, as this partial scroll is without
		// Scroll Events.
		// The 0,5 sec value is just a guess. It works on my PentiumM 1,6 GHz
		// Laptop for a redraw after a partial scroll without much lag.
		// The TableEditor uses 1,5 sec in a similar situation (for a resize),
		// but that caused the update to lag far behind the partial scroll
		// (making the TableEditor hang behind the other Columns.
		// However, if this value is to short, it will cause the TableEditor to
		// be off by one row.
		Display display = item.getDisplay();
		display.syncExec(updateEditors);
		display.timerExec(500, updateEditors);
	}

	/**
	 * A small Runnable that will call {@link TableEditor#layout()} on all TableEditors of the
	 * column
	 */
	private final Runnable	updateEditors	= new Runnable() {
		@Override
												public void run() {
													Collection<TableEditor> alleditors = fTableEditors
															.values();
													for (TableEditor e : alleditors) {
														e.layout();
													}
												}
											};

	/**
	 * Load and Display the given URL.
	 * <p>
	 * The File from the URL is first downloaded via the {@link URLDownloadManager} and then opened
	 * using the default Editor registered for this filetype.
	 * </p>
	 * <p>
	 * The download and the opening of the file is done in a Job, so this method returns immediatly.
	 * </p>
	 * <p>
	 * If a download of the same URL is still in progress, this method does nothing to avoid
	 * multiple parallel downloads of the same file by nervous users. </p
	 * 
	 * @param urlstring
	 *            A String with an URL.
	 */
	private void openURL(final URL url) {
		final Display display = PlatformUI.getWorkbench().getDisplay();
		// Test if a download of this file is already in progress.
		// If yes: do nothing and return, assuming that the user has clicked
		// on the url twice accidentally
		if (URLDownloadManager.isDownloading(url)) {
			return;
		}

		// The actual download is done in this Job.
		// For any Exception during the download an ErrorDialog is displayed
		// with the cause(s)
		// The Job also returns an IStatus result, but by the time this is
		// returned, the openURL() method has long finished and there is
		// no one there to actually read this message :-)
		Job loadandopenJob = new Job("Download and Open") {
			@Override
			protected IStatus run(final IProgressMonitor monitor) {
				try {
					monitor.beginTask("Download " + url.toExternalForm(), 100);

					// Download the file and...
					final File file = URLDownloadManager.download(url, new SubProgressMonitor(
							monitor, 95));

					// ...open the file in an editor.
					monitor.subTask("Opening Editor for " + file.getName());
					if (display == null || display.isDisposed()) {
						return new Status(Status.ERROR, ArduinoConst.CORE_PLUGIN_ID,
								"Cannot open Editor: no Display found", null);
					}
					openFileInEditor(file);

					monitor.worked(5);
				} catch (URLDownloadException ude) {
					final URLDownloadException exc = ude;
					// ErrorDialog for all Exceptions, in an
					// Display.syncExec() to run in the UI Thread.
					display.syncExec(new Runnable() {
						@Override
						public void run() {
							Shell shell = display.getActiveShell();
							String title = "Download Failed";
							String message = "The requested file could not be downloaded\nFile:  "
									+ url.getPath() + "\nHost:  " + url.getHost();
							String reason = exc.getMessage();
							MultiStatus status = new MultiStatus(ArduinoConst.CORE_PLUGIN_ID, 0, reason,
									null);
							Throwable cause = exc.getCause();
							// in case there are multiple root causes
							// (unlikely, but who knows?)
							while (cause != null) {
								status.add(new Status(Status.ERROR, ArduinoConst.CORE_PLUGIN_ID, cause
										.getClass().getSimpleName(), cause));
								cause = cause.getCause();
							}

							ErrorDialog.openError(shell, title, message, status, Status.ERROR);
							AVRPlugin.getDefault().log(status);
						}
					}); // fDisplay.asyncExec
				} finally {
					monitor.done();
				}
				return Status.OK_STATUS;
			} // run
		}; // new Job()

		// set some options and start the Job.
		loadandopenJob.setUser(true);
		loadandopenJob.setPriority(Job.LONG);
		loadandopenJob.schedule();

		return;
	}

	/**
	 * Opens the given file with the standard editor.
	 * <p>
	 * An ErrorDialog is shown when the opening of the file fails.
	 * </p>
	 * 
	 * @param file
	 *            <code>java.io.File</code> with the file to open
	 * @return
	 */
	private IStatus openFileInEditor(final File file) {

		final Display display = PlatformUI.getWorkbench().getDisplay();

		// Because this is called from a Job (which is not running in the UI
		// Thread, the opening is delegated to a Display.syncExec()
		display.syncExec(new Runnable() {
			@Override
			public void run() {
				IFileStore fileStore = EFS.getLocalFileSystem().getStore(new Path(file.toString()));
				if (!fileStore.fetchInfo().isDirectory() && fileStore.fetchInfo().exists()) {
					IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow()
							.getActivePage();
					try {
						IDE.openEditorOnFileStore(page, fileStore);
					} catch (PartInitException e) {
						IStatus status = new Status(Status.ERROR, ArduinoConst.CORE_PLUGIN_ID,
								"Could not open " + file.toString(), e);
						Shell shell = display.getActiveShell();
						String title = "Can't open File";
						String message = "The File " + file.toString() + " could not be opened";
						ErrorDialog.openError(shell, title, message, status);
						AVRPlugin.getDefault().log(status);
					}
				}
			}
		});

		return Status.OK_STATUS;
	}
}
