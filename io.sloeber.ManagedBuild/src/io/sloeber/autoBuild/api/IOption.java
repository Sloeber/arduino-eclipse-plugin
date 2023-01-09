/*******************************************************************************
 * Copyright (c) 2003, 2013 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM - Initial API and implementation
 *     ARM Ltd. - basic tooltip support
 *     James Blackburn (Broadcom Corp.)
 *     Petri Tuononen - [321040] Get Library Search Paths
 *     Baltasar Belyavsky (Texas Instruments) - [279633] Custom command-generator support
 *     cartu38 opendev (STMicroelectronics) - [514385] Custom defaultValue-generator support
 *******************************************************************************/
package io.sloeber.autoBuild.api;

import io.sloeber.autoBuild.extensionPoint.IManagedOptionValueHandler;
import io.sloeber.autoBuild.extensionPoint.IOptionApplicability;
import io.sloeber.autoBuild.extensionPoint.IOptionCommandGenerator;
import io.sloeber.autoBuild.extensionPoint.IOptionDefaultValueGenerator;

//import org.eclipse.cdt.managedbuilder.macros.IOptionContextData;

/**
 * Basic Tool / Tool-chain Option type.
 *
 * @noextend This class is not intended to be subclassed by clients.
 * @noimplement This interface is not intended to be implemented by clients.
 */
public interface IOption extends IBuildObject {
    // Type for the value of the option
    public static final int BOOLEAN = 0;
    public static final int ENUMERATED = 1;
    public static final int STRING = 2;
    public static final int STRING_LIST = 3;
    public static final int INCLUDE_PATH = 4;
    public static final int PREPROCESSOR_SYMBOLS = 5;
    /**
     * String list of library names to link against searched for
     * via LIBRARY_PATHS by the linker. In the GNU
     * tool-chain these correspond to -l{lib_name}. <br/>
     * This option type is persisted / referenced by the name
     * {@link IOption#TYPE_LIB}
     */
    public static final int LIBRARIES = 6;
    public static final int OBJECTS = 7;
    public static final int INCLUDE_FILES = 8;
    /**
     * String list of library search paths <br/>
     * This option type is persisted / referenced by the name
     * {@link IOption#TYPE_LIB_PATHS}
     */
    public static final int LIBRARY_PATHS = 9;
    /**
     * String list of absolute path to libraries.
     * Not currently used by the GNU integration <br/>
     * This option type is persisted / referenced by the name 'libFiles'
     * {@link IOption#TYPE_LIB_FILES}
     */
    public static final int LIBRARY_FILES = 10;
    public static final int MACRO_FILES = 11;

    /**
     * Tree of items to select one from.
     * 
     * @since 8.1
     */
    public static final int TREE = 12;

    public static final int UNDEF_INCLUDE_PATH = -INCLUDE_PATH;
    public static final int UNDEF_PREPROCESSOR_SYMBOLS = -PREPROCESSOR_SYMBOLS;
    public static final int UNDEF_INCLUDE_FILES = -INCLUDE_FILES;
    public static final int UNDEF_LIBRARY_PATHS = -LIBRARY_PATHS;
    public static final int UNDEF_LIBRARY_FILES = -LIBRARY_FILES;
    public static final int UNDEF_MACRO_FILES = -MACRO_FILES;

    // Browse type
    public static final int BROWSE_NONE = 0;
    public static final String NONE = "none"; //$NON-NLS-1$
    public static final int BROWSE_FILE = 1;
    public static final String FILE = "file"; //$NON-NLS-1$
    public static final int BROWSE_DIR = 2;
    public static final String DIR = "directory"; //$NON-NLS-1$

    // Resource Filter type
    public static final int FILTER_ALL = 0;
    public static final String ALL = "all"; //$NON-NLS-1$
    public static final int FILTER_FILE = 1;
    public static final int FILTER_PROJECT = 2;
    public static final String PROJECT = "project"; //$NON-NLS-1$

