package io.sloeber.core.toolchain;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.Vector;

import org.eclipse.cdt.managedbuilder.core.BuildException;
import org.eclipse.cdt.managedbuilder.core.IAdditionalInput;
import org.eclipse.cdt.managedbuilder.core.IConfiguration;
import org.eclipse.cdt.managedbuilder.core.IInputType;
import org.eclipse.cdt.managedbuilder.core.IManagedOutputNameProvider;
import org.eclipse.cdt.managedbuilder.core.IOption;
import org.eclipse.cdt.managedbuilder.core.IOutputType;
import org.eclipse.cdt.managedbuilder.core.ITool;
import org.eclipse.cdt.managedbuilder.core.ManagedBuildManager;
import org.eclipse.cdt.managedbuilder.internal.core.ManagedMakeMessages;
import org.eclipse.cdt.managedbuilder.internal.core.Tool;
import org.eclipse.cdt.managedbuilder.internal.macros.OptionContextData;
import org.eclipse.cdt.managedbuilder.macros.BuildMacroException;
import org.eclipse.cdt.managedbuilder.macros.IBuildMacroProvider;
import org.eclipse.cdt.managedbuilder.makegen.IManagedBuilderMakefileGenerator;
import org.eclipse.cdt.managedbuilder.makegen.IManagedDependencyCalculator;
import org.eclipse.cdt.managedbuilder.makegen.IManagedDependencyGenerator;
import org.eclipse.cdt.managedbuilder.makegen.IManagedDependencyGenerator2;
import org.eclipse.cdt.managedbuilder.makegen.IManagedDependencyGeneratorType;
import org.eclipse.cdt.managedbuilder.makegen.IManagedDependencyInfo;
import org.eclipse.cdt.managedbuilder.makegen.gnu.IManagedBuildGnuToolInfo;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;

import io.sloeber.core.common.Common;
import io.sloeber.core.common.Const;
import io.sloeber.core.toolchain.ArduinoGnuMakefileGenerator.ToolInfoHolder;

/**
 * This class represents information about a Tool's inputs and outputs while a
 * Gnu makefile is being generated.
 *
 * @noextend This class is not intended to be subclassed by clients.
 */
@SuppressWarnings({ "restriction", "deprecation" ,"unused"})
public class ArduinoManagedBuildGnuToolInfo implements IManagedBuildGnuToolInfo {

	/*
	 * Members
	 */
	private IProject project;
	private Tool tool;
	private boolean bIsTargetTool;
	private String targetName;
	private String targetExt;
	private boolean inputsCalculated = false;
	private boolean outputsCalculated = false;
	private boolean outputVariablesCalculated = false;
	private boolean dependenciesCalculated = false;
	private Vector<String> commandInputs = new Vector<>();
	private Vector<String> enumeratedInputs = new Vector<>();
	private Vector<String> commandOutputs = new Vector<>();
	private Vector<String> enumeratedPrimaryOutputs = new Vector<>();
	private Vector<String> enumeratedSecondaryOutputs = new Vector<>();
	private Vector<String> outputVariables = new Vector<>();
	private Vector<String> commandDependencies = new Vector<>();
	private Vector<String> additionalTargets = new Vector<>();

	// private Vector enumeratedDependencies = new Vector();
	// Map of macro names (String) to values (List)

	/*
	 * Constructor
	 */
	public ArduinoManagedBuildGnuToolInfo(IProject project, ITool tool, boolean targetTool, String name, String ext) {
		this.project = project;
		this.tool = (Tool) tool;
		this.bIsTargetTool = targetTool;
		if (this.bIsTargetTool) {
			this.targetName = name;
			this.targetExt = ext;
		}
	}

	/*
	 * IManagedBuildGnuToolInfo Methods
	 */
	@Override
	public boolean areInputsCalculated() {
		return this.inputsCalculated;
	}

	// Command inputs are top build directory relative
	@Override
	public Vector<String> getCommandInputs() {
		return this.commandInputs;
	}

	// Enumerated inputs are project relative
	@Override
	public Vector<String> getEnumeratedInputs() {
		return this.enumeratedInputs;
	}

	@Override
	public boolean areOutputsCalculated() {
		return this.outputsCalculated;
	}

	// Command outputs are top build directory relative
	@Override
	public Vector<String> getCommandOutputs() {
		return this.commandOutputs;
	}

	@Override
	public Vector<String> getEnumeratedPrimaryOutputs() {
		return this.enumeratedPrimaryOutputs;
	}

	@Override
	public Vector<String> getEnumeratedSecondaryOutputs() {
		return this.enumeratedSecondaryOutputs;
	}

	@Override
	public Vector<String> getOutputVariables() {
		return this.outputVariables;
	}

	public boolean areOutputVariablesCalculated() {
		return this.outputVariablesCalculated;
	}

	@Override
	public boolean areDependenciesCalculated() {
		return this.dependenciesCalculated;
	}

	// Command dependencies are top build directory relative
	@Override
	public Vector<String> getCommandDependencies() {
		return this.commandDependencies;
	}

	// Additional targets are top build directory relative
	@Override
	public Vector<String> getAdditionalTargets() {
		return this.additionalTargets;
	}

	// public Vector getEnumeratedDependencies() {
	// return enumeratedDependencies;
	// }

