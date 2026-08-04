package rainstar.abilitywar.synergy;

import static rainstar.abilitywar.system.config.RainStarAbilitySetting.rsSynergySettings;

import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Note;
import org.bukkit.Note.Tone;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.util.Vector;

import daybreak.abilitywar.AbilityWar;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.SubscribeEvent;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.config.ability.AbilitySettings.SettingObject;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.event.participant.ParticipantDeathEvent;
import daybreak.abilitywar.game.list.mix.synergy.Synergy;
import daybreak.abilitywar.game.manager.effect.Oppress;
import daybreak.abilitywar.utils.base.color.RGB;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.base.math.VectorUtil.Vectors;
import daybreak.abilitywar.utils.base.math.geometry.Crescent;
import daybreak.abilitywar.utils.base.minecraft.nms.IHologram;
import daybreak.abilitywar.utils.base.minecraft.nms.NMS;
import daybreak.abilitywar.utils.library.ParticleLib;
import daybreak.abilitywar.utils.library.SoundLib;

@AbilityManifest(name = "심판", rank = Rank.L, species = Species.HUMAN, explain = {
		"다른 대상이 공격할 때 그 피해 수치만큼 §3심판 표식§f이 부여됩니다.",
		"자신은 그 수치의 $[PERCENTAGE]%만큼 대상에게 추가 피해를 입힙니다.",
		"또한 대상이 처치한 사람 수당 $[OPPRESS_DURATION]초간 §c§n제압§f합니다."
})
public class Judgement extends Synergy {

	public Judgement(Participant participant) {
		super(participant);
	}
	
	public static final SettingObject<Double> PERCENTAGE = 
			rsSynergySettings.new SettingObject<Double>(Judgement.class, "damage-percentage", 10.0,
            "# 심판 표식 피해량", "# 단위: %") {
        @Override
        public boolean condition(Double value) {
            return value >= 0;
        }
    };
    
	public static final SettingObject<Double> OPPRESS_DURATION = 
			rsSynergySettings.new SettingObject<Double>(Judgement.class, "oppress-duration", 0.1,
            "# 제압 지속시간", "# 단위: 초") {
        @Override
        public boolean condition(Double value) {
            return value >= 0;
        }
    };
	
    private final double percentage = PERCENTAGE.getValue() * 0.01;
    private final int oppressDuration = (int) (OPPRESS_DURATION.getValue() * 20);
	private static final RGB red = RGB.of(188, 36, 36);
	private static final RGB cyan = RGB.of(111, 179, 174);
	private static final RGB green = RGB.of(128, 254, 128);
	private final Crescent crescent = Crescent.of(1, 20);
	private Map<Player, AttackStack> damageMap = new HashMap<>();
	private Map<Player, Integer> killStackMap = new HashMap<>();
	
	@SubscribeEvent(ignoreCancelled = true, priority = Integer.MAX_VALUE, eventPriority = EventPriority.MONITOR)
	public void onEntityDamageByEntityFinal(EntityDamageByEntityEvent e) {
		Player damager = null;
		if (e.getDamager() instanceof Projectile) {
			Projectile projectile = (Projectile) e.getDamager();
			if (projectile.getShooter() instanceof Player) damager = (Player) projectile.getShooter();
		} else if (e.getDamager() instanceof Player) damager = (Player) e.getDamager();
		
		if (damager != null && !getPlayer().equals(damager)) {
			if (!damageMap.containsKey(damager)) {
				new AttackStack(damager, e.getFinalDamage()).start();
			}
		}
	}
	
	@SubscribeEvent(ignoreCancelled = true)
	public void onEntityDamageByEntity(EntityDamageByEntityEvent e) {
		Player damager = null;
		if (e.getDamager() instanceof Projectile) {
			Projectile projectile = (Projectile) e.getDamager();
			if (projectile.getShooter() instanceof Player) damager = (Player) projectile.getShooter();
		} else if (e.getDamager() instanceof Player) damager = (Player) e.getDamager();
		
		if (damageMap.containsKey(e.getEntity()) && getPlayer().equals(damager)) {
			Player player = (Player) e.getEntity();
			
			if (damageMap.get(e.getEntity()).damage < 25) {
				new CutParticle(90, green).start();
				SoundLib.GUITAR.playInstrument(getPlayer(), Note.sharp(0, Tone.C));
				SoundLib.GUITAR.playInstrument(getPlayer(), Note.sharp(1, Tone.F));
			} else if (damageMap.get(e.getEntity()).damage < 50) {
				new CutParticle(90, cyan).start();
				SoundLib.GUITAR.playInstrument(getPlayer(), Note.natural(0, Tone.F));
				SoundLib.GUITAR.playInstrument(getPlayer(), Note.natural(1, Tone.A));
			} else {
				new CutParticle(90, red).start();
				SoundLib.GUITAR.playInstrument(getPlayer(), Note.natural(0, Tone.C));
				SoundLib.GUITAR.playInstrument(getPlayer(), Note.flat(0, Tone.E));
				SoundLib.GUITAR.playInstrument(getPlayer(), Note.natural(0, Tone.G));
			}
			
			e.setDamage(e.getDamage() + (damageMap.get(player).damage * percentage));
			
			if (killStackMap.get(player) != null) Oppress.apply(getGame().getParticipant(player), TimeUnit.TICKS, oppressDuration * killStackMap.get(player));
		}
	}
	
