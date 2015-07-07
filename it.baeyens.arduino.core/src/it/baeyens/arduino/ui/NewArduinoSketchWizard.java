package it.baeyens.arduino.ui;

import it.baeyens.arduino.common.ArduinoConst;
import it.baeyens.arduino.common.ArduinoInstancePreferences;
import it.baeyens.arduino.common.Common;
import it.baeyens.arduino.ide.connector.ArduinoGetPreferences;
import it.baeyens.arduino.tools.ArduinoHelpers;
import it.baeyens.arduino.tools.ShouldHaveBeenInCDT;
import it.baeyens.arduino.ui.BuildConfigurationsPage.ConfigurationDescriptor;

import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.util.ArrayList;

import org.eclipse.cdt.core.CCorePlugin;
import org.eclipse.cdt.core.envvar.EnvironmentVariable;
import org.eclipse.cdt.core.envvar.IContributedEnvironment;
import org.eclipse.cdt.core.envvar.IEnvironmentVariable;
import org.eclipse.cdt.core.envvar.IEnvironmentVariableManager;
import org.eclipse.cdt.core.model.CoreModel;
import org.eclipse.cdt.core.settings.model.CSourceEntry;
import org.eclipse.cdt.core.settings.model.ICConfigurationDescription;
import org.eclipse.cdt.core.settings.model.ICExclusionPatternPathEntry;
import org.eclipse.cdt.core.settings.model.ICProjectDescription;
import org.eclipse.cdt.core.settings.model.ICResourceDescription;
import org.eclipse.cdt.core.settings.model.ICSettingEntry;
import org.eclipse.cdt.core.settings.model.ICSourceEntry;
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
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
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
public class NewArduinoSketchWizard extends Wizard implements INewWizard, IExecutableExtension {

    private WizardNewProjectCreationPage mWizardPage; // first page of the dialog
    protected NewArduinoSketchWizardCodeSelectionPage mNewArduinoSketchWizardCodeSelectionPage; // add the folder for the templates
    protected NewArduinoSketchWizardBoardPage mArduinoPage; // add Arduino board and comp port
    private BuildConfigurationsPage mBuildCfgPage; // build the configuration
    private IConfigurationElement mConfig;
    private IProject mProject;

    public NewArduinoSketchWizard() {
	super();
    }

    @Override
    /**
     * adds pages to the wizard. We are using the standard project wizard of Eclipse
     */
    public void addPages() {
	//
	// We assume everything is OK as it is tested in the handler
	// create each page and fill in the title and description
	// first page to fill in the project name
	//
	mWizardPage = new WizardNewProjectCreationPage("New Arduino sketch");
	mWizardPage.setDescription("Create a new Arduino sketch.");
	mWizardPage.setTitle("New Arduino sketch");
	//
	// settings for Arduino board etc
	//
	mArduinoPage = new NewArduinoSketchWizardBoardPage("Arduino information");
	mArduinoPage.setTitle("Provide the Arduino information.");
	mArduinoPage.setDescription("These settings can be changed later.");
	//
	// settings for template file location
	//
	mNewArduinoSketchWizardCodeSelectionPage = new NewArduinoSketchWizardCodeSelectionPage("Sketch Template location");
	mNewArduinoSketchWizardCodeSelectionPage.setTitle("Provide the sketch template folder");
	mNewArduinoSketchWizardCodeSelectionPage.setDescription("The folder must contain a sketch.cpp and sketch.h");
	//
	// configuration page but I haven't seen it
	//
	mBuildCfgPage = new BuildConfigurationsPage("Build configurations");
	mBuildCfgPage.setTitle("Select additional build configurations for this project.");
	mBuildCfgPage.setDescription("If you are using additional tools you may want one or more of these extra configurations.");
	//
	// actually add the pages to the wizard
	// /
	addPage(mWizardPage);
	addPage(mArduinoPage);
	addPage(mNewArduinoSketchWizardCodeSelectionPage);
	addPage(mBuildCfgPage);

	mArduinoPage.setListener(new Listener() {

	    @Override
	    public void handleEvent(Event event) {
		if (event == null) {
		    mNewArduinoSketchWizardCodeSelectionPage.removeExamples();
		} else {
		    IPath PlatformPath = mArduinoPage.getPlatformFolder().append(ArduinoConst.LIBRARY_PATH_SUFFIX);
		    IPath arduinoExample = ArduinoInstancePreferences.getArduinoPath().append(ArduinoConst.ARDUINO_EXAMPLE_FOLDER_NAME);
		    IPath arduinoLibPath = ArduinoInstancePreferences.getArduinoPath().append(ArduinoConst.LIBRARY_PATH_SUFFIX);
		    IPath privateLibrary = new Path(ArduinoInstancePreferences.getPrivateLibraryPath());

		    mNewArduinoSketchWizardCodeSelectionPage.AddAllExamples(arduinoExample, arduinoLibPath, privateLibrary, PlatformPath);
		}

	    }
	});

    }

