/*******************************************************************************
 * 
 * Copyright (c) 2007, 2010 Thomas Holland (thomas@innot.de) and others
 * 
 * This program and the accompanying materials are made
 * available under the terms of the GNU Public License v3
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 * 
 * Contributors:
 *     Thomas Holland - initial API and implementation
 *     
 * $Id: DownloadRateCalculator.java 851 2010-08-07 19:37:00Z innot $
 *     
 *******************************************************************************/
package it.baeyens.avreclipse.util;

/**
 * Utility class to calculate an average download rate.
 * <p>
 * Objects of this class calculate the average rate of a download. To smooth the rate, the download
 * rate of the last <code>samplesize</code> blocks is used. The sample size can be set and has a
 * default of 20.
 * </p>
 * <p>
 * The timing is started with the {@link #start()} method, which should be as close as possible to
 * the actual start of the download.
 * </p>
 * Usage example with a sample size of 100:
 * 
 * <pre>
 * DownloadRateCalculator dac = new DownloadRateCalculator(100);
 * dac.start();
 * while (readbytes) {
 * 	int rate = dac.getCurrentRate(bytesread);
 * }
 * </pre>
 * 
 * 
 * @author Thomas Holland
 * @since 2.2
 * 
 */
public class DownloadRateCalculator {

	private final static int	DEFAULT_SAMPLE_SIZE	= 50;

	// Ring buffer for the last time/bytes read
	private long[][]			samples;

	// Index for the ring buffer
	private int					currentindex;

	// the start time of the download
	private long				starttime;

	// total number of bytes downloaded
	private int					totalbytes;

	// Number of nanoseconds in a second
	// Just a convenience so I don't miss a zero.
	private final static long	ONEBILLION			= 1000 * 1000 * 1000;

	/**
	 * Create a new DownloadRateCalculator with the default sample size.
	 */
	public DownloadRateCalculator() {
		this(DEFAULT_SAMPLE_SIZE);
	}

	/**
	 * Create a new DownloadRateCalculator with the given sample size.
	 * 
	 * @see #setSampleSize(int)
	 * 
	 * @param samplesize
	 *            Size of the sample buffer.
	 */
	public DownloadRateCalculator(int samplesize) {
		setSampleSize(samplesize);
	}

	/**
	 * Sets the size of the samples buffer.
	 * <p>
	 * While high sample sizes will result in a smoother rate, they will also cause the average rate
	 * to lag behind the real download rate.<br>
	 * Very small samplesizes will make the returned rate erratic because the time for a
	 * Stream.read() will differ dramatically for single calls (depending on the fill state of the
	 * internal buffers).
	 * </p>
	 * <p>
	 * A good samplesize is probably around 10% of the number of blocks and at least 20.
	 * </p>
	 * 
	 * @param size
	 */
	public void setSampleSize(int size) {

		samples = new long[(size < 3 ? 3 : size)][2];
		currentindex = 0;
	}

	/**
	 * Start the timing of the download.
	 */
	public void start() {
		currentindex = 0;

		starttime = System.nanoTime();

		totalbytes = 0;

		// clear the samples array
		for (int i = 0; i < samples.length; i++) {
			samples[i][0] = starttime;
			samples[i][1] = 0;
		}
	}

	/**
	 * Return the current average download rate in <code>bytes per second</code>.
	 * <p>
	 * The returned rate is the average of the last <code>samplesize</code> rates.
	 * </p>
	 * 
	 * @param bytesread
	 *            The number of bytes read since the last call to this method (or since
	 *            instantiation)
	 * @return long with the current download rate in Bytes per Second
	 */
	public long getCurrentRate(int bytesread) {
		long currenttime = System.nanoTime();
		totalbytes += bytesread;

		// Store the current time and total bytes read so far in the buffer
		samples[currentindex][0] = currenttime;
		samples[currentindex][1] = totalbytes;
		currentindex++;
		if (currentindex == samples.length) {
			currentindex = 0;
		}

		// Get the least recent time/totalbytes point and calculate the download
		// rate from that point to the current time/totalbytes
		// The oldest point is the one just one ahead in the buffer
		int fromindex = currentindex == samples.length - 1 ? 0 : currentindex;
		long fromtime = samples[fromindex][0];
		long frombytes = samples[fromindex][1];

		long deltatime = currenttime - fromtime;
		long deltabytes = totalbytes - frombytes;

		// convert to bytes per second
		long rate = (deltabytes * ONEBILLION) / deltatime;

		return rate;

	}

	private final static int	KBYTE	= 1024;
	private final static int	MBYTE	= KBYTE * KBYTE;

	/**
	 * Returns the given current rate in a human readable format.
	 * <p>
	 * The returned String is automatically adjusted to MByte / KByte / Bytes per second. Changeover
	 * to the next higher unit is done at 2.000 of the previous unit.
	 * </p>
	 * 
	 * @return <code>String</code> with the rate in human readable format.
	 */
	public String getCurrentRateString(int bytesread) {
		long rate = getCurrentRate(bytesread);
		if (rate > 2 * MBYTE) {
			return Long.toString(rate / MBYTE) + " MBytes/sec";
		} else if (rate > 2 * KBYTE) {
			return Long.toString(rate / KBYTE) + " KBytes/sec";
		} else {
			return Long.toString(rate) + " Bytes/sec";
		}
	}
}
