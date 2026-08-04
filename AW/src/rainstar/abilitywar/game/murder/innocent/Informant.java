package rainstar.abilitywar.game.murder.innocent;

import daybreak.abilitywar.AbilityWar;
import daybreak.abilitywar.ability.AbilityFactory;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.SubscribeEvent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import daybreak.abilitywar.ability.AbilityFactory.AbilityRegistration;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.list.murdermystery.Items;
import daybreak.abilitywar.game.list.murdermystery.JobList;
import daybreak.abilitywar.game.list.murdermystery.MurderMystery;
import daybreak.abilitywar.game.list.murdermystery.ability.AbstractInnocent;
import daybreak.abilitywar.game.list.murdermystery.ability.AbstractMurderer;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.base.minecraft.item.Skulls;
import daybreak.abilitywar.utils.base.minecraft.nms.NMS;
import daybreak.abilitywar.utils.base.random.Random;
import daybreak.abilitywar.utils.library.SoundLib;
import rainstar.abilitywar.game.murder.murderer.ScammerMurderer;
import rainstar.abilitywar.game.murder.system.config.statictype.CheckMurdererSword;

@AbilityManifest(name = "시민: 정보상", rank = Rank.SPECIAL, species = Species.HUMAN, explain = {
		"금 우클릭으로 금 8개를 소모해 활과 화살을 얻을 수 있습니다.",
		"§8[§b엠바고§8]§f 머더가 검을 얻고 15초가 지나기 전까지 §8[§c첩보§8]§f를 사용할 수 없습니다.",
		"§8[§c첩보§8]§f 금 좌클릭으로 금 18개를 소모해 대상 당 한 번 선택 가능합니다.",
		"선택된 대상의 §a진짜 직업§f과 §c가짜 직업§f을 하나씩 알아냅니다.",
		"§7§n무엇이 진실인지는 모릅니다.§f",
		"정보는 무조건 하나의 §a시민 직업§f, 하나의 §c머더 직업§f으로만 나옵니다."
})		
public class Informant extends AbstractInnocent {
	
	public Informant(Participant participant) {
		super(participant);
	}
	
	@Override
	protected void onUpdate(Update update) {
		super.onUpdate(update);
		if (update == Update.RESTRICTION_CLEAR) {
			NMS.sendTitle(getPlayer(), "§e역할§f: §a정보상", "§f모든 사람의 정보를 캐내세요.", 10, 80, 10);
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
	
	private Random random = new Random();
	private Set<Participant> usedPlayerSet = new HashSet<>();
	private CheckMurdererSword checkmurderersword = getGame().getModule(CheckMurdererSword.class);
	
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
				if (checkmurderersword.isCheckable()) {
					new SkillGUI().start();
				} else getPlayer().sendMessage("§4[§c!§4] §c아직은 첩보가 없습니다.");	
			}
		}
	}
	
	public class SkillGUI extends AbilityTimer implements Listener {
		
		private Participant selected;
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
	    		if (!((MurderMystery) getGame()).isDead(participant.getPlayer().getUniqueId()) && !participant.equals(getParticipant()) && !usedPlayerSet.contains(participant)) {
	    			ItemStack item = Skulls.createCustomSkull(participant.getPlayer().getName());
					ItemMeta meta = item.getItemMeta();
					meta.setDisplayName("§a" + participant.getPlayer().getName());
					List<String> lore = new ArrayList<>();
					lore.add("§7");
					lore.add("§5» §b이 대상의 정보를 캐내려면 선택하세요.");
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
				if (!usedPlayerSet.contains(selected)) {
					MurderMystery murderMystery = (MurderMystery) getGame();
					if (!murderMystery.isDead(selected.getPlayer().getUniqueId())) {
						if (murderMystery.consumeGold(getParticipant(), 18)) {
							SoundLib.BLOCK_ENCHANTMENT_TABLE_USE.playSound(getPlayer(), 1, 1.25f);
							String realability = "";
							String fakeability = "";
							
					    	List<String> jobnames = new ArrayList<>();
					    	List<String> innocent_jobnames = new ArrayList<>();
					    	List<String> murderer_jobnames = new ArrayList<>();
					    	for (AbilityRegistration registration : JobList.values()) {
					    		jobnames.add(registration.getManifest().name());
					    	}
					    	jobnames.add("머더: 부패 탐정");
					    	jobnames.add("머더: 광신도");
					    	jobnames.add("시민: 개구리");
					    	jobnames.remove("머더: 사기꾼");
					    	Collections.shuffle(jobnames);
					       	for (String jobname : jobnames) {
					       		AbilityRegistration registration = AbilityFactory.getByName(jobname);
					       		if (AbstractInnocent.class.isAssignableFrom(registration.getAbilityClass())) innocent_jobnames.add(jobname);
					    		else if (AbstractMurderer.class.isAssignableFrom(registration.getAbilityClass())) murderer_jobnames.add(jobname);
					       	}
							if (selected.getAbility() instanceof AbstractMurderer) {
								if (selected.getAbility().getClass().equals(ScammerMurderer.class)) {
									realability = "§a시민: 광대"; 
									fakeability = "§c" + murderer_jobnames.get(0);
								} else {
									realability = "§c" + selected.getAbility().getName();
									fakeability = "§a" + innocent_jobnames.get(0);	
								}
							} else {
								realability = "§a" + selected.getAbility().getName();
								fakeability = "§c" + murderer_jobnames.get(0);
							}
							
							if (random.nextBoolean()) {
								getPlayer().sendMessage("§8[§c첩보§8] §b" + selected.getPlayer().getName() + "§f님의 직업은 " + realability + " §f혹은 " + fakeability + "§f입니다.");
							} else {
								getPlayer().sendMessage("§8[§c첩보§8] §b" + selected.getPlayer().getName() + "§f님의 직업은 " + fakeability + " §f혹은 " + realability + "§f입니다.");
							}
							
							usedPlayerSet.add(selected);
						} else getPlayer().sendMessage("§4[§c!§4] §c금이 부족합니다.");	
					} else getPlayer().sendMessage("§4[§c!§4] §c이미 죽은 대상의 정보는 알아낼 수 없습니다.");		
				} else getPlayer().sendMessage("§4[§c!§4] §c이 대상의 정보는 이미 알아냈습니다.");		
			}
			HandlerList.unregisterAll(this);
			getPlayer().closeInventory();
		}
		
	}

}
