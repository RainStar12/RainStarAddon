package rainstar.abilitywar.ability;

import static rainstar.abilitywar.system.config.RainStarAbilitySetting.rsAbilitySettings;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.StringJoiner;

import javax.annotation.Nullable;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent.RegainReason;
import org.bukkit.scheduler.BukkitRunnable;

import daybreak.abilitywar.AbilityWar;
import daybreak.abilitywar.ability.AbilityBase;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.SubscribeEvent;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.ability.decorator.ActiveHandler;
import daybreak.abilitywar.ability.decorator.TargetHandler;
import daybreak.abilitywar.config.ability.AbilitySettings.SettingObject;
import daybreak.abilitywar.game.AbstractGame.GameTimer;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.AbstractGame.Participant.ActionbarNotification.ActionbarChannel;
import daybreak.abilitywar.game.event.participant.ParticipantDeathEvent;
import daybreak.abilitywar.game.list.mix.Mix;
import daybreak.abilitywar.game.module.DeathManager;
import daybreak.abilitywar.utils.base.Formatter;
import daybreak.abilitywar.utils.base.collect.SetUnion;
import daybreak.abilitywar.utils.base.color.RGB;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.base.concurrent.SimpleTimer.TaskType;
import daybreak.abilitywar.utils.base.math.LocationUtil;
import daybreak.abilitywar.utils.base.math.geometry.Circle;
import daybreak.abilitywar.utils.base.minecraft.entity.health.Healths;
import daybreak.abilitywar.utils.base.random.Random;
import daybreak.abilitywar.utils.library.ParticleLib;
import daybreak.abilitywar.utils.library.SoundLib;
import daybreak.google.common.base.Predicate;

@AbilityManifest(name = "라이벌", rank = Rank.L, species = Species.HUMAN, explain = {
        "§7패시브 §8- §c단 한 명의§f: 적에게 피해를 받으면 대상을 §c라이벌§f로 지정합니다.",
        " §c라이벌§f 외에게 피해를 주고받지 않습니다. §6지속 시간 §f: $[RIVAL_DURATION]초",
        " §c라이벌§f을 처치하면 §e챔피언 스택§f을 $[WIN_DURATION]만큼 얻습니다.",
        "§7철괴 우클릭 §8- §b매칭 성사§f: §c라이벌§f이 없다면 바라보는 대상을 §c라이벌§f로 지정합니다.",
        " §c라이벌§f 지속시간을 $[RIVAL_ADDITIONAL_DURATION]초 증가시킵니다. §c라이벌§f의 체력을 내 체력까지 회복시키고,",
        " §c라이벌§f에게 순간 이동합니다. 남은 지속시간 동안 §c라이벌§f의 능력을 획득하고",
        " 공격력이 $[PERCENTAGE]% 증가합니다. $[COOLDOWN]",
        "§7철괴 좌클릭 §8- §e챔피언§f: §e챔피언 스택§f을 전부 소모하고 그만큼의 지속시간 동안",
        " 지금까지 처치했던 모든 §c라이벌§f들의 능력을 사용할 수 있습니다.",
        "§a[§e능력 제공자§a] §dSakay0r1_Ir0ha"
        },
        summarize = {
        "자신을 공격한 자를 §c§n라이벌§f로 지정해 §c라이벌§f 외에게 피해를 주고받지 않습니다.",
        "§7철괴 우클릭§f으로 §c라이벌§f이나 바라본 대상을 새로 §c라이벌§f로 지정하여",
        "§c라이벌§f에게 이동 후 §c라이벌§f의 능력을 사용합니다.",
        "§c라이벌§f을 처치 시 모든 스킬을 비활성화하고 여태껏 처치한 모든 §c라이벌§f의 능력을",
        "잠시간 사용할 수 있습니다."
        })
public class Rival extends AbilityBase implements ActiveHandler, TargetHandler {
	
	public Rival(Participant participant) {
		super(participant);
	}
	
	public static final SettingObject<Integer> COOLDOWN = 
			rsAbilitySettings.new SettingObject<Integer>(Rival.class, "cooldown", 100,
            "# 매칭 성사 쿨타임", "# 단위: 초") {
        @Override
        public boolean condition(Integer value) {
            return value >= 0;
        }
        
        @Override
        public String toString() {
            return Formatter.formatCooldown(getValue());
        }
    };
    
	public static final SettingObject<Double> RIVAL_DURATION = 
			rsAbilitySettings.new SettingObject<Double>(Rival.class, "rival-duration", 5.0,
            "# 라이벌 지속 시간", "# 단위: 초") {
        @Override
        public boolean condition(Double value) {
            return value >= 0;
        }
    };
    
