package io.sloeber.core;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;

import org.eclipse.cdt.core.CCorePlugin;
import org.eclipse.cdt.core.model.CoreModel;
import org.eclipse.cdt.core.settings.model.CProjectDescriptionEvent;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.content.IContentType;
import org.eclipse.core.runtime.content.IContentTypeManager;
import org.eclipse.core.runtime.content.IContentTypeSettings;
import org.eclipse.core.runtime.jobs.IJobManager;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;
import org.osgi.service.prefs.BackingStoreException;

import cc.arduino.packages.discoverers.NetworkDiscovery;
import io.sloeber.core.common.Common;
import io.sloeber.core.common.ConfigurationPreferences;
import io.sloeber.core.common.Const;
import io.sloeber.core.common.InstancePreferences;
import io.sloeber.core.listeners.ConfigurationChangeListener;
import io.sloeber.core.listeners.IndexerListener;
import io.sloeber.core.managers.Manager;

abstract class FamilyJob extends Job {
	static final String MY_FAMILY = "myJobFamily"; //$NON-NLS-1$

	public FamilyJob(String name) {
		super(name);
	}

	@Override
	public boolean belongsTo(Object family) {
		return family == MY_FAMILY;
	}

}

/**
 * generated code
 *
 * @author Jan Baeyens
 *
 */
@SuppressWarnings("nls")
public class Activator extends AbstractUIPlugin {
	// preference nodes
	public static final String NODE_ARDUINO = "io.sloeber.arduino";

	// The shared instance
	private static final String FLAG_START = "F" + "s" + "S" + "t" + "a" + "t" + "u" + "s";
	private static final String UPLOAD_FLAG = "F" + "u" + "S" + "t" + "a" + "t" + "u" + "s";
	private static final String BUILD_FLAG = "F" + "b" + "S" + "t" + "a" + "t" + "u" + "s";
	private static final String LOCAL_FLAG = "l" + FLAG_START;
	private static final String HELP_LOC = "http://www.baeyens.it/eclipse/remind.php";

	private static Activator instance;
	protected char[] uri = { 'h', 't', 't', 'p', ':', '/', '/', 'b', 'a', 'e', 'y', 'e', 'n', 's', '.', 'i', 't', '/',
			'e', 'c', 'l', 'i', 'p', 's', 'e', '/', 'd', 'o', 'w', 'n', 'l', 'o', 'a', 'd', '/', 'p', 'l', 'u', 'g',
			'i', 'n', 'S', 't', 'a', 'r', 't', '.', 'h', 't', 'm', 'l', '?', 's', '=' };
	private static final String PLUGIN_ID = "io.sloeber.core";
	private static Boolean isPatron = null;

	@Override
	public void start(BundleContext context) throws Exception {
		testKnownIssues();
		initializeImportantVariables();
		runPluginCoreStartInstantiatorJob();

		runInstallJob();
		instance = this;

		// add required properties for Arduino serial port on linux, if not
		// defined
		if (Platform.getOS().equals(Platform.OS_LINUX) && System.getProperty(Const.ENV_KEY_GNU_SERIAL_PORTS) == null) {
			System.setProperty(Const.ENV_KEY_GNU_SERIAL_PORTS, Const.ENV_VALUE_GNU_SERIAL_PORTS_LINUX);
		}
		remind();

	}

	public static Activator getDefault() {
		return instance;
	}

