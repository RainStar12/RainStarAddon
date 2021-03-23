package RainStarAbility;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.entity.EntityDamageByBlockEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.entity.EntityRegainHealthEvent.RegainReason;

import daybreak.abilitywar.ability.AbilityBase;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.SubscribeEvent;
import daybreak.abilitywar.ability.AbilityBase.AbilityTimer;
import daybreak.abilitywar.ability.AbilityBase.Update;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.AbstractGame.Participant.ActionbarNotification.ActionbarChannel;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.base.math.LocationUtil;
import daybreak.abilitywar.utils.library.ParticleLib;

@AbilityManifest(name = "인페르노", rank = Rank.S, species = Species.DEMIGOD, explain = {
		"§c불 속성§f의 군주 마검사, 인페르노.",
		"§7패시브 §8- §c업화의 주인§f: 화염이 붙은 적을 타격할 때마다 §c작열하는 불꽃§f을",
		" 대상의 발화 초만큼 획득합니다. §c불꽃§f은 총 10개까지 한 번에 소지할 수 있습니다.",
		" 모든 화염 피해는 §c불꽃§f으로 대체되어 자가 발화가 진행됩니다.",
		" 매 초마다 §c불꽃§f 하나가 소비되며, §4불꽃§f당 자신의 화염 피해가 10% 상승합니다.",
		"§7검 공격 §8- §c열화폭참§f: 자신이 타격한 대상을 1초간 추가 발화시킵니다.",
		" 대상이 이미 2초 이상 발화 도중이면 대신 대상에게 추가 피해를 입힙니다.",
		" §7추가 피해량§f: §e(대상이 가진 화염 지속시간 * 0.2) + (§c불꽃§e * 0.1)",
		"§7검 우클릭 §8- §c화력전개§f: 나와 $[RANGE]칸 이내의 모든 플레이어를",
		" $[DURATION]초간 추가 발화시키고, 나를 제외한 모든 대상에게 §c화상§f 상태이상을 겁니다.",
		" 발화 도중이 아니던 대상에게는 효과가 없습니다. $[COOLDOWN]",
		"§7상태이상 §8- §c화상§f: 모든 화염 계열 피해를 2배로 입습니다.",
		" 화염이 꺼질 때 꺼지기 전의 화염 지속시간에 비례해 피해를 입습니다."
		})

public class Inferno extends AbilityBase {

	public Inferno(Participant participant) {
		super(participant);
	}
	
	private int burningflame = 0;
	private final ActionbarChannel ac = newActionbarChannel();
	
	protected void onUpdate(Update update) {
	    if (update == Update.RESTRICTION_CLEAR) {
	    	passive.start();
	    } 
	}
	
    private final AbilityTimer passive = new AbilityTimer() {
    	
    	@Override
		public void run(int count) {
    		if (getPlayer().getFireTicks() >= 0) {
    			if (burningflame <= 0) {
    				getPlayer().setFireTicks(0);
    			}
    		}
    		if (getPlayer().getFireTicks() % 20 == 0) {
    			if (getPlayer().getFireTicks() != burningflame * 20) {
    				getPlayer().setFireTicks(burningflame * 20);
    			}
    		}
    		if (count % 20 == 0) {
        		flameSet(-1);
    		}
    	}
    	
    }.setPeriod(TimeUnit.TICKS, 1).register();
    
	public void flameSet(int value) {
		if (value <= 0) {
			burningflame = Math.max(0, burningflame + value);
		} else {
			burningflame = Math.min(10, burningflame + value);
		}
		ac.update("§c♨ §e" + burningflame);
	}
	
    @SubscribeEvent
    public void onEntityDamage(EntityDamageEvent e) {
    	if (e.getEntity().equals(getPlayer())) {
    		if (e.getCause() == DamageCause.FIRE || e.getCause() == DamageCause.FIRE_TICK ||
					e.getCause() == DamageCause.LAVA || e.getCause() == DamageCause.HOT_FLOOR) {
    			if (burningflame >= 1) {
    				e.setDamage(e.getDamage() + (e.getDamage() * (burningflame * 0.1)));
    			} else {
    				e.setCancelled(true);
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
    	if (e.getDamager().equals(getPlayer())) {
    		if (e.getEntity().getFireTicks() >= 40) {
    			e.setDamage(e.getDamage() + (e.getEntity().getFireTicks() * 0.01) + (burningflame * 0.1));
    		} else {
    			e.getEntity().setFireTicks(e.getEntity().getFireTicks() + 20);
    		}
    		if (e.getEntity().getFireTicks() >= 0) {
    			flameSet((int) (e.getEntity().getFireTicks() * 0.05));
    		}
    		Bukkit.broadcastMessage("§c딜량§7: §f" + e.getFinalDamage());
    	}
    }
    
}
