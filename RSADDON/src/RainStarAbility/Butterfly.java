package RainStarAbility;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import RainStarEffect.Dream;
import RainStarEffect.Paralysis;
import RainStarEffect.Poison;
import daybreak.abilitywar.AbilityWar;
import daybreak.abilitywar.ability.AbilityBase;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.ability.decorator.ActiveHandler;
import daybreak.abilitywar.ability.SubscribeEvent;
import daybreak.abilitywar.config.ability.AbilitySettings.SettingObject;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.module.DeathManager;
import daybreak.abilitywar.game.team.interfaces.Teamable;
import daybreak.abilitywar.utils.base.Formatter;
import daybreak.abilitywar.utils.base.collect.Pair;
import daybreak.abilitywar.utils.base.color.RGB;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.base.math.LocationUtil;
import daybreak.abilitywar.utils.base.math.geometry.Circle;
import daybreak.abilitywar.utils.base.math.geometry.Wing;
import daybreak.abilitywar.utils.base.minecraft.item.builder.ItemBuilder;
import daybreak.abilitywar.utils.base.minecraft.version.ServerVersion;
import daybreak.abilitywar.utils.base.random.Random;
import daybreak.abilitywar.utils.library.MaterialX;
import daybreak.abilitywar.utils.library.ParticleLib;
import daybreak.abilitywar.utils.library.SoundLib;
import daybreak.google.common.base.Predicate;

@SuppressWarnings("deprecation")
@AbilityManifest(
		name = "버터플라이", rank = Rank.S, species = Species.ANIMAL, explain = {
		"§7철괴를 우클릭§f하면 §3$[DURATION]초§f간 주위 §3$[RANGE]칸§f 안에 있는 생명체들에게",
		"§a인분 가루§f를 뿌려 상태이상을 부여합니다. $[COOLDOWN]",
		"§a인분 가루§f는 세 종류로, 능력을 쓸 때마다 순차적으로 다음 효과로 넘어가며",
		"§7철괴를 든 채 F키§f를 눌러 §a가루§f의 상세 효과를 보며 조정 가능합니다.",
		"§e낮§f에는 쿨타임이 더 빠르게 끝납니다.",
		"§b[§7아이디어 제공자§b] §3lLeeShin"
		},
		summarize = {
		"§7철괴 들고 F키§f로 적들에게 줄 §3상태이상§f의 종류를 정할 수 있습니다.",
		"§7철괴 우클릭§f으로 저공 비행하며 일정 시간 주위에 §3상태이상§f을 뿌립니다.",
		"교체하지 않아도 §3상태이상§f은 자동으로 다음 효과로 넘어갑니다.",
		"§e낮§f에는 쿨타임이 더 빠르게 끝납니다. $[COOLDOWN]"
		})

public class Butterfly extends AbilityBase implements ActiveHandler {
	
	public Butterfly(Participant participant) {
		super(participant);
	}
	
