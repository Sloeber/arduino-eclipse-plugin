package it.baeyens.arduino.ui;

import it.baeyens.arduino.common.ArduinoConst;
import it.baeyens.arduino.common.ArduinoInstancePreferences;
import it.baeyens.arduino.common.Common;
import it.baeyens.arduino.tools.ArduinoHelpers;
import it.baeyens.arduino.tools.ShouldHaveBeenInCDT;
import it.baeyens.arduino.tools.Stream;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;

import org.eclipse.cdt.core.model.CoreModel;
import org.eclipse.cdt.core.settings.model.CSourceEntry;
import org.eclipse.cdt.core.settings.model.ICConfigurationDescription;
import org.eclipse.cdt.core.settings.model.ICExclusionPatternPathEntry;
import org.eclipse.cdt.core.settings.model.ICProjectDescription;
import org.eclipse.cdt.core.settings.model.ICResourceDescription;
import org.eclipse.cdt.core.settings.model.ICSettingEntry;
import org.eclipse.cdt.core.settings.model.ICSourceEntry;
import org.eclipse.cdt.managedbuilder.core.ManagedBuildManager;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExecutableExtension;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
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
import org.eclipse.ui.wizards.newresource.BasicNewResourceWizard;

/**
 * This class is the class related to the new arduino sketch
 * 
 * @author Jan Baeyens
 * 
 */
