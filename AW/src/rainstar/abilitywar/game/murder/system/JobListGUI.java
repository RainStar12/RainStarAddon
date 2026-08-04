package rainstar.abilitywar.game.murder.system;

import daybreak.abilitywar.ability.AbilityFactory;
import daybreak.abilitywar.ability.AbilityFactory.AbilityRegistration;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.game.list.murdermystery.JobList;
import daybreak.abilitywar.game.list.murdermystery.ability.AbstractInnocent;
import daybreak.abilitywar.game.list.murdermystery.ability.AbstractMurderer;
import daybreak.abilitywar.utils.base.Messager;
import daybreak.abilitywar.utils.base.RegexReplacer;
import daybreak.abilitywar.utils.base.minecraft.item.builder.ItemBuilder;
import daybreak.abilitywar.utils.base.reflect.ReflectionUtil;
import daybreak.abilitywar.utils.library.MaterialX;
import rainstar.abilitywar.game.murder.innocent.Frog;
import rainstar.abilitywar.game.murder.murderer.CorruptionDetectiveMurderer;
import rainstar.abilitywar.game.murder.murderer.FanaticMurderer;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.StringJoiner;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.regex.MatchResult;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;

public class JobListGUI implements Listener {

	private final ItemStack PREVIOUS_PAGE = new ItemBuilder(MaterialX.ARROW)
			.displayName(ChatColor.AQUA + "이전 페이지")
			.build();

	private final ItemStack NEXT_PAGE = new ItemBuilder(MaterialX.ARROW)
			.displayName(ChatColor.AQUA + "다음 페이지")
			.build();

	private static final RegexReplacer SQUARE_BRACKET = new RegexReplacer("\\$\\[([^\\[\\]]+)\\]");
	private static final RegexReplacer ROUND_BRACKET = new RegexReplacer("\\$\\(([^()]]+)\\)");

	private final Player player;
	private final Map<String, AbilityRegistration> values;
	private int currentPage = 1;
	private Inventory jobGUI;

	public JobListGUI(Player player, Plugin plugin) {
		this.player = player;
		Bukkit.getPluginManager().registerEvents(this, plugin);

		values = new TreeMap<>();
		for (AbilityRegistration registration : JobList.values()) {
			values.put(registration.getManifest().name(), registration);
		}
		values.put("머더: 광신도", AbilityFactory.getRegistration(FanaticMurderer.class));
		values.put("머더: 부패 탐정", AbilityFactory.getRegistration(CorruptionDetectiveMurderer.class));
		values.put("시민: 개구리", AbilityFactory.getRegistration(Frog.class));
		for (AbilityRegistration registration : AbilityFactory.getRegistrations()) {
			if (registration.getManifest().name().contains("탐정")) values.put(registration.getManifest().name(), registration);
		}
	}

	public void openGUI(int page) {
		int maxPage = ((values.size() - 1) / 36) + 1;
		if (maxPage < page) page = 1;
		if (page < 1) page = 1;
		jobGUI = Bukkit.createInventory(null, 54, "§0직업 목록");
		currentPage = page;
		int count = 0;

		for (Entry<String, AbilityRegistration> entry : values.entrySet()) {
			if (count / 36 == page - 1) {
				AbilityRegistration registration = entry.getValue();
				AbilityManifest manifest = registration.getManifest();
				MaterialX material = MaterialX.IRON_BLOCK;
				if (AbstractMurderer.class.isAssignableFrom(registration.getAbilityClass())) material = MaterialX.RED_CONCRETE;
				else if (manifest.name().contains("탐정")) material = MaterialX.CYAN_CONCRETE;
				else if (AbstractInnocent.class.isAssignableFrom(registration.getAbilityClass())) material = MaterialX.LIME_CONCRETE;
				ItemStack stack = new ItemBuilder(material).build();
				ItemMeta meta = stack.getItemMeta();
				meta.setDisplayName(jobColor(manifest.name()) + manifest.name());
				StringJoiner joiner = new StringJoiner(ChatColor.WHITE + ", ");
				final List<String> lore = Messager.asList(
						joiner.toString());
				final Function<MatchResult, String> valueProvider = new Function<MatchResult, String>() {
					@Override
					public String apply(MatchResult matchResult) {
						try {
							final Field field = registration.getAbilityClass().getDeclaredField(matchResult.group(1));
							if (Modifier.isStatic(field.getModifiers())) {
								try {
									return String.valueOf(ReflectionUtil.setAccessible(field).get(null));
								} catch (IllegalAccessException ignored) {
								}
							}
						} catch (NoSuchFieldException ignored) {
						}
						return "?";
					}
				};
				for (String explain : manifest.explain()) {
					lore.add(ChatColor.WHITE.toString().concat(ROUND_BRACKET.replaceAll(SQUARE_BRACKET.replaceAll(explain, valueProvider), valueProvider)));
				}
				meta.setLore(lore);
				stack.setItemMeta(meta);
				jobGUI.setItem(count % 36, stack);
			}
			count++;
		}

		if (page > 1) {
			jobGUI.setItem(48, PREVIOUS_PAGE);
		}

		if (page != maxPage) {
			jobGUI.setItem(50, NEXT_PAGE);
		}

		ItemStack stack = new ItemStack(Material.PAPER, 1);
		ItemMeta meta = stack.getItemMeta();
		meta.setDisplayName("§6페이지 §e" + page + " §6/ §e" + maxPage);
		stack.setItemMeta(meta);
		jobGUI.setItem(49, stack);

		player.openInventory(jobGUI);
	}

	@EventHandler
	private void onInventoryClose(InventoryCloseEvent e) {
		if (e.getInventory().equals(this.jobGUI)) {
			HandlerList.unregisterAll(this);
		}
	}

	@EventHandler
	private void onQuit(PlayerQuitEvent e) {
		if (e.getPlayer().getUniqueId().equals(player.getUniqueId())) {
			HandlerList.unregisterAll(this);
		}
	}

	@EventHandler
	private void onInventoryClick(InventoryClickEvent e) {
		if (e.getInventory().equals(jobGUI)) {
			e.setCancelled(true);
			if (e.getCurrentItem() != null && e.getCurrentItem().hasItemMeta() && e.getCurrentItem().getItemMeta().hasDisplayName() && e.getCurrentItem().getType() != Material.IRON_BLOCK) {
				String displayName = ChatColor.stripColor(e.getCurrentItem().getItemMeta().getDisplayName());
				if (displayName.equals("이전 페이지")) {
					openGUI(currentPage - 1);
				} else if (displayName.equals("다음 페이지")) {
					openGUI(currentPage + 1);
				}
			}
		}
	}
	
	public String jobColor(String name) {
		if (name.contains("머더")) return "§c";
		else if (name.contains("시민")) return "§a";
		else if (name.contains("탐정")) return "§3";
		else return "§f";
	}
	
}