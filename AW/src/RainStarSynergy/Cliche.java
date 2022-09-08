package RainStarSynergy;

import java.text.DecimalFormat;
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
import org.bukkit.event.entity.EntityResurrectEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.projectiles.ProjectileSource;
import org.bukkit.util.Vector;

import daybreak.abilitywar.AbilityWar;
import daybreak.abilitywar.ability.AbilityBase;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.SubscribeEvent;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.config.ability.AbilitySettings.SettingObject;
import daybreak.abilitywar.game.GameManager;
import daybreak.abilitywar.game.AbstractGame.CustomEntity;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.AbstractGame.Participant.ActionbarNotification.ActionbarChannel;
import daybreak.abilitywar.game.list.mix.Mix;
import daybreak.abilitywar.game.list.mix.synergy.Synergy;
import daybreak.abilitywar.game.manager.effect.event.ParticipantPreEffectApplyEvent;
import daybreak.abilitywar.game.module.DeathManager;
import daybreak.abilitywar.game.module.Wreck;
import daybreak.abilitywar.game.team.interfaces.Teamable;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.base.math.LocationUtil;
import daybreak.abilitywar.utils.base.math.NumberUtil;
import daybreak.abilitywar.utils.base.math.VectorUtil;
import daybreak.abilitywar.utils.base.minecraft.entity.decorator.Deflectable;
import daybreak.abilitywar.utils.base.minecraft.entity.health.Healths;
import daybreak.abilitywar.utils.base.minecraft.nms.NMS;
import daybreak.abilitywar.utils.library.MaterialX;
import daybreak.abilitywar.utils.library.SoundLib;
import daybreak.abilitywar.utils.library.item.EnchantLib;
import daybreak.google.common.base.Predicate;
import daybreak.google.common.collect.ImmutableSet;

@AbilityManifest(name = "클리셰", rank = Rank.L, species = Species.HUMAN, explain = {
		"§7패시브 §8- §b주인공 버프§f: 체력이 적어 §c위기 상태§f가 될 때 다양한 §b주인공 버프§f를",
		" 받으며, 적을 처치할 때마다 매번 §d성장§f합니다.",
		"§7패시브 §8- §a해치웠나?§f: 치명적 피해를 입을 때, §a불사의 토템 효과§f가 발동합니다.",
		" §a불사의 토템§f은 $[PERIOD]초마다 재충전됩니다.",
		" §a불사의 토템§f이 재충전되기 전까지, §b주인공 버프§f를 얻을 수 없습니다.",
		"§7패시브 §8- §c일당백§f: $[RANGE]칸 내 적의 수에 비례해 해치웠나? 효과가 더 빨리 발동합니다.",
		"§8[§7HIDDEN§8] §e금의환향§f: 이걸로 이야기 끝!"
		})

public class Cliche extends Synergy {

	public Cliche(Participant participant) {
		super(participant);
	}
	
	private int bufflevel = 0;
	private PotionEffect speed = new PotionEffect(PotionEffectType.SPEED, 20, 0, true, false);
	private Set<Projectile> projectiles = new HashSet<>();
	private boolean onetime = false;
	private double firstMaxHealth;
	private double nowMaxHealth = 0;
	private final int period = (int) Math.ceil(Wreck.isEnabled(GameManager.getGame()) ? Wreck.calculateDecreasedAmount(50) * PERIOD.getValue() : PERIOD.getValue());
	private final int range = RANGE.getValue();
	private final int invduration = INV_DURATION.getValue();
	private ActionbarChannel ac = newActionbarChannel();
	private final DecimalFormat df = new DecimalFormat("0.00");
	
	private static final Set<Material> swords;
	
	static {
		if (MaterialX.NETHERITE_SWORD.isSupported()) {
			swords = ImmutableSet.of(MaterialX.WOODEN_SWORD.getMaterial(), Material.STONE_SWORD, Material.IRON_SWORD, MaterialX.GOLDEN_SWORD.getMaterial(), Material.DIAMOND_SWORD, MaterialX.NETHERITE_SWORD.getMaterial());
		} else {
			swords = ImmutableSet.of(MaterialX.WOODEN_SWORD.getMaterial(), Material.STONE_SWORD, Material.IRON_SWORD, MaterialX.GOLDEN_SWORD.getMaterial(), Material.DIAMOND_SWORD);
		}
	}

