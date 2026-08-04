package rainstar.abilitywar.effect;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

import daybreak.abilitywar.AbilityWar;
import daybreak.abilitywar.game.AbstractGame;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.manager.effect.registry.ApplicationMethod;
import daybreak.abilitywar.game.manager.effect.registry.EffectManifest;
import daybreak.abilitywar.game.manager.effect.registry.EffectRegistry;
import daybreak.abilitywar.game.manager.effect.registry.EffectType;
import daybreak.abilitywar.game.manager.effect.registry.EffectRegistry.EffectRegistration;
import daybreak.abilitywar.utils.base.color.RGB;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.base.math.geometry.Points;
import daybreak.abilitywar.utils.library.ParticleLib;
import daybreak.abilitywar.utils.library.PotionEffects;

@EffectManifest(name = "암전", displayName = "§8암전", method = ApplicationMethod.UNIQUE_STACK, type = {
		EffectType.SIGHT_RESTRICTION
}, description = {
		"§f적을 공격하면 1초간 실명 상태에 빠집니다."
})
public class Blackout extends AbstractGame.Effect implements Listener {

	public static final EffectRegistration<Blackout> registration = EffectRegistry.registerEffect(Blackout.class);

	public static void apply(Participant participant, TimeUnit timeUnit, int duration) {
		registration.apply(participant, timeUnit, duration);
	}

	private final Participant participant;
	
	private static final Points CLOSED_EYE = Points.of(0.12, new boolean[][]{
		{true, false, false, false, false, false, false, false, false, false, false, false, false, false, true},
		{false, true, false, false, false, false, false, false, false, false, false, false, false, true, false},
		{false, false, true, true, false, false, false, false, false, false, false, true, true, false, false},
		{false, true, false, false, true, true, true, true, true, true, true, false, false, true, false},
		{false, false, false, false, true, false, false, true, false, false, true, false, false, false, false},
		{false, false, false, true, false, false, false, true, false, false, false, true, false, false, false},
		{false, false, false, false, false, false, false, true, false, false, false, false, false, false, false}
	});
	
	private static final RGB COLOR = RGB.of(25, 32, 161);

	public Blackout(Participant participant, TimeUnit timeUnit, int duration) {
		participant.getGame().super(registration, participant, timeUnit.toTicks(duration));
		this.participant = participant;
		setPeriod(TimeUnit.TICKS, 1);
	}

	@Override
	protected void onStart() {
		Bukkit.getPluginManager().registerEvents(this, AbilityWar.getPlugin());
		super.onStart();
	}
	
	@Override
	protected void run(int count) {
		super.run(count);
		if (count % 4 == 0) {
			final Location headLocation = participant.getPlayer().getEyeLocation().clone().add(0, 1.7, 0);
			final Location baseLocation = headLocation.clone().subtract(0, 1.6, 0);
			final float yaw = participant.getPlayer().getLocation().getYaw();
			for (Location loc : CLOSED_EYE.rotateAroundAxisY(-yaw).toLocations(baseLocation)) {
				ParticleLib.REDSTONE.spawnParticle(participant.getPlayer(), loc, COLOR);
			}
			CLOSED_EYE.rotateAroundAxisY(yaw);		
		}
	}

	
	@Override
	protected void onEnd() {
		onSilentEnd();
	}

	@Override
	protected void onSilentEnd() {
		HandlerList.unregisterAll(this);
		super.onSilentEnd();
	}
	
	@EventHandler()
	private void onEntityDamageByEntity(EntityDamageByEntityEvent e) {
		Player damager = null;
		if (e.getDamager() instanceof Projectile) {
			Projectile projectile = (Projectile) e.getDamager();
			if (projectile.getShooter() != null && projectile.getShooter().equals(participant.getPlayer())) {
				damager = participant.getPlayer();
			}
		} else if (participant.getPlayer().equals(e.getDamager())) damager = participant.getPlayer();
		
		if (participant.getPlayer().equals(damager)) {
			PotionEffects.BLINDNESS.addPotionEffect(participant.getPlayer(), 20, 0, false, false, true);
		}
	}
	
}