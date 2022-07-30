package RainStarAbility;

import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Map;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.entity.EntityDamageByBlockEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.util.Vector;

import daybreak.abilitywar.ability.AbilityBase;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.ability.decorator.ActiveHandler;
import daybreak.abilitywar.config.ability.AbilitySettings.SettingObject;
import daybreak.abilitywar.ability.SubscribeEvent;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.AbstractGame.Participant.ActionbarNotification.ActionbarChannel;
import daybreak.abilitywar.utils.base.Formatter;
import daybreak.abilitywar.utils.base.color.RGB;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.base.math.geometry.Points;
import daybreak.abilitywar.utils.base.minecraft.nms.IHologram;
import daybreak.abilitywar.utils.base.minecraft.nms.NMS;
import daybreak.abilitywar.utils.base.random.Random;
import daybreak.abilitywar.utils.library.ParticleLib;
import daybreak.abilitywar.utils.library.SoundLib;

@AbilityManifest(name = "판다", rank = Rank.S, species = Species.ANIMAL, explain = {
		"웅크린 채 철괴를 우클릭하면 §3방어 상태§f가 됩니다. $[COOLDOWN]",
		"웅크리기를 풀거나 §79.15초§f를 넘으면 §3방어 상태§f는 자동 해제됩니다.",
		"§3방어 상태§f간 모든 대미지 감소 효과(방어력, 저항 등)를 2배로 받습니다.",
		"§3방어 상태§f가 해제될 때, 공격자가 내게 준 최고 대미지의 $[COUNTER_DAMAGE_MULTIPLY]배를 반격합니다.",
		"§b[§7아이디어 제공자§b] §7Woojaekkun"
		},
		summarize = {
		"웅크린 채 철괴를 우클릭하면 §3방어 상태§f가 됩니다. $[COOLDOWN]",
		"웅크리기를 풀거나 §79.15초§f를 넘으면 §3방어 상태§f는 자동 해제됩니다.",
		"§3방어 상태§f간 모든 대미지 감소 효과(방어력, 저항 등)를 2배로 받습니다.",
		"§3방어 상태§f가 해제될 때, 공격자가 내게 준 최고 대미지의 $[COUNTER_DAMAGE_MULTIPLY]배를 반격합니다."
		})

public class Panda extends AbilityBase implements ActiveHandler {

	public Panda(Participant participant) {
		super(participant);
	}
	
	private ActionbarChannel ac = newActionbarChannel();
	
	public static final SettingObject<Integer> COOLDOWN = abilitySettings.new SettingObject<Integer>(
			Panda.class, "cooldown", 85, "# 쿨타임") {

		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}

