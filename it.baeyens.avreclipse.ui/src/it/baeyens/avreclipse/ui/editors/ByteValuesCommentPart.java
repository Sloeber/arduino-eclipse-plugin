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
 * $Id: ByteValuesCommentPart.java 851 2010-08-07 19:37:00Z innot $
 *     
 *******************************************************************************/
package it.baeyens.avreclipse.ui.editors;

import it.baeyens.avreclipse.core.toolinfo.fuses.ByteValueChangeEvent;
import it.baeyens.avreclipse.core.toolinfo.fuses.ByteValues;
import it.baeyens.avreclipse.core.toolinfo.fuses.IByteValuesChangeListener;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.IFormPart;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.SectionPart;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;


/**
 * A <code>SectionPart</code> that can edit the comment field of a <code>ByteValues</code>
 * source object.
 * <p>
 * This class automatically creates a <code>Section</code> and adds it to the parent composite.
 * After the class has been instantiated it must be added to a <code>IManagedForm</code> to
 * participate in the lifecycle management of the managed form.
 * 
 * <pre>
 *     Composite parent = ...
 *     FormToolkit toolkit = ...
 *     IManagedForm managedForm = ...
 * 
 *     IFormPart part = new ByteValuesCommentPart(parent, toolkit, Section.TITLE_BAR);
 *     managedForm.addPart(part);
 * </pre>
 * 
 * </p>
 * <p>
 * This class implements the {@link IFormPart} interface to participate in the lifecycle management
 * of a managed form. To set the value of the BitField use
 * 
 * <pre>
 *     ByteValues bytevalues = ...
 *     managedForm.setInput(bytevalues);
 * </pre>
 * 
 * The <code>ByteValues</code> passed to the managedForm is the model for this
 * <code>SectionPart</code>. Unlike normal IFormParts all changes to the source ByteValues are
 * applied immediately, because other other editors might be affected by the change. Therefore this
 * class uses its own dirty / stale management and does not use the one provided by the superclass
 * {@link SectionPart}
 * </p>
 * <p>
 * This part also adds itself as a listener for changes to the ByteValues model. If the ByteValues
 * comment gets changed from outside, then this part is marked as stale. The new value will be set
 * during the refresh method.
 * </p>
 * 
 * @author Thomas Holland
 * @since 2.3
 * 
 */
public class ByteValuesCommentPart extends SectionPart implements IByteValuesChangeListener {

	//
	// This stuff could be integrated into the ByteValuesMainPart class.
	// I have made it separate to
	//
	// 1. do not show this in the ByteValueEditor dialog for immediate fuse byte values, which
	// currently does not know anything about comments.
	//
	// 2. have more control in the layout of the FuseByteEditorPage class.
	//

	/**
	 * The model for this <code>SectionPart</code>. Will only be written to in the
	 * {@link #commit(boolean)} method.
	 */
	private ByteValues	fByteValues;

	/** The text control of this section. */
	private Text		fText;

	/** Last clean comment value. Used to check if this part is currently dirty. */
	private String		fLastCleanComment;

	/** Current comment. Used to check if this part is currently stale. */
	private String		fCurrentComment;

	/**
	 * Part is currently refreshing the value. Used to inhibit the ModifyTextListener.
	 */
	private boolean		fInRefresh	= false;

