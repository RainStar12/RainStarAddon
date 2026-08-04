package rainstar.abilitywar.ability;

import static rainstar.abilitywar.system.config.RainStarAbilitySetting.rsAbilitySettings;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.annotation.Nullable;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import daybreak.abilitywar.ability.AbilityBase;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.ability.SubscribeEvent;
import daybreak.abilitywar.ability.decorator.ActiveHandler;
import daybreak.abilitywar.config.ability.AbilitySettings.SettingObject;
import daybreak.abilitywar.game.AbstractGame.Effect;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.AbstractGame.Participant.ActionbarNotification.ActionbarChannel;
import daybreak.abilitywar.game.manager.effect.Frost;
import daybreak.abilitywar.game.manager.effect.registry.EffectRegistry;
import daybreak.abilitywar.game.manager.effect.registry.EffectRegistry.EffectRegistration;
import daybreak.abilitywar.game.manager.effect.registry.EffectType;
import daybreak.abilitywar.utils.base.Formatter;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.base.random.Random;
import daybreak.abilitywar.utils.library.SoundLib;
import daybreak.google.common.base.Predicate;
import daybreak.google.common.collect.ImmutableSet;
import rainstar.abilitywar.effect.Blackout;
import rainstar.abilitywar.effect.Slippery;
import rainstar.abilitywar.effect.Vertigo;

@AbilityManifest(name = "취권", rank = Rank.S, species = Species.HUMAN, explain = {
		"§7패시브 §8- §2취기§f: 이동할 때마다 확률적으로 §b§n미끌거림§f, §6§n현기증§f, §8§n암전§f 중",
		" 무작위의 §5§n상태이상§f 하나를 $[EFFECT_DURATION]초간 부여받습니다.",
		" 위 세 §5§n상태이상§f 하나당 공격을 §b+$[EVADE]%§f 확률로 §b회피§f하고,",
		" 가진 §3이동/시야계 디버프§f 하나당 §c공격력§f이 §c$[DAMAGE_INCREASE]%§f 증가합니다.",
		"§7패시브 §8- §b§o안 취했는데요§f: 이동이 취소되지 않습니다.",
		"§7철괴 우클릭 §8- §c부어라 마셔라§f: 즉시 무작위 §3§n이동/시야계 상태이상§f을 3개 받고,",
		" 소지한 모든 §3§n이동/시야계 상태이상§f을 $[TIMER_INCREASE]초 늘립니다. $[COOLDOWN]",
		"§3[§b미끌거림§3] §f때때로 이동량이나 밀/끌어당기는 힘이 증가됩니다.",
		"§2[§6현기증§2] §f멀미에 걸립니다. 공격 시 시야가 아주 약하게 튑니다.",
		"§5[§8암전§5] §f적을 공격하면 1초간 실명 상태에 빠집니다.",
		"§b[§7아이디어 제공자§b] §5_Millsage_"
		})
public class DrunkenFist extends AbilityBase implements ActiveHandler {

	public DrunkenFist(Participant participant) {
		super(participant);
	}
	
	public static final SettingObject<Integer> COOLDOWN = rsAbilitySettings.new SettingObject<Integer>(DrunkenFist.class,
			"cooldown", 110, "# 쿨타임") {
		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}
		
