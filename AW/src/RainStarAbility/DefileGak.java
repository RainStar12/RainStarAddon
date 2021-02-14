package RainStarAbility;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nullable;

import org.bukkit.Material;
import org.bukkit.Note;
import org.bukkit.Note.Tone;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerJoinEvent;

import daybreak.abilitywar.ability.AbilityBase;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.Materials;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.ability.SubscribeEvent;
import daybreak.abilitywar.ability.decorator.ActiveHandler;
import daybreak.abilitywar.config.ability.AbilitySettings.SettingObject;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.AbstractGame.Participant.ActionbarNotification.ActionbarChannel;
import daybreak.abilitywar.game.module.DeathManager;
import daybreak.abilitywar.game.team.interfaces.Teamable;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.base.math.LocationUtil;
import daybreak.abilitywar.utils.base.minecraft.damage.Damages;
import daybreak.abilitywar.utils.base.minecraft.entity.health.Healths;
import daybreak.abilitywar.utils.base.minecraft.nms.IHologram;
import daybreak.abilitywar.utils.base.minecraft.nms.NMS;
import daybreak.abilitywar.utils.library.ParticleLib;
import daybreak.abilitywar.utils.library.SoundLib;
import daybreak.google.common.base.Predicate;
import daybreak.google.common.base.Strings;

@AbilityManifest(name = "모독각", rank = Rank.B, species = Species.HUMAN, explain = {
		"§7다이아 우클릭 §8- §c모독§f: 주변 $[RANGE_CONFIG]칸 내 플레이어의 체력 1칸을 감소시킵니다.",
		" 능력을 사용할 때마다 준 피해의 총량만큼 나도 체력이 감소됩니다.",
		" 만약 능력으로 누군가를 처형했다면, 내가 받을 고정 피해는 사라집니다.",
		" 대상이 처형 가능한 상태일 때 체력을 감소시키는 대신 고정 1칸의 피해를 입힙니다.",
		" 일정 시간 내에 다른 플레이어를 공격하지 않은 채 4명 이상을 연속 처형했다면",
		" 위기 상태 표시가 강화되어 모독으로 처형할 수 있는 적에게만 표시됩니다.",
		"§7패시브 §8- §c각 재는중§f: 체력이 3칸 이하인 플레이어 위에 위기 상태 표시가 뜹니다."
		})

@Materials(materials = {
		Material.DIAMOND
	})

public class DefileGak extends AbilityBase implements ActiveHandler {

	public DefileGak(Participant participant) {
		super(participant);
	}

