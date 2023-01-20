package rainstar.abilitywar.ability;

import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.util.Vector;

import daybreak.abilitywar.AbilityWar;
import daybreak.abilitywar.ability.AbilityBase;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.SubscribeEvent;
import daybreak.abilitywar.ability.event.AbilityPreActiveSkillEvent;
import daybreak.abilitywar.ability.event.AbilityPreTargetEvent;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.ability.decorator.ActiveHandler;
import daybreak.abilitywar.config.ability.AbilitySettings.SettingObject;
import daybreak.abilitywar.config.enums.CooldownDecrease;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.AbstractGame.Participant.ActionbarNotification.ActionbarChannel;
import daybreak.abilitywar.game.manager.effect.Oppress;
import daybreak.abilitywar.utils.base.Formatter;
import daybreak.abilitywar.utils.base.color.Gradient;
import daybreak.abilitywar.utils.base.color.RGB;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.base.concurrent.SimpleTimer.TaskType;
import daybreak.abilitywar.utils.base.math.LocationUtil;
import daybreak.abilitywar.utils.base.math.geometry.Circle;
import daybreak.abilitywar.utils.base.math.geometry.Points;
import daybreak.abilitywar.utils.base.random.Random;
import daybreak.abilitywar.utils.library.ParticleLib;
import daybreak.abilitywar.utils.library.SoundLib;

@AbilityManifest(
		name = "모로스", rank = Rank.S, species = Species.GOD, explain = {
		"피할 수 없는 운명의 신, 모로스.",
		"§7게임 시작 §8- §9운명론§f: 자신의 모든 수치화된 효과는 §a오차범위§f가 존재합니다.",
		" §a오차범위§f의 최종 오차값은 게임 시작 시 정해지며, 바뀔 수 없습니다.",
		"§7적 타격 §8- §c필멸§f: $[MORTAL_DURATION]$[MORTAL_DURATION_SPREAD]초 이내에 공격했던 대상이 사망 위기에 처했을 때,",
		" 대상은 §c§n그 어떠한 방법으로도§f 죽음을 피할 수 없습니다.",
		"§7패시브 §8- §3운명 개찬§f: $[RANGE]$[RANGE_SPREAD]칸 내의 §a액티브§8 / §6타게팅§f 스킬을 미리 감지하고 직전에",
		" $[DURATION]$[DURATION_SPREAD]초간 §b타게팅 불가 상태§f가 됩니다. $[PASSIVE_COOLDOWN]$[PASSIVE_COOLDOWN_SPREAD]",
		"§7철괴 좌클릭 §8- §b변수 제거§f: §c필멸§f을 전부 §c§n제압§f으로 바꿉니다. $[ACTIVE_COOLDOWN]$[ACTIVE_COOLDOWN_SPREAD]",
		" §c§n제압§f된 대상에게는 §c필멸§f 부여 대신 $[DAMAGE_INCREASE]$[DAMAGE_INCREASE_SPREAD]%의 추가 피해를 입힙니다."
		},
		summarize = {
		"게임 시작 시 모든 스킬 효과의 수치들은 오차범위 내에서 재설정됩니다.",
		"공격한 적은 §c필멸§f 효과를 받아 치명적인 피해를 입으면 무조건 사망합니다.",
		"주변에서 능력 사용을 감지하고 잠시간 §3타게팅 불가 상태§f가 됩니다.",
		"§7철괴 좌클릭§f으로, 모든 §c필멸§f 효과를 §c§n제압§f으로 바꾸고 §c§n제압§f 대상 한정 공격력이 증가합니다."
		})

public class Moros extends AbilityBase implements ActiveHandler {

	public Moros(Participant participant) {
		super(participant);
	}
	
	private static final Points LAYER1 = Points.of(0.06, new boolean[][]{
		{false, false, false, false, false, false, false, true, true, true, true, true, true, false, false, false, false, false, false, false},
		{false, false, false, false, false, true, true, false, false, false, false, false, false, true, true, false, false, false, false, false},
		{false, false, false, false, true, false, false, false, false, false, false, false, false, false, false, true, false, false, false, false},
		{false, false, false, true, false, false, false, false, false, false, false, false, false, false, false, false, true, false, false, false},
		{false, false, true, false, false, false, false, false, false, false, false, false, false, false, false, false, false, true, false, false},
		{false, true, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, true, false},
		{false, true, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, true, false},
		{true, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, true},
		{true, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, true},
		{true, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, true},
		{true, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, true},
		{true, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, true},
		{false, true, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, true, false},
		{false, true, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, true, false},
		{false, false, true, false, false, false, false, false, false, false, false, false, false, false, false, false, false, true, false, false},
		{false, false, false, true, false, false, false, false, false, false, false, false, false, false, false, false, true, false, false, false},
		{false, false, false, false, true, false, false, false, false, false, false, false, false, false, false, true, false, false, false, false},
		{false, false, false, false, false, true, true, false, false, false, false, false, false, true, true, false, false, false, false, false},
		{false, false, false, false, false, false, false, true, true, true, true, true, true, false, false, false, false, false, false, false},
		{false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false},
		{false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false},
		{false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false},
		{false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false},
		{false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false}
	});
	
