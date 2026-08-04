package rainstar.abilitywar.game.murder.murderer;

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
import daybreak.abilitywar.game.list.murdermystery.ability.AbstractMurderer;
import daybreak.abilitywar.utils.base.color.RGB;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.base.math.LocationUtil;
import daybreak.abilitywar.utils.base.math.geometry.Circle;
import daybreak.abilitywar.utils.base.minecraft.nms.NMS;
import daybreak.abilitywar.utils.library.ParticleLib;
import daybreak.abilitywar.utils.library.SoundLib;
import rainstar.abilitywar.effect.Drug;

@AbilityManifest(name = "머더: 마약상", rank = Rank.SPECIAL, species = Species.HUMAN, explain = {
		"모든 시민을 죽이세요!",
		"금 우클릭으로 금 8개를 소모해 활과 화살을 얻을 수 있습니다.",
		"§8[§c밀매§8]§f 금 좌클릭으로 금 6개를 소모해 주변 13칸 내 모든 대상에게",
		"§d마약§f을 주입합니다. §d마약§f 효과는 10초간 지속 및 누적됩니다.",
		"§8[§d마약§8]§f §2멀미§f와 함께 때때로 슬롯이 변경됩니다.",
		"자신이 머더라면 위 효과 대신 §b신속 2§f를 얻습니다."
})
public class DrugDealerMurderer extends AbstractMurderer {

	public DrugDealerMurderer(Participant participant) {
		super(participant);
	}
	
	@Override
	protected void onUpdate(Update update) {
		super.onUpdate(update);
		if (update == Update.RESTRICTION_CLEAR) {
			NMS.sendTitle(getPlayer(), "§e직업§f: §5마약상", "§f모든 §a시민§f을 천국으로 보내버리세요.", 10, 80, 10);
			new AbilityTimer(1) {
				@Override
				protected void onEnd() {
					NMS.clearTitle(getPlayer());
				}
			}.setInitialDelay(TimeUnit.SECONDS, 5).start();
			PASSIVE.start();
			passive.start();
		} else if (update == Update.ABILITY_DESTROY) {
			NMS.setInvisible(getPlayer(), false);
		}
	}

	private final Circle circle = Circle.of(13, 130);
	private static final RGB magenta = RGB.of(254, 1, 130);
	
	private final Predicate<Entity> predicate = new Predicate<Entity>() {
		@Override
		public boolean test(Entity entity) {
			return !(entity instanceof Player) || (getGame().isParticipating(entity.getUniqueId())
					&& !((MurderMystery) getGame()).isDead(entity.getUniqueId())
					&& getGame().getParticipant(entity.getUniqueId()).attributes().TARGETABLE.getValue());
		}
	};
	
	private AbilityTimer passive = new AbilityTimer() {
		
		@Override
		public void run(int count) {
			for (final Location locs : circle.toLocations(getPlayer().getLocation()).floor(getPlayer().getLocation().getY())) {
				ParticleLib.REDSTONE.spawnParticle(getPlayer(), locs, magenta);
			}
		}
		
	}.setPeriod(TimeUnit.TICKS, 2).register();
	
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
				if (murderMystery.consumeGold(getParticipant(), 6)) {
					for (Player player : LocationUtil.getNearbyEntities(Player.class, getPlayer().getLocation(), 13, 13, predicate)) {
						Drug.apply(getGame().getParticipant(player), TimeUnit.TICKS, 200);
						SoundLib.ENTITY_GENERIC_DRINK.playSound(player, 1, 0.5f);
						SoundLib.ENTITY_GENERIC_DRINK.playSound(player, 1, 0.75f);
						SoundLib.ENTITY_GENERIC_DRINK.playSound(player, 1, 1);
						SoundLib.ENTITY_GENERIC_DRINK.playSound(player, 1, 1.5f);
						SoundLib.ENTITY_GENERIC_DRINK.playSound(player, 1, 2);
						SoundLib.ENTITY_VEX_CHARGE.playSound(player, 1, 0.65f);
					}
					getPlayer().sendMessage("§4[§c!§4] §d마약§f을 사용하였습니다.");
				}
			}
		}
	}
	
}
