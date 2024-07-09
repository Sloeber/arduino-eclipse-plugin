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

import io.sloeber.autoBuild.buildTools.api.IBuildTools;
import io.sloeber.autoBuild.buildTools.api.IBuildToolsManager;
import io.sloeber.autoBuild.buildTools.api.IBuildToolsProvider;
import io.sloeber.autoBuild.ui.internal.Messages;

public class BuildToolManagerTab extends AbstractAutoBuildPropertyTab {
	private static final String NEWLINE = "\n"; //$NON-NLS-1$
	private static final String EQUAL = "="; //$NON-NLS-1$
	private IBuildToolsManager buildToolManager = IBuildToolsManager.getDefault();
	private boolean myIsUpdating = true;
	private Combo myToolProviderCombo;
	private Combo myBuildToolCombo;
	private String myToolProviderName;
	private Label myLabel;
	private Button myRefreshButton;
	DialogCompleteEvent myParentListener=null;

	public BuildToolManagerTab() {
		// nothing to do here
	}

	/**
	 *
	 * @param comp the composite to add the controls
	 * @param parentListener if not null the parent that needs to be notified if the page is
	 */
	public void internalCreateControls(Composite comp,DialogCompleteEvent parentListener) {
		myParentListener=parentListener;
		int comboStyle = SWT.LEAD | SWT.READ_ONLY | SWT.DROP_DOWN | SWT.BORDER;
		int labelSyle = SWT.LEAD;
		int buttonStyle=SWT.LEAD|SWT.PUSH;
		GridData controlGridData = new GridData(GridData.FILL_HORIZONTAL);
		controlGridData.horizontalSpan = 1;

		GridLayout gridlayout = new GridLayout();
		gridlayout.numColumns = 2;
		gridlayout.marginHeight = 5;
		comp.setLayout(gridlayout);

		Label label = new Label(comp, labelSyle);
		label.setText(Messages.BuildToolManagerTab_readFromDisk);
		myRefreshButton=new Button(comp, buttonStyle);
		myRefreshButton.setText(Messages.BuildToolManagerTab_refreshTheToolChains);


		label = new Label(comp, labelSyle);
		label.setText(Messages.BuildToolManagerTab_ToolProviders);
		myToolProviderCombo = new Combo(comp, comboStyle);

		label = new Label(comp, labelSyle);
		label.setText(Messages.BuildToolManagerTab_BuildTools);
		myBuildToolCombo = new Combo(comp, comboStyle);

		GridData labelGridData = new GridData(GridData.FILL_HORIZONTAL | GridData.FILL_VERTICAL);
		labelGridData.horizontalSpan = 2;
		myLabel = new Label(comp, SWT.LEAD | SWT.WRAP);
		myLabel.setLayoutData(labelGridData);

		myToolProviderCombo.setLayoutData(controlGridData);
		myBuildToolCombo.setLayoutData(controlGridData);

		for (IBuildToolsProvider buildToolProvider : buildToolManager.GetToolProviders(true)) {
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
					setLabelText();
				}
				if(myParentListener!=null) {
					myParentListener.completeEvent(buildTools!=null);
				}
			}
		});

		myRefreshButton.addSelectionListener(new SelectionListener() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				buildToolManager.refreshToolchains();
				String selected =myToolProviderCombo.getText();
				myToolProviderCombo.removeAll();
				for (IBuildToolsProvider buildToolProvider : buildToolManager.GetToolProviders(true)) {
					myToolProviderCombo.add(buildToolProvider.getName());
				}
				myToolProviderCombo.setText(selected);

			}

			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				// TODO Auto-generated method stub

			}
		});
		myIsUpdating = false;
		if(myParentListener!=null) {
			myToolProviderCombo.select(0);
			updateBuildToolsCombo();
			myParentListener.completeEvent(getSelecteddBuildTool()!=null);
		}

	}
	@Override
	public void createControls(Composite par) {
		super.createControls(par);
		internalCreateControls(usercomp,null);
	}

	@Override
	protected void performDefaults() {
		// TODO Auto-generated method stub

	}

	private void updateBuildToolsCombo() {
		myToolProviderName = myToolProviderCombo.getText();
		IBuildToolsProvider toolProvider = buildToolManager.GetToolProviderByName(myToolProviderName);
		myBuildToolCombo.removeAll();
		if (toolProvider != null && toolProvider.getAllInstalledBuildTools()!=null) {
			for (IBuildTools buildTools : toolProvider.getAllInstalledBuildTools()) {
				myBuildToolCombo.add(buildTools.getSelectionID());
				myBuildToolCombo.setData(buildTools.getSelectionID(), buildTools);
			}
		}
		myBuildToolCombo.select(0);
		IBuildTools buildTools = getSelecteddBuildTool();
		if(buildTools!=null &&myAutoConfDesc!=null) {
			myAutoConfDesc.setBuildTools(buildTools);
		}
		if(myParentListener!=null) {
			myParentListener.completeEvent(buildTools!=null);
		}
		setLabelText();
	}

	private void setLabelText() {
		IBuildTools buildTools=getSelecteddBuildTool();
		if (buildTools == null) {
			myLabel.setText(Messages.BuildToolManagerTab_NoBuildToolSelected);
			return;
		}
		String description = Messages.BuildToolManagerTab_ProviderID + buildTools.getProviderID() + NEWLINE;
		description = description + Messages.BuildToolManagerTab_MyID + buildTools.getSelectionID() + NEWLINE;
		description = description + Messages.BuildToolManagerTab_Path + buildTools.getToolLocation() + NEWLINE;
		description = description + Messages.BuildToolManagerTab_ToolFlavour + buildTools.getToolFlavour().toString() + NEWLINE;
		if (buildTools.getToolVariables() == null) {
			description = description + NEWLINE + Messages.BuildToolManagerTab_NoToolVarsProvided + NEWLINE;
		} else {
			description = description + NEWLINE + Messages.BuildToolManagerTab_ToolVars + NEWLINE;
			for (Entry<String, String> var : buildTools.getToolVariables().entrySet()) {
				description = description + var.getKey() + EQUAL + var.getValue() + NEWLINE;
			}
		}

		if (buildTools.getEnvironmentVariables() == null) {
			description = description + NEWLINE + Messages.BuildToolManagerTab_NoEnvironmentVarsProvided + NEWLINE;
		} else {
			description = description + NEWLINE + Messages.BuildToolManagerTab_EnvironmentVars + NEWLINE;
			for (Entry<String, String> var : buildTools.getEnvironmentVariables().entrySet()) {
				description = description + var.getKey() + EQUAL + var.getValue() + NEWLINE;
			}
		}
		myLabel.setText(description);
	}

	public IBuildTools getSelecteddBuildTool() {
		return(IBuildTools)myBuildToolCombo.getData(myBuildToolCombo.getText());
	}

	@Override
	protected void updateButtons() {
		if(myIsUpdating) {
			//we are in new project wizard
			return;
		}
		IBuildTools buildTools = myAutoConfDesc.getBuildTools();
		if (buildTools != null) {
			String providerID = buildTools.getProviderID();
			IBuildToolsProvider buildToolProvider = buildToolManager.getToolProvider(providerID);
			if (buildToolProvider != null) {
				int index = myToolProviderCombo.indexOf(buildToolProvider.getName());
				myToolProviderCombo.select(index);
				updateBuildToolsCombo();
			}
			String id = buildTools.getSelectionID();
			int index = myBuildToolCombo.indexOf(id);
			myBuildToolCombo.select(index);
			setLabelText();
		}
	}

//	public Control getControl() {
//		return myToolProviderCombo;
//	}

}
