package RainStarEffect;

import org.bukkit.Bukkit;
import org.bukkit.entity.ArmorStand;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

import daybreak.abilitywar.AbilityWar;
import daybreak.abilitywar.game.AbstractGame;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.manager.effect.Stun;
import daybreak.abilitywar.game.manager.effect.event.ParticipantEffectApplyEvent;
import daybreak.abilitywar.game.manager.effect.registry.ApplicationMethod;
import daybreak.abilitywar.game.manager.effect.registry.EffectManifest;
import daybreak.abilitywar.game.manager.effect.registry.EffectRegistry;
import daybreak.abilitywar.game.manager.effect.registry.EffectType;
import daybreak.abilitywar.game.manager.effect.registry.EffectRegistry.EffectRegistration;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.base.minecraft.nms.NMS;

@EffectManifest(name = "감전", displayName = "§d감전", method = ApplicationMethod.UNIQUE_STACK, type = {
		EffectType.MOVEMENT_RESTRICTION
})
public class ElectricShock extends AbstractGame.Effect implements Listener {

	public static final EffectRegistration<ElectricShock> registration = EffectRegistry.registerEffect(ElectricShock.class);

	public static void apply(Participant participant, TimeUnit timeUnit, int duration) {
		registration.apply(participant, timeUnit, duration);
	}

	private double damagecounter = 0;
	private final Participant participant;
	private boolean stun = false;
	private int checking = 0;
	private ArmorStand armorstand;

	public ElectricShock(Participant participant, TimeUnit timeUnit, int duration) {
		participant.getGame().super(registration, participant, (timeUnit.toTicks(duration) / 2));
		this.participant = participant;
		setPeriod(TimeUnit.TICKS, 2);
	}
	
	@Override
	protected void onStart() {
		Bukkit.getPluginManager().registerEvents(this, AbilityWar.getPlugin());
		armorstand = participant.getPlayer().getWorld().spawn(participant.getPlayer().getLocation().clone().add(200, 0, 200), ArmorStand.class);
		armorstand.setVisible(false);
		armorstand.setCustomName("§d감전§f");
		NMS.removeBoundingBox(armorstand);
	}

	@Override
	protected void run(int count) {
		armorstand.teleport(participant.getPlayer().getLocation().clone().add(0, 2.2, 0));
		if (count % 20 == 0) {
			stun = true;
		}
		if (stun) {
			checking++;
			if (checking >= 5) {
				stun = false;
				checking = 0;
			}
		}
		super.run(count);
	}
	
	@EventHandler
	private void onParticipantEffectApply(final ParticipantEffectApplyEvent e) {
		if (e.getParticipant().equals(participant) && e.getEffectType().equals(Stun.registration)) {
			damagecounter += (e.getDuration() / 10);
		}
	}
	
	@EventHandler
	private void onPlayerMove(final PlayerMoveEvent e) {
		if (e.getPlayer().getUniqueId().equals(participant.getPlayer().getUniqueId())) {
			if (stun) e.setTo(e.getFrom());
		}
	}

	@Override
	protected void onEnd() {
		participant.getPlayer().damage(damagecounter, armorstand);
		HandlerList.unregisterAll(this);
		armorstand.remove();
		super.onEnd();
	}

	@Override
	protected void onSilentEnd() {
		participant.getPlayer().damage(damagecounter, armorstand);
		HandlerList.unregisterAll(this);
		armorstand.remove();
		super.onSilentEnd();
	}
	
}