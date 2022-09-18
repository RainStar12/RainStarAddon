package RainStarAbility;

import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Damageable;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import daybreak.abilitywar.ability.AbilityBase;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.ability.decorator.ActiveHandler;
import daybreak.abilitywar.config.ability.AbilitySettings.SettingObject;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.manager.effect.Stun;
import daybreak.abilitywar.game.module.DeathManager;
import daybreak.abilitywar.game.team.interfaces.Teamable;
import daybreak.abilitywar.utils.base.Formatter;
import daybreak.abilitywar.utils.base.color.Gradient;
import daybreak.abilitywar.utils.base.color.RGB;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.base.math.LocationUtil;
import daybreak.abilitywar.utils.base.math.geometry.Circle;
import daybreak.abilitywar.utils.base.random.Random;
import daybreak.abilitywar.utils.library.ParticleLib;
import daybreak.abilitywar.utils.library.SoundLib;
import daybreak.google.common.base.Predicate;

@AbilityManifest(name = "지진", rank = Rank.S, species = Species.OTHERS, explain = {
		"철괴 우클릭 시 무작위 §2진도§f의 §6지진§f을 일으켜 §c$[RANGE_MIN]§7~§b$[RANGE_MAX]§f칸 내의 지면에 착지 중인",
		"생명체들을 §2진도§f에 비례해 §b띄워올립니다§f. 이후 대상은 $[STUN]초간 §e기절§f합니다. $[COOLDOWN]",
		"§b[§7아이디어 제공자§b] §dspace_kdd"
		},
		summarize = {
		"§7철괴 우클릭 시§f 무작위 범위 내 착지 중인 생명체들을 띄워올리고",
		"대상들이 다시 발에 땅이 닿으면 $[STUN]초 §e기절§f합니다. $[COOLDOWN]"
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
			abilitySettings.new SettingObject<Double>(Earthquake.class, "stun", 4.0,
			"# 기절 시간") {

		@Override
		public boolean condition(Double value) {
			return value >= 0;
		}

	};
	
	public static final SettingObject<Integer> COOLDOWN = 
			abilitySettings.new SettingObject<Integer>(Earthquake.class, "cooldown", 200,
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
						|| (getGame() instanceof DeathManager.Handler &&
								((DeathManager.Handler) getGame()).getDeathManager().isExcluded(entity.getUniqueId()))
						|| !getGame().getParticipant(entity.getUniqueId()).attributes().TARGETABLE.getValue()) {
					return false;
				}
				if (getGame() instanceof Teamable) {
					final Teamable teamGame = (Teamable) getGame();
					final Participant entityParticipant = teamGame.getParticipant(
							entity.getUniqueId()), participant = getParticipant();
					return !teamGame.hasTeam(entityParticipant) || !teamGame.hasTeam(participant)
							|| (!teamGame.getTeam(entityParticipant).equals(teamGame.getTeam(participant)));
				}
			}
			return true;
		}

		@Override
		public boolean apply(@Nullable Entity arg0) {
			return false;
		}
	};
	
	private final Random random = new Random();
	private final int stun = (int) (STUN.getValue() * 20);
	private final Cooldown cooldown = new Cooldown(COOLDOWN.getValue());
	private final Map<Player, Airborn> airborned = new HashMap<>();
	private final DecimalFormat df = new DecimalFormat("0.000");
	private Circle circle;
	
	public boolean ActiveSkill(Material material, ClickType clickType) {
		if (material == Material.IRON_INGOT && clickType == ClickType.RIGHT_CLICK && !cooldown.isCooldown() && !earthquakeready.isRunning()) {
			return earthquakeready.start();
		}
		return false;
	}
	
	public AbilityTimer earthquakeready = new AbilityTimer() {
		
		double richter;
		
		@Override
		public void onStart() {
			richter = (random.nextInt(96) + 5) * 0.1;
			Bukkit.broadcastMessage("§4[§c!§4] §d" + getPlayer().getLocation().getX() + "");
			SoundLib.ENTITY_GENERIC_EXPLODE.playSound(getPlayer().getLocation(), (float) (richter / 2), 1);
		}
		
		@Override
		public void run(int count) {
			
		}
		
	}.setPeriod(TimeUnit.TICKS, 1).register();
	
	@SuppressWarnings("deprecation")
	public class Airborn extends AbilityTimer {
		
		private final Player player;
		
		public Airborn(Player player) {
			super(TaskType.REVERSE, 100);
			setPeriod(TimeUnit.TICKS, 1);
			this.player = player;
			airborned.put(player, this);
		}
		
		@Override
		public void run(int count) {
			if (player.isOnGround()) this.stop(false);
		}
	
		@Override
		public void onEnd() {
			airborned.remove(player);
			Stun.apply(getGame().getParticipant(player), TimeUnit.TICKS, stun);
		}
		
	}
	
	public class EarthquakeWave extends AbilityTimer {

		private double radius;
		private final Predicate<Entity> predicate;
		private final Location center;
		private final int maxCount;
		
		private RGB startColor = RGB.of(200, 1, 1), endColor = RGB.of(30, 1, 1);
		private final List<RGB> gradations;
		
		public EarthquakeWave(int count, Location center) {
			super(TaskType.NORMAL, count);
			setPeriod(TimeUnit.TICKS, 1);
			this.gradations = Gradient.createGradient(count, startColor, endColor);
			this.center = center;
			this.maxCount = count;
			this.predicate = new Predicate<Entity>() {
				@Override
				public boolean test(Entity entity) {
					if (!entity.isOnGround()) return false;
					if (entity instanceof ArmorStand) return false;
					if (entity.equals(getPlayer())) return false;
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
			
		}
		
		@Override
		public void run(int count) {
			radius += 0.3;
			for (Iterator<Location> iterator = Circle.iteratorOf(center, radius, maxCount); iterator.hasNext(); ) {
				Location loc = iterator.next();
				LocationUtil.floorY(loc);
				ParticleLib.CRIT.spawnParticle(loc, 0, 0, 0, 1, 0.35);
				ParticleLib.REDSTONE.spawnParticle(loc, gradations.get(count));
				SoundLib.BLOCK_STONE_HIT.playSound(loc, 0.5f, 1);
				for (Damageable damageable : LocationUtil.getNearbyEntities(Damageable.class, loc, 0.6, 0.6, predicate)) {
					damageable.setVelocity(new Vector(0, maxCount / 100.0, 0));
					if (damageable instanceof Player) new Airborn((Player) damageable).start();
				}
			}
		}
		
		@Override
		public void onEnd() {
			onSilentEnd();
		}
		
		@Override
		public void onSilentEnd() {
			
		}
		
	}
	
}
