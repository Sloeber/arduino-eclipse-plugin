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
 * $Id: BaseReader.java 851 2010-08-07 19:37:00Z innot $
 *     
 *******************************************************************************/
package it.baeyens.avreclipse.core.toolinfo.partdescriptionfiles;

import it.baeyens.avreclipse.core.util.AVRMCUidConverter;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;


/**
 * A basic implementation of an PartDescriptionFile reader.
 * <p>
 * This class will fetch the MCU id from the given Document and pass the document to the
 * {@link #parse(Document)} method of the subclass.
 * </p>
 * 
 * @author Thomas Holland
 * @since 2.2
 * 
 */
public abstract class BaseReader implements IPDFreader {

	/** Element name for the MCU type */
	private final static String	PARTNAME	= "PART_NAME";

	/** The MCU id value as read from the part description file */
	protected String			fMCUid;

	/*
	 * (non-Javadoc)
	 * 
	 * @see it.baeyens.avreclipse.core.toolinfo.partdescriptionfiles.IPDFreader#read(org.w3c.dom.Document)
	 */
	public void read(Document document) {

		// Get the MCU name from the <PART_NAME> element
		NodeList nodes = document.getElementsByTagName(PARTNAME);
		String partname = nodes.item(0).getFirstChild().getNodeValue();
		fMCUid = AVRMCUidConverter.name2id(partname);

		if (partname.endsWith("comp")) {
			// ignore entries ending with "comp".
			// These are the descriptions of a different MCU running in a compatibility mode and
			// acting as this MCU. This might be useful for Debugging, but for now we ignore these
			// files.
			return;
		}

		parse(document);
	}

	/**
	 * Parse the given <code>Document</code>.
	 * <p>
	 * The MCU id has already been parsed and is stored in the field {@link #fMCUid}.
	 * </p>
	 * 
	 * @param document
	 *            XML DOM with the content of a single Atmel part description file.
	 */
	abstract protected void parse(Document document);

}
