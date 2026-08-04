package rainstar.abilitywar.game.murder.system.detectivebuffer;

import daybreak.abilitywar.AbilityWar;
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
import daybreak.abilitywar.game.list.murdermystery.event.PreConsumeGoldEvent;
import daybreak.abilitywar.utils.base.ProgressBar;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.base.minecraft.item.Skulls;
import daybreak.abilitywar.utils.base.minecraft.nms.NMS;
import daybreak.abilitywar.utils.library.ParticleLib;
import daybreak.abilitywar.utils.base.color.RGB;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;

@AbilityManifest(name = "탐정 <재물조사>", rank = Rank.SPECIAL, species = Species.HUMAN, explain = {
		"활이 기본적으로 지급되며, 7초마다 최대 2개까지 화살이 보급됩니다.",
		"플레이어가 죽은 위치에 흔적이 남아 볼 수 있으며,",
		"흔적의 색이 30초마다 빨간색 -> 검붉은색 -> 검은색의 순서로 바뀝니다.",
		"금 우클릭으로 금 8개를 소모해 활과 화살을 얻을 수 있습니다.",
		"금 좌클릭으로 모든 대상의 현재 §5금 개수§f를 확인합니다. §c쿨타임 §7: §f10초",
		"금 F키로 금 3개를 소모해 대상을 정해 대상이 §5금을 소모할 때마다§f 소모량을 알아냅니다."
})
public class DetectiveGoldDetect extends AbstractDetective {

	private final ActionbarChannel actionbarChannel = newActionbarChannel();

	public DetectiveGoldDetect(Participant participant) {
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
		}
	}
	
	private Cooldown cooldown = new Cooldown(10);
	private Participant selected;

	private final AbilityTimer charge = new AbilityTimer(14) {
		private final ProgressBar progressBar = new ProgressBar(14, 14);

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

    @SubscribeEvent(onlyRelevant = true, priority = 0)
    public void onPlayerSwapHandItems(PlayerSwapHandItemsEvent e) {
    	if (Items.isGold(e.getOffHandItem()) && e.getPlayer().equals(getPlayer())) {
    		new SkillGUI().start();
    	}
    }
	
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
			} else if (e.getAction() == Action.LEFT_CLICK_AIR || e.getAction() == Action.LEFT_CLICK_BLOCK) {
				if (!cooldown.isCooldown()) {
					final MurderMystery murderMystery = (MurderMystery) getGame();
					getPlayer().sendMessage("§6==============================");
					for (Participant participant : getGame().getParticipants()) {
			    		if (!((MurderMystery) getGame()).isDead(participant.getPlayer().getUniqueId()) && !participant.equals(getParticipant())) {
			    			getPlayer().sendMessage("§4[§c!§4] §a" + participant.getPlayer().getName() + "§f: §e" + murderMystery.getGold(participant) + "§f개");	
			    		}
					}
					getPlayer().sendMessage("§6==============================");
					cooldown.start();
				}
			}
		}
	}
	
	@SubscribeEvent()
	private void onConsumeGold(PreConsumeGoldEvent e) {
		if (e.getParticipant().equals(selected)) {
			getPlayer().sendMessage("§6[§e!§6] §e" + e.getParticipant().getPlayer().getName() + "§f: §c-" + e.getAmount());
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
	
	public class SkillGUI extends AbilityTimer implements Listener {
		
		private Map<Integer, Participant> slots = new HashMap<>();
		private final Inventory gui = Bukkit.createInventory(null, 54, "§0대상을 선택해주세요.");
		
		public SkillGUI() {
			super(TaskType.REVERSE, 600);
			setPeriod(TimeUnit.TICKS, 1);
			Bukkit.getPluginManager().registerEvents(this, AbilityWar.getPlugin());
			openGUI();
		}
		
		private void openGUI() {
	    	gui.clear();
	    	int slot = 0;
	    	for (Participant participant : getGame().getParticipants()) {
	    		if (!((MurderMystery) getGame()).isDead(participant.getPlayer().getUniqueId()) && !participant.equals(getParticipant())) {
	    			ItemStack item = Skulls.createCustomSkull(participant.getPlayer().getName());
					ItemMeta meta = item.getItemMeta();
					meta.setDisplayName("§a" + participant.getPlayer().getName());
					List<String> lore = new ArrayList<>();
					lore.add("§7");
					lore.add("§6» §e이 대상을 감지 대상으로 지정하려면 선택하세요.");
					meta.setLore(lore);
					item.setItemMeta(meta);
	    			gui.setItem(slot, item);
					slots.put(slot, participant);
	    			slot++;
	    		}
	    	}
	        getPlayer().openInventory(gui);
	    }
		
	    @EventHandler()
	    public void onInventoryClick(InventoryClickEvent e) {
	    	if (e.getClickedInventory().equals(gui)) {
	    		e.setCancelled(true);
	    		if (slots.containsKey(e.getSlot())) {
	    			selected = slots.get(e.getSlot());
		    		stop(true);	
	    		}
	    	}
	    }
	    
	    @EventHandler()
	    public void onInventoryClose(InventoryCloseEvent e) {
	    	if (e.getInventory().equals(gui)) {
	    		stop(false);
	    	}
	    }
	    
		@Override
		protected void onEnd() {
			onSilentEnd();
		}
		
		@Override
		protected void onSilentEnd() {
			if (selected != null) {
				MurderMystery murderMystery = (MurderMystery) getGame();
				if (!murderMystery.isDead(selected.getPlayer().getUniqueId())) {
					if (murderMystery.consumeGold(getParticipant(), 3)) {
						getPlayer().sendMessage("§4[§c!§4] §3" + selected.getPlayer().getName() + "§f님을 §e감지§f 대상으로 지정하였습니다.");	
					} else {
						getPlayer().sendMessage("§4[§c!§4] §c금이 부족합니다.");	
						selected = null;
					}
				} else {
					getPlayer().sendMessage("§4[§c!§4] §c이미 죽은 대상을 감지할 순 없습니다.");
					selected = null;
				}
			}
			HandlerList.unregisterAll(this);
			getPlayer().closeInventory();
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