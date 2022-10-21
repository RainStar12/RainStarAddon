package RainStarSynergy;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

import javax.annotation.Nullable;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.data.BlockData;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.entity.EntityDamageByBlockEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent.RegainReason;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.material.MaterialData;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import daybreak.abilitywar.AbilityWar;
import daybreak.abilitywar.ability.AbilityBase;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.SubscribeEvent;
import daybreak.abilitywar.ability.SubscribeEvent.Priority;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.ability.decorator.ActiveHandler;
import daybreak.abilitywar.config.ability.AbilitySettings.SettingObject;
import daybreak.abilitywar.config.enums.CooldownDecrease;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.list.mix.synergy.Synergy;
import daybreak.abilitywar.game.manager.effect.Bleed;
import daybreak.abilitywar.game.module.DeathManager;
import daybreak.abilitywar.game.team.interfaces.Teamable;
import daybreak.abilitywar.utils.base.Formatter;
import daybreak.abilitywar.utils.base.color.RGB;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.base.concurrent.SimpleTimer.TaskType;
import daybreak.abilitywar.utils.base.math.LocationUtil;
import daybreak.abilitywar.utils.base.math.VectorUtil.Vectors;
import daybreak.abilitywar.utils.base.math.geometry.Circle;
import daybreak.abilitywar.utils.base.math.geometry.Crescent;
import daybreak.abilitywar.utils.base.minecraft.entity.health.Healths;
import daybreak.abilitywar.utils.base.minecraft.version.ServerVersion;
import daybreak.abilitywar.utils.library.MaterialX;
import daybreak.abilitywar.utils.library.ParticleLib;
import daybreak.abilitywar.utils.library.SoundLib;
import daybreak.google.common.base.Predicate;
import kotlin.ranges.RangesKt;

@AbilityManifest(
		name = "크로우", rank = Rank.L, species = Species.ANIMAL, explain = {
		"§7철괴 우클릭 §c- §8섀도우 스텝§f: 순간 §b무적 및 투명, 타게팅 불능 상태§f가 되어",
		" 이동 속도가 매우 높아지고 다른 생명체를 지나칠 수 있습니다. $[COOLDOWN]",
		" 이후 지속시간이 끝나면 지나친 생명체들에게 강력한 피해를 입히고 §c출혈§f시키며,",
		" 지속적으로 §c0§f이 될 때까지 감소하는 추가 공격력을 §c12§f만큼 얻습니다.",
		" 피해 입힌 대상 한 명당 §c추가 공격력§f의 감소 속도가 늦춰집니다.",
		"§7능력 지속 중 패시브 §c- §3섀도우 커튼§f: 자신의 주변에 그림자 장막이 깔려",
		" 10칸 내 플레이어를 매우 느리게 만듭니다.",
		"§7패시브 §c- §9섀도우 이터§f: 출혈 중인 대상에게 피해를 입히면 잠시간",
		" 추가 공격력의 감소 속도가 느려지고, §8섀도우 스텝§f의 쿨타임이 줄어듭니다.",
		" 또한 대상의 남은 출혈 지속시간에 비례해 체력을 회복할 수 있습니다."
		})

@SuppressWarnings("deprecation")
public class Crow extends Synergy implements ActiveHandler {
	
	public Crow(Participant participant) {
		super(participant);
	}
	
	private final Predicate<Entity> predicate = new Predicate<Entity>() {
		@Override
		public boolean test(Entity entity) {
			if (onetimeEntity.contains(entity)) return false;
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
	
	public static final SettingObject<Integer> DAMAGE = synergySettings.new SettingObject<Integer>(Crow.class, "damage", 9,
			"# 섀도우 스텝의 기본 대미지") {

		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}

	};
	
	public static final SettingObject<Integer> COOLDOWN = synergySettings.new SettingObject<Integer>(Crow.class, "right-cooldown", 50,
			"# 철괴 우클릭 쿨타임") {

		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}

		@Override
		public String toString() {
			return Formatter.formatCooldown(getValue());
		}

	};
	
	private final Cooldown cool = new Cooldown(COOLDOWN.getValue(), CooldownDecrease._50);
	private final int skillDamage = DAMAGE.getValue();
	
	private double addDamage = 0;
	private int damagingCount = 0;
	private boolean particlerun = false;
	private PotionEffect invisible = new PotionEffect(PotionEffectType.INVISIBILITY, 999, 0, true, false);
	private ItemStack[] armors;
	private ItemStack mainhandItem;
	private ItemStack offhandItem;
	private int timer;
	
