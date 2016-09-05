package io.sloeber.core.actions;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import io.sloeber.common.Common;
import io.sloeber.common.Const;
import io.sloeber.core.ui.Messages;

public class LinkCommandHandler extends AbstractHandler {

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
	try {
	    String url = event.getParameter("io.sloeber.core.link.parameter");//$NON-NLS-1$
	    org.eclipse.swt.program.Program.launch(url);
	} catch (IllegalArgumentException e) {
	    Common.log(new Status(IStatus.ERROR, Const.CORE_PLUGIN_ID, Messages.json_browser_fail, e));
	}
	return null;
    }

}
