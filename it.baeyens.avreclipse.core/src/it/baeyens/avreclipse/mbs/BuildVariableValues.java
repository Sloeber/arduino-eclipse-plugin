/*******************************************************************************
 * 
 * Copyright (c) 2008, 2010 Thomas Holland (thomas@innot.de) and others
 * 
 * This program and the accompanying materials are made
 * available under the terms of the GNU Public License v3
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 * 
 * Contributors:
 *     Thomas Holland - initial API and implementation
 *     
 * $Id: BuildVariableValues.java 851 2010-08-07 19:37:00Z innot $
 *     
 *******************************************************************************/
package it.baeyens.avreclipse.mbs;

import it.baeyens.arduino.common.ArduinoConst;
import it.baeyens.arduino.common.ArduinoInstancePreferences;
import it.baeyens.arduino.common.Common;
import it.baeyens.avreclipse.core.paths.AVRPath;
import it.baeyens.avreclipse.core.paths.AVRPathProvider;
import it.baeyens.avreclipse.core.paths.IPathProvider;
import it.baeyens.avreclipse.core.properties.AVRProjectProperties;
import it.baeyens.avreclipse.core.properties.ProjectPropertyManager;

import java.io.File;
import java.util.List;

import org.eclipse.cdt.managedbuilder.core.IConfiguration;
import org.eclipse.cdt.managedbuilder.envvar.IBuildEnvironmentVariable;
import org.eclipse.core.resources.IProject;

/**
 * This <code>Enum</code> contains a list of all available variable names.
 * <p>
 * Each Variable knows how to extract its current value from an
 * {@link AVRProjectProperties} object, respectively from an
 * {@link IConfiguration}.
 * </p>
 * <p>
 * Currently these Environment Variables are handled:
 * <ul>
 * <li><code>$(AVRTARGETMCU)</code>: The target MCU id value as selected by the
 * user</li>
 * <li><code>$(AVRTARGETFCPU)</code>: The target MCU FCPU value as selected by
 * the user</li>
 * <li><code>$(AVRDUDEOPTIONS)</code>: The command line options for avrdude,
 * except for any action options (<em>-U</em> options)</li>
 * <li><code>$(AVRDUDEACTIONOPTIONS)</code>: The command line options for
 * avrdude to execute all actions requested by the user. (<em>-U</em> options)</li>
 * <li><code>$(BUILDARTIFACT)</code>: name of the target build artifact (the
 * .elf file)</li>
 * <li><code>$(PATH)</code>: The current path prepended with the paths to the
 * avr-gcc executable and the make executable. This, together with the selection
 * of the paths on the preference page, allows for multiple avr-gcc toolchains
 * on one computer</li>
 * <li><code>$(AVRDUDEPATH)</code>: The current path to the avrdude executable.</li>
 * <li><code>$(ARDUINOBOARDNAME)</code>: The current used Arduino board type.
 * (Arduino plugin Only)</li>
 * </ul>
 * </p>
 * 
 * @author Thomas Holland
 * @since 2.2
 * @since 2.3 Added AVRDUDEPATH variable (fix for Bug 2136888)
 */
public enum BuildVariableValues
{

	AVRTARGETMCU()
		{
			@Override
			public String getValue(IConfiguration buildcfg)
				{
					AVRProjectProperties props = getPropsFromConfig(buildcfg);
					if (props == null)
						return "";
					String targetmcu = props.getMCUId();
					return targetmcu;
				}
		},

	AVRTARGETFCPU()
		{
			@Override
			public String getValue(IConfiguration buildcfg)
				{
					AVRProjectProperties props = getPropsFromConfig(buildcfg);
					if (props == null)
						return "";
					String fcpu = props.getFCPU();
					return fcpu;
				}
		},

