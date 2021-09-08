package RainStarSynergy;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Map.Entry;

import javax.annotation.Nullable;

import java.util.NoSuchElementException;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.FireworkEffect.Type;
import org.bukkit.attribute.Attribute;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Firework;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.entity.EntityDamageByBlockEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.player.PlayerArmorStandManipulateEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.material.MaterialData;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.EulerAngle;
import org.bukkit.util.Vector;

import daybreak.abilitywar.AbilityWar;
import daybreak.abilitywar.ability.AbilityBase;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.SubscribeEvent;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.ability.SubscribeEvent.Priority;
import daybreak.abilitywar.config.ability.AbilitySettings.SettingObject;
import daybreak.abilitywar.config.enums.CooldownDecrease;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.AbstractGame.Participant.ActionbarNotification.ActionbarChannel;
import daybreak.abilitywar.game.list.mix.Mix;
import daybreak.abilitywar.game.list.mix.synergy.Synergy;
import daybreak.abilitywar.game.module.DeathManager;
import daybreak.abilitywar.game.team.interfaces.Teamable;
import daybreak.abilitywar.utils.base.Formatter;
import daybreak.abilitywar.utils.base.collect.Pair;
import daybreak.abilitywar.utils.base.color.RGB;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.base.concurrent.SimpleTimer.TaskType;
import daybreak.abilitywar.utils.base.math.LocationUtil;
import daybreak.abilitywar.utils.base.math.VectorUtil;
import daybreak.abilitywar.utils.base.math.VectorUtil.Vectors;
import daybreak.abilitywar.utils.base.math.geometry.Circle;
import daybreak.abilitywar.utils.base.math.geometry.Crescent;
import daybreak.abilitywar.utils.base.math.geometry.Line;
import daybreak.abilitywar.utils.base.math.geometry.Wing;
import daybreak.abilitywar.utils.base.minecraft.FallingBlocks;
import daybreak.abilitywar.utils.base.minecraft.FallingBlocks.Behavior;
import daybreak.abilitywar.utils.base.minecraft.block.Blocks;
import daybreak.abilitywar.utils.base.minecraft.block.IBlockSnapshot;
import daybreak.abilitywar.utils.base.minecraft.entity.health.Healths;
import daybreak.abilitywar.utils.base.minecraft.nms.NMS;
import daybreak.abilitywar.utils.base.minecraft.version.ServerVersion;
import daybreak.abilitywar.utils.base.random.Random;
import daybreak.abilitywar.utils.library.BlockX;
import daybreak.abilitywar.utils.library.MaterialX;
import daybreak.abilitywar.utils.library.ParticleLib;
import daybreak.abilitywar.utils.library.SoundLib;
import daybreak.google.common.base.Predicate;
import daybreak.google.common.collect.ImmutableSet;

@AbilityManifest(
		name = "마왕", rank = Rank.L, species = Species.OTHERS, explain = {
		"§7최초 부여 §8- §c강림§f: 마왕 능력을 배정받을 때로부터 45초간 §3차원의 너머§f 속에서",
		" 자유롭게 이동할 수 있습니다. 도중에 빙의를 시도할 경우 대상 위에서 강림합니다.",
		" 강림 시 주변 일대를 15초간 §7어둠 지대§f로 만들어 지대 위 플레이어를 지속적으로",
		" 실명시키고 본인은 비행하며 §7어둠 지대§f 위에서 §8암전§f 효과가 상시 발동합니다.",
		"§7검 들고 F §8- §5시공간 절단§f: 스킬 사용 후 모든 플레이어가 이동한",
		" 시간의 흔적이 남고, 다시 능력을 사용 혹은 자동 중단할 때 범위 내의",
		" 시간의 흔적의 주인들에게 피해를 입히며 §3차원 너머§f로 보내버립니다. $[COOLDOWN]",
		" 이후 자신도 잠시간 §3차원 너머§f로 이동할 수 있습니다.",
		"§7패시브 §8- §e마력 치환§f: 회복 효과를 두 번 받을 때마다 해당 회복 효과를 절반으로 받고",
		" 대신 영구적인 §c근접 추가 공격력§f을 §d회복§f에 비례해 최대 §c5§f까지 얻을 수 있습니다.",
		" 체력이 50% 이하인 상태라면 §c추가 공격력§f을 소모해 더 많이 §d회복§f할 수 있습니다.",
		"§7패시브 §c- §8암전§f: 자신의 위치가 어두울수록 스킬 피해량이 강화됩니다.",
		" 실명을 가지고 있는 적을 공격할 때 대상에게 §c1.2배의 피해§f를 입힙니다.",
		"§7패시브 §8- §3차원 지배§f: 차원의 저편이나 너머에서 자유롭게 이동 가능합니다.",
		"§b[§7아이디어 제공자§b] keuleijeo"
		})

@SuppressWarnings("deprecation")
public class DemonLord extends Synergy {

	public DemonLord(Participant participant) {
		super(participant);
	}
	
	public static final SettingObject<Integer> COOLDOWN 
	= synergySettings.new SettingObject<Integer>(DemonLord.class,
			"cooldown", 40, "# 시공간 절단 쿨타임",
			"# 쿨타임 감소 효과를 최대 50%까지 받습니다.") {
		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}

