package rainstar.abilitywar.game.select;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

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

import com.google.common.collect.ImmutableMap;

import daybreak.abilitywar.AbilityWar;
import daybreak.abilitywar.ability.AbilityBase;
import daybreak.abilitywar.ability.AbilityFactory;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.AbilityFactory.AbilityRegistration;
import daybreak.abilitywar.ability.AbilityFactory.AbilityRegistration.Flag;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.config.Configuration;
import daybreak.abilitywar.config.Configuration.Settings;
import daybreak.abilitywar.game.AbstractGame;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.event.GameEndEvent;
import daybreak.abilitywar.game.list.mix.synergy.SynergyFactory;
import daybreak.abilitywar.game.manager.AbilityList;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.base.concurrent.SimpleTimer.TaskType;
import daybreak.abilitywar.utils.base.minecraft.item.builder.ItemBuilder;
import daybreak.abilitywar.utils.base.random.Random;
import daybreak.abilitywar.utils.library.MaterialX;
import daybreak.abilitywar.utils.library.SoundLib;
import rainstar.abilitywar.synergy.normalization.ancientability.AncientBerserker;
import rainstar.abilitywar.synergy.normalization.ancientability.AncientBlackCandle;
import rainstar.abilitywar.synergy.normalization.ancientability.AncientBombArrow;
import rainstar.abilitywar.synergy.normalization.ancientability.AncientBrewer;
import rainstar.abilitywar.synergy.normalization.ancientability.AncientChaos;
import rainstar.abilitywar.synergy.normalization.ancientability.AncientHacker;
import rainstar.abilitywar.synergy.normalization.ancientability.AncientSniper;
import rainstar.abilitywar.synergy.normalization.ancientability.AncientTheHighPriestess;
import rainstar.abilitywar.synergy.normalization.ancientability.AncientVoid;
import rainstar.abilitywar.synergy.normalization.ancientability.AncientYeti;
import rainstar.abilitywar.synergy.normalization.ancientability.AncientZombie;

@SuppressWarnings("serial")
public class SelectGUI implements Listener {

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
                    add("§7리롤 기회를 2회 소모합니다.");
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
            		add("§7콘크리트 블럭을 §b§n좌클릭§7하면, 해당 §b능력§7을 §d선택§7합니다.");
                    add("§7능력을 선택하였으면 §a라임색 유리판§7을 눌러 결정을 마칩니다.");
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
    
    private static final ImmutableMap<Rank, MaterialX> SYNERGY_RANK_MATERIALS = ImmutableMap.<Rank, MaterialX>builder()
            .put(Rank.C, MaterialX.YELLOW_STAINED_GLASS)
            .put(Rank.B, MaterialX.LIGHT_BLUE_STAINED_GLASS)
            .put(Rank.A, MaterialX.LIME_STAINED_GLASS)
            .put(Rank.S, MaterialX.PINK_STAINED_GLASS)
            .put(Rank.L, MaterialX.ORANGE_STAINED_GLASS)
            .put(Rank.SPECIAL, MaterialX.RED_STAINED_GLASS)
            .build();
    
    private static final ImmutableMap<Rank, String> NAMETAG = ImmutableMap.<Rank, String>builder()
            .put(Rank.C, "§6[§eC§6] §b")
            .put(Rank.B, "§3[§bB§3] §b")
            .put(Rank.A, "§2[§aA§2] §b")
            .put(Rank.S, "§5[§dS§5] §b")
            .put(Rank.L, "§c[§6L§c] §b")
            .put(Rank.SPECIAL, "§4[§cSPECIAL§4] §b")
            .build();


