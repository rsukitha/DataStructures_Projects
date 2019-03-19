import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

public class Router {

	Boolean isShutdown = false;
	String routerID;
	String networkName;
	Integer sequenceNum = 1;
	Integer tick = 0;
	final Integer TTL = 10;
	HashMap<Integer, HashSet<String>> tickData = new HashMap<Integer, HashSet<String>>();
	HashMap<String, String> outgoingLink = new HashMap<String, String>();
	HashMap<String, Integer> lastLSPSeq = new HashMap<String, Integer>();
	HashMap<String, Integer> neighborsCost = new HashMap<String, Integer>();
	HashMap<String, RoutingEntry> routingTable = new HashMap<String, RoutingEntry>();
	HashMap<String, HashMap<String, Integer>> graph = new HashMap<String, HashMap<String, Integer>>();

	//Shutsdown Router
	public void shutdown() {
		if (isShutdown == true) {
			System.out.println("Router already turned off.");
			return;
		}
		System.out.println("Router ID = " + this.routerID + " is now shut down.\n");
		isShutdown = true;
		outgoingLink.clear();
	}

	//Starts the router
	public void startup() {
		if (isShutdown == false) {
			System.out.println("Router already running.");
			return;
		}
		isShutdown = false;
		initializeRouter();
		System.out.println("Router ID = " + this.routerID + " is now up.\n");
	}

	//Initializes with original neighbor costs
	private void initializeRouter() {
		graph.put(this.routerID, new HashMap<String, Integer>());
		for (String router : neighborsCost.keySet()) {
			if (!LinkStateRouting.routerList.get(router).isShutdown) {
				graph.get(routerID).put(router, neighborsCost.get(router));

				if (tickData.get(tick) == null)
					tickData.put(tick, new HashSet<String>());
				tickData.get(tick).add(routerID);
			}
		}

	}

	//Creates LSP and forwards it
	public void originatePacket() {
		if (this.isShutdown)
			return;
		LSP lsp = generateLSP();
		forwardLSP(lsp, null);
	}

	//Increments Tick and removes old tick values
	public void updateTick() {
		if (isShutdown)
			return;
		tick++;
		tickData.remove(tick - 2);
		outgoingLink.clear();
		originatePacket();
	}

	//forwards LSP to neighbors
	public void forwardLSP(LSP lsp, String exceptRouterID) {
		if (lsp.TTL <= 0)
			return;

		ArrayList<Router> receivingRoutersList = new ArrayList<Router>();

		for (String routerID : neighborsCost.keySet()) {
			receivingRoutersList.add(LinkStateRouting.routerList.get(routerID));
		}

		for (Router router : receivingRoutersList) {
			if (exceptRouterID != null) {
				if (!router.routerID.equals(exceptRouterID)) {
					router.receivePacket(lsp, this.routerID);
				}
			} else {
				router.receivePacket(lsp, this.routerID);
			}
		}
	}

	//Receives LSP and modifies graph accordingly. Then forwards the LSP further
	public void receivePacket(LSP lsp, String fwdRouterID) {
		if (isShutdown) {
			return;
		}
		if (lsp.originRouterID.equals(this.routerID)) {
			return;
		}
		if (tickData.get(tick) == null)
			tickData.put(tick, new HashSet<String>());

		HashSet<String> routers = tickData.get(tick);
		routers.add(fwdRouterID);

		if (lastLSPSeq.get(fwdRouterID) == null || lastLSPSeq.get(fwdRouterID) < lsp.sequenceNum) {
			lastLSPSeq.put(routerID, lsp.sequenceNum);
			lsp.TTL--;

			if (graph.get(lsp.originRouterID) == null) {
				graph.put(lsp.originRouterID, new HashMap<String, Integer>());
			}

			for (RoutingEntry entry : lsp.directConnections) {
				graph.get(lsp.originRouterID).put(entry.routerID, entry.cost);
			}

			updateRoutingTable();

			forwardLSP(lsp, fwdRouterID);
		}
	}

