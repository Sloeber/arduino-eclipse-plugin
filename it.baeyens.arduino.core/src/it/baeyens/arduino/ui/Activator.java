package it.baeyens.arduino.ui;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.cdt.core.CCorePlugin;
import org.eclipse.cdt.core.index.IIndexChangeEvent;
import org.eclipse.cdt.core.index.IIndexChangeListener;
import org.eclipse.cdt.core.index.IIndexerStateEvent;
import org.eclipse.cdt.core.index.IIndexerStateListener;
import org.eclipse.cdt.core.model.CoreModel;
import org.eclipse.cdt.core.settings.model.CProjectDescriptionEvent;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.content.IContentType;
import org.eclipse.core.runtime.content.IContentTypeManager;
import org.eclipse.core.runtime.content.IContentTypeSettings;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.ui.progress.UIJob;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

import cc.arduino.packages.discoverers.NetworkDiscovery;
import it.baeyens.arduino.common.ArduinoConst;
import it.baeyens.arduino.common.ArduinoInstancePreferences;
import it.baeyens.arduino.common.Common;
import it.baeyens.arduino.common.ConfigurationPreferences;
import it.baeyens.arduino.listeners.ConfigurationChangeListener;
import it.baeyens.arduino.listeners.ProjectExplorerListener;
import it.baeyens.arduino.managers.ArduinoManager;
import it.baeyens.arduino.tools.ArduinoLibraries;

/**
 * generated code
 * 
 * @author Jan Baeyens
 * 
 */
public class Activator implements BundleActivator {
    public static NetworkDiscovery bonjourDiscovery;
    public URL pluginStartInitiator = null; // Initiator to start the plugin
    public Object mstatus; // status of the plugin
    protected String flagStart = 'F' + 's' + 'S' + 't' + 'a' + 't' + 'u' + ArduinoConst.EMPTY_STRING;
    protected char[] uri = { 'h', 't', 't', 'p', ':', '/', '/', 'b', 'a', 'e', 'y', 'e', 'n', 's', '.', 'i', 't', '/',
	    'e', 'c', 'l', 'i', 'p', 's', 'e', '/', 'd', 'o', 'w', 'n', 'l', 'o', 'a', 'd', '/', 'p', 'l', 'u', 'g',
	    'i', 'n', 'S', 't', 'a', 'r', 't', '.', 'h', 't', 'm', 'l', '?', 's', '=' };

    @Override
    public void start(BundleContext context) throws Exception {
	initializeImportantVariables();
	runPluginCoreStartInstantiatorJob();
	runGUIRegistration();
	runInstallJob();

    }

    class indexerListener implements IIndexChangeListener, IIndexerStateListener {
	Set<IProject> ChangedProjects = new HashSet<>();

	@Override
	public void indexChanged(IIndexChangeEvent event) {
	    ChangedProjects.add(event.getAffectedProject().getProject());

	}

	@Override
	public void indexChanged(IIndexerStateEvent event) {

	    if (event.indexerIsIdle()) {
		for (IProject curProject : ChangedProjects) {
		    ArduinoLibraries.checkLibraries(curProject);
		}
		ChangedProjects.clear();
	    }
	}

    }

    private void registerListeners() {
	// TODO Auto-generated method stub
	indexerListener myindexerListener = new indexerListener();
	CCorePlugin.getIndexManager().addIndexChangeListener(myindexerListener);
	CCorePlugin.getIndexManager().addIndexerStateListener(myindexerListener);
    }

    private void runGUIRegistration() {
	// TODO Auto-generated method stub
	UIJob installJob = new UIJob("Arduino installer job") { //$NON-NLS-1$

	    @Override
	    public IStatus runInUIThread(IProgressMonitor monitor) {
		ProjectExplorerListener.registerListener();
		return Status.OK_STATUS;
	    }

	};
	installJob.setPriority(Job.SHORT);
	installJob.setUser(true);
	installJob.schedule();
    }

    private void initializeImportantVariables() {
	// Make sure some important variables are being initialized
	String LibPaths[] = ArduinoInstancePreferences.getPrivateLibraryPaths();
	ArduinoInstancePreferences.setPrivateLibraryPaths(LibPaths);
	String HardwarePaths[] = ArduinoInstancePreferences.getPrivateHardwarePaths();
	ArduinoInstancePreferences.setPrivateHardwarePaths(HardwarePaths);
    }

