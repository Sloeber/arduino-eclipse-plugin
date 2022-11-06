package io.sloeber.autoBuild.integration;

import java.util.Map;

import org.eclipse.cdt.core.resources.ACBuilder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

public class AutoBuildBuilder extends ACBuilder {

    public AutoBuildBuilder() {
        // TODO Auto-generated constructor stub
    }

    @Override
    protected IProject[] build(int kind, Map<String, String> args, IProgressMonitor monitor) throws CoreException {
        // TODO Auto-generated method stub
        return null;
    }

}
