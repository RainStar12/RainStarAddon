package rainstar.abilitywar.game.murder.innocent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringJoiner;

import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import daybreak.abilitywar.AbilityWar;
import daybreak.abilitywar.ability.AbilityBase;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.SubscribeEvent;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.game.AbstractGame.GameTimer;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.list.murdermystery.Items;
import daybreak.abilitywar.game.list.murdermystery.MurderMystery;
import daybreak.abilitywar.game.list.murdermystery.ability.AbstractInnocent;
import daybreak.abilitywar.game.list.murdermystery.ability.AbstractMurderer;
import daybreak.abilitywar.utils.base.collect.SetUnion;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.base.minecraft.item.Skulls;
import daybreak.abilitywar.utils.base.minecraft.nms.NMS;
import daybreak.abilitywar.utils.library.SoundLib;
import rainstar.abilitywar.game.murder.system.config.statictype.GhostJobs;

@AbilityManifest(name = "시민: 영매사", rank = Rank.SPECIAL, species = Species.HUMAN, explain = {
		"금 우클릭으로 금 8개를 소모해 활과 화살을 얻을 수 있습니다.",
		"§8[§b영접§8]§f 자신의 모든 채팅은 유령 채팅입니다. 유령과의 대화가 가능합니다.",
		"§8[§c계승§8]§f 금 좌클릭으로 금 4개를 소모해 죽은 사람 중 한 명을",
		"택해 대상의 직업을 내부 능력으로 사용할 수 있습니다. §8(§71회용§8)",
		"이때 선택된 직업이 §5머더 직업§f이라면 발동하지 않고 기회와 금만 소모됩니다.",
		"§b---------------------------------",
		"$(EXPLAIN)",
		"§b---------------------------------"
})
public class Mediumship extends AbstractInnocent {
	
	public Mediumship(Participant participant) {
		super(participant);
	}
	
