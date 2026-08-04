package rainstar.abilitywar.game.murder.murderer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.SubscribeEvent;
import daybreak.abilitywar.AbilityWar;
import daybreak.abilitywar.ability.AbilityBase;
import daybreak.abilitywar.ability.AbilityFactory;
import daybreak.abilitywar.ability.AbilityFactory.AbilityRegistration;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.list.murdermystery.Items;
import daybreak.abilitywar.game.list.murdermystery.JobList;
import daybreak.abilitywar.game.list.murdermystery.MurderMystery;
import daybreak.abilitywar.game.list.murdermystery.ability.AbstractDetective;
import daybreak.abilitywar.game.list.murdermystery.ability.AbstractInnocent;
import daybreak.abilitywar.game.list.murdermystery.ability.AbstractMurderer;
import daybreak.abilitywar.game.list.murdermystery.ability.Detective;
import daybreak.abilitywar.game.list.murdermystery.ability.Innocent;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.base.math.LocationUtil;
import daybreak.abilitywar.utils.base.minecraft.item.builder.ItemBuilder;
import daybreak.abilitywar.utils.base.minecraft.nms.NMS;
import daybreak.abilitywar.utils.library.MaterialX;
import daybreak.abilitywar.utils.library.ParticleLib;
import daybreak.abilitywar.utils.library.SoundLib;
import rainstar.abilitywar.game.murder.system.Shield;

@AbilityManifest(name = "머더: 스파이", rank = Rank.SPECIAL, species = Species.HUMAN, explain = {
		"모든 시민을 처리하세요!",
		"금 우클릭으로 금 8개를 소모해 활과 화살을 얻을 수 있습니다.",
		"§8[§2염탐§8]§f 13칸 내 다른 대상을 바라보고 금을 좌클릭하면 금을 3개 소모하여,",
		"대상의 직업명 중 무작위 §b한 글자§f를 알아냅니다.",
		"§8[§d처리§8]§f 금 F키로 전체 §a시민 §f/ §3탐정§f 능력이 존재하는 GUI를 띄워",
		"선택한 직업의 모든 대상의 §c실드를 파괴§f하고, 이후 §c처리§7(살해)§f합니다.",
		"만일 선택한 직업의 종사자가 아무도 없다면 §8[§7처리 실패§8]§f가 작동합니다.",
		"§8[§7처리 실패§8]§f 처음 1회, 자신의 이름과 실패 내용을 전부 공개합니다.",
		"이후에는 본인이 사망합니다.",
		"§8[§c혈흔§8]§f 처리된 대상의 시체에 혈흔이 남겨집니다."
})
public class SpyMurderer extends AbstractMurderer {

	public SpyMurderer(Participant participant) {
		super(participant);
	}
	
	@Override
	protected void onUpdate(Update update) {
		super.onUpdate(update);
		if (update == Update.RESTRICTION_CLEAR) {
			NMS.sendTitle(getPlayer(), "§e직업§f: §5스파이", "§f모든 §a시민§f의 정보를 빼돌리세요.", 10, 80, 10);
			new AbilityTimer(1) {
				@Override
				protected void onEnd() {
					NMS.clearTitle(getPlayer());
				}
			}.setInitialDelay(TimeUnit.SECONDS, 5).start();
		} else if (update == Update.ABILITY_DESTROY) {
			NMS.setInvisible(getPlayer(), false);
		}
	}
	
	private Set<Class<? extends AbilityBase>> usedability = new HashSet<>();
	private boolean onetime = true;
	
	private final Predicate<Entity> predicate = new Predicate<Entity>() {
		@Override
		public boolean test(Entity entity) {
			if (entity.equals(getPlayer())) return false;
			return !(entity instanceof Player) || (getGame().isParticipating(entity.getUniqueId())
					&& !((MurderMystery) getGame()).isDead(entity.getUniqueId())
					&& getGame().getParticipant(entity.getUniqueId()).attributes().TARGETABLE.getValue());
		}
	};
	
