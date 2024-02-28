package io.sloeber.core.builder;

import org.eclipse.cdt.core.IMarkerGenerator;
import org.eclipse.cdt.core.resources.IConsole;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

import io.sloeber.autoBuild.api.AutoBuildBuilderExtension;
import io.sloeber.autoBuild.api.IAutoBuildConfigurationDescription;
import io.sloeber.schema.api.IBuilder;

public class SloeberBuilderExtension extends AutoBuildBuilderExtension {

	@Override
	public boolean invokeBuild(IBuilder builder, int kind, String[] envp, IAutoBuildConfigurationDescription autoData,
			IMarkerGenerator markerGenerator, IConsole console, IProgressMonitor monitor) throws CoreException {
		InoPreprocessor.generateSloeberInoCPPFile(false, autoData,monitor);
		return super.invokeBuild(builder, kind, envp, autoData, markerGenerator, console, monitor);
	}

	@Override
	public boolean invokeClean(IBuilder builder, int kind, String[] envp, IAutoBuildConfigurationDescription autoData,
			IMarkerGenerator markerGenerator, IConsole console, IProgressMonitor monitor) throws CoreException {
		 InoPreprocessor.deleteSloeberInoCPPFile(autoData,monitor);
		return super.invokeClean(builder, kind, envp, autoData, markerGenerator, console, monitor);
	}

	public SloeberBuilderExtension() {
		// Nothing to do here
	}

}
