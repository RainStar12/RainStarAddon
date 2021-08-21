package RainStarSynergy;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringJoiner;

import javax.annotation.Nullable;

import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Note;
import org.bukkit.Note.Tone;
import org.bukkit.block.Block;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Damageable;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.SplashPotion;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent.RegainReason;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionData;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.potion.PotionType;

import RainStarEffect.Agro;
import RainStarEffect.Charm;
import RainStarEffect.Confusion;
import RainStarEffect.Madness;
import RainStarEffect.SnowflakeMark;
import daybreak.abilitywar.AbilityWar;
import daybreak.abilitywar.ability.AbilityBase;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.SubscribeEvent;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.ability.decorator.ActiveHandler;
import daybreak.abilitywar.config.ability.AbilitySettings.SettingObject;
import daybreak.abilitywar.game.GameManager;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.list.mix.synergy.Synergy;
import daybreak.abilitywar.game.manager.effect.registry.EffectRegistry;
import daybreak.abilitywar.game.manager.effect.registry.EffectRegistry.EffectRegistration;
import daybreak.abilitywar.game.module.DeathManager;
import daybreak.abilitywar.game.module.Wreck;
import daybreak.abilitywar.game.team.interfaces.Teamable;
import daybreak.abilitywar.utils.base.Formatter;
import daybreak.abilitywar.utils.base.collect.Pair;
import daybreak.abilitywar.utils.base.color.RGB;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.base.language.korean.KoreanUtil;
import daybreak.abilitywar.utils.base.language.korean.KoreanUtil.Josa;
import daybreak.abilitywar.utils.base.math.LocationUtil;
import daybreak.abilitywar.utils.base.math.VectorUtil;
import daybreak.abilitywar.utils.base.math.geometry.Circle;
import daybreak.abilitywar.utils.base.minecraft.entity.health.Healths;
import daybreak.abilitywar.utils.base.minecraft.nms.NMS;
import daybreak.abilitywar.utils.base.minecraft.version.ServerVersion;
import daybreak.abilitywar.utils.base.random.Random;
import daybreak.abilitywar.utils.library.BlockX;
import daybreak.abilitywar.utils.library.MaterialX;
import daybreak.abilitywar.utils.library.ParticleLib;
import daybreak.abilitywar.utils.library.SoundLib;
import daybreak.google.common.base.Predicate;
import daybreak.google.common.collect.ImmutableMap;
import daybreak.google.common.collect.ImmutableSet;

@SuppressWarnings("deprecation")
@AbilityManifest(name = "이과생", rank = Rank.SPECIAL, species = Species.HUMAN, explain = {
		"§7패시브 §8- §d물리 화학§f: $[RESTOCK]초마다 무작위의 상태이상 하나가 충전된",
		" 투척용 고통 및 무작위 부정 효과의 포션 하나를 비주류 손에 지급받습니다.",
		" 비주류 손을 사용할 수 없고, 자신은 부정 포션 효과를 긍정 효과로 변경합니다.",
		"§7패시브 §8- §b기하와 벡터§f: 투척용 포션이 일정 거리 내의 가장 가까운 대상에게 유도됩니다.",
		"§7철괴 좌클릭 §8- §e확률과 통계§f: 슬롯 3개짜리의 슬롯머신 하나를 가동하여",
		" 당첨된 효과들을 $[SLOT_DURATION]초간 받습니다. $[SLOT_COOLDOWN]",
		" 세 효과가 전부 다를 경우 효과 수치가 3배로 상승합니다.",
        " §c<코>§7:§f 주는 대미지 §c$[SLOT_ADD_DAMAGE]§f 상승 §7| §6<크>§7:§f 받는 대미지 §3$[SLOT_REDUCE_DAMAGE]§f 감소 §7| §e<스>§7:§f 초당 HP §d$[SLOT_HEAL]§f 회복"
		})

public class NaturalScienceStudent extends Synergy implements ActiveHandler {
	
	public NaturalScienceStudent(Participant participant) {
		super(participant);
	}
	
