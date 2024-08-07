package io.sloeber.autoBuild.integrations;

import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Text;

import io.sloeber.autoBuild.ui.internal.Messages;

public class NewProjectSourceLocationPage extends WizardPage {
	Button RootFolderButton ;
	Button srcFolderButton;
	Button customFolderButton;
	Text customFolderText;

	protected NewProjectSourceLocationPage(String pageName) {
		super(pageName);
		setTitle(Messages.NewProjectSourceLocationPage_SelectLocation);
		setDescription(Messages.NewProjectSourceLocationPage_SeperateSourceCodeFromTheRest);
	}

	@Override
	public void createControl(Composite parent) {

		Group usercomp = new Group(parent, SWT.NONE);
		setControl(usercomp);
		usercomp.setLayout(new RowLayout(SWT.VERTICAL));


		SelectionListener selectionListener=new SelectionListener() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				customFolderText.setEditable(false);
				setErrorMessage(null);
				setPageComplete(true);
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				//not needed
			}
		};

		RootFolderButton = new Button(usercomp, SWT.RADIO);
		RootFolderButton.setText(Messages.NewProjectSourceLocationPage_PutCodeInRootOfProject);
		RootFolderButton.addSelectionListener(selectionListener);

		srcFolderButton = new Button(usercomp, SWT.RADIO);
		srcFolderButton.setText(Messages.NewProjectSourceLocationPage_PutCodeInSrcFolder);
		srcFolderButton.setSelection(true);
		srcFolderButton.addSelectionListener(selectionListener);

		customFolderButton = new Button(usercomp, SWT.RADIO);
		customFolderButton.setText(Messages.NewProjectSourceLocationPage_PutCodeInSelectedFolder);
		customFolderButton.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				customFolderText.setEditable(true);
				if(customFolderText.getText().isBlank()) {
					setErrorMessage(Messages.NewProjectSourceLocationPage_YouMustProvideText);
					setPageComplete(false);
				}
			}
			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				//not needed
			}
		});
		customFolderText = new Text(usercomp, SWT.SINGLE);
		customFolderText.setEditable(false);
		customFolderText.addModifyListener(new ModifyListener() {

			@Override
			public void modifyText(ModifyEvent e) {
				if(customFolderText.getText().isBlank()) {
					setErrorMessage(Messages.NewProjectSourceLocationPage_YouMustProvideText);
					setPageComplete(false);
				}else {
					setErrorMessage(null);
					setPageComplete(true);
				}
			}
		});
	}


	public String getSourceCodeLocation() {
		if(RootFolderButton.getSelection()) {
			return null;
		}
		if(srcFolderButton.getSelection()) {
			return "src"; //$NON-NLS-1$
		}
		return customFolderText.getText();

	}

}
