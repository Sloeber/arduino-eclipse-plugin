package it.baeyens.arduino.toolchain;

import it.baeyens.arduino.common.ArduinoConst;
import it.baeyens.arduino.common.Common;
import it.baeyens.arduino.tools.ArduinoHelpers;

import org.eclipse.cdt.managedbuilder.core.IConfiguration;
import org.eclipse.cdt.managedbuilder.core.ITool;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

public class LinkNameProvider implements IManagedOutputNameProviderJaba {
    @Override
    public IPath[] getOutputNames(ITool tool, IPath[] primaryInputNames) {

	Common.log(new Status(IStatus.ERROR, ArduinoConst.CORE_PLUGIN_ID, "LinkNameProvider: The call should go to the overloaded function not here."));
	return null;
    }

    @Override
    public IPath[] getOutputNames(IProject project, IConfiguration cConf, ITool tool, IPath[] primaryInputNames) {
	boolean bUseArchiver = Common.getBuildEnvironmentVariable(project, cConf.getName(), ArduinoConst.ENV_KEY_use_archiver, "TRUE").toUpperCase()
		.equals("TRUE");
	IPath[] outputNames = new IPath[primaryInputNames.length];
	for (int curPath = 0; curPath < outputNames.length; curPath++) {
	    if (primaryInputNames[curPath].toString().startsWith("arduino") && (bUseArchiver)) {
		return null;
	    }
	    if (primaryInputNames[curPath].toString().endsWith(".ino")) {
		return null;
	    }
	    if (primaryInputNames[curPath].toString().endsWith(".pde")) {
		return null;
	    }
	    outputNames[curPath] = ArduinoHelpers.GetOutputName(primaryInputNames[curPath]).addFileExtension("o");
	}
	return outputNames;
    }

}
