package RainStarEffect;

import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerVelocityEvent;
import org.bukkit.potion.PotionEffectType;

import daybreak.abilitywar.AbilityWar;
import daybreak.abilitywar.game.AbstractGame;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.manager.effect.registry.ApplicationMethod;
import daybreak.abilitywar.game.manager.effect.registry.EffectManifest;
import daybreak.abilitywar.game.manager.effect.registry.EffectRegistry;
import daybreak.abilitywar.game.manager.effect.registry.EffectType;
import daybreak.abilitywar.game.manager.effect.registry.EffectRegistry.EffectRegistration;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.library.PotionEffects;

@EffectManifest(name = "시간 왜곡", displayName = "§3시간 왜곡", method = ApplicationMethod.UNIQUE_LONGEST, type = {
		EffectType.MOVEMENT_RESTRICTION, EffectType.COMBAT_RESTRICTION
}, description = {
		"이동 속도 및 공격 속도가 감소합니다.",
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