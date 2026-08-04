package rainstar.abilitywar.synergy;

import static rainstar.abilitywar.system.config.RainStarAbilitySetting.rsAbilitySettings;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.StringJoiner;
import java.util.UUID;

import javax.annotation.Nullable;

import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Note;
import org.bukkit.World;
import org.bukkit.FireworkEffect.Type;
import org.bukkit.Note.Tone;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.attribute.AttributeModifier.Operation;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Damageable;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Firework;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByBlockEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.entity.EntityRegainHealthEvent.RegainReason;
import org.bukkit.event.entity.EntityResurrectEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import daybreak.abilitywar.AbilityWar;
import daybreak.abilitywar.ability.AbilityBase;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.SubscribeEvent;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.ability.decorator.ActiveHandler;
import daybreak.abilitywar.ability.event.AbilityPreActiveSkillEvent;
import daybreak.abilitywar.ability.event.AbilityPreTargetEvent;
import daybreak.abilitywar.config.ability.AbilitySettings.SettingObject;
import daybreak.abilitywar.game.AbstractGame.CustomEntity;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.event.participant.ParticipantDeathEvent;
import daybreak.abilitywar.game.list.mix.Mix;
import daybreak.abilitywar.game.list.mix.synergy.Synergy;
import daybreak.abilitywar.game.manager.effect.Oppress;
import daybreak.abilitywar.game.manager.effect.event.ParticipantPreEffectApplyEvent;
import daybreak.abilitywar.game.manager.effect.registry.EffectType;
import daybreak.abilitywar.game.module.DeathManager;
import daybreak.abilitywar.game.team.interfaces.Teamable;
import daybreak.abilitywar.utils.base.Formatter;
import daybreak.abilitywar.utils.base.Messager;
import daybreak.abilitywar.utils.base.collect.LimitedPushingList;
import daybreak.abilitywar.utils.base.color.Gradient;
import daybreak.abilitywar.utils.base.color.RGB;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.base.math.LocationUtil;
import daybreak.abilitywar.utils.base.math.VectorUtil;
import daybreak.abilitywar.utils.base.math.geometry.Circle;
import daybreak.abilitywar.utils.base.minecraft.entity.health.Healths;
import daybreak.abilitywar.utils.base.minecraft.item.builder.ItemBuilder;
import daybreak.abilitywar.utils.base.minecraft.nms.IHologram;
import daybreak.abilitywar.utils.base.minecraft.nms.NMS;
import daybreak.abilitywar.utils.base.minecraft.version.ServerVersion;
import daybreak.abilitywar.utils.base.random.Random;
import daybreak.abilitywar.utils.library.MaterialX;
import daybreak.abilitywar.utils.library.ParticleLib;
import daybreak.abilitywar.utils.library.PotionEffects;
import daybreak.abilitywar.utils.library.SoundLib;
import daybreak.abilitywar.utils.library.item.EnchantLib;
import daybreak.google.common.base.Predicate;
import daybreak.google.common.base.Strings;
import daybreak.google.common.collect.ArrayListMultimap;
import daybreak.google.common.collect.ImmutableMap;
import daybreak.google.common.collect.ImmutableSet;
import daybreak.google.common.collect.Multimap;
import rainstar.abilitywar.effect.BindingSmoke;
import rainstar.abilitywar.effect.Burn;
import rainstar.abilitywar.effect.Corrosion;
import rainstar.abilitywar.effect.Dream;
import rainstar.abilitywar.effect.Moisture;
import rainstar.abilitywar.system.event.SkyWhaleExpEarnEvent;
import rainstar.abilitywar.utils.Healing;
import rainstar.abilitywar.utils.RankColor;

@AbilityManifest(name = "루시드 드림", rank = Rank.SPECIAL, species = Species.ANIMAL, explain = {
		"§7패시브 §8- §a루시드 드림§f: 10가지의 §a현실 스킬§f을 얻고 시작합니다.",
		" §a현실 스킬§f은 영역이 지속 중이지 않을 때만 효과를 얻습니다.",
		"§7패시브 §8- §d드림 이터§f: 능력 소지자에게 입힌 최종 피해 × 대상의 능력 등급만큼",
		" §a꿈 경험치§f를 획득해 일정량 모으는 것으로 §a꿈 레벨§f을 상승시킬 수 있습니다.",
		" 이때 3가지의 선택지로 §a꿈 스킬§f을 얻으며, 그와 동시에 §a현실 스킬§f도 하나 사라집니다.",
		" §a꿈 스킬과 남은 현실 스킬§f은 §7철괴를 좌클릭§f하여 볼 수 있습니다.",
		"§7철괴 우클릭 §8- §b드림 아쿠아리움§f: $[DURATION]초간 나갈 수 없는 §b꿈§f의 §b들판§f을 펼칩니다.",
		" 또한 §a꿈 레벨§f에 따른 추가 효과를 사용할 수 있습니다. $[COOLDOWN]",
		"§7영역 내 패시브 §8- §3웨이브§f: 영역의 중심에서부터 파도가 퍼져나가",
		" 적을 피해입히며 밀쳐냅니다. 피해는 중심에서 멀어질수록 강력해집니다.",
		" 피해는 영역에서 자신의 최고 대미지에 비례§8(§7× 0.5~1.5§8)§f합니다.",
		"§b[§7아이디어 제공자§b] §bSleepground"
		})
public class LucidDream extends Synergy implements ActiveHandler {

	final Multimap<Integer, DreamSkill> dreamSkillList = ArrayListMultimap.create();
	
	public LucidDream(Participant participant) {
		super(participant);
		
		for (DreamSkill skill : DreamSkill.values()) {
			dreamSkillList.put(skill.getQuality(), skill);
		}
	}
	
	enum DreamSkill {
		FIRE_HEAL("화염 치유+", "모든 화염계 피해를 회복으로 받습니다.", 0),
		OTHER_SIDE("건너편+", "영역의 끝에 다다르면 반대편 영역의 끝으로 이동하고, 흡수 체력이 없다면 흡수 체력을 한 칸 얻습니다. 자신만 적용됩니다.", 0),
		REVERSE("역방향+", "파도가 영역의 끝에서 중심을 향해 치며 파도 주기가 20% 감소합니다. 이때 대미지는 중심쪽이 더 강해집니다.", 0),
		LUNAR_GRAVITY1("달의 인력+ I", "밤 시간에는 파도의 주기가 50% 감소합니다.", 0),
		FOAM1("물거품+ I", "영역을 개시할 때 체력을 최대 체력의 20%만큼 회복하고, 영역이 종료될 때 영역 내에서 받은 피해량 50%를 회복합니다.", 0),
		SURFING1("파도타기+ I", "파도에 자신이 닿으면 순간적으로 이동 속도가 30%, 공격력이 15% 증가합니다.", 0),
		DREAMY1("꿈결+ I", "영역 외부에서 가해지는 공격 피해를 받지 않습니다.", 0),
		
		WATER_ANSWER("물의 답+", "영역 내 모든 대상의 체력과 능력을 확인 가능합니다.", 1),
		VORTEX("소용돌이+", "자신이 공격한 적을 영역 내부 쪽으로 강하게 끌어당깁니다.", 1),
		RIPTIDE("이안류+", "영역 내에서 자신 외 블럭 설치 / 소모품 사용이 불가능해집니다.", 1),
		RAGING_WAVE1("거센 파도+ I", "파도 피해량과 넉백이 30% 증가합니다.", 1),
		FASTWALK1("빠른 걸음+ I", "영역 내에서 이동 속도가 25% 증가합니다.", 1),
		AREA_REDUCE1("영역 축소+ I", "영역 범위가 15칸에서 10칸으로 축소되고 파도 거리 피해량이 1배부터 시작합니다.", 1),
		LUNAR_GRAVITY2("달의 인력+ II", "밤 시간에는 파도의 주기가 50% 감소하고, 지속 시간이 천천히 흐릅니다. 영역이 지속되는 동안 세상의 시간이 밤이 됩니다.", 1),
		FOAM2("물거품+ II", "영역을 개시할 때 체력을 최대 체력의 30%만큼 회복하고, 영역이 종료될 때 영역 내에서 받은 피해량 75%를 회복합니다.", 1),
		SURFING2("파도타기+ II", "파도에 자신이 닿으면 순간적으로 이동 속도가 60%, 공격력이 30% 증가하고, 체력을 5% 회복합니다.", 1),
		DREAMY2("꿈결+ II", "영역 외부에서 가해지는 공격 피해를 받지 않고, 반격합니다.", 1),
		
		FLOOD("침수+", "파도에 맞은 적은 다음 파도에 피해를 20% 더 받습니다. 중첩됩니다.", 2),
		MARINE_LAYER("물안개+", "파도에 맞은 적을 1.5초간 실명에 빠뜨리고, 0.3초간 제압합니다.", 2),
		NOTHINGNESS("무+", "영역과 파도의 파티클과 소리를 자신만 볼 수 있습니다.", 2),
		SWORDAURA("검기+", "매 초마다 검을 휘두를 때 지형지물과 생명체를 관통하는 특수 투사체를 발사합니다.", 2),
		OVERHYDRATION1("수분 과다+ I", "파도에 맞은 적은 2초간 부식되고 습기를 받습니다.", 2),
		ABYSS1("심해+ I", "영역 내에서 자신 외 자연 회복의 회복량이 100%, 그 외 회복량이 50% 감소합니다.", 2),
		RAGING_WAVE2("거센 파도+ II", "파도 피해량과 넉백이 50% 증가합니다.", 2),
		FASTWALK2("빠른 걸음+ II", "영역 내에서 이동 속도가 50% 증가합니다.", 2),
		AREA_REDUCE2("영역 축소+ II", "영역 범위가 10칸에서 5칸으로 축소되고 파도 거리 피해량이 항상 1.5배가 됩니다.", 2),
		FOAM3("물거품+ III", "영역을 개시할 때 체력을 최대 체력의 40%만큼 회복하고, 영역이 종료될 때 영역 내에서 자신이 받은 피해를 전부 회복합니다.", 2),
		
		SINK("침몰+", "체력이 40% 이하인 적은 파도 피해를 50% 더 받고 1.5초간 속박의 연막이 부여됩니다.", 3),
		ABILITY_CONTROL("능력 장악+", "영역 내에서 자신 외 액티브 / 타게팅 스킬을 발동하려 하면 취소되고 체력을 10% 잃습니다.", 3),
		WAKE_UP("꿈 깨시지+", "치명적인 피해를 입을 때 영역이 즉시 종료되고 쿨타임이 절반 감소합니다. 물거품이 있을 때만 획득 가능합니다.", 3),
		FREEDOM("자유 해방+", "본인은 영역 밖으로 나갈 수 있으며, 영역의 중심이 그 위치로 이동됩니다. 건너편이 있으면 등장하지 않습니다.", 3),
		SLEEP_WAVE("수면파+", "40% 확률로 파도에 맞은 적이 5초간 몽환에 빠집니다.", 3),
		INCEPTION("몽중몽+", "영역 지속 중 적 처치 시 / 꿈 레벨 업 시 지속시간이 30초 추가됩니다.", 3),
		CALL_OF_SEA("바다의 부름+", "40칸 내의 생명체가 영역 중심으로 끌어당겨집니다.", 3),
		FLIGHT("비행+", "자유로운 비행이 가능하고 비행 중에는 타게팅 불가가 됩니다.", 3),
		SCALD("열탕+", "파도에 맞은 적을 5초간 불태우고 화상을 입힙니다.", 3),
		OVERHYDRATION2("수분 과다+ II", "파도에 맞은 적은 3.5초간 부식되고 습기를 받습니다.", 3),
		ABYSS2("심해+ II", "영역 내에서 자신 외 자연 회복의 회복량이 100%, 그 외 회복량이 50% 감소하고, 감소된 회복을 자신이 대신 받습니다.", 3),
		RAGING_WAVE3("거센 파도+ III", "파도 피해량과 넉백이 70% 증가합니다.", 3),
		
