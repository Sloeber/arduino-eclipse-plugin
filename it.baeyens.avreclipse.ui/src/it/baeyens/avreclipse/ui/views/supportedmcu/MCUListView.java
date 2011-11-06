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
 * $Id: MCUListView.java 851 2010-08-07 19:37:00Z innot $
 *     
 *******************************************************************************/
package it.baeyens.avreclipse.ui.views.supportedmcu;

import it.baeyens.avreclipse.PluginIDs;
import it.baeyens.avreclipse.core.properties.AVRProjectProperties;
import it.baeyens.avreclipse.core.properties.ProjectPropertyManager;
import it.baeyens.avreclipse.core.util.AVRMCUidConverter;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectNature;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.ColumnViewerToolTipSupport;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.OwnerDrawLabelProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.window.ToolTip;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.ViewPart;


/**
 * This is the main part of the Supported MCUs View.
 * 
 * @author Thomas Holland
 * @since 2.2
 */

public class MCUListView extends ViewPart {

	// The parent Composite of this Viewer
	private Composite					fViewParent;

	private TableViewer					fTable;

	private final List<MCUListColumn>	fColumns	= new ArrayList<MCUListColumn>();

	// private IMemento fMemento;

	private ISelectionListener			fWorkbenchSelectionListener;

	private SupportedContentProvider	fContentProvider;

	public enum LabelStyle {
		SHOW_STRING, SHOW_YESNO, SHOW_URL;
	}

	/**
	 * The constructor.
	 * 
	 * Nothing done here.
	 */
	public MCUListView() {
	}

	/*
	 * (non-Javadoc) Method declared on IViewPart.
	 */
	@Override
	public void init(IViewSite site, IMemento memento) throws PartInitException {
		// Initialize the SuperClass and store the passed memento for use by
		// the individual methods.
		super.init(site, memento);
		// fMemento = memento;
	}

	@Override
	public void saveState(IMemento memento) {
		// Save the current state of the viewer
		super.saveState(memento);

		// TODO: Save the Column Layout for each category

	}

