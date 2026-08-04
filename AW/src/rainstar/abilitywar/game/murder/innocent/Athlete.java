package rainstar.abilitywar.game.murder.innocent;

import java.text.DecimalFormat;
import java.util.UUID;

import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.attribute.AttributeModifier.Operation;
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
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.base.minecraft.nms.NMS;

@AbilityManifest(name = "시민: 육상선수", rank = Rank.SPECIAL, species = Species.HUMAN, explain = {
		"금 우클릭으로 금 8개를 소모해 활과 화살을 얻을 수 있습니다.",
		"§8[§b질주§8]§f 금 좌클릭으로 금 3개를 소모해 5초간 §b이동 속도§f가 300%로 증가합니다.",
		"§8[§c탈진§8]§f 사용 시마다 증가량이 30% 감소합니다§8(§7최소 150%§8)§f."
})
public class Athlete extends AbstractInnocent {
	
	public Athlete(Participant participant) {
		super(participant);
	}
	
	@Override
	protected void onUpdate(Update update) {
		super.onUpdate(update);
		if (update == Update.RESTRICTION_CLEAR) {
			NMS.sendTitle(getPlayer(), "§e역할§f: §a육상선수", "§f모든 §c머더§f에게서 달아나세요!", 10, 80, 10);
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
		ac2.update("§3속도§f: §b" + Math.max((300 - (usestack * 30)), 135) + "%§f");
	}
	
	private final AttributeModifier modifier1 = new AttributeModifier(UUID.randomUUID(), "addspeed", 2.0, Operation.ADD_SCALAR), modifier2 = new AttributeModifier(UUID.randomUUID(), "addspeed", 1.7, Operation.ADD_SCALAR), 
			modifier3 = new AttributeModifier(UUID.randomUUID(), "addspeed", 1.4, Operation.ADD_SCALAR), modifier4 = new AttributeModifier(UUID.randomUUID(), "addspeed", 1.1, Operation.ADD_SCALAR), 
			modifier5 = new AttributeModifier(UUID.randomUUID(), "addspeed", 0.8, Operation.ADD_SCALAR), modifier6 = new AttributeModifier(UUID.randomUUID(), "addspeed", 0.5, Operation.ADD_SCALAR);
	private final ActionbarChannel ac = newActionbarChannel(), ac2 = newActionbarChannel();
	private final DecimalFormat df = new DecimalFormat("0.0");
	private int usestack = 0;

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
				if (!skill.isRunning()) {
					if (murderMystery.consumeGold(getParticipant(), 3)) {
						getPlayer().sendMessage("§4[§c!§4] §b질주§f를 사용하였습니다.");
						skill.start();
					}
				} else getPlayer().sendMessage("§4[§c!§4] §c현재 질주가 지속 중입니다.");
			}
		}
	}
	
	private final AbilityTimer skill = new AbilityTimer(100) {

		@Override
		protected void onStart() {
			try {
				switch(usestack) {
				case 0: 
					getPlayer().getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).addModifier(modifier1);
					break;
				case 1: 
					getPlayer().getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).addModifier(modifier2);
					break;
				case 2: 
					getPlayer().getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).addModifier(modifier3);
					break;
				case 3: 
					getPlayer().getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).addModifier(modifier4);
					break;
				case 4: 
					getPlayer().getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).addModifier(modifier5);
					break;
				case 5: 
					getPlayer().getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).addModifier(modifier6);
					break;
				}
			} catch (IllegalArgumentException ignored) {
			}
		}

		@Override
		protected void onEnd() {
			onSilentEnd();
		}

		@Override
		protected void onSilentEnd() {
			ac.update(null);
			switch(usestack) {
			case 0: 
				getPlayer().getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).removeModifier(modifier1);
				break;
			case 1: 
				getPlayer().getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).removeModifier(modifier2);
				break;
			case 2: 
				getPlayer().getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).removeModifier(modifier3);
				break;
			case 3: 
				getPlayer().getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).removeModifier(modifier4);
				break;
			case 4: 
				getPlayer().getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).removeModifier(modifier5);
				break;
			case 5: 
				getPlayer().getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).removeModifier(modifier6);
				break;
			}
			
			usestack = Math.min(usestack + 1, 5);
			ac2.update("§3속도§f: §b" + Math.max((300 - (usestack * 30)), 150) + "%§f");
		}

		@Override
		protected void run(int count) {
			ac.update("§b질주§f: §3" + df.format(count / 20.0) + "§f초");
		}
		
	}.setPeriod(TimeUnit.TICKS, 1).register();
	
}