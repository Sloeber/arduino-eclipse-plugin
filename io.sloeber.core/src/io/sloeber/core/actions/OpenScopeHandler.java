package io.sloeber.core.actions;

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
public class OpenScopeHandler extends AbstractHandler {

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
	try {
	    PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().showView("io.sloeber.monitor.views.ScopeView"); //$NON-NLS-1$
	} catch (PartInitException e) {
	    e.printStackTrace();
	}
	return null;
    }

}