	private Set<LivingEntity> onetimeEntity = new HashSet<>();
	private Set<LivingEntity> damagedEntity = new HashSet<>();
	private Map<LivingEntity, Location> livingEntityLocation = new HashMap<>();
	
	private List<Location> log = new ArrayList<>();

	private BossBar bossBar1 = null;
	private BossBar bossBar2 = null;
	private final DecimalFormat df = new DecimalFormat("0.00");
	private static final Circle circle = Circle.of(0.25, 50);
	private final Crescent crescent = Crescent.of(1.25, 10);
	
	private static final RGB color1 = RGB.of(12, 90, 107);
	private static final Circle circle1 = Circle.of(10, 120);
	
	protected void onUpdate(AbilityBase.Update update) {
		if (update == AbilityBase.Update.RESTRICTION_SET || update == AbilityBase.Update.ABILITY_DESTROY) {
			getPlayer().setSprinting(false);
		    getPlayer().setWalkSpeed(0.2F);
		} 
	}
	
	@SubscribeEvent
	public void onPlayerMove(PlayerMoveEvent e) {
		if (e.getPlayer().equals(getPlayer()) && shadowstep.isRunning()) {
			for (LivingEntity livingEntity : LocationUtil.getNearbyEntities(LivingEntity.class, getPlayer().getLocation(), 2, 2, predicate)) {
				if (!damagedEntity.contains(livingEntity)) damagedEntity.add(livingEntity);
				if (!onetimeEntity.contains(livingEntity)) onetimeEntity.add(livingEntity);
				SoundLib.ENTITY_ZOMBIE_ATTACK_IRON_DOOR.playSound(getPlayer().getLocation(), (float) 0.5, 2);
				if (!(livingEntity instanceof Player)) livingEntityLocation.put(livingEntity, livingEntity.getLocation());
				new BukkitRunnable() {
					@Override
					public void run() {
						damagedEntity.remove(livingEntity);
						damagingCount = Math.min(5, damagingCount + 1);
						livingEntity.damage(1.5);
						for (Location loc : circle.toLocations(livingEntity.getLocation()).floor(livingEntity.getLocation().getY())) {
							ParticleLib.SMOKE_LARGE.spawnParticle(loc, 0.15, 0.1, 0.15, 1, 0);
						}
					}	
				}.runTaskLater(AbilityWar.getPlugin(), (long) (20 - (timer * 0.5)));
			}
		}
		if (onetimeEntity.contains(e.getPlayer())) {
			e.setTo(e.getFrom());
		}
	}
	
	@SubscribeEvent(priority = Priority.HIGHEST)
	public void onPlayerSwapHand(PlayerSwapHandItemsEvent e) {
		if (e.getPlayer().equals(getPlayer()) && shadowstep.isRunning()) {
			e.setCancelled(true);
		}
	}
	
	@SubscribeEvent(priority = Priority.HIGHEST)
	public void onSlotChange(PlayerItemHeldEvent e) {
		if (e.getPlayer().equals(getPlayer()) && shadowstep.isRunning()) {
			e.setCancelled(true);
		}
	}
	
	@SubscribeEvent(priority = Priority.HIGHEST)
	public void onPlayerPickup(PlayerPickupItemEvent e) {
		if (e.getPlayer().equals(getPlayer()) && shadowstep.isRunning()) {
			e.setCancelled(true);
		}
	}
	
	@SubscribeEvent(priority = Priority.HIGHEST)
	public void onEntityDamage(EntityDamageEvent e) {
		if (e.getEntity().equals(getPlayer()) && shadowstep.isRunning()) {
			e.setCancelled(true);
		}
		if (damagedEntity.contains(e.getEntity())) {
			e.setCancelled(true);
		}
	}
	
	@SubscribeEvent(priority = Priority.HIGHEST)
	public void onEntityDamageByBlock(EntityDamageByBlockEvent e) {
		onEntityDamage(e);
	}
	
