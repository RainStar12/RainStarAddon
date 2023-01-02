package rainstar.abilitywar.game.SelectMix;

import com.google.common.collect.ImmutableMap;

import daybreak.abilitywar.AbilityWar;
import daybreak.abilitywar.ability.AbilityBase;
import daybreak.abilitywar.ability.AbilityFactory.AbilityRegistration;
import daybreak.abilitywar.ability.AbilityFactory.AbilityRegistration.Flag;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.config.Configuration.Settings;
import daybreak.abilitywar.game.event.GameEndEvent;
import daybreak.abilitywar.game.list.mix.AbstractMix;
import daybreak.abilitywar.game.list.mix.AbstractMix.MixParticipant;
import daybreak.abilitywar.game.list.mix.Mix;
import daybreak.abilitywar.game.manager.AbilityList;
import daybreak.abilitywar.utils.base.concurrent.SimpleTimer.TaskType;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.base.minecraft.item.builder.ItemBuilder;
import daybreak.abilitywar.utils.base.random.Random;
import daybreak.abilitywar.utils.library.MaterialX;
import daybreak.abilitywar.utils.library.SoundLib;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.enchantments.Enchantment;
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
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

@SuppressWarnings("serial")
public class SelectMixGUI implements Listener {

    private static final int[] GLASS_PANES = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 29, 31, 33, 35, 36, 37, 38, 39, 40, 41, 42, 43, 44};

    //10 ~ 16

    private static final ItemStack GLASS_PANE = new ItemBuilder(MaterialX.WHITE_STAINED_GLASS_PANE)
            .displayName(ChatColor.WHITE.toString())
            .build();

	private static final ItemStack ALL_REROLL = new ItemBuilder(MaterialX.PAPER)
            .displayName("§c전체 리롤")
            .lore(new ArrayList<String>() {
            	{
                    add("§7선택지의 모든 능력을 다시 추첨합니다.");
                    add("§7리롤 기회를 1회 소모합니다.");
            	}
            })
            .build();
    
    private static final ItemStack SELECT_REROLL = new ItemBuilder(MaterialX.PAPER)
            .displayName("§c선택 리롤")
            .lore(new ArrayList<String>() {
            	{
                    add("§7현재 선택해 놓은 능력들만 다시 추첨합니다.");
                    add("§7리롤 기회를 1회 소모합니다.");
            	}
            })
            .build();
    
    private static final ItemStack EXPLAIN = new ItemBuilder(MaterialX.OAK_SIGN)
            .displayName("§eGUI 사용방법")
            .lore(new ArrayList<String>() {
            	{
            		add("§a==========================================");
            		add("§7콘크리트 블럭을 §b§n좌클릭§f하면, 해당 §b능력§f을 §d선택§f합니다.");
                    add("§7같은 방식으로 최대 §e2개§f까지 지정할 수 있으며,");
                    add("§e2개§f를 넘는다면 가장 먼저 선택한 §b능력§f이 지워집니다.");
                    add("§7배정되는 순서는 §b능력§f이 지정된 순서입니다.");
                    add("§e2개§f를 전부 선택하였으면 §a라임색 유리판§f을 눌러 결정을 마칩니다.");
                    add("§a==========================================");
                    add("§7종이를 눌러 리롤 기회를 사용해 선택지들을 바꿀 수도 있습니다.");
                    add("§7리롤 기회: -회");
                    add("§a==========================================");
                    add("§c자동 스킵까지 선택을 미루는 것을 추천드리지 않습니다...");
                    add("§a==========================================");
            	}
            })
            .build();
    
    private static final ItemStack DECIDE = new ItemBuilder(MaterialX.LIME_STAINED_GLASS_PANE)
            .displayName("§c결정")
            .lore("§7현재 선택된 능력들을 자신의 능력을 결정합니다.")
            .build();

    private static final ImmutableMap<Rank, MaterialX> RANK_MATERIALS = ImmutableMap.<Rank, MaterialX>builder()
            .put(Rank.C, MaterialX.YELLOW_CONCRETE)
            .put(Rank.B, MaterialX.LIGHT_BLUE_CONCRETE)
            .put(Rank.A, MaterialX.LIME_CONCRETE)
            .put(Rank.S, MaterialX.PINK_CONCRETE)
            .put(Rank.L, MaterialX.ORANGE_CONCRETE)
            .put(Rank.SPECIAL, MaterialX.RED_CONCRETE)
            .build();
    
    private static final ImmutableMap<Rank, String> NAMETAG = ImmutableMap.<Rank, String>builder()
            .put(Rank.C, "§6[§eC§6] §b")
            .put(Rank.B, "§3[§bB§3] §b")
            .put(Rank.A, "§2[§aA§2] §b")
            .put(Rank.S, "§5[§dS§5] §b")
            .put(Rank.L, "§c[§6L§c] §b")
            .put(Rank.SPECIAL, "§4[§cSPECIAL§4] §b")
            .build();


    private final AbstractMix game;
    private final Deque<Integer> selected = new ArrayDeque<>(2);
    private final AbilityRegistration[] abilities = new AbilityRegistration[7];
    private final MixParticipant player;
    private Inventory gui = Bukkit.createInventory(null, 45, "§4능력 선택");
    private final List<AbilityRegistration> randomAbilities = new ArrayList<>();
    private final Random random = new Random();
    private int rerollused;
    private final int rerollable;
    private boolean reroll = false;
    public boolean decide = false;
    private boolean handleCloseInventory = true;
    private boolean explain = false;

    public SelectMixGUI(@Nullable final MixParticipant player, @Nonnull final AbstractMix game, @Nonnull final int rerollable, @Nonnull final Plugin plugin) {
        this.game = game;
        this.player = player;
        this.rerollable = rerollable;
        this.rerollused = 0;
        Bukkit.getPluginManager().registerEvents(this, plugin);
        for (AbilityRegistration registration : AbilityList.values()) {
            if (!Settings.isBlacklisted(registration.getManifest().name()) && registration.isAvailable(game.getClass()) && (Settings.isUsingBetaAbility() || !registration.hasFlag(Flag.BETA))) {
                randomAbilities.add(registration);
            }
        }
    	ItemMeta meta = EXPLAIN.getItemMeta();
    	meta.setLore(new ArrayList<String>() {
        	{
        		add("§a==========================================");
        		add("§7콘크리트 블럭을 §b§n좌클릭§f하면, 해당 §b능력§f을 §d선택§f합니다.");
                add("§7같은 방식으로 최대 §e2개§f까지 지정할 수 있으며,");
                add("§e2개§f를 넘는다면 가장 먼저 선택한 §b능력§f이 지워집니다.");
                add("§7배정되는 순서는 §b능력§f이 지정된 순서입니다.");
                add("§e2개§f를 전부 선택하였으면 §a라임색 유리판§f을 눌러 결정을 마칩니다.");
                add("§a==========================================");
                add("§7종이를 눌러 리롤 기회를 사용해 선택지들을 바꿀 수도 있습니다.");
                add("§3리롤 기회§f: §a" + rerollable + "§f회");
                add("§a==========================================");
                add("§c자동 스킵까지 선택을 미루는 것을 추천드리지 않습니다...");
                add("§a==========================================");
        	}
        });
    	EXPLAIN.setItemMeta(meta);
        reroll(RerollTarget.INITIAL);
        openGUI();
    }

    private void openGUI() {
        gui.clear();
        for (int i : GLASS_PANES) {
            gui.setItem(i, GLASS_PANE);
        }
        {
            int index = 10;
            for (int i = 0; i < abilities.length; i++) {
                final AbilityRegistration registration = abilities[i];
                if (registration == null) continue;
                final AbilityManifest manifest = registration.getManifest();
                final ArrayList<String> lore = new ArrayList<>();
                lore.add("§7우클릭으로 능력 설명을 켜고 끌 수 있습니다.");
                if (explain) {
                    AbilityBase.getExplanation(registration).forEachRemaining(exp -> {
                        lore.add(ChatColor.WHITE + exp);
                    });	
                }
                final ItemStack item = new ItemBuilder(RANK_MATERIALS.get(manifest.rank()))
                        .displayName(NAMETAG.get(manifest.rank()) + manifest.name())
                        .lore(lore)
                        .build();
                if (selected.contains(i)) {
                    item.addUnsafeEnchantment(Enchantment.MENDING, 1);
                }
                gui.setItem(index, item);
                index++;
            }
        }
        gui.setItem(28, ALL_REROLL);
        gui.setItem(30, SELECT_REROLL);
        gui.setItem(32, EXPLAIN);
        gui.setItem(34, DECIDE);
        handleCloseInventory = false;
        player.getPlayer().openInventory(gui);
        handleCloseInventory = true;
    }
    
    public void reroll(RerollTarget target) {
        target.reroll(this);
        openGUI();
    }
    
    public void setAbility() {
    	Mix mix = player.getAbility();    	
    	switch(selected.size()) {
    	case 0:
        	try {
    			mix.setAbility(abilities[0], abilities[1]);
    		} catch (ReflectiveOperationException e1) {
    			e1.printStackTrace();
    		}
    		break;
    	case 1:
        	try {
        		mix.setAbility(abilities[selected.getFirst()].getAbilityClass(), Null.class);
    		} catch (ReflectiveOperationException e1) {
    			e1.printStackTrace();
    		}
    		break;
    	case 2:
        	try {
        		mix.setAbility(abilities[selected.getFirst()], abilities[selected.getLast()]);
    		} catch (ReflectiveOperationException e1) {
    			e1.printStackTrace();
    		}
    		break;
    	}
    	if (mix.hasSynergy()) player.getPlayer().sendMessage("§2[§a!§2] §f능력을 확정하였습니다. 당신의 능력: §b" + mix.getSynergy().getDisplayName() + "§f입니다.");
    	else player.getPlayer().sendMessage("§2[§a!§2] §f능력을 확정하였습니다. 당신의 능력: §b" + mix.getFirst().getDisplayName() + " §7+ §b" + mix.getSecond().getDisplayName() + "§f입니다.");
    }
    
    public void skip() {
    	if (!decide) {
        	decide = true;
        	setAbility();
        	HandlerList.unregisterAll(this);
        	player.getPlayer().closeInventory();	
    	}
    }
    
    @EventHandler
    private void onGameEnd(GameEndEvent e) {
    	decide = true;
    	player.getPlayer().closeInventory();
    }
    
	@EventHandler
	private void onInventoryClose(InventoryCloseEvent e) {
		if (e.getInventory().equals(gui) && !decide) {
			if (!handleCloseInventory) return;
			new BukkitRunnable() {				
				@Override
				public void run() {
					openGUI();
				}				
			}.runTaskLater(AbilityWar.getPlugin(), 1);
		}
	}

    @EventHandler
    private void onInventoryClick(InventoryClickEvent e) {
        if (e.getInventory().equals(gui)) {
            final int slot = e.getSlot();
            e.setCancelled(true);
            if (reroll) return;
            if (slot >= 10 && slot <= 16) {
            	if (e.getClick().equals(ClickType.RIGHT)) {
            		explain = !explain;
            	} else {
                	if (selected.contains(slot - 10)) return;
                    while (selected.size() >= 2) {
                        selected.removeFirst();
                    }
                    selected.add(slot - 10);	
            	}
                openGUI();
            } else if (slot == 28) {
            	if (rerollused < rerollable) {
                	rerollused++;
                	ItemMeta meta = EXPLAIN.getItemMeta();
                	meta.setLore(new ArrayList<String>() {
		            	{
		            		add("§a==========================================");
		            		add("§7콘크리트 블럭을 §b§n좌클릭§f하면, 해당 §b능력§f을 §d선택§f합니다.");
		                    add("§7같은 방식으로 최대 §e2개§f까지 지정할 수 있으며,");
		                    add("§e2개§f를 넘는다면 가장 먼저 선택한 §b능력§f이 지워집니다.");
		                    add("§7배정되는 순서는 §b능력§f이 지정된 순서입니다.");
		                    add("§e2개§f를 전부 선택하였으면 §a라임색 유리판§f을 눌러 결정을 마칩니다.");
		                    add("§a==========================================");
		                    add("§7종이를 눌러 리롤 기회를 사용해 선택지들을 바꿀 수도 있습니다.");
		                    add("§3리롤 기회§f: §a" + (rerollable - rerollused) + "§f회");
		                    add("§a==========================================");
		                    add("§c자동 스킵까지 선택을 미루는 것을 추천드리지 않습니다...");
		                    add("§a==========================================");
		            	}
		            });
                	EXPLAIN.setItemMeta(meta);
            		reroll(RerollTarget.ALL);
            	}
            } else if (slot == 30) {
            	if (rerollused < rerollable && selected.size() > 0) {
                	rerollused++;
                	ItemMeta meta = EXPLAIN.getItemMeta();
                	meta.setLore(new ArrayList<String>() {
		            	{
		            		add("§a==========================================");
		            		add("§7콘크리트 블럭을 §b§n좌클릭§f하면, 해당 §b능력§f을 §d선택§f합니다.");
		                    add("§7같은 방식으로 최대 §e2개§f까지 지정할 수 있으며,");
		                    add("§e2개§f를 넘는다면 가장 먼저 선택한 §b능력§f이 지워집니다.");
		                    add("§7배정되는 순서는 §b능력§f이 지정된 순서입니다.");
		                    add("§e2개§f를 전부 선택하였으면 §a라임색 유리판§f을 눌러 결정을 마칩니다.");
		                    add("§a==========================================");
		                    add("§7종이를 눌러 리롤 기회를 사용해 선택지들을 바꿀 수도 있습니다.");
		                    add("§3리롤 기회§f: §a" + (rerollable - rerollused) + "§f회");
		                    add("§a==========================================");
		                    add("§c자동 스킵까지 선택을 미루는 것을 추천드리지 않습니다...");
		                    add("§a==========================================");
		            	}
		            });
                	EXPLAIN.setItemMeta(meta);
            		reroll(RerollTarget.SELECTED);
            	}
            } else if (slot == 34) {
            	if (selected.size() == 2) {
                	decide = true;
                	setAbility();
                	HandlerList.unregisterAll(this);
                	player.getPlayer().closeInventory();	
            	}
            }
        }
    }

    public enum RerollTarget {
        ALL {
            @Override
            public void reroll(SelectMixGUI gui) {
                gui.reroll = true;
                gui.game.new GameTimer(TaskType.NORMAL, 16) {
                    @Override
                    protected void run(int count) {
                        if (count <= 7) {
                            gui.abilities[count - 1] = null;
                            SoundLib.BLOCK_WOODEN_TRAPDOOR_CLOSE.playSound(gui.player.getPlayer(), 1, 2);
                            SoundLib.BLOCK_WOODEN_TRAPDOOR_OPEN.playSound(gui.player.getPlayer(), 1, 2);
                            gui.openGUI();
                        } else if (count == 9) {
                            gui.selected.clear();
                        } else if (count >= 10) {
                            SoundLib.ENTITY_PLAYER_LEVELUP.playSound(gui.player.getPlayer(), 1, 2);
                            gui.abilities[6 - (count - 10)] = gui.random.pick(gui.randomAbilities);
                            gui.openGUI();
                            // 10 -> 6, 11 -> 5, 12 -> 4
                        }
                    }

                    @Override
                    protected void onEnd() {
                        gui.reroll = false;
                    }
                }.setPeriod(TimeUnit.TICKS, 2).start();
            }
        }, SELECTED {
            @Override
            public void reroll(SelectMixGUI gui) {
                gui.reroll = true;
                gui.game.new GameTimer(TaskType.NORMAL, 5) {   	
                    @Override
                    protected void run(int count) {
                    	for (Integer i : gui.selected) {
                        	gui.abilities[i] = gui.random.pick(gui.randomAbilities);
                    	}
                    	gui.openGUI();
                        SoundLib.BLOCK_WOODEN_TRAPDOOR_CLOSE.playSound(gui.player.getPlayer(), 1, 2);
                        SoundLib.BLOCK_WOODEN_TRAPDOOR_OPEN.playSound(gui.player.getPlayer(), 1, 2);
                    }
                    
                    @Override
                    protected void onEnd() {
                        for (Integer i : gui.selected) {
                            gui.abilities[i] = gui.random.pick(gui.randomAbilities);
                        }
                        gui.selected.clear();
                        gui.openGUI();
                        gui.reroll = false;
                        SoundLib.ENTITY_PLAYER_LEVELUP.playSound(gui.player.getPlayer(), 1, 2);
                    }
                }.setPeriod(TimeUnit.TICKS, 2).start();
            }
        }, INITIAL {
            @Override
            public void reroll(SelectMixGUI gui) {
                gui.selected.clear();
                for (int i = 0; i < gui.abilities.length; i++) {
                    gui.abilities[i] = gui.random.pick(gui.randomAbilities);
                }
            }
        };

        public abstract void reroll(SelectMixGUI gui);
    }

}