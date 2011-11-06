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
 * $Id: BitFieldValueDescription.java 851 2010-08-07 19:37:00Z innot $
 *     
 *******************************************************************************/
package it.baeyens.avreclipse.core.toolinfo.fuses;

import it.baeyens.avreclipse.core.toolinfo.partdescriptionfiles.FusesReader;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;


/**
 * Bitfield value enumeration element.
 * 
 * <p>
 * The <em>part description file</em> may have a enumeration of possible values for a bitfield.
 * This class represents a single enumeration value together with its description.
 * </p>
 * Objects of this class can be serialized to XML with the {@link #toXML(Node)} method. The
 * generated format is:
 * 
 * <pre>
 * &lt;value val=&quot;0x01&quot; desc=&quot;some text&quot;&gt;
 * </pre>
 * 
 * </p>
 * <p>
 * This class has two constructors. One to pass the value and description directly and another one
 * to read the value and description from an XML document node. The first one is used by the
 * {@link FusesReader} while parsing the <em>part description file</em> while the second one is
 * used when a {@link BitFieldDescription} is constructed from an XML file.<br>
 * </p>
 * 
 * @author Thomas Holland
 * @since 2.2
 * 
 */
public class BitFieldValueDescription {

	/** XML element tag name name for a <code>BitFieldValueDescription</code> object. */
	public final static String	TAG_VALUE			= "value";

	/** XML attribute name for the value */
	public final static String	ATTR_VALUE			= "val";

	/** XML attribute name for the description */
	public final static String	ATTR_DESCRIPTION	= "desc";

	/** The description of this bitfield enumeration value. */
	private String				fDescription		= "???";

	/** The value of this bitfield enumeration value */
	private int					fValue				= 0x00;

	/**
	 * Construct a new BitFieldValueDescription from the given value and description.
	 * 
	 * @param value
	 * @param description
	 */
	public BitFieldValueDescription(int value, String description) {
		fDescription = description;
		fValue = value;
	}

	/**
	 * Construct a new BitFieldValueDescription from a XML document node.
	 * <p>
	 * This constructor will take the node and take the value from the "val" attribute and the
	 * description from an "desc" attribute.<br>
	 * If either or both are missing the defaults <code>0x00</code> (for the value) and
	 * <code>"???"</code> (for the description) will be used.
	 * </p>
	 * 
	 * @param bitfieldvaluenode
	 *            A &lt;value&gt; document node.
	 */
	protected BitFieldValueDescription(Node bitfieldvaluenode) {

		NamedNodeMap attrs = bitfieldvaluenode.getAttributes();

		// Iterate over all attributes, parsing the known attrs and ignore all others.
		for (int i = 0; i < attrs.getLength(); i++) {
			Node attr = attrs.item(i);
			if (ATTR_VALUE.equalsIgnoreCase(attr.getNodeName())) {
				fValue = Integer.decode(attr.getTextContent());
			}
			if (ATTR_DESCRIPTION.equalsIgnoreCase(attr.getNodeName())) {
				fDescription = attr.getTextContent();
			}
		}
	}

	/**
	 * @return the description of this bitfield value enumeration element
	 */
	public String getDescription() {
		return fDescription;
	}

	/**
	 * @return the value of this bitfield value enumeration element
	 */
	public int getValue() {
		return fValue;
	}

	/**
	 * Convert to XML.
	 * <p>
	 * Create a new child element node in the given parent node with the name &lt;value&gt; and two
	 * attributes for the value and the description of this <code>BitFieldValueDescription</code>.
	 * </p>
	 * 
	 * @param parentnode
	 *            &lt;bitfield&gt; document node.
	 */
	protected void toXML(Node parentnode) {
		Document document = parentnode.getOwnerDocument();
		Element element = document.createElement(TAG_VALUE);
		element.setAttribute(ATTR_VALUE, ByteDescription.toHex(fValue));
		element.setAttribute(ATTR_DESCRIPTION, fDescription);

		parentnode.appendChild(element);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return ByteDescription.toHex(fValue) + ": " + fDescription;
	}

}
