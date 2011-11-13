package it.baeyens.arduino.ui;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;

/** The ArduinoSettingsPage class is linked to page in the import wizard.
 * It wraps around the ArduinPropertyPage
 * 
 * @author Jan Baeyens
 *
 */
public class ArduinoSettingsPage extends WizardPage implements IWizardPage {

	final Shell shell = new Shell();
	
	private ArduinoPageLayout mPageLayout= new ArduinoPageLayout();

	private Listener completeListener = new Listener() {
		@Override
		public void handleEvent(Event e) 
		{
			setPageComplete(mPageLayout.isPageComplete());
		}
	};

	public ArduinoSettingsPage(String pageName) {
		super(pageName);
		setPageComplete(false);
	}

	public ArduinoSettingsPage(String pageName, String title,
			ImageDescriptor titleImage) {
		super(pageName, title, titleImage);
		setPageComplete(false);
	}

	
@Override
	public void createControl(Composite parent)  
	{
		Composite composite = new Composite(parent, SWT.NULL);
		mPageLayout.draw(composite);
		setControl(composite);
		mPageLayout.feedbackControl.addListener(SWT.Modify, completeListener);
		setPageComplete(mPageLayout.isPageComplete());
	}

public void save(IProject project){
	mPageLayout.save(project);
}

public IPath getArduinoSourceCodeLocation()
{
	
	return mPageLayout.getArduinoSourceCodeLocation();
}

public String GetMCUName()
{
	return mPageLayout.getMCUName();
}

public ArduinoProperties GetProperties() {
	return mPageLayout.getProperties();
}

}
