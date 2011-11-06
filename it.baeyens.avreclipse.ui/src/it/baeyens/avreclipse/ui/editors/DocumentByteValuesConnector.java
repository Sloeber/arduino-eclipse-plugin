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
 * $Id: DocumentByteValuesConnector.java 851 2010-08-07 19:37:00Z innot $
 *     
 *******************************************************************************/
package it.baeyens.avreclipse.ui.editors;

import it.baeyens.arduino.globals.ArduinoConst;
import it.baeyens.avreclipse.AVRPlugin;
import it.baeyens.avreclipse.core.toolinfo.fuses.BitFieldDescription;
import it.baeyens.avreclipse.core.toolinfo.fuses.ByteValueChangeEvent;
import it.baeyens.avreclipse.core.toolinfo.fuses.ByteValues;
import it.baeyens.avreclipse.core.toolinfo.fuses.FuseType;
import it.baeyens.avreclipse.core.toolinfo.fuses.IByteValuesChangeListener;

import java.text.MessageFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentListener;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.Position;
import org.eclipse.ui.texteditor.IDocumentProvider;
import org.eclipse.ui.texteditor.IElementStateListener;


/**
 * Connect a Fuses file document to a ByteValues object.
 * 
 * @author Thomas Holland
 * @since 2.3
 * 
 */
public class DocumentByteValuesConnector {

	/** Reference to the parent DocumentProvider to add and remove Element State Change Listener. */
	private final IDocumentProvider			fProvider;

	/** Source of the Document. Used to set / clear problem markers. */
	private IFile							fSource;

	/** The source document */
	private final IDocument					fDocument;

	/** The ByteValues created from and synchronized with the source IDocument. */
	private ByteValues						fByteValues				= null;

	/**
	 * <code>true</code> while the source IDocument is modified from this class, so the document
	 * change listener can ignore the resulting change events.
	 */
	private boolean							fInDocumentChange		= false;

	/**
	 * <code>true</code> while the ByteValues are modified from this class, so the ByteValues change
	 * listener can ignore the resulting change events.
	 */
	private boolean							fInByteValuesChange		= false;

	private final IDocumentListener			fDocumentListener;
	private final IByteValuesChangeListener	fByteValuesListener;
	private final IElementStateListener		fElementStateListener;

	/** The current value of a key. */
	private final Map<String, String>		fKeyValueMap			= new HashMap<String, String>();

	/** The current linenumber of a key. */
	private final Map<String, Integer>		fKeyLineMap				= new HashMap<String, Integer>();

	/** The <code>Position</code> of a key in the document. */
	private final Map<String, Position>		fKeyPositionMap			= new HashMap<String, Position>();

	/** The <code>Position</code> of a key value in the document. */
	private final Map<String, Position>		fKeyValuePositionMap	= new HashMap<String, Position>();

	/** MCU property key string. */
	private final static String				KEY_MCU					= "MCU";

	/**
	 * Comment property key string. Called 'summary' to be compatible with AVR32 Studio file format.
	 */
	private final static String				KEY_COMMENT				= "summary";

	/** RegEx pattern for comment lines. Comments are all lines starting with "#". */
	private final static Pattern			fCommentPattern			= Pattern.compile("\\s*#.*");

	/** RegEx pattern for property lines. Properties match the "key=value" pattern. */
	private final static Pattern			fPropertyPattern		= Pattern
																			.compile("\\s*(\\w*)\\s*=(.*)");

	// ------------ IDocumentListener -------------------

	/**
	 * Listener to listen for Document change events.
	 * <p>
	 * Whenever the document is changed, e.g. by the TextEditor, the document is parsed and the
	 * associated ByteValues object is updated.
	 * </p>
	 */
	private class MyDocumentListener implements IDocumentListener {

		/*
		 * (non-Javadoc)
		 * @see
		 * org.eclipse.jface.text.IDocumentListener#documentAboutToBeChanged(org.eclipse.jface.text
		 * .DocumentEvent)
		 */
		@Override
		public void documentAboutToBeChanged(DocumentEvent event) {
			// ignore event
		}

