package it.baeyens.arduino.ui;

import org.eclipse.core.resources.IProject;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.dialogs.WizardResourceImportPage;

public class Wizard_Select_Project_Page extends WizardResourceImportPage
	{
		private Text controlLibraryPath=null;
		private IProject mProject=null;
		
		protected Wizard_Select_Project_Page(String name, IStructuredSelection selection) {
			super(name,selection);
		
		}

		@Override
		protected void createSourceGroup(Composite parent)
			{
				Composite composite = new Composite(parent, SWT.NONE);
				GridLayout theGridLayout = new GridLayout();
				GridData theGriddata;
				theGridLayout.numColumns = 3;
				composite.setLayout(theGridLayout);
				composite.setLayoutData(new GridData( GridData.HORIZONTAL_ALIGN_FILL | GridData.GRAB_HORIZONTAL));
				composite.setFont(parent.getFont());
				
				controlLibraryPath = new Text(composite, SWT.SINGLE | SWT.BORDER);
        theGriddata = new GridData(GridData.HORIZONTAL_ALIGN_FILL | GridData.GRAB_HORIZONTAL);
        theGriddata.widthHint = SIZING_TEXT_FIELD_WIDTH;
        controlLibraryPath.setVisible(false);
        controlLibraryPath.setLayoutData(theGriddata);		
				

				Label line = new Label(composite, SWT.HORIZONTAL | SWT.BOLD );
				line.setText("Arduino library to import to");
				theGriddata = new GridData(SWT.FILL,SWT.CENTER,true,false);
				theGriddata.horizontalSpan = 3;
				line.setLayoutData(theGriddata);
				setControl(composite);
			}
		
		@Override
		protected ITreeContentProvider getFileProvider()
			{
				return null;
			}
		@Override
		protected ITreeContentProvider getFolderProvider()
			{
				return null;
			}
		public void setProject(IProject project)
			{
				if (project!=null) 
					{
						mProject=project;
						setContainerFieldValue(project.getName());
					}
				
			}

		public boolean canFinish()
			{
				return (GetProject()!=null); 
			}

		public IProject GetProject()
			{
				if (controlLibraryPath==null) return mProject;
				return getSpecifiedContainer().getProject();
			}

	}
