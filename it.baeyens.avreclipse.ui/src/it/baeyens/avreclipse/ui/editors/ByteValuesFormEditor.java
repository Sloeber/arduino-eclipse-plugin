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
 * $Id: ByteValuesFormEditor.java 851 2010-08-07 19:37:00Z innot $
 *     
 *******************************************************************************/
package it.baeyens.avreclipse.ui.editors;

import it.baeyens.arduino.ArduinoConst;
import it.baeyens.avreclipse.AVRPlugin;
import it.baeyens.avreclipse.core.toolinfo.fuses.ByteValues;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.editor.FormPage;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;
import org.eclipse.ui.forms.widgets.TableWrapData;
import org.eclipse.ui.forms.widgets.TableWrapLayout;


/**
 * An <code>IEditorPart</code> to edit a <code>ByteValues</code> object.
 * <p>
 * This part will take an <code>IEditorInput</code> pointing to a fuses file and get the
 * <code>ByteValues</code> object for it from the {@link FuseFileDocumentProvider}. This object
 * is then used as the input for the managed form of this part.
 * </p>
 * <p>
 * The editor consists of three <code>IFormParts</code>:
 * <ul>
 * <li>The title part which will always display the current MCU in the form title.</li>
 * <li>The BitField sections part to edit the BitFields.</li>
 * <li>The comment SectionPart to edit the comment.</li>
 * </ul>
 * In addition to these three parts some actions are added to the form ToolBar (shown to the right
 * of the title).
 * </p>
 * 
 * @see FusesEditor
 * 
 * @author Thomas Holland
 * @since 2.3
 * 
 */
public class ByteValuesFormEditor extends FormPage {

	/** Provider for the <code>ByteValues</code> objects. */
	private final FuseFileDocumentProvider	fDocumentProvider;

	/** The current <code>ByteValues</code> that this editor works with. */
	private ByteValues						fByteValues;

	/**
	 * @param editor
	 *            The parent Editor. Must be a {@link FusesEditor}.
	 * @param id
	 *            The id of this page.
	 * @param title
	 *            The title of this editor, shown in the bottom tab of this editor.
	 */
	public ByteValuesFormEditor(FusesEditor editor, String id, String title) {
		super(editor, id, title);
		fDocumentProvider = FuseFileDocumentProvider.getDefault();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.forms.editor.FormPage#init(org.eclipse.ui.IEditorSite,
	 *      org.eclipse.ui.IEditorInput)
	 */
	@Override
	public void init(IEditorSite site, IEditorInput input) {
		super.init(site, input);

		// Connect to our special document provider and get the byte values for it
		try {
			fDocumentProvider.connect(input);
			fByteValues = fDocumentProvider.getByteValues(input);
		} catch (CoreException ce) {
			// Should not happen if the file exists, but log it anyway:
			IStatus status = new Status(Status.ERROR, ArduinoConst.CORE_PLUGIN_ID, "Could not open file ["
					+ input.getName() + "]", ce);
			AVRPlugin.getDefault().log(status);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.forms.editor.FormPage#dispose()
	 */
	@Override
	public void dispose() {
		fDocumentProvider.disconnect(getEditorInput());
		super.dispose();
	}

	/**
	 * Sets the input for this page.
	 * <p>
	 * Called by the parent <code>FusesEditor</code> when the source file has changed, either from
	 * an Save As action or when the source file has been moved or renamed.
	 * </p>
	 * <p>
	 * This method will connect to the new file and get the ByteValues for it. Then it informs the
	 * managed form and the superclass about the new input.
	 * </p>
	 * 
	 * @param newinput
	 *            <code>IEditorInput</code> with the new source file
	 */
	public void setEditorInput(IEditorInput newinput) {
		// First check if the input is actually new
		if (getEditorInput().equals(newinput)) {
			// No - then no actions are required.
			return;
		}
		try {
			// New Input: disconnect the previous input (if required) and then connect the new input
			if (fByteValues != null) {
				// The previous input is still connected
				fDocumentProvider.disconnect(getEditorInput());
			}
			fDocumentProvider.connect(newinput);
			fByteValues = fDocumentProvider.getByteValues(newinput);
		} catch (CoreException ce) {
			// TODO: log exception
			return;
		}

		// Next update the managed form. This needs to be done before the superclass is informed as
		// per the setInputWithNotify() API.
		getManagedForm().setInput(fByteValues);

		// And finally tell the superclass and all listeners about the new input.
		setInputWithNotify(newinput);

	}

	/**
	 * Get the current filename.
	 * 
	 * @return The current filename
	 */
	public String getFilename() {

		// This is only used by MCUChangeActionPart to show the filename in the MCU change dialog.

		IEditorInput input = getEditorInput();
		return input.getName();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.forms.editor.FormPage#createFormContent(org.eclipse.ui.forms.IManagedForm)
	 */
	@Override
	protected void createFormContent(IManagedForm managedForm) {

		// Add the toolbar actions
		managedForm.addPart(new MCUChangeActionPart());
		managedForm.addPart(new MCUReadActionPart());
		managedForm.addPart(new MCUDefaultsActionPart());

		// and the rest of the form
		fillBody(managedForm);

		managedForm.setInput(fByteValues);
	}

	/**
	 * Fill the managed Form.
	 * <p>
	 * This method will add three parts to the form:
	 * <ul>
	 * <li>The title part which will always display the current MCU in the form title.</li>
	 * <li>The BitField sections part to edit the bitfields.</li>
	 * <li>The comment sectionpart to edit the comment.</li>
	 * </ul>
	 * </p>
	 * 
	 * @param managedForm
	 * @param toolkit
	 */
	private void fillBody(IManagedForm managedForm) {

		Composite body = managedForm.getForm().getBody();
		FormToolkit toolkit = managedForm.getToolkit();

		body.setLayout(new TableWrapLayout());

		// Add a part that will update the form title to the current MCU.
		ByteValuesTitlePart titlepart = new ByteValuesTitlePart();
		managedForm.addPart(titlepart);

		// The main section has all BitField sections
		Composite main = toolkit.createComposite(body);
		main.setLayoutData(new TableWrapData(TableWrapData.FILL_GRAB));
		managedForm.addPart(new ByteValuesMainPart(main, fByteValues));

		// The comments section is separate to cover all columns
		Composite comment = toolkit.createComposite(body);
		comment.setLayoutData(new TableWrapData(TableWrapData.FILL_GRAB));
		comment.setLayout(new FillLayout());
		ByteValuesCommentPart commentpart = new ByteValuesCommentPart(comment, toolkit,
				Section.TITLE_BAR | Section.TWISTIE | Section.EXPANDED);
		managedForm.addPart(commentpart);

	}

}
