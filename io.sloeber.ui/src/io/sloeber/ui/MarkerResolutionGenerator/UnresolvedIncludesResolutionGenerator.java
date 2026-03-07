package io.sloeber.ui.MarkerResolutionGenerator;

import static io.sloeber.core.api.Const.*;
import static io.sloeber.ui.Messages.*;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import org.eclipse.cdt.core.CCorePlugin;
import org.eclipse.cdt.core.settings.model.ICProjectDescription;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.ui.IMarkerResolution;
import org.eclipse.ui.IMarkerResolutionGenerator;

import io.sloeber.arduinoFramework.api.IArduinoLibraryVersion;
import io.sloeber.arduinoFramework.api.LibraryManager;
import io.sloeber.core.api.ConfigurationPreferences;
import io.sloeber.core.api.ISloeberConfiguration;


public class UnresolvedIncludesResolutionGenerator implements IMarkerResolutionGenerator {

	@Override
	public IMarkerResolution[] getResolutions(IMarker marker) {
		IMarkerResolution[] ret = new IMarkerResolution[0];
		try {
			Map<String, Object> attributes = marker.getAttributes();
			Object severityObject = attributes.get("severity"); //$NON-NLS-1$
			if (!(severityObject instanceof Integer)) {
				return ret;
			}
			int severity = ((Integer) severityObject).intValue();
			if (severity != 2) {
				return ret;
			}
			Object messageObject = attributes.get("message"); //$NON-NLS-1$
			if (!(messageObject instanceof String)) {
				return ret;
			}
			String message = (String) messageObject;
			if (!message.endsWith(": No such file or directory")) { //$NON-NLS-1$
				return ret;
			}
			String libName = message.split(COLON)[1].trim();
			if (libName.isBlank()) {
				return ret;
			}
			if (!libName.endsWith(".h")) { //$NON-NLS-1$
				return ret;
			}

			libName = libName.substring(0, libName.length() - 2);
			// Now we have the libname of the unresolved error
			// try to find the library
			IProject project = marker.getResource().getProject();
			ISloeberConfiguration SloeberCfg = ISloeberConfiguration.getActiveConfig(project, false);
			Map<String, IArduinoLibraryVersion> availableLibs = LibraryManager
					.getLibrariesAll(SloeberCfg.getBoardDescription());

			// find the library version to add
			for (IArduinoLibraryVersion curlib : availableLibs.values()) {
				if (libName.equals(curlib.getName())) {
					ret = new IMarkerResolution[1];
					ret[0] = new IMarkerResolution() {

						@Override
						public String getLabel() {
							return AddLibraryToProject.replace(LIB_KEY, curlib.getFQN().toString());
						}

						@Override
						public void run(IMarker marker11) {
							CCorePlugin cCorePlugin = CCorePlugin.getDefault();
							ICProjectDescription projectDescription = cCorePlugin.getProjectDescription(project, true);

							ISloeberConfiguration sloeberConfDesc = ISloeberConfiguration
									.getActiveConfig(projectDescription);
							Set<IArduinoLibraryVersion> libraries = new HashSet<>();
							libraries.add(curlib);
							if (sloeberConfDesc.addLibraries(libraries)) {
								try {
									cCorePlugin.setProjectDescription(project, projectDescription, true, null);
								} catch (CoreException e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
								}
							}

						}

					};
					return ret;
				}
			}

			// No library version that is installed has been found
			// can we install a matching library?

			// In case we want to install and add to project
			// Check wether we need to download and install libraries
			if (!ConfigurationPreferences.getInstallLibraries() ) {
				return ret;
			}
			Set<String> librariesToInstall = new HashSet<>();
			librariesToInstall.add(libName);

			Map<String, IArduinoLibraryVersion> toInstallLibs = LibraryManager.getLatestInstallableLibraries(librariesToInstall);

			if (toInstallLibs.size()!=1) {
				//No or more than 1 installable lib found
				//skip
				return ret;
			}
			IArduinoLibraryVersion toInstalllLib=toInstallLibs.get(libName);

				ret = new IMarkerResolution[1];
				ret[0] = new IMarkerResolution() {

					@Override
					public String getLabel() {
						return InstallAndAddLibraryToProject.replace(LIB_KEY,toInstalllLib.getName())
								.replace(VERSION_KEY, toInstalllLib.getVersion().toString())
								.replace(MAINTAINER, toInstalllLib.getMaintainer());
					}

					@Override
					public void run( IMarker marker1) {
						IArduinoLibraryVersion curlib=null;

						for (Entry<String, IArduinoLibraryVersion> curLibSet : toInstallLibs.entrySet()) {
							LibraryManager.install(curLibSet.getValue(), new NullProgressMonitor());
							curlib=curLibSet.getValue();
						}

						// Now the lib has been added to the workspace add it to the active
						// configuration
						CCorePlugin cCorePlugin = CCorePlugin.getDefault();
						ICProjectDescription projectDescription = cCorePlugin.getProjectDescription(project, true);

						ISloeberConfiguration sloeberConfDesc = ISloeberConfiguration
								.getActiveConfig(projectDescription);
						Set<IArduinoLibraryVersion> libraries = new HashSet<>();
						libraries.add(curlib);
						if (sloeberConfDesc.addLibraries(libraries)) {
							try {
								cCorePlugin.setProjectDescription(project, projectDescription, true, null);
							} catch (CoreException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
						}

					}

				};
				return ret;

			// nothing we can do
		} catch (CoreException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return ret;
	}

}
