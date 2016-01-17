package it.baeyens.arduino.monitor.views;

import java.net.URL;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.jface.resource.ColorRegistry;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.ViewPart;
import org.eclipse.ui.themes.ITheme;
import org.eclipse.ui.themes.IThemeManager;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;

import it.baeyens.arduino.arduino.Serial;
import it.baeyens.arduino.common.ArduinoConst;
import it.baeyens.arduino.monitor.Activator;
import multichannel.Oscilloscope;
import multichannel.OscilloscopeDispatcher;

public class ScopeView extends ViewPart implements ServiceListener {

    Oscilloscope myScope = null;
    ScopeListener myScopelistener = null;
    Serial mySerial = null;

    private static final String flagMonitor = "FmStatus"; //$NON-NLS-1$
    String uri = "h tt p://bae yens.i t/ec li pse/do wnl oad/Sc opeS tart.h t ml?m="; //$NON-NLS-1$
    public Object mstatus; // status of the scope

    public ScopeView() {

	Job job = new Job("pluginSerialmonitorInitiator") { //$NON-NLS-1$
	    @Override
	    protected IStatus run(IProgressMonitor monitor) {
		try {
		    IEclipsePreferences mySCope = InstanceScope.INSTANCE.getNode(ArduinoConst.NODE_ARDUINO);
		    int curFsiStatus = mySCope.getInt(flagMonitor, 0) + 1;
		    mySCope.putInt(flagMonitor, curFsiStatus);
		    URL pluginStartInitiator = new URL(
			    ScopeView.this.uri.replaceAll(" ", ArduinoConst.EMPTY_STRING) + Integer.toString(curFsiStatus)); //$NON-NLS-1$
		    ScopeView.this.mstatus = pluginStartInitiator.getContent();
		} catch (Exception e) {// JABA is not going to add code
		}
		return Status.OK_STATUS;
	    }
	};
	job.setPriority(Job.DECORATE);
	job.schedule();
    }

