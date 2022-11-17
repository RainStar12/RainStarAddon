package RainStarEffect;

import org.bukkit.Bukkit;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChatEvent;

import daybreak.abilitywar.AbilityWar;
import daybreak.abilitywar.ability.SubscribeEvent;
import daybreak.abilitywar.ability.event.AbilityPreActiveSkillEvent;
import daybreak.abilitywar.ability.event.AbilityPreTargetEvent;
import daybreak.abilitywar.game.AbstractGame;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.manager.effect.registry.ApplicationMethod;
import daybreak.abilitywar.game.manager.effect.registry.EffectManifest;
import daybreak.abilitywar.game.manager.effect.registry.EffectRegistry;
import daybreak.abilitywar.game.manager.effect.registry.EffectRegistry.EffectRegistration;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;

@EffectManifest(name = "침묵", displayName = "§3침묵", method = ApplicationMethod.UNIQUE_STACK, type = {
}, description = {
		"채팅을 칠 수 없습니다.",
		"액티브, 타게팅 스킬을 사용할 수 없습니다."
})
@SuppressWarnings("deprecation")
public class Mute extends AbstractGame.Effect implements Listener {

	public static final EffectRegistration<Mute> registration = EffectRegistry.registerEffect(Mute.class);

	public static void apply(Participant participant, TimeUnit timeUnit, int duration) {
		registration.apply(participant, timeUnit, duration);
	}

	private final Participant participant;

	public Mute(Participant participant, TimeUnit timeUnit, int duration) {
		participant.getGame().super(registration, participant, timeUnit.toTicks(duration));
		this.participant = participant;
		setPeriod(TimeUnit.TICKS, 1);
	}

	@Override
	protected void onStart() {
		Bukkit.getPluginManager().registerEvents(this, AbilityWar.getPlugin());
		super.onStart();
	}
	
	@SubscribeEvent
	public void onChat(PlayerChatEvent e) {
		if (participant.getPlayer().equals(e.getPlayer())) {
			participant.getPlayer().sendMessage("§3[§b!§3] §c침묵되었습니다!");
			e.setCancelled(true);
		}
	}

	@SubscribeEvent
	public void onPreActiveSkill(AbilityPreActiveSkillEvent e) {
		if (e.getParticipant().equals(participant)) {
			participant.getPlayer().sendMessage("§3[§b!§3] §c침묵되었습니다!");
			e.setCancelled(true);
		}
	}
	
	@SubscribeEvent
	public void onPreTargetSkill(AbilityPreTargetEvent e) {
		if (e.getParticipant().equals(participant)) {
			participant.getPlayer().sendMessage("§3[§b!§3] §c침묵되었습니다!");
			e.setCancelled(true);
		}
	}
	
	@Override
	protected void onEnd() {
		HandlerList.unregisterAll(this);
		super.onEnd();
	}

	@Override
	protected void onSilentEnd() {
		HandlerList.unregisterAll(this);
		super.onSilentEnd();
	}
	
}
