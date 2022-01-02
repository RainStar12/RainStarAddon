package RainStarEffect;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

import daybreak.abilitywar.AbilityWar;
import daybreak.abilitywar.game.AbstractGame;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.manager.effect.registry.ApplicationMethod;
import daybreak.abilitywar.game.manager.effect.registry.EffectConstructor;
import daybreak.abilitywar.game.manager.effect.registry.EffectManifest;
import daybreak.abilitywar.game.manager.effect.registry.EffectRegistry;
import daybreak.abilitywar.game.manager.effect.registry.EffectRegistry.EffectRegistration;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;

@EffectManifest(name = "도발", displayName = "§c도발", method = ApplicationMethod.UNIQUE_LONGEST, type = {
}, description = {
		"도발한 사람을 근접 타격할 때 추가 피해를 줄 수 있습니다."
})
public class Agro extends AbstractGame.Effect implements Listener {

	public static final EffectRegistration<Agro> registration = EffectRegistry.registerEffect(Agro.class);

	public static void apply(Participant participant, TimeUnit timeUnit, int duration, Player applyPlayer, int increasedDamage) {
		registration.apply(participant, timeUnit, duration, "with-player", applyPlayer, increasedDamage);
	}

	private final Participant participant;
	private final Player applyPlayer;
	private final int increasedDamage;

	@EffectConstructor(name = "with-player")
	public Agro(Participant participant, TimeUnit timeUnit, int duration, Player applyPlayer, int increasedDamage) {
		participant.getGame().super(registration, participant, timeUnit.toTicks(duration));
		this.participant = participant;
		this.applyPlayer = applyPlayer;
		this.increasedDamage = increasedDamage;
		setPeriod(TimeUnit.TICKS, 1);
	}

	@Override
	protected void onStart() {
		Bukkit.getPluginManager().registerEvents(this, AbilityWar.getPlugin());
		super.onStart();
	}

	@EventHandler
	private void onEntityDamageByEntity(EntityDamageByEntityEvent e) {
		if (e.getEntity().equals(applyPlayer) && e.getDamager().equals(participant.getPlayer())) {
			e.setDamage(e.getDamage() + increasedDamage);
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