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
 * $Id: PageAVRDudeStaticLib.java 851 2010-08-07 19:37:00Z innot $
 *     
 *******************************************************************************/
package it.baeyens.avreclipse.ui.propertypages;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;

/**
 * The AVRDude dummy page for static library projects.
 * <p>
 * AVRDude support does not make sense for static library projects. But to keep the UI consistent a
 * dummy page is shown informing the user about this.
 * </p>
 * 
 * @author Thomas Holland
 * @since 2.3.2
 * 
 */
public class PageAVRDudeStaticLib extends AbstractAVRPage {
	
	private static Image fImage = null;

	/* (non-Javadoc)
	 * @see org.eclipse.cdt.ui.newui.AbstractPage#createWidgets(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	public void createWidgets(Composite c) {

		Composite compo = new Composite(c, SWT.NONE);
		compo.setLayout(new GridLayout(2, false));

		Label icon = new Label(compo, SWT.NONE);
		icon.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
		icon.setImage(getInfoImage(c));

		Label label = new Label(compo, SWT.BOLD);
		label.setText("AVRDude is not supported for Static library projects.");
		label.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.cdt.ui.newui.AbstractPage#isSingle()
	 */
	@Override
	protected boolean isSingle() {

		// This page uses no tabs

		return true;
	}

	/**
	 * Get the Information Icon Image..
	 * 
	 * @return image the image
	 */
	private Image getInfoImage(Composite c) {
		if (fImage != null) {
			return fImage;
		}
		Display display = c.getDisplay();
		fImage = display.getSystemImage(SWT.ICON_INFORMATION);
		return fImage;
	}

}
