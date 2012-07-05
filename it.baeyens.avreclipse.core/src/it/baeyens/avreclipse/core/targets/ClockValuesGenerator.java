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
 * $Id: ClockValuesGenerator.java 851 2010-08-07 19:37:00Z innot $
 *     
 *******************************************************************************/

package it.baeyens.avreclipse.core.targets;

import java.util.ArrayList;
import java.util.List;

/**
 * Generator for ISP / JTAG clock frequencies.
 * <p>
 * This class is used by the user interface to get a list of all possible clock frequencies a
 * programmer can accept.
 * </p>
 * <p>
 * This class has only one static method, {@link #getValues(ClockValuesType)}.
 * </p>
 * 
 * @author Thomas Holland
 * @since 2.4
 * 
 */
public final class ClockValuesGenerator {

	/**
	 * Enumeration of all available protocol types that {@link ClockValuesGenerator} can generate
	 * clock values for.
	 */
	public enum ClockValuesType {
		/** AVR ICE MkI. */
		JTAG1,

		/** AVR ICE MkII and AVR Dragon in JTAG or DW mode. */
		JTAG2,

		/** STK500, STK500v2 and clones. */
		STK500,

		/** AVR ISP MkII and AVR Dragon in ISP, PP or HVSP modes. */
		AVRISPMK2,

		/** AVR STK600. */
		STK600,

		/** USBTiny programmer. */
		USBTINY;

	}

	/**
	 * The clock frequency of the STK500 / STK 500v2 programmers. Used to calculate the STK500 list.
	 */
	private final static int		STK500_XTAL		= 7372800;

	/**
	 * The values for the JTAG1 protocol. AVR ICE MkI uses a fixed list with just 4 values from 125
	 * KHz to 1 MHz.
	 */
	private final static int[]		fJTAG1Values	= new int[] { 0, 125000, 250000, 500000,
			1000000								};

	/** JTAG2 values cache. */
	private static int[]			fJTAG2Values;

	/** STK500 values cache. */
	private static int[]			fSTK500Values;

	/** AVRISPMK2 values cache. */
	private static int[]			fAVRISPMK2Values;

	/** STK600 values cache. */
	private static int[]			fSTK600Values;

	/** USBTiny values cache. */
	private static int[]			fUSBTinyValues;

	/**
	 * Static list of all possible AVRISPMk2 values.
	 * <p>
	 * This list is used for the AVR ISP MkII / AVR Dragon programmers in ISP, PP or HVSP modes.
	 * </p>
	 * <p>
	 * The list of frequencies is taken from AVR069. All numbers with fractions have been rounded up
	 * to the next integer.
	 */
	private final static double[]	AVRICEMK2CLOCKS	= new double[] { 8000000, 4000000, 2000000,
			1000000, 500000, 250000, 125000, 96386, 89888, 84211, 79208, 74767, 70797, 67227,
			64000, 61069, 58395, 55945, 51613, 49690, 47905, 46243, 43244, 41885, 39409, 38278,
			36200, 34335, 32654, 31129, 29740, 28470, 27304, 25724, 24768, 23461, 22285, 21221,
			20254, 19371, 18562, 17583, 16914, 16097, 15356, 14520, 13914, 13224, 12599, 12031,
			11511, 10944, 10431, 9963, 9468, 9081, 8612, 8239, 7851, 7498, 7137, 6809, 6478, 6178,
			5879, 5607, 5359, 5093, 4870, 4633, 4418, 4209, 4019, 3823, 3645, 3474, 3310, 3161,
			3011, 2869, 2734, 2611, 2484, 2369, 2257, 2152, 2052, 1956, 1866, 1779, 1695, 1615,
			1539, 1468, 1398, 1333, 1271, 1212, 1155, 1101, 1049, 1000, 953, 909, 866, 826, 787,
			750, 715, 682, 650, 619, 590, 563, 536, 511, 487, 465, 443, 422, 402, 384, 366, 349,
			332, 317, 302, 288, 274, 261, 249, 238, 226, 216, 206, 196, 187, 178, 170, 162, 154,
			147, 140, 134, 128, 122, 116, 111, 105, 100, 95.4, 90.9, 86.6, 82.6, 78.7, 75.0, 71.5,
			68.2, 65.0, 61.9, 59.0, 56.3, 53.6, 51.1 };

	// prohibit instantiation
	private ClockValuesGenerator() {
	}

