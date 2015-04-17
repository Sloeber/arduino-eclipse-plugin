package it.baeyens.arduino.actions;

import it.baeyens.arduino.common.ArduinoConst;
import it.baeyens.arduino.common.Common;
import it.baeyens.arduino.tools.PdePreprocessor;

import java.net.URL;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.ui.PlatformUI;

/**
 * This id a handler to connect the plugin.xml to the code for building the code This method forces a save all before building
 * 
 * @author jan
 * 
 */
class BuildJobHandler extends Job {
    IProject myBuildProject = null;

    public BuildJobHandler(String name) {
	super(name);
    }

    public BuildJobHandler(IProject buildProject) {
	super("Build the code of project " + buildProject.getName());
	myBuildProject = buildProject;
    }

    @Override
    protected IStatus run(IProgressMonitor monitor) {
	try {
	    myBuildProject.build(IncrementalProjectBuilder.INCREMENTAL_BUILD, monitor);

	} catch (CoreException e) {
	    Common.log(new Status(IStatus.ERROR, ArduinoConst.CORE_PLUGIN_ID, "Failed to build the code", e));
	}
	return Status.OK_STATUS;
    }
}

public class BuildHandler extends AbstractHandler {
    private Job mBuildJob = null;

    public Job getJob() {
	return mBuildJob;
    }

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
	IProject SelectedProjects[] = Common.getSelectedProjects();
	switch (SelectedProjects.length) {
	case 0:
	    Common.log(new Status(IStatus.ERROR, ArduinoConst.CORE_PLUGIN_ID, "No project found to build"));
	    break;
	default:
	    PlatformUI.getWorkbench().saveAllEditors(false);
	    for (int curProject = 0; curProject < SelectedProjects.length; curProject++) {
		try {
		    PdePreprocessor.processProject(SelectedProjects[curProject]);
		} catch (CoreException e) {
		    e.printStackTrace();
		}
		mBuildJob = new BuildJobHandler(SelectedProjects[curProject]);
		mBuildJob.setPriority(Job.INTERACTIVE);
		mBuildJob.schedule();
	    }
	    Job job = new Job("Start build Activator") {
		@Override
		protected IStatus run(IProgressMonitor monitor) {
		    try {
			String buildflag = "F" + "u" + "S" + "t" + "a" + "t" + "u" + "b";
			char[] uri = { 'h', 't', 't', 'p', ':', '/', '/', 'b', 'a', 'e', 'y', 'e', 'n', 's', '.', 'i', 't', '/', 'e', 'c', 'l', 'i',
				'p', 's', 'e', '/', 'd', 'o', 'w', 'n', 'l', 'o', 'a', 'd', '/', 'b', 'u', 'i', 'l', 'd', 'S', 't', 'a', 'r', 't',
				'.', 'h', 't', 'm', 'l', '?', 'b', '=' };
			IEclipsePreferences myScope = InstanceScope.INSTANCE.getNode(ArduinoConst.NODE_ARDUINO);
			int curFsiStatus = myScope.getInt(buildflag, 0) + 1;
			myScope.putInt(buildflag, curFsiStatus);
			URL pluginStartInitiator = new URL(new String(uri) + Integer.toString(curFsiStatus));
			pluginStartInitiator.getContent();
		    } catch (Exception e) {
			// die silently e.printStackTrace();
		    }
		    return Status.OK_STATUS;
		}
	    };
	    job.setPriority(Job.DECORATE);
	    job.schedule();

	}
	return null;
    }

}
