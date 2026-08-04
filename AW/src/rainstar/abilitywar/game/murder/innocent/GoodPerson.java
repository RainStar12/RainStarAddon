package rainstar.abilitywar.game.murder.innocent;

import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.ability.SubscribeEvent;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.list.murdermystery.Items;
import daybreak.abilitywar.game.list.murdermystery.MurderMystery;
import daybreak.abilitywar.game.list.murdermystery.MurderMystery.ArrowKillEvent;
import daybreak.abilitywar.game.list.murdermystery.ability.AbstractInnocent;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.base.minecraft.nms.NMS;
import daybreak.abilitywar.utils.library.ParticleLib;

import org.bukkit.event.EventPriority;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.player.PlayerInteractEvent;

@AbilityManifest(name = "시민: 선량한 시민", rank = Rank.SPECIAL, species = Species.HUMAN, explain = {
		"금 우클릭으로 금 5개를 소모해 활과 화살을 얻을 수 있습니다.",
		"§8[§a입증§8]§f 화살 공격을 §a받지 않습니다§f.",
		"§8[§7주저§8]§f 자신의 화살 속도가 25% 감소합니다."
})
public class GoodPerson extends AbstractInnocent {
	
	public GoodPerson(Participant participant) {
		super(participant);
	}

	@Override
	protected void onUpdate(Update update) {
		super.onUpdate(update);
		if (update == Update.RESTRICTION_CLEAR) {
			NMS.sendTitle(getPlayer(), "§e역할§f: §a선량한 시민", "§f시민을 입증하고 §c머더§f를 피해 살아남으세요!", 10, 80, 10);
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
	
	@SubscribeEvent(priority = 1, eventPriority = EventPriority.LOWEST)
	private void onArrowKill(final ArrowKillEvent e) {
		if (e.getTarget().equals(getParticipant())) {
			e.setCancelled(true);
			ParticleLib.VILLAGER_HAPPY.spawnParticle(getPlayer().getLocation(), 1, 2, 1, 10, 0);
			e.getShooter().getPlayer().sendMessage("§4[§c!§4] §a선량한 시민§f은 활로 쏠 수 없습니다.");
		}
	}

	@SubscribeEvent(onlyRelevant = true)
	private void onInteract(PlayerInteractEvent e) {
		if (Items.isGold(e.getItem())) {
			if (e.getAction() == Action.RIGHT_CLICK_AIR || e.getAction() == Action.RIGHT_CLICK_BLOCK) {
				final MurderMystery murderMystery = (MurderMystery) getGame();
				if (murderMystery.consumeGold(getParticipant(), 5)) {
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
	
	@SubscribeEvent
	public void onProjectileLaunch(ProjectileLaunchEvent e) {
		if (getPlayer().equals(e.getEntity().getShooter()) && NMS.isArrow(e.getEntity())) {
			e.getEntity().setVelocity(e.getEntity().getVelocity().multiply(0.75));
		}
	}
	
}
