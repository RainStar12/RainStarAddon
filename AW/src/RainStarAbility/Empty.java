package RainStarAbility;

import daybreak.abilitywar.ability.AbilityBase;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.Tips;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.ability.Tips.Description;
import daybreak.abilitywar.ability.Tips.Difficulty;
import daybreak.abilitywar.ability.Tips.Level;
import daybreak.abilitywar.ability.Tips.Stats;
import daybreak.abilitywar.ability.decorator.ActiveHandler;
import daybreak.abilitywar.ability.decorator.TargetHandler;
import daybreak.abilitywar.config.ability.AbilitySettings.SettingObject;
import daybreak.abilitywar.game.AbstractGame.GameTimer;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.AbstractGame.Participant.ActionbarNotification.ActionbarChannel;
import daybreak.abilitywar.game.list.mix.AbstractMix;
import daybreak.abilitywar.game.list.mix.Mix;
import daybreak.abilitywar.game.module.DeathManager;
import daybreak.abilitywar.utils.base.Formatter;
import daybreak.abilitywar.utils.base.TimeUtil;
import daybreak.abilitywar.utils.base.collect.SetUnion;
import daybreak.abilitywar.utils.base.math.LocationUtil;
import daybreak.google.common.base.Predicate;

import java.util.Iterator;
import java.util.Set;
import java.util.StringJoiner;

import javax.annotation.Nullable;

import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

@AbilityManifest(name = "[ ]", rank = Rank.S, species = Species.OTHERS, explain = {
		"§a---------------------------------",
		"$(EXPLAIN)",
		"§a---------------------------------",
		"철괴로 대상을 30칸 내에서 우클릭하여 능력을 30초간 복제합니다. $[MIN_COOLDOWN]",
		"이후 [  ]으로 되돌아옵니다. 되돌아올 때 복제한 능력이",
		"쿨타임일 경우, 절반의 시간만큼 [  ]의 쿨타임을 더합니다."
})

@Tips(tip = {
        "다른 대상의 능력을 복제할 수 있어서 무한한 가능성을 가진 능력입니다.",
        "대상의 능력을 복제하여 능력을 알아차릴 수 있다던가, 강한 능력을 복제하여",
        "내 멋대로 사용이 가능하고 어느 한 능력에 멈춰있지 않는 능력입니다.",
        "다만 능력 사용에 있어 타게팅이 강제되기에 다른 대상에게 접근하는 것을",
        "유의하셔야만 합니다."
}, strong = {
        @Description(subject = "강한 능력의 대상", explain = {
                "다른 대상의 능력이 강력할수록 복제하는 본인의 능력도 강력해집니다."
        }),
        @Description(subject = "능력 파악", explain = {
                "타인의 능력을 복제하여 대상의 능력이 무엇인지 알아낼 수 있습니다."
        }),
        @Description(subject = "유동성", explain = {
                "능력을 복제할 수 있다는 것은 다시 말해, 게임 참가자의 모든 능력들을",
                "본인이 사용 가능하다는 말입니다. 여러 능력으로 바꾸며 수많은 전술을",
                "펼칠 수 있는 유동성이야말로 이 능력의 최대 강점입니다."
        })
}, weak = {
        @Description(subject = "약한 능력의 대상", explain = {
                "역으로 대상이 약한 능력을 소지하고 있다면, 복제한 자신의 능력도",
                "약해지는 꼴이 됩니다. 다른 대상의 능력을 미리 파악해두세요."
        }),
        @Description(subject = "적은 참가자 수", explain = {
                "게임 참가자 수가 적으면 결과적으로 이 능력의 최대 강점인",
                "유동성을 해치는 꼴이 됩니다. 되도록 플레이어가 많은 게임을",
                "진행하는 편이 좋습니다."
        }),
        @Description(subject = "짧은 지속시간", explain = {
                "복제한 능력을 사용 가능한 시간은 20초밖에 채 되지 않아,",
                "대부분의 액티브 능력은 기껏해야 한 번 사용할 수 있습니다.",
                "또한 복제한 능력의 쿨타임을 절반 가지므로",
                "빠르고 신중한 판단력을 요합니다."
        })
}, stats = @Stats(offense = Level.ZERO, survival = Level.ZERO, crowdControl = Level.ZERO, mobility = Level.ZERO, utility = Level.TEN), difficulty = Difficulty.NORMAL)

public class Empty extends AbilityBase implements ActiveHandler, TargetHandler {

	public static final SettingObject<Integer> MIN_COOLDOWN = abilitySettings.new SettingObject<Integer>(Empty.class, "COOLDOWN", 30, "# 최소 쿨타임") {

		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}

