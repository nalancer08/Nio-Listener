package DirectoryWatcher;

public interface Service {
	
	/*
	 * Este metodo inicia el servicio. 
	 */
	void start() throws Exception;
	
	/*
	 * Para el servicio.
	 */
	void stop();
	
}
