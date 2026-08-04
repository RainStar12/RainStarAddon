package rainstar.abilitywar.game.murder.innocent;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
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
import daybreak.abilitywar.game.list.murdermystery.MurderMystery.ArrowKillEvent;
import daybreak.abilitywar.game.list.murdermystery.ability.AbstractInnocent;
import daybreak.abilitywar.game.list.murdermystery.ability.AbstractMurderer.MurderEvent;
import daybreak.abilitywar.game.manager.effect.Stun;
import daybreak.abilitywar.utils.base.color.RGB;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.base.math.LocationUtil;
import daybreak.abilitywar.utils.base.math.geometry.Circle;
import daybreak.abilitywar.utils.base.minecraft.nms.NMS;
import daybreak.abilitywar.utils.library.ParticleLib;
import daybreak.abilitywar.utils.library.SoundLib;

@AbilityManifest(name = "시민: 경호원", rank = Rank.SPECIAL, species = Species.HUMAN, explain = {
		"금 우클릭으로 금 6개를 소모해 활과 화살을 얻을 수 있습니다.",
		"§8[§b경호§8]§f 금 좌클릭으로 금 6개를 소모해 바라보는 대상을 §b경호 대상§f으로",
		"지정합니다. 대상은 단 한 명만 지정 가능하며, 재사용 시 대상을 변경합니다.",
		"§8[§3임무§8]§f §b경호 대상§f과 5칸 내에 있다면 자신은 죽지 않습니다.",
		"경호 대상에게 공격받으면 경호가 해제되고 공격자를 잠깐 §e§n기절§f시킵니다.",
		"§8[§c책임§8]§f §b경호 대상§f이 공격당하면 §b같은 대상§f당 딱 한 번,",
		"공격을 1초간 막아내고 대상에게로 순간 이동하며, 공격자를 잠깐 §e§n기절§f시킵니다.",
})
public class BodyGuard extends AbstractInnocent {
	
	public BodyGuard(Participant participant) {
		super(participant);
	}
	
