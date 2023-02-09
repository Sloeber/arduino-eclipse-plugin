/*******************************************************************************
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 
 *  
 *  This class is an pretty standard implementation of the IOption interface
 *  What is important to note is that the value returned by getDefaultValue is what should be provided
 *  as value in getCommandLineContribution
 *  The value is as follows
 *  BOOLEAN string that when provided to Boolean.parseBoolean returns the wanted boolean value
 *  STRING the string itself
 *  ENUMERATED The ID of the selected enumeratedOptionValue
 *  TREE Not tested so will probably not work yet
 *  STRING_LIST: values concatenated with \r\n
 *  INCLUDE_PATH: values concatenated with \r\n
 *  PREPROCESSOR_SYMBOLS: values concatenated with \r\n
 *  LIBRARIES: values concatenated with \r\n
 *  OBJECTS: values concatenated with \r\n
 *  INCLUDE_FILES: values concatenated with \r\n
 *  LIBRARY_PATH: values concatenated with \r\n
 *  LIBRARY_FILES: values concatenated with \r\n
 *  MACRO_FILES: values concatenated with \r\n
 *  UNDEF_INCLUDE_PATH: values concatenated with \r\n
 *  UNDEF_PREPROCESSOR_SYMBOLS: values concatenated with \r\n
 *  UNDEF_INCLUDE_FILES: values concatenated with \r\n
 *  UNDEF_LIBRARY_PATHS: values concatenated with \r\n
 *  UNDEF_LIBRARY_FILES: values concatenated with \r\n
 *  UNDEF_MACRO_FILES: values concatenated with \r\n

 *  
 *******************************************************************************/
package io.sloeber.schema.internal;

import static io.sloeber.autoBuild.core.Messages.*;
import static io.sloeber.autoBuild.integration.Const.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.eclipse.cdt.core.settings.model.ICConfigurationDescription;
import org.eclipse.cdt.core.settings.model.ICStorageElement;
import org.eclipse.cdt.internal.core.SafeStringInterner;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import io.sloeber.autoBuild.api.BuildException;
import io.sloeber.autoBuild.api.OptionStringValue;
import io.sloeber.autoBuild.core.Activator;
import io.sloeber.autoBuild.extensionPoint.IOptionCommandGenerator;
import io.sloeber.autoBuild.extensionPoint.IOptionDefaultValueGenerator;
import io.sloeber.autoBuild.extensionPoint.providers.AutoBuildCommon;
import io.sloeber.autoBuild.integration.AutoBuildConfigurationData;
import io.sloeber.schema.api.IOption;
import io.sloeber.schema.api.IOptionCategory;
import io.sloeber.schema.api.ISchemaObject;

public class Option extends SchemaObject implements IOption {
	// Static default return values
	public static final String[] EMPTY_STRING_ARRAY = new String[0];
	public static final OptionStringValue[] EMPTY_LV_ARRAY = new OptionStringValue[0];
	private static final String EMPTY_QUOTED_STRING = "\"\""; //$NON-NLS-1$
	private static final String STRING_SEPERATOR = "\n\r";//$NON-NLS-1$
	private static final String STRING_SEPARATOR_REGEX = Pattern.quote(STRING_SEPERATOR);

	private String[] modelCategoryId;
	private String[] modelResFilterStr;
	private String[] modelValueTypeStr;
	private String[] modelBrowseTypeStr;
	private String[] modelBrowseFilterPath;
	private String[] modelBrowseFilterExtensionsStr;
	private String[] modelDefaultValueString;
	private String[] modelDefaultValueGeneratorStr;
	private String[] modelCommand;
	private String[] modelCommandGeneratorStr;
	private String[] modelCommandFalse;
	private String[] modelIsForSD;
	private String[] modelTip;
	private String[] modelContextId;
	private String[] modelValueHandlerString;
	private String[] modelValueHandlerExtraArgument;
	private String[] modelApplicabilityCalculatorStr;
	private String[] modelFieldEditorId;
	private String[] modelFieldEditorExtraArgument;

	private ISchemaObject myParent;
	private int browseType = BROWSE_NONE;
	private List<OptionStringValue> myBuiltIns;
	private IOptionCategory category;
	private IOptionCommandGenerator commandGenerator;
	private boolean isForScannerDiscovery;
	private String myDefaultStringValue = null;
	private IOptionDefaultValueGenerator defaultValueGenerator;
	private int valueType;
	private int resourceFilter;
	private TreeRoot myTreeRoot;
	private Map<String, EnumOptionValue> myEnumOptionValues = new LinkedHashMap<>();

	/**
	 * This constructor is called to create an option defined by an extension point
	 * in a plugin manifest file
	 *
	 * @param parent  The parent of this option
	 * @param element The option definition from the manifest file or a dynamic
	 *                element provider
	 */
	public Option(ISchemaObject parent, IExtensionPoint root, IConfigurationElement element) {
		myParent = parent;

		loadNameAndID(root, element);

		modelCategoryId = getAttributes(CATEGORY);
		modelResFilterStr = getAttributes(RESOURCE_FILTER);
		modelValueTypeStr = getAttributes(VALUE_TYPE);
		modelBrowseTypeStr = getAttributes(BROWSE_TYPE);
		modelBrowseFilterPath = getAttributes(BROWSE_FILTER_PATH);
		modelBrowseFilterExtensionsStr = getAttributes(BROWSE_FILTER_EXTENSIONS);
		modelDefaultValueString = getAttributes(DEFAULT_VALUE);
		modelDefaultValueGeneratorStr = getAttributes(DEFAULTVALUE_GENERATOR);
		modelCommand = getAttributes(COMMAND);
		modelCommandGeneratorStr = getAttributes(COMMAND_GENERATOR);
		modelCommandFalse = getAttributes(COMMAND_FALSE);
		modelIsForSD = getAttributes(USE_BY_SCANNER_DISCOVERY);
		modelTip = getAttributes(TOOL_TIP);
		modelContextId = getAttributes(CONTEXT_ID);
		modelValueHandlerString = getAttributes(VALUE_HANDLER);
		modelValueHandlerExtraArgument = getAttributes(VALUE_HANDLER_EXTRA_ARGUMENT);
		modelApplicabilityCalculatorStr = getAttributes(APPLICABILITY_CALCULATOR);
		modelFieldEditorId = getAttributes(FIELD_EDITOR_ID);
		modelFieldEditorExtraArgument = getAttributes(FIELD_EDITOR_EXTRA_ARGUMENT);

		valueType = ValueTypeStrToInt(modelValueTypeStr[SUPER]);

		switch (valueType) {
		case ENUMERATED: {
			IConfigurationElement[] enumElements = element.getChildren(ENUM_VALUE);
			for (IConfigurationElement curEnumElement : enumElements) {
				EnumOptionValue neEnumValue = new EnumOptionValue(curEnumElement);
				myEnumOptionValues.put(neEnumValue.getID(), neEnumValue);
			}
			break;
		}
		case TREE: {

			IConfigurationElement[] treeRootConfigs = element.getChildren(TREE_ROOT);
			switch (treeRootConfigs.length) {
			case 1:
				IConfigurationElement treeRootConfig = treeRootConfigs[0];
				myTreeRoot = new TreeRoot(treeRootConfig);
				break;
			case 0:
				myTreeRoot = null;
				break;
			default:
				myTreeRoot = null;
				System.err.println("Wrong number of children " + TREE_ROOT + BLANK + treeRootConfigs.length); //$NON-NLS-1$
			}
			break;
		}

		}

		resolveFields();

	}

	private void resolveFields() {
		isForScannerDiscovery = Boolean.parseBoolean(modelIsForSD[SUPER]);

		browseType = resolveBrowseType(modelBrowseTypeStr[SUPER]);
		resourceFilter = resolveResourceFilter(modelResFilterStr[SUPER]);
		if (defaultValueGenerator != null) {
			myDefaultStringValue = defaultValueGenerator.generateDefaultValue(this);
		} else {

			if (myTreeRoot != null) {
				myDefaultStringValue = myTreeRoot.getDefaultValueString();
			}
			if (myEnumOptionValues.size() > 0) {
				for (EnumOptionValue cur : myEnumOptionValues.values()) {
					if (cur.isDefault()) {
						myDefaultStringValue = cur.getID();
						break;
					}
					if (myDefaultStringValue == null) {
						if (cur.getCommandLIneDistribution().isBlank()) {
							myDefaultStringValue = EMPTY_STRING;
						} else {
							myDefaultStringValue = cur.getID();
						}
					}
				}
			}
			if ((!modelDefaultValueString[SUPER].isBlank())||(myDefaultStringValue==null)) {
				myDefaultStringValue = modelDefaultValueString[SUPER];
			}
		}
	}

	private static int resolveResourceFilter(String string) {
		switch (string) {
		case ALL:
			return FILTER_ALL;
		case FILE:
			return FILTER_FILE;
		case PROJECT:
			return FILTER_PROJECT;
		}
		return FILTER_NONE;
	}

	private static int resolveBrowseType(String string) {
		switch (string) {
		case NONE:
			return BROWSE_NONE;
		case FILE:
			return BROWSE_FILE;
		case DIR:
			return BROWSE_DIR;
		}
		return BROWSE_NONE;
	}

