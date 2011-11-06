/*******************************************************************************
 * 
 * Copyright (c) 2007, 2010 Thomas Holland (thomas@innot.de)
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 * 
 * Contributors:
 *     Thomas Holland - initial API and implementation
 *     
 *******************************************************************************/
package it.baeyens.avreclipse;

import it.baeyens.arduino.globals.ArduinoConst;

/**
 * Definitions of id values used in the plugin.xml
 * 
 * Some id's from the plugin.xml are used quite frequently to programmatically access some parts of the toolchain.
 * They are defined here in one central place to aid refactoring of the plugin.xml
 * 
 * @author Thomas Holland
 * @since 2.0
 *
 */
public interface PluginIDs {

	/** 
	 * ID of the base toolchain, all other toolchains are derived from this.
	 * Value: {@value}
	 */
	public final static String PLUGIN_BASE_TOOLCHAIN = "it.baeyens.avreclipse.toolchain.winavr.base";
	
	/** ID of the mcu type option of the base toolchain. Value: {@value} */
	public final static String PLUGIN_TOOLCHAIN_OPTION_MCU = "it.baeyens.avreclipse.toolchain.options.target.mcutype";
	
	/** ID of the cpu frequency option of the base toolchain. Value: {@value} */
	public final static String PLUGIN_TOOLCHAIN_OPTION_FCPU = "it.baeyens.avreclipse.toolchain.options.target.fcpu";
	
	/** ID of the "generate Flash" toolchain option. Value: {@value} */
	public final static String PLUGIN_TOOLCHAIN_OPTION_GENERATEFLASH = "it.baeyens.avreclipse.toolchain.options.toolchain.objcopy.flash";
	
	/** ID of the "generate EEPROM" toolchain option. Value: {@value} */
	public final static String PLUGIN_TOOLCHAIN_OPTION_GENERATEEEPROM = "it.baeyens.avreclipse.toolchain.options.toolchain.objcopy.eeprom";
	
	/** ID of the "avrdude" toolchain option. Value: {@value} */
	public final static String PLUGIN_TOOLCHAIN_OPTION_AVRDUDE = "it.baeyens.avreclipse.toolchain.options.toolchain.objcopy.eeprom";

	/** ID of the compiler tool. Value: {@value} */
	public final static String PLUGIN_TOOLCHAIN_TOOL_COMPILER = "it.baeyens.avreclipse.tool.compiler.winavr";

	/** ID of the linker tool. Value: {@value} */
	public final static String PLUGIN_TOOLCHAIN_TOOL_LINKER = "it.baeyens.avreclipse.tool.linker.winavr";

	/** ID of the flash objcopy tools. Value: {@value} */
	public final static String PLUGIN_TOOLCHAIN_TOOL_FLASH = "it.baeyens.avreclipse.tool.objcopy.flash.winavr";

	/** ID of the eeprom objcopy tools. Value: {@value} */
	public final static String PLUGIN_TOOLCHAIN_TOOL_EEPROM = "it.baeyens.avreclipse.tool.objcopy.eeprom.winavr";

	/** ID of the size tool. Value: {@value} */
	public final static String PLUGIN_TOOLCHAIN_TOOL_SIZE = "it.baeyens.avreclipse.tool.size.winavr";

	/** ID of the size tool format option with avr. Value: {@value} */
	public final static String PLUGIN_TOOLCHAIN_TOOL_SIZE_FORMATWITHAVR = "it.baeyens.avreclipse.size.option.formatwithavr";
	
	/** ID of the size tool format option without avr. Value: {@value} */
	public final static String PLUGIN_TOOLCHAIN_TOOL_SIZE_FORMAT = "it.baeyens.avreclipse.size.option.format";
	
	/** ID of the AVR Nature. Value: {@value} */
	public final static String NATURE_ID =  ArduinoConst.AVRnatureid;//"it.baeyens.avreclipse.core.avrnature";
}
