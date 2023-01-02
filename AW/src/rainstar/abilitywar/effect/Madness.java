package rainstar.abilitywar.effect;

import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.util.Vector;

import daybreak.abilitywar.AbilityWar;
import daybreak.abilitywar.game.AbstractGame;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.manager.effect.registry.ApplicationMethod;
import daybreak.abilitywar.game.manager.effect.registry.EffectConstructor;
import daybreak.abilitywar.game.manager.effect.registry.EffectManifest;
import daybreak.abilitywar.game.manager.effect.registry.EffectRegistry;
import daybreak.abilitywar.game.manager.effect.registry.EffectType;
import daybreak.abilitywar.game.manager.effect.registry.EffectRegistry.EffectRegistration;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.base.math.VectorUtil;
import daybreak.abilitywar.utils.base.random.Random;
import daybreak.abilitywar.utils.library.SoundLib;

@EffectManifest(name = "광란", displayName = "§5광란", method = ApplicationMethod.UNIQUE_STACK, type = {
		EffectType.MOVEMENT_INTERRUPT, EffectType.COMBAT_RESTRICTION
}, description = {
		"상하전후좌우의 무작위 방향으로 튕겨나갑니다.",
		"또한 엔티티에 의한 피해 외 모든 피해를 1.5배로 받습니다."
})
public class Madness extends AbstractGame.Effect implements Listener {

	public static final EffectRegistration<Madness> registration = EffectRegistry.registerEffect(Madness.class);

	public static void apply(Participant participant, TimeUnit timeUnit, int duration, int period) {
		registration.apply(participant, timeUnit, duration, "with-period", period);
	}

	private final Participant participant;
	private Random random = new Random();
	private int period;

	@EffectConstructor(name = "with-period")
	public Madness(Participant participant, TimeUnit timeUnit, int duration, int period) {
		participant.getGame().super(registration, participant, (timeUnit.toTicks(duration) / period));
		this.participant = participant;
		this.period = period;
		setPeriod(TimeUnit.TICKS, period);
	}
	
	@Override
	protected void onStart() {
		Bukkit.getPluginManager().registerEvents(this, AbilityWar.getPlugin());
	}
	
	@EventHandler
	public void onEntityDamage(EntityDamageEvent e) {
		if (e.getCause() != DamageCause.ENTITY_ATTACK && e.getCause() != DamageCause.ENTITY_SWEEP_ATTACK && e.getCause() != DamageCause.PROJECTILE) {
			if (participant.getPlayer().equals(e.getEntity())) e.setDamage(e.getDamage() * 1.5);
		}
	}
	
	@EventHandler
	public void onPlayerDeath(PlayerDeathEvent e) {
		if (participant.getPlayer().equals(e.getEntity())) {
			this.stop(false);
		}
	}
	
	@Override
	protected void run(int count) {
		participant.getPlayer().setVelocity(VectorUtil.validateVector(new Vector((((random.nextDouble() * 2) - 1) * 0.9), (((random.nextDouble() * 2) - 1) * 0.5), (((random.nextDouble() * 2) - 1)) * 0.9)));
		SoundLib.BLOCK_PISTON_EXTEND.playSound(participant.getPlayer(), 1, 1.5f);
		super.run(count);
	}
	
	public int getDuration() {
		return (getCount() * period);
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