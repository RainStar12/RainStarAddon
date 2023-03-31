package rainstar.abilitywar.ability;

import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.event.entity.EntityDamageByBlockEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.entity.EntityRegainHealthEvent.RegainReason;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import daybreak.abilitywar.ability.AbilityBase;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.ability.decorator.ActiveHandler;
import daybreak.abilitywar.ability.SubscribeEvent;
import daybreak.abilitywar.config.ability.AbilitySettings.SettingObject;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.AbstractGame.Participant.ActionbarNotification.ActionbarChannel;
import daybreak.abilitywar.utils.base.Formatter;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.base.minecraft.damage.Damages;
import daybreak.abilitywar.utils.base.minecraft.entity.health.Healths;
import daybreak.abilitywar.utils.base.minecraft.nms.NMS;

@AbilityManifest(name = "파이로매니악", rank = Rank.S, species = Species.HUMAN, explain = {
		"§7철괴 좌클릭 §8- §cTNT 캐논§f: 바라보는 방향으로 §cTNT§f를 날립니다. $[TNT_CANNON_COOLDOWN]",
		"§7철괴 우클릭 §8- §3리벤지 붐§f: $[REVENGE_BOOM_DURATION]초간 공격을 할 수 없고, 받는 피해량이 $[REVENGE_BOOM_DECREASE]% 감소합니다.",
		" 종료 시 피해를 준 대상들에게 준 피해량만큼 폭발 피해를 입힙니다. $[REVENGE_BOOM_COOLDOWN]",
		"§7패시브 §8- §c파이로매니악§f: 폭발 피해량을 받지 않고 그 1/3만큼 §d회복§f합니다.",
		" 근접 치명타 공격 $[CRIT_COUNT]회마다 자신의 위치에 §c$[EXPLOSIVE]§f의 폭발을 일으킵니다.",
		"§a[§e능력 제공자§a] §3Dire5778"
		},
		summarize = {
		"§7철괴 좌클릭 시§f 바라보는 방향으로 §cTNT§f를 날립니다.",
		"§7철괴 우클릭 시§f 잠시간 공격이 불가능하고, 받는 피해량이 감소하며, 종료될 때",
		"자신을 공격한 대상들에게 폭발 피해를 입힙니다.",
		"기본적으로 모든 폭발 피해량을 받지 않고 그 절반만큼 §d회복§f합니다.",
		"근접 치명타 공격 $[CRIT_COUNT]회마다 자신의 위치에 §c$[EXPLOSIVE]§f의 폭발을 일으킵니다."
		})
public class Pyromaniac extends AbilityBase implements ActiveHandler {
	
	public Pyromaniac(Participant participant) {
		super(participant);
	}
	
	public static final SettingObject<Integer> TNT_CANNON_COOLDOWN = 
			abilitySettings.new SettingObject<Integer>(Pyromaniac.class, "tnt-cannon-cooldown", 60,
			"# TNT 캐논 쿨타임") {

		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}
		
