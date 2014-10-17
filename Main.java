import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class Main {

	public static MasterController masterController;
	public static ProcessManager processManager;

	public static void main(String[] args) {

		if (args.length == 3) {
			String slaveName = args[0];
			String masterHostName = args[1];
			int masterSocketPort = Integer.parseInt(args[2]);
			processManager = new ProcessManager(slaveName, masterHostName,
					masterSocketPort);

		} else if (args.length == 1) {
			System.out.println("Demo started\n");

			// Initialization
			int portNumber = Integer.parseInt(args[0]);
			masterController = new MasterController(portNumber);

			// Begin accepting commands from user
			Utils.printSampleUsage();
			beginUserInteraction();

		} else {
			System.out
					.println("Master Usage: <port>\nSlave Usage: <slave name> <master hostname> <port>");
		}
	}

	private static void beginUserInteraction() {
		InputStreamReader isReader = new InputStreamReader(System.in);
		BufferedReader bufferRead = new BufferedReader(isReader);
		String line = "";

		// Just an infinite loop that reads lines and parses them
		while (line != null && !line.equalsIgnoreCase("exit")) {
			try {
				line = bufferRead.readLine();

				if (line != null) {

					// Run in a separate thread so user input is not blocked
					final String fline = line;
					new Thread(new Runnable() {
						@Override
						public void run() {
							parseCommand(fline);
						}
					}).start();
				}
			} catch (IOException e) {
				System.out.println(e.toString());
			}
		}
	}

	private static void parseCommand(String line) {

		String[] tokens = line.split(" ");
		if (!Utils.sanityCheckInput(tokens, masterController)) {
			return;
		}

		if (tokens[0].equalsIgnoreCase("create") && tokens.length >= 2) {
			// Create process
			int argLength = tokens.length - 2;
			String[] args = new String[argLength];
			for (int i = 0; i < argLength; i++) {
				args[i] = tokens[i + 2];
			}

			masterController.createProcess(tokens[1], args);

		} else if (tokens[0].equalsIgnoreCase("start") && tokens.length == 3) {
			// Start process
			masterController.migrateProcess(tokens[1], tokens[2]);

		} else if (tokens[0].equalsIgnoreCase("migrate") && tokens.length == 3) {
			// Migrate process
			masterController.migrateProcess(tokens[1], tokens[2]);

		} else if (tokens[0].equalsIgnoreCase("status") && tokens.length == 1) {
			// Print status
			masterController.printStatus();

		} else if (tokens[0].equalsIgnoreCase("exit") && tokens.length == 1) {
			// Terminate
			masterController.terminate();

		} else {
			System.out.println("Invalid command");
		}
	}
}
