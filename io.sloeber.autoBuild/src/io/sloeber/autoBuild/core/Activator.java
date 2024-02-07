package io.sloeber.autoBuild.core;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Plugin;
import org.eclipse.core.runtime.Status;
import org.eclipse.ui.statushandlers.StatusManager;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;


public class Activator extends Plugin {
    public static final String PLUGIN_ID = "io.sloeber.autoBuild"; //$NON-NLS-1$
    private static BundleContext myBundleContext = null;
	private static Activator instance;

    public static BundleContext getBundleContext() {
        return myBundleContext;
    }

    @Override
    public void start(BundleContext context) throws Exception {
        myBundleContext = context;
        instance = this;

    }

    @Override
    public void stop(BundleContext context) throws Exception {
        // TODO Auto-generated method stub

    }

    public static String getId() {
        return PLUGIN_ID;
    }

    public static void log(Status status) {

		if (status.getSeverity() == IStatus.ERROR) {
			int style = StatusManager.LOG | StatusManager.SHOW | StatusManager.BLOCK;
			StatusManager stMan = StatusManager.getManager();
			stMan.handle(status, style);
		} else {
			instance.getLog().log(status);
		}
    }

    public static void log(Exception e) {
        e.printStackTrace();

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
