package rainstar.abilitywar.game.murder.murderer;

import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.SubscribeEvent;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;

import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.list.murdermystery.Items;
import daybreak.abilitywar.game.list.murdermystery.MurderMystery;
import daybreak.abilitywar.game.list.murdermystery.MurderMystery.ArrowKillEvent;
import daybreak.abilitywar.game.list.murdermystery.ability.AbstractMurderer;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.base.color.RGB;
import daybreak.abilitywar.utils.base.concurrent.SimpleTimer.TaskType;
import daybreak.abilitywar.utils.base.math.LocationUtil;
import daybreak.abilitywar.utils.base.math.geometry.Circle;
import daybreak.abilitywar.utils.base.minecraft.nms.NMS;
import daybreak.abilitywar.utils.library.ParticleLib;
import daybreak.abilitywar.utils.library.SoundLib;

@AbilityManifest(name = "머더: 테러리스트", rank = Rank.SPECIAL, species = Species.HUMAN, explain = {
		"모든 시민을 죽이세요!",
		"금 우클릭으로 금 8개를 소모해 활과 화살을 얻을 수 있습니다.",
		"§8[§3매설§8]§f 금 좌클릭으로 금 3개를 소모해 현재 위치에 §c폭탄§f을",
		"설치할 수 있습니다. 폭탄은 한 번에 10개까지 설치 가능합니다.",
		"§8[§c테러§8]§f 금 F키로 설치한 §c폭탄§f을 일제히 터뜨려",
		"1.5초 후 폭탄으로부터 13칸 내 모든 대상을 5회 살해할 수 있습니다.",
		"해당 폭탄의 범위 내에 아무도 없다면 발동하지 않습니다.",
		"§8[§4자폭§8]§f 사망 직전 자신을 죽인 대상과 자신에게 폭탄을 설치합니다.",
		"이후, 설치해둔 모든 폭탄을 즉시 1회 터뜨리고 사망합니다."
})
public class TerroristMurderer extends AbstractMurderer {
	
	public TerroristMurderer(Participant participant) {
		super(participant);
	}

	private Set<Location> tnts = new HashSet<>();
	private Circle circle = Circle.of(13, 80);
	private static final RGB red = RGB.of(192, 1, 1);
	
	private final Predicate<Entity> predicate = new Predicate<Entity>() {
		@Override
		public boolean test(Entity entity) {
			if (entity.equals(getPlayer())) return false;
			return !(entity instanceof Player) || (getGame().isParticipating(entity.getUniqueId())
					&& !((MurderMystery) getGame()).isDead(entity.getUniqueId())
					&& getGame().getParticipant(entity.getUniqueId()).attributes().TARGETABLE.getValue())
					&& getGame().getParticipant(entity.getUniqueId()).hasAbility()
					&& !(getGame().getParticipant(entity.getUniqueId()).getAbility() instanceof AbstractMurderer);
		}
	};
	