		/*
		 * (non-Javadoc)
		 * @see
		 * org.eclipse.jface.text.IDocumentListener#documentChanged(org.eclipse.jface.text.DocumentEvent
		 * )
		 */
		@Override
		public void documentChanged(DocumentEvent event) {
			if (fInDocumentChange) {
				// don't listen to events we generated ourself
				return;
			}

			// This is sub-optimal (but easy):
			// For each modification of the document we parse the complete document again.
			// Not very efficient, but the fuses files are small and even on my
			// old and slow Notebook this takes less than one millisecond.
			updateByteValuesFromDocument();
		}
	}

	// ------------ IByteValuesChangedListener -------------------

	/**
	 * Listener for ByteValues change events.
	 * <p>
	 * Whenever the <code>ByteValues</code> object has been changed, e.g. by the form editor, the
	 * document is updated accordingly.
	 * </p>
	 */
	private class MyByteValuesChangedListener implements IByteValuesChangeListener {


		@Override
		public void byteValuesChanged(ByteValueChangeEvent[] events) {

			if (fInByteValuesChange) {
				// don't listen to events we generated ourself
				return;
			}

			for (ByteValueChangeEvent event : events) {
				String key = event.name;
				if (key.equals(ByteValues.MCU_CHANGE_EVENT)) {
					// If the MCU has changed we clear the document and rewrite it completely
					clearDocument();
					updateDocumentFromByteValues(fByteValues);
				} else if (key.equals(ByteValues.COMMENT_CHANGE_EVENT)) {
					// The comment has changed
					String comment = fByteValues.getComment();
					setDocumentComment(comment);
				} else {
					// a single value has changed. Update the property or remove it if the value has
					// become undefined (-1)
					if (event.bytevalue != -1) {
						setDocumentValue(key, event.bitfieldvalue);
					} else {
						removeDocumentValue(key);
					}
				}
			}
		}

	}

	// ---- DocumentProvider Element Change Listener Methods ------

	/**
	 * Listener for Element State change events.
	 * <p>
	 * This is used to listen for move / rename events to track the source file, so that problem
	 * markers can be set and removed.
	 * </p>
	 * 
	 */
	private class MyElementStateListener implements IElementStateListener {

		@Override
		public void elementMoved(Object originalElement, Object movedElement) {

			if (originalElement == null) {
				return;
			}
			// If our source file has moved, we need to store the new file, so that we can create
			// new problem markers for it. Existing problem markers are automatically moved by the
			// Workbench.
			IFile originalfile = getFileFromAdaptable(originalElement);
			IFile movedfile = getFileFromAdaptable(movedElement);
			if (fSource.equals(originalfile)) {
				fSource = movedfile;
			}
		}

		@Override
		public void elementDeleted(Object element) {
			// Nothing to do
			// Even if the source file has been deleted, the Document and therefore the ByteValues
			// object is still valid and it might be saved under a different name.
		}

		@Override
		public void elementContentAboutToBeReplaced(Object element) {
			// Nothing to do
		}

		@Override
		public void elementContentReplaced(Object element) {
			// Nothing to do
		}

		@Override
		public void elementDirtyStateChanged(Object element, boolean isDirty) {
			// Nothing to do
		}

	}