	@Override
	public boolean isTargetTool() {
		return this.bIsTargetTool;
	}

	/*
	 * Other Methods
	 */

	public boolean calculateInputs(ArduinoGnuMakefileGenerator makeGen, IConfiguration config,
			IResource[] projResources, ToolInfoHolder h, boolean lastChance) {
		// Get the inputs for this tool invocation
		// Note that command inputs that are also dependencies are also added to
		// the command dependencies list

		/*
		 * The priorities for determining the names of the inputs of a tool are:
		 * 1. If an option is specified, use the value of the option. 2. If a
		 * build variable is specified, use the files that have been added to
		 * the build variable as the output(s) of other build steps. 3. Use the
		 * file extensions and the resources in the project
		 */
		boolean done = true;
		Vector<String> myCommandInputs = new Vector<>(); // Inputs for the
		// tool command
		// line
		Vector<String> myCommandDependencies = new Vector<>(); // Dependencies
		// for the
		// make
		// rule
		Vector<String> myEnumeratedInputs = new Vector<>(); // Complete
		// list of
		// individual
		// inputs

		IInputType[] inTypes = this.tool.getInputTypes();
		if (inTypes != null && inTypes.length > 0) {
			for (IInputType type : inTypes) {
				Vector<String> itCommandInputs = new Vector<>(); // Inputs
				// for
				// the
				// tool
				// command
				// line
				// for
				// this
				// input-type
				Vector<String> itCommandDependencies = new Vector<>(); // Dependencies
				// for
				// the
				// make
				// rule
				// for
				// this
				// input-type
				Vector<String> itEnumeratedInputs = new Vector<>(); // Complete
				// list
				// of
				// individual
				// inputs
				// for
				// this
				// input-type
				String variable = type.getBuildVariable();
				boolean primaryInput = type.getPrimaryInput();
				boolean useFileExts = false;
				IOption option = this.tool.getOptionBySuperClassId(type.getOptionId());
				IOption assignToOption = this.tool.getOptionBySuperClassId(type.getAssignToOptionId());

				// Option?
				if (option != null) {
					try {
						List<String> inputs = new ArrayList<>();
						int optType = option.getValueType();
						if (optType == IOption.STRING) {
							inputs.add(option.getStringValue());
						} else if (optType == IOption.STRING_LIST || optType == IOption.LIBRARIES
								|| optType == IOption.OBJECTS || optType == IOption.INCLUDE_FILES
								|| optType == IOption.LIBRARY_PATHS || optType == IOption.LIBRARY_FILES
								|| optType == IOption.MACRO_FILES) {
							@SuppressWarnings("unchecked")
							List<String> valueList = (List<String>) option.getValue();
							inputs = valueList;
							this.tool.filterValues(optType, inputs);
							this.tool.filterValues(optType, inputs);
						}
						for (int j = 0; j < inputs.size(); j++) {
							String inputName = inputs.get(j);

							try {
								// try to resolve the build macros in the output
								// names

								String resolved = null;

								// does the input name contain spaces?
								// TODO: support other special characters
								if (inputName.indexOf(" ") != -1) //$NON-NLS-1$
								{
									// resolve to string
									resolved = ManagedBuildManager.getBuildMacroProvider().resolveValue(inputName, "", //$NON-NLS-1$
											" ", //$NON-NLS-1$
											IBuildMacroProvider.CONTEXT_OPTION,
											new OptionContextData(option, this.tool));
								} else {

									// resolve to makefile variable format
									resolved = ManagedBuildManager.getBuildMacroProvider().resolveValueToMakefileFormat(
											inputName, "", //$NON-NLS-1$
											" ", //$NON-NLS-1$
											IBuildMacroProvider.CONTEXT_OPTION,
											new OptionContextData(option, this.tool));
								}

								if ((resolved = resolved.trim()).length() > 0)
									inputName = resolved;
							} catch (BuildMacroException e) {// JABA is not
								// going to add
								// code
							}

							if (primaryInput) {
								itCommandDependencies.add(j, inputName);
							} else {
								itCommandDependencies.add(inputName);
							}
							// NO - itCommandInputs.add(inputName);
							// NO - itEnumeratedInputs.add(inputName);
						}
					} catch (BuildException ex) {// JABA is not going to add
						// code
					}

				} else {

					// Build Variable?
					if (variable.length() > 0) {
						String cmdVariable = variable = "$(" + variable + ")"; //$NON-NLS-1$ //$NON-NLS-2$
						itCommandInputs.add(cmdVariable);
						if (primaryInput) {
							itCommandDependencies.add(0, cmdVariable);
						} else {
							itCommandDependencies.add(cmdVariable);
						}
						// If there is an output variable with the same name,
						// get
						// the files associated with it.
						List<String> outMacroList = makeGen.getBuildVariableList(h, variable,
								ArduinoGnuMakefileGenerator.PROJECT_RELATIVE, null, true);
						if (outMacroList != null) {
							itEnumeratedInputs.addAll(outMacroList);
						} else {
							// If "last chance", then calculate using file
							// extensions below
							if (lastChance) {
								useFileExts = true;
							} else {
								done = false;
								break;
							}
						}
					}

					// Use file extensions
					if (variable.length() == 0 || useFileExts) {
						// if (type.getMultipleOfType()) {
						// Calculate EnumeratedInputs using the file extensions
						// and the resources in the project
						// Note: This is only correct for tools with
						// multipleOfType == true, but for other tools
						// it gives us an input resource for generating default
						// names
						// Determine the set of source input macros to use
						HashSet<String> handledInputExtensions = new HashSet<>();
						String[] exts = type.getSourceExtensions(this.tool);
						if (projResources != null) {
							for (IResource rc : projResources) {
								if (rc.getType() == IResource.FILE) {
									String fileExt = rc.getFileExtension();

									// fix for NPE, bugzilla 99483
									if (fileExt == null) {
										fileExt = ""; //$NON-NLS-1$
									}

									for (int k = 0; k < exts.length; k++) {
										if (fileExt.equals(exts[k])) {
											if (!useFileExts) {
												if (!handledInputExtensions.contains(fileExt)) {
													handledInputExtensions.add(fileExt);
													String buildMacro = "$(" //$NON-NLS-1$
															+ makeGen.getSourceMacroName(fileExt).toString() + ")"; //$NON-NLS-1$
													itCommandInputs.add(buildMacro);
													if (primaryInput) {
														itCommandDependencies.add(0, buildMacro);
													} else {
														itCommandDependencies.add(buildMacro);
													}
												}
											}
											if (type.getMultipleOfType() || itEnumeratedInputs.size() == 0) {
												// Add a path that is relative
												// to the project directory
												itEnumeratedInputs.add(rc.getProjectRelativePath().toOSString());
											}
											break;
										}
									}
								}
							}
						}
						// }
					}
				}

				// Get any additional inputs specified in the manifest file or
				// the project file
				IAdditionalInput[] addlInputs = type.getAdditionalInputs();
				if (addlInputs != null) {
					for (int j = 0; j < addlInputs.length; j++) {
						IAdditionalInput addlInput = addlInputs[j];
						int kind = addlInput.getKind();
						if (kind == IAdditionalInput.KIND_ADDITIONAL_INPUT
								|| kind == IAdditionalInput.KIND_ADDITIONAL_INPUT_DEPENDENCY) {
							String[] paths = addlInput.getPaths();
							if (paths != null) {
								for (int k = 0; k < paths.length; k++) {
									String path = paths[k];
									itEnumeratedInputs.add(path);
									// Translate the path from project relative
									// to build directory relative
									if (!(path.startsWith("$("))) { //$NON-NLS-1$
										IResource addlResource = this.project.getFile(path);
										if (addlResource != null) {
											IPath addlPath = addlResource.getLocation();
											if (addlPath != null) {
												path = ManagedBuildManager
														.calculateRelativePath(makeGen.getTopBuildDir(), addlPath)
														.toOSString();
											}
										}
									}
									itCommandInputs.add(path);
								}
							}
						}
					}
				}

				// If the assignToOption attribute is specified, set the
				// input(s) as the value of that option
				if (assignToOption != null && option == null) {
					try {
						int optType = assignToOption.getValueType();
						if (optType == IOption.STRING) {
							String optVal = ""; //$NON-NLS-1$
							for (int j = 0; j < itCommandInputs.size(); j++) {
								if (j != 0) {
									optVal += " "; //$NON-NLS-1$
								}
								optVal += itCommandInputs.get(j);
							}
							ManagedBuildManager.setOption(config, this.tool, assignToOption, optVal);
						} else if (optType == IOption.STRING_LIST || optType == IOption.LIBRARIES
								|| optType == IOption.OBJECTS || optType == IOption.INCLUDE_FILES
								|| optType == IOption.LIBRARY_PATHS || optType == IOption.LIBRARY_FILES
								|| optType == IOption.MACRO_FILES) {
							// TODO: do we need to do anything with undefs here?
							// Mote that when using the enumerated inputs, the
							// path(s) must be translated from project relative
							// to top build directory relative
							String[] paths = new String[itEnumeratedInputs.size()];
							for (int j = 0; j < itEnumeratedInputs.size(); j++) {
								paths[j] = itEnumeratedInputs.get(j);
								IResource enumResource = this.project.getFile(paths[j]);
								if (enumResource != null) {
									IPath enumPath = enumResource.getLocation();
									if (enumPath != null) {
										paths[j] = ManagedBuildManager
												.calculateRelativePath(makeGen.getTopBuildDir(), enumPath).toOSString();
									}
								}
							}
							ManagedBuildManager.setOption(config, this.tool, assignToOption, paths);
						} else if (optType == IOption.BOOLEAN) {
							if (itEnumeratedInputs.size() > 0) {
								ManagedBuildManager.setOption(config, this.tool, assignToOption, true);
							} else {
								ManagedBuildManager.setOption(config, this.tool, assignToOption, false);
							}
						} else if (optType == IOption.ENUMERATED || optType == IOption.TREE) {
							if (itCommandInputs.size() > 0) {
								ManagedBuildManager.setOption(config, this.tool, assignToOption,
										itCommandInputs.firstElement());
							}
						}
						itCommandInputs.removeAllElements();
						// itEnumeratedInputs.removeAllElements();
					} catch (BuildException ex) {// JABA is not going to add
						// code
					}
				}

				myCommandInputs.addAll(itCommandInputs);
				myCommandDependencies.addAll(itCommandDependencies);
				myEnumeratedInputs.addAll(itEnumeratedInputs);
			}
		} else {
			// For support of pre-CDT 3.0 integrations.
			if (this.bIsTargetTool) {
				// NOTE WELL: This only supports the case of a single
				// "target tool"
				// with the following characteristics:
				// 1. The tool consumes exactly all of the object files produced
				// by other tools in the build and produces a single output
				// 2. The target name comes from the configuration artifact name
				// The rule looks like:
				// <targ_prefix><target>.<extension>: $(OBJS) <refd_project_1
				// ... refd_project_n>
				myCommandInputs.add("$(OBJS)"); //$NON-NLS-1$
				myCommandInputs.add("$(USER_OBJS)"); //$NON-NLS-1$
				myCommandInputs.add("$(LIBS)"); //$NON-NLS-1$
			} else {
				// Rule will be generated by addRuleForSource
			}
		}

		if (done) {
			this.commandInputs.addAll(myCommandInputs);
			this.commandDependencies.addAll(0, myCommandDependencies);
			this.enumeratedInputs.addAll(myEnumeratedInputs);
			this.inputsCalculated = true;
			return true;
		}

		return false;
	}

