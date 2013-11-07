package it.baeyens.arduino.actions;

import it.baeyens.arduino.common.ArduinoConst;
import it.baeyens.arduino.common.ArduinoInstancePreferences;
import it.baeyens.arduino.common.Common;

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

public class AddLibraryAction extends AbstractHandler {

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
	if (!ArduinoInstancePreferences.isConfigured(true))
	    return null;
	IProject SelectedProjects[] = Common.getSelectedProjects();
	switch (SelectedProjects.length) {
	case 0:
	    Common.log(new Status(IStatus.ERROR, ArduinoConst.CORE_PLUGIN_ID, "No project found to build"));
	    break;
	case 1:
	    //
	    IWizardDescriptor wizardDescriptor = PlatformUI.getWorkbench().getImportWizardRegistry()
		    .findWizard("it.baeyens.arduino.Import_Arduino_Libraries");
	    IWizard wizard;
	    try {
		wizard = wizardDescriptor.createWizard();
	    } catch (CoreException e) {
		Common.log(new Status(IStatus.ERROR, ArduinoConst.CORE_PLUGIN_ID, "Failed to find import wizard", e));
		return null;
	    }
	    WizardDialog wd = new WizardDialog(ConsolePlugin.getStandardDisplay().getActiveShell(), wizard);
	    wd.setTitle(wizard.getWindowTitle());
	    wd.open();
	    break;
	default:
	    Common.log(new Status(IStatus.ERROR, ArduinoConst.CORE_PLUGIN_ID, "Adding libraries to multiple projects is not supported"));
	}
	return null;
    }

}
