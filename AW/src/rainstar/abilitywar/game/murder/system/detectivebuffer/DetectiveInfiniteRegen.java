package rainstar.abilitywar.game.murder.system.detectivebuffer;

import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.ability.SubscribeEvent;
import daybreak.abilitywar.ability.SubscribeEvent.Priority;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.AbstractGame.Participant.ActionbarNotification.ActionbarChannel;
import daybreak.abilitywar.game.list.murdermystery.Items;
import daybreak.abilitywar.game.list.murdermystery.MurderMystery;
import daybreak.abilitywar.game.list.murdermystery.MurderMystery.ArrowKillEvent;
import daybreak.abilitywar.game.list.murdermystery.ability.AbstractDetective;
import daybreak.abilitywar.game.list.murdermystery.ability.AbstractMurderer.MurderEvent;
import daybreak.abilitywar.utils.base.ProgressBar;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.base.minecraft.nms.NMS;
import daybreak.abilitywar.utils.library.ParticleLib;
import daybreak.abilitywar.utils.base.color.RGB;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

@AbilityManifest(name = "탐정 <무한 보급>", rank = Rank.SPECIAL, species = Species.HUMAN, explain = {
		"활이 기본적으로 지급되며, §51~20초§f마다 최대 §564개까지§f 화살이 보급됩니다.",
		"플레이어가 죽은 위치에 흔적이 남아 볼 수 있으며,",
		"흔적의 색이 30초마다 빨간색 -> 검붉은색 -> 검은색의 순서로 바뀝니다.",
		"금 우클릭으로 금 8개를 소모해 활과 화살을 얻을 수 있습니다."
})
public class DetectiveInfiniteRegen extends AbstractDetective {

	private final ActionbarChannel actionbarChannel = newActionbarChannel();

	public DetectiveInfiniteRegen(Participant participant) {
		super(participant);
	}

	@Override
	protected void onUpdate(Update update) {
		if (update == Update.RESTRICTION_CLEAR) {
			PlayerInventory inventory = getPlayer().getInventory();
			inventory.clear();
			getPlayer().getInventory().setHeldItemSlot(0);
			inventory.setItem(1, Items.DETECTIVE_BOW.getStack());
			if (getArrowCount() < 64) {
				charge.start();
			}
			((MurderMystery) getGame()).updateGold(getParticipant());
			NMS.sendTitle(getPlayer(), "§e역할§f: §3탐정", "§c머더§f로부터 §a시민§f들을 보호하세요!", 10, 80, 10);
			new AbilityTimer(1) {
				@Override
				protected void onEnd() {
					NMS.clearTitle(getPlayer());
				}
			}.setInitialDelay(TimeUnit.SECONDS, 5).start();
		}
	}
	
	private int randomvalue = 10;

	private final AbilityTimer charge = new AbilityTimer(10) {
		private ProgressBar progressBar;

		@Override
		protected void onStart() {
			progressBar = new ProgressBar(randomvalue, randomvalue);
			progressBar.setStep(0);
		}

		@Override
		protected void run(int count) {
			if (getArrowCount() >= 64) {
				stop(true);
				return;
			}
			progressBar.step();
			actionbarChannel.update("화살 보급: " + progressBar.toString());
		}

		@Override
		protected void onEnd() {
			onSilentEnd();
			addArrow();
			if (getArrowCount() < 64) {
				randomvalue = random.nextInt(20) + 1;
				start();
				setCount(randomvalue);
			}
		}

		@Override
		protected void onSilentEnd() {
			actionbarChannel.update(null);
		}
	}.setPeriod(TimeUnit.TICKS, 20);

	@SubscribeEvent(onlyRelevant = true)
	private void onShoot(EntityShootBowEvent e) {
		charge.start();
	}

	@SubscribeEvent(onlyRelevant = true)
	private void onInteract(PlayerInteractEvent e) {
		if (Items.isGold(e.getItem())) {
			if (e.getAction() == Action.RIGHT_CLICK_AIR || e.getAction() == Action.RIGHT_CLICK_BLOCK) {
				final MurderMystery murderMystery = (MurderMystery) getGame();
				if (murderMystery.consumeGold(getParticipant(), 8)) {
					if (!addArrow()) {
						murderMystery.addGold(getParticipant());
					}
				}
			}
		}
	}

	@SubscribeEvent(priority = Priority.HIGHEST, ignoreCancelled = true)
	private void onMurder(final MurderEvent e) {
		new Blood(e.getTarget().getPlayer().getLocation().clone().add(0, 1, 0)).start();
	}

	@SubscribeEvent(priority = Priority.HIGHEST, ignoreCancelled = true)
	private void onArrowKill(final ArrowKillEvent e) {
		new Blood(e.getTarget().getPlayer().getLocation().add(0, 1, 0)).start();
	}

	public boolean hasBow() {
		ItemStack stack = getPlayer().getInventory().getItem(2);
		return stack != null && stack.getType() == Material.BOW;
	}

	public boolean addArrow() {
		ItemStack stack = getPlayer().getInventory().getItem(3);
		if (stack != null && stack.getType() == Material.ARROW) {
			if (stack.getAmount() < 64) {
				stack.setAmount(stack.getAmount() + 1);
				getPlayer().getInventory().setItem(3, stack);
				getPlayer().sendMessage("§8+ §f1 화살");
				return true;
			} else return false;
		} else {
			getPlayer().getInventory().setItem(3, new ItemStack(Material.ARROW));
			getPlayer().sendMessage("§8+ §f1 화살");
			return true;
		}
	}

	private static final RGB RED = RGB.of(254, 54, 54), DARK_RED = RGB.of(128, 8, 8), BLACK = RGB.of(1, 1, 1);

	private class Blood extends AbilityTimer {

		private final Location location;

		private Blood(final Location location) {
			super();
			setPeriod(TimeUnit.TICKS, 1);
			this.location = location;
		}

		@Override
		protected void run(int count) {
			ParticleLib.REDSTONE.spawnParticle(getPlayer(), location, count <= 600 ? RED : (count <= 1200 ? DARK_RED : BLACK));
		}

	}

}