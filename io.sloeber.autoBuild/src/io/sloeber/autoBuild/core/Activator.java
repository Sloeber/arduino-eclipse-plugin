package io.sloeber.autoBuild.core;

import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Plugin;
import org.eclipse.core.runtime.Status;
import org.eclipse.ui.statushandlers.StatusManager;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

import io.sloeber.autoBuild.internal.AutobuilResourceChangeListener;


public class Activator extends Plugin {
    public static final String PLUGIN_ID = "io.sloeber.autoBuild"; //$NON-NLS-1$
    private static BundleContext myBundleContext = null;
	private static Activator instance;
    private static AutobuilResourceChangeListener myResourceChangelistener = new AutobuilResourceChangeListener();

    public static BundleContext getBundleContext() {
        return myBundleContext;
    }

    public static Activator getInstance() {
    	return instance;
    }

    @Override
    public void start(BundleContext context) throws Exception {
        myBundleContext = context;
        instance = this;
        ResourcesPlugin.getWorkspace().addResourceChangeListener(myResourceChangelistener,
                IResourceChangeEvent.POST_CHANGE);
    }

    @Override
    public void stop(BundleContext context) throws Exception {
        ResourcesPlugin.getWorkspace().removeResourceChangeListener(myResourceChangelistener);

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
