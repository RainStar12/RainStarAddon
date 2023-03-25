package rainstar.abilitywar.ability;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.annotation.Nullable;

import org.bukkit.Bukkit;
import org.bukkit.DyeColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Wolf;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent.RegainReason;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.util.Vector;

import daybreak.abilitywar.AbilityWar;
import daybreak.abilitywar.ability.AbilityBase;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.ability.SubscribeEvent;
import daybreak.abilitywar.ability.decorator.ActiveHandler;
import daybreak.abilitywar.config.ability.AbilitySettings.SettingObject;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.manager.effect.Bleed;
import daybreak.abilitywar.game.manager.effect.Stun;
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
import daybreak.abilitywar.utils.base.minecraft.entity.health.Healths;
import daybreak.abilitywar.utils.base.minecraft.item.builder.ItemBuilder;
import daybreak.abilitywar.utils.base.random.Random;
import daybreak.abilitywar.utils.library.MaterialX;
import daybreak.abilitywar.utils.library.ParticleLib;
import daybreak.abilitywar.utils.library.SoundLib;
import daybreak.google.common.base.Predicate;

@AbilityManifest(
		name = "사냥개", rank = Rank.S, species = Species.ANIMAL, explain = {
		"§7철괴 우클릭 §8- §c사냥개를 풀 시간§f: §6$[DURATION_A] 혹은 $[DURATION_B]초§f간 §e$[COUNT]§f마리의 사냥개를 소환합니다.",
		" 사냥개는 §c빨간색§f, §a녹색§f, §b하늘색§f 목걸이에 따른 별개의 능력치로",
		" 적을 사냥하러 나섭니다. $[COOLDOWN]",
		" 이 사냥개가 물어뜯는 적은 $[DAMAGE]의 피해를 입고 $[STUN]틱간 §e§n기절§f합니다.",
		"§7철괴 좌클릭 §8- §c힘 재기§f: 각 사냥개의 스탯을 볼 수 있는 GUI를 오픈합니다.",
		"§b[§7아이디어 제공자§b] §6DUCKGAE"
		},
		summarize = {
		"§7철괴 우클릭으로§f §6사냥개§f를 소환합니다. 사냥개는 §c3§a가§b지§f 타입이 존재합니다.",
		"타입에 따른 §6사냥개§f의 능력치는 §7철괴 좌클릭§f을 통해서 볼 수 있습니다."
		})

public class HuntingDog extends AbilityBase implements ActiveHandler {
	
	public HuntingDog(Participant participant) {
		super(participant);
	}
	
	private static final SettingObject<Integer> COOLDOWN = abilitySettings.new SettingObject<Integer>(HuntingDog.class, "cooldown", 110, "# 쿨타임") {

		@Override
		public boolean condition(Integer arg0) {
			return arg0 >= 0;
		}

		@Override
		public String toString() {
			return Formatter.formatCooldown(getValue());
		}

	};
	
	private static final SettingObject<Integer> DURATION_A = abilitySettings.new SettingObject<Integer>(HuntingDog.class, "duration-a", 12, "# 지속 시간") {

		@Override
		public boolean condition(Integer arg0) {
			return arg0 >= 1;
		}

	};

	private static final SettingObject<Integer> DURATION_B = abilitySettings.new SettingObject<Integer>(HuntingDog.class, "duration-b", 14, "# 지속 시간") {

		@Override
		public boolean condition(Integer arg0) {
			return arg0 >= 1;
		}

	};
	
	private static final SettingObject<Integer> COUNT = abilitySettings.new SettingObject<Integer>(HuntingDog.class, "count", 7, "# 소환하는 사냥개의 수") {

		@Override
		public boolean condition(Integer arg0) {
			return arg0 >= 1;
		}

	};
	
	private static final SettingObject<Integer> DAMAGE = abilitySettings.new SettingObject<Integer>(HuntingDog.class, "damage", 6, "# 사냥개의 기본 대미지") {

		@Override
		public boolean condition(Integer arg0) {
			return arg0 >= 1;
		}

	};
	
	private static final SettingObject<Integer> HEALTH = abilitySettings.new SettingObject<Integer>(HuntingDog.class, "health", 20, "# 사냥개의 기본 체력") {

		@Override
		public boolean condition(Integer arg0) {
			return arg0 >= 1;
		}

	};
	
