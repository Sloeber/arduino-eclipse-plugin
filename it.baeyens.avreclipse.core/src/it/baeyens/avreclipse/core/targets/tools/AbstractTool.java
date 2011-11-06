/*******************************************************************************
 * 
 * Copyright (c) 2009, 2010 Thomas Holland (thomas@innot.de) and others
 * 
 * This program and the accompanying materials are made
 * available under the terms of the GNU Public License v3
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 * 
 * Contributors:
 *     Thomas Holland - initial API and implementation
 *     
 * $Id: AbstractTool.java 851 2010-08-07 19:37:00Z innot $
 *     
 *******************************************************************************/

package it.baeyens.avreclipse.core.targets.tools;

import it.baeyens.avreclipse.AVRPlugin;
import it.baeyens.avreclipse.core.avrdude.AVRDudeException;
import it.baeyens.avreclipse.core.avrdude.AVRDudeException.Reason;
import it.baeyens.avreclipse.core.targets.ITargetConfigConstants;
import it.baeyens.avreclipse.core.targets.ITargetConfiguration;
import it.baeyens.avreclipse.core.targets.ToolManager;
import it.baeyens.avreclipse.core.toolinfo.ExternalCommandLauncher;
import it.baeyens.avreclipse.core.toolinfo.ICommandOutputListener;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.ui.console.IOConsoleOutputStream;
import org.eclipse.ui.console.MessageConsole;


/**
 * @author Thomas Holland
 * @since
 * 
 */
public abstract class AbstractTool {

	private final ITargetConfiguration	fHC;

	protected AbstractTool(ITargetConfiguration hc) {
		fHC = hc;
	}

	protected abstract String getName();

	protected abstract String getId();

	/**
	 * Returns the value of the command attribute.
	 * <p>
	 * This is used to get the name of the executable for the tool. The command can be either just
	 * the command name (e.g. 'avrdude') or a absolute path (e.g. '/usr/bin/avrdude')
	 * </p>
	 * 
	 * @return String with the command
	 */
	protected abstract String getCommand();

	protected abstract ICommandOutputListener getOutputListener();

	protected ITargetConfiguration getHardwareConfig() {
		return fHC;
	}

	/**
	 * Runs the tool with the given arguments.
	 * <p>
	 * The Output of stdout and stderr are merged and returned in a <code>List&lt;String&gt;</code>.
	 * </p>
	 * <p>
	 * If the command fails to execute an entry is written to the log and an
	 * {@link AVRDudeException} with the reason is thrown.
	 * </p>
	 * 
	 * @param arguments
	 *            Zero or more arguments for avrdude
	 * @return A list of all output lines, or <code>null</code> if the command could not be
	 *         launched.
	 * @throws AVRDudeException
	 *             when avrdude cannot be started or when avrdude returned an
	 */
	public List<String> runCommand(String... arguments) throws AVRDudeException {

		List<String> arglist = new ArrayList<String>(1);
		for (String arg : arguments) {
			arglist.add(arg);
		}

		return runCommand(arglist, new NullProgressMonitor(), false, null);
	}

	/**
	 * Runs tool with the given arguments.
	 * <p>
	 * The Output of stdout and stderr are merged and returned in a <code>List&lt;String&gt;</code>.
	 * If the "use Console" flag is set in the Preferences, the complete output is shown on a
	 * Console as well.
	 * </p>
	 * <p>
	 * If the command fails to execute an entry is written to the log and an
	 * {@link AVRDudeException} with the reason is thrown.
	 * </p>
	 * 
	 * @param arguments
	 *            <code>List&lt;String&gt;</code> with the arguments
	 * @param monitor
	 *            <code>IProgressMonitor</code> to cancel the running process.
	 * @param forceconsole
	 *            If <code>true</code> all output is copied to the console, regardless of the "use
	 *            console" flag.
	 * @param cwd
	 *            <code>IPath</code> with a current working directory or <code>null</code> to use
	 *            the default working directory (usually the one defined with the system property
	 *            <code>user.dir</code). May not be empty.
	 * @return A list of all output lines, or <code>null</code> if the command could not be
	 *         launched.
	 * @throws AVRDudeException
	 *             when the tool cannot be started or when it returns with an error.
	 */
	public List<String> runCommand(List<String> arglist, IProgressMonitor monitor,
			boolean forceconsole, IPath cwd) throws AVRDudeException {

		try {
			monitor.beginTask("Running " + getName(), 100);

			// Check if the CWD is valid
			if (cwd != null && cwd.isEmpty()) {
				throw new AVRDudeException(Reason.INVALID_CWD,
						"CWD does not point to a valid directory.");
			}

			// TODO: resolve variables in the path
			String command = getCommand();

			// Set up the External Command
			ExternalCommandLauncher launcher = new ExternalCommandLauncher(command, arglist, cwd);
			launcher.redirectErrorStream(true);

			// Set the Console (if requested by the user for the target configuratio)
			MessageConsole console = null;
			String consoleattr = getId() + ".useconsole";
			boolean useconsole = fHC.getBooleanAttribute(consoleattr);
			if (useconsole || forceconsole) {
				console = AVRPlugin.getDefault().getConsole("External Tools");
				launcher.setConsole(console);
			}

			ICommandOutputListener outputlistener = getOutputListener();
			outputlistener.init(monitor);
			launcher.setCommandOutputListener(outputlistener);

			// USB devices:
			// This will delay the actual call if the previous call finished less than the
			// user provided time in milliseconds
			avrdudeInvocationDelay(console, new SubProgressMonitor(monitor, 10));

			// Run avrdude
			try {
				int result = launcher.launch(new SubProgressMonitor(monitor, 80));

				// Test if launch was aborted
				Reason abortReason = outputlistener.getAbortReason();
				if (abortReason != null) {
					throw new AVRDudeException(abortReason, outputlistener.getAbortLine());
				}

				if (result == -1) {
					throw new AVRDudeException(Reason.USER_CANCEL, "");
				}
			} catch (IOException e) {
				// Something didn't work while running the external command
				throw new AVRDudeException(Reason.NO_AVRDUDE_FOUND,
						"Cannot run AVRDude executable. Please check the AVR path preferences.", e);
			}

			// Everything was fine: get the ooutput from avrdude and return it
			// to the caller
			List<String> stdout = launcher.getStdOut();

			monitor.worked(10);

			return stdout;
		} finally {
			monitor.done();
			String progport = fHC.getAttribute(ITargetConfigConstants.ATTR_PROGRAMMER_PORT);
			ToolManager.getDefault().setLastAccess(progport, System.currentTimeMillis());
		}
	}

