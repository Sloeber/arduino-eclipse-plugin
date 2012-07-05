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
 * $Id: FusesEditor.java 851 2010-08-07 19:37:00Z innot $
 *     
 *******************************************************************************/
package it.baeyens.avreclipse.ui.editors;

import it.baeyens.arduino.common.ArduinoConst;
import it.baeyens.avreclipse.AVRPlugin;
import it.baeyens.avreclipse.core.toolinfo.fuses.ByteValues;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.forms.editor.FormEditor;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.ide.IGotoMarker;
import org.eclipse.ui.texteditor.IElementStateListener;


/**
 * The FuseByte File Editor.
 * <p>
 * This editor has two pages
 * <ul>
 * <li>page 0 contains the form based editor</li>
 * <li>page 1 is a simple text editor to edit the raw file</li>
 * </ul>
 * While the first one is a real IFormPage, the latter is only a TextEditor wrapped in an IFormPage
 * to make the interfaces consistent.
 * </p>
 * <p>
 * The basic source for this editor is an <code>IFile</code>, extracted from the
 * <code>IEditorInput</code> to this editor. With the <code>IEditorInput</code> the
 * {@link FuseFileDocumentProvider} can provide both the <code>IDocument</code> required for the
 * source text editor as well as the {@link ByteValues} for the form editor. Both target formats are
 * linked internally and changes to one are also applied to the other.
 * </p>
 * <p>
 * As the source file is a text document, the source editor acts as a master for this editor. The
 * save, save as and revert user actions are sent to the source editor and handled there.
 * </p>
 * <p>
 * This editor implements two listeners:
 * <ul>
 * <li>A resource change listener which will close the editor when the parent project is closed.</li>
 * <li>An element state listener which will close the editor when the source file is deleted and
 * which will update the EditorInput when the source file is renamed / moved.</li>
 * </ul>
 * 
 * @see LockbitsEditor
 * 
 * @author Thomas Holland
 * @since 2.3
 * 
 */
