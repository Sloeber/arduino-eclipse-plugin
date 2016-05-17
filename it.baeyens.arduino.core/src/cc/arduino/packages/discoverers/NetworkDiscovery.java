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

package cc.arduino.packages.discoverers;

import java.io.IOException;
import java.net.InetAddress;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.Timer;

import javax.jmdns.JmDNS;
import javax.jmdns.NetworkTopologyDiscovery;
import javax.jmdns.ServiceEvent;
import javax.jmdns.ServiceInfo;
import javax.jmdns.ServiceListener;
import javax.jmdns.impl.DNSTaskStarter;

import it.baeyens.arduino.common.Const;

public class NetworkDiscovery implements ServiceListener {

    private class bonour {
	public String address;
	public String name;
	public String board;
	public String distroversion;
	public String port;
	public boolean ssh_upload;
	public boolean tcp_check;
	public boolean auth_upload;

	public bonour() {
	    this.address = ""; //$NON-NLS-1$
	    this.name = ""; //$NON-NLS-1$
	    this.board = ""; //$NON-NLS-1$
	    this.distroversion = ""; //$NON-NLS-1$
	    this.port = ""; //$NON-NLS-1$
	    this.ssh_upload = true;
	    this.tcp_check = true;
	    this.auth_upload = false;
	}

	public String getLabel() {
	    return this.name + " at " + this.address + " (" + this.board + ")" + this.distroversion + " " + this.port; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
	}

    }

    private static Timer timer = new Timer("Network discovery timer"); //$NON-NLS-1$ ;
    private static final HashSet<bonour> myComPorts = new HashSet<>(); // well
								       // not
								       // really
								       // com
								       // ports
								       // but
    // we treat them like com ports
    private final static Map<InetAddress, JmDNS> mappedJmDNSs = new Hashtable<>();
    private static NetworkDiscovery me = null;

    private NetworkDiscovery() {
	DNSTaskStarter.Factory.setClassDelegate(new ArduinoDNSTaskStarter());
    }

    public static String[] getList() {
	String[] ret = new String[myComPorts.size()];
	int curPort = 0;
	Iterator<bonour> iterator = myComPorts.iterator();
	while (iterator.hasNext()) {
	    bonour board = iterator.next();
	    ret[curPort++] = board.getLabel();
	}
	return ret;
    }

    public static void start() {
	if (me == null) {
	    me = new NetworkDiscovery();
	}
	new NetworkChecker(NetworkTopologyDiscovery.Factory.getInstance()).start(timer);
    }

    public static void stop() {
	timer.purge();
	// we don't close each JmDNS instance as it's too slow
    }

    @SuppressWarnings("resource")
    @Override
    public void serviceAdded(ServiceEvent serviceEvent) {
	String type = serviceEvent.getType();
	String name = serviceEvent.getName();

	JmDNS dns = serviceEvent.getDNS();

	dns.requestServiceInfo(type, name);
	ServiceInfo serviceInfo = dns.getServiceInfo(type, name);
	if (serviceInfo != null) {
	    dns.requestServiceInfo(type, name);
	}

    }

    @Override
    public void serviceRemoved(ServiceEvent serviceEvent) {
	String name = serviceEvent.getName();
	synchronized (this) {
	    removeBoardswithSameName(name);
	}
    }

    @Override
    public void serviceResolved(ServiceEvent serviceEvent) {
	ServiceInfo info = serviceEvent.getInfo();
	for (InetAddress inetAddress : info.getInet4Addresses()) {
	    bonour newItem = new bonour();
	    newItem.address = inetAddress.getHostAddress();
	    newItem.name = serviceEvent.getName();
	    if (info.hasData()) {
		newItem.board = info.getPropertyString("board"); //$NON-NLS-1$
		newItem.distroversion = info.getPropertyString("distro_version"); //$NON-NLS-1$
		newItem.name = info.getServer();
		String useSSH = info.getPropertyString("ssh_upload"); //$NON-NLS-1$
		String checkTCP = info.getPropertyString("tcp_check"); //$NON-NLS-1$
		String useAuth = info.getPropertyString("auth_upload"); //$NON-NLS-1$
		if (useSSH != null && useSSH.contentEquals("no")) //$NON-NLS-1$
		    newItem.ssh_upload = false;
		if (checkTCP != null && checkTCP.contentEquals("no")) //$NON-NLS-1$
		    newItem.tcp_check = false;
		if (useAuth != null && useAuth.contentEquals("yes")) //$NON-NLS-1$
		    newItem.auth_upload = true;
	    }
	    while (newItem.name.endsWith(".")) { //$NON-NLS-1$
		newItem.name = newItem.name.substring(0, newItem.name.length() - 1);
	    }
	    newItem.port = Integer.toString(info.getPort());

	    synchronized (this) {
		removeBoardswithSameAdress(newItem);
		myComPorts.add(newItem);
	    }
	}
    }

    private static void removeBoardswithSameAdress(bonour newBoard) {
	Iterator<bonour> iterator = myComPorts.iterator();
	while (iterator.hasNext()) {
	    bonour board = iterator.next();
	    if (newBoard.address.equals(board.address)) {
		iterator.remove();
	    }
	}
    }

    private static void removeBoardswithSameName(String name) {
	Iterator<bonour> iterator = myComPorts.iterator();
	while (iterator.hasNext()) {
	    bonour board = iterator.next();
	    if (name.equals(board.name)) {
		iterator.remove();
	    }
	}
    }

    @SuppressWarnings("resource")
    public static void inetAddressAdded(InetAddress address) {
	if (mappedJmDNSs.containsKey(address)) {
	    return;
	}
	try {
	    JmDNS jmDNS = JmDNS.create(address);
	    jmDNS.addServiceListener("_arduino._tcp.local.", me); //$NON-NLS-1$
	    mappedJmDNSs.put(address, jmDNS);
	} catch (Exception e) {
	    e.printStackTrace();
	}
    }

    @SuppressWarnings("resource")
    public static void inetAddressRemoved(InetAddress address) {
	JmDNS jmDNS = mappedJmDNSs.remove(address);
	if (jmDNS != null) {
	    try {
		jmDNS.close();
	    } catch (IOException e) {
		e.printStackTrace();
	    }
	}
    }

    private static bonour getBoardByName(String name) {
	Iterator<bonour> iterator = myComPorts.iterator();
	while (iterator.hasNext()) {
	    bonour board = iterator.next();
	    if (name.equals(board.name)) {
		return board;
	    }
	}
	return null;
    }

    public static String getAddress(String name) {
	bonour board = getBoardByName(name);
	if (board == null)
	    return null;
	return board.address;
    }

    public static String getPort(String name) {
	bonour board = getBoardByName(name);
	if (board == null)
	    return Const.EMPTY_STRING;
	return board.port;
    }

    public static boolean hasAuth(String name) {
	bonour board = getBoardByName(name);
	if (board == null)
	    return false;
	return board.auth_upload;
    }

    public static boolean isSSH(String name) {
	bonour board = getBoardByName(name);
	if (board == null)
	    return false;
	return board.ssh_upload;
    }

    public static boolean needstcpCheck(String name) {
	bonour board = getBoardByName(name);
	if (board == null)
	    return false;
	return board.tcp_check;
    }
}