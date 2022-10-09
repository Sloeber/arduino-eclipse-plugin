package io.sloeber.managedBuild.Internal;

import java.util.List;
import java.util.Set;

import org.eclipse.cdt.core.settings.model.util.PathSettingsContainer;
import org.eclipse.cdt.managedbuilder.core.IFileInfo;
import org.eclipse.cdt.managedbuilder.core.IResourceInfo;
import org.eclipse.cdt.managedbuilder.core.ITool;
import org.eclipse.core.runtime.IPath;

public class ToolInfoHolder {
    public ITool[] buildTools;
    public boolean[] buildToolsUsed;
    public ArduinoManagedBuildGnuToolInfo[] gnuToolInfos;
    public Set<String> outputExtensionsSet;
    public List<IPath> dependencyMakefiles;

    public static ToolInfoHolder getToolInfo(ArduinoGnuMakefileGenerator caller, IPath path) {
        return getToolInfo(caller, path, false);
    }

    public static ToolInfoHolder getToolInfo(ArduinoGnuMakefileGenerator caller, IPath path, boolean create) {
        PathSettingsContainer child = caller.toolInfos.getChildContainer(path, create, create);
        ToolInfoHolder h = null;
        if (child != null) {
            h = (ToolInfoHolder) child.getValue();
            if (h == null && create) {
                h = new ToolInfoHolder();
                child.setValue(h);
            }
        }
        return h;
    }

    public static ToolInfoHolder getFolderToolInfo(ArduinoGnuMakefileGenerator caller, IPath path) {
        IResourceInfo rcInfo = caller.config.getResourceInfo(path, false);
        while (rcInfo instanceof IFileInfo) {
            path = path.removeLastSegments(1);
            rcInfo = caller.config.getResourceInfo(path, false);
        }
        return getToolInfo(caller, path, false);
    }
}