	/*
	 * The priorities for determining the names of the outputs of a tool are: 1.
	 * If the tool is the build target and primary output, use artifact name &
	 * extension 2. If an option is specified, use the value of the option 3. If
	 * a nameProvider is specified, call it 4. If outputNames is specified, use
	 * it 5. Use the name pattern to generate a transformation macro so that the
	 * source names can be transformed into the target names using the built-in
	 * string substitution functions of <code>make</code>.
	 *
	 * NOTE: If an option is not specified and this is not the primary output
	 * type, the outputs from the type are not added to the command line
	 */
	public boolean calculateOutputs(ArduinoGnuMakefileGenerator makeGen, IConfiguration config,
			HashSet<String> handledInputExtensions, boolean lastChance) {

		boolean done = true;
		Vector<String> myCommandOutputs = new Vector<>();
		Vector<String> myEnumeratedPrimaryOutputs = new Vector<>();
		Vector<String> myEnumeratedSecondaryOutputs = new Vector<>();
		HashMap<String, List<IPath>> myOutputMacros = new HashMap<>();
		// The next two fields are used together
		Vector<String> myBuildVars = new Vector<>();
		Vector<Vector<String>> myBuildVarsValues = new Vector<>();

		// Get the outputs for this tool invocation
		IOutputType[] outTypes = this.tool.getOutputTypes();
		if (outTypes != null && outTypes.length > 0) {
			for (int i = 0; i < outTypes.length; i++) {
				Vector<String> typeEnumeratedOutputs = new Vector<>();
				IOutputType type = outTypes[i];
				String outputPrefix = type.getOutputPrefix();

				// Resolve any macros in the outputPrefix
				// Note that we cannot use file macros because if we do a clean
				// we need to know the actual name of the file to clean, and
				// cannot use any builder variables such as $@. Hence we use the
				// next best thing, i.e. configuration context.

				if (config != null) {

					try {
						outputPrefix = ManagedBuildManager.getBuildMacroProvider().resolveValueToMakefileFormat(
								outputPrefix, "", //$NON-NLS-1$
								" ", //$NON-NLS-1$
								IBuildMacroProvider.CONTEXT_CONFIGURATION, config);
					}

					catch (BuildMacroException e) {// JABA is not going to add
						// code
					}
				}

				String variable = type.getBuildVariable();
				boolean multOfType = type.getMultipleOfType();
				boolean primaryOutput = (type == this.tool.getPrimaryOutputType());
				IOption option = this.tool.getOptionBySuperClassId(type.getOptionId());
				IManagedOutputNameProvider nameProvider = type.getNameProvider();
				String[] outputNames = type.getOutputNames();

				// 1. If the tool is the build target and this is the primary
				// output,
				// use artifact name & extension
				if (this.bIsTargetTool && primaryOutput) {
					String outputName = outputPrefix + this.targetName;
					if (this.targetExt.length() > 0) {
						outputName += (DOT + this.targetExt);
					}
					myCommandOutputs.add(outputName);
					typeEnumeratedOutputs.add(outputName);
					// But this doesn't use any output macro...
				} else
				// 2. If an option is specified, use the value of the option
				if (option != null) {
					try {
						List<String> outputs = new ArrayList<>();
						int optType = option.getValueType();
						if (optType == IOption.STRING) {
							outputs.add(outputPrefix + option.getStringValue());
						} else if (optType == IOption.STRING_LIST || optType == IOption.LIBRARIES
								|| optType == IOption.OBJECTS || optType == IOption.INCLUDE_FILES
								|| optType == IOption.LIBRARY_PATHS || optType == IOption.LIBRARY_FILES
								|| optType == IOption.MACRO_FILES) {
							@SuppressWarnings("unchecked")
							List<String> value = (List<String>) option.getValue();
							outputs = value;
							this.tool.filterValues(optType, outputs);
							// Add outputPrefix to each if necessary
							if (outputPrefix.length() > 0) {
								for (int j = 0; j < outputs.size(); j++) {
									outputs.set(j, outputPrefix + outputs.get(j));
								}
							}
						}
						for (int j = 0; j < outputs.size(); j++) {
							String outputName = outputs.get(j);
							try {
								// try to resolve the build macros in the output
								// names
								String resolved = ManagedBuildManager.getBuildMacroProvider()
										.resolveValueToMakefileFormat(outputName, "", //$NON-NLS-1$
												" ", //$NON-NLS-1$
												IBuildMacroProvider.CONTEXT_OPTION,
												new OptionContextData(option, this.tool));
								if ((resolved = resolved.trim()).length() > 0)
									outputs.set(j, resolved);
							} catch (BuildMacroException e) {// JABA is not
								// going to add
								// code
							}
						}

						// NO - myCommandOutputs.addAll(outputs);
						typeEnumeratedOutputs.addAll(outputs);
						if (variable.length() > 0) {
							List<IPath> outputPaths = new ArrayList<>();
							for (int j = 0; j < outputs.size(); j++) {
								outputPaths.add(Path.fromOSString(outputs.get(j)));
							}
							if (myOutputMacros.containsKey(variable)) {
								List<IPath> currList = myOutputMacros.get(variable);
								currList.addAll(outputPaths);
								myOutputMacros.put(variable, currList);
							} else {
								myOutputMacros.put(variable, outputPaths);
							}
						}
					} catch (BuildException ex) {// JABA is not going to add
						// code
					}
				} else
				// 3. If a nameProvider is specified, call it
				if (nameProvider != null) {
					// The inputs must have been calculated before we can do
					// this
					IPath[] outNames = null;
					if (!this.inputsCalculated) {
						done = false;
					} else {
						Vector<String> inputs = getEnumeratedInputs();
						IPath[] inputPaths = new IPath[inputs.size()];
						for (int j = 0; j < inputPaths.length; j++) {
							inputPaths[j] = Path.fromOSString(inputs.get(j));
						}
						// JABA: I inserted this code to cater for bin or hex
						// extensions based on the board. Basically you need the
						// Project
						// to get to information to know whether this is a sam
						// or avr project (having it in the platform file would
						// be easier)
						// To make it as much compliant as possible with the
						// "CDT way" I added a getOutputNames which takes al the
						// info needed to
						// know the correct output name
						// if (inputPaths.length == 0) {
						try {
							IManagedOutputNameProviderJaba newNameProvider = (IManagedOutputNameProviderJaba) nameProvider;
							outNames = newNameProvider.getOutputNames(this.project, config, this.tool, inputPaths);
						} catch (Exception e) {
							// The provided class is not a
							// IManagedOutputNameProviderJaba class;
							Common.log(new Status(IStatus.ERROR, Const.CORE_PLUGIN_ID,
									"The provided class is not of type IManagedOutputNameProviderJaba", e)); //$NON-NLS-1$
						}
						// } else {
						// outNames = nameProvider.getOutputNames(tool,
						// inputPaths);
						// }
						// JABA end of insertion
						if (outNames != null) {
							for (int j = 0; j < outNames.length; j++) {
								String outputName = outNames[j].toOSString();
								try {
									// try to resolve the build macros in the
									// output names
									String resolved = ManagedBuildManager.getBuildMacroProvider()
											.resolveValueToMakefileFormat(outputName, "", //$NON-NLS-1$
													" ", //$NON-NLS-1$
													IBuildMacroProvider.CONTEXT_CONFIGURATION, config);
									if ((resolved = resolved.trim()).length() > 0) {
										outputName = resolved;
										outNames[j] = Path.fromOSString(resolved);
									}
								} catch (BuildMacroException e) {// JABA is not
									// going to add
									// code
								}

								if (primaryOutput) {
									myCommandOutputs.add(outputName);
								}
								typeEnumeratedOutputs.add(outputName);
							}
						}
					}
					if (variable.length() > 0 && outNames != null) {
						if (myOutputMacros.containsKey(variable)) {
							List<IPath> currList = myOutputMacros.get(variable);
							currList.addAll(Arrays.asList(outNames));
							myOutputMacros.put(variable, currList);
						} else {
							myOutputMacros.put(variable, new ArrayList<>(Arrays.asList(outNames)));
						}
					}
				} else
				// 4. If outputNames is specified, use it
				if (outputNames != null) {
					if (outputNames.length > 0) {
						for (int j = 0; j < outputNames.length; j++) {
							String outputName = outputNames[j];
							try {
								// try to resolve the build macros in the output
								// names
								String resolved = ManagedBuildManager.getBuildMacroProvider()
										.resolveValueToMakefileFormat(outputName, "", //$NON-NLS-1$
												" ", //$NON-NLS-1$
												IBuildMacroProvider.CONTEXT_OPTION,
												new OptionContextData(option, this.tool));
								if ((resolved = resolved.trim()).length() > 0)
									outputNames[j] = resolved;
							} catch (BuildMacroException e) {// JABA is not
								// going to add
								// code
							}
						}
						List<String> namesList = Arrays.asList(outputNames);
						if (primaryOutput) {
							myCommandOutputs.addAll(namesList);
						}
						typeEnumeratedOutputs.addAll(namesList);
						if (variable.length() > 0) {
							List<IPath> outputPaths = new ArrayList<>();
							for (int j = 0; j < namesList.size(); j++) {
								outputPaths.add(Path.fromOSString(namesList.get(j)));
							}
							if (myOutputMacros.containsKey(variable)) {
								List<IPath> currList = myOutputMacros.get(variable);
								currList.addAll(outputPaths);
								myOutputMacros.put(variable, currList);
							} else {
								myOutputMacros.put(variable, outputPaths);
							}
						}
					}
				} else {
					// 5. Use the name pattern to generate a transformation
					// macro
					// so that the source names can be transformed into the
					// target names
					// using the built-in string substitution functions of
					// <code>make</code>.
					if (multOfType) {
						// This case is not handled - a nameProvider or
						// outputNames must be specified
						List<String> errList = new ArrayList<>();
						errList.add(ManagedMakeMessages.getResourceString("MakefileGenerator.error.no.nameprovider")); //$NON-NLS-1$
						myCommandOutputs.addAll(errList);
					} else {
						String namePattern = type.getNamePattern();
						if (namePattern == null || namePattern.length() == 0) {
							namePattern = outputPrefix + IManagedBuilderMakefileGenerator.WILDCARD;
							String outExt = (type.getOutputExtensions(this.tool))[0];
							if (outExt != null && outExt.length() > 0) {
								namePattern += DOT + outExt;
							}
						} else if (outputPrefix.length() > 0) {
							namePattern = outputPrefix + namePattern;
						}

						// Calculate the output name
						// The inputs must have been calculated before we can do
						// this
						if (!this.inputsCalculated) {
							done = false;
						} else {
							Vector<String> inputs = getEnumeratedInputs();
							String fileName;
							if (inputs.size() > 0) {
								// Get the input file name
								fileName = (Path.fromOSString(inputs.get(0))).removeFileExtension().lastSegment();
								// Check if this is a build macro. If so, use
								// the raw macro name.
								if (fileName.startsWith("$(") && fileName.endsWith(")")) { //$NON-NLS-1$ //$NON-NLS-2$
									fileName = fileName.substring(2, fileName.length() - 1);
								}
							} else {
								fileName = "default"; //$NON-NLS-1$
							}
							// Replace the % with the file name
							if (primaryOutput) {
								myCommandOutputs.add(namePattern.replaceAll("%", fileName)); //$NON-NLS-1$
							}
							typeEnumeratedOutputs.add(namePattern.replaceAll("%", fileName)); //$NON-NLS-1$
							if (variable.length() > 0) {
								List<IPath> outputs = new ArrayList<>();
								outputs.add(Path.fromOSString(fileName));
								if (myOutputMacros.containsKey(variable)) {
									List<IPath> currList = myOutputMacros.get(variable);
									currList.addAll(outputs);
									myOutputMacros.put(variable, currList);
								} else {
									myOutputMacros.put(variable, outputs);
								}
							}
						}
					}
				}
				if (variable.length() > 0) {
					myBuildVars.add(variable);
					myBuildVarsValues.add(typeEnumeratedOutputs);
				}
				if (primaryOutput) {
					myEnumeratedPrimaryOutputs.addAll(typeEnumeratedOutputs);
				} else {
					myEnumeratedSecondaryOutputs.addAll(typeEnumeratedOutputs);
				}
			}
		} else {
			if (this.bIsTargetTool) {
				String outputPrefix = this.tool.getOutputPrefix();
				String outputName = outputPrefix + this.targetName;
				if (this.targetExt.length() > 0) {
					outputName += (DOT + this.targetExt);
				}
				myCommandOutputs.add(outputName);
				myEnumeratedPrimaryOutputs.add(outputName);
			} else {
				// For support of pre-CDT 3.0 integrations.
				// NOTE WELL: This only supports the case of a single
				// "target tool"
				// that consumes exactly all of the object files, $OBJS,
				// produced
				// by other tools in the build and produces a single output
			}
		}

		// Add the output macros of this tool to the buildOutVars map
		Set<Entry<String, List<IPath>>> entrySet = myOutputMacros.entrySet();
		for (Entry<String, List<IPath>> entry : entrySet) {
			String macroName = entry.getKey();
			List<IPath> newMacroValue = entry.getValue();
			Map<String, List<IPath>> map = makeGen.getBuildOutputVars();
			if (map.containsKey(macroName)) {
				List<IPath> macroValue = map.get(macroName);
				macroValue.addAll(newMacroValue);
				map.put(macroName, macroValue);
			} else {
				map.put(macroName, newMacroValue);
			}
		}
		this.outputVariablesCalculated = true;

		if (done) {
			this.commandOutputs.addAll(myCommandOutputs);
			this.enumeratedPrimaryOutputs.addAll(myEnumeratedPrimaryOutputs);
			this.enumeratedSecondaryOutputs.addAll(myEnumeratedSecondaryOutputs);
			this.outputVariables.addAll(myOutputMacros.keySet());
			this.outputsCalculated = true;
			for (int i = 0; i < myBuildVars.size(); i++) {
				makeGen.addMacroAdditionFiles(makeGen.getTopBuildOutputVars(), myBuildVars.get(i),
						myBuildVarsValues.get(i));
			}
			return true;
		}

		return false;
	}

