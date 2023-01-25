package rainstar.abilitywar.effect;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.ArmorStand;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import daybreak.abilitywar.AbilityWar;
import daybreak.abilitywar.ability.event.AbilityPreActiveSkillEvent;
import daybreak.abilitywar.ability.event.AbilityPreTargetEvent;
import daybreak.abilitywar.game.AbstractGame;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.manager.effect.registry.ApplicationMethod;
import daybreak.abilitywar.game.manager.effect.registry.EffectManifest;
import daybreak.abilitywar.game.manager.effect.registry.EffectRegistry;
import daybreak.abilitywar.game.manager.effect.registry.EffectRegistry.EffectRegistration;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.base.minecraft.nms.NMS;
import rainstar.abilitywar.system.event.MuteRemoveEvent;

@EffectManifest(name = "침묵", displayName = "§3침묵", method = ApplicationMethod.UNIQUE_STACK, type = {
}, description = {
		"채팅을 칠 수 없습니다.",
		"액티브, 타게팅 스킬을 사용할 수 없습니다."
})
public class Mute extends AbstractGame.Effect implements Listener {

	public static final EffectRegistration<Mute> registration = EffectRegistry.registerEffect(Mute.class);

	public static void apply(Participant participant, TimeUnit timeUnit, int duration) {
		registration.apply(participant, timeUnit, duration);
	}

	private final Participant participant;
	private final ArmorStand hologram;

	public Mute(Participant participant, TimeUnit timeUnit, int duration) {
		participant.getGame().super(registration, participant, timeUnit.toTicks(duration));
		this.participant = participant;
		setPeriod(TimeUnit.TICKS, 1);
		final Location location = participant.getPlayer().getLocation();
		this.hologram = location.getWorld().spawn(location.clone().add(0, 2.2, 0), ArmorStand.class);
		hologram.setVisible(false);
		hologram.setGravity(false);
		hologram.setInvulnerable(true);
		NMS.removeBoundingBox(hologram);
		hologram.setCustomName("§c🔇");
		hologram.setCustomNameVisible(true);
	}

	@Override
	protected void run(int count) {
		hologram.teleport(participant.getPlayer().getLocation().clone().add(0, 2.2, 0));
		super.run(count);
	}
	
	@Override
	protected void onStart() {
		Bukkit.getPluginManager().registerEvents(this, AbilityWar.getPlugin());
		super.onStart();
	}
	
	@EventHandler()
	public void onChat(AsyncPlayerChatEvent e) {
		if (participant.getPlayer().equals(e.getPlayer())) {
			participant.getPlayer().sendMessage("§3[§b!§3] §c침묵되었습니다!");
			e.setCancelled(true);
		}
	}

	@EventHandler()
	public void onPreActiveSkill(AbilityPreActiveSkillEvent e) {
		if (e.getParticipant().equals(participant)) {
			participant.getPlayer().sendMessage("§3[§b!§3] §c침묵되었습니다!");
			e.setCancelled(true);
		}
	}
	
	@EventHandler()
	public void onPreTargetSkill(AbilityPreTargetEvent e) {
		if (e.getParticipant().equals(participant)) {
			participant.getPlayer().sendMessage("§3[§b!§3] §c침묵되었습니다!");
			e.setCancelled(true);
		}
	}
	
	@Override
	protected void onEnd() {
		final MuteRemoveEvent event = new MuteRemoveEvent(participant);
		Bukkit.getPluginManager().callEvent(event);
		hologram.remove();
		HandlerList.unregisterAll(this);
		super.onEnd();
	}

	@Override
	protected void onSilentEnd() {
		final MuteRemoveEvent event = new MuteRemoveEvent(participant);
		Bukkit.getPluginManager().callEvent(event);
		hologram.remove();
		HandlerList.unregisterAll(this);
		super.onSilentEnd();
	}
	
}
