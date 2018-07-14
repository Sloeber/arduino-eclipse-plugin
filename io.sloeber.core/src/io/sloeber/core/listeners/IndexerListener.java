package io.sloeber.core.listeners;

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
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;

import io.sloeber.core.Activator;
import io.sloeber.core.Messages;
import io.sloeber.core.common.Common;
import io.sloeber.core.common.Const;
import io.sloeber.core.common.InstancePreferences;
import io.sloeber.core.tools.Libraries;

public class IndexerListener implements IIndexChangeListener, IIndexerStateListener {
	protected Set<IProject> ChangedProjects = new HashSet<>();
	Job installLibJob = null;

	@Override
	public void indexChanged(IIndexChangeEvent event) {
		IProject project = event.getAffectedProject().getProject();
		try {
			if (project.hasNature(Const.ARDUINO_NATURE_ID)) {
				this.ChangedProjects.add(project);
			}
		} catch (CoreException e) {
			// ignore
			e.printStackTrace();
		}

	}

	@Override
	public void indexChanged(IIndexerStateEvent event) {

		if (event.indexerIsIdle()) {
			if (InstancePreferences.getAutomaticallyImportLibraries()) {
				if ((this.installLibJob == null) || (this.installLibJob.getState() == Job.NONE)) {
					this.installLibJob = new Job("Adding Arduino libs...") { //$NON-NLS-1$

						@Override
						protected IStatus run(IProgressMonitor monitor) {
							try {
								for (IProject curProject : IndexerListener.this.ChangedProjects) {
									Libraries.checkLibraries(curProject);
								}
								IndexerListener.this.ChangedProjects.clear();
							} catch (Exception e) {
								Common.log(new Status(IStatus.WARNING, Activator.getId(),
										Messages.Failed_To_Add_Libraries, e));
							}
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
