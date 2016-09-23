package io.sloeber.ui.project.properties;

import org.eclipse.cdt.core.settings.model.ICResourceDescription;
import org.eclipse.cdt.ui.newui.AbstractCPropertyTab;
import org.eclipse.cdt.ui.newui.ICPropertyProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Text;

import io.sloeber.core.api.CompileOptions;
import io.sloeber.ui.Messages;

public class CompileProperties extends AbstractCPropertyTab {
    private static final String EMPTY_STRING = ""; //$NON-NLS-1$
    Button myWarningLevel;
    Button mySizeCommand;
    public Text myCAndCppCommand;
    public Text myCppCommand;
    public Text myCCommand;
    CompileOptions myCompileOptions;

    private static void createLine(Composite parent, int ncol) {
	Label line = new Label(parent, SWT.SEPARATOR | SWT.HORIZONTAL | SWT.BOLD);
	GridData gridData = new GridData(GridData.FILL_HORIZONTAL);
	gridData.horizontalSpan = ncol;
	line.setLayoutData(gridData);
    }

    @Override
    public void createControls(Composite parent, ICPropertyProvider provider) {
	super.createControls(parent, provider);
	this.myCompileOptions = new CompileOptions(getResDesc().getConfiguration());
	GridLayout theGridLayout = new GridLayout();
	theGridLayout.numColumns = 2;
	this.usercomp.setLayout(theGridLayout);

	// checkbox show all warnings => Set WARNING_LEVEL=wall else
	// WARNING_LEVEL=$ARDUINO_WARNING_LEVEL
	this.myWarningLevel = new Button(this.usercomp, SWT.CHECK);
	this.myWarningLevel.setText(Messages.ui_show_all_warnings);
	this.myWarningLevel.setEnabled(true);
	this.myWarningLevel.setLayoutData(new GridData(SWT.BEGINNING, SWT.BEGINNING, false, false, 2, 1));
	this.myWarningLevel.addListener(UPDATE, new Listener() {
	    @Override
	    public void handleEvent(Event e) {
		CompileProperties.this.myCompileOptions
			.setMyWarningLevel(CompileProperties.this.myWarningLevel.getSelection());
		if (getResDesc().getConfiguration() != null) {
		    CompileProperties.this.myCompileOptions.save(getResDesc().getConfiguration());
		}
	    }
	});

	// checkbox show alternative size
	this.mySizeCommand = new Button(this.usercomp, SWT.CHECK);
	this.mySizeCommand.setText(Messages.ui_Alternative_size);
	this.mySizeCommand.setEnabled(true);
	this.mySizeCommand.setLayoutData(new GridData(SWT.BEGINNING, SWT.BEGINNING, false, false, 2, 1));
	this.mySizeCommand.addListener(UPDATE, new Listener() {
	    @Override
	    public void handleEvent(Event e) {
		CompileProperties.this.myCompileOptions
			.setMyAlternativeSizeCommand(CompileProperties.this.mySizeCommand.getSelection());
		if (getResDesc().getConfiguration() != null) {
		    CompileProperties.this.myCompileOptions.save(getResDesc().getConfiguration());
		}
	    }

	});

	createLine(this.usercomp, 2);
	// edit field add to C & C++ command line
	Label label = new Label(this.usercomp, SWT.LEFT);
	label.setText(Messages.ui_Apend_c_cpp);
	label.setLayoutData(new GridData(SWT.BEGINNING, SWT.BEGINNING, false, false, 1, 2));
	this.myCAndCppCommand = new Text(this.usercomp, SWT.MULTI | SWT.WRAP | SWT.V_SCROLL | SWT.BORDER);
	this.myCAndCppCommand.setText(EMPTY_STRING);
	this.myCAndCppCommand.setToolTipText(Messages.ui_append_c_cpp_text);
	this.myCAndCppCommand.setEnabled(true);

	GridData gridData = new GridData(GridData.FILL_HORIZONTAL);
	gridData.horizontalSpan = 1;
	gridData.verticalSpan = 2;
	this.myCAndCppCommand.setLayoutData(gridData);
	this.myCAndCppCommand.addModifyListener(new ModifyListener() {

	    @Override
	    public void modifyText(ModifyEvent e) {
		CompileProperties.this.myCompileOptions
			.setMyAditional_C_andCPP_CompileOptions(CompileProperties.this.myCAndCppCommand.getText());
		if (getResDesc().getConfiguration() != null) {
		    CompileProperties.this.myCompileOptions.save(getResDesc().getConfiguration());
		}
	    }
	});

	// edit field add to C++ command line
	label = new Label(this.usercomp, SWT.LEFT);
	label.setText(Messages.ui_append_cpp);
	label.setLayoutData(new GridData(SWT.BEGINNING, SWT.BEGINNING, false, false, 1, 2));
	this.myCppCommand = new Text(this.usercomp, SWT.MULTI | SWT.WRAP | SWT.V_SCROLL | SWT.BORDER);
	this.myCppCommand.setText(EMPTY_STRING);
	this.myCppCommand.setToolTipText(Messages.ui_append_cpp_text);
	this.myCppCommand.setEnabled(true);
	this.myCppCommand.setLayoutData(gridData);
	this.myCppCommand.addModifyListener(new ModifyListener() {

	    @Override
	    public void modifyText(ModifyEvent e) {
		CompileProperties.this.myCompileOptions
			.setMyAditional_CPP_CompileOptions(CompileProperties.this.myCppCommand.getText());
		if (getResDesc().getConfiguration() != null) {
		    CompileProperties.this.myCompileOptions.save(getResDesc().getConfiguration());
		}
	    }
	});

	// edit field add to C command line
	label = new Label(this.usercomp, SWT.LEFT);
	label.setText(Messages.ui_append_c);
	label.setLayoutData(new GridData(SWT.BEGINNING, SWT.BEGINNING, false, false, 1, 2));
	this.myCCommand = new Text(this.usercomp, SWT.MULTI | SWT.WRAP | SWT.V_SCROLL | SWT.BORDER);
	this.myCCommand.setText(EMPTY_STRING);
	this.myCCommand.setToolTipText(Messages.ui_append_c_text);
	this.myCCommand.setEnabled(true);
	this.myCCommand.setLayoutData(gridData);
	this.myCppCommand.addModifyListener(new ModifyListener() {

	    @Override
	    public void modifyText(ModifyEvent e) {
		CompileProperties.this.myCompileOptions
			.setMyAditional_CPP_CompileOptions(CompileProperties.this.myCCommand.getText());
		if (getResDesc().getConfiguration() != null) {
		    CompileProperties.this.myCompileOptions.save(getResDesc().getConfiguration());
		}
	    }
	});

	theGridLayout = new GridLayout();
	theGridLayout.numColumns = 2;
	this.usercomp.setLayout(theGridLayout);
	setValues();
	setVisible(true);
    }

    private void setValues() {

	this.myWarningLevel.setSelection(this.myCompileOptions.isMyWarningLevel());

	this.mySizeCommand.setSelection(this.myCompileOptions.isMyAlternativeSizeCommand());
	this.myCAndCppCommand.setText(this.myCompileOptions.getMyAditional_C_andCPP_CompileOptions());
	this.myCCommand.setText(this.myCompileOptions.getMyAditional_C_CompileOptions());
	this.myCppCommand.setText(this.myCompileOptions.getMyAditional_CPP_CompileOptions());
    }

    @Override
    protected void updateData(ICResourceDescription cfg) {
	this.myCompileOptions = new CompileOptions(getResDesc().getConfiguration());
	setValues();
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
	// nothing to do here
    }

    @Override
    protected void performDefaults() {
	this.myWarningLevel.setSelection(true);
	this.mySizeCommand.setSelection(false);
	this.myCAndCppCommand.setText(EMPTY_STRING);
	this.myCCommand.setText(EMPTY_STRING);
	this.myCppCommand.setText(EMPTY_STRING);
    }
}