	/**
	 * Create, layout and initialize the controls of this viewer
	 */
	@SuppressWarnings("deprecation")
	@Override
	public void createPartControl(Composite parent) {

		fViewParent = parent;

		// All listeners that are need to unregistered on dispose()
		fWorkbenchSelectionListener = new WorkbenchSelectionListener();

		TableColumnLayout tcl = new TableColumnLayout();
		fViewParent.setLayout(tcl);

		fContentProvider = new SupportedContentProvider();

		fTable = new TableViewer(parent, SWT.BORDER | SWT.FULL_SELECTION);
		fTable.setContentProvider(fContentProvider);
		fTable.setUseHashlookup(true);
		fTable.getTable().setHeaderVisible(true);
		fTable.getTable().setLinesVisible(true);

		MCUListColumn[] providerlist = MCUListColumn.values();
		for (MCUListColumn provider : providerlist) {
			provider.addColumn(fTable, tcl);
			fColumns.add(provider);
		}

		// setUpOwnerDraw has been deprecated in Eclipse 3.4, but we still support 3.3
		OwnerDrawLabelProvider.setUpOwnerDraw(fTable);
		ColumnViewerToolTipSupport.enableFor(fTable, ToolTip.NO_RECREATE);

		fTable.setInput(fContentProvider);

		// Add the Table as a Workbench Selection provider
		getSite().setSelectionProvider(fTable);

		// Activate the Workbench selection listener
		getSite().getWorkbenchWindow().getSelectionService().addPostSelectionListener(
				fWorkbenchSelectionListener);

		for (MCUListColumn mlc : fColumns) {
			mlc.updateColumn();
		}

	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.part.WorkbenchPart#setFocus()
	 */
	@Override
	public void setFocus() {
		// This is a pure viewer, so nothing to set the focus to
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.part.WorkbenchPart#dispose()
	 */
	@Override
	public void dispose() {
		// remove the listeners from their objects
		getSite().getWorkbenchWindow().getSelectionService().removePostSelectionListener(
				fWorkbenchSelectionListener);
		super.dispose();
	}

	/**
	 * Handle Selection Change Events.
	 * <p>
	 * This is called by the workbench selection services to inform this viewer, that something has
	 * been selected on the workbench. If something with an AVR MCU type has been selected
	 * (Project), then the viewer will show the description of the associated mcu.
	 * </p>
	 */
	private class WorkbenchSelectionListener implements ISelectionListener {

		/*
		 * (non-Javadoc)
		 * @see org.eclipse.ui.ISelectionListener#selectionChanged(org.eclipse.ui.IWorkbenchPart,
		 * org.eclipse.jface.viewers.ISelection)
		 */
		@Override
		public void selectionChanged(IWorkbenchPart part, final ISelection selection) {
			// we ignore our own selections
			if (part == MCUListView.this) {
				return;
			}

			// To minimize the GUI impact run the rest of this method in a Job
			Job selectionjob = new Job("AVR MCU List") {

				@Override
				protected IStatus run(IProgressMonitor monitor) {
					String newid = null;
					try {

						monitor.beginTask("Selection Change", 3);
						Set<String> mculist = fContentProvider.getMasterMCUList();
						monitor.worked(1);

						// see if the selection is something that has an avr mcu
						// id
						if (selection instanceof IStructuredSelection) {
							// First: Projects
							IStructuredSelection ss = (IStructuredSelection) selection;
							newid = getMCUId(ss);
						} else if (selection instanceof ITextSelection) {
							// Second: Selected Text
							String text = ((ITextSelection) selection).getText();
							// Test if the text is an MCU name/id
							if (mculist.contains(AVRMCUidConverter.name2id(text))) {
								// yes: use it
								newid = AVRMCUidConverter.name2id(text);
							}
						}
						monitor.worked(1);

						if (newid != null) {
							// Test if it is a valid mcu id. Only set valid mcu
							// id values as otherwise an error message would be
							// displayed
							if (!mculist.contains(AVRMCUidConverter.name2id(newid))) {
								return Status.OK_STATUS;
							}

							// Next cause a SelectionChange Event in the UI
							// Thread, which handles the rest of the change.
							// (Only if the control still exists)
							final IStructuredSelection newselection = new StructuredSelection(newid);
							if ((fViewParent != null) && (!fViewParent.isDisposed())) {
								fViewParent.getDisplay().asyncExec(new Runnable() {
									@Override
									public void run() {
										if ((fTable != null) && (!fTable.getControl().isDisposed())) {
											fTable.setSelection(newselection, true);
										}
									}
								}); // Runnable
							}
						}
						monitor.worked(1);
					} finally {
						monitor.done();
					}
					return Status.OK_STATUS;
				}
			};
			selectionjob.setSystem(true);
			selectionjob.setPriority(Job.SHORT);
			selectionjob.schedule();
		}

		/**
		 * Get the mcu id from the given structured selection.
		 * <p>
		 * If the first element of the selection is an AVR project, the mcu type is taken from the
		 * properties of the active build configuration.
		 * </p>
		 * <p>
		 * If the selection did not contain a valid mcu type <code>null</code> is returned
		 * 
		 * @param selection
		 *            <code>IStructuredSelection</code> from the Eclipse Selection Services
		 * @return String with the mcu id or <code>null</null> if no mcu id was found.
		 */
		private String getMCUId(IStructuredSelection selection) {

			Object item = selection.getFirstElement();
			if (item == null) {
				return null;
			}
			IProject project = null;

			// See if the given is an IProject (directly or via IAdaptable
			if (item instanceof IProject) {
				project = (IProject) item;
			} else if (item instanceof IAdaptable) {
				IAdaptable adaptable = (IAdaptable) item;
				project = (IProject) adaptable.getAdapter(IProject.class);
			}

			if (project != null) {
				// Ok, the item is a IProject
				try {
					IProjectNature nature = project.getNature(PluginIDs.NATURE_ID);
					if (nature != null) {
						// This is an AVR Project
						// Get the AVR properties for the active build
						// configuration and fetch the mcu id from it.
						ProjectPropertyManager projprops = ProjectPropertyManager
								.getPropertyManager(project);
						AVRProjectProperties props = projprops.getActiveProperties();
						return props.getMCUId();
					}
				} catch (CoreException e) {
					return null;
				}
			} else if (item instanceof String) {
				// The item was not an IProject, but a String
				String mcuname = (String) item;
				String mcuid = AVRMCUidConverter.name2id(mcuname);
				return mcuid;
			}

			// Selection does not contain a mcuid
			return null;
		}

	}

}