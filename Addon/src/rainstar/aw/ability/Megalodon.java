package rainstar.aw.ability;

import daybreak.abilitywar.ability.AbilityBase;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.ability.SubscribeEvent;
import daybreak.abilitywar.config.ability.AbilitySettings.SettingObject;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.manager.effect.Bleed;
import daybreak.abilitywar.game.module.DeathManager;
import daybreak.abilitywar.game.team.interfaces.Teamable;
import daybreak.abilitywar.utils.base.color.RGB;
import daybreak.abilitywar.utils.base.concurrent.SimpleTimer.TaskType;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.base.math.LocationUtil;
import daybreak.abilitywar.utils.base.math.VectorUtil;
import daybreak.abilitywar.utils.base.math.VectorUtil.Vectors;
import daybreak.abilitywar.utils.base.math.geometry.Circle;
import daybreak.abilitywar.utils.base.math.geometry.Crescent;
import daybreak.abilitywar.utils.base.math.geometry.Line;
import daybreak.abilitywar.utils.base.math.geometry.Sphere;
import daybreak.abilitywar.utils.base.minecraft.damage.Damages;
import daybreak.abilitywar.utils.base.minecraft.entity.health.Healths;
import daybreak.abilitywar.utils.base.minecraft.version.ServerVersion;
import daybreak.abilitywar.utils.base.random.Random;
import daybreak.abilitywar.utils.library.MaterialX;
import daybreak.abilitywar.utils.library.ParticleLib;
import daybreak.abilitywar.utils.library.PotionEffects;
import daybreak.abilitywar.utils.library.SoundLib;
import daybreak.google.common.base.Predicate;
import kotlin.ranges.RangesKt;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Note;
import org.bukkit.Note.Tone;
import org.bukkit.block.Block;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.entity.EntityDamageByBlockEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent.RegainReason;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import javax.annotation.Nullable;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@AbilityManifest(
		name = "메갈로돈", rank = Rank.L, species = Species.ANIMAL, explain = {
		"§7패시브 §8- §b킹§f: 액체 내에서 §6힘§f 버프, 웅크릴 때 §b이속§f 버프를 획득합니다.",
		" §c용암 피해§f를 입지 않고 용암 속에서 §d회복 속도 버프§f를 획득합니다.",
		"§7패시브 §8- §c버스트§f: 적에게 근접 피해를 입히거나 피해를 받을 때마다",
		" 대미지에 비례하여 §4버스트 아웃§f의 게이지가 차차 차오릅니다.",
		" 게이지를 가득 채우면, §c12.15초§f간 §4버스트 아웃§f 상태가 되어 주변에 액체를 흩뿌린 뒤",
		" 추가 공격력과 추가 피해를 얻고, 타격한 대상을 §c출혈§f시킵니다.",
		"§b[§7아이디어 제공자§b] §cHSRD"
		},
		summarize = {
		"액체에서 웅크리면 바라보는 방향으로 §b돌진§f합니다. 또한 §6힘§f 버프를 얻습니다.",
		"§c용암 피해§f에 면역을 가지고 용암 속에서 §d회복 속도 버프§f를 획득합니다.",
		"적에게 근접 피해를 입히거나 피해를 받을 때마다 대미지 비례 게이지가 차오릅니다.",
		"게이지가 가득 차면 §4버스트 아웃§f 상태가 되어 추가 공격력을 획득합니다."
		})

public class Megalodon extends AbilityBase {
	
	public Megalodon(Participant participant) {
		super(participant);
	}
	
	private static final int particleCount = 20;
	private static final double yDiff = 0.6 / particleCount;
	private static final Circle circle = Circle.of(0.5, particleCount);
	
	private double damagestack = 0;
	private final double requestDamage = REQUEST_DAMAGE.getValue();
	private final double addDamage = DAMAGE.getValue();
	private double nowDamage = addDamage;
	private BossBar bossBar1 = null;
	private BossBar bossBar2 = null;
	private final DecimalFormat df = new DecimalFormat("0.00");
	private Random random = new Random();
	private final int liquidcount = LIQUID_COUNT.getValue();
	
	private int gauge = 0;
	
	private Note E = Note.natural(0, Tone.E), Fs = Note.sharp(1, Tone.F), G = Note.natural(1, Tone.G),
			As = Note.sharp(1, Tone.A), B = Note.natural(1, Tone.B);
	
