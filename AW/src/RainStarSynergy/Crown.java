package RainStarSynergy;

import javax.annotation.Nullable;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Damageable;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerArmorStandManipulateEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.EulerAngle;
import org.bukkit.util.Vector;

import daybreak.abilitywar.AbilityWar;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.ability.SubscribeEvent;
import daybreak.abilitywar.ability.decorator.ActiveHandler;
import daybreak.abilitywar.config.ability.AbilitySettings.SettingObject;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.AbstractGame.Participant.ActionbarNotification.ActionbarChannel;
import daybreak.abilitywar.game.list.mix.synergy.Synergy;
import daybreak.abilitywar.game.module.DeathManager;
import daybreak.abilitywar.game.module.Wreck;
import daybreak.abilitywar.game.team.interfaces.Teamable;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.base.math.LocationUtil;
import daybreak.abilitywar.utils.base.minecraft.damage.Damages;
import daybreak.abilitywar.utils.base.minecraft.version.ServerVersion;
import daybreak.abilitywar.utils.library.MaterialX;
import daybreak.abilitywar.utils.library.ParticleLib;
import daybreak.abilitywar.utils.library.SoundLib;
import daybreak.google.common.base.Predicate;
import daybreak.google.common.base.Strings;

@AbilityManifest(name = "왕관", rank = Rank.A, species = Species.OTHERS, explain = {
		"§e방패 병사§f를 $[MAX_CHARGE]만큼 고용해 다니며, 전부 사용 시 20초 후 고용합니다.",
		"철괴 우클릭으로 §e방패 병사§f를 하나씩 내보내, 생명체를 밀쳐내며",
		"5초 후 $[DAMAGE]의 §c폭발 피해§f와 함께 §4자폭§f시킵니다.",
		"§e방패 병사§f의 $[RANGE]칸 내의 플레이어는 위압감 탓에 느려집니다."
		})
public class Crown extends Synergy implements ActiveHandler {
	
	public Crown(Participant participant) {
		super(participant);
	}
	
	public static final SettingObject<Integer> MAX_CHARGE = 
			synergySettings.new SettingObject<Integer>(Crown.class, "max-charge", 10,
			"# 방패 병사 최대 충전 수") {

		@Override
		public boolean condition(Integer value) {
			return value >= 1;
		}

	};
	
	public static final SettingObject<Double> DAMAGE = 
			synergySettings.new SettingObject<Double>(Crown.class, "damage", 15.0,
            "# 방패 병사의 자폭 대미지") {
        @Override
        public boolean condition(Double value) {
            return value >= 0;
        }
    };
    
	public static final SettingObject<Double> RANGE = 
			synergySettings.new SettingObject<Double>(Crown.class, "range", 5.0,
            "# 병사 위압감 범위", "# 단위: 칸") {
        @Override
        public boolean condition(Double value) {
            return value >= 0;
        }
    };
	
	@Override
	public boolean ActiveSkill(Material material, ClickType clickType) {
		if (clickType == ClickType.RIGHT_CLICK) {
			if (material == Material.IRON_INGOT) {
				if (charge.charges > 0) {
					if (Crown.this.charge.subtractCharge(1)) {
						new Shieldbearer(1).start();	
						return true;
					}
				}
			}
		}
		return false;
	}
    
    private final int maxCharge = MAX_CHARGE.getValue();
	private final double damage = DAMAGE.getValue();
	private final double range = RANGE.getValue();
	
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

	public class Shieldbearer extends AbilityTimer implements Listener {
		
		private ArmorStand shieldbearer;
		private Vector direction;
		private Vector push;
		
		public Shieldbearer(int value) {
			super(TaskType.REVERSE, 100);
			setPeriod(TimeUnit.TICKS, 1);
		}
		
		@Override
		public void onStart() {
			Bukkit.getPluginManager().registerEvents(this, AbilityWar.getPlugin());
			final Location playerLocation = getPlayer().getLocation();
			direction = playerLocation.getDirection();
			final Location lineTarget = playerLocation.clone().add(direction.clone().setY(0).normalize().multiply(3.75));
			this.shieldbearer = getPlayer().getWorld().spawn(lineTarget, ArmorStand.class);
			if (ServerVersion.getVersion() >= 10) {
				shieldbearer.setInvulnerable(true);
				shieldbearer.setCollidable(false);
			}
			shieldbearer.setVisible(false);

			final EulerAngle eulerAngle = new EulerAngle(Math.toRadians(270), Math.toRadians(270), 0);
			shieldbearer.setBasePlate(false);
			shieldbearer.setArms(true);
			shieldbearer.setVisible(false);
			shieldbearer.setRightArmPose(eulerAngle);
			shieldbearer.setGravity(false);
			final EntityEquipment equipment = shieldbearer.getEquipment();
			equipment.setItemInMainHand(new ItemStack(Material.SHIELD));
			equipment.setHelmet(MaterialX.GOLDEN_HELMET.createItem());
			push = direction.clone().multiply(2).setY(0);
		}
		
