package RainStarAbility;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import javax.annotation.Nullable;

import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

import daybreak.abilitywar.ability.AbilityBase;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.ability.Materials;
import daybreak.abilitywar.ability.Tips;
import daybreak.abilitywar.ability.Tips.Description;
import daybreak.abilitywar.ability.Tips.Difficulty;
import daybreak.abilitywar.ability.Tips.Level;
import daybreak.abilitywar.ability.Tips.Stats;
import daybreak.abilitywar.ability.SubscribeEvent;
import daybreak.abilitywar.ability.decorator.TargetHandler;
import daybreak.abilitywar.config.ability.AbilitySettings.SettingObject;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.AbstractGame.Participant.ActionbarNotification.ActionbarChannel;
import daybreak.abilitywar.game.module.DeathManager;
import daybreak.abilitywar.game.team.interfaces.Teamable;
import daybreak.abilitywar.utils.base.Formatter;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.base.math.LocationUtil;
import daybreak.abilitywar.utils.library.MaterialX;
import daybreak.abilitywar.utils.library.ParticleLib;
import daybreak.abilitywar.utils.library.SoundLib;
import daybreak.google.common.base.Predicate;

@AbilityManifest(
		name = "간파",
		rank = Rank.A, 
		species = Species.HUMAN, 
		explain = {
		"검으로 대상을 우클릭 시 대상의 수를 $[DurationConfig]초간 간파합니다. $[CooldownConfig]",
		"수를 간파당한 대상의 근접 공격을 전부 회피합니다.",
		"간파 중 예상하지 못한 피해를 받았을 때 1의 추가 피해를 입습니다."
		})

@Tips(tip = {
        "근접 공격을 봉인함으로서 대상에게 2~3타 정도의 프리딜을 넣을 수 있습니다.",
        "또한 버서커 같은 한 방의 공격이 강력한 공격을 완전히 막을 수 있습니다.",
        "다만 대상에게서 간파 도중 활 공격 등으로 피해를 입는다면 독이 되니,",
        "상대의 능력을 잘 파악하고 사용하시는 것을 추천합니다."
}, strong = {
        @Description(subject = "근접전", explain = {
                "근접전이야말로 이 능력을 최대 활용 가능한 강점입니다.",
                "상대의 근접 공격을 무시하여 생존력을 높일 수 있습니다."
        }),
        @Description(subject = "좁은 공간", explain = {
                "좁은 공간일수록 간파 도중 대상이 도망치지 못하게 막아",
                "공격을 가하기 더 용이합니다."
        }),
        @Description(subject = "순간 높은 근접대미지를 내는 대상", explain = {
                "버서커나 넥스 등 한 방의 힘을 실린 공격을 무시하여,",
                "상대의 능력 피해를 최소화시킵니다."
        })
}, weak = {
        @Description(subject = "원거리전", explain = {
                "대상이 활 능력을 소지하고 있거나 원거리전으로 돌입한다면",
                "오히려 대상에게서 매번 2의 추가 피해를 입으므로 주의해야 합니다."
        })
}, stats = @Stats(offense = Level.ZERO, survival = Level.SEVEN, crowdControl = Level.ZERO, mobility = Level.ZERO, utility = Level.TWO), difficulty = Difficulty.EASY)

@Materials(materials = {
		Material.WOOD_SWORD,
		Material.STONE_SWORD,
		Material.IRON_SWORD,
		Material.GOLD_SWORD,
		Material.DIAMOND_SWORD
	})

public class Detection extends AbilityBase implements TargetHandler { 

	public Detection(Participant participant) {
		super(participant);
	}

	private final Cooldown skill = new Cooldown(CooldownConfig.getValue());
	private final int duration = DurationConfig.getValue();

	private Set<UUID> noatk = new HashSet<>();
	private boolean dur = false;
	private boolean config = ChangeConfig.getValue();
	private int range = RangeConfig.getValue();
	
	@Override
	public boolean usesMaterial(Material material) {
		return (material == MaterialX.WOODEN_SWORD.getMaterial()
				|| material == MaterialX.STONE_SWORD.getMaterial()
				|| material == MaterialX.IRON_SWORD.getMaterial()
				|| material == MaterialX.GOLDEN_SWORD.getMaterial()
				|| material == MaterialX.DIAMOND_SWORD.getMaterial()
				|| material == MaterialX.NETHERITE_SWORD.getMaterial());
	}
	
	public static final SettingObject<Integer> DurationConfig = 
			abilitySettings.new SettingObject<Integer>(Detection.class,
			"Duration", 3, "# 능력 지속시간") {

		@Override
		public boolean condition(Integer value) {
			return value >= 1;
		}

	};
	
