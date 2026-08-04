package rainstar.abilitywar.synergy.normalization.ancientability;

import static rainstar.abilitywar.system.config.RainStarAbilitySetting.rsAbilitySettings;

import java.util.function.Predicate;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

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
import daybreak.abilitywar.utils.base.math.geometry.Circle;
import daybreak.abilitywar.utils.library.ParticleLib;
import daybreak.abilitywar.utils.library.PotionEffects;
import daybreak.abilitywar.utils.library.SoundLib;

@AbilityManifest(name = "고대의 교황", rank = Rank.SPECIAL, species = Species.HUMAN, explain = {
		"철괴를 우클릭하면 $[DURATION_CONFIG]초간 주변 $[RANGE_CONFIG]칸에 자신의 §b영역§f을 선포합니다. $[COOLDOWN_CONFIG]",
		"§b영역§f 안에서 자신은 §d재생§f 효과를, 적들은 §0위더§f 효과를 받습니다."
		})
public class AncientTheHighPriestess  extends AbilityBase implements ActiveHandler {

	public AncientTheHighPriestess(Participant participant) {
		super(participant);
	}
	
	public static final SettingObject<Integer> COOLDOWN_CONFIG = rsAbilitySettings.new SettingObject<Integer>(AncientTheHighPriestess.class,
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

	public static final SettingObject<Double> DURATION_CONFIG = rsAbilitySettings.new SettingObject<Double>(AncientTheHighPriestess.class, 
			"duration", 6.0, "# 지속 시간") {

		@Override
		public boolean condition(Double value) {
			return value >= 0;
		}

	};

	public static final SettingObject<Double> RANGE_CONFIG = rsAbilitySettings.new SettingObject<Double>(AncientTheHighPriestess.class, 
			"range", 8.0, "# 범위") {

		@Override
		public boolean condition(Double value) {
			return value >= 0;
		}

	};
	
	private final int duration = (int) (DURATION_CONFIG.getValue() * 20);
	private final double range = RANGE_CONFIG.getValue();
	private final Cooldown cooldownTimer = new Cooldown(COOLDOWN_CONFIG.getValue());
	private final Circle circle = Circle.of(range, (int) (range * range));
	
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

	private final Duration skill = new Duration(duration, cooldownTimer) {

		private Location center;

		@Override
		protected void onDurationStart() {
			center = getPlayer().getLocation();

			for (Player player : LocationUtil.getNearbyEntities(Player.class, center, range, range, predicate)) {
				if (LocationUtil.isInCircle(center, player.getLocation(), range)) {
					SoundLib.ENTITY_EVOKER_CAST_SPELL.playSound(player);
				}
			}
		}

		@Override
		protected void onDurationProcess(int count) {
			for (Location location : circle.toLocations(center).floor(center.getY())) {
				ParticleLib.SPELL_INSTANT.spawnParticle(location);
			}

			for (Player player : LocationUtil.getNearbyEntities(Player.class, center, range, range, null)) {
				if (LocationUtil.isInCircle(center, player.getLocation(), range)) {
					if (player.equals(getPlayer())) {
						PotionEffects.REGENERATION.addPotionEffect(player, 100, 1, true);
					} else {
						PotionEffects.WITHER.addPotionEffect(player, 100, 1, true);
					}
				}
			}
		}

	}.setPeriod(TimeUnit.TICKS, 1);
	
	@Override
	public boolean ActiveSkill(Material material, ClickType clickType) {
		if (material == Material.IRON_INGOT && clickType == ClickType.RIGHT_CLICK && !skill.isDuration() && !cooldownTimer.isCooldown()) {
			skill.start();
			return true;
		}
		return false;
	}


}
