package it.baeyens.arduino.application;

import org.eclipse.jface.action.IMenuManager;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.application.ActionBarAdvisor;
import org.eclipse.ui.application.IActionBarConfigurer;

public class ApplicationActionBarAdvisor extends ActionBarAdvisor {

    public ApplicationActionBarAdvisor(IActionBarConfigurer configurer) {
	super(configurer);
    }

    @Override
    protected void makeActions(IWorkbenchWindow window) {
	super.makeActions(window);
	System.out.println("Jaba Was here");
	// Not sure whether something is needed here JABA
    }

    @Override
    protected void fillMenuBar(IMenuManager menuBar) {
	super.fillMenuBar(menuBar);
	System.out.println("Jaba Was here");
	// Not sure whether something is needed here JABA
    }

}
