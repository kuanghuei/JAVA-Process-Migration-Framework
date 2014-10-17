import java.io.BufferedReader;
import java.io.FileReader;
import java.util.Hashtable;

public class Utils {
	private static int sNextProcessID = 0;

	// Returns the next unique process ID
	public static synchronized String getNextProcessID() {
		sNextProcessID++;
		return "p" + sNextProcessID;
	}

	// Shows sample usage
	public static void printSampleUsage() {
		System.out.println("Usage:");
		System.out.println("\tcreate   processClassName  processArgs");
		System.out.println("\tstart    processID         managerID");
		System.out.println("\tmigrate  processID         managerID");
		System.out.println("\tstatus");
		System.out.println("\texit\n");
	}

	// Shows status
	public static void printStatus(MasterController masterController) {
		System.out.println("\nNODE STATUS");
		System.out.println("---------------------");
		for (String managerID : masterController.mManagerIDToAliveMap.keySet()) {
			boolean isAlive = masterController.mManagerIDToAliveMap
					.get(managerID);
			String statusString = isAlive ? "Healthy" : "Unresponsive";
			System.out.println("Manager " + managerID + ": " + statusString);
		}

		System.out.println("\nPROCESS STATUS");
		System.out.println("---------------------");
		for (String procID : masterController.mProcIDToStateMap.keySet()) {
			String managerID = masterController.mProcIDToManagerIDMap
					.get(procID);
			MigratableProcess.State state = masterController.mProcIDToStateMap
					.get(procID);
			System.out.println(procID + " is on " + managerID + "\tstate: "
					+ state.toString() + "\tclass: "
					+ masterController.mProcIDToProcessNameMap.get(procID));
		}
		System.out.println("");
	}

	// Returns a list of host names and their ports from the file hosts.txt
	public static Hashtable<String, Integer> getNodeToPortMap() {
		Hashtable<String, Integer> hostPortMap = new Hashtable<String, Integer>();

		try {
			FileReader fileReader = new FileReader("hosts.txt");
			BufferedReader reader = new BufferedReader(fileReader);
			String line = reader.readLine();

			while (line != null) {
				String[] tokens = line.split(" ");
				hostPortMap.put(tokens[0], Integer.parseInt(tokens[1]));
				line = reader.readLine();
			}
			reader.close();
		} catch (Exception e) {
			System.out.println(e.toString());
		}
		return hostPortMap;
	}

	// Determines whether an input is valid
	public static boolean sanityCheckInput(String[] tokens,
			MasterController masterController) {

		boolean isStart = tokens[0].equalsIgnoreCase("start");
		boolean isMigrate = tokens[0].equalsIgnoreCase("migrate");
		boolean isCommandValid = (isStart || isMigrate) && tokens.length == 3;

		if (isCommandValid) {
			String processID = tokens[1];
			String managerID = tokens[2];
			MigratableProcess.State processState = getProcessState(processID,
					masterController);

			if (!containsProcess(processID, masterController)) {
				System.out.println(processID + " not found");
				return false;
			} else if (!containsManager(managerID, masterController)) {
				System.out.println(managerID + " not found");
				return false;
			} else if (!isManagerAlive(managerID, masterController)) {
				System.out
						.println(managerID
								+ " is not responsive right now, please try again later.");
				return false;
			} else if (processState != MigratableProcess.State.CREATED
					&& isStart) {
				System.out.println(processID + " is already " + processState
						+ ", use migrate instead.");
				return false;
			} else if (processState == MigratableProcess.State.FINISHED
					&& isMigrate) {
				System.out.println(processID + " is already " + processState
						+ ", cannot migrate.");
				return false;
			} else if (getCurrentManagerID(processID, masterController).equals(
					managerID)
					&& isMigrate) {
				System.out.println(processID + " is already on " + managerID
						+ ", cannot migrate.");
				return false;
			}
		}

		return true;
	}

	public static String getCurrentManagerID(String processID,
			MasterController masterController) {
		return masterController.mProcIDToManagerIDMap.get(processID);
	}

	public static boolean containsProcess(String processID,
			MasterController masterController) {
		return masterController.mProcIDToStateMap.containsKey(processID);
	}

	public static boolean containsManager(String managerID,
			MasterController masterController) {
		return managerID.equals(MasterController.LOCAL_MANAGER_ID)
				|| masterController.mManagerIDToSocketMap
						.containsKey(managerID);
	}

	public static boolean isManagerAlive(String managerID,
			MasterController masterController) {
		if (managerID.equals(MasterController.LOCAL_MANAGER_ID))
			return true;
		if (!masterController.mManagerIDToAliveMap.containsKey(managerID))
			return false;
		return masterController.mManagerIDToAliveMap.get(managerID);
	}

	public static MigratableProcess.State getProcessState(String processID,
			MasterController masterController) {
		return masterController.mProcIDToStateMap.get(processID);
	}
}
