package rainstar.abilitywar.synergy.heatabnormal;

import static rainstar.abilitywar.system.config.RainStarAbilitySetting.rsSynergySettings;

import java.util.Arrays;
import java.util.List;

import org.bukkit.Location;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByBlockEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.entity.EntityRegainHealthEvent.RegainReason;
import org.bukkit.event.player.PlayerInteractEvent;

import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.ability.SubscribeEvent;
import daybreak.abilitywar.config.ability.AbilitySettings.SettingObject;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.list.mix.synergy.Synergy;
import daybreak.abilitywar.game.manager.effect.Frost;
import daybreak.abilitywar.game.manager.effect.Frost.ParticipantFrost;
import daybreak.abilitywar.game.manager.effect.event.ParticipantPreEffectApplyEvent;
import daybreak.abilitywar.utils.base.minecraft.damage.Damages;
import daybreak.abilitywar.utils.base.minecraft.entity.health.Healths;
import daybreak.abilitywar.utils.base.minecraft.nms.NMS;
import daybreak.abilitywar.utils.library.MaterialX;
import daybreak.abilitywar.utils.library.ParticleLib;
import daybreak.abilitywar.utils.library.SoundLib;

@AbilityManifest(name = "열이상", rank = Rank.L, species = Species.OTHERS, explain = {
		"자신을 포함한 모든 대상은 §e불§6타§c오§4르§0기§f 시작합니다.",
		"§c화염계 피해§f에 면역을 가지고, 그 절반만큼 §d역회복§f합니다.",
		"모두와 공유하는 §c열기§f 수치가 존재하며, §c5%§f씩 모든 §c화염계 피해§f를 증가시킵니다.",
		"§c열기§f는 적을 공격할 때마다 증가하고, 증가 시 $[STOP]초간 감소하지 않습니다.",
		"§c열기§f의 최대치는 §c20§f이며, 매 3초마다 §c1§f씩 감소합니다.",
		"만일 §c열기§f가 최대치에 도달한다면, 모든 대상은 $[FROST_DURATION]초간 §7§n얼§b§n어§3§n붙§9§n습§1§n니§0§n다§f.",
		"이후 §c열기§f는 §c0§f이 되고 $[COOLDOWN]초간 상승하지 않지만, 대신 쿨타임을 0.5초 줄입니다.",
		"자신은 §b§n빙결§f을 무시하고 §b§n빙결§f된 대상을 공격해 깨뜨려,",
		"남은 지속시간의 §b$[DAMAGE_BREAK]%§f만큼 §b트루 대미지§f를 입힙니다."
		})
public class HeatAbnormal extends Synergy {
	
	public HeatAbnormal(Participant participant) {
		super(participant);
	}
	
	public static final SettingObject<Double> STOP = rsSynergySettings.new SettingObject<Double>(HeatAbnormal.class,
			"decrease-stop", 5.0, "# 열기 감소 정지 시간", "# 단위: 초") {
		@Override
		public boolean condition(Double value) {
			return value >= 0;
		}
	};
	
	public static final SettingObject<Double> FROST_DURATION = rsSynergySettings.new SettingObject<Double>(HeatAbnormal.class,
			"frost-duration", 10.0, "# 빙결 시간", "# 단위: 초") {
		@Override
		public boolean condition(Double value) {
			return value >= 0;
		}
	};
	
	public static final SettingObject<Double> COOLDOWN = rsSynergySettings.new SettingObject<Double>(HeatAbnormal.class,
			"increase-cooldown", 60.0, "# 열기 상승 쿨타임", "# 단위: 초", "# 주의! 쿨타임 감소와 연관이 없습니다.") {
		@Override
		public boolean condition(Double value) {
			return value >= 0;
		}
	};
	
	public static final SettingObject<Double> COOLDOWN_DECREASE = rsSynergySettings.new SettingObject<Double>(HeatAbnormal.class,
			"decrease-cooldown", 0.5, "# 쿨타임 중 열기 상승 시도 시 감소 쿨타임", "# 단위: 초") {
		@Override
		public boolean condition(Double value) {
			return value >= 0;
		}
	};
	