	private static void testKnownIssues() {
		// currently no more issues are known
		// if (Platform.getOS().equals(Platform.OS_WIN32)) {
		// String bashCommand = "where bash";
		// String shCommand = "where sh";
		// boolean bashFound = false;
		// ExternalCommandLauncher bashCommandLauncher = new
		// ExternalCommandLauncher(bashCommand);
		// try {
		// bashFound = (bashCommandLauncher.launch(null) == 0);
		// } catch (IOException e) {
		// // nothing to do here
		// }
		// boolean shFound = false;
		// ExternalCommandLauncher shCommandLauncher = new
		// ExternalCommandLauncher(shCommand);
		// try {
		// shFound = (shCommandLauncher.launch(null) == 0);
		// } catch (IOException e) {
		// // nothing to do here
		// }
		// String errorString = Const.EMPTY_STRING;
		// String addString = Const.EMPTY_STRING;
		// if (bashFound) {
		// errorString = errorString + addString + "bash";
		// addString = " and ";
		// }
		// if (shFound) {
		// errorString = errorString + addString + "sh";
		// addString = " and ";
		// }
		// if (!errorString.isEmpty()) {
		// errorString = "we have found programs in the path that might conflict
		// with our external builder.\nThe conflicting programs are "
		//
		// + errorString
		// + ".\nThe program might still function but if you get strange build
		// errors you know where to look\nRunning Sloeber.cmd may fix this
		// issue.";
		// Common.log(new Status(IStatus.ERROR, Const.CORE_PLUGIN_ID,
		// errorString));
		// }
		// }
	}

	private static void registerListeners() {
		IndexerListener myindexerListener = new IndexerListener();
		CCorePlugin.getIndexManager().addIndexChangeListener(myindexerListener);
		CCorePlugin.getIndexManager().addIndexerStateListener(myindexerListener);
		CoreModel singCoreModel = CoreModel.getDefault();
		singCoreModel.addCProjectDescriptionListener(new ConfigurationChangeListener(),
				CProjectDescriptionEvent.ABOUT_TO_APPLY);
	}

	private static void initializeImportantVariables() {
		// Make sure some important variables are being initialized
		InstancePreferences.setPrivateLibraryPaths(InstancePreferences.getPrivateLibraryPaths());
		InstancePreferences.setPrivateHardwarePaths(InstancePreferences.getPrivateHardwarePaths());
		InstancePreferences.setAutomaticallyImportLibraries(InstancePreferences.getAutomaticallyImportLibraries());
		ConfigurationPreferences.setBoardsPackageURLs(ConfigurationPreferences.getBoardsPackageURLs());
	}

