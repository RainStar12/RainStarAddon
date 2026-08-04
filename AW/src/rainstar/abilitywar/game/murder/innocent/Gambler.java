package rainstar.abilitywar.game.murder.innocent;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

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
import daybreak.abilitywar.game.list.murdermystery.ability.AbstractJob;
import daybreak.abilitywar.game.list.murdermystery.event.PreAddGoldEvent;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.base.minecraft.item.builder.ItemBuilder;
import daybreak.abilitywar.utils.base.minecraft.nms.NMS;
import daybreak.abilitywar.utils.library.MaterialX;
import daybreak.abilitywar.utils.library.SoundLib;
import rainstar.abilitywar.game.murder.murderer.CorruptionDetectiveMurderer;
import rainstar.abilitywar.game.murder.system.Shield;

@AbilityManifest(name = "시민: 도박꾼", rank = Rank.SPECIAL, species = Species.HUMAN, explain = {
		"금 우클릭으로 금 8개를 소모해 활과 화살을 얻을 수 있습니다.",
		"§8[§c갬블§8]§f 금 좌클릭으로 금 4개를 소모해 슬롯을 돌립니다.",
		"슬롯이 돌아가는 동안§7(2초)§f은 이동이 불가능합니다.",
		"슬롯이 전부 다 다르면 금 10개를 얻고, 다 같으면 특수보상을 획득합니다.",
		"§8[§7철§8] §f1분간 도박 속도가 2배가 됩니다.",
		"§6[§e금§6] §f1분간 금을 획득할 때 금이 2배로 들어옵니다.",
		"§2[§a에메랄드§2] §f시민팀 전원 활과 화살을 2개 획득합니다.",
		"§3[§b다이아몬드§3] §f화살 및 머더 공격 방어 실드를 2~7개 받습니다."
})
public class Gambler extends AbstractInnocent {
	
	public Gambler(Participant participant) {
		super(participant);
	}
	
