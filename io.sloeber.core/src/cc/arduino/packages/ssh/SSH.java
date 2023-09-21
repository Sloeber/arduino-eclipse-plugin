/*
 * This file is part of Arduino.
 *
 * Arduino is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 *
 * As a special exception, you may use this file as part of a free software
 * library without restriction.  Specifically, if other files instantiate
 * templates or use macros or inline functions from this file, or you compile
 * this file and link it with other files to produce an executable, this
 * file does not by itself cause the resulting executable to be covered by
 * the GNU General Public License.  This exception does not however
 * invalidate any other reasons why the executable file might be covered by
 * the GNU General Public License.
 *
 * Copyright 2013 Arduino LLC (http://www.arduino.cc/)
 */

package cc.arduino.packages.ssh;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.ui.console.MessageConsoleStream;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;

import io.sloeber.core.Messages;
import io.sloeber.core.api.Common;
import io.sloeber.core.api.Const;

@SuppressWarnings({ "nls", "unused" })
public class SSH {

    final Session session;

    public SSH(Session session) {
        this.session = session;
    }

    public boolean execSyncCommand(String command) throws JSchException, IOException {
        return execSyncCommand(command, null, null);
    }

    public boolean execSyncCommand(String command, MessageConsoleStream stdoutStream,
            MessageConsoleStream stderrConsumer) throws JSchException, IOException {
        ChannelExec channel = (ChannelExec) session.openChannel("exec");

        try (InputStream stdout = channel.getInputStream(); InputStream stderr = channel.getErrStream();) {

            channel.setCommand(command);

            channel.setInputStream(null);

            // for one reason or another I need to swap error and output stream here
            Thread stdoutRunner = new Thread(new LogStreamRunner(stderr, stdoutStream));
            Thread stderrRunner = new Thread(new LogStreamRunner(stdout, stderrConsumer));
            stdoutRunner.start();
            stderrRunner.start();

            channel.connect();

            int exitCode = consumeOutputSyncAndReturnExitCode(channel);

            return exitCode == 0;

        } finally {
            if (channel != null) {
                channel.disconnect();
            }
        }
    }

    private static int consumeOutputSyncAndReturnExitCode(Channel channel) {
        while (true) {

            if (channel.isClosed()) {
                return channel.getExitStatus();
            }
            try {
                Thread.sleep(100);
            } catch (Exception ee) {
                // noop
            }
        }
    }

    /**
     * A runnable class that will read a Stream until EOF, storing each line in a
     * List and also calling a listener for each line.
     */
    private class LogStreamRunner implements Runnable {

        private final BufferedReader fReader;
        private MessageConsoleStream fConsoleOutput = null;

        /**
         * Construct a Streamrunner that will read the given InputStream and log all
         * lines in the given List.
         * <p>
         * If a valid <code>OutputStream</code> is set, everything read by this
         * <code>LogStreamRunner</code> is also written to it.
         *
         * @param instream
         *            <code>InputStream</code> to read
         * @param log
         *            <code>List&lt;String&gt;</code> where all lines of the instream
         *            are stored
         * @param consolestream
         *            <code>OutputStream</code> for secondary console output, or
         *            <code>null</code> for no console output.
         */
        public LogStreamRunner(InputStream instream, MessageConsoleStream consolestream) {
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
                IStatus status = new Status(IStatus.ERROR, Const.CORE_PLUGIN_ID, Messages.command_io, e);
                Common.log(status);
            } finally {
                try {
                    this.fReader.close();
                } catch (IOException e) {
                    // can't do anything
                }
            }
        }
    }

}
