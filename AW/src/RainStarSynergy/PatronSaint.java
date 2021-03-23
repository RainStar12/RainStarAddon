package RainStarSynergy;

import java.util.HashSet;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Set;

import javax.annotation.Nullable;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Damageable;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.projectiles.ProjectileSource;
import org.bukkit.util.Vector;

import daybreak.abilitywar.ability.AbilityBase;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.SubscribeEvent;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.ability.decorator.ActiveHandler;
import daybreak.abilitywar.config.Configuration.Settings;
import daybreak.abilitywar.config.ability.AbilitySettings.SettingObject;
import daybreak.abilitywar.game.GameManager;
import daybreak.abilitywar.game.AbstractGame.CustomEntity;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.AbstractGame.Participant.ActionbarNotification.ActionbarChannel;
import daybreak.abilitywar.game.list.mix.Mix;
import daybreak.abilitywar.game.list.mix.synergy.Synergy;
import daybreak.abilitywar.game.manager.effect.Stun;
import daybreak.abilitywar.game.module.DeathManager;
import daybreak.abilitywar.game.module.Wreck;
import daybreak.abilitywar.game.team.interfaces.Teamable;
import daybreak.abilitywar.utils.base.Formatter;
import daybreak.abilitywar.utils.base.ProgressBar;
import daybreak.abilitywar.utils.base.color.RGB;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.base.math.LocationUtil;
import daybreak.abilitywar.utils.base.minecraft.damage.Damages;
import daybreak.abilitywar.utils.base.minecraft.entity.decorator.Deflectable;
import daybreak.abilitywar.utils.base.minecraft.nms.NMS;
import daybreak.abilitywar.utils.library.MaterialX;
import daybreak.abilitywar.utils.library.ParticleLib;
import daybreak.abilitywar.utils.library.SoundLib;
import daybreak.abilitywar.utils.library.item.EnchantLib;
import daybreak.google.common.base.Predicate;
import daybreak.google.common.collect.ImmutableSet;

@AbilityManifest(name = "수호신", rank = Rank.L, species = Species.GOD, explain = {
		"§7활 우클릭 §8- §c심판§f: 바라보는 방향으로 빛의 화살을 발사합니다.",
		" 빛의 화살은 관통 효과가 있고, 블록에 적중할 때 다시 튕겨 시전자의 위치까지",
		" 되돌아오고 수호 스택에 비례해 적중 대상을 기절시키고 대미지가 증가합니다.",
		"§7패시브 §8- §b사명§f: 살아있는 신 능력자 한 명당 수호 스택을 얻습니다.",
		" 수호 스택으로 얻은 효과는 신 종족에게 영향을 주지 못하며,",
		" 날 피해입힌 대상이 신 종족일 경우 대미지가 20% 경감됩니다.",
		"§7철괴 우클릭 §8- §e통찰§f: $[DurationConfig]초간 튕겨진 화살이 화살로부터 가장 가까운 대상에게로",
		" 유도 효과를 지니게 됩니다. 대상이 피해를 입었다면 대상을 유도 표적에서",
		" 제외시킵니다. $[CooldownConfig]"
		})

public class PatronSaint extends Synergy implements ActiveHandler {

	public PatronSaint(Participant participant) {
		super(participant);
	}
	
	private static final Set<Material> bows;
	private final Set<Player> gods = new HashSet<>();
	private Bullet bullet = null;
	private Bullet2 bullet2 = null;
	private AbilityTimer reload = null;
	private final ActionbarChannel actionbarChannel = newActionbarChannel();
	private final ActionbarChannel ac = newActionbarChannel();
	private static final RGB COLOR = RGB.of(254, 252, 206);
	private static final RGB COLOR2 = RGB.of(206, 237, 244);
	private int stack = 0;

	static {
		if (MaterialX.CROSSBOW.isSupported()) {
			bows = ImmutableSet.of(MaterialX.BOW.getMaterial(), MaterialX.CROSSBOW.getMaterial());
		} else {
			bows = ImmutableSet.of(MaterialX.BOW.getMaterial());
		}
	}
	
	protected void onUpdate(AbilityBase.Update update) {
	    if (update == AbilityBase.Update.RESTRICTION_CLEAR) {
	    	if (!checkgod.isRunning()) checkgod.start();
	    	ac.update("§b수호 스택§f: " + stack);
	    }
	}
	
