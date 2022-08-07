package rainstar.aw.ability;

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
import kotlin.ranges.RangesKt;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
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
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.player.PlayerArmorStandManipulateEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.util.Vector;

import javax.annotation.Nullable;

@AbilityManifest(name = "신선", rank = Rank.L, species = Species.GOD, explain = {
		"§7철괴 우클릭 §8- §b신선놀음§f: 자신의 분신을 $[DURATION]초간 현재 위치에 소환합니다.",
		" 분신으로부터 6칸 내의 모든 플레이어는 분신에게 도발당합니다.",
		" 분신 지속 중 자신이 주는 모든 피해량이 $[DAMAGE_DECREASE]%로 감소합니다. $[COOLDOWN]",
		"§7철괴 좌클릭 §8- §b신출귀몰§f: 신선놀음 지속 도중 사용 시 지속시간 1초를 소모해",
		" 자신의 위치와 분신의 위치를 서로 뒤바꾸고 피해량 감소를 2초간 없앱니다.",
		"§7패시브 §8- §b허공답보§f: 낙하하며 공중에서 웅크릴 시 한 번 더 점프합니다.",
		" 사용 후 첫 낙하 피해를 무시합니다. 사용 후 착지 전 공중에서 근접 타격했을 때",
		" 체공한 시간에 비례하여 최대 5의 추가 피해를 입힐 수 있습니다."
		},
		summarize = {
		"§7철괴 우클릭 시§f 적의 시선을 강제로 조작하는 §3분신§f을 소환합니다. $[COOLDOWN]",
		"분신이 있을 때 모든 피해량이 50% 감소합니다.",
		"§7철괴 좌클릭 시§f 분신이 있을 때 지속시간 1초를 소모해 분신과 나의 위치를 뒤바꾸고",
		"피해량 감소 효과를 2초간 제거합니다.",
		"공중에서 웅크릴 시 한 번 더 점프하고 첫 낙하 피해를 무시합니다."
		})

@SuppressWarnings("deprecation")
public class Divinity extends AbilityBase implements ActiveHandler {

	public Divinity(Participant participant) {
		super(participant);
	}
	
	private ArmorStand armorstand = null;
	private static final Sphere sphere = Sphere.of(6, 12);
	private boolean morejump = false;
	private boolean fallcancel = false;
	private BossBar bossBar = null;
	private BossBar bossBar2 = null;
	private int addDamage = 0;
	private final int decrease = DAMAGE_DECREASE.getValue();
	
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
	
	public static final SettingObject<Integer> COOLDOWN 
	= abilitySettings.new SettingObject<Integer>(Divinity.class,
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
	
	public static final SettingObject<Integer> DURATION 
	= abilitySettings.new SettingObject<Integer>(Divinity.class,
			"duration", 8, "# 지속 시간") {
		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}
	};
	
