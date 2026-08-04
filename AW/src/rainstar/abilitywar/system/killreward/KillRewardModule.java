package rainstar.abilitywar.system.killreward;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.ItemStack;

import daybreak.abilitywar.AbilityWar;
import daybreak.abilitywar.game.event.participant.ParticipantDeathEvent;
import daybreak.abilitywar.game.module.ListenerModule;
import daybreak.abilitywar.game.module.ModuleBase;
import daybreak.abilitywar.utils.base.random.Random;

@ModuleBase(KillRewardModule.class)
public class KillRewardModule implements ListenerModule {
	
	private Random random = new Random();
	
	@Override
	public void register() {
		Bukkit.getPluginManager().registerEvents(this, AbilityWar.getPlugin());
	}
	
	@Override
	public void unregister() {
		HandlerList.unregisterAll(this);
	}
	
	@EventHandler(priority = EventPriority.HIGHEST)
	public void onParticipantDeath(ParticipantDeathEvent e) {
		if (e.getPlayer().getKiller() != null) {
			Player killer = e.getPlayer().getKiller();
			if (KillRewardGUI.type.equals(KillRewardGUI.Type.ALL)) {
    			for (ItemStack itemStack : KillRewardGUI.getItems().toArray(new ItemStack[0])) {
		    		if (killer.getInventory().firstEmpty() == -1) {
		    			killer.getWorld().dropItem(killer.getLocation(), itemStack);
		    		} else killer.getInventory().addItem(itemStack);
    			}
			}
			if (KillRewardGUI.type.equals(KillRewardGUI.Type.RANDOM)) {
				ItemStack itemStack = KillRewardGUI.getItems().get(random.nextInt(KillRewardGUI.getItems().size()));
	    		if (killer.getInventory().firstEmpty() == -1) {
	    			killer.getWorld().dropItem(killer.getLocation(), itemStack);
	    		} else killer.getInventory().addItem(itemStack);
				killer.sendMessage("§4[§b!§4] §c킬 보상§7: §a" + itemStack.getType() + " ×" + itemStack.getAmount());
			}
			if (KillRewardGUI.type.equals(KillRewardGUI.Type.SELECT)) {
				new KillRewardSelectGUI(killer, AbilityWar.getPlugin());
			}	
		}
	}

}
