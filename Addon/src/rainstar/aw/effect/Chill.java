package rainstar.aw.effect;

import daybreak.abilitywar.AbilityWar;
import daybreak.abilitywar.ability.AbilityBase;
import daybreak.abilitywar.ability.AbilityBase.Cooldown;
import daybreak.abilitywar.ability.AbilityBase.Duration;
import daybreak.abilitywar.game.AbstractGame;
import daybreak.abilitywar.game.AbstractGame.GameTimer;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.manager.effect.registry.ApplicationMethod;
import daybreak.abilitywar.game.manager.effect.registry.EffectManifest;
import daybreak.abilitywar.game.manager.effect.registry.EffectRegistry;
import daybreak.abilitywar.game.manager.effect.registry.EffectRegistry.EffectRegistration;
import daybreak.abilitywar.game.manager.effect.registry.EffectType;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.base.minecraft.version.ServerVersion;
import daybreak.abilitywar.utils.library.MaterialX;
import daybreak.abilitywar.utils.library.ParticleLib;
import daybreak.abilitywar.utils.library.PotionEffects;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.data.BlockData;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.material.MaterialData;
import org.bukkit.potion.PotionEffectType;

@EffectManifest(name = "냉기", displayName = "§9냉기", method = ApplicationMethod.UNIQUE_LONGEST, type = {
		EffectType.MOVEMENT_RESTRICTION, EffectType.HEALING_REDUCTION, EffectType.COMBAT_RESTRICTION
}, description = {
		"이동 속도 및 공격 속도가 감소합니다.",
		"회복 효과가 25% 감소합니다.",
		"또한 지속시간 및 쿨타임이 천천히 흐릅니다."
})
@SuppressWarnings("deprecation")
public class Chill extends AbstractGame.Effect implements Listener {

	public static final EffectRegistration<Chill> registration = EffectRegistry.registerEffect(Chill.class);

	public static void apply(Participant participant, TimeUnit timeUnit, int duration) {
		registration.apply(participant, timeUnit, duration);
	}

	private final Participant participant;

	public Chill(Participant participant, TimeUnit timeUnit, int duration) {
		participant.getGame().super(registration, participant, (timeUnit.toTicks(duration) / 2));
		this.participant = participant;
		setPeriod(TimeUnit.TICKS, 2);
	}
	
	@Override
	protected void onStart() {
		Bukkit.getPluginManager().registerEvents(this, AbilityWar.getPlugin());
	}
	
	@EventHandler
	public void onRegainHealth(EntityRegainHealthEvent e) {
		if (participant.getPlayer().equals(e.getEntity())) {
			e.setAmount(e.getAmount() * 0.75);
		}
	}

	@Override
	protected void run(int count) {
		PotionEffects.SLOW.addPotionEffect(participant.getPlayer(), 10000, 0, true);
		PotionEffects.SLOW_DIGGING.addPotionEffect(participant.getPlayer(), 10000, 0, true);
		if (count % 20 == 0) {
			if (getGame().getParticipant(participant.getPlayer()).hasAbility() && !getGame().getParticipant(participant.getPlayer()).getAbility().isRestricted()) {
				AbilityBase ab = getGame().getParticipant(participant.getPlayer()).getAbility();
				for (GameTimer t : ab.getTimers()) {
					if (t instanceof Cooldown.CooldownTimer || t instanceof Duration.DurationTimer) {
						t.setCount(t.getCount() + 1);
					}
				}
			}
		}
		if (ServerVersion.getVersion() >= 13) {
			BlockData ice = MaterialX.ICE.getMaterial().createBlockData();
			BlockData diamond = MaterialX.DIAMOND_BLOCK.getMaterial().createBlockData();
			ParticleLib.FALLING_DUST.spawnParticle(participant.getPlayer().getLocation(), 0.2, 1, 0.2, 1, 0, ice);
			ParticleLib.FALLING_DUST.spawnParticle(participant.getPlayer().getLocation(), 0.2, 1, 0.2, 1, 0, diamond);
		} else {
			ParticleLib.FALLING_DUST.spawnParticle(participant.getPlayer().getLocation(), 0.2, 1, 0.2, 1, 0, new MaterialData(Material.ICE));
			ParticleLib.FALLING_DUST.spawnParticle(participant.getPlayer().getLocation(), 0.2, 1, 0.2, 1, 0, new MaterialData(Material.DIAMOND_BLOCK));	
		}
		super.run(count);
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