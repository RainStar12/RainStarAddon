package rainstar.abilitywar.effect;

import org.bukkit.Bukkit;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.potion.PotionEffectType;

import daybreak.abilitywar.AbilityWar;
import daybreak.abilitywar.game.AbstractGame;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.list.murdermystery.ability.AbstractMurderer;
import daybreak.abilitywar.game.manager.effect.registry.ApplicationMethod;
import daybreak.abilitywar.game.manager.effect.registry.EffectManifest;
import daybreak.abilitywar.game.manager.effect.registry.EffectRegistry;
import daybreak.abilitywar.game.manager.effect.registry.EffectType;
import daybreak.abilitywar.game.manager.effect.registry.EffectRegistry.EffectRegistration;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.base.random.Random;
import daybreak.abilitywar.utils.library.PotionEffects;

@EffectManifest(name = "마약", displayName = "§d마약", method = ApplicationMethod.UNIQUE_STACK, type = {
		EffectType.MOVEMENT_RESTRICTION, EffectType.COMBAT_RESTRICTION, EffectType.SIGHT_CONTROL
}, description = {
		"§2멀미§f와 함께 슬롯이 때때로 무작위로 변경됩니다.",
		"머더라면 위 효과 대신 §b신속 2§f를 얻습니다."
})
public class Drug extends AbstractGame.Effect implements Listener {

	public static final EffectRegistration<Drug> registration = EffectRegistry.registerEffect(Drug.class);

	public static void apply(Participant participant, TimeUnit timeUnit, int duration) {
		registration.apply(participant, timeUnit, duration);
	}

	private final Participant participant;
	private final Random random = new Random();
	
	public Drug(Participant participant, TimeUnit timeUnit, int duration) {
		participant.getGame().super(registration, participant, (timeUnit.toTicks(duration) / 2));
		this.participant = participant;
		setPeriod(TimeUnit.TICKS, 2);
	}
	
	@Override
	protected void onStart() {
		Bukkit.getPluginManager().registerEvents(this, AbilityWar.getPlugin());
		if (!(participant.getAbility() instanceof AbstractMurderer)) participant.getPlayer().getInventory().setHeldItemSlot(0);
	}
	
	@Override
	protected void run(int count) {
		if (count % 10 == 0) {
			if (!(participant.getAbility() instanceof AbstractMurderer)) PotionEffects.CONFUSION.addPotionEffect(participant.getPlayer(), 100, 0, true, false, true);
			else PotionEffects.SPEED.addPotionEffect(participant.getPlayer(), 100, 1, true, false, true);
		}
		if (!(participant.getAbility() instanceof AbstractMurderer) && random.nextInt(8) == 0) {
			participant.getPlayer().getInventory().setHeldItemSlot(random.nextInt(9));
		}
		super.run(count);
	}

	@Override
	protected void onEnd() {
		onSilentEnd();
		super.onEnd();
	}

	@Override
	protected void onSilentEnd() {
		if (!(participant.getAbility() instanceof AbstractMurderer)) participant.getPlayer().removePotionEffect(PotionEffectType.CONFUSION);
		else participant.getPlayer().removePotionEffect(PotionEffectType.SPEED);
		HandlerList.unregisterAll(this);
		super.onSilentEnd();
	}
	
}