	/**
	 * Create a new DocumentByteValuesConnector.
	 * <p>
	 * The new connector takes the given document and registers as a listener to all changes of the
	 * document. The synchronized ByteValues object is created lazily with the getByteValues() or
	 * setByteValues() methods.
	 * </p>
	 * <p>
	 * The connector also registers itself as a listener to the provider to be informed if the
	 * source file is moved or renamed. The source element, which needs to be adaptable to
	 * <code>IFile</code> is used to determine the type of the ByteValues (FUSE or LOCKBITS) via the
	 * file extension. The file is also needed to create the <code>IMarker</code>s for all problems
	 * parsing the file.
	 * </p>
	 * <p>
	 * The connector needs to be disposed when it is not needed anymore to remove the listeners.
	 * </p>
	 * 
	 * @param provider
	 *            An <code>IDocumentProvider</code>
	 * @param document
	 *            The source <code>IDocument</code>
	 * @param element
	 *            The source file as an object that can be adapted to an <code>IFile</code>, e.g. an
	 *            <code>IFileEditorInput</code>
	 * @throws CoreException
	 *             if the element is not adaptable to <code>IFile</code>.
	 */
	public DocumentByteValuesConnector(IDocumentProvider provider, IDocument document,
			Object element) throws CoreException {

		// Note: With the provider and the source element we could determine the document ourself.
		// But then we would either depend on the caller to connect the source element for us, or
		// connect ourself and risk infinite loops as the constructor is called from the connect()
		// method of the FuseFileDocumentProvider.

		fSource = getFileFromAdaptable(element);
		if (fSource == null) {
			IStatus status = new Status(Status.ERROR, ArduinoConst.CORE_PLUGIN_ID,
					"Object must be an IFile", null);
			throw new CoreException(status);
		}

		// Create the listener objects
		fDocumentListener = new MyDocumentListener();
		fByteValuesListener = new MyByteValuesChangedListener();
		fElementStateListener = new MyElementStateListener();

		// and add the listener to the provider and the document
		fProvider = provider;
		fProvider.addElementStateListener(fElementStateListener);
		fDocument = document;
		fDocument.addDocumentListener(fDocumentListener);

		// The ByteValues object will be created lazily, i.e. when actually requested with the
		// getByteValues() method.
		fByteValues = null;
	}

	// ------- Public Methods -------

	/**
	 * Disposes the Connector.
	 * <p>
	 * This method will remove the document, provider and ByteValues listeners. After calling this
	 * method changes to either the document or the ByteValues are not synchronized anymore.
	 * </p>
	 */
	public void dispose() {
		fDocument.removeDocumentListener(fDocumentListener);
		fProvider.removeElementStateListener(fElementStateListener);

		if (fByteValues != null) {
			fByteValues.removeChangeListener(fByteValuesListener);
		}
	}

	/**
	 * Connects the given <code>ByteValues</code> to the source Document / File, copying all its
	 * values to the document.
	 * <p>
	 * The new values object replaces any previously generated values object. The previous values
	 * object is disconnected and will not be updated anymore.
	 * </p>
	 * 
	 * @param newvalues
	 *            A <code>ByteValues</code> object to connect to the source document. Must not be
	 *            <code>null</code>.
	 */
	public void setByteValues(ByteValues newvalues) {

		Assert.isNotNull(newvalues);

		updateDocumentFromByteValues(newvalues);

		if (fByteValues != null) {
			fByteValues.removeChangeListener(fByteValuesListener);
		}
		fByteValues = newvalues;
		fByteValues.addChangeListener(fByteValuesListener);
	}

	/**
	 * @return A <code>ByteValues</code> object connected to the source document.
	 */
	public ByteValues getByteValues() {
		if (fByteValues == null) {
			fByteValues = createByteValues();
			updateByteValuesFromDocument();
		}
		return fByteValues;
	}

	// ------- Private Methods -------

	/**
	 * Create a new ByteValues object from the source document.
	 * 
	 * @return Valid <code>ByteValues</code> object or <code>null</code> if either the source file
	 *         has an unknown extension or there is 'MCU' property tag in the source document.
	 */
	private ByteValues createByteValues() {

		FuseType type = null;
		try {
			type = getTypeFromFileExtension(fSource);
		} catch (CoreException ce) {
			// Exception is thrown if the file extension is neither ".fuses" or ".locks".
			// This should not happen, so we log the message and return null.
			AVRPlugin.getDefault().log(ce.getStatus());
			return null;
		}

		parseDocument();

		String mcuid = fKeyValueMap.get(KEY_MCU);
		if (mcuid == null) {
			setMissingMCUMarker();
			return null;
		}

		ByteValues newvalues = new ByteValues(type, mcuid);
		newvalues.addChangeListener(fByteValuesListener);
		if (newvalues.getByteCount() == 0) {
			// unknown MCU
			setIllegalValueMarker("MCU", mcuid);
		} else {
			clearMarker("MCU");
		}
		return newvalues;

	}

