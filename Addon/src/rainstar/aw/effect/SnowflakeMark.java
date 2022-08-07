package rainstar.aw.effect;

import daybreak.abilitywar.AbilityWar;
import daybreak.abilitywar.game.AbstractGame;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.manager.effect.event.ParticipantEffectApplyEvent;
import daybreak.abilitywar.game.manager.effect.registry.ApplicationMethod;
import daybreak.abilitywar.game.manager.effect.registry.EffectConstructor;
import daybreak.abilitywar.game.manager.effect.registry.EffectManifest;
import daybreak.abilitywar.game.manager.effect.registry.EffectRegistry;
import daybreak.abilitywar.game.manager.effect.registry.EffectRegistry.EffectRegistration;
import daybreak.abilitywar.game.manager.effect.registry.EffectType;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.base.minecraft.nms.NMS;
import daybreak.google.common.base.Strings;
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
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

import java.util.UUID;

@EffectManifest(name = "눈꽃 표식", displayName = "§b눈꽃 표식", method = ApplicationMethod.UNIQUE_LONGEST, type = {
		EffectType.COMBAT_RESTRICTION
}, description = {
		"방어력을 레벨당 2씩 감소시킵니다.",
		"화염계 피해를 받으면 피해를 줄이고 효과가 빠르게 사라집니다."
})

public class SnowflakeMark extends AbstractGame.Effect implements Listener {

	public static final EffectRegistration<SnowflakeMark> registration = EffectRegistry.registerEffect(SnowflakeMark.class);

	public static void apply(Participant participant, TimeUnit timeUnit, int duration, int level) {
		registration.apply(participant, timeUnit, duration, "with-level", level);
	}

	private final Participant participant;
	private AttributeModifier snowflake;
	private final ArmorStand hologram;
	private int level;

	@EffectConstructor(name = "with-level")
	public SnowflakeMark(Participant participant, TimeUnit timeUnit, int duration, int level) {
		participant.getGame().super(registration, participant, (timeUnit.toTicks(duration) / 2));
		this.participant = participant;
		setPeriod(TimeUnit.TICKS, 2);
		final Location location = participant.getPlayer().getLocation();
		this.hologram = location.getWorld().spawn(location.clone().add(0, 2.2, 0), ArmorStand.class);
		hologram.setVisible(false);
		hologram.setGravity(false);
		hologram.setInvulnerable(true);
		NMS.removeBoundingBox(hologram);
		hologram.setCustomNameVisible(true);
		hologram.setCustomName(Strings.repeat("§b❄", level));
		this.level = level;
	}
	
	@EventHandler
	private void onParticipantEffectApply(ParticipantEffectApplyEvent e) {
		if (e.getParticipant().equals(participant)) {
			if (e.getEffectType().equals(SnowflakeMark.registration)) {
				if (e.getDuration() > this.getCount()) {
					this.stop(false);
				}
			}	
		}
	}
	
	@EventHandler
	private void onEntityDamage(EntityDamageEvent e) {
		if (e.getEntity().equals(participant.getPlayer())) {
			if (e.getCause() == DamageCause.FIRE || e.getCause() == DamageCause.FIRE_TICK || 
					e.getCause() == DamageCause.HOT_FLOOR || e.getCause() == DamageCause.LAVA) {
				e.setDamage(e.getDamage() / 3);
				this.setCount((int) (this.getCount() - (e.getFinalDamage() * 30)));
			}
		}
	}
	
	public int getLevel() {
		return level;
	}

	@Override
	protected void run(int count) {
		hologram.teleport(participant.getPlayer().getLocation().clone().add(0, 2.2, 0));
		super.run(count);
	}
	
	@Override
	protected void onStart() {
		this.snowflake = new AttributeModifier(UUID.randomUUID(), "snowflake", -(level * 2), Operation.ADD_NUMBER);
		Bukkit.getPluginManager().registerEvents(this, AbilityWar.getPlugin());
		participant.getPlayer().getAttribute(Attribute.GENERIC_ARMOR).addModifier(snowflake);
		super.onStart();
	}

	@Override
	protected void onEnd() {
		hologram.remove();
		participant.getPlayer().getAttribute(Attribute.GENERIC_ARMOR).removeModifier(snowflake);
		HandlerList.unregisterAll(this);
		super.onEnd();
	}

	@Override
	protected void onSilentEnd() {
		hologram.remove();
		participant.getPlayer().getAttribute(Attribute.GENERIC_ARMOR).removeModifier(snowflake);
		HandlerList.unregisterAll(this);
		super.onSilentEnd();
	}
	
}