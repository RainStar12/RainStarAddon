package rainstar.abilitywar.effect;

import java.lang.reflect.Field;
import java.text.DecimalFormat;

import org.bukkit.Bukkit;
import org.bukkit.entity.ArmorStand;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.metadata.FixedMetadataValue;

import daybreak.abilitywar.AbilityWar;
import daybreak.abilitywar.game.AbstractGame;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.AbstractGame.Participant.ActionbarNotification.ActionbarChannel;
import daybreak.abilitywar.game.manager.effect.Stun;
import daybreak.abilitywar.game.manager.effect.event.ParticipantPreEffectApplyEvent;
import daybreak.abilitywar.game.manager.effect.registry.ApplicationMethod;
import daybreak.abilitywar.game.manager.effect.registry.EffectManifest;
import daybreak.abilitywar.game.manager.effect.registry.EffectRegistry;
import daybreak.abilitywar.game.manager.effect.registry.EffectType;
import daybreak.abilitywar.game.manager.effect.registry.EffectRegistry.EffectRegistration;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.base.minecraft.nms.NMS;

@EffectManifest(name = "감전", displayName = "§d감전", method = ApplicationMethod.UNIQUE_STACK, type = {
		EffectType.MOVEMENT_RESTRICTION
}, description = {
		"§f매 2초마다 0.5초간 이동이 불가능해집니다. §d감전§f 도중에 §e기절§f 효과가",
		"§f새로이 들어올 때마다 획득한 §e기절§f 시간에 비례해",
		"§f0.25초당 1의 피해를 지속 종료 시 입습니다."
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
	private Field field;
	private ActionbarChannel defaultChannel;
	private DecimalFormat df = new DecimalFormat("0.0");

	public ElectricShock(Participant participant, TimeUnit timeUnit, int duration) {
		participant.getGame().super(registration, participant, (timeUnit.toTicks(duration) / 2));
		this.participant = participant;
		setPeriod(TimeUnit.TICKS, 2);
		try {
			field = AbstractGame.Effect.class.getDeclaredField("channel");
			field.setAccessible(true);	
			defaultChannel = (ActionbarChannel) field.get(this);
		} catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	@Override
	protected void onStart() {
		Bukkit.getPluginManager().registerEvents(this, AbilityWar.getPlugin());
		armorstand = participant.getPlayer().getWorld().spawn(participant.getPlayer().getLocation().clone(), ArmorStand.class);
		armorstand.setVisible(false);
		armorstand.setCustomName("§b⚡ §d감전§f §b⚡");
		armorstand.setGravity(false);
		armorstand.setInvulnerable(true);
		armorstand.setCustomNameVisible(true);
		armorstand.setMetadata("ElectricShock", new FixedMetadataValue(AbilityWar.getPlugin(), true));
		NMS.removeBoundingBox(armorstand);
	}

	@Override
	protected void run(int count) {
		armorstand.teleport(participant.getPlayer().getLocation().clone().add(0, 2.2, 0));
		if (count % 20 == 0) {
			stun = true;
		}
		if (stun) {
			armorstand.setCustomName("§b§l⚡ §e감전§f §b§l⚡");
			checking++;
			if (checking >= 5) {
				stun = false;
				checking = 0;
			}
		} else {
			armorstand.setCustomName("§7§l⚡ §d감전§f §7§l⚡");
		}
		defaultChannel.update((stun ? "§b" : "§d") + "감전§8(" + (stun ? "§e" : "§7") + df.format(damagecounter) + "§8)§7: §f" + df.format(count / 10.0) + "초");
	}
	
	@EventHandler
	private void onParticipantEffectApply(final ParticipantPreEffectApplyEvent e) {
		if (e.getParticipant().equals(participant) && e.getEffectType().equals(Stun.registration)) {
			damagecounter += (e.getDuration() / 5);
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
	
	public int getRemainingTicks() {
	    return getCount() * 2;
	}
	
}