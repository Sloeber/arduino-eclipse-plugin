package io.sloeber.ui.project.properties;

import java.util.Arrays;

import org.eclipse.cdt.core.settings.model.ICConfigurationDescription;
import org.eclipse.cdt.ui.newui.ICPropertyProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Text;

import io.sloeber.core.api.CompileDescription;
import io.sloeber.core.api.CompileDescription.WarningLevels;
import io.sloeber.ui.LabelCombo;
import io.sloeber.ui.Messages;

public class CompileProperties extends SloeberCpropertyTab {
	private LabelCombo myWarningLevel;
	private Text myCustomWarningLevel;
	private Button mySizeCommand;
	private Text myCAndCppCommand;
	private Text myCppCommand;
	private Text myCCommand;
	private Text myAllCommand;
	private Text myArchiveCommand;
	private Text myAssemblyCommand;
	private Text myLinkCommand;

	private boolean disableListeners = false;

	private Listener buttonListener = new Listener() {
		@Override
		public void handleEvent(Event e) {
			if (disableListeners)
				return;
			switch (e.type) {
			case SWT.Selection:
				getFromScreen();
				break;
			}
		}
	};
	protected Listener myLabelComboListener = new Listener() {
		@Override
		public void handleEvent(Event e) {
			if (disableListeners)
				return;
			getFromScreen();
			CompileDescription compDesc = (CompileDescription) getDescription(getConfdesc());
			myCustomWarningLevel.setEnabled(compDesc.getWarningLevel() == WarningLevels.CUSTOM);
		}
	};
	private FocusListener foucusListener = new FocusListener() {

		@Override
		public void focusGained(FocusEvent e) {
			// Not interested
		}

		@Override
		public void focusLost(FocusEvent e) {
			if (disableListeners)
				return;
			getFromScreen();
		}

	};

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
		textField.addFocusListener(foucusListener);
		return textField;
	}

	// From
	// https://stackoverflow.com/questions/13783295/getting-all-names-in-an-enum-as-a-string#13783744
	private static String[] getNames(Class<? extends Enum<?>> e) {
		return Arrays.stream(e.getEnumConstants()).map(Enum::name).toArray(String[]::new);
	}

	@Override
	public void createControls(Composite parent, ICPropertyProvider provider) {
		super.createControls(parent, provider);

		GridLayout theGridLayout = new GridLayout();
		theGridLayout.numColumns = 3;
		usercomp.setLayout(theGridLayout);

		// combobox to select warning level and optional custom values

		myWarningLevel = new LabelCombo(usercomp, Messages.ui_show_all_warnings, 1, true);
		myWarningLevel.setItems(getNames(WarningLevels.class));
		myWarningLevel.addListener(myLabelComboListener);


		myCustomWarningLevel = new Text(usercomp, SWT.BORDER | SWT.LEFT);
		GridData gridData = new GridData(GridData.FILL_HORIZONTAL);
		myCustomWarningLevel.setLayoutData(gridData);
		myCustomWarningLevel.addFocusListener(foucusListener);

		// checkbox show alternative size
		this.mySizeCommand = new Button(this.usercomp, SWT.CHECK);
		this.mySizeCommand.setText(Messages.ui_Alternative_size);
		this.mySizeCommand.setEnabled(true);
		this.mySizeCommand.setLayoutData(new GridData(SWT.BEGINNING, SWT.BEGINNING, false, false, 3, 1));
		mySizeCommand.addListener(SWT.Selection, buttonListener);

		createLine(this.usercomp, 3);
		this.myCAndCppCommand = makeOptionField(Messages.ui_append_c_cpp, Messages.ui_append_c_cpp_text);
		this.myCppCommand = makeOptionField(Messages.ui_append_cpp, Messages.ui_append_cpp_text);
		this.myCCommand = makeOptionField(Messages.ui_append_c, Messages.ui_append_c_text);
		this.myAssemblyCommand = makeOptionField(Messages.ui_append_assembly, Messages.ui_append_assembly_text);
		this.myArchiveCommand = makeOptionField(Messages.ui_append_archive, Messages.ui_append_archive_text);
		this.myLinkCommand = makeOptionField(Messages.ui_append_link, Messages.ui_append_link_text);
		this.myAllCommand = makeOptionField(Messages.ui_append_all, Messages.ui_append_all_text);

		updateScreen();
	}

	@Override
	protected String getQualifierString() {
		return "SloeberCompileProperties"; //$NON-NLS-1$
	}

	@Override
	protected void updateScreen() {
		disableListeners = true;
		CompileDescription compDesc = (CompileDescription) getDescription(getConfdesc());
		myWarningLevel.setText(compDesc.getWarningLevel().toString());
		myCustomWarningLevel.setEnabled(compDesc.getWarningLevel() == WarningLevels.CUSTOM);
		myCustomWarningLevel.setText(compDesc.getWarningLevel().getCustomWarningLevel());
		mySizeCommand.setSelection(compDesc.isAlternativeSizeCommand());
		myCAndCppCommand.setText(compDesc.get_C_andCPP_CompileOptions());
		myCCommand.setText(compDesc.get_C_CompileOptions());
		myCppCommand.setText(compDesc.get_CPP_CompileOptions());
		myAllCommand.setText(compDesc.get_All_CompileOptions());
		myArchiveCommand.setText(compDesc.get_Archive_CompileOptions());
		myAssemblyCommand.setText(compDesc.get_Assembly_CompileOptions());
		myLinkCommand.setText(compDesc.get_Link_CompileOptions());
		disableListeners = false;
	}

	@Override
	protected Object getFromScreen() {
		CompileDescription compDesc = (CompileDescription) getDescription(getConfdesc());
		WarningLevels warningLevel = WarningLevels.valueOf(myWarningLevel.getText());
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
	protected void updateSloeber(ICConfigurationDescription confDesc) {
		CompileDescription theObjectToStore = (CompileDescription) getDescription(confDesc);
		mySloeberProject.setCompileDescription(confDesc.getName(), theObjectToStore);
	}

	@Override
	protected Object getnewDefaultObject() {
		return new CompileDescription();
	}

}
