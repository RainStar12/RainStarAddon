package rainstar.abilitywar.ability;

import static rainstar.abilitywar.system.config.RainStarAbilitySetting.rsAbilitySettings;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.LivingEntity;

import daybreak.abilitywar.ability.AbilityBase;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.AbilityFactory.AbilityRegistration;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.ability.decorator.ActiveHandler;
import daybreak.abilitywar.ability.decorator.TargetHandler;
import daybreak.abilitywar.config.Configuration;
import daybreak.abilitywar.config.ability.AbilitySettings.SettingObject;
import daybreak.abilitywar.game.AbstractGame.GameTimer;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.list.mix.synergy.SynergyFactory;
import daybreak.abilitywar.game.manager.AbilityList;
import daybreak.abilitywar.utils.annotations.Beta;
import daybreak.abilitywar.utils.base.collect.SetUnion;
import daybreak.abilitywar.utils.base.random.Random;
import rainstar.abilitywar.utils.RankColor;

@AbilityManifest(name = "언노운", rank = Rank.A, species = Species.OTHERS, explain = {
		"능력 활성화 시 무작위 §b능력§f을 2개 받습니다.",
		"해당 §b능력§f에 대한 설명이 따로 나오지 않습니다. §c§n스스로 알아내야 합니다.§f",
		"아주 낮은 확률로 해당 §b능력§f에 §6시너지§f가 포함될 수 있습니다."
		})
public class Unknown extends AbilityBase implements ActiveHandler, TargetHandler {
	
	public Unknown(Participant participant) {
		super(participant);
	}
	
