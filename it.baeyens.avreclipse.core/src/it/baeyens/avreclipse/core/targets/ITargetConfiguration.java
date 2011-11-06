/*******************************************************************************
 * 
 * Copyright (c) 2009, 2010 Thomas Holland (thomas@innot.de) and others
 * 
 * This program and the accompanying materials are made
 * available under the terms of the GNU Public License v3
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 * 
 * Contributors:
 *     Thomas Holland - initial API and implementation
 *     
 * $Id: ITargetConfiguration.java 851 2010-08-07 19:37:00Z innot $
 *     
 *******************************************************************************/

package it.baeyens.avreclipse.core.targets;

import java.util.Map;
import java.util.Set;

import org.eclipse.core.runtime.CoreException;

/**
 * The Hardware Configuration API.
 * <p>
 * A hardware configuration is basically a bag of attribute - value pairs that describe all
 * properties required to access a remote MCU.
 * </p>
 * <p>
 * Each hardware configuration has a unique id and information on:
 * <ul>
 * <li>The name of the configuration + an optional description.</li>
 * <li>The target MCU type and its clock frequency.</li>
 * <li>An ID for the programmer hardware used to access the MCU, including its host and target
 * interface settings.</li>
 * <li>The ID of the programmer tool for uploading AVR applications to the target MCU.</li>
 * <li>The ID of the gdbserver tool for debugging AVR applications.</li>
 * </ul>
 * The attributes common for all target configurations are defined in {@link ITargetConfigConstants}
 * . The programmer tool and the gdbserver use their own custom attributes.
 * </p>
 * 
 * <p>
 * All hardware configurations are managed by the {@link TargetConfigurationManager}. It has methods
 * to
 * <ul>
 * <li>create new configs {@link TargetConfigurationManager#createNewConfig()}</li>
 * <li>get existing configs {@link TargetConfigurationManager#getConfig(String)}
 * <li>delete configs {@link TargetConfigurationManager#deleteConfig(String)}</li>
 * </ul>
 * </p>
 * <p>
 * This interface in not supposed to be implemented by clients.
 * </p>
 * 
 * @noimplement
 * 
 * @author Thomas Holland
 * @since 2.4
 * 
 */
public interface ITargetConfiguration {

	/**
	 * Get the Id of this target configuration.
	 * 
	 * @return
	 */
	public String getId();

	/**
	 * Get the name of this target configuration.
	 * 
	 * @return
	 * @throws CoreException
	 */
	public String getName();

	/**
	 * Get the optional user supplied description of this target configuration.
	 * 
	 * @return
	 */
	public String getDescription();

	/**
	 * @return the id of the target MCU
	 */
	public String getMCU();

	/**
	 * @return the target MCU clock
	 */
	public int getFCPU();

	/**
	 * @return the current programmer tool
	 */
	public IProgrammerTool getProgrammerTool();

	/**
	 * @return the current gdb server tool
	 */
	public IGDBServerTool getGDBServerTool();

	/**
	 * Get the list of all supported MCUs.
	 * <p>
	 * If the <code>filtered</code> argument is <code>true</code>, then only those MCUs are returned
	 * that are supported by both the programmer tool and the gdbserver (Intersection). If
	 * <code>filtered</code> is <code>false</code>, then all MCUs supported by avr-gcc as well as
	 * the selected programmer tool and gdbserver are returned (Union).
	 * </p>
	 * 
	 * @param filtered
	 *            Restrict the list to the MCUs that are actually supported by the current
	 *            configuration.
	 * @return Set of mcu id values in avr-gcc format
	 */
	public Set<String> getSupportedMCUs(boolean filtered);

	/**
	 * Get the set of IDs for all supported Programmers.
	 * <p>
	 * Depending on the <code>supported</code> flag the returned set will have either the union or
	 * the intersection of the programmer sets from the programmer tool and the gdbserver tool.
	 * </p>
	 * 
	 * @param supported
	 *            If <code>true</code> then only the Programmers supported by the current
	 *            configuration are returned. If <code>false</code> then all Programmers are
	 * @return Set of <code>IProgrammers</code>
	 */
	public Set<String> getAllProgrammers(boolean supported);

