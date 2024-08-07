package io.sloeber.autoBuild.integrations;

import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;

import io.sloeber.autoBuild.schema.api.IProjectType;
import io.sloeber.autoBuild.ui.internal.Messages;
import io.sloeber.autoBuild.ui.tabs.DialogCompleteEvent;
import io.sloeber.autoBuild.ui.tabs.ProjectSettingsTab;

public class NewProjectProjectTypePage extends WizardPage {
	private ProjectSettingsTab myProjectSettingsTab;

	protected NewProjectProjectTypePage(String pageName) {
		super(pageName);
		myProjectSettingsTab=new ProjectSettingsTab();
		setTitle(Messages.NewProjectProjectTypePage_SelectProjectType);
		setDescription(Messages.NewProjectProjectTypePage_SelectProjectTypeDescription);
		setPageComplete(false);
	}

	@Override
	public void createControl(Composite parent) {
		Composite usercomp = new Composite(parent, SWT.NONE);
		usercomp.setLayout(new GridLayout());
		usercomp.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_FILL));
		setControl(usercomp);
		myProjectSettingsTab.internalCreateControls(usercomp, new DialogCompleteEvent(){
			@Override
			public void completeEvent(boolean isComplete) {
				setPageComplete(isComplete);
			}
		});
	}


	public IProjectType getProjectType() {
		return myProjectSettingsTab.getProjectType();
	}
}
