package rainstar.abilitywar.effect;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import daybreak.abilitywar.game.AbstractGame.Effect;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.manager.effect.registry.ApplicationMethod;
import daybreak.abilitywar.game.manager.effect.registry.EffectManifest;
import daybreak.abilitywar.game.manager.effect.registry.EffectRegistry;
import daybreak.abilitywar.game.manager.effect.registry.EffectType;
import daybreak.abilitywar.game.manager.effect.registry.EffectRegistry.EffectRegistration;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.base.math.LocationUtil;
import daybreak.abilitywar.utils.base.minecraft.nms.NMS;

@EffectManifest(name = "시야 고정", displayName = "§5시야 고정", method = ApplicationMethod.UNIQUE_LONGEST, type = {
		EffectType.SIGHT_RESTRICTION
}, description = {
		"상태이상을 받을 때의 시점으로 시야가 고정됩니다."
})
public class SightLock extends Effect {

	public static final EffectRegistration<SightLock> registration = EffectRegistry.registerEffect(SightLock.class);

	public static void apply(Participant participant, TimeUnit timeUnit, int duration) {
		registration.apply(participant, timeUnit, duration);
	}

	private final Participant participant;
	private final float yaw, pitch;

	public SightLock(Participant participant, TimeUnit timeUnit, int duration) {
		participant.getGame().super(registration, participant, timeUnit.toTicks(duration));
		this.participant = participant;
		Vector direction = participant.getPlayer().getLocation().getDirection();
		this.yaw = LocationUtil.getYaw(direction);
		this.pitch = LocationUtil.getPitch(direction);
		setPeriod(TimeUnit.TICKS, 1);
	}
	
	@Override
	protected void run(int count) {
		for (Player player : Bukkit.getOnlinePlayers()) {
		    NMS.rotateHead(player, participant.getPlayer(), yaw, pitch);	
		}
		super.run(count);
	}

	@Override
	protected void onEnd() {
		onSilentEnd();
		super.onEnd();
	}

	@Override
	protected void onSilentEnd() {
		super.onSilentEnd();
	}
	
}
