package rainstar.abilitywar.ability;

import static rainstar.abilitywar.system.config.RainStarAbilitySetting.rsAbilitySettings;

import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nullable;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import daybreak.abilitywar.AbilityWar;
import daybreak.abilitywar.ability.AbilityBase;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.SubscribeEvent;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.ability.decorator.ActiveHandler;
import daybreak.abilitywar.config.ability.AbilitySettings.SettingObject;
import daybreak.abilitywar.game.GameManager;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.AbstractGame.Participant.ActionbarNotification.ActionbarChannel;
import daybreak.abilitywar.game.module.DeathManager;
import daybreak.abilitywar.game.module.Wreck;
import daybreak.abilitywar.game.team.interfaces.Teamable;
import daybreak.abilitywar.utils.base.color.RGB;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.base.math.FastMath;
import daybreak.abilitywar.utils.base.math.VectorUtil;
import daybreak.abilitywar.utils.library.ParticleLib;
import daybreak.abilitywar.utils.library.SoundLib;
import daybreak.google.common.base.Predicate;
import daybreak.google.common.base.Strings;

@AbilityManifest(name = "윈드리버", rank = Rank.L, species = Species.HUMAN, explain = {
		"§7패시브 §8- §a바람 갈퀴§f: 공중에 떠 있는 적을 §a근접 공격§f할 때,",
		" §b체공 시간§f에 따라 초당 §c$[DAMAGE_PERCENTAGE]§8(§7최대 $[MAX_PERCENTAGE]§8)§c%§f의 추가 피해를 입힙니다.",
		" §b신속 효과§f가 있다면 계수당 모든 피해량이 $[SPEED_DAMAGE_PERCENTAGE]% 증가합니다.",
		"§7근접 공격 §8- §3훨윈드§f: $[ATTACK_UPGRADE_PERIOD]초마다 다음 §a근접 공격§f 시 대상을 살짝",
		" 위로 띄워올리고, 땅에 착지하기 전까지 이동력을 크게 감소시킵니다.",
		" 대상은 웅크리면 이 효과를 빠르게 탈출할 수 있습니다.",
		"§7철괴 우클릭 §8- §b바람의 길§f: 바라보는 방향으로 §b구름 장판§f을 형성합니다.",
		" 최대 3번까지 연속으로 사용할 수 있으며, $[CHARGE_PERIOD]초마다 충전됩니다.",
		" 아군은 §b구름 장판§f 위에서 웅크리는 것으로 빠르게 이동할 수 있습니다."
		})
@SuppressWarnings("deprecation")
public class WindReaver extends AbilityBase implements ActiveHandler {
	
	public WindReaver(Participant participant) {
		super(participant);
	}
	