	@SubscribeEvent(priority = Priority.HIGHEST)
	public void onEntityDamageByEntity(EntityDamageByEntityEvent e) {
		onEntityDamage(e);
		if (e.getDamager().equals(getPlayer())) {
			if (shadowstep.isRunning()) {
				e.setCancelled(true);
			} else if (e.getEntity() instanceof Player) {
				Player player = (Player) e.getEntity();
				Participant target = getGame().getParticipant(player);
				if (target.hasEffect(Bleed.registration) && !e.isCancelled()) {
					new CutParticle(180).start();
					int bleedcount = (target.getPrimaryEffect(Bleed.registration).getCount() * target.getPrimaryEffect(Bleed.registration).getPeriod());
					if (!slower.isRunning()) slower.start();
					else slower.setCount(10);
					if (!cooldecreaseCooldown.isRunning()) {
						cool.setCount(cool.getCount() - 2);
						cooldecreaseCooldown.start();
					}
					final EntityRegainHealthEvent event = new EntityRegainHealthEvent(getPlayer(), Math.min(3, bleedcount * 0.05), RegainReason.CUSTOM);
					Bukkit.getPluginManager().callEvent(event);
					if (!event.isCancelled()) {
						Healths.setHealth(getPlayer(), getPlayer().getHealth() + Math.min(3, bleedcount * 0.05));
					}
					SoundLib.ENTITY_GENERIC_EAT.playSound(getPlayer().getLocation(), 1, 0.55f);
				}
			}
			e.setDamage(e.getDamage() + addDamage);
		}
		if (e.getDamager() instanceof Projectile) {
			Projectile projectile = (Projectile) e.getDamager();
			if (getPlayer().equals(projectile.getShooter())) {
				if (shadowstep.isRunning()) {
					e.setCancelled(true);
				}
				e.setDamage(e.getDamage() + addDamage);
			}
		}
	}
	
	@SubscribeEvent
	private void onPlayerJoin(final PlayerJoinEvent e) {
		if (getPlayer().getUniqueId().equals(e.getPlayer().getUniqueId()) && shadowstep.isRunning()) {
			if (bossBar1 != null) bossBar1.addPlayer(e.getPlayer());
		}
	}

	@SubscribeEvent
	private void onPlayerQuit(final PlayerQuitEvent e) {
		if (getPlayer().getUniqueId().equals(e.getPlayer().getUniqueId())) {
			if (bossBar1 != null) bossBar1.removePlayer(e.getPlayer());
		}
	}
	
	public boolean ActiveSkill(Material material, ClickType clicktype) {
	    if (material.equals(Material.IRON_INGOT) && clicktype.equals(ClickType.RIGHT_CLICK) && !shadowstep.isRunning() && !cool.isCooldown()) {
	    	if (livingEntityLocation.size() > 0 || damagedEntity.size() > 0 || particlerun) {
	    		getPlayer().sendMessage("§8[§7!§8] §c아직 능력의 효과가 지속 중입니다.");
	    		return false;
	    	} else {
		    	armors = getPlayer().getInventory().getArmorContents();
		    	mainhandItem = getPlayer().getInventory().getItemInMainHand();
		    	offhandItem = getPlayer().getInventory().getItemInOffHand();
		    	return shadowstep.start();	
	    	}
	    }
	    return false;
	}
	
	private final AbilityTimer slower = new AbilityTimer(10) {
		
	}.setPeriod(TimeUnit.TICKS, 1).register();
	
	private final AbilityTimer cooldecreaseCooldown = new AbilityTimer(20) {
		
	}.setPeriod(TimeUnit.TICKS, 1).register();
	
	private final AbilityTimer damageAdder = new AbilityTimer() {
		
    	@Override
    	public void onStart() {
    		bossBar2 = Bukkit.createBossBar("§c추가 피해량", BarColor.RED, BarStyle.SEGMENTED_12);
    		bossBar2.setProgress(addDamage / 12);
    		bossBar2.addPlayer(getPlayer());
    		if (ServerVersion.getVersion() >= 10) bossBar2.setVisible(true);
    	}
    	
    	@Override
		public void run(int count) {
    		double decreaseSpeed = (6 - damagingCount);
    		if (slower.isRunning()) {
        		addDamage = Math.max(0, (addDamage - (0.0075 + ((addDamage / 12) * 0.025)) * decreaseSpeed));
    		} else {
        		addDamage = Math.max(0, (addDamage - (0.0125 + ((addDamage / 12) * 0.04)) * decreaseSpeed));	
    		}
    		bossBar2.setTitle("§c추가 피해량 §7: §e" + df.format(addDamage));
			bossBar2.setProgress(RangesKt.coerceIn(addDamage / 12, 0, 1));
			if (addDamage <= 0) stop(false);
    	}
    	
		@Override
		public void onEnd() {
			bossBar2.removeAll();
			damagingCount = 0;
		}

		@Override
		public void onSilentEnd() {
			bossBar2.removeAll();
			damagingCount = 0;
		}
		
	}.setPeriod(TimeUnit.TICKS, 1).register();
	
