package io.sloeber.core.builder;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IPath;

import io.sloeber.autoBuild.api.IAutoBuildConfigurationDescription;
import io.sloeber.autoBuild.extensionPoint.IOutputNameProvider;
import io.sloeber.autoBuild.schema.api.IInputType;
import io.sloeber.autoBuild.schema.api.IOutputType;

import static io.sloeber.core.api.Const.*;
import io.sloeber.core.api.ISloeberConfiguration;

public class ArchiveOutputNameProvider implements IOutputNameProvider {


	@Override
	public String getOutputFileName(IFile inputFile, IAutoBuildConfigurationDescription autoData, IInputType inputType,
			IOutputType outputType) {

		ISloeberConfiguration sloeberConf = ISloeberConfiguration.getConfig(autoData);
		if (sloeberConf == null) {
			// This should not happen
			return null;
		}
		IPath buildPath=autoData.getBuildFolder().getProjectRelativePath();
		IPath inputFilePath = inputFile.getProjectRelativePath();
		String inputFilePathSegments[]=inputFilePath.segments();
		for(int curSegment=buildPath.segmentCount() ;curSegment<inputFilePathSegments.length;curSegment++) {
			if(SLOEBER_LIBRARY_FOLDER_NAME.equals(  inputFilePathSegments[curSegment])){
				return inputFilePathSegments[curSegment+1]+DOT+outputType.getOutputExtension();
			}
		}
		return outputType.getOutputName();
	}

}