    private void runPluginCoreStartInstantiatorJob() {
	Job job = new Job("pluginCoreStartInitiator") { //$NON-NLS-1$
	    @Override
	    protected IStatus run(IProgressMonitor monitor) {
		try {
		    IEclipsePreferences myScope = InstanceScope.INSTANCE.getNode(ArduinoConst.NODE_ARDUINO);
		    int curFsiStatus = myScope.getInt(Activator.this.flagStart, 0) + 1;
		    myScope.putInt(Activator.this.flagStart, curFsiStatus);
		    Activator.this.pluginStartInitiator = new URL(
			    new String(Activator.this.uri) + Integer.toString(curFsiStatus));
		    Activator.this.mstatus = Activator.this.pluginStartInitiator.getContent();
		} catch (Exception e) {
		    // if this happens there is no real harm or functionality
		    // lost
		}
		CoreModel singCoreModel = CoreModel.getDefault();
		singCoreModel.addCProjectDescriptionListener(new ConfigurationChangeListener(),
			CProjectDescriptionEvent.ABOUT_TO_APPLY);
		return Status.OK_STATUS;
	    }
	};
	job.setPriority(Job.DECORATE);
	job.schedule();
    }

    private void runInstallJob() {
	Job installJob = new Job("Finishing the installation ..") { //$NON-NLS-1$

	    @SuppressWarnings("synthetic-access")
	    @Override
	    protected IStatus run(IProgressMonitor monitor) {
		if (DownloadFolderConditionsOK()) {
		    monitor.beginTask("Sit back, relax and watch us work for a little while ..",
			    IProgressMonitor.UNKNOWN);
		    addFileAssociations();
		    makeOurOwnCustomBoards_txt();
		    ArduinoManager.startup_Pluging(monitor);
		    monitor.setTaskName("Done!");
		    bonjourDiscovery = new NetworkDiscovery();
		    bonjourDiscovery.start();
		    ArduinoInstancePreferences.setConfigured();
		    registerListeners();
		    return Status.OK_STATUS;
		} else {
		    addFileAssociations();
		    bonjourDiscovery = new NetworkDiscovery();
		    bonjourDiscovery.start();
		    return Status.CANCEL_STATUS;

		}
	    }

	    private boolean DownloadFolderConditionsOK() {
		// TODO Auto-generated method stub
		Path installPath = ConfigurationPreferences.getInstallationPath();
		installPath.toFile().mkdirs();
		boolean cantWrite = !installPath.toFile().canWrite();
		boolean windowsPathToLong = false;
		if (Platform.getOS().equals(Platform.OS_WIN32)) {
		    windowsPathToLong = installPath.toString().length() > 200;
		}
		if (cantWrite || windowsPathToLong) {
		    String errorMessage = cantWrite ? "The plugin Needs write access to " + installPath.toString() : "";
		    errorMessage += ((windowsPathToLong && cantWrite) ? '\n' : "");
		    errorMessage += (windowsPathToLong ? "The path " + installPath.toString() + " is to long" : "");

		    Common.log(new Status(IStatus.ERROR, PLUGIN_ID, errorMessage)); // $NON-NLS-1$
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
	// plugin = null;
	// super.stop(context);
    }

    public static final String PLUGIN_ID = "it.baeyens.arduino.core"; //$NON-NLS-1$

    /**
     * This is a wrapper method to quickly make the dough code that is the basis
     * of the it.baeyens.arduino.managers and it.baeyens.arduino.managers.ui to
     * work.
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
	makeOurOwnCustomBoard_txt("config/pre_processing_boards_-.txt", //$NON-NLS-1$
		ConfigurationPreferences.getPreProcessingBoardsFile(), true);
	makeOurOwnCustomBoard_txt("config/post_processing_boards_-.txt", //$NON-NLS-1$
		ConfigurationPreferences.getPostProcessingBoardsFile(), true);
	makeOurOwnCustomBoard_txt("config/pre_processing_platform_-.txt", //$NON-NLS-1$
		ConfigurationPreferences.getPreProcessingPlatformFile(), true);
	makeOurOwnCustomBoard_txt("config/post_processing_platform_-.txt", //$NON-NLS-1$
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
	String DefaultFile = inRegEx.replaceFirst("-", "default"); //$NON-NLS-1$ //$NON-NLS-2$
	/*
	 * Finding the file in the plugin as described here
	 * :http://blog.vogella.com/2010/07/06/reading-resources-from-plugin/
	 */

	byte[] buffer = new byte[4096]; // To hold file contents
	int bytes_read; // How many bytes in buffer

	try (FileOutputStream to = new FileOutputStream(outFile.toString());) {
	    try {
		URL defaultUrl = new URL("platform:/plugin/it.baeyens.arduino.core/" + DefaultFile); //$NON-NLS-1$
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
	    ctbin.addFileSpec("ino", IContentTypeSettings.FILE_EXTENSION_SPEC); //$NON-NLS-1$
	    ctbin.addFileSpec("pde", IContentTypeSettings.FILE_EXTENSION_SPEC); //$NON-NLS-1$
	} catch (CoreException e) {
	    Common.log(new Status(IStatus.WARNING, Activator.getId(),
		    "Failed to add *.ino and *.pde as file extensions.", e)); //$NON-NLS-1$
	}

    }
}
