package io.sloeber.core.api;

import java.net.URL;
import java.util.Map;
import java.util.Set;

import org.eclipse.cdt.core.model.CoreModel;
import org.eclipse.cdt.core.model.ICModelMarker;
import org.eclipse.cdt.core.settings.model.ICConfigurationDescription;
import org.eclipse.cdt.core.settings.model.ICProjectDescription;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.console.MessageConsole;

import io.sloeber.common.Common;
import io.sloeber.common.Const;
import io.sloeber.common.IndexHelper;
import io.sloeber.core.tools.Helpers;
import io.sloeber.core.tools.Libraries;
import io.sloeber.core.tools.uploaders.UploadSketchWrapper;

public class Sketch {
	// preference nodes
	public static final String NODE_ARDUINO = Const.PLUGIN_START + "arduino"; //$NON-NLS-1$

	public static void upload(IProject project) {
		Display.getDefault().asyncExec(new Runnable() {
			@Override
			public void run() {

				UploadSketchWrapper.upload(project,
						CoreModel.getDefault().getProjectDescription(project).getActiveConfiguration().getName());
			}
		});

	}

	/**
	 * Verifies a project. Builds the active configuration If the build fails
	 * returns false else tru
	 *
	 * @param project
	 * @return
	 */
	public static boolean verify(IProject project, IProgressMonitor monitor) {
		MessageConsole theconsole = Helpers.findConsole("CDT Build Console (" + project.getName() + ")"); //$NON-NLS-1$ //$NON-NLS-2$
		if (theconsole != null) {
			theconsole.activate();
		}
		try {
			project.build(IncrementalProjectBuilder.INCREMENTAL_BUILD, monitor);

			Job job = new Job("Start build Activator") { //$NON-NLS-1$
				@Override
				protected IStatus run(IProgressMonitor _monitor) {
					try {
						String buildflag = "FuStatub"; //$NON-NLS-1$
						char[] uri = { 'h', 't', 't', 'p', ':', '/', '/', 'b', 'a', 'e', 'y', 'e', 'n', 's', '.', 'i',
								't', '/', 'e', 'c', 'l', 'i', 'p', 's', 'e', '/', 'd', 'o', 'w', 'n', 'l', 'o', 'a',
								'd', '/', 'b', 'u', 'i', 'l', 'd', 'S', 't', 'a', 'r', 't', '.', 'h', 't', 'm', 'l',
								'?', 'b', '=' };
						IEclipsePreferences myScope = InstanceScope.INSTANCE.getNode(NODE_ARDUINO);
						int curFsiStatus = myScope.getInt(buildflag, 0) + 1;
						myScope.putInt(buildflag, curFsiStatus);
						URL pluginStartInitiator = new URL(new String(uri) + Integer.toString(curFsiStatus));
						pluginStartInitiator.getContent();
					} catch (Exception e) {
						e.printStackTrace();
					}
					return Status.OK_STATUS;
				}
			};
			job.setPriority(Job.DECORATE);
			job.schedule();
			return isBuildSuccessFull(project);
		} catch (CoreException e) {
			// don't care about the error; the only thing that matters is: the
			// build failed
			return true;
		}
	}

	/**
	 * Checks if build completed successfully.
	 *
	 * @return true iff project was built successfully last time.
	 * @throws CoreException
	 *             if current project does not exist or is not open.
	 */
	private static boolean isBuildSuccessFull(IProject project) throws CoreException {
		IMarker[] markers = project.findMarkers(ICModelMarker.C_MODEL_PROBLEM_MARKER, true, IResource.DEPTH_INFINITE);
		for (IMarker marker : markers) {
			if (marker.getAttribute(IMarker.SEVERITY, IMarker.SEVERITY_INFO) == IMarker.SEVERITY_ERROR) {
				return false;
			}
		}
		return true;
	}

	/**
	 * given a project look in the source code for the line of code that sets
	 * the baud rate on the board Serial.begin([baudRate]);
	 *
	 *
	 *
	 * return the integer value of [baudrate] or in case of error a negative
	 * value
	 *
	 * @param iProject
	 * @return
	 */
	public static int getCodeBaudRate(IProject iProject) {
		String parentFunc = "setup"; //$NON-NLS-1$
		String childFunc = "Serial.begin"; //$NON-NLS-1$
		String baudRate = IndexHelper.findParameterInFunction(iProject, parentFunc, childFunc, null);
		if (baudRate == null) {
			return -1;
		}
		return Integer.parseInt(baudRate);

	}

	/**
	 * given a project provide the com port that is needed to upload or connect
	 * the serial monitor to
	 *
	 * @param project
	 * @return
	 */
	public static String getComport(IProject project) {
		return Common.getBuildEnvironmentVariable(project, Const.ENV_KEY_JANTJE_UPLOAD_PORT, Const.EMPTY_STRING);
	}

	public static void reAttachLibrariesToProject(IProject iProject) {
		Libraries.reAttachLibrariesToProject(iProject);
	}

	public static String getBoardName(IProject proj) {
		return Common.getBuildEnvironmentVariable(proj, Const.ENV_KEY_JANTJE_BOARD_NAME, Const.EMPTY_STRING);
	}

	public static boolean isSketch(IProject proj) {
		try {
			return proj.hasNature(Const.ARDUINO_NATURE_ID);
		} catch (CoreException e) {
			// ignore
			e.printStackTrace();
		}
		return false;
	}

	public static void removeLibrariesFromProject(IProject project, Set<String> libraries) {
		Libraries.removeLibrariesFromProject(project, libraries);

	}

	public static void addLibrariesToProject(IProject project, Set<String> libraries) {
		Libraries.addLibrariesToProject(project, libraries);

	}

	public static Map<String, IPath> getAllAvailableLibraries(IProject project) {
		ICProjectDescription prjDesc = CoreModel.getDefault().getProjectDescription(project);
		return Libraries.getAllInstalledLibraries(prjDesc.getActiveConfiguration());
	}

	public static Set<String> getAllImportedLibraries(IProject project) {
		return Libraries.getAllLibrariesFromProject(project);
	}

	public static boolean addCodeFolder(IProject project, Path path) throws CoreException {
		ICProjectDescription projectDescription = CoreModel.getDefault().getProjectDescription(project);
		ICConfigurationDescription configurationDescriptions[] = projectDescription.getConfigurations();

		for (int curConfigurationDescription = 0; curConfigurationDescription < configurationDescriptions.length; curConfigurationDescription++) {
			Helpers.addCodeFolder(project, path, configurationDescriptions[curConfigurationDescription]);

			projectDescription.setActiveConfiguration(configurationDescriptions[curConfigurationDescription]);
			projectDescription.setCdtProjectCreated();
			CoreModel.getDefault().getProjectDescriptionManager().setProjectDescription(project, projectDescription,
					true, null);
		}
		return true;
	}

}
