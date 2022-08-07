package rainstar.aw.synergy;

import rainstar.aw.effect.Confusion;
import rainstar.aw.effect.Madness;
import daybreak.abilitywar.AbilityWar;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.ability.SubscribeEvent;
import daybreak.abilitywar.ability.decorator.ActiveHandler;
import daybreak.abilitywar.config.Configuration.Settings;
import daybreak.abilitywar.config.ability.AbilitySettings.SettingObject;
import daybreak.abilitywar.config.serializable.SpawnLocation;
import daybreak.abilitywar.game.AbstractGame.CustomEntity;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.list.mix.synergy.Synergy;
import daybreak.abilitywar.game.manager.effect.Bleed;
import daybreak.abilitywar.game.module.DeathManager;
import daybreak.abilitywar.game.team.interfaces.Teamable;
import daybreak.abilitywar.utils.base.Formatter;
import daybreak.abilitywar.utils.base.color.RGB;
import daybreak.abilitywar.utils.base.concurrent.SimpleTimer.TaskType;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.base.math.LocationUtil;
import daybreak.abilitywar.utils.base.math.VectorUtil;
import daybreak.abilitywar.utils.base.math.geometry.Points;
import daybreak.abilitywar.utils.base.minecraft.FireworkUtil;
import daybreak.abilitywar.utils.base.minecraft.damage.Damages;
import daybreak.abilitywar.utils.base.minecraft.entity.decorator.Deflectable;
import daybreak.abilitywar.utils.base.minecraft.entity.health.Healths;
import daybreak.abilitywar.utils.library.ParticleLib;
import daybreak.abilitywar.utils.library.PotionEffects;
import daybreak.abilitywar.utils.library.SoundLib;
import daybreak.google.common.base.Predicate;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.FireworkEffect.Type;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Damageable;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByBlockEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent.RegainReason;
import org.bukkit.projectiles.ProjectileSource;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;

@AbilityManifest(
		name = "조커", rank = Rank.S, species = Species.HUMAN, explain = {
		"§7철괴 우클릭 §8- §c짠 짠 짠§f: 스폰으로 즉시 이동합니다. 이동한 후 10초 이내에",
		" 철괴를 다시 우클릭하면 원래 위치로 돌아가 주변 $[BLIND_RANGE]칸 내 플레이어를 실명시키고,",
		" 카드를 휘날려 피해를 입힙니다. $[COOLDOWN_CONFIG]",
		"§7사망 §8- §c내 죽음이 내 삶보다 가치있기를§f: 나를 죽인 대상과 그 주변 $[MADNESS_RANGE]칸 내",
		" 모든 플레이어에게 3분간 지속되는 광란 디버프를 겁니다."
		})

public class Joker extends Synergy implements ActiveHandler {
	
	public Joker(Participant participant) {
		super(participant);
	}
	
	public static final SettingObject<Integer> COOLDOWN_CONFIG = synergySettings.new SettingObject<Integer>(Joker.class, "cooldown", 110,
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

	public static final SettingObject<Integer> BLIND_RANGE = synergySettings.new SettingObject<Integer>(Joker.class, "blind-range", 10,
			"# 실명 스킬 범위") {

		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}

	};
	
