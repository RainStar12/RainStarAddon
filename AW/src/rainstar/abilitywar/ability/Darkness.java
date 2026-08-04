package rainstar.abilitywar.ability;

import static rainstar.abilitywar.system.config.RainStarAbilitySetting.rsAbilitySettings;

import java.text.DecimalFormat;

import javax.annotation.Nullable;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.util.Vector;

import daybreak.abilitywar.ability.AbilityBase;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.SubscribeEvent;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.ability.decorator.ActiveHandler;
import daybreak.abilitywar.config.ability.AbilitySettings.SettingObject;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.AbstractGame.Participant.ActionbarNotification.ActionbarChannel;
import daybreak.abilitywar.game.event.participant.ParticipantDeathEvent;
import daybreak.abilitywar.game.module.DeathManager;
import daybreak.abilitywar.game.team.interfaces.Teamable;
import daybreak.abilitywar.utils.base.Formatter;
import daybreak.abilitywar.utils.base.color.RGB;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.base.math.geometry.Circle;
import daybreak.abilitywar.utils.base.minecraft.damage.Damages;
import daybreak.abilitywar.utils.base.random.Random;
import daybreak.abilitywar.utils.library.ParticleLib;
import daybreak.abilitywar.utils.library.SoundLib;
import daybreak.google.common.base.Predicate;
import rainstar.abilitywar.utils.Notes;

@AbilityManifest(name = "다크니스", rank = Rank.L, species = Species.OTHERS, explain = {
		"§7철괴 우클릭 §8- §8암울한 전망§f: §5어둠§f을 모으기 시작합니다.",
		" 이미 모으고 있던 §5어둠§f이 있다면, 그 §5어둠§f은 §3시전§f됩니다.",
		"§7패시브 §8- §5어둠§f: 소지하고 있는 동안, 가한 공격 피해의 $[PERCENTAGE]%는 §5어둠§f이 가져갑니다.",
		" 또한 축적될 때 그 $[LOSS_PERCENTAGE]%만큼은 증발합니다.",
		" §3시전§f될 때, 가장 체력이 낮은 적에게 모인 수치만큼의 피해를 입힙니다.",
		"§7철괴 좌클릭 §8- §d이중 시전§f: §5어둠§f을 $[DOUBLE_CAST_DAMAGE]%로 2회 §3시전§f합니다.",
		" 이후 적을 처치하기 전까지 §d그림자 방패§f가 되며, §c쿨타임§f을 공유합니다.",
		"§7철괴 좌클릭 §8- §d그림자 방패§f: $[DURATION]초간 받는 피해가 $[DAMAGE_DECREASE]% 감소합니다.",
		" 감소시킨 피해량만큼 §5어둠§f이 모아집니다. $[COOLDOWN]",
		"§b[§7아이디어 제공자§b] §22D100"
		})
public class Darkness extends AbilityBase implements ActiveHandler {
	
	public Darkness(Participant participant) {
		super(participant);
	}
	
