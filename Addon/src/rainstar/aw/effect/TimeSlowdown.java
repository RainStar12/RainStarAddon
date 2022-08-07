package rainstar.aw.effect;

import daybreak.abilitywar.ability.AbilityBase;
import daybreak.abilitywar.ability.AbilityBase.Cooldown;
import daybreak.abilitywar.game.AbstractGame;
import daybreak.abilitywar.game.AbstractGame.GameTimer;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.manager.effect.registry.ApplicationMethod;
import daybreak.abilitywar.game.manager.effect.registry.EffectManifest;
import daybreak.abilitywar.game.manager.effect.registry.EffectRegistry;
import daybreak.abilitywar.game.manager.effect.registry.EffectRegistry.EffectRegistration;
import daybreak.abilitywar.game.manager.effect.registry.EffectType;
import daybreak.abilitywar.utils.base.color.RGB;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.base.math.geometry.Points;
import daybreak.abilitywar.utils.library.ParticleLib;
import org.bukkit.Location;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;

@EffectManifest(name = "�ð� ��ȭ", displayName = "��3�ð� ��ȭ", method = ApplicationMethod.UNIQUE_STACK, type = {
		EffectType.ABILITY_RESTRICTION
}, description = {
		"��Ÿ���� �� �ʸ��� 1�ʾ� �þ�ϴ�."
})
public class TimeSlowdown extends AbstractGame.Effect implements Listener {

	public static final EffectRegistration<TimeSlowdown> registration = EffectRegistry.registerEffect(TimeSlowdown.class);

	public static void apply(Participant participant, TimeUnit timeUnit, int duration) {
		registration.apply(participant, timeUnit, duration);
	}

	private final Participant participant;
	private static final RGB color = RGB.of(134, 229, 127);

	public TimeSlowdown(Participant participant, TimeUnit timeUnit, int duration) {
		participant.getGame().super(registration, participant, (timeUnit.toTicks(duration) / 20));
		this.participant = participant;
		setPeriod(TimeUnit.SECONDS, 1);
	}
	
	private static final Points Particles = Points.of(0.06, new boolean[][]{
		{false, false, false, false, false, false, true, true, true, true, true, true, true, false, false, false, false, false, false},
		{false, false, false, false, true, true, false, false, false, false, false, false, false, true, true, false, false, false, false},
		{false, false, false, true, false, false, false, false, false, false, false, false, false, false, false, true, false, false, false},
		{false, false, true, false, false, false, false, false, false, false, false, false, false, false, false, false, true, false, false},
		{false, true, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, true, false},
		{false, true, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, true, false},
		{true, false, false, false, false, false, true, false, false, false, false, false, false, false, true, true, false, false, true},
		{true, false, false, false, false, false, false, true, false, false, false, false, true, true, false, false, false, false, true},
		{true, false, false, false, false, false, false, false, true, false, true, true, false, false, false, false, false, false, true},
		{true, false, false, false, false, false, false, false, false, true, false, false, false, false, false, false, false, false, true},
		{true, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, true},
		{true, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, true},
		{true, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, true},
		{false, true, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, true, false},
		{false, true, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, true, false},
		{false, false, true, false, false, false, false, false, false, false, false, false, false, false, false, false, true, false, false},
		{false, false, false, true, false, false, false, false, false, false, false, false, false, false, false, true, false, false, false},
		{false, false, false, false, true, true, false, false, false, false, false, false, false, true, true, false, false, false, false},
		{false, false, false, false, false, false, true, true, true, true, true, true, true, false, false, false, false, false, false}
		});
	
	@Override
	protected void run(int count) {
		if (participant.hasAbility() && !participant.getAbility().isRestricted()) {
			AbilityBase ab = participant.getAbility();
			for (GameTimer t : ab.getTimers()) {
				if (t instanceof Cooldown.CooldownTimer) {
					if (t.getCount() != 0) {
						t.setCount(t.getCount() + 1);
						final Location headLocation = participant.getPlayer().getEyeLocation().clone().add(0, 1.5, 0);
						final Location baseLocation = headLocation.clone().subtract(0, 1.4, 0);
						final float yaw = participant.getPlayer().getLocation().getYaw();
						for (Location loc : Particles.rotateAroundAxisY(-yaw).toLocations(baseLocation)) {
							ParticleLib.REDSTONE.spawnParticle(loc, color);
						}
						Particles.rotateAroundAxisY(yaw);	
					}
				}
			}
		}		
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