package io.sloeber.core.ui.launchconfig;

import java.util.ArrayList;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationType;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.debug.ui.ILaunchShortcut;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.ide.ResourceUtil;

import io.sloeber.core.api.LaunchConfiguration;

public class LaunchShortcut implements ILaunchShortcut {

    /**
     * The file handle from which this launch shortcut has been startet.
     */
    private IFile file;
    /**
     * The project handle from which this launch shortcut has been startet.
     */
    private IProject project;

    /**
     * The mode of the launch ("run" or "debug").
     */
    private String mode;

    /**
     * {@inheritDoc}
     */
    @Override
    public void launch(IEditorPart editor, String launchMode) {
	IFile editorFile = ResourceUtil.getFile(editor.getEditorInput());
	if (editorFile != null) {
	    launch(editorFile, launchMode);
	}
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void launch(ISelection selection, String launchMode) {
	if (selection instanceof IStructuredSelection) {
	    IStructuredSelection structuredSelection = (IStructuredSelection) selection;
	    if (structuredSelection.getFirstElement() instanceof IFile) {
		launch((IFile) structuredSelection.getFirstElement(), launchMode);
	    }
	}
    }

    /**
     * Launch the project of the file via an existing launch configuration or
     * create a new one if there is none yet.
     * 
     * @param file
     *            The file to be launched
     * @param mode
     *            The mode the launch should be performed in (e.g. 'run' or
     *            'debug')
     */
    private void launch(IFile launchFile, String launchMode) {
	this.file = launchFile;
	this.project = this.file.getProject();
	this.mode = launchMode;

	// Find launch config for the project or initialize new one.
	ILaunchConfiguration config = findOrCreateLaunchConfiguration();
	// Launch
	DebugUITools.launch(config, this.mode);
    }

    /**
     * Searches for a launch configuration in the project. Creates a new one if
     * none found.
     * 
     * @param mode
     *            The mode the launch should be performed in (e.g. 'run' or
     *            'debug')
     * @return launch configuration for the project.
     */
    private ILaunchConfiguration findOrCreateLaunchConfiguration() {
	ArrayList<ILaunchConfiguration> configs = getLaunchConfigurations();
	ILaunchConfiguration config;
	if (configs.isEmpty()) {
	    config = createNewConfiguration();
	} else {
	    config = configs.get(0);
	}
	return config;
    }

    /**
     * Creates and initializes a new launch config for the project.
     * 
     * @return the new launch configuration
     */
    private ILaunchConfiguration createNewConfiguration() {
	try {
	    ILaunchManager lm = DebugPlugin.getDefault().getLaunchManager();
	    ILaunchConfigurationType type = lm
		    .getLaunchConfigurationType(LaunchConfiguration.LAUNCH_CONFIGURATION_TYPE_ID);
	    // Infere name of launch config from project name
	    String name = this.project.getName();
	    // Create launch config
	    ILaunchConfigurationWorkingCopy wc = type.newInstance(null, name);
	    initializeConfiguration(wc);
	    return wc.doSave();
	} catch (CoreException ce) {
	    // Stupid Exception
	}
	return null;
    }

    /**
     * Initializes a new launch config for the project. The main file and
     * environment used are loaded from the project's properties if possible or
     * from dialogs if not.
     * 
     * @param config
     *            The launch configuration to be initialized
     */

    private void initializeConfiguration(ILaunchConfigurationWorkingCopy config) {
	// Set project
	config.setAttribute(LaunchConfiguration.ATTR_PROJECT, this.project.getName());
    }

    /**
     * Searches for all applicable launch configurations for this project.
     * 
     * @return list with the launch configurations.
     */
    private ArrayList<ILaunchConfiguration> getLaunchConfigurations() {
	ArrayList<ILaunchConfiguration> result = new ArrayList<>();
	try {
	    ILaunchManager manager = DebugPlugin.getDefault().getLaunchManager();
	    ILaunchConfigurationType type = manager
		    .getLaunchConfigurationType(LaunchConfiguration.LAUNCH_CONFIGURATION_TYPE_ID);
	    ILaunchConfiguration[] configurations = manager.getLaunchConfigurations(type);
	    for (int i = 0; i < configurations.length; i++) {
		ILaunchConfiguration config = configurations[i];
		if (!DebugUITools.isPrivate(config) && isGoodMatch(config)) {
		    result.add(config);
		}
	    }
	} catch (CoreException e) {
	    // Stupid Exception
	}
	return result;
    }

    /**
     * Checks if the launch configuration is for this project.
     * 
     * @param configuration
     *            The configuration to be checked
     */
    private boolean isGoodMatch(ILaunchConfiguration configuration) {
	try {
	    String projectName = configuration.getAttribute(LaunchConfiguration.ATTR_PROJECT, ""); //$NON-NLS-1$
	    return projectName.equals(this.project.getName());
	} catch (CoreException e) {
	    // Stupid exception...
	}
	return false;
    }
}
