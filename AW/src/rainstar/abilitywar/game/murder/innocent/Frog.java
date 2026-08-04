package rainstar.abilitywar.game.murder.innocent;

import java.text.DecimalFormat;

import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.util.Vector;

import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.SubscribeEvent;
import daybreak.abilitywar.ability.AbilityFactory.AbilityRegistration;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.AbstractGame.Participant.ActionbarNotification.ActionbarChannel;
import daybreak.abilitywar.game.list.murdermystery.Items;
import daybreak.abilitywar.game.list.murdermystery.MurderMystery;
import daybreak.abilitywar.game.list.murdermystery.ability.AbstractInnocent;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.base.minecraft.nms.NMS;

@AbilityManifest(name = "시민: 개구리", rank = Rank.SPECIAL, species = Species.ANIMAL, explain = {
		"마녀의 §5저주§f에 당했습니다!",
		"금 우클릭으로 금 8개를 소모해 활과 화살을 얻을 수 있습니다.",
		"§8[§5저주§8]§f 지면에 착지하면 자동으로 튀어오릅니다.",
		"44초가 지나면 §5저주§f가 풀려 원래 직업으로 되돌아갑니다."
})
public class Frog extends AbstractInnocent {
	
	public Frog(Participant participant) {
		super(participant);
	}
	
	@Override
	protected void onUpdate(Update update) {
		super.onUpdate(update);
		if (update == Update.RESTRICTION_CLEAR) {
			NMS.sendTitle(getPlayer(), "§e역할§f: §a개구리", "§f저주당했습니다! §d마녀§f를 찾아내세요.", 10, 80, 10);
			new AbilityTimer(1) {
				@Override
				protected void run(int count) {
				}

				@Override
				protected void onEnd() {
					NMS.clearTitle(getPlayer());
				}
			}.setInitialDelay(TimeUnit.SECONDS, 5).start();
			passive.start();
		}
	}
	
	private static final Vector upVector = new Vector(0, 0.43, 0);
	private final ActionbarChannel ac = newActionbarChannel();
	private final DecimalFormat df = new DecimalFormat("0.0");
	private AbilityRegistration previousRegistration;
	
	public void setPreviousAbility(AbilityRegistration registration) {
		previousRegistration = registration;
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
	
	public AbilityTimer passive = new AbilityTimer(880) {
		
		@SuppressWarnings("deprecation")
		@Override
		public void run(int count) {
			ac.update("§a개구리화§7: §5" + df.format(count / 20.0) + "§f초");
			if (getPlayer().isOnGround()) {
				getPlayer().setVelocity(upVector);
			}
		}
		
		@Override
		public void onEnd() {
			ac.update(null);
			try {
				getParticipant().setAbility(previousRegistration);
			} catch (UnsupportedOperationException | ReflectiveOperationException e) {
				e.printStackTrace();
			}
		}
		
	}.setPeriod(TimeUnit.TICKS, 1).register();

}
