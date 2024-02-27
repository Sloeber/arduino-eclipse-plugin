package io.sloeber.core.builder;

import java.util.Map;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

import io.sloeber.autoBuild.api.IAutoBuildConfigurationDescription;
import io.sloeber.core.tools.PdePreprocessor;

public class inoToCpp extends IncrementalProjectBuilder {

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.core.internal.events.InternalBuilder#build(int,
     * java.util.Map, org.eclipse.core.runtime.IProgressMonitor)
     */
    @Override
    protected IProject[] build(int kind, @SuppressWarnings("rawtypes") Map args, IProgressMonitor monitor)
            throws CoreException {
    	PdePreprocessor.processProject(false, getProject(),IAutoBuildConfigurationDescription.getActiveConfig(getProject(), false),monitor);
        return null;
    }

    @Override
    protected void clean(IProgressMonitor monitor) throws CoreException {
        PdePreprocessor.deleteSloeberInoCPPFile(IAutoBuildConfigurationDescription.getActiveConfig(getProject(), false),monitor);
    }

 
}