	private int ValueTypeStrToInt(String valueTypeStr) {
		switch (valueTypeStr) {
		case TYPE_STRING:
			return STRING;
		case TYPE_STR_LIST:
			return STRING_LIST;
		case TYPE_BOOL:
			return BOOLEAN;
		case TYPE_ENUM:
			return ENUMERATED;
		case TYPE_INC_PATH:
			return INCLUDE_PATH;
		case TYPE_LIB:
			return LIBRARIES;
		case TYPE_USER_OBJS:
			return OBJECTS;
		case TYPE_DEFINED_SYMBOLS:
			return PREPROCESSOR_SYMBOLS;
		case TYPE_LIB_PATHS:
			return LIBRARY_PATHS;
		case TYPE_LIB_FILES:
			return LIBRARY_FILES;
		case TYPE_INC_FILES:
			return INCLUDE_FILES;
		case TYPE_SYMBOL_FILES:
			return MACRO_FILES;
		case TYPE_UNDEF_INC_PATH:
			return UNDEF_INCLUDE_PATH;
		case TYPE_UNDEF_DEFINED_SYMBOLS:
			return UNDEF_PREPROCESSOR_SYMBOLS;
		case TYPE_UNDEF_LIB_PATHS:
			return UNDEF_LIBRARY_PATHS;
		case TYPE_UNDEF_LIB_FILES:
			return UNDEF_LIBRARY_FILES;
		case TYPE_UNDEF_INC_FILES:
			return UNDEF_INCLUDE_FILES;
		case TYPE_UNDEF_SYMBOL_FILES:
			return UNDEF_MACRO_FILES;
		case TYPE_TREE:
			return TREE;
		default:
			Activator.log(new Status(IStatus.ERROR, Activator.getId(),
					"Invalid option type=\"" + valueTypeStr + "\" specified for option " + getId())); //$NON-NLS-1$ //$NON-NLS-2$
			// This was the CDT 2.0 default
			return PREPROCESSOR_SYMBOLS;
		}
	}

	@Override
	public ISchemaObject getParent() {
		return myParent;
	}

	@Override
	public int getBrowseType() {
		return browseType;
	}

	@Override
	public int getResourceFilter() {
		return resourceFilter;
	}

	@Override
	public String[] getBuiltIns() {
		// Return the list of built-ins as an array
		List<OptionStringValue> list = myBuiltIns;
		List<String> valueList = listValueListToValueList(list);

		if (valueList == null) {
			return EMPTY_STRING_ARRAY;
		}
		return valueList.toArray(new String[valueList.size()]);
	}

	@Override
	public IOptionCategory getCategory() {
		return category;
	}

	@Override
	public boolean isForScannerDiscovery() {
		return isForScannerDiscovery;
	}

	@Override
	public String getToolTip() {
		return modelTip[SUPER];
	}

	@Override
	public String getContextId() {
		return modelContextId[SUPER];
	}

	@Override
	public String getEnumName(String commandID) throws BuildException {
		return getName(commandID);
	}

	@Override
	public String getName(String commandID) throws BuildException {
		// Sanity
		if (commandID == null) {
			return EMPTY_STRING;
		}

		// Does this option instance have the list of values?
		// if (applicableValuesList == null) {
		// return EMPTY_STRING;
		// }
		if (getValueType() != ENUMERATED) {
			throw new BuildException(Option_error_bad_value_type);
		}

		// First check for the command in ID->name map
		// String nameFromMap = namesMap.get(commandID);
		// if (nameFromMap == null) {
		// // This may be a 1.2 project or plugin manifest. If so, the argument is the
		// human readable
		// // name of the enumeration.
		// nameFromMap = commandID;
		// }
		// return nameFromMap;
		return commandID;
	}

	@Override
	public int getValueType() throws BuildException {
		return valueType;
	}

	private static List<String> listValueListToValueList(List<OptionStringValue> list) {
		if (list == null) {
			return null;
		}

		List<String> valueList = new ArrayList<>(list.size());
		for (int i = 0; i < list.size(); i++) {
			OptionStringValue el = list.get(i);
			valueList.add(el.getValue());
		}
		return valueList;
	}

	@Override
	public String getFieldEditorId() {
		return modelFieldEditorId[SUPER];
	}

	@Override
	public String getFieldEditorExtraArgument() {
		return modelFieldEditorExtraArgument[SUPER];
	}

	@Override
	public int getBasicValueType() throws BuildException {
		switch (getValueType()) {
		case IOption.BOOLEAN:
			return IOption.BOOLEAN;
		case IOption.STRING:
			return IOption.STRING;
		case IOption.ENUMERATED:
			return IOption.ENUMERATED;
		case IOption.TREE:
			return IOption.TREE;
		default:
			return IOption.STRING_LIST;
		}
	}

	public static int getOppositeType(int type) {
		switch (type) {
		case INCLUDE_PATH:
			return UNDEF_INCLUDE_PATH;
		case PREPROCESSOR_SYMBOLS:
			return UNDEF_PREPROCESSOR_SYMBOLS;
		case INCLUDE_FILES:
			return UNDEF_INCLUDE_FILES;
		case LIBRARY_PATHS:
			return UNDEF_LIBRARY_PATHS;
		case LIBRARY_FILES:
			return UNDEF_LIBRARY_FILES;
		case MACRO_FILES:
			return UNDEF_MACRO_FILES;
		case UNDEF_INCLUDE_PATH:
			return INCLUDE_PATH;
		case UNDEF_PREPROCESSOR_SYMBOLS:
			return PREPROCESSOR_SYMBOLS;
		case UNDEF_INCLUDE_FILES:
			return INCLUDE_FILES;
		case UNDEF_LIBRARY_PATHS:
			return LIBRARY_PATHS;
		case UNDEF_LIBRARY_FILES:
			return LIBRARY_FILES;
		case UNDEF_MACRO_FILES:
			return MACRO_FILES;
		}
		return 0;
	}

	public static class TreeRoot implements ITreeRoot {
		private boolean selectLeafOnly = true;
		private String myName;
		private String myID;
		private Map<String, TreeOption> myTreeOptions = new HashMap<>();

		TreeRoot(IConfigurationElement element) {
			myName = element.getAttribute(NAME);
			myID = element.getAttribute(ID);
			String leaf = element.getAttribute(SELECT_LEAF_ONLY);
			if (leaf != null) {
				selectLeafOnly = Boolean.parseBoolean(leaf);
			}
			for (IConfigurationElement curElement : element.getChildren(TREE_VALUE)) {
				TreeOption newTreeOption = new TreeOption(curElement);
				myTreeOptions.put(newTreeOption.getID(), newTreeOption);
			}
		}

		@Override
		public boolean isSelectLeafsOnly() {
			return selectLeafOnly;
		}

		@Override
		public ITreeOption findNode(String id) {
			if (id == null) {
				return null;
			}
			ITreeOption ret = myTreeOptions.get(id);
			if (ret != null) {
				return ret;
			}
			for (TreeOption curTreeOption : myTreeOptions.values()) {
				ret = curTreeOption.findNode(id);
				if (ret != null) {
					return ret;
				}
			}
			return null;
		}

		public String getDefaultValueString() {
			List<String> defaultValues = new LinkedList<>();
			for (TreeOption curTreeOption : myTreeOptions.values()) {
				curTreeOption.getDefaultValueStrings(defaultValues);
			}
			return String.join(STRING_SEPERATOR, defaultValues);
		}

	}

	private static class TreeOption implements ITreeOption {
		private String treeNodeId;
		private String treeNodeName;
		protected String description;
		protected String icon;
		protected String command;
		protected Map<String, TreeOption> children = new HashMap<>();
		private int order = DEFAULT_ORDER;
		private boolean isDefault = false;

		TreeOption(IConfigurationElement element) {
			treeNodeId = element.getAttribute(ID);
			treeNodeName = element.getAttribute(NAME);
			description = element.getAttribute(DESCRIPTION);
			command = element.getAttribute(COMMAND);
			icon = element.getAttribute(ICON);
			String attribute = element.getAttribute(IS_DEFAULT);
			if (attribute != null) {
				isDefault = Boolean.parseBoolean(attribute);
			}

			if (treeNodeId == null)
				treeNodeId = EMPTY_STRING;
			if (treeNodeName == null)
				treeNodeName = EMPTY_STRING;
			if (description == null)
				description = EMPTY_STRING;
			if (command == null)
				command = EMPTY_STRING;
			if (icon == null)
				icon = EMPTY_STRING;

			String orderStr = element.getAttribute(ORDER);
			if (orderStr != null && orderStr.trim().length() > 0) {
				try {
					order = Integer.parseInt(orderStr);
				} catch (NumberFormatException e) {
					Activator.log(e);
					// Do nothing, default value is used.
				}
			}

			IConfigurationElement[] treeChildren = element.getChildren(TREE_VALUE);
			for (IConfigurationElement configElement : treeChildren) {
				TreeOption child = new TreeOption(configElement);
				children.put(child.getID(), child);
			}
		}

		public void getDefaultValueStrings(List<String> defaultValues) {
			if (isDefault) {
				defaultValues.add(treeNodeId);
			}
			for (TreeOption curChild : children.values()) {
				curChild.getDefaultValueStrings(defaultValues);
			}
		}

		public ITreeOption findNode(String id) {
			ITreeOption ret = children.get(id);
			if (ret != null) {
				return ret;
			}
			for (ITreeOption curChild : children.values()) {
				ret = ((TreeOption) curChild).findNode(id);
				if (ret != null) {
					return ret;
				}
			}
			return null;
		}

		@Override
		public boolean isContainer() {
			return children != null && !children.isEmpty(); // TODO do we need explicit marking as container for empty
															// ones
		}

