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
 * $Id: ConversionResults.java 851 2010-08-07 19:37:00Z innot $
 *     
 *******************************************************************************/
package it.baeyens.avreclipse.core.toolinfo.fuses;

import it.baeyens.avreclipse.core.util.AVRMCUidConverter;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.ui.console.IOConsoleOutputStream;
import org.eclipse.ui.console.MessageConsole;


/**
 * This class contains the results from a {@link ByteValues} conversion from one MCU to another.
 * <p>
 * It maintains three lists of {@link BitFieldDescription} objects:
 * <ul>
 * <li>BitFields successfully converted (<em>Success</em></li>
 * <li>BitFields from the source that had no match in the target mcu (<em>NotCopied</em>)</li>
 * <li>Bitfields in the target that had no corresponding BitField in the source (<em>UnSet</em>)</li>
 * </ul>
 * Currently there are only three public methods:</br> {@link #getStatusForName(String)} returns
 * the {@link ConversionStatus} for the BitField with a given name.</br>
 * {@link #printToConsole(MessageConsole)} dumps the result of the conversion in a human readable
 * format to the given Console.</br> {@link #getSuccessRate()} returns how successful the
 * conversion was as a percentage.</br>
 * </p>
 * <p>
 * To use this class instantiate it and pass the new object to the
 * {@link ByteValues#convertTo(String, ConversionResults)} method.
 * </p>
 * <p>
 * Objects of this class can be reused, as all parameters will be cleared once it is passed to the
 * <code>ByteValues.convertTo()</code> method.
 * </p>
 * <p>
 * This class is <strong>not</strong> thread safe. It should not be accessed while the conversion
 * is still running.
 * </p>
 * 
 * @author Thomas Holland
 * @since 2.3
 * 
 */
public class ConversionResults {

	/**
	 * An enumeration of conversion results for a single BitField.
	 * 
	 * @see ConversionResults#getStatusForName(String);
	 */
	public enum ConversionStatus {
		/** BitField has not been converted. This is the default if no conversion has taken place yet */
		NO_CONVERSION,

		/** BitField successfully converted, value text identical. */
		SUCCESS,

		/** BitField successfully converted, value text differs between source and target. */
		VALUE_CHANGED,

		/**
		 * Target BitField was set to the default value, because there was no matching BitField in
		 * the Source.
		 */
		NOT_IN_SOURCE,

		/** Source BitField was not copied because there was no matching BitField in the target. */
		NOT_IN_TARGET,

		/**
		 * A new value has been assigned to the BitField (presumably by the user and he has
		 * recognized the status).
		 */
		MODIFIED,

		/** Status is unknown. The BitField name was not in any of the three lists. */
		UNKNOWN;
	}

	/** List of all BitFields successfully copied. */
	private final List<BitFieldDescription>	fSuccessList		= new ArrayList<BitFieldDescription>();

	/** List of all BitFields from the source, that have no match in the target. */
	private final List<BitFieldDescription>	fNotCopiedList		= new ArrayList<BitFieldDescription>();

	/** List of all BitFields in the target, that have no match in the source. */
	private final List<BitFieldDescription>	fUnsetFieldsList	= new ArrayList<BitFieldDescription>();

	/** List of all BitFields in the target that have been modified since the last conversion. */
	private final List<String>				fModifiedList		= new ArrayList<String>();

	/** The source <code>ByteValues</code>. */
	private ByteValues						fSource				= null;

	/** The target <code>ByteValues</code>. */
	private ByteValues						fTarget				= null;

	/** Internal flag to indicate that the conversion is complete. */
	private boolean							fReady				= false;

