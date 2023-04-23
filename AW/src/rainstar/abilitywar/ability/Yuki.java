package rainstar.abilitywar.ability;

import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import javax.annotation.Nullable;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Damageable;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.projectiles.ProjectileSource;
import org.bukkit.util.Vector;

import daybreak.abilitywar.ability.AbilityBase;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.SubscribeEvent;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.config.ability.AbilitySettings.SettingObject;
import daybreak.abilitywar.game.AbstractGame.CustomEntity;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.manager.effect.Frost;
import daybreak.abilitywar.game.manager.effect.event.ParticipantPreEffectApplyEvent;
import daybreak.abilitywar.game.module.DeathManager;
import daybreak.abilitywar.game.team.interfaces.Teamable;
import daybreak.abilitywar.utils.base.Formatter;
import daybreak.abilitywar.utils.base.color.Gradient;
import daybreak.abilitywar.utils.base.color.RGB;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.base.math.LocationUtil;
import daybreak.abilitywar.utils.base.math.VectorUtil.Vectors;
import daybreak.abilitywar.utils.base.math.geometry.Circle;
import daybreak.abilitywar.utils.base.math.geometry.Points;
import daybreak.abilitywar.utils.base.minecraft.damage.Damages;
import daybreak.abilitywar.utils.base.minecraft.entity.decorator.Deflectable;
import daybreak.abilitywar.utils.base.minecraft.nms.NMS;
import daybreak.abilitywar.utils.base.minecraft.raytrace.RayTrace;
import daybreak.abilitywar.utils.base.random.Random;
import daybreak.abilitywar.utils.library.MaterialX;
import daybreak.abilitywar.utils.library.ParticleLib;
import daybreak.abilitywar.utils.library.SoundLib;
import daybreak.google.common.base.Predicate;
import daybreak.google.common.base.Strings;
import rainstar.abilitywar.effect.Chill;
import rainstar.abilitywar.effect.SnowflakeMark;

@AbilityManifest(name = "유키", rank = Rank.S, species = Species.HUMAN, explain = {
		"§7패시브 §8- §b만년설§f: §b§n빙결§f 외 §n상태이상§f을 받을 때 지속시간 절반의 §b§n빙결§f로 교체합니다.",
		"§7지팡이 우클릭 §8- §3아이스 볼트§f: 우클릭을 유지하는 동안 영창하여 이동 속도가 감소합니다.",
		" 우클릭을 해제할 때 영창한 만큼 강해진 §b냉기탄§f을 발사해 가까이의 적에게 발사됩니다.",
		" §b냉기탄§f에 맞은 적은 §9§n냉기§f 상태이상을 받고, 이미 §9§n냉기§f라면 §b§n빙결§f시킵니다.",
		" 영창을 $[MAX_CASTING]초 이상 유지하면 영창이 취소되고 자신이 $[SELF_FROST]초 §b§n빙결§f됩니다.",
		"§7지팡이 좌클릭 §8- §9블리자드§f: 자신을 포함한 $[RANGE]칸 내 모든 생명체의 §b§n빙결§f을 깨뜨리는 한기를",
		" 퍼뜨립니다. 자신은 §b§n빙결§f 시간의 $[MULTIPLY]배만큼 §b신속 $[SPEED_AMPLIFIER]§f를 획득하고, 다른 생명체는",
		" 방어력이 2 감소하는 §b§n눈꽃 표식§f을 최대 3까지 얻습니다. $[COOLDOWN]",
		"§1[§9냉기§1]§f 이동 속도, 공격 속도, 회복력이 감소합니다.",
		" 지속시간과 쿨타임 타이머가 천천히 흐릅니다."
		},
		summarize = {
		"자신이 받는 모든 상태이상이 지속시간 절반의 §b§n빙결§f이 됩니다.",
		"§6지팡이 우클릭§f을 유지하여 영창해, 영창한 만큼 강해지는 §b냉기탄§f을 발사합니다.",
		"냉기탄에 적중한 적은 §9§n냉기§f되고, 이미 §9§n냉기§f였다면 §b§n빙결§f시킵니다.",
		"영창 게이지를 최대까지 영창해버리면 영창이 취소되고 자신이 §b§n빙결§f됩니다.",
		"§6지팡이 좌클릭§f으로 자신을 포함한 주변 §b§n빙결§f자들의 §b§n빙결§f을 깨부숩니다.",
		"이때 자신은 신속을, 적은 방어력이 감소하는 §b§n눈꽃 표식§f을 얻습니다. §8(§73중첩)§8"
		})