	/**
	 * Parse the source document and copy all applicable properties to the <code>ByteValues</code>
	 * object.
	 */
	private void updateByteValuesFromDocument() {

		if (fByteValues == null) {
			// no need to waste cycles until the ByteValues have been created.
			return;
		}

		parseDocument();

		fInByteValuesChange = true;
		for (String key : fKeyValueMap.keySet()) {
			String value = fKeyValueMap.get(key);
			if ("MCU".equalsIgnoreCase(key)) {
				String oldmcuid = fByteValues.getMCUId();
				if (!oldmcuid.equals(value)) {
					fByteValues.setMCUId(value, false);
					// All previously set BitFields might have changed
					// so just restart from the beginning.
					// On the second iteration the MCUs will match, so no
					// danger of recursion.
					updateByteValuesFromDocument();
					return;
				}
				if (fByteValues.getByteCount() == 0) {
					// unknown MCU
					setIllegalValueMarker("MCU", value);
				} else {
					clearMarker("MCU");
				}
				continue;
			}

			if ("summary".equalsIgnoreCase(key)) {
				String comment = value.replace("\\n", "\n");
				fByteValues.setComment(comment);
				continue;
			}

			if (fByteValues.getBitFieldDescription(key) == null) {
				setInvalidKeyMarker(key);
				continue;
			}
			try {
				int intvalue = Integer.decode(fKeyValueMap.get(key));
				fByteValues.setNamedValue(key, intvalue);
				clearMarker(key);
			} catch (NumberFormatException nfe) {
				setIllegalValueMarker(key, value);
			} catch (IllegalArgumentException iae) {
				setIllegalValueMarker(key, value);
			}
		}
		fInByteValuesChange = false;

	}

	/**
	 * Clears the document (removing all non-comment lines) and writes all values from the
	 * <code>ByteValues</code> object to the document.
	 * 
	 * @param newvalues
	 */
	private void updateDocumentFromByteValues(ByteValues newvalues) {

		clearDocument();

		setDocumentValue(KEY_MCU, newvalues.getMCUId());

		List<BitFieldDescription> bfdlist = newvalues.getBitfieldDescriptions();
		for (BitFieldDescription bfd : bfdlist) {
			String key = bfd.getName();
			int value = newvalues.getNamedValue(key);
			if (value == -1) {
				continue;
			}
			setDocumentValue(key, value);
		}

		String comment = newvalues.getComment();
		setDocumentComment(comment);
	}

	/**
	 * Sets the comment property of the document.
	 * <p>
	 * This method converts the given comment to a single line form, escaping all new line
	 * characters.
	 * </p>
	 * 
	 * @param comment
	 *            The new comment, may be <code>null</code>
	 */
	private void setDocumentComment(String comment) {
	
		String newcomment = comment==null?"":comment;
		
		// Escape all new line characters. The comment must stay on one line because the parser only
		// works with single lines.
		newcomment = newcomment.replace("\r\n", "\\n");
		newcomment = newcomment.replace("\n", "\\n");
		newcomment = newcomment.replace("\r", "\\n");
		setDocumentValue(KEY_COMMENT, newcomment);
	}

	/**
	 * Sets the document property with the given key to a new integer value. The value is converted
	 * to a hex string and prepended with "0x".
	 * 
	 * @param key
	 *            The property key
	 * @param value
	 *            The new integer value
	 */
	private void setDocumentValue(String key, int value) {
		String textvalue = "0x" + Integer.toHexString(value);
		setDocumentValue(key, textvalue);
	}

