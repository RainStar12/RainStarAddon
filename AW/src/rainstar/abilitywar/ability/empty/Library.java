package rainstar.abilitywar.ability.empty;

import java.util.HashMap;
import java.util.Map;

import daybreak.abilitywar.ability.AbilityFactory.AbilityRegistration;
import daybreak.abilitywar.game.module.Module;
import daybreak.abilitywar.game.module.ModuleBase;

@ModuleBase(Library.class)
public class Library implements Module {

	private Map<String, AbilityRegistration> abilityMap = new HashMap<>();
	
	public Library() {}

	@Override
	public void register() {}

	@Override
	public void unregister() {
		abilityMap.clear();
	}
	
	public Map<String, AbilityRegistration> getAbilityMap() {
		return abilityMap;
	}
	
}
