package rainstar.abilitywar.ability;

import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import daybreak.abilitywar.AbilityWar;
import daybreak.abilitywar.ability.AbilityBase;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.ability.decorator.ActiveHandler;
import daybreak.abilitywar.config.ability.AbilitySettings.SettingObject;
import daybreak.abilitywar.ability.SubscribeEvent;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.AbstractGame.Participant.ActionbarNotification.ActionbarChannel;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.base.minecraft.damage.Damages;
import daybreak.abilitywar.utils.base.minecraft.entity.health.Healths;
import daybreak.abilitywar.utils.library.ParticleLib;
import daybreak.abilitywar.utils.library.SoundLib;

@AbilityManifest(name = "복수귀", rank = Rank.L, species = Species.UNDEAD, explain = {
		"살해당할 경우, $[WAIT]초간 §b유령 상태§f가 되어 돌아다니다 최대 체력으로 §a부활§f합니다.",
		"§a부활§f 이후 §c복수귀§f가 되어 체력이 $[HEALTH_LOSE]초에 걸쳐 빠르게 줄어듭니다.",
		"§c복수귀§f 모드간 자신을 죽인 사람하고만 피해를 주고받을 수 있습니다.",
		"이때 나를 죽이기 전까지 대상이 내게 줬던 피해량의 $[PERCENTAGE]%만큼 공격력이 증가합니다.",
		"철괴 우클릭 시, 체력 $[TELEPORT_COST]%를 소모해 복수 대상에게 순간 이동합니다.",
		"대상을 내 손으로 처치할 경우, §c복수귀§f 모드가 종료됩니다."
		},
		summarize = {
		"살해당할 경우 §b유령 상태§f로 돌아다니다 최대 체력으로 §a부활§f합니다.",
		"§a부활§f 이후에는 §c복수귀§f가 됩니다. 체력이 빠르게 감소하고,",
		"자신을 죽인 사람하고만 싸울 수 있으며 대상이 내게 준 피해량에 비례해",
		"공격력이 증가합니다. 대상 처치 시, §c복수귀§f 모드만 종료됩니다."
		})

public class Revenger extends AbilityBase implements ActiveHandler {
	
	public Revenger(Participant participant) {
		super(participant);
	}
	
