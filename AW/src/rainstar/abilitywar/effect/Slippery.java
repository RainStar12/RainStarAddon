package rainstar.abilitywar.effect;

import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerVelocityEvent;
import org.bukkit.util.Vector;

import daybreak.abilitywar.AbilityWar;
import daybreak.abilitywar.game.AbstractGame;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.manager.effect.registry.ApplicationMethod;
import daybreak.abilitywar.game.manager.effect.registry.EffectManifest;
import daybreak.abilitywar.game.manager.effect.registry.EffectRegistry;
import daybreak.abilitywar.game.manager.effect.registry.EffectType;
import daybreak.abilitywar.game.manager.effect.registry.EffectRegistry.EffectRegistration;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.base.random.Random;
import daybreak.abilitywar.utils.library.MaterialX;
import daybreak.abilitywar.utils.library.ParticleLib;

@EffectManifest(name = "미끌거림", displayName = "§b미끌거림", method = ApplicationMethod.UNIQUE_STACK, type = {
		EffectType.MOVEMENT_INTERRUPT
}, description = {
		"§f때때로 이동량이나 밀/끌어당기는 힘이 증가됩니다."
})
public class Slippery extends AbstractGame.Effect implements Listener {

	public static final EffectRegistration<Slippery> registration = EffectRegistry.registerEffect(Slippery.class);

	public static void apply(Participant participant, TimeUnit timeUnit, int duration) {
		registration.apply(participant, timeUnit, duration);
	}

	private final Participant participant;
	private final Random random = new Random();

	public Slippery(Participant participant, TimeUnit timeUnit, int duration) {
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
		super.run(count);
		ParticleLib.ITEM_CRACK.spawnParticle(participant.getPlayer().getLocation().clone().add(0, 0.2, 0), 0.5, 0.1, 0.5, 5, 0.05, MaterialX.PACKED_ICE);
		ParticleLib.ITEM_CRACK.spawnParticle(participant.getPlayer().getLocation().clone().add(0, 0.2, 0), 0.5, 0.1, 0.5, 5, 0.05, MaterialX.ICE);
	}

	
	@Override
	protected void onEnd() {
		onSilentEnd();
	}

	@Override
	protected void onSilentEnd() {
		HandlerList.unregisterAll(this);
		super.onSilentEnd();
	}
	
	@EventHandler()
	private void onPlayerMove(PlayerMoveEvent e) {
		if (e.getPlayer().equals(participant.getPlayer())) {
			if (random.nextInt(100) == 0) {
				if (e.getFrom().distanceSquared(e.getTo()) == 0) return;
			    Vector move = e.getTo().toVector().subtract(e.getFrom().toVector());
			    e.setTo(e.getFrom().clone().add(move.multiply(2)));
			}
		}
	}
	
	@EventHandler()
	public void onVelocity(PlayerVelocityEvent e) {
		if (random.nextInt(10) == 0) {
		    e.setVelocity(e.getVelocity().multiply(2));	
		}
	}
	
}