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
 * $Id: FileMCUMismatchDialog.java 851 2010-08-07 19:37:00Z innot $
 *     
 *******************************************************************************/
package it.baeyens.avreclipse.ui.dialogs;

import it.baeyens.avreclipse.core.toolinfo.fuses.FuseType;

import java.text.MessageFormat;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Shell;


/**
 * An small Warning dialog that will be shown when the MCU for the bytes does not match the current
 * MCU.
 * <p>
 * In addition to a fixed warning message, this dialog sports three buttons to:
 * <ul>
 * <li>Convert the values to the current MCU</li>
 * <li>Change the current MCU to the new MCU</li>
 * <li>Cancel</li>
 * </ul>
 * </p>
 * <p>
 * The open method of this dialog returns one of three values
 * <ul>
 * <li><code>CONVERT</code> Convert button pressed.</li>
 * <li><code>CHANGE</code> Change button pressed.</li>
 * <li><code>CANCEL</code> Cancel button pressed.</li>
 * </ul>
 * </p>
 * 
 * @author Thomas Holland
 * @since 2.3
 */
public class FileMCUMismatchDialog extends MessageDialog {

	public final static int	CONVERT	= 0;
	public final static int	CHANGE	= 1;
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
	 *            The MCU id loaded from the device.
	 * @param projectmcu
	 *            The MCU id of the file.
	 * @param type
	 *            The <code>FuseType</code> for which this dialog is shown.
	 */
	public FileMCUMismatchDialog(Shell shell, String newmcu, String projectmcu, FuseType type) {

		super(shell, "Incompatible MCU warning", null, "", WARNING, new String[] { "Convert",
				"Change", "Cancel" }, 0);

		String source = "The new {2} values are for an {0} MCU.\n"
				+ "This MCU is not compatible with the current MCU [{1}].\n\n"
				+ "\"Convert\" to convert the values to the current MCU [{1}].\n"
				+ "\"Change\" to change the current MCU to the new MCU [{0}].\n"
				+ "\"Cancel\" to discard the new values.";

		this.message = MessageFormat.format(source, newmcu, projectmcu, type.toString());
	}
}
