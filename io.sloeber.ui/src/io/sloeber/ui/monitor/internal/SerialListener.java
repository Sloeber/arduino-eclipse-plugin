package io.sloeber.ui.monitor.internal;

import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.swt.widgets.Display;

import io.sloeber.core.api.MessageConsumer;
import io.sloeber.ui.Activator;
import io.sloeber.ui.monitor.views.Messages;
import io.sloeber.ui.monitor.views.SerialMonitor;

public class SerialListener implements MessageConsumer {
	private static boolean myPlotterFilterFlag = false;
	SerialMonitor theMonitor;
	boolean isDisposed = false;
	int theColorIndex;
	private ByteBuffer myReceivedPlotterData = ByteBuffer.allocate(2000);

	public SerialListener(SerialMonitor monitor, int colorIndex) {
		this.theMonitor = monitor;
		this.theColorIndex = colorIndex;
		this.myReceivedPlotterData.order(ByteOrder.LITTLE_ENDIAN);
	}

	public int removeBytesFromStart(int n) {
		if (n == 0) {
			return this.myReceivedPlotterData.position();
		}
		int index = 0;
		for (int i = 0; i < n; i++)
			this.myReceivedPlotterData.put(i, (byte) 0);
		for (int i = n; i < this.myReceivedPlotterData.position(); i++) {
			this.myReceivedPlotterData.put(index++, this.myReceivedPlotterData.get(i));
			this.myReceivedPlotterData.put(i, (byte) 0);
		}

		return this.myReceivedPlotterData.position(index).position();
	}

	@Override
	public void message(byte[] newData) {
		if (myPlotterFilterFlag) {
			// filter plotter data
			try {
				this.myReceivedPlotterData.put(newData);
			} catch (BufferOverflowException e) {
				this.myReceivedPlotterData.clear();
				Activator.log(new Status(IStatus.WARNING, Activator.getId(), Messages.serialListenerPlotterSkippingData));
			}
			internalExtractAndRemovePlotterData();
		} else {
			// treat data just like a event
			if (newData[newData.length - 1] == '\r') {
				newData[newData.length - 1] = ' ';
			}
			event(new String(newData));
		}
	}

	private void internalExtractAndRemovePlotterData() {

		String monitorMessage = ""; //$NON-NLS-1$
		boolean doneSearching = false;
		int length = this.myReceivedPlotterData.position();
		int searchPointer;
		for (searchPointer = 0; (searchPointer < length - 1) && !doneSearching; searchPointer++) {
			if (this.myReceivedPlotterData.getShort(searchPointer) != Activator.PLOTTER_START_DATA) {
				char addChar = (char) this.myReceivedPlotterData.get(searchPointer);
				monitorMessage += Character.toString(addChar);
			} else {
				// have we received the full header of the plotter data?
				if (length < (searchPointer + 6)) {
					if (searchPointer != 0) {
						length = removeBytesFromStart(searchPointer);
					}
					doneSearching = true;
					// System.out.println("case 1"); //$NON-NLS-1$
				} else {
					int bytestoRead = this.myReceivedPlotterData.getShort(2);
					if ((bytestoRead < 0) || (bytestoRead > (10 * 2))) {
						Activator.log(
								new Status(IStatus.WARNING, Activator.getId(), Messages.serialListenerErrorInputPart1
										+ bytestoRead / 2 + Messages.serialListenerErrorInputPart2));
						searchPointer += 4;// skip the plotter start and length
											// so
						// data is shown
						// System.out.println("case 2"); //$NON-NLS-1$
					} else {
						if ((searchPointer + (bytestoRead + 4)) < length) {
							searchPointer += (bytestoRead + 4) - 1; // just skip
							// the
							// data
							// System.out.println("case 3"); //$NON-NLS-1$
						} else // not all data arrived for the latest data set
						{
							if (searchPointer != 0) {
								length = removeBytesFromStart(searchPointer);
							}
							doneSearching = true;

							// System.out.println("case 4"); //$NON-NLS-1$
						}
					}
				}
			}
		}
		if (!doneSearching) {
			if (searchPointer == length - 1) {

				byte addChar = this.myReceivedPlotterData.get(searchPointer);

				if (addChar != (byte) (Activator.PLOTTER_START_DATA)) {
					if (addChar == '\r') {
						addChar = ' ';
					}
					searchPointer++;
					monitorMessage += Character.toString((char) addChar);
				}
			}
			removeBytesFromStart(searchPointer);
		}
		event(monitorMessage);
	}

	@Override
	public void dispose() {
		this.isDisposed = true;
		this.myReceivedPlotterData.clear();
	}

	@Override
	public void event(String event) {
		final String tempString = new String(event);
		Display.getDefault().asyncExec(new Runnable() {
			@Override
			public void run() {
				try {
					if (!SerialListener.this.isDisposed) {
					SerialListener.this.theMonitor.ReportSerialActivity(tempString, SerialListener.this.theColorIndex);
					}
				} catch (Exception e) {// ignore as we get errors when closing
					// down
				}
			}
		});

	}

	public static void setPlotterFilter(boolean selection) {
		myPlotterFilterFlag = selection;

	}
}
