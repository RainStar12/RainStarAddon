package rainstar.abilitywar.game.murder.system.detectivebuffer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import daybreak.abilitywar.AbilityWar;
import daybreak.abilitywar.game.AbstractGame;
import daybreak.abilitywar.game.AbstractGame.GameTimer;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.list.murdermystery.ability.AbstractInnocent;
import daybreak.abilitywar.game.list.murdermystery.ability.Detective;
import daybreak.abilitywar.game.module.Module;
import daybreak.abilitywar.game.module.ModuleBase;
import daybreak.abilitywar.utils.base.Messager;
import daybreak.abilitywar.utils.base.collect.LimitedPushingList;
import daybreak.abilitywar.utils.base.concurrent.SimpleTimer;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.base.minecraft.item.builder.ItemBuilder;
import daybreak.abilitywar.utils.library.MaterialX;
import daybreak.abilitywar.utils.library.SoundLib;

@ModuleBase(DetectiveBuffer.class)
public class DetectiveBuffer extends GameTimer implements Module {
	
	public DetectiveBuffer(AbstractGame abstractGame) {
		abstractGame.super(TaskType.REVERSE, 50);
		setPeriod(TimeUnit.SECONDS, 1);
	}
	
	private Set<Participant> guiopens = new HashSet<>();
	
	@Override
	public void run(int count) {
		for (Participant participant : getGame().getParticipants()) {
			if (participant.hasAbility()) {
				if (participant.getAbility().getClass().equals(Detective.class) && !guiopens.contains(participant)) {
					new SelectGUI(participant).start();
				}
			}
		}
	}

	@Override
	public void register() {
		for (Participant participant : getGame().getParticipants()) {
			if (participant.hasAbility()) {
				if (participant.getAbility().getClass().equals(Detective.class) && !guiopens.contains(participant)) {
					new SelectGUI(participant).start();
				}
			}
		}
		start();
	}

	@Override
	public void unregister() {
	}
	
	public class SelectGUI extends SimpleTimer implements Listener {
		
		private final ItemStack NULL = (new ItemBuilder(MaterialX.GRAY_STAINED_GLASS_PANE)).displayName(" ").build();
		private final LimitedPushingList<Class<? extends AbstractInnocent>> list = new LimitedPushingList<>(3);
		private Map<Integer, Class<? extends AbstractInnocent>> slots = new HashMap<>();
		
		private Class<? extends AbstractInnocent> selected;
		private final Inventory gui;
    	List<Class<? extends AbstractInnocent>> jobs = new ArrayList<>();
    	private final Participant participant;
		
		public SelectGUI(Participant participant) {
			super(TaskType.REVERSE, 800);
			setPeriod(TimeUnit.TICKS, 1);
			this.participant = participant;
			guiopens.add(participant);
			gui = Bukkit.createInventory(null, InventoryType.HOPPER, "§0옵션을 선택해주세요.");
			createJobList();
		}
		
		private void createJobList() {
			jobs.add(DetectiveDisarmament.class);
	    	jobs.add(DetectiveFastRegen.class);
	    	jobs.add(DetectiveGlower.class);
	    	jobs.add(DetectiveGoldDetect.class);
	    	jobs.add(DetectiveGoldGiver.class);
	    	jobs.add(DetectiveHomingUp.class);
	    	jobs.add(DetectiveInfiniteRegen.class);
	    	jobs.add(DetectiveIntuition.class);
	    	jobs.add(DetectiveMonitoring.class);
	    	jobs.add(DetectiveNonCharge.class);
	    	jobs.add(DetectivePsychometry.class);
	    	jobs.add(DetectiveResponsibility.class);
	    	jobs.add(DetectiveShield.class);
	    	jobs.add(DetectiveSpeedUp.class);
	    	jobs.add(DetectiveTaxCut.class);
	    	
			Collections.shuffle(jobs);
			list.add(jobs.get(0));
			list.add(jobs.get(1));
			list.add(jobs.get(2));
		}
		
