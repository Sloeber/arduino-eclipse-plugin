package io.sloeber.ui.actions;

import static io.sloeber.ui.Activator.*;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.wizard.IWizard;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.console.ConsolePlugin;
import org.eclipse.ui.wizards.IWizardDescriptor;

import io.sloeber.arduinoFramework.api.BoardsManager;
import io.sloeber.ui.Messages;
import io.sloeber.ui.listeners.ProjectExplorerListener;

public class AddLibraryAction extends AbstractHandler {

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
	if (!BoardsManager.isReady()) {
		log(new Status(IStatus.ERROR, PLUGIN_ID, Messages.pleaseWaitForInstallerJob, null));
	    return null;
	}

	IProject SelectedProjects[] = ProjectExplorerListener.getSelectedProjects();
	switch (SelectedProjects.length) {
	case 0:
		log(new Status(IStatus.ERROR, PLUGIN_ID, "No project found to build")); //$NON-NLS-1$
	    break;
	case 1:
	    //
	    IWizardDescriptor wizardDescriptor = PlatformUI.getWorkbench().getImportWizardRegistry()
		    .findWizard("io.sloeber.Import_Arduino_Libraries"); //$NON-NLS-1$
	    IWizard wizard;
	    try {
		wizard = wizardDescriptor.createWizard();
	    } catch (CoreException e) {
			log(new Status(IStatus.ERROR, PLUGIN_ID, "Failed to find import wizard", e)); //$NON-NLS-1$
		return null;
	    }
	    WizardDialog wd = new WizardDialog(ConsolePlugin.getStandardDisplay().getActiveShell(), wizard);
	    wd.setTitle(wizard.getWindowTitle());
	    wd.open();
	    break;
	default:
		log(new Status(IStatus.ERROR, PLUGIN_ID,
		    "Adding libraries to multiple projects is not supported")); //$NON-NLS-1$
	}
	return null;
    }

}
