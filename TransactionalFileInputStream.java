import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;

/**
 * 
 * @author khlee
 * 
 */

public class TransactionalFileInputStream extends InputStream implements
		Serializable {

	private static final long serialVersionUID = 1L;
	private String fileName;
	private long filePointer;

	private transient FileInputStream file;
	private boolean migrated;

	public TransactionalFileInputStream(String _fileName, boolean _migrated) throws IOException {
		this.migrated = true;
		this.fileName = _fileName;
		this.filePointer = 0;
		file = openFile();
	}

	private FileInputStream openFile() throws IOException {
		FileInputStream fileIS = new FileInputStream(fileName);
		fileIS.skip(filePointer);
		return fileIS;
	}

	public void setMigrated(boolean _migrated) {
		migrated = _migrated;
	}

	public void closeFile() throws IOException {
		file.close();
	}

	public long getFilePointer() {
		return filePointer;
	}

	@Override
	public int read() throws IOException {
		migrationCheck();
		int ret = file.read();

		if (ret != -1)
			filePointer += 1;

		return ret;
	}

	@Override
	public int read(byte[] b) throws IOException {
		migrationCheck();
		int ret = file.read(b);

		if (ret != -1)
			filePointer += ret;

		return ret;
	}

	@Override
	public int read(byte[] b, int off, int len) throws IOException {
		migrationCheck();
		int ret = file.read(b, off, len);

		if (ret != -1)
			filePointer += ret;

		return ret;
	}

	@Override
	public long skip(long n) {
		if (n <= 0)
			return 0;
		filePointer += n;
		return n;
	}

	/**
	 * Reopens the file if the process has been migrated
	 * */
	private void migrationCheck() throws IOException {
		if (migrated) {
			file = openFile();
			migrated = false;
		}
	}

}
