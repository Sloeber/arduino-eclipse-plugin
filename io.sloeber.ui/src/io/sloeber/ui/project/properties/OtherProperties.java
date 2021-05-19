package io.sloeber.ui.project.properties;

import org.eclipse.cdt.core.settings.model.ICConfigurationDescription;
import org.eclipse.cdt.ui.newui.ICPropertyProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;

import io.sloeber.core.api.OtherDescription;
import io.sloeber.ui.Messages;

public class OtherProperties extends SloeberCpropertyTab {

	private Button myOtherProperties;

	private Listener buttonListener = new Listener() {
		@Override
		public void handleEvent(Event e) {
			switch (e.type) {
			case SWT.Selection:
				getFromScreen();
				break;
			}
		}
	};


	@Override
	public void createControls(Composite parent, ICPropertyProvider provider) {
		super.createControls(parent, provider);

		GridLayout theGridLayout = new GridLayout();
		theGridLayout.numColumns = 2;
		usercomp.setLayout(theGridLayout);

		myOtherProperties = new Button(this.usercomp, SWT.CHECK);
		myOtherProperties.setText(Messages.ui_put_in_version_control);
		myOtherProperties.setEnabled(true);
		myOtherProperties.setLayoutData(new GridData(SWT.BEGINNING, SWT.BEGINNING, false, false, 2, 1));
		myOtherProperties.addListener(SWT.Selection, buttonListener);

		theGridLayout = new GridLayout();
		theGridLayout.numColumns = 2;
		this.usercomp.setLayout(theGridLayout);
		updateScreen();
	}

	@Override
	protected String getQualifierString() {
		return "SloeberOtherDescription"; //$NON-NLS-1$
	}

	@Override
	protected void updateScreen() {
		OtherDescription otherDesc = (OtherDescription) getDescription(getConfdesc());
		myOtherProperties.setSelection(otherDesc.IsVersionControlled());
	}

	@Override
	protected Object getFromScreen() {
		OtherDescription otherDesc = (OtherDescription) getDescription(getConfdesc());
		otherDesc.setVersionControlled(myOtherProperties.getSelection());
		return otherDesc;
	}

	@Override
	protected Object getFromSloeber(ICConfigurationDescription confDesc) {
		return mySloeberProject.getOtherDescription(confDesc.getName(), true);

	}

	@Override
	protected Object makeCopy(Object srcObject) {
		return new OtherDescription((OtherDescription) srcObject);
	}

	@Override
	protected void updateSloeber(ICConfigurationDescription confDesc) {
		OtherDescription theObjectToStore = (OtherDescription) getDescription(confDesc);
		mySloeberProject.setOtherDescription(confDesc.getName(), theObjectToStore);

	}

	@Override
	protected Object getnewDefaultObject() {
		return new OtherDescription();
	}


}
