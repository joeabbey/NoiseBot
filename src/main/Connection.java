package main;

/**
 * Connection
 *
 * @author Michael Mrozek
 *         Created Oct 3, 2010.
 */
public class Connection {
	public final String server, nick, password;
	public final int port;
	public final String[] fixedModules;

	public Connection(String server, int port, String nick, String password, String[] fixedModules) {
		this.server = server;
		this.port = port;
		this.nick = nick;
		this.password = password;
		this.fixedModules = fixedModules;
	}
}
