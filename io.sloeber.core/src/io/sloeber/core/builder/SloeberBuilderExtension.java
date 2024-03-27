package io.sloeber.core.builder;

import static io.sloeber.autoBuild.api.AutoBuildConstants.COMMENT_SYMBOL;
import static io.sloeber.autoBuild.api.AutoBuildConstants.NEWLINE;
import static io.sloeber.autoBuild.api.AutoBuildConstants.WHITESPACE;
import static io.sloeber.autoBuild.core.Messages.MakefileGenerator_comment_header;
import static io.sloeber.core.api.Const.*;

import java.io.ByteArrayInputStream;
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
import io.sloeber.autoBuild.api.AutoBuildConstants;
import io.sloeber.autoBuild.api.AutoBuildProject;
import io.sloeber.autoBuild.api.IAutoBuildConfigurationDescription;
import io.sloeber.autoBuild.api.IAutoBuildMakeRules;
import io.sloeber.core.Messages;
import io.sloeber.core.api.Common;
import io.sloeber.core.api.ISloeberConfiguration;
import io.sloeber.core.tools.Helpers;
import io.sloeber.schema.api.IBuilder;

public class SloeberBuilderExtension extends AutoBuildBuilderExtension {

	@Override
	public void beforeAddingSourceRules(IAutoBuildMakeRules makeRules,
			IAutoBuildConfigurationDescription autoBuildConfData) {
		IFile sloeberInoCppFile = InoPreprocessor.getSloeberInoCPPFile(autoBuildConfData);
		if (sloeberInoCppFile != null && sloeberInoCppFile.exists()) {
			makeRules.getSourceFilesToBuild().add(sloeberInoCppFile);
		}
		generateAwkFile(autoBuildConfData);
		super.beforeAddingSourceRules(makeRules, autoBuildConfData);
	}

	@Override
	public boolean invokeBuild(IBuilder builder, int kind, IAutoBuildConfigurationDescription autoData,
			IMarkerGenerator markerGenerator, IConsole console, IProgressMonitor monitor) throws CoreException {
		InoPreprocessor.generateSloeberInoCPPFile(false, autoData, monitor);
		if (builder.getId().equals(AutoBuildProject.MAKE_BUILDER_ID)) {
			generateMakeFiles(autoData);
		}
		return super.invokeBuild(builder, kind, autoData, markerGenerator, console, monitor);
	}

	private static void generateMakeFiles(IAutoBuildConfigurationDescription autoData) {
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
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Override
	public boolean invokeClean(IBuilder builder, int kind, IAutoBuildConfigurationDescription autoData,
			IMarkerGenerator markerGenerator, IConsole console, IProgressMonitor monitor) throws CoreException {
		InoPreprocessor.deleteSloeberInoCPPFile(autoData, monitor);
		IFile file = autoData.getBuildFolder().getFile(AutoBuildConstants.MAKE_FILE_EXTENSION);
		if (file.exists()) {
			file.delete(true, monitor);
		}
		return super.invokeClean(builder, kind, autoData, markerGenerator, console, monitor);
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
