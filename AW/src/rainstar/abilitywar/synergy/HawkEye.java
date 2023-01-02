package rainstar.abilitywar.synergy;

import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.ability.SubscribeEvent;
import daybreak.abilitywar.config.Configuration.Settings;
import daybreak.abilitywar.game.AbstractGame.CustomEntity;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.AbstractGame.Participant.ActionbarNotification.ActionbarChannel;
import daybreak.abilitywar.game.GameManager;
import daybreak.abilitywar.game.list.mix.synergy.Synergy;
import daybreak.abilitywar.game.module.DeathManager;
import daybreak.abilitywar.game.module.Wreck;
import daybreak.abilitywar.game.team.interfaces.Teamable;
import daybreak.abilitywar.utils.base.ProgressBar;
import daybreak.abilitywar.utils.base.color.RGB;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.base.math.LocationUtil;
import daybreak.abilitywar.utils.base.math.geometry.Line;
import daybreak.abilitywar.utils.base.minecraft.entity.decorator.Deflectable;
import daybreak.abilitywar.utils.base.minecraft.nms.NMS;
import daybreak.abilitywar.utils.base.minecraft.version.ServerVersion;
import daybreak.abilitywar.utils.library.MaterialX;
import daybreak.abilitywar.utils.library.ParticleLib;
import daybreak.abilitywar.utils.library.PotionEffects;
import daybreak.abilitywar.utils.library.SoundLib;
import daybreak.abilitywar.utils.library.item.EnchantLib;
import daybreak.abilitywar.utils.library.item.ItemLib;
import java.util.Iterator;
import java.util.function.Predicate;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Damageable;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.projectiles.ProjectileSource;
import org.bukkit.util.Vector;

@AbilityManifest(name = "매의 눈", rank = Rank.S, species = Species.HUMAN, explain = {
		"활을 쏠 때 매우 빠른 속도로 나아가는 특수한 투사체를 쏩니다.",
		"투사체는 하나의 대상만 공격할 수 있고, 블록에 닿으면 소멸합니다.",
		"단, 유리나 유리 판과 같은 블록은 뚫고 지나갑니다.",
		"투사체를 쏘고 난 후 일정 시간동안 재장전을 하며, 재장전 중에는",
		"활을 쏠 수 없습니다. 활을 들고 있을 경우 빠르게 이동할 수 없으며,",
		"이동이 제한되고, 가장 가까운 플레이어를 자동 조준합니다.",
		"피해를 받은 대상에게 §7실명§f과 §e발광§f 효과를 부여합니다."
})

public class HawkEye extends Synergy {

	public HawkEye(Participant participant) {
		super(participant);
	}

	private static final Material GLASS_PANE = ServerVersion.getVersion() > 12 ? Material.valueOf("GLASS_PANE") : Material.valueOf("THIN_GLASS");
	private static final RGB BULLET_COLOR = new RGB(77, 77, 77);

	private final AbilityTimer snipeMode = new AbilityTimer() {
		@Override
		protected void run(int count) {
			Material main = getPlayer().getInventory().getItemInMainHand().getType();
			Material off = getPlayer().getInventory().getItemInOffHand().getType();
			if (main.equals(Material.BOW) || off.equals(Material.BOW) || (ServerVersion.getVersion() >= 14 && (main.equals(MaterialX.CROSSBOW.compare(getPlayer().getInventory().getItemInMainHand())) || off.equals(MaterialX.CROSSBOW.compare(getPlayer().getInventory().getItemInOffHand()))))) {
				PotionEffects.SLOW.addPotionEffect(getPlayer(), 2, 3, true);
				getPlayer().setVelocity(getPlayer().getVelocity().setX(0).setY(Math.min(0, getPlayer().getVelocity().getY())).setZ(0));
				final Player target = LocationUtil.getNearestEntity(Player.class, getPlayer().getLocation(), predicate);
				if (target != null) {
					final Vector direction = target.getEyeLocation().toVector().subtract(getPlayer().getEyeLocation().toVector());
					final float yaw = LocationUtil.getYaw(direction), pitch = LocationUtil.getPitch(direction);
					for (Player player : Bukkit.getOnlinePlayers()) {
					       NMS.rotateHead(player, getPlayer(), yaw, pitch);
					}
				}
			}
		}
	}.setPeriod(TimeUnit.TICKS, 1);
	
	@Override
	protected void onUpdate(Update update) {
		if (update == Update.RESTRICTION_CLEAR) {
			snipeMode.start();
		}
	}
	
	private AbilityTimer reload = null;

	private final ActionbarChannel actionbarChannel = newActionbarChannel();

