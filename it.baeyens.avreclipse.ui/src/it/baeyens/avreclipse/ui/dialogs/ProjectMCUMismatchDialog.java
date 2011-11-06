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
 * $Id: ProjectMCUMismatchDialog.java 851 2010-08-07 19:37:00Z innot $
 *     
 *******************************************************************************/
package it.baeyens.avreclipse.ui.dialogs;

import it.baeyens.avreclipse.core.toolinfo.fuses.FuseType;

import java.text.MessageFormat;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Shell;


/**
 * An small Warning dialog that will be shown when the MCU for the bytes does not match the current
 * Project / Configuration MCU.
 * <p>
 * In addition to a fixed warning message, this dialog sports two buttons to accept the byte values,
 * even if they don't match, or to cancel the changes.
 * </p>
 * <p>
 * The open method of this dialog will returns two values
 * <ul>
 * <li><code>0</code> Accept button pressed.</li>
 * <li><code>1</code> Cancel button pressed.</li>
 * </ul>
 * </p>
 * 
 * @author Thomas Holland
 * @since 2.3
 */
public class ProjectMCUMismatchDialog extends MessageDialog {

	public final static int	CONVERT	= 0;
	public final static int	ACCEPT	= 1;
	public final static int	CANCEL	= 2;

	/**
	 * Create a new Dialog.
	 * <p>
	 * The dialog will not be shown until the <code>open()</code> method has been called.
	 * </p>
	 * 
	 * @param shell
	 *            Parent <code>Shell</code>
	 * @param newmcu
	 *            The MCU id for the fuse bytes.
	 * @param projectmcu
	 *            The MCU id for the project or build configuration.
	 * @param type
	 *            The <code>FuseType</code> for which this dialog is shown.
	 * @param perconfig
	 *            If <code>true</code> then "build configuration" is used in this dialog,
	 *            "project" otherwise.
	 */
	public ProjectMCUMismatchDialog(Shell shell, String newmcu, String projectmcu, FuseType type,
			boolean perconfig) {

		super(shell, "AVRDude Warning", null, "", WARNING, new String[] { "Convert", "Accept",
				"Cancel" }, 0);

		String proptype = perconfig ? "build configuration" : "project";

		String source = "The loaded {3} values are valid for an {0} MCU.\n"
				+ "This MCU is not compatible with the current {2} MCU [{1}].\n\n"
				+ "\"Convert\" to try to convert the values to {1} {3} settings.\n"
				+ "\"Accept\" to accept the new values anyway (and convert them later).\n"
				+ "\"Cancel\" to discard the new values.";

		this.message = MessageFormat.format(source, newmcu, projectmcu, proptype, type.toString());
	}
}
