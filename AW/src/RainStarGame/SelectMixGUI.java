package RainStarGame;

import com.google.common.collect.ImmutableMap;

import RainStarAbility.Null;
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
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class SelectMixGUI implements Listener {

    private static final int[] GLASS_PANES = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 29, 31, 32, 33, 35, 36, 37, 38, 39, 40, 41, 42, 43, 44};

    //10 ~ 16

    private static final ItemStack GLASS_PANE = new ItemBuilder(MaterialX.WHITE_STAINED_GLASS_PANE)
            .displayName(ChatColor.WHITE.toString())
            .build();

    private static final ItemStack ALL_REROLL = new ItemBuilder(MaterialX.PAPER)
            .displayName("§c전체 리롤")
            .lore("§7선택지의 모든 능력을 다시 추첨합니다.")
            .build();
    
    private static final ItemStack SELECT_REROLL = new ItemBuilder(MaterialX.PAPER)
            .displayName("§c선택 리롤")
            .lore("§7현재 선택된 능력들만 다시 추첨합니다.")
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
            .put(Rank.L, MaterialX.YELLOW_CONCRETE)
            .put(Rank.SPECIAL, MaterialX.RED_CONCRETE)
            .build();


    private final AbstractMix game;
    private final Deque<Integer> selected = new ArrayDeque<>(2);
    private final AbilityRegistration[] abilities = new AbilityRegistration[7];
    private final MixParticipant player;
    private Inventory gui = Bukkit.createInventory(null, 45, "§c능력 선택");;
    private final List<AbilityRegistration> randomAbilities = new ArrayList<>();
    private final Random random = new Random();
    private boolean rerollchance = true;
    private boolean reroll = false;
    private boolean decide = false;
    private boolean will = true;

    public SelectMixGUI(@Nullable final MixParticipant player, @Nonnull final AbstractMix game, @Nonnull final Plugin plugin) {
        this.game = game;
        this.player = player;
        Bukkit.getPluginManager().registerEvents(this, plugin);
        for (AbilityRegistration registration : AbilityList.values()) {
            if (!Settings.isBlacklisted(registration.getManifest().name()) && registration.isAvailable(game.getClass()) && (Settings.isUsingBetaAbility() || !registration.hasFlag(Flag.BETA))) {
                randomAbilities.add(registration);
            }
        }
        reroll(RerollTarget.INITIAL);
        will = false;
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
                AbilityBase.getExplanation(registration).forEachRemaining(exp -> {
                    lore.add(ChatColor.WHITE + exp);
                });
                final ItemStack item = new ItemBuilder(RANK_MATERIALS.get(manifest.rank()))
                        .displayName("§b" + manifest.name())
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
        gui.setItem(34, DECIDE);
        player.getPlayer().openInventory(gui);
        will = true;
    }

    public void reroll(RerollTarget target) {
    	rerollchance = false;
        will = false;
        target.reroll(this);
        will = false;
        openGUI();
    }
    
    public void skip() {
    	if (!decide) {
        	decide = true;
        	if (selected.size() < 2) {
            	try {
        			((Mix) player.getAbility()).setAbility(Null.class, Null.class);
        		} catch (ReflectiveOperationException e1) {
        			e1.printStackTrace();
        		}
        	} else {
            	try {
        			((Mix) player.getAbility()).setAbility(abilities[selected.getFirst()], abilities[selected.getLast()]);
        		} catch (ReflectiveOperationException e1) {
        			e1.printStackTrace();
        		}	
        	}
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
		if (e.getInventory().equals(gui) && !decide && will) {
	        will = false;
			openGUI();
		}
	}

    @EventHandler
    private void onInventoryClick(InventoryClickEvent e) {
		Bukkit.broadcastMessage("클릭");
        if (e.getInventory().equals(gui)) {
            final int slot = e.getSlot();
            e.setCancelled(true);
            if (reroll) return;
            if (slot >= 10 && slot <= 16) {
            	if (selected.contains(slot - 10)) return;
                while (selected.size() >= 2) {
                    selected.removeFirst();
                }
                selected.add(slot - 10);
                will = false;
                openGUI();
            } else if (slot == 28) {
            	if (rerollchance) reroll(RerollTarget.ALL);
            } else if (slot == 30) {
            	if (rerollchance) reroll(RerollTarget.SELECTED);
            } else if (slot == 34) {
            	if (selected.size() == 2) {
                	decide = true;
                	try {
    					((Mix) player.getAbility()).setAbility(abilities[selected.getFirst()], abilities[selected.getLast()]);
    				} catch (ReflectiveOperationException e1) {
    					e1.printStackTrace();
    				}
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
                }.setPeriod(TimeUnit.TICKS, 3).start();
            }
        }, SELECTED {
            @Override
            public void reroll(SelectMixGUI gui) {
                gui.reroll = true;
                for (Integer i : gui.selected) {
                    gui.abilities[i] = gui.random.pick(gui.randomAbilities);
                }
                gui.selected.clear();
                gui.reroll = false;
                gui.openGUI();
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