    @Override
    public void createPartControl(Composite parent) {
	parent.setLayout(new GridLayout(1, false));
	OscilloscopeDispatcher dsp = new OscilloscopeDispatcher() {

	    @Override
	    public void dispatch() {
		init();

		for (int i = 0; i < getOscilloscope().getChannels(); i++) {

		    ScopeView.this.myScope.setPercentage(i, false);
		    ScopeView.this.myScope.setSteady(i, isSteady(), getSteadyPosition());
		    ScopeView.this.myScope.setFade(i, false);
		    ScopeView.this.myScope.setTailFade(i, 0);
		    ScopeView.this.myScope.setConnect(i, false);
		    ScopeView.this.myScope.setLineWidth(i, 1);
		    ScopeView.this.myScope.setBaseOffset(i, 0);
		    ScopeView.this.myScope.SetChannelName(i, Messages.ScopeView_channel + Integer.toString(i));
		}
		ScopeView.this.myScope.setShowLabels(true);
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
		return ScopeView.this.myScope.getSize().x - 10; // Oscilloscope.TAILSIZE_MAX;
	    }
	};

	IThemeManager themeManager = PlatformUI.getWorkbench().getThemeManager();
	ITheme currentTheme = themeManager.getCurrentTheme();
	ColorRegistry colorRegistry = currentTheme.getColorRegistry();

	this.myScope = new Oscilloscope(6, dsp, parent, SWT.NONE, colorRegistry.get("it.baeyens.scope.color.background"), //$NON-NLS-1$
		colorRegistry.get("it.baeyens.scope.color.foreground"), colorRegistry.get("it.baeyens.scope.color.grid")); //$NON-NLS-1$//$NON-NLS-2$
	GridData theGriddata = new GridData(SWT.FILL, SWT.FILL, true, true);
	theGriddata.horizontalSpan = 7;

	this.myScope.setLayoutData(theGriddata);
	for (int i = 0; i < this.myScope.getChannels(); i++) {
	    String colorID = "it.baeyens.scope.color." + (1 + i); //$NON-NLS-1$
	    Color color = colorRegistry.get(colorID);
	    this.myScope.setForeground(i, color);
	}
	this.myScope.getDispatcher(0).dispatch();

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
		    if (!(this.inDrag || this.inSize)) {
			this.orgLowRange = ScopeView.this.myScope.getLowRangeValue();
			this.orgHighRange = ScopeView.this.myScope.getHighRangeValue();
			this.scale = (((float) (ScopeView.this.myScope.getHighRangeValue() - ScopeView.this.myScope.getLowRangeValue()))
				/ (float) ScopeView.this.myScope.getSize().y);
			this.orgY = event.y;
			switch (event.button) {
			case 1:
			    this.inDrag = true;
			    break;
			case 3:
			    this.orgHeight = this.orgHighRange - this.orgLowRange;
			    this.scrollPointPercentage = (double) event.y / (double) ScopeView.this.myScope.getSize().y;
			    this.ValueAtScrollPoint = this.orgHighRange - this.scrollPointPercentage * this.orgHeight;
			    this.inSize = true;
			    break;
			default:
			    break;
			}
		    }
		    break;
		case SWT.MouseMove:
		    if (this.inDrag) {
			ScopeView.this.myScope.setRange((int) (this.orgLowRange - (this.orgY - event.y) * this.scale),
				(int) (this.orgHighRange - (this.orgY - event.y) * this.scale));
		    }
		    if (this.inSize) {
			double newscale = Math.max(this.scale * (1.0 + (this.orgY - event.y) * 0.01), 1.0);
			int newHeight = (int) (this.orgHeight / this.scale * newscale);
			int NewHighValue = (int) (this.ValueAtScrollPoint + this.scrollPointPercentage * newHeight);
			ScopeView.this.myScope.setRange(NewHighValue - newHeight, NewHighValue);
		    }
		    break;
		case SWT.MouseUp:
		    this.inDrag = false;
		    this.inSize = false;
		    break;
		default:
		    break;
		}
	    }

	};
	this.myScope.addListener(SWT.MouseDown, listener);
	this.myScope.addListener(SWT.MouseUp, listener);
	this.myScope.addListener(SWT.MouseMove, listener);
	this.myScope.addControlListener(new ControlAdapter() {
	    @Override
	    public void controlResized(ControlEvent e) {
		int scopeWidth = ScopeView.this.myScope.getSize().x - 10;
		for (int i = 0; i < ScopeView.this.myScope.getChannels(); i++) {
		    ScopeView.this.myScope.setSteady(i, true, scopeWidth);
		    ScopeView.this.myScope.setTailSize(i, scopeWidth);
		}
		ScopeView.this.myScope.setnewBackgroundImage();
		ScopeView.this.myScope.redraw();
	    }

	});
	this.myScope.setRange(0, 1050); // I set this as starting range
	this.myScope.setStatus("Use the serial monitor to connect to a Serial port."); //$NON-NLS-1$
	this.myScope.setShowLabels(false);
	this.myScope.setnewBackgroundImage();

	registerSerialTracker();
    }

    private void registerSerialTracker() {
	registerExistingSerialService();
	FrameworkUtil.getBundle(getClass()).getBundleContext().addServiceListener(this);
    }

    @Override
    public void setFocus() {
	this.myScope.setFocus();
	this.myScope.redraw();
    }

    @Override
    public void serviceChanged(ServiceEvent event) {
	if (event.getType() == ServiceEvent.REGISTERED) {
	    registerSerialService(event);
	} else if (event.getType() == ServiceEvent.UNREGISTERING) {
	    unregisterSerialService(event);
	} else {
	    final ServiceReference<?> reference = event.getServiceReference();
	    final Object service = FrameworkUtil.getBundle(getClass()).getBundleContext().getService(reference);
	    if (service instanceof Serial) {
		System.err.println(Messages.ScopeView_serial_message_missed);
	    }
	}
    }

    private void unregisterSerialService(ServiceEvent event) {
	final ServiceReference<?> reference = event.getServiceReference();
	final Object service = FrameworkUtil.getBundle(getClass()).getBundleContext().getService(reference);
	if (service instanceof Serial) {
	    if (service == this.mySerial) {
		this.mySerial.removeListener(this.myScopelistener);
		this.myScope.setStatus(Messages.ScopeView_disconnected_from + this.mySerial.toString());
		this.myScope.setShowLabels(true);
		this.myScope.setnewBackgroundImage();
		this.myScopelistener.dispose();
		this.myScopelistener = null;
		this.mySerial = null;
	    }
	}
    }

    private void registerSerialService(ServiceEvent event) {
	final ServiceReference<?> reference = event.getServiceReference();
	final Object service = FrameworkUtil.getBundle(getClass()).getBundleContext().getService(reference);
	if ((service instanceof Serial)) {
	    registerSerialService((Serial) service);
	}
    }

    /**
     * When the scope starts it needs to look is there are already serial services running. This method looks for the serial services and if found
     * this class is added as listener
     */
    private void registerExistingSerialService() {
	final ServiceReference<?> reference = Activator.context.getServiceReference(Serial.class.getName());
	if (reference != null) {
	    final Object service = FrameworkUtil.getBundle(getClass()).getBundleContext().getService(reference);
	    if (service instanceof Serial) {
		registerSerialService((Serial) service);
	    }
	}
    }

    /**
     * There we actually add the listener to the service.
     * 
     * @param service
     */
    private void registerSerialService(Serial service) {
	if ((this.myScopelistener == null) && (this.myScope != null)) {
	    this.myScopelistener = new ScopeListener(this.myScope);
	    this.mySerial = service;
	    PlatformUI.getWorkbench().getDisplay().syncExec(new Runnable() {
		@Override
		public void run() {
		    ScopeView.this.mySerial.addListener(ScopeView.this.myScopelistener);
		    ScopeView.this.myScope.setStatus(Messages.ScopeView_connected_to + ScopeView.this.mySerial.toString());
		    ScopeView.this.myScope.setShowLabels(true);
		    ScopeView.this.myScope.setnewBackgroundImage();
		}
	    });
	}
    }

    @Override
    public void dispose() {
	// As we have a listener we need to remove the listener
	if ((this.myScopelistener != null) && (this.mySerial != null)) {
	    Serial tempSerial = this.mySerial;
	    this.mySerial = null;
	    tempSerial.removeListener(this.myScopelistener);
	    this.myScopelistener.dispose();
	    this.myScopelistener = null;
	    this.myScope.dispose();
	    this.myScope = null;
	}
	super.dispose();
    }
}
