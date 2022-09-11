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
		"§7수치 §8- §c히트§f: §c히트§f가 §e100§c%§f 이상이면 받는 피해량이 비례하여 증가합니다.",
		" 최대 §e300§c%§f까지 증가하고, §e250§c%§f 이상이라면 §6잔열§f이 쌓입니다.",
		"§7수치 §8- §6잔열§f: 증가할 때 체력이 감소하고, 감소할 때 체력이 증가합니다.",
		" §6잔열§f은 최대 체력을 차지해, §6잔열§f만큼의 최대 체력은 채울 수 없습니다.",
		"§7검 우클릭 §8- §3대시§f: 바라보는 방향을 향해 지속 대시합니다. §c히트§f 수치가 지속 상승하며,",
		" 사용 직후엔 §c히트§f가 추가 증가하고 $[INV_DURATION]초간 공격을 회피합니다.",
		" 회피한 피해량에 비례하여, §6잔열§f은 감소하고, §b액셀러레이터§f 스킬 게이지가 모여듭니다.",
		"§7철괴 우클릭 §8- §b액셀러레이터§f: 게이지를 모으면 사용할 수 있습니다.",
		" $[ACCELERATOR_DURATION]초간 §c히트§f가 §e0§c%§f가 되고, §6잔열§f이 천천히 감소합니다.",
		" 또한 공격에 1~6의 추가 피해를 줍니다."
		},
		summarize = {
		"낙하 피해를 받지 않습니다.",
		"공중에서 웅크리면 지속 대시하지만 §c히트 수치§f가 기하급수적으로 상승합니다.",
		"§c히트 수치§f에 비례해 받는 피해량이 증가하고 대시 종료 시 §e기절§f합니다.",
		"§7철괴 우클릭으로§f §c히트 수치§f를 0으로 만들고 대시 종료 후 공격을 회피합니다.",
		"또한 공격에 1~6의 추가 피해를 입힙니다."
		})

public class SuguriR extends AbilityBase implements ActiveHandler {
	
	public SuguriR(Participant participant) {
		super(participant);
	}
	
	public static final SettingObject<Double> ACCELERATOR_DURATION = 
			abilitySettings.new SettingObject<Double>(SuguriR.class, "accelerator-duration", 7.5,
			"# 액셀러레이터 지속 시간") {

		@Override
		public boolean condition(Double value) {
			return value >= 0;
		}

	};
	
	public static final SettingObject<Integer> COOLDOWN = 
			abilitySettings.new SettingObject<Integer>(SuguriR.class, "cooldown", 120,
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
    		ParticleLib.END_ROD.spawnParticle(getPlayer().getLocation(), 1, 2, 1, 35, 1);
    		SoundLib.ENTITY_ILLUSIONER_CAST_SPELL.playSound(getPlayer().getLocation(), 1, 1.3f);
    	}
    	
    	@Override
    	public void run(int count) {
    		heat = 0;
    		dash.setCount(0);
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
	
	@SuppressWarnings("deprecation")
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