		private MaterialX getJobBlock(Class<? extends AbstractInnocent> job) {
			if (job.equals(DetectiveDisarmament.class)) return MaterialX.BARRIER;
			else if (job.equals(DetectiveFastRegen.class)) return MaterialX.CLOCK;
			else if (job.equals(DetectiveGlower.class)) return MaterialX.GLOWSTONE_DUST;
			else if (job.equals(DetectiveGoldDetect.class)) return MaterialX.GOLDEN_PICKAXE;
			else if (job.equals(DetectiveGoldGiver.class)) return MaterialX.GOLD_INGOT;
			else if (job.equals(DetectiveHomingUp.class)) return MaterialX.BOW;
			else if (job.equals(DetectiveInfiniteRegen.class)) return MaterialX.ARROW;
			else if (job.equals(DetectiveIntuition.class)) return MaterialX.IRON_SWORD;
			else if (job.equals(DetectiveMonitoring.class)) return MaterialX.MUSIC_DISC_11;
			else if (job.equals(DetectiveNonCharge.class)) return MaterialX.SPECTRAL_ARROW;
			else if (job.equals(DetectivePsychometry.class)) return MaterialX.ENDER_EYE;
			else if (job.equals(DetectiveResponsibility.class)) return MaterialX.IRON_CHESTPLATE;
			else if (job.equals(DetectiveShield.class)) return MaterialX.SHULKER_SHELL;
			else if (job.equals(DetectiveSpeedUp.class)) return MaterialX.DIAMOND_BOOTS;
			else if (job.equals(DetectiveStakeOut.class)) return MaterialX.GRASS;
			else if (job.equals(DetectiveTaxCut.class)) return MaterialX.PAPER;
			return null;
		}
		
		private String getOptionName(Class<? extends AbstractInnocent> job) {
			if (job.equals(DetectiveDisarmament.class)) return "§3무장해제";
			else if (job.equals(DetectiveFastRegen.class)) return "§3보급속도+";
			else if (job.equals(DetectiveGlower.class)) return "§3감지";
			else if (job.equals(DetectiveGoldDetect.class)) return "§3재물조사";
			else if (job.equals(DetectiveGoldGiver.class)) return "§3재분배";
			else if (job.equals(DetectiveHomingUp.class)) return "§3유도력+";
			else if (job.equals(DetectiveInfiniteRegen.class)) return "§3무한 보급";
			else if (job.equals(DetectiveIntuition.class)) return "§3직감";
			else if (job.equals(DetectiveMonitoring.class)) return "§3감청";
			else if (job.equals(DetectiveNonCharge.class)) return "§3즉발";
			else if (job.equals(DetectivePsychometry.class)) return "§3사이코메트리";
			else if (job.equals(DetectiveResponsibility.class)) return "§3책임감";
			else if (job.equals(DetectiveShield.class)) return "§3보호막";
			else if (job.equals(DetectiveSpeedUp.class)) return "§3이속+";
			else if (job.equals(DetectiveStakeOut.class)) return "§3잠복";
			else if (job.equals(DetectiveTaxCut.class)) return "§3세금 감면";
			return null;
		}
		
		private String getOptionExplain(Class<? extends AbstractInnocent> job) {
			if (job.equals(DetectiveDisarmament.class)) return "§f금 좌클릭으로 금 6개를 소모해 모든 머더가 4초간 금을 소모하지 못하게 합니다. §c쿨타임 §7: §f12초";
			else if (job.equals(DetectiveFastRegen.class)) return "§f화살을 2초마다 보급받습니다.";
			else if (job.equals(DetectiveGlower.class)) return "§f금 좌클릭으로 금 4개를 소모해 원하는 대상을 10초간 발광시킵니다.";
			else if (job.equals(DetectiveGoldDetect.class)) return "§f금 F키로 금 4개를 소모해 대상을 정해 대상이 §5금을 소모할 때마다§f 소모량을 알아냅니다. 금 좌클릭으로 모든 대상의 금 소지량을 알아냅니다. §c쿨타임 §7: §f10초";
			else if (job.equals(DetectiveGoldGiver.class)) return "§f금 획득 시 다른 시민팀 중 두 명도 그만큼 금을 얻습니다.";
			else if (job.equals(DetectiveHomingUp.class)) return "§f화살 유도 범위가 √5칸으로 강화됩니다.";
			else if (job.equals(DetectiveInfiniteRegen.class)) return "§f화살이 1~20초마다 보급되며, 최대 64개까지 보급받습니다.";
			else if (job.equals(DetectiveIntuition.class)) return "§f화살이 5초마다 보급됩니다. 머더가 검을 들면 1초 무적이 되며, 자신에게 소리가 들리고 나침반이 해당 방향을 가리킵니다.";
			else if (job.equals(DetectiveMonitoring.class)) return "§f머더가 머더 채팅을 할 때 대화 내용을 엿듣습니다. 영매사의 채팅을 엿듣습니다. 1분 30초 후, 무작위 머더 하나의 직업을 알아냅니다.";
			else if (job.equals(DetectiveNonCharge.class)) return "§f활 좌클릭으로도 화살이 발사되나 화살 속도가 느립니다. 좌클릭 발사 시 50% 확률로 화살을 소모하지 않습니다.";
			else if (job.equals(DetectivePsychometry.class)) return "§f화살이 최대 4개까지 보급됩니다. 30초간 나침반이 가장 마지막으로 죽은 대상의 위치를 가리킵니다. 사망 위치에 사망한 사람의 이름과 시간, 능력이 정확하게 표기됩니다. 사망 위치에 사망 당시 대상 주변 20칸 이내의 사람 명단이 나타납니다.";
			else if (job.equals(DetectiveResponsibility.class)) return "§f시민 팀이 죽을 때마다 실드 1개를 받습니다.";
			else if (job.equals(DetectiveShield.class)) return "§f웅크리면 금 8개를 소모해 2초간 무적 상태가 됩니다. §c쿨타임 §7: §f2초";
			else if (job.equals(DetectiveSpeedUp.class)) return "§f이동 속도 40% 증가를 항시 받습니다.";
			else if (job.equals(DetectiveStakeOut.class)) return "§f실드를 1개 얻고 시작하고, 항상 지정 불가 상태입니다.";
			else if (job.equals(DetectiveTaxCut.class)) return "§f시민팀이 금을 소모할 때 금 25%를 덜 소모합니다. §8(§7최소 1개 소모§8)";
			return null;
		}
		