	public static final SettingObject<Integer> RANGE = abilitySettings.new SettingObject<Integer>(Butterfly.class, "range", 12,
			"# 능력 범위") {

		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}

	};

	public static final SettingObject<Integer> COOLDOWN = abilitySettings.new SettingObject<Integer>(Butterfly.class, "cooldown", 120,
			"# 쿨타임", "# 쿨타임 감소 효과를 66%까지 받습니다.") {

		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}

		@Override
		public String toString() {
			return Formatter.formatCooldown(getValue());
		}

	};

	public static final SettingObject<Integer> DURATION = abilitySettings.new SettingObject<Integer>(Butterfly.class, "duration", 7,
			"# 지속시간 (단위: 초)") {

		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}

	};
	
	public static final SettingObject<Integer> DREAM_DURATION = abilitySettings.new SettingObject<Integer>(Butterfly.class, "dream-duration", 12,
			"# 몽환 지속시간 (단위: 초)") {

		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}

	};
	
	public static final SettingObject<Integer> POISON_DURATION = abilitySettings.new SettingObject<Integer>(Butterfly.class, "poison-duration", 11,
			"# 중독 지속시간 (단위: 초)") {

		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}

	};
	
	public static final SettingObject<Integer> PARALYSIS_DURATION = abilitySettings.new SettingObject<Integer>(Butterfly.class, "paralysis-duration", 10,
			"# 마비 지속시간 (단위: 초)") {

		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}

	};
	
	private static final Pair<Wing, Wing> BUTTERFLY_WING_LAYER_1 = Wing.of(new boolean[][]{
		{false, true, false, false, false, false, false, false, false, false, false, false, false, false, false, false},
		{true, false, true, true, false, false, false, false, false, false, false, false, false, false, false, false},
		{true, false, false, false, true, true, true, false, false, false, false, false, false, false, false, false},
		{true, false, false, false, false, false, false, true, true, false, false, false, false, false, false, false},
		{true, false, false, false, false, false, false, false, false, true, false, false, false, false, false, false},
		{false, true, false, false, false, false, false, false, false, false, true, false, false, false, false, false},
		{false, false, true, false, false, false, false, false, false, false, false, true, false, false, false, false},
		{false, false, true, false, false, false, false, false, false, false, false, false, true, false, false, false},
		{false, false, true, false, false, false, false, false, false, false, false, false, false, true, false, false},
		{false, false, false, true, false, false, false, false, false, false, false, false, false, false, true, false},
		{false, false, false, true, false, false, false, false, false, false, false, false, false, false, true, false},
		{false, false, false, false, true, false, false, false, false, false, false, false, false, false, false, true},
		{false, false, false, false, false, true, true, false, false, false, false, false, false, false, false, true},
		{false, false, false, false, false, true, false, false, false, false, false, false, false, false, false, true},
		{false, false, false, false, true, false, false, false, false, false, false, false, false, false, false, true},
		{false, false, false, false, true, false, false, false, false, false, false, false, false, false, false, true},
		{false, false, false, false, true, false, false, false, false, false, false, false, false, false, false, true},
		{false, false, false, false, true, false, false, false, false, false, false, false, false, false, true, false},
		{false, false, false, false, false, true, false, false, false, false, false, false, false, true, false, false},
		{false, false, false, false, false, false, true, false, false, false, false, false, false, true, false, false},
		{false, false, false, false, false, false, false, true, false, false, false, false, false, true, false, false},
		{false, false, false, false, false, false, false, false, true, false, false, false, true, false, false, false},
		{false, false, false, false, false, false, false, false, false, true, true, true, false, false, false, false}
		});
	
	private static final Pair<Wing, Wing> BUTTERFLY_WING_LAYER_2 = Wing.of(new boolean[][]{
		{false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false},
		{false, true, false, false, false, false, false, false, false, false, false, false, false, false, false, false},
		{false, true, true, true, false, false, false, false, false, false, false, false, false, false, false, false},
		{false, true, true, true, true, true, true, false, false, false, false, false, false, false, false, false},
		{false, true, true, false, false, true, true, true, true, false, false, false, false, false, false, false},
		{false, false, true, true, false, false, false, true, true, true, false, false, false, false, false, false},
		{false, false, false, true, true, false, false, false, false, true, true, false, false, false, false, false},
		{false, false, false, true, true, false, false, false, false, false, true, true, false, false, false, false},
		{false, false, false, true, true, true, false, false, false, false, false, true, true, false, false, false},
		{false, false, false, false, true, true, false, false, false, false, false, true, true, true, false, false},
		{false, false, false, false, true, true, true, true, false, false, false, false, true, true, false, false},
		{false, false, false, false, false, true, true, true, true, false, false, false, false, true, true, false},
		{false, false, false, false, false, false, false, true, true, true, false, false, false, true, true, false},
		{false, false, false, false, false, false, true, true, true, false, false, false, false, true, true, false},
		{false, false, false, false, false, true, true, true, false, false, false, false, false, true, true, false},
		{false, false, false, false, false, true, true, false, false, false, false, false, false, true, true, false},
		{false, false, false, false, false, true, true, false, false, false, false, false, true, true, true, false},
		{false, false, false, false, false, true, true, true, false, false, false, false, true, true, false, false},
		{false, false, false, false, false, false, true, true, true, false, false, true, true, false, false, false},
		{false, false, false, false, false, false, false, true, true, true, false, true, true, false, false, false},
		{false, false, false, false, false, false, false, false, true, true, true, true, true, false, false, false},
		{false, false, false, false, false, false, false, false, false, true, true, true, false, false, false, false},
		{false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false}
		});
	
	private static final Pair<Wing, Wing> BUTTERFLY_WING_LAYER_3 = Wing.of(new boolean[][]{
		{false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false},
		{false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false},
		{false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false},
		{false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false},
		{false, false, false, true, true, false, false, false, false, false, false, false, false, false, false, false},
		{false, false, false, false, true, true, true, false, false, false, false, false, false, false, false, false},
		{false, false, false, false, false, true, true, true, true, false, false, false, false, false, false, false},
		{false, false, false, false, false, true, true, true, true, true, false, false, false, false, false, false},
		{false, false, false, false, false, false, true, true, true, true, true, false, false, false, false, false},
		{false, false, false, false, false, false, true, true, true, true, true, false, false, false, false, false},
		{false, false, false, false, false, false, false, false, true, true, true, true, false, false, false, false},
		{false, false, false, false, false, false, false, false, false, true, true, true, true, false, false, false},
		{false, false, false, false, false, false, false, false, false, false, true, true, true, false, false, false},
		{false, false, false, false, false, false, false, false, false, true, true, true, true, false, false, false},
		{false, false, false, false, false, false, false, false, true, true, true, true, true, false, false, false},
		{false, false, false, false, false, false, false, true, true, true, true, true, true, false, false, false},
		{false, false, false, false, false, false, false, true, true, true, true, true, false, false, false, false},
		{false, false, false, false, false, false, false, false, true, true, true, true, false, false, false, false},
		{false, false, false, false, false, false, false, false, false, true, true, false, false, false, false, false},
		{false, false, false, false, false, false, false, false, false, false, true, false, false, false, false, false},
		{false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false},
		{false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false},
		{false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false}
		});

	private final Predicate<Entity> STRICT_PREDICATE = new Predicate<Entity>() {
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
	
	private final int range = RANGE.getValue();
	private final int dreamduration = DREAM_DURATION.getValue();
	private final int poisonduration = POISON_DURATION.getValue();
	private final int paralysisduration = PARALYSIS_DURATION.getValue();
	private final Cooldown cooldown = new Cooldown(COOLDOWN.getValue(), 66);
	
	private int type = 0;
	
	private final Circle circle = Circle.of(range, range * 15);
	private static final RGB DREAM_DARK = RGB.of(148, 1, 174), DREAM = RGB.of(220, 19, 254), DREAM_LIGHT = RGB.of(240, 153, 254),
			POISON_DARK = RGB.of(1, 87, 1), POISON = RGB.of(1, 166, 1), POISON_LIGHT = RGB.of(147, 254, 147),
			PARALYSIS_DARK = RGB.of(35, 71, 68), PARALYSIS = RGB.of(64, 139, 138), PARALYSIS_LIGHT = RGB.of(97, 172, 188);
	
	@Override
	protected void onUpdate(Update update) {
		if (update == Update.RESTRICTION_CLEAR) {
			passive.start();
		}
	}

	public boolean ActiveSkill(Material material, ClickType clickType) {
		if (material == Material.IRON_INGOT) {
			if (clickType == ClickType.RIGHT_CLICK) {
				if (!skill.isDuration() && !cooldown.isCooldown()) {
					skill.start();
					return true;
				}
			}
		}
		return false;
	}
	
	private final Duration skill = new Duration(DURATION.getValue() * 20, cooldown) {
		private List<Player> targets;
		private List<Player> instrumentListeners;
		private Wing leftWing1, rightWing1, leftWing2, rightWing2, leftWing3, rightWing3;
		
		private int wingstack;
		private boolean wingmax;
		private Random random = new Random();
		private int randomInt;
		
		private RGB COLOR_DARK, COLOR, COLOR_LIGHT;

		public void target() {
			this.leftWing1 = BUTTERFLY_WING_LAYER_1.getLeft().clone();
			this.rightWing1 = BUTTERFLY_WING_LAYER_1.getRight().clone();
			this.leftWing2 = BUTTERFLY_WING_LAYER_2.getLeft().clone();
			this.rightWing2 = BUTTERFLY_WING_LAYER_2.getRight().clone();
			this.leftWing3 = BUTTERFLY_WING_LAYER_3.getLeft().clone();
			this.rightWing3 = BUTTERFLY_WING_LAYER_3.getRight().clone();
			this.instrumentListeners = new ArrayList<>();
			this.targets = LocationUtil.getNearbyEntities(Player.class, getPlayer().getLocation(), range, 250, STRICT_PREDICATE);
			for (Player target : targets) {
				instrumentListeners.add(target);		
			}
		}
		
		public void wing() {
			float yaw = getPlayer().getLocation().getYaw();
			if (getPlayer().getName().equals("lLeeShin")) {
				wingstack = wingmax ? Math.min(60, wingstack + 10) : Math.max(0, wingstack - 10);
				if ((wingstack == 60 && wingmax) || (wingstack == 0 && !wingmax)) wingmax = !wingmax;
			} else if (randomInt <= 1) {
				wingstack = wingmax ? Math.min(60, wingstack + 10) : Math.max(0, wingstack - 10);
				if ((wingstack == 60 && wingmax) || (wingstack == 0 && !wingmax)) wingmax = !wingmax;
			} else {
				wingstack = 15;
			}
			for (Location loc : leftWing1.rotateAroundAxisY(-yaw + wingstack).toLocations(getPlayer().getLocation().clone().subtract(0, 0.5, 0))) {
				ParticleLib.REDSTONE.spawnParticle(loc, COLOR_DARK);
			}
			leftWing1.rotateAroundAxisY(yaw - wingstack);
			for (Location loc : rightWing1.rotateAroundAxisY(-yaw - wingstack).toLocations(getPlayer().getLocation().clone().subtract(0, 0.5, 0))) {
				ParticleLib.REDSTONE.spawnParticle(loc, COLOR_DARK);
			}
			rightWing1.rotateAroundAxisY(yaw + wingstack);
			for (Location loc : leftWing2.rotateAroundAxisY(-yaw + wingstack).toLocations(getPlayer().getLocation().clone().subtract(0, 0.5, 0))) {
				ParticleLib.REDSTONE.spawnParticle(loc, COLOR_LIGHT);
			}
			leftWing2.rotateAroundAxisY(yaw - wingstack);
			for (Location loc : rightWing2.rotateAroundAxisY(-yaw - wingstack).toLocations(getPlayer().getLocation().clone().subtract(0, 0.5, 0))) {
				ParticleLib.REDSTONE.spawnParticle(loc, COLOR_LIGHT);
			}
			rightWing2.rotateAroundAxisY(yaw + wingstack);
			for (Location loc : leftWing3.rotateAroundAxisY(-yaw + wingstack).toLocations(getPlayer().getLocation().clone().subtract(0, 0.5, 0))) {
				ParticleLib.REDSTONE.spawnParticle(loc, COLOR);
			}
			leftWing3.rotateAroundAxisY(yaw - wingstack);
			for (Location loc : rightWing3.rotateAroundAxisY(-yaw - wingstack).toLocations(getPlayer().getLocation().clone().subtract(0, 0.5, 0))) {
				ParticleLib.REDSTONE.spawnParticle(loc, COLOR);
			}
			rightWing3.rotateAroundAxisY(yaw + wingstack);
			
			for (Location location : circle.toLocations(getPlayer().getLocation()).floor(getPlayer().getLocation().getY())) {
				ParticleLib.REDSTONE.spawnParticle(location, COLOR);
			}
		}

		@Override
		protected void onDurationStart() {
			type++;
			switch(type) {
			case 1:
				COLOR = DREAM; 
				COLOR_DARK = DREAM_DARK;
				COLOR_LIGHT = DREAM_LIGHT;
				break;
			case 2:
				COLOR = POISON; 
				COLOR_DARK = POISON_DARK;
				COLOR_LIGHT = POISON_LIGHT;
				break;
			case 3:
				COLOR = PARALYSIS;
				COLOR_DARK = PARALYSIS_DARK;
				COLOR_LIGHT = PARALYSIS_LIGHT;
				break;
			}
			randomInt = random.nextInt(10);
		}

		@Override
		protected void onDurationProcess(int count) {
			target();
			getPlayer().setFlySpeed(0.2f);
			getPlayer().setAllowFlight(true);
			getPlayer().setFlying(true);
			if (count % 10 == 0) {
				instrumentListeners.add(getPlayer());
				SoundLib.ENTITY_ENDER_DRAGON_FLAP.playSound(instrumentListeners, 1f, 1.8f);
			}
			
			if (count % 5 == 0) {
				wing();
			}
			
			if (count % 20 == 0) {
				for (Player target : targets) {
					if (type == 1) Dream.apply(getGame().getParticipant(target), TimeUnit.TICKS, dreamduration * 20);
					if (type == 2) Poison.apply(getGame().getParticipant(target), TimeUnit.TICKS, poisonduration * 20);
					if (type == 3) Paralysis.apply(getGame().getParticipant(target), TimeUnit.TICKS, paralysisduration * 20);
				}
				Location loc = LocationUtil.floorY(getPlayer().getLocation());
				
				if (type == 1) {
					if (ServerVersion.getVersion() >= 13) {
						BlockData powder = Material.MAGENTA_WOOL.createBlockData();
						ParticleLib.FALLING_DUST.spawnParticle(loc, range, 2, range, 250, 0, powder);
					} else {
						ItemStack powder = new ItemStack(Material.getMaterial("WOOL"), 1, (byte) 2);
						ParticleLib.FALLING_DUST.spawnParticle(loc, range, 2, range, 250, 0, powder.getData());
					}
				} else if (type == 2) {
					if (ServerVersion.getVersion() >= 13) {
						BlockData powder = Material.GREEN_WOOL.createBlockData();
						ParticleLib.FALLING_DUST.spawnParticle(loc, range, 2, range, 250, 0, powder);
					} else {
						ItemStack powder = new ItemStack(Material.getMaterial("WOOL"), 1, (byte) 13);
						ParticleLib.FALLING_DUST.spawnParticle(loc, range, 2, range, 250, 0, powder.getData());
					}
				} else if (type == 3) {
					if (ServerVersion.getVersion() >= 13) {
						BlockData powder = Material.CYAN_WOOL.createBlockData();
						ParticleLib.FALLING_DUST.spawnParticle(loc, range, 2, range, 250, 0, powder);
					} else {
						ItemStack powder = new ItemStack(Material.getMaterial("WOOL"), 1, (byte) 9);
						ParticleLib.FALLING_DUST.spawnParticle(loc, range, 2, range, 250, 0, powder.getData());
					}
				}
				
			}
			
			final Location playerLocation = getPlayer().getLocation();
			final double floorY = LocationUtil.getFloorYAt(getPlayer().getWorld(), playerLocation.getY(), playerLocation.getBlockX(), playerLocation.getBlockZ()) + 2;
			getPlayer().setVelocity(getPlayer().getVelocity().setY(floorY > playerLocation.getY() ? 0.05 : (floorY == playerLocation.getY() ? 0 : -0.05)));
		}

		@Override
		public void onDurationEnd() {
			onDurationSilentEnd();
		}

		@Override
		protected void onDurationSilentEnd() {
			if (type == 3) type = 0;
			getPlayer().setFlying(false);
			getPlayer().setFlySpeed(.1f);
			GameMode mode = getPlayer().getGameMode();
			getPlayer().setAllowFlight(mode != GameMode.SURVIVAL && mode != GameMode.ADVENTURE);
		}

	}.setPeriod(TimeUnit.TICKS, 1);

	private boolean isNight(long time) {
		return time > 12300 && time < 23850;
	}

	@SubscribeEvent(onlyRelevant = true)
	private void onMove(PlayerMoveEvent e) {
		if (!skill.isRunning()) return;
		final Location playerLocation = getPlayer().getLocation();
		final double floorY = LocationUtil.getFloorYAt(getPlayer().getWorld(), playerLocation.getY(), playerLocation.getBlockX(), playerLocation.getBlockZ()) + 3;
		if ((e.getFrom().getY() < floorY && e.getTo().getY() > floorY) || (e.getFrom().getY() > floorY && e.getTo().getY() > e.getFrom().getY())) {
			e.getTo().setY(e.getFrom().getY());
		}
	}

	private final AbilityTimer passive = new AbilityTimer() {
		@Override
		protected void run(int count) {
			if (cooldown.isRunning()) {
				long time = getPlayer().getWorld().getTime();
				if (!isNight(time)) {
					cooldown.setCount(Math.max(cooldown.getCount() - 1, 0));
				}
			}
		}
	}.setPeriod(TimeUnit.SECONDS, 1).register();
	
    @SubscribeEvent(onlyRelevant = true)
    public void onPlayerSwapHandItems(PlayerSwapHandItemsEvent e) {
    	if (e.getOffHandItem().getType().equals(Material.IRON_INGOT) && e.getPlayer().equals(getPlayer())) {
    		if (!skill.isRunning()) {
        		new PowderGui().start();	
    		} else {
    			getPlayer().sendMessage("§4[§c!§4] §f능력 지속 중에는 GUI를 열 수 없습니다.");
    		}
    		e.setCancelled(true);
    	}
    }
	
	public class PowderGui extends AbilityTimer implements Listener {
		
		private final ItemStack NULL = (new ItemBuilder(MaterialX.GRAY_STAINED_GLASS_PANE)).displayName(" ").build();
		private final ItemStack MAGENTA = (new ItemBuilder(MaterialX.MAGENTA_CONCRETE_POWDER)).displayName("§d몽환 가루").build();
		private final ItemStack GREEN = (new ItemBuilder(MaterialX.GREEN_CONCRETE_POWDER)).displayName("§2중독 가루").build();
		private final ItemStack BLUE = (new ItemBuilder(MaterialX.CYAN_CONCRETE_POWDER)).displayName("§3마비 가루").build();
		private final ItemStack EXIT = (new ItemBuilder(MaterialX.SPRUCE_DOOR)).displayName("§e나가기").build();
		
		private final Inventory gui;
		
		public PowderGui() {
			super(TaskType.REVERSE, 99999);
			setPeriod(TimeUnit.TICKS, 1);
			gui = Bukkit.createInventory(null, InventoryType.HOPPER, "§0인분 가루 정보");
			
			ItemMeta magentameta = MAGENTA.getItemMeta();
			final List<String> magentalore = new ArrayList<>();
			magentalore.add("§f적을 나른하게 만들어 §d몽환§f에 빠지게 함");
			magentalore.add("§d몽환§f에 빠진 적은 흐르던 모든 시간이 멈추게 되며");
			magentalore.add("§f누군가가 공격해 깨우기 전까진 일어날 수 없음");
			magentalore.add("§f강제로 깨어날 경우 정신이 몽롱해져");
			magentalore.add("§f이 상태에서 하는 모든 공격은 30% 확률로 빗나감");
			magentalore.add("§8==========================");
			magentalore.add("§c지속 시간§7: §e" + dreamduration + "초");
			magentalore.add("§f");
			magentalore.add("§b» §f이 효과를 부여하려면 클릭하세요.");
			magentameta.setLore(magentalore);
			MAGENTA.setItemMeta(magentameta);
			
			ItemMeta greenmeta = GREEN.getItemMeta();
			final List<String> greenlore = new ArrayList<>();
			greenlore.add("§f적을 §2중독§f시켜 지속적 피해를 입힘");
			greenlore.add("§f체력이 반 칸 이하일 때는 피해를 입히지 않음");
			greenlore.add("§f적이 회복 효과를 받을 때 대신 피해를 받음");
			greenlore.add("§8==========================");
			greenlore.add("§c지속 시간§7: §e" + poisonduration + "초");
			greenlore.add("§f");
			greenlore.add("§b» §f이 효과를 부여하려면 클릭하세요.");
			greenmeta.setLore(greenlore);
			GREEN.setItemMeta(greenmeta);
			
			ItemMeta bluemeta = BLUE.getItemMeta();
			final List<String> bluelore = new ArrayList<>();
			bluelore.add("§f적의 눈, 팔, 다리 중 한 부분을 §3마비§f시킴");
			bluelore.add("§3마비§f된 부위에 따라 적의 행동을 제약함");
			bluelore.add("§6눈§7: §f적의 시야를 차단함");
			bluelore.add("§6팔§7: §f적의 근접 공격을 매우 느리고 약하게 만듦");
			bluelore.add("§6다리§7: §f적의 이동 속도가 급격히 느려짐");
			bluelore.add("§8==========================");
			bluelore.add("§c지속 시간§7: §e" + paralysisduration + "초");
			bluelore.add("§f");
			bluelore.add("§b» §f이 효과를 부여하려면 클릭하세요.");
			bluemeta.setLore(bluelore);
			BLUE.setItemMeta(bluemeta);
		}
		
		protected void onStart() {
			Bukkit.getPluginManager().registerEvents(this, AbilityWar.getPlugin());
			getPlayer().openInventory(gui);
		}
		
		@Override
		protected void run(int arg0) {
			gui.setItem(0, MAGENTA);
			gui.setItem(1, GREEN);
			gui.setItem(2, BLUE);
			gui.setItem(3, NULL);
			gui.setItem(4, EXIT);
		}
		
		@Override
		protected void onEnd() {
			onSilentEnd();
		}
		
		@Override
		protected void onSilentEnd() {
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
		private void onInventoryClick(InventoryClickEvent e) {
			if (e.getInventory().equals(gui)) {
				if (e.getSlot() == 3) e.setCancelled(true);
				if (e.getSlot() < 3) {
					type = e.getSlot();
					getPlayer().closeInventory();
					switch(type) {
					case 0:
						getPlayer().sendMessage("§5[§d!§5] §f다음 효과를 §d몽환§f으로 변경했습니다. §d몽환§7 > §2중독§7 > §3마비§7");
						break;
					case 1:
						getPlayer().sendMessage("§2[§a!§2] §f다음 효과를 §2중독§f으로 변경했습니다. §2중독§7 > §3마비§7 > §d몽환§7");
						break;
					case 2:
						getPlayer().sendMessage("§3[§b!§3] §f다음 효과를 §3마비§f로 변경했습니다. §3마비§7 > §d몽환§7 > §2중독§7");
						break;
					}
				} else if (e.getSlot() == 4) {
					getPlayer().closeInventory();
				}
			}
		}
		
	}

}
