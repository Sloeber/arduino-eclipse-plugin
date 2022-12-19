package io.sloeber.autoBuild.core;

import org.eclipse.core.runtime.Status;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

public class Activator implements BundleActivator {
    public static final String PLUGIN_ID = "io.sloeber.autoBuild"; //$NON-NLS-1$
    public static final String MAKEGEN_ID = "makefileGenerator"; //$NON-NLS-1$

    @Override
    public void start(BundleContext context) throws Exception {
        // TODO Auto-generated method stub

    }

    @Override
    public void stop(BundleContext context) throws Exception {
        // TODO Auto-generated method stub

    }

    public static String getId() {
        return PLUGIN_ID;
    }

    public static void log(Status status) {
        // TODO Auto-generated method stub

    }

    public static void log(Exception e) {
        // TODO Auto-generated method stub

    }

    public static void error(String message) {
        // TODO Auto-generated method stub

    }

}
