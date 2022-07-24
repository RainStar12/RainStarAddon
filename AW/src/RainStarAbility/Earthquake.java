package RainStarAbility;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.annotation.Nullable;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Damageable;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import daybreak.abilitywar.ability.AbilityBase;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.AbilityBase.AbilityTimer;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.ability.decorator.ActiveHandler;
import daybreak.abilitywar.config.ability.AbilitySettings.SettingObject;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.module.DeathManager;
import daybreak.abilitywar.game.team.interfaces.Teamable;
import daybreak.abilitywar.utils.base.Formatter;
import daybreak.abilitywar.utils.base.color.RGB;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.base.math.LocationUtil;
import daybreak.abilitywar.utils.base.math.VectorUtil;
import daybreak.abilitywar.utils.base.math.geometry.Circle;
import daybreak.abilitywar.utils.base.minecraft.nms.NMS;
import daybreak.abilitywar.utils.library.ParticleLib;
import daybreak.abilitywar.utils.library.PotionEffects;
import daybreak.abilitywar.utils.library.SoundLib;
import daybreak.google.common.base.Predicate;

@AbilityManifest(name = "지진", rank = Rank.S, species = Species.OTHERS, explain = {
		"철괴 우클릭 시 $[WAIT_TIME]초간 정지한 채로 힘을 모읍니다. 힘을 모은 후",
		"§6지진§f을 일으켜 §c$[RANGE_MIN]§7~§b$[RANGE_MAX]§f칸 내의 지면에 착지 중인",
		"생명체들을 §b띄워올립니다§f. 이후 대상은 $[STUN]초간 §e기절§f합니다. $[COOLDOWN]",
		"§b[§7아이디어 제공자§b] §dspace_kdd"
		})

public class Earthquake extends AbilityBase implements ActiveHandler {
	
	public Earthquake(Participant participant) {
		super(participant);
	}

	public static final SettingObject<Double> RANGE_MIN = 
			abilitySettings.new SettingObject<Double>(Earthquake.class, "range-min", 1.5,
			"# 지진의 최소 범위") {

		@Override
		public boolean condition(Double value) {
			return value >= 0;
		}

	};
	
	public static final SettingObject<Double> RANGE_MAX = 
			abilitySettings.new SettingObject<Double>(Earthquake.class, "range-max", 30.0,
			"# 지진의 최대 범위") {

		@Override
		public boolean condition(Double value) {
			return value >= 0;
		}

	};
	
	public static final SettingObject<Double> STUN = 
			abilitySettings.new SettingObject<Double>(Earthquake.class, "stun", 3.0,
			"# 기절 시간") {

		@Override
		public boolean condition(Double value) {
			return value >= 0;
		}

	};
	
	public static final SettingObject<Integer> COOLDOWN = 
			abilitySettings.new SettingObject<Integer>(Earthquake.class, "cooldown", 105,
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
	
	public boolean ActiveSkill(Material material, ClickType clickType) {
		if (material == Material.IRON_INGOT && clickType == ClickType.RIGHT_CLICK) {
			
		}
		return false;
	}
	
	public class Wave extends AbilityTimer {
		
		private final Location center;
		private final double damage;
		private double waveRadius;
		private final Predicate<Entity> predicate;
		private Set<Damageable> hitEntity = new HashSet<>();
		private ArmorStand damager;
		
		public Wave(double damage) {
			setPeriod(TimeUnit.TICKS, 1);
			this.damage = damage;
			this.predicate = new Predicate<Entity>() {
				@Override
				public boolean test(Entity entity) {
					if (entity instanceof ArmorStand) return false;
					if (entity.equals(getPlayer())) return false;
					if (hitEntity.contains(entity)) return false;
					if (entity instanceof Player) {
						if (!getGame().isParticipating(entity.getUniqueId())
								|| (getGame() instanceof DeathManager.Handler && ((DeathManager.Handler) getGame()).getDeathManager().isExcluded(entity.getUniqueId()))
								|| !getGame().getParticipant(entity.getUniqueId()).attributes().TARGETABLE.getValue()) {
							return false;
						}
						if (getGame() instanceof Teamable) {
							final Teamable teamGame = (Teamable) getGame();
							final Participant entityParticipant = teamGame.getParticipant(entity.getUniqueId()), participant = teamGame.getParticipant(getPlayer().getUniqueId());
							if (participant != null) {
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
		}
		
		@Override
		public void onStart() {
			waveRadius = 0;
			damager = center.getWorld().spawn(center.clone().add(99999, 0, 99999), ArmorStand.class);
			damager.setVisible(false);
			damager.setInvulnerable(true);
			damager.setGravity(false);
			damager.setMetadata("Wave", NULL_VALUE);
			NMS.removeBoundingBox(damager);
		}
		
		@Override
		public void run(int count) {
			if (!skill.isRunning()) this.stop(false);
			if (waveRadius < 15) waveRadius += 0.35;
			else this.stop(false);
			
			if (count == 1) color = waveColor1;
			if (count % 4 == 0) color = waveColors.get(count / 4);
			if (count % 5 == 0) up = !up;
			
			addY = addY + (up ? 0.1 : -0.1);
			
			double playerY = getPlayer().getLocation().getY();
			for (Iterator<Location> iterator = Circle.iteratorOf(center, waveRadius, 75); iterator.hasNext(); ) {
				Location loc = iterator.next();
				loc.setY(LocationUtil.getFloorYAt(loc.getWorld(), playerY, loc.getBlockX(), loc.getBlockZ()) + addY + 0.2);
				ParticleLib.REDSTONE.spawnParticle(loc, color);
				if (addY <= 0.1 || count < 5) ParticleLib.WATER_SPLASH.spawnParticle(loc, 0, 0, 0, 1, 0);
				for (Damageable damageable : LocationUtil.getNearbyEntities(Damageable.class, loc, 0.6, 0.6, predicate)) {
					double increase = (waveRadius / 15) + 0.5;
					damageable.damage(increase * damage, damager);
					hitEntity.add(damageable);
					damageable.setVelocity(VectorUtil.validateVector(damageable.getLocation().toVector().subtract(center.toVector()).normalize().multiply(1.1).setY(0.5)));
					if (damageable instanceof LivingEntity) PotionEffects.SLOW.addPotionEffect((LivingEntity) damageable, 100, 1, false);
				}
			}
		}
		
		@Override
		public void onEnd() {
			onSilentEnd();
		}
		
		@Override
		public void onSilentEnd() {
			SoundLib.ENTITY_PLAYER_SWIM.playSound(center, 2f, 1.4f);
			damager.remove();
		}
		
	}
	
}
