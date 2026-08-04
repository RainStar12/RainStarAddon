package rainstar.abilitywar.ability;

import static rainstar.abilitywar.system.config.RainStarAbilitySetting.rsAbilitySettings;

import java.text.DecimalFormat;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.annotation.Nullable;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Damageable;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.material.MaterialData;
import org.bukkit.util.Vector;

import daybreak.abilitywar.ability.AbilityBase;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.ability.decorator.ActiveHandler;
import daybreak.abilitywar.config.ability.AbilitySettings.SettingObject;
import daybreak.abilitywar.config.enums.CooldownDecrease;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.AbstractGame.Participant.ActionbarNotification.ActionbarChannel;
import daybreak.abilitywar.game.module.DeathManager;
import daybreak.abilitywar.game.team.interfaces.Teamable;
import daybreak.abilitywar.utils.base.Formatter;
import daybreak.abilitywar.utils.base.color.Gradient;
import daybreak.abilitywar.utils.base.color.RGB;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.base.math.LocationUtil;
import daybreak.abilitywar.utils.base.math.VectorUtil;
import daybreak.abilitywar.utils.base.math.geometry.Line;
import daybreak.abilitywar.utils.base.minecraft.damage.Damages;
import daybreak.abilitywar.utils.base.minecraft.version.ServerVersion;
import daybreak.abilitywar.utils.library.MaterialX;
import daybreak.abilitywar.utils.library.ParticleLib;
import daybreak.abilitywar.utils.library.SoundLib;
import daybreak.google.common.base.Predicate;

@SuppressWarnings("deprecation")
@AbilityManifest(name = "퀘이사", rank = Rank.S, species = Species.OTHERS, explain = {
		"§7철괴 좌클릭 §8- §8블랙홀§f: 현재 위치를 §8블랙홀§f로 지정합니다.",
		" 이미 §8블랙홀§f이 있다면, §8블랙홀§f에게 멀어지는 방향으로",
		" 넉백되고 $[KNOCKBACK_COOLDOWN]를 가집니다.",
		"§7철괴 우클릭 §8- §e섬광§f: 자신으로부터 §8블랙홀§f까지를 잇는 사선 내의",
		" 모든 대상에게 $[DEFAULT_DAMAGE]의 피해를 가합니다. $[LIGHT_COOLDOWN]",
		" 이 피해는 자신과 §8블랙홀§f 중 더 가까운 쪽의 거리에 비례해 §b+$[DAMAGE_INCREASE]%§f까지 증가합니다.",
		" 이후 3초 이내로 이 스킬을 재사용하면 §8블랙홀§f의 위치까지 §3관전 상태§f로",
		" §b고속 이동§f해, 스치는 모든 대상을 납치한 뒤 도착 시에 이동 거리에",
		" 비례해서 최대 $[DASH_DAMAGE]의 피해를 입히고 §8블랙홀§f을 제거합니다.",
		" 이후 §8블랙홀§f 지정에 $[SET_COOLDOWN]를 가집니다.",
		"§b[§a능력 제공자§b] §emes_seu"
		})
public class Quasar extends AbilityBase implements ActiveHandler {

	public Quasar(Participant participant) {
		super(participant);
	}
	
	public static final SettingObject<Integer> SET_COOLDOWN = rsAbilitySettings.new SettingObject<Integer>(Quasar.class,
			"cooldown-set", 70, "# 블랙홀 파괴 시 재지정 쿨타임") {
		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}
		