	@SubscribeEvent
	public void onParticipantDeath(ParticipantDeathEvent e) {
		if (damageMap.containsKey(e.getPlayer())) {
			damageMap.get(e.getPlayer()).stop(false);
		}
		
		if (e.getPlayer().getKiller() != null) {
			killStackMap.put(getPlayer(), killStackMap.getOrDefault(e.getPlayer().getKiller(), 0) + 1);
		}
	}
	
	private class AttackStack extends AbilityTimer implements Listener {
		
		private final Player player;
		private final IHologram hologram;
		private boolean hid = false;
		private double damage = 0;
		private final DecimalFormat df = new DecimalFormat("0.00");
		
		private AttackStack(Player damager, double damage) {
			super(TaskType.INFINITE, -1);
			setPeriod(TimeUnit.TICKS, 1);
			this.hologram = NMS.newHologram(damager.getWorld(), damager.getLocation().getX(), damager.getLocation().getY() + damager.getEyeHeight() + 0.6, damager.getLocation().getZ(), "§4§l" + df.format(damage));
			hologram.display(getPlayer());
			this.player = damager;
			Judgement.this.damageMap.put(damager, this);
		}
		
		@Override
		protected void onStart() {
			Bukkit.getPluginManager().registerEvents(this, AbilityWar.getPlugin());
		}
		
		@Override
		protected void run(int count) {
			if (NMS.isInvisible(player)) {
				if (!hid) {
					this.hid = true;
					hologram.hide(getPlayer());
				}
			} else {
				if (hid) {
					this.hid = false;
					hologram.display(getPlayer());
				}
				hologram.teleport(player.getWorld(), player.getLocation().getX(), player.getLocation().getY() + player.getEyeHeight() + 0.6, player.getLocation().getZ(), player.getLocation().getYaw(), 0);
			}
		}
		
		@EventHandler(priority = EventPriority.MONITOR)
		public void onEntityDamageByEntity(EntityDamageByEntityEvent e) {
			Player damager = null;
			if (e.getDamager() instanceof Projectile) {
				Projectile projectile = (Projectile) e.getDamager();
				if (projectile.getShooter() instanceof Player) damager = (Player) projectile.getShooter();
			} else if (e.getDamager() instanceof Player) damager = (Player) e.getDamager();
			
			if (player.equals(damager) && !e.getEntity().equals(damager)) {
				damage += e.getFinalDamage();
				hologram.setText("§4§l" + df.format(damage));
			}
		}

		@Override
		protected void onEnd() {
			onSilentEnd();
		}

		@Override
		protected void onSilentEnd() {
			HandlerList.unregisterAll(this);
			hologram.unregister();
			Judgement.this.damageMap.remove(player);
		}
		
	}
	
	private class CutParticle extends AbilityTimer {

		private final Vector vector;
		private final Vectors crescentVectors;
		private final RGB color;
		
		private CutParticle(double angle, RGB color) {
			super(4);
			setPeriod(TimeUnit.TICKS, 1);
			this.vector = getPlayer().getLocation().getDirection().setY(0).normalize().multiply(0.5);
			this.crescentVectors = crescent.clone()
					.rotateAroundAxisY(-getPlayer().getLocation().getYaw())
					.rotateAroundAxis(getPlayer().getLocation().getDirection().setY(0).normalize(), (180 - angle) % 180);
			this.color = color;
		}

		@Override
		protected void run(int count) {
			Location baseLoc = getPlayer().getLocation().clone().add(vector).add(0, 1.3, 0);
			for (Location loc : crescentVectors.toLocations(baseLoc)) {
				ParticleLib.REDSTONE.spawnParticle(loc, color);
			}
		}

	}
	
}
