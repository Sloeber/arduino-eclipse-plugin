package io.sloeber.ui.monitor.internal;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.swt.widgets.Display;

import io.sloeber.core.api.MessageConsumer;
import io.sloeber.ui.Activator;
import io.sloeber.ui.Messages;
import io.sloeber.ui.monitor.views.MyPlotter;
@SuppressWarnings({"unused"})
public class PlotterListener implements MessageConsumer {

	MyPlotter myPlotter;
	/**
	 * myReceivedSerialData is a fixed size buffer holding the bytes that have been
	 * received from the com port
	 *
	 * if we are more than this behind we will loose data
	 */
	private ByteBuffer myReceivedSerialData = ByteBuffer.allocate(2000);

	public PlotterListener(MyPlotter plotter) {
		this.myReceivedSerialData.order(ByteOrder.LITTLE_ENDIAN);
		this.myPlotter = plotter;
	}

	/**
	 * Here the message comes in from the serial port. If there is not enough place
	 * in If there is enough place in myReceivedSerialData the data is added to
	 * myReceivedSerialData to hold all the data; all the data (that in
	 * myReceivedSerialData and in s) is ignored and a warning is dumped If there is
	 * enough place in myReceivedSerialData the data is added to
	 * myReceivedSerialData and myReceivedSerialData is scanned for plotter data
	 *
	 */
	@Override
	public synchronized void message(byte[] newData) {
		if (this.myPlotter.isDisposed())
			return;
		if (this.myReceivedSerialData.remaining() < newData.length) {
			this.myReceivedSerialData.clear();
			Activator.log(new Status(IStatus.WARNING, Activator.getId(), Messages.serialListenerPlotterSkippingData));
		} else {

			this.myReceivedSerialData.put(newData);
			this.myReceivedSerialData.flip();
			if (internalExtractAndProcessSerialData()) {
				addValuesToPlotter();
			}
			this.myReceivedSerialData.compact();
		}

	}

	@Override
	public void dispose() {
		this.myPlotter = null;
		this.myReceivedSerialData = null;
	}

	/**
	 * addValuesToPlotter This method makes the plotter to draw these values
	 * when a redraw is triggered
	 */
	public void addValuesToPlotter() {
		if (this.myPlotter.isDisposed())
			return;
		// run on the gui thread only
		Display.getDefault().asyncExec(new Runnable() {
			@Override
			public void run() {
				try {
					PlotterListener.this.myPlotter.redraw();
				} catch (Exception e) {
					// ignore as we get errors when closing down
				}
			}
		});

	}

	/**
	 * internalExtractAndProcessSerialData scans the incoming serial for plotter
	 * data in myReceivedSerialData If data is found it is send to the plotter all
	 * data that has been scanned
	 *
	 * returns true if the plotter needs to redraw (because data has been send)
	 * false if no data has been send to plotter
	 */
	private boolean internalExtractAndProcessSerialData() {
		boolean addedDataToPlotter = false;
		while (this.myReceivedSerialData.remaining() >= 6) {
			boolean found = false;
			// Scan for plotter data
			for (int curByte = this.myReceivedSerialData.position(); curByte < this.myReceivedSerialData.limit()
					- 4; curByte++) {
				if (this.myReceivedSerialData.getShort(curByte) == Activator.PLOTTER_START_DATA) {
					// we have a hit.
					this.myReceivedSerialData.position(curByte + 2);
					found = true;
					break;
				}
			}
			if (found) {
				int bytestoRead = this.myReceivedSerialData.getShort();
				if ((bytestoRead < 0) || (bytestoRead > 10 * 2)) {
					Activator.log(new Status(IStatus.WARNING, Activator.getId(), Messages.serial_listener_error.replace(Messages.NUMBER,Integer.toString( bytestoRead / 2))));
				} else {
					if (bytestoRead < this.myReceivedSerialData.remaining()) {
						// all data is available
						int numChannels = bytestoRead / 2;
						for (int curChannel = 0; curChannel < numChannels; curChannel++) {
							int data = this.myReceivedSerialData.getShort();
							this.myPlotter.setValue(curChannel, data);
							addedDataToPlotter = true;
						}

					} else {// not all data is available set the position at the beginning and wait for new
							// data
						this.myReceivedSerialData.position(this.myReceivedSerialData.position() - 4);
						return addedDataToPlotter;
					}
				}
			} else {
				this.myReceivedSerialData.position(this.myReceivedSerialData.limit() - 4);
			}
		}
		return addedDataToPlotter;
	}

	@Override
	public void event(String event) {
		// ignore events

	}
}
