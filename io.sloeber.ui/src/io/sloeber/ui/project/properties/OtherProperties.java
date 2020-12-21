package io.sloeber.ui.project.properties;

import org.eclipse.cdt.core.settings.model.ICConfigurationDescription;
import org.eclipse.cdt.ui.newui.ICPropertyProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;

import io.sloeber.core.api.CompileDescription;
import io.sloeber.core.api.OtherDescription;
import io.sloeber.ui.Messages;

public class OtherProperties extends SloeberCpropertyTab {

	private Button myOtherProperties;



	@Override
	public void createControls(Composite parent, ICPropertyProvider provider) {
		super.createControls(parent, provider);

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
		updateScreen(getDescription(getConfdesc()));
		setVisible(true);
	}

	@Override
	protected String getQualifierString() {
		return "SloeberOtherDescription"; //$NON-NLS-1$
	}

	@Override
	protected void updateScreen(Object object) {
		OtherDescription otherDesc = (OtherDescription) object;
		myOtherProperties.setSelection(otherDesc.IsVersionControlled());
	}

	@Override
	protected Object getFromScreen() {
		OtherDescription otherDesc = new OtherDescription();
		otherDesc.setVersionControlled(this.myOtherProperties.getSelection());
		return otherDesc;
	}

	@Override
	protected Object getFromSloeber(ICConfigurationDescription confDesc) {
		return mySloeberProject.getOtherDescription(confDesc, true);

	}

	@Override
	protected Object makeCopy(Object srcObject) {
		return new OtherDescription((OtherDescription) srcObject);
	}

	@Override
	protected void updateSloeber(ICConfigurationDescription confDesc, Object theObjectToStore) {
		mySloeberProject.setOtherDescription(confDesc, (OtherDescription) theObjectToStore);

	}

	@Override
	protected Object getnewDefaultObject() {
		return new CompileDescription();
	}


}
