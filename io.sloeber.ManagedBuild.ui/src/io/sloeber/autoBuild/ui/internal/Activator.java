package io.sloeber.autoBuild.ui.internal;

import org.eclipse.core.runtime.Plugin;
import org.osgi.framework.BundleContext;

public class Activator extends Plugin {
    public static final String PLUGIN_ID = "io.sloeber.autoBuild.ui"; //$NON-NLS-1$
    private static Activator plugin;

    @Override
    public void start(BundleContext bundleContext) throws Exception {
        super.start(bundleContext);
        Activator.plugin = this;

    }

    @Override
    public void stop(BundleContext bundleContext) throws Exception {
        super.stop(bundleContext);
        Activator.plugin = null;

    }

    public static String getId() {
        return PLUGIN_ID;
    }

    public static Activator getPlugin() {
        return plugin;
    }
}
