package RainStarAbility;

import java.util.Arrays;
import java.util.List;

import org.bukkit.Note;
import org.bukkit.Note.Tone;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

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
import daybreak.abilitywar.config.ability.AbilitySettings.SettingObject;
import daybreak.abilitywar.config.enums.CooldownDecrease;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.AbstractGame.Participant.ActionbarNotification.ActionbarChannel;
import daybreak.abilitywar.utils.base.Formatter;
import daybreak.abilitywar.utils.base.color.RGB;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.base.math.geometry.Line;
import daybreak.abilitywar.utils.library.MaterialX;
import daybreak.abilitywar.utils.library.ParticleLib;
import daybreak.abilitywar.utils.library.SoundLib;

@AbilityManifest(name = "메아리", rank = Rank.S, species = Species.OTHERS, explain = {
		"§7패시브 §8- §c카운터§f: 다른 플레이어에게 근접 피해를 받을 때",
		" 대상에게 0.75초 내로 근접 피해를 입힐 시 피해량만큼 회복하고",
		" 대상이 준 피해량을 되돌려줍니다. $[COOLDOWN]",
		" 이때 반격까지 걸린 시간에 반비례해 반격 피해가 1.5배에서 0.1배까지 변동됩니다.",
		"§7쿨타임 패시브 §8- §c리플렉스§f: 쿨타임 도중 카운터 조건을 충족할 시,",
		" 카운터 효과가 발동되지 않고 쿨타임이 $[DECREASE]초씩 줄어듭니다."
		},
		summarize = {
		"적에게 피해를 입고 재빨리 근접 공격을 가하면 해당 피해를 반격합니다.",
		"반격된 피해는 반격에 걸린 시간에 반비례해 증감합니다. $[COOLDOWN]",
		"쿨타임 중에는 반격 성공 시 효과 발동 대신 쿨타임이 줄어듭니다."
		})

@Tips(tip = {
        "들이닥치는 근접 피해를 되돌려주는 반격 근접 능력입니다.",
        "내 기본 피해에 더해 대상이 준 피해량을 입혀 대상에게 치명적인",
        "피해를 입히고, 나 자신은 대상이 준 피해만큼 다시 회복합니다.",
        "하지만 근거리 피해로만 발동되기에, 원거리 피해나 마법 피해를",
        "주의해야 합니다."
}, strong = {
        @Description(subject = "한 방의 대미지가 높은 대상", explain = {
                "단 한 방만 높은 대미지인 버서커 등의 능력이라면",
                "이 능력으로 간단히 반격해 대상에게 역공을 취할 수",
                "있습니다."
        }),
        @Description(subject = "근접형 능력", explain = {
                "근접 전투를 요하는 이 능력에게는 대상이 들고 있던",
                "내가 들고 있던 근접형 능력이 좋습니다."
        }),
        @Description(subject = "가시 인챈트", explain = {
                "가시는 근접 대미지 판정의 반격 인챈트입니다.",
                "가시 인챈트와 조합한다면, 반격 대미지를 입히려 하기도",
                "전에 즉시 반격되겠죠?"
        }),
        @Description(subject = "좁은 공간", explain = {
                "재빠르게 반격해야 하는 이 능력에겐 대상의 공격에게",
                "넉백되어도 멀리 날아가지 않는 좁은 공간이 유리합니다."
        })
}, weak = {
        @Description(subject = "근접 피해 외 다른 타입의 피해", explain = {
                "원거리 전투, 날아오는 포션 등. 메아리는 아무것도",
                "반격할 수 없습니다. 최대한 피해다니세요."
        }),
        @Description(subject = "넉백", explain = {
                "강한 넉백을 가진 공격으로 맞을 시 대상에게 반격하러 가기 전에",
                "반격 시간이 끝날 수 있습니다..."
        })
}, stats = @Stats(offense = Level.FIVE, survival = Level.FIVE, crowdControl = Level.ZERO, mobility = Level.ZERO, utility = Level.ZERO), difficulty = Difficulty.NORMAL)

public class Echo extends AbilityBase {
	
	public Echo(Participant participant) {
		super(participant);
	}
	
