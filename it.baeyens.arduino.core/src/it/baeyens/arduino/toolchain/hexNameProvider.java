package it.baeyens.arduino.toolchain;

import it.baeyens.arduino.common.ArduinoConst;
import it.baeyens.arduino.common.Common;

import org.eclipse.cdt.managedbuilder.core.IConfiguration;
import org.eclipse.cdt.managedbuilder.core.ITool;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;

public class hexNameProvider implements IManagedOutputNameProviderJaba {

    @Override
    public IPath[] getOutputNames(ITool tool, IPath[] primaryInputNames) {
	Common.log(new Status(IStatus.ERROR, ArduinoConst.CORE_PLUGIN_ID, "hexNameProvider: The call should go to the overloaded function not here."));
	return null;
    }

    @Override
    public IPath[] getOutputNames(IProject project, IConfiguration cConf, ITool tool, IPath[] primaryInputNames) {
	String fileExtension = "hex";
	String command = Common.getBuildEnvironmentVariable(project, cConf.getName(), ArduinoConst.ENV_KEY_recipe_objcopy_hex_pattern, ".hex");
	if (command.indexOf(".hex") != -1)
	    fileExtension = "hex";
	else if (command.indexOf(".bin") != -1)
	    fileExtension = "bin";

	IPath[] outputNames = new IPath[1];
	outputNames[0] = new Path(project.getName()).addFileExtension(fileExtension);
	return outputNames;
    }
}
