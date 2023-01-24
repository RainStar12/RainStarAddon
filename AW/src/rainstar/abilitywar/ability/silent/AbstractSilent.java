package rainstar.abilitywar.ability.silent;

import java.util.HashSet;
import java.util.Set;

import javax.annotation.Nullable;

import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import daybreak.abilitywar.ability.AbilityBase;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.ability.decorator.ActiveHandler;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.module.DeathManager;
import daybreak.abilitywar.game.team.interfaces.Teamable;
import daybreak.abilitywar.utils.base.math.LocationUtil;
import daybreak.google.common.base.Predicate;

@AbilityManifest(name = "사일런트", rank = Rank.L, species = Species.HUMAN, explain = {
		"개별 은신이 가능한지 테스트하는 능력",
		"철괴 우클릭하면 가장 가까운 대상 한정으로만 은신함",
		"이미 대상한테 은신 중인데 은신 재시도하면 은신 해제됨"
		},
		summarize = {
		""
		})

public abstract class AbstractSilent extends AbilityBase implements ActiveHandler {

	public AbstractSilent(Participant participant) {
		super(participant);
	}
	
	private Set<Player> notshow = new HashSet<>();
	
	protected abstract void hide0(Player player);
	protected abstract void show0(Player player);
	
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
	
	public boolean ActiveSkill(Material material, ClickType clickType) {
		if (material == Material.IRON_INGOT && clickType == ClickType.RIGHT_CLICK) {
			if (LocationUtil.getNearestEntity(Player.class, getPlayer().getLocation(), predicate) != null) {
				Player player = LocationUtil.getNearestEntity(Player.class, getPlayer().getLocation(), predicate);
				if (!notshow.contains(player)) {
					getPlayer().sendMessage(player + "님으로부터 은신합니다.");
					hide0(player);
				} else {
					getPlayer().sendMessage(player + "님에게 은신을 해제합니다.");
					show0(player);
				}
				return true;
			}
		}
		return false;
	}
	
	
}