public class NewArduinoSketchWizard extends Wizard implements INewWizard,
	IExecutableExtension {

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

	final IProject projectHandle = ResourcesPlugin
		.getWorkspace()
		.getRoot()
		.getProject(
			Common.MakeNameCompileSafe(mWizardPage.getProjectName()));
	try {

	    URI projectURI = (!mWizardPage.useDefaults()) ? mWizardPage
		    .getLocationURI() : null;

	    IWorkspace workspace = ResourcesPlugin.getWorkspace();

	    final IProjectDescription desc = workspace
		    .newProjectDescription(projectHandle.getName());

	    desc.setLocationURI(projectURI);

	    /*
	     * Just like the ExampleWizard, but this time with an operation
	     * object that modifies workspaces.
	     */
	    WorkspaceModifyOperation op = new WorkspaceModifyOperation() {
		@Override
		protected void execute(IProgressMonitor monitor)
			throws CoreException {
		    createProject(desc, projectHandle, monitor);
		}
	    };

	    /*
	     * This isn't as robust as the code in the
	     * BasicNewProjectResourceWizard class. Consider beefing this up to
	     * improve error handling.
	     */
	    getContainer().run(false, true, op);
	} catch (InterruptedException e) {
	    return false;
	} catch (InvocationTargetException e) {
	    Throwable realException = e.getTargetException();
	    MessageDialog.openError(getShell(), "Error",
		    realException.getMessage());
	    return false;
	}

	mProject = projectHandle;

	if (mProject == null) {
	    return false;
	}

	BasicNewProjectResourceWizard.updatePerspective(mConfig);
	IWorkbenchWindow TheWindow = PlatformUI.getWorkbench()
		.getActiveWorkbenchWindow();
	BasicNewResourceWizard.selectAndReveal(mProject, TheWindow);

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
    void createProject(IProjectDescription description, IProject project,
	    IProgressMonitor monitor) throws OperationCanceledException {

	monitor.beginTask("", 2000);
	try {
	    project.create(description, new SubProgressMonitor(monitor, 1000));

	    if (monitor.isCanceled()) {
		throw new OperationCanceledException();
	    }

	    project.open(IResource.BACKGROUND_REFRESH, new SubProgressMonitor(
		    monitor, 1000));
	    IContainer container = project;
	    String s = "it.baeyens.arduino.core.toolChain.release";
	    ShouldHaveBeenInCDT.setCProjectDescription(project,
		    ManagedBuildManager.getExtensionToolChain(s), "Release",
		    true, monitor); // this creates the .cproject file

	    // Add the C C++ AVR and other needed Natures to the project
	    ArduinoHelpers.addTheNatures(project);

	    // Add the arduino folder
	    ArduinoHelpers.createNewFolder(project, "arduino", null);

	    // Set the environment variables
	    ICProjectDescription prjDesc = CoreModel.getDefault()
		    .getProjectDescription(project);
	    ICConfigurationDescription configurationDescription = prjDesc
		    .getConfigurationByName("Release");
	    mArduinoPage.saveAllSelections(configurationDescription);

	    ArduinoHelpers.setTheEnvironmentVariables(project,
		    configurationDescription, true);

	    // Set the path variables
	    IPath platformPath = new Path(new File(
		    mArduinoPage.mPageLayout.mControlBoardsTxtFile.getText()
			    .trim()).getParent())
		    .append(ArduinoConst.PLATFORM_FILE_NAME);
	    ArduinoHelpers.setProjectPathVariables(project,
		    platformPath.removeLastSegments(1));

	    // intermediately save or the adding code will fail
	    prjDesc.setActiveConfiguration(configurationDescription);

	    // Insert The Arduino Code
	    ArduinoHelpers.addArduinoCodeToProject(project,
		    configurationDescription);

	    /* Add the sketch source code file */
	    ArduinoHelpers.addFileToProject(container,
		    new Path(project.getName() + ".cpp"), Stream
			    .openContentStream(project.getName(), "",
				    "templates/sketch.cpp"), monitor);

	    /* Add the sketch header file */
	    String Include = "WProgram.h";
	    if (ArduinoInstancePreferences.isArduinoIdeOne()) // this is Arduino
							      // version 1.0
	    {
		Include = "Arduino.h";
	    }
	    ArduinoHelpers.addFileToProject(container,
		    new Path(project.getName() + ".h"), Stream
			    .openContentStream(project.getName(), Include,
				    "templates/sketch.h"), monitor);

	    // exclude "Librarie/*/?xample from the build
	    // ICSourceEntry[] folder;
	    // folder = configurationDescription.getSourceEntries();
	    // char[][] exclusions = entry.fullExclusionPatternChars();
	    // CoreModelUtil.isExcluded(project.getFullPath(), exclusions);
	    // folder[0].
	    // getResourceDescription(new Path(""), true);
	    // folder[0].createFilter(IResourceFilterDescription.EXCLUDE_ALL |
	    // IResourceFilterDescription.FOLDERS, new
	    // FileInfoMatcherDescription("org.eclipse.core.resources.regexFilterMatcher",
	    // "Librarie/*/?xample"), IResource.BACKGROUND_REFRESH, monitor);

	    // IFile file = project.getFile("Librarie/*/?xample");
	    // Object activeConfig;
	    // IResourceConfiguration ResConfig = configurationDescription.
	    // .createResourceConfiguration(file);
	    // ResConfig.setExclude(true);

	    ICResourceDescription cfgd = configurationDescription
		    .getResourceDescription(new Path(""), true);
	    ICExclusionPatternPathEntry[] entries = cfgd.getConfiguration()
		    .getSourceEntries();
	    if (entries.length == 1) {
		Path exclusionPath[] = new Path[1];
		exclusionPath[0] = new Path("Libraries/*/?xamples");
		ICExclusionPatternPathEntry newSourceEntry = new CSourceEntry(
			entries[0].getFullPath(), exclusionPath,
			ICSettingEntry.VALUE_WORKSPACE_PATH);
		ICSourceEntry[] out = null;
		out = new ICSourceEntry[1];
		out[0] = (ICSourceEntry) newSourceEntry;
		try {
		    cfgd.getConfiguration().setSourceEntries(out);
		} catch (CoreException e) {
		    // ignore
		}

	    } else {
		// this should not happen
	    }

	    // private void saveData() {
	    // ICExclusionPatternPathEntry[] p = new
	    // ICExclusionPatternPathEntry[src.size()];
	    // Iterator<_Entry> it = src.iterator();
	    // int i=0;
	    // while(it.hasNext()) { p[i++] = (it.next()).ent; }
	    // setEntries(cfgd, p);
	    // tree.setInput(cfgd);
	    // updateData(cfgd);
	    // if (page instanceof AbstractPage) {
	    // ICConfigurationDescription cfgDescription =
	    // cfgd.getConfiguration();
	    // ((AbstractPage)page).cfgChanged(cfgDescription);
	    // }
	    // }

	    prjDesc.setActiveConfiguration(configurationDescription);
	    prjDesc.setCdtProjectCreated();
	    CoreModel.getDefault().getProjectDescriptionManager()
		    .setProjectDescription(project, prjDesc, true, null);
	    monitor.done();

	} catch (CoreException e) {
	    Common.log(new Status(IStatus.ERROR, ArduinoConst.CORE_PLUGIN_ID,
		    "Failed to create project " + project.getName(), e));
	    throw new OperationCanceledException();
	}

    }

    @Override
    public void init(IWorkbench workbench, IStructuredSelection selection) {
	// snipped...
    }

    @Override
    public void setInitializationData(IConfigurationElement config,
	    String propertyName, Object data) throws CoreException {
	// snipped...
	mConfig = config;

    }

}
