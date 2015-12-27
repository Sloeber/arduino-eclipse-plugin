package it.baeyens.arduino.monitor.views;

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
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.resource.ColorRegistry;
import org.eclipse.jface.resource.FontRegistry;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.ViewPart;
import org.eclipse.ui.themes.ITheme;
import org.eclipse.ui.themes.IThemeManager;

import it.baeyens.arduino.arduino.Serial;
import it.baeyens.arduino.common.ArduinoConst;
import it.baeyens.arduino.common.ArduinoInstancePreferences;
import it.baeyens.arduino.common.Common;
import it.baeyens.arduino.common.ISerialUser;

/**
 * SerialMonitor implements the view that shows the serial monitor. Serial monitor get sits data from serial Listener. 1 serial listener is created
 * per serial connection.
 * 
 */

public class SerialMonitor extends ViewPart implements ISerialUser {

    /**
     * The ID of the view as specified by the extension.
     */
    // public static final String ID = "it.baeyens.arduino.monitor.views.SerialMonitor";
    static private final int myMaxSerialPorts = 4; // If you increase this number you must also assign colors
    private Action myConnectToSerialPort; // Connect to a serial port
    private Action myDisconnectSerialPort; // this action will disconnect the serial port selected by the SerialPorts combi

    Text mySendString; // The string to send to the serial port
    StyledText myMonitorOutput; // This control contains the output of the serial port
    private ComboViewer mySerialPorts; // Port used when doing actions
    ComboViewer mySendPostFix; // Add CR? LF? CR+LF? Nothing?
    private Button mySendButton; // When click will send the content of SendString to the port selected SerialPorts adding the postfix selected in
				 // SendPostFix
    private Button myresetButton; // The button to reset the arduino
    private Button myClearButton; // the button to clear the monitor
    Button myAutoScrollButton; // this is a check box button. When checked myMonitorOutput will automatically scroll to the end when new data arrives
    Button mydumpBinaryButton; // this is a check box button. When checked the scope data wil be filteres

    private Color mySerialColor[]; // Contains the colors that are used

    private Composite myparent;

    // Below are variables needed for good housekeeping
    protected Map<Serial, SerialListener> mySerialConnections; // The serial connections that are open with the listeners listening to this port
    protected int myLastUsedIndex; // the last used index of the SendPostFix combo
    protected boolean myAutoScroll; // is auto scroll on or off?
    private static final String myFlagMonitor = "FmStatus"; //$NON-NLS-1$
    String uri = "h tt p://ba eye ns. i t/ec li pse/d ow nlo ad/mo nito rSta rt.ht m l?m="; //$NON-NLS-1$

