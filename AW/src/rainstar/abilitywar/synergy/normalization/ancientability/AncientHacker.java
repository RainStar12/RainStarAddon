package rainstar.abilitywar.synergy.normalization.ancientability;

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
import daybreak.abilitywar.utils.base.color.RGB;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.base.math.LocationUtil;
import daybreak.abilitywar.utils.base.math.geometry.Circle;
import daybreak.abilitywar.utils.library.ParticleLib;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import static rainstar.abilitywar.system.config.RainStarAbilitySetting.rsAbilitySettings;

import java.util.function.Predicate;

@AbilityManifest(name = "고대의 해커", rank = Rank.SPECIAL, species = Species.HUMAN, explain = {
		"철괴를 우클릭하면 자신에게 제일 가까운 플레이어를 §2해킹§f해 좌표를 알아내고",
		"$[DURATION]초간 해당 플레이어를 §e§n기절§f시킵니다. $[COOLDOWN]"
		})
public class AncientHacker extends AbilityBase implements ActiveHandler {

	public AncientHacker(Participant participant) {
		super(participant);
	}

	public static final SettingObject<Integer> COOLDOWN = rsAbilitySettings.new SettingObject<Integer>(AncientHacker.class, "cooldown", 180,
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

	public static final SettingObject<Integer> DURATION = rsAbilitySettings.new SettingObject<Integer>(AncientHacker.class, "duration", 5,
			"# 능력 지속시간") {

		@Override
		public boolean condition(Integer value) {
			return value >= 1;
		}

	};

	private Player target = null;

	private final Cooldown cooldownTimer = new Cooldown(COOLDOWN.getValue());
	private final int stunDuration = DURATION.getValue();
	private static final RGB PURPLE = RGB.of(113, 43, 204);

	private final int amount = 25;
	private final Circle circle = Circle.of(1, amount);
	private final AbilityTimer particleShow = new AbilityTimer(stunDuration * 20) {

		private double y;
		private boolean add;

		@Override
		public void onStart() {
			y = 0.0;
			add = true;
		}

		@Override
		public void run(int count) {
			if (target != null) {
				if (add && y >= 2.0) {
					add = false;
				} else if (!add && y <= 0) {
					add = true;
				}

				if (add) {
					y += 0.1;
				} else {
					y -= 0.1;
				}

				for (Location location : circle.toLocations(target.getLocation().add(0, y, 0)))
					ParticleLib.REDSTONE.spawnParticle(location, PURPLE);
				for (Location location : circle.toLocations(target.getLocation().add(0, 2.0 - y, 0)))
					ParticleLib.REDSTONE.spawnParticle(location, PURPLE);
			}
		}

		@Override
		public void onEnd() {
			target = null;
		}

	}.setPeriod(TimeUnit.TICKS, 1).register();

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

	@Override
	public boolean ActiveSkill(Material material, ClickType clickType) {
		if (material == Material.IRON_INGOT) {
			if (clickType == ClickType.RIGHT_CLICK) {
				if (!cooldownTimer.isCooldown()) {
					target = LocationUtil.getNearestEntity(Player.class, getPlayer().getLocation(), predicate);

					if (target != null) {
						getPlayer().sendMessage("§e" + target.getName() + "§f님은 " +
								"§aX " + ((int) target.getLocation().getX()) + "§f, " +
								"§aY " + ((int) target.getLocation().getY()) + "§f, " +
								"§aZ " + ((int) target.getLocation().getZ()) + "§f에 있습니다.");

						target.sendMessage("§5해킹당했습니다!");
						Stun.apply(getGame().getParticipant(target), TimeUnit.SECONDS, stunDuration);
						particleShow.start();
						cooldownTimer.start();

						return true;
					} else {
						getPlayer().sendMessage("§a가장 가까운 플레이어§f가 존재하지 않습니다.");
					}
				}
			}
		}

		return false;
	}
	
}
