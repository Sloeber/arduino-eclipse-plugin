package io.sloeber.autoBuild.integrations;

import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;

import io.sloeber.autoBuild.buildTools.api.IBuildTools;
import io.sloeber.autoBuild.ui.internal.Messages;
import io.sloeber.autoBuild.ui.tabs.BuildToolManagerTab;
import io.sloeber.autoBuild.ui.tabs.DialogCompleteEvent;

public class NewProjectBuildToolsPage extends  WizardPage {


	private BuildToolManagerTab myBuildToolsManagerTab;

	protected NewProjectBuildToolsPage(String pageName) {
		super(pageName);
		myBuildToolsManagerTab =new BuildToolManagerTab();
		setTitle(Messages.NewProjectBuildToolsPage_title);
		setDescription(Messages.NewProjectBuildToolsPage_description);
		setPageComplete(false);
	}

	@Override
	public void createControl(Composite parent) {
		Composite usercomp = new Composite(parent, SWT.NONE);
		usercomp.setLayout(new GridLayout());
		usercomp.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_FILL));
		setControl(usercomp);
		myBuildToolsManagerTab.internalCreateControls(usercomp, new DialogCompleteEvent(){
			@Override
			public void completeEvent(boolean isComplete) {
				setPageComplete(isComplete);
			}
		});
	}

	public IBuildTools getBuildTools() {
		return myBuildToolsManagerTab.getSelecteddBuildTool();
	}

}
