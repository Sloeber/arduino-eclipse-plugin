package it.baeyens.arduino.monitor.views;

import it.baeyens.arduino.arduino.MessageConsumer;

import org.eclipse.swt.widgets.Display;

public class SerialListener implements MessageConsumer {
	SerialMonitor TheMonitor;
	int theColorIndex;

	SerialListener(SerialMonitor Monitor, int ColorIndex) {
		TheMonitor = Monitor;
		theColorIndex = ColorIndex;
	}

	@Override
	public void message(String info) {

		final String TempString = info;
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

}
