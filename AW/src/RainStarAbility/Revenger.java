package RainStarAbility;

import javax.annotation.Nullable;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import daybreak.abilitywar.AbilityWar;
import daybreak.abilitywar.ability.AbilityBase;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.SubscribeEvent;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.AbstractGame.Participant.ActionbarNotification.ActionbarChannel;
import daybreak.abilitywar.game.module.DeathManager;
import daybreak.abilitywar.game.team.interfaces.Teamable;
import daybreak.abilitywar.utils.base.color.RGB;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.base.math.LocationUtil;
import daybreak.abilitywar.utils.base.math.geometry.Line;
import daybreak.abilitywar.utils.base.minecraft.nms.NMS;
import daybreak.abilitywar.utils.library.ParticleLib;
import daybreak.abilitywar.utils.library.SoundLib;
import daybreak.google.common.base.Predicate;

@AbilityManifest(name = "복수귀", rank = Rank.A, species = Species.UNDEAD, explain = {
		"§7패시브 §8- §c선인§f: 공격 시 모든 피해를 25%만 줍니다.",
		"§7사망 §8- §c한§f: 플레이어에게 사망 시 사망 메시지를 띄우고 다시 부활합니다.",
		" 한 맺힌 존재가 될 때부터 체력을 회복할 수 없고, 선인 효과가 사라집니다.",
		"§7패시브 §8- §c복수§f: 오직 나를 죽인 대상과 싸울 수 있으며,",
		" 대상의 방향이 나타나고, 대상을 공격할 때 모든 피해를 사망 전 최종 피해량에",
		" 비례해서 증가시킵니다. 대상이 나에게 사망할 경우, 자신도 성불합니다.",
		" 대상을 바라볼 때마다 10초 주기로 대상이 실명에 걸리고 나와 같은 방향을 바라봅니다.",
		"§7패시브 §8- §c증오의 씨§f: 대상이 다른 플레이어에게 사망 시",
		" 복수 대상이 대상을 사망시킨 플레이어로 옮겨갑니다."
		},
		summarize = {
		"부활 전까지 내가 주는 모든 공격 피해량이 75%가 감소합니다.",
		"다른 플레이어에게 사망 시 §c복수§f를 시작해, §c복수 대상§f 외에겐 싸우지 못하고",
		"§c복수 대상§f에겐 대상이 날 죽일때 준 최종 피해량에 비례해 추가 피해를 줍니다.",
		"대상을 바라보면 대상은 실명에 걸리고 나와 같은 방향을 바라봅니다.",
		"§c복수§f를 시작하고 나면 체력을 회복할 수 없습니다."
		})

public class Revenger extends AbilityBase {
	
	public Revenger(Participant participant) {
		super(participant);
	}
	
	private boolean checkdeath = true;
	private boolean checkilook = true;
	private double lastdmg;
	private Player target;
	private static final RGB trace = RGB.of(183, 1, 1);
	private Location lastlocation;
	private PotionEffect blind = new PotionEffect(PotionEffectType.BLINDNESS, 25, 0, true, false);
	private final ActionbarChannel ac = newActionbarChannel();
	
