package io.sloeber.ui.wizard.newsketch;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;

import io.sloeber.arduinoFramework.api.BoardDescription;
import io.sloeber.ui.project.properties.BoardSelectionPage;

/**
 * The ArduinoSettingsPage class is linked to page in the import wizard. It
 * wraps around the ArduinPropertyPage
 * 
 * @author Jan Baeyens
 * 
 */
public class NewSketchWizardBoardPage extends WizardPage {

	final Shell shell = new Shell();

	protected BoardSelectionPage mPageLayout = new BoardSelectionPage();

	private Listener completeListener = new Listener() {
		@Override
		public void handleEvent(Event e) {
			setPageComplete(e.doit);
		}
	};

	public NewSketchWizardBoardPage(String pageName) {
		super(pageName);
		setPageComplete(false);
	}

	public NewSketchWizardBoardPage(String pageName, String title, ImageDescriptor titleImage) {
		super(pageName, title, titleImage);
		setPageComplete(false);
	}

	@Override
	public void createControl(Composite parent) {
		Composite composite = new Composite(parent, SWT.NULL);
		this.mPageLayout.addListener(completeListener);
		this.mPageLayout.draw(composite);
		setControl(composite);

	}

	public BoardDescription getBoardDescriptor() {
		return this.mPageLayout.getBoardDescription();
	}

}
