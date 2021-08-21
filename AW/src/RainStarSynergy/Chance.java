package RainStarSynergy;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringJoiner;

import javax.annotation.Nullable;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByBlockEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.SubscribeEvent;
import daybreak.abilitywar.AbilityWar;
import daybreak.abilitywar.ability.AbilityBase;
import daybreak.abilitywar.ability.AbilityFactory.AbilityRegistration;
import daybreak.abilitywar.ability.AbilityFactory.AbilityRegistration.Flag;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.ability.decorator.ActiveHandler;
import daybreak.abilitywar.ability.decorator.TargetHandler;
import daybreak.abilitywar.config.Configuration;
import daybreak.abilitywar.game.AbstractGame.GameTimer;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.list.mix.Mix;
import daybreak.abilitywar.game.list.mix.synergy.Synergy;
import daybreak.abilitywar.game.list.mix.synergy.SynergyFactory;
import daybreak.abilitywar.game.manager.AbilityList;
import daybreak.abilitywar.game.module.DeathManager;
import daybreak.abilitywar.utils.base.Messager;
import daybreak.abilitywar.utils.base.collect.Pair;
import daybreak.abilitywar.utils.base.collect.SetUnion;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.base.language.korean.KoreanUtil;
import daybreak.abilitywar.utils.base.language.korean.KoreanUtil.Josa;
import daybreak.abilitywar.utils.base.minecraft.item.builder.ItemBuilder;
import daybreak.abilitywar.utils.base.random.Random;
import daybreak.abilitywar.utils.library.MaterialX;
import daybreak.abilitywar.utils.library.SoundLib;
import daybreak.google.common.base.Predicate;
import net.md_5.bungee.api.ChatColor;

@AbilityManifest(name = "기회", rank = Rank.L, species = Species.GOD, explain = {
		"§7사망 위기 §8- §b기회§f: 모든 플레이어의 능력을 재추첨하고 절반의 체력으로 부활합니다.",
		" 재추첨 대상의 원래 능력이 시너지 능력일 경우 다른 시너지로 변경됩니다.",
		"§7패시브 §8- §b선택§f: 기회 발동 후 자신은 다섯 가지 시너지 선택지 중 하나를 택합니다.",
		"§7사망 §8- §c기회 압수§f: 자신은 어떤 부활계 효과도 받을 수 없습니다.",
		" 플레이어들의 기회로 바뀐 능력들이 원래대로 되돌아옵니다.",
		" 원래 능력이 기회였던 대상은 선택 효과가 즉시 발동합니다.",
		" 기회를 죽인 사람의 원 능력이 시너지였을 경우, 대상도 선택 효과를 발동합니다.",
		"$(NOW_ABILITY)"})

public class Chance extends Synergy implements ActiveHandler, TargetHandler {
	
	public Chance(Participant participant) {
		super(participant);
	}
	
	private Player killer = null;
	private boolean first = true;
	private Map<Participant, Pair<AbilityBase, AbilityBase>> abilitiesMap = new HashMap<>();
	private Map<Participant, AbilityRegistration> synergiesMap = new HashMap<>();
	private Set<Participant> chanceSet = new HashSet<>();
	
	private final Predicate<Entity> predicate = new Predicate<Entity>() {
		@Override
		public boolean test(Entity entity) {
			if (entity instanceof Player) {
				if (!getGame().isParticipating(entity.getUniqueId())
						|| (getGame() instanceof DeathManager.Handler && ((DeathManager.Handler) getGame()).getDeathManager().isExcluded(entity.getUniqueId()))) {
					return false;
				}
			}
			return true;
		}

		@Override
		public boolean apply(@Nullable Entity arg0) {
			return false;
		}
	};
	
    public Class<? extends AbilityBase> getRandomAbility() {

        ArrayList<Class<? extends AbilityBase>> abilities = new ArrayList<>();
        for (String name : AbilityList.nameValues()) {
            if (!Configuration.Settings.isBlacklisted(name)) {
                abilities.add(AbilityList.getByString(name));
            }
        }
        for (Participant participant : getGame().getParticipants()) {
            if (participant.hasAbility() && participant.attributes().TARGETABLE.getValue()) {
                abilities.remove(participant.getAbility().getClass());
            }
        }

        Random r = new Random();
        return abilities.get(r.nextInt(abilities.size()));

    }

    public AbilityRegistration getRandomSynergy() {

		Set<AbilityRegistration> synergies = new HashSet<>();
        Random r = new Random();
		
        for (AbilityRegistration synergy : SynergyFactory.getSynergies()) {
        	String name = synergy.getManifest().name();
        	if (!Configuration.Settings.isBlacklisted(name)) {
        		synergies.add(synergy);
        	}
        }
        
        return synergies.toArray(new AbilityRegistration[]{})[r.nextInt(synergies.size())];
    }
    
