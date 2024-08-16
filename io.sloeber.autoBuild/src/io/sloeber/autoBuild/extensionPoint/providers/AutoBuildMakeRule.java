package io.sloeber.autoBuild.extensionPoint.providers;

import static io.sloeber.autoBuild.api.AutoBuildCommon.*;
import static io.sloeber.autoBuild.helpers.api.AutoBuildConstants.*;

import java.io.BufferedReader;
import java.io.File;
import java.nio.file.Path;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

import org.eclipse.cdt.core.language.settings.providers.ILanguageSettingsProvider;
import org.eclipse.cdt.core.language.settings.providers.ILanguageSettingsProvidersKeeper;
import org.eclipse.cdt.core.settings.model.ICConfigurationDescription;
import org.eclipse.cdt.core.settings.model.ICLanguageSettingEntry;
import org.eclipse.cdt.core.settings.model.ICSettingEntry;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import io.sloeber.autoBuild.api.AutoBuildBuilderExtension;
import io.sloeber.autoBuild.api.IAutoBuildMakeRule;
import io.sloeber.autoBuild.core.Activator;
import io.sloeber.autoBuild.integration.AutoBuildConfigurationDescription;
import io.sloeber.autoBuild.schema.api.IInputType;
import io.sloeber.autoBuild.schema.api.IOption;
import io.sloeber.autoBuild.schema.api.IOutputType;
import io.sloeber.autoBuild.schema.api.ITool;

public class AutoBuildMakeRule implements IAutoBuildMakeRule {

	private Map<IOutputType, Set<IFile>> myTargets = new LinkedHashMap<>(); // Macro file target map
	private Map<IInputType, Set<IFile>> myPrerequisites = new LinkedHashMap<>();// Macro file prerequisites map
	private Map<String, Set<IFile>> myDependencies = new LinkedHashMap<>(); // Macro file target map
	private ITool myTool = null;
	private int mySequenceGroupID = 0;

	@Override
	public ITool getTool() {
		return myTool;
	}

	public AutoBuildMakeRule(ITool tool, IInputType inputType, IFile inputFile, IOutputType outputType, IFile outFile,
			int sequenceID) {
		addPrerequisite(inputType, inputFile);
		addTarget(outputType, outFile);
		myTool = tool;
		mySequenceGroupID = sequenceID;
	}

	public AutoBuildMakeRule(ITool tool, IInputType inputType, Set<IFile> inputFiles, IOutputType outputType,
			IFile outFile, int sequenceID) {
		addPrerequisites(inputType, inputFiles);
		addTarget(outputType, outFile);
		myTool = tool;
		mySequenceGroupID = sequenceID;
	}

	public void getDependencies() {
		myDependencies.clear();
		for (Entry<IOutputType, Set<IFile>> curTarget : myTargets.entrySet()) {
			IOutputType curOutputType = curTarget.getKey();
			Set<IFile> files = curTarget.getValue();
			String depkey = curOutputType.getBuildVariable() + DEPENDENCY_SUFFIX;
			Set<IFile> depFiles = new HashSet<>();
			for (IFile curTargetFile : files) {
				depFiles.add(myTool.getDependencyFile(curTargetFile));
			}
			depFiles.remove(null);
			myDependencies.put(depkey, depFiles);
		}
	}

	@Override
	public Set<IFile> getPrerequisiteFiles() {
		HashSet<IFile> ret = new HashSet<>();
		for (Set<IFile> cur : myPrerequisites.values()) {
			ret.addAll(cur);
		}
		return ret;
	}

	@Override
	public Map<IInputType, Set<IFile>> getPrerequisites() {
		return myPrerequisites;
	}

	@Override
	public Set<IFile> getTargetFiles() {
		Set<IFile> ret = new HashSet<>();
		for (Set<IFile> cur : myTargets.values()) {
			ret.addAll(cur);
		}
		return ret;
	}

	@Override
	public Set<IFile> getDependencyFiles() {
		getDependencies();
		Set<IFile> ret = new HashSet<>();
		for (Set<IFile> cur : myDependencies.values()) {
			ret.addAll(cur);
		}
		return ret;
	}

	@Override
	public Map<IOutputType, Set<IFile>> getTargets() {
		return myTargets;
	}

	public Set<String> getAllMacros() {
		Set<String> ret = getTargetMacros();
		ret.addAll(getPrerequisiteMacros());
		ret.addAll(getDependencyMacros());
		return ret;
	}

	public Set<String> getTargetMacros() {
		HashSet<String> ret = new LinkedHashSet<>();
		for (IOutputType cur : myTargets.keySet()) {
			ret.add(cur.getBuildVariable());
		}
		return ret;
	}

