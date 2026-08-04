package rainstar.abilitywar.game.murder.innocent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import daybreak.abilitywar.AbilityWar;
import daybreak.abilitywar.ability.AbilityFactory;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.SubscribeEvent;
import daybreak.abilitywar.ability.AbilityFactory.AbilityRegistration;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.list.murdermystery.Items;
import daybreak.abilitywar.game.list.murdermystery.JobList;
import daybreak.abilitywar.game.list.murdermystery.MurderMystery;
import daybreak.abilitywar.game.list.murdermystery.ability.AbstractInnocent;
import daybreak.abilitywar.game.list.murdermystery.ability.AbstractMurderer;
import daybreak.abilitywar.game.list.murdermystery.ability.Detective;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.base.minecraft.item.builder.ItemBuilder;
import daybreak.abilitywar.utils.base.minecraft.nms.NMS;
import daybreak.abilitywar.utils.library.MaterialX;
import daybreak.abilitywar.utils.library.SoundLib;
import rainstar.abilitywar.game.murder.murderer.CorruptionDetectiveMurderer;
import rainstar.abilitywar.game.murder.murderer.ScammerMurderer;

@AbilityManifest(name = "시민: 공무원", rank = Rank.SPECIAL, species = Species.HUMAN, explain = {
		"금 우클릭으로 금 8개를 소모해 활과 화살을 얻을 수 있습니다.",
		"§8[§a신원 조회§8]§f 금 좌클릭으로 GUI를 열어 전체 직업 중",
		"하나를 선택해 현재 해당 직업 종사자가 몇 명인지 알 수 있습니다.",
		"§5머더 직업§f은 금 1개, §3탐정§f은 금 2개, §a시민 직업§f은 금 8개를 소모합니다.",
		"§8[§b공문서§8]§f §3탐정§f을 조사한다면 누가 §3탐정§f인지 알 수 있습니다."
})
public class Official extends AbstractInnocent {

	public Official(Participant participant) {
		super(participant);
	}
	
