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
 * $Id: ActionType.java 851 2010-08-07 19:37:00Z innot $
 *     
 *******************************************************************************/
package it.baeyens.avreclipse.ui.actions;

import it.baeyens.avreclipse.ui.AVRUIPlugin;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.resource.ImageDescriptor;


/**
 * Convenience Enum to collect the user interface elements of all ByteValues actions in one class.
 * <p>
 * This enum knows all Actions applicable to ByteValues and has the Text, ToolTipText,
 * ImageDescriptor and DisabledImageDescriptor for them.
 * </p>
 * <p>
 * The {@link #setupAction(IAction)} method will take an exising method and sets the above mentioned
 * four properties accordingly.<br>
 * Example usage:
 * 
 * <pre>
 * 		IAction action = ...
 * 		ActionType type = ActionType.xxxxx;
 * 		type.setupAction(action);
 * </pre>
 * 
 * @author Thomas Holland
 * @since 2.3
 * 
 */
public enum ActionType {

	/** Change the MCU Action. */
	CHANGE_MCU() {
		@Override
		public String getText() {
			return "Change";
		}

		@Override
		public String getToolTipText() {
			return "Change the current MCU";
		}

		@Override
		public ImageDescriptor getImage() {
			return AVRUIPlugin.getImageDescriptor("icons/objs16/e_change_mcu.png");
		}

		@Override
		public ImageDescriptor getDisabledImage() {
			return AVRUIPlugin.getImageDescriptor("icons/objs16/d_change_mcu.png");
		}
	},

	/** Open a ByteValues editor Action. */
	EDIT() {
		@Override
		public String getText() {
			return "Edit";
		}

		@Override
		public String getToolTipText() {
			return "Start editor";
		}

		@Override
		public ImageDescriptor getImage() {
			return AVRUIPlugin.getImageDescriptor("icons/objs16/e_edit_fuses.png");
		}

		@Override
		public ImageDescriptor getDisabledImage() {
			return AVRUIPlugin.getImageDescriptor("icons/objs16/d_edit_fuses.png");
		}
	},

	/** Copy from file Action. */
	COPY() {
		@Override
		public String getText() {
			return "Copy";
		}

		@Override
		public String getToolTipText() {
			return "Copy from file";
		}

		@Override
		public ImageDescriptor getImage() {
			return AVRUIPlugin.getImageDescriptor("icons/objs16/e_copy_fusefile.png");
		}

		@Override
		public ImageDescriptor getDisabledImage() {
			return AVRUIPlugin.getImageDescriptor("icons/objs16/d_copy_fusefile.png");
		}

	},

	/** Read from attached MCU Action */
	READ() {
		@Override
		public String getText() {
			return "Read";
		}

		@Override
		public String getToolTipText() {
			return "Read values from attached MCU";
		}

		@Override
		public ImageDescriptor getImage() {
			return AVRUIPlugin.getImageDescriptor("icons/objs16/e_read_mcu.png");
		}

		@Override
		public ImageDescriptor getDisabledImage() {
			return AVRUIPlugin.getImageDescriptor("icons/objs16/d_read_mcu.png");
		}

	},

	/** Set to default values Action. */
	DEFAULTS() {
		@Override
		public String getText() {
			return "Defaults";
		}

		@Override
		public String getToolTipText() {
			return "Set to default (if available)";
		}

		@Override
		public ImageDescriptor getImage() {
			return AVRUIPlugin.getImageDescriptor("icons/objs16/e_copy_default.png");
		}

		@Override
		public ImageDescriptor getDisabledImage() {
			return AVRUIPlugin.getImageDescriptor("icons/objs16/d_copy_default.png");
		}

	},

	/** Set all bytes to <code>0xff</code> Action. */
	ALL_1() {
		@Override
		public String getText() {
			return "All 1s";
		}

		@Override
		public String getToolTipText() {
			return "Set all bits to 1";
		}

		@Override
		public ImageDescriptor getImage() {
			return AVRUIPlugin.getImageDescriptor("icons/objs16/e_0xff.png");
		}

		@Override
		public ImageDescriptor getDisabledImage() {
			return AVRUIPlugin.getImageDescriptor("icons/objs16/d_0xff.png");
		}

	},

	/** Set all bytes to <code>0x00</code> Action. */
	ALL_0() {
		@Override
		public String getText() {
			return "All 0s";
		}

		@Override
		public String getToolTipText() {
			return "Set all bits to 0";
		}

		@Override
		public ImageDescriptor getImage() {
			return AVRUIPlugin.getImageDescriptor("icons/objs16/e_0x00.png");
		}

		@Override
		public ImageDescriptor getDisabledImage() {
			return AVRUIPlugin.getImageDescriptor("icons/objs16/d_0x00.png");
		}

	},

	/** Set all bytes to <code>-1</code> Action. */
	CLEAR() {
		@Override
		public String getText() {
			return "Clear";
		}

		@Override
		public String getToolTipText() {
			return "Clear all bytes";
		}

		@Override
		public ImageDescriptor getImage() {
			return AVRUIPlugin.getImageDescriptor("icons/objs16/e_clear_bytes.png");
		}

		@Override
		public ImageDescriptor getDisabledImage() {
			return AVRUIPlugin.getImageDescriptor("icons/objs16/d_clear_bytes.png");
		}

	};

	/**
	 * Get the text for the action. Not shown if Action is part of a ToolBar.
	 * 
	 * @return The text for this Action.
	 */
	protected abstract String getText();

	/**
	 * Get the tooltip text for the action.
	 * 
	 * @return The tooltip text for this Action.
	 */
	protected abstract String getToolTipText();

	/**
	 * Get the enabled state image description for the Action.
	 * 
	 * @return <code>ImageDescriptor</code>
	 */
	protected abstract ImageDescriptor getImage();

	/**
	 * Get the disabled state image description for the Action.
	 * 
	 * @return <code>ImageDescriptor</code>
	 */
	protected abstract ImageDescriptor getDisabledImage();

	/**
	 * Takes an Action and sets the following fields according to the current type:
	 * <ul>
	 * <li>Text</li>
	 * <li>ToolTipText</li>
	 * <li>ImageDescriptor</li>
	 * <li>DisabledImageDescriptor</li>
	 * </ul>
	 * 
	 * @param action
	 *            The action to initialize with some values.
	 */
	public void setupAction(IAction action) {

		action.setText(getText());
		action.setToolTipText(getToolTipText());
		action.setImageDescriptor(getImage());
		action.setDisabledImageDescriptor(getDisabledImage());

	}

}
