package rainstar.abilitywar.game.murder.murderer;

import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.inventory.PlayerInventory;

import daybreak.abilitywar.ability.AbilityBase;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.SubscribeEvent;
import daybreak.abilitywar.ability.AbilityFactory.AbilityRegistration;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.game.AbstractGame.GameTimer;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.AbstractGame.Participant.ActionbarNotification.ActionbarChannel;
import daybreak.abilitywar.game.list.murdermystery.Items;
import daybreak.abilitywar.game.list.murdermystery.ability.AbstractMurderer;
import daybreak.abilitywar.utils.base.collect.SetUnion;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.base.minecraft.nms.NMS;

@AbilityManifest(name = "머더: 광신도", rank = Rank.SPECIAL, species = Species.HUMAN, explain = {
		"교주를 도와 모든 시민을 죽이세요!",
		"§8[§a천직§8]§f 광신도가 되기 이전 직업도 여전히 사용 가능합니다.",
		"§8[§c피폐§8]§f 한 번 검을 잡으면 10초간 검을 놓을 수 없습니다.",
		"검을 들 때 이동 속도 증가를 받지 않습니다.",
		"검을 들고 있는 동안은 탭리스트에 이름이 붉은 색으로 뜹니다.",
		"§8[§f일심§8]§f 교주가 사망할 때 본인도 같이 사망합니다."
})
public class FanaticMurderer extends AbstractMurderer {

	public FanaticMurderer(Participant participant) {
		super(participant);
	}
	
	private AbilityBase ability;
	private ActionbarChannel ac = newActionbarChannel();
	
	public AbilityBase getSubAbility() {
		return ability;
	}
	
	@Override
	public Set<GameTimer> getTimers() {
		return ability != null ? SetUnion.union(super.getTimers(), ability.getTimers()) : super.getTimers();
	}

	@Override
	public Set<GameTimer> getRunningTimers() {
		return ability != null ? SetUnion.union(super.getRunningTimers(), ability.getRunningTimers()) : super.getRunningTimers();
	}

	@Override
	protected void onUpdate(Update update) {
		if (update == Update.RESTRICTION_CLEAR) {
			if (ability != null) {
				ability.setRestricted(false);
			}
			NMS.sendTitle(getPlayer(), "§e직업§f: §5광신도", "§5교주§f를 도와 모든 §a시민§f을 죽이세요.", 10, 80, 10);
			new AbilityTimer(1) {
				@Override
				protected void onEnd() {
					NMS.clearTitle(getPlayer());
				}
			}.setInitialDelay(TimeUnit.SECONDS, 5).start();
			PASSIVE.stop(false);
			passive.start();
		} else if (update == Update.RESTRICTION_SET) {
			if (ability != null) {
				ability.setRestricted(true);
			}
		} else if (update == Update.ABILITY_DESTROY) {
			if (ability != null) {
				ability.destroy();
				ability = null;
			}
			Bukkit.broadcastMessage("§5§o광신도 §e§o" + getPlayer().getName() + "§5§o님을 잡아냈습니다!");
		}
	}
	
	public void setInsideAbility(AbilityRegistration registration) {
		try {
			this.ability = AbilityBase.create(registration, getParticipant());
			this.ability.setRestricted(false);
			new AbilityTimer(1) {
				@Override
				protected void onEnd() {
					final PlayerInventory inventory = getPlayer().getInventory();
					getPlayer().getInventory().setHeldItemSlot(0);
					inventory.setItem(1, Items.MURDERER_SWORD.getStack());
					NMS.sendTitle(getPlayer(), "§e직업§f: §5광신도", "§5교주§f를 도와 모든 §a시민§f을 죽이세요.", 10, 80, 10);
				}
			}.setInitialDelay(TimeUnit.TICKS, 30).start();
			new AbilityTimer(1) {
				@Override
				protected void onEnd() {
					NMS.clearTitle(getPlayer());
				}
			}.setInitialDelay(TimeUnit.SECONDS, 5).start();
		} catch (ReflectiveOperationException e) {
			e.printStackTrace();
		}
	}
	
	@SubscribeEvent
	public void onSlotChange(PlayerItemHeldEvent e) {
		if (notchange.isRunning() && e.getPlayer().equals(getPlayer()) && e.getPreviousSlot() == 1) {
			e.setCancelled(true);
			e.getPlayer().getInventory().setHeldItemSlot(e.getPreviousSlot());
		}
		if (!notchange.isRunning() && e.getPlayer().equals(getPlayer()) && e.getNewSlot() == 1) {
			notchange.start();
		}
	}
	
	private final AbilityTimer passive = new AbilityTimer() {
		
    	@Override
		public void run(int count) {
    		if (getPlayer().getInventory().getItemInMainHand().isSimilar(Items.MURDERER_SWORD.getStack())) getPlayer().setPlayerListName("§c" + getPlayer().getName());
    		else getPlayer().setPlayerListName(getPlayer().getName());
    	}
    	
    	@Override
    	public void onEnd() {
    		onSilentEnd();
    	}
    	
    	@Override
    	public void onSilentEnd() {
    		getPlayer().setPlayerListName(getPlayer().getName());
    	}
    	
	}.setPeriod(TimeUnit.TICKS, 1).register();
	
	private final AbilityTimer notchange = new AbilityTimer(10) {
		
		@Override
		public void onStart() {
			ac.update("§c교체 불가§7: §c10§f초");
		}
		
    	@Override
		public void run(int count) {
    		ac.update("§c교체 불가§7: §c" + count + "§f초");
    	}
    	
    	@Override
    	public void onEnd() {
    		onSilentEnd();
    	}
    	
    	@Override
    	public void onSilentEnd() {
    		ac.update(null);
    	}
    	
	}.setPeriod(TimeUnit.SECONDS, 1).register();

}
