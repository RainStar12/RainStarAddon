package RainStarAbility;

import javax.annotation.Nullable;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByBlockEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.entity.EntityRegainHealthEvent.RegainReason;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import RainStarEffect.ElectricShock;
import daybreak.abilitywar.AbilityWar;
import daybreak.abilitywar.ability.AbilityBase;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.SubscribeEvent;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.ability.decorator.ActiveHandler;
import daybreak.abilitywar.config.ability.AbilitySettings.SettingObject;
import daybreak.abilitywar.config.enums.CooldownDecrease;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.AbstractGame.Participant.ActionbarNotification.ActionbarChannel;
import daybreak.abilitywar.game.manager.effect.Rooted;
import daybreak.abilitywar.game.module.DeathManager;
import daybreak.abilitywar.game.team.interfaces.Teamable;
import daybreak.abilitywar.utils.base.Formatter;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.base.math.LocationUtil;
import daybreak.abilitywar.utils.base.math.VectorUtil;
import daybreak.abilitywar.utils.base.math.geometry.Circle;
import daybreak.abilitywar.utils.base.minecraft.damage.Damages;
import daybreak.abilitywar.utils.base.minecraft.entity.health.Healths;
import daybreak.abilitywar.utils.base.minecraft.version.ServerVersion;
import daybreak.abilitywar.utils.base.random.Random;
import daybreak.abilitywar.utils.library.SoundLib;
import daybreak.google.common.base.Predicate;

@AbilityManifest(
		name = "�ٶ���", rank = Rank.S, species = Species.ANIMAL, explain = {
		"��7�нú� ��8- ��6���丮��f: �ڽ��� �ֺ��� 3�� �����Ǵ� ��6���丮��f�� ����� ���ܳ��ϴ�.",
		" ��ȯ ������ 0.7�ʸ���, ��Ÿ���� �� 1.2�ʸ��� �ϳ��� ��ȯ�˴ϴ�.",
		" ��6���丮��f�� �ִ� ��e22����f���� ���� �����մϴ�. ��6���丮��f�� �Ҹ��Ͽ�",
		" ��ų�� ��� �����ϰ�, �� ��ų�� ��Ÿ���� �����մϴ�. $[COOLDOWN]",
		"��7ö�� ��Ŭ�� ��8- ��a�ȳȡ�f: ��� �ִ� ü���� $[HEAL_AMOUNT]%�� ȸ���մϴ�. $[EAT_CONSUME]",
		"��7ö�� ��Ŭ�� ��8- ��b��������f: $[CHANNELING]�ʰ� �̵� �Ұ� ���°� �ǰ�, �� ���� ��ġ����",
		" ���������� ������ ����߷� ������ ������ŵ�ϴ�. $[THUNDER_CONSUME]",
		"��b[��7���̵�� �����ڡ�b] ��eLessso"
		})

public class Squirrel extends AbilityBase implements ActiveHandler {

	public Squirrel(Participant participant) {
		super(participant);
	}
	
	public static final SettingObject<Integer> EAT_CONSUME 
	= abilitySettings.new SettingObject<Integer>(Squirrel.class,
			"eat-consume", 8, "# �ȳ����� �Ҹ�Ǵ� ���丮�� ��") {
		
		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}
		
