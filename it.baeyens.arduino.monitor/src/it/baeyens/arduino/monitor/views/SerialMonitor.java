package it.baeyens.arduino.monitor.views;

import it.baeyens.arduino.arduino.Serial;
import it.baeyens.arduino.common.ArduinoConst;
import it.baeyens.arduino.common.ArduinoInstancePreferences;
import it.baeyens.arduino.common.Common;
import it.baeyens.arduino.common.ISerialUser;

import java.net.URL;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;

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
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.LabelProvider;
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

/**
 * This sample class demonstrates how to plug-in a new workbench view. The view
 * shows data obtained from the model. The sample creates a dummy model on the
 * fly, but a real implementation would connect to the model available either in
 * this or another plug-in (e.g. the workspace). The view is connected to the
 * model using a content provider.
 * <p>
 * The view uses a label provider to define how model objects should be
 * presented in the view. Each view can present the same model objects using
 * different labels and icons, if needed. Alternatively, a single label provider
 * can be shared between views in order to ensure that objects of the same type
 * are presented in the same way everywhere.
 * <p>
 */

public class SerialMonitor extends ViewPart implements ISerialUser {

	/**
	 * The ID of the view as specified by the extension.
	 */
	public static final String ID = "it.baeyens.arduino.monitor.views.SerialMonitor";
	static private final int MaxSerialPorts = 3; // If you increase this number
	// you must also assign colors
	private Action ConnectToSerialPort; // Connect to a serial port
	private Action disconnectSerialPort; // this action will disconnect the
	// serial port selected by the
	// SerialPorts combi

	Text SendString; // The string to send to the serial port
	StyledText MonitorOutput; // This control contains the output of the serial
	// port
	private ComboViewer SerialPorts; // Port used when doing actions
	ComboViewer SendPostFix; // Add CR? LF? CR+LF? Nothing?
	private Button SendButton; // When click will send the content of SendString
	// to the port selected
	// SerialPorts adding the postfix selected in
	// SendPostFix
	private Button resetButton; // The button to reset the arduino
	private Button ClearButton; // the button to clear the monitor
	Button autoScrollButton;

	private Color SerialColor[]; // Contains the colors that are used

	private Composite myparent;

	// Below are variables needed for good housekeeping
	Collection<Serial> SerialConnections; // The serial connections that are
	// open
	int LastUsedIndex; // the last used index of the SendPostFix combo
	boolean autoScroll; // is auto scroll on or off?

	public URL pluginStartInitiator = null; // Initiator to start the plugin
	public Object mstatus; // status of the plugin
	private static final String flagMonitor = "F" + "m" + "S" + "t" + "a" + "t" + "u" + "s";
	char[] uri = { 'h', 't', 't', 'p', ':', '/', '/', 'b', 'a', 'e', 'y', 'e', 'n', 's', '.', 'i', 't', '/', 'e', 'c', 'l', 'i', 'p', 's', 'e', '/',
			'd', 'o', 'w', 'n', 'l', 'o', 'a', 'd', '/', 'm', 'o', 'n', 'i', 't', 'o', 'r', 'S', 't', 'a', 'r', 't', '.', 'h', 't', 'm', 'l', '?',
			'm', '=' };

