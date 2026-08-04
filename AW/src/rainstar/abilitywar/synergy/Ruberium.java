package rainstar.abilitywar.synergy;

import static rainstar.abilitywar.system.config.RainStarAbilitySetting.rsSynergySettings;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Note;
import org.bukkit.Note.Tone;
import org.bukkit.attribute.Attribute;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.SubscribeEvent;
import daybreak.abilitywar.ability.decorator.ActiveHandler;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.config.ability.AbilitySettings.SettingObject;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.AbstractGame.Participant.ActionbarNotification.ActionbarChannel;
import daybreak.abilitywar.game.list.mix.synergy.Synergy;
import daybreak.abilitywar.game.manager.effect.Bleed;
import daybreak.abilitywar.game.manager.effect.Bleed.ParticipantBleedEvent;
import daybreak.abilitywar.game.manager.effect.Hemophilia;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.base.color.RGB;
import daybreak.abilitywar.utils.base.concurrent.SimpleTimer.TaskType;
import daybreak.abilitywar.utils.base.minecraft.version.ServerVersion;
import daybreak.abilitywar.utils.base.random.Random;
import daybreak.abilitywar.utils.library.ParticleLib;
import daybreak.abilitywar.utils.library.PotionEffects;
import daybreak.abilitywar.utils.library.SoundLib;

@AbilityManifest(name = "루베륨", rank = Rank.S, species = Species.HUMAN, explain = {
		"철괴 우클릭으로 §c피§f를 복용할 수 있습니다.",
		"복용 시 영구적인 §6힘 $[STRENGTH]§f, §b신속 $[SPEED]§f, §8저항 $[RESISTANCE]§f 버프를 획득합니다.",
		"또한 공격 시 §c§n출혈§f이 없는 대상에게 §c§n출혈§f을 $[BLEED_DURATION]초 부여하고,",
		"§c§n출혈§f이 있다면 §4§n혈사병§f을 $[HEMOPHILLIA_DURATION]초 부여합니다.",
		"자신으로부터 $[RANGE]칸 이내에서 §c§n출혈§f되는 대상의 체력을 §c흡혈§f해옵니다.",
		"그러나 복용 이후에는 §c§l6§f초 내로 §c피§f를 다시 복용해야만 합니다. §8(§7실패 시 §4§l사망§8)",
		"적 처치 시 §c피§f의 효능과 부작용을 없애고 복용 제약 시간이 4초 증가하며,",
		"§c피§f로 주어지는 무작위 포션 효과가 영구적으로 1 증가합니다."
		})
public class Ruberium extends Synergy implements ActiveHandler {
	
	public Ruberium(Participant participant) {
		super(participant);
	}

	public static final SettingObject<Integer> STRENGTH = 
			rsSynergySettings.new SettingObject<Integer>(Ruberium.class, "strength", 1,
            "# 힘 포션 효과 계수", "# 주의! 1부터 시작합니다.", "# 1일 때 포션 효과 계수는 1레벨,", "# 2일 때 포션 효과 계수는 2레벨입니다.") {
        @Override
        public boolean condition(Integer value) {
            return value >= 0;
        }
    };
    
	public static final SettingObject<Integer> SPEED = 
			rsSynergySettings.new SettingObject<Integer>(Ruberium.class, "speed", 1,
            "# 신속 포션 효과 계수", "# 주의! 1부터 시작합니다.", "# 1일 때 포션 효과 계수는 1레벨,", "# 2일 때 포션 효과 계수는 2레벨입니다.") {
        @Override
        public boolean condition(Integer value) {
            return value >= 0;
        }
    };
    
	public static final SettingObject<Integer> RESISTANCE = 
			rsSynergySettings.new SettingObject<Integer>(Ruberium.class, "resistance", 1,
            "# 저항 포션 효과 계수", "# 주의! 1부터 시작합니다.", "# 1일 때 포션 효과 계수는 1레벨,", "# 2일 때 포션 효과 계수는 2레벨입니다.") {
        @Override
        public boolean condition(Integer value) {
            return value >= 0;
        }
    };
    