	/**
	 * Get the list of clock frequencies for the given programmer type.
	 * <p>
	 * To reduce the sizes of the lists and to make them more user friendly the values in the lists
	 * are rounded.
	 * <ul>
	 * <li>Values up to 1,000 are rounded to the next highest 10.</li>
	 * <li>Values up to 10,000 are rounded to the next highest 100.</li>
	 * <li>Values up to 100,000 are rounded to the next highest 1000.</li>
	 * </ul>
	 * </p>
	 * Due to the rounding the actual clock frequency used by the programmer can be slightly
	 * different. It is assumed that these aliasing errors will be small enough to not matter. </p>
	 * 
	 * @param type
	 *            The type of programmer.
	 * @return Array with some or all possible clock values. The first element in the array is
	 *         always 0, representing the default.
	 */
	public static int[] getValues(ClockValuesType type) {
		switch (type) {
			case JTAG1:
				return getJTAG1Values();
			case JTAG2:
				return getJTAG2Values();
			case STK500:
				return getSTK500Values();
			case AVRISPMK2:
				return getAVRISPMK2Values();
			case STK600:
				return getSTK600Values();
			case USBTINY:
				return getUSBTinyValues();
			default:
				return null;
		}
	}

	/**
	 * Calculate the JTAG1 protocol clocks.
	 * <p>
	 * The JTAG1 protocol can use only 4 fixed clock frequencies: 125KHz, 250KHz, 500KHz and 1MHz.
	 * </p>
	 * 
	 * @return
	 */
	private static int[] getJTAG1Values() {
		return fJTAG1Values;
	}

	/**
	 * Calculate the JTAG2 protocol clocks.
	 * <p>
	 * The JTAG2 protocol uses an 8 bit value representing frequencies from 3.6 KHz to 6.4 MHz. The
	 * actual formula is
	 * 
	 * <pre>
	 * freq = 5.35 MHz / value ; value = [2;255]
	 * </pre>
	 * 
	 * with the two special cases <code>value = 0</code> &rarr; <code>freq = 6.4 MHz</code> and
	 * <code>value = 1</code> &rarr; </code>freq = 2.8 MHz</code>.
	 * </p>
	 * 
	 * @return
	 */
	private static int[] getJTAG2Values() {
		if (fJTAG2Values == null) {
			List<Integer> values = new ArrayList<Integer>(256);

			int lastvalue = -1;

			// Step over all 256 possible duration values
			for (int i = 0; i < 256; i++) {

				// Special case for 0 and 1
				if (i == 0) {
					values.add(6400000);
				} else if (i == 1) {
					values.add(2800000);
				} else {
					double value = 5.35e6 / i;
					int newvalue = ceilRound(value);
					if (newvalue != lastvalue) {
						values.add(newvalue);
						lastvalue = newvalue;
					}
				}
			}
			values.add(0); // representing the default

			// Now reverse the list and convert it to integers
			int numvalues = values.size();
			fJTAG2Values = new int[numvalues];
			for (int i = 0; i < numvalues; i++) {
				fJTAG2Values[i] = values.get(numvalues - i - 1);
			}
		}

		return fJTAG2Values;
	}

	/**
	 * Calculate the STK500 protocol clocks.
	 * <p>
	 * The STK500 protocol uses an 8 bit value representing frequencies from 4.0 KHz to 1.0 MHz. The
	 * actual formula is
	 * 
	 * <pre>
	 * freq = 9.216 KHz / value ; value = [1;256]
	 * </pre>
	 * 
	 * where <code>9.216 KHz = STK500_XTAL / 8.0</code> (1/8th of the STK500 master clock)
	 * </p>
	 * <p>
	 * This uses the reverse of the formula used by avrdude, which differs from the formula given in
	 * AVR061, even though the avrdude source comments states that its algorithm is based on AVR061.
	 * </p>
	 * <p>
	 * Excerpt from the avrdude source code: <br/>
	 * <code>
	 * This code assumes that each count of the SCK duration parameter represents 8/f, where f is
	 * the clock frequency of the STK500 master processors (not the target). It appears that the
	 * STK500 bit bangs SCK. For small duration values, the actual SCK width is larger than
	 * expected. As the duration value increases, the SCK width error diminishes.
	 * </code>
	 * </p>
	 * 
	 * @return
	 */
	private static int[] getSTK500Values() {
		if (fSTK500Values == null) {
			List<Integer> values = new ArrayList<Integer>(256);

			int lastvalue = -1;

			double stepsize = STK500_XTAL / 8.0;

			// Step over all 256 possible duration values
			for (int i = 1; i <= 256; i++) {
				double value = stepsize / i;
				int newvalue = ceilRound(value);
				if (newvalue != lastvalue) {
					values.add(newvalue);
					lastvalue = newvalue;
				}
			}
			values.add(0); // representing the default

			// Now reverse the list and convert it to integers
			int numvalues = values.size();
			fSTK500Values = new int[numvalues];
			for (int i = 0; i < numvalues; i++) {
				fSTK500Values[i] = values.get(numvalues - i - 1);
			}
		}

		return fSTK500Values;
	}

