package rainstar.abilitywar.effect;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.entity.Zombie;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

import daybreak.abilitywar.AbilityWar;
import daybreak.abilitywar.game.AbstractGame;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.GameManager;
import daybreak.abilitywar.game.list.mix.AbstractMix;
import daybreak.abilitywar.game.list.mix.Mix;
import daybreak.abilitywar.game.manager.effect.Infection;
import daybreak.abilitywar.game.manager.effect.registry.ApplicationMethod;
import daybreak.abilitywar.game.manager.effect.registry.EffectManifest;
import daybreak.abilitywar.game.manager.effect.registry.EffectRegistry;
import daybreak.abilitywar.game.manager.effect.registry.EffectType;
import daybreak.abilitywar.game.manager.effect.registry.EffectRegistry.EffectRegistration;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.base.minecraft.version.ServerVersion;

@EffectManifest(name = "침식", displayName = "§2침식", method = ApplicationMethod.UNIQUE_STACK, type = {
		EffectType.MOVEMENT_RESTRICTION, EffectType.SIGHT_RESTRICTION
}, description = {
		"신체가 좀비화되어 통제할 수 없게 됩니다.",
		"좀비가 공격한 대상을 1.5초간 §5§n감염§f시킵니다.",
		"이 상태에서 적에게 사망할 경우 본인의 능력을 상대에게 감염시킵니다."
})
public class Erosion extends AbstractGame.Effect implements Listener {

	public static final EffectRegistration<Erosion> registration = EffectRegistry.registerEffect(Erosion.class);

	public static void apply(Participant participant, TimeUnit timeUnit, int duration) {
		registration.apply(participant, timeUnit, duration);
	}

	private final Participant participant;
	private final Zombie zombie;
			
	public Erosion(Participant participant, TimeUnit timeUnit, int duration) {
		participant.getGame().super(registration, participant, timeUnit.toTicks(duration));
		this.participant = participant;
		participant.getPlayer().setGameMode(GameMode.SPECTATOR);
		this.zombie = participant.getPlayer().getWorld().spawn(participant.getPlayer().getLocation(), Zombie.class);
		
		if (ServerVersion.getVersion() >= 16) zombie.setAdult();
		else zombie.setBaby(false);
		zombie.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).setBaseValue(0.26);
		
		zombie.getEquipment().setItemInMainHand(null);
		zombie.getEquipment().setArmorContents(participant.getPlayer().getInventory().getArmorContents());
		zombie.getEquipment().setItemInMainHandDropChance(0);
		zombie.getEquipment().setBootsDropChance(0);
		zombie.getEquipment().setChestplateDropChance(0);
		zombie.getEquipment().setHelmetDropChance(0);
		zombie.getEquipment().setLeggingsDropChance(0);
		zombie.setCustomName("§2침식된 " + participant.getPlayer().getName());
		zombie.setCustomNameVisible(true);
		
		zombie.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(participant.getPlayer().getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue());
		zombie.setHealth(participant.getPlayer().getHealth());
		
		participant.getPlayer().setSpectatorTarget(zombie);
		
		setPeriod(TimeUnit.TICKS, 1);
	}

	@Override
	protected void onStart() {
		Bukkit.getPluginManager().registerEvents(this, AbilityWar.getPlugin());
		super.onStart();
	}
	
	@Override
	protected void run(int count) {
		if (zombie.isDead()) this.stop(true);
		else participant.getPlayer().setSpectatorTarget(zombie);
		super.run(count);
	}

	
	@Override
	protected void onEnd() {
		onSilentEnd();
	}

	@Override
	protected void onSilentEnd() {
		participant.getPlayer().setGameMode(GameMode.SURVIVAL);
		if (zombie.isDead()) {
			participant.getPlayer().damage(Integer.MAX_VALUE);
			final Participant killerParticipant = GameManager.getGame().getParticipant(zombie.getKiller());
			if (killerParticipant != null && participant.getAbility() != null) {
				if (getGame() instanceof AbstractMix) {
					Mix mix = (Mix) participant.getAbility();
					Mix targetMix = (Mix) killerParticipant.getAbility();
					try {
						targetMix.setAbility(mix.getFirst().getRegistration(), mix.getSecond().getRegistration());
					} catch (ReflectiveOperationException ignored) {}
				} else {
					try {
						killerParticipant.setAbility(participant.getAbility().getRegistration());
					} catch (ReflectiveOperationException ignored) {}
				}
			}
		}
		else participant.getPlayer().setHealth(zombie.getHealth());
		HandlerList.unregisterAll(this);
		super.onSilentEnd();
	}
	
	@EventHandler()
	private void onEntityDamageByEntity(EntityDamageByEntityEvent e) {
		if (e.getDamager().equals(zombie) && e.getEntity() instanceof Player) {
			if (getGame().getParticipant((Player) e.getEntity()) != null) {
				Participant p = getGame().getParticipant((Player) e.getEntity());
				Infection.apply(p, TimeUnit.TICKS, 30);
			}
		}
	}
	
}