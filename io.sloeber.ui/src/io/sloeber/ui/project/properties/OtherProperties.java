package io.sloeber.ui.project.properties;

import org.eclipse.cdt.core.settings.model.ICConfigurationDescription;
import org.eclipse.cdt.core.settings.model.ICResourceDescription;
import org.eclipse.cdt.ui.newui.AbstractCPropertyTab;
import org.eclipse.cdt.ui.newui.ICPropertyProvider;
import org.eclipse.core.resources.IProject;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;

import io.sloeber.core.api.OtherDescription;
import io.sloeber.core.api.SloeberProject;
import io.sloeber.ui.Messages;

public class OtherProperties extends AbstractCPropertyTab {

	private Button myOtherProperties;
	private OtherDescription myOtherOptions;

	@Override
	protected void performOK() {
		updateStorageData();
		ICConfigurationDescription confDesc = getConfdesc();
		if (confDesc != null) {
			IProject project = confDesc.getProjectDescription().getProject();
			SloeberProject sloeberProject = SloeberProject.getSloeberProject(project);
			sloeberProject.setOtherDescription(confDesc, myOtherOptions);
		}
		super.performOK();
	}



	@Override
	public void createControls(Composite parent, ICPropertyProvider provider) {
		super.createControls(parent, provider);
		ICConfigurationDescription confDesc = getConfdesc();
		IProject project = confDesc.getProjectDescription().getProject();
		SloeberProject sloeberProject = SloeberProject.getSloeberProject(project);
		myOtherOptions = sloeberProject.getOtherDescription(confDesc);
		if (myOtherOptions == null) {
			myOtherOptions = new OtherDescription();
		}
		GridLayout theGridLayout = new GridLayout();
		theGridLayout.numColumns = 2;
		this.usercomp.setLayout(theGridLayout);

		this.myOtherProperties = new Button(this.usercomp, SWT.CHECK);
		this.myOtherProperties.setText(Messages.ui_put_in_version_control);
		this.myOtherProperties.setEnabled(true);
		this.myOtherProperties.setLayoutData(new GridData(SWT.BEGINNING, SWT.BEGINNING, false, false, 2, 1));

		theGridLayout = new GridLayout();
		theGridLayout.numColumns = 2;
		this.usercomp.setLayout(theGridLayout);
		updateScreenData();
		setVisible(true);
	}

	private void updateScreenData() {
		this.myOtherProperties.setSelection(this.myOtherOptions.IsVersionControlled());
	}
	private void updateStorageData() {
		this.myOtherOptions.setVersionControlled(this.myOtherProperties.getSelection());
	}

	@Override
	protected void updateData(ICResourceDescription cfg) {
//		this.myCompileOptions = new CompileDescription(getConfdesc());
//		updateScreenData();
	}

	@Override
	public boolean canBeVisible() {
		return true;
	}

	@Override
	protected void updateButtons() {
		// nothing to do here

	}

	@Override
	protected void performApply(ICResourceDescription src, ICResourceDescription dst) {
		updateStorageData();
		if (dst.getConfiguration() != null) {
			// myOtherOptions.getEnvVars();
		}
	}

	@Override
	protected void performDefaults() {
		this.myOtherOptions = new OtherDescription();
		updateScreenData();
	}

	/**
	 * Get the configuration we are currently working in. The configuration is
	 * null if we are in the create sketch wizard.
	 *
	 * @return the configuration to save info into
	 */
	protected ICConfigurationDescription getConfdesc() {
		if (this.page != null) {
			return getResDesc().getConfiguration();
		}
		return null;
	}

}
