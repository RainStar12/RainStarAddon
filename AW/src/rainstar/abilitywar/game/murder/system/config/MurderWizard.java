package rainstar.abilitywar.game.murder.system.config;

import daybreak.abilitywar.utils.base.minecraft.item.builder.ItemBuilder;
import daybreak.abilitywar.utils.library.MaterialX;
import daybreak.abilitywar.utils.library.SoundLib;
import rainstar.abilitywar.system.config.RainStarConfig;
import rainstar.abilitywar.system.config.RainStarConfig.ConfigOption;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MurderWizard {

	private final Plugin plugin;
    private final Player player;
    private Inventory gui;
	private Listener listener;

	private static final ItemStack DECO = new ItemBuilder(MaterialX.WHITE_STAINED_GLASS_PANE).displayName("§f").build();
    private final ItemStack DOUBLE_MURDER = new ItemBuilder(MaterialX.IRON_SWORD).displayName("§c더블 머더").build();
	private final ItemStack GOLD_RUSH = new ItemBuilder(MaterialX.GOLD_INGOT).displayName("§e골드 러쉬").build();
	private final ItemStack BLACK_COMPASS = new ItemBuilder(MaterialX.COMPASS).displayName("§8블랙 나침반 고장").build();
	private final ItemStack HEAD_COUNT = new ItemBuilder(MaterialX.PLAYER_HEAD).displayName("§a인원 수 밸런스").build();
	private final ItemStack NO_KILL = new ItemBuilder(MaterialX.BARRIER).displayName("§f킬 금지").build();
	private final ItemStack HOMING = new ItemBuilder(MaterialX.BOW).displayName("§5탐정 유도").build();
	private final ItemStack TIMER = new ItemBuilder(MaterialX.CLOCK).displayName("§b머더 타이머").build();

    public MurderWizard(Player player, Plugin plugin) {
        this.player = player;
        this.plugin = plugin;
    }

    public void show() {
		if (gui == null) {
			gui = Bukkit.createInventory(null, 27, "§4§l머더 진행 설정");
			registerListener();
		}

		refresh();
		player.openInventory(gui);
    }
    
	private void refresh() {
		gui.clear();
		openGUI(gui);
	}
    
    private void registerListener() {
		listener = new Listener() {

			@EventHandler
			public void onInventoryClick(InventoryClickEvent e) {
				if (!e.getInventory().equals(gui)) return;
				onClick(e);
			}

			@EventHandler
			public void onInventoryClose(InventoryCloseEvent e) {
				if (!e.getInventory().equals(gui)) return;

				HandlerList.unregisterAll(listener);
				gui = null;
			}
		};

		Bukkit.getPluginManager().registerEvents(listener, plugin);
    }
    
	private void openGUI(Inventory gui) {

		for (int i = 0; i < gui.getSize(); i++) {
			gui.setItem(i, DECO);
		}

		gui.setItem(10, createBooleanItem(DOUBLE_MURDER, ConfigOption.DOUBLE_MURDER));
		gui.setItem(11, createBooleanItem(GOLD_RUSH, ConfigOption.GOLD_RUSH));
		gui.setItem(12, createBooleanItem(BLACK_COMPASS, ConfigOption.BLACK_COMPASS));
		gui.setItem(13, createBooleanItem(HEAD_COUNT, ConfigOption.HEAD_COUNT_BALANCE));
		gui.setItem(14, createBooleanItem(HOMING, ConfigOption.DETECTIVE_HOMING));
		gui.setItem(15, createValueItem(NO_KILL, ConfigOption.NO_KILL, ConfigOption.NO_KILL_TIME));
		gui.setItem(16, createValueItem(TIMER, ConfigOption.MURDER_TIMER, ConfigOption.MURDER_TIMER_TIME));
	}

	private void onClick(InventoryClickEvent e) {
		if (e.getInventory().equals(gui) && e.getWhoClicked().equals(player)) {
			e.setCancelled(true);			
			SoundLib.UI_BUTTON_CLICK.playSound(player);
			switch (e.getSlot()) {
			case 10:
				toggle(ConfigOption.DOUBLE_MURDER);
				break;
				
			case 11:
				toggle(ConfigOption.GOLD_RUSH);
				break;
		
			case 12:
				toggle(ConfigOption.BLACK_COMPASS);
				break;
		
			case 13:
				toggle(ConfigOption.HEAD_COUNT_BALANCE);
				break;
		
			case 14:
				toggle(ConfigOption.DETECTIVE_HOMING);
				break;
				
			case 15:
				handleValueOption(e.getClick(), ConfigOption.NO_KILL, ConfigOption.NO_KILL_TIME, 1, 0);
				break;
		
			case 16:
				handleValueOption(e.getClick(), ConfigOption.MURDER_TIMER, ConfigOption.MURDER_TIMER_TIME, 10, 10);
				break;
			}

			refresh();	
		}
	}
    
    private ItemStack createBooleanItem(ItemStack item, ConfigOption option) {
    	ItemStack clone = item.clone();
        ItemMeta meta = clone.getItemMeta();
        boolean value = RainStarConfig.getBoolean(option);

        List<String> lore = new ArrayList<>();

        lore.add("");
        lore.addAll(Arrays.asList(option.getDescription()));
        lore.add("");

        lore.add("§7상태 : " + (value ? "§a활성화" : "§c비활성화"));

        lore.add("");
        lore.add("§a클릭§f으로 변경");

        meta.setLore(lore);
        clone.setItemMeta(meta);

        return clone;
    }
    
    private ItemStack createValueItem(ItemStack item, ConfigOption enable, ConfigOption valueOption) {

    	ItemStack clone = item.clone();

    	ItemMeta meta = clone.getItemMeta();

    	boolean enabled = RainStarConfig.getBoolean(enable);
    	int value = RainStarConfig.getInt(valueOption);
    	
    	List<String> lore = new ArrayList<>();

    	lore.add("");
    	lore.addAll(Arrays.asList(enable.getDescription()));
    	lore.add("");

    	lore.add("§7상태 : " + (enabled ? "§a활성화" : "§c비활성화"));
    	lore.add("§7수치 : §f" + value);

    	lore.add("");
    	lore.add("§a좌클릭§f 증가, §c우클릭§f 감소");
    	lore.add("§b쉬프트+좌클릭§f으로 활성화/비활성화");

    	meta.setLore(lore);

    	clone.setItemMeta(meta);

    	return clone;
    }
    
    private void toggle(ConfigOption option) {
        boolean value = RainStarConfig.getBoolean(option);
        RainStarConfig.set(option, !value);
    }
    
    private void handleValueOption(ClickType click, ConfigOption enableOption, ConfigOption valueOption, int amount, int min) {

    	boolean enabled = RainStarConfig.getBoolean(enableOption);

    	if (click == ClickType.SHIFT_LEFT) {
    		RainStarConfig.set(enableOption, !enabled);
    		return;
    	}

    	if (click.isLeftClick()) {
    		if (!enabled) {
    			RainStarConfig.set(enableOption, true);
    		} else {
    			int value = RainStarConfig.getInt(valueOption);
    			RainStarConfig.set(valueOption, value + amount);
    		}

    	} else if (click.isRightClick()) {

    		int value = RainStarConfig.getInt(valueOption);
    		value -= amount;

    		if (value <= min) {
    			value = min;
    		}

    		RainStarConfig.set(valueOption, value);
    	}
    }

}