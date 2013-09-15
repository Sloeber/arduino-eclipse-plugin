package it.baeyens.arduino.actions;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;

/**
 * This is a handler to connect the plugin.xml to the code for opening the serial monitor
 * 
 * @author jan
 * 
 */
public class OpenSerialMonitorHandler extends AbstractHandler {

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
	try {
	    PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().showView("it.baeyens.arduino.monitor.views.SerialMonitor");
	} catch (PartInitException e) {
	    e.printStackTrace();
	}
	return null;
    }

}
