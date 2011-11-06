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
 * $Id: TabAVRDudeLockbits.java 851 2010-08-07 19:37:00Z innot $
 *     
 *******************************************************************************/
package it.baeyens.avreclipse.ui.propertypages;

import it.baeyens.avreclipse.core.avrdude.AVRDudeException;
import it.baeyens.avreclipse.core.avrdude.BaseBytesProperties;
import it.baeyens.avreclipse.core.properties.AVRDudeProperties;
import it.baeyens.avreclipse.core.toolinfo.AVRDude;
import it.baeyens.avreclipse.core.toolinfo.fuses.ByteValues;
import it.baeyens.avreclipse.core.toolinfo.fuses.FuseType;

import org.eclipse.core.runtime.IProgressMonitor;


/**
 * The AVRDude Lockbits Tab page.
 * <p>
 * On this tab, the following properties are edited:
 * <ul>
 * <li>Upload of the Lockbits</li>
 * </ul>
 * The lockbit values can either be entered directly, or a lockbits file can be selected which
 * provides the lockbit values.
 * </p>
 * 
 * @author Thomas Holland
 * @since 2.2
 * 
 */
public class TabAVRDudeLockbits extends AbstractTabAVRDudeBytes {

	private final static String[]	LABELS			= new String[] { "Lockbits", "lockbits" };

	/** The file extensions for lockbits files. Used by the file selector. */
	private final static String[]	LOCKBITS_EXTS	= new String[] { "*.locks" };

	/*
	 * (non-Javadoc)
	 * 
	 * @see it.baeyens.avreclipse.ui.propertypages.AbstractTabAVRDudeBytes#getType()
	 */
	@Override
	protected FuseType getType() {
		return FuseType.LOCKBITS;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see it.baeyens.avreclipse.ui.propertypages.AbstractTabAVRDudeBytes#getByteEditorLabel(int)
	 */
	@Override
	protected String getByteEditorLabel(int index) {
		// don't use a label for the lockbits byte value editor
		return "";
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see it.baeyens.avreclipse.ui.propertypages.AbstractTabAVRDudeBytes#getByteProps(it.baeyens.avreclipse.core.properties.AVRDudeProperties)
	 */
	@Override
	protected BaseBytesProperties getByteProps(AVRDudeProperties avrdudeprops) {
		return avrdudeprops.getLockbitBytes(getCfg());
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see it.baeyens.avreclipse.ui.propertypages.AbstractTabAVRDudeBytes#getByteValues(it.baeyens.avreclipse.core.properties.AVRDudeProperties)
	 */
	@Override
	protected ByteValues getByteValues(AVRDudeProperties avrdudeprops, IProgressMonitor monitor)
			throws AVRDudeException {
		return AVRDude.getDefault().getLockbits(avrdudeprops.getProgrammer(), monitor);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see it.baeyens.avreclipse.ui.propertypages.AbstractTabAVRDudeBytes#getFileExtensions()
	 */
	@Override
	protected String[] getFileExtensions() {
		return LOCKBITS_EXTS;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see it.baeyens.avreclipse.ui.propertypages.AbstractTabAVRDudeBytes#getLabelString(int)
	 */
	@Override
	protected String[] getLabels() {
		return LABELS;
	}

}
