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
 * $Id: AVRDudeAction.java 851 2010-08-07 19:37:00Z innot $
 *     
 *******************************************************************************/
package it.baeyens.avreclipse.core.avrdude;

import it.baeyens.avreclipse.mbs.BuildMacro;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.cdt.managedbuilder.core.IConfiguration;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.Path;


/**
 * @author Thomas Holland
 * 
 */
public class AVRDudeAction {

	public enum MemType {
		// The names of the ATXmega fusebytes is currently just speculation because avrdude does not
		// support more than 3 fuse bytes at this time.
		flash("Flash"), eeprom("EEPROM"), signature("Signature"), fuse("Fuse Byte"), lfuse(
				"Low Fuse Byte"), hfuse("High Fuse Byte"), efuse("Extended Fuse Byte"), lock(
				"Lock Byte"), calibration("Calibration Bytes"), fuse0("Fuse Byte 0"), fuse1(
				"Fuse Byte 1"), fuse2("Fuse Byte 2"), fuse3("Fuse Byte 3"), fuse4("Fuse Byte 4"), fuse5(
				"Fuse Byte 5");

		private String	name;

		private MemType(String name) {
			this.name = name;
		}

		@Override
		public String toString() {
			return name;
		}
	}

	public enum Action {
		read("r"), write("w"), verify("v");

		public String	symbol;

		private Action(String op) {
			symbol = op;
		}

		protected static Action getAction(String symbol) {
			Action[] allactions = Action.values();
			for (Action action : allactions) {
				if (action.symbol.equals(symbol)) {
					return action;
				}
			}
			return null;
		}
	}

	public enum FileType {
		iHex("i"), sRec("s"), raw("r"), immediate("m"), decimal("d"), hex("h"), octal("o"), binary(
				"b"), auto("a");

		public String	symbol;

		private FileType(String type) {
			symbol = type;
		}

		protected static FileType getFileType(String symbol) {
			FileType[] alltypes = FileType.values();
			for (FileType filetype : alltypes) {
				if (filetype.symbol.equals(symbol)) {
					return filetype;
				}
			}
			return null;
		}
	}

	private final MemType	fMemType;
	private final Action	fAction;
	private String			fFilename;
	private final FileType	fFileType;
	private int				fImmediateValue;

	public AVRDudeAction(MemType memtype, Action action, String filename, FileType filetype) {

		Assert.isTrue(filetype != FileType.immediate);

		fMemType = memtype;
		fAction = action;
		fFilename = filename;
		fFileType = filetype;
	}

	public AVRDudeAction(MemType memtype, Action action, int value) {
		fMemType = memtype;
		fAction = action;
		fImmediateValue = value;
		fFileType = FileType.immediate;
	}

	/**
	 * Get the name of the file.
	 * 
	 * @return <code>String</code> with the filename or <code>null</code> if this is an
	 *         immediate action.
	 */
	public String getFilename() {
		if (fFileType == FileType.immediate) {
			return null;
		}
		return fFilename;
	}

	/**
	 * Get the memory type of this action.
	 * 
	 * @return <code>MemType</code> enum value
	 */
	public MemType getMemType() {
		return fMemType;
	}

	/**
	 * Get the avrdude action option without resolving the filename.
	 * 
	 * @return <code>String</code> with an avrdude option
	 */
	public String getArgument() {
		return getArgument(null);
	}

	/**
	 * Get the avrdude action option.
	 * <p>
	 * The filename (if set) will be resolved against the given <code>IConfiguration</code>. Also
	 * the filename is converted to the OS format.
	 * 
	 * @param buildcfg
	 *            <code>IConfiguration</code> context for resolving macros.
	 * @return <code>String</code> with an avrdude option
	 */
	public String getArgument(IConfiguration buildcfg) {

		StringBuffer sb = new StringBuffer("-U");
		sb.append(fMemType.name());
		sb.append(":");
		sb.append(fAction.symbol);
		sb.append(":");

		// Two types: immediate mode or file mode
		// in the first case the immediate value is added to the output
		// in the second case the filename is included in the output
		if (fFileType.equals(FileType.immediate)) {
			// Eye Candy. We could just pass the integer value.
			// However, because the user always sees hex values for the
			// fusebytes in th user interface, we convert the byte value to hex
			// here as well.
			String hexvalue = Integer.toHexString(fImmediateValue);
			sb.append("0x" + hexvalue);

		} else {
			// We insert the Filename
			// Resolve the filename if we have a IConfiguration to resolve
			// against. Also change to path to the OS format while we are at it
			String filename;
			if (buildcfg != null) {
				String resolvedfilename = BuildMacro.resolveMacros(buildcfg, fFilename);
				filename = new Path(resolvedfilename).toOSString();
			} else {
				filename = fFilename;
			}

			sb.append(filename);
		}

		sb.append(":");
		sb.append(fFileType.symbol);

		return sb.toString();
	}

