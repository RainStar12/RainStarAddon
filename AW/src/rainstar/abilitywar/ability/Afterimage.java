package rainstar.abilitywar.ability;

import static rainstar.abilitywar.system.config.RainStarAbilitySetting.rsAbilitySettings;

import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

import daybreak.abilitywar.ability.AbilityBase;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.SubscribeEvent;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.config.ability.AbilitySettings.SettingObject;
import daybreak.abilitywar.config.enums.CooldownDecrease;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.utils.base.Formatter;
import daybreak.abilitywar.utils.base.math.VectorUtil;
import daybreak.abilitywar.utils.library.ParticleLib;
import daybreak.abilitywar.utils.library.PotionEffects;
import daybreak.abilitywar.utils.library.SoundLib;

@AbilityManifest(name = "잔상", rank = Rank.B, species = Species.HUMAN, explain = {
		"적에게 피격 시 이를 §b무효화§f하고 살짝 뒤로 이동합니다. $[COOLDOWN]",
		"이때 그 적에게 짧은 §7실명§f을 부여합니다."
		})
public class Afterimage extends AbilityBase {
	
	public Afterimage(Participant participant) {
		super(participant);
	}
	
	public static final SettingObject<Integer> COOLDOWN = rsAbilitySettings.new SettingObject<Integer>(Afterimage.class,
			"cooldown", 15, "# 쿨타임", "# 최대 쿨타임 감소가 50%까지만 적용됩니다.") {
		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}

		@Override
		public String toString() {
			return Formatter.formatCooldown(getValue());
		}
	};
	
	private final Cooldown cooldown = new Cooldown(COOLDOWN.getValue(), CooldownDecrease._50);

	@SubscribeEvent(ignoreCancelled = true)
	private void onEntityDamageByEntity(EntityDamageByEntityEvent e) {
		Player damager = null;
		if (e.getDamager() instanceof Projectile) {
			Projectile projectile = (Projectile) e.getDamager();
			if (projectile.getShooter() instanceof Player) damager = (Player) projectile.getShooter();
		} else if (e.getDamager() instanceof Player) damager = (Player) e.getDamager();
		
		if (damager != null && !cooldown.isRunning() && getPlayer().equals(e.getEntity()) && !e.getCause().equals(DamageCause.THORNS)) {
			cooldown.start();
			e.setCancelled(true);
			SoundLib.ENTITY_WITCH_THROW.playSound(getPlayer().getLocation(), 1, 2f);
			ParticleLib.CLOUD.spawnParticle(getPlayer().getLocation(), 0.7, 0, 0.7, 25, 0);
			getPlayer().setVelocity(VectorUtil.validateVector(getPlayer().getLocation().getDirection().clone().setY(0).normalize().multiply(-1.5)));
			damager.sendMessage("§c[§e능력§c] §b" + getPlayer().getName() + "§7: §f§o어딜 보시는 거죠? 그건 제 §b§o잔상§f§o입니다만?");
			PotionEffects.BLINDNESS.addPotionEffect(damager, 40, 0, true);
		}
	}
	
}
