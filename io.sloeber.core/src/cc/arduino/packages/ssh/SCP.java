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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.Session;

public class SCP extends SSH {

    private Channel channel;
    private OutputStream out;
    private InputStream in;

    public SCP(Session session) {
	super(session);
    }

    public void open() throws IOException {
	try {
	    this.channel = this.session.openChannel("exec"); //$NON-NLS-1$
	    ((ChannelExec) this.channel).setCommand("scp -t -r -d /"); //$NON-NLS-1$

	    this.out = this.channel.getOutputStream();
	    this.in = this.channel.getInputStream();

	    this.channel.connect();
	    ensureAcknowledged();
	} catch (Exception e) {
	    close();
	}
    }

    public void close() throws IOException {
	if (this.out != null) {
	    this.out.close();
	}
	if (this.in != null) {
	    this.in.close();
	}
	if (this.channel != null) {
	    this.channel.disconnect();
	}
    }

    protected void ensureAcknowledged() throws IOException {
	this.out.flush();

	int b = this.in.read();

	if (b == 0)
	    return;
	if (b == -1)
	    return;

	if (b == 1 || b == 2) {
	    StringBuilder sb = new StringBuilder();
	    sb.append("SCP error: "); //$NON-NLS-1$

	    int c;
	    do {
		c = this.in.read();
		sb.append((char) c);
	    } while (c != '\n');

	    throw new IOException(sb.toString());
	}

	throw new IOException("Uknown SCP error: " + b); //$NON-NLS-1$
    }

    public void sendFile(File localFile) throws IOException {
	sendFile(localFile, localFile.getName());
    }

    public void sendFile(File localFile, String remoteFile) throws IOException {
	this.out.write(("C0644 " + localFile.length() + " " + remoteFile + "\n").getBytes()); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	ensureAcknowledged();

	try (FileInputStream fis = new FileInputStream(localFile);) {

	    byte[] buf = new byte[4096];
	    while (true) {
		int len = fis.read(buf, 0, buf.length);
		if (len <= 0)
		    break;
		this.out.write(buf, 0, len);
	    }

	    // \0 terminates file
	    buf[0] = 0;
	    this.out.write(buf, 0, 1);
	}

	ensureAcknowledged();
    }

    public void startFolder(String folder) throws IOException {
	this.out.write(("D0755 0 " + folder + "\n").getBytes()); //$NON-NLS-1$ //$NON-NLS-2$
	ensureAcknowledged();
    }

    public void endFolder() throws IOException {
	this.out.write("E\n".getBytes()); //$NON-NLS-1$
	ensureAcknowledged();
    }

}