	public static final SettingObject<Integer> RangeConfig = abilitySettings.new SettingObject<Integer>(
			Detection.class,
			"Range", 3, "# 범위 설정", "# 주의! 범위 모드로 Change 콘피그 변경 후 적용됩니다.") {

		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}

	};	
	
	public static final SettingObject<Boolean> ChangeConfig = abilitySettings.new SettingObject<Boolean>(
			Detection.class,
			"Change", false, "# true로 변경하시면 간파 시도시 대상의",
			"일부 범위 내의 모든 플레이어의 근접 공격을 간파합니다.") {
	};
	
	@SubscribeEvent
	public void onEntityDamageByEntity(EntityDamageByEntityEvent e) {
		if (noatk.contains(e.getDamager().getUniqueId()) && e.getEntity() == getPlayer()) {
			SoundLib.ENTITY_PLAYER_ATTACK_SWEEP.playSound(getPlayer().getLocation(), 1, 1.7f);
			e.setCancelled(true);
		}
		if (dur == true) {
				if (e.getEntity().equals(getPlayer())) {
					e.setDamage(e.getDamage() + 1);
			}
		}
	}
	 
	public static final SettingObject<Integer> CooldownConfig = 
			abilitySettings.new SettingObject<Integer>(Detection.class, "Cooldown", 45,
            "# 쿨타임") {

        @Override
        public boolean condition(Integer value) {
            return value >= 0;
        }

        @Override
        public String toString() {
            return Formatter.formatCooldown(getValue());
        }

    };

	private final Predicate<Entity> predicate = new Predicate<Entity>() {
		@Override
		public boolean test(Entity entity) {
			if (entity.equals(getPlayer())) return false;
			if (entity instanceof Player) {
				if (!getGame().isParticipating(entity.getUniqueId())
						|| (getGame() instanceof DeathManager.Handler &&
								((DeathManager.Handler) getGame()).getDeathManager().isExcluded(entity.getUniqueId()))
						|| !getGame().getParticipant(entity.getUniqueId()).attributes().TARGETABLE.getValue()) {
					return false;
				}
				if (getGame() instanceof Teamable) {
					final Teamable teamGame = (Teamable) getGame();
					final Participant entityParticipant = teamGame.getParticipant(
							entity.getUniqueId()), participant = getParticipant();
					return !teamGame.hasTeam(entityParticipant) || !teamGame.hasTeam(participant)
							|| (!teamGame.getTeam(entityParticipant).equals(teamGame.getTeam(participant)));
				}
			}
			return true;
		}

		@Override
		public boolean apply(@Nullable Entity arg0) {
			return false;
		}
	};
    
	public void TargetSkill (Material material, LivingEntity entity) {
		if ((material.equals(Material.WOOD_SWORD) || 
				material.equals(Material.STONE_SWORD) || 
				material.equals(Material.IRON_SWORD) || 
				material.equals(Material.GOLD_SWORD) || 
				material.equals(Material.DIAMOND_SWORD)) 
				&& entity instanceof Player && !skill.isCooldown()) {
			
			Player p = (Player) entity;
			if (config) {
				for (Player player : LocationUtil.getNearbyEntities(Player.class, p.getLocation(), range, range, predicate)) {
					Participant target = getGame().getParticipant(player);
					new AbilityTimer(duration * 20) {

						private ActionbarChannel actionbarChannel;

						@Override
						protected void run(int count) {
							actionbarChannel.update("§e" + getPlayer().getName() + " §f에게 §c근접 공격 불능 §7: §f" + (getFixedCount()) + " 초");
						}

						@Override
						protected void onStart() {
							noatk.add(target.getPlayer().getUniqueId());
							dur = true;
							actionbarChannel = target.actionbar().newChannel();
						}

						@Override
						protected void onEnd() {
							noatk.remove(target.getPlayer().getUniqueId());
							dur = false;
							if (actionbarChannel != null)
								actionbarChannel.unregister();
						}

						@Override
						protected void onSilentEnd() {
							noatk.remove(target.getPlayer().getUniqueId());
							dur = false;
							if (actionbarChannel != null)
								actionbarChannel.unregister();
						}
					}.setPeriod(TimeUnit.TICKS, 1).start();		
					SoundLib.ENTITY_IRON_GOLEM_DEATH.playSound(target.getPlayer(), 10, 0.5f);
					getPlayer().sendMessage("§e" + target.getPlayer().getName() + " §f님의 공격을 간파하였습니다.");
				}
				SoundLib.BLOCK_ENCHANTMENT_TABLE_USE.playSound(getPlayer());
				ParticleLib.ENCHANTMENT_TABLE.spawnParticle(getPlayer().getLocation(), 1, 1, 1, 500, 0);
				skill.start();
			} else {
				Participant target = getGame().getParticipant(p);
				new AbilityTimer(duration * 20) {

					private ActionbarChannel actionbarChannel;

					@Override
					protected void run(int count) {
						actionbarChannel.update("§e" + getPlayer().getName() + " §f에게 §c근접 공격 불능 §7: §f" + (getFixedCount()) + " 초");
					}

					@Override
					protected void onStart() {
						noatk.add(target.getPlayer().getUniqueId());
						dur = true;
						actionbarChannel = target.actionbar().newChannel();
					}

					@Override
					protected void onEnd() {
						noatk.remove(target.getPlayer().getUniqueId());
						dur = false;
						if (actionbarChannel != null)
							actionbarChannel.unregister();
					}

					@Override
					protected void onSilentEnd() {
						noatk.remove(target.getPlayer().getUniqueId());
						dur = false;
						if (actionbarChannel != null)
							actionbarChannel.unregister();
					}
				}.setPeriod(TimeUnit.TICKS, 1).start();		
				SoundLib.BLOCK_ENCHANTMENT_TABLE_USE.playSound(getPlayer());
				SoundLib.ENTITY_IRON_GOLEM_DEATH.playSound(target.getPlayer(), 10, 0.5f);
				getPlayer().sendMessage("§e" + target.getPlayer().getName() + " §f님의 공격을 간파하였습니다.");
				ParticleLib.ENCHANTMENT_TABLE.spawnParticle(getPlayer().getLocation(), 1, 1, 1, 500, 0);
				
				skill.start();
			}
		}		
	}
}