	private static final Points LAYER2 = Points.of(0.06, new boolean[][]{
		{false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false},
		{false, false, false, false, false, false, false, true, true, true, true, true, true, false, false, false, false, false, false, false},
		{false, false, false, false, false, true, true, true, true, true, true, true, true, true, true, false, false, false, false, false},
		{false, false, false, false, true, true, true, true, true, true, true, true, true, true, true, true, false, false, false, false},
		{false, false, false, true, true, false, false, false, true, true, true, true, true, true, true, true, true, false, false, false},
		{false, false, true, true, false, false, false, false, false, true, true, true, true, true, true, true, true, true, false, false},
		{false, false, true, true, false, false, false, false, false, true, true, true, true, true, true, true, true, true, false, false},
		{false, true, true, true, false, false, false, false, false, true, true, true, true, true, true, true, true, true, true, false},
		{false, true, true, true, true, false, false, false, true, true, true, true, true, true, true, true, true, true, true, false},
		{false, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, false},
		{false, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, false},
		{false, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, false},
		{false, false, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, false, false},
		{false, false, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, false, false},
		{false, false, false, true, true, true, true, true, true, true, true, true, true, true, true, true, true, false, false, false},
		{false, false, false, false, true, true, true, true, true, true, true, true, true, true, true, true, false, false, false, false},
		{false, false, false, false, false, true, true, true, true, true, true, true, true, true, true, false, false, false, false, false},
		{false, false, false, false, false, false, false, true, true, true, true, true, true, false, false, false, false, false, false, false},
		{false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false},
		{false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false},
		{false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false},
		{false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false},
		{false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false},
		{false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false},
	});
	
	private static final Points LAYER3 = Points.of(0.06, new boolean[][]{
		{false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false},
		{false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false},
		{false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false},
		{false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false},
		{false, false, false, false, false, true, true, true, false, false, false, false, false, false, false, false, false, false, false, false},
		{false, false, false, false, true, true, true, true, true, false, false, false, false, false, false, false, false, false, false, false},
		{false, false, false, false, true, true, true, true, true, false, false, false, false, false, false, false, false, false, false, false},
		{false, false, false, false, true, true, true, true, true, false, false, false, false, false, false, false, false, false, false, false},
		{false, false, false, false, false, true, true, true, false, false, false, false, false, false, false, false, false, false, false, false},
		{false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false},
		{false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false},
		{false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false},
		{false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false},
		{false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false},
		{false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false},
		{false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false},
		{false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false},
		{false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false},
		{false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false},
		{false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false},
		{false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false},
		{false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false},
		{false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false},
		{false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false}
	});
	
	private static final Points LAYER4 = Points.of(0.06, new boolean[][]{
		{false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false},
		{false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false},
		{false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false},
		{false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false},
		{false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false},
		{false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false},
		{false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false},
		{false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false},
		{false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false},
		{false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false},
		{false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false},
		{false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false},
		{false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false},
		{false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false},
		{false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false},
		{false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false},
		{false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false},
		{false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false},
		{false, false, false, false, false, true, true, false, false, false, false, false, false, true, true, false, false, false, false, false},
		{false, false, false, false, true, true, true, true, true, true, true, true, true, true, true, true, false, false, false, false},
		{false, false, false, false, true, true, true, true, true, true, true, true, true, true, true, true, false, false, false, false},
		{false, false, false, true, true, true, true, true, true, true, true, true, true, true, true, true, true, false, false, false},
		{false, false, false, false, false, true, true, true, true, true, true, true, true, true, true, false, false, false, false, false},
		{false, false, false, false, false, false, true, true, true, true, true, true, true, true, false, false, false, false, false, false},
	});
	