    private AbilityTimer deathTimer = new AbilityTimer(300) {
    	
    	@Override
    	public void run(int count) {
    		getPlayer().setGameMode(GameMode.SPECTATOR);
    	}
    	
    	@Override
    	public void onEnd() {
    		onSilentEnd();
    	}
    	
    	@Override
    	public void onSilentEnd() {
    		getPlayer().setGameMode(GameMode.SURVIVAL);
    		if (killer != null) getPlayer().damage(10, killer);
    		getPlayer().setHealth(0);
    	}
    	
	}.setPeriod(TimeUnit.TICKS, 1).register();
    
	@SubscribeEvent(priority = 999)
	public void onEntityDamageByEntity(EntityDamageByEntityEvent e) {
		if (e.getEntity().equals(getPlayer()) && getPlayer().getHealth() - e.getFinalDamage() <= 0 && !first) {
			if (e.getDamager() instanceof Player) {
				killer = (Player) e.getDamager();
			}
			if (e.getDamager() instanceof Projectile) {
				Projectile projectile = (Projectile) e.getDamager();
				if (projectile.getShooter() != null) {
					if (projectile.getShooter() instanceof Player) {
						killer = (Player) projectile.getShooter();
					}
				}
			}
		}
		onEntityDamage(e);
	}
	
	@SubscribeEvent(priority = 999)
	public void onEntityDamageByBlock(EntityDamageByBlockEvent e) {
		onEntityDamage(e);
	}
    
	@SubscribeEvent(priority = 999)
	public void onEntityDamage(EntityDamageEvent e) {
		if (e.getEntity().equals(getPlayer()) && getPlayer().getHealth() - e.getFinalDamage() <= 0) {
			if (first) {
				first = false;
				getPlayer().setHealth(getPlayer().getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue() / 2);
				Bukkit.broadcastMessage("§b[§e!§b] §e" + getPlayer().getName() + "§f로 인해 모든 플레이어에게 §b기회§f가 다시금 주어집니다...");
				SoundLib.UI_TOAST_CHALLENGE_COMPLETE.broadcastSound();
				e.setCancelled(true);
				getGame().getParticipants().forEach(participant -> {
						try {
							Mix mix = (Mix) participant.getAbility();
							if (predicate.test(participant.getPlayer())) {
								if (participant.getPlayer().equals(getPlayer())) {
									new AbilitySelect(getPlayer()).start();
								} else {
									if (mix.hasSynergy()) {
										if (mix.getSynergy().getClass().equals(Chance.class)) chanceSet.add(participant);
										synergiesMap.put(participant, mix.getSynergy().getRegistration());
										AbilityRegistration synergy = getRandomSynergy();
										mix.setAbility(SynergyFactory.getSynergyBase(synergy).getLeft().getAbilityClass(), SynergyFactory.getSynergyBase(synergy).getRight().getAbilityClass());
										participant.getPlayer().sendMessage("§b[§e!§b] §f당신의 새 능력§7: §e" + synergy.getManifest().name());
									} else {
										abilitiesMap.put(participant, Pair.of(mix.getFirst(), mix.getSecond()));
										mix.setAbility(getRandomAbility(), getRandomAbility());
										participant.getPlayer().sendMessage("§b[§e!§b] §f당신의 새 능력§7: §e" + mix.getFirst().getDisplayName() + "§7, §e" + mix.getSecond().getDisplayName());
									}
									participant.getPlayer().sendMessage("§c[§e!§c] §f기회가 새로운 능력을 선택하기 전까지 §3무적§f 상태가 됩니다.");
								}	
							}
						} catch (ReflectiveOperationException e1) {
							e1.printStackTrace();
						}
				});
			} else {
				Bukkit.broadcastMessage("§b[§e!§b] §a기회§f가 사망하여 모든 플레이어의 능력이 제자리로 돌아옵니다...");
				Bukkit.broadcastMessage("§b[§e!§b] §c15초§f 후 기회의 주인 §e" + getPlayer().getName() + "§f님이 사망합니다.");
				SoundLib.ENTITY_WITHER_DEATH.broadcastSound();
				if (killer != null && !killer.equals(getPlayer())) {
					if (synergiesMap.containsKey(getGame().getParticipant(killer))) {
						new AbilitySelect(killer).start();
					}
				}
				for (Participant participant : abilitiesMap.keySet()) {
					if (predicate.test(participant.getPlayer())) {
						Mix mix = (Mix) participant.getAbility();
						try {
							mix.setAbility(abilitiesMap.get(participant).getLeft().getClass(), abilitiesMap.get(participant).getRight().getClass());
						} catch (ReflectiveOperationException e1) {
							e1.printStackTrace();
						}	
					}
				}
				for (Participant participant : synergiesMap.keySet()) {
					if (predicate.test(participant.getPlayer())) {
						if (chanceSet.contains(participant)) {
							new AbilitySelect(participant.getPlayer()).start();
						} else {
							Mix mix = (Mix) participant.getAbility();
							try {
								mix.setSynergy(synergiesMap.get(participant));
							} catch (ReflectiveOperationException e1) {
								e1.printStackTrace();
							}	
						}	
					}
				}
				deathTimer.start();
				e.setCancelled(true);
			}
		}
	}
	
