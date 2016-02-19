package it.baeyens.arduino.actions;

import java.awt.Desktop;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import it.baeyens.arduino.common.Common;
import it.baeyens.arduino.common.Const;
import it.baeyens.arduino.ui.Messages;

public class LinkCommandHandler extends AbstractHandler {

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
	try {
	    String url = event.getParameter("it.baeyens.arduino.core.link.parameter");//$NON-NLS-1$
	    Desktop.getDesktop().browse(new URI(url));
	} catch (IOException | URISyntaxException e) {
	    Common.log(new Status(IStatus.ERROR, Const.CORE_PLUGIN_ID, Messages.json_browser_fail, e));
	}
	return null;
    }

}