public class Yuki extends AbilityBase {
	
	public Yuki(Participant participant) {
		super(participant);
	}

	public static final SettingObject<Double> MAX_CASTING = 
			abilitySettings.new SettingObject<Double>(Yuki.class, "max-casting", 7.5,
            "# 최대 영창시간", "# 단위: 초") {
		
        @Override
        public boolean condition(Double value) {
            return value >= 0;
        }
        
    };
    
	public static final SettingObject<Double> SELF_FROST = 
			abilitySettings.new SettingObject<Double>(Yuki.class, "self-frost", 3.0,
            "# 영창 캔슬 시 자체 빙결", "# 단위: 초") {
		
        @Override
        public boolean condition(Double value) {
            return value >= 0;
        }
        
    };
	
	public static final SettingObject<Double> RANGE = 
			abilitySettings.new SettingObject<Double>(Yuki.class, "range", 8.0,
            "# 블리자드 범위", "# 단위: 칸") {
		
        @Override
        public boolean condition(Double value) {
            return value >= 0;
        }
        
    };

	public static final SettingObject<Double> MULTIPLY = 
			abilitySettings.new SettingObject<Double>(Yuki.class, "multiply", 3.0,
            "# 신속 지속시간", "# 단위: 배") {
		
        @Override
        public boolean condition(Double value) {
            return value >= 0;
        }
        
    };
    
	public static final SettingObject<Integer> SPEED_AMPLIFIER = 
			abilitySettings.new SettingObject<Integer>(Yuki.class, "speed-amplifier", 1,
            "# 신속 포션 효과 계수", "# 주의! 0부터 시작합니다.", "# 0일 때 포션 효과 계수는 1레벨,", "# 1일 때 포션 효과 계수는 2레벨입니다.") {
		
        @Override
        public boolean condition(Integer value) {
            return value >= 0;
        }
        
		@Override
		public String toString() {
			return "" + (1 + getValue());
        }
		
    };
    
	public static final SettingObject<Integer> COOLDOWN = 
			abilitySettings.new SettingObject<Integer>(Yuki.class, "cooldown", 33,
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
    
	private static final Points LAYER = Points.of(0.12, new boolean[][]{
		{false, false, false, false, false, false, false, true, false, false, false, false, false, false, false},
		{false, false, false, false, false, true, false, false, false, true, false, false, false, false, false},
		{false, false, false, false, false, false, true, true, true, false, false, false, false, false, false},
		{true, false, true, false, false, false, false, false, false, false, false, false, true, false, true},
		{false, false, true, false, false, false, false, true, false, false, false, false, true, false, false},
		{true, true, true, false, false, true, false, true, false, true, false, false, true, true, true},
		{false, false, false, true, false, false, true, true, true, false, false, true, false, false, false},
		{false, false, false, false, true, true, false, false, false, true, true, false, false, false, false},
		{false, true, true, false, false, false, false, true, false, false, false, false, true, true, false},
		{false, false, false, false, true, true, false, false, false, true, true, false, false, false, false},
		{false, false, false, true, false, false, true, true, true, false, false, true, false, false, false},
		{true, true, true, false, false, true, false, true, false, true, false, false, true, true, true},
		{false, false, true, false, false, false, false, true, false, false, false, false, true, false, false},
		{true, false, true, false, false, false, false, false, false, false, false, false, true, false, true},
		{false, false, false, false, false, false, true, true, true, false, false, false, false, false, false},
		{false, false, false, false, false, true, false, false, false, true, false, false, false, false, false},
		{false, false, false, false, false, false, false, true, false, false, false, false, false, false, false}
	});
    
    private final int maxcasting = (int) (MAX_CASTING.getValue() * 20);
    private final int selffrost = (int) (SELF_FROST.getValue() * 20);
    private final double range = RANGE.getValue();
    private final double multiply = MULTIPLY.getValue();
    private final int speed = SPEED_AMPLIFIER.getValue();
	private long last;
	private final Cooldown cooldown = new Cooldown(COOLDOWN.getValue());
	private static final RGB startColor = RGB.of(254, 254, 254), endColor = RGB.of(16, 248, 254);
	private final List<RGB> gradations = Gradient.createGradient(maxcasting, startColor, endColor);
	private final Random random = new Random();
	private Vectors layervectors;
	private final Circle circle = Circle.of(1.5, 70);
	private boolean give = false;
	private int addDamage = 6;
	private static final Vector zeroV = new Vector(0, 0, 0);
	
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
		public boolean apply(@javax.annotation.Nullable Entity arg0) {
			return false;
		}
	};
	
