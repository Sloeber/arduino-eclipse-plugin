package io.sloeber.core.api;

import org.apache.commons.lang.StringUtils;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.model.ILaunchConfigurationDelegate;
@SuppressWarnings("unused")
public class LaunchConfiguration implements ILaunchConfigurationDelegate {

    /**
     * The id of the Arduino launch configuration type.
     */
    public static final String LAUNCH_CONFIGURATION_TYPE_ID = "io.sloeber.core.tools.arduinoLaunchConfiguration"; //$NON-NLS-1$

    // Attribute names
    public static final String ATTR_PROJECT = "io.sloeber.core.tools.launchconfig.main.project"; //$NON-NLS-1$

    // Loaded attributes
    /**
     * The project loaded from the configuration.
     */
    IProject project;

    // Attributes of the launch method
    /**
     * The configuration used for this launch
     */
    ILaunchConfiguration config;

    /**
     * The mode of the launch (either "run" or "debug").
     */
    String mode;

    /**
     * The object that is associated with this launch.
     */
    ILaunch launch;

    /**
     * The monitor to show progress.
     */
    IProgressMonitor monitor;

    @Override
    public void launch(ILaunchConfiguration launchConfig, String launchMode, ILaunch launchHandle,
	    IProgressMonitor launchMonitor) throws CoreException {

	this.config = launchConfig;
	this.mode = launchMode;
	this.launch = launchHandle;
	this.monitor = launchMonitor;

	// Get data from config
	loadSettingsFromConfiguration();

	if (this.project != null) {
	    // Delegate launching the project
	    Sketch.upload(this.project);
	}
    }

    private void loadSettingsFromConfiguration() {
	try {
	    String projectName = this.config.getAttribute(ATTR_PROJECT, ""); //$NON-NLS-1$
	    this.project = findProject(projectName);
	} catch (CoreException e) {
	    // Stupid exception...
	}
    }

    /**
     * Searches for a project with the given name.
     * 
     * @param name
     * @return the project handle if a project was found
     */
    public static IProject findProject(String name) {
	if (StringUtils.isNotBlank(name) && new Path(name).isValidPath(name)) {
	    IProject p = ResourcesPlugin.getWorkspace().getRoot().getProject(name);
	    if (p.getLocation() != null)
		return p;
	}
	return null;
    }

}
