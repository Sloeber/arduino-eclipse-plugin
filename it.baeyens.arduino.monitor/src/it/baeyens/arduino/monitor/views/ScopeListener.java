package it.baeyens.arduino.monitor.views;

import it.baeyens.arduino.arduino.MessageConsumer;
import it.baeyens.arduino.common.ArduinoConst;
import it.baeyens.arduino.common.Common;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.LinkedList;
import java.util.Queue;

import multichannel.Oscilloscope;
import multichannel.OscilloscopeStackAdapter;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.swt.widgets.Display;

public class ScopeListener implements MessageConsumer {

    Queue<Integer> fStack = new LinkedList<Integer>();
    StringBuilder fSaveString = new StringBuilder();
    private OscilloscopeStackAdapter stackAdapter;
    Oscilloscope myScope;
    private ByteBuffer myReceivedScopeData = ByteBuffer.allocate(2000); // if we are more than this behind we will loose data
    private int myCurEnd = 0;

    public ScopeListener(Oscilloscope oscilloscope) {
	myReceivedScopeData.order(ByteOrder.LITTLE_ENDIAN);
	myScope = oscilloscope;
	stackAdapter = new OscilloscopeStackAdapter() {
	    private int oldValue = 0;

	    @Override
	    public void stackEmpty(Oscilloscope scope, int channel) {
		if (!fStack.isEmpty()) {
		    oldValue = fStack.remove().intValue();
		}
		// myScope.setValue(0, oldValue);
	    }
	};

	oscilloscope.addStackListener(0, stackAdapter);
    }

    @Override
    public synchronized void message(byte[] s) {
	if (myCurEnd >= myReceivedScopeData.capacity()) {
	    myCurEnd = 0;
	    Common.log(new Status(IStatus.WARNING, ArduinoConst.CORE_PLUGIN_ID, "skipping scope info"));
	}
	myReceivedScopeData.position(myCurEnd);
	int readsize = Math.min(myReceivedScopeData.capacity() - myCurEnd, s.length);
	myReceivedScopeData.put(s, 0, readsize);
	myCurEnd = myCurEnd + readsize;

	internalExtractAndProcessScopeData();

    }

    @Override
    public void dispose() {
	myScope.removeStackListener(0, stackAdapter);
    }

    /**
     * AddValuesToOsciloscope This method makes the scope to draw the values that have been delivered to the scope
     */
    public void AddValuesToOsciloscope() {
	if (myScope.isDisposed())
	    return;
	// run on the gui thread only
	Display.getDefault().asyncExec(new Runnable() {
	    @Override
	    public void run() {
		try {
		    myScope.getDispatcher(0).hookPulse(myScope, 1);
		    myScope.redraw();
		} catch (Exception e) {// ignore as we get errors when closing
				       // down
		}
	    }
	});

    }

    private void internalExtractAndProcessScopeData() {
	int scannnedDataPointer = 0;
	for (scannnedDataPointer = 0; scannnedDataPointer < myCurEnd; scannnedDataPointer++) {
	    if (myReceivedScopeData.get(scannnedDataPointer) == ArduinoConst.SCOPE_START_DATA[0]) {
		if (myReceivedScopeData.get(scannnedDataPointer + 1) == ArduinoConst.SCOPE_START_DATA[1]) {
		    // we have a hit.
		    scannnedDataPointer = scannnedDataPointer + 2;
		    if (myReceivedScopeData.getShort(scannnedDataPointer) + 2 + scannnedDataPointer < myCurEnd) {
			// all data is available
			int myNumDataSetsToReceive = myReceivedScopeData.getShort(scannnedDataPointer) / 2 + 0;
			for (int CurData = 0; CurData < myNumDataSetsToReceive; CurData++) {
			    int data = myReceivedScopeData.getShort(scannnedDataPointer + 2 + CurData * 2);
			    myScope.setValue(CurData, data);
			}
			AddValuesToOsciloscope();
			scannnedDataPointer = scannnedDataPointer + 2 + myNumDataSetsToReceive * 2;
			for (int curByte = 0; curByte < myCurEnd - scannnedDataPointer; curByte++) {
			    myReceivedScopeData.put(curByte, myReceivedScopeData.get(curByte + scannnedDataPointer));
			}
			myCurEnd = myCurEnd - scannnedDataPointer;
			scannnedDataPointer = 0;

		    }
		}
	    }
	}
	// // see whether we have a hit
	// String[] dataSets = myReceivedScopeData..split(ArduinoConst.SCOPE_START_DATA, 2);
	//
	// if (dataSets.length == 1) // no hit found. If the string is longer than 100 only keep the last 5 characters
	// {
	// if (myReceivedScopeData.length() > 100)
	// myReceivedScopeData = myReceivedScopeData.substring(myReceivedScopeData.length() - 5, myReceivedScopeData.length());
	// return;
	// }
	//
	// myReceivedScopeData = dataSets[1];
	// for (int curset = 0; curset < dataSets.length; curset++) {
	// if ((dataSets[curset].length()) > 4) // we need at least the data telling us how long the header is
	// {
	//
	// // we received the number of data elements we should receive
	// int myNumDataSetsToReceive = getIntValueFromString(dataSets[curset].getBytes(), 0) / 2;
	// if ((myNumDataSetsToReceive < 0) || (dataSets[curset].length() < (2 + (myNumDataSetsToReceive * 2)))) // Not all data is here
	// {
	// // This is the first data or a false hit
	//
	// } else {
	// // all data arrived
	//
	// for (int CurData = 0; CurData < myNumDataSetsToReceive; CurData++) {
	// myScope.setValue(CurData, getIntValueFromString(dataSets[curset].getBytes(), (2 + CurData * 2)));
	// }
	//
	//
	// if (curset == dataSets.length) // we processed the data in the last data set
	// {
	// myReceivedScopeData = dataSets[1].substring((2 + (myNumDataSetsToReceive * 2)));
	// }
	// }
	// }
	// }

    }

    // /**
    // * This method converts the byte stInfo[start] and stInfo[start+1] to a integer Following assumptions are made The bytes are send by arduino.
    // The
    // * first and second byte need to be swapped The receiving OS uses the same way of coding negative numbers as the arduino
    // */
    // private static int getIntValueFromString(byte[] stInfo, int start) {
    // ByteBuffer buf = ByteBuffer.allocate(2);
    // byte val1 = stInfo[start];
    // byte val2 = stInfo[start + 1];
    // buf.put(val2);
    // buf.put(val1);
    // short ret = buf.getShort(0);
    // return ret;
    //
    // // int val1 = stInfo[start];
    // // int val2 = stInfo[start + 1];
    // //
    // // int ret;
    // // if ((val2 & 128) == 128)
    // // ret = (0XFFFF0000 | ((val2 & 0xFF) << 8) | (val1 & 0XFF));
    // // else
    // // ret = (((val2 & 0xFF) << 8) | (val1 & 0XFF));
    // // return ret;
    //
    // }

    @Override
    public void event(String event) {
	// ignore events

    }
}