	private boolean callDependencyCalculator(ArduinoGnuMakefileGenerator makeGen, IConfiguration config,
			HashSet<String> handledInputExtensions, IManagedDependencyGeneratorType depGen, String[] extensionsList,
			Vector<String> myCommandDependencies, HashMap<String, List<IPath>> myOutputMacros,
			Vector<String> myAdditionalTargets, ToolInfoHolder h, boolean _done) {
		boolean done = _done;
		int calcType = depGen.getCalculatorType();
		switch (calcType) {
		case IManagedDependencyGeneratorType.TYPE_COMMAND:
		case IManagedDependencyGeneratorType.TYPE_BUILD_COMMANDS:
			// iterate over all extensions that the tool knows how to handle
			for (int i = 0; i < extensionsList.length; i++) {
				String extensionName = extensionsList[i];

				// Generated files should not appear in the list.
				if (!makeGen.getOutputExtensions(h).contains(extensionName)
						&& !handledInputExtensions.contains(extensionName)) {
					handledInputExtensions.add(extensionName);
					String depExt = IManagedBuilderMakefileGenerator.DEP_EXT;
					if (calcType == IManagedDependencyGeneratorType.TYPE_BUILD_COMMANDS) {
						IManagedDependencyGenerator2 depGen2 = (IManagedDependencyGenerator2) depGen;
						String xt = depGen2.getDependencyFileExtension(config, this.tool);
						if (xt != null && xt.length() > 0)
							depExt = xt;
					}
					String depsMacroEntry = calculateSourceMacro(makeGen, extensionName, depExt,
							IManagedBuilderMakefileGenerator.WILDCARD);

					List<IPath> depsList = new ArrayList<>();
					depsList.add(Path.fromOSString(depsMacroEntry));
					String depsMacro = makeGen.getDepMacroName(extensionName).toString();
					if (myOutputMacros.containsKey(depsMacro)) {
						List<IPath> currList = myOutputMacros.get(depsMacro);
						currList.addAll(depsList);
						myOutputMacros.put(depsMacro, currList);
					} else {
						myOutputMacros.put(depsMacro, depsList);
					}
				}
			}
			break;

		case IManagedDependencyGeneratorType.TYPE_INDEXER:
		case IManagedDependencyGeneratorType.TYPE_EXTERNAL:
		case IManagedDependencyGeneratorType.TYPE_CUSTOM:
			// The inputs must have been calculated before we can do this
			if (!this.inputsCalculated) {
				done = false;
			} else {
				Vector<String> inputs = getEnumeratedInputs();

				if (calcType == IManagedDependencyGeneratorType.TYPE_CUSTOM) {
					IManagedDependencyGenerator2 depGen2 = (IManagedDependencyGenerator2) depGen;
					IManagedDependencyInfo depInfo = null;
					for (int i = 0; i < inputs.size(); i++) {

						depInfo = depGen2.getDependencySourceInfo(Path.fromOSString(inputs.get(i)), config, this.tool,
								makeGen.getBuildWorkingDir());

						if (depInfo instanceof IManagedDependencyCalculator) {
							IManagedDependencyCalculator depCalc = (IManagedDependencyCalculator) depInfo;
							IPath[] depPaths = depCalc.getDependencies();
							if (depPaths != null) {
								for (int j = 0; j < depPaths.length; j++) {
									if (!depPaths[j].isAbsolute()) {
										// Convert from project relative to
										// build directory relative
										IPath absolutePath = this.project.getLocation().append(depPaths[j]);
										depPaths[j] = ManagedBuildManager
												.calculateRelativePath(makeGen.getTopBuildDir(), absolutePath);
									}
									myCommandDependencies.add(depPaths[j].toOSString());
								}
							}
							IPath[] targetPaths = depCalc.getAdditionalTargets();
							if (targetPaths != null) {
								for (int j = 0; j < targetPaths.length; j++) {
									myAdditionalTargets.add(targetPaths[j].toOSString());
								}
							}
						}
					}
				} else {
					IManagedDependencyGenerator oldDepGen = (IManagedDependencyGenerator) depGen;
					for (String input : inputs) {
						IResource[] outNames = oldDepGen.findDependencies(this.project.getFile(input), this.project);
						if (outNames != null) {
							for (IResource outName : outNames) {
								myCommandDependencies.add(outName.toString());
							}
						}
					}
				}
			}
			break;

		default:
			break;
		}

		return done;
	}

