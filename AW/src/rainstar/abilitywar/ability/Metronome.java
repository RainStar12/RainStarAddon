package rainstar.abilitywar.ability;

import static rainstar.abilitywar.system.config.RainStarAbilitySetting.rsAbilitySettings;

import java.text.DecimalFormat;

import org.bukkit.Bukkit;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import daybreak.abilitywar.ability.AbilityBase;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.ability.SubscribeEvent;
import daybreak.abilitywar.config.ability.AbilitySettings.SettingObject;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.AbstractGame.Participant.ActionbarNotification.ActionbarChannel;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.base.minecraft.version.ServerVersion;
import daybreak.abilitywar.utils.library.ParticleLib;
import daybreak.abilitywar.utils.library.SoundLib;

@AbilityManifest(name = "메트로놈", rank = Rank.S, species = Species.OTHERS, explain = {
		"적에게 공격을 시도할 때, 가장 마지막으로 가한 §3공격 시간§f에서",
		"현재 §3입힌 시간§f의 §b차§8(§7간격§8)§f가 이전의 §b차§8(§7간격§8)§f와 1초 이내로 동일하다면,",
		"§c공격력§f이 §e$[DAMAGE_INCREASE]%§f 증가합니다. §8(§7정확하다면 최대 5번 중첩§8)",
		"이는 최대 §c20§f번까지 중첩될 수 있으며, §c실패 시 초기화됩니다."
		})
public class Metronome extends AbilityBase {
	
	public Metronome(Participant participant) {
		super(participant);
	}
	
	public static final SettingObject<Double> DAMAGE_INCREASE = rsAbilitySettings.new SettingObject<Double>(Metronome.class,
			"damage-increase", 5.0, "# 스택당 공격력 증가량", "# 단위: %") {

		@Override
		public boolean condition(Double value) {
			return value >= 0;
		}
		
	};
	
	private BossBar bossBar = null;
	private int combo = 0, lastcount = 0, lastminus = 0;
	private double damageincrease = DAMAGE_INCREASE.getValue() * 0.01;
	private DecimalFormat df = new DecimalFormat("0.0");
	private ActionbarChannel ac = newActionbarChannel();
	
	@Override
	public void onUpdate(Update update) {
		if (update == Update.RESTRICTION_CLEAR) {
			counter.start();
		}
	}
	
	public AbilityTimer counter = new AbilityTimer() {
		
    	@Override
    	public void onStart() {
    		bossBar = Bukkit.createBossBar("§b콤보 §f| §c공격력 증가§7: §e0%", BarColor.GREEN, BarStyle.SEGMENTED_20);
    		bossBar.setProgress(combo);
    		bossBar.addPlayer(getPlayer());
    		if (ServerVersion.getVersion() >= 10) bossBar.setVisible(true);
    	}
    	
		@Override
		public void onEnd() {
			bossBar.removeAll();
		}

		@Override
		public void onSilentEnd() {
			bossBar.removeAll();
		}
		
	}.setPeriod(TimeUnit.TICKS, 1).register();
	
	public AbilityTimer acupdate = new AbilityTimer(20) {
		
		@Override
		public void onEnd() {
			ac.update(null);
		}
		
		@Override
		public void onSilentEnd() {
			ac.update(null);
		}
		
	}.setPeriod(TimeUnit.TICKS, 1).register();
	
	public void showEffect() {
		SoundLib.BLOCK_NOTE_BLOCK_PLING.playSound(getPlayer().getLocation(), 1f, (float) (1 + (combo / 20.0)));
		ParticleLib.NOTE.spawnParticle(getPlayer().getLocation().clone().add(0, 1, 0), 1, 1, 1, 2 + combo, 1);
		if (combo == 20) {
			SoundLib.BLOCK_NOTE_BLOCK_BELL.playSound(getPlayer().getLocation(), 1f, (float) (1 + (combo / 20.0)));
			SoundLib.BLOCK_NOTE_BLOCK_CHIME.playSound(getPlayer().getLocation(), 1f, (float) (1 + (combo / 20.0)));
		}
	}
	
	@SubscribeEvent
	public void onEntityDamageByEntity(EntityDamageByEntityEvent e) {
		Player damager = null;
		if (e.getDamager() instanceof Projectile) {
			Projectile projectile = (Projectile) e.getDamager();
			if (projectile.getShooter() != null && projectile.getShooter() instanceof Player) damager = (Player) projectile.getShooter();
		} else if (e.getDamager() instanceof Player) damager = (Player) e.getDamager();
		
		if (getPlayer().equals(damager)) {
			int nowcount = counter.getCount();
			int nowminus = nowcount - lastcount;
			if (nowminus == lastminus) {
				combo = Math.min(20, combo + 5);
				ac.update("§aP§bE§3R§5P§dE§cC§6T§e!");
			} else if ((nowminus >= lastminus - 2) && (nowminus <= lastminus + 2)) {
				combo = Math.min(20, combo + 3);
				ac.update("§dGREAT");
			}  else if ((nowminus >= lastminus - 5) && (nowminus <= lastminus + 5)) {
				combo = Math.min(20, combo + 2);
				ac.update("§bGOOD");
			} else if ((nowminus >= lastminus - 10) && (nowminus <= lastminus + 10)) {
				combo = Math.min(20, combo + 1);
				ac.update("§aBAD");
			} else {
				combo = 0;
				SoundLib.BLOCK_NOTE_BLOCK_PLING.playSound(getPlayer().getLocation(), 1f, 0.5f);
				ac.update("§7MISS");
			}
			
			e.setDamage(e.getDamage() * (1 + (damageincrease * combo)));
			showEffect();
			if (acupdate.isRunning()) acupdate.setCount(20);
			else acupdate.start();
			bossBar.setProgress(Math.min(1, Math.max(0, combo / 20.0)));
			
			bossBar.setTitle("§b콤보 §f| §c공격력 증가§7: §e" + df.format(100 * combo * damageincrease) + "%");
			
			lastminus = nowminus;
			lastcount = nowcount;
		}
	}
	
	@SubscribeEvent
	private void onPlayerJoin(final PlayerJoinEvent e) {
		if (getPlayer().getUniqueId().equals(e.getPlayer().getUniqueId())) {
			if (bossBar != null) bossBar.addPlayer(e.getPlayer());
		}
	}

	@SubscribeEvent
	private void onPlayerQuit(final PlayerQuitEvent e) {
		if (getPlayer().getUniqueId().equals(e.getPlayer().getUniqueId())) {
			if (bossBar != null) bossBar.removePlayer(e.getPlayer());
		}
	}

}
