package it.baeyens.arduino.application;

import org.eclipse.swt.graphics.Point;
import org.eclipse.ui.application.ActionBarAdvisor;
import org.eclipse.ui.application.IActionBarConfigurer;
import org.eclipse.ui.application.IWorkbenchWindowConfigurer;
import org.eclipse.ui.application.WorkbenchWindowAdvisor;

public class ApplicationWorkbenchWindowAdvisor extends WorkbenchWindowAdvisor {

    public ApplicationWorkbenchWindowAdvisor(IWorkbenchWindowConfigurer configurer) {
	super(configurer);
    }

    @Override
    public ActionBarAdvisor createActionBarAdvisor(IActionBarConfigurer configurer) {
	System.out.println("Jaba Was here"); //$NON-NLS-1$
	return new ApplicationActionBarAdvisor(configurer);
    }

    @Override
    public void preWindowOpen() {
	System.out.println("Jaba Was here"); //$NON-NLS-1$
	IWorkbenchWindowConfigurer configurer = getWindowConfigurer();
	configurer.setInitialSize(new Point(400, 300));
	configurer.setShowCoolBar(true);
	configurer.setShowStatusLine(true);
    }
}
