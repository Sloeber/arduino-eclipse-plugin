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
 * $Id: ByteValuesSourceEditor.java 851 2010-08-07 19:37:00Z innot $
 *     
 *******************************************************************************/
package it.baeyens.avreclipse.ui.editors;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.editors.text.TextEditor;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.editor.FormEditor;
import org.eclipse.ui.forms.editor.IFormPage;

/**
 * Plain <code>TextEditor</code> wrapped in a IFormPart.
 * <p>
 * While the {@link FusesEditor} would accept an <code>TextEditor</code> directly, we use this
 * wrapper for two reasons:
 * <ul>
 * <li>To make the interface consistent with the {@link ByteValuesFormEditor}</li>
 * <li>To have control over the part name, which is always set to the filename by a
 * <code>TextEditor</code>, but which should be "source" (see {@link #doSetInput(IEditorInput)})</li>
 * </ul>
 * </p>
 * 
 * @author Thomas Holland
 * @since 2.3
 * 
 */
public class ByteValuesSourceEditor extends TextEditor implements IFormPage {

	/** Id of this editor. Set by the parent <code>FusesEditor</code> to "sourceEditorPageId". */
	private final String	fId;

	/** Parent editor. */
	private FormEditor		fParentEditor;

	/** Tab page index of this editor. */
	private int				fIndex;

	/** Part name for this editor. Set by the parent <code>FusesEditor</code> to "source". */
	private final String	fTitle;

	/** The SWT control for this editor page. */
	private Control			fPartControl;

	/**
	 * A constructor that creates the editor page and initializes it.
	 * 
	 * @param editor
	 *            the parent editor
	 * @param id
	 *            the unique identifier
	 * @param title
	 *            the page title
	 */
	public ByteValuesSourceEditor(FormEditor editor, String id, String title) {
		fId = id;
		fTitle = title;
		setPartName(title);
		setContentDescription(title);
		setDocumentProvider(FuseFileDocumentProvider.getDefault());
		initialize(editor);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.forms.editor.IFormPage#initialize(org.eclipse.ui.forms.editor.FormEditor)
	 */
	@Override
	public void initialize(FormEditor editor) {
		fParentEditor = editor;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.forms.editor.IFormPage#canLeaveThePage()
	 */
	@Override
	public boolean canLeaveThePage() {
		// TODO Change this to inhibit switching if there is a syntax error (invalid mcu)
		return true;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.forms.editor.IFormPage#getEditor()
	 */
	@Override
	public FormEditor getEditor() {
		return fParentEditor;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.forms.editor.IFormPage#getId()
	 */
	@Override
	public String getId() {
		return fId;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.forms.editor.IFormPage#getIndex()
	 */
	@Override
	public int getIndex() {
		return fIndex;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.forms.editor.IFormPage#setIndex(int)
	 */
	@Override
	public void setIndex(int index) {
		fIndex = index;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.forms.editor.IFormPage#getManagedForm()
	 */
	@Override
	public IManagedForm getManagedForm() {
		// The source editor does not have a managed form.
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.texteditor.AbstractDecoratedTextEditor#createPartControl(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	public void createPartControl(Composite parent) {
		super.createPartControl(parent);

		// Assume that the last child control of the parent is the one just created by the call to
		// out superclass (TextEditor).
		Control[] children = parent.getChildren();
		fPartControl = children[children.length - 1];
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.forms.editor.IFormPage#getPartControl()
	 */
	@Override
	public Control getPartControl() {
		return fPartControl;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.forms.editor.IFormPage#isActive()
	 */
	@Override
	public boolean isActive() {
		return this.equals(fParentEditor.getActivePageInstance());
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.forms.editor.IFormPage#setActive(boolean)
	 */
	@Override
	public void setActive(boolean active) {
		// nothing to do here
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.forms.editor.IFormPage#isEditor()
	 */
	@Override
	public boolean isEditor() {
		// This is only a wrapper for a TextEditor
		return true;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.forms.editor.IFormPage#selectReveal(java.lang.Object)
	 */
	@Override
	public boolean selectReveal(Object object) {
		// not supported
		return false;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.editors.text.TextEditor#doSetInput(org.eclipse.ui.IEditorInput)
	 */
	@Override
	protected void doSetInput(IEditorInput input) throws CoreException {
		super.doSetInput(input);

		// The super implementation of doSetInput() will overwrite the part name with the name of
		// the file. Undo this:
		setPartName(fTitle);
	}

}
