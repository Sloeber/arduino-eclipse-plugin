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
 * $Id: BundlePathHelper.java 851 2010-08-07 19:37:00Z innot $
 *     
 *******************************************************************************/
package it.baeyens.avreclipse.core.paths;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;

/**
 * Convenience class to get the path for a given resource from a Eclipse bundle.
 * 
 * @author Thomas Holland
 * @since 2.1
 */
final class BundlePathHelper {

	/**
	 * @param path
	 *            AVRPath for the path
	 * @param bundeid
	 *            Id of the Bundle from which to get the path
	 * @return IPath with the path
	 */
	public static IPath getPath(AVRPath path, String bundeid) {

		// TODO: not implemented yet
		return new Path("");
	}

}
