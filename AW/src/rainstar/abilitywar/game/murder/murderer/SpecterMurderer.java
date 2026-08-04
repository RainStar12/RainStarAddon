package rainstar.abilitywar.game.murder.murderer;

import org.bukkit.event.EventPriority;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.PlayerInventory;

import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.SubscribeEvent;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.list.murdermystery.Items;
import daybreak.abilitywar.game.list.murdermystery.MurderMystery;
import daybreak.abilitywar.game.list.murdermystery.MurderMystery.ArrowKillEvent;
import daybreak.abilitywar.game.list.murdermystery.ability.AbstractDetective;
import daybreak.abilitywar.game.list.murdermystery.ability.AbstractInnocent;
import daybreak.abilitywar.game.list.murdermystery.ability.AbstractMurderer;
import daybreak.abilitywar.game.list.murdermystery.ability.Murderer;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.base.minecraft.nms.NMS;

@AbilityManifest(name = "머더: 망령", rank = Rank.SPECIAL, species = Species.UNDEAD, explain = {
		"모든 시민을 죽이세요!",
		"금 우클릭으로 금 8개를 소모해 활과 화살을 얻을 수 있습니다.",
		"§8[§c망자의 혼§8]§f 사망 시, 남은 시민팀이 1명이 아니라면 발동합니다.",
		"자신을 죽인 대상을 일반 §c머더§f로 만든 뒤, §e금§f과 §7화살§f을 전부 넘겨줍니다.",
		"§8[§9불온한 속삭임§8]§f 자신의 유령 채팅은, §8[§c망자의 혼§8]§f으로 만들어 낸",
		"§c머더§f에게도 보입니다."
})
public class SpecterMurderer extends AbstractMurderer {
	
	public SpecterMurderer(Participant participant) {
		super(participant);
	}

	@Override
	protected void onUpdate(Update update) {
		super.onUpdate(update);
		if (update == Update.RESTRICTION_CLEAR) {
			NMS.sendTitle(getPlayer(), "§e직업§f: §b망령", "§f자신을 죽인 대상을 §c머더§f로 타락시키세요.", 10, 80, 10);
			new AbilityTimer(1) {
				@Override
				protected void onEnd() {
					NMS.clearTitle(getPlayer());
				}
			}.setInitialDelay(TimeUnit.SECONDS, 5).start();
			final MurderMystery murderMystery = (MurderMystery) getGame();
			final PlayerInventory inventory = getPlayer().getInventory();
			final int arrowCount = getArrowCount();
			inventory.clear();
			murderMystery.updateGold(getParticipant());
			addArrow(arrowCount, false);
			if (!hasBow()) {
				getPlayer().getInventory().setItem(2, Items.NORMAL_BOW.getStack());
			}
		} else if (update == Update.ABILITY_DESTROY) {
			NMS.setInvisible(getPlayer(), false);
		}
	}
	
	private Participant target = null;
	
	@SubscribeEvent(onlyRelevant = true)
	private void onInteract(PlayerInteractEvent e) {
		if (Items.isGold(e.getItem())) {
			if (e.getAction() == Action.RIGHT_CLICK_AIR || e.getAction() == Action.RIGHT_CLICK_BLOCK) {
				final MurderMystery murderMystery = (MurderMystery) getGame();
				if (murderMystery.consumeGold(getParticipant(), 8)) {
					if (!addArrow()) {
						murderMystery.addGold(getParticipant());
					} else {
						if (!hasBow()) {
							getPlayer().getInventory().setItem(2, Items.NORMAL_BOW.getStack());
						}
					}
				}
			}
		}
	}
	
	@SubscribeEvent(priority = 4, eventPriority = EventPriority.HIGHEST)
	private void onArrowKill(final ArrowKillEvent e) {
		if (e.getTarget().equals(getParticipant()) && !e.isCancelled()) {
			MurderMystery murdermystery = (MurderMystery) getGame();
			int count = 0;
			for (Participant participant : getGame().getParticipants()) {
				if (participant.hasAbility() && !murdermystery.isDead(participant.getPlayer().getUniqueId()) && (participant.getAbility() instanceof AbstractInnocent || participant.getAbility() instanceof AbstractDetective)) count++;
			}
			if (count > 1) {
				target = e.getShooter();
				try {
					target.setAbility(Murderer.class);
					murdermystery.addGold(target, murdermystery.getGold(getParticipant()));
					Murderer murderer = (Murderer) target.getAbility();
					murderer.addArrow(getArrowCount(), true);
				} catch (UnsupportedOperationException | ReflectiveOperationException e1) {
					e1.printStackTrace();
				}	
			}
		}
	}
	
	public Participant getTarget() {
		return target;
	}
	
}
