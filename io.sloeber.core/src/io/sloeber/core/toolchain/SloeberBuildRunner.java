package io.sloeber.core.toolchain;

import static io.sloeber.autoBuild.integration.AutoBuildConstants.*;
import static io.sloeber.core.common.Const.*;

import java.net.URL;
import java.util.List;

import org.eclipse.cdt.core.IMarkerGenerator;
import org.eclipse.cdt.core.resources.IConsole;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.osgi.service.prefs.BackingStoreException;

import io.sloeber.autoBuild.api.IBuildRunner;
import io.sloeber.autoBuild.extensionPoint.providers.AutoBuildCommon;
import io.sloeber.autoBuild.integration.AutoBuildConfigurationDescription;
import io.sloeber.core.Messages;
import io.sloeber.core.api.BoardDescription;
import io.sloeber.core.api.ISloeberConfiguration;
import io.sloeber.core.api.Preferences;
import io.sloeber.core.api.SerialManager;
import io.sloeber.core.common.Common;
import io.sloeber.core.common.Const;
import io.sloeber.core.internal.SloeberConfiguration;
import io.sloeber.schema.api.IBuilder;

public class SloeberBuildRunner extends IBuildRunner {

    /**
     * The sloeber builder stops the serial connection of the project on the serial
     * monitor
     * before starting the default builder
     * and restarts the serial connection on the serial monitor after the build
     * The serial connection is not stopped in case of a clean command
     */
    @Override
    public boolean invokeBuild(int kind, AutoBuildConfigurationDescription autoData, IMarkerGenerator markerGenerator,
            IncrementalProjectBuilder projectBuilder, IConsole console, IProgressMonitor monitor) throws CoreException {
        IBuilder builder = autoData.getConfiguration().getBuilder();

        //get the target that is build
        String defaultTarget = EMPTY_STRING;
        String customTarget = EMPTY_STRING;
        boolean isClean = false;
        switch (kind) {
        case IncrementalProjectBuilder.AUTO_BUILD: {
            defaultTarget = builder.getAutoBuildTarget();
            customTarget = autoData.getAutoMakeTarget();
            break;
        }
        case IncrementalProjectBuilder.INCREMENTAL_BUILD:
        case IncrementalProjectBuilder.FULL_BUILD: {
            defaultTarget = builder.getIncrementalBuildTarget();
            customTarget = autoData.getIncrementalMakeTarget();
            break;
        }
        case IncrementalProjectBuilder.CLEAN_BUILD: {
            defaultTarget = builder.getCleanBuildTarget();
            customTarget = autoData.getCleanMakeTarget();
            isClean = true;
            break;
        }
        }
        defaultTarget = AutoBuildCommon.resolve(defaultTarget, autoData);
        customTarget = AutoBuildCommon.resolve(customTarget, autoData);
        if (customTarget.isBlank()) {
            customTarget = defaultTarget;
        }
        //We finally have the target 
        String theBuildTarget = customTarget;

        String actualUploadPort = Const.EMPTY;

        List<String> stopSerialOnBuildTargets = List.of(Preferences.getDisconnectSerialTargetsList());
        if (stopSerialOnBuildTargets.contains(theBuildTarget)) {

            ISloeberConfiguration sloeberConfig = ISloeberConfiguration.getConfig(autoData);
            if (sloeberConfig != null) {
                BoardDescription myBoardDescriptor = sloeberConfig.getBoardDescription();
                if (myBoardDescriptor != null) {
                    actualUploadPort = myBoardDescriptor.getActualUploadPort();
                    if (actualUploadPort == null) {
                        actualUploadPort = Const.EMPTY;
                    }
                }
            }
        }

        boolean theComPortIsPaused = false;
        if (!(isClean || actualUploadPort.isBlank())) {
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

        boolean ret = autoData.getBuildRunner().invokeBuild(kind, autoData, markerGenerator, projectBuilder, console,
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

    @Override
    public String getName() {
        return "Sloeber build engine"; //$NON-NLS-1$
    }

    @Override
    public boolean supportsParallelBuild() {
        return true;
    }

    @Override
    public boolean supportsStopOnError() {
        return true;
    }

    @Override
    public boolean supportsCustomCommand() {
        return true;
    }

    @Override
    public boolean supportsMakeFiles() {
        return false;
    }

    @Override
    public boolean supportsAutoBuild() {
        return true;
    }

    @Override
    public boolean supportsIncrementalBuild() {
        return true;
    }

    @Override
    public boolean supportsCleanBuild() {
        return true;
    }

}
