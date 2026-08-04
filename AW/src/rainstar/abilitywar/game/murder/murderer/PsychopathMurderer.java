package rainstar.abilitywar.game.murder.murderer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import daybreak.abilitywar.AbilityWar;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.SubscribeEvent;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.AbstractGame.Participant.ActionbarNotification.ActionbarChannel;
import daybreak.abilitywar.game.list.murdermystery.Items;
import daybreak.abilitywar.game.list.murdermystery.MurderMystery;
import daybreak.abilitywar.game.list.murdermystery.MurderMystery.ArrowKillEvent;
import daybreak.abilitywar.game.list.murdermystery.ability.AbstractMurderer;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.base.minecraft.item.Skulls;
import daybreak.abilitywar.utils.base.minecraft.nms.NMS;
import daybreak.abilitywar.utils.library.ParticleLib;

@AbilityManifest(name = "머더: 사이코패스", rank = Rank.SPECIAL, species = Species.HUMAN, explain = {
		"모든 시민을 죽이세요!",
		"금 우클릭으로 금 5개를 소모해 활과 화살을 얻을 수 있습니다.",
		"§8[§a암약§8]§f 화살 공격을 §a받지 않고, 선량한 시민§f으로 안내됩니다.",
		"다른 사람을 해할 수 없습니다.",
		"§8[§c돌변§8]§f 금 F키로 금 소모 없이 GUI를 통해 임의의 대상을 즉시",
		"§4처형§f할 수 있습니다. §8(§71회용§8)§f. 이후, §8[§a암약§8]§f 효과가 비활성화됩니다."
})
public class PsychopathMurderer extends AbstractMurderer {

	public PsychopathMurderer(Participant participant) {
		super(participant);
	}
	
	@Override
	protected void onUpdate(Update update) {
		super.onUpdate(update);
		if (update == Update.RESTRICTION_CLEAR) {
			NMS.sendTitle(getPlayer(), "§e직업§f: §a사이코패스", "§f중요한 §a시민§f을 §4처형§f하고 본성을 드러내세요.", 10, 80, 10);
			new AbilityTimer(1) {
				@Override
				protected void onEnd() {
					NMS.clearTitle(getPlayer());
				}
			}.setInitialDelay(TimeUnit.SECONDS, 5).start();
			ac.update("§c살인 불가");
		} else if (update == Update.ABILITY_DESTROY) {
			NMS.setInvisible(getPlayer(), false);
		}
	}
	
	private boolean onetime = true;
	private ActionbarChannel ac = newActionbarChannel();
	
	@SubscribeEvent(priority = 1, eventPriority = EventPriority.LOWEST)
	private void onArrowKill(final ArrowKillEvent e) {
		if (onetime) {
			if (e.getTarget().equals(getParticipant()) || e.getShooter().equals(getParticipant())) {
				e.setCancelled(true);
				if (e.getTarget().equals(getParticipant())) {
					ParticleLib.VILLAGER_HAPPY.spawnParticle(getPlayer().getLocation(), 1, 2, 1, 10, 0);
					e.getShooter().getPlayer().sendMessage("§4[§c!§4] §a선량한 시민§f은 활로 쏠 수 없습니다.");	
				} else if (e.getShooter().equals(getParticipant())) getPlayer().sendMessage("§5[§4!§5]§f §4처형§c하기 전까진 살인할 수 없습니다.");
			}	
		}
	}
	
	@SubscribeEvent(priority = 1, eventPriority = EventPriority.LOWEST)
	private void onMurder(MurderEvent e) {
		if (onetime && getParticipant().equals(e.getMurderer().getParticipant())) {
			e.setCancelled(true);
			getPlayer().sendMessage("§5[§4!§5]§f §4처형§c하기 전까진 살인할 수 없습니다.");
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
	
    @SubscribeEvent(onlyRelevant = true, priority = 0)
    public void onPlayerSwapHandItems(PlayerSwapHandItemsEvent e) {
    	if (Items.isGold(e.getOffHandItem()) && e.getPlayer().equals(getPlayer())) {
    		if (onetime) new SkillGUI().start();
    	}
    }
	
	@SubscribeEvent
	public void onProjectileLaunch(ProjectileLaunchEvent e) {
		if (getPlayer().equals(e.getEntity().getShooter()) && NMS.isArrow(e.getEntity()) && onetime) {
			e.getEntity().setVelocity(e.getEntity().getVelocity().multiply(0.75));
		}
	}
	
	public class SkillGUI extends AbilityTimer implements Listener {
		
		private Map<Integer, Participant> slots = new HashMap<>();
		private final Inventory gui = Bukkit.createInventory(null, 54, "§0대상을 선택해주세요.");
		private Participant selected;
		
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
	    		if (!((MurderMystery) getGame()).isDead(participant.getPlayer().getUniqueId()) && !participant.equals(getParticipant())
	    				&& !(participant.getAbility() instanceof AbstractMurderer)) {
	    			ItemStack item = Skulls.createCustomSkull(participant.getPlayer().getName());
					ItemMeta meta = item.getItemMeta();
					meta.setDisplayName("§a" + participant.getPlayer().getName());
					List<String> lore = new ArrayList<>();
					lore.add("§7");
					lore.add("§5» §c이 대상을 §4처형§c하려면 선택하세요.");
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
	    	if (gui != null) {
		    	if (gui.equals(e.getClickedInventory())) {
		    		e.setCancelled(true);
		    		if (slots.containsKey(e.getSlot())) {
		    			selected = slots.get(e.getSlot());
			    		stop(true);
		    		}
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
					getPlayer().sendMessage("§4[§c!§4] §3" + selected.getPlayer().getName() + "§f님을 §4처형§f합니다.");
					MurderEvent event = new MurderEvent(PsychopathMurderer.this, selected);
					Bukkit.getPluginManager().callEvent(event);
					event.setCancelled(true);
					selected.getPlayer().setHealth(0);
					ac.update("§a살인 가능");
					onetime = false;
				} else getPlayer().sendMessage("§4[§c!§4] §c이미 죽은 대상을 §4처형§f할 순 없습니다.");
				selected = null;
			}
			HandlerList.unregisterAll(this);
			getPlayer().closeInventory();
		}
		
	}
	
}
