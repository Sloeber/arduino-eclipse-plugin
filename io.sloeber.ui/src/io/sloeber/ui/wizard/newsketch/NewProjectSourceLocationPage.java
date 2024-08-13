package io.sloeber.ui.wizard.newsketch;

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

import io.sloeber.core.api.ConfigurationPreferences;
import io.sloeber.ui.Messages;

public class NewProjectSourceLocationPage extends WizardPage {
	private static String LAST_USED_SOURCE_LOCATION = "Last used Source Loaction"; //$NON-NLS-1$
	private static String DEFAULT_FOLDER = "src"; //$NON-NLS-1$
	Button RootFolderButton;
	Button srcFolderButton;
	Button customFolderButton;
	Text customFolderText;

	protected NewProjectSourceLocationPage(String pageName) {
		super(pageName);
		setTitle(Messages.ui_new_sketch_sketch_source_folder);
		setDescription(
				Messages.NewProjectSourceLocationPage_ExplainText);
	}

	@Override
	public void createControl(Composite parent) {

		Group usercomp = new Group(parent, SWT.NONE);
		setControl(usercomp);
		usercomp.setLayout(new RowLayout(SWT.VERTICAL));

		SelectionListener selectionListener = new SelectionListener() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				customFolderText.setEditable(false);
				setErrorMessage(null);
				setPageComplete(true);
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				// not needed
			}
		};

		srcFolderButton = new Button(usercomp, SWT.RADIO);
		srcFolderButton.setText(Messages.NewProjectSourceLocationPage_CodeInSrcFolder);
		srcFolderButton.addSelectionListener(selectionListener);

		RootFolderButton = new Button(usercomp, SWT.RADIO);
		RootFolderButton.setText(Messages.NewProjectSourceLocationPage_CodeInRootFolder);
		RootFolderButton.addSelectionListener(selectionListener);

		customFolderButton = new Button(usercomp, SWT.RADIO);
		customFolderButton.setText(Messages.NewProjectSourceLocationPage_CodeInCustomFolder);
		customFolderButton.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				customFolderText.setEditable(true);
				if (customFolderText.getText().isBlank()) {
					setErrorMessage(Messages.NewProjectSourceLocationPage_CustomSourceTextFieldError);
					setPageComplete(false);
				}
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				// not needed
			}
		});
		customFolderText = new Text(usercomp, SWT.SINGLE);
		customFolderText.setEditable(false);
		customFolderText.addModifyListener(new ModifyListener() {

			@Override
			public void modifyText(ModifyEvent e) {
				if (customFolderText.getText().isBlank()) {
					setErrorMessage(Messages.NewProjectSourceLocationPage_CustomSourceTextFieldError);
					setPageComplete(false);
				} else {
					setErrorMessage(null);
					setPageComplete(true);
				}
			}
		});

		String storedValue = ConfigurationPreferences.getString(LAST_USED_SOURCE_LOCATION, DEFAULT_FOLDER);
		if (DEFAULT_FOLDER.equals(storedValue)) {
			srcFolderButton.setSelection(true);
		} else {
			if (storedValue == null) {
				RootFolderButton.setSelection(true);
			} else {
				customFolderButton.setSelection(true);
				customFolderText.setText(storedValue);
			}
		}
	}

	public String getSourceCodeLocation() {
		String ret = customFolderText.getText();
		if (RootFolderButton.getSelection()) {
			ret = null;
		}
		if (srcFolderButton.getSelection()) {
			ret = DEFAULT_FOLDER;
		}
		ConfigurationPreferences.setString(LAST_USED_SOURCE_LOCATION, ret);
		return ret;
	}

}