	private Set<Projectile> myProjectile = new HashSet<>();
	private Set<Location> blocks = new HashSet<>();
	private Map<Projectile, Double> projectileMap = new HashMap<>();
	
	private final Crescent crescent = Crescent.of(0.85, 20);
	private int particleSide = 60;
	
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
	
	public static final SettingObject<Double> DAMAGE = 
			abilitySettings.new SettingObject<Double>(Megalodon.class, "damage", 5.0,
            "# 추가 피해량", "# 추가 피해량의 1/2만큼 자신도 추가 피해를 입습니다.") {

        @Override
        public boolean condition(Double value) {
            return value >= 0.1; 
        }

    };
    
	public static final SettingObject<Double> REQUEST_DAMAGE = 
			abilitySettings.new SettingObject<Double>(Megalodon.class, "request-damage", 25.0,
            "# 버스트 아웃의 요구 피해량의 총합") {

        @Override
        public boolean condition(Double value) {
            return value >= 0.1;
        }

    };
    
	public static final SettingObject<Integer> RANGE = 
			abilitySettings.new SettingObject<Integer>(Megalodon.class, "range", 5,
            "# 액체를 흩뿌릴 범위") {

        @Override
        public boolean condition(Integer value) {
            return value >= 1;
        }

    };
    
	public static final SettingObject<Integer> LIQUID_COUNT = 
			abilitySettings.new SettingObject<Integer>(Megalodon.class, "liquid-count", 3,
            "# 흩뿌릴 액체의 수") {

        @Override
        public boolean condition(Integer value) {
            return value >= 0;
        }

    };
	
	private static boolean isLava(final Block block) {
		return block.getType().name().endsWith("LAVA");
	}
	
	@Override
	protected void onUpdate(Update update) {
	    if (update == Update.RESTRICTION_CLEAR) {
	    	bossbarUpdate.start();
	    	passive.start();
	    } else if (update == Update.ABILITY_DESTROY) {
			for (Location location : blocks) {
				location.getBlock().setType(Material.AIR);
			}
		}
	}
	
	private final AbilityTimer bossbarUpdate = new AbilityTimer() {
		
    	@Override
    	public void onStart() {
    		bossBar1 = Bukkit.createBossBar("§b버스트 아웃§f: §e" + df.format(damagestack) + " §7/ §e" + df.format(requestDamage), BarColor.WHITE, BarStyle.SOLID);
    		bossBar1.setProgress(damagestack * (1 / requestDamage));
    		bossBar1.addPlayer(getPlayer());
    		if (ServerVersion.getVersion() >= 10) bossBar1.setVisible(true);
    	}
    	
    	@Override
		public void run(int count) {
    		bossBar1.setProgress(damagestack * (1 / requestDamage));
    		Note note = null;
    		switch(gauge) {
    		case 0:
    			if (bossBar1.getProgress() >= 0.15) note = E;
    			break;
    		case 1:
    			if (bossBar1.getProgress() >= 0.35) note = Fs;
    			break;
    		case 2:
    			if (bossBar1.getProgress() >= 0.55) note = G;
    			break;
    		case 3:
    			if (bossBar1.getProgress() >= 0.75) note = As;
    			break;
    		case 4:
    			if (bossBar1.getProgress() >= 0.95) note = B;
    			break;
    		}
    		if (note != null && gauge <= 4) {
        		SoundLib.GUITAR.playInstrument(getPlayer(), note);
        		SoundLib.PIANO.playInstrument(getPlayer(), note);
        		SoundLib.SNARE_DRUM.playInstrument(getPlayer(), note);
        		SoundLib.BASS_DRUM.playInstrument(getPlayer(), note);
        		SoundLib.BASS_GUITAR.playInstrument(getPlayer(), note);
        		gauge++;
    		}
    		if (damagestack * (1 / requestDamage) == 1) {
    			this.stop(false);
    			burstout.start();
    			ParticleLib.CRIT.spawnParticle(getPlayer().getLocation(), 1, 1, 1, 130, 1);
    			SoundLib.ENTITY_ENDER_DRAGON_GROWL.playSound(getPlayer().getLocation(), 1.5f, 1.85f);
				SoundLib.ENTITY_PLAYER_SPLASH.playSound(getPlayer());
				boolean randomvalue = random.nextBoolean();
				for (Location loc : LocationUtil.getRandomLocations(getPlayer().getLocation(), 5, liquidcount)) {
					for (Location locations : Line.between(loc, loc.clone().add(0, 3, 0), 75).toLocations(loc)) {
						if (randomvalue) ParticleLib.WATER_SPLASH.spawnParticle(locations, 0, 0, 0, 1, 1);
						else ParticleLib.DRIP_LAVA.spawnParticle(locations, 0, 0, 0, 1, 1);
					}
					if (loc.getBlock().getType().equals(Material.AIR)) {
						if (randomvalue) loc.getBlock().setType(Material.WATER);
						else loc.getBlock().setType(Material.LAVA);
						blocks.add(loc);
					}
				}
				showHelix(RGB.RED);
    		}
    		bossBar1.setTitle("§b버스트 아웃§f: §e" + df.format(damagestack) + " §7/ §e" + df.format(requestDamage));
    	}
    	
		@Override
		public void onEnd() {
			bossBar1.removeAll();
		}

		@Override
		public void onSilentEnd() {
			bossBar1.removeAll();
		}
		
	}.setPeriod(TimeUnit.TICKS, 1).register();
	
