package rainstar.abilitywar.ability;

import static rainstar.abilitywar.system.config.RainStarAbilitySetting.rsAbilitySettings;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import javax.annotation.Nullable;

import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.Note;
import org.bukkit.Note.Tone;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.attribute.AttributeModifier.Operation;
import org.bukkit.block.data.BlockData;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByBlockEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent.RegainReason;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.scheduler.BukkitRunnable;

import daybreak.abilitywar.AbilityWar;
import daybreak.abilitywar.ability.AbilityBase;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.Materials;
import daybreak.abilitywar.ability.SubscribeEvent;
import daybreak.abilitywar.ability.decorator.ActiveHandler;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.config.ability.AbilitySettings.SettingObject;
import daybreak.abilitywar.game.AbstractGame.GameTimer;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.AbstractGame.Participant.ActionbarNotification.ActionbarChannel;
import daybreak.abilitywar.game.module.DeathManager;
import daybreak.abilitywar.game.team.interfaces.Teamable;
import daybreak.abilitywar.utils.base.Formatter;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.base.math.LocationUtil;
import daybreak.abilitywar.utils.base.math.VectorUtil;
import daybreak.abilitywar.utils.base.minecraft.entity.health.Healths;
import daybreak.abilitywar.utils.base.minecraft.item.builder.ItemBuilder;
import daybreak.abilitywar.utils.base.minecraft.nms.NMS;
import daybreak.abilitywar.utils.base.minecraft.version.ServerVersion;
import daybreak.abilitywar.utils.base.random.Random;
import daybreak.abilitywar.utils.library.MaterialX;
import daybreak.abilitywar.utils.library.ParticleLib;
import daybreak.abilitywar.utils.library.SoundLib;
import daybreak.google.common.base.Predicate;

@AbilityManifest(name = "믹솔로지스트", rank = Rank.L, species = Species.HUMAN, explain = {
		"§7패시브 §8- §a재료 확보§f: 금 F키로 임의의 §a재료§f를 하나 얻습니다.",
		" §a재료§f는 §d복숭아§7 / §6오렌지§7 / §e레몬§7 / §a라임§7 / §b청포도§7 / §3탄산§f의 6종으로,",
		" 금을 들고 웅크린 채 휠로 수급할 §a재료§f를 정할 수 있습니다.",
		" §a재료§f는 최대 $[MAX_INGREDIENT]개까지 보유할 수 있습니다.",
		"§7금 우클릭 §8- §6에이드 제조§f: GUI를 오픈하여 무작위 3개의 레시피를 제시받습니다.",
		" 그 중 하나를 택해 §6음료§f로 제조해 보관합니다§8(§7최대 1개§8)§f. $[COOLDOWN]",
		" 제조된 §6음료§f는 금 좌클릭으로 소모해서 효과를 받을 수 있습니다.",
		"§f[§d복숭아에이드§f] §d복§7+§d복§7+§3탄§7 / §d복§7+§3탄§7+§3탄§f.",
		" 즉시 잃은 체력의 $[PEACH_RECOVERY]%를 §d회복§f하고, $[PEACH_DURATION]초간 §d회복 효과§f가 2배로 증가합니다.",
		"§f[§6오렌지에이드§f] §6오§7+§6오§7+§3탄§7 / §6오§7+§3탄§7+§3탄§f.",
		" $[ORANGE_DURATION]초간 §6원거리 공격력§f이 $[ORANGE_INCREASE]% 증가하고, 화살이 미세하게 §6유도§f됩니다.",
		"§f[§e레모네이드§f] §e레§7+§e레§7+§3탄§7 / §e레§7+§3탄§7+§3탄§f.",
		" $[LEMON_DURATION]초간 §e받는 피해량§f이 $[LEMON_DECREASE]% 감소하고, 감소시킨 피해만큼 §e흡수 체력§f을 얻습니다.",
		"§f[§a라임에이드§f] §a라§7+§a라§7+§3탄§7 / §a라§7+§3탄§7+§3탄§f.",
		" $[LIME_DURATION]초간 §a타게팅 불가 상태§f가 되며 모든 능력 §a쿨타임이 빠르게 감소§f합니다.",
		"§f[§b청포도에이드§f] §b청§7+§b청§7+§3탄§7 / §b청§7+§3탄§7+§3탄§f.",
		" $[MUSCAT_DURATION]초간 §b근접 공격력§f이 $[MUSCAT_INCREASE]%, §b이동 속도§f가 $[MUSCAT_SPEED]% 증가합니다.",
		"§f[§d트§6로§e피§a컬§b?§f] §6서로 다른 과일 세 개§8(§7무작위 소모§8)§f.",
		" §d무§6작§e위 §a에§b이§3드§f 효과를 2개 얻지만 $[TROPICAL_DURATION]초간 받는 피해가 $[TROPICAL_INCREASE]% 증가합니다.",
		"§f[§3탄산수§f] §3탄§7+§3탄§7+§3탄§f.",
		" §3HP§f를 $[SPARKLING_HP] 잃고 $[SPARKLING_DURATION]초간 §3무적§f이 됩니다.",
		"§f[§5믹스에이드§f] §6서로 다른 과일 두 개§8(§7무작위 소모§8)§7+§3탄§f.",
		" $[MIX_DURATION]초간 근/원거리 공격력이 §51~50%§f만큼 §5무작위 감소§f하고 반대가 §5증가§f합니다.",
		"§f[§c남은 재료 처리§f] §c무작위 재료 3개 제시§f.",
		" 그 재료를 잃고 다음 §c쿨타임§f은 §c20%§f만 가집니다.",
		"§b[§7아이디어 제공자§b] §6aidaid1234"
		})
@SuppressWarnings("deprecation")
@Materials(materials = Material.GOLD_INGOT)
public class Mixologist extends AbilityBase implements ActiveHandler {
	
	public Mixologist(Participant participant) {
		super(participant);
	}
	
	public static final SettingObject<Integer> COOLDOWN = rsAbilitySettings.new SettingObject<Integer>(Mixologist.class,
			"cooldown", 90, "# 에이드 제조 쿨타임") {
		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}
		
