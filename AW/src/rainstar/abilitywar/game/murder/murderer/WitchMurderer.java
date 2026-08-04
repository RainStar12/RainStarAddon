package rainstar.abilitywar.game.murder.murderer;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Ocelot;
import org.bukkit.entity.Ocelot.Type;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerGameModeChangeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;

import daybreak.abilitywar.AbilityWar;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.SubscribeEvent;
import daybreak.abilitywar.ability.AbilityFactory.AbilityRegistration;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.AbstractGame.Participant.ActionbarNotification.ActionbarChannel;
import daybreak.abilitywar.game.list.murdermystery.Items;
import daybreak.abilitywar.game.list.murdermystery.MurderMystery;
import daybreak.abilitywar.game.list.murdermystery.MurderMystery.ArrowKillEvent;
import daybreak.abilitywar.game.list.murdermystery.ability.AbstractMurderer;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.base.math.LocationUtil;
import daybreak.abilitywar.utils.base.math.VectorUtil;
import daybreak.abilitywar.utils.base.minecraft.item.Skulls;
import daybreak.abilitywar.utils.base.minecraft.nms.NMS;
import daybreak.abilitywar.utils.library.ParticleLib;
import daybreak.abilitywar.utils.library.PotionEffects;
import daybreak.abilitywar.utils.library.SoundLib;
import rainstar.abilitywar.game.murder.innocent.Frog;
import rainstar.abilitywar.game.murder.system.Shield;

@SuppressWarnings("deprecation")
@AbilityManifest(name = "머더: 마녀", rank = Rank.SPECIAL, species = Species.HUMAN, explain = {
		"모든 시민을 죽이세요!",
		"금 우클릭으로 금 8개를 소모해 활과 화살을 얻을 수 있습니다.",
		"§8[§c처세술§8]§f 화살에 맞으면 §7고양이§f로 변신해 이동을 제어할 수 없습니다.",
		"근접 공격을 받으면 사망하나 13초를 버티면 부활합니다.",
		"§8[§5저주§8]§f 금 좌클릭으로 금 5개를 소모해 원하는 §a시민팀§f 한 명을 지정합니다.",
		"대상에게 §5저주§f를 내려 대상으로부터 15칸 내 투사체가 유도됩니다.",
		"단, 머더의 투사체라면 2칸 거리 내에서만 유도됩니다.",
		"§8[§d지독한 장난§8]§f 금 F키로 §5저주§f가 걸린 모든 대상의 직업을 44초간",
		"무한히 통통 튀는 §2개구리§f로 만듭니다. §8(§71회용§8)"
})
public class WitchMurderer extends AbstractMurderer implements Listener {
	
	public WitchMurderer(Participant participant) {
		super(participant);
	}
	
	@Override
	protected void onUpdate(Update update) {
		super.onUpdate(update);
		if (update == Update.RESTRICTION_CLEAR) {
			final PlayerInventory inventory = getPlayer().getInventory();
			final boolean hadSword = Items.isMurdererSword(inventory.getItem(1));
			final int arrowCount = getArrowCount();
			inventory.clear();
			final MurderMystery murderMystery = (MurderMystery) getGame();
			murderMystery.addGold(getParticipant(), arrowCount * 8);
			getPlayer().getInventory().setHeldItemSlot(0);
			murderMystery.updateGold(getParticipant());
			if (!hadSword) {
				getPlayer().sendMessage("§e15초 §f뒤에 §4살인자§c의 검§f을 얻습니다.");
				new AbilityTimer(1) {
					@Override
					protected void run(int count) {
						getPlayer().getInventory().setHeldItemSlot(0);
						inventory.setItem(1, Items.MURDERER_SWORD.getStack());
						getPlayer().sendMessage("§4살인자§c의 검§f을 들고 있을 때 더 빠르게 움직일 수 있습니다.");
						for (Player player : Bukkit.getOnlinePlayers()) {
							NMS.sendTitle(player, "§4머더§c가 검을 얻었습니다.", "", 10, 80, 10);
							new AbilityTimer(1) {
								@Override
								protected void onEnd() {
									NMS.clearTitle(player);
								}
							}.setInitialDelay(TimeUnit.SECONDS, 5).start();
						}
					}
				}.setInitialDelay(TimeUnit.SECONDS, 15).start();
			} else {
				getPlayer().getInventory().setHeldItemSlot(0);
				inventory.setItem(1, Items.MURDERER_SWORD.getStack());
			}
			
			NMS.sendTitle(getPlayer(), "§e직업§f: §d마녀", "§f모든 §a시민§f을 저주하세요!", 10, 80, 10);
			new AbilityTimer(1) {
				@Override
				protected void onEnd() {
					NMS.clearTitle(getPlayer());
				}
			}.setInitialDelay(TimeUnit.SECONDS, 5).start();
			PASSIVE.start();
			passive.start();
			Bukkit.getPluginManager().registerEvents(this, AbilityWar.getPlugin());
		} else if (update == Update.ABILITY_DESTROY) {
			NMS.setInvisible(getPlayer(), false);
			HandlerList.unregisterAll(this);
		}
	}
	
