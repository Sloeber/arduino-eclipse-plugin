package it.baeyens.arduino.ui;

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
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.WorkspaceModifyOperation;
import org.eclipse.ui.dialogs.WizardNewProjectCreationPage;
import org.eclipse.ui.wizards.newresource.BasicNewProjectResourceWizard;
import org.eclipse.ui.wizards.newresource.BasicNewResourceWizard;

import it.baeyens.arduino.common.ArduinoConst;
import it.baeyens.arduino.common.Common;
import it.baeyens.arduino.tools.ArduinoHelpers;
import it.baeyens.arduino.tools.ShouldHaveBeenInCDT;
import it.baeyens.arduino.ui.BuildConfigurationsPage.ConfigurationDescriptor;

/**
 * This class is the class related to the new arduino sketch
 * 
 * @author Jan Baeyens
 * 
 */
public class NewArduinoSketchWizard extends Wizard implements INewWizard, IExecutableExtension {
    private WizardNewProjectCreationPage mWizardPage; // first page of the
						      // dialog
    protected NewArduinoSketchWizardCodeSelectionPage mNewArduinoSketchWizardCodeSelectionPage; // add
												// the
												// folder
												// for
												// the
												// templates
    protected NewArduinoSketchWizardBoardPage mArduinoPage; // add Arduino board
							    // and comp port
    private BuildConfigurationsPage mBuildCfgPage; // build the configuration
    private IConfigurationElement mConfig;
    private IProject mProject;

    @Override
    public IWizardPage getNextPage(IWizardPage page) {
	// TODO Auto-generated method stub
	// if (page.equals(this.mNewArduinoSketchWizardCodeSelectionPage)) {
	String PlatformPath = NewArduinoSketchWizard.this.mArduinoPage.getPlatformFolder().toString();
	NewArduinoSketchWizard.this.mNewArduinoSketchWizardCodeSelectionPage.setPlatformPath(PlatformPath);
	// }
	return super.getNextPage(page);
    }

    public NewArduinoSketchWizard() {
	super();
    }

    @Override
    /**
     * adds pages to the wizard. We are using the standard project wizard of
     * Eclipse
     */
    public void addPages() {
	//
	// We assume everything is OK as it is tested in the handler
	// create each page and fill in the title and description
	// first page to fill in the project name
	//
	this.mWizardPage = new WizardNewProjectCreationPage(Messages.ui_new_sketch_title);
	this.mWizardPage.setDescription(Messages.ui_new_sketch_title_help);
	this.mWizardPage.setTitle(Messages.ui_new_sketch_title); // $NON-NLS-1$
	//
	// settings for Arduino board etc
	//
	this.mArduinoPage = new NewArduinoSketchWizardBoardPage(Messages.ui_new_sketch_arduino_information);
	this.mArduinoPage.setTitle(Messages.ui_new_sketch_arduino_information_help);
	this.mArduinoPage.setDescription(Messages.ui_new_sketch_these_settings_cn_be_changed_later);
	//
	// settings for template file location
	//
	this.mNewArduinoSketchWizardCodeSelectionPage = new NewArduinoSketchWizardCodeSelectionPage(
		Messages.ui_new_sketch_sketch_template_location);
	this.mNewArduinoSketchWizardCodeSelectionPage.setTitle(Messages.ui_new_sketch_sketch_template_folder);
	this.mNewArduinoSketchWizardCodeSelectionPage
		.setDescription(Messages.ui_new_sketch_error_folder_must_contain_sketch_cpp);
	//
	// configuration page but I haven't seen it
	//
	this.mBuildCfgPage = new BuildConfigurationsPage(Messages.ui_new_sketch_build_configurations);
	this.mBuildCfgPage.setTitle(Messages.ui_new_sketch_Select_additional_configurations);
	this.mBuildCfgPage.setDescription(Messages.ui_new_sketch_Select_additional_configurations_help);
	//
	// actually add the pages to the wizard
	//
	addPage(this.mWizardPage);
	addPage(this.mArduinoPage);
	addPage(this.mNewArduinoSketchWizardCodeSelectionPage);
	addPage(this.mBuildCfgPage);

    }

