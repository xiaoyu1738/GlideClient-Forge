package me.eldodebug.soar.management.command;

import java.util.ArrayList;

import me.eldodebug.soar.Glide;
import me.eldodebug.soar.management.command.impl.ScreenshotCommand;
import me.eldodebug.soar.management.command.impl.TranslateCommand;
import me.eldodebug.soar.management.event.EventTarget;
import me.eldodebug.soar.management.event.impl.EventSendChat;

public class CommandManager {

	private ArrayList<Command> commands = new ArrayList<Command>();
	
	public CommandManager() {
		
		commands.add(new ScreenshotCommand());
		commands.add(new TranslateCommand());
		
		Glide.getInstance().getEventManager().register(this);
	}
	
	@EventTarget
	public void onSendChat(EventSendChat event) {
		
		if(event.getMessage().startsWith(".soarcmd")) {
			
			event.setCancelled(true);
			
			String[] args = event.getMessage().split(" ");
			
			if(args.length > 1) {
				for(Command c : commands) {
					if(args[1].equals(c.getPrefix())) {
						c.onCommand(event.getMessage().replace(".soarcmd ", "").replace(args[1] + " ", ""));
					}
				}
			}
		}
	}

	public ArrayList<Command> getCommands() {
		return commands;
	}
}
