package RainStarEffect;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.potion.PotionEffectType;

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
import daybreak.abilitywar.utils.base.math.geometry.Circle;
import daybreak.abilitywar.utils.base.random.Random;
import daybreak.abilitywar.utils.library.ParticleLib;
import daybreak.abilitywar.utils.library.PotionEffects;

@EffectManifest(name = "마비", displayName = "§3마비", method = ApplicationMethod.UNIQUE_LONGEST, type = {
		EffectType.MOVEMENT_INTERRUPT, EffectType.COMBAT_RESTRICTION, EffectType.SIGHT_RESTRICTION 
}, description = {
		"§f눈, 팔, 다리 중 한 부분이 §3마비§f됩니다.",
		"§3마비§f된 부위에 따라 각각의 행동이 제약됩니다.",
		"§6눈§7: §f시야가 차단됩니다. §7(실명 효과)",
		"§6팔§7: §f근접 피해량이 매우 느리고 약해집니다.",
		"§6다리§7: §f이동 속도가 급격히 느려집니다."
})
public class Paralysis extends AbstractGame.Effect implements Listener {

	public static final EffectRegistration<Paralysis> registration = EffectRegistry.registerEffect(Paralysis.class);

	public static void apply(Participant participant, TimeUnit timeUnit, int duration) {
		registration.apply(participant, timeUnit, duration);
	}

	private final Participant participant;
	private Random random = new Random();
	private int type = 0;
	private boolean jumped = false;
	private static final Circle circle = Circle.of(0.35, 20);
	private RGB COLOR = RGB.of(64, 139, 138);

	public Paralysis(Participant participant, TimeUnit timeUnit, int duration) {
		participant.getGame().super(registration, participant, (timeUnit.toTicks(duration) / 2));
		this.participant = participant;
		setPeriod(TimeUnit.TICKS, 2);
		type = random.nextInt(3) + 1;
	}
	
	@Override
	protected void onStart() {
		Bukkit.getPluginManager().registerEvents(this, AbilityWar.getPlugin());
	}
	
	@EventHandler
	public void onEntityDamageByEntity(EntityDamageByEntityEvent e) {
		if (participant.getPlayer().equals(e.getDamager())) {
			if (type == 2) e.setDamage(e.getDamage() * 0.4);
		}
	}
	
	@SuppressWarnings("deprecation")
	@EventHandler
	public void onPlayerMove(PlayerMoveEvent e) {
		if (participant.getPlayer().equals(e.getPlayer())) {
			if (type == 3) {
				if (!jumped) e.getPlayer().setVelocity(e.getPlayer().getVelocity().multiply(0.5));
				if (e.getTo().getY() > e.getFrom().getY()) {
					jumped = true;
				}
				if (e.getPlayer().isOnGround()) {
					jumped = false;
				}
			}
		}
	}

	@Override
	protected void run(int count) {
		if (type == 1) PotionEffects.BLINDNESS.addPotionEffect(participant.getPlayer(), 10000, 0, true);
		if (type == 2) PotionEffects.SLOW_DIGGING.addPotionEffect(participant.getPlayer(), 10000, 1, true);
		for (Location loc : circle.toLocations(participant.getPlayer().getLocation())) {
			if (type == 1) loc.add(0, 1.6, 0);
			if (type == 2) loc.add(0, 1, 0);
			if (type == 3) loc.add(0, 0.5, 0);
			ParticleLib.REDSTONE.spawnParticle(loc, COLOR);
		}
		super.run(count);
	}

	@Override
	protected void onEnd() {
		onSilentEnd();
		HandlerList.unregisterAll(this);
		super.onEnd();
	}

	@Override
	protected void onSilentEnd() {
		if (type == 1) participant.getPlayer().removePotionEffect(PotionEffectType.BLINDNESS);
		if (type == 2) participant.getPlayer().removePotionEffect(PotionEffectType.SLOW_DIGGING);
		HandlerList.unregisterAll(this);
		super.onSilentEnd();
	}
	
}