	@Override
	protected void onUpdate(Update update) {
		if (update == Update.RESTRICTION_CLEAR) {
			getlocation.start();
		}
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
	
	@SubscribeEvent
	public void onEntityRegainHealth(EntityRegainHealthEvent e) {
		if (!checkdeath && e.getEntity().equals(getPlayer())) {
			e.setCancelled(true);
		}
	}
	
	private final AbilityTimer passive = new AbilityTimer() {
	    	
    	@Override
    	public void run(int count) {
        	for (Location loc : Line.between(target.getLocation(), getPlayer().getLocation(), (int) Math.min(500, (15 * Math.sqrt(target.getLocation().distance(getPlayer().getLocation()))))).toLocations(target.getLocation())) {
    	   		ParticleLib.REDSTONE.spawnParticle(getPlayer(), loc, trace);
        	}	
	    }
    	
	}.setPeriod(TimeUnit.SECONDS, 2).register();
	
	private final AbilityTimer ilook = new AbilityTimer(200) {
		
		@Override
		public void run(int count) {
			Player p = LocationUtil.getEntityLookingAt(Player.class, getPlayer(), 100, predicate);
    		if (p != null && p.equals(target)) {
    			ac.update("§c주시 중");
    			if (checkilook == true) {
        			target.addPotionEffect(blind);
        			SoundLib.AMBIENT_CAVE.playSound(target, 1, 1.5f);
        			NMS.rotateHead(target, target, getPlayer().getLocation().getYaw(), getPlayer().getLocation().getPitch());
        			checkilook = false;
    			}
    		} else {
    			ac.update(null);
    		}
		}
		
		@Override
		public void onEnd() {
			onSilentEnd();
		}
		
		@Override
		public void onSilentEnd() {
			checkilook = true;
			ilook.start();
		}
		
	}.setPeriod(TimeUnit.TICKS, 1).register();
	
	private final AbilityTimer getlocation = new AbilityTimer() {
		
    	@Override
    	public void run(int count) {
    		lastlocation = getPlayer().getLocation();
	    }
		
	}.setPeriod(TimeUnit.SECONDS, 20).register();
	
	@SubscribeEvent
	public void onEntityDamageByEntity(EntityDamageByEntityEvent e) {
		
		if (!e.isCancelled()) {
			if (e.getEntity().equals(getPlayer()) && checkdeath) {
				ParticleLib.SPELL_WITCH.spawnParticle(getPlayer().getLocation(), 0.5, 0.5, 0.5, 1, 30);
			}
			
			if (e.getDamager().equals(getPlayer()) && checkdeath) {
				e.setDamage(e.getDamage() / 4);
			}
		
			if (NMS.isArrow(e.getDamager())) {
				Arrow arrow = (Arrow) e.getDamager();
				if (arrow.getShooter().equals(getPlayer())) {
					if (checkdeath) {
						e.setDamage(e.getDamage() / 4);
					} else {
						if (e.getEntity().equals(target) && target != null) {
							e.setDamage(e.getDamage() + lastdmg);
						} else {
							e.setCancelled(true);
						}
					}
				} else if (!arrow.getShooter().equals(target) && e.getEntity().equals(getPlayer()) && target != null) {
					e.setCancelled(true);
				}
			}
		
			if (getPlayer().getHealth() - e.getFinalDamage() <= 0 && getPlayer().getKiller() instanceof Player && checkdeath == true) {
				target = getPlayer().getKiller();
				if (target != null) {
					Bukkit.broadcastMessage("§f[§c능력§f] §c" + getPlayer().getName() + "§f님의 능력은 §e복수귀§f였습니다.");
					Bukkit.broadcastMessage("§c" + getPlayer().getName() + "§f가 §a" + getPlayer().getKiller().getName() + "§f에게 살해당했습니다. §7컷!");
					Bukkit.broadcastMessage("§c" + getPlayer().getName() + "§f는 이제 §a" + getPlayer().getKiller().getName() + "§f에게 §c복수§f를 준비합니다...");
					lastdmg = Math.min((2 * (e.getFinalDamage() / 3)), 4);
					getPlayer().setHealth(getPlayer().getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue());
					new BukkitRunnable() {
						@Override
						public void run() {
							if (lastlocation != null) {
								getPlayer().teleport(lastlocation);	
							}
						}	
					}.runTaskLater(AbilityWar.getPlugin(), 1L);
					new BukkitRunnable() {
						@Override
						public void run() {
							if (!target.isDead()) target.sendMessage("§7누군가가 쳐다보는 것 같은 기분이 듭니다...");
						}	
					}.runTaskLater(AbilityWar.getPlugin(), 200L);
					checkdeath = false;
					ilook.start();
					e.setCancelled(true);
				}
			}
		
			if (e.getDamager().equals(getPlayer()) && e.getEntity().equals(target) && checkdeath == false) {
				e.setDamage(e.getDamage() + lastdmg);
			}
		
			if (e.getDamager().equals(getPlayer()) && !e.getEntity().equals(target) && checkdeath == false) {
				e.setCancelled(true);
			}	
		
			if (e.getEntity().equals(getPlayer()) && !e.getDamager().equals(target) && checkdeath == false
					&& e.getDamager() instanceof Player) {
				e.setCancelled(true);
			}
		}	
	}
	
	@SubscribeEvent
	public void onPlayerDeath(PlayerDeathEvent e) {
		if (target != null && !checkdeath) {
			if (e.getEntity().equals(target)) {
				if (target.getKiller() == null) {
					getPlayer().setHealth(0);
					passive.stop(false);
					ilook.stop(false);
				} else {
					if (!target.getKiller().equals(getPlayer()) && target.getKiller() instanceof Player) {
						Bukkit.broadcastMessage("§c" + getPlayer().getName() + "§f는 이제 §a" + target.getName() + "§f을 죽인 §a" + target.getKiller().getName() + "§f에게 §c복수§f를 준비합니다...");
						target = target.getKiller();
					}
					if (target.getKiller().equals(getPlayer())) {
						target = null;
						getPlayer().setHealth(0);
						passive.stop(false);
						ilook.stop(false);
					}
					if (!(target.getKiller() instanceof Player)) {
						target = null;
						getPlayer().setHealth(0);
						passive.stop(false);
						ilook.stop(false);
					}
				}
			}	
		}
	}

}