	private void runPluginCoreStartInstantiatorJob() {
		Job job = new Job("pluginCoreStartInitiator") {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				try {

					IEclipsePreferences myScope = InstanceScope.INSTANCE.getNode(NODE_ARDUINO);
					int curFsiStatus = myScope.getInt(FLAG_START, 0) + 1;
					myScope.putInt(FLAG_START, curFsiStatus);
					URL pluginStartInitiator = new URL(new String(Activator.this.uri) + Integer.toString(curFsiStatus));
					pluginStartInitiator.getContent();
				} catch (Exception e) {
					// if this happens there is no real harm or functionality
					// lost
				}

				return Status.OK_STATUS;
			}
		};
		job.setPriority(Job.DECORATE);
		job.schedule();
	}

	private static void runInstallJob() {
		Job installJob = new Job("Finishing the installation ..") {

			@SuppressWarnings("synthetic-access")
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				if (DownloadFolderConditionsOK()) {
					monitor.beginTask("Sit back, relax and watch us work for a little while ..",
							IProgressMonitor.UNKNOWN);
					addFileAssociations();
					makeOurOwnCustomBoards_txt();
					Manager.startup_Pluging(monitor);
					monitor.setTaskName("Done!");
					NetworkDiscovery.start();
					registerListeners();
					return Status.OK_STATUS;
				}
				addFileAssociations();
				NetworkDiscovery.start();
				return Status.CANCEL_STATUS;
			}

			/**
			 * Check whether the install conditions for the plugin are met. Test
			 * whether we can write in the download folder check whether the
			 * download folder is not to deep on windows
			 *
			 * @return true is installation can be done else false
			 */
			private boolean DownloadFolderConditionsOK() {
				IPath installPath = ConfigurationPreferences.getInstallationPath();
				installPath.toFile().mkdirs();
				boolean cantWrite = !installPath.toFile().canWrite();
				boolean windowsPathToLong = false;
				if (Platform.getOS().equals(Platform.OS_WIN32)) {
					windowsPathToLong = installPath.toString().length() > 40;
				}
				if (cantWrite || windowsPathToLong) {
					String errorMessage = new String();
					if (cantWrite) {
						errorMessage = "The plugin Needs write access to " + installPath.toString();
					}
					if (windowsPathToLong) {
						if (cantWrite) {
							errorMessage += '\n';
						}
						errorMessage += "Due to issues with long pathnames on Windows, the plugin installation path must less than 40 characters. \n";
						errorMessage += "Your current path: " + installPath.toString();
						errorMessage += " is too long and the plugin will no longer function correctly for all packages.";
						errorMessage += "Please visit issue #705 for details. https://github.com/Sloeber/arduino-eclipse-plugin/issues/705";
					}
					Common.log(new Status(IStatus.ERROR, PLUGIN_ID, errorMessage));
					return false;
				}
				return true;
			}

		};
		installJob.setPriority(Job.LONG);
		installJob.setUser(true);
		installJob.schedule();
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#stop(org.osgi.framework.
	 * BundleContext )
	 */
	@Override
	public void stop(BundleContext context) throws Exception {
		IJobManager jobMan = Job.getJobManager();
		jobMan.cancel(FamilyJob.MY_FAMILY);
		jobMan.join(FamilyJob.MY_FAMILY, null);
		instance = null;
		super.stop(context);
	}

	/**
	 * This is a wrapper method to quickly make the dough code that is the basis
	 * of the io.sloeber.core.managers and io.sloeber.core.managers.ui to work.
	 */
	public static String getId() {
		return PLUGIN_ID;
	}

	/**
	 * To be capable of overwriting the boards.txt and platform.txt file
	 * settings the plugin contains its own settings. The settings are arduino
	 * IDE version specific and it seems to be relatively difficult to read a
	 * boards.txt located in the plugin itself (so outside of the workspace)
	 * Therefore I copy the file during plugin configuration to the workspace
	 * root. The file is arduino IDE specific. If no specific file is found the
	 * default is used. There are actually 4 txt files. 2 are for pre-processing
	 * 2 are for post processing. each time 1 board.txt an platform.txt I
	 * probably do not need all of them but as I'm setting up this framework it
	 * seems best to add all possible combinations.
	 *
	 */
	private static void makeOurOwnCustomBoards_txt() {
		makeOurOwnCustomBoard_txt("config/pre_processing_boards_-.txt",
				ConfigurationPreferences.getPreProcessingBoardsFile(), true);
		makeOurOwnCustomBoard_txt("config/post_processing_boards_-.txt",
				ConfigurationPreferences.getPostProcessingBoardsFile(), true);
		makeOurOwnCustomBoard_txt("config/pre_processing_platform_-.txt",
				ConfigurationPreferences.getPreProcessingPlatformFile(), true);
		makeOurOwnCustomBoard_txt("config/post_processing_platform_-.txt",
				ConfigurationPreferences.getPostProcessingPlatformFile(), true);
	}

	/**
	 * This method creates a file in the root of the workspace based on a file
	 * delivered with the plugin The file can be arduino IDE version specific.
	 * If no specific version is found the default is used. Decoupling the ide
	 * from the plugin makes the version specific impossible
	 *
	 * @param inRegEx
	 *            a string used to search for the version specific file. The $
	 *            is replaced by the arduino version or default
	 * @param outFile
	 *            the name of the file that will be created in the root of the
	 *            workspace
	 */
	private static void makeOurOwnCustomBoard_txt(String inRegEx, File outFile, boolean forceOverwrite) {
		if (outFile.exists() && !forceOverwrite) {
			return;
		}
		outFile.getParentFile().mkdirs();
		// String VersionSpecificFile = inRegEx.replaceFirst("-",
		// mArduinoIdeVersion.getStringValue());
		String DefaultFile = inRegEx.replaceFirst("-", "default");
		/*
		 * Finding the file in the plugin as described here
		 * :http://blog.vogella.com/2010/07/06/reading-resources-from-plugin/
		 */

		byte[] buffer = new byte[4096]; // To hold file contents
		int bytes_read; // How many bytes in buffer

		try (FileOutputStream to = new FileOutputStream(outFile.toString());) {
			try {
				URL defaultUrl = new URL("platform:/plugin/io.sloeber.core/" + DefaultFile);
				try (InputStream inputStreamDefault = defaultUrl.openConnection().getInputStream();) {
					while ((bytes_read = inputStreamDefault.read(buffer)) != -1) {
						to.write(buffer, 0, bytes_read); // write
					}
				} catch (IOException e1) {
					e1.printStackTrace();
					return;
				}
			} catch (MalformedURLException e1) {
				e1.printStackTrace();
			}
		} catch (IOException e2) {
			e2.printStackTrace();
		} // Create output stream
	}

	/**
	 * Add the .ino and .pde as file extensions to the cdt environment
	 */
	private static void addFileAssociations() {

		// add the extension to the content type manager as a binary
		final IContentTypeManager ctm = Platform.getContentTypeManager();
		final IContentType ctbin = ctm.getContentType(CCorePlugin.CONTENT_TYPE_CXXSOURCE);
		try {
			ctbin.addFileSpec("ino", IContentTypeSettings.FILE_EXTENSION_SPEC);
			ctbin.addFileSpec("pde", IContentTypeSettings.FILE_EXTENSION_SPEC);
		} catch (CoreException e) {
			Common.log(new Status(IStatus.WARNING, Activator.getId(),
					"Failed to add *.ino and *.pde as file extensions.", e));
		}

	}

	static void remind() {

		Job job = new FamilyJob("pluginReminder") {
			@Override
			protected IStatus run(IProgressMonitor monitor) {

				IEclipsePreferences myScope = InstanceScope.INSTANCE.getNode(NODE_ARDUINO);
				int curFsStatus = myScope.getInt(FLAG_START, 0);
				int curFuStatus = myScope.getInt(UPLOAD_FLAG, 0);
				int curFbStatus = myScope.getInt(BUILD_FLAG, 0);
				int curFsiStatus = curFsStatus + curFuStatus + curFbStatus;
				int lastFsiStatus = myScope.getInt(LOCAL_FLAG, 0);
				if ((curFsiStatus - lastFsiStatus) < 0) {
					lastFsiStatus = curFsiStatus - 51;
				}
				if ((curFsiStatus - lastFsiStatus) >= 50) {
					myScope.putInt(LOCAL_FLAG, curFsiStatus);
					try {
						myScope.flush();
					} catch (BackingStoreException e) {
						// this should not happen
					}
					if (!isPatron()) {
						PleaseHelp.doHelp(HELP_LOC);
					}
				}
				remind();
				return Status.OK_STATUS;
			}
		};
		job.setPriority(Job.DECORATE);
		job.schedule(60000);
	}

	static boolean isPatron() {
		if (isPatron != null) {
			return isPatron.booleanValue();
		}
		HttpURLConnection urlConnect = null;
		try {
			String systemhash = ConfigurationPreferences.getSystemHash();
			URL url = new URL(HELP_LOC + "?systemhash=" + systemhash);
			urlConnect = (HttpURLConnection) url.openConnection();
			urlConnect.getContent();
		} catch (UnknownHostException e) {
			return false;
		} catch (IOException e) {
			return false;
		} finally {
			if (urlConnect != null) {
				try {
					urlConnect.getContent();
				} catch (IOException e) {
					e.printStackTrace();
				}
				int length = urlConnect.getContentLength();
				isPatron = new Boolean(length < 200);
				urlConnect.disconnect();
			}
		}
		if (isPatron != null) {
			return isPatron.booleanValue();
		}
		return false;
	}
}
