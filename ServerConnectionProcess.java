import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

// A process that continuously accepts new socket connections and notifies the master
public class ServerConnectionProcess implements Runnable {

	private boolean mSuspended;
	private int mServerPort;
	private MasterController mMasterController;
	private ServerSocket mServerSocket;

	public ServerConnectionProcess(int hostPort,
			MasterController masterController) {
		mSuspended = false;
		mServerPort = hostPort;
		mMasterController = masterController;
	}

	public void suspend() {
		mSuspended = true;
		try {
			mServerSocket.close();
		} catch (IOException e) {
		}
	}

	@Override
	public void run() {
		try {
			mServerSocket = new ServerSocket(mServerPort);

			// Continue to accept connections and notify master
			while (!mSuspended) {
				Socket clientSocket = mServerSocket.accept();
				mMasterController.onClientConnected(clientSocket);
			}
		} catch (IOException e) {
			// Do nothing, service is not closed
		}
		System.out.println("Server socket process terminated.");
	}
}
