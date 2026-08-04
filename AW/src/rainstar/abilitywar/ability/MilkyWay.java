package rainstar.abilitywar.ability;

import static rainstar.abilitywar.system.config.RainStarAbilitySetting.rsAbilitySettings;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nullable;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

import daybreak.abilitywar.ability.AbilityBase;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.ability.SubscribeEvent;
import daybreak.abilitywar.ability.decorator.ActiveHandler;
import daybreak.abilitywar.config.ability.AbilitySettings.SettingObject;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.module.DeathManager;
import daybreak.abilitywar.game.team.interfaces.Teamable;
import daybreak.abilitywar.utils.base.Formatter;
import daybreak.abilitywar.utils.base.color.Gradient;
import daybreak.abilitywar.utils.base.color.RGB;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.base.math.LocationUtil;
import daybreak.abilitywar.utils.base.math.VectorUtil;
import daybreak.abilitywar.utils.base.math.geometry.Circle;
import daybreak.abilitywar.utils.library.ParticleLib;
import daybreak.abilitywar.utils.library.SoundLib;
import daybreak.google.common.base.Predicate;

@AbilityManifest(name = "은하수", rank = Rank.S, species = Species.OTHERS, explain = {
		"§7철괴 좌클릭 §8- §b은하의 흐름§f: 자신이 바라보는 방향으로, 사거리 $[RANGE_ACTIVE] 이내의",
		" 모든 대상을 밀어버립니다. 이후 자신에게 가까워졌다면 §a근거리§f,",
		" 멀어졌다면 §b원거리§f §c공격력§f이 $[DURATION]초간 §c$[DAMAGE_DECREASE]%§f 감소합니다. $[COOLDOWN]",
		"§7패시브 §8- §e인력과 척력§f: 공격력 감소 효과를 받고 있는 대상은 그 타입에 따라",
		" 사거리 $[RANGE_PASSIVE] 이내에 다가오면 §a끌려오거나§f §b밀려납니다§f.",
		"§b[§7아이디어 제공자§b] §e_LunaEunha_§f"
		})
public class MilkyWay extends AbilityBase implements ActiveHandler {
	
	public MilkyWay(Participant participant) {
		super(participant);
	}
	
	public static final SettingObject<Double> RANGE_ACTIVE = 
			rsAbilitySettings.new SettingObject<Double>(MilkyWay.class, "range-active", 10.0, "# 액티브 스킬 사거리") {
		@Override
		public boolean condition(Double value) {
			return value >= 0;
		}
	};
	
	public static final SettingObject<Double> RANGE_PASSIVE = 
			rsAbilitySettings.new SettingObject<Double>(MilkyWay.class, "range-passive", 6.5, "# 패시브 스킬 사거리") {
		@Override
		public boolean condition(Double value) {
			return value >= 0;
		}
	};
	
	public static final SettingObject<Double> DURATION = 
			rsAbilitySettings.new SettingObject<Double>(MilkyWay.class, "damage-decrease-duration", 10.0, "# 공격력 감소 지속시간") {
		@Override
		public boolean condition(Double value) {
			return value >= 0;
		}
	};
	
	public static final SettingObject<Integer> DAMAGE_DECREASE = 
			rsAbilitySettings.new SettingObject<Integer>(MilkyWay.class, "damage-decrease-amount", 20, "# 공격력 감소 수치", "# 단위: %") {
        @Override
        public boolean condition(Integer value) {
            return value >= 0;
        }
    };
	