	/**
	 * The constructor.
	 */
	public SerialMonitor() {
		SerialConnections = new HashSet<Serial>(MaxSerialPorts);
		SerialColor = new Color[MaxSerialPorts];
		SerialColor[0] = new Color(null, 0, 0, 0);
		SerialColor[1] = new Color(null, 255, 0, 0);
		SerialColor[2] = new Color(null, 0, 255, 0);
		Common.registerSerialUser(this);

		Job job = new Job("pluginSerialmonitorInitiator") {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				try {
					IEclipsePreferences myScope = InstanceScope.INSTANCE.getNode(ArduinoConst.NODE_ARDUINO);
					int curFsiStatus = myScope.getInt(flagMonitor, 0) + 1;
					myScope.putInt(flagMonitor, curFsiStatus);
					pluginStartInitiator = new URL(new String(uri) + Integer.toString(curFsiStatus));
					mstatus = pluginStartInitiator.getContent();
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
		ArduinoInstancePreferences.SetLastUsedSerialLineEnd(LastUsedIndex);
		ArduinoInstancePreferences.setLastUsedAutoScroll(autoScroll);
		for (int curColor = 0; curColor < MaxSerialPorts; curColor++) {
			SerialColor[curColor].dispose();
		}
		java.util.Iterator<Serial> iterator = SerialConnections.iterator();
		while (iterator.hasNext()) {
			Serial element = iterator.next();
			element.dispose();
		}
	}

	/**
	 * This is a callback that will allow us to create the viewer and initialize
	 * it.
	 */
	@Override
	public void createPartControl(Composite parent) {
		myparent = parent;
		parent.setLayout(new GridLayout());
		GridLayout gl = new GridLayout(8, false);
		gl.marginHeight = 0;
		gl.marginWidth = 0;
		Composite fTop = new Composite(parent, SWT.NONE);
		fTop.setLayout(gl);
		fTop.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		SerialPorts = new ComboViewer(fTop, SWT.READ_ONLY | SWT.DROP_DOWN);
		GridData MinimuSizeGridData = new GridData(SWT.LEFT, SWT.NONE, false, false);
		MinimuSizeGridData.widthHint = 50;
		MinimuSizeGridData.horizontalSpan = 1;
		SerialPorts.getControl().setLayoutData(MinimuSizeGridData);
		SerialPorts.setContentProvider(new ArrayContentProvider());
		SerialPorts.setLabelProvider(new LabelProvider());
		SerialPorts.setInput(SerialConnections);
		SerialPorts.addSelectionChangedListener(new ComPortChanged(this));

		SendString = new Text(fTop, SWT.SINGLE | SWT.BORDER);
		GridData theGriddata = new GridData(SWT.FILL, SWT.CENTER, true, false);
		theGriddata.horizontalSpan = 1;
		SendString.setLayoutData(theGriddata);

		SendPostFix = new ComboViewer(fTop, SWT.READ_ONLY | SWT.DROP_DOWN);
		SendPostFix.getControl().setLayoutData(new GridData(SWT.LEFT, SWT.NONE, false, false));
		SendPostFix.setContentProvider(new ArrayContentProvider());
		SendPostFix.setLabelProvider(new LabelProvider());
		SendPostFix.setInput(Common.listLineEndings());
		SendPostFix.getCombo().select(ArduinoInstancePreferences.GetLastUsedSerialLineEnd());

		SendButton = new Button(fTop, SWT.BUTTON1);
		SendButton.setText("Send");
		SendButton.setLayoutData(new GridData(SWT.RIGHT, SWT.NONE, false, false));
		SendButton.addSelectionListener(new SelectionListener() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				LastUsedIndex = SendPostFix.getCombo().getSelectionIndex();
				GetSelectedSerial().write(SendString.getText(), Common.getLineEnding(LastUsedIndex)); // System.getProperty("line.separator"));
				SendString.setText("");
				SendString.setFocus();
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				// nothing needs to be done here
			}
		});
		SendButton.setEnabled(false);

		resetButton = new Button(fTop, SWT.BUTTON1);
		resetButton.setText("Reset");
		resetButton.setLayoutData(new GridData(SWT.RIGHT, SWT.NONE, false, false));
		resetButton.addSelectionListener(new SelectionListener() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				LastUsedIndex = SendPostFix.getCombo().getSelectionIndex();
				GetSelectedSerial().reset();
				SendString.setFocus();
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				// nothing needs to be done here
			}
		});
		resetButton.setEnabled(false);

		ClearButton = new Button(fTop, SWT.BUTTON1);
		ClearButton.setText("Clear");
		ClearButton.setLayoutData(new GridData(SWT.RIGHT, SWT.NONE, false, false));
		ClearButton.addSelectionListener(new SelectionListener() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				MonitorOutput.setText("");
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				// nothing needs to be done here
			}
		});
		ClearButton.setEnabled(true);

		autoScrollButton = new Button(fTop, SWT.CHECK);
		autoScrollButton.setText("AutoScroll");
		autoScrollButton.setLayoutData(new GridData(SWT.RIGHT, SWT.NONE, false, false));
		autoScrollButton.addSelectionListener(new SelectionListener() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				autoScroll = autoScrollButton.getSelection();
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				// nothing needs to be done here
			}
		});
		autoScrollButton.setSelection(ArduinoInstancePreferences.getLastUsedAutoScroll());
		autoScroll = ArduinoInstancePreferences.getLastUsedAutoScroll();

		// register the combo as a Selection Provider
		getSite().setSelectionProvider(SerialPorts);

		MonitorOutput = new StyledText(fTop, SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL);
		theGriddata = new GridData(SWT.FILL, SWT.FILL, true, true);
		theGriddata.horizontalSpan = 7;
		MonitorOutput.setLayoutData(theGriddata);
		MonitorOutput.setEditable(false);
		MonitorOutput.setText("Currently there are no serial ports registered - please use the + button to add a port to monitor.");

		myparent.getShell().setDefaultButton(SendButton);
		makeActions();
		contributeToActionBars();
	}

	Serial GetSelectedSerial() {
		return GetSerial(SerialPorts.getCombo().getText());
	}

	private Serial GetSerial(String ComName) {
		Iterator<Serial> iterator = SerialConnections.iterator();
		while (iterator.hasNext()) {
			Serial thePortToSendDataTo = iterator.next();
			// Do something with element
			if (thePortToSendDataTo.toString().matches(ComName))
				return thePortToSendDataTo;
		}
		return null;
	}

	private void contributeToActionBars() {
		IActionBars bars = getViewSite().getActionBars();
		fillLocalPullDown(bars.getMenuManager());
		fillLocalToolBar(bars.getToolBarManager());
	}

	private void fillLocalToolBar(IToolBarManager manager) {
		manager.add(ConnectToSerialPort);
		manager.add(disconnectSerialPort);
	}

	private void fillLocalPullDown(IMenuManager manager) {
		manager.add(ConnectToSerialPort);
		manager.add(new Separator());
		manager.add(disconnectSerialPort);
	}

	private void makeActions() {
		ConnectToSerialPort = new Action() {
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
		ConnectToSerialPort.setText("Connect to serial port");
		ConnectToSerialPort.setToolTipText("Add a serial port to the monitor.");
		ConnectToSerialPort.setImageDescriptor(PlatformUI.getWorkbench().getSharedImages().getImageDescriptor(ISharedImages.IMG_OBJ_ADD)); // IMG_OBJS_INFO_TSK));

		disconnectSerialPort = new Action() {
			@Override
			public void run() {
				Serial newSerial = GetSelectedSerial();
				if (newSerial != null) {
					SerialConnections.remove(newSerial);
					newSerial.dispose();
					SerialPortsUpdated();
				}
			}
		};
		disconnectSerialPort.setText("Disconnect from serial port");
		disconnectSerialPort.setToolTipText("Remove a serial port from the monitor.");
		disconnectSerialPort.setImageDescriptor(PlatformUI.getWorkbench().getSharedImages().getImageDescriptor(ISharedImages.IMG_ELCL_REMOVE));// IMG_OBJS_INFO_TSK));
		disconnectSerialPort.setEnabled(SerialConnections.size() != 0);
	}

	/**
	 * Passing the focus request to the viewer's control.
	 */
	@Override
	public void setFocus() {
		// MonitorOutput.setf .getControl().setFocus();
		myparent.getShell().setDefaultButton(SendButton);
	}

	public void ReportSerialActivity(String stInfo, int Style) {
		int StartPoint = MonitorOutput.getCharCount();
		MonitorOutput.append(stInfo);
		StyleRange styleRange = new StyleRange();
		styleRange.start = StartPoint;
		styleRange.length = stInfo.length();
		styleRange.fontStyle = SWT.NORMAL;
		styleRange.foreground = SerialColor[Style];
		MonitorOutput.setStyleRange(styleRange);
		if (autoScroll) {
			MonitorOutput.setSelection(MonitorOutput.getCharCount());
		}
	}

	void SerialPortsUpdated() {
		disconnectSerialPort.setEnabled(SerialConnections.size() != 0);
		Serial CurSelection = GetSelectedSerial();
		SerialPorts.setInput(SerialConnections);
		if (SerialConnections.size() == 0) {
			SendButton.setEnabled(false);
			resetButton.setEnabled(false);
			// //SerialPorts.setSelection(null); // this is not needed as this
			// is done automatically
		} else {

			if (SerialPorts.getSelection().isEmpty()) // nothing is selected
			{
				if (CurSelection == null) // nothing was selected
				{
					CurSelection = (Serial) SerialConnections.toArray()[0];
				}
				SerialPorts.getCombo().setText(CurSelection.toString());
				ComboSerialChanged();
			}
		}
	}

	public void connectSerial(String ComPort, int BaudRate) {
		if (SerialConnections.size() < MaxSerialPorts) {
			int colorindex = SerialConnections.size();
			Serial newSerial = new Serial(ComPort, BaudRate);
			if (newSerial.IsConnected()) {
				newSerial.registerService();
				SerialListener theListener = new SerialListener(this, colorindex);
				newSerial.addListener(theListener);
				theListener.message(System.getProperty("line.separator") + "Connected to " + ComPort + " at " + BaudRate
						+ System.getProperty("line.separator"));
				SerialConnections.add(newSerial);
				return;
			}
		} else {
			Common.log(new Status(IStatus.ERROR, ArduinoConst.CORE_PLUGIN_ID, "No more serial ports supported ", null));
		}

	}

	public void ComboSerialChanged() {
		SendButton.setEnabled(SerialPorts.toString().length() > 0);
		resetButton.setEnabled(SerialPorts.toString().length() > 0);
		myparent.getShell().setDefaultButton(SendButton);
	}

	@Override
	public boolean PauzePort(String PortName) {
		Serial TheSerial = GetSerial(PortName);
		if (TheSerial != null) {
			TheSerial.disconnect();
			return true;
		}
		return false;
	}

	@Override
	public void ResumePort(String PortName) {
		Serial TheSerial = GetSerial(PortName);
		if (TheSerial != null) {
			TheSerial.connect();
		}
	}

}