	public static final SettingObject<Double> BLEED_DURATION = 
			rsSynergySettings.new SettingObject<Double>(Ruberium.class, "bleed-duration", 2.0,
            "# 출혈 지속 시간", "# 단위: 초") {
        @Override
        public boolean condition(Double value) {
            return value >= 0;
        }
    };
    
	public static final SettingObject<Double> HEMOPHILLIA_DURATION = 
			rsSynergySettings.new SettingObject<Double>(Ruberium.class, "hemophillia-duration", 3.0,
            "# 혈사병 지속 시간", "# 단위: 초") {
        @Override
        public boolean condition(Double value) {
            return value >= 0;
        }
    };
    
	public static final SettingObject<Double> RANGE = 
			rsSynergySettings.new SettingObject<Double>(Ruberium.class, "range", 30.0,
            "# 출혈을 흡혈할 범위", "# 단위: 칸") {
        @Override
        public boolean condition(Double value) {
            return value >= 0;
        }
    };
	
	private final int strength = STRENGTH.getValue() - 1;
	private final int speed = SPEED.getValue() - 1;
	private final int resistance = RESISTANCE.getValue() - 1;
	private final int bleedDuration = (int) (BLEED_DURATION.getValue() * 20);
	private final int hemophilliaDuration = (int) (HEMOPHILLIA_DURATION.getValue() * 20);
	private final double range = RANGE.getValue() * RANGE.getValue();
	private BossBar bossBar;
	private int killstack = 0;
	private int randomAddStrength = 0, randomAddSpeed = 0, randomAddResistance = 0;
	private ActionbarChannel ac = newActionbarChannel();
	private Random random = new Random();
	private final List<Integer> pumplistA = new ArrayList<Integer>(Arrays.asList(1300, 1200, 1100, 1000, 950, 900, 850, 810, 770, 730, 700, 670, 640, 610, 
			580, 550, 530, 510, 490, 470, 450, 430, 410, 395, 380, 365, 350, 335, 320, 305, 290, 275, 260, 248, 236, 224, 212, 200, 188, 176, 164, 152, 140,
			130, 120, 110, 100, 90, 82, 74, 66, 58, 50, 44, 38, 32, 26, 20, 16, 12, 8, 6, 4, 2));   
	private final List<Integer> pumplistE = new ArrayList<Integer>(Arrays.asList(1295, 1195, 1095, 995, 945, 895, 845, 805, 765, 725, 695, 665, 635, 605, 
			575, 545, 525, 505, 485, 465, 445, 425, 405, 390, 375, 360, 345, 330, 315, 300, 285, 270, 255, 243, 231, 219, 207, 195, 183, 171, 159, 147, 135,
			125, 115, 105,  95, 85, 77, 69, 61, 53, 46, 40, 34, 30, 24, 18, 14, 10, 7, 5, 3, 1));
    
