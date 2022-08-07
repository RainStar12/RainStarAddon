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
import daybreak.abilitywar.utils.base.minecraft.nms.NMS;
import daybreak.abilitywar.utils.library.MaterialX;
import daybreak.abilitywar.utils.library.ParticleLib;
import daybreak.abilitywar.utils.library.PotionEffects;
import daybreak.abilitywar.utils.library.SoundLib;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.attribute.AttributeModifier.Operation;
import org.bukkit.entity.ArmorStand;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.UUID;

@EffectManifest(name = "시간 간섭", displayName = "§3시간 간섭", method = ApplicationMethod.UNIQUE_STACK, type = {
		EffectType.ABILITY_RESTRICTION, EffectType.MOVEMENT_INTERRUPT, EffectType.COMBAT_RESTRICTION
}, description = {
		" §f이동 속도§7 · §f공격 속도§7 · §f쿨타임이 §e느리게 흐르고",
		" §f지속 시간§7 · §f기본 무적 시간이 §b빠르게 흐릅니다§f."
})
public class TimeInterrupt extends AbstractGame.Effect implements Listener {

	public static final EffectRegistration<TimeInterrupt> registration = EffectRegistry.registerEffect(TimeInterrupt.class);

	public static void apply(Participant participant, TimeUnit timeUnit, int duration) {
		registration.apply(participant, timeUnit, duration);
	}

	private static final ItemStack CLOCK = MaterialX.CLOCK.createItem();
	
	private final Participant participant;
	private ArmorStand armorStand;
	private AttributeModifier attackspeeddown;

	public TimeInterrupt(Participant participant, TimeUnit timeUnit, int duration) {
		participant.getGame().super(registration, participant, timeUnit.toTicks(duration));
		this.participant = participant;
		setPeriod(TimeUnit.TICKS, 1);
		this.armorStand = participant.getPlayer().getWorld().spawn(participant.getPlayer().getLocation(), ArmorStand.class);
		armorStand.setVisible(false);
		armorStand.setInvulnerable(true);
		armorStand.setGravity(false);
		armorStand.getEquipment().setHelmet(CLOCK);;
		NMS.removeBoundingBox(armorStand);
	}
	
	@Override
	protected void onStart() {
		Bukkit.getPluginManager().registerEvents(this, AbilityWar.getPlugin());
		attackspeeddown = new AttributeModifier(UUID.randomUUID(), "attackspeeddown", -0.5, Operation.ADD_NUMBER);
		participant.getPlayer().getAttribute(Attribute.GENERIC_ATTACK_SPEED).addModifier(attackspeeddown);
		super.onStart();
	}
	
	@Override
	protected void run(int count) {
		PotionEffects.SLOW.addPotionEffect(participant.getPlayer(), 10000, 0, true);
		armorStand.teleport(participant.getPlayer().getLocation().clone().add(0, 0.25, 0).setDirection(participant.getPlayer().getLocation().getDirection().setY(0)));
		if (count % 30 == 0) {
			if (count % 60 == 0) SoundLib.BLOCK_NOTE_BLOCK_HAT.playSound(participant.getPlayer(), 1, 0.7f);
			else SoundLib.BLOCK_NOTE_BLOCK_HAT.playSound(participant.getPlayer(), 1, 0.9f);
			ParticleLib.ITEM_CRACK.spawnParticle(armorStand.getLocation().clone().add(0, 2.25, 0), 0, 0, 0, 20, 0.15, MaterialX.CLOCK);
			if (participant.hasAbility() && !participant.getAbility().isRestricted()) {
				AbilityBase ab = participant.getAbility();
				for (GameTimer t : ab.getRunningTimers()) {
					if (t instanceof Cooldown.CooldownTimer) {
						t.setCount(t.getCount() + 1);
					}
					if (t instanceof Duration.DurationTimer) {
						t.setCount(t.getCount() - 1);
					}
				}
			}
		}
		super.run(count);
	}
	
	@EventHandler
	private void onPlayerMove(PlayerMoveEvent e) {
		if (e.getPlayer().getUniqueId().equals(participant.getPlayer().getUniqueId())) {
			final double fromY = e.getFrom().getY(), toY = e.getTo().getY();
			if (toY > fromY) {
				double dx, dy, dz;
				final Location from = e.getFrom(), to = e.getTo();
				dx = to.getX() - from.getX();
				dy = to.getY() - from.getY();
				dz = to.getZ() - from.getZ();
				if (toY - fromY <= 1) {
					e.getPlayer().setVelocity(new Vector((dx * 0.65), (dy * 0.7), (dz * 0.65)));	
				}
			}	
		}
	}
	
	@EventHandler
	private void onEntityDamage(EntityDamageEvent e) {
		if (participant.getPlayer().equals(e.getEntity())) {
			participant.getPlayer().setMaximumNoDamageTicks(15);
		}
	}

	@Override
	protected void onEnd() {
		onSilentEnd();
		super.onEnd();
	}

	@Override
	protected void onSilentEnd() {
		participant.getPlayer().removePotionEffect(PotionEffectType.SLOW);
		participant.getPlayer().setMaximumNoDamageTicks(20);
		participant.getPlayer().getAttribute(Attribute.GENERIC_ATTACK_SPEED).removeModifier(attackspeeddown);
		armorStand.remove();
		HandlerList.unregisterAll(this);
		super.onSilentEnd();
	}
}