	/**
	 * Delay for the user specified invocation delay time.
	 * <p>
	 * This method will take the user supplied delay value from the given ProgrammerConfig, check
	 * how much time has passed since the last tool invocation finished and - if actually required -
	 * wait for the remaining milliseconds.
	 * </p>
	 * <p>
	 * While sleeping this method will wake up every 10 ms to check if the user has cancelled, in
	 * which case an {@link AVRDudeException} with {@link Reason#USER_CANCEL} is thrown.
	 * </p>
	 * 
	 * @param console
	 *            If not <code>null</code>, then the start and end of the delay is logged on the
	 *            console.
	 * @param monitor
	 *            polled for user cancel event.
	 * @throws AVRDudeException
	 *             when the user cancels the delay.
	 */
	private void avrdudeInvocationDelay(MessageConsole console, IProgressMonitor monitor)
			throws AVRDudeException {

		// Get the (optional) invocation delay value
		String delayattr = ITargetConfigConstants.ATTR_USB_DELAY;
		String delayvalue = fHC.getAttribute(delayattr);
		if (delayvalue == null || delayvalue.length() == 0) {
			return;
		}
		final int delay = Integer.decode(delayvalue);
		if (delay == 0) {
			return;
		}

		IOConsoleOutputStream ostream = null;
		if (console != null) {
			ostream = console.newOutputStream();
		}

		String programmerport = fHC.getAttribute(ITargetConfigConstants.ATTR_PROGRAMMER_PORT);
		long lastaccess = ToolManager.getDefault().getLastAccess(programmerport);
		final long targetmillis = lastaccess + delay;

		// Quick exit if the delay has already expired
		long targetdelay = targetmillis - System.currentTimeMillis();
		if (targetdelay < 1L) {
			return;
		}

		try {
			monitor.beginTask("delay", (int) (targetdelay / 10));

			writeOutput(ostream, "\n>>> " + getName() + " invocation delay: " + targetdelay
					+ " milliseconds\n");

			// delay for specified amount of milliseconds
			// To allow user cancel during long delays we check the monitor every 10
			// milliseconds.
			// This is the fix for Bug 2071415
			while (System.currentTimeMillis() < targetmillis) {
				if (monitor.isCanceled()) {
					writeOutput(ostream, ">>> " + getName() + " invocation delay: cancelled\n");
					throw new AVRDudeException(Reason.USER_CANCEL, "User cancelled");
				}
				Thread.sleep(10);
				monitor.worked(1);
			}
			writeOutput(ostream, ">>> " + getName() + " invocation delay: finished\n");

		} catch (InterruptedException e) {
			throw new AVRDudeException(Reason.USER_CANCEL, "System interrupt");
		} catch (IOException e) {
			// ignore exception
		} finally {
			if (ostream != null) {
				try {
					ostream.close();
				} catch (IOException e) {
					// ignore exception
				}
			}
			monitor.done();
		}

	}

	/**
	 * Convenience method to print a message to the given stream. This method checks that the stream
	 * exists.
	 * 
	 * @param ostream
	 * @param message
	 * @throws IOException
	 */
	private void writeOutput(IOConsoleOutputStream ostream, String message) throws IOException {
		if (ostream != null) {
			ostream.write(message);
		}
	}

}
