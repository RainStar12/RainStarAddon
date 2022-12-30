package RainStarAbility;

import java.text.DecimalFormat;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.potion.PotionEffectType;

import daybreak.abilitywar.ability.AbilityBase;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.SubscribeEvent;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.ability.decorator.ActiveHandler;
import daybreak.abilitywar.config.ability.AbilitySettings.SettingObject;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.AbstractGame.Participant.ActionbarNotification.ActionbarChannel;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.library.ParticleLib;
import daybreak.abilitywar.utils.library.PotionEffects;
import daybreak.abilitywar.utils.library.SoundLib;


@AbilityManifest(name = "루시페륨", rank = Rank.S, species = Species.OTHERS, explain = {
		"웅크린 채 철괴 좌클릭으로 약을 복용할 수 있습니다.",
		"복용 시 영구적인 §6힘 $[STRENGTH]§f, §b신속 $[SPEED]§f, §8저항 $[RESISTANCE]§f 버프를 획득합니다.",
		"그러나 복용 이후에는 $[MAX_NOT_TAKE]초 내로 약을 다시 복용해야만 합니다. §8(§7실패 시 §c사망§8)",
		"복용까지 걸린 시간으로 다음 복용 시간이 줄어듭니다.",
		"적 처치 시 약의 지속시간이 $[MAX_NOT_TAKE]초로 갱신됩니다."
		},
		summarize = {
		"§7웅크린 채 철괴 좌클릭§f시 약을 먹어 §6힘§7 / §b신속§7 / §8저항§f 버프를 얻습니다.",
		"약 복용 후 일정 시간 재복용하지 않으면 §4즉사§f합니다.",
		"복용까지 걸린 시간으로 다음 복용 시간이 줄어들고, §c적 처치§f로 지속시간을 리셋합니다."
		})

public class Luciferium extends AbilityBase implements ActiveHandler {
	
	public Luciferium(Participant participant) {
		super(participant);
	}
	
	public static final SettingObject<Integer> STRENGTH = 
			abilitySettings.new SettingObject<Integer>(Luciferium.class, "strength-amplifier", 0,
            "# 힘 포션 효과 계수", "# 주의! 0부터 시작합니다.", "# 0일 때 포션 효과 계수는 1레벨,", "# 1일 때 포션 효과 계수는 2레벨입니다.") {
        @Override
        public boolean condition(Integer value) {
            return value >= 0;
        }
		@Override
		public String toString() {
			return "" + (1 + getValue());
        }
    };
    
	public static final SettingObject<Integer> SPEED = 
			abilitySettings.new SettingObject<Integer>(Luciferium.class, "speed-amplifier", 0,
            "# 신속 포션 효과 계수", "# 주의! 0부터 시작합니다.", "# 0일 때 포션 효과 계수는 1레벨,", "# 1일 때 포션 효과 계수는 2레벨입니다.") {
        @Override
        public boolean condition(Integer value) {
            return value >= 0;
        }
		@Override
		public String toString() {
			return "" + (1 + getValue());
        }
    };
    
	public static final SettingObject<Integer> RESISTANCE = 
			abilitySettings.new SettingObject<Integer>(Luciferium.class, "resistance-amplifier", 0,
            "# 저항 포션 효과 계수", "# 주의! 0부터 시작합니다.", "# 0일 때 포션 효과 계수는 1레벨,", "# 1일 때 포션 효과 계수는 2레벨입니다.") {
        @Override
        public boolean condition(Integer value) {
            return value >= 0;
        }
		@Override
		public String toString() {
			return "" + (1 + getValue());
        }
    };
    
	public static final SettingObject<Integer> MAX_NOT_TAKE = 
			abilitySettings.new SettingObject<Integer>(Luciferium.class, "max-not-take", 66,
            "# 다음 복용까지 주어지는 최대 유효 기간") {
        @Override
        public boolean condition(Integer value) {
            return value >= 0;
        }
    };
    
    private ActionbarChannel ac = newActionbarChannel();
    private DecimalFormat df = new DecimalFormat("0.0");
    private int strength = STRENGTH.getValue();
    private int speed = SPEED.getValue();
    private int resistance = RESISTANCE.getValue();
    private int unknown = (int) ((MAX_NOT_TAKE.getValue() * 20) / 6.0);
    
