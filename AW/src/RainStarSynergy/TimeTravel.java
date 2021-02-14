package RainStarSynergy;

import java.util.Collection;

import javax.annotation.Nullable;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByBlockEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.EulerAngle;
import org.bukkit.util.Vector;

import daybreak.abilitywar.AbilityWar;
import daybreak.abilitywar.ability.AbilityBase;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.SubscribeEvent;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.ability.decorator.ActiveHandler;
import daybreak.abilitywar.config.ability.AbilitySettings.SettingObject;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.AbstractGame.Participant.ActionbarNotification.ActionbarChannel;
import daybreak.abilitywar.game.list.mix.synergy.Synergy;
import daybreak.abilitywar.game.module.DeathManager;
import daybreak.abilitywar.game.team.interfaces.Teamable;
import daybreak.abilitywar.utils.base.Formatter;
import daybreak.abilitywar.utils.base.color.RGB;
import daybreak.abilitywar.utils.base.concurrent.SimpleTimer.TaskType;
import daybreak.abilitywar.utils.base.math.FastMath;
import daybreak.abilitywar.utils.base.math.LocationUtil;
import daybreak.abilitywar.utils.base.math.VectorUtil;
import daybreak.abilitywar.utils.base.math.geometry.Circle;
import daybreak.abilitywar.utils.base.minecraft.nms.NMS;
import daybreak.abilitywar.utils.library.MaterialX;
import daybreak.abilitywar.utils.library.ParticleLib;
import daybreak.abilitywar.utils.library.PotionEffects;
import daybreak.abilitywar.utils.library.SoundLib;
import daybreak.google.common.base.Predicate;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;

@AbilityManifest(name = "시간 여행", rank = Rank.L, species = Species.HUMAN, explain = {
		"§7철괴 우클릭 §8- §d시간 저장§f: 체력, 상태효과, 위치, 인벤토리 등",
		" 현재 나의 모든 상태를 저장합니다. $[RCooldownConfig]",
		"§7패시브 §8- §a과거 여행§f: 치명적인 피해를 입었을 때 단 한 번 사망하지 않고",
		" 저장된 시간으로 자신의 상태를 되돌립니다.",
		"§7철괴 좌클릭 §8- §b미래 여행§f: $[DurationConfig]초간 §c무적 §f/ §d공격 불능 §f및",
		" §3타게팅 불가 §f상태가 되며 중도에 다시 좌클릭하여",
		" 즉시 §b미래 여행§f을 그만둘 수 있습니다.",
		" §b미래 여행§f에서 나올 때, 주변 $[RangeConfig]칸 내 플레이어의 시간을 §7왜곡§f하여",
		" $[EffectDuration]초간 이동 속도와 공격 속도를 느리게 하고, 자신은 빨라집니다.",
		"§2[§a!§2] §b_Daybreak_§f님이 시계 파티클 제작에 도움주셨습니다."
})

public class TimeTravel extends Synergy implements ActiveHandler {
	
	public TimeTravel(Participant participant) {
		super(participant);
	}
	
	private final Cooldown PastTravel = new Cooldown(LCooldownConfig.getValue(), "과거");
	private final Cooldown FutureTravel = new Cooldown(RCooldownConfig.getValue(), "미래");
	private final int effect = EffectDuration.getValue();
	private final int range = RangeConfig.getValue();
	private final ActionbarChannel ac = newActionbarChannel();
	private static final Circle circle = Circle.of(7, 100);
	private static final RGB color = RGB.of(36, 252, 254);
	
	public static final SettingObject<Integer> RCooldownConfig = synergySettings.new SettingObject<Integer>(TimeTravel.class,
			"Cooldown", 30, "# 과거 쿨타임") {
		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}

