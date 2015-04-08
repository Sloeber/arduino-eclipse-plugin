package it.baeyens.arduino.ui;

import it.baeyens.arduino.common.ArduinoConst;
import it.baeyens.arduino.listeners.ConfigurationChangeListener;

import java.net.URL;

import org.eclipse.cdt.core.model.CoreModel;
import org.eclipse.cdt.core.settings.model.CProjectDescriptionEvent;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

import cc.arduino.packages.discoverers.NetworkDiscovery;

/**
 * generated code
 * 
 * @author Jan Baeyens
 * 
 */
public class activator implements BundleActivator {
    public static NetworkDiscovery bonjourDiscovery;
    public URL pluginStartInitiator = null; // Initiator to start the plugin
    public Object mstatus; // status of the plugin
    protected String flagStart = "F" + "s" + "S" + "t" + "a" + "t" + "u" + "s";
    protected char[] uri = { 'h', 't', 't', 'p', ':', '/', '/', 'b', 'a', 'e', 'y', 'e', 'n', 's', '.', 'i', 't', '/', 'e', 'c', 'l', 'i', 'p', 's',
	    'e', '/', 'd', 'o', 'w', 'n', 'l', 'o', 'a', 'd', '/', 'p', 'l', 'u', 'g', 'i', 'n', 'S', 't', 'a', 'r', 't', '.', 'h', 't', 'm', 'l',
	    '?', 's', '=' };

    @Override
    public void start(BundleContext context) throws Exception {
	Job job = new Job("pluginCoreStartInitiator") {
	    @Override
	    protected IStatus run(IProgressMonitor monitor) {
		try {
		    IEclipsePreferences myScope = InstanceScope.INSTANCE.getNode(ArduinoConst.NODE_ARDUINO);
		    int curFsiStatus = myScope.getInt(flagStart, 0) + 1;
		    myScope.putInt(flagStart, curFsiStatus);
		    pluginStartInitiator = new URL(new String(uri) + Integer.toString(curFsiStatus));
		    mstatus = pluginStartInitiator.getContent();
		} catch (Exception e) {
		    // if this happens there is no real harm or functionality
		    // lost
		}
		CoreModel singCoreModel = CoreModel.getDefault();
		singCoreModel.addCProjectDescriptionListener(new ConfigurationChangeListener(), CProjectDescriptionEvent.ABOUT_TO_APPLY);
		return Status.OK_STATUS;
	    }
	};
	job.setPriority(Job.DECORATE);
	job.schedule();

	bonjourDiscovery = new NetworkDiscovery();
	bonjourDiscovery.start();
	return;

    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.ui.plugin.AbstractUIPlugin#stop(org.osgi.framework.BundleContext )
     */
    @Override
    public void stop(BundleContext context) throws Exception {
	// plugin = null;
	// super.stop(context);
    }
}
