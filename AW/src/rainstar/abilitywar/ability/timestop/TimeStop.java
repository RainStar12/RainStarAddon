package rainstar.abilitywar.ability.timestop;

import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Damageable;
import org.bukkit.entity.Enderman;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByBlockEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityInteractEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.entity.EntityTeleportEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.event.vehicle.VehicleDestroyEvent;
import org.bukkit.event.vehicle.VehicleEnterEvent;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import daybreak.abilitywar.ability.AbilityBase;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.ability.SubscribeEvent.Priority;
import daybreak.abilitywar.ability.Tips.Description;
import daybreak.abilitywar.ability.Tips.Difficulty;
import daybreak.abilitywar.ability.Tips.Level;
import daybreak.abilitywar.ability.Tips.Stats;
import daybreak.abilitywar.ability.SubscribeEvent;
import daybreak.abilitywar.ability.Tips;
import daybreak.abilitywar.ability.decorator.ActiveHandler;
import daybreak.abilitywar.config.ability.AbilitySettings.SettingObject;
import daybreak.abilitywar.config.enums.CooldownDecrease;
import daybreak.abilitywar.game.AbstractGame.GameTimer;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.list.mix.Mix;
import daybreak.abilitywar.game.module.DeathManager;
import daybreak.abilitywar.game.module.ZeroTick;
import daybreak.abilitywar.utils.base.Formatter;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.base.math.LocationUtil;
import daybreak.abilitywar.utils.base.minecraft.entity.health.Healths;
import daybreak.abilitywar.utils.base.minecraft.nms.IHologram;
import daybreak.abilitywar.utils.base.minecraft.nms.NMS;
import daybreak.abilitywar.utils.library.PotionEffects;
import daybreak.abilitywar.utils.library.SoundLib;

@AbilityManifest(name = "시간 정지", rank = Rank.L, species = Species.HUMAN, explain = {
		"§7철괴 우클릭 §8- §b타임 스톱§f: 5초의 시간을 재고 $[DURATION_CONFIG]초간 시간을 멈춥니다.",
		" 시간이 멈춘 동안 모든 플레이어는 행동 불능이 되어 정지 해제 후 정지된 시간동안",
		" 받을 피해를 한 번에 1/5로 줄여서 받습니다. 시간이 정지되기 전 지속 중이던",
		" 능력은 정지가 끝난 이후 연속해서 지속하게 됩니다. $[COOLDOWN_CONFIG]",
		" §7반전 세계 효과§f: $[EFFECT]",
		"§7패시브 §8- §a타임 플로우§f: 시간 정지의 영향을 받지 않습니다.",
		"§7패시브 §8- §3타임 디비전§f: 시간 정지 소유자 한 명당 지속시간이 30%씩 감소합니다.",
		" 줄어든 지속시간의 소수점은 올림 처리되며, 1초 미만으로는 줄지 않습니다.",
		" 또한 한 명당 정지 종료 후 피해 감소량이 줄어듭니다."
		},
		summarize = {
		"§7철괴 우클릭 시§f 시간을 멈춰 자신 혹은 시간 정지 능력자들 외 모든 플레이어의",
		"시간이 멈추게 됩니다. 이 능력은 타게팅 불능 대상도 무시합니다. $[COOLDOWN_CONFIG]"
		})

@Tips(tip = {
        "세상의 시간을 멈춰, 모든 플레이어를 행동하지 못하게 만들고",
        "멈춘 시간동안 공격, 도망, 회복 등 무엇이든 할 수 있는 능력입니다.",
        "시간을 어떻게 사용할 지는 당신의 선택입니다."
}, strong = {
        @Description(subject = "시간 독점", explain = {
        		"다른 플레이어의 개입을 완전히 배제하고 시간을 온전히",
        		"내 것으로 만들어, 모든 능력을 카운팅할 수 있습니다.",
        })
}, weak = {
		@Description(subject = "시간 정지 능력 소유자", explain = {
        		"시간 정지 능력을 보유하고 있는 대상에게",
        		"이 능력은 통하지 않음을 주의하세요."
        })
}, stats = @Stats(offense = Level.TWO, survival = Level.EIGHT, crowdControl = Level.TEN, mobility = Level.ZERO, utility = Level.SEVEN), difficulty = Difficulty.VERY_EASY)

