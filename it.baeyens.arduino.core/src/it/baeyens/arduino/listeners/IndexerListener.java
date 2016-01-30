package it.baeyens.arduino.listeners;

/**
 * this index listener makes it possible to detect missing libraries
 * if configured to do so libraries are added automatically to the project
 */
import java.util.HashSet;
import java.util.Set;

import org.eclipse.cdt.core.index.IIndexChangeEvent;
import org.eclipse.cdt.core.index.IIndexChangeListener;
import org.eclipse.cdt.core.index.IIndexerStateEvent;
import org.eclipse.cdt.core.index.IIndexerStateListener;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;

import it.baeyens.arduino.common.InstancePreferences;
import it.baeyens.arduino.tools.Libraries;

public class IndexerListener implements IIndexChangeListener, IIndexerStateListener {
    protected Set<IProject> ChangedProjects = new HashSet<>();
    Job installLibJob = null;

    @Override
    public void indexChanged(IIndexChangeEvent event) {
	this.ChangedProjects.add(event.getAffectedProject().getProject());

    }

    @Override
    public void indexChanged(IIndexerStateEvent event) {

	if (event.indexerIsIdle()) {
	    if (InstancePreferences.getAutomaticallyIncludeLibraries()) {
		if (this.installLibJob == null) {
		    this.installLibJob = new Job("Adding Arduino libs...") { //$NON-NLS-1$

			@Override
			protected IStatus run(IProgressMonitor monitor) {
			    for (IProject curProject : IndexerListener.this.ChangedProjects) {
				Libraries.checkLibraries(curProject);
			    }
			    IndexerListener.this.ChangedProjects.clear();
			    IndexerListener.this.installLibJob = null;
			    return Status.OK_STATUS;
			}

		    };

		    this.installLibJob.setPriority(Job.DECORATE);
		    this.installLibJob.schedule();
		}

	    }

	}
    }

}