	private final AbilityTimer burstout = new AbilityTimer() {
		
    	@Override
    	public void onStart() {
    		bossBar2 = Bukkit.createBossBar("§c버스트 아웃§7: §e" + df.format(nowDamage * 0.1), BarColor.RED, BarStyle.SOLID);
    		bossBar2.setProgress(1);
    		bossBar2.addPlayer(getPlayer());
    		if (ServerVersion.getVersion() >= 10) bossBar2.setVisible(true);
    		nowDamage = addDamage;
    		gauge = 0;
    		
    		if (getPlayer().getName().equals("HSRD")) particle.start();
    		else if (random.nextInt(10) < 2) particle.start();
    		
    		ParticleLib.EXPLOSION_HUGE.spawnParticle(getPlayer().getLocation());
    		SoundLib.ENTITY_GENERIC_EXPLODE.playSound(getPlayer().getLocation(), 1.5f, 1);
    		
    		for (Entity entity : LocationUtil.getNearbyEntities(Entity.class, getPlayer().getLocation(), 3.5, 3.5, predicate)) {
    			entity.setVelocity(VectorUtil.validateVector(entity.getLocation().toVector().subtract(getPlayer().getLocation().toVector()).normalize().setY(0.5).multiply(0.95)));
    		}
    	}
    	
    	@Override
		public void run(int count) {
    		nowDamage = Math.max(0, nowDamage - (double) addDamage / 243);
    		if (nowDamage == 0) {
    			this.stop(false);
    			damagestack = 0;
    			bossbarUpdate.start();
    		} else bossBar2.setProgress(RangesKt.coerceIn((double) nowDamage / addDamage, 0, 1));
    		bossBar2.setTitle("§c버스트 아웃§7: §e" + df.format(nowDamage));
    	}
    	
		@Override
		public void onEnd() {
			onSilentEnd();
		}

		@Override
		public void onSilentEnd() {
			damagestack = 0;
			bossBar2.removeAll();
		}
		
	}.setPeriod(TimeUnit.TICKS, 1).register();
	
	private AbilityTimer passive = new AbilityTimer() {
	
		@Override
		public void run(int count) {
			if (getPlayer().getLocation().getBlock().isLiquid()) {
				PotionEffects.INCREASE_DAMAGE.addPotionEffect(getPlayer(), 5, 0, true);
    			if (getPlayer().getInventory().getBoots() != null) {
    				ItemStack enchantedboots = getPlayer().getInventory().getBoots();
    				if (enchantedboots.containsEnchantment(Enchantment.DEPTH_STRIDER) && enchantedboots.getEnchantmentLevel(Enchantment.DEPTH_STRIDER) < 3) {
    					enchantedboots.addEnchantment(Enchantment.DEPTH_STRIDER, 3);
    					getPlayer().getInventory().setBoots(enchantedboots);	
    				}
    				if (!enchantedboots.containsEnchantment(Enchantment.DEPTH_STRIDER)) {
    					enchantedboots.addEnchantment(Enchantment.DEPTH_STRIDER, 3);
    					getPlayer().getInventory().setBoots(enchantedboots);
    				}	
    			}
				getPlayer().setRemainingAir(getPlayer().getMaximumAir());
				if (isLava(getPlayer().getLocation().getBlock())) {
					final EntityRegainHealthEvent event = new EntityRegainHealthEvent(getPlayer(), 0.01, RegainReason.CUSTOM);
					Bukkit.getPluginManager().callEvent(event);
					if (!event.isCancelled()) {
						if (!getPlayer().isDead()) Healths.setHealth(getPlayer(), getPlayer().getHealth() + 0.01);
					}
				}	
			} else {
    			if (getPlayer().getInventory().getBoots() != null) {
    				ItemStack enchantedboots = getPlayer().getInventory().getBoots();
    				if (enchantedboots.containsEnchantment(Enchantment.DEPTH_STRIDER)) {
    					enchantedboots.removeEnchantment(Enchantment.DEPTH_STRIDER);
    					getPlayer().getInventory().setBoots(enchantedboots);
    				}
    			}
    		}
		}
		
	}.setPeriod(TimeUnit.TICKS, 1).register();
	