		private void placeItem() {
			for (int i = 0; i < 3; i++) {
				list.add(jobs.get(i));
				ItemStack item = new ItemBuilder(getJobBlock(list.get(i))).build();
				ItemMeta meta = item.getItemMeta();
				meta.setDisplayName(getOptionName(list.get(i)));
				List<String> lore = Messager.asList();
				String exp = getOptionExplain(list.get(i));
				int len = exp.length();
				StringBuilder builder = new StringBuilder("§f");
				int spaces = 0;
				for (int j = 0; j < len; j++) {
				    char c = exp.charAt(j);
				    if (c == ' ') spaces++;
				    builder.append(c);
				    if (spaces == 5) {
				    	spaces = 0;
				        lore.add(builder.toString());
				        builder = new StringBuilder("§f");
				    }
				}
				lore.add(builder.toString());
				meta.setLore(lore);
				item.setItemMeta(meta);
				item.addUnsafeEnchantment(Enchantment.ARROW_INFINITE, 1);
				gui.setItem((i * 2), item);
				slots.put((i * 2), list.get(i));
			}
			gui.setItem(1, NULL);
			gui.setItem(3, NULL);
		}
		
		protected void onStart() {
			Bukkit.getPluginManager().registerEvents(this, AbilityWar.getPlugin());
			placeItem();
			participant.getPlayer().sendMessage("§3[§!§3] §a10초 후 옵션 선택 GUI가 나타납니다!");
		}
		
		protected void run(int arg0) {
			switch(arg0) {
			case 700:
			case 680:
			case 660:
			case 640:
			case 620:
				SoundLib.ENTITY_ARROW_HIT_PLAYER.playSound(participant.getPlayer(), 1, 1.5f);
				break;
			case 600:
				SoundLib.ENTITY_ARROW_HIT_PLAYER.playSound(participant.getPlayer(), 1, 1.75f);
				participant.getPlayer().openInventory(gui);
				break;
			}
			placeItem();
			if (arg0 == 60) SoundLib.BLOCK_NOTE_BLOCK_SNARE.playSound(participant.getPlayer(), 1, 1.7f); 
			if (arg0 == 40) SoundLib.BLOCK_NOTE_BLOCK_SNARE.playSound(participant.getPlayer(), 1, 1.7f); 
			if (arg0 == 20) SoundLib.BLOCK_NOTE_BLOCK_SNARE.playSound(participant.getPlayer(), 1, 1.7f); 
		}
		
		@Override
		protected void onEnd() {
			onSilentEnd();
		}
		
		@Override
		protected void onSilentEnd() {
			try {
				if (selected != null) participant.setAbility(selected);
				else participant.setAbility(list.get(0));
			} catch (UnsupportedOperationException | ReflectiveOperationException e) {
				e.printStackTrace();
			}
			HandlerList.unregisterAll(this);
			participant.getPlayer().closeInventory();
			guiopens.remove(participant);
		}
		
		@EventHandler
		private void onInventoryClose(InventoryCloseEvent e) {
			if (e.getInventory().equals(gui)) stop(false);
		}

		@EventHandler
		private void onQuit(PlayerQuitEvent e) {
			if (e.getPlayer().getUniqueId().equals(participant.getPlayer().getUniqueId())) stop(false);
		}
		
		@EventHandler
		private void onPlayerMove(PlayerMoveEvent e) {
			if (e.getPlayer().equals(participant.getPlayer()) && this.getCount() < 600) e.setCancelled(true);
		}
		
		@EventHandler
		private void onEntityDamage(EntityDamageEvent e) {
			if (e.getEntity().equals(participant.getPlayer())) e.setDamage(e.getDamage() / 2);
		}

		@EventHandler
		private void onInventoryClick(InventoryClickEvent e) {
			if (e.getInventory().equals(gui)) {
				e.setCancelled(true);
				if (slots.containsKey(e.getSlot())) {
					selected = slots.get(e.getSlot());
					participant.getPlayer().closeInventory();
				}
			}
		}
		
	}
	
}