package RainStarAbility;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.annotation.Nullable;

import org.bukkit.Color;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.SplashPotion;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionData;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.potion.PotionType;

import RainStarEffect.Addiction;
import RainStarEffect.Agro;
import RainStarEffect.Charm;
import RainStarEffect.Confusion;
import RainStarEffect.Madness;
import RainStarEffect.SnowflakeMark;
import daybreak.abilitywar.ability.AbilityBase;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.ability.decorator.ActiveHandler;
import daybreak.abilitywar.config.ability.AbilitySettings.SettingObject;
import daybreak.abilitywar.ability.SubscribeEvent;
import daybreak.abilitywar.game.GameManager;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.manager.effect.Fear;
import daybreak.abilitywar.game.manager.effect.event.ParticipantEffectApplyEvent;
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
import daybreak.abilitywar.utils.base.math.geometry.Circle;
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
@AbilityManifest(name = "시트러스", rank = Rank.S, species = Species.HUMAN, explain = {
		"§a풀 속성§f의 미스터리 연금술사, 시트러스.",
		"§7철괴 좌클릭 §8- §2엘릭서§f: §a긍정 포션 효과§f의 계수를 §e1단계§f 높여서 받습니다.",
		" 이 효과로 강화된 수에 비례해 다음 쿨타임이 감소합니다. $[COOLDOWN]",
		"§7패시브 §8- §3케미컬 쓰로워§f: 기본적으로 비주류 손을 사용하지 못합니다.",
		" 비주류 손에 §a$[RESTOCK]§f초마다 재충전되는 §c부정 투척형 포션§f이 생깁니다.",
		" 10칸 내에 플레이어가 있다면 해당 포션을 멀리 던질 수 있습니다.",
		" 또한 10칸 내 대상이 4개 이상의 포션 효과를 갖고 있다면 §5포션 중독§f을 일으켜",
		" 대상이 가진 §c부정 계열 포션 효과§f를 7초간 §e1단계§f 높입니다.",
		"§7패시브 §8- §5케미컬 콜랩스§f: 비주류 손에 §4즉시 피해 포션§f이 있을 경우에",
		" 상태이상 효과를 받는다면 무시하고 포션에 해당 상태이상을 주입합니다.",
		" §c부정 포션 효과§f를 받으면 상반되는 §a긍정 효과§f로 대체됩니다."
		}, summarize = {
		"§7철괴 좌클릭으로 §f받고 있는 §a긍정 포션효과§f를 강화하고 §c부정 포션효과§f를 교체합니다.",
		"계속 재충전되는 §c부정형 투척 포션§f은 다른 플레이어가 10칸 내에 있어야 던질 수 있습니다.",
		"비주류 손에 §4즉시 피해 포션§f이 있으면 상태이상을 받을 때 포션에 주입합니다."
		})

public class Citrus extends AbilityBase implements ActiveHandler {

	public Citrus(Participant participant) {
		super(participant);
	}
	
