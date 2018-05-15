package io.sloeber.core.tools.uploaders;

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
import org.eclipse.ui.console.MessageConsole;
import org.eclipse.ui.console.MessageConsoleStream;

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
	MessageConsole myConsole = null;
	MessageConsoleStream myHighLevelStream = null;
	MessageConsoleStream myOutStream = null;
	MessageConsoleStream myErrStream = null;

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

		// TOFIX issue 769 can it be fixed by simply adding the projectname to
		// the name
		myConsole = Helpers.findConsole(Messages.Upload_console);
		myConsole.clearConsole();
		myConsole.activate();
		myHighLevelStream = myConsole.newMessageStream();
		myOutStream = myConsole.newMessageStream();
		myErrStream = myConsole.newMessageStream();
		// TOFIX issue 717 set font/color based on preference settings
		// this.myHighLevelConsoleStream.setColor(PlatformUI.getWorkbench().getDisplay().getSystemColor(SWT.COLOR_BLACK));
		// this.myOutconsoleStream.setColor(PlatformUI.getWorkbench().getDisplay().getSystemColor(SWT.COLOR_DARK_GREEN));
		// this.myErrconsoleStream.setColor(PlatformUI.getWorkbench().getDisplay().getSystemColor(SWT.COLOR_RED));
		this.myHighLevelStream.println(Messages.Upload_starting);
		IRealUpload realUploader = null;
		String uploadJobName = null;

		if (boardDescriptor.isNetworkUpload()) {
			// TOFIX is this ok?
			// if (!Const.UPLOAD_CLASS_DEFAULT.equals(uploadClass)) {
			this.myHighLevelStream.println(Messages.Upload_arduino);
			realUploader = new arduinoUploader(project, confDesc, UpLoadTool,
					myHighLevelStream, myOutStream, myErrStream);
			uploadJobName = UpLoadTool;
			// } else {
			// this.myHighLevelConsoleStream.println(Messages.Upload_ssh);
			//
			// realUploader = new SSHUpload(project, UpLoadTool,
			// this.myHighLevelConsoleStream,
			// this.myOutconsoleStream, this.myErrconsoleStream, host);
			// uploadJobName = UPLOAD_SSH;
			// }
		} else if (UpLoadTool.equalsIgnoreCase(UPLOAD_TOOL_TEENSY)) {
			this.myHighLevelStream.println(Messages.Upload_generic);
			realUploader = new GenericLocalUploader(UpLoadTool, confDesc,
					myHighLevelStream, myErrStream, myOutStream);
			uploadJobName = UpLoadTool;
		} else {
			this.myHighLevelStream.println(Messages.Upload_arduino);
			realUploader = new arduinoUploader(project, confDesc, UpLoadTool,
					myHighLevelStream, myOutStream, myErrStream);
			uploadJobName = UpLoadTool;
		}

		Job uploadjob = new UploadJobWrapper(uploadJobName, project, confDesc,
				realUploader, boardDescriptor);
		uploadjob.setRule(null);
		uploadjob.setPriority(Job.LONG);
		uploadjob.setUser(true);
		uploadjob.schedule();

		Job job = new Job(Messages.Upload_PluginStartInitiator) {
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
		IProject myProject;
		ICConfigurationDescription myConfDes;
		String myNAmeTag;
		IRealUpload myUploader;
		BoardDescriptor myBoardDescriptor;

		public UploadJobWrapper(String name, IProject project,
				ICConfigurationDescription cConf, IRealUpload uploader,
				BoardDescriptor boardDescriptor) {
			super(name);
			this.myNAmeTag = name.toUpperCase();
			this.myProject = project;
			this.myConfDes = cConf;
			this.myUploader = uploader;
			this.myBoardDescriptor = boardDescriptor;
		}

		@Override
		protected IStatus run(IProgressMonitor monitor) {
			IStatus ret = Status.OK_STATUS;
			boolean WeStoppedTheComPort = false;
			try {
				String message = Messages.Upload_uploading
						.replace("{project}", myProject.getName()) //$NON-NLS-1$
						.replace("{uploader}", myNAmeTag) + ' '; //$NON-NLS-1$
				UploadSketchWrapper.this.myHighLevelStream.println(message);
				monitor.beginTask(message, 2);
				try {
					WeStoppedTheComPort = SerialManager.StopSerialMonitor(
							myBoardDescriptor.getActualUploadPort());
				} catch (Exception e) {
					ret = new Status(IStatus.WARNING, Const.CORE_PLUGIN_ID,
							Messages.Upload_Error_com_port, e);
					Common.log(ret);
				}

				IFile hexFile = myProject.getFile(myConfDes.getBuildSetting()
						.getBuilderCWD().append(myProject.getName() + ".hex")); //$NON-NLS-1$
				if (myUploader.uploadUsingPreferences(hexFile,
						this.myBoardDescriptor, monitor)) {
					UploadSketchWrapper.this.myHighLevelStream
							.println(Messages.Upload_Done);
				} else {
					UploadSketchWrapper.this.myHighLevelStream
							.println(Messages.Upload_failed_upload);
					ret = new Status(IStatus.ERROR, Const.CORE_PLUGIN_ID,
							Messages.Upload_failed_upload);
				}

			} catch (Exception e) {
				Common.log(new Status(IStatus.ERROR, Const.CORE_PLUGIN_ID,
						Messages.Upload_failed_upload, e));
			} finally {
				try {
					if (WeStoppedTheComPort) {
						SerialManager.StartSerialMonitor(
								myBoardDescriptor.getActualUploadPort());
					}
					if (UploadSketchWrapper.this.myHighLevelStream != null) {
						UploadSketchWrapper.this.myHighLevelStream.close();
					}
					if (UploadSketchWrapper.this.myErrStream != null) {
						UploadSketchWrapper.this.myErrStream.close();
					}
					if (UploadSketchWrapper.this.myOutStream != null) {
						UploadSketchWrapper.this.myOutStream.close();
					}
					UploadSketchWrapper.this.myHighLevelStream = null;
					UploadSketchWrapper.this.myErrStream = null;
					UploadSketchWrapper.this.myOutStream = null;
				} catch (Exception e) {
					ret = new Status(IStatus.WARNING, Const.CORE_PLUGIN_ID,
							Messages.Upload_Error_serial_monitor_restart, e);
					Common.log(ret);
				}
				monitor.done();
			}

			return ret;
		}
	}
}
