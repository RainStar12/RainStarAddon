package rainstar.aw.ability;

import com.google.common.base.Predicate;
import daybreak.abilitywar.ability.AbilityBase;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.ability.SubscribeEvent;
import daybreak.abilitywar.config.ability.AbilitySettings.SettingObject;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.AbstractGame.Participant.ActionbarNotification.ActionbarChannel;
import daybreak.abilitywar.game.manager.effect.Stun;
import daybreak.abilitywar.game.module.DeathManager;
import daybreak.abilitywar.game.team.interfaces.Teamable;
import daybreak.abilitywar.utils.base.Formatter;
import daybreak.abilitywar.utils.base.concurrent.SimpleTimer.TaskType;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.base.math.LocationUtil;
import daybreak.abilitywar.utils.base.math.VectorUtil;
import daybreak.abilitywar.utils.base.math.VectorUtil.Vectors;
import daybreak.abilitywar.utils.base.math.geometry.Crescent;
import daybreak.abilitywar.utils.base.minecraft.FallingBlocks;
import daybreak.abilitywar.utils.base.minecraft.FallingBlocks.Behavior;
import daybreak.abilitywar.utils.base.minecraft.version.ServerVersion;
import daybreak.abilitywar.utils.library.MaterialX;
import daybreak.abilitywar.utils.library.ParticleLib;
import daybreak.abilitywar.utils.library.SoundLib;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.WorldBorder;
import org.bukkit.attribute.Attribute;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByBlockEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import javax.annotation.Nullable;
import java.text.DecimalFormat;

@AbilityManifest(name = "미노타우로스", rank = Rank.S, species = Species.HUMAN, explain = {
		"§7검 공격 §8- §3무자비§f: 공격을 할 때마다 범위 피해를 입힙니다. §c쿨타임 §7: §f$[ATTACK_COOLDOWN]초",
		"§7패시브 §8- §c광폭화§f: 자신의 체력이 낮으면 낮을수록 근접 공격력이 강력해집니다.",
		" 돌진 스킬이 쿨타임일 때 피해를 입을 때마다 돌진 쿨타임이 $[COOLDOWN_DECREASE]%씩 감소합니다.",
		"§7검 우클릭 §8- §c돌진§f: 바라보는 방향으로 돌진합니다. $[RUSH_COOLDOWN]",
		" 돌진하는 동안은 무적 상태가 되고, 생명체나 지형에 충돌 전까지 2초간 돌진합니다.",
		" 이후 돌진이 끝난 지점 주변 $[CRASH_RANGE]칸 내에 피해를 입히며 $[STUN_DURATION]초간 기절시킵니다.",
		"§3[§b종족야생 콜라보 능력§3]"
		}, summarize = {
		"근접 공격을 시도할 때 광범위 피해를 입힙니다.",
		"체력이 낮을수록 근접 공격력이 강해집니다.",
		"돌진 스킬을 사용하여 벽이나 적에 충돌 시",
		"주변에 광역 피해를 입히고 기절시킵니다."
		})

public class Minotauros extends AbilityBase {

	public Minotauros(Participant participant) {
		super(participant);
	}
	
	@Override
	public void onUpdate(Update update) {
		if (update == Update.RESTRICTION_CLEAR) {
			passive.start();
		}
	}
	
	public static final SettingObject<Integer> RUSH_COOLDOWN = abilitySettings.new SettingObject<Integer>(Minotauros.class,
			"rush-cooldown", 100, "# 돌진 쿨타임") {
		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}

