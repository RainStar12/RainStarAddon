package rainstar.abilitywar.synergy;

import static rainstar.abilitywar.system.config.RainStarAbilitySetting.rsSynergySettings;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;

import javax.annotation.Nullable;

import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.FireworkEffect.Type;
import org.bukkit.attribute.Attribute;
import org.bukkit.block.Block;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Damageable;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Firework;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByBlockEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.projectiles.ProjectileSource;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import daybreak.abilitywar.AbilityWar;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.SubscribeEvent;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.ability.decorator.ActiveHandler;
import daybreak.abilitywar.config.ability.AbilitySettings.SettingObject;
import daybreak.abilitywar.game.GameManager;
import daybreak.abilitywar.game.AbstractGame.CustomEntity;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.AbstractGame.Participant.ActionbarNotification.ActionbarChannel;
import daybreak.abilitywar.game.list.mix.synergy.Synergy;
import daybreak.abilitywar.game.manager.effect.Stun;
import daybreak.abilitywar.game.module.DeathManager;
import daybreak.abilitywar.game.module.Wreck;
import daybreak.abilitywar.game.team.interfaces.Teamable;
import daybreak.abilitywar.utils.base.Formatter;
import daybreak.abilitywar.utils.base.color.RGB;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.base.math.LocationUtil;
import daybreak.abilitywar.utils.base.math.geometry.Circle;
import daybreak.abilitywar.utils.base.minecraft.damage.Damages;
import daybreak.abilitywar.utils.base.minecraft.entity.decorator.Deflectable;
import daybreak.abilitywar.utils.base.random.Random;
import daybreak.abilitywar.utils.library.MaterialX;
import daybreak.abilitywar.utils.library.ParticleLib;
import daybreak.abilitywar.utils.library.SoundLib;
import daybreak.google.common.base.Predicate;
import daybreak.google.common.collect.ImmutableSet;


@AbilityManifest(name = "유성우", rank = Rank.SPECIAL, species = Species.SPECIAL, explain = { 
		"§7기본 공격 §8- §3낙성§f: $[PERIOD]초마다 검을 휘두르면 하늘에서 §b별똥별§f이 떨어집니다.",
		" 바라보는 방향의 5칸 앞이 낙하 지점이 되며, 떨어지기 전 가장 가까운",
		" 생명체에게 살짝 유도됩니다.",
		"§7특수 투사체 §8- §b별똥별§f: 땅에 착지하면 가벼운 폭발을 일으키고,",
		" 피해량은 무기 피해량의 $[PERCENTAGE]%입니다.",
		" 적중 시마다 피해량이 $[INCREASE]%씩 증가하고, 땅에 착지하면 초기화됩니다.",
		"§7철괴 우클릭 §8- §a유성우§f: 게임에 존재하는 모든 대상의 머리 위에,",
		" §b별똥별§f을 떨어뜨립니다. 이 §b별똥별§f은 가장 마지막으로 §b별똥별§f이 가한",
		" 피해량을 다시 입히고, 대상을 $[STUN]초간 §e§n기절§f시킵니다. $[COOLDOWN]"
		})
public class MeteorShower extends Synergy implements ActiveHandler {
	
	public MeteorShower(Participant participant) {
		super(participant);
	}
	
	public static final SettingObject<Double> PERIOD =
			rsSynergySettings.new SettingObject<Double>(MeteorShower.class, "period", 5.0,
			"# 별똥별 소환 쿨타임", "# 단위: 초") {
		@Override
		public boolean condition(Double value) {
			return value >= 0;
		}
	};
	