    public AbilityTimer nextpill = new AbilityTimer(MAX_NOT_TAKE.getValue() * 20) {
    	
    	@Override
    	public void run(int count) {
    		PotionEffects.INCREASE_DAMAGE.addPotionEffect(getPlayer(), 999999, strength, true);
    		PotionEffects.SPEED.addPotionEffect(getPlayer(), 999999, speed, true);
    		PotionEffects.DAMAGE_RESISTANCE.addPotionEffect(getPlayer(), 999999, resistance, true);
    		if (count > unknown) ac.update("§3남은 시간§f: " + (count <= 200 ? "§c" : "§e") + df.format(count / 20.0));
    		else ac.update("§3남은 시간§f: §c§k???");
    	}
    	
    	@Override
    	public void onEnd() {
    		onSilentEnd();
    		getPlayer().setHealth(0);
    		Bukkit.broadcastMessage("§4[§c!§4] §e" + getPlayer().getName() + "§f님이 §5악마의 유혹§f에 잠식되고 말았습니다.");
    	}
    	
    	@Override
    	public void onSilentEnd() {
    		ac.update(null);
    		getPlayer().removePotionEffect(PotionEffectType.INCREASE_DAMAGE);
    		getPlayer().removePotionEffect(PotionEffectType.SPEED);
    		getPlayer().removePotionEffect(PotionEffectType.DAMAGE_RESISTANCE);
    	}
    	
	}.setPeriod(TimeUnit.TICKS, 1).register();
	
	public AbilityTimer takecount = new AbilityTimer() {
	}.setPeriod(TimeUnit.TICKS, 1).register();
	
	@SubscribeEvent
	public void onPlayerDeath(PlayerDeathEvent e) {
		if (getPlayer().equals(e.getEntity().getKiller()) && nextpill.isRunning()) {
			SoundLib.ENTITY_ZOMBIE_VILLAGER_CURE.playSound(getPlayer().getLocation(), 1, 0.75f);
			ParticleLib.SMOKE_LARGE.spawnParticle(getPlayer().getLocation(), 0.25, 0, 0.25, 50, 1);
			takecount.setCount(0);
			nextpill.setCount(MAX_NOT_TAKE.getValue() * 20);
			unknown = (int) ((MAX_NOT_TAKE.getValue() * 20) / 6.0);
		}
	}
	
	public boolean ActiveSkill(Material material, ClickType clicktype) {
		if (material == Material.IRON_INGOT && clicktype == ClickType.LEFT_CLICK && getPlayer().isSneaking()) {
			if (!nextpill.isRunning()) {
				SoundLib.ENTITY_GENERIC_DRINK.playSound(getPlayer().getLocation(), 1, 0.5f);
				SoundLib.ENTITY_GENERIC_DRINK.playSound(getPlayer().getLocation(), 1, 0.75f);
				SoundLib.ENTITY_GENERIC_DRINK.playSound(getPlayer().getLocation(), 1, 1);
				SoundLib.ENTITY_GENERIC_DRINK.playSound(getPlayer().getLocation(), 1, 1.5f);
				SoundLib.ENTITY_GENERIC_DRINK.playSound(getPlayer().getLocation(), 1, 2);
				SoundLib.ENTITY_VEX_CHARGE.playSound(getPlayer(), 1, 0.65f);
				nextpill.start();
				takecount.start();
				return true;
			} else {
				SoundLib.ENTITY_GENERIC_DRINK.playSound(getPlayer().getLocation(), 1, 0.5f);
				SoundLib.ENTITY_GENERIC_DRINK.playSound(getPlayer().getLocation(), 1, 0.75f);
				SoundLib.ENTITY_GENERIC_DRINK.playSound(getPlayer().getLocation(), 1, 1);
				SoundLib.ENTITY_GENERIC_DRINK.playSound(getPlayer().getLocation(), 1, 1.5f);
				SoundLib.ENTITY_GENERIC_DRINK.playSound(getPlayer().getLocation(), 1, 2);
				SoundLib.ENTITY_VEX_CHARGE.playSound(getPlayer(), 1, 0.65f);
				nextpill.setCount(takecount.getCount());
				unknown = (int) (takecount.getCount() / 6.0);
				takecount.setCount(0);
				return true;
			}
		}
		return false;
	}
	

}
