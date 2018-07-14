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
 * $Id: ExternalCommandLauncher.java 851 2010-08-07 19:37:00Z innot $
 *
 *******************************************************************************/
package io.sloeber.core.tools;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.List;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.ui.console.MessageConsoleStream;

import io.sloeber.core.Messages;
import io.sloeber.core.common.Common;
import io.sloeber.core.common.Const;

/**
 * Launch external programs.
 * <p>
 * This is a wrapper around the <code>java.lang.ProcessBuilder</code> to launch
 * external programs and fetch their results.
 * </p>
 * <p>
 * The results of the program run are stored in two
 * <code>List&lt;String&gt;</code> arrays, one for the stdout and one for
 * stderr. Receivers can also register a {@link ICommandOutputListener} to get
 * the output line by line while it is generated, for example to update the user
 * interface.
 * </p>
 * <p>
 * Optionally an <code>IProgressMonitor</code> can be passed to the launch
 * method to cancel running commands
 * </p>
 *
 * @author Thomas Holland
 * @since 2.2
 *
 */
@SuppressWarnings("unused") 
public class ExternalCommandLauncher {
	private static final String COMMAND=Messages.COMMAND;

	/** Lock for internal synchronization */
	protected final Object myRunLock;

	private final ProcessBuilder myProcessBuilder;

	/**
	 * A runnable class that will read a Stream until EOF, storing each line in
	 * a List and also calling a listener for each line.
	 */
	private class LogStreamRunner implements Runnable {

		private final BufferedReader fReader;
		private MessageConsoleStream fConsoleOutput = null;

		/**
		 * Construct a Streamrunner that will read the given InputStream and log
		 * all lines in the given List.
		 * <p>
		 * If a valid <code>OutputStream</code> is set, everything read by this
		 * <code>LogStreamRunner</code> is also written to it.
		 *
		 * @param instream
		 *            <code>InputStream</code> to read
		 * @param log
		 *            <code>List&lt;String&gt;</code> where all lines of the
		 *            instream are stored
		 * @param consolestream
		 *            <code>OutputStream</code> for secondary console output, or
		 *            <code>null</code> for no console output.
		 */
		public LogStreamRunner(InputStream instream,
				MessageConsoleStream consolestream) {
			this.fReader = new BufferedReader(new InputStreamReader(instream));
			this.fConsoleOutput = consolestream;
		}

		/*
		 * (non-Javadoc)
		 *
		 * @see java.lang.Runnable#run()
		 */
		@Override
		public void run() {
			try {
				for (;;) {
					// Processes a new process output line.
					// If a Listener has been registered, call it
					int read = this.fReader.read();
					if (read != -1) {
						String readChar = new String() + (char) read;

						// And print to the console (if active)
						if (this.fConsoleOutput != null) {
							this.fConsoleOutput.print(readChar);
						}
					} else {
						break;
					}
				}
			} catch (IOException e) {
				// This is unlikely to happen, but log it nevertheless
				IStatus status = new Status(IStatus.ERROR, Const.CORE_PLUGIN_ID,
						Messages.command_io, e);
				Common.log(status);
			} finally {
				try {
					this.fReader.close();
				} catch (IOException e) {
					// can't do anything
				}
			}
			synchronized (ExternalCommandLauncher.this.myRunLock) {
				// Notify the caller that this thread is finished
				ExternalCommandLauncher.this.myRunLock.notifyAll();
			}
		}
	}

	/**
	 * Creates a new ExternalCommandLauncher for the given command and a list of
	 * arguments.
	 *
	 * @param command
	 *            <code>String</code> with the command
	 * @param arguments
	 *            all arguments
	 */
	public ExternalCommandLauncher(String command) {
		Assert.isNotNull(command);
		this.myRunLock = this;
		String[] commandParts = command
				.split(" +(?=([^\"]*\"[^\"]*\")*[^\"]*$)"); //$NON-NLS-1$
		//TODO do we really want to remove the quotes?
		for (int curCommand = 0; curCommand < commandParts.length; curCommand++) {
			if (commandParts[curCommand].startsWith("\"") //$NON-NLS-1$
					&& commandParts[curCommand].endsWith("\"")) { //$NON-NLS-1$
				commandParts[curCommand] = commandParts[curCommand].substring(1,
						commandParts[curCommand].length() - 1);
			}

		}
		myProcessBuilder = new ProcessBuilder(Arrays.asList(commandParts));
		myProcessBuilder.redirectErrorStream(true);
	}