public class FusesEditor extends FormEditor implements IResourceChangeListener,
		IElementStateListener, IGotoMarker {

	private final static String			FORMEDITOR_ID	= "formEditorPageId";
	private final static String			SOURCEEDITOR_ID	= "sourceEditorPageId";

	/** The form based editor for the source ByteValues. */
	private ByteValuesFormEditor		fFuseEditor;

	/** The source text editor for the source file. */
	private ByteValuesSourceEditor		fSourceEditor;

	/**
	 * The document provider for fuse file documents. Only used to add a Element state listener to
	 * be notified when the source file is renamed, moved or deleted.
	 */
	private FuseFileDocumentProvider	fDocumentProvider;

	/**
	 * Creates a fuse bytes editor.
	 * <p>
	 * Registers this editor as an <code>ResourceChangeListener</code> to be informed about
	 * project closure.
	 * </p>
	 */
	public FusesEditor() {
		super();
		ResourcesPlugin.getWorkspace().addResourceChangeListener(this);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.forms.editor.FormEditor#init(org.eclipse.ui.IEditorSite,
	 *      org.eclipse.ui.IEditorInput)
	 */
	@Override
	public void init(IEditorSite site, IEditorInput editorInput) throws PartInitException {
		if (!(editorInput instanceof IFileEditorInput))
			throw new PartInitException("Invalid Input: Must be IFileEditorInput");
		super.init(site, editorInput);

		IFile file = (IFile) editorInput.getAdapter(IFile.class);
		if (file == null) {
			throw new PartInitException("Invalid Input: Must be a IFile");
		}

		if (!file.exists()) {
			throw new PartInitException("Invalid Input: File does not exist.");
		}

		// Get the document provider and add ourself as a listener, so we are informed if the editor
		// input file is moved, renamed or deleted.
		fDocumentProvider = FuseFileDocumentProvider.getDefault();
		fDocumentProvider.addElementStateListener(this);

		// Use the file name as a part name
		setPartName(editorInput.getName());

		// Description is not required as it should be obvious to the user what he is editing.
		// setContentDescription("Edit Fuse Bytes");

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.forms.editor.FormEditor#addPages()
	 */
	@Override
	protected void addPages() {
		try {
			fFuseEditor = new ByteValuesFormEditor(this, FORMEDITOR_ID, "BitFields");
			addPage(fFuseEditor);
			fSourceEditor = new ByteValuesSourceEditor(this, SOURCEEDITOR_ID, "Source");
			addPage(fSourceEditor, getEditorInput());
		} catch (PartInitException pie) {
			IStatus status = new Status(IStatus.ERROR, ArduinoConst.CORE_PLUGIN_ID,
					"Could not add editor page to the FusesEditor", pie);
			AVRPlugin.getDefault().log(status);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.forms.editor.FormEditor#dispose()
	 */
	@Override
	public void dispose() {
		fDocumentProvider.removeElementStateListener(this);
		ResourcesPlugin.getWorkspace().removeResourceChangeListener(this);
		super.dispose();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.part.EditorPart#doSave(org.eclipse.core.runtime.IProgressMonitor)
	 */
	@Override
	public void doSave(IProgressMonitor monitor) {

		IWorkspaceRunnable batchSave = new IWorkspaceRunnable() {

			@Override
			public void run(IProgressMonitor monitor) {
				// Commit the pages
				fFuseEditor.doSave(monitor);
				fSourceEditor.doSave(monitor);
				editorDirtyStateChanged();
			}
		};
		try {
			ResourcesPlugin.getWorkspace().run(batchSave, null, IWorkspace.AVOID_UPDATE, monitor);
		} catch (CoreException e) {
			e.printStackTrace();
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.part.EditorPart#doSaveAs()
	 */
	@Override
	public void doSaveAs() {
		{
			// TODO: implement our own Save As dialog to inhibit changes to the extension.
			// The current implementation lets the source text editor do the save as operation and
			// gets the new IEditorInput from the source editor.
			// However, if the user changes the extension the file will not be recognized as a
			// fuse/locks anymore once the editor is closed. This side effect has to be document in
			// the user guide, but it still would be nicer if the user could not change the
			// extension at all.

			fSourceEditor.doSaveAs();
			IEditorInput newinput = fSourceEditor.getEditorInput();
			fFuseEditor.setEditorInput(newinput);
			setInput(newinput);
			editorDirtyStateChanged();
			IFileEditorInput newfileinput = (IFileEditorInput) newinput;
			setPartName(newfileinput.getName());
		}

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.ide.IGotoMarker#gotoMarker(org.eclipse.core.resources.IMarker)
	 */
	@Override
	public void gotoMarker(IMarker marker) {
		// The only markers we create are the problem markers from the source editor. So we activate
		// this editor if the user wants to go to a marker.
		// Change to the source editor and goto to the marker
		setActivePage(SOURCEEDITOR_ID);
		IDE.gotoMarker(fSourceEditor, marker);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.part.EditorPart#isSaveAsAllowed()
	 */
	@Override
	public boolean isSaveAsAllowed() {
		// see #doSaveAs() above for the caveat with "save as"
		return true;
	}

	// ---- Resource Change Listener Method ------

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.core.resources.IResourceChangeListener#resourceChanged(org.eclipse.core.resources.IResourceChangeEvent)
	 */
	@Override
	public void resourceChanged(final IResourceChangeEvent event) {

		switch (event.getType()) {
			case IResourceChangeEvent.PRE_CLOSE:
				handleCloseEvent(event);
				break;
			case IResourceChangeEvent.POST_CHANGE:
				// We could try to listen for resource move / rename / delete events here. However I
				// found it easier to listen to the element change events from the document
				// provider, because these events work with IEditorInput objects directly. OTOH the
				// resource change events only have IPaths and its a bit tedious to go from an IPath
				// to an IEditorInput.
				break;
			default:
				// Other types of Resource change events are ignored.
		}
	}

	/**
	 * Handle a <code>PRE_CLOSE</code> resource change event.
	 * <p>
	 * This method will go through all workbench pages, test if they are for the project to be
	 * closed, and close the Editor of the page.
	 * </p>
	 * 
	 * @param event
	 */
	private void handleCloseEvent(final IResourceChangeEvent event) {
		Display.getDefault().asyncExec(new Runnable() {
			@Override
			public void run() {
				IWorkbenchPage[] pages = getSite().getWorkbenchWindow().getPages();
				for (int i = 0; i < pages.length; i++) {
					IFile sourcefile = (IFile) getEditorInput().getAdapter(IFile.class);
					if (sourcefile != null && sourcefile.getProject().equals(event.getResource())) {
						IEditorPart editorPart = pages[i].findEditor(getEditorInput());
						pages[i].closeEditor(editorPart, true);
					}
				}
			}
		});
	}

	// ---- DocumentProvider Element Change Listener Methods ------

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.texteditor.IElementStateListener#elementDeleted(java.lang.Object)
	 */
	@Override
	public void elementDeleted(Object element) {

		// Close the editor if the file has been deleted.

		// If there are unsaved changed the source file editor will inhibit the closure of this
		// editor and will ask the user to save the changes to a different file once the editor is
		// closed by the user.

		if (element != null && element.equals(getEditorInput())) {
			close(true);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.texteditor.IElementStateListener#elementMoved(java.lang.Object,
	 *      java.lang.Object)
	 */
	@Override
	public void elementMoved(Object originalElement, Object movedElement) {

		// sou

		if (movedElement == null) {
			// An element has been moved to file nirvana. Handle this as a delete.
			elementDeleted(originalElement);
		}

		// Check that our source file has been moved
		IEditorInput original = null;
		IEditorInput moved = null;

		if (originalElement instanceof IEditorInput) {
			original = (IEditorInput) originalElement;
		}
		if (movedElement instanceof IEditorInput) {
			moved = (IEditorInput) movedElement;
		}

		if (moved != null && original != null && original.equals(getEditorInput())) {
			// OK - our source file has moved.
			// The source editor has its own listener, so we don't need to tell him.
			// Just tell the form editor and update the filename.
			fFuseEditor.setEditorInput(moved);
			setPartName(moved.getName());
			setInputWithNotify(moved);
		}
	}

	@Override
	public void elementContentAboutToBeReplaced(Object element) {
		// no actions required
	}

	@Override
	public void elementContentReplaced(Object element) {
		// no actions required
	}

	@Override
	public void elementDirtyStateChanged(Object element, boolean isDirty) {
		// no actions required
	}

}
