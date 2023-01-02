package rainstar.abilitywar.synergy.chance;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringJoiner;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import daybreak.abilitywar.AbilityWar;
import daybreak.abilitywar.ability.AbilityFactory.AbilityRegistration;
import daybreak.abilitywar.ability.AbilityFactory.AbilityRegistration.Flag;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.config.Configuration;
import daybreak.abilitywar.game.AbstractGame;
import daybreak.abilitywar.game.AbstractGame.GameTimer;
import daybreak.abilitywar.game.list.mix.Mix;
import daybreak.abilitywar.game.list.mix.synergy.SynergyFactory;
import daybreak.abilitywar.utils.base.Messager;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.base.minecraft.item.builder.ItemBuilder;
import daybreak.abilitywar.utils.base.random.Random;
import daybreak.abilitywar.utils.library.MaterialX;
import daybreak.abilitywar.utils.library.SoundLib;
import net.md_5.bungee.api.ChatColor;

public class AbilitySelectChance extends GameTimer implements Listener {
	
    public AbilityRegistration getRandomSynergy() {

		Set<AbilityRegistration> synergies = new HashSet<>();
        Random r = new Random();
		
        for (AbilityRegistration synergy : SynergyFactory.getSynergies()) {
        	String name = synergy.getManifest().name();
        	if (!Configuration.Settings.isBlacklisted(name) && !name.equals("기회")) {
        		synergies.add(synergy);
        	}
        }
        
        return synergies.toArray(new AbilityRegistration[]{})[r.nextInt(synergies.size())];
    }
	
	private final ItemStack NULL = (new ItemBuilder(MaterialX.GRAY_STAINED_GLASS_PANE)).displayName(" ").build();
	private final ItemStack MYSTERY = (new ItemBuilder(MaterialX.GRAY_CONCRETE)).displayName("§7§l?").build();
	
	private Player player;
	
	private final List<AbilityRegistration> values = new ArrayList<>();
	private Set<AbilityRegistration> synergies = new HashSet<>();
	
	private Map<Integer, AbilityRegistration> slots = new HashMap<>();
	
	private AbilityRegistration selected;
	
	private final Inventory gui;
	
	public AbilitySelectChance(AbstractGame abstractGame, Player player) {
		abstractGame.super(TaskType.REVERSE, 300);
		setPeriod(TimeUnit.TICKS, 1);
		this.player = player;
		gui = Bukkit.createInventory(null, 9, "§0능력을 선택해주세요.");
	}
	
	protected void onStart() {
		Bukkit.getPluginManager().registerEvents(this, AbilityWar.getPlugin());
		player.openInventory(gui);
		for (int i = 0; i < 5; i++) {
			AbilityRegistration randomSynergy = getRandomSynergy();
			values.add(randomSynergy);
			selected = randomSynergy;
			synergies.add(randomSynergy);
		}
	}
	
	@Override
	protected void run(int arg0) {
		placeItem();
		player.setGameMode(GameMode.SPECTATOR);
		if (arg0 == 60) SoundLib.BLOCK_NOTE_BLOCK_SNARE.playSound(player, 1, 1.7f); 
		if (arg0 == 40) SoundLib.BLOCK_NOTE_BLOCK_SNARE.playSound(player, 1, 1.7f); 
		if (arg0 == 20) SoundLib.BLOCK_NOTE_BLOCK_SNARE.playSound(player, 1, 1.7f); 
	}
	
	@Override
	protected void onEnd() {
		onSilentEnd();
		HandlerList.unregisterAll(this);
	}
	
	@Override
	protected void onSilentEnd() {
		Mix mix = (Mix) getGame().getParticipant(player).getAbility();
		try {
			mix.setAbility(SynergyFactory.getSynergyBase(selected).getLeft().getAbilityClass(), SynergyFactory.getSynergyBase(selected).getRight().getAbilityClass());
		} catch (UnsupportedOperationException | ReflectiveOperationException e1) {
			e1.printStackTrace();
		}
		HandlerList.unregisterAll(this);
		player.setGameMode(GameMode.SURVIVAL);
		player.closeInventory();
	}
	
