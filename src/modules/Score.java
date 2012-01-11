package modules;

import static org.jibble.pircbot.Colors.*;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import main.Message;
import main.NoiseBot;
import main.NoiseModule;

import static modules.Slap.slapUser;


/**
 * Score
 *
 * @author Arathald (Greg Jackson)
 *         Created January 10, 2012.
 */

public class Score extends NoiseModule implements Serializable {
	private static final String COLOR_POSITIVE = ""; // No color
	private static final String COLOR_NEGATIVE = RED;
	
	private Map<String, Integer> userScores = new HashMap<String, Integer>();
	private Map<String, String> aliasToUsername = new HashMap<String, String>();

	@Command("(.+)\\+\\+.*")
	public void incrementScore(Message message, String target) {
		if (this.isSameUser(target, message.getSender())) {
			this.bot.sendAction(slapUser(message.getSender()));
		} else {
			this.changeScore(target, 1);
		}
	}
	
	@Command("(.+)\\-\\-.*")
	public void decrementScore(Message message, String target) {
		this.changeScore(target, -1);
	}
	
	@Command("\\.score (.+)")
	public void getScore(Message message, String target) {
		String user = this.getUser(target);
		if (this.userScores.containsKey(user)) {
			Integer score = this.userScores.get(user);
			String color = (score >= 0 ? this.COLOR_POSITIVE : this.COLOR_NEGATIVE);
			this.bot.sendMessage(target + "'s score is " + color + score);
		} else {
			this.bot.sendMessage(target + " has no score");
		}
	}
	
	@Command("\\.score")
	public void getSelfScore(Message message) {
		this.getScore(message, message.getSender());
	}
	
	@Override public void onNickChange(String oldNick, String login, String hostname, String newNick) {
		if (!this.aliasToUsername.containsKey(newNick)) {
			this.aliasToUsername.put(newNick, getUser(oldNick));
			this.save();
		} 
	}
	
	@Override public String getFriendlyName() {return "Score";}
	@Override public String getDescription() {return "Keeps users' scores";}
	@Override public String[] getExamples() {
		return new String[] {
				"_nick_++ -- Increment _nick_'s score",
				"_nick_-- -- Decrement _nick_'s score",
				".score _nick_ -- Display _nick_'s score"
		};
	}
	
	private void changeScore(String nick, Integer amount) {
		String user = this.getUser(nick);
		Integer oldScore = (this.userScores.containsKey(user) ? this.userScores.get(user) : 0);
		this.userScores.put(user, oldScore + amount);
		this.save();
	}
	
	private String getUser(String nick) {
		if (this.aliasToUsername.containsKey(nick)) {
			return this.aliasToUsername.get(nick);
		} else {
			this.aliasToUsername.put(nick, nick);
			this.save();
			return nick;
		}
			
	}
	
	private boolean isSameUser(String nick1, String nick2) {
		return getUser(nick1).equals(getUser(nick2));
	}
}