	public static final SettingObject<Integer> WAIT = abilitySettings.new SettingObject<Integer>(Revenger.class,
			"wait", 15, "# 유령 상태 지속시간") {
		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}
	};
	
	public static final SettingObject<Integer> HEALTH_LOSE = abilitySettings.new SettingObject<Integer>(Revenger.class,
			"health-lose-duration", 60, "# 체력이 사라지는 시간", "# (최대 체력 / 시간)만큼의 체력을 매 초마다 없앱니다.") {
		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}
	};
	
	public static final SettingObject<Double> PERCENTAGE = abilitySettings.new SettingObject<Double>(Revenger.class,
			"percentage", 1.0, "# 증가할 공격력 배율", "# 단위: %") {
		@Override
		public boolean condition(Double value) {
			return value >= 0;
		}
	};
	
	public static final SettingObject<Integer> TELEPORT_COST = abilitySettings.new SettingObject<Integer>(Revenger.class,
			"teleport-cost", 10, "# 순간 이동 체력 소모량", "# 단위: %") {
		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}
	};
	
	private GameMode previousGameMode = GameMode.SURVIVAL;
	private Player killer = null;
	private boolean revenger = false;
	private final ActionbarChannel ac = newActionbarChannel();
	private final DecimalFormat df = new DecimalFormat("0.0");
	private final Map<UUID, Double> damageCounter = new HashMap<>();
	private final int wait = WAIT.getValue() * 20;
	private final int duration = HEALTH_LOSE.getValue();
	private final double percentage = PERCENTAGE.getValue() * 0.01;
	private final double teleportcost = TELEPORT_COST.getValue() * 0.01;
	private PotionEffect speed = new PotionEffect(PotionEffectType.SPEED, duration * 20, 1, true, false);
	
	@Override
	public boolean ActiveSkill(Material material, ClickType clicktype) {
		if (material == Material.IRON_INGOT && clicktype == ClickType.RIGHT_CLICK) {
			if (revenger) {
				double maxHP = getPlayer().getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue();
				Healths.setHealth(getPlayer(), getPlayer().getHealth() - (maxHP * teleportcost));
				SoundLib.ITEM_CHORUS_FRUIT_TELEPORT.playSound(getPlayer().getLocation(), 1, 1.5f);
				SoundLib.ITEM_CHORUS_FRUIT_TELEPORT.playSound(killer.getLocation(), 1, 1.5f);
				ParticleLib.DRAGON_BREATH.spawnParticle(getPlayer().getLocation(), 1, 1, 1, 50, 0);
				ParticleLib.DRAGON_BREATH.spawnParticle(killer.getLocation(), 1, 1, 1, 50, 0);
				new BukkitRunnable() {
					@Override
					public void run() {
						getPlayer().teleport(killer);
					}
				}.runTaskLater(AbilityWar.getPlugin(), 1L);	
				return true;
			} else getPlayer().sendMessage("§4[§c!§4] §f현재 복수귀 모드가 아닙니다.");
		}
		return false;
	}
	
	public AbilityTimer ghost = new AbilityTimer(wait) {
		
		@Override
		public void run(int count) {
			ac.update("§c부활까지§7: §f" + df.format(count / 20.0));
			if (count == 10) SoundLib.ENTITY_WITHER_SPAWN.playSound(getPlayer().getLocation(), 1, 1.2f);
		}
		
		@Override
		public void onEnd() {
			onSilentEnd();
		}
		
		@Override
		public void onSilentEnd() {
			ac.update(null);
			getPlayer().setGameMode(previousGameMode);
			Healths.setHealth(getPlayer(), getPlayer().getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue());
			revenger = true;
			SoundLib.ENTITY_GENERIC_EXPLODE.playSound(getPlayer().getLocation(), 1, 0.85f);
			ParticleLib.EXPLOSION_HUGE.spawnParticle(getPlayer().getLocation());
			hpdecrease.start();
			revengeParticle.start();
			getPlayer().addPotionEffect(speed);
		}
		
	}.setPeriod(TimeUnit.TICKS, 1).register();
	
	public AbilityTimer revengeParticle = new AbilityTimer() {
		
		@Override
		public void run(int count) {
			ParticleLib.FLAME.spawnParticle(getPlayer().getLocation().clone().add(0, 2.5, 0), 0, 0, 0, 1, 0);
		}
		
	}.setPeriod(TimeUnit.TICKS, 1).register();
	
	public AbilityTimer hpdecrease = new AbilityTimer() {
		
		@Override
		public void run(int count) {
			if (revenger && Damages.canDamage(getPlayer(), DamageCause.CUSTOM, 1)) {
				double maxHP = getPlayer().getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue();
				double decreaseHealth = maxHP / duration;
				if (getPlayer().getHealth() <= decreaseHealth) getPlayer().damage(Integer.MAX_VALUE);
				else Healths.setHealth(getPlayer(), getPlayer().getHealth() - decreaseHealth);
			}
		}
		
	}.setPeriod(TimeUnit.SECONDS, 1).register();
	
	@SubscribeEvent(priority = 1000)
	public void onDeath(EntityDamageByEntityEvent e) {
		if (!e.isCancelled() && !revenger && e.getEntity().equals(getPlayer())) {
	    	Player damager = null;
			if (e.getDamager() instanceof Projectile) {
				Projectile projectile = (Projectile) e.getDamager();
				if (projectile.getShooter() instanceof Player) damager = (Player) projectile.getShooter();
			} else if (e.getDamager() instanceof Player) damager = (Player) e.getDamager();
			
			if (damager != null) {
				damageCounter.put(damager.getUniqueId(), damageCounter.getOrDefault(damager.getUniqueId(), 0.0) + e.getDamage());
			}
			
			if (getPlayer().getHealth() - e.getFinalDamage() <= 0 && getPlayer().getKiller() != null) {
				SoundLib.ENTITY_GHAST_SCREAM.playSound(getPlayer().getLocation(), 1, 1.65f);
				Bukkit.broadcastMessage("§f[§c능력§f] §c" + getPlayer().getName() + "§f님의 능력은 §e복수귀§f였습니다.");
				Bukkit.broadcastMessage("§c" + getPlayer().getName() + "§f가 §a" + getPlayer().getKiller().getName() + "§f에게 살해당했습니다. §7컷!");
				Bukkit.broadcastMessage("§c" + getPlayer().getName() + "§f는 이제 §a" + getPlayer().getKiller().getName() + "§f에게 §c복수§f를 준비합니다...");
				killer = getPlayer().getKiller();
				e.setCancelled(true);
				previousGameMode = getPlayer().getGameMode() != GameMode.SPECTATOR ? getPlayer().getGameMode() : GameMode.SURVIVAL;
				getPlayer().setGameMode(GameMode.SPECTATOR);
				ghost.start();
			}	
		}
	}
	
	@SubscribeEvent
	public void onEntityDamageByEntity(EntityDamageByEntityEvent e) {
    	Player damager = null;
		if (e.getDamager() instanceof Projectile) {
			Projectile projectile = (Projectile) e.getDamager();
			if (projectile.getShooter() instanceof Player) damager = (Player) projectile.getShooter();
		} else if (e.getDamager() instanceof Player) damager = (Player) e.getDamager();
		
		if (revenger && getPlayer().equals(damager)) {
			if (e.getEntity().equals(killer)) e.setDamage(e.getDamage() + (damageCounter.get(killer.getUniqueId()) * percentage));
			else e.setCancelled(true);
		}
		
		if (revenger && getPlayer().equals(e.getEntity()) && damager != null && !damager.equals(killer)) {
			e.setCancelled(true);
		}
	}
	
	@SubscribeEvent
	public void onPlayerDeath(PlayerDeathEvent e) {
		if (getPlayer().equals(e.getEntity().getKiller()) && revenger && e.getEntity().equals(killer)) {
			hpdecrease.stop(false);
			revengeParticle.stop(false);
			revenger = false;
		}
	}

}