    // Schema attribute names for option elements
    public static final String BROWSE_TYPE = "browseType"; //$NON-NLS-1$
    /** @since 7.0 */
    public static final String BROWSE_FILTER_PATH = "browseFilterPath"; //$NON-NLS-1$
    /** @since 7.0 */
    public static final String BROWSE_FILTER_EXTENSIONS = "browseFilterExtensions"; //$NON-NLS-1$
    public static final String CATEGORY = "category"; //$NON-NLS-1$
    /**
     * @since 8.1
     */
    public static final String ICON = "icon"; //$NON-NLS-1$
    /**
     * @since 8.1
     */
    public static final String ORDER = "order"; //$NON-NLS-1$
    public static final String COMMAND = "command"; //$NON-NLS-1$
    public static final String COMMAND_FALSE = "commandFalse"; //$NON-NLS-1$
    /** @since 8.3 */
    public static final String USE_BY_SCANNER_DISCOVERY = "useByScannerDiscovery"; //$NON-NLS-1$
    /** @since 8.0 */
    public static final String COMMAND_GENERATOR = "commandGenerator"; //$NON-NLS-1$
    public static final String TOOL_TIP = "tip"; //$NON-NLS-1$
    public static final String CONTEXT_ID = "contextId"; //$NON-NLS-1$
    public static final String DEFAULT_VALUE = "defaultValue"; //$NON-NLS-1$
    /**
     * @since 8.5
     */
    public static final String DEFAULTVALUE_GENERATOR = "defaultValueGenerator"; //$NON-NLS-1$
    public static final String ENUM_VALUE = "enumeratedOptionValue"; //$NON-NLS-1$
    /**
     * @since 8.1
     */
    public static final String TREE_ROOT = "treeOptionRoot"; //$NON-NLS-1$
    /**
     * @since 8.1
     */
    public static final String SELECT_LEAF_ONLY = "selectLeafOnly"; //$NON-NLS-1$
    /**
     * @since 8.1
     */
    public static final String TREE_VALUE = "treeOption"; //$NON-NLS-1$
    /**
     * @since 8.1
     */
    public static final String DESCRIPTION = "description"; //$NON-NLS-1$
    public static final String IS_DEFAULT = "isDefault"; //$NON-NLS-1$
    public static final String LIST_VALUE = "listOptionValue"; //$NON-NLS-1$
    public static final String RESOURCE_FILTER = "resourceFilter"; //$NON-NLS-1$
    public static final String APPLICABILITY_CALCULATOR = "applicabilityCalculator"; //$NON-NLS-1$
    public static final String TYPE_BOOL = "boolean"; //$NON-NLS-1$
    public static final String TYPE_ENUM = "enumerated"; //$NON-NLS-1$
    public static final String TYPE_INC_PATH = "includePath"; //$NON-NLS-1$
    public static final String TYPE_LIB = "libs"; //$NON-NLS-1$
    public static final String TYPE_STRING = "string"; //$NON-NLS-1$
    public static final String TYPE_STR_LIST = "stringList"; //$NON-NLS-1$
    public static final String TYPE_USER_OBJS = "userObjs"; //$NON-NLS-1$
    public static final String TYPE_DEFINED_SYMBOLS = "definedSymbols"; //$NON-NLS-1$
    public static final String TYPE_LIB_PATHS = "libPaths"; //$NON-NLS-1$
    public static final String TYPE_LIB_FILES = "libFiles"; //$NON-NLS-1$
    public static final String TYPE_INC_FILES = "includeFiles"; //$NON-NLS-1$
    public static final String TYPE_SYMBOL_FILES = "symbolFiles"; //$NON-NLS-1$
    public static final String TYPE_UNDEF_INC_PATH = "undefIncludePath"; //$NON-NLS-1$
    public static final String TYPE_UNDEF_DEFINED_SYMBOLS = "undefDefinedSymbols"; //$NON-NLS-1$
    public static final String TYPE_UNDEF_LIB_PATHS = "undefLibPaths"; //$NON-NLS-1$
    public static final String TYPE_UNDEF_LIB_FILES = "undefLibFiles"; //$NON-NLS-1$
    public static final String TYPE_UNDEF_INC_FILES = "undefIncludeFiles"; //$NON-NLS-1$
    public static final String TYPE_UNDEF_SYMBOL_FILES = "undefSymbolFiles"; //$NON-NLS-1$
    /**
     * @since 8.1
     */
    public static final String TYPE_TREE = "tree"; //$NON-NLS-1$

    public static final String VALUE = "value"; //$NON-NLS-1$
    public static final String VALUE_TYPE = "valueType"; //$NON-NLS-1$
    public static final String VALUE_HANDLER = "valueHandler"; //$NON-NLS-1$
    public static final String VALUE_HANDLER_EXTRA_ARGUMENT = "valueHandlerExtraArgument"; //$NON-NLS-1$

