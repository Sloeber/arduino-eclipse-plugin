/*******************************************************************************
 * 
 * Copyright (c) 2008, 2010 Thomas Holland (thomas@innot.de) and others
 * 
 * This program and the accompanying materials are made
 * available under the terms of the GNU Public License v3
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 * 
 * Contributors:
 *     Thomas Holland - initial API and implementation
 *     
 * $Id: UploadProjectAction.java 851 2010-08-07 19:37:00Z innot $
 *     
 *******************************************************************************/
package it.baeyens.avreclipse.ui.actions;


import it.baeyens.arduino.common.ArduinoConst;
import it.baeyens.arduino.common.Common;
import it.baeyens.avreclipse.AVRPlugin;
import it.baeyens.avreclipse.core.avrdude.AVRDudeAction;
import it.baeyens.avreclipse.core.avrdude.AVRDudeException;
import it.baeyens.avreclipse.core.avrdude.AVRDudeSchedulingRule;
import it.baeyens.avreclipse.core.avrdude.BaseBytesProperties;
import it.baeyens.avreclipse.core.avrdude.ProgrammerConfig;
import it.baeyens.avreclipse.core.properties.AVRDudeProperties;
import it.baeyens.avreclipse.core.properties.AVRProjectProperties;
import it.baeyens.avreclipse.core.properties.ProjectPropertyManager;
import it.baeyens.avreclipse.core.toolinfo.AVRDude;
import it.baeyens.avreclipse.core.toolinfo.fuses.FuseType;
import it.baeyens.avreclipse.core.util.AVRMCUidConverter;
import it.baeyens.avreclipse.mbs.BuildMacro;
import it.baeyens.avreclipse.ui.dialogs.AVRDudeErrorDialogJob;

import java.io.File;
import java.text.MessageFormat;
import java.util.List;

import org.eclipse.cdt.core.model.ICElement;
import org.eclipse.cdt.core.model.ICProject;
import org.eclipse.cdt.managedbuilder.core.IConfiguration;
import org.eclipse.cdt.managedbuilder.core.IManagedBuildInfo;
import org.eclipse.cdt.managedbuilder.core.ManagedBuildManager;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IActionDelegate;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.ActionDelegate;
import org.eclipse.ui.console.MessageConsole;
import org.eclipse.ui.progress.UIJob;


/**
 * @author Thomas Holland
 * @since 2.2
 * @since 2.3 Added optional delay between avrdude invocations
 * 
 */
public class UploadProjectAction extends ActionDelegate implements IWorkbenchWindowActionDelegate {

	private final static String	TITLE_UPLOAD			= "AVRDude Upload";

	private final static String	SOURCE_BUILDCONFIG		= "active build configuration";
	private final static String	SOURCE_PROJECT			= "project";

	private final static String	MSG_NOPROJECT			= "No AVR project selected";

	private final static String	MSG_NOPROGRAMMER		= "No Programmer has been set for the {0}.\n\n"
																+ "Please select a Programmer in the project properties\n"
																+ "(Properties -> AVRDude -> Programmer)";

	private final static String	MSG_WRONGMCU			= "AVRDude does not support the project target MCU [{0}]\n\n"
																+ "Please select a different target MCU if you want to use AVRDude.\n"
																+ "(Properties -> Target Hardware)";

	private final static String	MSG_NOACTIONS			= "The {0} has no options set to upload anything to the device.\n\n"
																+ "Please select at least one item to upload (flash / eeprom / fuses / lockbits)";

	private final static String	MSG_MISSING_FILE		= "The file [{0}] for the {1} memory does not exist or is not readable\n\n"
																+ "Maybe the project needs to be build first.";

	private final static String	MSG_MISSING_FUSES_FILE	= "The selected {0} file [{1}] does not exist or is not readable\n\n"
																+ "Please select a different {0} source.\n"
																+ "(Properties -> AVRDude -> {0}";

	private final static String	MSG_INVALIDFUSEBYTE		= "The {0} byte(s) to upload are for an {1} MCU, "
																+ "which is not compatible with the {3} target MCU [{2}]\n\n"
																+ "Please check the fuse byte settings.\n"
																+ "(Properties -> AVRDude -> {0})";

	private IProject			fProject;

