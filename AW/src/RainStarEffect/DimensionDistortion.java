package RainStarEffect;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Arrow;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import daybreak.abilitywar.AbilityWar;
import daybreak.abilitywar.game.AbstractGame;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.manager.effect.registry.ApplicationMethod;
import daybreak.abilitywar.game.manager.effect.registry.EffectManifest;
import daybreak.abilitywar.game.manager.effect.registry.EffectRegistry;
import daybreak.abilitywar.game.manager.effect.registry.EffectType;
import daybreak.abilitywar.game.manager.effect.registry.EffectRegistry.EffectRegistration;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.library.ParticleLib;
import daybreak.abilitywar.utils.library.PotionEffects;

@EffectManifest(name = "차원 왜곡", displayName = "§3차원 왜곡", method = ApplicationMethod.UNIQUE_LONGEST, type = {
		EffectType.MOVEMENT_RESTRICTION, EffectType.COMBAT_RESTRICTION
}, description = {
		"점프 및 이동 속도가 급격하게 감소합니다.",
		"주는 모든 피해가 절반으로 감소합니다."
})
public class DimensionDistortion extends AbstractGame.Effect implements Listener {

	public static final EffectRegistration<DimensionDistortion> registration = EffectRegistry.registerEffect(DimensionDistortion.class);

	public static void apply(Participant participant, TimeUnit timeUnit, int duration) {
		registration.apply(participant, timeUnit, duration);
	}

	private final Participant participant;

	public DimensionDistortion(Participant participant, TimeUnit timeUnit, int duration) {
		participant.getGame().super(registration, participant, timeUnit.toTicks(duration));
		this.participant = participant;
		setPeriod(TimeUnit.TICKS, 1);
	}

	@Override
	protected void onStart() {
		Bukkit.getPluginManager().registerEvents(this, AbilityWar.getPlugin());
		PotionEffects.SLOW.addPotionEffect(participant.getPlayer(), 10000, 0, true);
	}

	@EventHandler
	public void onPlayerMove(PlayerMoveEvent e) {
		if (e.getPlayer().getUniqueId().equals(participant.getPlayer().getUniqueId())) {
			final double fromY = e.getFrom().getY(), toY = e.getTo().getY();
			if (toY > fromY) {
				double dx, dy, dz;
				final Location from = e.getFrom(), to = e.getTo();
				dx = to.getX() - from.getX();
				dy = to.getY() - from.getY();
				dz = to.getZ() - from.getZ();
				if (toY - fromY <= 1) {
					e.getPlayer().setVelocity(new Vector((dx * 0.65), (dy * 0.7), (dz * 0.65)));	
				}
			}
			ParticleLib.PORTAL.spawnParticle(e.getPlayer().getLocation(), 0, 0, 0, 3, 0.2);
		}
	}
	
	@EventHandler
	public void onEntityDamageByEntity(EntityDamageByEntityEvent e) {
		if (e.getDamager().getUniqueId().equals(participant.getPlayer().getUniqueId())) {
			e.setDamage(e.getDamage() * 0.5);
		}
		if (e.getDamager() instanceof Arrow) {
			Arrow arrow = (Arrow) e.getDamager();
			if (participant.getPlayer().equals(arrow.getShooter())) {
				e.setDamage(e.getDamage() * 0.5);
			}
		}
	}

	@Override
	protected void onEnd() {
		participant.getPlayer().removePotionEffect(PotionEffectType.SLOW);
		HandlerList.unregisterAll(this);
		super.onEnd();
	}

	@Override
	protected void onSilentEnd() {
		participant.getPlayer().removePotionEffect(PotionEffectType.SLOW);
		HandlerList.unregisterAll(this);
		super.onSilentEnd();
	}
	
}