/*******************************************************************************
 * 
 * Copyright (c) 2007, 2010 Thomas Holland (thomas@innot.de) and others
 * 
 * This program and the accompanying materials are made
 * available under the terms of the GNU Public License v3
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 * 
 * Contributors:
 *     Thomas Holland - initial API and implementation
 *     
 * $Id: SizeOptionsApplicabilityCalculator.java 851 2010-08-07 19:37:00Z innot $
 *     
 *******************************************************************************/
package it.baeyens.avreclipse.mbs;

import org.eclipse.cdt.managedbuilder.core.BuildException;
import org.eclipse.cdt.managedbuilder.core.IBuildObject;
import org.eclipse.cdt.managedbuilder.core.IHoldsOptions;
import org.eclipse.cdt.managedbuilder.core.IOption;
import org.eclipse.cdt.managedbuilder.core.IOptionApplicability;

import it.baeyens.avreclipse.PluginIDs;
import it.baeyens.avreclipse.core.toolinfo.Size;

/**
 * Calculate which size options are applicable.
 * 
 * Depending on the selected format some options are enabled / disabled
 * <ul>
 *   <li>The -t option is only applicable for the berkeley format.</li>
 *   <li>The -x option is not applicable for the avr format.</li>
 *   <li>The -mcu option is only applicable for the avr format</li>
 * </ul>
 * 
 * @author Thomas Holland
 * @version 1.0
 * @since 2.1
 * 
 */
public class SizeOptionsApplicabilityCalculator implements IOptionApplicability {

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.cdt.managedbuilder.core.IOptionApplicability#isOptionEnabled(org.eclipse.cdt.managedbuilder.core.IBuildObject,
	 *      org.eclipse.cdt.managedbuilder.core.IHoldsOptions,
	 *      org.eclipse.cdt.managedbuilder.core.IOption)
	 */
	public boolean isOptionEnabled(IBuildObject configuration, IHoldsOptions holder, IOption option) {
		// the totals option is only enabled when the berkeley format
		// is selected.
		if (option.getId().contains("totals")) {
			if (berkeleySelected(option, holder)) {
				return true;
			}
		}

		// the hex option is only enabled when the avr format 
		// is not selected.
		if (option.getId().contains("hex")) {
			if (!avrSelected(option, holder)) {
				return true;
			}
		}

		// Don't need to handle the targetmcu option, as it is
		// invisible anyways.
		return false;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.cdt.managedbuilder.core.IOptionApplicability#isOptionUsedInCommandLine(org.eclipse.cdt.managedbuilder.core.IBuildObject,
	 *      org.eclipse.cdt.managedbuilder.core.IHoldsOptions,
	 *      org.eclipse.cdt.managedbuilder.core.IOption)
	 */
	public boolean isOptionUsedInCommandLine(IBuildObject configuration, IHoldsOptions holder,
	        IOption option) {
		// the totals option is used on the command
		// line if berkeley is selected
		if (option.getId().contains("totals")) {
			return berkeleySelected(option, holder);
		}

		// the hex option is used on the command
		// line if avr format is not selected 
		if (option.getId().contains("hex")) {
			return !avrSelected(option, holder);
		}

		// The Target MCU option is used on the command
		// line if avr format is selected
		if (option.getId().contains("targetmcu")) {
			return avrSelected(option, holder);
		}

		return true;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.cdt.managedbuilder.core.IOptionApplicability#isOptionVisible(org.eclipse.cdt.managedbuilder.core.IBuildObject,
	 *      org.eclipse.cdt.managedbuilder.core.IHoldsOptions,
	 *      org.eclipse.cdt.managedbuilder.core.IOption)
	 */
	public boolean isOptionVisible(IBuildObject configuration, IHoldsOptions holder, IOption option) {
		// all options are visible
		// the targetmcu option is invisible by default, as it has no category
		return true;
	}

	private boolean berkeleySelected(IOption option, IHoldsOptions holder) {
		boolean hasavr = Size.getDefault().hasAVROption();
		IOption formatoption = null;
		if (hasavr) {
			formatoption = holder
			        .getOptionBySuperClassId(PluginIDs.PLUGIN_TOOLCHAIN_TOOL_SIZE_FORMATWITHAVR);
		} else {
			formatoption = holder
			        .getOptionBySuperClassId(PluginIDs.PLUGIN_TOOLCHAIN_TOOL_SIZE_FORMAT);
		}
		try {
			if (formatoption.getSelectedEnum().endsWith("berkeley")) {
				return true;
			}
		} catch (BuildException e) {
			// should not happen
			e.printStackTrace();
		}
		return false;
	}

	private boolean avrSelected(IOption option, IHoldsOptions holder) {
		boolean hasavr = Size.getDefault().hasAVROption();
		IOption formatoption = null;
		if (hasavr) {
			formatoption = holder
			        .getOptionBySuperClassId(PluginIDs.PLUGIN_TOOLCHAIN_TOOL_SIZE_FORMATWITHAVR);
		} else {
			return false;
		}
		try {
			if (formatoption.getSelectedEnum().endsWith("avr")) {
				return true;
			}
		} catch (BuildException e) {
			// should not happen
			e.printStackTrace();
		}
		return false;
	}
}



