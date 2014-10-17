import java.io.PrintStream;
import java.util.Arrays;

/**
 * PrintPrimeNumberToNProcess - print all prime numbers up to n
 * 
 * @author khlee
 * 
 */

public class PrintPrime extends MigratableProcess {

	private static final long serialVersionUID = 1L;

	private int mPrintPeriod;
	private boolean[] primes;
	private int n;
	private int i;

	private TransactionalFileOutputStream outFile;

	public PrintPrime(String[] args) throws Exception {
		super(args);

		if (args.length != 3) {
			System.out
					.println("usage: PrintPrimeNumberToNProcess <n> <break after each print (ms)> <outputFile>");
			throw new Exception("Invalid Arguments");
		}

		n = Integer.parseInt(args[0]);
		mPrintPeriod = Integer.parseInt(args[1]);
		outFile = new TransactionalFileOutputStream(args[2], true);
		primes = new boolean[n + 1];
		Arrays.fill(primes, true);
		primes[0] = primes[1] = false;
		i = 2;
	}

	public boolean isPrime(int n) {
		return primes[n];
	}

	@Override
	void executeProcess() {
		PrintStream out = new PrintStream(outFile);
		while (i < n && !mSuspended) {
			if (primes[i]) {
				for (int j = 2; i * j < primes.length; j++) {
					primes[i * j] = false;
				}
				System.out.println("PrintPrime: " + i);
				out.println(i);
				System.out.println("exe");
			}
			++i;
			try {
				Thread.sleep(mPrintPeriod);
			} catch (InterruptedException e) {
				System.out.println("PrintPrime: " + e.toString());
			}
		}
	}

	@Override
	void suspend() {
		mSuspended = true;
		outFile.setMigrated(true);
	}

	@Override
	public String getName() {
		return "PrintPrime";
	}
}