	public Set<String> getPrerequisiteMacros() {
		HashSet<String> ret = new LinkedHashSet<>();
		for (IInputType cur : myPrerequisites.keySet()) {
			ret.add(cur.getBuildVariable());
		}
		ret.remove(EMPTY_STRING);
		return ret;
	}

	public Set<String> getDependencyMacros() {
		getDependencies();
		HashSet<String> ret = new LinkedHashSet<>();
		ret.addAll(myDependencies.keySet());
		return ret;
	}

	public HashSet<IFile> getMacroElements(String macroName) {
		HashSet<IFile> ret = new HashSet<>();

		for (Entry<IOutputType, Set<IFile>> cur : myTargets.entrySet()) {
			if (macroName.equals(cur.getKey().getBuildVariable())) {
				ret.addAll(cur.getValue());
			}
		}
		for (Entry<IInputType, Set<IFile>> cur : myPrerequisites.entrySet()) {
			if (macroName.equals(cur.getKey().getBuildVariable())) {
				ret.addAll(cur.getValue());
			}
		}
		Set<IFile> tmp = myDependencies.get(macroName);
		if (tmp != null) {
			ret.addAll(tmp);
		}
		return ret;
	}

	private void addTarget(IOutputType outputType, IFile file) {
		Set<IFile> files = myTargets.get(outputType);
		if (files == null) {
			files = new HashSet<>();
			files.add(file);
			myTargets.put(outputType, files);
		} else {
			files.add(file);
		}
	}

	private void addPrerequisite(IInputType inputType, IFile file) {
		Set<IFile> files = myPrerequisites.get(inputType);
		if (files == null) {
			files = new HashSet<>();
			files.add(file);
			myPrerequisites.put(inputType, files);
		} else {
			files.add(file);
		}
	}

	/**
	 * validate if the makerule contains valid recipes
	 *
	 * @return true if valid false if not
	 */
	private boolean validateRecipes() {
		Set<IFile> local_targets = getTargetFiles();
		Set<IFile> local_prerequisites = getPrerequisiteFiles();
		if (local_targets.size() != 1) {
			System.err.println("Only 1 target per build rule is supported in this managed build"); //$NON-NLS-1$
			return false;
		}
		if (local_prerequisites.size() == 0) {
			System.err.println("0 prerequisites is not supported in this managed build"); //$NON-NLS-1$
			return false;
		}
		return true;
	}

