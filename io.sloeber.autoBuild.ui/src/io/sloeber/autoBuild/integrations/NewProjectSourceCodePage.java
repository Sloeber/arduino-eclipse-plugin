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

public class NewProjectSourceCodePage extends WizardPage {
	Button RootFolderButton ;
	Button srcFolderButton;
	Button customFolderButton;
	Text customFolderText;

	protected NewProjectSourceCodePage(String pageName) {
		super(pageName);
		setTitle("select the location of the source code in the project.");
		setDescription("You can seperate the source code from the rest of the project content by putting it in a source folder.");
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
		RootFolderButton.setText("Put Code in the root of the project");
		RootFolderButton.addSelectionListener(selectionListener);

		srcFolderButton = new Button(usercomp, SWT.RADIO);
		srcFolderButton.setText("put code in the src folder");
		srcFolderButton.setSelection(true);
		srcFolderButton.addSelectionListener(selectionListener);
		
		customFolderButton = new Button(usercomp, SWT.RADIO);
		customFolderButton.setText("put code in a custom folder");
		customFolderButton.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				customFolderText.setEditable(true);
				if(customFolderText.getText().isBlank()) {
					setErrorMessage("You must provide a text in the text field that matches a valid folder name");
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
					setErrorMessage("You must provide a text in the text field that matches a valid folder name");
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
			return "src";
		}
		return customFolderText.getText();
		
	}

}