	@Override
	protected void onUpdate(Update update) {
		super.onUpdate(update);
		if (update == Update.RESTRICTION_CLEAR) {
			NMS.sendTitle(getPlayer(), "§e직업§f: §5테러리스트", "§f모든 §a시민§f과 §5탐정§f을 폭발시키세요.", 10, 80, 10);
			new AbilityTimer(1) {
				@Override
				protected void onEnd() {
					NMS.clearTitle(getPlayer());
				}
			}.setInitialDelay(TimeUnit.SECONDS, 5).start();
			
			skill.start();
			PASSIVE.start();
		} else if (update == Update.ABILITY_DESTROY) {
			NMS.setInvisible(getPlayer(), false);
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
				if (tnts.size() < 10) {
					MurderMystery murderMystery = (MurderMystery) getGame();
					if (murderMystery.consumeGold(getParticipant(), 3)) {
						tnts.add(getPlayer().getLocation());
						getPlayer().sendMessage("§4[§c!§4] §c폭탄§f을 성공적으로 설치했습니다!");
					}	
				} else getPlayer().sendMessage("§4[§c!§4] §c이미 매설된 폭탄이 10개 이상입니다.");
			}
		}
	}
	
    @SubscribeEvent(onlyRelevant = true, priority = 0)
    public void onPlayerSwapHandItems(PlayerSwapHandItemsEvent e) {
    	if (Items.isGold(e.getOffHandItem()) && e.getPlayer().equals(getPlayer())) {
			if (tnts.size() > 0) {
				List<Location> locs = new ArrayList<>(tnts);
				boolean used = false;
				for (Location loc : locs) {
					if (LocationUtil.getNearbyEntities(Player.class, loc, 9.5, 9.5, predicate).size() > 0) {
						if (!used) used = true;
						new AbilityTimer(TaskType.REVERSE, 30) {
							
							@Override
							public void onStart() {
								SoundLib.ENTITY_ARROW_HIT_PLAYER.playSound(loc, 0.5f, 2f);
							}
							
							@Override
							public void run(int count) {
								if (count % 13 == 0) {
									SoundLib.ENTITY_ARROW_HIT_PLAYER.playSound(loc, 0.5f, 2f);
								}
							}

							@Override
							public void onEnd() {
								onSilentEnd();
							}
							
							@Override
							public void onSilentEnd() {
								ParticleLib.EXPLOSION_HUGE.spawnParticle(loc, 3, 0, 3, 5, 1);
								SoundLib.ENTITY_GENERIC_EXPLODE.playSound(loc, 1, 1);
								SoundLib.ENTITY_GENERIC_EXPLODE.playSound(loc.clone().add(random.nextInt(40) * 0.1, 0, random.nextInt(40) * 0.1), 1, (float) ((random.nextInt(16) * 0.1) + 0.5));
								SoundLib.ENTITY_GENERIC_EXPLODE.playSound(loc.clone().add(random.nextInt(40) * 0.1, 0, random.nextInt(40) * 0.1), 1, (float) ((random.nextInt(16) * 0.1) + 0.5));
								SoundLib.ENTITY_GENERIC_EXPLODE.playSound(loc.clone().add(random.nextInt(40) * 0.1, 0, random.nextInt(40) * 0.1), 1, (float) ((random.nextInt(16) * 0.1) + 0.5));				
								for (Player player : LocationUtil.getNearbyEntities(Player.class, loc, 13, 13, predicate)) {
									new AbilityTimer(5) {
										
										@Override
										public void run(int count) {
											MurderEvent event = new MurderEvent(TerroristMurderer.this, getGame().getParticipant(player));
											Bukkit.getPluginManager().callEvent(event);
											if (!event.isCancelled()) {
												player.setHealth(0);
											}
										}
										
									}.setPeriod(TimeUnit.TICKS, 5).start();
								}
								tnts.remove(loc);
							}
							
						}.setPeriod(TimeUnit.TICKS, 1).start();
					}
				}
				if (used) getPlayer().sendMessage("§4[§c!§4] §c테러§f가 사용되었습니다.");
				else getPlayer().sendMessage("§4[§c!§4] §c테러§f가 사용되지 않았습니다.");
			} else getPlayer().sendMessage("§4[§c!§4] §c아직 폭탄을 설치하지 않았습니다.");	
    	}
    }
    
	@SubscribeEvent(priority = 10)
	private void onArrowDeath(ArrowKillEvent e) {
		if (!(e.getShooter().getAbility() instanceof AbstractMurderer) && e.getTarget().equals(getParticipant()) && !e.isCancelled()) {
			if (e.getShooter() != null) tnts.add(e.getShooter().getPlayer().getLocation());
			tnts.add(getPlayer().getLocation());
			List<Location> locs = new ArrayList<>(tnts);
			for (Location loc : locs) {
				if (LocationUtil.getNearbyEntities(Player.class, loc, 9.5, 9.5, predicate).size() > 0) {
					ParticleLib.EXPLOSION_HUGE.spawnParticle(loc, 3, 0, 3, 5, 1);
					SoundLib.ENTITY_GENERIC_EXPLODE.playSound(loc, 1, 1);
					SoundLib.ENTITY_GENERIC_EXPLODE.playSound(loc.clone().add(random.nextInt(40) * 0.1, 0, random.nextInt(40) * 0.1), 1, (float) ((random.nextInt(16) * 0.1) + 0.5));
					SoundLib.ENTITY_GENERIC_EXPLODE.playSound(loc.clone().add(random.nextInt(40) * 0.1, 0, random.nextInt(40) * 0.1), 1, (float) ((random.nextInt(16) * 0.1) + 0.5));
					SoundLib.ENTITY_GENERIC_EXPLODE.playSound(loc.clone().add(random.nextInt(40) * 0.1, 0, random.nextInt(40) * 0.1), 1, (float) ((random.nextInt(16) * 0.1) + 0.5));
					for (Player player : LocationUtil.getNearbyEntities(Player.class, loc, 13, 13, predicate)) {
						MurderEvent event = new MurderEvent(TerroristMurderer.this, getGame().getParticipant(player));
						Bukkit.getPluginManager().callEvent(event);
						if (!event.isCancelled()) {
							player.setHealth(0);
						}
					}
					tnts.remove(loc);	
				}
			}
		}
	}
	
	private final AbilityTimer skill = new AbilityTimer() {
		
		@Override
		public void run(int count) {
			if (tnts.size() > 0) {
				for (Location loc : tnts) {
					for (final Location locs : circle.toLocations(loc).floor(loc.getY())) {
						ParticleLib.REDSTONE.spawnParticle(getPlayer(), locs, red);
					}
					for (Participant participant : getGame().getParticipants()) {
						if (participant.getAbility() instanceof AbstractMurderer) {
							ParticleLib.REDSTONE.spawnParticle(participant.getPlayer(), loc, red);		
						}
					}
				}
			}
		}
		
	}.setPeriod(TimeUnit.TICKS, 10).register();
	
}