        @Override
        public String toString() {
            return Formatter.formatCooldown(getValue());
        }
	};
	
	public static final SettingObject<Integer> MAX_INGREDIENT = rsAbilitySettings.new SettingObject<Integer>(Mixologist.class,
			"max-ingredient", 9, "# 최대 소지 가능한 재료") {
		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}
	};

	public static final SettingObject<Integer> PEACH_RECOVERY = rsAbilitySettings.new SettingObject<Integer>(Mixologist.class,
			"peach-recovery", 50, "# 복숭아에이드 잃은 체력 회복량", "# 단위: %") {
		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}
	};
	
	public static final SettingObject<Double> PEACH_DURATION = rsAbilitySettings.new SettingObject<Double>(Mixologist.class,
			"peach-duration", 15.0, "# 복숭아에이드 회복량 증가 지속시간", "# 단위: 초") {
		@Override
		public boolean condition(Double value) {
			return value >= 0;
		}
	};
	
	public static final SettingObject<Integer> ORANGE_INCREASE = rsAbilitySettings.new SettingObject<Integer>(Mixologist.class,
			"orange-increase", 25, "# 오렌지에이드 원거리 공격력 증가량", "# 단위: %") {
		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}
	};
	
	public static final SettingObject<Double> ORANGE_DURATION = rsAbilitySettings.new SettingObject<Double>(Mixologist.class,
			"orange-duration", 10.0, "# 오렌지에이드 지속 시간", "# 단위: 초") {
		@Override
		public boolean condition(Double value) {
			return value >= 0;
		}
	};
	
	public static final SettingObject<Integer> LEMON_DECREASE = rsAbilitySettings.new SettingObject<Integer>(Mixologist.class,
			"lemon-decrease", 40, "# 레모네이드 받는 피해 감소량", "# 단위: %") {
		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}
	};
	
	public static final SettingObject<Double> LEMON_DURATION = rsAbilitySettings.new SettingObject<Double>(Mixologist.class,
			"lemon-duration", 6.0, "# 레모네이드 지속 시간", "# 단위: 초") {
		@Override
		public boolean condition(Double value) {
			return value >= 0;
		}
	};
	
	public static final SettingObject<Double> LIME_DURATION = rsAbilitySettings.new SettingObject<Double>(Mixologist.class,
			"lime-duration", 14.0, "# 라임에이드 지속 시간", "# 단위: 초") {
		@Override
		public boolean condition(Double value) {
			return value >= 0;
		}
	};
	
	public static final SettingObject<Integer> MUSCAT_INCREASE = rsAbilitySettings.new SettingObject<Integer>(Mixologist.class,
			"muscat-increase", 25, "# 청포도에이드 근거리 공격력 증가량", "# 단위: %") {
		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}
	};
	
	public static final SettingObject<Integer> MUSCAT_SPEED = rsAbilitySettings.new SettingObject<Integer>(Mixologist.class,
			"muscat-speed", 20, "# 청포도에이드 이동 속도 증가량", "# 단위: %") {
		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}
	};
	
	public static final SettingObject<Double> MUSCAT_DURATION = rsAbilitySettings.new SettingObject<Double>(Mixologist.class,
			"muscat-duration", 10.0, "# 청포도에이드 지속 시간", "# 단위: 초") {
		@Override
		public boolean condition(Double value) {
			return value >= 0;
		}
	};
	
	public static final SettingObject<Integer> TROPICAL_INCREASE = rsAbilitySettings.new SettingObject<Integer>(Mixologist.class,
			"tropical-increase", 30, "# 트로피컬 받는 피해 증가량", "# 단위: %") {
		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}
	};
	
	public static final SettingObject<Double> TROPICAL_DURATION = rsAbilitySettings.new SettingObject<Double>(Mixologist.class,
			"tropical-duration", 10.0, "# 트로피컬 받는 피해 증가 지속 시간", "# 단위: 초") {
		@Override
		public boolean condition(Double value) {
			return value >= 0;
		}
	};
	
	public static final SettingObject<Double> SPARKLING_HP = rsAbilitySettings.new SettingObject<Double>(Mixologist.class,
			"sparkling-hp", 4.0, "# 탄산수 잃을 체력", "# 단위: HP") {
		@Override
		public boolean condition(Double value) {
			return value >= 0;
		}
	};

	public static final SettingObject<Double> SPARKLING_DURATION = rsAbilitySettings.new SettingObject<Double>(Mixologist.class,
			"sparkling-duration", 4.0, "# 탄산수 무적 지속 시간", "# 단위: 초") {
		@Override
		public boolean condition(Double value) {
			return value >= 0;
		}
	};
	
	public static final SettingObject<Double> MIX_DURATION = rsAbilitySettings.new SettingObject<Double>(Mixologist.class,
			"mix-duration", 15.0, "# 믹스에이드 공격력 증감 지속 시간", "# 단위: 초") {
		@Override
		public boolean condition(Double value) {
			return value >= 0;
		}
	};
	
	private final Cooldown cooldown = new Cooldown(COOLDOWN.getValue());
	private final int maxIngredient = MAX_INGREDIENT.getValue();
	private final double peachRecovery = PEACH_RECOVERY.getValue() * 0.01;
	private final int peachDuration = (int) (PEACH_DURATION.getValue() * 20);
	private final double orangeIncrease = ORANGE_INCREASE.getValue() * 0.01;
	private final int orangeDuration = (int) (ORANGE_DURATION.getValue() * 20);
	private final double lemonDecrease = LEMON_DECREASE.getValue() * 0.01;
	private final int lemonDuration = (int) (LEMON_DURATION.getValue() * 20);
	private final int limeDuration = (int) (LIME_DURATION.getValue() * 20);
	private final double muscatIncrease = MUSCAT_INCREASE.getValue() * 0.01;
	private final double muscatSpeed = MUSCAT_SPEED.getValue() * 0.01;
	private final int muscatDuration = (int) (MUSCAT_DURATION.getValue() * 20);
	private final double tropicalIncrease = TROPICAL_INCREASE.getValue() * 0.01;
	private final int tropicalDuration = (int) (TROPICAL_DURATION.getValue() * 20);
	private final double sparklingHp = SPARKLING_HP.getValue();
	private final int sparklingDuration = (int) (SPARKLING_DURATION.getValue() * 20);
	private final int mixDuration = (int) (MIX_DURATION.getValue() * 20);
	
	private final ActionbarChannel peachac = newActionbarChannel();
	private final ActionbarChannel orangeac = newActionbarChannel();
	private final ActionbarChannel lemonac = newActionbarChannel();
	private final ActionbarChannel limeac = newActionbarChannel();
	private final ActionbarChannel muscatac = newActionbarChannel();
	private final ActionbarChannel tropicalac = newActionbarChannel();
	private final ActionbarChannel sparklingac = newActionbarChannel();
	private final ActionbarChannel mixac = newActionbarChannel();
	private final ActionbarChannel ingredientac = newActionbarChannel();
	private final ActionbarChannel adeac = newActionbarChannel();
	private final DecimalFormat df = new DecimalFormat("0.0");
	private final List<Ingredient> ingredients = new ArrayList<>();
	private RecipeType ade = null;
	private Ingredient selectedIngredient = Ingredient.PEACH;
	
	private static final RecipeType[] TROPICAL_EFFECTS = {
		    RecipeType.PEACH,
		    RecipeType.ORANGE,
		    RecipeType.LEMON,
		    RecipeType.LIME,
		    RecipeType.MUSCAT,
		    RecipeType.SPARKLING,
		    RecipeType.MIX
		};
	
	private AttributeModifier modifier = new AttributeModifier(UUID.randomUUID(), "muscatspeed", muscatSpeed, Operation.ADD_SCALAR);
	private Random random = new Random();
	private boolean melee = false;
	private int multiply = 0;
	
	private final Predicate<Entity> predicate = new Predicate<Entity>() {
		@Override
		public boolean test(Entity entity) {
			if (entity.equals(getPlayer())) return false;
			if (entity instanceof ArmorStand) return false;
			if (entity instanceof Player) {
				if (!getGame().isParticipating(entity.getUniqueId())
						|| (getGame() instanceof DeathManager.Handler &&
								((DeathManager.Handler) getGame()).getDeathManager().isExcluded(entity.getUniqueId()))
						|| !getGame().getParticipant(entity.getUniqueId()).attributes().TARGETABLE.getValue()) {
					return false;
				}
				if (getGame() instanceof Teamable) {
					final Teamable teamGame = (Teamable) getGame();
					final Participant entityParticipant = teamGame.getParticipant(
							entity.getUniqueId()), participant = getParticipant();
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
	
	@Override
	public void onUpdate(Update update) {
		if (update == Update.RESTRICTION_CLEAR) passive.start();
	}
	
	public boolean ActiveSkill(Material material, ClickType clicktype) {
		if (material.equals(Material.GOLD_INGOT)) {
			if (clicktype.equals(ClickType.LEFT_CLICK)) {
				if (ade == null) {
					getPlayer().sendMessage("§6[§e!§6] §c마실 에이드가 없습니다.");
					return false;
				}

		        useAde();

		        ade = null;
		        adeac.update(null);

		        return true;
			} else if (!cooldown.isCooldown()) {
				if (ingredients.size() < 3) {
				    getPlayer().sendMessage("§c재료가 부족합니다.");
				    return false;
				}
				
				List<RecipeChoice> choices = createChoices();

		        RecipeGUI gui = new RecipeGUI();
		        gui.openGUI(choices);
		        gui.start();

		        return cooldown.start();
			}
		}
		return false;
	}
	
	private void activate(RecipeType type) {
	    switch (type) {
		case PEACH:
		    if (peach.isRunning()) peach.stop(false);
		    peach.start();
		    break;
		
		case ORANGE:
		    if (orange.isRunning()) orange.stop(false);
		    orange.start();
		    break;
		
		case LEMON:
		    if (lemon.isRunning()) lemon.stop(false);
		    lemon.start();
		    break;
		
		case LIME:
		    if (lime.isRunning()) lime.stop(false);
		    lime.start();
		    break;
		
		case MUSCAT:
		    if (muscat.isRunning()) muscat.stop(false);
		    muscat.start();
		    break;
		
		case SPARKLING:
		    if (sparkling.isRunning()) sparkling.stop(false);
		    sparkling.start();
		    break;
		
		case MIX:
		    if (mix.isRunning()) mix.stop(false);
		    mix.start();
		    break;
		
		case DISCARD:
			if (cooldown.isRunning()) cooldown.setCount((int) (cooldown.getCount() * 0.2));
			break;
		    
		default:
		    break;
	    }
	}
	
	private void useAde() {
		SoundLib.BLOCK_BREWING_STAND_BREW.playSound(getPlayer().getLocation(), 1, 0.5f);
	    if (ade == RecipeType.TROPICAL) {
	    	List<RecipeType> effects = new ArrayList<>(Arrays.asList(TROPICAL_EFFECTS));
	    	
	        Collections.shuffle(effects);

	        activate(effects.get(0));
	        activate(effects.get(1));
	        tropical.start();
	        return;
	    } else activate(ade);
	}
	
    @SubscribeEvent(onlyRelevant = true)
    public void onPlayerSwapHandItems(PlayerSwapHandItemsEvent e) {
    	if (e.getOffHandItem().getType().equals(Material.GOLD_INGOT)) {
    		e.setCancelled(true);
    		
    		if (ingredients.size() >= maxIngredient) {
    	        getPlayer().sendMessage("§c재료를 더 이상 보관할 수 없습니다.");
    	        SoundLib.BASS_GUITAR.playInstrument(getPlayer(), Note.natural(1, Tone.A));
    	        return;
    	    }

    		if (!gathercool.isRunning()) {
        	    ingredients.add(selectedIngredient);

        	    SoundLib.ENTITY_ITEM_PICKUP.playSound(getPlayer());
        	    gathercool.start();	
    		}
    	}
    }
    
	private final AbilityTimer gathercool = new AbilityTimer(5) {
		
	}.setPeriod(TimeUnit.TICKS, 1).register();
    
	private final AbilityTimer passive = new AbilityTimer() {

		@Override
		protected void run(int count) {
			ingredientac.update(
					"§d■ " + count(Ingredient.PEACH)
				+ " §6■ " + count(Ingredient.ORANGE)
				+ " §e■ " + count(Ingredient.LEMON)
				+ " §a■ " + count(Ingredient.LIME)
				+ " §b■ " + count(Ingredient.MUSCAT)
				+ " §3■ " + count(Ingredient.SPARKLING)
			);
		}

	}.setPeriod(TimeUnit.TICKS, 1).register();
	
	private final AbilityTimer peach = new AbilityTimer(peachDuration) {
		
		@Override
		public void onStart() {
			peachac.update("§d복숭아에이드§7: §b" + df.format(peachDuration / 20.0) + "§f초");
			double maxHealth = getPlayer().getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue();
			double nowHealth = getPlayer().getHealth();
			double addHealth = (maxHealth - nowHealth) * peachRecovery;
			final EntityRegainHealthEvent event = new EntityRegainHealthEvent(getPlayer(), addHealth, RegainReason.CUSTOM);
			Bukkit.getPluginManager().callEvent(event);
			if (!event.isCancelled()) {
				Healths.setHealth(getPlayer(), nowHealth + addHealth);
			}
		}
		
		@Override
		public void run(int count) {
			peachac.update("§d복숭아에이드§7: §b" + df.format(count / 20.0) + "§f초");
		}
		
		@Override
		public void onEnd() {
			onSilentEnd();
		}
		
		@Override
		public void onSilentEnd() {
			peachac.update(null);
		}
		
	}.setPeriod(TimeUnit.TICKS, 1).register();
	
	private final AbilityTimer orange = new AbilityTimer(orangeDuration) {
		
		@Override
		public void onStart() {
			orangeac.update("§6오렌지에이드§7: §b" + df.format(orangeDuration / 20.0) + "§f초");
		}
		
		@Override
		public void run(int count) {
			orangeac.update("§6오렌지에이드§7: §b" + df.format(count / 20.0) + "§f초");
		}
		
		@Override
		public void onEnd() {
			onSilentEnd();
		}
		
		@Override
		public void onSilentEnd() {
			orangeac.update(null);
		}
		
	}.setPeriod(TimeUnit.TICKS, 1).register();
	
	private final AbilityTimer lemon = new AbilityTimer(lemonDuration) {
		
		@Override
		public void onStart() {
			lemonac.update("§e레모네이드§7: §b" + df.format(lemonDuration / 20.0) + "§f초");
		}
		
		@Override
		public void run(int count) {
			lemonac.update("§e레모네이드§7: §b" + df.format(count / 20.0) + "§f초");
		}
		
		@Override
		public void onEnd() {
			onSilentEnd();
		}
		
		@Override
		public void onSilentEnd() {
			lemonac.update(null);
		}
		
	}.setPeriod(TimeUnit.TICKS, 1).register();
	
	private final AbilityTimer lime = new AbilityTimer(limeDuration) {
		
		@Override
		public void onStart() {
			limeac.update("§a라임에이드§7: §b" + df.format(limeDuration / 20.0) + "§f초");
			getParticipant().attributes().TARGETABLE.setValue(false);
			
			if (ServerVersion.getVersion() >= 13) {
				BlockData powder = Material.LIME_WOOL.createBlockData();
				ParticleLib.FALLING_DUST.spawnParticle(getPlayer().getLocation().clone().add(0, 1, 0), 0.3, 1, 0.3, 20, 0, powder);
			} else {
				ItemStack powder = new ItemStack(Material.getMaterial("WOOL"), 1, (byte) 5);
				ParticleLib.FALLING_DUST.spawnParticle(getPlayer().getLocation().clone().add(0, 1, 0), 0.3, 1, 0.3, 20, 0, powder.getData());
			}
		}
		
		@Override
		public void run(int count) {
			limeac.update("§a라임에이드§7: §b" + df.format(count / 20.0) + "§f초");
			if (count % 10 == 0) {
				AbilityBase ab = getParticipant().getAbility();
				for (GameTimer t : ab.getRunningTimers()) {
					if (t instanceof Cooldown.CooldownTimer) {
						t.setCount(Math.max(0, t.getCount() - 1));
					}
				}
			}
		}
		
		@Override
		public void onEnd() {
			onSilentEnd();
		}
		
		@Override
		public void onSilentEnd() {
			limeac.update(null);
			getParticipant().attributes().TARGETABLE.setValue(true);
		}
		
	}.setPeriod(TimeUnit.TICKS, 1).register();
	
	private final AbilityTimer muscat = new AbilityTimer(muscatDuration) {
		
		@Override
		public void onStart() {
			muscatac.update("§b청포도에이드§7: §b" + df.format(muscatDuration / 20.0) + "§f초");
			getPlayer().getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).addModifier(modifier);
		}
		
		@Override
		public void run(int count) {
			muscatac.update("§b청포도에이드§7: §b" + df.format(count / 20.0) + "§f초");
		}
		
		@Override
		public void onEnd() {
			onSilentEnd();
		}
		
		@Override
		public void onSilentEnd() {
			muscatac.update(null);
			getPlayer().getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).removeModifier(modifier);
		}
		
	}.setPeriod(TimeUnit.TICKS, 1).register();
	
	private final AbilityTimer tropical = new AbilityTimer(tropicalDuration) {
		
		@Override
		public void onStart() {
			tropicalac.update("§d트§6로§e피§a컬§b?§7: §b" + df.format(tropicalDuration / 20.0) + "§f초");
		}
		
		@Override
		public void run(int count) {
			tropicalac.update("§d트§6로§e피§a컬§b?§7: §b" + df.format(count / 20.0) + "§f초");
		}
		
		@Override
		public void onEnd() {
			onSilentEnd();
		}
		
		@Override
		public void onSilentEnd() {
			tropicalac.update(null);
		}
		
	}.setPeriod(TimeUnit.TICKS, 1).register();
	
	private final AbilityTimer sparkling = new AbilityTimer(sparklingDuration) {
		
		@Override
		public void onStart() {
			sparklingac.update("§3탄산수§7: §b" + df.format(sparklingDuration / 20.0) + "§f초");
			if (getPlayer().getHealth() - sparklingHp <= 0) getPlayer().damage(Integer.MAX_VALUE);
			else Healths.setHealth(getPlayer(), getPlayer().getHealth() - sparklingHp);
		}
		
		@Override
		public void run(int count) {
			sparklingac.update("§3탄산수§7: §b" + df.format(count / 20.0) + "§f초");
		}
		
		@Override
		public void onEnd() {
			onSilentEnd();
		}
		
		@Override
		public void onSilentEnd() {
			sparklingac.update(null);
		}
		
	}.setPeriod(TimeUnit.TICKS, 1).register();
	
	private final AbilityTimer mix = new AbilityTimer(mixDuration) {
		
		@Override
		public void onStart() {
			mixac.update("§5믹스에이드§7: §b" + df.format(mixDuration / 20.0) + "§f초 §7| §5" + multiply + "% §a근 " + (melee ? "§c▲" : "§b▼") + " §b원 " + (melee ? "§b▼" : "§c▲"));
			multiply = random.nextInt(50) + 1;
			melee = random.nextBoolean();
		}
		
		@Override
		public void run(int count) {
			mixac.update("§5믹스에이드§7: §b" + df.format(count / 20.0) + "§f초 §7| §5" + multiply + "% §a근 " + (melee ? "§c▲" : "§b▼") + " §b원 " + (melee ? "§b▼" : "§c▲"));
		}
		
		@Override
		public void onEnd() {
			onSilentEnd();
		}
		
		@Override
		public void onSilentEnd() {
			mixac.update(null);
			multiply = 0;
			melee = false;
		}
		
	}.setPeriod(TimeUnit.TICKS, 1).register();
	
	@SubscribeEvent(onlyRelevant = true)
	private void onEntityRegainHealth(EntityRegainHealthEvent e) {
		if (peach.isRunning() && peach.getCount() < peachDuration - 1) {
			e.setAmount(e.getAmount() * 2);
			if (ServerVersion.getVersion() >= 13) {
				BlockData powder = Material.PINK_WOOL.createBlockData();
				ParticleLib.FALLING_DUST.spawnParticle(getPlayer().getLocation().clone().add(0, 1, 0), 0.3, 1, 0.3, 20, 0, powder);
			} else {
				ItemStack powder = new ItemStack(Material.getMaterial("WOOL"), 1, (byte) 6);
				ParticleLib.FALLING_DUST.spawnParticle(getPlayer().getLocation().clone().add(0, 1, 0), 0.3, 1, 0.3, 20, 0, powder.getData());
			}
		}
	}
	
	@SubscribeEvent
	private void onEntityDamage(EntityDamageEvent e) {
		if (e.getEntity().equals(getPlayer())) {
			if (tropical.isRunning()) {
				e.setDamage(e.getDamage() * (1 + tropicalIncrease));
				if (ServerVersion.getVersion() >= 13) {
					BlockData powder = Material.WHITE_WOOL.createBlockData();
					ParticleLib.FALLING_DUST.spawnParticle(getPlayer().getLocation().clone().add(0, 1, 0), 0.3, 1, 0.3, 20, 0, powder);
				} else {
					ItemStack powder = new ItemStack(Material.getMaterial("WOOL"), 1, (byte) 0);
					ParticleLib.FALLING_DUST.spawnParticle(getPlayer().getLocation().clone().add(0, 1, 0), 0.3, 1, 0.3, 20, 0, powder.getData());
				}
			}
			
			if (lemon.isRunning()) {
				double finalDamage = e.getFinalDamage();
				e.setDamage(e.getDamage() * lemonDecrease);
				NMS.setAbsorptionHearts(getPlayer(), (float) (NMS.getAbsorptionHearts(getPlayer()) + (finalDamage - e.getFinalDamage())));
				if (ServerVersion.getVersion() >= 13) {
					BlockData powder = Material.YELLOW_WOOL.createBlockData();
					ParticleLib.FALLING_DUST.spawnParticle(getPlayer().getLocation().clone().add(0, 1, 0), 0.3, 1, 0.3, 20, 0, powder);
				} else {
					ItemStack powder = new ItemStack(Material.getMaterial("WOOL"), 1, (byte) 4);
					ParticleLib.FALLING_DUST.spawnParticle(getPlayer().getLocation().clone().add(0, 1, 0), 0.3, 1, 0.3, 20, 0, powder.getData());
				}
			}
			
			if (sparkling.isRunning()) {
				e.setCancelled(true);
				if (ServerVersion.getVersion() >= 13) {
					BlockData powder = Material.CYAN_WOOL.createBlockData();
					ParticleLib.FALLING_DUST.spawnParticle(getPlayer().getLocation().clone().add(0, 1, 0), 0.3, 1, 0.3, 20, 0, powder);
				} else {
					ItemStack powder = new ItemStack(Material.getMaterial("WOOL"), 1, (byte) 9);
					ParticleLib.FALLING_DUST.spawnParticle(getPlayer().getLocation().clone().add(0, 1, 0), 0.3, 1, 0.3, 20, 0, powder.getData());
				}
			}
		}
	}
	
	@SubscribeEvent
	private void onEntityDamageByBlock(EntityDamageByBlockEvent e) {
		onEntityDamage(e);
	}
	
	@SubscribeEvent
	private void onEntityDamageByEntity(EntityDamageByEntityEvent e) {
		onEntityDamage(e);
		
		if (e.getDamager() instanceof Projectile) {
			Projectile projectile = (Projectile) e.getDamager();
			if (getPlayer().equals(projectile.getShooter())) {
				if (orange.isRunning()) {
					e.setDamage(e.getDamage() * (1 + orangeIncrease));
					if (ServerVersion.getVersion() >= 13) {
						BlockData powder = Material.ORANGE_WOOL.createBlockData();
						ParticleLib.FALLING_DUST.spawnParticle(e.getEntity().getLocation().clone().add(0, 1, 0), 0.3, 1, 0.3, 20, 0, powder);
					} else {
						ItemStack powder = new ItemStack(Material.getMaterial("WOOL"), 1, (byte) 1);
						ParticleLib.FALLING_DUST.spawnParticle(e.getEntity().getLocation().clone().add(0, 1, 0), 0.3, 1, 0.3, 20, 0, powder.getData());
					}
				}
				
				if (mix.isRunning()) {
					e.setDamage(e.getDamage() * (melee ? (1 - multiply * 0.01) : (1 + multiply * 0.01)));
					
					if (ServerVersion.getVersion() >= 13) {
						BlockData powder = Material.PURPLE_WOOL.createBlockData();
						ParticleLib.FALLING_DUST.spawnParticle(e.getEntity().getLocation().clone().add(0, 1, 0), 0.3, 1, 0.3, 20, 0, powder);
					} else {
						ItemStack powder = new ItemStack(Material.getMaterial("WOOL"), 1, (byte) 10);
						ParticleLib.FALLING_DUST.spawnParticle(e.getEntity().getLocation().clone().add(0, 1, 0), 0.3, 1, 0.3, 20, 0, powder.getData());
					}
				}
			}
		}
		
		if (getPlayer().equals(e.getDamager())) {
			if (muscat.isRunning()) {
				e.setDamage(e.getDamage() * (1 + muscatIncrease));
				if (ServerVersion.getVersion() >= 13) {
					BlockData powder = Material.LIGHT_BLUE_WOOL.createBlockData();
					ParticleLib.FALLING_DUST.spawnParticle(e.getEntity().getLocation().clone().add(0, 1, 0), 0.3, 1, 0.3, 20, 0, powder);
				} else {
					ItemStack powder = new ItemStack(Material.getMaterial("WOOL"), 1, (byte) 3);
					ParticleLib.FALLING_DUST.spawnParticle(e.getEntity().getLocation().clone().add(0, 1, 0), 0.3, 1, 0.3, 20, 0, powder.getData());
				}
			}
			
			if (mix.isRunning()) {
				e.setDamage(e.getDamage() * (melee ? (1 + multiply * 0.01) : (1 - multiply * 0.01)));
				
				if (ServerVersion.getVersion() >= 13) {
					BlockData powder = Material.PURPLE_WOOL.createBlockData();
					ParticleLib.FALLING_DUST.spawnParticle(e.getEntity().getLocation().clone().add(0, 1, 0), 0.3, 1, 0.3, 20, 0, powder);
				} else {
					ItemStack powder = new ItemStack(Material.getMaterial("WOOL"), 1, (byte) 10);
					ParticleLib.FALLING_DUST.spawnParticle(e.getEntity().getLocation().clone().add(0, 1, 0), 0.3, 1, 0.3, 20, 0, powder.getData());
				}
			}
		}
	}
	
	@SubscribeEvent
	public void onProjectileLaunch(ProjectileLaunchEvent e) {
		if (orange.isRunning() && getPlayer().equals(e.getEntity().getShooter()) && NMS.isArrow(e.getEntity())) {
			new Homing((Arrow) e.getEntity(), e.getEntity().getVelocity().length()).start();
		}
	}
	
	@SubscribeEvent(onlyRelevant = true)
	private void onSlotChange(final PlayerItemHeldEvent e) {
		if (!getPlayer().isSneaking() || e.getPreviousSlot() == e.getNewSlot()) return;

		final PlayerInventory inventory = getPlayer().getInventory();
		final ItemStack previous = inventory.getItem(e.getPreviousSlot());

		if (previous == null || previous.getType() != Material.GOLD_INGOT) return;

		e.setCancelled(true);

		final State state = getState(e.getPreviousSlot(), e.getNewSlot());
		if (state == State.UNKNOWN) return;

		if (state == State.UP) selectedIngredient = previousIngredient();
		else selectedIngredient = nextIngredient();

		NMS.sendTitle(getPlayer(), "§f[" + selectedIngredient.getDisplay() + "§f]", previousIngredient().getDisplay() + " §8◀      ▶ §7" + nextIngredient().getDisplay(), 0, 20, 0);
		SoundLib.UI_BUTTON_CLICK.playSound(getPlayer());
		
		if (!titleClear.start()) titleClear.setCount(10);
	}
	
	private Ingredient nextIngredient() {
		return nextIngredient(selectedIngredient);
	}

	private Ingredient previousIngredient() {
		return previousIngredient(selectedIngredient);
	}

	private Ingredient nextIngredient(Ingredient ingredient) {
		Ingredient[] values = Ingredient.values();
		return values[(ingredient.ordinal() + 1) % values.length];
	}

	private Ingredient previousIngredient(Ingredient ingredient) {
		Ingredient[] values = Ingredient.values();
		return values[(ingredient.ordinal() + values.length - 1) % values.length];
	}
	
	private State getState(final int previousSlot, final int newSlot) {
		if (previousSlot == 0) {
			return newSlot >= 6 ? State.UP : (newSlot <= 3 ? State.DOWN : State.UNKNOWN);
		} else if (previousSlot == 8) {
			return newSlot <= 2 ? State.DOWN : (newSlot >= 5 ? State.UP : State.UNKNOWN);
		} else {
			return calculate(previousSlot, -1) == newSlot
					|| calculate(previousSlot, -2) == newSlot
					|| calculate(previousSlot, -3) == newSlot ? State.UP :
					(calculate(previousSlot, 1) == newSlot
					|| calculate(previousSlot, 2) == newSlot
					|| calculate(previousSlot, 3) == newSlot ? State.DOWN : State.UNKNOWN
			);
		}
	}
	
	private int calculate(int slot, int offset) {
		final int value = slot + offset;
		if (value < 0) return 9 + value;
		else if (value > 8) return value - 9;
		else return value;
	}

	private enum State {
		UP, DOWN, UNKNOWN
	}
	
	private final AbilityTimer titleClear = new AbilityTimer(10) {
		@Override
		protected void run(int count) {
		}

		@Override
		protected void onEnd() {
			NMS.clearTitle(getPlayer());
		}

		@Override
		protected void onSilentEnd() {
			NMS.clearTitle(getPlayer());
		}
	}.setPeriod(TimeUnit.TICKS, 4);
	
	public enum Ingredient {
	    PEACH("§d복숭아"),
	    ORANGE("§6오렌지"),
	    LEMON("§e레몬"),
	    LIME("§a라임"),
	    MUSCAT("§b청포도"),
	    SPARKLING("§3탄산");

	    private final String display;

	    Ingredient(String display) {
	        this.display = display;
	    }

	    public String getDisplay() {
	        return display;
	    }
	}
	
	public enum RecipeType {
	    PEACH("§d복숭아에이드", Color.fromRGB(254, 140, 21), "§7즉시 잃은 체력을 회복시키고#§7회복력을 늘려주는 에이드."),
	    ORANGE("§6오렌지에이드", Color.fromRGB(254, 158, 6), "§7원거리 공격력 증폭 및 유도로#§7원거리 공격 특화 에이드."),
	    LEMON("§e레모네이드", Color.fromRGB(250, 254, 107), "§7받는 피해를 대폭 감소시키고,#§7흡수 체력을 제공하는 에이드."),
	    LIME("§a라임에이드", Color.fromRGB(148, 254, 60), "§7타게팅 불가 및 능력 쿨타임 감소로#§7능력 시너지를 제공하는 에이드."),
	    MUSCAT("§b청포도에이드", Color.fromRGB(44, 254, 201), "§7근거리 공격력 증폭 및 이동 속도 증가로#§7근거리 공격 특화 에이드."),
	    TROPICAL("§d트§6로§e피§a컬§b?", Color.fromRGB(254, 250, 235), "§7무작위 효과를 제공하지만#§7받는 피해량이 증가하는 에이드."),
	    SPARKLING("§3탄산수", Color.fromRGB(108, 220, 254), "§7체력을 조금 소모해 즉시#§7무적 상태가 되는 에이드."),
	    MIX("§5믹스에이드", Color.fromRGB(128, 39, 254), "§7무작위 근/원거리 공격력을#§7서로 증/감으로 부여하는 에이드."),
	    DISCARD("§c남은 재료 처리", Color.fromRGB(89, 28, 28), "§7재료를 처리하고 다음 쿨타임 감소.");
		
		private final String name;
	    private final Color color;
	    private final String explain;
	    
		RecipeType(String name, Color color, String explain) {
			this.name = name;
			this.color = color;
			this.explain = explain;
		}
		
		public String getDisplayName() {
			return name;
		}
		
		public Color getColor() {
			return color;
		}
		
		public String getExplain() {
			return explain;
		}
	}
	
	public class RecipeChoice {

	    private final RecipeType type;
	    private final List<Ingredient> consume;

	    public RecipeChoice(RecipeType type, Ingredient... consume) {
	        this.type = type;
	        this.consume = Arrays.asList(consume);
	    }

	    public RecipeType getType() {
	        return type;
	    }

	    public List<Ingredient> getConsume() {
	        return consume;
	    }

	}
	
	private RecipeChoice createChoice(RecipeType type) {
	    switch (type) {
	    case PEACH: return createFruitRecipe(RecipeType.PEACH, Ingredient.PEACH);
	    case ORANGE: return createFruitRecipe(RecipeType.ORANGE, Ingredient.ORANGE);
	    case LEMON: return createFruitRecipe(RecipeType.LEMON, Ingredient.LEMON);
	    case LIME: return createFruitRecipe(RecipeType.LIME, Ingredient.LIME);
	    case MUSCAT: return createFruitRecipe(RecipeType.MUSCAT, Ingredient.MUSCAT);
	    case MIX: return createMix();
	    case TROPICAL: return createTropical();
	    case SPARKLING: return createSparkling();
	    default: return null;
	    }
	}
	
	private List<RecipeChoice> createChoices() {

	    List<RecipeChoice> result = new ArrayList<>();

	    addIfExists(result, createChoice(RecipeType.PEACH));
	    addIfExists(result, createChoice(RecipeType.ORANGE));
	    addIfExists(result, createChoice(RecipeType.LEMON));
	    addIfExists(result, createChoice(RecipeType.LIME));
	    addIfExists(result, createChoice(RecipeType.MUSCAT));
	    addIfExists(result, createChoice(RecipeType.MIX));
	    addIfExists(result, createChoice(RecipeType.TROPICAL));
	    addIfExists(result, createChoice(RecipeType.SPARKLING));

	    Collections.shuffle(result);

	    while (result.size() < 3) {
	        result.add(createDiscardChoice());
	    }

	    Collections.shuffle(result);

	    return result.size() > 3 ? result.subList(0, 3) : result;
	}
	
	private void addIfExists(List<RecipeChoice> list, RecipeChoice choice) {
	    if (choice != null) list.add(choice);
	}
	
	private List<Ingredient> getDistinctFruits() {
	    List<Ingredient> fruits = new ArrayList<>();

	    if (count(Ingredient.PEACH) > 0) fruits.add(Ingredient.PEACH);
	    if (count(Ingredient.ORANGE) > 0) fruits.add(Ingredient.ORANGE);
	    if (count(Ingredient.LEMON) > 0) fruits.add(Ingredient.LEMON);
	    if (count(Ingredient.LIME) > 0) fruits.add(Ingredient.LIME);
	    if (count(Ingredient.MUSCAT) > 0) fruits.add(Ingredient.MUSCAT);

	    return fruits;
	}

	private RecipeChoice createMix() {
	    if (count(Ingredient.SPARKLING) < 1) return null;

	    List<Ingredient> fruits = getDistinctFruits();
	    if (fruits.size() < 2) return null;

	    Collections.shuffle(fruits);

	    return new RecipeChoice(RecipeType.MIX, fruits.get(0), fruits.get(1), Ingredient.SPARKLING);
	}

	private RecipeChoice createTropical() {
	    List<Ingredient> fruits = getDistinctFruits();
	    if (fruits.size() < 3) return null;

	    Collections.shuffle(fruits);

	    return new RecipeChoice(RecipeType.TROPICAL, fruits.get(0), fruits.get(1), fruits.get(2));
	}

	private RecipeChoice createSparkling() {
	    if (count(Ingredient.SPARKLING) < 3) return null;

	    return new RecipeChoice(RecipeType.SPARKLING, Ingredient.SPARKLING, Ingredient.SPARKLING, Ingredient.SPARKLING);
	}

	private RecipeChoice createDiscardChoice() {
	    List<Ingredient> copy = new ArrayList<>(ingredients);

	    Collections.shuffle(copy);

	    return new RecipeChoice(RecipeType.DISCARD, copy.get(0), copy.get(1), copy.get(2));
	}
	
	private RecipeChoice createFruitRecipe(RecipeType type, Ingredient fruit) {

	    final int fruitCount = count(fruit);
	    final int sparklingCount = count(Ingredient.SPARKLING);

	    List<RecipeChoice> choices = new ArrayList<>(2);

	    if (fruitCount >= 2 && sparklingCount >= 1) {
	        choices.add(new RecipeChoice(type, fruit, fruit, Ingredient.SPARKLING));
	    }

	    if (fruitCount >= 1 && sparklingCount >= 2) {
	        choices.add(new RecipeChoice(type, fruit, Ingredient.SPARKLING, Ingredient.SPARKLING));
	    }

	    if (choices.isEmpty()) {
	        return null;
	    }

	    return choices.get(random.nextInt(choices.size()));
	}
	
	private int count(Ingredient ingredient) {
	    int count = 0;

	    for (Ingredient in : ingredients) {
	        if (in == ingredient) {
	            count++;
	        }
	    }

	    return count;
	}
	
	private void consume(RecipeChoice choice) {
	    for (Ingredient ingredient : choice.getConsume()) {
	        ingredients.remove(ingredient);
	    }
	}
	
	public class RecipeGUI extends AbilityTimer implements Listener {
		
		private final ItemStack NULL = (new ItemBuilder(MaterialX.GRAY_STAINED_GLASS_PANE)).displayName(" ").build();
		
		private final Inventory gui;
		
		private final List<RecipeChoice> choices = new ArrayList<>();
		private RecipeChoice selected;
		
		public RecipeGUI() {
			super(TaskType.REVERSE, 99999);
			setPeriod(TimeUnit.TICKS, 1);
			gui = Bukkit.createInventory(null, InventoryType.HOPPER, "§6에이드 제조장");
			Bukkit.getPluginManager().registerEvents(this, AbilityWar.getPlugin());
		}
		
		private void openGUI(List<RecipeChoice> choices) {
			this.choices.clear();
			this.choices.addAll(choices);
			
		    gui.clear();

	    	gui.setItem(1, NULL);
		    gui.setItem(3, NULL);
	    	
		    gui.setItem(0, createItem(choices.get(0)));
		    gui.setItem(2, createItem(choices.get(1)));
	    	gui.setItem(4, createItem(choices.get(2)));

		    getPlayer().openInventory(gui);
		}
		
		private ItemStack createItem(RecipeChoice choice) {
		    ItemStack item = new ItemBuilder(MaterialX.POTION).build();
		    PotionMeta meta = (PotionMeta) item.getItemMeta();

		    meta.setDisplayName(choice.getType().getDisplayName());
		    meta.setColor(choice.getType().getColor());
		    
		    String[] description = choice.getType().getExplain().split("#");

		    List<String> lore = new ArrayList<>();

		    lore.add("§7재료");

		    StringBuilder recipe = new StringBuilder();

		    for (Ingredient ingredient : choice.getConsume()) {
		        if (recipe.length() > 0) recipe.append(" §7+ ");
		        recipe.append(ingredient.getDisplay());
		    }

		    lore.add(recipe.toString());
		    lore.add("§7");
			for (int a = 0; a < description.length; a++) {
				lore.add(description[a]);
			}
		    lore.add("§7");
		    lore.add("§5» §d클릭하여 제조");

		    meta.setLore(lore);
		 
		    item.setItemMeta(meta);
		    item.addUnsafeEnchantment(Enchantment.MENDING, 1);

		    return item;
		}
		
	    @EventHandler()
	    public void onInventoryClick(InventoryClickEvent e) {
	    	if (e.getClickedInventory().equals(gui)) {
	    		e.setCancelled(true);
	    		switch (e.getSlot()) {
	    		case 0: selected = this.choices.get(0); break;
	    		case 2: selected = this.choices.get(1); break;
	    		case 4: selected = this.choices.get(2); break;
	    		}
	    		getPlayer().closeInventory();
	    		this.stop(false);
	    	}
	    }
	    
		@Override
		protected void onEnd() {
			onSilentEnd();
		}
		
		@Override
		protected void onSilentEnd() {
			if (selected != null) {
				consume(selected);
				ade = selected.getType();
				adeac.update("§6보관§7: " + ade.getDisplayName());
			}
			HandlerList.unregisterAll(this);
			getPlayer().closeInventory();
			selected = null;
		}
		
	}
	
	class Homing extends AbilityTimer implements Listener {
		
		private Arrow arrow;
		private double lengths;
		
		private Homing(Arrow arrow, Double length) {
			super(TaskType.REVERSE, 50);
			setPeriod(TimeUnit.TICKS, 1);
			this.arrow = arrow;
			this.lengths = length;
		}
		
		@Override
		protected void onStart() {
			Bukkit.getPluginManager().registerEvents(this, AbilityWar.getPlugin());
		}
		
		@Override
		protected void run(int arg0) {
			if (arrow != null) {
				arrow.setGlowing(true);
				Player p = LocationUtil.getNearestEntity(Player.class, arrow.getLocation(), predicate);
				if (p != null) {
					if (arrow.getLocation().distanceSquared(p.getEyeLocation()) <= 5) {
						arrow.setGravity(false);
						arrow.setVelocity(VectorUtil.validateVector((p.getPlayer().getEyeLocation().toVector()
									.subtract(arrow.getLocation().toVector())).normalize().multiply(lengths)));	
					}
				}
			}
		}
		
		@EventHandler
		public void onProjectileHit(ProjectileHitEvent e) {
			if (e.getEntity().equals(arrow)) {
				if (e.getHitBlock() != null) stop(false);
				else {
					this.pause();
					arrow.setGravity(true);
					arrow.setGlowing(false);
					new BukkitRunnable() {
						@Override
						public void run() {
							stop(false);
						}
					}.runTaskLater(AbilityWar.getPlugin(), 1L);
				}
			}
		}
		
		@Override
		protected void onEnd() {
			HandlerList.unregisterAll(this);
			arrow.setGravity(true);
			arrow.setGlowing(false);
		}
		
		@Override
		protected void onSilentEnd() {
			onEnd();
		}
	}
	
}
