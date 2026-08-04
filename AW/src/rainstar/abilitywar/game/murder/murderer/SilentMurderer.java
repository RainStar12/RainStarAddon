package rainstar.abilitywar.game.murder.murderer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringJoiner;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;

import daybreak.abilitywar.AbilityWar;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.SubscribeEvent;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.AbstractGame.Participant.ActionbarNotification.ActionbarChannel;
import daybreak.abilitywar.game.list.murdermystery.Items;
import daybreak.abilitywar.game.list.murdermystery.MurderMystery;
import daybreak.abilitywar.game.list.murdermystery.ability.AbstractInnocent;
import daybreak.abilitywar.game.list.murdermystery.ability.AbstractMurderer;
import daybreak.abilitywar.utils.base.color.RGB;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.base.minecraft.item.Skulls;
import daybreak.abilitywar.utils.base.minecraft.nms.NMS;
import daybreak.abilitywar.utils.library.ParticleLib;
import daybreak.abilitywar.utils.library.SoundLib;

@AbilityManifest(name = "머더: 사일런트", rank = Rank.SPECIAL, species = Species.HUMAN, explain = {
		"모든 시민을 죽이세요!",
		"금 우클릭으로 금 8개를 소모해 활과 화살을 얻을 수 있습니다.",
		"§8[§3비문§8]§f 금 좌클릭으로 금 2개를 소모해 §b글자§f 하나를 정합니다.",
		"재사용 시 §b글자§f를 추가합니다. 해당 글자가 들어가는 모든 채팅을 지우고,",
		"채팅을 친 대상이 §a시민팀§f이라면 영구적으로 §c채팅을 차단§f시킵니다.",
		"§8[§c기만§8]§f 금 F키로 §8[§3비문§8]§f으로 침묵시킨 대상을 선택 가능합니다.",
		"채팅을 칠 때 선택한 대상이 치는 채팅처럼 보여집니다.",
		"웅크린 채 금 F키를 누르면 기만 상태를 켜고 끌 수 있습니다.",
		"§8[§4그리고 아무도 없었다§8]§f 남은 모든 §a시민팀§f이",
		"침묵당한 상태라면 전부 처형하고 §e승리합니다§f."
})
public class SilentMurderer extends AbstractMurderer {
	
	public SilentMurderer(Participant participant) {
		super(participant);
	}
	
	@Override
	protected void onUpdate(Update update) {
		super.onUpdate(update);
		if (update == Update.RESTRICTION_CLEAR) {
			NMS.sendTitle(getPlayer(), "§e직업§f: §3사일런트", "§f모든 §a시민§f과 §5탐정§f을 조용히 시키세요.", 10, 80, 10);
			new AbilityTimer(1) {
				@Override
				protected void onEnd() {
					NMS.clearTitle(getPlayer());
				}
			}.setInitialDelay(TimeUnit.SECONDS, 5).start();
			PASSIVE.start();
			passive.start();
			ac2.update("§8[§7기만§8] §cOFF");
		} else if (update == Update.ABILITY_DESTROY) {
			NMS.setInvisible(getPlayer(), false);
		}
	}
	
	private List<CharSequence> epitaphs = new ArrayList<>();
	private boolean selecting = false;
	private Set<Participant> muted = new HashSet<>();
	private final ActionbarChannel ac = newActionbarChannel(), ac2 = newActionbarChannel();
	private static final RGB cyan = RGB.of(1, 176, 156);
	private Participant selected;
	private Map<Participant, String> formatMap = new HashMap<>();
	private boolean deceive = false;
	private boolean allkillstarted = false;
	
	public AbilityTimer passive = new AbilityTimer() {
		
		@Override
		public void run(int count) {
			for (Participant participant : muted) {
				ParticleLib.REDSTONE.spawnParticle(getPlayer(), participant.getPlayer().getLocation().add(0, 2.2, 0), cyan);
			}
			
			if (!allkillstarted) {
				boolean allkill = true;
				MurderMystery murderMystery = (MurderMystery) getGame();
				for (Participant participant : getGame().getParticipants()) {
					if (!murderMystery.isDead(participant.getPlayer().getUniqueId()) && participant.getAbility() instanceof AbstractInnocent && !muted.contains(participant)) {
						allkill = false;
						break;
					}
				}
				if (allkill) {
					allkill();
				}	
			}
		}
		
	}.setPeriod(TimeUnit.TICKS, 1).register();
	
