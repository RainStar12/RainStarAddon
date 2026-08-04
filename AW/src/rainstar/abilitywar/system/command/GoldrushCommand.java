package rainstar.abilitywar.system.command;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

import org.bukkit.command.CommandSender;

import daybreak.abilitywar.Command;
import daybreak.abilitywar.game.GameManager;
import daybreak.abilitywar.utils.base.Messager;

public class GoldrushCommand extends Command {
	
	private final File fastgold;
	
	public GoldrushCommand(File fastgold) {
		super(Condition.OP);
		this.fastgold = fastgold;
	}
	
    protected List<String> onTabComplete(CommandSender sender, String[] args) {
        return Collections.emptyList();
    }

	@Override
	protected boolean onCommand(CommandSender sender, String command, String[] args) {
		if (GameManager.isGameRunning()) {
			Messager.sendErrorMessage(sender, "게임이 진행되는 도중에는 변경할 수 없습니다.");
		} else {
			if (fastgold.exists()) {
				fastgold.delete();
				sender.sendMessage("§3[§b!§3] §f골드 러시 옵션을 §c비활성화§f합니다.");
			} else {
				try {
					fastgold.createNewFile();
					sender.sendMessage("§3[§b!§3] §f골드 러시 옵션을 §a활성화§f합니다.");
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		return true;
	}
	
}
