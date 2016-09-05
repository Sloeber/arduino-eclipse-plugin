package io.sloeber.core.toolchain;

import org.eclipse.cdt.managedbuilder.core.IBuildObject;
import org.eclipse.cdt.managedbuilder.core.IConfiguration;
import org.eclipse.cdt.managedbuilder.core.ITool;
import org.eclipse.cdt.managedbuilder.makegen.IManagedBuilderMakefileGenerator;
import org.eclipse.cdt.managedbuilder.makegen.IManagedDependencyGenerator2;
import org.eclipse.cdt.managedbuilder.makegen.IManagedDependencyInfo;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IPath;

public class ArduinoDependencyCalculator implements IManagedDependencyGenerator2 {

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.cdt.managedbuilder.makegen.IManagedDependencyGeneratorType #getCalculatorType()
     */
    @Override
    public int getCalculatorType() {
	return TYPE_BUILD_COMMANDS;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.cdt.managedbuilder.makegen.IManagedDependencyGenerator2# getDependencyFileExtension
     * (org.eclipse.cdt.managedbuilder.core.IConfiguration, org.eclipse.cdt.managedbuilder.core.ITool)
     */
    @Override
    public String getDependencyFileExtension(IConfiguration buildContext, ITool tool) {
	return IManagedBuilderMakefileGenerator.DEP_EXT;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.cdt.managedbuilder.makegen.IManagedDependencyGenerator2# getDependencySourceInfo(org.eclipse.core.runtime.IPath,
     * org.eclipse.cdt.managedbuilder.core.IBuildObject, org.eclipse.cdt.managedbuilder.core.ITool, org.eclipse.core.runtime.IPath)
     */
    @Override
    public IManagedDependencyInfo getDependencySourceInfo(IPath source, IResource resource, IBuildObject buildContext, ITool tool,
	    IPath topBuildDirectory) {
	return new ArduinoDependencyCalculatorCommands(source, resource, buildContext, tool, topBuildDirectory);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.cdt.managedbuilder.makegen.IManagedDependencyGenerator2# getDependencySourceInfo(org.eclipse.core.runtime.IPath,
     * org.eclipse.cdt.managedbuilder.core.IBuildObject, org.eclipse.cdt.managedbuilder.core.ITool, org.eclipse.core.runtime.IPath)
     */
    @Override
    public IManagedDependencyInfo getDependencySourceInfo(IPath source, IBuildObject buildContext, ITool tool, IPath topBuildDirectory) {
	return new ArduinoDependencyCalculatorCommands(source, buildContext, tool, topBuildDirectory);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.cdt.managedbuilder.makegen.IManagedDependencyGenerator2# postProcessDependencyFile(org.eclipse.core.runtime.IPath,
     * org.eclipse.cdt.managedbuilder.core.IConfiguration, org.eclipse.cdt.managedbuilder.core.ITool, org.eclipse.core.runtime.IPath)
     */
    @Override
    public boolean postProcessDependencyFile(IPath dependencyFile, IConfiguration buildContext, ITool tool, IPath topBuildDirectory) {
	// Nothing
	return false;
    }
}