	/**
	 * Sets the document property with the given key to a new string value.
	 * 
	 * @param key
	 *            The property key
	 * @param value
	 *            The new string value. Must not contain new line characters but can be
	 *            <code>null</code>
	 */
	private void setDocumentValue(String key, String value) {
		try {

			int offset;
			int length;
			String text;

			Integer linenumber = fKeyLineMap.get(key);
			if (linenumber == null) {
				// Key does not exist yet - add it.
				offset = fDocument.getLength();
				length = 0;
				text = key + "=" + (value == null ? "" : value) + "\n";
				fInDocumentChange = true;
				fDocument.replace(offset, length, text);
				linenumber = fDocument.getLineOfOffset(offset + 4);

			} else {
				// Key exists - replace value;
				Position position = fKeyValuePositionMap.get(key);
				offset = position.getOffset();
				length = position.getLength();
				text = value == null ? "" : value;
				fInDocumentChange = true;
				fDocument.replace(offset, length, text);

				// remove the key from all maps so that it can be re-read
				fKeyValueMap.remove(key);
				fKeyLineMap.remove(key);
				fDocument.removePosition(fKeyPositionMap.get(key));
				fDocument.removePosition(fKeyValuePositionMap.get(key));
				fKeyPositionMap.remove(key);
				fKeyValuePositionMap.remove(key);
			}
			// (re)read the line to update the internal maps
			parseLine(linenumber);
			clearMarker(key);

		} catch (BadLocationException ble) {
			// This exception probably means that there is a bug in the code above, in other words
			// it should not happen.
			// The Exception is logged, but otherwise we ignore it. It should be thrown, but then we
			// would have to throw it all the way up to the ByteValues.setValue() method, breaking
			// lots of stuff on the way.
			IStatus status = new Status(IStatus.WARNING, ArduinoConst.CORE_PLUGIN_ID,
					"Bug in setDocumentValue(). Please contact the plugin author.", ble);
			AVRPlugin.getDefault().log(status);

			// Because our stored meta information about the document might have become foul we
			// parse the document again just in case
			parseDocument();
		} finally {
			fInDocumentChange = false;
		}
	}

	/**
	 * Remove the line containing the given key from the document.
	 * <p>
	 * If the key does not exist in the document nothing is changed.
	 * </p>
	 * 
	 * @param key
	 *            The key which is to be completely removed from the document.
	 */
	private void removeDocumentValue(String key) {

		// This method is called from the ByteValues change listener
		// when a BitField has been set to -1, i.e. it has been undefined.

		try {
			Integer linenumber = fKeyLineMap.get(key);
			if (linenumber == null) {
				// document did not contain the key.
				// do nothing and return
				return;
			}

			// first remove the key from all lists
			fKeyValueMap.remove(key);
			fKeyLineMap.remove(key);
			fDocument.removePosition(fKeyPositionMap.get(key));
			fDocument.removePosition(fKeyValuePositionMap.get(key));
			fKeyPositionMap.remove(key);
			fKeyValuePositionMap.remove(key);
			clearMarker(key);

			// then update the list of lines, moving all lines behind the one to remove up by one.
			for (String otherkey : fKeyLineMap.keySet()) {
				int otherline = fKeyLineMap.get(otherkey);
				if (otherline > linenumber) {
					fKeyLineMap.put(otherkey, otherline - 1);
				}
			}

			// finally remove the line from the document
			IRegion lineregion = fDocument.getLineInformation(linenumber);
			String delimiter = fDocument.getLineDelimiter(linenumber);
			int offset = lineregion.getOffset();
			int length = lineregion.getLength() + delimiter.length();
			fInDocumentChange = true;
			fDocument.replace(offset, length, null);

		} catch (BadLocationException ble) {
			// This exception probably means that there is a bug in the code above, in other words
			// it should not happen.
			// The Exception is logged, but otherwise we ignore it. It should be thrown, but then we
			// would have to throw it all the way up to the ByteValues.setValue() method, breaking
			// lots of stuff on the way.
			IStatus status = new Status(IStatus.WARNING, ArduinoConst.CORE_PLUGIN_ID,
					"Bug in removeDocumentValue(). Please contact the plugin author.", ble);
			AVRPlugin.getDefault().log(status);

			// Because our stored meta information about the document might have become foul we
			// parse the document again just in case
			parseDocument();
		} finally {
			fInDocumentChange = false;
		}

	}