	/**
	 * Create a new <code>SectionPart</code> to handle the comment of a <code>ByteValues</code>
	 * object.
	 * <p>
	 * This constructor automatically creates a new section part inside the provided parent and
	 * using the provided toolkit.
	 * </p>
	 * 
	 * @param parent
	 *            the parent
	 * @param toolkit
	 *            the toolkit to use for the section
	 * @param style
	 *            the section widget style
	 */
	public ByteValuesCommentPart(Composite parent, FormToolkit toolkit, int style) {
		super(parent, toolkit, style);
		getSection().setText("Notes");
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.forms.AbstractFormPart#initialize(org.eclipse.ui.forms.IManagedForm)
	 */
	@Override
	public void initialize(IManagedForm form) {
		super.initialize(form);

		Section parent = getSection();
		FormToolkit toolkit = form.getToolkit();
		GridData gd = new GridData(SWT.FILL, SWT.FILL, true, true);
		gd.heightHint = 54; // TODO: make it 4 lines according to the used font.

		// Create the Section client area.
		Composite client = form.getToolkit().createComposite(parent);
		parent.setClient(client);
		client.setLayout(new GridLayout());

		// And add the single Text control to it.
		fText = toolkit.createText(client, "", SWT.BORDER | SWT.MULTI);
		fText.setLayoutData(gd);
		fText.addModifyListener(new ModifyListener() {

			@Override
			public void modifyText(ModifyEvent e) {
				if (fInRefresh) {
					// don't do anything if the new text value has been set in the refresh() method.
					return;
				}
				fCurrentComment = fText.getText();
				fByteValues.setComment(fCurrentComment);

				// Our dirty state might have changed, depending on the new value.
				// Inform the parent ManagedForm - it will call our isDirty() implementation to get
				// the actual state.
				getManagedForm().dirtyStateChanged();
			}
		});

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.forms.AbstractFormPart#dispose()
	 */
	@Override
	public void dispose() {
		if (fByteValues != null) {
			fByteValues.removeChangeListener(this);
		}
		super.dispose();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.forms.AbstractFormPart#setFormInput(java.lang.Object)
	 */
	@Override
	public boolean setFormInput(Object input) {

		if (!(input instanceof ByteValues)) {
			return false;
		}

		fByteValues = (ByteValues) input;
		fByteValues.addChangeListener(this);

		refresh();

		return true;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.forms.AbstractFormPart#refresh()
	 */
	@Override
	public void refresh() {
		// refresh() was once called before setInput(), but I am not sure if this was a bug in the
		// Editor. I have left this test just in case, even if it is probably not required.
		if (fByteValues == null) {
			return;
		}

		String comment = fByteValues.getComment();
		if (comment == null) {
			comment = "";
		}
		fInRefresh = true;
		fText.setText(comment);
		fInRefresh = false;

		fLastCleanComment = fCurrentComment = comment;

		super.refresh();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.forms.AbstractFormPart#commit(boolean)
	 */
	@Override
	public void commit(boolean onSave) {

		fLastCleanComment = fCurrentComment = fByteValues.getComment();

		super.commit(onSave);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.forms.AbstractFormPart#isDirty()
	 */
	@Override
	public boolean isDirty() {
		// This part is dirty if the source ByteValues has a different value than what it had on the
		// last setInput(), refresh() or commit()
		String comment = fByteValues.getComment();
		if (comment == null && fLastCleanComment == null) {
			return false;
		}
		if (comment == null && fLastCleanComment != null) {
			return true;
		}
		if (fByteValues.getComment().equals(fLastCleanComment)) {
			return false;
		}
		return true;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.forms.AbstractFormPart#isStale()
	 */
	@Override
	public boolean isStale() {
		// This part is stale if the source ByteValues has a different value than what this part
		// thinks it should have.
		String comment = fByteValues.getComment();
		if (comment == null && fCurrentComment == null) {
			return false;
		}
		if (comment == null && fCurrentComment != null) {
			return true;
		}
		if (fByteValues.getComment().equals(fCurrentComment)) {
			return false;
		}
		return true;
	}


	@Override
	public void byteValuesChanged(ByteValueChangeEvent[] events) {

		if (fInRefresh) {
			// don't listen to our own changes to the Comment
			return;
		}

		// go through all events and if any event changes the comment to a different value then mark
		// ourself as stale.
		for (ByteValueChangeEvent event : events) {
			if (event.name.equals(ByteValues.COMMENT_CHANGE_EVENT)) {
				// Our stale state might have changed, depending on the new value.
				// Inform the parent ManagedForm - it will call our isStale() implementation to get
				// the actual state.
				getManagedForm().staleStateChanged();
			}
		}
	}

}
