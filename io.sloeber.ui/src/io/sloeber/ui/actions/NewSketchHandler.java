package io.sloeber.ui.actions;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.wizard.IWizard;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.ui.console.ConsolePlugin;

import io.sloeber.core.api.BoardsManager;
import io.sloeber.ui.Activator;
import io.sloeber.ui.Messages;
import io.sloeber.ui.wizard.newsketch.NewSketchWizard;

public class NewSketchHandler extends AbstractHandler {

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
	if (!BoardsManager.isReady()) {
	    Activator.log(new Status(IStatus.ERROR, Activator.getId(), Messages.pleaseWaitForInstallerJob, null));
	    return null;
	}

	IWizard wizard = new NewSketchWizard();
	WizardDialog wd = new WizardDialog(ConsolePlugin.getStandardDisplay().getActiveShell(), wizard);
	wd.setTitle(wizard.getWindowTitle());
	wd.open();
	return null;
    }
}
