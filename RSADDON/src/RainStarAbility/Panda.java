package RainStarAbility;

import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.ArmorStand;
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
import daybreak.abilitywar.utils.base.minecraft.nms.NMS;
import daybreak.abilitywar.utils.base.random.Random;
import daybreak.abilitywar.utils.library.ParticleLib;
import daybreak.abilitywar.utils.library.SoundLib;

@AbilityManifest(name = "판다", rank = Rank.S, species = Species.ANIMAL, explain = {
		"§7패시브 §8- §3아머§f: 기본 공격력이 §c$[DAMAGE_DECREASE]% 감소§f합니다.",
        " 매번 §e감소시킨 피해량§f만큼 §b아머§f를 획득해, §b아머§f 당 공격력이 $[DAMAGE_INCREASE]% 증가합니다.",
        " §b아머§f는 $[MAX_STACK]까지 모을 수 있으며, $[DURATION]초간 갱신하지 않을 경우 전부 잃습니다.",
        "§7철괴 우클릭 §8- §3방어 상태§f: §79.15초§f간 §e대미지 감소 효과§f를 2배로 받습니다.",
        " 방어 상태간 §b아머§f 최대치가 $[MAX_STACK_INCREASED]까지 증가합니다. $[SKILL_COOLDOWN]",
		"§b[§7아이디어 제공자§b] §7Woojaekkun"
		},
		summarize = {
		"기본 공격력이 대폭 감소하나 방어한 피해량에 비례해 공격력이 일시적 증가합니다.",
		"§7철괴 우클릭§f으로 §79.15초§f간 대미지 감소 효과가 2배가 되며,",
		"증가할 수 있는 공격력의 한도치가 증가합니다. $[COOLDOWN]"
		})

public class Panda extends AbilityBase implements ActiveHandler {

	public Panda(Participant participant) {
		super(participant);
	}
	
	public static final SettingObject<Integer> SKILL_COOLDOWN = abilitySettings.new SettingObject<Integer>(
			Panda.class, "skill-cooldown", 150, "# 쿨타임") {

		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}

		@Override
		public String toString() {
			return Formatter.formatCooldown(getValue());
		}

	};
	
	
	public static final SettingObject<Integer> DAMAGE_DECREASE = abilitySettings.new SettingObject<Integer>(
			Panda.class, "damage-decrease", 30, "# 기본 피해량 감소", "# 단위: %") {

		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}

	};
	
	public static final SettingObject<Integer> DAMAGE_INCREASE = abilitySettings.new SettingObject<Integer>(
			Panda.class, "damage-increase", 10, "# 아머당 피해량 증가", "# 단위: %") {

		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}

	};
	
	public static final SettingObject<Integer> MAX_STACK = abilitySettings.new SettingObject<Integer>(
			Panda.class, "max-stack", 5, "# 아머 최대 스택") {

		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}

	};
	
	public static final SettingObject<Integer> MAX_STACK_INCREASED = abilitySettings.new SettingObject<Integer>(
			Panda.class, "max-stack-increased", 10, "# 증가된 아머 최대 스택") {

		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}

	};
	
	private static final SettingObject<Double> DURATION = abilitySettings.new SettingObject<Double>(Panda.class,
			"stack-duration", 3.0, "# 아머 지속시간") {

		@Override
		public boolean condition(Double arg0) {
			return arg0 >= 0.1;
		}

	};
	
	private final ActionbarChannel ac = newActionbarChannel(), ac2 = newActionbarChannel();
	private final Cooldown cooldown = new Cooldown(SKILL_COOLDOWN.getValue());
	private final int duration = (int) (DURATION.getValue() * 20);
	private final double decrease = DAMAGE_DECREASE.getValue() * 0.01;
	private final double increase = DAMAGE_INCREASE.getValue() * 0.01;
	private final int maxstack = MAX_STACK.getValue();
	private final int increasedmaxstack = MAX_STACK_INCREASED.getValue();
	private final DecimalFormat df = new DecimalFormat("0.00");
	private Map<Player, ShieldSpin> shieldMap = new HashMap<>();
	private int stack = 0;
	private double ministack = 0;
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
	    	if (!protecting.isRunning() && !cooldown.isCooldown()) {
				return protecting.start();
			}
	    }
		return false;
	}
	
	private final AbilityTimer stackduration = new AbilityTimer(duration) {
		
		@Override
		public void run(int count) {
			ac2.update("§b아머§f: §3" + stack + "§7/§9" + (protecting.isRunning() ? increasedmaxstack : maxstack));
		}
		
		@Override
		public void onEnd() {
			onSilentEnd();
		}
		
		@Override
		public void onSilentEnd() {
			stack = 0;
			ac2.update("§b아머§f: " + stack);
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
			if (stack > 5) stack = 5;
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
		firstDamage = e.getDamage();
	} 
	
	@SubscribeEvent(priority = 6, onlyRelevant = true)
	public void onEntityDamageByEntityLast(EntityDamageByEntityEvent e) {	
    	Player damager = null;
		if (e.getDamager() instanceof Projectile) {
			Projectile projectile = (Projectile) e.getDamager();
			if (projectile.getShooter() instanceof Player) damager = (Player) projectile.getShooter();
		} else if (e.getDamager() instanceof Player) damager = (Player) e.getDamager();
		
		if (protecting.isRunning()) {
			if (e.getDamage() != 0 && e.getFinalDamage() != 0) {
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
		
		if (!getPlayer().equals(damager)) {
			double decreasePercent = (double) ((firstDamage - e.getFinalDamage()) / firstDamage);
			ministack += decreasePercent;
			if (ministack > 1) {
				ministack -= 1;
				if (protecting.isRunning()) stack = Math.min(increasedmaxstack, stack + 1);
				else stack = Math.min(maxstack, stack + 1);
				if (!stackduration.isRunning()) stackduration.start();
				else stackduration.setCount(duration);
			}	
		}
	}
	
	@SubscribeEvent()
	public void onEntityAddDamage(EntityDamageByEntityEvent e) {
    	Player damager = null;
		if (e.getDamager() instanceof Projectile) {
			Projectile projectile = (Projectile) e.getDamager();
			if (projectile.getShooter() instanceof Player) damager = (Player) projectile.getShooter();
		} else if (e.getDamager() instanceof Player) damager = (Player) e.getDamager();
		
		if (getPlayer().equals(damager)) e.setDamage(e.getDamage() * (1 - decrease + (stack * increase)));
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