package io.sloeber.ui.project.properties;

import org.eclipse.cdt.core.settings.model.ICConfigurationDescription;
import org.eclipse.cdt.core.settings.model.ICResourceDescription;
import org.eclipse.cdt.ui.newui.AbstractCPropertyTab;
import org.eclipse.cdt.ui.newui.ICPropertyProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import io.sloeber.core.api.CompileOptions;
import io.sloeber.ui.Messages;

public class CompileProperties extends AbstractCPropertyTab {

	@Override
	protected void performOK() {

		updateStorageData();
		if (getConfdesc() != null) {
			CompileProperties.this.myCompileOptions.save(getConfdesc());
		}
		super.performOK();
	}

	private Button myWarningLevel;
	private Button mySizeCommand;
	private Text myCAndCppCommand;
	private Text myCppCommand;
	private Text myCCommand;
	private Text myAllCommand;
	private Text myArchiveCommand;
	private Text myAssemblyCommand;
	private Text myLinkCommand;



	protected CompileOptions myCompileOptions;

	private static void createLine(Composite parent, int ncol) {
		Label line = new Label(parent, SWT.SEPARATOR | SWT.HORIZONTAL | SWT.BOLD);
		GridData gridData = new GridData(GridData.FILL_HORIZONTAL);
		gridData.horizontalSpan = ncol;
		line.setLayoutData(gridData);
	}

	private Text makeOptionField( String labelText,String toolTipText) {
		// edit field add to C & C++ command line
		Label label = new Label(this.usercomp, SWT.LEFT);
		label.setText(labelText);
		label.setLayoutData(new GridData(SWT.BEGINNING, SWT.BEGINNING, false, false, 1, 2));
		Text textField = new Text(this.usercomp, SWT.MULTI | SWT.WRAP | SWT.V_SCROLL | SWT.BORDER);
		textField.setToolTipText(toolTipText);
		textField.setEnabled(true);
		GridData gridData = new GridData(GridData.FILL_HORIZONTAL);
		gridData.horizontalSpan = 1;
		gridData.verticalSpan = 2;
		textField.setLayoutData(gridData);
		return textField;
	}

	@Override
	public void createControls(Composite parent, ICPropertyProvider provider) {
		super.createControls(parent, provider);
		this.myCompileOptions = new CompileOptions(getConfdesc());
		GridLayout theGridLayout = new GridLayout();
		theGridLayout.numColumns = 2;
		this.usercomp.setLayout(theGridLayout);

		// checkbox show all warnings => Set WARNING_LEVEL=wall else
		// WARNING_LEVEL=$ARDUINO_WARNING_LEVEL
		this.myWarningLevel = new Button(this.usercomp, SWT.CHECK);
		this.myWarningLevel.setText(Messages.ui_show_all_warnings);
		this.myWarningLevel.setEnabled(true);
		this.myWarningLevel.setLayoutData(new GridData(SWT.BEGINNING, SWT.BEGINNING, false, false, 2, 1));


		// checkbox show alternative size
		this.mySizeCommand = new Button(this.usercomp, SWT.CHECK);
		this.mySizeCommand.setText(Messages.ui_Alternative_size);
		this.mySizeCommand.setEnabled(true);
		this.mySizeCommand.setLayoutData(new GridData(SWT.BEGINNING, SWT.BEGINNING, false, false, 2, 1));
;

		createLine(this.usercomp, 2);
		this.myCAndCppCommand = makeOptionField(Messages.ui_append_c_cpp, Messages.ui_append_c_cpp_text);
		this.myCppCommand = makeOptionField(Messages.ui_append_cpp, Messages.ui_append_cpp_text);
		this.myCCommand = makeOptionField(Messages.ui_append_c, Messages.ui_append_c_text);
		this.myAssemblyCommand = makeOptionField(Messages.ui_append_assembly, Messages.ui_append_assembly_text);
		this.myArchiveCommand = makeOptionField(Messages.ui_append_archive, Messages.ui_append_archive_text);
		this.myLinkCommand = makeOptionField(Messages.ui_append_link, Messages.ui_append_link_text);
		this.myAllCommand = makeOptionField(Messages.ui_append_all, Messages.ui_append_all_text);

		theGridLayout = new GridLayout();
		theGridLayout.numColumns = 2;
		this.usercomp.setLayout(theGridLayout);
		updateScreenData();
		setVisible(true);
	}

	private void updateScreenData() {

		this.myWarningLevel.setSelection(this.myCompileOptions.isWarningLevel());
		this.mySizeCommand.setSelection(this.myCompileOptions.isAlternativeSizeCommand());
		this.myCAndCppCommand.setText(this.myCompileOptions.get_C_andCPP_CompileOptions());
		this.myCCommand.setText(this.myCompileOptions.get_C_CompileOptions());
		this.myCppCommand.setText(this.myCompileOptions.get_CPP_CompileOptions());

		this.myAllCommand.setText(this.myCompileOptions.get_All_CompileOptions());
		this.myArchiveCommand.setText(this.myCompileOptions.get_Archive_CompileOptions());
		this.myAssemblyCommand.setText(this.myCompileOptions.get_Assembly_CompileOptions());
		this.myLinkCommand.setText(this.myCompileOptions.get_Link_CompileOptions());
	}
	private void updateStorageData() {

		this.myCompileOptions.setWarningLevel(this.myWarningLevel.getSelection());
		this.myCompileOptions.setAlternativeSizeCommand(this.mySizeCommand.getSelection());
		this.myCompileOptions.set_C_andCPP_CompileOptions(this.myCAndCppCommand.getText());
		this.myCompileOptions.set_C_CompileOptions(this.myCCommand.getText());
		this.myCompileOptions.set_CPP_CompileOptions(this.myCppCommand.getText());

		this.myCompileOptions.set_All_CompileOptions(this.myAllCommand.getText());
		this.myCompileOptions.set_Archive_CompileOptions(this.myArchiveCommand.getText());
		this.myCompileOptions.set_Assembly_CompileOptions(this.myAssemblyCommand.getText());
		this.myCompileOptions.set_Link_CompileOptions(this.myLinkCommand.getText());
	}
	@Override
	protected void updateData(ICResourceDescription cfg) {
		this.myCompileOptions = new CompileOptions(getConfdesc());
		updateScreenData();
	}

	@Override
	public boolean canBeVisible() {
		return true;
	}

	@Override
	protected void updateButtons() {
		// nothing to do here

	}

	@Override
	protected void performApply(ICResourceDescription src, ICResourceDescription dst) {
		updateStorageData();
		if (dst.getConfiguration() != null) {
			CompileProperties.this.myCompileOptions.save(dst.getConfiguration());
		}
	}

	@Override
	protected void performDefaults() {
		this.myCompileOptions = new CompileOptions(null);
		updateScreenData();
	}

	/**
	 * Get the configuration we are currently working in. The configuration is
	 * null if we are in the create sketch wizard.
	 *
	 * @return the configuration to save info into
	 */
	protected ICConfigurationDescription getConfdesc() {
		if (this.page != null) {
			return getResDesc().getConfiguration();
		}
		return null;
	}

}