	/**
	 * Launch the external program with a ProgressMonitor.
	 * <p>
	 * This method blocks until the external program has finished or the
	 * ProgressMonitor is canceled.
	 * <p>
	 * The output from <code>stdout</code> can be retrieved with
	 * {@link #getStdOut()}, the output from <code>stderr</code> likewise with
	 * {@link #getStdErr()}.
	 * </p>
	 *
	 * @see java.lang.Process
	 * @see java.lang.ProcessBuilder#start()
	 *
	 * @param monitor
	 *            A <code>IProgressMonitor</code> to cancel the running external
	 *            program
	 * @return Result code of the external program. Usually <code>0</code> means
	 *         successful. A canceled program will return <code>-1</code>
	 * @throws IOException
	 *             An Exception from the underlying Process.
	 */
	public int launch(IProgressMonitor monitor, MessageConsoleStream highStream,
			MessageConsoleStream stdoutStream,
			MessageConsoleStream stderrStream) throws IOException {

		Process process = null;

		highStream.println();
		highStream.println();
		highStream.print(Messages.command_launching + ' ');
		List<String> commandAndOptions = myProcessBuilder.command();
		for (String str : commandAndOptions) {
			highStream.print(str + ' ');
		}
		highStream.println();
		highStream.println(Messages.command_output);

		// Get the name of the command (without the path)
		// This is used upon exit to print a nice exit message
		String command = commandAndOptions.get(0);
		String commandname = command
				.substring(command.lastIndexOf(File.separatorChar) + 1);

		// After the setup we can now start the command
		try {
			monitor.beginTask(Messages.command_launching + ' ' + command, 100);

			myProcessBuilder
					.directory(Common.getWorkspaceRoot().toPath().toFile());
			try {
				process = myProcessBuilder.start();
			} catch (IOException ioe) {
				String errorMessage = ioe.getMessage();
				if (errorMessage == null) {
					errorMessage = "no error message given"; //$NON-NLS-1$
				}
				stderrStream.println(errorMessage);
				ioe.printStackTrace();
				throw ioe;
			}

			Thread stdoutRunner = new Thread(new LogStreamRunner(
					process.getInputStream(), stdoutStream));
			Thread stderrRunner = new Thread(new LogStreamRunner(
					process.getErrorStream(), stderrStream));

			synchronized (myRunLock) {
				// Wait either for the longrunners to terminate or the user to
				// cancel the job.
				// The monitor is polled 10 times / sec.
				stdoutRunner.start();
				stderrRunner.start();

				monitor.worked(5);

				while (stdoutRunner.isAlive() || stderrRunner.isAlive()) {
					myRunLock.wait(100);
					if (monitor.isCanceled() == true) {
						process.destroy();
						process.waitFor();

						if (highStream != null) {
							// Write an Abort Message to the console (if active)
							highStream.println( Messages.command_aborted.replace(COMMAND, commandname)); 
						}
						return -1;
					}
				}
			}

			// external process finished normally
			monitor.worked(95);
			highStream.println(  Messages.command_finished.replace(COMMAND, commandname)); 

		} catch (InterruptedException e) {
			// This thread was interrupted from outside
			// consider this to be a failure of the external programm
			highStream.println( Messages.command_interupted.replace(COMMAND, commandname));
			return -1;
		}
		// if we make it to here, the process has run without any Exceptions
		// Wait for the process to finish and then get the return value.
		try {
			process.waitFor();
			return process.exitValue();
		} catch (InterruptedException e) {
			// If the process was interrupted by an external source we won't do
			// anything but return
			// an error value. (the return value is unused anyway throughout the
			// plugin.
			return -1;
		}
	}

}