	public static final SettingObject<Integer> RESTOCK = 
			abilitySettings.new SettingObject<Integer>(Citrus.class, "restock", 13,
			"# 포션 재충전 시간",
			"# 쿨타임 감소 효과를 40%까지 받습니다.") {

		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}

	};
	
	public static final SettingObject<Integer> COOLDOWN 
	= abilitySettings.new SettingObject<Integer>(Citrus.class,
			"cooldown", 75, "# 쿨타임") {
		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}

		@Override
		public String toString() {
			return Formatter.formatCooldown(getValue());
		}
	};
	
	public static final SettingObject<Integer> POISON_DURATION = 
			abilitySettings.new SettingObject<Integer>(Citrus.class, "poison-duration", 14,
			"# 독 지속시간 (단위: 초)") {

		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}

	};
	
	public static final SettingObject<Integer> WEAKNESS_DURATION = 
			abilitySettings.new SettingObject<Integer>(Citrus.class, "weakness-duration", 17,
			"# 나약함 지속시간 (단위: 초)") {

		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}

	};
	
	public static final SettingObject<Integer> SLOWNESS_DURATION = 
			abilitySettings.new SettingObject<Integer>(Citrus.class, "slowness-duration", 23,
			"# 구속 지속시간 (단위: 초)") {

		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}

	};

	public static final SettingObject<Integer> WITHER_DURATION = 
			abilitySettings.new SettingObject<Integer>(Citrus.class, "wither-duration", 19,
			"# 시듦 지속시간 (단위: 초)") {

		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}

	};
	
	public static final SettingObject<Integer> BLINDNESS_DURATION = 
			abilitySettings.new SettingObject<Integer>(Citrus.class, "blind-duration", 12,
			"# 실명 지속시간 (단위: 초)") {

		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}

	};
	
	public static final SettingObject<Integer> CONFUSION_DURATION = 
			abilitySettings.new SettingObject<Integer>(Citrus.class, "confusion-duration", 14,
			"# 멀미 지속시간 (단위: 초)") {

		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}

	};
	
	public static final SettingObject<Integer> GLOWING_DURATION = 
			abilitySettings.new SettingObject<Integer>(Citrus.class, "glowing-duration", 90,
			"# 발광 지속시간 (단위: 초)") {

		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}

	};
	
	public static final SettingObject<Integer> HUNGER_DURATION = 
			abilitySettings.new SettingObject<Integer>(Citrus.class, "hunger-duration", 120,
			"# 허기 지속시간 (단위: 초)") {

		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}

	};
	
	public static final SettingObject<Integer> LEVITATION_DURATION = 
			abilitySettings.new SettingObject<Integer>(Citrus.class, "levitation-duration", 5,
			"# 공중 부양 지속시간 (단위: 초)") {

		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}

	};
	
	public static final SettingObject<Integer> SLOW_DIGGING_DURATION = 
			abilitySettings.new SettingObject<Integer>(Citrus.class, "slow-digging-duration", 30,
			"# 채굴 피로 지속시간 (단위: 초)") {

		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}

	};
	
	public static final SettingObject<Integer> UNLUCK_DURATION = 
			abilitySettings.new SettingObject<Integer>(Citrus.class, "unluck-duration", 300,
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
			.put(PotionEffectType.HEAL, Pair.of("§d치유", PotionEffectType.HEAL.getColor()))
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
			.put(PotionEffectType.HARM, Pair.of("§4고통", PotionEffectType.HARM.getColor()))
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
			.put(PotionEffectType.POISON, POISON_DURATION.getValue())
			.put(PotionEffectType.WEAKNESS, WEAKNESS_DURATION.getValue())
			.put(PotionEffectType.SLOW, SLOWNESS_DURATION.getValue())
			.put(PotionEffectType.HARM, 1)
			// 이 아래는 없는 효과들
			.put(PotionEffectType.WITHER, WITHER_DURATION.getValue())
			.put(PotionEffectType.BLINDNESS, BLINDNESS_DURATION.getValue())
			.put(PotionEffectType.CONFUSION, CONFUSION_DURATION.getValue())
			.put(PotionEffectType.GLOWING, GLOWING_DURATION.getValue())
			.put(PotionEffectType.HUNGER, HUNGER_DURATION.getValue())
			.put(PotionEffectType.LEVITATION, LEVITATION_DURATION.getValue())
			.put(PotionEffectType.SLOW_DIGGING, SLOW_DIGGING_DURATION.getValue())
			.put(PotionEffectType.UNLUCK, UNLUCK_DURATION.getValue())
			.build();
		
	
	private static final ImmutableMap<PotionEffectType, PotionEffectType> POTION_REVERSE 
	= ImmutableMap.<PotionEffectType, PotionEffectType>builder()
			.put(PotionEffectType.POISON, PotionEffectType.REGENERATION)
			.put(PotionEffectType.WEAKNESS, PotionEffectType.INCREASE_DAMAGE)
			.put(PotionEffectType.SLOW, PotionEffectType.SPEED)
			.put(PotionEffectType.HARM, PotionEffectType.HEAL)
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
	
	private int messagestack = 0;
	private final int restocktimer = (int) Math.ceil(Wreck.isEnabled(GameManager.getGame()) ? Wreck.calculateDecreasedAmount(40) * RESTOCK.getValue() : RESTOCK.getValue());
	private final Cooldown cooldown = new Cooldown(COOLDOWN.getValue());
	private boolean converted = false;
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
	
	private AbilityTimer restock = new AbilityTimer(restocktimer * 20) {
		
    	@Override
		public void onEnd() {
    		converted = false;
    		ItemStack item = new ItemStack(Material.SPLASH_POTION, 1);
    		PotionMeta meta = (PotionMeta) item.getItemMeta();
    		
    		Random random = new Random();
    		offhandPotion = new ArrayList<>(POTION_TYPES_BAD.keySet()).get(random.nextInt(POTION_TYPES_BAD.size()));
    		if (offhandPotion.equals(PotionEffectType.HARM)) meta.setBasePotionData(new PotionData(PotionType.INSTANT_DAMAGE));
    		meta.addCustomEffect(new PotionEffect(offhandPotion, POTION_DURATION.get(offhandPotion) * 20, 1), true);
    		meta.setColor(POTION_TYPES_BAD.get(offhandPotion).getRight());
    		meta.setDisplayName("§f투척용 " + POTION_TYPES_BAD.get(offhandPotion).getLeft() + "§f의 포션");
    		
    		getPlayer().sendMessage("§6[§a!§e] §f투척용 " + POTION_TYPES_BAD.get(offhandPotion).getLeft() + "§f의 포션을 만들어냈습니다.");
    		
    		item.setItemMeta(meta);
    		
    		getPlayer().getInventory().setItemInOffHand(item);
    		SoundLib.ITEM_BOTTLE_FILL.playSound(getPlayer().getLocation(), 1, 1.2f);
    		ParticleLib.SPELL_MOB.spawnParticle(getPlayer().getLocation().clone().add(0, 0.5, 0), 0.15, 0.5, 0.15, 15, 1);
    	}
    	
    	@Override
    	public void onSilentEnd() {
    	}
		
	}.setPeriod(TimeUnit.TICKS, 1).register();
	
	public boolean ActiveSkill(Material material, ClickType clicktype) {
	    if (material.equals(Material.IRON_INGOT) && clicktype.equals(ClickType.LEFT_CLICK) && !cooldown.isCooldown()) {
	    	int changedcounter = 0;
	    	for (PotionEffect pe : getPlayer().getActivePotionEffects()) {
    			if (POTION_TYPES_GOOD.containsKey(pe.getType())) {
    				PotionEffect newpe = new PotionEffect(pe.getType(), pe.getDuration(), pe.getAmplifier() + 1, false, true);
    				getPlayer().sendMessage("§6[§a!§e] " + POTION_TYPES_GOOD.get(pe.getType()).getLeft() + "§f 효과를 강화시켜 계수가 §e" + (newpe.getAmplifier() + 1) + "§f" + KoreanUtil.getJosa("" + newpe.getAmplifier() + 1, Josa.이가) + " 되었습니다.");
    				getPlayer().removePotionEffect(pe.getType());
    				getPlayer().addPotionEffect(newpe);
    				changedcounter++;
    			}
    		}
			SoundLib.BLOCK_BREWING_STAND_BREW.playSound(getPlayer().getLocation(), 1, 0.75f);
	    	cooldown.start();
	    	cooldown.setCount(Math.max(0, cooldown.getCount() - Math.min(changedcounter, (cooldown.getCount() / 2))));
	    	if (restock.isRunning()) restock.setCount(Math.max(0, restock.getCount() - changedcounter));
	    	return true;
	    }
	    return false;
	}
	
	@SubscribeEvent(onlyRelevant = true)
	public void onParticipantEffectApply(ParticipantEffectApplyEvent e) {
		if (getPlayer().getInventory().getItemInOffHand().getType().equals(Material.SPLASH_POTION)) {
			ItemStack item = getPlayer().getInventory().getItemInOffHand();
			PotionMeta meta = (PotionMeta) item.getItemMeta();
			PotionType type = meta.getBasePotionData().getType();
			if (type.equals(PotionType.INSTANT_DAMAGE) && !converted) {
				effecttype = e.getEffectType();
				effectduration = e.getDuration();
				meta.setDisplayName("§f투척용 " + effecttype.getManifest().displayName() + "§f" + KoreanUtil.getJosa(effecttype.getManifest().displayName(), Josa.과와) + " §4고통§f의 포션");
				@SuppressWarnings("serial")
				List<String> lores = new ArrayList<String>() {
					{
						add("§f" + effecttype.getManifest().displayName() + " (" + (effectduration / 1200) + ":" + ((effectduration / 20) % 60) + ") ");
					}
				};
				meta.setLore(lores);
				item.setItemMeta(meta);
				converted = true;
				getPlayer().sendMessage("§6[§a!§e] 포션 효과에 " + effecttype.getManifest().displayName() + "§f" + KoreanUtil.getJosa(effecttype.getManifest().displayName(), Josa.을를) + " 추가했습니다.");
				e.setCancelled(true);
			}
		}
	}
	
	@SubscribeEvent
	public void onEntityDamageByEntity(EntityDamageByEntityEvent e) {
		if (e.getDamager() instanceof Projectile) {
			Projectile projectile = (Projectile) e.getDamager();
			if (projectile.getType().equals(EntityType.SPLASH_POTION) && getPlayer().equals(projectile.getShooter()) 
					&& converted && e.getEntity() instanceof Player) {
				Player player = (Player) e.getEntity();
				Participant p = getGame().getParticipant(player);
				if (!getPlayer().equals(player)) {
					if (effecttype.equals(Agro.registration)) Agro.apply(p, TimeUnit.TICKS, effectduration, getPlayer(), 2);
					else if (effecttype.equals(Charm.registration)) Charm.apply(p, TimeUnit.TICKS, effectduration, getPlayer(), 55, 35);
					else if (effecttype.equals(Confusion.registration)) Confusion.apply(p, TimeUnit.TICKS, effectduration, 10);
					else if (effecttype.equals(Madness.registration)) Madness.apply(p, TimeUnit.TICKS, effectduration, 10);
					else if (effecttype.equals(SnowflakeMark.registration)) SnowflakeMark.apply(p, TimeUnit.TICKS, effectduration, 1);
					else if (effecttype.equals(Fear.registration)) Fear.apply(p, TimeUnit.TICKS, effectduration, getPlayer());
					else effecttype.apply(p, TimeUnit.TICKS, effectduration);	
				}
			}
		}
	}
	
	@SubscribeEvent
	private void onProjectileLaunch(ProjectileLaunchEvent e) {
		if (getPlayer().equals(e.getEntity().getShooter()) && e.getEntity() instanceof SplashPotion) {
			e.getEntity().setVelocity(e.getEntity().getVelocity().multiply(1.35));
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
							for (Player target : LocationUtil.getNearbyEntities(Player.class, getPlayer().getLocation(), 10, 10, predicate)) {
								if (target.getPlayer().getActivePotionEffects().size() >= 4) Addiction.apply(getGame().getParticipant(target), TimeUnit.SECONDS, 7);
								for (Location loc : circle.toLocations(getPlayer().getLocation()).floor(getPlayer().getLocation().getY())) {
									ParticleLib.SPELL_WITCH.spawnParticle(loc);
								}
								restock.start();
							}
						} else {
							messagestack++;
							e.setCancelled(true);
							if (messagestack % 5 == 0) getPlayer().sendMessage("§4[§c!§4] §f원 안에 플레이어가 있어야 포션을 던질 수 있습니다.");
						}
					} else {
						e.setCancelled(true);
						ItemStack item =  getPlayer().getInventory().getItemInMainHand();
						Block air = getPlayer().getWorld().getBlockAt(e.getClickedBlock().getRelative(e.getBlockFace()).getLocation());
						Material block = item.getType();
						if (air.isEmpty() || air.isLiquid()) {
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
					}
				} else if (isBucket(getPlayer().getInventory().getItemInMainHand().getType())) {
					if (e.getAction() != Action.RIGHT_CLICK_BLOCK) {
						if (LocationUtil.getNearbyEntities(Player.class, getPlayer().getLocation(), 10, 10, predicate).size() >= 1) {
							for (Player target : LocationUtil.getNearbyEntities(Player.class, getPlayer().getLocation(), 10, 10, predicate)) {
								if (target.getPlayer().getActivePotionEffects().size() >= 4) Addiction.apply(getGame().getParticipant(target), TimeUnit.SECONDS, 7);
								for (Location loc : circle.toLocations(getPlayer().getLocation()).floor(getPlayer().getLocation().getY())) {
									ParticleLib.SPELL_WITCH.spawnParticle(loc);
								}
								restock.start();
							}
						} else {
							messagestack++;
							e.setCancelled(true);
							if (messagestack % 5 == 0) getPlayer().sendMessage("§4[§c!§4] §f원 안에 플레이어가 있어야 포션을 던질 수 있습니다.");
						}
					} else if (getPlayer().getInventory().getItemInMainHand().getType().equals(Material.BUCKET)) {
						Block block = getPlayer().getWorld().getBlockAt(e.getClickedBlock().getRelative(e.getBlockFace()).getLocation());
						boolean returnwhat = false;
						if (block.isLiquid() && block.getData() == (byte) 0) {
							returnwhat = true;
						}
						if (!returnwhat) {
							if (LocationUtil.getNearbyEntities(Player.class, getPlayer().getLocation(), 10, 10, predicate).size() >= 1) {
								for (Player target : LocationUtil.getNearbyEntities(Player.class, getPlayer().getLocation(), 10, 10, predicate)) {
									if (target.getPlayer().getActivePotionEffects().size() >= 4) Addiction.apply(getGame().getParticipant(target), TimeUnit.SECONDS, 7);
									for (Location loc : circle.toLocations(getPlayer().getLocation()).floor(getPlayer().getLocation().getY())) {
										ParticleLib.SPELL_WITCH.spawnParticle(loc);
									}
									restock.start();
								}
							} else {
								messagestack++;
								e.setCancelled(true);
								if (messagestack % 5 == 0) getPlayer().sendMessage("§4[§c!§4] §f원 안에 플레이어가 있어야 포션을 던질 수 있습니다.");
							}
						}
					}
				} else if (!bows.contains(getPlayer().getInventory().getItemInMainHand().getType()) &&
						!getPlayer().getInventory().getItemInMainHand().getType().equals(Material.ENDER_PEARL) &&
						!isApple(getPlayer().getInventory().getItemInMainHand().getType()) &&
						!isPotion(getPlayer().getInventory().getItemInMainHand().getType())) {
					if (LocationUtil.getNearbyEntities(Player.class, getPlayer().getLocation(), 10, 10, predicate).size() >= 1) {
						for (Player target : LocationUtil.getNearbyEntities(Player.class, getPlayer().getLocation(), 10, 10, predicate)) {
							if (target.getPlayer().getActivePotionEffects().size() >= 4) Addiction.apply(getGame().getParticipant(target), TimeUnit.SECONDS, 7);
							for (Location loc : circle.toLocations(getPlayer().getLocation()).floor(getPlayer().getLocation().getY())) {
								ParticleLib.SPELL_WITCH.spawnParticle(loc);
							}
							restock.start();
						}
					} else {
						messagestack++;
						e.setCancelled(true);
						if (messagestack % 5 == 0) getPlayer().sendMessage("§4[§c!§4] §f원 안에 플레이어가 있어야 포션을 던질 수 있습니다.");
					}
				}	
			}
		}
	}
	
}