	/**
	 * Called by {@link ByteValues#convertTo(String, ConversionResults)} to initialize the results.
	 * 
	 * @param source
	 *            The source <code>ByteValues</code> for the conversion.
	 * @param target
	 *            The target <code>ByteValues</code> for the conversion.
	 */
	protected void init(ByteValues source, ByteValues target) {
		fSource = source;
		fTarget = target;
		fSuccessList.clear();
		fNotCopiedList.clear();
		fNotCopiedList.clear();
		fNotCopiedList.addAll(fSource.getBitfieldDescriptions());
	}

	/**
	 * Called by {@link ByteValues#convertTo(String, ConversionResults)} to add a successful
	 * BitField conversion to the list.
	 * 
	 * @param desc
	 *            <code>BitFieldDescription</code>
	 */
	protected void addSuccess(BitFieldDescription desc) {
		fSuccessList.add(desc);
	}

	/**
	 * Called by {@link ByteValues#convertTo(String, ConversionResults)} to remove a source BitField
	 * which could not be converted.
	 * 
	 * @param desc
	 *            <code>BitFieldDescription</code>
	 */
	protected void removeNotCopied(BitFieldDescription desc) {
		fNotCopiedList.remove(desc);
	}

	/**
	 * Called by {@link ByteValues#convertTo(String, ConversionResults)} to add a target BitField
	 * which was set to the default value.
	 * 
	 * @param desc
	 *            <code>BitFieldDescription</code>
	 */
	protected void addUnset(BitFieldDescription desc) {
		fUnsetFieldsList.add(desc);
	}

	/**
	 * Called by {@link ByteValues#convertTo(String, ConversionResults)} to indicate that the
	 * conversion has finished.
	 * <p>
	 * Currently unused but might be used in the future to implement a threat safe access to this
	 * class.
	 * </p>
	 * 
	 */
	protected void setReady() {
		fReady = true;
	}

	/**
	 * Adds a BitField name to the list of bitfields that have been modified since the last
	 * conversion.
	 * <p>
	 * {@link #getStatusForName(String)} will return {@link ConversionStatus#MODIFIED} for these
	 * fields.
	 * </p>
	 * 
	 * @param name
	 *            The name of a BitField. Ignored if <code>null</code>
	 */
	protected void setModified(String name) {
		if (name != null) {
			fModifiedList.add(name);
		}
	}

	/**
	 * Get the {@link ConversionStatus} for the BitField with the given name.
	 * 
	 * @param name
	 *            The name of a BitField.
	 * @return The conversion status for the BitField with the given name.
	 */
	public ConversionStatus getStatusForName(String name) {

		for (String bitfieldname : fModifiedList) {
			if (bitfieldname.equals(name)) {
				return ConversionStatus.MODIFIED;
			}
		}

		for (BitFieldDescription bfd : fSuccessList) {
			if (bfd.getName().equals(name)) {
				// Check if the value text has changed
				String oldvalue = fSource.getNamedValueText(name);
				String newvalue = fTarget.getNamedValueText(name);
				if (oldvalue.equals(newvalue)) {
					return ConversionStatus.SUCCESS;
				}
				return ConversionStatus.VALUE_CHANGED;
			}
		}

		for (BitFieldDescription bfd : fNotCopiedList) {
			if (bfd.getName().equals(name)) {
				return ConversionStatus.NOT_IN_TARGET;
			}
		}

		for (BitFieldDescription bfd : fUnsetFieldsList) {
			if (bfd.getName().equals(name)) {
				return ConversionStatus.NOT_IN_SOURCE;
			}
		}

		return ConversionStatus.UNKNOWN;
	}

