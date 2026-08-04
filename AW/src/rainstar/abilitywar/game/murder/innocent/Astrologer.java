package rainstar.abilitywar.game.murder.innocent;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Predicate;

import org.bukkit.Bukkit;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.attribute.AttributeModifier.Operation;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
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
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.SubscribeEvent;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.AbstractGame.Participant.ActionbarNotification.ActionbarChannel;
import daybreak.abilitywar.game.list.murdermystery.Items;
import daybreak.abilitywar.game.list.murdermystery.MurderMystery;
import daybreak.abilitywar.game.list.murdermystery.MurderMystery.ArrowKillEvent;
import daybreak.abilitywar.game.list.murdermystery.ability.AbstractInnocent;
import daybreak.abilitywar.game.list.murdermystery.ability.AbstractMurderer;
import daybreak.abilitywar.game.list.murdermystery.ability.AbstractMurderer.MurderEvent;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.base.math.LocationUtil;
import daybreak.abilitywar.utils.base.minecraft.item.Skulls;
import daybreak.abilitywar.utils.base.minecraft.nms.NMS;
import daybreak.abilitywar.utils.library.ParticleLib;
import daybreak.abilitywar.utils.library.SoundLib;

@AbilityManifest(name = "시민: 점성술사", rank = Rank.SPECIAL, species = Species.HUMAN, explain = {
		"활과 화살 하나, 금 7개를 기본적으로 얻고 시작합니다.",
		"금 우클릭으로 금 8개를 소모해 활과 화살을 얻을 수 있습니다.",
		"§8[§5예언§8]§f 금 좌클릭으로 금 7개를 소모해 자신 외 한 명을 선택합니다.",
		"만일 선택한 대상이 선택 이후 처음으로 사망 위기에 처하는 사람일 경우,",
		"§5예언§f에 성공해 남아있는 사람 × 15초만큼 화살을 무한정으로 쏠 수 있습니다.",
		"지속시간 동안 나침반은 대상을 죽인 사람을 가리키고, 이동 속도 40% 증가를 받습니다."
})
public class Astrologer extends AbstractInnocent {

	public Astrologer(Participant participant) {
		super(participant);
	}
	
	@Override
	protected void onUpdate(Update update) {
		super.onUpdate(update);
		if (update == Update.RESTRICTION_CLEAR) {
			NMS.sendTitle(getPlayer(), "§e역할§f: §a점성술사", "§f미래를 예지해내세요!", 10, 80, 10);
			new AbilityTimer(1) {
				@Override
				protected void run(int count) {
				}

				@Override
				protected void onEnd() {
					NMS.clearTitle(getPlayer());
				}
			}.setInitialDelay(TimeUnit.SECONDS, 5).start();
			passive.start();
			final MurderMystery murderMystery = (MurderMystery) getGame();
			if (!addArrow()) {
				murderMystery.addGold(getParticipant());
			} else {
				if (!hasBow()) {
					getPlayer().getInventory().setItem(2, Items.NORMAL_BOW.getStack());
				}
			}
			murderMystery.addGold(getParticipant(), 7);
		}
	}
	
	private final Predicate<Entity> predicate = new Predicate<Entity>() {
		@Override
		public boolean test(Entity entity) {
			if (entity.equals(getPlayer())) return false;
			return !(entity instanceof Player) || (getGame().isParticipating(entity.getUniqueId())
					&& !((MurderMystery) getGame()).isDead(entity.getUniqueId())
					&& getGame().getParticipant(entity.getUniqueId()).attributes().TARGETABLE.getValue());
		}
	};
	
	private Participant selected, target;
	private ActionbarChannel ac = newActionbarChannel();
	private DecimalFormat df = new DecimalFormat("0.0");
	private static final int time = 300;
	private final AttributeModifier modifier = new AttributeModifier(UUID.randomUUID(), "addspeed", 0.4, Operation.ADD_SCALAR);
	
	@SubscribeEvent(priority = 10)
	private void onMurder(MurderEvent e) {
		if (e.getTarget().equals(selected)) {
			skill.start();
			int i = 0;
			for (Participant participant : getGame().getParticipants()) {
				if (!((MurderMystery) getGame()).isDead(participant.getPlayer().getUniqueId()) && !participant.equals(getParticipant())) i++;
			}
			skill.setCount(time * i);
			target = e.getMurderer().getParticipant();
		} else if (selected != null) {
			getPlayer().sendMessage("§4[§c!§4] §5예언§f에 실패하셨습니다...");
			selected = null;
		}
	}
	
