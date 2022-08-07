package rainstar.aw.effect;

import daybreak.abilitywar.game.AbstractGame;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.manager.effect.registry.ApplicationMethod;
import daybreak.abilitywar.game.manager.effect.registry.EffectConstructor;
import daybreak.abilitywar.game.manager.effect.registry.EffectManifest;
import daybreak.abilitywar.game.manager.effect.registry.EffectRegistry;
import daybreak.abilitywar.game.manager.effect.registry.EffectRegistry.EffectRegistration;
import daybreak.abilitywar.game.manager.effect.registry.EffectType;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.base.math.VectorUtil;
import daybreak.abilitywar.utils.base.random.Random;
import daybreak.abilitywar.utils.library.SoundLib;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.util.Vector;

@EffectManifest(name = "혼란", displayName = "§6혼란", method = ApplicationMethod.MULTIPLE, type = {
		EffectType.MOVEMENT_INTERRUPT
}, description = {
		"전후좌우의 무작위 방향으로 튕겨나갑니다."
})
public class Confusion extends AbstractGame.Effect implements Listener {

	public static final EffectRegistration<Confusion> registration = EffectRegistry.registerEffect(Confusion.class);

	public static void apply(Participant participant, TimeUnit timeUnit, int duration, int period) {
		registration.apply(participant, timeUnit, duration, "with-period", period);
	}

	private final Participant participant;
	private Random random = new Random();

	@EffectConstructor(name = "with-period")
	public Confusion(Participant participant, TimeUnit timeUnit, int duration, int period) {
		participant.getGame().super(registration, participant, (timeUnit.toTicks(duration) / period));
		this.participant = participant;
		setPeriod(TimeUnit.TICKS, period);
	}
	
	@Override
	protected void run(int count) {
		participant.getPlayer().setVelocity(VectorUtil.validateVector(new Vector((((random.nextDouble() * 2) - 1) * 0.9), 0, (((random.nextDouble() * 2) - 1)) * 0.9)));
		SoundLib.BLOCK_PISTON_EXTEND.playSound(participant.getPlayer(), 1, 1.5f);
		super.run(count);
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