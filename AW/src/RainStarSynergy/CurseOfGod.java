package RainStarSynergy;

import javax.annotation.Nullable;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.entity.EntityDamageByBlockEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerArmorStandManipulateEvent;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.util.Vector;

import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.ability.SubscribeEvent;
import daybreak.abilitywar.ability.decorator.ActiveHandler;
import daybreak.abilitywar.config.ability.AbilitySettings.SettingObject;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.list.mix.synergy.Synergy;
import daybreak.abilitywar.game.module.DeathManager;
import daybreak.abilitywar.game.team.interfaces.Teamable;
import daybreak.abilitywar.utils.base.Formatter;
import daybreak.abilitywar.utils.base.color.RGB;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.base.math.LocationUtil;
import daybreak.abilitywar.utils.base.math.geometry.Circle;
import daybreak.abilitywar.utils.base.math.geometry.Sphere;
import daybreak.abilitywar.utils.base.minecraft.item.Skulls;
import daybreak.abilitywar.utils.base.minecraft.nms.NMS;
import daybreak.abilitywar.utils.base.minecraft.version.ServerVersion;
import daybreak.abilitywar.utils.library.ParticleLib;
import daybreak.abilitywar.utils.library.SoundLib;
import daybreak.google.common.base.Predicate;

@AbilityManifest(
		name = "신의 저주", rank = Rank.S, species = Species.HUMAN, explain = {
		"다른 플레이어를 13칸 내에서 철괴 우클릭 시 $[DURATION]초간 지속되는 분신을",
		"소환해냅니다. 분신으로부터 6.5칸 내 모든 플레이어는 분신을 바라보며",
		"분신을 타격할 때 50%의 피해량을 분신의 주인에게 입힙니다.",
		"다만 자신은 분신을 타격할 수 없습니다. $[COOLDOWN]",
		"또한 분신이 있을 때 자신이 주는 피해량이 25% 감소합니다.",
		"§b[§7아이디어 제공자§b] §ecommon_Mango"
		})

public class CurseOfGod extends Synergy implements ActiveHandler {

