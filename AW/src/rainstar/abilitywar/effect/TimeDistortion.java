package rainstar.abilitywar.effect;

import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent.RegainReason;
import org.bukkit.event.player.PlayerVelocityEvent;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import daybreak.abilitywar.AbilityWar;
import daybreak.abilitywar.game.AbstractGame;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.manager.effect.registry.ApplicationMethod;
import daybreak.abilitywar.game.manager.effect.registry.EffectManifest;
import daybreak.abilitywar.game.manager.effect.registry.EffectRegistry;
import daybreak.abilitywar.game.manager.effect.registry.EffectType;
import daybreak.abilitywar.game.manager.effect.registry.EffectRegistry.EffectRegistration;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.base.minecraft.entity.health.Healths;
import daybreak.abilitywar.utils.library.PotionEffects;

@EffectManifest(name = "시간 왜곡", displayName = "§3시간 왜곡", method = ApplicationMethod.UNIQUE_LONGEST, type = {
		EffectType.MOVEMENT_RESTRICTION, EffectType.COMBAT_RESTRICTION
}, description = {
		"이동 / 공격 / 회복 속도가 감속됩니다.",
		"또한 모든 벡터 영향을 절반만 받습니다."
})
public class TimeDistortion extends AbstractGame.Effect implements Listener {

	public static final EffectRegistration<TimeDistortion> registration = EffectRegistry.registerEffect(TimeDistortion.class);

	public static void apply(Participant participant, TimeUnit timeUnit, int duration) {
		registration.apply(participant, timeUnit, duration);
	}

	private final Participant participant;

	public TimeDistortion(Participant participant, TimeUnit timeUnit, int duration) {
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
		PotionEffects.SLOW.addPotionEffect(participant.getPlayer(), 20, 2, true);
		PotionEffects.SLOW_DIGGING.addPotionEffect(participant.getPlayer(), 20, 2, true);
		super.run(count);
	}

	@EventHandler
	private void onVelocity(PlayerVelocityEvent e) {
		if (e.getPlayer().equals(participant.getPlayer())) {
			e.setVelocity(e.getVelocity().multiply(0.5));
		}
	}
	
	@EventHandler
	private void onEntityRegainHealth(EntityRegainHealthEvent e) {
		if (e.getEntity().equals(participant.getPlayer()) && !e.getRegainReason().equals(RegainReason.ENDER_CRYSTAL)) {
			double amount = e.getAmount();
			e.setAmount(amount / 2);
			new BukkitRunnable() {
				@Override
				public void run() {
					final EntityRegainHealthEvent event = new EntityRegainHealthEvent(e.getEntity(), amount / 2, RegainReason.ENDER_CRYSTAL);
					Bukkit.getPluginManager().callEvent(event);
					if (!event.isCancelled()) {
						Healths.setHealth(participant.getPlayer(), participant.getPlayer().getHealth() + amount / 2);
					}
				}
			}.runTaskLater(AbilityWar.getPlugin(), 30L);	
		}
	}
	
	@Override
	protected void onEnd() {
		participant.getPlayer().removePotionEffect(PotionEffectType.SLOW);
		participant.getPlayer().removePotionEffect(PotionEffectType.SLOW_DIGGING);
		HandlerList.unregisterAll(this);
		super.onEnd();
	}

	@Override
	protected void onSilentEnd() {
		participant.getPlayer().removePotionEffect(PotionEffectType.SLOW);
		participant.getPlayer().removePotionEffect(PotionEffectType.SLOW_DIGGING);
		HandlerList.unregisterAll(this);
		super.onSilentEnd();
	}
	
}