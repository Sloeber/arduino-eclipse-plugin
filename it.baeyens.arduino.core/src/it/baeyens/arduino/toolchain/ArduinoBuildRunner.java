package it.baeyens.arduino.toolchain;

import org.eclipse.cdt.core.IMarkerGenerator;
import org.eclipse.cdt.core.resources.IConsole;
import org.eclipse.cdt.managedbuilder.core.AbstractBuildRunner;
import org.eclipse.cdt.managedbuilder.core.IBuilder;
import org.eclipse.cdt.managedbuilder.core.IConfiguration;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

public class ArduinoBuildRunner extends AbstractBuildRunner {

    public ArduinoBuildRunner() {
	// TODO Auto-generated constructor stub
    }

    @Override
    public boolean invokeBuild(int kind, IProject project, IConfiguration configuration, IBuilder builder, IConsole console,
	    IMarkerGenerator markerGenerator, IncrementalProjectBuilder projectBuilder, IProgressMonitor monitor) throws CoreException {
	// TODO Auto-generated method stub
	return false;
    }

}