		@Override
		public String getName() {
			return treeNodeName;
		}

		@Override
		public String getID() {
			return treeNodeId;
		}

		@Override
		public String getDescription() {
			return description;
		}

		@Override
		public String getCommand() {
			return command;
		}

		@Override
		public String getIcon() {
			return icon;
		}

		@Override
		public ITreeOption[] getChildren() {
			if (children == null) {
				return null;
			}
			return children.values().toArray(new ITreeOption[children.size()]);
		}

		@Override
		public int getOrder() {
			return order;
		}

		@Override
		public String toString() {
			return getName();
		}
	}

	public static class EnumOptionValue implements IEnumOptionValue {
		private String myName;
		private String myID;
		private String myDescription;
		private String myCommandLineContribution;
		private boolean myIsDefault = false;

		public EnumOptionValue(IConfigurationElement curEnumElement) {
			myID = curEnumElement.getAttribute(ID);
			myName = curEnumElement.getAttribute(NAME);
			myDescription = curEnumElement.getAttribute(DESCRIPTION);
			myCommandLineContribution = curEnumElement.getAttribute(COMMAND);
			if (myID == null) {
				myID = EMPTY_STRING;
			}
			if (myName == null) {
				myName = myID;
			}
			if (myDescription == null) {
				myDescription = EMPTY_STRING;
			}
			if (myCommandLineContribution == null) {
				myCommandLineContribution = EMPTY_STRING;
			}
			if (curEnumElement.getAttribute(IS_DEFAULT) != null) {
				myIsDefault = Boolean.parseBoolean(curEnumElement.getAttribute(IS_DEFAULT));
			}

		}

		@Override
		public String getID() {
			return myID;
		}

		@Override
		public String getName() {
			return myName;
		}

		@Override
		public String getDescription() {
			return myDescription;
		}

		@Override
		public String getCommandLIneDistribution() {
			return myCommandLineContribution;
		}

		@Override
		public boolean isDefault() {
			return myIsDefault;
		}

	}

	@Override
	public ITreeRoot getTreeRoot() throws BuildException {
		if (getValueType() != TREE) {
			throw new BuildException(Option_error_bad_value_type);
		}
		return myTreeRoot;
	}

	public StringBuffer dump(int leadingChars) {
		StringBuffer ret = new StringBuffer();
		String prepend = StringUtils.repeat(DUMPLEAD, leadingChars);
		ret.append(prepend + IOption.ELEMENT_NAME + NEWLINE);
		ret.append(prepend + NAME + EQUAL + myName + NEWLINE);
		ret.append(prepend + ID + EQUAL + myID + NEWLINE);
		ret.append(prepend + CATEGORY + EQUAL + modelCategoryId[SUPER] + NEWLINE);
		ret.append(prepend + RESOURCE_FILTER + EQUAL + modelResFilterStr[SUPER] + NEWLINE);
		ret.append(prepend + VALUE_TYPE + EQUAL + modelValueTypeStr[SUPER] + NEWLINE);
		ret.append(prepend + BROWSE_TYPE + EQUAL + modelBrowseTypeStr[SUPER] + NEWLINE);
		ret.append(prepend + BROWSE_FILTER_PATH + EQUAL + modelBrowseFilterPath[SUPER] + NEWLINE);
		ret.append(prepend + BROWSE_FILTER_EXTENSIONS + EQUAL + modelBrowseFilterExtensionsStr[SUPER] + NEWLINE);
		ret.append(prepend + DEFAULT_VALUE + EQUAL + modelDefaultValueString[SUPER] + NEWLINE);
		ret.append(prepend + DEFAULTVALUE_GENERATOR + EQUAL + modelDefaultValueGeneratorStr[SUPER] + NEWLINE);
		ret.append(prepend + COMMAND + EQUAL + modelCommand[SUPER] + NEWLINE);
		ret.append(prepend + COMMAND_GENERATOR + EQUAL + modelCommandGeneratorStr[SUPER] + NEWLINE);
		ret.append(prepend + COMMAND_FALSE + EQUAL + modelCommandFalse[SUPER] + NEWLINE);
		ret.append(prepend + USE_BY_SCANNER_DISCOVERY + EQUAL + modelIsForSD[SUPER] + NEWLINE);
		ret.append(prepend + TOOL_TIP + EQUAL + modelTip[SUPER] + NEWLINE);
		ret.append(prepend + CONTEXT_ID + EQUAL + modelContextId[SUPER] + NEWLINE);
		ret.append(prepend + VALUE_HANDLER + EQUAL + modelValueHandlerString[SUPER] + NEWLINE);
		ret.append(prepend + VALUE_HANDLER_EXTRA_ARGUMENT + EQUAL + modelValueHandlerExtraArgument[SUPER] + NEWLINE);
		ret.append(prepend + APPLICABILITY_CALCULATOR + EQUAL + modelApplicabilityCalculatorStr[SUPER] + NEWLINE);
		ret.append(prepend + FIELD_EDITOR_ID + EQUAL + modelFieldEditorId[SUPER] + NEWLINE);
		ret.append(prepend + FIELD_EDITOR_EXTRA_ARGUMENT + EQUAL + modelFieldEditorExtraArgument[SUPER] + NEWLINE);
		return ret;
	}

	@Override
	public String getDefaultValue() {
		return myDefaultStringValue;
	}

	@Override
	public String[] getCommandLineContribution(IResource resource, String optionValue,
			AutoBuildConfigurationData autoConfData) {
		ICConfigurationDescription confDesc = autoConfData.getCdtConfigurationDescription();
		String[] retString = new String[1];
		String[] retNothing = new String[0];
		if (!isEnabled(resource, autoConfData)) {
			return new String[0];
		}
		if (commandGenerator != null) {
			String command[] = commandGenerator.generateCommand(this, optionValue, autoConfData);
			if (command != null) {
				return command;
			}
			// if commandGenerator returns null do the default command
		}
		switch (valueType) {
		case IOption.BOOLEAN:
			if (Boolean.parseBoolean(optionValue)) {
				retString[0] = modelCommand[SUPER];
			} else {
				retString[0] = modelCommandFalse[SUPER];
			}
			return retString;
		case IOption.ENUMERATED: {
			EnumOptionValue selectedEnumValue = myEnumOptionValues.get(optionValue);
			if (selectedEnumValue == null) {
				// This should not happen
				return retNothing;
			}
			retString[0] = selectedEnumValue.getCommandLIneDistribution();
			return retString;
		}
		case IOption.TREE: {
			String[] values = optionValue.split(STRING_SEPARATOR_REGEX);
			List<String> ret = new LinkedList<>();
			if (myTreeRoot != null) {
				for (String curptionValue : values) {
					ITreeOption treeNode = myTreeRoot.findNode(curptionValue);
					if (treeNode != null) {
						String command = treeNode.getCommand();
						if (!command.isBlank()) {
							ret.add(command);
						}
					}
				}
			}
			return ret.toArray(new String[ret.size()]);
		}
		case IOption.STRING:
			retString[0] = evaluateCommand(modelCommand[SUPER], AutoBuildCommon.resolve(optionValue, confDesc));
			return retString;
		case IOption.STRING_LIST:
		case IOption.INCLUDE_FILES:
		case IOption.INCLUDE_PATH:
		case IOption.LIBRARY_PATHS:
		case IOption.LIBRARY_FILES:
		case IOption.MACRO_FILES:
		case IOption.UNDEF_INCLUDE_FILES:
		case IOption.UNDEF_INCLUDE_PATH:
		case IOption.UNDEF_LIBRARY_PATHS:
		case IOption.UNDEF_LIBRARY_FILES:
		case IOption.UNDEF_MACRO_FILES:
		case IOption.PREPROCESSOR_SYMBOLS:
		case IOption.UNDEF_PREPROCESSOR_SYMBOLS: {
			String listCmd = modelCommand[SUPER];
			String[] values = optionValue.split(STRING_SEPARATOR_REGEX);
			String[] resolvedList = AutoBuildCommon.resolveStringListValues(values, autoConfData, true);
			List<String> ret = new LinkedList<>();
			for (String curResolved : resolvedList) {
				if (!curResolved.isBlank() && !curResolved.contains(EMPTY_QUOTED_STRING))
					ret.add(evaluateCommand(listCmd, curResolved));
			}
			return ret.toArray(new String[ret.size()]);
		}
		default:
			break;
		}
		return retNothing;
	}

	/**
	 * replaces (case insensitive) all ${VALUE} instances in de content of the
	 * parameter command with the content of the parameter values
	 * 
	 * if no ${VALUE} instances are found the method returns (command +
	 * values).trim()
	 * 
	 * @param command
	 * @param values
	 * @return
	 */
	private static String evaluateCommand(String command, String values) {
		final int DOLLAR_VALUE_LENGTH = 8;

		if (command == null)
			return values.trim();

		String ret = command;
		boolean found = false;
		int start = 0;
		int index;
		int len;
		while ((index = ret.indexOf("${", start)) >= 0 && //$NON-NLS-1$
				(len = ret.length()) >= index + DOLLAR_VALUE_LENGTH) {
			start = index;
			index = index + 2;
			int ch = ret.charAt(index);
			if (ch == 'v' || ch == 'V') {
				index++;
				ch = ret.charAt(index);
				if (ch == 'a' || ch == 'A') {
					index++;
					ch = ret.charAt(index);
					if (ch == 'l' || ch == 'L') {
						index++;
						ch = ret.charAt(index);
						if (ch == 'u' || ch == 'U') {
							index++;
							ch = ret.charAt(index);
							if (ch == 'e' || ch == 'E') {
								index++;
								ch = ret.charAt(index);
								if (ch == '}') {
									String temp = ""; //$NON-NLS-1$
									index++;
									found = true;
									if (start > 0) {
										temp = ret.substring(0, start);
									}
									temp = temp.concat(values.trim());
									if (len > index) {
										start = temp.length();
										ret = temp.concat(ret.substring(index));
										index = start;
									} else {
										ret = temp;
										break;
									}
								}
							}
						}
					}
				}
			}
			start = index;
		}
		if (found)
			return ret.trim();
		return (command + values).trim();
	}