    /** @since 8.0 */
    public static final String FIELD_EDITOR_ID = "fieldEditor"; //$NON-NLS-1$
    /** @since 8.0 */
    public static final String FIELD_EDITOR_EXTRA_ARGUMENT = "fieldEditorExtraArgument"; //$NON-NLS-1$

    // Schema attribute names for listOptionValue elements
    public static final String LIST_ITEM_VALUE = "value"; //$NON-NLS-1$
    public static final String LIST_ITEM_BUILTIN = "builtIn"; //$NON-NLS-1$
    public static final String EMPTY_STRING = "";

    /**
     * @return the parent of this option. This is an object implementing ITool
     *         or IToolChain.
     *
     * @since 3.0 - changed return type from ITool to IBuildObject. The method
     *        returns
     *        the same object as getOptionHolder(). It is included as a convenience
     *        for clients.
     */
    public IBuildObject getParent();

    /**
     * @return the holder (parent) of this option. This may be an object
     *         implementing ITool or IToolChain, which both extend IHoldsOptions
     *
     * @since 3.0
     */
    public IHoldsOptions getOptionHolder();

    /**
     * @param holder
     *            - the actual option-holder for the context-data. This holder
     *            is usually a subclass of this option's {@link #getOptionHolder()
     *            holder}.
     * @return the option context-data to be used for macro resolution.
     * @since 7.0
     */
    //public IOptionContextData getOptionContextData(IHoldsOptions holder);

    /**
     * @return If this option is defined as an enumeration, this function returns
     *         the list of possible values for that enum.
     *
     *         If this option is not defined as an enumeration, it returns
     *         <code>null</code>.
     */
    public String[] getApplicableValues();

    /**
     * @return the value for a boolean option.
     */
    public boolean getBooleanValue() throws BuildException;

    /**
     * @return the setting of the browseType attribute
     */
    public int getBrowseType();

    /**
     * Sets the browseType attribute.
     *
     * @param type
     *            - browseType attribute
     */
    public void setBrowseType(int type);

    /**
     * @return the setting of the browseFilterPath attribute. For options of
     *         {@link #BROWSE_FILE} and {@link #BROWSE_DIR} types.
     * @since 7.0
     */
    public String getBrowseFilterPath();

    /**
     * Sets the browseFilterPath attribute. For options of {@link #BROWSE_FILE} and
     * {@link #BROWSE_DIR} types.
     * 
     * @param path
     *            - default filter-path for the underlying browse dialog
     * @since 7.0
     */
    public void setBrowseFilterPath(String path);

    /**
     * @return the setting of the browseFilterExtensions attribute. For options of
     *         {@link #BROWSE_FILE} type.
     * @since 7.0
     */
    public String[] getBrowseFilterExtensions();

    /**
     * Sets the browseFilterExtensions attribute. For options of
     * {@link #BROWSE_FILE} type.
     * 
     * @param extensions
     *            - file extensions to show in browse files dialog
     *
     * @since 7.0
     */
    public void setBrowseFilterExtensions(String[] extensions);

    /**
     * @return the setting of the resourceFilter attribute
     */
    public int getResourceFilter();

    /**
     * Sets the resourceFilter attribute.
     *
     * @param filter
     *            - resourceFilter attribute
     */
    public void setResourceFilter(int filter);

    /**
     * @return an instance of the class that calculates whether the option is
     *         visible,
     *         enabled, and used in command line generation
     */
    public IOptionApplicability getApplicabilityCalculator();

    /**
     * @return an array of strings containing the built-in values
     *         defined for a stringList, includePaths, definedSymbols, or libs
     *         option. If none have been defined, the array will be empty but
     *         never <code>null</code>.
     */
    public String[] getBuiltIns();

    /**
     * @return the category for this option.
     */
    public IOptionCategory getCategory();

    /**
     * Sets the category for this option.
     */
    public void setCategory(IOptionCategory category);

    /**
     * @return a <code>String</code> containing the actual command line
     *         option associated with the option
     */
    public String getCommand();

    /**
     * @return an instance of the class that overrides the default command
     *         generation for the option
     * @since 8.0
     */
    public IOptionCommandGenerator getCommandGenerator();

