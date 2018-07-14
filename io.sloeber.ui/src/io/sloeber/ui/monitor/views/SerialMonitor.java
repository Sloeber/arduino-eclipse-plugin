package io.sloeber.ui.monitor.views;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.io.FileUtils;
import org.eclipse.cdt.core.model.CoreModel;
import org.eclipse.cdt.core.settings.model.ICConfigurationDescription;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IPath;
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
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
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
import io.sloeber.ui.Activator;
import io.sloeber.ui.Messages;
import io.sloeber.ui.helpers.MyPreferences;
import io.sloeber.ui.listeners.ProjectExplorerListener;
import io.sloeber.ui.monitor.internal.SerialListener;

/**
 * SerialMonitor implements the view that shows the serial monitor. Serial
 * monitor get sits data from serial Listener. 1 serial listener is created per
 * serial connection.
 * 
 */
@SuppressWarnings({"unused"})
public class SerialMonitor extends ViewPart implements ISerialUser {

	/**
	 * The ID of the view as specified by the extension.
	 */
	// public static final String ID =
	// "io.sloeber.ui.monitor.views.SerialMonitor";
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
	// filter out binary data from serial monitor
	private Action plotterFilter;
	// clear serial monitor
	private Action clear;

	// The string to send to the serial port
	protected Text sendString;
	//  control contains the output of the serial port
	static protected StyledText monitorOutput;
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

	static private Composite parent;

	/*
	 * ************** Below are variables needed for good housekeeping
	 */

	// The serial connections that are open with the listeners listening to this
	// port
	protected Map<Serial, SerialListener> serialConnections;

	private static final String MY_FLAG_MONITOR = "FmStatus"; //$NON-NLS-1$
	static final String uri = "h tt p://ba eye ns. i t/ec li pse/d ow nlo ad/mo nito rSta rt.ht m l?m="; //$NON-NLS-1$

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
		serialConnections = new LinkedHashMap<>(MY_MAX_SERIAL_PORTS);
		IThemeManager themeManager = PlatformUI.getWorkbench().getThemeManager();
		ITheme currentTheme = themeManager.getCurrentTheme();
		colorRegistry = currentTheme.getColorRegistry();
		serialColorID = new String[MY_MAX_SERIAL_PORTS];
		for (int i = 0; i < MY_MAX_SERIAL_PORTS; i++) {
			serialColorID[i] = "io.sloeber.serial.color." + (1 + i); //$NON-NLS-1$

		}
		SerialManager.registerSerialUser(this);

		Job job = new Job("pluginSerialmonitorInitiator") { //$NON-NLS-1$
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				try {
					IEclipsePreferences myScope = InstanceScope.INSTANCE.getNode(MyPreferences.NODE_ARDUINO);
					int curFsiStatus = myScope.getInt(MY_FLAG_MONITOR, 0) + 1;
					myScope.putInt(MY_FLAG_MONITOR, curFsiStatus);
					URL mypluginStartInitiator = new URL(uri.replace(" ", new String()) //$NON-NLS-1$ 
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

		for (Entry<Serial, SerialListener> entry : serialConnections.entrySet()) {
			entry.getValue().dispose();
			entry.getKey().dispose();
		}
		serialConnections.clear();
		instance = null;
	}

	/**
	 * This is a callback that will allow us to create the viewer and initialize
	 * it.
	 */
	@Override
	public void createPartControl(Composite parent1) {
		parent = parent1;
		parent1.setLayout(new GridLayout());
		GridLayout layout = new GridLayout(5, false);
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		Composite top = new Composite(parent1, SWT.NONE);
		top.setLayout(layout);
		top.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		serialPorts = new ComboViewer(top, SWT.READ_ONLY | SWT.DROP_DOWN);
		GridData minSizeGridData = new GridData(SWT.LEFT, SWT.CENTER, false, false);
		minSizeGridData.widthHint = 150;
		serialPorts.getControl().setLayoutData(minSizeGridData);
		serialPorts.setContentProvider(new IStructuredContentProvider() {

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
		serialPorts.setLabelProvider(new LabelProvider());
		serialPorts.setInput(serialConnections);
		serialPorts.addSelectionChangedListener(new ComPortChanged(this));

		sendString = new Text(top, SWT.SINGLE | SWT.BORDER);
		sendString.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

		lineTerminator = new ComboViewer(top, SWT.READ_ONLY | SWT.DROP_DOWN);
		lineTerminator.setContentProvider(new ArrayContentProvider());
		lineTerminator.setLabelProvider(new LabelProvider());
		lineTerminator.setInput(SerialManager.listLineEndings());
		lineTerminator.getCombo().select(MyPreferences.getLastUsedSerialLineEnd());
		lineTerminator.addSelectionChangedListener(new ISelectionChangedListener() {

			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				MyPreferences
						.setLastUsedSerialLineEnd(lineTerminator.getCombo().getSelectionIndex());
			}
		});

		send = new Button(top, SWT.BUTTON1);
		send.setText(Messages.serialMonitorSend);
		send.addSelectionListener(new SelectionListener() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				int index = lineTerminator.getCombo().getSelectionIndex();
				GetSelectedSerial().write(sendString.getText(), SerialManager.getLineEnding(index));
				sendString.setText(new String()); 
				sendString.setFocus();
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				// nothing needs to be done here
			}
		});
		send.setEnabled(false);

		reset = new Button(top, SWT.BUTTON1);
		reset.setText(Messages.serialMonitorReset);
		reset.addSelectionListener(new SelectionListener() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				GetSelectedSerial().reset();
				sendString.setFocus();
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				// nothing needs to be done here
			}
		});
		reset.setEnabled(false);