	private Participant selected;
	private Set<Participant> cursed = new HashSet<>();
	private boolean onetime = false;
	private final ActionbarChannel ac = newActionbarChannel();
	private final DecimalFormat df = new DecimalFormat("0.0");
	private Ocelot ocelot;
	
	private final Predicate<Entity> arrowpredicate = new Predicate<Entity>() {
		@Override
		public boolean test(Entity entity) {
			if (!(entity instanceof Arrow)) return false;
			return true;
		}
	};
	
	private final Predicate<Entity> predicate = new Predicate<Entity>() {
		@Override
		public boolean test(Entity entity) {
			if (entity.equals(getPlayer())) return false;
			return !(entity instanceof Player) || (getGame().isParticipating(entity.getUniqueId())
					&& !((MurderMystery) getGame()).isDead(entity.getUniqueId())
					&& getGame().getParticipant(entity.getUniqueId()).attributes().TARGETABLE.getValue());
		}
	};
	
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
				new SkillGUI().start();
				SoundLib.BLOCK_ENDER_CHEST_OPEN.playSound(getPlayer(), 1, 0.6f);
			}
		}
	}
	
	@SubscribeEvent(priority = 5)
	private void onArrowKill(ArrowKillEvent e) {
		if (inv.isRunning() && e.getTarget().equals(getParticipant())) e.setCancelled(true);
		if (!e.isCancelled() && Shield.getShield(getParticipant()) < 1) {
			if (!(e.getShooter().getAbility() instanceof AbstractMurderer) && e.getTarget().equals(getParticipant())) {
				e.setCancelled(true);
				skill.start();
			}	
		}
	}
	
	@EventHandler(priority = EventPriority.LOWEST)
	private void onEntityDamageByEntity(EntityDamageByEntityEvent e) {	
		if (e.getEntity().equals(ocelot) && e.getDamager() instanceof Player && getGame().isParticipating((Player) e.getDamager()) &&
				!(getGame().getParticipant((Player) e.getDamager()).getAbility() instanceof AbstractMurderer)) {
			skill.stop(true);
		}
	}
	
	public AbilityTimer inv = new AbilityTimer(50) {
		
		@Override
		protected void run(int count) {
			ac.update("§3무적§f: §b" + df.format(count / 20.0) + "§f초");
		}
		
		@Override
		protected void onEnd() {
			onSilentEnd();
		}

		@Override
		protected void onSilentEnd() {
			ac.update(null);
		}
		
	}.setPeriod(TimeUnit.TICKS, 1).register();
	
	public AbilityTimer skill = new AbilityTimer(260) {
		
		@Override
		protected void onStart() {
			ParticleLib.CLOUD.spawnParticle(getPlayer().getLocation(), 1, 1, 1, 100, 0.1);
			SoundLib.ENTITY_OCELOT_AMBIENT.playSound(getPlayer().getLocation(), 1, 1);
			SoundLib.ENTITY_FIREWORK_ROCKET_TWINKLE.playSound(getPlayer().getLocation(), 1, 1);
			getPlayer().setGameMode(GameMode.SPECTATOR);
			getPlayer().sendMessage("§4[§c!§4] §c처세술§f을 발동합니다.");
			ocelot = getPlayer().getWorld().spawn(getPlayer().getLocation(), Ocelot.class);
			ocelot.setCatType(Type.BLACK_CAT);
			getPlayer().setSpectatorTarget(ocelot);
		}

		@Override
		protected void onEnd() {
			ocelot.setHealth(0);
			getPlayer().setGameMode(GameMode.ADVENTURE);
			ac.update(null);
			inv.start();
		}

		@Override
		protected void onSilentEnd() {
			ocelot.setHealth(0);
			SoundLib.ENTITY_GHAST_SCREAM.playSound(getPlayer().getLocation(), 1, 1.3f);
			getPlayer().setHealth(0);
			ac.update(null);
		}

		@Override
		protected void run(int count) {
			ac.update("§d변신§f: §b" + df.format(count / 20.0) + "§f초");
			getPlayer().setGameMode(GameMode.SPECTATOR);
			getPlayer().setSpectatorTarget(ocelot);
			PotionEffects.SPEED.addPotionEffect(ocelot, 20, 0, false, true, true);
		}
		
	}.setPeriod(TimeUnit.TICKS, 1).register();
	
	@SubscribeEvent
	private void onGameModeChange(PlayerGameModeChangeEvent e) {
		if (e.getPlayer().equals(getPlayer()) && skill.isRunning() && !e.getNewGameMode().equals(GameMode.SPECTATOR)) {
			e.setCancelled(true);
		}
	}
	
    @SubscribeEvent(onlyRelevant = true, priority = 0)
    public void onPlayerSwapHandItems(PlayerSwapHandItemsEvent e) {
    	if (Items.isGold(e.getOffHandItem()) && e.getPlayer().equals(getPlayer())) {
    		if (!onetime) {
    			if (cursed.size() > 0) {
            		for (Participant participant : cursed) {
            			try {
            				if (participant.hasAbility()) {
                				AbilityRegistration registration = participant.getAbility().getRegistration();
            					participant.setAbility(Frog.class);
            					Frog frog = (Frog) participant.getAbility();
            					frog.setPreviousAbility(registration);
            					participant.getPlayer().sendMessage("§d§o마녀의 장난에 당했습니다!");
            					SoundLib.ENTITY_ZOMBIE_VILLAGER_CONVERTED.playSound(getPlayer(), 1, 0.6f);
            				}
        				} catch (UnsupportedOperationException | ReflectiveOperationException e1) {
        					e1.printStackTrace();
        				}
            		}
            		onetime = true;
            		getPlayer().sendMessage("§4[§c!§4] §d지독한 장난§f을 사용하였습니다.");	
            		SoundLib.ENTITY_WITCH_AMBIENT.playSound(getPlayer(), 1, 2);
    			} else getPlayer().sendMessage("§4[§c!§4] 현재 §5저주§f하고 있는 대상이 없습니다.");	
    		} else getPlayer().sendMessage("§4[§c!§4] §c이미 §d지독한 장난§c을 사용하였습니다.");	
    	}
    }
    
	public AbilityTimer passive = new AbilityTimer() {
		
		@Override
		public void run(int count) {
			MurderMystery murderMystery = (MurderMystery) getGame();
			Set<Participant> remove = new HashSet<>();
			for (Participant participant : cursed) {
				if (predicate.test(participant.getPlayer())) {
					for (Arrow arrow : LocationUtil.getNearbyEntities(Arrow.class, participant.getPlayer().getLocation(), 15, 15, arrowpredicate)) {
						if (arrow.getShooter() != null && !participant.getPlayer().equals(arrow.getShooter()) && arrow.getShooter() instanceof Player && getGame().isParticipating((Player) arrow.getShooter())) {
							Participant shooter = getGame().getParticipant((Player) arrow.getShooter());
							if (shooter.hasAbility() && shooter.getAbility() instanceof AbstractMurderer) {
								if (arrow.getLocation().distanceSquared(participant.getPlayer().getEyeLocation()) <= 4) {
									arrow.setGravity(false);
									arrow.setVelocity(VectorUtil.validateVector((participant.getPlayer().getEyeLocation().toVector().subtract(arrow.getLocation().toVector()))));	
								}
							} else {
								arrow.setGravity(false);
								arrow.setVelocity(VectorUtil.validateVector((participant.getPlayer().getEyeLocation().toVector().subtract(arrow.getLocation().toVector()))));	
							}
						}
					}
					ParticleLib.SPELL_WITCH.spawnParticle(getPlayer(), participant.getPlayer().getLocation(), 0.5, 1.5, 0.5, 1, 7);
					if (murderMystery.isDead(participant.getPlayer().getUniqueId())) remove.add(participant);	
				}
			}
			
			for (Participant participant : remove) {
				cursed.remove(participant);
			}
		}
		
	}.setPeriod(TimeUnit.TICKS, 1).register();
	
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
	    				&& !(participant.getAbility() instanceof AbstractMurderer) && !cursed.contains(participant)) {
	    			ItemStack item = Skulls.createCustomSkull(participant.getPlayer().getName());
					ItemMeta meta = item.getItemMeta();
					meta.setDisplayName("§a" + participant.getPlayer().getName());
					List<String> lore = new ArrayList<>();
					lore.add("§7");
					lore.add("§5» §d이 대상을 §5저주§d하려면 선택하세요.");
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
					if (murderMystery.consumeGold(getParticipant(), 5)) {
						SoundLib.ENTITY_ELDER_GUARDIAN_CURSE.playSound(getPlayer(), 1, 0.6f);
						getPlayer().sendMessage("§4[§c!§4] §3" + selected.getPlayer().getName() + "§f님에게 §5저주§f를 내립니다.");	
						cursed.add(selected);
					} else getPlayer().sendMessage("§4[§c!§4] §c금이 부족합니다.");
				} else getPlayer().sendMessage("§4[§c!§4] §c이미 죽은 대상을 저주할 순 없습니다.");
				selected = null;
			}
			HandlerList.unregisterAll(this);
			getPlayer().closeInventory();
		}
		
	}

}