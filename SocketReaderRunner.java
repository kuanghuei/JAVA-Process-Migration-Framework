import java.io.ObjectInputStream;

public class SocketReaderRunner implements Runnable {

	private boolean mSuspended;
	private ObjectInputStream mObjectIS;
	private SocketMessageListener mListener;
	private ServerSocketProcess mProc;

	public SocketReaderRunner(ObjectInputStream objectIS,
			SocketMessageListener listener, ServerSocketProcess proc) {
		mSuspended = false;
		mObjectIS = objectIS;
		mListener = listener;
		mProc = proc;
	}

	public void suspend() {
		mSuspended = true;
	}

	@Override
	public void run() {
		// Loop to read message
		while (!mSuspended) {
			try {
				Message message = (Message) mObjectIS.readObject();
				mListener.onMessageReceived(message, mProc);
			} catch (Exception e) {
				break;
			}
		}
	}
}