	public static final SettingObject<Integer> PERIOD = synergySettings.new SettingObject<Integer>(Cliche.class,
			"period", 60, "# 불사의 토템 재충전 시간", "# 쿨타임 감소가 50%까지 적용됩니다.") {
		
		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}
		
	};
	
	public static final SettingObject<Integer> RANGE = synergySettings.new SettingObject<Integer>(Cliche.class,
			"range", 13, "# 일당백 효과 사거리") {
		
		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}
		
	};
	
	public static final SettingObject<Integer> INV_DURATION = synergySettings.new SettingObject<Integer>(Cliche.class,
			"inv-duration", 100, "# 금의환향 무적 지속시간") {
		
		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}
		
	};
	
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
	
	private final Predicate<Entity> notEqualPredicate = new Predicate<Entity>() {
		@Override
		public boolean test(Entity entity) {
			if (entity.equals(getPlayer())) return false;
			return (!(entity instanceof Player)) || (getGame().isParticipating(entity.getUniqueId())
					&& (!(getGame() instanceof DeathManager.Handler) || !((DeathManager.Handler) getGame()).getDeathManager().isExcluded(entity.getUniqueId()))
					&& getGame().getParticipant(entity.getUniqueId()).attributes().TARGETABLE.getValue());
		}

		@Override
		public boolean apply(@Nullable Entity arg0) {
			return false;
		}
	};
	
    private final AbilityTimer invtimer = new AbilityTimer(invduration * 20) {
    	
    	@Override
    	public void onStart() {
    		SoundLib.UI_TOAST_CHALLENGE_COMPLETE.broadcastSound();
    		SoundLib.ENTITY_FIREWORK_ROCKET_LARGE_BLAST.broadcastSound();
    		SoundLib.ENTITY_FIREWORK_ROCKET_TWINKLE.broadcastSound();
    		Bukkit.broadcastMessage("§8[§7HIDDEN§8] §b주인공 §e" + getPlayer().getName() + "§f님에 의해 마왕이 쓰러졌습니다.");
    		Bukkit.broadcastMessage("§8[§7HIDDEN§8] §b" + invduration + "§f초간 주인공이 선제 공격하기 전까지 주인공을 공격하지 못합니다.");
    		getPlayer().sendMessage("§8[§7HIDDEN§8] §e금의환향§f을 달성하였습니다.");
    	}
    	
    	@Override
		public void run(int count) {
    		ac.update("§d무적§7: §f" + df.format(invtimer.getCount() * 0.05) + "초");
    	}
    	
    	@Override
    	public void onEnd() {
    		onSilentEnd();
    	}
    	
    	@Override
    	public void onSilentEnd() {
    		ac.update(null);
    	}
    	
    }.setBehavior(RestrictionBehavior.PAUSE_RESUME).setPeriod(TimeUnit.TICKS, 1).register();
	
    private final AbilityTimer periodtimer = new AbilityTimer(period * 20) {
    	
    	@Override
		public void run(int count) {
    		if (getPoint(range, range) > 1) {
    			setCount((int) (getCount() - Math.min(3, (getPoint(range, range) * 0.5))));
    		}
    	}
    	
    	@Override
    	public void onEnd() {
    		SoundLib.ENTITY_PLAYER_LEVELUP.playSound(getPlayer(), 1, 1.75f);
    		getPlayer().sendMessage("§a[§e!§a] §f위기 탈출 기회가 다시 생겨났습니다.");
    	}
    	
    }.setBehavior(RestrictionBehavior.PAUSE_RESUME).setPeriod(TimeUnit.TICKS, 1).register();
	
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
			if (!periodtimer.isRunning()) {
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
			} else {
    			bufflevel = 0;
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
    
	@SubscribeEvent
	public void onEntityResurrectEvent(EntityResurrectEvent e) {
		if (e.getEntity().equals(getPlayer())) {
			if (!periodtimer.isRunning()) {
				ItemStack leftHand = getPlayer().getInventory().getItemInOffHand();
				e.setCancelled(false);
				getPlayer().getInventory().setItemInOffHand(leftHand);
				periodtimer.start();
			}
		}
	}
    
	@SubscribeEvent(onlyRelevant = true)
	public void onParticipantEffectApply(ParticipantPreEffectApplyEvent e) {
		if (bufflevel >= 1) {
			e.setDuration(TimeUnit.TICKS, (int) (e.getDuration() * 0.5));
		}
	}
	
	@SubscribeEvent
	public void onPlayerDeath(PlayerDeathEvent e) {
		if (e.getEntity().getKiller() != null) {
			if (e.getEntity().getKiller().equals(getPlayer())) {
				Player player = e.getEntity();
				AbilityBase ab = getGame().getParticipant(player).getAbility();
				if (ab.getClass().equals(Mix.class)) {
					Mix mix = (Mix) ab;
					if (mix.hasAbility() && mix.hasSynergy()) {
						if (mix.getSynergy().getClass().equals(DemonLord.class)) invtimer.start();
					}
				}
				double maxHealth = getPlayer().getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue();
				getPlayer().getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(maxHealth + (firstMaxHealth * 0.1));
				nowMaxHealth = getPlayer().getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue();
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
    			e.setAmount(e.getAmount() * 1.2);
    			break;
    		case 4:
    			e.setAmount(e.getAmount() * 1.4);
    			break;
    		case 5:
    			e.setAmount(e.getAmount() * 1.65);
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
    		if (invtimer.isRunning()) e.setCancelled(true);
    		switch(bufflevel) {
    		case 0:
    			if (!periodtimer.isRunning()) {
        			if (getPlayer().getHealth() - e.getFinalDamage() <= 0) {
        				e.setCancelled(true);
        				Healths.setHealth(getPlayer(), getPlayer().getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue() * 0.25);
        			}	
    			}
    			break;
    		case 2:
    		case 3:
    			e.setDamage(e.getDamage() * 0.8);
    			break;
    		case 4:
    			e.setDamage(e.getDamage() * 0.7);
    			break;
    		case 5:
    			e.setDamage(e.getDamage() * 0.65);
    			break;
    		}
    	}
    	if (getPlayer().equals(e.getDamager())) {
    		if (invtimer.isRunning()) invtimer.stop(false);
    		switch(bufflevel) {
    		case 0:
    			e.setDamage(e.getDamage() * 1.05);
    			break;
    		case 2:
    			e.setDamage(e.getDamage() * 1.1);
    			break;
    		case 3:
    			e.setDamage(e.getDamage() * 1.15);
    			break;
    		case 4:
    			e.setDamage(e.getDamage() * 1.2);
    			break;
    		case 5:
    			e.setDamage(e.getDamage() * 1.25);
    			break;
    		}
    	}
    	if (e.getDamager() instanceof Projectile) {
    		Projectile p = (Projectile) e.getDamager();
    		if (getPlayer().equals(p.getShooter()) && invtimer.isRunning()) invtimer.stop(false);
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
				new Bullet(deflectedPlayer, lastLocation, newDirection, sharpnessEnchant, damage).start();
			}

			@Override
			public ProjectileSource getShooter() {
				return shooter;
			}

			@Override
			protected void onRemove() {
				Bullet.this.stop(false);
			}

		}

	}
	
	private float getPoint(int horizontal, int vertical) {
		final Location center = getPlayer().getLocation();
		final double centerX = center.getX(), centerZ = center.getZ();
		float point = 0;
		for (Entity entity : LocationUtil.collectEntities(center, horizontal)) {
			final Location entityLocation = entity.getLocation();
			if (LocationUtil.distanceSquared2D(centerX, centerZ, entityLocation.getX(), entityLocation.getZ()) <= (horizontal * horizontal) && NumberUtil.subtract(center.getY(), entityLocation.getY()) <= vertical && (notEqualPredicate == null || notEqualPredicate.test(entity))) {
				if (entity instanceof Player) {
					point += 1f;
				}
			}
		}
		return point;
	}
	
}