		@Override
		public String toString() {
			return Formatter.formatCooldown(getValue());
		}
	};
	
	public static final SettingObject<Double> ATTACK_COOLDOWN = abilitySettings.new SettingObject<Double>(Minotauros.class,
			"attack-cooldown", 1.2, "# 쿨타임", "# WRECK 게임 모드의 영향을 받지 않습니다.") {
		@Override
		public boolean condition(Double value) {
			return value >= 0;
		}
	};
	
	public static final SettingObject<Double> CRASH_RANGE = abilitySettings.new SettingObject<Double>(Minotauros.class,
			"crash-range", 5.0, "# 충돌 범위") {
		@Override
		public boolean condition(Double value) {
			return value >= 0;
		}
	};
	
	public static final SettingObject<Double> STUN_DURATION = abilitySettings.new SettingObject<Double>(Minotauros.class,
			"attack-cooldown", 2.2, "# 충돌된 적들의 기절 시간") {
		@Override
		public boolean condition(Double value) {
			return value >= 0;
		}
	};
	
	public static final SettingObject<Integer> COOLDOWN_DECREASE = abilitySettings.new SettingObject<Integer>(Minotauros.class,
			"cooldown-decrease", 8, "# 피격 시 돌진 쿨타임 감소 (단위: %)") {
		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}
	};
	
	private final Cooldown rush_cooldown = new Cooldown(RUSH_COOLDOWN.getValue(), 50);
	private final int attack_cooldown = (int) (ATTACK_COOLDOWN.getValue() * 20);
	private final int stunduration = (int) (STUN_DURATION.getValue() * 20);
	private final int decrease = COOLDOWN_DECREASE.getValue();
	private final double range = CRASH_RANGE.getValue();
	
	private final Crescent crescent1 = Crescent.of(2.8, 60);
	private final Crescent crescent2 = Crescent.of(2.8, 40);
	private final Crescent crescent3 = Crescent.of(2.8, 20);
	private int particleSide = 15;
	private double skillDamage;
	private final DecimalFormat df = new DecimalFormat("0.0");
	private ActionbarChannel ac = newActionbarChannel();
	
	private final Predicate<Entity> predicate = new Predicate<Entity>() {
		@Override
		public boolean test(Entity entity) {
			if (entity instanceof ArmorStand) return false;
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
	
	private final Predicate<Entity> teamExceptPredicate = new Predicate<Entity>() {
		@Override
		public boolean test(Entity entity) {
			if (entity instanceof ArmorStand) return false;
			if (entity.equals(getPlayer())) return false;
			if (entity instanceof Player) {
				if (!getGame().isParticipating(entity.getUniqueId())
						|| (getGame() instanceof DeathManager.Handler && ((DeathManager.Handler) getGame()).getDeathManager().isExcluded(entity.getUniqueId()))
						|| !getGame().getParticipant(entity.getUniqueId()).attributes().TARGETABLE.getValue()) {
					return false;
				}
			}
			return true;
		}

		@Override
		public boolean apply(@Nullable Entity arg0) {
			return false;
		}
	};
	
	private AbilityTimer passive = new AbilityTimer() {
		
		@Override
		public void run(int count) {
			double maxHP = getPlayer().getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue();
			double add = (((double) (maxHP - getPlayer().getHealth()) / maxHP) * 0.3) * 100;
			ac.update("§c공격력§7: §e" + df.format(add) + "§b%");
		}
		
	}.setPeriod(TimeUnit.TICKS, 1);
	
	private AbilityTimer sweep_cooldown = new AbilityTimer(TaskType.REVERSE, attack_cooldown) {
		
	}.setPeriod(TimeUnit.TICKS, 1);
	
	private AbilityTimer rush = new AbilityTimer(TaskType.REVERSE, 40) {
		
		@Override
		public void onStart() {
			SoundLib.ENTITY_RAVAGER_ROAR.playSound(getPlayer().getLocation(), 1.25f, 1.35f);
		}
		
		@SuppressWarnings("deprecation")
		@Override
		public void run(int count) {
			if (count % 4 == 0) SoundLib.ENTITY_HORSE_GALLOP.playSound(getPlayer().getLocation(), 0.35f, 1.15f);
			if (getPlayer().isOnGround()) {
				getPlayer().setVelocity(getPlayer().getLocation().getDirection().normalize().multiply(1.15));
			}
			getPlayer().addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 2, 2, false, false));
			if (isBlockObstructing()) {
				SoundLib.ENTITY_ZOMBIE_BREAK_WOODEN_DOOR.playSound(getPlayer().getLocation());
				SoundLib.ENTITY_ZOMBIE_ATTACK_IRON_DOOR.playSound(getPlayer().getLocation());
				SoundLib.ENTITY_ZOMBIE_ATTACK_WOODEN_DOOR.playSound(getPlayer().getLocation());
				rush.stop(false);
			}
			if (LocationUtil.getConflictingEntities(LivingEntity.class, getPlayer(), teamExceptPredicate).size() >= 1) {
				SoundLib.ENTITY_ZOMBIE_BREAK_WOODEN_DOOR.playSound(getPlayer().getLocation());
				SoundLib.ENTITY_ZOMBIE_ATTACK_IRON_DOOR.playSound(getPlayer().getLocation());
				SoundLib.ENTITY_ZOMBIE_ATTACK_WOODEN_DOOR.playSound(getPlayer().getLocation());
				rush.stop(false);
			}
		}
		
		@Override
		public void onEnd() {
			onSilentEnd();
		}
		
		@Override
		public void onSilentEnd() {
			for (LivingEntity livingEntity : LocationUtil.getNearbyEntities(LivingEntity.class, getPlayer().getLocation(), range, range, predicate)) {
				livingEntity.damage(skillDamage * 1.5, getPlayer());
				if (livingEntity instanceof Player) Stun.apply(getGame().getParticipant((Player) livingEntity), TimeUnit.TICKS, stunduration);
			}
			ParticleLib.FLASH.spawnParticle(getPlayer().getLocation(), 0, 0, 0, 1, 0);
			ParticleLib.EXPLOSION_HUGE.spawnParticle(getPlayer().getLocation(), 0, 0, 0, 1, 0);
			SoundLib.ENTITY_GENERIC_EXPLODE.playSound(getPlayer().getLocation(), 1, 0.5f);
			for (Block blocks : LocationUtil.getBlocks2D(getPlayer().getLocation(), 5, true, true, true)) {
				if (blocks.getType() == Material.AIR) blocks = blocks.getRelative(BlockFace.DOWN);
				if (blocks.getType() == Material.AIR) continue;
				Location location = blocks.getLocation().add(0, 1, 0);
				FallingBlocks.spawnFallingBlock(location, blocks.getType(), false, getPlayer().getLocation().toVector().subtract(location.toVector()).multiply(-0.1).setY(Math.random()), Behavior.FALSE);
			}
			getPlayer().removePotionEffect(PotionEffectType.SLOW);
			rush_cooldown.start();
			skillDamage = 1;
		}
		
		private boolean isBlockObstructing() {
			final Location playerLocation = getPlayer().getLocation().clone();
			final Vector direction = playerLocation.getDirection().setY(0).normalize().multiply(.75);
			return isBlockObstructing(playerLocation, direction)
					|| isBlockObstructing(playerLocation, VectorUtil.rotateAroundAxisY(direction.clone(), 45))
					|| isBlockObstructing(playerLocation, VectorUtil.rotateAroundAxisY(direction.clone(), -45));
		}

		private boolean isBlockObstructing(Location location, Vector direction) {
			final Location front = location.clone().add(direction);
			final WorldBorder worldBorder = getPlayer().getWorld().getWorldBorder();
			return checkBlock(front.getBlock()) || checkBlock(front.clone().add(0, 1, 0).getBlock()) || (worldBorder.isInside(location) && !worldBorder.isInside(front));
		}

		private boolean checkBlock(final Block block) {
			return !block.isEmpty() && block.getType().isSolid();
		}
		
	}.setPeriod(TimeUnit.TICKS, 1);
	
	@SubscribeEvent()
	private void onPlayerMove(PlayerMoveEvent e) {
		if (rush.isRunning()) {
			if (LocationUtil.getConflictingEntities(LivingEntity.class, getPlayer(), teamExceptPredicate).size() >= 1) {
				rush.stop(false);
			}
		}
	}
	
	@SubscribeEvent()
	private void onEntityDamage(EntityDamageEvent e) {
		if (e.getEntity().equals(getPlayer()) && rush.isRunning()) {
			e.setCancelled(true);
		}
	}
	
	@SubscribeEvent()
	private void onEntityDamageByBlock(EntityDamageByBlockEvent e) {
		onEntityDamage(e);
	}
	
	@SubscribeEvent()
	private void onEntityDamageByEntity(EntityDamageByEntityEvent e) {
		onEntityDamage(e);
		
    	if (getPlayer().equals(e.getDamager())) {
    		double maxHP = getPlayer().getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue();
    		double multiply = 1 + (((double) (maxHP - getPlayer().getHealth()) / maxHP) * 0.3);
    		e.setDamage(e.getDamage() * multiply);
    		if (multiply >= 1.24) SoundLib.ENTITY_RAVAGER_ATTACK.playSound(getPlayer().getLocation(), 1, 1);
    	}
    	
    	if (e.getEntity().equals(getPlayer())) {
    		if (rush_cooldown.isRunning()) rush_cooldown.setCount((int) (rush_cooldown.getCount() - (rush_cooldown.getCount() * (decrease / 100.0))));
    	}
	}
	
	@SubscribeEvent()
	private void onPlayerInteract(PlayerInteractEvent e) {
		if (e.getPlayer().equals(getPlayer()) && e.getItem() != null) {
			ItemStack mainhand = getPlayer().getInventory().getItemInMainHand();
			if ((e.getItem().getType().toString().endsWith("SWORD") && mainhand.getType().toString().endsWith("SWORD"))) {
				if (e.getAction().equals(Action.RIGHT_CLICK_BLOCK) || e.getAction().equals(Action.RIGHT_CLICK_AIR)) {
					if (!rush_cooldown.isRunning()) {
						if (!rush.isRunning()) {
							skillDamage = getItemDamage(mainhand);
							getPlayer().sendMessage("§4[§c!§4] §c돌진§d 능력을 사용하였습니다.");
							rush.start();
						}
					} else {
						int bysecond = (rush_cooldown.getCount() / 20);
						int minute = bysecond / 60;
						int second = bysecond % 60;
						getPlayer().sendMessage("§c돌진 쿨타임§7: §f" + (minute != 0 ? minute + "분 " : "") + second + "초");
					}
				}
				if (e.getAction().equals(Action.LEFT_CLICK_BLOCK) || e.getAction().equals(Action.LEFT_CLICK_AIR)) {
					if (!sweep_cooldown.isRunning()) {
						new CutParticle(getPlayer(), particleSide, getPlayer().getInventory().getItemInMainHand().getType()).start();
						for (LivingEntity livingEntity : LocationUtil.getNearbyEntities(LivingEntity.class, getPlayer().getLocation(), 4.5, 4.5, predicate)) {
							Vector direction = getPlayer().getLocation().getDirection().setY(0).normalize();
							Vector targetLocation = livingEntity.getLocation().toVector().subtract(getPlayer().getLocation().toVector()).setY(0).normalize();
							double dot = targetLocation.dot(direction);
							if (dot > 0.575D) {
								double damage = getItemDamage(mainhand);
								livingEntity.damage(damage, getPlayer());
							}
						}
						sweep_cooldown.start();
						particleSide *= -1;
					}
				}
			}
		}
	}
	
	private class CutParticle extends AbilityTimer {

		private final Vector axis1;
		private final Vector vector1;
		private final Vectors crescentVectors1;
		
		private final Vector axis2;
		private final Vectors crescentVectors2;
		
		private final Vector axis3;
		private final Vectors crescentVectors3;
		
		private final Entity entity;
		private final Material material;

		private CutParticle(Entity entity, double angle, Material material) {
			super(TaskType.REVERSE, 1);
			setPeriod(TimeUnit.TICKS, 1);
			this.entity = entity;
			this.material = material;
			this.axis1 = VectorUtil.rotateAroundAxis(VectorUtil.rotateAroundAxisY(entity.getLocation().getDirection().setY(0).normalize(), 90), 
					entity.getLocation().getDirection().setY(0).normalize(), angle);
			this.vector1 = entity.getLocation().getDirection().setY(0).normalize().multiply(0.5);
			this.crescentVectors1 = crescent1.clone()
					.rotateAroundAxisY(-entity.getLocation().getYaw())
					.rotateAroundAxis(entity.getLocation().getDirection().setY(0).normalize(), (180 - angle) % 180)
					.rotateAroundAxis(axis1, -15);
			this.axis2 = VectorUtil.rotateAroundAxis(VectorUtil.rotateAroundAxisY(entity.getLocation().getDirection().setY(0).normalize(), 90), 
					entity.getLocation().getDirection().setY(0).normalize(), angle);
			this.crescentVectors2 = crescent2.clone()
					.rotateAroundAxisY(-entity.getLocation().getYaw())
					.rotateAroundAxis(entity.getLocation().getDirection().setY(0).normalize(), (180 - angle) % 180)
					.rotateAroundAxis(axis2, -15);
			this.axis3 = VectorUtil.rotateAroundAxis(VectorUtil.rotateAroundAxisY(entity.getLocation().getDirection().setY(0).normalize(), 90), 
					entity.getLocation().getDirection().setY(0).normalize(), angle);
			this.crescentVectors3 = crescent3.clone()
					.rotateAroundAxisY(-entity.getLocation().getYaw())
					.rotateAroundAxis(entity.getLocation().getDirection().setY(0).normalize(), (180 - angle) % 180)
					.rotateAroundAxis(axis3, -15);
		}
		
		@Override
		protected void run(int count) {
			Material particle1 = null;
			Material particle2 = null;
			Material particle3 = null;
			if (material.toString().contains("WOODEN")) {
				particle1 = Material.OAK_WOOD;
				particle2 = Material.OAK_SAPLING;
				particle3 = Material.OAK_PLANKS;
			} else if (material.toString().contains("STONE")) {
				particle1 = Material.POLISHED_ANDESITE;
				particle2 = Material.POLISHED_GRANITE;
				particle3 = Material.POLISHED_DIORITE;
			} else if (material.toString().contains("GOLD")) {
				particle1 = Material.GOLD_INGOT;
				particle2 = Material.GOLD_BLOCK;
				particle3 = Material.GOLD_ORE;
			} else if (material.toString().contains("IRON")) {
				particle1 = Material.IRON_INGOT;
				particle2 = Material.IRON_BLOCK;
				particle3 = Material.IRON_ORE;
			} else if (material.toString().contains("DIAMOND")) {
				particle1 = Material.DIAMOND;
				particle2 = Material.DIAMOND_BLOCK;
				particle3 = Material.DIAMOND_ORE;
			} else if (material.toString().contains("NETHERITE")) {
				particle1 = Material.NETHERITE_INGOT;
				particle2 = Material.CRYING_OBSIDIAN;
				particle3 = Material.NETHERITE_BLOCK;
			}
			
			Location baseLoc = entity.getLocation().clone().add(vector1).add(0, 0.45, 0);
			for (Location loc : crescentVectors1.toLocations(baseLoc)) {
				ParticleLib.ITEM_CRACK.spawnParticle(loc, 0, 0, 0, 1, 0, ServerVersion.getVersion() >= 13 ? particle1 : MaterialX.valueOf(particle1.toString()).createItem());
			}
			for (Location loc : crescentVectors2.toLocations(baseLoc)) {
				ParticleLib.ITEM_CRACK.spawnParticle(loc, 0, 0, 0, 1, 0, ServerVersion.getVersion() >= 13 ? particle2 : MaterialX.valueOf(particle2.toString()).createItem());
			}
			for (Location loc : crescentVectors3.toLocations(baseLoc)) {
				ParticleLib.ITEM_CRACK.spawnParticle(loc, 0, 0, 0, 1, 0, ServerVersion.getVersion() >= 13 ? particle3 : MaterialX.valueOf(particle3.toString()).createItem());
			}
			SoundLib.ENTITY_PLAYER_ATTACK_SWEEP.playSound(baseLoc, 1f, 0.5f);
		}

	}
	
	public static double getItemDamage(ItemStack itemstack) {
		
		double damage = 0;
		
		if (itemstack.getType().equals(MaterialX.WOODEN_SWORD.getMaterial())) {
			damage += 4;
		} else if (itemstack.getType().equals(MaterialX.STONE_SWORD.getMaterial())) {
			damage += 5;
		} else if (itemstack.getType().equals(MaterialX.GOLDEN_SWORD.getMaterial())) {
			damage += 4;
		} else if (itemstack.getType().equals(MaterialX.IRON_SWORD.getMaterial())) {
			damage += 6;
		} else if (itemstack.getType().equals(MaterialX.DIAMOND_SWORD.getMaterial())) {
			damage += 7;
		} else if (itemstack.getType().equals(MaterialX.NETHERITE_SWORD.getMaterial())) {
			damage += 8;
		}
		
		if (itemstack.getType().equals(MaterialX.WOODEN_AXE.getMaterial()) || itemstack.getType().equals(MaterialX.GOLDEN_AXE.getMaterial())) {
			damage += 7;
		} else if (itemstack.getType().equals(MaterialX.NETHERITE_AXE.getMaterial())) {
			damage += 10;
		} else if (itemstack.getType().toString().endsWith("_AXE")) {
			damage += 9;
		}
		
		if (itemstack.containsEnchantment(Enchantment.DAMAGE_ALL)) {
			int level = itemstack.getEnchantmentLevel(Enchantment.DAMAGE_ALL);
			damage += (0.5 + (level * 0.5));
		}
		
		return damage;
	}
	
}