public class TimeStop extends AbilityBase implements ActiveHandler {
	
	public TimeStop(Participant participant) {
		super(participant);
	}
	
	private final Map<Player, Double> damageCounter = new HashMap<>();
	private final Map<Player, Stack> stackMap = new HashMap<>();
	private final Map<Projectile, Vector> velocityMap = new HashMap<>();
	private Set<Player> custominv = new HashSet<>();
	private Set<Player> timestoppers = new HashSet<>();
	private final Cooldown timeStop = new Cooldown(COOLDOWN_CONFIG.getValue(), CooldownDecrease._25);
	private boolean effectboolean = EFFECT.getValue();
	private Duration stopduration = null;
	private static final Vector zerov = new Vector(0, 0, 0);
	private ArmorStand hologram;
	private Location defaultLocation;
	private long worldtime = 0;
	
	public static final SettingObject<Integer> COOLDOWN_CONFIG = abilitySettings.new SettingObject<Integer>(TimeStop.class,
			"cooldown", 120, "# 쿨타임") {
		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}

		@Override
		public String toString() {
			return Formatter.formatCooldown(getValue());
		}
	};	
	
	public static final SettingObject<Integer> DURATION_CONFIG = abilitySettings.new SettingObject<Integer>(TimeStop.class,
			"duration", 7, "# 시간을 몇 초 정지시킬지 설정합니다.") {
		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}
	};
	
	public static final SettingObject<Boolean> EFFECT = abilitySettings.new SettingObject<Boolean>(TimeStop.class,
			"effect-config", false, "# 이펙트 여부", "# 시간 정지 시 반전 세계 시점을 볼 수 있을지 선택합니다.") {
		
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
						|| (getGame() instanceof DeathManager.Handler && ((DeathManager.Handler) getGame()).getDeathManager().isExcluded(entity.getUniqueId()))) {
					return false;
				}
				if (getGame().getParticipant(entity.getUniqueId()).hasAbility()) {
					AbilityBase ab = getGame().getParticipant(entity.getUniqueId()).getAbility();
					if (ab.getClass().equals(TimeStop.class)) {
						return false;
					} else if (ab.getClass().equals(Mix.class)) {
						Mix mix = (Mix) ab;
						if (mix.hasAbility()) {
							if (!mix.hasSynergy()) {
								if (mix.getFirst().getClass().equals(TimeStop.class)
										|| mix.getSecond().getClass().equals(TimeStop.class)) {
										return false;
									}
							}
						}
					}
				}
			}
			return true;
		}
	};
	
	protected void onUpdate(AbilityBase.Update update) {
	    if (update == AbilityBase.Update.RESTRICTION_CLEAR) {
	    	if (!checkTimestoppers.isRunning()) checkTimestoppers.start();
	    	defaultLocation = getPlayer().getLocation().clone().add(0, 100, 0);
			this.hologram = getPlayer().getWorld().spawn(defaultLocation, ArmorStand.class);
			hologram.setVisible(false);
			hologram.setGravity(false);
			hologram.setInvulnerable(true);
			NMS.removeBoundingBox(hologram);
			hologram.setCustomNameVisible(true);
			hologram.setCustomName("§b⌚");
	    }
	    if (update == Update.RESTRICTION_SET || update == Update.ABILITY_DESTROY) {
	    	if (hologram != null) {
		    	hologram.remove();	
	    	}
	    }
	}
	
    private final AbilityTimer checkTimestoppers = new AbilityTimer() {
    	
    	@Override
		public void run(int count) {
			for (Participant participants : getGame().getParticipants()) {
				if (participants.hasAbility()) {
					AbilityBase ab = participants.getAbility();
					if (ab.getClass().equals(Mix.class)) {
						Mix mix = (Mix) ab;
						if (!mix.hasSynergy()) {
							if (mix.getFirst() != null && mix.getSecond() != null) {
								if (mix.getFirst().getClass().equals(TimeStop.class)) timestoppers.add(participants.getPlayer());
								if (mix.getSecond().getClass().equals(TimeStop.class)) timestoppers.add(participants.getPlayer());
								if (!mix.getFirst().getClass().equals(TimeStop.class) && !mix.getSecond().getClass().equals(TimeStop.class)) timestoppers.remove(participants.getPlayer());	
							} else timestoppers.remove(participants.getPlayer());
						} else timestoppers.remove(participants.getPlayer());
					} else {
						if (ab.getClass().equals(TimeStop.class)) timestoppers.add(participants.getPlayer());
						else timestoppers.remove(participants.getPlayer());
					}
				} else if (!participants.hasAbility()) timestoppers.remove(participants.getPlayer());
			}
    	}
    
    }.setPeriod(TimeUnit.TICKS, 1).register();
	
	private AbilityTimer timecount = new AbilityTimer(100) {
		
		@Override
		protected void run(int count) {
			hologram.teleport(getPlayer().getLocation().clone().add(0, 2.1, 0));
		}
		
		@Override
		protected void onStart() {
			PotionEffects.SLOW.addPotionEffect(getPlayer(), 100, 1, false);
			timecount1.start();
		}
		
		@Override
		protected void onEnd() {
			SoundLib.ENTITY_EXPERIENCE_ORB_PICKUP.playSound(getPlayer());
		    hologram.teleport(defaultLocation);
		    stopduration.start();
		}
		
	}.setPeriod(TimeUnit.TICKS, 1);
	
	private final AbilityTimer timecount1 = new AbilityTimer(5) {
		@Override
		protected void run(int arg0) {
			for (Participant participants : getGame().getParticipants()) {
				if (arg0 % 2 == 0) {
					SoundLib.BLOCK_NOTE_BLOCK_SNARE.playSound(participants.getPlayer(), 1, 2f);
				} else {
					SoundLib.BLOCK_NOTE_BLOCK_SNARE.playSound(participants.getPlayer(), 1, 1.7f);
				}
			}
		}
		
	}.setPeriod(TimeUnit.SECONDS, 1);
	
	@SubscribeEvent(priority = Priority.HIGHEST)
	public void onPlayerMove(PlayerMoveEvent e) {
		if (stopduration != null) {
			if (stopduration.isRunning() && predicate.test(e.getPlayer())) {
				e.setTo(e.getFrom());
			}	
		}
	}
	
	@SubscribeEvent(priority = Priority.HIGHEST)
	public void onEntityPickupItem(EntityPickupItemEvent e) {
		if (stopduration != null) {
			if (stopduration.isRunning() && predicate.test(e.getEntity())) {
				e.setCancelled(true);
			}	
		}
	}
	
	@SubscribeEvent(priority = Priority.HIGHEST)
	public void onPlayerDrop(PlayerDropItemEvent e) {
		if (stopduration != null) {
			if (stopduration.isRunning() && predicate.test(e.getPlayer())) {
				e.setCancelled(true);
			}	
		}
	}
	
	@SubscribeEvent(priority = Priority.HIGHEST)
	public void onClick(InventoryClickEvent e) {
		if (stopduration != null) {
			if (stopduration.isRunning() && predicate.test(e.getWhoClicked())) {
				e.setCancelled(true);
			}	
		}
	}
	
	@SubscribeEvent(priority = Priority.HIGHEST)
	public void onEntityShootBow(EntityShootBowEvent e) {
		if (stopduration != null) {
			if (stopduration.isRunning() && predicate.test(e.getEntity())) {
				e.setCancelled(true);
			}	
		}
	}
	
	@SubscribeEvent(priority = Priority.HIGHEST)
	public void onEntityRegainHealth(EntityRegainHealthEvent e) {
		if (stopduration != null) {
			if (stopduration.isRunning() && predicate.test(e.getEntity())) {
				e.setCancelled(true);
			}	
		}
	}
	
	@SubscribeEvent(priority = Priority.HIGHEST)
	public void onPlayerSwapHandItems(PlayerSwapHandItemsEvent e) {
		if (stopduration != null) {
			if (stopduration.isRunning() && predicate.test(e.getPlayer())) {
				e.setCancelled(true);
			}	
		}
	}
	
	@SubscribeEvent(priority = Priority.HIGHEST)
	public void onEntityInteract(EntityInteractEvent e) {
		if (stopduration.isRunning() && predicate.test(e.getEntity())) {
			e.setCancelled(true);
		}
	}
	
	@SubscribeEvent(priority = Priority.HIGHEST)
	public void onVehicleEnter(VehicleEnterEvent e) {
		if (stopduration != null) {
			if (stopduration.isRunning() && predicate.test(e.getEntered())) {
				e.setCancelled(true);
			}	
		}
	}
	
	@SubscribeEvent(priority = Priority.HIGHEST)
	public void onVehicleDestory(VehicleDestroyEvent e) {
		if (stopduration != null) {
			if (stopduration.isRunning() && predicate.test(e.getAttacker())) {
				e.setCancelled(true);
			}	
		}
	}
	
	@SubscribeEvent(priority = Priority.HIGHEST)
	public void onBlockBreak(BlockBreakEvent e) {
		if (stopduration != null) {
			if (stopduration.isRunning() && predicate.test(e.getPlayer())) {
				e.setCancelled(true);
			}	
		}
	}
	
	@SubscribeEvent(priority = Priority.HIGHEST)
	public void onBlockPlace(BlockPlaceEvent e) {
		if (stopduration != null) {
			if (stopduration.isRunning() && predicate.test(e.getPlayer())) {
				e.setCancelled(true);
			}	
		}
	}
	
	@SubscribeEvent(priority = Priority.HIGHEST)
	public void onEntityDamage(PlayerItemHeldEvent e) {
		if (stopduration != null) {
			if (stopduration.isRunning() && predicate.test(e.getPlayer())) {
				e.setCancelled(true);
			}	
		}
	}
	
	@SubscribeEvent(priority = Priority.HIGHEST)
	public void onProjectileLaunch(ProjectileLaunchEvent e) {
		if (stopduration != null) {
			if (stopduration.isRunning()) {
				if (predicate.test((Entity) e.getEntity().getShooter())) {
					e.setCancelled(true);
				} else {
					new AbilityTimer(2) {
						
						@Override
						protected void run(int count) {
						}
						
						@Override
						protected void onEnd() {
							velocityMap.put(e.getEntity(), e.getEntity().getVelocity());
							e.getEntity().setGravity(false);
							e.getEntity().setVelocity(zerov);
						}

						@Override
						protected void onSilentEnd() {
							velocityMap.put(e.getEntity(), e.getEntity().getVelocity());
							e.getEntity().setGravity(false);
							e.getEntity().setVelocity(zerov);
						}
					}.setPeriod(TimeUnit.TICKS, 1).start();
				}
			}
		}
	}
	
	@SubscribeEvent(priority = Priority.HIGHEST)
	public void onEntityDamageByBlock(EntityDamageByBlockEvent e) {
		if (stopduration != null) {
			if (stopduration.isRunning() && e.getEntity() instanceof Damageable) {
				e.setCancelled(true);
			}	
		}
	}
	
	@SubscribeEvent(priority = Priority.HIGHEST)
	public void onTeleport(EntityTeleportEvent e) {
		if (e.getEntity() instanceof Enderman && stopduration.isRunning()) {
			e.setCancelled(true);
		}
	}
	
	@SubscribeEvent(priority = Priority.HIGHEST)
	public void onEntityDamage(EntityDamageEvent e) {
		if (stopduration != null) {
			if (stopduration.isRunning() && e.getEntity() instanceof Damageable) {
				e.setCancelled(true);
			}	
		}
	}
	
	@SubscribeEvent(priority = Priority.HIGHEST)
	public void onEntityDamageByEntity(EntityDamageByEntityEvent e) {
		if (stopduration != null) {
			if (stopduration.isRunning() && e.getEntity() instanceof Enderman) {
				for (Player player : LocationUtil.getConflictingEntities(Player.class, e.getEntity(), predicate)) {
					if (player.equals(LocationUtil.getNearestEntity(Player.class, e.getEntity().getLocation(), predicate))) {
						player.damage(e.getDamage(), e.getDamager());
					}
				}
				e.setCancelled(true);
				e.setDamage(0);
			}
			if (stopduration.isRunning() && e.getEntity() instanceof Player) {
				e.setCancelled(true);
				if (!custominv.contains(e.getEntity())) {
					if (NMS.isArrow(e.getDamager())) {
						Arrow arrow = (Arrow) e.getDamager();
						arrow.remove();
			      	}
					double damage = (damageCounter.getOrDefault(e.getEntity(), Double.valueOf(0.0D))).doubleValue();
					damageCounter.put((Player) e.getEntity(), Double.valueOf(damage + e.getFinalDamage()));
					if (!getGame().hasModule(ZeroTick.class)) {
						new CustomInv((Player) e.getEntity()).start();	
					}
				}
		    }	
		}
	}
	
	public boolean ActiveSkill(Material material, AbilityBase.ClickType clicktype) {
		if (material.equals(Material.IRON_INGOT) && clicktype.equals(ClickType.RIGHT_CLICK) && !timeStop.isCooldown() && 
			!timecount.isRunning()) {
			if (stopduration != null) {
				if (!stopduration.isDuration()) {
					timecount.start();
				    stopduration = new InvTimer();
				    return true;
				}
			} else {
				timecount.start();
			    stopduration = new InvTimer();
			    return true;	
			}
		} 
		return false;
	}
	
	private class Stack extends AbilityTimer {
		
		private final Player player;
		private final IHologram hologram;
		private final DecimalFormat df = new DecimalFormat("0.00");
		
		private Stack(Player player) {
			setPeriod(TimeUnit.TICKS, 1);
			this.player = player;
			this.hologram = NMS.newHologram(player.getWorld(), player.getLocation().getX(),
					player.getLocation().getY() + player.getEyeHeight() + 0.6, player.getLocation().getZ());
			hologram.setText("§c§l0");
			hologram.display(getPlayer());
			stackMap.put(player, this);
		}

		@Override
		protected void run(int count) {
			hologram.teleport(player.getWorld(), player.getLocation().getX(), 
					player.getLocation().getY() + player.getEyeHeight() + 0.6, player.getLocation().getZ(), 
					player.getLocation().getYaw(), 0);
			if (damageCounter.containsKey(player)) {
				hologram.setText("§c§l" + df.format((damageCounter.get(player) / Math.max(1, 5 - (timestoppers.size() - 1)))));
			}
		}
		
		@Override
		protected void onEnd() {
			onSilentEnd();
		}
		
		@Override
		protected void onSilentEnd() {
			hologram.unregister();
			stackMap.remove(player);
		}
		
	}
	
	class InvTimer extends Duration {

		public InvTimer() {
			super((int) Math.max(1, Math.ceil((DURATION_CONFIG.getValue() * Math.pow(0.7, timestoppers.size() - 1)))), timeStop);
			setPeriod(TimeUnit.SECONDS, 1);
		}
		
		@Override
		protected void onDurationProcess(int arg0) {
			getPlayer().getWorld().setTime(worldtime);
			for (Participant participants : getGame().getParticipants()) {
				if (predicate.test(participants.getPlayer())) {
					if (participants.hasAbility()) {
						AbilityBase ab = participants.getAbility();
						for (GameTimer t : ab.getRunningTimers()) {
							if (t instanceof Duration.DurationTimer || t instanceof Cooldown.CooldownTimer) {
								t.pause();
							} else {
								if (!ab.isRestricted()) {
									ab.setRestricted(true);	
								}
							}
						}
					}
				}
			}
		}
		
		@Override
		protected void onDurationStart() {
			worldtime = getPlayer().getWorld().getTime();
			for (Participant participants : getGame().getParticipants()) {
				if (predicate.test(participants.getPlayer())) {
					new Stack(participants.getPlayer()).start();
					if (effectboolean) {
						Enderman enderman = participants.getPlayer().getWorld().spawn(participants.getPlayer().getLocation().clone().subtract(0, 0.8, 0), Enderman.class);
						enderman.setSilent(true);
						enderman.setAI(false);
						PotionEffects.INVISIBILITY.addPotionEffect(enderman, 9999, 1, false);
						new AbilityTimer((int) Math.max(1, Math.ceil((DURATION_CONFIG.getValue() * Math.pow(0.7, timestoppers.size() - 1)))) * 20) {
								
							@Override
							public void onStart() {
								NMS.setCamera(participants.getPlayer(), enderman);	
							}
							
							@Override
							public void onEnd() {
								onSilentEnd();
							}
								
							@Override
							public void onSilentEnd() {
								NMS.setCamera(participants.getPlayer(), participants.getPlayer());
								enderman.remove();
							}
								
						}.setPeriod(TimeUnit.TICKS, 1).start();	
					} else {
						PotionEffects.BLINDNESS.addPotionEffect(participants.getPlayer(), ((int) Math.max(1, Math.ceil((DURATION_CONFIG.getValue() * Math.pow(0.7, timestoppers.size() - 1)))) * 20), 2, false);
					}
				}
				
				if (predicate.test(participants.getPlayer())) {
					participants.getPlayer().leaveVehicle();
					if (participants.hasAbility()) {
						AbilityBase ab = participants.getAbility();
						for (GameTimer t : ab.getTimers()) {
							t.pause();
						}
					}
					PotionEffects.SLOW_DIGGING.addPotionEffect(participants.getPlayer(), 400, 30, true);
					NMS.sendTitle(participants.getPlayer(), "§b시간이 멈췄다.", "", 0, 40, 20);	
				} else {
					getParticipant().attributes().TARGETABLE.setValue(false);
				}
			}
			
			for (Entity entity : getPlayer().getWorld().getEntities()) {
				if (entity instanceof Projectile) {
					velocityMap.put((Projectile) entity, entity.getVelocity());
					entity.setGravity(false);
					entity.setVelocity(zerov);
				}
			}
		}
		
		@Override
		protected void onDurationEnd() {
			onDurationSilentEnd();
		}
		
		@Override
		protected void onDurationSilentEnd() {
			for (Player p : damageCounter.keySet()) {
				Healths.setHealth((Player) p, Math.max(1, p.getHealth() - (damageCounter.get(p) / Math.max(1, 5 - (timestoppers.size() - 1)))));
			}
			damageCounter.clear();
			
			for (Participant participants : getGame().getParticipants()) {
				if (predicate.test(participants.getPlayer())) {
					stackMap.get(participants.getPlayer()).stop(false);
					if (participants.hasAbility()) {
						AbilityBase ab = participants.getAbility();
						for (GameTimer t : ab.getTimers()) {
							t.resume();
						}
						if (ab.isRestricted()) {
							ab.setRestricted(false);	
						}
					}
					
					NMS.clearTitle(participants.getPlayer());
					participants.getPlayer().removePotionEffect(PotionEffectType.SLOW_DIGGING);	
					participants.getPlayer().removePotionEffect(PotionEffectType.BLINDNESS);
				} else {
					getParticipant().attributes().TARGETABLE.setValue(true);
				}
			}
			
			for (Entity entity : getPlayer().getWorld().getEntities()) {
				if (entity instanceof Projectile) {
					velocityMap.forEach(Projectile::setVelocity);
					entity.setGravity(true);
					velocityMap.clear();
				}
			}
		}
	}
	
	class CustomInv extends AbilityTimer {
		
		Player player;
		
		public CustomInv(Player player) {
			super(7);
			setPeriod(TimeUnit.TICKS, 1);
			this.player = player;
		}
		
		@Override
		protected void onStart() {
			if (!custominv.contains(player)) {
				custominv.add(player);			
			}
		}
		
		@Override
		protected void onEnd() {
			onSilentEnd();
		}
		
		@Override
		protected void onSilentEnd() {
			if (custominv.contains(player)) {
				custominv.remove(player);			
			}
		}
		
	}
}