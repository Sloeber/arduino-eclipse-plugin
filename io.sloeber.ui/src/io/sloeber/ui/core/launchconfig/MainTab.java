package io.sloeber.ui.core.launchconfig;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.internal.ui.SWTFactory;
import org.eclipse.debug.ui.AbstractLaunchConfigurationTab;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.dialogs.ElementListSelectionDialog;

import io.sloeber.core.api.LaunchConfiguration;
import io.sloeber.core.api.Sketch;
import io.sloeber.ui.Messages;

// SWTFactory is a restricted class. Nontheless it is very usefull.
@SuppressWarnings("restriction")
public class MainTab extends AbstractLaunchConfigurationTab {

    Text project;

    @Override
    public void createControl(Composite parent) {
	Composite comp = new Composite(parent, SWT.NONE);
	setControl(comp);

	comp.setLayout(new GridLayout(1, true));
	comp.setFont(parent.getFont());

	createProjectComponent(comp);
    }

    /**
     * Creates the components to select a project.
     * 
     * @param parent
     *            The parent composite
     */
    private void createProjectComponent(Composite parent) {
	Group group = SWTFactory.createGroup(parent, Messages.launch_config_mainTab_project, 2, 1, GridData.FILL_HORIZONTAL);

	// Create text field
	this.project = SWTFactory.createSingleText(group, 1);
	this.project.addModifyListener(new ModifyListener() {
	    @SuppressWarnings("synthetic-access")
	    @Override
	    public void modifyText(ModifyEvent e) {
		updateLaunchConfigurationDialog();
	    }
	});

	// Create browse button
	createProjectSelectionButton(group, this.project);
    }

    /**
     * Creates a button that will open a project selection dialog for Arduino
     * projects. If a project has been selected via the button, the text field
     * input will be changed accordingly.
     * 
     * @param parent
     *            The parent composite
     * @param text
     *            The text field that should adapt to the selected project
     */
    private static void createProjectSelectionButton(Composite parent, Text text) {
	Button browse = SWTFactory.createPushButton(parent, Messages.launch_config_mainTab_browse, null);
	browse.addSelectionListener(new SelectionAdapter() {
	    @Override
	    public void widgetSelected(SelectionEvent e) {

		// Create a dialog with all projects in the workspace as
		// possible selections.
		ElementListSelectionDialog dialog = new ElementListSelectionDialog(parent.getShell(),
			new LabelProvider() {
			    @Override
			    public String getText(Object element) {
				IProject data = (IProject) element;
				if (data != null) {
				    return data.getName();
				}
				return ""; //$NON-NLS-1$
			    }
			});
		dialog.setTitle(Messages.launch_config_mainTab_project_selection);

		// Set the selectable elements of the dialog.
		// A project may be closed
		// so we use only open (accessible) projects as possible
		// selection.
		List<IProject> projects = new ArrayList<>();
		for (IProject p : ResourcesPlugin.getWorkspace().getRoot().getProjects()) {

		    if (p.isAccessible() && Sketch.isSketch(p)) {
			projects.add(p);
		    }
		}
		dialog.setElements(projects.toArray());
		dialog.open();

		// Get result from dialog and set text field value
		Object[] results = dialog.getResult();
		if (results != null && results.length > 0) {
		    IProject result = (IProject) results[0];
		    text.setText(result.getName());
		}
	    }

	});
    }

    /**
     * Checks if the current input makes sense and set an error message
     * accordingly.
     * 
     * @return true if the input is valid. false otherwise
     */
    private boolean checkConsistency() {
	setErrorMessage(checkErrors());
	return getErrorMessage() == null;
    }

    /**
     * Checks if the current input makes sense and returns an error message
     * accordingly.
     * 
     * @return an appropriate error message or null if there is no error.
     */
    private String checkErrors() {
	// Project is specified
	if (StringUtils.isBlank(this.project.getText())) {
	    return Messages.launch_config_mainTab_specify_project;
	}

	// Project exists
	IProject proj = LaunchConfiguration.findProject(this.project.getText());
	if (proj == null) {
	    return Messages.project_does_not_exist;
	}

	// Project has correct nature

	if (!Sketch.isSketch(proj)) {
	    return Messages.launch_config_mainTab_project_wrong_type;
	}

	return null;
    }

    @Override
    public String getName() {
	return Messages.launch_config_mainTab_main;
    }

    @Override
    public void initializeFrom(ILaunchConfiguration launchConfig) {
	try {
	    this.project.setText(launchConfig.getAttribute(LaunchConfiguration.ATTR_PROJECT, "")); //$NON-NLS-1$
	} catch (CoreException e) {
	    e.printStackTrace();
	}
    }

    @Override
    public void performApply(ILaunchConfigurationWorkingCopy launchConfig) {
	launchConfig.setAttribute(LaunchConfiguration.ATTR_PROJECT, this.project.getText());

	// Check the user input for consistency
	checkConsistency();
    }

    @Override
    public void setDefaults(ILaunchConfigurationWorkingCopy launchConfig) {
	// We set default values via a LaunchShortcut
    }

}
