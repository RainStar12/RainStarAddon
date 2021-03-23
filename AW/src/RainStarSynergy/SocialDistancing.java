package RainStarSynergy;

import org.bukkit.Location;
import org.bukkit.Note;
import org.bukkit.Note.Tone;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.ability.SubscribeEvent;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.AbstractGame.Participant.ActionbarNotification.ActionbarChannel;
import daybreak.abilitywar.game.list.mix.synergy.Synergy;
import daybreak.abilitywar.utils.base.color.RGB;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.base.math.geometry.Circle;
import daybreak.abilitywar.utils.base.minecraft.nms.NMS;
import daybreak.abilitywar.utils.library.ParticleLib;
import daybreak.abilitywar.utils.library.SoundLib;

@AbilityManifest(name = "사회적 거리 조절", rank = Rank.S, species = Species.HUMAN, explain = {
		"§7패시브 §8- §a에너지 블로커§f: 최근에 내가 받은 피해의 근·원거리에 따라",
		" 다음 피해 방어상태를 동일하게 조절합니다. 만약 방어상태인 피해 타입으로",
		" 피격당했을 경우 피해량을 2/3으로 줄여 받습니다.",
		"§7패시브 §8- §b우유부단§f: 최근에 내가 받은 피해의 근·원거리에 따라",
		" 내 공격 상태를 동일하게 조절합니다. 만약 공격상태인 피해 타입으로",
		" 공격했을 경우 1.5의 추가 피해를 줍니다."
})

public class SocialDistancing extends Synergy {
	
	public SocialDistancing(Participant participant) {
		super(participant);
	}
	
	private boolean isshort = false;
	private final ActionbarChannel actionbarChannel = newActionbarChannel();
	private static final RGB LONG_DISTANCE = RGB.of(82, 108, 179), SHORT_DISTANCE = RGB.of(130, 255, 147);
	private static final Circle circle = Circle.of(1, 10);
	
	@Override
	protected void onUpdate(Update update) {
		if (update == Update.RESTRICTION_CLEAR) {
			actionbarChannel.update(getState());
			particle.start();
		}
	}
	
	public String getState() {
		return isshort ? "§a근거리 보호 및 공격" : "§b원거리 보호 및 공격§f";
	}
	
	private final AbilityTimer particle = new AbilityTimer() {

		@Override
		public void run(int count) {
			if (isshort == false) {
				ParticleLib.REDSTONE.spawnParticle(getPlayer().getLocation().add(0, 2.2, 0), LONG_DISTANCE);
			} else {
				ParticleLib.REDSTONE.spawnParticle(getPlayer().getLocation().add(0, 2.2, 0), SHORT_DISTANCE);
			}
		}

	}.setPeriod(TimeUnit.TICKS, 1).register();

	@SubscribeEvent
	private void onEntityDamageByEntity(EntityDamageByEntityEvent e) {
		if (e.getEntity().equals(getPlayer()) && !e.getDamager().equals(getPlayer())) {
			if (e.getCause() == DamageCause.PROJECTILE) {
				if (isshort == false) {
					e.setDamage(e.getDamage() * ((double) 2 / 3));
					SoundLib.ITEM_SHIELD_BLOCK.playSound(getPlayer());
				} else {
					isshort = false;
					actionbarChannel.update(getState());
				}
			} else if (e.getCause() == DamageCause.ENTITY_ATTACK) {
				if (isshort == true) {
					e.setDamage(e.getDamage() * ((double) 2 / 3));
					SoundLib.ITEM_SHIELD_BLOCK.playSound(getPlayer());
				} else {
					isshort = true;
					actionbarChannel.update(getState());
				}
			}
		}
		
		if (e.getDamager().equals(getPlayer()) && e.getEntity() instanceof Entity && isshort == true) {
			SoundLib.GUITAR.playInstrument(getPlayer(), new Note(1, Tone.A, false));
			SoundLib.GUITAR.playInstrument(getPlayer(), new Note(1, Tone.A, false));
			SoundLib.GUITAR.playInstrument(getPlayer(), new Note(1, Tone.A, false));
			e.setDamage(e.getDamage() + 1.5);
			new AbilityTimer(5) {
				@Override
				protected void run(int count) {
					Location center = e.getEntity().getLocation().clone().add(0, 1 - count * 0.2, 0);
					for (Location loc : circle.toLocations(center).floor(center.getY())) {
						ParticleLib.DAMAGE_INDICATOR.spawnParticle(loc, 0, 0, 0, 1, 0);
					}
				}
			}.setPeriod(TimeUnit.TICKS, 1).start();
		}
		
		if (NMS.isArrow(e.getDamager()) && isshort == false) {
			Arrow arrow = (Arrow) e.getDamager();
			if (arrow.getShooter().equals(getPlayer()) && e.getEntity() instanceof LivingEntity
					&& !e.getEntity().equals(getPlayer())) {
				SoundLib.GUITAR.playInstrument(getPlayer(), new Note(1, Tone.A, false));
				SoundLib.GUITAR.playInstrument(getPlayer(), new Note(1, Tone.A, false));
				SoundLib.GUITAR.playInstrument(getPlayer(), new Note(1, Tone.A, false));
				e.setDamage(e.getDamage() + 1.5);
				new AbilityTimer(5) {
					@Override
					protected void run(int count) {
						Location center = e.getEntity().getLocation().clone().add(0, 1 - count * 0.2, 0);
						for (Location loc : circle.toLocations(center).floor(center.getY())) {
							ParticleLib.DAMAGE_INDICATOR.spawnParticle(loc, 0, 0, 0, 1, 0);
						}
					}
				}.setPeriod(TimeUnit.TICKS, 1).start();
			}
		}
	}
}