    @SubscribeEvent(onlyRelevant = true, priority = 0)
    public void onPlayerSwapHandItems(PlayerSwapHandItemsEvent e) {
    	if (Items.isGold(e.getOffHandItem()) && e.getPlayer().equals(getPlayer())) {
    		new SkillGUI().start();
    	}
    }
		
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
				Player player = LocationUtil.getEntityLookingAt(Player.class, getPlayer(), 13, predicate);
				if (player != null) {
					if (murderMystery.consumeGold(getParticipant(), 3)) {
						if (getGame().getParticipant(player).getAbility() instanceof AbstractMurderer) {
							getPlayer().sendMessage("§4[§c!§4] §c대상은 같은 머더 팀입니다.");	
						} else {
							if (getGame().getParticipant(player).getAbility().getClass().equals(Innocent.class)) {
								getPlayer().sendMessage("§4[§c!§4] §a" + player.getName() + "§f님은 아직 직업이 없습니다.");	
							} else {
								String jobname = getGame().getParticipant(player).getAbility().getDisplayName();
								char namechar;
								while(true) {
									namechar = jobname.charAt(random.nextInt(jobname.length()));
									if (namechar != ' ' && namechar != '시' && namechar != '민' && namechar != ':' && namechar != '<' && namechar != '>') break;
								}
								getPlayer().sendMessage("§4[§c!§4] §a" + player.getName() + "§f님의 직업 명 중 한 글자는 §5[§c" + namechar + "§5] §f입니다.");		
							}
						}
					}
				} else getPlayer().sendMessage("§4[§c!§4] §c바라보는 방향에 사람이 없습니다.");	
			}
		}
	}
	
	public class SkillGUI extends AbilityTimer implements Listener {

		private Map<Integer, Class<? extends AbilityBase>> slots = new HashMap<>();
		private Class<? extends AbilityBase> selected;
		private final Inventory gui = Bukkit.createInventory(null, 54, "§0직업을 선택해주세요.");
		private int currentPage = 1;
		
		private final ItemStack PREVIOUS_PAGE = new ItemBuilder(MaterialX.ARROW)
				.displayName(ChatColor.AQUA + "이전 페이지")
				.build();

		private final ItemStack NEXT_PAGE = new ItemBuilder(MaterialX.ARROW)
				.displayName(ChatColor.AQUA + "다음 페이지")
				.build();
		
		private final ItemStack NULL = (new ItemBuilder(MaterialX.GRAY_STAINED_GLASS_PANE)).displayName(" ").build();
		
		public SkillGUI() {
			super(TaskType.REVERSE, 600);
			setPeriod(TimeUnit.TICKS, 1);
			Bukkit.getPluginManager().registerEvents(this, AbilityWar.getPlugin());
			openGUI(1);
		}
		
		private void openGUI(int page) {
	    	gui.clear();
	    	slots.clear();
	    	
	    	List<String> jobnames = new ArrayList<>();
	    	for (AbilityRegistration registration : JobList.values()) {
	    		if (AbstractInnocent.class.isAssignableFrom(registration.getAbilityClass()) && !registration.getAbilityClass().equals(Detective.class)) {
		    		jobnames.add(registration.getManifest().name());	
	    		}
	    	}
	    	for (AbilityRegistration registration : AbilityFactory.getRegistrations()) {
	    		if (AbstractDetective.class.isAssignableFrom(registration.getAbilityClass())) {
		    		jobnames.add(registration.getManifest().name());	
	    		}
	    	}
	    	jobnames.add("시민: 개구리");
	    	Collections.sort(jobnames);
	    	int maxPage = ((jobnames.size() - 1) / 36) + 1;
			if (maxPage < page) page = 1;
			if (page < 1) page = 1;
			
			currentPage = page;
	    	int count = 0;
	    	
	       	for (String jobname : jobnames) {
	       		if (count / 36 == page - 1) {
	       			AbilityRegistration registration = AbilityFactory.getByName(jobname);
					ItemStack item = new ItemBuilder(MaterialX.IRON_BLOCK).build();
					ItemMeta meta = item.getItemMeta();
					meta.setDisplayName("§a" + registration.getManifest().name());
					List<String> lore = new ArrayList<>();
					lore.add("§7");
					lore.add("§4» §c이 직업을 죽이려면 선택하세요.");
					meta.setLore(lore);
					item.setItemMeta(meta);
	    			gui.setItem(count % 36, item);
					slots.put(count % 36, registration.getAbilityClass());
	       		}
	       		count++;
	    	}
	       	
	       	if (page > 1) {
				gui.setItem(48, PREVIOUS_PAGE);
			}

			if (page != maxPage) {
				gui.setItem(50, NEXT_PAGE);
			}
			
			for (int a = 36; a < 45; a++) {
				gui.setItem(a, NULL);
			}
			
			ItemStack stack = new ItemStack(Material.PAPER, 1);
			ItemMeta meta = stack.getItemMeta();
			meta.setDisplayName("§6페이지 §e" + page + " §6/ §e" + maxPage);
			stack.setItemMeta(meta);
			gui.setItem(49, stack);
	        getPlayer().openInventory(gui);
	    }
		
	    @EventHandler()
	    public void onInventoryClick(InventoryClickEvent e) {
	    	if (e.getClickedInventory().equals(gui)) {
	    		e.setCancelled(true);
	    		if (e.getCurrentItem() != null) {
		    		if (slots.containsKey(e.getSlot())) {
		    			selected = slots.get(e.getSlot());
			    		getPlayer().closeInventory();
			    		stop(false);
		    		} else if (e.getCurrentItem().getItemMeta() != null && e.getCurrentItem().getItemMeta().getDisplayName().equals(ChatColor.AQUA + "이전 페이지")) {
						openGUI(currentPage - 1);
					} else if (e.getCurrentItem().getItemMeta() != null && e.getCurrentItem().getItemMeta().getDisplayName().equals(ChatColor.AQUA + "다음 페이지")) {
						openGUI(currentPage + 1);
					}	
	    		}
	    	}
	    }
	    
		@Override
		protected void onEnd() {
			onSilentEnd();
		}
		
		@Override
		protected void onSilentEnd() {
			if (selected != null) {
				if (!usedability.contains(selected)) {
					boolean handlefail = true;
					getPlayer().sendMessage("§4[§c!§4] §d처리§f를 사용하였습니다.");
					for (Participant participant : getGame().getParticipants()) {
						if (participant.hasAbility()) {
							if (selected.equals(participant.getAbility().getClass())) {
								if (Shield.getShield(participant) > 0) Shield.setShield(participant, 0);
								MurderEvent event = new MurderEvent(SpyMurderer.this, participant);
								Bukkit.getPluginManager().callEvent(event);
								if (!event.isCancelled()) {
									new Blood(participant.getPlayer().getLocation()).start();
									SoundLib.ENTITY_PLAYER_ATTACK_SWEEP.playSound(participant.getPlayer().getLocation(), 1, 2);
									participant.getPlayer().setHealth(0);
									getPlayer().sendMessage("§4[§c!§4] §e" + participant.getPlayer().getName() + "§f님을 처리하였습니다.");	
								} else getPlayer().sendMessage("§4[§c!§4] §e" + participant.getPlayer().getName() + "§f님을 처리 시도하였습니다.");
								handlefail = false;
							}	
						}
					}
					if (handlefail) {
						getPlayer().sendMessage("§4[§c!§4] §c처리를 실패했습니다!");
						if (onetime) {
							Bukkit.broadcastMessage("§4[§c!§4] §d스파이 §c" + getPlayer().getName() + "§f님이 처리에 실패했습니다.");
							onetime = false;
						} else {
							getPlayer().setHealth(0);	
						}
					}
					SoundLib.ENTITY_PLAYER_ATTACK_SWEEP.playSound(getPlayer(), 1, 2);
					usedability.add(selected);
				} else getPlayer().sendMessage("§4[§c!§4] §f이미 처리를 시도한 적 있는 직업입니다.");
			}
			HandlerList.unregisterAll(this);
			getPlayer().closeInventory();
		}
		
	}
	
	private class Blood extends AbilityTimer {

		private final Location location;

		private Blood(final Location location) {
			super(TaskType.INFINITE, -1);
			setPeriod(TimeUnit.TICKS, 1);
			this.location = location;
		}

		@Override
		protected void run(int count) {
			ParticleLib.BLOCK_CRACK.spawnParticle(location, 0, 0, 0, 1, 0, MaterialX.REDSTONE_BLOCK);		
		}

	}
	
}
