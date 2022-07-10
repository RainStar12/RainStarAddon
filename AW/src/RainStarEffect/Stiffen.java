package RainStarEffect;

import org.bukkit.Bukkit;
import org.bukkit.entity.ArmorStand;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
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
import daybreak.abilitywar.utils.base.minecraft.nms.NMS;
import daybreak.abilitywar.utils.library.SoundLib;

@EffectManifest(name = "경직", displayName = "§c경직", method = ApplicationMethod.UNIQUE_LONGEST, type = {
		EffectType.MOVEMENT_RESTRICTION, EffectType.HEALING_BAN, EffectType.ABILITY_RESTRICTION,
		EffectType.COMBAT_RESTRICTION
}, description = {
		"이동, 공격, 체력 회복이 불가능합니다."
})
public class Stiffen extends AbstractGame.Effect implements Listener {

	public static final EffectRegistration<Stiffen> registration = EffectRegistry.registerEffect(Stiffen.class);

	public static void apply(Participant participant, TimeUnit timeUnit, int duration) {
		registration.apply(participant, timeUnit, duration);
	}

	private final Participant participant;
	private final ArmorStand hologram;

	public Stiffen(Participant participant, TimeUnit timeUnit, int duration) {
		participant.getGame().super(registration, participant, (timeUnit.toTicks(duration) / 2));
		this.participant = participant;
		this.hologram = participant.getPlayer().getWorld().spawn(participant.getPlayer().getLocation(), ArmorStand.class);
		hologram.setVisible(false);
		hologram.setGravity(false);
		hologram.setInvulnerable(true);
		NMS.removeBoundingBox(hologram);
		hologram.setCustomNameVisible(true);
		hologram.setCustomName("§8경직!");
		setPeriod(TimeUnit.TICKS, 2);
	}
	
	@Override
	protected void onStart() {
		SoundLib.BLOCK_ANVIL_FALL.playSound(participant.getPlayer().getLocation(), 1, 1.2f);
		SoundLib.ENTITY_GENERIC_EXPLODE.playSound(participant.getPlayer().getLocation(), 1, 1.15f);
		Bukkit.getPluginManager().registerEvents(this, AbilityWar.getPlugin());
	}
	
	@EventHandler
	public void onRegainHealth(EntityRegainHealthEvent e) {
		if (participant.getPlayer().equals(e.getEntity())) {
			e.setCancelled(true);
		}
	}
	
	@EventHandler
	public void onEntityDamageByEntity(EntityDamageByEntityEvent e) {
		if (participant.getPlayer().equals(e.getDamager())) {
			e.setCancelled(true);
		}
	}
	
	@EventHandler
	public void onProjectileLaunch(ProjectileLaunchEvent e) {
		if (participant.getPlayer().equals(e.getEntity().getShooter())) {
			e.setCancelled(true);
		}
	}

	@EventHandler
	public void onPlayerMove(PlayerMoveEvent e) {
		if (participant.getPlayer().equals(e.getPlayer())) {
			e.setTo(e.getFrom());
		}
	}
	
	@Override
	protected void run(int count) {
		hologram.teleport(participant.getPlayer().getLocation().clone().add(0, 2.2, 0));
		super.run(count);
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