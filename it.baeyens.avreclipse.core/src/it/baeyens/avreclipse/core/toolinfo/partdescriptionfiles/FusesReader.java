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
 * $Id: FusesReader.java 851 2010-08-07 19:37:00Z innot $
 *     
 *******************************************************************************/
package it.baeyens.avreclipse.core.toolinfo.partdescriptionfiles;

import it.baeyens.arduino.common.ArduinoConst;
import it.baeyens.avreclipse.AVRPlugin;
import it.baeyens.avreclipse.core.toolinfo.fuses.BitFieldDescription;
import it.baeyens.avreclipse.core.toolinfo.fuses.BitFieldValueDescription;
import it.baeyens.avreclipse.core.toolinfo.fuses.ByteDescription;
import it.baeyens.avreclipse.core.toolinfo.fuses.FuseType;
import it.baeyens.avreclipse.core.toolinfo.fuses.Fuses;
import it.baeyens.avreclipse.core.toolinfo.fuses.IMCUDescription;
import it.baeyens.avreclipse.core.toolinfo.fuses.MCUDescription;
import it.baeyens.avreclipse.core.util.AVRMCUidConverter;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
//import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.w3c.dom.Comment;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;



/**
 * Fuses info reader.
 * <p>
 * This Class will take a PartDescriptionFile Document and read either the Fuse Byte or the Lockbit
 * settings from it. What to read is determined by the subclass.
 * </p>
 * <p>
 * The definitions of Fuses and Lockbits are very similar, therefore this class does most of the
 * parsing and the subclasses just need to supply a few informations.
 * </p>
 * <p>
 * 
 * </p>
 * 
 * @author Thomas Holland
 * @since 2.2
 */
public class FusesReader extends BaseReader {

	private final static String			FILE_POSTFIX	= ".desc";

	/** List of all Fuses Descriptions */
	private Map<String, MCUDescription>	fFuseDescriptions;