	private final Predicate<Entity> frostpredicate = new Predicate<Entity>() {
		@Override
		public boolean test(Entity entity) {
			if (entity instanceof Player) {
				Participant participants = getGame().getParticipant((Player) entity);
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
				if (!participants.hasEffect(Frost.registration)) return false;
			}
			return true;
		}

		@Override
		public boolean apply(@Nullable Entity arg0) {
			return false;
		}
	};
	
	protected void onUpdate(Update update) {
		if (update == Update.RESTRICTION_CLEAR && !give) {			
			ItemStack stick = new ItemStack(Material.STICK, 1);
			ItemMeta stickmeta = stick.getItemMeta();	
			
			if (random.nextInt(100) == 0) {
				stickmeta.setDisplayName("§6딱총나무 지팡이");
				stickmeta.getLore().add("§7어디 마법학교에서 훔쳐왔나요?");
				stickmeta.addEnchant(Enchantment.DAMAGE_ALL, 10, true);
			} else {
				stickmeta.setDisplayName("§b설한의 지팡이");
			}		
			stickmeta.addEnchant(Enchantment.SILK_TOUCH, 1, true);
			stickmeta.addEnchant(Enchantment.KNOCKBACK, 2, true);
			stickmeta.setUnbreakable(true);		
			stick.setItemMeta(stickmeta);
			
			getPlayer().getInventory().addItem(stick);
			give = true;
		}
	}

	private final AbilityTimer breaking = new AbilityTimer() {
		
		Location mylocation;
		
    	@Override
		public void onStart() {
    		mylocation = getPlayer().getLocation().clone();
    		SoundLib.ENTITY_ILLUSIONER_CAST_SPELL.playSound(mylocation, 1, 1.3f);
    	}
		
    	@Override
		public void run(int count) {
    		Player p = LocationUtil.getNearestEntity(Player.class, mylocation, frostpredicate);
    		if (p != null) {  
    			if (p.equals(getPlayer())) {
    				ParticleLib.BLOCK_CRACK.spawnParticle(p.getLocation(), 1, 1, 1, 200, 0.5, MaterialX.ICE);
    				SoundLib.BLOCK_GLASS_BREAK.playSound(p.getLocation(), 1, 1.2f);
    				getPlayer().addPotionEffect(new PotionEffect(PotionEffectType.SPEED, (int) (getParticipant().getPrimaryEffect(Frost.registration).getCount() * multiply), speed));
    				getParticipant().removeEffects(Frost.registration);
    			} else {
            		if (LocationUtil.isInCircle(mylocation, p.getLocation(), range) && getGame().getParticipant(p).hasEffect(Frost.registration)) {
        				ParticleLib.BLOCK_CRACK.spawnParticle(p.getLocation(), 1, 1, 1, 200, 0.5, MaterialX.ICE);
        				SoundLib.BLOCK_GLASS_BREAK.playSound(p.getLocation(), 1, 1.2f);
        				getGame().getParticipant(p).removeEffects(Frost.registration);
        				Damages.damageMagic(p, getPlayer(), false, addDamage);
        				if (getGame().getParticipant(p).hasEffect(SnowflakeMark.registration)) {
        					if (getGame().getParticipant(p).getPrimaryEffect(SnowflakeMark.registration).getLevel() == 1) {
        						SnowflakeMark.apply(getGame().getParticipant(p), TimeUnit.SECONDS, 60, 2);
        					} else if (getGame().getParticipant(p).getPrimaryEffect(SnowflakeMark.registration).getLevel() >= 2) {
        						SnowflakeMark.apply(getGame().getParticipant(p), TimeUnit.SECONDS, 60, 3);
        					}
        				} else {
        					SnowflakeMark.apply(getGame().getParticipant(p), TimeUnit.SECONDS, 60, 1);
        				}
        				addDamage += 2;
            		}		
    			}
    		} else {
    			stop(false);
    		}
    	}
    	
    	@Override
    	public void onEnd() {
    		onSilentEnd();
    	}
    	
    	@Override
    	public void onSilentEnd() {
    		cooldown.start();
    		addDamage = 6;
    	}
		
	}.setPeriod(TimeUnit.TICKS, 1).register();
	