		@Override
		public String toString() {
			return Formatter.formatCooldown(getValue()) + " + n";
		}

	};

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
			}
			return true;
		}

		@Override
		public boolean apply(@Nullable Entity arg0) {
			return false;
		}
	};
	
	private AbilityBase ability;

	@SuppressWarnings("unused")
	private final Object EXPLAIN = new Object() {
		@Override
		public String toString() {
			if (ability == null) {
				return "능력을 복제할 수 있습니다.".toString();
			} else {
				final StringJoiner joiner = new StringJoiner("\n");
				joiner.add("§a복제한 능력 §f| §7[§b" + ability.getName() + "§7] " + ability.getRank().getRankName() + " " + ability.getSpecies().getSpeciesName());
				for (final Iterator<String> iterator = ability.getExplanation(); iterator.hasNext();) {
					joiner.add("§f" + iterator.next());
				}
				return joiner.toString();
			}
		}
	};
	
	@Override
	public String getDisplayName() {
		return "[" + (ability != null ? (ability.getName() + "]") : " ]");
	}

	private final int minCooldown = MIN_COOLDOWN.getValue();
	private final ActionbarChannel actionbarChannel = newActionbarChannel();
	private final Cooldown cooldown = new Cooldown(MIN_COOLDOWN.getValue(), "공백");

	public Empty(Participant participant) {
		super(participant);
	}

	@Override
	public boolean ActiveSkill(Material material, ClickType clickType) {
		if (ability != null) {
			return ability instanceof ActiveHandler && ((ActiveHandler) ability).ActiveSkill(material, clickType);	
		} else {
			if (material == Material.IRON_INGOT && clickType == ClickType.RIGHT_CLICK) {
				if (!cooldown.isCooldown()) {
					Player player = LocationUtil.getEntityLookingAt(Player.class, getPlayer(), 30, predicate);
					if (player != null) {
						final Player entityPlayer = (Player) player;
						final Participant target = getGame().getParticipant(entityPlayer);
						if (target.hasAbility() && !target.getAbility().isRestricted()) {
							final AbilityBase targetAbility = target.getAbility();
							if (getGame() instanceof AbstractMix) {
								final Mix targetMix = (Mix) targetAbility;
								if (targetMix.hasAbility()) {
									if (targetMix.hasSynergy()) {
										try {
											this.ability = AbilityBase.create(targetMix.getSynergy().getClass(), getParticipant());
											this.ability.setRestricted(false);
											getPlayer().sendMessage("§b능력을 복제하였습니다. 당신의 능력은 §e" + ability.getName() + "§b 입니다.");
											new ReturnTimer();
										} catch (ReflectiveOperationException e) {
											e.printStackTrace();
										}
									} else {
										final Mix myMix = (Mix) getParticipant().getAbility();
										final AbilityBase myFirst = myMix.getFirst(), first = targetMix.getFirst(), second = targetMix.getSecond();
										
										try {
											final Class<? extends AbilityBase> clazz = (this.equals(myFirst) ? first : second).getClass();
											if (clazz != Empty.class) {
												this.ability = AbilityBase.create(clazz, getParticipant());
												this.ability.setRestricted(false);
												getPlayer().sendMessage("§b능력을 복제하였습니다. 당신의 능력은 §e" + ability.getName() + "§b 입니다.");
												new ReturnTimer();
											} else {
												getPlayer().sendMessage("§b공백은 복제할 수 없습니다.");
											}
										} catch (ReflectiveOperationException e) {
											e.printStackTrace();
										}
									}

								}

							} else {
								try {
									final Class<? extends AbilityBase> clazz = targetAbility.getClass();
									if (clazz != Empty.class) {
										this.ability = AbilityBase.create(clazz, getParticipant());
										this.ability.setRestricted(false);
										getPlayer().sendMessage("§b능력을 복제하였습니다. 당신의 능력은 §e" + targetAbility.getName() + "§b 입니다.");
										new ReturnTimer();
									} else {
										getPlayer().sendMessage("§b공백은 복제할 수 없습니다.");
									}
								} catch (ReflectiveOperationException e) {
									e.printStackTrace();
								}
							}
						}
					}
				}
			}
		}
		return false;
	}

	@Override
	public void TargetSkill(Material material, LivingEntity entity) {
		if (ability != null) {
			if (ability instanceof TargetHandler) {
			((TargetHandler) ability).TargetSkill(material, entity);
			}
		}
	}

	@Override
	public Set<GameTimer> getTimers() {
		return ability != null ? SetUnion.union(super.getTimers(), ability.getTimers()) : super.getTimers();
	}

	@Override
	public Set<GameTimer> getRunningTimers() {
		return ability != null ? SetUnion.union(super.getRunningTimers(), ability.getRunningTimers()) : super.getRunningTimers();
	}

	@Override
	public boolean usesMaterial(Material material) {
		if (ability != null) {
			return ability.usesMaterial(material);
		}
		return super.usesMaterial(material);
	}

	@Override
	protected void onUpdate(Update update) {
		if (update == Update.RESTRICTION_CLEAR) {
			if (ability != null) {
				ability.setRestricted(false);
			}
		} else if (update == Update.RESTRICTION_SET) {
			if (ability != null) {
				ability.setRestricted(true);
			}
		} else if (update == Update.ABILITY_DESTROY) {
			if (ability != null) {
				ability.destroy();
				ability = null;
			}
		}
	}

	private class ReturnTimer extends AbilityTimer {

		private ReturnTimer() {
			super(TaskType.REVERSE, 30);
			this.start();
		}

		@Override
		protected void run(int arg0) {
			actionbarChannel.update("[  ]으로 귀환까지 §3" + TimeUtil.parseTimeAsString(getFixedCount()) + "§f 남았습니다.");
		}

		@Override
		protected void onEnd() {
			try {
				int cooltimeSum = 0;
				if (Empty.this.ability != null) {
					for (GameTimer timer : Empty.this.ability.getRunningTimers()) {
						if (timer instanceof Cooldown.CooldownTimer) {
							cooltimeSum += timer.getCount();
						}
					}
					Empty.this.ability.destroy();
				}
				Empty.this.ability = null;
				getParticipant().getPlayer().sendMessage("§b당신의 능력이 §f[  ]§b으로 되돌아왔습니다.");
				actionbarChannel.update(null);
				cooldown.setCooldown(minCooldown + (cooltimeSum / 2));
				cooldown.start();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

}