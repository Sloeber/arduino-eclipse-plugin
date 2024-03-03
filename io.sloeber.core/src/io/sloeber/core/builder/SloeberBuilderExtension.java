package io.sloeber.core.builder;

import static io.sloeber.core.api.Const.*;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;

import org.apache.commons.io.FileUtils;
import org.eclipse.cdt.core.IMarkerGenerator;
import org.eclipse.cdt.core.resources.IConsole;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

import io.sloeber.autoBuild.api.AutoBuildBuilderExtension;
import io.sloeber.autoBuild.api.IAutoBuildConfigurationDescription;
import io.sloeber.autoBuild.api.IAutoBuildMakeRules;
import io.sloeber.core.Messages;
import io.sloeber.core.api.Common;
import io.sloeber.core.api.ISloeberConfiguration;
import io.sloeber.schema.api.IBuilder;

public class SloeberBuilderExtension extends AutoBuildBuilderExtension {

	@Override
	public void beforeAddingSourceRules(IAutoBuildMakeRules makeRules,
			IAutoBuildConfigurationDescription autoBuildConfData) {
		IFile sloeberInoCppFile=InoPreprocessor.getSloeberInoCPPFile(autoBuildConfData);
		if(sloeberInoCppFile != null &&  sloeberInoCppFile.exists()) {
			makeRules.getSourceFilesToBuild().add(sloeberInoCppFile);
		}
		generateAwkFile( autoBuildConfData);
		super.beforeAddingSourceRules(makeRules, autoBuildConfData);
	}

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
	
	
	@SuppressWarnings("nls")
	private static void generateAwkFile(IAutoBuildConfigurationDescription autoBuildConfData) {
		IFile sizeAwkFile1 = autoBuildConfData.getBuildFolder().getFile("size.awk");
		ISloeberConfiguration confDesc = (ISloeberConfiguration) autoBuildConfData
				.getAutoBuildConfigurationExtensionDescription();

		File sizeAwkFile = sizeAwkFile1.getLocation().toFile();
		String regex = Common.getBuildEnvironmentVariable(confDesc, "recipe.size.regex", EMPTY);
		String awkContent = "/" + regex + "/ {arduino_size += $2 }\n";
		regex = Common.getBuildEnvironmentVariable(confDesc, "recipe.size.regex.data", EMPTY);
		awkContent += "/" + regex + "/ {arduino_data += $2 }\n";
		regex = Common.getBuildEnvironmentVariable(confDesc, "recipe.size.regex.eeprom", EMPTY);
		awkContent += "/" + regex + "/ {arduino_eeprom += $2 }\n";
		awkContent += "END { print \"\\n";
		String max = Common.getBuildEnvironmentVariable(confDesc, "upload.maximum_size", "10000");
		awkContent += Messages.sizeReportSketch.replace("maximum_size", max);
		awkContent += "\\n";
		max = Common.getBuildEnvironmentVariable(confDesc, "upload.maximum_data_size", "10000");
		awkContent += Messages.sizeReportData.replace("maximum_data_size", max);
		awkContent += "\\n";
		awkContent += "\"}";

		try {
			FileUtils.write(sizeAwkFile, awkContent, Charset.defaultCharset());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}


}
