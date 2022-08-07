package rainstar.aw.effect;

import daybreak.abilitywar.AbilityWar;
import daybreak.abilitywar.game.AbstractGame;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.manager.effect.registry.ApplicationMethod;
import daybreak.abilitywar.game.manager.effect.registry.EffectManifest;
import daybreak.abilitywar.game.manager.effect.registry.EffectRegistry;
import daybreak.abilitywar.game.manager.effect.registry.EffectRegistry.EffectRegistration;
import daybreak.abilitywar.game.manager.effect.registry.EffectType;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.base.minecraft.entity.health.Healths;
import daybreak.abilitywar.utils.library.MaterialX;
import daybreak.abilitywar.utils.library.ParticleLib;
import daybreak.abilitywar.utils.library.PotionEffects;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent.RegainReason;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.potion.PotionEffectType;

@EffectManifest(name = "프로즌 하트", displayName = "§3프로즌 하트", method = ApplicationMethod.UNIQUE_LONGEST, type = {
		EffectType.COMBAT_RESTRICTION, EffectType.HEALING_REDUCTION, EffectType.HEALING_BAN
}, description = {
		"모든 회복 효과를 받을 수 없습니다.",
		"슬롯을 변경할 때마다 2.5초를 대기해야 하고,", 
		"이동 속도와 공격 속도가 감소합니다.",
		"효과가 해제될 때, 원래 받을 회복 효과를",
		"절반으로 줄여 한꺼번에 받습니다.",
		"이 효과는 이동할 때마다 더 빨리 지속시간이 줄어듭니다."
})
public class FrozenHeart extends AbstractGame.Effect implements Listener {

	public static final EffectRegistration<FrozenHeart> registration = EffectRegistry.registerEffect(FrozenHeart.class);

	public static void apply(Participant participant, TimeUnit timeUnit, int duration) {
		registration.apply(participant, timeUnit, duration);
	}

	private final Participant participant;
	private double healstack;
	private int wait;
	private long lastMillis = System.currentTimeMillis();

	public FrozenHeart(Participant participant, TimeUnit timeUnit, int duration) {
		participant.getGame().super(registration, participant, timeUnit.toTicks(duration));
		this.participant = participant;
		this.wait = timeUnit.toTicks(duration);
		setPeriod(TimeUnit.TICKS, 1);
	}
	
	@Override
	protected void onStart() {
		Bukkit.getPluginManager().registerEvents(this, AbilityWar.getPlugin());
		super.onStart();
	}	
	
	@Override
	protected void run(int count) {
		PotionEffects.SLOW.addPotionEffect(participant.getPlayer(), 10000, 3, true);
		PotionEffects.SLOW_DIGGING.addPotionEffect(participant.getPlayer(), 10000, 3, true);
		if (count % 20 == 0) {
			ParticleLib.ITEM_CRACK.spawnParticle(participant.getPlayer().getLocation(), 0.3, 1, 0.3, 25, 0, MaterialX.ICE);
			ParticleLib.BLOCK_CRACK.spawnParticle(participant.getPlayer().getLocation(), 0.3, 1, 0.3, 25, 0, MaterialX.FROSTED_ICE);
		}
		super.run(count);
	}

	@EventHandler
	private void onSlotChange(final PlayerItemHeldEvent e) {
		if (e.getPlayer().getUniqueId().equals(participant.getPlayer().getUniqueId())) {
			if (getCount() > wait) {
				e.setCancelled(true);
			} else {
				wait = (getCount() - 50);
			}	
		}
	}
	
	@EventHandler
	private void onPlayerMove(final PlayerMoveEvent e) {
		if (participant.getPlayer().equals(e.getPlayer())) {
			final Location from = e.getFrom(), to = e.getTo();
			if (to == null || (from.getX() == to.getX() && from.getY() == to.getY() && from.getZ() == to.getZ())) return;
			final long current = System.currentTimeMillis();
			if (current - lastMillis >= 175) {
				if (getCount() != 0) {
					setCount(getCount() - 1);	
				}
				this.lastMillis = current;
			}
		}
	}
	
	@EventHandler
	private void onEntityRegainHealth(final EntityRegainHealthEvent e) {
		if (e.getEntity().getUniqueId().equals(participant.getPlayer().getUniqueId())) {
			healstack += e.getAmount();
			e.setCancelled(true);
		}
	}

	@Override
	protected void onEnd() {
		onSilentEnd();
		super.onEnd();
	}

	@Override
	protected void onSilentEnd() {
		participant.getPlayer().removePotionEffect(PotionEffectType.SLOW);
		participant.getPlayer().removePotionEffect(PotionEffectType.SLOW_DIGGING);
		final EntityRegainHealthEvent event = new EntityRegainHealthEvent(participant.getPlayer(), (healstack / 2), RegainReason.CUSTOM);
		Bukkit.getPluginManager().callEvent(event);
		if (!event.isCancelled()) {
			Healths.setHealth(participant.getPlayer(), participant.getPlayer().getHealth() + (healstack / 2));	
		}
		HandlerList.unregisterAll(this);
		super.onSilentEnd();
	}
}