    /**
     * Sets a <code>String</code> containing the actual command line
     * option associated with the option
     *
     * @param command
     *            - the actual command line option
     */
    public void setCommand(String command);

    /**
     * @return {@code String} containing the actual command line
     *         option associated with a Boolean option when the value is
     *         {@code false}
     */
    public String getCommandFalse();

    /**
     * Sets a <code>String</code> containing the actual command line
     * option associated with a Boolean option when the value is {@code false}
     *
     * @param commandFalse
     *            - the actual command line option associated
     *            with a Boolean option when the value is {@code false}
     */
    public void setCommandFalse(String commandFalse);

    /**
     * @return a <code>String</code> containing the tooltip
     *         associated with the option
     */
    public String getToolTip();

    /**
     * Sets a <code>String</code> containing the tooltip associated with the option
     *
     * @param tooltip
     *            - the tooltip associated with the option
     */
    public void setToolTip(String tooltip);

    /**
     * @return a <code>String</code> containing the contextId
     *         associated with the option
     */
    public String getContextId();

    /**
     * Sets a <code>String</code> containing the contextId associated with the
     * option
     *
     * @param id
     *            - the contextId associated with the option
     */
    public void setContextId(String id);

    /**
     * @return the user-defined preprocessor symbols.
     */
    public String[] getDefinedSymbols() throws BuildException;

    /**
     *
     * @param id
     *            - enumeration id
     * @return the command associated with the enumeration id. For
     *         example, if the enumeration id was
     *         <code>gnu.debug.level.default</code>
     *         for the debug level option of the Gnu compiler, and the plugin
     *         manifest defined that as -g, then the return value would be the
     *         String "-g"
     */
    public String getEnumCommand(String id) throws BuildException;

    /**
     * Returns the command associated with the child of this option
     * with the given id. Applies to options of types that has children
     * for example {@link #TREE} or {@link #ENUMERATED}
     *
     * @param id
     *            - child id
     * @return the command associated with the child id. For
     *         example, if the child id was <code>gnu.debug.level.default</code>
     *         for the debug level option of the Gnu compiler, and the plugin
     *         manifest defined that as -g, then the return value would be the
     *         String "-g"
     *
     * @throws BuildException
     *             if this option is not of type {@link #TREE} or
     *             {@link #ENUMERATED}
     * @since 8.1
     */
    public String getCommand(String id) throws BuildException;

    /**
     * @param id
     *            - enumeration id
     * @return the "name" associated with the enumeration id.
     */
    public String getEnumName(String id) throws BuildException;

    /**
     * Returns the name associated with the child of this option
     * with the given id. Applies to options of types that has children
     * for example {@link #TREE} or {@link #ENUMERATED}
     *
     * @param id
     *            The id to look for
     * @return Name of the child with the passed id or <code>null</code> if not
     *         found.
     * @throws BuildException
     *             if any issue happened while searching.
     * @since 8.1
     */
    public abstract String getName(String id) throws BuildException;

    /**
     * @param name
     *            - a "name" associated with enumeration id
     * @return enumeration id
     */
    public String getEnumeratedId(String name) throws BuildException;

    /**
     * Returns the id associated with the child of this option
     * with the given name. Applies to options of types that has children
     * for example {@link #TREE} or {@link #ENUMERATED}
     *
     * @param name
     *            the name of the child to look for.
     * @return The id of the found child or <code>null</code> if not found.
     * @throws BuildException
     *             if any error happened while searching
     * @since 8.1
     */
    public abstract String getId(String name) throws BuildException;

    /**
     * @return an array of <code>String</code> containing the includes paths
     *         defined in the build model.
     */
    public String[] getIncludePaths() throws BuildException;

    /**
     * @return an array or <code>String</code>s containing the libraries
     *         that must be linked into the project.
     */
    public String[] getLibraries() throws BuildException;

    /**
     * @return an array or <code>String</code>s containing the library files
     *         that must be linked into the project.
     *
     * @since 7.0
     */
    public String[] getLibraryFiles() throws BuildException;

    /**
     * @return an array or <code>String</code>s containing the library paths
     *         passed to the linker.
     *
     * @throws BuildException
     *             if the option isn't of type IOption#LIBRARY_PATHS
     * @since 8.0
     */
    public String[] getLibraryPaths() throws BuildException;

