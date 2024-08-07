package io.sloeber.core.builder;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IPath;

import io.sloeber.autoBuild.api.IAutoBuildConfigurationDescription;
import io.sloeber.autoBuild.extensionPoint.IOutputNameProvider;
import io.sloeber.autoBuild.helpers.api.KeyValueTree;
import io.sloeber.autoBuild.schema.api.IInputType;
import io.sloeber.autoBuild.schema.api.IOutputType;

import static io.sloeber.core.api.Const.*;
import io.sloeber.core.api.ISloeberConfiguration;
import io.sloeber.core.txt.TxtFile;

@SuppressWarnings("nls")
public class CompileOutputNameProvider implements IOutputNameProvider {

	private static Set<String> archiveOutputTypeIDs = Set.of("io.sloeber.compiler.cpp.ar.output",
			"io.sloeber.compiler.c.ar.output", "io.sloeber.compiler.S.ar.output");
	private static Set<String> ignoreFileExtensions = Set.of("ino", "pde", "cxx");
	private static Map<String, Boolean> isArchiveLib = new HashMap<>();

	@Override
	public String getOutputFileName(IFile inputFile, IAutoBuildConfigurationDescription autoData, IInputType inputType,
			IOutputType outputType) {

		// ignore files with an extension we always need to ignore
		String fileExt = inputFile.getFileExtension();
		if (ignoreFileExtensions.contains(fileExt)) {
			return null;
		}

		ISloeberConfiguration sloeberConf = ISloeberConfiguration.getConfig(autoData);
		if (sloeberConf == null) {
			// This should not happen
			return null;
		}
		boolean isArchiverOutputType = archiveOutputTypeIDs.contains(outputType.getId());
		boolean isArchiverFile = isArchiverFile(inputFile, sloeberConf);
		return isArchiverFile == isArchiverOutputType ? outputType.getOutputNameWithoutNameProvider(inputFile) : null;

	}

	private static boolean isArchiverFile(IFile inputFile, ISloeberConfiguration sloeberConf) {

		IPath inputFilePath = inputFile.getProjectRelativePath();
		IPath corePath = sloeberConf.getArduinoCoreFolder().getProjectRelativePath();

		if (corePath.isPrefixOf(inputFilePath)) {
			// This is core code so needs archiver
			return true;
		}
		IPath libraryFolder = sloeberConf.getArduinoLibraryFolder().getProjectRelativePath();
		if (libraryFolder.isPrefixOf(inputFilePath)) {
			// It is a library input file check the library properties file
			String libName = inputFilePath.segment(libraryFolder.segmentCount());
			Boolean storedValue = isArchiveLib.get(libName);
			if (storedValue != null) {
				return storedValue.booleanValue();
			}
			IFile libPropertiesFile = sloeberConf.getArduinoLibraryFolder().getFolder(libName)
					.getFile(LIBRARY_PROPERTIES);
			boolean ret = false;
			if (libPropertiesFile.exists()) {
				TxtFile libProps = new TxtFile(libPropertiesFile.getLocation().toFile());
				KeyValueTree rootSection = libProps.getData();
				ret = Boolean.valueOf(rootSection.getValue(LIBRARY_DOT_A_LINKAGE)).booleanValue();
			}
			isArchiveLib.put(libName, Boolean.valueOf(ret));
			return ret;

		}
		// It is standard sketch code. No archiver needed

		return false;
	}

}