	public static final SettingObject<Integer> COOLDOWN = abilitySettings.new SettingObject<Integer>(Echo.class,
			"cooldown", 30, "# 반격 쿨타임") {
		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}

		@Override
		public String toString() {
			return Formatter.formatCooldown(getValue());
		}
	};
	
	public static final SettingObject<Integer> DECREASE = abilitySettings.new SettingObject<Integer>(Echo.class,
			"cooldown-decrease", 5, "# 쿨타임 감소치") {
		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}

		@Override
		public String toString() {
			return Formatter.formatCooldown(getValue());
		}
	};
	
	private final ActionbarChannel ac = newActionbarChannel();
	private final Cooldown cool = new Cooldown(COOLDOWN.getValue(), CooldownDecrease._50);
	private int stack = 0;
	private static final RGB color = RGB.of(189, 189, 189);
	private final int decrease = DECREASE.getValue();
	private Player target;
	private double dmg;
	private double finaldmg;
	
	private final AbilityTimer counter = new AbilityTimer(15) {
		
		@Override
		public void run(int count) {
			ac.update("§c반격 찬스§f: §e" + target.getName() + "§f, " + (getCount() / 20.0) + "초");
		}
		
		@Override
		public void onEnd() {
			onSilentEnd();
		}
		
		@Override
		protected void onSilentEnd() {
			ac.update(null);
		}
		
    }.setPeriod(TimeUnit.TICKS, 1).register();
    
	private final AbilityTimer particle = new AbilityTimer(3) {
		
		@Override
		public void run(int count) {
			ParticleLib.REDSTONE.spawnParticle(getPlayer().getLocation().clone().add(Line.vectorAt(getPlayer().getLocation(), target.getPlayer().getLocation(), 3, count - 1)), color);
		}
		
		@Override
		public void onEnd() {
			onSilentEnd();
		}
		
		@Override
		public void onSilentEnd() {
			ParticleLib.ITEM_CRACK.spawnParticle(target.getLocation(), 0.5, 1, 0.5, 10, 0, MaterialX.WHITE_STAINED_GLASS_PANE);
			ParticleLib.ITEM_CRACK.spawnParticle(target.getLocation(), 0.5, 1, 0.5, 10, 0, MaterialX.GRAY_STAINED_GLASS_PANE);
			ParticleLib.ITEM_CRACK.spawnParticle(target.getLocation(), 0.5, 1, 0.5, 10, 0, MaterialX.LIGHT_GRAY_STAINED_GLASS_PANE);
			ParticleLib.ITEM_CRACK.spawnParticle(target.getLocation(), 0.5, 1, 0.5, 10, 0, MaterialX.BLACK_STAINED_GLASS_PANE);
		}
		
	}.setPeriod(TimeUnit.TICKS, 1).register();
	
	@SubscribeEvent
	public void onEntityDamageByEntity(EntityDamageByEntityEvent e) {
		if (e.getEntity().equals(getPlayer()) && e.getDamager() instanceof Player) {
			dmg = e.getDamage();
			finaldmg = e.getFinalDamage();
			target = (Player) e.getDamager();
			counter.start();
		}
		if (target != null) {
			if (e.getEntity().equals(target.getPlayer()) && e.getDamager().equals(getPlayer()) && counter.isRunning()) {
				if (cool.isRunning()) {
					cool.setCount(Math.max(cool.getCount() - decrease, 0));
				} else {
					particle.start();
					getPlayer().setHealth(Math.min(getPlayer().getHealth() + finaldmg, getPlayer().getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue()));
					if (counter.getCount() <= 1) {
						e.setDamage(e.getDamage() + (dmg * 0.1));
						getPlayer().sendMessage("§b>>> §c0.1배 대미지 반격!");
					} else if (counter.getCount() <= 2) {
						e.setDamage(e.getDamage() + (dmg * 0.2));
						getPlayer().sendMessage("§b>>> §c0.2배 대미지 반격!");
					} else if (counter.getCount() <= 3) {
						e.setDamage(e.getDamage() + (dmg * 0.25));
						getPlayer().sendMessage("§b>>> §c0.25배 대미지 반격!");
					} else if (counter.getCount() <= 4) {
						e.setDamage(e.getDamage() + (dmg * 0.5));
						getPlayer().sendMessage("§b>>> §c0.5배 대미지 반격!");
					} else if (counter.getCount() <= 5) {
						e.setDamage(e.getDamage() + (dmg * 0.75));
						getPlayer().sendMessage("§b>>> §c0.75배 대미지 반격!");
					} else if (counter.getCount() <= 7) {
						e.setDamage(e.getDamage() + (dmg * 1));
						getPlayer().sendMessage("§b>>> §c1배 대미지 반격!");
					} else if (counter.getCount() <= 8) {
						e.setDamage(e.getDamage() + (dmg * 1.2));
						getPlayer().sendMessage("§b>>> §c1.2배 대미지 반격!");
					} else if (counter.getCount() <= 10) {
						e.setDamage(e.getDamage() + (dmg * 1.3));
						getPlayer().sendMessage("§b>>> §c1.3배 대미지 반격!");
					} else if (counter.getCount() <= 12) {
						e.setDamage(e.getDamage() + (dmg * 1.4));
						getPlayer().sendMessage("§b>>> §c1.4배 대미지 반격!");
					} else {
						e.setDamage(e.getDamage() + (dmg * 1.5));
						getPlayer().sendMessage("§b>>> §c1.5배 대미지 반격!");
					}
					counter.stop(false);
					cool.start();
				}
				stack++;
				double temp = (double) stack;
				int soundnumber = (int) (temp - (Math.ceil(temp / SOUND_RUNNABLES.size()) - 1) * SOUND_RUNNABLES.size()) - 1;
				SOUND_RUNNABLES.get(soundnumber).run();
			}
		}
	}
	
	private final List<Runnable> SOUND_RUNNABLES = Arrays.asList(
			
			() -> {
					SoundLib.PIANO.playInstrument(getPlayer(), Note.natural(0, Tone.G));
					SoundLib.PIANO.playInstrument(getPlayer(), Note.sharp(0, Tone.A));
					SoundLib.PIANO.playInstrument(getPlayer(), Note.natural(1, Tone.G));
			},
			() -> {
					SoundLib.PIANO.playInstrument(getPlayer(), Note.natural(0, Tone.G));
					SoundLib.PIANO.playInstrument(getPlayer(), Note.sharp(0, Tone.A));
					SoundLib.PIANO.playInstrument(getPlayer(), Note.natural(1, Tone.G));
			},
			() -> {
					SoundLib.PIANO.playInstrument(getPlayer(), Note.natural(0, Tone.G));
					SoundLib.PIANO.playInstrument(getPlayer(), Note.sharp(0, Tone.A));
					SoundLib.PIANO.playInstrument(getPlayer(), Note.natural(1, Tone.G));
			},
			() -> {
					SoundLib.PIANO.playInstrument(getPlayer(), Note.natural(0, Tone.F));
			},
			() -> {
					SoundLib.PIANO.playInstrument(getPlayer(), Note.natural(0, Tone.G));
					SoundLib.PIANO.playInstrument(getPlayer(), Note.natural(1, Tone.G));
			},
			() -> {
					SoundLib.PIANO.playInstrument(getPlayer(), Note.natural(0, Tone.A));
					SoundLib.PIANO.playInstrument(getPlayer(), Note.natural(1, Tone.A));
			},
			() -> {
					SoundLib.PIANO.playInstrument(getPlayer(), Note.sharp(1, Tone.A));
					SoundLib.PIANO.playInstrument(getPlayer(), Note.natural(0, Tone.D));
					SoundLib.PIANO.playInstrument(getPlayer(), Note.natural(0, Tone.A));
			},
			() -> {
					SoundLib.PIANO.playInstrument(getPlayer(), Note.sharp(1, Tone.A));
					SoundLib.PIANO.playInstrument(getPlayer(), Note.natural(0, Tone.D));
					SoundLib.PIANO.playInstrument(getPlayer(), Note.natural(0, Tone.A));
			},
			() -> {
					SoundLib.PIANO.playInstrument(getPlayer(), Note.sharp(1, Tone.A));
					SoundLib.PIANO.playInstrument(getPlayer(), Note.natural(0, Tone.D));
					SoundLib.PIANO.playInstrument(getPlayer(), Note.natural(0, Tone.A));
			},
			() -> {
					SoundLib.PIANO.playInstrument(getPlayer(), Note.natural(1, Tone.A));
					SoundLib.PIANO.playInstrument(getPlayer(), Note.natural(0, Tone.D));
					SoundLib.PIANO.playInstrument(getPlayer(), Note.natural(0, Tone.A));
			},
			() -> {
					SoundLib.PIANO.playInstrument(getPlayer(), Note.sharp(1, Tone.A));
					SoundLib.PIANO.playInstrument(getPlayer(), Note.natural(0, Tone.F));
					SoundLib.PIANO.playInstrument(getPlayer(), Note.natural(0, Tone.A));
			},
			() -> {
					SoundLib.PIANO.playInstrument(getPlayer(), Note.natural(1, Tone.C));
					SoundLib.PIANO.playInstrument(getPlayer(), Note.sharp(0, Tone.F));
					SoundLib.PIANO.playInstrument(getPlayer(), Note.natural(0, Tone.C));
			},
			() -> {
					SoundLib.PIANO.playInstrument(getPlayer(), Note.natural(1, Tone.D));
					SoundLib.PIANO.playInstrument(getPlayer(), Note.natural(0, Tone.G));
					SoundLib.PIANO.playInstrument(getPlayer(), Note.natural(0, Tone.D));
			},
			() -> {
					SoundLib.PIANO.playInstrument(getPlayer(), Note.natural(1, Tone.D));
					SoundLib.PIANO.playInstrument(getPlayer(), Note.natural(0, Tone.G));
					SoundLib.PIANO.playInstrument(getPlayer(), Note.natural(0, Tone.D));
			},
			() -> {
					SoundLib.PIANO.playInstrument(getPlayer(), Note.natural(1, Tone.C));
					SoundLib.PIANO.playInstrument(getPlayer(), Note.natural(0, Tone.F));
					SoundLib.PIANO.playInstrument(getPlayer(), Note.natural(0, Tone.C));
			},
			() -> {
					SoundLib.PIANO.playInstrument(getPlayer(), Note.natural(1, Tone.D));
					SoundLib.PIANO.playInstrument(getPlayer(), Note.natural(0, Tone.G));
					SoundLib.PIANO.playInstrument(getPlayer(), Note.natural(0, Tone.D));
			},
			() -> {
					SoundLib.PIANO.playInstrument(getPlayer(), Note.natural(1, Tone.F));
					SoundLib.PIANO.playInstrument(getPlayer(), Note.sharp(0, Tone.A));
					SoundLib.PIANO.playInstrument(getPlayer(), Note.natural(0, Tone.F));
			},
			() -> {
					SoundLib.PIANO.playInstrument(getPlayer(), Note.natural(0, Tone.A));
					SoundLib.PIANO.playInstrument(getPlayer(), Note.natural(1, Tone.A));
			},
			() -> {
					SoundLib.PIANO.playInstrument(getPlayer(), Note.natural(0, Tone.A));
					SoundLib.PIANO.playInstrument(getPlayer(), Note.natural(1, Tone.A));
			},
			() -> {
					SoundLib.PIANO.playInstrument(getPlayer(), Note.natural(0, Tone.F));
					SoundLib.PIANO.playInstrument(getPlayer(), Note.natural(1, Tone.F));
			},
			() -> {
					SoundLib.PIANO.playInstrument(getPlayer(), Note.natural(0, Tone.D));
					SoundLib.PIANO.playInstrument(getPlayer(), Note.natural(1, Tone.D));
			},
			() -> {
					SoundLib.PIANO.playInstrument(getPlayer(), Note.natural(0, Tone.A));
					SoundLib.PIANO.playInstrument(getPlayer(), Note.natural(1, Tone.A));
			},
			() -> {
					SoundLib.PIANO.playInstrument(getPlayer(), Note.natural(0, Tone.A));
					SoundLib.PIANO.playInstrument(getPlayer(), Note.natural(1, Tone.A));
			},
			() -> {
					SoundLib.PIANO.playInstrument(getPlayer(), Note.natural(0, Tone.D));
					stack = 0;
			}
			
	);
	
}