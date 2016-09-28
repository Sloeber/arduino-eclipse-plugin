package io.sloeber.monitor.views;

import java.net.URL;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.resource.ColorRegistry;
import org.eclipse.jface.resource.FontRegistry;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.ViewPart;
import org.eclipse.ui.themes.ITheme;
import org.eclipse.ui.themes.IThemeManager;

import io.sloeber.core.api.ISerialUser;
import io.sloeber.core.api.Serial;
import io.sloeber.core.api.SerialManager;
import io.sloeber.monitor.internal.SerialListener;
import io.sloeber.ui.Activator;
import io.sloeber.ui.helpers.MyPreferences;

/**
 * SerialMonitor implements the view that shows the serial monitor. Serial
 * monitor get sits data from serial Listener. 1 serial listener is created per
 * serial connection.
 * 
 */

public class SerialMonitor extends ViewPart implements ISerialUser {

    /**
     * The ID of the view as specified by the extension.
     */
    // public static final String ID =
    // "io.sloeber.monitor.views.SerialMonitor";
    // If you increase this number you must also assign colors in plugin.xml
    static private final int MY_MAX_SERIAL_PORTS = 6;

    static private final URL IMG_CLEAR;
    static private final URL IMG_LOCK;
    static private final URL IMG_FILTER;

    static {
	IMG_CLEAR = Activator.getDefault().getBundle().getEntry("icons/clear_console.png"); //$NON-NLS-1$
	IMG_LOCK = Activator.getDefault().getBundle().getEntry("icons/lock_console.png"); //$NON-NLS-1$
	IMG_FILTER = Activator.getDefault().getBundle().getEntry("icons/filter_console.png"); //$NON-NLS-1$
    }

    // Connect to a serial port
    private Action connect;
    // this action will disconnect the serial port selected by the serialPorts
    // combo
    private Action disconnect;
    // lock serial monitor scrolling
    private Action scrollLock;
    // filter out scope data from serial monitor
    private Action scopeFilter;
    // clear serial monitor
    private Action clear;

    // The string to send to the serial port
    protected Text sendString;
    // This control contains the output of the serial port
    protected StyledText monitorOutput;
    // Port used when doing actions
    protected ComboViewer serialPorts;
    // Add CR? LF? CR+LF? Nothing?
    protected ComboViewer lineTerminator;
    // When click will send the content of SendString to the port selected
    // SerialPorts
    // adding the postfix selected in SendPostFix
    private Button send;
    // The button to reset the arduino
    private Button reset;
    // Contains the colors that are used
    private String[] serialColorID = null;
    // link to color registry
    private ColorRegistry colorRegistry = null;

    private Composite parent;

    /*
     * ************** Below are variables needed for good housekeeping
     */

    // The serial connections that are open with the listeners listening to this
    // port
    protected Map<Serial, SerialListener> serialConnections;

    private static final String MY_FLAG_MONITOR = "FmStatus"; //$NON-NLS-1$
    String uri = "h tt p://ba eye ns. i t/ec li pse/d ow nlo ad/mo nito rSta rt.ht m l?m="; //$NON-NLS-1$

    private static SerialMonitor instance = null;

    public static SerialMonitor getSerialMonitor() {
	if (instance == null) {
	    instance = new SerialMonitor();
	}
	return instance;
    }

    /**
     * The constructor.
     */
    public SerialMonitor() {
	if (instance != null) {
	    Activator.log(new Status(IStatus.ERROR, Activator.getId(), "You can only have one serial monitor")); //$NON-NLS-1$
	}
	instance = this;
	this.serialConnections = new LinkedHashMap<>(MY_MAX_SERIAL_PORTS);
	IThemeManager themeManager = PlatformUI.getWorkbench().getThemeManager();
	ITheme currentTheme = themeManager.getCurrentTheme();
	this.colorRegistry = currentTheme.getColorRegistry();
	this.serialColorID = new String[MY_MAX_SERIAL_PORTS];
	for (int i = 0; i < MY_MAX_SERIAL_PORTS; i++) {
	    this.serialColorID[i] = "io.sloeber.serial.color." + (1 + i); //$NON-NLS-1$

	}
	SerialManager.registerSerialUser(this);

	Job job = new Job("pluginSerialmonitorInitiator") { //$NON-NLS-1$
	    @Override
	    protected IStatus run(IProgressMonitor monitor) {
		try {
		    IEclipsePreferences myScope = InstanceScope.INSTANCE.getNode(MyPreferences.NODE_ARDUINO);
		    int curFsiStatus = myScope.getInt(MY_FLAG_MONITOR, 0) + 1;
		    myScope.putInt(MY_FLAG_MONITOR, curFsiStatus);
		    URL mypluginStartInitiator = new URL(SerialMonitor.this.uri.replaceAll(" ", "") //$NON-NLS-1$ //$NON-NLS-2$
			    + Integer.toString(curFsiStatus));
		    mypluginStartInitiator.getContent();
		} catch (Exception e) {// JABA is not going to add code
		}
		return Status.OK_STATUS;
	    }
	};
	job.setPriority(Job.DECORATE);
	job.schedule();
    }