    private final AbstractGame game;
    private final Deque<Integer> selected = new ArrayDeque<>(1);
    private final AbilityRegistration[] abilities = new AbilityRegistration[7];
    private final Participant player;
    private Inventory gui = Bukkit.createInventory(null, 45, "§4능력 선택");
    private final List<AbilityRegistration> randomAbilities = new ArrayList<>();
    private final List<AbilityRegistration> randomSynergies = new ArrayList<>();
    private final List<AbilityRegistration> randomAncientAbilities = new ArrayList<>();
    private final Random random = new Random();
    private int rerollused;
    private final int rerollable;
    private final int synergychance;
    private boolean reroll = false;
    public boolean decide = false;
    private boolean handleCloseInventory = true;
    private boolean explain = false;

    public SelectGUI(@Nullable final Participant player, @Nonnull final AbstractGame game, @Nonnull final int rerollable, @Nonnull final int synergychance, @Nonnull final Plugin plugin) {
        this.game = game;
        this.player = player;
        this.rerollable = rerollable;
        this.rerollused = 0;
        this.synergychance = synergychance;
        Bukkit.getPluginManager().registerEvents(this, plugin);
        for (AbilityRegistration registration : AbilityList.values()) {
            if (!Settings.isBlacklisted(registration.getManifest().name()) && registration.isAvailable(game.getClass()) && (Settings.isUsingBetaAbility() || !registration.hasFlag(Flag.BETA))) {
                randomAbilities.add(registration);
            }
        }
        for (AbilityRegistration synergy : SynergyFactory.getSynergies()) {
        	String name = synergy.getManifest().name();
        	if (!Configuration.Settings.isBlacklisted(name)) {
        		randomSynergies.add(synergy);
        	}
        }
        randomAncientAbilities.add(AbilityFactory.getRegistration(AncientBerserker.class));
        randomAncientAbilities.add(AbilityFactory.getRegistration(AncientBlackCandle.class));
        randomAncientAbilities.add(AbilityFactory.getRegistration(AncientBombArrow.class));
        randomAncientAbilities.add(AbilityFactory.getRegistration(AncientBrewer.class));
        randomAncientAbilities.add(AbilityFactory.getRegistration(AncientChaos.class));
        randomAncientAbilities.add(AbilityFactory.getRegistration(AncientHacker.class));
        randomAncientAbilities.add(AbilityFactory.getRegistration(AncientSniper.class));
        randomAncientAbilities.add(AbilityFactory.getRegistration(AncientTheHighPriestess.class));
        randomAncientAbilities.add(AbilityFactory.getRegistration(AncientVoid.class));
        randomAncientAbilities.add(AbilityFactory.getRegistration(AncientYeti.class));
        randomAncientAbilities.add(AbilityFactory.getRegistration(AncientZombie.class));
    	ItemMeta meta = EXPLAIN.getItemMeta();
    	meta.setLore(new ArrayList<String>() {
        	{
        		add("§a==========================================");
        		add("§7콘크리트 블럭을 §b§n좌클릭§7하면, 해당 §b능력§7을 §d선택§7합니다.");
                add("§7능력을 선택하였으면 §a라임색 유리판§f을 눌러 결정을 마칩니다.");
                add("§a==========================================");
                add("§7종이를 눌러 리롤 기회를 사용해 선택지들을 바꿀 수도 있습니다.");
                add("§7리롤 기회: §a" + rerollable + "§7회");
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
                final ItemStack item = new ItemBuilder(SynergyFactory.isSynergy(registration) || randomAncientAbilities.contains(registration) ? SYNERGY_RANK_MATERIALS.get(manifest.rank()) : RANK_MATERIALS.get(manifest.rank()))
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
    	switch(selected.size()) {
    	case 0:
        	try {
    			player.setAbility(abilities[0]);
    		} catch (ReflectiveOperationException e1) {
    			e1.printStackTrace();
    		}
    		break;
    	case 1:
        	try {
        		player.setAbility(abilities[selected.getFirst()].getAbilityClass());
    		} catch (ReflectiveOperationException e1) {
    			e1.printStackTrace();
    		}
    		break;
    	}
    player.getPlayer().sendMessage("§2[§a!§2] §f능력을 확정하였습니다. 당신의 능력은 §b" + player.getAbility().getDisplayName() + "§f입니다.");
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
                    while (selected.size() >= 1) {
                        selected.removeFirst();
                    }
                    selected.add(slot - 10);
            	}
                openGUI();
            } else if (slot == 28) {
            	if (rerollused + 1 < rerollable) {
                	rerollused += 2;
                	ItemMeta meta = EXPLAIN.getItemMeta();
                	meta.setLore(new ArrayList<String>() {
		            	{
		            		add("§a==========================================");
		            		add("§7콘크리트 블럭을 §b§n좌클릭§7하면, 해당 §b능력§7을 §d선택§7합니다.");
		                    add("§7능력을 선택하였으면 §a라임색 유리판§f을 눌러 결정을 마칩니다.");
		                    add("§a==========================================");
		                    add("§7종이를 눌러 리롤 기회를 사용해 선택지들을 바꿀 수도 있습니다.");
		                    add("§7리롤 기회: §a" + (rerollable - rerollused) + "§7회");
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
		            		add("§7콘크리트 블럭을 §b§n좌클릭§7하면, 해당 §b능력§7을 §d선택§7합니다.");
		                    add("§7능력을 선택하였으면 §a라임색 유리판§f을 눌러 결정을 마칩니다.");
		                    add("§a==========================================");
		                    add("§7종이를 눌러 리롤 기회를 사용해 선택지들을 바꿀 수도 있습니다.");
		                    add("§7리롤 기회: §a" + (rerollable - rerollused) + "§7회");
		                    add("§a==========================================");
		                    add("§c자동 스킵까지 선택을 미루는 것을 추천드리지 않습니다...");
		                    add("§a==========================================");
		            	}
		            });
                	EXPLAIN.setItemMeta(meta);
            		reroll(RerollTarget.SELECTED);
            	}
            } else if (slot == 34) {
            	if (selected.size() == 1) {
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
            public void reroll(SelectGUI gui) {
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
                        	if (gui.random.nextInt(1000) < gui.synergychance) {
                        		if (gui.random.nextInt(10) == 0) gui.abilities[6 - (count - 10)] = gui.random.pick(gui.randomAncientAbilities);
                        		else gui.abilities[6 - (count - 10)] = gui.random.pick(gui.randomSynergies);
                        		SoundLib.ITEM_TOTEM_USE.playSound(gui.player.getPlayer(), 0.4f, 0.8f);
                        	} else gui.abilities[6 - (count - 10)] = gui.random.pick(gui.randomAbilities);
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
            public void reroll(SelectGUI gui) {
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
                        	if (gui.random.nextInt(1000) < gui.synergychance) {
                        		if (gui.random.nextInt(10) == 0) gui.abilities[i] = gui.random.pick(gui.randomAncientAbilities);
                        		else gui.abilities[i] = gui.random.pick(gui.randomSynergies);
                        		SoundLib.ITEM_TOTEM_USE.playSound(gui.player.getPlayer(), 0.4f, 0.8f);
                        	} else gui.abilities[i] = gui.random.pick(gui.randomAbilities);
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
            public void reroll(SelectGUI gui) {
            	gui.selected.clear();
                for (int i = 0; i < gui.abilities.length; i++) {
                	if (gui.random.nextInt(1000) < gui.synergychance) {
                		if (gui.random.nextInt(10) == 0) gui.abilities[i] = gui.random.pick(gui.randomAncientAbilities);
                		else gui.abilities[i] = gui.random.pick(gui.randomSynergies);
                		SoundLib.ITEM_TOTEM_USE.playSound(gui.player.getPlayer(), 0.4f, 0.8f);
                	} else gui.abilities[i] = gui.random.pick(gui.randomAbilities);
                }
            }
        };

        public abstract void reroll(SelectGUI gui);
    }

}
