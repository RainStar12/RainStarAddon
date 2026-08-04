package rainstar.abilitywar.game.murder.innocent;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;

import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.SubscribeEvent;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.list.murdermystery.Items;
import daybreak.abilitywar.game.list.murdermystery.MurderMystery;
import daybreak.abilitywar.game.list.murdermystery.ability.AbstractInnocent;
import daybreak.abilitywar.game.list.murdermystery.ability.AbstractMurderer;
import daybreak.abilitywar.utils.base.color.RGB;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.base.concurrent.SimpleTimer.TaskType;
import daybreak.abilitywar.utils.base.math.LocationUtil;
import daybreak.abilitywar.utils.base.math.geometry.Circle;
import daybreak.abilitywar.utils.base.minecraft.nms.NMS;
import daybreak.abilitywar.utils.library.ParticleLib;
import daybreak.abilitywar.utils.library.SoundLib;

@AbilityManifest(name = "시민: 추적자", rank = Rank.SPECIAL, species = Species.HUMAN, explain = {
		"금 우클릭으로 금 8개를 소모해 활과 화살을 얻을 수 있습니다.",
		"§8[§7작업§8]§f 금 좌클릭으로 금 4개를 소모해, 현재 위치에 §3추적 장치§f를 매설합니다.",
		"§3추적 장치§f는 최대 3개까지 동시에 존재하며, 1분 후부터 작동합니다.",
		"§8[§3추적§8]§f §3추적 장치§f의 사거리 5 이내로 §c머더§f가 들어온다면,",
		"즉시 추적자에게 §c무§a슨 §b색§f §3추적 장치§f에서 §c머더§f가 감지됐는지 안내되고 사라집니다.",
		"§8[§c감지§8]§f 사거리 15칸 이내에 사람이 들어온다면 감지해 소리로 알려줍니다."
})
public class Tracker extends AbstractInnocent {

	public Tracker(Participant participant) {
		super(participant);
	}

	@Override
	protected void onUpdate(Update update) {
		super.onUpdate(update);
		if (update == Update.RESTRICTION_CLEAR) {
			NMS.sendTitle(getPlayer(), "§e역할§f: §a추적자", "§f위치추적기를 매설하고 §c머더§f를 감지하세요!", 10, 80, 10);
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
	
	private Map<RGB, Trackers> tracker = new HashMap<>();
	private Player target;
	private Circle circle = Circle.of(15, 100);
	private static final RGB skyblue = RGB.of(160, 227, 245);
	
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
				if (tracker.size() < 3) {
					final MurderMystery murderMystery = (MurderMystery) getGame();
					if (murderMystery.consumeGold(getParticipant(), 4)) {
						RGB color;
						if (!tracker.keySet().contains(RGB.RED)) color = RGB.RED;
						else if (!tracker.keySet().contains(RGB.GREEN)) color = RGB.GREEN;
						else color = RGB.BLUE;
						tracker.put(color, new Trackers(color, getPlayer().getLocation()));
						getPlayer().sendMessage("§c[§e!§c] §f" + getColorName(color) + " §f추적 장치를 매설하였습니다.");
					}
				} else getPlayer().sendMessage("§c[§e!§c] 추적 장치는 최대 3개까지만 매설할 수 있습니다.");
			}
		}
	}
	
	@SubscribeEvent
	public void onPlayerMove(PlayerMoveEvent e) {
		Participant participant = getGame().getParticipant(e.getPlayer());
		if (participant != null && !getParticipant().equals(participant) && getPlayer().getWorld().equals(participant.getPlayer().getWorld())) {
			if (!LocationUtil.isInCircle(getPlayer().getLocation(), e.getFrom(), 15) && LocationUtil.isInCircle(getPlayer().getLocation(), e.getTo(), 15)) {
				if (!participant.getPlayer().equals(target)) getPlayer().sendMessage("§8[§c감지§8]§c 누군가가 접근 중입니다!!!");
				target = participant.getPlayer();
				new AbilityTimer(TaskType.REVERSE, 20) {
					
					@Override
					public void onStart() {
						SoundLib.ENTITY_ARROW_HIT_PLAYER.playSound(getPlayer(), target.getLocation(), 1f, 2f);
					}
					
					@Override
					public void run(int count) {
						if (count % 10 == 0) {
							SoundLib.ENTITY_ARROW_HIT_PLAYER.playSound(getPlayer(), target.getLocation(), 1f, 2f);
						}
					}
					
				}.setPeriod(TimeUnit.TICKS, 1).start();
			}
		}
		
		if (target != null) {
			if (LocationUtil.isInCircle(getPlayer().getLocation(), e.getFrom(), 15) && !LocationUtil.isInCircle(getPlayer().getLocation(), e.getTo(), 15)) {
				target = null;
			}
		}
	}
	
	private AbilityTimer passive = new AbilityTimer() {
		
		@Override
		public void run(int count) {
			for (final Location locs : circle.toLocations(getPlayer().getLocation()).floor(getPlayer().getLocation().getY())) {
				ParticleLib.REDSTONE.spawnParticle(getPlayer(), locs, skyblue);
			}
		}
		
	}.setPeriod(TimeUnit.TICKS, 5).register();
	
	public String getColorName(RGB rgb) {
		if (rgb.equals(RGB.RED)) return "§c빨간색";
		else if (rgb.equals(RGB.GREEN)) return "§a초록색";
		else return "§b파란색";
	}
	
	private class Trackers extends AbilityTimer {
		
		private final RGB rgb;
		private final Location location;
		private Circle circle = Circle.of(5, 55);
		
		private final Predicate<Entity> predicate = new Predicate<Entity>() {
			@Override
			public boolean test(Entity entity) {
				if (entity.equals(getPlayer())) return false;
				return !(entity instanceof Player) || (getGame().isParticipating(entity.getUniqueId())
						&& !((MurderMystery) getGame()).isDead(entity.getUniqueId())
						&& getGame().getParticipant(entity.getUniqueId()).attributes().TARGETABLE.getValue())
						&& getGame().getParticipant(entity.getUniqueId()).hasAbility()
						&& (getGame().getParticipant(entity.getUniqueId()).getAbility() instanceof AbstractMurderer);
			}
		};
		
		private Trackers(RGB rgb, Location location) {
			super(TaskType.NORMAL, 10000000);
			setPeriod(TimeUnit.TICKS, 1);
			this.rgb = rgb;
			this.location = location;
			start();
		}
		
		@Override
		protected void run(int count) {
			if (count % 5 == 0) {
				for (final Location locs : circle.toLocations(location).floor(location.getY())) {
					ParticleLib.REDSTONE.spawnParticle(getPlayer(), locs, rgb);
				}	
			}
			if (count == 1200) getPlayer().sendMessage("§4[§c!§4] " + getColorName(rgb) + " §f추적 장치가 활성화되었습니다.");
			if (count > 1200) {
				if (LocationUtil.getNearbyEntities(Player.class, location, 5, 5, predicate).size() > 0) {
					SoundLib.ENTITY_PLAYER_LEVELUP.playSound(getPlayer(), 1f, 1.6f);
					getPlayer().sendMessage("§4[§c!§4] §c머더§f가 " + getColorName(rgb) + " §f추적 장치를 밟았습니다!");
					this.stop(false);
				}	
			}
		}
		
		@Override
		protected void onEnd() {
			onSilentEnd();
		}
		
		@Override
		protected void onSilentEnd() {
			tracker.remove(rgb);
		}
		
	}
	
}
