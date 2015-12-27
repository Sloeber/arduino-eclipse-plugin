package it.baeyens.arduino.monitor.views;

import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.swt.widgets.Display;

import it.baeyens.arduino.arduino.MessageConsumer;
import it.baeyens.arduino.common.ArduinoConst;
import it.baeyens.arduino.common.Common;

public class SerialListener implements MessageConsumer {
    private static boolean myScopeFilterFlag = false;
    SerialMonitor TheMonitor;
    int theColorIndex;
    private ByteBuffer myReceivedScopeData = ByteBuffer.allocate(2000);

    public void removeBytesFromStart(int n) {
	int index = 0;
	for (int i = 0; i < n; i++)
	    this.myReceivedScopeData.put(i, (byte) 0);
	for (int i = n; i < this.myReceivedScopeData.position(); i++) {
	    this.myReceivedScopeData.put(index++, this.myReceivedScopeData.get(i));
	    this.myReceivedScopeData.put(i, (byte) 0);
	}
	this.myReceivedScopeData.position(index);
    }

    SerialListener(SerialMonitor Monitor, int ColorIndex) {
	this.TheMonitor = Monitor;
	this.theColorIndex = ColorIndex;
	this.myReceivedScopeData.order(ByteOrder.LITTLE_ENDIAN);
    }

    @Override
    public void message(byte[] newData) {
	if (myScopeFilterFlag) {
	    // filter scope data
	    try {
		this.myReceivedScopeData.put(newData);
	    } catch (BufferOverflowException e) {
		this.myReceivedScopeData.clear();
		Common.log(new Status(IStatus.WARNING, ArduinoConst.CORE_PLUGIN_ID, Messages.SerialListener_scope_skipping_data));
	    }
	    internalExtractAndProcessScopeData();
	} else {
	    // treat data just like a event
	    event(new String(newData));
	}
    }

    private void internalExtractAndProcessScopeData() {
	String MonitorMessage = ArduinoConst.EMPTY_STRING;
	boolean dontProcessLastPart = false;
	for (int scannnedScopePointer = 0; scannnedScopePointer < this.myReceivedScopeData.position() - 1; scannnedScopePointer++) {
	    if (this.myReceivedScopeData.getShort(scannnedScopePointer) == ArduinoConst.SCOPE_START_DATA) {
		// we have a hit.
		if (scannnedScopePointer > 0)// there is data before the scopehit->handle it and remove it
		{
		    for (int n = 0; n < scannnedScopePointer; n++)
			MonitorMessage += Character.toString((char) this.myReceivedScopeData.get(n));
		    removeBytesFromStart(scannnedScopePointer);
		}
		// now we have a hit at the beginning of the buffer.
		if (this.myReceivedScopeData.position() < 4) // the scopedata is not complete yet
		{
		    scannnedScopePointer = this.myReceivedScopeData.position(); // stop the loop
		    dontProcessLastPart = true;
		} else {
		    int bytestoRead = this.myReceivedScopeData.getShort(2);
		    if ((bytestoRead < 0) || (bytestoRead > (10 * 2))) {
			Common.log(new Status(IStatus.WARNING, ArduinoConst.CORE_PLUGIN_ID,
				Messages.SerialListener_error_input_part_1 + bytestoRead / 2 + Messages.SerialListener_error_input_part_2));
			this.myReceivedScopeData.putShort(0, (short) 0); // process scope data as normal data remove the ArduinoConst.SCOPE_START_DATA
		    } else {
			if (bytestoRead + 4 < this.myReceivedScopeData.position()) {
			    // all data is available
			    removeBytesFromStart(bytestoRead + 4);
			    scannnedScopePointer = -1;
			} else // not all data arrived for the latest data set
			{
			    scannnedScopePointer = this.myReceivedScopeData.position();
			    dontProcessLastPart = true;
			}
		    }
		}
	    }
	}
	if (!dontProcessLastPart) // we don't end on a scope data set; check whether the last char is start of a new scope data set
	{
	    if (this.myReceivedScopeData.get(this.myReceivedScopeData.position()) == (byte) (ArduinoConst.SCOPE_START_DATA >> 8)) {
		for (int n = 0; n < this.myReceivedScopeData.position() - 1; n++)
		    MonitorMessage += Character.toString((char) this.myReceivedScopeData.get(n));
		removeBytesFromStart(this.myReceivedScopeData.position() - 1);
		this.myReceivedScopeData.position(1);
	    } else {
		for (int n = 0; n < this.myReceivedScopeData.position(); n++)
		    MonitorMessage += Character.toString((char) this.myReceivedScopeData.get(n));
		removeBytesFromStart(this.myReceivedScopeData.position());
		this.myReceivedScopeData.position(0);
	    }
	}
	event(MonitorMessage);
    }

    @Override
    public void dispose() {
	// No need to dispose something
    }

    @Override
    public void event(String event) {
	final String TempString = new String(event);
	Display.getDefault().asyncExec(new Runnable() {
	    @Override
	    public void run() {
		try {
		    SerialListener.this.TheMonitor.ReportSerialActivity(TempString, SerialListener.this.theColorIndex);
		} catch (Exception e) {// ignore as we get errors when closing
				       // down
		}
	    }
	});

    }

    static void setScopeFilter(boolean selection) {
	myScopeFilterFlag = selection;

    }
}
