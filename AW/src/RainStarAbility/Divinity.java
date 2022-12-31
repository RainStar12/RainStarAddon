package RainStarAbility;

import javax.annotation.Nullable;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.entity.EntityDamageByBlockEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.player.PlayerArmorStandManipulateEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.util.Vector;

import daybreak.abilitywar.ability.AbilityBase;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.ability.SubscribeEvent;
import daybreak.abilitywar.ability.decorator.ActiveHandler;
import daybreak.abilitywar.config.ability.AbilitySettings.SettingObject;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.module.DeathManager;
import daybreak.abilitywar.game.team.interfaces.Teamable;
import daybreak.abilitywar.utils.base.Formatter;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.base.math.LocationUtil;
import daybreak.abilitywar.utils.base.math.VectorUtil;
import daybreak.abilitywar.utils.base.math.geometry.Sphere;
import daybreak.abilitywar.utils.base.minecraft.item.Skulls;
import daybreak.abilitywar.utils.base.minecraft.nms.NMS;
import daybreak.abilitywar.utils.base.minecraft.version.ServerVersion;
import daybreak.abilitywar.utils.library.ParticleLib;
import daybreak.abilitywar.utils.library.SoundLib;
import daybreak.google.common.base.Predicate;

@AbilityManifest(name = "신선", rank = Rank.L, species = Species.GOD, explain = {
		"§7철괴 우클릭 §8- §b신선놀음§f: 자신의 분신을 $[ALTER_DURATION]초간 현재 위치에 소환합니다.",
		" 분신으로부터 6칸 내의 모든 플레이어는 분신에게 도발당합니다.",
		" 분신 지속 중 자신이 주는 모든 피해량이 $[ATTACK_DAMAGE_DECREASE]%로 감소합니다. $[COOLDOWN]",
		"§7스킬 패시브 §8- §b신출귀몰§f: 적에게 피해를 받을 때 해당 피해를 무효화하고 분신과",
		" 자신의 위치를 변경합니다. 효과 발동 시 지속 시간이 $[DURATION_DECREASE]초 줄어듭니다.",
		"§7패시브 §8- §b허공답보§f: 공중에서 웅크릴 시 한 번 더 점프합니다."
		},
		summarize = {
		"§7철괴 우클릭 시§f 적의 시선을 강제로 조작하는 §3분신§f을 소환합니다. $[COOLDOWN]",
		"분신이 있을 때 공격력이 소폭 감소하고, 적에게 피해를 받을 때",
		"피해를 무효화하고 지속시간을 감소시킨 뒤 분신과 자신의 위치를 변경합니다.",
		"공중에서 웅크릴 시 한 번 더 점프하고 첫 낙하 피해를 무시합니다."
		})

@SuppressWarnings("deprecation")
public class Divinity extends AbilityBase implements ActiveHandler {

	public Divinity(Participant participant) {
		super(participant);
	}
	
	public static final SettingObject<Integer> COOLDOWN = abilitySettings.new SettingObject<Integer>(Divinity.class,
			"cooldown", 100, "# 쿨타임") {
		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}