		@Override
		public String toString() {
			return Formatter.formatCooldown(getValue());
		}

	};
	
	private static final SettingObject<Double> COUNTER_DAMAGE_MULTIPLY = abilitySettings.new SettingObject<Double>(Panda.class,
			"counter-damage-multiply", 1.5, "# 반격 대미지 배율") {

		@Override
		public boolean condition(Double arg0) {
			return arg0 >= 0.1;
		}

	};
	
	@Override
	protected void onUpdate(Update update) {
	    if (update == Update.RESTRICTION_CLEAR) {
	    	isSneaking.start();
	    }	
	}
	
	private final Cooldown cooldown = new Cooldown(COOLDOWN.getValue());
	private final double counter = COUNTER_DAMAGE_MULTIPLY.getValue();
	private final DecimalFormat df = new DecimalFormat("0.00");
	private Map<LivingEntity, Double> damageMap = new HashMap<>();
	private Map<LivingEntity, Stack> stackMap = new HashMap<>();
	private Map<Player, ShieldSpin> shieldMap = new HashMap<>();
	private double firstDamage = 0;
	private static final double pointrange = 0.10;
	private Random random = new Random();
	
	private static final RGB color1 = RGB.of(1, 1, 1), color2 = RGB.of(42, 42, 42), color3 = RGB.of(128, 128, 128),
			color4 = RGB.of(192, 192, 192), color5 = RGB.of(254, 254, 254);
	
	private static final Points LAYER1 = Points.of(pointrange, new boolean[][]{
		{true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true},
		{true, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, true},
		{true, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, true},
		{true, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, true},
		{true, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, true},
		{true, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, true},
		{true, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, true},
		{false, true, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, true, false},
		{false, true, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, true, false},
		{false, true, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, true, false},
		{false, true, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, true, false},
		{false, true, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, true, false},
		{false, false, true, false, false, false, false, false, false, false, false, false, false, false, false, false, true, false, false},
		{false, false, true, false, false, false, false, false, false, false, false, false, false, false, false, false, true, false, false},
		{false, false, true, false, false, false, false, false, false, false, false, false, false, false, false, false, true, false, false},
		{false, false, false, true, false, false, false, false, false, false, false, false, false, false, false, true, false, false, false},
		{false, false, false, true, false, false, false, false, false, false, false, false, false, false, false, true, false, false, false},
		{false, false, false, false, true, false, false, false, false, false, false, false, false, false, true, false, false, false, false},
		{false, false, false, false, false, true, false, false, false, false, false, false, false, true, false, false, false, false, false},
		{false, false, false, false, false, false, true, false, false, false, false, false, true, false, false, false, false, false, false},
		{false, false, false, false, false, false, false, true, false, false, false, true, false, false, false, false, false, false, false},
		{false, false, false, false, false, false, false, false, true, true, true, false, false, false, false, false, false, false, false}
		});
	
	private static final Points LAYER2 = Points.of(pointrange, new boolean[][]{
		{false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false},
		{false, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, false},
		{false, true, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, true, false},
		{false, true, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, true, false},
		{false, true, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, true, false},
		{false, true, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, true, false},
		{false, true, true, false, false, false, false, false, false, false, false, false, false, false, false, false, true, true, false},
		{false, false, true, false, false, false, false, false, false, false, false, false, false, false, false, false, true, false, false},
		{false, false, true, false, false, false, false, false, false, false, false, false, false, false, false, false, true, false, false},
		{false, false, true, false, false, false, false, false, false, false, false, false, false, false, false, false, true, false, false},
		{false, false, true, false, false, false, false, false, false, false, false, false, false, false, false, false, true, false, false},
		{false, false, true, true, false, false, false, false, false, false, false, false, false, false, false, true, true, false, false},
		{false, false, false, true, false, false, false, false, false, false, false, false, false, false, false, true, false, false, false},
		{false, false, false, true, false, false, false, false, false, false, false, false, false, false, false, true, false, false, false},
		{false, false, false, true, true, false, false, false, false, false, false, false, false, false, true, true, false, false, false},
		{false, false, false, false, true, false, false, false, false, false, false, false, false, false, true, false, false, false, false},
		{false, false, false, false, true, true, false, false, false, false, false, false, false, true, true, false, false, false, false},
		{false, false, false, false, false, true, true, false, false, false, false, false, true, true, false, false, false, false, false},
		{false, false, false, false, false, false, true, true, false, false, false, true, true, false, false, false, false, false, false},
		{false, false, false, false, false, false, false, true, true, false, true, true, false, false, false, false, false, false, false},
		{false, false, false, false, false, false, false, false, true, true, true, false, false, false, false, false, false, false, false},
		{false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false}
		});
	
	private static final Points LAYER3 = Points.of(pointrange, new boolean[][]{
		{false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false},
		{false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false},
		{false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, true, true, false, false},
		{false, false, false, true, true, true, false, false, false, false, false, false, false, false, false, true, true, false, false},
		{false, false, false, true, false, false, false, false, false, false, false, false, false, false, false, true, true, false, false},
		{false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, true, true, false, false},
		{false, false, false, false, false, false, false, false, false, false, false, false, false, false, true, true, false, false, false},
		{false, false, false, false, false, false, false, false, false, false, false, false, false, false, true, true, false, false, false},
		{false, false, false, false, false, false, false, false, false, false, false, false, false, true, true, true, false, false, false},
		{false, false, false, false, false, false, false, false, false, false, false, false, false, true, true, true, false, false, false},
		{false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false},
		{false, false, false, false, false, false, false, false, false, false, false, true, true, true, true, false, false, false, false},
		{false, false, false, false, false, false, false, false, false, false, true, true, true, true, true, false, false, false, false},
		{false, false, false, false, false, false, false, true, true, false, true, true, true, true, true, false, false, false, false},
		{false, false, false, false, false, true, true, true, true, false, true, true, true, true, false, false, false, false, false},
		{false, false, false, false, false, true, true, true, true, false, true, true, true, true, false, false, false, false, false},
		{false, false, false, false, false, false, true, true, true, false, true, true, true, false, false, false, false, false, false},
		{false, false, false, false, false, false, false, true, true, false, true, true, false, false, false, false, false, false, false},
		{false, false, false, false, false, false, false, false, true, false, true, false, false, false, false, false, false, false, false},
		{false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false},
		{false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false},
		{false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false}
		});
	
	private static final Points LAYER4 = Points.of(pointrange, new boolean[][]{
		{false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false},
		{false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false},
		{false, false, true, true, true, true, true, true, true, false, true, true, true, true, true, false, false, false, false},
		{false, false, true, false, false, false, true, true, true, false, true, true, true, true, true, false, false, false, false},
		{false, false, true, false, true, true, true, true, true, false, true, true, true, true, true, false, false, false, false},
		{false, false, true, true, true, true, true, true, true, false, true, true, true, true, true, false, false, false, false},
		{false, false, false, true, true, true, true, true, true, false, true, true, true, true, false, false, false, false, false},
		{false, false, false, true, true, true, true, true, true, false, true, true, true, true, false, false, false, false, false},
		{false, false, false, true, true, true, true, true, true, false, true, true, true, false, false, false, false, false, false},
		{false, false, false, true, true, true, true, true, true, false, true, true, true, false, false, false, false, false, false},
		{false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false},
		{false, false, false, false, true, true, true, true, true, false, true, false, false, false, false, false, false, false, false},
		{false, false, false, false, true, true, true, true, true, false, false, false, false, false, false, false, false, false, false},
		{false, false, false, false, true, true, true, false, false, false, false, false, false, false, false, false, false, false, false},
		{false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false},
		{false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false},
		{false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false},
		{false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false},
		{false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false},
		{false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false},
		{false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false},
		{false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false}
		});
	
	private static final Points LAYER5 = Points.of(pointrange, new boolean[][]{
		{false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false},
		{false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false},
		{false, false, false, false, false, false, false, false, false, true, false, false, false, false, false, false, false, false, false},
		{false, false, false, false, false, false, false, false, false, true, false, false, false, false, false, false, false, false, false},
		{false, false, false, false, false, false, false, false, false, true, false, false, false, false, false, false, false, false, false},
		{false, false, false, false, false, false, false, false, false, true, false, false, false, false, false, false, false, false, false},
		{false, false, false, false, false, false, false, false, false, true, false, false, false, false, false, false, false, false, false},
		{false, false, false, false, false, false, false, false, false, true, false, false, false, false, false, false, false, false, false},
		{false, false, false, false, false, false, false, false, false, true, false, false, false, false, false, false, false, false, false},
		{false, false, false, false, false, false, false, false, false, true, false, false, false, false, false, false, false, false, false},
		{false, false, false, true, true, true, true, true, true, true, true, true, true, true, true, true, false, false, false},
		{false, false, false, false, false, false, false, false, false, true, false, false, false, false, false, false, false, false, false},
		{false, false, false, false, false, false, false, false, false, true, false, false, false, false, false, false, false, false, false},
		{false, false, false, false, false, false, false, false, false, true, false, false, false, false, false, false, false, false, false},
		{false, false, false, false, false, false, false, false, false, true, false, false, false, false, false, false, false, false, false},
		{false, false, false, false, false, false, false, false, false, true, false, false, false, false, false, false, false, false, false},
		{false, false, false, false, false, false, false, false, false, true, false, false, false, false, false, false, false, false, false},
		{false, false, false, false, false, false, false, false, false, true, false, false, false, false, false, false, false, false, false},
		{false, false, false, false, false, false, false, false, false, true, false, false, false, false, false, false, false, false, false},
		{false, false, false, false, false, false, false, false, false, true, false, false, false, false, false, false, false, false, false},
		{false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false},
		{false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false}
		});
	
	public boolean ActiveSkill(Material material, ClickType clicktype) {
	    if (material.equals(Material.IRON_INGOT) && clicktype.equals(ClickType.RIGHT_CLICK)) {
	    	if (getPlayer().isSneaking() && !protecting.isRunning() && !cooldown.isCooldown()) {
				return protecting.start();
			}
	    }
		return false;
	}
	
	private final AbilityTimer isSneaking = new AbilityTimer() {
		
		@Override
		public void run(int count) {
			if (protecting.isRunning()) {
				if (!getPlayer().isSneaking()) {
					protecting.stop(false);
				}
			}
		}
		
	}.setPeriod(TimeUnit.TICKS, 1).register();
	
	private final AbilityTimer protecting = new AbilityTimer(183) {
		
		@Override
		public void onStart() {
			SoundLib.ITEM_SHIELD_BLOCK.playSound(getPlayer().getLocation(), 1, 0.65f);
			if (getPlayer().getName().equals("Woojaekkun")) particle.start();
			else if (random.nextInt(10) < 2) particle.start();
			new ShieldSpin(getPlayer()).start();
		}
		
		@Override
		public void run(int count) {
			ac.update("§8방어 모드§7: " + df.format((double) count / 20));
		}
		
		@Override
		public void onEnd() {
			onSilentEnd();
		}
		
		@Override
		public void onSilentEnd() {
			if (particle.isRunning()) particle.stop(false);
			if (shieldMap.containsKey(getPlayer())) shieldMap.get(getPlayer()).stop(false);
			ac.update(null);
			cooldown.start();
			for (LivingEntity l : damageMap.keySet()) {
				l.damage(damageMap.get(l) * counter, getPlayer());
				stackMap.get(l).stop(false);
			}
			damageMap.clear();
		}
		
	}.setPeriod(TimeUnit.TICKS, 1).register();
	
	private final AbilityTimer particle = new AbilityTimer(5) {
		
		private Location eyeLocation;
		private Vector direction;
		private float yaw;
		
		@Override
		public void onStart() {
			eyeLocation = getPlayer().getLocation().clone();
			yaw = getPlayer().getLocation().getYaw();
			direction = getPlayer().getLocation().getDirection();
			SoundLib.ENTITY_ILLUSIONER_PREPARE_MIRROR.playSound(getPlayer().getLocation(), 1, 1.65f);
		}
		
    	@Override
		public void run(int count) {
    		eyeLocation.add(direction.setY(0).normalize().multiply(0.75));
			for (Location loc : LAYER1.rotateAroundAxisY(-yaw).toLocations(eyeLocation)) {
				ParticleLib.REDSTONE.spawnParticle(loc, color1);
			}
			for (Location loc : LAYER2.rotateAroundAxisY(-yaw).toLocations(eyeLocation)) {
				ParticleLib.REDSTONE.spawnParticle(loc, color2);
			}
			for (Location loc : LAYER3.rotateAroundAxisY(-yaw).toLocations(eyeLocation)) {
				ParticleLib.REDSTONE.spawnParticle(loc, color3);
			}
			for (Location loc : LAYER4.rotateAroundAxisY(-yaw).toLocations(eyeLocation)) {
				ParticleLib.REDSTONE.spawnParticle(loc, color4);
			}
			for (Location loc : LAYER5.rotateAroundAxisY(-yaw).toLocations(eyeLocation)) {
				ParticleLib.REDSTONE.spawnParticle(loc, color5);
			}
			LAYER1.rotateAroundAxisY(yaw);
			LAYER2.rotateAroundAxisY(yaw);
			LAYER3.rotateAroundAxisY(yaw);
			LAYER4.rotateAroundAxisY(yaw);
			LAYER5.rotateAroundAxisY(yaw);
    	}
    	
    	@Override
    	public void onEnd() {
    		for (Location loc : LAYER1.rotateAroundAxisY(-yaw).toLocations(eyeLocation)) {
				ParticleLib.END_ROD.spawnParticle(loc, 0, 0, 0, 1, 1);
			}
			for (Location loc : LAYER2.rotateAroundAxisY(-yaw).toLocations(eyeLocation)) {
				ParticleLib.END_ROD.spawnParticle(loc, 0, 0, 0, 1, 1);
			}
			for (Location loc : LAYER3.rotateAroundAxisY(-yaw).toLocations(eyeLocation)) {
				ParticleLib.END_ROD.spawnParticle(loc, 0, 0, 0, 1, 1);
			}
			for (Location loc : LAYER4.rotateAroundAxisY(-yaw).toLocations(eyeLocation)) {
				ParticleLib.END_ROD.spawnParticle(loc, 0, 0, 0, 1, 1);
			}
			for (Location loc : LAYER5.rotateAroundAxisY(-yaw).toLocations(eyeLocation)) {
				ParticleLib.END_ROD.spawnParticle(loc, 0, 0, 0, 1, 1);
			}
			LAYER1.rotateAroundAxisY(yaw);
			LAYER2.rotateAroundAxisY(yaw);
			LAYER3.rotateAroundAxisY(yaw);
			LAYER4.rotateAroundAxisY(yaw);
			LAYER5.rotateAroundAxisY(yaw);
			SoundLib.ENTITY_FIREWORK_ROCKET_LARGE_BLAST.playSound(eyeLocation);
    	}
		
	}.setBehavior(RestrictionBehavior.PAUSE_RESUME).setPeriod(TimeUnit.TICKS, 2).register();
	
	@SubscribeEvent(priority = -999, onlyRelevant = true)
	public void onEntityDamageFirst(EntityDamageEvent e) {
		if (protecting.isRunning()) firstDamage = e.getDamage();
	}
	
	@SubscribeEvent(priority = 6, onlyRelevant = true)
	public void onEntityDamageLast(EntityDamageEvent e) {
		if (protecting.isRunning()) {
			if (e.getDamage() > firstDamage) {
				e.setDamage(e.getFinalDamage());
			} else {
				if ((firstDamage - e.getFinalDamage()) == 0 && firstDamage == 0) e.setDamage(e.getFinalDamage());
				else {
					double decreasePercent = (double) ((firstDamage - e.getFinalDamage()) / firstDamage);
					if (decreasePercent != 0) {
						double fixDamage = (e.getDamage() * (1 - decreasePercent));
						e.setDamage(fixDamage);
					} else e.setDamage(e.getFinalDamage());	
				}
			}
		}
	}
	
	@SubscribeEvent(priority = -999, onlyRelevant = true)
	public void onEntityDamageByBlockFirst(EntityDamageByBlockEvent e) {
		if (protecting.isRunning()) firstDamage = e.getDamage();
	}
	
	@SubscribeEvent(priority = 6, onlyRelevant = true)
	public void onEntityDamageByBlockLast(EntityDamageByBlockEvent e) {
		if (protecting.isRunning()) {
			if (e.getDamage() > firstDamage) {
				e.setDamage(e.getFinalDamage());
			} else {
				if ((firstDamage - e.getFinalDamage()) == 0 && firstDamage == 0) e.setDamage(e.getFinalDamage());
				else {
					double decreasePercent = (double) ((firstDamage - e.getFinalDamage()) / firstDamage);
					if (decreasePercent != 0) {
						double fixDamage = (e.getDamage() * (1 - decreasePercent));
						e.setDamage(fixDamage);
					} else e.setDamage(e.getFinalDamage());	
				}
			}
		}
	}
	
	@SubscribeEvent(priority = -999, onlyRelevant = true)
	public void onEntityDamageByEntityFirst(EntityDamageByEntityEvent e) {
		if (protecting.isRunning()) {
			firstDamage = e.getDamage();
		}
	} 
	
	@SubscribeEvent(priority = 6, onlyRelevant = true)
	public void onEntityDamageByEntityLast(EntityDamageByEntityEvent e) {
		if (protecting.isRunning()) {
			if (e.getDamage() != 0 && e.getFinalDamage() != 0) {
				if (e.getDamage() > firstDamage) {
					if (e.getDamager() instanceof Projectile) {
						Projectile p = (Projectile) e.getDamager();
						if (!getPlayer().equals(p.getShooter())) {
							if (p.getShooter() != null) {
								if (damageMap.containsKey(p.getShooter())) {
									if (e.getDamage() > damageMap.get(p.getShooter())) damageMap.put((LivingEntity) p.getShooter(), e.getDamage());
								} else damageMap.put((LivingEntity) p.getShooter(), e.getDamage());
								if (!stackMap.containsKey(p.getShooter())) new Stack((LivingEntity) p.getShooter()).start();
							}
						}
					} else if (e.getDamager() instanceof LivingEntity) {
						if (damageMap.containsKey(e.getDamager())) {
							if (e.getDamage() > damageMap.get(e.getDamager())) damageMap.put((LivingEntity) e.getDamager(), e.getDamage());
						} else damageMap.put((LivingEntity) e.getDamager(), e.getDamage());
						if (!stackMap.containsKey(e.getDamager())) new Stack((LivingEntity) e.getDamager()).start();
					}
					e.setDamage(e.getFinalDamage());
				} else {
					if (e.getDamager() instanceof Projectile) {
						Projectile p = (Projectile) e.getDamager();
						if (!getPlayer().equals(p.getShooter())) {
							if (p.getShooter() != null) {
								if (damageMap.containsKey(p.getShooter())) {
									if (firstDamage > damageMap.get(p.getShooter())) damageMap.put((LivingEntity) p.getShooter(), firstDamage);
								} else damageMap.put((LivingEntity) p.getShooter(), firstDamage);
								if (!stackMap.containsKey(p.getShooter())) new Stack((LivingEntity) p.getShooter()).start();
							}
						}
					} else if (e.getDamager() instanceof LivingEntity) {
						if (damageMap.containsKey(e.getDamager())) {
							if (firstDamage > damageMap.get(e.getDamager())) damageMap.put((LivingEntity) e.getDamager(), firstDamage);
						} else damageMap.put((LivingEntity) e.getDamager(), firstDamage);
						if (!stackMap.containsKey(e.getDamager())) new Stack((LivingEntity) e.getDamager()).start();
					}
					if ((firstDamage - e.getFinalDamage()) == 0 && firstDamage == 0) e.setDamage(e.getFinalDamage());
					else {
						double decreasePercent = (double) ((firstDamage - e.getFinalDamage()) / firstDamage);
						if (decreasePercent != 0) {
							double fixDamage = (e.getDamage() * (1 - decreasePercent));
							e.setDamage(fixDamage);
						} else e.setDamage(e.getFinalDamage());	
					}
				}	
			}
		}
	}
	
	private class Stack extends AbilityTimer {
		
		private final LivingEntity livingEntity;
		private final IHologram hologram;
		private final DecimalFormat df = new DecimalFormat("0.00");
		
		private Stack(LivingEntity livingEntity) {
			setPeriod(TimeUnit.TICKS, 1);
			this.livingEntity = livingEntity;
			this.hologram = NMS.newHologram(livingEntity.getWorld(), livingEntity.getLocation().getX(),
					livingEntity.getLocation().getY() + livingEntity.getEyeHeight() + 0.6, livingEntity.getLocation().getZ());
			hologram.setText("§c§l0");
			hologram.display(getPlayer());
			stackMap.put(livingEntity, this);
		}

		@Override
		protected void run(int count) {
			hologram.teleport(livingEntity.getWorld(), livingEntity.getLocation().getX(), 
					livingEntity.getLocation().getY() + livingEntity.getEyeHeight() + 0.6, livingEntity.getLocation().getZ(), 
					livingEntity.getLocation().getYaw(), 0);
			if (damageMap.containsKey(livingEntity)) {
				hologram.setText("§c§l" + df.format(damageMap.get(livingEntity) * counter));
			}
		}
		
		@Override
		protected void onEnd() {
			onSilentEnd();
		}
		
		@Override
		protected void onSilentEnd() {
			hologram.unregister();
			stackMap.remove(livingEntity);
		}
		
	}
	
	private class ShieldSpin extends AbilityTimer {
		
		private final Player player;
		private final ArmorStand[] shields = new ArmorStand[3];
		
		private ShieldSpin(Player player) {
			super(183);
			setPeriod(TimeUnit.TICKS, 1);
			this.player = player;
			for (int a = 0; a < 3; a++) {
				shields[a] = player.getWorld().spawn(player.getLocation().clone(), ArmorStand.class);
				shields[a].setVisible(false);
				shields[a].setInvulnerable(true);
				shields[a].setGravity(false);
				NMS.removeBoundingBox(shields[a]);
				
				ItemStack shield = new ItemStack(Material.SHIELD);
				ItemMeta shieldmeta = shield.getItemMeta();
				shieldmeta.addEnchant(Enchantment.PROTECTION_ENVIRONMENTAL, 10, true);
				shield.setItemMeta(shieldmeta);
				
				EntityEquipment equipment = shields[a].getEquipment();
				
				equipment.setHelmet(shield);
			}
			shieldMap.put(player, this);
		}
		
		@Override
		public void run(int count) {
			for (int iteration = 0; iteration < 3; iteration++) {
                double angle = Math.toRadians(120.0D * iteration + (count * 1));
                double x = Math.cos(angle);
                double z = Math.sin(angle);
                Vector direction = player.getEyeLocation().toVector().subtract(shields[iteration].getEyeLocation().toVector());
                shields[iteration].teleport(player.getLocation().clone().add(x, -0.5, z).setDirection(direction));    
            }
		}
		
		@Override
		public void onEnd() {
			for (ArmorStand stand : shields) {
				stand.remove();
			}
			SoundLib.ITEM_SHIELD_BREAK.playSound(player.getLocation(), 1, 0.5f);
			shieldMap.remove(player);
		}
		
		@Override
		public void onSilentEnd() {
			onEnd();
		}
		
	}
	
}