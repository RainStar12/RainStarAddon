package RainStarSynergy;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.util.EulerAngle;
import org.bukkit.util.Vector;

import daybreak.abilitywar.AbilityWar;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.ability.decorator.ActiveHandler;
import daybreak.abilitywar.config.ability.AbilitySettings.SettingObject;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.AbstractGame.Participant.ActionbarNotification.ActionbarChannel;
import daybreak.abilitywar.game.list.mix.synergy.Synergy;
import daybreak.abilitywar.game.module.DeathManager;
import daybreak.abilitywar.game.module.Wreck;
import daybreak.abilitywar.game.team.interfaces.Teamable;
import daybreak.abilitywar.utils.base.color.RGB;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.base.math.FastMath;
import daybreak.abilitywar.utils.base.math.LocationUtil;
import daybreak.abilitywar.utils.base.math.geometry.Line;
import daybreak.abilitywar.utils.base.minecraft.damage.Damages;
import daybreak.abilitywar.utils.base.minecraft.version.ServerVersion;
import daybreak.abilitywar.utils.base.random.Random;
import daybreak.abilitywar.utils.library.MaterialX;
import daybreak.abilitywar.utils.library.ParticleLib;
import daybreak.abilitywar.utils.library.SoundLib;
import daybreak.google.common.base.Predicate;
import daybreak.google.common.base.Strings;
import daybreak.google.common.collect.ImmutableMap;

@AbilityManifest(name = "왕관", rank = Rank.S, species = Species.OTHERS, explain = {
		"§e방패병§f을 $[MAX_CHARGE]만큼 고용해 다니며, 전부 사용 시 20초 후 고용합니다.",
		"철괴 우클릭으로 §e방패병§f을 하나씩 내보내, 생명체를 밀쳐내며",
		"5초 후 $[DAMAGE]의 §c폭발 피해§f와 함께 §4자폭§f시킵니다. 좌클릭 시 해고합니다.",
		"§e방패병§f의 $[RANGE]칸 내의 플레이어는 위압감 탓에 느려집니다.",
		"$[CHANCE]% 확률로, 특수한 방패병이 나올 가능성이 있습니다.",
		"§8[§7철§8] §f맞닿는 생명체가 있다면 대기 시간 없이 폭발합니다.",
		"§2[§a사슬§2] §f세 명의 방패 병사가 일렬로 나갑니다.",
		"§3[§b다이아몬드§3] §f위압감과 폭발 범위가 증가합니다.",
		"§5[§d네더라이트§5] §f폭발 피해가 1.3배 증가하고 폭발 위치로 순간 이동합니다."
		})
public class Crown extends Synergy implements ActiveHandler {
	
	public Crown(Participant participant) {
		super(participant);
	}
	
	private static final ImmutableMap<Integer, String> shieldcolor = ImmutableMap.<Integer, String>builder()
			.put(0, "§e◆")
			.put(1, "§7◆")
			.put(2, "§a◆")
			.put(3, "§b◆")
			.put(4, "§5◆")
			.build();
	
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
    
	public static final SettingObject<Integer> CHANCE = 
			synergySettings.new SettingObject<Integer>(Crown.class, "chance", 20,
            "# 특수 병사 확률", "# 단위: %") {
        @Override
        public boolean condition(Integer value) {
            return value >= 0;
        }
    };
	
	@Override
	public boolean ActiveSkill(Material material, ClickType clickType) {
		if (material == Material.IRON_INGOT) {
			if (clickType == ClickType.RIGHT_CLICK) {
				if (charge.charges > 0) {
					if (Crown.this.charge.subtractCharge(1)) {	
						new Shieldbearer(shieldbearers.get(0)).start();	
						return true;
					}
				}
			}
			if (clickType == ClickType.LEFT_CLICK) {
				if (charge.charges > 0) {
					if (Crown.this.charge.subtractCharge(1)) {
						getPlayer().sendMessage("§c[§e!§c] §f방패병을 해고하였습니다.");
						return true;
					}
				}
			}
		}
		return false;
	}
	
	private List<Integer> shieldbearers = new ArrayList<>();
    
	private Random random = new Random();
	private final int chance = CHANCE.getValue();
    private final int maxCharge = MAX_CHARGE.getValue();
	private final double damage = DAMAGE.getValue();
	private final double range = RANGE.getValue();
	private static final double radians = Math.toRadians(90);
	
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
		private final int value;
		private HashMap<ArmorStand, Vector> diff;
		private final Set<ArmorStand> armorStands = new HashSet<>();
		
		public Shieldbearer(int value) {
			super(TaskType.REVERSE, 100);
			setPeriod(TimeUnit.TICKS, 1);
			this.value = value;
		}
		
