package io.sloeber.core.listeners;

import static io.sloeber.core.api.Const.*;

/**
 * this index listener makes it possible to detect missing libraries
 * if configured to do so libraries are added automatically to the project
 */
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Map.Entry;

import org.eclipse.cdt.core.CCorePlugin;
import org.eclipse.cdt.core.index.IIndex;
import org.eclipse.cdt.core.index.IIndexChangeEvent;
import org.eclipse.cdt.core.index.IIndexChangeListener;
import org.eclipse.cdt.core.index.IIndexFile;
import org.eclipse.cdt.core.index.IIndexInclude;
import org.eclipse.cdt.core.index.IIndexerStateEvent;
import org.eclipse.cdt.core.index.IIndexerStateListener;
import org.eclipse.cdt.core.model.CoreModel;
import org.eclipse.cdt.core.model.ICProject;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;

import io.sloeber.core.Activator;
import io.sloeber.core.Messages;
import io.sloeber.core.api.Common;
import io.sloeber.core.api.Const;
import io.sloeber.core.api.IArduinoLibraryVersion;
import io.sloeber.core.api.IInstallLibraryHandler;
import io.sloeber.core.api.ISloeberConfiguration;
import io.sloeber.core.api.LibraryManager;
import io.sloeber.core.common.InstancePreferences;

public class IndexerListener implements IIndexChangeListener, IIndexerStateListener {
	private static Set<ISloeberConfiguration> newChangedProjects = new HashSet<>();

	private static Map<String, String> myIncludeHeaderReplacement;

	@Override
	public void indexChanged(IIndexChangeEvent event) {
		if (!InstancePreferences.getAutomaticallyImportLibraries()) {
			newChangedProjects.clear();
			return;
		}
		IProject project = event.getAffectedProject().getProject();
		ISloeberConfiguration sloeberConfDesc = ISloeberConfiguration.getActiveConfig(project);
//        if (IndexerController.isPosponed(project)) {
//            // Do not update libraries if project is in creation
//            return;
//        }
		if (sloeberConfDesc == null) {
			return;
		}
		if (!newChangedProjects.contains(sloeberConfDesc)) {
			Common.log(new Status(Const.SLOEBER_STATUS_DEBUG, Activator.getId(),
					"Index of project changed :" + project.getName())); //$NON-NLS-1$
			newChangedProjects.add(sloeberConfDesc);
		}
	}

	@Override
	public void indexChanged(IIndexerStateEvent event) {

		if (event.indexerIsIdle()) {
			if (!newChangedProjects.isEmpty()) {
				Set<ISloeberConfiguration> curChangedProjects = new HashSet<>(newChangedProjects);
				newChangedProjects.clear();
				for (ISloeberConfiguration sloeberConfDesc : curChangedProjects) {
					String projectName = sloeberConfDesc.getProject().getName();
					try {
						Common.log(new Status(Const.SLOEBER_STATUS_DEBUG, Activator.getId(),
								"Looking for libraries for project :" + projectName)); //$NON-NLS-1$
						checkLibraries(sloeberConfDesc);
					} catch (Exception e) {
						Common.log(new Status(IStatus.WARNING, Activator.getId(), Messages.Failed_To_Add_Libraries, e));
					}
					Common.log(new Status(Const.SLOEBER_STATUS_DEBUG, Activator.getId(),
							"libraries added for project " + projectName)); //$NON-NLS-1$
				}
			}
		}
	}

	private static void checkLibraries(ISloeberConfiguration SloeberCfg) {
		Map<String, IArduinoLibraryVersion> alreadyAddedLibs = SloeberCfg.getUsedLibraries();
		Set<String> UnresolvedIncludedHeaders = getUnresolvedProjectIncludes(SloeberCfg.getProject());
		// remove pgmspace as it gives a problem
		UnresolvedIncludedHeaders.remove("pgmspace"); //$NON-NLS-1$

		//The line below is for cases where libs have been excluded from the build
		UnresolvedIncludedHeaders.removeAll(alreadyAddedLibs.keySet());

		if (UnresolvedIncludedHeaders.isEmpty()) {
			return;
		}

		for (Map.Entry<String, String> entry : getIncludeHeaderReplacement().entrySet()) {
			if (UnresolvedIncludedHeaders.contains(entry.getKey())) {
				UnresolvedIncludedHeaders.remove(entry.getKey());
				UnresolvedIncludedHeaders.add(entry.getValue());
			}
		}

		Map<String, IArduinoLibraryVersion> availableLibs = LibraryManager.getLibrariesAll(SloeberCfg.getBoardDescription());

		//Check wether we need to download and install libraries
		IInstallLibraryHandler installHandler = LibraryManager.getInstallLibraryHandler();
		if (installHandler.autoInstall()) {
			// Check if there are libraries that are not found in
			// the installed libraries
			Set<String> uninstalledIncludedHeaders = new TreeSet<>(UnresolvedIncludedHeaders);
			for(IArduinoLibraryVersion curlib:availableLibs.values()) {
				uninstalledIncludedHeaders.remove(curlib.getName());
			}
			//uninstalledIncludedHeaders.removeAll(availableLibs.keySet());
			if (!uninstalledIncludedHeaders.isEmpty()) {
				// some libraries may need to be installed

				Map<String, IArduinoLibraryVersion> toInstallLibs = LibraryManager
						.getLatestInstallableLibraries(uninstalledIncludedHeaders);

				if (!toInstallLibs.isEmpty()) {
					// Ask the user which libs need installing
					toInstallLibs = installHandler.selectLibrariesToInstall(toInstallLibs);
					for (Entry<String, IArduinoLibraryVersion> curLib : toInstallLibs.entrySet()) {
						LibraryManager.install(curLib.getValue(), new NullProgressMonitor());
					}
					//As libraries have been installed update the lst of available libraries
					availableLibs= LibraryManager.getLibrariesAll(SloeberCfg.getBoardDescription());
				}
			}
		}

		//find the libs we can add
		String toInstallLibString=new String();
		Set< IArduinoLibraryVersion> toInstallLibs=new HashSet<>();
		for(IArduinoLibraryVersion curlib:availableLibs.values()) {
			if(UnresolvedIncludedHeaders.contains(curlib.getName())) {
				toInstallLibs.add(curlib);
				toInstallLibString=toInstallLibString+SPACE+curlib.getFQN();
			}
		}
		if (!toInstallLibs.isEmpty()) {
			// there are possible libraries to add
			Common.log(new Status(IStatus.INFO, CORE_PLUGIN_ID, "list of libraries to add to project " //$NON-NLS-1$
					+ SloeberCfg.getProject().getName() + COLON + SPACE
					+ toInstallLibString));
			SloeberCfg.addLibraries(toInstallLibs);
		}
	}

