// Observer pattern to get notified when a process has ended.
public interface ProcessObserver {

	public void onProcessEnded(MigratableProcess process);

}
