package RainStarAbility;

import java.text.DecimalFormat;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Note;
import org.bukkit.Note.Tone;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

import daybreak.abilitywar.ability.AbilityBase;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.SubscribeEvent;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.ability.SubscribeEvent.Priority;
import daybreak.abilitywar.config.ability.AbilitySettings.SettingObject;
import daybreak.abilitywar.game.GameManager;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.AbstractGame.Participant.ActionbarNotification.ActionbarChannel;
import daybreak.abilitywar.game.module.Wreck;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.base.minecraft.nms.IHologram;
import daybreak.abilitywar.utils.base.minecraft.nms.NMS;
import daybreak.abilitywar.utils.base.minecraft.version.ServerVersion;
import daybreak.abilitywar.utils.base.random.Random;
import daybreak.abilitywar.utils.library.ParticleLib;
import daybreak.abilitywar.utils.library.SoundLib;
import kotlin.ranges.RangesKt;

@AbilityManifest(
		name = "���� ����", rank = Rank.L, species = Species.HUMAN, explain = {
		"$[DURATION]�ʰ� ��a��ġ�� ������f�Ͽ�, ��c���ݡ�f�� ��b����f���� �����մϴ�.",
		"��c���ݡ�f�� ���� ���߿� ���� ��c���� �ְ�ġ�� ���� ���ط���f ���Ϸ� ��e�������� �������f,",
		"��b����f�� ���� ���߿� ���� ��b���� �ּ�ġ�� ���� ���ط���f �̻����� ��e�ö��� �ʽ��ϴ١�f.",
		"�ɷ��� $[PERIOD]�ʸ��� �����Ǿ� ������ ���ظ� ���� �� �ߵ��մϴ�."
		})

public class RandomNumberControl extends AbilityBase {

	public RandomNumberControl(Participant participant) {
		super(participant);
	}
	