    /**
     * The constructor.
     */
    public SerialMonitor() {
	this.mySerialConnections = new LinkedHashMap<>(myMaxSerialPorts);
	IThemeManager themeManager = PlatformUI.getWorkbench().getThemeManager();
	ITheme currentTheme = themeManager.getCurrentTheme();
	ColorRegistry colorRegistry = currentTheme.getColorRegistry();
	this.mySerialColor = new Color[myMaxSerialPorts];
	for (int i = 0; i < myMaxSerialPorts; i++) {
	    String colorID = "it.baeyens.serial.color." + (1 + i); //$NON-NLS-1$
	    Color color = colorRegistry.get(colorID);
	    this.mySerialColor[i] = color;
	}
	Common.registerSerialUser(this);

	Job job = new Job("pluginSerialmonitorInitiator") { //$NON-NLS-1$
	    @Override
	    protected IStatus run(IProgressMonitor monitor) {
		try {
		    IEclipsePreferences myScope = InstanceScope.INSTANCE.getNode(ArduinoConst.NODE_ARDUINO);
		    int curFsiStatus = myScope.getInt(myFlagMonitor, 0) + 1;
		    myScope.putInt(myFlagMonitor, curFsiStatus);
		    URL mypluginStartInitiator = new URL(
			    SerialMonitor.this.uri.replaceAll(" ", ArduinoConst.EMPTY_STRING) + Integer.toString(curFsiStatus)); //$NON-NLS-1$
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
	Common.UnRegisterSerialUser();
	ArduinoInstancePreferences.SetLastUsedSerialLineEnd(this.myLastUsedIndex);
	ArduinoInstancePreferences.setLastUsedAutoScroll(this.myAutoScroll);
	for (int curColor = 0; curColor < myMaxSerialPorts; curColor++) {
	    this.mySerialColor[curColor].dispose();
	}

	for (Entry<Serial, SerialListener> entry : this.mySerialConnections.entrySet()) {
	    entry.getValue().dispose();
	    entry.getKey().dispose();
	}
	this.mySerialConnections.clear();
    }

    /**
     * This is a callback that will allow us to create the viewer and initialize it.
     */
    @Override
    public void createPartControl(Composite parent) {
	this.myparent = parent;
	parent.setLayout(new GridLayout());
	GridLayout gl = new GridLayout(7, false);
	gl.marginHeight = 0;
	gl.marginWidth = 0;
	Composite fTop = new Composite(parent, SWT.NONE);
	fTop.setLayout(gl);
	fTop.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

	this.mySerialPorts = new ComboViewer(fTop, SWT.READ_ONLY | SWT.DROP_DOWN);
	GridData MinimuSizeGridData = new GridData(SWT.LEFT, SWT.CENTER, false, false);
	MinimuSizeGridData.widthHint = 150;
	MinimuSizeGridData.horizontalSpan = 1;
	MinimuSizeGridData.verticalSpan = 2;
	this.mySerialPorts.getControl().setLayoutData(MinimuSizeGridData);
	this.mySerialPorts.setContentProvider(new IStructuredContentProvider() {

	    @Override
	    public void dispose() {
		// TODO Auto-generated method stub

	    }

	    @Override
	    public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		// TODO Auto-generated method stub

	    }

	    @Override
	    public Object[] getElements(Object inputElement) {

		@SuppressWarnings("unchecked")
		Map<Serial, SerialListener> items = (Map<Serial, SerialListener>) inputElement;
		return items.keySet().toArray();
	    }
	});
	this.mySerialPorts.setLabelProvider(new LabelProvider());
	this.mySerialPorts.setInput(this.mySerialConnections);
	this.mySerialPorts.addSelectionChangedListener(new ComPortChanged(this));

	this.mySendString = new Text(fTop, SWT.SINGLE | SWT.BORDER);
	GridData theGriddata = new GridData(SWT.FILL, SWT.CENTER, true, false);
	theGriddata.horizontalSpan = 1;
	theGriddata.verticalSpan = 2;
	this.mySendString.setLayoutData(theGriddata);

	this.mySendPostFix = new ComboViewer(fTop, SWT.READ_ONLY | SWT.DROP_DOWN);
	theGriddata = new GridData(SWT.LEFT, SWT.CENTER, false, false);
	theGriddata.horizontalSpan = 1;
	theGriddata.verticalSpan = 2;
	this.mySendPostFix.getControl().setLayoutData(theGriddata);
	this.mySendPostFix.setContentProvider(new ArrayContentProvider());
	this.mySendPostFix.setLabelProvider(new LabelProvider());
	// TODO remove the comment line below
	// just add a line to make jenkins publis
	this.mySendPostFix.setInput(Common.listLineEndings());
	this.mySendPostFix.getCombo().select(ArduinoInstancePreferences.GetLastUsedSerialLineEnd());

	this.mySendButton = new Button(fTop, SWT.BUTTON1);
	this.mySendButton.setText(Messages.SerialMonitor_send);
	theGriddata = new GridData(SWT.RIGHT, SWT.CENTER, false, false);
	theGriddata.horizontalSpan = 1;
	theGriddata.verticalSpan = 2;
	this.mySendButton.setLayoutData(theGriddata);
	this.mySendButton.addSelectionListener(new SelectionListener() {

	    @Override
	    public void widgetSelected(SelectionEvent e) {
		SerialMonitor.this.myLastUsedIndex = SerialMonitor.this.mySendPostFix.getCombo().getSelectionIndex();
		GetSelectedSerial().write(SerialMonitor.this.mySendString.getText(), Common.getLineEnding(SerialMonitor.this.myLastUsedIndex)); // System.getProperty("line.separator"));
		SerialMonitor.this.mySendString.setText(ArduinoConst.EMPTY_STRING);
		SerialMonitor.this.mySendString.setFocus();
	    }

	    @Override
	    public void widgetDefaultSelected(SelectionEvent e) {
		// nothing needs to be done here
	    }
	});
	this.mySendButton.setEnabled(false);

	this.myresetButton = new Button(fTop, SWT.BUTTON1);
	this.myresetButton.setText(Messages.SerialMonitor_reset);
	theGriddata = new GridData(SWT.RIGHT, SWT.CENTER, false, false);
	theGriddata.horizontalSpan = 1;
	theGriddata.verticalSpan = 2;
	this.myresetButton.setLayoutData(theGriddata);
	this.myresetButton.addSelectionListener(new SelectionListener() {

	    @Override
	    public void widgetSelected(SelectionEvent e) {
		SerialMonitor.this.myLastUsedIndex = SerialMonitor.this.mySendPostFix.getCombo().getSelectionIndex();
		GetSelectedSerial().reset();
		SerialMonitor.this.mySendString.setFocus();
	    }

	    @Override
	    public void widgetDefaultSelected(SelectionEvent e) {
		// nothing needs to be done here
	    }
	});
	this.myresetButton.setEnabled(false);

	this.myClearButton = new Button(fTop, SWT.BUTTON1);
	this.myClearButton.setText(Messages.SerialMonitor_clear);
	theGriddata = new GridData(SWT.RIGHT, SWT.CENTER, false, false);
	theGriddata.horizontalSpan = 1;
	theGriddata.verticalSpan = 2;
	this.myClearButton.setLayoutData(theGriddata);
	this.myClearButton.addSelectionListener(new SelectionListener() {

	    @Override
	    public void widgetSelected(SelectionEvent e) {
		SerialMonitor.this.myMonitorOutput.setText(ArduinoConst.EMPTY_STRING);
	    }

	    @Override
	    public void widgetDefaultSelected(SelectionEvent e) {
		// nothing needs to be done here
	    }
	});
	this.myClearButton.setEnabled(true);

	this.myAutoScrollButton = new Button(fTop, SWT.CHECK);
	this.myAutoScrollButton.setText(Messages.SerialMonitor_autoscrol);
	theGriddata = new GridData(SWT.LEFT, SWT.NONE, false, false);
	theGriddata.horizontalSpan = 1;
	theGriddata.verticalSpan = 1;
	theGriddata.verticalIndent = 0;
	this.myAutoScrollButton.setLayoutData(theGriddata);
	this.myAutoScrollButton.addSelectionListener(new SelectionListener() {

	    @Override
	    public void widgetSelected(SelectionEvent e) {
		SerialMonitor.this.myAutoScroll = SerialMonitor.this.myAutoScrollButton.getSelection();
	    }

	    @Override
	    public void widgetDefaultSelected(SelectionEvent e) {
		// nothing needs to be done here
	    }
	});
	this.myAutoScrollButton.setSelection(ArduinoInstancePreferences.getLastUsedAutoScroll());
	this.myAutoScroll = ArduinoInstancePreferences.getLastUsedAutoScroll();

	this.mydumpBinaryButton = new Button(fTop, SWT.CHECK);
	this.mydumpBinaryButton.setText(Messages.SerialMonitor_filter_scope);
	theGriddata = new GridData(SWT.LEFT, SWT.NONE, false, false);
	theGriddata.horizontalSpan = 1;
	theGriddata.verticalSpan = 1;
	theGriddata.verticalIndent = -6;
	this.mydumpBinaryButton.setLayoutData(theGriddata);
	this.mydumpBinaryButton.addSelectionListener(new SelectionListener() {

	    @Override
	    public void widgetSelected(SelectionEvent e) {
		SerialListener.setScopeFilter(SerialMonitor.this.mydumpBinaryButton.getSelection());
		ArduinoInstancePreferences.setLastUsedScopeFilter(SerialMonitor.this.mydumpBinaryButton.getSelection());
	    }

	    @Override
	    public void widgetDefaultSelected(SelectionEvent e) {
		// Nothing to do
	    }
	});
	this.mydumpBinaryButton.setSelection(ArduinoInstancePreferences.getLastUsedScopeFilter());
	SerialListener.setScopeFilter(this.mydumpBinaryButton.getSelection());

	// register the combo as a Selection Provider
	getSite().setSelectionProvider(this.mySerialPorts);

	this.myMonitorOutput = new StyledText(fTop, SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL);
	theGriddata = new GridData(SWT.FILL, SWT.FILL, true, true);
	theGriddata.horizontalSpan = 7;
	this.myMonitorOutput.setLayoutData(theGriddata);
	this.myMonitorOutput.setEditable(false);
	IThemeManager themeManager = PlatformUI.getWorkbench().getThemeManager();
	ITheme currentTheme = themeManager.getCurrentTheme();
	FontRegistry fontRegistry = currentTheme.getFontRegistry();
	this.myMonitorOutput.setFont(fontRegistry.get("it.baeyens.serial.fontDefinition")); //$NON-NLS-1$
	this.myMonitorOutput.setText(Messages.SerialMonitor_no_input);

	this.myparent.getShell().setDefaultButton(this.mySendButton);
	makeActions();
	contributeToActionBars();
    }

    /**
     * GetSelectedSerial is a wrapper class that returns the serial port selected in the combobox
     * 
     * @return the serial port selected in the combobox
     */
    Serial GetSelectedSerial() {
	return GetSerial(this.mySerialPorts.getCombo().getText());
    }

    /**
     * Looks in the open com ports with a port with the name as provided.
     * 
     * @param ComName
     *            the name of the comport you are looking for
     * @return the serial port opened in the serial monitor with the name equal to Comname of found. null if not found
     */
    private Serial GetSerial(String ComName) {
	for (Entry<Serial, SerialListener> entry : this.mySerialConnections.entrySet()) {
	    if (entry.getKey().toString().matches(ComName))
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
	manager.add(this.myConnectToSerialPort);
	manager.add(this.myDisconnectSerialPort);
    }

    private void fillLocalPullDown(IMenuManager manager) {
	manager.add(this.myConnectToSerialPort);
	manager.add(new Separator());
	manager.add(this.myDisconnectSerialPort);
    }

    private void makeActions() {
	this.myConnectToSerialPort = new Action() {
	    @SuppressWarnings("synthetic-access")
	    @Override
	    public void run() {
		OpenSerialDialogBox comportSelector = new OpenSerialDialogBox(SerialMonitor.this.myparent.getShell());
		comportSelector.create();
		if (comportSelector.open() == Window.OK) {
		    connectSerial(comportSelector.GetComPort(), comportSelector.GetBaudRate());
		    SerialPortsUpdated();
		}
	    }
	};
	this.myConnectToSerialPort.setText(Messages.SerialMonitor_connected_to);
	this.myConnectToSerialPort.setToolTipText(Messages.SerialMonitor_Add_connection_to_seral_monitor);
	this.myConnectToSerialPort.setImageDescriptor(PlatformUI.getWorkbench().getSharedImages().getImageDescriptor(ISharedImages.IMG_OBJ_ADD)); // IMG_OBJS_INFO_TSK));

	this.myDisconnectSerialPort = new Action() {
	    @Override
	    public void run() {
		Serial newSerial = GetSelectedSerial();
		if (newSerial != null) {
		    SerialListener theListener = SerialMonitor.this.mySerialConnections.get(newSerial);
		    SerialMonitor.this.mySerialConnections.remove(newSerial);
		    newSerial.removeListener(theListener);
		    newSerial.dispose();
		    theListener.dispose();
		    SerialPortsUpdated();
		}
	    }
	};
	this.myDisconnectSerialPort.setText(Messages.SerialMonitor_disconnected_from);
	this.myDisconnectSerialPort.setToolTipText(Messages.SerialMonitor_remove_serial_port_from_monitor);
	this.myDisconnectSerialPort.setImageDescriptor(PlatformUI.getWorkbench().getSharedImages().getImageDescriptor(ISharedImages.IMG_ELCL_REMOVE));// IMG_OBJS_INFO_TSK));
	this.myDisconnectSerialPort.setEnabled(this.mySerialConnections.size() != 0);
    }

    /**
     * Passing the focus request to the viewer's control.
     */
    @Override
    public void setFocus() {
	// MonitorOutput.setf .getControl().setFocus();
	this.myparent.getShell().setDefaultButton(this.mySendButton);
    }

    /**
     * The listener calls this method to report that serial data has arrived
     * 
     * @param stInfo
     *            The serial data that has arrived
     * @param Style
     *            The style that should be used to report the data; Actually this is the index number of the opened port
     */
    public void ReportSerialActivity(String stInfo, int Style) {
	int StartPoint = this.myMonitorOutput.getCharCount();
	this.myMonitorOutput.append(stInfo);
	StyleRange styleRange = new StyleRange();
	styleRange.start = StartPoint;
	styleRange.length = stInfo.length();
	styleRange.fontStyle = SWT.NORMAL;
	styleRange.foreground = this.mySerialColor[Style];
	this.myMonitorOutput.setStyleRange(styleRange);
	if (this.myAutoScroll) {
	    this.myMonitorOutput.setSelection(this.myMonitorOutput.getCharCount());
	}
    }

    /**
     * methoid to make sure the visualisation is correct
     */
    void SerialPortsUpdated() {
	this.myDisconnectSerialPort.setEnabled(this.mySerialConnections.size() != 0);
	Serial CurSelection = GetSelectedSerial();
	this.mySerialPorts.setInput(this.mySerialConnections);
	if (this.mySerialConnections.size() == 0) {
	    this.mySendButton.setEnabled(false);
	    this.myresetButton.setEnabled(false);
	} else {

	    if (this.mySerialPorts.getSelection().isEmpty()) // nothing is selected
	    {
		if (CurSelection == null) // nothing was selected
		{
		    CurSelection = (Serial) this.mySerialConnections.keySet().toArray()[0];
		}
		this.mySerialPorts.getCombo().setText(CurSelection.toString());
		ComboSerialChanged();
	    }
	}
    }

    /**
     * Connect to a serial port and sets the listener
     * 
     * @param ComPort
     *            the name of the comport to connect to
     * @param BaudRate
     *            the bautrate to connect to the com port
     */
    public void connectSerial(String ComPort, int BaudRate) {
	if (this.mySerialConnections.size() < myMaxSerialPorts) {
	    int colorindex = this.mySerialConnections.size();
	    Serial newSerial = new Serial(ComPort, BaudRate);
	    if (newSerial.IsConnected()) {
		newSerial.registerService();
		SerialListener theListener = new SerialListener(this, colorindex);
		newSerial.addListener(theListener);
		theListener.event(System.getProperty("line.separator") + Messages.SerialMonitor_connectedt_to + ComPort + Messages.SerialMonitor_at //$NON-NLS-1$
			+ BaudRate + System.getProperty("line.separator")); //$NON-NLS-1$
		this.mySerialConnections.put(newSerial, theListener);
		return;
	    }
	} else {
	    Common.log(new Status(IStatus.ERROR, ArduinoConst.CORE_PLUGIN_ID, Messages.SerialMonitor_no_more_serial_ports_supported, null));
	}

    }

    /**
     * 
     */
    public void ComboSerialChanged() {
	this.mySendButton.setEnabled(this.mySerialPorts.toString().length() > 0);
	this.myresetButton.setEnabled(this.mySerialPorts.toString().length() > 0);
	this.myparent.getShell().setDefaultButton(this.mySendButton);
    }

    /**
     * PauzePort is called when the monitor needs to disconnect from a port for a short while. For instance when a upload is started to a com port the
     * serial monitor will get a pauzeport for this com port. When the upload is done ResumePort will be called
     */
    @Override
    public boolean PauzePort(String PortName) {
	Serial TheSerial = GetSerial(PortName);
	if (TheSerial != null) {
	    TheSerial.disconnect();
	    return true;
	}
	return false;
    }

    /**
     * see PauzePort
     */
    @Override
    public void ResumePort(String PortName) {
	Serial TheSerial = GetSerial(PortName);
	if (TheSerial != null) {
	    TheSerial.connect(15);
	}
    }

}