	@SubscribeEvent(onlyRelevant = true)
	private void onInteract(PlayerInteractEvent e) {
		if (Items.isGold(e.getItem())) {
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
				final MurderMystery murderMystery = (MurderMystery) getGame();
				if (murderMystery.consumeGold(getParticipant(), 2)) {
					selecting = true;
					ac.update("§c금지시킬 글자를 정해주세요.");
				}
			}
		}
	}
	
	public void allkill() {
		allkillstarted = true;
		MurderMystery murderMystery = (MurderMystery) getGame();
		for (Player player : Bukkit.getOnlinePlayers()) {
			player.sendMessage("§c§o그리고 아무도 없었다.");
		}
		SoundLib.AMBIENT_CAVE.broadcastSound();
		new BukkitRunnable() {
			@Override
			public void run() {
				for (Participant participant : getGame().getParticipants()) {
					if (!murderMystery.isDead(participant.getPlayer().getUniqueId()) && participant.getAbility() instanceof AbstractInnocent) {
						MurderEvent event = new MurderEvent(SilentMurderer.this, participant);
						Bukkit.getPluginManager().callEvent(event);
						event.setCancelled(true);
						participant.getPlayer().setHealth(0);
					}
				}
			}	
		}.runTaskLater(AbilityWar.getPlugin(), 100L);
	}
	
	@SubscribeEvent
	private void onAsyncPlayerChat(AsyncPlayerChatEvent e) {
		if (selecting && e.getPlayer().equals(getPlayer())) {
			if (e.getMessage().length() == 1) {
				e.setCancelled(true);
				epitaphs.add(e.getMessage());
				final StringJoiner joiner = new StringJoiner("§f, ");
				for (CharSequence chars : epitaphs) {
					joiner.add("§c" + chars);
				}
				ac.update("§3금칙어§7: §c" + joiner);
				selecting = false;
				getPlayer().sendMessage("§4[§c!§4] §f금칙어에 §3" + e.getMessage() + "§f를 추가하였습니다.");
			}
		}
		if (getGame().isParticipating(e.getPlayer()) && !((MurderMystery) getGame()).isDead(e.getPlayer().getUniqueId())) {
			if (muted.contains(getGame().getParticipant(e.getPlayer()))) {
				e.setCancelled(true);
				e.getPlayer().sendMessage("§c침묵당했습니다!");
			} else if (epitaphs.size() > 0) {
				boolean contains = false;
				for (CharSequence chars : epitaphs) {
					if (e.getMessage().contains(chars)) {
						contains = true;
						break;
					}
				}
				if (contains) {
					if (!(getGame().getParticipant(e.getPlayer()).getAbility() instanceof AbstractMurderer)) {
						e.setCancelled(true);
						String format = e.getFormat().replace(e.getMessage(), "");
						formatMap.put(getGame().getParticipant(e.getPlayer()), format);
						e.getPlayer().sendMessage("§8[§3비문§8] §c금기를 말하여 침묵당했습니다!");
						getPlayer().sendMessage("§4[§c!§4] §e" + e.getPlayer().getName() + "§f님을 침묵시켰습니다.");
						muted.add(getGame().getParticipant(e.getPlayer()));
					}	
				}
			}	
		}
		if (selected != null && e.getPlayer().equals(getPlayer()) && deceive) e.setFormat(formatMap.get(selected) + e.getMessage());
	}
	
    @SubscribeEvent(onlyRelevant = true, priority = 0)
    public void onPlayerSwapHandItems(PlayerSwapHandItemsEvent e) {
    	if (Items.isGold(e.getOffHandItem()) && e.getPlayer().equals(getPlayer())) {
    		if (getPlayer().isSneaking()) {
    			if (deceive) {
    				deceive = false;
        			getPlayer().sendMessage("§4[§c!§4] §b기만이 해제되었습니다.");	
        			ac2.update("§8[§7기만§8] §cOFF");
    			} else {
    				deceive = true;
    				getPlayer().sendMessage("§4[§c!§4] §3" + selected.getPlayer().getName() + "§f님을 기만합니다.");
					getPlayer().sendMessage("§4[§c!§4] §b이제부터 당신이 치는 채팅은 §c" + selected.getPlayer().getName() + "§b님이 치는 것처럼 보여집니다.");
					ac2.update("§3[§b기만§3] §e" + selected.getPlayer().getName());
    			}
    		} else {
    			new SkillGUI().start();
    		}
    		e.setCancelled(true);
    	}
    }
    
	public class SkillGUI extends AbilityTimer implements Listener {
		
		private Map<Integer, Participant> slots = new HashMap<>();
		private final Inventory gui = Bukkit.createInventory(null, 54, "§0대상을 선택해주세요.");
		
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
	    		if (!((MurderMystery) getGame()).isDead(participant.getPlayer().getUniqueId()) && !participant.equals(getParticipant())
	    				&& !(participant.getAbility() instanceof AbstractMurderer) && muted.contains(participant) && formatMap.containsKey(participant)) {
	    			ItemStack item = Skulls.createCustomSkull(participant.getPlayer().getName());
					ItemMeta meta = item.getItemMeta();
					meta.setDisplayName("§a" + participant.getPlayer().getName());
					List<String> lore = new ArrayList<>();
					lore.add("§7");
					lore.add("§3» §b이 대상으로 채팅을 §c기만§b하려면 선택하세요.");
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
	    	if (gui != null) {
		    	if (gui.equals(e.getClickedInventory())) {
		    		e.setCancelled(true);
		    		if (slots.containsKey(e.getSlot())) {
		    			selected = slots.get(e.getSlot());
			    		stop(true);
		    		}
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
				if (!murderMystery.isDead(selected.getPlayer().getUniqueId())) {
					SoundLib.BLOCK_SAND_BREAK.playSound(getPlayer(), 1, 0.6f);
					getPlayer().sendMessage("§4[§c!§4] §3" + selected.getPlayer().getName() + "§f님을 기만 대상으로 지정하였습니다.");
				} else getPlayer().sendMessage("§4[§c!§4] §c이미 죽은 대상을 기만할 순 없습니다.");
			}
			HandlerList.unregisterAll(this);
			getPlayer().closeInventory();
		}
		
	}
	
}