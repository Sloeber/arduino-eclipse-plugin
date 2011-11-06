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
 * $Id: SizeFormatApplicabilityCalculator.java 851 2010-08-07 19:37:00Z innot $
 *     
 *******************************************************************************/
package it.baeyens.avreclipse.mbs;

import org.eclipse.cdt.managedbuilder.core.IBuildObject;
import org.eclipse.cdt.managedbuilder.core.IHoldsOptions;
import org.eclipse.cdt.managedbuilder.core.IOption;
import org.eclipse.cdt.managedbuilder.core.IOptionApplicability;

import it.baeyens.avreclipse.core.toolinfo.Size;

/**
 * Calculate which size format options are applicable.
 * 
 * The Size Tool has two options for the format. One with and one without the
 * --format=avr option.
 * 
 * Depending on the actual size tool only one option is used, the other is
 * disabled and hidden.
 * 
 * @author Thomas Holland
 * @version 1.0
 * @since 2.1
 * 
 */
public class SizeFormatApplicabilityCalculator implements IOptionApplicability {

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.cdt.managedbuilder.core.IOptionApplicability#isOptionEnabled(org.eclipse.cdt.managedbuilder.core.IBuildObject,
	 *      org.eclipse.cdt.managedbuilder.core.IHoldsOptions,
	 *      org.eclipse.cdt.managedbuilder.core.IOption)
	 */
	public boolean isOptionEnabled(IBuildObject configuration, IHoldsOptions holder, IOption option) {
		return hasAVR(option) == Size.getDefault().hasAVROption();
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
		return hasAVR(option) == Size.getDefault().hasAVROption();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.cdt.managedbuilder.core.IOptionApplicability#isOptionVisible(org.eclipse.cdt.managedbuilder.core.IBuildObject,
	 *      org.eclipse.cdt.managedbuilder.core.IHoldsOptions,
	 *      org.eclipse.cdt.managedbuilder.core.IOption)
	 */
	public boolean isOptionVisible(IBuildObject configuration, IHoldsOptions holder, IOption option) {
//		boolean cond1 = hasAVR(option);
//		boolean cond2 = Size.getDefault().hasAVROption();
//		boolean cond3 = (cond1 == cond2);
		return hasAVR(option) == Size.getDefault().hasAVROption();
	}

	private boolean hasAVR(IOption option) {
		// Test if the id of the option ends with "withavr"
		if (option.getId().contains("withavr")) {
			return true;
		}
		return false;
	}
}

