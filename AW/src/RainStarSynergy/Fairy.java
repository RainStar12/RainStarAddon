package RainStarSynergy;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Random;
import java.util.UUID;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Note;
import org.bukkit.World;
import org.bukkit.Note.Tone;
import org.bukkit.attribute.Attribute;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.projectiles.ProjectileSource;
import org.bukkit.util.Vector;

import daybreak.abilitywar.AbilityWar;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.ability.SubscribeEvent;
import daybreak.abilitywar.ability.decorator.ActiveHandler;
import daybreak.abilitywar.config.ability.AbilitySettings.SettingObject;
import daybreak.abilitywar.game.AbstractGame.CustomEntity;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.AbstractGame.Participant.ActionbarNotification.ActionbarChannel;
import daybreak.abilitywar.game.list.mix.synergy.Synergy;
import daybreak.abilitywar.game.module.DeathManager;
import daybreak.abilitywar.game.team.interfaces.Teamable;
import daybreak.abilitywar.utils.base.Formatter;
import daybreak.abilitywar.utils.base.color.RGB;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.base.math.LocationUtil;
import daybreak.abilitywar.utils.base.math.geometry.Line;
import daybreak.abilitywar.utils.base.minecraft.block.Blocks;
import daybreak.abilitywar.utils.base.minecraft.block.IBlockSnapshot;
import daybreak.abilitywar.utils.base.minecraft.entity.decorator.Deflectable;
import daybreak.abilitywar.utils.base.minecraft.entity.health.Healths;
import daybreak.abilitywar.utils.base.minecraft.nms.NMS;
import daybreak.abilitywar.utils.base.random.RouletteWheel;
import daybreak.abilitywar.utils.library.BlockX;
import daybreak.abilitywar.utils.library.MaterialX;
import daybreak.abilitywar.utils.library.ParticleLib;
import daybreak.abilitywar.utils.library.SoundLib;
import daybreak.google.common.base.Predicate;

@AbilityManifest(
		name = "페어리", rank = Rank.L, species = Species.OTHERS, explain = {
		"§7철괴 우클릭 §8- §b결정화§f: 주변 $[RANGE]칸 내의 모든 플레이어를 $[DURATION]초간 §b결정화§f시킵니다.",
		" 결정화된 플레이어의 §d회복 효과§f는 대신 자신이 2배의 §e흡수 체력§f으로 변환해",
		" 최대 자신의 최대 체력만큼까지 소지할 수 있으며 이미 최대치일 경우",
		" 일반 체력이 §d회복§f됩니다. $[COOLDOWN]",
		"§7활 발사 §8- §a결정 화살§f: §3더 빠르고 무조건 치명타가 발동하는 화살§f을 발사합니다.",
		" 결정 화살은 적중한 적을 §e$[CHANCE]%§f 확률로 $[SHOT_DURATION]초간 §b결정화§f시킵니다.",
		" 위 효과가 발동하지 않으면, 대신 적을 더 크게 밀쳐냅니다.",
		})

@SuppressWarnings("unused")
public class Fairy extends Synergy implements ActiveHandler {

	public Fairy(Participant participant) {
		super(participant);
	}
	
	public static final SettingObject<Integer> COOLDOWN = synergySettings.new SettingObject<Integer>(Fairy.class, "cooldown", 100, "# 쿨타임") {

		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}

