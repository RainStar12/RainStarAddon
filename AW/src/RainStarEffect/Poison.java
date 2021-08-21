package RainStarEffect;

import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityRegainHealthEvent;
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
import daybreak.abilitywar.utils.library.MaterialX;
import daybreak.abilitywar.utils.library.ParticleLib;
import daybreak.abilitywar.utils.library.PotionEffects;

@EffectManifest(name = "중독", displayName = "§2중독", method = ApplicationMethod.UNIQUE_LONGEST, type = {
		EffectType.HEALING_BAN
}, description = {
		"§2중독§f되어 지속적 피해를 입습니다.",
		"§f체력이 반 칸 이하일 때는 피해를 입히지 않고,",
		"§f중독 도중 회복 효과를 받을 때 대신 피해를 받습니다."
})
public class Poison extends AbstractGame.Effect implements Listener {

	public static final EffectRegistration<Poison> registration = EffectRegistry.registerEffect(Poison.class);

	public static void apply(Participant participant, TimeUnit timeUnit, int duration) {
		registration.apply(participant, timeUnit, duration);
	}

	private final Participant participant;

	public Poison(Participant participant, TimeUnit timeUnit, int duration) {
		participant.getGame().super(registration, participant, (timeUnit.toTicks(duration) / 2));
		this.participant = participant;
		setPeriod(TimeUnit.TICKS, 2);
	}
	
	@Override
	protected void onStart() {
		Bukkit.getPluginManager().registerEvents(this, AbilityWar.getPlugin());
	}
	
	@EventHandler
	public void onEntityRegainHealth(EntityRegainHealthEvent e) {
		if (participant.getPlayer().equals(e.getEntity())) {
			e.setCancelled(true);
			if (participant.getPlayer().getHealth() - e.getAmount() > 0) {
				participant.getPlayer().setNoDamageTicks(0);
				participant.getPlayer().damage(e.getAmount());
			}
		}
	}

	@Override
	protected void run(int count) {
		if (count % 10 == 0) PotionEffects.POISON.addPotionEffect(participant.getPlayer(), 25, 0, true);
		if (count % 2 == 0) ParticleLib.ITEM_CRACK.spawnParticle(participant.getPlayer().getLocation(), 0.3, 1, 0.3, 25, 0, MaterialX.SLIME_BLOCK);
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
		participant.getPlayer().removePotionEffect(PotionEffectType.POISON);
		HandlerList.unregisterAll(this);
		super.onSilentEnd();
	}
	
}