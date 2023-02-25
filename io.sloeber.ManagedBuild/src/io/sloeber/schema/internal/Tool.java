package io.sloeber.schema.internal;

import static io.sloeber.autoBuild.core.Messages.*;
import static io.sloeber.autoBuild.integration.AutoBuildConstants.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import org.apache.commons.lang.StringUtils;
import org.eclipse.cdt.core.CCProjectNature;
import org.eclipse.cdt.core.settings.model.extension.CLanguageData;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ProjectScope;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.content.IContentType;
import org.eclipse.core.runtime.content.IContentTypeSettings;
import org.eclipse.core.runtime.preferences.IScopeContext;

import io.sloeber.autoBuild.api.BuildException;
import io.sloeber.autoBuild.api.IEnvVarBuildPath;
import io.sloeber.autoBuild.core.Activator;
import io.sloeber.autoBuild.extensionPoint.IManagedCommandLineGenerator;
import io.sloeber.autoBuild.extensionPoint.providers.MakeRule;
import io.sloeber.autoBuild.extensionPoint.providers.MakeRules;
import io.sloeber.autoBuild.integration.AutoBuildConfigurationData;
import io.sloeber.schema.api.IInputType;
import io.sloeber.schema.api.IOption;
import io.sloeber.schema.api.IOutputType;
import io.sloeber.schema.api.ITool;
import io.sloeber.schema.api.IToolChain;

/**
 * Represents a tool that can be invoked during a build. Note that this class
 * implements IOptionCategory to represent the top category.
 */
public class Tool extends SchemaObject implements ITool {
	// @formatter:off
	/**
	 * A tool is enabled if the enablement part of the xml model validates to true
	 * and the modelNatureFilter matches the nature of the project The table below
	 * shows the decisions
	 *  
	 *  tool cnature ccnature result 
	 *  both    yes   yes      true 
	 *  both    yes   no       true 
	 *  both    no    yes      true 
	 *  both    no    no       false
	 * 
	 * cnature  yes   yes      false 
	 * cnature  yes   no       true 
	 * cnature  no    yes      false 
	 * cnature  no    no       false
	 * 
	 * ccnature yes   yes      true 
	 * ccnature yes   no       false 
	 * ccnature no    yes      true 
	 * ccnature no    no       false
	 * 
	 * note that the case no/no should not happen as a MBS project should have a
	 * cNature and/or CCNature Therefore the code simplifies the decision table to
	 * 
	 * tool cnature ccnature result 
	 * both    yes   yes      true 
	 * both    yes   no       true 
	 * both    no    yes      true 
	 * both    no    no       true
	 *                       
	 * cnature yes   yes      false 
	 * cnature yes   no       true 
	 * cnature no    yes      false 
	 * cnature no    no       true
	 *                       
	 * ccnature yes  yes      true 
	 * ccnature yes  no       false 
	 * ccnature no   yes      true 
	 * ccnature no   no       false
	 * 
	 */
	// @formatter:on
	@Override
	public boolean isEnabled(IResource resource, AutoBuildConfigurationData autoData) {
		if (!super.isEnabled(resource, autoData)) {
			return false;
		}
		try {
			switch (modelNatureFilter[SUPER]) {
			case "both": //$NON-NLS-1$
				return true;
			case "cnature": //$NON-NLS-1$
				return !autoData.getProject().hasNature(CCProjectNature.CC_NATURE_ID);
			case "ccnature": //$NON-NLS-1$
				return autoData.getProject().hasNature(CCProjectNature.CC_NATURE_ID);
			}
		} catch (CoreException e) {
			e.printStackTrace();
		}
		return true;
	}

	private List<IEnvVarBuildPath> envVarBuildPathList;

	// private BooleanExpressionApplicabilityCalculator booleanExpressionCalculator;

	private String[] modelIsAbstract;
	private String[] modelOutputFlag;
	private String[] modelNatureFilter;
	private String[] modelCommand;
	private String[] modelCommandLinePattern;
	private String[] modelCommandLineGenerator;
	private String[] modelErrorParsers;
	private String[] modelCustomBuildStep;
	private String[] modelAnnouncement;
	private String[] modelIcon;
	private String[] modelIsSystem;
	private String[] modelIsHidden;

	private boolean isHidden;
	private boolean customBuildStep;
	private boolean isAbstract;
	private boolean isSystem;
	private ToolChain parent;
	private String myAnnouncement;
	private Map<String, InputType> inputTypeMap = new HashMap<>();
	private Map<String, OutputType> outputTypeMap = new HashMap<>();

	private IManagedCommandLineGenerator commandLineGenerator;

