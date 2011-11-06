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
 * $Id: ByteValuesEditorDialog.java 851 2010-08-07 19:37:00Z innot $
 *     
 *******************************************************************************/
package it.baeyens.avreclipse.ui.dialogs;

import it.baeyens.avreclipse.core.toolinfo.fuses.ByteValues;
import it.baeyens.avreclipse.ui.editors.BitFieldEditorSectionPart;
import it.baeyens.avreclipse.ui.editors.ByteValuesMainPart;
import it.baeyens.avreclipse.ui.editors.ByteValuesTitlePart;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.forms.FormDialog;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.TableWrapData;
import org.eclipse.ui.forms.widgets.TableWrapLayout;


/**
 * A Fuse Byte Editor as a Dialog.
 * <p>
 * This Dialog is called from the AVRDude Fuses Property Dialog to optionally edit fuse byte values
 * in the same fashion as the Editor for .fuses files.<br>
 * It will show all BitFields of the given ByteValues as {@link BitFieldEditorSectionPart}s for
 * editing.
 * </p>
 * To use this Dialog instantiate it with the <code>ByteValues</code> to edit and call
 * {@link #open()}. While this Dialog will only commit any modifications to the given
 * <code>ByteValues</code> if the <em>OK</em> was pressed, callers should not depend on this and
 * should discard the <code>ByteValues</code> object after the <em>Cancel</em> button has been
 * pressed.
 * </p>
 * <p>
 * A safe access pattern could look like this:
 * 
 * <pre>
 *     	ByteValues values = ....
 * 	   	ByteValuesEditorDialog dialog = new ByteValuesEditorDialog(getShell(), values);
 *     	dialog.create();
 *      dialog.optimizeSize();
 *  	if (dialog.open() == Dialog.OK) {
 *  		ByteValues newvalues = dialog.getByteValues();
 *  		....
 *  	}
 * </pre>
 * 
 * <p>
 * Because the <code>ColumnLayout</code> used in the Dialog has some problems when its size is not
 * constrained. It will grab to much screen space, sometimes all of it. The {@link #optimizeSize()}
 * method contains a fix to reduce the size of the dialog to the minimum required. Alternatively a
 * fixed size can be set with <code>dialog.getShell().setSize(int width, int height)</code>.
 * </p>
 * <p>
 * Note: there is currently an unresolved bug that when the alphabetically first BitField shown is
 * of a Radio Button type and its value is undefined (<code>-1</code>) then the first radio
 * button gets set to true anyway. This is because SWT (or Windows) will set the first radio button
 * to true whenever the control gets the focus, and the first control on the screen will always get
 * the focus.
 * </p>
 * 
 * @author Thomas Holland
 * @since 2.3
 * 
 */
public class ByteValuesEditorDialog extends FormDialog {

	/**
	 * The <code>ByteValues</code> this dialog works with. Its content will only be modified when
	 * the <em>OK</em> button is pressed.
	 */
	private final ByteValues	fByteValues;

	private IManagedForm		fForm;

	private ByteValuesMainPart	fMainPart;