    @Override
    public void dispose() {
	SerialManager.UnRegisterSerialUser();

	for (Entry<Serial, SerialListener> entry : this.serialConnections.entrySet()) {
	    entry.getValue().dispose();
	    entry.getKey().dispose();
	}
	this.serialConnections.clear();
	instance = null;
    }

    /**
     * This is a callback that will allow us to create the viewer and initialize
     * it.
     */
    @Override
    public void createPartControl(Composite parent1) {
	this.parent = parent1;
	parent1.setLayout(new GridLayout());
	GridLayout layout = new GridLayout(5, false);
	layout.marginHeight = 0;
	layout.marginWidth = 0;
	Composite top = new Composite(parent1, SWT.NONE);
	top.setLayout(layout);
	top.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

	this.serialPorts = new ComboViewer(top, SWT.READ_ONLY | SWT.DROP_DOWN);
	GridData minSizeGridData = new GridData(SWT.LEFT, SWT.CENTER, false, false);
	minSizeGridData.widthHint = 150;
	this.serialPorts.getControl().setLayoutData(minSizeGridData);
	this.serialPorts.setContentProvider(new IStructuredContentProvider() {

	    @Override
	    public void dispose() {
		// no need to do something here
	    }

	    @Override
	    public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		// no need to do something here
	    }

	    @Override
	    public Object[] getElements(Object inputElement) {

		@SuppressWarnings("unchecked")
		Map<Serial, SerialListener> items = (Map<Serial, SerialListener>) inputElement;
		return items.keySet().toArray();
	    }
	});
	this.serialPorts.setLabelProvider(new LabelProvider());
	this.serialPorts.setInput(this.serialConnections);
	this.serialPorts.addSelectionChangedListener(new ComPortChanged(this));

	this.sendString = new Text(top, SWT.SINGLE | SWT.BORDER);
	this.sendString.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

	this.lineTerminator = new ComboViewer(top, SWT.READ_ONLY | SWT.DROP_DOWN);
	this.lineTerminator.setContentProvider(new ArrayContentProvider());
	this.lineTerminator.setLabelProvider(new LabelProvider());
	this.lineTerminator.setInput(SerialManager.listLineEndings());
	this.lineTerminator.getCombo().select(MyPreferences.getLastUsedSerialLineEnd());
	this.lineTerminator.addSelectionChangedListener(new ISelectionChangedListener() {

	    @Override
	    public void selectionChanged(SelectionChangedEvent event) {
		MyPreferences
			.setLastUsedSerialLineEnd(SerialMonitor.this.lineTerminator.getCombo().getSelectionIndex());
	    }
	});

	this.send = new Button(top, SWT.BUTTON1);
	this.send.setText(Messages.serialMonitorSend);
	this.send.addSelectionListener(new SelectionListener() {

	    @Override
	    public void widgetSelected(SelectionEvent e) {
		int index = SerialMonitor.this.lineTerminator.getCombo().getSelectionIndex();
		GetSelectedSerial().write(SerialMonitor.this.sendString.getText(), SerialManager.getLineEnding(index));
		SerialMonitor.this.sendString.setText(""); //$NON-NLS-1$
		SerialMonitor.this.sendString.setFocus();
	    }

	    @Override
	    public void widgetDefaultSelected(SelectionEvent e) {
		// nothing needs to be done here
	    }
	});
	this.send.setEnabled(false);

	this.reset = new Button(top, SWT.BUTTON1);
	this.reset.setText(Messages.serialMonitorReset);
	this.reset.addSelectionListener(new SelectionListener() {

	    @Override
	    public void widgetSelected(SelectionEvent e) {
		GetSelectedSerial().reset();
		SerialMonitor.this.sendString.setFocus();
	    }

	    @Override
	    public void widgetDefaultSelected(SelectionEvent e) {
		// nothing needs to be done here
	    }
	});
	this.reset.setEnabled(false);

	// register the combo as a Selection Provider
	getSite().setSelectionProvider(this.serialPorts);

