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
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;

import io.sloeber.core.Activator;
import io.sloeber.core.Messages;
import io.sloeber.core.common.Common;
import io.sloeber.core.common.Const;
import io.sloeber.core.common.InstancePreferences;
import io.sloeber.core.tools.Libraries;

public class IndexerListener implements IIndexChangeListener, IIndexerStateListener {
    private static Set<IProject> newChangedProjects = new HashSet<>();

    @Override
    public void indexChanged(IIndexChangeEvent event) {
        IProject project = event.getAffectedProject().getProject();
        try {
            if (project.hasNature(Const.ARDUINO_NATURE_ID)) {
                if (!newChangedProjects.contains(project)) {
                    Common.log(new Status(Const.SLOEBER_STATUS_DEBUG, Activator.getId(),
                            "Index of project changed :" + project.getName())); //$NON-NLS-1$
                    newChangedProjects.add(project);
                }
            }
        } catch (CoreException e) {
            // ignore
            e.printStackTrace();
        }

    }

    @Override
    public void indexChanged(IIndexerStateEvent event) {

        if (event.indexerIsIdle()) {
            if (!newChangedProjects.isEmpty()) {
                Set<IProject> curChangedProjects = new HashSet<>(newChangedProjects);
                newChangedProjects.clear();
                if (InstancePreferences.getAutomaticallyImportLibraries()) {
                    for (IProject curProject : curChangedProjects) {
                        updateLibraries(curProject);
                    }
                }
            }
        }
    }

    private static void updateLibraries(IProject updateProject) {
        final QualifiedName PROJECT = new QualifiedName("SLOEBER", "project"); //$NON-NLS-1$ //$NON-NLS-2$
        Job installLibJob = new Job("Adding Arduino libraries to project " + updateProject.getName()) { //$NON-NLS-1$

            @Override
            protected IStatus run(IProgressMonitor monitor) {
                IProject project = (IProject) getProperty(PROJECT);
                try {
                    Common.log(new Status(Const.SLOEBER_STATUS_DEBUG, Activator.getId(),
                            "Looking for libraries for project :" + project.getName())); //$NON-NLS-1$
                    Libraries.checkLibraries(project);

                } catch (Exception e) {
                    Common.log(new Status(IStatus.WARNING, Activator.getId(), Messages.Failed_To_Add_Libraries, e));
                }
                Common.log(new Status(Const.SLOEBER_STATUS_DEBUG, Activator.getId(),
                        "libraries added for project " + project.getName())); //$NON-NLS-1$
                return Status.OK_STATUS;
            }

        };
        installLibJob.setProperty(PROJECT, updateProject);
        installLibJob.setPriority(Job.DECORATE);
        installLibJob.schedule();
    }

}