	@Override
	public String[] getRecipes(IFolder buildFolder, AutoBuildConfigurationDescription autoBuildConfData) {
		if (!validateRecipes()) {
			return new String[0];
		}

		// get the input files
		Set<IFile> inputFiles = getPrerequisiteFiles();

		// from all the options for this project; get the options for this
		// tool/prerequisites
		// not there is a filtering happening in this step and there may be duplicates
		// here we will assume this is handled properly by
		// AutoBuildConfigurationDescription
		TreeMap<IOption, String> selectedOptions = autoBuildConfData.getSelectedOptions(inputFiles, myTool);

		// with all the options applicable for this makerule generate variables to
		// expand in the recipes
		Map<String, String> toolCommandVars = myTool.getToolCommandVars(autoBuildConfData, selectedOptions);

		// add the myPrerequisites to the variables
		for (Entry<IInputType, Set<IFile>> cur : myPrerequisites.entrySet()) {
			String var = cur.getKey().getAssignToCmdVarriable();
			String curCommandVarValue = toolCommandVars.get(var);
			if (curCommandVarValue == null) {
				curCommandVarValue = new String();
			} else {
				curCommandVarValue = curCommandVarValue.trim() + WHITESPACE;
			}
			for (IFile curPrereqFile : cur.getValue()) {
				if (curPrereqFile != null) {
					String curFileName = GetNiceFileName(buildFolder, curPrereqFile);
					// if the input resource isn't a variable then quote it
					if (curFileName.indexOf("$(") != 0) { //$NON-NLS-1$
						curFileName = DOUBLE_QUOTE + curFileName + DOUBLE_QUOTE;
					}
					curCommandVarValue = curCommandVarValue + curFileName + WHITESPACE;
				}
			}
			if (!curCommandVarValue.isBlank()) {
				toolCommandVars.put(var, curCommandVarValue.trim());
			}
		}

		// add the provider items to the flags
		ICConfigurationDescription cfgDescription = autoBuildConfData.getCdtConfigurationDescription();
		IProject project = autoBuildConfData.getProject();
		String includeFiles = new String();
		String includePath = new String();
		String providerMacros = new String();
		for (ILanguageSettingsProvider provider : ((ILanguageSettingsProvidersKeeper) cfgDescription)
				.getLanguageSettingProviders()) {
			for (IInputType curInputType : myPrerequisites.keySet()) {
				String languageId = curInputType.getLanguageID();
				if (languageId == null || languageId.isEmpty()) {
					continue;
				}
				for (IFile curFile : myPrerequisites.get(curInputType)) {
					List<ICLanguageSettingEntry> configEntries = provider.getSettingEntries(cfgDescription, curFile,
							languageId);
					if (configEntries != null) {
						for (ICLanguageSettingEntry curEntry : configEntries) {
							if (curEntry.isBuiltIn()) {
								// ignore build in settings
								continue;
							}
							switch (curEntry.getKind()) {
							case ICSettingEntry.INCLUDE_FILE: {
								IFile file = project.getWorkspace().getRoot()
										.getFile(IPath.forPosix(curEntry.getValue()));
								includeFiles = includeFiles + WHITESPACE + DOUBLE_QUOTE + CMD_LINE_INCLUDE_FILE
										+ file.getLocation().toString() + DOUBLE_QUOTE;
								break;
							}
							case ICSettingEntry.INCLUDE_PATH: {
								IPath path = project.getWorkspace().getRoot()
										.getFolder(IPath.forPosix(curEntry.getValue())).getLocation();
								if(path==null) {
									//Log error to allow for investigation
									Activator.log(new Status(IStatus.WARNING, Activator.PLUGIN_ID, "No location found for "+curEntry.getValue()));

								}else {
								includePath = includePath + WHITESPACE + DOUBLE_QUOTE + CMD_LINE_INCLUDE_FOLDER
										+ path.toString() + DOUBLE_QUOTE;
								}
								break;
							}
							case ICSettingEntry.MACRO: {
								providerMacros = providerMacros + WHITESPACE + DOUBLE_QUOTE + CMD_LINE_DEFINE
										+ curEntry.getValue() + DOUBLE_QUOTE;
								break;
							}
							default:
								break;

							}
						}
					}

				}
			}
		}
		String flags = toolCommandVars.get(FLAGS_PRM_NAME);
		if (flags == null) {
			flags = new String();
		}
		flags = flags.trim() + WHITESPACE + providerMacros;
		flags = flags.trim() + WHITESPACE + includeFiles;
		flags = flags.trim() + WHITESPACE + includePath;
		toolCommandVars.put(FLAGS_PRM_NAME, flags.trim());

		// add the target files to the variables
		String targetFiles = toolCommandVars.get(OUTPUT_PRM_NAME);
		if (targetFiles == null) {
			targetFiles = new String();
		}
		IFile targetFile = null;
		for (IFile curTargetFile : getTargetFiles()) {
			if (targetFile == null) {
				targetFile = curTargetFile;
			}
			targetFiles = targetFiles.trim() + WHITESPACE + DOUBLE_QUOTE + GetNiceFileName(buildFolder, curTargetFile)
					+ DOUBLE_QUOTE;
		}
		toolCommandVars.put(OUTPUT_PRM_NAME, targetFiles.trim());

		// get the recipes
		String buildRecipes[] = myTool.getRecipes(autoBuildConfData, buildFolder, inputFiles, toolCommandVars,
				targetFile);

		AutoBuildBuilderExtension builderExt =autoBuildConfData.getProjectType().getBuilderExtension();
		if(builderExt!=null) {
		 buildRecipes = builderExt.modifyRecipes( autoBuildConfData,this,buildRecipes);
		}
		// expand the recipes
		ArrayList<String> ret = new ArrayList<>();
		for (String curRecipe : buildRecipes) {
			String resolvedCommand = resolveRecursive(curRecipe, EMPTY_STRING, WHITESPACE, autoBuildConfData);
			if (resolvedCommand.isBlank()) {
				resolvedCommand = curRecipe;
			}
			if (!resolvedCommand.isBlank()) {
				// We need to split again as the variable expansion may have introduced new
				// newlines
				ret.addAll(Arrays.asList(resolvedCommand.split(LINE_BREAK_REGEX)));
			}
		}
		return ret.toArray(new String[ret.size()]);
	}

	public void addPrerequisites(IInputType inputType, Set<IFile> files) {
		Set<IFile> entrypoint = myPrerequisites.get(inputType);
		if (entrypoint != null) {
			entrypoint.addAll(files);
		} else {
			Set<IFile> copyOfFiles = new HashSet<>();
			copyOfFiles.addAll(files);
			myPrerequisites.put(inputType, copyOfFiles);
		}
	}


