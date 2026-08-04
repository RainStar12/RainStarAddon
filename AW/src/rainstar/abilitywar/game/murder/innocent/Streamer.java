package rainstar.abilitywar.game.murder.innocent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.StringJoiner;
import java.util.function.Predicate;

import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

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
import daybreak.abilitywar.utils.base.concurrent.SimpleTimer.TaskType;
import daybreak.abilitywar.utils.base.math.LocationUtil;
import daybreak.abilitywar.utils.base.minecraft.nms.NMS;
import daybreak.abilitywar.utils.library.SoundLib;

@AbilityManifest(name = "시민: 스트리머", rank = Rank.SPECIAL, species = Species.HUMAN, explain = {
		"금 우클릭으로 금 8개를 소모해 활과 화살을 얻을 수 있습니다.",
		"§8[§a공개§8]§f 금 좌클릭으로 금 20개를 소모해 스트리머임을 공개합니다.",
		"공개 시마다 §8[§e후원§8]§f으로 얻는 금 수급량이 1 증가합니다.",
		"§8[§b방송§8]§f 사망 시, 자신의 범위 40칸 내에 있던 모든 사람을",
		"예외 없이 이름을 공개합니다. 순서는 무작위입니다.",
		"§8[§e후원§8]§f 45초마다, 모든 §5머더§f는 자신에게 금 1개를 지불합니다."
})
public class Streamer extends AbstractInnocent {

	public Streamer(Participant participant) {
		super(participant);
	}
	
	@Override
	protected void onUpdate(Update update) {
		super.onUpdate(update);
		if (update == Update.RESTRICTION_CLEAR) {
			NMS.sendTitle(getPlayer(), "§e역할§f: §a스트리머", "§f공개적인 방송으로 §5머더§f의 위협을 담아내세요!", 10, 80, 10);
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
		}
	}
	
	private final Predicate<Entity> predicate = new Predicate<Entity>() {
		@Override
		public boolean test(Entity entity) {
			if (entity.equals(getPlayer())) return false;
			return !(entity instanceof Player) || (getGame().isParticipating(entity.getUniqueId())
					&& !((MurderMystery) getGame()).isDead(entity.getUniqueId()));
		}
	};
	
	private ActionbarChannel ac = newActionbarChannel();
	private int goldcount = 1;
	
	public AbilityTimer passive = new AbilityTimer(TaskType.REVERSE, 10000) {
		
		@Override
		public void run(int count) {
			ac.update("§e다음 수금까지§7: §a" + (count % 45) + "§f초 §7/ §b" + goldcount + "§f개");
			if (count % 45 == 0) {
				final MurderMystery murderMystery = (MurderMystery) getGame();
				int finalgold = 0;
				for (Participant participant : getGame().getParticipants()) {
					if (participant.getAbility() instanceof AbstractMurderer) {
						int murdergold = 0;
						for (int i = 0; i < goldcount; i++) {
							if (murderMystery.consumeGold(participant, 1)) murdergold++;
							else break;
						}
						participant.getPlayer().sendMessage("§4[§c!§4] §a스트리머 §e" + getPlayer().getName() + "§f님에게 금 " + murdergold + "개를 후원하셨습니다.");
						finalgold += murdergold;
					}
				}
				SoundLib.ENTITY_PLAYER_LEVELUP.playSound(getPlayer(), 1, 1.75f);
				getPlayer().sendMessage("§8[§e후원§8]§f §a금 " + finalgold + "개를 후원받았습니다.");
				murderMystery.addGold(getParticipant(), finalgold);
			}
		}
		
	}.setPeriod(TimeUnit.SECONDS, 1).register();
	
	@SubscribeEvent
	private void onMurder(MurderEvent e) {
		if (e.getTarget().equals(getParticipant()) && !e.isCancelled()) {
			broadcast();
		}
	}
	
	@SubscribeEvent
	private void onArrowKill(ArrowKillEvent e) {
		if (e.getTarget().equals(getParticipant()) && !e.isCancelled()) {
			broadcast();
		}
	}
	
	private void broadcast() {
		List<Player> players = new ArrayList<>();
		for (Player player : LocationUtil.getNearbyEntities(Player.class, getPlayer().getLocation(), 40, 40, predicate)) {
			players.add(player);
		}
		Collections.shuffle(players);
		final StringJoiner joiner = new StringJoiner("§f, ");
		for (Player player : players) {
			joiner.add("§e" + player.getName());
		}
		Bukkit.broadcastMessage("§4[§c!§4] §a스트리머§f 주변 40칸에 있던 인원 명단");
		Bukkit.broadcastMessage(joiner.toString());
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
				MurderMystery murderMystery = (MurderMystery) getGame();
				if (murderMystery.consumeGold(getParticipant(), 20)) {
					Bukkit.broadcastMessage("§8[§a공개§8] §a" + getPlayer().getName() + "§f님은 스트리머입니다.");
					goldcount++;
				}
			}
		}
	}
	
}
