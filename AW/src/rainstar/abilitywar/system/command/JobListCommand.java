package rainstar.abilitywar.system.command;

import java.util.Collections;
import java.util.List;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import daybreak.abilitywar.AbilityWar;
import daybreak.abilitywar.Command;
import daybreak.abilitywar.ability.AbilityFactory;
import daybreak.abilitywar.game.list.murdermystery.JobList;
import daybreak.abilitywar.utils.base.Formatter;
import daybreak.abilitywar.utils.base.Messager;
import daybreak.abilitywar.utils.base.language.korean.KoreanUtil;
import daybreak.abilitywar.utils.base.language.korean.KoreanUtil.Josa;
import rainstar.abilitywar.game.murder.system.JobListGUI;

public class JobListCommand extends Command {
	
	public JobListCommand() {
		super(Condition.PLAYER);
	}
	
    protected List<String> onTabComplete(CommandSender sender, String[] args) {
        return Collections.emptyList();
    }
    
    @Override
	protected boolean onCommand(CommandSender sender, String command, String[] args) {
		if (args.length == 0) {
			if (sender instanceof Player) new JobListGUI((Player) sender, AbilityWar.getPlugin()).openGUI(1); else Messager.sendErrorMessage(sender, "사용법 §7: §f/" + command + " abilities <능력>");
		} else {
			final String name = String.join(" ", args);
			if (AbilityFactory.isRegistered(name) && (JobList.isRegistered(name) || AbilityFactory.isRegistered(name))) {
				for (String line : Formatter.formatInfo(AbilityFactory.getByName(name))) {
					sender.sendMessage(line);
				}
			} else Messager.sendErrorMessage(sender, name + KoreanUtil.getJosa(name, Josa.은는) + " 존재하지 않는 직업입니다.");
		}
		return true;
	}

}
