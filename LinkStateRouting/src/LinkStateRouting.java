import java.io.BufferedReader;
import java.io.FileReader;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Scanner;

public class LinkStateRouting {

	static HashMap<String, Router> routerList = new HashMap<String, Router>();
	static HashMap<String, String> routerIDNameMap = new HashMap<String, String>();
	static final Integer INVALID_TICK_COUNT = 2;

	public static void main(String[] args) {
		//Read file infile.dat
		if (!readFile()) {
			System.out.println("Error reading file. Terminating program.");
			return;
		}


		Scanner scan = new Scanner(System.in);
		String print = "Type C to continue or " + "\n     Q to quit or "
				+ "\n     P followed by router's ID to print routing table or "
				+ "\n     S followed by router's ID to shutdown router or "
				+ "\n     T followed by router's ID to restart router\n";
		System.out.println(print);
		String input;
		boolean exit = false;

		while (!exit) {
			input = scan.nextLine();
			Character c = input.length() == 0 ? ' ' : input.toLowerCase().charAt(0);

			switch (c) {
			case 'c':
				if (input.toLowerCase().equals("c")) {
					originatePacketsAllRouters();
					System.out.println("Continuing...");
				}
				else {
					System.out.println("Invalid entry! Try again.");
					System.out.println(print);
				}
				break;
			case 'q':
				if (input.toLowerCase().equals("q"))
					exit = true;
				else {
					System.out.println("Invalid entry! Try again.");
					System.out.println(print);
				}
				break;
			case 'p':
				for (String routerID : routerList.keySet()) {
					Router r = routerList.get(routerID);
					r.printRoutingTable();
				}
				break;
			case 's':
				String[] inputs = input.split("\\s+");
				if (inputs.length == 2 && inputs[0].toLowerCase().equals("s")) {
					if (shutdownRouter(inputs[1])) {
						originatePacketsAllRouters();
					} else {
						System.out.println("No such router.");
						System.out.println(print);
					}
				} else {
					System.out.println("Invalid entry! Try again.");
					System.out.println(print);
				}
				break;
			case 't':
				String[] inputs1 = input.split("\\s+");
				if (inputs1.length == 2 && inputs1[0].toLowerCase().equals("t")) {
					if (inputs1[1].length() >= 1 && startupRouter(inputs1[1]))
						originatePacketsAllRouters();
					else {
						System.out.println("No such router or invalid entry.");
						System.out.println(print);
					}
				} else {
					System.out.println("Invalid entry! Try again.");
					System.out.println(print);
				}
				break;
			default:
				System.out.println("Invalid entry! Try again.");
				System.out.println(print);
				break;
			}
		}

		System.out.println("\nGoodbye!");
		scan.close();
	}

	//Starts the given router
	private static boolean startupRouter(String router) {
		if (routerList.get(router) == null) {
			return false;
		} else {
			routerList.get(router).startup();
			return true;
		}
	}

	//Shuts down given router
	private static boolean shutdownRouter(String router) {
		if (routerList.get(router) == null) {
			return false;
		} else {
			routerList.get(router).shutdown();
			return true;
		}
	}

	//Initiates all routers to send LSP
	private static void originatePacketsAllRouters() {
		for (String rID : routerList.keySet()) {
			routerList.get(rID).updateTick();
		}
		for (String rID : routerList.keySet()) {
			routerList.get(rID).originatePacket();
		}
		adjustRoutingTables();
	}

	//Adjust routing table for unwanted tick data
	private static void adjustRoutingTables() {
		for (String rID : routerList.keySet()) {
			routerList.get(rID).adjustRoutingTables();
		}
	}

	
	//Reads file, creates routers and stores neighbor data for each router
	private static boolean readFile() {
		try {
			FileReader fr = new FileReader("infile.dat");
			BufferedReader br = new BufferedReader(fr);
			String line = null;
			Router router = null;

			while ((line = br.readLine()) != null) {

				if (line.charAt(0) == ' ' || line.charAt(0) == '\t') {
					String[] inputs = line.split("\\s+");
					String neighborID = inputs[1];
					try {
						router.neighborsCost.put(neighborID, Integer.parseInt(inputs[2]));
					} catch (Exception e) {
						router.neighborsCost.put(neighborID, 1);
					}
					if (router.tickData.get(router.tick) == null) {
						HashSet<String> tickrouters = new HashSet<>();
						tickrouters.add(neighborID);
						router.tickData.put(router.tick, tickrouters);
					} else {
						router.tickData.get(router.tick).add(neighborID);
					}
					RoutingEntry entry = new RoutingEntry();
					entry.routerID = neighborID;
					entry.outgoingLink = null;
					try {
						entry.cost = Integer.parseInt(inputs[2]);
					} catch (Exception e) {
						entry.cost = 1;
					}
					router.graph.get(router.routerID).put(neighborID, entry.cost);
				} else {
					String[] inputs = line.split("\\s+");
					router = new Router();
					router.routerID = inputs[0];
					router.networkName = inputs[1];
					router.graph.put(inputs[0], new HashMap<String, Integer>());
					routerList.put(inputs[0], router);
					routerIDNameMap.put(inputs[0], inputs[1]);
				}
			}

			br.close();
			return true;
		} catch (Exception e) {
			return false;
		}
	}
}
