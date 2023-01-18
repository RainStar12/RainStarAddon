package rainstar.abilitywar.ability;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

import daybreak.abilitywar.ability.AbilityBase;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.config.ability.AbilitySettings.SettingObject;
import daybreak.abilitywar.config.enums.CooldownDecrease;
import daybreak.abilitywar.ability.SubscribeEvent;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.AbstractGame.Participant.ActionbarNotification.ActionbarChannel;
import daybreak.abilitywar.game.manager.effect.Bleed;
import daybreak.abilitywar.utils.base.Formatter;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.library.MaterialX;
import daybreak.abilitywar.utils.library.ParticleLib;
import daybreak.abilitywar.utils.library.SoundLib;
import daybreak.google.common.base.Strings;

@AbilityManifest(name = "지건", rank = Rank.A, species = Species.HUMAN, explain = {
		"3초간 맨 손으로 차징 후 적을 타격하면 $[DURATION]초간 §c§n출혈§f시킵니다. $[COOLDOWN]",
		"§c§n출혈§f 중인 적에게 입히는 피해량이 $[INCREASE]% 증가합니다.",
		"§b[§7아이디어 제공자§b] §chorn1111"
		},
		summarize = {
		"맨 손 상태를 일정 시간 유지하면 지건이 충전됩니다. 이때 근접 공격 시",
		"대상에게 §c출혈 피해§f를 입힙니다. §c출혈§f 중인 적에게 가하는 피해가 증가합니다."
		})
public class Fingun extends AbilityBase {

	public Fingun(Participant participant) {
		super(participant);
	}
	
	public static final SettingObject<Integer> COOLDOWN = 
			abilitySettings.new SettingObject<Integer>(Fingun.class, "cooldown", 50,
            "# 지건 쿨타임", "# 단위: 초") {
        @Override
        public boolean condition(Integer value) {
            return value >= 0;
        }
        @Override
        public String toString() {
            return Formatter.formatCooldown(getValue());
        }
    };
	
	public static final SettingObject<Double> DURATION = 
			abilitySettings.new SettingObject<Double>(Fingun.class, "bleed-duration", 6.5,
            "# 출혈 지속 시간") {
        @Override
        public boolean condition(Double value) {
            return value >= 0;
        }
    };
    
	
	public static final SettingObject<Integer> INCREASE = 
			abilitySettings.new SettingObject<Integer>(Fingun.class, "increase", 25,
            "# 출혈 대상에게 공격력 증가 수치", "# 단위: %") {
        @Override
        public boolean condition(Integer value) {
            return value >= 0;
        }
    };
    
    private Cooldown cooldown = new Cooldown(COOLDOWN.getValue(), CooldownDecrease._50);
    private final int increase = (int) (1 + (INCREASE.getValue() / (double) 100));
    private final double duration = DURATION.getValue();
    private ActionbarChannel ac = newActionbarChannel();
    private int chargestack = 0;
    
	protected void onUpdate(Update update) {
		if (update == Update.RESTRICTION_CLEAR) {
			charging.start();
		}
	}
    
    private final AbilityTimer charging = new AbilityTimer() {
    	
    	@Override
    	public void run(int count) {
    		if (!cooldown.isRunning()) {
        		if (getPlayer().getInventory().getItemInMainHand().getType().equals(Material.AIR)) {
        			chargestack = Math.min(chargestack + 1, 60);
        			if (chargestack == 19) SoundLib.ENTITY_ARROW_HIT_PLAYER.playSound(getPlayer(), 1, 1.05f);
        			else if (chargestack == 39) SoundLib.ENTITY_ARROW_HIT_PLAYER.playSound(getPlayer(), 1, 1.25f);
        			else if (chargestack == 59) SoundLib.ENTITY_ARROW_HIT_PLAYER.playSound(getPlayer(), 1, 1.45f);
        			if (chargestack == 60) ac.update("§c||||||||||");
        			else ac.update(Strings.repeat("§a|", (chargestack / 6)) + Strings.repeat("§7|", 10 - (chargestack / 6)));
        		} else {
        			chargestack = 0;
        			ac.update("§7||||||||||");
        		}	
    		}
    	}
    	
	}.setPeriod(TimeUnit.TICKS, 1).register();
	
	@SubscribeEvent
	public void onEntityDamageByEntity(EntityDamageByEntityEvent e) {
		Player damager = null;
		if (e.getDamager() instanceof Projectile) {
			Projectile projectile = (Projectile) e.getDamager();
			if (projectile.getShooter() instanceof Player) damager = (Player) projectile.getShooter();
		} else if (e.getDamager() instanceof Player) damager = (Player) e.getDamager();
		
		if (getPlayer().equals(damager) && chargestack >= 60 && e.getEntity() instanceof Player) {
			Player player = (Player) e.getEntity();
			chargestack = 0;
			ac.update("§7||||||||||");
			Bleed.apply(getGame().getParticipant(player), TimeUnit.TICKS, (int) (duration * 20));
			SoundLib.BLOCK_ANVIL_LAND.playSound(player.getLocation(), 1, 0.7f);
			cooldown.start();
		}
		
		if (getPlayer().equals(damager) && e.getEntity() instanceof Player) {
			Player player = (Player) e.getEntity();
			if (getGame().getParticipant(player).hasEffect(Bleed.registration)) {
				e.setDamage(e.getDamage() * increase);
				SoundLib.BLOCK_ANVIL_LAND.playSound(player.getLocation(), 1, 1.8f);
				ParticleLib.ITEM_CRACK.spawnParticle(player.getLocation(), 0.25, 0.5, 0.25, 150, 0.3, MaterialX.REDSTONE_BLOCK);
			}
		}
	}
	
}
