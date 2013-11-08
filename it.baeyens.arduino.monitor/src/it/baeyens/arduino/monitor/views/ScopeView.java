package it.baeyens.arduino.monitor.views;

import it.baeyens.arduino.arduino.Serial;
import multichannel.Oscilloscope;
import multichannel.OscilloscopeDispatcher;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.ViewPart;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;

public class ScopeView extends ViewPart implements ServiceListener {

    Oscilloscope myScope = null;
    ScopeListener myScopelistener = null;
    Serial mySerial = null;

    public ScopeView() {
	// TODO Can we do something here to register if the serial port is already open?
    }

    @Override
    public void createPartControl(Composite parent) {
	parent.setLayout(new GridLayout(1, false));
	OscilloscopeDispatcher dsp = new OscilloscopeDispatcher(0) {

	    @Override
	    public void dispatch() {
		init();

		for (int i = 0; i < getOscilloscope().getChannels(); i++) {

		    myScope.setPercentage(i, false);
		    myScope.setSteady(i, isSteady(), getSteadyPosition());
		    myScope.setFade(i, false);
		    myScope.setTailFade(i, 0);
		    myScope.setConnect(i, false);
		    myScope.setLineWidth(i, 1);
		    myScope.setBaseOffset(i, 0);
		    myScope.SetChannelName(i, "Channel " + Integer.toString(i));
		}
		myScope.setShowLabels(true);
	    }

	    @Override
	    public int getDelayLoop() {
		return 30;
	    }

	    @Override
	    public boolean getFade() {
		return false;
	    }

	    @Override
	    public int getTailSize() {
		return myScope.getSize().x - 10; // Oscilloscope.TAILSIZE_MAX;
	    }
	};
	myScope = new Oscilloscope(6, dsp, parent, SWT.NONE);
	GridData theGriddata = new GridData(SWT.FILL, SWT.FILL, true, true);
	theGriddata.horizontalSpan = 7;
	int ScopeColors[] = { SWT.COLOR_BLUE, SWT.COLOR_CYAN, SWT.COLOR_DARK_BLUE, SWT.COLOR_DARK_GRAY, SWT.COLOR_DARK_GREEN, SWT.COLOR_DARK_RED,
		SWT.COLOR_DARK_YELLOW, SWT.COLOR_DARK_CYAN, SWT.COLOR_WHITE, SWT.COLOR_DARK_MAGENTA };
	myScope.setLayoutData(theGriddata);
	for (int i = 0; i < myScope.getChannels(); i++) {
	    myScope.setForeground(i, Display.getDefault().getSystemColor(ScopeColors[i]));
	}
	myScope.getDispatcher(0).dispatch();

	Listener listener = new Listener() {
	    boolean inDrag = false;
	    boolean inSize = false;
	    int orgLowRange = 0;
	    int orgHighRange = 0;
	    int orgY = 0;
	    double ValueAtScrollPoint = 0;
	    double scale = 1;
	    double orgHeight;
	    double scrollPointPercentage;

	    @Override
	    public void handleEvent(Event event) {

		switch (event.type) {
		case SWT.MouseDown:
		    if (!(inDrag || inSize)) {
			orgLowRange = myScope.getLowRangeValue();
			orgHighRange = myScope.getHighRangeValue();
			scale = (((float) (myScope.getHighRangeValue() - myScope.getLowRangeValue())) / (float) myScope.getSize().y);
			orgY = event.y;
			switch (event.button) {
			case 1:
			    inDrag = true;
			    break;
			case 3:
			    orgHeight = orgHighRange - orgLowRange;
			    scrollPointPercentage = (double) event.y / (double) myScope.getSize().y;
			    ValueAtScrollPoint = orgHighRange - scrollPointPercentage * orgHeight;
			    inSize = true;
			    break;
			default:
			    break;
			}
		    }
		    break;
		case SWT.MouseMove:
		    if (inDrag) {
			myScope.setRange((int) (orgLowRange - (orgY - event.y) * scale), (int) (orgHighRange - (orgY - event.y) * scale));
		    }
		    if (inSize) {
			double newscale = Math.max(scale * (1.0 + (orgY - event.y) * 0.01), 1.0);
			int newHeight = (int) (orgHeight / scale * newscale);
			int NewHighValue = (int) (ValueAtScrollPoint + scrollPointPercentage * newHeight);
			myScope.setRange(NewHighValue - newHeight, NewHighValue);
		    }
		    break;
		case SWT.MouseUp:
		    inDrag = false;
		    inSize = false;
		    break;
		default:
		    break;
		}
	    }

	};
	myScope.addListener(SWT.MouseDown, listener);
	myScope.addListener(SWT.MouseUp, listener);
	myScope.addListener(SWT.MouseMove, listener);
	myScope.addControlListener(new ControlAdapter() {
	    @Override
	    public void controlResized(ControlEvent e) {
		int scopeWidth = myScope.getSize().x - 10;
		for (int i = 0; i < myScope.getChannels(); i++) {
		    myScope.setSteady(i, true, scopeWidth);
		    myScope.setTailSize(i, scopeWidth);
		}
		myScope.setnewBackgroundImage();
		myScope.redraw();
	    }

	});
	myScope.setRange(0, 1050); // I set this as starting range
	myScope.setStatus("Use the serial monitor to connect to a Serial port.");
	myScope.setShowLabels(false);
	myScope.setnewBackgroundImage();

	registerSerialTracker();
    }

    private void registerSerialTracker() {
	FrameworkUtil.getBundle(getClass()).getBundleContext().addServiceListener(this);
    }

    @Override
    public void setFocus() {
	myScope.setFocus();
	myScope.redraw();
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
	    if (service == mySerial) {
		mySerial.removeListener(myScopelistener);
		myScope.setStatus("disconnected from:" + mySerial.toString());
		myScope.setShowLabels(true);
		myScope.setnewBackgroundImage();
		myScopelistener.dispose();
		myScopelistener = null;
		mySerial = null;
	    }
	}
    }

    private void registerSerialService(ServiceEvent event) {
	final ServiceReference<?> reference = event.getServiceReference();
	final Object service = FrameworkUtil.getBundle(getClass()).getBundleContext().getService(reference);
	if ((service instanceof Serial) && (myScopelistener == null) && (myScope != null)) {
	    myScopelistener = new ScopeListener(myScope);
	    mySerial = (Serial) service;
	    PlatformUI.getWorkbench().getDisplay().syncExec(new Runnable() {
		@Override
		public void run() {
		    mySerial.addListener(myScopelistener);
		    myScope.setStatus("connected to:" + mySerial.toString());
		    myScope.setShowLabels(true);
		    myScope.setnewBackgroundImage();
		}
	    });
	}
    }

    @Override
    public void dispose() {
	// As we have a listener we need to remove the listener
	if ((myScopelistener != null) && (mySerial != null)) {
	    Serial tempSerial = mySerial;
	    mySerial = null;
	    tempSerial.removeListener(myScopelistener);
	    myScopelistener.dispose();
	    myScopelistener = null;
	    myScope.dispose();
	    myScope = null;
	}
	super.dispose();
    }
}
