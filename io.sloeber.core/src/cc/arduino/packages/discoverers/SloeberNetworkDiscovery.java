package cc.arduino.packages.discoverers;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import cc.arduino.packages.BoardPort;

public class SloeberNetworkDiscovery {
	private static NetworkDiscovery discoverer=null;
public static void start() {
	getDiscovery().start();
}
public static void stop() {
	getDiscovery().stop();
}
@SuppressWarnings("nls")
public static String[] getList() {
	List<BoardPort> boardPorts=getDiscovery().getBoardPortsDiscoveredWithJmDNS();
	HashSet<String> allBoards = new HashSet<>();
	for (BoardPort boardPort : boardPorts) {
		String boardName=boardPort.getBoardName();
		if(!boardName.contains(".")) {
			boardName=boardName+".local";
		}
		allBoards.add(boardName+" "+boardPort.getAddress());
	}
	String[] sBoards = new String[allBoards.size()];
	allBoards.toArray(sBoards);
	Arrays.sort(sBoards);
	return sBoards;
}
private static NetworkDiscovery getDiscovery() {
	if(discoverer==null) {
		discoverer=new NetworkDiscovery();
	}
	return discoverer;
}
}
