import java.io.PrintStream;
import java.io.EOFException;
import java.io.DataInputStream;
import java.io.IOException;
import java.lang.Thread;
import java.lang.InterruptedException;

/**
 * ReverseLines - reverse permutation of words of each line in file
 * 
 * @author khlee
 * 
 */

public class ReverseLines extends MigratableProcess {
	private static final long serialVersionUID = 1L;
	private TransactionalFileInputStream inFile;
	private TransactionalFileOutputStream outFile;
	private int mPrintPeriod;

	public ReverseLines(String args[]) throws Exception {
		super(args);

		if (args.length != 3) {
			System.out
					.println("usage: ReverseLines <inputFile> <outputFile> <break after eafach print (ms)>");
			throw new Exception("Invalid Arguments");
		}

		inFile = new TransactionalFileInputStream(args[0], true);
		outFile = new TransactionalFileOutputStream(args[1], true);
		mPrintPeriod = Integer.parseInt(args[2]);
	}

	@Override
	void executeProcess() {
		PrintStream out = new PrintStream(outFile);
		DataInputStream in = new DataInputStream(inFile);

		try {
			while (!mSuspended) {
				String line = in.readLine();
				if (line == null) {
					break;
				}
				String reversedLine = reverseWords(line);
				out.println(reversedLine);
				System.out.println(reversedLine);
				try {
					Thread.sleep(mPrintPeriod);
				} catch (InterruptedException e) {
					// ignore it
				}
			}
		} catch (EOFException e) {
			// End of File
		} catch (IOException e) {
			System.out.println("ReverseSentences: Error: " + e);
		}
	}

	public String reverseWords(String s) {
		String parts[] = s.trim().split("\\s+");
		String out = "";
		for (int i = parts.length - 1; i > 0; i--) {
			out += parts[i] + " ";
		}
		out += parts[0];

		return out;
	}

	@Override
	public String getName() {
		return "ReverseSentences";
	}

	@Override
	void suspend() {
		mSuspended = true;
		inFile.setMigrated(true);
		outFile.setMigrated(true);
	}

}