	/**
	 * Constructor to create a new tool for a tool-chain based on the information
	 * defined in the plugin.xml manifest.
	 *
	 * @param parent               The parent of this tool. This can be a ToolChain
	 *                             or a ResourceConfiguration.
	 * @param element              The element containing the information about the
	 *                             tool.
	 * @param managedBuildRevision the fileVersion of Managed Build System
	 */
	public Tool(ToolChain parent, IExtensionPoint root, IConfigurationElement element) {
		this.parent = parent;
		loadNameAndID(root, element);
		modelIsAbstract = getAttributes(IS_ABSTRACT);
		modelOutputFlag = getAttributes(OUTPUT_FLAG);
		modelNatureFilter = getAttributes(NATURE);
		modelCommand = getAttributes(COMMAND);
		modelCommandLinePattern = getAttributes(COMMAND_LINE_PATTERN);
		modelCommandLineGenerator = getAttributes(COMMAND_LINE_GENERATOR);
		modelErrorParsers = getAttributes(ERROR_PARSERS);
		modelCustomBuildStep = getAttributes(CUSTOM_BUILD_STEP);
		modelAnnouncement = getAttributes(ANNOUNCEMENT);
		modelIcon = getAttributes(ICON);
		modelIsSystem = getAttributes(IS_SYSTEM);
		modelIsHidden = getAttributes(IS_HIDDEN);

		isAbstract = Boolean.parseBoolean(modelIsAbstract[ORIGINAL]);
		customBuildStep = Boolean.parseBoolean(modelCustomBuildStep[SUPER]);
		isHidden = Boolean.parseBoolean(modelIsHidden[ORIGINAL]);
		isSystem = Boolean.parseBoolean(modelIsSystem[ORIGINAL]);

		if (modelAnnouncement[SUPER].isBlank()) {
			myAnnouncement = Tool_default_announcement + BLANK + getName(); // + "(" + getId() + ")";
		} else {
			myAnnouncement = modelAnnouncement[SUPER];
		}

		if (!modelCommandLineGenerator[SUPER].isBlank()) {
			commandLineGenerator = (IManagedCommandLineGenerator) createExecutableExtension(COMMAND_LINE_GENERATOR);
		}

		for (IConfigurationElement curChild : getFirstChildren(INPUT_TYPE_ELEMENT_NAME)) {
				InputType child = new InputType(this, root, curChild);
				inputTypeMap.put(child.getId(), child);
			}
		for (IConfigurationElement curChild : getFirstChildren(OUTPUT_TYPE_ELEMENT_NAME)) {
				OutputType child = new OutputType(this, root, curChild);
				outputTypeMap.put(child.getId(), child);
			}
	}

	@Override
	public IToolChain getParent() {
		return parent;
	}

	@Override
	public List<IInputType> getInputTypes() {
		List<IInputType> ret = new LinkedList<>();
		for (InputType cur : inputTypeMap.values()) {
			ret.add(cur);
		}
		return ret;
	}

	@Override
	public List<IOutputType> getOutputTypes() {
		List<IOutputType> out = new LinkedList<>();
		for (OutputType cur : outputTypeMap.values()) {
			// if (cur.isEnabled(this)) {
			out.add(cur);
			// }
		}
		return out;
	}

	@Override
	public IOutputType getOutputTypeById(String optputTypeID) {
		return getAllOutputTypeById(optputTypeID);
		// OutputType type = (OutputType) getAllOutputTypeById(optputTypeID);
		//
		// if (type == null || type.isEnabled(this))
		// return type;
		// return null;
	}

	public IOutputType getAllOutputTypeById(String optputTypeID) {
		return outputTypeMap.get(optputTypeID);
	}