	private AbilityTimer rightclickchecker = new AbilityTimer() {
		
		Location loc;
		int randominta, randomintb;
		
		@Override
		public void onStart() {
			randominta = random.nextInt(70);
			randomintb = random.nextInt(100);
		}
		
		@Override
		public void run(int count) {
			if (System.currentTimeMillis() - last >= 250) {
				if (count > (maxcasting / 20.0)) {
					Damageable nearest = LocationUtil.getNearestEntity(Damageable.class, getPlayer().getLocation(), predicate);
					if (nearest != null) {
						SoundLib.BLOCK_GLASS_BREAK.playSound(loc, 1, 1.5f);
						SoundLib.ENTITY_ENDER_EYE_DEATH.playSound(loc, 1, 2);
						new Bullet(getPlayer(), loc, nearest.getLocation().add(0, 1, 0).clone().subtract(loc.clone()).toVector().normalize(), (int) ((count / (double) maxcasting) * 300), (count / (double) maxcasting) * 7, gradations.get(count)).start();
					}	
				}
				this.stop(false);
			} else if (count >= maxcasting - 1) {
				Frost.apply(getParticipant(), TimeUnit.TICKS, selffrost);
				stop(false);
			} else {
				NMS.sendTitle(getPlayer(), Strings.repeat("§b§k|", (int) ((count / (double) maxcasting) * 10)).concat(Strings.repeat("§7§k|", 10 - (int) ((count / (double) maxcasting) * 10))), "", 0, 100, 1);
				layervectors = LAYER.clone().rotateAroundAxisX(90).clone();
				if (count % 10 == 0) {
					for (Location location : layervectors.toLocations(getPlayer().getLocation()).floor(getPlayer().getLocation().getY())) {
						ParticleLib.REDSTONE.spawnParticle(location.clone().add(0, 0, -1), gradations.get(count));
					}	
				}
				loc = circle.toLocations(getPlayer().getLocation()).get(randominta).clone().add(0, (randomintb) * 0.01, 0);
				ParticleLib.REDSTONE.spawnParticle(loc, gradations.get(count));
				ParticleLib.PORTAL.spawnParticle(loc, 0, 0, 0, 5, (count * 0.1));	
			}
		}
		
		@Override
		public void onEnd() {
			onSilentEnd();
		}
		
		@Override
		public void onSilentEnd() {
			NMS.clearTitle(getPlayer());
		}
		
	}.setPeriod(TimeUnit.TICKS, 1).register();
    
	@SubscribeEvent
	public void onPlayerInteract(PlayerInteractEvent e) {
		if (e.getPlayer().equals(getPlayer()) && getPlayer().getInventory().getItemInMainHand().getType().equals(Material.STICK)) {
			if ((e.getAction().equals(Action.RIGHT_CLICK_AIR) || e.getAction().equals(Action.RIGHT_CLICK_BLOCK))) {
				last = System.currentTimeMillis();
				if (!rightclickchecker.isRunning()) {
					rightclickchecker.start();
				}
				getPlayer().setVelocity(zeroV);
			} else if ((e.getAction().equals(Action.LEFT_CLICK_AIR) || e.getAction().equals(Action.LEFT_CLICK_BLOCK))) {
				if (!cooldown.isCooldown() && !breaking.isRunning()) {
					if (LocationUtil.getEntitiesInCircle(Player.class, getPlayer().getLocation(), range, frostpredicate).size() > 0) {
						getPlayer().sendMessage("§3[§b!§3] §a능력을 사용하였습니다.");
						breaking.start();	
					} else getPlayer().sendMessage("§3[§b!§3] §f주변에 빙결된 대상이 없습니다.");
				}
			}
		}
	}
    
