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
 * $Id: Signatures.java 851 2010-08-07 19:37:00Z innot $
 *     
 *******************************************************************************/
package it.baeyens.avreclipse.core.toolinfo;

import it.baeyens.arduino.common.ArduinoConst;
import it.baeyens.avreclipse.AVRPlugin;
import it.baeyens.avreclipse.core.IMCUProvider;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.osgi.framework.Bundle;


/**
 * This class handles the list of known MCU signatures.
 * <p>
 * Each AVR MCU is identified by a 3-byte signature. This class handles the mappings between the MCU
 * id and its signature.
 * </p>
 * <p>
 * <ul>
 * <li>To get the Signature for a known MCU Id use {@link #getSignature(String)}</li>
 * <li>To get the MCU ID for a known Signature use {@link #getMCU(String)}</li>
 * </ul>
 * </p>
 * <p>
 * The signatures are stored as Strings with a format of "0x123456" (C style hex format)
 * </p>
 * <p>
 * This class loads a default list of signatures from the signatures.properties file, which is
 * located in the properties folder of the core plugin. Additional / overriding signatures can be
 * added with the {@link #addSignature(String, String)} method. With a call to
 * {@link #storeSignatures()} these additional signatures are persisted in the instance state area (<code>.metadata/.plugins/it.baeyens.avreclipse.core/signatures.properties</core>)
 * and reloaded at the next start.
 * </p>
 * @author Thomas Holland
 * @since 2.2
 *
 */
public class Signatures implements IMCUProvider {

	// paths to the default and instance properties files
	private final static IPath	DEFAULTPROPSFILE	= new Path("properties/signature.properties");
	private final static IPath	INSTANCEPROPSFILE	= new Path("signatures.properties");

	// properties are stored as key=mcuid, value=signature
	private Properties			fProps				= new Properties();

	private static Signatures	fInstance			= null;

	/**
	 * Get the default instance of the Signatures class
	 */
	public static Signatures getDefault() {
		if (fInstance == null)
			fInstance = new Signatures();
		return fInstance;
	}

	// private constructor to prevent instantiation
	private Signatures() {
		// The constructor will first read the default signatures from
		// the plugin signature.properties file.
		// Then it tries to load an existing instance signature property file.

		// Load the list of signatures from the signature.properties file
		// as the default values.
		Properties mcuDefaultProps = new Properties();
		Bundle avrplugin = AVRPlugin.getDefault().getBundle();
		InputStream is = null;
		try {
			is = FileLocator.openStream(avrplugin, DEFAULTPROPSFILE, false);
			mcuDefaultProps.load(is);
			is.close();
		} catch (IOException e) {
			// this should not happen because the signatures.properties is
			// part of the plugin and always there.
			AVRPlugin.getDefault().log(
					new Status(Status.ERROR, ArduinoConst.CORE_PLUGIN_ID,
							"Can't find signatures.properties", e));
			return;
		}

		// Load any instance signatures from the plugin state location
		fProps = new Properties(mcuDefaultProps);
		File propsfile = getInstanceSignatureProperties();
		if (propsfile.canRead()) {
			try {
				is = new FileInputStream(propsfile);
				fProps.load(is);
				is.close();
			} catch (IOException e) {
				AVRPlugin.getDefault().log(
						new Status(Status.ERROR,ArduinoConst.CORE_PLUGIN_ID,
								"Can't read instance signatures.properties", e));
				// continue anyway without the instance signatures
			}
		}
	}

	/**
	 * Get the Signature for the given MCU id.
	 * 
	 * @param mcuid
	 *            String with a MCU id
	 * @return String with the MCU signature in hex ("0x123456") or <code>null</code> if the given
	 *         MCU id is unknown.
	 */
	public String getSignature(String mcuid) {
		return fProps.getProperty(mcuid);
	}