    public AbilityTimer nextpill = new AbilityTimer(TaskType.REVERSE, 120 + (killstack * 80)) {
    	
    	int startcount;
    	
    	@Override
    	public void onStart() {
    		startcount = getCount();
    		bossBar = Bukkit.createBossBar("§c루베륨을 복용해야 합니다!", BarColor.RED, BarStyle.SOLID);
    		bossBar.setProgress(1);
    		bossBar.addPlayer(getPlayer());
    		if (ServerVersion.getVersion() >= 10) bossBar.setVisible(true);
    	}
    	
    	@Override
    	public void run(int count) {
    		if (strength >= 0 && !getPlayer().hasPotionEffect(PotionEffectType.INCREASE_DAMAGE)) PotionEffects.INCREASE_DAMAGE.addPotionEffect(getPlayer(), 999999, strength + randomAddStrength, true);
    		if (speed >= 0 && !getPlayer().hasPotionEffect(PotionEffectType.SPEED)) PotionEffects.SPEED.addPotionEffect(getPlayer(), 999999, speed + randomAddSpeed, true);
    		if (resistance >= 0 && !getPlayer().hasPotionEffect(PotionEffectType.DAMAGE_RESISTANCE)) PotionEffects.DAMAGE_RESISTANCE.addPotionEffect(getPlayer(), 999999, resistance + randomAddResistance, true);
    		
    		if (pumplistA.contains(count)) SoundLib.BASS_DRUM.playInstrument(getPlayer(), Note.natural(0, Tone.A));
    		if (pumplistE.contains(count)) SoundLib.BASS_DRUM.playInstrument(getPlayer(), Note.natural(0, Tone.E));
    		
    		bossBar.setProgress(Math.max(0, Math.min(1, count  / (double) startcount)));
    		if (count <= startcount / 20.0) {
    			bossBar.setColor(BarColor.PURPLE);
    			bossBar.setTitle("§4§l루베륨을 복용해야 합니다!");
    		} else {
    			bossBar.setColor(BarColor.RED);
    			bossBar.setTitle("§c루베륨을 복용해야 합니다!");
    		}
    		
    		ac.update("§6힘 " + (strength + randomAddStrength + 1) + " §7| §b신속 " + (speed + randomAddSpeed + 1) + " §7| §3저항 " + (resistance + randomAddResistance + 1));
    	}
    	
    	@Override
    	public void onEnd() {
    		onSilentEnd();
    		getPlayer().setHealth(0);
    		Bukkit.broadcastMessage("§4[§c!§4] §e" + getPlayer().getName() + "§f님이 §4피의 유혹§f에 잠식되고 말았습니다.");
    	}
    	
    	@Override
    	public void onSilentEnd() {
    		getPlayer().removePotionEffect(PotionEffectType.INCREASE_DAMAGE);
    		getPlayer().removePotionEffect(PotionEffectType.SPEED);
    		getPlayer().removePotionEffect(PotionEffectType.DAMAGE_RESISTANCE);
    		bossBar.removeAll();
    	}
    	
	}.setPeriod(TimeUnit.TICKS, 1).register();
	
	@SubscribeEvent
	private void onParticipantBleed(ParticipantBleedEvent e) {
		if (getParticipant().equals(e.getParticipant())) return;
		final Location playerLocation = getPlayer().getLocation(), targetLocation = e.getPlayer().getLocation();
		if (playerLocation.getWorld() != targetLocation.getWorld() || playerLocation.distanceSquared(targetLocation) > range)
			return;
		new Transfusion(e.getParticipant(), e.getAmount() * 0.5).start();
	}
	
	@SubscribeEvent
	public void onEntityDamageByEntity(EntityDamageByEntityEvent e) {
		Player damager = null;
		if (e.getDamager() instanceof Projectile) {
			Projectile projectile = (Projectile) e.getDamager();
			if (projectile.getShooter() != null && projectile.getShooter() instanceof Player) damager = (Player) projectile.getShooter();
		} else if (e.getDamager() instanceof Player) damager = (Player) e.getDamager();
		
		if (getPlayer().equals(damager) && e.getEntity() instanceof Player && !getPlayer().equals(e.getEntity()) && nextpill.isRunning()) {
			Player victim = (Player) e.getEntity();
			if (getGame().getParticipant(victim).hasEffect(Bleed.registration)) Hemophilia.apply(getGame().getParticipant(victim), TimeUnit.TICKS, hemophilliaDuration);
			else Bleed.apply(getGame().getParticipant(victim), TimeUnit.TICKS, bleedDuration, 6);
		}
	}
	
	@SubscribeEvent
	public void onPlayerDeath(PlayerDeathEvent e) {
		if (getPlayer().equals(e.getEntity().getKiller()) && nextpill.isRunning()) {
			SoundLib.ENTITY_ZOMBIE_VILLAGER_CURE.playSound(getPlayer().getLocation(), 1, 0.75f);
			ParticleLib.SMOKE_LARGE.spawnParticle(getPlayer().getLocation(), 0.25, 0, 0.25, 50, 1);
			killstack++;
			if (randomAddResistance + resistance >= 3) {
				switch(random.nextInt(2)) {
				case 0:
					randomAddStrength++;
					break;
				case 1:
					randomAddSpeed++;
					break;
				}
			} else {
				switch(random.nextInt(3)) {
				case 0:
					randomAddStrength++;
					break;
				case 1:
					randomAddSpeed++;
					break;
				case 2:
					randomAddResistance++;
					break;
				}	
			}
			ac.update("§6힘 " + (strength + randomAddStrength + 1) + " §7| §b신속 " + (speed + randomAddSpeed + 1) + " §7| §3저항 " + (resistance + randomAddResistance + 1));
			nextpill.stop(true);
		}
	}
	
