package io.sloeber.autoBuild.ui.tabs;

import java.util.Map.Entry;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

import io.sloeber.buildTool.api.IBuildToolManager;
import io.sloeber.buildTool.api.IBuildToolProvider;
import io.sloeber.buildTool.api.IBuildTools;

public class BuildToolManagerTab extends AbstractAutoBuildPropertyTab {
	private static final String NEWLINE = "\n"; //$NON-NLS-1$
	private IBuildToolManager buildToolManager = IBuildToolManager.getDefault();
	private boolean myIsUpdating = true;
	private Combo myToolProviderCombo;
	private Combo myBuildToolCombo;
//	private Combo myToolFlavourCombo;
	private String myToolProviderName;
	private Label myLabel;
	private Button myRefreshButton;

	public BuildToolManagerTab() {
		// TODO Auto-generated constructor stub
	}

	@Override
	public void createControls(Composite par) {
		super.createControls(par);
		int comboStyle = SWT.LEAD | SWT.READ_ONLY | SWT.DROP_DOWN | SWT.BORDER;
		int labelSyle = SWT.LEAD;
		int buttonStyle=SWT.LEAD|SWT.PUSH;
		GridData controlGridData = new GridData(GridData.FILL_HORIZONTAL);
		controlGridData.horizontalSpan = 1;

		GridLayout gridlayout = new GridLayout();
		gridlayout.numColumns = 2;
		gridlayout.marginHeight = 5;
		usercomp.setLayout(gridlayout);

//		Label label = new Label(usercomp, labelSyle);
//		label.setText("Tool flavour filter");
//		myToolFlavourCombo = new Combo(usercomp, comboStyle);
		Label label = new Label(usercomp, labelSyle);
		label.setText("Reread from disk");
		myRefreshButton=new Button(usercomp, buttonStyle);
		myRefreshButton.setText("Refresh the toolchains");
		myRefreshButton.addSelectionListener(new SelectionListener() {
			
			@Override
			public void widgetSelected(SelectionEvent e) {
				buildToolManager.refreshToolchains();
				String selected =myToolProviderCombo.getText();
				myToolProviderCombo.removeAll();
				for (IBuildToolProvider buildToolProvider : buildToolManager.GetToolProviders(true)) {
					myToolProviderCombo.add(buildToolProvider.getName());
				}
				myToolProviderCombo.setText(selected);
				
			}
			
			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				// TODO Auto-generated method stub
				
			}
		});

		label = new Label(usercomp, labelSyle);
		label.setText("Tool providers");
		myToolProviderCombo = new Combo(usercomp, comboStyle);

		label = new Label(usercomp, labelSyle);
		label.setText("Build tools");
		myBuildToolCombo = new Combo(usercomp, comboStyle);

		GridData labelGridData = new GridData(GridData.FILL_HORIZONTAL | GridData.FILL_VERTICAL);
		labelGridData.horizontalSpan = 2;
		myLabel = new Label(usercomp, SWT.LEAD | SWT.WRAP);
		myLabel.setLayoutData(labelGridData);

		myToolProviderCombo.setLayoutData(controlGridData);
		myBuildToolCombo.setLayoutData(controlGridData);
//		myToolFlavourCombo.setLayoutData(controlGridData);

		for (IBuildToolProvider buildToolProvider : buildToolManager.GetToolProviders(true)) {
			myToolProviderCombo.add(buildToolProvider.getName());
		}

		myToolProviderCombo.addSelectionListener(new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent event) {
				if (myIsUpdating) {
					return;
				}
				updateBuildToolsCombo();
			}

		});
		myBuildToolCombo.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent event) {
				if (myIsUpdating) {
					return;
				}
				IBuildTools buildTools=getSelecteddBuildTool();
				if(buildTools!=null &&myAutoConfDesc!=null) {
					myAutoConfDesc.setBuildTools(buildTools);
					setLabelText(buildTools);
				}
			}
		});
		myIsUpdating = false;
	}

	@Override
	protected void performDefaults() {
		// TODO Auto-generated method stub

	}

	private void updateBuildToolsCombo() {
		myToolProviderName = myToolProviderCombo.getText();
		IBuildToolProvider toolProvider = buildToolManager.GetToolProviderByName(myToolProviderName);
		myBuildToolCombo.removeAll();
		if (toolProvider != null && toolProvider.getAllInstalledBuildTools()!=null) {
			for (IBuildTools buildTools : toolProvider.getAllInstalledBuildTools()) {
				myBuildToolCombo.add(buildTools.getSelectionID());
			}
		}
		myBuildToolCombo.select(0);
		IBuildTools buildTools = getSelecteddBuildTool();
		if(buildTools!=null &&myAutoConfDesc!=null) {
			myAutoConfDesc.setBuildTools(buildTools);
		}
		setLabelText(buildTools);
	}

	private void setLabelText(IBuildTools buildTools) {
		if (buildTools == null) {
			myLabel.setText("No build tool selected");
			return;
		}
		String description = "provider ID " + buildTools.getProviderID() + NEWLINE;
		description = description + "my ID " + buildTools.getSelectionID() + NEWLINE;
		description = description + "Path " + buildTools.getToolLocation() + NEWLINE;
		description = description + "Tool flavour " + buildTools.getToolFlavour().toString() + NEWLINE;
		if (buildTools.getToolVariables() == null) {
			description = description + NEWLINE + "No Tool Vars provided" + NEWLINE;
		} else {
			description = description + NEWLINE + "Tool Vars: " + NEWLINE;
			for (Entry<String, String> var : buildTools.getToolVariables().entrySet()) {
				description = description + var.getKey() + "=" + var.getValue() + NEWLINE;
			}
		}

		if (buildTools.getEnvironmentVariables() == null) {
			description = description + NEWLINE + "No environment Vars provided" + NEWLINE;
		} else {
			description = description + NEWLINE + "Environment Vars: " + NEWLINE;
			for (Entry<String, String> var : buildTools.getEnvironmentVariables().entrySet()) {
				description = description + var.getKey() + "=" + var.getValue() + NEWLINE;
			}
		}
		myLabel.setText(description);
	}

	private IBuildTools getSelecteddBuildTool() {
		myToolProviderName = myToolProviderCombo.getText();
		IBuildToolProvider toolProvider = buildToolManager.GetToolProviderByName(myToolProviderName);
		if (toolProvider == null) {
			return null;
		}
		return toolProvider.getTargetTool(myBuildToolCombo.getText());
	}

	@Override
	protected void updateButtons() {
		IBuildTools buildTools = myAutoConfDesc.getBuildTools();
		if (buildTools != null) {
			String providerID = buildTools.getProviderID();
			IBuildToolProvider buildToolProvider = buildToolManager.getToolProvider(providerID);
			if (buildToolProvider != null) {
				int index = myToolProviderCombo.indexOf(buildToolProvider.getName());
				myToolProviderCombo.select(index);
				updateBuildToolsCombo();
			}
			String id = buildTools.getSelectionID();
			int index = myBuildToolCombo.indexOf(id);
			myBuildToolCombo.select(index);
			setLabelText(buildTools);
		}
	}

}
