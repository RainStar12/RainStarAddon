package rainstar.aw;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import daybreak.abilitywar.utils.base.io.FileUtil;
import daybreak.abilitywar.utils.base.minecraft.item.builder.ItemBuilder;
import daybreak.abilitywar.utils.library.MaterialX;

@SuppressWarnings("serial")
public class KillRewardGUI implements Listener {
	
	private static final int[] GLASS_PANES = {1, 2, 3, 4, 5, 6, 7};
	private static final int[] GRAY_GLASS_PANES = {9, 10, 11, 12, 13, 14, 15, 16, 17};
	
    private static final ItemStack GLASS_PANE = new ItemBuilder(MaterialX.WHITE_STAINED_GLASS_PANE)
            .displayName(ChatColor.WHITE.toString())
            .build();
	
    private static final ItemStack GRAY_GLASS_PANE = new ItemBuilder(MaterialX.GRAY_STAINED_GLASS_PANE)
            .displayName(ChatColor.WHITE.toString())
            .build();
    
    private static final ItemStack REWARD_ALL = new ItemBuilder(MaterialX.CHEST)
            .displayName("§a보상 지급 방법")
            .lore(new ArrayList<String>() {
            	{
                    add("§7보상을 어떻게 지급할 지 결정합니다.");
                    add("§7현재: §a전체 지급");
            	}
            })
            .build();
    
    private static final ItemStack REWARD_RANDOM = new ItemBuilder(MaterialX.DISPENSER)
            .displayName("§a보상 지급 방법")
            .lore(new ArrayList<String>() {
            	{
                    add("§7보상을 어떻게 지급할 지 결정합니다.");
                    add("§7현재: §c무작위 하나 지급");
            	}
            })
            .build();
    
    private static final ItemStack REWARD_SELECT = new ItemBuilder(MaterialX.JUKEBOX)
            .displayName("§a보상 지급 방법")
            .lore(new ArrayList<String>() {
            	{
                    add("§7보상을 어떻게 지급할 지 결정합니다.");
                    add("§7현재: §b전체 보상 중 택 1");
            	}
            })
            .build();
    
    private static final ItemStack RUNE_BAG = new ItemBuilder(MaterialX.ENDER_CHEST)
            .displayName("§d룬 가방")
            .lore(new ArrayList<String>() {
            	{
                    add("§7룬들이 보관되어 있습니다.");
                    add("§c현재 개발 중.");
            	}
            })
            .build();
	
	public KillRewardGUI(Player opener, Plugin plugin) {
        Bukkit.getPluginManager().registerEvents(this, plugin);
        this.player = opener;
        openGUI();
	}
	
	private final Player player;
	private final YamlConfiguration rewardconfig = YamlConfiguration.loadConfiguration(FileUtil.newFile("killreward.txt"));
    private Inventory gui = Bukkit.createInventory(null, 54, "§c킬 보상 GUI");
    private List<ItemStack> rewardtype = new ArrayList<ItemStack>(){
    	{
    		add(REWARD_ALL);
    		add(REWARD_RANDOM);
    		add(REWARD_SELECT);
    	}
    };
    private List<ItemStack> itemstacks = new ArrayList<>();
    private int nowtype = 0;
	
    @SuppressWarnings("unchecked")
	private void openGUI() {
    	if (rewardconfig.get("list") != null) itemstacks = (List<ItemStack>) rewardconfig.get("list");
    	nowtype = rewardtype.indexOf(itemstacks.get(0) == null ? 0 : itemstacks.get(0));
        gui.setItem(0, rewardtype.get(nowtype));
        gui.setItem(8, RUNE_BAG);
        for (int i : GLASS_PANES) {
            gui.setItem(i, GLASS_PANE);
        }
        for (int i : GRAY_GLASS_PANES) {
            gui.setItem(i, GRAY_GLASS_PANE);
        }
        gui.addItem(itemstacks.toArray(new ItemStack[0]));
        player.openInventory(gui);
    }
    
    public ItemStack getItem(int slot) {
    	return itemstacks.get(slot);
    }
    
	public List<ItemStack> getItems() {
		return itemstacks;
	}
    
    @EventHandler()
    public void onInventoryClick(InventoryClickEvent e) {
    	if (e.getSlot() == 0) {
    		nowtype = (nowtype >= 2) ? 0 : nowtype + 1;
            gui.setItem(0, rewardtype.get(nowtype));
    	}
    }
    
    @EventHandler()
    public void onInventoryClose(InventoryCloseEvent e) {
    	if (e.getInventory().equals(gui)) {
    		rewardconfig.set("list", itemstacks);
    		try {
				rewardconfig.save(FileUtil.newFile("killreward.txt"));
			} catch (IOException e1) {
				e1.printStackTrace();
			}
    	}
    }

	
	
}