		@Override
		public void onStart() {
			Bukkit.getPluginManager().registerEvents(this, AbilityWar.getPlugin());
			final Location playerLocation = getPlayer().getLocation();
			direction = playerLocation.getDirection();
			final Location lineTarget = playerLocation.clone().add(direction.clone().setY(0).normalize().multiply(3.75));
			if (value == 2) {
				for (Vector vector : Line.between(playerLocation, lineTarget, 1)) {
					final double originX = vector.getX();
					final double originZ = vector.getZ();
					armorStands.add(getPlayer().getWorld().spawn(playerLocation.clone().add(vector.clone()
							.setX(rotateX(originX, originZ, radians))
							.setZ(rotateZ(originX, originZ, radians))), ArmorStand.class)
					);
					armorStands.add(getPlayer().getWorld().spawn(playerLocation.clone().add(vector.clone()
							.setX(rotateX(originX, originZ, radians * 3))
							.setZ(rotateZ(originX, originZ, radians * 3))), ArmorStand.class)
					);
				}
			}
			final Vector centerVector = lineTarget.toVector();
			this.shieldbearer = getPlayer().getWorld().spawn(lineTarget, ArmorStand.class);
			if (ServerVersion.getVersion() >= 10) {
				shieldbearer.setInvulnerable(true);
				shieldbearer.setCollidable(false);
			}
			shieldbearer.setVisible(false);

			final EulerAngle eulerAngle = new EulerAngle(Math.toRadians(270), Math.toRadians(270), 0);
			
			if (value == 2) {
				for (ArmorStand armorStand : armorStands) {
					if (ServerVersion.getVersion() >= 10) {
						armorStand.setInvulnerable(true);
						armorStand.setCollidable(false);
					}
					armorStand.setBasePlate(false);
					armorStand.setArms(true);
					armorStand.setVisible(false);
					armorStand.setRightArmPose(eulerAngle);
					armorStand.setGravity(false);
					final EntityEquipment equipment = armorStand.getEquipment();
					equipment.setItemInMainHand(new ItemStack(Material.SHIELD));
					equipment.setHelmet(MaterialX.CHAINMAIL_HELMET.createItem());
					diff.put(armorStand, armorStand.getLocation().toVector().subtract(centerVector).add(direction.clone()));
				}	
			}
			shieldbearer.setBasePlate(false);
			shieldbearer.setArms(true);
			shieldbearer.setVisible(false);
			shieldbearer.setRightArmPose(eulerAngle);
			shieldbearer.setGravity(true);
			final EntityEquipment equipment = shieldbearer.getEquipment();
			equipment.setItemInMainHand(new ItemStack(Material.SHIELD));
			switch(value) {
			case 0:
				equipment.setHelmet(MaterialX.GOLDEN_HELMET.createItem());
				break;
			case 1:
				equipment.setHelmet(MaterialX.IRON_HELMET.createItem());
				break;
			case 2:
				equipment.setHelmet(MaterialX.CHAINMAIL_HELMET.createItem());
				break;
			case 3:
				equipment.setHelmet(MaterialX.DIAMOND_HELMET.createItem());
				break;
			case 4:
				if (MaterialX.NETHERITE_HELMET.isSupported()) equipment.setHelmet(MaterialX.NETHERITE_HELMET.createItem());
				else {
					ItemStack leather = new ItemStack(Material.LEATHER_HELMET, 1);
					LeatherArmorMeta meta = (LeatherArmorMeta) leather.getItemMeta();
					meta.setColor(RGB.of(64, 0, 64).getColor());
					leather.setItemMeta(meta);
					equipment.setHelmet(leather);
				}
				break;
			}
			push = direction.clone().multiply(2).setY(0);
		}
		
