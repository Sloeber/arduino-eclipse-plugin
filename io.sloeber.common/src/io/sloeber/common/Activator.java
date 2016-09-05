package io.sloeber.common;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.UnknownHostException;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.IJobManager;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;
import org.osgi.service.prefs.BackingStoreException;

abstract class FamilyJob extends Job {
    static final String MY_FAMILY = "myJobFamily"; //$NON-NLS-1$

    public FamilyJob(String name) {
	super(name);
    }

    @Override
    public boolean belongsTo(Object family) {
	return family == MY_FAMILY;
    }

}

/**
 * The activator class controls the plug-in life cycle
 */
public class Activator extends AbstractUIPlugin {

    // The plug-in ID
    public static final String PLUGIN_ID = "io.sloeber.common"; //$NON-NLS-1$

    // The shared instance
    private static final String FLAGS_TART = "F" + "s" + "S" + "t" + "a" + "t" + "u" + "s"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$ //$NON-NLS-7$ //$NON-NLS-8$
    private static final String FLAG_MONITOR = "F" + "m" + "S" + "t" + "a" + "t" + "u" + "s"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$ //$NON-NLS-7$ //$NON-NLS-8$
    private static final String UPLOAD_FLAG = "F" + "u" + "S" + "t" + "a" + "t" + "u" + "s"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$ //$NON-NLS-7$ //$NON-NLS-8$
    private static final String BUILD_FLAG = "F" + "u" + "S" + "t" + "a" + "t" + "u" + "b"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$ //$NON-NLS-7$ //$NON-NLS-8$
    private static final String LOCAL_FLAG = "l" + FLAGS_TART; //$NON-NLS-1$
    private static final String HELP_LOC = "http://www.baeyens.it/eclipse/remind.php"; //$NON-NLS-1$

    private static Activator instance;

    /**
     * The constructor
     */
    public Activator() {
	// no activator code needed
    }

    public static Activator getDefault() {
	return instance;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.ui.plugin.AbstractUIPlugin#start(org.osgi.framework.
     * BundleContext )
     */
    @Override
    public void start(BundleContext context) throws Exception {
	super.start(context);
	instance = this;

	// add required properties for Arduino serial port on linux, if not
	// defined
	if (Platform.getOS().equals(Platform.OS_LINUX) && System.getProperty(Const.ENV_KEY_GNU_SERIAL_PORTS) == null) {
	    System.setProperty(Const.ENV_KEY_GNU_SERIAL_PORTS, Const.ENV_VALUE_GNU_SERIAL_PORTS_LINUX);
	}
	remind();
	return;

    }

    static void remind() {
	Job job = new FamilyJob("pluginReminder") { //$NON-NLS-1$
	    @Override
	    protected IStatus run(IProgressMonitor monitor) {

		IEclipsePreferences myScope = InstanceScope.INSTANCE.getNode(Const.NODE_ARDUINO);
		int curFsiStatus = myScope.getInt(FLAGS_TART, 0) + myScope.getInt(FLAG_MONITOR, 0)
			+ myScope.getInt(UPLOAD_FLAG, 0) + myScope.getInt(BUILD_FLAG, 0);
		int lastFsiStatus = myScope.getInt(LOCAL_FLAG, 0);
		if ((curFsiStatus - lastFsiStatus) >= 50 && isInternetReachable()) {
		    myScope.putInt(LOCAL_FLAG, curFsiStatus);
		    try {
			myScope.flush();
		    } catch (BackingStoreException e) {
			// this should not happen
		    }
		    PleaseHelp.doHelp(HELP_LOC);
		    return Status.OK_STATUS;
		}
		remind();
		return Status.OK_STATUS;
	    }
	};
	job.setPriority(Job.DECORATE);
	job.schedule(60000);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.ui.plugin.AbstractUIPlugin#stop(org.osgi.framework.
     * BundleContext )
     */
    @Override
    public void stop(BundleContext context) throws Exception {
	IJobManager jobMan = Job.getJobManager();
	jobMan.cancel(FamilyJob.MY_FAMILY);
	jobMan.join(FamilyJob.MY_FAMILY, null);
	instance = null;
	super.stop(context);
    }

    static boolean isInternetReachable() {
	boolean ret = false;
	HttpURLConnection urlConnect = null;

	try {
	    // make a URL to a known source
	    URL url = new URL(HELP_LOC + "?systemhash=" + ConfigurationPreferences.getSystemHash());//$NON-NLS-1$
	    // open a connection to that source
	    urlConnect = (HttpURLConnection) url.openConnection();
	    // trying to retrieve data from the source. If there is no
	    // connection, this line will fail
	    urlConnect.getContent();
	} catch (UnknownHostException e) {
	    return false;
	} catch (IOException e) {
	    return false;
	} finally {
	    if (urlConnect != null) {
		try {
		    ret = (urlConnect.getResponseCode() != 404);
		} catch (IOException e) {
		    // ignore
		}
		urlConnect.disconnect();
	    }

	}
	return ret;
    }
}
