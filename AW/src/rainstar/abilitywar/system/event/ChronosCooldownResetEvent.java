package rainstar.abilitywar.system.event;

import javax.annotation.Nonnull;

import org.bukkit.event.HandlerList;

import daybreak.abilitywar.game.AbstractGame;
import daybreak.abilitywar.game.event.participant.ParticipantEvent;

public class ChronosCooldownResetEvent extends ParticipantEvent {

    private static final HandlerList handlers = new HandlerList();

    public ChronosCooldownResetEvent(@Nonnull AbstractGame.Participant who) {
        super(who);
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }

    @Override
    public @Nonnull HandlerList getHandlers() {
        return handlers;
    }

}