    private final AbilityTimer checkgod = new AbilityTimer() {
    	
    	@Override
		public void run(int count) {
			if (gods.size() != stack) {
				stack = gods.size();
		    	ac.update("§b수호 스택§f: " + stack);
			}
			for (Participant participants : getGame().getParticipants()) {
				if (participants.hasAbility()) {
					AbilityBase ab = participants.getAbility();
					if (ab.getClass().equals(Mix.class)) {
						Mix mix = (Mix) ab;
						if (mix.hasSynergy()) {
							if (mix.getSynergy().getSpecies().equals(Species.GOD)) {
								gods.add(participants.getPlayer());	
							}
							if (!mix.getSynergy().getSpecies().equals(Species.GOD)) gods.remove(participants.getPlayer());
						} else if (mix.getFirst() != null && mix.getSecond() != null) {
							if (mix.getFirst().getSpecies().equals(Species.GOD) && !mix.getSecond().getSpecies().equals(Species.GOD)) gods.add(participants.getPlayer());
							if (!mix.getFirst().getSpecies().equals(Species.GOD) && mix.getSecond().getSpecies().equals(Species.GOD)) gods.add(participants.getPlayer());
							if (mix.getFirst().getSpecies().equals(Species.GOD) && mix.getSecond().getSpecies().equals(Species.GOD)) gods.add(participants.getPlayer());
							if (!mix.getFirst().getSpecies().equals(Species.GOD) && !mix.getSecond().getSpecies().equals(Species.GOD)) gods.remove(participants.getPlayer());	
						}	
					} else if (ab.getSpecies().equals(Species.GOD)) gods.add(participants.getPlayer());
				} else if (!participants.hasAbility()) gods.remove(participants.getPlayer());
			}
    	}
    
    }.setPeriod(TimeUnit.TICKS, 1).register();
	
	private final Cooldown cool = new Cooldown(CooldownConfig.getValue());
	
	private final Duration insight = new Duration(DurationConfig.getValue() * 20, cool) {

		@Override
		public void onDurationProcess(int arg0) {
			
		}
		
	}.setPeriod(TimeUnit.TICKS, 1);
	
	public static final SettingObject<Integer> CooldownConfig 
	= synergySettings.new SettingObject<Integer>(PatronSaint.class,
			"Cooldown", 60, "# 쿨타임") {
		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}