	private AbilityTimer particle = new AbilityTimer(TaskType.NORMAL, 20) {
		
		private Location location;
		
		@Override
		public void onStart() {
			location = getPlayer().getLocation();
		}
		
		@Override
		public void run(int count) {
			Sphere sphere = Sphere.of(count * 0.5, (int) (1 + (count * 0.5)));
			for (Location loc : sphere.toLocations(location)) {
				ParticleLib.DRIP_LAVA.spawnParticle(loc, 0, 0, 0, 1, 0);
			}
		}
		
	}.setPeriod(TimeUnit.TICKS, 1).register();
	
	@SubscribeEvent(onlyRelevant = true)
	public void onPlayerMove(PlayerMoveEvent e) {
		if (getPlayer().getLocation().getBlock().isLiquid() && getPlayer().isSneaking()) {
			getPlayer().setVelocity(getPlayer().getLocation().getDirection().multiply(0.675));
			if (isLava(getPlayer().getLocation().getBlock())) ParticleLib.ITEM_CRACK.spawnParticle(getPlayer().getLocation(), 0, 0, 0, 10, 0.35, MaterialX.REDSTONE_BLOCK);
			else ParticleLib.ITEM_CRACK.spawnParticle(getPlayer().getLocation(), 0, 0, 0, 10, 0.35, MaterialX.LAPIS_BLOCK);
		}
	}
	
	@SubscribeEvent(priority = 4)
	public void onEntityDamage(EntityDamageEvent e) {
		if (e.getEntity().equals(getPlayer())) {
			if (e.getCause().equals(DamageCause.LAVA) || e.getCause().equals(DamageCause.FIRE_TICK) || e.getCause().equals(DamageCause.FIRE)) {
				e.setCancelled(true);
				getPlayer().setFireTicks(0);
			}
		}
	}
	
	@SubscribeEvent
	public void onEntityDamageByBlock(EntityDamageByBlockEvent e) {
		onEntityDamage(e);
	}
	
	@SubscribeEvent
	public void onEntityDamageByEntity(EntityDamageByEntityEvent e) {
		onEntityDamage(e);
		if (burstout.isRunning()) {
			if (Damages.canDamage(e.getEntity(), DamageCause.ENTITY_ATTACK, e.getDamage() + nowDamage) && !e.isCancelled()) {
				if (e.getDamager() instanceof Projectile) {
					Projectile p = (Projectile) e.getDamager();
					if (getPlayer().equals(p.getShooter()) && projectileMap.containsKey(p)) {
						if (!e.getEntity().equals(getPlayer())) {
							e.setDamage(e.getDamage() + projectileMap.get(p));
							if (e.getEntity() instanceof Player) {
								Player player = (Player) e.getEntity();
								if (!getGame().getParticipant(player).hasEffect(Bleed.registration)) {
									Bleed.apply(getGame().getParticipant(player), TimeUnit.TICKS, 200, 15);	
								}
							}
							ParticleLib.ITEM_CRACK.spawnParticle(e.getEntity().getLocation(), 0, 0, 0, 35, 0.35, MaterialX.NETHER_WART);
						}
					}
				} else if (e.getDamager() instanceof Player) {
					if (e.getDamager().equals(getPlayer())) {
						e.setDamage(e.getDamage() + nowDamage);
						if (e.getEntity() instanceof Player) {
							Player player = (Player) e.getEntity();
							if (!getGame().getParticipant(player).hasEffect(Bleed.registration)) {
								Bleed.apply(getGame().getParticipant(player), TimeUnit.TICKS, 200, 15);		
							}
						}
						new CutParticle(getPlayer(), particleSide).start();
						particleSide *= -1;
					}
				}
				if (e.getEntity().equals(getPlayer())) {
					e.setDamage(e.getDamage() + (nowDamage / 2));
				}
			}
		} else {
			if (e.getEntity() instanceof Player) {
				if (e.getEntity().equals(getPlayer()) || e.getDamager().equals(getPlayer())) {
					damagestack = Math.min(requestDamage, damagestack + e.getFinalDamage());
				}	
			}
		}
	}
	