	private final static Pattern	fRemoveTrim	= Pattern.compile(".*?-U\\s*(.*)");

	/**
	 * Create an action for the given argument.
	 * <p>
	 * This is the reverse of {@link #getArgument()}. It parses the argument and extracts the
	 * information required to make a new <code>AVRDudeAction</code>
	 * </p>
	 * <p>
	 * The current implementation requires that the filetype at the end is set, otherwise it will
	 * not parse.
	 * </p>
	 * 
	 * @param argument
	 * @return
	 */
	public static AVRDudeAction getActionForArgument(String argument) {
		// This method is needed to program myself out of an hole. I want the
		// User interface to show a nice description of what is going on, but
		// within the user interface I have only the argument because
		// AVRDudeProperties keeps the Actions to itself. And changing
		// AVRDudeProperties would have broken more stuff, so I use this little
		// hack to infer a description from the argument it created itself.

		// From the basic flow this is close to the parse_op() function from
		// avrdude, although this is more forgiving.

		// The four parts of info we extract from the argument
		// FileType is optional for avrdude and defaults to "auto"
		MemType memtype;
		Action action;
		String filename;
		FileType filetype = FileType.auto;

		// First remove all trimming, i.e. the "-U", and enclosing quotes and
		// whitespaces
		Matcher matcher = fRemoveTrim.matcher(argument);
		if (!matcher.matches())
			// argument does not start with "-U"
			throw new IllegalArgumentException("Invalid argument for avrdude");

		String arg = matcher.group(1);

		// Get the first colon. Everything before it is the memtype,
		// the first char behind it is the action (r / w / v)
		int p = arg.indexOf(':');

		if (p == -1) {
			// Without colon the argument is filename for a write flash action
			return new AVRDudeAction(MemType.flash, Action.write, arg, FileType.auto);
		}

		// MemType
		String field = arg.substring(0, p);
		try {
			memtype = MemType.valueOf(field);
		} catch (IllegalArgumentException iae) {
			throw new IllegalArgumentException("Invalid memory specification \"" + field + "\"",
					iae);
		}

		// Action
		field = arg.substring(p + 1, p + 2);
		action = Action.getAction(field);
		if (action == null) {
			throw new IllegalArgumentException("Invalid action specification \"" + field + "\"");
		}

		// Get the next colon after the action field
		p = arg.indexOf(':', p + 1);

		int startfilename = p + 1;
		int endfilename = arg.length();

		// Is there a colon behind the filename?
		p = arg.lastIndexOf(':');
		if (p >= startfilename && p < arg.length() - 1) {
			// Yes. The char behind it is the filetype
			field = arg.substring(p + 1, p + 2);
			filetype = FileType.getFileType(field);
			if (filetype == null) {
				throw new IllegalArgumentException("Invalid file type specification \"" + field
						+ "\"");
			}
			endfilename = p;
		}

		// Get the filename
		filename = arg.substring(startfilename, endfilename);

		// OK, we have all four pieces of information
		AVRDudeAction result;

		if (filetype == FileType.immediate) {
			result = new AVRDudeAction(memtype, action, Integer.decode(filename));
		} else {
			result = new AVRDudeAction(memtype, action, filename, filetype);
		}

		return result;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		// Build a nice, descriptive String that can be shown in the user
		// interface.
		StringBuilder sb = new StringBuilder(80);
		switch (fAction) {
			case read:
				sb.append("Reading ");
				sb.append(fMemType);
				sb.append(" to file \"");
				sb.append(fFilename);
				sb.append("\" in ");
				sb.append(fFileType);
				sb.append(" format");
				break;
			case write:
				sb.append("Writing \"");
				if (fFileType == FileType.immediate) {
					sb.append("0x");
					sb.append(Integer.toHexString(fImmediateValue));
				} else {
					sb.append(fFilename);
				}
				sb.append("\" to ");
				sb.append(fMemType);
				break;
			case verify:
				sb.append("Verifying \"");
				sb.append(fMemType);
				sb.append(" against \"");
				if (fFileType == FileType.immediate) {
					sb.append("0x");
					sb.append(Integer.toHexString(fImmediateValue));
				} else {
					sb.append(fFilename);
				}
				sb.append("\"");
				break;
		}

		return sb.toString();
	}

}
