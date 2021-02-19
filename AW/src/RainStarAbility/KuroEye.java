package RainStarAbility;

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

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.player.PlayerArmorStandManipulateEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.EulerAngle;
import org.bukkit.util.Vector;

import daybreak.abilitywar.AbilityWar;
import daybreak.abilitywar.ability.AbilityBase;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.config.ability.AbilitySettings.SettingObject;
import daybreak.abilitywar.config.enums.CooldownDecrease;
import daybreak.abilitywar.ability.SubscribeEvent;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.AbstractGame.Participant.ActionbarNotification.ActionbarChannel;
import daybreak.abilitywar.game.module.DeathManager;
import daybreak.abilitywar.game.team.interfaces.Teamable;
import daybreak.abilitywar.utils.base.Formatter;
import daybreak.abilitywar.utils.base.color.RGB;
import daybreak.abilitywar.utils.base.concurrent.SimpleTimer.TaskType;
import daybreak.abilitywar.utils.base.math.LocationUtil;
import daybreak.abilitywar.utils.base.math.geometry.Circle;
import daybreak.abilitywar.utils.base.minecraft.nms.NMS;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.base.random.Random;
import daybreak.abilitywar.utils.library.MaterialX;
import daybreak.abilitywar.utils.library.ParticleLib;
import daybreak.abilitywar.utils.library.SoundLib;
import daybreak.google.common.base.Predicate;
import daybreak.google.common.collect.ImmutableSet;

@AbilityManifest(
		name = "쿠로「개안」", rank = Rank.S, species = Species.OTHERS, explain = {
		"마안의 힘을 통제하지 못하는 §8어둠§f 속성의 검사다.",
		"§7패시브 §8- §7이명 §2『§a어둠 군주§2』§f: 어둠에서 자라, 어둠의 힘을 사용하기에",
		" 현재 내가 있는 위치가 어두우면 어두울수록 나의 스킬이 강력해진다.",
		"§7검 들고 F §8- §7비기 §3『§b시간 절단§3』§f: 시간의 틈새에 참격을 끼워넣는 기술로,",
		" 우선 검의 힘을 끌어모으기 위해 10초를 대기한다. 대기 이후에 내 주변으로",
		" 거대한 참격을 날려 검의 힘을 모을 때부터 단 한 번이라도 과거에",
		" 그 위치에서 존재했던 자들의 시간을 뒤틀어 큰 피해를 입힐 수 있다. $[COOLDOWN]",
		" 검의 힘을 모으는 도중 다시 F를 누르는 것으로 즉시 발현도 가능하다.",
		"§7패시브 §8- §4『§c마안 폭주§4』§f: 나는 저주받은 마안을 사용하기에,",
		" 회복을 받을 수 없는 몸이 되었지만 회복을 하려 할 때마다 대신",
		" 회복량의 5%만큼 추가 공격력을 최대 $[MAX_DAMAGE]까지 획득할 수 있다."
		})

public class KuroEye extends AbilityBase {

	public KuroEye(Participant participant) {
		super(participant);
	}
	
	public static final SettingObject<Integer> COOLDOWN 
	= abilitySettings.new SettingObject<Integer>(KuroEye.class,
			"cooldown", 60, "# 시간 절단 쿨타임",
			"# 쿨타임 감소 효과를 최대 50%까지 받습니다.") {
		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}

