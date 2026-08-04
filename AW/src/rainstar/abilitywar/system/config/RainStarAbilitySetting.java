package rainstar.abilitywar.system.config;

import daybreak.abilitywar.config.ability.AbilitySettings;
import rainstar.abilitywar.RainStarAddon;

public class RainStarAbilitySetting {

	public static final AbilitySettings rsAbilitySettings = new AbilitySettings(RainStarAddon.getAddonFile("RainStarAbility.yml"));
    public static final AbilitySettings rsSynergySettings = new AbilitySettings(RainStarAddon.getAddonFile("RainStarSynergy.yml"));
    public static final AbilitySettings rsLiteAbilitySettings = new AbilitySettings(RainStarAddon.getAddonFile("RainStarLiteAbility.yml"));
    
}