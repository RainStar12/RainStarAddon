package RainStarAbility;

import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

import daybreak.abilitywar.ability.AbilityBase;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.SubscribeEvent;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.config.ability.AbilitySettings.SettingObject;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.list.mix.Mix;

@AbilityManifest(name = "세줄이상안읽음", rank = Rank.S, species = Species.HUMAN, explain = {
		"적이 가진 능력 설명이 $[LENGTH]줄 이상이라면",
		"이후부터 한 줄당 $[INCREASE]%의 §c추가 피해§f를 입힙니다.",
		"§d믹스 능력자§f에서는 같은 칸의 능력만 적용됩니다."
		})
public class TLDR extends AbilityBase {
	
	public TLDR(Participant participant) {
		super(participant);
	}
	
	public static final SettingObject<Integer> LENGTH = 
			abilitySettings.new SettingObject<Integer>(TLDR.class, "length", 3,
			"# 추가 대미지를 입히는 기준") {

		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}

	};
	
	public static final SettingObject<Integer> INCREASE = 
			abilitySettings.new SettingObject<Integer>(TLDR.class, "increase", 10,
			"# 공격력 증가 수치", "# 단위: %") {

		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}

	};
	
	private final int increase = INCREASE.getValue();
	private final int maxlength = LENGTH.getValue();
	
    @SubscribeEvent
    public void onEntityDamageByEntity(EntityDamageByEntityEvent e) {
    	Player damager = null;
		if (e.getDamager() instanceof Projectile) {
			Projectile projectile = (Projectile) e.getDamager();
			if (projectile.getShooter() instanceof Player) damager = (Player) projectile.getShooter();
		} else if (e.getDamager() instanceof Player) damager = (Player) e.getDamager();
		
		if (damager.equals(getPlayer()) && e.getEntity() instanceof Player) {
			Player player = (Player) e.getEntity();
    		Participant p = getGame().getParticipant(player);
    		AbilityBase ab = p.getAbility();
    		if (ab.getClass().equals(Mix.class)) {
    			Mix mix = (Mix) ab;
    			Mix mymix = (Mix) getParticipant().getAbility();
    			if (mymix.getFirst().getClass().equals(TLDR.class)) {
    				if (mix.hasSynergy()) {
    					String[] description = mix.getSynergy().getManifest().explain();
    				} else {
        				AbilityBase first = mix.getFirst(), second = mix.getSecond();
        				String[] description1 = first.getManifest().explain();
        				String[] description2 = second.getManifest().explain();
        			}
    			}
    			
    		}
  
		}
    }

}