	/**
	 * Get a specific programmer.
	 * <p>
	 * All target configuration tools (programmer tools and gdbservers) are queried for the given
	 * id. Note that even tools not currently active are queried. This is used by the user interface
	 * to correctly show the information for an id that had been selected but has become invalid
	 * afterwards.
	 * </p>
	 * </p>
	 * 
	 * @param programmerid
	 *            The id value of a specific programmer.
	 * @return An <code>IProgrammer</code> object
	 */
	public IProgrammer getProgrammer(String programmerid);

	/**
	 * Checks if this target configuration is capable of debugging.
	 * <p>
	 * Debugging can be either on-chip or with a simulator. If <code>true</code> is returned, then
	 * the {@link #getGDBServerLaunchConfig()} will return the launch configuration for a GDB
	 * Server.
	 * </p>
	 * 
	 * @return <code>true</code> if this target configuration is capable of debugging.
	 */
	public boolean isDebugCapable();

	/**
	 * Checks if this target configuration is capable of uploading an AVR project to a target.
	 * <p>
	 * If <code>true</code> is returned, then the {@link #getLoaderLaunchConfig()} will return the
	 * launch configuration for a project up-loader.
	 * </p>
	 * 
	 * @return <code>true</code> if target configuration contains an image loader.
	 */
	public boolean isImageLoaderCapable();

	/**
	 * Returns the string-valued attribute with the given name.
	 * <p>
	 * Returns the given default value if the attribute is undefined.
	 * </p>
	 * 
	 * @param attributeName
	 *            the name of the attribute
	 * @return the value or the default value if no value was found.
	 */
	public String getAttribute(String attributeName);

	/**
	 * Returns the boolean-valued attribute with the given name.
	 * <p>
	 * Returns the given default value if the attribute is undefined or if the value was not a
	 * boolean.
	 * </p>
	 * <p>
	 * This method is not type save, i.e. there is no checking whether the attribute is actually an
	 * boolean. It is up to the caller to ensure that the attribute only contains a boolean value.
	 * </p>
	 * 
	 * @param attributeName
	 *            the name of the attribute
	 * @return the value or the default value if no valid value was found.
	 */
	public boolean getBooleanAttribute(String attributeName);

	/**
	 * Returns the integer-valued attribute with the given name.
	 * <p>
	 * Returns the given default value if the attribute is undefined or if the value was not a
	 * integer.
	 * </p>
	 * <p>
	 * This method is not type save, i.e. there is no checking whether the attribute is actually an
	 * integer. It is up to the caller to ensure that the attribute only contains integer values.
	 * </p>
	 * 
	 * @param attributeName
	 *            the name of the attribute
	 * @return the value or the default value if no valid value was found.
	 */
	public int getIntegerAttribute(String attributeName);

	/**
	 * Returns a map containing the attributes in this target configuration. Returns an empty map if
	 * this configuration has no attributes.
	 * <p>
	 * Modifying the map does not affect this target configuration's attributes. A target
	 * configuration is modified by obtaining a working copy of that target configuration, modifying
	 * the working copy, and then saving the working copy.
	 * </p>
	 * 
	 * @return a map of attribute keys and values
	 * @exception CoreException
	 *                unable to generate/retrieve an attribute map
	 * @since 2.1
	 */
	public Map<String, String> getAttributes() throws CoreException;

	/**
	 * Adds a property change listener to this target configuration. Has no affect if the identical
	 * listener is already registered.
	 * 
	 * @param listener
	 *            a property change listener
	 */
	public void addPropertyChangeListener(ITargetConfigChangeListener listener);

	/**
	 * Removes the given listener from this target configuration. Has no affect if the listener is
	 * not registered.
	 * 
	 * @param listener
	 *            a property change listener
	 */
	public void removePropertyChangeListener(ITargetConfigChangeListener listener);

	public ValidationResult validateAttribute(String attr);

	public enum Result {
		OK, WARNING, ERROR, UNKNOWN_ATTRIBUTE;
	}

	public class ValidationResult {

		public final static ValidationResult	OK_RESULT	= new ValidationResult(Result.OK, "");

		public final Result						result;
		public final String						description;

		public ValidationResult(Result result, String description) {
			this.result = result;
			this.description = description;
		}
	}
}
