package io.sloeber.autoBuild.internal;


import java.util.ArrayList;

import org.eclipse.core.resources.IFile;
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
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;

import io.sloeber.autoBuild.core.Activator;
import io.sloeber.autoBuild.integration.AutoBuildConfigurationDescriptionProvider;


public class AutobuilResourceChangeListener implements IResourceChangeListener {

    @Override
    public void resourceChanged(IResourceChangeEvent event) {
        // we are only interested in POST_CHANGE events
        if (event.getType() != IResourceChangeEvent.POST_CHANGE)
            return;
        final ArrayList<IProject> changedCfgFiles = new ArrayList<>();
        IResourceDelta rootDelta = event.getDelta();
        for (IResourceDelta resourceDelta : rootDelta.getAffectedChildren()) {
        	IProject iProject= resourceDelta.getResource().getProject();
        	IFile cfgFile=AutoBuildConfigurationDescriptionProvider.getTeamFile(iProject);
            IResourceDelta cfgDelta = resourceDelta.findMember(cfgFile.getProjectRelativePath());
            if (cfgDelta != null) {
                if (cfgDelta.getKind() != IResourceDelta.REMOVED) {
                    //the autobuild.cfg file has been added or changed
                    changedCfgFiles.add(iProject);
                }
            }
        }

        // ignore when no autoBuild.cfg files have been modified
        if (changedCfgFiles.size() == 0)
            return;

        Job job = new Job("autobuild.cfg modification processor") { //$NON-NLS-1$
            @Override
            protected IStatus run(IProgressMonitor monitor) {
                final IWorkspace workspace = ResourcesPlugin.getWorkspace();
                IWorkspaceRoot root = workspace.getRoot();
                ICoreRunnable runnable = new ICoreRunnable() {

                    @Override
                    public void run(IProgressMonitor monitor2) throws CoreException {
                        for (IProject curProject : changedCfgFiles) {
                        	curProject.close(monitor2);
                        	curProject.open(monitor2);
                        }
                    }
                };

                try {
                    workspace.run(runnable, root, IWorkspace.AVOID_UPDATE, null);
                } catch (Exception e) {
                    Activator.log(new Status(IStatus.INFO, Activator.getId(),
                            "failed to start autobuild.cfg updater", e)); //$NON-NLS-1$
                }

                return Status.OK_STATUS;
            }
        };
        job.setPriority(Job.SHORT);
        job.schedule();

    }

}
