package io.sloeber.autoBuild.api;

import org.eclipse.cdt.core.IMarkerGenerator;
import org.eclipse.cdt.core.resources.IConsole;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

import io.sloeber.schema.api.IBuilder;

@SuppressWarnings("static-method")
public class AutoBuildBuilderExtension {
	
	public boolean invokeBuild(IBuilder builder, int kind, String[] envp, IAutoBuildConfigurationDescription autoData,
			IMarkerGenerator markerGenerator, IConsole console, IProgressMonitor monitor) throws CoreException {
		return builder.getBuildRunner().invokeBuild(kind,envp, autoData, markerGenerator,  console, monitor);
	}
	
    public  boolean invokeClean(IBuilder builder,int kind,String envp[], IAutoBuildConfigurationDescription autoData,
            IMarkerGenerator markerGenerator,  IConsole console,
            IProgressMonitor monitor) throws CoreException{
    	return builder.getBuildRunner().invokeClean(kind, envp,autoData, markerGenerator,  console,
                monitor);
    }

}
