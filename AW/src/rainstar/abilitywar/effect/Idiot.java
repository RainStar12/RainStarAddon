package rainstar.abilitywar.effect;

import java.text.DecimalFormat;
import java.util.Random;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent.RegainReason;

import daybreak.abilitywar.AbilityWar;
import daybreak.abilitywar.ability.AbilityBase;
import daybreak.abilitywar.game.AbstractGame;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.AbstractGame.Participant.ActionbarNotification.ActionbarChannel;
import daybreak.abilitywar.game.manager.effect.registry.ApplicationMethod;
import daybreak.abilitywar.game.manager.effect.registry.EffectManifest;
import daybreak.abilitywar.game.manager.effect.registry.EffectRegistry;
import daybreak.abilitywar.game.manager.effect.registry.EffectType;
import daybreak.abilitywar.game.manager.effect.registry.EffectRegistry.EffectRegistration;
import daybreak.abilitywar.utils.base.concurrent.SimpleTimer;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.base.minecraft.entity.health.Healths;
import daybreak.abilitywar.utils.base.minecraft.nms.NMS;
import daybreak.abilitywar.utils.base.minecraft.version.NMSVersion;
import daybreak.abilitywar.utils.base.minecraft.version.ServerVersion;

@EffectManifest(name = "멍청함", displayName = "§c멍청함", method = ApplicationMethod.UNIQUE_LONGEST, type = {
		EffectType.ABILITY_RESTRICTION, EffectType.COMBAT_RESTRICTION, EffectType.SIGHT_CONTROL
}, description = {
		"§f능력 사용법을 잊어버려 능력이 비활성화됩니다.",
		"§f공격을 하면 §84.1§f초 후에 그 사실을 잊어버려 피해를 받은 사람이 다시 회복합니다.",
		"§f이때, §c31§f% 확률로 자신의 시야가 무작위로 돌아갑니다."
})
public class Idiot extends AbstractGame.Effect implements Listener {

	public static final EffectRegistration<Idiot> registration = EffectRegistry.registerEffect(Idiot.class);

	public static void apply(Participant participant, TimeUnit timeUnit, int duration) {
		registration.apply(participant, timeUnit, duration);
	}

	private final Participant participant;
	private final ArmorStand hologram;
	private int stack = 0;
	private boolean direction = true;

	public Idiot(Participant participant, TimeUnit timeUnit, int duration) {
		participant.getGame().super(registration, participant, timeUnit.toTicks(duration));
		this.participant = participant;
		final Location location = participant.getPlayer().getLocation();
		this.hologram = location.getWorld().spawn(location.clone().add(0, 2.2, 0), ArmorStand.class);
		hologram.setVisible(false);
		hologram.setGravity(false);
		hologram.setInvulnerable(true);
		NMS.removeBoundingBox(hologram);
		hologram.setCustomNameVisible(true);
		hologram.setCustomName("§c§o바부");
		setPeriod(TimeUnit.TICKS, 1);
	}

	@Override
	protected void onStart() {
		Bukkit.getPluginManager().registerEvents(this, AbilityWar.getPlugin());
		restrict(true);
	}
	
	private void restrict(boolean restrict) {
		final AbilityBase ability = participant.getAbility();
		if (ability != null) ability.setRestricted(restrict);
	}
	
	@EventHandler(priority = EventPriority.MONITOR)
	private void onEntityDamageByEntity(EntityDamageByEntityEvent e) {
    	Player damager = null;
		if (e.getDamager() instanceof Projectile) {
			Projectile projectile = (Projectile) e.getDamager();
			if (projectile.getShooter() instanceof Player) damager = (Player) projectile.getShooter();
		} else if (e.getDamager() instanceof Player) damager = (Player) e.getDamager();
		
		if (participant.getPlayer().equals(damager) && !e.isCancelled() && e.getEntity() instanceof Player && getGame().isParticipating((Player) e.getEntity())) {
			new ForgetTimer(82, e.getFinalDamage(), (Player) e.getEntity()).start();
		}
	}
	
	@Override
	protected void run(int count) {
		super.run(count);
		if (hologram.isValid()) {
			if (direction) stack++;
			else stack--;
			if (stack <= 0 || stack >= 30) {
				this.direction = !direction;
			}
			final Location location = participant.getPlayer().getLocation().clone().add(0, (ServerVersion.isAboveOrEqual(NMSVersion.v1_17_R1) ? 4 : 2) + (stack * 0.008), 0);
			hologram.teleport(location);
		}
		restrict(true);
	}

	@Override
	protected void onEnd() {
		hologram.remove();
		HandlerList.unregisterAll(this);
		restrict(false);
		super.onEnd();
	}

	@Override
	protected void onSilentEnd() {
		hologram.remove();
		HandlerList.unregisterAll(this);
		restrict(false);
		super.onSilentEnd();
	}
	
	private class ForgetTimer extends SimpleTimer {
		
		private final double damage;
		private final ActionbarChannel channel;
		private final Player target;
		private final DecimalFormat df = new DecimalFormat("0.0");
		private final Random random = new Random();
		
		public ForgetTimer(int duration, Double damage, Player target) {
			super(TaskType.REVERSE, duration);
			setPeriod(TimeUnit.TICKS, 1);
			this.target = target;
			this.channel = getGame().getParticipant(target).actionbar().newChannel();
			this.damage = damage;
			
		}
		
		@Override
		public void run(int count) {
			channel.update("§e" + df.format(count / (double) 20) + "§f초: §a+" + (count < 20 ? "§a§k" : "") + df.format(damage));
		}
		
		@Override
		public void onEnd() {
			final EntityRegainHealthEvent event = new EntityRegainHealthEvent(target, damage, RegainReason.CUSTOM);
			Bukkit.getPluginManager().callEvent(event);
			if (!event.isCancelled()) {
				Healths.setHealth(target, target.getHealth() + event.getAmount());
				if (random.nextInt(100) < 31) {
					final Location location = participant.getPlayer().getLocation();
					float yaw = location.getYaw() + random.nextInt(180) - 90;
					if (yaw > 180 || yaw < -180) {
						float mod = yaw % 180;
						if (mod < 0) {
							yaw = 180 + mod;
						} else if (mod > 0) {
							yaw = -180 + mod;
						}
					}
					for (Player player : Bukkit.getOnlinePlayers()) {
						NMS.rotateHead(player, participant.getPlayer(), yaw, location.getPitch() + random.nextInt(90) - 45);	
					}
				}
			}
			channel.unregister();
		}
		
		@Override
		public void onSilentEnd() {
			channel.unregister();
		}
		
	}

}