		@Override
		public String toString() {
			return Formatter.formatCooldown(getValue());
		}
	};
	
	public static final SettingObject<Integer> DurationConfig 
	= synergySettings.new SettingObject<Integer>(PatronSaint.class,
			"Duration", 10, "# 지속 시간") {
		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}
	};
	
	public boolean ActiveSkill(Material material, AbilityBase.ClickType clicktype) {
	    if (material.equals(Material.IRON_INGOT) && clicktype.equals(ClickType.RIGHT_CLICK) &&
	    		!cool.isCooldown() && !insight.isDuration()) {
	    	insight.start();
	    	return true;
	    }
	    return false;
	}
	
	@SubscribeEvent
	public void onEntityDamageByEntity(EntityDamageByEntityEvent e) {
		if (e.getDamager() instanceof Player && e.getEntity().equals(getPlayer())) {
			if (gods.contains((Player) e.getDamager())) e.setDamage(e.getDamage() * 0.8);
		} else if (NMS.isArrow(e.getDamager()) && e.getEntity().equals(getPlayer())) {
			Arrow arrow = (Arrow) e.getDamager();
			if (gods.contains((Player) arrow.getShooter())) e.setDamage(e.getDamage() * 0.8);
		}
	}
	
	@SuppressWarnings("deprecation")
	@SubscribeEvent(onlyRelevant = true)
	private void onPlayerInteract(PlayerInteractEvent e) {
		if ((e.getAction() == Action.RIGHT_CLICK_BLOCK || e.getAction() == Action.RIGHT_CLICK_AIR) && e.getItem() != null && bows.contains(e.getItem().getType())) {
			getPlayer().updateInventory();
			if (bullet == null && bullet2 == null) {
				if (reload == null) {
					if (bows.contains(getPlayer().getInventory().getItemInMainHand().getType())) {
						final ItemStack mainhand = getPlayer().getInventory().getItemInMainHand();
						new Bullet(getPlayer(), getPlayer().getLocation().clone().add(0, 1.5, 0), getPlayer().getLocation().getDirection(), mainhand.getEnchantmentLevel(Enchantment.ARROW_DAMAGE), 10, COLOR).start();
					} else if (bows.contains(getPlayer().getInventory().getItemInOffHand().getType())) {
						final ItemStack offhand = getPlayer().getInventory().getItemInOffHand();
						new Bullet(getPlayer(), getPlayer().getLocation().clone().add(0, 1.5, 0), getPlayer().getLocation().getDirection(), offhand.getEnchantmentLevel(Enchantment.ARROW_DAMAGE), 10, COLOR).start();
					}
				} else {
					getPlayer().sendMessage("§b재장전 §f중입니다.");
				}
			} else {
				getPlayer().sendMessage("§e빛의 화살§f이 회수되지 않았습니다.");
			}
		}
	}
	
	@SubscribeEvent(onlyRelevant = true)
	private void onEntityShootBow(EntityShootBowEvent e) {
		e.setCancelled(true);
	}
	
	public class Bullet extends AbilityTimer {
		
		private final LivingEntity shooter;
		private final CustomEntity entity;
		private final Vector forward;
		private final int powerEnchant;
		private final double damage;
		private final Predicate<Entity> predicate;
		private boolean checkhit = false;
		private final Set<Damageable> hitcheck = new HashSet<>();

		private final RGB color;
		private Location lastLocation;
		
		private Bullet(LivingEntity shooter, Location startLocation, Vector arrowVelocity, int powerEnchant, double damage, RGB color) {
			super(60);
			setPeriod(TimeUnit.TICKS, 1);
			PatronSaint.this.bullet = this;
			this.shooter = shooter;
			this.entity = new Bullet.ArrowEntity(startLocation.getWorld(), startLocation.getX(), startLocation.getY(), startLocation.getZ()).resizeBoundingBox(-.75, -.75, -.75, .75, .75, .75);
			this.forward = arrowVelocity.multiply(2.75);
			this.powerEnchant = powerEnchant;
			this.damage = damage;
			this.color = color;
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
				final Block block = location.getBlock();
				final Material type = block.getType();
				if (type.isSolid()) {
					SoundLib.ENTITY_ARROW_HIT_PLAYER.playSound(getPlayer());
					checkhit = true;
					stop(false);
					return;
				}
				for (LivingEntity livingEntity : LocationUtil.getConflictingEntities(LivingEntity.class, entity.getWorld(), entity.getBoundingBox(), predicate)) {
					if (!shooter.equals(livingEntity)) {
						if (!hitcheck.contains(livingEntity)) {
							if (livingEntity instanceof Player) {
								if (!gods.contains((Player) livingEntity)) Damages.damageArrow(livingEntity, shooter, (float) ((EnchantLib.getDamageWithPowerEnchantment(damage, powerEnchant) + Math.min(5, stack * 0.5)) * 0.55));	
								else Damages.damageArrow(livingEntity, shooter, (float) ((EnchantLib.getDamageWithPowerEnchantment(damage, powerEnchant)) * 0.55));
							} else {
								Damages.damageArrow(livingEntity, shooter, (float) ((EnchantLib.getDamageWithPowerEnchantment(damage, powerEnchant) + Math.min(5, stack * 0.5)) * 0.55));	
							}
							if (livingEntity instanceof Player) {
								if (!gods.contains((Player) livingEntity)) Stun.apply(getGame().getParticipant((Player) livingEntity), TimeUnit.TICKS, Math.min(10 + (stack * 5), 60));
								else Stun.apply(getGame().getParticipant((Player) livingEntity), TimeUnit.TICKS, 10);
							}
							hitcheck.add(livingEntity);
							checkhit = true;
						}
					}
				}
				ParticleLib.REDSTONE.spawnParticle(location, color);
			}
			lastLocation = newLocation;
		}
		
		@Override
		protected void onEnd() {
			if (checkhit) {
				new Bullet2(getPlayer(), entity.getLocation(), (float) (EnchantLib.getDamageWithPowerEnchantment(damage, powerEnchant)), COLOR2).start();
				checkhit = false;
			}
			hitcheck.clear();
			entity.remove();
			PatronSaint.this.bullet = null;
		}

		@Override
		protected void onSilentEnd() {
			hitcheck.clear();
			entity.remove();
			PatronSaint.this.bullet = null;
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
				new Bullet(deflectedPlayer, lastLocation, newDirection, powerEnchant, damage, color).start();
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
	
	public class Bullet2 extends AbilityTimer {
		
		private final LivingEntity shooter;
		private final CustomEntity entity;
		private final double damage;
		private Vector velocity;
		private final Predicate<Entity> predicate;
		private final Set<Damageable> hitcheck = new HashSet<>();

		private final RGB color;
		private Location lastLocation;
		
		private Bullet2(LivingEntity shooter, Location startLocation, double damage, RGB color) {
			super(1200);
			setPeriod(TimeUnit.TICKS, 1);
			PatronSaint.this.bullet2 = this;
			this.shooter = shooter;
			this.entity = new Bullet2.ArrowEntity(startLocation.getWorld(), startLocation.getX(), startLocation.getY(), startLocation.getZ()).resizeBoundingBox(-.75, -.75, -.75, .75, .75, .75);
			this.velocity = getPlayer().getLocation().add(0, 1, 0).clone().subtract(startLocation.clone()).toVector().normalize().multiply(0.8);
			this.damage = damage;
			this.color = color;
			this.lastLocation = startLocation;
			this.predicate = new Predicate<Entity>() {
				@Override
				public boolean test(Entity entity) {
					if (entity instanceof ArmorStand) return false;
					if (entity instanceof Player) {
						if (entity.equals(getPlayer())) return true;
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
					if (hitcheck.contains(entity)) return false;
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
			if (insight.isRunning()) {
				Damageable nearest = LocationUtil.getNearestEntity(Damageable.class, entity.getLocation(), predicate);
				if (nearest != null) {
					this.velocity = nearest.getLocation().add(0, 1, 0).clone().subtract(lastLocation.clone()).toVector().normalize().multiply(0.8);		
				} else {
					this.velocity = getPlayer().getLocation().add(0, 1, 0).clone().subtract(lastLocation.clone()).toVector().normalize().multiply(0.8);	
				}
			} else {
				this.velocity = getPlayer().getLocation().add(0, 1, 0).clone().subtract(lastLocation.clone()).toVector().normalize().multiply(0.8);	
			}
			final Location newLocation = lastLocation.clone().add(velocity);
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
				for (LivingEntity livingEntity : LocationUtil.getConflictingEntities(LivingEntity.class, entity.getWorld(), entity.getBoundingBox(), predicate)) {
					if (!shooter.equals(livingEntity)) {
						if (!hitcheck.contains(livingEntity)) {
							if (livingEntity instanceof Player) {
								if (!gods.contains((Player) livingEntity)) Damages.damageArrow(livingEntity, shooter, (float) ((damage + Math.min(5, stack * 0.5)) * 1.1));	
								else Damages.damageArrow(livingEntity, shooter, (float) (damage * 1.1));
							} else {
								Damages.damageArrow(livingEntity, shooter, (float) ((damage + Math.min(5, stack * 0.5)) * 1.1));	
							}
							if (livingEntity instanceof Player) {
								if (!gods.contains((Player) livingEntity)) Stun.apply(getGame().getParticipant((Player) livingEntity), TimeUnit.TICKS, Math.min(5 + (stack * 2), 60));
								else Stun.apply(getGame().getParticipant((Player) livingEntity), TimeUnit.TICKS, 5);
							}
							hitcheck.add(livingEntity);	
						}
					} else if (livingEntity.equals(shooter)) {
						stop(false);
						return;
					}
				}
				ParticleLib.REDSTONE.spawnParticle(location, color);
			}
			lastLocation = newLocation;
		}
		
		@Override
		protected void onEnd() {
			final int reloadCount = Wreck.isEnabled(GameManager.getGame()) ? (int) (Math.max(((100 - Settings.getCooldownDecrease().getPercentage()) / 100.0), 0.85) * 20) : 20;
			reload = new AbilityTimer(reloadCount) {
				private final ProgressBar progressBar = new ProgressBar(reloadCount, 15);

				@Override
				protected void run(int count) {
					progressBar.step();
					actionbarChannel.update("재장전: " + progressBar.toString());
				}

				@Override
				protected void onEnd() {
					PatronSaint.this.reload = null;
					actionbarChannel.update(null);
					SoundLib.BLOCK_STONE_PRESSURE_PLATE_CLICK_ON.playSound(getPlayer());
				}
			}.setBehavior(RestrictionBehavior.PAUSE_RESUME).setPeriod(TimeUnit.TICKS, 2);
			reload.start();
			entity.remove();
			PatronSaint.this.bullet2 = null;
		}

		@Override
		protected void onSilentEnd() {
			hitcheck.clear();
			entity.remove();
			PatronSaint.this.bullet2 = null;
		}

		public class ArrowEntity extends CustomEntity {

			public ArrowEntity(World world, double x, double y, double z) {
				getGame().super(world, x, y, z);
			}

			public ProjectileSource getShooter() {
				return shooter;
			}

			@Override
			protected void onRemove() {
			}

		}
		
	}
	
}