	@Override
	public String getCommand() {
		return modelCommand[SUPER];
	}

}
//@Override
//public IOptions getOptionHolder() {
//  // Do not take superclasses into account
//  return holder;
//}
//public void setWasOptRef(boolean was) {
//wasOptRef = was;
//}
///**
//* @return Returns true if this Option was created from an MBS 2.0 model
//*         OptionReference element.
//*/
//public boolean wasOptRef() {
// return wasOptRef;
//}
//public void setValueHandlerElement(IConfigurationElement element) {
//valueHandlerElement = element;
//}
//public void setAdjusted(boolean adjusted) {
//isUdjusted = adjusted;
//}

//
//@Override
//public boolean isValid() {
//  return true;
//}
//protected boolean isAbstract() {
//return isAbstract;
//}
//public IConfigurationElement getValueHandlerElement() {
//return valueHandlerElement;
//}
///**
//* Create an <code>Option</code> based on the specification stored in the
//* project file (.cdtbuild).
//*
//* @param parent
//*            The <code>IHoldsOptions</code> the option will be added to.
//* @param element
//*            The XML element that contains the option settings.
//*/
//public Option(IOptions parent, ICStorageElement element) {
// //        this.holder = parent;
// //        isExtensionOption = false;
// //
// //        // Initialize from the XML attributes
// //        loadFromProject(element);
//}
/**
 * //* Create an <code>Option</code> based upon an existing option. //*
 * //* @param parent //* The <code>IHoldsOptions</code> the option will be added
 * to. //* @param Id //* New ID for the option. //* @param myName //* New name
 * for the option. //* @param option //* The existing option to clone, except
 * for the above fields. //
 */
//public Option(IOptions parent, String newId, String newName, Option option) {
//  //        this.holder = parent;
//  //        superClass = option.superClass;
//  //        if (superClass != null) {
//  //            superClassId = option.superClass.getId();
//  //        } else if (option.superClassId != null) {
//  //            superClassId = option.superClassId;
//  //        }
//  //        id = newId;
//  //        name = newName;
//  //        isExtensionOption = false;
//  //        boolean copyIds = newId.equals(option.id);
//  //
//  //        //  Copy the remaining attributes
//  //        if (option.unusedChildren != null) {
//  //            unusedChildren = option.unusedChildren;
//  //        }
//  //        if (option.isAbstract != null) {
//  //            isAbstract = option.isAbstract;
//  //        }
//  //        if (option.command != null) {
//  //            command = option.command;
//  //        }
//  //        if (option.commandFalse != null) {
//  //            commandFalse = option.commandFalse;
//  //        }
//  //        if (option.isForScannerDiscovery != null) {
//  //            isForScannerDiscovery = option.isForScannerDiscovery;
//  //        }
//  //        if (option.tip != null) {
//  //            tip = option.tip;
//  //        }
//  //        if (option.contextId != null) {
//  //            contextId = option.contextId;
//  //        }
//  //        if (option.categoryId != null) {
//  //            categoryId = option.categoryId;
//  //        }
//  //        if (option.builtIns != null) {
//  //            builtIns = new ArrayList<>(option.builtIns);
//  //        }
//  //        if (option.browseType != null) {
//  //            browseType = option.browseType;
//  //        }
//  //        if (option.browseFilterPath != null) {
//  //            browseFilterPath = option.browseFilterPath;
//  //        }
//  //        if (option.browseFilterExtensions != null) {
//  //            browseFilterExtensions = option.browseFilterExtensions.clone();
//  //        }
//  //        if (option.resourceFilter != null) {
//  //            resourceFilter = option.resourceFilter;
//  //        }
//  //        if (option.applicableValuesList != null) {
//  //            applicableValuesList = new ArrayList<>(option.applicableValuesList);
//  //            commandsMap = new HashMap<>(option.commandsMap);
//  //            namesMap = new HashMap<>(option.namesMap);
//  //        }
//  //        if (option.treeRoot != null) {
//  //            treeRoot = new TreeRoot((TreeRoot) option.treeRoot);
//  //        }
//  //
//  //        if (option.valueType != null) {
//  //            valueType = option.valueType;
//  //        }
//  //        try {
//  //            int vType = option.getValueType();
//  //            switch (vType) {
//  //            case BOOLEAN:
//  //                if (option.value != null) {
//  //                    value = option.value;
//  //                }
//  //                if (option.defaultValue != null) {
//  //                    defaultValue = option.defaultValue;
//  //                }
//  //                break;
//  //            case STRING:
//  //            case ENUMERATED:
//  //            case TREE:
//  //                if (option.value != null) {
//  //                    value = option.value;
//  //                }
//  //                if (option.defaultValue != null) {
//  //                    defaultValue = option.defaultValue;
//  //                }
//  //                break;
//  //            case STRING_LIST:
//  //            case INCLUDE_PATH:
//  //            case PREPROCESSOR_SYMBOLS:
//  //            case LIBRARIES:
//  //            case OBJECTS:
//  //            case INCLUDE_FILES:
//  //            case LIBRARY_PATHS:
//  //            case LIBRARY_FILES:
//  //            case MACRO_FILES:
//  //            case UNDEF_INCLUDE_PATH:
//  //            case UNDEF_PREPROCESSOR_SYMBOLS:
//  //            case UNDEF_INCLUDE_FILES:
//  //            case UNDEF_LIBRARY_PATHS:
//  //            case UNDEF_LIBRARY_FILES:
//  //            case UNDEF_MACRO_FILES:
//  //                if (option.value != null) {
//  //                    @SuppressWarnings("unchecked")
//  //                    ArrayList<OptionStringValue> list = new ArrayList<>((ArrayList<OptionStringValue>) option.value);
//  //                    value = list;
//  //                }
//  //                if (option.defaultValue != null) {
//  //                    @SuppressWarnings("unchecked")
//  //                    ArrayList<OptionStringValue> list = new ArrayList<>(
//  //                            (ArrayList<OptionStringValue>) option.defaultValue);
//  //                    defaultValue = list;
//  //                }
//  //                break;
//  //            }
//  //        } catch (BuildException be) {
//  //            // TO DO: should we ignore this??
//  //        }
//  //
//  //        category = option.category;
//  //
//  //        defaultValueGeneratorElement = option.defaultValueGeneratorElement;
//  //        defaultValueGenerator = option.defaultValueGenerator;
//  //
//  //        commandGeneratorElement = option.commandGeneratorElement;
//  //        commandGenerator = option.commandGenerator;
//  //
//  //        applicabilityCalculatorElement = option.applicabilityCalculatorElement;
//  //        applicabilityCalculator = option.applicabilityCalculator;
//  //
//  //        booleanExpressionCalculator = option.booleanExpressionCalculator;
//  //
//  //        if (option.valueHandlerElement != null) {
//  //            valueHandlerElement = option.valueHandlerElement;
//  //            valueHandler = option.valueHandler;
//  //        }
//  //        if (option.valueHandlerExtraArgument != null) {
//  //            valueHandlerExtraArgument = option.valueHandlerExtraArgument;
//  //        }
//  //
//  //        if (option.fieldEditorId != null) {
//  //            fieldEditorId = option.fieldEditorId;
//  //        }
//  //        if (option.fieldEditorExtraArgument != null) {
//  //            fieldEditorExtraArgument = option.fieldEditorExtraArgument;
//  //        }
//  //
//  //        if (copyIds) {
//  //            isDirty = option.isDirty;
//  //            rebuildState = option.rebuildState;
//  //        } else {
//  //            setDirty(true);
//  //            setRebuildState(true);
//  //        }
//}
//public boolean isAdjustedExtension() {
//return isUdjusted;
//}
//private boolean isUdjusted = false;

///**
//* This constructor is called to create an Option whose attributes and children
//* will be
//* added by separate calls.
//*
//* @param parent
//*            - the parent of the option, if any
//* @param superClass
//*            - the superClass, if any
//* @param Id
//*            - the id for the new option
//* @param myName
//*            - the name for the new option
//* @param isExtensionElement
//*            - indicates whether this is an extension element or a managed
//*            project element
//*/
//public Option(IOptions parent, IOption superClass, String Id, String newName, boolean isExtensionElement) {
//  //        this.holder = parent;
//  //        this.superClass = superClass;
//  //        if (this.superClass != null) {
//  //            superClassId = this.superClass.getId();
//  //        }
//  //        id = (Id);
//  //        name = (newName);
//  //        isExtensionOption = isExtensionElement;
//  //        if (isExtensionElement) {
//  //            // Hook me up to the Managed Build Manager
//  //            ManagedBuildManager.addExtensionOption(this);
//  //        } else {
//  //            setDirty(true);
//  //            setRebuildState(true);
//  //        }
//}

// 

/*
 * E L E M E N T A T T R I B U T E R E A D E R S A N D W R I T E R S
 */