	AVRDUDEOPTIONS()
		{
			@Override
			public String getValue(IConfiguration buildcfg)
				{
					AVRProjectProperties props = getPropsFromConfig(buildcfg);
					if (props == null)
						return "";
					List<String> avrdudeoptions = props.getAVRDudeProperties().getArguments();
					StringBuilder sb = new StringBuilder();
					for (String option : avrdudeoptions)
						{
							sb.append(option + " ");
						}
					return sb.toString();
				}
		},

	AVRDUDEACTIONOPTIONS()
		{
			@Override
			public String getValue(IConfiguration buildcfg)
				{
					AVRProjectProperties props = getPropsFromConfig(buildcfg);
					if (props == null)
						return "";
					List<String> avrdudeoptions = props.getAVRDudeProperties().getActionArguments(buildcfg);
					StringBuilder sb = new StringBuilder();
					for (String option : avrdudeoptions)
						{
							sb.append(option + " ");
						}
					return sb.toString();
				}
		},

	BUILDARTIFACT()
		{
			// This is only defined to export the BuildArtifact Build Macro as an
			// environment variable in case some makefile requires the path to the
			// .elf target file.
			@Override
			public String getValue(IConfiguration buildcfg)
				{
					String artifact = buildcfg.getArtifactName() + "." + buildcfg.getArtifactExtension();
					return artifact;
				}

			@Override
			public boolean isMacro()
				{
					// BUILDARTIFACT is not needed as a build macro, because CDT already
					// has a macro with this name.
					return false;
				}
		},

	PATH()
		{
			@Override
			public String getValue(IConfiguration buildcfg)
				{
					// Get the paths to "avr-gcc" and "make" from the PathProvider
					// and return the paths, separated with a System specific path
					// separator.
					// The path to the avrdude executable is handled as a separate
					// variable because at least with WinAVR avr-gcc and avrdude are
					// in the same directory and adding the path to avrdude to the
					// global path would have no effect as the avrdude executable
					// from the gccpath would be used anyway.

					StringBuilder paths = new StringBuilder();

					IPathProvider gccpathprovider = new AVRPathProvider(AVRPath.AVRGCC);
					String gccpath = gccpathprovider.getPath().toOSString();
					if (gccpath != null && !("".equals(gccpath)))
						{
							paths.append(gccpath);
							paths.append(PATH_SEPARATOR);
						}

					IPathProvider makepathprovider = new AVRPathProvider(AVRPath.MAKE);
					String makepath = makepathprovider.getPath().toOSString();
					if (makepath != null && !("".equals(makepath)))
						{
							paths.append(makepath);
							paths.append(PATH_SEPARATOR);
						}

					return paths.toString();
				}

			@Override
			public int getOperation()
				{
					// Prepend our paths to the System paths
					return IBuildEnvironmentVariable.ENVVAR_PREPEND;
				}

			@Override
			public boolean isMacro()
				{
					// PATH not supported as a BuildMacro
					return false;
				}

		},

	AVRDUDEPATH()
		{
			@Override
			public String getValue(IConfiguration buildcfg)
				{
					IPathProvider avrdudepathprovider = new AVRPathProvider(AVRPath.AVRDUDE);
					String avrdudepath = avrdudepathprovider.getPath().toOSString();
					if (avrdudepath != null && !("".equals(avrdudepath)))
						{

							return avrdudepath + File.separator;
						}
					return "";
				}

			// Jan Baeyens added the definitions below to create environment variables
			// containing the Arduino board name
		},

	ARDUINOBOARDNAME()
		{
			@Override
			public String getValue(IConfiguration buildcfg)
				{

					String sRet = Common.getPersistentProperty((IProject) buildcfg.getOwner(), ArduinoConst.KEY_ARDUINOBOARD);
					return Common.MakeNameCompileSafe(sRet);
				}

		},

