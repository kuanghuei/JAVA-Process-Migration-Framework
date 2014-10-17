import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

// Used by slaves to report status to masters.
// Contains process state information.
public class ConfirmAliveMessage extends Message {

	private static final long serialVersionUID = 1L;

	public List<String> mProcIDs;
	public List<MigratableProcess.State> mStates;

	public ConfirmAliveMessage(
			HashMap<String, MigratableProcess.State> procIDToStateMap,
			String senderManagerID) {
		super(Message.Type.CONFIRM_ALIVE);

		mProcIDs = new ArrayList<String>();
		mStates = new ArrayList<MigratableProcess.State>();

		for (String id : procIDToStateMap.keySet()) {
			mProcIDs.add(id);
			mStates.add(procIDToStateMap.get(id));
		}

		mSenderManagerID = senderManagerID;
	}

	public String getSenderManagerID() {
		return mSenderManagerID;
	}
}
