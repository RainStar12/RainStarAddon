package rainstar.abilitywar.game.murder.innocent;

import java.text.DecimalFormat;

import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.SubscribeEvent;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.AbstractGame.Participant.ActionbarNotification.ActionbarChannel;
import daybreak.abilitywar.game.list.murdermystery.Items;
import daybreak.abilitywar.game.list.murdermystery.MurderMystery;
import daybreak.abilitywar.game.list.murdermystery.ability.AbstractInnocent;
import daybreak.abilitywar.game.list.murdermystery.ability.AbstractMurderer;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.base.minecraft.nms.NMS;
import daybreak.abilitywar.utils.library.PotionEffects;
import daybreak.abilitywar.utils.library.SoundLib;

@AbilityManifest(name = "시민: 화가", rank = Rank.SPECIAL, species = Species.HUMAN, explain = {
		"금 우클릭으로 금 8개를 소모해 활과 화살을 얻을 수 있습니다.",
		"§c[§8먹칠§c]§f 금 좌클릭으로 금 1개를 소모해 모든 §5머더§f를 1초간 §7실명§f시킵니다.",
		"§8[§e감명§8]§f §8먹칠§f 시마다 지속시간이 0.5초씩 늘어납니다. 요구 금도 늘어납니다.",
})
public class Painter extends AbstractInnocent {
	
	public Painter(Participant participant) {
		super(participant);
	}
	
	@Override
	protected void onUpdate(Update update) {
		super.onUpdate(update);
		if (update == Update.RESTRICTION_CLEAR) {
			NMS.sendTitle(getPlayer(), "§e역할§f: §a화가", "§f모든 §c머더§f에게 예술혼을 불태우세요!", 10, 80, 10);
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

	private ActionbarChannel ac = newActionbarChannel();
	private DecimalFormat df = new DecimalFormat("0.0");
	private int stack = 0;
	
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
			} else if (e.getAction() == Action.LEFT_CLICK_AIR || e.getAction() == Action.LEFT_CLICK_BLOCK) {
				MurderMystery murderMystery = (MurderMystery) getGame();
				if (murderMystery.consumeGold(getParticipant(), 1 + stack)) {
					for (Participant participant : getGame().getParticipants()) {
						if (participant.getAbility() instanceof AbstractMurderer) {
							PotionEffects.BLINDNESS.addPotionEffect(participant.getPlayer(), 20 + (stack * 10), 1, false, false, true);
							SoundLib.ITEM_BUCKET_EMPTY_LAVA.playSound(participant.getPlayer(), 1, 0.66f);
							participant.getPlayer().sendMessage("§8먹칠당했습니다!");
						}
					}
					stack++;
					ac.update("§c실명§7: §9" + df.format(1 + (stack * 0.5)) + "§f초 §f| §e금§7: §e" + (1 + stack) + "개");
					getPlayer().sendMessage("§4[§c!§4] §8먹칠§f을 사용하였습니다.");
					SoundLib.ITEM_BUCKET_EMPTY_LAVA.playSound(getPlayer(), 1, 0.66f);
				}
			}
		}
	}
	
}
