package it.baeyens.arduino.monitor.views;

import it.baeyens.arduino.arduino.Serial;

import org.eclipse.nebula.widgets.oscilloscope.multichannel.Oscilloscope;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.ViewPart;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;

public class ScopeView extends ViewPart implements ServiceListener {

    Oscilloscope oscilloscope;

    public ScopeView() {
	// TODO Auto-generated constructor stub
    }

    @Override
    public void createPartControl(Composite parent) {
	parent.setLayout(new GridLayout(1, false));
	oscilloscope = new Oscilloscope(parent, SWT.NONE);
	oscilloscope.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));
	registerSerialTracker();
    }

    private void registerSerialTracker() {
	FrameworkUtil.getBundle(getClass()).getBundleContext().addServiceListener(this);
    }

    @Override
    public void setFocus() {
	oscilloscope.setFocus();
    }

    @Override
    public void serviceChanged(ServiceEvent event) {
	if (event.getType() == ServiceEvent.REGISTERED) {
	    registerSerialService(event);
	} else if (event.getType() == ServiceEvent.UNREGISTERING) {
	    unregisterSerialService(event);
	}
    }

    private void unregisterSerialService(ServiceEvent event) {
	final ServiceReference<?> reference = event.getServiceReference();
	final Object service = FrameworkUtil.getBundle(getClass()).getBundleContext().getService(reference);
	if (service instanceof Serial) {
	    // do something?
	}
    }

    private void registerSerialService(ServiceEvent event) {
	final ServiceReference<?> reference = event.getServiceReference();
	final Object service = FrameworkUtil.getBundle(getClass()).getBundleContext().getService(reference);
	if (service instanceof Serial) {
	    PlatformUI.getWorkbench().getDisplay().syncExec(new Runnable() {
		@Override
		public void run() {
		    Serial serial = (Serial) service;
		    serial.addListener(new ScopeListener(oscilloscope));
		}
	    });
	}
    }
}
