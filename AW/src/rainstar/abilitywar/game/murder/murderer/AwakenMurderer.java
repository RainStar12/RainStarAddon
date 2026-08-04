package rainstar.abilitywar.game.murder.murderer;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.event.HandlerList;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.PlayerInventory;

import daybreak.abilitywar.AbilityWar;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.SubscribeEvent;
import daybreak.abilitywar.ability.AbilityBase.AbilityTimer;
import daybreak.abilitywar.ability.AbilityBase.Update;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.list.murdermystery.Items;
import daybreak.abilitywar.game.list.murdermystery.MurderMystery;
import daybreak.abilitywar.game.list.murdermystery.ability.AbstractMurderer;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.base.minecraft.nms.NMS;

@AbilityManifest(name = "머더: 깨어난 자", rank = Rank.SPECIAL, species = Species.HUMAN, explain = {
		"모든 시민을 죽이세요!",
		"금 우클릭으로 금 8개를 소모해 활과 화살을 얻을 수 있습니다.",
		"§8[§a계시§8]§f 매 30초마다 무작위 §a계시§f가 내려옵니다.",
		"시간 안에 §a계시§f를 완수한다면, §8[§c종언§8]§f이 1단계 증가합니다.",
		"§8[§c종언§8]§f 올라갈 때마다 채팅에 공개됩니다.",
		"§4[§cⅠ§4]§f 모든 시민팀의 화살 속도가 15% 감소합니다.",
		"§4[§cⅡ§4]§f 모든 시민팀이 §e금§f을 50% 더 소모합니다.",
		"§4[§cⅢ§4]§f 모든 시민팀의 이동 속도가 20% 감소합니다.",
		"§4[§cⅣ§4]§f 모든 시민팀이 활 사용 후 1~3초의 쿨타임을 가집니다.",
		"§4[§cⅤ§4]§f 모든 시민팀이 영구적으로 §8실명§f합니다.",
		"§4[§cⅩ§4]§f 모든 시민팀이 §4처형§f당하고 §a승리§f합니다."
})
public class AwakenMurderer extends AbstractMurderer {
	
	public AwakenMurderer(Participant participant) {
		super(participant);
	}

	@Override
	protected void onUpdate(Update update) {
		super.onUpdate(update);
		if (update == Update.RESTRICTION_CLEAR) {
			NMS.sendTitle(getPlayer(), "§e직업§f: §9깨어난 자", "§f계시를 받고, 종언을 이룩하세요.", 10, 80, 10);
			new AbilityTimer(1) {
				@Override
				protected void onEnd() {
					NMS.clearTitle(getPlayer());
				}
			}.setInitialDelay(TimeUnit.SECONDS, 5).start();
			PASSIVE.stop(false);
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
	
	private int apocalypse = 0;
	
	@SuppressWarnings("serial")
	private static final List<String> revelation = new ArrayList<String>() {
		{
			add("§3금 좌클릭으로 1개씩, 총 32회 소모하라.");
			add("§3시민의 화살이 자신으로부터 2칸 이내를 지나쳐라.");
			add("§3목표 대상을 사살해라.");
			add("§3에 도착해 10초간 가만히 멈추어라.");
			add("§3목표 대상으로부터 사거리 7 이내를 10초간 유지하라.");
			add("§3시간 동안 한 번도 뛰어다니지 않아라.");
			add("§3");
		}
	};
	
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
	
}
