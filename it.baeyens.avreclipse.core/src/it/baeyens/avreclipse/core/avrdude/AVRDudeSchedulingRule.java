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
 * $Id: AVRDudeSchedulingRule.java 851 2010-08-07 19:37:00Z innot $
 *     
 *******************************************************************************/
package it.baeyens.avreclipse.core.avrdude;

import org.eclipse.core.runtime.jobs.ISchedulingRule;

/**
 * A simple SchedulingRule to prevent avrdude from being started multiple times (which would
 * probably result in a PORT_BLOCKED Exception).
 * <p>
 * Instances of this Rule can should be added to all Jobs that run avrdude and will cause actual
 * access to a programmer.
 * </p>
 * <p>
 * The rule will try to determine conflicts by comparing the ProgrammerConfig of this Rule with that
 * of an conflicting rule. If there is a chance that both configs use the same port, this rule will
 * report a conflict.
 * </p>
 * 
 * @author Thomas Holland
 * @since 2.2
 * 
 */
public class AVRDudeSchedulingRule implements ISchedulingRule {

	/** The Config to determine which port avrdude is currently running on */
	private final ProgrammerConfig	fProgrammerConfig;

	/**
	 * Creates a new SchedulingRule for the given ProgrammerConfig.
	 * 
	 * @param config
	 *            <code>ProgrammerConfig</code>
	 */
	public AVRDudeSchedulingRule(ProgrammerConfig config) {
		fProgrammerConfig = config;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.core.runtime.jobs.ISchedulingRule#contains(org.eclipse.core.runtime.jobs.ISchedulingRule)
	 */
	public boolean contains(ISchedulingRule rule) {
		if (rule == this) {
			return true;
		}
		// Don't need any nesting
		return false;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.core.runtime.jobs.ISchedulingRule#isConflicting(org.eclipse.core.runtime.jobs.ISchedulingRule)
	 */
	public boolean isConflicting(ISchedulingRule rule) {
		if (!(rule instanceof AVRDudeSchedulingRule))
			// Don't care about other Rules
			return false;

		// But conflict with ourself
		if (rule == this)
			return true;

		AVRDudeSchedulingRule testrule = (AVRDudeSchedulingRule) rule;
		ProgrammerConfig testcfg = testrule.fProgrammerConfig;

		// if either config is null we have no conflict (because the call to avrdude will fail
		// anyway)
		if (fProgrammerConfig == null || testcfg == null) {
			return false;
		}

		// We only have no conflict for sure, if the Programmers are the same...
		if (fProgrammerConfig.getProgrammer().equals(testcfg.getProgrammer())) {
			// ... but on different, non-empty ports
			String myport = fProgrammerConfig.getPort();
			String testport = testcfg.getPort();
			if (myport.length() > 0 && testport.length() > 0 && !myport.equals(testport)) {
				return false;
			}
		}

		return true;

		// Even if the Programmers are different, they may still use the same
		// port.
		// And because they might use default ports (not set in the config), we
		// can't really tell if they work on the same port.
		// And we can't ask avrdude about the port, because it is already
		// running :-(
		// Maybe at some point we will have the ProgrammerConfig call avrdude to
		// get the real port. But this is currently not worth the effort for
		// just a stupid SchedulingRule. The worst thing is that the user has to
		// wait for a short moment longer after he has started multiple parallel
		// uploads.

	}
}
