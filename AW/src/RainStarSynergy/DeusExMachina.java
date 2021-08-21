package RainStarSynergy;

import java.text.DecimalFormat;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent;

import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.SubscribeEvent;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.ability.SubscribeEvent.Priority;
import daybreak.abilitywar.ability.decorator.ActiveHandler;
import daybreak.abilitywar.config.ability.AbilitySettings.SettingObject;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.AbstractGame.Participant.ActionbarNotification.ActionbarChannel;
import daybreak.abilitywar.game.list.mix.synergy.Synergy;
import daybreak.abilitywar.utils.base.Formatter;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.base.math.geometry.Circle;
import daybreak.abilitywar.utils.base.minecraft.nms.IHologram;
import daybreak.abilitywar.utils.base.minecraft.nms.NMS;
import daybreak.abilitywar.utils.base.minecraft.version.ServerVersion;
import daybreak.abilitywar.utils.base.random.Random;
import daybreak.abilitywar.utils.library.ParticleLib;
import daybreak.abilitywar.utils.library.SoundLib;
import kotlin.ranges.RangesKt;

@AbilityManifest(name = "���콺 ���� ��Ű��", rank = Rank.L, species = Species.GOD, explain = {
		"ö�� ��Ŭ�� �� $[DURATION]�ʰ� ��c���ݷ¡�7(ATK)��f, ��3���¡�7(DEF)��f, ��dȸ���¡�7(REC)��f�� ��a��ġ�� ���ۡ�f�Ͽ�",
		"���� ���� �־��� ��b�ְ� ���ġ��f�� �ҷ��ɴϴ�. $[COOLDOWN]",
		"�ɷ��� ����ϰ� ���� ��b�ְ� ���ġ��f�� �ʱ�ȭ�˴ϴ�.",
		"��b[��7���̵�� �����ڡ�b] ��dhorn1111"
})

public class DeusExMachina extends Synergy implements ActiveHandler {

	public DeusExMachina(Participant participant) {
		super(participant);
	}
	
	public static final SettingObject<Integer> COOLDOWN = synergySettings.new SettingObject<Integer>(DeusExMachina.class, 
			"cooldown", 60, "# ��Ÿ��") {

		@Override
		public boolean condition(Integer value) {
			return value >= 1;
		}

		@Override
		public String toString() {
			return Formatter.formatCooldown(getValue());
		}

	};
	