//
//    /**
//     * Initialize the option information from the XML element
//     * specified in the argument
//     *
//     * @param element
//     *            An XML element containing the option information
//     */
//    protected void loadFromProject(ICStorageElement element) {
//
//        // id (unique, don't intern)
//        id = (element.getAttribute(IBuildObject.ID));
//
//        // name
//        if (element.getAttribute(IBuildObject.NAME) != null) {
//            name = (SafeStringInterner.safeIntern(element.getAttribute(IBuildObject.NAME)));
//        }
//
//        // superClass
//        superClassId = SafeStringInterner.safeIntern(element.getAttribute(IProjectType.SUPERCLASS));
//        if (superClassId != null && superClassId.length() > 0) {
//            superClass = ManagedBuildManager.getExtensionOption(superClassId);
//            if (superClass == null) {
//                /*
//                 * This can happen when options are set at the resource level, for a project using a toolchain definition
//                 * where there are options at the toolchain level & one or more of those options is set at a
//                 * non-default value.
//                 *
//                 * In these cases the superclass is set to the option from the parent not the extension's ID
//                 * Workaround this by searching for any missing superclass IDs at on the parent configs toolchain
//                 *
//                 * See the "bug580009.tests.cfg1.tc" definition in org.eclipse.cdt.managedbuilder.core.tests for an example
//                 */
//                IBuildObject parent = this.getParent();
//                if (parent instanceof IToolChain) {
//                    IConfiguration config = ((IToolChain) parent).getParent();
//                    IOption foundOption = null;
//                    //In rare cases the RootFolderInfo may not have loaded & will cause an NPE
//                    if (config != null && config.getRootFolderInfo() != null) {
//                        IToolChain parentToolchain = config.getToolChain();
//                        if (parentToolchain != null) {
//                            foundOption = parentToolchain.getOptionById(superClassId);
//                        }
//                    }
//                    if (foundOption != null) {
//                        superClass = foundOption;
//                    } else {
//                        Activator.log(new Status(IStatus.ERROR, Activator.getId(),
//                                MessageFormat.format("Missing superclass \"{0}\" for \"{1}\"", superClassId, getId()))); //$NON-NLS-1$
//                    }
//                } else {
//                    Activator.log(new Status(IStatus.ERROR, Activator.getId(),
//                            MessageFormat.format("Missing superclass \"{0}\" for \"{1}\"", superClassId, getId()))); //$NON-NLS-1$
//                }
//            }
//        }
//
//        // isAbstract
//        if (element.getAttribute(IS_ABSTRACT) != null) {
//            String isAbs = element.getAttribute(IS_ABSTRACT);
//            if (isAbs != null) {
//                isAbstract = Boolean.parseBoolean(isAbs);
//            }
//        }
//
//        // Get the command defined for the option
//        if (element.getAttribute(COMMAND) != null) {
//            command = SafeStringInterner.safeIntern(element.getAttribute(COMMAND));
//        }
//
//        // Get the command defined for a Boolean option when the value is False
//        if (element.getAttribute(COMMAND_FALSE) != null) {
//            commandFalse = SafeStringInterner.safeIntern(element.getAttribute(COMMAND_FALSE));
//        }
//
//        // isForScannerDiscovery
//        if (element.getAttribute(USE_BY_SCANNER_DISCOVERY) != null) {
//            String isForSD = element.getAttribute(USE_BY_SCANNER_DISCOVERY);
//            if (isForSD != null) {
//                isForScannerDiscovery = Boolean.parseBoolean(isForSD);
//            }
//        }
//
//        // Get the tooltip for the option
//        if (element.getAttribute(TOOL_TIP) != null) {
//            tip = SafeStringInterner.safeIntern(element.getAttribute(TOOL_TIP));
//        }
//
//        // Get the contextID for the option
//        if (element.getAttribute(CONTEXT_ID) != null) {
//            contextId = SafeStringInterner.safeIntern(element.getAttribute(CONTEXT_ID));
//        }
//
//        // Options hold different types of values
//        if (element.getAttribute(VALUE_TYPE) != null) {
//            String valueTypeStr = element.getAttribute(VALUE_TYPE);
//            valueType = Integer.valueOf(ValueTypeStrToInt(valueTypeStr));
//        }
//
//        // Now get the actual value based upon value-type
//        try {
//            int valType = getValueType();
//            switch (valType) {
//            case BOOLEAN:
//                // Convert the string to a boolean
//                if (element.getAttribute(VALUE) != null) {
//                    value = Boolean.valueOf(element.getAttribute(VALUE));
//                }
//                if (element.getAttribute(DEFAULT_VALUE) != null) {
//                    defaultValue = Boolean.valueOf(element.getAttribute(DEFAULT_VALUE));
//                }
//                break;
//            case STRING:
//                // Just get the value out of the option directly
//                if (element.getAttribute(VALUE) != null) {
//                    value = SafeStringInterner.safeIntern(element.getAttribute(VALUE));
//                }
//                if (element.getAttribute(DEFAULT_VALUE) != null) {
//                    defaultValue = SafeStringInterner.safeIntern(element.getAttribute(DEFAULT_VALUE));
//                }
//                break;
//            case ENUMERATED:
//                if (element.getAttribute(VALUE) != null) {
//                    value = SafeStringInterner.safeIntern(element.getAttribute(VALUE));
//                }
//                if (element.getAttribute(DEFAULT_VALUE) != null) {
//                    defaultValue = SafeStringInterner.safeIntern(element.getAttribute(DEFAULT_VALUE));
//                }
//
//                //  Do we have enumeratedOptionValue children?  If so, load them
//                //  to define the valid values and the default value.
//                ICStorageElement configElements[] = element.getChildren();
//                for (int i = 0; i < configElements.length; ++i) {
//                    ICStorageElement configNode = configElements[i];
//                    if (configNode.getName().equals(ENUM_VALUE)) {
//                        ICStorageElement configElement = configNode;
//                        String optId = SafeStringInterner.safeIntern(configElement.getAttribute(ID));
//                        if (i == 0) {
//                            applicableValuesList = new ArrayList<>();
//                            if (defaultValue == null) {
//                                defaultValue = optId; //  Default value to be overridden is default is specified
//                            }
//                        }
//                        applicableValuesList.add(optId);
//                        if (configElement.getAttribute(COMMAND) != null) {
//                            getCommandMap().put(optId,
//                                    SafeStringInterner.safeIntern(configElement.getAttribute(COMMAND)));
//                        } else {
//                            getCommandMap().put(optId, EMPTY_STRING);
//                        }
//                        getNameMap().put(optId, SafeStringInterner.safeIntern(configElement.getAttribute(NAME)));
//                        if (configElement.getAttribute(IS_DEFAULT) != null) {
//                            Boolean isDefault = Boolean.valueOf(configElement.getAttribute(IS_DEFAULT));
//                            if (isDefault.booleanValue()) {
//                                defaultValue = optId;
//                            }
//                        }
//                    }
//                }
//                break;
//            case TREE:
//                if (element.getAttribute(VALUE) != null) {
//                    value = element.getAttribute(VALUE);
//                }
//                if (element.getAttribute(DEFAULT_VALUE) != null) {
//                    defaultValue = element.getAttribute(DEFAULT_VALUE);
//                }
//                break;
//            case STRING_LIST:
//            case INCLUDE_PATH:
//            case PREPROCESSOR_SYMBOLS:
//            case LIBRARIES:
//            case OBJECTS:
//            case INCLUDE_FILES:
//            case LIBRARY_PATHS:
//            case LIBRARY_FILES:
//            case MACRO_FILES:
//            case UNDEF_INCLUDE_PATH:
//            case UNDEF_PREPROCESSOR_SYMBOLS:
//            case UNDEF_INCLUDE_FILES:
//            case UNDEF_LIBRARY_PATHS:
//            case UNDEF_LIBRARY_FILES:
//            case UNDEF_MACRO_FILES:
//                //  Note:  These string-list options do not load either the "value" or
//                //         "defaultValue" attributes.  Instead, the ListOptionValue children
//                //         are loaded in the value field.
//                List<OptionStringValue> vList = new ArrayList<>();
//                List<OptionStringValue> biList = new ArrayList<>();
//                configElements = element.getChildren();
//                for (ICStorageElement veNode : configElements) {
//                    if (veNode.getName().equals(LIST_VALUE)) {
//                        OptionStringValue ve = new OptionStringValue(veNode);
//                        if (ve.isBuiltIn()) {
//                            biList.add(ve);
//                        } else {
//                            vList.add(ve);
//                        }
//                    }
//                }
//
//                //Assume not empty unless specificaly flagged
//                boolean isValueEmpty = false;
//                boolean isBuiltinEmpty = false;
//
//                if (element.getAttribute(IS_VALUE_EMPTY) != null) {
//                    Boolean isEmpty = Boolean.valueOf(element.getAttribute(IS_VALUE_EMPTY));
//                    if (isEmpty.booleanValue()) {
//                        isValueEmpty = true;
//                    }
//                }
//                if (element.getAttribute(IS_BUILTIN_EMPTY) != null) {
//                    Boolean isEmpty = Boolean.valueOf(element.getAttribute(IS_BUILTIN_EMPTY));
//                    if (isEmpty.booleanValue()) {
//                        isBuiltinEmpty = true;
//                    }
//                }
//
//                if (vList.size() != 0 || isValueEmpty) {
//                    value = vList;
//                } else {
//                    value = null;
//                }
//                if (biList.size() != 0 || isBuiltinEmpty) {
//                    builtIns = biList;
//                } else {
//                    builtIns = null;
//                }
//
//                break;
//            default:
//                break;
//            }
//        } catch (BuildException e) {
//            // TO DO: report error
//        }
//
//        // Determine if there needs to be a browse button
//        if (element.getAttribute(BROWSE_TYPE) != null) {
//            String browseTypeStr = element.getAttribute(BROWSE_TYPE);
//
//            if (browseTypeStr == null) {
//                // Set to null, to indicate no browse type specification
//                // This will allow any superclasses to be searched for the
//                // browse type specification, and thus inherited, if found,
//                // which they should be
//                browseType = null;
//            } else if (browseTypeStr.equals(NONE)) {
//                browseType = BROWSE_NONE;
//            } else if (browseTypeStr.equals(FILE)) {
//                browseType = BROWSE_FILE;
//            } else if (browseTypeStr.equals(DIR)) {
//                browseType = BROWSE_DIR;
//            }
//        }
//
//        // Get the browseFilterPath attribute
//        if (element.getAttribute(BROWSE_FILTER_PATH) != null) {
//            this.browseFilterPath = SafeStringInterner.safeIntern(element.getAttribute(BROWSE_FILTER_PATH));
//        }
//
//        // Get the browseFilterExtensions attribute
//        if (element.getAttribute(BROWSE_FILTER_EXTENSIONS) != null) {
//            String browseFilterExtensionsStr = element.getAttribute(BROWSE_FILTER_EXTENSIONS);
//            if (browseFilterExtensionsStr != null) {
//                this.browseFilterExtensions = SafeStringInterner
//                        .safeIntern(browseFilterExtensionsStr.split("\\s*,\\s*")); //$NON-NLS-1$
//            }
//        }
//
//        if (element.getAttribute(CATEGORY) != null) {
//            categoryId = SafeStringInterner.safeIntern(element.getAttribute(CATEGORY));
//            if (categoryId != null) {
//                category = holder.getOptionCategory(categoryId);
//            }
//        }
//
//        // Get the resourceFilter attribute
//        if (element.getAttribute(RESOURCE_FILTER) != null) {
//            String resFilterStr = element.getAttribute(RESOURCE_FILTER);
//            if (resFilterStr == null) {
//                // Set to null, to indicate no resource filter specification
//                // This will allow any superclasses to be searched for the
//                // resource filter specification, and thus inherited, if found,
//                // which they should be
//                resourceFilter = null;
//            } else if (resFilterStr.equals(ALL)) {
//                resourceFilter = FILTER_ALL;
//            } else if (resFilterStr.equals(FILE)) {
//                resourceFilter = FILTER_FILE;
//            } else if (resFilterStr.equals(PROJECT)) {
//                resourceFilter = FILTER_PROJECT;
//            }
//        }
//
//        // Note: valueHandlerElement and VALUE_HANDLER are not restored,
//        // as they are not saved. See note in serialize().
//
//        // valueHandlerExtraArgument
//        if (element.getAttribute(VALUE_HANDLER_EXTRA_ARGUMENT) != null) {
//            valueHandlerExtraArgument = SafeStringInterner
//                    .safeIntern(element.getAttribute(VALUE_HANDLER_EXTRA_ARGUMENT));
//        }
//    }

