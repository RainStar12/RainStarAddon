package rainstar.aw;

import RainStarEffect.*;
import daybreak.abilitywar.game.AbstractGame;

public enum RainStarEffectFactory {
    AGRO(Agro.class),
    BACKSEAT_GAMING(BackseatGaming.class),
    BINDING_SMOKE(BindingSmoke.class),
    CHARM(Charm.class),
    CHILL(Chill.class),
    CONFUSION(Confusion.class),
    CORROSION(Corrosion.class),
    DIMENSION_DISTORTION(DimensionDistortion.class),
    ELECTRIC_SHOCK(ElectricShock.class),
    FROZEN_HEART(FrozenHeart.class),
    IRREPARABLE(Irreparable.class),
    MADNESS(Madness.class),
    SNOWFLAKE_MARK(SnowflakeMark.class),
    TIME_SLOWDOWN(TimeSlowdown.class);

    Class<? extends AbstractGame.Effect> clazz;

    RainStarEffectFactory(Class<? extends AbstractGame.Effect> clazz) {
        this.clazz = clazz;
    }

    public Class<? extends AbstractGame.Effect> getEffectClass() {
        return clazz;
    }

    public static void load() {
        for (RainStarEffectFactory factory : RainStarEffectFactory.values()) {
            try {
                Class.forName(factory.getEffectClass().getName());
            } catch (Exception ignored) {
            	
            }
        }
    }
}