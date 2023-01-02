package rainstar.abilitywar;

import daybreak.abilitywar.game.AbstractGame;
import rainstar.abilitywar.effect.*;

public enum RainStarEffectFactory {
	ADDICTION(Addiction.class),
    AGRO(Agro.class),
    BACKSEAT_GAMING(BackseatGaming.class),
    BINDING_SMOKE(BindingSmoke.class),
    BURN(Burn.class),
    CHARM(Charm.class),
    CHILL(Chill.class),
    CONFUSION(Confusion.class),
    CORROSION(Corrosion.class),
    DIMENSION_DISTORTION(DimensionDistortion.class),
    DREAM(Dream.class),
    ELECTRIC_SHOCK(ElectricShock.class),
    FROZEN_HEART(FrozenHeart.class),
    IRREPARABLE(Irreparable.class),
    MADNESS(Madness.class),
    MUTE(Mute.class),
    PARALYSIS(Paralysis.class),
    PETRIFICATION(Petrification.class),
    POISON(Poison.class),
    SNOWFLAKE_MARK(SnowflakeMark.class),
    STIFFEN(Stiffen.class),
    SUPERREGEN(SuperRegen.class),
    TIME_DISTORTION(TimeDistortion.class),
    TIME_INTERRUPT(TimeInterrupt.class),
    VAIN_DREAM(VainDream.class);

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