	@Override
	protected void onUpdate(Update update) {
		super.onUpdate(update);
		if (update == Update.RESTRICTION_CLEAR) {
			NMS.sendTitle(getPlayer(), "§e역할§f: §a경호원", "§f경호 대상을 철저히 지켜내세요!", 10, 80, 10);
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
	
	private Participant target;
	private static final RGB red = RGB.of(181, 19, 19);
	private static final RGB green = RGB.of(127, 254, 1);
	private final Circle circle = Circle.of(5, 45);
	private Set<UUID> blocked = new HashSet<>();
	
	private final Predicate<Entity> predicate = new Predicate<Entity>() {
		@Override
		public boolean test(Entity entity) {
			if (entity.equals(getPlayer())) return false;
			return !(entity instanceof Player) || (getGame().isParticipating(entity.getUniqueId())
					&& !((MurderMystery) getGame()).isDead(entity.getUniqueId()));
		}
	};
	
	@SubscribeEvent(priority = 1)
	private void onMurderKilled(MurderEvent e) {
		if (target != null) {
			if (LocationUtil.getNearbyEntities(Player.class, getPlayer().getLocation(), 5, 5, predicate).contains(target.getPlayer())) {
				if (e.getTarget().equals(getParticipant())) {
					if (e.getMurderer().getParticipant().equals(target)) {
						e.setCancelled(true);
						getPlayer().sendMessage("§4[§c!§4] §f경호 대상에게 공격받아서 경호가 해제됩니다!");
						Stun.apply(target, TimeUnit.TICKS, 60);
						target = null;
					}
					e.setCancelled(true);
					SoundLib.ENTITY_ZOMBIE_ATTACK_IRON_DOOR.playSound(getPlayer().getLocation(), (float) 0.5, 2);
				}	
			}	
		}
	}
	
	@SubscribeEvent(priority = 11)
	private void onMurder(MurderEvent e) {
		if (!e.isCancelled() && e.getTarget().equals(target)) {
			if (blocking.isRunning()) {
				e.setCancelled(true);
				SoundLib.ENTITY_ZOMBIE_ATTACK_IRON_DOOR.playSound(getPlayer().getLocation(), (float) 0.5, 2);
			} else {
				if (!blocked.contains(target.getPlayer().getUniqueId())) {
					e.setCancelled(true);
					SoundLib.ENTITY_ZOMBIE_ATTACK_IRON_DOOR.playSound(getPlayer().getLocation(), (float) 0.5, 2);
					getPlayer().teleport(target.getPlayer());
					blocked.add(target.getPlayer().getUniqueId());
					blocking.start();
					Stun.apply(e.getMurderer().getParticipant(), TimeUnit.TICKS, 30);
				} else target = null;
			}
		}
	}
	
	@SubscribeEvent(priority = 1)
	private void onArrowKilled(ArrowKillEvent e) {
		if (target != null) {
			if (LocationUtil.getNearbyEntities(Player.class, getPlayer().getLocation(), 5, 5, predicate).contains(target.getPlayer())) {
				if (e.getTarget().equals(getParticipant())) {
					if (e.getShooter().equals(target)) {
						getPlayer().sendMessage("§4[§c!§4] §f경호 대상에게 공격받아서 경호가 해제됩니다!");
						Stun.apply(target, TimeUnit.TICKS, 60);
						target = null;
					}
					e.setCancelled(true);
				}	
			}	
		}
	}
	
	@SubscribeEvent(priority = 11)
	private void onArrowKill(ArrowKillEvent e) {
		if (!e.isCancelled() && e.getTarget().equals(target)) {
			if (blocking.isRunning()) {
				e.setCancelled(true);
				SoundLib.ENTITY_ZOMBIE_ATTACK_IRON_DOOR.playSound(getPlayer().getLocation(), (float) 0.5, 2);
			} else {
				if (!blocked.contains(target.getPlayer().getUniqueId())) {
					e.setCancelled(true);
					SoundLib.ENTITY_ZOMBIE_ATTACK_IRON_DOOR.playSound(getPlayer().getLocation(), (float) 0.5, 2);
					getPlayer().teleport(target.getPlayer());
					blocked.add(target.getPlayer().getUniqueId());
					blocking.start();
					Stun.apply(e.getShooter(), TimeUnit.TICKS, 30);
				} else target = null;
			}
		}
	}
	
	@SubscribeEvent(onlyRelevant = true)
	private void onInteract(PlayerInteractEvent e) {
		if (Items.isGold(e.getItem())) {
			if (e.getAction() == Action.RIGHT_CLICK_AIR || e.getAction() == Action.RIGHT_CLICK_BLOCK) {
				final MurderMystery murderMystery = (MurderMystery) getGame();
				if (murderMystery.consumeGold(getParticipant(), 6)) {
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
					if (getGame().getParticipant(player).hasAbility()) {
						if (!getGame().getParticipant(player).getAbility().getClass().equals(BodyGuard.class)) {
							if (murderMystery.consumeGold(getParticipant(), 6)) {
								target = getGame().getParticipant(player);
								getPlayer().sendMessage("§4[§c!§4] §b" + target.getPlayer().getName() + "§f님은 이제 당신의 경호 대상입니다.");	
							}	
						} else getPlayer().sendMessage("§4[§c!§4] §c경호원을 경호할 순 없습니다.");	
					}
				} else getPlayer().sendMessage("§4[§c!§4] §c바라보는 방향에 사람이 없습니다.");	
			}
		}
	}
	
	public AbilityTimer passive = new AbilityTimer() {
		
		@Override
		public void run(int count) {
			if (target != null) {
				for (final Location locs : circle.toLocations(getPlayer().getLocation()).floor(getPlayer().getLocation().getY())) {
					if (LocationUtil.getNearbyEntities(Player.class, getPlayer().getLocation(), 5, 5, predicate).contains(target.getPlayer())) ParticleLib.REDSTONE.spawnParticle(getPlayer(), locs, green);
					else ParticleLib.REDSTONE.spawnParticle(getPlayer(), locs, red);
				}
			}
		}
		
	}.setPeriod(TimeUnit.TICKS, 2).register();
	
	private final AbilityTimer blocking = new AbilityTimer(20) {

	}.setPeriod(TimeUnit.TICKS, 1).register();

}
