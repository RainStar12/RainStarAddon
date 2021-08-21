package RainStarAbility;

import java.util.HashSet;
import java.util.Set;

import javax.annotation.Nullable;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByBlockEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import RainStarEffect.BindingSmoke;
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
import daybreak.abilitywar.game.manager.effect.Stun;
import daybreak.abilitywar.game.module.DeathManager;
import daybreak.abilitywar.game.team.interfaces.Teamable;
import daybreak.abilitywar.utils.base.Formatter;
import daybreak.abilitywar.utils.base.color.RGB;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.base.math.LocationUtil;
import daybreak.abilitywar.utils.base.math.geometry.Circle;
import daybreak.abilitywar.utils.base.minecraft.nms.NMS;
import daybreak.abilitywar.utils.base.minecraft.nms.PickupStatus;
import daybreak.abilitywar.utils.library.ParticleLib;
import daybreak.abilitywar.utils.library.SoundLib;
import daybreak.google.common.base.Predicate;

@AbilityManifest(
		name = "�̶�", rank = Rank.S, species = Species.HUMAN, explain = {
		"��7ȭ�� ���� ��8- ��b���� ǥ�ġ�f: ��ũ�� ä�� Ȱ �߻� �� ������ ��ġ�� ���� ǥ���� �����",
		" $[FIELD_RANGE]ĭ�� ���� ���� �ӹ��� ���� ȿ���� �޴� �ʵ带 ����ϴ�. $[ARROW_COOL]", 
		" ���� ǥ���� ���� ������� ������ ���ŵǰ� ���� ������ 3�ʰ� ������� ���մϴ�.",
		"��7ö�� ��Ŭ�� ��8- ��3���� �����f: ���� ǥ���� �ִ� ������ �����̵��մϴ�. $[COOLDOWN]",
		" �̶� ������ �ڽ��� �ִ� ��ġ�κ��� $[TELEPORT_RANGE]ĭ �̳��� ��� �÷��̾ �Բ�",
		" �̵���Ų �� �����̵��� ��ġ���� ������ ����Ű�� 0.2�� ������ŵ�ϴ�.",
		" ���� ����� �ӹ��� ������ ȿ���� �ް� �־��� ��� 1.5�ʰ� ������ŵ�ϴ�.",
		" ��7���� ��� �ڷ���Ʈ ���Ρ�f: $[TELEPORT_COUNT]",
		"��7�����̻� ��8- ��9�ӹ��� ������f: �̵� �ӵ� �� ������ �������ϴ�.",
		" ���� ��ƼƼ�� ���� ���� �̿��� ��� ���ظ� 1.5��� �ް� �˴ϴ�.",
		},
		summarize = {
		"��7��ũ�� ä�� Ȱ�� �߻��ء�f ���� ��ġ�� ��3���� ǥ�ġ�f�� ����ϴ�. $[ARROW_COOL]",
		"��3ǥ�ġ�f �ֺ����� �̵� �ӵ��� �������� ��9�ӹ��� ������f ȿ���� �޴� �ʵ尡 �����˴ϴ�.",
		"��7ö�� ��Ŭ�� �á�f ���� ǥ���� �ִ� ������ �ֺ� �÷��̾�� �Բ� ��5�����̵���f��",
		"��3���� ǥ�ġ�f�� �ִ� ������ ������ ����Ű�� ���� ��5�����̵���f�� ������ ������ŵ�ϴ�.",
		" $[COOLDOWN]"
		})

public class Mira extends AbilityBase implements ActiveHandler {

	public Mira(Participant participant) {
		super(participant);
	}
	
	public static final SettingObject<Integer> ARROW_COOL = abilitySettings.new SettingObject<Integer>(Mira.class,
			"arrow-cooldown", 30, "# ǥ�� ���� ��Ÿ��") {
		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}