		@Override
		public String toString() {
			return Formatter.formatCooldown(getValue());
		}
	};
	
	public static final SettingObject<Integer> DAMAGE 
	= synergySettings.new SettingObject<Integer>(DemonLord.class,
			"damage", 12, "# 시공간 절단 피해량") {
		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}

	};
	
	public static final SettingObject<Integer> DAMAGE_INCREASE 
	= synergySettings.new SettingObject<Integer>(DemonLord.class,
			"damage-increase", 40, "# 밝기가 1단계 낮을수록 증가하는 피해량", "# 단위: %", "# 밝기는 15단계로, 최소 밝기인 0일 경우 40%의 기준", "# 15 * 0.4 = 6의 추댐을 가집니다.") {
		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}

	};
	
	private boolean descend = false;
	private boolean falldamagecancel = true;
	private boolean explosioncancel = true;
	
	private final DecimalFormat df = new DecimalFormat("0");
	private final DecimalFormat df2 = new DecimalFormat("0.0");
	
	private final ActionbarChannel dimensionac = newActionbarChannel();
	
	private static final Vector zerov = new Vector(0, 0, 0);
	private static final Vector downv = new Vector(0, -5, 0);
	
	private final Circle[] circles = newCircleArray(2);
	
	private static final Circle circle1 = Circle.of(1, 5);
	private static final Circle circle2 = Circle.of(2, 10);
	private static final Circle circle3 = Circle.of(3, 15);
	private static final Circle bindingpoint = Circle.of(5, 3);
	private static final Circle bindingcircle = Circle.of(0.35, 20);
	
	private final DescendingParticles descendingparticles = new DescendingParticles();
	private Player target;
	private double getY;
	
	private final Map<Block, IBlockSnapshot> blockData = new HashMap<>();
	private Map<Player, LogParticle> logMap = new HashMap<>();
	private Set<Player> blinded = new HashSet<>();
	
	private PotionEffect blindness = new PotionEffect(PotionEffectType.BLINDNESS, 300, 2, true, false);
	private PotionEffect lightblindness = new PotionEffect(PotionEffectType.BLINDNESS, 50, 1, true, false);
	private PotionEffect confusionsight = new PotionEffect(PotionEffectType.CONFUSION, 140, 1, true, false);
	
	private final Cooldown cool = new Cooldown(COOLDOWN.getValue(), "시공간 절단", CooldownDecrease._50);
	private final int damage = DAMAGE.getValue();
	private final int increase = DAMAGE_INCREASE.getValue();
	
	private static final Circle circle = Circle.of(6, 70);

	private Set<Player> damaged = new HashSet<>();
	private final Crescent crescent = Crescent.of(3, 50);
	private static final Crescent crescent2 = Crescent.of(2, 35);
	private Map<Player, OverDimension> overMap = new HashMap<>(); 
	
	private int stack;
	private final ActionbarChannel ac = newActionbarChannel();
	private double addDamage;
	private int particleSide = 45;
	private Location darkcenter;
	private static final FixedMetadataValue NULL_VALUE = new FixedMetadataValue(AbilityWar.getPlugin(), null);
	
	private static final Set<Material> swords;
	static {
		if (MaterialX.NETHERITE_SWORD.isSupported()) {
			swords = ImmutableSet.of(MaterialX.WOODEN_SWORD.getMaterial(), Material.STONE_SWORD, Material.IRON_SWORD, MaterialX.GOLDEN_SWORD.getMaterial(), Material.DIAMOND_SWORD, MaterialX.NETHERITE_SWORD.getMaterial());
		} else {
			swords = ImmutableSet.of(MaterialX.WOODEN_SWORD.getMaterial(), Material.STONE_SWORD, Material.IRON_SWORD, MaterialX.GOLDEN_SWORD.getMaterial(), Material.DIAMOND_SWORD);
		}
	}
	
	private static Circle[] newCircleArray(final int maxAmount) {
		final Circle[] circles = new Circle[maxAmount];
		for (int i = 0; i < circles.length; i++) {
			circles[i] = Circle.of(0.75, i + 1);
		}
		return circles;
	}
	
	private static final Pair<Wing, Wing> DEMON_WING_BLACK = Wing.of(new boolean[][]{
		{false, false, false, false, false, false, false, true, true, false, false, false, false, false},
		{false, false, false, false, false, true, true, true, true, false, false, false, false, false},
		{false, false, false, false, true, true, true, true, false, false, false, false, false, false},
		{false, false, false, true, true, true, true, true, false, false, false, false, false, false},
		{false, false, true, true, true, true, true, true, true, false, false, false, false, false},
		{false, false, true, true, true, true, true, true, true, false, false, false, false, false},
		{false, true, true, true, true, true, true, true, true, true, false, false, false, false},
		{false, true, true, true, true, true, true, true, true, true, true, false, false, false},
		{true, true, true, true, true, true, true, true, true, true, true, true, false, false},
		{true, true, true, true, true, true, true, true, true, true, true, true, true, true},
		{true, true, true, true, true, true, true, true, true, true, true, true, false, false},
		{true, true, true, true, true, true, true, true, true, false, false, false, false, false},
		{true, true, true, true, true, true, true, true, false, false, false, false, false, false},
		{true, true, true, true, true, true, true, false, false, false, false, false, false, false},
		{true, true, true, true, true, true, true, false, false, false, false, false, false, false},
		{true, true, true, true, true, true, false, false, false, false, false, false, false, false},
		{true, true, true, true, true, true, false, false, false, false, false, false, false, false},
		{false, true, true, true, true, true, false, false, false, false, false, false, false, false},
		{false, true, true, true, true, false, false, false, false, false, false, false, false, false},
		{false, false, true, true, true, false, false, false, false, false, false, false, false, false},
		{false, false, true, true, false, false, false, false, false, false, false, false, false, false},
		{false, false, false, true, false, false, false, false, false, false, false, false, false, false}
	});
	
	private static final Pair<Wing, Wing> DEMON_WING_RED_LIGHT = Wing.of(new boolean[][]{
		{false, false, false, false, false, false, false, false, false, false, false, false, false, false},
		{false, false, false, false, false, false, false, false, false, false, false, false, false, false},
		{false, false, false, false, false, false, false, false, false, false, false, false, false, false},
		{false, false, false, false, false, true, false, false, false, false, false, false, false, false},
		{false, false, false, false, false, true, true, false, false, false, false, false, false, false},
		{false, false, false, false, true, true, true, false, false, false, false, false, false, false},
		{false, false, false, true, true, true, true, false, false, false, false, false, false, false},
		{false, false, false, true, true, true, false, true, true, false, false, false, false, false},
		{false, false, true, true, true, true, false, true, true, true, false, false, false, false},
		{false, false, true, true, true, true, false, true, true, true, false, false, false, false},
		{false, false, true, true, true, false, true, true, true, true, true, false, false, false},
		{false, false, true, true, true, false, true, true, true, false, false, false, false, false},
		{false, false, true, true, true, false, true, true, false, false, false, false, false, false},
		{false, false, true, true, true, false, true, false, false, false, false, false, false, false},
		{false, false, true, true, true, false, true, false, false, false, false, false, false, false},
		{false, false, true, true, true, true, false, false, false, false, false, false, false, false},
		{false, false, false, true, true, true, false, false, false, false, false, false, false, false},
		{false, false, false, true, true, true, false, false, false, false, false, false, false, false},
		{false, false, false, false, true, false, false, false, false, false, false, false, false, false},
		{false, false, false, false, false, false, false, false, false, false, false, false, false, false},
		{false, false, false, false, false, false, false, false, false, false, false, false, false, false},
		{false, false, false, false, false, false, false, false, false, false, false, false, false, false}
	});
	
	private static final Pair<Wing, Wing> DEMON_WING_RED = Wing.of(new boolean[][]{
		{false, false, false, false, false, false, false, false, false, false, false, false, false, false},
		{false, false, false, false, false, false, false, false, false, false, false, false, false, false},
		{false, false, false, false, false, false, false, false, false, false, false, false, false, false},
		{false, false, false, false, false, true, false, false, false, false, false, false, false, false},
		{false, false, false, false, false, true, true, false, false, false, false, false, false, false},
		{false, false, false, false, true, true, true, false, false, false, false, false, false, false},
		{false, false, false, true, true, true, true, false, false, false, false, false, false, false},
		{false, false, false, true, true, true, false, true, true, false, false, false, false, false},
		{false, false, true, true, true, true, false, true, true, true, false, false, false, false},
		{false, false, true, true, true, true, false, true, true, true, false, false, false, false},
		{false, false, true, true, true, false, true, true, true, true, true, false, false, false},
		{false, false, true, true, true, false, true, true, true, false, false, false, false, false},
		{false, false, true, true, true, false, true, true, false, false, false, false, false, false},
		{false, false, true, true, true, false, true, false, false, false, false, false, false, false},
		{false, false, true, true, true, false, true, false, false, false, false, false, false, false},
		{false, false, true, true, true, true, false, false, false, false, false, false, false, false},
		{false, false, false, true, true, true, false, false, false, false, false, false, false, false},
		{false, false, false, true, true, true, false, false, false, false, false, false, false, false},
		{false, false, false, false, true, false, false, false, false, false, false, false, false, false},
		{false, false, false, false, false, false, false, false, false, false, false, false, false, false},
		{false, false, false, false, false, false, false, false, false, false, false, false, false, false},
		{false, false, false, false, false, false, false, false, false, false, false, false, false, false}
	});
	
	private static final Pair<Wing, Wing> DEMON_WING_RED_DARK = Wing.of(new boolean[][]{
		{false, false, false, false, false, false, false, false, false, false, false, false, false, false},
		{false, false, false, false, false, false, false, false, false, false, false, false, false, false},
		{false, false, false, false, false, false, false, false, false, false, false, false, false, false},
		{false, false, false, false, false, true, false, false, false, false, false, false, false, false},
		{false, false, false, false, false, true, true, false, false, false, false, false, false, false},
		{false, false, false, false, true, true, true, false, false, false, false, false, false, false},
		{false, false, false, true, true, true, true, false, false, false, false, false, false, false},
		{false, false, false, true, true, true, false, true, true, false, false, false, false, false},
		{false, false, true, true, true, true, false, true, true, true, false, false, false, false},
		{false, false, true, true, true, true, false, true, true, true, false, false, false, false},
		{false, false, true, true, true, false, true, true, true, true, true, false, false, false},
		{false, false, true, true, true, false, true, true, true, false, false, false, false, false},
		{false, false, true, true, true, false, true, true, false, false, false, false, false, false},
		{false, false, true, true, true, false, true, false, false, false, false, false, false, false},
		{false, false, true, true, true, false, true, false, false, false, false, false, false, false},
		{false, false, true, true, true, true, false, false, false, false, false, false, false, false},
		{false, false, false, true, true, true, false, false, false, false, false, false, false, false},
		{false, false, false, true, true, true, false, false, false, false, false, false, false, false},
		{false, false, false, false, true, false, false, false, false, false, false, false, false, false},
		{false, false, false, false, false, false, false, false, false, false, false, false, false, false},
		{false, false, false, false, false, false, false, false, false, false, false, false, false, false},
		{false, false, false, false, false, false, false, false, false, false, false, false, false, false}
	});
	
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
	
	private final Predicate<Block> blockpredicate = new Predicate<Block>() {
		@Override
		public boolean test(Block block) {
			if (!block.getType().isSolid() && !block.isLiquid()) {
				return true;
			}
			return false;
		}

		@Override
		public boolean apply(@Nullable Block arg0) {
			return false;
		}
	};
	
	@Override
	protected void onUpdate(Update update) {
		if (update == Update.RESTRICTION_CLEAR && !descend) {
			Healths.setHealth(getPlayer(), getPlayer().getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue());
			getPlayer().sendMessage("§8[§7!§8] §f당신의 능력이 §8마왕§f으로 §e변경§f되었습니다. §7/aw check");
			getPlayer().sendMessage("§8[§7!§8] §3차원의 너머§f로 은신합니다...");
			ParticleLib.DRAGON_BREATH.spawnParticle(getPlayer().getEyeLocation(), 0.2, 0.2, 0.2, 50, 0);
			SoundLib.ENTITY_BAT_TAKEOFF.playSound(getPlayer().getLocation(), 1, 0.7f);
			dimensionBeyond.start();
			descend = true;
		}
		if (update == Update.RESTRICTION_CLEAR) {
			
		}
	}
	
	private final AbilityTimer dimensionBeyond = new AbilityTimer(TaskType.REVERSE, 900) {
		
		@Override
		public void onStart() {
			getParticipant().attributes().TARGETABLE.setValue(false);
		}
		
		@Override
		public void run(int count) {
			getPlayer().setGameMode(GameMode.SPECTATOR);
			dimensionac.update("§3차원 너머§7: §f" + df.format(count * 0.05) + "초");
			if (getPlayer().getSpectatorTarget() != null && getPlayer().getSpectatorTarget() instanceof Player) {
				target = (Player) getPlayer().getSpectatorTarget();
				stop(false);
			}
	   	}
	   	
	   	@Override
	   	public void onEnd() {
	   		onSilentEnd();
	   	}
	    	
	   	@Override
	    public void onSilentEnd() {
	   		if (getPlayer().getSpectatorTarget() != null) {
		   		getY = getPlayer().getSpectatorTarget().getLocation().getY() + 3;
		   		getPlayer().teleport(getPlayer().getSpectatorTarget().getLocation().clone().add(0, 250, 0).setDirection(new Vector(0, 0, 0)), TeleportCause.PLUGIN);	
		   		getPlayer().setGameMode(GameMode.SURVIVAL);
		   		descending.start();
	   		} else {
		   		getPlayer().setGameMode(GameMode.SURVIVAL);	
	   		}
			dimensionac.update(null);
	   	}
	
	}.setBehavior(RestrictionBehavior.PAUSE_RESUME).setPeriod(TimeUnit.TICKS, 1).register();
	
	private final AbilityTimer descending = new AbilityTimer() {
		
    	@Override
		public void onStart() {
			getPlayer().setGameMode(GameMode.SPECTATOR);
			descendingparticles.start();
			if (descendingparticles.dparticles.size() != 1) {
				descendingparticles.add(new DescendingParticle(getPlayer(), getPlayer().getLocation()));	
			}
			SoundLib.ITEM_ELYTRA_FLYING.playSound(getPlayer(), 30, 1.5f);
			SoundLib.ENTITY_WITHER_SPAWN.playSound(target.getLocation(), 1.5f, 1.25f);
			SoundLib.ENTITY_WITHER_HURT.playSound(target.getLocation(), 0.85f, 0.5f);
    	}
		
		@Override
		public void run(int count) {
			if (getY >= getPlayer().getLocation().getY() || getY <= 3) stop(false);
			if (count % 10 == 0 && target != null) {
				ParticleLib.BARRIER.spawnParticle(target.getLocation().clone().add(0, 2.5, 0));
				for (Location circleloc : bindingpoint.toLocations(target.getLocation()).floor(target.getLocation().getY())) {
					for (Location loc : Line.of(circleloc.toVector().subtract(target.getEyeLocation().toVector()), 70).toLocations(target.getLocation().clone().add(0, 1, 0))) {
						ParticleLib.TOWN_AURA.spawnParticle(loc);
					}
					for (Location loc : Line.of(circleloc.toVector().subtract(target.getEyeLocation().toVector()), 30).toLocations(target.getLocation().clone().add(0, 1, 0))) {
						ParticleLib.REDSTONE.spawnParticle(loc, RGB.GRAY);
					}
				}
				for (Location binding : bindingcircle.toLocations(target.getLocation()).floor(target.getLocation().getY())) {
					ParticleLib.TOWN_AURA.spawnParticle(binding.clone().add(0, 1, 0));
				}
			}
			getPlayer().setVelocity(VectorUtil.validateVector(downv));
	   	}
		
		@Override
		public void onEnd() {
			onSilentEnd();
		}
		
	   	@Override
	    public void onSilentEnd() {
	   		descendingparticles.dparticles.remove(0);
			getPlayer().setVelocity(zerov);
			getParticipant().attributes().TARGETABLE.setValue(true);
			getPlayer().setGameMode(GameMode.SURVIVAL);
			getPlayer().setAllowFlight(false);
			getPlayer().setFlying(false);
			getPlayer().teleport(target.getLocation().setDirection(getPlayer().getLocation().getDirection()));
			fieldparticle.start();
			target = null;
			final Firework firework = getPlayer().getWorld().spawn(getPlayer().getEyeLocation(), Firework.class);
			final FireworkMeta meta = firework.getFireworkMeta();
			meta.addEffect(
					FireworkEffect.builder()
							.withColor(Color.BLACK)
							.with(Type.BALL_LARGE)
							.build()
			);
			meta.setPower(0);
			firework.setFireworkMeta(meta);
			firework.setMetadata("DemonLord", NULL_VALUE);
			new BukkitRunnable() {
				@Override
				public void run() {
					firework.detonate();
				}
			}.runTaskLater(AbilityWar.getPlugin(), 1L);
	   	}
		
	}.setPeriod(TimeUnit.TICKS, 1).register();
	
	private final AbilityTimer fieldparticle = new AbilityTimer() {
		
		@Override
		public void onStart() {
			ParticleLib.EXPLOSION_HUGE.spawnParticle(getPlayer().getLocation());
			getPlayer().getWorld().createExplosion(getPlayer().getLocation(), 1.7f, false, false);
			SoundLib.ENTITY_GENERIC_EXPLODE.playSound(getPlayer().getLocation(), 1.5f, 0.8f);
		}
		
		@Override
		public void run(int count) {
			switch(count) {
			case 1:
				for (Location loc : circle1.toLocations(getPlayer().getLocation()).floor(getPlayer().getLocation().getY())) {
					if (ServerVersion.getVersion() >= 13) {
						for (Block block : LocationUtil.getBlocks2D(loc, 2, true, true, true)) {
							if (block.getType() == Material.AIR) block = block.getRelative(BlockFace.DOWN);
							if (block.getType() == Material.AIR) continue;
							Location location = block.getLocation().add(0, 1, 0);
							FallingBlocks.spawnFallingBlock(location, block.getType(), false, getPlayer().getLocation().toVector().subtract(location.toVector()).multiply(-0.05).setY(Math.random()), Behavior.FALSE);
						}
					} else {
						for (Block block : LocationUtil.getBlocks2D(loc, 2, true, true, true)) {
							if (block.getType() == Material.AIR) block = block.getRelative(BlockFace.DOWN);
							if (block.getType() == Material.AIR) continue;
							Location location = block.getLocation().add(0, 1, 0);
							FallingBlocks.spawnFallingBlock(location, block.getType(), block.getData(), false, getPlayer().getLocation().toVector().subtract(location.toVector()).multiply(-0.05).setY(Math.random()), Behavior.FALSE);
						}
					}
				}
				SoundLib.ENTITY_ZOMBIE_BREAK_WOODEN_DOOR.playSound(getPlayer().getLocation(), 1, 0.5f);
				break;
			case 5:
				for (Location loc : circle2.toLocations(getPlayer().getLocation()).floor(getPlayer().getLocation().getY())) {
					if (ServerVersion.getVersion() >= 13) {
						for (Block block : LocationUtil.getBlocks2D(loc, 2, true, true, true)) {
							if (block.getType() == Material.AIR) block = block.getRelative(BlockFace.DOWN);
							if (block.getType() == Material.AIR) continue;
							Location location = block.getLocation().add(0, 1, 0);
							FallingBlocks.spawnFallingBlock(location, block.getType(), false, getPlayer().getLocation().toVector().subtract(location.toVector()).multiply(-0.05).setY(Math.random()), Behavior.FALSE);
						}
					} else {
						for (Block block : LocationUtil.getBlocks2D(loc, 2, true, true, true)) {
							if (block.getType() == Material.AIR) block = block.getRelative(BlockFace.DOWN);
							if (block.getType() == Material.AIR) continue;
							Location location = block.getLocation().add(0, 1, 0);
							FallingBlocks.spawnFallingBlock(location, block.getType(), block.getData(), false, getPlayer().getLocation().toVector().subtract(location.toVector()).multiply(-0.05).setY(Math.random()), Behavior.FALSE);
						}
					}
				}
				SoundLib.ENTITY_ZOMBIE_BREAK_WOODEN_DOOR.playSound(getPlayer().getLocation(), 1, 0.6f);
				break;
			case 10:
				for (Location loc : circle3.toLocations(getPlayer().getLocation()).floor(getPlayer().getLocation().getY())) {
					if (ServerVersion.getVersion() >= 13) {
						for (Block block : LocationUtil.getBlocks2D(loc, 2, true, true, true)) {
							if (block.getType() == Material.AIR) block = block.getRelative(BlockFace.DOWN);
							if (block.getType() == Material.AIR) continue;
							Location location = block.getLocation().add(0, 1, 0);
							FallingBlocks.spawnFallingBlock(location, block.getType(), false, getPlayer().getLocation().toVector().subtract(location.toVector()).multiply(-0.05).setY(Math.random()), Behavior.FALSE);
						}
					} else {
						for (Block block : LocationUtil.getBlocks2D(loc, 2, true, true, true)) {
							if (block.getType() == Material.AIR) block = block.getRelative(BlockFace.DOWN);
							if (block.getType() == Material.AIR) continue;
							Location location = block.getLocation().add(0, 1, 0);
							FallingBlocks.spawnFallingBlock(location, block.getType(), block.getData(), false, getPlayer().getLocation().toVector().subtract(location.toVector()).multiply(-0.05).setY(Math.random()), Behavior.FALSE);
						}
					}
				}
				SoundLib.ENTITY_ZOMBIE_BREAK_WOODEN_DOOR.playSound(getPlayer().getLocation(), 1, 0.7f);
				break;
			case 20:
				darkfield.start();
				stop(false);
				break;
			}
		}
		
	}.setPeriod(TimeUnit.TICKS, 1).register();
	
	private final AbilityTimer darkfield = new AbilityTimer(300) {
		
		private Wing leftWing1, leftWing2, leftWing3, leftWing4,
					rightWing1, rightWing2, rightWing3, rightWing4;
		private RGB black = RGB.of(1, 1, 1), red = RGB.of(208, 1, 1), darkred = RGB.of(128, 1, 1), lightred = RGB.of(254, 34, 34);
		
		@Override
		public void onStart() {
			this.leftWing1 = DEMON_WING_BLACK.getLeft().clone();
			this.rightWing1 = DEMON_WING_BLACK.getRight().clone();
			this.leftWing2 = DEMON_WING_RED.getLeft().clone();
			this.rightWing2 = DEMON_WING_RED.getRight().clone();
			this.leftWing3 = DEMON_WING_RED_DARK.getLeft().clone();
			this.rightWing3 = DEMON_WING_RED_DARK.getRight().clone();
			this.leftWing4 = DEMON_WING_RED_LIGHT.getLeft().clone();
			this.rightWing4 = DEMON_WING_RED_LIGHT.getRight().clone();
			SoundLib.BLOCK_END_PORTAL_SPAWN.playSound(getPlayer().getLocation(), 2, 0.5f);
			darkcenter = LocationUtil.floorY(getPlayer().getLocation(), blockpredicate).add(0, -1, 0);
			for (int count = 0; count < 20; count++) {
				for (Block block : LocationUtil.getBlocks2D(darkcenter, count, true, true, true)) {
					Block belowBlock = block.getRelative(BlockFace.DOWN);
					if (MaterialX.BLACK_CONCRETE.compare(belowBlock)) {
						block = belowBlock;
						belowBlock = belowBlock.getRelative(BlockFace.DOWN);
					}
					blockData.putIfAbsent(belowBlock, Blocks.createSnapshot(belowBlock));
					BlockX.setType(belowBlock, MaterialX.BLACK_CONCRETE);
					ParticleLib.FALLING_DUST.spawnParticle(block.getLocation().clone().add(0, 1.5, 0), 1, 1, 1, 2, 0);
				}
			}
		}
		
		@Override
		public void run(int count) {
			if (count % 5 == 0 && getPlayer().isFlying()) {
				float yaw = getPlayer().getLocation().getYaw();
				for (Location loc : leftWing1.rotateAroundAxisY(-yaw).toLocations(getPlayer().getLocation().clone().subtract(0, 1.5, 0))) {
					ParticleLib.REDSTONE.spawnParticle(loc, black);
				}
				leftWing1.rotateAroundAxisY(yaw);
				for (Location loc : leftWing2.rotateAroundAxisY(-yaw).toLocations(getPlayer().getLocation().clone().subtract(0, 1.5, 0))) {
					ParticleLib.REDSTONE.spawnParticle(loc, red);
				}
				leftWing2.rotateAroundAxisY(yaw);
				for (Location loc : leftWing3.rotateAroundAxisY(-yaw).toLocations(getPlayer().getLocation().clone().subtract(0, 1.5, 0))) {
					ParticleLib.REDSTONE.spawnParticle(loc, darkred);
				}
				leftWing3.rotateAroundAxisY(yaw);
				for (Location loc : leftWing4.rotateAroundAxisY(-yaw).toLocations(getPlayer().getLocation().clone().subtract(0, 1.5, 0))) {
					ParticleLib.REDSTONE.spawnParticle(loc, lightred);
				}
				leftWing4.rotateAroundAxisY(yaw);
				for (Location loc : rightWing1.rotateAroundAxisY(-yaw).toLocations(getPlayer().getLocation().clone().subtract(0, 1.5, 0))) {
					ParticleLib.REDSTONE.spawnParticle(loc, black);
				}
				rightWing1.rotateAroundAxisY(yaw);
				for (Location loc : rightWing2.rotateAroundAxisY(-yaw).toLocations(getPlayer().getLocation().clone().subtract(0, 1.5, 0))) {
					ParticleLib.REDSTONE.spawnParticle(loc, red);
				}
				rightWing2.rotateAroundAxisY(yaw);
				for (Location loc : rightWing3.rotateAroundAxisY(-yaw).toLocations(getPlayer().getLocation().clone().subtract(0, 1.5, 0))) {
					ParticleLib.REDSTONE.spawnParticle(loc, darkred);
				}
				rightWing3.rotateAroundAxisY(yaw);
				for (Location loc : rightWing4.rotateAroundAxisY(-yaw).toLocations(getPlayer().getLocation().clone().subtract(0, 1.5, 0))) {
					ParticleLib.REDSTONE.spawnParticle(loc, lightred);
				}
				rightWing4.rotateAroundAxisY(yaw);
			}
			getPlayer().setAllowFlight(true);
			for (Player p : LocationUtil.getEntitiesInCircle(Player.class, darkcenter, 20, predicate)) {
				if (!blinded.contains(p)) {
					p.addPotionEffect(blindness);
					blinded.add(p);
				}
			}
			for (Player p : getPlayer().getWorld().getPlayers()) {
				if (!LocationUtil.isInCircle(darkcenter, p.getLocation(), 20) && blinded.contains(p)) {
					p.removePotionEffect(PotionEffectType.BLINDNESS);
					blinded.remove(p);
				}
			}
		}
		
		@Override
		public void onEnd() {
			onSilentEnd();
		}
		
		@Override
		public void onSilentEnd() {
			for (Player p : LocationUtil.getEntitiesInCircle(Player.class, darkcenter, 20, predicate)) {
				p.removePotionEffect(PotionEffectType.BLINDNESS);
			}
			getPlayer().setAllowFlight(getPlayer().getGameMode() != GameMode.SURVIVAL && getPlayer().getGameMode() != GameMode.ADVENTURE);
			getPlayer().setFlying(getPlayer().getGameMode() != GameMode.SURVIVAL && getPlayer().getGameMode() != GameMode.ADVENTURE);
			for (Entry<Block, IBlockSnapshot> entry : blockData.entrySet()) {
				Block key = entry.getKey();
				if (MaterialX.BLACK_CONCRETE.compare(key)) {
					entry.getValue().apply();
				}
			}
		}
		
	}.setPeriod(TimeUnit.TICKS, 1).register();
	
	@SubscribeEvent
	public void onEntityDamage(EntityDamageEvent e) {
		if (e.getEntity().equals(getPlayer())) {
			if (e.getCause().equals(DamageCause.FALL) && falldamagecancel) {
				e.setCancelled(true);
				falldamagecancel = false;
			}
			if ((e.getCause().equals(DamageCause.BLOCK_EXPLOSION) || e.getCause().equals(DamageCause.ENTITY_EXPLOSION)) && explosioncancel) {
				e.setCancelled(true);
				explosioncancel = false;
			}
		}
		if (e.getEntity().equals(target) && descending.isRunning()) {
			e.setCancelled(true);
		}
	}
	
	@SubscribeEvent(priority = Priority.HIGHEST)
	public void onPlayerMove(PlayerMoveEvent e) {
		if (target != null) {
			if (target.equals(e.getPlayer())) e.setTo(e.getFrom());
		}
		if (e.getPlayer().equals(getPlayer()) && getPlayer().getGameMode().equals(GameMode.SPECTATOR)) {
			e.setTo(e.getTo());
		}
	}
	
	@SubscribeEvent
	public void onEntityDamageByBlock(EntityDamageByBlockEvent e) {
		onEntityDamage(e);
	}
	
	@SubscribeEvent
	public void onEntityDamageByEntity(EntityDamageByEntityEvent e) {
		onEntityDamage(e);
		if (e.getDamager().hasMetadata("DemonLord")) {
			e.setCancelled(true);
		}
		if (e.getDamager().equals(getPlayer())) {
			if (addDamage >= 0.5) {
				new PassiveCutParticle(particleSide).start();
				particleSide *= -1;
			}
			if (e.getEntity() instanceof Player) {
				Player p = (Player) e.getEntity();
				if (p.hasPotionEffect(PotionEffectType.BLINDNESS)) {
					e.setDamage(e.getDamage() * 1.2);
					ParticleLib.ITEM_CRACK.spawnParticle(p.getLocation(), .5f, 1f, .5f, 100, 0.35, MaterialX.BLACK_CONCRETE);
				}
			}
			e.setDamage(e.getDamage() + addDamage);
		}
	}
	
	@SubscribeEvent
	public void onBlockBreak(BlockBreakEvent e) {
		if (blockData.containsKey(e.getBlock())) {
			if (skill.isRunning()) e.setCancelled(true);
		}
	}

	@SubscribeEvent
	public void onExplode(BlockExplodeEvent e) {
		e.blockList().removeIf(blockData::containsKey);
	}

	@SubscribeEvent
	public void onExplode(EntityExplodeEvent e) {
		e.blockList().removeIf(blockData::containsKey);
	}
	
	@SubscribeEvent(onlyRelevant = true)
	public void onEntityRegainHealth(EntityRegainHealthEvent e) {
		stack++;
		if (stack == 2) {
			double maxHealth = getPlayer().getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue();
			if (addDamage >= 1) {
				if (getPlayer().getHealth() <= maxHealth * 0.5) {
					e.setAmount(e.getAmount() + 2);
					addDamage = Math.max(0, addDamage - 0.5);
				} else {
					if (addDamage != 5) {
						e.setAmount(e.getAmount() * 0.5);
						addDamage = Math.min(5, addDamage + e.getAmount());	
					}
				}
			} else {
				e.setAmount(e.getAmount() * 0.5);
				addDamage = Math.min(5, addDamage + e.getAmount());
			}
			ac.update("§c추가 공격력§f: " + df2.format(addDamage));
			stack = 0;
		}
	}
	
	private class DescendingParticles extends AbilityTimer {

		@SuppressWarnings("serial")
		private final List<DescendingParticle> dparticles = new ArrayList<DescendingParticle>() {
			@Override
			public boolean add(DescendingParticle o) {
				if (size() < 2)
					return super.add(o);
				return false;
			}
		};

		private double rotation = 0.0;

		private DescendingParticles() {
			setPeriod(TimeUnit.TICKS, 1);
		}

		private void add(DescendingParticle DescendingParticle) {
			dparticles.add(DescendingParticle);
		}

		@Override
		protected void run(int count) {
			if (dparticles.isEmpty())
				return;
			final List<Vector> circle = circles[dparticles.size() - 1].clone().rotateAroundAxisY(rotation += 40);
			final Location playerLocation = getPlayer().getLocation().clone().add(0, 1, 0);
			for (int i = 0; i < (circle.size() - 1); i++) {
				if (i < dparticles.size()) {
					final Vector vector = circle.get(i);
					final DescendingParticle particle = dparticles.get(i);
					particle.updateLocation(playerLocation.clone().add(vector));
				}
			}
		}

		@Override
		protected void onEnd() {
			onSilentEnd();
		}
	}
	
	public class DescendingParticle {

		private int stacks = 0;
		private boolean turns = true;

		private RGB Agradation, Bgradation;

		private final RGB Agradation1 = RGB.of(1, 1, 1), Agradation2 = RGB.of(1, 2, 17), Agradation3 = RGB.of(2, 4, 34),
				Agradation4 = RGB.of(3, 6, 52), Agradation5 = RGB.of(4, 8, 70), Agradation6 = RGB.of(5, 10, 87),
				Agradation7 = RGB.of(7, 13, 106), Agradation8 = RGB.of(8, 15, 123), Agradation9 = RGB.of(10, 18, 140),
				Agradation10 = RGB.of(12, 20, 150), Agradation11 = RGB.of(14, 22, 159), Agradation12 = RGB.of(16, 23, 168),
				Agradation13 = RGB.of(19, 25, 179), Agradation14 = RGB.of(22, 27, 189), Agradation15 = RGB.of(24, 28, 199),
				Agradation16 = RGB.of(50, 21, 203), Agradation17 = RGB.of(74, 15, 207), Agradation18 = RGB.of(112, 5, 212),
				Agradation19 = RGB.of(122, 25, 188), Agradation20 = RGB.of(131, 44, 166), Agradation21 = RGB.of(140, 61, 145);
		
		private final RGB Bgradation1 = RGB.of(1, 1, 1), Bgradation2 = RGB.of(1, 1, 17), Bgradation3 = RGB.of(1, 1, 32),
				Bgradation4 = RGB.of(1, 1, 47), Bgradation5 = RGB.of(1, 1, 64), Bgradation6 = RGB.of(10, 1, 64),
				Bgradation7 = RGB.of(21, 1, 64), Bgradation8 = RGB.of(32, 1, 64), Bgradation9 = RGB.of(43, 1, 64),
				Bgradation10 = RGB.of(53, 1, 64), Bgradation11 = RGB.of(64, 1, 64), Bgradation12 = RGB.of(80, 1, 64),
				Bgradation13 = RGB.of(96, 1, 64), Bgradation14 = RGB.of(113, 1, 64), Bgradation15 = RGB.of(128, 1, 64),
				Bgradation16 = RGB.of(143, 1, 64), Bgradation17 = RGB.of(176, 38, 43), Bgradation18 = RGB.of(209, 77, 22),
				Bgradation19 = RGB.of(242, 115, 1), Bgradation20 = RGB.of(249, 153, 1), Bgradation21 = RGB.of(254, 191, 1);

		@SuppressWarnings("serial")
		private List<RGB> Agradations = new ArrayList<RGB>() {
			{
				add(Agradation1);
				add(Agradation2);
				add(Agradation3);
				add(Agradation4);
				add(Agradation5);
				add(Agradation6);
				add(Agradation7);
				add(Agradation8);
				add(Agradation9);
				add(Agradation10);
				add(Agradation11);
				add(Agradation12);
				add(Agradation13);
				add(Agradation14);
				add(Agradation15);
				add(Agradation16);
				add(Agradation17);
				add(Agradation18);
				add(Agradation19);
				add(Agradation20);
				add(Agradation21);
			}
		};
			
		@SuppressWarnings("serial")
		private List<RGB> Bgradations = new ArrayList<RGB>() {
			{
				add(Bgradation21);
				add(Bgradation20);
				add(Bgradation19);
				add(Bgradation18);
				add(Bgradation17);
				add(Bgradation16);
				add(Bgradation15);
				add(Bgradation14);
				add(Bgradation13);
				add(Bgradation12);
				add(Bgradation11);
				add(Bgradation10);
				add(Bgradation9);
				add(Bgradation8);
				add(Bgradation7);
				add(Bgradation6);
				add(Bgradation5);
				add(Bgradation4);
				add(Bgradation3);
				add(Bgradation2);
				add(Bgradation1);
			}
		};

		private DescendingParticle(LivingEntity shooter, Location startLocation) {
			this.lastLocation = startLocation;
		}

		private Location lastLocation;

		private void updateLocation(final Location newLocation) {
			if (turns)
				stacks++;
			else
				stacks--;
			if (stacks % (Agradations.size() - 1) == 0) {
				turns = !turns;
			}
			Agradation = Agradations.get(stacks);
			Bgradation = Bgradations.get(stacks);
			for (Iterator<Location> iterator = new Iterator<Location>() {
				private final Vector vectorBetween = newLocation.toVector().subtract(lastLocation.toVector()),
						unit = vectorBetween.clone().normalize().multiply(.1);
				private final int amount = (int) (vectorBetween.length() / .1);
				private int cursor = 0;

				@Override
				public boolean hasNext() {
					return cursor < amount;
				}

				@Override
				public Location next() {
					if (cursor >= amount)
						throw new NoSuchElementException();
					cursor++;
					return lastLocation.clone().add(unit.clone().multiply(cursor));
				}
			};iterator.hasNext();) {
				final Location location = iterator.next();
				final Location opposite = getPlayer().getLocation().toVector().add(getPlayer().getLocation().toVector().subtract(location.toVector())).toLocation(getPlayer().getWorld());
				final Location center = new Location(getPlayer().getWorld(), (opposite.getX() + location.getX()) / 2, opposite.getY(), (opposite.getZ() + location.getZ()) / 2);
				opposite.setY(location.getY());
				center.setY(location.getY() + 1);
				ParticleLib.SMOKE_LARGE.spawnParticle(center, 0, 0, 0, 1, 0);
				ParticleLib.REDSTONE.spawnParticle(location, Agradation);
				ParticleLib.REDSTONE.spawnParticle(opposite, Bgradation);
			}
			lastLocation = newLocation;
		}

	}
	
	@SubscribeEvent
	private void onArmorStandManipulate(PlayerArmorStandManipulateEvent e) {
		if (e.getRightClicked().hasMetadata("TimeCutter")) e.setCancelled(true);
	}
	
    @SubscribeEvent(onlyRelevant = true)
    public void onPlayerSwapHandItems(PlayerSwapHandItemsEvent e) {
    	if (swords.contains(e.getOffHandItem().getType())) {
    		if (!cool.isCooldown()) {
        		if (skill.isRunning()) {
        			if (skill.getCount() >= 7) {
        				getPlayer().sendMessage("§4[§c!§4] §f아직 최소한의 검의 힘이 모아지지 않았습니다.");
        				getPlayer().sendMessage("§4[§c!§4] §3최소 대기 시간§f: 앞으로 §e" + Math.abs(6 - skill.getCount()) + "§f초");
        			} else {
        				skill.stop(false);
        			}
        		} else {
            		skill.start();	
        		}	
    		}
    		e.setCancelled(true);
    	}
    }
    
	private final AbilityTimer skillcircle = new AbilityTimer(50) {
		
		@Override
		public void run(int count) {
			for (Location loc : circle.toLocations(getPlayer().getLocation()).floor(getPlayer().getLocation().getY())) {
				ParticleLib.REDSTONE.spawnParticle(getPlayer(), loc, RGB.of(77, 77, 77));
			}
		}
		
	}.setPeriod(TimeUnit.TICKS, 4).register();
    
	private final AbilityTimer skill = new AbilityTimer(TaskType.REVERSE, 10) {
		
		@Override
		public void onStart() {
			SoundLib.BLOCK_END_PORTAL_SPAWN.playSound(getPlayer(), 0.75f, 0.75f);
			for (Participant participant : getGame().getParticipants()) {
				if (predicate.test(participant.getPlayer())) {
					new LogParticle(participant.getPlayer()).start();	
				}
			}
			skillcircle.start();
		}
		
		@Override
		public void run(int count) {
			if (count <= 3) {
				getPlayer().sendMessage("§4[§c!§4] §e" + skill.getCount() + "§f초 후 즉시 §3사용§f됩니다!");
				SoundLib.BLOCK_NOTE_BLOCK_SNARE.playSound(getPlayer(), 1, 1.7f);
			}
		}
		
		@Override
		public void onEnd() {
			onSilentEnd();
		}
		
		@Override
		public void onSilentEnd() {
			new CutParticle(180).start();
			SoundLib.ENTITY_PLAYER_ATTACK_SWEEP.playSound(getPlayer().getLocation(), 1, 1.2f);
			skillcircle.stop(false);
			SoundLib.ENTITY_EXPERIENCE_ORB_PICKUP.playSound(getPlayer());
			for (Participant participant : getGame().getParticipants()) {
				if (predicate.test(participant.getPlayer()) && logMap.containsKey(participant.getPlayer())) {
					List<Location> locations = logMap.get(participant.getPlayer()).getLog();
					for (int a=0; a<(locations.size() - 1); a++) {
						if (LocationUtil.isInCircle(getPlayer().getLocation(), locations.get(a), 6)) {
							if (!damaged.contains(participant.getPlayer())) {
								ArmorStand armorstand = participant.getPlayer().getWorld().spawn(participant.getPlayer().getEyeLocation().clone().add(0, 20, 0), ArmorStand.class);
				            	armorstand.setRightArmPose(new EulerAngle(Math.toRadians(80), 0, 0));
				            	armorstand.setMetadata("TimeCutter", new FixedMetadataValue(AbilityWar.getPlugin(), null));
				            	armorstand.setVisible(false);
				            	armorstand.getEquipment().setItemInMainHand(new ItemStack(Material.IRON_SWORD));
				            	armorstand.setGravity(true);
				            	NMS.removeBoundingBox(armorstand);
				            	armorstand.setVelocity(new Vector(0, -0.75, 0));
				            	
				            	ParticleLib.FALLING_DUST.spawnParticle(armorstand.getLocation().clone().add(0, 1, 0), 0.25, 0.25, 0.25, 10, 0, new MaterialData(Material.DRAGON_EGG));
				            	
				            	SoundLib.ENTITY_WITHER_SHOOT.playSound(participant.getPlayer(), 0.8f, 0.65f);
				            	
								ArmorStand clock = locations.get(a).getWorld().spawn(locations.get(a), ArmorStand.class);
								clock.setRightArmPose(new EulerAngle(Math.toRadians(270), 0, 0));
								clock.setMetadata("TimeCutter", new FixedMetadataValue(AbilityWar.getPlugin(), null));
								clock.setVisible(false);
								clock.getEquipment().setItemInMainHand(new ItemStack(MaterialX.CLOCK.getMaterial()));
								clock.setGravity(false);
				            	NMS.removeBoundingBox(clock);
				    			new BukkitRunnable() {
				    				@Override
				    				public void run() {
			    						double skillDamage = damage + ((15 - getPlayer().getLocation().clone().add(0, 1, 0).getBlock().getLightLevel()) * (increase * 0.01));
			    						double maxDamage = damage + (15 * (increase * 0.01));
				    					if (darkfield.isRunning()) {
				    						if (!LocationUtil.isInCircle(darkcenter, getPlayer().getLocation(), 20)) {
					    						participant.getPlayer().damage(skillDamage, getPlayer());	
					    					} else {
					    						participant.getPlayer().damage(maxDamage, getPlayer());
					    					}	
				    					} else participant.getPlayer().damage(skillDamage, getPlayer());
				    					ParticleLib.ITEM_CRACK.spawnParticle(clock.getLocation().clone().add(0, 1, 0), 0, 0, 0, 10, 0.5f, MaterialX.CLOCK);
				    					ParticleLib.ITEM_CRACK.spawnParticle(participant.getPlayer().getEyeLocation(), 0.4, 0.5, 0.4, 50, 0.35, MaterialX.CLOCK);
				    					SoundLib.ENTITY_ENDER_EYE_DEATH.playSound(clock.getLocation().clone().add(0, 1, 0), 1, 0.7f);
				    					SoundLib.ENTITY_GENERIC_EXPLODE.playSound(participant.getPlayer().getLocation());
				    					if (ServerVersion.getVersion() >= 13) {
				    						for (Block block : LocationUtil.getBlocks2D(participant.getPlayer().getLocation(), 2, true, true, true)) {
				    							if (block.getType() == Material.AIR) block = block.getRelative(BlockFace.DOWN);
				    							if (block.getType() == Material.AIR) continue;
				    							Location location = block.getLocation().add(0, 1, 0);
				    							FallingBlocks.spawnFallingBlock(location, block.getType(), false, getPlayer().getLocation().toVector().subtract(location.toVector()).multiply(-0.05).setY(Math.random()), Behavior.FALSE);
				    						}
				    					} else {
				    						for (Block block : LocationUtil.getBlocks2D(participant.getPlayer().getLocation(), 2, true, true, true)) {
				    							if (block.getType() == Material.AIR) block = block.getRelative(BlockFace.DOWN);
				    							if (block.getType() == Material.AIR) continue;
				    							Location location = block.getLocation().add(0, 1, 0);
				    							FallingBlocks.spawnFallingBlock(location, block.getType(), block.getData(), false, getPlayer().getLocation().toVector().subtract(location.toVector()).multiply(-0.05).setY(Math.random()), Behavior.FALSE);
				    						}
				    					}
				    					participant.getPlayer().getWorld().strikeLightningEffect(participant.getPlayer().getLocation());
				    					clock.remove();
				    					armorstand.remove();
				    					ParticleLib.PORTAL.spawnParticle(participant.getPlayer().getLocation().clone().add(0, 1, 0), 0, 0, 0, 50, 5);
				    					new OverDimension(participant.getPlayer(), participant.getPlayer().getGameMode()).start();
				    				}
		    					}.runTaskLater(AbilityWar.getPlugin(), 20L);
								damaged.add(participant.getPlayer());
							}
						}
					}
					logMap.get(participant.getPlayer()).stop(false);
				}
			}
			new OverDimension(getPlayer(), getPlayer().getGameMode()).start();
			damaged.clear();
			cool.start();
		}
		
	}.setPeriod(TimeUnit.SECONDS, 1).register();
	
	private class CutParticle extends AbilityTimer {

		private final Vector vector;
		private final Vectors crescentVectors;

		private CutParticle(double angle) {
			super(4);
			setPeriod(TimeUnit.TICKS, 1);
			this.vector = getPlayer().getLocation().getDirection().setY(0).normalize().multiply(0.5);
			this.crescentVectors = crescent.clone()
					.rotateAroundAxisY(-getPlayer().getLocation().getYaw())
					.rotateAroundAxis(getPlayer().getLocation().getDirection().setY(0).normalize(), (180 - angle) % 180);
		}

		@Override
		protected void run(int count) {
			Location baseLoc = getPlayer().getLocation().clone().add(vector).add(0, 1, 0);
			for (Location loc : crescentVectors.toLocations(baseLoc)) {
				ParticleLib.ITEM_CRACK.spawnParticle(loc, 0, 0, 0, 1, 0, MaterialX.OBSIDIAN);
			}
		}

	}
	
	private class PassiveCutParticle extends AbilityTimer {

		private final Vector axis2;
		private final Vector vector2;
		private final Vectors crescentVectors2;

		private PassiveCutParticle(double angle) {
			super(4);
			setPeriod(TimeUnit.TICKS, 1);
			this.axis2 = VectorUtil.rotateAroundAxis(VectorUtil.rotateAroundAxisY(getPlayer().getLocation().getDirection().setY(0).normalize(), 90), getPlayer().getLocation().getDirection().setY(0).normalize(), angle);
			this.vector2 = getPlayer().getLocation().getDirection().setY(0).normalize().multiply(0.5);
			this.crescentVectors2 = crescent2.clone()
					.rotateAroundAxisY(-getPlayer().getLocation().getYaw())
					.rotateAroundAxis(getPlayer().getLocation().getDirection().setY(0).normalize(), (180 - angle) % 180)
					.rotateAroundAxis(axis2, -75);
		}

		@Override
		protected void run(int count) {
			Location baseLoc = getPlayer().getLocation().clone().add(vector2).add(0, 1.3, 0);
			for (Location loc : crescentVectors2.toLocations(baseLoc)) {
				Random random = new Random();
				switch(random.nextInt(4)) {
				case 0:
					ParticleLib.ITEM_CRACK.spawnParticle(loc, 0, 0, 0, 1, 0, MaterialX.WHITE_STAINED_GLASS_PANE);
					break;
				case 1:
					ParticleLib.ITEM_CRACK.spawnParticle(loc, 0, 0, 0, 1, 0, MaterialX.GRAY_STAINED_GLASS_PANE);
					break;
				case 2:
					ParticleLib.ITEM_CRACK.spawnParticle(loc, 0, 0, 0, 1, 0, MaterialX.LIGHT_GRAY_STAINED_GLASS_PANE);
					break;
				case 3:
					ParticleLib.ITEM_CRACK.spawnParticle(loc, 0, 0, 0, 1, 0, MaterialX.BLACK_STAINED_GLASS_PANE);
					break;
				}
			}
			crescentVectors2.rotateAroundAxis(axis2, 40);
		}

	}
	
	private class LogParticle extends AbilityTimer {
		
		private List<Location> locations = new ArrayList<>();
		private final Player player;
		private Random random = new Random();
		private final RGB color = RGB.of(random.nextInt(254) + 1, random.nextInt(254) + 1, random.nextInt(254) + 1);
		
		private LogParticle(Player player) {
			super(200);
			setPeriod(TimeUnit.TICKS, 1);
			this.player = player;
			logMap.put(player, this);
		}
		
		public List<Location> getLog() {
			return locations;
		}
		
    	@Override
    	protected void run(int count) {
    		locations.add(player.getLocation());
    		if (count % 4 == 0) {
        		int momentCount = 0;
        		final ListIterator<Location> listIterator = locations.listIterator(locations.size() - 1);
        		if (!listIterator.hasPrevious()) return;
    			listIterator.previous();
    			while (listIterator.hasPrevious()) {
    				final Location base;
    				if (momentCount == 1) {
    					base = getPlayer().getLocation();
    					listIterator.previous();
    				} else {
    					base = listIterator.previous();
    				}
    				listIterator.next();
    				final Location previous = listIterator.next();
    				if (base.getWorld() != previous.getWorld() || base.distanceSquared(previous) > 36) return;
    				for (Iterator<Location> iterator = new Iterator<Location>() {
    					private final Vector vectorBetween = previous.toVector().subtract(base.toVector()), unit = vectorBetween.clone().normalize().multiply(.1);
    					private final int amount = (int) (vectorBetween.length() / 0.25);
    					private int cursor = 0;

    					@Override
    					public boolean hasNext() {
    						return cursor < amount;
    					}

    					@Override
    					public Location next() {
    						if (cursor >= amount) throw new NoSuchElementException();
    						cursor++;
    						return base.clone().add(unit.clone().multiply(cursor));
    					}
    				}; iterator.hasNext(); ) {
    					ParticleLib.REDSTONE.spawnParticle(getPlayer(), iterator.next().add(0, 1, 0), color);
    				}
    				listIterator.previous();
    				listIterator.previous();
    			}	
    		}
    	}
    	
    	@Override
    	protected void onEnd() {
    		onSilentEnd();
    	}
    	
    	@Override
    	protected void onSilentEnd() {
    		locations.clear();
    	}

	}

	private class OverDimension extends AbilityTimer implements Listener {
		
		private final Player player;
		private ActionbarChannel actionbarChannel;
		private final GameMode originalMode;
		
		private final Predicate<Entity> demonlordexceptpredicate = new Predicate<Entity>() {
			@Override
			public boolean test(Entity entity) {
				if (entity instanceof Player) {
					Participant participant = getGame().getParticipant((Player) entity);
					if (participant.hasAbility()) {
						AbilityBase ab = participant.getAbility();
						if (ab.getClass().equals(Mix.class)) {
							Mix mix = (Mix) ab;
							if (mix.hasSynergy()) {
								if (mix.getSynergy().getClass().equals(DemonLord.class)) {
									return false;
								}
							}
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
		
		private OverDimension(Player player, GameMode originalMode) {
			super(30);
			setPeriod(TimeUnit.TICKS, 2);
			this.player = player;
			if (originalMode.equals(GameMode.SPECTATOR)) {
				this.originalMode = GameMode.SURVIVAL;
			} else {
				this.originalMode = originalMode;
			}
			overMap.put(player, this);
		}
		
    	@Override
    	protected void onStart() {
    		actionbarChannel = getGame().getParticipant(player).actionbar().newChannel();
    		Bukkit.getPluginManager().registerEvents(this, AbilityWar.getPlugin());
    		getGame().getParticipant(player).attributes().TARGETABLE.setValue(false);
    	}
    	
    	@EventHandler
    	public void onPlayerMove(PlayerMoveEvent e) {
    		if (e.getPlayer().equals(player) && demonlordexceptpredicate.test(player)) {
    			e.setCancelled(true);
    		}
    	}
    	
    	@EventHandler
    	public void onPlayerTeleport(PlayerTeleportEvent e) {
    		if (e.getPlayer().equals(player) && e.getCause() == TeleportCause.SPECTATE && demonlordexceptpredicate.test(player)) {
    			e.setCancelled(true);
    		}
    	}
		
    	@Override
    	protected void run(int count) {
    		player.setGameMode(GameMode.SPECTATOR);
    		if (!player.equals(getPlayer())) {
        		ParticleLib.SMOKE_LARGE.spawnParticle(player.getLocation().clone().add(0, 1, 0), 0, 0, 0, 3, 0);	
    		}
    		actionbarChannel.update("§3차원 너머§f: " + count / 10 + "초");
    	}
		
		@Override
		protected void onEnd() {
			onSilentEnd();
		}
		
		@Override
		protected void onSilentEnd() {
			HandlerList.unregisterAll(this);
			overMap.remove(player);
			player.setGameMode(originalMode);
			if (!player.equals(getPlayer())) {
				player.addPotionEffect(confusionsight);
				player.addPotionEffect(lightblindness);	
			}
    		getGame().getParticipant(player).attributes().TARGETABLE.setValue(true);
			actionbarChannel.unregister();
		}
		
	}
	
}