	private final Predicate<Entity> predicate = new Predicate<Entity>() {
		@Override
		public boolean test(Entity entity) {
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
					final Participant entityParticipant = getGame().getParticipant(entity.getUniqueId());
					return !teamGame.hasTeam(entityParticipant) || !teamGame.hasTeam(getParticipant()) || (!teamGame.getTeam(entityParticipant).equals(teamGame.getTeam(getParticipant())));
				}
			}
			return true;
		}
	};

	@SubscribeEvent
	public void onProjectileLaunch(EntityShootBowEvent e) {
		if (getPlayer().equals(e.getEntity()) && NMS.isArrow(e.getProjectile())) {
			e.setCancelled(true);
			if (reload == null) {
				if (!getPlayer().getGameMode().equals(GameMode.CREATIVE) && (!e.getBow().hasItemMeta() || !e.getBow().getItemMeta().hasEnchant(Enchantment.ARROW_INFINITE))) {
					ItemLib.removeItem(getPlayer().getInventory(), Material.ARROW, 1);
				}
				Arrow arrow = (Arrow) e.getProjectile();
				new Bullet<>(getPlayer(), arrow.getLocation(), arrow.getVelocity(), e.getBow().getEnchantmentLevel(Enchantment.ARROW_DAMAGE), BULLET_COLOR).start();
				SoundLib.ENTITY_WITHER_BREAK_BLOCK.playSound(getPlayer().getLocation(), 7, 2);
				final int reloadCount = Wreck.isEnabled(GameManager.getGame()) ? (int) (Math.max(((100 - Settings.getCooldownDecrease().getPercentage()) / 100.0), 0.85) * 25.0) : 25;
				reload = new AbilityTimer(reloadCount) {
					private final ProgressBar progressBar = new ProgressBar(reloadCount, 15);

					@Override
					protected void run(int count) {
						progressBar.step();
						actionbarChannel.update("재장전: " + progressBar.toString());
					}

					@Override
					protected void onEnd() {
						HawkEye.this.reload = null;
						actionbarChannel.update(null);
						SoundLib.BLOCK_STONE_PRESSURE_PLATE_CLICK_ON.playSound(getPlayer());
					}
				}.setBehavior(RestrictionBehavior.PAUSE_RESUME).setPeriod(TimeUnit.TICKS, 2);
				reload.start();
			} else {
				getPlayer().sendMessage("§b재장전 §f중입니다.");
			}
		}
	}

	public class Bullet<Shooter extends Entity & ProjectileSource> extends AbilityTimer {

		private final Shooter shooter;
		private final CustomEntity entity;
		private final Vector forward;
		private final int powerEnchant;

		private final RGB color;

		private Bullet(Shooter shooter, Location startLocation, Vector arrowVelocity, int powerEnchant, RGB color) {
			super(160);
			setPeriod(TimeUnit.TICKS, 1);
			this.shooter = shooter;
			this.entity = new ArrowEntity(startLocation.getWorld(), startLocation.getX(), startLocation.getY(), startLocation.getZ()).resizeBoundingBox(-1.5, -1.5, -1.5, 1.5, 1.5, 1.5);
			this.forward = arrowVelocity.multiply(30);
			this.powerEnchant = powerEnchant;
			this.color = color;
			this.lastLocation = startLocation;
		}

		private Location lastLocation;

		@Override
		protected void run(int i) {
			Location newLocation = lastLocation.clone().add(forward);
			for (Iterator<Location> iterator = Line.iteratorBetween(lastLocation, newLocation, 70); iterator.hasNext(); ) {
				Location location = iterator.next();
				entity.setLocation(location);
				Block block = location.getBlock();
				Material type = block.getType();
				if (type.isSolid()) {
					if (ItemLib.STAINED_GLASS.compareType(type) || Material.GLASS == type || ItemLib.STAINED_GLASS_PANE.compareType(type) || type == GLASS_PANE) {
						block.breakNaturally();
						SoundLib.BLOCK_GLASS_BREAK.playSound(block.getLocation(), 3, 1);
					} else {
						stop(false);
						return;
					}
				}
				for (Damageable damageable : LocationUtil.getConflictingEntities(Damageable.class, shooter.getWorld(), entity.getBoundingBox(), predicate)) {
					if (!shooter.equals(damageable)) {
						damageable.damage(EnchantLib.getDamageWithPowerEnchantment(Math.min((forward.getX() * forward.getX()) + (forward.getY() * forward.getY()) + (forward.getZ() * forward.getZ()) / 10.0, 10), powerEnchant), shooter);
						stop(false);
						if (damageable instanceof LivingEntity) {
						PotionEffects.BLINDNESS.addPotionEffect((LivingEntity) damageable, 30, 0, true);
						PotionEffects.GLOWING.addPotionEffect((LivingEntity) damageable, 60, 0, true);
						}
						return;
					}
				}
				ParticleLib.REDSTONE.spawnParticle(location, color);
			}
			lastLocation = newLocation;
		}

		@Override
		protected void onEnd() {
			entity.remove();
		}

		@Override
		protected void onSilentEnd() {
			entity.remove();
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
				Player deflectedPlayer = deflector.getPlayer();
				new Bullet<>(deflectedPlayer, lastLocation, newDirection, powerEnchant, color).start();
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