	/**
	 * Constructor for this Action.
	 */
	public UploadProjectAction() {
		super();
	}

	/**
	 * @see IActionDelegate#selectionChanged(IAction, ISelection)
	 */
	@Override
	public void selectionChanged(IAction action, ISelection selection) {

		// The user has selected a different Workbench object.
		// If it is an IProject we keep it.

		Object item;

		if (selection instanceof IStructuredSelection) {
			item = ((IStructuredSelection) selection).getFirstElement();
		} else {
			return;
		}
		if (item == null) {
			return;
		}
		IProject project = null;

		// See if the given is an IProject (directly or via IAdaptable)
		if (item instanceof IProject) {
			project = (IProject) item;
		} else if (item instanceof IResource) {
			project = ((IResource) item).getProject();
		} else if (item instanceof IAdaptable) {
			IAdaptable adaptable = (IAdaptable) item;
			project = (IProject) adaptable.getAdapter(IProject.class);
			if (project == null) {
				// Try ICProject -> IProject
				ICProject cproject = (ICProject) adaptable.getAdapter(ICProject.class);
				if (cproject == null) {
					// Try ICElement -> ICProject -> IProject
					ICElement celement = (ICElement) adaptable.getAdapter(ICElement.class);
					if (celement != null) {
						cproject = celement.getCProject();
					}
				}
				if (cproject != null) {
					project = cproject.getProject();
				}
			}
		}

		fProject = project;
	}

	/**
	 * @see IActionDelegate#run(IAction)
	 */
	@Override
	public void run(IAction action) {

		// Check that we have a AVR Project
		try {
			if (fProject == null || !fProject.hasNature(ArduinoConst.AVRnatureid)) {
				MessageDialog.openError(getShell(), TITLE_UPLOAD, MSG_NOPROJECT);
				return;
			}
		} catch (CoreException e) {
			// Log the Exception
			AVRPlugin.getDefault().log( new Status(Status.ERROR, ArduinoConst.CORE_PLUGIN_ID,
					"Can't access project nature", e));
		}

		// Get the active build configuration
		IManagedBuildInfo bi = ManagedBuildManager.getBuildInfo(fProject);
		IConfiguration activecfg = bi.getDefaultConfiguration();

		// Get the avr properties for the active configuration
		AVRProjectProperties targetprops = ProjectPropertyManager.getPropertyManager(fProject)
				.getActiveProperties();

		// Check if the avrdude properties are valid.
		// if not the checkProperties() method will display an error message box
		if (!checkProperties(activecfg, targetprops)) {
			return;
		}

		// Everything is fine -> run avrdude
		runAVRDude(activecfg, targetprops);
	}