	public static final SettingObject<Integer> PERCENTAGE =
			rsSynergySettings.new SettingObject<Integer>(MeteorShower.class, "damage-percentage", 127,
			"# 별똥별의 무기 비례 피해량", "# 단위: %") {
		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}
	};

	public static final SettingObject<Integer> INCREASE =
			rsSynergySettings.new SettingObject<Integer>(MeteorShower.class, "damage-increase", 12,
			"# 피해 증가량", "# 단위: %") {
		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}
	};

	public static final SettingObject<Double> STUN =
			rsSynergySettings.new SettingObject<Double>(MeteorShower.class, "stun", 4.0,
			"# 기절 시간", "# 단위: 초") {
		@Override
		public boolean condition(Double value) {
			return value >= 0;
		}
	};

	public static final SettingObject<Integer> COOLDOWN =
			rsSynergySettings.new SettingObject<Integer>(MeteorShower.class, "cooldown", 80,
			"# 쿨타임") {
		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}

		@Override
		public String toString() {
			return Formatter.formatCooldown(getValue());
		}
	};
	
	public enum GradationType {

	    TYPE_1(Arrays.asList(
	            RGB.of(1, 1, 1),
	            RGB.of(1, 2, 17),
	            RGB.of(2, 4, 34),
	            RGB.of(3, 6, 52),
	            RGB.of(4, 8, 70),
	            RGB.of(5, 10, 87),
	            RGB.of(7, 13, 106),
	            RGB.of(8, 15, 123),
	            RGB.of(10, 18, 140),
	            RGB.of(12, 20, 150),
	            RGB.of(14, 22, 159),
	            RGB.of(16, 23, 168),
	            RGB.of(19, 25, 179),
	            RGB.of(22, 27, 189),
	            RGB.of(24, 28, 199),
	            RGB.of(50, 21, 203),
	            RGB.of(74, 15, 207),
	            RGB.of(112, 5, 212),
	            RGB.of(122, 25, 188),
	            RGB.of(131, 44, 166),
	            RGB.of(140, 61, 145)
	    )),

	    TYPE_2(Arrays.asList(
	            RGB.of(3, 212, 168),
	            RGB.of(8, 212, 178),
	            RGB.of(15, 213, 190),
	            RGB.of(18, 211, 198),
	            RGB.of(27, 214, 213),
	            RGB.of(29, 210, 220),
	            RGB.of(30, 207, 225),
	            RGB.of(24, 196, 223),
	            RGB.of(23, 191, 226),
	            RGB.of(19, 182, 226),
	            RGB.of(16, 174, 227),
	            RGB.of(13, 166, 228),
	            RGB.of(10, 159, 228),
	            RGB.of(7, 151, 229),
	            RGB.of(3, 143, 229),
	            RGB.of(1, 135, 230),
	            RGB.of(1, 126, 222),
	            RGB.of(1, 118, 214),
	            RGB.of(1, 109, 207),
	            RGB.of(1, 101, 199),
	            RGB.of(1, 92, 191)
	    )),

	    TYPE_3(Arrays.asList(
	    		RGB.of(254, 220, 1),
	    		RGB.of(245, 228, 1),
	    		RGB.of(235, 236, 1),
	    		RGB.of(220, 245, 1),
	    		RGB.of(200, 254, 1),
	    		RGB.of(175, 254, 1),
	    		RGB.of(150, 254, 1),
	    		RGB.of(120, 254, 1),
	    		RGB.of(90, 254, 1),
	    		RGB.of(60, 254, 1),
	    		RGB.of(30, 254, 1),
	    		RGB.of(1, 254, 20),
	    		RGB.of(1, 254, 50),
	    		RGB.of(1, 254, 80),
	    		RGB.of(1, 254, 110),
	    		RGB.of(1, 254, 140),
	    		RGB.of(1, 254, 165),
	    		RGB.of(1, 254, 185),
	    		RGB.of(1, 245, 190),
	    		RGB.of(1, 235, 195),
	    		RGB.of(1, 225, 200)
	    )),

	    TYPE_4(Arrays.asList(
	    		RGB.of(254, 1, 1),
	    		RGB.of(254, 20, 1),
	    		RGB.of(254, 40, 1),
	    		RGB.of(254, 60, 1),
	    		RGB.of(254, 80, 1),
	    		RGB.of(254, 100, 1),
	    		RGB.of(254, 120, 1),
	    		RGB.of(254, 140, 1),
	    		RGB.of(254, 160, 1),
	    		RGB.of(254, 180, 1),
	    		RGB.of(254, 200, 1),
	    		RGB.of(254, 220, 1),
	    		RGB.of(254, 240, 1),
	    		RGB.of(240, 220, 1),
	    		RGB.of(220, 190, 1),
	    		RGB.of(180, 150, 1),
	    		RGB.of(140, 110, 1),
	    		RGB.of(100, 75, 1),
	    		RGB.of(65, 45, 1),
	    		RGB.of(30, 20, 1),
	    		RGB.of(1, 1, 1)
	    )),

	    TYPE_5(Arrays.asList(
	    		RGB.of(254, 254, 254),
	    		RGB.of(240, 240, 240),
	    		RGB.of(225, 225, 225),
	    		RGB.of(210, 210, 210),
	    		RGB.of(195, 195, 195),
	    		RGB.of(180, 180, 180),
	    		RGB.of(165, 165, 165),
	    		RGB.of(150, 150, 150),
	    		RGB.of(135, 135, 135),
	    		RGB.of(120, 120, 120),
	    		RGB.of(105, 105, 105),
	    		RGB.of(90, 90, 90),
	    		RGB.of(75, 75, 75),
	    		RGB.of(60, 60, 60),
	    		RGB.of(45, 45, 45),
	    		RGB.of(30, 30, 30),
	    		RGB.of(15, 15, 15),
	    		RGB.of(5, 10, 25),
	    		RGB.of(4, 15, 45),
	    		RGB.of(3, 20, 70),
	    		RGB.of(2, 25, 100)
	    ));

	    private final List<RGB> colors;

	    GradationType(List<RGB> colors) {
	        this.colors = colors;
	    }

	    public List<RGB> getColors() {
	        return colors;
	    }
	}

	private final double fallingStarDefaultDamage = PERCENTAGE.getValue() * 0.01;
	private final double damageIncrease = INCREASE.getValue() * 0.01;
	private final int stunDuration = (int) (STUN.getValue() * 20);
	private final Cooldown cooldown = new Cooldown(COOLDOWN.getValue());
	private int meteorCool = (int) Math.ceil(Wreck.isEnabled(GameManager.getGame()) ? Wreck.calculateDecreasedAmount(75) * PERIOD.getValue() * 20 : PERIOD.getValue() * 20);

	private int stack = 0;
	private static final Set<Material> swords;
	private Random random = new Random();
	private ActionbarChannel ac = newActionbarChannel();
	private DecimalFormat df = new DecimalFormat("0.0");
	private Circle circle = Circle.of(0.5, 10);
	private static final RGB color = RGB.of(179, 245, 205);
	private static final FixedMetadataValue NULL_VALUE = new FixedMetadataValue(AbilityWar.getPlugin(), null);
	
	static {
		if (MaterialX.NETHERITE_SWORD.isSupported()) {
			swords = ImmutableSet.of(MaterialX.WOODEN_SWORD.getMaterial(), Material.STONE_SWORD, Material.IRON_SWORD, MaterialX.GOLDEN_SWORD.getMaterial(), Material.DIAMOND_SWORD, MaterialX.NETHERITE_SWORD.getMaterial());
		} else {
			swords = ImmutableSet.of(MaterialX.WOODEN_SWORD.getMaterial(), Material.STONE_SWORD, Material.IRON_SWORD, MaterialX.GOLDEN_SWORD.getMaterial(), Material.DIAMOND_SWORD);
		}
	}
	
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
	
	public double getDamage() {
		double damage = getPlayer().getAttribute(Attribute.GENERIC_ATTACK_DAMAGE).getValue();
		if (getPlayer().getInventory().getItemInMainHand() != null && getPlayer().getInventory().getItemInMainHand().containsEnchantment(Enchantment.DAMAGE_ALL)) damage += ((getPlayer().getInventory().getItemInMainHand().getEnchantmentLevel(Enchantment.DAMAGE_ALL) + 1) * 0.5);
		return damage *= fallingStarDefaultDamage * (1 + (damageIncrease * stack));
	}
	
	public Location getPoint(Player player) {
		return LocationUtil.floorY(player.getEyeLocation().add(player.getLocation().getDirection().multiply(5)));
	}
	
	public void meteor(boolean stun, boolean located, Player target) {
		SoundLib.BLOCK_ENCHANTMENT_TABLE_USE.playSound(getPlayer().getLocation(), 1, 0.5f);
		if (located) new Bullet(getPlayer(), target.getLocation().clone().add(random.nextInt(1000) * 0.01, target.getLocation().clone().getY() + random.nextInt(10) + 10, random.nextInt(1000) * 0.01), target.getLocation(), getDamage(), stun).start();
		else new Bullet(getPlayer(), getPoint(getPlayer()).clone().add(random.nextInt(2500) * 0.01, random.nextInt(51) + 50, random.nextInt(2500) * 0.01), getPoint(getPlayer()), getDamage(), stun).start();
	}
	
	@Override
	public void onUpdate(Update update) {
		if (update == Update.RESTRICTION_CLEAR) {
			passive.start();
		}
	}
	
	@SubscribeEvent(onlyRelevant = true)
	private void onPlayerInteract(final PlayerInteractEvent e) {
		if (e.getItem() != null && swords.contains(e.getItem().getType())) {
			if (e.getAction() == Action.LEFT_CLICK_AIR || e.getAction() == Action.LEFT_CLICK_BLOCK) {
				if (!meteorCooldown.isRunning()) {
					meteorCooldown.start();
					meteor(false, false, null);
				}	
			}
		}
	}
	
	@SubscribeEvent
	public void onEntityDamage(EntityDamageEvent e) {
		if (e.getEntity().equals(getPlayer()) && (e.getCause().equals(DamageCause.BLOCK_EXPLOSION) || e.getCause().equals(DamageCause.ENTITY_EXPLOSION))) {
			e.setCancelled(true);
		}
	}
	
	@SubscribeEvent
	public void onEntityDamageByBlock(EntityDamageByBlockEvent e) {
		onEntityDamage(e);
	}
	
	@SubscribeEvent(priority = 5)
	public void onEntityDamageByEntity(EntityDamageByEntityEvent e) {
		onEntityDamage(e);
		if (e.getDamager().hasMetadata("Firework")) {
			e.setCancelled(true);
		}
	}
	
	public boolean ActiveSkill(Material material, ClickType clicktype) {
		if (material.equals(Material.IRON_INGOT) && clicktype.equals(ClickType.RIGHT_CLICK)) {
			if (!cooldown.isCooldown()) {
				for (Participant participant : getGame().getParticipants()) {
					if (predicate.test(participant.getPlayer())) {
						meteor(true, true, participant.getPlayer());
					}
				}
				return cooldown.start();
			}
		}
		return false;
	}
	
	private AbilityTimer meteorCooldown = new AbilityTimer(meteorCool) {
		
	}.setPeriod(TimeUnit.TICKS, 1).register();
	
	private AbilityTimer passive = new AbilityTimer() {
		
		@Override
		public void run(int count) {
			ac.update("§b별똥별 피해량§f: §c" + df.format(getDamage()));
			if (!meteorCooldown.isRunning() && swords.contains(getPlayer().getInventory().getItemInMainHand().getType())) {
				for (Location loc : circle.toLocations(getPoint(getPlayer())).floor(getPoint(getPlayer()).getY())) {
    				ParticleLib.REDSTONE.spawnParticle(getPlayer(), loc, color);
    			}
			}
		}
		
	}.setPeriod(TimeUnit.TICKS, 1).register();
	
	public class Bullet extends AbilityTimer implements Listener {
		
		private final LivingEntity shooter;
		private final CustomEntity entity;
		private final Predicate<Entity> predicate;
		private final Set<Damageable> hitcheck = new HashSet<>();
		private Vector forward;
		private int stacks = 0;
		private boolean turns = true;
		private boolean add = false;
		private double damage;
		private boolean stun;
		
		private RGB gradation;
		
		private final Location endLocation;
		private Location lastLocation;
		
		private GradationType type = random.pick(GradationType.values());
		private List<RGB> gradations = type.getColors();
		
		private int particle = 0;
		
		private Bullet(LivingEntity shooter, Location startLocation, Location endLocation, double damage, boolean stun) {
			super(200);
			setPeriod(TimeUnit.TICKS, 1);
			this.stun = stun;
			this.damage = damage;
			this.shooter = shooter;
			this.endLocation = endLocation;
			this.entity = new Bullet.ArrowEntity(startLocation.getWorld(), startLocation.getX(), startLocation.getY(),
			        startLocation.getZ()).resizeBoundingBox(-.75, -.75, -.75, .75, .75, .75);
			this.lastLocation = startLocation;
			this.forward = endLocation.toVector().subtract(startLocation.toVector()).normalize().multiply(5);
			this.predicate = new Predicate<Entity>() {
			    @Override
			    public boolean test(Entity entity) {
			        if (entity.equals(shooter))
			            return false;
			        if (entity instanceof ArmorStand)
			            return false;
			        if (entity instanceof Player) {
			            if (entity.equals(getPlayer()))
			                return true;
			            if (!getGame().isParticipating(entity.getUniqueId())
			                    || (getGame() instanceof DeathManager.Handler && ((DeathManager.Handler) getGame())
			                            .getDeathManager().isExcluded(entity.getUniqueId()))
			                    || !getGame().getParticipant(entity.getUniqueId()).attributes().TARGETABLE.getValue()) {
			                return false;
			            }
			            if (getGame() instanceof Teamable) {
			                final Teamable teamGame = (Teamable) getGame();
			                final Participant entityParticipant = teamGame.getParticipant(entity.getUniqueId()),
			                        participant = teamGame.getParticipant(shooter.getUniqueId());
			                if (participant != null) {
			                    return !teamGame.hasTeam(entityParticipant) || !teamGame.hasTeam(participant)
			                            || (!teamGame.getTeam(entityParticipant).equals(teamGame.getTeam(participant)));
			                }
			            }
			        }
			        if (hitcheck.contains(entity))
			            return false;
			        return true;
			    }
			
			    @Override
			    public boolean apply(@Nullable Entity arg0) {
			        return false;
			    }
			};
		}
		
		@Override
		protected void onStart() {
			Bukkit.getPluginManager().registerEvents(this, AbilityWar.getPlugin());
		}

		@Override
		protected void run(int i) {
			Vector baseDir = endLocation.toVector().subtract(lastLocation.toVector()).normalize();
		
			Player p = LocationUtil.getNearestEntity(Player.class, lastLocation, predicate);
			if (p != null && lastLocation.distanceSquared(p.getEyeLocation()) <= 25) {
				Vector target = p.getEyeLocation().toVector().subtract(lastLocation.toVector()).normalize();
				forward = baseDir.multiply(0.15).add(target.multiply(0.85)).normalize().multiply(4);
			} else {
				forward = baseDir.multiply(4);
			}
		
		    Location newLocation = lastLocation.clone().add(forward);
		    
		    SoundLib.BLOCK_NOTE_BLOCK_HARP.playSound(lastLocation, 1, (float) (1.8 + random.nextInt(21) * 0.01));
		
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
		    }; iterator.hasNext();) {
		    	Location location = iterator.next();
				entity.setLocation(location);
				
		        for (Damageable damageable : LocationUtil.getConflictingEntities(Damageable.class, entity.getWorld(), entity.getBoundingBox(), predicate)) {
		            if (!shooter.equals(damageable)) {
		            	hitcheck.add(damageable);
		            	((LivingEntity) damageable).setNoDamageTicks(0);
		                Damages.damageArrow(damageable, getPlayer(), (float) damage);
		                stack++;
		                if (stun && damageable instanceof Player && getGame().isParticipating((Player) damageable)) Stun.apply(getGame().getParticipant((Player) damageable), TimeUnit.TICKS, stunDuration);
						SoundLib.ENTITY_FIREWORK_ROCKET_LARGE_BLAST.playSound(location, 1, 0.8f);
						final Firework firework = location.getWorld().spawn(location, Firework.class);
						final FireworkMeta meta = firework.getFireworkMeta();
						List<RGB> allColors = new ArrayList<>();

						for (GradationType gradationtype : GradationType.values()) {
						    allColors.addAll(gradationtype.getColors());
						}

						Collections.shuffle(allColors);

						Color color1 = allColors.get(0).getColor();
						Color color2 = allColors.get(1).getColor();
						Color color3 = allColors.get(2).getColor();
						Color color4 = allColors.get(3).getColor();
						
						meta.addEffect(
								FireworkEffect.builder()
								.withColor(color1, color2, color3, color4)
								.with(random.nextBoolean() ? Type.STAR : Type.BALL)
								.build()
						);	
						meta.setPower(0);
						firework.setFireworkMeta(meta);
						firework.setMetadata("Firework", NULL_VALUE);
						new BukkitRunnable() {
							@Override
							public void run() {
								firework.detonate();
							}
						}.runTaskLater(AbilityWar.getPlugin(), 1L);
		                this.stop(false);
		                return;
		            }
		        }
				
				Block block = location.getBlock();
				Material type = block.getType();
				if (type.isSolid()) {
					if (entity.getLocation().getY() - endLocation.getY() <= 5) {
						getPlayer().getWorld().createExplosion(location.getX(), location.getY(), location.getZ(), 0.25f + (stack * 0.25f), false, true);
						stop(false);
						stack = 0;
						SoundLib.ENTITY_FIREWORK_ROCKET_LARGE_BLAST.playSound(location, 1, 0.8f);
						final Firework firework = location.getWorld().spawn(location, Firework.class);
						final FireworkMeta meta = firework.getFireworkMeta();
						List<RGB> allColors = new ArrayList<>();

						for (GradationType gradationtype : GradationType.values()) {
						    allColors.addAll(gradationtype.getColors());
						}

						Collections.shuffle(allColors);

						Color color1 = allColors.get(0).getColor();
						Color color2 = allColors.get(1).getColor();
						Color color3 = allColors.get(2).getColor();
						Color color4 = allColors.get(3).getColor();
						
						meta.addEffect(
								FireworkEffect.builder()
								.withColor(color1, color2, color3, color4)
								.withTrail()
								.withFlicker()
								.with(random.nextBoolean() ? Type.STAR : Type.BALL)
								.build()
						);	
						meta.setPower(0);
						firework.setFireworkMeta(meta);
						firework.setMetadata("Firework", NULL_VALUE);
						new BukkitRunnable() {
							@Override
							public void run() {
								firework.detonate();
							}
						}.runTaskLater(AbilityWar.getPlugin(), 1L);
						return;
					} else {
						block.breakNaturally();
						SoundLib.ENTITY_GENERIC_EXPLODE.playSound(block.getLocation(), 0.4f, 0.5f);
						ParticleLib.EXPLOSION_NORMAL.spawnParticle(block.getLocation(), 0.2, 0.2, 0.2, 3, 0.1);
					}
				}
		
				if (particle >= 3) {
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
			        particle = 0;
				} else particle++;
		    }
		
		    lastLocation = newLocation;
		    
		    if (lastLocation.distanceSquared(endLocation) <= 3 && lastLocation.getY() <= endLocation.getY()) stop(false);
		}

	    @Override
	    protected void onEnd() {
	        onSilentEnd();
	    }

	    @Override
	    protected void onSilentEnd() {
	        hitcheck.clear();
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
	            new Bullet(deflectedPlayer, getPoint(deflectedPlayer).clone().add(random.nextInt(1000) * 0.01, random.nextInt(51) + 50, random.nextInt(1000) * 0.01), getPoint(deflectedPlayer), damage, stun).start();
	        }

	        @Override
	        public ProjectileSource getShooter() {
	            return shooter;
	        }

	        @Override
	        protected void onRemove() {}

	    }

	}
	
}
