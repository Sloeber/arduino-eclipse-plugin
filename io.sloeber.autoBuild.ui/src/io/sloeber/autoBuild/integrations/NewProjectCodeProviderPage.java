package io.sloeber.autoBuild.integrations;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import org.eclipse.jface.wizard.IWizard;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import io.sloeber.autoBuild.api.AutoBuildNewProjectCodeManager;
import io.sloeber.autoBuild.api.ICodeProvider;
import io.sloeber.autoBuild.ui.internal.Messages;

public class NewProjectCodeProviderPage extends WizardPage {
	private static final String EMPTY=""; //$NON-NLS-1$
	private Group myButtonComp;
	private Button myNoCodeButton;
	private List<Button> myToolProviderButtons = new ArrayList<>();
	private AutoBuildNewProjectCodeManager codeManager = AutoBuildNewProjectCodeManager.getDefault();
	private SelectionListener selectionListener;
	private String mySelectedProjectTarget;
	private String mySelectedNatureID;
	private Composite myParent;
	private ICodeProvider myCodeProvider = null;
	private Label myDescriptionText;


	@Override
	public void setVisible(boolean visible) {
		showCorrectCodeProviderButtons();
		super.setVisible(visible);
	}


	protected NewProjectCodeProviderPage(String pageName) {
		super(pageName);
		setTitle(Messages.NewProjectCodeProviderPage_title);
		setDescription(Messages.NewProjectCodeProviderPage_description);
	}

	@Override
	public void createControl(Composite parent) {
		myParent=parent;
		Group usercomp = new Group(parent, SWT.NONE);
		setControl(usercomp);
		// usercomp.setLayout(new RowLayout(SWT.VERTICAL));
		GridLayout gridlayout = new GridLayout();
		gridlayout.numColumns = 1;
		gridlayout.marginHeight = 5;
		usercomp.setLayout(gridlayout);

		selectionListener = new SelectionListener() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				setErrorMessage(null);
				setPageComplete(true);
				Button button = (Button) e.getSource();
				ICodeProvider codeProvider = (ICodeProvider) button.getData();
				String description = EMPTY;
				if (codeProvider == null) {
					if (button.equals(myNoCodeButton)) {
						description = Messages.NewProjectCodeProviderPage_noFilesDescription;
					}
				} else {
					description = codeProvider.getDescription();
					myCodeProvider=codeProvider;
				}
				myDescriptionText.setText(description);
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				// not needed
			}
		};

		myButtonComp = new Group(usercomp, SWT.NONE);
		myButtonComp.setLayout(new RowLayout(SWT.VERTICAL));
		GridData buttonGridData = new GridData(GridData.FILL_HORIZONTAL );
		myButtonComp.setLayoutData(buttonGridData);

		myNoCodeButton = new Button(myButtonComp, SWT.RADIO);
		myNoCodeButton.setText(Messages.NewProjectCodeProviderPage_noFilesName);
		myNoCodeButton.addSelectionListener(selectionListener);

		GridData labelGridData = new GridData(GridData.FILL_HORIZONTAL | GridData.FILL_VERTICAL);
		labelGridData.horizontalSpan = 2;
		myDescriptionText = new Label(usercomp, SWT.LEAD | SWT.WRAP);
		myDescriptionText.setLayoutData(labelGridData);

		showCorrectCodeProviderButtons();

	}

	public ICodeProvider getCodeProvider() {
		return myCodeProvider;
	}

	private void showCorrectCodeProviderButtons() {
		IWizard wizard = getWizard();
		if (wizard instanceof NewProjectWizard) {
			NewProjectWizard newProjectWizard = (NewProjectWizard) wizard;
			mySelectedProjectTarget = newProjectWizard.getSelectedBuildArtifactType();
			mySelectedNatureID = newProjectWizard.getSelectedNatureID();
		}
		for (Button curButton : myToolProviderButtons) {
			curButton.setText(EMPTY);
			curButton.setVisible(false);
		}
		int curButtonCounter = 0;
		//System.out.println("recalculating code on target "+mySelectedProjectTarget+" for nature "+mySelectedNatureID);
		HashMap<String, ICodeProvider> codeProviders = codeManager.getCodeProviders(mySelectedProjectTarget,
				mySelectedNatureID);
		boolean needsRedraw = false;
		for (Entry<String, ICodeProvider> curCodeProviderEntry : codeProviders.entrySet()) {

			ICodeProvider codeProvider = curCodeProviderEntry.getValue();
			String name = codeProvider.getName();
			Button curButton;
			if (myToolProviderButtons.size() > curButtonCounter) {
				curButton = myToolProviderButtons.get(curButtonCounter);
				curButtonCounter++;
			} else {
				curButton = new Button(myButtonComp, SWT.RADIO);
				curButton.addSelectionListener(selectionListener);
				myToolProviderButtons.add(curButton);
				needsRedraw = true;
			}
			curButton.setText(name);
			curButton.setData(codeProvider);
			curButton.setVisible(true);
			curButton.pack();

		}
		if (needsRedraw) {

			myParent.pack(true);
			myButtonComp.pack(true);
			myParent.layout(true, true);
			myParent.redraw();
		}
	}

}
