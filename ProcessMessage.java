// A type of message containing process ID. 
// Used to specify the process during migration and serialization.
public class ProcessMessage extends Message {

	private static final long serialVersionUID = 1L;

	private String mProcessID;

	public ProcessMessage(Type messageType, String processID) {
		super(messageType);
		mProcessID = processID;
	}

	public String getProcessID() {
		return mProcessID;
	}

}