	/**
	 * Get the MCU id for the given Signature.
	 * <p>
	 * If multiple MCUs share the same signature (e.g. ATmega169 and ATmega169p), then the one with the shortest name is returned.
	 * 
	 * @param signature
	 *            String with a signature in hex ("0x123456")
	 * @return String with the corresponding MCU id or * <code>null</code> if the given signature
	 *         is unknown.
	 */
	public String getMCU(String signature) {
		// iterate over all mcuids to find the one with the given signature
		// I do not use a reverse lookup map because this method will not be
		// called often and a reverse map would add code complexity.
		
		// However there is a problem
		Enumeration<?> keyset = fProps.propertyNames();
		List<String> allMCUs = new ArrayList<String>();
		while (keyset.hasMoreElements()) {
			Object mcukey = keyset.nextElement();
			if (mcukey != null && mcukey instanceof String) {
				String mcuid = (String) mcukey;
				if (fProps.getProperty(mcuid).equalsIgnoreCase(signature)) {
					allMCUs.add(mcuid);
				}
			}
		}
		if (allMCUs.size() == 0) {
			// No matching MCU found
			return null;
		}
		if (allMCUs.size() == 1) {
			// Only one found
			return allMCUs.get(0);
		}
		
		// more than one MCU matched the signature.
		// find the one with the shortest name and return it.
		String bestmcu = null;
		int bestmculength = 99;
		for (String currmcu : allMCUs) {
			if (currmcu.length() < bestmculength) {
				bestmcu = currmcu;
				bestmculength = currmcu.length();
			}
		}
		return bestmcu;
	}

	/**
	 * Add a MCU signature to the list.
	 * <p>
	 * The signature is only added if it differs from the default signature.
	 * </p>
	 * 
	 * @param mcuid
	 *            String with a MCU id
	 * @param signature
	 *            String with the signature in format "0x123456"
	 */
	public void addSignature(String mcuid, String signature) {
		Assert.isNotNull(mcuid);
		Assert.isNotNull(signature);

		String oldsig = fProps.getProperty(mcuid);
		if (!signature.equals(oldsig)) {
			fProps.setProperty(mcuid, signature);
		}
	}

	/**
	 * Stores the signature properties in the Eclipse instance storage area.
	 * <p>
	 * The generated properties file only contains additional signatures not in the default list.
	 * </p>
	 * <p>
	 * 
	 * @throws IOException
	 *             for any error writing the properties file
	 */
	public void storeSignatures() throws IOException {
		File propsfile = getInstanceSignatureProperties();
		FileOutputStream os = null;
		try {
			os = new FileOutputStream(propsfile);
			fProps.store(os, "Additional MCU Signature values");
		} finally {
			// close the stream if the fProps.store() method failed
			if (os != null) {
				os.close();
			}
		}
	}

	//
	// Methods of the IMCUProvider Interface
	//

	/*
	 * (non-Javadoc)
	 * 
	 * @see it.baeyens.avreclipse.core.IMCUProvider#getMCUInfo(java.lang.String)
	 */
	public String getMCUInfo(String mcuid) {
		return fProps.getProperty(mcuid);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see it.baeyens.avreclipse.core.IMCUProvider#getMCUList()
	 */
	public Set<String> getMCUList() {
		// Return all keys of the underlying properties (the mcuids)
		// as a List
		// I used "fProps.stringPropertyNames() first, but that is a JDK 1.6 method and we still
		// want to run on 1.5
		Enumeration<?> keyset = fProps.propertyNames();
		Set<String> mcuset = new HashSet<String>();
		while (keyset.hasMoreElements()) {
			Object name = keyset.nextElement();
			if (name != null && name instanceof String) {
				mcuset.add((String) name);
			}
		}

		return mcuset;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see it.baeyens.avreclipse.core.IMCUProvider#hasMCU(java.lang.String)
	 */
	public boolean hasMCU(String mcuid) {
		String sig = fProps.getProperty(mcuid);
		return sig != null ? true : false;
	}

	/**
	 * @return File pointing to the instance signature properties file
	 */
	private File getInstanceSignatureProperties() {
		IPath propslocation = AVRPlugin.getDefault().getStateLocation().append(INSTANCEPROPSFILE);
		return propslocation.toFile();

	}
}