	public static final SettingObject<Integer> DAMAGE_PERCENTAGE =
			rsAbilitySettings.new SettingObject<Integer>(WindReaver.class, "damage-percentage", 7,
			"# 바람 갈퀴 초당 추가 피해량", "# 단위: %") {

		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}

	};

	public static final SettingObject<Integer> MAX_PERCENTAGE =
			rsAbilitySettings.new SettingObject<Integer>(WindReaver.class, "max-percentage", 50,
			"# 바람 갈퀴 최대 추가 피해량", "# 단위: %") {

		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}

	};
	
	public static final SettingObject<Integer> SPEED_DAMAGE_PERCENTAGE =
			rsAbilitySettings.new SettingObject<Integer>(WindReaver.class, "speed-damage-percentage", 10,
			"# 신속 계수 1당 추가 피해량", "# 단위: %") {

		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}

	};

	public static final SettingObject<Double> ATTACK_UPGRADE_PERIOD =
			rsAbilitySettings.new SettingObject<Double>(WindReaver.class, "attack-upgrade-period", 15.0,
			"# 훨윈드 강화 공격 충전 주기", "# 단위: 초") {

		@Override
		public boolean condition(Double value) {
			return value >= 0;
		}

	};

	public static final SettingObject<Double> CHARGE_PERIOD =
			rsAbilitySettings.new SettingObject<Double>(WindReaver.class, "wind-charge-period", 20.0,
			"# 바람의 길 충전 주기", "# 단위: 초") {

		@Override
		public boolean condition(Double value) {
			return value >= 0;
		}

	};

	private final double speedMultiply = SPEED_DAMAGE_PERCENTAGE.getValue() * 0.01;
	private final double damageMultiply = DAMAGE_PERCENTAGE.getValue() * 0.01;
	private final double maxMultiply = MAX_PERCENTAGE.getValue() * 0.01;
	private final int attackPeriod = (int) (ATTACK_UPGRADE_PERIOD.getValue() * 20);
	private final int chargePeriod = (int) (CHARGE_PERIOD.getValue() * 20);
	
	private static final Vector up = new Vector(0, 1.0175, 0);
	private static final Vector downV = new Vector(0, -0.015, 0);
	private static final Vector fastdownV = new Vector(0, -0.15, 0);
	
	private Map<Player, Integer> airStack = new HashMap<>();
	private Set<Player> bound = new HashSet<>();
	private boolean attackUpgrade = false;
	private int windStack = 3;
	private boolean state = false;
	private final Set<WindPath> paths = new HashSet<>();
	
	private ActionbarChannel ac1 = newActionbarChannel(), ac2 = newActionbarChannel();
	private DecimalFormat df = new DecimalFormat("0.0");
	
	private final Predicate<Entity> windPredicate = new Predicate<Entity>() {
		@Override
		public boolean test(Entity entity) {
			if (!(entity instanceof Player)) return false;

			if (entity.equals(getPlayer())) return true;

			if (!getGame().isParticipating(entity.getUniqueId())
					|| (getGame() instanceof DeathManager.Handler
					&& ((DeathManager.Handler) getGame()).getDeathManager().isExcluded(entity.getUniqueId()))) {
				return false;
			}

			if (getGame() instanceof Teamable) {
				final Teamable teamGame = (Teamable) getGame();
				final Participant entityParticipant = teamGame.getParticipant(entity.getUniqueId());
				final Participant participant = getParticipant();

				return teamGame.hasTeam(entityParticipant)
						&& teamGame.hasTeam(participant)
						&& teamGame.getTeam(entityParticipant).equals(teamGame.getTeam(participant));
			}

			return false;
		}

		@Override
		public boolean apply(@Nullable Entity arg0) {
			return test(arg0);
		}
	};
	
	private int getReload(int baseReload, int maxDecrease) {
		double decrease = Wreck.calculateDecreasedAmount(maxDecrease);
		if (Wreck.isEnabled(GameManager.getGame())) {
			if (decrease == 100) return 1;
			else return (int) (decrease * baseReload);
		} else return baseReload;
	}
	
	@Override
	public void onUpdate(Update update) {
		if (update == Update.RESTRICTION_CLEAR) {
			passive.start();
			ac1.update("§a바람 갈퀴§7: " + (attackUpgrade ? "§a" : "§7") + "§l༄");
			ac2.update(Strings.repeat("§b§l☁", windStack).concat(Strings.repeat("§7§l☁", 3 - windStack)));
		}
	}
	
	public AbilityTimer passive = new AbilityTimer() {
		
		@Override
		public void run(int count) {
			if (!attackUpgrade && !attackCharging.isRunning()) attackCharging.start();
			if (windStack < 3 && !windCharging.isRunning()) windCharging.start();
			
			for (Participant participant : getGame().getParticipants()) {
				Player player = participant.getPlayer();
				if (player.isSneaking() && windPredicate.test(player)) {
					for (WindPath path : paths) {
						if (path.isInRange(player.getLocation())) {
							player.setVelocity(path.direction.clone().multiply(1.1));
							ParticleLib.CLOUD.spawnParticle(player.getLocation(), 0.2, 0.1, 0.2, 8, 0);
							break;
						}
					}
				}
				if (player.isOnGround()) {
					if (airStack.containsKey(player)) airStack.remove(player);
				} else airStack.put(player, airStack.getOrDefault(player, 0) + 1);
			}
		}
		
	}.setPeriod(TimeUnit.TICKS, 1).register();
	
	private AbilityTimer attackCharging = new AbilityTimer(getReload(attackPeriod, 40)) {

		@Override
		protected void onEnd() {
			attackUpgrade = true;
			SoundLib.ENTITY_PLAYER_ATTACK_SWEEP.playSound(getPlayer(), 1, 1.7f);
			new BukkitRunnable() {
				@Override
				public void run() {
					SoundLib.ENTITY_PLAYER_ATTACK_SWEEP.playSound(getPlayer(), 1, 2);
				}
			}.runTaskLater(AbilityWar.getPlugin(), 10L);
			ac1.update("§a바람 갈퀴§7: " + (attackUpgrade ? "§a" : "§7") + "§l༄");
		}
		
	}.setPeriod(TimeUnit.TICKS, 1).register();
	
	private AbilityTimer windCharging = new AbilityTimer(getReload(chargePeriod, 100)) {

		@Override
		protected void onEnd() {
			windStack = Math.min(windStack + 1, 3);
			SoundLib.ENTITY_WITCH_THROW.playSound(getPlayer(), 1, 2);
			ac2.update(Strings.repeat("§b§l☁", windStack).concat(Strings.repeat("§7§l☁", 3 - windStack)));
		}
		
	}.setPeriod(TimeUnit.TICKS, 1).register();
	
	public boolean ActiveSkill(Material material, ClickType clicktype) {
		if (material.equals(Material.IRON_INGOT) && clicktype.equals(ClickType.RIGHT_CLICK)) {
			if (windStack > 0) {
				new WindPath(getPlayer().getLocation().clone().add(0, 0.5, 0), getPlayer().getLocation().getDirection()).start();
				SoundLib.ENTITY_WITCH_THROW.playSound(getPlayer().getLocation(), 1, 0.5f);
				windStack--;
				ac2.update(Strings.repeat("§b§l☁", windStack).concat(Strings.repeat("§7§l☁", 3 - windStack)));
				return true;
			} else getPlayer().sendMessage("§b[§a!§b] §3구름 장판§c이 아직 충전되지 않았습니다. 남은 시간: §e" + df.format(windCharging.getCount() / 20.0) + "§c초");
		}
		return false;
	}

	@SubscribeEvent
	private void onEntityDamageByEntity(EntityDamageByEntityEvent e) {
		Player damager = null;
		if (e.getDamager() instanceof Player) damager = (Player) e.getDamager();
		else if (e.getDamager() instanceof Projectile) {
			Projectile projectile = (Projectile) e.getDamager();
			if (projectile.getShooter() != null && projectile.getShooter() instanceof Player) damager = (Player) projectile.getShooter();
		}
		
		if (getPlayer().equals(damager) && getPlayer().hasPotionEffect(PotionEffectType.SPEED)) {
			e.setDamage(e.getDamage() * (1 + (speedMultiply * (getPlayer().getPotionEffect(PotionEffectType.SPEED).getAmplifier() + 1))));
		}
		
		if (getPlayer().equals(e.getDamager()) && airStack.containsKey(e.getEntity())) {
			e.setDamage(e.getDamage() * (1 + (Math.min(maxMultiply, damageMultiply * 0.05 * airStack.get(e.getEntity())))));
		}
		
		if (getPlayer().equals(e.getDamager()) && attackUpgrade && e.getEntity() instanceof Player && !e.getCause().equals(DamageCause.THORNS)) {
			SoundLib.ENTITY_FIREWORK_ROCKET_LAUNCH.playSound(e.getEntity().getLocation(), 1, 2);
			new Cut(e.getEntity(), state = !state).start();
			new BukkitRunnable() {
				@Override
				public void run() {
					e.getEntity().setVelocity(up);
				}
			}.runTaskLater(AbilityWar.getPlugin(), 2L);
			new BukkitRunnable() {
				@Override
				public void run() {
					bound.add((Player) e.getEntity());
				}
			}.runTaskLater(AbilityWar.getPlugin(), 8L);
			attackUpgrade = false;
			ac1.update("§a바람 갈퀴§7: " + (attackUpgrade ? "§a" : "§7") + "§l༄");
		}
	}
	
	@SubscribeEvent
	private void onPlayerMove(PlayerMoveEvent e) {
		if (bound.contains(e.getPlayer())) {
			if (e.getPlayer().isOnGround() || e.getPlayer().getGameMode().equals(GameMode.SPECTATOR)) bound.remove(e.getPlayer());
			else {
				if (e.getPlayer().isSneaking()) e.getPlayer().setVelocity(fastdownV);
				else e.getPlayer().setVelocity(downV);
			}
		}
	}
	
	private class WindPath extends AbilityTimer {

		private final Location start;
		private final Vector direction;
		private final Vector side;
		private final Vector up;

		private final double length = 12;
		private final double width = 3;
		private final double height = 1.5;

		private WindPath(Location start, Vector direction) {
			super(TaskType.REVERSE, 210);
			setPeriod(TimeUnit.TICKS, 1);

			this.direction = direction.clone().normalize();

			Vector axis = new Vector(0, 1, 0);
			if (Math.abs(this.direction.dot(axis)) > 0.99) axis = new Vector(1, 0, 0);

			this.side = axis.clone().crossProduct(this.direction).normalize();
			this.up = this.direction.clone().crossProduct(this.side).normalize();

			this.start = start.clone().add(this.direction.clone());

			paths.add(this);
		}

		public boolean isInRange(Location loc) {
			Vector relative = loc.toVector().subtract(start.toVector());

			double forward = relative.dot(direction);
			double sideways = Math.abs(relative.dot(side));
			double vertical = Math.abs(relative.dot(up));

			return forward >= 0 && forward <= length && sideways <= width * 0.5 && vertical <= height;
		}

		@Override
		public void run(int count) {
			if (count % 70 == 0) {
				Location middle = start.clone().add(direction.clone().multiply(length * 0.5));
				Location end = start.clone().add(direction.clone().multiply(length));

				SoundLib.ITEM_ELYTRA_FLYING.playSound(start, 1, 0.5f);
				SoundLib.ITEM_ELYTRA_FLYING.playSound(middle, 1, 0.5f);
				SoundLib.ITEM_ELYTRA_FLYING.playSound(end, 1, 0.5f);
			}
			
			if (count % 6 == 0) {
				for (double x = 0; x <= length; x += 0.7) {
					Location left = start.clone().add(direction.clone().multiply(x)).add(side.clone().multiply(-width * 0.5));
					Location right = start.clone().add(direction.clone().multiply(x)).add(side.clone().multiply(width * 0.5));

					ParticleLib.REDSTONE.spawnParticle(left, RGB.AQUA);
					ParticleLib.REDSTONE.spawnParticle(right, RGB.AQUA);
				}

				for (double w = -width * 0.5; w <= width * 0.5; w += 0.5) {
					Location front = start.clone().add(side.clone().multiply(w));
					Location back = start.clone().add(direction.clone().multiply(length)).add(side.clone().multiply(w));

					ParticleLib.REDSTONE.spawnParticle(front, RGB.AQUA);
					ParticleLib.REDSTONE.spawnParticle(back, RGB.AQUA);
				}
			}

			if (count % 2 != 0) return;

			double phase = ((200 - count) * 0.4) % length;

			for (double w = -width * 0.5 + 0.2; w <= width * 0.5 - 0.2; w += 0.4) {
				Location loc = start.clone().add(direction.clone().multiply(phase)).add(side.clone().multiply(w)).add(up.clone().multiply(0.15));
				ParticleLib.CLOUD.spawnParticle(loc, 0, 0, 0, 1, 0);
			}
		}

		@Override
		protected void onEnd() {
			onSilentEnd();
		}

		@Override
		protected void onSilentEnd() {
			paths.remove(this);
		}
	}
	
	private static class Component {

		private final RGB color;
		private final double radius, length;

		private Component(RGB color, double radius, double length) {
			this.color = color;
			this.radius = radius;
			this.length = length;
		}

	}

	private static final Component[] components = new Component[]{
			new Component(RGB.of(203, 254, 119), 1.5, 1.05),
			new Component(RGB.of(39, 254, 175), 1.5, 1)
	};
	
	public class Cut extends AbilityTimer {

		private final Location base;
		private final double angle;

		private Cut(Entity target, boolean state) {
			super(state ? TaskType.NORMAL : TaskType.REVERSE, 3);
			setPeriod(TimeUnit.TICKS, 1);
			final Vector delta = target.getLocation().toVector().subtract(getPlayer().getLocation().toVector()).normalize();
			this.base = target.getLocation().clone().subtract(delta.multiply(1.6)).setDirection(delta.normalize()).add(0, 1, 0);
			this.angle = state ? 25 : -25;
		}

		@Override
		protected void run(int count) {
			final Vector direction = base.getDirection().setY(0).normalize(), axis = VectorUtil.rotateAroundAxisY(direction.clone(), 90);
			for (int i = 0; i < 8; i++) {
				final double radians = 0.78539816339744830961566084581988 + 0.06544984694978735913463840381832 * ((count - 1) * 8 + i);
				final double cos = FastMath.cos(radians), sin = FastMath.sin(radians);
				for (Component component : components) {
					final Location loc = base.clone().add(
							VectorUtil.rotateAroundAxis(
									VectorUtil.rotateAroundAxis(
											VectorUtil.rotateAroundAxisY(new Vector(cos, 0, component.length * sin), -base.getYaw()), direction, angle
									), axis, base.getPitch()
							).multiply(component.radius)
					).subtract(0, .75, 0);
					for (int y = 0; y < 3; y++) {
						ParticleLib.REDSTONE.spawnParticle(loc.add(0, .5, 0), component.color);
					}
				}
			}

		}

	}
	
}