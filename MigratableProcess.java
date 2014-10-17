import java.io.Serializable;

/**
 * 
 * All subclasses need to implement the "executeProcess" template method used in
 * "run". After execution, observers will be notified that the process has
 * ended.
 * 
 * Note: executeProcess is expected to check the value of the member variable
 * mSuspended to determined whether the process is interrupted and should be
 * stopped.
 * 
 * */
public abstract class MigratableProcess implements Serializable, Runnable {

	private static final long serialVersionUID = 1L;

	public static enum State {
		RUNNING, FINISHED, CREATED, SUSPENDED
	}

	protected boolean mSuspended;
	protected String[] mArgs;
	private String mID;
	private transient ProcessObserver mObserver;

	public MigratableProcess(String[] args) {
		mSuspended = false;
		mArgs = args;
		mID = Utils.getNextProcessID();
	}

	@Override
	public void run() {
		// This uses the template method design pattern: subclasses provide the
		// executeProcess function. Observer is notified of ending afterwards.
		mSuspended = false;
		executeProcess();

		// Notify that the process has finished only if it was not suspended
		if (!mSuspended) {
			mObserver.onProcessEnded(this);
		}
	}

	@Override
	public String toString() {
		StringBuilder stringBuilder = new StringBuilder();
		stringBuilder.append(getID());
		stringBuilder.append("\n");
		stringBuilder.append("args: ");

		for (String s : mArgs) {
			stringBuilder.append(s + " ");
		}
		stringBuilder.append("\n");
		return stringBuilder.toString();
	}

	public String getID() {
		return mID;
	}

	public void setObserver(ProcessObserver observer) {
		mObserver = observer;
	}

	public void removeObserver() {
		mObserver = null;
	}

	abstract void executeProcess();

	abstract void suspend();

	abstract String getName();
}
