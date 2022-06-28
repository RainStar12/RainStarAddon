package RainStarAbility;

import java.text.DecimalFormat;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerMoveEvent;

import daybreak.abilitywar.ability.AbilityBase;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.SubscribeEvent;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.ability.decorator.ActiveHandler;
import daybreak.abilitywar.config.ability.AbilitySettings.SettingObject;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.utils.base.Formatter;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.base.minecraft.entity.health.Healths;
import daybreak.abilitywar.utils.base.minecraft.version.ServerVersion;
import daybreak.abilitywar.utils.base.random.Random;
import daybreak.abilitywar.utils.library.ParticleLib;
import daybreak.abilitywar.utils.library.SoundLib;

@AbilityManifest(name = "스구리", rank = Rank.S, species = Species.HUMAN, explain = {
		"§7패시브 §8- §c히트§f: §c히트§f 수치만큼 피해를 추가로 입습니다.",
		" 100% 히트 이상에서 대시할 경우 체력을 (§c히트§f / 100)만큼 소모합니다.",
		"§7공중에서 웅크리기 §8- §3대시§f: 바라보는 방향으로 날아갑니다. 히트 $[DASH_HEAT]% 증가.",
		" 이후 다음 공격을 1회 회피할 수 있습니다. §8(§7중첩 불가§8)",
		"§7철괴 우클릭 §8- §b액셀러레이터§f: 히트 수치를 0으로 만들고 $[ACCELERATOR_DURATION]초간 대시 거리가 증가합니다.",
		" 또한 히트 수치가 오르지 않으며, 공격 시 1~6의 추가 피해를 입힙니다. $[COOLDOWN]",
		"§8[§7HIDDEN§8] §b속도 경쟁§f: 과연 누가 더 빠를려나?"
		})

public class Suguri extends AbilityBase implements ActiveHandler {
	
	public Suguri(Participant participant) {
		super(participant);
	}
	
	public static final SettingObject<Integer> DASH_HEAT = 
			abilitySettings.new SettingObject<Integer>(Suguri.class, "dash-heat", 20,
			"# 대시로 상승되는 히트 수치") {

		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}

	};
	
	public static final SettingObject<Integer> ACCELERATOR_DURATION = 
			abilitySettings.new SettingObject<Integer>(Suguri.class, "accelerator-duration", 5,
			"# 액셀러레이터 지속 시간") {

		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}

	};
	
	public static final SettingObject<Integer> COOLDOWN = 
			abilitySettings.new SettingObject<Integer>(Suguri.class, "cooldown", 100,
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
	
	private final int duration = ACCELERATOR_DURATION.getValue() * 20;
	private final int dashheat = DASH_HEAT.getValue();
	private final Cooldown cooldown = new Cooldown(COOLDOWN.getValue());
	private final DecimalFormat df = new DecimalFormat("0");
	private double heat = 0;
	private BossBar bossBar = null;
	private long lastdash = 0;
	private final Random random = new Random();
	private boolean evade = false;
	
	private final AbilityTimer heatupdater = new AbilityTimer() {
		
    	@Override
    	public void onStart() {
    		bossBar = Bukkit.createBossBar("§c히트 §e" + df.format(heat) + "§c%", BarColor.RED, BarStyle.SOLID);
    		bossBar.setProgress(heat * 0.01);
    		bossBar.addPlayer(getPlayer());
    		if (ServerVersion.getVersion() >= 10) bossBar.setVisible(true);
    	}
    	
    	@Override 
		public void run(int count) {
    		final long current = System.currentTimeMillis();
			if (current - lastdash <= 5000) {
				 heat = Math.max(0, heat - 0.2);
			}
			
			if (accelerator.isRunning()) {
				bossBar.setColor(BarColor.BLUE);
				bossBar.setTitle("§b액셀러레이터");
				bossBar.setProgress(accelerator.getCount() / (double) duration);
			} else {
				bossBar.setColor(BarColor.RED);
				bossBar.setTitle("§c히트 §e" + df.format(heat) + "§c%");
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
	private void onPlayerMove(PlayerMoveEvent e) {
		if (!getPlayer().isOnGround() && getPlayer().isSneaking() && System.currentTimeMillis() - lastdash >= 1000) {
			getPlayer().setVelocity(getPlayer().getLocation().getDirection().normalize().multiply(accelerator.isRunning() ? 1.2 : 1.1).setY(0.15));
			lastdash = System.currentTimeMillis();
			if (!accelerator.isRunning()) heat = Math.min(100, heat + dashheat);
			SoundLib.ENTITY_FIREWORK_ROCKET_LARGE_BLAST.playSound(getPlayer(), 1, 2);
			ParticleLib.FLAME.spawnParticle(getPlayer().getLocation(), 0, 0, 0, 50, 0.35);
			evade = true;
			
			if (heat >= 100) {
				Healths.setHealth(getPlayer(), getPlayer().getHealth() - (heat * 0.01));
			}
		}
	}
	
	public boolean ActiveSkill(Material material, AbilityBase.ClickType clicktype) {
		if (material.equals(Material.IRON_INGOT) && clicktype.equals(ClickType.RIGHT_CLICK) && !cooldown.isCooldown() && !accelerator.isRunning()) {
			return accelerator.start();
		}
		return false;
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
			evade = false;
		}
		
		if (getPlayer().equals(damager) && accelerator.isRunning()) {
			e.setDamage(e.getDamage() + (random.nextInt(6) + 1));
		}
		
		if (e.getEntity().equals(getPlayer())) {
			e.setDamage(e.getDamage() * (1 + (heat * 0.01)));
		}
	}
	

}