		// register the combo as a Selection Provider
		getSite().setSelectionProvider(serialPorts);

		monitorOutput = new StyledText(top, SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL);
		monitorOutput.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 5, 1));
		monitorOutput.setEditable(false);
		IThemeManager themeManager = PlatformUI.getWorkbench().getThemeManager();
		ITheme currentTheme = themeManager.getCurrentTheme();
		FontRegistry fontRegistry = currentTheme.getFontRegistry();
		monitorOutput.setFont(fontRegistry.get("io.sloeber.serial.fontDefinition")); //$NON-NLS-1$
		monitorOutput.setText(Messages.serialMonitorNoInput);
		monitorOutput.addMouseListener(new MouseListener() {
            
            @Override
            public void mouseUp(MouseEvent e) {
             // ignore
            }
            
            @Override
            public void mouseDown(MouseEvent e) {
                // If right button get selected text save it and start external tool
                if (e.button==3) {
                    String selectedText=monitorOutput.getSelectionText();
                    if(!selectedText.isEmpty()) {
                        IProject selectedProject = ProjectExplorerListener.getSelectedProject();
                        if (selectedProject!=null) {
                            
                            try {
                                ICConfigurationDescription activeCfg=CoreModel.getDefault().getProjectDescription(selectedProject).getActiveConfiguration();
                                String activeConfigName= activeCfg.getName();
                                IPath buildFolder=selectedProject.findMember(activeConfigName).getLocation();
                                File dumpFile=buildFolder.append("serialdump.txt").toFile(); //$NON-NLS-1$
                                FileUtils.writeStringToFile(dumpFile, selectedText);
                            } catch (Exception e1) {
                                // ignore
                                e1.printStackTrace();
                            }
                        }
                        
                    }
                }
            }
            
            @Override
            public void mouseDoubleClick(MouseEvent e) {
                // ignore
            }
        });

		parent.getShell().setDefaultButton(send);
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
		return GetSerial(serialPorts.getCombo().getText());
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
		for (Entry<Serial, SerialListener> entry : serialConnections.entrySet()) {
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
		manager.add(clear);
		manager.add(scrollLock);
		manager.add(plotterFilter);
		manager.add(connect);
		manager.add(disconnect);
	}

	private void fillLocalPullDown(IMenuManager manager) {
		manager.add(connect);
		manager.add(new Separator());
		manager.add(disconnect);
	}

	private void makeActions() {
		connect = new Action() {
			@SuppressWarnings("synthetic-access")
			@Override
			public void run() {
				OpenSerialDialogBox comportSelector = new OpenSerialDialogBox(parent.getShell());
				comportSelector.create();
				if (comportSelector.open() == Window.OK) {
					connectSerial(comportSelector.GetComPort(), comportSelector.GetBaudRate());

				}
			}
		};
		connect.setText(Messages.serialMonitorConnectedTo);
		connect.setToolTipText(Messages.serialMonitorAddConnectionToSeralMonitor);
		connect.setImageDescriptor(
				PlatformUI.getWorkbench().getSharedImages().getImageDescriptor(ISharedImages.IMG_OBJ_ADD)); // IMG_OBJS_INFO_TSK));

		disconnect = new Action() {
			@Override
			public void run() {
				disConnectSerialPort(getSerialMonitor().serialPorts.getCombo().getText());
			}
		};
		disconnect.setText(Messages.serialMonitorDisconnectedFrom);
		disconnect.setToolTipText(Messages.serialMonitorRemoveSerialPortFromMonitor);
		disconnect.setImageDescriptor(
				PlatformUI.getWorkbench().getSharedImages().getImageDescriptor(ISharedImages.IMG_TOOL_DELETE));// IMG_OBJS_INFO_TSK));
		disconnect.setEnabled(serialConnections.size() != 0);

		clear = new Action(Messages.serialMonitorClear) {
			@Override
			public void run() {
				clearMonitor();
			}
		};
		clear.setImageDescriptor(ImageDescriptor.createFromURL(IMG_CLEAR));
		clear.setEnabled(true);

		scrollLock = new Action(Messages.serialMonitorScrollLock, IAction.AS_CHECK_BOX) {
			@Override
			public void run() {
				MyPreferences.setLastUsedAutoScroll(!isChecked());
			}
		};
		scrollLock.setImageDescriptor(ImageDescriptor.createFromURL(IMG_LOCK));
		scrollLock.setEnabled(true);
		scrollLock.setChecked(!MyPreferences.getLastUsedAutoScroll());

		plotterFilter = new Action(Messages.serialMonitorFilterPloter, IAction.AS_CHECK_BOX) {
			@Override
			public void run() {
				SerialListener.setPlotterFilter(isChecked());
				MyPreferences.setLastUsedPlotterFilter(isChecked());
			}
		};
		plotterFilter.setImageDescriptor(ImageDescriptor.createFromURL(IMG_FILTER));
		plotterFilter.setEnabled(true);
		plotterFilter.setChecked(MyPreferences.getLastUsedPlotterFilter());
		SerialListener.setPlotterFilter(MyPreferences.getLastUsedPlotterFilter());
	}

	/**
	 * Passing the focus request to the viewer's control.
	 */
	@Override
	public void setFocus() {
		// MonitorOutput.setf .getControl().setFocus();
		parent.getShell().setDefaultButton(send);
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
		int startPoint = monitorOutput.getCharCount();
		monitorOutput.append(stInfo);
		StyleRange styleRange = new StyleRange();
		styleRange.start = startPoint;
		styleRange.length = stInfo.length();
		styleRange.fontStyle = SWT.NORMAL;
		styleRange.foreground = colorRegistry.get(serialColorID[style]);
		monitorOutput.setStyleRange(styleRange);
		if (!scrollLock.isChecked()) {
			monitorOutput.setSelection(monitorOutput.getCharCount());
		}
	}

	/**
	 * method to make sure the visualization is correct
	 */
	void SerialPortsUpdated() {
		disconnect.setEnabled(serialConnections.size() != 0);
		Serial curSelection = GetSelectedSerial();
		serialPorts.setInput(serialConnections);
		if (serialConnections.size() == 0) {
			send.setEnabled(false);
			reset.setEnabled(false);
		} else {

			if (serialPorts.getSelection().isEmpty()) // nothing is
			// selected
			{
				if (curSelection == null) // nothing was selected
				{
					curSelection = (Serial) serialConnections.keySet().toArray()[0];
				}
				serialPorts.getCombo().setText(curSelection.toString());
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
		if (serialConnections.size() < MY_MAX_SERIAL_PORTS) {
			int colorindex = serialConnections.size();
			Serial newSerial = new Serial(comPort, baudRate);
			if (newSerial.IsConnected()) {
				newSerial.registerService();
				SerialListener theListener = new SerialListener(this, colorindex);
				newSerial.addListener(theListener);
				String newLine=System.getProperty("line.separator");//$NON-NLS-1$
				theListener.event( newLine+ Messages.serialMonitorConnectedTo.replace(Messages.PORT, comPort).replace(Messages.BAUD,Integer.toString(baudRate) ) 
						+ newLine); 
				serialConnections.put(newSerial, theListener);
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
			SerialListener theListener = serialConnections.get(newSerial);
			serialConnections.remove(newSerial);
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
		send.setEnabled(serialPorts.toString().length() > 0);
		reset.setEnabled(serialPorts.toString().length() > 0);
		parent.getShell().setDefaultButton(send);
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
						monitorOutput.setText(new String()); 
					}
				});

			}
			theSerial.connect(15);
		}
	}

	public static List<String> getMonitorContent() {
		int numLines =monitorOutput.getContent().getLineCount();
		List<String>ret=new ArrayList<>();
		for(int curLine=1;curLine<numLines;curLine++) {
			ret.add(monitorOutput.getContent().getLine(curLine-1));
		}
		return ret;
	}
	public static void clearMonitor() {
		monitorOutput.setText(new String());
	}
}
