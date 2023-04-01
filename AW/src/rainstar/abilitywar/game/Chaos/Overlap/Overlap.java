package rainstar.abilitywar.game.Chaos.Overlap;

import daybreak.abilitywar.ability.AbilityBase;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.ability.decorator.ActiveHandler;
import daybreak.abilitywar.ability.decorator.TargetHandler;
import daybreak.abilitywar.game.AbstractGame.GameTimer;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.utils.base.collect.SetUnion;
import rainstar.abilitywar.utils.RankColor;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.LivingEntity;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.StringJoiner;

import javax.annotation.Nonnull;

@AbilityManifest(name = "중첩", rank = Rank.SPECIAL, species = Species.OTHERS, explain = "$(EXPLAIN)")
public class Overlap extends AbilityBase implements ActiveHandler, TargetHandler {

	private List<AbilityBase> abilities = new ArrayList<>();

	@SuppressWarnings("unused")
	private final Object EXPLAIN = new Object() {
		@Override
		public String toString() {
			final StringJoiner joiner = new StringJoiner("\n");
			for (AbilityBase ability : abilities) {
				joiner.add("§a---------------------------------");
				formatInfo(joiner, ability);
			}
			return joiner.toString();
		}

		private void formatInfo(final StringJoiner joiner, final AbilityBase ability) {
			if (ability != null) {
				joiner.add("§b" + ability.getName() + " §f[" + (ability.isRestricted() ? "§7능력 비활성화됨" : "§a능력 활성화됨") + "§f] " + ability.getRank().getRankName() + " " + ability.getSpecies().getSpeciesName());
				for (final Iterator<String> iterator = ability.getExplanation(); iterator.hasNext(); ) {
					joiner.add(ChatColor.RESET + iterator.next());
				}
			} else {
				joiner.add("§f능력이 없습니다.");
			}
		}
	};

	public Overlap(Participant participant) {
		super(participant);
	}

	public List<AbilityBase> getAbilities() {
		return abilities;
	}
	
	@Override
	public String getDisplayName() {
		String name;
		if (abilities.size() > 0) {
			final StringJoiner joiner = new StringJoiner(" §f+ ");
			for (AbilityBase ability : abilities) {
				joiner.add(RankColor.getColor(ability.getRank()) + ability.getDisplayName());
			}
			name = joiner.toString();
		} else {
			name = "중첩";
		}
		return name;
	}

	@Override
	public boolean usesMaterial(Material material) {
		for (AbilityBase ability : abilities) {
			if (ability != null) {
				return ability.usesMaterial(material);
			}	
		}
		return super.usesMaterial(material);
	}

	@Override
	public Set<GameTimer> getTimers() {
		Set<GameTimer> timers = new HashSet<>();
		for (AbilityBase ability : abilities) {
			timers.addAll(SetUnion.union(super.getTimers(), ability.getTimers()));	
		}
		return timers;
	}

	@Override
	public Set<GameTimer> getRunningTimers() {
		Set<GameTimer> timers = new HashSet<>();
		for (AbilityBase ability : abilities) {
			timers.addAll(SetUnion.union(super.getRunningTimers(), ability.getRunningTimers()));	
		}
		return timers;
	}
	
	public boolean hasAbility() {
		return !abilities.isEmpty();
	}

	public void removeAbility() {
		if (hasAbility()) {
			for (AbilityBase ability : abilities) {
				ability.destroy();
			}
			abilities.clear();
		}
	}

	@Override
	public boolean ActiveSkill(@Nonnull Material material, @Nonnull AbilityBase.ClickType clickType) {
		if (hasAbility()) {
			boolean actived = false;
			boolean dontchange = false;
			for (AbilityBase ability : abilities) {
				 if (ability instanceof ActiveHandler) {
					 if (actived == true) dontchange = true;
					 actived = ((ActiveHandler) ability).ActiveSkill(material, clickType);
				 }
			}
			if (dontchange == true) actived = true;
			if (actived) return true;
		}
		return false;
	}

	@Override
	public void TargetSkill(@Nonnull Material material, @Nonnull LivingEntity entity) {
		if (hasAbility()) {
			for (AbilityBase ability : abilities) {
				if (ability instanceof TargetHandler) {
					((TargetHandler) ability).TargetSkill(material, entity);
				}	
			}
		}
	}

	@Override
	protected void onUpdate(Update update) {
		if (update == Update.RESTRICTION_CLEAR) {
			if (!abilities.isEmpty()) {
				for (AbilityBase ability : abilities) {
					ability.setRestricted(false);
				}
			}
		} else if (update == Update.RESTRICTION_SET) {
			if (!abilities.isEmpty()) {
				for (AbilityBase ability : abilities) {
					ability.setRestricted(true);
				}
			}
		} else if (update == Update.ABILITY_DESTROY) {
			if (!abilities.isEmpty()) {
				for (AbilityBase ability : abilities) {
					ability.destroy();
				}
				abilities.clear();
			}
		}
	}
}