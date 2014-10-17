import java.io.Serializable;

/**
 * Represents a message sent between nodes.
 * */
public class Message implements Serializable {

	public enum Type {
		CHECK_ALIVE, CONFIRM_ALIVE, SERIALIZE, RECEIVE_MIGRATION, TERMINATE, CONNECT, SERIALIZATION_COMPLETE
	}

	private static final long serialVersionUID = 1L;

	protected String mSenderManagerID;

	private Type mMessageType;

	public Message(Type messageType) {
		mMessageType = messageType;
	}

	public Type getMessageType() {
		return mMessageType;
	}

	public void setSenderManagerID(String id) {
		mSenderManagerID = id;
	}

	public String getSenderManagerID() {
		return mSenderManagerID;
	}
}
