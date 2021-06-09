package RainStarEffect;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.util.Vector;

import daybreak.abilitywar.AbilityWar;
import daybreak.abilitywar.game.AbstractGame;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.manager.effect.registry.ApplicationMethod;
import daybreak.abilitywar.game.manager.effect.registry.EffectConstructor;
import daybreak.abilitywar.game.manager.effect.registry.EffectManifest;
import daybreak.abilitywar.game.manager.effect.registry.EffectRegistry;
import daybreak.abilitywar.game.manager.effect.registry.EffectRegistry.EffectRegistration;
import daybreak.abilitywar.game.manager.effect.registry.EffectType;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.base.math.LocationUtil;
import daybreak.abilitywar.utils.base.minecraft.entity.health.Healths;
import daybreak.abilitywar.utils.base.minecraft.nms.NMS;
import daybreak.abilitywar.utils.library.ParticleLib;

@EffectManifest(name = "유혹", displayName = "§d유혹", method = ApplicationMethod.UNIQUE_LONGEST, type = {
		EffectType.SIGHT_CONTROL
}, description = {
		"유혹한 대상을 지속해서 바라보게 되며,",
		"유혹한 대상이 유혹된 플레이어를 타격할 때 준 최종 피해량에",
		"비례하여 유혹한 대상의 체력을 회복합니다."
})
public class Charm extends AbstractGame.Effect implements Listener {

	public static final EffectRegistration<Charm> registration = EffectRegistry.registerEffect(Charm.class);

	public static void apply(Participant participant, TimeUnit timeUnit, int duration, Player applyPlayer, int regenPercent, int decreasePercent) {
		registration.apply(participant, timeUnit, duration, "with-player", applyPlayer, regenPercent, decreasePercent);
	}

	private final Participant participant;
	private final Player applyPlayer;
	private final int regenPercent;
	private final int decreasePercent;

	@EffectConstructor(name = "with-player")
	public Charm(Participant participant, TimeUnit timeUnit, int duration, Player applyPlayer, int regenPercent, int decreasePercent) {
		participant.getGame().super(registration, participant, timeUnit.toTicks(duration));
		this.participant = participant;
		this.applyPlayer = applyPlayer;
		this.regenPercent = regenPercent;
		this.decreasePercent = decreasePercent;
		setPeriod(TimeUnit.TICKS, 1);
	}

	@Override
	protected void onStart() {
		Bukkit.getPluginManager().registerEvents(this, AbilityWar.getPlugin());
		super.onStart();
	}

	@EventHandler
	private void onEntityDamageByEntity(EntityDamageByEntityEvent e) {
		if (e.getEntity().getUniqueId().equals(participant.getPlayer().getUniqueId()) && e.getDamager().equals(applyPlayer)) {
			Healths.setHealth(applyPlayer, applyPlayer.getHealth() + (e.getFinalDamage() * (regenPercent * 0.01)));
		}
		if (e.getEntity().getUniqueId().equals(participant.getPlayer().getUniqueId()) && e.getDamager() instanceof Projectile) {
			Projectile projectile = (Projectile) e.getDamager();
			if (applyPlayer.equals(projectile.getShooter())) Healths.setHealth(applyPlayer, applyPlayer.getHealth() + (e.getFinalDamage() * (regenPercent * 0.01)));
		}
		if (e.getDamager().getUniqueId().equals(participant.getPlayer().getUniqueId()) && e.getEntity().equals(applyPlayer)) {
			e.setDamage(e.getDamage() * (1 - (decreasePercent * 0.01)));
		}
		if (e.getEntity().equals(applyPlayer) && e.getDamager() instanceof Projectile) {
			Projectile projectile = (Projectile) e.getDamager();
			if (participant.getPlayer().equals(projectile.getShooter())) e.setDamage(e.getDamage() * (1 - (decreasePercent * 0.01)));
		}
	}
	
	@Override
	protected void run(int count) {
		Vector direction = applyPlayer.getEyeLocation().toVector().subtract(participant.getPlayer().getEyeLocation().toVector());
		float yaw = LocationUtil.getYaw(direction), pitch = LocationUtil.getPitch(direction);
		for (Player player : Bukkit.getOnlinePlayers()) {
		    NMS.rotateHead(player, participant.getPlayer(), yaw, pitch);	
		}
		if (count % 20 == 0) {
    		ParticleLib.HEART.spawnParticle(participant.getPlayer().getLocation(), 0.5, 1, 0.5, 10, 1);
		}
		super.run(count);
	}

	public int getDuration() {
		return (getCount() * 20);
	}
	
	public Player getApplier() {
		return applyPlayer;
	}
	
	@Override
	protected void onEnd() {
		HandlerList.unregisterAll(this);
		super.onEnd();
	}

	@Override
	protected void onSilentEnd() {
		HandlerList.unregisterAll(this);
		super.onSilentEnd();
	}
	
}