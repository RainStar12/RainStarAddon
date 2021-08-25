package RainStarAbility;

import javax.annotation.Nullable;

import org.bukkit.attribute.Attribute;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

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
import daybreak.abilitywar.game.module.DeathManager;
import daybreak.abilitywar.game.team.interfaces.Teamable;
import daybreak.abilitywar.utils.base.Formatter;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.base.math.LocationUtil;
import daybreak.abilitywar.utils.base.minecraft.damage.Damages;
import daybreak.abilitywar.utils.library.ParticleLib;
import daybreak.abilitywar.utils.library.SoundLib;
import daybreak.google.common.base.Predicate;

@AbilityManifest(
		name = "라이트닝 카운터", rank = Rank.A, species = Species.HUMAN, explain = {
		"§7달리기 §8- §3빨리빨리§f: 달리면서 10초 안에 누군가를 스쳐 지나갈 때마다",
		" $[DAMAGE]의 피해를 입히고 느리게 만들며, 가속합니다. 달리지 않을 때 가속을 1 잃습니다.",
		"§7가속 §8- §b남들보다 빠르게!§f: 가속 시마다 이동 속도가 증가하며,",
		" 스치며 주는 피해량이 $[DAMAGE_INCREASE]씩 증가합니다. 최대 5단계까지 가속이 가능하고",
		" 매 가속 시마다 §3빨리빨리§f의 지속 시간이 10초로 갱신됩니다.",
		"§7피격 §8- §c내가 졌다고?§f: 최대 가속 중 플레이어에게 피해받을 때",
		" 가속이 초기화되고, 쿨타임을 가집니다. $[COOLDOWN]",
		"§7근접 공격 §8- §eE=mc²§f: 내 현재 이동 속도에 비례하여 피해량이 강력해집니다."
		},
		summarize = {
		"§7달리면서 누군가를 스쳐 지나가면§f 피해를 입히고 느리게 만든 후, §b가속§f합니다.",
		"달리기를 해제할 때마다 가속을 1 잃고, §b가속§f의 수치만큼",
		"스쳐 지나가는 피해량이 점점 증가합니다.",
		"또한 내 현재 이동 속도에 비례하여 근접 타격 시 추가 피해를 입힙니다."
		})

@Tips(tip = {
        "빠른 이동속도를 살려 적을 치고 나가며 싸우는 능력입니다.",
        "기본 이동속도가 증가하기 때문에 신속 버프를 얻는다면 더 빨리",
        "움직일 수 있고, 중도에 달리기가 끊기지 않아야 하기에",
        "넓은 평지 지형 및 뭉쳐있는 여럿 대상으로 유리합니다.",
        "다만 달리는 도중 공격 시 달리기가 끊기는 판정이 존재하므로",
        "능력을 잘 살리고 싶다면 주의하세요."
}, strong = {
        @Description(subject = "속도", explain = {
        		"빠른 속도야말로 이 능력의 강점입니다.",
        		"다른 플레이어를 추격할 때도, 상황을 벗어날 때도",
        		"기본적인 속도 증가는 좋게 작용합니다."
        }),
        @Description(subject = "평야", explain = {
        		"달리기가 끊이지 않아야 하기 때문에, 평야 등",
        		"장애물이 없는 지형에서 더욱 유리합니다."
        })
}, weak = {
        @Description(subject = "장애물", explain = {
        		"산 등 자연적인 상승지형과 나무, 건물 등 모든 블록이",
        		"이 능력의 장애물이 될 수 있습니다. 가능하다면 평야를",
        		"찾는 것을 추천드립니다."
        })
}, stats = @Stats(offense = Level.TWO, survival = Level.ZERO, crowdControl = Level.ZERO, mobility = Level.SIX, utility = Level.ZERO), difficulty = Difficulty.NORMAL)

public class LightningCounter extends AbilityBase {
	
	public LightningCounter(Participant participant) {
		super(participant);
	}
	
	private final Cooldown RunCool = new Cooldown(COOLDOWN.getValue(), CooldownDecrease._25);
	private PotionEffect slow = new PotionEffect(PotionEffectType.SLOW, 60, 1, true, false);
	
	public static final SettingObject<Integer> COOLDOWN = abilitySettings.new SettingObject<Integer>(
			LightningCounter.class, "cooldown", 10, "# 쿨타임") {
		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}

