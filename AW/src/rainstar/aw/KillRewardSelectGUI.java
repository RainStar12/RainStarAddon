package rainstar.aw;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

public class KillRewardSelectGUI implements Listener {
	
	public KillRewardSelectGUI(Player opener, Plugin plugin) {
        Bukkit.getPluginManager().registerEvents(this, plugin);
        this.player = opener;
        openGUI();
	}
	
	private final Player player;
	
    private Inventory gui = Bukkit.createInventory(null, 36, "§c킬 보상 선택");
	
	private void openGUI() {
    	gui.clear();
        gui.addItem(KillRewardGUI.getItems().toArray(new ItemStack[0]));
        player.getPlayer().openInventory(gui);
    }
	
    @EventHandler()
    public void onInventoryClick(InventoryClickEvent e) {
    	if (e.getClickedInventory().equals(gui)) {
    		player.getInventory().addItem(e.getCurrentItem());
    		e.setCancelled(true);
    		player.closeInventory();
    	}
    }
    
    @EventHandler()
    public void onInventoryClose(InventoryCloseEvent e) {
    	if (e.getInventory().equals(gui)) {
        	HandlerList.unregisterAll(this);	
    	}
    }
}