	public static final SettingObject<Integer> DURATION = synergySettings.new SettingObject<Integer>(DeusExMachina.class,
			"duration", 4, "# �ɷ� ���ӽð�") {
		
		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}
		
	};
	
	protected void onUpdate(Update update) {
	    if (update == Update.RESTRICTION_CLEAR) {
	    	acAttack.update("��cATK��7: ��e" + df.format(topDamage));
	    	acDefence.update("��bDEF��7: ��e" + df.format(topDefence));
	    	acRecovery.update("��aREC��7: ��e" + df.format(topRecovery));
	    } 
	}
	
	private final Cooldown cool = new Cooldown(COOLDOWN.getValue());
	private BossBar bossBar = null;
	private ActionbarChannel acAttack = newActionbarChannel();
	private ActionbarChannel acDefence = newActionbarChannel();
	private ActionbarChannel acRecovery = newActionbarChannel();
	private final DecimalFormat df = new DecimalFormat("0.00");
	private double topDamage = 0, topDefence = 99999, topRecovery = 0;
	private double lastTopDamage = 0, lastTopDefence = 99999, lastTopRecovery = 0;
	private final int duration = DURATION.getValue();
	private static final Circle circle = Circle.of(1.5, 20);
	
	@Override
	public boolean ActiveSkill(Material material, ClickType clickType) {
		if (material == Material.IRON_INGOT && clickType == ClickType.RIGHT_CLICK) {
			if (!cool.isCooldown()) {
				if (!skill.isRunning()) {
					skill.start();	
				} else {
					getPlayer().sendMessage("��2[��a!��2] ��f�ɷ��� ���� ���Դϴ�.");
				}
			}
		}
		return false;
	}
	
	private final AbilityTimer attackActionbarUpdater = new AbilityTimer(5) {
		
		private double damages;
		
		@Override
		public void run(int count) {
			damages = lastTopDamage + (((topDamage - lastTopDamage) * (6 - count)) / 5);
			acAttack.update("��cATK��7: ��e" + df.format(damages));
		}
		
		@Override
		public void onEnd() {
			acAttack.update("��cATK��7: ��e" + df.format(topDamage));
		}
		
		@Override
		public void onSilentEnd() {
		}
		
	}.setPeriod(TimeUnit.TICKS, 1).register();
	
	private final AbilityTimer defenceActionbarUpdater = new AbilityTimer(5) {
		
		private double defences;
		
		@Override
		public void run(int count) {
			defences = lastTopDefence - (((lastTopDefence - topDefence) * (6 - count)) / 5);
			acDefence.update("��bDEF��7: ��e" + df.format(defences));
		}
		
		@Override
		public void onEnd() {
			acDefence.update("��bDEF��7: ��e" + df.format(topDefence));
		}
		
		@Override
		public void onSilentEnd() {
		}
		
	}.setPeriod(TimeUnit.TICKS, 1).register();
	
	private final AbilityTimer recoveryActionbarUpdater = new AbilityTimer(5) {
		
		private double recovers;
		
		@Override
		public void run(int count) {
			recovers = lastTopRecovery + (((topRecovery - lastTopRecovery) * (6 - count)) / 5);
			acRecovery.update("��aREC��7: ��e" + df.format(recovers));
		}
		
		@Override
		public void onEnd() {
			acRecovery.update("��aREC��7: ��e" + df.format(topRecovery));
		}
		
		@Override
		public void onSilentEnd() {
		}
		
	}.setPeriod(TimeUnit.TICKS, 1).register();
	
	private final AbilityTimer skill = new AbilityTimer(duration * 20) {
		
		@Override
    	public void onStart() {
			SoundLib.ENTITY_PLAYER_LEVELUP.playSound(getPlayer(), 1, 0.9f);
    		bossBar = Bukkit.createBossBar("��b���� ���� ��", BarColor.GREEN, BarStyle.SOLID);
    		bossBar.setProgress(1);
    		bossBar.addPlayer(getPlayer());
    		if (ServerVersion.getVersion() >= 10) bossBar.setVisible(true);
    	}
    	
    	@Override
		public void run(int count) {
			for (Location loc : circle.toLocations(getPlayer().getLocation().clone().add(0, 1, 0))) {
				ParticleLib.ENCHANTMENT_TABLE.spawnParticle(loc, 0, 0, 0, 1, 0.15f);	
			}
    		bossBar.setProgress(RangesKt.coerceIn((double) count / (duration * 20), 0, 1));
    	}
    	
		@Override
		public void onEnd() {
			onSilentEnd();
		}

		@Override
		public void onSilentEnd() {
			topDamage = 0;
			topDefence = 99999;
			topRecovery = 0;
	    	acAttack.update("��cATK��7: ��e��k" + 0.00);
	    	acDefence.update("��bDEF��7: ��e��k" + 0.00);
	    	acRecovery.update("��aREC��7: ��e��k" + 0.00);
			bossBar.removeAll();
			cool.start();
		}
		
	}.setPeriod(TimeUnit.TICKS, 1).register();
	
	@SubscribeEvent(priority = Priority.HIGHEST)
	public void onEntityRegainHealth(EntityRegainHealthEvent e) {
		if (!e.isCancelled()) {
			boolean over = false, control = false;
			double startRecovery = 0;
			if (topRecovery < e.getAmount()) {
				lastTopRecovery = topRecovery;
				topRecovery = e.getAmount();
				if (recoveryActionbarUpdater.isRunning()) recoveryActionbarUpdater.stop(false);
				recoveryActionbarUpdater.start();	
			}
			if (e.getEntity().equals(getPlayer())) {
				if (skill.isRunning() && e.getAmount() < topRecovery) {
					startRecovery = e.getAmount();
					e.setAmount(topRecovery);
					control = true;
				}
				if (topRecovery <= e.getAmount()) over = true;
				if (!control) new Holograms(e.getEntity().getLocation(), e.getAmount(), e.getAmount(), over, control, true).start();
				else new Holograms(e.getEntity().getLocation(), startRecovery, e.getAmount(), over, control, true).start();
			}
		}
	}
	
	@SubscribeEvent(priority = Priority.HIGHEST)
	public void onEntityDamageByEntity(EntityDamageByEntityEvent e) {
		if (!e.isCancelled()) {
			boolean over = false, control = false;
			double startDamage = 0;
			if (topDamage < e.getDamage()) {
				lastTopDamage = topDamage;
				topDamage = e.getDamage();
				if (attackActionbarUpdater.isRunning()) attackActionbarUpdater.stop(false);
			    attackActionbarUpdater.start();	
			}
			
			if (e.getDamager().equals(getPlayer())) {
				if (skill.isRunning() && e.getDamage() < topDamage) {
					startDamage = e.getDamage();
					e.setDamage(topDamage);
					control = true;
				}
				if (topDamage <= e.getDamage()) over = true;
				if (!control) new Holograms(e.getEntity().getLocation(), e.getDamage(), e.getDamage(), over, control, true).start();
				else new Holograms(e.getEntity().getLocation(), startDamage, e.getDamage(), over, control, true).start();
			}
			if (e.getDamager() instanceof Projectile) {
				Projectile projectile = (Projectile) e.getDamager();
				if (getPlayer().equals(projectile.getShooter())) {
					if (topDamage <= e.getDamage()) over = true;
					if (!control) new Holograms(e.getEntity().getLocation(), e.getDamage(), e.getDamage(), over, control, true).start();
					else new Holograms(e.getEntity().getLocation(), startDamage, e.getDamage(), over, control, true).start();
				}
			}
			
			if (e.getDamager() instanceof Projectile) {
				Projectile projectile = (Projectile) e.getDamager();
				if (!getPlayer().equals(projectile.getShooter())) {
					if (topDefence > e.getDamage()) {
						if (topDefence > e.getDamage()) {
							lastTopDefence = topDefence;
							topDefence = e.getDamage();
							if (defenceActionbarUpdater.isRunning()) defenceActionbarUpdater.stop(false);
							defenceActionbarUpdater.start();	
						}
					}
					if (e.getEntity().equals(getPlayer())) {
						if (skill.isRunning() && e.getDamage() > topDefence) {
							startDamage = e.getDamage();
							e.setDamage(topDefence);
							control = true;
						}
						if (topDefence >= e.getDamage()) over = true;
						if (!control) new Holograms(e.getEntity().getLocation(), e.getDamage(), e.getDamage(), over, control, false).start();
						else new Holograms(e.getEntity().getLocation(), startDamage, e.getDamage(), over, control, false).start();	
					}
				}
			} else {
				if (topDefence > e.getDamage()) {
					if (topDefence > e.getDamage()) {
						lastTopDefence = topDefence;
						topDefence = e.getDamage();
						if (defenceActionbarUpdater.isRunning()) defenceActionbarUpdater.stop(false);
						defenceActionbarUpdater.start();	
					}
				}
				if (e.getEntity().equals(getPlayer())) {
					if (skill.isRunning() && e.getDamage() > topDefence) {
						startDamage = e.getDamage();
						e.setDamage(topDefence);
						control = true;
					}
					if (topDefence >= e.getDamage()) over = true;
					if (!control) new Holograms(e.getEntity().getLocation(), e.getDamage(), e.getDamage(), over, control, false).start();
					else new Holograms(e.getEntity().getLocation(), startDamage, e.getDamage(), over, control, false).start();	
				}
			}	
		}
	}
	
	private class Holograms extends AbilityTimer {
		
		private final IHologram hologram;
		private Random random = new Random();
		private final DecimalFormat damageDF = new DecimalFormat("0.00");
		private boolean control;
		private double damages;
		private double damage;
		private double controldamage;
		
		private Holograms(Location hitLoc, double damage, double controldamage, boolean over, boolean control, boolean showWho) {
			super(TaskType.REVERSE, 30);
			setPeriod(TimeUnit.TICKS, 1);
			this.hologram = NMS.newHologram(getPlayer().getWorld(), 
					hitLoc.getX() + (((random.nextDouble() * 2) - 1) * 0.5),
					hitLoc.getY() + 1.25 + (((random.nextDouble() * 2) - 1) * 0.25), 
					hitLoc.getZ() + (((random.nextDouble() * 2) - 1) * 0.5), 
					over ? "��b��l" + damageDF.format(damage) : "��c��l" + damageDF.format(damage));
			this.damage = damage;
			this.control = control;
			this.controldamage = controldamage;
			if (showWho) {
				hologram.display(getPlayer());
			} else {
				for (Player player : getPlayer().getWorld().getPlayers()) {
					hologram.display(player);
				}	
			}
		}
		
		@Override
		protected void run(int count) {
			if (control) {
				if (count > 20) {
					if (damage > controldamage) {
						damages = damage - (((damage - controldamage) * (31 - count)) / 10);	
					} else if (damage < controldamage) {
						damages = damage + (((controldamage - damage) * (31 - count)) / 10);
					} else if (damage == controldamage) {
						damages = damage;
					}
					hologram.setText("��a��l" + damageDF.format(damages));
				}	
			}
			hologram.teleport(hologram.getLocation().add(0, 0.03, 0));
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