	private MaterialX getRankBlock(Rank rank) {
		if (rank.equals(Rank.C)) {
			return MaterialX.YELLOW_CONCRETE;
		} else if (rank.equals(Rank.B)) {
			return MaterialX.LIGHT_BLUE_CONCRETE;
		} else if (rank.equals(Rank.A)) {
			return MaterialX.LIME_CONCRETE;
		} else if (rank.equals(Rank.S)) {
			return MaterialX.MAGENTA_CONCRETE;
		} else if (rank.equals(Rank.L)) {
			return MaterialX.ORANGE_CONCRETE;
		} else if (rank.equals(Rank.SPECIAL)) {
			return MaterialX.RED_CONCRETE;
		}
		return null;
	}
	
	private void placeItem() {
		for (int i = 0; i < 5; i++) {
			ItemStack item = new ItemBuilder(getRankBlock(values.get(i).getManifest().rank())).build();
			ItemMeta meta = item.getItemMeta();
			if (synergies.contains(values.get(i))) meta.addEnchant(Enchantment.LOOT_BONUS_BLOCKS, 1, true);
			meta.setDisplayName(ChatColor.AQUA + values.get(i).getManifest().name());
			final StringJoiner joiner = new StringJoiner(ChatColor.WHITE + ", ");
			if (values.get(i).hasFlag(Flag.ACTIVE_SKILL)) joiner.add(ChatColor.GREEN + "액티브");
			if (values.get(i).hasFlag(Flag.TARGET_SKILL)) joiner.add(ChatColor.GOLD + "타게팅");
			if (values.get(i).hasFlag(Flag.BETA)) joiner.add(ChatColor.DARK_AQUA + "베타");
			final List<String> lore = Messager.asList(
					"§f등급: " + values.get(i).getManifest().rank().getRankName(),
					"§f종류: " + values.get(i).getManifest().species().getSpeciesName(),
					joiner.toString(),
					"");
			for (final String line : values.get(i).getManifest().explain()) {
				lore.add(ChatColor.WHITE.toString().concat(line));
			}
			lore.add("");
			lore.add("§2» §f이 시너지를 부여하려면 클릭하세요.");
			meta.setLore(lore);
			item.setItemMeta(meta);
			switch(i) {
			case 0:
				gui.setItem(0, item);
				slots.put(0, values.get(i));
				break;
			case 1:
				gui.setItem(2, MYSTERY);
				slots.put(2, values.get(i));
				break;
			case 2:
				gui.setItem(4, item);
				slots.put(4, values.get(i));
				break;
			case 3:
				gui.setItem(6, MYSTERY);
				slots.put(6, values.get(i));
				break;
			case 4:
				gui.setItem(8, item);
				slots.put(8, values.get(i));
				break;
			}
			
			for (int j = 0; j < 8; j++) {
				if (gui.getItem(j) == null) {
					gui.setItem(j, NULL);
				}
			}
		}
	}
	
	@EventHandler
	private void onInventoryClose(InventoryCloseEvent e) {
		if (e.getInventory().equals(gui)) stop(false);
	}

	@EventHandler
	private void onQuit(PlayerQuitEvent e) {
		if (e.getPlayer().getUniqueId().equals(player.getUniqueId())) stop(false);
	}
	
	@EventHandler
	private void onPlayerMove(PlayerMoveEvent e) {
		if (e.getPlayer().equals(player)) e.setCancelled(true);
	}

	@EventHandler
	private void onEntityDamage(EntityDamageEvent e) {
		e.setCancelled(true);
	}
	
	@EventHandler
	private void onInventoryClick(InventoryClickEvent e) {
		if (e.getInventory().equals(gui)) {
			e.setCancelled(true);
			if (slots.containsKey(e.getSlot())) {
				selected = slots.get(e.getSlot());
				player.closeInventory();
			}
		}
	}
	
}