    /**
     * this method is required by IWizard otherwise nothing will actually happen
     */
    @Override
    public boolean performFinish() {
	//
	// if the project is filled in then we are done
	//
	if (mProject != null) {
	    return true;
	}
	//
	// get an IProject handle to our project
	//
	final IProject projectHandle = ResourcesPlugin.getWorkspace().getRoot().getProject(Common.MakeNameCompileSafe(mWizardPage.getProjectName()));
	//
	// let's validate it
	//
	try {
	    //
	    // get the URL if it is filled in. This depends on the check box "use defaults" is checked
	    // or not
	    //
	    URI projectURI = (!mWizardPage.useDefaults()) ? mWizardPage.getLocationURI() : null;
	    //
	    // get the workspace name
	    //
	    IWorkspace workspace = ResourcesPlugin.getWorkspace();
	    //
	    // the project descriptions is set equal to the name of the project
	    //
	    final IProjectDescription desc = workspace.newProjectDescription(projectHandle.getName());
	    //
	    // get our workspace location
	    //
	    desc.setLocationURI(projectURI);

	    /*
	     * Just like the ExampleWizard, but this time with an operation object that modifies workspaces.
	     */
	    WorkspaceModifyOperation op = new WorkspaceModifyOperation() {
		@Override
		protected void execute(IProgressMonitor monitor) throws CoreException {
		    //
		    // actually create the project
		    //
		    createProject(desc, projectHandle, monitor);
		}
	    };

	    /*
	     * This isn't as robust as the code in the BasicNewProjectResourceWizard class. Consider beefing this up to improve error handling.
	     */
	    getContainer().run(false, true, op);
	} catch (InterruptedException e) {
	    return false;
	} catch (InvocationTargetException e) {
	    Throwable realException = e.getTargetException();
	    MessageDialog.openError(getShell(), "Error", realException.getMessage());
	    return false;
	}
	//
	// so the project is created we can start
	//
	mProject = projectHandle;

	if (mProject == null) {
	    return false;
	}
	//
	// so now we set Eclipse to the right perspective and switch to our just created
	// project
	//
	BasicNewProjectResourceWizard.updatePerspective(mConfig);
	IWorkbenchWindow TheWindow = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
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
    void createProject(IProjectDescription description, IProject project, IProgressMonitor monitor) throws OperationCanceledException {

	monitor.beginTask("", 2000);
	try {
	    project.create(description, new SubProgressMonitor(monitor, 1000));

	    if (monitor.isCanceled()) {
		throw new OperationCanceledException();
	    }

	    project.open(IResource.BACKGROUND_REFRESH, new SubProgressMonitor(monitor, 1000));

	    // Get the Build Configurations (names and toolchain IDs) from the property page
	    ArrayList<ConfigurationDescriptor> cfgNamesAndTCIds = mBuildCfgPage.getBuildConfigurationDescriptors();

	    // Creates the .cproject file with the configurations
	    ICProjectDescription prjCDesc = ShouldHaveBeenInCDT.setCProjectDescription(project, cfgNamesAndTCIds, true, monitor);

	    // Add the C C++ AVR and other needed Natures to the project
	    ArduinoHelpers.addTheNatures(description);

	    // Add the Arduino folder
	    ArduinoHelpers.createNewFolder(project, "arduino", null);

	    // since Arduino IDE 1.6.5 a request to arduino IDE needs to be done to know the needed environment variables
	    ArduinoGetPreferences.generateDumpFileForBoardIfNeeded(mArduinoPage.getPackage(), mArduinoPage.getArchitecture(),
		    mArduinoPage.getBoardID(), monitor);

	    for (int i = 0; i < cfgNamesAndTCIds.size(); i++) {
		ICConfigurationDescription configurationDescription = prjCDesc.getConfigurationByName(cfgNamesAndTCIds.get(i).Name);
		mArduinoPage.saveAllSelections(configurationDescription);
		ArduinoHelpers.setTheEnvironmentVariables(project, configurationDescription, cfgNamesAndTCIds.get(i).DebugCompilerSettings);
	    }

	    // Set the path variables
	    ArduinoHelpers.setProjectPathVariables(prjCDesc.getActiveConfiguration());

	    // Intermediately save or the adding code will fail
	    // Release is the active config (as that is the "IDE" Arduino type....)
	    ICConfigurationDescription defaultConfigDescription = prjCDesc.getConfigurationByName(cfgNamesAndTCIds.get(0).Name);
	    prjCDesc.setActiveConfiguration(defaultConfigDescription);

	    // Insert The Arduino Code
	    // NOTE: Not duplicated for debug (the release reference is just to get at some environment variables)
	    ArduinoHelpers.addArduinoCodeToProject(project, defaultConfigDescription);

	    //
	    // add the correct files to the project
	    //
	    mNewArduinoSketchWizardCodeSelectionPage.createFiles(project, monitor);
	    //
	    // add the libraries to the project if needed
	    //
	    mNewArduinoSketchWizardCodeSelectionPage.importLibraries(project, prjCDesc.getConfigurations());

	    ICResourceDescription cfgd = defaultConfigDescription.getResourceDescription(new Path(""), true);
	    ICExclusionPatternPathEntry[] entries = cfgd.getConfiguration().getSourceEntries();
	    if (entries.length == 1) {
		Path exclusionPath[] = new Path[2];
		exclusionPath[0] = new Path("Libraries/*/?xamples");
		exclusionPath[1] = new Path("Libraries/*/?xtras");
		ICExclusionPatternPathEntry newSourceEntry = new CSourceEntry(entries[0].getFullPath(), exclusionPath,
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

	    // set warning levels default on
	    IEnvironmentVariableManager envManager = CCorePlugin.getDefault().getBuildEnvironmentManager();
	    IContributedEnvironment contribEnv = envManager.getContributedEnvironment();
	    IEnvironmentVariable var = new EnvironmentVariable(ArduinoConst.ENV_KEY_JANTJE_WARNING_LEVEL, ArduinoConst.ENV_KEY_WARNING_LEVEL_ON);
	    contribEnv.addVariable(var, cfgd.getConfiguration());

	    prjCDesc.setActiveConfiguration(defaultConfigDescription);
	    prjCDesc.setCdtProjectCreated();
	    CoreModel.getDefault().getProjectDescriptionManager().setProjectDescription(project, prjCDesc, true, null);
	    project.setDescription(description, new NullProgressMonitor());
	    monitor.done();

	} catch (CoreException e) {
	    Common.log(new Status(IStatus.ERROR, ArduinoConst.CORE_PLUGIN_ID, "Failed to create project " + project.getName(), e));
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