		@Override
		public void run(int count) {
			if (count >= 80) {
				shieldbearer.setVelocity(direction);
				if (value == 2) {
					Location centerLocation = shieldbearer.getLocation();
					for (ArmorStand armorStand : armorStands) {
						if (!armorStand.equals(shieldbearer) && diff.containsKey(armorStand)) {
							armorStand.teleport(centerLocation.clone().add(diff.get(armorStand)));
						}
					}
				}
			} else {
				shieldbearer.setGravity(false);
				if (value == 2) {
					for (ArmorStand armorStand : armorStands) {
						if (!armorStand.equals(shieldbearer) && diff.containsKey(armorStand)) {
							armorStand.setGravity(false);
						}
					}
				}
			}
			for (Entity entity : LocationUtil.getConflictingEntities(Entity.class, shieldbearer, predicate)) {
				entity.setVelocity(push);
				if (value == 1) this.stop(false);
			}
			if (count <= 20) {
				shieldbearer.setGlowing(count % 4 <= 1);
				if (value == 2) {
					for (ArmorStand armorStand : armorStands) {
						if (!armorStand.equals(shieldbearer)) {
							armorStand.setGlowing(count % 4 <= 1);
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
			double realRange = range;
			if (value == 3) realRange += 3;
			for (Damageable damageable : LocationUtil.getNearbyEntities(Damageable.class, shieldbearer.getLocation(), realRange, realRange, predicate)) {
				Damages.damageExplosion(damageable, getPlayer(), (float) (value == 4 ? damage * 1.3 : damage));
			}
			if (value == 2) {
				for (ArmorStand armorstands : armorStands) {
					for (Damageable damageable : LocationUtil.getNearbyEntities(Damageable.class, armorstands.getLocation(), range, range, predicate)) {
						Damages.damageExplosion(damageable, getPlayer(), (float) damage);
					}
					ParticleLib.EXPLOSION_HUGE.spawnParticle(armorstands.getLocation());
					SoundLib.ENTITY_GENERIC_EXPLODE.playSound(armorstands.getLocation());
					armorstands.remove();
				}
				armorStands.clear();
			}
			ParticleLib.EXPLOSION_HUGE.spawnParticle(shieldbearer.getLocation());
			SoundLib.ENTITY_GENERIC_EXPLODE.playSound(shieldbearer.getLocation());
			if (value == 4) getPlayer().teleport(shieldbearer);
			shieldbearer.remove();
			HandlerList.unregisterAll(this);
		}
		
		@EventHandler()
		public void onPlayerMove(PlayerMoveEvent e) {
			double realRange = range;
			if (value == 3) realRange += 3;
			if (value == 2) {
				for (ArmorStand armorstands : armorStands) {
					if (LocationUtil.isInCircle(armorstands.getLocation(), e.getPlayer().getLocation(), range) && predicate.test(e.getPlayer())) {
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
			}
			if (LocationUtil.isInCircle(shieldbearer.getLocation(), e.getPlayer().getLocation(), realRange) && predicate.test(e.getPlayer())) {
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
			if (armorStands.contains(e.getRightClicked())) e.setCancelled(true);
		}
		
		@EventHandler()
		private void onEntityDamage(EntityDamageEvent e) {
			if (e.getEntity().equals(shieldbearer)) e.setCancelled(true);
			if (armorStands.contains(e.getEntity())) e.setCancelled(true);
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
				shieldbearers.remove(0);
				actionbarChannel.update(toString());
				if (charges == 0) {
					start();
				}
				return true;
			} else return false;
		}

		@Override
		protected void onStart() {
			shieldbearers.clear();
		}
		
		@Override
		protected void run(int count) {
			actionbarChannel.update(Strings.repeat("§e◆", charges).concat(Strings.repeat("§7◇", maxCharge - charges)) + " §7| §c고용§f: " + count + "초");
		}

		@Override
		protected void onEnd() {
			this.charges = maxCharge;
			for (int a = 0; a < maxCharge; a++) {
				shieldbearers.add(random.nextInt(100) < chance ? random.nextInt(4) + 1 : 0);
			}
			actionbarChannel.update(toString());
			SoundLib.ITEM_ARMOR_EQUIP_CHAIN.playSound(getPlayer().getLocation());
			SoundLib.ITEM_ARMOR_EQUIP_GOLD.playSound(getPlayer().getLocation());
			SoundLib.ITEM_ARMOR_EQUIP_DIAMOND.playSound(getPlayer().getLocation());
			SoundLib.ITEM_ARMOR_EQUIP_IRON.playSound(getPlayer().getLocation());
			SoundLib.ITEM_ARMOR_EQUIP_LEATHER.playSound(getPlayer().getLocation());
		}

		@Override
		protected void onSilentEnd() {
			actionbarChannel.update(null);
		}

		@Override
		public String toString() {
			if (!isRunning()) {
				String string = "";
				for (Integer integer : shieldbearers) {
					string = string + shieldcolor.get(integer);
				}
				return string.concat(Strings.repeat("§7◇", maxCharge - charges));
			} else {
				return Strings.repeat("§7◇", maxCharge - charges) + " §7| §c고용§f: " + getCount() + "초";
			}
		}
	}

	private final Charge charge = new Charge();
	
	private double rotateX(double x, double z, double radians) {
		return (x * FastMath.cos(radians)) + (z * FastMath.sin(radians));
	}

	private double rotateZ(double x, double z, double radians) {
		return (-x * FastMath.sin(radians)) + (z * FastMath.cos(radians));
	}
	
}
