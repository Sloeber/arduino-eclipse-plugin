package io.sloeber.ui.actions;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IProject;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;

import io.sloeber.core.api.BoardDescriptor;
import io.sloeber.core.api.Sketch;
import io.sloeber.ui.helpers.MyPreferences;
import io.sloeber.ui.listeners.ProjectExplorerListener;

/**
 * This is a handler to connect the plugin.xml to the code for opening the
 * serial monitor
 *
 *
 * The code looks for all selected projects for the com port and the baudrate
 * and connects if they both are found
 *
 * @author jan
 *
 */
public class OpenSerialMonitorHandler extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		try {

			PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage()
					.showView("io.sloeber.ui.monitor.views.SerialMonitor"); //$NON-NLS-1$
			// find all projects
			IProject SelectedProjects[] = ProjectExplorerListener.getSelectedProjects();
			// if there are project selected and the autoConnectSerial feature
			// is
			// on
			if ((SelectedProjects.length > 0) && (MyPreferences.getOpenSerialWithMonitor() == true)) {
				for (IProject curproject : SelectedProjects) {
					int baud = Sketch.getCodeBaudRate(curproject);
					if (baud > 0) {
						String comPort = BoardDescriptor.getUploadPort(curproject);
						if (!comPort.isEmpty()) {
							io.sloeber.ui.monitor.SerialConnection.add(comPort, baud);
						}
					}
				}
			}
		} catch (PartInitException e) {
			e.printStackTrace();
		}
		return null;
	}

}
