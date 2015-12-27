package it.baeyens.arduino.toolchain;

import org.eclipse.cdt.managedbuilder.core.IConfiguration;
import org.eclipse.cdt.managedbuilder.core.ITool;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import it.baeyens.arduino.common.ArduinoConst;
import it.baeyens.arduino.common.Common;
import it.baeyens.arduino.tools.ArduinoHelpers;

public class ArchiveNameProvider implements IManagedOutputNameProviderJaba {

    @Override
    public IPath[] getOutputNames(ITool tool, IPath[] primaryInputNames) {
	Common.log(new Status(IStatus.ERROR, ArduinoConst.CORE_PLUGIN_ID,
		"ArchiveNameProvider: The call should go to the overloaded function not here.")); //$NON-NLS-1$
	return null;
    }

    @Override
    public IPath[] getOutputNames(IProject project, IConfiguration cConf, ITool tool, IPath[] primaryInputNames) {
	IPath[] outputNames = new IPath[primaryInputNames.length];
	boolean bUseArchiver = Common.getBuildEnvironmentVariable(project, cConf.getName(), ArduinoConst.ENV_KEY_use_archiver, ArduinoConst.TRUE)
		.equalsIgnoreCase(ArduinoConst.TRUE);
	for (int curPath = 0; curPath < primaryInputNames.length; curPath++) {
	    if (primaryInputNames[curPath].toString().startsWith(ArduinoConst.ARDUINO_CODE_FOLDER_NAME) && (bUseArchiver)) {
		outputNames[curPath] = ArduinoHelpers.GetOutputName(primaryInputNames[curPath]).addFileExtension("o"); //$NON-NLS-1$
	    } else {
		return null;
	    }
	}
	return outputNames;
    }

}
