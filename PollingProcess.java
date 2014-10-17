// A process that sends polling messages from the master every POLLING_PERIOD.
public class PollingProcess implements Runnable {

	private static final int POLLING_PERIOD = 1000;

	private MasterController mController;
	private boolean mSuspended;

	public PollingProcess(MasterController controller) {
		mController = controller;
		mSuspended = false;
	}

	public void suspend() {
		mSuspended = true;
	}

	public void run() {
		while (!mSuspended) {
			mController.sendPollMessages();

			try {
				Thread.sleep(POLLING_PERIOD);
			} catch (Exception e) {
				break;
			}
			mController.updateAliveStatus();
		}
		System.out.println("Polling process terminated.");
	}
}
