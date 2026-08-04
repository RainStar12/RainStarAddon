package rainstar.abilitywar.game.murder.innocent;

import java.text.DecimalFormat;
import java.util.function.Predicate;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerGameModeChangeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;

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
import daybreak.abilitywar.utils.base.minecraft.nms.NMS;
import daybreak.abilitywar.utils.library.PotionEffects;

@AbilityManifest(name = "시민: 감시자", rank = Rank.SPECIAL, species = Species.HUMAN, explain = {
		"금 우클릭으로 금 8개를 소모해 활과 화살을 얻을 수 있습니다.",
		"§8[§c요주의§8]§f 금 좌클릭으로 금 4개를 소모해 바라보는 대상을 §c지정§f할 수 있습니다.",
		"§c지정§f은 단일 대상이며, 재사용 시 §c지정 대상§f을 변경 가능합니다.",
		"나침반의 방향이 §c지정§f한 대상으로 잡힙니다.",
		"§8[§3감시§8]§f 금 F키로 지정 대상을 10초간 §3관전§f합니다. 지정 후 3회만 가능하며,",
		"재지정 시마다 다시 3회씩 사용할 수 있습니다.",
		"§8[§e발각§8]§f §c지정§f된 대상이 누군가를 살해한다면 대상은 20초간 §e발광§f, §3감속§f됩니다."
})
public class Watcher extends AbstractInnocent {
	
	public Watcher(Participant participant) {
		super(participant);
	}
	
	private final Predicate<Entity> predicate = new Predicate<Entity>() {
		@Override
		public boolean test(Entity entity) {
			if (entity.equals(getPlayer())) return false;
			return !(entity instanceof Player) || (getGame().isParticipating(entity.getUniqueId())
					&& !((MurderMystery) getGame()).isDead(entity.getUniqueId())
					&& getGame().getParticipant(entity.getUniqueId()).attributes().TARGETABLE.getValue())
					&& !((Player) entity).getGameMode().equals(GameMode.SPECTATOR);
		}
	};
	
	private Participant target;
	private DecimalFormat df = new DecimalFormat("0.0");
	private int stack = 3;
	private final ActionbarChannel ac = newActionbarChannel();
	
	@Override
	protected void onUpdate(Update update) {
		super.onUpdate(update);
		if (update == Update.RESTRICTION_CLEAR) {
			NMS.sendTitle(getPlayer(), "§e역할§f: §a감시자", "§f모든 §c머더§f의 행동을 감시하세요!", 10, 80, 10);
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
	
	public AbilityTimer skill = new AbilityTimer(200) {
		
		Location location;
		
		@Override
		public void onStart() {
			location = getPlayer().getLocation();
			if (target != null) {
				getPlayer().setGameMode(GameMode.SPECTATOR);
				getPlayer().setSpectatorTarget(target.getPlayer());	
			} else stop(false);
		}
		
		@Override
		public void run(int count) {
			getParticipant().attributes().TARGETABLE.setValue(false);
			if (target != null) {
				getPlayer().setGameMode(GameMode.SPECTATOR);
				getPlayer().setSpectatorTarget(target.getPlayer());	
			} else stop(false);
			ac.update("§3감시§7: §e" + df.format(count / 20.0) + "§f초");
		}
		
		@Override
		public void onEnd() {
			onSilentEnd();
		}
		
		@Override
		public void onSilentEnd() {
			getParticipant().attributes().TARGETABLE.setValue(true);
			getPlayer().setGameMode(GameMode.ADVENTURE);
			getPlayer().teleport(location);
			ac.update(null);
		}
		
	}.setPeriod(TimeUnit.TICKS, 1).register();
	
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
		}
		
	}.setPeriod(TimeUnit.TICKS, 1).register();
	
	@SubscribeEvent
	private void onTeleport(PlayerTeleportEvent e) {
		if (e.getPlayer().equals(getPlayer()) && skill.isRunning() && e.getCause().equals(TeleportCause.SPECTATE) && skill.getCount() < 195) e.setCancelled(true);
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
				if (!skill.isRunning()) {
					MurderMystery murderMystery = (MurderMystery) getGame();
					Player player = LocationUtil.getEntityLookingAt(Player.class, getPlayer(), 15, predicate);
					if (player != null) {
						if (!getGame().getParticipant(player).equals(target) || stack < 3) {
							if (murderMystery.consumeGold(getParticipant(), 4)) {
								target = getGame().getParticipant(player);
								getPlayer().sendMessage("§4[§c!§4] §b" + player.getName() + "§f님에게 §3감시§f를 사용하였습니다.");	
								stack = 3;
							}	
						} else getPlayer().sendMessage("§4[§c!§4] §c이미 대상을 3번 감시할 수 있습니다.");
					} else getPlayer().sendMessage("§4[§c!§4] §c바라보는 방향에 사람이 없습니다.");		
				}
			}
		}
	}
	
    @SubscribeEvent(onlyRelevant = true, priority = 0)
    public void onPlayerSwapHandItems(PlayerSwapHandItemsEvent e) {
    	if (Items.isGold(e.getOffHandItem()) && e.getPlayer().equals(getPlayer())) {
    		if (target != null) {
        		if (stack > 0) {
            		getPlayer().sendMessage("§4[§c!§4] §3감시§f를 사용합니다.");
            		skill.start();	
            		stack--;
        		} else getPlayer().sendMessage("§4[§c!§4] §c감시를 전부 사용하였습니다.");		
    		} else getPlayer().sendMessage("§4[§c!§4] §c아직 아무도 감시 대상으로 지정하지 않았습니다.");	
    	}
    }
	
	@SubscribeEvent(priority = 10)
	private void onMurder(MurderEvent e) {
		if (!e.isCancelled()) {
			if (e.getMurderer().getParticipant().equals(target)) {
				PotionEffects.GLOWING.addPotionEffect(target.getPlayer(), 400, 1, false, false, true);
				PotionEffects.SLOW.addPotionEffect(target.getPlayer(), 400, 1, false, false, true);
			} else if (e.getTarget().equals(target)) target = null;
		}
	}
	
	@SubscribeEvent(priority = 10)
	private void onArrowKill(ArrowKillEvent e) {
		if (!e.isCancelled()) {
			if (e.getShooter().equals(target)) {
				PotionEffects.GLOWING.addPotionEffect(target.getPlayer(), 400, 1, false, false, true);
				PotionEffects.SLOW.addPotionEffect(target.getPlayer(), 400, 1, false, false, true);
				if (!(e.getShooter().getAbility() instanceof AbstractMurderer) && !(e.getTarget().getAbility() instanceof AbstractMurderer)) target = null;
			} else if (e.getTarget().equals(target)) target = null;
		}
	}
	
	@SubscribeEvent
	private void onGameModeChange(PlayerGameModeChangeEvent e) {
		if (e.getPlayer().equals(getPlayer()) && skill.isRunning() && !e.getNewGameMode().equals(GameMode.SPECTATOR)) {
			e.setCancelled(true);
		}
	}

}
