package it.baeyens.arduino.tools;


import it.baeyens.arduino.common.ArduinoConst;
import it.baeyens.arduino.common.ArduinoInstancePreferences;
import org.eclipse.core.resources.IProject;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.swt.SWT;
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
/**
 * Import_Arduino_Library_Page is the one and only page in the library import wizard.
 * It controls a text field and a browse button.
 * @author Jan Baeyens
 *
 */
public class Import_Arduino_Library_Page extends WizardResourceImportPage {
	private Text controlLibraryPath;
	private Button controlBrowseButton;

	protected Import_Arduino_Library_Page(String name, IStructuredSelection selection) {
		super(name, selection);
	
	}
	
	

	@Override
	protected void createSourceGroup(Composite parent) {
		Composite composite = new Composite(parent, SWT.NONE);
		GridLayout theGridLayout = new GridLayout();
		GridData theGriddata;
		theGridLayout.numColumns = 3;
		composite.setLayout(theGridLayout);
		composite.setLayoutData(new GridData( GridData.HORIZONTAL_ALIGN_FILL | GridData.GRAB_HORIZONTAL));
		composite.setFont(parent.getFont());
		

		Label line = new Label(composite, SWT.HORIZONTAL | SWT.BOLD );
		line.setText("Arduino library to import");
		theGriddata = new GridData(SWT.FILL,SWT.CENTER,true,false);
		theGriddata.horizontalSpan = 3;
		line.setLayoutData(theGriddata);
		
		
		Label TheLabel = new Label(composite, SWT.NONE);
		TheLabel.setText("Library Location"); //$NON-NLS-1$
		theGriddata = new GridData();
		theGriddata.horizontalAlignment = SWT.LEFT;
		theGriddata.horizontalSpan = 1;
		theGriddata.grabExcessHorizontalSpace = false;
		TheLabel.setLayoutData(theGriddata);
		
		controlLibraryPath = new Text(composite, SWT.SINGLE | SWT.BORDER);
        theGriddata = new GridData(GridData.HORIZONTAL_ALIGN_FILL | GridData.GRAB_HORIZONTAL);
        theGriddata.widthHint = SIZING_TEXT_FIELD_WIDTH;
        //theGriddata.horizontalSpan = 1;
        controlLibraryPath.setLayoutData(theGriddata);		
		

		controlBrowseButton = new Button(composite, SWT.NONE);
		controlBrowseButton.setText("Browse..."); //$NON-NLS-1$
		theGriddata = new GridData();;
		theGriddata.horizontalSpan = 1;
		theGriddata.horizontalAlignment = SWT.LEAD;
		theGriddata.grabExcessHorizontalSpace = false;
		controlBrowseButton.setLayoutData(theGriddata);
		controlBrowseButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent event) {
				final Shell shell = new Shell();
				DirectoryDialog theDialog = new DirectoryDialog(shell);
				if ((controlLibraryPath.getText()==null) | (controlLibraryPath.getText()==""))
				{
					theDialog.setFilterPath(ArduinoInstancePreferences.getArduinoPath().append( ArduinoConst.LIBRARY_PATH_SUFFIX).toString());
				}
				else
				{
					theDialog.setFilterPath(controlLibraryPath.getText());
				}
				
				String Path = theDialog.open();
				if (Path!=null)	
					{
					controlLibraryPath.setText(Path);
					updateWidgetEnablements();
					}
			}
		});

		line = new Label(composite, SWT.HORIZONTAL | SWT.BOLD );
		line.setText("Arduino sketch to import to");
		theGriddata = new GridData(SWT.FILL,SWT.CENTER,true,false);
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
		return !( (controlLibraryPath.getText().equals("")) | (getContainerFullPath()==null) );
	}



	public IProject GetProject() {
		if (validateDestinationGroup())
		{
			return getSpecifiedContainer().getProject();
		}
		return null;
	}
	
	public String GetLibraryFolder()
	{
		return controlLibraryPath.getText()==null ? "" : controlLibraryPath.getText().trim();
	}



	public void setImportProject(IProject project) {
		 setContainerFieldValue(project.getName());
	}


}