		@Override
		public String toString() {
			return Formatter.formatCooldown(getValue());
		}
	};
	
	public static final SettingObject<Double> ALTER_DURATION = abilitySettings.new SettingObject<Double>(Divinity.class,
			"alter-duration", 10.0, "# 분신 지속 시간") {
		@Override
		public boolean condition(Double value) {
			return value >= 0;
		}
	};
	
	public static final SettingObject<Double> DURATION_DECREASE = abilitySettings.new SettingObject<Double>(Divinity.class,
			"duration", 1.5, "# 감소 지속 시간") {
		@Override
		public boolean condition(Double value) {
			return value >= 0;
		}
	};
	
	public static final SettingObject<Integer> ATTACK_DAMAGE_DECREASE = abilitySettings.new SettingObject<Integer>(Divinity.class,
			"attack-damage-decrease", 15, "# 분신 지속 중 대미지 감소") {
		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}
	};
	
	private ArmorStand armorstand = null;
	private static final Sphere sphere = Sphere.of(6, 12);
	private boolean morejump = false;
	private boolean fallcancel = false;
	private BossBar bossBar = null;
	private final int decrease = ATTACK_DAMAGE_DECREASE.getValue();
	private final int duration = (int) (ALTER_DURATION.getValue() * 20);
	private final int durationdec = (int) (DURATION_DECREASE.getValue() * 20);
	private final Cooldown cool = new Cooldown(COOLDOWN.getValue());
	
	private final Predicate<Entity> predicate = new Predicate<Entity>() {
		@Override
		public boolean test(Entity entity) {
			if (entity.equals(getPlayer())) return false;
			if (entity instanceof Player) {
				if (!getGame().isParticipating(entity.getUniqueId())
						|| (getGame() instanceof DeathManager.Handler &&
								((DeathManager.Handler) getGame()).getDeathManager().isExcluded(entity.getUniqueId()))
						|| !getGame().getParticipant(entity.getUniqueId()).attributes().TARGETABLE.getValue()) {
					return false;
				}
				if (getGame() instanceof Teamable) {
					final Teamable teamGame = (Teamable) getGame();
					final Participant entityParticipant = teamGame.getParticipant(
							entity.getUniqueId()), participant = getParticipant();
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
	
	public boolean ActiveSkill(Material material, ClickType clicktype) {
		if (material.equals(Material.IRON_INGOT) && clicktype.equals(ClickType.RIGHT_CLICK)) {
	    	if (!skill.isDuration() && !cool.isCooldown()) {
	    		return skill.start();
	    	}
		}
		return false;
	}
	
	@SubscribeEvent
	private void onPlayerJoin(final PlayerJoinEvent e) {
		if (getPlayer().getUniqueId().equals(e.getPlayer().getUniqueId())) {
			if (bossBar != null) bossBar.addPlayer(e.getPlayer());
		}
	}

	@SubscribeEvent
	private void onPlayerQuit(final PlayerQuitEvent e) {
		if (getPlayer().getUniqueId().equals(e.getPlayer().getUniqueId())) {
			if (bossBar != null) bossBar.removePlayer(e.getPlayer());
		}
	}
    
	@SubscribeEvent
	private void onPlayerMove(PlayerMoveEvent e) {
		if (e.getPlayer().equals(getPlayer())) {
			if (!e.getPlayer().isOnGround() && !morejump && !getPlayer().isFlying()) {
				if (e.getPlayer().isSneaking()) {
					e.getPlayer().setVelocity(VectorUtil.validateVector(new Vector(e.getPlayer().getVelocity().getX() * 1.25, 1, e.getPlayer().getVelocity().getZ() * 1.25)));
					ParticleLib.CLOUD.spawnParticle(getPlayer().getLocation(), 0.5, 0.1, 0.5, 100, 0);
					SoundLib.BLOCK_SNOW_FALL.playSound(getPlayer().getLocation(), 1, 0.7f);
					morejump = true;
					fallcancel = true;
				}	
			}
			if (e.getPlayer().isOnGround() && morejump) {
				morejump = false;
			}
		}
	}
	
	@SubscribeEvent
	private void onPlayerArmorStandManipulate(PlayerArmorStandManipulateEvent e) {
		if (e.getRightClicked().equals(armorstand)) e.setCancelled(true);
	}
	
	@SubscribeEvent
	private void onEntityDamage(EntityDamageEvent e) {
		if (e.getEntity().equals(getPlayer()) && e.getCause() == DamageCause.FALL && fallcancel) {
			fallcancel = false;
			e.setCancelled(true);
		}
	}
	
	@SubscribeEvent
	private void onEntityDamageByBlock(EntityDamageByBlockEvent e) {
		onEntityDamage(e);
	}
	
	@SubscribeEvent
	private void onEntityDamageByEntity(EntityDamageByEntityEvent e) {
		onEntityDamage(e);
		if (skill.isRunning()) {
			Player damager = null;
			if (e.getDamager() instanceof Projectile) {
				Projectile projectile = (Projectile) e.getDamager();
				if (projectile.getShooter() instanceof Player) damager = (Player) projectile.getShooter();
			} else if (e.getDamager() instanceof Player) damager = (Player) e.getDamager();
			
			if (getPlayer().equals(damager)) e.setDamage(e.getDamage() * ((100 - decrease) * 0.01));
			else if (armorstand != null && e.getEntity().equals(getPlayer())) {
				e.setCancelled(true);
	    		SoundLib.ENTITY_PLAYER_ATTACK_SWEEP.playSound(getPlayer().getLocation(), 1, 2);
	    		SoundLib.ENTITY_PLAYER_ATTACK_SWEEP.playSound(armorstand.getLocation(), 1, 2);
	    		ParticleLib.CLOUD.spawnParticle(getPlayer().getLocation(), 0.2, 1.5, 0.2, 50, 0.1f);
	    		ParticleLib.CLOUD.spawnParticle(armorstand.getLocation(), 0.2, 1.5, 0.2, 50, 0.1f);
	    		Location previousloc = getPlayer().getLocation();
	    		Vector previousdirection = getPlayer().getLocation().getDirection();
	    		getPlayer().teleport(armorstand.getLocation().setDirection(armorstand.getLocation().getDirection()));
	    		armorstand.teleport(previousloc.setDirection(previousdirection));
	    		skill.setCount(Math.max(1, skill.getCount() - durationdec));
	    	}
		}
	}
	
	private final Duration skill = new Duration(duration, cool) {

		@Override
		protected void onDurationStart() {
			armorstand = getPlayer().getWorld().spawn(getPlayer().getLocation(), ArmorStand.class);
			if (ServerVersion.getVersion() >= 10 && ServerVersion.getVersion() < 15)
				armorstand.setInvulnerable(true);
			armorstand.setCustomName("§e" + getPlayer().getName() + "§f의 §b분신");
			armorstand.setCustomNameVisible(true);
			armorstand.setBasePlate(false);
			armorstand.setArms(true);
			armorstand.setGravity(false);
			EntityEquipment equipment = armorstand.getEquipment();
			equipment.setArmorContents(getPlayer().getInventory().getArmorContents());
			equipment.setHelmet(Skulls.createSkull(getPlayer()));
		}
		
		@Override
		protected void onDurationProcess(int count) {
			if (armorstand == null) {
				this.stop(false);
				getPlayer().sendMessage("§3[§b!§3] §c분신이 사라져 능력이 즉시 종료되었습니다.");
			}
			if (count % 8 == 0) {
				for (Location loc : sphere.toLocations(armorstand.getLocation().clone().add(0, 1, 0))) {
					ParticleLib.ENCHANTMENT_TABLE.spawnParticle(loc, 0, 0, 0, 1, 0.2f);
				}
			}
			for (LivingEntity livingEntity : LocationUtil.getNearbyEntities(LivingEntity.class, armorstand.getLocation().clone().add(0, 1, 0), 6, 6, predicate)) {
				if (!livingEntity.equals(armorstand)) {
					Vector direction = armorstand.getEyeLocation().toVector().subtract(livingEntity.getEyeLocation().toVector());
					float yaw = LocationUtil.getYaw(direction), pitch = LocationUtil.getPitch(direction);
					for (Player player : Bukkit.getOnlinePlayers()) {
					    NMS.rotateHead(player, livingEntity, yaw, pitch);	
					}
				}
			}
		}
		
		@Override
		protected void onDurationEnd() {
			onDurationSilentEnd();
		}
		
		@Override
		protected void onDurationSilentEnd() {
			armorstand.remove();
		}
		
	}.setPeriod(TimeUnit.TICKS, 1);
	
}