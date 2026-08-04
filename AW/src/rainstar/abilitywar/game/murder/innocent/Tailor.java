package rainstar.abilitywar.game.murder.innocent;

import java.util.function.Predicate;

import org.bukkit.GameMode;
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
import daybreak.abilitywar.utils.library.SoundLib;
import rainstar.abilitywar.game.murder.system.Shield;

@AbilityManifest(name = "시민: 재봉사", rank = Rank.SPECIAL, species = Species.HUMAN, explain = {
		"금 우클릭으로 금 8개를 소모해 활과 화살을 얻을 수 있습니다.",
		"§8[§6직물§8]§f 금 좌클릭으로 금 8개를 소모해 바라보는 대상에게",
		"§3실드§f를 1개 부여합니다. 바라보는 대상이 없다면 자신이 받습니다.",
		"대상이 가진 §3실드§f 1개당 금을 3개씩 더 소모합니다."
})
public class Tailor extends AbstractInnocent {

	public Tailor(Participant participant) {
		super(participant);
	}

	@Override
	protected void onUpdate(Update update) {
		super.onUpdate(update);
		if (update == Update.RESTRICTION_CLEAR) {
			NMS.sendTitle(getPlayer(), "§e역할§f: §a재봉사", "§f옷을 짜내어 §a시민§f을 보호하세요!", 10, 80, 10);
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
	
	private final Predicate<Entity> predicate = new Predicate<Entity>() {
		@Override
		public boolean test(Entity entity) {
			if (entity.equals(getPlayer())) return false;
			return !(entity instanceof Player) || (getGame().isParticipating(entity.getUniqueId())
					&& !((MurderMystery) getGame()).isDead(entity.getUniqueId())
					&& getGame().getParticipant(entity.getUniqueId()).attributes().TARGETABLE.getValue())
					&& getGame().getParticipant(entity.getUniqueId()).hasAbility()
					&& !((Player) entity).getGameMode().equals(GameMode.SPECTATOR);
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
				Player player = LocationUtil.getEntityLookingAt(Player.class, getPlayer(), 120, predicate);
				if (player != null) {
					final MurderMystery murderMystery = (MurderMystery) getGame();
					if (murderMystery.consumeGold(getParticipant(), Math.min(64, 8 + (Shield.getShield(getGame().getParticipant(player)) * 3)))) {	
						Shield.addShield(getGame().getParticipant(player), 1);
						new AbilityTimer(3) {
							
							@Override
							public void run(int count) {
								SoundLib.BLOCK_WOOL_PLACE.playSound(getPlayer());
								SoundLib.BLOCK_WOOL_PLACE.playSound(player);
							}
							
							@Override
							public void onEnd() {
								SoundLib.ITEM_ARMOR_EQUIP_LEATHER.playSound(getPlayer(), 1f, 0.5f);
								SoundLib.ITEM_ARMOR_EQUIP_LEATHER.playSound(player, 1f, 0.5f);
							}
							
						}.setPeriod(TimeUnit.TICKS, 10).start();
						getPlayer().sendMessage("§3[§b!§3] §e" + player.getName() + "§f님에게 §3실드§f를 부여합니다.");	
					} else getPlayer().sendMessage("§3[§b!§3] §c금이 §e" + Math.min(64, 8 + (Shield.getShield(getGame().getParticipant(player)) * 3)) + "§c개 필요합니다.");
				} else {
					final MurderMystery murderMystery = (MurderMystery) getGame();
					if (murderMystery.consumeGold(getParticipant(), Math.min(64, 8 + (Shield.getShield(getParticipant()) * 3)))) {	
						Shield.addShield(getParticipant(), 1);
						new AbilityTimer(3) {
							
							@Override
							public void run(int count) {
								SoundLib.BLOCK_WOOL_PLACE.playSound(getPlayer());
							}
							
							@Override
							public void onEnd() {
								SoundLib.ITEM_ARMOR_EQUIP_LEATHER.playSound(getPlayer(), 1f, 0.5f);
							}
							
						}.setPeriod(TimeUnit.TICKS, 10).start();
						getPlayer().sendMessage("§3[§b!§3] §3실드§f를 획득합니다.");	
					} else getPlayer().sendMessage("§3[§b!§3] §c금이 §e" + Math.min(64, 8 + (Shield.getShield(getParticipant()) * 3)) + "§c개 필요합니다.");
				}
			}
		}
	}
	
}
