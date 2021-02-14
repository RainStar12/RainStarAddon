package RainStarSynergy;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nullable;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Note;
import org.bukkit.Note.Tone;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByBlockEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import RainStarEffect.Charm;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.SubscribeEvent;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
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
import daybreak.abilitywar.utils.base.math.VectorUtil;
import daybreak.abilitywar.utils.base.math.geometry.Circle;
import daybreak.abilitywar.utils.base.math.geometry.Line;
import daybreak.abilitywar.utils.base.minecraft.damage.Damages;
import daybreak.abilitywar.utils.base.minecraft.nms.IHologram;
import daybreak.abilitywar.utils.base.minecraft.nms.NMS;
import daybreak.abilitywar.utils.library.MaterialX;
import daybreak.abilitywar.utils.library.ParticleLib;
import daybreak.abilitywar.utils.library.PotionEffects;
import daybreak.abilitywar.utils.library.SoundLib;
import daybreak.google.common.base.Predicate;
import daybreak.google.common.base.Strings;
import daybreak.google.common.collect.ImmutableSet;

@AbilityManifest(name = "구미호[완전 둔갑]", rank = Rank.S, species = Species.HUMAN, explain = {
		"§7패시브 §8- §c둔갑§f: 완벽히 둔갑하여 회복 효과를 받을 수 있습니다.",
		"§7공격 §8- §c사랑의 매질§f: 다른 플레이어를 공격할 때마다 표식을 쌓고,",
		" 표식이 3개가 될 때 대상을 5초간 유혹합니다. 유혹 도중엔 표식을 쌓지 못합니다.",
		"§7철괴 좌클릭 §8- §c집착§f: 바라보는 방향에 10초간 원 파티클이 생기고,",
		" 다시 좌클릭 시 끈을 던져 범위의 중심에 가장 가까운 대상에게 돌진합니다.",
		" 이때 범위 내 모든 대상에게 방어 무시 대미지를 입힙니다. 만약 범위 내 대상 중",
		" 유혹 중인 대상이 있다면 유혹을 풀고 남은 시간에 반비례하여 피해를 입힙니다.",
		"§7상태이상 §8- §d유혹§f: 대상이 강제로 나를 바라보게 되고,",
		" 대상이 내게 주는 피해량이 절반으로 감소합니다. 대상을 공격할 때마다",
		" 준 최종 대미지만큼 체력을 회복합니다."
		})

public class NineTailFoxSynergy extends Synergy implements ActiveHandler {
	
	public NineTailFoxSynergy(Participant participant) {
		super(participant);
	}
	
	private final Cooldown cool = new Cooldown(CooldownConfig.getValue());
	private final Map<Player, Stack> stackMap = new HashMap<>();
	private static final Set<Material> nocheck;
	private Participant target = null;
	private static final Circle circle = Circle.of(5, 100);
	private static final Circle heart = Circle.of(5, 40);
	private static final RGB color = new RGB(243, 97, 166);
	private static final RGB color2 = new RGB(99, 58, 1);
	private Location targetblock;
	
	static { nocheck = ImmutableSet.of(MaterialX.AIR.getMaterial(), MaterialX.GRASS.getMaterial(), MaterialX.WATER.getMaterial(),
			MaterialX.LAVA.getMaterial()); }
	
	@Override
	protected void onUpdate(Update update) {
		if (update == Update.RESTRICTION_CLEAR) {
			getPlayer().setHealth(getPlayer().getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue() * 0.75);
		}
	}
	