//    /**
//     * Persist the option to the {@link ICStorageElement}.
//     *
//     * @param element
//     *            - storage element to persist the option
//     */
//    public void serialize(ICStorageElement element) throws BuildException {
//        if (superClass != null) {
//            element.setAttribute(IProjectType.SUPERCLASS, superClass.getId());
//        } else if (superClassId != null) {
//            element.setAttribute(IProjectType.SUPERCLASS, superClassId);
//        }
//
//        element.setAttribute(IBuildObject.ID, id);
//
//        if (name != null) {
//            element.setAttribute(IBuildObject.NAME, name);
//        }
//
//        if (isAbstract != null) {
//            element.setAttribute(IS_ABSTRACT, isAbstract.toString());
//        }
//
//        if (command != null) {
//            element.setAttribute(COMMAND, command);
//        }
//
//        if (commandFalse != null) {
//            element.setAttribute(COMMAND_FALSE, commandFalse);
//        }
//
//        if (isForScannerDiscovery != null) {
//            element.setAttribute(USE_BY_SCANNER_DISCOVERY, isForScannerDiscovery.toString());
//        }
//
//        if (tip != null) {
//            element.setAttribute(TOOL_TIP, tip);
//        }
//
//        if (contextId != null) {
//            element.setAttribute(CONTEXT_ID, contextId);
//        }
//        /*
//         * Note:  We store value & value-type as a pair, so we know what type of value we are
//         *        dealing with when we read it back in.
//         *        This is also true of defaultValue.
//         */
//        boolean storeValueType = false;
//
//        // value
//        if (value != null) {
//            storeValueType = true;
//            switch (getValueType()) {
//            case BOOLEAN:
//                element.setAttribute(VALUE, ((Boolean) value).toString());
//                break;
//            case STRING:
//            case ENUMERATED:
//            case TREE:
//                element.setAttribute(VALUE, (String) value);
//                break;
//            case STRING_LIST:
//            case INCLUDE_PATH:
//            case PREPROCESSOR_SYMBOLS:
//            case LIBRARIES:
//            case OBJECTS:
//            case INCLUDE_FILES:
//            case LIBRARY_PATHS:
//            case LIBRARY_FILES:
//            case MACRO_FILES:
//            case UNDEF_INCLUDE_PATH:
//            case UNDEF_PREPROCESSOR_SYMBOLS:
//            case UNDEF_INCLUDE_FILES:
//            case UNDEF_LIBRARY_PATHS:
//            case UNDEF_LIBRARY_FILES:
//            case UNDEF_MACRO_FILES:
//                if (value != null) {
//                    @SuppressWarnings("unchecked")
//                    ArrayList<OptionStringValue> stringList = (ArrayList<OptionStringValue>) value;
//                    for (OptionStringValue optValue : stringList) {
//                        ICStorageElement valueElement = element.createChild(LIST_VALUE);
//                        optValue.serialize(valueElement);
//                    }
//
//                    if (stringList.isEmpty()) {
//                        element.setAttribute(IS_VALUE_EMPTY, Boolean.TRUE.toString());
//                    } else {
//                        element.setAttribute(IS_VALUE_EMPTY, Boolean.FALSE.toString());
//                    }
//                } else {
//                    element.setAttribute(IS_VALUE_EMPTY, Boolean.FALSE.toString());
//                }
//
//                // Serialize the built-ins that have been overridden
//                if (builtIns != null) {
//                    for (OptionStringValue optionValue : builtIns) {
//                        ICStorageElement valueElement = element.createChild(LIST_VALUE);
//                        optionValue.serialize(valueElement);
//                    }
//
//                    if (builtIns.isEmpty()) {
//                        element.setAttribute(IS_BUILTIN_EMPTY, Boolean.TRUE.toString());
//                    } else {
//                        element.setAttribute(IS_BUILTIN_EMPTY, Boolean.FALSE.toString());
//                    }
//                } else {
//                    element.setAttribute(IS_BUILTIN_EMPTY, Boolean.FALSE.toString());
//                }
//
//                break;
//            }
//        }
//
//        // defaultValue
//        if (defaultValue != null) {
//            storeValueType = true;
//            switch (getValueType()) {
//            case BOOLEAN:
//                element.setAttribute(DEFAULT_VALUE, ((Boolean) defaultValue).toString());
//                break;
//            case STRING:
//            case ENUMERATED:
//            case TREE:
//                element.setAttribute(DEFAULT_VALUE, (String) defaultValue);
//                break;
//            default:
//                break;
//            }
//        }
//
//        if (storeValueType) {
//            String str;
//            switch (getValueType()) {
//            case BOOLEAN:
//                str = TYPE_BOOL;
//                break;
//            case STRING:
//                str = TYPE_STRING;
//                break;
//            case ENUMERATED:
//                str = TYPE_ENUM;
//                break;
//            case STRING_LIST:
//                str = TYPE_STR_LIST;
//                break;
//            case INCLUDE_PATH:
//                str = TYPE_INC_PATH;
//                break;
//            case LIBRARIES:
//                str = TYPE_LIB;
//                break;
//            case OBJECTS:
//                str = TYPE_USER_OBJS;
//                break;
//            case PREPROCESSOR_SYMBOLS:
//                str = TYPE_DEFINED_SYMBOLS;
//                break;
//            case INCLUDE_FILES:
//                str = TYPE_INC_FILES;
//                break;
//            case LIBRARY_PATHS:
//                str = TYPE_LIB_PATHS;
//                break;
//            case LIBRARY_FILES:
//                str = TYPE_LIB_FILES;
//                break;
//            case MACRO_FILES:
//                str = TYPE_SYMBOL_FILES;
//                break;
//            case UNDEF_INCLUDE_PATH:
//                str = TYPE_UNDEF_INC_PATH;
//                break;
//            case UNDEF_PREPROCESSOR_SYMBOLS:
//                str = TYPE_UNDEF_DEFINED_SYMBOLS;
//                break;
//            case UNDEF_INCLUDE_FILES:
//                str = TYPE_UNDEF_INC_FILES;
//                break;
//            case UNDEF_LIBRARY_PATHS:
//                str = TYPE_UNDEF_LIB_PATHS;
//                break;
//            case UNDEF_LIBRARY_FILES:
//                str = TYPE_UNDEF_LIB_FILES;
//                break;
//            case UNDEF_MACRO_FILES:
//                str = TYPE_UNDEF_SYMBOL_FILES;
//                break;
//            case TREE:
//                str = TYPE_TREE;
//                break;
//            default:
//                //  TO DO; is this a problem...
//                str = EMPTY_STRING;
//                break;
//            }
//            element.setAttribute(VALUE_TYPE, str);
//        }
//
//        // browse type
//        if (browseType != null) {
//            String str;
//            switch (getBrowseType()) {
//            case BROWSE_NONE:
//                str = NONE;
//                break;
//            case BROWSE_FILE:
//                str = FILE;
//                break;
//            case BROWSE_DIR:
//                str = DIR;
//                break;
//            default:
//                str = EMPTY_STRING;
//                break;
//            }
//            element.setAttribute(BROWSE_TYPE, str);
//        }
//
//        // browse filter path
//        if (browseFilterPath != null) {
//            element.setAttribute(BROWSE_FILTER_PATH, browseFilterPath);
//        }
//
//        // browse filter extensions
//        if (browseFilterExtensions != null) {
//            StringBuilder sb = new StringBuilder();
//            for (String ext : browseFilterExtensions) {
//                sb.append(ext).append(',');
//            }
//            element.setAttribute(BROWSE_FILTER_EXTENSIONS, sb.toString());
//        }
//
//        if (categoryId != null) {
//            element.setAttribute(CATEGORY, categoryId);
//        }
//
//        // resource filter
//        if (resourceFilter != null) {
//            String str;
//            switch (getResourceFilter()) {
//            case FILTER_ALL:
//                str = ALL;
//                break;
//            case FILTER_FILE:
//                str = FILE;
//                break;
//            case FILTER_PROJECT:
//                str = PROJECT;
//                break;
//            default:
//                str = EMPTY_STRING;
//                break;
//            }
//            element.setAttribute(RESOURCE_FILTER, str);
//        }
//
//        // Note: applicability calculator cannot be specified in a project file because
//        //       an IConfigurationElement is needed to load it!
//        if (applicabilityCalculatorElement != null) {
//            //  TO DO:  issue warning?
//        }
//
//        // Note: a value handler cannot be specified in a project file because
//        //       an IConfigurationElement is needed to load it!
//        if (valueHandlerElement != null) {
//            //  TO DO:  Issue warning? Stuck with behavior of this elsewhere in
//            //         CDT, e.g. the implementation of Tool
//        }
//        if (valueHandlerExtraArgument != null) {
//            element.setAttribute(VALUE_HANDLER_EXTRA_ARGUMENT, valueHandlerExtraArgument);
//        }
//
//        // I am clean now
//        isDirty = false;
//    }

