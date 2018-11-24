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
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Timer;

import javax.jmdns.JmmDNS;
import javax.jmdns.ServiceEvent;
import javax.jmdns.ServiceInfo;
import javax.jmdns.ServiceListener;

import cc.arduino.packages.BoardPort;
import cc.arduino.packages.Discovery;


public class NetworkDiscovery implements Discovery, ServiceListener {

  private final List<BoardPort> reachableBoardPorts = new LinkedList<>();
  private final List<BoardPort> boardPortsDiscoveredWithJmDNS = new LinkedList<>();
  private Timer reachabilityTimer=null;
  private JmmDNS jmdns = null;
  

  private void removeDuplicateBoards(BoardPort newBoard) {
    synchronized (this.boardPortsDiscoveredWithJmDNS) {
      Iterator<BoardPort> iterator = this.boardPortsDiscoveredWithJmDNS.iterator();
      while (iterator.hasNext()) {
        BoardPort board = iterator.next();
        if (newBoard.getAddress().equals(board.getAddress())) {
          iterator.remove();
        }
      }
    }
  }

  @Override
  public void serviceAdded(ServiceEvent serviceEvent) {
	  //do nothing
  }

  @Override
  public void serviceRemoved(ServiceEvent serviceEvent) {
    String name = serviceEvent.getName();
    synchronized (this.boardPortsDiscoveredWithJmDNS) {
      this.boardPortsDiscoveredWithJmDNS.stream().filter(port -> port.getBoardName().equals(name)).forEach(this.boardPortsDiscoveredWithJmDNS::remove);
    }
  }

  @SuppressWarnings("nls")
@Override
  public void serviceResolved(ServiceEvent serviceEvent) {

    ServiceInfo info = serviceEvent.getInfo();
    for (InetAddress inetAddress : info.getInet4Addresses()) {
      String address = inetAddress.getHostAddress();
      String name = serviceEvent.getName();

      BoardPort port = new BoardPort();

      String board = null;
      String description = null;
      if (info.hasData()) {
        board = info.getPropertyString("board");
        description = info.getPropertyString("description");
        port.getPrefs().put("board", board);
        port.getPrefs().put("distro_version", info.getPropertyString("distro_version"));
        port.getPrefs().put("port", "" + info.getPort());

        //Add additional fields to permit generic ota updates
        //and make sure we do not intefere with Arduino boards
        // define "ssh_upload=no" TXT property to use generic uploader
        // define "tcp_check=no" TXT property if you are not using TCP
        // define "auth_upload=yes" TXT property if you want to use authenticated generic upload
        String useSSH = info.getPropertyString("ssh_upload");
        String checkTCP = info.getPropertyString("tcp_check");
        String useAuth = info.getPropertyString("auth_upload");
        if(useSSH == null || !useSSH.contentEquals("no")) useSSH = "yes";
        if(checkTCP == null || !checkTCP.contentEquals("no")) checkTCP = "yes";
        if(useAuth == null || !useAuth.contentEquals("yes")) useAuth = "no";
        port.getPrefs().put("ssh_upload", useSSH);
        port.getPrefs().put("tcp_check", checkTCP);
        port.getPrefs().put("auth_upload", useAuth);
      }

      String label = name + " at " + address;
//      if (board != null && BaseNoGui.packages != null) {
//        String boardName = BaseNoGui.getPlatform().resolveDeviceByBoardID(BaseNoGui.packages, board);
//        if (boardName != null) {
//          label += " (" + boardName + ")";
//        }
//      } else if (description != null) {
        label += " (" + description + ")";
//      }

      port.setAddress(address);
      port.setBoardName(name);
      port.setProtocol("network");
      port.setLabel(label);

      synchronized (this.boardPortsDiscoveredWithJmDNS) {
        removeDuplicateBoards(port);
        this.boardPortsDiscoveredWithJmDNS.add(port);
      }
    }
  }

  public NetworkDiscovery() {

  }

  @SuppressWarnings("nls")
@Override
	public void start() {
		if (jmdns == null) {
			this.jmdns = JmmDNS.Factory.getInstance();
			this.jmdns.addServiceListener("_arduino._tcp.local.", this);
			if (reachabilityTimer == null) {
				this.reachabilityTimer = new Timer();
				new BoardReachabilityFilter(this).start(this.reachabilityTimer);
			}
		}
	}

	@Override
	public void stop() {
		if (reachabilityTimer != null) {
			this.reachabilityTimer.cancel();
			reachabilityTimer = null;
		}
		if (jmdns != null) {
			this.jmdns.unregisterAllServices();

			try {
				this.jmdns.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		jmdns = null;
		reachabilityTimer = null;
	}

  @Override
  public List<BoardPort> listDiscoveredBoards() {
      synchronized (this.reachableBoardPorts) {
      return new LinkedList<>(this.reachableBoardPorts);
    }
  }

  @Override
  public List<BoardPort> listDiscoveredBoards(boolean complete) {
    synchronized (this.reachableBoardPorts) {
      return new LinkedList<>(this.reachableBoardPorts);
    }
  }

  public void setReachableBoardPorts(List<BoardPort> newReachableBoardPorts) {
    synchronized (this.reachableBoardPorts) {
      this.reachableBoardPorts.clear();
      this.reachableBoardPorts.addAll(newReachableBoardPorts);
    }
  }

  public List<BoardPort> getBoardPortsDiscoveredWithJmDNS() {
    synchronized (this.boardPortsDiscoveredWithJmDNS) {
      return new LinkedList<>(this.boardPortsDiscoveredWithJmDNS);
    }
  }
}
