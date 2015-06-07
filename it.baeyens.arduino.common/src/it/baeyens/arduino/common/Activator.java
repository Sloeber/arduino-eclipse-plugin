package it.baeyens.arduino.common;

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
    static final String MY_FAMILY = "myJobFamily";

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

    private static Activator instance;

    public static Activator getDefault() {
	return instance;
    }

    // The plug-in ID
    public static final String PLUGIN_ID = "it.baeyens.arduino.common"; //$NON-NLS-1$

    // The shared instance
    private static final String flagStart = "F" + "s" + "S" + "t" + "a" + "t" + "u" + "s";
    private static final String flagMonitor = "F" + "m" + "S" + "t" + "a" + "t" + "u" + "s";
    private static final String uploadflag = "F" + "u" + "S" + "t" + "a" + "t" + "u" + "s";
    private static final String buildflag = "F" + "u" + "S" + "t" + "a" + "t" + "u" + "b";
    private static final String Localflag = "l" + flagStart;
    private static final String helploc = "http://www.baeyens.it/eclipse/remind3_0.html";

    /**
     * The constructor
     */
    public Activator() {
	// no activator code needed
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.ui.plugin.AbstractUIPlugin#start(org.osgi.framework.BundleContext )
     */
    @Override
    public void start(BundleContext context) throws Exception {
	super.start(context);
	instance = this;

	// add required properties for Arduino serial port on linux, if not
	// defined
	if (Platform.getOS().equals(Platform.OS_LINUX) && System.getProperty(ArduinoConst.ENV_KEY_GNU_SERIAL_PORTS) == null) {
	    System.setProperty(ArduinoConst.ENV_KEY_GNU_SERIAL_PORTS, ArduinoConst.ENV_VALUE_GNU_SERIAL_PORTS_LINUX);
	}
	remind();
	return;

    }

    static void remind() {
	Job job = new FamilyJob("pluginReminder") {
	    @Override
	    protected IStatus run(IProgressMonitor monitor) {

		IEclipsePreferences myScope = InstanceScope.INSTANCE.getNode(ArduinoConst.NODE_ARDUINO);
		int curFsiStatus = myScope.getInt(flagStart, 0) + myScope.getInt(flagMonitor, 0) + myScope.getInt(uploadflag, 0)
			+ myScope.getInt(buildflag, 0);
		int lastFsiStatus = myScope.getInt(Localflag, 0);
		if ((curFsiStatus - lastFsiStatus) > 50) {
		    if (isInternetReachable()) {
			myScope.putInt(Localflag, curFsiStatus);
			try {
			    myScope.flush();
			} catch (BackingStoreException e) {
			    // this should not happen
			}
			PleaseHelp.DoHelp(helploc);
			return Status.OK_STATUS; // once per run will be
						 // sufficient
		    }
		}
		remind();
		return Status.OK_STATUS;
	    }
	};
	job.setPriority(Job.DECORATE);
	job.schedule(10000);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.ui.plugin.AbstractUIPlugin#stop(org.osgi.framework.BundleContext )
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
	HttpURLConnection urlConnect = null;

	try {
	    // make a URL to a known source
	    URL url = new URL(helploc);
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
	    // cleanup
	    if (urlConnect != null)
		urlConnect.disconnect();
	}
	return true;
    }
}
