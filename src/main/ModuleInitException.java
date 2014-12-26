package main;

/**
 * ModuleInitException
 *
 * @author Michael Mrozek
 *         Created Jun 15, 2009.
 */
public class ModuleInitException extends Exception {
	public ModuleInitException(String message) {super(message);}
	public ModuleInitException(String message, Throwable cause) {super(message, cause);}
	public ModuleInitException(Throwable cause) {super(cause);}
}