	private final AbilityTimer shadowstep = new AbilityTimer(TaskType.REVERSE, 40) {
		
		@Override
		protected void onStart() {
			if (ServerVersion.getVersion() >= 13) {
				BlockData gravel = MaterialX.GRAVEL.getMaterial().createBlockData();
				BlockData dragonegg = MaterialX.DRAGON_EGG.getMaterial().createBlockData();
				ParticleLib.FALLING_DUST.spawnParticle(getPlayer().getLocation(), 0.4, 1, 0.4, 120, 0, gravel);
				ParticleLib.FALLING_DUST.spawnParticle(getPlayer().getLocation(), 0.4, 1, 0.4, 120, 0, dragonegg);
			} else {
				ParticleLib.FALLING_DUST.spawnParticle(getPlayer().getLocation(), 0.4, 1, 0.4, 120, 0, new MaterialData(Material.GRAVEL));
				ParticleLib.FALLING_DUST.spawnParticle(getPlayer().getLocation(), 0.4, 1, 0.4, 120, 0, new MaterialData(Material.DRAGON_EGG));	
			}
			SoundLib.ENTITY_PIG_SADDLE.playSound(getPlayer().getLocation(), 1, 0.85f);
			bossBar1 = Bukkit.createBossBar("§7섀도우 스텝", BarColor.WHITE, BarStyle.SOLID);
			bossBar1.setProgress(1);
    		bossBar1.addPlayer(getPlayer());
    		if (getPlayer().getInventory().getHelmet() == null && getPlayer().getInventory().getChestplate() == null
    				&& getPlayer().getInventory().getLeggings() == null && getPlayer().getInventory().getBoots() == null) {
    			armors = null;
    		}
    		if (ServerVersion.getVersion() >= 10) bossBar1.setVisible(true);
			getPlayer().getInventory().setArmorContents(null);
			getParticipant().attributes().TARGETABLE.setValue(false);
			getPlayer().getInventory().setItemInMainHand(null);
			getPlayer().getInventory().setItemInOffHand(null);
			timer = 0;
		}

		@Override
		protected void run(int count) {
			for (LivingEntity livingEntity : LocationUtil.getNearbyEntities(LivingEntity.class, getPlayer().getLocation(), 10, 10, predicate)) {
				if (!livingEntity.equals(getPlayer())) livingEntity.setVelocity(livingEntity.getVelocity().multiply(0.35));
			}
			if (count % 2 == 0) {
				log.add(getPlayer().getLocation());
				for (Location loc : circle1.toLocations(getPlayer().getLocation()).floor(getPlayer().getLocation().getY())) {
					ParticleLib.REDSTONE.spawnParticle(getPlayer(), loc, color1);
				}
			}
			timer++;
			bossBar1.setProgress(RangesKt.coerceIn((double) count / 40, 0, 1));
		    getPlayer().setWalkSpeed(0.7F);
		    getPlayer().addPotionEffect(invisible);
			for (LivingEntity livingEntity : getPlayer().getWorld().getLivingEntities()) {
				if (livingEntityLocation.containsKey(livingEntity)) {
					livingEntity.teleport(livingEntityLocation.get(livingEntity));
				}
			}
			if (count == 20) {
				new LogParticle().start();
			}
		}
		
		@Override
		protected void onEnd() {
			onSilentEnd();
		}
		
		@Override
		protected void onSilentEnd() {
			getPlayer().setSprinting(false);
			getPlayer().setWalkSpeed(0.2F);
			if (armors != null) getPlayer().getInventory().setArmorContents(armors);
			getPlayer().removePotionEffect(PotionEffectType.INVISIBILITY);
			if (!mainhandItem.getType().equals(Material.AIR)) getPlayer().getInventory().setItemInMainHand(mainhandItem);
			if (!offhandItem.getType().equals(Material.AIR)) getPlayer().getInventory().setItemInOffHand(offhandItem);
			getParticipant().attributes().TARGETABLE.setValue(true);
			bossBar1.removeAll();
			SoundLib.ENTITY_BAT_TAKEOFF.playSound(getPlayer().getLocation(), 1, 0.7f);
			ParticleLib.EXPLOSION_NORMAL.spawnParticle(getPlayer().getLocation(), 0.1, 0.1, 0.1, 350, 0.1f);
			cool.start();
		}
		
	}.setPeriod(TimeUnit.TICKS, 1).register();
	
	private class LogParticle extends AbilityTimer {
		