	@SubscribeEvent(onlyRelevant = true)
	public void onParticipantEffectApply(ParticipantPreEffectApplyEvent e) {
		if (!e.getEffectType().getManifest().name().equals("빙결")) {
			e.setCancelled(true);
			Frost.apply(getParticipant(), TimeUnit.TICKS, e.getDuration() / 2);
		}
	}
    
	public class Bullet extends AbilityTimer {

		private final LivingEntity shooter;
		private final CustomEntity entity;
		private final Vector forward;
		private final Predicate<Entity> predicate;
		private final int time;
		private final double damage;
		private Damageable targeted;

		private final RGB color;

		private Bullet(LivingEntity shooter, Location startLocation, Vector arrowVelocity, int time, double damage, RGB color) {
			super(100);
			setPeriod(TimeUnit.TICKS, 1);
			this.shooter = shooter;
			this.entity = new ArrowEntity(startLocation.getWorld(), startLocation.getX(), startLocation.getY(), startLocation.getZ()).resizeBoundingBox(-.75, -.75, -.75, .75, .75, .75);
			this.forward = arrowVelocity.multiply(damage);
			this.time = time;
			this.damage = damage;
			this.color = color;
			this.lastLocation = startLocation;
			this.predicate = new Predicate<Entity>() {
				@Override
				public boolean test(Entity entity) {
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
				public boolean apply(@javax.annotation.Nullable Entity arg0) {
					return false;
				}
			};
		}

		private Location lastLocation;
		private Location finalLocation;

		@Override
		protected void run(int i) {
			final Location newLocation = lastLocation.clone().add(forward);
			for (Iterator<Location> iterator = new Iterator<Location>() {
				private final Vector vectorBetween = newLocation.toVector().subtract(lastLocation.toVector()), unit = vectorBetween.clone().normalize().multiply(.35);
				private final int amount = (int) (vectorBetween.length() / .35);
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
				final double y = location.getY();
				if (y < 0 || y > 256 || !location.getChunk().isLoaded()) {
					stop(false);
					return;
				}
				if (type.isSolid()) {
					if (RayTrace.hitsBlock(location.getWorld(), lastLocation.getX(), lastLocation.getY(), lastLocation.getZ(), location.getX(), location.getY(), location.getZ())) {
						finalLocation = location;
						stop(false);
						return;
					}
				}
				for (Damageable damageable : LocationUtil.getConflictingEntities(Damageable.class, shooter.getWorld(), entity.getBoundingBox(), predicate)) {
					if (!shooter.equals(damageable)) {
						Damages.damageMagic(damageable, getPlayer(), true, (float) damage);
						targeted = damageable;
						if (damageable instanceof Player) {
							Participant p = getGame().getParticipant((Player) damageable);
							if (!p.hasEffect(Chill.registration)) Chill.apply(p, TimeUnit.TICKS, time);
							else {
								p.removeEffects(Chill.registration);
								Frost.apply(p, TimeUnit.TICKS, (int) ((time * 2) / 3.0));
							}
						}
						finalLocation = location;
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
			onSilentEnd();
		}

		@Override
		protected void onSilentEnd() {
			entity.remove();
			if (finalLocation != null) {
				SoundLib.BLOCK_FIRE_EXTINGUISH.playSound(finalLocation, 1, 1.35f);
				ParticleLib.CLOUD.spawnParticle(finalLocation, 0, 0, 0, 8, 1.5);
				for (Player player : LocationUtil.getNearbyEntities(Player.class, finalLocation, 1.5, 1.5, predicate)) {
					if (!player.equals(targeted)) {
						Participant p = getGame().getParticipant(player);
						if (!p.hasEffect(Chill.registration)) Chill.apply(p, TimeUnit.TICKS, time);
						else {
							p.removeEffects(Chill.registration);
							Frost.apply(p, TimeUnit.TICKS, (int) ((time * 2) / 3.0));
						}	
					}
				}	
			}
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
				new Bullet(deflectedPlayer, lastLocation, newDirection, time, damage, color).start();
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
	
}
