package RainStarEffect;

import java.util.UUID;

import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.attribute.AttributeModifier.Operation;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;

import daybreak.abilitywar.game.AbstractGame;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.manager.effect.registry.ApplicationMethod;
import daybreak.abilitywar.game.manager.effect.registry.EffectManifest;
import daybreak.abilitywar.game.manager.effect.registry.EffectRegistry;
import daybreak.abilitywar.game.manager.effect.registry.EffectType;
import daybreak.abilitywar.game.manager.effect.registry.EffectRegistry.EffectRegistration;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.base.minecraft.nms.NMS;
import daybreak.abilitywar.utils.library.MaterialX;
import daybreak.abilitywar.utils.library.ParticleLib;

@EffectManifest(name = "부식", displayName = "§7부식", method = ApplicationMethod.UNIQUE_LONGEST, type = {
		EffectType.COMBAT_RESTRICTION
}, description = {
		"철과 관련된 아이템을 사용하지 못합니다. 또한 방어력이 감소됩니다.",
		"낮은 등급의 광물을 사용한 갑옷일수록 방어력이 더 많이 감소됩니다."
})
public class Corrosion extends AbstractGame.Effect implements Listener {

	public static final EffectRegistration<Corrosion> registration = EffectRegistry.registerEffect(Corrosion.class);

	public static void apply(Participant participant, TimeUnit timeUnit, int duration) {
		registration.apply(participant, timeUnit, duration);
	}

	private final Participant participant;
	private AttributeModifier armorcorrosion;
	private double level;
	
	private static boolean isIron(final Material material) {
		return material.name().contains("IRON");
	}

	public Corrosion(Participant participant, TimeUnit timeUnit, int duration) {
		participant.getGame().super(registration, participant, (timeUnit.toTicks(duration) / 20));
		this.participant = participant;
		setPeriod(TimeUnit.SECONDS, 1);
	}
	
	@Override
	protected void onStart() {
		if (participant.getPlayer().getInventory().getHelmet() != null) {
			Material helmet = participant.getPlayer().getInventory().getHelmet().getType();
			if (helmet == MaterialX.GOLDEN_HELMET.getMaterial()) level += 2;
			if (helmet == MaterialX.IRON_HELMET.getMaterial()) level += 1;
			if (helmet == MaterialX.DIAMOND_HELMET.getMaterial()) level += 0.5;
		}
		if (participant.getPlayer().getInventory().getChestplate() != null) {
			Material chestplate = participant.getPlayer().getInventory().getChestplate().getType();
			if (chestplate == MaterialX.GOLDEN_CHESTPLATE.getMaterial()) level += 2;
			if (chestplate == MaterialX.IRON_CHESTPLATE.getMaterial()) level += 1;
			if (chestplate == MaterialX.DIAMOND_CHESTPLATE.getMaterial()) level += 0.5;
		}
		if (participant.getPlayer().getInventory().getLeggings() != null) {
			Material leggings = participant.getPlayer().getInventory().getLeggings().getType();
			if (leggings == MaterialX.GOLDEN_LEGGINGS.getMaterial()) level += 2;
			if (leggings == MaterialX.IRON_LEGGINGS.getMaterial()) level += 1;
			if (leggings == MaterialX.DIAMOND_LEGGINGS.getMaterial()) level += 0.5;
		}
		if (participant.getPlayer().getInventory().getBoots() != null) {
			Material boots = participant.getPlayer().getInventory().getBoots().getType();
			if (boots == MaterialX.GOLDEN_BOOTS.getMaterial()) level += 2;
			if (boots == MaterialX.IRON_BOOTS.getMaterial()) level += 1;
			if (boots == MaterialX.DIAMOND_BOOTS.getMaterial()) level += 0.5;
		}
		this.armorcorrosion = new AttributeModifier(UUID.randomUUID(), "armor-corrosion", -(level), Operation.ADD_NUMBER);
		participant.getPlayer().getAttribute(Attribute.GENERIC_ARMOR).addModifier(armorcorrosion);
		super.onStart();
	}
	
	@Override
	protected void run(int count) {
		for (Material iron : Material.values()) {
			if (isIron(iron)) {
				if (participant.getPlayer().getInventory().contains(iron)) {
					NMS.setCooldown(participant.getPlayer(), iron, NMS.getCooldown(participant.getPlayer(), iron) + 22);
				}
			}
		}
		ParticleLib.DRIP_WATER.spawnParticle(participant.getPlayer().getLocation().clone().add(0, 1, 0), 0.5, 1, 0.5, 20, 1);
		super.run(count);
	}

	@Override
	protected void onEnd() {
		participant.getPlayer().getAttribute(Attribute.GENERIC_ARMOR).removeModifier(armorcorrosion);
		HandlerList.unregisterAll(this);
		super.onEnd();
	}

	@Override
	protected void onSilentEnd() {
		participant.getPlayer().getAttribute(Attribute.GENERIC_ARMOR).removeModifier(armorcorrosion);
		HandlerList.unregisterAll(this);
		super.onSilentEnd();
	}
}