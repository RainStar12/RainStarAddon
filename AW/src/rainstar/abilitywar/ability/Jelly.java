package rainstar.abilitywar.ability;

import static rainstar.abilitywar.system.config.RainStarAbilitySetting.rsAbilitySettings;

import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

import daybreak.abilitywar.ability.AbilityBase;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.SubscribeEvent;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.config.ability.AbilitySettings.SettingObject;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.utils.base.math.VectorUtil;
import daybreak.abilitywar.utils.library.MaterialX;
import daybreak.abilitywar.utils.library.ParticleLib;
import daybreak.abilitywar.utils.library.SoundLib;

@AbilityManifest(name = "젤리", rank = Rank.C, species = Species.OTHERS, explain = {
		"피해를 받으면 공격한 대상을 피해량에 비례해 살짝 §a밀쳐냅니다§f.",
		"적과의 거리 1칸당 받는 피해량이 §c$[DECREASE]%§f 감소합니다."
		})
public class Jelly extends AbilityBase {
	
	public Jelly(Participant participant) {
		super(participant);
	}
	
	public static final SettingObject<Double> DECREASE = rsAbilitySettings.new SettingObject<Double>(Jelly.class,
			"decrease-amount", 2.0, "# 거리 1칸당 받는 피해량 감소량", "# 단위: %") {
		@Override
		public boolean condition(Double value) {
			return value >= 0;
		}
	};
	
	private final double decrease = DECREASE.getValue() * 0.01;
	
	@SubscribeEvent(ignoreCancelled = true, priority = 10)
	private void onEntityDamageByEntity(EntityDamageByEntityEvent e) {
		Player damager = null;
		if (e.getDamager() instanceof Projectile) {
			Projectile projectile = (Projectile) e.getDamager();
			if (projectile.getShooter() instanceof Player) damager = (Player) projectile.getShooter();
		} else if (e.getDamager() instanceof Player) damager = (Player) e.getDamager();
		
		if (e.getEntity().equals(getPlayer()) && !getPlayer().equals(damager) && damager != null) {
			if (damager.equals(e.getDamager())) {
				damager.setVelocity(VectorUtil.validateVector(damager.getLocation().toVector().subtract(getPlayer().getLocation().toVector()).normalize().setY(0).multiply(e.getFinalDamage() * 0.5)));
				SoundLib.ENTITY_SLIME_JUMP.playSound(getPlayer().getLocation(), 1, 0.66f);
				ParticleLib.ITEM_CRACK.spawnParticle(getPlayer().getLocation().clone().add(0, 0.5, 0), 0, 0, 0, 25, 1, MaterialX.SLIME_BLOCK);
			}
			e.setDamage(e.getDamage() * (1 - Math.min(1, (Math.sqrt(damager.getLocation().distanceSquared(getPlayer().getLocation())) * decrease))));
		}
	}

}
