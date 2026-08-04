package rainstar.abilitywar.ability;

import static rainstar.abilitywar.system.config.RainStarAbilitySetting.rsAbilitySettings;

import java.text.DecimalFormat;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.entity.EntityDamageByBlockEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;

import daybreak.abilitywar.ability.AbilityBase;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.SubscribeEvent;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.AbstractGame.Participant.ActionbarNotification.ActionbarChannel;
import daybreak.abilitywar.utils.base.Formatter;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.base.minecraft.entity.health.event.PlayerSetHealthEvent;
import daybreak.abilitywar.utils.base.minecraft.nms.NMS;
import daybreak.abilitywar.utils.library.SoundLib;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.ability.event.AbilityPreActiveSkillEvent;
import daybreak.abilitywar.config.ability.AbilitySettings.SettingObject;

@AbilityManifest(name = "시간 고정", rank = Rank.L, species = Species.HUMAN, explain = {
		"철괴 F키 시 $[DURATION]초간 §3공격 불가§f가 되지만 §d체력§f이 §b절대 감소하지 않습니다§f. $[COOLDOWN]"
		})
public class TimeFreeze extends AbilityBase {
	
	public TimeFreeze(Participant participant) {
		super(participant);
	}
	
	public static final SettingObject<Integer> COOLDOWN = rsAbilitySettings.new SettingObject<Integer>(TimeFreeze.class,
			"cooldown", 60, "# 쿨타임") {
		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}
		
        @Override
        public String toString() {
            return Formatter.formatCooldown(getValue());
        }
	};
	
	public static final SettingObject<Double> DURATION = rsAbilitySettings.new SettingObject<Double>(TimeFreeze.class,
			"duration", 5.0, "# 지속시간", "# 단위: 초") {
		@Override
		public boolean condition(Double value) {
			return value >= 0;
		}
	};
	
	private final Cooldown cooldown = new Cooldown(COOLDOWN.getValue());
	private final int duration = (int) (DURATION.getValue() * 20);
	private ActionbarChannel ac = newActionbarChannel();
	private DecimalFormat df = new DecimalFormat("0.0");
	private double health = 0;
	private float absorption = 0;
	
    @SubscribeEvent(onlyRelevant = true)
    private void onPlayerSwapHandItems(PlayerSwapHandItemsEvent e) {
    	if (e.getOffHandItem().getType().equals(Material.IRON_INGOT) && e.getPlayer().equals(getPlayer()) && !cooldown.isCooldown() && !skill.isRunning()) {
    		final AbilityPreActiveSkillEvent event = new AbilityPreActiveSkillEvent(this, Material.IRON_INGOT, null);
    		Bukkit.getPluginManager().callEvent(event);
    		if (!event.isCancelled()) {
    			SoundLib.ENTITY_EXPERIENCE_ORB_PICKUP.playSound(getPlayer().getLocation(), 1, 1);
    			skill.start();
    		}
    		e.setCancelled(true);
    	}
    }
    
	private final AbilityTimer skill = new AbilityTimer(duration) {
		
		private double sound = 0;
		
		@Override
		public void onStart() {
			ac.update("§3시간 고정§7: §b" + df.format(duration / 20.0) + "§f초");
			health = getPlayer().getHealth();
			absorption = NMS.getAbsorptionHearts(getPlayer());
			sound = 0;
		}
		
		@Override
		public void run(int count) {
		    double progress = 1.0 - (count / (double) duration);
		    double curve = Math.sin(progress * Math.PI);
		    double rate = 20 - curve * 19;

		    sound += rate;

		    while (sound >= 20) {
		        sound -= 20;
		        SoundLib.ENTITY_EXPERIENCE_ORB_PICKUP.playSound(getPlayer().getLocation(), 1f, 1f);
		    }
			
			ac.update("§3시간 고정§7: §b" + df.format(count / 20.0) + "§f초");
			if (health < getPlayer().getHealth()) health = getPlayer().getHealth();
			if (absorption < NMS.getAbsorptionHearts(getPlayer())) absorption = NMS.getAbsorptionHearts(getPlayer());
			
			getPlayer().setHealth(health);
			NMS.setAbsorptionHearts(getPlayer(), absorption);
		}
		
		@Override
		public void onEnd() {
			getPlayer().setHealth(health);
			NMS.setAbsorptionHearts(getPlayer(), absorption);
			cooldown.start();
			onSilentEnd();
		}
		
		@Override
		public void onSilentEnd() {
			ac.update(null);
		}
		
	}.setPeriod(TimeUnit.TICKS, 1).register();

	@SubscribeEvent(onlyRelevant = true)
	private void onHealth(PlayerSetHealthEvent e) {
		if (skill.isRunning() && e.getHealth() < health) e.setCancelled(true);
	}
	
	@SubscribeEvent(onlyRelevant = true)
	private void onEntityDamage(EntityDamageEvent e) {
		if (skill.isRunning()) e.setCancelled(true);
	}
	
	@SubscribeEvent(onlyRelevant = true)
	private void onEntityDamageByBlock(EntityDamageByBlockEvent e) {
		if (skill.isRunning()) e.setCancelled(true);
	}
	
	
	@SubscribeEvent
	private void onEntityDamageByEntity(EntityDamageByEntityEvent e) {
		if (skill.isRunning() && e.getEntity().equals(getPlayer())) e.setCancelled(true);
		
		Player damager = null;
		if (e.getDamager() instanceof Projectile) {
			Projectile projectile = (Projectile) e.getDamager();
			if (projectile.getShooter() instanceof Player) damager = (Player) projectile.getShooter();
		} else if (e.getDamager() instanceof Player) damager = (Player) e.getDamager();
		
		if (getPlayer().equals(damager) && skill.isRunning()) e.setCancelled(true);
	}
	
}
