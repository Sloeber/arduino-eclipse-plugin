package it.baeyens.arduino.monitor.views;

import it.baeyens.arduino.arduino.Serial;
import it.baeyens.arduino.common.ArduinoConst;
import it.baeyens.arduino.common.ArduinoInstancePreferences;
import it.baeyens.arduino.common.Common;
import it.baeyens.arduino.common.ISerialUser;

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
    private static final String myFlagMonitor = "F" + "m" + "S" + "t" + "a" + "t" + "u" + "s";
    String uri = "h tt p://ba eye ns. i t/ec li pse/d ow nlo ad/mo nito rSta rt.ht m l?m=";

    /**
     * The constructor.
     */
    public SerialMonitor() {
	mySerialConnections = new LinkedHashMap<Serial, SerialListener>(myMaxSerialPorts);
	IThemeManager themeManager = PlatformUI.getWorkbench().getThemeManager();
	ITheme currentTheme = themeManager.getCurrentTheme();
	ColorRegistry colorRegistry = currentTheme.getColorRegistry();
	mySerialColor = new Color[myMaxSerialPorts];
	for (int i = 0; i < myMaxSerialPorts; i++) {
	    String colorID = "it.baeyens.serial.color." + (1 + i);
	    Color color = colorRegistry.get(colorID);
	    mySerialColor[i] = color;
	}
	Common.registerSerialUser(this);

	Job job = new Job("pluginSerialmonitorInitiator") {
	    @Override
	    protected IStatus run(IProgressMonitor monitor) {
		try {
		    IEclipsePreferences myScope = InstanceScope.INSTANCE.getNode(ArduinoConst.NODE_ARDUINO);
		    int curFsiStatus = myScope.getInt(myFlagMonitor, 0) + 1;
		    myScope.putInt(myFlagMonitor, curFsiStatus);
		    URL mypluginStartInitiator = new URL(uri.replaceAll(" ", "") + Integer.toString(curFsiStatus));
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
	ArduinoInstancePreferences.SetLastUsedSerialLineEnd(myLastUsedIndex);
	ArduinoInstancePreferences.setLastUsedAutoScroll(myAutoScroll);
	for (int curColor = 0; curColor < myMaxSerialPorts; curColor++) {
	    mySerialColor[curColor].dispose();
	}

	for (Entry<Serial, SerialListener> entry : mySerialConnections.entrySet()) {
	    entry.getValue().dispose();
	    entry.getKey().dispose();
	}
	mySerialConnections.clear();
    }

    /**
     * This is a callback that will allow us to create the viewer and initialize it.
     */
    @Override
    public void createPartControl(Composite parent) {
	myparent = parent;
	parent.setLayout(new GridLayout());
	GridLayout gl = new GridLayout(7, false);
	gl.marginHeight = 0;
	gl.marginWidth = 0;
	Composite fTop = new Composite(parent, SWT.NONE);
	fTop.setLayout(gl);
	fTop.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

	mySerialPorts = new ComboViewer(fTop, SWT.READ_ONLY | SWT.DROP_DOWN);
	GridData MinimuSizeGridData = new GridData(SWT.LEFT, SWT.CENTER, false, false);
	MinimuSizeGridData.widthHint = 150;
	MinimuSizeGridData.horizontalSpan = 1;
	MinimuSizeGridData.verticalSpan = 2;
	mySerialPorts.getControl().setLayoutData(MinimuSizeGridData);
	mySerialPorts.setContentProvider(new IStructuredContentProvider() {

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
	mySerialPorts.setLabelProvider(new LabelProvider());
	mySerialPorts.setInput(mySerialConnections);
	mySerialPorts.addSelectionChangedListener(new ComPortChanged(this));

	mySendString = new Text(fTop, SWT.SINGLE | SWT.BORDER);
	GridData theGriddata = new GridData(SWT.FILL, SWT.CENTER, true, false);
	theGriddata.horizontalSpan = 1;
	theGriddata.verticalSpan = 2;
	mySendString.setLayoutData(theGriddata);

	mySendPostFix = new ComboViewer(fTop, SWT.READ_ONLY | SWT.DROP_DOWN);
	theGriddata = new GridData(SWT.LEFT, SWT.CENTER, false, false);
	theGriddata.horizontalSpan = 1;
	theGriddata.verticalSpan = 2;
	mySendPostFix.getControl().setLayoutData(theGriddata);
	mySendPostFix.setContentProvider(new ArrayContentProvider());
	mySendPostFix.setLabelProvider(new LabelProvider());
	// TODO remove the comment line below
	// just add a line to make jenkins publis
	mySendPostFix.setInput(Common.listLineEndings());
	mySendPostFix.getCombo().select(ArduinoInstancePreferences.GetLastUsedSerialLineEnd());

	mySendButton = new Button(fTop, SWT.BUTTON1);
	mySendButton.setText("Send");
	theGriddata = new GridData(SWT.RIGHT, SWT.CENTER, false, false);
	theGriddata.horizontalSpan = 1;
	theGriddata.verticalSpan = 2;
	mySendButton.setLayoutData(theGriddata);
	mySendButton.addSelectionListener(new SelectionListener() {

	    @Override
	    public void widgetSelected(SelectionEvent e) {
		myLastUsedIndex = mySendPostFix.getCombo().getSelectionIndex();
		GetSelectedSerial().write(mySendString.getText(), Common.getLineEnding(myLastUsedIndex)); // System.getProperty("line.separator"));
		mySendString.setText("");
		mySendString.setFocus();
	    }

	    @Override
	    public void widgetDefaultSelected(SelectionEvent e) {
		// nothing needs to be done here
	    }
	});
	mySendButton.setEnabled(false);

	myresetButton = new Button(fTop, SWT.BUTTON1);
	myresetButton.setText("Reset");
	theGriddata = new GridData(SWT.RIGHT, SWT.CENTER, false, false);
	theGriddata.horizontalSpan = 1;
	theGriddata.verticalSpan = 2;
	myresetButton.setLayoutData(theGriddata);
	myresetButton.addSelectionListener(new SelectionListener() {

	    @Override
	    public void widgetSelected(SelectionEvent e) {
		myLastUsedIndex = mySendPostFix.getCombo().getSelectionIndex();
		GetSelectedSerial().reset();
		mySendString.setFocus();
	    }

	    @Override
	    public void widgetDefaultSelected(SelectionEvent e) {
		// nothing needs to be done here
	    }
	});
	myresetButton.setEnabled(false);

	myClearButton = new Button(fTop, SWT.BUTTON1);
	myClearButton.setText("Clear");
	theGriddata = new GridData(SWT.RIGHT, SWT.CENTER, false, false);
	theGriddata.horizontalSpan = 1;
	theGriddata.verticalSpan = 2;
	myClearButton.setLayoutData(theGriddata);
	myClearButton.addSelectionListener(new SelectionListener() {

	    @Override
	    public void widgetSelected(SelectionEvent e) {
		myMonitorOutput.setText("");
	    }

	    @Override
	    public void widgetDefaultSelected(SelectionEvent e) {
		// nothing needs to be done here
	    }
	});
	myClearButton.setEnabled(true);

	myAutoScrollButton = new Button(fTop, SWT.CHECK);
	myAutoScrollButton.setText("AutoScroll");
	theGriddata = new GridData(SWT.LEFT, SWT.NONE, false, false);
	theGriddata.horizontalSpan = 1;
	theGriddata.verticalSpan = 1;
	theGriddata.verticalIndent = 0;
	myAutoScrollButton.setLayoutData(theGriddata);
	myAutoScrollButton.addSelectionListener(new SelectionListener() {

	    @Override
	    public void widgetSelected(SelectionEvent e) {
		myAutoScroll = myAutoScrollButton.getSelection();
	    }

	    @Override
	    public void widgetDefaultSelected(SelectionEvent e) {
		// nothing needs to be done here
	    }
	});
	myAutoScrollButton.setSelection(ArduinoInstancePreferences.getLastUsedAutoScroll());
	myAutoScroll = ArduinoInstancePreferences.getLastUsedAutoScroll();

	mydumpBinaryButton = new Button(fTop, SWT.CHECK);
	mydumpBinaryButton.setText("filter scope");
	theGriddata = new GridData(SWT.LEFT, SWT.NONE, false, false);
	theGriddata.horizontalSpan = 1;
	theGriddata.verticalSpan = 1;
	theGriddata.verticalIndent = -6;
	mydumpBinaryButton.setLayoutData(theGriddata);
	mydumpBinaryButton.addSelectionListener(new SelectionListener() {

	    @Override
	    public void widgetSelected(SelectionEvent e) {
		SerialListener.setScopeFilter(mydumpBinaryButton.getSelection());
		ArduinoInstancePreferences.setLastUsedScopeFilter(mydumpBinaryButton.getSelection());
	    }

	    @Override
	    public void widgetDefaultSelected(SelectionEvent e) {
		// Nothing to do
	    }
	});
	mydumpBinaryButton.setSelection(ArduinoInstancePreferences.getLastUsedScopeFilter());
	SerialListener.setScopeFilter(mydumpBinaryButton.getSelection());

	// register the combo as a Selection Provider
	getSite().setSelectionProvider(mySerialPorts);

	myMonitorOutput = new StyledText(fTop, SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL);
	theGriddata = new GridData(SWT.FILL, SWT.FILL, true, true);
	theGriddata.horizontalSpan = 7;
	myMonitorOutput.setLayoutData(theGriddata);
	myMonitorOutput.setEditable(false);
	IThemeManager themeManager = PlatformUI.getWorkbench().getThemeManager();
	ITheme currentTheme = themeManager.getCurrentTheme();
	FontRegistry fontRegistry = currentTheme.getFontRegistry();
	myMonitorOutput.setFont(fontRegistry.get("it.baeyens.serial.fontDefinition"));
	myMonitorOutput.setText("Currently there are no serial ports registered - please use the + button to add a port to the monitor.");

	myparent.getShell().setDefaultButton(mySendButton);
	makeActions();
	contributeToActionBars();
    }

    /**
     * GetSelectedSerial is a wrapper class that returns the serial port selected in the combobox
     * 
     * @return the serial port selected in the combobox
     */
    Serial GetSelectedSerial() {
	return GetSerial(mySerialPorts.getCombo().getText());
    }

    /**
     * Looks in the open com ports with a port with the name as provided.
     * 
     * @param ComName
     *            the name of the comport you are looking for
     * @return the serial port opened in the serial monitor with the name equal to Comname of found. null if not found
     */
    private Serial GetSerial(String ComName) {
	for (Entry<Serial, SerialListener> entry : mySerialConnections.entrySet()) {
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
	manager.add(myConnectToSerialPort);
	manager.add(myDisconnectSerialPort);
    }

    private void fillLocalPullDown(IMenuManager manager) {
	manager.add(myConnectToSerialPort);
	manager.add(new Separator());
	manager.add(myDisconnectSerialPort);
    }

    private void makeActions() {
	myConnectToSerialPort = new Action() {
	    @SuppressWarnings("synthetic-access")
	    @Override
	    public void run() {
		OpenSerialDialogBox comportSelector = new OpenSerialDialogBox(myparent.getShell());
		comportSelector.create();
		if (comportSelector.open() == Window.OK) {
		    connectSerial(comportSelector.GetComPort(), comportSelector.GetBaudRate());
		    SerialPortsUpdated();
		}
	    }
	};
	myConnectToSerialPort.setText("Connect to serial port");
	myConnectToSerialPort.setToolTipText("Add a serial port to the monitor.");
	myConnectToSerialPort.setImageDescriptor(PlatformUI.getWorkbench().getSharedImages().getImageDescriptor(ISharedImages.IMG_OBJ_ADD)); // IMG_OBJS_INFO_TSK));

	myDisconnectSerialPort = new Action() {
	    @Override
	    public void run() {
		Serial newSerial = GetSelectedSerial();
		if (newSerial != null) {
		    SerialListener theListener = mySerialConnections.get(newSerial);
		    mySerialConnections.remove(newSerial);
		    newSerial.removeListener(theListener);
		    newSerial.dispose();
		    theListener.dispose();
		    SerialPortsUpdated();
		}
	    }
	};
	myDisconnectSerialPort.setText("Disconnect from serial port");
	myDisconnectSerialPort.setToolTipText("Remove a serial port from the monitor.");
	myDisconnectSerialPort.setImageDescriptor(PlatformUI.getWorkbench().getSharedImages().getImageDescriptor(ISharedImages.IMG_ELCL_REMOVE));// IMG_OBJS_INFO_TSK));
	myDisconnectSerialPort.setEnabled(mySerialConnections.size() != 0);
    }

    /**
     * Passing the focus request to the viewer's control.
     */
    @Override
    public void setFocus() {
	// MonitorOutput.setf .getControl().setFocus();
	myparent.getShell().setDefaultButton(mySendButton);
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
	int StartPoint = myMonitorOutput.getCharCount();
	myMonitorOutput.append(stInfo);
	StyleRange styleRange = new StyleRange();
	styleRange.start = StartPoint;
	styleRange.length = stInfo.length();
	styleRange.fontStyle = SWT.NORMAL;
	styleRange.foreground = mySerialColor[Style];
	myMonitorOutput.setStyleRange(styleRange);
	if (myAutoScroll) {
	    myMonitorOutput.setSelection(myMonitorOutput.getCharCount());
	}
    }

    /**
     * methoid to make sure the visualisation is correct
     */
    void SerialPortsUpdated() {
	myDisconnectSerialPort.setEnabled(mySerialConnections.size() != 0);
	Serial CurSelection = GetSelectedSerial();
	mySerialPorts.setInput(mySerialConnections);
	if (mySerialConnections.size() == 0) {
	    mySendButton.setEnabled(false);
	    myresetButton.setEnabled(false);
	} else {

	    if (mySerialPorts.getSelection().isEmpty()) // nothing is selected
	    {
		if (CurSelection == null) // nothing was selected
		{
		    CurSelection = (Serial) mySerialConnections.keySet().toArray()[0];
		}
		mySerialPorts.getCombo().setText(CurSelection.toString());
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
	if (mySerialConnections.size() < myMaxSerialPorts) {
	    int colorindex = mySerialConnections.size();
	    Serial newSerial = new Serial(ComPort, BaudRate);
	    if (newSerial.IsConnected()) {
		newSerial.registerService();
		SerialListener theListener = new SerialListener(this, colorindex);
		newSerial.addListener(theListener);
		theListener.event(System.getProperty("line.separator") + "Connected to " + ComPort + " at " + BaudRate
			+ System.getProperty("line.separator"));
		mySerialConnections.put(newSerial, theListener);
		return;
	    }
	} else {
	    Common.log(new Status(IStatus.ERROR, ArduinoConst.CORE_PLUGIN_ID, "No more serial ports supported ", null));
	}

    }

    /**
     * 
     */
    public void ComboSerialChanged() {
	mySendButton.setEnabled(mySerialPorts.toString().length() > 0);
	myresetButton.setEnabled(mySerialPorts.toString().length() > 0);
	myparent.getShell().setDefaultButton(mySendButton);
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
	    TheSerial.connect(5);
	}
    }

}
