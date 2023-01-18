package rainstar.abilitywar.ability;

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
import rainstar.abilitywar.effect.BindingSmoke;

@AbilityManifest(
		name = "미라", rank = Rank.S, species = Species.HUMAN, explain = {
		"§7화살 적중 §8- §b도약 표식§f: 웅크린 채로 활 발사 시 적중한 위치에 도약 표식을 만들어",
		" $[FIELD_RANGE]칸의 범위 내에 §9§n속박의 연막§f 효과를 받는 필드를 만듭니다. $[ARROW_COOL]", 
		" 도약 표식은 새로 만들어질 때마다 갱신되고 공간 도약을 3초간 사용하지 못합니다.",
		"§7철괴 좌클릭 §8- §3공간 도약§f: 도약 표식이 있는 곳으로 순간이동합니다. $[COOLDOWN]",
		" 이때 기존에 자신이 있던 위치로부터 $[TELEPORT_RANGE]칸 이내의 모든 플레이어를 함께",
		" 이동시킨 뒤 순간이동한 위치에서 폭발을 일으키고 0.2초 기절시킵니다.",
		" 만약 대상이 §9§n속박의 연막§f의 효과를 받고 있었을 경우 1.5초간 기절시킵니다.",
		" §7단일 대상 텔레포트 여부§f: $[TELEPORT_COUNT]",
		"§1[§9속박의 연막§1]§f 이동 속도 및 점프가 느려집니다.",
		" 또한 엔티티에 대한 피해 이외의 모든 피해를 1.5배로 받게 됩니다.",
		},
		summarize = {
		"§7웅크린 채로 활을 발사해§f 적중 위치에 §3도약 표식§f을 만듭니다. $[ARROW_COOL]",
		"§3표식§f 주변에는 이동 속도가 느려지는 §9§n속박의 연막§f 효과를 받는 필드가 생성됩니다.",
		"§7철괴 좌클릭 시§f 도약 표식이 있는 곳으로 주변 플레이어와 함께 §5순간이동§f해",
		"§3도약 표식§f이 있던 지점에 폭발을 일으키고 같이 §5순간이동§f한 대상들을 기절시킵니다.",
		" $[COOLDOWN]"
		})

public class Mira extends AbilityBase implements ActiveHandler {

	public Mira(Participant participant) {
		super(participant);
	}
	
	public static final SettingObject<Integer> ARROW_COOL = abilitySettings.new SettingObject<Integer>(Mira.class,
			"arrow-cooldown", 30, "# 표식 생성 쿨타임") {
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
			"cooldown", 70, "# 쿨타임") {
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
			"field-range", 7, "# 표식 필드 범위", "§2[§c!§2] §7주의! 파티클이 변경되지 않습니다.") {

		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}

	};
	
	public static final SettingObject<Integer> TELEPORT_RANGE = abilitySettings.new SettingObject<Integer>(Mira.class,
			"teleport-range", 5, "# 텔레포트 범위") {

		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}

	};
	
	public static final SettingObject<Boolean> TELEPORT_COUNT = abilitySettings.new SettingObject<Boolean>(Mira.class,
			"teleport-count", false, "# 단일 대상 텔레포트 여부", "# true로 변경하시면 주변 모든 플레이어가 아닌", "  가장 가까운 한 플레이어만 같이 텔레포트됩니다.") {
		
		@Override
		public String toString() {
                return getValue() ? "§b켜짐" : "§c꺼짐";
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
	private final Cooldown teleCool = new Cooldown(COOLDOWN.getValue(), "공간 도약");
	private final Cooldown arrowCool = new Cooldown(ARROW_COOL.getValue(), "표식");
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
					ac.update("§3표식 위치§f: §5" + arrow.getLocation().getBlockX() + "§f, §5" + arrow.getLocation().getBlockY() + "§f, §5" + arrow.getLocation().getBlockZ());
				} else {
					getPlayer().sendMessage("[§c!§f] §3표식§f이 세계 경계선 밖을 지나 사라졌습니다.");
					arrowCool.setCount(0);
					activecool.stop(false);
					getPlayer().getInventory().addItem(new ItemStack(Material.ARROW, 1));
					arrow.remove();
					arrow = null;
					ac.update(null);
				}
				if (arrow != null) {
					if (arrow.isDead() && !check && !teleCool.isRunning()) {
						getPlayer().sendMessage("[§c!§f] §b화살§f이 사라져 §3표식§f이 사라졌습니다.");
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
			e.getEntity().sendMessage("§a낙하 대미지를 받지 않습니다.");
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
					getPlayer().sendMessage("[§c!§f] 표식이 설치된 시간이 너무 빨라 §3공간 도약§f을 할 수 없습니다.");
					getPlayer().sendMessage("[§c!§f] 남은 시간: §e" + ((activecool.getCount() / 20) + 1) + "§f초");
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
								getPlayer().getWorld().createExplosion(getPlayer().getLocation().getX(), getPlayer().getLocation().getY(), getPlayer().getLocation().getZ(), 1.8f, false, false);
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
									getPlayer().getWorld().createExplosion(getPlayer().getLocation().getX(), getPlayer().getLocation().getY(), getPlayer().getLocation().getZ(), 1.8f, false, false);
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
				getPlayer().sendMessage("[§c!§f] 표식이 존재하지 않아 §3공간 도약§f을 할 수 없습니다.");
			}
		}
		return false;
	}

}