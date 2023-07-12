package rainstar.abilitywar.ability;

import java.text.DecimalFormat;
import java.util.ArrayList;
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
import org.bukkit.event.entity.EntityDamageByBlockEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.entity.EntityRegainHealthEvent.RegainReason;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerInteractEvent;
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
import rainstar.abilitywar.effect.Burn;
import rainstar.abilitywar.effect.Corrosion;
import rainstar.abilitywar.effect.Dream;
import rainstar.abilitywar.utils.Healing;

@AbilityManifest(name = "하늘고래", rank = Rank.SPECIAL, species = Species.ANIMAL, explain = {
		"§7패시브 §8- §d드림 이터§f: 능력 소지자에게 입힌 최종 피해 × 대상의 능력 등급만큼",
		" §a꿈 경험치§f를 획득해 일정량 모으는 것으로 §a꿈 레벨§f을 상승시킬 수 있습니다.",
		" §a꿈 레벨§f로 얻는 스킬은 §7철괴를 좌클릭§f하여 볼 수 있습니다.",
		"§7철괴 우클릭 §8- §b드림 아쿠아리움§f: $[DURATION]초간 나갈 수 없는 §b꿈§f의 §b들판§f을 펼칩니다.",
		" 또한 §a꿈 레벨§f에 따른 추가 효과를 사용할 수 있습니다. $[COOLDOWN]",
		"§7영역 내 패시브 §8- §3웨이브§f: 영역의 중심에서부터 파도가 퍼져나가",
		" 적을 피해입히며 밀쳐냅니다. 피해는 중심에서 멀어질수록 강력해집니다.",
		" 피해는 영역에서 자신의 최고 대미지에 비례§8(§7× 0.5~1.5§8)§f합니다.",
		"§b[§7아이디어 제공자§b] §bSleepground"
		},
		summarize = {
		"§c적 공격 시§f 꿈 경험치를 모아 레벨이 상승합니다.",
		"꿈 레벨 효과는 §b필드§f 내에서 사용 가능하고, §7철괴 좌클릭§f으로 확인합니다.",
		"§7철괴 우클릭 시§f §b필드§f를 만들어 지속적으로 §3파도§f가 쳐 적들에게 피해를 입힙니다."
		})

public class SkyWhale extends AbilityBase implements ActiveHandler {

	final Multimap<Integer, DreamSkill> skillList = ArrayListMultimap.create();
	
	public SkyWhale(Participant participant) {
		super(participant);
		
		skillList.put(0, DreamSkill.FIRE_IMMUNE);
		skillList.put(0, DreamSkill.OTHER_SIDE);
		skillList.put(0, DreamSkill.REVERSE);
		skillList.put(0, DreamSkill.LUNAR_GRAVITY1);
		skillList.put(0, DreamSkill.FOAM1);
		skillList.put(1, DreamSkill.RAGING_WAVE1);
		skillList.put(1, DreamSkill.WATER_ANSWER);
		skillList.put(1, DreamSkill.SURFING1);
		skillList.put(1, DreamSkill.FASTWALK1);
		skillList.put(1, DreamSkill.AREA_REDUCE1);
		skillList.put(1, DreamSkill.LUNAR_GRAVITY2);
		skillList.put(1, DreamSkill.FOAM2);
		skillList.put(2, DreamSkill.OVERHYDRATION1);
		skillList.put(2, DreamSkill.NOTHINGNESS);
		skillList.put(2, DreamSkill.SWORDAURA);
		skillList.put(2, DreamSkill.FOAM3);
		skillList.put(2, DreamSkill.RAGING_WAVE2);
		skillList.put(2, DreamSkill.SURFING2);
		skillList.put(2, DreamSkill.FASTWALK2);
		skillList.put(2, DreamSkill.AREA_REDUCE2);
		skillList.put(3, DreamSkill.ABILITY_CONTROL);
		skillList.put(3, DreamSkill.WAKE_UP);
		skillList.put(3, DreamSkill.FREEDOM);
		skillList.put(3, DreamSkill.SLEEP_WAVE);
		skillList.put(3, DreamSkill.INCEPTION);
		skillList.put(3, DreamSkill.CALL_OF_SEA);
		skillList.put(3, DreamSkill.FLIGHT);
		skillList.put(3, DreamSkill.SCALD);
		skillList.put(3, DreamSkill.RAGING_WAVE3);
		skillList.put(3, DreamSkill.OVERHYDRATION2);
		skillList.put(4, DreamSkill.INSTANT);
		skillList.put(4, DreamSkill.STEP_UP);
		skillList.put(4, DreamSkill.DREAM_COLLECTOR);
		skillList.put(4, DreamSkill.TSUNAMI);
		skillList.put(4, DreamSkill.SIRENS_BLESS);
		skillList.put(4, DreamSkill.LIMIT_BREAKER);
	}
	