        @Override
        public String toString() {
            return Formatter.formatCooldown(getValue());
        }
	};
	
	public static final SettingObject<Integer> EVADE = rsAbilitySettings.new SettingObject<Integer>(DrunkenFist.class,
			"evade", 20, "# 소지한 미끌거림 / 현기증 / 암전 하나당 회피 확률", "# 단위: %") {
		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}
	};
	
	public static final SettingObject<Integer> DAMAGE_INCREASE = rsAbilitySettings.new SettingObject<Integer>(DrunkenFist.class,
			"damage-increase", 10, "# 소지한 이동/시야계 디버프 하나당 공격력 증가치", "# 단위: %") {
		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}
	};
	
	public static final SettingObject<Double> EFFECT_DURATION = rsAbilitySettings.new SettingObject<Double>(DrunkenFist.class,
			"effect-duration", 10.0, "# 3종 상태이상 기본 지속 시간", "# 단위: 초") {
		@Override
		public boolean condition(Double value) {
			return value >= 0;
		}
	};
	
	public static final SettingObject<Double> TIMER_INCREASE = rsAbilitySettings.new SettingObject<Double>(DrunkenFist.class,
			"timer-increase", 15.0, "# 상태이상 지속 시간 증가치", "# 단위: 초") {
		@Override
		public boolean condition(Double value) {
			return value >= 0;
		}
	};
	
	private final Cooldown cooldown = new Cooldown(COOLDOWN.getValue());
	private final int evade = EVADE.getValue();
	private final double damageIncrease = DAMAGE_INCREASE.getValue() * 0.01;
	private final int effectDuration = (int) (EFFECT_DURATION.getValue() * 20);
	private final int timerIncrease = (int) (TIMER_INCREASE.getValue() * 20);
	
	private ActionbarChannel ac = newActionbarChannel();
	private DecimalFormat df = new DecimalFormat("0.0");
	private Random random = new Random();
	private Location originalTo;
	
	private final Predicate<Effect> effectpredicate = new Predicate<Effect>() {
		@Override
		public boolean test(Effect effect) {
			final ImmutableSet<EffectType> effectType = effect.getRegistration().getEffectType();
			return effectType.contains(EffectType.MOVEMENT_RESTRICTION) || effectType.contains(EffectType.MOVEMENT_INTERRUPT) || effectType.contains(EffectType.SIGHT_CONTROL) || effectType.contains(EffectType.SIGHT_RESTRICTION);
		}

		@Override
		public boolean apply(@Nullable Effect arg0) {
			return false;
		}
	};
	
	private final Predicate<EffectRegistration<?>> registrationPredicate = new Predicate<EffectRegistration<?>>() {
	    @Override
	    public boolean test(EffectRegistration<?> registration) {
	        final ImmutableSet<EffectType> types = registration.getEffectType();
	        return types.contains(EffectType.MOVEMENT_RESTRICTION)
	            || types.contains(EffectType.MOVEMENT_INTERRUPT)
	            || types.contains(EffectType.SIGHT_CONTROL)
	            || types.contains(EffectType.SIGHT_RESTRICTION);
	    }

	    @Override
	    public boolean apply(@Nullable EffectRegistration<?> input) {
	        return test(input);
	    }
	};
	
	private static final List<PotionEffectType> POTION_EFFECTS = Arrays.asList(
		    PotionEffectType.BLINDNESS,
		    PotionEffectType.CONFUSION,
		    PotionEffectType.HUNGER,
		    PotionEffectType.SLOW,
		    PotionEffectType.LEVITATION
		);
	
	@Override
	public void onUpdate(Update update) {
		if (update == Update.RESTRICTION_CLEAR) passive.start();
	}
	
	private AbilityTimer passive = new AbilityTimer() {
		
		@Override
		public void run(int count) {
			int stack = 0;
			for (Effect effect : getParticipant().getEffects()) {
				if (effectpredicate.test(effect)) stack++;
			}
			for (PotionEffect pe : getPlayer().getActivePotionEffects()) {
				if (POTION_EFFECTS.contains(pe.getType())) stack++;
			}
			
			ac.update("§c공격력§7: §c+" + df.format(stack * damageIncrease * 100) + "%");
		}
		
	}.setPeriod(TimeUnit.TICKS, 1).register();
	
	@SubscribeEvent(priority = -999, eventPriority = EventPriority.LOWEST)
	private void onPlayerMoveTry(PlayerMoveEvent e) {
		if (e.getPlayer().equals(getPlayer())) {
			originalTo = e.getTo().clone();
		}
	}
	
	@SubscribeEvent(priority = 999, eventPriority = EventPriority.MONITOR)
	private void onPlayerMove(PlayerMoveEvent e) {
		if (e.getPlayer().equals(getPlayer())) {
			e.setCancelled(false);
			if (e.getTo().distanceSquared(e.getFrom()) < 1 && originalTo != null) {
				e.setTo(originalTo);
				originalTo = null;
			}
			if (random.nextInt(333) == 0) {
				switch(random.nextInt(3)) {
				case 0:
					Slippery.apply(getParticipant(), TimeUnit.TICKS, effectDuration);
					break;
				case 1:
					Vertigo.apply(getParticipant(), TimeUnit.TICKS, effectDuration);
					break;
				case 2:
					Blackout.apply(getParticipant(), TimeUnit.TICKS, effectDuration);
					break;
				}
				getPlayer().sendMessage("§4[§c!§4] §2취기§c가 한층 더 강해집니다...");
				SoundLib.ENTITY_WITCH_DRINK.playSound(getPlayer());
			}
		}
	}
	
	@SubscribeEvent
	private void onTeleport(PlayerTeleportEvent e) {
		if (e.getPlayer().equals(getPlayer()) && getParticipant().hasEffect(Frost.registration)) {
			e.setCancelled(true);
		}
	}
	
	@SubscribeEvent
	private void onEntityDamageByEntity(EntityDamageByEntityEvent e) {
		Player damager = null;
		if (e.getDamager() instanceof Projectile) {
			Projectile projectile = (Projectile) e.getDamager();
			if (projectile.getShooter() != null && projectile.getShooter().equals(getPlayer())) {
				damager = getPlayer();
			}
		} else if (getPlayer().equals(e.getDamager())) damager = getPlayer();
		
		if (getPlayer().equals(damager) && !e.getEntity().equals(getPlayer())) {
			int stack = 0;
			for (Effect effect : getParticipant().getEffects()) {
				if (effectpredicate.test(effect)) stack++;
			}
			for (PotionEffect pe : getPlayer().getActivePotionEffects()) {
				if (POTION_EFFECTS.contains(pe.getType())) stack++;
			}
			
			e.setDamage(e.getDamage() * (1 + (stack * damageIncrease)));
		}
		
		if (e.getEntity().equals(getPlayer())) {
			int stack = 0;
			if (getParticipant().hasEffect(Slippery.registration)) stack++;
			if (getParticipant().hasEffect(Vertigo.registration)) stack++;
			if (getParticipant().hasEffect(Blackout.registration)) stack++;
			if (random.nextInt(100) < (stack * evade)) {
				e.setCancelled(true);
				SoundLib.ENTITY_PLAYER_ATTACK_SWEEP.playSound(getPlayer().getLocation(), 1, 1.7f);
			}
		}
	}
	
	public boolean ActiveSkill(Material material, ClickType clicktype) {
		if (material == Material.IRON_INGOT && clicktype == ClickType.RIGHT_CLICK && !cooldown.isCooldown()) {
			List<EffectRegistration<?>> candidates = new ArrayList<>();

			for (EffectRegistration<?> registration : EffectRegistry.values()) {
			    if (registrationPredicate.test(registration)) {
			        candidates.add(registration);
			    }
			}

			Collections.shuffle(candidates, random);

			List<EffectRegistration<?>> selected = candidates.subList(0, Math.min(3, candidates.size()));
			
			for (EffectRegistration<?> select : selected) {
				select.apply(getParticipant(), TimeUnit.TICKS, 10);	
			}
			
			for (Effect effect : getParticipant().getEffects()) {
				effect.setCount(effect.getCount() + (timerIncrease / effect.getPeriod()));
			}
			return cooldown.start();
		}
		return false;
	}
	
}
