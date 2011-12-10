package it.baeyens.arduino.ui;

import it.baeyens.arduino.common.ArduinoConst;
import it.baeyens.arduino.common.Common;
import it.baeyens.arduino.tools.ArduinoHelpers;
import it.baeyens.arduino.tools.ArduinoProperties;

import java.lang.reflect.InvocationTargetException;
import java.net.URI;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExecutableExtension;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;

import org.eclipse.ui.actions.WorkspaceModifyOperation;
import org.eclipse.ui.dialogs.WizardNewProjectCreationPage;
import org.eclipse.ui.wizards.newresource.BasicNewProjectResourceWizard;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;

/**
 * This class is the class related to the new arduino sketch
 * 
 * @author Jan Baeyens
 * 
 */
public class NewArduinoSketchWizard extends Wizard implements INewWizard, IExecutableExtension {

	private WizardNewProjectCreationPage mWizardPage;
	private ArduinoSettingsPage mArduinoPage;
	private IConfigurationElement mConfig;
	private IProject mProject;

	public NewArduinoSketchWizard() {
		super();
	}

	@Override
	public void addPages() {
		mWizardPage = new WizardNewProjectCreationPage("New Arduino sketch");
		mWizardPage.setDescription("Create a new Arduino sketch.");
		mWizardPage.setTitle("New Arduino sketch");

		mArduinoPage = new ArduinoSettingsPage("Arduino information");
		mArduinoPage.setTitle("Provide the Arduino informtion.");
		mArduinoPage.setDescription("These settings can be changed later.");
		addPage(mWizardPage);
		addPage(mArduinoPage);
	}

	@Override
	public boolean performFinish() {

		if (mProject != null) {
			return true;
		}

		final IProject projectHandle = ResourcesPlugin.getWorkspace().getRoot().getProject(mWizardPage.getProjectName().replace(" ", "_"));
		try {

			URI projectURI = (!mWizardPage.useDefaults()) ? mWizardPage.getLocationURI() : null;

			IWorkspace workspace = ResourcesPlugin.getWorkspace();

			final IProjectDescription desc = workspace.newProjectDescription(projectHandle.getName().replace(" ", "_"));

			desc.setLocationURI(projectURI);

			/*
			 * Just like the ExampleWizard, but this time with an operation
			 * object that modifies workspaces.
			 */
			WorkspaceModifyOperation op = new WorkspaceModifyOperation() {
				@Override
				protected void execute(IProgressMonitor monitor) throws CoreException {
					createProject(desc, projectHandle, monitor);
				}
			};

			/*
			 * This isn't as robust as the code in the
			 * BasicNewProjectResourceWizard class. Consider beefing this up to
			 * improve error handling.
			 */
			getContainer().run(true, true, op);
		} catch (InterruptedException e) {
			return false;
		} catch (InvocationTargetException e) {
			Throwable realException = e.getTargetException();
			MessageDialog.openError(getShell(), "Error", realException.getMessage());
			return false;
		}

		mProject = projectHandle;

		if (mProject == null) {
			return false;
		}

		BasicNewProjectResourceWizard.updatePerspective(mConfig);
		IWorkbenchWindow TheWindow = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
		BasicNewProjectResourceWizard.selectAndReveal(mProject, TheWindow);

		return true;
	}

	/**
	 * This creates the project in the workspace.
	 * 
	 * @param description
	 * @param projectHandle
	 * @param monitor
	 * @throws OperationCanceledException
	 */
	void createProject(IProjectDescription description, IProject proj, IProgressMonitor monitor) throws OperationCanceledException {

		monitor.beginTask("", 2000);
		try {
			proj.create(description, new SubProgressMonitor(monitor, 1000));

			if (monitor.isCanceled()) {
				throw new OperationCanceledException();
			}

			proj.open(IResource.BACKGROUND_REFRESH, new SubProgressMonitor(monitor, 1000));
			IContainer container = (IContainer) proj;

			// Add the C C++ AVR and other needed Natures to the project
			ArduinoHelpers.addTheNatures(proj);

			/* Add the c configuration file */
			ArduinoHelpers.addFileToProject(container, new Path(".cproject"), Stream.openContentStream(proj.getName(),"", "templates/cproject.sketch"), monitor);

			/* Add the sketch source code file */
			ArduinoHelpers.addFileToProject(container, new Path(proj.getName() + ".cpp"), Stream.openContentStream(proj.getName(),"", "templates/sketch.cpp"), monitor);

			ArduinoProperties ProjectProps= mArduinoPage.GetProperties();
			/* Add the sketch header file */
			String Include ="WProgram.h";
			if (ArduinoHelpers.isArduinoIdeOne() ) //this is Arduino version 1.0
			{ 
				Include ="Arduino.h";
			}
			ArduinoHelpers.addFileToProject(container, new Path(proj.getName() + ".h"), Stream.openContentStream(proj.getName(),Include, "templates/sketch.h"), monitor);

			
			
			/* Create the Arduino project */
			IProject Arduino_Core_project = ArduinoHelpers.createArduino_coreProject(description, monitor,ProjectProps );

			/* Make a reference to the arduino project */
			ArduinoHelpers.addLibraryDependency(proj, Arduino_Core_project);
			
			if (ArduinoHelpers.isArduinoIdeOne() ) //this is Arduino version 1.0
			{
				ArduinoHelpers.addIncludeFolder(proj,new Path("/${ARDUINOBOARDNAME}/${ARDUINOBOARDVARIANT}"));
			}
			/* Save the properties */
			mArduinoPage.save(proj);

			monitor.done();

		} catch (CoreException e) {
			Common.log(new Status(Status.ERROR, ArduinoConst.CORE_PLUGIN_ID, "Failed to create project " + proj.getName(), e));
			throw new OperationCanceledException();
		}

	}

	@Override
	public void init(IWorkbench workbench, IStructuredSelection selection) {
		// snipped...
	}

	@Override
	public void setInitializationData(IConfigurationElement config, String propertyName, Object data) throws CoreException {
		// snipped...
		mConfig = config;

	}

}
