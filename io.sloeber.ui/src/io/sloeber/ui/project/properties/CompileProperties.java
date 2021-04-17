package io.sloeber.ui.project.properties;

import org.eclipse.cdt.core.settings.model.ICConfigurationDescription;
import org.eclipse.cdt.ui.newui.ICPropertyProvider;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import io.sloeber.core.api.CompileDescription;
import io.sloeber.core.api.CompileDescription.WarningLevels;
import io.sloeber.ui.Messages;

public class CompileProperties extends SloeberCpropertyTab {
	private ComboViewer myWarningLevel;
	private Text myCustomWarningLevel;
	private Button mySizeCommand;
	private Text myCAndCppCommand;
	private Text myCppCommand;
	private Text myCCommand;
	private Text myAllCommand;
	private Text myArchiveCommand;
	private Text myAssemblyCommand;
	private Text myLinkCommand;

	private static void createLine(Composite parent, int ncol) {
		Label line = new Label(parent, SWT.SEPARATOR | SWT.HORIZONTAL | SWT.BOLD);
		GridData gridData = new GridData(GridData.FILL_HORIZONTAL);
		gridData.horizontalSpan = ncol;
		line.setLayoutData(gridData);
	}

	private Text makeOptionField(String labelText, String toolTipText) {
		// edit field add to C & C++ command line
		Label label = new Label(this.usercomp, SWT.LEFT);
		label.setText(labelText);
		label.setLayoutData(new GridData(SWT.BEGINNING, SWT.BEGINNING, false, false, 1, 2));
		Text textField = new Text(this.usercomp, SWT.MULTI | SWT.WRAP | SWT.V_SCROLL | SWT.BORDER);
		textField.setToolTipText(toolTipText);
		textField.setEnabled(true);
		GridData gridData = new GridData(GridData.FILL_HORIZONTAL);
		gridData.horizontalSpan = 2;
		gridData.verticalSpan = 2;
		textField.setLayoutData(gridData);
		return textField;
	}

	@Override
	public void createControls(Composite parent, ICPropertyProvider provider) {
		super.createControls(parent, provider);

		GridLayout theGridLayout = new GridLayout();
		theGridLayout.numColumns = 3;
		usercomp.setLayout(theGridLayout);

		// combobox to select warning level and optional custom values
		Label label = new Label(usercomp, SWT.LEFT);
		label.setText(Messages.ui_show_all_warnings);

		myWarningLevel = new ComboViewer(usercomp, SWT.READ_ONLY);
		myWarningLevel.setContentProvider(ArrayContentProvider.getInstance());
		myWarningLevel.setInput(WarningLevels.values());
		myWarningLevel.addSelectionChangedListener(new ISelectionChangedListener() {

			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				ISelection selection = event.getSelection();
				if (selection instanceof IStructuredSelection) {
					WarningLevels newSelection = (WarningLevels) ((IStructuredSelection) selection).getFirstElement();
					myCustomWarningLevel.setEnabled(newSelection == WarningLevels.CUSTOM);
					myCustomWarningLevel.setText(newSelection.getEnvValue());
				}
			}
		});

		myCustomWarningLevel = new Text(usercomp, SWT.BORDER | SWT.LEFT);
		GridData gridData = new GridData(GridData.FILL_HORIZONTAL);
		myCustomWarningLevel.setLayoutData(gridData);

		// checkbox show alternative size
		this.mySizeCommand = new Button(this.usercomp, SWT.CHECK);
		this.mySizeCommand.setText(Messages.ui_Alternative_size);
		this.mySizeCommand.setEnabled(true);
		this.mySizeCommand.setLayoutData(new GridData(SWT.BEGINNING, SWT.BEGINNING, false, false, 3, 1));

		createLine(this.usercomp, 3);
		this.myCAndCppCommand = makeOptionField(Messages.ui_append_c_cpp, Messages.ui_append_c_cpp_text);
		this.myCppCommand = makeOptionField(Messages.ui_append_cpp, Messages.ui_append_cpp_text);
		this.myCCommand = makeOptionField(Messages.ui_append_c, Messages.ui_append_c_text);
		this.myAssemblyCommand = makeOptionField(Messages.ui_append_assembly, Messages.ui_append_assembly_text);
		this.myArchiveCommand = makeOptionField(Messages.ui_append_archive, Messages.ui_append_archive_text);
		this.myLinkCommand = makeOptionField(Messages.ui_append_link, Messages.ui_append_link_text);
		this.myAllCommand = makeOptionField(Messages.ui_append_all, Messages.ui_append_all_text);

		updateScreen(getDescription(getConfdesc()));
		setVisible(true);
	}

	@Override
	protected String getQualifierString() {
		return "SloeberCompileProperties"; //$NON-NLS-1$
	}

	@Override
	protected void updateScreen(Object object) {
		CompileDescription compDesc = (CompileDescription) object;
		final ISelection selection = new StructuredSelection(compDesc.getWarningLevel());
		myWarningLevel.setSelection(selection);
		myCustomWarningLevel.setEnabled(compDesc.getWarningLevel() == WarningLevels.CUSTOM);
		myCustomWarningLevel.setText(compDesc.getWarningLevel().getEnvValue());
		this.mySizeCommand.setSelection(compDesc.isAlternativeSizeCommand());
		this.myCAndCppCommand.setText(compDesc.get_C_andCPP_CompileOptions());
		this.myCCommand.setText(compDesc.get_C_CompileOptions());
		this.myCppCommand.setText(compDesc.get_CPP_CompileOptions());
		this.myAllCommand.setText(compDesc.get_All_CompileOptions());
		this.myArchiveCommand.setText(compDesc.get_Archive_CompileOptions());
		this.myAssemblyCommand.setText(compDesc.get_Assembly_CompileOptions());
		this.myLinkCommand.setText(compDesc.get_Link_CompileOptions());

	}

	@Override
	protected Object getFromScreen() {
		CompileDescription compDesc = mySloeberProject.getCompileDescription(getConfdesc().getName(), true);
		WarningLevels warningLevel = WarningLevels.valueOf(myWarningLevel.getCombo().getText());
		warningLevel.setCustomWarningLevel(myCustomWarningLevel.getText());
		compDesc.setWarningLevel(warningLevel);
		compDesc.setAlternativeSizeCommand(this.mySizeCommand.getSelection());
		compDesc.set_C_andCPP_CompileOptions(this.myCAndCppCommand.getText());
		compDesc.set_C_CompileOptions(this.myCCommand.getText());
		compDesc.set_CPP_CompileOptions(this.myCppCommand.getText());
		compDesc.set_All_CompileOptions(this.myAllCommand.getText());
		compDesc.set_Archive_CompileOptions(this.myArchiveCommand.getText());
		compDesc.set_Assembly_CompileOptions(this.myAssemblyCommand.getText());
		compDesc.set_Link_CompileOptions(this.myLinkCommand.getText());

		return compDesc;
	}

	@Override
	protected Object getFromSloeber(ICConfigurationDescription confDesc) {
		return mySloeberProject.getCompileDescription(confDesc.getName(), true);
	}

	@Override
	protected Object makeCopy(Object srcObject) {
		return new CompileDescription((CompileDescription) srcObject);
	}

	@Override
	protected void updateSloeber(ICConfigurationDescription confDesc, Object theObjectToStore) {
		mySloeberProject.setCompileDescription(confDesc.getName(), (CompileDescription) theObjectToStore);
	}

	@Override
	protected Object getnewDefaultObject() {
		return new CompileDescription();
	}

}