		@Override
		public String toString() {
			return Formatter.formatCooldown(getValue());
		}
	};	
	
	public static final SettingObject<Integer> COOLDOWN = abilitySettings.new SettingObject<Integer>(Mira.class,
			"cooldown", 70, "# ��Ÿ��") {
		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}

		@Override
		public String toString() {
			return Formatter.formatCooldown(getValue());
		}
	};
	
	public static final SettingObject<Integer> FIELD_RANGE = abilitySettings.new SettingObject<Integer>(Mira.class,
			"field-range", 7, "# ǥ�� �ʵ� ����", "��2[��c!��2] ��7����! ��ƼŬ�� ������� �ʽ��ϴ�.") {

		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}

	};
	
	public static final SettingObject<Integer> TELEPORT_RANGE = abilitySettings.new SettingObject<Integer>(Mira.class,
			"teleport-range", 5, "# �ڷ���Ʈ ����") {

		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}

	};
	
	public static final SettingObject<Boolean> TELEPORT_COUNT = abilitySettings.new SettingObject<Boolean>(Mira.class,
			"teleport-count", false, "# ���� ��� �ڷ���Ʈ ����", "# true�� �����Ͻø� �ֺ� ��� �÷��̾ �ƴ�", "  ���� ����� �� �÷��̾ ���� �ڷ���Ʈ�˴ϴ�.") {
		
		@Override
		public String toString() {
                return getValue() ? "��b����" : "��c����";
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
	
	private final Predicate<Entity> predicate2 = new Predicate<Entity>() {
		@Override
		public boolean test(Entity entity) {
			if (entity instanceof Player) {
				if (!entity.equals(getPlayer())) {
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
			}
			return true;
		}

		@Override
		public boolean apply(@Nullable Entity arg0) {
			return false;
		}
	};
	
	private final Circle circle = Circle.of(7, 100);
	private final Cooldown teleCool = new Cooldown(COOLDOWN.getValue(), "���� ����");
	private final Cooldown arrowCool = new Cooldown(ARROW_COOL.getValue(), "ǥ��");
	private final int range = FIELD_RANGE.getValue();
	private final int teleport = TELEPORT_RANGE.getValue();
	private boolean teleportcount = TELEPORT_COUNT.getValue();
	private static final RGB color = RGB.of(25, 147, 168);
	private final ActionbarChannel ac = newActionbarChannel();
	private Arrow arrow;
	private Arrow shootarrow;
	private boolean check = false;
	private final Set<Player> fallcancel = new HashSet<>();
	
	protected void onUpdate(AbilityBase.Update update) {
	    if (update == AbilityBase.Update.RESTRICTION_SET || update == AbilityBase.Update.ABILITY_DESTROY) {
	    	if (arrow != null) {
				arrow.setGlowing(false);
				NMS.setPickupStatus(arrow, PickupStatus.ALLOWED);
				arrow = null;
				ac.update(null);
	    	}
	    } 
	}
	
	private final AbilityTimer inv = new AbilityTimer(20) {
		
		@Override
		public void run(int count) { }
		
	}.setPeriod(TimeUnit.TICKS, 1).register();
	
	private final AbilityTimer passive = new AbilityTimer() {
		
		@Override
		public void run(int count) {
			if (arrow != null) {
				if (arrow.getWorld().getWorldBorder().isInside(arrow.getLocation())) {
					for (Location loc : circle.toLocations(arrow.getLocation()).floor(arrow.getLocation().getY())) {
						ParticleLib.REDSTONE.spawnParticle(getPlayer(), loc, color);
					}
					ParticleLib.SMOKE_LARGE.spawnParticle(arrow.getLocation(), 0, 0, 0, 3, 0);
					for (Player p : LocationUtil.getEntitiesInCircle(Player.class, arrow.getLocation(), range, predicate)) {
						if (!getGame().getParticipant(p).hasEffect(BindingSmoke.registration)) {
							BindingSmoke.apply(getGame().getParticipant(p), TimeUnit.SECONDS, 3);
						} else {
							getGame().getParticipant(p).getPrimaryEffect(BindingSmoke.registration).setCount(30);
						}
					}
					ac.update("��3ǥ�� ��ġ��f: ��5" + arrow.getLocation().getBlockX() + "��f, ��5" + arrow.getLocation().getBlockY() + "��f, ��5" + arrow.getLocation().getBlockZ());
				} else {
					getPlayer().sendMessage("[��c!��f] ��3ǥ�ġ�f�� ���� ��輱 ���� ���� ��������ϴ�.");
					arrowCool.setCount(0);
					activecool.stop(false);
					getPlayer().getInventory().addItem(new ItemStack(Material.ARROW, 1));
					arrow.remove();
					arrow = null;
					ac.update(null);
				}
				if (arrow != null) {
					if (arrow.isDead() && !check && !teleCool.isRunning()) {
						getPlayer().sendMessage("[��c!��f] ��bȭ���f�� ����� ��3ǥ�ġ�f�� ��������ϴ�.");
						arrowCool.setCount(0);
						activecool.stop(false);
						getPlayer().getInventory().addItem(new ItemStack(Material.ARROW, 1));
						arrow = null;
						ac.update(null);
						check = true;
					}
				}
			}
		}
		
	}.setPeriod(TimeUnit.TICKS, 1).register();
	
	private final AbilityTimer activecool = new AbilityTimer(60) {
		
		@Override
		public void run(int count) {
		}
		
	}.setPeriod(TimeUnit.TICKS, 1).register();
	
	@SubscribeEvent
	public void onProjectileHit(ProjectileHitEvent e) {
		if (e.getHitEntity() == null && !arrowCool.isRunning() && NMS.isArrow(e.getEntity()) && getPlayer().equals(e.getEntity().getShooter())) {
			Arrow hitarrow = (Arrow) e.getEntity();
			if (hitarrow == shootarrow) {
				if (arrow != null) {
					arrow.setGlowing(false);
					NMS.setPickupStatus(arrow, PickupStatus.ALLOWED);	
				}
				arrow = (Arrow) e.getEntity();
				NMS.setPickupStatus(arrow, PickupStatus.DISALLOWED);
				arrow.setGlowing(true);
				if (!passive.isRunning()) {
					passive.start();
				}
				arrowCool.start();
				activecool.start();
				check = false;
			}
		}
	}
	
	@SubscribeEvent
	public void onEntityShootBow(EntityShootBowEvent e) {
		if (getPlayer().isSneaking()) {
			shootarrow = (Arrow) e.getProjectile();
		}
	}

	@SubscribeEvent
	public void onEntityDamage(EntityDamageEvent e) {
		if (e.getEntity().equals(getPlayer()) && inv.isRunning()) e.setCancelled(true);
		if (fallcancel.contains(e.getEntity()) && e.getCause() == DamageCause.FALL) {
			e.getEntity().sendMessage("��a���� ������� ���� �ʽ��ϴ�.");
			SoundLib.ENTITY_EXPERIENCE_ORB_PICKUP.playSound((Player) e.getEntity());
			fallcancel.remove(e.getEntity());
			e.setCancelled(true);
		}
	}
	
	@SubscribeEvent
	public void onEntityDamageByBlock(EntityDamageByBlockEvent e) {
		onEntityDamage(e);
	}
	
	@SubscribeEvent
	public void onEntityDamageByEntity(EntityDamageByEntityEvent e) {
		onEntityDamage(e);
	}
	
	public boolean ActiveSkill(Material material, ClickType clicktype) {
		if (material.equals(Material.IRON_INGOT) && clicktype.equals(ClickType.LEFT_CLICK) && !teleCool.isCooldown()) {
			if (arrow != null) {
				if (activecool.isRunning()) {
					getPlayer().sendMessage("[��c!��f] ǥ���� ��ġ�� �ð��� �ʹ� ���� ��3���� �����f�� �� �� �����ϴ�.");
					getPlayer().sendMessage("[��c!��f] ���� �ð�: ��e" + ((activecool.getCount() / 20) + 1) + "��f��");
				} else {
					if (teleportcount) {
						Player p = LocationUtil.getNearestEntity(Player.class, getPlayer().getLocation(), predicate);
						ParticleLib.PORTAL.spawnParticle(p.getLocation(), 0, 0, 0, 200, 0.5);
						ParticleLib.PORTAL.spawnParticle(getPlayer().getLocation(), 0, 0, 0, 200, 0.5);
						p.teleport(arrow.getLocation());
						getPlayer().teleport(arrow.getLocation());
						if (!fallcancel.contains(p)) fallcancel.add(p);
						if (!fallcancel.contains(getPlayer())) fallcancel.add(getPlayer());
						inv.start();
						if (getGame().getParticipant(p).hasEffect(BindingSmoke.registration)) {
							SoundLib.ITEM_CHORUS_FRUIT_TELEPORT.playSound(p, 1, 0.7f);
							Stun.apply(getGame().getParticipant(p), TimeUnit.TICKS, 30);
						} else {
							SoundLib.ITEM_CHORUS_FRUIT_TELEPORT.playSound(p, 1, 1.3f);
							Stun.apply(getGame().getParticipant(p), TimeUnit.TICKS, 4);
						}
						SoundLib.ITEM_CHORUS_FRUIT_TELEPORT.playSound(getPlayer(), 1, 1.3f);
						Stun.apply(getGame().getParticipant(getPlayer()), TimeUnit.TICKS, 4);
						new BukkitRunnable() {
							@Override
							public void run() {
								ParticleLib.EXPLOSION_HUGE.spawnParticle(getPlayer().getLocation());
								getPlayer().getWorld().createExplosion(getPlayer().getLocation(), 1.8f, false, false);
							}	
						}.runTaskLater(AbilityWar.getPlugin(), 1L);
					} else {
						for (Player p : LocationUtil.getEntitiesInCircle(Player.class, getPlayer().getLocation(), teleport, predicate2)) {
							ParticleLib.PORTAL.spawnParticle(p.getLocation(), 0, 0, 0, 200, 0.5);
							p.teleport(arrow.getLocation());
							if (!fallcancel.contains(p)) fallcancel.add(p);
							inv.start();
							if (getGame().getParticipant(p).hasEffect(BindingSmoke.registration)) {
								SoundLib.ITEM_CHORUS_FRUIT_TELEPORT.playSound(p, 1, 0.7f);
								Stun.apply(getGame().getParticipant(p), TimeUnit.TICKS, 30);
							} else {
								SoundLib.ITEM_CHORUS_FRUIT_TELEPORT.playSound(p, 1, 1.3f);
								Stun.apply(getGame().getParticipant(p), TimeUnit.TICKS, 4);
							}
							new BukkitRunnable() {
								@Override
								public void run() {
									ParticleLib.EXPLOSION_HUGE.spawnParticle(getPlayer().getLocation());
									getPlayer().getWorld().createExplosion(getPlayer().getLocation(), 1.8f, false, false);
								}	
							}.runTaskLater(AbilityWar.getPlugin(), 1L);
						}	
					}
					arrow.setGlowing(false);
					NMS.setPickupStatus(arrow, PickupStatus.ALLOWED);
					teleCool.start();
					arrow = null;
					ac.update(null);
					return true;
				}
			} else {
				getPlayer().sendMessage("[��c!��f] ǥ���� �������� �ʾ� ��3���� �����f�� �� �� �����ϴ�.");
			}
		}
		return false;
	}

}