	public static final SettingObject<Integer> RESTOCK
	= synergySettings.new SettingObject<Integer>(NaturalScienceStudent.class,
			"restock", 13, "# 포션 재충전 시간", "# 쿨타임 감소 효과를 30%까지 받습니다.") {
		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}
	};
	
	public static final SettingObject<Double> SLOT_ADD_DAMAGE
	= synergySettings.new SettingObject<Double>(NaturalScienceStudent.class,
			"slot-add-damage", 1.5, "# 슬롯머신 <코> 추가 대미지") {
		@Override
		public boolean condition(Double value) {
			return value >= 0;
		}
	};
	
	public static final SettingObject<Double> SLOT_REDUCE_DAMAGE
	= synergySettings.new SettingObject<Double>(NaturalScienceStudent.class,
			"slot-reduce-damage", 1.5, "# 슬롯머신 <크> 대미지 감소") {
		@Override
		public boolean condition(Double value) {
			return value >= 0;
		}
	};
	
	public static final SettingObject<Double> SLOT_HEAL
	= synergySettings.new SettingObject<Double>(NaturalScienceStudent.class,
			"slot-heal", 0.5, "# 슬롯머신 <스> 초당 회복량") {
		@Override
		public boolean condition(Double value) {
			return value >= 0;
		}
	};
	
	public static final SettingObject<Integer> SLOT_COOLDOWN
	= synergySettings.new SettingObject<Integer>(NaturalScienceStudent.class,
			"slot-cooldown", 90, "# 슬롯머신 능력 쿨타임") {
		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}

		@Override
		public String toString() {
			return Formatter.formatCooldown(getValue());
		}
	};
	
	public static final SettingObject<Integer> SLOT_DURATION
	= synergySettings.new SettingObject<Integer>(NaturalScienceStudent.class,
			"slot-duration", 10, "# 슬롯머신 지속시간") {
		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}
	};
	
	public static final SettingObject<Integer> POTION_POISON_DURATION = 
			synergySettings.new SettingObject<Integer>(NaturalScienceStudent.class, "poiton-poison-duration", 14,
			"# 독 지속시간 (단위: 초)") {

		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}

	};
	
	public static final SettingObject<Integer> POTION_WEAKNESS_DURATION = 
			synergySettings.new SettingObject<Integer>(NaturalScienceStudent.class, "poiton-weakness-duration", 17,
			"# 나약함 지속시간 (단위: 초)") {

		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}

	};
	
	public static final SettingObject<Integer> POTION_SLOWNESS_DURATION = 
			synergySettings.new SettingObject<Integer>(NaturalScienceStudent.class, "poiton-slowness-duration", 23,
			"# 구속 지속시간 (단위: 초)") {

		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}

	};

	public static final SettingObject<Integer> POTION_WITHER_DURATION = 
			synergySettings.new SettingObject<Integer>(NaturalScienceStudent.class, "poiton-wither-duration", 19,
			"# 시듦 지속시간 (단위: 초)") {

		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}

	};
	
	public static final SettingObject<Integer> POTION_BLINDNESS_DURATION = 
			synergySettings.new SettingObject<Integer>(NaturalScienceStudent.class, "poiton-blind-duration", 12,
			"# 실명 지속시간 (단위: 초)") {

		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}

	};
	
	public static final SettingObject<Integer> POTION_CONFUSION_DURATION = 
			synergySettings.new SettingObject<Integer>(NaturalScienceStudent.class, "poiton-confusion-duration", 14,
			"# 멀미 지속시간 (단위: 초)") {

		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}

	};
	
	public static final SettingObject<Integer> POTION_GLOWING_DURATION = 
			synergySettings.new SettingObject<Integer>(NaturalScienceStudent.class, "poiton-glowing-duration", 90,
			"# 발광 지속시간 (단위: 초)") {

		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}

	};
	
	public static final SettingObject<Integer> POTION_HUNGER_DURATION = 
			synergySettings.new SettingObject<Integer>(NaturalScienceStudent.class, "poiton-hunger-duration", 120,
			"# 허기 지속시간 (단위: 초)") {

		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}

	};
	
	public static final SettingObject<Integer> POTION_LEVITATION_DURATION = 
			synergySettings.new SettingObject<Integer>(NaturalScienceStudent.class, "poiton-levitation-duration", 5,
			"# 공중 부양 지속시간 (단위: 초)") {

		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}

	};
	
	public static final SettingObject<Integer> POTION_SLOW_DIGGING_DURATION = 
			synergySettings.new SettingObject<Integer>(NaturalScienceStudent.class, "poiton-slow-digging-duration", 30,
			"# 채굴 피로 지속시간 (단위: 초)") {

		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}

	};
	
	public static final SettingObject<Integer> POTION_UNLUCK_DURATION = 
			synergySettings.new SettingObject<Integer>(NaturalScienceStudent.class, "poiton-unluck-duration", 300,
			"# 불행 지속시간 (단위: 초)") {

		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}

	};
	
	private final Predicate<Entity> predicate = new Predicate<Entity>() {
		@Override
		public boolean test(Entity entity) {
			if (entity.equals(getPlayer())) return false;
			if (entity instanceof ArmorStand) return false;
			if (entity instanceof Player) {
				if (!getGame().isParticipating(entity.getUniqueId())
						|| (getGame() instanceof DeathManager.Handler && ((DeathManager.Handler) getGame()).getDeathManager().isExcluded(entity.getUniqueId()))
						|| !getGame().getParticipant(entity.getUniqueId()).attributes().TARGETABLE.getValue()) {
					return false;
				}
				if (getGame() instanceof Teamable) {
					final Teamable teamGame = (Teamable) getGame();
					final Participant entityParticipant = teamGame.getParticipant(entity.getUniqueId()), participant = getParticipant();
					return !teamGame.hasTeam(entityParticipant) || !teamGame.hasTeam(participant) || (!teamGame.getTeam(entityParticipant).equals(teamGame.getTeam(participant)));
				}
			}
			return true;
		}

		@Override
		public boolean apply(@Nullable Entity arg0) {
			return false;
		}
	};
	
	private static final ImmutableMap<PotionEffectType, Pair<String, Color>> POTION_TYPES_GOOD 
	= ImmutableMap.<PotionEffectType, Pair<String, Color>>builder()
			.put(PotionEffectType.REGENERATION, Pair.of("§d재생", PotionEffectType.REGENERATION.getColor()))
			.put(PotionEffectType.SPEED, Pair.of("§b신속", PotionEffectType.SPEED.getColor()))
			.put(PotionEffectType.FIRE_RESISTANCE, Pair.of("§c화염 저항", PotionEffectType.FIRE_RESISTANCE.getColor()))
			.put(PotionEffectType.NIGHT_VISION, Pair.of("§9야간 투시", PotionEffectType.NIGHT_VISION.getColor()))
			.put(PotionEffectType.INCREASE_DAMAGE, Pair.of("§6힘", PotionEffectType.INCREASE_DAMAGE.getColor()))
			.put(PotionEffectType.JUMP, Pair.of("§a점프 강화", PotionEffectType.JUMP.getColor()))
			.put(PotionEffectType.WATER_BREATHING, Pair.of("§3수중 호흡", PotionEffectType.WATER_BREATHING.getColor()))
			.put(PotionEffectType.INVISIBILITY, Pair.of("§7투명화", PotionEffectType.INVISIBILITY.getColor()))
			.put(PotionEffectType.LUCK, Pair.of("§a행운", PotionEffectType.LUCK.getColor()))
			// 이 아래는 없는 효과들
			.put(PotionEffectType.ABSORPTION, Pair.of("§e흡수", Color.fromRGB(254, 246, 18)))
			.put(PotionEffectType.FAST_DIGGING, Pair.of("§e성급함", Color.fromRGB(254, 254, 143)))
			.put(PotionEffectType.HEALTH_BOOST, Pair.of("§c생명력 강화", Color.fromRGB(254, 178, 217)))
			.put(PotionEffectType.SATURATION, Pair.of("§e포만감", Color.fromRGB(254, 221, 115)))
			.put(PotionEffectType.DAMAGE_RESISTANCE, Pair.of("§8저항", Color.fromRGB(1, 96, 106)))
			.build();
	
	private static final ImmutableMap<PotionEffectType, Pair<String, Color>> POTION_TYPES_BAD 
	= ImmutableMap.<PotionEffectType, Pair<String, Color>>builder()
			.put(PotionEffectType.POISON, Pair.of("§2독", PotionEffectType.POISON.getColor()))
			.put(PotionEffectType.WEAKNESS, Pair.of("§7나약함", PotionEffectType.WEAKNESS.getColor()))
			.put(PotionEffectType.SLOW, Pair.of("§8구속", PotionEffectType.SLOW.getColor()))
			// 이 아래는 없는 효과들
			.put(PotionEffectType.WITHER, Pair.of("§0시듦", Color.fromRGB(1, 1, 1)))
			.put(PotionEffectType.BLINDNESS, Pair.of("§7실명", Color.fromRGB(140, 140, 140)))
			.put(PotionEffectType.CONFUSION, Pair.of("§5멀미", Color.fromRGB(171, 130, 18)))
			.put(PotionEffectType.GLOWING, Pair.of("§f발광", Color.fromRGB(254, 254, 254)))
			.put(PotionEffectType.HUNGER, Pair.of("§2허기", Color.fromRGB(134, 229, 127)))
			.put(PotionEffectType.LEVITATION, Pair.of("§5공중 부양", Color.fromRGB(171, 18, 151)))
			.put(PotionEffectType.SLOW_DIGGING, Pair.of("§8채굴 피로", Color.fromRGB(93, 93, 93)))
			.put(PotionEffectType.UNLUCK, Pair.of("§a불운", Color.fromRGB(206, 242, 121)))
			.build();
	
	private static final ImmutableMap<PotionEffectType, Integer> POTION_DURATION
	= ImmutableMap.<PotionEffectType, Integer>builder()
			.put(PotionEffectType.POISON, POTION_POISON_DURATION.getValue())
			.put(PotionEffectType.WEAKNESS, POTION_WEAKNESS_DURATION.getValue())
			.put(PotionEffectType.SLOW, POTION_SLOWNESS_DURATION.getValue())
			.put(PotionEffectType.HARM, 1)
			// 이 아래는 없는 효과들
			.put(PotionEffectType.WITHER, POTION_WITHER_DURATION.getValue())
			.put(PotionEffectType.BLINDNESS, POTION_BLINDNESS_DURATION.getValue())
			.put(PotionEffectType.CONFUSION, POTION_CONFUSION_DURATION.getValue())
			.put(PotionEffectType.GLOWING, POTION_GLOWING_DURATION.getValue())
			.put(PotionEffectType.HUNGER, POTION_HUNGER_DURATION.getValue())
			.put(PotionEffectType.LEVITATION, POTION_LEVITATION_DURATION.getValue())
			.put(PotionEffectType.SLOW_DIGGING, POTION_SLOW_DIGGING_DURATION.getValue())
			.put(PotionEffectType.UNLUCK, POTION_UNLUCK_DURATION.getValue())
			.build();
		
	
	private static final ImmutableMap<PotionEffectType, PotionEffectType> POTION_REVERSE 
	= ImmutableMap.<PotionEffectType, PotionEffectType>builder()
			.put(PotionEffectType.POISON, PotionEffectType.REGENERATION)
			.put(PotionEffectType.WEAKNESS, PotionEffectType.INCREASE_DAMAGE)
			.put(PotionEffectType.SLOW, PotionEffectType.SPEED)
			.put(PotionEffectType.WITHER, PotionEffectType.ABSORPTION)
			.put(PotionEffectType.BLINDNESS, PotionEffectType.NIGHT_VISION)
			.put(PotionEffectType.CONFUSION, PotionEffectType.DAMAGE_RESISTANCE)
			.put(PotionEffectType.GLOWING, PotionEffectType.INVISIBILITY)
			.put(PotionEffectType.HUNGER, PotionEffectType.SATURATION)
			.put(PotionEffectType.LEVITATION, PotionEffectType.JUMP)
			.put(PotionEffectType.SLOW_DIGGING, PotionEffectType.FAST_DIGGING)
			.put(PotionEffectType.UNLUCK, PotionEffectType.LUCK)
			.build();

	protected void onUpdate(AbilityBase.Update update) {
	    if (update == AbilityBase.Update.RESTRICTION_CLEAR) {
	    	restock.start();
	    	passive.start();
	    }
	}
	
    private final SlotMachineTimer slot = new SlotMachineTimer();
	private final double slotadddamage = SLOT_ADD_DAMAGE.getValue();
	private final double slotreducedamage = SLOT_REDUCE_DAMAGE.getValue();
	private final double slotheal = SLOT_HEAL.getValue();
	private final int restocktimer = (int) Math.ceil(Wreck.isEnabled(GameManager.getGame()) ? Wreck.calculateDecreasedAmount(30) * RESTOCK.getValue() : RESTOCK.getValue());
	private final Cooldown slotcooldown = new Cooldown(SLOT_COOLDOWN.getValue(), "슬롯머신");
	private PotionEffectType offhandPotion;
	private static final Set<Material> bows;
	private static final Circle circle = Circle.of(10, 70);
	private final RGB purple = RGB.of(171, 18, 151);
	
	private int effectduration;
	private EffectRegistration<?> effecttype;
	
	static {
		if (MaterialX.CROSSBOW.isSupported()) {
			bows = ImmutableSet.of(MaterialX.BOW.getMaterial(), MaterialX.CROSSBOW.getMaterial());
		} else {
			bows = ImmutableSet.of(MaterialX.BOW.getMaterial());
		}
	}
	
	private static boolean isBucket(final Material material) {
		return material.name().contains("BUCKET");
	}
	
	private static boolean isApple(final Material material) {
		return material.name().contains("APPLE");
	}
	
	private static boolean isPotion(final Material material) {
		return material.name().contains("POTION");
	}
	
	private final AbilityTimer passive = new AbilityTimer() {
	
		@Override
		public void run(int count) {
			for (PotionEffect pe : getPlayer().getActivePotionEffects()) {
				if (POTION_TYPES_BAD.containsKey(pe.getType())) {
					PotionEffect newpe = new PotionEffect(POTION_REVERSE.get(pe.getType()), pe.getDuration(), pe.getAmplifier(), false, true);
					getPlayer().sendMessage("§6[§a!§e] " + POTION_TYPES_BAD.get(pe.getType()).getLeft() + "§f 효과를 교체시켜 " + POTION_TYPES_GOOD.get(newpe.getType()).getLeft() + "§f" + KoreanUtil.getJosa(POTION_TYPES_GOOD.get(newpe.getType()).getLeft(), Josa.이가) + " 되었습니다.");
					getPlayer().removePotionEffect(pe.getType());
					getPlayer().addPotionEffect(newpe);
				}	
			}
    		if (count % 2 == 0) {
				for (Location loc : circle.toLocations(getPlayer().getLocation()).floor(getPlayer().getLocation().getY())) {
					ParticleLib.REDSTONE.spawnParticle(getPlayer(), loc, purple);
				}
    		}
		}
		
	}.setPeriod(TimeUnit.TICKS, 1).register();
	
	private final AbilityTimer restock = new AbilityTimer(restocktimer * 20) {
		
    	@Override
		public void onEnd() {
    		ItemStack item = new ItemStack(Material.SPLASH_POTION, 1);
    		PotionMeta meta = (PotionMeta) item.getItemMeta();
    		
    		Random random = new Random();
    		offhandPotion = new ArrayList<>(POTION_TYPES_BAD.keySet()).get(random.nextInt(POTION_TYPES_BAD.size()));
    		meta.setBasePotionData(new PotionData(PotionType.INSTANT_DAMAGE));
    		meta.addCustomEffect(new PotionEffect(offhandPotion, POTION_DURATION.get(offhandPotion) * 20, 1), true);
    		meta.addCustomEffect(new PotionEffect(PotionEffectType.HARM, 1, 1), true);
    		meta.setColor(POTION_TYPES_BAD.get(offhandPotion).getRight());    		
    		effecttype = random.pick(EffectRegistry.values().toArray(new EffectRegistry.EffectRegistration[0]));
			effectduration = (random.nextInt(15) + 1) * 20;
			meta.setDisplayName("§f투척용 " + effecttype.getManifest().displayName() + "§f" + KoreanUtil.getJosa(effecttype.getManifest().displayName(), Josa.과와) + " " + POTION_TYPES_BAD.get(offhandPotion).getLeft() + "§f 및 §4고통§f의 포션");
			@SuppressWarnings("serial")
			List<String> lores = new ArrayList<String>() {
				{
					add("§f" + effecttype.getManifest().displayName() + " (" + (effectduration / 1200) + ":" + ((effectduration / 20) % 60) + ") ");
				}
			};
			
			getPlayer().sendMessage("§6[§a!§e] §f투척용 " + effecttype.getManifest().displayName() + "§f" + KoreanUtil.getJosa(effecttype.getManifest().displayName(), Josa.과와) + " " + POTION_TYPES_BAD.get(offhandPotion).getLeft() + "§f 및 §4고통§f의 포션을 만들어냈습니다.");
			meta.setLore(lores);
    		
    		item.setItemMeta(meta);
    		
    		getPlayer().getInventory().setItemInOffHand(item);
    		SoundLib.ITEM_BOTTLE_FILL.playSound(getPlayer().getLocation(), 1, 1.2f);
    		ParticleLib.SPELL_MOB.spawnParticle(getPlayer().getLocation().clone().add(0, 0.5, 0), 0.15, 0.5, 0.15, 15, 1);
    	}
    	
    	@Override
    	public void onSilentEnd() {
    	}
		
	}.setPeriod(TimeUnit.TICKS, 1).register();
	
	public boolean ActiveSkill(Material material, ClickType clickType) {
		if (material == Material.IRON_INGOT && clickType == ClickType.LEFT_CLICK && !slot.isRunning() && !slotcooldown.isCooldown()) {
			return slot.start();
		}
	    return false;
	}
	
	@SubscribeEvent
	public void onEntityDamageByEntity(EntityDamageByEntityEvent e) {
		if (e.getDamager() instanceof Projectile) {
			Projectile projectile = (Projectile) e.getDamager();
			if (projectile.getType().equals(EntityType.SPLASH_POTION) && getPlayer().equals(projectile.getShooter()) 
					&& e.getEntity() instanceof Player) {
				Player player = (Player) e.getEntity();
				Participant p = getGame().getParticipant(player);
				if (!getPlayer().equals(player)) {
					if (effecttype != null) {
						if (effecttype.equals(Agro.registration)) Agro.apply(p, TimeUnit.TICKS, effectduration, getPlayer(), 2);
						else if (effecttype.equals(Charm.registration)) Charm.apply(p, TimeUnit.TICKS, effectduration, getPlayer(), 55, 35);
						else if (effecttype.equals(Confusion.registration)) Confusion.apply(p, TimeUnit.TICKS, effectduration, 10);
						else if (effecttype.equals(Madness.registration)) Madness.apply(p, TimeUnit.TICKS, effectduration, 10);
						else if (effecttype.equals(SnowflakeMark.registration)) SnowflakeMark.apply(p, TimeUnit.TICKS, effectduration, 1);
						else effecttype.apply(p, TimeUnit.TICKS, effectduration);		
					}
				}
			}
		}
	}
	
	@SubscribeEvent(onlyRelevant = true)
	public void onPlayerSwapHand(PlayerSwapHandItemsEvent e) {
		e.setCancelled(true);
	}
	
	@SubscribeEvent
	public void onInventoryClick(InventoryClickEvent e) {
		if (e.getWhoClicked().equals(getPlayer())) {
			if (e.getSlot() == 40) {
				e.setCancelled(true);
			}
		}
	}
	
	@SubscribeEvent
	private void onProjectileLaunch(ProjectileLaunchEvent e) {
		if (getPlayer().equals(e.getEntity().getShooter()) && e.getEntity() instanceof SplashPotion) {
			new Homing(e.getEntity(), e.getEntity().getVelocity().length()).start();
		}
	}
	
	private static Method SET_DATA = null;
	
	static {
		if (ServerVersion.getVersion() < 13) {
			try {
				SET_DATA = Block.class.getDeclaredMethod("setData", byte.class);
			} catch (NoSuchMethodException ignored) {
			}
		}
	}
	
	@SubscribeEvent(onlyRelevant = true)
	private void onPlayerInteract(PlayerInteractEvent e) {
		if ((e.getAction() == Action.RIGHT_CLICK_BLOCK || e.getAction() == Action.RIGHT_CLICK_AIR)) {
			if (getPlayer().getInventory().getItemInOffHand().getType().equals(Material.SPLASH_POTION)) {
				if (getPlayer().getInventory().getItemInMainHand().getType().isBlock()) {
					if (e.getAction() != Action.RIGHT_CLICK_BLOCK) {
						if (LocationUtil.getNearbyEntities(Player.class, getPlayer().getLocation(), 10, 10, predicate).size() >= 1) {
							for (Location loc : circle.toLocations(getPlayer().getLocation()).floor(getPlayer().getLocation().getY())) {
								ParticleLib.SPELL_WITCH.spawnParticle(loc);
							}
						    restock.start();
						} else e.setCancelled(true);
					} else {
						e.setCancelled(true);
						ItemStack item =  getPlayer().getInventory().getItemInMainHand();
						Block air = getPlayer().getWorld().getBlockAt(e.getClickedBlock().getRelative(e.getBlockFace()).getLocation());
						Material block = item.getType();
						air.setType(block);
						short data = item.getDurability();
						BlockX.setDirection(air, e.getBlockFace());
						if (ServerVersion.getVersion() < 13 && data != 0) {
							try {
								SET_DATA.invoke(air, (byte) data);
							} catch (IllegalAccessException | InvocationTargetException ignored) {
							}
						}
						if (!getPlayer().getGameMode().equals(GameMode.CREATIVE)) {
							item.setAmount(item.getAmount() - 1);
						}
					}
				} else if (!bows.contains(getPlayer().getInventory().getItemInMainHand().getType()) &&
						!getPlayer().getInventory().getItemInMainHand().getType().equals(Material.ENDER_PEARL) &&
						!isApple(getPlayer().getInventory().getItemInMainHand().getType()) &&
						!isPotion(getPlayer().getInventory().getItemInMainHand().getType()) &&
						!isBucket(getPlayer().getInventory().getItemInMainHand().getType())) {
					if (LocationUtil.getNearbyEntities(Player.class, getPlayer().getLocation(), 10, 10, predicate).size() >= 1) {
						for (Location loc : circle.toLocations(getPlayer().getLocation()).floor(getPlayer().getLocation().getY())) {
							ParticleLib.SPELL_WITCH.spawnParticle(loc);
						}
					    restock.start();
					} else e.setCancelled(true);
				}	
			}
		}
	}
	
	public class Homing extends AbilityTimer implements Listener {
		
		private Projectile projectile;
		private double lengths;
		
		private Homing(Projectile projectile, Double length) {
			super(TaskType.REVERSE, 150);
			setPeriod(TimeUnit.TICKS, 1);
			this.projectile = projectile;
			this.lengths = length;
		}
		
		@Override
		protected void onStart() {
			Bukkit.getPluginManager().registerEvents(this, AbilityWar.getPlugin());
		}
		
		@Override
		protected void run(int arg0) {
			if (projectile != null) {
				Damageable d = LocationUtil.getNearestEntity(Damageable.class, projectile.getLocation(), predicate);
				if (d != null) {
					if (projectile.getLocation().distanceSquared(d.getLocation().clone().add(0, 1, 0)) <= 5) {
						projectile.setGravity(false);
						projectile.setVelocity(VectorUtil.validateVector((d.getLocation().clone().add(0, 1, 0).toVector()
									.subtract(projectile.getLocation().toVector())).normalize().multiply(lengths)));	
					} else {
						projectile.setGravity(true);
					}
				}
			}
		}
		
		@EventHandler
		public void onProjectileHit(ProjectileHitEvent e) {
			if (e.getEntity().equals(projectile)) {
				stop(false);
			}
		}
		
		@Override
		protected void onEnd() {
			HandlerList.unregisterAll(this);
			projectile.setGravity(true);
		}
		
		@Override
		protected void onSilentEnd() {
			onEnd();
		}
	}
	
    private class SlotMachineTimer extends AbilityTimer implements Listener {
        
    	private final Map<String, Integer> results = new HashMap<>();
    	private final Random random = new Random();
    	private final SlotDuration duration = new SlotDuration();
    	
    	public boolean isRunning() {
    		return super.isRunning() || duration.isRunning();
        }

        public SlotMachineTimer() {
            super(TaskType.NORMAL, 3);
            setPeriod(TimeUnit.TICKS, 10);
        }
        
        private StringJoiner joiner;
        
        public void onStart() {
            results.clear();
            results.put("§c<코>", 0);
            results.put("§6<크>", 0);
            results.put("§e<스>", 0);
            joiner = new StringJoiner("§f ");
        }

        public void run(int count) {
            String get = random.pick(results.keySet().toArray(new String[0]));
            joiner.add(get);
            results.put(get, results.get(get) + 1);
            NMS.sendTitle(getPlayer(), "", joiner.toString(), 0, 20, 0);
            SoundLib.ENTITY_ARROW_HIT_PLAYER.playSound(getPlayer());
        }

        public void onSilentEnd() {
            NMS.clearTitle(getPlayer());
        }

        public void onEnd() {
            duration.start();
            NMS.clearTitle(getPlayer());

            if (results.get("§c<코>") == 1 && results.get("§6<크>") == 1 && results.get("§e<스>") == 1) {
                getPlayer().sendMessage("§c[§e!§c] §c화§6려§e하§a게 §b터§9진§5다§d!");
                
                results.put("§c<코>", 3);
                results.put("§6<크>", 3);
                results.put("§e<스>", 3);

                new AbilityTimer(TaskType.NORMAL, 5) {
                    public void run(int count) {
                        switch (count) {
                            case 1: {
                                NMS.sendTitle(getPlayer(), "", "§b<C> §c<O> §6<K> §e<E> §a<S>", 0, 20, 0);
                                break;
                            }
                            case 2: {
                                NMS.sendTitle(getPlayer(), "", "§a<C> §b<O> §c<K> §6<E> §e<S>", 0, 20, 0);
                                break;
                            }
                            case 3: {
                                NMS.sendTitle(getPlayer(), "", "§e<C> §a<O> §b<K> §c<E> §6<S>", 0, 20, 0);
                                break;
                            }
                            case 4: {
                                NMS.sendTitle(getPlayer(), "", "§6<C> §e<O> §a<K> §b<E> §c<S>", 0, 20, 0);
                                break;
                            }
                            case 5: {
                                NMS.sendTitle(getPlayer(), "", "§c<C> §6<O> §e<K> §a<E> §b<S>", 0, 20, 0);
                                break;
                            }
                        }
                        SoundLib.PIANO.playInstrument(getPlayer(), Note.natural(0, Tone.C));
						SoundLib.PIANO.playInstrument(getPlayer(), Note.natural(0, Tone.E));
                    }

                    public void onSilentEnd() {
                        NMS.clearTitle(getPlayer());
                    }

                    public void onEnd() {
                        onSilentEnd();
                    }
                    
                }.setPeriod(TimeUnit.TICKS,4).start();
            }

            if (results.get("§c<코>") != 0) {
                getPlayer().sendMessage("주는 대미지 §c" + results.get("§c<코>") * slotadddamage + "§f 증가");
            }
            if (results.get("§6<크>") != 0) {
                getPlayer().sendMessage("받는 대미지 §3" + results.get("§6<크>") * slotreducedamage + "§f 감소");
            }
            if (results.get("§e<스>") != 0) {
                getPlayer().sendMessage("초당 HP §d" + results.get("§e<스>") * slotheal + "§f 회복");
            }

            SoundLib.ENTITY_PLAYER_LEVELUP.playSound(getPlayer());
        }

        class SlotDuration extends Duration {

            public SlotDuration() {
                super(SLOT_DURATION.getValue(), slotcooldown, "슬롯머신");
            }

            public void onDurationStart() {
                Bukkit.getPluginManager().registerEvents(SlotMachineTimer.this, AbilityWar.getPlugin());
            }

            @Override
            protected void onDurationProcess(int i) { 
				final EntityRegainHealthEvent event = new EntityRegainHealthEvent(getPlayer(), results.get("§e<스>") * slotheal, RegainReason.CUSTOM);
				Bukkit.getPluginManager().callEvent(event);
				if (!event.isCancelled()) {
					Healths.setHealth(getPlayer(), getPlayer().getHealth() + results.get("§e<스>") * slotheal);
				}
            }

            public void onDurationEnd() {
                HandlerList.unregisterAll(SlotMachineTimer.this);
            }

            public void onDurationSilentEnd() {
                HandlerList.unregisterAll(SlotMachineTimer.this);
            }
        }
        
        @EventHandler
        public void onEntityDamageByEntity(EntityDamageByEntityEvent e) {
            if (e.getEntity().equals(getPlayer())) {
                e.setDamage(e.getDamage() - (results.get("§6<크>") * slotreducedamage));
            }

            Entity attacker = e.getDamager();
            if (attacker instanceof Projectile) {
                Projectile projectile = (Projectile) attacker;
                if (projectile.getShooter() != null) {
                    if (projectile.getShooter() instanceof Entity) {
                        attacker = (Entity) projectile.getShooter();
                    }	
                }
            }

            if (attacker.equals(getPlayer())) {
                e.setDamage(e.getDamage() + (results.get("§c<코>") * slotadddamage));
            }
        }
    }
}
