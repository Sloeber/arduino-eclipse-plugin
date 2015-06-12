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
package it.baeyens.arduino.tools;

import it.baeyens.arduino.common.ArduinoConst;
import it.baeyens.arduino.common.ArduinoInstancePreferences;
import it.baeyens.arduino.common.Common;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.console.MessageConsole;
import org.eclipse.ui.console.MessageConsoleStream;

/**
 * Launch external programs.
 * <p>
 * This is a wrapper around the <code>java.lang.ProcessBuilder</code> to launch external programs and fetch their results.
 * </p>
 * <p>
 * The results of the program run are stored in two <code>List&lt;String&gt;</code> arrays, one for the stdout and one for stderr. Receivers can also
 * register a {@link ICommandOutputListener} to get the output line by line while it is generated, for example to update the user interface.
 * </p>
 * <p>
 * Optionally an <code>IProgressMonitor</code> can be passed to the launch method to cancel running commands
 * </p>
 * 
 * @author Thomas Holland
 * @since 2.2
 * 
 */
public class ExternalCommandLauncher {

    /** Lock for internal synchronization */
    protected final Object fRunLock;

    private final ProcessBuilder fProcessBuilder;

    private List<String> fStdOut;
    private List<String> fStdErr;

    private MessageConsole fConsole = null;

    private final static int COLOR_STDOUT = SWT.COLOR_DARK_GREEN;
    private final static int COLOR_STDERR = SWT.COLOR_DARK_RED;

    /**
     * A runnable class that will read a Stream until EOF, storing each line in a List and also calling a listener for each line.
     */
    private class LogStreamRunner implements Runnable {

	private final BufferedReader fReader;
	private final List<String> fLog;
	private MessageConsoleStream fConsoleOutput = null;

