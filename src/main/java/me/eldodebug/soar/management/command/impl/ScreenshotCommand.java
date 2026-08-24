package me.eldodebug.soar.management.command.impl;

import java.awt.Toolkit;
import java.io.File;

import me.eldodebug.soar.Glide;
import me.eldodebug.soar.management.command.Command;
import me.eldodebug.soar.management.file.FileManager;
import me.eldodebug.soar.utils.transferable.FileTransferable;
import me.eldodebug.soar.utils.file.FileUtils;
import net.minecraft.util.ChatComponentText;

public class ScreenshotCommand extends Command {

	public ScreenshotCommand() {
		super("screenshot");
	}

	@Override
	public void onCommand(String message) {
		String[] args = message.split(" ");
		if(args.length < 2) {
			return;
		}

		FileManager fileManager = Glide.getInstance().getFileManager();
		File file = new File(fileManager.getScreenshotDir(), args[1]);
		
		if(args[0].equals("open")) {
			FileUtils.openFile(file);
		}
		
		if(args[0].equals("copy")) {
            FileTransferable selection = new FileTransferable(file);
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(selection, null);
		}
		
		if(args[0].equals("del")) {
			file.delete();
			mc.ingameGUI.getChatGUI().printChatMessage(new ChatComponentText(args[1] + " has been deleted"));
		}
	}
}
