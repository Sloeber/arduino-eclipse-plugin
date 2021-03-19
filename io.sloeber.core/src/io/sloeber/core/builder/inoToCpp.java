package io.sloeber.core.builder;

import java.util.Map;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

import io.sloeber.core.tools.PdePreprocessor;

public class inoToCpp extends IncrementalProjectBuilder {

    class SampleDeltaVisitor implements IResourceDeltaVisitor {
        /*
         * (non-Javadoc)
         *
         * @see org.eclipse.core.resources.IResourceDeltaVisitor#visit(org.eclipse.
         * core.resources.IResourceDelta)
         */
        @Override
        public boolean visit(IResourceDelta delta) throws CoreException {
            IResource resource = delta.getResource();
            if (resource.getFileExtension() != null) {
                if (resource.getFileExtension().equalsIgnoreCase("ino") //$NON-NLS-1$
                        || resource.getFileExtension().equalsIgnoreCase("pde")) { //$NON-NLS-1$
                    try {
                        PdePreprocessor.processProject(true, getProject());
                    } catch (CoreException e) {
                        e.printStackTrace();
                    }
                    return false;
                }
            }
            return true;
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.core.internal.events.InternalBuilder#build(int,
     * java.util.Map, org.eclipse.core.runtime.IProgressMonitor)
     */
    @Override
    protected IProject[] build(int kind, @SuppressWarnings("rawtypes") Map args, IProgressMonitor monitor)
            throws CoreException {
        if (kind == FULL_BUILD) {
            fullBuild(monitor);
        } else {
            IResourceDelta delta = getDelta(getProject());
            if (delta == null) {
                fullBuild(monitor);
            } else {
                incrementalBuild(delta, monitor);
            }
        }
        return null;
    }

    @Override
    protected void clean(IProgressMonitor monitor) throws CoreException {
        PdePreprocessor.deleteSloeberInoCPPFile(getProject());
    }

    @SuppressWarnings("unused")
    protected void fullBuild(final IProgressMonitor monitor) {
        try {
            PdePreprocessor.processProject(false, getProject());
        } catch (CoreException e) {
            e.printStackTrace();
        }
    }

    @SuppressWarnings("unused")
    protected void incrementalBuild(IResourceDelta delta, IProgressMonitor monitor) throws CoreException {
        // the visitor does the work.
        delta.accept(new SampleDeltaVisitor());

    }
}
