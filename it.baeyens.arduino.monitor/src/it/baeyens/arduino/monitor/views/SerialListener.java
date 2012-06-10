package it.baeyens.arduino.monitor.views;

import org.eclipse.swt.widgets.Display;
import it.baeyens.arduino.arduino.MessageConsumer;

public class SerialListener implements MessageConsumer
	{
		private SerialMonitor TheMonitor;
		private int theColorIndex;

		SerialListener(SerialMonitor Monitor, int ColorIndex)
		{
			TheMonitor =Monitor;
			theColorIndex=ColorIndex;
		}
		@Override
		public void message(String info)
			{
				
			  final String TempString= info;
        Display.getDefault().asyncExec(new Runnable() {
          public void run() {
          	try
          		{
          	TheMonitor.ReportSerialActivity(TempString,theColorIndex);
          		}
          	catch ( Exception e)
          		{//ignore as we get errors when closing down
          		}
          }});
			
			}

	}
