import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

// Responsible for writing and reading data for the slave
public class SlaveSocketProcess implements Runnable {

	private String mServerHostName;
	private int mServerPort;
	private SocketMessageListener mListener;

	private SocketReaderRunner mReader;
	private ObjectOutputStream mObjectOS;

	private String mManagerID;

	public SlaveSocketProcess(String serverHostName, int serverPort,
			SocketMessageListener listener, String managerID) {
		mServerHostName = serverHostName;
		mServerPort = serverPort;
		mListener = listener;
		mManagerID = managerID;
	}

	public synchronized void sendMessage(Message message) {
		try {
			mObjectOS.writeObject(message);
		} catch (IOException e) {
			System.out.println("Error sending message: " + e.toString());
		}
	}

	public void suspend() {
		mReader.suspend();
	}

	@Override
	public void run() {
		try {
			// Setup socket for slave
			Socket socket = new Socket(mServerHostName, mServerPort);
			mObjectOS = new ObjectOutputStream(socket.getOutputStream());
			ObjectInputStream objectIS = new ObjectInputStream(
					socket.getInputStream());

			mReader = new SocketReaderRunner(objectIS, mListener, null);
			new Thread(mReader).start();

			// Send first message to let master know the id
			Message connectionMessage = new Message(Message.Type.CONNECT);
			connectionMessage.setSenderManagerID(mManagerID);
			sendMessage(connectionMessage);

		} catch (IOException e) {
			throw new RuntimeException("Error: " + e.toString());
		}

	}
}
