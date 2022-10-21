package RainStarEffect;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerMoveEvent;

import daybreak.abilitywar.AbilityWar;
import daybreak.abilitywar.ability.AbilityBase;
import daybreak.abilitywar.game.AbstractGame;
import daybreak.abilitywar.game.AbstractGame.GameTimer;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.manager.effect.registry.ApplicationMethod;
import daybreak.abilitywar.game.manager.effect.registry.EffectManifest;
import daybreak.abilitywar.game.manager.effect.registry.EffectRegistry;
import daybreak.abilitywar.game.manager.effect.registry.EffectType;
import daybreak.abilitywar.game.manager.effect.registry.EffectRegistry.EffectRegistration;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.base.minecraft.entity.health.Healths;
import daybreak.abilitywar.utils.base.minecraft.nms.NMS;
import daybreak.abilitywar.utils.base.random.Random;
import daybreak.abilitywar.utils.library.SoundLib;

@EffectManifest(name = "덧없는 꿈", displayName = "§d덧없는 꿈", method = ApplicationMethod.UNIQUE_IGNORE, type = {
		EffectType.MOVEMENT_RESTRICTION, EffectType.COMBAT_RESTRICTION
}, description = {
		"§f지속적으로 체력을 잃습니다.",
		"§f흐르고 있는 모든 시간이 멈추게 되며",
		"§f누군가가 공격해 깨우기 전까진 이동할 수 없습니다.",
		"§f강제로 깨어날 경우 남은 시간만큼 정신이 몽롱해져",
		"§f모든 공격이 50% 확률로 빗나갑니다.",
		"§f살아남을 시 체력을 잃는 효과로 잃은 체력은 보상됩니다."
})
public class VainDream extends AbstractGame.Effect implements Listener {

	public static final EffectRegistration<VainDream> registration = EffectRegistry.registerEffect(VainDream.class);

	public static void apply(Participant participant, TimeUnit timeUnit, int duration) {
		registration.apply(participant, timeUnit, duration);
	}

	private final Participant participant;
	private boolean wakeup = false;
	private Random random = new Random();
	private final ArmorStand hologram;
	private int stack;
	
	public VainDream(Participant participant, TimeUnit timeUnit, int duration) {
		participant.getGame().super(registration, participant, (timeUnit.toTicks(duration) / 2));
		this.participant = participant;
		setPeriod(TimeUnit.TICKS, 2);
		final Location location = participant.getPlayer().getLocation();
		this.hologram = location.getWorld().spawn(location.clone().add(0, 2.2, 0), ArmorStand.class);
		hologram.setVisible(false);
		hologram.setGravity(false);
		hologram.setInvulnerable(true);
		NMS.removeBoundingBox(hologram);
		hologram.setCustomNameVisible(true);
		hologram.setCustomName("§3Zzzzz...");
	}
	
	@Override
	protected void onStart() {
		Bukkit.getPluginManager().registerEvents(this, AbilityWar.getPlugin());
	}
	
	@EventHandler
	public void onEntityDamageByEntity(EntityDamageByEntityEvent e) {
		if (!wakeup) {
			if (e.getEntity().equals(participant.getPlayer())) {
				wakeup = true;
				hologram.setCustomName("§dஓஓஓ");
				if (participant.hasAbility() && !participant.getAbility().isRestricted()) {
					AbilityBase ab = participant.getAbility();
					for (GameTimer t : ab.getTimers()) {
						if (t.isPaused()) t.resume();
					}
				}
			}	
		} else {
			if (e.getDamager() instanceof Projectile) {
				Projectile p = (Projectile) e.getDamager();
				if (participant.getPlayer().equals(p.getShooter())) {
					if (random.nextBoolean()) {
						SoundLib.ENTITY_PLAYER_ATTACK_SWEEP.playSound(participant.getPlayer().getLocation(), 1, 1.7f);
						e.setCancelled(true);	
					}
				}
			} else if (e.getDamager().equals(participant.getPlayer())) {
				if (random.nextBoolean()) {
					SoundLib.ENTITY_PLAYER_ATTACK_SWEEP.playSound(participant.getPlayer().getLocation(), 1, 1.7f);
					e.setCancelled(true);
				}
			}
		}
	}
	
	@EventHandler
	public void onPlayerMove(PlayerMoveEvent e) {
		if (participant.getPlayer().equals(e.getPlayer()) && !wakeup) {
			e.setCancelled(true);
		}
	}
	
	@Override
	protected void run(int count) {
		if (count % 10 == 0) {
			Healths.setHealth(participant.getPlayer(), Math.max(1, participant.getPlayer().getHealth() - 1));
			stack++;
		}
		hologram.teleport(participant.getPlayer().getLocation().clone().add(0, 2.2, 0));
		if (!wakeup) {
			if (participant.hasAbility() && !participant.getAbility().isRestricted()) {
				AbilityBase ab = participant.getAbility();
				for (GameTimer t : ab.getTimers()) {
					if (t.isRunning()) t.pause();
				}
			}
		}
		
		super.run(count);
	}

	@Override
	protected void onEnd() {
		onSilentEnd();
		HandlerList.unregisterAll(this);
		super.onEnd();
	}

	@Override
	protected void onSilentEnd() {
		if (!wakeup) {
			if (participant.hasAbility() && !participant.getAbility().isRestricted()) {
				AbilityBase ab = participant.getAbility();
				for (GameTimer t : ab.getTimers()) {
					if (t.isPaused()) t.resume();
				}
			}
		}
		if (!participant.getPlayer().isDead()) {
			Healths.setHealth(participant.getPlayer(), participant.getPlayer().getHealth() + stack);
		}
		hologram.remove();
		HandlerList.unregisterAll(this);
		super.onSilentEnd();
	}
	
}