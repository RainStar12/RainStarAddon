package RainStarAbility;

import java.util.ArrayList;
import java.util.Random;

import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.entity.AreaEffectCloud;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import daybreak.abilitywar.ability.AbilityBase;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.SubscribeEvent;
import daybreak.abilitywar.ability.Tips;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.ability.Tips.Description;
import daybreak.abilitywar.ability.Tips.Difficulty;
import daybreak.abilitywar.ability.Tips.Level;
import daybreak.abilitywar.ability.Tips.Stats;
import daybreak.abilitywar.ability.decorator.ActiveHandler;
import daybreak.abilitywar.config.ability.AbilitySettings.SettingObject;
import daybreak.abilitywar.config.enums.CooldownDecrease;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.AbstractGame.Participant.ActionbarNotification.ActionbarChannel;
import daybreak.abilitywar.utils.base.Formatter;
import daybreak.abilitywar.utils.base.collect.Pair;
import daybreak.abilitywar.utils.base.minecraft.nms.NMS;
import daybreak.google.common.collect.ImmutableMap;

@AbilityManifest(name = "잔류 화살", rank = Rank.B, species = Species.HUMAN, explain = {
		"활로 적을 맞히면 포션 지대를 잔류시킵니다. $[COOLDOWN]",
		"철괴 우클릭 시 포션 효과를 다음으로 넘깁니다. $[ACTIVE]"
		},
		summarize = {
		"활로 적을 맞히면 포션 지대를 잔류시킵니다. $[COOLDOWN]",
		"철괴 우클릭 시 포션 효과를 다음으로 넘깁니다. $[ACTIVE]"
		})

@Tips(tip = {
        "적의 위치에 5~20초 지속의 무작위 포션 효과를 거는",
        "잔류 구름을 10초간 유지시키는 능력입니다.",
        "하지만 좋은 효과도, 나쁜 효과도, 중립적 효과도 전부",
        "나올 수 있으니 행운이 따르길 기도하세요."
}, strong = {
        @Description(subject = "판단력", explain = {
        		"적에게 건 포션 버프 효과를 보고 빠른 판단력으로",
        		"적에게서 거리를 벌릴 지, 같이 버프를 받을 지",
        		"혹은 적을 공격할 지 결단내려야 합니다."
        }),
        @Description(subject = "행운", explain = {
        		"결과적으로 나쁜 효과의 화살이 걸리는 것이 좋습니다.",
        		"행운이 따르길 기도하세요."
        })
}, weak = {
        @Description(subject = "불운", explain = {
        		"적에게 좋은 효과의 화살을 걸어버리면 역효과가 나겠죠?",
        		"불운이 없기를 기도하세요."
        })
}, stats = @Stats(offense = Level.ZERO, survival = Level.ZERO, crowdControl = Level.TWO, mobility = Level.ZERO, utility = Level.ZERO), difficulty = Difficulty.EASY)

public class LingeringArrow extends AbilityBase implements ActiveHandler {
	
	public LingeringArrow(Participant participant) {
		super (participant);
	}
	
	private final Cooldown arrowC = new Cooldown(COOLDOWN.getValue(), CooldownDecrease._50);
	private final Cooldown activeC = new Cooldown(ACTIVE.getValue(), CooldownDecrease._50);
	private ActionbarChannel actionbar = newActionbarChannel();
	private Random random = new Random();
	
	public static final SettingObject<Integer> COOLDOWN 
	= abilitySettings.new SettingObject<Integer>(LingeringArrow.class,
			"cooldown", 12, "# 쿨타임") {
		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}