	@SubscribeEvent
	private void onPlayerJoin(final PlayerJoinEvent e) {
		if (getPlayer().getUniqueId().equals(e.getPlayer().getUniqueId())) {
			if (bossBar != null) bossBar.addPlayer(e.getPlayer());
		}
	}

	@SubscribeEvent
	private void onPlayerQuit(final PlayerQuitEvent e) {
		if (getPlayer().getUniqueId().equals(e.getPlayer().getUniqueId())) {
			if (bossBar != null) bossBar.removePlayer(e.getPlayer());
		}
	}
	
	public boolean ActiveSkill(Material material, ClickType clicktype) {
		if (material == Material.IRON_INGOT && clicktype == ClickType.RIGHT_CLICK) {
			if (!nextpill.isRunning()) {
				SoundLib.ENTITY_GENERIC_DRINK.playSound(getPlayer().getLocation(), 1, 0.5f);
				SoundLib.ENTITY_GENERIC_DRINK.playSound(getPlayer().getLocation(), 1, 0.75f);
				SoundLib.ENTITY_GENERIC_DRINK.playSound(getPlayer().getLocation(), 1, 1);
				SoundLib.ENTITY_GENERIC_DRINK.playSound(getPlayer().getLocation(), 1, 1.5f);
				SoundLib.ENTITY_GENERIC_DRINK.playSound(getPlayer().getLocation(), 1, 2);
				SoundLib.ENTITY_VEX_CHARGE.playSound(getPlayer(), 1, 0.65f);
				nextpill.start();
				nextpill.setCount(120 + (killstack * 80));
				return true;
			} else {
				SoundLib.ENTITY_GENERIC_DRINK.playSound(getPlayer().getLocation(), 1, 0.5f);
				SoundLib.ENTITY_GENERIC_DRINK.playSound(getPlayer().getLocation(), 1, 0.75f);
				SoundLib.ENTITY_GENERIC_DRINK.playSound(getPlayer().getLocation(), 1, 1);
				SoundLib.ENTITY_GENERIC_DRINK.playSound(getPlayer().getLocation(), 1, 1.5f);
				SoundLib.ENTITY_GENERIC_DRINK.playSound(getPlayer().getLocation(), 1, 2);
				SoundLib.ENTITY_VEX_CHARGE.playSound(getPlayer(), 1, 0.65f);
				nextpill.setCount(120 + (killstack * 80));
				return true;
			}
		}
		return false;
	}
    
	private class Transfusion extends AbilityTimer {

		private final Player target;
		private final double amount;
		private final Location currentLocation;

		private Transfusion(final Participant target, final double amount) {
			super();
			setPeriod(TimeUnit.TICKS, 1);
			this.target = target.getPlayer();
			this.amount = amount;
			this.currentLocation = this.target.getLocation().clone().add(0, 1, 0);
		}

		@Override
		protected void run(int count) {
			final Location playerLocation = getPlayer().getLocation().clone().add(0, 1, 0);
			if (playerLocation.getWorld() != currentLocation.getWorld()) {
				stop(true);
				return;
			}
			final Vector direction = playerLocation.toVector().subtract(currentLocation.toVector()).normalize().multiply(.1);
			for (int i = 0; i < 5; i++) {
				currentLocation.setX(currentLocation.getX() + direction.getX());
				currentLocation.setY(currentLocation.getY() + direction.getY());
				currentLocation.setZ(currentLocation.getZ() + direction.getZ());
				ParticleLib.REDSTONE.spawnParticle(currentLocation, RGB.RED);
				if (playerLocation.distanceSquared(currentLocation) <= .04) {
					stop(false);
					return;
				}
			}
			if (count >= 300) {
				stop(true);
			}
		}

		@Override
		protected void onEnd() {
			if (getPlayer().isDead()) return;
			getPlayer().setHealth(Math.min(getPlayer().getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue(), Math.max(1, getPlayer().getHealth() + amount)));
			SoundLib.ENTITY_PLAYER_BREATH.playSound(getPlayer(), 100f, .7757f);
			SoundLib.ENTITY_PLAYER_BREATH.playSound(getPlayer(), 100f, 2f);
		}
	}
	
}