	public static final SettingObject<Integer> PERCENTAGE = rsAbilitySettings.new SettingObject<Integer>(Darkness.class,
			"attack-steal-percentage", 60, "# 어둠이 가져갈 공격 피해량", "# 단위: %") {
		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}
	};
	
	public static final SettingObject<Integer> LOSS_PERCENTAGE = rsAbilitySettings.new SettingObject<Integer>(Darkness.class,
			"loss-percentage", 65, "# 어둠의 피해 손실량", "# 단위: %") {
		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}
	};
	
	public static final SettingObject<Integer> DOUBLE_CAST_DAMAGE = rsAbilitySettings.new SettingObject<Integer>(Darkness.class,
			"double-cast-damage", 70, "# 이중 시전 1회당 피해량", "# 단위: %") {
		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}
	};
	
	public static final SettingObject<Double> DURATION = rsAbilitySettings.new SettingObject<Double>(Darkness.class,
			"shadow-shield-duration", 5.0, "# 그림자 방패 지속시간", "# 단위: 초") {
		@Override
		public boolean condition(Double value) {
			return value >= 0;
		}
	};
	
	public static final SettingObject<Integer> DAMAGE_DECREASE = rsAbilitySettings.new SettingObject<Integer>(Darkness.class,
			"damage-decrease", 40, "# 그림자 방패 피해 감소량", "# 단위: %") {
		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}
	};
	
	public static final SettingObject<Integer> COOLDOWN = rsAbilitySettings.new SettingObject<Integer>(Darkness.class,
			"cooldown", 90, "# 쿨타임", "# 단위: 초") {
		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}
        @Override
        public String toString() {
            return Formatter.formatCooldown(getValue());
        }
	};
	
	private final double stealPercentage = PERCENTAGE.getValue() * 0.01;
	private final double lossPercentage = LOSS_PERCENTAGE.getValue() * 0.01;
	private final int duration = (int) (DURATION.getValue() * 20);
	private final double damageDecrease = DAMAGE_DECREASE.getValue() * 0.01;
	private final double doubleCastDamage = DOUBLE_CAST_DAMAGE.getValue() * 0.01;
	private final Cooldown cooldown = new Cooldown(COOLDOWN.getValue());
	
	private static final RGB DARKNESS = RGB.of(92, 17, 138), LIGHT_DARKNESS = RGB.of(154, 101, 192);
	private double darkness = 0;
	private Location darknessLocation = null;
	private int darknessLocNumber = 0;
	private final Random random = new Random();
	private final DecimalFormat df = new DecimalFormat("0.0");
	private final ActionbarChannel ac1 = newActionbarChannel(), ac2 = newActionbarChannel();
	private boolean doubleCast = true;
	private final Circle myRing = Circle.of(1.5, 30);
	private boolean casting = false;
	
	private final Predicate<Entity> predicate = new Predicate<Entity>() {
		@Override
		public boolean test(Entity entity) {
			if (entity.equals(getPlayer())) return false;
			if (entity instanceof Player) {
				if (!getGame().isParticipating(entity.getUniqueId())
						|| (getGame() instanceof DeathManager.Handler && ((DeathManager.Handler) getGame()).getDeathManager().isExcluded(entity.getUniqueId()))) {
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
	
	public static void drawRing(Location center, double radius, int points, double rotX, double rotY, double rotZ, RGB color) {

		Circle circle = Circle.of(radius, points);
		
		double rx = Math.toRadians(rotX);
		double ry = Math.toRadians(rotY);
		double rz = Math.toRadians(rotZ);
		
		for (Location loc : circle.toLocations(center)) {
			Vector v = loc.clone().subtract(center).toVector();
			
			rotateX(v, rx);
			rotateY(v, ry);
			rotateZ(v, rz);
			
			ParticleLib.REDSTONE.spawnParticle(center.clone().add(v), color);
		}
	}
	
	private static void rotateX(Vector v, double angle) {
		double cos = Math.cos(angle);
		double sin = Math.sin(angle);
		
		double y = v.getY();
		double z = v.getZ();
		
		v.setY(y * cos - z * sin);
		v.setZ(y * sin + z * cos);
	}
	
	private static void rotateY(Vector v, double angle) {
		double cos = Math.cos(angle);
		double sin = Math.sin(angle);
		
		double x = v.getX();
		double z = v.getZ();
		
		v.setX(x * cos + z * sin);
		v.setZ(-x * sin + z * cos);
	}
	
	private static void rotateZ(Vector v, double angle) {
		double cos = Math.cos(angle);
		double sin = Math.sin(angle);
		
		double x = v.getX();
		double y = v.getY();
		
		v.setX(x * cos - y * sin);
		v.setY(x * sin + y * cos);
	}
	
	private void darknessAdd(double amount) {
		darkness += (amount * (1 - lossPercentage));
		ParticleLib.PORTAL.spawnParticle(darknessLocation, 0, 0, 0, (int) Math.min(200, amount * 5), 0.7f);
		drawRing(darknessLocation, 1, 15, random.nextDouble() * 360, random.nextDouble() * 360, random.nextDouble() * 360, LIGHT_DARKNESS);
		drawRing(darknessLocation, 1, 15, random.nextDouble() * 360, random.nextDouble() * 360, random.nextDouble() * 360, LIGHT_DARKNESS);
		
		darknessSound(darknessLocation, darkness);
	}
	
	private void darknessCast(int cast) {
		casting = true;
		for (int i = 0; i < cast; i++) {
			Participant target = null;
			double lessHealth = Integer.MAX_VALUE;
			for (Participant participant : getGame().getParticipants()) {
				if (predicate.test(participant.getPlayer())) {
					double health = participant.getPlayer().getHealth();
					if (lessHealth > health) {
						lessHealth = health;
						target = participant;
					}
				}
			}
			if (target != null) {
				target.getPlayer().setNoDamageTicks(0);
				Damages.damageMagic(target.getPlayer(), getPlayer(), false, (float) (cast > 1 ? darkness * doubleCastDamage : darkness));
				showEffect(target.getPlayer().getLocation());
				showEffect(darknessLocation);
				target.getPlayer().setNoDamageTicks(0);
			}
		}
		darkness = 0;
		darknessLocation = null;
		casting = false;
	}
	
	private void showEffect(Location location) {
		SoundLib.ENTITY_ELDER_GUARDIAN_CURSE.playSound(location, 1, 2);
		darknessSound(location, darkness);
		ParticleLib.PORTAL.spawnParticle(location, 0, 0, 0, (int) Math.min(200, darkness * 10), 1.5f);
		drawRing(location, 2, 30, random.nextDouble() * 360, random.nextDouble() * 360, random.nextDouble() * 360, LIGHT_DARKNESS);
		drawRing(location, 2, 30, random.nextDouble() * 360, random.nextDouble() * 360, random.nextDouble() * 360, DARKNESS);
	}
	
	private void darknessSound(Location location, double amount) {
		SoundLib.GUITAR.playInstrument(location, Notes.getNote(0));
		if (amount > 10) SoundLib.GUITAR.playInstrument(location, Notes.getNote(6));
		if (amount > 20) SoundLib.GUITAR.playInstrument(location, Notes.getNote(12));
		if (amount > 30) SoundLib.GUITAR.playInstrument(location, Notes.getNote(17));
		if (amount > 40) SoundLib.GUITAR.playInstrument(location, Notes.getNote(21));
	}
	
	@Override
	public void onUpdate(Update update) {
		if (update == Update.RESTRICTION_CLEAR) {
			passive.start();
		}
	}
	
	private AbilityTimer passive = new AbilityTimer() {
		
		@Override
		public void run(int count) {
			if (darkness > 0) {
				darknessLocation = myRing.toLocations(getPlayer().getLocation().clone().add(0, 1, 0)).get(darknessLocNumber);
				ParticleLib.REDSTONE.spawnParticle(darknessLocation, DARKNESS);
				ParticleLib.PORTAL.spawnParticle(darknessLocation, 0, 0, 0, 1, 0.4f);	
			}
			ac1.update("§5어둠§7: §3" + df.format(darkness));
		}
	
	}.setPeriod(TimeUnit.TICKS, 1).register();
	
	private AbilityTimer shadowShield = new AbilityTimer(duration) {
		
		@Override
		public void onStart() {
			SoundLib.ENTITY_PIG_SADDLE.playSound(getPlayer().getLocation(), 1, 0.85f);
			ac2.update("§d그림자 방패§7: " + df.format(duration / 20.0));
		}
		
		@Override
		public void run(int count) {
			ac2.update("§d그림자 방패§7: " + df.format(count / 20.0));
		}
		
		@Override
		public void onEnd() {
			cooldown.start();
			onSilentEnd();
		}
		
		@Override
		public void onSilentEnd() {
			ac2.update(null);
		}
		
	}.setPeriod(TimeUnit.TICKS, 1).register();
		
	@SubscribeEvent(priority = 900)
	private void onEntityDamageByEntity(EntityDamageByEntityEvent e) {
		Player damager = null;
		if (e.getDamager() instanceof Projectile) {
			Projectile projectile = (Projectile) e.getDamager();
			if (projectile.getShooter() != null && projectile.getShooter().equals(getPlayer())) {
				damager = getPlayer();
			}
		} else if (getPlayer().equals(e.getDamager())) damager = getPlayer();
		
		if (e.getEntity() instanceof Player) {
			if (!casting && !e.getCause().equals(DamageCause.MAGIC) && getPlayer().equals(damager) && !e.getEntity().equals(getPlayer()) && darkness > 0) {
				double steal = e.getDamage() * stealPercentage;
				e.setDamage(e.getDamage() - steal);
				darknessAdd(steal);
			}
			
			if (shadowShield.isRunning() && !getPlayer().equals(damager) && e.getEntity().equals(getPlayer()) && darkness > 0) {
				double steal = e.getDamage() * damageDecrease;
				e.setDamage(e.getDamage() - steal);
				darknessAdd(steal);
			}	
		}
	}
	
	@SubscribeEvent
	private void onParticipantDeath(ParticipantDeathEvent e) {
		if (getPlayer().equals(e.getPlayer().getKiller())) {
			doubleCast = true;
		}
	}
	
	public boolean ActiveSkill(Material material, ClickType clickType) {
		if (material.equals(Material.IRON_INGOT)) {
			if (clickType.equals(ClickType.LEFT_CLICK)) {
				if (!cooldown.isCooldown()) {
					if (!shadowShield.isRunning()) {
						if (doubleCast) {
							if (darkness > 0) {
								doubleCast = false;
								darknessCast(2);
								return true;
							} else getPlayer().sendMessage("§5[§d!§5] §c모아진 §5어둠§c이 없습니다.");
						} else return shadowShield.start();
					} else getPlayer().sendMessage("§5[§d!§5] §d그림자 방패§c가 지속 중입니다.");
				}
			} else if (clickType.equals(ClickType.RIGHT_CLICK)) {
				if (darkness > 0.1) darknessCast(1);
				darkness = 0.1;
				darknessLocNumber = random.nextInt(30);
				darknessLocation = myRing.toLocations(getPlayer().getLocation().clone().add(0, 1, 0)).get(darknessLocNumber);
				return true;
			}
		}
		return false;
	}
	
}
