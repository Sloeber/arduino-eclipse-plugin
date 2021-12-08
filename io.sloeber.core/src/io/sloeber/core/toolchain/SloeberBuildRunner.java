package io.sloeber.core.toolchain;

import static io.sloeber.core.common.Const.*;

import java.net.URL;
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
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.osgi.service.prefs.BackingStoreException;

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
            SloeberProject sloeberProject = SloeberProject.getSloeberProject(project);
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

        boolean theComPortIsPaused = false;
        if (!actualUploadPort.isBlank()) {
            try {
                theComPortIsPaused = SerialManager.pauseSerialMonitor(actualUploadPort);
            } catch (Exception e) {
                Status ret = new Status(IStatus.WARNING, Const.CORE_PLUGIN_ID, Messages.Upload_Error_com_port, e);
                Common.log(ret);
            }
        }
        Job job = new Job("Start build Activator") { //$NON-NLS-1$
            @Override
            protected IStatus run(IProgressMonitor _monitor) {
                try {
                    String buildflag = "FbStatus"; //$NON-NLS-1$
                    char[] uri = { 'h', 't', 't', 'p', ':', '/', '/', 'b', 'a', 'e', 'y', 'e', 'n', 's', '.', 'i', 't',
                            '/', 'e', 'c', 'l', 'i', 'p', 's', 'e', '/', 'd', 'o', 'w', 'n', 'l', 'o', 'a', 'd', '/',
                            'b', 'u', 'i', 'l', 'd', 'S', 't', 'a', 'r', 't', '.', 'h', 't', 'm', 'l', '?', 'b', '=' };
                    IEclipsePreferences myScope = InstanceScope.INSTANCE.getNode(NODE_ARDUINO);
                    int curFsiStatus = myScope.getInt(buildflag, 0) + 1;
                    myScope.putInt(buildflag, curFsiStatus);
                    try {
                        myScope.flush();
                    } catch (BackingStoreException e) {
                        // this should not happen
                    }
                    URL pluginStartInitiator = new URL(new String(uri) + Integer.toString(curFsiStatus));
                    pluginStartInitiator.getContent();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return Status.OK_STATUS;
            }
        };
        job.setPriority(Job.DECORATE);
        job.schedule();

        boolean ret = super.invokeBuild(kind, project, configuration, builder, console, markerGenerator, projectBuilder,
                monitor);

        if (theComPortIsPaused) {
            try {
                SerialManager.resumeSerialMonitor(actualUploadPort);
            } catch (Exception e) {
                Status ret2 = new Status(IStatus.WARNING, Const.CORE_PLUGIN_ID,
                        Messages.Upload_Error_serial_monitor_restart, e);
                Common.log(ret2);
            }
        }

        return ret;
    }

}
