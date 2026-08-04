package rainstar.abilitywar.game.murder.murderer;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.WorldBorder;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.ItemStack;

import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.SubscribeEvent;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.AbstractGame.Participant.ActionbarNotification.ActionbarChannel;
import daybreak.abilitywar.game.list.murdermystery.Items;
import daybreak.abilitywar.game.list.murdermystery.MurderMystery;
import daybreak.abilitywar.game.list.murdermystery.MurderMystery.ArrowKillEvent;
import daybreak.abilitywar.game.list.murdermystery.ability.AbstractMurderer;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.base.concurrent.SimpleTimer.TaskType;
import daybreak.abilitywar.utils.base.math.LocationUtil;
import daybreak.abilitywar.utils.base.minecraft.item.builder.ItemBuilder;
import daybreak.abilitywar.utils.base.minecraft.nms.NMS;
import daybreak.abilitywar.utils.base.minecraft.version.ServerVersion;
import daybreak.abilitywar.utils.library.MaterialX;
import daybreak.abilitywar.utils.library.PotionEffects;
import daybreak.abilitywar.utils.library.SoundLib;

@AbilityManifest(name = "머더: 괴도", rank = Rank.SPECIAL, species = Species.HUMAN, explain = {
		"모든 시민을 죽이세요!",
		"§8[§c강탈§8]§f 금 우클릭으로 금 8개를 소모해 활과 화살을 얻을 수 있습니다.",
		"금이 모자라면 시민들에게서 총 8개의 §e금§f을 훔쳐옵니다. §c쿨타임 §7: §f75초",
		"§8[§b글라이드§8]§f 금 F키로 겉날개를 장착 / 해제 가능합니다.",
		"§8[§3예고장§8]§f 금 좌클릭으로 금 15개를 소모해 무작위 위치에 §b다이아몬드§f를",
		"소환해 위치를 전원에게 전송합니다. 자신의 나침반은 §b보물§f의 방향을 가리킵니다.",
		"사용 후 §7은신§f하며, 살인이 불가능하고, 자신 외 모두가 §b신속 2§f를 받습니다.",
		"§b보물§f을 획득 후 15초간 잡히지 않으면 40초간 §c[§5괴도의 밤§c]§f을 발동합니다.",
		"두 번째 사용 시부터는 40초가 아닌 20초만 발동합니다. §c쿨타임 §7: §f90초",
		"이미 §b다이아몬드§f가 있을 때 사용 시 금 1개로 위치를 변경합니다. §8(§72회용§8)",
		"§c[§5괴도의 밤§c]§f 모든 §a시민팀§f을 §c활 봉인§f 및 §8실명§f 및 §e발광§f시킵니다.",
		"자신은 §c폭죽§f을 얻습니다."
})
public class PhantomThiefMurderer extends AbstractMurderer {
	
	public PhantomThiefMurderer(Participant participant) {
		super(participant);
	}
	
	@Override
	protected void onUpdate(Update update) {
		super.onUpdate(update);
		if (update == Update.RESTRICTION_CLEAR) {
			NMS.sendTitle(getPlayer(), "§e직업§f: §5괴도", "§b다이아몬드§f를 안전하게 훔쳐내세요!", 10, 80, 10);
			new AbilityTimer(1) {
				@Override
				protected void onEnd() {
					NMS.clearTitle(getPlayer());
				}
			}.setInitialDelay(TimeUnit.SECONDS, 5).start();
			PASSIVE.start();
			passive.start();
			ac.update("§8[§7겉날개§8]");
		} else if (update == Update.ABILITY_DESTROY) {
			NMS.setInvisible(getPlayer(), false);
			if (diamond != null) diamond.remove();
		}
	}
	
	private final Cooldown stealcooldown = new Cooldown(75, "강탈"), spawncooldown = new Cooldown(90, "예고장");
	private boolean pickup = false;
	private boolean first = true;
	private int changestack = 2;
	private ItemStack DIAMOND = (new ItemBuilder(MaterialX.DIAMOND)).displayName("§b다이아몬드").build();
	private Location diamondloc = null;
	private Item diamond = null;
	private DecimalFormat df = new DecimalFormat("0");
	private BossBar bossBar = null;
	private ActionbarChannel ac = newActionbarChannel();
	
