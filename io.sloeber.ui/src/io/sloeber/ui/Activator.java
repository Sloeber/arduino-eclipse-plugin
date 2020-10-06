package io.sloeber.ui;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.IJobManager;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.ResourceLocator;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.eclipse.ui.progress.UIJob;
import org.eclipse.ui.statushandlers.StatusManager;
import org.osgi.framework.BundleContext;
import org.osgi.service.prefs.BackingStoreException;
import io.sloeber.core.api.LibraryManager;
import io.sloeber.ui.helpers.MyPreferences;
import io.sloeber.ui.listeners.MyLibraryInstallHandler;
import io.sloeber.ui.listeners.ProjectExplorerListener;

/**
 * generated code
 *
 * @author Jan Baeyens
 *
 */
@SuppressWarnings("nls")
public class Activator extends AbstractUIPlugin {
	static abstract class FamilyJob extends Job {
		static final String MY_FAMILY = "myJobFamily"; //$NON-NLS-1$

		public FamilyJob(String name) {
			super(name);
		}

		@Override
		public boolean belongsTo(Object family) {
			return family == MY_FAMILY;
		}

	}
	private static final String PLUGIN_ID = "io.sloeber.core.ui"; //$NON-NLS-1$
	public static final String NODE_ARDUINO = "io.sloeber.arduino";
	private static Activator instance;
	private static BundleContext myContext;
	// The shared instance
	private static final String FLAG_START = "F" + "s" + "S" + "t" + "a" + "t" + "u" + "s";
	private static final String UPLOAD_FLAG = "F" + "u" + "S" + "t" + "a" + "t" + "u" + "s";
	private static final String BUILD_FLAG = "F" + "b" + "S" + "t" + "a" + "t" + "u" + "s";
	private static final String LOCAL_FLAG = "l" + FLAG_START;
	private static final String HELP_LOC = "https://www.baeyens.it/eclipse/remind.php";
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
				LibraryManager.registerInstallLibraryHandler(new MyLibraryInstallHandler());
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
		return ResourceLocator.imageDescriptorFromBundle(PLUGIN_ID, path).orElse(null);
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
		remind();

	}

	@Override
	public void stop(BundleContext context) throws Exception {
		IJobManager jobMan = Job.getJobManager();
		jobMan.cancel(FamilyJob.MY_FAMILY);
		jobMan.join(FamilyJob.MY_FAMILY, null);
	}
	
	static void remind() {

		Job job = new FamilyJob("pluginReminder") { //$NON-NLS-1$
			@Override
			protected IStatus run(IProgressMonitor monitor) {

				IEclipsePreferences myScope = InstanceScope.INSTANCE.getNode(NODE_ARDUINO);
				int curFsStatus = myScope.getInt(FLAG_START, 0);
				int curFuStatus = myScope.getInt(UPLOAD_FLAG, 0);
				int curFbStatus = myScope.getInt(BUILD_FLAG, 0);
				int curFsiStatus = curFsStatus + curFuStatus + curFbStatus;
				int lastFsiStatus = myScope.getInt(LOCAL_FLAG, 0);
				final int trigger = 30;
				if ((curFsiStatus - lastFsiStatus) < 0) {
					lastFsiStatus = curFsiStatus - (trigger + 1);
				}
				if ((curFsiStatus - lastFsiStatus) >= trigger) {
					myScope.putInt(LOCAL_FLAG, curFsiStatus);
					try {
						myScope.flush();
					} catch (BackingStoreException e) {
						// this should not happen
					}
					PleaseHelp.doHelp(HELP_LOC);
				}
				remind();
				return Status.OK_STATUS;
			}
		};
		job.setPriority(Job.DECORATE);
		job.schedule(60000);
	}
}
