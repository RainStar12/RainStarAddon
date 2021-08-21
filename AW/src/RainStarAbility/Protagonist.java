package RainStarAbility;

import java.util.HashSet;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Random;
import java.util.Set;

import javax.annotation.Nullable;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.block.Block;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import daybreak.abilitywar.AbilityWar;
import daybreak.abilitywar.ability.AbilityBase;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.SubscribeEvent;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.game.AbstractGame.CustomEntity;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.list.mix.Mix;
import daybreak.abilitywar.game.manager.effect.event.ParticipantEffectApplyEvent;
import daybreak.abilitywar.game.module.DeathManager;
import daybreak.abilitywar.game.team.interfaces.Teamable;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.base.math.LocationUtil;
import daybreak.abilitywar.utils.base.math.VectorUtil;
import daybreak.abilitywar.utils.base.minecraft.entity.health.Healths;
import daybreak.abilitywar.utils.base.minecraft.nms.NMS;
import daybreak.abilitywar.utils.library.MaterialX;
import daybreak.abilitywar.utils.library.SoundLib;
import daybreak.abilitywar.utils.library.item.EnchantLib;
import daybreak.google.common.base.Predicate;
import daybreak.google.common.collect.ImmutableSet;

@AbilityManifest(name = "주인공", rank = Rank.S, species = Species.HUMAN, explain = {
		"당신은 이 능력자 전쟁의 §e주인공§f입니다.",
		"체력이 적어 §c위기 상태§f가 될 때 다양한 §a주인공 버프§f를 받습니다.",
		"또한 적을 처치할 때마다 매번 §d성장§f합니다.",
		"§8[§7HIDDEN§8] §c이야기 쟁탈§f: 자, 이제 누가 주인공이지?"
		})

public class Protagonist extends AbilityBase {
	
	public Protagonist(Participant participant) {
		super(participant);
	}
	
	private int bufflevel = 0;
	private PotionEffect speed = new PotionEffect(PotionEffectType.SPEED, 20, 0, true, false);
	private Set<Projectile> projectiles = new HashSet<>();
	private boolean onetime = false;
	private double firstMaxHealth;
	private double nowMaxHealth = 0;
	
	private static final Set<Material> swords;
	
	static {
		if (MaterialX.NETHERITE_SWORD.isSupported()) {
			swords = ImmutableSet.of(MaterialX.WOODEN_SWORD.getMaterial(), Material.STONE_SWORD, Material.IRON_SWORD, MaterialX.GOLDEN_SWORD.getMaterial(), Material.DIAMOND_SWORD, MaterialX.NETHERITE_SWORD.getMaterial());
		} else {
			swords = ImmutableSet.of(MaterialX.WOODEN_SWORD.getMaterial(), Material.STONE_SWORD, Material.IRON_SWORD, MaterialX.GOLDEN_SWORD.getMaterial(), Material.DIAMOND_SWORD);
		}
	}

	@Override
	protected void onUpdate(Update update) {
		if (update == Update.RESTRICTION_CLEAR) {
			buffchecker.start();
			if (!onetime) {
				firstMaxHealth = getPlayer().getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue();
				onetime = true;
			}
			if (nowMaxHealth != 0) {
				getPlayer().getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(nowMaxHealth);	
			}
		}
		if (update == Update.RESTRICTION_SET || update == Update.ABILITY_DESTROY) {
			if (firstMaxHealth >= 1) getPlayer().getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(firstMaxHealth);	
		}
	}
	
