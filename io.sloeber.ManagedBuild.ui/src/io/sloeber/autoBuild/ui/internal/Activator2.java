package io.sloeber.autoBuild.ui.internal;

import org.eclipse.core.runtime.Plugin;
import org.osgi.framework.BundleContext;

public class Activator2 extends Plugin {
    public static final String PLUGIN_ID = "io.sloeber.autoBuild.ui"; //$NON-NLS-1$
    private static Activator2 plugin;

    @Override
    public void start(BundleContext bundleContext) throws Exception {
        super.start(bundleContext);
        Activator2.plugin = this;

    }

    @Override
    public void stop(BundleContext bundleContext) throws Exception {
        super.stop(bundleContext);
        Activator2.plugin = null;

    }

    public static String getId() {
        return PLUGIN_ID;
    }

    public static Activator2 getPlugin() {
        return plugin;
    }
}