//    // @Override
//    public IOptionContextData getOptionContextData(IHoldsOptions holder) {
//        return new OptionContextData(this, holder);
//    }

//
//    @Override
//    public String getName() {
//        return myName;
//    }
//
//public IConfigurationElement getApplicabilityCalculatorElement() {
//  return applicabilityCalculatorElement;
//}
//private Object getExactValue() {
//  /*
//   *  In order to determine the current value of an option, perform the following steps until a value is found:
//   *   1.   Examine the value attribute of the option.
//   *   2.   Examine the value attribute of the option's superClass recursively.
//   *   3.   Examine the dynamicDefaultValue attribute of the option and invoke it if specified. (not yet implemented)
//   *   4.   Examine the defaultValue attribute of the option.
//   *   5.   Examine the dynamicDefaultValue attribute of the option's superClass and invoke it if specified. (not yet implemented)
//   *   6.   Examine the defaultValue attribute of the option's superClass.
//   *   7.   Go to step 5 recursively until no more super classes.
//   *   8.   Use the default value for the option type.
//   */
//
//  Object val = myRawValue;
//  if (val == null) {
//      val = myDefaultValue;
//      if (val == null) {
//          int valType;
//          try {
//              valType = getValueType();
//          } catch (BuildException e) {
//              Activator.log(e);
//              return EMPTY_STRING;
//          }
//          switch (valType) {
//          case BOOLEAN:
//              val = Boolean.FALSE;
//              break;
//          case STRING:
//          case TREE:
//              val = EMPTY_STRING;
//              break;
//          case ENUMERATED:
//              // TO DO: Can we default to the first enumerated id?
//              val = EMPTY_STRING;
//              break;
//          case STRING_LIST:
//          case INCLUDE_PATH:
//          case PREPROCESSOR_SYMBOLS:
//          case LIBRARIES:
//          case OBJECTS:
//          case INCLUDE_FILES:
//          case LIBRARY_PATHS:
//          case LIBRARY_FILES:
//          case MACRO_FILES:
//          case UNDEF_INCLUDE_PATH:
//          case UNDEF_PREPROCESSOR_SYMBOLS:
//          case UNDEF_INCLUDE_FILES:
//          case UNDEF_LIBRARY_PATHS:
//          case UNDEF_LIBRARY_FILES:
//          case UNDEF_MACRO_FILES:
//              val = new ArrayList<OptionStringValue>();
//              break;
//          default:
//              val = EMPTY_STRING;
//              break;
//          }
//      }
//  }
//  return val;
//}
//  @Override
//  public String[] getApplicableValues() {
//      // Does this option instance have the list of values?
//      if (applicableValuesList == null) {
//          return EMPTY_STRING_ARRAY;
//      }
//      // Get all of the enumerated names from the option
//      if (applicableValuesList.size() == 0) {
//          return EMPTY_STRING_ARRAY;
//      }
//      // Return the elements in the order they are specified in the manifest
//      String[] enumNames = new String[applicableValuesList.size()];
//      for (int index = 0; index < applicableValuesList.size(); ++index) {
//          enumNames[index] = namesMap.get(applicableValuesList.get(index));
//      }
//      return enumNames;
//  }

//@Override
//public void remove() {
//  ((TreeOption) parent).children.remove(this);
//
//}

//@Override
//public ITreeOption addChild(String id, String name) {
//  ITreeOption option = new TreeOption(id, name, this);
//  if (children == null) {
//      children = new ArrayList<>();
//  }
//  children.add(0, option);
//  return option;
//}

//@Override
//public ITreeOption addNode(String id, String name, String category, int order) {
//  ITreeOption parent = this;
//  if (category != null && category.length() > 0) {
//      ITreeOption tempParent;
//      String[] categories = category.split("\\."); //$NON-NLS-1$
//      for (String cat : categories) {
//          tempParent = parent.getChild(cat);
//          if (tempParent == null) {
//              tempParent = parent.addChild(cat, cat);
//              tempParent.setOrder(order);
//          }
//          parent = tempParent;
//      }
//  }
//
//  ITreeOption child = parent.addChild(id, name);
//  child.setOrder(order);
//  return child;
//}

//TreeOption(TreeOption clone, ITreeOption parent) {
//treeNodeId = clone.treeNodeId;
//treeNodeName = clone.treeNodeName;
//description = clone.description;
//command = clone.command;
//icon = clone.icon;
//order = clone.order;
//this.parent = parent;
//
//if (clone.children != null) {
//  children = new ArrayList<>();
//  for (ITreeOption cloneChild : clone.children) {
//      children.add(new TreeOption((TreeOption) cloneChild, this));
//  }
//}
//}

//@Override
//public String[] getUserObjects() throws BuildException {
//  if (getValueType() != OBJECTS) {
//      throw new BuildException(Option_error_bad_value_type);
//  }
//  // This is the right puppy, so return its list value
//  @SuppressWarnings("unchecked")
//  ArrayList<String> v = (ArrayList<String>) getValue();
//  if (v == null) {
//      return EMPTY_STRING_ARRAY;
//  }
//  v.trimToSize();
//  return v.toArray(new String[v.size()]);
//}

//@Override
//public String getEnumeratedId(String inName) throws BuildException {
//  return getId(inName);
//}
//
//@Override
//public String getId(String inName) throws BuildException {
//  if (inName == null) {
//      return null;
//  }
//
//  // Does this option instance have the list of values?
//  if (applicableValuesList == null) {
//      return EMPTY_STRING;
//  }
//  if (getValueType() != ENUMERATED && getValueType() != TREE) {
//      throw new BuildException(Option_error_bad_value_type);
//  }
//
//  Set<String> idSet = namesMap.keySet();
//  for (String id2 : idSet) {
//      String enumName = namesMap.get(id2);
//      if (inName.equals(enumName)) {
//          return id2;
//      }
//  }
//  return null;
//}
//        private ITreeOption find(String id, List<ITreeOption> inChildren) {
//            ITreeOption found = null;
//            if (inChildren != null) {
//                for (ITreeOption child : inChildren) {
//                    if (id.equals(child.getID())) {
//                        found = child;
//                        break;
//                    }
//                    found = find(id, ((TreeOption) child).children);
//                    if (found != null) {
//                        break;
//                    }
//                }
//            }
//            return found;
//        }

//@Override
//public boolean getBooleanValue() {
//  return ((Boolean) getValue()).booleanValue();
//}

//@Override
//public IOptionCommandGenerator getCommandGenerator() {
//  return commandGenerator;
//}

//@Override
//public String getCommand() {
//    return;
//}

//@Override
//public String getCommandFalse() {
//  return modelCommandFalse[SUPER];
//}

//@Override
//public IOptionDefaultValueGenerator getDefaultValueGenerator() {
//  return defaultValueGenerator;
//}