	/**
	 * Remove all lines with valid properties from the document. After calling this method only the
	 * comments and lines without an '=' remain.
	 */
	private void clearDocument() {

		clearAllMarkers();

		// remove all lines with keys in them
		for (String key : fKeyPositionMap.keySet()) {
			Position position = fKeyPositionMap.get(key);
			if (!position.isDeleted) {
				try {
					int line = fDocument.getLineOfOffset(position.offset);
					int offset = fDocument.getLineOffset(line);
					int length = fDocument.getLineLength(line);
					fInDocumentChange = true;
					fDocument.replace(offset, length, null);
				} catch (BadLocationException ble) {
					// This exception probably means that there is a bug in the code above, in other
					// words it should not happen.
					// The Exception is logged, but otherwise we ignore it.
					IStatus status = new Status(IStatus.WARNING, ArduinoConst.CORE_PLUGIN_ID,
							"Bug in clearDocument(). Please contact the plugin author.", ble);
					AVRPlugin.getDefault().log(status);

				} finally {
					fInDocumentChange = false;
				}
			}
		}
		fKeyLineMap.clear();
		fKeyPositionMap.clear();
		fKeyValueMap.clear();
		fKeyValuePositionMap.clear();

	}

	/**
	 * Create an error marker to inform the user that the document has no valid "MCU" property.
	 */
	private void setMissingMCUMarker() {
		String message = MessageFormat.format("Required Property '{0}' missing", KEY_MCU);
		createMarker(KEY_MCU, IMarker.SEVERITY_ERROR, -1, 0, 0, message);
	}

	/**
	 * Create a warning marker to inform the user that a property key was not valid.
	 * 
	 * @param key
	 *            Property key
	 */
	private void setInvalidKeyMarker(String key) {
		Position keyPosition = fKeyPositionMap.get(key);
		int start = keyPosition.getOffset();
		int end = start + keyPosition.getLength();
		int linenumber = fKeyLineMap.get(key);
		String message = MessageFormat.format("Invalid BitField name '{0}'", key);
		createMarker(key, IMarker.SEVERITY_WARNING, linenumber, start, end, message);
	}

	/**
	 * Create a warning marker to inform the user that a property has an invalid value.
	 * 
	 * @param key
	 *            Property key
	 * @param value
	 *            the invalid value
	 */
	private void setIllegalValueMarker(String key, String value) {
		int linenumber = fKeyLineMap.get(key);
		Position valuePosition = fKeyValuePositionMap.get(key);
		int start = valuePosition.getOffset();
		int end = start + valuePosition.getLength();
		String message = MessageFormat.format("{0}: Invalid value [{1}]", key, value);
		createMarker(key, IMarker.SEVERITY_WARNING, linenumber, start, end, message);
	}

	/**
	 * Create a warning marker to inform the user that there is a duplicate key in the file.
	 * 
	 * @param key
	 *            The duplicate key
	 * @param linenumber
	 *            The line number of the duplicate
	 * @param start
	 *            Start offset of the duplicate key in the document
	 * @param end
	 *            End offset of the duplicate key in the document
	 */
	private void setDuplicateKeyMarker(String key, int linenumber, int start, int end) {
		String message = MessageFormat.format("Duplicate BitField name {0}", key);
		createMarker(null, IMarker.SEVERITY_WARNING, linenumber, start, end, message);
	}

