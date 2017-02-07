package DirectoryWatcher;

import java.io.IOException;

public interface DirectoryWatchService extends Service {

	@Override
	default void start() throws Exception {
		// TODO Auto-generated method stub
		
	}

	@Override
	default void stop() {
		// TODO Auto-generated method stub
		
	}
	
	/*
	 * @param listener =  The listener
	 * @param path = The directory path
	 * @param patterns = Zero or more
	 * @throws IOException = If @path is not a directory
	 */
	void register( OnFileChangeListener listener, String path, String... patterns ) throws IOException;
	
	/*
	 * Methods for catch the file changes
	 */
	interface OnFileChangeListener {
		
		/*
		 * Called when a file it's created
		 * @param filePath = The file path.
		 */
		default void onFileCreate( String filePath ) {}
		
		/*
		 * Called when the file is modified
		 * @param filePath = The file path
		 */
		default void onFileModify( String filePath ) {}
		
		/*
		 * Called when the file is deleted
		 * @param filePath = The file path
		 */
		default void onFileDelete( String filePath ) {}
	}
	

}
