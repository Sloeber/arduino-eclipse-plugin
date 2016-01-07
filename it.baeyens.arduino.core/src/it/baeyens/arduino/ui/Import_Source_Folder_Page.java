package it.baeyens.arduino.ui;

import org.eclipse.core.resources.IProject;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.dialogs.WizardResourceImportPage;

import it.baeyens.arduino.common.ArduinoConst;

//TODO: Should this be called Wizard_Source_Folder_Page to make consistent with the library import.....
//TODO: Or Arduino_Source_Folder_Import_Page

/**
 * Import_Source_Folder_Page is the one and only page in the source folder
 * import wizard. It controls a text field and a browse button.
 * 
 * @author Jan Baeyens
 * 
 */
public class Import_Source_Folder_Page extends WizardResourceImportPage {
    protected Text controlLibraryPath;
    private Button controlBrowseButton;

    private IProject mProject = null;

    protected Import_Source_Folder_Page(IProject project, String name, IStructuredSelection selection) {

	super(name, selection);

	setImportProject(project);
	if (this.mProject == null) {
	    setTitle(Messages.error_no_Arduino_project_selected);
	    setDescription(Messages.ui_import_no_arduino_project_help);
	} else {
	    setTitle(Messages.ui_import_source_folder);
	    setDescription(Messages.ui_import_source_folder_help + ' ' + this.mProject.getName());
	}

    }

    @Override
    protected void createSourceGroup(Composite parent) {

	Composite composite = new Composite(parent, SWT.NONE);
	GridLayout theGridLayout = new GridLayout();
	GridData theGriddata;
	theGridLayout.numColumns = 3;
	composite.setLayout(theGridLayout);
	composite.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_FILL | GridData.GRAB_HORIZONTAL));
	composite.setFont(parent.getFont());

	Label line = new Label(composite, SWT.HORIZONTAL | SWT.BOLD);
	line.setText(Messages.ui_select_folder);
	theGriddata = new GridData(SWT.FILL, SWT.CENTER, true, false);
	theGriddata.horizontalSpan = 3;
	line.setLayoutData(theGriddata);

	Label TheLabel = new Label(composite, SWT.NONE);
	TheLabel.setText("Source Folder Location:"); //$NON-NLS-1$
	theGriddata = new GridData();
	theGriddata.horizontalAlignment = SWT.LEFT;
	theGriddata.horizontalSpan = 1;
	theGriddata.grabExcessHorizontalSpace = false;
	TheLabel.setLayoutData(theGriddata);

	this.controlLibraryPath = new Text(composite, SWT.SINGLE | SWT.BORDER);
	theGriddata = new GridData(GridData.HORIZONTAL_ALIGN_FILL | GridData.GRAB_HORIZONTAL);
	theGriddata.widthHint = SIZING_TEXT_FIELD_WIDTH;
	// theGriddata.horizontalSpan = 1;
	this.controlLibraryPath.setLayoutData(theGriddata);
	this.controlLibraryPath.addKeyListener(new KeyListener() {

	    @Override
	    public void keyReleased(KeyEvent e) {
		updateWidgetEnablements();
	    }

	    @Override
	    public void keyPressed(KeyEvent e) {
		// TODO Auto-generated method stub

	    }
	});

	this.controlBrowseButton = new Button(composite, SWT.NONE);
	this.controlBrowseButton.setText("Browse..."); //$NON-NLS-1$
	theGriddata = new GridData();
	theGriddata.horizontalSpan = 1;
	theGriddata.horizontalAlignment = SWT.LEAD;
	theGriddata.grabExcessHorizontalSpace = false;
	this.controlBrowseButton.setLayoutData(theGriddata);

	this.controlBrowseButton.addSelectionListener(new SelectionAdapter() {
	    @SuppressWarnings("synthetic-access")
	    @Override
	    public void widgetSelected(SelectionEvent event) {
		final Shell shell = new Shell();
		DirectoryDialog theDialog = new DirectoryDialog(shell);
		if ((Import_Source_Folder_Page.this.controlLibraryPath.getText() == null)
			|| (Import_Source_Folder_Page.this.controlLibraryPath.getText() == ArduinoConst.EMPTY_STRING)) {
		    theDialog.setFilterPath(Import_Source_Folder_Page.this.controlLibraryPath.getText());
		}

		String Path = theDialog.open();
		if (Path != null) {
		    Import_Source_Folder_Page.this.controlLibraryPath.setText(Path);
		    updateWidgetEnablements();
		}
	    }
	});

	line = new Label(composite, SWT.HORIZONTAL | SWT.BOLD);
	line.setText(Messages.ui_import_subfolder_to_import_to);
	theGriddata = new GridData(SWT.FILL, SWT.CENTER, true, false);
	theGriddata.horizontalSpan = 3;
	line.setLayoutData(theGriddata);

    }

    @Override
    protected ITreeContentProvider getFileProvider() {
	return null;
    }

    @Override
    protected ITreeContentProvider getFolderProvider() {
	return null;
    }

    public boolean canFinish() {
	return !((this.controlLibraryPath.getText().equals(ArduinoConst.EMPTY_STRING))
		|| (getContainerFullPath() == null));
    }

    public String GetLibraryFolder() {
	return this.controlLibraryPath.getText() == null ? ArduinoConst.EMPTY_STRING
		: this.controlLibraryPath.getText().trim();
    }

    public void setImportProject(IProject project) {
	if (project != null) {
	    this.mProject = project;
	    setContainerFieldValue(project.getName());
	}
    }

    public IProject getProject() {
	if (validateDestinationGroup()) {
	    return getSpecifiedContainer().getProject();
	}
	return null;
    }

}
