package io.sloeber.core.tools.uploaders;

import java.io.IOException;
import java.net.URL;

import org.eclipse.cdt.core.settings.model.ICConfigurationDescription;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.jface.resource.ColorRegistry;
import org.eclipse.jface.resource.FontRegistry;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.console.MessageConsole;
import org.eclipse.ui.console.MessageConsoleStream;
import org.eclipse.ui.themes.ITheme;
import org.eclipse.ui.themes.IThemeManager;

import io.sloeber.core.Activator;
import io.sloeber.core.Messages;
import io.sloeber.core.api.BoardDescriptor;
import io.sloeber.core.api.SerialManager;
import io.sloeber.core.common.Common;
import io.sloeber.core.common.Const;
import io.sloeber.core.tools.Helpers;

public class UploadSketchWrapper {
	// preference nodes
	public static final String NODE_ARDUINO = Activator.NODE_ARDUINO;
	public static final String UPLOAD_TOOL_TEENSY = "teensy_reboot"; //$NON-NLS-1$
	public static final String UPLOAD_SSH = "ssh upload"; //$NON-NLS-1$

	static UploadSketchWrapper myThis = null;

	private UploadSketchWrapper() {
		// no constructor needed
	}

	static private UploadSketchWrapper getUploadSketchWrapper() {
		if (myThis == null) {
			myThis = new UploadSketchWrapper();
		}
		return myThis;
	}

	static public Job upload(IProject Project,
			ICConfigurationDescription confDesc) {
		return getUploadSketchWrapper().internalUpload(Project, confDesc);
	}

