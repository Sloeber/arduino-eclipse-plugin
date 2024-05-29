package io.sloeber.core.listeners;

import static io.sloeber.core.api.Const.*;

import java.util.ArrayList;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.ICoreRunnable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;

import io.sloeber.core.api.Common;

public class resourceChangeListener implements IResourceChangeListener {

    @Override
    public void resourceChanged(IResourceChangeEvent event) {
        // we are only interested in POST_CHANGE events
        if (event.getType() != IResourceChangeEvent.POST_CHANGE)
            return;
        final ArrayList<IProject> changedSloeberCfgFiles = new ArrayList<>();
        IResourceDelta rootDelta = event.getDelta();
        for (IResourceDelta projectDelta : rootDelta.getAffectedChildren()) {
            IResourceDelta sloeberCfgDelta = projectDelta.findMember(new Path(SLOEBER_CFG));
            if (sloeberCfgDelta != null) {
                if (sloeberCfgDelta.getKind() != IResourceDelta.REMOVED) {
                    //the sloeber.cfg file has been added or changed
                    IProject iProject = sloeberCfgDelta.getResource().getProject();
                    changedSloeberCfgFiles.add(iProject);
                }
            }
        }

        // ignore when no sloeber.cfg files have been modified
        if (changedSloeberCfgFiles.size() == 0)
            return;

        Job job = new Job("Sloeber.cfg modification processor") { //$NON-NLS-1$
            @Override
            protected IStatus run(IProgressMonitor monitor) {
                final IWorkspace workspace = ResourcesPlugin.getWorkspace();
                IWorkspaceRoot root = workspace.getRoot();
                ICoreRunnable runnable = new ICoreRunnable() {

                    @Override
                    public void run(IProgressMonitor monitor2) throws CoreException {
                        for (IProject curProject : changedSloeberCfgFiles) {
                        	curProject.close(monitor2);
                        	curProject.open(monitor2);
                        }
                    }
                };

                try {
                    workspace.run(runnable, root, IWorkspace.AVOID_UPDATE, null);
                } catch (Exception e) {
                    Common.log(new Status(IStatus.INFO, io.sloeber.core.Activator.getId(),
                            "failed to start sloeber.cfg updater", e)); //$NON-NLS-1$
                }

                return Status.OK_STATUS;
            }
        };
        job.setPriority(Job.SHORT);
        job.schedule();

    }

}
