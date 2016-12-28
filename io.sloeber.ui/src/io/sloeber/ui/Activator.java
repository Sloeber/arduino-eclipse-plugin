package io.sloeber.ui;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.eclipse.ui.progress.UIJob;
import org.eclipse.ui.statushandlers.StatusManager;
import org.osgi.framework.BundleContext;

import io.sloeber.ui.helpers.MyPreferences;
import io.sloeber.ui.listeners.ProjectExplorerListener;

/**
 * generated code
 * 
 * @author Jan Baeyens
 * 
 */
public class Activator extends AbstractUIPlugin {

	private static final String PLUGIN_ID = "io.sloeber.core.ui"; //$NON-NLS-1$
	private static Activator instance;
	private static BundleContext myContext;

	/**
	 * This is a wrapper method to quickly make the dough code that is the basis
	 * of the io.sloeber.core.managers and io.sloeber.core.managers.ui to work.
	 */
	public static String getId() {
		return PLUGIN_ID;
	}

	private static void runGUIRegistration() {
		UIJob installJob = new UIJob("Gui Registration") { //$NON-NLS-1$

			@Override
			public IStatus runInUIThread(IProgressMonitor monitor) {
				ProjectExplorerListener.registerListener();
				return Status.OK_STATUS;
			}

		};
		installJob.setPriority(Job.BUILD);
		installJob.setUser(true);
		installJob.schedule();
	}

	public static Activator getDefault() {
		return instance;
	}

	public static BundleContext getContext() {
		return myContext;
	}

	/**
	 * Logs the status information
	 * 
	 * @param status
	 *            the status information to log
	 */
	public static void log(IStatus status) {
		int style;

		if (status.getSeverity() == IStatus.ERROR) {
			style = StatusManager.LOG | StatusManager.SHOW | StatusManager.BLOCK;
			StatusManager stMan = StatusManager.getManager();
			stMan.handle(status, style);
		} else {
			Activator.getDefault().getLog().log(status);
		}
	}

	private static void initializeImportantVariables() {
		// Make sure some important variables are being initialized
		MyPreferences.setOpenSerialWithMonitor(MyPreferences.getOpenSerialWithMonitor());
		MyPreferences.setCleanSerialMonitorAfterUpload(MyPreferences.getCleanSerialMonitorAfterUpload());
	}

	/**
	 * Returns an image descriptor for the image file at the given plug-in
	 * relative path
	 * 
	 * @param path
	 *            the path
	 * @return the image descriptor
	 */
	public static ImageDescriptor getImageDescriptor(String path) {
		return imageDescriptorFromPlugin(PLUGIN_ID, path);
	}

	public static final short PLOTTER_START_DATA = (short) 0xCDAB;// This is the
	// 205 171 or
	// -85 -51 flag
	// that
	// indicates
	// plotter data
	// is following
	// least
	// significant
	// first
	// 0xCDAB;

	@Override
	public void start(BundleContext context) throws Exception {
		super.start(context);
		myContext = context;
		instance = this;
		initializeImportantVariables();
		runGUIRegistration();
	}

	@Override
	public void stop(BundleContext context) throws Exception {
		// nothing to do here

	}
}
