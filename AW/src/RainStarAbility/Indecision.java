package RainStarAbility;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Note;
import org.bukkit.Note.Tone;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent.RegainReason;

import daybreak.abilitywar.ability.AbilityBase;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.SubscribeEvent;
import daybreak.abilitywar.ability.Tips;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.ability.Tips.Description;
import daybreak.abilitywar.ability.Tips.Difficulty;
import daybreak.abilitywar.ability.Tips.Level;
import daybreak.abilitywar.ability.Tips.Stats;
import daybreak.abilitywar.ability.decorator.ActiveHandler;
import daybreak.abilitywar.config.ability.AbilitySettings.SettingObject;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.AbstractGame.Participant.ActionbarNotification.ActionbarChannel;
import daybreak.abilitywar.utils.base.Formatter;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.base.math.geometry.Circle;
import daybreak.abilitywar.utils.library.ParticleLib;
import daybreak.abilitywar.utils.library.SoundLib;

@AbilityManifest(name = "우유부단", rank = Rank.A, species = Species.HUMAN, explain = {
		"원 / 근거리 공격 중 하나는 §c딜§f, 하나는 §d힐§f을 합니다.",
		"§c딜§f의 경우에는 §c2.5의 추가 대미지§f를, §d힐§f의 경우에는 §d1칸 회복§f해줍니다.",
		"이 효과는 나를 통해서도 발동합니다. 또한 생명체를 $[CountConfig]번 타격할 때마다",
		"각각의 공격 타입이 뒤바뀝니다.",
		"철괴 우클릭 시, 누적된 타격 횟수를 초기화합니다. $[CooldownConfig]"})

@Tips(tip = {
        "근거리 / 원거리 공격을 완벽하게 수행해낼 수 있는 완성형 딜러입니다.",
        "다만 요구하는 공격으로 공격하지 않는다면, 완성형 힐러가 되겠죠?"
}, strong = {
        @Description(subject = "실력", explain = {
        		"이 능력은 당신의 실력에 따라 추가대미지 2.5를 리스크 없이",
        		"계속해서 가할 수 있습니다!"
        })
}, weak = {
        @Description(subject = "실력", explain = {
                "이 능력은 당신의 실력에 따라 적에게 매번 체력 1칸을",
                "계속해서 회복해 줄 수 있습니다!"
        })
}, stats = @Stats(offense = Level.SIX, survival = Level.ZERO, crowdControl = Level.ZERO, mobility = Level.ZERO, utility = Level.ZERO), difficulty = Difficulty.VERY_HARD)

public class Indecision extends AbilityBase implements ActiveHandler {
	
	private boolean sword = true;

	private static final Circle circle = Circle.of(1, 10);
	
	private int stack = 0;

	public Indecision(Participant participant) {
		super(participant);
	}

	private final ActionbarChannel ac = newActionbarChannel();
	private final ActionbarChannel stackac = newActionbarChannel();
	private final Cooldown reset = new Cooldown(CooldownConfig.getValue());
	private final int count = CountConfig.getValue();
	
	public String getState() {
		if (sword)
			return "§b원거리 §7: §dHeal §8| §a근거리 §7: §cDeal";
		else
			return "§b원거리 §7: §cDeal §8| §a근거리 §7: §dHeal";
	}

	@Override
	protected void onUpdate(Update update) {
		if (update == Update.RESTRICTION_CLEAR) {
			ac.update(getState());
			stackac.update("§7타격 횟수 : §f" + stack + "§7 회");
		}
	}
	
	public static final SettingObject<Integer> CooldownConfig = abilitySettings.new SettingObject<Integer>(Indecision.class,
			"Cooldown", 60, "# 쿨타임") {
		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}

