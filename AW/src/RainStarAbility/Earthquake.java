package RainStarAbility;

import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nullable;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Damageable;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.util.Vector;

import daybreak.abilitywar.ability.AbilityBase;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.ability.SubscribeEvent;
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
		"철괴 우클릭 시 무작위 §2진도§f의 §6지진§f을 일으켜 $[WAIT]초 후 지진파가 퍼져나갑니다.",
		"지진파에 닿은 생명체들을 §2진도§f에 비례해 §b띄워올립니다§f.",
		"이후 대상은 $[STUN]초간 §e기절§f합니다. $[COOLDOWN]",
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
	
	public static final SettingObject<Double> WAIT = 
			abilitySettings.new SettingObject<Double>(Earthquake.class, "wait", 0.5,
			"# 지진파 대기 시간") {

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
	
	private final int wait = (int) (WAIT.getValue() * 20);
	private final Random random = new Random();
	private final int stun = (int) (STUN.getValue() * 20);
	private final Cooldown cooldown = new Cooldown(COOLDOWN.getValue());
	private final Map<Player, Airborn> airborned = new HashMap<>();
	private final DecimalFormat df = new DecimalFormat("0.0");
	
	public boolean ActiveSkill(Material material, ClickType clickType) {
		if (material == Material.IRON_INGOT && clickType == ClickType.RIGHT_CLICK && !cooldown.isCooldown() && !earthquakeready.isRunning()) {
			return earthquakeready.start();
		}
		return false;
	}
	
	@SubscribeEvent
	public void onPlayerMove(PlayerMoveEvent e) {
		if (e.getPlayer().equals(getPlayer()) && earthquakeready.isRunning()) e.setTo(e.getFrom());
	}
	
	public AbilityTimer earthquakeready = new AbilityTimer(wait) {
		
		double richter;
		
		@Override
		public void onStart() {
			richter = (random.nextInt(96) + 5) * 0.1;
			for (Player player : Bukkit.getOnlinePlayers()) {
				Location noY = player.getLocation().clone();
				Location noYcenter = getPlayer().getLocation().clone();
				noY.setY(0);
				noYcenter.setY(0);
				double distance = Math.sqrt(Math.abs(noY.distanceSquared(noYcenter)));
				player.sendMessage("§4[§c!§4] §d" + df.format(distance) + "m§f 밖에서 진도 §b" + df.format(richter) + "§f의 지진이 일어났습니다!");
			}
			SoundLib.ENTITY_GENERIC_EXPLODE.playSound(getPlayer().getLocation(), (float) (richter / 3), 1);
			ParticleLib.EXPLOSION_HUGE.spawnParticle(getPlayer().getLocation(), 0, 0, 0, 1, 1);
		}
		
		@Override
		public void onEnd() {
			new EarthquakeWave((int) (richter * 15), getPlayer().getLocation()).start();
			cooldown.start();
		}
		
	}.setPeriod(TimeUnit.TICKS, 1).register();
	
	@SuppressWarnings("deprecation")
	public class Airborn extends AbilityTimer {
		
		private final Player player;
		
		public Airborn(Player player) {
			super(TaskType.NORMAL, 100000);
			setPeriod(TimeUnit.TICKS, 1);
			this.player = player;
			airborned.put(player, this);
		}
		
		@Override
		public void run(int count) {
			if (count >= 5 && player.isOnGround()) this.stop(false);
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
		private Set<Damageable> hitEntity = new HashSet<>();
		
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
		public void run(int count) {
			radius += 0.35;
			for (Iterator<Location> iterator = Circle.iteratorOf(center, radius, maxCount); iterator.hasNext(); ) {
				Location loc = iterator.next();
				LocationUtil.floorY(loc);
				ParticleLib.CRIT.spawnParticle(loc, 0, 0, 0, 1, 0.35);
				ParticleLib.REDSTONE.spawnParticle(loc, gradations.get(count - 1));
				SoundLib.BLOCK_STONE_BREAK.playSound(loc, 0.5f, 1);
				for (Damageable damageable : LocationUtil.getNearbyEntities(Damageable.class, loc, 0.6, 0.6, predicate)) {
					hitEntity.add(damageable);
					damageable.setVelocity(new Vector(0, 0.5 + (maxCount / 75.0), 0));
					if (damageable instanceof Player) new Airborn((Player) damageable).start();
				}
			}
		}
		
		@Override
		public void onEnd() {
			onSilentEnd();
		}
		
	}
	
}