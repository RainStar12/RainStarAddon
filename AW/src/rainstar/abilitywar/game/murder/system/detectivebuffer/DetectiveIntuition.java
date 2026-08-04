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
import daybreak.abilitywar.utils.base.concurrent.SimpleTimer.TaskType;
import daybreak.abilitywar.utils.base.math.LocationUtil;
import daybreak.abilitywar.utils.base.minecraft.nms.NMS;
import daybreak.abilitywar.utils.library.ParticleLib;
import daybreak.abilitywar.utils.library.SoundLib;
import daybreak.abilitywar.utils.base.color.RGB;

import java.text.DecimalFormat;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

@AbilityManifest(name = "탐정 <직감>", rank = Rank.SPECIAL, species = Species.HUMAN, explain = {
		"활이 기본적으로 지급되며, §55초마다§f 최대 2개까지 화살이 보급됩니다.",
		"플레이어가 죽은 위치에 흔적이 남아 볼 수 있으며,",
		"흔적의 색이 30초마다 빨간색 -> 검붉은색 -> 검은색의 순서로 바뀝니다.",
		"§5검을 드는 것§f을 §5소리§f로 알아채고 이때 §51.5초 무적§f이 되며,",
		"나침반이 §5가장 가까운 검을 든 사람§f의 방향을 가리킵니다.",
		"금 우클릭으로 금 8개를 소모해 활과 화살을 얻을 수 있습니다."
})
public class DetectiveIntuition extends AbstractDetective {

	private final ActionbarChannel actionbarChannel = newActionbarChannel();

	public DetectiveIntuition(Participant participant) {
		super(participant);
	}

	@Override
	protected void onUpdate(Update update) {
		if (update == Update.RESTRICTION_CLEAR) {
			PlayerInventory inventory = getPlayer().getInventory();
			inventory.clear();
			getPlayer().getInventory().setHeldItemSlot(0);
			inventory.setItem(1, Items.DETECTIVE_BOW.getStack());
			if (getArrowCount() < 2) {
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
			passive.start();
		}
	}
	
	private final Predicate<Entity> predicate = new Predicate<Entity>() {
		@Override
		public boolean test(Entity entity) {
			if (entity.equals(getPlayer())) return false;
			return !(entity instanceof Player) || (getGame().isParticipating(entity.getUniqueId())
					&& !((MurderMystery) getGame()).isDead(entity.getUniqueId())
					&& getGame().getParticipant(entity.getUniqueId()).attributes().TARGETABLE.getValue());
		}
	};
	
	private final Predicate<Entity> predicate2 = new Predicate<Entity>() {
		@Override
		public boolean test(Entity entity) {
			if (entity.equals(getPlayer())) return false;
			return !(entity instanceof Player) || (getGame().isParticipating(entity.getUniqueId())
					&& !((MurderMystery) getGame()).isDead(entity.getUniqueId())
					&& getGame().getParticipant(entity.getUniqueId()).attributes().TARGETABLE.getValue())
					&& swordhand.contains(getGame().getParticipant(entity.getUniqueId()));
		}
	};
	
	private Set<Participant> swordhand = new HashSet<>();
	private ActionbarChannel ac = newActionbarChannel(), ac2 = newActionbarChannel();
	private DecimalFormat df = new DecimalFormat("0.0");
	
	public AbilityTimer passive = new AbilityTimer() {
		
		@Override
		public void run(int count) {
			for (Participant participant : getGame().getParticipants()) {
				Player player = participant.getPlayer();
				if (Items.isMurdererSword(player.getInventory().getItemInMainHand()) && !swordhand.contains(getGame().getParticipant(player))) {
					swordhand.add(getGame().getParticipant(player));
					SoundLib.ITEM_ARMOR_EQUIP_IRON.playSound(getPlayer(), player.getLocation(), 30f, 1.3f);
					if (skill.isRunning()) skill.setCount(20);
					else skill.start();
				}
			}
			
			if (swordhand.size() > 0) {
				if (LocationUtil.getNearestEntity(Player.class, getPlayer().getLocation(), predicate2) != null) {
					getPlayer().setCompassTarget(LocationUtil.getNearestEntity(Player.class, getPlayer().getLocation(), predicate2).getLocation());	
					ac.update("§c!!!!!");
				} else {
					if (LocationUtil.getNearestEntity(Player.class, getPlayer().getLocation(), predicate) != null) {
						getPlayer().setCompassTarget(LocationUtil.getNearestEntity(Player.class, getPlayer().getLocation(), predicate).getLocation());	
						ac.update(null);
					}
				}
			} else {
				if (LocationUtil.getNearestEntity(Player.class, getPlayer().getLocation(), predicate) != null) {
					getPlayer().setCompassTarget(LocationUtil.getNearestEntity(Player.class, getPlayer().getLocation(), predicate).getLocation());	
					ac.update(null);
				}
			}
			
			Set<Participant> removers = new HashSet<>();
			
			for (Participant participant : swordhand) {
				if (!Items.isMurdererSword(participant.getPlayer().getInventory().getItemInMainHand())) {
					removers.add(participant);
				}
			}
			
			swordhand.removeAll(removers);
			removers.clear();
		}
		
	}.setPeriod(TimeUnit.TICKS, 1).register();
	
	private final AbilityTimer skill = new AbilityTimer(TaskType.REVERSE, 30) {
		
		@Override
		public void onStart() {
		}
		
		@Override
		public void run(int count) {
			ac2.update("§3무적§7: §b" + df.format(count / 20.0) + "§f초");
		}
		
		@Override
		public void onEnd() {
			onSilentEnd();
		}
		
		@Override
		public void onSilentEnd() {
			ac2.update(null);
		}
		
	}.setPeriod(TimeUnit.TICKS, 1).register();
	
	@SubscribeEvent(priority = 1)
	private void onMurderShield(MurderEvent e) {
		if (e.getTarget().equals(getParticipant()) && !e.isCancelled() && skill.isRunning()) {
			e.setCancelled(true);
			SoundLib.ITEM_SHIELD_BLOCK.playSound(getPlayer().getLocation(), 1 , 1.25f);
			ParticleLib.CRIT_MAGIC.spawnParticle(getPlayer().getLocation(), 0.5, 1, 0.5, 10, 0);
		}
	}
	
	@SubscribeEvent(priority = 1)
	private void onArrowShield(ArrowKillEvent e) {
		if (e.getTarget().equals(getParticipant()) && !e.isCancelled() && skill.isRunning()) {
			e.setCancelled(true);
			SoundLib.ITEM_SHIELD_BLOCK.playSound(getPlayer().getLocation(), 1 , 1.25f);
			ParticleLib.CRIT_MAGIC.spawnParticle(getPlayer().getLocation(), 0.5, 1, 0.5, 10, 0);
		}
	}

	private final AbilityTimer charge = new AbilityTimer(10) {
		private final ProgressBar progressBar = new ProgressBar(10, 10);

		@Override
		protected void onStart() {
			progressBar.setStep(0);
		}

		@Override
		protected void run(int count) {
			if (getArrowCount() >= 2) {
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
			if (getArrowCount() < 2) {
				start();
			}
		}

		@Override
		protected void onSilentEnd() {
			actionbarChannel.update(null);
		}
	}.setPeriod(TimeUnit.TICKS, 10);

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