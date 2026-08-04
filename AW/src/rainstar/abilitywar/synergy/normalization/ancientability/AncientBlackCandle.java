package rainstar.abilitywar.synergy.normalization.ancientability;

import static rainstar.abilitywar.system.config.RainStarAbilitySetting.rsAbilitySettings;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByBlockEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent.RegainReason;

import daybreak.abilitywar.ability.AbilityBase;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.ability.SubscribeEvent;
import daybreak.abilitywar.config.ability.AbilitySettings.SettingObject;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.base.minecraft.entity.health.Healths;
import daybreak.abilitywar.utils.base.random.Random;
import daybreak.abilitywar.utils.library.PotionEffects;

@AbilityManifest(name = "고대의 검은 양초", rank = Rank.SPECIAL, species = Species.OTHERS, explain = {
		"디버프를 받지 않으며, 대미지를 받으면 $[CHANCE_CONFIG]% 확률로 체력 $[HEAL]을 회복합니다."
		})
public class AncientBlackCandle extends AbilityBase {
	
	public AncientBlackCandle(Participant participant) {
		super(participant);
	}
	
	public static final SettingObject<Integer> CHANCE_CONFIG = rsAbilitySettings.new SettingObject<Integer>(AncientBlackCandle.class,
			"chance", 35, "# 대미지를 받았을 때 체력을 회복할 확률") {

        @Override
        public boolean condition(Integer value) {
            return value >= 0 && value <= 100;
        }

    };
    
	public static final SettingObject<Double> HEAL = rsAbilitySettings.new SettingObject<Double>(AncientBlackCandle.class,
			"heal-amount", 3.0, "# 체력 회복량") {

        @Override
        public boolean condition(Double value) {
            return value >= 0;
        }

    };
	
	private static final Random random = new Random();
	private static final PotionEffects[] NEGATIVE_EFFECTS = {
			PotionEffects.BLINDNESS,
			PotionEffects.CONFUSION,
			PotionEffects.GLOWING,
			PotionEffects.HARM,
			PotionEffects.HUNGER,
			PotionEffects.POISON,
			PotionEffects.SLOW,
			PotionEffects.SLOW_DIGGING,
			PotionEffects.UNLUCK,
			PotionEffects.WEAKNESS,
			PotionEffects.WITHER
	};
	
	private final int chance = CHANCE_CONFIG.getValue();
	private final double heal = HEAL.getValue();
	
	private final AbilityTimer noDebuff = new AbilityTimer() {
	    @Override
	    public void run(int count) {
	        final Player player = getPlayer();
	        for (PotionEffects effect : NEGATIVE_EFFECTS) {
	            effect.removePotionEffect(player);
	        }
	    }
	}.setPeriod(TimeUnit.TICKS, 1).register();

	@Override
	protected void onUpdate(Update update) {
		if (update == Update.RESTRICTION_CLEAR) {
			noDebuff.start();
		}
	}

	@SubscribeEvent(onlyRelevant = true)
	public void onEntityDamage(EntityDamageEvent e) {
	    if (e.getEntity().equals(getPlayer()) && random.nextInt(100) < chance) {
            final EntityRegainHealthEvent event = new EntityRegainHealthEvent(getPlayer(), heal, RegainReason.CUSTOM);
            Bukkit.getPluginManager().callEvent(event);
            if (!event.isCancelled()) {
            	Healths.setHealth(getPlayer(), getPlayer().getHealth() + event.getAmount());
            }
	    }
	}
    
    @SubscribeEvent(onlyRelevant = true)
    public void onEntityDamageByBlock(EntityDamageByBlockEvent e) {
    	onEntityDamage(e);
    }
    
    @SubscribeEvent(onlyRelevant = true)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent e) {
    	onEntityDamage(e);
    }

}
