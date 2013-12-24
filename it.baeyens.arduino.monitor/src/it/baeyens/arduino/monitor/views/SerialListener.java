package it.baeyens.arduino.monitor.views;

import it.baeyens.arduino.arduino.MessageConsumer;
import it.baeyens.arduino.common.ArduinoConst;
import it.baeyens.arduino.common.Common;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import org.eclipse.swt.widgets.Display;

public class SerialListener implements MessageConsumer {
    private static boolean myScopeFilterFlag = false;
    SerialMonitor TheMonitor;
    int theColorIndex;
    private ByteBuffer myReceivedScopeData = ByteBuffer.allocate(2000);
    private int myEndPosition;

    SerialListener(SerialMonitor Monitor, int ColorIndex) {
	TheMonitor = Monitor;
	theColorIndex = ColorIndex;
	myReceivedScopeData.order(ByteOrder.LITTLE_ENDIAN);
    }

    @Override
    public void message(byte[] newData) {
	if (myScopeFilterFlag) {
	    // filter scope data

	    if (myEndPosition + newData.length >= myReceivedScopeData.capacity()) {
		myEndPosition = 0;
		Common.logWarn("Scope: skipping scope info to avoid buffer overflow");
	    } else {
		myReceivedScopeData.position(myEndPosition);
		myReceivedScopeData.put(newData, 0, newData.length);
		myEndPosition = myReceivedScopeData.position();
		internalExtractAndProcessScopeData();
	    }
	} else {
	    // treat data just like a event
	    event(new String(newData));
	}
    }

    private void internalExtractAndProcessScopeData() {
	int scannnedStartPointer = 0;
	byte[] dst;
	String inputMessage = new String(myReceivedScopeData.array());
	System.out.print(inputMessage.substring(0, myEndPosition));
	String MonitorMessage = "";
	int lastFoundData = -1;
	for (int scannnedScopePointer = 0; scannnedScopePointer < myEndPosition - 1; scannnedScopePointer++) {
	    if (myReceivedScopeData.getShort(scannnedScopePointer) == ArduinoConst.SCOPE_START_DATA) {
		// we have a hit.
		if (scannnedStartPointer > 0)// there is data before the scopehit
		{
		    // // buffer the data to send to scope
		    // ByteArrayOutputStream result = new ByteArrayOutputStream();
		    // myReceivedScopeData.position(scannnedStartPointer);
		    // result.write(myReceivedScopeData.array(), 0, scannnedScopePointer - scannnedStartPointer);
		    // MonitorMessage += result.toString();

		    dst = new byte[scannnedScopePointer - scannnedStartPointer];
		    myReceivedScopeData.position(scannnedStartPointer);
		    try {
			myReceivedScopeData.get(dst, 0, scannnedScopePointer - scannnedStartPointer);

			MonitorMessage += new String(dst);
		    } catch (Exception e) {
			Common.logWarn(
				"Serial Montor: buffer copy still not fixed scannnedScopePointer" + scannnedScopePointer + " scannnedStartPointer "
					+ scannnedStartPointer + " bufsize " + (scannnedScopePointer - scannnedStartPointer));
		    }

		}
		if (scannnedScopePointer + 4 >= myEndPosition) // the length of the scopedata set can not be read yet
		{
		    lastFoundData = scannnedScopePointer;
		    scannnedScopePointer = myEndPosition;
		} else {
		    int bytestoRead = myReceivedScopeData.getShort(scannnedScopePointer + 2);
		    if ((bytestoRead < 0) || (bytestoRead > 10 * 2)) {
			Common.logWarn("Serial Montor: There are supposedly " + bytestoRead / 2
				+ "channels to read");
			scannnedStartPointer = scannnedScopePointer; // process scope data as normal data
		    } else {
			if (bytestoRead + 4 + scannnedScopePointer < myEndPosition) {
			    // all data is available
			    scannnedScopePointer = scannnedScopePointer + bytestoRead + 4; // continue after the scope data
			    scannnedStartPointer = scannnedScopePointer;
			} else // not all data arrived for the latest data set
			{
			    lastFoundData = scannnedScopePointer;
			    scannnedScopePointer = myEndPosition;
			}
		    }
		}
	    }
	}
	if (lastFoundData == -1) // we did not end on a scope data set; check wether the last char is start of a new scope data set
	{
	    if (myReceivedScopeData.get(myEndPosition) == (byte) (ArduinoConst.SCOPE_START_DATA >> 8)) {
		lastFoundData = myEndPosition - 1;
	    }
	}
	if (lastFoundData != -1) // we need to keep some data
	{
	    // remove all the scanned data
	    for (int curByte = 0; curByte <= myEndPosition - lastFoundData; curByte++) {
		try {
		    myReceivedScopeData.put(curByte, myReceivedScopeData.get(curByte + lastFoundData));
		} catch (IndexOutOfBoundsException e) {
		    Common.logWarn("buffer overflow in ScopeListener ", e);
		}

	    }
	    myEndPosition -= lastFoundData;
	} else {
	    dst = new byte[myEndPosition - scannnedStartPointer];
	    myReceivedScopeData.position(scannnedStartPointer);
	    myReceivedScopeData.get(dst, 0, myEndPosition - scannnedStartPointer);
	    MonitorMessage += new String(dst);
	    myEndPosition = 0;
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
		    TheMonitor.ReportSerialActivity(TempString, theColorIndex);
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