	/**
	 * @param lastChance
	 */
	public boolean calculateDependencies(ArduinoGnuMakefileGenerator makeGen, IConfiguration config,
			HashSet<String> handledInputExtensions, ToolInfoHolder h, boolean lastChance) {
		// Get the dependencies for this tool invocation
		boolean done = true;
		Vector<String> myCommandDependencies = new Vector<>();
		Vector<String> myAdditionalTargets = new Vector<>();
		// Vector myEnumeratedDependencies = new Vector();
		HashMap<String, List<IPath>> myOutputMacros = new HashMap<>();

		IInputType[] inTypes = this.tool.getInputTypes();
		if (inTypes != null && inTypes.length > 0) {
			for (int i = 0; i < inTypes.length; i++) {
				IInputType type = inTypes[i];

				// Handle dependencies from the dependencyCalculator
				IManagedDependencyGeneratorType depGen = type.getDependencyGenerator();
				String[] extensionsList = type.getSourceExtensions(this.tool);
				if (depGen != null) {
					done = callDependencyCalculator(makeGen, config, handledInputExtensions, depGen, extensionsList,
							myCommandDependencies, myOutputMacros, myAdditionalTargets, h, done);
				}

				// Add additional dependencies specified in AdditionalInput
				// elements
				IAdditionalInput[] addlInputs = type.getAdditionalInputs();
				if (addlInputs != null && addlInputs.length > 0) {
					for (int j = 0; j < addlInputs.length; j++) {
						IAdditionalInput addlInput = addlInputs[j];
						int kind = addlInput.getKind();
						if (kind == IAdditionalInput.KIND_ADDITIONAL_DEPENDENCY
								|| kind == IAdditionalInput.KIND_ADDITIONAL_INPUT_DEPENDENCY) {
							String[] paths = addlInput.getPaths();
							if (paths != null) {
								for (int k = 0; k < paths.length; k++) {
									// Translate the path from project relative
									// to
									// build directory relative
									String path = paths[k];
									if (!(path.startsWith("$("))) { //$NON-NLS-1$
										IResource addlResource = this.project.getFile(path);
										if (addlResource != null) {
											IPath addlPath = addlResource.getLocation();
											if (addlPath != null) {
												path = ManagedBuildManager
														.calculateRelativePath(makeGen.getTopBuildDir(), addlPath)
														.toOSString();
											}
										}
									}
									myCommandDependencies.add(path);
									// myEnumeratedInputs.add(path);
								}
							}
						}
					}
				}
			}
		} else {
			if (this.bIsTargetTool) {
				// For support of pre-CDT 3.0 integrations.
				// NOTE WELL: This only supports the case of a single
				// "target tool"
				// with the following characteristics:
				// 1. The tool consumes exactly all of the object files produced
				// by other tools in the build and produces a single output
				// 2. The target name comes from the configuration artifact name
				// The rule looks like:
				// <targ_prefix><target>.<extension>: $(OBJS) <refd_project_1
				// ... refd_project_n>
				myCommandDependencies.add("$(OBJS)"); //$NON-NLS-1$
				myCommandDependencies.add("$(USER_OBJS)"); //$NON-NLS-1$
			} else {
				// Handle dependencies from the dependencyCalculator
				IManagedDependencyGeneratorType depGen = this.tool.getDependencyGenerator();
				String[] extensionsList = this.tool.getAllInputExtensions();
				if (depGen != null) {
					done = callDependencyCalculator(makeGen, config, handledInputExtensions, depGen, extensionsList,
							myCommandDependencies, myOutputMacros, myAdditionalTargets, h, done);
				}

			}
		}

		// Add the output macros of this tool to the buildOutVars map
		Set<Entry<String, List<IPath>>> entrySet = myOutputMacros.entrySet();
		for (Entry<String, List<IPath>> entry : entrySet) {
			String macroName = entry.getKey();
			List<IPath> newMacroValue = entry.getValue();
			Map<String, List<IPath>> map = makeGen.getBuildOutputVars();
			if (map.containsKey(macroName)) {
				List<IPath> macroValue = map.get(macroName);
				macroValue.addAll(newMacroValue);
				map.put(macroName, macroValue);
			} else {
				map.put(macroName, newMacroValue);
			}
		}

		if (done) {
			this.commandDependencies.addAll(myCommandDependencies);
			this.additionalTargets.addAll(myAdditionalTargets);
			// enumeratedDependencies.addAll(myEnumeratedDependencies);
			this.dependenciesCalculated = true;
			return true;
		}

		return false;
	}

