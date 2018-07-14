package io.sloeber.ui.monitor.internal;

import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.swt.widgets.Display;

import io.sloeber.core.api.MessageConsumer;
import io.sloeber.ui.Activator;
import io.sloeber.ui.Messages;
import io.sloeber.ui.monitor.views.SerialMonitor;
@SuppressWarnings({"unused"})
public class SerialListener implements MessageConsumer {
	private static boolean myPlotterFilterFlag = false;
	SerialMonitor theMonitor;
	boolean isDisposed = false;
	int theColorIndex;
	private ByteBuffer myReceivedSerialData = ByteBuffer.allocate(2000);

	public SerialListener(SerialMonitor monitor, int colorIndex) {
		this.theMonitor = monitor;
		this.theColorIndex = colorIndex;
		this.myReceivedSerialData.order(ByteOrder.LITTLE_ENDIAN);
	}

	@Override
	public void message(byte[] newData) {
		event(internalMessage(newData));
	}

	 public String internalMessage(byte[] newData) {
		String ret = new String();
		if (myPlotterFilterFlag) {
			// filter plotter data
			try {
				this.myReceivedSerialData.put(newData);
			} catch (BufferOverflowException e) {
				this.myReceivedSerialData.clear();
				Activator.log(
						new Status(IStatus.WARNING, Activator.getId(), Messages.serialListenerPlotterSkippingData));
				return ret;
			}

			if (this.myReceivedSerialData.remaining() > 4) {
				this.myReceivedSerialData.flip();
				ret = internalRemovePlotterData();
				this.myReceivedSerialData.compact();
			}
		} else {
			// treat data just like a event
			if (newData[newData.length - 1] == '\r') {
				newData[newData.length - 1] = ' ';
			}
			ret = (new String(newData));
		}
		return ret;
	}

	private String internalRemovePlotterData() {
		String outMessage = new String();

		int pos = this.myReceivedSerialData.position();
		String inMessage = StandardCharsets.US_ASCII.decode(this.myReceivedSerialData).toString();
		this.myReceivedSerialData.position(pos);


		boolean found = false;
		int lastFound = this.myReceivedSerialData.position();
		// Scan for plotter data
		for (int curByte = this.myReceivedSerialData.position(); curByte < this.myReceivedSerialData.limit()
				- 1; curByte++) {
			if (this.myReceivedSerialData.getShort(curByte) == Activator.PLOTTER_START_DATA) {
				// we have a hit.
				found = true;
				if ((lastFound != curByte) && (curByte > lastFound)) {
					outMessage += inMessage.substring(lastFound, curByte );
				}
				this.myReceivedSerialData.position(curByte);
				if (this.myReceivedSerialData.remaining() > 4) {
					int bytestoRead = this.myReceivedSerialData.getShort(curByte + 2);
					if ((bytestoRead < 0) || (bytestoRead > 10 * 2)) {
						Activator.log(
								new Status(IStatus.WARNING, Activator.getId(), Messages.serial_listener_error.replace(Messages.NUMBER,Integer.toString( bytestoRead / 2))));
					} else {
						if (bytestoRead + 4 <= this.myReceivedSerialData.remaining()) {
							int numChannels = bytestoRead / 2;
							this.myReceivedSerialData.getShort();
							this.myReceivedSerialData.getShort();
							for (int curChannel = 0; curChannel < numChannels; curChannel++) {
								this.myReceivedSerialData.getShort();
							}
							lastFound = this.myReceivedSerialData.position();
							curByte = lastFound - 1;
						} else {
							break;
						}
					}
				}
			}
		}
		if ((!found) && (this.myReceivedSerialData.limit() > 1) && (this.myReceivedSerialData
				.get(this.myReceivedSerialData.limit() - 1) != (Activator.PLOTTER_START_DATA >> 8))) {
			this.myReceivedSerialData.position(this.myReceivedSerialData.limit());
			outMessage = inMessage;
		}

		return outMessage;
	}

	@Override
	public void dispose() {
		this.isDisposed = true;
		this.myReceivedSerialData.clear();
	}

	public class TxtUpdater implements Runnable {
		private boolean running = false;
		private String additionalSerialData = new String();

		public synchronized void addData(String event) {
			if (!event.isEmpty()) {
				this.additionalSerialData = this.additionalSerialData + event;
				if (!this.running || this.additionalSerialData.length() > 500) {
					Display.getDefault().asyncExec(this);
					this.running = true;
				}
			}
		}
		private synchronized void synchronizedrun() {
			try {
				if (!SerialListener.this.isDisposed) {
					SerialListener.this.theMonitor.ReportSerialActivity(this.additionalSerialData,
							SerialListener.this.theColorIndex);
					this.additionalSerialData = new String();
				}
			} catch (Exception e) {// ignore as we get errors when closing
				// down
			}
			this.running = false;
		}
		@Override
		public void run() {
			synchronizedrun();
		}
	};

	TxtUpdater textUpdater = new TxtUpdater();

	@Override
	public void event(String event) {
		this.textUpdater.addData(event);

	}

	public static void setPlotterFilter(boolean selection) {
		myPlotterFilterFlag = selection;

	}
}
