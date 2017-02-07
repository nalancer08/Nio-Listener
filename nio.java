package com.appbuilders;

import java.io.IOException;

import DirectoryWatcher.DirectoryWatchService;
import DirectoryWatcher.SimpleDirectoryWatchService;

public class nio {
	
	public static void main(String[] args) throws Exception {

		DirectoryWatchService watchService = null;
		String parent_path = "C:/Users/user01/Documents/folder";
		
		try {
			watchService = new SimpleDirectoryWatchService();
			watchService.register( // May throw
                new DirectoryWatchService.OnFileChangeListener() {
                    @Override
                    public void onFileCreate(String filePath) {
                        // File created
                        System.out.println( "Se creo archivo: " + filePath );
                    }

                    @Override
                    public void onFileModify(String filePath) {
                        // File modified
                        System.out.println( "Cambio archivo: " + filePath );

                        //if (filePath == "mex")
                    }

                    @Override
                    public void onFileDelete(String filePath) {
                        // File deleted
                        System.out.println( "Se borro archivo: " + filePath );
                    }
                }, parent_path
			);
	        
	        watchService.start();
        
	    } catch (IOException e) {
	        System.out.println("Unable to register file change listener for");
	    }

		while (true) {
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				watchService.stop();
				break;
			}
		};		
	}
}
