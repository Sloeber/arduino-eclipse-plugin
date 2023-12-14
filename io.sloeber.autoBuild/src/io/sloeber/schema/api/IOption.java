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
package io.sloeber.schema.api;

import org.eclipse.core.resources.IResource;
import io.sloeber.autoBuild.api.BuildException;
import io.sloeber.autoBuild.api.IAutoBuildConfigurationDescription;
import io.sloeber.autoBuild.integration.AutoBuildConfigurationDescription;
import static io.sloeber.autoBuild.integration.AutoBuildConstants.*;

import java.util.Map;

/**
 * Basic Tool / Tool-chain Option type.
 *
 * @noextend This class is not intended to be subclassed by clients.
 * @noimplement This interface is not intended to be implemented by clients.
 */
public interface IOption extends ISchemaObject {
    public static final String ELEMENT_NAME = OPTION;
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
    public static final int FILTER_FILE = 1;
    public static final int FILTER_PROJECT = 2;
    public static final int FILTER_NONE = 1;
    public static final String PROJECT = "project"; //$NON-NLS-1$

    /**
     * @return the parent of this option. This is an object implementing ITool
     *         or IToolChain.
     *
     * @since 3.0 - changed return type from ITool to IBuildObject. The method
     *        returns
     *        the same object as getOptionHolder(). It is included as a convenience
     *        for clients.
     */
    public ISchemaObject getParent();

    /**
     * @return the setting of the browseType attribute
     */
    public int getBrowseType();

    /**
     * @return the setting of the resourceFilter attribute
     */
    public int getResourceFilter();

    /**
     * @return an array of strings containing the built-in values
     *         defined for a stringList, includePaths, definedSymbols, or libs
     *         option. If none have been defined, the array will be empty but
     *         never <code>null</code>.
     */
    public String[] getBuiltIns();

    /**
     * @return a <code>String</code> containing the actual command line
     *         option associated with the option
     */
    public String getCommand();

    /**
     * @return a <code>String</code> containing the tooltip
     *         associated with the option
     */
    public String getToolTip();

    /**
     * @return a <code>String</code> containing the contextId
     *         associated with the option
     */
    public String getContextId();

    /**
     * @param id
     *            - enumeration id
     * @return the "name" associated with the enumeration id.
     *         null if the option is not a enumeration or when the provided id==null
     *         empty string if the option id is not found
     */
    public String getEnumName(String id);

    /**
     * @return the type for the value of the option.
     */
    public int getValueType();

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
    int getBasicValueType();

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
    public interface ITreeRoot {
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

        ITreeOption[] getChildren();

        String getCommand();

        String getIcon();
    }

    /**
     * class to store options for a enum
     * 
     * @author Jan Baeyens
     *
     */
    public interface IEnumOptionValue {
        /**
         * Get the unique Identifier for this enumeration option
         * 
         * @return the identifyer
         */
        String getID();

        /**
         * Get the name for this enumeration option
         * 
         * @return the name
         */
        String getName();

        /**
         * Get the description for this enumeration option
         * 
         * @return the description
         */
        String getDescription();

        /**
         * Get the contribution to the command line for this enumeration option
         * 
         * @return the contribution to the command line
         */
        String getCommandLIneDistribution();

        /**
         * Is this enumeration option set by default
         * Only one enumeration option can be set by default.
         * Therefore if multiple enumeration option values are set to default
         * only the first found will be treated as set by default
         * 
         * @return the set by default flag
         */
        boolean isDefault();

    }

    public String[] getBrowseFilterExtensions();

    public String getBrowseFilterPath();

    /**
     * @return If this option is defined as an enumeration, this function returns
     *         the list of possible values for that enum.
     *
     *         If this option is not defined as an enumeration, it returns
     *         <code>null</code>.
     */
    public String[] getEnumIDs();

    String getDefaultValue(IResource resource, ITool tool, IAutoBuildConfigurationDescription myAutoConfDesc);

    public String getEnumIDFromName(String enumOptionName);

    public Map<String, String> getCommandVars(String optionValue, IAutoBuildConfigurationDescription autoConfData);

}