		@Override
		public String toString() {
			return Formatter.formatCooldown(getValue());
		}

	};
	
	public static final SettingObject<Double> REVENGE_BOOM_DURATION = 
			abilitySettings.new SettingObject<Double>(Pyromaniac.class, "revenge-boom-duration", 5.0,
			"# 리벤지 붐 지속시간", "# 단위: 초") {

		@Override
		public boolean condition(Double value) {
			return value >= 0;
		}

	};
	
	public static final SettingObject<Integer> REVENGE_BOOM_DECREASE = 
			abilitySettings.new SettingObject<Integer>(Pyromaniac.class, "revenge-boom-decrease", 20,
			"# 리벤지 붐 받는 피해량 감소량", "# 단위: %") {

		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}

	};
	
	public static final SettingObject<Integer> REVENGE_BOOM_COOLDOWN = 
			abilitySettings.new SettingObject<Integer>(Pyromaniac.class, "revenge-boom-cooldown", 110,
			"# 리벤지 붐 쿨타임") {

		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}
		
		@Override
		public String toString() {
			return Formatter.formatCooldown(getValue());
		}

	};
	
	public static final SettingObject<Integer> CRIT_COUNT = 
			abilitySettings.new SettingObject<Integer>(Pyromaniac.class, "crit-count", 3,
			"# 근접 치명타 공격 횟수마다 폭발 발동") {

		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}

	};
	
	public static final SettingObject<Double> EXPLOSIVE = 
			abilitySettings.new SettingObject<Double>(Pyromaniac.class, "explosive", 1.6,
			"# 고정 피해 폭발력") {

		@Override
		public boolean condition(Double value) {
			return value >= 0;
		}

	};
	
	private final Cooldown tntcooldown = new Cooldown(TNT_CANNON_COOLDOWN.getValue()), revengecooldown = new Cooldown(REVENGE_BOOM_COOLDOWN.getValue());
	private final int revengeduration = (int) (REVENGE_BOOM_DURATION.getValue() * 20);
	private final double decrease = 1 - (REVENGE_BOOM_DECREASE.getValue() * 0.01);
	private final int critcount = CRIT_COUNT.getValue();
	private final double explosive = EXPLOSIVE.getValue();
	private int count = 0;
	private Map<UUID, Double> damageMap = new HashMap<>();
	private ActionbarChannel ac = newActionbarChannel();
	private final DecimalFormat df = new DecimalFormat();
	
    private boolean attackCooldown = false;
	
    @Override
    public void onUpdate(Update update) {
    	if (update == Update.RESTRICTION_CLEAR) {
    		attackCooldownChecker.start();
    	}
    }
    
	private final AbilityTimer attackCooldownChecker = new AbilityTimer() {
		
		@Override
		public void run(int count) {
			if (NMS.getAttackCooldown(getPlayer()) > 0.848 && attackCooldown) attackCooldown = false;
			else if (NMS.getAttackCooldown(getPlayer()) <= 0.848 && !attackCooldown) attackCooldown = true;
		}
		
	}.setPeriod(TimeUnit.TICKS, 1).register();
	
	private final AbilityTimer skill = new AbilityTimer(revengeduration) {
		
		@Override
		public void run(int count) {
			ac.update("§c리벤지 붐§7: §3" + df.format(count / 20.0) + "§f초");
		}
		
		@Override
		public void onEnd() {
			onSilentEnd();
		}
		
		@Override
		public void onSilentEnd() {
			ac.update(null);
			for (UUID uuid : damageMap.keySet()) {
				Player player = Bukkit.getPlayer(uuid);
				Damages.damageExplosion(player, getPlayer(), Double.valueOf(damageMap.get(uuid)).floatValue());
			}
			revengecooldown.start();
		}
		
	}.setPeriod(TimeUnit.TICKS, 1).register();
	
	@Override
	public boolean ActiveSkill(Material material, ClickType clicktype) {
		if (material.equals(Material.IRON_INGOT)) {
			if (clicktype.equals(ClickType.LEFT_CLICK)) {
				if (!tntcooldown.isCooldown()) {
					TNTPrimed tntprimed = getPlayer().getWorld().spawn(getPlayer().getEyeLocation(), TNTPrimed.class);
					tntprimed.setFuseTicks(30);
					final Vector direction = getPlayer().getLocation().getDirection().multiply(1.75);
					tntprimed.setVelocity(direction.setY(Math.min(0.45, direction.getY())));
					return tntcooldown.start();
				}
			} else {
				if (!revengecooldown.isCooldown() && !skill.isRunning()) {
					return skill.start();
				}
			}
		}
		return false;
	}
	
	@SuppressWarnings("deprecation")
	public static boolean isCriticalHit(Player p, boolean attackcool) {
		return (!p.isOnGround() && p.getFallDistance() > 0.0F && 
	      !p.getLocation().getBlock().isLiquid() &&
	      attackcool == false &&
	      !p.isInsideVehicle() && !p.isSprinting() && p
	      .getActivePotionEffects().stream().noneMatch(pe -> (pe.getType() == PotionEffectType.BLINDNESS)));
	}
	
	@SubscribeEvent
	public void onEntityDamage(EntityDamageEvent e) {
		if (e.getEntity().equals(getPlayer())) {
			if (e.getCause().equals(DamageCause.BLOCK_EXPLOSION) || e.getCause().equals(DamageCause.ENTITY_EXPLOSION)) {
				e.setCancelled(true);
				final EntityRegainHealthEvent event = new EntityRegainHealthEvent(getPlayer(), (e.getFinalDamage() / 3.0), RegainReason.CUSTOM);
				Bukkit.getPluginManager().callEvent(event);
				if (!event.isCancelled()) {
					Healths.setHealth(getPlayer(), getPlayer().getHealth() + event.getAmount());	
				}
			}	
		}
	}
	
	@SubscribeEvent
	public void onEntityDamageByBlock(EntityDamageByBlockEvent e) {
		onEntityDamage(e);
	}
	
	@SubscribeEvent
	public void onEntityDamageByEntity(EntityDamageByEntityEvent e) {
		onEntityDamage(e);
		
		Player damager = null;
		if (e.getDamager() instanceof Projectile) {
			Projectile projectile = (Projectile) e.getDamager();
			if (projectile.getShooter() instanceof Player) damager = (Player) projectile.getShooter();
		} else if (e.getDamager() instanceof Player) damager = (Player) e.getDamager();
		
		if (skill.isRunning()) {
			if (getPlayer().equals(damager)) e.setCancelled(true);
			else if (e.getEntity().equals(getPlayer()) && damager != null) {
				e.setDamage(e.getDamage() * decrease);
				damageMap.put(damager.getUniqueId(), damageMap.getOrDefault(damager.getUniqueId(), 0.0) + e.getFinalDamage());	
			}
		}
		
		if (getPlayer().equals(e.getDamager()) && isCriticalHit(getPlayer(), attackCooldown) && !e.isCancelled()) {
			count++;
			if (count >= critcount) {
				getPlayer().getWorld().createExplosion(getPlayer().getLocation().getX(), getPlayer().getLocation().getY(), getPlayer().getLocation().getZ(), (float) explosive, false, false);
				count = 0;
			}
		}
	}
	
}
