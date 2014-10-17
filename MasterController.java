import java.lang.reflect.Constructor;
import java.net.Socket;
import java.util.HashMap;
import java.util.List;

public class MasterController implements SocketMessageListener {

	public static final String LOCAL_MANAGER_ID = "LOCAL";

	private ProcessManager mLocalManager;

	public HashMap<String, MigratableProcess.State> mProcIDToStateMap;
	public HashMap<String, String> mProcIDToManagerIDMap;
	public HashMap<String, String> mProcIDToProcessNameMap;
	public HashMap<String, ServerSocketProcess> mManagerIDToSocketMap;
	public HashMap<String, Boolean> mManagerIDToAliveMap;

	// Process id to destination manager id
	public HashMap<String, String> mMigrationPending;

	private PollingProcess mPollIngProcess;
	private HashMap<String, Boolean> mConfirmsReceived;

	private ServerConnectionProcess mServerConnectionProcess;

	public MasterController(int portNumber) {
		mLocalManager = new ProcessManager(LOCAL_MANAGER_ID, this);

		mProcIDToStateMap = new HashMap<String, MigratableProcess.State>();
		mProcIDToManagerIDMap = new HashMap<String, String>();
		mProcIDToProcessNameMap = new HashMap<String, String>();
		mManagerIDToSocketMap = new HashMap<String, ServerSocketProcess>();
		mManagerIDToAliveMap = new HashMap<String, Boolean>();
		mMigrationPending = new HashMap<String, String>();

		mManagerIDToAliveMap.put(LOCAL_MANAGER_ID, true);

		// Process to poll status
		mPollIngProcess = new PollingProcess(this);
		mConfirmsReceived = new HashMap<String, Boolean>();

		// Process to accept connections from nodes
		mServerConnectionProcess = new ServerConnectionProcess(portNumber, this);

		new Thread(mServerConnectionProcess).start();
		new Thread(mPollIngProcess).start();
	}

	// Creates a process with reflection on the local manager without starting
	public void createProcess(String className, String[] args) {
		Object[] constructorArgs = { (Object[]) args };

		try {
			Constructor<?> processConstructor = Class.forName(className)
					.getConstructor(String[].class);

			// Create process and put on local process manager
			MigratableProcess process = (MigratableProcess) processConstructor
					.newInstance(constructorArgs);
			mLocalManager.addProcess(process);

			mProcIDToStateMap.put(process.getID(),
					MigratableProcess.State.CREATED);
			mProcIDToManagerIDMap.put(process.getID(), LOCAL_MANAGER_ID);
			mProcIDToProcessNameMap.put(process.getID(), process.getName());

			System.out.println("Created " + process.getName()
					+ "\t processID: " + process.getID());
		} catch (Exception e) {
			System.out.println("Error: " + e.toString());
		}
	}

	// Migrates a process
	public void migrateProcess(String processID, String managerID) {
		// Store process id to manager id in a table
		mMigrationPending.put(processID, managerID);

		// Serialize the process
		ProcessMessage migrateMessage = new ProcessMessage(
				Message.Type.SERIALIZE, processID);
		String currentManagerID = mProcIDToManagerIDMap.get(processID);
		sendMessageTo(currentManagerID, migrateMessage);
	}

	// Start the process on destination after serialization has completed
	private void onSerializationComplete(String processID) {
		String managerID = mMigrationPending.get(processID);
		mMigrationPending.remove(processID);

		ProcessMessage receiveMigrateMessage = new ProcessMessage(
				Message.Type.RECEIVE_MIGRATION, processID);
		sendMessageTo(managerID, receiveMigrateMessage);

		mProcIDToStateMap.put(processID, MigratableProcess.State.RUNNING);
		mProcIDToManagerIDMap.put(processID, managerID);
	}

	// Display process and cluster status
	public void printStatus() {
		Utils.printStatus(this);
	}

	private void sendMessageTo(String managerID, Message message) {
		if (managerID.equals(LOCAL_MANAGER_ID)) {
			mLocalManager.onMessageReceived(message, null);
		} else {
			ServerSocketProcess socket = mManagerIDToSocketMap.get(managerID);
			socket.sendMessage(message);
		}
	}

	public void updateAliveStatus() {
		for (String managerID : mManagerIDToAliveMap.keySet()) {
			boolean confirmationReceived = mConfirmsReceived
					.containsKey(managerID);
			mManagerIDToAliveMap.put(managerID, confirmationReceived);
		}
		mManagerIDToAliveMap.put(LOCAL_MANAGER_ID, true);
	}

	public void sendPollMessages() {
		mConfirmsReceived = new HashMap<String, Boolean>();

		for (String managerID : mManagerIDToSocketMap.keySet()) {
			ServerSocketProcess socket = mManagerIDToSocketMap.get(managerID);
			Message checkAlive = new Message(Message.Type.CHECK_ALIVE);
			socket.sendMessage(checkAlive);
		}

		// Add local state to global state
		HashMap<String, MigratableProcess.State> localProcIDToStateMap = mLocalManager
				.getProcIDToStateMap();
		for (String procID : localProcIDToStateMap.keySet()) {
			MigratableProcess.State state = localProcIDToStateMap.get(procID);
			mProcIDToManagerIDMap.put(procID, LOCAL_MANAGER_ID);
			mProcIDToStateMap.put(procID, state);
		}
	}

	@Override
	public void onMessageReceived(Message message,
			ServerSocketProcess socketProcess) {

		Message.Type type = message.getMessageType();
		String managerID = message.getSenderManagerID();

		if (type == Message.Type.CONNECT) {
			// Identify the manager id for a new process
			mManagerIDToSocketMap.put(message.getSenderManagerID(),
					socketProcess);
			mManagerIDToAliveMap.put(message.getSenderManagerID(), true);
			System.out.println("Established connection with " + managerID);

		} else if (type == Message.Type.CONFIRM_ALIVE) {
			ConfirmAliveMessage confirmMessage = (ConfirmAliveMessage) message;
			mConfirmsReceived.put(managerID, true);
			List<String> procIds = confirmMessage.mProcIDs;
			List<MigratableProcess.State> states = confirmMessage.mStates;

			for (int i = 0; i < procIds.size(); i++) {
				MigratableProcess.State state = states.get(i);
				String procID = procIds.get(i);
				mProcIDToManagerIDMap.put(procID, managerID);
				mProcIDToStateMap.put(procID, state);
			}
		} else if (type == Message.Type.SERIALIZATION_COMPLETE) {
			ProcessMessage processMessage = (ProcessMessage) message;
			onSerializationComplete(processMessage.getProcessID());
		}
	}

	public void onClientConnected(Socket clientSocket) {
		ServerSocketProcess process = new ServerSocketProcess(clientSocket,
				this);
		new Thread(process).start();
	}

	// Preparation for ending the program
	public void terminate() {
		for (String managerID : mManagerIDToSocketMap.keySet()) {
			ServerSocketProcess socket = mManagerIDToSocketMap.get(managerID);
			Message terminate = new Message(Message.Type.TERMINATE);
			socket.sendMessage(terminate);
			socket.suspend();
		}
		mServerConnectionProcess.suspend();
		mPollIngProcess.suspend();
		mLocalManager.terminate();
	}
}
