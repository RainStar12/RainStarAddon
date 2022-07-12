package RainStarAbility;

import java.text.DecimalFormat;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;

import daybreak.abilitywar.ability.AbilityBase;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.SubscribeEvent;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.ability.decorator.ActiveHandler;
import daybreak.abilitywar.config.ability.AbilitySettings.SettingObject;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.manager.effect.Stun;
import daybreak.abilitywar.utils.base.Formatter;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.base.minecraft.version.ServerVersion;
import daybreak.abilitywar.utils.base.random.Random;
import daybreak.abilitywar.utils.library.ParticleLib;
import daybreak.abilitywar.utils.library.SoundLib;

@AbilityManifest(name = "스구리", rank = Rank.S, species = Species.HUMAN, explain = {
		"낙하 피해를 받지 않습니다.",
		"§7패시브 §8- §c히트§f: §c히트§f가 §e100§c%§f 이상이면 받는 피해량이 비례하여 증가합니다.",
		"§7공중에서 웅크리기 §8- §3대시§f: 바라보는 방향을 향해 지속 대시합니다.",
		" 대시하는 동안 §c히트§f 수치가 기하급수적으로 상승하며, 액셀 쿨타임이 멈춥니다.",
		" 대시를 끝낼 때 현재 §c히트§f 수치에 비례해 잠시 §e기절§f합니다.",
		"§7철괴 우클릭 §8- §b액셀러레이터§f: $[ACCELERATOR_DURATION]초간 히트 수치가 0이 됩니다.",
		" 지속 중 대시를 끝낼 때 다음 공격을 1회 §b회피§f할 수 있습니다. §8(§7중첩 불가§8)",
		" 또한 공격에 1~6의 추가 피해를 줍니다. $[COOLDOWN]"
		})

public class Suguri extends AbilityBase implements ActiveHandler {
	
	public Suguri(Participant participant) {
		super(participant);
	}
	
	public static final SettingObject<Double> ACCELERATOR_DURATION = 
			abilitySettings.new SettingObject<Double>(Suguri.class, "accelerator-duration", 7.5,
			"# 액셀러레이터 지속 시간") {

		@Override
		public boolean condition(Double value) {
			return value >= 0;
		}

	};
	
	public static final SettingObject<Integer> COOLDOWN = 
			abilitySettings.new SettingObject<Integer>(Suguri.class, "cooldown", 120,
            "# 액셀 쿨타임", "# 단위: 초") {
        @Override
        public boolean condition(Integer value) {
            return value >= 0;
        }
        @Override
        public String toString() {
            return Formatter.formatCooldown(getValue());
        }
    };
    
    @Override
    public void onUpdate(Update update) {
    	if (update == Update.RESTRICTION_CLEAR) {
    		heatupdater.start();
    	}
    }
	
	private final int duration = (int) (ACCELERATOR_DURATION.getValue() * 20);
	private final Cooldown cooldown = new Cooldown(COOLDOWN.getValue());
	private final DecimalFormat df = new DecimalFormat("0");
	private double heat = 0;
	private BossBar bossBar = null;
	private final Random random = new Random();
	private boolean evade = false;
	
	private final AbilityTimer dash = new AbilityTimer() {
		
		@Override
		public void run(int count) {
			if (getPlayer().isFlying()) {
				if (cooldown.isRunning()) cooldown.setCount(cooldown.getCount());
				getPlayer().setVelocity(getPlayer().getLocation().getDirection().multiply(accelerator.isRunning() ? 1.4 : 1.25));
				heat += (count * 0.015);
			} else stop(false);
		}
		
		@Override
		public void onEnd() {
			if (heat >= 10) Stun.apply(getParticipant(), TimeUnit.TICKS, (int) (heat * 0.2));
			if (accelerator.isRunning()) evade = true;
		}
		
	}.setPeriod(TimeUnit.TICKS, 1).register();
	