	/**
	 * Construct a Streamrunner that will read the given InputStream and log all lines in the given List.
	 * <p>
	 * If a valid <code>OutputStream</code> is set, everything read by this <code>LogStreamRunner</code> is also written to it.
	 * 
	 * @param instream
	 *            <code>InputStream</code> to read
	 * @param log
	 *            <code>List&lt;String&gt;</code> where all lines of the instream are stored
	 * @param consolestream
	 *            <code>OutputStream</code> for secondary console output, or <code>null</code> for no console output.
	 */
	public LogStreamRunner(InputStream instream, List<String> log, MessageConsoleStream consolestream) {
	    fReader = new BufferedReader(new InputStreamReader(instream));
	    fLog = log;
	    fConsoleOutput = consolestream;
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
		    String line = fReader.readLine();
		    if (line != null) {
			// Add the line to the total output
			fLog.add(line);

			// And print to the console (if active)
			if (fConsoleOutput != null) {
			    fConsoleOutput.print(line + "\n");
			}
		    } else {
			break;
		    }
		}
	    } catch (IOException e) {
		// This is unlikely to happen, but log it nevertheless
		IStatus status = new Status(IStatus.ERROR, ArduinoConst.CORE_PLUGIN_ID, "I/O Error reading output", e);
		Common.log(status);
	    } finally {
		try {
		    fReader.close();
		} catch (IOException e) {
		    // can't do anything
		}
	    }
	    synchronized (fRunLock) {
		// Notify the caller that this thread is finished
		fRunLock.notifyAll();
	    }
	}
    }

    /**
     * Creates a new ExternalCommandLauncher for the given command and a list of arguments.
     * 
     * @param command
     *            <code>String</code> with the command
     * @param arguments
     *            all arguments
     */
    public ExternalCommandLauncher(String command) {
	Assert.isNotNull(command);
	fRunLock = this;
	String[] commandParts = command.split(" +(?=([^\"]*\"[^\"]*\")*[^\"]*$)");
	for (int curCommand = 0; curCommand < commandParts.length; curCommand++) {
	    if (commandParts[curCommand].startsWith("\"") && commandParts[curCommand].endsWith("\"")) {
		commandParts[curCommand] = commandParts[curCommand].substring(1, commandParts[curCommand].length() - 1);
	    }

	}
	fProcessBuilder = new ProcessBuilder(Arrays.asList(commandParts));
    }

    public ExternalCommandLauncher(List<String> command) {
	Assert.isNotNull(command);
	fRunLock = this;
	// "-C/home/jan/programs/arduino-1.5.2/hardware/tools/avrdude.conf"
	// -patmega2560 -cwiring -P -b115200 -D
	// "-Uflash:w:/home/jan/workspaces/runtime-eclipse/tst_avr_mega_41/release/tst_avr_mega_41.hex:i"

	fProcessBuilder = new ProcessBuilder(command);
    }

    /**
     * Launch the external program.
     * <p>
     * This method blocks until the external program has finished.
     * <p>
     * The output from <code>stdout</code> can be retrieved with {@link #getStdOut()}, the output from <code>stderr</code> likewise with
     * {@link #getStdErr()}.
     * </p>
     * 
     * @see java.lang.Process
     * @see java.lang.ProcessBuilder#start()
     * 
     * @return Result code of the external program. Usually <code>0</code> means successful.
     * @throws IOException
     *             An Exception from the underlying Process.
     */
    public int launch() throws IOException {
	return launch(new NullProgressMonitor());
    }

    /**
     * Launch the external program with a ProgressMonitor.
     * <p>
     * This method blocks until the external program has finished or the ProgressMonitor is canceled.
     * <p>
     * The output from <code>stdout</code> can be retrieved with {@link #getStdOut()}, the output from <code>stderr</code> likewise with
     * {@link #getStdErr()}.
     * </p>
     * 
     * @see java.lang.Process
     * @see java.lang.ProcessBuilder#start()
     * 
     * @param monitor
     *            A <code>IProgressMonitor</code> to cancel the running external program
     * @return Result code of the external program. Usually <code>0</code> means successful. A canceled program will return <code>-1</code>
     * @throws IOException
     *             An Exception from the underlying Process.
     */
    @SuppressWarnings("resource")
    public int launch(IProgressMonitor inMonitor) throws IOException {
	IProgressMonitor monitor = inMonitor;
	if (monitor == null) {
	    monitor = new NullProgressMonitor();
	}
	Process process = null;
	final MessageConsoleStream defaultConsoleStream;
	final MessageConsoleStream stdoutConsoleStream;
	final MessageConsoleStream stderrConsoleStream;

	// Init the console output if a console has been set
	// This will set the low / high water marks,
	// get three MessageStreams for the console
	// (default in black, stdout in dark green, stderr in dark red)
	// and print a small header (incl. command name and all args)
	if (fConsole != null) {
	    // Limit the size of the console
	    fConsole.setWaterMarks(8192, 16384);

	    // and get the output streams
	    defaultConsoleStream = fConsole.newMessageStream();
	    stdoutConsoleStream = fConsole.newMessageStream();
	    stderrConsoleStream = fConsole.newMessageStream();

	    // Set colors for the streams. This needs to be done in the UI
	    // thread (in which we may not be)
	    Display display = PlatformUI.getWorkbench().getDisplay();
	    if (display != null && !display.isDisposed()) {
		display.syncExec(new Runnable() {
		    @Override
		    public void run() {
			stdoutConsoleStream.setColor(PlatformUI.getWorkbench().getDisplay().getSystemColor(COLOR_STDOUT));
			stderrConsoleStream.setColor(PlatformUI.getWorkbench().getDisplay().getSystemColor(COLOR_STDERR));
		    }
		});
	    }
	    // Now print the Command line before any output is written to
	    // the console.
	    defaultConsoleStream.println();
	    defaultConsoleStream.println();
	    defaultConsoleStream.print("Launching ");
	    List<String> commandAndOptions = fProcessBuilder.command();
	    for (String str : commandAndOptions) {
		defaultConsoleStream.print(str + " ");
	    }
	    defaultConsoleStream.println();
	    defaultConsoleStream.println("Output:");
	} else {
	    // No console output requested, set all streams to null
	    defaultConsoleStream = null;
	    stdoutConsoleStream = null;
	    stderrConsoleStream = null;
	}

	// Get the name of the command (without the path)
	// This is used upon exit to print a nice exit message
	String command = fProcessBuilder.command().get(0);
	String commandname = command.substring(command.lastIndexOf(File.separatorChar) + 1);

	// After the setup we can now start the command
	try {
	    monitor.beginTask("Launching " + fProcessBuilder.command().get(0), 100);

	    fStdOut = new ArrayList<String>();
	    fStdErr = new ArrayList<String>();

	    fProcessBuilder.directory(ArduinoInstancePreferences.getArduinoPath().toFile());
	    process = fProcessBuilder.start();

	    Thread stdoutRunner = new Thread(new LogStreamRunner(process.getInputStream(), fStdOut, stdoutConsoleStream));
	    Thread stderrRunner = new Thread(new LogStreamRunner(process.getErrorStream(), fStdErr, stderrConsoleStream));

	    synchronized (fRunLock) {
		// Wait either for the logrunners to terminate or the user to
		// cancel the job.
		// The monitor is polled 10 times / sec.
		stdoutRunner.start();
		stderrRunner.start();

		monitor.worked(5);

		while (stdoutRunner.isAlive() || stderrRunner.isAlive()) {
		    fRunLock.wait(100);
		    if (monitor.isCanceled() == true) {
			process.destroy();
			process.waitFor();

			if (defaultConsoleStream != null) {
			    // Write an Abort Message to the console (if active)
			    defaultConsoleStream.println(commandname + " execution aborted");
			}
			return -1;
		    }
		}
	    }

	    // external process finished normally
	    monitor.worked(95);
	    if (defaultConsoleStream != null) {
		defaultConsoleStream.println(commandname + " finished");
	    }
	} catch (InterruptedException e) {
	    // This thread was interrupted from outside
	    // consider this to be a failure of the external programm
	    if (defaultConsoleStream != null) {
		// Write an Abort Message to the console (if active)
		defaultConsoleStream.println(commandname + " execution interrupted");
	    }
	    return -1;
	} finally {
	    monitor.done();
	    if (defaultConsoleStream != null)
		defaultConsoleStream.close();
	    if (stdoutConsoleStream != null)
		stdoutConsoleStream.close();
	    if (stderrConsoleStream != null)
		stderrConsoleStream.close();
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

    /**
     * Returns the <code>stdout</code> output from the last external Program launch.
     * 
     * @return <code>List&lt;String&gt;</code> with all lines or <code>null</code> if the external program has never been launched
     */
    public List<String> getStdOut() {
	return fStdOut;
    }

    /**
     * Returns the <code>stderr</code> output from the last external Program launch.
     * 
     * @return <code>List&lt;String&gt;</code> with all lines or <code>null</code> if the external program has never been launched
     */
    public List<String> getStdErr() {
	return fStdErr;
    }

    /**
     * Redirects the <code>stderr</code> output to <code>stdout</code>.
     * <p>
     * Use this either when not sure which stream an external program writes its output to (some programs, like avr-size.exe write their help output
     * to stderr), or when you like any error messages inserted into the normal output stream for analysis
     * </p>
     * <p>
     * Note: The redirection takes place at system level, so a command output listener will only receive the mixed output.
     * </p>
     * 
     * @see ProcessBuilder#redirectErrorStream(boolean)
     * 
     * @param redirect
     *            <code>true</code> to redirect <code>stderr</code> to <code>stdout</code>
     */
    public void redirectErrorStream(boolean redirect) {
	fProcessBuilder.redirectErrorStream(redirect);
    }

    /**
     * Sets a Console where all output of the external command will go.
     * <p>
     * This is mostly for debugging. The output to the console is in addition to the normal logging of this class.
     * </p>
     * <p>
     * This method must be called before the {@link #launch()} method. Once the external command has been launched, calling this method will not have
     * any effect.
     * </p>
     * 
     * @param console
     *            <code>MessageConsole</code> or <code>null</code> to disable console output.
     */
    public void setConsole(MessageConsole console) {
	fConsole = console;
    }

}
