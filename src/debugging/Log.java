package debugging;

import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * Log
 *
 * @author Michael Mrozek
 *         Created Dec 19, 2010.
 */
public class Log {
	// 	DEBUG, VERBOSE, INFO, WARN, ERROR;

	public static void d(String msg, Object... args) {Debugger.me.log(Level.DEBUG, String.format(msg, args));}
	public static void v(String msg, Object... args) {Debugger.me.log(Level.VERBOSE, String.format(msg, args));}
	public static void i(String msg, Object... args) {Debugger.me.log(Level.INFO, String.format(msg, args));}
	public static void w(String msg, Object... args) {Debugger.me.log(Level.WARN, String.format(msg, args));}
	public static void e(String msg, Object... args) {Debugger.me.log(Level.ERROR, String.format(msg, args));}

	public static void d(Throwable e) {d("%s", parseThrowable(e));}
	public static void v(Throwable e) {v("%s", parseThrowable(e));}
	public static void i(Throwable e) {i("%s", parseThrowable(e));}
	public static void w(Throwable e) {w("%s", parseThrowable(e));}
	public static void e(Throwable e) {e("%s", parseThrowable(e));}

	public static void in(String text) {i("%s", text); Debugger.me.in.add(text);}
	public static void out(String text) {i("%s", text); Debugger.me.out.add(text);}

	private static String parseThrowable(Throwable e) {
		final StringWriter sw = new StringWriter();
		final PrintWriter pw = new PrintWriter(sw);
		e.printStackTrace(pw);
		pw.flush();
		sw.flush();

		return sw.toString();
	}
}