	private final AbilityTimer heatupdater = new AbilityTimer() {
		
    	@Override
    	public void onStart() {
    		bossBar = Bukkit.createBossBar("§c히트 §e" + df.format(heat) + "§c%", BarColor.PINK, BarStyle.SOLID);
    		bossBar.setProgress(Math.min(1, heat * 0.01));
    		bossBar.addPlayer(getPlayer());
    		if (ServerVersion.getVersion() >= 10) bossBar.setVisible(true);
    	}
    	
    	@Override 
		public void run(int count) {
    		if (!dash.isRunning()) heat = Math.max(0, heat - 0.5);
			
			if (accelerator.isRunning()) {
				bossBar.setColor(BarColor.BLUE);
				bossBar.setTitle("§b액셀러레이터");
				bossBar.setProgress(accelerator.getCount() / (double) duration);
			} else {
				if (heat <= 100) bossBar.setColor(BarColor.PINK);
				else if (heat <= 200) bossBar.setColor(BarColor.RED);
				else bossBar.setColor(BarColor.PURPLE);
				bossBar.setTitle("§c히트 §e" + df.format(heat) + "§c%");
				bossBar.setProgress(Math.min(1, heat * 0.01));
			}
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
	
	private final AbilityTimer accelerator = new AbilityTimer(duration) {
		
    	@Override
    	public void onStart() {
    		heat = 0;
    		ParticleLib.END_ROD.spawnParticle(getPlayer().getLocation(), 1, 2, 1, 35, 1);
    		SoundLib.ENTITY_ILLUSIONER_CAST_SPELL.playSound(getPlayer().getLocation(), 1, 1.3f);
    	}
    	
    	@Override
    	public void onEnd() {
    		cooldown.start();
    	}
		
	}.setPeriod(TimeUnit.TICKS, 1).register();
	
	@SubscribeEvent(onlyRelevant = true)
	public void onPlayerMove(PlayerMoveEvent e) {
		if (dash.isRunning()) {
			ParticleLib.END_ROD.spawnParticle(e.getFrom().clone().add(0, 1, 0), 0, 0, 0, 1, 0);
		}
	}
	
	@SubscribeEvent(onlyRelevant = true)
	private void onToggleSneak(PlayerToggleSneakEvent e) {
		if (!getPlayer().isOnGround() && e.isSneaking()) {
			getPlayer().setAllowFlight(true);
			getPlayer().setFlying(true);
			dash.start();
		} else {
			getPlayer().setFlying(false);
			GameMode mode = getPlayer().getGameMode();
			getPlayer().setAllowFlight(mode != GameMode.SURVIVAL && mode != GameMode.ADVENTURE);
			dash.stop(false);
		}
	}
	
	public boolean ActiveSkill(Material material, AbilityBase.ClickType clicktype) {
		if (material.equals(Material.IRON_INGOT) && clicktype.equals(ClickType.RIGHT_CLICK) && !cooldown.isCooldown() && !accelerator.isRunning()) {
			return accelerator.start();
		}
		return false;
	}
	
	@SubscribeEvent
	public void onEntityDamage(EntityDamageEvent e) {
		if (!e.isCancelled() && getPlayer().equals(e.getEntity()) && e.getCause().equals(DamageCause.FALL)) {
			e.setCancelled(true);
			getPlayer().sendMessage("§a낙하 대미지를 받지 않습니다.");
			SoundLib.ENTITY_EXPERIENCE_ORB_PICKUP.playSound(getPlayer());
		}
	}
	
	@SubscribeEvent
	public void onEntityDamageByEntity(EntityDamageByEntityEvent e) {
		Player damager = null;
		if (e.getDamager() instanceof Projectile) {
			Projectile projectile = (Projectile) e.getDamager();
			if (projectile.getShooter() instanceof Player) damager = (Player) projectile.getShooter();
		} else if (e.getDamager() instanceof Player) damager = (Player) e.getDamager();
		
		if (evade && e.getEntity().equals(getPlayer())) {
			e.setCancelled(true);
			SoundLib.ENTITY_PLAYER_ATTACK_SWEEP.playSound(getPlayer().getLocation(), 1, 1.7f);
			getPlayer().setNoDamageTicks(20);
			evade = false;
		}
		
		if (getPlayer().equals(damager) && accelerator.isRunning()) {
			e.setDamage(e.getDamage() + (random.nextInt(6) + 1));
		}
		
		if (e.getEntity().equals(getPlayer())) {
			if (heat > 100) e.setDamage(e.getDamage() * (1 + ((heat - 100) * 0.01)));
		}
	}
	

}
