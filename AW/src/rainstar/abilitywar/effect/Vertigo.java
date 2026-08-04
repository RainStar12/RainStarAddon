package rainstar.abilitywar.effect;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

import daybreak.abilitywar.AbilityWar;
import daybreak.abilitywar.game.AbstractGame;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.manager.effect.registry.ApplicationMethod;
import daybreak.abilitywar.game.manager.effect.registry.EffectManifest;
import daybreak.abilitywar.game.manager.effect.registry.EffectRegistry;
import daybreak.abilitywar.game.manager.effect.registry.EffectType;
import daybreak.abilitywar.game.manager.effect.registry.EffectRegistry.EffectRegistration;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.base.concurrent.SimpleTimer;
import daybreak.abilitywar.utils.base.minecraft.nms.IHologram;
import daybreak.abilitywar.utils.base.minecraft.nms.NMS;
import daybreak.abilitywar.utils.base.random.Random;
import daybreak.abilitywar.utils.library.PotionEffects;

@EffectManifest(name = "현기증", displayName = "§6현기증", method = ApplicationMethod.UNIQUE_STACK, type = {
		EffectType.SIGHT_CONTROL
}, description = {
		"§f멀미에 걸립니다.",
		"§f공격 시 시야가 아주 약하게 튑니다."
})
public class Vertigo extends AbstractGame.Effect implements Listener {

	public static final EffectRegistration<Vertigo> registration = EffectRegistry.registerEffect(Vertigo.class);

	public static void apply(Participant participant, TimeUnit timeUnit, int duration) {
		registration.apply(participant, timeUnit, duration);
	}

	private final Participant participant;
	private final Random random = new Random();

	public Vertigo(Participant participant, TimeUnit timeUnit, int duration) {
		participant.getGame().super(registration, participant, timeUnit.toTicks(duration));
		this.participant = participant;
		setPeriod(TimeUnit.TICKS, 1);
	}

	@Override
	protected void onStart() {
		Bukkit.getPluginManager().registerEvents(this, AbilityWar.getPlugin());
		super.onStart();
	}
	
	@Override
	protected void run(int count) {
		super.run(count);
		PotionEffects.CONFUSION.addPotionEffect(participant.getPlayer(), 100, 0, false, false, true);
		if (count % 10 == 0) new Holograms().start();
	}

	
	@Override
	protected void onEnd() {
		onSilentEnd();
	}

	@Override
	protected void onSilentEnd() {
		HandlerList.unregisterAll(this);
		super.onSilentEnd();
	}
	
	@EventHandler()
	private void onEntityDamageByEntity(EntityDamageByEntityEvent e) {
		Player damager = null;
		if (e.getDamager() instanceof Projectile) {
			Projectile projectile = (Projectile) e.getDamager();
			if (projectile.getShooter() != null && projectile.getShooter().equals(participant.getPlayer())) {
				damager = participant.getPlayer();
			}
		} else if (participant.getPlayer().equals(e.getDamager())) damager = participant.getPlayer();
		
		if (participant.getPlayer().equals(damager)) {
			float yawOffset = (float) (random.nextGaussian() * 3.3);
			float pitchOffset = (float) (random.nextGaussian() * 2.7);

			yawOffset = Math.max(-10, Math.min(10, yawOffset));
			pitchOffset = Math.max(-8, Math.min(8, pitchOffset));

			Location loc = participant.getPlayer().getLocation();

			float yaw = loc.getYaw() + yawOffset;
			float pitch = Math.max(-90, Math.min(90, loc.getPitch() + pitchOffset));

			for (Player player : Bukkit.getOnlinePlayers()) {
			    NMS.rotateHead(player, participant.getPlayer(), yaw, pitch);
			}
		}
	}
	
	private class Holograms extends SimpleTimer {
		
		private final IHologram hologram;
		private Random random = new Random();
		
		char[] codes = {
			    '0','1','2','3','4','5','6','7',
			    '8','9','a','b','c','d','e','f'
			};
		
		private Holograms() {
			super(TaskType.REVERSE, 20);
			setPeriod(TimeUnit.TICKS, 1);
			char code = codes[random.nextInt(codes.length)];
			this.hologram = NMS.newHologram(participant.getPlayer().getWorld(), 
					participant.getPlayer().getLocation().getX() + (((random.nextDouble() * 2) - 1) * 0.5),
					participant.getPlayer().getLocation().getY() + 1.25 + (((random.nextDouble() * 2) - 1) * 0.25), 
					participant.getPlayer().getLocation().getZ() + (((random.nextDouble() * 2) - 1) * 0.5), 
					"§" + code + "§l?");
			for (Player player : participant.getPlayer().getWorld().getPlayers()) {
				hologram.display(player);
			}
		}
		
		@Override
		protected void run(int count) {
			if (count % 5 == 0) {
				char code = codes[random.nextInt(codes.length)];
				hologram.setText("§" + code + "§l?");
			}
			hologram.teleport(hologram.getLocation().add(0, 0.04, 0));
		}
		
		@Override
		protected void onEnd() {
			onSilentEnd();
		}
		
		@Override
		protected void onSilentEnd() {
			hologram.unregister();
		}
		
	}
	
}