    /**
     * @return a <code>String</code> containing the unique ID of the selected
     *         enumeration in an enumerated option. For an option that has not been
     *         changed by the user, the receiver will answer with the default
     *         defined
     *         in the plugin manifest. If the user has modified the selection, the
     *         receiver will answer with the overridden selection.
     *
     * @throws BuildException
     *             if the option type is not an enumeration
     */
    public String getSelectedEnum() throws BuildException;

    /**
     * @return the current value for this option if it is a List of Strings.
     */
    public String[] getStringListValue() throws BuildException;

    /**
     * @return the current value for this option if it is a String
     */
    public String getStringValue() throws BuildException;

    /**
     * @return all of the user-defined object files that must be linked with
     *         the final build target.
     */
    public String[] getUserObjects() throws BuildException;

    /**
     * @return the raw value of this option which is the Object that contains the
     *         raw value of the option.
     *         The type of Object is specific to the option type.
     */
    public Object getValue();

    /**
     * @return the raw default value of this option which is the Object that
     *         contains the raw default value of the option.
     *         The type of Object is specific to the option type.
     */
    public Object getDefaultValue();

    /**
     * @return an instance of the class that overrides the default defaultValue
     *         generation for the option
     * @since 8.5
     */
    public IOptionDefaultValueGenerator getDefaultValueGenerator();

    /**
     * @return the type for the value of the option.
     */
    public int getValueType() throws BuildException;

    /**
     * Sets the boolean value of the receiver to the value specified in the
     * argument.
     * If the receiver is not a reference to a boolean option, method will throw an
     * exception.
     */
    public void setValue(boolean value) throws BuildException;

    /**
     * Sets the string value of the receiver to the value specified in the argument.
     */
    public void setValue(String value) throws BuildException;

    /**
     * Sets the value of the receiver to be an array of strings.
     *
     * @param value
     *            An array of strings to place in the option reference.
     */
    public void setValue(String[] value) throws BuildException;

    /**
     * Sets the raw value of this option.
     *
     * @param v
     *            The Object that contains the raw value of the option. The type
     *            of Object is specific to the option type.
     */
    public void setValue(Object v);

    /**
     * Sets the default value of this option.
     *
     * @param v
     *            The Object that contains the default value of the option. The type
     *            of Object is specific to the option type.
     */
    public void setDefaultValue(Object v);

    /**
     * Sets the value-type of this option. Use with care.
     */
    public void setValueType(int type);

    /**
     * @return the value handler specified for this tool.
     * @since 3.0
     */
    public IManagedOptionValueHandler getValueHandler();

    /**
     * @return the value handlers extra argument specified for this tool
     * @since 3.0
     */
    public String getValueHandlerExtraArgument();

    /**
     * Sets the value handlers extra argument specified for this tool
     * 
     * @since 3.0
     */
    public void setValueHandlerExtraArgument(String extraArgument);

    /**
     * @return the custom field-editor ID for this build-option. This ID should
     *         match a custom-field editor
     *         contributed through the {@code <fieldEditor>} element of the
     *         {@code org.eclipse.cdt.managedbuilder.ui.buildDefinitionsUI}
     *         extension-point.
     * @since 8.0
     */
    public String getFieldEditorId();

    /**
     * @return an optional extra argument for the {@link #getFieldEditorId()
     *         field-editor}.
     * @since 8.0
     */
    public String getFieldEditorExtraArgument();

    /**
     * Sets the optional extra argument for the field-editor.
     * 
     * @param extraArgument
     *            free-form extra argument to be interpreted by the
     *            {@link #getFieldEditorId() field-editor}
     * @since 8.0
     */
    public void setFieldEditorExtraArgument(String extraArgument);

    /**
     * @return <code>true</code> if this option was loaded from a manifest file,
     *         and <code>false</code> if it was loaded from a project (.cdtbuild)
     *         file.
     */
    public boolean isExtensionElement();

    /**
     * @return <code>true</code> if this option is valid and <code>false</code>
     *         if the option cannot be safely used due to an error in the MBS
     *         grammar.
     *
     * @since 3.0
     *
     * @pre Can only be used after Ids in MBS grammar have been resolved by
     *      pointers.
     */
    public boolean isValid();

