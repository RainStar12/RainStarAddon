package rainstar.abilitywar.ability.mirage;

import static rainstar.abilitywar.system.config.RainStarAbilitySetting.rsAbilitySettings;

import java.text.DecimalFormat;

import org.bukkit.event.entity.EntityDamageByBlockEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;

import daybreak.abilitywar.ability.AbilityBase;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.ability.SubscribeEvent;
import daybreak.abilitywar.config.ability.AbilitySettings.SettingObject;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.AbstractGame.Participant.ActionbarNotification.ActionbarChannel;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.base.minecraft.entity.health.event.PlayerSetHealthEvent;
import daybreak.abilitywar.utils.base.random.Random;
import daybreak.abilitywar.utils.library.SoundLib;

@AbilityManifest(name = "신기루", rank = Rank.A, species = Species.HUMAN, explain = {
		"§c체력을 잃으면§f $[CHANCE]% 확률로 $[STEALTH]초간 §7은신 상태§f가 됩니다.",
		"발동에 실패하면 성공 전까지 확률이 +$[ADD_CHANCE]%씩 증가합니다."
})
public abstract class AbstractMirage extends AbilityBase {
	
	public AbstractMirage(Participant participant) {
		super(participant);
	}
	
	public static final SettingObject<Integer> CHANCE = 
			rsAbilitySettings.new SettingObject<Integer>(AbstractMirage.class, "chance", 15,
            "# 은신 확률", "# 단위: %") {
        @Override
        public boolean condition(Integer value) {
            return value >= 0;
        }
    };
    
	public static final SettingObject<Integer> ADD_CHANCE = 
			rsAbilitySettings.new SettingObject<Integer>(AbstractMirage.class, "add-chance", 15,
            "# 실패 시 확률 증가량", "# 단위: %") {
        @Override
        public boolean condition(Integer value) {
            return value >= 0;
        }
    };
    
	public static final SettingObject<Double> STEALTH = 
			rsAbilitySettings.new SettingObject<Double>(AbstractMirage.class, "stealth-duration", 4.0,
            "# 은신 지속시간", "# 단위: 초") {
        @Override
        public boolean condition(Double value) {
            return value >= 0;
        }
    };

    private final int defaultChance = CHANCE.getValue();
    private final int addChance = ADD_CHANCE.getValue();
    private int chance = defaultChance;
    private final int stealthDuration = (int) (STEALTH.getValue() * 20);
    private Random random = new Random();
    private ActionbarChannel ac = newActionbarChannel();
    private DecimalFormat df = new DecimalFormat("0.0");
    
	protected abstract void hide0();

	protected abstract void show0();

	private void hide() {
		this.hiding = true;
		getParticipant().attributes().TARGETABLE.setValue(false);
		hide0();
	}

	private void show() {
		this.hiding = false;
		getParticipant().attributes().TARGETABLE.setValue(true);
		show0();
	}
	
	private void tryHide() {
		if (random.nextInt(100) < chance) {
			chance = defaultChance;
			SoundLib.ENTITY_WITCH_THROW.playSound(getPlayer());
			if (!hidingTimer.isRunning()) hidingTimer.start();
			else hidingTimer.setCount(stealthDuration);
		} else {
			chance += addChance;
		}
	}

	private boolean hiding = false;

	public boolean isHiding() {
		return hiding;
	}
	
	@Override
	protected void onUpdate(Update update) {
		if (update == Update.RESTRICTION_SET || update == Update.ABILITY_DESTROY) {
			if (hiding) show();
		}
	}
	
	private final AbilityTimer hidingTimer = new AbilityTimer(stealthDuration) {
		
		@Override
		public void onStart() {
			hide();
			ac.update("§7은신§f: §b" + df.format(stealthDuration / 20.0) + "§f초");
		}
		
		@Override
		public void run(int count) {
			ac.update("§7은신§f: §b" + df.format(count / 20.0) + "§f초");
		}
		
		@Override
		public void onEnd() {
			onSilentEnd();
		}
		
		@Override
		public void onSilentEnd() {
			show();
			ac.update(null);
		}
		
	}.setPeriod(TimeUnit.TICKS, 1).register();
	
	@SubscribeEvent(ignoreCancelled = true)
	private void onSetHealth(PlayerSetHealthEvent e) {
		if (getPlayer().equals(e.getPlayer())) {
			if (getPlayer().getHealth() - e.getHealth() > 0) {
				tryHide();
			}
		}
	}
	
	@SubscribeEvent(priority = 1000, ignoreCancelled = true)
	private void onEntityDamage(EntityDamageEvent e) {
		if (getPlayer().equals(e.getEntity())) {
			if (e.getFinalDamage() > 0) {
				tryHide();
			}
		}
	}
	
	@SubscribeEvent(priority = 1000, ignoreCancelled = true)
	private void onEntityDamageByBlock(EntityDamageByBlockEvent e) {
		onEntityDamage(e);
	}
    
	@SubscribeEvent(priority = 1000, ignoreCancelled = true)
	private void onEntityDamageByEntity(EntityDamageByEntityEvent e) {
		onEntityDamage(e);
	}
    
}
