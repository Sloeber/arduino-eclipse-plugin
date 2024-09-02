package io.sloeber.core.builder;

import static io.sloeber.core.api.Const.*;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import org.apache.commons.io.FileUtils;
import org.eclipse.cdt.core.IMarkerGenerator;
import org.eclipse.cdt.core.resources.IConsole;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

import io.sloeber.arduinoFramework.api.BoardDescription;
import io.sloeber.autoBuild.api.AutoBuildBuilderExtension;
import io.sloeber.autoBuild.api.AutoBuildProject;
import io.sloeber.autoBuild.api.IAutoBuildConfigurationDescription;
import io.sloeber.autoBuild.api.IAutoBuildMakeRule;
import io.sloeber.autoBuild.api.IAutoBuildMakeRules;
import io.sloeber.autoBuild.helpers.api.AutoBuildConstants;
import io.sloeber.autoBuild.integration.AutoBuildConfigurationDescription;
import io.sloeber.autoBuild.schema.api.IBuilder;
import io.sloeber.core.Messages;
import io.sloeber.core.api.Common;
import io.sloeber.core.internal.SloeberConfiguration;
import io.sloeber.core.tools.Helpers;

public class SloeberBuilderExtension extends AutoBuildBuilderExtension {

	@Override
	public String[] modifyRecipes(AutoBuildConfigurationDescription autoBuildConfData,IAutoBuildMakeRule autoBuildMakeRule, String[] buildRecipes) {
		switch (autoBuildMakeRule.getTool().getId()) {

			default:
				return super.modifyRecipes(autoBuildConfData,autoBuildMakeRule,buildRecipes);
			case "io.sloeber.tool.combine": //$NON-NLS-1$
				SloeberConfiguration confDesc = SloeberConfiguration.getFromAutoBuildConfDesc(autoBuildConfData);
				BoardDescription boardDescription=confDesc.getBoardDescription();
				LinkedHashSet<String> recipes=new LinkedHashSet<>();
				LinkedHashMap<String, String> pre=new LinkedHashMap<>();
				LinkedHashSet <String> preHooks=new LinkedHashSet<>();
				preHooks.add("linking"); //$NON-NLS-1$
				preHooks.add("prelink"); //$NON-NLS-1$
				LinkedHashSet <String> postHooks=new LinkedHashSet<>();
				postHooks.add("linking"); //$NON-NLS-1$
				postHooks.add("postlink"); //$NON-NLS-1$
				pre.putAll(boardDescription.getHookSteps(preHooks,autoBuildConfData));
				LinkedHashMap<String, String> post=new LinkedHashMap<>();
				post.putAll(boardDescription.getHookSteps(postHooks,autoBuildConfData));
				recipes.addAll(pre.values());
				recipes.addAll(Arrays.asList(buildRecipes));
				recipes.addAll(post.values());
				return super.modifyRecipes(autoBuildConfData,autoBuildMakeRule, recipes.toArray(new String[recipes.size()]));
		}

	}


	@Override
	public void beforeAddingSourceRules(IAutoBuildMakeRules makeRules,
			IAutoBuildConfigurationDescription autoBuildConfData) {
		IFile sloeberInoCppFile = InoPreprocessor.getSloeberInoCPPFile(autoBuildConfData);
		if (sloeberInoCppFile != null && sloeberInoCppFile.exists()) {
			makeRules.getSourceFilesToBuild().add(sloeberInoCppFile);
		}
		generateAwkFile(autoBuildConfData);
		generateArduinoSizeCommandFile(autoBuildConfData);

		super.beforeAddingSourceRules(makeRules, autoBuildConfData);
	}




	@Override
	public boolean invokeBuild(IBuilder builder, int kind, String targetName, IAutoBuildConfigurationDescription autoData,
			IMarkerGenerator markerGenerator, IConsole console, IProgressMonitor monitor) throws CoreException {
		InoPreprocessor.generateSloeberInoCPPFile(false, autoData, monitor);
		if (builder.getId().equals(AutoBuildProject.MAKE_BUILDER_ID)) {
			generateExtensionMakeFile(autoData);
		}
		return super.invokeBuild(builder, kind,targetName, autoData, markerGenerator, console, monitor);
	}

