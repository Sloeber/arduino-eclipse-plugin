/*******************************************************************************
 * 
 * Copyright (c) 2009, 2010 Thomas Holland (thomas@innot.de) and others
 * 
 * This program and the accompanying materials are made
 * available under the terms of the GNU Public License v3
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 * 
 * Contributors:
 *     Thomas Holland - initial API and implementation
 *     
 * $Id: AVRHardwareConfigValidator.java 851 2010-08-07 19:37:00Z innot $
 *     
 *******************************************************************************/

package it.baeyens.avreclipse.core.targets;

import it.baeyens.avreclipse.core.avrdude.AVRDudeException;
import it.baeyens.avreclipse.core.targets.ITargetConfiguration.Result;
import it.baeyens.avreclipse.core.targets.ITargetConfiguration.ValidationResult;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;


/**
 * @author Thomas Holland
 * @since 2.4
 * 
 */
public class AVRHardwareConfigValidator implements ITargetConfigConstants {

	public static List<ValidationResult> validate(ITargetConfiguration config) {

		List<ValidationResult> allresults = new ArrayList<ValidationResult>();

		ValidationResult result;

		result = checkMCU(config);
		if (!result.result.equals(Result.OK)) {
			allresults.add(result);
		}

		result = checkJTAGClock(config);
		if (!result.result.equals(Result.OK)) {
			allresults.add(result);
		}

		result = checkJTAGDaisyChainBitsBefore(config);
		if (!result.result.equals(Result.OK)) {
			allresults.add(result);
		}

		result = checkJTAGDaisyChainBitsAfter(config);
		if (!result.result.equals(Result.OK)) {
			allresults.add(result);
		}

		result = checkJTAGDaisyChainUnitsBefore(config);
		if (!result.result.equals(Result.OK)) {
			allresults.add(result);
		}

		result = checkJTAGDaisyChainUnitsAfter(config);
		if (!result.result.equals(Result.OK)) {
			allresults.add(result);
		}

		return allresults;
	}

	/**
	 * Check if the current MCU is supported by all tools.
	 * <p>
	 * This method will return {@link Result#OK} iff
	 * <ul>
	 * <li>The current MCU is in the list of supported MCUs from both the Programmer tool and the
	 * GDB Server (if they have been set).</li>
	 * </ul>
	 * If either tool has a list of supported mcus and the current mcu is not in them then
	 * {@link Result#ERROR} is returned.
	 * </p>
	 * <p>
	 * Calling this method may cause I/O activity (execution of the selected tools), it should not
	 * be called directly from the UI Thread.
	 * </p>
	 * 
	 * @param config
	 *            The target configuration to check.
	 * @return {@link ValidationResult} with the result code and a human readable description.
	 */
	public static ValidationResult checkMCU(ITargetConfiguration config) {
		try {

			String currentmcu = config.getMCU();

			// Check if the MCU is valid for both the Programmer Tool and the GDBServer
			// If either tool returns null as a list of supported mcus, then it is
			// assumed that the tool does not care about mcus and that every mcu is valid.
			IProgrammerTool progtool = config.getProgrammerTool();
			Set<String> progmcus = progtool.getMCUs();
			boolean progtoolOK = progmcus != null ? progmcus.contains(currentmcu) : true;

			IGDBServerTool gdbserver = config.getGDBServerTool();
			Set<String> gdbservermcus = gdbserver.getMCUs();
			boolean gdbserverOK = gdbservermcus != null ? gdbservermcus.contains(currentmcu) : true;

			if (!progtoolOK && !gdbserverOK) {
				// Neither tool supports the mcu
				String progtoolname = progtool.getName();
				String gdbservername = gdbserver.getName();
				String msg;
				if (progtoolname.equals(gdbservername)) {
					msg = "MCU is not supported by programming tool / gdbserver " + progtoolname;
				} else {
					msg = "MCU is not supported by programming tool " + progtool.getName()
							+ " and by gdbserver " + gdbserver.getName();
				}
				return new ValidationResult(Result.ERROR, msg);
			}
			if (!progtoolOK) {
				String msg = "MCU not supported by programming tool " + progtool.getName();
				return new ValidationResult(Result.ERROR, msg);
			}
			if (!progtoolOK) {
				String msg = "MCU not supported by gdbserver " + progtool.getName();
				return new ValidationResult(Result.ERROR, msg);
			}
		} catch (AVRDudeException ade) {
			// Don't wan't to throw the exception, but we can't ignore it either.
			// so we just report an error with the exception text as description.
			String msg = ade.getLocalizedMessage();
			return new ValidationResult(Result.ERROR, msg);
		}

		return ValidationResult.OK_RESULT;
	}

