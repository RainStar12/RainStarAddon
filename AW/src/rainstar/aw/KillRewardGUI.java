package rainstar.aw;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import daybreak.abilitywar.utils.base.io.FileUtil;
import daybreak.abilitywar.utils.base.minecraft.item.builder.ItemBuilder;
import daybreak.abilitywar.utils.library.MaterialX;
import daybreak.abilitywar.utils.library.SoundLib;

@SuppressWarnings("serial")
public class KillRewardGUI implements Listener {
	
	private static final int[] GLASS_PANES = {1, 3, 4, 5, 6, 7};
	private static final int[] GRAY_GLASS_PANES = {9, 10, 11, 12, 13, 14, 15, 16, 17};
	
    private static final ItemStack GLASS_PANE = new ItemBuilder(MaterialX.LIGHT_GRAY_STAINED_GLASS_PANE)
            .displayName(ChatColor.WHITE.toString())
            .build();
	
    private static final ItemStack GRAY_GLASS_PANE = new ItemBuilder(MaterialX.GRAY_STAINED_GLASS_PANE)
            .displayName(ChatColor.WHITE.toString())
            .build();
    
    private static final ItemStack RUNE_BAG = new ItemBuilder(MaterialX.ENDER_CHEST)
            .displayName("§d룬 가방")
            .lore(new ArrayList<String>() {
            	{
                    add("§7룬들이 보관되어 있습니다.");
                    add("§7업데이트를 기대해주세요.");
            	}
            })
            .build();
	
	public KillRewardGUI(Player opener, Plugin plugin) {
        Bukkit.getPluginManager().registerEvents(this, plugin);
        this.player = opener;
        openGUI();
	}
	
	public static Status status = Status.DISABLE;
	public static Type type = Type.ALL;
	private final Player player;
	private final YamlConfiguration rewardconfig = YamlConfiguration.loadConfiguration(FileUtil.newFile("killreward.txt"));
    private Inventory gui = Bukkit.createInventory(null, 54, "§c킬 보상 설정");
    private static List<ItemStack> itemstacks = new ArrayList<>();
    private boolean openguicalled = false;
	
    @SuppressWarnings("unchecked")
	private void openGUI() {
    	openguicalled = true;
    	if (rewardconfig.get("활성화 여부") != null) status = Status.valueOf(rewardconfig.getString("활성화 여부"));
    	if (rewardconfig.get("지급 방법") != null) type = Type.valueOf(rewardconfig.getString("지급 방법"));
    	if (rewardconfig.get("킬 보상 아이템") != null) itemstacks = (List<ItemStack>) rewardconfig.get("킬 보상 아이템");
    	gui.clear();
        gui.setItem(0, status.getItem());
        gui.setItem(2, type.getItem());
        gui.setItem(8, RUNE_BAG);
        for (int i : GLASS_PANES) {
            gui.setItem(i, GLASS_PANE);
        }
        for (int i : GRAY_GLASS_PANES) {
            gui.setItem(i, GRAY_GLASS_PANE);
        }
        if (!itemstacks.isEmpty()) {
        	for (int a = 18; a < getItems().size(); a++) {
        		gui.setItem(a, getItems().get(a));
        	}
        }
        player.getPlayer().openInventory(gui);
        openguicalled = false;
    }
    
    public ItemStack getItem(int slot) {
    	return itemstacks.get(slot);
    }
    
	public static List<ItemStack> getItems() {
		return itemstacks;
	}
	
    @EventHandler()
    public void onInventoryClick(InventoryClickEvent e) {
    	if (e.getClickedInventory().equals(gui)) {
        	if (e.getSlot() >= 0 && e.getSlot() <= 17) {
            	if (e.getSlot() == 0) {
            		SoundLib.UI_BUTTON_CLICK.playSound(player);
            		status = status.flip();
            	}
            	if (e.getSlot() == 2) {
            		SoundLib.UI_BUTTON_CLICK.playSound(player);
            		type = type.next();
            	}
            	if (e.getSlot() == 8) {
            		SoundLib.BLOCK_ENDER_CHEST_OPEN.playSound(player, 1, 1.25f);
            	}
        		e.setCancelled(true);
            	save();
            	openGUI();
        	}	
    	}
    }
    
    public void save() {
        itemstacks.clear();
        for (int a = 18; a < 53; a++) {
        	if (gui.getItem(a) != null) itemstacks.add(gui.getItem(a));
        }
		rewardconfig.set("활성화 여부", status.name());
		rewardconfig.set("지급 방법", type.name());
		rewardconfig.set("킬 보상 아이템", itemstacks);
		try {
			rewardconfig.save(FileUtil.newFile("killreward.txt"));
		} catch (IOException e1) {
			e1.printStackTrace();
		}
    }
    
    @EventHandler()
    public void onInventoryClose(InventoryCloseEvent e) {
    	if (e.getInventory().equals(gui) && !openguicalled) {
        	save();
        	HandlerList.unregisterAll(this);	
    	}
    }
    
    enum Type {  	
    	ALL("전체", new ItemBuilder(MaterialX.CHEST)
                .displayName("§a보상 지급 방법")
                .lore(new ArrayList<String>() {
                	{
                        add("§7보상을 어떻게 지급할 지 결정합니다.");
                        add("§7현재: §a전체 지급");
                	}
                })
                .build()) {
    		@Override
    		public Type next() {
    			return Type.RANDOM;
    		}
    	},
    	RANDOM("무작위", new ItemBuilder(MaterialX.DISPENSER)
                .displayName("§a보상 지급 방법")
                .lore(new ArrayList<String>() {
                	{
                        add("§7보상을 어떻게 지급할 지 결정합니다.");
                        add("§7현재: §c무작위 하나 지급");
                	}
                })
                .build()) {
    		@Override
    		public Type next() {
    			return Type.SELECT;
    		}
    	},
    	SELECT("선택", new ItemBuilder(MaterialX.JUKEBOX)
                .displayName("§a보상 지급 방법")
                .lore(new ArrayList<String>() {
                	{
                        add("§7보상을 어떻게 지급할 지 결정합니다.");
                        add("§7현재: §b전체 보상 중 택 1");
                	}
                })
                .build()) {
    		@Override
    		public Type next() {
    			return Type.ALL;
    		}
    	};
    	
    	private final String name;
    	private final ItemStack item;
    	
    	Type(String name, ItemStack item) {
    		this.name = name;
    		this.item = item;
    	}
    	
		public String getName() {
			return name;
		}
		
		public ItemStack getItem() {
			return item;
		}
		
		public abstract Type next();
		
    }
    
    enum Status { 	
    	ENABLE("활성화", new ItemBuilder(MaterialX.GREEN_WOOL)
                .displayName("§b킬 보상§7: §a켜짐")
                .build()) {
    		@Override
    		public Status flip() {
    			return Status.DISABLE;
    		}
    	},
    	DISABLE("비활성화", new ItemBuilder(MaterialX.RED_WOOL)
                .displayName("§b킬 보상§7: §c꺼짐")
                .build()) {
    		@Override
    		public Status flip() {
    			return Status.ENABLE;
    		}
    	};
    	
    	private final String name;
    	private final ItemStack item;
    	
    	Status(String name, ItemStack item) {
    		this.name = name;
    		this.item = item;
    	}
    	
		public String getName() {
			return name;
		}
		
		public ItemStack getItem() {
			return item;
		}
		
		public abstract Status flip();
		
    }
	
}