		@Override
		public String toString() {
			return Formatter.formatCooldown(getValue());
		}
	};	
	
	public static final SettingObject<Integer> LCooldownConfig = synergySettings.new SettingObject<Integer>(TimeTravel.class,
			"Cooldown", 60, "# 미래 쿨타임") {
		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}

		@Override
		public String toString() {
			return Formatter.formatCooldown(getValue());
		}
	};	
	
	public static final SettingObject<Integer> DurationConfig = synergySettings.new SettingObject<Integer>(TimeTravel.class,
			"Duration", 10, "# 미래 여행 지속 시간") {

		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}

	};
	
	public static final SettingObject<Integer> EffectDuration = synergySettings.new SettingObject<Integer>(TimeTravel.class,
			"Effect", 7, "# 효과 지속 시간") {

		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}

	};
	
	public static final SettingObject<Integer> RangeConfig = synergySettings.new SettingObject<Integer>(TimeTravel.class,
			"Range", 7, "# 효과 범위", "§2[§c!§2] §7주의! 파티클이 변경되지 않습니다.") {

		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}

	};
	
	public boolean checkdeath = true;
	
	private Location saveloc = null;
	private Collection<PotionEffect> savepotion;
	private int savefiretick = 0;
	private float savefall = 0;
	private double savehp = 0;
	private ItemStack[] saveinv;
	private float flyspeed = 0;
	private GameMode orgGM = null;
	
	public boolean ActiveSkill(Material material, AbilityBase.ClickType clicktype) {
	    if (material.equals(Material.IRON_INGOT)) {
	    	if (clicktype.equals(ClickType.RIGHT_CLICK)) {
	    		if (PastTravel.isCooldown()) return false;
	    	savehp = getPlayer().getHealth();
	    	saveloc = getPlayer().getLocation();
	    	savefiretick = getPlayer().getFireTicks();
	    	savefall = getPlayer().getFallDistance();
	    	savepotion = getPlayer().getActivePotionEffects();
	    	saveinv = getPlayer().getInventory().getContents();
	    	PastTravel.start();
	    	getPlayer().sendMessage("시간을 §a저장§f하였습니다.");
	      return true;
	    } else if (clicktype.equals(ClickType.LEFT_CLICK)) {
	    	if (FutureTravel.isCooldown()) return false;
	    	if (!traveling.isDuration()) {
	    		traveling.start();
	    	} else if (traveling.isDuration()) {
	    		traveling.stop(false);
	    	}
	      return true;
	    }
	    }
	    return false;
	}
	
	@SubscribeEvent
	private void onPlayerTeleport(PlayerTeleportEvent e) {
		if (traveling.isRunning() && getPlayer().equals(e.getPlayer())) {
			if (e.getCause() == TeleportCause.SPECTATE) e.setCancelled(true);
		}
	}
	
	@SubscribeEvent(priority = 6)
	private void onEntityDamage(EntityDamageEvent e) {
		if (checkdeath && e.getEntity().equals(getPlayer())) {
			if (getPlayer().getHealth() - e.getFinalDamage() <= 0) {
				ParticleLib.ITEM_CRACK.spawnParticle(getPlayer().getLocation(), 0, 1, 0, 50, 0.3, MaterialX.CLOCK);
				SoundLib.BLOCK_END_PORTAL_SPAWN.playSound(getPlayer().getLocation(), 1, 1.5f);
				getPlayer().setHealth(savehp);
				getPlayer().setFireTicks(savefiretick);
				getPlayer().setFallDistance(savefall);
				for (PotionEffect effect : getPlayer().getActivePotionEffects()) {
					getPlayer().removePotionEffect(effect.getType());
				}
				getPlayer().addPotionEffects(savepotion);
				
				new BukkitRunnable() {
					
					@Override
					public void run() {
						getPlayer().teleport(saveloc);
						getPlayer().getInventory().setContents(saveinv);
						ParticleLib.ITEM_CRACK.spawnParticle(getPlayer().getLocation(), 0, 1, 0, 100, 0.3, MaterialX.CLOCK);
						SoundLib.BLOCK_END_PORTAL_SPAWN.playSound(getPlayer().getLocation(), 1, 1.5f);
					}
				}.runTaskLater(AbilityWar.getPlugin(), 1L);
				
				checkdeath = false;
				e.setCancelled(true);
				}
		}
	}
	
	@SubscribeEvent(priority = 6)
	public void onEntityDamageByEntity(EntityDamageByEntityEvent e) {
		onEntityDamage(e);
	}
	
	@SubscribeEvent(priority = 6)
	public void onEntityDamageByBlock(EntityDamageByBlockEvent e) {
		onEntityDamage(e);
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
	
	private final Duration traveling = new Duration(DurationConfig.getValue() * 20, FutureTravel) {
		
		@Override
		protected void onDurationStart() {
			flyspeed = getPlayer().getFlySpeed();
			orgGM = getPlayer().getGameMode();
		}
		
		@Override
		protected void onDurationProcess(int arg0) {
			getPlayer().setGameMode(GameMode.SPECTATOR);
			getParticipant().attributes().TARGETABLE.setValue(false);
			ac.update("§b미래 여행 중");
			getPlayer().setFlySpeed(0.15f);
		}
		
		@Override
		protected void onDurationEnd() {
			onDurationSilentEnd();
		}
		
		@Override
		protected void onDurationSilentEnd() {
			getPlayer().setGameMode(orgGM);
			SoundLib.ITEM_TOTEM_USE.playSound(getPlayer(), 1, 1.7f);
			PotionEffects.FAST_DIGGING.addPotionEffect(getPlayer(), effect * 20, 2, true);
			PotionEffects.SPEED.addPotionEffect(getPlayer(), effect * 20, 2, true);
			getParticipant().attributes().TARGETABLE.setValue(true);
			clockeffect.start();
			getPlayer().setFlySpeed(flyspeed);
			
			for (Player p : LocationUtil.getEntitiesInCircle(Player.class, getPlayer().getLocation(), range, predicate)) {
				SoundLib.ENTITY_EVOKER_FANGS_ATTACK.playSound(p, 1, 0.7f);
				PotionEffects.SLOW.addPotionEffect(p, effect * 20, 2, true);
				PotionEffects.SLOW_DIGGING.addPotionEffect(p, effect * 20, 2, true);
			}
			
			new AbilityTimer(20) {
				@Override
				protected void run(int count) {
					Location center = getPlayer().getLocation().clone().add(0, 2 - count * 0.1, 0);
					for (Location loc : circle.toLocations(center).floor(center.getY())) {
						ParticleLib.REDSTONE.spawnParticle(loc, color);
					}
				}
			}.setPeriod(TimeUnit.TICKS, 1).start();
			
			ac.update(null);
		}
		
	}.setPeriod(TimeUnit.TICKS, 1);
	
	private static final EulerAngle DEFAULT_EULER_ANGLE = new EulerAngle(Math.toRadians(270), 0, 0);
	private static final ItemStack CLOCK = MaterialX.CLOCK.createItem();

	private final AbilityTimer clockeffect = new AbilityTimer(TaskType.NORMAL, 100) {
		private ArmorStand[] armorStands;

		@Override
		protected void onStart() {
			this.armorStands = new ArmorStand[] {
					getPlayer().getWorld().spawn(getPlayer().getLocation(), ArmorStand.class),
					getPlayer().getWorld().spawn(getPlayer().getLocation(), ArmorStand.class)
			};
			for (ArmorStand armorStand : armorStands) {
				armorStand.setVisible(false);
				armorStand.setInvulnerable(true);
				armorStand.setGravity(false);
				armorStand.setRightArmPose(DEFAULT_EULER_ANGLE);
				armorStand.getEquipment().setItemInMainHand(CLOCK);
				NMS.removeBoundingBox(armorStand);
			}
		}

		@Override
		protected void run(int count) {
			for (int i = 0; i < 5; i++) {
				final int index = (count - 1) * 5 + i;
				final double t = index * 0.0155;
				armorStands[0].teleport(adjustLocation(getPlayer().getLocation().clone().add(FastMath.cos(t) * 0.8, count * 0.0155, FastMath.sin(t) * 0.8)));
				armorStands[1].teleport(adjustLocation(getPlayer().getLocation().clone().add(-FastMath.cos(t) * 0.8, count * 0.0155, -FastMath.sin(t) * 0.8)));
			}
		}

		@Override
		protected void onEnd() {
			onSilentEnd();
		}

		@Override
		protected void onSilentEnd() {
			for (ArmorStand armorStand : armorStands) {
				armorStand.remove();
			}
			this.armorStands = null;
		}
	}.setPeriod(TimeUnit.TICKS, 1);

	private static Location adjustLocation(final Location location) {
		final Vector direction = location.getDirection().setY(0).normalize();
		return location.clone().subtract(0, 1, 0).subtract(direction.clone().multiply(0.75)).add(VectorUtil.rotateAroundAxisY(direction.clone(), 90).multiply(0.4));
	}
	
}
