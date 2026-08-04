package rainstar.abilitywar.system.command;

import java.util.Collections;
import java.util.List;

import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import daybreak.abilitywar.Command;
import daybreak.abilitywar.game.GameManager;
import daybreak.abilitywar.game.list.murdermystery.MurderMystery;
import daybreak.abilitywar.utils.base.TimeUtil.ParsedTime;
import rainstar.abilitywar.game.murder.system.config.variabletype.MurderTimer;

public class MurderTimeCommand extends Command implements TabCompleter {
	
	public MurderTimeCommand() {
		super(Condition.OP);
	}
    
    @Override
	protected boolean onCommand(CommandSender sender, String command, String[] args) {
		if (GameManager.isGameRunning() && GameManager.isGameOf(MurderMystery.class) && args.length >= 2) {
			MurderTimer timer = GameManager.getGame().getModule(MurderTimer.class);
			if (args[0].equals("add")) {
				timer.addcount(Integer.valueOf(args[1]).intValue());
			} else if (args[0].equals("set")) {
				timer.setcount(Integer.valueOf(args[1]).intValue());
			}
			final ParsedTime parsedTime = ParsedTime.parse(timer.getCount() / 20);
			sender.sendMessage(String.format("§4[§c!§4] §f시간을 §c%d§f분 §c%d§f초로 조정하였습니다.", parsedTime.minutes, parsedTime.seconds));
		}
		return true;
	}

	@Override
	public List<String> onTabComplete(CommandSender sender, org.bukkit.command.Command cmd, String label, String[] args) {
		return Collections.emptyList();
	}

}
