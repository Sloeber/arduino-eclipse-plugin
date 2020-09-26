package io.sloeber.core.toolchain;

import org.eclipse.cdt.managedbuilder.core.IBuildObject;
import org.eclipse.cdt.managedbuilder.core.IConfiguration;
import org.eclipse.cdt.managedbuilder.core.IResourceInfo;
import org.eclipse.cdt.managedbuilder.core.ITool;
import org.eclipse.cdt.managedbuilder.makegen.IManagedBuilderMakefileGenerator;
import org.eclipse.cdt.managedbuilder.makegen.gnu.DefaultGCCDependencyCalculator2Commands;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IPath;

import io.sloeber.core.common.Common;
import io.sloeber.core.common.Const;
import io.sloeber.core.tools.Helpers;

public class ArduinoDependencyCalculatorCommands extends DefaultGCCDependencyCalculator2Commands {

	private IProject myproject;

	@Override
	public String[] getDependencyCommandOptions() {
		if (this.getTool().getInputTypeById("io.sloeber.compiler.S.sketch.input") != null) { //$NON-NLS-1$
			// (this.)
			String assemblyCommand = Common.getBuildEnvironmentVariable(this.myproject,
					Const.RECIPE_S_to_O, ""); //$NON-NLS-1$
			if (!assemblyCommand.contains("assembler-with-cpp")) { //$NON-NLS-1$
				String options[] = new String[0];
				return options;
			}
		}

		// String options[] = super.getDependencyCommandOptions();
		// String[] newOptions = new String[options.length + 1];
		// System.arraycopy(options, 0, newOptions, 0, options.length);
		// newOptions[options.length] = "-D__IN_ECLIPSE__=1"; //$NON-NLS-1$
		return super.getDependencyCommandOptions();
	}

	public ArduinoDependencyCalculatorCommands(IPath source, IBuildObject buildContext, ITool tool,
			IPath topBuildDirectory) {
		super(source, buildContext, tool, topBuildDirectory);
		// Compute the project
		if (buildContext instanceof IConfiguration) {
			IConfiguration config = (IConfiguration) buildContext;
			this.myproject = (IProject) config.getOwner();
		} else if (buildContext instanceof IResourceInfo) {
			IResourceInfo rcInfo = (IResourceInfo) buildContext;
			this.myproject = rcInfo.getParent().getOwner().getProject();
		}
	}

	public ArduinoDependencyCalculatorCommands(IPath source, IResource resource, IBuildObject buildContext, ITool tool,
			IPath topBuildDirectory) {
		super(source, resource, buildContext, tool, topBuildDirectory);
		// Compute the project
		if (buildContext instanceof IConfiguration) {
			IConfiguration config = (IConfiguration) buildContext;
			this.myproject = (IProject) config.getOwner();
		} else if (buildContext instanceof IResourceInfo) {
			IResourceInfo rcInfo = (IResourceInfo) buildContext;
			this.myproject = rcInfo.getParent().getOwner().getProject();
		}
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.eclipse.cdt.managedbuilder.makegen.IManagedDependencyCommands#
	 * getDependencyFiles()
	 */
	@Override
	public IPath[] getDependencyFiles() {
		// The source file is project relative and the dependency file is top
		// build directory relative
		// Remove the source extension and add the dependency extension
		IPath depFilePath = Helpers.GetOutputName(getSource())
				.addFileExtension(IManagedBuilderMakefileGenerator.DEP_EXT);
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
