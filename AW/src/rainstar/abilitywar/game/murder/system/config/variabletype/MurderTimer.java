package rainstar.abilitywar.game.murder.system.config.variabletype;

import java.util.function.Predicate;

import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffectType;

import daybreak.abilitywar.ability.AbilityBase;
import daybreak.abilitywar.game.AbstractGame;
import daybreak.abilitywar.game.AbstractGame.GameTimer;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.list.murdermystery.MurderMystery;
import daybreak.abilitywar.game.list.murdermystery.ability.AbstractMurderer;
import daybreak.abilitywar.game.list.murdermystery.ability.jobs.murderer.BlackMurderer;
import daybreak.abilitywar.game.module.Module;
import daybreak.abilitywar.game.module.ModuleBase;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.base.math.LocationUtil;
import daybreak.abilitywar.utils.base.minecraft.BroadBar;
import daybreak.abilitywar.utils.library.MaterialX;
import daybreak.google.common.collect.ImmutableSet;
import rainstar.abilitywar.game.murder.innocent.Astrologer;
import rainstar.abilitywar.game.murder.innocent.Bartender;
import rainstar.abilitywar.game.murder.innocent.Watcher;
import rainstar.abilitywar.game.murder.murderer.HitmanMurderer;
import rainstar.abilitywar.game.murder.murderer.PhantomThiefMurderer;
import rainstar.abilitywar.game.murder.system.detectivebuffer.DetectiveIntuition;
import rainstar.abilitywar.game.murder.system.detectivebuffer.DetectivePsychometry;
import daybreak.abilitywar.utils.base.TimeUtil.ParsedTime;

@ModuleBase(MurderTimer.class)
public class MurderTimer extends GameTimer implements Module {

	private final int duration;
	private final AbstractGame abstractGame;
	private BroadBar bossbar;
	
	private static final ImmutableSet<Class<? extends AbilityBase>> noCompass = ImmutableSet.<Class<? extends AbilityBase>>builder()
			.add(Watcher.class)
			.add(HitmanMurderer.class)
			.add(Astrologer.class)
			.add(Bartender.class)
			.add(PhantomThiefMurderer.class)
			.add(DetectivePsychometry.class)
			.add(DetectiveIntuition.class)
			.build();
	
	public MurderTimer(AbstractGame abstractGame, int duration) {
		abstractGame.super(TaskType.REVERSE, duration * 20);
		setPeriod(TimeUnit.TICKS, 1);
		this.abstractGame = abstractGame;
		this.duration = duration;
		final ParsedTime parsedTime = ParsedTime.parse(duration);
		bossbar = new BroadBar(String.format("§c§l남은 시간§f: §c%d§f분 §c%d§f초 남음", parsedTime.minutes, parsedTime.seconds), BarColor.BLUE, BarStyle.SOLID);
		bossbar.setProgress(0);
	}
	
	public void addcount(int count) {
		this.setCount(this.getCount() + (count * 20));
	}
	
	public void setcount(int count) {
		this.setCount((count * 20));
	}
	
	@Override
	public void onStart() {
		for (Participant participant : abstractGame.getParticipants()) {
			participant.getPlayer().getInventory().setHeldItemSlot(0);
			participant.getPlayer().getInventory().setItem(7, MaterialX.COMPASS.createItem(1));
		}
	}
	
	@Override
	public void run(int count) {
		boolean hasBlack = false;
		for (Participant participant : abstractGame.getParticipants()) {
			if (participant.hasAbility() && participant.getAbility().getClass().equals(BlackMurderer.class)) {
				hasBlack = true;
				break;
			}
		}
		
		for (Participant participant : abstractGame.getParticipants()) {
			if (participant.hasAbility()) {
				if (!noCompass.contains(participant.getAbility().getClass())) {
					if (participant.getAbility() instanceof AbstractMurderer) {
						Predicate<Entity> nomurderpredicate = new Predicate<Entity>() {
							@Override
							public boolean test(Entity entity) {
								if (entity.equals(participant.getPlayer())) return false;
								return !(entity instanceof Player) || (getGame().isParticipating(entity.getUniqueId())
										&& !((MurderMystery) getGame()).isDead(entity.getUniqueId())
										&& getGame().getParticipant(entity.getUniqueId()).attributes().TARGETABLE.getValue())
										&& getGame().getParticipant(entity.getUniqueId()).hasAbility()
										&& !(getGame().getParticipant(entity.getUniqueId()).getAbility() instanceof AbstractMurderer);
							}
						};
						if (LocationUtil.getNearestEntity(Player.class, participant.getPlayer().getLocation(), nomurderpredicate) != null) {
							participant.getPlayer().setCompassTarget(LocationUtil.getNearestEntity(Player.class, participant.getPlayer().getLocation(), nomurderpredicate).getLocation());	
						}	
					} else {
						Predicate<Entity> predicate = new Predicate<Entity>() {
							@Override
							public boolean test(Entity entity) {
								if (entity.equals(participant.getPlayer())) return false;
								return !(entity instanceof Player) || (getGame().isParticipating(entity.getUniqueId())
										&& !((MurderMystery) getGame()).isDead(entity.getUniqueId())
										&& getGame().getParticipant(entity.getUniqueId()).attributes().TARGETABLE.getValue());
							}
						};
						if (!(hasBlack && participant.getPlayer().hasPotionEffect(PotionEffectType.BLINDNESS))) {
							if (LocationUtil.getNearestEntity(Player.class, participant.getPlayer().getLocation(), predicate) != null) {
								participant.getPlayer().setCompassTarget(LocationUtil.getNearestEntity(Player.class, participant.getPlayer().getLocation(), predicate).getLocation());	
							}		
						}
					}	
				}	
			}
		}
		
		if (count % 100 == 0) {
			for (Participant participant : abstractGame.getParticipants()) {
				participant.getPlayer().getInventory().setItem(7, MaterialX.COMPASS.createItem(1));
			}
		}
		
		final ParsedTime parsedTime = ParsedTime.parse(count / 20);
		bossbar.setTitle(String.format("§c남은 시간§f: §c%d§f분 §c%d§f초 남음", parsedTime.minutes, parsedTime.seconds));
		bossbar.setProgress(Math.min(1, (double) count / (duration * 20)));
	}
	
	@Override
	public void onEnd() {
		for (Participant participant : abstractGame.getParticipants()) {
			if (participant.getAbility() instanceof AbstractMurderer) {
				participant.getPlayer().setHealth(0);
			}
		}
		unregister();
	}
	
	@Override
	public void register() {
		start();
	}

	@Override
	public void unregister() {
		bossbar.unregister();
	}
	
}