		@Override
		public String toString() {
			return Formatter.formatCooldown(getValue());
		}
	};	
	
	public static final SettingObject<Integer> CountConfig = abilitySettings.new SettingObject<Integer>(Indecision.class,
			"Count", 3, "# 타격 요구 횟수") {
		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}
	};	
	
	public boolean ActiveSkill(Material material, AbilityBase.ClickType clicktype) {
	    if (material.equals(Material.IRON_INGOT) && clicktype.equals(AbilityBase.ClickType.RIGHT_CLICK)
	    		&& !reset.isCooldown()) {
	    	stack = 0;
			stackac.update("§7타격 횟수 : §f" + stack + "§7 회");
			reset.start();
	    	return true;
	    }
	    return false;
	}   		
	
	@SubscribeEvent
	public void onEntityDamageByEntity(EntityDamageByEntityEvent e) {

		if (e.getDamager() instanceof Arrow) {
			Arrow arrow = (Arrow) e.getDamager();
			if (arrow.getShooter().equals(getPlayer()) && e.getEntity() instanceof LivingEntity) {
				final LivingEntity target = (LivingEntity) e.getEntity();

				if (sword == false) {
					e.setDamage(e.getDamage() + 2.5);

					new AbilityTimer(5) {
						@Override
						protected void run(int count) {
							Location center = target.getLocation().clone().add(0, 1 - count * 0.2, 0);
							for (Location loc : circle.toLocations(center).floor(center.getY())) {
								ParticleLib.DAMAGE_INDICATOR.spawnParticle(loc, 0, 0, 0, 1, 0);
							}
						}
					}.setPeriod(TimeUnit.TICKS, 1).start();
					SoundLib.GUITAR.playInstrument(getPlayer(), new Note(1, Tone.A, false));
					SoundLib.GUITAR.playInstrument(getPlayer(), new Note(1, Tone.A, false));
					SoundLib.GUITAR.playInstrument(getPlayer(), new Note(1, Tone.A, false));
				} else {
   					final EntityRegainHealthEvent event = new EntityRegainHealthEvent(getPlayer(), 2, RegainReason.CUSTOM);
					Bukkit.getPluginManager().callEvent(event);
					if (!event.isCancelled()) {
						target.setHealth(Math.min(target.getHealth() + 2, target.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue()));
					}
					e.setCancelled(true);
					(arrow).remove();

					new AbilityTimer(5) {
						@Override
						protected void run(int count) {
							Location center = target.getLocation().clone().add(0, 1 - count * 0.2, 0);
							for (Location loc : circle.toLocations(center).floor(center.getY())) {
								ParticleLib.HEART.spawnParticle(loc);
							}
						}
					}.setPeriod(TimeUnit.TICKS, 1).start();
					SoundLib.CHIME.playInstrument(getPlayer(), new Note(1, Tone.G, false));
					SoundLib.CHIME.playInstrument(getPlayer(), new Note(1, Tone.G, false));
					SoundLib.CHIME.playInstrument(getPlayer(), new Note(1, Tone.G, false));
				}
				stack++;
				stackac.update("§7타격 횟수 : §f" + stack + "§7 회");
				if (stack >= count) {
					sword = !sword;
					ac.update(getState());
					stack = 0;
					stackac.update("§7타격 횟수 : §f" + stack + "§7 회");
				}
			}
		}

		if (e.getDamager().equals(getPlayer()) && e.getEntity() instanceof LivingEntity) {
			final LivingEntity target = (LivingEntity) e.getEntity();

			if (sword == true) {
				e.setDamage(e.getDamage() + 2.5);

				new AbilityTimer(5) {
					@Override
					protected void run(int count) {
						Location center = target.getLocation().clone().add(0, 1 - count * 0.2, 0);
						for (Location loc : circle.toLocations(center).floor(center.getY())) {
							ParticleLib.DAMAGE_INDICATOR.spawnParticle(loc, 0, 0, 0, 1, 0);
						}
					}
				}.setPeriod(TimeUnit.TICKS, 1).start();
				SoundLib.GUITAR.playInstrument(getPlayer(), new Note(1, Tone.A, false));
				SoundLib.GUITAR.playInstrument(getPlayer(), new Note(1, Tone.A, false));
				SoundLib.GUITAR.playInstrument(getPlayer(), new Note(1, Tone.A, false));
			} else {
				final EntityRegainHealthEvent event = new EntityRegainHealthEvent(getPlayer(), 2, RegainReason.CUSTOM);
				Bukkit.getPluginManager().callEvent(event);
				if (!event.isCancelled()) {
					target.setHealth(Math.min(target.getHealth() + 2, target.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue()));
				}
				
				e.setCancelled(true);

				new AbilityTimer(5) {
					@Override
					protected void run(int count) {
						Location center = target.getLocation().clone().add(0, 1 - count * 0.2, 0);
						for (Location loc : circle.toLocations(center).floor(center.getY())) {
							ParticleLib.HEART.spawnParticle(loc);
						}
					}
				}.setPeriod(TimeUnit.TICKS, 1).start();
				SoundLib.CHIME.playInstrument(getPlayer(), new Note(1, Tone.G, false));
				SoundLib.CHIME.playInstrument(getPlayer(), new Note(1, Tone.G, false));
				SoundLib.CHIME.playInstrument(getPlayer(), new Note(1, Tone.G, false));
			}
			stack++;
			stackac.update("§7타격 횟수 : §f" + stack + "§7 회");
			if (stack >= count) {
				sword = !sword;
				ac.update(getState());
				stack = 0;
				stackac.update("§7타격 횟수 : §f" + stack + "§7 회");
			}
		}
	}
}