	@Override
	protected void onUpdate(Update update) {
		super.onUpdate(update);
		if (update == Update.RESTRICTION_CLEAR) {
			NMS.sendTitle(getPlayer(), "§e역할§f: §a도박꾼", "§f금을 무한대로 불려보세요!", 10, 80, 10);
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
	
	private final ActionbarChannel ac0 = newActionbarChannel(), ac1 = newActionbarChannel();
	private boolean isRunning = false;
	
	public AbilityTimer speedup = new AbilityTimer(60) {
		
		@Override
		protected void run(int count) {
			ac0.update("§b속도 증가§f: §3" + count + "§f초");
		}
		
		@Override
		protected void onEnd() {
			onSilentEnd();
		}

		@Override
		protected void onSilentEnd() {
			ac0.update(null);
		}
		
	}.setPeriod(TimeUnit.SECONDS, 1).register();
	
	public AbilityTimer goldmultiply = new AbilityTimer(60) {
		
		@Override
		protected void run(int count) {
			ac1.update("§e금 2배§f: §a" + count + "§f초");
		}
		
		@Override
		protected void onEnd() {
			onSilentEnd();
		}

		@Override
		protected void onSilentEnd() {
			ac1.update(null);
		}
		
	}.setPeriod(TimeUnit.SECONDS, 1).register();
	
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
				MurderMystery murderMystery = (MurderMystery) getGame();
				if (!isRunning) {
					if (murderMystery.consumeGold(getParticipant(), 4)) {
						isRunning = true;
						new SkillGUI().start();
					}	
				}
			}
		}
	}
	
	@SubscribeEvent
	private void onAddGold(PreAddGoldEvent e) {
		if (goldmultiply.isRunning()) {
			if (e.getPlayer().equals(getPlayer())) {
				e.setAmount(Math.max(1, Math.min(64, e.getAmount() * 2)));
			}	
		}
	}
	
	public class SkillGUI extends AbilityTimer implements Listener {

		private final Inventory gui = Bukkit.createInventory(null, 27, "§e§lS§a§lL§e§lO§a§lT §e§lM§a§lA§e§lC§a§lH§e§lI§a§lN§e§lE");
		private final ItemStack NULL = (new ItemBuilder(MaterialX.WHITE_STAINED_GLASS_PANE)).displayName(" ").build();
		private final ItemStack BLANK = (new ItemBuilder(MaterialX.LIGHT_GRAY_STAINED_GLASS_PANE)).displayName(" ").build();
		private final ItemStack IRON = (new ItemBuilder(MaterialX.IRON_INGOT)).displayName("§7철").build();
		private final ItemStack GOLD = (new ItemBuilder(MaterialX.GOLD_INGOT)).displayName("§e금").build();
		private final ItemStack EMERALD = (new ItemBuilder(MaterialX.EMERALD)).displayName("§a에메랄드").build();
		private final ItemStack DIAMOND = (new ItemBuilder(MaterialX.DIAMOND)).displayName("§b다이아몬드").build();
		private int number1 = 0;
		private int number2 = 0;
		private int number3 = 0;
		
		public SkillGUI() {
			super(TaskType.REVERSE, 4);
			setPeriod(TimeUnit.TICKS, speedup.isRunning() ? 5 : 10);
			isRunning = true;
			Bukkit.getPluginManager().registerEvents(this, AbilityWar.getPlugin());
			openGUI();
		}
		
		private void openGUI() {
	    	gui.clear();
	    	gui.setItem(10, getItem(number1));
	    	gui.setItem(13, getItem(number2));
	    	gui.setItem(16, getItem(number3));
	    	for (int i = 0; i < 27; i++) {
	    		if (i != 10 && i != 13 && i != 16) {
	    			gui.setItem(i, NULL);
	    		}
	    	}
	        getPlayer().openInventory(gui);
	    }
		
		private ItemStack getItem(int number) {
			switch(number) {
			case 1:
				return IRON;
			case 2:
				return GOLD;
			case 3:
				return EMERALD;
			case 4:
				return DIAMOND;
			}
			return BLANK;
		}
		
	    @EventHandler()
	    public void onInventoryClick(InventoryClickEvent e) {
	    	if (gui != null && e.getClickedInventory() != null) {
		    	if (e.getClickedInventory().equals(gui)) {
		    		e.setCancelled(true);
		    	}	
	    	}
	    }
		
		@Override
		protected void run(int count) {
			switch(count) {
			case 4:
				number1 = random.nextInt(4) + 1;
				SoundLib.ENTITY_ARROW_HIT_PLAYER.playSound(getPlayer());
				break;
			case 3:
				number2 = random.nextInt(4) + 1;
				SoundLib.ENTITY_ARROW_HIT_PLAYER.playSound(getPlayer());
				break;
			case 2:
				number3 = random.nextInt(4) + 1;
				SoundLib.ENTITY_ARROW_HIT_PLAYER.playSound(getPlayer());
				break;
			}
			openGUI();
		}
	    
		@Override
		protected void onEnd() {
			onSilentEnd();
		}
		
		@Override
		protected void onSilentEnd() {
			MurderMystery murderMystery = (MurderMystery) getGame();
			if (number1 != number2 && number1 != number3 && number2 != number3) {
				SoundLib.ENTITY_PLAYER_LEVELUP.playSound(getPlayer());
				getPlayer().sendMessage("§a[§e!§a] §f세 광물이 전부 달라 §e금§f을 §e10§f개 획득합니다.");
				murderMystery.addGold(getParticipant(), 10);
			} else if (number1 == number2 && number1 == number3 && number2 == number3) {
				SoundLib.UI_TOAST_CHALLENGE_COMPLETE.playSound(getPlayer(), 1, 2f);
				switch(number1) {
				case 1:
					getPlayer().sendMessage("§a[§e!§a] §7철§f을 맞추어서 1분간 이동 속도 30% 증가를 받습니다!");
					if (!speedup.isRunning()) speedup.start();
					else speedup.setCount(speedup.getCount() + 60);
					break;
				case 2:
					getPlayer().sendMessage("§a[§e!§a] §e금§f을 맞추어서 1분간 금 획득량이 2배가 됩니다!");
					if (!goldmultiply.isRunning()) goldmultiply.start();
					else goldmultiply.setCount(goldmultiply.getCount() + 60);
					break;
				case 3:
					getPlayer().sendMessage("§a[§e!§a] §a에메랄드§f를 맞추어서 시민팀 전원이 활과 화살을 2개 얻습니다!");
					for (Participant participant : getGame().getParticipants()) {
						if (participant.hasAbility()) {
							if ((participant.getAbility().getClass().getSimpleName().contains("Detective") && !participant.getAbility().getClass().equals(CorruptionDetectiveMurderer.class)) || participant.getAbility() instanceof AbstractInnocent) {
								ItemStack stack = participant.getPlayer().getInventory().getItem(3);
		        				if (stack != null && stack.getType() == Material.ARROW) {
		        					if (stack.getAmount() < 64) {
		        						stack.setAmount(stack.getAmount() + 2);
		        						participant.getPlayer().getInventory().setItem(3, stack);
		        						participant.getPlayer().sendMessage("§8+ §f2 화살");
		        					}
		        				} else {
		        					participant.getPlayer().getInventory().setItem(3, new ItemStack(Material.ARROW, 2));
		        					participant.getPlayer().sendMessage("§8+ §f2 화살");
		        				}
		        				if (!((AbstractJob) participant.getAbility()).hasBow()) {
		        					participant.getPlayer().getInventory().setItem(2, Items.NORMAL_BOW.getStack());
		        				}
							}
						}
					}
					break;
				case 4:
					int shieldcount = random.nextInt(6) + 2;
					getPlayer().sendMessage("§a[§e!§a] §b다이아몬드§f를 맞추어서 실드 " + shieldcount + "개를 획득합니다!");
					Shield.addShield(getParticipant(), shieldcount);
					break;
				}
			} else {
				SoundLib.ENTITY_ARROW_HIT.playSound(getPlayer());
				getPlayer().sendMessage("§a[§e!§a] §c꽝...");
			}
			HandlerList.unregisterAll(this);
			getPlayer().closeInventory();
			isRunning = false;
		}
		
	}
	
}