	public static final SettingObject<Integer> MORTAL_DURATION = abilitySettings.new SettingObject<Integer>(Moros.class,
			"mortal-duration", 5, "# 필멸 지속시간") {
		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}
	};
	
	public static final SettingObject<Integer> MORTAL_DURATION_SPREAD = abilitySettings.new SettingObject<Integer>(Moros.class,
			"mortal-duration-spread", 3, "# 필멸 지속시간 오차범위") {
		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}
		
		@Override
		public String toString() {
			return "§7±" + getValue() + "§f";
        }
	};
	
	public static final SettingObject<Integer> RANGE = abilitySettings.new SettingObject<Integer>(Moros.class,
			"range", 10, "# 운명 개찬 범위", "# 단위: 칸") {
		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}
	};
	
	public static final SettingObject<Integer> RANGE_SPREAD = abilitySettings.new SettingObject<Integer>(Moros.class,
			"range-spread", 5, "# 운명 개찬 범위 오차범위") {
		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}
		
		@Override
		public String toString() {
			return "§7±" + getValue() + "§f";
        }
	};
	
	public static final SettingObject<Integer> DURATION = abilitySettings.new SettingObject<Integer>(Moros.class,
			"targetable-false-duration", 10, "# 타게팅 불가 지속시간") {
		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}
	};
	
	public static final SettingObject<Integer> DURATION_SPREAD = abilitySettings.new SettingObject<Integer>(Moros.class,
			"targetable-false-duration-spread", 7, "# 타게팅 불가 지속시간 오차범위") {
		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}
		
		@Override
		public String toString() {
			return "§7±" + getValue() + "§f";
        }
	};
	
	public static final SettingObject<Integer> PASSIVE_COOLDOWN = abilitySettings.new SettingObject<Integer>(Moros.class,
			"passive-cooldown", 60, "# 운명 개찬 쿨타임", "# WRECK 감소 최대 50% 적용") {
		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}

		@Override
		public String toString() {
			return Formatter.formatCooldown(getValue());
		}
	};
	
	public static final SettingObject<Integer> PASSIVE_COOLDOWN_SPREAD = abilitySettings.new SettingObject<Integer>(Moros.class,
			"passive-cooldown-spread", 20, "# 운명 개찬 쿨타임의 오차범위") {
		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}

		@Override
		public String toString() {
			return "§7±" + getValue() + "§f";
        }
	};
	
	public static final SettingObject<Integer> ACTIVE_COOLDOWN = abilitySettings.new SettingObject<Integer>(Moros.class,
			"active-cooldown", 80, "# 변수 제거 쿨타임") {
		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}

		@Override
		public String toString() {
			return Formatter.formatCooldown(getValue());
		}
	};
	
	public static final SettingObject<Integer> ACTIVE_COOLDOWN_SPREAD = abilitySettings.new SettingObject<Integer>(Moros.class,
			"active-cooldown-spread", 40, "# 변수 제거 쿨타임의 오차범위") {
		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}

		@Override
		public String toString() {
			return "§7±" + getValue() + "§f";
        }
	};
	
	public static final SettingObject<Integer> DAMAGE_INCREASE = abilitySettings.new SettingObject<Integer>(Moros.class,
			"damage-increase", 25, "# 공격력 배율", "# 단위: %") {
		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}
	};
	
	public static final SettingObject<Integer> DAMAGE_INCREASE_SPREAD = abilitySettings.new SettingObject<Integer>(Moros.class,
			"damage-increase-spread", 15, "# 공격력 배율 오차범위") {
		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}
		
		@Override
		public String toString() {
			return "§7±" + getValue() + "§f";
        }
	};
	
	private int mortalDuration = MORTAL_DURATION.getValue();
	private final int mortalDurationSpread = MORTAL_DURATION_SPREAD.getValue();
	private double range = RANGE.getValue();
	private final int rangeSpread = RANGE_SPREAD.getValue();
	private int duration = DURATION.getValue();
	private final int durationSpread = DURATION_SPREAD.getValue();
	private Cooldown passivecool = new Cooldown(PASSIVE_COOLDOWN.getValue());
	private final int passiveCooldownSpread = PASSIVE_COOLDOWN_SPREAD.getValue();
	private Cooldown activecool = new Cooldown(ACTIVE_COOLDOWN.getValue());
	private final int activeCooldownSpread = ACTIVE_COOLDOWN_SPREAD.getValue();
	private int damageIncrease = DAMAGE_INCREASE.getValue();
	private final int damageIncreaseSpread = DAMAGE_INCREASE_SPREAD.getValue();
	private Map<Player, Mortal> mortals = new HashMap<>();
	private boolean first = false;
	private final Random random = new Random();
	private final DecimalFormat df = new DecimalFormat("0.0");
	private ActionbarChannel ac = newActionbarChannel();
	private static final RGB color1 = RGB.of(1, 204, 254), color2 = RGB.of(101, 224, 254), color3 = RGB.of(195, 243, 254), color4 = RGB.of(153, 102, 1),
			color5 = RGB.of(182, 10, 5), startColor = RGB.of(49, 254, 254), endColor = RGB.of(234, 111, 254);
	private final List<RGB> gradations = Gradient.createGradient(30, startColor, endColor);
	private final Circle circle = Circle.of(0.75, 25);
	
	@Override
	public void onUpdate(Update update) {
		if (update == Update.RESTRICTION_CLEAR && !first) {
			first = true;
			mortalDuration = (int) ((mortalDuration + ((random.nextInt(mortalDurationSpread * 100 + 1) * (random.nextBoolean() ? -0.01 : 0.01)))) * 20);
			range = (range + ((random.nextInt(rangeSpread * 100 + 1) * (random.nextBoolean() ? -0.01 : 0.01))));
			duration = (int) ((duration + ((random.nextInt(durationSpread * 100 + 1) * (random.nextBoolean() ? -0.01 : 0.01)))) * 20);
			passivecool.setCooldown((int) (PASSIVE_COOLDOWN.getValue() + ((random.nextInt(passiveCooldownSpread * 100 + 1) * (random.nextBoolean() ? -0.01 : 0.01)))), CooldownDecrease._50);
			activecool.setCooldown((int) (ACTIVE_COOLDOWN.getValue() + ((random.nextInt(activeCooldownSpread * 100 + 1) * (random.nextBoolean() ? -0.01 : 0.01)))));
			damageIncrease = (int) ((damageIncrease + ((random.nextInt(damageIncreaseSpread * 100 + 1) * (random.nextBoolean() ? -0.01 : 0.01)))));
			getPlayer().sendMessage("§3[§b운명론§3] §c필멸 §e" + df.format(mortalDuration / 20.0) + "§f초 §7/ §a범위 §e" + range + "§f칸 §7/ §3타게팅 불가 §e" + df.format(duration / 20.0) 
			+ "§f초 §7/ §c운명 개찬 쿨타임 §e" + passivecool.getCooldown() + "§f초 §7/ §c변수 제거 쿨타임 §e" + activecool.getCooldown() + "§f초 §7/ §c공격력 증가 §e" + damageIncrease + "§f%로 결정되었습니다.");
		}
	}
	
	private final AbilityTimer particle = new AbilityTimer(5) {
		
    	@Override
		public void run(int count) {
			final Location eyeLocation = getPlayer().getLocation().clone().add(getPlayer().getLocation().getDirection().add(new Vector(0, 0.5, 0)));
			final float yaw = getPlayer().getLocation().getYaw();
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
			LAYER1.rotateAroundAxisY(yaw);
			LAYER2.rotateAroundAxisY(yaw);
			LAYER3.rotateAroundAxisY(yaw);
			LAYER4.rotateAroundAxisY(yaw);
    	}
		
	}.setPeriod(TimeUnit.TICKS, 4).register();
	
	public AbilityTimer nontargetable = new AbilityTimer(TaskType.REVERSE, 10) {
		
		private double y;
		private boolean add;
		private int stack;
		
		@Override
		public void onStart() {
			nontargetable.setCount(duration);
			SoundLib.BLOCK_END_PORTAL_SPAWN.playSound(getPlayer().getLocation(), (float) 0.7, 2);
			particle.start();
			y = 0.0;
			add = true;
			stack = 0;
		}
		
		@Override
		public void run(int count) {
			getParticipant().attributes().TARGETABLE.setValue(false);
			ac.update("§3운명 개찬§f: " + df.format(count / 20.0));
			
			if (add && y >= 2.0) add = false;
			else if (!add && y <= 0) add = true;

			y = add ? y + 0.1 : y - 0.1;
			stack = stack < 30 ? stack + 1 : 0;
			
			for (Location location : circle.toLocations(getPlayer().getLocation().add(0, y, 0))) {
				ParticleLib.REDSTONE.spawnParticle(location, gradations.get(Math.max(0, stack - 1)));
			}
		}
		
		@Override
		public void onEnd() {
			onSilentEnd();
		}
		
		@Override
		public void onSilentEnd() {
			ac.update(null);
			getParticipant().attributes().TARGETABLE.setValue(true);
			passivecool.start();
		}
		
	}.setPeriod(TimeUnit.TICKS, 1).register();
	
	@SubscribeEvent
	public void onEntityDamageByEntity(EntityDamageByEntityEvent e) {
		if (e.getEntity() instanceof Player) {
	    	Player damager = null;
			if (e.getDamager() instanceof Projectile) {
				Projectile projectile = (Projectile) e.getDamager();
				if (projectile.getShooter() instanceof Player) damager = (Player) projectile.getShooter();
			} else if (e.getDamager() instanceof Player) damager = (Player) e.getDamager();
			
			Player player = (Player) e.getEntity();
			
			if (getPlayer().equals(damager) && !e.isCancelled() && !player.equals(getPlayer())) {
				if (getGame().getParticipant(player).hasEffect(Oppress.registration)) {
					e.setDamage(e.getDamage() * (1 + (damageIncrease * 0.01)));
				} else {
					if (!mortals.containsKey(player)) {
						mortals.put(player, new Mortal(player));
						mortals.get(player).start();
					} else mortals.get(player).addDamage();	
				}
			}
		}
	}
	
	@SubscribeEvent
	public void onPreActiveSkillEvent(AbilityPreActiveSkillEvent e) {
		if (!e.getPlayer().equals(getPlayer()) && !nontargetable.isRunning() && !passivecool.isRunning() && LocationUtil.isInCircle(getPlayer().getLocation(), e.getPlayer().getLocation(), range)) {
			getParticipant().attributes().TARGETABLE.setValue(false);
			nontargetable.start();
		}
	}
	
	@SubscribeEvent
	public void onPreTargetEvent(AbilityPreTargetEvent e) {
		if (!e.getPlayer().equals(getPlayer()) && !nontargetable.isRunning() && !passivecool.isRunning() && LocationUtil.isInCircle(getPlayer().getLocation(), e.getPlayer().getLocation(), range)) {
			getParticipant().attributes().TARGETABLE.setValue(false);
			nontargetable.start();
		}
	}
	
	public boolean ActiveSkill(Material material, ClickType clicktype) {
	    if (material.equals(Material.IRON_INGOT) && clicktype.equals(ClickType.LEFT_CLICK) && !activecool.isCooldown() && mortals.size() > 0) {
	    	for (Player player : mortals.keySet()) {
	    		Oppress.apply(getGame().getParticipant(player), TimeUnit.TICKS, mortals.get(player).getCount());
				for (Location location : circle.toLocations(player.getLocation().add(0, 1, 0))) {
					ParticleLib.REDSTONE.spawnParticle(location, color5);
				}
				SoundLib.ENTITY_ELDER_GUARDIAN_CURSE.playSound(player.getLocation(), 1, 0.7f);
	    		mortals.get(player).stop(false);
	    	}
	    	return activecool.start();
	    }
		return false;
	}
	
    public class Mortal extends AbilityTimer implements Listener {
    	
    	private Player player;
		private ActionbarChannel actionbarChannel = newActionbarChannel();
    	
    	private Mortal(Player player) {
			super(TaskType.REVERSE, mortalDuration);
			setPeriod(TimeUnit.TICKS, 1);
			this.player = player;
    	}
    	
		@Override
		protected void onStart() {
			Bukkit.getPluginManager().registerEvents(this, AbilityWar.getPlugin());
			actionbarChannel = getGame().getParticipant(player).actionbar().newChannel();
		}
		
		@Override
		protected void run(int count) {
			actionbarChannel.update("§4필멸§f: " + df.format(count / 20) + "초");
		}
		
		private void addDamage() {
			if (isRunning()) {
				setCount(mortalDuration);
				actionbarChannel.update("§4필멸§f: " + df.format(getCount() / 20) + "초");
			}
		}
		
		@EventHandler(priority = EventPriority.MONITOR)
		public void onEntityDamageByEntity(EntityDamageByEntityEvent e) {
			if (e.getEntity().equals(player)) {
				Player player = (Player) e.getEntity();
				if (player.getHealth() - e.getFinalDamage() <= 0) {
					e.setCancelled(true);
					player.setHealth(0);
				}
			}
		}
		
		@Override
		protected void onEnd() {
			onSilentEnd();
		}
		
		@Override
		protected void onSilentEnd() {
			HandlerList.unregisterAll(this);
			mortals.remove(player);
			if (actionbarChannel != null) {
				actionbarChannel.unregister();	
			}
		}
    	
    }
	
}
