package io.sloeber.ui.actions;

import static io.sloeber.ui.Activator.*;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import io.sloeber.core.api.BoardsManager;
import io.sloeber.core.api.SloeberProject;
import io.sloeber.ui.Messages;
import io.sloeber.ui.listeners.ProjectExplorerListener;

/**
 * This is a handler to connect the plugin.xml to the code for programming code
 * to arduino teensy .. using a programmer
 *
 * @author jan
 *
 */
public class BurnBootloaderHandler extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		if (!BoardsManager.isReady()) {
			log(new Status(IStatus.ERROR, PLUGIN_ID, Messages.pleaseWaitForInstallerJob, null));
			return null;
		}
		IProject selectedProject = ProjectExplorerListener.getSelectedProject();
		if (selectedProject != null) {
			SloeberProject sProject = SloeberProject.getSloeberProject(selectedProject, true);
			if (sProject != null) {
				sProject.burnBootloader();
			}
		}
		return null;
	}

}