	/**
	 * Dump the results of the conversion in a human readable form to a console.
	 * <p>
	 * The console is automatically moved to the front.
	 * </p>
	 * 
	 * @param console
	 *            Eclipse <code>MessageConsole</code>
	 */
	public void printToConsole(final MessageConsole console) {

		if (!fReady) {
			// The conversion is not yet finished
			// TODO: throw an Exception
			return;
		}

		Job consoleoutputjob = new Job("Console output") {

			@Override
			public IStatus run(IProgressMonitor monitor) {
				try {

					IOConsoleOutputStream iocos = console.newOutputStream();
					iocos.setActivateOnWrite(true);

					iocos.write("----------------------------------------\n");

					String sourcemcuname = AVRMCUidConverter.id2name(fSource.getMCUId());
					String targetmcuname = AVRMCUidConverter.id2name(fTarget.getMCUId());
					String message = MessageFormat.format(
							"Converting {0} Bytes from {1} MCU to the new project MCU {2}.\n",
							fSource.getType().toString(), sourcemcuname, targetmcuname);
					iocos.write(message);

					iocos.write("Successfully converted fields:\n");
					if (fSuccessList.size() == 0) {
						iocos.write("\tnone!\n");
					} else {
						for (BitFieldDescription bfd : fSuccessList) {
							String name = bfd.getName();
							iocos.write("\t" + name + "\t" + bfd.getDescription() + "\n");
							iocos
									.write("\t\told value:  " + fSource.getNamedValueText(name)
											+ "\n");
							iocos
									.write("\t\tnew value:  " + fTarget.getNamedValueText(name)
											+ "\n");
						}
					}
					iocos.write("\n");

					message = MessageFormat
							.format("Fields not converted (a {0} MCU does not have them):\n",
									targetmcuname);
					iocos.write(message);
					if (fNotCopiedList.size() == 0) {
						iocos.write("\tnone!\n");
					} else {
						for (BitFieldDescription bfd : fNotCopiedList) {
							String name = bfd.getName();
							iocos.write("\t" + name + "\t" + bfd.getDescription() + "\n");
							iocos
									.write("\t\told value:  " + fSource.getNamedValueText(name)
											+ "\n");
						}
					}
					iocos.write("\n");

					message = MessageFormat.format(
							"Fields not set (a {0} MCU does not have them):\n", sourcemcuname);
					iocos.write(message);
					if (fUnsetFieldsList.size() == 0) {
						iocos.write("\tnone!\n");
					} else {
						for (BitFieldDescription bfd : fUnsetFieldsList) {
							String name = bfd.getName();
							iocos.write("\t" + name + "\t" + bfd.getDescription() + "\n");
							iocos.write("\t\tdefault value:  " + fTarget.getNamedValueText(name)
									+ "\n");
						}
					}
					iocos.write("\n");

					int successrate = getSuccessRate();

					message = MessageFormat.format("Conversion {0}% successfull\n", successrate);
					iocos.write(message);

					message = MessageFormat.format("Please check the new {0} settings.\n"
							+ "Some bits might have a slightly different meaning for a {1} MCU\n"
							+ "than they had on a {2} MCU.\n", fSource.getType().toString(),
							targetmcuname, sourcemcuname);
					iocos.write(message);

					iocos.flush();
					iocos.close();
				} catch (IOException e) {
				} finally {
					monitor.done();
				}
				return Status.OK_STATUS;
			}
		};

		consoleoutputjob.setPriority(Job.SHORT);
		consoleoutputjob.setSystem(true);
		consoleoutputjob.schedule();

	}

	/**
	 * Get the successrate for the conversion.
	 * <p>
	 * The successrate is determined as the ratio of successful BitField conversions to the total of
	 * all (unique) BitFields form the source and the target.
	 * </p>
	 * <p>
	 * <code>100%</code> means all BitFields have been successfully converted 1 to 1.<br>
	 * <code>0%</code> means that not a single BitField could be converted.
	 * </p>
	 * 
	 * @return <code>int</code> between 0 and 100 (%).
	 */
	public int getSuccessRate() {

		int successsize = fSuccessList.size();
		int notcopiedsize = fNotCopiedList.size();
		int unsetsize = fUnsetFieldsList.size();

		int successrate = 100 - (((notcopiedsize + unsetsize) * 100) / (successsize + notcopiedsize + unsetsize));

		return successrate;
	}

}