        @Override
        public String toString() {
            return Formatter.formatCooldown(getValue());
        }
	};
	
	public static final SettingObject<Integer> KNOCKBACK_COOLDOWN = rsAbilitySettings.new SettingObject<Integer>(Quasar.class,
			"cooldown-knockback", 20, "# 블랙홀에서부터 넉백 쿨타임", "# 주의! 최대 쿨타임 감소를 50%까지만 받습니다.") {
		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}
		
        @Override
        public String toString() {
            return Formatter.formatCooldown(getValue());
        }
	};
	
	public static final SettingObject<Integer> LIGHT_COOLDOWN = rsAbilitySettings.new SettingObject<Integer>(Quasar.class,
			"cooldown-light", 10, "# 광선 발사 쿨타임", "# 주의! 최대 쿨타임 감소를 50%까지만 받습니다.") {
		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}
		
        @Override
        public String toString() {
            return Formatter.formatCooldown(getValue());
        }
	};
	
	public static final SettingObject<Integer> DAMAGE_INCREASE = rsAbilitySettings.new SettingObject<Integer>(Quasar.class,
			"damage-increase", 200, "# 광선의 최대 공격력 증가량", "# 단위: +%", "# +%이므로 수치가 200이라면 +200%, 즉 3배까지 증가합니다.") {
		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}
	};
	
	public static final SettingObject<Double> DEFAULT_DAMAGE = rsAbilitySettings.new SettingObject<Double>(Quasar.class,
			"default-damage", 9.0, "# 광선의 기본 공격력") {
		@Override
		public boolean condition(Double value) {
			return value >= 0;
		}
	};
	
	public static final SettingObject<Double> LIGHT_RANGE_CAP = rsAbilitySettings.new SettingObject<Double>(Quasar.class,
			"light-range-cap", 20.0, "# 광선이 최대 대미지가 되기 위한 블랙홀과의 최소 거리", "# 단위: 칸") {
		@Override
		public boolean condition(Double value) {
			return value >= 0;
		}
	};
	
	public static final SettingObject<Double> DASH_DAMAGE = rsAbilitySettings.new SettingObject<Double>(Quasar.class,
			"dash-damage", 27.0, "# 고속 이동 최대 피해량") {
		@Override
		public boolean condition(Double value) {
			return value >= 0;
		}
	};
	
	public static final SettingObject<Double> DASH_RANGE_CAP = rsAbilitySettings.new SettingObject<Double>(Quasar.class,
			"dash-range-cap", 50.0, "# 고속 이동이 최대 피해량에 도달하기 위해 필요한 거리", "# 단위: 칸") {
		@Override
		public boolean condition(Double value) {
			return value >= 0;
		}
	};
	
	private final Cooldown cooldownSet = new Cooldown(SET_COOLDOWN.getValue(), "설치");
	private final Cooldown cooldownKnockback = new Cooldown(KNOCKBACK_COOLDOWN.getValue(), "넉백", CooldownDecrease._50);
	private final Cooldown cooldownLight = new Cooldown(LIGHT_COOLDOWN.getValue(), "광선", CooldownDecrease._50);
	private final double damageIncrease = DAMAGE_INCREASE.getValue() * 0.01;
	private final double lightDamage = DEFAULT_DAMAGE.getValue();
	private final double lightRangeCap = LIGHT_RANGE_CAP.getValue();
	private final double dashMaxDamage = DASH_DAMAGE.getValue();
	private final double dashRangeCap = DASH_RANGE_CAP.getValue();
	
	private Location blackHole = null;
	private final List<RGB> gradation = Gradient.createGradient(30, RGB.of(1, 254, 220), RGB.of(254, 254, 254), RGB.of(247, 254, 1));
	private int stack = 0;
	private boolean turns = true;
	private ActionbarChannel ac = newActionbarChannel();
	private DecimalFormat df = new DecimalFormat("0.0");
	
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
	
	@Override
	public void onUpdate(Update update) {
		if (update == Update.RESTRICTION_CLEAR) {
			passive.start();
		}
	}
	
	private final AbilityTimer passive = new AbilityTimer() {
		
		@Override
		public void run(int count) {
			if (blackHole != null) {
				if (!blackHole.getWorld().getWorldBorder().isInside(blackHole)) {
					getPlayer().sendMessage("§5[§d!§5] §8블랙홀§f이 세계 경계선 밖을 지나 사라졌습니다.");
					blackHole = null;
					return;
				}
				
				if (ServerVersion.getVersion() >= 13) {
	    			BlockData obsidian = MaterialX.OBSIDIAN.getMaterial().createBlockData();
	    			ParticleLib.FALLING_DUST.spawnParticle(blackHole, 0.5, 1, 0.5, 10, 0, obsidian);
	    		} else {
	    			ParticleLib.FALLING_DUST.spawnParticle(blackHole, 0.5, 1, 0.5, 10, 0, new MaterialData(Material.OBSIDIAN));
	    		}
				
				if (count % 4 == 0) {
					if (getPlayer().getInventory().getItemInMainHand().getType().equals(Material.IRON_INGOT)) {
						Location myLoc = getPlayer().getLocation().clone().add(0, 1, 0);
						int particleCount = (int) Math.max(1, Math.min(100, Math.sqrt(myLoc.distanceSquared(blackHole)) * 3));
						for (Location loc : Line.of(blackHole.toVector().subtract(myLoc.toVector()), particleCount).toLocations(myLoc)) {
							if (turns) stack++;
							else stack--;
							if (stack % (gradation.size() - 1) == 0) {
								turns = !turns;
							}
							ParticleLib.REDSTONE.spawnParticle(getPlayer(), loc, gradation.get(stack));
						}
					}	
				}
				
				if (count % 10 == 0) SoundLib.BLOCK_PORTAL_TRIGGER.playSound(blackHole, 0.35f, 0.5f);
			}
		}
		
	}.setPeriod(TimeUnit.TICKS, 1).register();
	
	private final AbilityTimer skillReuse = new AbilityTimer(60) {
		
		@Override
		public void onStart() {
			ac.update("§c광자 이동 사용 가능§7: §b" + df.format(60 / 20.0) + "초");
		}
		
		@Override
		public void run(int count) {
			ac.update("§c광자 이동 사용 가능§7: §b" + df.format(count / 20.0) + "초");
		}
		
		@Override
		public void onEnd() {
			onSilentEnd();
		}
		
		@Override
		public void onSilentEnd() {
			ac.update(null);
		}
		
	}.setPeriod(TimeUnit.TICKS, 1).register();
	
	private final AbilityTimer dash = new AbilityTimer() {
		
		private GameMode previousGamemode;
		private double damage;
		private Location startLocation;
		private final Set<Player> kidnapped = new HashSet<>();
		private Vector direction;
		private double distance;
		private final double dashSpeed = 2.0;
		
		@Override
		public void onStart() {
			startLocation = getPlayer().getLocation();
			previousGamemode = getPlayer().getGameMode();
			getPlayer().setGameMode(GameMode.SPECTATOR);
			kidnapped.clear();
			direction = blackHole.clone().toVector().subtract(startLocation.toVector());
			distance = direction.length();
			direction.normalize();
			damage = 0;
			ac.update("§c피해량§7: §c" + df.format(damage));
		}
		
		@Override
		public void run(int count) {
			getPlayer().teleport(getPlayer().getLocation().add(direction.clone().multiply(dashSpeed)));
			double moved = startLocation.distance(getPlayer().getLocation());
			damage = dashMaxDamage * Math.min(1.0, moved / dashRangeCap);
			ac.update("§c피해량§7: §c" + df.format(damage));
			for (Player player : LocationUtil.getNearbyEntities(Player.class, getPlayer().getLocation(), 3,3 , predicate)) kidnapped.add(player);
			for (Player player : kidnapped) player.teleport(getPlayer());
			if (moved >= distance || getPlayer().getLocation().distanceSquared(blackHole) <= 3) stop(false);
			if (count % 10 == 0) SoundLib.BLOCK_PORTAL_TRIGGER.playSound(getPlayer().getLocation(), 0.35f, 0.5f);
    		if (ServerVersion.getVersion() >= 13) {
    			BlockData portal = MaterialX.CHORUS_PLANT.getMaterial().createBlockData();
    			ParticleLib.FALLING_DUST.spawnParticle(getPlayer().getLocation().clone().add(0, 1, 0), 0.1, 0.1, 0.1, 5, 0, portal);
    		} else {
    			ParticleLib.FALLING_DUST.spawnParticle(getPlayer().getLocation().clone().add(0, 1, 0), 0.1, 0.1, 0.1, 5, 0, new MaterialData(Material.CHORUS_PLANT));
    		}
    		ParticleLib.TOWN_AURA.spawnParticle(getPlayer().getLocation().clone().add(0, 1, 0), 0.65, 0.65, 0.65, 10, 1);
		}
		
		@Override
		public void onEnd() {
			onSilentEnd();
			getPlayer().teleport(blackHole);
			for (Player player : kidnapped) {
				player.teleport(getPlayer());
				player.damage(damage, getPlayer());
			}
			SoundLib.ENTITY_ZOMBIE_BREAK_WOODEN_DOOR.playSound(blackHole, 1, 1.25f);
			blackHole = null;
			cooldownSet.start();
		}
		
		@Override
		public void onSilentEnd() {
			ac.update(null);
			getPlayer().setGameMode(previousGamemode == GameMode.SPECTATOR ? GameMode.SURVIVAL : previousGamemode);
		}
		
	}.setPeriod(TimeUnit.TICKS, 1).register();
	
	public boolean ActiveSkill(Material material, ClickType clicktype) {
		if (material.equals(Material.IRON_INGOT)) {
			if (clicktype.equals(ClickType.LEFT_CLICK)) {
				if (!dash.isRunning()) {
					if (blackHole == null) {
						if (!cooldownSet.isCooldown()) {
							blackHole = getPlayer().getLocation().clone().add(0, 1, 0);
							getPlayer().sendMessage("§5[§d!§5] §8블랙홀§f을 생성했습니다.");
							SoundLib.BLOCK_ENCHANTMENT_TABLE_USE.playSound(blackHole, 1, 0.5f);
							return true;	
						}
					} else if (!cooldownKnockback.isCooldown()) {
						getPlayer().setVelocity(VectorUtil.validateVector(getPlayer().getLocation().clone().add(0, 1, 0).toVector().subtract(blackHole.toVector()).normalize().multiply(1.4)));
						SoundLib.ENTITY_ENDER_EYE_DEATH.playSound(blackHole, 1, 0.5f);
						SoundLib.BLOCK_END_PORTAL_FRAME_FILL.playSound(getPlayer().getLocation(), 1, 0.85f);
			    		if (ServerVersion.getVersion() >= 13) {
			    			BlockData portal = MaterialX.CHORUS_PLANT.getMaterial().createBlockData();
			    			ParticleLib.FALLING_DUST.spawnParticle(getPlayer().getLocation().clone().add(0, 1, 0), 0.1, 0.1, 0.1, 15, 0, portal);
			    			ParticleLib.FALLING_DUST.spawnParticle(blackHole.clone(), 0.1, 0.1, 0.1, 15, 0, portal);
			    		} else {
			    			ParticleLib.FALLING_DUST.spawnParticle(getPlayer().getLocation().clone().add(0, 1, 0), 0.1, 0.1, 0.1, 15, 0, new MaterialData(Material.CHORUS_PLANT));
			    			ParticleLib.FALLING_DUST.spawnParticle(blackHole.clone(), 0.1, 0.1, 0.1, 15, 0, new MaterialData(Material.CHORUS_PLANT));
			    		}
						return cooldownKnockback.start();
					}	
				}
			} else if (clicktype.equals(ClickType.RIGHT_CLICK)) {
				if (blackHole == null) {
					getPlayer().sendMessage("§5[§d!§5] §8블랙홀§f이 없어 사용할 수 없습니다.");
					return false;
				}
				
				if (skillReuse.isRunning()) {
					skillReuse.stop(false);
					dash.start();
				} else if (!dash.isRunning()) {
					if (!cooldownLight.isCooldown()) {
						Location start = getPlayer().getLocation().clone().add(0, 1, 0);
						for (Damageable damageable : LocationUtil.rayTraceEntities(Damageable.class, start, blackHole, 1, predicate)) {
							Vector line = blackHole.toVector().subtract(getPlayer().getLocation().toVector());
							Vector unit = line.clone().normalize();

							Vector toTarget = damageable.getLocation().toVector().subtract(start.toVector());

							double projected = Math.max(0, Math.min(toTarget.dot(unit), line.length()));

							double nearest = Math.min(projected, line.length() - projected);
							double positionRatio = nearest / (line.length() / 2.0);
							positionRatio = Math.min(1.0, positionRatio);
							double rangeRatio = Math.min(1.0, line.length() / lightRangeCap);
							
							double positionBonus = damageIncrease * 0.5 * positionRatio;
							double rangeBonus = damageIncrease * 0.5 * rangeRatio;
							
							double multiplier = 1 + positionBonus + rangeBonus;
							double damage = lightDamage * multiplier;
							
							Damages.damageArrow(damageable, getPlayer(), (float) damage);
						}
						int particleCount = (int) Math.max(1, Math.min(100, Math.sqrt(start.distanceSquared(blackHole)) * 3));
						for (Location loc : Line.of(blackHole.toVector().subtract(start.toVector()), particleCount).toLocations(start)) {
							if (turns) stack++;
							else stack--;
							if (stack % (gradation.size() - 1) == 0) {
								turns = !turns;
							}
							ParticleLib.REDSTONE.spawnParticle(loc, gradation.get(stack));
						}
						
						Vector direction = blackHole.toVector().subtract(start.toVector());

						double length = direction.length();
						direction.normalize();
						
						for (double d = 0; d <= length; d += 3) {
						    Location soundLoc = start.clone().add(direction.clone().multiply(d));
						    SoundLib.ENTITY_ARROW_HIT_PLAYER.playSound(soundLoc, 1f, 2f);
						}
						SoundLib.ENTITY_ARROW_HIT_PLAYER.playSound(blackHole, 1f, 2f);
						
						skillReuse.start();
						return cooldownLight.start();
					}
				}
			}
		}
		return false;
	}
	
}
