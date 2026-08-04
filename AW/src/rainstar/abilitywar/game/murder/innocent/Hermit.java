package rainstar.abilitywar.game.murder.innocent;

import java.text.DecimalFormat;

import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;

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
import daybreak.abilitywar.utils.base.concurrent.SimpleTimer.TaskType;
import daybreak.abilitywar.utils.base.minecraft.nms.NMS;
import daybreak.abilitywar.utils.library.ParticleLib;
import daybreak.abilitywar.utils.library.SoundLib;

@AbilityManifest(name = "시민: 은둔자", rank = Rank.SPECIAL, species = Species.HUMAN, explain = {
		"금 우클릭으로 금 8개를 소모해 활과 화살을 얻을 수 있습니다.",
		"§8[§7은신§8]§f 웅크리면 금 3개를 소모해 최대 7.5초간 §7은신§f할 수 있습니다.",
		"웅크리기를 해제하면 남은 지속시간이 2.5초까지 줄어듭니다."
})
public class Hermit extends AbstractInnocent {

	public Hermit(Participant participant) {
		super(participant);
	}
	
	private ActionbarChannel ac = newActionbarChannel();
	private DecimalFormat df = new DecimalFormat("0.0");
	
	@Override
	protected void onUpdate(Update update) {
		super.onUpdate(update);
		if (update == Update.RESTRICTION_CLEAR) {
			NMS.sendTitle(getPlayer(), "§e역할§f: §a은둔자", "§c머더§f에게서 숨어 살아남으세요!", 10, 80, 10);
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
	
	private final AbilityTimer skill = new AbilityTimer(TaskType.REVERSE, 150) {
		
		@Override
		public void onStart() {
			SoundLib.ENTITY_SILVERFISH_AMBIENT.playSound(getPlayer().getLocation(), 0.5f, 1f);
			ParticleLib.DRIP_LAVA.spawnParticle(getPlayer().getLocation().clone().add(0, 1, 0), 0.15, 0.15, 0.15, 100, 0);
			getPlayer().getInventory().setArmorContents(null);
			NMS.setArrowsInBody(getPlayer(), 0);
			getPlayer().getInventory().setHeldItemSlot(5);
			getParticipant().attributes().TARGETABLE.setValue(false);
			NMS.setInvisible(getPlayer(), true);
		}
		
		@Override
		public void run(int count) {
			ac.update("§7은신§3: §b" + df.format(count / 20.0) + "§f초");
			NMS.setInvisible(getPlayer(), true);
		}
		
		@Override
		public void onEnd() {
			onSilentEnd();
		}
		
		@Override
		public void onSilentEnd() {
			ac.update(null);
			getParticipant().attributes().TARGETABLE.setValue(true);
			NMS.setInvisible(getPlayer(), false);
		}
		
	}.setPeriod(TimeUnit.TICKS, 1).register();
	
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
	
	@SubscribeEvent(onlyRelevant = true)
	private void onToggleSneak(final PlayerToggleSneakEvent e) {
		if (e.isSneaking()) {
			MurderMystery murderMystery = (MurderMystery) getGame();
			if (!skill.isRunning()) {
				if (murderMystery.consumeGold(getParticipant(), 3)) {
					skill.start();
					getPlayer().sendMessage("§4[§c!§4] §7은신§f을 사용하였습니다.");
				}
			} else getPlayer().sendMessage("§4[§c!§4] §c현재 은신이 지속 중입니다.");
		}
		
		if (skill.isRunning() && skill.getCount() > 50 && !e.isSneaking()) {
			skill.setCount(50);
		}
	}
	
}
