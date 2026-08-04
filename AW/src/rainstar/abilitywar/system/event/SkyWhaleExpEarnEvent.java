package rainstar.abilitywar.system.event;

import javax.annotation.Nonnull;

import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;

import daybreak.abilitywar.game.AbstractGame;
import daybreak.abilitywar.game.event.participant.ParticipantEvent;

public class SkyWhaleExpEarnEvent extends ParticipantEvent implements Cancellable {
	
    private static final HandlerList handlers = new HandlerList();
    
    private boolean cancelled = false;

    public SkyWhaleExpEarnEvent(@Nonnull AbstractGame.Participant who) {
        super(who);
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }

    @Override
    public @Nonnull HandlerList getHandlers() {
        return handlers;
    }
    
	@Override
	public boolean isCancelled() {
		return cancelled;
	}

	@Override
	public void setCancelled(boolean cancelled) {
		this.cancelled = cancelled;
	}

}
