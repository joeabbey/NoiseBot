package main;

import com.google.gson.internal.StringMap;
import debugging.Log;
import org.jibble.pircbot.User;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import static main.Utilities.sleep;

/**
 * IRCNoiseBot
 *
 * @author Michael Mrozek
 *         Created December 31, 2015.
 */
public class IRCNoiseBot extends NoiseBot {
	private final IRCServer server;
	private final Vector outQueue; // This is controlled by the "server"'s parent PircBot instance

	public IRCNoiseBot(IRCServer server, String channel, boolean quiet) {
		super(channel, quiet, server.getConnection().fixedModules);
		this.server = server;

		Vector outQueue = null;
		try {
			// I laugh at abstractions
			final Field outQueueField = this.server.getClass().getSuperclass().getDeclaredField("_outQueue");
			outQueueField.setAccessible(true);
			final org.jibble.pircbot.Queue _outQueue = (org.jibble.pircbot.Queue)outQueueField.get(this.server);

			// I laugh at them further
			// Also, implementing a queue using a vector is terrible
			final Field queueField = _outQueue.getClass().getDeclaredField("_queue");
			queueField.setAccessible(true);
			outQueue = (Vector)queueField.get(_outQueue);
		} catch(NoSuchFieldException | IllegalAccessException e) {
			Log.e(e);
		}
		this.outQueue = outQueue;
	}

	static void createBots(String connectionName, StringMap data) throws IOException {
		final String host = (String)data.get("server");
		final int port = (int)Double.parseDouble("" + data.get("port"));
		final String nick = (String)data.get("nick");
		final String pass = data.containsKey("password") ? (String)data.get("password") : null;
		final boolean quiet = data.containsKey("quiet") ? (Boolean)data.get("quiet") : false;
		final String[] modules = data.containsKey("modules") ? ((List<String>)data.get("modules")).toArray(new String[0]) : null;
		final IRCServer server = new IRCServer(new Connection(host, port, nick, pass, modules));

		final List<String> channels = (List<String>)data.get("channels");
		for(String channel : channels) {
			final NoiseBot bot = new IRCNoiseBot(server, channel, quiet);
			NoiseBot.bots.put(connectionName + channel, bot);
			server.addBot(channel, bot);
		}

		if(!server.connect()) {
			throw new IOException("Unable to connect to server");
		}
	}

	// Note: 'exitCode' is only applicable if this is the last connection
	@Override public void quit(int exitCode) {
		// Wait (for a little while) for outgoing messages to be sent
		if(this.outQueue != null) {
			for(int tries = 0; tries < 5 && !this.outQueue.isEmpty(); tries++) {
				sleep(1);
			}
		}

		if(this.server.getChannels().length > 1) {
			Log.i("Parting " + this.channel);
			this.server.partChannel(this.channel);
		} else {
			Log.i("Disconnecting");
			this.server.disconnect();
		}

		super.quit(exitCode);
	}

	@Override public Protocol getProtocol() {
		return Protocol.IRC;
	}

	@Override public String getBotNick() {
		return this.server.getNick();
	}

	private User[] getUsers() {return this.server.getUsers(this.channel);}
	@Override public String[] getNicks() {
		return Arrays.stream(this.getUsers()).map(User::getNick).toArray(String[]::new);
	}

	@Override public boolean clearPendingSends() {
		if(this.outQueue == null) {
			return false;
		}

		synchronized(this.outQueue) {
			final Iterator iter = this.outQueue.iterator();
			while(iter.hasNext()) {
				final Object obj = iter.next();
				if(obj.toString().startsWith("PRIVMSG " + this.channel + " ")) {
					iter.remove();
				}
			}
		}
		return true;
	}

	@Override File getStoreDirectory() {
		final Connection conn = this.server.getConnection();
		return new File(STORE_DIRECTORY, String.format("%s@%s:%d%s", conn.nick, conn.server, conn.port, this.channel));
	}

	@Override public void whois(String nick, WhoisHandler handler) {
		handler.setNick(nick);
		handler.startWaiting();
		this.server.sendRawLine("WHOIS " + nick);
	}

	@Override public void sendMessage(String target, String message) {this.server.sendMessage(target, message);}
	@Override public void sendAction(String target, String message) {this.server.sendAction(target, message);}
	@Override public void sendNotice(String target, String message) {this.server.sendNotice(target, message);}
	@Override public void kickVictim(String victim, String reason) {this.server.kick(this.channel, victim, reason);}

	@Override public void sendTargetedMessageParts(final String target, final String separator, final String... parts) {
		final String whois = this.server.getWhoisString();
		if(whois == null) {
			this.whois(this.getBotNick(), new WhoisHandler() {
				@Override public void onResponse() {
					IRCNoiseBot.this.server.setWhoisString(String.format("%s!%s@%s", this.nick, this.username, this.hostname));
					IRCNoiseBot.this.sendTargetedMessageParts(target, separator, parts);
				}

				@Override public void onTimeout() {
					IRCNoiseBot.this.server.setWhoisString("");
					IRCNoiseBot.this.sendMessageParts(separator, parts);
				}
			});
			return;
		}

		final int maxLen = this.server.getMaxLineLength()
				- (whois.isEmpty() ? 100 : 1 + whois.length())
				- " PRIVMSG ".length()
				- this.channel.length()
				-  " :\r\n".length();

		final StringBuilder message = new StringBuilder();
		for(int i = 0; i < parts.length; i++) {
			final String part = (message.length() > 0 ? separator : "") + parts[i];
			if(message.length() + part.length() <= maxLen) {
				message.append(part);
			} else if(message.length() == 0) {
				this.sendMessage(target, COLOR_ERROR + "Message part too long to send");
				// Skip it
			} else {
				this.sendMessage(target, message.toString());
				message.setLength(0);
				i--; // Redo this piece
			}
		}
		if(message.length() > 0) {
			this.sendMessage(target, message.toString());
		}
	}
}
