package rainstar.abilitywar.ability;

import static rainstar.abilitywar.system.config.RainStarAbilitySetting.rsAbilitySettings;

import java.text.DecimalFormat;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent.RegainReason;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import daybreak.abilitywar.ability.AbilityBase;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.SubscribeEvent;
import daybreak.abilitywar.ability.decorator.ActiveHandler;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.config.ability.AbilitySettings.SettingObject;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.utils.base.Formatter;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.base.minecraft.entity.health.Healths;
import daybreak.abilitywar.utils.base.minecraft.version.ServerVersion;
import daybreak.abilitywar.utils.library.MaterialX;
import daybreak.abilitywar.utils.library.ParticleLib;
import daybreak.abilitywar.utils.library.SoundLib;

@AbilityManifest(name = "갈취", rank = Rank.S, species = Species.HUMAN, explain = {
		"§7패시브 §8- §c식탐§f: 모든 §a회복 효과§f를 $[RECOVERY_MULTIPLY]배로 받습니다.",
		"§7철괴 우클릭 §8- §4갈취§f: 다음 $[DURATION]초 동안 적에게 입힌 §4최종 피해량§f의",
		" $[RECOVERY_PERCENTAGE]%만큼 체력을 §a회복§f합니다. $[COOLDOWN]",
		"§b[§7아이디어 제공자§b] §6aidaid1234§f"
		})
public class Pillage extends AbilityBase implements ActiveHandler {
	
	public Pillage(Participant participant) {
		super(participant);
	}
	
	public static final SettingObject<Double> RECOVERY_MULTIPLY = rsAbilitySettings.new SettingObject<Double>(Pillage.class, 
			"recovery-multiply", 1.5, "# 상시 회복 효과 배율", "# 단위: 배") {
        @Override
        public boolean condition(Double value) {
            return value >= 0;
        }
    };
    
	public static final SettingObject<Double> DURATION = rsAbilitySettings.new SettingObject<Double>(Pillage.class, 
			"skill-duration", 6.0, "# 갈취 지속 시간", "# 단위: 초") {
        @Override
        public boolean condition(Double value) {
            return value >= 0;
        }
    };
    
	public static final SettingObject<Integer> RECOVERY_PERCENTAGE = rsAbilitySettings.new SettingObject<Integer>(Pillage.class,
			"recovery-percentage", 60, "# 피해량의 회복 비율", "# 단위: %") {
		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}
	};
    
	public static final SettingObject<Integer> COOLDOWN = rsAbilitySettings.new SettingObject<Integer>(Pillage.class,
			"cooldown", 85, "# 쿨타임") {
		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}

		@Override
		public String toString() {
			return Formatter.formatCooldown(getValue());
		}
	};
	
	private final double recoveryMultiply = RECOVERY_MULTIPLY.getValue();
	private final int duration = (int) (DURATION.getValue() * 20);
	private final double percentage = RECOVERY_PERCENTAGE.getValue() * 0.01;
	private final Cooldown cooldown = new Cooldown(COOLDOWN.getValue());
	private DecimalFormat df = new DecimalFormat("0.0");
	private BossBar bossBar = null;
	
	private AbilityTimer skill = new AbilityTimer(duration) {
		
		@Override
		public void onStart() {
			bossBar = Bukkit.createBossBar("§4갈취§7: §c" + df.format(duration / 20.0) + "§f초", BarColor.RED, BarStyle.SOLID);
    		bossBar.setProgress(1);
			bossBar.addPlayer(getPlayer());
    		if (ServerVersion.getVersion() >= 10) bossBar.setVisible(true);
		}
		
		@Override
		public void run(int count) {
			bossBar.setTitle("§4갈취§7: §c" + df.format(count / 20.0) + "§f초");
			bossBar.setProgress((double) count / duration);
		}
		
		@Override
		public void onEnd() {
			onSilentEnd();
		}
		
		@Override
		public void onSilentEnd() {
			bossBar.removeAll();
			SoundLib.ENTITY_PLAYER_BURP.playSound(getPlayer().getLocation(), 1, 0.75f);
			cooldown.start();
		}
		
	}.setPeriod(TimeUnit.TICKS, 1).register();
	
	@SubscribeEvent(onlyRelevant = true)
	private void onEntityRegainHealth(EntityRegainHealthEvent e) {
		e.setAmount(e.getAmount() * recoveryMultiply);
	}
	
	@SubscribeEvent()
	private void onPlayerJoin(final PlayerJoinEvent e) {
		if (getPlayer().getUniqueId().equals(e.getPlayer().getUniqueId()) && skill.isRunning()) {
			if (bossBar != null) bossBar.addPlayer(e.getPlayer());
		}
	}

	@SubscribeEvent()
	private void onPlayerQuit(final PlayerQuitEvent e) {
		if (getPlayer().getUniqueId().equals(e.getPlayer().getUniqueId())) {
			if (bossBar != null) bossBar.removePlayer(e.getPlayer());
		}
	}
	
	@SubscribeEvent(priority = 1000)
	private void onEntityDamageByEntity(EntityDamageByEntityEvent e) {
		Player damager = null;
		if (e.getDamager() instanceof Projectile) {
			Projectile projectile = (Projectile) e.getDamager();
			if (projectile.getShooter() instanceof Player) damager = (Player) projectile.getShooter();
		} else if (e.getDamager() instanceof Player) damager = (Player) e.getDamager();
		
		if (skill.isRunning() && getPlayer().equals(damager)) {
			final EntityRegainHealthEvent event = new EntityRegainHealthEvent(getPlayer(), (e.getFinalDamage() * percentage), RegainReason.CUSTOM);
			Bukkit.getPluginManager().callEvent(event);
			if (!event.isCancelled()) {
				Healths.setHealth(getPlayer(), getPlayer().getHealth() + event.getAmount());
			}
			SoundLib.ENTITY_GENERIC_EAT.playSound(getPlayer().getLocation(), 1, 0.5f);
			ParticleLib.ITEM_CRACK.spawnParticle(e.getEntity().getLocation(), 0.25, 0.5, 0.25, 33, 0.77, MaterialX.NETHER_WART);
		}
	}
	
	@Override
	public boolean ActiveSkill(Material material, ClickType clickType) {
		if (material == Material.IRON_INGOT && clickType == ClickType.RIGHT_CLICK) {
			if (!cooldown.isCooldown() && !skill.isRunning()) {
				return skill.start();
			}
		}
		return false;
	}

}
