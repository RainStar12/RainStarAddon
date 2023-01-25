package rainstar.abilitywar.system.event;

import daybreak.abilitywar.game.AbstractGame;
import daybreak.abilitywar.game.event.participant.ParticipantEvent;

import javax.annotation.Nonnull;

import org.bukkit.event.HandlerList;

public class MuteRemoveEvent extends ParticipantEvent {

    private static final HandlerList handlers = new HandlerList();

    public MuteRemoveEvent(@Nonnull AbstractGame.Participant who) {
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