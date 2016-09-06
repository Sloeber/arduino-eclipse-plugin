package io.sloeber.core.actions;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.wizard.IWizard;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.ui.console.ConsolePlugin;

import io.sloeber.common.InstancePreferences;
import io.sloeber.core.ui.NewSketchWizard;

public class NewSketchHandler extends AbstractHandler {

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
	if (!InstancePreferences.isConfigured(true))
	    return null;
	IWizard wizard = new NewSketchWizard();
	WizardDialog wd = new WizardDialog(ConsolePlugin.getStandardDisplay().getActiveShell(), wizard);
	wd.setTitle(wizard.getWindowTitle());
	wd.open();
	return null;
    }
}
