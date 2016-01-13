package it.baeyens.arduino.toolchain;

import org.eclipse.cdt.managedbuilder.core.IBuildObject;
import org.eclipse.cdt.managedbuilder.core.ITool;
import org.eclipse.cdt.managedbuilder.makegen.IManagedBuilderMakefileGenerator;
import org.eclipse.cdt.managedbuilder.makegen.gnu.DefaultGCCDependencyCalculator2Commands;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IPath;

import it.baeyens.arduino.tools.ArduinoHelpers;

public class ArduinoDependencyCalculatorCommands extends DefaultGCCDependencyCalculator2Commands {

    public ArduinoDependencyCalculatorCommands(IPath source, IBuildObject buildContext, ITool tool, IPath topBuildDirectory) {
	super(source, buildContext, tool, topBuildDirectory);
    }

    public ArduinoDependencyCalculatorCommands(IPath source, IResource resource, IBuildObject buildContext, ITool tool, IPath topBuildDirectory) {
	super(source, resource, buildContext, tool, topBuildDirectory);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.cdt.managedbuilder.makegen.IManagedDependencyCommands# getDependencyFiles()
     */
    @Override
    public IPath[] getDependencyFiles() {
	// The source file is project relative and the dependency file is top
	// build directory relative
	// Remove the source extension and add the dependency extension
	IPath depFilePath = ArduinoHelpers.GetOutputName(getSource()).addFileExtension(IManagedBuilderMakefileGenerator.DEP_EXT);
	// Remember that the source folder hierarchy and the build output folder
	// hierarchy are the same
	// but if this is a generated resource, then it may already be under the
	// top build directory
	if (!depFilePath.isAbsolute()) {
	    if (getTopBuildDirectory().isPrefixOf(depFilePath)) {
		depFilePath = depFilePath.removeFirstSegments(1);
	    }
	}
	IPath[] paths = new IPath[1];
	paths[0] = depFilePath;
	return paths;

    }

}
