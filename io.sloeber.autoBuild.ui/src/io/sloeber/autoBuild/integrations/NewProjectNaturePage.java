package io.sloeber.autoBuild.integrations;

import org.eclipse.cdt.core.CCProjectNature;
import org.eclipse.cdt.core.CProjectNature;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;

import io.sloeber.autoBuild.ui.internal.Messages;

public class NewProjectNaturePage extends WizardPage{
	private String myNatureId=CCProjectNature.CC_NATURE_ID;

	protected NewProjectNaturePage(String pageName) {
		super(pageName);
		setTitle(Messages.NewProjectNaturePage_SelectNaturesTitle);
		setDescription(Messages.NewProjectNaturePage_SelectNaturesDescription);
	}

	@Override
	public void createControl(Composite parent) {
		// Create a group to contain 2 radio (Male & Female)
		Group natureGroup = new Group(parent, SWT.NONE);
		natureGroup.setLayout(new RowLayout(SWT.VERTICAL));



		Button cppNatureButton = new Button(natureGroup, SWT.RADIO);
		cppNatureButton.setText(Messages.NewProjectNaturePage_CPPNature);
		cppNatureButton.setSelection(true);
		cppNatureButton.addSelectionListener(new SelectionListener() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				myNatureId=CCProjectNature.CC_NATURE_ID;

			}

			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				// TODO Auto-generated method stub

			}
		});
		Button cNatureButton = new Button(natureGroup, SWT.RADIO);
		cNatureButton.setText(Messages.NewProjectNaturePage_CNature);
		cNatureButton.addSelectionListener(new SelectionListener() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				myNatureId=CProjectNature.C_NATURE_ID;

			}

			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				// TODO Auto-generated method stub

			}
		});

		setControl(natureGroup);
	}

	public String getNatureID() {
		return myNatureId;
	}

}