	public static final SettingObject<Integer> SYNERGY_CHANCE = rsAbilitySettings.new SettingObject<Integer>(Unknown.class,
			"synergy-chance", 10, "# 시너지 등장 확률", "# 기준: n/1000", "# 10으로 입력 시 10/1000, 1%입니다.") {
		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}
	};
	
	public static final SettingObject<Integer> RANK_C_CHANCE = rsAbilitySettings.new SettingObject<Integer>(Unknown.class,
			"rank-c-chance", 5, "# C등급 등장 확률", "# 기준: n/모든 등급 확률 합산") {
		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}
	};

	public static final SettingObject<Integer> RANK_B_CHANCE = rsAbilitySettings.new SettingObject<Integer>(Unknown.class,
			"rank-b-chance", 6, "# B등급 등장 확률", "# 기준: n/모든 등급 확률 합산") {
		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}
	};

	public static final SettingObject<Integer> RANK_A_CHANCE = rsAbilitySettings.new SettingObject<Integer>(Unknown.class,
			"rank-a-chance", 7, "# A등급 등장 확률", "# 기준: n/모든 등급 확률 합산") {
		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}
	};

	public static final SettingObject<Integer> RANK_S_CHANCE = rsAbilitySettings.new SettingObject<Integer>(Unknown.class,
			"rank-s-chance", 6, "# S등급 등장 확률", "# 기준: n/모든 등급 확률 합산") {
		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}
	};

	public static final SettingObject<Integer> RANK_L_CHANCE = rsAbilitySettings.new SettingObject<Integer>(Unknown.class,
			"rank-l-chance", 5, "# L등급 등장 확률", "# 기준: n/모든 등급 확률 합산") {
		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}
	};

	public static final SettingObject<Integer> RANK_SPECIAL_CHANCE = rsAbilitySettings.new SettingObject<Integer>(Unknown.class,
			"rank-special-chance", 1, "# SPECIAL등급 등장 확률", "# 기준: n/모든 등급 확률 합산") {
		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}
	};
	
	private AbilityBase ability1, ability2;
	private final Random random = new Random();
	private final int chance = SYNERGY_CHANCE.getValue();
	private final int rankCchance = RANK_C_CHANCE.getValue();
	private final int rankBchance = RANK_B_CHANCE.getValue();
	private final int rankAchance = RANK_A_CHANCE.getValue();
	private final int rankSchance = RANK_S_CHANCE.getValue();
	private final int rankLchance = RANK_L_CHANCE.getValue();
	private final int rankSpecialchance = RANK_SPECIAL_CHANCE.getValue();

	private final int chanceSum = rankCchance + rankBchance + rankAchance + rankSchance + rankLchance + rankSpecialchance;
	
	private Rank getRandomRank() {
		int value = random.nextInt(chanceSum);

		if ((value -= rankCchance) < 0) return Rank.C;
		if ((value -= rankBchance) < 0) return Rank.B;
		if ((value -= rankAchance) < 0) return Rank.A;
		if ((value -= rankSchance) < 0) return Rank.S;
		if ((value -= rankLchance) < 0) return Rank.L;
		return Rank.SPECIAL;
	}
	
	public AbilityRegistration getRandomRankAbility(Rank rank) {
		final List<AbilityRegistration> registrations = AbilityList.values().stream().filter(
				ability -> ability.getAbilityClass().getAnnotation(Beta.class) == null && ability.isAvailable(getGame().getClass()) && ability.getManifest().rank().equals(rank) && !Configuration.Settings.isBlacklisted(ability.getManifest().name())
		).collect(Collectors.toList());
		return registrations.isEmpty() ? null : random.pick(registrations);
	}
	
    public AbilityRegistration getRandomSynergy() {
		Set<AbilityRegistration> synergies = new HashSet<>();
		
        for (AbilityRegistration synergy : SynergyFactory.getSynergies()) {
        	String name = synergy.getManifest().name();
        	if (!Configuration.Settings.isBlacklisted(name) && synergy.getAbilityClass().getAnnotation(Beta.class) == null) {
        		synergies.add(synergy);
        	}
        }
		
        Random r = new Random();
        return synergies.toArray(new AbilityRegistration[]{})[r.nextInt(synergies.size())];
    }
	
	private AbilityBase[] abilities() {
		return new AbilityBase[] { ability1, ability2 };
	}
	
	@Override
	public boolean ActiveSkill(Material material, ClickType clickType) {
		boolean activated = false;

		if (ability1 instanceof ActiveHandler) {
			activated |= ((ActiveHandler) ability1).ActiveSkill(material, clickType);
		}

		if (ability2 instanceof ActiveHandler) {
			activated |= ((ActiveHandler) ability2).ActiveSkill(material, clickType);
		}

		return activated;
	}
	
	@Override
	public void TargetSkill(Material material, LivingEntity entity) {
		for (AbilityBase ability : abilities()) {
			if (ability instanceof TargetHandler) {
				((TargetHandler) ability).TargetSkill(material, entity);
			}
		}
	}
	
	@Override
	public Set<GameTimer> getTimers() {
		Set<GameTimer> timers = super.getTimers();

		for (AbilityBase ability : abilities()) {
			if (ability != null) {
				timers = SetUnion.union(timers, ability.getTimers());
			}
		}

		return timers;
	}

	@Override
	public Set<GameTimer> getRunningTimers() {
		Set<GameTimer> timers = super.getRunningTimers();

		for (AbilityBase ability : abilities()) {
			if (ability != null) {
				timers = SetUnion.union(timers, ability.getRunningTimers());
			}
		}

		return timers;
	}

	@Override
	public boolean usesMaterial(Material material) {
		for (AbilityBase ability : abilities()) {
			if (ability != null && ability.usesMaterial(material)) {
				return true;
			}
		}

		return super.usesMaterial(material);
	}
	
	@Override
	protected void onUpdate(Update update) {
		if (update == Update.RESTRICTION_CLEAR) {
			if (ability1 == null) {
				try {
					this.ability1 = AbilityBase.create(random.nextInt(1000) < chance ? getRandomSynergy() : getRandomRankAbility(getRandomRank()), getParticipant());
					this.ability1.setRestricted(false);
				} catch (ReflectiveOperationException e) {
					e.printStackTrace();
				}
			}
			if (ability2 == null) {
				try {
					this.ability2 = AbilityBase.create(random.nextInt(1000) < chance ? getRandomSynergy() : getRandomRankAbility(getRandomRank()), getParticipant());
					this.ability2.setRestricted(false);
				} catch (ReflectiveOperationException e) {
					e.printStackTrace();
				}
			}	
		}
		
		if (update == Update.ABILITY_DESTROY) {
			if (ability1 != null && ability2 != null) Bukkit.broadcastMessage("§8[§7Unknown§8] §b능력은 " + RankColor.getColor(ability1.getRank()) + ability1.getDisplayName() + "§7, " + RankColor.getColor(ability2.getRank()) + ability2.getDisplayName() + "§b이었습니다.");
		}
		
		for (AbilityBase ability : abilities()) {
			if (ability == null) continue;

			switch (update) {
			case RESTRICTION_CLEAR:
				ability.setRestricted(false);
				break;
			case RESTRICTION_SET:
				ability.setRestricted(true);
				break;
			case ABILITY_DESTROY:
				ability.destroy();
				break;
			}
		}

		if (update == Update.ABILITY_DESTROY) {
			ability1 = null;
			ability2 = null;
		}
	}

}
