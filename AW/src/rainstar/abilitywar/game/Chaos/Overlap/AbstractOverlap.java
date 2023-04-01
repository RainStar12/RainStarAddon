package rainstar.abilitywar.game.Chaos.Overlap;

import java.util.Collection;
import java.util.StringJoiner;

import javax.annotation.Nonnull;

import org.bukkit.entity.Player;

import daybreak.abilitywar.ability.AbilityBase;
import daybreak.abilitywar.game.Game;
import daybreak.abilitywar.game.module.DeathManager;
import daybreak.abilitywar.utils.base.language.korean.KoreanUtil;
import daybreak.abilitywar.utils.base.language.korean.KoreanUtil.Josa;
import rainstar.abilitywar.utils.RankColor;

public class AbstractOverlap extends Game {

	public AbstractOverlap(Collection<Player> players) {
		super(players);
	}

	@Override
	protected @Nonnull DeathManager newDeathManager() {
		return new DeathManager(this) {
			@Override
			protected String getRevealMessage(Participant victim) {
				final Overlap overlap = (Overlap) victim.getAbility();
				if (overlap.hasAbility()) {
					final StringJoiner joiner = new StringJoiner(" §f+ ");
					for (AbilityBase ability : overlap.getAbilities()) {
						joiner.add(RankColor.getColor(ability.getRank()) + ability.getDisplayName());
					}
					return "§f[§c능력§f] §c" + victim.getPlayer().getName() + "§f님의 능력은 §e" + joiner.toString() + "§f" + KoreanUtil.getJosa(joiner.toString(), Josa.이었였) + "습니다.";
				} else {
					return "§f[§c능력§f] §c" + victim.getPlayer().getName() + "§f님은 능력이 없습니다.";
				}
			}
		};
	}

	@Override
	protected void progressGame(int arg0) {
		
	}

	
}