	/**
	 * Check that the current properties are valid.
	 * <p>
	 * This method will check that:
	 * <ul>
	 * <li>there has been a Programmer selected</li>
	 * <li>avrdude supports the selected MCU</li>
	 * <li>there are some actions to perform</li>
	 * <li>all source files exist</li>
	 * <li>the fuse bytes (if uploaded) are valid for the target MCU</li>
	 * </ul>
	 * <p>
	 * 
	 * @param buildcfg
	 *            The current build configuration
	 * @param props
	 *            The current Properties
	 * @return <code>true</code> if everything is OK.
	 */
	private boolean checkProperties(IConfiguration buildcfg, AVRProjectProperties props) {

		boolean perconfig = ProjectPropertyManager.getPropertyManager(fProject).isPerConfig();
		String source = perconfig ? SOURCE_BUILDCONFIG : SOURCE_PROJECT;

		// Check that a Programmer has been selected
		if (props.getAVRDudeProperties().getProgrammer() == null) {
			String message = MessageFormat.format(MSG_NOPROGRAMMER, source);
			MessageDialog.openError(getShell(), TITLE_UPLOAD, message);
			return false;
		}

		// Check that the MCU is valid for avrdude
		String mcuid = props.getMCUId();
		if (!AVRDude.getDefault().hasMCU(mcuid)) {
			String message = MessageFormat.format(MSG_WRONGMCU, AVRMCUidConverter.id2name(mcuid));
			MessageDialog.openError(getShell(), TITLE_UPLOAD, message);
			return false;
		}

		// Check that there is actually anything to upload
		List<String> actionlist = props.getAVRDudeProperties().getActionArguments(buildcfg, true);
		if (actionlist.size() == 0) {
			String message = MessageFormat.format(MSG_NOACTIONS, source);
			MessageDialog.openWarning(getShell(), TITLE_UPLOAD, message);
			return false;
		}

		// Check all referenced files
		// It would be cumbersome to go through all possible cases. Instead we
		// convert all action arguments back to AVRDudeActions and get the
		// filename from it.
		IPath invalidfile = null;
		String formemtype = null;
		for (String argument : actionlist) {
			AVRDudeAction action = AVRDudeAction.getActionForArgument(argument);
			String filename = action.getFilename();
			if (filename == null)
				continue;
			IPath rawfile = new Path(filename);
			IPath unresolvedfile = rawfile;
			IPath resolvedfile = rawfile;
			if (!rawfile.isAbsolute()) {
				// The filename is relative to the build folder. Get the build
				// folder and append our filename. Then resolve any macros
				unresolvedfile = buildcfg.getBuildData().getBuilderCWD().append(rawfile);
				resolvedfile = new Path(BuildMacro.resolveMacros(buildcfg, unresolvedfile
						.toString()));
			}
			File realfile = resolvedfile.toFile();
			if (!realfile.canRead()) {
				invalidfile = unresolvedfile;
				formemtype = action.getMemType().toString();
				break;
			}
		}
		if (invalidfile != null) {
			String message = MessageFormat.format(MSG_MISSING_FILE, invalidfile.toString(),
					formemtype);
			MessageDialog.openError(getShell(), TITLE_UPLOAD, message);
			return false;
		}

		// Check that the fuses and locks are valid (if they are to be uploaded)
		for (FuseType type : FuseType.values()) {

			if (props.getAVRDudeProperties().getBytesProperties(type, buildcfg).getWrite()) {
				BaseBytesProperties bytesproperties = props.getAVRDudeProperties()
						.getBytesProperties(type, buildcfg);
				if (bytesproperties.getMCUId() == null) {
					// A non-existing file has been selected as source for the fuses
					String message = MessageFormat.format(MSG_MISSING_FUSES_FILE, type.toString(),
							bytesproperties.getFileNameResolved());
					MessageDialog.openError(getShell(), TITLE_UPLOAD, message);
					return false;
				}
				if (!bytesproperties.isCompatibleWith(props.getMCUId())) {
					String fusesmcuid = AVRMCUidConverter.id2name(bytesproperties.getMCUId());
					String propsmcuid = AVRMCUidConverter.id2name(props.getMCUId());
					String message = MessageFormat.format(MSG_INVALIDFUSEBYTE, type.toString(),
							fusesmcuid, propsmcuid, source);
					MessageDialog.openError(getShell(), TITLE_UPLOAD, message);
					return false;
				}
			}
		}

		// Everything is OK
		return true;
	}

	/**
	 * Start the AVRDude UploadJob.
	 * 
	 * @param buildcfg
	 *            The build configuration for resolving macros.
	 * @param props
	 *            The AVR properties for the project / the current configuration
	 */
	private void runAVRDude(IConfiguration buildcfg, AVRProjectProperties props) {

		AVRDudeProperties avrdudeprops = props.getAVRDudeProperties();

		// get the list of normal (non-action) arguments
		List<String> optionargs = avrdudeprops.getArguments();

		// get a list of actions
		List<String> actionargs = avrdudeprops.getActionArguments(buildcfg, true);

		// Get the ProgrammerConfig in case we need to display an error
		// message
		ProgrammerConfig programmer = avrdudeprops.getProgrammer();

		// Set the working directory to the CWD of the active build config, so that
		// relative paths are resolved correctly.
		IPath cwdunresolved = buildcfg.getBuildData().getBuilderCWD();
		IPath cwd = new Path(BuildMacro.resolveMacros(buildcfg, cwdunresolved.toString()));
		
		// Modified by Jan Baeyens to handle to com port better

		//Job uploadjob = new UploadJob(optionargs, actionargs, cwd, programmer); Original code
		Job uploadjob = new UploadJob(optionargs, actionargs, cwd, programmer,avrdudeprops.getProgrammer().getPort());
        //end of modified by Jan Baeyens


		uploadjob.setRule(new AVRDudeSchedulingRule(programmer));
		uploadjob.setPriority(Job.LONG);
		uploadjob.setUser(true);
		


		uploadjob.schedule();

	}

