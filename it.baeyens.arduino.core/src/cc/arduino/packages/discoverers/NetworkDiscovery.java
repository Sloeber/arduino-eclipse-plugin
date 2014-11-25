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

//import cc.arduino.packages.BoardPort;
//import cc.arduino.packages.Discovery;
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

import processing.app.zeroconf.jmdns.ArduinoDNSTaskStarter;
import cc.arduino.packages.discoverers.network.NetworkChecker;

public class NetworkDiscovery implements ServiceListener, cc.arduino.packages.discoverers.network.NetworkTopologyListener {

    private class bonour {
	public String address;
	public String name;

	public String board;
	public String distroversion;

	public String port;

	public bonour() {
	    address = "";
	    name = "";
	    board = "";
	    distroversion = "";
	    port = "";
	}

	public String getLabel() {
	    return name + " at " + address + " (" + board + ")" + distroversion + " " + port;
	}

    }

    private Timer timer;
    private final HashSet<bonour> myComPorts; // well not really com ports but we treat them like com ports
    private final Map<InetAddress, JmDNS> mappedJmDNSs;

    public NetworkDiscovery() {
	DNSTaskStarter.Factory.setClassDelegate(new ArduinoDNSTaskStarter());
	this.myComPorts = new HashSet<bonour>();
	this.mappedJmDNSs = new Hashtable<InetAddress, JmDNS>();
    }

    public String[] getList() {
	String[] ret = new String[myComPorts.size()];
	int curPort = 0;
	Iterator<bonour> iterator = myComPorts.iterator();
	while (iterator.hasNext()) {
	    bonour board = iterator.next();
	    ret[curPort++] = board.getLabel();
	}
	return ret;
    }

    // @Override
    // public List<BoardPort> discovery() {
    // List<BoardPort> ports = clonePortsList();
    // Iterator<BoardPort> iterator = ports.iterator();
    // while (iterator.hasNext()) {
    // try {
    // BoardPort board = iterator.next();
    // if (!NetUtils.isReachable(InetAddress.getByName(board.getAddress()), Integer.parseInt(board.getPrefs().get("port")))) {
    // iterator.remove();
    // }
    // } catch (UnknownHostException e) {
    // iterator.remove();
    // }
    // }
    // return ports;
    // }

    // private List<BoardPort> clonePortsList() {
    // synchronized (this) {
    // return new ArrayList<BoardPort>(this.ports);
    // }
    // }

    public void start() {
	this.timer = new Timer(this.getClass().getName() + " timer");
	new NetworkChecker(this, NetworkTopologyDiscovery.Factory.getInstance()).start(timer);
    }

    public void stop() {
	timer.purge();
	// we don't close each JmDNS instance as it's too slow
    }

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
		newItem.board = info.getPropertyString("board");
		newItem.distroversion = info.getPropertyString("distro_version");
		newItem.name = info.getServer();
	    }
	    while (newItem.name.endsWith(".")) {
		newItem.name = newItem.name.substring(0, newItem.name.length() - 1);
	    }
	    newItem.port = Integer.toString(info.getPort());

	    synchronized (this) {
		removeBoardswithSameAdress(newItem);
		myComPorts.add(newItem);
	    }
	}
    }

    private void removeBoardswithSameAdress(bonour newBoard) {
	Iterator<bonour> iterator = myComPorts.iterator();
	while (iterator.hasNext()) {
	    bonour board = iterator.next();
	    if (newBoard.address.equals(board.address)) {
		iterator.remove();
	    }
	}
    }

    private void removeBoardswithSameName(String name) {
	Iterator<bonour> iterator = myComPorts.iterator();
	while (iterator.hasNext()) {
	    bonour board = iterator.next();
	    if (name.equals(board.name)) {
		iterator.remove();
	    }
	}
    }

    @Override
    public void inetAddressAdded(InetAddress address) {
	if (mappedJmDNSs.containsKey(address)) {
	    return;
	}
	try {
	    JmDNS jmDNS = JmDNS.create(address);
	    jmDNS.addServiceListener("_arduino._tcp.local.", this);
	    mappedJmDNSs.put(address, jmDNS);
	} catch (Exception e) {
	    e.printStackTrace();
	}
    }

    @Override
    public void inetAddressRemoved(InetAddress address) {
	JmDNS jmDNS = mappedJmDNSs.remove(address);
	if (jmDNS != null) {
	    try {
		jmDNS.close();
	    } catch (IOException e) {
		e.printStackTrace();
	    }
	}
    }
}