	public static final SettingObject<Double> RIVAL_ADDITIONAL_DURATION = 
			rsAbilitySettings.new SettingObject<Double>(Rival.class, "rival-additional-duration", 20.0,
            "# 매칭 성사 라이벌 추가 지속 시간", "# 단위: 초") {
        @Override
        public boolean condition(Double value) {
            return value >= 0;
        }
    };
    
	public static final SettingObject<Integer> PERCENTAGE = 
			rsAbilitySettings.new SettingObject<Integer>(Rival.class, "damage-increase-percentage", 15,
            "# 매칭 성사 공격력 증가", "# 단위: %") {
        @Override
        public boolean condition(Integer value) {
            return value >= 0;
        }
    };
    
	public static final SettingObject<Integer> WIN_DURATION = 
			rsAbilitySettings.new SettingObject<Integer>(Rival.class, "win-duration", 20,
            "# 챔피언 스택 시간", "# 단위: 초") {
        @Override
        public boolean condition(Integer value) {
            return value >= 0;
        }
    };
    
    private final Cooldown cooldown = new Cooldown(COOLDOWN.getValue());
    private final int rivalduration = (int) (RIVAL_DURATION.getValue() * 20);
    private final int rivaladdduration = (int) (RIVAL_ADDITIONAL_DURATION.getValue() * 20);
    private final int winduration = WIN_DURATION.getValue();
    private final double multiply = 1 + (PERCENTAGE.getValue() * 0.01);
    private final ActionbarChannel ac = newActionbarChannel(), ac2 = newActionbarChannel();
    private final DecimalFormat df = new DecimalFormat("0.0");
    private final Random random = new Random();
	private final Circle circle = Circle.of(1, 25);
    private List<AbilityBase> abilities = new ArrayList<>();
    private AbilityBase rivalability;
    private Player rival;
    private int championstack = 0;
    
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
			}
			return true;
		}

		@Override
		public boolean apply(@Nullable Entity arg0) {
			return false;
		}
	};
	
	@Override
	public Set<GameTimer> getTimers() {
		if (champion.isRunning()) {
			Set<GameTimer> timers = new HashSet<>();
			for (AbilityBase ability : abilities) {
				timers.addAll(SetUnion.union(super.getTimers(), ability.getTimers()));	
			}
			if (rivalability != null) timers.addAll(SetUnion.union(super.getTimers(), rivalability.getTimers()));
			else timers.addAll(super.getTimers());
			return timers;
		}
		return rivalability != null ? SetUnion.union(super.getTimers(), rivalability.getTimers()) : super.getTimers();
	}

	@Override
	public Set<GameTimer> getRunningTimers() {
		if (champion.isRunning()) {
			Set<GameTimer> timers = new HashSet<>();
			for (AbilityBase ability : abilities) {
				timers.addAll(SetUnion.union(super.getRunningTimers(), ability.getRunningTimers()));	
			}
			if (rivalability != null) timers.addAll(SetUnion.union(super.getRunningTimers(), rivalability.getRunningTimers()));
			else timers.addAll(super.getRunningTimers());
			return timers;
		}
		return rivalability != null ? SetUnion.union(super.getRunningTimers(), rivalability.getRunningTimers()) : super.getRunningTimers();
	}

	@Override
	public boolean usesMaterial(Material material) {
		if (champion.isRunning()) {
			for (AbilityBase ability : abilities) {
				if (ability != null) {
					return ability.usesMaterial(material);
				}	
			}
			if (rivalability != null) {
				return rivalability.usesMaterial(material);
			}
			return super.usesMaterial(material);
		}
		if (rivalability != null) {
			return rivalability.usesMaterial(material);
		}
		return super.usesMaterial(material);
	}

	@Override
	protected void onUpdate(Update update) {
		if (update == Update.RESTRICTION_CLEAR) {
			if (rivalability != null) {
				rivalability.setRestricted(false);
			}	
		} else if (update == Update.RESTRICTION_SET) {
			if (champion.isRunning()) {
				if (!abilities.isEmpty()) {
					for (AbilityBase ability : abilities) {
						ability.setRestricted(true);
					}
				}
			}
			if (rivalability != null) {
				rivalability.setRestricted(true);
			}	
		} else if (update == Update.ABILITY_DESTROY) {
			if (champion.isRunning()) {
				if (!abilities.isEmpty()) {
					for (AbilityBase ability : abilities) {
						ability.destroy();
					}
					abilities.clear();
				}
			}
			if (rivalability != null) {
				rivalability.destroy();
				rivalability = null;
			}
		}
	}
    
	@Override
	public boolean ActiveSkill(Material material, ClickType clickType) {
		if (champion.isRunning()) {
			if (!abilities.isEmpty()) {
				boolean actived = false;
				boolean dontchange = false;
				for (AbilityBase ability : abilities) {
					 if (ability instanceof ActiveHandler) {
						 if (actived == true) dontchange = true;
						 actived = ((ActiveHandler) ability).ActiveSkill(material, clickType);
					 }
				}
				if (dontchange == true) actived = true;
				if (actived) return true;	
			}
		}
		if (material == Material.IRON_INGOT && clickType == ClickType.LEFT_CLICK) {
			if (championstack > 0) {
				champion.start();
				champion.setCount(championstack * winduration);
				championstack = 0;
				return true;
			} else getPlayer().sendMessage("§c[§e!§c] §e챔피언 스택이 없습니다.");
		}
		if (Rival.this.rivalability == null) {
			if (material == Material.IRON_INGOT && clickType == ClickType.RIGHT_CLICK && !cooldown.isCooldown()) {
				if (rival != null || LocationUtil.getEntityLookingAt(Player.class, getPlayer(), 100, predicate) != null) {
					if (rival == null) rival = LocationUtil.getEntityLookingAt(Player.class, getPlayer(), 100, predicate);
					if (!rivaltimer.isRunning()) rivaltimer.start();
					rivaltimer.setCount(rivaltimer.getCount() + rivaladdduration);
					if (getGame().getParticipant(rival).hasAbility()) {
						AbilityBase ab = getGame().getParticipant(rival).getAbility();
						AbilityBase myab = null;
						if (ab.getClass().equals(Mix.class)) {
							Mix mix = (Mix) ab;
							Mix myMix = (Mix) getParticipant().getAbility();
							if (mix.hasSynergy()) {
								myab = mix.getSynergy();
							} else {
								if (myMix.getFirst().equals(Rival.this)) {
									myab = mix.getFirst();
								} else if (myMix.getSecond().equals(Rival.this)) {
									myab = mix.getSecond();
								}	
							}
						} else myab = ab;
						
						try {
							this.rivalability = AbilityBase.create(myab.getClass(), getParticipant());
							this.rivalability.setRestricted(false);
							getPlayer().sendMessage("§3[§b!§3] §b라이벌의 능력을 복제했습니다. 능력: §a" + rivalability.getDisplayName());
						} catch (ReflectiveOperationException e) {
							e.printStackTrace();
						}
					} else cooldown.start();
					
					if (rival.getHealth() < getPlayer().getHealth()) {
						final EntityRegainHealthEvent event = new EntityRegainHealthEvent(getPlayer(), getPlayer().getHealth() - rival.getHealth(), RegainReason.CUSTOM);
						Bukkit.getPluginManager().callEvent(event);
						if (!event.isCancelled()) {
							Healths.setHealth(rival, getPlayer().getHealth());
						}
					}
					
					new BukkitRunnable() {
						@Override
						public void run() {
							getPlayer().teleport(rival);
						}	
					}.runTaskLater(AbilityWar.getPlugin(), 1L);
				} else {
					getPlayer().sendMessage("§c[§e!§c] §f바라보고 있는 곳에 아무런 대상이 없습니다.");
					return false;
				}
				return true;
			}
		} else {
			return rivalability instanceof ActiveHandler && ((ActiveHandler) rivalability).ActiveSkill(material, clickType);	
		}
		return false;
	}
	
	@Override
	public void TargetSkill(Material material, LivingEntity entity) {
		if (champion.isRunning()) {
			if (!abilities.isEmpty()) {
				for (AbilityBase ability : abilities) {
					if (ability instanceof TargetHandler) {
						((TargetHandler) ability).TargetSkill(material, entity);
					}	
				}
			}
		}
		if (rivalability != null) {
			if (rivalability instanceof TargetHandler) {
			((TargetHandler) rivalability).TargetSkill(material, entity);
			}
		}
	}
    
	private AbilityTimer champion = new AbilityTimer(TaskType.REVERSE, winduration) {
		
		@Override
		public void onStart() {
			ParticleLib.TOTEM.spawnParticle(getPlayer().getLocation(), 0.5, 1, 0.5, 25, 1);
			SoundLib.UI_TOAST_CHALLENGE_COMPLETE.playSound(getPlayer().getLocation(), 1, 2);
			ac2.update("§e챔피언§7: §f" + winduration + "초");
			for (AbilityBase ability : abilities) {
				ability.setRestricted(false);
			}
	    	getPlayer().sendMessage("§c========= §e흡수 능력 §c=========");
			final StringJoiner joiner = new StringJoiner("§f, ");
			for (AbilityBase ability : abilities) {
				joiner.add(ability.getDisplayName());
			}
			getPlayer().sendMessage(joiner.toString());
			getPlayer().sendMessage("§c=============================");
		}
		
		@Override
		public void run(int count) {
			ac2.update("§e챔피언§7: §f" + count + "초");
		}
		
		@Override
		public void onEnd() {
			onSilentEnd();
		}
		
		@Override
		public void onSilentEnd() {
			ac2.update(null);
			for (AbilityBase ability : abilities) {
				ability.setRestricted(true);
			}
		}
		
	}.setPeriod(TimeUnit.SECONDS, 1).register();
	
    private AbilityTimer rivaltimer = new AbilityTimer(TaskType.REVERSE, rivalduration) {
    	
    	private RGB color;
    	
    	@Override
    	public void onStart() {
    		color = RGB.of(random.nextInt(254) + 1, random.nextInt(254) + 1, random.nextInt(254) + 1);
    	}
    	
    	@Override
    	public void run(int count) {
    		if (count % 5 == 0) {
				for (Location loc : circle.toLocations(getPlayer().getLocation()).floor(getPlayer().getLocation().getY())) {
					ParticleLib.REDSTONE.spawnParticle(loc, color);
				}
				for (Location loc : circle.toLocations(rival.getLocation()).floor(rival.getLocation().getY())) {
					ParticleLib.REDSTONE.spawnParticle(loc, color);
				}
    		}
    		ac.update("§c라이벌 §e" + rival.getName() + "§f " + df.format(count / 20));
    	}
    	
    	@Override
    	public void onEnd() {
    		onSilentEnd();
    	}
    	
    	@Override
    	public void onSilentEnd() {
    		ac.update(null);
    		if (Rival.this.rivalability != null) {
    			Rival.this.rivalability.destroy();
    			Rival.this.rivalability = null;
    			cooldown.start();
    		}
    		rival = null;
    	}
    	
	}.setPeriod(TimeUnit.TICKS, 1).register();
	
	@SubscribeEvent
	public void onParticipantDeath(ParticipantDeathEvent e) {
		if (e.getPlayer().equals(rival) && getPlayer().equals(e.getPlayer().getKiller())) {
			AbilityBase ab = getGame().getParticipant(rival).getAbility();
			AbilityBase myab = null;
			if (ab.getClass().equals(Mix.class)) {
				Mix mix = (Mix) ab;
				Mix myMix = (Mix) getParticipant().getAbility();
				if (mix.hasSynergy()) {
					myab = mix.getSynergy();
				} else {
					if (myMix.getFirst().equals(Rival.this)) {
						myab = mix.getFirst();
					} else if (myMix.getSecond().equals(Rival.this)) {
						myab = mix.getSecond();
					}	
				}
			} else myab = ab;
			try {
				abilities.add(AbilityBase.create(myab.getClass(), getParticipant()));
			} catch (ReflectiveOperationException e1) {
				e1.printStackTrace();
			}
			rivaltimer.setCount(0);
			championstack++;
			ac2.update("§e챔피언§7: §c" + championstack);
		}
	}
    
	@SubscribeEvent
	public void onEntityDamageByEntity(EntityDamageByEntityEvent e) {
		Player damager = null;
		if (e.getDamager() instanceof Projectile) {
			Projectile projectile = (Projectile) e.getDamager();
			if (projectile.getShooter() instanceof Player) damager = (Player) projectile.getShooter();
		} else if (e.getDamager() instanceof Player) damager = (Player) e.getDamager();
		
		if (rival == null) {
			if (e.getEntity().equals(getPlayer()) && damager != null) {
				rival = damager;
				rivaltimer.start();
			}
		} else {
			if (getPlayer().equals(damager) && e.getEntity() instanceof Player && !e.getEntity().equals(rival)) {
				e.setCancelled(true);
			}
			if (getPlayer().equals(e.getEntity()) && !rival.equals(damager)) {
				e.setCancelled(true);
			}
			if (Rival.this.rivalability != null && getPlayer().equals(damager) && e.getEntity().equals(rival)) {
				rivaltimer.setCount(rivalduration);
				e.setDamage(e.getDamage() * multiply);
			}
		}
	}

}