    /**
     * this method is required by IWizard otherwise nothing will actually happen
     */
    @Override
    public boolean performFinish() {
	//
	// if the project is filled in then we are done
	//
	if (this.mProject != null) {
	    return true;
	}
	//
	// get an IProject handle to our project
	//
	final IProject projectHandle = ResourcesPlugin.getWorkspace().getRoot()
		.getProject(Common.MakeNameCompileSafe(this.mWizardPage.getProjectName()));
	//
	// let's validate it
	//
	try {
	    //
	    // get the URL if it is filled in. This depends on the check box
	    // "use defaults" is checked
	    // or not
	    //
	    URI projectURI = (!this.mWizardPage.useDefaults()) ? this.mWizardPage.getLocationURI() : null;
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
	     * Just like the ExampleWizard, but this time with an operation
	     * object that modifies workspaces.
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
	     * This isn't as robust as the code in the
	     * BasicNewProjectResourceWizard class. Consider beefing this up to
	     * improve error handling.
	     */
	    getContainer().run(false, true, op);
	} catch (InterruptedException e) {
	    return false;
	} catch (InvocationTargetException e) {
	    Throwable realException = e.getTargetException();
	    MessageDialog.openError(getShell(), Messages.error, realException.getMessage()); // $NON-NLS-1$
	    return false;
	}
	//
	// so the project is created we can start
	//
	this.mProject = projectHandle;

	if (this.mProject == null) {
	    return false;
	}
	//
	// so now we set Eclipse to the right perspective and switch to our just
	// created
	// project
	//
	BasicNewProjectResourceWizard.updatePerspective(this.mConfig);
	IWorkbenchWindow TheWindow = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
	BasicNewResourceWizard.selectAndReveal(this.mProject, TheWindow);

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
    void createProject(IProjectDescription description, IProject project, IProgressMonitor monitor)
	    throws OperationCanceledException {

	monitor.beginTask(ArduinoConst.EMPTY_STRING, 2000);
	try {
	    project.create(description, new SubProgressMonitor(monitor, 1000));

	    if (monitor.isCanceled()) {
		throw new OperationCanceledException();
	    }

	    project.open(IResource.BACKGROUND_REFRESH, new SubProgressMonitor(monitor, 1000));

	    // Get the Build Configurations (names and toolchain IDs) from the
	    // property page
	    ArrayList<ConfigurationDescriptor> cfgNamesAndTCIds = this.mBuildCfgPage.getBuildConfigurationDescriptors();

	    // Creates the .cproject file with the configurations
	    ICProjectDescription prjCDesc = ShouldHaveBeenInCDT.setCProjectDescription(project, cfgNamesAndTCIds, true,
		    monitor);

	    // Add the C C++ AVR and other needed Natures to the project
	    ArduinoHelpers.addTheNatures(description);

	    // Add the Arduino folder
	    ArduinoHelpers.createNewFolder(project, ArduinoConst.ARDUINO_CODE_FOLDER_NAME, null);

	    for (int i = 0; i < cfgNamesAndTCIds.size(); i++) {
		ICConfigurationDescription configurationDescription = prjCDesc
			.getConfigurationByName(cfgNamesAndTCIds.get(i).Name);
		this.mArduinoPage.saveAllSelections(configurationDescription);
		ArduinoHelpers.setTheEnvironmentVariables(project, configurationDescription,
			cfgNamesAndTCIds.get(i).DebugCompilerSettings);
	    }

	    // Set the path variables
	    // ArduinoHelpers.setProjectPathVariables(prjCDesc.getActiveConfiguration());

	    // Intermediately save or the adding code will fail
	    // Release is the active config (as that is the "IDE" Arduino
	    // type....)
	    ICConfigurationDescription defaultConfigDescription = prjCDesc
		    .getConfigurationByName(cfgNamesAndTCIds.get(0).Name);
	    prjCDesc.setActiveConfiguration(defaultConfigDescription);

	    // Insert The Arduino Code
	    // NOTE: Not duplicated for debug (the release reference is just to
	    // get at some environment variables)
	    ArduinoHelpers.addArduinoCodeToProject(project, defaultConfigDescription);

	    //
	    // add the correct files to the project
	    //
	    this.mNewArduinoSketchWizardCodeSelectionPage.createFiles(project, monitor);
	    //
	    // add the libraries to the project if needed
	    //
	    this.mNewArduinoSketchWizardCodeSelectionPage.importLibraries(project, prjCDesc.getActiveConfiguration());

	    ICResourceDescription cfgd = defaultConfigDescription
		    .getResourceDescription(new Path(ArduinoConst.EMPTY_STRING), true);
	    ICExclusionPatternPathEntry[] entries = cfgd.getConfiguration().getSourceEntries();
	    if (entries.length == 1) {
		Path exclusionPath[] = new Path[5];
		exclusionPath[0] = new Path(ArduinoConst.LIBRARY_PATH_SUFFIX + "/**/?xamples/**"); //$NON-NLS-1$
		exclusionPath[1] = new Path(ArduinoConst.LIBRARY_PATH_SUFFIX + "/**/?xtras/**"); //$NON-NLS-1$
		exclusionPath[2] = new Path(ArduinoConst.LIBRARY_PATH_SUFFIX + "/**/test/**"); //$NON-NLS-1$
		exclusionPath[3] = new Path(ArduinoConst.LIBRARY_PATH_SUFFIX + "/**/third-party/**"); //$NON-NLS-1$
		exclusionPath[4] = new Path(ArduinoConst.LIBRARY_PATH_SUFFIX + "**/._*"); //$NON-NLS-1$

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
	    IEnvironmentVariable var = new EnvironmentVariable(ArduinoConst.ENV_KEY_JANTJE_WARNING_LEVEL,
		    ArduinoConst.ENV_KEY_WARNING_LEVEL_ON);
	    contribEnv.addVariable(var, cfgd.getConfiguration());

	    prjCDesc.setActiveConfiguration(defaultConfigDescription);
	    prjCDesc.setCdtProjectCreated();
	    CoreModel.getDefault().getProjectDescriptionManager().setProjectDescription(project, prjCDesc, true, null);
	    project.setDescription(description, new NullProgressMonitor());
	    monitor.done();

	} catch (CoreException e) {
	    Common.log(new Status(IStatus.ERROR, ArduinoConst.CORE_PLUGIN_ID,
		    Messages.ui_new_sketch_error_failed_to_create_project + project.getName(), e));
	    throw new OperationCanceledException();
	}

    }

    @Override
    public void init(IWorkbench workbench, IStructuredSelection selection) {
	// snipped...
    }

    @Override
    public void setInitializationData(IConfigurationElement config, String propertyName, Object data)
	    throws CoreException {
	// snipped...
	this.mConfig = config;

    }

}
