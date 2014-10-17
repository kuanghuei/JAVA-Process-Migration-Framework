import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.HashMap;

public class ProcessManager implements ProcessObserver, SocketMessageListener {

	private HashMap<String, MigratableProcess> mProcIDToProcessMap;
	private HashMap<String, MigratableProcess.State> mProcIDToStateMap;
	private String mID;
	private SlaveSocketProcess mSlaveSocketProcess;

	private MasterController mMasterController;

	// Local process manager, does not use sockets
	public ProcessManager(String id, MasterController master) {
		mProcIDToProcessMap = new HashMap<String, MigratableProcess>();
		mProcIDToStateMap = new HashMap<String, MigratableProcess.State>();
		mID = id;
		mMasterController = master;
	}

	// Remote process manager, creates sockets
	public ProcessManager(String slaveID, String masterHostName,
			int masterSocketPort) {
		mProcIDToProcessMap = new HashMap<String, MigratableProcess>();
		mProcIDToStateMap = new HashMap<String, MigratableProcess.State>();
		mID = slaveID;

		mSlaveSocketProcess = new SlaveSocketProcess(masterHostName,
				masterSocketPort, this, mID);
		Thread thread = new Thread(mSlaveSocketProcess);
		thread.start();
	}

	// For local process manager only, stores a newly process
	public void addProcess(MigratableProcess process) {
		mProcIDToProcessMap.put(process.getID(), process);
		mProcIDToStateMap.put(process.getID(), MigratableProcess.State.CREATED);
	}

	private void startProcess(MigratableProcess process) {
		System.out.println(process.getID() + " started on " + mID);
		mProcIDToProcessMap.put(process.getID(), process);
		mProcIDToStateMap.put(process.getID(), MigratableProcess.State.RUNNING);
		process.setObserver(this);
		Thread thread = new Thread(process);
		thread.start();
	}

	private void serializeProcess(MigratableProcess process) {
		process.suspend();
		mProcIDToProcessMap.remove(process.getID());
		mProcIDToStateMap.remove(process.getID());

		try {
			TransactionalFileOutputStream fileOS = new TransactionalFileOutputStream(
					process.getID(), false);
			ObjectOutputStream objectOS = new ObjectOutputStream(fileOS);
			objectOS.writeObject(process);
			objectOS.close();
			fileOS.setMigrated(true);
			fileOS.close();
		} catch (Exception e) {
			System.out.println(e.toString());
		}
	}

	protected void receiveMigratedProcess(String processID) {
		try {
			TransactionalFileInputStream fileIS = new TransactionalFileInputStream(
					processID, false);
			ObjectInputStream objectIS = new ObjectInputStream(fileIS);
			MigratableProcess process = (MigratableProcess) objectIS
					.readObject();
			objectIS.close();
			fileIS.close();
			startProcess(process);
		} catch (Exception e) {
			System.out.println(e.toString());
		}
	}

	@Override
	public synchronized void onMessageReceived(Message message,
			ServerSocketProcess socketProcess) {
		Message.Type messageType = message.getMessageType();

		if (messageType == Message.Type.CHECK_ALIVE) {
			// Received a poll message, reply master with current status
			ConfirmAliveMessage reply = new ConfirmAliveMessage(
					mProcIDToStateMap, mID);
			sendMessage(reply);

		} else if (messageType == Message.Type.SERIALIZE) {
			// Serialize the specified process
			ProcessMessage processMessage = (ProcessMessage) message;
			MigratableProcess process = mProcIDToProcessMap.get(processMessage
					.getProcessID());
			serializeProcess(process);

			// Inform master serialization is done
			ProcessMessage completeMessage = new ProcessMessage(
					Message.Type.SERIALIZATION_COMPLETE, process.getID());
			sendMessage(completeMessage);

		} else if (messageType == Message.Type.RECEIVE_MIGRATION) {
			// A process has migrated to this node
			ProcessMessage processMessage = (ProcessMessage) message;
			receiveMigratedProcess(processMessage.getProcessID());

		} else {
			// Termination
			terminate();

		}
	}

	private void sendMessage(Message message) {
		if (mMasterController != null) {
			mMasterController.onMessageReceived(message, null);
		} else {
			mSlaveSocketProcess.sendMessage(message);
		}
	}

	@Override
	public void onProcessEnded(MigratableProcess process) {
		System.out.println(process.getID() + " ended on " + mID);
		mProcIDToProcessMap.put(process.getID(), process);
		mProcIDToStateMap
				.put(process.getID(), MigratableProcess.State.FINISHED);
	}

	public String getID() {
		return mID;
	}

	public HashMap<String, MigratableProcess.State> getProcIDToStateMap() {
		return mProcIDToStateMap;
	}

	public void terminate() {
		System.out.println("Process manager terminated");
		for (String procID : mProcIDToProcessMap.keySet()) {
			MigratableProcess process = mProcIDToProcessMap.get(procID);
			process.suspend();
		}

		if (mSlaveSocketProcess != null) {
			mSlaveSocketProcess.suspend();
		}
	}
}