	enum DreamSkill {
		FIRE_IMMUNE("화염 내성", "모든 화염계 피해를 받지 않습니다.", 0),
		OTHER_SIDE("건너편", "영역의 끝에 다다르면 반대편 영역의 끝으로 이동됩니다. 자신만 적용됩니다.", 0),
		REVERSE("역방향", "파도가 영역의 끝에서 중심을 향해 칩니다. 이때 대미지는 중심쪽이 더 강해집니다.", 0),
		LUNAR_GRAVITY1("달의 인력 I", "밤 시간에는 파도의 주기가 25% 감소합니다.", 0),
		FOAM1("물거품 I", "영역이 종료될 때 영역 내에서 받은 피해량 50%를 회복합니다.", 0),
		RAGING_WAVE1("거센 파도 I", "파도 피해량과 넉백이 20% 증가합니다.", 1),
		WATER_ANSWER("물의 답", "영역 내 모든 대상의 체력을 확인 가능합니다.", 1),
		SURFING1("파도타기 I", "파도에 자신이 닿으면 이동 속도가 순간적으로 30% 증가합니다.", 1),
		FASTWALK1("빠른 걸음 I", "영역 내에서 이동 속도가 15% 증가합니다.", 1),
		AREA_REDUCE1("영역 축소 I", "영역 범위가 15칸에서 10칸으로 축소됩니다.", 1),
		LUNAR_GRAVITY2("달의 인력 II", "밤 시간에는 파도의 주기가 50% 감소합니다. 영역이 지속되는 동안 세상의 시간이 밤이 됩니다.", 1),
		FOAM2("물거품 II", "영역이 종료될 때 영역 내에서 받은 피해량 75%를 회복합니다.", 1),
		OVERHYDRATION1("수분 과다 I", "파도에 맞은 적은 2초간 부식됩니다.", 2),
		NOTHINGNESS("무", "영역과 파도의 파티클을 자신만 볼 수 있습니다.", 2),
		SWORDAURA("검기", "2초마다 검을 휘두를 때 지형지물과 생명체를 관통하는 특수 투사체를 발사합니다.", 2),
		FOAM3("물거품 III", "영역이 종료될 때 영역 내에서 자신이 받은 피해를 전부 회복합니다.", 2),
		RAGING_WAVE2("거센 파도 II", "파도 피해량과 넉백이 40% 증가합니다.", 2),
		SURFING2("파도타기 II", "파도에 자신이 닿으면 이동 속도가 순간적으로 30% 증가하고, 체력을 5% 회복합니다.", 2),
		FASTWALK2("빠른 걸음 II", "영역 내에서 이동 속도가 30% 증가합니다.", 2),
		AREA_REDUCE2("영역 축소 II", "영역 범위가 10칸에서 5칸으로 축소됩니다.", 2),
		ABILITY_CONTROL("능력 장악", "영역 내에서 자신 외 액티브 / 타게팅 스킬을 발동할 수 없습니다.", 3),
		WAKE_UP("꿈 깨시지", "치명적인 피해를 입을 때 영역이 즉시 종료됩니다. 물거품이 있을 때만 획득 가능합니다.", 3),
		FREEDOM("자유 해방", "본인은 영역 밖으로 나갈 수 있습니다. 건너편이 있으면 등장하지 않습니다.", 3),
		SLEEP_WAVE("수면파", "25% 확률로 파도에 맞은 적이 5초간 몽환에 빠집니다.", 3),
		INCEPTION("몽중몽", "영역 지속 중 적 처치 시 / 꿈 레벨 업 시 지속시간이 20초 추가됩니다.", 3),
		CALL_OF_SEA("바다의 부름", "30칸 내의 생명체가 영역 중심으로 끌어당겨집니다.", 3),
		FLIGHT("비행", "자유로운 비행이 가능합니다.", 3),
		SCALD("열탕", "파도에 맞은 적을 3초간 불태우고 화상을 입힙니다.", 3),
		RAGING_WAVE3("거센 파도 III", "파도 피해량과 넉백이 60% 증가합니다.", 3),
		OVERHYDRATION2("수분 과다 II", "파도에 맞은 적은 3.5초간 부식됩니다.", 3),
		INSTANT("즉발", "꿈 영역이 연출 없이 즉시 전개됩니다.", 4),
		STEP_UP("스텝 업", "획득 이후 꿈 레벨 업 시마다 공격력이 7.5%씩 증가합니다.", 4),
		DREAM_COLLECTOR("꿈 수집가", "꿈 경험치 획득량이 2배가 됩니다.", 4),
		TSUNAMI("쓰나미", "파도 판정 범위가 3배로 증가합니다. 파도가 영역을 넘어 조금 더 잔류합니다.", 4),
		SIRENS_BLESS("세이렌의 축복", "등장하는 스킬의 최저 퀄리티가 1씩 증가합니다. 4퀄리티 스킬이 등장할 확률이 증가합니다.", 4),
		LIMIT_BREAKER("리밋 브레이커", "레벨 업을 10까지 할 수 있습니다. 8~10렙 간 레벨 업에서는 퀄리티 상관 없이 모든 스킬이 등장합니다.", 4);
		