	@SubscribeEvent(priority = 10)
	private void onArrowKill(ArrowKillEvent e) {
		if (e.getShooter().equals(selected) && !(e.getTarget().getAbility() instanceof AbstractMurderer) && !e.getTarget().getAbility().getClass().equals(Clown.class)) {
			skill.start();
			int i = 0;
			for (Participant participant : getGame().getParticipants()) {
				if (!((MurderMystery) getGame()).isDead(participant.getPlayer().getUniqueId()) && !participant.equals(getParticipant())) i++;
			}
			skill.setCount(time * i);
		} else if (e.getTarget().equals(selected)) {
			skill.start();
			int i = 0;
			for (Participant participant : getGame().getParticipants()) {
				if (!((MurderMystery) getGame()).isDead(participant.getPlayer().getUniqueId()) && !participant.equals(getParticipant())) i++;
			}
			skill.setCount(time * i);
			target = e.getShooter();
		} else if (selected != null) {
			getPlayer().sendMessage("§4[§c!§4] §5예언§f에 실패하셨습니다...");
		}
		selected = null;
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
				new SkillGUI().start();
			}
		}
	}
	
	public AbilityTimer passive = new AbilityTimer() {
		
		@Override
		public void run(int count) {
			if (target == null) {
				if (LocationUtil.getNearestEntity(Player.class, getPlayer().getLocation(), predicate) != null) {
					getPlayer().setCompassTarget(LocationUtil.getNearestEntity(Player.class, getPlayer().getLocation(), predicate).getLocation());	
				}
			} else {
				getPlayer().setCompassTarget(target.getPlayer().getLocation());
			}
			if (selected != null) ParticleLib.SPELL_WITCH.spawnParticle(getPlayer(), selected.getPlayer().getLocation(), 0.5, 1.5, 0.5, 1, 7);
		}
		
	}.setPeriod(TimeUnit.TICKS, 1).register();
	
	public AbilityTimer skill = new AbilityTimer(100) {
		
		@Override
		public void onStart() {
			new AbilityTimer(TaskType.NORMAL, 3) {
				
				@Override
				protected void run(int count) {
					SoundLib.BLOCK_END_PORTAL_FRAME_FILL.playSound(getPlayer(), 1, 1 + (count * 0.3f));
				}
				
				@Override
				protected void onEnd() {
					SoundLib.BLOCK_END_PORTAL_SPAWN.playSound(getPlayer(), 1, 2);
				}
				
			}.setInitialDelay(TimeUnit.TICKS, 2).start();
			if (Items.NORMAL_BOW.getStack().getType().equals(getPlayer().getInventory().getItem(2).getType())) {
				ItemStack item = getPlayer().getInventory().getItem(2);
				item.addUnsafeEnchantment(Enchantment.ARROW_INFINITE, 10);	
			}
			getPlayer().getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).addModifier(modifier);	
		}
		
		@Override
		public void run(int count) {
			if (Items.NORMAL_BOW.getStack().getType().equals(getPlayer().getInventory().getItem(2).getType())) {
				ItemStack item = getPlayer().getInventory().getItem(2);
				if (!item.getEnchantments().containsKey(Enchantment.ARROW_INFINITE)) item.addUnsafeEnchantment(Enchantment.ARROW_INFINITE, 10);
			}
			ac.update("§5점성술§3: §e" + df.format(count / 20.0) + "§a초");
		}
		
		@Override
		public void onEnd() {
			onSilentEnd();
		}
		
		@Override
		public void onSilentEnd() {
			if (Items.NORMAL_BOW.getStack().getType().equals(getPlayer().getInventory().getItem(2).getType())) {
				ItemStack item = getPlayer().getInventory().getItem(2);
				if (item.getEnchantments().containsKey(Enchantment.ARROW_INFINITE)) item.removeEnchantment(Enchantment.ARROW_INFINITE);
			}
			ac.update(null);
			target = null;
			getPlayer().getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).removeModifier(modifier);
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
	    		if (!((MurderMystery) getGame()).isDead(participant.getPlayer().getUniqueId()) && !participant.equals(getParticipant())) {
	    			ItemStack item = Skulls.createCustomSkull(participant.getPlayer().getName());
					ItemMeta meta = item.getItemMeta();
					meta.setDisplayName("§a" + participant.getPlayer().getName());
					List<String> lore = new ArrayList<>();
					lore.add("§7");
					lore.add("§5» §b이 대상을 예측 대상으로 지정하려면 선택하세요.");
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
				if (!murderMystery.isDead(selected.getPlayer().getUniqueId())) {
					if (murderMystery.consumeGold(getParticipant(), 7)) {
						SoundLib.ENTITY_ENDER_EYE_DEATH.playSound(getPlayer(), 1, 0.5f);
						getPlayer().sendMessage("§4[§c!§4] §3" + selected.getPlayer().getName() + "§f님을 §5예언§f 대상으로 지정하였습니다.");	
					} else getPlayer().sendMessage("§4[§c!§4] §c금이 부족합니다.");	
				} else getPlayer().sendMessage("§4[§c!§4] §c이미 죽은 대상을 예언할 순 없습니다.");	
			}
			HandlerList.unregisterAll(this);
			getPlayer().closeInventory();
		}
		
	}
	
}