	public static final SettingObject<Integer> RANGE_CONFIG
	= abilitySettings.new SettingObject<Integer>(DefileGak.class,
			"range", 5, "# 피해를 입힐 범위") {
		@Override
		public boolean condition(Integer value) {
			return value >= 0;
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
	
	private int damage = 0;
	private boolean killed = false;
	private int killcount = 0;
	private boolean upgradepassive = false;
	private final ActionbarChannel ac = newActionbarChannel();
	private final Map<Player, Lowhealth> lowhealthMap = new HashMap<>();
	
	protected void onUpdate(AbilityBase.Update update) {
	    if (update == AbilityBase.Update.RESTRICTION_CLEAR) {
	    	passive.start();
	    }
	}
	
    private final AbilityTimer passive = new AbilityTimer() {
    	
    	@Override
		public void run(int count) {
			for (Participant participant : getGame().getParticipants()) {
	    		if (!lowhealthMap.containsKey(participant.getPlayer())) {
    				if (predicate.test(participant.getPlayer())) {
    					if (upgradepassive) {
        					if (participant.getPlayer().getHealth() <= 2) {
        						new Lowhealth(participant.getPlayer()).start();
        					}	
    					} else {
        					if (participant.getPlayer().getHealth() <= 6) {
        						new Lowhealth(participant.getPlayer()).start();
        					}
    					}
    				}
	    		} else {
	    			if (upgradepassive) {
		    			if (participant.getPlayer().getHealth() > 2) {
		    				lowhealthMap.get(participant.getPlayer()).stop(false);
		    			}
	    			} else {
		    			if (participant.getPlayer().getHealth() > 6) {
		    				lowhealthMap.get(participant.getPlayer()).stop(false);
		    			}
	    			}
	    		}
			}
    	}
    	
    }.setBehavior(RestrictionBehavior.PAUSE_RESUME).setPeriod(TimeUnit.TICKS, 1).register();
    
    private final AbilityTimer killcounter = new AbilityTimer(200) {
    	
    	@Override
		public void run(int count) {
    		if (killcount >= 4) {
    			SoundLib.ENTITY_PLAYER_LEVELUP.playSound(getPlayer(), 1, 1.2f);
    			upgradepassive = true;
    			this.stop(false);
    		}
    	}
    	
    	@Override
    	public void onEnd() {
    		onSilentEnd();
    	}
    	
    	@Override
    	public void onSilentEnd() {
    		killcount = 0;
    		ac.update(null);
    	}
    	
    }.setBehavior(RestrictionBehavior.PAUSE_RESUME).setPeriod(TimeUnit.TICKS, 1).register();
	
	public boolean ActiveSkill(Material material, AbilityBase.ClickType clicktype) {
	    if (material.equals(Material.DIAMOND) && clicktype.equals(ClickType.RIGHT_CLICK)) {
	    	for (Player p : LocationUtil.getNearbyEntities(Player.class, getPlayer().getLocation(), RANGE_CONFIG.getValue(), RANGE_CONFIG.getValue(), predicate)) {
	    		if (p.getHealth() - 2 <= 0) {
	    			Damages.damageFixed(p, getPlayer(), 2);
	    			if (p.isDead()) {
		    			SoundLib.ENTITY_GHAST_WARN.playSound(p, 1, 0.5f);
			    		NMS.broadcastEntityEffect(p, (byte) 2);
			    		ParticleLib.SMOKE_LARGE.spawnParticle(p.getLocation(), 0, 0, 0, 100, 0);
			    		damage += 2;
		    			killed = true;
		    			if (!upgradepassive) killcount++;	
	    			}
	    		} else {
		    		Healths.setHealth(p, p.getHealth() - 2);
		    		NMS.broadcastEntityEffect(p, (byte) 2);
		    		ParticleLib.SMOKE_LARGE.spawnParticle(p.getLocation(), 0, 0, 0, 100, 0);
		    		damage += 2;
	    		}
	    	}
	    	if (damage != 0) {
	    		if (killed) {
					SoundLib.GUITAR.playInstrument(getPlayer(), Note.sharp(0, Tone.C));
					SoundLib.GUITAR.playInstrument(getPlayer(), Note.sharp(1, Tone.F));
	    			if (!upgradepassive) {
		    			ac.update("§c킬 카운터§f: " + Strings.repeat("§4✕", killcount));
	    				killcounter.start();
	    			}
	    			killed = false;
	    		} else {
	    			if (!upgradepassive) {
	    				killcount = 0;
		    			ac.update(null);
	    			}
		    		Healths.setHealth(getPlayer(), getPlayer().getHealth() - damage);
		    		NMS.broadcastEntityEffect(getPlayer(), (byte) 2);	
	    		}
	    		damage = 0;
	    		return true;
	    	} else {
	    		getPlayer().sendMessage("§4[§c!§4] §f주위 §a" + RANGE_CONFIG.getValue() + "§f칸 내에 §8모독§f할 수 있는 플레이어가 없습니다.");
	    		return false;
	    	}
	    }
	    return false;
	}
	
	@SubscribeEvent(onlyRelevant = true)
	private void onPlayerJoin(PlayerJoinEvent e) {
		for (Lowhealth lowhealth : lowhealthMap.values()) {
			lowhealth.hid = true;
		}
	}
	
	@SubscribeEvent
	public void onEntityDamageByEntity(EntityDamageByEntityEvent e) {
		if (killcounter.isRunning() && !upgradepassive) {
			if (e.getDamager().equals(getPlayer())) {
				killcounter.stop(false);
			}
			if (e.getDamager() instanceof Projectile) {
				Projectile p = (Projectile) e.getDamager();
				if (getPlayer().equals(p.getShooter()) && !e.getEntity().equals(getPlayer())) {
					killcounter.stop(false);
				}
			}
		}
	}
	
	private class Lowhealth extends AbilityTimer {
		
		private final Player player;
		private final IHologram hologram;
		private boolean hid = false;
		
		private Lowhealth(Player player) {
			super();
			setPeriod(TimeUnit.TICKS, 2);
			this.player = player;
			this.hologram = NMS.newHologram(player.getWorld(), player.getLocation().getX(), player.getLocation().getY() + player.getEyeHeight() + 0.6, player.getLocation().getZ(), "");
			hologram.display(getPlayer());
			if (upgradepassive) hologram.setText("§4처형 가능");
			else hologram.setText("§c위기 상태");
			DefileGak.this.lowhealthMap.put(player, this);
		}
		
		@Override
		protected void run(int count) {
			if (NMS.isInvisible(player)) {
				if (!hid) {
					this.hid = true;
					hologram.hide(getPlayer());
				}
			} else {
				if (hid) {
					this.hid = false;
					hologram.display(getPlayer());
				}
				hologram.teleport(player.getWorld(), player.getLocation().getX(), player.getLocation().getY() + player.getEyeHeight() + 0.6, player.getLocation().getZ(), player.getLocation().getYaw(), 0);
			}
		}

		@Override
		protected void onEnd() {
			onSilentEnd();
		}

		@Override
		protected void onSilentEnd() {
			hologram.unregister();
			DefileGak.this.lowhealthMap.remove(player);
		}
		
	}
}