	/**
	 * Calculate the AVR ISP MkII protocol clocks.
	 * <p>
	 * The clock frequencies are based on values from a table defined in AVR069. The values are just
	 * rounded but otherwise left as is.
	 * </p>
	 * 
	 * @return
	 */
	private static int[] getAVRISPMK2Values() {
		if (fAVRISPMK2Values == null) {
			List<Integer> values = new ArrayList<Integer>(256);

			int lastvalue = -1;

			// Step over all possible clock values
			for (double value : AVRICEMK2CLOCKS) {

				int newvalue = ceilRound(value);
				if (newvalue != lastvalue) {
					values.add(newvalue);
					lastvalue = newvalue;
				}
			}
			values.add(0); // representing the default

			// Now reverse the list
			int numvalues = values.size();
			fAVRISPMK2Values = new int[numvalues];
			for (int i = 0; i < numvalues; i++) {
				fAVRISPMK2Values[i] = values.get(numvalues - i - 1);
			}
		}

		return fAVRISPMK2Values;
	}

	/**
	 * Calculate the STK600 protocol clocks.
	 * <p>
	 * The STK600 protocol uses an 12 bit value representing frequencies from 1953 Hz to 8.0 MHz.
	 * The actual formula is
	 * 
	 * <pre>
	 * freq = 16 MHz / (2*(value+1)) ; value = [0;4095]
	 * </pre>
	 * 
	 * </p>
	 * 
	 * @return
	 */
	private static int[] getSTK600Values() {
		if (fSTK600Values == null) {
			List<Integer> values = new ArrayList<Integer>(4096);

			int lastvalue = -1;

			// Step over all 4096 possible duration values
			for (int i = 0; i < 4096; i++) {
				double value = 16e6 / (2 * (i + 1));
				int newvalue = ceilRound(value);
				if (newvalue != lastvalue) {
					values.add(newvalue);
					lastvalue = newvalue;
				}
			}
			values.add(0); // representing the default

			// Now reverse the list
			int numvalues = values.size();
			fSTK600Values = new int[numvalues];
			for (int i = 0; i < numvalues; i++) {
				fSTK600Values[i] = values.get(numvalues - i - 1);
			}
		}

		return fSTK600Values;
	}

	/**
	 * Calculate the TinyUSB protocol clocks.
	 * <p>
	 * The USBTiny protocol uses an 8 bit value representing frequencies from 4.0 KHz to 1.0 MHz.
	 * The actual formula is
	 * 
	 * <pre>
	 * freq = 1.0 MHz / value ; value = [1;250]
	 * </pre>
	 * 
	 * </p>
	 * 
	 * @return
	 */
	private static int[] getUSBTinyValues() {
		if (fUSBTinyValues == null) {
			List<Integer> values = new ArrayList<Integer>(250);

			int lastvalue = -1;

			// Step over all 250 possible duration values
			for (int i = 1; i <= 250; i++) {
				double value = 1e6 / i;
				int newvalue = ceilRound(value);
				if (newvalue != lastvalue) {
					values.add(newvalue);
					lastvalue = newvalue;
				}
			}
			values.add(0); // representing the default

			// Now reverse the list and convert it to integers
			int numvalues = values.size();
			fUSBTinyValues = new int[numvalues];
			for (int i = 0; i < numvalues; i++) {
				fUSBTinyValues[i] = values.get(numvalues - i - 1);
			}
		}

		return fUSBTinyValues;
	}

	/**
	 * Round the argument depending on the magnitude to the next higher value.
	 * <p>
	 * <ul>
	 * <li>Values up to 1,000 are rounded to the next highest 10.</li>
	 * <li>Values up to 10,000 are rounded to the next highest 100.</li>
	 * <li>Values up to 100,000 are rounded to the next highest 1000.</li>
	 * </ul>
	 * 
	 * @param value
	 *            The double to be rounded
	 * @return The rounded integer
	 */
	private static int ceilRound(double value) {
		if (value < 1000) {
			return (int) Math.ceil(value / 10) * 10;
		} else if (value < 10000) {
			return (int) Math.ceil(value / 100) * 100;
		} else {
			return (int) Math.ceil(value / 1000) * 1000;
		}

	}
}
