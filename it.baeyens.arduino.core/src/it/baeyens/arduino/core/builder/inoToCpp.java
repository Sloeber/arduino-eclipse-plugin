package it.baeyens.arduino.core.builder;

import java.util.Map;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

import it.baeyens.arduino.tools.PdePreprocessor;

public class inoToCpp extends IncrementalProjectBuilder {

    class SampleDeltaVisitor implements IResourceDeltaVisitor {
	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.core.resources.IResourceDeltaVisitor#visit(org.eclipse.
	 * core.resources.IResourceDelta)
	 */
	public boolean visit(IResourceDelta delta) throws CoreException {
	    IResource resource = delta.getResource();
	    if (resource.getFileExtension() != null) {
		if (resource.getFileExtension().equalsIgnoreCase("ino")
			|| resource.getFileExtension().equalsIgnoreCase("pde")) {
		    try {
			PdePreprocessor.processProject(getProject());
		    } catch (CoreException e) {
			e.printStackTrace();
		    }
		    return false;
		}
	    }
	    return true;
	}
    }

    public static final String BUILDER_ID = "it.baeyens.arduino.core.inoToCpp";

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.core.internal.events.InternalBuilder#build(int,
     * java.util.Map, org.eclipse.core.runtime.IProgressMonitor)
     */
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

    protected void clean(IProgressMonitor monitor) throws CoreException {
	// delete markers set and files created

    }

    protected void fullBuild(final IProgressMonitor monitor) throws CoreException {
	try {
	    PdePreprocessor.processProject(getProject());
	} catch (CoreException e) {
	    e.printStackTrace();
	}
    }

    protected void incrementalBuild(IResourceDelta delta, IProgressMonitor monitor) throws CoreException {
	// the visitor does the work.
	delta.accept(new SampleDeltaVisitor());

    }
}
