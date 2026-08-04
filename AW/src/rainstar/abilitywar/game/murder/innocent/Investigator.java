package rainstar.abilitywar.game.murder.innocent;

import java.util.function.Predicate;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.player.PlayerInteractEvent;

import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.SubscribeEvent;
import daybreak.abilitywar.ability.AbilityBase;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.list.murdermystery.Items;
import daybreak.abilitywar.game.list.murdermystery.MurderMystery;
import daybreak.abilitywar.game.list.murdermystery.ability.AbstractInnocent;
import daybreak.abilitywar.game.list.murdermystery.ability.AbstractMurderer;
import daybreak.abilitywar.utils.base.color.RGB;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.base.math.LocationUtil;
import daybreak.abilitywar.utils.base.math.geometry.Circle;
import daybreak.abilitywar.utils.base.math.geometry.Line;
import daybreak.abilitywar.utils.base.minecraft.nms.NMS;
import daybreak.abilitywar.utils.library.ParticleLib;
import rainstar.abilitywar.game.murder.murderer.ScammerMurderer;
import rainstar.abilitywar.game.murder.system.config.statictype.CheckMurdererSword;

@AbilityManifest(name = "시민: 수사관", rank = Rank.SPECIAL, species = Species.HUMAN, explain = {
		"금 우클릭으로 금 8개를 소모해 활과 화살을 얻을 수 있습니다.",
		"§8[§3무영장§8]§f 머더가 검을 얻고 15초가 지나기 전까지 §8[§c잠입§8]§f할 수 없습니다.",
		"§8[§c잠입§8]§f 금 좌클릭으로 금 3개를 소모해 6.5칸 이내 가장 가까운",
		"플레이어를 대상으로 §c수사§f를 시작합니다. 3.5초간 6.5칸 이내의 §b거리를 유지§f하면",
		"§c수사§f를 종결시켜 대상이 §a시민§f인지 §5머더§f인지 알 수 있습니다.",
		"실패 시, 금은 3초 후 돌려받습니다.",
		"§8[§4검거§8]§f §c수사§f 결과가 §5머더§f라면 화살을 하나 얻고,",
		"영구적으로 화살 속도가 50% 증가합니다§8(§7중첩 불가§8)§f."
})
public class Investigator extends AbstractInnocent {
	
	public Investigator(Participant participant) {
		super(participant);
	}
	
