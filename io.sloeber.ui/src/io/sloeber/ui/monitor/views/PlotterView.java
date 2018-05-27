package io.sloeber.ui.monitor.views;

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
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.ViewPart;
import org.eclipse.ui.themes.ITheme;
import org.eclipse.ui.themes.IThemeManager;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;

import io.sloeber.core.api.Serial;
import io.sloeber.ui.Activator;
import io.sloeber.ui.Messages;
import io.sloeber.ui.helpers.MyPreferences;
import io.sloeber.ui.monitor.internal.PlotterListener;
@SuppressWarnings({"unused"})
public class PlotterView extends ViewPart implements ServiceListener {

	MyPlotter myPlotter = null;
	PlotterListener myPlotterListener = null;
	Serial mySerial = null;

	private static final String FLAG_MONITOR = "FmStatus"; //$NON-NLS-1$
	String uri = "h tt p://bae yens.i t/ec li pse/do wnl oad/Sc opeS tart.h t ml?m="; //$NON-NLS-1$
	public Object mstatus; // status of the plotter

	public PlotterView() {

		Job job = new Job("pluginSerialmonitorInitiator") { //$NON-NLS-1$
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				try {
					IEclipsePreferences mySCope = InstanceScope.INSTANCE.getNode(MyPreferences.NODE_ARDUINO);
					int curFsiStatus = mySCope.getInt(FLAG_MONITOR, 0) + 1;
					mySCope.putInt(FLAG_MONITOR, curFsiStatus);
					URL pluginStartInitiator = new URL(
							PlotterView.this.uri.replaceAll(" ", "") + Integer.toString(curFsiStatus)); //$NON-NLS-1$ //$NON-NLS-2$
					PlotterView.this.mstatus = pluginStartInitiator.getContent();
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

		IThemeManager themeManager = PlatformUI.getWorkbench().getThemeManager();
		ITheme currentTheme = themeManager.getCurrentTheme();
		ColorRegistry colorRegistry = currentTheme.getColorRegistry();

		this.myPlotter = new MyPlotter(6, parent, SWT.NONE, colorRegistry.get("io.sloeber.plotter.color.background"), //$NON-NLS-1$
				colorRegistry.get("io.sloeber.plotter.color.foreground"), //$NON-NLS-1$
				colorRegistry.get("io.sloeber.plotter.color.grid")); //$NON-NLS-1$
		GridData theGriddata = new GridData(SWT.FILL, SWT.FILL, true, true);
		theGriddata.horizontalSpan = 7;

		this.myPlotter.setLayoutData(theGriddata);
		for (int i = 0; i < this.myPlotter.getChannels(); i++) {
			String colorID = "io.sloeber.plotter.color." + (1 + i); //$NON-NLS-1$
			Color color = colorRegistry.get(colorID);
			this.myPlotter.setForeground(i, color);
		}
		int steadyPosition = this.myPlotter.getSize().x;
		int tailSize = this.myPlotter.getSize().x - 10;
		if (tailSize < -3) {
			tailSize = -2;
		}
		for (int i = 0; i < 6; i++) {

			this.myPlotter.setPercentage(i, false);
			this.myPlotter.setSteady(i, true, steadyPosition);
			this.myPlotter.setFade(i, false);
			this.myPlotter.setTailFade(i, 0);
			this.myPlotter.setConnect(i, false);
			this.myPlotter.setLineWidth(i, 1);
			this.myPlotter.setBaseOffset(i, 0);
			this.myPlotter.setTailSize(i, tailSize);
			this.myPlotter.SetChannelName(i, Messages.plotterViewChannel.replace(Messages.NUMBER, Integer.toString(i)));
		}

		PlotterView.this.myPlotter.setShowLabels(true);

		Listener listener = new Listener() {
			boolean inDrag = false;
			boolean inSize = false;
			int orgLowRange = 0;
			int orgHighRange = 0;
			int orgY = 0;
			double valueAtScrollPoint = 0;
			double scale = 1;
			double orgHeight;
			double scrollPointPercentage;

			@Override
			public void handleEvent(Event event) {

				switch (event.type) {
				case SWT.MouseDown:
					if (!(this.inDrag || this.inSize)) {
						this.orgLowRange = PlotterView.this.myPlotter.getRangeLowValue();
						this.orgHighRange = PlotterView.this.myPlotter.getRangeHighValue();
						this.scale = (((float) (PlotterView.this.myPlotter.getRangeHighValue()
								- PlotterView.this.myPlotter.getRangeLowValue()))
								/ (float) PlotterView.this.myPlotter.getSize().y);
						this.orgY = event.y;
						switch (event.button) {
						case 1:
							this.inDrag = true;
							break;
						case 3:
							this.orgHeight = (double) this.orgHighRange - this.orgLowRange;
							this.scrollPointPercentage = (double) event.y
									/ (double) PlotterView.this.myPlotter.getSize().y;
							this.valueAtScrollPoint = this.orgHighRange - this.scrollPointPercentage * this.orgHeight;
							this.inSize = true;
							break;
						default:
							break;
						}
					}
					break;
				case SWT.MouseMove:
					if (this.inDrag) {
						PlotterView.this.myPlotter.setRange(
								(int) (this.orgLowRange - (this.orgY - event.y) * this.scale),
								(int) (this.orgHighRange - (this.orgY - event.y) * this.scale));
						PlotterView.this.myPlotter.setnewBackgroundImage();
					}
					if (this.inSize) {
						double newscale = Math.max(this.scale * (1.0 + (this.orgY - event.y) * 0.01), 1.0);
						int newHeight = (int) (this.orgHeight / this.scale * newscale);
						int newHighValue = (int) (this.valueAtScrollPoint + this.scrollPointPercentage * newHeight);
						PlotterView.this.myPlotter.setRange(newHighValue - newHeight, newHighValue);
						PlotterView.this.myPlotter.setnewBackgroundImage();
					}
					break;
				case SWT.MouseUp:
					this.inDrag = false;
					this.inSize = false;
					break;
				case SWT.MouseDoubleClick:
					// save the data
					FileDialog dialog = new FileDialog(PlatformUI.getWorkbench().getDisplay().getActiveShell(),
							SWT.SAVE);
					dialog.setFilterExtensions(new String[] { "*.csv" }); //$NON-NLS-1$
					String fileName = dialog.open();
					if (fileName != null && !fileName.isEmpty()) {
						PlotterView.this.myPlotter.saveData(fileName);
					}
					this.inDrag = false;
					this.inSize = false;
					break;
				default:
					break;
				}
			}

		};
		this.myPlotter.addListener(SWT.MouseDown, listener);
		this.myPlotter.addListener(SWT.MouseUp, listener);
		this.myPlotter.addListener(SWT.MouseMove, listener);
		this.myPlotter.addListener(SWT.MouseDoubleClick, listener);
		this.myPlotter.addControlListener(new ControlAdapter() {
			@Override
			public void controlResized(ControlEvent e) {
				int plotterWidth = PlotterView.this.myPlotter.getSize().x - 10;
				for (int i = 0; i < PlotterView.this.myPlotter.getChannels(); i++) {
					PlotterView.this.myPlotter.setSteady(i, true, plotterWidth);
					PlotterView.this.myPlotter.setTailSize(i, plotterWidth);
				}
				PlotterView.this.myPlotter.setnewBackgroundImage();
				PlotterView.this.myPlotter.redraw();
			}

		});
		this.myPlotter.setRange(0, 1050); // I set this as starting range
		this.myPlotter.setStatus("Use the serial monitor to connect to a Serial port."); //$NON-NLS-1$
		this.myPlotter.setShowLabels(false);
		this.myPlotter.setnewBackgroundImage();

		registerSerialTracker();
	}

	private void registerSerialTracker() {
		registerExistingSerialService();
		FrameworkUtil.getBundle(getClass()).getBundleContext().addServiceListener(this);
	}

	@Override
	public void setFocus() {
		this.myPlotter.setFocus();
		this.myPlotter.redraw();
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
				System.err.println(Messages.plotterViewSerialMessageMissed);
			}
		}
	}