	/**
	 * Check if the current Programmer is supported by all tools.
	 * <p>
	 * This method will return {@link Result#OK} iff
	 * <ul>
	 * <li>The current Programmer is in the list of supported Programmers from both the Programmer
	 * tool and the GDB Server (if they have been set).</li>
	 * </ul>
	 * If either tool has a list of supported programmers and the current programmer is not in them
	 * then {@link Result#ERROR} is returned.
	 * </p>
	 * <p>
	 * Calling this method may cause I/O activity (execution of the selected tools), it should not
	 * be called directly from the UI Thread.
	 * </p>
	 * 
	 * @param config
	 *            The target configuration to check.
	 * @return {@link ValidationResult} with the result code and a human readable description.
	 */
	public static ValidationResult checkProgrammer(ITargetConfiguration config) {
		try {

			String currentprogger = config.getAttribute(ATTR_PROGRAMMER_ID);

			// Check if the Programmer is valid for both the Programmer Tool and the GDBServer
			// If either tool returns null as a list of supported programmers, then it is
			// assumed that the tool does not care about programmers and that every programmer is
			// valid.
			IProgrammerTool progtool = config.getProgrammerTool();
			Set<String> progProggers = progtool.getProgrammers();
			boolean progtoolOK = progProggers != null ? progProggers.contains(currentprogger)
					: true;

			IGDBServerTool gdbserver = config.getGDBServerTool();
			Set<String> gdbserverProggers = gdbserver.getProgrammers();
			boolean gdbserverOK = gdbserverProggers != null ? gdbserverProggers
					.contains(currentprogger) : true;

			if (!progtoolOK && !gdbserverOK) {
				// Neither tool supports the Programmer
				String progtoolname = progtool.getName();
				String gdbservername = gdbserver.getName();
				String msg;
				if (progtoolname.equals(gdbservername)) {
					msg = "Programmer interface is not supported by programming tool / gdbserver "
							+ progtoolname;
				} else {
					msg = "Programmer interface is not supported by programming tool "
							+ progtool.getName() + " and by gdbserver " + gdbserver.getName();
				}
				return new ValidationResult(Result.ERROR, msg);
			}
			if (!progtoolOK) {
				String msg = "Programmer interface not supported by programming tool "
						+ progtool.getName();
				return new ValidationResult(Result.ERROR, msg);
			}
			if (!gdbserverOK) {
				String msg = "Programmer interface not supported by gdbserver "
						+ progtool.getName();
				return new ValidationResult(Result.WARNING, msg);
			}
		} catch (AVRDudeException ade) {
			// Don't wan't to throw the exception, but we can't ignore it either.
			// so we just report an error with the exception text as description.
			String msg = ade.getLocalizedMessage();
			return new ValidationResult(Result.ERROR, msg);
		}

		return ValidationResult.OK_RESULT;

	}