	/**
	 * Instantiate a new Dialog.
	 * <p>
	 * Note that the Dialog will not be shown until the {@link #open()} method is called.
	 * </p>
	 * 
	 * @param parentShell
	 *            <code>Shell</code> to associate this Dialog with, so that it always stays on top
	 *            of the given Shell.
	 * @param values
	 *            The <code>ByteValues</code> to edit.
	 */
	public ByteValuesEditorDialog(Shell parentShell, ByteValues values) {
		super(parentShell);

		fByteValues = values;

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.dialogs.Dialog#okPressed()
	 */
	@Override
	protected void okPressed() {
		// Only commit the changes if any have been made and the OK button was pressed.
		if (fForm.isDirty()) {
			fForm.commit(false);
		}
		super.okPressed();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.forms.FormDialog#createFormContent(org.eclipse.ui.forms.IManagedForm)
	 */
	@Override
	protected void createFormContent(IManagedForm managedForm) {

		// The general setup has already been done by the FormDialog superclass.
		// We only set the title for the dialog and let fillBody() add the rest of the content.
		// Once it is finished we can tell the form about the ByteValues we are supposed to edit.

		this.getShell().setText(fByteValues.getType().toString() + " Editor");

		fForm = managedForm;

		fillBody(managedForm);
	}

	/**
	 * Fill the managed Form with the SectionParts for all BitFields.
	 * 
	 * @param managedForm
	 * @param toolkit
	 */
	private void fillBody(IManagedForm managedForm) {

		Composite body = managedForm.getForm().getBody();
		FormToolkit toolkit = managedForm.getToolkit();

		body.setLayout(new TableWrapLayout());

		// Add a part that will update the form title to the latest MCU.
		ByteValuesTitlePart titlepart = new ByteValuesTitlePart();
		managedForm.addPart(titlepart);

		// The main section has all BitField sections
		Composite main = toolkit.createComposite(body);
		main.setLayoutData(new TableWrapData(TableWrapData.FILL));
		ByteValuesMainPart mainpart = new ByteValuesMainPart(main, fByteValues);
		managedForm.addPart(mainpart);

		fMainPart = mainpart;

		managedForm.setInput(fByteValues);
	}

	/**
	 * Try to optimize the size of the dialog.
	 * <p>
	 * This is done by determining the width of the widest BitField section. This is the minimum
	 * width of the dialog.<br>
	 * If two sections will fit on the screen, then the width of the dialog is set to twice the the
	 * minimum width for a two column layout.
	 * </p>
	 * <p>
	 * This method must be called after {@link #create()}, but before {@link #open()}.
	 * </p>
	 * <p>
	 * This method is a solution for the layout problems of the <code>ColumnLayout</code> used for
	 * the BitField sections. If <code>ColumnLayout</code> is not constrained by the shell size it
	 * will grab to much screen-space, usually the complete screen.
	 * <p>
	 * 
	 * 
	 */
	public void optimizeSize() {
		//
		// Even after single stepping thru ColumnLayout I am not sure why ColumnLayout grabs to much
		// space. It seems like it always tries to get the width for three columns, and as those do
		// not fit on a 1024px screen will use the maximum available while falling back to a two
		// column layout, thus rendering the columns much wider than required.
		//
		// With regard to the height of a ColumnLayout I am even more at a loss what ColumnLayout is
		// doing and why it sometimes grabs all screen space. It could be because at one point in
		// the layout process it calculates the minimum size for each child control, which due to
		// label wrapping will result in the minimum height of each section being much higher than
		// actually required.
		//
		Point dialogareasize = getContents().computeSize(SWT.DEFAULT, SWT.DEFAULT);
		Rectangle screensize = getShell().getDisplay().getBounds();

		// Get the width of the widest Section. As this does not take borders and margins into
		// account, I add a arbitrary 12,5% fudge-factor. This value seems to work quite well and is
		// much easier to determine as all the margins.
		int columnwidth = fMainPart.getMaxWidth();
		int width = columnwidth + columnwidth / 8;

		// If we can fit two columns on the screen than do it.
		boolean twocolumn = screensize.width > width * 2;
		width *= twocolumn ? 2 : 1;

		// For the height we use the current dialogareasize, which seems to always work, regardless
		// of a single or double column layout (I don't know why!).
		// The 20 is a little addition for the the top and bottom border. Should try to determine
		// this dynamically, but right now I can't be bothered to find out how to determine this.
		// Maybe you can?
		int height = dialogareasize.y + 20;

		// Now that we have a good width and height we can set the shell of the dialog accordingly.
		getShell().setSize(width, height);
	}

	/**
	 * Get the <code>ByteValues</code> this Dialog has worked with.
	 * <p>
	 * This method should only be called after {@link #open()}, otherwise the returned ByteValues
	 * may not reflect the latest modifications by the user.
	 * </p>
	 * 
	 * @return <code>ByteValues</code> with the new settings.
	 */
	public ByteValues getResult() {
		// TODO: Do we really need this?
		// This is probably redundant, because we return a reference to an object that we got from
		// the caller. So unless the Dialog object has been passed around the caller should still
		// have the reference himself.
		// (This code is a leftover from when the Dialog would make a copy of the ByteValues. But
		// this is not a good idea because the ByteValues could be a subclass, like FileByteValues
		// and copying it would change the type - I know, stupid design, but I work on it some other
		// time.)
		return fByteValues;
	}
}