	private static void generateExtensionMakeFile(IAutoBuildConfigurationDescription autoData) {
		IFile file = autoData.getBuildFolder().getFile(AutoBuildConstants.MAKE_FILE_EXTENSION);
		if (file.exists()) {
			return;
		}
		String content = "\n#bootloaderTest\n" + "BurnBootLoader: \n" //$NON-NLS-1$ //$NON-NLS-2$
				+ "\t@echo trying to burn bootloader ${bootloader.tool}\n" //$NON-NLS-1$
				+ "\t${tools.${bootloader.tool}.erase.pattern}\n" + "\t${tools.${bootloader.tool}.bootloader.pattern}\n" //$NON-NLS-1$ //$NON-NLS-2$
				+ "\n" + "uploadWithBuild: all\n" //$NON-NLS-1$ //$NON-NLS-2$
				+ "\t@echo trying to build and upload with upload tool ${upload.tool}\n" //$NON-NLS-1$
				+ "\t${tools.${upload.tool}.upload.pattern}\n" + "\n" + "uploadWithoutBuild: \n" //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				+ "\t@echo trying to upload without build with upload tool ${upload.tool}\n" //$NON-NLS-1$
				+ "\t${tools.${upload.tool}.upload.pattern}\n" + "    \n" + "uploadWithProgrammerWithBuild: all\n" //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				+ "\t@echo trying to build and upload with programmer ${program.tool}\n" //$NON-NLS-1$
				+ "\t${tools.${program.tool}.program.pattern}\n" + "\n" + "uploadWithProgrammerWithoutBuild: \n" //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				+ "\t@echo trying to upload with programmer ${program.tool} without build\n" //$NON-NLS-1$
				+ "\t${tools.${program.tool}.program.pattern}\n\n"; //$NON-NLS-1$

		try {
			Helpers.addFileToProject(file, new ByteArrayInputStream(content.getBytes()), null, true);
		} catch (CoreException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void invokeClean(IBuilder builder, int kind, IAutoBuildConfigurationDescription autoData,
			IMarkerGenerator markerGenerator, IConsole console, IProgressMonitor monitor) throws CoreException {
		InoPreprocessor.deleteSloeberInoCPPFile(autoData, monitor);
		IFile file = autoData.getBuildFolder().getFile(AutoBuildConstants.MAKE_FILE_EXTENSION);
		if (file.exists()) {
			file.delete(true, monitor);
		}
		super.invokeClean(builder, kind, autoData, markerGenerator, console, monitor);
	}

	public SloeberBuilderExtension() {
		// Nothing to do here
	}


	@SuppressWarnings("nls")
	private static void generateArduinoSizeCommandFile(IAutoBuildConfigurationDescription autoBuildConfData) {
		if(!isWindows) {
			return;
		}
		IFile sizeCommandIFile = autoBuildConfData.getBuildFolder().getFile("arduino-size.bat");
		SloeberConfiguration confDesc = SloeberConfiguration.getFromAutoBuildConfDesc(autoBuildConfData);

		File sizeCommandFile = sizeCommandIFile.getLocation().toFile();
		String content = Common.getBuildEnvironmentVariable(confDesc, "sloeber.size_command.awk", EMPTY);

		try {
			if (sizeCommandFile.exists()) {
				String curContent = FileUtils.readFileToString(sizeCommandFile, Charset.defaultCharset());
				if (!curContent.equals(content)) {
					sizeCommandFile.delete();
				}
			}
			if (!sizeCommandFile.exists()) {
				FileUtils.write(sizeCommandFile, content, Charset.defaultCharset());
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@SuppressWarnings("nls")
	private static void generateAwkFile(IAutoBuildConfigurationDescription autoBuildConfData) {
		IFile sizeAwkFile1 = autoBuildConfData.getBuildFolder().getFile("size.awk");
		SloeberConfiguration confDesc = SloeberConfiguration.getFromAutoBuildConfDesc(autoBuildConfData);

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
			if (sizeAwkFile.exists()) {
				String curContent = FileUtils.readFileToString(sizeAwkFile, Charset.defaultCharset());
				if (!curContent.equals(awkContent)) {
					sizeAwkFile.delete();
				}
			}
			if (!sizeAwkFile.exists()) {
				FileUtils.write(sizeAwkFile, awkContent, Charset.defaultCharset());
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}


}