	/**
	 * Creates a new <code>IMarker</code> and sets the given attibutes.
	 * 
	 * @param key
	 *            The property key of the marker. Used to find and remove the marker once the
	 *            problem has been solved. May be <code>null</code>
	 * @param severity
	 *            One of the IMarker.SEVERITY_xxx levels
	 * @param linenumber
	 *            Linenumber of the Problem, or <code>-1</code> if the problem is not bound to a
	 *            single line
	 * @param start
	 *            The offset in the document where the problem starts
	 * @param end
	 *            The offset in the document where the problem ends.
	 * @param message
	 *            A human readable description of the problem
	 */
	private void createMarker(String key, int severity, int linenumber, int start, int end,
			String message) {

		if (fSource.exists()) {
			try {
				IMarker marker = fSource.createMarker(IMarker.PROBLEM);
				marker.setAttribute(IMarker.SEVERITY, severity);
				marker.setAttribute(IMarker.LINE_NUMBER, linenumber + 1);
				marker.setAttribute(IMarker.CHAR_START, start);
				marker.setAttribute(IMarker.CHAR_END, end);
				marker.setAttribute(IMarker.MESSAGE, message);
				marker.setAttribute(IMarker.SOURCE_ID, key);
				return;
			} catch (CoreException ce) {
				// ignore the exception -> no marker created
			}
		}
	}

	/**
	 * Removes all markers that may exist for a given key.
	 * <p>
	 * This method is called when the parser determines that a line is completely valid.
	 * </p>
	 * 
	 * @param key
	 */
	private void clearMarker(String key) {
		if (fSource.exists()) {
			// find all markers with the SOURCE_ID with the given key
			try {
				IMarker[] allmarkers = fSource.findMarkers(IMarker.PROBLEM, true,
						IResource.DEPTH_INFINITE);
				for (IMarker marker : allmarkers) {
					String markerkey = marker.getAttribute(IMarker.SOURCE_ID, "");
					if (markerkey.equals(key)) {
						marker.delete();
					}
				}

			} catch (CoreException ce) {
				// This Exception would be thrown if the resource does not exist (but we check that
				// it exists) or when the project is not open. The first case should not happen, the
				// latter case is just logged but otherwise ignored.
				IStatus status = new Status(IStatus.WARNING, ArduinoConst.CORE_PLUGIN_ID,
						"Could not clear marker for key '" + key + "'", ce);
				AVRPlugin.getDefault().log(status);
			}
		}
	}

	/**
	 * Clear all markers associated with the source file.
	 */
	private void clearAllMarkers() {
		if (fSource.exists()) {
			try {
				fSource.deleteMarkers(IMarker.PROBLEM, true, IResource.DEPTH_INFINITE);
			} catch (CoreException ce) {
				// This Exception would be thrown if the resource does not exist (but we check that
				// it exists), when Resource changes are not allowed, or the project is not open.
				// The first two cases should not happen and the last case is just logged but
				// otherwise ignored.
				IStatus status = new Status(IStatus.WARNING, ArduinoConst.CORE_PLUGIN_ID,
						"Could not clear markers", ce);
				AVRPlugin.getDefault().log(status);
			}
		}
	}

	/**
	 * Parses the source document.
	 * <p>
	 * Parses the document line by line. Once this method has finished, all internal metadata about
	 * the source document is up to date.
	 * </p>
	 * 
	 */
	private void parseDocument() {

		int lines = fDocument.getNumberOfLines();

		// Clear all maps
		fKeyLineMap.clear();
		fKeyPositionMap.clear();
		fKeyValuePositionMap.clear();
		fKeyValueMap.clear();
		clearAllMarkers();

		try {
			for (int linenumber = 0; linenumber < lines; linenumber++) {
				parseLine(linenumber);
			}
		} catch (BadLocationException ble) {
			// This exception probably means that there is a bug in the parseLine() method, in other
			// words it should not happen.
			// The Exception is logged, but otherwise we ignore it.
			IStatus status = new Status(IStatus.WARNING, ArduinoConst.CORE_PLUGIN_ID,
					"Bug in parseLine(). Please contact the plugin author.", ble);
			AVRPlugin.getDefault().log(status);

		}
	}