	public CurseOfGod(Participant participant) {
		super(participant);
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
	
	public static final SettingObject<Integer> COOLDOWN = synergySettings.new SettingObject<Integer>(CurseOfGod.class, "cooldown", 100,
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

	public static final SettingObject<Integer> DURATION = synergySettings.new SettingObject<Integer>(CurseOfGod.class, "duration", 10,
			"# 지속시간") {

		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}

	};
	
	private final Cooldown cool = new Cooldown(COOLDOWN.getValue());

	private static final RGB BLACK = RGB.of(1, 1, 1);

	private Player target = null;
	private ArmorStand armorStand = null;
	private static final Sphere sphere = Sphere.of(6.5, 14);

	@Override
	public boolean ActiveSkill(Material material, ClickType clickType) {
		if (material == Material.IRON_INGOT && clickType == ClickType.RIGHT_CLICK && !skill.isDuration() && !cool.isCooldown()) {
			Player player = LocationUtil.getEntityLookingAt(Player.class, getPlayer(), 13, predicate);
			if (player != null) {
				target = player;
				skill.start();
			}
		}
		return false;
	}
	
	private final Duration skill = new Duration(DURATION.getValue() * 20, cool) {
		private int particle;

		@Override
		protected void onDurationStart() {
			armorStand = target.getWorld().spawn(getPlayer().getLocation(), ArmorStand.class);
			if (ServerVersion.getVersion() >= 10 && ServerVersion.getVersion() < 15)
				armorStand.setInvulnerable(true);
			armorStand.setCustomName("§e" + target.getName() + "§f의 §b분신");
			armorStand.setCustomNameVisible(true);
			armorStand.setBasePlate(false);
			armorStand.setArms(true);
			armorStand.setGravity(false);
			EntityEquipment equipment = armorStand.getEquipment();
			equipment.setArmorContents(target.getInventory().getArmorContents());
			equipment.setHelmet(Skulls.createSkull(target));
			this.particle = 0;
		}

		@Override
		protected void onDurationProcess(int count) {
			if (count % 2 == 0) {
				if (++particle >= 10) {
					showHelix(armorStand.getLocation());
					particle = 0;
				}
				Location location = armorStand.getLocation();
				location.setYaw(location.getYaw() + 5);
				armorStand.teleport(location);	
			}
			if (armorStand == null) {
				this.stop(false);
				getPlayer().sendMessage("§3[§b!§3] §c분신이 사라져 능력이 즉시 종료되었습니다.");
			}
			if (count % 8 == 0) {
				for (Location loc : sphere.toLocations(armorStand.getLocation().clone().add(0, 1, 0))) {
					ParticleLib.ENCHANTMENT_TABLE.spawnParticle(loc, 0, 0, 0, 1, 0.2f);
				}
			}
			for (LivingEntity livingEntity : LocationUtil.getNearbyEntities(LivingEntity.class, armorStand.getLocation().clone().add(0, 1, 0), 6, 6, predicate)) {
				if (!livingEntity.equals(armorStand)) {
					Vector direction = armorStand.getEyeLocation().toVector().subtract(livingEntity.getEyeLocation().toVector());
					float yaw = LocationUtil.getYaw(direction), pitch = LocationUtil.getPitch(direction);
					for (Player player : Bukkit.getOnlinePlayers()) {
					    NMS.rotateHead(player, livingEntity, yaw, pitch);	
					}
				}
			}
		}

		@Override
		protected void onDurationEnd() {
			target = null;
			armorStand.remove();
			armorStand = null;
		}

		@Override
		protected void onDurationSilentEnd() {
			target = null;
			armorStand.remove();
			armorStand = null;
		}
	}.setPeriod(TimeUnit.TICKS, 1);

	private static final int particleCount = 20;
	private static final double yDiff = 0.6 / particleCount;
	private static final Circle helixCircle = Circle.of(0.5, particleCount);

	private void showHelix(Location target) {
		new AbilityTimer((particleCount * 3) / 2) {
			int count = 0;

			@Override
			protected void run(int a) {
				for (int i = 0; i < 2; i++) {
					ParticleLib.REDSTONE.spawnParticle(target.clone().add(helixCircle.get(count % 20)).add(0, count * yDiff, 0), BLACK);
					count++;
				}
			}
		}.setPeriod(TimeUnit.TICKS, 1).start();
	}

	@SubscribeEvent
	private void onEntityDamageByEntity(EntityDamageByEntityEvent e) {
		if (skill.isRunning() && e.getEntity().equals(armorStand)) {
			e.setCancelled(true);
			if (!e.getDamager().equals(getPlayer())) {
				target.damage(e.getDamage() * 0.5, armorStand);
				if (e.getDamager() instanceof Player) {
					SoundLib.ENTITY_PLAYER_ATTACK_SWEEP.playSound((Player) e.getDamager());
				}	
			} else {
				getPlayer().sendMessage("§3[§b!§3] §c당신은 분신을 공격할 수 없습니다.");
			}
		}
		if (skill.isRunning()) {
			if (e.getDamager().equals(getPlayer())) {
				e.setDamage(e.getDamage() * 0.75);
			}
			if (e.getDamager() instanceof Projectile) {
				Projectile p = (Projectile) e.getDamager();
				if (getPlayer().equals(p.getShooter())) {
					e.setDamage(e.getDamage() * 0.75);
				}
			}	
		}
	}

	@SubscribeEvent
	private void onEntityDamageByBlock(EntityDamageByBlockEvent e) {
		onEntityDamage(e);
	}

	@SubscribeEvent
	private void onEntityDamage(EntityDamageEvent e) {
		if (skill.isRunning() && e.getEntity().equals(armorStand)) {
			e.setCancelled(true);
			target.damage(e.getDamage() * (2.3 * (1 / Math.max(target.getHealth(), 0.01))), armorStand);
		}
	}

	@SubscribeEvent
	private void onPlayerArmorStandManipulate(PlayerArmorStandManipulateEvent e) {
		if (e.getRightClicked().equals(armorStand)) e.setCancelled(true);
	}
	
	
}
