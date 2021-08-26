package io.sloeber.ui.actions;

import static io.sloeber.ui.Activator.*;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.osgi.framework.FrameworkUtil;

import io.sloeber.ui.Messages;

public class LinkCommandHandler extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		try {
			org.osgi.framework.Version version = FrameworkUtil.getBundle(getClass()).getVersion();
			String url = event.getParameter("io.sloeber.core.link.parameter");//$NON-NLS-1$

			url = url.replace("${MAJOR_VERSION}", String.valueOf(version.getMajor())); //$NON-NLS-1$
			url = url.replace("${MINOR_VERSION}", String.valueOf(version.getMinor())); //$NON-NLS-1$
			url = url.replace("${MICRO_VERSION}", String.valueOf(version.getMinor())); //$NON-NLS-1$
			url = url.replace("${QUALIFIER_VERSION}", String.valueOf(version.getMinor())); //$NON-NLS-1$
			org.eclipse.swt.program.Program.launch(url);
		} catch (IllegalArgumentException e) {
			log(new Status(IStatus.ERROR, PLUGIN_ID, Messages.json_browser_fail, e));
		}
		return null;
	}

}