	public static final SettingObject<Integer> COOLDOWN = 
			rsAbilitySettings.new SettingObject<Integer>(MilkyWay.class, "cooldown", 60, "# 액티브 스킬 쿨타임", "# 쿨타임 감소 효과를 66%까지만 받습니다.") {
		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}
		@Override
		public String toString() {
			return Formatter.formatCooldown(getValue());
		}
	};
	
	private double range_active = RANGE_ACTIVE.getValue(), range_passive = RANGE_PASSIVE.getValue();
	private int duration = (int) (DURATION.getValue() * 10);
	private double damage_decrease = 1 - (DAMAGE_DECREASE.getValue() * 0.01);
	private Cooldown cooldown = new Cooldown(COOLDOWN.getValue(), 66);
	private static final List<RGB> gradation1 = Gradient.createGradient(30, RGB.of(36, 235, 193), RGB.of(254, 254, 101)), gradation2 = Gradient.createGradient(30, RGB.of(102, 74, 254), RGB.of(149, 254, 226));
	private int colorstack1 = 0, colorstack2 = 0;
	private boolean colordirection1 = false, colordirection2 = false;
	private final Circle circle1 = Circle.of(range_active, (int) Math.min(range_active * 10, 200)), circle2 = Circle.of(range_passive, (int) Math.min(range_passive * 10, 200));
	private Set<Player> melee_decrease = new HashSet<>(), range_decrease = new HashSet<>();
	
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
	
	private AbilityTimer passive = new AbilityTimer() {
		
		@Override
		public void run(int count) {
			if (getPlayer().getInventory().getItemInMainHand().getType().equals(Material.IRON_INGOT)) {
				if (colorstack1 == 29 || colorstack1 == 0) colordirection1 = !colordirection1;
				colorstack1 = colordirection1 ? Math.min(colorstack1 + 1, 29) : Math.max(colorstack1 - 1, 0);
				for (Location loc : circle1.toLocations(getPlayer().getLocation()).floor(getPlayer().getLocation().getY())) {
					ParticleLib.REDSTONE.spawnParticle(getPlayer(), loc, gradation1.get(colorstack1));
				}
			}
		}
		
	}.setPeriod(TimeUnit.TICKS, 2).register();
	
	private AbilityTimer durationtimer = new AbilityTimer(duration) {
		
		@Override
		public void run(int count) {
			for (Player player : LocationUtil.getEntitiesInCircle(Player.class, getPlayer().getLocation(), range_passive, predicate)) {
				if (melee_decrease.contains(player)) player.setVelocity(VectorUtil.validateVector(getPlayer().getLocation().toVector().subtract(player.getLocation().toVector()).normalize().setY(0).multiply(0.12)));
				if (range_decrease.contains(player)) player.setVelocity(VectorUtil.validateVector(player.getLocation().toVector().subtract(getPlayer().getLocation().toVector()).normalize().setY(0).multiply(0.12)));
			}
			
			if (colorstack2 == 29 || colorstack2 == 0) colordirection2 = !colordirection2;
			colorstack2 = colordirection2 ? Math.min(colorstack2 + 1, 29) : Math.max(colorstack2 - 1, 0);
			for (Location loc : circle2.toLocations(getPlayer().getLocation()).floor(getPlayer().getLocation().getY())) {
				ParticleLib.REDSTONE.spawnParticle(getPlayer(), loc, gradation2.get(colorstack2));
			}
		}
		
		@Override
		public void onEnd() {
			onSilentEnd();
		}
		
		@Override
		public void onSilentEnd() {
			melee_decrease.clear();
			range_decrease.clear();
		}
		
	}.setPeriod(TimeUnit.TICKS, 2).register();
	
	public boolean ActiveSkill(Material material, ClickType clicktype) {
		if (material.equals(Material.IRON_INGOT) && clicktype.equals(ClickType.LEFT_CLICK)) {
			if (!cooldown.isCooldown()) {
				if (LocationUtil.getEntitiesInCircle(Player.class, getPlayer().getLocation(), range_active, predicate).size() > 0) {
					Map<Player, Double> velocitied = new HashMap<>();
					for (Player player : LocationUtil.getEntitiesInCircle(Player.class, getPlayer().getLocation(), range_active, predicate)) {
						velocitied.put(player, player.getLocation().distanceSquared(getPlayer().getLocation()));
						player.setVelocity(getPlayer().getEyeLocation().getDirection().normalize().multiply(1.8));
						ParticleLib.END_ROD.spawnParticle(player.getLocation().clone().add(0, 0.5, 0), 0.3, 0.3, 0.3, 30, 0.15f);
					}
					
					new AbilityTimer(4) {
						
						@Override
						public void onEnd() {
							for (Player player : velocitied.keySet()) {
								if (velocitied.get(player) >= player.getLocation().distanceSquared(getPlayer().getLocation())) {
									melee_decrease.add(player);
								}
								if (velocitied.get(player) <= player.getLocation().distanceSquared(getPlayer().getLocation())) {
									range_decrease.add(player);
								}
							}
							if (durationtimer.isRunning()) durationtimer.stop(false);
							durationtimer.start();
						}
						
					}.setPeriod(TimeUnit.TICKS, 1).start();
					
					SoundLib.BLOCK_END_PORTAL_SPAWN.playSound(getPlayer().getLocation(), 1, 2f);
					
					return cooldown.start();
				} else getPlayer().sendMessage("§b[§e!§b] §c범위 내에 사람이 없습니다.");
			}
		}
		return false;
	}
	
	@SubscribeEvent()
	public void onEntityDamageByEntity(EntityDamageByEntityEvent e) {
		if (melee_decrease.contains(e.getDamager())) {
			e.setDamage(e.getDamage() * damage_decrease);
		}
		if (e.getDamager() instanceof Projectile) {
			if (range_decrease.contains(((Projectile) e.getDamager()).getShooter())) {
				e.setDamage(e.getDamage() * damage_decrease);
			}
		}
	}

}