// 
//    @Override
//    public String getEnumCommand(String commandID) throws BuildException {
//        return getCommand(commandID);
//    }

//@Override
//public String[] getIncludePaths() throws BuildException {
//  if (getValueType() != INCLUDE_PATH) {
//      throw new BuildException(Option_error_bad_value_type);
//  }
//  @SuppressWarnings("unchecked")
//  ArrayList<String> v = (ArrayList<String>) getValue();
//  if (v == null) {
//      return EMPTY_STRING_ARRAY;
//  }
//  v.trimToSize();
//  return v.toArray(new String[v.size()]);
//}
//
//@Override
//public String[] getLibraries() throws BuildException {
//  if (getValueType() != LIBRARIES) {
//      throw new BuildException(Option_error_bad_value_type);
//  }
//  @SuppressWarnings("unchecked")
//  ArrayList<String> v = (ArrayList<String>) getValue();
//  if (v == null) {
//      return EMPTY_STRING_ARRAY;
//  }
//  v.trimToSize();
//  return v.toArray(new String[v.size()]);
//}

//
//@Override
//public String[] getLibraryFiles() throws BuildException {
//  if (getValueType() != LIBRARY_FILES) {
//      throw new BuildException(Option_error_bad_value_type);
//  }
//  @SuppressWarnings("unchecked")
//  ArrayList<String> v = (ArrayList<String>) getValue();
//  if (v == null) {
//      return EMPTY_STRING_ARRAY;
//  }
//  v.trimToSize();
//  return v.toArray(new String[v.size()]);
//}
//
//@Override
//public String[] getLibraryPaths() throws BuildException {
//  if (getValueType() != LIBRARY_PATHS) {
//      throw new BuildException(Option_error_bad_value_type);
//  }
//  @SuppressWarnings("unchecked")
//  ArrayList<String> v = (ArrayList<String>) getValue();
//  if (v == null) {
//      return EMPTY_STRING_ARRAY;
//  }
//  v.trimToSize();
//  return v.toArray(new String[v.size()]);
//}

//    private static List<OptionStringValue> valueListToListValueList(List<String> list, boolean builtIn) {
//        if (list == null) {
//            return null;
//        }
//
//        List<OptionStringValue> lvList = new ArrayList<>(list.size());
//        for (int i = 0; i < list.size(); i++) {
//            String v = list.get(i);
//            lvList.add(new OptionStringValue(v, builtIn));
//        }
//        return lvList;
//    }

//@Override
//public OptionStringValue[] getBasicStringListValueElements() throws BuildException {
//  if (getBasicValueType() != STRING_LIST) {
//      throw new BuildException(Option_error_bad_value_type);
//  }
//  @SuppressWarnings("unchecked")
//  ArrayList<OptionStringValue> v = (ArrayList<OptionStringValue>) getValue();
//  if (v == null) {
//      return EMPTY_LV_ARRAY;
//  }
//
//  return v.toArray(new OptionStringValue[v.size()]);
//}

//@Override
//public String[] getDefinedSymbols() throws BuildException {
//  if (getValueType() != PREPROCESSOR_SYMBOLS) {
//      throw new BuildException(Option_error_bad_value_type);
//  }
//  @SuppressWarnings("unchecked")
//  ArrayList<String> v = (ArrayList<String>) getValue();
//  if (v == null) {
//      return EMPTY_STRING_ARRAY;
//  }
//  v.trimToSize();
//  return v.toArray(new String[v.size()]);
//}
//@Override
//public String getStringValue() throws BuildException {
//  if (getValueType() != STRING && getValueType() != ENUMERATED && getValueType() != TREE) {
//      throw new BuildException(Option_error_bad_value_type);
//  }
//  return getValue() == null ? EMPTY_STRING : (String) getValue();
//}
//@Override
//public String getSelectedEnum() throws BuildException {
//  if (getValueType() != ENUMERATED) {
//      throw new BuildException(Option_error_bad_value_type);
//  }
//  return getStringValue();
//}

//@Override
//public String[] getStringListValue() throws BuildException {
//  if (getValueType() != STRING_LIST) {
//      throw new BuildException(Option_error_bad_value_type);
//  }
//  @SuppressWarnings("unchecked")
//  ArrayList<String> v = (ArrayList<String>) getValue();
//  if (v == null) {
//      return EMPTY_STRING_ARRAY;
//  }
//  v.trimToSize();
//  return v.toArray(new String[v.size()]);
//}

///**
//* Gets the value, applying appropriate defaults if necessary.
//*/
////    @Override
//public Object getValue() {
// /*
//  *  In order to determine the current value of an option, perform the following steps until a value is found:
//  *   1.	Examine the value attribute of the option.
//  *   2.	Examine the value attribute of the option's superClass recursively.
//  *   3.	Examine the dynamicDefaultValue attribute of the option and invoke it if specified. (not yet implemented)
//  *   4.	Examine the defaultValue attribute of the option.
//  *   5.	Examine the dynamicDefaultValue attribute of the option's superClass and invoke it if specified. (not yet implemented)
//  *   6.	Examine the defaultValue attribute of the option's superClass.
//  *   7.	Go to step 5 recursively until no more super classes.
//  *   8.	Use the default value for the option type.
//  */
//
// Object val = getRawValue();
// if (val == null) {
//     val = getDefaultValue();
//     if (val == null) {
//         int valType;
//         try {
//             valType = getValueType();
//         } catch (BuildException e) {
//             Activator.log(e);
//             return EMPTY_STRING;
//         }
//         switch (valType) {
//         case BOOLEAN:
//             val = Boolean.FALSE;
//             break;
//         case STRING:
//         case TREE:
//             val = EMPTY_STRING;
//             break;
//         case ENUMERATED:
//             // TO DO: Can we default to the first enumerated id?
//             val = EMPTY_STRING;
//             break;
//         case STRING_LIST:
//         case INCLUDE_PATH:
//         case PREPROCESSOR_SYMBOLS:
//         case LIBRARIES:
//         case OBJECTS:
//         case INCLUDE_FILES:
//         case LIBRARY_PATHS:
//         case LIBRARY_FILES:
//         case MACRO_FILES:
//         case UNDEF_INCLUDE_PATH:
//         case UNDEF_PREPROCESSOR_SYMBOLS:
//         case UNDEF_INCLUDE_FILES:
//         case UNDEF_LIBRARY_PATHS:
//         case UNDEF_LIBRARY_FILES:
//         case UNDEF_MACRO_FILES:
//             val = new ArrayList<String>();
//             break;
//         default:
//             val = EMPTY_STRING;
//             break;
//         }
//     }
// }
// return val;
//}

//// 
///**
//* Gets the raw value, applying appropriate defauls if necessary.
//*/
//private Object getRawValue() {
//  Object ev = getValue();
//  if (ev instanceof List<?>) {
//      @SuppressWarnings("unchecked")
//      List<String> evList = listValueListToValueList((List<OptionStringValue>) ev);
//      return evList;
//  }
//  return ev;
//}

///**
//* Gets the raw default value.
//*/
//@Override
//public Object getDefaultValue() {
// Object ev = myDefaultValue;
// if (ev instanceof List<?>) {
//     @SuppressWarnings("unchecked")
//     List<String> evList = listValueListToValueList((List<OptionStringValue>) ev);
//     return evList;
// }
// return ev;
//}

//public Object makeStringValueObject(String string) {
//switch (valueType) {
//case BOOLEAN:
//  // Convert the string to a Boolean
//  return Boolean.valueOf(string);
//case STRING:
//  // Just get the value out of the option directly
//  return string;
//case TREE:
//case ENUMERATED:
//  if (myTreeRoot != null) {
//      return myTreeRoot.findOptionOnValue(string);
//  }
//  break;
//case STRING_LIST:
//case INCLUDE_PATH:
//case PREPROCESSOR_SYMBOLS:
//case LIBRARIES:
//case OBJECTS:
//case INCLUDE_FILES:
//case LIBRARY_PATHS:
//case LIBRARY_FILES:
//case MACRO_FILES:
//case UNDEF_INCLUDE_PATH:
//case UNDEF_PREPROCESSOR_SYMBOLS:
//case UNDEF_INCLUDE_FILES:
//case UNDEF_LIBRARY_PATHS:
//case UNDEF_LIBRARY_FILES:
//case UNDEF_MACRO_FILES:
//  //  Note:  These string-list options do not load either the "value" or
//  //         "defaultValue" attributes.  Instead, the ListOptionValue children
//  //         are loaded in the value field.
//  List<OptionStringValue> vList = new ArrayList<>();
//  //            IConfigurationElement[] vElements = element.getChildren(LIST_VALUE);
//  //            for (IConfigurationElement vElement : vElements) {
//  //                OptionStringValue ve = new OptionStringValue(vElement);
//  //                if (ve.isBuiltIn()) {
//  //                    myBuiltIns.add(ve);
//  //                } else {
//  //                    vList.add(ve);
//  //                }
//  //            }
//  return vList;
//}
//return null;
//}

//    @Override
//    public String[] getBasicStringListValue() throws BuildException {
//        if (getBasicValueType() != STRING_LIST) {
//            throw new BuildException(Option_error_bad_value_type);
//        }
//        return internalGetBasicStringListValue();
//    }

//private String[] internalGetBasicStringListValue(Object value) {
//@SuppressWarnings("unchecked")
//ArrayList<String> v = (ArrayList<String>) value;
//if (v == null) {
//	return EMPTY_STRING_ARRAY;
//}
//
//return v.toArray(new String[v.size()]);
//}