	@SubscribeEvent
	public void onProjectileLaunch(ProjectileLaunchEvent e) {
		if (getPlayer().equals(e.getEntity().getShooter()) && burstout.isRunning()) myProjectile.add(e.getEntity());
	}
	
	@SubscribeEvent
	public void onProjectileHit(ProjectileHitEvent e) {
		if (myProjectile.contains(e.getEntity()) && e.getHitBlock() == null) {
			if (burstout.isRunning()) {
				projectileMap.put(e.getEntity(), nowDamage);	
			}
		}
	}
	
	@SubscribeEvent
	private void onPlayerJoin(final PlayerJoinEvent e) {
		if (getPlayer().getUniqueId().equals(e.getPlayer().getUniqueId())) {
			if (bossBar1 != null) bossBar1.addPlayer(e.getPlayer());
			if (bossBar2 != null) bossBar2.addPlayer(e.getPlayer());
		}
	}

	@SubscribeEvent
	private void onPlayerQuit(final PlayerQuitEvent e) {
		if (getPlayer().getUniqueId().equals(e.getPlayer().getUniqueId())) {
			if (bossBar1 != null) bossBar1.removePlayer(e.getPlayer());
			if (bossBar2 != null) bossBar2.removePlayer(e.getPlayer());
		}
	}
	
	private class CutParticle extends AbilityTimer {

		private final Vector axis;
		private final Vector vector;
		private final Vectors crescentVectors;
		private final Entity entity;

		private CutParticle(Entity entity, double angle) {
			super(1);
			setPeriod(TimeUnit.TICKS, 1);
			this.entity = entity;
			this.axis = VectorUtil.rotateAroundAxis(VectorUtil.rotateAroundAxisY(entity.getLocation().getDirection().setY(0).normalize(), 90), entity.getLocation().getDirection().setY(0).normalize(), angle);
			this.vector = entity.getLocation().getDirection().setY(0).normalize().multiply(0.5);
			this.crescentVectors = crescent.clone()
					.rotateAroundAxisY(-entity.getLocation().getYaw())
					.rotateAroundAxis(entity.getLocation().getDirection().setY(0).normalize(), (180 - angle) % 180)
					.rotateAroundAxis(axis, -15);
		}
		
		@Override
		protected void run(int count) {
			Location baseLoc = entity.getLocation().clone().add(vector).add(0, 1, 0);
			for (Location loc : crescentVectors.toLocations(baseLoc)) {
				ParticleLib.ITEM_CRACK.spawnParticle(loc, 0, 0, 0, 1, 0, MaterialX.MAGMA_BLOCK);
				ParticleLib.DRIP_LAVA.spawnParticle(loc, 0, 0, 0, 1, 0);
			}
			SoundLib.ENTITY_EVOKER_FANGS_ATTACK.playSound(entity.getLocation(), 1, 1.24f);
		}

	}
	
	private void showHelix(final RGB color) {
		new AbilityTimer((particleCount * 3) / 2) {
			int count = 0;

			@Override
			protected void run(int a) {
				for (int i = 0; i < 2; i++) {
					ParticleLib.LAVA.spawnParticle(getPlayer().getLocation().clone().add(circle.get(count % 20)).add(0, count * yDiff, 0));
					ParticleLib.REDSTONE.spawnParticle(getPlayer().getLocation().clone().add(circle.get(count % 20)).add(0, count * yDiff, 0), color);
					count++;
				}
			}
		}.setPeriod(TimeUnit.TICKS, 1).start();
	}

}