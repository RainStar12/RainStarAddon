package rainstar.abilitywar.ability.silent;

import org.bukkit.entity.Player;

import daybreak.abilitywar.ability.AbilityBase;
import daybreak.abilitywar.game.AbstractGame.Participant;

public abstract class AbstractSilent extends AbilityBase {

	public AbstractSilent(Participant participant) {
		super(participant);
	}
	
	protected abstract void hide0(Player player);
	protected abstract void show0(Player player);
	
	
	
}