	/**
	 * Check the JTAG clock frequency.
	 * <p>
	 * This method will return {@link Result#WARN} iff
	 * <ul>
	 * <li>the target interface supports settable clocks</li>
	 * <li>&& the bitclock is not set to the default</li>
	 * <li>&& the bitclock is greater than 1/4th of the current FCPU</li>
	 * </ul>
	 * In all other cases {@link Result#OK} is returned.
	 * </p>
	 * 
	 * @param config
	 *            The target configuration to check.
	 * @return {@link ValidationResult} with the result code and a human readable description.
	 */
	public static ValidationResult checkJTAGClock(ITargetConfiguration config) {

		// Check if the current configuration actually has a settable clock
		String programmerid = config.getAttribute(ATTR_PROGRAMMER_ID);
		IProgrammer programmer;
		programmer = config.getProgrammer(programmerid);
		int[] clocks = programmer.getTargetInterfaceClockFrequencies();
		if (clocks.length > 0) {

			// OK, the target interface has a selectable clock.
			// Now check if the default is set ( = ""). The warning is
			// inhibited with the default because we don't know what value the
			// default might have.
			String bitclock = config.getAttribute(ATTR_JTAG_CLOCK);
			if (bitclock.length() > 0) {

				// Not the default but an actual value.
				// Finally check if the selected clock is > 1/4th the target FCPU value
				int bitclockvalue = Integer.parseInt(bitclock);
				int targetfcpu = config.getFCPU();
				if (bitclockvalue > targetfcpu / 4) {
					String msg = MessageFormat
							.format(
									"selected BitClock Frequency of {0} is greater than 1/4th of the target MCU Clock ({1})",
									convertFrequencyToString(bitclockvalue),
									convertFrequencyToString(targetfcpu));
					return new ValidationResult(Result.WARNING, msg);
				}
			}
		}

		// JTAG_CLOCk is valid
		return ValidationResult.OK_RESULT;
	}

	/**
	 * Check the JTAG daisy chain 'bits before' attribute.
	 * <p>
	 * This method will return {@link Result#ERROR} iff
	 * <ul>
	 * <li>The current target interface is JTAG</li>
	 * <li>and daisy chaining is enabled</li>
	 * <li>and 'bits before' > 255</li>
	 * </ul>
	 * In all other cases {@link Result#OK} is returned.
	 * </p>
	 * <p>
	 * Note: The current implementation of AVRDude only accepts instruction bit values < 32. But
	 * this does not seem to be correct because the JTAG protocol accepts an 8-bit field.<br/>
	 * AVaRICE accepts all values but uses only the lower 8 bits.
	 * </p>
	 * 
	 * @param config
	 *            The target configuration to check.
	 * @return {@link ValidationResult} with the result code and a human readable description.
	 */
	public static ValidationResult checkJTAGDaisyChainBitsBefore(ITargetConfiguration config) {

		if (isDaisyChainEnabled(config)) {
			int bitsbefore = config.getIntegerAttribute(ATTR_DAISYCHAIN_BB);
			if (bitsbefore > 255) {
				String msg = "Daisy chain 'bits before' out of range (0 - 255)";
				return new ValidationResult(Result.ERROR, msg);
			}
		}

		return ValidationResult.OK_RESULT;
	}

	/**
	 * Check the JTAG daisy chain 'bits after' attribute.
	 * <p>
	 * This method will return {@link Result#ERROR} iff
	 * <ul>
	 * <li>The current target interface is JTAG</li>
	 * <li>and daisy chaining is enabled</li>
	 * <li>and 'bits after' > 255</li>
	 * </ul>
	 * In all other cases {@link Result#OK} is returned.
	 * </p>
	 * <p>
	 * Note: The current implementation of AVRDude only accepts instruction bit values < 32. But
	 * this does not seem to be correct because the JTAG protocol accepts an 8-bit field.<br/>
	 * AVaRICE accepts all values but uses only the lower 8 bits.
	 * </p>
	 * 
	 * @param config
	 *            The target configuration to check.
	 * @return {@link ValidationResult} with the result code and a human readable description.
	 */
	public static ValidationResult checkJTAGDaisyChainBitsAfter(ITargetConfiguration config) {

		if (isDaisyChainEnabled(config)) {
			int bitsafter = config.getIntegerAttribute(ATTR_DAISYCHAIN_BA);
			if (bitsafter > 255) {
				String msg = "Daisy chain 'bits after' out of range (0 - 255)";
				return new ValidationResult(Result.ERROR, msg);
			}
		}

		return ValidationResult.OK_RESULT;
	}