	private final Predicate<Entity> predicate = new Predicate<Entity>() {
		@Override
		public boolean test(Entity entity) {
			if (entity.equals(getPlayer())) return false;
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
	
    private final AbilityTimer attackcool = new AbilityTimer(40) {
    	
    	@Override
		public void run(int count) {
    	}
    	
    }.setPeriod(TimeUnit.TICKS, 1).register();
	
    private final AbilityTimer buffchecker = new AbilityTimer() {
    	
    	@Override
		public void run(int count) {
    		double nowHealth = getPlayer().getHealth();
    		double maxHealth = getPlayer().getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue();
    		if (nowHealth > maxHealth * 0.6) {
    			bufflevel = 0;
    		} else if (nowHealth <= maxHealth * 0.6 && nowHealth > maxHealth * 0.5) {
    			bufflevel = 1;
    		} else if (nowHealth <= maxHealth * 0.5 && nowHealth > maxHealth * 0.4) {
    			bufflevel = 2;
    		} else if (nowHealth <= maxHealth * 0.4 && nowHealth > maxHealth * 0.3) {
    			bufflevel = 3;
    		} else if (nowHealth <= maxHealth * 0.3 && nowHealth > maxHealth * 0.2) {
    			bufflevel = 4;
    		} else if (nowHealth <= maxHealth * 0.2) {
    			bufflevel = 5;
    		}
    		if (bufflevel >= 1) {
    			for (Projectile projectile : LocationUtil.getNearbyEntities(Projectile.class, getPlayer().getLocation(), 7, 7, predicate)) {
    				if (!getPlayer().equals(projectile.getShooter()) && !projectiles.contains(projectile)) {
    					Random random = new Random();
    					if (bufflevel <= 4) {
        					if (random.nextInt(10) <= 2) {
            					if (random.nextBoolean() == true) {
            						projectile.setVelocity(VectorUtil.rotateAroundAxisY(projectile.getVelocity(), 20));	
            					} else {
            						projectile.setVelocity(VectorUtil.rotateAroundAxisY(projectile.getVelocity(), -20));
            					}	
        					}	
    					} else if (bufflevel == 5) {
    						if (random.nextBoolean() == true) {
        						projectile.setVelocity(VectorUtil.rotateAroundAxisY(projectile.getVelocity(), 20));	
        					} else {
        						projectile.setVelocity(VectorUtil.rotateAroundAxisY(projectile.getVelocity(), -20));
        					}
    					}
    					projectiles.add(projectile);
    				}
    			}
    		}
    		if (bufflevel >= 3) {
    			getPlayer().addPotionEffect(speed);
    		}
    	}
    	
    }.setPeriod(TimeUnit.TICKS, 1).register();
    
	@SubscribeEvent(onlyRelevant = true)
	public void onParticipantEffectApply(ParticipantEffectApplyEvent e) {
		if (bufflevel >= 3) {
			e.setDuration(TimeUnit.TICKS, (int) (e.getDuration() * 0.5));
		}
	}
	
	@SubscribeEvent
	public void onPlayerDeath(PlayerDeathEvent e) {
		if (e.getEntity().getKiller() != null) {
			if (e.getEntity().getKiller().equals(getPlayer())) {
				Player player = e.getEntity();
				double maxHealth = getPlayer().getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue();
				getPlayer().getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(maxHealth + (firstMaxHealth * 0.1));
				nowMaxHealth = getPlayer().getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue();
				AbilityBase ab = getGame().getParticipant(player).getAbility();
				if (bufflevel >= 3) {
					if (ab.getClass().equals(Mix.class)) {
						Mix mix = (Mix) ab;
						if (mix.hasAbility() && !mix.hasSynergy()) {
							if (mix.getFirst().getClass().equals(Protagonist.class) || mix.getSecond().getClass().equals(Protagonist.class)) {
				    			getPlayer().sendMessage("§8[§7HIDDEN§8] §f당신과는 다른 이야기의 주인공을 만나 §a주인공 버프§f를 받고 승리하였습니다.");
				    			getPlayer().sendMessage("§8[§7HIDDEN§8] §c이야기 쟁탈§f을 달성하였습니다.");
				    			SoundLib.UI_TOAST_CHALLENGE_COMPLETE.playSound(getPlayer());
				    			double targetMaxHealth = e.getEntity().getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue();
				    			getPlayer().getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(maxHealth + ((targetMaxHealth + (firstMaxHealth * 0.1)) - firstMaxHealth));
							}
						}
					} else if (ab.getClass().equals(Protagonist.class)) {
						getPlayer().sendMessage("§8[§7HIDDEN§8] §f당신과는 다른 이야기의 주인공을 만나 §a주인공 버프§f를 받고 승리하였습니다.");
		    			getPlayer().sendMessage("§8[§7HIDDEN§8] §c이야기 쟁탈§f을 달성하였습니다.");
		    			SoundLib.UI_TOAST_CHALLENGE_COMPLETE.playSound(getPlayer());
		    			double targetMaxHealth = e.getEntity().getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue();
		    			getPlayer().getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(maxHealth + ((targetMaxHealth + (firstMaxHealth * 0.1)) - firstMaxHealth));
					}	
				}
			}	
		}
	}
    
	@SubscribeEvent(onlyRelevant = true)
	private void onPlayerInteract(final PlayerInteractEvent e) {
		if (bufflevel >= 4) {
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
    public void onEntityRegainHealth(EntityRegainHealthEvent e) {
    	if (e.getEntity().equals(getPlayer())) {
    		switch(bufflevel) {
    		case 2:
    		case 3:
    			e.setAmount(e.getAmount() * 1.15);
    			break;
    		case 4:
    			e.setAmount(e.getAmount() * 1.35);
    			break;
    		case 5:
    			e.setAmount(e.getAmount() * 1.5);
    			break;
    		}
    	}
    }
    
    @SubscribeEvent
    public void onProjectileHit(ProjectileHitEvent e) {
    	if (projectiles.contains(e.getEntity())) projectiles.remove(e.getEntity());
    }
    
    @SubscribeEvent
    public void onEntityDamageByEntity(EntityDamageByEntityEvent e) {
    	if (getPlayer().equals(e.getEntity())) {
    		switch(bufflevel) {
    		case 0:
    			if (getPlayer().getHealth() - e.getFinalDamage() <= 0) {
    				e.setCancelled(true);
    				Healths.setHealth(getPlayer(), getPlayer().getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue() * 0.25);
    			}
    			break;
    		case 2:
    		case 3:
    			e.setDamage(e.getDamage() * 0.85);
    			break;
    		case 4:
    			e.setDamage(e.getDamage() * 0.75);
    			break;
    		case 5:
    			e.setDamage(e.getDamage() * 0.7);
    			break;
    		}
    	}
    	if (getPlayer().equals(e.getDamager())) {
    		switch(bufflevel) {
    		case 3:
    			e.setDamage(e.getDamage() * 1.05);
    			break;
    		case 4:
    			e.setDamage(e.getDamage() * 1.1);
    			break;
    		case 5:
    			e.setDamage(e.getDamage() * 1.15);
    			break;
    		}
    	}
    }
    
	@SubscribeEvent
	public void onProjectileLaunch(ProjectileLaunchEvent e) {
		if (getPlayer().equals(e.getEntity().getShooter()) && NMS.isArrow(e.getEntity())) {
			if (bufflevel >= 4) {
				new Homing((Arrow) e.getEntity(), e.getEntity().getVelocity().length()).start();
			}
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
				arrow.setCritical(true);
				Player p = LocationUtil.getNearestEntity(Player.class, arrow.getLocation(), predicate);
				if (p != null) {
					if (arrow.getLocation().distanceSquared(p.getEyeLocation()) <= (1.75 * 1.75)) {
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
				stop(false);
			}
		}
		
		@Override
		protected void onEnd() {
			arrow.setGravity(true);
			arrow.setGlowing(false);
		}
		
		@Override
		protected void onSilentEnd() {
			onEnd();
		}
	}
	
	public class Bullet extends AbilityTimer {

		private final LivingEntity shooter;
		private final CustomEntity entity;
		private final Vector forward;
		private final int sharpnessEnchant;
		private final double damage;
		private final Predicate<Entity> predicate;

		private Location lastLocation;

		private Bullet(LivingEntity shooter, Location startLocation, Vector arrowVelocity, int sharpnessEnchant, double damage) {
			super(1);
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
				final Block block = location.getBlock();
				final Material type = block.getType();
				if (type.isSolid()) {
					stop(true);
					return;
				}
				for (LivingEntity livingEntity : LocationUtil.getConflictingEntities(LivingEntity.class, entity.getWorld(), entity.getBoundingBox(), predicate)) {
					if (!shooter.equals(livingEntity)) {
						livingEntity.damage((float) (EnchantLib.getDamageWithSharpnessEnchantment(damage, sharpnessEnchant)), getPlayer());
					}
				}
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
	
}
