package RainStarEffect;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;

import daybreak.abilitywar.AbilityWar;
import daybreak.abilitywar.game.AbstractGame;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.manager.effect.event.ParticipantPreEffectApplyEvent;
import daybreak.abilitywar.game.manager.effect.registry.ApplicationMethod;
import daybreak.abilitywar.game.manager.effect.registry.EffectManifest;
import daybreak.abilitywar.game.manager.effect.registry.EffectRegistry;
import daybreak.abilitywar.game.manager.effect.registry.EffectType;
import daybreak.abilitywar.game.manager.effect.registry.EffectRegistry.EffectRegistration;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.base.minecraft.nms.NMS;
import daybreak.google.common.base.Strings;

@EffectManifest(name = "고대 저주", displayName = "§5고대 저주", method = ApplicationMethod.UNIQUE_LONGEST, type = {
		EffectType.COMBAT_RESTRICTION
}, description = {
		"레벨당 공격력 및 받는 피해가 10% 증가합니다."
})

public class AncientCurse extends AbstractGame.Effect implements Listener {

	public static final EffectRegistration<AncientCurse> registration = EffectRegistry.registerEffect(AncientCurse.class);

	public static void apply(Participant participant, TimeUnit timeUnit, int duration) {
		registration.apply(participant, timeUnit, duration);
	}

	private final Participant participant;
	private final ArmorStand hologram;
	private int level = 1;

	public AncientCurse(Participant participant, TimeUnit timeUnit, int duration) {
		participant.getGame().super(registration, participant, timeUnit.toTicks(duration));
		this.participant = participant;
		setPeriod(TimeUnit.TICKS, 1);
		final Location location = participant.getPlayer().getLocation();
		this.hologram = location.getWorld().spawn(location.clone().add(0, 2.2, 0), ArmorStand.class);
		hologram.setVisible(false);
		hologram.setGravity(false);
		hologram.setInvulnerable(true);
		NMS.removeBoundingBox(hologram);
		hologram.setCustomName(Strings.repeat("§5↓", level));
		hologram.setCustomNameVisible(true);
	}
	
	@EventHandler
	private void onParticipantEffectApply(ParticipantPreEffectApplyEvent e) {
		if (e.getParticipant().equals(participant)) {
			if (e.getEffectType().equals(AncientCurse.registration)) {
				this.setCount(e.getDuration());
				level++;
				hologram.setCustomName(Strings.repeat("§5↓", level));
			}	
		}
	}
	
	@EventHandler
	private void onEntityDamage(EntityDamageEvent e) {
		if (e.getEntity().equals(participant.getPlayer())) e.setDamage(e.getDamage() * (1 + (level * 0.1)));
	}
	
	@EventHandler
	private void onEntityDamageByEntity(EntityDamageByEntityEvent e) {
		Player damager = null;
		if (e.getDamager() instanceof Projectile) {
			Projectile projectile = (Projectile) e.getDamager();
			if (projectile.getShooter() instanceof Player) damager = (Player) projectile.getShooter();
		} else if (e.getDamager() instanceof Player) damager = (Player) e.getDamager();
		
		if (damager.equals(participant.getPlayer())) e.setDamage(e.getDamage() * (1 + (level * 0.1)));
	}
	
	public int getLevel() {
		return level;
	}

	@Override
	protected void run(int count) {
		hologram.teleport(participant.getPlayer().getLocation().clone().add(0, 2.2, 0));
		super.run(count);
	}
	
	@Override
	protected void onStart() {
		Bukkit.getPluginManager().registerEvents(this, AbilityWar.getPlugin());
		super.onStart();
	}

	@Override
	protected void onEnd() {
		hologram.remove();
		HandlerList.unregisterAll(this);
		super.onEnd();
	}

	@Override
	protected void onSilentEnd() {
		hologram.remove();
		HandlerList.unregisterAll(this);
		super.onSilentEnd();
	}
	
}