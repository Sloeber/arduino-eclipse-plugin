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
 * Project Settings Tab in project properties Build Settings
 * This allows to change the project type
 *
 * @noextend This class is not intended to be subclassed by clients.
 * @noinstantiate This class is not intended to be instantiated by clients.
 */
public class ProjectSettingsTab extends AbstractAutoBuildPropertyTab {

    /*
     * Dialog widgets
     */
    private Combo myExtentionPointIDCombo;
    private Combo myExtentionIDCombo;
    private Combo myProjectsCombo;
    private Combo myConfigurationsCombo;
    private boolean isIndexerAffected = true;
    private boolean isUpdating = false;
    private String myExtensionPointID;
    private String myExtensionID;
    private String myProjectType;
    private String myAutoBuildConfiguration;

    @Override
    public void createControls(Composite par) {
        super.createControls(par);
        int comboStyle = SWT.LEAD | SWT.READ_ONLY | SWT.DROP_DOWN | SWT.BORDER;
        int labelSyle = SWT.LEAD;
        GridData controlGridData = new GridData(GridData.FILL_HORIZONTAL);
        controlGridData.horizontalSpan = 1;

        GridLayout gridlayout = new GridLayout();
        gridlayout.numColumns = 2;
        gridlayout.marginHeight = 5;
        usercomp.setLayout(gridlayout);

        Label label = new Label(usercomp, labelSyle);
        label.setText("Extension point ID");
        myExtentionPointIDCombo = new Combo(usercomp, comboStyle);

        label = new Label(usercomp, labelSyle);
        label.setText("Extension ID");
        myExtentionIDCombo = new Combo(usercomp, comboStyle);

        label = new Label(usercomp, labelSyle);
        label.setText("Project");
        myProjectsCombo = new Combo(usercomp, comboStyle);

        label = new Label(usercomp, labelSyle);
        label.setText("Configuration");
        myConfigurationsCombo = new Combo(usercomp, comboStyle);

        myExtentionPointIDCombo.setLayoutData(controlGridData);
        myExtentionIDCombo.setLayoutData(controlGridData);
        myProjectsCombo.setLayoutData(controlGridData);
        myConfigurationsCombo.setLayoutData(controlGridData);

        for (String extensionPointID : AutoBuildManager.supportedExtensionPointIDs()) {
            myExtentionPointIDCombo.add(extensionPointID);
        }

        //        copyAutoConfData();
        //        setValues();

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
                myProjectType = myProjectsCombo.getText();
                setValues();
            }
        });

    }

    @Override
    public void setVisible(boolean visible) {
        if (visible) {
            updateData(page.getResDesc());
        }
        super.setVisible(visible);
    }

    private String getComboValue(Combo combo, String preferredValue) {
        combo.setText(preferredValue);
        String ret = combo.getText();
        if (ret.isBlank() && (combo.getItemCount() > 0)) {
            ret = combo.getItem(0);
            combo.setText(ret);
        }
        return ret;

    }

    protected void setValues() {
        if (myAutoConfDesc == null) {
            return;
        }
        boolean orgIsUpdating = isUpdating;
        isUpdating = true;
        internalSetValues();
        isUpdating = orgIsUpdating;
    }

    private void internalSetValues() {
        myExtentionIDCombo.removeAll();
        myProjectsCombo.removeAll();
        myConfigurationsCombo.removeAll();
        //No need to update the supported extension point ID list as it is a hardcoded list
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
        myProjectType = getComboValue(myProjectsCombo, myProjectType);
        if (myProjectType.isBlank()) {
            return;
        }
        IProjectType projectType = (IProjectType) myProjectsCombo.getData(myProjectType);

        if (projectType != null) {
            for (IConfiguration configuration : projectType.getConfigurations()) {
                myConfigurationsCombo.add(configuration.getName());
                myProjectsCombo.setData(configuration.getName(), configuration);
            }
            myAutoBuildConfiguration = getComboValue(myConfigurationsCombo, myAutoBuildConfiguration);
            if (!myAutoBuildConfiguration.isBlank()) {
                IConfiguration configuration = (IConfiguration) myProjectsCombo.getData(myAutoBuildConfiguration);
                myAutoConfDesc.setModelConfiguration(configuration);
            }
        }
    }

    private void copyAutoConfData() {
        myExtensionPointID = myAutoConfDesc.getExtensionPointID();
        myExtensionID = myAutoConfDesc.getExtensionID();
        myProjectType = myAutoConfDesc.getProjectType().getName();
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

}
