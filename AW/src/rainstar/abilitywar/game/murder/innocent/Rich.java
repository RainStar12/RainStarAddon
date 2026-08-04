package rainstar.abilitywar.game.murder.innocent;

import java.util.function.Predicate;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.SubscribeEvent;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.list.murdermystery.Items;
import daybreak.abilitywar.game.list.murdermystery.MurderMystery;
import daybreak.abilitywar.game.list.murdermystery.ability.AbstractInnocent;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.base.math.LocationUtil;
import daybreak.abilitywar.utils.base.minecraft.nms.NMS;

@AbilityManifest(name = "시민: 부자", rank = Rank.SPECIAL, species = Species.HUMAN, explain = {
		"금 우클릭으로 금 8개를 소모해 활과 화살을 얻을 수 있습니다.",
		"§8[§e재력§8]§f 자신의 주변에 금이 5초마다 하나 더 생성됩니다.",
		"§8[§a기부§8]§f 다른 대상을 바라보고 금을 좌클릭하면 금을 1개 소모하여,",
		"대상에게 §e금을 1개 줄 수 있습니다§f."
})
public class Rich extends AbstractInnocent {

	public Rich(Participant participant) {
		super(participant);
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
	
	@Override
	protected void onUpdate(Update update) {
		super.onUpdate(update);
		if (update == Update.RESTRICTION_CLEAR) {
			NMS.sendTitle(getPlayer(), "§e역할§f: §a부자", "§f시민에게 부를 베푸세요!", 10, 80, 10);
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
				Player player = LocationUtil.getEntityLookingAt(Player.class, getPlayer(), 15, predicate);
				if (player != null) {
					if (murderMystery.consumeGold(getParticipant(), 1)) {
						getPlayer().sendMessage("§4[§c!§4] §b" + player.getName() + "§f님에게 §e노블레스 오블리주§f를 사용하였습니다.");	
						murderMystery.addGold(getGame().getParticipant(player), 1);
					}
				} else getPlayer().sendMessage("§4[§c!§4] §c바라보는 방향에 사람이 없습니다.");	
			}
		}
	}
	
	public AbilityTimer passive = new AbilityTimer() {
		
		@Override
		public void run(int count) {
			Location loc = LocationUtil.getRandomLocations(getPlayer().getLocation(), 15, 1).get(0);
			loc.setY(LocationUtil.getFloorYAt(loc.getWorld(), getPlayer().getLocation().getY(), loc.getBlockX(), loc.getBlockZ()));
			getPlayer().getWorld().dropItemNaturally(loc, Items.GOLD.getStack());
		}
		
	}.setPeriod(TimeUnit.SECONDS, 5).register();
	
}