		@Override
		public String toString() {
			return Formatter.formatCooldown(getValue());
		}
	};
	
	public static final SettingObject<Integer> MAX_DAMAGE 
	= abilitySettings.new SettingObject<Integer>(KuroEye.class,
			"max-damage", 10, "# 최대 추가 피해량") {
		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}
	};
	
	protected void onUpdate(AbilityBase.Update update) {
	    if (update == Update.RESTRICTION_CLEAR) {
	    	ac.update("§c마안 폭주§f: §7" + df.format(stack));
	    }
	}
	
	private final DecimalFormat df = new DecimalFormat("0.00");
	private double stack = 0;
	private final ActionbarChannel ac = newActionbarChannel();
	private static final Set<Material> swords;
	private final Cooldown cool = new Cooldown(COOLDOWN.getValue(), "시간 절단", CooldownDecrease._50);
	private static final Circle circle = Circle.of(6, 70);
	private Map<Player, LogParticle> logMap = new HashMap<>();
	private Set<Player> damaged = new HashSet<>();
	
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
	
	@SubscribeEvent
	public void onEntityRegainHealth(EntityRegainHealthEvent e) {
		if (e.getEntity().equals(getPlayer())) {
			stack = Math.min(MAX_DAMAGE.getValue(), stack + (e.getAmount() * 0.05));
			ac.update("§c마안 폭주§f: §7" + df.format(stack));
			e.setCancelled(true);
		}
	}
	
	@SubscribeEvent
	private void onArmorStandManipulate(PlayerArmorStandManipulateEvent e) {
		if (e.getRightClicked().hasMetadata("TimeCutter")) e.setCancelled(true);
	}
	
	@SubscribeEvent
	public void onEntityDamageByEntity(EntityDamageByEntityEvent e) {
		if (e.getDamager().equals(getPlayer())) {
			e.setDamage(e.getDamage() + stack);
		}
		if (e.getDamager() instanceof Projectile) {
			Projectile projectile = (Projectile) e.getDamager();
			if (getPlayer().equals(projectile.getShooter())) {
				e.setDamage(e.getDamage() + stack);
			}
		}
	}
	
    @SubscribeEvent(onlyRelevant = true)
    public void onPlayerSwapHandItems(PlayerSwapHandItemsEvent e) {
    	if (swords.contains(e.getOffHandItem().getType()) && !cool.isCooldown()) {
    		if (skill.isRunning()) {
    			if (skill.getCount() >= 7) {
    				getPlayer().sendMessage("§4[§c!§4] §f아직 최소한의 검의 힘이 모아지지 않았습니다.");
    				getPlayer().sendMessage("§4[§c!§4] §3최소 대기 시간: 앞으로 §e" + Math.abs(6 - skill.getCount()) + "§f초");
    			} else {
    				skill.stop(false);
    			}
    		} else {
        		skill.start();	
    		}
    		e.setCancelled(true);
    	}
    }
    
	private final AbilityTimer skillcircle = new AbilityTimer(50) {
		
		@Override
		public void run(int count) {
			for (Location loc : circle.toLocations(getPlayer().getLocation()).floor(getPlayer().getLocation().getY())) {
				ParticleLib.REDSTONE.spawnParticle(getPlayer(), loc, RGB.of(77, 77, 77));
			}
		}
		
	}.setPeriod(TimeUnit.TICKS, 4).register();
    
	private final AbilityTimer skill = new AbilityTimer(TaskType.REVERSE, 10) {
		
		@Override
		public void onStart() {
			for (Participant participant : getGame().getParticipants()) {
				if (predicate.test(participant.getPlayer())) {
					new LogParticle(participant.getPlayer()).start();	
				}
			}
			skillcircle.start();
		}
		
		@Override
		public void run(int count) {
			if (count <= 3) {
				getPlayer().sendMessage("§4[§c!§4] §e" + skill.getCount() + "§f초 후 즉시 §3사용§f됩니다!");
				SoundLib.BLOCK_NOTE_BLOCK_SNARE.playSound(getPlayer(), 1, 1.7f);
			}
		}
		
		@Override
		public void onEnd() {
			onSilentEnd();
		}
		
		@Override
		public void onSilentEnd() {
			skillcircle.stop(false);
			SoundLib.ENTITY_EXPERIENCE_ORB_PICKUP.playSound(getPlayer());
			for (Participant participant : getGame().getParticipants()) {
				if (predicate.test(participant.getPlayer()) && logMap.containsKey(participant.getPlayer())) {
					List<Location> locations = logMap.get(participant.getPlayer()).getLog();
					for (int a=0; a<(locations.size() - 1); a++) {
						if (LocationUtil.isInCircle(getPlayer().getLocation(), locations.get(a), 6)) {
							if (!damaged.contains(participant.getPlayer())) {
								ArmorStand armorstand = participant.getPlayer().getWorld().spawn(participant.getPlayer().getEyeLocation().clone().add(0, 20, 0), ArmorStand.class);
				            	armorstand.setRightArmPose(new EulerAngle(Math.toRadians(80), 0, 0));
				            	armorstand.setMetadata("TimeCutter", new FixedMetadataValue(AbilityWar.getPlugin(), null));
				            	armorstand.setVisible(false);
				            	armorstand.getEquipment().setItemInMainHand(new ItemStack(Material.IRON_SWORD));
				            	armorstand.setGravity(true);
				            	NMS.removeBoundingBox(armorstand);
				            	armorstand.setVelocity(new Vector(0, -0.75, 0));
				            	
								ArmorStand clock = locations.get(a).getWorld().spawn(locations.get(a), ArmorStand.class);
								clock.setRightArmPose(new EulerAngle(Math.toRadians(270), 0, 0));
								clock.setMetadata("TimeCutter", new FixedMetadataValue(AbilityWar.getPlugin(), null));
								clock.setVisible(false);
								clock.getEquipment().setItemInMainHand(new ItemStack(Material.WATCH));
								clock.setGravity(false);
				            	NMS.removeBoundingBox(clock);
				    			new BukkitRunnable() {
				    				@Override
				    				public void run() {
				    					participant.getPlayer().damage(10 + (15 - getPlayer().getLocation().getBlock().getLightLevel()) * 0.4, getPlayer());
				    					ParticleLib.ITEM_CRACK.spawnParticle(clock.getLocation().clone().add(0, 1, 0), 0, 0, 0, 10, 0.5f, MaterialX.CLOCK);
				    					ParticleLib.ITEM_CRACK.spawnParticle(participant.getPlayer().getEyeLocation(), 0.4, 0.5, 0.4, 50, 0.35, MaterialX.CLOCK);
				    					SoundLib.ENTITY_ENDER_EYE_DEATH.playSound(clock.getLocation().clone().add(0, 1, 0), 1, 0.7f);
				    					SoundLib.ENTITY_GENERIC_EXPLODE.playSound(participant.getPlayer().getLocation());
				    					clock.remove();
				    					armorstand.remove();
				    				}
		    					}.runTaskLater(AbilityWar.getPlugin(), 30L);
								damaged.add(participant.getPlayer());
							}
						}
					}
					logMap.get(participant.getPlayer()).stop(false);
				}
			}
			damaged.clear();
		}
		
	}.setPeriod(TimeUnit.SECONDS, 1).register();
	
	private class LogParticle extends AbilityTimer {
		
		private List<Location> locations = new ArrayList<>();
		private final Player player;
		private Random random = new Random();
		private final RGB color = RGB.of(random.nextInt(254) + 1, random.nextInt(254) + 1, random.nextInt(254) + 1);
		
		private LogParticle(Player player) {
			super(200);
			setPeriod(TimeUnit.TICKS, 1);
			this.player = player;
			logMap.put(player, this);
		}
		
		public List<Location> getLog() {
			return locations;
		}
		
    	@Override
    	protected void run(int count) {
    		locations.add(player.getLocation());
    		if (count % 4 == 0) {
        		int momentCount = 0;
        		final ListIterator<Location> listIterator = locations.listIterator(locations.size() - 1);
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
    				if (base.getWorld() != previous.getWorld() || base.distanceSquared(previous) > 36) return;
    				for (Iterator<Location> iterator = new Iterator<Location>() {
    					private final Vector vectorBetween = previous.toVector().subtract(base.toVector()), unit = vectorBetween.clone().normalize().multiply(.1);
    					private final int amount = (int) (vectorBetween.length() / 0.25);
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
    					ParticleLib.REDSTONE.spawnParticle(getPlayer(), iterator.next().add(0, 1, 0), color);
    				}
    				listIterator.previous();
    				listIterator.previous();
    			}	
    		}
    	}
    	
    	@Override
    	protected void onEnd() {
    		onSilentEnd();
    	}
    	
    	@Override
    	protected void onSilentEnd() {
    		locations.clear();
    	}

	}
	
	
	
}