    /**
     * @return the type of the option value, i.e. whether it is string, boolean,
     *         string list or enumeration. As opposed to the getValueType() method,
     *         the returned type does not specifies the "sense" of the value, e.g.
     *         whether it represents the list of includes or not.
     *
     *         <br/>
     *         Possible return values:
     *         <li/>{@link IOption#BOOLEAN}
     *         <li/>{@link IOption#STRING}
     *         <li/>{@link IOption#ENUMERATED}
     *         <li/>{@link IOption#TREE}
     *         <li/>{@link IOption#STRING_LIST} - corresponds to
     *         {@link IOption#INCLUDE_PATH}, {@link IOption#PREPROCESSOR_SYMBOLS},
     *         {@link IOption#LIBRARIES},
     *         {@link IOption#OBJECTS}, {@link IOption#INCLUDE_FILES},
     *         {@link IOption#LIBRARY_PATHS},
     *         {@link IOption#LIBRARY_FILES}, {@link IOption#MACRO_FILES}
     */
    int getBasicValueType() throws BuildException;

    /**
     * @return in case the option basic value type is STRING_LIST, returns the
     *         String list value,
     *         throws BuildException otherwise
     */
    String[] getBasicStringListValue() throws BuildException;

    public OptionStringValue[] getBasicStringListValueElements() throws BuildException;

    /**
     * Flag to indicate whether the option is also used by scanner discovery.
     * 
     * @return {@code true} if the option is intended to be passed to scanner
     *         discovery command
     *         or {@code false} otherwise.
     *
     * @since 8.3
     */
    public boolean isForScannerDiscovery();

    /**
     * Returns the tree root of this option if it is of type {@link #TREE}
     * 
     * @return tree root of this option or <code>null</code> if not found.
     * @throws BuildException
     *             if this option is not of type {@link #TREE}
     * @since 8.1
     */
    public ITreeRoot getTreeRoot() throws BuildException;

    /**
     * Represents the root of the tree of values in options of
     * type {@link IOption#TREE}
     * 
     * @author mhussein
     * @since 8.1
     *
     */
    public interface ITreeRoot extends ITreeOption {
        /**
         * Determines whether this tree allows selecting leaf nodes
         * only or any nodes.
         * 
         * @return <code>true</code> if only leaf nodes are allowed.
         *         <code>false</code> if all child nodes could be selected.
         * @see ITreeOption#isContainer()
         */
        boolean isSelectLeafsOnly();

        /**
         * Locates the node with the given id anywhere in the tree.
         * 
         * @param id
         *            the id to search for
         * @return the found child or <code>null</code> if not found.
         */
        ITreeOption findNode(String id);

        /**
         * Adds a new node to the tree.
         * 
         * @param id
         *            The id of the new child.
         * @param name
         *            The name of the new child.
         * @param category
         *            The category of the new child.category is a '.'
         *            separated string representing hierarchical path
         *            of the child from the root of the tree.
         *            can cause other nodes to be created to construct the
         *            full path to the new child.
         * @param order
         *            The order of the newly created node among its peers.
         *            see {@link ITreeOption#getOrder()} for more information.
         *            Note: this order will apply to any parents auto-created
         *            according to the passed category.
         *            if <code>null</code> the {@link ITreeOption#DEFAULT_ORDER}
         *            will be used.
         * @return the newly added node.
         */
        ITreeOption addNode(String id, String name, String category, Integer order);
    }

    /**
     * Represents a one of the possible values for options of type
     * {@link IOption#TREE}
     * 
     * @author mhussein
     * @since 8.1
     *
     */
    public interface ITreeOption {
        /**
         * The default order for tree nodes without order specified.
         * Tree options with Orders smaller than this should appear above
         * tree options with no order specified and vice versa.
         */
        public static final int DEFAULT_ORDER = 1000;

        String getName();

        String getID();

        String getDescription();

        /**
         * The order that determines UI appearance of the tree node,
         * not necessarily its position in {@link #getChildren()}
         * 
         * @return The order of this tree option relative to its peers.
         *         Smaller number means it should appear above peers.
         * @see #DEFAULT_ORDER
         */
        int getOrder();

        void setOrder(int order);

        ITreeOption[] getChildren();

        ITreeOption getParent();

        boolean isContainer();

        String getCommand();

        ITreeOption getChild(String name);

        /**
         * Adds a new child directly under this node.
         * 
         * @param id
         *            The id of the new child.
         * @param name
         *            The name of the new child.
         * @return The added child.
         */
        ITreeOption addChild(String id, String name);

        void remove();

        String getIcon();
    }
}
