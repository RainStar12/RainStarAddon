package rainstar.aw.synergy.chance;

import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.config.ability.AbilitySettings.SettingObject;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.list.mix.synergy.Synergy;
import daybreak.abilitywar.utils.base.concurrent.SimpleTimer.TaskType;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.base.minecraft.version.ServerVersion;
import daybreak.abilitywar.utils.library.SoundLib;
import kotlin.ranges.RangesKt;
import org.bukkit.Bukkit;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;

import java.util.HashSet;
import java.util.Set;

@AbilityManifest(name = "기회", rank = Rank.L, species = Species.GOD, explain = {
		"게임 시작 $[READY_COUNT]초 후 시너지 능력의 모든 플레이어는 세 가지 선택지 중",
		"하나를 택할 수 있는 GUI를 오픈합니다. 선택지에는 §a기존 능력§f, §b무작위 능력§f,",
		"§7능력명과 능력 설명이 보이지 않는 무작위 능력§f이 존재하며",
		"플레이어는 §a30§f초 내에 능력을 선택하여 나올 수 있습니다.",
		"기회에게는, 기존 능력 선택지가 없고 대신 무작위 2개와 블라인드 1개가 더 생깁니다."
		})

public class Chance extends Synergy {
	
	public Chance(Participant participant) {
		super(participant);
	}
	
	public static final SettingObject<Integer> READY_COUNT = 
			synergySettings.new SettingObject<Integer>(Chance.class, "ready-count", 30,
            "# 게임 시작 후 대기하는 시간", "# 단위: 초") {
        @Override
        public boolean condition(Integer value) {
            return value >= 0;
        }
    };
	
	@Override
	protected void onUpdate(Update update) {
		if (update == Update.RESTRICTION_CLEAR) {
	    	if (onetime) {
				chancewait.start();
	    		onetime = false;
	    	}
		}
		if (update == Update.ABILITY_DESTROY) {
			if (getGame().hasModule(ChanceBossbarModule.class)) getGame().getModule(ChanceBossbarModule.class).setCount(0);
		}
	}
	
	private BossBar bossBar = null;
	private Set<Player> addedPlayer = new HashSet<>();
	private boolean onetime = true;
	private final int readycount = READY_COUNT.getValue();
	
	private final AbilityTimer chancewait = new AbilityTimer(TaskType.REVERSE, readycount * 20) {
		
		@Override
		public void run(int count) {
			if (!getGame().hasModule(ChanceBossbarModule.class)) {
				getGame().addModule(new ChanceBossbarModule(getGame(), readycount));
	    		bossBar = Bukkit.createBossBar("§b§l기회§7", BarColor.WHITE, BarStyle.SOLID);
	    		bossBar.setProgress(1);
	    		for (Player player : Bukkit.getOnlinePlayers()) {
		    		if (!addedPlayer.contains(player)) {
		    			bossBar.addPlayer(player);
		    			addedPlayer.add(player);
		    		}
	    		}
	    		for (Participant participant : getGame().getParticipants()) {
	    			Player player = participant.getPlayer();
	    			if (!addedPlayer.contains(player)) {
		    			bossBar.addPlayer(player);
		    			addedPlayer.add(player);
		    		}
	    		}
	    		if (ServerVersion.getVersion() >= 10) bossBar.setVisible(true);
			}
			if (bossBar != null) {
	    		for (Player player : Bukkit.getOnlinePlayers()) {
		    		if (!addedPlayer.contains(player)) {
		    			bossBar.addPlayer(player);
		    			addedPlayer.add(player);
		    		}
	    		}
	    		for (Participant participant : getGame().getParticipants()) {
	    			Player player = participant.getPlayer();
	    			if (!addedPlayer.contains(player)) {
		    			bossBar.addPlayer(player);
		    			addedPlayer.add(player);
		    		}
	    		}
	    		bossBar.setProgress(RangesKt.coerceIn((double) count / (readycount * 20), 0, 1));	
			}
		}
		
		@Override
		public void onEnd() {
			if (!getGame().hasModule(AbilityChanceGUI.class)) {
				if (getGame().hasModule(ChanceBossbarModule.class)) getGame().getModule(ChanceBossbarModule.class).setCount(0);
				SoundLib.UI_TOAST_CHALLENGE_COMPLETE.broadcastSound();
				getGame().addModule(new AbilityChanceGUI(getGame()));
			}
			if (bossBar != null) bossBar.removeAll();
		}
		
		@Override
		public void onSilentEnd() {
			if (getGame().hasModule(ChanceBossbarModule.class)) getGame().getModule(ChanceBossbarModule.class).setCount(0);
			if (bossBar != null) bossBar.removeAll();
		}
		
	}.setBehavior(RestrictionBehavior.PAUSE_RESUME).setPeriod(TimeUnit.TICKS, 1).register();
	
}