package rainstar.abilitywar.effect;

import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.attribute.AttributeModifier.Operation;
import org.bukkit.entity.ArmorStand;
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
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.base.minecraft.nms.NMS;
import daybreak.abilitywar.utils.library.ParticleLib;

@EffectManifest(name = "습기", displayName = "§3습기", method = ApplicationMethod.UNIQUE_STACK, type = {
		EffectType.MOVEMENT_INTERRUPT, EffectType.COMBAT_RESTRICTION
}, description = {
		"이동 속도가 25%, 공격력이 15% 감소합니다.",
		"이 상태이상은 시간이 중첩됩니다."
})
public class Moisture extends AbstractGame.Effect implements Listener {

	public static final EffectRegistration<Moisture> registration = EffectRegistry.registerEffect(Moisture.class);

	public static void apply(Participant participant, TimeUnit timeUnit, int duration) {
		registration.apply(participant, timeUnit, duration);
	}

	private final Participant participant;
	private final ArmorStand hologram;
	private AttributeModifier decmovespeed;

	public Moisture(Participant participant, TimeUnit timeUnit, int duration) {
		participant.getGame().super(registration, participant, timeUnit.toTicks(duration));
		this.participant = participant;
		setPeriod(TimeUnit.TICKS, 1);
		final Location location = participant.getPlayer().getLocation();
		this.hologram = location.getWorld().spawn(location.clone().add(0, 2.2, 0), ArmorStand.class);
		hologram.setVisible(false);
		hologram.setGravity(false);
		hologram.setInvulnerable(true);
		NMS.removeBoundingBox(hologram);
		hologram.setCustomName("§3습기");
		hologram.setCustomNameVisible(true);
	}
	
	@EventHandler
	private void onEntityDamageByEntity(EntityDamageByEntityEvent e) {
		Player damager = null;
		if (e.getDamager() instanceof Projectile) {
			Projectile projectile = (Projectile) e.getDamager();
			if (projectile.getShooter() instanceof Player) damager = (Player) projectile.getShooter();
		} else if (e.getDamager() instanceof Player) damager = (Player) e.getDamager();
		
		if (damager.equals(participant.getPlayer())) e.setDamage(e.getDamage() * 0.85);
	}

	@Override
	protected void run(int count) {
		ParticleLib.WATER_SPLASH.spawnParticle(participant.getPlayer().getLocation().clone().add(0, 1, 0), 0.5, 1, 0.5, 20, 1);
		hologram.teleport(participant.getPlayer().getLocation().clone().add(0, 2.2, 0));
		super.run(count);
	}
	
	@Override
	protected void onStart() {
		decmovespeed = new AttributeModifier(UUID.randomUUID(), "decmovespeed", -0.25, Operation.ADD_SCALAR);
		participant.getPlayer().getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).addModifier(decmovespeed);
		Bukkit.getPluginManager().registerEvents(this, AbilityWar.getPlugin());
		super.onStart();
	}

	@Override
	protected void onEnd() {
		participant.getPlayer().getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).removeModifier(decmovespeed);
		hologram.remove();
		HandlerList.unregisterAll(this);
		super.onEnd();
	}

	@Override
	protected void onSilentEnd() {
		participant.getPlayer().getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).removeModifier(decmovespeed);
		hologram.remove();
		HandlerList.unregisterAll(this);
		super.onSilentEnd();
	}
	
}