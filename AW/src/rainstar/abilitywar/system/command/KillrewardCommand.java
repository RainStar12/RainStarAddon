package rainstar.abilitywar.system.command;

import java.util.Collections;
import java.util.List;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import daybreak.abilitywar.AbilityWar;
import daybreak.abilitywar.Command;
import rainstar.abilitywar.system.killreward.KillRewardGUI;

public class KillrewardCommand extends Command {
	
	public KillrewardCommand() {
		super(Condition.OP);
	}
	
    protected List<String> onTabComplete(CommandSender sender, String[] args) {
        return Collections.emptyList();
    }
    
	@Override
	protected boolean onCommand(CommandSender sender, String command, String[] args) {
		if (sender instanceof Player) {
			Player opener = (Player) sender;
			new KillRewardGUI(opener, AbilityWar.getPlugin());
		}
		return true;
	}

}