	/*
	 * (non-Javadoc)
	 * 
	 * @see it.baeyens.avreclipse.core.toolinfo.partdescriptionfiles.BaseReader#start()
	 */
	public void start() {
		fFuseDescriptions = new HashMap<String, MCUDescription>();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see it.baeyens.avreclipse.core.toolinfo.partdescriptionfiles.BaseReader#parse(org.w3c.dom.Document)
	 */
	@Override
	public void parse(Document document) {

		// initialize the description object we will fill with data
		MCUDescription desc = new MCUDescription(fMCUid);

		// Get the default Fuse byte values (if present)
		Map<String, Integer> defaults = getDefaultValues(document);

		// Find the <registers memspace="FUSE|LOCKBIT"> nodes.
		// Get all <registers> nodes and look for the one which has the
		// right memspace attribute.
		NodeList allregistersnodes = document.getElementsByTagName("registers");

		for (int i = 0; i < allregistersnodes.getLength(); i++) {
			Node node = allregistersnodes.item(i);
			if (node.hasAttributes()) {
				NamedNodeMap attributes = allregistersnodes.item(i).getAttributes();
				Node memspaceattr = attributes.getNamedItem("memspace");
				if (memspaceattr != null) {
					if ("FUSE".equalsIgnoreCase(memspaceattr.getTextContent())
							|| "LOCKBIT".equalsIgnoreCase(memspaceattr.getTextContent())) {
						readRegistersNode(node, desc, defaults);
					}
				}
			}
		}

		// and the build version and the status
		setVersionAndStatus(document, desc);

		// Add the description object to the internal list of all
		// descriptions.
		fFuseDescriptions.put(fMCUid, desc);

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see it.baeyens.avreclipse.core.toolinfo.partdescriptionfiles.IPDFreader#finish()
	 */
	public void finish() {

		// The FuseDescription Objects are serialized to the plugin storage
		// area.

		// Serialization was chosen instead of text properties, because the
		// FuseDescription is a more complex object which would have required
		// some code to write to and read from a properties file. Object
		// serialization is much easier to code and, because the files are
		// generated and should not require manual modifications, this binary
		// storage format is suitable.
		// However, changes to the FuseDescription Class or its subclasses
		// should be made carefully not to break compatibility.

		// get the location where the descriptions will be written to.
		IPath location = getStoragePath();

		// Create the fusedesc folder if necessary
		File folder = location.toFile();
		if (!folder.isDirectory()) {
			// Create the folder
			if (!folder.mkdirs()) {
				IStatus status = new Status(Status.ERROR, ArduinoConst.CORE_PLUGIN_ID,
						"Can't create fusedesc folder [" + folder.toString() + "]", null);
				AVRPlugin.getDefault().log(status);
				// TODO Throw an Exception
				return;
			}
		}

		// Now serialize all FuseDescription Objects to the storage area
		Set<String> allmcus = fFuseDescriptions.keySet();

		for (String mcuid : allmcus) {
			// Generate a filename: "mcuid.desc"
			File file = location.append(mcuid + FILE_POSTFIX).toFile();
			FileOutputStream fos = null;

			MCUDescription fusesdesc = fFuseDescriptions.get(mcuid);

			// Create a blank XML DOM document...
			try {

				DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
				DocumentBuilder parser = factory.newDocumentBuilder();
				Document document = parser.newDocument();

				// Add a few comments to the document
				Comment comment = document
						.createComment("Fuse/Lockbit description file for the AVR Eclipse plugin");
				document.appendChild(comment);

				comment = document
						.createComment("Author: automatically created by AVR Eclipse plugin");
				document.appendChild(comment);

				comment = document.createComment("Date: "
						+ new SimpleDateFormat().format(new Date()));
				document.appendChild(comment);

				comment = document.createComment("Based on: Atmel Part Description File \""
						+ AVRMCUidConverter.id2name(mcuid) + ".xml\"");
				document.appendChild(comment);

				comment = document
						.createComment("SVN: $Id: FusesReader.java 851 2010-08-07 19:37:00Z innot $");
				document.appendChild(comment);

				// And now add the Actual Description to the document
				fusesdesc.toXML(document);

				// Use a Transformer for output
				TransformerFactory tFactory = TransformerFactory.newInstance();
				Transformer transformer = tFactory.newTransformer();
				transformer.setOutputProperty(OutputKeys.INDENT, "yes");

				DOMSource source = new DOMSource(document);

				fos = new FileOutputStream(file);

				StreamResult result = new StreamResult(fos);
				transformer.transform(source, result);

				fos.close();

//				if (false) {
//					// Short fragment I used for debugging.
//					// This dumps all BitFieldDescriptions into a large CSV file,
//					// which can then be imported and analyzed with a Database.
//					FileWriter fw = new FileWriter(new File("allfuses.csv"), true);
//					for (FuseType type : FuseType.values()) {
//						List<IByteDescription> list = fusesdesc.getByteDescriptions(type);
//						for (IByteDescription desc : list) {
//							List<BitFieldDescription> bfdlist = desc.getBitFieldDescriptions();
//							for (BitFieldDescription bfd : bfdlist) {
//								fw.write(mcuid + "; " + type.name() + "; " + bfd.getName() + "; \""
//										+ bfd.getDescription() + "\"; " + bfd.getMaxValue() + "\n");
//							}
//						}
//					}
//					fw.close();
//				}

			} catch (ParserConfigurationException pce) {
				IStatus status = new Status(Status.ERROR, ArduinoConst.CORE_PLUGIN_ID,
						"Could not create a XML object for " + mcuid, pce);
				AVRPlugin.getDefault().log(status);
				// TODO throw an Exception to notify the caller
				// For now we just continue and try the next object.
			} catch (TransformerException te) {
				IStatus status = new Status(Status.ERROR, ArduinoConst.CORE_PLUGIN_ID,
						"Could not transform the XML content for " + mcuid, te);
				AVRPlugin.getDefault().log(status);
				// TODO throw an Exception to notify the caller
				// For now we just continue and try the next object.
			} catch (FileNotFoundException fnfe) {
				IStatus status = new Status(Status.ERROR, ArduinoConst.CORE_PLUGIN_ID,
						"Could not create the MCUDescription file for " + mcuid, fnfe);
				AVRPlugin.getDefault().log(status);
				// TODO throw an Exception to notify the caller
				// For now we just continue and try the next object.
			} catch (IOException ioe) {
				IStatus status = new Status(Status.ERROR, ArduinoConst.CORE_PLUGIN_ID,
						"Could not write the MCUDescription file for " + mcuid, ioe);
				AVRPlugin.getDefault().log(status);
				// TODO throw an Exception to notify the caller
				// For now we just continue and try the next object.
			}
		} // for loop

	}

	/**
	 * Get the storage destination folder for the fuse description files.
	 * <p>
	 * Override this method to supply a different location.
	 * </p>
	 * 
	 * @see Fuses#getInstanceStorageLocation()
	 * 
	 * @return <code>IPath</code> to the instance storage area.
	 */
	protected IPath getStoragePath() {
		// The default is to get the folder from the {@link Fuses} class.
		return Fuses.getDefault().getInstanceStorageLocation();
	}

	/**
	 * @param registersnode
	 *            A &lt;registers memspace="FUSE|LOCKBIT"&gt; node.
	 * @param desc
	 *            A <code>MCUDescription</code> container which will be filled with the
	 *            <code>ByteDescription</code> objects for each byte.
	 * @param A
	 *            map with the names of all fuse bytes and their default value.
	 */
	private void readRegistersNode(Node registersnode, MCUDescription desc,
			Map<String, Integer> defaults) {

		// First get the type of the node
		NamedNodeMap attrs = registersnode.getAttributes();
		Node memspacenode = attrs.getNamedItem("memspace");
		String memspacevalue = memspacenode.getTextContent();
		FuseType type = FuseType.getTypeFromPDFmemspace(memspacevalue);

		// Then read all enumerators. The Enumerators are siblings of the <registers> node.
		Map<String, List<BitFieldValueDescription>> enumerators = new HashMap<String, List<BitFieldValueDescription>>();
		Node child = registersnode.getParentNode().getFirstChild();
		while (child != null) {
			if ("enumerator".equals(child.getNodeName())) {
				readEnumeratorNode(child, enumerators);
			}
			child = child.getNextSibling();
		}

		// Now get and parse all <reg> nodes. Each <reg> node stands for one byte.
		Node regnode = registersnode.getFirstChild();

		while (regnode != null) {
			if ("reg".equals(regnode.getNodeName())) {

				// read the offset attribute and store it
				// This is used to get the correct order of the bytes (they
				// are in reverse order within the document).
				// We also read the name of the byte. This is later used to
				// map to the correct name for avrdude.
				Node offsetattr = regnode.getAttributes().getNamedItem("offset");
				Integer offset = Integer.decode(offsetattr.getTextContent());

				Node nameattr = regnode.getAttributes().getNamedItem("name");
				String name = (nameattr != null) ? nameattr.getTextContent() : "";

				int defaultvalue = defaults.containsKey(name) ? defaults.get(name) : -1;

				ByteDescription bytedesc = new ByteDescription(type, name, offset, defaultvalue);

				// Now we can read the bitfields for each node
				Node bitfieldnode = regnode.getFirstChild();
				while (bitfieldnode != null) {
					if ("bitfield".equals(bitfieldnode.getNodeName())) {
						BitFieldDescription bitfield = readBitfieldNode(bitfieldnode, enumerators,
								offset, defaultvalue);
						bytedesc.addBitFieldDescription(bitfield);
					}
					bitfieldnode = bitfieldnode.getNextSibling();
				}

				desc.addByteDescription(type, bytedesc);
			}
			regnode = regnode.getNextSibling();
		}

	}

	/**
	 * Read the given &lt;enumerator&gt; node and add its content to the given global enumerators
	 * list.
	 * <p>
	 * All &lt;enum&gt; children of the &lt;enumerator&gt; node are collected in a list of
	 * BitFieldValueDescription objects and mapped to the name of the enumerator node, which is then
	 * added to the global list.
	 * </p>
	 * 
	 * @param node
	 *            A &lt;enumerator&gt; node
	 * @param enums
	 *            The global map of all enumerator names and their
	 *            <code>BitFieldValueDescription</code> lists
	 */
	private void readEnumeratorNode(Node node, Map<String, List<BitFieldValueDescription>> enums) {

		List<BitFieldValueDescription> values = new ArrayList<BitFieldValueDescription>();

		// Get the name of the <enumerator>
		NamedNodeMap attrs = node.getAttributes();
		Node nameattr = attrs.getNamedItem("name");
		String enumname = nameattr.getTextContent();

		// Get all <enum> children
		NodeList children = node.getChildNodes();

		for (int i = 0; i < children.getLength(); i++) {
			Node child = children.item(i);
			if ("enum".equalsIgnoreCase(child.getNodeName())) {
				BitFieldValueDescription value = readEnumNode(child);
				values.add(value);
			}
		}

		enums.put(enumname, values);
	}

	/**
	 * Read the given &lt;enum&gt; node and return a new BitFieldValueDescription with the
	 * descriptive text and the value of the enum node.
	 * 
	 * @param node
	 *            A &lt;enum&gt; node
	 * @return New <code>BitFieldValueDescription</code> object with the values from the "text"
	 *         and "val" attributes.
	 */
	private BitFieldValueDescription readEnumNode(Node node) {

		NamedNodeMap attrs = node.getAttributes();

		String description = attrs.getNamedItem("text").getNodeValue();
		int value = Integer.decode(attrs.getNamedItem("val").getNodeValue());

		return new BitFieldValueDescription(value, description);
	}

	/**
	 * @param node
	 *            A &lt;bitfield&gt; node
	 * @param enumerators
	 *            The global list of enumerators for this node.
	 * @param index
	 *            the fuse byte index this bitfield is for.
	 * @return A <code>BitFieldDescription</code> object representing the given &lt;bitfield&gt;
	 *         node.
	 */
	private BitFieldDescription readBitfieldNode(Node node,
			Map<String, List<BitFieldValueDescription>> enumerators, int index, int defaultvalue) {

		// Get the Attributes of the <bitfield> node
		NamedNodeMap attrs = node.getAttributes();
		String name = attrs.getNamedItem("name").getTextContent();
		String description = attrs.getNamedItem("text").getTextContent();
		int mask = Integer.decode(attrs.getNamedItem("mask").getTextContent());

		List<BitFieldValueDescription> values = null;

		Node enumattrnode = attrs.getNamedItem("enum");
		if (enumattrnode != null) {
			String enumname = enumattrnode.getTextContent();
			values = enumerators.get(enumname);
			if (values == null) {
				System.out.println("Found non-existing enum value: " + enumname);
			}
		}

		// Check if this bitfield is one that contains errors in the part description files and fix
		// the errors.
		name = bitfieldNameFixer(name, mask, description);

		BitFieldDescription bitfield = new BitFieldDescription(index, name, description, mask,
				defaultvalue, values);
		return bitfield;
	}

	/**
	 * Extract the build version and release status from the part description file document and call
	 * the {@link #setVersionAndStatus(IMCUDescription, int, String)} method of the subclass to set
	 * the values.
	 * 
	 * @param document
	 *            Part description file DOM
	 * @param desc
	 *            The description holder created by the {@link #getDescriptionHolder(String, int)}
	 *            call.
	 */
	private void setVersionAndStatus(Document document, MCUDescription desc) {

		int version = 0;
		String status = "UNKNOWN";

		// Find the <BUILD> element and extract its value
		NodeList nodeslist = document.getElementsByTagName("BUILD");

		if (nodeslist.getLength() > 0) {
			Node buildnode = nodeslist.item(0);
			String value = buildnode.getFirstChild().getNodeValue();
			version = Integer.decode(value);
		}

		// Find the <RELEASE_STATUS> element and extract its value
		nodeslist = document.getElementsByTagName("RELEASE_STATUS");

		if (nodeslist.getLength() > 0) {
			Node buildnode = nodeslist.item(0);
			status = buildnode.getFirstChild().getNodeValue();
		}

		desc.setVersionAndStatus(version, status);
	}

	/**
	 * Read the Document and extract the default values for all fuse bytes.
	 * 
	 * @param document
	 */
	private Map<String, Integer> getDefaultValues(Document document) {

		Map<String, Integer> defaultsmap = new HashMap<String, Integer>(6);

		// Get the <FUSE> nodes (from the old, pre-V2, part of the PDF)
		NodeList allfusenodes = document.getElementsByTagName("FUSE");
		for (int i = 0; i < allfusenodes.getLength(); i++) {
			Node fusenode = allfusenodes.item(i);

			String[] fusenames = new String[] {};

			// The <LIST> child node has the name of all Fuse Bytes defined
			Node childnode = fusenode.getFirstChild();
			while (childnode != null) {
				if ("LIST".equalsIgnoreCase(childnode.getNodeName())) {
					// Get the value of the <LIST> node, remove the Brackets and
					// split it into the separate fusenames.
					Node valuenode = childnode.getFirstChild();
					String allnames = valuenode.getNodeValue();
					allnames = allnames.substring(1, allnames.length() - 1);
					fusenames = allnames.split(":");
					break;
				}
				childnode = childnode.getNextSibling();
			}

			// Now we have the names of the fuse bytes
			// Iterate once again through all child nodes, this time look for a node
			// which matches one of the fusenames. Once found pass this node to
			// readFuseByteNode() to read all fuse bits and their default value.

			childnode = fusenode.getFirstChild();
			while (childnode != null) {
				for (String fuse : fusenames) {
					if (fuse.equalsIgnoreCase(childnode.getNodeName())) {
						// Found the fuse node in the childnodes
						int defaultvalue = readFuseByteNode(childnode);
						String name = childnode.getNodeName();

						defaultsmap.put(name, Integer.valueOf(defaultvalue));
					}
				}
				childnode = childnode.getNextSibling();
			}
		}
		return defaultsmap;
	}

	/**
	 * With the given fuse byte element (e.g. &lt;LOW&gt;, &lt;HIGH&gt; or &lt;FUSEYTE0&gt;) extract
	 * the default values.
	 * <p>
	 * For all bits of the byte which do not have a &lt;DEFAULT&gt; element, <code>1</code> is
	 * used as a default value (= unset fusebit), unless no bit has a &lt;DEFAULT&gt; element, in
	 * which case -1 (= no default available) is returned.
	 * </p>
	 * 
	 * @param fusenode
	 *            The fuse byte element node.
	 * @return An <code>int</code> with a byte value (0x00-0xff), or <code>-1</code> if no
	 *         defaults are available.
	 */
	private int readFuseByteNode(Node fusenode) {

		int value = 0xFF;

		// This is set to true if at least one <DEFAULT> Element was found
		boolean hasDefault = false;

		// We could read the <NMB_FUSE_BITS> node to get the number of fuse
		// bits. However we don't bother and read just interpret all <FUSEx>
		// child nodes that are there.
		Node bitnode = fusenode.getFirstChild();
		while (bitnode != null) {
			String nodename = bitnode.getNodeName();
			if (nodename.startsWith("FUSE")) {
				String bitnumberstring = nodename.substring(4);
				int bitnumber = Integer.parseInt(bitnumberstring);
				int bitvalue = readFuseBitNode(bitnode);
				if (bitvalue != -1) {
					hasDefault = true;
				}
				// Clear the bit if <DEFAULT>0</DEFAULT>
				if (bitvalue == 0)
					value &= ~(1 << bitnumber);
				// we don't need to set bits because they are set by default.
			}

			bitnode = bitnode.getNextSibling();
		}
		if (hasDefault) {
			return value;
		}
		return -1;
	}

	/**
	 * With the given &lt;FUSEx&gt; element node, return the value of the &lt;DEFAULT&gt; child
	 * element.
	 * 
	 * @param bitnode
	 *            A &lt;FUSEx&gt; element node.
	 * @return The value of the &lt;DEFAULT&gt; element, or <code>-1</code> if no &lt;DEFAULT&gt;
	 *         element exists.
	 */
	private int readFuseBitNode(Node bitnode) {

		Node childnode = bitnode.getFirstChild();
		while (childnode != null) {

			String nodename = childnode.getNodeName();
			if ("DEFAULT".equalsIgnoreCase(nodename)) {
				// This is the right node. Get its value
				// The value is in the first child (the TEXT node)
				String value = childnode.getFirstChild().getNodeValue();
				return Integer.parseInt(value, 2);
			}
			childnode = childnode.getNextSibling();
		}

		// Did not find any <DEFAULT> Node.
		// Return -1, which means "not available"
		return -1;
	}

	/**
	 * With the given parameters check if they match a few obvious errors in the part description
	 * files and fix them.
	 * <p>
	 * As long as the description files generated by the plugin are only used by the plugin this
	 * should be save. If we ever need to interoperate with some external tools this modification of
	 * the fuses information needs to be checked to avoid compatibility issues.
	 * </p>
	 * 
	 * @param name
	 * @param mask
	 * @param description
	 * @return The fixed name, or the original name if no fix was required.
	 */
	private String bitfieldNameFixer(String name, int mask, String description) {
		if (name.equals("CKSEL") && mask == 0x3f) {
			// This is a bug in the atmega32.xml file
			// The bitfield is named "CKSEL", even though it is a combined
			// SUT and CKSEL field (like most other MCUs)
			return "SUT_CKSEL";
		}
		if (name.equals("WTDON") && description.startsWith("Watch-Dog Timer")) {
			// This is a typo in the atmega8.xml file
			// Should be "WDTON", like in all other MCUs.
			return "WTDON";
		}

		// The atmega103.xml file is really fucked up. All Fuses names are wrong in the part
		// description file! We can fix two of them, but because at this point we do not know the
		// MCU Id, we cannot fix the last bug: The "CKSEL" field is in reality a "SUT" as
		// documented in the datasheet.
		if (name.equals("CKSEL3") && description.startsWith("Preserve EEPROM")) {
			// According to the description this bitfield should be "EESAVE"
			return "EESAVE";
		}
		if (name.equals("SUT1") && description.startsWith("Serial program")) {
			// According to the description this bitfield should be "SPIEN"
			return "SPIEN";
		}

		return name;
	}
}
