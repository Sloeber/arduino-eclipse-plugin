package io.sloeber.ui.actions;

import static io.sloeber.ui.Activator.*;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.wizard.IWizard;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.ui.console.ConsolePlugin;

import io.sloeber.core.api.PackageManager;
import io.sloeber.ui.Messages;
import io.sloeber.ui.wizard.newsketch.NewSketchWizard;

public class NewSketchHandler extends AbstractHandler {

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
	if (!PackageManager.isReady()) {
		log(new Status(IStatus.ERROR, PLUGIN_ID, Messages.pleaseWaitForInstallerJob, null));
	    return null;
	}

	IWizard wizard = new NewSketchWizard();
	WizardDialog wd = new WizardDialog(ConsolePlugin.getStandardDisplay().getActiveShell(), wizard);
	wd.setTitle(wizard.getWindowTitle());
	wd.open();
	return null;
    }
}
