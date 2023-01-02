package rainstar.abilitywar.effect;

import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import daybreak.abilitywar.AbilityWar;
import daybreak.abilitywar.game.AbstractGame;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.manager.effect.registry.ApplicationMethod;
import daybreak.abilitywar.game.manager.effect.registry.EffectManifest;
import daybreak.abilitywar.game.manager.effect.registry.EffectRegistry;
import daybreak.abilitywar.game.manager.effect.registry.EffectRegistry.EffectRegistration;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.library.ParticleLib;

@EffectManifest(name = "화상", displayName = "§4화상", method = ApplicationMethod.UNIQUE_LONGEST, type = {
}, description = {
		"모든 화염계 피해를 2.5배로 받습니다.",
		"또한 화염계 피해를 무시할 수 없게 되며,",
		"화염이 꺼질 때 꺼지기 전의 화염 지속시간에 비례해",
		"추가 피해를 입습니다."
})
public class Burn extends AbstractGame.Effect implements Listener {
	
	public static final EffectRegistration<Burn> registration = EffectRegistry.registerEffect(Burn.class);

	public static void apply(Participant participant, TimeUnit timeUnit, int duration) {
		registration.apply(participant, timeUnit, duration);
	}

	private final Participant participant;
	private int fireticks = 0;
	private int potionticks = 0;

	public Burn(Participant participant, TimeUnit timeUnit, int duration) {
		participant.getGame().super(registration, participant, timeUnit.toTicks(duration));
		this.participant = participant;
		setPeriod(TimeUnit.TICKS, 1);
	}
	
	@Override
	protected void onStart() {
		Bukkit.getPluginManager().registerEvents(this, AbilityWar.getPlugin());
		if (participant.getPlayer().hasPotionEffect(PotionEffectType.FIRE_RESISTANCE)) {
			potionticks = participant.getPlayer().getPotionEffect(PotionEffectType.FIRE_RESISTANCE).getDuration();
			participant.getPlayer().removePotionEffect(PotionEffectType.FIRE_RESISTANCE);
		}
	}
	
	@EventHandler(priority = EventPriority.HIGHEST)
	public void onEntityDamage(EntityDamageEvent e) {
		if (participant.getPlayer().equals(e.getEntity())) {
    		if (e.getCause() == DamageCause.FIRE || e.getCause() == DamageCause.FIRE_TICK ||
					e.getCause() == DamageCause.LAVA || e.getCause() == DamageCause.HOT_FLOOR) {
    			if (e.isCancelled()) {
    				e.setCancelled(false);
    			}
    			e.setDamage(e.getDamage() * 2.5);
    		}
		}
	}

	@Override
	protected void run(int count) {
		if (count % 4 == 0) ParticleLib.SMOKE_LARGE.spawnParticle(participant.getPlayer().getLocation(), 0, 0, 0, 1, 0);
		if (count % 20 == 0) ParticleLib.DRIP_LAVA.spawnParticle(participant.getPlayer().getLocation().clone().add(0, 1, 0), 0.25, 0.5, 0.25, 20, 1);
		if (participant.getPlayer().getFireTicks() <= 0) {
			if (fireticks > 0) {
				participant.getPlayer().damage(Math.min(6, fireticks * 0.025));
				ParticleLib.SMOKE_LARGE.spawnParticle(participant.getPlayer().getLocation(), 0, 0, 0, 150, 0.05);
			}
		}
		fireticks = participant.getPlayer().getFireTicks();
		super.run(count);
	}

	@Override
	protected void onEnd() {
		onSilentEnd();
		super.onEnd();
	}

	@Override
	protected void onSilentEnd() {
		participant.getPlayer().addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, potionticks, 0));
		HandlerList.unregisterAll(this);
		super.onSilentEnd();
	}
	
}