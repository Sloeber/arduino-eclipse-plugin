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
 * $Id: FuseFileDocumentProvider.java 851 2010-08-07 19:37:00Z innot $
 *     
 *******************************************************************************/
package it.baeyens.avreclipse.ui.editors;

import it.baeyens.avreclipse.core.toolinfo.fuses.ByteValues;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.filebuffers.IDocumentSetupParticipant;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.ui.editors.text.ForwardingDocumentProvider;
import org.eclipse.ui.editors.text.TextFileDocumentProvider;
import org.eclipse.ui.texteditor.IDocumentProvider;


/**
 * Special Document Provider for fuses and locks files.
 * <p>
 * This Provider is based on a <code>TextFileDocumentProvider</code>. For every connected Input
 * this provider maintains a {@link DocumentByteValuesConnector} object, which connects an
 * <code>IDocument</code> to a <code>ByteValues</code> object. This connector is responsible for
 * keeping the Document and the ByteValues in sync. Changes to either of them are immediatly
 * reflected in the other.
 * </p>
 * <p>
 * In addition to the <code>IDocumentProvider</code> interface this provider has two public
 * methods to get and set the ByteValues for a given input object.
 * </p>
 * <p>
 * This provider uses the singleton pattern. There is only one provider per Eclipse instance and it
 * get be retrieved with the static {@link #getDefault()} method.
 * </p>
 * 
 * @author Thomas Holland
 * @since 2.3
 */
public class FuseFileDocumentProvider extends ForwardingDocumentProvider {

	/** Singleton provider instance. */
	private static FuseFileDocumentProvider					fInstance;

	/** Map of ByteValues connectors for all connected inputs. */
	private static Map<Object, DocumentByteValuesConnector>	fConnectorsMap		= new HashMap<Object, DocumentByteValuesConnector>();

	/** Usage counter for all connected inputs. */
	private static Map<Object, Integer>						fConnectorsCount	= new HashMap<Object, Integer>();

	/**
	 * Dummy document setup participant. Currently only used because one is required for the
	 * <code>ForwardingDocumentProvider</code> superclass.
	 */
	private static class InternalDocumentSetupParticipant implements IDocumentSetupParticipant {

		@Override
		public void setup(IDocument document) {
			// nothing to setup yet
			// In the future we could set up a partitioner here, as well as a content assist.
		}
	}

	/**
	 * Get the Fuse file document provider.
	 * 
	 * @return The instance FuseFileDocumentProvider.
	 */
	public static FuseFileDocumentProvider getDefault() {

		if (fInstance == null) {
			fInstance = createProvider();
		}
		return fInstance;
	}

	/**
	 * Constructs a new fuse file document provider.
	 * <p>
	 * This constructor is implemented to support the "org.eclipse.ui.editors.documentProviders"
	 * extension point defined in the plugin.xml. It should not be called directly.
	 * </p>
	 * 
	 */
	public FuseFileDocumentProvider() {

		// TODO: Check if this constructor and the extension point is actually needed.
		// The Fuse file editor uses the singleton provider from the getDefault() method.
		super("__fuses", new InternalDocumentSetupParticipant(), new TextFileDocumentProvider());

	}

	/**
	 * Private constructor for the provider. All parameters are passed on to the superclass.
	 * 
	 * @param partitioning
	 *            the partitioning
	 * @param documentSetupParticipant
	 *            the document setup participant
	 * @param parentProvider
	 *            the parent document provider
	 */
	private FuseFileDocumentProvider(String partitioning,
			IDocumentSetupParticipant documentSetupParticipant, IDocumentProvider parentProvider) {
		super(partitioning, documentSetupParticipant, parentProvider);

	}

	/**
	 * Create a new fuse file document provider.
	 * <p>
	 * The new provider uses a <code>TextFileDocumentProvider</code> as a parent provider for the
	 * chain-of-command pattern and an empty <code>DocumentSetupParticipant</code>.
	 * </p>
	 * <p>
	 * This method is only called once from the static {@link #getDefault()} method to create the
	 * single instance.
	 * 
	 * @return A new document provider.
	 */
	private static FuseFileDocumentProvider createProvider() {

		String partitioning = "__fuses"; // don't yet know what this is for
		IDocumentSetupParticipant setupparticipant = new InternalDocumentSetupParticipant();
		IDocumentProvider parentprovider = new TextFileDocumentProvider();

		FuseFileDocumentProvider provider = new FuseFileDocumentProvider(partitioning,
				setupparticipant, parentprovider);

		return provider;

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.editors.text.ForwardingDocumentProvider#connect(java.lang.Object)
	 */
	@Override
	public void connect(Object element) throws CoreException {

		super.connect(element);

		DocumentByteValuesConnector connector = fConnectorsMap.get(element);
		if (connector == null) {
			connector = new DocumentByteValuesConnector(this, getDocument(element), element);
			fConnectorsMap.put(element, connector);
			fConnectorsCount.put(element, 1);
		} else {
			// someone already connected to this element
			// increase use counter
			int usecount = fConnectorsCount.get(element);
			fConnectorsCount.put(element, usecount + 1);
		}

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.editors.text.ForwardingDocumentProvider#disconnect(java.lang.Object)
	 */
	@Override
	public void disconnect(Object element) {

		// TODO Auto-generated method stub
		super.disconnect(element);
		Integer usecount = fConnectorsCount.get(element);
		if (usecount == null) {
			// the element was never connected

			return;
		} else {
			--usecount;
			if (usecount == 0) {
				// no active connections anymore
				DocumentByteValuesConnector dbvp = fConnectorsMap.get(element);
				dbvp.dispose();
				fConnectorsMap.remove(element);
				fConnectorsCount.remove(element);
			} else {
				// there are still some active connections
				fConnectorsCount.put(element, usecount);
			}
		}

	}

	/**
	 * Get the <code>ByteValues</code> for the given element.
	 * <p>
	 * The returned values object is connected to the <code>IDocument</code> for the same element.
	 * All changes to the values are immediately passed on to the source document as long as the
	 * element remains connected. Once the element is disconnected, the <code>ByteValues</code>
	 * can still be used, but the connection to the source document and source file is removed.
	 * </p>
	 * 
	 * @param element
	 *            the input element
	 * @return A <code>ByteValues</code> object. Can be <code>null</code> if the object could
	 *         not be created.
	 */
	public ByteValues getByteValues(Object element) {

		DocumentByteValuesConnector connector = fConnectorsMap.get(element);
		if (connector == null) {
			// probably not connected yet or the element was null
			return null;
		}
		return connector.getByteValues();
	}

	/**
	 * Copies the given <code>ByteValues</code> to an element.
	 * <p>
	 * The element must be connected before this method can be used.
	 * </p>
	 * <p>
	 * This method is used to initialize new fuses / locks files. The given byte values object is
	 * not modified. Only its values for MCU id and the current byte values are copied.<br>
	 * The <code>IDocument</code> associated with the input element is updated immediately. But it
	 * is up to the caller to save the document with the
	 * {@link #saveDocument(org.eclipse.core.runtime.IProgressMonitor, Object, IDocument, boolean)}
	 * method afterwards.
	 * </p>
	 * 
	 * @param element
	 *            the input element
	 * @param newvalues
	 *            Source <code>ByteValues</code>
	 */
	public void setByteValues(Object element, ByteValues newvalues) {

		DocumentByteValuesConnector connector = fConnectorsMap.get(element);
		if (connector == null) {
			// probably not connected yet
			return;
		}
		connector.setByteValues(newvalues);
	}
}
