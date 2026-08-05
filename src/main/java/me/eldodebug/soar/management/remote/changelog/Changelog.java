package me.eldodebug.soar.management.remote.changelog;

public class Changelog {

	private String text;
	private ChangelogType type;
	
	public Changelog(String text, ChangelogType type) {
		this.text = text;
		this.type = type;
	}

	public String getText() {
		return text;
	}

	public ChangelogType getType() {
		return type;
	}
}
