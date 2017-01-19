package io.sloeber.ui.monitor.internal;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.swt.widgets.Display;

import io.sloeber.core.api.MessageConsumer;
import io.sloeber.ui.Activator;
import io.sloeber.ui.monitor.views.Messages;
import io.sloeber.ui.monitor.views.MyPlotter;

public class PlotterListener implements MessageConsumer {

	MyPlotter myPlotter;
	/**
	 * myReceivedSerialData is a fixed size buffer holding the bytes that have
	 * been received from the com port
	 *
	 * if we are more than this behind we will loose data
	 */
	private ByteBuffer myReceivedSerialData = ByteBuffer.allocate(2000);
	/**
	 * myEndPosition points to the last byte that is still valid in the buffer.
	 * This is needed because myReceivedSerialData is fixed size.
	 *
	 */
	private int myEndPosition = 0;

	public PlotterListener(MyPlotter plotter) {
		this.myReceivedSerialData.order(ByteOrder.LITTLE_ENDIAN);
		this.myPlotter = plotter;
	}

	/**
	 * Here the message comes in from the serial port. If there is not enough
	 * place in If there is enough place in myReceivedSerialData the data is
	 * added to myReceivedSerialData to hold all the data; all the data (that in
	 * myReceivedSerialData and in s) is ignored and a warning is dumped If
	 * there is enough place in myReceivedSerialData the data is added to
	 * myReceivedSerialData and myReceivedSerialData is scanned for plotter data
	 *
	 */
	@Override
	public synchronized void message(byte[] newData) {
		if (this.myPlotter.isDisposed())
			return;
		if (this.myEndPosition + newData.length >= this.myReceivedSerialData.capacity()) {
			this.myEndPosition = 0;
			Activator.log(new Status(IStatus.WARNING, Activator.getId(), Messages.serialListenerPlotterSkippingData));
		} else {
			this.myReceivedSerialData.position(this.myEndPosition);
			this.myReceivedSerialData.put(newData, 0, newData.length);
			this.myEndPosition = this.myReceivedSerialData.position();
			internalExtractAndProcessSerialData();
		}

	}

	@Override
	public void dispose() {
		this.myPlotter = null;
		this.myReceivedSerialData = null;
	}

	/**
	 * addValuesToPlotter This method makes the plotter to draw the values that
	 * have been delivered to the plotter
	 */
	public void addValuesToPlotter() {
		if (this.myPlotter.isDisposed())
			return;
		// run on the gui thread only
		Display.getDefault().asyncExec(new Runnable() {
			@Override
			public void run() {
				try {
					// PlotterListener.this.myPlotter.getDispatcher(0).hookPulse(PlotterListener.this.myPlotter,
					// 1);
					PlotterListener.this.myPlotter.redraw();
				} catch (Exception e) {
					// ignore as we get errors when closing down
				}
			}
		});

	}

	/**
	 * internalExtractAndProcessSerialData scans the incoming serial for plotter
	 * data in myReceivedSerialData If data is found it is send to the plotter
	 * all data that has been scanned is removed from myReceivedSerialData
	 */
	private void internalExtractAndProcessSerialData() {
		int lastFoundData = this.myEndPosition - 6;
		if (lastFoundData < 0)
			return;
		// Scan for plotter data
		for (int scannnedDataPointer = 0; scannnedDataPointer < this.myEndPosition - 8; scannnedDataPointer++) {
			if (this.myReceivedSerialData.getShort(scannnedDataPointer) == Activator.PLOTTER_START_DATA) {
				// we have a hit.
				lastFoundData = scannnedDataPointer;
				scannnedDataPointer = scannnedDataPointer + 2;
				int bytestoRead = this.myReceivedSerialData.getShort(scannnedDataPointer);
				if ((bytestoRead < 0) || (bytestoRead > 10 * 2)) {
					Activator.log(new Status(IStatus.WARNING, Activator.getId(), Messages.serialListenerErrorInputPart1
							+ bytestoRead / 2 + ' ' + Messages.serialListenerErrorInputPart2));
				} else {
					if (bytestoRead + 2 + scannnedDataPointer < this.myEndPosition) {
						// all data is available
						int myNumDataSetsToReceive = this.myReceivedSerialData.getShort(scannnedDataPointer) / 2 + 0;
						for (int CurData = 0; CurData < myNumDataSetsToReceive; CurData++) {
							int data = this.myReceivedSerialData.getShort(scannnedDataPointer + 2 + CurData * 2);
							this.myPlotter.setValue(CurData, data);
						}
						addValuesToPlotter();
						scannnedDataPointer = scannnedDataPointer + 2 + myNumDataSetsToReceive * 2;
						lastFoundData = scannnedDataPointer;

					}
				}
			}
		}
		// remove all the scanned data
		for (int curByte = 0; curByte <= this.myEndPosition - lastFoundData; curByte++) {
			try {
				this.myReceivedSerialData.put(curByte, this.myReceivedSerialData.get(curByte + lastFoundData));
			} catch (IndexOutOfBoundsException e) {
				Activator
						.log(new Status(IStatus.WARNING, Activator.getId(), Messages.plotterListenerBufferOverflow, e));
			}

		}
		this.myEndPosition -= lastFoundData;

	}

	@Override
	public void event(String event) {
		// ignore events

	}
}
