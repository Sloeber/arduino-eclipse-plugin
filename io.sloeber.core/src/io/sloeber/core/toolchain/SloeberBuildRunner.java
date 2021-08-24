package io.sloeber.core.toolchain;

import java.util.List;

import org.eclipse.cdt.core.IMarkerGenerator;
import org.eclipse.cdt.core.resources.IConsole;
import org.eclipse.cdt.managedbuilder.core.ExternalBuildRunner;
import org.eclipse.cdt.managedbuilder.core.IBuilder;
import org.eclipse.cdt.managedbuilder.core.IConfiguration;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import io.sloeber.core.Messages;
import io.sloeber.core.api.BoardDescription;
import io.sloeber.core.api.Preferences;
import io.sloeber.core.api.SerialManager;
import io.sloeber.core.api.SloeberProject;
import io.sloeber.core.common.Common;
import io.sloeber.core.common.Const;

public class SloeberBuildRunner extends ExternalBuildRunner {

    @Override
    public boolean invokeBuild(int kind, IProject project, IConfiguration configuration, IBuilder builder,
            IConsole console, IMarkerGenerator markerGenerator, IncrementalProjectBuilder projectBuilder,
            IProgressMonitor monitor) throws CoreException {

        String theBuildTarget = builder.getFullBuildTarget();

        String actualUploadPort = Const.EMPTY;

        List<String> stopSerialOnBuildTargets = List.of(Preferences.getDisconnectSerialTargetsList());
        if (stopSerialOnBuildTargets.contains(theBuildTarget)) {
            SloeberProject sloeberProject = SloeberProject.getSloeberProject(project, true);
            if (sloeberProject != null) {
                BoardDescription myBoardDescriptor = sloeberProject.getBoardDescription(configuration.getName(), true);
                if (myBoardDescriptor != null) {
                    actualUploadPort = myBoardDescriptor.getActualUploadPort();
                    if (actualUploadPort == null) {
                        actualUploadPort = Const.EMPTY;
                    }
                }
            }
        }

        boolean WeStoppedTheComPort = false;
        if (!actualUploadPort.isBlank()) {
            try {
                WeStoppedTheComPort = SerialManager.StopSerialMonitor(actualUploadPort);
            } catch (Exception e) {
                Status ret = new Status(IStatus.WARNING, Const.CORE_PLUGIN_ID, Messages.Upload_Error_com_port, e);
                Common.log(ret);
            }
        }

        boolean ret = super.invokeBuild(kind, project, configuration, builder, console, markerGenerator, projectBuilder,
                monitor);

        if (WeStoppedTheComPort) {
            try {
                SerialManager.StartSerialMonitor(actualUploadPort);
            } catch (Exception e) {
                Status ret2 = new Status(IStatus.WARNING, Const.CORE_PLUGIN_ID,
                        Messages.Upload_Error_serial_monitor_restart, e);
                Common.log(ret2);
            }
        }

        return ret;
    }

}