	/**
	 * The background Job to execute the requested avrdude commands.
	 * 
	 */
	private class UploadJob extends Job {

		private final List<String>		fOptions;
		private final List<String>		fActions;
		private final IPath				fCwd;
		private final ProgrammerConfig	fProgrammerConfig;
		//private final String			MComPort;
		private String			MComPort;

		public UploadJob(List<String> options, List<String> actions, IPath cwd,
				ProgrammerConfig programmer,String Port) {
			super("AVRDude Upload");
			fOptions = options;
			fActions = actions;
			fCwd = cwd;
			fProgrammerConfig = programmer;
			MComPort=Port;
		}

		@Override
		public IStatus run(IProgressMonitor monitor) {

			try {
				monitor.beginTask("Running AVRDude", fActions.size());

				// init console. Clears the console and puts it on top.
				// AVRDude is forced to use the console, so the user will always
				// see the output, regardless of the "use console" flag.
				MessageConsole console = AVRPlugin.getDefault().getConsole("AVRDude");
				console.clearConsole();
				console.activate();

				AVRDude avrdude = AVRDude.getDefault();

				// Append all requested actions
				// The reason this is done here is because in earlier versions
				// of the plugin each action was send separately to avrdude to better
				// track the progress.
				// However some users complained that this slows the whole upload process down.
				// So now we sent all actions in one go, as the user can monitor the progress
				// in the console anyway.
				fOptions.addAll(fActions);
				monitor.subTask("Running AVRDude");
				
				// Inserted by Jan Baeyens to toggle the DTR port to rest arduino
				// I also added code to stop and restart the com port but the functions itself are not yet implemented
				Boolean WeStoppedTheComPort = false;
				try
				{
				WeStoppedTheComPort= Common.StopSerialMonitor( MComPort);
				String NewComPort = Common.ResetArduino( fProject, MComPort, 9600 );
				
				int index = fOptions.indexOf(Common.UploadPortPrefix() + MComPort);
				fOptions.set(index, Common.UploadPortPrefix() + NewComPort);
				}
				catch ( Exception e)
				{
					AVRPlugin.getDefault().log( new Status(Status.WARNING, ArduinoConst.CORE_PLUGIN_ID,
							"Failed to handle Com port properly", e));
				}
				//end of inserted by Jan Baeyens

				// Now avrdude can be started.
				avrdude.runCommand(fOptions, new SubProgressMonitor(monitor, 1), true, fCwd,
						fProgrammerConfig);
				
				// Inserted by Jan Baeyens to toggle the DTR port to rest arduino
				// I also added code to stop and restart the com port but the functions itself are not yet implmented
				try
				{
				if (WeStoppedTheComPort) 
				{
					Common.StartSerialMonitor(MComPort);
				}
				}
				catch ( Exception e)
				{
					AVRPlugin.getDefault().log( new Status(Status.WARNING, ArduinoConst.CORE_PLUGIN_ID,
							"Failed to restart serial monitor", e));					
				}
		        //end of inserted by Jan Baeyens
				
			} catch (AVRDudeException ade) {
				// Show an Error message and exit
				Display display = PlatformUI.getWorkbench().getDisplay();
				if (display != null && !display.isDisposed()) {
					UIJob messagejob = new AVRDudeErrorDialogJob(display, ade, fProgrammerConfig
							.getId());
					messagejob.setPriority(Job.INTERACTIVE);
					messagejob.schedule();
					try {
						messagejob.join(); // block until the dialog is closed.
					} catch (InterruptedException e) {
						// Don't care if the dialog is interrupted from outside.
					}
				}
			} finally {
				monitor.done();
			}

			return Status.OK_STATUS;
		}
	}

	/**
	 * Get the current Shell.
	 * 
	 * @return <code>Shell</code> of the active Workbench window.
	 */
	private Shell getShell() {
		return PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.IWorkbenchWindowActionDelegate#init(org.eclipse.ui.IWorkbenchWindow)
	 */
	@Override
	public void init(IWorkbenchWindow window) {
		// TODO Auto-generated method stub

	}

}
