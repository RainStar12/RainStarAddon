package RainStarAbility;

import java.text.DecimalFormat;

import org.bukkit.GameMode;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

import daybreak.abilitywar.ability.AbilityBase;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.ability.SubscribeEvent;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.AbstractGame.Participant.ActionbarNotification.ActionbarChannel;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.base.minecraft.damage.Damages;
import daybreak.abilitywar.utils.base.minecraft.entity.health.Healths;

@AbilityManifest(name = "복수귀", rank = Rank.A, species = Species.UNDEAD, explain = {
		"살해당할 경우, $[WAIT]초간 유령 상태가 되어 돌아다닙니다.",
		"이후 §c복수귀§f가 되어 풀 체력으로 부활합니다. 체력은 $[DURATION]초에 걸쳐 빠르게 줄어듭니다.",
		"§c복수귀§f 모드간 자신을 죽인 사람하고만 피해를 주고받을 수 있습니다.",
		"이때 나를 죽이기 전까지 대상이 내게 줬던 피해량의 $[PERCENTAGE]%만큼 공격력이 증가합니다.",
		"대상을 내 손으로 처치할 경우, §c복수귀§f 모드가 종료됩니다."
		},
		summarize = {
		""
		})

public class RevengerR extends AbilityBase {
	
	public RevengerR(Participant participant) {
		super(participant);
	}
	
	private GameMode previousGameMode = GameMode.SURVIVAL;
	private Player killer = null;
	private boolean revenger = false;
	private ActionbarChannel ac = newActionbarChannel();
	private DecimalFormat df = new DecimalFormat("0.0");
	private int duration;
	
	public AbilityTimer ghost = new AbilityTimer() {
		
		@Override
		public void run(int count) {
			ac.update("§c부활까지§7: §f" + df.format(count / 20.0));
		}
		
		@Override
		public void onEnd() {
			onSilentEnd();
		}
		
		@Override
		public void onSilentEnd() {
			getPlayer().setGameMode(previousGameMode);
			revenger = true;
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
		if (getPlayer().getHealth() - e.getFinalDamage() <= 0 && getPlayer().getKiller() != null && !revenger) {
			killer = getPlayer().getKiller();
			e.setCancelled(true);
			previousGameMode = getPlayer().getGameMode() != GameMode.SPECTATOR ? getPlayer().getGameMode() : GameMode.SURVIVAL;
			getPlayer().setGameMode(GameMode.SPECTATOR);
			ghost.start();
		}
	}
	
	@SubscribeEvent
	public void onEntityDamageByEntity(EntityDamageByEntityEvent e) {
		
	}

}
