package rainstar.abilitywar.effect;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.player.PlayerMoveEvent;

import daybreak.abilitywar.AbilityWar;
import daybreak.abilitywar.game.AbstractGame;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.manager.effect.registry.ApplicationMethod;
import daybreak.abilitywar.game.manager.effect.registry.EffectManifest;
import daybreak.abilitywar.game.manager.effect.registry.EffectRegistry;
import daybreak.abilitywar.game.manager.effect.registry.EffectType;
import daybreak.abilitywar.game.manager.effect.registry.EffectRegistry.EffectRegistration;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.base.minecraft.entity.health.event.PlayerSetHealthEvent;
import daybreak.abilitywar.utils.base.minecraft.nms.NMS;

@EffectManifest(name = "가사", displayName = "§7가사", method = ApplicationMethod.UNIQUE_LONGEST, type = {
		EffectType.COMBAT_RESTRICTION, EffectType.MOVEMENT_RESTRICTION, EffectType.HEALING_BAN
}, description = {
		"§f체력이 44.44% 이하라면 체력이 고정되고,",
		"§f공격 및 이동이 불가능해집니다.",
		"§f체력이 44.44%를 초과한다면 받는 피해량이 25% 증가합니다."
})
public class ApparentDeath extends AbstractGame.Effect implements Listener {

	public static final EffectRegistration<ApparentDeath> registration = EffectRegistry.registerEffect(ApparentDeath.class);

	public static void apply(Participant participant, TimeUnit timeUnit, int duration) {
		registration.apply(participant, timeUnit, duration);
	}

	private final Participant participant;
	private double healthlock = 0;
	private final ArmorStand hologram;

	public ApparentDeath(Participant participant, TimeUnit timeUnit, int duration) {
		participant.getGame().super(registration, participant, timeUnit.toTicks(duration));
		this.participant = participant;
		final Location location = participant.getPlayer().getLocation();
		this.hologram = location.getWorld().spawn(location.clone().add(0, 2.7, 0), ArmorStand.class);
		hologram.setVisible(false);
		hologram.setGravity(false);
		hologram.setInvulnerable(true);
		NMS.removeBoundingBox(hologram);
		hologram.setCustomName("§7X_X");
		hologram.setCustomNameVisible(true);
		setPeriod(TimeUnit.TICKS, 1);
	}
	
	private boolean isLowHealth() {
		if (participant.getPlayer().getHealth() <= participant.getPlayer().getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue() * 0.4444) return true;
		return false;
	}
	
	@Override
	protected void onStart() {
		Bukkit.getPluginManager().registerEvents(this, AbilityWar.getPlugin());
		super.onStart();
	}	
	
	@Override
	protected void run(int count) {
		hologram.teleport(participant.getPlayer().getLocation().clone().add(0, 2.5, 0));
		hologram.setCustomName((isLowHealth() ? "§4" : "§7") + "X_X");
		if (isLowHealth() && healthlock == 0) healthlock = participant.getPlayer().getHealth();
		if (participant.getPlayer().getHealth() > 0 && healthlock > 0) participant.getPlayer().setHealth(healthlock);
		super.run(count);
	}
	
	@EventHandler
	private void onPlayerMove(final PlayerMoveEvent e) {
		if (participant.getPlayer().equals(e.getPlayer()) && isLowHealth()) {
			e.setTo(e.getFrom());
		}
	}
	
	@EventHandler
	private void onEntityRegainHealth(final EntityRegainHealthEvent e) {
		if (e.getEntity().getUniqueId().equals(participant.getPlayer().getUniqueId()) && isLowHealth()) {
			e.setCancelled(true);
			if (participant.getPlayer().getHealth() > 0 && healthlock > 0) participant.getPlayer().setHealth(healthlock);
		}
	}
	
	@EventHandler
	private void onSetHealth(final PlayerSetHealthEvent e) {
		if (e.getPlayer().getUniqueId().equals(participant.getPlayer().getUniqueId()) && isLowHealth()) {
			e.setCancelled(true);
			if (participant.getPlayer().getHealth() > 0 && healthlock > 0) participant.getPlayer().setHealth(healthlock);
		}
	}
	
	@EventHandler
	private void onEntityDamage(final EntityDamageEvent e) {
		if (e.getEntity().getUniqueId().equals(participant.getPlayer().getUniqueId())) {
			if (isLowHealth()) {
				e.setDamage(0);
				if (participant.getPlayer().getHealth() > 0 && healthlock > 0) participant.getPlayer().setHealth(healthlock);	
			} else {
				e.setDamage(e.getDamage() * 1.25);
			}
		}
	}
	
	@EventHandler
	private void onEntityDamageByEntity(final EntityDamageByEntityEvent e) {
		onEntityDamage(e);
		Player damager = null;
		if (e.getDamager() instanceof Projectile) {
			Projectile projectile = (Projectile) e.getDamager();
			if (projectile.getShooter() instanceof Player) damager = (Player) projectile.getShooter();
		} else if (e.getDamager() instanceof Player) damager = (Player) e.getDamager();
		
		if (isLowHealth() && participant.getPlayer().equals(damager)) {
			e.setCancelled(true);
		}
	}

	@Override
	protected void onEnd() {
		onSilentEnd();
		super.onEnd();
	}

	@Override
	protected void onSilentEnd() {
		HandlerList.unregisterAll(this);
		hologram.remove();
		super.onSilentEnd();
	}
}