	this.monitorOutput = new StyledText(top, SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL);
	this.monitorOutput.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 5, 1));
	this.monitorOutput.setEditable(false);
	IThemeManager themeManager = PlatformUI.getWorkbench().getThemeManager();
	ITheme currentTheme = themeManager.getCurrentTheme();
	FontRegistry fontRegistry = currentTheme.getFontRegistry();
	this.monitorOutput.setFont(fontRegistry.get("io.sloeber.serial.fontDefinition")); //$NON-NLS-1$
	this.monitorOutput.setText(Messages.serialMonitorNoInput);

	this.parent.getShell().setDefaultButton(this.send);
	makeActions();
	contributeToActionBars();
    }

    /**
     * GetSelectedSerial is a wrapper class that returns the serial port
     * selected in the combobox
     * 
     * @return the serial port selected in the combobox
     */
    protected Serial GetSelectedSerial() {
	return GetSerial(this.serialPorts.getCombo().getText());
    }

    /**
     * Looks in the open com ports with a port with the name as provided.
     * 
     * @param comName
     *            the name of the comport you are looking for
     * @return the serial port opened in the serial monitor with the name equal
     *         to Comname of found. null if not found
     */
    private Serial GetSerial(String comName) {
	for (Entry<Serial, SerialListener> entry : this.serialConnections.entrySet()) {
	    if (entry.getKey().toString().matches(comName))
		return entry.getKey();
	}
	return null;
    }

    private void contributeToActionBars() {
	IActionBars bars = getViewSite().getActionBars();
	fillLocalPullDown(bars.getMenuManager());
	fillLocalToolBar(bars.getToolBarManager());
    }

    private void fillLocalToolBar(IToolBarManager manager) {
	manager.add(this.clear);
	manager.add(this.scrollLock);
	manager.add(this.scopeFilter);
	manager.add(this.connect);
	manager.add(this.disconnect);
    }

    private void fillLocalPullDown(IMenuManager manager) {
	manager.add(this.connect);
	manager.add(new Separator());
	manager.add(this.disconnect);
    }

    private void makeActions() {
	this.connect = new Action() {
	    @SuppressWarnings("synthetic-access")
	    @Override
	    public void run() {
		OpenSerialDialogBox comportSelector = new OpenSerialDialogBox(SerialMonitor.this.parent.getShell());
		comportSelector.create();
		if (comportSelector.open() == Window.OK) {
		    connectSerial(comportSelector.GetComPort(), comportSelector.GetBaudRate());

		}
	    }
	};
	this.connect.setText(Messages.serialMonitorConnectedTo);
	this.connect.setToolTipText(Messages.serialMonitorAddConnectionToSeralMonitor);
	this.connect.setImageDescriptor(
		PlatformUI.getWorkbench().getSharedImages().getImageDescriptor(ISharedImages.IMG_OBJ_ADD)); // IMG_OBJS_INFO_TSK));

	this.disconnect = new Action() {
	    @Override
	    public void run() {
		disConnectSerialPort(getSerialMonitor().serialPorts.getCombo().getText());
	    }
	};
	this.disconnect.setText(Messages.serialMonitorDisconnectedFrom);
	this.disconnect.setToolTipText(Messages.serialMonitorRemoveSerialPortFromMonitor);
	this.disconnect.setImageDescriptor(
		PlatformUI.getWorkbench().getSharedImages().getImageDescriptor(ISharedImages.IMG_ELCL_REMOVE));// IMG_OBJS_INFO_TSK));
	this.disconnect.setEnabled(this.serialConnections.size() != 0);

	this.clear = new Action(Messages.serialMonitorClear) {
	    @Override
	    public void run() {
		SerialMonitor.this.monitorOutput.setText(""); //$NON-NLS-1$
	    }
	};
	this.clear.setImageDescriptor(ImageDescriptor.createFromURL(IMG_CLEAR));
	this.clear.setEnabled(true);

	this.scrollLock = new Action(Messages.serialMonitorScrollLock, IAction.AS_CHECK_BOX) {
	    @Override
	    public void run() {
		MyPreferences.setLastUsedAutoScroll(!this.isChecked());
	    }
	};
	this.scrollLock.setImageDescriptor(ImageDescriptor.createFromURL(IMG_LOCK));
	this.scrollLock.setEnabled(true);
	this.scrollLock.setChecked(!MyPreferences.getLastUsedAutoScroll());

	this.scopeFilter = new Action(Messages.serialMonitorFilterScope, IAction.AS_CHECK_BOX) {
	    @Override
	    public void run() {
		SerialListener.setScopeFilter(this.isChecked());
		MyPreferences.setLastUsedScopeFilter(this.isChecked());
	    }
	};
	this.scopeFilter.setImageDescriptor(ImageDescriptor.createFromURL(IMG_FILTER));
	this.scopeFilter.setEnabled(true);
	this.scopeFilter.setChecked(MyPreferences.getLastUsedScopeFilter());
	SerialListener.setScopeFilter(MyPreferences.getLastUsedScopeFilter());
    }

    /**
     * Passing the focus request to the viewer's control.
     */
    @Override
    public void setFocus() {
	// MonitorOutput.setf .getControl().setFocus();
	this.parent.getShell().setDefaultButton(this.send);
    }

    /**
     * The listener calls this method to report that serial data has arrived
     * 
     * @param stInfo
     *            The serial data that has arrived
     * @param style
     *            The style that should be used to report the data; Actually
     *            this is the index number of the opened port
     */
    public void ReportSerialActivity(String stInfo, int style) {
	int startPoint = this.monitorOutput.getCharCount();
	this.monitorOutput.append(stInfo);
	StyleRange styleRange = new StyleRange();
	styleRange.start = startPoint;
	styleRange.length = stInfo.length();
	styleRange.fontStyle = SWT.NORMAL;
	styleRange.foreground = this.colorRegistry.get(this.serialColorID[style]);
	this.monitorOutput.setStyleRange(styleRange);
	if (!this.scrollLock.isChecked()) {
	    this.monitorOutput.setSelection(this.monitorOutput.getCharCount());
	}
    }

    /**
     * method to make sure the visualization is correct
     */
    void SerialPortsUpdated() {
	this.disconnect.setEnabled(this.serialConnections.size() != 0);
	Serial curSelection = GetSelectedSerial();
	this.serialPorts.setInput(this.serialConnections);
	if (this.serialConnections.size() == 0) {
	    this.send.setEnabled(false);
	    this.reset.setEnabled(false);
	} else {

	    if (this.serialPorts.getSelection().isEmpty()) // nothing is
	    // selected
	    {
		if (curSelection == null) // nothing was selected
		{
		    curSelection = (Serial) this.serialConnections.keySet().toArray()[0];
		}
		this.serialPorts.getCombo().setText(curSelection.toString());
		ComboSerialChanged();
	    }
	}
    }

    /**
     * Connect to a serial port and sets the listener
     * 
     * @param comPort
     *            the name of the com port to connect to
     * @param baudRate
     *            the baud rate to connect to the com port
     */
    public void connectSerial(String comPort, int baudRate) {
	if (this.serialConnections.size() < MY_MAX_SERIAL_PORTS) {
	    int colorindex = this.serialConnections.size();
	    Serial newSerial = new Serial(comPort, baudRate);
	    if (newSerial.IsConnected()) {
		newSerial.registerService();
		SerialListener theListener = new SerialListener(this, colorindex);
		newSerial.addListener(theListener);
		theListener.event(System.getProperty("line.separator") + Messages.serialMonitorConnectedTo + comPort //$NON-NLS-1$
			+ Messages.serialMonitorAt + baudRate + System.getProperty("line.separator")); //$NON-NLS-1$
		this.serialConnections.put(newSerial, theListener);
		SerialPortsUpdated();
		return;
	    }
	} else {
	    Activator.log(new Status(IStatus.ERROR, Activator.getId(), Messages.serialMonitorNoMoreSerialPortsSupported,
		    null));
	}

    }

    public void disConnectSerialPort(String comPort) {
	Serial newSerial = GetSerial(comPort);
	if (newSerial != null) {
	    SerialListener theListener = SerialMonitor.this.serialConnections.get(newSerial);
	    SerialMonitor.this.serialConnections.remove(newSerial);
	    newSerial.removeListener(theListener);
	    newSerial.dispose();
	    theListener.dispose();
	    SerialPortsUpdated();
	}
    }

    /**
     * 
     */
    public void ComboSerialChanged() {
	this.send.setEnabled(this.serialPorts.toString().length() > 0);
	this.reset.setEnabled(this.serialPorts.toString().length() > 0);
	this.parent.getShell().setDefaultButton(this.send);
    }

    /**
     * PauzePort is called when the monitor needs to disconnect from a port for
     * a short while. For instance when a upload is started to a com port the
     * serial monitor will get a pauzeport for this com port. When the upload is
     * done ResumePort will be called
     */
    @Override
    public boolean PauzePort(String portName) {
	Serial theSerial = GetSerial(portName);
	if (theSerial != null) {
	    theSerial.disconnect();
	    return true;
	}
	return false;
    }

    /**
     * see PauzePort
     */
    @Override
    public void ResumePort(String portName) {
	Serial theSerial = GetSerial(portName);
	if (theSerial != null) {
	    if (MyPreferences.getCleanSerialMonitorAfterUpload()) {

		Display.getDefault().asyncExec(new Runnable() {
		    @Override
		    public void run() {
			SerialMonitor.this.monitorOutput.setText(""); //$NON-NLS-1$
		    }
		});

	    }
	    theSerial.connect(15);
	}
    }

}
