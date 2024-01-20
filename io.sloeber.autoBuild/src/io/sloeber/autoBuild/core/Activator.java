package io.sloeber.autoBuild.core;

import org.eclipse.cdt.core.build.IToolChainManager;
import org.eclipse.core.runtime.Status;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

public class Activator implements BundleActivator {
    public static final String PLUGIN_ID = "io.sloeber.autoBuild"; //$NON-NLS-1$
    private static BundleContext myBundleContext = null;

    public static BundleContext getBundleContext() {
        return myBundleContext;
    }

    @Override
    public void start(BundleContext context) throws Exception {
        myBundleContext = context;

    }

    @Override
    public void stop(BundleContext context) throws Exception {
        // TODO Auto-generated method stub

    }

    public static String getId() {
        return PLUGIN_ID;
    }

    public static void log(Status status) {
        //TOFIX     status.

    }

    public static void log(Exception e) {
        e.printStackTrace();

    }

    public static void error(String message) {
        // TODO Auto-generated method stub

    }

	/**
	 * Return the given OSGi service.
	 *
	 * @param service service class
	 * @return service
	 * @since 6.0
	 */
	public static <T> T getService(Class<T> service) {
		BundleContext context = myBundleContext.getBundle().getBundleContext();
		ServiceReference<T> ref = context.getServiceReference(service);
		return ref != null ? context.getService(ref) : null;
	}

}
