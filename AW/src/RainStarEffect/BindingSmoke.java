package RainStarEffect;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
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

@EffectManifest(name = "속박의 연막", displayName = "§8속박의 연막", method = ApplicationMethod.UNIQUE_LONGEST, type = {
		EffectType.MOVEMENT_RESTRICTION, EffectType.COMBAT_RESTRICTION
}, description = {
		"점프 및 이동 속도가 급격하게 감소합니다.",
		"또한 엔티티에 의한 피해 외 모든 피해를 1.5배로 받습니다."
})
public class BindingSmoke extends AbstractGame.Effect implements Listener {

	public static final EffectRegistration<BindingSmoke> registration = EffectRegistry.registerEffect(BindingSmoke.class);

	public static void apply(Participant participant, TimeUnit timeUnit, int duration) {
		registration.apply(participant, timeUnit, duration);
	}

	private final Participant participant;

	public BindingSmoke(Participant participant, TimeUnit timeUnit, int duration) {
		participant.getGame().super(registration, participant, (timeUnit.toTicks(duration) / 2));
		this.participant = participant;
		setPeriod(TimeUnit.TICKS, 2);
	}
	
	@Override
	protected void onStart() {
		Bukkit.getPluginManager().registerEvents(this, AbilityWar.getPlugin());
	}
	
	@Override
	protected void run(int count) {
		PotionEffects.SLOW.addPotionEffect(participant.getPlayer(), 10000, 0, true);
		if (count % 10 == 0) {
			ParticleLib.EXPLOSION_NORMAL.spawnParticle(participant.getPlayer().getLocation(), 0.7, 0.7, 0.7, 40, 0);	
		}
		super.run(count);
	}
	
	@EventHandler
	private void onPlayerMove(PlayerMoveEvent e) {
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
		}
	}
	
	@EventHandler
	public void onEntityDamage(EntityDamageEvent e) {
		if (e.getCause() != DamageCause.ENTITY_ATTACK && e.getCause() != DamageCause.ENTITY_SWEEP_ATTACK && e.getCause() != DamageCause.PROJECTILE) {
			if (participant.getPlayer().equals(e.getEntity())) e.setDamage(e.getDamage() * 1.5);
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