		INSTANT("즉발+", "꿈 영역이 연출 없이 즉시 전개되고, 지속 시간이 스킵된 만큼 증가합니다.", 4),
		STEP_UP("스텝 업+", "획득 이후 꿈 레벨 업 시마다 공격력이 10%씩 증가합니다.", 4),
		DREAM_COLLECTOR("꿈 수집가+", "즉시 무작위 꿈 스킬을 5개 획득합니다.", 4),
		TSUNAMI("쓰나미+", "파도 판정 범위가 수평으로 3배, 수직으로 60배 증가합니다. 파도가 영역을 넘어 조금 더 잔류합니다.", 4),
		SIRENS_BLESS("세이렌의 축복+", "등장하는 스킬의 선택지가 2개 늘어나고, 최저 퀄리티가 1씩 증가합니다. 4퀄리티 스킬이 등장할 확률이 증가합니다.", 4),
		EQUALITY("동등성+", "파도가 항상 최고 피해를 입힙니다. 앞으로 모든 퀄리티의 확률이 동등하게 등장합니다.", 4),
		LIMIT_BREAKER("리밋 브레이커+", "레벨 업을 20까지 할 수 있습니다. 10~20렙 간 레벨 업에서는 퀄리티 상관 없이 모든 스킬이 등장합니다.", 4);
		
		private final String name;
		private final String explain;
		private final int quality;
		
		private DreamSkill(String name, String explain, Integer quality) {
			this.name = name;
			this.explain = explain;
			this.quality = quality;
		}
		
		public String getName() {
			return name;
		}
		
		public String getExplain() {
			return explain;
		}
		
		public int getQuality() {
			return quality;
		}
		
	}
	
	enum RealSkill {
		EXP_FARM("경험치 증가", "꿈 경험치 획득량이 50% 증가합니다."),
		DAMAGE_DECREASE("받는 피해 감소", "받는 피해가 20% 감소합니다."),
		COOLDOWN_DECREASE("쿨타임 감소", "피격 시 남은 쿨타임의 20%가 감소합니다."),
		SPEED_UP("신속", "신속 1을 항시 얻습니다."),
		INVINCIBLE("연출 무적", "꿈 연출, 레벨 업 GUI가 전개되는 동안은 무적 상태가 됩니다."),
		LIFE_VESSEL("사망 연기", "치명적인 피해를 입으면 꿈 영역을 전개하고 최대 체력의 20%로 부활합니다."),
		REGENERATION("회복 속도 증가", "3초 동안 피격받지 않으면 회복 속도가 증가합니다."),
		ABILITY_RESTRICTION_IMMUNE("비활성화 무시", "능력 비활성화 상태이상을 무시합니다."),
		CONSUME_REGAIN("소모품 복귀", "소모품을 소모할 때 50% 확률로 그 소모품이 다시 돌아옵니다."),
		ABSORPTION("흡수 체력", "흡수 체력이 없다면 피격 시 흡수 체력을 반 칸 얻습니다.");
		
		private final String name;
		private final String explain;
		
		RealSkill(String name, String explain) {
			this.name = name;
			this.explain = explain;
		}
		
		public String getName() {
			return name;
		}
		
		public String getExplain() {
			return explain;
		}
		
	}
	
