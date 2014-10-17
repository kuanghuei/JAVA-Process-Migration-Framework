import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

// Responsible for writing and reading data for the master
public class ServerSocketProcess implements Runnable {

	private Socket mClientSocket;
	private ObjectInputStream mObjectIS;
	private ObjectOutputStream mObjectOS;
	private SocketMessageListener mListener;

	private SocketReaderRunner mReader;

	public ServerSocketProcess(Socket socket, SocketMessageListener listener) {
		mClientSocket = socket;
		mListener = listener;
	}

	public synchronized void sendMessage(Message message) {
		try {
			mObjectOS.writeObject(message);
		} catch (IOException e) {
			// System.out.println("Error sending message: " + e.toString());
		}
	}

	public void suspend() {
		mReader.suspend();
	}

	@Override
	public void run() {
		try {
			mObjectOS = new ObjectOutputStream(mClientSocket.getOutputStream());
			mObjectOS.flush();
			mObjectIS = new ObjectInputStream(mClientSocket.getInputStream());

			mReader = new SocketReaderRunner(mObjectIS, mListener, this);
			new Thread(mReader).start();
		} catch (Exception e) {
		}
	}
}