	@Override
	public String getName() {
		return myName;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.cdt.core.build.managed.ITool#isAbstract()
	 */
	@Override
	public boolean isAbstract() {
		return isAbstract;
	}

	@Override
	public String getErrorParserIds() {
		return modelErrorParsers[SUPER];
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.cdt.managedbuilder.core.ITool#getErrorParserList()
	 */
	@Override
	public String[] getErrorParserList() {
		String parserIDs = getErrorParserIds();
		String[] errorParsers;
		if (parserIDs != null) {
			// Check for an empty string
			if (parserIDs.length() == 0) {
				errorParsers = new String[0];
			} else {
				StringTokenizer tok = new StringTokenizer(parserIDs, ";"); //$NON-NLS-1$
				List<String> list = new ArrayList<>(tok.countTokens());
				while (tok.hasMoreElements()) {
					list.add(tok.nextToken());
				}
				String[] strArr = { "" }; //$NON-NLS-1$
				errorParsers = list.toArray(strArr);
			}
		} else {
			errorParsers = new String[0];
		}
		return errorParsers;
	}

	@Override
	public String getOutputFlag() {
		return modelOutputFlag[SUPER];
	}

	@Override
	public String getToolCommand() {
		return modelCommand[SUPER];
	}

	@Override
	public String getCommandLinePattern() {
		return modelCommandLinePattern[SUPER];
	}

	@Override
	public boolean getCustomBuildStep() {
		return customBuildStep;
	}

	@Override
	public String getAnnouncement() {
		return myAnnouncement;
	}

	@Override
	public IManagedCommandLineGenerator getCommandLineGenerator() {
		return commandLineGenerator;
	}

	public List<InputType> getAllInputTypes() {
		return new LinkedList<>(inputTypeMap.values());

	}

	//
	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.cdt.managedbuilder.core.ITool#getEnvVarBuildPaths()
	 */
	@Override
	public IEnvVarBuildPath[] getEnvVarBuildPaths() {
		// if (envVarBuildPathList != null) {
		return envVarBuildPathList.toArray(new IEnvVarBuildPath[envVarBuildPathList.size()]);
		// } else if (getSuperClass() != null)
		// return getSuperClass().getEnvVarBuildPaths();
		// return null;
	}

	public static String[] getContentTypeFileSpecs(IContentType type, IProject project) {
		String[] globalSpecs = type.getFileSpecs(IContentType.FILE_EXTENSION_SPEC);
		IContentTypeSettings settings = null;
		// IProject project = getProject();
		if (project != null) {
			IScopeContext projectScope = new ProjectScope(project);
			try {
				settings = type.getSettings(projectScope);
			} catch (Exception e) {
				Activator.log(e);
			}
			if (settings != null) {
				String[] specs = settings.getFileSpecs(IContentType.FILE_EXTENSION_SPEC);
				if (specs.length > 0) {
					int total = globalSpecs.length + specs.length;
					String[] projSpecs = new String[total];
					int i = 0;
					for (String spec : specs) {
						projSpecs[i] = spec;
						i++;
					}
					for (String spec : globalSpecs) {
						projSpecs[i] = spec;
						i++;
					}
					return projSpecs;
				}
			}
		}
		return globalSpecs;
	}

	@Override
	public CLanguageData getCLanguageData(IInputType type) {
		// JABA dead code
		return null;
		// initDataMap();
		// return typeToDataMap.get(type);
	}

	@Override
	public boolean isSystemObject() {
		return isSystem;
	}

	@Override
	public boolean isHidden() {
		return isHidden;
	}

	@Override
	public IInputType getInputTypeByID(String id2) {
		return inputTypeMap.get(id2);
	}

	/**
	 * This method used internally by the Tool to obtain the command flags with the
	 * build macros resolved, but could be also used by other MBS components to
	 * adjust the tool flags resolution behavior by passing the method some custom
	 * macro substitutor
	 *
	 * @return the command flags with the build macros resolved
	 */
	private String[] getToolCommandFlags(IFile inputFile, IFile outputFile,
			AutoBuildConfigurationData autoBuildConfData) {
		// List<IOption> opts = getOptions();
		Map<String, String> selectedOptions = autoBuildConfData.getSelectedOptions(inputFile);

		ArrayList<String> flags = new ArrayList<>();

		for (IOption curOption : getOptions().getOptions()) {
			String optionValue = selectedOptions.get(curOption.getId());
			if (optionValue == null) {
				// no value set for this option
				// as options with a default are set during project creation time it is
				// safe to ignore this option
				// or this option has no default setting nor a user setting
				// At least that is my thinking JABA
				continue;
			}

			String[] cmdContrib = curOption.getCommandLineContribution(inputFile, optionValue, autoBuildConfData);
			java.util.Collections.addAll(flags, cmdContrib);
			//
			// } catch (BuildException e) {
			// // Bug 315187 one broken option shouldn't cascade to all other options
			// //breaking the build...
			// Status s = new Status(IStatus.ERROR, Activator.getId(),
			// MessageFormat.format(Tool_Problem_Discovering_Args_For_Option, curOption,
			// curOption.getId()),
			// e);
			// Activator.log(new CoreException(s));
			// } catch (CdtVariableException e) {
			// Status s = new Status(IStatus.ERROR, Activator.getId(),
			// MessageFormat.format(Tool_Problem_Discovering_Args_For_Option, curOption,
			// curOption.getId()),
			// e);
			// Activator.log(new CoreException(s));
			// }
		}
		return flags.toArray(new String[flags.size()]);

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.cdt.managedbuilder.core.ITool#getToolCommandFlags(org.eclipse.
	 * core.runtime.IPath, org.eclipse.core.runtime.IPath)
	 */
	@Override
	public String[] getToolCommandFlags(AutoBuildConfigurationData autoConfData, IFile inputFile, IFile outputFile)
			throws BuildException {
		return getToolCommandFlags(inputFile, outputFile, autoConfData);
	}

	public StringBuffer dump(int leadingChars) {
		StringBuffer ret = new StringBuffer();
		String prepend = StringUtils.repeat(DUMPLEAD, leadingChars);
		ret.append(prepend + TOOL_ELEMENT_NAME + NEWLINE);
		ret.append(prepend + NAME + EQUAL + myName + NEWLINE);
		ret.append(prepend + ID + EQUAL + myID + NEWLINE);
		ret.append(prepend + IS_ABSTRACT + EQUAL + modelIsAbstract[ORIGINAL] + NEWLINE);
		ret.append(prepend + OUTPUT_FLAG + EQUAL + modelOutputFlag[SUPER] + NEWLINE);
		ret.append(prepend + NATURE + EQUAL + modelNatureFilter[SUPER] + NEWLINE);
		ret.append(prepend + COMMAND + EQUAL + modelCommand[SUPER] + NEWLINE);
		ret.append(prepend + COMMAND_LINE_PATTERN + EQUAL + modelCommandLinePattern[SUPER] + NEWLINE);

		ret.append(prepend + COMMAND_LINE_GENERATOR + EQUAL + modelCommandLineGenerator[SUPER] + NEWLINE);
		ret.append(prepend + ERROR_PARSERS + EQUAL + modelErrorParsers[SUPER] + NEWLINE);
		ret.append(prepend + CUSTOM_BUILD_STEP + EQUAL + modelCustomBuildStep[SUPER] + NEWLINE);
		ret.append(prepend + ANNOUNCEMENT + EQUAL + modelAnnouncement[SUPER] + NEWLINE);
		ret.append(prepend + ICON + EQUAL + modelIcon[SUPER] + NEWLINE);
		ret.append(prepend + IS_HIDDEN + EQUAL + modelIsHidden[SUPER] + NEWLINE);
		ret.append(prepend + IS_SYSTEM + EQUAL + modelIsSystem[SUPER] + NEWLINE);
		if (myEnablement.isBlank()) {
			ret.append(prepend + "No enablement found" + NEWLINE); //$NON-NLS-1$
		} else {
			ret.append(prepend + "Enablement found" + NEWLINE); //$NON-NLS-1$
		}
		for (InputType curInputType : inputTypeMap.values()) {
			ret.append(curInputType.dump(leadingChars + 1));
		}
		for (OutputType curOutputType : outputTypeMap.values()) {
			ret.append(curOutputType.dump(leadingChars + 1));
		}
		ret.append(myOptions.dump(leadingChars + 1));

		return ret;
	}

	static private final String IGNORED_BY = " ignored by: "; //$NON-NLS-1$
	static private final String DISABLED = " disabled: ";//$NON-NLS-1$
	static private final String ACCEPTED_BY = " accepted by: "; //$NON-NLS-1$

	@Override
	public MakeRules getMakeRules(AutoBuildConfigurationData autoBuildConfData, IOutputType outputTypeIn,
			IFile inputFile, int makeRuleSequenceID, boolean VERBOSE) {
		MakeRules ret = new MakeRules();
		if (!isEnabled(inputFile, autoBuildConfData)) {
			return ret;
		}
		for (IInputType inputType : getInputTypes()) {
			if (inputType.isAssociatedWith(inputFile, outputTypeIn)) {
				for (IOutputType outputType : getOutputTypes()) {
					if (!outputType.isEnabled(inputFile, autoBuildConfData)) {
						if (VERBOSE) {
							System.out.println(inputFile + BLANK + myName + ACCEPTED_BY + inputType.getName() + DISABLED
									+ outputType.getName());
						}
						continue;
					}
					IFile outputFile = outputType.getOutputName(inputFile, autoBuildConfData, inputType);
					if (outputFile == null) {
						if (VERBOSE) {
							System.out.println(inputFile + BLANK + myName + ACCEPTED_BY + inputType.getName()
									+ IGNORED_BY + outputType.getName());
						}
						continue;
					}
					if (VERBOSE) {
						System.out.println(inputFile + BLANK + myName + ACCEPTED_BY + inputType.getName() + ACCEPTED_BY
								+ outputType.getName());
					}
					MakeRule newMakeRule = new MakeRule(this, inputType, inputFile, outputType, outputFile,
							makeRuleSequenceID);

					ret.addRule(newMakeRule);
				}
			} else {
				if (VERBOSE) {
					System.out.println(inputFile + BLANK + myName + IGNORED_BY + inputType.getName());
				}
			}
		}
		return ret;
	}

}
