package io.sloeber.autoBuild.api;

import org.eclipse.cdt.core.IMarkerGenerator;
import org.eclipse.cdt.core.resources.IConsole;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

import io.sloeber.autoBuild.extensionPoint.providers.AutoBuildMakeRules;
import io.sloeber.autoBuild.integration.AutoBuildConfigurationDescription;
import io.sloeber.schema.api.IBuilder;

@SuppressWarnings({"static-method","unused" })
public class AutoBuildBuilderExtension {

	public boolean invokeBuild(IBuilder builder, int kind, IAutoBuildConfigurationDescription autoData,
			IMarkerGenerator markerGenerator, IConsole console, IProgressMonitor monitor) throws CoreException {
		return builder.getBuildRunner().invokeBuild(kind, autoData, markerGenerator,  console, monitor);
	}

    public  boolean invokeClean(IBuilder builder,int kind, IAutoBuildConfigurationDescription autoData,
            IMarkerGenerator markerGenerator,  IConsole console,
            IProgressMonitor monitor) throws CoreException{
    	return builder.getBuildRunner().invokeClean(kind, autoData, markerGenerator,  console,
                monitor);
    }

	public  void beforeAddingSourceRules(IAutoBuildMakeRules makeRules, IAutoBuildConfigurationDescription autoBuildConfData) {
		//Nothing to do here
		return ;
	}

	public  void beforeAddingSecondaryRules(AutoBuildMakeRules autoBuildMakeRules,
			IAutoBuildConfigurationDescription autoBuildConfData) {
		//Nothing to do here
		return ;
	}

	public  void endOfRuleCreation(AutoBuildMakeRules autoBuildMakeRules,
			IAutoBuildConfigurationDescription autoBuildConfData) {
		//Nothing to do here
		return ;
	}

}
