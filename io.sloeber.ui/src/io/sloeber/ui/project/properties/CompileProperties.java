package io.sloeber.ui.project.properties;

import java.util.Arrays;

import org.eclipse.cdt.core.settings.model.ICResourceDescription;
import org.eclipse.cdt.ui.newui.ICPropertyProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Text;

import io.sloeber.core.api.CompileDescription;
import io.sloeber.core.api.CompileDescription.SizeCommands;
import io.sloeber.core.api.CompileDescription.WarningLevels;
import io.sloeber.ui.LabelCombo;
import io.sloeber.ui.Messages;

public class CompileProperties extends SloeberCpropertyTab {
	private LabelCombo myWarningLevel;
	private Text myCustomWarningLevel;
	private LabelCombo mySizeCommand;
	private Text myCustomSizeCommand;
	private Text myCAndCppCommand;
	private Text myCppCommand;
	private Text myCCommand;
	private Text myAllCommand;
	private Text myArchiveCommand;
	private Text myAssemblyCommand;
	private Text myLinkCommand;
	private CompileDescription myCompDesc = new CompileDescription();

	private boolean disableListeners = false;

	protected Listener myLabelComboListener = new Listener() {
		@Override
		public void handleEvent(Event e) {
			if (disableListeners)
				return;
			getFromScreen();
			myCustomWarningLevel.setEnabled(myCompDesc.getWarningLevel() == WarningLevels.CUSTOM);
			myCustomSizeCommand.setEnabled(myCompDesc.getSizeCommand() == SizeCommands.CUSTOM);
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
		mySizeCommand = new LabelCombo(usercomp, Messages.ui_Alternative_size, 1, true);
		mySizeCommand.setItems(getNames(SizeCommands.class));
		mySizeCommand.addListener(myLabelComboListener);

		myCustomSizeCommand = new Text(usercomp, SWT.BORDER | SWT.LEFT);
		gridData = new GridData(GridData.FILL_HORIZONTAL);
		myCustomSizeCommand.setLayoutData(gridData);
		myCustomSizeCommand.addFocusListener(foucusListener);

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
	protected void updateScreen() {
		disableListeners = true;
		myWarningLevel.setText(myCompDesc.getWarningLevel().toString());
		myCustomWarningLevel.setEnabled(myCompDesc.getWarningLevel() == WarningLevels.CUSTOM);
		myCustomWarningLevel.setText(myCompDesc.getWarningLevel().getCustomWarningLevel());

		mySizeCommand.setText(myCompDesc.getSizeCommand().toString());
		myCustomSizeCommand.setEnabled(myCompDesc.getSizeCommand() == SizeCommands.CUSTOM);
		myCustomSizeCommand.setText(myCompDesc.getSizeCommand().getCustomSizeCommand());

		myCAndCppCommand.setText(myCompDesc.get_C_andCPP_CompileOptions());
		myCCommand.setText(myCompDesc.get_C_CompileOptions());
		myCppCommand.setText(myCompDesc.get_CPP_CompileOptions());
		myAllCommand.setText(myCompDesc.get_All_CompileOptions());
		myArchiveCommand.setText(myCompDesc.get_Archive_CompileOptions());
		myAssemblyCommand.setText(myCompDesc.get_Assembly_CompileOptions());
		myLinkCommand.setText(myCompDesc.get_Link_CompileOptions());
		disableListeners = false;
	}

	private void getFromScreen() {

		WarningLevels warningLevel = WarningLevels.valueOf(myWarningLevel.getText());
		warningLevel.setCustomWarningLevel(myCustomWarningLevel.getText());

		SizeCommands sizeCommand = SizeCommands.valueOf(mySizeCommand.getText());
		sizeCommand.setCustomSizeCommand(myCustomSizeCommand.getText());

		myCompDesc.setWarningLevel(warningLevel);
		myCompDesc.setSizeCommand(sizeCommand);
		myCompDesc.set_C_andCPP_CompileOptions(this.myCAndCppCommand.getText());
		myCompDesc.set_C_CompileOptions(this.myCCommand.getText());
		myCompDesc.set_CPP_CompileOptions(this.myCppCommand.getText());
		myCompDesc.set_All_CompileOptions(this.myAllCommand.getText());
		myCompDesc.set_Archive_CompileOptions(this.myArchiveCommand.getText());
		myCompDesc.set_Assembly_CompileOptions(this.myAssemblyCommand.getText());
		myCompDesc.set_Link_CompileOptions(this.myLinkCommand.getText());

	}

	@Override
	protected void performApply(ICResourceDescription src, ICResourceDescription dst) {
		// TODO Auto-generated method stub

	}

	@Override
	protected void performDefaults() {
		// TODO Auto-generated method stub

	}

}