	@Override
	public boolean isTool(ITool tool) {
		return myTool.getName().equals(tool.getName());
	}

	@Override
	public int getSequenceGroupID() {
		return mySequenceGroupID;
	}

	public void setSequenceGroupID(int mySequenceGroupID) {
		this.mySequenceGroupID = mySequenceGroupID;
	}

	@Override
	public boolean isForContainer(IContainer folder) {
		for (Set<IFile> files : myPrerequisites.values()) {
			for (IFile file : files) {
				if (file.getParent().equals(folder)) {
					return true;
				}
			}
		}
		return false;
	}

	@Override
	public boolean needsExecuting(IFolder buildfolder) {
		Set<IFile> dependencyFiles = new HashSet<>();
		// check whether all targets exists
		// also get the timestamp of the oldest target
		long jongestTargetTimeStamp = Long.MAX_VALUE;
		for (Set<IFile> curTargetSet : myTargets.values()) {
			for (IFile curTarget : curTargetSet) {
				if (!curTarget.exists()) {
					return true;
				}
				// could be that a refresh is needed due to cached local time stamp
				jongestTargetTimeStamp = Math.min(jongestTargetTimeStamp, curTarget.getLocalTimeStamp());
			}
		}
		// check wether all dependency files exists
		getDependencies();// TODO JABA this is very error prone.
		for (Set<IFile> curDependencySet : myDependencies.values()) {
			for (IFile curDependency : curDependencySet) {
				if (!curDependency.exists()) {
					return true;
				}
				dependencyFiles.add(curDependency);
			}
		}
		// get the newest prerequisite timeStamp
		long oldestPreReqTimeStamp = Long.MIN_VALUE;
		for (Set<IFile> curPrereqSet : myPrerequisites.values()) {
			for (IFile curPrereq : curPrereqSet) {
				if (!curPrereq.exists()) {
					return true;
				}
				oldestPreReqTimeStamp = Math.max(oldestPreReqTimeStamp, curPrereq.getLocalTimeStamp());
			}
		}
		if (oldestPreReqTimeStamp > jongestTargetTimeStamp) {
			return true;
		}

		// get the newest dependency timeStamp
		long oldestDependencyTimeStamp = Long.MIN_VALUE;
		for (IFile curdepFile : dependencyFiles) {
			oldestDependencyTimeStamp = Math.max(oldestDependencyTimeStamp,
					getDepFileTimeStamp(curdepFile, buildfolder));
		}
		if (oldestDependencyTimeStamp >= jongestTargetTimeStamp) {
			return true;
		}
		return false;
	}

	/**
	 * given a dependency file; return the time stamp of the youngest file mentioned
	 * in the dependency file
	 *
	 * @param depFile the dependency file created by a compiler
	 *
	 * @return the timestamp of the oldest file in the files; 0 if a referenced file
	 *         does not exist
	 */
	private static long getDepFileTimeStamp(IFile curdepFile, IFolder buildPath) {
		long newestTime = Long.MIN_VALUE;
		File depFile = curdepFile.getLocation().toFile();
		try (BufferedReader reader = new BufferedReader(new FileReader(depFile));) {
			String curLine = null;
			while ((curLine = reader.readLine()) != null) {
				if (curLine.endsWith(COLON)) {
					String headerName = curLine.substring(0, curLine.length() - 1).replace(BACKSLACH + BLANK, BLANK);
					Path headerFile = Path.of(headerName);
					if (!headerFile.isAbsolute()) {
						//The line below does not work for URI based projects
						//headerName = buildPath.getFile(headerName).getLocation().toString();
						headerName = buildPath.getLocation().append(headerName).toString();
						headerFile = Path.of(headerName);
					}
					BasicFileAttributes attr = Files.readAttributes(headerFile, BasicFileAttributes.class);
					newestTime = Math.max(attr.lastModifiedTime().toMillis(), newestTime);
				}
			}
			reader.close();
		} catch ( IOException e) {
			e.printStackTrace();
			return Long.MAX_VALUE;
		}
		return newestTime;
	}

	@Override
	public String getAnnouncement() {
		String announcement = getTool().getAnnouncement();
		if (announcement.isBlank()) {
			announcement = DEFAULT_BUILDSTEP_ANNOUNCEMENT;
		}
		String targetFileNames = new String();
		String separator = new String();
		for (IFile curFile : getTargetFiles()) {
			targetFileNames = targetFileNames + separator + curFile.getName();
			separator = COMMA + BLANK;
		}
		return announcement + COLON + BLANK + targetFileNames;
	}

}