	public Job internalUpload(IProject project,
			ICConfigurationDescription confDesc) {
		BoardDescriptor boardDescriptor = BoardDescriptor
				.makeBoardDescriptor(confDesc);
		String UpLoadTool = boardDescriptor.getActualUploadTool(confDesc);
		// String uploadClass = Common.getBuildEnvironmentVariable(confDesc,
		// Common.get_ENV_KEY_TOOL(Const.UPLOAD_CLASS),
		// new String());/** @jniclass flags=no_gen */

		IRealUpload realUploader = null;
		String uploadJobName = null;

		if (boardDescriptor.isNetworkUpload()) {
			// TOFIX is this ok?
			// if (!Const.UPLOAD_CLASS_DEFAULT.equals(uploadClass)) {
			realUploader = new arduinoUploader(project, confDesc, UpLoadTool);
			uploadJobName = UpLoadTool;
			// } else {
			// myHighLevelConsoleStream.println(Messages.Upload_ssh);
			//
			// realUploader = new SSHUpload(project, UpLoadTool,
			// myHighLevelConsoleStream,
			// myOutconsoleStream, myErrconsoleStream, host);
			// uploadJobName = UPLOAD_SSH;
			// }
		} else if (UpLoadTool.equalsIgnoreCase(UPLOAD_TOOL_TEENSY)) {
			realUploader = new GenericLocalUploader(UpLoadTool, confDesc);
			uploadJobName = UpLoadTool;
		} else {
			realUploader = new arduinoUploader(project, confDesc, UpLoadTool);
			uploadJobName = UpLoadTool;
		}

		Job uploadjob = new UploadJobWrapper(uploadJobName, project, confDesc,
				realUploader, boardDescriptor);
		uploadjob.setRule(null);
		uploadjob.setPriority(Job.LONG);
		uploadjob.setUser(true);
		uploadjob.schedule();

		Job job = new Job("pluginUploadStartInitiator") { //$NON-NLS-1$
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				try {
					String uploadflag = "FuStatus"; //$NON-NLS-1$
					char[] uri = {'h', 't', 't', 'p', ':', '/', '/', 'b', 'a',
							'e', 'y', 'e', 'n', 's', '.', 'i', 't', '/', 'e',
							'c', 'l', 'i', 'p', 's', 'e', '/', 'd', 'o', 'w',
							'n', 'l', 'o', 'a', 'd', '/', 'u', 'p', 'l', 'o',
							'a', 'd', 'S', 't', 'a', 'r', 't', '.', 'h', 't',
							'm', 'l', '?', 'u', '='};
					IEclipsePreferences myScope = InstanceScope.INSTANCE
							.getNode(NODE_ARDUINO);
					int curFsiStatus = myScope.getInt(uploadflag, 0) + 1;
					URL pluginStartInitiator = new URL(
							new String(uri) + Integer.toString(curFsiStatus));
					pluginStartInitiator.getContent();
					myScope.putInt(uploadflag, curFsiStatus);
				} catch (Exception e) {
					e.printStackTrace();
				}
				return Status.OK_STATUS;
			}
		};
		job.setPriority(Job.DECORATE);
		job.schedule();
		return uploadjob;

	}

	/**
	 * UploadJobWrapper stops the serial port and restarts the serial port as
	 * needed. in between it calls the real uploader IUploader
	 *
	 * @author jan
	 *
	 */
	private class UploadJobWrapper extends Job {
		private static final String PROJECT = Messages.PROJECT;
		private static final String UPLOADER = Messages.UPLOADER;
		private static final String FILE = Messages.FILE;
		IProject myProject;
		ICConfigurationDescription myConfDes;
		String myNAmeTag;
		IRealUpload myUploader;
		BoardDescriptor myBoardDescriptor;

		public UploadJobWrapper(String name, IProject project,
				ICConfigurationDescription cConf, IRealUpload uploader,
				BoardDescriptor boardDescriptor) {
			super(name);
			myNAmeTag = name.toUpperCase();
			myProject = project;
			myConfDes = cConf;
			myUploader = uploader;
			myBoardDescriptor = boardDescriptor;
		}

		@Override
		protected IStatus run(IProgressMonitor monitor) {
			IStatus ret = Status.OK_STATUS;
			MessageConsole console = Helpers.findConsole(
					Messages.Upload_console_name.replace(PROJECT, myProject.getName() ));
			console.clearConsole();
			console.activate();
			try (MessageConsoleStream highLevelStream = console
					.newMessageStream();
					MessageConsoleStream outStream = console.newMessageStream();
					MessageConsoleStream errStream = console
							.newMessageStream();) {
				PlatformUI.getWorkbench().getDisplay().syncExec(new Runnable() {

					@Override
					public void run() {
						IThemeManager themeManager = PlatformUI.getWorkbench()
								.getThemeManager();
						ITheme currentTheme = themeManager.getCurrentTheme();
						ColorRegistry colorRegistry = currentTheme
								.getColorRegistry();
						FontRegistry fontRegistry = currentTheme
								.getFontRegistry();

						console.setFont(fontRegistry.get(
								"io.sloeber.ui.uploadConsole.fontDefinition")); //$NON-NLS-1$
						highLevelStream.setColor(colorRegistry.get(
								"io.sloeber.ui.uploadConsole.colorDefinition.high")); //$NON-NLS-1$
						outStream.setColor(colorRegistry.get(
								"io.sloeber.ui.uploadConsole.colorDefinition.stdout")); //$NON-NLS-1$
						errStream.setColor(colorRegistry.get(
								"io.sloeber.ui.uploadConsole.colorDefinition.stderr")); //$NON-NLS-1$
					}
				});

				highLevelStream.println(Messages.Upload_starting);
				IFile hexFile = myProject
						.getFile(myConfDes.getBuildSetting().getBuilderCWD()
								.append(myProject.getName() + ".hex")); //$NON-NLS-1$
				boolean WeStoppedTheComPort = false;
				try {
					String message = Messages.Upload_uploading
							.replace(PROJECT, myProject.getName()) 
							.replace(UPLOADER, myNAmeTag) ; 
					highLevelStream.println(message);
					monitor.beginTask(message, 2);
					try {
						WeStoppedTheComPort = SerialManager.StopSerialMonitor(
								myBoardDescriptor.getActualUploadPort());
					} catch (Exception e) {
						ret = new Status(IStatus.WARNING, Const.CORE_PLUGIN_ID,
								Messages.Upload_Error_com_port, e);
						Common.log(ret);
					}


					if (!myUploader.uploadUsingPreferences(hexFile,
							myBoardDescriptor, monitor, highLevelStream,
							outStream, errStream)) {
						String error=Messages.Upload_failed_upload_file.replace(FILE, hexFile.toString());
						highLevelStream.println(error);
						ret = new Status(IStatus.ERROR, Const.CORE_PLUGIN_ID,error);
					}

				} catch (Exception e) {
					String error=Messages.Upload_failed_upload_file.replace(FILE, hexFile.toString());
					Common.log(new Status(IStatus.ERROR, Const.CORE_PLUGIN_ID,error, e));
				} finally {
					try {
						if (WeStoppedTheComPort) {
							SerialManager.StartSerialMonitor(
									myBoardDescriptor.getActualUploadPort());
						}

					} catch (Exception e) {
						ret = new Status(IStatus.WARNING, Const.CORE_PLUGIN_ID,
								Messages.Upload_Error_serial_monitor_restart,
								e);
						Common.log(ret);
					}
					monitor.done();
				}

			} catch (IOException e1) {
				e1.printStackTrace();
			}
			return ret;
		}
	}
}
