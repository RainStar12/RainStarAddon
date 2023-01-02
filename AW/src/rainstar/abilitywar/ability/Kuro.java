package rainstar.abilitywar.ability;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nullable;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Damageable;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByBlockEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerArmorStandManipulateEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BlockIterator;
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
import daybreak.abilitywar.game.module.DeathManager;
import daybreak.abilitywar.game.team.interfaces.Teamable;
import daybreak.abilitywar.utils.base.Formatter;
import daybreak.abilitywar.utils.base.color.RGB;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.base.math.LocationUtil;
import daybreak.abilitywar.utils.base.math.VectorUtil;
import daybreak.abilitywar.utils.base.math.VectorUtil.Vectors;
import daybreak.abilitywar.utils.base.math.geometry.Crescent;
import daybreak.abilitywar.utils.base.math.geometry.Line;
import daybreak.abilitywar.utils.base.minecraft.FallingBlocks;
import daybreak.abilitywar.utils.base.minecraft.FallingBlocks.Behavior;
import daybreak.abilitywar.utils.base.minecraft.damage.Damages;
import daybreak.abilitywar.utils.base.minecraft.nms.NMS;
import daybreak.abilitywar.utils.base.minecraft.version.ServerVersion;
import daybreak.abilitywar.utils.library.MaterialX;
import daybreak.abilitywar.utils.library.ParticleLib;
import daybreak.abilitywar.utils.library.SoundLib;
import daybreak.google.common.base.Predicate;
import daybreak.google.common.collect.ImmutableSet;
import rainstar.abilitywar.synergy.DemonLord;

@AbilityManifest(
		name = "쿠로", rank = Rank.A, species = Species.HUMAN, explain = {
		"§7패시브 §c- §8어둠의 추종자§f: 자신이 있는 위치가 어두울수록 스킬 피해량이 증가합니다.",
		" 실명을 가진 적을 근접 공격하면 추가 피해를 입힙니다.",
		"§7근접 타격 후 F §8- §5차원 절단§f: 바라보는 방향으로 빠르게 질주합니다.",
		" 질주하며 지나간 공간을 절단시켜 주변 엔티티들을 끌어와 피해를 입히고",
		" 잠시간 차원의 저편으로 보내버립니다. $[COOLDOWN]",
		" §7대시형 절단§f: $[DASH_CONFIG]",
		"§7패시브 §8- §c마안 개방§f: 치명적인 피해를 입었을 때, 체력을 최대 체력의 절반까지",
		" 즉시 회복하고 마안을 개방합니다."
		},
		summarize = {
		"§7근접 타격 후 검을 들고 F키를 빠르게§f 누르면 바라보는 방향으로 질주해",
		"지나간 공간을 절단시켜 주변 엔티티들을 끌어와 피해를 입히고",
		"잠시간 §3차원의 저편§f으로 보내 공격 불능, 무적, 타게팅 불능 상태로 만듭니다.",
		" $[COOLDOWN]",
		"사망 위기에 놓일 때 체력을 최대 체력의 절반까지 즉시 회복 후",
		"마안이 개방된 능력으로 변경됩니다."
		})

@SuppressWarnings("deprecation")
public class Kuro extends AbilityBase {

	public Kuro(Participant participant) {
		super(participant);
	}
	