	/*
	 * Calculate the source macro for the given extension
	 */
	protected String calculateSourceMacro(ArduinoGnuMakefileGenerator makeGen, String srcExtensionName,
			String outExtensionName, String wildcard) {
		StringBuffer macroName = makeGen.getSourceMacroName(srcExtensionName);
		String OptDotExt = ""; //$NON-NLS-1$
		if (outExtensionName != null) {
			OptDotExt = DOT + outExtensionName;
		} else if (this.tool.getOutputExtension(srcExtensionName) != "") //$NON-NLS-1$
			OptDotExt = DOT + this.tool.getOutputExtension(srcExtensionName);

		// create rule of the form
		// OBJS = $(macroName1: ../%.input1=%.output1) ... $(macroNameN:
		// ../%.inputN=%.outputN)
		StringBuffer objectsBuffer = new StringBuffer();
		objectsBuffer.append(IManagedBuilderMakefileGenerator.WHITESPACE + "$(" + macroName + //$NON-NLS-1$
				IManagedBuilderMakefileGenerator.COLON + IManagedBuilderMakefileGenerator.ROOT
				+ IManagedBuilderMakefileGenerator.SEPARATOR + IManagedBuilderMakefileGenerator.WILDCARD + DOT
				+ srcExtensionName + "=" + wildcard + OptDotExt + ")"); //$NON-NLS-1$ //$NON-NLS-2$
		return objectsBuffer.toString();
	}

}