	public static final SettingObject<Integer> PERIOD = abilitySettings.new SettingObject<Integer>(RandomNumberControl.class,
			"period", 30, "# �ɷ� ���� �ֱ�") {
		
		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}

	};
	
	public static final SettingObject<Integer> DURATION = abilitySettings.new SettingObject<Integer>(RandomNumberControl.class,
			"duration", 3, "# �ɷ� ���ӽð�") {
		
		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}
		
	};
	
	protected void onUpdate(Update update) {
	    if (update == Update.RESTRICTION_CLEAR) {
	    	periodtimer.start();
	    	acAttack.update("��c�ְ� ���� ���ط���7: ��e" + df.format(topDamage));
	    	acDefence.update("��b�ּ� ��� ���ط���7: ��e" + df.format(topDefence));
	    } 
	}
	
	private boolean available = false;
	private BossBar bossBar = null;
	private ActionbarChannel acAttack = newActionbarChannel();
	private ActionbarChannel acDefence = newActionbarChannel();
	private final DecimalFormat df = new DecimalFormat("0.00");
	private double topDamage = 0;
	private double topDefence = 99999;
	private double lastTopDamage = 0;
	private double lastTopDefence = 99999;
	private final int duration = DURATION.getValue();
	private final int period = (int) Math.ceil(Wreck.isEnabled(GameManager.getGame()) ? Wreck.calculateDecreasedAmount(60) * PERIOD.getValue() : PERIOD.getValue());
	
	private final AbilityTimer attackActionbarUpdater = new AbilityTimer(5) {
		
		private double damages;
		
		@Override
		public void run(int count) {
			damages = lastTopDamage + (((topDamage - lastTopDamage) * (6 - count)) / 5);
			acAttack.update("��c�ְ� ���� ���ط���7: ��e" + df.format(damages));
		}
		
		@Override
		public void onEnd() {
			acAttack.update("��c�ְ� ���� ���ط���7: ��e" + df.format(topDamage));
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
			acDefence.update("��b�ּ� ��� ���ط���7: ��e" + df.format(defences));
		}
		
		@Override
		public void onEnd() {
			acDefence.update("��b�ּ� ��� ���ط���7: ��e" + df.format(topDefence));
		}
		
		@Override
		public void onSilentEnd() {
		}
		
	}.setPeriod(TimeUnit.TICKS, 1).register();
	
	private final AbilityTimer periodtimer = new AbilityTimer() {
		
		@Override
		public void run(int count) {
			if (period == 0) stop(false);
			else if (count % (period * 20) == 0) stop(false);	
		}
		
		@Override
		public void onEnd() {
			if (period != 0) {
				getPlayer().sendMessage("��2[��a!��2] ��f���� ������ ��� �����մϴ�.");
				SoundLib.BELL.playInstrument(getPlayer(), Note.natural(1, Tone.A));	
			}
			available = true;
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
    		ParticleLib.ENCHANTMENT_TABLE.spawnParticle(getPlayer().getLocation().add(0, 2.2, 0), 0, 0, 0, 1, 0);
    		bossBar.setProgress(RangesKt.coerceIn((double) count / (duration * 20), 0, 1));
    	}
    	
		@Override
		public void onEnd() {
			bossBar.removeAll();
		}

		@Override
		public void onSilentEnd() {
			bossBar.removeAll();
		}
		
	}.setPeriod(TimeUnit.TICKS, 1).register();
	
	@SubscribeEvent(priority = Priority.HIGHEST)
	public void onEntityDamageByEntity(EntityDamageByEntityEvent e) {
		if (!e.isCancelled()) {
			if (e.getDamager().equals(getPlayer())) {
				if (available) {
					if (skill.isRunning()) skill.setCount(duration * 20);
					else skill.start();
					available = false;
					periodtimer.start();
				}
				boolean over = false, control = false;
				double startDamage = 0;
				if (topDamage < e.getDamage()) {
					lastTopDamage = topDamage;
					topDamage = e.getDamage();
					if (attackActionbarUpdater.isRunning()) attackActionbarUpdater.stop(false);
			    	attackActionbarUpdater.start();
				}
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
					if (available) {
						if (skill.isRunning()) skill.setCount(duration * 20);
						else skill.start();
						available = false;
						periodtimer.start();
					}
					boolean over = false, control = false;
					double startDamage = 0;
					if (topDamage < e.getDamage()) {
						lastTopDamage = topDamage;
						topDamage = e.getDamage();
						if (attackActionbarUpdater.isRunning()) attackActionbarUpdater.stop(false);
				    	attackActionbarUpdater.start();
					}
					if (skill.isRunning() && e.getDamage() < topDamage) {
						startDamage = e.getDamage();
						e.setDamage(topDamage);
						control = true;
					}
					if (topDamage <= e.getDamage()) over = true;
					if (!control) new Holograms(e.getEntity().getLocation(), e.getDamage(), e.getDamage(), over, control, true).start();
					else new Holograms(e.getEntity().getLocation(), startDamage, e.getDamage(), over, control, true).start();
				} else if (e.getEntity().equals(getPlayer())) {
					boolean over = false, control = false;
					double startDamage = 0;
					if (topDefence > e.getDamage()) {
						lastTopDefence = topDefence;
						topDefence = e.getDamage();
						if (defenceActionbarUpdater.isRunning()) defenceActionbarUpdater.stop(false);
						defenceActionbarUpdater.start();
					}
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
			if (e.getEntity().equals(getPlayer()) && !e.getDamager().equals(getPlayer()) && !(e.getDamager() instanceof Projectile)) {
				boolean over = false, control = false;
				double startDamage = 0;
				if (topDefence > e.getDamage()) {
					lastTopDefence = topDefence;
					topDefence = e.getDamage();
					if (defenceActionbarUpdater.isRunning()) defenceActionbarUpdater.stop(false);
					defenceActionbarUpdater.start();
				}
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
