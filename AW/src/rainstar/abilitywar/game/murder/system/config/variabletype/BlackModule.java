package rainstar.abilitywar.game.murder.system.config.variabletype;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.ScoreboardManager;
import org.bukkit.scoreboard.Team;
import org.bukkit.scoreboard.Team.Option;
import org.bukkit.scoreboard.Team.OptionStatus;

import daybreak.abilitywar.AbilityWar;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.list.murdermystery.ability.jobs.murderer.BlackMurderer;
import daybreak.abilitywar.game.list.murdermystery.event.PreConsumeGoldEvent;
import daybreak.abilitywar.game.module.ListenerModule;
import daybreak.abilitywar.game.module.ModuleBase;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.base.concurrent.SimpleTimer;

@ModuleBase(BlackModule.class)
public class BlackModule implements ListenerModule {
	
	private ScoreboardManager manager = Bukkit.getScoreboardManager();
	private Scoreboard board;
	private Team team;
	
	public BlackModule() {
		board = manager.getMainScoreboard();
		team = board.getTeam("BlackTeam");
	}
	
	public void register() {
		Bukkit.getPluginManager().registerEvents(this, AbilityWar.getPlugin());
		
		if (team == null) {
			team = board.registerNewTeam("BlackTeam");
		}
		
		team.setOption(Option.NAME_TAG_VISIBILITY, OptionStatus.NEVER);
	}
	
	public void unregister() {
		HandlerList.unregisterAll(this);
		if (team != null) {
			team.unregister();
		}
	}

	@EventHandler
	public void onPreConsumeGoldEvent(PreConsumeGoldEvent e) {
		if (e.getAmount() == 5 && e.getParticipant().getAbility().getClass().equals(BlackMurderer.class) && !e.isCancelled()) {
			for (Participant p : e.getParticipant().getGame().getParticipants()) {
				p.getPlayer().setScoreboard(board);
				team.addEntry(p.getPlayer().getName());
				new Remover(p.getPlayer()).start();
			}
		}
	}
	
	class Remover extends SimpleTimer {
		
		private Player player;
		
		public Remover(Player player) {
			super(TaskType.REVERSE, 90);
			setPeriod(TimeUnit.TICKS, 1);
			this.player = player;
		}
		
		@Override
		public void onEnd() {
			onSilentEnd();
		}
		
		@Override
		public void onSilentEnd() {
			team.removeEntry(player.getName());
		}
		
	}
	
}