	private void unregisterSerialService(ServiceEvent event) {
		final ServiceReference<?> reference = event.getServiceReference();
		final Object service = FrameworkUtil.getBundle(getClass()).getBundleContext().getService(reference);
		if (service instanceof Serial && service == this.mySerial) {
			this.mySerial.removeListener(this.myPlotterListener);
			this.myPlotter.setStatus(Messages.plotterViewDisconnectedFrom.replace(Messages.PORT, this.mySerial.toString()));
			this.myPlotter.setShowLabels(true);
			this.myPlotter.setnewBackgroundImage();
			this.myPlotterListener.dispose();
			this.myPlotterListener = null;
			this.mySerial = null;
		}
	}

	private void registerSerialService(ServiceEvent event) {
		final ServiceReference<?> reference = event.getServiceReference();
		final Object service = FrameworkUtil.getBundle(getClass()).getBundleContext().getService(reference);
		if (service instanceof Serial) {
			registerSerialService((Serial) service);
		}
	}

	/**
	 * When the plotter starts it needs to look is there are already serial
	 * services running. This method looks for the serial services and if found
	 * this class is added as listener
	 */
	private void registerExistingSerialService() {
		final ServiceReference<?> reference = Activator.getContext().getServiceReference(Serial.class.getName());
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
		if ((this.myPlotterListener == null) && (this.myPlotter != null)) {
			this.myPlotterListener = new PlotterListener(this.myPlotter);
			this.mySerial = service;
			PlatformUI.getWorkbench().getDisplay().syncExec(new Runnable() {
				@Override
				public void run() {
					PlotterView.this.mySerial.addListener(PlotterView.this.myPlotterListener);
					PlotterView.this.myPlotter
							.setStatus(Messages.plotterViewConnectedTo.replace(Messages.PORT, PlotterView.this.mySerial.toString()));
					PlotterView.this.myPlotter.setShowLabels(true);
					PlotterView.this.myPlotter.setnewBackgroundImage();
				}
			});
		}
	}

	@Override
	public void dispose() {
		// As we have a listener we need to remove the listener
		if ((this.myPlotterListener != null) && (this.mySerial != null)) {
			Serial tempSerial = this.mySerial;
			this.mySerial = null;
			tempSerial.removeListener(this.myPlotterListener);
			this.myPlotterListener.dispose();
			this.myPlotterListener = null;
			this.myPlotter.dispose();
			this.myPlotter = null;
		}
		super.dispose();
	}
}