	@Override
	protected void onUpdate(Update update) {
		super.onUpdate(update);
		if (update == Update.RESTRICTION_CLEAR) {
			if (ability != null) {
				ability.setRestricted(false);
			}
			NMS.sendTitle(getPlayer(), "§e직업§f: §a영매사", "§b죽은 자§f의 뜻을 계승하세요.", 10, 80, 10);
			new AbilityTimer(1) {
				@Override
				protected void onEnd() {
					NMS.clearTitle(getPlayer());
				}
			}.setInitialDelay(TimeUnit.SECONDS, 5).start();
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
	
	private boolean onetime = false;
	private AbilityBase ability;
	private GhostJobs ghostjobs = getGame().getModule(GhostJobs.class);
	
	public AbilityBase getSubAbility() {
		return ability;
	}
	
	@SuppressWarnings("unused")
	private final Object EXPLAIN = new Object() {
		@Override
		public String toString() {
			final StringJoiner joiner = new StringJoiner("\n");
			if (ability == null) joiner.add("계승된 직업이 없습니다.");
			else {
				joiner.add("§a계승한 직업 §f| §7[§b" + ability.getName() + "§7] " + ability.getRank().getRankName() + " " + ability.getSpecies().getSpeciesName());
				for (final Iterator<String> iterator = ability.getExplanation(); iterator.hasNext();) {
					joiner.add("§f" + iterator.next());
				}
			}
			return joiner.toString();
		}
	};
	
	@Override
	public Set<GameTimer> getTimers() {
		return ability != null ? SetUnion.union(super.getTimers(), ability.getTimers()) : super.getTimers();
	}

	@Override
	public Set<GameTimer> getRunningTimers() {
		return ability != null ? SetUnion.union(super.getRunningTimers(), ability.getRunningTimers()) : super.getRunningTimers();
	}
	
	@SubscribeEvent(onlyRelevant = true)
	private void onInteract(PlayerInteractEvent e) {
		if (Items.isGold(e.getItem())) {
			if (ability == null) {
				if (e.getAction() == Action.RIGHT_CLICK_AIR || e.getAction() == Action.RIGHT_CLICK_BLOCK) {
					final MurderMystery murderMystery = (MurderMystery) getGame();
					if (murderMystery.consumeGold(getParticipant(), 8)) {
						if (!addArrow()) {
							murderMystery.addGold(getParticipant());
						} else {
							if (!hasBow()) {
								getPlayer().getInventory().setItem(2, Items.NORMAL_BOW.getStack());
							}
						}
					}
				} else if (e.getAction() == Action.LEFT_CLICK_AIR || e.getAction() == Action.LEFT_CLICK_BLOCK) {
					if (!onetime) {
						new SkillGUI().start();	
					} else getPlayer().sendMessage("§4[§c!§4] §c이미 계승을 사용하였습니다.");
				}	
			}
		}
	}
	
	public class SkillGUI extends AbilityTimer implements Listener {
		
		private Map<Integer, Participant> slots = new HashMap<>();
		private final Inventory gui = Bukkit.createInventory(null, 54, "§0대상을 선택해주세요.");
		private Participant selected;
		
		public SkillGUI() {
			super(TaskType.REVERSE, 600);
			setPeriod(TimeUnit.TICKS, 1);
			Bukkit.getPluginManager().registerEvents(this, AbilityWar.getPlugin());
			openGUI();
		}
		
		private void openGUI() {
	    	gui.clear();
	    	int slot = 0;
	    	for (Participant participant : getGame().getParticipants()) {
	    		if (((MurderMystery) getGame()).isDead(participant.getPlayer().getUniqueId()) && !participant.equals(getParticipant())) {
	    			ItemStack item = Skulls.createCustomSkull(participant.getPlayer().getName());
					ItemMeta meta = item.getItemMeta();
					meta.setDisplayName("§7" + participant.getPlayer().getName());
					List<String> lore = new ArrayList<>();
					lore.add("§7");
					lore.add("§3» §b이 대상의 직업을 §c계승§b하려면 선택하세요.");
					meta.setLore(lore);
					item.setItemMeta(meta);
	    			gui.setItem(slot, item);
					slots.put(slot, participant);
	    			slot++;
	    		}
	    	}
	        getPlayer().openInventory(gui);
	    }
		
	    @EventHandler()
	    public void onInventoryClick(InventoryClickEvent e) {
	    	if (e.getClickedInventory().equals(gui)) {
	    		e.setCancelled(true);
	    		if (slots.containsKey(e.getSlot())) {
	    			selected = slots.get(e.getSlot());
		    		stop(true);
	    		}
	    	}
	    }
	    
	    @EventHandler()
	    public void onInventoryClose(InventoryCloseEvent e) {
	    	if (e.getInventory().equals(gui)) {
	    		stop(false);
	    	}
	    }
	    
		@Override
		protected void onEnd() {
			onSilentEnd();
		}
		
		@Override
		protected void onSilentEnd() {
			if (selected != null) {
				MurderMystery murderMystery = (MurderMystery) getGame();
				if (ghostjobs.getGhostJob(selected) != null) {
					if (murderMystery.consumeGold(getParticipant(), 4)) {
						if (AbstractMurderer.class.isAssignableFrom(ghostjobs.getGhostJob(selected).getAbilityClass())) {
							SoundLib.AMBIENT_CAVE.playSound(getPlayer(), 1, 2f);
							getPlayer().sendMessage("§4[§c!§4] §3" + selected.getPlayer().getName() + "§f님은 §c악령 머더§f이기에 계승하지 못했습니다.");	
						} else {
							SoundLib.BLOCK_ENCHANTMENT_TABLE_USE.playSound(getPlayer(), 1, 0.6f);
							getPlayer().sendMessage("§4[§c!§4] §3" + selected.getPlayer().getName() + "§f님의 뜻을 계승합니다...");
							try {
								ability = AbilityBase.create(ghostjobs.getGhostJob(selected), getParticipant());
								ability.setRestricted(false);
							} catch (ReflectiveOperationException e) {
								e.printStackTrace();
							}
						}
						onetime = true;
					} else getPlayer().sendMessage("§4[§c!§4] §c금이 부족합니다.");	
				} else getPlayer().sendMessage("§4[§c!§4] §c직업이 없는 대상입니다.");	
				selected = null;
			}
			HandlerList.unregisterAll(this);
			getPlayer().closeInventory();
		}
		
	}
	
}