	public static final SettingObject<Integer> COOLDOWN 
	= abilitySettings.new SettingObject<Integer>(Kuro.class,
			"cooldown", 50, "# 차원 절단 쿨타임",
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
	
	public static final SettingObject<Boolean> DASH_CONFIG = abilitySettings.new SettingObject<Boolean>(Kuro.class,
			"dash-config", false, "# 차원 절단 이동 대시형 여부", "# 질주 개념으로 적용시켜 블럭을 관통할 지 정합니다.") {
		
		@Override
		public String toString() {
                return getValue() ? "§b켜짐" : "§c꺼짐";
        }
		
	};
	
	private final Cooldown cool = new Cooldown(COOLDOWN.getValue(), "차원 절단", CooldownDecrease._50);
	private Map<Player, NextDimension> nextMap = new HashMap<>(); 
	private final Crescent crescent = Crescent.of(1, 20);
	private boolean dashboolean = DASH_CONFIG.getValue();
	private static final Vector zerov = new Vector(0, 0, 0);
	private Location startLocation;
	
	private static final Set<Material> swords;
	
	static {
		if (MaterialX.NETHERITE_SWORD.isSupported()) {
			swords = ImmutableSet.of(MaterialX.WOODEN_SWORD.getMaterial(), Material.STONE_SWORD, Material.IRON_SWORD, MaterialX.GOLDEN_SWORD.getMaterial(), Material.DIAMOND_SWORD, MaterialX.NETHERITE_SWORD.getMaterial());
		} else {
			swords = ImmutableSet.of(MaterialX.WOODEN_SWORD.getMaterial(), Material.STONE_SWORD, Material.IRON_SWORD, MaterialX.GOLDEN_SWORD.getMaterial(), Material.DIAMOND_SWORD);
		}
	}
	
	private final AbilityTimer attacked = new AbilityTimer(7) {
		
		@Override
		public void run(int count) {
		}
		
	}.setPeriod(TimeUnit.TICKS, 1).register();
	
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
	
	@SubscribeEvent
	private void onArmorStandManipulate(PlayerArmorStandManipulateEvent e) {
		if (e.getRightClicked().hasMetadata("DimensionCutter")) e.setCancelled(true);
	}
	
	@SubscribeEvent(priority = Priority.HIGHEST)
	public void onEntityDamage(EntityDamageEvent e) {
		if (e.getEntity().equals(getPlayer())) {
	    	if (getPlayer().getHealth() - e.getFinalDamage() <= 0 && !e.isCancelled()) {
				getPlayer().setHealth(getPlayer().getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue() / 2);
			   	SoundLib.ENTITY_ELDER_GUARDIAN_CURSE.playSound(getPlayer(), 1, 0.7f);
			   	getPlayer().sendMessage("§8[§7!§8] §c죽음의 위기§f에서 탈출하기 위해 §4금지된 마안§f을 §3개방§f하였습니다! §7/aw check");
		    	AbilityBase ab = getParticipant().getAbility();
		    	if (ab.getClass().equals(Mix.class)) {
		    		final Mix mix = (Mix) ab;
					final AbilityBase first = mix.getFirst(), second = mix.getSecond();
					if (this.equals(first)) {
						try {
							mix.setAbility(KuroEye.class, second.getClass());
						} catch (ReflectiveOperationException e1) {
							e1.printStackTrace();
						}
					} else if (this.equals(second)) {
						try {
							mix.setAbility(first.getClass(), KuroEye.class);
						} catch (ReflectiveOperationException e1) {
							e1.printStackTrace();
						}
					}
		    	} else {
			    	try {
						getParticipant().setAbility(KuroEye.class);
					} catch (UnsupportedOperationException | ReflectiveOperationException e1) {
						e1.printStackTrace();
					}	
		    	}
		    	e.setCancelled(true);
	    	}
		}
	}
	
	@SubscribeEvent(priority = Priority.HIGHEST)
	public void onEntityDamageByBlock(EntityDamageByBlockEvent e) {
		onEntityDamage(e);
	}
	
	@SubscribeEvent(priority = Priority.HIGHEST)
	public void onEntityDamageByEntity(EntityDamageByEntityEvent e) {
		onEntityDamage(e);
		if (getPlayer().equals(e.getDamager())) {
			if (attacked.isRunning()) attacked.setCount(7);
			else attacked.start();
			if (e.getEntity() instanceof Player) {
				Player player = (Player) e.getEntity();
				if (player.hasPotionEffect(PotionEffectType.BLINDNESS)) {
					e.setDamage(e.getDamage() + 2);
				}
			}
		}
	}
	
	private final AbilityTimer dashing = new AbilityTimer(1) {
		
		@Override
		public void onStart() {
			startLocation = getPlayer().getLocation();
	    	getPlayer().setVelocity(VectorUtil.validateVector(getPlayer().getLocation().getDirection().normalize().multiply(10)));
	   	}
	   	
	   	@Override
	   	public void onEnd() {
	   		onSilentEnd();
	   	}
	    	
	   	@Override
	    public void onSilentEnd() {
			getPlayer().setVelocity(zerov);
			new BukkitRunnable() {
				
				@Override
				public void run() {
					new DimensionCutter(startLocation, getPlayer().getLocation()).start();
					new CutterParticle(startLocation, getPlayer().getLocation()).start();
				}
				
			}.runTaskLater(AbilityWar.getPlugin(), 1L);
	   	}
	
	}.setPeriod(TimeUnit.TICKS, 1).register();
	
    @SubscribeEvent(onlyRelevant = true)
    public void onPlayerSwapHandItems(PlayerSwapHandItemsEvent e) {
    	if (swords.contains(e.getOffHandItem().getType())) {
    		if (!cool.isCooldown() && attacked.isRunning()) {
    			if (dashboolean) {
    				dashing.start();
    				cool.start();
    			} else {
        			Block lastEmpty = null;
        			try {
    					for (BlockIterator iterator = new BlockIterator(getPlayer().getWorld(), getPlayer().getLocation().toVector(), getPlayer().getLocation().getDirection().clone().setY(0).normalize(), 1, 10); iterator.hasNext(); ) {
    						final Block block = iterator.next();
    						if (!block.getType().isSolid()) {
    							lastEmpty = block;
    						}
    					}
    				} catch (IllegalStateException ignored) {
    				}
    				if (lastEmpty != null) {
    					new CutParticle(45, getPlayer().getLocation(), getPlayer().getLocation().getDirection(), RGB.of(50, 50, 50)).start();
    					new CutParticle(-45, getPlayer().getLocation(), getPlayer().getLocation().getDirection(), RGB.of(50, 50, 50)).start();
    					new DimensionCutter(getPlayer().getLocation(), lastEmpty.getLocation().clone().subtract(0, 0.5, 0)).start();
    					new CutterParticle(getPlayer().getLocation(), lastEmpty.getLocation().clone().subtract(0, 0.5, 0)).start();
    					getPlayer().teleport(LocationUtil.floorY(lastEmpty.getLocation()).setDirection(getPlayer().getLocation().getDirection()));
    					new CutParticle(45, getPlayer().getLocation(), getPlayer().getLocation().getDirection(), RGB.of(150, 150, 150)).start();
    					new CutParticle(-45, getPlayer().getLocation(), getPlayer().getLocation().getDirection(), RGB.of(150, 150, 150)).start();
            			cool.start();
    				} else {
    					getPlayer().sendMessage("§4[§c!§4] §f바라보는 방향에 이동할 수 있는 곳이 없습니다.");
    				}	
    			}
    		}
			e.setCancelled(true);
    	}
    }
	
	private class CutParticle extends AbilityTimer {

		private final Vector axis;
		private final Vector vector;
		private final Vectors crescentVectors;
		private final Location location;
		private final RGB color;

		private CutParticle(double angle, Location location, Vector direction, RGB color) {
			super(4);
			setPeriod(TimeUnit.TICKS, 1);
			this.axis = VectorUtil.rotateAroundAxis(VectorUtil.rotateAroundAxisY(direction.setY(0).normalize(), 90), direction.setY(0).normalize(), angle);
			this.vector = direction.setY(0).normalize().multiply(0.5);
			this.location = location;
			this.crescentVectors = crescent.clone()
					.rotateAroundAxisY(-location.getYaw())
					.rotateAroundAxis(direction.setY(0).normalize(), (180 - angle) % 180)
					.rotateAroundAxis(axis, -75);
			this.color = color;
		}

		@Override
		protected void run(int count) {
			Location baseLoc = location.clone().add(vector).add(0, 1.3, 0);
			for (Location loc : crescentVectors.toLocations(baseLoc)) {
				ParticleLib.REDSTONE.spawnParticle(loc, color);
			}
			crescentVectors.rotateAroundAxis(axis, 40);
		}

	}
    
	private class CutterParticle extends AbilityTimer {
		
    	private final Location startLoc;
    	private final Location endLoc;
    	private List<Location> locations = new ArrayList<>();
		
		private CutterParticle(Location startLoc, Location endLoc) {
			super(TaskType.NORMAL, Integer.MAX_VALUE);
			setPeriod(TimeUnit.TICKS, 5);
			this.startLoc = startLoc.clone().add(0, 3, 0);
			this.endLoc = endLoc.clone().add(0, 3, 0);
		}
		
		@Override
		protected void onStart() {
			locations.addAll(Line.between(startLoc, endLoc, (int) Math.min(15, Math.sqrt(startLoc.distanceSquared(endLoc)))).toLocations(startLoc));
		}
		
    	@Override
    	protected void run(int count) {
    		if (startLoc.equals(endLoc)) stop(false);
        	if (locations.size() == count) {
        		stop(false);
        	} else {
            	ArmorStand armorstand = locations.get(count - 1).getWorld().spawn(locations.get(count - 1), ArmorStand.class);
            	armorstand.setRightArmPose(new EulerAngle(Math.toRadians(80), 0, 0));
            	armorstand.setMetadata("DimensionCutter", new FixedMetadataValue(AbilityWar.getPlugin(), null));
            	armorstand.setVisible(false);
            	armorstand.getEquipment().setItemInMainHand(new ItemStack(Material.IRON_SWORD));
            	armorstand.setGravity(true);
            	NMS.removeBoundingBox(armorstand);
            	armorstand.setVelocity(new Vector(0, -0.5, 0));
            	new AbilityTimer(2) {
            		
            		@Override
            		public void run(int count) {
                    	armorstand.setVelocity(new Vector(0, -1, 0));
            		}
            		
            	}.setPeriod(TimeUnit.TICKS, 1).start();
    			new BukkitRunnable() {
    				@Override
    				public void run() {
    					armorstand.teleport(LocationUtil.floorY(armorstand.getLocation(), blockpredicate).subtract(0, 0.5, 0));
    					if (ServerVersion.getVersion() >= 13) {
    						for (Block block : LocationUtil.getBlocks2D(armorstand.getLocation(), 2, true, true, true)) {
    							if (block.getType() == Material.AIR) block = block.getRelative(BlockFace.DOWN);
    							if (block.getType() == Material.AIR) continue;
    							Location location = block.getLocation().add(0, 1, 0);
    							FallingBlocks.spawnFallingBlock(location, block.getType(), false, getPlayer().getLocation().toVector().subtract(location.toVector()).multiply(-0.05).setY(Math.random()), Behavior.FALSE);
    						}
    					} else {
    						for (Block block : LocationUtil.getBlocks2D(armorstand.getLocation(), 2, true, true, true)) {
    							if (block.getType() == Material.AIR) block = block.getRelative(BlockFace.DOWN);
    							if (block.getType() == Material.AIR) continue;
    							Location location = block.getLocation().add(0, 1, 0);
    							FallingBlocks.spawnFallingBlock(location, block.getType(), block.getData(), false, getPlayer().getLocation().toVector().subtract(location.toVector()).multiply(-0.05).setY(Math.random()), Behavior.FALSE);
    						}
    					}
    					armorstand.getWorld().strikeLightningEffect(armorstand.getLocation());
    					SoundLib.ENTITY_GENERIC_EXPLODE.playSound(armorstand.getLocation());
    				}
    			}.runTaskLater(AbilityWar.getPlugin(), 3L);
    			new BukkitRunnable() {
    				@Override
    				public void run() {
    					armorstand.remove();
    				}
    			}.runTaskLater(AbilityWar.getPlugin(), 4L);
        	}
    	}
    	
    	@Override
    	protected void onEnd() {
    		onSilentEnd();
    	}
    	
    	@Override
    	protected void onSilentEnd() {
    		for (Location loc : Line.between(startLoc, endLoc, (int) Math.max(1, Math.min(15, Math.sqrt(startLoc.distanceSquared(endLoc))))).toLocations(startLoc)) {
    			ArmorStand armorstand = loc.getWorld().spawn(loc, ArmorStand.class);
    			armorstand.setRightArmPose(new EulerAngle(Math.toRadians(80), 0, 0));
            	armorstand.setMetadata("DimensionCutter", new FixedMetadataValue(AbilityWar.getPlugin(), null));
            	armorstand.setVisible(false);
            	armorstand.getEquipment().setItemInMainHand(new ItemStack(Material.IRON_SWORD));
            	armorstand.setGravity(true);
            	NMS.removeBoundingBox(armorstand);
            	armorstand.setVelocity(new Vector(0, -1, 0));
    			new BukkitRunnable() {
    				@Override
    				public void run() {
    					armorstand.teleport(LocationUtil.floorY(armorstand.getLocation(), blockpredicate).subtract(0, 0.5, 0));
    					if (ServerVersion.getVersion() >= 13) {
    						for (Block block : LocationUtil.getBlocks2D(armorstand.getLocation(), 2, true, true, true)) {
    							if (block.getType() == Material.AIR) block = block.getRelative(BlockFace.DOWN);
    							if (block.getType() == Material.AIR) continue;
    							Location location = block.getLocation().add(0, 1, 0);
    							FallingBlocks.spawnFallingBlock(location, block.getType(), false, getPlayer().getLocation().toVector().subtract(location.toVector()).multiply(-0.05).setY(Math.random()), Behavior.FALSE);
    						}
    					} else {
    						for (Block block : LocationUtil.getBlocks2D(armorstand.getLocation(), 2, true, true, true)) {
    							if (block.getType() == Material.AIR) block = block.getRelative(BlockFace.DOWN);
    							if (block.getType() == Material.AIR) continue;
    							Location location = block.getLocation().add(0, 1, 0);
    							FallingBlocks.spawnFallingBlock(location, block.getType(), block.getData(), false, getPlayer().getLocation().toVector().subtract(location.toVector()).multiply(-0.05).setY(Math.random()), Behavior.FALSE);
    						}
    					}
    					armorstand.getWorld().strikeLightningEffect(armorstand.getLocation());
    					SoundLib.ENTITY_GENERIC_EXPLODE.playSound(armorstand.getLocation());
    				}
				}.runTaskLater(AbilityWar.getPlugin(), 3L);
    			new BukkitRunnable() {
    				@Override
    				public void run() {
    					armorstand.remove();
    				}
    			}.runTaskLater(AbilityWar.getPlugin(), 4L);
    		}
    	}
		
	}
    
	private class DimensionCutter extends AbilityTimer {
		
    	private Set<Damageable> damagedcheck = new HashSet<>();
    	private final Location startLoc;
    	private final Location endLoc;
    	private final Location midLoc;
		
		private DimensionCutter(Location startLoc, Location endLoc) {
			super(100);
			setPeriod(TimeUnit.TICKS, 1);
			this.startLoc = startLoc.clone().add(0, 1, 0);
			this.endLoc = endLoc.clone().add(0, 1, 0);
			this.midLoc = startLoc.clone().add(0, 1, 0).toVector().midpoint(endLoc.clone().add(0, 1, 0).toVector()).toLocation(getPlayer().getWorld());
		}
		
    	@Override
    	protected void run(int count) {
        	for (Location loc : Line.between(startLoc, endLoc, (int) Math.min(250, 5 * Math.sqrt(startLoc.distanceSquared(endLoc)))).toLocations(startLoc)) {
        		if (count % 10 == 0) {
        			ParticleLib.PORTAL.spawnParticle(loc, 0, 0, 0, 5, 0.25);
        		}
        	}
    		for (Damageable d : LocationUtil.rayTraceEntities(Damageable.class, startLoc, endLoc, 0.75, predicate)) {
    			if (d instanceof ArmorStand) continue;
    			if (!d.equals(getPlayer()) && !damagedcheck.contains(d)) {
        			Damages.damageMagic(d, getPlayer(), false, (float) (7 + (15 - getPlayer().getLocation().getBlock().getLightLevel()) * 0.4));
            		new AbilityTimer(20) {
            			
            			@Override
            			public void onStart() {
            				damagedcheck.add(d);
            			}
            			
            			@Override
            			public void onEnd() {
            				onSilentEnd();
            			}
            			
            			@Override
            			public void onSilentEnd() {
            				damagedcheck.remove(d);
            			} 
            		}.setPeriod(TimeUnit.TICKS, 1).start();
    			}
    		}
    		for (Damageable d : LocationUtil.getNearbyEntities(Damageable.class, midLoc, 5, 5, predicate)) {
    			d.setVelocity(VectorUtil.validateVector(midLoc.toVector().subtract(d.getLocation().toVector()).normalize().multiply(0.1)));
    			if (d.getLocation().distanceSquared(midLoc) < 1) {
    				if (d instanceof Player) {
    					Player p = (Player) d;
    					if (!nextMap.containsKey(p)) {
        					new NextDimension(p, p.getGameMode()).start();	
    					}
    				}
    			}
    		}
    	}
    	
	}
	
	private class NextDimension extends AbilityTimer implements Listener {
		
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
		
		private NextDimension(Player player, GameMode originalMode) {
			super(35);
			setPeriod(TimeUnit.TICKS, 2);
			this.player = player;
			if (originalMode.equals(GameMode.SPECTATOR)) {
				this.originalMode = GameMode.SURVIVAL;
			} else {
				this.originalMode = originalMode;
			}
			nextMap.put(player, this);
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
    		ParticleLib.SMOKE_LARGE.spawnParticle(player.getLocation().clone().add(0, 1, 0), 0, 0, 0, 3, 0);
    		actionbarChannel.update("§5차원의 저편§f: " + count / 10 + "초");
    	}
		
		@Override
		protected void onEnd() {
			onSilentEnd();
		}
		
		@Override
		protected void onSilentEnd() {
			HandlerList.unregisterAll(this);
			nextMap.remove(player);
			player.setGameMode(originalMode);
			getGame().getParticipant(player).attributes().TARGETABLE.setValue(true);
			actionbarChannel.unregister();
		}
		
	}
    
}