	public static final SettingObject<Integer> MADNESS_RANGE = synergySettings.new SettingObject<Integer>(Joker.class, "madness-range", 5,
			"# 광란 스킬 범위") {

		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}

	};
	
	private static final Color[] colors = {
			Color.SILVER, Color.YELLOW, Color.AQUA, Color.LIME, Color.PURPLE, Color.RED, Color.ORANGE, Color.NAVY, Color.MAROON
	};
	
	private static final Type[] types = {
			Type.BALL_LARGE, Type.STAR
	};
	
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
	
	protected void onUpdate(Update update) {
	    if (update == Update.RESTRICTION_CLEAR) {
	    	refill();
	    } 
	}
	
	@SuppressWarnings("unused")
	private CardBullet bullet = null;
	private Location originalPoint = null;
	private final int blindrange = BLIND_RANGE.getValue();
	private final int madnessrange = MADNESS_RANGE.getValue();
	private List<Cards> deck = new ArrayList<>();
	
	private void updateTime(World world) {
		world.setTime(13000);
	}
	
	private final Cooldown cool = new Cooldown(COOLDOWN_CONFIG.getValue());
	
	@SubscribeEvent
	public void onEntityDamage(EntityDamageEvent e) {
		if (e.getEntity().equals(getPlayer()) && e.getCause() == DamageCause.BLOCK_EXPLOSION) {
			e.setCancelled(true);
		}
	}
	
	@SubscribeEvent
	public void onEntityDamageByBlock(EntityDamageByBlockEvent e) {
		onEntityDamage(e);
	}
	
	@SubscribeEvent
	public void onEntityDamageByEntity(EntityDamageByEntityEvent e) {
		onEntityDamage(e);
		if (e.getEntity().equals(getPlayer()) && getPlayer().getHealth() - e.getFinalDamage() <= 0 && e.getDamager() instanceof Player) {
			for (Player p : LocationUtil.getNearbyEntities(Player.class, e.getDamager().getLocation(), madnessrange, madnessrange, predicate)) {
				Madness.apply(getGame().getParticipant(p), TimeUnit.MINUTES, 3, 20);	
			}
			updateTime(getPlayer().getWorld());
			new BukkitRunnable() {
				@Override
				public void run() {
					if (e.getDamager() != null) {
			    		FireworkUtil.spawnRandomFirework(e.getDamager().getLocation(), colors, colors, types, 1);	
					}
				}	
			}.runTaskLater(AbilityWar.getPlugin(), 1L);
			new BukkitRunnable() {
				@Override
				public void run() {
					if (e.getDamager() != null) {
			    		FireworkUtil.spawnRandomFirework(e.getDamager().getLocation(), colors, colors, types, 1);	
					}
				}	
			}.runTaskLater(AbilityWar.getPlugin(), 20L);
			new BukkitRunnable() {
				@Override
				public void run() {
					if (e.getDamager() != null) {
			    		FireworkUtil.spawnRandomFirework(e.getDamager().getLocation(), colors, colors, types, 1);	
					}
				}	
			}.runTaskLater(AbilityWar.getPlugin(), 40L);
			new BukkitRunnable() {
				@Override
				public void run() {
					if (e.getDamager() != null) {
			    		FireworkUtil.spawnRandomFirework(e.getDamager().getLocation(), colors, colors, types, 1);	
					}
				}	
			}.runTaskLater(AbilityWar.getPlugin(), 60L);
			new BukkitRunnable() {
				@Override
				public void run() {
					if (e.getDamager() != null) {
			    		FireworkUtil.spawnRandomFirework(e.getDamager().getLocation(), colors, colors, types, 1);	
					}
				}	
			}.runTaskLater(AbilityWar.getPlugin(), 80L);
			new BukkitRunnable() {
				@Override
				public void run() {
					if (e.getDamager() != null) {
			    		FireworkUtil.spawnRandomFirework(e.getDamager().getLocation(), colors, colors, types, 1);	
					}
				}	
			}.runTaskLater(AbilityWar.getPlugin(), 100L);
			new BukkitRunnable() {
				@Override
				public void run() {
					if (e.getDamager() != null) {
			    		FireworkUtil.spawnRandomFirework(e.getDamager().getLocation(), colors, colors, types, 1);	
					}
				}	
			}.runTaskLater(AbilityWar.getPlugin(), 120L);
			new BukkitRunnable() {
				@Override
				public void run() {
					if (e.getDamager() != null) {
			    		FireworkUtil.spawnRandomFirework(e.getDamager().getLocation(), colors, colors, types, 1);	
					}
				}	
			}.runTaskLater(AbilityWar.getPlugin(), 140L);
			new BukkitRunnable() {
				@Override
				public void run() {
					if (e.getDamager() != null) {
			    		FireworkUtil.spawnRandomFirework(e.getDamager().getLocation(), colors, colors, types, 1);	
					}
				}	
			}.runTaskLater(AbilityWar.getPlugin(), 160L);
			new BukkitRunnable() {
				@Override
				public void run() {
					if (e.getDamager() != null) {
			    		FireworkUtil.spawnRandomFirework(e.getDamager().getLocation(), colors, colors, types, 1);	
					}
				}	
			}.runTaskLater(AbilityWar.getPlugin(), 180L);
		}
	}
	
	public boolean ActiveSkill(Material material, ClickType clicktype) {
		if (material == Material.IRON_INGOT && clicktype == ClickType.RIGHT_CLICK) {
			if (!skill.isDuration()) {
				if (!cool.isCooldown()) {
					skill.start();
					return true;
				}
			} else {
				if (originalPoint != null) getPlayer().teleport(originalPoint);
				SoundLib.ENTITY_BAT_TAKEOFF.playSound(getPlayer());
				skill.stop(false);
				for (Player p : LocationUtil.getEntitiesInCircle(Player.class, getPlayer().getLocation(), blindrange, predicate)) {
					SoundLib.ENTITY_WITHER_SPAWN.playSound(p);
					PotionEffects.BLINDNESS.addPotionEffect(p, 100, 2, true);
				}
				new AbilityTimer(TaskType.NORMAL, 10) {
					
					@Override
					public void run(int count) {
						SoundLib.ENTITY_PLAYER_ATTACK_SWEEP.playSound(getPlayer().getLocation(), 1, 2f);
						new CardBullet(getPlayer(), getPlayer().getEyeLocation().clone().subtract(0, 0.5, 0), VectorUtil.rotateAroundAxisY(getPlayer().getEyeLocation().getDirection().clone().setY(0).normalize(), count * 36), getPlayer().getLocation().getYaw(), deck.get(0).getSuit(), 15).start();
						deck.remove(0);
						if (deck.isEmpty()) {
							refill();
						}
					}
					
				}.setPeriod(TimeUnit.TICKS, 1).start();
			}
		}
		return false;
	}
	
	private final Duration skill = new Duration(10, cool) {
		
		@Override
		protected void onDurationStart() {
			originalPoint = getPlayer().getLocation();
			final SpawnLocation spawnLocation = Settings.getSpawnLocation();
			if (getPlayer().getWorld().getName().equals(spawnLocation.world)) {
				getPlayer().teleport(spawnLocation.toBukkitLocation());
			} else {
				getPlayer().teleport(getPlayer().getWorld().getSpawnLocation());
			}
		}

		@Override
		protected void onDurationProcess(int seconds) {
		}
		
	};
	
	public void refill() {
		deck.clear();
		for (int a = 0; a < 6; a++) {
            deck.add(new Cards(a, 0));
		}
        Collections.shuffle(deck);
	}
	
	class Cards {
		
		private final int rank;
        private final int suit;
        private final String[] ranks = {"Joker"};
        private final String[] suits = {"§8♠ §f","§c♥ §f","§8♣ §f","§c♦ §f", "§8", "§c"};

        public Cards(int suit, int values) {
            this.rank = values;
            this.suit = suit;
        }

        public String toString() {
            return getSuitName() + getRankName();
        }

        public String getRankName() {
            return ranks[rank];
        }

        public String getSuitName() {
            return suits[suit];
        }
        
        public int getRank() {
        	return rank;
        }
        
        public int getSuit() {
        	return suit;
        }
        
	}
	
	public class CardBullet extends AbilityTimer {
		
		private final LivingEntity shooter;
		private final CustomEntity entity;
		private final Vector forward;
		private final Predicate<Entity> predicate;
		private final int suit;
		private final int rank;
		private Set<Damageable> checkhit = new HashSet<>();
		private final float yaw;

		private RGB black = RGB.of(1, 1, 1), white = RGB.of(254, 254, 254), red = RGB.of(254, 1, 1);
		private Location lastLocation;
		
		private final Points SPADE1 = Points.of(0.08, new boolean[][]{
			{false, false, false, false, false, false, false, false, false},
			{false, true, false, false, false, false, false, false, false},
			{false, false, false, false, false, false, false, false, false},
			{false, false, false, false, false, false, false, false, false},
			{false, false, false, false, true, false, false, false, false},
			{false, false, false, true, true, true, false, false, false},
			{false, false, true, true, true, true, true, false, false},
			{false, false, true, true, true, true, true, false, false},
			{false, false, true, true, true, true, true, false, false},
			{false, false, true, false, true, false, true, false, false},
			{false, false, false, false, true, false, false, false, false},
			{false, false, false, true, true, true, false, false, false},
			{false, false, false, false, false, false, false, false, false},
			{false, false, false, false, false, false, false, false, false},
			{false, false, false, false, false, false, false, true, false},
			{false, false, false, false, false, false, false, false, false}
		});
		
		private final Points SPADE2 = Points.of(0.08, new boolean[][]{
			{true, true, true, true, true, true, true, true, true},
			{true, false, true, true, true, true, true, true, true},
			{true, true, true, true, true, true, true, true, true},
			{true, true, true, true, true, true, true, true, true},
			{true, true, true, true, false, true, true, true, true},
			{true, true, true, false, false, false, true, true, true},
			{true, true, false, false, false, false, false, true, true},
			{true, true, false, false, false, false, false, true, true},
			{true, true, false, false, false, false, false, true, true},
			{true, true, false, true, false, true, false, true, true},
			{true, true, true, true, false, true, true, true, true},
			{true, true, true, false, false, false, true, true, true},
			{true, true, true, true, true, true, true, true, true},
			{true, true, true, true, true, true, true, true, true},
			{true, true, true, true, true, true, true, false, true},
			{true, true, true, true, true, true, true, true, true}
		});
		
		private final Points HEART1 = Points.of(0.08, new boolean[][]{
			{false, false, false, false, false, false, false, false, false},
			{false, true, false, false, false, false, false, false, false},
			{false, false, false, false, false, false, false, false, false},
			{false, false, false, false, false, false, false, false, false},
			{false, false, false, false, false, false, false, false, false},
			{false, false, false, true, false, true, false, false, false},
			{false, false, true, true, true, true, true, false, false},
			{false, false, true, true, true, true, true, false, false},
			{false, false, true, true, true, true, true, false, false},
			{false, false, false, true, true, true, false, false, false},
			{false, false, false, false, true, false, false, false, false},
			{false, false, false, false, false, false, false, false, false},
			{false, false, false, false, false, false, false, false, false},
			{false, false, false, false, false, false, false, false, false},
			{false, false, false, false, false, false, false, true, false},
			{false, false, false, false, false, false, false, false, false}
		});
		
		private final Points HEART2 = Points.of(0.08, new boolean[][]{
			{true, true, true, true, true, true, true, true, true},
			{true, false, true, true, true, true, true, true, true},
			{true, true, true, true, true, true, true, true, true},
			{true, true, true, true, true, true, true, true, true},
			{true, true, true, true, true, true, true, true, true},
			{true, true, true, false, true, false, true, true, true},
			{true, true, false, false, false, false, false, true, true},
			{true, true, false, false, false, false, false, true, true},
			{true, true, false, false, false, false, false, true, true},
			{true, true, true, false, false, false, true, true, true},
			{true, true, true, true, false, true, true, true, true},
			{true, true, true, true, true, true, true, true, true},
			{true, true, true, true, true, true, true, true, true},
			{true, true, true, true, true, true, true, true, true},
			{true, true, true, true, true, true, true, false, true},
			{true, true, true, true, true, true, true, true, true}
		});
		
		private final Points CLUB1 = Points.of(0.08, new boolean[][]{
			{false, false, false, false, false, false, false, false, false},
			{false, true, false, false, false, false, false, false, false},
			{false, false, false, false, false, false, false, false, false},
			{false, false, false, false, false, false, false, false, false},
			{false, false, false, false, true, false, false, false, false},
			{false, false, false, true, true, true, false, false, false},
			{false, false, true, false, true, false, true, false, false},
			{false, true, true, true, true, true, true, true, false},
			{false, false, true, false, true, false, true, false, false},
			{false, false, false, false, true, false, false, false, false},
			{false, false, false, true, true, true, false, false, false},
			{false, false, false, false, false, false, false, false, false},
			{false, false, false, false, false, false, false, false, false},
			{false, false, false, false, false, false, false, false, false},
			{false, false, false, false, false, false, false, true, false},
			{false, false, false, false, false, false, false, false, false}
		});
		
		private final Points CLUB2 = Points.of(0.08, new boolean[][]{
			{true, true, true, true, true, true, true, true, true},
			{true, false, true, true, true, true, true, true, true},
			{true, true, true, true, true, true, true, true, true},
			{true, true, true, true, true, true, true, true, true},
			{true, true, true, true, false, true, true, true, true},
			{true, true, true, false, false, false, true, true, true},
			{true, true, false, true, false, true, false, true, true},
			{true, false, false, false, false, false, false, false, true},
			{true, true, false, true, false, true, false, true, true},
			{true, true, true, true, false, true, true, true, true},
			{true, true, true, false, false, false, true, true, true},
			{true, true, true, true, true, true, true, true, true},
			{true, true, true, true, true, true, true, true, true},
			{true, true, true, true, true, true, true, true, true},
			{true, true, true, true, true, true, true, false, true},
			{true, true, true, true, true, true, true, true, true}
		});
		
		private final Points DIAMOND1 = Points.of(0.08, new boolean[][]{
			{false, false, false, false, false, false, false, false, false},
			{false, true, false, false, false, false, false, false, false},
			{false, false, false, false, false, false, false, false, false},
			{false, false, false, false, false, false, false, false, false},
			{false, false, false, false, true, false, false, false, false},
			{false, false, false, true, true, true, false, false, false},
			{false, false, true, true, true, true, true, false, false},
			{false, false, true, true, true, true, true, false, false},
			{false, false, true, true, true, true, true, false, false},
			{false, false, true, true, true, true, true, false, false},
			{false, false, false, true, true, true, false, false, false},
			{false, false, false, false, true, false, false, false, false},
			{false, false, false, false, false, false, false, false, false},
			{false, false, false, false, false, false, false, false, false},
			{false, false, false, false, false, false, false, true, false},
			{false, false, false, false, false, false, false, false, false}
		});
		
		private final Points DIAMOND2 = Points.of(0.08, new boolean[][]{
			{true, true, true, true, true, true, true, true, true},
			{true, false, true, true, true, true, true, true, true},
			{true, true, true, true, true, true, true, true, true},
			{true, true, true, true, true, true, true, true, true},
			{true, true, true, true, false, true, true, true, true},
			{true, true, true, false, false, false, true, true, true},
			{true, true, false, false, false, false, false, true, true},
			{true, true, false, false, false, false, false, true, true},
			{true, true, false, false, false, false, false, true, true},
			{true, true, false, false, false, false, false, true, true},
			{true, true, true, false, false, false, true, true, true},
			{true, true, true, true, false, true, true, true, true},
			{true, true, true, true, true, true, true, true, true},
			{true, true, true, true, true, true, true, true, true},
			{true, true, true, true, true, true, true, false, true},
			{true, true, true, true, true, true, true, true, true}
		});
		
		private final Points JOKER1 = Points.of(0.08, new boolean[][]{
			{false, false, false, false, false, false, false, false, false},
			{false, true, false, false, false, false, false, false, false},
			{false, false, false, false, false, false, false, false, false},
			{false, false, false, false, false, false, false, false, false},
			{false, false, false, true, true, true, false, false, false},
			{false, false, true, false, false, false, true, false, false},
			{false, false, true, false, false, false, true, false, false},
			{false, false, false, false, false, false, true, false, false},
			{false, false, false, false, false, true, false, false, false},
			{false, false, false, false, true, false, false, false, false},
			{false, false, false, false, false, false, false, false, false},
			{false, false, false, false, true, false, false, false, false},
			{false, false, false, false, false, false, false, false, false},
			{false, false, false, false, false, false, false, false, false},
			{false, false, false, false, false, false, false, true, false},
			{false, false, false, false, false, false, false, false, false}
		});
		
		private final Points JOKER2 = Points.of(0.08, new boolean[][]{
			{true, true, true, true, true, true, true, true, true},
			{true, false, true, true, true, true, true, true, true},
			{true, true, true, true, true, true, true, true, true},
			{true, true, true, true, true, true, true, true, true},
			{true, true, true, false, false, false, true, true, true},
			{true, true, false, true, true, true, false, true, true},
			{true, true, false, true, true, true, false, true, true},
			{true, true, true, true, true, true, false, true, true},
			{true, true, true, true, true, false, true, true, true},
			{true, true, true, true, false, true, true, true, true},
			{true, true, true, true, true, true, true, true, true},
			{true, true, true, true, false, true, true, true, true},
			{true, true, true, true, true, true, true, true, true},
			{true, true, true, true, true, true, true, true, true},
			{true, true, true, true, true, true, true, false, true},
			{true, true, true, true, true, true, true, true, true}
		});
		
		private CardBullet(LivingEntity shooter, Location startLocation, Vector arrowVelocity, float yaw, int suit, int rank) {
			super(3);
			setPeriod(TimeUnit.TICKS, 1);
			Joker.this.bullet = this;
			this.shooter = shooter;
			this.entity = new ArrowEntity(startLocation.getWorld(), startLocation.getX(), startLocation.getY(), startLocation.getZ()).resizeBoundingBox(-1, -0.5, -1, 1, 0.5, 1);
			this.forward = arrowVelocity.multiply(3);
			this.lastLocation = startLocation;
			this.suit = suit;
			this.rank = rank;
			this.yaw = yaw;
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
		protected void onStart() {
			SPADE1.rotateAroundAxisY(-yaw).rotateAroundAxis(VectorUtil.rotateAroundAxisY(forward.clone().normalize().setY(0), 90), 90);
			SPADE2.rotateAroundAxisY(-yaw).rotateAroundAxis(VectorUtil.rotateAroundAxisY(forward.clone().normalize().setY(0), 90), 90);
			HEART1.rotateAroundAxisY(-yaw).rotateAroundAxis(VectorUtil.rotateAroundAxisY(forward.clone().normalize().setY(0), 90), 90);
			HEART2.rotateAroundAxisY(-yaw).rotateAroundAxis(VectorUtil.rotateAroundAxisY(forward.clone().normalize().setY(0), 90), 90);
			CLUB1.rotateAroundAxisY(-yaw).rotateAroundAxis(VectorUtil.rotateAroundAxisY(forward.clone().normalize().setY(0), 90), 90);
			CLUB2.rotateAroundAxisY(-yaw).rotateAroundAxis(VectorUtil.rotateAroundAxisY(forward.clone().normalize().setY(0), 90), 90);
			DIAMOND1.rotateAroundAxisY(-yaw).rotateAroundAxis(VectorUtil.rotateAroundAxisY(forward.clone().normalize().setY(0), 90), 90);
			DIAMOND2.rotateAroundAxisY(-yaw).rotateAroundAxis(VectorUtil.rotateAroundAxisY(forward.clone().normalize().setY(0), 90), 90);
			JOKER1.rotateAroundAxisY(-yaw).rotateAroundAxis(VectorUtil.rotateAroundAxisY(forward.clone().normalize().setY(0), 90), 90);
			JOKER2.rotateAroundAxisY(-yaw).rotateAroundAxis(VectorUtil.rotateAroundAxisY(forward.clone().normalize().setY(0), 90), 90);
		}
		
		@Override
		protected void run(int i) {
			final Location newLocation = lastLocation.clone().add(forward);
			for (Iterator<Location> iterator = new Iterator<Location>() {
				private final Vector vectorBetween = newLocation.toVector().subtract(lastLocation.toVector()), unit = vectorBetween.clone().normalize().multiply(1.5);
				private final int amount = (int) (vectorBetween.length() / 1.5);
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
					if (suit == 2) {
						ParticleLib.EXPLOSION_HUGE.spawnParticle(entity.getLocation());
						shooter.getWorld().createExplosion(entity.getLocation(), (float) (1 + ((rank + 1) * 0.2)), false, true);
					}
					if (suit == 4) {
						ParticleLib.EXPLOSION_HUGE.spawnParticle(entity.getLocation());
						shooter.getWorld().createExplosion(entity.getLocation(), 3.6f, false, true);
					}
					stop(false);
					return;
				}
				for (Damageable damageable : LocationUtil.getConflictingEntities(Damageable.class, entity.getWorld(), entity.getBoundingBox(), predicate)) {
					if (!shooter.equals(damageable) && !checkhit.contains(damageable)) {
						checkhit.add(damageable);
						if (suit == 0) {
							Damages.damageArrow(damageable, shooter, (float) (4 + ((rank + 1) * 0.3)));
							if (damageable instanceof Player) {
								Player p = (Player) damageable;
								Bleed.apply(getGame().getParticipant(p), TimeUnit.TICKS, (rank + 1) * 10, 10);
							}
						}
						if (suit == 1) {
							if (damageable instanceof Player) {
								Player p = (Player) damageable;
								final EntityRegainHealthEvent event = new EntityRegainHealthEvent(p, ((rank + 1) * 0.3), RegainReason.CUSTOM);
								Bukkit.getPluginManager().callEvent(event);
								if (!event.isCancelled()) {
									Healths.setHealth(p, p.getHealth() + ((rank + 1) * 0.3));
									SoundLib.ENTITY_PLAYER_LEVELUP.playSound(p, 1, 1.2f);
									ParticleLib.HEART.spawnParticle(p.getLocation(), 0.5, 1, 0.5, 10, 1);
								}
							}
							final EntityRegainHealthEvent event = new EntityRegainHealthEvent(shooter, ((rank + 1) * 0.5), RegainReason.CUSTOM);
							Bukkit.getPluginManager().callEvent(event);
							if (!event.isCancelled()) {
								Healths.setHealth((Player) shooter, shooter.getHealth() + ((rank + 1) * 0.5));
								SoundLib.ENTITY_PLAYER_LEVELUP.playSound((Player) shooter, 1, 1.2f);
								ParticleLib.HEART.spawnParticle(shooter.getLocation(), 0.5, 1, 0.5, 10, 1);
							}
							stop(false);
							return;
						}
						if (suit == 2) {
							ParticleLib.EXPLOSION_HUGE.spawnParticle(entity.getLocation());
							shooter.getWorld().createExplosion(entity.getLocation(), (float) (1 + ((rank + 1) * 0.2)), false, true);
							stop(false);
							return;
						}
						if (suit == 3) {
							Damages.damageArrow(damageable, shooter, (float) (3 + ((rank + 1) * 0.7)));
							if (damageable instanceof Player) {
								Player p = (Player) damageable;
								Confusion.apply(getGame().getParticipant(p), TimeUnit.TICKS, (rank + 1) * 10, 10);
							}
							stop(false);
							return;
						}
						if (suit == 4) {
							if (damageable instanceof Player) {
								Player p = (Player) damageable;
								Bleed.apply(getGame().getParticipant(p), TimeUnit.TICKS, 130, 10);
							}
							ParticleLib.EXPLOSION_HUGE.spawnParticle(entity.getLocation());
							shooter.getWorld().createExplosion(entity.getLocation(), 3.6f, false, true);
						}
						if (suit == 5) {
							if (damageable instanceof Player) {
								Player p = (Player) damageable;
								final EntityRegainHealthEvent event = new EntityRegainHealthEvent(p, 3.9, RegainReason.CUSTOM);
								Bukkit.getPluginManager().callEvent(event);
								if (!event.isCancelled()) {
									Healths.setHealth(p, p.getHealth() + 3.9);
									SoundLib.ENTITY_PLAYER_LEVELUP.playSound(p, 1, 1.2f);
									ParticleLib.HEART.spawnParticle(p.getLocation(), 0.5, 1, 0.5, 10, 1);
								}
								Confusion.apply(getGame().getParticipant(p), TimeUnit.TICKS, 130, 10);
							}
							Damages.damageArrow(damageable, shooter, 12.1f);
							final EntityRegainHealthEvent event = new EntityRegainHealthEvent(shooter, 6.5, RegainReason.CUSTOM);
							Bukkit.getPluginManager().callEvent(event);
							if (!event.isCancelled()) {
								Healths.setHealth((Player) shooter, shooter.getHealth() + 6.5);
								SoundLib.ENTITY_PLAYER_LEVELUP.playSound((Player) shooter, 1, 1.2f);
								ParticleLib.HEART.spawnParticle(shooter.getLocation(), 0.5, 1, 0.5, 10, 1);
							}
							stop(false);
							return;
						}
					}
				}
				if (suit == 0) {
					for (Location loc : SPADE1.toLocations(entity.getLocation())) {
						ParticleLib.REDSTONE.spawnParticle(loc, black);
					}
					for (Location loc : SPADE2.toLocations(entity.getLocation())) {
						ParticleLib.REDSTONE.spawnParticle(loc, white);
					}
				}
				if (suit == 1) {
					for (Location loc : HEART1.toLocations(entity.getLocation())) {
						ParticleLib.REDSTONE.spawnParticle(loc, red);
					}
					for (Location loc : HEART2.toLocations(entity.getLocation())) {
						ParticleLib.REDSTONE.spawnParticle(loc, white);
					}
				}
				if (suit == 2) {
					for (Location loc : CLUB1.toLocations(entity.getLocation())) {
						ParticleLib.REDSTONE.spawnParticle(loc, black);
					}
					for (Location loc : CLUB2.toLocations(entity.getLocation())) {
						ParticleLib.REDSTONE.spawnParticle(loc, white);
					}
				}
				if (suit == 3) {
					for (Location loc : DIAMOND1.toLocations(entity.getLocation())) {
						ParticleLib.REDSTONE.spawnParticle(loc, red);
					}
					for (Location loc : DIAMOND2.toLocations(entity.getLocation())) {
						ParticleLib.REDSTONE.spawnParticle(loc, white);
					}
				}
				if (suit == 4) {
					for (Location loc : JOKER1.toLocations(entity.getLocation())) {
						ParticleLib.REDSTONE.spawnParticle(loc, black);
					}
					for (Location loc : JOKER2.toLocations(entity.getLocation())) {
						ParticleLib.REDSTONE.spawnParticle(loc, white);
					}
				}
				if (suit == 5) {
					for (Location loc : JOKER1.toLocations(entity.getLocation())) {
						ParticleLib.REDSTONE.spawnParticle(loc, red);
					}
					for (Location loc : JOKER2.toLocations(entity.getLocation())) {
						ParticleLib.REDSTONE.spawnParticle(loc, white);
					}
				}
			}
			lastLocation = newLocation;
		}
		
		@Override
		protected void onEnd() {
			entity.remove();
			Joker.this.bullet = null;
		}

		@Override
		protected void onSilentEnd() {
			entity.remove();
			Joker.this.bullet = null;
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
				new CardBullet(deflectedPlayer, lastLocation, newDirection, (yaw - 180), suit, rank).start();
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