		private final String name;
		private final String explain;
		private final int quality;
		
		DreamSkill(String name, String explain, Integer quality) {
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
	
	private static final ImmutableMap<Rank, Double> rankmultiply = ImmutableMap.<Rank, Double>builder()
			.put(Rank.C, 1.0)
			.put(Rank.B, 1.2)
			.put(Rank.A, 1.5)
			.put(Rank.S, 2.0)
			.put(Rank.L, 2.3)
			.put(Rank.SPECIAL, 3.0)
			.build();

	
	public static final SettingObject<Integer> DURATION = abilitySettings.new SettingObject<Integer>(
			SkyWhale.class, "duration", 20, "# 지속 시간") {

		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}

	};
	
	public static final SettingObject<Integer> COOLDOWN = abilitySettings.new SettingObject<Integer>(
			SkyWhale.class, "cooldown", 135, "# 쿨타임") {

		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}

		@Override
		public String toString() {
			return Formatter.formatCooldown(getValue());
		}

	};
	
	public static final SettingObject<Integer> WAVE_PERIOD = abilitySettings.new SettingObject<Integer>(
			SkyWhale.class, "wave-period", 60, "# 파도의 주기 (단위: 틱, 1틱 = 0.05초)") {

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
	
	private BossBar bossbar;
	
	private int stacks;
	private boolean turns = true;
	
	private final DecimalFormat df = new DecimalFormat("0");
	
	private List<DreamSkill> skills = new ArrayList<>();

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
		case 7: return "";
		case 8: return "+";
		case 9: return "++";
		case 10: return "+++";
		}
		return "";
	}
	
	public boolean ActiveSkill(Material material, ClickType clicktype) {
	    if (material.equals(Material.IRON_INGOT)) {
	    	if (clicktype.equals(ClickType.RIGHT_CLICK)) {
		    	if (!isSkillRunning && !cooldown.isCooldown()) {
	    	   		if (skills.contains(DreamSkill.INSTANT)) skill.start();
	    	   		else showSpiral(RGB.AQUA);
	    	   		isSkillRunning = true;
	    	   		return true;		
		    	} else if (isSkillRunning) {
		    		getPlayer().sendMessage("§3[§b!§3] §c아직 스킬이 지속 중입니다.");
		    	}	
	    	} else if (clicktype.equals(ClickType.LEFT_CLICK)) {
	    		getPlayer().sendMessage("§e=========== §b꿈 스킬 §e===========");
	    		final StringJoiner joiner = new StringJoiner("\n");
	    		joiner.add("§aLevel " + (dreamlevel == 7 ? "MAX" + getOver(dreamlevel) : dreamlevel));
	    		for (DreamSkill dreamskill : skills) {
	    			joiner.add("§3" + dreamskill.getName() + "§7: §b" + dreamskill.getExplain());
	    		}
	    		getPlayer().sendMessage(joiner.toString());
	    		getPlayer().sendMessage("§e================================");
	    	}
	    }
		return false;
	}
	
    private final AbilityTimer attackcool = new AbilityTimer(40) {
    	
    	@Override
		public void run(int count) {
    	}
    	
    }.setPeriod(TimeUnit.TICKS, 1).register();
	
	@SubscribeEvent(priority = 5)
	public void onEntityDamage(EntityDamageEvent e) {
		if (e.getEntity().equals(getPlayer())) {
			if (skill.isRunning() && LocationUtil.isInCircle(center, getPlayer().getLocation(), nowrange)) {
				if (skills.contains(DreamSkill.FIRE_IMMUNE)) {
					if (e.getCause().equals(DamageCause.LAVA) || e.getCause().equals(DamageCause.FIRE_TICK) || e.getCause().equals(DamageCause.FIRE) || e.getCause().equals(DamageCause.HOT_FLOOR)) {
						e.setDamage(0);
						e.setCancelled(true);
					}	
				}
				
				if (skills.contains(DreamSkill.FOAM3)) lostHealth += e.getFinalDamage();	
				else if (skills.contains(DreamSkill.FOAM2)) lostHealth += (e.getFinalDamage() * 0.75);
				else if (skills.contains(DreamSkill.FOAM1)) lostHealth += (e.getFinalDamage() * 0.5);
				
				if (getPlayer().getHealth() - e.getFinalDamage() <= 0 && skills.contains(DreamSkill.WAKE_UP)) {
					ParticleLib.ITEM_CRACK.spawnParticle(getPlayer().getLocation(), 0, 1, 0, 100, 0.3, MaterialX.CLOCK);
					SoundLib.BLOCK_END_PORTAL_SPAWN.playSound(getPlayer().getLocation(), 1, 1.5f);
					e.setCancelled(true);
					skill.stop(false);
				}
			}	
		}
	}
	
	@SubscribeEvent
	public void onParticipantDeath(ParticipantDeathEvent e) {
		if (getPlayer().equals(e.getParticipant().getPlayer().getKiller()) && skill.isDuration() && skills.contains(DreamSkill.INCEPTION)) {
			skill.setCount(skill.getCount() + 400);
		}
	}
	
	@SubscribeEvent
	public void onEntityDamageByBlock(EntityDamageByBlockEvent e) {
		onEntityDamage(e);
	}
	
	@SubscribeEvent
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
			if (skill.isRunning()) e.setDamage(e.getDamage() * (1 + (damagestack * 0.075)));
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
							if (mymix.getFirst().equals(this)) rankvalue = mix.getFirst().getRank();
							else if (mymix.getSecond().equals(this)) rankvalue = mix.getSecond().getRank();
						}
					} else rankvalue = ab.getRank();
					if ((dreamlevel < 10 && skills.contains(DreamSkill.LIMIT_BREAKER)) || dreamlevel < 7) {
						dreamexp += e.getFinalDamage() * 3 * rankmultiply.get(rankvalue) * (skills.contains(DreamSkill.DREAM_COLLECTOR) ? 2 : 1);
						SoundLib.ENTITY_EXPERIENCE_ORB_PICKUP.playSound(getPlayer(), 1, 1.35f);
					}
				}
			}
		}
		if (skill.isRunning() && e.getDamager().hasMetadata("Wave")) {
			if ((e.getDamage() * 0.8) > bestDamage) bestDamage = e.getDamage() * 0.8;
		}
	}
	
	@SubscribeEvent
	public void onPreActiveSkill(AbilityPreActiveSkillEvent e) {
		if (skill.isRunning() && skills.contains(DreamSkill.ABILITY_CONTROL) && !e.getParticipant().equals(getParticipant()) && LocationUtil.isInCircle(center, e.getParticipant().getPlayer().getLocation(), nowrange)) {
			e.getParticipant().getPlayer().sendMessage("§3[§b하늘고래§3] §c능력을 사용할 수 없습니다!");
			e.setCancelled(true);
		}
	}
	
	@SubscribeEvent
	public void onPreTargetSkill(AbilityPreTargetEvent e) {
		if (skill.isRunning() && skills.contains(DreamSkill.ABILITY_CONTROL) && !e.getParticipant().equals(getParticipant()) && LocationUtil.isInCircle(center, e.getParticipant().getPlayer().getLocation(), nowrange)) {
			e.getParticipant().getPlayer().sendMessage("§3[§b하늘고래§3] §c능력을 사용할 수 없습니다!");
			e.setCancelled(true);
		}
	}
	
	
	@SubscribeEvent
	public void onPlayerMove(PlayerMoveEvent e) {
		if (skill.isRunning() && (predicate.test(e.getPlayer()) || getPlayer().equals(e.getPlayer()))) {
			if (LocationUtil.isInCircle(center, e.getFrom(), nowrange) && !LocationUtil.isInCircle(center, e.getTo(), nowrange)) {
				if (skills.contains(DreamSkill.OTHER_SIDE) && e.getPlayer().equals(getPlayer())) {
					Vector beside = center.toVector().clone().subtract(e.getFrom().toVector().clone());
					Location goTo = center.clone().add(beside);
					goTo.setY(e.getFrom().getY());
					e.setTo(goTo.setDirection(getPlayer().getLocation().getDirection()));
				} else if (!(skills.contains(DreamSkill.FREEDOM) && e.getPlayer().equals(getPlayer()))) e.setTo(e.getFrom());
			}
		}
	}
	
	@SubscribeEvent(onlyRelevant = true)
	private void onPlayerInteract(final PlayerInteractEvent e) {
		if (skill.isRunning() && skills.contains(DreamSkill.SWORDAURA)) {
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
    		bossbar = Bukkit.createBossBar("§3Lv §b" + (dreamlevel >= 7 ? "§cMAX" + getOver(dreamlevel) : df.format(dreamlevel)) + " §7/ §aEXP§7: §e" + (df.format(dreamexp)), BarColor.GREEN, BarStyle.SOLID);
    		bossbar.setProgress(dreamlevel >= 7 ? 1 : Math.min(1, dreamexp / (100 + (dreamlevel * 20))));
    		bossbar.addPlayer(getPlayer());
    		if (ServerVersion.getVersion() >= 10) bossbar.setVisible(true);
    	}
    	
    	@Override
		public void run(int count) {
    		if ((dreamlevel >= 7 && skills.contains(DreamSkill.LIMIT_BREAKER)) || dreamlevel >= 10) bossbar.setProgress(1);
    		else bossbar.setProgress(Math.min(1, dreamexp / (100 + (dreamlevel * 20))));
			bossbar.setTitle("§3Lv §b" + (dreamlevel >= 7 ? "§cMAX" + getOver(dreamlevel) : df.format(dreamlevel)) + " §7/ §aEXP§7: §e" + (df.format(dreamexp)));
    		if (dreamexp >= (100 + (dreamlevel * 20)) && dreamlevel < (skills.contains(DreamSkill.LIMIT_BREAKER) ? 10 : 7)) levelup();
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
		if (skill.isDuration() && skills.contains(DreamSkill.INCEPTION)) {
			skill.setCount(skill.getCount() + 400);
		}
		if (skills.contains(DreamSkill.STEP_UP)) damagestack++;
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
		dreamlevel = Math.min((skills.contains(DreamSkill.LIMIT_BREAKER) ? 10 : 7), dreamlevel + 1);
		new SkillGUI(dreamlevel).start();
		dreamexp = 0;
	}
	
	private final AbilityTimer speedup = new AbilityTimer(30) {
		
		private final AttributeModifier modifier = new AttributeModifier(UUID.randomUUID(), "addspeed", 0.3, Operation.ADD_SCALAR);
		
		@Override
		public void onStart() {
			try {
				getPlayer().getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).addModifier(modifier);
			} catch (IllegalArgumentException ignored) {
			}
		}
		
		@Override
		public void onEnd() {
			onSilentEnd();
		}
		
		@Override
		public void onSilentEnd() {
			getPlayer().getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).removeModifier(modifier);
		}
		
	}.setPeriod(TimeUnit.TICKS, 1).register();
	
	private final AbilityTimer biggerEffect = new AbilityTimer() {
		
		@Override
		protected void onStart() {
			if (skills.contains(DreamSkill.AREA_REDUCE2)) nowrange = 5;
			else if (skills.contains(DreamSkill.AREA_REDUCE1)) nowrange = 10;
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
				if (skills.contains(DreamSkill.NOTHINGNESS)) ParticleLib.REDSTONE.spawnParticle(getPlayer(), loc, aqua);
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
			worldtime = getPlayer().getWorld().getTime();
			if (skills.contains(DreamSkill.INSTANT)) {
				SoundLib.ENTITY_PLAYER_SPLASH.playSound(center, 2, 0.5f);
				ParticleLib.TOTEM.spawnParticle(center, 1, 1, 1, 25, 0);
			}
			if (center == null) center = LocationUtil.floorY(getPlayer().getLocation().clone());
			if (skills.contains(DreamSkill.AREA_REDUCE2)) nowrange = 5;
			else if (skills.contains(DreamSkill.AREA_REDUCE1)) nowrange = 10;
			else nowrange = 15;
			
			if (skills.contains(DreamSkill.FASTWALK2)) modifier1 = new AttributeModifier(UUID.randomUUID(), "addspeed3", 0.3, Operation.ADD_SCALAR);
			else if (skills.contains(DreamSkill.FASTWALK1)) modifier1 = new AttributeModifier(UUID.randomUUID(), "addspeed2", 0.15, Operation.ADD_SCALAR);
			
			if (modifier1 != null) getPlayer().getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).addModifier(modifier1);

			if (skills.contains(DreamSkill.WATER_ANSWER)) {
				for (Player player : getPlayer().getWorld().getPlayers()) {
					if (predicate.test(player)) {
						new DataCatch(player).start();
					}
				}
			}
		}

		@Override
		protected void onDurationProcess(int count) {
			if (skills.contains(DreamSkill.NOTHINGNESS)) ParticleLib.DRIP_WATER.spawnParticle(getPlayer(), center, nowrange, 5, nowrange, 20, 0);
			else ParticleLib.DRIP_WATER.spawnParticle(center, nowrange, 5, nowrange, 20, 0);
			
			if (skills.contains(DreamSkill.FLIGHT)) {
				getPlayer().setAllowFlight(true);
				if (getPlayer().isFlying()) ParticleLib.CLOUD.spawnParticle(getPlayer().getLocation(), 0.5, 0.1, 0.5, 10, 0);
			}
			
			if (count % 2 == 0) {
				double playerY = getPlayer().getLocation().getY();
				Circle nowcircle = circle;
				if (skills.contains(DreamSkill.AREA_REDUCE2)) nowcircle = circle3;
				else if (skills.contains(DreamSkill.AREA_REDUCE1)) nowcircle = circle2;
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
					if (skills.contains(DreamSkill.NOTHINGNESS)) ParticleLib.REDSTONE.spawnParticle(getPlayer(), location, aqua);
					else ParticleLib.REDSTONE.spawnParticle(location, aqua);
				}
				
				if (skills.contains(DreamSkill.CALL_OF_SEA)) {
					for (LivingEntity livingEntity : LocationUtil.getNearbyEntities(LivingEntity.class, center, 30, 30, predicate)) {
						livingEntity.setVelocity(VectorUtil.validateVector(center.toVector().subtract(livingEntity.getLocation().toVector()).normalize().setY(0).multiply(0.035)));
					}
				}
			}
			
			if (count % 10 == 0) {
				for (Player p : LocationUtil.getEntitiesInCircle(Player.class, center, nowrange, soundpredicate)) {
					SoundLib.BLOCK_WATER_AMBIENT.playSound(p, 1, 1.2f);	
				}
			}
			
			int nowperiod = period;
			if (isNight(getPlayer().getWorld().getTime())) {
				if (skills.contains(DreamSkill.LUNAR_GRAVITY2)) nowperiod *= 0.5;
				else if(skills.contains(DreamSkill.LUNAR_GRAVITY1)) nowperiod *= 0.75;	
			} else if (skills.contains(DreamSkill.LUNAR_GRAVITY2)) {
				getPlayer().getWorld().setTime(17500);
				timechanged = true;
			}
			
			if (count % nowperiod == 0) {
				new Wave(bestDamage).start();
				SoundLib.PLING.playInstrument(center, 1.5f, Note.sharp(0, Tone.F));
			}
		}
		
		@Override
		protected void onDurationEnd() {
			onDurationSilentEnd();
		}
		
		@Override
		protected void onDurationSilentEnd() {
			bestDamage = 7;
			isSkillRunning = false;
			if (skills.contains(DreamSkill.FLIGHT)) {
				if (!getPlayer().getGameMode().equals(GameMode.CREATIVE) && !getPlayer().getGameMode().equals(GameMode.SPECTATOR)) {
					getPlayer().setAllowFlight(false);
				}
			}
			
			if (skills.contains(DreamSkill.FOAM1)) {
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
			if (skills.contains(DreamSkill.REVERSE)) waveRadius = nowrange;
			else waveRadius = 0;
			damager = center.getWorld().spawn(center.clone().add(99999, 0, 99999), ArmorStand.class);
			damager.setVisible(false);
			damager.setInvulnerable(true);
			damager.setGravity(false);
			damager.setMetadata("Wave", NULL_VALUE);
			NMS.removeBoundingBox(damager);
			boundingbox = skills.contains(DreamSkill.TSUNAMI) ? 1.8 : 0.6;
		}
		
		@Override
		public void run(int count) {
			if (!skill.isRunning()) this.stop(false);
			if (skills.contains(DreamSkill.REVERSE)) {if (waveRadius > 0) waveRadius = Math.max(0.01, waveRadius - 0.35);}
			else if (waveRadius < nowrange + (skills.contains(DreamSkill.TSUNAMI) ? 7.5 : 0)) waveRadius += 0.35;
			
			if ((skills.contains(DreamSkill.REVERSE) && waveRadius <= 0.01) || waveRadius >= nowrange + (skills.contains(DreamSkill.TSUNAMI) ? 7.5 : 0)) this.stop(false);
			
			if (skills.contains(DreamSkill.SCALD)) {
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
				if (skills.contains(DreamSkill.NOTHINGNESS)) {
					ParticleLib.REDSTONE.spawnParticle(getPlayer(), loc, color);
					if (addY <= 0.1 || count < 5) ParticleLib.WATER_SPLASH.spawnParticle(getPlayer(), loc, 0, 0, 0, 1, 0);
				} else {
					ParticleLib.REDSTONE.spawnParticle(loc, color);
					if (addY <= 0.1 || count < 5) ParticleLib.WATER_SPLASH.spawnParticle(loc, 0, 0, 0, 1, 0);
				}
				for (Damageable damageable : LocationUtil.getNearbyEntities(Damageable.class, loc, boundingbox, boundingbox, predicate)) {
					double increase = 0.5;
					if (skills.contains(DreamSkill.REVERSE)) increase = (1 - (waveRadius / nowrange)) + 0.5;
					else increase = (waveRadius / nowrange) + 0.5;
					
					double addIncrease = 1;
					if (skills.contains(DreamSkill.RAGING_WAVE3)) addIncrease = 1.6;
					else if (skills.contains(DreamSkill.RAGING_WAVE2)) addIncrease = 1.4;
					else if (skills.contains(DreamSkill.RAGING_WAVE1)) addIncrease = 1.2;
					
					if (!damageable.equals(getPlayer())) {
						damageable.damage(increase * damage * addIncrease, damager);
						damageable.setVelocity(VectorUtil.validateVector(damageable.getLocation().toVector().subtract(center.toVector()).normalize().multiply(1.1 * addIncrease).setY(0.5)));
						if (damageable instanceof LivingEntity) PotionEffects.SLOW.addPotionEffect((LivingEntity) damageable, 100, 1, false);
						if (damageable instanceof Player) {
							Player player = (Player) damageable;
							if (skills.contains(DreamSkill.OVERHYDRATION1)) Corrosion.apply(getGame().getParticipant(player), TimeUnit.TICKS, skills.contains(DreamSkill.OVERHYDRATION2) ? 70 : 40);
							if (skills.contains(DreamSkill.SLEEP_WAVE) && random.nextInt(4) == 0) Dream.apply(getGame().getParticipant(player), TimeUnit.TICKS, 100);
							if (skills.contains(DreamSkill.SCALD)) Burn.apply(getGame().getParticipant(player), TimeUnit.TICKS, 60);
						}
						if (skills.contains(DreamSkill.SCALD)) damageable.setFireTicks(damageable.getFireTicks() + (damageable.getFireTicks() <= 0 ? 80 : 60));
					} else if (skills.contains(DreamSkill.SURFING1)) {
						speedup.start();
						if (skills.contains(DreamSkill.SURFING2)) Healing.heal(getPlayer(), getPlayer().getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue() * 0.05, RegainReason.CUSTOM);
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
			SoundLib.ENTITY_PLAYER_SWIM.playSound(center, 2f, 1.4f);
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
			protected void run(int a) {				
				Location me = getPlayer().getLocation();			
				for (int i = 0; i < 6; i++) {
					if (addSpiralY >= 1.6) yUp = false;
					else if (addSpiralY <= 0.4) yUp = true;
					addSpiralY = yUp ? Math.min(1.6, addSpiralY + 0.02) : Math.max(0.4, addSpiralY - 0.02);
					radius -= (0.5 / 12);
					theta += (fourPi / count);
					Location loc = me.clone().add(Math.cos(theta) * radius, addSpiralY, Math.sin(theta) * radius);
					ParticleLib.REDSTONE.spawnParticle(loc, color);
					ParticleLib.DRIP_WATER.spawnParticle(loc, 0, 0, 0, 1, 0);
					SoundLib.ENTITY_BOAT_PADDLE_WATER.playSound(loc);
				}
			}
			
			@Override
			protected void onEnd() {
				biggerEffect.start();
			}
			
		}.setPeriod(TimeUnit.TICKS, 1).start();
	}
	
	public class DataCatch extends AbilityTimer {
		
		private final Player player;
		private final IHologram hologram;
		private int health = 0;
		private int maxHealth = 0;
		
		private DataCatch(Player player) {
			setPeriod(TimeUnit.TICKS, 1);
			this.player = player;
			this.hologram = NMS.newHologram(player.getWorld(), player.getLocation().getX(),
					player.getLocation().getY() + player.getEyeHeight() + 0.6, player.getLocation().getZ(), 
					Strings.repeat("§a|", health).concat(Strings.repeat("§7|", maxHealth - health)));
			hologram.display(getPlayer());
		}
		
		@Override
		protected void run(int count) {
			health = (int) player.getHealth();
			maxHealth = (int) player.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue();
			hologram.teleport(player.getWorld(), player.getLocation().getX(), 
					player.getLocation().getY() + player.getEyeHeight() + 0.6, player.getLocation().getZ(), 
					player.getLocation().getYaw(), 0);
			hologram.setText(Strings.repeat("§c|", health).concat(Strings.repeat("§7|", maxHealth - health)));
			if (!skill.isRunning()) this.stop(false);
			if (health <= 0) this.stop(false);
		}
		
		@Override
		protected void onEnd() {
			onSilentEnd();
		}
		
		@Override
		protected void onSilentEnd() {
			hologram.unregister();
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
		private final LimitedPushingList<DreamSkill> list = new LimitedPushingList<>(3);
		private Map<Integer, DreamSkill> slots = new HashMap<>();
		
		private DreamSkill selected;
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
				minquality = 0;
				maxquality = 0;
				break;
			case 2:
				minquality = 0;
				maxquality = 1;
				break;
			case 3:
			case 4:
				minquality = 1;
				maxquality = 2;
				break;
			case 5:
				minquality = 1;
				maxquality = 3;
				break;
			case 6:
				minquality = 2;
				maxquality = 3;
				break;
			case 7:
				minquality = 3;
				maxquality = 3;
				break;
			case 8:
			case 9:
			case 10:
				minquality = 0;
				maxquality = 3;
				break;
			}
			
			if (skills.contains(DreamSkill.SIRENS_BLESS)) minquality = Math.min(3, minquality + 1);
			
			for (int a = minquality; a <= maxquality; a++) {
				for (DreamSkill skill : skillList.get(a)) {
					if (!skills.contains(skill)) tempvalues.add(skill);
				}	
			}
			
			for (DreamSkill skill : tempvalues) {
				if (skill.equals(DreamSkill.AREA_REDUCE2) && !skills.contains(DreamSkill.AREA_REDUCE1)) {}
				else if (skill.equals(DreamSkill.FASTWALK2) && !skills.contains(DreamSkill.FASTWALK1)) {}
				else if (skill.equals(DreamSkill.FOAM3) && !skills.contains(DreamSkill.FOAM2)) {}
				else if (skill.equals(DreamSkill.FOAM2) && !skills.contains(DreamSkill.FOAM1)) {}
				else if (skill.equals(DreamSkill.LUNAR_GRAVITY2) && !skills.contains(DreamSkill.LUNAR_GRAVITY1)) {}
				else if (skill.equals(DreamSkill.OVERHYDRATION2) && !skills.contains(DreamSkill.OVERHYDRATION1)) {}
				else if (skill.equals(DreamSkill.RAGING_WAVE3) && !skills.contains(DreamSkill.RAGING_WAVE2)) {}
				else if (skill.equals(DreamSkill.RAGING_WAVE2) && !skills.contains(DreamSkill.RAGING_WAVE1)) {}
				else if (skill.equals(DreamSkill.SURFING2) && !skills.contains(DreamSkill.SURFING1)) {}
				else if (skill.equals(DreamSkill.FREEDOM) && skills.contains(DreamSkill.OTHER_SIDE)) {}
				else if (skill.equals(DreamSkill.WAKE_UP) && !skills.contains(DreamSkill.FOAM1)) {}
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
				meta.setLore(lore);
				item.setItemMeta(meta);
				gui.setItem((i * 2), item);
				slots.put((i * 2), list.get(i));
			}
			gui.setItem(1, NULL);
			gui.setItem(3, NULL);
		}
		
		protected void onStart() {
			SoundLib.BLOCK_ENDER_CHEST_OPEN.playSound(getPlayer().getLocation(), 1f, 0.5f);
			Bukkit.getPluginManager().registerEvents(this, AbilityWar.getPlugin());
			getPlayer().openInventory(gui);
			for (int a = 0; a < 3; a++) {
				DreamSkill addskill = values.get(random.nextInt(values.size()));
				list.add(addskill);
				values.remove(addskill);
			}
			if (random.nextInt(skills.contains(DreamSkill.SIRENS_BLESS) ? 15 : 20) == 0) {
				List<DreamSkill> quality4 = new ArrayList<>(skillList.get(4));
				List<DreamSkill> leftquality4 = new ArrayList<>();
				for (DreamSkill q4 : quality4) {
					if (!skills.contains(q4)) leftquality4.add(q4);
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
			if (selected != null) skills.add(selected);
			else skills.add(slots.get(0));
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
			if (e.getEntity().equals(getPlayer())) e.setDamage(e.getDamage() / 2);
		}

		@EventHandler
		private void onInventoryClick(InventoryClickEvent e) {
			if (e.getInventory().equals(gui)) {
				e.setCancelled(true);
				if (slots.containsKey(e.getSlot())) {
					selected = slots.get(e.getSlot());
					getPlayer().closeInventory();
				}
			}
		}
		
	}
	
}