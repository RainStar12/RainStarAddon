package RainStarEffect;

import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityRegainHealthEvent;

import daybreak.abilitywar.AbilityWar;
import daybreak.abilitywar.game.AbstractGame;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.manager.effect.registry.ApplicationMethod;
import daybreak.abilitywar.game.manager.effect.registry.EffectManifest;
import daybreak.abilitywar.game.manager.effect.registry.EffectRegistry;
import daybreak.abilitywar.game.manager.effect.registry.EffectRegistry.EffectRegistration;
import daybreak.abilitywar.game.manager.effect.registry.EffectType;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.library.ParticleLib;

@EffectManifest(name = "회복 불능", displayName = "§c회복 불능", method = ApplicationMethod.UNIQUE_LONGEST, type = {
		EffectType.HEALING_BAN
}, description = {
		"모든 회복 효과를 받을 수 없습니다."
})
public class Irreparable extends AbstractGame.Effect implements Listener {

	public static final EffectRegistration<Irreparable> registration = EffectRegistry.registerEffect(Irreparable.class);

	public static void apply(Participant participant, TimeUnit timeUnit, int duration) {
		registration.apply(participant, timeUnit, duration);
	}

	private final Participant participant;

	public Irreparable(Participant participant, TimeUnit timeUnit, int duration) {
		participant.getGame().super(registration, participant, (timeUnit.toTicks(duration) / 20));
		this.participant = participant;
		setPeriod(TimeUnit.SECONDS, 1);
	}

	@Override
	protected void onStart() {
		Bukkit.getPluginManager().registerEvents(this, AbilityWar.getPlugin());
		super.onStart();
	}

	@EventHandler
	private void onEntityRegainHealth(final EntityRegainHealthEvent e) {
		if (e.getEntity().getUniqueId().equals(participant.getPlayer().getUniqueId())) {
			ParticleLib.VILLAGER_ANGRY.spawnParticle(e.getEntity().getLocation().add(0, 1.5, 0), 0, 0, 0, 10, 1);
			e.setCancelled(true);
		}
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