		@Override
		public String toString() {
			return "��3�Ҹ� ��7: ��f" + getValue();
        }
		
	};
	
	public static final SettingObject<Integer> THUNDER_CONSUME 
	= abilitySettings.new SettingObject<Integer>(Squirrel.class,
			"thunder-consume", 16, "# �������� �Ҹ�Ǵ� ���丮�� ��") {
		
		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}
		
		@Override
		public String toString() {
			return "��3�Ҹ� ��7: ��f" + getValue();
        }
		
	};
	
	public static final SettingObject<Integer> HEAL_AMOUNT 
	= abilitySettings.new SettingObject<Integer>(Squirrel.class,
			"heal-amount", 40, "# ���� ȸ���Ǵ� �� (����: %)") {
		
		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}
		
	};
	
	public static final SettingObject<Double> CHANNELING 
	= abilitySettings.new SettingObject<Double>(Squirrel.class,
			"channeling", 1.5, "# �������� ä�θ� �ð�(�̵� �Ұ� ���ӽð�)") {
		
		@Override
		public boolean condition(Double value) {
			return value >= 0;
		}
		
	};
	
	public static final SettingObject<Integer> COOLDOWN = abilitySettings.new SettingObject<Integer>(
			Squirrel.class, "cooldown", 50, "# ��Ÿ��", "# ��Ÿ�� ���� ȿ���� 50%���� �޽��ϴ�.") {

		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}

		@Override
		public String toString() {
			return Formatter.formatCooldown(getValue());
		}

	};
	
	private final Predicate<Entity> predicate = new Predicate<Entity>() {
		@Override
		public boolean test(Entity entity) {
			if (entity.equals(getPlayer()))
				return false;
			if (entity instanceof Player) {
				if (!getGame().isParticipating(entity.getUniqueId())
						|| (getGame() instanceof DeathManager.Handler && ((DeathManager.Handler) getGame())
								.getDeathManager().isExcluded(entity.getUniqueId()))
						|| !getGame().getParticipant(entity.getUniqueId()).attributes().TARGETABLE.getValue()) {
					return false;
				}
				if (getGame() instanceof Teamable) {
					final Teamable teamGame = (Teamable) getGame();
					final Participant entityParticipant = teamGame.getParticipant(entity.getUniqueId()),
							participant = getParticipant();
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
	
	@Override
	protected void onUpdate(Update update) {
	    if (update == Update.RESTRICTION_CLEAR) {
	    	passive.start();
	    	ac.update("��6���丮��f: ��e" + acornstack);
	    }
	}
	
	private int acornstack = 0;
	
	private static final Circle circle = Circle.of(7, 5);
	
	private ActionbarChannel ac = newActionbarChannel();
	
	private boolean isThunder = false;
	private boolean used = false;
	
	private final Cooldown cool = new Cooldown(COOLDOWN.getValue(), CooldownDecrease._50);
	private final int eatconsume = EAT_CONSUME.getValue();
	private final int thunderconsume = THUNDER_CONSUME.getValue();
	private final int healamount = HEAL_AMOUNT.getValue();
	private final double channeling = CHANNELING.getValue();
	private Random r = new Random();
	
	public boolean ActiveSkill(Material material, ClickType clickType) {
		if (material == Material.IRON_INGOT) {
			if (!cool.isCooldown()) {
				if (clickType == ClickType.LEFT_CLICK) {
					if (acornstack >= thunderconsume) {
						Rooted.apply(getParticipant(), TimeUnit.TICKS, (int) (channeling * 20));
						if (getPlayer().getName().equals("Lessso") || r.nextInt(10) < 2) {
							isThunder = getPlayer().getWorld().hasStorm();
							if (!isThunder) {
								getPlayer().getWorld().setStorm(true);
								getPlayer().getWorld().setThundering(true);
								getPlayer().getWorld().setWeatherDuration(9999);
								getPlayer().getWorld().setThunderDuration(9999);
							}	
							used = true;
						}
						new AbilityTimer((int) (channeling * 20)) {
							
							@Override
							public void onEnd() {
								for (Location loc : circle.toLocations(getPlayer().getLocation()).floor(getPlayer().getLocation().getY())) {
									new Thunders(getPlayer().getLocation(), loc).start();
								}
							}
							
						}.setPeriod(TimeUnit.TICKS, 1).start();
						acornstack = (acornstack - thunderconsume);
						ac.update("��6���丮��f: ��e" + acornstack);
						return cool.start();
					} else getPlayer().sendMessage("��6[��e!��6] ��c���� ���丮�� �����մϴ�.");
				} else if (clickType == ClickType.RIGHT_CLICK) {
					if (acornstack >= eatconsume) {
						double maxHealth = getPlayer().getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue();
						double nowHealth = getPlayer().getHealth();
						double addHealth = (maxHealth - nowHealth) * (healamount * 0.01);
						final EntityRegainHealthEvent event = new EntityRegainHealthEvent(getPlayer(), addHealth, RegainReason.CUSTOM);
						Bukkit.getPluginManager().callEvent(event);
						if (!event.isCancelled()) {
							Healths.setHealth(getPlayer(), nowHealth + addHealth);
							SoundLib.ENTITY_PLAYER_BURP.playSound(getPlayer().getLocation(), 1f, 1.5f);
						}
						acornstack = (acornstack - eatconsume);
						ac.update("��6���丮��f: ��e" + acornstack);
						return cool.start();
					} else getPlayer().sendMessage("��6[��e!��6] ��c���� ���丮�� �����մϴ�.");
				}	
			}
		}
		return false;
	}
	
	@SubscribeEvent(onlyRelevant = true)
	public void onEntityDamage(EntityDamageEvent e) {
		if (e.getCause().equals(DamageCause.LIGHTNING)) {
			e.setCancelled(true);
		}
	}
	
	@SubscribeEvent(onlyRelevant = true)
	public void onEntityDamageByBlock(EntityDamageByBlockEvent e) {
		if (e.getCause().equals(DamageCause.LIGHTNING)) {
			e.setCancelled(true);
		}
	}
	
	@SubscribeEvent(onlyRelevant = true)
	public void onEntityDamageByEntity(EntityDamageByEntityEvent e) {
		if (e.getCause().equals(DamageCause.LIGHTNING)) {
			e.setCancelled(true);
		}
	}
	
	private final AbilityTimer passive = new AbilityTimer() {

		@Override
		public void run(int count) {
			if (acornstack < 22) {
				if (cool.isRunning()) {
					if (count % 24 == 0) {
						Location location = getPlayer().getLocation();
						for (Location loc : LocationUtil.getRandomLocations(LocationUtil.floorY(location), 6, 1)) {
							loc.add(0, 1, 0);
							location = loc;
						}
						new AcornSpawner(location).start();	
					}
				} else {
					if (count % 14 == 0) {
						Location location = getPlayer().getLocation();
						for (Location loc : LocationUtil.getRandomLocations(LocationUtil.floorY(location), 6, 1)) {
							loc.add(0, 1, 0);
							location = loc;
						}
						new AcornSpawner(location).start();	
					}
				}
			}
		}
	
	}.setPeriod(TimeUnit.TICKS, 1).register();
	
	public class AcornSpawner extends AbilityTimer implements Listener {
		
		private final Location location;
		private Item acorn;
		
		public AcornSpawner(Location location) {
			super(TaskType.REVERSE, 60);
			setPeriod(TimeUnit.TICKS, 1);
			this.location = location;
		}
		
		@SuppressWarnings("deprecation")
		@Override
		public void onStart() {
			Bukkit.getPluginManager().registerEvents(this, AbilityWar.getPlugin());
			ItemStack acorntype;
			if (ServerVersion.getVersion() >= 13) acorntype = new ItemStack(Material.COCOA_BEANS);
			else acorntype = new ItemStack(Material.getMaterial("INK_SACK"), 1, (byte) 3);
			acorn = location.getWorld().dropItem(location, acorntype);
		}
		
		@Override
		public void run(int count) {
			if (acornstack >= 22) stop(false);
			if (count <= 10) {
				if (count % 2 == 0) acorn.setGlowing(true);
				else acorn.setGlowing(false);
			}
		}
 		
		@Override
		public void onEnd() {
			onSilentEnd();
		}
		
		@Override
		public void onSilentEnd() {
			HandlerList.unregisterAll(this);
			acorn.remove();
		}
		
		@EventHandler
		public void onEntityPickup(EntityPickupItemEvent e) {
			if (e.getItem().equals(acorn)) {
				e.setCancelled(true);
				if (e.getEntity().equals(getPlayer())) {
					this.stop(false);
					SoundLib.ENTITY_ITEM_PICKUP.playSound(getPlayer().getLocation(), 1, 0.65f);
					acornstack = Math.min(22, acornstack + 1);
					ac.update("��6���丮��f: ��e" + acornstack);
				}
			}
		}
		
	}
	
	public class Thunders extends AbilityTimer implements Listener {
		
		private final Location startLocation;
		private final Location endLocation;
		private Location nowLocation;
		private Vector firstDirection;
		private Vector nowDirection;
		private Random random = new Random();
		private int must = 0;
		
		public Thunders(Location startLocation, Location endLocation) {
			super(30);
			setPeriod(TimeUnit.TICKS, 2);
			this.startLocation = startLocation;
			this.endLocation = endLocation;
		}
		
		@Override
		public void onStart() {
			Bukkit.getPluginManager().registerEvents(this, AbilityWar.getPlugin());
			firstDirection = endLocation.toVector().subtract(startLocation.toVector()).normalize().multiply(1);
			nowLocation = startLocation;
			nowDirection = firstDirection;
		}
		
		@Override
		public void run(int count) {
			if (random.nextInt(5) <= must) {
				must = 0;
				nowDirection = VectorUtil.rotateAroundAxisY(firstDirection, ((random.nextDouble() * 2) - 1) * 90);
			} else must = Math.min(4, must + 1);
			nowLocation.add(nowDirection);
			nowLocation.getWorld().strikeLightning(LocationUtil.floorY(nowLocation));
		}
		
		@EventHandler
		public void onEntityDamageByEntity(EntityDamageByEntityEvent e) {
			if (e.getEntity() instanceof Player) {
				if (e.getCause().equals(DamageCause.LIGHTNING)) {
					if (e.getEntity().equals(getPlayer())) e.setCancelled(true);
					else {
						Player player = (Player) e.getEntity();
						if (Damages.canDamage(player, DamageCause.LIGHTNING, e.getDamage())) {
							Healths.setHealth(player, Math.max(1, player.getHealth() - (e.getDamage() * 0.45)));
							if (predicate.test(player)) ElectricShock.apply(getGame().getParticipant(player), TimeUnit.TICKS, 20);
							e.setDamage(0);
						}
					}
				}	
			}
		}
		
		@Override
		public void onEnd() {
			onSilentEnd();
		}
		
		@Override
		public void onSilentEnd() {
			HandlerList.unregisterAll(this);
			if (used) {
				if (!isThunder) {
					getPlayer().getWorld().setStorm(false);
					getPlayer().getWorld().setThunderDuration(0);
				}
				used = false;
			}
		}
		
	}
	
}
