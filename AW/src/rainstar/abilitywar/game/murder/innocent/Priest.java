package rainstar.abilitywar.game.murder.innocent;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
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
import daybreak.abilitywar.ability.AbilityFactory.AbilityRegistration;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.SubscribeEvent;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.ability.SubscribeEvent.Priority;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.list.murdermystery.Items;
import daybreak.abilitywar.game.list.murdermystery.MurderMystery;
import daybreak.abilitywar.game.list.murdermystery.MurderMystery.ArrowKillEvent;
import daybreak.abilitywar.game.list.murdermystery.ability.AbstractInnocent;
import daybreak.abilitywar.game.list.murdermystery.ability.AbstractMurderer;
import daybreak.abilitywar.game.list.murdermystery.ability.AbstractMurderer.MurderEvent;
import daybreak.abilitywar.game.list.murdermystery.ability.Innocent;
import daybreak.abilitywar.game.list.murdermystery.ability.Murderer;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.base.minecraft.item.Skulls;
import daybreak.abilitywar.utils.base.minecraft.nms.NMS;
import daybreak.abilitywar.utils.library.SoundLib;
import rainstar.abilitywar.game.murder.murderer.SpecterMurderer;
import rainstar.abilitywar.game.murder.system.config.statictype.GhostJobs;

@AbilityManifest(name = "시민: 사제", rank = Rank.SPECIAL, species = Species.HUMAN, explain = {
		"금 우클릭으로 금 8개를 소모해 활과 화살을 얻을 수 있습니다.",
		"§8[§e구원§8]§f 금 좌클릭으로 죽은 사람 한 명을 선택해 되살릴 수 있습니다.",
		"그 대상은 사망 전의 직업을 가지고 사망 위치에서 되살아납니다. §8(§71회용§8)",
		"§8[§b성불§8]§f §b유령§f이나 §c망령§f은 사망하지 않아도 선택할 수 있으며,",
		"사용 횟수 소모 없이 대상은 일반 §a시민§7 / §c머더§f가 되고 금 5개를 받습니다."
})
public class Priest extends AbstractInnocent {

	public Priest(Participant participant) {
		super(participant);
	}

	@Override
	protected void onUpdate(Update update) {
		super.onUpdate(update);
		if (update == Update.RESTRICTION_CLEAR) {
			NMS.sendTitle(getPlayer(), "§e역할§f: §a사제", "§f죽은 사람이나 영혼을 되살리세요.", 10, 80, 10);
			new AbilityTimer(1) {
				@Override
				protected void run(int count) {
				}

				@Override
				protected void onEnd() {
					NMS.clearTitle(getPlayer());
				}
			}.setInitialDelay(TimeUnit.SECONDS, 5).start();
		}
	}
	
	private GhostJobs ghostjobs = getGame().getModule(GhostJobs.class);
	private boolean onetime = true;
	private Map<Participant, Location> deathLocation = new HashMap<>();
	
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
				if (onetime) {
					new SkillGUI().start();	
				} else getPlayer().sendMessage("§4[§c!§4] §c이미 구원을 사용하였습니다.");
			}	
		}
	}
	
	@SubscribeEvent(priority = Priority.HIGHEST)
	private void onMurder(final MurderEvent e) {
		if (deathLocation.containsKey(e.getTarget())) deathLocation.remove(e.getTarget());
		deathLocation.put(e.getTarget(), e.getTarget().getPlayer().getLocation());
	}
	
	@SubscribeEvent(priority = Priority.HIGHEST)
	private void onArrowKill(final ArrowKillEvent e) {
		if (e.getShooter().hasAbility() && e.getTarget().hasAbility()) {
			if (!(e.getShooter().getAbility() instanceof AbstractMurderer) && !(e.getTarget().getAbility() instanceof AbstractMurderer)) {
				if (deathLocation.containsKey(e.getShooter())) deathLocation.remove(e.getShooter());
				deathLocation.put(e.getShooter(), e.getShooter().getPlayer().getLocation());
			}
		}
		if (deathLocation.containsKey(e.getTarget())) deathLocation.remove(e.getTarget());
		deathLocation.put(e.getTarget(), e.getTarget().getPlayer().getLocation());
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
					meta.setDisplayName("§b" + participant.getPlayer().getName());
					List<String> lore = new ArrayList<>();
					lore.add("§7");
					lore.add("§3» §b이 대상을 부활시키려면 선택하세요.");
					meta.setLore(lore);
					item.setItemMeta(meta);
	    			gui.setItem(slot, item);
					slots.put(slot, participant);
	    			slot++;
	    		} else if (participant.hasAbility() && (participant.getAbility().getClass().equals(Ghost.class) || participant.getAbility().getClass().equals(SpecterMurderer.class))) {
	    			ItemStack item = Skulls.createCustomSkull(participant.getPlayer().getName());
					ItemMeta meta = item.getItemMeta();
					meta.setDisplayName("§b" + participant.getPlayer().getName());
					List<String> lore = new ArrayList<>();
					lore.add("§7");
					lore.add("§3» §b이 대상을 부활시키려면 선택하세요.");
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
				SoundLib.ENTITY_EVOKER_PREPARE_SUMMON.playSound(getPlayer(), 1, 2);
				getPlayer().sendMessage("§a[§e!§a] §3" + selected.getPlayer().getName() + "§a님을 부활시킵니다...");
				selected.getPlayer().sendMessage("§a[§e!§a] §a누군가의 힘으로 되살아났습니다!");
				
				if (murderMystery.isDead(selected.getPlayer().getUniqueId())) {
					try {
						Class<?> clazz = murderMystery.getClass();
						Field field = clazz.getDeclaredField("deadPlayers");
						field.setAccessible(true);
						@SuppressWarnings("unchecked")
						Set<UUID> deadPlayers = (Set<UUID>) field.get(murderMystery);
						deadPlayers.remove(selected.getPlayer().getUniqueId());
						
						if (ghostjobs.getGhostJob(selected) != null) {
							AbilityRegistration job = ghostjobs.getGhostJob(selected);
							if (job.getClass().equals(Ghost.class)) {
								selected.setAbility(Innocent.class);
								murderMystery.addGold(selected, 5);
							} else if (job.getClass().equals(SpecterMurderer.class)) {
								selected.setAbility(Murderer.class);
								murderMystery.addGold(selected, 5);
							} else selected.setAbility(job);
						}
						if (deathLocation.get(selected) != null) selected.getPlayer().teleport(deathLocation.get(selected));
						
						selected.getPlayer().setGameMode(GameMode.ADVENTURE);
					} catch (ReflectiveOperationException | SecurityException | IllegalArgumentException | UnsupportedOperationException e) {
						e.printStackTrace();
					}
					onetime = false;
				} else {
					if (selected.hasAbility()) {
						if (selected.getAbility().getClass().equals(Ghost.class)) {
							try {
								selected.setAbility(Innocent.class);
							} catch (UnsupportedOperationException | ReflectiveOperationException e) {
								e.printStackTrace();
							}
							murderMystery.addGold(selected, 5);
						} else if (selected.getAbility().getClass().equals(SpecterMurderer.class)) {
							try {
								selected.setAbility(Murderer.class);
							} catch (UnsupportedOperationException | ReflectiveOperationException e) {
								e.printStackTrace();
							}
							murderMystery.addGold(selected, 5);
						}
					}
				}
				
				SoundLib.ENTITY_EVOKER_PREPARE_SUMMON.playSound(selected.getPlayer(), 1, 2);
				selected = null;
			}
			HandlerList.unregisterAll(this);
			getPlayer().closeInventory();
		}
		
	}

}
