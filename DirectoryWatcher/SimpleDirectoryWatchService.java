package DirectoryWatcher;

import java.nio.file.WatchService;
import java.util.concurrent.atomic.AtomicBoolean;

import java.io.IOException;
import java.nio.file.*;
import java.util.Arrays;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

import static java.nio.file.StandardWatchEventKinds.*;

public class SimpleDirectoryWatchService implements DirectoryWatchService, Runnable {
	
	private final WatchService mWacthService;
	private final AtomicBoolean mIsRunnign;
	private final ConcurrentMap<WatchKey, Path> mWatchMap;
	private final ConcurrentMap<Path, Set<OnFileChangeListener>> mDirListener;
	private final ConcurrentMap<OnFileChangeListener, Set<PathMatcher>> mListenerFile;

	public SimpleDirectoryWatchService() throws IOException {
		
		mWacthService = FileSystems.getDefault().newWatchService();
		mIsRunnign = new AtomicBoolean(false);
		mWatchMap = newConcurrentMap();
		mDirListener = newConcurrentMap();
		mListenerFile = newConcurrentMap();
	}
	
	 @SuppressWarnings("unchecked")
	 private static <T> WatchEvent<T> cast( WatchEvent<?> event ) {
		 return (WatchEvent<T>)event;
	 }
	 
	 private static <K, V> ConcurrentMap<K, V> newConcurrentMap() {
		 return new ConcurrentHashMap<>();
	 }
	 
	 private static <T> Set<T> newConcurrentSet() {
		 return Collections.newSetFromMap(newConcurrentMap());
	 }
	 
	 public static PathMatcher matcherForGlobExpression( String globalPattern ) {
		 return FileSystems.getDefault().getPathMatcher("glob:" + globalPattern);
	 }
	 
	 public static boolean matches( Path input, PathMatcher pattern ) {
		 return pattern.matches(input);
	 }
	 
	 public static boolean matchesAny( Path input, Set<PathMatcher> patterns ) {
		 
		 for ( PathMatcher pattern : patterns ) {
			 if ( matches(input, pattern) ) {
				 return true;
			 }
		 }
		 
		 return false;
	 }
	 
	 private Path getDirPath( WatchKey key ) {
		 return mWatchMap.get(key);
	 }
	 
	 private Set<OnFileChangeListener> getListeners(Path dir) {
		 return mDirListener.get(dir);
	 }

	 private Set<PathMatcher> getPatterns(OnFileChangeListener listener) {
		 return mListenerFile.get(listener);
	 }

	 private Set<OnFileChangeListener> matchedListeners(Path dir, Path file) {
		 return getListeners(dir)
	            .stream()
	            .filter(listener -> matchesAny(file, getPatterns(listener)))
	            .collect(Collectors.toSet());
	 }

	 private void notifyListeners(WatchKey key) {
		 for (WatchEvent<?> event : key.pollEvents()) {
			 	WatchEvent.Kind eventKind = event.kind();

				if (eventKind.equals(OVERFLOW)) {
				    // TODO: Notify all listeners.
				    return;
				}

		        WatchEvent<Path> pathEvent = cast(event);
		        Path file = pathEvent.context();

		        if (eventKind.equals(ENTRY_CREATE)) {
	                matchedListeners(getDirPath(key), file)
	                        .forEach(listener -> listener.onFileCreate(file.toString()));
	            } else if (eventKind.equals(ENTRY_MODIFY)) {
	                matchedListeners(getDirPath(key), file)
	                        .forEach(listener -> listener.onFileModify(file.toString()));
	            } else if (eventKind.equals(ENTRY_DELETE)) {
	                matchedListeners(getDirPath(key), file)
	                        .forEach(listener -> listener.onFileDelete(file.toString()));
	            }
	        }
	    }

     @Override
     public void register(OnFileChangeListener listener, String dirPath, String... globPatterns) throws IOException {
        
    	Path dir = Paths.get(dirPath);

        if (!Files.isDirectory(dir)) {
            throw new IllegalArgumentException(dirPath + " is not a directory.");
        }

        if (!mDirListener.containsKey(dir)) {
            // May throw
            WatchKey key = dir.register(
                    mWacthService, ENTRY_CREATE, ENTRY_MODIFY, ENTRY_DELETE
            );

            mWatchMap.put(key, dir);
            mDirListener.put(dir, newConcurrentSet());
        }

        getListeners(dir).add(listener);

        Set<PathMatcher> patterns = newConcurrentSet();

        for (String globPattern : globPatterns) {
            patterns.add(matcherForGlobExpression(globPattern));
        }

        if (patterns.isEmpty()) {
            patterns.add(matcherForGlobExpression("*")); // Match everything if no filter is found
        }

        mListenerFile.put(listener, patterns);
    }

     @Override
     public void start() {
        if (mIsRunnign.compareAndSet(false, true)) {
            Thread runnerThread = new Thread(this, DirectoryWatchService.class.getSimpleName());
            runnerThread.start();
        }
    }

     public void stop() {
        // Kill thread lazily
        mIsRunnign.set(false);
     }

     @Override
     public void run() {

    	 while (mIsRunnign.get()) {
            
    		 WatchKey key;
             try {
            	 key = mWacthService.take();
             } catch (InterruptedException e) {
                System.out.println( DirectoryWatchService.class.getSimpleName() + " service interrupted.");
                break;
            }

            if (null == getDirPath(key)) {
                System.out.println("Error - Watch key not recognized.");
                continue;
            }

            notifyListeners(key);

            // Reset key to allow further events for this key to be processed.
            boolean valid = key.reset();
            if (!valid) {
                mWatchMap.remove(key);
                if (mWatchMap.isEmpty()) {
                    break;
                }
            }
        }

	        mIsRunnign.set(false);
	        System.out.println("Stopping file watcher service.");
	}
	 
}
