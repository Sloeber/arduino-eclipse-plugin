package it.baeyens.arduino.toolchain;

import it.baeyens.arduino.common.ArduinoConst;
import it.baeyens.arduino.common.Common;
import it.baeyens.arduino.tools.ArduinoHelpers;

import org.eclipse.cdt.managedbuilder.core.IConfiguration;
import org.eclipse.cdt.managedbuilder.core.ITool;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IPath;

public class ArchiveNameProvider implements IManagedOutputNameProviderJaba {

    @Override
    public IPath[] getOutputNames(ITool tool, IPath[] primaryInputNames) {
    	Common.logError("ArchiveNameProvider: The call should go to the overloaded function not here.");
	return null;
    }

    @Override
    public IPath[] getOutputNames(IProject project, IConfiguration cConf, ITool tool, IPath[] primaryInputNames) {
	IPath[] outputNames = new IPath[primaryInputNames.length];
	boolean bUseArchiver = Common.getBuildEnvironmentVariableBoolean(project,
			cConf.getName(), ArduinoConst.ENV_KEY_use_archiver, true);
	for (int curPath = 0; curPath < primaryInputNames.length; curPath++) {
	    if (primaryInputNames[curPath].toString().startsWith("arduino") && (bUseArchiver)) {
		outputNames[curPath] = ArduinoHelpers.GetOutputName(primaryInputNames[curPath]).addFileExtension("o");
	    } else {
		return null;
	    }
	}
	return outputNames;
    }

}