		@Override
		public String toString() {
			return Formatter.formatCooldown(getValue());
		}

	};
	
	public static final SettingObject<Integer> RANGE = synergySettings.new SettingObject<Integer>(Fairy.class, "range", 10, "# 사거리") {

		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}

	};
	
	public static final SettingObject<Integer> DURATION = synergySettings.new SettingObject<Integer>(Fairy.class, "duration", 7, "# 지속 시간") {

		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}

	};
	
	public static final SettingObject<Integer> SHOT_DURATION = synergySettings.new SettingObject<Integer>(Fairy.class, "shot-duration", 3, "# 활 결정화 지속 시간") {

		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}

	};
	
	public static final SettingObject<Integer> CHANCE = synergySettings.new SettingObject<Integer>(Fairy.class, "chance", 20, "# 결정 화살 확률") {

		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}

	};
	
	public static final SettingObject<Integer> CHANCE_INCREASE = synergySettings.new SettingObject<Integer>(Fairy.class, "chance-increase", 10, "# 실패시 확률 증가") {

		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}

	};
	
	private final Cooldown cooldown = new Cooldown(COOLDOWN.getValue());
	private final int range = RANGE.getValue();
	private final double particleRange = (range / 3.0) * 2;
	private final int chance = CHANCE.getValue();
	private final int chanceIncrease = CHANCE_INCREASE.getValue();
	private CrystalField crystalField = null;
	private final int duration = DURATION.getValue();
	private final int shotduration = SHOT_DURATION.getValue();
	private final Map<Arrow, Boolean> myarrow = new HashMap<>();
	private final Map<Arrow, Bullet> bullets = new HashMap<>();
	private final Map<UUID, Crystalize> crystalized = new HashMap<>();
	private final RouletteWheel rouletteWheel = new RouletteWheel();
	private final RouletteWheel.Slice positive = rouletteWheel.newSlice(chance),
			negative = rouletteWheel.newSlice(100 - positive.getWeight());
	private int stack;
	
	private static final Relative[] relatives1 = {
			new RelativeRange(-1, 0, -1, 1, 1, 1, MaterialX.LIGHT_BLUE_STAINED_GLASS, (x, y, z) -> !((x == -1 || x == 1) && (z == -1 || z == 1)) && !(x == 0 && z == 0 && (y == 0 || y == 1))),
			new RelativeFixed(0, -1, 0, MaterialX.LIGHT_BLUE_STAINED_GLASS),
			new RelativeFixed(0, 2, 0, MaterialX.LIGHT_BLUE_STAINED_GLASS)
	};
	
	private static final Relative[] relatives2 = {
			new RelativeRange(-1, 0, -1, 1, 1, 1, MaterialX.LIME_STAINED_GLASS, (x, y, z) -> !((x == -1 || x == 1) && (z == -1 || z == 1)) && !(x == 0 && z == 0 && (y == 0 || y == 1))),
			new RelativeFixed(0, -1, 0, MaterialX.LIME_STAINED_GLASS),
			new RelativeFixed(0, 2, 0, MaterialX.LIME_STAINED_GLASS)
	};
	
	private static final Relative[] relatives3 = {
			new RelativeRange(-1, 0, -1, 1, 1, 1, MaterialX.ORANGE_STAINED_GLASS, (x, y, z) -> !((x == -1 || x == 1) && (z == -1 || z == 1)) && !(x == 0 && z == 0 && (y == 0 || y == 1))),
			new RelativeFixed(0, -1, 0, MaterialX.ORANGE_STAINED_GLASS),
			new RelativeFixed(0, 2, 0, MaterialX.ORANGE_STAINED_GLASS)
	};
	
	private static final Relative[] relatives4 = {
			new RelativeRange(-1, 0, -1, 1, 1, 1, MaterialX.PINK_STAINED_GLASS, (x, y, z) -> !((x == -1 || x == 1) && (z == -1 || z == 1)) && !(x == 0 && z == 0 && (y == 0 || y == 1))),
			new RelativeFixed(0, -1, 0, MaterialX.PINK_STAINED_GLASS),
			new RelativeFixed(0, 2, 0, MaterialX.PINK_STAINED_GLASS)
	};
	
	private static final Relative[] relatives5 = {
			new RelativeRange(-1, 0, -1, 1, 1, 1, MaterialX.YELLOW_STAINED_GLASS, (x, y, z) -> !((x == -1 || x == 1) && (z == -1 || z == 1)) && !(x == 0 && z == 0 && (y == 0 || y == 1))),
			new RelativeFixed(0, -1, 0, MaterialX.YELLOW_STAINED_GLASS),
			new RelativeFixed(0, 2, 0, MaterialX.YELLOW_STAINED_GLASS)
	};
	
	private static final Relative[] relatives6 = {
			new RelativeRange(-1, 0, -1, 1, 1, 1, MaterialX.WHITE_STAINED_GLASS, (x, y, z) -> !((x == -1 || x == 1) && (z == -1 || z == 1)) && !(x == 0 && z == 0 && (y == 0 || y == 1))),
			new RelativeFixed(0, -1, 0, MaterialX.WHITE_STAINED_GLASS),
			new RelativeFixed(0, 2, 0, MaterialX.WHITE_STAINED_GLASS)
	};
	
	@FunctionalInterface
	private interface CoordConsumer {

		void accept(final int x, final int y, final int z, final MaterialX type);

	}
	
	private interface Relative {
		void forEach(final CoordConsumer consumer);
	}
	
	private static class RelativeFixed implements Relative {

		private final int x, y, z;
		private final MaterialX material;

		private RelativeFixed(final int x, final int y, final int z, final MaterialX material) {
			this.x = x;
			this.y = y;
			this.z = z;
			this.material = material;
		}

		@Override
		public void forEach(CoordConsumer consumer) {
			consumer.accept(x, y, z, material);
		}
	}

	private static class RelativeRange implements Relative {

		private final int minX, minY, minZ, maxX, maxY, maxZ;
		private final MaterialX material;
		private final CoordPredicate predicate;

		private RelativeRange(final int minX, final int minY, final int minZ, final int maxX, final int maxY, final int maxZ, final MaterialX material, final CoordPredicate predicate) {
			this.minX = minX;
			this.minY = minY;
			this.minZ = minZ;
			this.maxX = maxX;
			this.maxY = maxY;
			this.maxZ = maxZ;
			this.material = material;
			this.predicate = predicate;
		}

		@Override
		public void forEach(final CoordConsumer consumer) {
			for (int x = minX; x <= maxX; x++) {
				for (int y = minY; y <= maxY; y++) {
					for (int z = minZ; z <= maxZ; z++) {
						if (predicate != null && !predicate.test(x, y, z)) continue;
						consumer.accept(x, y, z, material);
					}
				}
			}
		}
	}
	
	private static class SnapshotEvent extends Event {

		private static final HandlerList handlers = new HandlerList();
		private final Block block;
		private IBlockSnapshot snapshot = null;

		private SnapshotEvent(final Block block) {
			this.block = block;
		}

		public static HandlerList getHandlerList() {
			return handlers;
		}

		@Override
		public @Nonnull HandlerList getHandlers() {
			return handlers;
		}

	}
	
	@FunctionalInterface
	public interface CoordPredicate {
		boolean test(int x, int y, int z);
	}

	private static class CrystalizeEvent extends Event implements Cancellable {

		private static final HandlerList handlers = new HandlerList();
		private final Player player;
		private boolean cancelled = false;

		private CrystalizeEvent(final Player player) {
			this.player = player;
		}

		public static HandlerList getHandlerList() {
			return handlers;
		}

		@Override
		public @Nonnull HandlerList getHandlers() {
			return handlers;
		}

		@Override
		public boolean isCancelled() {
			return cancelled;
		}

		@Override
		public void setCancelled(boolean cancelled) {
			this.cancelled = cancelled;
		}
	}
	
	@Override
	public boolean ActiveSkill(Material material, ClickType clickType) {
		if (material == Material.IRON_INGOT) {
			if (clickType == ClickType.RIGHT_CLICK) {
				if (crystalField == null && !cooldown.isCooldown()) {
					new CrystalField().start();
				}
			}
		}
		return false;
	}
	
	@SubscribeEvent
	private void onSnapshot(final SnapshotEvent e) {
		if (e.snapshot != null) return;
		if (crystalField != null) {
			final IBlockSnapshot snapshot = crystalField.snapshots.get(e.block);
			if (snapshot != null) {
				e.snapshot = snapshot;
				return;
			}
		}
	}

	@SubscribeEvent
	private void onBlockBreak(final BlockBreakEvent e) {
		if (crystalField != null && crystalField.snapshots.containsKey(e.getBlock())) {
			e.setCancelled(true);
			return;
		}
	}
	
	@SubscribeEvent
	public void onProjectileHit(ProjectileHitEvent e) {
		if (NMS.isArrow(e.getEntity())) {
			Arrow arrow = (Arrow) e.getEntity();
			if (myarrow.containsKey(arrow)) {
	    		ParticleLib.ITEM_CRACK.spawnParticle(arrow.getLocation(), 0, 0, 0, 35, 0.2, MaterialX.PRISMARINE_CRYSTALS);
	    		ParticleLib.ITEM_CRACK.spawnParticle(arrow.getLocation(), 0, 0, 0, 15, 0.2, MaterialX.DIAMOND);
	    		if (myarrow.get(arrow) == true) {
	    			bullets.get(arrow).stop(false);
		    		if (e.getHitEntity() instanceof Player) {
		    			Random random = new Random();
		    			new Crystalize((Player) e.getHitEntity(), random.nextInt(6) + 1, shotduration).start();
		    		}
	    		}
				myarrow.remove(arrow);
			}	
		}
	}
    
    @SubscribeEvent
	public void onProjectileLaunch(ProjectileLaunchEvent e) {
    	if (getPlayer().equals(e.getEntity().getShooter()) && NMS.isArrow(e.getEntity())) {
    		Arrow arrow = (Arrow) e.getEntity();
    		arrow.setCritical(true);
			arrow.setVelocity(arrow.getVelocity().multiply(1.5));
			final RouletteWheel.Slice select = rouletteWheel.select();
			if (select == positive) {
        		stack++;
    			double temp = (double) stack;
    			int soundnumber = (int) (temp - (Math.ceil(temp / SOUND_RUNNABLES.size()) - 1) * SOUND_RUNNABLES.size()) - 1;
	    		SOUND_RUNNABLES.get(soundnumber).run();
	       		myarrow.put(arrow, true);
	       		new Bullet(getPlayer(), getPlayer().getLocation().clone().add(0, 1, 0), arrow, arrow.getVelocity().length()).start();
	    		positive.resetWeight();
    		} else {
    			arrow.setKnockbackStrength(arrow.getKnockbackStrength() + 2);
            	SoundLib.ENTITY_ENDER_EYE_DEATH.playSound(arrow.getLocation(), 1, 1.75f);
				positive.increaseWeight(chanceIncrease);
		   		myarrow.put(arrow, false);
				negative.resetWeight();
    		}
    	}
    }

	public class CrystalField extends AbilityTimer implements Listener {

		private final Map<Block, IBlockSnapshot> snapshots = new HashMap<>();
		private final Location center = getPlayer().getLocation().clone();
		private int stack = 0;

		private CrystalField() {
			super(TaskType.REVERSE, range + 40);
			setPeriod(TimeUnit.TICKS, 2);
			setBehavior(RestrictionBehavior.PAUSE_RESUME);
		}

		private final Predicate<Entity> predicate = new Predicate<Entity>() {
			@Override
			public boolean test(Entity entity) {
				if (crystalized.containsKey(entity.getUniqueId())) return false;
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
		
		private void addSnapshot(final Block block) {
			if (!snapshots.containsKey(block)) {
				final SnapshotEvent event = new SnapshotEvent(block);
				Bukkit.getPluginManager().callEvent(event);
				final IBlockSnapshot snapshot = event.snapshot;
				if (snapshot == null) {
					snapshots.put(block, Blocks.createSnapshot(block));
				} else {
					snapshots.put(block, snapshot);
				}
			}
		}

		private void restoreAll() {
			for (final Iterator<IBlockSnapshot> iterator = snapshots.values().iterator(); iterator.hasNext(); ) {
				iterator.next().apply();
				iterator.remove();
			}
		}

		@EventHandler
		private void onCrystalize(final CrystalizeEvent e) {
			if (crystalized.containsKey(e.player.getUniqueId())) {
				e.setCancelled(true);
			}
		}

		@Override
		protected void onStart() {
			SoundLib.ENTITY_PLAYER_LEVELUP.playSound(center, 2f, 1.75f);
			ParticleLib.VILLAGER_HAPPY.spawnParticle(center, particleRange, particleRange, particleRange, 70, 0);
			ParticleLib.END_ROD.spawnParticle(center, particleRange, particleRange, particleRange, 70, 0);
			Fairy.this.crystalField = this;
			Bukkit.getPluginManager().registerEvents(this, AbilityWar.getPlugin());
		}

		@Override
		protected void run(int count) {
			if (count >= (getMaximumCount() - range)) {
				final int radius = getMaximumCount() - count + 1;
				for (Block block : LocationUtil.getBlocks2D(center, radius, true, true, true)) {
					Block below = block.getRelative(BlockFace.DOWN);
					if (snapshots.containsKey(below)) {
						below = below.getRelative(BlockFace.DOWN);
					}
					addSnapshot(below);
					BlockX.setType(below, MaterialX.OAK_LEAVES);
				}
				for (Player player : LocationUtil.getEntitiesInCircle(Player.class, center, radius, predicate)) {
					final CrystalizeEvent event = new CrystalizeEvent(player);
					Bukkit.getPluginManager().callEvent(event);
					if (!event.isCancelled()) {
						Random random = new Random();
						new Crystalize(player, (random.nextInt(6) + 1), duration);
					}
				}
			} else {
				stack++;
			}
			if (stack >= 10) {
				stop(false);
			}
		}

		@Override
		protected void onEnd() {
			cooldown.start();
			onSilentEnd();
		}

		@Override
		protected void onSilentEnd() {
			HandlerList.unregisterAll(this);
			restoreAll();
			Fairy.this.crystalField = null;
		}

	}
	
	private class Crystalize extends AbilityTimer implements Listener {
		
		private final Map<Block, IBlockSnapshot> snapshots = new HashMap<>();

		private final Player player;
		private final ActionbarChannel actionbarChannel;
		private Block lastBlock;
		private boolean placed = false;
		private int color;
		private MaterialX materialX;
		private int maxCount;
		
		private Crystalize(final Player player, int color, int duration) {
			super(TaskType.NORMAL, duration * 10);
			setPeriod(TimeUnit.TICKS, 2);
			this.player = player;
			this.color = color;
			crystalized.put(player.getUniqueId(), this);
			switch(color) {
			case 1:
				this.materialX = MaterialX.LIGHT_BLUE_STAINED_GLASS;
				break;
			case 2:
				this.materialX = MaterialX.LIME_STAINED_GLASS;
				break;
			case 3:
				this.materialX = MaterialX.ORANGE_STAINED_GLASS;
				break;
			case 4:
				this.materialX = MaterialX.PINK_STAINED_GLASS;
				break;
			case 5:
				this.materialX = MaterialX.YELLOW_STAINED_GLASS;
				break;
			case 6:
				this.materialX = MaterialX.WHITE_STAINED_GLASS;
				break;
			}
			final Participant participant = getGame().getParticipant(player);
			this.actionbarChannel = participant.actionbar().newChannel();
			maxCount = duration * 10;
			start();
		}

		private void addSnapshot(final Block block) {
			if (!snapshots.containsKey(block)) {
				final SnapshotEvent event = new SnapshotEvent(block);
				Bukkit.getPluginManager().callEvent(event);
				final IBlockSnapshot snapshot = event.snapshot;
				if (snapshot == null) {
					snapshots.put(block, Blocks.createSnapshot(block));
				} else {
					snapshots.put(block, snapshot);
				}
			}
		}
		
		@Override
		protected void run(int count) {
			actionbarChannel.update(toString());
			if (count <= 4) {
				if (lastBlock == null) {
					this.lastBlock = player.getLocation().getBlock();
				} else {
					final IBlockSnapshot snapshot = snapshots.remove(lastBlock);
					if (snapshot != null) snapshot.apply();
					this.lastBlock = lastBlock.getRelative(BlockFace.UP);
				}
				if (!lastBlock.getType().equals(MaterialX.LIGHT_BLUE_STAINED_GLASS.getMaterial()) &&
						!lastBlock.getType().equals(MaterialX.LIME_STAINED_GLASS.getMaterial()) &&
						!lastBlock.getType().equals(MaterialX.ORANGE_STAINED_GLASS.getMaterial()) &&
						!lastBlock.getType().equals(MaterialX.PINK_STAINED_GLASS.getMaterial()) &&
						!lastBlock.getType().equals(MaterialX.YELLOW_STAINED_GLASS.getMaterial()) &&
						!lastBlock.getType().equals(MaterialX.WHITE_STAINED_GLASS.getMaterial())) {
					addSnapshot(lastBlock);	
				}
				if (!checkBlocks(lastBlock)) {
					setCount(5);
				}
				SoundLib.ENTITY_ENDER_EYE_DEATH.playSound(lastBlock.getLocation(), .8f, 1.6f);
				BlockX.setType(lastBlock, materialX);
				final Location lastBlockLoc = lastBlock.getLocation(), loc = player.getLocation().clone();
				loc.setX(lastBlockLoc.getX() + .5);
				loc.setY(lastBlockLoc.getY() + 2);
				loc.setZ(lastBlockLoc.getZ() + .5);
				player.teleport(loc);
			} else {
				if (!placed) {
					this.placed = true;
					final IBlockSnapshot snapshot = snapshots.remove(lastBlock);
					if (snapshot != null) snapshot.apply();
					final Block block = player.getLocation().getBlock();
					final CoordConsumer consumer = new CoordConsumer() {
						@Override
						public void accept(int x, int y, int z, MaterialX material) {
							final Block blockRel = block.getRelative(x, y, z);
							if (!blockRel.getType().equals(MaterialX.LIGHT_BLUE_STAINED_GLASS.getMaterial()) &&
								!blockRel.getType().equals(MaterialX.LIME_STAINED_GLASS.getMaterial()) &&
								!blockRel.getType().equals(MaterialX.ORANGE_STAINED_GLASS.getMaterial()) &&
								!blockRel.getType().equals(MaterialX.PINK_STAINED_GLASS.getMaterial()) &&
								!blockRel.getType().equals(MaterialX.YELLOW_STAINED_GLASS.getMaterial()) &&
								!blockRel.getType().equals(MaterialX.WHITE_STAINED_GLASS.getMaterial())) {
								addSnapshot(blockRel);	
							}
							SoundLib.ENTITY_ENDER_EYE_DEATH.playSound(lastBlock.getLocation(), .8f, 1.8f);
							BlockX.setType(blockRel, material);
						}
					};
					switch(color) {
					case 1:
						for (Relative relative : relatives1) {
							relative.forEach(consumer);
						}
						break;
					case 2:
						for (Relative relative : relatives2) {
							relative.forEach(consumer);
						}
						break;
					case 3:
						for (Relative relative : relatives3) {
							relative.forEach(consumer);
						}
						break;
					case 4:
						for (Relative relative : relatives4) {
							relative.forEach(consumer);
						}
						break;
					case 5:
						for (Relative relative : relatives5) {
							relative.forEach(consumer);
						}
						break;
					case 6:
						for (Relative relative : relatives6) {
							relative.forEach(consumer);
						}
						break;
					}
					final Location blockLoc = block.getLocation(), loc = player.getLocation().clone();
					loc.setX(blockLoc.getX() + .5);
					loc.setY(blockLoc.getY());
					loc.setZ(blockLoc.getZ() + .5);
					player.teleport(loc);
				}
			}
		}

		private boolean checkBlocks(final Block criterion) {
			if (!criterion.isEmpty()) return false;
			final Block up = criterion.getRelative(BlockFace.UP);
			return up.isEmpty() && up.getRelative(BlockFace.UP).isEmpty();
		}

		@EventHandler
		private void onPlayerMove(final PlayerMoveEvent e) {
			if (player.getUniqueId().equals(e.getPlayer().getUniqueId())) {
				final Location to = e.getTo(), from = e.getFrom();
				if (to != null) {
					to.setX(from.getX());
					to.setY(from.getY());
					to.setZ(from.getZ());
				}
			}
		}

		@EventHandler
		private void onEntityDamage(final EntityDamageEvent e) {
			if (player.getUniqueId().equals(e.getEntity().getUniqueId())) {
				e.setCancelled(true);
			}
		}
		
		@EventHandler
		private void onBlockBreak(final BlockBreakEvent e) {
			if (snapshots.containsKey(e.getBlock())) {
				e.setCancelled(true);
				return;
			}
		}

		@EventHandler
		private void onEntityRegainHealth(final EntityRegainHealthEvent e) {
			if (player.equals(e.getEntity())) {
				e.setCancelled(true);
				new AbilityTimer(TaskType.NORMAL, 20) {
					
					private final Location startLocation = e.getEntity().getLocation().clone();
					
					@Override
					protected void run(int count) {
						ParticleLib.VILLAGER_HAPPY.spawnParticle(startLocation.clone().add(0, 1, 0).add(Line.vectorAt(startLocation.clone().add(0, 1, 0), getPlayer().getLocation().clone().add(0, 1, 0), 20, count)), 0, 0, 0, 1, 0);
					}
					
					@Override
					protected void onEnd() {
						onSilentEnd();
					}
					
					@Override
					protected void onSilentEnd() {
						double maxHealth = getPlayer().getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue();
						double yellowHeart = NMS.getAbsorptionHearts(getPlayer());
						if (maxHealth <= yellowHeart) {
							Healths.setHealth(getPlayer(), getPlayer().getHealth() + e.getAmount());
						} else {
							NMS.setAbsorptionHearts(getPlayer(), (float) (yellowHeart + (e.getAmount() * 2)));
						}
						SoundLib.ENTITY_PLAYER_LEVELUP.playSound(getPlayer());
					}
					
				}.setPeriod(TimeUnit.TICKS, 1).start();
			}
		}
		
		private void restoreAll() {
			for (final Iterator<IBlockSnapshot> iterator = snapshots.values().iterator(); iterator.hasNext(); ) {
				iterator.next().apply();
				iterator.remove();
			}
		}

		@Override
		protected void onStart() {
			Bukkit.getPluginManager().registerEvents(this, AbilityWar.getPlugin());
		}

		@Override
		protected void onEnd() {
			onSilentEnd();
		}

		@Override
		protected void onSilentEnd() {
			actionbarChannel.unregister();
			restoreAll();
			HandlerList.unregisterAll(this);
			crystalized.remove(player.getUniqueId());
		}

		@Override
		public final String toString() {
			return "§b결정화§f" + ": §a" + ((maxCount - getCount()) / 10.0) + "초";
		}
		
	}
	
	private final List<Runnable> SOUND_RUNNABLES = Arrays.asList(
			() -> {
				SoundLib.CHIME.playInstrument(getPlayer().getLocation(), Note.natural(1, Tone.D));
			},
			() -> {
				SoundLib.CHIME.playInstrument(getPlayer().getLocation(), Note.natural(1, Tone.B));
			},
			() -> {
				SoundLib.CHIME.playInstrument(getPlayer().getLocation(), Note.natural(1, Tone.A));
			},
			() -> {
				SoundLib.CHIME.playInstrument(getPlayer().getLocation(), Note.natural(1, Tone.G));
			},
			() -> {
				SoundLib.CHIME.playInstrument(getPlayer().getLocation(), Note.natural(1, Tone.A));
			},
			() -> {
				SoundLib.CHIME.playInstrument(getPlayer().getLocation(), Note.natural(1, Tone.G));
			},
			() -> {
				SoundLib.CHIME.playInstrument(getPlayer().getLocation(), Note.natural(1, Tone.A));
			},
			() -> {
				SoundLib.CHIME.playInstrument(getPlayer().getLocation(), Note.natural(1, Tone.B));
			},
			() -> {
				SoundLib.CHIME.playInstrument(getPlayer().getLocation(), Note.natural(0, Tone.D));
			},
			() -> {
				SoundLib.CHIME.playInstrument(getPlayer().getLocation(), Note.natural(0, Tone.D));
			},
			() -> {
				SoundLib.CHIME.playInstrument(getPlayer().getLocation(), Note.natural(1, Tone.G));
			},
			() -> {
				SoundLib.CHIME.playInstrument(getPlayer().getLocation(), Note.natural(1, Tone.A));
			},
			() -> {
				SoundLib.CHIME.playInstrument(getPlayer().getLocation(), Note.natural(1, Tone.G));
			},
			() -> {
				SoundLib.CHIME.playInstrument(getPlayer().getLocation(), Note.natural(1, Tone.A));
			},
			() -> {
				SoundLib.CHIME.playInstrument(getPlayer().getLocation(), Note.natural(1, Tone.B));
			},
			() -> {
				SoundLib.CHIME.playInstrument(getPlayer().getLocation(), Note.natural(1, Tone.B));
			},
			() -> {
				SoundLib.CHIME.playInstrument(getPlayer().getLocation(), Note.natural(1, Tone.D));
			},
			() -> {
				SoundLib.CHIME.playInstrument(getPlayer().getLocation(), Note.natural(1, Tone.G));
			},
			() -> {
				SoundLib.CHIME.playInstrument(getPlayer().getLocation(), Note.natural(1, Tone.B));
			},
			() -> {
				SoundLib.CHIME.playInstrument(getPlayer().getLocation(), Note.natural(1, Tone.C));
			},
			() -> {
				SoundLib.CHIME.playInstrument(getPlayer().getLocation(), Note.natural(1, Tone.B));
			},
			() -> {
				SoundLib.CHIME.playInstrument(getPlayer().getLocation(), Note.natural(1, Tone.G));
			},
			() -> {
				SoundLib.CHIME.playInstrument(getPlayer().getLocation(), Note.natural(0, Tone.E));
			},
			() -> {
				SoundLib.CHIME.playInstrument(getPlayer().getLocation(), Note.natural(1, Tone.G));
			},
			() -> {
				SoundLib.CHIME.playInstrument(getPlayer().getLocation(), Note.natural(1, Tone.B));
			}
		);
	
	public class Bullet extends AbilityTimer implements Listener {

		private final LivingEntity shooter;
		private final CustomEntity entity;
		private Vector forward;
		private int stacks = 0;
		private boolean turns = true;
		private boolean add = false;
		private Arrow arrow;
		private double length;

		private RGB gradation;

		private final RGB gradation1 = RGB.of(3, 212, 168), gradation2 = RGB.of(8, 212, 178),
				gradation3 = RGB.of(15, 213, 190), gradation4 = RGB.of(18, 211, 198), gradation5 = RGB.of(27, 214, 213),
				gradation6 = RGB.of(29, 210, 220), gradation7 = RGB.of(30, 207, 225), gradation8 = RGB.of(24, 196, 223),
				gradation9 = RGB.of(23, 191, 226), gradation10 = RGB.of(19, 182, 226),
				gradation11 = RGB.of(16, 174, 227), gradation12 = RGB.of(13, 166, 228),
				gradation13 = RGB.of(10, 159, 228), gradation14 = RGB.of(7, 151, 229),
				gradation15 = RGB.of(3, 143, 229), gradation16 = RGB.of(1, 135, 230), gradation17 = RGB.of(1, 126, 222),
				gradation18 = RGB.of(1, 118, 214), gradation19 = RGB.of(1, 109, 207), gradation20 = RGB.of(1, 101, 199),
				gradation21 = RGB.of(1, 92, 191);

		@SuppressWarnings("serial")
		private List<RGB> gradations = new ArrayList<RGB>() {
			{
				add(gradation1);
				add(gradation2);
				add(gradation3);
				add(gradation4);
				add(gradation5);
				add(gradation6);
				add(gradation7);
				add(gradation8);
				add(gradation9);
				add(gradation10);
				add(gradation11);
				add(gradation12);
				add(gradation13);
				add(gradation14);
				add(gradation15);
				add(gradation16);
				add(gradation17);
				add(gradation18);
				add(gradation19);
				add(gradation20);
				add(gradation21);
			}
		};

		private Location lastLocation;

		private Bullet(LivingEntity shooter, Location startLocation, Arrow arrow, double length) {
			super(200);
			setPeriod(TimeUnit.TICKS, 1);
			bullets.put(arrow, this);
			this.shooter = shooter;
			this.arrow = arrow;
			this.length = length;
			this.entity = new Bullet.ArrowEntity(startLocation.getWorld(), startLocation.getX(), startLocation.getY(),
					startLocation.getZ()).resizeBoundingBox(-.75, -.75, -.75, .75, .75, .75);
			this.lastLocation = startLocation;
			this.forward = arrow.getLocation().clone().subtract(entity.getLocation().clone()).toVector().normalize().multiply(length);
		}

		@Override
		protected void onStart() {
			Bukkit.getPluginManager().registerEvents(this, AbilityWar.getPlugin());
		}

		@Override
		protected void run(int i) {
			this.forward = arrow.getLocation().clone().subtract(entity.getLocation().clone()).toVector().normalize().multiply(length);
			Location newLocation = lastLocation.clone().add(forward);
			for (Iterator<Location> iterator = new Iterator<Location>() {
				private final Vector vectorBetween = newLocation.toVector().subtract(lastLocation.toVector()),
						unit = vectorBetween.clone().normalize().multiply(.1);
				private final int amount = (int) (vectorBetween.length() / 0.1);
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
				entity.setLocation(location);
				add = !add;
				if (add) {
					if (turns)
						stacks++;
					else
						stacks--;
					if (stacks % (gradations.size() - 1) == 0) {
						turns = !turns;
					}
					gradation = gradations.get(stacks);
				}
				ParticleLib.REDSTONE.spawnParticle(location, gradation);
			}
			lastLocation = newLocation;
		}

		@Override
		protected void onEnd() {
			onSilentEnd();
		}

		@Override
		protected void onSilentEnd() {
			entity.remove();
		}
		
		public class ArrowEntity extends CustomEntity implements Deflectable {

			public ArrowEntity(World world, double x, double y, double z) {
				getGame().super(world, x, y, z);
			}

			@Override
			public Vector getDirection() {
				return forward.clone();
			}

			@Override
			public void onDeflect(Participant deflector, Vector newDirection) {
				stop(false);
				final Player deflectedPlayer = deflector.getPlayer();
				new Bullet(deflectedPlayer, lastLocation, arrow, length).start();
			}

			@Override
			public ProjectileSource getShooter() {
				return shooter;
			}

			@Override
			protected void onRemove() {
			}

		}

	}
	
}