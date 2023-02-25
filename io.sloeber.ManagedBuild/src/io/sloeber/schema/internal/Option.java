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
import static io.sloeber.autoBuild.integration.AutoBuildConstants.*;

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
	public String getDefaultValue(IResource resource, AutoBuildConfigurationData autoData) {
		String ret = EMPTY_STRING;
		if (defaultValueGenerator != null) {
			return defaultValueGenerator.generateDefaultValue(this);
		}

		if(!myEnablement.isBlank()) {
			ret=myEnablement.getDefaultValue( resource,  autoData);
			if(!ret.isBlank()) {
				return ret;
			}
		}
		if (myTreeRoot != null) {
			return myTreeRoot.getDefaultValueString();
		}
		if (myEnumOptionValues.size() > 0) {
			for (EnumOptionValue cur : myEnumOptionValues.values()) {
				if (cur.isDefault()) {
					return cur.getID();
				}
				if (ret == null) {
					if (cur.getCommandLIneDistribution().isBlank()) {
						ret = EMPTY_STRING;
					} else {
						ret = cur.getID();
					}
				}
			}
		}
		if ((!modelDefaultValueString[SUPER].isBlank()) || (ret == null)) {
			return modelDefaultValueString[SUPER];
		}
		return ret;
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
