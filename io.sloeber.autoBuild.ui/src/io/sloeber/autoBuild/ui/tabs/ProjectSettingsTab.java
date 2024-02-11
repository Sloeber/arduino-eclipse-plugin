package io.sloeber.autoBuild.ui.tabs;

import org.eclipse.cdt.core.settings.model.ICResourceDescription;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import io.sloeber.autoBuild.integration.AutoBuildManager;
import io.sloeber.schema.api.IConfiguration;
import io.sloeber.schema.api.IProjectType;

/**
 * Project Settings Tab in project properties Build Settings This allows to
 * change the project type
 *
 * @noextend This class is not intended to be subclassed by clients.
 * @noinstantiate This class is not intended to be instantiated by clients.
 */
public class ProjectSettingsTab extends AbstractAutoBuildPropertyTab {
	private Combo myExtentionPointIDCombo = null;
	private Combo myExtentionIDCombo = null;
	private Combo myProjectsCombo = null;
	private Combo myConfigurationsCombo = null;

	private boolean isIndexerAffected = true;
	private boolean isUpdating = true;
	private String myExtensionPointID = null;
	private String myExtensionID = null;
	private String myProjectTypeName = null;
	private String myAutoBuildConfiguration = null;
	private DialogCompleteEvent myParentListener = null;
	private IProjectType myProjectType;

	public void internalCreateControls(Composite comp,DialogCompleteEvent parentListener) {
		myParentListener=parentListener;
		int comboStyle = SWT.LEAD | SWT.READ_ONLY | SWT.DROP_DOWN | SWT.BORDER;
		int labelSyle = SWT.LEAD;
		GridData controlGridData = new GridData(GridData.FILL_HORIZONTAL);
		controlGridData.horizontalSpan = 1;

		GridLayout gridlayout = new GridLayout();
		gridlayout.numColumns = 2;
		gridlayout.marginHeight = 5;
		comp.setLayout(gridlayout);

		Label label = new Label(comp, labelSyle);
		label.setText("Extension point ID");
		myExtentionPointIDCombo = new Combo(comp, comboStyle);

		label = new Label(comp, labelSyle);
		label.setText("Extension ID");
		myExtentionIDCombo = new Combo(comp, comboStyle);

		label = new Label(comp, labelSyle);
		label.setText("Project");
		myProjectsCombo = new Combo(comp, comboStyle);

		label = new Label(comp, labelSyle);
		label.setText("Configuration");
		myConfigurationsCombo = new Combo(comp, comboStyle);

		myExtentionPointIDCombo.setLayoutData(controlGridData);
		myExtentionIDCombo.setLayoutData(controlGridData);
		myProjectsCombo.setLayoutData(controlGridData);
		myConfigurationsCombo.setLayoutData(controlGridData);

		for (String extensionPointID : AutoBuildManager.supportedExtensionPointIDs()) {
			myExtentionPointIDCombo.add(extensionPointID);
		}


		myExtentionPointIDCombo.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent event) {
				if (isUpdating) {
					return;
				}
				myExtensionPointID = myExtentionPointIDCombo.getText();
				setValues();
			}
		});
		myExtentionIDCombo.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent event) {
				if (isUpdating) {
					return;
				}
				myExtensionID = myExtentionIDCombo.getText();
				setValues();
			}
		});
		myProjectsCombo.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent event) {
				if (isUpdating) {
					return;
				}
				myProjectTypeName = myProjectsCombo.getText();
				setValues();
			}
		});
		if(myParentListener!=null) {
			setValues();
		}
		isUpdating=false;
	}

	@Override
	public void createControls(Composite par) {
		super.createControls(par);
		internalCreateControls(usercomp, null);

	}

	@Override
	public void setVisible(boolean visible) {
		if (visible) {
			updateData(page.getResDesc());
		}
		super.setVisible(visible);
	}

	private String getComboValue(Combo combo, String preferredValue) {
		if(preferredValue!=null) {
		combo.setText(preferredValue);
		}
		String ret = combo.getText();
		if (ret.isBlank() && (combo.getItemCount() > 0)) {
			ret = combo.getItem(0);
			combo.setText(ret);
		}
		return ret;

	}

	private void setValues() {
		boolean orgIsUpdating = isUpdating;
		isUpdating = true;
		internalSetValues();
		isUpdating = orgIsUpdating;
	}

	private void internalSetValues() {
		myExtentionIDCombo.removeAll();
		myProjectsCombo.removeAll();
		myConfigurationsCombo.removeAll();
		// No need to update the supported extension point ID list as it is a hardcoded
		// list
		myExtensionPointID = getComboValue(myExtentionPointIDCombo, myExtensionPointID);
		if (myExtensionPointID.isBlank()) {
			return;
		}

		for (String extensionPointID : AutoBuildManager.getSupportedExtensionIDs(myExtensionPointID)) {
			myExtentionIDCombo.add(extensionPointID);
		}
		myExtensionID = getComboValue(myExtentionIDCombo, myExtensionID);
		if (myExtensionID.isBlank()) {
			return;
		}

		for (IProjectType projectType : AutoBuildManager.getProjectTypes(myExtensionPointID, myExtensionID)) {
			myProjectsCombo.add(projectType.getName());
			myProjectsCombo.setData(projectType.getName(), projectType);
		}
		myProjectTypeName = getComboValue(myProjectsCombo, myProjectTypeName);
		if (myProjectTypeName.isBlank()) {
			return;
		}
		myProjectType = (IProjectType) myProjectsCombo.getData(myProjectTypeName);

		if (myProjectType != null) {
			for (IConfiguration configuration : myProjectType.getConfigurations()) {
				myConfigurationsCombo.add(configuration.getName());
				myProjectsCombo.setData(configuration.getName(), configuration);
			}
			myAutoBuildConfiguration = getComboValue(myConfigurationsCombo, myAutoBuildConfiguration);
			if (!myAutoBuildConfiguration.isBlank()) {
				IConfiguration configuration = (IConfiguration) myProjectsCombo.getData(myAutoBuildConfiguration);
				if (myAutoConfDesc != null) {
					// we are in project properties update the project
					myAutoConfDesc.setModelConfiguration(configuration);
				}
			}
			
			if(myParentListener!=null) {
				//we are in project creation. Tell the parent we have a project selected
				myParentListener.completeEvent(!myAutoBuildConfiguration.isBlank());
			}

		}
	}

	private void copyAutoConfData() {
		myExtensionPointID = myAutoConfDesc.getExtensionPointID();
		myExtensionID = myAutoConfDesc.getExtensionID();
		myProjectTypeName = myAutoConfDesc.getProjectType().getName();
		myAutoBuildConfiguration = myAutoConfDesc.getAutoBuildConfiguration().getName();
	}

	@Override
	public void updateData(ICResourceDescription cfgd) {
		super.updateData(cfgd);
		copyAutoConfData();
		setValues();
	}

	@Override
	public void performApply(ICResourceDescription src, ICResourceDescription dst) {

		updateData(getResDesc());
	}

	@Override
	protected boolean isIndexerAffected() {
		return isIndexerAffected;
	}

	@Override
	protected void performDefaults() {
		// TODO Auto-generated method stub

	}

	@Override
	protected void updateButtons() {
		// TODO Auto-generated method stub

	}

	public IProjectType getProjectType() {
		return myProjectType;
	}

}