	/**
	 * Check the JTAG daisy chain 'units before' attribute.
	 * <p>
	 * This method will return {@link Result#ERROR} iff
	 * <ul>
	 * <li>The current target interface is JTAG</li>
	 * <li>and daisy chaining is enabled</li>
	 * <li>and 'units before' > 'bits before'</li>
	 * </ul>
	 * In all other cases {@link Result#OK} is returned.
	 * </p>
	 * 
	 * @param config
	 *            The target configuration to check.
	 * @return {@link ValidationResult} with the result code and a human readable description.
	 */
	public static ValidationResult checkJTAGDaisyChainUnitsBefore(ITargetConfiguration config) {

		if (isDaisyChainEnabled(config)) {
			int unitsbefore = config.getIntegerAttribute(ATTR_DAISYCHAIN_UB);
			int bitsbefore = config.getIntegerAttribute(ATTR_DAISYCHAIN_BB);

			if (unitsbefore > bitsbefore) {
				String msg = "Daisy chain 'Devices before' greater than 'bits before'";
				return new ValidationResult(Result.ERROR, msg);
			}
		}

		return ValidationResult.OK_RESULT;
	}

	/**
	 * Check the JTAG daisy chain 'units after' attribute.
	 * <p>
	 * This method will return {@link Result#ERROR} iff
	 * <ul>
	 * <li>The current target interface is JTAG</li>
	 * <li>and daisy chaining is enabled</li>
	 * <li>and 'units after' > 'bits after'</li>
	 * </ul>
	 * In all other cases {@link Result#OK} is returned.
	 * </p>
	 * 
	 * @param config
	 *            The target configuration to check.
	 * @return {@link ValidationResult} with the result code and a human readable description.
	 */
	public static ValidationResult checkJTAGDaisyChainUnitsAfter(ITargetConfiguration config) {

		if (isDaisyChainEnabled(config)) {
			int unitsafter = config.getIntegerAttribute(ATTR_DAISYCHAIN_UA);
			int bitsafter = config.getIntegerAttribute(ATTR_DAISYCHAIN_BA);

			if (unitsafter > bitsafter) {
				String msg = "Daisy chain 'Devices after' greater than 'bits after'";
				return new ValidationResult(Result.ERROR, msg);
			}
		}

		return ValidationResult.OK_RESULT;
	}

	/**
	 * Checks if daisy chain is possible and enabled.
	 * <p>
	 * The implementation checks if the selected programmer is capable of daisy chaining and if the
	 * ATTR_DAISYCHAIN_ENABLED flag is set.
	 * </p>
	 * 
	 * @see #isDaisyChainCapable(ITargetConfiguration)
	 * 
	 * @param config
	 *            The target configuration to check.
	 * @return <code>true</code> if the programmer supports daisy chaining and it is enabled.
	 */
	private static boolean isDaisyChainEnabled(ITargetConfiguration config) {
		String programmerid = config.getAttribute(ATTR_PROGRAMMER_ID);
		IProgrammer programmer = config.getProgrammer(programmerid);

		if (programmer.isDaisyChainCapable()) {
			return config.getBooleanAttribute(ATTR_DAISYCHAIN_ENABLE);
		}
		return false;
	}

	/**
	 * Convert a integer Hz value to a String.
	 * <p>
	 * The result has the unit appended:
	 * <ul>
	 * <li><code>Hz</code> for values below 1KHZ</li>
	 * <li><code>KHz</code> for values between 1 and 1000 KHz</li>
	 * <li><code>MHz</code> for values above 1000 KHz</li>
	 * </ul>
	 * As a special case the value <code>0</code> will result in "default".
	 * </p>
	 * 
	 * @param value
	 *            integer Hz value
	 * @return
	 */
	private static String convertFrequencyToString(int value) {
		String text;
		if (value == 0) {
			text = "default";
		} else if (value < 1000) {
			text = value + " Hz";
		} else if (value < 1000000) {
			float newvalue = value / 1000.0F;
			text = newvalue + " KHz";
		} else {
			float newvalue = value / 1000000.0F;
			text = newvalue + " MHz";
		}
		return text;
	}

}