	@Override
	protected void onUpdate(Update update) {
		super.onUpdate(update);
		if (update == Update.RESTRICTION_CLEAR) {
			NMS.sendTitle(getPlayer(), "§e역할§f: §a공무원", "§f모든 직업을 조사하세요!", 10, 80, 10);
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
				new SkillGUI().start();
			}
		}
	}
	
	public class SkillGUI extends AbilityTimer implements Listener {
		
		private Map<Integer, AbilityRegistration> slots = new HashMap<>();
		private AbilityRegistration selected;
		private final Inventory gui = Bukkit.createInventory(null, 54, "§0직업을 선택해주세요.");
		private int currentPage = 1;
		
		private final ItemStack PREVIOUS_PAGE = new ItemBuilder(MaterialX.ARROW)
				.displayName(ChatColor.AQUA + "이전 페이지")
				.build();

		private final ItemStack NEXT_PAGE = new ItemBuilder(MaterialX.ARROW)
				.displayName(ChatColor.AQUA + "다음 페이지")
				.build();
		
		private final ItemStack NULL = (new ItemBuilder(MaterialX.GRAY_STAINED_GLASS_PANE)).displayName(" ").build();
		
		public SkillGUI() {
			super(TaskType.REVERSE, 600);
			setPeriod(TimeUnit.TICKS, 1);
			Bukkit.getPluginManager().registerEvents(this, AbilityWar.getPlugin());
			selected = null;
			openGUI(1);
		}
		
		private void openGUI(int page) {
	    	gui.clear();
	    	slots.clear();
	    	
	    	List<String> jobnames = new ArrayList<>();
	    	for (AbilityRegistration registration : JobList.values()) {
	    		jobnames.add(registration.getManifest().name());
	    	}
	    	jobnames.add("머더: 부패 탐정");
	    	jobnames.add("머더: 광신도");
	    	jobnames.add("시민: 개구리");
	    	Collections.sort(jobnames);
	    	
	    	int maxPage = ((jobnames.size() - 1) / 36) + 1;
			if (maxPage < page) page = 1;
			if (page < 1) page = 1;
	    	
	    	currentPage = page;
	    	int count = 0;
			
	       	for (String jobname : jobnames) {
	       		AbilityRegistration registration = AbilityFactory.getByName(jobname);
	    		if (!registration.getAbilityClass().equals(ScammerMurderer.class)) {
	    			if (count / 36 == page - 1) {
	    				String color = "";
			    		if (AbstractMurderer.class.isAssignableFrom(registration.getAbilityClass())) color = "§c";
			    		else if (registration.getAbilityClass().getSimpleName().contains("Detective")) color = "§3";
			    		else if (AbstractInnocent.class.isAssignableFrom(registration.getAbilityClass())) color = "§a";
						ItemStack item = new ItemBuilder(MaterialX.IRON_BLOCK).build();
						ItemMeta meta = item.getItemMeta();
						meta.setDisplayName(color + registration.getManifest().name());
						List<String> lore = new ArrayList<>();
						lore.add("§7");
						lore.add("§b» §a이 직업의 종사자 수를 알아보려면 선택하세요.");
						meta.setLore(lore);
						item.setItemMeta(meta);
		    			gui.setItem(count % 36, item);
						slots.put(count % 36, registration);		
	    			}
	    			count++;
	    		}
	    	}
	       	
	     	if (page > 1) {
				gui.setItem(48, PREVIOUS_PAGE);
			}

			if (page != maxPage) {
				gui.setItem(50, NEXT_PAGE);
			}
			
			for (int a = 36; a < 45; a++) {
				gui.setItem(a, NULL);
			}
	       	
			ItemStack stack = new ItemStack(Material.PAPER, 1);
			ItemMeta meta = stack.getItemMeta();
			meta.setDisplayName("§6페이지 §e" + page + " §6/ §e" + maxPage);
			stack.setItemMeta(meta);
			gui.setItem(49, stack);
	        getPlayer().openInventory(gui);
	    }
		
	    @EventHandler()
	    public void onInventoryClick(InventoryClickEvent e) {
	    	if (e.getClickedInventory() != null && e.getClickedInventory().equals(gui)) {
	    		e.setCancelled(true);
	    		if (e.getCurrentItem() != null) {
		    		if (slots.containsKey(e.getSlot())) {
		    			selected = slots.get(e.getSlot());
			    		getPlayer().closeInventory();
			    		stop(false);
		    		} else if (e.getCurrentItem().getItemMeta() != null && e.getCurrentItem().getItemMeta().getDisplayName().equals(ChatColor.AQUA + "이전 페이지")) {
						openGUI(currentPage - 1);
					} else if (e.getCurrentItem().getItemMeta() != null && e.getCurrentItem().getItemMeta().getDisplayName().equals(ChatColor.AQUA + "다음 페이지")) {
						openGUI(currentPage + 1);
					}	
	    		}
	    	}
	    }
	    
		@Override
		protected void onEnd() {
			onSilentEnd();
		}
		
		@Override
		protected void onSilentEnd() {
			final MurderMystery murderMystery = (MurderMystery) getGame();
			if (selected != null) {
				int cost = 1;
	    		if (AbstractInnocent.class.isAssignableFrom(selected.getAbilityClass())) cost = 8;
	    		if (AbstractMurderer.class.isAssignableFrom(selected.getAbilityClass())) cost = 1;
	    		if (selected.getAbilityClass().getSimpleName().contains("Detective") && !selected.getAbilityClass().equals(CorruptionDetectiveMurderer.class)) cost = 2;
				if (murderMystery.consumeGold(getParticipant(), cost)) {
					List<Participant> detectives = new ArrayList<>();
					int stack = 0;
		    		String color = "";
		    		if (AbstractInnocent.class.isAssignableFrom(selected.getAbilityClass())) color = "§a";
		    		if (AbstractMurderer.class.isAssignableFrom(selected.getAbilityClass())) color = "§c";
		    		if (selected.getAbilityClass().getSimpleName().contains("Detective") && !selected.getAbilityClass().equals(CorruptionDetectiveMurderer.class)) color = "§3";
					for (Participant participant : getGame().getParticipants()) {
						if (participant.hasAbility()) {
							if (selected.getAbilityClass().equals(Detective.class) && participant.getAbility().getRegistration().getAbilityClass().getSimpleName().contains("Detective") && !participant.getAbility().getRegistration().getAbilityClass().equals(CorruptionDetectiveMurderer.class)) {
								detectives.add(participant);
							}
							if (selected.equals(participant.getAbility().getRegistration())) {
								stack++;
							}
							
							if (selected.getAbilityClass().equals(Clown.class) && participant.getAbility().getClass().equals(ScammerMurderer.class)) {
								stack++;
							}
						}
					}
					if (selected.getAbilityClass().getSimpleName().contains("Detective") && !selected.getAbilityClass().equals(CorruptionDetectiveMurderer.class)) {
						final StringJoiner joiner = new StringJoiner("§f, ");
						for (Participant participant : detectives) {
							joiner.add("§3" + participant.getPlayer().getName() + "§f님");
						}
						getPlayer().sendMessage("§4[§c!§4] §f현재 §3탐정§f은 " + joiner.toString() + "§f입니다.");
					} else getPlayer().sendMessage("§4[§c!§4] §f조사하신 직업 §8<" + color + selected.getManifest().name() + "§8>§f에 현재 종사하고 있는 사람은 §b" + stack + "명§f입니다.");	
					SoundLib.ENTITY_BAT_LOOP.playSound(getPlayer(), 1f, 2f);
				} else getPlayer().sendMessage("§4[§c!§4] §c금이 부족합니다.");
			}
			HandlerList.unregisterAll(this);
			getPlayer().closeInventory();
			selected = null;
		}
		
	}
	
}
