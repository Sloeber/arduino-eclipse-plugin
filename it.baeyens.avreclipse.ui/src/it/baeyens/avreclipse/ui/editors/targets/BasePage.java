/*******************************************************************************
 * 
 * Copyright (c) 2009, 2010 Thomas Holland (thomas@innot.de) and others
 * 
 * This program and the accompanying materials are made
 * available under the terms of the GNU Public License v3
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 * 
 * Contributors:
 *     Thomas Holland - initial API and implementation
 *     
 * $Id: BasePage.java 851 2010-08-07 19:37:00Z innot $
 *     
 *******************************************************************************/

package it.baeyens.avreclipse.ui.editors.targets;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.ui.forms.IMessageManager;
import org.eclipse.ui.forms.editor.FormEditor;
import org.eclipse.ui.forms.editor.FormPage;
import org.eclipse.ui.forms.editor.SharedHeaderFormEditor;

/**
 * @author Thomas Holland
 * @since
 * 
 */
public class BasePage extends FormPage {

	final List<ITCEditorPart>		fParts	= new ArrayList<ITCEditorPart>();

	private SharedHeaderFormEditor	fEditor;

	/**
	 * @param editor
	 * @param id
	 * @param title
	 */
	public BasePage(SharedHeaderFormEditor editor, String id, String title) {
		super(editor, id, title);

		fEditor = editor;
	}

	public BasePage(String id, String title) {
		super(id, title);
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.forms.editor.FormPage#initialize(org.eclipse.ui.forms.editor.FormEditor)
	 */
	@Override
	public void initialize(FormEditor editor) {
		if (editor instanceof SharedHeaderFormEditor) {
			fEditor = (SharedHeaderFormEditor) editor;
		}
		super.initialize(editor);
	}

	protected void registerPart(ITCEditorPart part) {
		fParts.add(part);
	}

	protected void unregisterPart(ITCEditorPart part) {
		fParts.remove(part);
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.forms.editor.FormPage#getEditor()
	 */
	// @Override
	// public SharedHeaderFormEditor getEditor() {
	// return fEditor;
	// }
	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.forms.editor.FormPage#selectReveal(java.lang.Object)
	 */
	@Override
	public boolean selectReveal(Object object) {
		if (object instanceof String) {
			String attribute = (String) object;
			for (ITCEditorPart part : fParts) {
				if (part.setFocus(attribute)) {
					// found a part that knows the attribute
					fEditor.setActivePage(getId());
					// do the set focus again because setActivePage() changes the focus
					part.setFocus(attribute);
					return true;
				}
			}
		}
		return false;
	}

	public IMessageManager getMessageManager() {
		FormEditor editor = getEditor();
		if (editor instanceof SharedHeaderFormEditor) {
			SharedHeaderFormEditor shfe = (SharedHeaderFormEditor) editor;
			return shfe.getHeaderForm().getMessageManager();
		}

		// Fallback in case the parent editor is not a SharedHeaderFormEditor
		return getManagedForm().getMessageManager();

	}

}