		@Override
		public String toString() {
			return Formatter.formatCooldown(getValue());
		}
	}, ACTIVE = abilitySettings.new SettingObject<Integer>(LingeringArrow.class,
			"active-cooldown", 30, "# 변경 쿨타임") {
		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}

		@Override
		public String toString() {
			return Formatter.formatCooldown(getValue());
		}
	};

	private PotionEffectType potionEffect = new ArrayList<>(POTION_TYPES.keySet()).get(random.nextInt(POTION_TYPES.size()));

	@Override
	protected void onUpdate(Update update) {
		if (update == Update.RESTRICTION_CLEAR) {
			potionEffect = new ArrayList<>(POTION_TYPES.keySet()).get(random.nextInt(POTION_TYPES.size()));
			actionbar.update("§b다음 효과§f: §e" + POTION_TYPES.get(potionEffect).getLeft());
		}
	}

	public boolean ActiveSkill(Material material, ClickType clicktype) {
		if (material.equals(Material.IRON_INGOT) && clicktype.equals(ClickType.RIGHT_CLICK) && !activeC.isCooldown()) {
			potionEffect = new ArrayList<>(POTION_TYPES.keySet()).get(random.nextInt(POTION_TYPES.size()));
			actionbar.update("§b다음 효과§f: §e" + POTION_TYPES.get(potionEffect).getLeft());
			return activeC.start();
		}
		return false;
	}
	
	private static final ImmutableMap<PotionEffectType, Pair<String, Color>> POTION_TYPES 
	= ImmutableMap.<PotionEffectType, Pair<String, Color>>builder()
			.put(PotionEffectType.REGENERATION, Pair.of("§d재생", PotionEffectType.REGENERATION.getColor()))
			.put(PotionEffectType.SPEED, Pair.of("§b신속", PotionEffectType.SPEED.getColor()))
			.put(PotionEffectType.FIRE_RESISTANCE, Pair.of("§c화염 저항", PotionEffectType.FIRE_RESISTANCE.getColor()))
			.put(PotionEffectType.HEAL, Pair.of("§d치유", PotionEffectType.HEAL.getColor()))
			.put(PotionEffectType.NIGHT_VISION, Pair.of("§9야간 투시", PotionEffectType.NIGHT_VISION.getColor()))
			.put(PotionEffectType.INCREASE_DAMAGE, Pair.of("§6힘", PotionEffectType.INCREASE_DAMAGE.getColor()))
			.put(PotionEffectType.JUMP, Pair.of("§a점프 강화", PotionEffectType.JUMP.getColor()))
			.put(PotionEffectType.WATER_BREATHING, Pair.of("§3수중 호흡", PotionEffectType.WATER_BREATHING.getColor()))
			.put(PotionEffectType.INVISIBILITY, Pair.of("§7투명화", PotionEffectType.INVISIBILITY.getColor()))
			.put(PotionEffectType.LUCK, Pair.of("§a행운", PotionEffectType.LUCK.getColor()))
			.put(PotionEffectType.POISON, Pair.of("§2독", PotionEffectType.POISON.getColor()))
			.put(PotionEffectType.WEAKNESS, Pair.of("§7나약함", PotionEffectType.WEAKNESS.getColor()))
			.put(PotionEffectType.SLOW, Pair.of("§8구속", PotionEffectType.SLOW.getColor()))
			.put(PotionEffectType.HARM, Pair.of("§4고통", PotionEffectType.HARM.getColor()))
			// 이 아래는 없는 효과들
			.put(PotionEffectType.WITHER, Pair.of("§0시듦", Color.fromRGB(1, 1, 1)))
			.put(PotionEffectType.ABSORPTION, Pair.of("§e흡수", Color.fromRGB(254, 246, 18)))
			.put(PotionEffectType.BLINDNESS, Pair.of("§7실명", Color.fromRGB(140, 140, 140)))
			.put(PotionEffectType.CONFUSION, Pair.of("§5멀미", Color.fromRGB(171, 130, 18)))
			.put(PotionEffectType.FAST_DIGGING, Pair.of("§e성급함", Color.fromRGB(254, 254, 143)))
			.put(PotionEffectType.GLOWING, Pair.of("§f발광", Color.fromRGB(254, 254, 254)))
			.put(PotionEffectType.HEALTH_BOOST, Pair.of("§c생명력 강화", Color.fromRGB(254, 178, 217)))
			.put(PotionEffectType.HUNGER, Pair.of("§2허기", Color.fromRGB(134, 229, 127)))
			.put(PotionEffectType.LEVITATION, Pair.of("§5공중 부양", Color.fromRGB(171, 18, 151)))
			.put(PotionEffectType.SATURATION, Pair.of("§e포만감", Color.fromRGB(254, 221, 115)))
			.put(PotionEffectType.SLOW_DIGGING, Pair.of("§8채굴 피로", Color.fromRGB(93, 93, 93)))
			.put(PotionEffectType.UNLUCK, Pair.of("§a불운", Color.fromRGB(206, 242, 121)))
			.put(PotionEffectType.DAMAGE_RESISTANCE, Pair.of("§8저항", Color.fromRGB(1, 96, 106)))
			.build();
	
	
	@SubscribeEvent
	public void onEntityDamageByEntity(EntityDamageByEntityEvent e) {
		if (NMS.isArrow(e.getDamager())) {
			Arrow arrow = (Arrow) e.getDamager();
			if (getPlayer().equals(arrow.getShooter()) && e.getEntity() instanceof Player
					&& !e.getEntity().equals(getPlayer()) && !arrowC.isCooldown()) {
				final Player target = (Player) e.getEntity();
				AreaEffectCloud AEC
					= target.getPlayer().getWorld().spawn(target.getPlayer().getLocation().add(0, 0.2, 0), 
							AreaEffectCloud.class);
				AEC.setDuration(200);
				AEC.addCustomEffect(new PotionEffect((potionEffect), (random.nextInt(15) + 5) * 20, 0), true);
				AEC.setColor(POTION_TYPES.get(potionEffect).getRight());
				AEC.setWaitTime(0);
				getPlayer().sendMessage(POTION_TYPES.get(potionEffect).getLeft() + "§f 지대가 생성되었습니다.");
				arrowC.start();

				potionEffect = new ArrayList<>(POTION_TYPES.keySet()).get(random.nextInt(POTION_TYPES.size()));
				actionbar.update("§b다음 효과§f: §e" + POTION_TYPES.get(potionEffect).getLeft());
			}
		}
	}
	
}