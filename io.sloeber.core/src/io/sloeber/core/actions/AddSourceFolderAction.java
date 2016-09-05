package io.sloeber.core.actions;

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

import io.sloeber.common.Common;
import io.sloeber.common.Const;
import io.sloeber.common.InstancePreferences;
import io.sloeber.core.listeners.ProjectExplorerListener;

//Brody added this to be symmetrical with AddLibrary

public class AddSourceFolderAction extends AbstractHandler {

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
	if (!InstancePreferences.isConfigured(true))
	    return null;
	IProject SelectedProjects[] = ProjectExplorerListener.getSelectedProjects();
	switch (SelectedProjects.length) {
	case 0:
	    Common.log(new Status(IStatus.ERROR, Const.CORE_PLUGIN_ID, "No project found to build")); //$NON-NLS-1$
	    break;
	case 1:
	    //
	    IWizardDescriptor wizardDescriptor = PlatformUI.getWorkbench().getImportWizardRegistry()
		    .findWizard("io.sloeber.Import_Source_Folder"); //$NON-NLS-1$
	    IWizard wizard;
	    try {
		wizard = wizardDescriptor.createWizard();
	    } catch (CoreException e) {
		Common.log(new Status(IStatus.ERROR, Const.CORE_PLUGIN_ID, "Failed to find import wizard", e)); //$NON-NLS-1$
		return null;
	    }
	    WizardDialog wd = new WizardDialog(ConsolePlugin.getStandardDisplay().getActiveShell(), wizard);
	    wd.setTitle(wizard.getWindowTitle());
	    wd.open();
	    break;
	default:
	    Common.log(new Status(IStatus.ERROR, Const.CORE_PLUGIN_ID, "Adding a source folder to multiple projects is not supported")); //$NON-NLS-1$
	}
	return null;
    }

}
