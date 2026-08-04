package rainstar.abilitywar.synergy.normalization.ancientability;

import static rainstar.abilitywar.system.config.RainStarAbilitySetting.rsAbilitySettings;

import java.util.function.Predicate;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Damageable;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import daybreak.abilitywar.ability.AbilityBase;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.ability.decorator.ActiveHandler;
import daybreak.abilitywar.config.ability.AbilitySettings.SettingObject;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.module.DeathManager;
import daybreak.abilitywar.game.team.interfaces.Teamable;
import daybreak.abilitywar.utils.base.Formatter;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.base.math.LocationUtil;
import daybreak.abilitywar.utils.library.ParticleLib;

@AbilityManifest(name = "고대의 카오스", rank = Rank.SPECIAL, species = Species.GOD, explain = {
        "시작의 신 카오스.",
        "철괴를 우클릭하면 $[DURATION]초간 §8짙은 암흑 속§f으로 주변의 생명체들을",
        "모두 끌어당깁니다. $[COOLDOWN]"
		})
public class AncientChaos extends AbilityBase implements ActiveHandler {

	public AncientChaos(Participant participant) {
		super(participant);
	}
	
	public static final SettingObject<Integer> COOLDOWN = rsAbilitySettings.new SettingObject<Integer>(AncientChaos.class, 
			"cooldown", 80, "# 쿨타임") {

		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}

		@Override
		public String toString() {
			return Formatter.formatCooldown(getValue());
		}

	};
	
	public static final SettingObject<Double> DURATION = rsAbilitySettings.new SettingObject<Double>(AncientChaos.class, 
			"duration", 5.0, "# 지속 시간") {

		@Override
		public boolean condition(Double value) {
			return value >= 0;
		}

	};
	
	public static final SettingObject<Double> RANGE = rsAbilitySettings.new SettingObject<Double>(AncientChaos.class, 
			"range", 5.0, "# 사거리") {

		@Override
		public boolean condition(Double value) {
			return value >= 0;
		}

	};
	
	private final Cooldown cooldown = new Cooldown(COOLDOWN.getValue());
	private final double range = RANGE.getValue();
	
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
    };
	
	private Duration duration = new Duration((int) (DURATION.getValue() * 20), cooldown) {

		Location center;
		
		@Override
		public void onDurationStart() {
			center = getPlayer().getLocation();
		}
		
		@Override
		public void onDurationProcess(int count) {
			ParticleLib.SMOKE_NORMAL.spawnParticle(center, 2, 2, 2, 150, 0.3);
			for(Damageable d : LocationUtil.getNearbyEntities(Damageable.class, center, range, range, predicate)) {
				if(!d.equals(getPlayer())) {
					d.damage(1.5);
					Vector vector = center.toVector().subtract(d.getLocation().toVector());
					d.setVelocity(vector);
				}
			}
		}

		@Override
		protected void onDurationEnd() {}
		
	}.setPeriod(TimeUnit.TICKS, 1);
	
	public boolean ActiveSkill(Material material, ClickType clickType) {
		if (material == Material.IRON_INGOT && clickType == ClickType.RIGHT_CLICK) {
			if (!duration.isDuration() && !cooldown.isCooldown()) {
				return duration.start();
			}
		}
		return false;
	}
	
}
