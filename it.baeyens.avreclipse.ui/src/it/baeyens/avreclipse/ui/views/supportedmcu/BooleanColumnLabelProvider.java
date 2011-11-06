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
 * $Id: BooleanColumnLabelProvider.java 851 2010-08-07 19:37:00Z innot $
 *     
 *******************************************************************************/
package it.baeyens.avreclipse.ui.views.supportedmcu;

import it.baeyens.avreclipse.core.IMCUProvider;
import it.baeyens.avreclipse.ui.AVRUIPlugin;

import org.eclipse.jface.viewers.OwnerDrawLabelProvider;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.TableItem;


/**
 * A special LabelProvider that draws a checkmark or a cross, depending on whether a MCU is
 * supported or not.
 * <p>
 * Extends {@link OwnerDrawLabelProvider} to draw the symbol centered in the cell.
 * </p>
 * 
 * @author Thomas Holland
 * @since 2.2.
 */
public class BooleanColumnLabelProvider extends OwnerDrawLabelProvider {

	/**
	 * The provider of the yes/no information. The {@link IMCUProvider#hasMCU(String)} method of the
	 * provider is called to determine what image to draw
	 */
	private IMCUProvider	fProvider	= null;

	/** The image for a supported MCU */
	private final Image		fYesImage;

	/** The image for an unsupported MCU */
	private final Image		fNoImage;

	/**
	 * Initialize this ColumnLabelProvider with an IMCUProvider
	 * 
	 * @param provider
	 *            <code>IMCUProvider</code> that will be used for this column
	 */
	public BooleanColumnLabelProvider(IMCUProvider provider) {
		fProvider = provider;

		// Load the images
		fYesImage = AVRUIPlugin.getImageDescriptor("icons/viewer16/yes.png").createImage();
		fNoImage = AVRUIPlugin.getImageDescriptor("icons/viewer16/no.png").createImage();
	}

	/**
	 * Disposes the allocated images and calls the superclass to dispose an other resources.
	 * 
	 * @see OwnerDrawLabelProvider#dispose();
	 */
	@Override
	public void dispose() {
		fYesImage.dispose();
		fNoImage.dispose();
		super.dispose();
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.jface.viewers.OwnerDrawLabelProvider#measure(org.eclipse.swt.widgets.Event,
	 * java.lang.Object)
	 */
	@Override
	protected void measure(Event event, Object element) {
		// Set the bounds of the images
		Rectangle yesbounds = fYesImage.getBounds();
		Rectangle nobounds = fNoImage.getBounds();
		event.width = Math.max(yesbounds.width, nobounds.width);
		event.height = Math.max(yesbounds.height, nobounds.height);
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.jface.viewers.OwnerDrawLabelProvider#paint(org.eclipse.swt.widgets.Event,
	 * java.lang.Object)
	 */
	@Override
	protected void paint(Event event, Object element) {

		String mcuid = (String) element;
		Image img = fProvider.hasMCU(mcuid) ? fYesImage : fNoImage;

		if (img != null) {
			// draw the image centered into the cell
			Rectangle bounds = ((TableItem) event.item).getBounds(event.index);
			Rectangle imgBounds = img.getBounds();
			bounds.width /= 2;
			bounds.width -= imgBounds.width / 2;
			bounds.height /= 2;
			bounds.height -= imgBounds.height / 2;

			int x = bounds.width > 0 ? bounds.x + bounds.width : bounds.x;
			int y = bounds.height > 0 ? bounds.y + bounds.height : bounds.y;

			event.gc.drawImage(img, x, y);
		}
	}

	/**
	 * Handle the erase event.
	 * <p>
	 * This method does nothing and is only here to prevent the superclass from messing up
	 * </p>
	 */
	@Override
	protected void erase(Event event, Object element) {
		// Just let SWT handle this just right
		// (unlike the implementation in the superclass, which
		// does not account for the focus)
		return;
	}
}