		@Override
		public String toString() {
			return Formatter.formatCooldown(getValue());
		}
	};
	
	public static final SettingObject<Double> DAMAGE = abilitySettings.new SettingObject<Double>(
			LightningCounter.class, "damage", 7.0, "# 기본 대미지") {
		@Override
		public boolean condition(Double value) {
			return value >= 0;
		}
	};
	
	public static final SettingObject<Double> DAMAGE_INCREASE = abilitySettings.new SettingObject<Double>(
			LightningCounter.class, "damage-increase", 1.0, "# 대미시 상승량") {
		@Override
		public boolean condition(Double value) {
			return value >= 0;
		}
	};
	
	protected void onUpdate(AbilityBase.Update update) {
		if (update == AbilityBase.Update.RESTRICTION_CLEAR) {
			actionbarChannel.update("§3가속 §b0§3단계");
		    running.start();
		    getPlayer().setWalkSpeed(0.3f);
		    getPlayer().setFlySpeed(0.2f);
		}
		if (update == AbilityBase.Update.RESTRICTION_SET || update == AbilityBase.Update.ABILITY_DESTROY) {
			getPlayer().setSprinting(false);
		    getPlayer().setWalkSpeed(0.2F);
		    getPlayer().setFlySpeed(0.1F);
		} 
	}
	
	private final double baseDMG = DAMAGE.getValue();
	private final double increaseDMG = DAMAGE_INCREASE.getValue();
	private double dmg = baseDMG;
	private LivingEntity pentity = null;
	private final ActionbarChannel actionbarChannel = newActionbarChannel();
	
	private final Attacking attacking = new Attacking();
	
	private final Predicate<Entity> predicate = new Predicate<Entity>() {
		@Override
		public boolean test(Entity entity) {
			if (entity.equals(getPlayer())) return false;
			if (entity instanceof Player) {
				if (!getGame().isParticipating(entity.getUniqueId())
						|| (getGame() instanceof DeathManager.Handler && ((DeathManager.Handler) getGame()).getDeathManager().isExcluded(entity.getUniqueId()))
						|| !getGame().getParticipant(entity.getUniqueId()).attributes().TARGETABLE.getValue()) {
					return false;
				}
				if (getGame() instanceof Teamable) {
					final Teamable teamGame = (Teamable) getGame();
					final Participant entityParticipant = teamGame.getParticipant(entity.getUniqueId()), participant = getParticipant();
					return !teamGame.hasTeam(entityParticipant) || !teamGame.hasTeam(participant) || (!teamGame.getTeam(entityParticipant).equals(teamGame.getTeam(participant)));
				}
			}
			if (entity instanceof ArmorStand) {
				return false;
			}
			return true;
		}

		@Override
		public boolean apply(@Nullable Entity arg0) {
			return false;
		}
	};
	
	@SubscribeEvent
	public void onEntityDamageByEntity(EntityDamageByEntityEvent e) {
		if (attacking.stack >= 5) {
			if (e.getDamager() instanceof Player && e.getEntity().equals(getPlayer())) {
			attacking.stop(true);
			RunCool.start();
			}
		}
		if (e.getDamager().equals(getPlayer())) {
			double speed = getPlayer().getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).getValue();
			e.setDamage(e.getDamage() * (1 + ((speed - 0.15) * 2)));
		}
	}
	
	private final AbilityTimer running = new AbilityTimer() {
		@Override
		protected void run(int count) {
			if (getPlayer().isSprinting() && !RunCool.isRunning()) {
				attacking.start();
			} else if (!getPlayer().isSprinting() && !RunCool.isRunning() && attacking.isRunning() && attacking.stack >= 1) {
				attacking.stack--;
				attacking.stop(true);
				actionbarChannel.update("§3가속 §b" + attacking.stack + "§3단계, §6지속 시간§f: §f종료");
				float speed = 0.2f;
				switch(attacking.stack) {
				case 0:
					speed = 0.2f;
					break;
				case 1:
					speed = 0.225f;
					break;
				case 2:
					speed = 0.25f;
					break;
				case 3:
					speed = 0.3f;
					break;
				case 4:
					speed = 0.35f;
					break;
				case 5:
					speed = 0.4f;
					break;
				}
				dmg = baseDMG + (increaseDMG * attacking.stack);
				getPlayer().setWalkSpeed(speed + 0.1f);
				getPlayer().setFlySpeed(speed);
			}
		}
	}.setPeriod(TimeUnit.TICKS, 1);
	
	private class Attacking extends AbilityTimer {
		
		int stack = 0;
		
		private Attacking() {
			super(TaskType.REVERSE, 200);
			setPeriod(TimeUnit.TICKS, 1);
		}
		
		@Override
		protected void run(int count) {
			actionbarChannel.update("§3가속 §b" + stack + "§3단계, §6지속 시간§f: " + getFixedCount() + "초");		
			if (!getPlayer().isDead() && getPlayer().isSprinting()) {		
				if (pentity == null) {
					for (LivingEntity livingentity : LocationUtil.getConflictingEntities(LivingEntity.class, getPlayer(), predicate)) {
						pentity = livingentity;
						break;
					}
				} else {
					if (!LocationUtil.isConflicting(getPlayer(), pentity)) {
						Damages.damageMagic(pentity, getPlayer(), false, (float) dmg);
						pentity.addPotionEffect(slow);
						pentity = null;
						addStack();
					}
				}
			}
 			
		}
		
		private void addStack() {
			if (stack < 5) {
				stack++;
			}
			if (isRunning()) {
				setCount(200);
				actionbarChannel.update("§3가속 §b" + stack + "§3단계, §6지속 시간§f: " + getFixedCount() + "초");
			}
			float speed = 0.2f;
			switch(attacking.stack) {
			case 0:
				speed = 0.2f;
				break;
			case 1:
				speed = 0.225f;
				break;
			case 2:
				speed = 0.25f;
				break;
			case 3:
				speed = 0.3f;
				break;
			case 4:
				speed = 0.35f;
				break;
			case 5:
				speed = 0.4f;
				break;
			}
			SoundLib.ENTITY_FIREWORK_ROCKET_LARGE_BLAST.playSound(getPlayer(), 1, stack == 1 ? 1f : 0.9f + (stack * 0.2f));
			ParticleLib.FLAME.spawnParticle(getPlayer().getLocation(), 0, 0, 0, 50, 0.2 + (stack * 0.1));
			dmg = baseDMG + (increaseDMG * attacking.stack);
			getPlayer().setWalkSpeed(speed + 0.1f);
			getPlayer().setFlySpeed(speed);
		}
		
		@Override
		protected void onEnd() {
			actionbarChannel.update("§6지속 시간 §f종료");
			stack = 0;
			getPlayer().setWalkSpeed(0.3f);
			getPlayer().setFlySpeed(0.2f);
			dmg = baseDMG;
		}
		
		@Override
		protected void onSilentEnd() {
		}	
	}
}