	private static final SettingObject<Integer> STUN = abilitySettings.new SettingObject<Integer>(HuntingDog.class, "stun", 2, "# 사냥개 공격의 기절 시간(단위: 틱)") {

		@Override
		public boolean condition(Integer arg0) {
			return arg0 >= 1;
		}

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
	
	private Random random = new Random();
	
	private final Set<Wolf> dogs = new HashSet<>();
	
	private Player targetPlayer;
	
	private final int dogCount = COUNT.getValue();
	private final int dogHealth = HEALTH.getValue();
	private final int stunduration = STUN.getValue();
	private final int dogDamage = DAMAGE.getValue();
	
	private long worldtime = 0;
	
	private final Crescent crescent = Crescent.of(0.85, 20);
	private int particleSide = 15;

	private List<Location> locations = new ArrayList<>();
	
	@Override
	protected void onUpdate(Update update) {
		if (update == Update.RESTRICTION_CLEAR) {
			fastNight.start();
		}
	}
	
	private static boolean isLongDay(long worldtime) {
		return worldtime < 12000;
	}
	
	private static boolean isNight(long worldtime) {
		return worldtime > 17000 && worldtime < 23850;
	}
	
	private static boolean isRealNight(long worldtime) {
		return worldtime > 12300 && worldtime < 23850;
	}
	
	private final Cooldown cooldown = new Cooldown(COOLDOWN.getValue());
	
	private final AbilityTimer fastNight = new AbilityTimer() {
		
    	@Override
		public void run(int count) {
    		if (!timeControl.isRunning()) {
        		if (isRealNight(getPlayer().getWorld().getTime())) {
        			getPlayer().getWorld().setTime(getPlayer().getWorld().getTime() + 2);
        		}	
    		}
    	}
		
	}.setPeriod(TimeUnit.TICKS, 1).register();
	
	private final AbilityTimer timeControl = new AbilityTimer() {
    	
		private boolean summondog = false;
		private Location startLocation;
		
		private int stacks = 0;
		private boolean turns = true;
		
		private RGB gradation;
		
		private final RGB gradation1 = RGB.of(192, 1, 1), gradation2 = RGB.of(201, 27, 2),
				gradation3 = RGB.of(210, 54, 5), gradation4 = RGB.of(219, 81, 8), gradation5 = RGB.of(228, 108, 10);
		
		@SuppressWarnings("serial")
		private List<RGB> gradations = new ArrayList<RGB>() {
			{
				add(gradation1);
				add(gradation2);
				add(gradation3);
				add(gradation4);
				add(gradation5);
			}
		};
		
    	@Override
    	public void onStart() {
			worldtime = getPlayer().getWorld().getTime();
			startLocation = getPlayer().getLocation();
			SoundLib.ENTITY_WOLF_HOWL.playSound(getPlayer().getLocation(), 1, 0.9f);
			for (Location randomloc : LocationUtil.getRandomLocations(getPlayer().getLocation(), 7.5, dogCount)) {
				locations.add(randomloc.clone().add(0, 1, 0));
			}
			if (isLongDay(getPlayer().getWorld().getTime())) {
				if (getPlayer().getName().equals("DUCKGAE")) {
					summondog = true;
				} else if (random.nextInt(10) <= 2) {
					summondog = true;	
				}
			}
    	}
    	
    	@Override
		public void run(int count) {
    		if (isNight(getPlayer().getWorld().getTime())) {
    			stop(false);
    		} else {
    			getPlayer().getWorld().setTime(getPlayer().getWorld().getTime() + 250);
    		}
    		
			if (turns) stacks++;
			else stacks--;
			if (stacks % (gradations.size() - 1) == 0) turns = !turns;
			gradation = gradations.get(stacks);
    		
    		Location addLocation = getPlayer().getLocation().subtract(startLocation);
    		for (Location location : locations) {
    			if (addLocation != null) location.add(addLocation);
    			if (summondog) {
        			if (count <= 20) ParticleLib.REDSTONE.spawnParticle(getPlayer().getLocation().clone().add(0, 1, 0).add(Line.vectorAt(getPlayer().getLocation().clone().add(0, 1, 0), location.clone(), 20, count)), gradation);
        			else ParticleLib.REDSTONE.spawnParticle(location, gradation);
    			}
    		}
			
    		startLocation = getPlayer().getLocation();
    	}
    	
    	@Override
    	public void onEnd() {
    		if (summondog) {
    			for (Location location : locations) {
    				getPlayer().getWorld().strikeLightningEffect(location);
    			}
    		}
    		skill.start();
    	}
    	
    	@Override
    	public void onSilentEnd() {
    		if (summondog) {
    			for (Location location : locations) {
    				getPlayer().getWorld().strikeLightningEffect(location);
    			}
    		}
    	}
    
    }.setPeriod(TimeUnit.TICKS, 1).register();
	
	private final Duration skill = new Duration(20, cooldown) {
		
		@Override
		protected void onDurationStart() {
			switch(random.nextInt(2)) {
			case 0:
				skill.setCount(DURATION_A.getValue() * 20);
				break;
			case 1:
				skill.setCount(DURATION_B.getValue() * 20);
				break;
			}
			SoundLib.BLOCK_END_PORTAL_SPAWN.playSound(getPlayer().getLocation(), 1, 1.25f);
			final double criterionY = getPlayer().getLocation().getY();
			for (Location location : locations) {
				Wolf wolf = getPlayer().getWorld().spawn(LocationUtil.floorY(location, criterionY), Wolf.class);
				ParticleLib.CLOUD.spawnParticle(wolf.getLocation(), 0.25, 0.15, 0.25, 45, 0.1);
				wolf.setOwner(getPlayer());
				wolf.setAngry(true);
				switch(random.nextInt(3)) {
				case 0:
					wolf.setCollarColor(DyeColor.RED);
					break;
				case 1:
					wolf.setCollarColor(DyeColor.LIME);
					break;
				case 2:
					wolf.setCollarColor(DyeColor.LIGHT_BLUE);
					break;
				}
				if (wolf.getCollarColor().equals(DyeColor.RED)) {
					wolf.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).setBaseValue(0.475);
					wolf.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(Math.max(1, dogHealth * 0.4));
					wolf.setHealth(Math.max(1, dogHealth * 0.4));
					wolf.setAdult();
				} else if (wolf.getCollarColor().equals(DyeColor.LIME)) {
					wolf.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).setBaseValue(0.325);
					wolf.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(Math.max(1, dogHealth * 2.15));
					wolf.setHealth(Math.max(1, dogHealth * 2.15));
					wolf.setAdult();
				} else if (wolf.getCollarColor().equals(DyeColor.LIGHT_BLUE)) {
					wolf.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).setBaseValue(0.645);
					wolf.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(Math.max(1, dogHealth));
					wolf.setHealth(Math.max(1, dogHealth));
					wolf.setAdult();
				}
				dogs.add(wolf);
			}
			