	private static final ImmutableMap<Rank, Double> rankmultiply = ImmutableMap.<Rank, Double>builder()
			.put(Rank.C, 1.0)
			.put(Rank.B, 2.0)
			.put(Rank.A, 3.0)
			.put(Rank.S, 4.0)
			.put(Rank.L, 5.0)
			.put(Rank.SPECIAL, 7.0)
			.build();

	
	public static final SettingObject<Integer> DURATION = rsAbilitySettings.new SettingObject<Integer>(
			LucidDream.class, "duration-amount", 20, "# 지속 시간") {

		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}

	};
	
	public static final SettingObject<Integer> COOLDOWN = rsAbilitySettings.new SettingObject<Integer>(
			LucidDream.class, "cooldown-amount", 85, "# 쿨타임") {

		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}

		@Override
		public String toString() {
			return Formatter.formatCooldown(getValue());
		}

	};
	
	public static final SettingObject<Integer> WAVE_PERIOD = rsAbilitySettings.new SettingObject<Integer>(
			LucidDream.class, "wave-period", 60, "# 파도의 주기 (단위: 틱, 1틱 = 0.05초)") {

		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}

	};
	
	@Override
	protected void onUpdate(Update update) {
		if (update == Update.RESTRICTION_CLEAR) {
			if (isSkillRunning == true) isSkillRunning = false;
			expbar.start();
			passive.start();
		}
	}

	private final Cooldown cooldown = new Cooldown(COOLDOWN.getValue(), 50);	
	private final int period = WAVE_PERIOD.getValue();
	private boolean isSkillRunning = false;
	private Random random = new Random();
	private static final FixedMetadataValue NULL_VALUE = new FixedMetadataValue(AbilityWar.getPlugin(), null);
	
	private int dreamlevel = 0;
	private double dreamexp = 0;
	
	private double bestDamage = 7;
	private double lostHealth = 0;
	
	private final Circle circle = Circle.of(15, 100);
	private final Circle circle2 = Circle.of(10, 75);
	private final Circle circle3 = Circle.of(5, 50);

	private Location center;
	private double currentRadius;
	private double nowrange = 15;
	private int damagestack = 0;
	private boolean spiral = false;
	private int noDamageTimer = 0;
	
	private BossBar bossbar;
	
	private int stacks;
	private boolean turns = true;
	
	private final DecimalFormat df = new DecimalFormat("0");
	
	private List<DreamSkill> dreamSkills = new ArrayList<>();
	private List<RealSkill> realSkills = new ArrayList<>(Arrays.asList(
		    RealSkill.ABILITY_RESTRICTION_IMMUNE,
		    RealSkill.ABSORPTION,
		    RealSkill.CONSUME_REGAIN,
		    RealSkill.COOLDOWN_DECREASE,
		    RealSkill.DAMAGE_DECREASE,
		    RealSkill.EXP_FARM,
		    RealSkill.INVINCIBLE,
		    RealSkill.LIFE_VESSEL,
		    RealSkill.REGENERATION,
		    RealSkill.SPEED_UP
		));
	
	private Map<Damageable, Integer> damageMap = new HashMap<>();

	private final static Color sky = Color.fromRGB(72, 254, 254), mint = Color.fromRGB(236, 254, 254), snow = Color.fromRGB(28, 254, 243),
			teal = Color.TEAL, lime = Color.fromRGB(49, 254, 32), yellow = Color.YELLOW, pink = Color.fromRGB(254, 174, 201),
			orange = Color.fromRGB(254, 177, 32), applemint = Color.fromRGB(34, 253, 220);
	
	private RGB aqua1 = RGB.of(74, 208, 229), aqua2 = RGB.of(85, 212, 231), aqua3 = RGB.of(96, 216, 232),
			aqua4 = RGB.of(107, 221, 234), aqua5 = RGB.of(118, 225, 235), aqua6 = RGB.of(130, 230, 237),
			aqua7 = RGB.of(148, 234, 240), aqua8 = RGB.of(167, 238, 243), aqua9 = RGB.of(186, 242, 245),
			aqua10 = RGB.of(204, 245, 247), aqua11 = RGB.of(223, 249, 250);
	
	@SuppressWarnings("serial")
	private List<RGB> aquas = new ArrayList<RGB>() {
		{
			add(aqua1);
			add(aqua2);
			add(aqua3);
			add(aqua4);
			add(aqua5);
			add(aqua6);
			add(aqua7);
			add(aqua8);
			add(aqua9);
			add(aqua10);
			add(aqua11);
		}
	};
	
	private RGB aqua = aqua1;
	
	private final Predicate<Entity> predicate = new Predicate<Entity>() {
		@Override
		public boolean test(Entity entity) {
			if (entity.equals(getPlayer()))
				return false;
			if (entity instanceof Player) {
				if (!getGame().isParticipating(entity.getUniqueId())
						|| (getGame() instanceof DeathManager.Handler && ((DeathManager.Handler) getGame())
								.getDeathManager().isExcluded(entity.getUniqueId()))
						|| !getGame().getParticipant(entity.getUniqueId()).attributes().TARGETABLE.getValue()) {
					return false;
				}
				if (getGame() instanceof Teamable) {
					final Teamable teamGame = (Teamable) getGame();
					final Participant entityParticipant = teamGame.getParticipant(entity.getUniqueId()),
							participant = getParticipant();
					return !teamGame.hasTeam(entityParticipant) || !teamGame.hasTeam(participant)
							|| (!teamGame.getTeam(entityParticipant).equals(teamGame.getTeam(participant)));
				}
			}
			return true;
		}

		@Override
		public boolean apply(@Nullable Entity arg0) {
			return false;
		}
	};
	
	private final Predicate<Entity> soundpredicate = new Predicate<Entity>() {
		@Override
		public boolean test(Entity entity) {
			return true;
		}

		@Override
		public boolean apply(@Nullable Entity arg0) {
			return false;
		}
	};
	
	private static final Set<Material> swords;
	
	static {
		if (MaterialX.NETHERITE_SWORD.isSupported()) {
			swords = ImmutableSet.of(MaterialX.WOODEN_SWORD.getMaterial(), Material.STONE_SWORD, Material.IRON_SWORD, MaterialX.GOLDEN_SWORD.getMaterial(), Material.DIAMOND_SWORD, MaterialX.NETHERITE_SWORD.getMaterial());
		} else {
			swords = ImmutableSet.of(MaterialX.WOODEN_SWORD.getMaterial(), Material.STONE_SWORD, Material.IRON_SWORD, MaterialX.GOLDEN_SWORD.getMaterial(), Material.DIAMOND_SWORD);
		}
	}
	
	private static boolean isNight(long worldtime) {
		return worldtime >= 13000;
	}
	
	public String getOver(int level) {
		switch(level) {
		case 10: return "";
		case 11: return "+";
		case 12: return "++";
		case 13: return "+++";
		case 14: return "++++";
		case 15: return "+++++";
		case 16: return "!+";
		case 17: return "!++";
		case 18: return "!+++";
		case 19: return "!++++";
		case 20: return "!+++++";
		}
		return "";
	}
	
	public boolean ActiveSkill(Material material, ClickType clicktype) {
	    if (material.equals(Material.IRON_INGOT)) {
	    	if (clicktype.equals(ClickType.RIGHT_CLICK)) {
		    	if (!isSkillRunning && !cooldown.isCooldown()) {
	    	   		if (dreamSkills.contains(DreamSkill.INSTANT)) {
	    				if (dreamSkills.contains(DreamSkill.AREA_REDUCE2)) nowrange = 5;
	    				else if (dreamSkills.contains(DreamSkill.AREA_REDUCE1)) nowrange = 10;
	    				else nowrange = 15;
	    				
	    				center = LocationUtil.floorY(getPlayer().getLocation().clone());
	    				currentRadius = 0;
	    				final Firework firework = getPlayer().getWorld().spawn(center, Firework.class);
	    				final FireworkMeta meta = firework.getFireworkMeta();
	    				meta.addEffect(
	    						FireworkEffect.builder()
	    						.withColor(aqua1.getColor(), aqua11.getColor(), Color.AQUA)
	    						.with(Type.BALL_LARGE)
	    						.build()
	    				);
	    				meta.setPower(0);
	    				firework.setFireworkMeta(meta);
	    				firework.setMetadata("Firework", NULL_VALUE);
	    				new BukkitRunnable() {
	    					@Override
	    					public void run() {
	    						firework.detonate();
	    					}
	    				}.runTaskLater(AbilityWar.getPlugin(), 1L);
	    				SoundLib.ENTITY_PLAYER_SPLASH.playSound(center, 2, 0.5f);
	    	   			skill.start();
	    	   			skill.setCount(skill.getCount() + 70);
	    	   		} else showSpiral(RGB.AQUA);
	    	   		isSkillRunning = true;
	    	   		return true;
		    	} else if (isSkillRunning) {
		    		getPlayer().sendMessage("§3[§b!§3] §c아직 스킬이 지속 중입니다.");
		    	}	
	    	} else if (clicktype.equals(ClickType.LEFT_CLICK)) {
	    		getPlayer().sendMessage("§3=========== §a현실 스킬 §3===========");
	    		final StringJoiner joiner1 = new StringJoiner("\n");
	    		for (RealSkill realskill : realSkills) {
	    			joiner1.add("§2" + realskill.getName() + "§7: §a" + realskill.getExplain());
	    		}
	    		getPlayer().sendMessage(joiner1.toString());
	    		getPlayer().sendMessage("§e=========== §b꿈 스킬 §e===========");
	    		final StringJoiner joiner2 = new StringJoiner("\n");
	    		joiner2.add("§aLevel " + (dreamlevel >= 10 ? "MAX" + getOver(dreamlevel) : dreamlevel));
	    		for (DreamSkill dreamskill : dreamSkills) {
	    			joiner2.add("§3" + dreamskill.getName() + "§7: §b" + dreamskill.getExplain());
	    		}
	    		getPlayer().sendMessage(joiner2.toString());
	    		getPlayer().sendMessage("§e================================");
	    	}
	    }
		return false;
	}
	
	private AbilityTimer passive = new AbilityTimer() {
		
		@Override
		public void run(int count) {
			if (realSkills.contains(RealSkill.SPEED_UP)) PotionEffects.SPEED.addPotionEffect(getPlayer(), 10, 0, false);
			if (realSkills.contains(RealSkill.REGENERATION)) {
				noDamageTimer++;
				if (noDamageTimer >= 60) {
					Healing.heal(getPlayer(), 0.025, RegainReason.REGEN);
				}
			}
		}
		
	}.setPeriod(TimeUnit.TICKS, 1).register();
	
    private final AbilityTimer attackcool = new AbilityTimer(20) {
    	
    	@Override
		public void run(int count) {
    	}
    	
    }.setPeriod(TimeUnit.TICKS, 1).register();
    
	@SubscribeEvent(priority = 1000)
	public void onEntityDie(EntityDamageEvent e) {
		if (e.getEntity().equals(getPlayer()) && !isSkillRunning && realSkills.contains(RealSkill.LIFE_VESSEL)) {
			if (getPlayer().getHealth() - e.getFinalDamage() <= 0) {
		   		e.setCancelled(true);
		   		double maxHealth = getPlayer().getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue();
		   		Healths.setHealth(getPlayer(), maxHealth * 0.2);
				if (dreamSkills.contains(DreamSkill.INSTANT)) {
		   			skill.start();
		   			skill.setCount(skill.getCount() + 70);
		   		} else showSpiral(RGB.AQUA);
		   		isSkillRunning = true;
			}
		}
		
		if (e.getEntity().equals(getPlayer()) && realSkills.contains(RealSkill.REGENERATION) && !e.isCancelled()) noDamageTimer = 0;
	}
	
	@SubscribeEvent(priority = 5)
	public void onEntityDamage(EntityDamageEvent e) {
		if (e.getEntity().equals(getPlayer())) {
			if (skill.isRunning() && LocationUtil.isInCircle(center, getPlayer().getLocation(), nowrange)) {
				if (dreamSkills.contains(DreamSkill.FIRE_HEAL)) {
					if (e.getCause().equals(DamageCause.LAVA) || e.getCause().equals(DamageCause.FIRE_TICK) || e.getCause().equals(DamageCause.FIRE) || e.getCause().equals(DamageCause.HOT_FLOOR)) {
						Healing.heal(getPlayer(), e.getFinalDamage(), RegainReason.CUSTOM);
						e.setDamage(0);
						e.setCancelled(true);
					}	
				}
				
				if (dreamSkills.contains(DreamSkill.FOAM3)) lostHealth += e.getFinalDamage();	
				else if (dreamSkills.contains(DreamSkill.FOAM2)) lostHealth += (e.getFinalDamage() * 0.75);
				else if (dreamSkills.contains(DreamSkill.FOAM1)) lostHealth += (e.getFinalDamage() * 0.5);
				
				if (getPlayer().getHealth() - e.getFinalDamage() <= 0 && dreamSkills.contains(DreamSkill.WAKE_UP)) {
					ParticleLib.ITEM_CRACK.spawnParticle(getPlayer().getLocation(), 0, 1, 0, 100, 0.3, MaterialX.CLOCK);
					SoundLib.BLOCK_END_PORTAL_SPAWN.playSound(getPlayer().getLocation(), 1, 1.5f);
					e.setCancelled(true);
					skill.stop(false);
					cooldown.setCooldown(cooldown.getCount() / 2);
				}
			}
			if (!skill.isRunning()) {
				if (realSkills.contains(RealSkill.DAMAGE_DECREASE)) e.setDamage(e.getDamage() * 0.8);
				if (realSkills.contains(RealSkill.INVINCIBLE)) {
					if (biggerEffect.isRunning() || spiral) {
						e.setCancelled(true);
					}
				}
			}
		}
	}
	
	@SubscribeEvent
	public void onParticipantDeath(ParticipantDeathEvent e) {
		if (getPlayer().equals(e.getParticipant().getPlayer().getKiller()) && skill.isDuration() && dreamSkills.contains(DreamSkill.INCEPTION)) {
			skill.setCount(skill.getCount() + 600);
		}
	}
	
	@SubscribeEvent
	public void onParticipantPreEffectApply(ParticipantPreEffectApplyEvent e) {
		if (!skill.isRunning() && e.getParticipant().equals(getParticipant()) && realSkills.contains(RealSkill.ABILITY_RESTRICTION_IMMUNE) && Arrays.asList(e.getEffectType().getManifest().type()).contains(EffectType.ABILITY_RESTRICTION)) {
			e.setCancelled(true);
		}
	}
	
	@SubscribeEvent
	public void onEntityDamageByBlock(EntityDamageByBlockEvent e) {
		onEntityDamage(e);
	}
	
	@SubscribeEvent(priority = 5)
	public void onEntityDamageByEntity(EntityDamageByEntityEvent e) {
		onEntityDamage(e);
		if (e.getDamager().hasMetadata("Firework")) {
			e.setCancelled(true);
		}
		
		Player damager = null;
		if (e.getDamager() instanceof Projectile) {
			Projectile projectile = (Projectile) e.getDamager();
			if (projectile.getShooter() instanceof Player) damager = (Player) projectile.getShooter();
		} else if (e.getDamager() instanceof Player) damager = (Player) e.getDamager();
		
		if (getPlayer().equals(damager)) {
			if (skill.isRunning()) {
				e.setDamage(e.getDamage() * (1 + (damagestack * 0.1)));
				if (dreamSkills.contains(DreamSkill.VORTEX)) {
					e.getEntity().setVelocity(VectorUtil.validateVector(center.toVector().subtract(e.getEntity().getLocation().toVector()).normalize().setY(0).multiply(1.35)));
				}
			}
			
			if (speedup.isRunning()) {
				e.setDamage(e.getDamage() * (1 + (dreamSkills.contains(DreamSkill.SURFING2) ? 0.3 : 0.15)));
			}

			if (e.getEntity() instanceof Player && getGame().isParticipating(((Player) e.getEntity()))) {
				Player player = (Player) e.getEntity();
				if (getGame().getParticipant(player).hasAbility()) {
					Rank rankvalue = Rank.C;
					AbilityBase ab = getGame().getParticipant(player).getAbility();
					if (ab.getClass().equals(Mix.class)) {
						Mix mix = (Mix) ab;
						if (mix.hasSynergy()) rankvalue = mix.getSynergy().getRank();
						else {
							Mix mymix = (Mix) getParticipant().getAbility();
							if (mymix.hasSynergy()) rankvalue = mix.getFirst().getRank();
							else if (mymix.getFirst().equals(this)) rankvalue = mix.getFirst().getRank();
							else if (mymix.getSecond().equals(this)) rankvalue = mix.getSecond().getRank();
						}
					} else rankvalue = ab.getRank();
					if ((dreamlevel < 20 && dreamSkills.contains(DreamSkill.LIMIT_BREAKER)) || dreamlevel < 10) {
    					final SkyWhaleExpEarnEvent event = new SkyWhaleExpEarnEvent(getParticipant());
    					Bukkit.getPluginManager().callEvent(event);
    					if (!event.isCancelled()) {
    						double multiply = 1;
    						if (!skill.isRunning() && realSkills.contains(RealSkill.EXP_FARM)) multiply = 2;
    						dreamexp += (e.getFinalDamage() * 5 * rankmultiply.get(rankvalue) * multiply);
    						SoundLib.ENTITY_EXPERIENCE_ORB_PICKUP.playSound(getPlayer(), 1, 1.35f);	
    					}
					}
				}
			}
		}
		
		if (skill.isRunning()) {
			if (e.getDamager().hasMetadata("Wave")) {
				if ((e.getDamage() * 0.8) > bestDamage) bestDamage = e.getDamage() * 0.8;
				
				if (e.getEntity() instanceof Player && getGame().isParticipating(((Player) e.getEntity()))) {
					Player player = (Player) e.getEntity();
					if (getGame().getParticipant(player).hasAbility()) {
						Rank rankvalue = Rank.C;
						AbilityBase ab = getGame().getParticipant(player).getAbility();
						if (ab.getClass().equals(Mix.class)) {
							Mix mix = (Mix) ab;
							if (mix.hasSynergy()) rankvalue = mix.getSynergy().getRank();
							else {
								Mix mymix = (Mix) getParticipant().getAbility();
								if (mymix.hasSynergy()) rankvalue = mix.getFirst().getRank();
								else if (mymix.getFirst().equals(this)) rankvalue = mix.getFirst().getRank();
								else if (mymix.getSecond().equals(this)) rankvalue = mix.getSecond().getRank();
							}
						} else rankvalue = ab.getRank();
						if ((dreamlevel < 20 && dreamSkills.contains(DreamSkill.LIMIT_BREAKER)) || dreamlevel < 10) {
	    					final SkyWhaleExpEarnEvent event = new SkyWhaleExpEarnEvent(getParticipant());
	    					Bukkit.getPluginManager().callEvent(event);
	    					if (!event.isCancelled()) {
	    						dreamexp += (e.getFinalDamage() * 5 * rankmultiply.get(rankvalue));
	    						SoundLib.ENTITY_EXPERIENCE_ORB_PICKUP.playSound(getPlayer(), 1, 1.35f);	
	    					}
						}
					}
				}
			}
			
			if (damager != null && getPlayer().equals(e.getEntity()) && !LocationUtil.isInCircle(center, damager.getLocation(), nowrange)) {
				if (dreamSkills.contains(DreamSkill.DREAMY2) || dreamSkills.contains(DreamSkill.DREAMY1)) {
					if (dreamSkills.contains(DreamSkill.DREAMY2)) damager.damage(e.getDamage());
					e.setCancelled(true);
					e.setDamage(0);
				}
			}
		} else if (e.getEntity().equals(getPlayer()) && !getPlayer().equals(damager)) {
			if (realSkills.contains(RealSkill.COOLDOWN_DECREASE)) cooldown.setCount((int) (cooldown.getCount() * 0.8));
			if (realSkills.contains(RealSkill.ABSORPTION) && NMS.getAbsorptionHearts(getPlayer()) <= 0) NMS.setAbsorptionHearts(getPlayer(), 1);
		}
	}
	
	@SubscribeEvent
	public void onPreActiveSkill(AbilityPreActiveSkillEvent e) {
		if (skill.isRunning() && dreamSkills.contains(DreamSkill.ABILITY_CONTROL) && !e.getParticipant().equals(getParticipant()) && LocationUtil.isInCircle(center, e.getParticipant().getPlayer().getLocation(), nowrange)) {
			e.getParticipant().getPlayer().sendMessage("§3[§b루시드 드림§3] §c능력을 사용할 수 없습니다!");
			double maxHealth = e.getParticipant().getPlayer().getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue();
			Healths.setHealth(e.getParticipant().getPlayer(), Math.max(1, e.getParticipant().getPlayer().getHealth() - (maxHealth / 10)));
			e.setCancelled(true);
		}
	}
	
	@SubscribeEvent
	public void onPreTargetSkill(AbilityPreTargetEvent e) {
		if (skill.isRunning() && dreamSkills.contains(DreamSkill.ABILITY_CONTROL) && !e.getParticipant().equals(getParticipant()) && LocationUtil.isInCircle(center, e.getParticipant().getPlayer().getLocation(), nowrange)) {
			e.getParticipant().getPlayer().sendMessage("§3[§b루시드 드림§3] §c능력을 사용할 수 없습니다!");
			double maxHealth = e.getParticipant().getPlayer().getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue();
			Healths.setHealth(e.getParticipant().getPlayer(), Math.max(1, e.getParticipant().getPlayer().getHealth() - (maxHealth / 10)));
			e.setCancelled(true);
		}
	}
	
	@SubscribeEvent
	public void onBlockPlace(BlockPlaceEvent e) {
		if (skill.isRunning() && dreamSkills.contains(DreamSkill.RIPTIDE) && !e.getPlayer().equals(getPlayer()) && LocationUtil.isInCircle(center, e.getPlayer().getLocation(), nowrange)) {
			e.setCancelled(true);
			e.getPlayer().sendMessage("§3[§b루시드 드림§3] §c이안류가 블록 설치를 방해하고 있습니다!");
		}
	}
	
	@SubscribeEvent
	public void onPlayerItemConsume(PlayerItemConsumeEvent e) {
		if (skill.isRunning() && dreamSkills.contains(DreamSkill.RIPTIDE) && !e.getPlayer().equals(getPlayer()) && LocationUtil.isInCircle(center, e.getPlayer().getLocation(), nowrange)) {
			e.setCancelled(true);
			e.getPlayer().sendMessage("§3[§b루시드 드림§3] §c이안류가 소모품 사용을 방해하고 있습니다!");
		}
		
		if (e.getPlayer().equals(getPlayer()) && !skill.isRunning() && !e.isCancelled() && realSkills.contains(RealSkill.CONSUME_REGAIN) && random.nextBoolean()) {
			ItemStack consume = e.getItem().clone();
			consume.setAmount(1);
			getPlayer().getInventory().addItem(consume);
		}
	}
	
	@SubscribeEvent
	public void onEntityRegainHealth(EntityRegainHealthEvent e) {
		if (skill.isRunning() && (dreamSkills.contains(DreamSkill.ABYSS1) || dreamSkills.contains(DreamSkill.ABYSS2)) && !e.getEntity().equals(getPlayer()) && LocationUtil.isInCircle(center, e.getEntity().getLocation(), nowrange)) {
			double decrease = 0;
			if (e.getRegainReason().equals(RegainReason.SATIATED) || e.getRegainReason().equals(RegainReason.REGEN)) decrease = 1.0;
			else decrease = 0.5;
			if (dreamSkills.contains(DreamSkill.ABYSS2)) {
				Healing.heal(getPlayer(), e.getAmount() * decrease, e.getRegainReason());
			}
			e.setAmount(Math.max(0, e.getAmount() - (e.getAmount() * decrease)));
		}
	}
	
	@SubscribeEvent
	public void onPlayerMove(PlayerMoveEvent e) {
		if (skill.isRunning() && (predicate.test(e.getPlayer()) || getPlayer().equals(e.getPlayer()))) {
			if (LocationUtil.isInCircle(center, e.getFrom(), nowrange) && !LocationUtil.isInCircle(center, e.getTo(), nowrange)) {
				if (dreamSkills.contains(DreamSkill.OTHER_SIDE) && e.getPlayer().equals(getPlayer())) {
					Vector beside = center.toVector().clone().subtract(e.getFrom().toVector().clone());
					Location goTo = center.clone().add(beside);
					goTo.setY(e.getFrom().getY());
					e.setTo(goTo.setDirection(getPlayer().getLocation().getDirection()));
					if (NMS.getAbsorptionHearts(getPlayer()) <= 0) NMS.setAbsorptionHearts(getPlayer(), 2);
				} else if (dreamSkills.contains(DreamSkill.FREEDOM) && e.getPlayer().equals(getPlayer())) center = getPlayer().getLocation();
				else e.setTo(e.getFrom());
			}
		}
	}
	
	@SuppressWarnings("deprecation")
	@SubscribeEvent(onlyRelevant = true)
	private void onPlayerInteract(final PlayerInteractEvent e) {
		if (skill.isRunning() && dreamSkills.contains(DreamSkill.SWORDAURA)) {
			if (e.getItem() != null && swords.contains(e.getItem().getType()) && !attackcool.isRunning()) {
				if (e.getAction() == Action.LEFT_CLICK_AIR || e.getAction() == Action.LEFT_CLICK_BLOCK) {
					final ItemStack mainHand = getPlayer().getInventory().getItemInMainHand();
					if (swords.contains(mainHand.getType())) {
						new Bullet(getPlayer(), getPlayer().getLocation().clone().add(0, 1.5, 0), getPlayer().getLocation().getDirection().multiply(.4), mainHand.getEnchantmentLevel(Enchantment.DAMAGE_ALL), getPlayer().getAttribute(Attribute.GENERIC_ATTACK_DAMAGE).getValue()).start();
						attackcool.start();
					}
				}
			}	
		}
		
		if ((e.getAction() == Action.RIGHT_CLICK_BLOCK || e.getAction() == Action.RIGHT_CLICK_AIR)) {
			if (!skill.isRunning() && !e.isCancelled() && realSkills.contains(RealSkill.CONSUME_REGAIN) && random.nextBoolean()) {
				boolean potion = false;
				if (getPlayer().getInventory().getItemInMainHand().getType().equals(Material.SPLASH_POTION)) potion = true;
				else if (getPlayer().getInventory().getItemInMainHand().getType().equals(Material.ENDER_PEARL)) potion = false;
				else if (getPlayer().getInventory().getItemInOffHand().getType().equals(Material.SPLASH_POTION)) potion = true;
				else if (getPlayer().getInventory().getItemInOffHand().getType().equals(Material.ENDER_PEARL)) potion = false;
				
				if (potion && (getPlayer().getInventory().getItemInOffHand().getType().equals(Material.SPLASH_POTION) || getPlayer().getInventory().getItemInMainHand().getType().equals(Material.SPLASH_POTION))) {
					ItemStack splashPotion = null;
					if (getPlayer().getInventory().getItemInMainHand().getType().equals(Material.SPLASH_POTION)) splashPotion = getPlayer().getInventory().getItemInMainHand();
					else getPlayer().getInventory().getItemInOffHand();
					splashPotion.setAmount(1);
					getPlayer().getInventory().addItem(splashPotion);	
				}
				
				if (!potion && (getPlayer().getInventory().getItemInOffHand().getType().equals(Material.ENDER_PEARL) || getPlayer().getInventory().getItemInMainHand().getType().equals(Material.ENDER_PEARL))) {
					ItemStack enderPearl = null;
					if (getPlayer().getInventory().getItemInMainHand().getType().equals(Material.ENDER_PEARL)) enderPearl = getPlayer().getInventory().getItemInMainHand();
					else getPlayer().getInventory().getItemInOffHand();
					enderPearl.setAmount(1);
					getPlayer().getInventory().addItem(enderPearl);	
				}
			}
		}
	}
	
	@SubscribeEvent
	private void onEntityResurrect(EntityResurrectEvent e) {
		if (e.getEntity().equals(getPlayer()) && (getPlayer().getInventory().getItemInOffHand().getType().equals(MaterialX.TOTEM_OF_UNDYING.getMaterial()) || getPlayer().getInventory().getItemInMainHand().getType().equals(MaterialX.TOTEM_OF_UNDYING.getMaterial())) && !skill.isRunning() && !e.isCancelled() && realSkills.contains(RealSkill.CONSUME_REGAIN) && random.nextBoolean()) {
			ItemStack totem = null;
			if (getPlayer().getInventory().getItemInMainHand().getType().equals(MaterialX.TOTEM_OF_UNDYING.getMaterial())) totem = getPlayer().getInventory().getItemInMainHand();
			else getPlayer().getInventory().getItemInOffHand();
			totem.setAmount(1);
			getPlayer().getInventory().addItem(totem);
		}
	}
	
	@SubscribeEvent
	private void onPlayerJoin(final PlayerJoinEvent e) {
		if (getPlayer().getUniqueId().equals(e.getPlayer().getUniqueId())) {
			if (bossbar != null) bossbar.addPlayer(e.getPlayer());
		}
	}

	@SubscribeEvent
	private void onPlayerQuit(final PlayerQuitEvent e) {
		if (getPlayer().getUniqueId().equals(e.getPlayer().getUniqueId())) {
			if (bossbar != null) bossbar.removePlayer(e.getPlayer());
		}
	}
	
	private final AbilityTimer expbar = new AbilityTimer() {
		
    	@Override
    	public void onStart() {
    		bossbar = Bukkit.createBossBar("§3Lv §b" + (dreamlevel >= 10 ? "§cMAX" + getOver(dreamlevel) : df.format(dreamlevel)) + " §7/ §aEXP§7: §e" + (df.format(dreamexp)), BarColor.GREEN, BarStyle.SOLID);
    		bossbar.setProgress(dreamlevel >= 10 ? 1 : Math.min(1, dreamexp / (100 + (dreamlevel * 20))));
    		bossbar.addPlayer(getPlayer());
    		if (ServerVersion.getVersion() >= 10) bossbar.setVisible(true);
    	}
    	
    	@Override
		public void run(int count) {
    		if ((dreamlevel >= 20 && dreamSkills.contains(DreamSkill.LIMIT_BREAKER)) || (dreamlevel >= 10 && !dreamSkills.contains(DreamSkill.LIMIT_BREAKER))) bossbar.setProgress(1);
    		else bossbar.setProgress(Math.min(1, dreamexp / (100 + (dreamlevel * 20))));
			bossbar.setTitle("§3Lv §b" + (dreamlevel >= 10 ? "§cMAX" + getOver(dreamlevel) : df.format(dreamlevel)) + " §7/ §aEXP§7: §e" + (df.format(dreamexp)));
    		if (dreamexp >= (100 + (dreamlevel * 20)) && dreamlevel < (dreamSkills.contains(DreamSkill.LIMIT_BREAKER) ? 20 : 10)) levelup();
    	}
    	
		@Override
		public void onEnd() {
			onSilentEnd();
		}

		@Override
		public void onSilentEnd() {
			bossbar.removeAll();
		}
		
	}.setPeriod(TimeUnit.TICKS, 1).register();
	
	public void levelup() {
		if (cooldown.isRunning()) cooldown.setCount(0);
		if (skill.isDuration() && dreamSkills.contains(DreamSkill.INCEPTION)) {
			skill.setCount(skill.getCount() + 400);
		}
		if (dreamSkills.contains(DreamSkill.STEP_UP)) damagestack++;
		final Firework firework = getPlayer().getWorld().spawn(getPlayer().getLocation(), Firework.class);
		final FireworkMeta meta = firework.getFireworkMeta();
		Color color1 = null, color2 = null, color3 = null, color4 = null;
		switch(random.nextInt(5)) {
		case 0:
			color1 = mint;
			color2 = snow;
			color3 = teal;
			color4 = sky;
			break;
		case 1:
			color1 = pink;
			color2 = yellow;
			color3 = lime;
			color4 = applemint;
			break;
		case 2:
			color1 = applemint;
			color2 = snow;
			color3 = lime;
			color4 = yellow;
			break;
		case 3:
			color1 = orange;
			color2 = yellow;
			color3 = pink;
			color4 = snow;
			break;
		case 4:
			color1 = Color.WHITE;
			color2 = Color.SILVER;
			color3 = Color.GRAY;
			color4 = Color.BLACK;
			break;
		}
		meta.addEffect(
				FireworkEffect.builder()
				.withColor(color1, color2, color3, color4)
				.withTrail()
				.withFlicker()
				.with(Type.STAR)
				.build()
		);	
		meta.setPower(0);
		firework.setFireworkMeta(meta);
		firework.setMetadata("Firework", NULL_VALUE);
		new BukkitRunnable() {
			@Override
			public void run() {
				firework.detonate();
			}
		}.runTaskLater(AbilityWar.getPlugin(), 1L);
		SoundLib.ENTITY_PLAYER_LEVELUP.playSound(getPlayer(), 1, 1);
		dreamlevel = Math.min((dreamSkills.contains(DreamSkill.LIMIT_BREAKER) ? 20 : 10), dreamlevel + 1);
		new SkillGUI(dreamlevel).start();
		dreamexp = 0;
	}
	
	private final AbilityTimer speedup = new AbilityTimer(30) {
		
		private final AttributeModifier modifier1 = new AttributeModifier(UUID.randomUUID(), "addspeed", 0.3, Operation.ADD_SCALAR);
		private final AttributeModifier modifier2 = new AttributeModifier(UUID.randomUUID(), "addspeed", 0.6, Operation.ADD_SCALAR);
		private boolean upgrade = false;
		
		@Override
		public void onStart() {
			if (dreamSkills.contains(DreamSkill.SURFING2)) {
				try {
					getPlayer().getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).addModifier(modifier2);
					upgrade = true;
				} catch (IllegalArgumentException ignored) {
				}
			} else {
				try {
					getPlayer().getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).addModifier(modifier1);
				} catch (IllegalArgumentException ignored) {
				}
			}
		}
		
		@Override
		public void onEnd() {
			onSilentEnd();
		}
		
		@Override
		public void onSilentEnd() {
			if (upgrade) getPlayer().getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).removeModifier(modifier2);
			else getPlayer().getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).removeModifier(modifier1);
		}
		
	}.setPeriod(TimeUnit.TICKS, 1).register();
	
	private final AbilityTimer biggerEffect = new AbilityTimer() {
		
		@Override
		protected void onStart() {
			if (dreamSkills.contains(DreamSkill.AREA_REDUCE2)) nowrange = 5;
			else if (dreamSkills.contains(DreamSkill.AREA_REDUCE1)) nowrange = 10;
			else nowrange = 15;
			
			center = LocationUtil.floorY(getPlayer().getLocation().clone());
			currentRadius = 0;
			final Firework firework = getPlayer().getWorld().spawn(center, Firework.class);
			final FireworkMeta meta = firework.getFireworkMeta();
			meta.addEffect(
					FireworkEffect.builder()
					.withColor(aqua1.getColor(), aqua11.getColor(), Color.AQUA)
					.with(Type.BALL_LARGE)
					.build()
			);
			meta.setPower(0);
			firework.setFireworkMeta(meta);
			firework.setMetadata("Firework", NULL_VALUE);
			new BukkitRunnable() {
				@Override
				public void run() {
					firework.detonate();
				}
			}.runTaskLater(AbilityWar.getPlugin(), 1L);
			SoundLib.ENTITY_PLAYER_SPLASH.playSound(center, 2, 0.5f);
		}
		
		@Override
		protected void run(int count) {
			if (count % 3 == 0) {
				if (turns)
					stacks++;
				else
					stacks--;
				if (stacks % (aquas.size() - 1) == 0) {
					turns = !turns;
				}
				aqua = aquas.get(stacks);	
			}
			
			double playerY = getPlayer().getLocation().getY();
			if (currentRadius < nowrange) currentRadius += 0.5;
			for (Iterator<Location> iterator = Circle.iteratorOf(center, currentRadius, 100); iterator.hasNext(); ) {
				Location loc = iterator.next();
				loc.setY(LocationUtil.getFloorYAt(loc.getWorld(), playerY, loc.getBlockX(), loc.getBlockZ()) + 0.1);
				if (dreamSkills.contains(DreamSkill.NOTHINGNESS)) ParticleLib.REDSTONE.spawnParticle(getPlayer(), loc, aqua);
				else ParticleLib.REDSTONE.spawnParticle(loc, aqua);
			}
			if (currentRadius == nowrange) this.stop(false);
			
			
		}
		
		@Override
		protected void onEnd() {
			skill.start();
		}
		
	}.setPeriod(TimeUnit.TICKS, 1).register();
	
	private final Duration skill = new Duration(DURATION.getValue() * 20, cooldown) {
		
		private boolean up = true;
		private double addY = 0.1;
		private AttributeModifier modifier1 = null;
		private long worldtime = 0;
		private boolean timechanged = false;
		
		@Override
		protected void onDurationStart() {
			damageMap.clear();
			worldtime = getPlayer().getWorld().getTime();
			if (dreamSkills.contains(DreamSkill.INSTANT)) {
				if (dreamSkills.contains(DreamSkill.NOTHINGNESS)) SoundLib.ENTITY_PLAYER_SPLASH.playSound(getPlayer(), center, 2, 0.5f);
				else SoundLib.ENTITY_PLAYER_SPLASH.playSound(center, 2, 0.5f);
				ParticleLib.TOTEM.spawnParticle(center, 1, 1, 1, 25, 0);
			}
			if (center == null) center = LocationUtil.floorY(getPlayer().getLocation().clone());
			if (dreamSkills.contains(DreamSkill.AREA_REDUCE2)) nowrange = 5;
			else if (dreamSkills.contains(DreamSkill.AREA_REDUCE1)) nowrange = 10;
			else nowrange = 15;
			
			if (dreamSkills.contains(DreamSkill.FASTWALK2)) modifier1 = new AttributeModifier(UUID.randomUUID(), "addspeed3", 0.5, Operation.ADD_SCALAR);
			else if (dreamSkills.contains(DreamSkill.FASTWALK1)) modifier1 = new AttributeModifier(UUID.randomUUID(), "addspeed2", 0.25, Operation.ADD_SCALAR);
			
			if (modifier1 != null) getPlayer().getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).addModifier(modifier1);

			if (dreamSkills.contains(DreamSkill.WATER_ANSWER)) {
				for (Player player : getPlayer().getWorld().getPlayers()) {
					if (predicate.test(player)) {
						new DataCatch(player).start();
					}
				}
			}
			
			if (dreamSkills.contains(DreamSkill.FOAM1) || dreamSkills.contains(DreamSkill.FOAM2) || dreamSkills.contains(DreamSkill.FOAM3)) {
				double maxHealth = getPlayer().getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue();
				double multiply = 0;
				if (dreamSkills.contains(DreamSkill.FOAM3)) multiply = 0.4;
				else if (dreamSkills.contains(DreamSkill.FOAM2)) multiply = 0.3;
				else if (dreamSkills.contains(DreamSkill.FOAM1)) multiply = 0.2;
				Healing.heal(getPlayer(), maxHealth * multiply, RegainReason.CUSTOM);
			}
		}

		@Override
		protected void onDurationProcess(int count) {
			if (dreamSkills.contains(DreamSkill.NOTHINGNESS)) ParticleLib.DRIP_WATER.spawnParticle(getPlayer(), center, nowrange, 5, nowrange, 10, 0);
			else ParticleLib.DRIP_WATER.spawnParticle(center, nowrange, 5, nowrange, 10, 0);
			
			if (dreamSkills.contains(DreamSkill.FLIGHT)) {
				getPlayer().setAllowFlight(true);
				if (getPlayer().isFlying()) {
					getParticipant().attributes().TARGETABLE.setValue(false);
					ParticleLib.CLOUD.spawnParticle(getPlayer().getLocation(), 0.5, 0.1, 0.5, 10, 0);
				} else getParticipant().attributes().TARGETABLE.setValue(true);
			}
			
			if (count % 2 == 0) {
				double playerY = getPlayer().getLocation().getY();
				Circle nowcircle = circle;
				if (dreamSkills.contains(DreamSkill.AREA_REDUCE2)) nowcircle = circle3;
				else if (dreamSkills.contains(DreamSkill.AREA_REDUCE1)) nowcircle = circle2;
				if (count % 4 == 0) {
					for (Location loc : nowcircle.toLocations(center).floor(playerY)) {
						if (turns)
							stacks++;
						else
							stacks--;
						if (stacks % (aquas.size() - 1) == 0) {
							turns = !turns;
						}
						aqua = aquas.get(stacks);	
						if (addY >= 1.5) up = false;
						else if (addY <= 0) up = true;		
						addY = addY + (up ? 0.05 : -0.05);
						Location location = loc.clone().add(0, addY + 0.3, 0);
						if (dreamSkills.contains(DreamSkill.NOTHINGNESS)) ParticleLib.REDSTONE.spawnParticle(getPlayer(), location, aqua);
						else ParticleLib.REDSTONE.spawnParticle(location, aqua);
					}	
				}
				
				if (dreamSkills.contains(DreamSkill.CALL_OF_SEA)) {
					for (LivingEntity livingEntity : LocationUtil.getNearbyEntities(LivingEntity.class, center, 40, 40, predicate)) {
						livingEntity.setVelocity(VectorUtil.validateVector(center.toVector().subtract(livingEntity.getLocation().toVector()).normalize().setY(0).multiply(0.0375)));
					}
				}
			}
			
			if (count % 10 == 0) {
				if (dreamSkills.contains(DreamSkill.NOTHINGNESS)) {
					SoundLib.BLOCK_WATER_AMBIENT.playSound(getPlayer(), 1, 1.2f);
				} else {
					for (Player p : LocationUtil.getEntitiesInCircle(Player.class, center, nowrange, soundpredicate)) {
						SoundLib.BLOCK_WATER_AMBIENT.playSound(p, 1, 1.2f);	
					}	
				}
			}
			
			int nowperiod = period;
			double decrease = 0;
			if (isNight(getPlayer().getWorld().getTime())) {
				if (dreamSkills.contains(DreamSkill.LUNAR_GRAVITY1) || dreamSkills.contains(DreamSkill.LUNAR_GRAVITY2)) decrease += 0.5;	
			} else if (dreamSkills.contains(DreamSkill.LUNAR_GRAVITY2)) {
				getPlayer().getWorld().setTime(17500);
				timechanged = true;
				if (count % 3 == 0) this.setCount(count + 1);
			}
			
			if (dreamSkills.contains(DreamSkill.REVERSE)) decrease += 0.2;
			
			if (decrease >= 1) decrease = 0.9;
			
			nowperiod = (int) (nowperiod - (nowperiod * decrease));
			
			if (count % nowperiod == 0) {
				new Wave(bestDamage).start();
				if (dreamSkills.contains(DreamSkill.NOTHINGNESS)) SoundLib.PLING.playInstrument(getPlayer(), center, 1.5f, Note.sharp(0, Tone.F));
				else SoundLib.PLING.playInstrument(center, 1.5f, Note.sharp(0, Tone.F));
			}
		}
		
		@Override
		protected void onDurationEnd() {
			onDurationSilentEnd();
		}
		
		@Override
		protected void onDurationSilentEnd() {
			damageMap.clear();
			if (!dreamSkills.contains(DreamSkill.EQUALITY)) bestDamage = 7;
			isSkillRunning = false;
			if (dreamSkills.contains(DreamSkill.FLIGHT)) {
				if (!getPlayer().getGameMode().equals(GameMode.CREATIVE) && !getPlayer().getGameMode().equals(GameMode.SPECTATOR)) {
					getPlayer().setAllowFlight(false);
				}
				getParticipant().attributes().TARGETABLE.setValue(true);
			}
			
			if (dreamSkills.contains(DreamSkill.FOAM1) || dreamSkills.contains(DreamSkill.FOAM2) || dreamSkills.contains(DreamSkill.FOAM3)) {
				Healing.heal(getPlayer(), lostHealth, RegainReason.CUSTOM);
				lostHealth = 0;
			}
			
			if (timechanged) getPlayer().getWorld().setTime(worldtime);
			
			if (modifier1 != null) getPlayer().getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).removeModifier(modifier1);
		}
	
	}.setPeriod(TimeUnit.TICKS, 1);
	
	public class Wave extends AbilityTimer {
		
		private final double damage;
		private double waveRadius;
		private final Predicate<Entity> predicate;
		private Set<Damageable> hitEntity = new HashSet<>();
		private ArmorStand damager;
		private boolean up = true;
		private double addY = 0.1;
		private final RGB startColor = RGB.of(230, 1, 1), endColor = RGB.of(254, 164, 164);
		private final List<RGB> gradations = Gradient.createGradient(12, startColor, endColor);
		private double boundingbox = 0;
		
		private RGB waveColor1 = RGB.of(1, 183, 194), waveColor2 = RGB.of(18, 186, 197), waveColor3 = RGB.of(36, 189, 200),
				waveColor4 = RGB.of(55, 192, 204), waveColor5 = RGB.of(73, 195, 207), waveColor6 = RGB.of(92, 198, 211),
				waveColor7 = RGB.of(110, 200, 214), waveColor8 = RGB.of(128, 202, 217), waveColor9 = RGB.of(147, 205, 221),
				waveColor10 = RGB.of(177, 213, 228), waveColor11 = RGB.of(202, 222, 236), waveColor12 = RGB.AQUA;
		
		@SuppressWarnings("serial")
		private List<RGB> waveColors = new ArrayList<RGB>() {
			{
				add(waveColor1);
				add(waveColor2);
				add(waveColor3);
				add(waveColor4);
				add(waveColor5);
				add(waveColor6);
				add(waveColor7);
				add(waveColor8);
				add(waveColor9);
				add(waveColor10);
				add(waveColor11);
				add(waveColor12);
			}
		};
		
		private RGB color = waveColor1;
		
		public Wave(double damage) {
			setPeriod(TimeUnit.TICKS, 1);
			this.damage = damage;
			this.predicate = new Predicate<Entity>() {
				@Override
				public boolean test(Entity entity) {
					if (entity instanceof ArmorStand) return false;
					if (hitEntity.contains(entity)) return false;
					if (entity instanceof Player) {
						if (!getGame().isParticipating(entity.getUniqueId())
								|| (getGame() instanceof DeathManager.Handler && ((DeathManager.Handler) getGame()).getDeathManager().isExcluded(entity.getUniqueId()))
								|| !getGame().getParticipant(entity.getUniqueId()).attributes().TARGETABLE.getValue()) {
							return false;
						}
						if (getGame() instanceof Teamable) {
							final Teamable teamGame = (Teamable) getGame();
							final Participant entityParticipant = teamGame.getParticipant(entity.getUniqueId()), participant = teamGame.getParticipant(getPlayer().getUniqueId());
							if (participant != null) {
								return !teamGame.hasTeam(entityParticipant) || !teamGame.hasTeam(participant) || (!teamGame.getTeam(entityParticipant).equals(teamGame.getTeam(participant)));
							}
						}
					}
					return true;
				}

				@Override
				public boolean apply(@Nullable Entity arg0) {
					return false;
				}
			};
		}
		
		@Override
		public void onStart() {			
			if (dreamSkills.contains(DreamSkill.REVERSE)) waveRadius = nowrange;
			else waveRadius = 0;
			damager = center.getWorld().spawn(center.clone().add(99999, 0, 99999), ArmorStand.class);
			damager.setVisible(false);
			damager.setInvulnerable(true);
			damager.setGravity(false);
			damager.setMetadata("Wave", NULL_VALUE);
			NMS.removeBoundingBox(damager);
			boundingbox = dreamSkills.contains(DreamSkill.TSUNAMI) ? 1.8 : 0.6;
		}
		
		@Override
		public void run(int count) {
			if (!skill.isRunning()) this.stop(false);
			if (dreamSkills.contains(DreamSkill.REVERSE)) {if (waveRadius > 0) waveRadius = Math.max(0.01, waveRadius - 0.35);}
			else if (waveRadius < nowrange + (dreamSkills.contains(DreamSkill.TSUNAMI) ? 7.5 : 0)) waveRadius += 0.35;
			
			if ((dreamSkills.contains(DreamSkill.REVERSE) && waveRadius <= 0.01) || waveRadius >= nowrange + (dreamSkills.contains(DreamSkill.TSUNAMI) ? 7.5 : 0)) this.stop(false);
			
			if (dreamSkills.contains(DreamSkill.SCALD)) {
				if (count == 1) color = gradations.get(0);
				if (count % 4 == 0) color = gradations.get(Math.min(12, count / 4));
			} else {
				if (count == 1) color = waveColor1;
				if (count % 4 == 0) color = waveColors.get(Math.min(12, count / 4));	
			}
			if (count % 5 == 0) up = !up;
			
			addY = addY + (up ? 0.1 : -0.1);
			
			double playerY = getPlayer().getLocation().getY();
			for (Iterator<Location> iterator = Circle.iteratorOf(center, waveRadius, 75); iterator.hasNext(); ) {
				Location loc = iterator.next();
				loc.setY(LocationUtil.getFloorYAt(loc.getWorld(), playerY, loc.getBlockX(), loc.getBlockZ()) + addY + 0.2);
				if (dreamSkills.contains(DreamSkill.NOTHINGNESS)) {
					if (count % 2 == 0) ParticleLib.REDSTONE.spawnParticle(getPlayer(), loc, color);
					if (addY <= 0.1 || count < 5) ParticleLib.WATER_SPLASH.spawnParticle(getPlayer(), loc, 0, 0, 0, 1, 0);
				} else {
					if (count % 2 == 0) ParticleLib.REDSTONE.spawnParticle(loc, color);
					if (addY <= 0.1 || count < 5) ParticleLib.WATER_SPLASH.spawnParticle(loc, 0, 0, 0, 1, 0);
				}
				for (Damageable damageable : LocationUtil.getNearbyEntities(Damageable.class, loc, boundingbox, (dreamSkills.contains(DreamSkill.TSUNAMI) ? boundingbox * 20 : boundingbox), predicate)) {
					double minIncrease = 0.5;

					if (dreamSkills.contains(DreamSkill.AREA_REDUCE2)) minIncrease = 1.5;
					else if (dreamSkills.contains(DreamSkill.AREA_REDUCE1)) minIncrease = 1.0;

					double maxIncrease = 1.5;
					double progress = waveRadius / nowrange;

					double increase;
					if (dreamSkills.contains(DreamSkill.REVERSE)) increase = maxIncrease - (maxIncrease - minIncrease) * progress;
					else increase = minIncrease + (maxIncrease - minIncrease) * progress;

					if (dreamSkills.contains(DreamSkill.EQUALITY)) increase = 1.5;
					
					double addIncrease = 1;
					if (dreamSkills.contains(DreamSkill.RAGING_WAVE3)) addIncrease = 1.7;
					else if (dreamSkills.contains(DreamSkill.RAGING_WAVE2)) addIncrease = 1.5;
					else if (dreamSkills.contains(DreamSkill.RAGING_WAVE1)) addIncrease = 1.3;
					
					if (!damageable.equals(getPlayer())) {
						double nowDamage = increase * damage * addIncrease;
						if (dreamSkills.contains(DreamSkill.FLOOD)) {
							nowDamage *= (1 + (0.2 * damageMap.getOrDefault(damageable, 0)));
							damageMap.put(damageable, damageMap.getOrDefault(damageable, 0) + 1);
						}
						if (dreamSkills.contains(DreamSkill.SINK) && damageable instanceof Player) {
							Player player = (Player) damageable;
							double maxHealth = player.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue();
							if (damageable.getHealth() <= (maxHealth * 0.4)) {
								nowDamage *= 1.5;
								BindingSmoke.apply(getGame().getParticipant(player), TimeUnit.TICKS, 30);
							}
						}
						damageable.damage(nowDamage, damager);
						damageable.setVelocity(VectorUtil.validateVector(damageable.getLocation().toVector().subtract(center.toVector()).normalize().multiply(1.1 * addIncrease).setY(0.5)));
						if (damageable instanceof LivingEntity) {
							PotionEffects.SLOW.addPotionEffect((LivingEntity) damageable, 100, 1, false);
							if (dreamSkills.contains(DreamSkill.MARINE_LAYER)) PotionEffects.BLINDNESS.addPotionEffect((LivingEntity) damageable, 30, 0, false);
						}
						if (damageable instanceof Player) {
							Player player = (Player) damageable;
							if (dreamSkills.contains(DreamSkill.OVERHYDRATION1)) {
								Corrosion.apply(getGame().getParticipant(player), TimeUnit.TICKS, dreamSkills.contains(DreamSkill.OVERHYDRATION2) ? 70 : 40);
								Moisture.apply(getGame().getParticipant(player), TimeUnit.TICKS, dreamSkills.contains(DreamSkill.OVERHYDRATION2) ? 70 : 40);
							}
							if (dreamSkills.contains(DreamSkill.SLEEP_WAVE) && random.nextInt(10) < 4) Dream.apply(getGame().getParticipant(player), TimeUnit.TICKS, 100);
							if (dreamSkills.contains(DreamSkill.SCALD)) Burn.apply(getGame().getParticipant(player), TimeUnit.TICKS, 100);
							if (dreamSkills.contains(DreamSkill.MARINE_LAYER)) Oppress.apply(getGame().getParticipant(player), TimeUnit.TICKS, 6);
						}
						if (dreamSkills.contains(DreamSkill.SCALD)) damageable.setFireTicks(damageable.getFireTicks() + (damageable.getFireTicks() <= 0 ? 120 : 100));
						
					} else if (dreamSkills.contains(DreamSkill.SURFING1) || dreamSkills.contains(DreamSkill.SURFING2)) {
						speedup.start();
						if (dreamSkills.contains(DreamSkill.SURFING2)) Healing.heal(getPlayer(), getPlayer().getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue() * 0.05, RegainReason.CUSTOM);
					}
					
					hitEntity.add(damageable);
				}
			}
		}
		
		@Override
		public void onEnd() {
			onSilentEnd();
		}
		
		@Override
		public void onSilentEnd() {
			if (dreamSkills.contains(DreamSkill.NOTHINGNESS)) SoundLib.ENTITY_PLAYER_SWIM.playSound(getPlayer(), center, 2f, 1.4f);
			else SoundLib.ENTITY_PLAYER_SWIM.playSound(center, 2f, 1.4f);
			damager.remove();
		}
		
	}
	
	private void showSpiral(final RGB color) {
		new AbilityTimer(40) {
			private double radius = 10;
			private double count = 120;
			private double fourPi = Math.PI * 4;
			private double theta = 0;
			private double addSpiralY = 1;
			private boolean yUp = false;

			@Override
			protected void onStart() {
				spiral = true;
			}
			
			@Override
			protected void run(int a) {
				Location me = getPlayer().getLocation();			
				for (int i = 0; i < 6; i++) {
					if (addSpiralY >= 1.6) yUp = false;
					else if (addSpiralY <= 0.4) yUp = true;
					addSpiralY = yUp ? Math.min(1.6, addSpiralY + 0.02) : Math.max(0.4, addSpiralY - 0.02);
					radius -= (0.5 / 12);
					theta += (fourPi / count);
					Location loc = me.clone().add(Math.cos(theta) * radius, addSpiralY, Math.sin(theta) * radius);
					if (dreamSkills.contains(DreamSkill.NOTHINGNESS)) {
						ParticleLib.REDSTONE.spawnParticle(getPlayer(), loc, color);
						ParticleLib.DRIP_WATER.spawnParticle(getPlayer(), loc, 0, 0, 0, 1, 0);
						SoundLib.ENTITY_BOAT_PADDLE_WATER.playSound(getPlayer(), loc);
					} else {
						ParticleLib.REDSTONE.spawnParticle(loc, color);
						ParticleLib.DRIP_WATER.spawnParticle(loc, 0, 0, 0, 1, 0);
						SoundLib.ENTITY_BOAT_PADDLE_WATER.playSound(loc);	
					}
				}
			}
			
			@Override
			protected void onEnd() {
				spiral = false;
				biggerEffect.start();
			}
			
			@Override
			protected void onSilentEnd() {
				spiral = false;
			}
			
		}.setPeriod(TimeUnit.TICKS, 1).start();
	}
	
	public class DataCatch extends AbilityTimer {
		
		private final Player player;
		private final IHologram hologramHealth, hologramAbility;
		private int health = 0;
		private int maxHealth = 0;
		
		private DataCatch(Player player) {
			setPeriod(TimeUnit.TICKS, 1);
			this.player = player;
			String abilityName = "§7-";
			if (getGame().isParticipating(player)) {
				Participant participant = getGame().getParticipant(player);
				if (participant.getAbility() != null) {
					abilityName = RankColor.getColor(participant.getAbility().getRank()) + participant.getAbility().getDisplayName();
					if (participant.getAbility() instanceof Mix) {
						Mix mix = (Mix) participant.getAbility();
						if (mix.hasSynergy()) {
							abilityName = RankColor.getColor(mix.getSynergy().getRank()) + mix.getSynergy().getDisplayName();
						} else if (mix.getFirst() != null && mix.getSecond() != null) {
							abilityName = RankColor.getColor(mix.getFirst().getRank()) + mix.getFirst().getDisplayName() + " §7+ " + RankColor.getColor(mix.getSecond().getRank()) + mix.getSecond().getDisplayName();
						}
					}
				}
			}
			this.hologramHealth = NMS.newHologram(player.getWorld(), player.getLocation().getX(),
					player.getLocation().getY() + player.getEyeHeight() + 0.6, player.getLocation().getZ(), 
					Strings.repeat("§a|", health).concat(Strings.repeat("§7|", maxHealth - health)));
			hologramHealth.display(getPlayer());
			this.hologramAbility = NMS.newHologram(player.getWorld(), player.getLocation().getX(),
					player.getLocation().getY() + player.getEyeHeight() + 1.0, player.getLocation().getZ(), 
					abilityName);
			hologramAbility.display(getPlayer());
		}
		
		@Override
		protected void run(int count) {
			String abilityName = "§7-";
			if (getGame().isParticipating(player)) {
				Participant participant = getGame().getParticipant(player);
				if (participant.getAbility() != null) {
					abilityName = RankColor.getColor(participant.getAbility().getRank()) + participant.getAbility().getDisplayName();
					if (participant.getAbility() instanceof Mix) {
						Mix mix = (Mix) participant.getAbility();
						if (mix.hasSynergy()) {
							abilityName = RankColor.getColor(mix.getSynergy().getRank()) + mix.getSynergy().getDisplayName();
						} else if (mix.getFirst() != null && mix.getSecond() != null) {
							abilityName = RankColor.getColor(mix.getFirst().getRank()) + mix.getFirst().getDisplayName() + " §7+ " + RankColor.getColor(mix.getSecond().getRank()) + mix.getSecond().getDisplayName();
						}
					}
				}
			}
			health = (int) player.getHealth();
			maxHealth = (int) player.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue();
			hologramHealth.teleport(player.getWorld(), player.getLocation().getX(), 
					player.getLocation().getY() + player.getEyeHeight() + 0.6, player.getLocation().getZ(), 
					player.getLocation().getYaw(), 0);
			hologramHealth.setText(Strings.repeat("§c|", health).concat(Strings.repeat("§7|", maxHealth - health)));
			hologramAbility.teleport(player.getWorld(), player.getLocation().getX(), 
					player.getLocation().getY() + player.getEyeHeight() + 1.0, player.getLocation().getZ(), 
					player.getLocation().getYaw(), 0);
			hologramAbility.setText(abilityName);
			if (!skill.isRunning()) this.stop(false);
			if (health <= 0) this.stop(false);
		}
		
		@Override
		protected void onEnd() {
			onSilentEnd();
		}
		
		@Override
		protected void onSilentEnd() {
			hologramHealth.unregister();
		}
		
	}
	
	public class Bullet extends AbilityTimer {

		private final LivingEntity shooter;
		private final CustomEntity entity;
		private final Vector forward;
		private final int sharpnessEnchant;
		private final double damage;
		private final Predicate<Entity> predicate;
		private Set<LivingEntity> hitEntity = new HashSet<>();

		private Location lastLocation;

		private Bullet(LivingEntity shooter, Location startLocation, Vector arrowVelocity, int sharpnessEnchant, double damage) {
			super(4);
			setPeriod(TimeUnit.TICKS, 1);
			this.shooter = shooter;
			this.entity = new Bullet.ArrowEntity(startLocation.getWorld(), startLocation.getX(), startLocation.getY(), startLocation.getZ()).resizeBoundingBox(-1.2, -1.2, -1.2, 1.2, 1.2, 1.2);
			this.forward = arrowVelocity.multiply(10);
			this.sharpnessEnchant = sharpnessEnchant;
			this.damage = damage;
			this.lastLocation = startLocation;
			this.predicate = new Predicate<Entity>() {
				@Override
				public boolean test(Entity entity) {
					if (entity instanceof ArmorStand) return false;
					if (entity.equals(shooter)) return false;
					if (hitEntity.contains(entity)) return false;
					if (entity instanceof Player) {
						if (!getGame().isParticipating(entity.getUniqueId())
								|| (getGame() instanceof DeathManager.Handler && ((DeathManager.Handler) getGame()).getDeathManager().isExcluded(entity.getUniqueId()))
								|| !getGame().getParticipant(entity.getUniqueId()).attributes().TARGETABLE.getValue()) {
							return false;
						}
						if (getGame() instanceof Teamable) {
							final Teamable teamGame = (Teamable) getGame();
							final Participant entityParticipant = teamGame.getParticipant(entity.getUniqueId()), participant = teamGame.getParticipant(shooter.getUniqueId());
							if (participant != null) {
								return !teamGame.hasTeam(entityParticipant) || !teamGame.hasTeam(participant) || (!teamGame.getTeam(entityParticipant).equals(teamGame.getTeam(participant)));
							}
						}
					}
					return true;
				}

				@Override
				public boolean apply(@Nullable Entity arg0) {
					return false;
				}
			};
		}
		
		@Override
		protected void onStart() {
			SoundLib.ENTITY_PLAYER_ATTACK_SWEEP.playSound(shooter.getLocation(), 1, 0.75f);
		}

		@Override
		protected void run(int i) {
			final Location newLocation = lastLocation.clone().add(forward);
			for (Iterator<Location> iterator = new Iterator<Location>() {
				private final Vector vectorBetween = newLocation.toVector().subtract(lastLocation.toVector()), unit = vectorBetween.clone().normalize().multiply(.1);
				private final int amount = (int) (vectorBetween.length() / 0.1);
				private int cursor = 0;

				@Override
				public boolean hasNext() {
					return cursor < amount;
				}

				@Override
				public Location next() {
					if (cursor >= amount) throw new NoSuchElementException();
					cursor++;
					return lastLocation.clone().add(unit.clone().multiply(cursor));
				}
			}; iterator.hasNext(); ) {
				final Location location = iterator.next();
				entity.setLocation(location);
				if (!isRunning()) {
					return;
				}
				for (LivingEntity livingEntity : LocationUtil.getConflictingEntities(LivingEntity.class, entity.getWorld(), entity.getBoundingBox(), predicate)) {
					if (!shooter.equals(livingEntity)) {
						livingEntity.damage((float) (EnchantLib.getDamageWithSharpnessEnchantment(damage, sharpnessEnchant)) * 1.2, getPlayer());
						hitEntity.add(livingEntity);
					}
				}
				ParticleLib.CRIT.spawnParticle(location, 0, 0, 0, 1, 0);
			}
			lastLocation = newLocation;
		}

		@Override
		protected void onEnd() {
			entity.remove();
		}

		@Override
		protected void onSilentEnd() {
			entity.remove();
		}

		public class ArrowEntity extends CustomEntity {

			public ArrowEntity(World world, double x, double y, double z) {
				getGame().super(world, x, y, z);
			}

			@Override
			protected void onRemove() {
				Bullet.this.stop(false);
			}

		}

	}
	
	public class SkillGUI extends AbilityTimer implements Listener {
		
		private final ItemStack NULL = (new ItemBuilder(MaterialX.GRAY_STAINED_GLASS_PANE)).displayName(" ").build();
		private final List<DreamSkill> tempvalues = new ArrayList<>();
		private final List<DreamSkill> values = new ArrayList<>();
		private final LimitedPushingList<DreamSkill> list = new LimitedPushingList<>(dreamSkills.contains(DreamSkill.SIRENS_BLESS) ? 5 : 3);
		private final LimitedPushingList<RealSkill> lossList = new LimitedPushingList<>(dreamSkills.contains(DreamSkill.SIRENS_BLESS) ? 5 : 3);
		private Map<Integer, DreamSkill> slots = new HashMap<>();
		private Map<Integer, RealSkill> lossSlots = new HashMap<>();
		
		private DreamSkill selected;
		private RealSkill lossSelected;
		private final Inventory gui;
		
		public SkillGUI(int quality) {
			super(TaskType.REVERSE, 300);
			setPeriod(TimeUnit.TICKS, 1);
			gui = Bukkit.createInventory(null, InventoryType.HOPPER, "§0스킬을 선택해주세요.");
			createSkillList(quality);
		}
		
		private void createSkillList(int quality) {
			int minquality = 0, maxquality = 0;
			switch(quality) {
			case 1:
			case 2:
				minquality = 0;
				maxquality = 0;
				break;
			case 3:
			case 4:
				minquality = 0;
				maxquality = 1;
				break;
			case 5:
			case 6:
				minquality = 1;
				maxquality = 2;
				break;
			case 7:
			case 8:
				minquality = 1;
				maxquality = 3;
				break;
			case 9:
				minquality = 2;
				maxquality = 3;
				break;
			case 10:
				minquality = 3;
				maxquality = 3;
				break;
			case 11:
			case 12:
			case 13:
			case 14:
			case 15:
			case 16:
			case 17:
			case 18:
			case 19:
			case 20:
				minquality = 0;
				maxquality = 3;
				break;
			}
			
			if (dreamSkills.contains(DreamSkill.EQUALITY)) {
				minquality = 0;
				maxquality = 4;
			}
			
			if (dreamSkills.contains(DreamSkill.SIRENS_BLESS)) minquality = Math.min(3, minquality + 1);
			
			for (int a = minquality; a <= maxquality; a++) {
				for (DreamSkill skill : dreamSkillList.get(a)) {
					if (!dreamSkills.contains(skill)) tempvalues.add(skill);
				}	
			}
			
			for (DreamSkill skill : tempvalues) {
				if (skill.equals(DreamSkill.AREA_REDUCE2) && !dreamSkills.contains(DreamSkill.AREA_REDUCE1)) {}
				else if (skill.equals(DreamSkill.FASTWALK2) && !dreamSkills.contains(DreamSkill.FASTWALK1)) {}
				else if (skill.equals(DreamSkill.FOAM3) && !dreamSkills.contains(DreamSkill.FOAM2)) {}
				else if (skill.equals(DreamSkill.FOAM2) && !dreamSkills.contains(DreamSkill.FOAM1)) {}
				else if (skill.equals(DreamSkill.LUNAR_GRAVITY2) && !dreamSkills.contains(DreamSkill.LUNAR_GRAVITY1)) {}
				else if (skill.equals(DreamSkill.OVERHYDRATION2) && !dreamSkills.contains(DreamSkill.OVERHYDRATION1)) {}
				else if (skill.equals(DreamSkill.RAGING_WAVE3) && !dreamSkills.contains(DreamSkill.RAGING_WAVE2)) {}
				else if (skill.equals(DreamSkill.RAGING_WAVE2) && !dreamSkills.contains(DreamSkill.RAGING_WAVE1)) {}
				else if (skill.equals(DreamSkill.SURFING2) && !dreamSkills.contains(DreamSkill.SURFING1)) {}
				else if (skill.equals(DreamSkill.FREEDOM) && dreamSkills.contains(DreamSkill.OTHER_SIDE)) {}
				else if (skill.equals(DreamSkill.WAKE_UP) && !dreamSkills.contains(DreamSkill.FOAM1)) {}
				else if (skill.equals(DreamSkill.DREAMY2) && !dreamSkills.contains(DreamSkill.DREAMY1)) {}
				else if (skill.equals(DreamSkill.ABYSS2) && !dreamSkills.contains(DreamSkill.ABYSS1)) {}
				else values.add(skill);
			}
		}
		
		private MaterialX getQualityBlock(int a) {
			switch(a) {
			case 0: return MaterialX.WHITE_BED;
			case 1: return MaterialX.LIME_BED;
			case 2: return MaterialX.LIGHT_BLUE_BED;
			case 3: return MaterialX.MAGENTA_BED;
			case 4: return MaterialX.YELLOW_BED;
			}
			return null;
		}
		
		private String getQualityColor(int a) {
			switch(a) {
			case 0: return "§f";
			case 1: return "§a";
			case 2: return "§b";
			case 3: return "§d";
			case 4: return "§e§l";
			}
			return null;
		}
		
		private void placeItem() {
			if (dreamSkills.contains(DreamSkill.SIRENS_BLESS)) {
				for (int i = 0; i < 5; i++) {
					ItemStack item = new ItemBuilder(getQualityBlock(list.get(i).getQuality())).build();
					ItemMeta meta = item.getItemMeta();
					meta.setDisplayName(getQualityColor(list.get(i).getQuality()) + list.get(i).getName());
					List<String> lore = Messager.asList();
					lore.add("§3퀄리티§7: " + getQualityColor(list.get(i).getQuality()) + list.get(i).getQuality());
					String exp = list.get(i).getExplain();
					int len = exp.length();
					StringBuilder builder = new StringBuilder("§f");
					int spaces = 0;
					for (int j = 0; j < len; j++) {
					    char c = exp.charAt(j);
					    if (c == ' ') spaces++;
					    builder.append(c);
					    if (spaces == 5) {
					    	spaces = 0;
					        lore.add(builder.toString());
					        builder = new StringBuilder("§f");
					    }
					}
					lore.add(builder.toString());
					if (lossList.size() > 0) {
						lore.add("§7");
						RealSkill realSkill = lossList.get(i);
						lore.add("§c[효과 제거§7: §c" + realSkill.getName() + "]");
						String exp2 = realSkill.getExplain();
						int len2 = exp2.length();
						StringBuilder builder2 = new StringBuilder("§c");
						int spaces2 = 0;
						for (int j = 0; j < len2; j++) {
						    char c = exp2.charAt(j);
						    if (c == ' ') spaces2++;
						    builder2.append(c);
						    if (spaces2 == 5) {
						    	spaces2 = 0;
						        lore.add(builder2.toString());
						        builder2 = new StringBuilder("§c");
						    }
						}
					    if (builder2.length() > 2) {
					        lore.add(builder2.toString());
					    }
						lossSlots.put(i, realSkill);
					}
					meta.setLore(lore);
					item.setItemMeta(meta);
					gui.setItem(i, item);
					slots.put(i, list.get(i));
				}
			} else {
				for (int i = 0; i < 3; i++) {
					ItemStack item = new ItemBuilder(getQualityBlock(list.get(i).getQuality())).build();
					ItemMeta meta = item.getItemMeta();
					meta.setDisplayName(getQualityColor(list.get(i).getQuality()) + list.get(i).getName());
					List<String> lore = Messager.asList();
					lore.add("§3퀄리티§7: " + getQualityColor(list.get(i).getQuality()) + list.get(i).getQuality());
					String exp = list.get(i).getExplain();
					int len = exp.length();
					StringBuilder builder = new StringBuilder("§f");
					int spaces = 0;
					for (int j = 0; j < len; j++) {
					    char c = exp.charAt(j);
					    if (c == ' ') spaces++;
					    builder.append(c);
					    if (spaces == 5) {
					    	spaces = 0;
					        lore.add(builder.toString());
					        builder = new StringBuilder("§f");
					    }
					}
					lore.add(builder.toString());
					if (lossList.size() > 0) {
						lore.add("§7");
						RealSkill realSkill = lossList.get(i);
						lore.add("§c[효과 제거§7: §c" + realSkill.getName() + "]");
						String exp2 = realSkill.getExplain();
						int len2 = exp2.length();
						StringBuilder builder2 = new StringBuilder("§c");
						int spaces2 = 0;
						for (int j = 0; j < len2; j++) {
						    char c = exp2.charAt(j);
						    if (c == ' ') spaces2++;
						    builder2.append(c);
						    if (spaces2 == 5) {
						    	spaces2 = 0;
						        lore.add(builder2.toString());
						        builder2 = new StringBuilder("§c");
						    }
						}
					    if (builder2.length() > 2) {
					        lore.add(builder2.toString());
					    }
						lossSlots.put((i * 2), realSkill);
					}
					meta.setLore(lore);
					item.setItemMeta(meta);
					gui.setItem((i * 2), item);
					slots.put((i * 2), list.get(i));
				}
				gui.setItem(1, NULL);
				gui.setItem(3, NULL);	
			}
		}
		
		protected void onStart() {
			SoundLib.BLOCK_ENDER_CHEST_OPEN.playSound(getPlayer().getLocation(), 1f, 0.5f);
			Bukkit.getPluginManager().registerEvents(this, AbilityWar.getPlugin());
			getPlayer().openInventory(gui);
			if (dreamSkills.contains(DreamSkill.SIRENS_BLESS)) {
				for (int a = 0; a < 5; a++) {
					DreamSkill addskill = values.get(random.nextInt(values.size()));
					list.add(addskill);
					values.remove(addskill);
					if (realSkills.size() > 0) {
						lossList.add(random.pick(realSkills));
					}
				}	
			} else {
				for (int a = 0; a < 3; a++) {
					DreamSkill addskill = values.get(random.nextInt(values.size()));
					list.add(addskill);
					values.remove(addskill);
					if (realSkills.size() > 0) {
						lossList.add(random.pick(realSkills));
					}
				}
			}
			if (random.nextInt(dreamSkills.contains(DreamSkill.SIRENS_BLESS) ? 10 : 15) == 0) {
				List<DreamSkill> quality4 = new ArrayList<>(dreamSkillList.get(4));
				List<DreamSkill> leftquality4 = new ArrayList<>();
				for (DreamSkill q4 : quality4) {
					if (!dreamSkills.contains(q4)) leftquality4.add(q4);
				}
				DreamSkill specialskill = random.pick(leftquality4);
				list.add(specialskill);
				Collections.shuffle(list);
			}
		}
		
		protected void run(int arg0) {
			placeItem();
			if (arg0 == 60) SoundLib.BLOCK_NOTE_BLOCK_SNARE.playSound(getPlayer(), 1, 1.7f); 
			if (arg0 == 40) SoundLib.BLOCK_NOTE_BLOCK_SNARE.playSound(getPlayer(), 1, 1.7f); 
			if (arg0 == 20) SoundLib.BLOCK_NOTE_BLOCK_SNARE.playSound(getPlayer(), 1, 1.7f); 
		}
		
		@Override
		protected void onEnd() {
			onSilentEnd();
		}
		
		@Override
		protected void onSilentEnd() {
			if (selected != null) {
				if (selected.equals(DreamSkill.DREAM_COLLECTOR)) {
					dreamSkills.add(selected);
					List<DreamSkill> randomSkill = new ArrayList<>();
					for (DreamSkill dreamdreamSkills : DreamSkill.values()) {
						if (!dreamSkills.contains(dreamdreamSkills) && !dreamdreamSkills.equals(DreamSkill.DREAM_COLLECTOR)) randomSkill.add(dreamdreamSkills);
					}
					new AbilityTimer(TaskType.REVERSE, 5) {
						
						@Override
						public void run(int count) {
							DreamSkill skill = random.pick(randomSkill);
							dreamSkills.add(skill);
							getPlayer().sendMessage("§e[§b꿈 수집가§e] " + getQualityColor(skill.getQuality()) + skill.getName() + "을 획득하였습니다.");
							getPlayer().sendMessage("§e[" + getQualityColor(skill.getQuality()) + skill.getName() + "§e] §f" + skill.getExplain());
							SoundLib.ENTITY_PLAYER_LEVELUP.playSound(getPlayer(), 1, 1);
							randomSkill.remove(skill);
						}

					}.setPeriod(TimeUnit.SECONDS, 1).start();
				} else dreamSkills.add(selected);
			} else dreamSkills.add(slots.get(0));
			
			if (lossSelected != null) realSkills.remove(lossSelected);
			else if (lossSlots.size() > 0) realSkills.remove(lossSlots.get(0));
			HandlerList.unregisterAll(this);
			getPlayer().closeInventory();
		}
		
		@EventHandler
		private void onInventoryClose(InventoryCloseEvent e) {
			if (e.getInventory().equals(gui)) stop(false);
		}

		@EventHandler
		private void onQuit(PlayerQuitEvent e) {
			if (e.getPlayer().getUniqueId().equals(getPlayer().getUniqueId())) stop(false);
		}
		
		@EventHandler
		private void onPlayerMove(PlayerMoveEvent e) {
			if (e.getPlayer().equals(getPlayer())) e.setCancelled(true);
		}
		
		@EventHandler
		private void onEntityDamage(EntityDamageEvent e) {
			if (e.getEntity().equals(getPlayer())) {
				e.setDamage(e.getDamage() / 2);
				if (!skill.isRunning() && realSkills.contains(RealSkill.INVINCIBLE)) {
					e.setCancelled(true);
				}
			}
		}

		@EventHandler
		private void onInventoryClick(InventoryClickEvent e) {
			if (e.getInventory().equals(gui)) {
				e.setCancelled(true);
				if (slots.containsKey(e.getSlot())) {
					selected = slots.get(e.getSlot());
					if (lossSlots.containsKey(e.getSlot())) lossSelected = lossSlots.get(e.getSlot());
					getPlayer().closeInventory();
				}
			}
		}
		
		@EventHandler
		private void onSkyWhaleExpEarn(SkyWhaleExpEarnEvent e) {
			if (this.isRunning()) e.setCancelled(true);
		}
		
	}

}