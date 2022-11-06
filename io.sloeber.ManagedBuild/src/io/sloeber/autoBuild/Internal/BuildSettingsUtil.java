package io.sloeber.autoBuild.Internal;

import org.eclipse.cdt.core.settings.model.ICProjectDescription;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;

import io.sloeber.autoBuild.api.IConfiguration;
import io.sloeber.autoBuild.api.IManagedBuildInfo;
import io.sloeber.autoBuild.api.ITool;

public class BuildSettingsUtil {

    public static void disconnectDepentents(IConfiguration parent, ITool[] removed) {
        // TODO Auto-generated method stub

    }

    public static void checkApplyDescription(IProject project, ICProjectDescription des) throws CoreException {
        // TODO Auto-generated method stub

    }

    public static ICProjectDescription checkSynchBuildInfo(IProject project) throws CoreException {
        // TODO Auto-generated method stub
        return null;
    }

    public static boolean applyConfiguration(IConfiguration cfg, ICProjectDescription projDes, boolean b) {
        // TODO Auto-generated method stub
        return false;
    }

    public static void checkApplyDescription(IProject project, ICProjectDescription projDes,
            boolean avoidSerialization) {
        // TODO Auto-generated method stub

    }

    public static ICProjectDescription synchBuildInfo(IManagedBuildInfo info, ICProjectDescription projDes,
            boolean force) {
        // TODO Auto-generated method stub
        return null;
    }

}