		@Override
		public void run(int count) {
			if (count >= 80) {
				shieldbearer.setVelocity(direction);
			} else shieldbearer.setGravity(false);
			for (Entity entity : LocationUtil.getConflictingEntities(Entity.class, shieldbearer, predicate)) {
				entity.setVelocity(push);
			}
			if (count <= 20) {
				shieldbearer.setGlowing(count % 4 <= 1);
			}
		}
		
		@Override
		public void onEnd() {
			onSilentEnd();
		}
		
		@Override
		public void onSilentEnd() {
			for (Damageable damageable : LocationUtil.getNearbyEntities(Damageable.class, shieldbearer.getLocation(), 5, 5, predicate)) {
				Damages.damageExplosion(damageable, getPlayer(), (float) damage);
			}
			ParticleLib.EXPLOSION_HUGE.spawnParticle(shieldbearer.getLocation());
			SoundLib.ENTITY_GENERIC_EXPLODE.playSound(shieldbearer.getLocation());
			shieldbearer.remove();
			HandlerList.unregisterAll(this);
		}
		
		@EventHandler()
		public void onPlayerMove(PlayerMoveEvent e) {
			if (LocationUtil.isInCircle(shieldbearer.getLocation(), e.getPlayer().getLocation(), range) && predicate.test(e.getPlayer())) {
				final double fromY = e.getFrom().getY(), toY = e.getTo().getY();
				if (toY > fromY) {
					double dx, dy, dz;
					final Location from = e.getFrom(), to = e.getTo();
					dx = to.getX() - from.getX();
					dy = to.getY() - from.getY();
					dz = to.getZ() - from.getZ();
					e.getPlayer().setVelocity(new Vector((dx * 0.6), (dy * 0.1), (dz * 0.6)));	
				} else if (e.getPlayer().isOnGround()) {
					e.getPlayer().setVelocity(e.getPlayer().getVelocity().multiply(0.6));	
				}	
			}
		}
		
		@EventHandler()
		private void onPlayerArmorStandManipulate(PlayerArmorStandManipulateEvent e) {
			if (e.getRightClicked().equals(shieldbearer)) e.setCancelled(true);
		}
		
		@EventHandler()
		private void onEntityDamage(EntityDamageEvent e) {
			if (e.getEntity().equals(shieldbearer)) e.setCancelled(true);
		}
		
	}
	
	private class Charge extends AbilityTimer {

		private final ActionbarChannel actionbarChannel = newActionbarChannel();
		private int charges = maxCharge;

		private Charge() {
			super(TaskType.REVERSE, (int) (20 * Wreck.calculateDecreasedAmount(25)));
			setBehavior(RestrictionBehavior.PAUSE_RESUME);
		}

		private boolean subtractCharge(int amount) {
			if (!isRunning() && charges > 0) {
				charges = Math.max(0, charges - amount);
				actionbarChannel.update(toString());
				if (charges == 0) {
					start();
				}
				return true;
			} else return false;
		}

		@Override
		protected void run(int count) {
			actionbarChannel.update(Strings.repeat("§e◆", charges).concat(Strings.repeat("§7◇", maxCharge - charges)) + " §7| §c고용§f: " + count + "초");
		}

		@Override
		protected void onEnd() {
			this.charges = maxCharge;
			actionbarChannel.update(toString());
			SoundLib.ITEM_ARMOR_EQUIP_CHAIN.playSound(getPlayer().getLocation());
		}

		@Override
		protected void onSilentEnd() {
			actionbarChannel.update(null);
		}

		@Override
		public String toString() {
			if (!isRunning()) {
				return Strings.repeat("§e◆", charges).concat(Strings.repeat("§7◇", maxCharge - charges));
			} else {
				return Strings.repeat("§e◆", charges).concat(Strings.repeat("§7◇", maxCharge - charges)) + " §7| §c고용§f: " + getCount() + "초";
			}
		}
	}

	private final Charge charge = new Charge();
	
}