	ARDUINOBOARDVARIANT()
		{
			@Override
			public String getValue(IConfiguration buildcfg)
				{
					return Common.getPersistentProperty((IProject) buildcfg.getOwner(), ArduinoConst.KEY_ARDUINOBOARDVARIANT);
				}

		},
	ARDUINO_IDE_VERSION()
		{
			@Override
			public String getValue(IConfiguration buildcfg)
				{
					return ArduinoInstancePreferences.GetARDUINODefineValue();
				}
		},
	ARDUINO_CORE_FOLDER()
		{
			@Override
			public String getValue(IConfiguration buildcfg)
				{
					return Common.getPersistentProperty((IProject) buildcfg.getOwner(), ArduinoConst.KEY_ARDUINO_CORE_FOLDER);
				}
		},
	ARDUINO_CPP_COMPILE_OPTIONS()
		{
			@Override
			public String getValue(IConfiguration buildcfg)
				{
					return Common.getPersistentProperty((IProject) buildcfg.getOwner(), ArduinoConst.KEY_ARDUINO_CPP_COMPILE_OPTIONS);
				}
		},
	ARDUINO_C_COMPILE_OPTIONS()
		{
			@Override
			public String getValue(IConfiguration buildcfg)
				{
					return Common.getPersistentProperty((IProject) buildcfg.getOwner(), ArduinoConst.KEY_ARDUINO_C_COMPILE_OPTIONS);
				}
		},
	ARDUINO_LINK_OPTIONS()
		{
			@Override
			public String getValue(IConfiguration buildcfg)
				{
					return Common.getPersistentProperty((IProject) buildcfg.getOwner(), ArduinoConst.KEY_ARDUINO_LINK_OPTIONS);
				}
		},
	ARDUINO_PID_VALUE()
		{
			@Override
			public String getValue(IConfiguration buildcfg)
				{
					return Common.getPersistentProperty((IProject) buildcfg.getOwner(), ArduinoConst.KEY_ARDUINO_BUILD_PID);
				}
		},
	ARDUINO_VID_VALUE()
		{
			@Override
			public String getValue(IConfiguration buildcfg)
				{
					return Common.getPersistentProperty((IProject) buildcfg.getOwner(), ArduinoConst.KEY_ARDUINO_BUILD_VID);
				}
		};

	/** System default Path Separator. On Windows ";", on Posix ":" */
	private final static String PATH_SEPARATOR = System.getProperty("path.separator");

	/**
	 * Get the current variable value for the given Configuration
	 * 
	 * @param buildcfg
	 *          <code>IConfiguration</code> for which to get the variable value.
	 * @return <code>String</code> with the current value of the variable.
	 */
	public abstract String getValue(IConfiguration buildcfg);

	/**
	 * @return <code>true</code> if this variable is supported as a build macro.
	 */
	public boolean isMacro()
		{
			// This method is overridden in some Enum values
			return true;
		}

	/**
	 * @return <code>true</code> if this variable is supported as an environment
	 *         variable.
	 */
	public boolean isVariable()
		{
			// This method could be overridden in some Enum values.
			return true;
		}

	/**
	 * Get the Operation code for environment variables.
	 * <p>
	 * Most Variables will return {@link IBuildEnvironmentVariable#ENVVAR_REPLACE}
	 * . However the <code>PATH</code> environment variable will return
	 * {@link IBuildEnvironmentVariable#ENVVAR_PREPEND}.
	 * </p>
	 * 
	 * @see IBuildEnvironmentVariable#getOperation()
	 * 
	 * @return <code>int</code> with the operation code.
	 */
	public int getOperation()
		{
			// Default is REPLACE.
			// The PATH Variable, which requires ENVVAR_PREPEND, will override this
			// method.
			return IBuildEnvironmentVariable.ENVVAR_REPLACE;
		}

	/**
	 * Get the AVR Project properties for the given Configuration.
	 * 
	 * @param buildcfg
	 *          <code>IConfiguration</code> for which to get the properties.
	 * @return
	 */
	private static AVRProjectProperties getPropsFromConfig(IConfiguration buildcfg)
		{
			ProjectPropertyManager manager = ProjectPropertyManager.getPropertyManager((IProject) buildcfg.getOwner());
			AVRProjectProperties props = manager.getConfigurationProperties(buildcfg);
			return props;
		}

}
