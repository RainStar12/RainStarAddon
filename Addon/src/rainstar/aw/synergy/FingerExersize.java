package rainstar.aw.synergy;

import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.ability.SubscribeEvent;
import daybreak.abilitywar.config.ability.AbilitySettings.SettingObject;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.AbstractGame.Participant.ActionbarNotification.ActionbarChannel;
import daybreak.abilitywar.game.list.mix.synergy.Synergy;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.library.SoundLib;
import org.bukkit.entity.Projectile;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

import java.util.HashSet;
import java.util.Set;

@AbilityManifest(name = "손가락 운동", rank = Rank.A, species = Species.HUMAN, explain = {
		"$[COUNT]번 웅크릴 때마다 $[DURATION]초간 유지되는 §c추가 공격력§f $[DAMAGE]을 얻습니다."
		})

public class FingerExersize extends Synergy {
	
	public FingerExersize(Participant participant) {
		super(participant);
	}
	
	public static final SettingObject<Integer> COUNT = 
			synergySettings.new SettingObject<Integer>(FingerExersize.class, "count", 7,
			"# 추가 공격력을 획득하기 위한 요구 웅크리기 횟수") {

		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}

	};
	
	public static final SettingObject<Integer> DURATION = 
			synergySettings.new SettingObject<Integer>(FingerExersize.class, "duration", 12,
			"# 추가 공격력의 지속 시간") {

		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}

	};
	
	public static final SettingObject<Integer> DAMAGE = 
			synergySettings.new SettingObject<Integer>(FingerExersize.class, "damage", 1,
			"# 조건 충족시 얻는 추가 공격력") {

		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}

	};
	
	private int needcount = COUNT.getValue();
	private int duration = DURATION.getValue();
	private int damage = DAMAGE.getValue();
	private Set<Damage> damages = new HashSet<>();
	private ActionbarChannel ac = newActionbarChannel();
	private int liftcount = 0;
	private boolean lift = false;
	
	protected void onUpdate(Update update) {
	    if (update == Update.RESTRICTION_CLEAR) {
	    	ac.update("§c추가 대미지§7: §e" + (damages.size() * damage));
	    	lifting.start();
	    }
	}
	
	@SubscribeEvent
	public void onEntityDamageByEntity(EntityDamageByEntityEvent e) {
		if (getPlayer().equals(e.getDamager())) {
			e.setDamage(e.getDamage() + (damages.size() * damage));
		}
		if (e.getDamager() instanceof Projectile) {
			Projectile projectile = (Projectile) e.getDamager();
			if (getPlayer().equals(projectile.getShooter()) && !getPlayer().equals(e.getEntity())) {
				e.setDamage(e.getDamage() + (damages.size() * damage));
			}
		}
	}
	
    private final AbilityTimer lifting = new AbilityTimer() {
    	
    	@Override
    	public void run(int count) {
    		if (getPlayer().isSneaking() && !lift) {
    			if (liftcount < needcount) {
    				liftcount++;
    			} else {
    				liftcount = 0;
    				new Damage().start();
    				ac.update("§c추가 대미지§7: §e" + (damages.size() * damage));
    				SoundLib.ENTITY_PLAYER_LEVELUP.playSound(getPlayer(), 1, 1.2f);
    			}
    			lift = true;
    		} else if (!getPlayer().isSneaking() && lift) {
    			lift = false;
    		}
    	}
    	
    }.setPeriod(TimeUnit.TICKS, 1).register();
	
	public class Damage extends AbilityTimer {
		
		public Damage() {
			super(TaskType.REVERSE, duration * 20);
			setPeriod(TimeUnit.TICKS, 1);
			damages.add(this);
		}
		
		@Override
		public void onEnd() {
			onSilentEnd();
		}
		
		@Override
		public void onSilentEnd() {
			damages.remove(this);
			ac.update("§c추가 대미지§7: §e" + (damages.size() * damage));
			SoundLib.ENTITY_ARROW_HIT.playSound(getPlayer(), 1, 0.5f);
		}
		
	}

}