	public static final SettingObject<Integer> DAMAGE_DECREASE 
	= abilitySettings.new SettingObject<Integer>(Divinity.class,
			"damage-decrease", 30, "# 분신 지속 중 대미지 감소") {
		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}
	};
	
	public boolean ActiveSkill(Material material, ClickType clicktype) {
		if (material.equals(Material.IRON_INGOT)) {
			if (clicktype.equals(ClickType.RIGHT_CLICK)) {
		    	if (!skill.isDuration() && !cool.isCooldown()) {
		    		return skill.start();
		    	}
		    } else {
		    	if (skill.isDuration() && armorstand != null) {
		    		SoundLib.ENTITY_PLAYER_ATTACK_SWEEP.playSound(getPlayer().getLocation(), 1, 2);
		    		SoundLib.ENTITY_PLAYER_ATTACK_SWEEP.playSound(armorstand.getLocation(), 1, 2);
		    		ParticleLib.CLOUD.spawnParticle(getPlayer().getLocation(), 0.2, 1.5, 0.2, 50, 0.1f);
		    		ParticleLib.CLOUD.spawnParticle(armorstand.getLocation(), 0.2, 1.5, 0.2, 50, 0.1f);
		    		Location previousloc = getPlayer().getLocation();
		    		Vector previousdirection = getPlayer().getLocation().getDirection();
		    		getPlayer().teleport(armorstand.getLocation().setDirection(armorstand.getLocation().getDirection()));
		    		armorstand.teleport(previousloc.setDirection(previousdirection));
		    		skill.setCount(skill.getCount() - 20);
		    		if (nodamage.isRunning()) {
		    			nodamage.setCount(40);
		    		} else {
			    		nodamage.start();	
		    		}
		    	} else {
			    	getPlayer().sendMessage("§3[§b!§3] §c분신이 없거나, 능력이 지속 중이지 않습니다.");	
		    	}
		    }
		}
		return false;
	}
	
    private final AbilityTimer addcounter = new AbilityTimer() {
    	
    	@Override
    	public void onStart() {
    		bossBar2 = Bukkit.createBossBar("체공 추가 피해량", BarColor.WHITE, BarStyle.SEGMENTED_10);
    		bossBar2.setProgress(0);
    		bossBar2.addPlayer(getPlayer());
    		if (ServerVersion.getVersion() >= 10) bossBar2.setVisible(true);
    	}
    	
    	@Override
		public void run(int count) {
    		bossBar2.setProgress(RangesKt.coerceIn((double) count / 50, 0, 1));
    		addDamage = count;
    	}
    	
		@Override
		public void onEnd() {
			bossBar2.removeAll();
		}

		@Override
		public void onSilentEnd() {
			bossBar2.removeAll();
		}
    	
    }.setPeriod(TimeUnit.TICKS, 1).register();
	
    private final AbilityTimer nodamage = new AbilityTimer(40) {
    	
    	@Override
    	public void onStart() {
    		bossBar = Bukkit.createBossBar("대미지 감소 무효화", BarColor.BLUE, BarStyle.SEGMENTED_6);
    		bossBar.setProgress(1);
    		bossBar.addPlayer(getPlayer());
    		if (ServerVersion.getVersion() >= 10) bossBar.setVisible(true);
    	}
    	
    	@Override
		public void run(int count) {
    		bossBar.setProgress(RangesKt.coerceIn((double) count / 40, 0, 1));
    	}
    	
		@Override
		public void onEnd() {
			bossBar.removeAll();
		}

		@Override
		public void onSilentEnd() {
			bossBar.removeAll();
		}
    	
    }.setPeriod(TimeUnit.TICKS, 1).register();
    
	@SubscribeEvent
	private void onPlayerJoin(final PlayerJoinEvent e) {
		if (getPlayer().getUniqueId().equals(e.getPlayer().getUniqueId())) {
			if (bossBar != null) bossBar.addPlayer(e.getPlayer());
			if (bossBar2 != null) bossBar2.addPlayer(e.getPlayer());
		}
	}

	@SubscribeEvent
	private void onPlayerQuit(final PlayerQuitEvent e) {
		if (getPlayer().getUniqueId().equals(e.getPlayer().getUniqueId())) {
			if (bossBar != null) bossBar.removePlayer(e.getPlayer());
			if (bossBar2 != null) bossBar2.removePlayer(e.getPlayer());
		}
	}
    
	@SubscribeEvent
	private void onPlayerMove(PlayerMoveEvent e) {
		if (e.getPlayer().equals(getPlayer())) {
			if (!e.getPlayer().isOnGround() && !morejump && !getPlayer().isFlying()) {
				final double fromY = e.getFrom().getY(), toY = e.getTo().getY();
				if (toY < fromY) {
					if (e.getPlayer().isSneaking()) {
						e.getPlayer().setVelocity(VectorUtil.validateVector(new Vector(e.getPlayer().getVelocity().getX() * 1.25, 1, e.getPlayer().getVelocity().getZ() * 1.25)));
						ParticleLib.CLOUD.spawnParticle(getPlayer().getLocation(), 0.5, 0.1, 0.5, 100, 0);
						SoundLib.BLOCK_SNOW_FALL.playSound(getPlayer().getLocation(), 1, 0.7f);
						morejump = true;
						addcounter.start();
						fallcancel = true;
					}	
				}
			}
			if (e.getPlayer().isOnGround() && morejump) {
				morejump = false;
				addcounter.stop(false);
				addDamage = 0;
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
			if (!nodamage.isRunning()) {
				if (e.getDamager().equals(getPlayer())) {
					e.setDamage(e.getDamage() * ((100 - decrease) * 0.01));
				}
				if (e.getDamager() instanceof Projectile) {
					Projectile p = (Projectile) e.getDamager();
					if (getPlayer().equals(p.getShooter())) {
						e.setDamage(e.getDamage() * ((100 - decrease) * 0.01));
					}
				}	
			}
		}
		if (e.getDamager().equals(getPlayer())) {
			if (!getPlayer().isOnGround()) {
				e.setDamage(e.getDamage() + Math.min(5, addDamage * 0.04));
			}
		}
	}
	
	private final Duration skill = new Duration(DURATION.getValue() * 20, cool) {

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
			nodamage.stop(false);
		}
		
	}.setPeriod(TimeUnit.TICKS, 1);
	
}