	//Uses Dijkstra's algorithm to calculate shortest distance to every other router
	private void updateRoutingTable() {

		HashSet<String> selectedRouters = new HashSet<String>();
		HashSet<String> allRouters = new HashSet<String>();
		HashMap<String, Integer> distances = new HashMap<String, Integer>();

		selectedRouters.add(this.routerID);
		for (String routerID : LinkStateRouting.routerList.keySet()) {
			allRouters.add(routerID);
		}

		for (String routerID : LinkStateRouting.routerList.keySet()) {
			if (routerID.equals(this.routerID))
				continue;
			Integer dist = this.graph.get(this.routerID) == null ? null : this.graph.get(this.routerID).get(routerID);
			if (dist == null)
				dist = Integer.MAX_VALUE;
			distances.put(routerID, dist);
		}
		allRouters.removeAll(selectedRouters);

		//Dijkstra's while loop part
		while (allRouters.size() > 0) {
			String router = findMin(allRouters, distances);
			allRouters.remove(router);
			selectedRouters.add(router);
			if (outgoingLink.get(router) == null)
				outgoingLink.put(router, router);

			for (String r : allRouters) {
				Integer dw = distances.get(r) == null ? Integer.MAX_VALUE : distances.get(r);
				Integer dv = distances.get(router) == null ? Integer.MAX_VALUE : distances.get(router);
				Integer cv = this.graph.get(router) == null || this.graph.get(router).get(r) == null ? Integer.MAX_VALUE
						: this.graph.get(router).get(r);
				Integer cvdv = cv == Integer.MAX_VALUE || dv == Integer.MAX_VALUE ? Integer.MAX_VALUE : cv + dv;
				Integer origDist = distances.get(r);
				Integer newDist = Math.min(dw, cvdv);

				distances.put(r, newDist);
				if (origDist == null || origDist > newDist)
					if (dw == Integer.MAX_VALUE && cvdv == Integer.MAX_VALUE) {
						outgoingLink.put(r, null);
					} else if (dw > cvdv) {
						String outgoingRouter = outgoingLink.get(router);
						while (outgoingRouter != outgoingLink.get(outgoingRouter)) {
							outgoingRouter = outgoingLink.get(outgoingRouter);
						}
						outgoingLink.put(r, outgoingRouter);
					} else {
						outgoingLink.put(r, r);
					}
			}
		}

		//stores routing info in routing table within router
		for (String routerID : distances.keySet()) {
			RoutingEntry e = routingTable.get(routerID);
			RoutingEntry entry = new RoutingEntry();
			entry.routerID = routerID;
			entry.cost = distances.get(routerID);
			entry.outgoingLink = outgoingLink.get(routerID);
			if (e == null || entry.cost < e.cost)
				routingTable.put(routerID, entry);
		}
	}

	//Prints routing table 
	public void printRoutingTable() {
		if (this.isShutdown) {
			System.out.println("Router ID = " + this.routerID + ", Network Name = "
					+ LinkStateRouting.routerIDNameMap.get(routerID) + " is shutdown!");
			System.out.println();
			return;
		}
		System.out.println("Printing table for Router ID = " + this.routerID + ", Network Name = "
				+ LinkStateRouting.routerIDNameMap.get(routerID));
		System.out.println("Network Name     Cost    Outgoing Link");
		for (String routerID : routingTable.keySet()) {
			RoutingEntry entry = routingTable.get(routerID);
			String destRouter = LinkStateRouting.routerIDNameMap.get(routerID);
			Integer cost = entry.cost == Integer.MAX_VALUE || LinkStateRouting.routerList.get(routerID).isShutdown
					? null
					: entry.cost;
			String outgoing = cost == null ? null : entry.outgoingLink;
			System.out.printf("%-14s  %5d    %13s", destRouter, cost, outgoing);
			System.out.println();
		}
		System.out.println();
	}

	//Find shortest distance router from available routers
	private String findMin(HashSet<String> allRouters, HashMap<String, Integer> distances) {
		Integer dist = Integer.MAX_VALUE;
		String minRouter = null;

		for (String routerID : allRouters) {
			if (dist >= distances.get(routerID)) {
				dist = distances.get(routerID);
				minRouter = routerID;
			}
		}
		return minRouter;
	}

	//Creates the Link State Packet
	private LSP generateLSP() {
		LSP lsp = new LSP();
		lsp.originRouterID = this.routerID;
		lsp.sequenceNum = this.sequenceNum;
		this.sequenceNum++;
		lsp.TTL = this.TTL;
		for (String neighborRouterID : neighborsCost.keySet()) {
			if (!checkTickData(neighborRouterID))
				continue;
			RoutingEntry entry = new RoutingEntry();
			entry.routerID = neighborRouterID;
			entry.networkName = LinkStateRouting.routerIDNameMap.get(routerID);
			entry.cost = neighborsCost.get(neighborRouterID);
			lsp.directConnections.add(entry);
		}
		return lsp;
	}

	//Checks tick data and verifies it for neighbors
	private boolean checkTickData(String neighborRouterID) {
		HashSet<String> validRouters = new HashSet<String>();
		if (tickData.get(tick) != null)
			validRouters.addAll(tickData.get(tick));
		if (tickData.get(tick - 1) != null)
			validRouters.addAll(tickData.get(tick - 1));

		validRouters.add(this.routerID);

		if (validRouters.contains(neighborRouterID))
			return true;
		return false;
	}

	//Adjusts graph for obsolete ticks
	public void adjustRoutingTables() {
		HashSet<String> validRouters = new HashSet<String>();
		if (tickData.get(tick) != null)
			validRouters.addAll(tickData.get(tick));
		if (tickData.get(tick - 1) != null)
			validRouters.addAll(tickData.get(tick - 1));

		validRouters.add(this.routerID);

		HashMap<String, HashMap<String, Integer>> graph2 = new HashMap<String, HashMap<String, Integer>>();

		for (String router : graph.keySet()) {
			if (validRouters.contains(router)) {
				graph2.put(router, new HashMap<String, Integer>());
				for (String router2 : graph.get(router).keySet()) {
					if (validRouters.contains(router2)) {
						graph2.get(router).put(router2, graph.get(router).get(router2));
					}
				}
			}
		}
		graph = graph2;
	}
}

//Class for LSP 
class LSP {
	String originRouterID;
	Integer sequenceNum;
	Integer TTL = 10;
	ArrayList<RoutingEntry> directConnections = new ArrayList<RoutingEntry>();
}

//Class to store routing entry
class RoutingEntry {
	String routerID;
	String networkName;
	String outgoingLink;
	Integer cost;
}