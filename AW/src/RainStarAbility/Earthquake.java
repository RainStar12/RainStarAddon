package RainStarAbility;

import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nullable;

import org.bukkit.Location;
import org.bukkit.Material;
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
			abilitySettings.new SettingObject<Double>(Earthquake.class, "stun", 3.5,
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
	private final double min = RANGE_MIN.getValue();
	private final double max = RANGE_MAX.getValue();
	private final int stun = (int) (STUN.getValue() * 20);
	private final Cooldown cooldown = new Cooldown(COOLDOWN.getValue());
	private final Map<Player, Airborn> airborned = new HashMap<>();
	private Vector upper;
	private final DecimalFormat df = new DecimalFormat("0.000");
	private Circle circle;
	
	public boolean ActiveSkill(Material material, ClickType clickType) {
		if (material == Material.IRON_INGOT && clickType == ClickType.RIGHT_CLICK && !cooldown.isCooldown()) {
			double range = (random.nextDouble() * (max - min)) + min;
			circle = Circle.of(range, (int) (range * 15));
			getPlayer().sendMessage("§e진도 범위§f: " + df.format(range));
			upper = new Vector(0, (range * 0.07), 0);
			for (LivingEntity livingEntity : LocationUtil.getEntitiesInCircle(LivingEntity.class, getPlayer().getLocation(), range, predicate)) {
				if (livingEntity.isOnGround()) {
					livingEntity.setVelocity(upper);
					if (livingEntity instanceof Player) new Airborn((Player) livingEntity).start();
				}
			}
			for (Location loc : circle.toLocations(getPlayer().getLocation()).floor(getPlayer().getLocation().getY())) {
				ParticleLib.CLOUD.spawnParticle(getPlayer(), loc, 0, 0, 0, 1, 0);
			}
			SoundLib.ENTITY_GENERIC_EXPLODE.playSound(getPlayer().getLocation(), (float) (range / 5), 1);
			return cooldown.start();
		}
		return false;
	}
	
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
	
}