	private static Set<String> getUnresolvedProjectIncludes(IProject iProject) {
		Set<String> ret = new TreeSet<>();
		ICProject tt = CoreModel.getDefault().create(iProject);
		IIndex index = null;

		try {
			index = CCorePlugin.getIndexManager().getIndex(tt);
			index.acquireReadLock();
			try {

				IIndexFile allFiles[] = index.getFilesWithUnresolvedIncludes();
				for (IIndexFile curUnesolvedIncludeFile : allFiles) {
					IIndexInclude includes[] = curUnesolvedIncludeFile.getIncludes();
					for (IIndexInclude curinclude : includes) {
						if (curinclude.isActive() && !curinclude.isResolved()) {
							ret.add(new Path(curinclude.getName()).removeFileExtension().toString());
						}
					}
				}
			} finally {
				index.releaseReadLock();
			}
		} catch (CoreException e1) {
			e1.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		return ret;
	}

	private static Map<String, String> getIncludeHeaderReplacement() {
		if (myIncludeHeaderReplacement == null) {
			myIncludeHeaderReplacement = buildincludeHeaderReplacementMap();
		}
		return myIncludeHeaderReplacement;
	}

	/**
	 * Builds a map of includes->libraries for all headers not mapping libraryname.h
	 * If a include is found more than once in the libraries it is not added to the
	 * list If a library has to many includes it is ignored
	 *
	 * @return
	 */
	private static Map<String, String> buildincludeHeaderReplacementMap() {

		Map<String, String> map = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
//		LinkedList<String> doubleHeaders = new LinkedList<>();
//		TreeMap<String, IPath> libraries = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
//
//		libraries.putAll(findAllArduinoManagerLibraries());
//		libraries.putAll(findAllPrivateLibraries());
//		for (Entry<String, IPath> CurItem : libraries.entrySet()) {
//			IPath sourcePath = CurItem.getValue();
//			String curLibName = CurItem.getKey();
//			if (sourcePath.append(SRC_FODER).toFile().exists()) {
//				sourcePath = sourcePath.append(SRC_FODER);
//			}
//			File[] allHeaderFiles = sourcePath.toFile().listFiles(new FilenameFilter() {
//				@Override
//				public boolean accept(File dir, String name) {
//					return name.toLowerCase().endsWith(".h"); //$NON-NLS-1$
//				}
//			});
//			if (Arrays.stream(allHeaderFiles).anyMatch(new File(curLibName + ".h")::equals)) { //$NON-NLS-1$
//				// if (ArrayUtils.contains(allHeaderFiles, new File(curLibName + ".h"))) {
//				// //$NON-NLS
//				// We found a one to one match make sure others do not
//				// overrule
//				doubleHeaders.add(curLibName);
//				map.remove(curLibName + ".h"); //$NON-NLS-1$
//			} else if (allHeaderFiles.length <= 10) { // Ignore libraries with
//														// to many headers
//				for (File CurFile : allHeaderFiles) {
//					String curInclude = CurFile.getName().substring(0, CurFile.getName().length() - 2);
//
//					// here we have a lib using includes that do not map the
//					// folder name
//					if ((map.get(curInclude) == null) && (!doubleHeaders.contains(curInclude))) {
//						map.put(curInclude, curLibName);
//					} else {
//						doubleHeaders.add(curInclude);
//						map.remove(curInclude);
//					}
//				}
//			}
//		}
//		// return KeyValue.makeMap(
//		// "AFMotor=Adafruit_Motor_Shield_library\nAdafruit_MotorShield=Adafruit_Motor_Shield_V2_Library\nAdafruit_Simple_AHRS=Adafruit_AHRS\nAdafruit_ADS1015=Adafruit_ADS1X15\nAdafruit_ADXL345_U=Adafruit_ADXL345\n\nAdafruit_LSM303_U=Adafruit_LSM303DLHC\nAdafruit_BMP085_U=Adafruit_BMP085_Unified\nAdafruit_BLE=Adafruit_BluefruitLE_nRF51");
//		// //$NON-NLS-1$
//		// add adrfruit sensor as this lib is highly used and the header is in
//		// libs
//		map.put("Adafruit_Sensor", "Adafruit_Unified_Sensor"); //$NON-NLS-1$ //$NON-NLS-2$
//		// remove the common hardware libraries so they will never be redirected
//		map.remove("SPI"); //$NON-NLS-1$
//		map.remove("SoftwareSerial"); //$NON-NLS-1$
//		map.remove("HID"); //$NON-NLS-1$
//		map.remove("EEPROM"); //$NON-NLS-1$
		return map;
	}

}