	public static final SettingObject<Integer> CooldownConfig = 
			synergySettings.new SettingObject<Integer>(NineTailFoxSynergy.class, "Cooldown", 40,
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
    
	public boolean ActiveSkill(Material material, ClickType clicktype) {
	    if (material.equals(Material.IRON_INGOT) && clicktype.equals(ClickType.LEFT_CLICK) && !cool.isCooldown()) {
	    	if (!skill.isRunning()) {
	    		skill.start();
	    	} else {
	    		skill.stop(false);
		    	return true;
	    	}
	    }
		return false;
	}
	
	@SubscribeEvent
	public void onEntityDamage(EntityDamageEvent e) {
		if (e.getEntity().equals(getPlayer())) {
			if (e.getCause().equals(DamageCause.FALL)) {
				getPlayer().sendMessage("§a낙하 대미지를 받지 않습니다.");
				SoundLib.ENTITY_EXPERIENCE_ORB_PICKUP.playSound(getPlayer());
				e.setCancelled(true);
			}
		}
	}
	
	@SubscribeEvent
	public void onEntityDamageByBlock(EntityDamageByBlockEvent e) {
		onEntityDamage(e);
	}
	
	@SubscribeEvent
	public void onEntityDamageByEntity(EntityDamageByEntityEvent e) {
		onEntityDamage(e);
		if (e.getDamager().equals(getPlayer()) && e.getEntity() instanceof Player
				&& !e.isCancelled() && predicate.test(e.getEntity())) {
			target = getGame().getParticipant((Player) e.getEntity());
			if (!target.hasEffect(Charm.registration)) {
				if (stackMap.containsKey(e.getEntity())) {
					if (stackMap.get(e.getEntity()).addStack()) {
						Charm.apply(target, TimeUnit.SECONDS, 5, getPlayer(), 100, 50);
					}
				} else new Stack((Player) e.getEntity()).start();
			}
		}
		if (e.getDamager() instanceof Arrow && e.getEntity() instanceof Player
				&& !e.isCancelled() && predicate.test(e.getEntity())) {
			Arrow arrow = (Arrow) e.getDamager();
			if (getPlayer().equals(arrow.getShooter())) {
				target = getGame().getParticipant((Player) e.getEntity());
				if (!target.hasEffect(Charm.registration)) {
					if (stackMap.containsKey(e.getEntity())) {
						if (stackMap.get(e.getEntity()).addStack()) {
							Charm.apply(target, TimeUnit.SECONDS, 5, getPlayer(), 100, 50);
						}
					} else new Stack((Player) e.getEntity()).start();
				}
			}
		}
	}
	
	@SubscribeEvent(onlyRelevant = true)
	private void onPlayerJoin(PlayerJoinEvent e) {
		for (Stack stack : stackMap.values()) {
			stack.hologram.display(getPlayer());
		}
	}
	
	private final Duration skill = new Duration(200, cool) {

		@Override
		protected void onDurationProcess(int count) {
			targetblock = getPlayer().getTargetBlock(nocheck, 30).getLocation();
			if (count % 2 == 0) {
				for (Location loc : circle.toLocations(targetblock).floor(targetblock.getY())) {
					ParticleLib.REDSTONE.spawnParticle(getPlayer(), loc, color);
				}	
			}
			for (Player p : getPlayer().getWorld().getPlayers()) {
				if (LocationUtil.isInCircle(targetblock, p.getLocation(), 5) && predicate.test(p)) {
					if (!p.hasPotionEffect(PotionEffectType.GLOWING)) {
						SoundLib.BELL.playInstrument(getPlayer(), Note.natural(1, Tone.A));
						PotionEffects.GLOWING.addPotionEffect(p, 9999, 0, true, false, false);	
					}
				} else {
					p.removePotionEffect(PotionEffectType.GLOWING);
				}
			}
		}
		
		@Override
		protected void onDurationEnd() {
			onDurationSilentEnd();
		}
		
		@Override
		protected void onDurationSilentEnd() {
			for (Player p : LocationUtil.getEntitiesInCircle(Player.class, targetblock, 5, predicate)) {
				if (p != null) {
					p.removePotionEffect(PotionEffectType.GLOWING);
					Player nearest = LocationUtil.getNearestEntity(Player.class, targetblock, predicate);
					new Rush(nearest).start();
				} else {
					cool.setCount(cool.getCount() / 2);
				}
			}
		}

	}.setPeriod(TimeUnit.TICKS, 1);
	
	private class Rush extends AbilityTimer {
		
		private Player player;
		
		private Rush(Player player) {
			setPeriod(TimeUnit.TICKS, 1);
			this.player = player;
		}
		
		@Override
		protected void run(int count) {
			if (player != null) {
				if (getPlayer().getLocation().distanceSquared(player.getLocation()) >= 4) {
					for (Location loc : Line.between(player.getLocation(), getPlayer().getLocation(), 1).toLocations(getPlayer().getLocation())) {
						ParticleLib.REDSTONE.spawnParticle(loc, color2);
					}
					getPlayer().setVelocity(VectorUtil.validateVector(player.getLocation().toVector().subtract(getPlayer().getLocation().toVector()).normalize().multiply(3)));	
				} else {
					getPlayer().setVelocity(new Vector(0, 0, 0));
					stop(false);
				}
			}
		}
		
		@Override
		protected void onEnd() {
			onSilentEnd();
		}
		
		@Override
		protected void onSilentEnd() {
			for (Player p : LocationUtil.getEntitiesInCircle(Player.class, targetblock, 5, predicate)) {
				if (getGame().getParticipant(p).hasEffect(Charm.registration)) {
					Damages.damageFixed(p, getPlayer(), Math.max(5, ((100 - getGame().getParticipant(p).getPrimaryEffect(Charm.registration).getDuration()) / 8)));
					getGame().getParticipant(p).removeEffects(Charm.registration);
				} else {
					Damages.damageFixed(p, getPlayer(), 4);
				}
			}
			for (Location loc : heart.toLocations(targetblock).floor(targetblock.getY())) {
				ParticleLib.HEART.spawnParticle(loc, 0, 0, 0, 1, 1);
			}
		}
		
	}
	
	private class Stack extends AbilityTimer {
		
		private final Player player;
		private final IHologram hologram;
		private int stack = 0;
		
		private Stack(Player player) {
			super(30);
			setPeriod(TimeUnit.TICKS, 4);
			this.player = player;
			this.hologram = NMS.newHologram(player.getWorld(), player.getLocation().getX(),
					player.getLocation().getY() + player.getEyeHeight() + 0.6, player.getLocation().getZ(), 
					Strings.repeat("§d♥", stack).concat(Strings.repeat("§d♡", 3 - stack)));
			hologram.display(getPlayer());
			stackMap.put(player, this);
			addStack();
		}

		@Override
		protected void run(int count) {
			hologram.teleport(player.getWorld(), player.getLocation().getX(), 
					player.getLocation().getY() + player.getEyeHeight() + 0.6, player.getLocation().getZ(), 
					player.getLocation().getYaw(), 0);
		}

		private boolean addStack() {
			setCount(30);
			stack++;
			hologram.setText(Strings.repeat("§d♥", stack).concat(Strings.repeat("§d♡", 3 - stack)));
			if (stack >= 3) {
				stop(false);
				return true;
			} else {
				return false;
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

}