		private final RGB color = RGB.BLACK;
		private List<Location> getlog = new ArrayList<>();
		
		private LogParticle() {
			super(20);
			setPeriod(TimeUnit.TICKS, 1);
		}
		
		@Override
		protected void onStart() {
			particlerun = true;
		}
		
    	@Override
    	protected void run(int count) {
    		getlog.add(log.get(0));
    		log.remove(0);
        	int momentCount = 0;
        	final ListIterator<Location> listIterator = getlog.listIterator(getlog.size() - 1);
        	if (!listIterator.hasPrevious()) return;
    		listIterator.previous();
    		while (listIterator.hasPrevious()) {
    			final Location base;
    			if (momentCount == 1) {
    				base = getPlayer().getLocation();
    				listIterator.previous();
    			} else {
    				base = listIterator.previous();
    			}
    			listIterator.next();
    			final Location previous = listIterator.next();
    			for (Iterator<Location> iterator = new Iterator<Location>() {
    				private final Vector vectorBetween = previous.toVector().subtract(base.toVector()), unit = vectorBetween.clone().normalize().multiply(.1);
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
    					return base.clone().add(unit.clone().multiply(cursor));
    				}
    			}; iterator.hasNext(); ) {
    				ParticleLib.REDSTONE.spawnParticle(getPlayer(), iterator.next().add(0, 0.1, 0), color);
    			}
    			listIterator.previous();
    			listIterator.previous();
    		}	
    	}
    	
    	@Override
    	protected void onEnd() {
    		onSilentEnd();
    	}
    	
    	@Override
    	protected void onSilentEnd() {
        	int momentCount = 0;
        	final ListIterator<Location> listIterator = getlog.listIterator(getlog.size() - 1);
        	if (!listIterator.hasPrevious()) return;
    		listIterator.previous();
    		while (listIterator.hasPrevious()) {
    			final Location base;
    			if (momentCount == 1) {
    				base = getPlayer().getLocation();
    				listIterator.previous();
    			} else {
    				base = listIterator.previous();
    			}
    			listIterator.next();
    			final Location previous = listIterator.next();
    			for (Iterator<Location> iterator = new Iterator<Location>() {
    				private final Vector vectorBetween = previous.toVector().subtract(base.toVector()), unit = vectorBetween.clone().normalize().multiply(.1);
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
    					return base.clone().add(unit.clone().multiply(cursor));
    				}
    			}; iterator.hasNext(); ) {
    				ParticleLib.REDSTONE.spawnParticle(getPlayer(), iterator.next().add(0, 0.1, 0), RGB.RED);
    			}
    			listIterator.previous();
    			listIterator.previous();
    		}
			if (damagingCount > 0) addDamage = 12;
			else addDamage = 0;
			if (!damageAdder.isRunning()) damageAdder.start();
    		SoundLib.BLOCK_ANVIL_LAND.playSound(getPlayer().getLocation(), 1, 1.77f);
    		for (LivingEntity livingEntity : getPlayer().getWorld().getLivingEntities()) {
    			if (onetimeEntity.contains(livingEntity)) {
    				livingEntity.damage(skillDamage, getPlayer());
    				ParticleLib.ITEM_CRACK.spawnParticle(livingEntity.getLocation(), .5f, 1f, .5f, 100, 0.35, MaterialX.REDSTONE);
					Bleed.apply(getGame(), livingEntity, TimeUnit.TICKS, 80);
    			}
    		}
			livingEntityLocation.clear();
			onetimeEntity.clear();
    		particlerun = false;
    		log.clear();
    		getlog.clear();
    	}

	}
	
	private class CutParticle extends AbilityTimer {

		private final Vector vector;
		private final Vectors crescentVectors;

		private CutParticle(double angle) {
			super(4);
			setPeriod(TimeUnit.TICKS, 1);
			this.vector = getPlayer().getLocation().getDirection().setY(0).normalize().multiply(0.5);
			this.crescentVectors = crescent.clone()
					.rotateAroundAxisY(-getPlayer().getLocation().getYaw())
					.rotateAroundAxis(getPlayer().getLocation().getDirection().setY(0).normalize(), (180 - angle) % 180);
		}

		@Override
		protected void run(int count) {
			Location baseLoc = getPlayer().getLocation().clone().add(vector).add(0, 1, 0);
			for (Location loc : crescentVectors.toLocations(baseLoc)) {
				ParticleLib.SMOKE_LARGE.spawnParticle(loc, 0, 0, 0, 1, 0);
			}
		}

	}

}