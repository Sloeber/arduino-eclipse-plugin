package it.baeyens.arduino.actions;

import it.baeyens.arduino.ui.NewArduinoSketchWizard;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.wizard.IWizard;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.ui.console.ConsolePlugin;

public class NewSketchHandler extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		IWizard wizard = new NewArduinoSketchWizard();
		WizardDialog wd = new WizardDialog(ConsolePlugin.getStandardDisplay().getActiveShell(), wizard);
		wd.setTitle(wizard.getWindowTitle());
		wd.open();
		return null;
	}

}
