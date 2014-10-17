public class PrintNumberProcess extends MigratableProcess {

	private static final long serialVersionUID = 1L;

	private int mPrintCount;
	private int mPrintPeriod;

	// Used for storing state during suspend
	private int mCurrentPrintCount;
	private volatile boolean suspending;
	
	/**
	 * args[1] - this process will print from 1 up to and including this number
	 * args[2] - the period between each print in milliseconds
	 * */
	public PrintNumberProcess(String[] args) throws Exception {
		super(args);

		mPrintCount = Integer.parseInt(args[0]);
		mPrintPeriod = Integer.parseInt(args[1]);
		mCurrentPrintCount = 0;
	}

	@Override
	void suspend() {
		suspending = true;
		while (suspending);
	}

	@Override
	void executeProcess() {

		while (mCurrentPrintCount < mPrintCount && !suspending) {
			System.out.println(mCurrentPrintCount + 1);
			mCurrentPrintCount++;

			try {
				Thread.sleep(mPrintPeriod);
			} catch (InterruptedException e) {
				System.out.println(e.toString());
			}
		}
		if (mCurrentPrintCount >= mPrintCount) System.out.println("");
		
		suspending = false;
	}

	@Override
	public String getName() {
		return "PrintNumberProcess";
	}
}
