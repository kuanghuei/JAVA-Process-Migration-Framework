import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.io.Serializable;

/**
 * 
 * @author khlee
 * 
 */

public class TransactionalFileOutputStream extends OutputStream implements
		Serializable {
	private static final long serialVersionUID = 3L;
	private String fileName;
	private long filePointer;

	private transient RandomAccessFile file;
	boolean migrated;

	public TransactionalFileOutputStream(String _fileName, boolean _migrated)
			throws IOException {
		this.migrated = _migrated;
		this.fileName = _fileName;
		this.filePointer = 0;
		file = openFile();
	}

	private RandomAccessFile openFile() throws IOException {
		RandomAccessFile fileOS = new RandomAccessFile(fileName, "rws");
		fileOS.seek(filePointer);
		return fileOS;
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
	public void write(int b) throws IOException {
		migrationCheck();
		file.write(b);
		filePointer++;
	}

	@Override
	public void write(byte[] b) throws IOException {
		migrationCheck();
		file.write(b);
		filePointer += b.length;
	}

	@Override
	public void write(byte[] b, int off, int len) throws IOException {
		migrationCheck();
		file.write(b, off, len);
		filePointer += len;
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
