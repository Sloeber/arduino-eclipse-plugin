package it.baeyens.arduino.eclipse;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

public class activator implements BundleActivator {
	
	//private activator plugin;

	@Override
	public void start(BundleContext context) throws Exception {
		//super.start(context);
		//plugin = this;
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#stop(org.osgi.framework.BundleContext)
	 */
	@Override
	public void stop(BundleContext context) throws Exception {
		//plugin = null;
		//super.stop(context);
	}
}
