package rainstar.abilitywar.game.murder.system.config.statictype;

import daybreak.abilitywar.game.AbstractGame;
import daybreak.abilitywar.game.AbstractGame.GameTimer;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.list.murdermystery.Items;
import daybreak.abilitywar.game.list.murdermystery.MurderMystery;
import daybreak.abilitywar.game.list.murdermystery.ability.AbstractMurderer;
import daybreak.abilitywar.game.module.Module;
import daybreak.abilitywar.game.module.ModuleBase;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.base.minecraft.nms.NMS;
import rainstar.abilitywar.game.murder.murderer.CorruptionDetectiveMurderer;
import rainstar.abilitywar.game.murder.murderer.JudasMurderer;
import rainstar.abilitywar.game.murder.murderer.PlagueDoctorMurderer;

@ModuleBase(CheckMurdererSword.class)
public class CheckMurdererSword extends GameTimer implements Module {

	private final MurderMystery murderyMystery;
	private final int time;
	
	public CheckMurdererSword(AbstractGame abstractGame, int time) {
		abstractGame.super(TaskType.REVERSE, time);
		setPeriod(TimeUnit.TICKS, 1);
		this.time = time;
		this.murderyMystery = (MurderMystery) abstractGame;
	}
	
	public boolean isCheckable() {
		return getCount() < time - 1300;
	}
	
	public int getTime() {
		return time;
	}
	
	@Override
	public void onStart() {
	}
	
	@Override
	public void run(int count) {
		if (count < time - 1000) {
			for (Participant participant : murderyMystery.getParticipants()) {
				if (count == time - 1100) NMS.clearTitle(participant.getPlayer());
				if (participant.hasAbility()) {
					if (participant.getAbility() instanceof AbstractMurderer) {
						if (!participant.getAbility().getClass().equals(PlagueDoctorMurderer.class) && !participant.getAbility().getClass().equals(JudasMurderer.class) && !participant.getAbility().getClass().equals(CorruptionDetectiveMurderer.class)) {
							AbstractMurderer abstractMurderer = (AbstractMurderer) participant.getAbility();
							if (!abstractMurderer.hasSword()) {
								participant.getPlayer().getInventory().setHeldItemSlot(0);
								participant.getPlayer().getInventory().setItem(1, Items.MURDERER_SWORD.getStack());
							}
						}
					}	
				}
			}	
		}
	}
	
	@Override
	public void onEnd() {
		unregister();
	}
	
	@Override
	public void register() {
		start();
	}

	@Override
	public void unregister() {
	}
	
}