	@Override
	protected void onUpdate(Update update) {
		super.onUpdate(update);
		if (update == Update.RESTRICTION_CLEAR) {
			NMS.sendTitle(getPlayer(), "§e역할§f: §a수사관", "§f사람들 중 섞여있는 §5머더§f를 잡아내세요!", 10, 80, 10);
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
	
	private Player player;
	private final Circle circle = Circle.of(6.5, 66);
	private static final RGB cyan = RGB.of(1, 170, 150);
	private static final RGB red = RGB.of(150, 1, 1);
	private CheckMurdererSword checkmurderersword = getGame().getModule(CheckMurdererSword.class);
	private boolean arrowspeed = false;
	
	private final Predicate<Entity> predicate = new Predicate<Entity>() {
		@Override
		public boolean test(Entity entity) {
			if (entity.equals(getPlayer())) return false;
			return !(entity instanceof Player) || (getGame().isParticipating(entity.getUniqueId())
					&& !((MurderMystery) getGame()).isDead(entity.getUniqueId())
					&& getGame().getParticipant(entity.getUniqueId()).attributes().TARGETABLE.getValue());
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
				if (checkmurderersword.isCheckable()) {
					MurderMystery murderMystery = (MurderMystery) getGame();
					if (!skill.isRunning()) {
						if (LocationUtil.getEntitiesInCircle(Player.class, getPlayer().getLocation(), 6.5, predicate).size() > 0) {
							if (murderMystery.consumeGold(getParticipant(), 3)) {
								player = LocationUtil.getNearestEntity(Player.class, getPlayer().getLocation(), predicate);
								skill.start();
								getPlayer().sendMessage("§4[§c!§4] §c수사§f를 시작합니다.");
							}	
						} else getPlayer().sendMessage("§4[§c!§4] §c범위 내에 대상이 없습니다.");	
					} else getPlayer().sendMessage("§4[§c!§4] §c아직 진행중인 수사가 있습니다.");		
				} else getPlayer().sendMessage("§4[§c!§4] §c아직 영장이 나오지 않았습니다.");	
			}
		}
	}
	
	@SubscribeEvent
	public void onProjectileLaunch(ProjectileLaunchEvent e) {
		if (getPlayer().equals(e.getEntity().getShooter()) && NMS.isArrow(e.getEntity())) {
			if (arrowspeed) e.getEntity().setVelocity(e.getEntity().getVelocity().multiply(1.5));
		}
	}
	
	public AbilityTimer passive = new AbilityTimer() {
		
		@Override
		public void run(int count) {
			if (!skill.isRunning()) {
				for (final Location locs : circle.toLocations(getPlayer().getLocation()).floor(getPlayer().getLocation().getY())) {
					ParticleLib.REDSTONE.spawnParticle(getPlayer(), locs, cyan);
				}	
			}
		}
		
	}.setPeriod(TimeUnit.TICKS, 3).register();
	
	public AbilityTimer skill = new AbilityTimer(70) {
		
		@Override
		public void run(int count) {
			if (LocationUtil.getEntitiesInCircle(Player.class, getPlayer().getLocation(), 6.5, predicate).contains(player)) {
				if (count % 2 == 0) {
					for (final Location locs : circle.toLocations(getPlayer().getLocation()).floor(getPlayer().getLocation().getY())) {
						ParticleLib.REDSTONE.spawnParticle(getPlayer(), locs, red);
					}
					for (Location loc : Line.between(getPlayer().getLocation().clone().add(0, 1, 0), player.getLocation().clone().add(0, 1, 0), 25).toLocations(getPlayer().getLocation().clone().add(0, 1, 0))) {
						ParticleLib.TOWN_AURA.spawnParticle(getPlayer(), loc, 0, 0, 0, 1, 0);
					}	
				}
			} else stop(true);
		}
		
		@Override
		public void onEnd() {
			String string = "";
			AbilityBase ab = getGame().getParticipant(player).getAbility();
			if (ab.getClass().getSimpleName().contains("Detective")) string = "§3탐정";
			else if (ab instanceof AbstractInnocent) string = "§a시민";
			else if (ab instanceof AbstractMurderer) string = "§5머더";
			if (ab instanceof ScammerMurderer) string = "§a시민";
			getPlayer().sendMessage("§4[§c!§4] §c수사§a를 성공하셨습니다.");
			getPlayer().sendMessage("§4[§c!§4] §e" + player.getName() + "§f님은 " + string + "§f입니다.");
			if (string == "§5머더") {
				getPlayer().sendMessage("§8[§4검거§8] §5머더§f를 찾아냈기에 화살을 지급받습니다.");
				MurderMystery murderMystery = (MurderMystery) getGame();
				if (!addArrow()) {
					murderMystery.addGold(getParticipant());
				} else {
					if (!hasBow()) {
						getPlayer().getInventory().setItem(2, Items.NORMAL_BOW.getStack());
					}
				}
				arrowspeed = true;
			}
			if (getGame().getParticipant(player).getAbility().getClass().equals(ScammerMurderer.class)) player.sendMessage("§4[§c!§4] §3위장§f을 성공하였습니다.");
		}
		
		@Override
		public void onSilentEnd() {
			getPlayer().sendMessage("§4[§c!§4] §c수사에 실패하셨습니다.");
			new AbilityTimer(TaskType.REVERSE, 60) {
				
				@Override
				public void onEnd() {
					final MurderMystery murderMystery = (MurderMystery) getGame();
					murderMystery.addGold(getParticipant(), 3);
				}
				
			}.setPeriod(TimeUnit.TICKS, 1).start();
		}
		
	}.setPeriod(TimeUnit.TICKS, 1).register();
	
}
