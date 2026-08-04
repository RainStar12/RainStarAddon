package rainstar.abilitywar.game.murder.innocent;

import org.bukkit.event.EventPriority;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

import daybreak.abilitywar.ability.AbilityBase;
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
import daybreak.abilitywar.game.list.murdermystery.ability.Innocent;
import daybreak.abilitywar.game.list.murdermystery.ability.Murderer;
import daybreak.abilitywar.game.list.murdermystery.ability.AbstractMurderer.MurderEvent;
import daybreak.abilitywar.game.manager.effect.Fear;
import daybreak.abilitywar.game.list.murdermystery.ability.Detective;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.base.minecraft.nms.NMS;

@AbilityManifest(name = "시민: 유령", rank = Rank.SPECIAL, species = Species.UNDEAD, explain = {
		"금 우클릭으로 금 8개를 소모해 활과 화살을 얻을 수 있습니다.",
		"§8[§b영체§8]§f §b영매사§f의 채팅을 볼 수 있습니다.",
		"자신을 쏜 §a시민§f이 사망하지 않습니다.",
		"§8[§c원혼§8]§f 사망 시, 자신을 죽인 대상의 §e금§f을 내가 소지한 금만큼 제거한 뒤,",
		"일반 직업§8(§7머더/시민/탐정§8)§f으로 되돌리고, 1초간 §b§n공포§f에 빠뜨립니다."
})
public class Ghost extends AbstractInnocent {

	public Ghost(Participant participant) {
		super(participant);
	}

	@Override
	protected void onUpdate(Update update) {
		super.onUpdate(update);
		if (update == Update.RESTRICTION_CLEAR) {
			NMS.sendTitle(getPlayer(), "§e역할§f: §a유령", "§f자신을 죽인 사람을 §5저주§f해보세요!", 10, 80, 10);
			new AbilityTimer(1) {
				@Override
				protected void run(int count) {
				}

				@Override
				protected void onEnd() {
					NMS.clearTitle(getPlayer());
				}
			}.setInitialDelay(TimeUnit.SECONDS, 5).start();
		}
	}
	
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
	
	@SubscribeEvent(priority = 12, eventPriority = EventPriority.HIGHEST)
	private void onMurder(MurderEvent e) {
		if (e.getTarget().equals(getParticipant()) && !e.isCancelled()) {
			e.setCancelled(true);
			try {
				curse(e.getMurderer().getParticipant());
			} catch (UnsupportedOperationException | ReflectiveOperationException e1) {
				e1.printStackTrace();
			}
			getPlayer().setHealth(0);
		}
	}
	
	@SubscribeEvent(priority = 12, eventPriority = EventPriority.HIGHEST)
	private void onArrowKill(final ArrowKillEvent e) {
		if (e.getTarget().equals(getParticipant()) && !e.isCancelled()) {
			e.setCancelled(true);
			try {
				curse(e.getShooter());
			} catch (UnsupportedOperationException | ReflectiveOperationException e1) {
				e1.printStackTrace();
			}
			getPlayer().setHealth(0);
		}
	}
	
	public void curse(Participant attacker) throws UnsupportedOperationException, ReflectiveOperationException {
		MurderMystery murdermystery = (MurderMystery) getGame();
		murdermystery.consumeGold(attacker, Math.min(murdermystery.getGold(attacker), murdermystery.getGold(getParticipant())));
		AbilityBase ab = attacker.getAbility();
		if (ab != null) {
			if (ab instanceof AbstractDetective) attacker.setAbility(Detective.class);
			else if (ab instanceof AbstractInnocent) attacker.setAbility(Innocent.class);
			else if (ab instanceof AbstractMurderer) attacker.setAbility(Murderer.class);
		}
		Fear.apply(attacker, TimeUnit.TICKS, 20, getPlayer());
	}
	
}