	public class AbilitySelect extends AbilityTimer implements Listener {
		
		private final ItemStack NULL = (new ItemBuilder(MaterialX.GRAY_STAINED_GLASS_PANE)).displayName(" ").build();
		
		private Player player;
		
		private final List<AbilityRegistration> values = new ArrayList<>();
		private Set<AbilityRegistration> synergies = new HashSet<>();
		
		private Map<Integer, AbilityRegistration> slots = new HashMap<>();
		
		private AbilityRegistration selected;
		
		private final Inventory gui;
		
		public AbilitySelect(Player player) {
			super(TaskType.REVERSE, 300);
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
			if (player.equals(getPlayer())) {
				Bukkit.broadcastMessage("§b[§e!§b] §f기회가 새로운 능력을 선택하여 §3무적§f이 §a해제§f되었습니다.");
				try {
					Chance.this.ability = AbilityBase.create(selected.getAbilityClass(), getParticipant());
				} catch (UnsupportedOperationException | ReflectiveOperationException e1) {
					e1.printStackTrace();
				}	
			} else {
				Mix mix = (Mix) getGame().getParticipant(player).getAbility();
				try {
					mix.setAbility(SynergyFactory.getSynergyBase(selected).getLeft().getAbilityClass(), SynergyFactory.getSynergyBase(selected).getRight().getAbilityClass());
				} catch (UnsupportedOperationException | ReflectiveOperationException e1) {
					e1.printStackTrace();
				}
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
					gui.setItem(2, item);
					slots.put(2, values.get(i));
					break;
				case 2:
					gui.setItem(4, item);
					slots.put(4, values.get(i));
					break;
				case 3:
					gui.setItem(6, item);
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
	
	//선택 후
	
	private AbilityBase ability;

	@SuppressWarnings("unused")
	private final Object NOW_ABILITY = new Object() {
		@Override
		public String toString() {
			if (ability == null) {
				return "§7아직 기회를 사용하지 않았습니다.".toString();
			} else {
				final StringJoiner joiner = new StringJoiner("\n");
				joiner.add("§a바뀐 능력 §f| §b" + ability.getName() + " " + ability.getRank().getRankName() + " " + ability.getSpecies().getSpeciesName());
				joiner.add("§b⋙ §f바뀐 능력 설명은 §e/aw synergies " + ability.getName() + "§f" + KoreanUtil.getJosa(ability.getName(), Josa.으로로) + " 볼 수 있습니다.");
				return joiner.toString();
			}
		}
	};
	
	@Override
	public String getDisplayName() {
		return ability != null ? ("기회(" + ability.getName() + ")") : ("기회");
	}
	@Override
	public boolean ActiveSkill(Material material, ClickType clickType) {
		if (ability != null) {
			return ability instanceof ActiveHandler && ((ActiveHandler) ability).ActiveSkill(material, clickType);	
		}
		return false;
	}

	@Override
	public void TargetSkill(Material material, LivingEntity entity) {
		if (ability != null) {
			if (ability instanceof TargetHandler) {
			((TargetHandler) ability).TargetSkill(material, entity);
			}
		}
	}

	@Override
	public Set<GameTimer> getTimers() {
		return ability != null ? SetUnion.union(super.getTimers(), ability.getTimers()) : super.getTimers();
	}

	@Override
	public Set<GameTimer> getRunningTimers() {
		return ability != null ? SetUnion.union(super.getRunningTimers(), ability.getRunningTimers()) : super.getRunningTimers();
	}

	@Override
	public boolean usesMaterial(Material material) {
		if (ability != null) {
			return ability.usesMaterial(material);
		}
		return super.usesMaterial(material);
	}

	@Override
	protected void onUpdate(Update update) {
		if (update == Update.RESTRICTION_CLEAR) {
			if (ability != null) {
				ability.setRestricted(false);
			}
		} else if (update == Update.RESTRICTION_SET) {
			if (ability != null) {
				ability.setRestricted(true);
			}
		} else if (update == Update.ABILITY_DESTROY) {
			if (ability != null) {
				ability.destroy();
				ability = null;
			}
		}
	}
	
}