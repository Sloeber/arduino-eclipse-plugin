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
 * $Id: PartDescriptionFilesReader.java 851 2010-08-07 19:37:00Z innot $
 *     
 *******************************************************************************/
package it.baeyens.avreclipse.core.toolinfo;

import it.baeyens.avreclipse.core.paths.AVRPath;
import it.baeyens.avreclipse.core.paths.AVRPathProvider;
import it.baeyens.avreclipse.core.paths.IPathProvider;
import it.baeyens.avreclipse.core.toolinfo.partdescriptionfiles.IPDFreader;
import it.baeyens.avreclipse.core.toolinfo.partdescriptionfiles.SignatureReader;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;


/**
 * This is an utility class to read and parse the Atmel Part Description files.
 * 
 * @author Thomas Holland
 * @since 2.2
 * 
 */
public class PartDescriptionFilesReader {

	private static PartDescriptionFilesReader fInstance = null;

	private List<IPDFreader> fReaders;

	public static PartDescriptionFilesReader getDefault() {
		if (fInstance == null) {
			fInstance = new PartDescriptionFilesReader();
		}
		return fInstance;
	}

	private PartDescriptionFilesReader() {
		fReaders = new ArrayList<IPDFreader>();
		fReaders.add(new SignatureReader());
	}

	public PartDescriptionFilesReader(List<IPDFreader> readers) {
		fReaders = readers;
	}

	public void parseAllFiles(IProgressMonitor monitor) {

		// Get the path to the PartDescriptionFiles
		IPathProvider provider = new AVRPathProvider(AVRPath.PDFPATH);
		IPath pdfpath = provider.getPath();
		File pdfdirectory = pdfpath.toFile();
		if (!pdfdirectory.isDirectory()) {
			return;
		}

		// get and parse all XML files in the PartDescriptionFiles directory
		File[] allfiles = pdfdirectory.listFiles(new FilenameFilter() {
			public boolean accept(File dir, String name) {
				if (name.endsWith(".xml")) {
					return true;
				}
				return false;
			}
		});

		try {
			monitor.beginTask("Parsing Atmel Part Description Files", allfiles.length);

			// Tell all registered readers that we are about to start reading
			// files.
			for (IPDFreader reader : fReaders) {
				reader.start();
			}

			// Go through all files and call the read() method for each file and
			// every registered reader
			for (File pdffile : allfiles) {
				monitor.subTask("Reading [" + pdffile.getName() + "]");
				Document root = getDocument(pdffile);

				// Pass the document to all readers
				for (IPDFreader reader : fReaders) {
					reader.read(root);
				}
				monitor.worked(1);
			}
		} finally {
			monitor.done();
		}

		// Tell all registered readers that all files have been processed and
		// that they can close up shop.
		for (IPDFreader reader : fReaders) {
			reader.finish();
		}

	}

	/**
	 * Read and parse the given XML file and return an DOM for it.
	 * 
	 * @param pdffile
	 *            <code>File</code> to an XML file.
	 * @return <code>Document</code> root node of the DOM or <code>null</code>
	 *         if the file could not be read or parsed.
	 */
	private Document getDocument(File pdffile) {

		Document root = null;
		try {
			// Read the xml file
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			DocumentBuilder builder = factory.newDocumentBuilder();
			root = builder.parse(pdffile);

		} catch (SAXParseException spe) {
			System.out.println("\n** Parsing error, line " + spe.getLineNumber() + ", uri "
			        + spe.getSystemId());
			System.out.println("   " + spe.getMessage());
			Exception e = (spe.getException() != null) ? spe.getException() : spe;
			e.printStackTrace();
		} catch (SAXException sxe) {
			Exception e = (sxe.getException() != null) ? sxe.getException() : sxe;
			e.printStackTrace();
		} catch (ParserConfigurationException pce) {
			pce.printStackTrace();
		} catch (IOException ioe) {
			ioe.printStackTrace();
		}

		return root;
	}

}