			locations.clear();
		}
		
		@Override
		protected void onDurationProcess(int count) {
			getPlayer().getWorld().setTime(17500);
			for (Wolf wolf : dogs) {
				wolf.setAngry(true);
				wolf.getAttribute(Attribute.GENERIC_FOLLOW_RANGE).setBaseValue(9999999);
				if (wolf.getCollarColor().equals(DyeColor.RED)) {
					if (targetPlayer != null) { 
						wolf.setTarget(targetPlayer);
					} else {
						wolf.setTarget(LocationUtil.getNearestEntity(Player.class, getPlayer().getLocation(), predicate));
					}
					ParticleLib.REDSTONE.spawnParticle(wolf.getLocation().clone().add(0, 1, 0), RGB.RED);
				} else if (wolf.getCollarColor().equals(DyeColor.LIME)) {
					wolf.setTarget(LocationUtil.getNearestEntity(Player.class, wolf.getLocation(), predicate));
					ParticleLib.REDSTONE.spawnParticle(wolf.getLocation().clone().add(0, 1, 0), RGB.LIME);
				} else if (wolf.getCollarColor().equals(DyeColor.LIGHT_BLUE)) {
					wolf.setTarget(LocationUtil.getNearestEntity(Player.class, getPlayer().getLocation(), predicate));
					ParticleLib.REDSTONE.spawnParticle(wolf.getLocation().clone().add(0, 1, 0), RGB.TEAL);
				}
			}
		}
		
		@Override
		protected void onDurationEnd() {
			onDurationSilentEnd();
		}
		
		@Override
		protected void onDurationSilentEnd() {
			getPlayer().getWorld().setTime(worldtime);
			for (Wolf wolf : dogs) {
				ParticleLib.CLOUD.spawnParticle(wolf.getLocation(), 0.25, 0.15, 0.25, 30, 0.1);
				SoundLib.ENTITY_WOLF_WHINE.playSound(wolf.getLocation());
				wolf.remove();
			}
			dogs.clear();
		}
		
	}.setPeriod(TimeUnit.TICKS, 1);
	
	public boolean ActiveSkill(Material material, ClickType clicktype) {
	    if (material.equals(Material.IRON_INGOT)) {
	    	if (clicktype.equals(ClickType.LEFT_CLICK)) {
	    		new DogGui().start();
	    	} else if (!cooldown.isCooldown() && !skill.isDuration() && !timeControl.isRunning()) {
	    		return timeControl.start();
	    	}
	    }
		return false;
	}

	@SubscribeEvent
	public void onEntityDamageByEntity(EntityDamageByEntityEvent e) {
		if (dogs.contains(e.getEntity()) && e.getDamager().equals(getPlayer())) {
			e.setCancelled(true);
		}
		if (e.getEntity().equals(getPlayer())) {
			if (e.getDamager() instanceof Projectile) {
				Projectile projectile = (Projectile) e.getDamager();
				if (!getPlayer().equals(projectile.getShooter())) {
					if (projectile.getShooter() != null) {
						if (projectile.getShooter() instanceof Player) {
							targetPlayer = (Player) projectile.getShooter();
						}
					}
				}
			} else if (e.getDamager() instanceof Player) {
				targetPlayer = (Player) e.getDamager();
			}
		}
		if (dogs.contains(e.getDamager()) && e.getEntity() instanceof Player) {
			Wolf wolf = (Wolf) e.getDamager();
			if (!e.isCancelled() && !e.getEntity().isInvulnerable()) {
				RGB rgb = null;
				if (wolf.getCollarColor().equals(DyeColor.RED)) {
					e.setDamage(dogDamage * 1.65);
					Stun.apply(getGame().getParticipant((Player) e.getEntity()), TimeUnit.TICKS, stunduration);
					if (!getGame().getParticipant((Player) e.getEntity()).hasEffect(Bleed.registration)) Bleed.apply(getGame().getParticipant((Player) e.getEntity()), TimeUnit.TICKS, 20);
					rgb = RGB.of(168, 13, 20);
				} else if (wolf.getCollarColor().equals(DyeColor.LIME)) {
					e.setDamage(dogDamage);
					final EntityRegainHealthEvent event = new EntityRegainHealthEvent(getPlayer(), e.getFinalDamage() * 0.5, RegainReason.CUSTOM);
					Bukkit.getPluginManager().callEvent(event);
					if (!event.isCancelled()) {
						Healths.setHealth(getPlayer(), getPlayer().getHealth() + event.getAmount());
					}
					Stun.apply(getGame().getParticipant((Player) e.getEntity()), TimeUnit.TICKS, stunduration);
					rgb = RGB.of(1, 113, 1);
				} else if (wolf.getCollarColor().equals(DyeColor.LIGHT_BLUE)) {
					e.setDamage(dogDamage * 0.6);
					Stun.apply(getGame().getParticipant((Player) e.getEntity()), TimeUnit.TICKS, (int) (stunduration * 1.5));
					rgb = RGB.of(1, 94, 138);
				}
				new CutParticle(wolf, particleSide, rgb).start();
			}
		}
	}
	
	public class DogGui extends AbilityTimer implements Listener {
		
		private final ItemStack NULL = (new ItemBuilder(MaterialX.GRAY_STAINED_GLASS_PANE)).displayName(" ").build();
		private final ItemStack RED = (new ItemBuilder(MaterialX.RED_WOOL)).displayName("§c빨간색 목걸이의 사냥개").build();
		private final ItemStack GREEN = (new ItemBuilder(MaterialX.LIME_WOOL)).displayName("§a초록색 목걸이의 사냥개").build();
		private final ItemStack BLUE = (new ItemBuilder(MaterialX.LIGHT_BLUE_WOOL)).displayName("§b파란색 목걸이의 사냥개").build();
		private final ItemStack EXIT = (new ItemBuilder(MaterialX.SPRUCE_DOOR)).displayName("§3나가기").build();
		
		private final Inventory gui;
		
		public DogGui() {
			super(TaskType.REVERSE, 99999);
			setPeriod(TimeUnit.TICKS, 1);
			gui = Bukkit.createInventory(null, InventoryType.HOPPER, "§c사냥개 §0능력치");
			
			ItemMeta redmeta = RED.getItemMeta();
			final List<String> redlore = new ArrayList<>();
			redlore.add("§c공격력§7: §a■■■■■");
			redlore.add("§d체력§7  : §a■§8□□□□");
			redlore.add("§b속도§7  : §a■■■§8□□");
			redlore.add("§8====================");
			redlore.add("§3우선 타게팅 대상§7: §f가장 마지막으로 주인을 공격한 적");
			redlore.add("§2특수 능력§7: §f적을 피해입힐 때 §c§n출혈§f효과를 부여함");
			redmeta.setLore(redlore);
			RED.setItemMeta(redmeta);
			
			ItemMeta greenmeta = GREEN.getItemMeta();
			final List<String> greenlore = new ArrayList<>();
			greenlore.add("§c공격력§7: §a■■■§8□□");
			greenlore.add("§d체력§7  : §a■■■■■");
			greenlore.add("§b속도§7  : §a■§8□□□□");
			greenlore.add("§8====================");
			greenlore.add("§3우선 타게팅 대상§7: §f자신으로부터 가장 가까운 적");
			greenlore.add("§2특수 능력§7: §f적에게 입힌 피해에 비례해 주인을 §a회복§f시킴");
			greenmeta.setLore(greenlore);
			GREEN.setItemMeta(greenmeta);
			
			ItemMeta bluemeta = BLUE.getItemMeta();
			final List<String> bluelore = new ArrayList<>();
			bluelore.add("§c공격력§7: §a■■§8□□□");
			bluelore.add("§d체력§7  : §a■■■§8□□");
			bluelore.add("§b속도§7  : §a■■■■§8□");
			bluelore.add("§8====================");
			bluelore.add("§3우선 타게팅 대상§7: §f주인에게 가장 가까이 있는 적");
			bluelore.add("§2특수 능력§7: §f피해를 입힐 때 적이 §b§n기절§f하는 시간이 2배 길어짐");
			bluemeta.setLore(bluelore);
			BLUE.setItemMeta(bluemeta);
		}
		
		protected void onStart() {
			Bukkit.getPluginManager().registerEvents(this, AbilityWar.getPlugin());
			getPlayer().openInventory(gui);
		}
		
		@Override
		protected void run(int arg0) {
			gui.setItem(0, RED);
			gui.setItem(1, GREEN);
			gui.setItem(2, BLUE);
			gui.setItem(3, NULL);
			gui.setItem(4, EXIT);
		}
		
		@Override
		protected void onEnd() {
			onSilentEnd();
		}
		
		@Override
		protected void onSilentEnd() {
			HandlerList.unregisterAll(this);
			getPlayer().closeInventory();
		}

		@EventHandler
		private void onInventoryClose(InventoryCloseEvent e) {
			if (e.getInventory().equals(gui)) stop(false);
		}

		@EventHandler
		private void onQuit(PlayerQuitEvent e) {
			if (e.getPlayer().getUniqueId().equals(getPlayer().getUniqueId())) stop(false);
		}

		@EventHandler
		private void onInventoryClick(InventoryClickEvent e) {
			if (e.getInventory().equals(gui)) {
				if (e.getSlot() <= 3) {
					e.setCancelled(true);
				} else {
					getPlayer().closeInventory();
				}
			}
		}
		
	}
	
	private class CutParticle extends AbilityTimer {

		private final Vector axis;
		private final Vector vector;
		private final Vectors crescentVectors;
		private RGB color;
		private final Entity entity;

		private CutParticle(Entity entity, double angle, RGB color) {
			super(1);
			setPeriod(TimeUnit.TICKS, 1);
			this.color = color;
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
			Location baseLoc = entity.getLocation().clone().add(vector).add(0, 0.45, 0);
			for (Location loc : crescentVectors.toLocations(baseLoc)) {
				ParticleLib.REDSTONE.spawnParticle(loc, color);
			}
			SoundLib.ENTITY_PLAYER_ATTACK_SWEEP.playSound(baseLoc, 0.75f, 0.5f);
		}

	}
	
}