	public static final SettingObject<Integer> DAMAGE_BREAK = rsSynergySettings.new SettingObject<Integer>(HeatAbnormal.class,
			"damage-break", 150, "# 남은 빙결 시간 초당 관통 피해 비례량", "# 단위: %") {
		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}
	};

	private final int stop = (int) (STOP.getValue() * 20);
	private final int frostDuration = (int) (FROST_DURATION.getValue() * 20);
	private final int cooldown = (int) (COOLDOWN.getValue() * 20);
	private final int decreaseCooldown = (int) (COOLDOWN_DECREASE.getValue() * 20);
	private final double damageBreak = DAMAGE_BREAK.getValue() * 0.01;
	
	private static Heat heat = null;
    private static int holders = 0;
    
	private static final List<DamageCause> DAMAGE_CAUSES = Arrays.asList(
			DamageCause.FIRE,
			DamageCause.FIRE_TICK,
			DamageCause.LAVA,
			DamageCause.HOT_FLOOR
		);
	
	@Override
	protected void onUpdate(Update update) {
		if (update == Update.RESTRICTION_CLEAR) {
			if (holders++ == 0) {
				heat = new Heat(getGame());
				heat.register();
			}
		}
	
		if (update == Update.ABILITY_DESTROY) {
			if (--holders <= 0) {
				holders = 0;
			
				if (heat != null) {
					heat.unregister();
					heat = null;
				}
			}
		}
	}
	
	@SubscribeEvent(priority = 100, onlyRelevant = true)
	private void onEntityDamage(EntityDamageEvent e) {
		if (DAMAGE_CAUSES.contains(e.getCause())) {
			EntityRegainHealthEvent event = new EntityRegainHealthEvent(getPlayer(), e.getFinalDamage() * 0.5, RegainReason.CUSTOM);
    		if (!event.isCancelled()) {
    			Healths.setHealth(getPlayer(), Math.min(getPlayer().getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue(), getPlayer().getHealth() + event.getAmount()));
    		}
			e.setDamage(0);
		}
	}
	
	@SubscribeEvent(priority = 100, onlyRelevant = true)
	private void onEntityDamageByBlock(EntityDamageByBlockEvent e) {
		onEntityDamage(e);
	}
	
	@SubscribeEvent(priority = 100)
	private void onEntityDamageByEntity(EntityDamageByEntityEvent e) {
		if (e.getEntity().equals(getPlayer())) onEntityDamage(e);
		
    	Player damager = null;
		if (e.getDamager() instanceof Projectile) {
			Projectile projectile = (Projectile) e.getDamager();
			if (projectile.getShooter() instanceof Player) damager = (Player) projectile.getShooter();
		} else if (e.getDamager() instanceof Player) damager = (Player) e.getDamager();
		
		if (getPlayer().equals(damager) && !e.isCancelled() && e.getEntity() instanceof Player && ((Player) e.getEntity()).getNoDamageTicks() <= 10) {
			if (heat.heatUp(stop, frostDuration, cooldown, decreaseCooldown)) {
				SoundLib.ENTITY_GENERIC_EXTINGUISH_FIRE.playSound(getPlayer().getLocation(), 1, (float) (0.5 + (heat.getHeat() * 0.075)));
			}
		}
	}
	
	@SubscribeEvent(onlyRelevant = true)
	private void onInteractBlock(PlayerInteractEvent e) {
	    if (e.getAction() == Action.LEFT_CLICK_BLOCK && e.getClickedBlock() != null) {
	        final ParticipantFrost frost = ParticipantFrost.getFrost(e.getClickedBlock().getLocation());
	        if (frost != null && frost.getParticipant() != getParticipant()) {
	            final Player target = frost.getParticipant().getPlayer();
	            frost.noDamage = false;
	            int count = frost.getCount();
	            if (Damages.canDamage(target, getPlayer(), DamageCause.CUSTOM, count * 0.05 * damageBreak)) {
	                frost.setCount(0);
	                final Location loc = target.getLocation();
	                ParticleLib.BLOCK_CRACK.spawnParticle(loc, 1.5, 1.5, 1.5, 20, MaterialX.ICE);
	                SoundLib.BLOCK_GLASS_BREAK.playSound(loc, 1f, 0);
	                SoundLib.BLOCK_GLASS_BREAK.playSound(loc, 1f, 1);
	                SoundLib.BLOCK_GLASS_BREAK.playSound(loc, 1f, 2);
	                target.setHealth(Math.max(0, target.getHealth() - (count * 0.05 * damageBreak)));
	                NMS.broadcastEntityEffect(target, (byte) 2);
	            }
	            frost.noDamage = true;
	        }
	    }
	}

    @SubscribeEvent(onlyRelevant = true)
    private void onEffectApply(ParticipantPreEffectApplyEvent e) {
        if (e.getEffectType().equals(Frost.registration)) {
            e.setCancelled(true);
        }
    }

}