	private final Predicate<Entity> compasspredicate = new Predicate<Entity>() {
		@Override
		public boolean test(Entity entity) {
			if (entity.equals(getPlayer())) return false;
			return !(entity instanceof Player) || (getGame().isParticipating(entity.getUniqueId())
					&& !((MurderMystery) getGame()).isDead(entity.getUniqueId())
					&& getGame().getParticipant(entity.getUniqueId()).attributes().TARGETABLE.getValue())
					&& getGame().getParticipant(entity.getUniqueId()).hasAbility()
					&& !(getGame().getParticipant(entity.getUniqueId()).getAbility() instanceof AbstractMurderer);
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
				} else if (!stealcooldown.isCooldown()) {
					List<Participant> innocents = new ArrayList<>();
					for (Participant participant : getGame().getParticipants()) {
						if (participant.hasAbility() && !(participant.getAbility() instanceof AbstractMurderer)) innocents.add(participant);
					}
					if (innocents.size() > 0) {
						Collections.shuffle(innocents);
						int goldcount = 8;
						for (Participant innocent : innocents) {
							int losegold = murderMystery.getGold(innocent) - Math.max(0, murderMystery.getGold(innocent) - goldcount);
							murderMystery.consumeGold(innocent, losegold);
							murderMystery.addGold(getParticipant(), losegold);
							goldcount -= losegold;
							if (goldcount <= 0) break;
						}
						getPlayer().sendMessage("§4[§c!§4] §a시민들§f에게서 §e금 " + (8 - goldcount) + "§f개를 훔쳤습니다.");
						stealcooldown.start();
					}
				}
			} else if (e.getAction() == Action.LEFT_CLICK_AIR || e.getAction() == Action.LEFT_CLICK_BLOCK) {
				if (!skill.isRunning()) {
					if (!spawncooldown.isCooldown() && !blind.isRunning()) {
						WorldBorder myborder = getPlayer().getLocation().getWorld().getWorldBorder();
						if (myborder.isInside(getPlayer().getLocation())) {
							final MurderMystery murderMystery = (MurderMystery) getGame();
							if (murderMystery.consumeGold(getParticipant(), 15)) {
								Location loc = null;
								while(true) {
									Location location = LocationUtil.getRandomLocations(getPlayer().getLocation(), 150, 1).get(0);
									WorldBorder border = location.getWorld().getWorldBorder();
									if (border.isInside(location) && getPlayer().getLocation().distanceSquared(location) > 1000) {
										loc = location;
										break;
									}
								}				
								loc.setY(LocationUtil.getFloorYAt(loc.getWorld(), getPlayer().getLocation().getY(), loc.getBlockX(), loc.getBlockZ()));
								DIAMOND.addUnsafeEnchantment(Enchantment.DURABILITY, 10);
								diamond = getPlayer().getWorld().dropItemNaturally(loc, DIAMOND);
								diamondloc = loc;
								Bukkit.broadcastMessage("§8[§3예고장§8]§f 오늘 밤, §5X §e" + df.format(loc.getX()) + " §7/ §5Y §e" + df.format(loc.getY()) + " §7/ §5Z §e" + df.format(loc.getZ()) + "§f의 §b다이아몬드§f를 훔치러 갈 것이다.");
								skill.start();
								diamond.setGlowing(true);
							}
						} else getPlayer().sendMessage("§4[§c!§4] §c월드보더 밖에서 사용할 수 없는 스킬입니다.");	
					}
				} else {
					if (changestack > 0) {
						WorldBorder myborder = getPlayer().getLocation().getWorld().getWorldBorder();
						if (myborder.isInside(getPlayer().getLocation())) {
							final MurderMystery murderMystery = (MurderMystery) getGame();
							if (murderMystery.consumeGold(getParticipant(), 1)) {
								diamond.remove();
								Location loc = null;
								while(true) {
									Location location = LocationUtil.getRandomLocations(getPlayer().getLocation(), 150, 1).get(0);
									WorldBorder border = location.getWorld().getWorldBorder();
									if (border.isInside(location) && getPlayer().getLocation().distanceSquared(location) >= 3000) {
										loc = location;
										break;
									}
								}				
								loc.setY(LocationUtil.getFloorYAt(loc.getWorld(), getPlayer().getLocation().getY(), loc.getBlockX(), loc.getBlockZ()));
								DIAMOND.addUnsafeEnchantment(Enchantment.DURABILITY, 10);
								diamond = getPlayer().getWorld().dropItemNaturally(loc, DIAMOND);
								diamondloc = loc;
								Bukkit.broadcastMessage("§8[§3예고장§8]§f 위치 변경. 오늘 밤, §5X §e" + df.format(loc.getX()) + " §7/ §5Y §e" + df.format(loc.getY()) + " §7/ §5Z §e" + df.format(loc.getZ()) + "§f의 §b다이아몬드§f를 훔치러 갈 것이다.");
								skill.start();
								diamond.setGlowing(true);
								changestack--;
							}
						} else getPlayer().sendMessage("§4[§c!§4] §c금이 모자랍니다.");
					} else getPlayer().sendMessage("§4[§c!§4] §c이미 위치 변경을 사용하였습니다.");
				}
			}
		}
	}
	
	@SubscribeEvent(priority = 0)
	private void onMurder(MurderEvent e) {
		if (skill.isRunning() && getParticipant().equals(e.getMurderer().getParticipant())) {
			e.setCancelled(true); 
		}	
	}
	
	@SubscribeEvent(priority = 0)
	private void onArrowKill(ArrowKillEvent e) {
		if (skill.isRunning() && getParticipant().equals(e.getShooter())) {
			e.setCancelled(true);
		}	
	}
	
	@SubscribeEvent
	private void onPlayerJoin(final PlayerJoinEvent e) {
		if (getPlayer().getUniqueId().equals(e.getPlayer().getUniqueId())) {
			if (bossBar != null) bossBar.addPlayer(e.getPlayer());
		}
	}

	@SubscribeEvent
	private void onPlayerQuit(final PlayerQuitEvent e) {
		if (getPlayer().getUniqueId().equals(e.getPlayer().getUniqueId())) {
			if (bossBar != null) bossBar.removePlayer(e.getPlayer());
		}
	}
	
	@SubscribeEvent
	private void onPickupItem(EntityPickupItemEvent e) {
		if (e.getEntity().equals(getPlayer()) && e.getItem().getItemStack().equals(DIAMOND)) {
			e.getItem().remove();
			pickup = true;
			diamond = null;
			NMS.setInvisible(getPlayer(), true);
			Bukkit.broadcastMessage("§c§o보물을 뺏겼습니다!");
			diamondloc = null;
		}
	}
	
    @SubscribeEvent(onlyRelevant = true, priority = 0)
    public void onPlayerSwapHandItems(PlayerSwapHandItemsEvent e) {
    	if (Items.isGold(e.getOffHandItem()) && e.getPlayer().equals(getPlayer())) {
    		if (getPlayer().getInventory().getChestplate() != null && getPlayer().getInventory().getChestplate().getType().equals(Material.ELYTRA)) {
        		getPlayer().sendMessage("§4[§c!§4] §7겉날개 장착 해제.");
        		getPlayer().getInventory().setChestplate(null);
    			ac.update("§8[§7겉날개§8]");
    		} else {
        		getPlayer().sendMessage("§4[§c!§4] §b겉날개 장착 완료.");
        		getPlayer().getInventory().setChestplate(new ItemStack(Material.ELYTRA, 1));
    			ac.update("§3[§b겉날개§3]");
    		}
    	}
    }

	private final AbilityTimer skill = new AbilityTimer() {
		
		int survivecount = 0;
		
		@Override
		public void run(int count) {
			for (Participant participant : getGame().getParticipants()) {
				if (!participant.equals(getParticipant())) PotionEffects.SPEED.addPotionEffect(participant.getPlayer(), 40, 1, false, false, true);
			}
			if (!pickup) {
				NMS.setInvisible(getPlayer(), true);
			} else {
				PotionEffects.INVISIBILITY.addPotionEffect(getPlayer(), 40, 0, false, false, true);
				PotionEffects.GLOWING.addPotionEffect(getPlayer(), 40, 0, false, false, true);
				survivecount++;
				if (survivecount >= 15) {
					stop(false);
					blind.start();
				}
			}
		}
		
		@Override
		public void onEnd() {
			onSilentEnd();
		}
		
		@Override
		public void onSilentEnd() {
			survivecount = 0;
			pickup = false;
			NMS.setInvisible(getPlayer(), false);
		}
		
	}.setPeriod(TimeUnit.SECONDS, 1).register();
	
	private final AbilityTimer blind = new AbilityTimer(TaskType.REVERSE, 800) {
		
		@Override
		public void onStart() {
			if (!first) setCount(400);
			first = false;
			for (Participant participant : getGame().getParticipants()) {
				SoundLib.ENTITY_WITCH_AMBIENT.playSound(participant.getPlayer(), 1, 0.5f);
			}
			Bukkit.broadcastMessage("§4[§c!§4] §5괴도의 밤이 시작됩니다!");
			bossBar = Bukkit.createBossBar("§9괴도의 밤§7: §5" + df.format(800 / 20.0) + "§f초", BarColor.PURPLE, BarStyle.SOLID);
    		bossBar.setProgress(1);
    		bossBar.addPlayer(getPlayer());
    		if (ServerVersion.getVersion() >= 15) bossBar.setVisible(true);
			for (Participant participant : getGame().getParticipants()) {
				if (participant.hasAbility() && !(participant.getAbility() instanceof AbstractMurderer)) {
					participant.getPlayer().getInventory().setHeldItemSlot(0);
		    		bossBar.addPlayer(participant.getPlayer());
				}
			}
		}
		
		@Override
		public void run(int count) {
			bossBar.setTitle("§9괴도의 밤§7: §5" + df.format(count / 20.0) + "§f초");
			bossBar.setProgress(Math.max(0, Math.min(1, count / 800.0)));
			for (Participant participant : getGame().getParticipants()) {
				if (participant.hasAbility() && !(participant.getAbility() instanceof AbstractMurderer)) {
					NMS.setCooldown(participant.getPlayer(), Material.BOW, 1200);
					PotionEffects.BLINDNESS.addPotionEffect(participant.getPlayer(), 100, 0, false, false, true);
					PotionEffects.GLOWING.addPotionEffect(participant.getPlayer(), 20, 0, false, false, true);
				}
			}
			if (ServerVersion.getVersion() > 13) getPlayer().getInventory().setItem(2, new ItemStack(MaterialX.FIREWORK_ROCKET.getMaterial(), 64));
			else getPlayer().getInventory().setItem(2, new ItemStack(MaterialX.FIREWORK_ROCKET.getMaterial(), 64));
		}
		
		@Override
		public void onEnd() {
			onSilentEnd();
		}
		
		@Override
		public void onSilentEnd() {
			for (Participant participant : getGame().getParticipants()) {
				if (NMS.hasCooldown(participant.getPlayer(), Material.BOW)) NMS.setCooldown(participant.getPlayer(), Material.BOW, 0);
			}
			getPlayer().getInventory().setItem(2, null);
			bossBar.removeAll();
			bossBar = null;
			spawncooldown.start();
			changestack = 2;
		}
		
	}.setPeriod(TimeUnit.TICKS, 1).register();
	
	public AbilityTimer passive = new AbilityTimer() {
		
		@Override
		public void run(int count) {
			if (diamondloc == null) {
				if (LocationUtil.getNearestEntity(Player.class, getPlayer().getLocation(), compasspredicate) != null) {
					getPlayer().setCompassTarget(LocationUtil.getNearestEntity(Player.class, getPlayer().getLocation(), compasspredicate).getLocation());	
				}
			} else {
				getPlayer().setCompassTarget(diamondloc);	
			}
		}
		
	}.setPeriod(TimeUnit.TICKS, 1).register();
	
}