	/**
	 * Parses a single line of the source document.
	 * <p>
	 * Reads the line with the given line number from the source document and determines if it is
	 * either a comment, a valid key/value pair, an empty line or an invalid line. In the last case
	 * a problem marker is created immediatley.
	 * </p>
	 * <p>
	 * If it is a key/value pair, then its information is added to the internal document metadata,
	 * like the value or the <code>Position</code> of both the key and the value.
	 * </p>
	 * 
	 * @param linenumber
	 * @throws BadLocationException
	 */
	private void parseLine(int linenumber) throws BadLocationException {

		IRegion lineregion = fDocument.getLineInformation(linenumber);

		int offset = lineregion.getOffset();
		int length = lineregion.getLength();
		String linecontent = fDocument.get(offset, length);

		Matcher matcher;

		// Test if valid property line
		matcher = fPropertyPattern.matcher(linecontent);
		if (matcher.matches()) {
			String key = matcher.group(1);
			String value = matcher.group(2).trim();
			if (fKeyValueMap.containsKey(key)) {
				// duplicate key -> marks as error
				setDuplicateKeyMarker(key, linenumber, offset + matcher.start(1), offset
						+ matcher.end(1));
			}
			fKeyValueMap.put(key, value);
			fKeyLineMap.put(key, linenumber);

			int keyoffset = offset + matcher.start(1);
			int keylength = offset + matcher.end(1) - keyoffset;
			fKeyPositionMap.put(key, addPosition(keyoffset, keylength));

			int valueoffset = offset + matcher.start(2);
			int valuelength = offset + matcher.end(2) - valueoffset;
			fKeyValuePositionMap.put(key, addPosition(valueoffset, valuelength));
			return;

		}

		// Test if Comment
		matcher = fCommentPattern.matcher(linecontent);
		if (matcher.matches()) {
			return;
		}

		// Test if empty line
		if (linecontent.trim().length() == 0) {
			return;
		}

		createMarker(null, IMarker.SEVERITY_WARNING, linenumber, offset, offset + length,
				"Undefined line");
		return;
	}

	/**
	 * Creates a new <code>Position</code> object and adds it to the source document.
	 * <p>
	 * The new position is tracked by the document and updated whenever document changes affect the
	 * Position.
	 * </p>
	 * 
	 * @param offset
	 *            start of the position range
	 * @param length
	 *            number of chars in the position range
	 * @return New <code>Position</code> object.
	 * @throws BadLocationException
	 *             if the given range is not contained within the source document.
	 */
	private Position addPosition(int offset, int length) throws BadLocationException {
		Position position = new Position(offset, length);
		fDocument.addPosition(position);
		return position;
	}

	/**
	 * Gets the <code>FuseType</code> from the extension of a file.
	 * 
	 * @param file
	 *            File with a valid file extension
	 * @return Either {@link FuseType#FUSE} or {@link FuseType#LOCKBITS}
	 * @throws CoreException
	 *             if the given file has no or an unrecognized extension.
	 */
	private FuseType getTypeFromFileExtension(IFile file) throws CoreException {

		// First get the type of file from the extension
		String extension = file.getFileExtension();
		if (FuseType.FUSE.getExtension().equalsIgnoreCase(extension)) {
			return FuseType.FUSE;
		} else if (FuseType.LOCKBITS.getExtension().equalsIgnoreCase(extension)) {
			return FuseType.LOCKBITS;
		} else {
			IStatus status = new Status(Status.ERROR, ArduinoConst.CORE_PLUGIN_ID, "File ["
					+ file.getFullPath().toOSString() + "] has an unrecognized extension.", null);
			throw new CoreException(status);
		}
	}

	/**
	 * Get an <code>IFile</code> from an adaptable element.
	 * <p>
	 * In the normal use of this class the object element will be an <code>IFileEditorInput</code>,
	 * but other adaptable objects would be accepted if they are adaptable to an <code>IFile</code>.
	 * </p>
	 * 
	 * @param element
	 *            an <code>IAdaptable</code> object that can be adapted to an <code>IFile</code>.
	 * @return the <code>IFile</code> from the given element or <code>null</code> if the element
	 *         could not be adapted.
	 */
	private IFile getFileFromAdaptable(Object element) {
		if (element instanceof IAdaptable) {
			IAdaptable adaptable = (IAdaptable) element;
			IFile file = (IFile) adaptable.getAdapter(IFile.class);
			return file;
		}
		return null;
	}

}
