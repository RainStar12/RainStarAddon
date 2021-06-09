package RainStarAbility;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.StringJoiner;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;

import daybreak.abilitywar.ability.AbilityBase;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.SubscribeEvent;
import daybreak.abilitywar.ability.AbilityFactory.AbilityRegistration;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.ability.decorator.ActiveHandler;
import daybreak.abilitywar.ability.decorator.TargetHandler;
import daybreak.abilitywar.config.Configuration;
import daybreak.abilitywar.game.AbstractGame.GameTimer;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.manager.AbilityList;
import daybreak.abilitywar.utils.base.collect.SetUnion;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.base.random.Random;
import daybreak.google.common.collect.ImmutableMap;

@AbilityManifest(
		name = "어비스", rank = Rank.L, species = Species.OTHERS, explain = {
		"누군가를 죽일 때 대상이 갖고 있던 능력 중 하나를 가져옵니다.",
		"가져온 능력은 심연의 능력이 되어 ",
		"철괴를 들고 F키를 누르면 심연 속에서 능력 하나를 최대 10개까지 가져옵니다.",
		"철괴 우클릭 시, 가진 모든 심연의 능력을 사용합니다. $[COOLDOWN]",
		"심연의 능력은 생명체에게 $[COUNT]번 피해입을 때마다 사라집니다.",
		"심연의 능력 하나당 받는 피해량이 1.5씩 증가합니다.",
		"사망 시, 나를 죽인 대상은 가지고 있던 능력을 포함한 이 능력이 됩니다.",
		"§8=============== §7보유 능력 §8===============",
		"$(EXPLAIN)"
		})

public class Abyss extends AbilityBase implements ActiveHandler, TargetHandler {

	public Abyss(Participant participant) {
		super(participant);
	}
	
	private List<AbilityBase> abilities = new ArrayList<>();
    Random random = new Random();
	
	private static final ImmutableMap<Rank, String> rankcolor = ImmutableMap.<Rank, String>builder()
			.put(Rank.C, "§e")
			.put(Rank.B, "§b")
			.put(Rank.A, "§a")
			.put(Rank.S, "§d")
			.put(Rank.L, "§6")
			.put(Rank.SPECIAL, "§c")
			.build();
	
	public AbilityRegistration getRandomAbility() {
		
		Set<AbilityRegistration> myAbilities = new HashSet<>();
		
        for (AbilityBase ab : abilities) {
        	myAbilities.add(ab.getRegistration());
        }
		
		final List<AbilityRegistration> registrations = AbilityList.values().stream().filter(
				ability -> !Configuration.Settings.isBlacklisted(ability.getManifest().name()) && !myAbilities.contains(ability)
				&& !ability.getManifest().name().equals("어비스")
		).collect(Collectors.toList());
		return registrations.isEmpty() ? null : random.pick(registrations);
	}
	
	private final AbilityTimer cool = new AbilityTimer(10) {
		
		@Override
		public void run(int count) {
		}
		
    }.setPeriod(TimeUnit.TICKS, 1).register();

    @SubscribeEvent(onlyRelevant = true)
    public void onPlayerSwapHandItems(PlayerSwapHandItemsEvent e) {
    	if (e.getOffHandItem().getType().equals(Material.IRON_INGOT)) {
    		if (!cool.isRunning()) {
				try {
					abilities.add(AbilityBase.create(getRandomAbility(), getParticipant()));
				} catch (ReflectiveOperationException e1) {
					e1.printStackTrace();
				}
    		}
    		e.setCancelled(true);
    	}
    }
	
	@SuppressWarnings("unused")
	private final Object EXPLAIN = new Object() {
		@Override
		public String toString() {
			if (abilities != null) {
				if (abilities.size() == 0) {
					return "능력이 없습니다.".toString();
				} else {
					final StringJoiner joiner = new StringJoiner("§7, ");
					for (AbilityBase ab : abilities) {
						joiner.add(rankcolor.get(ab.getRank()) + ab.getName());
					}
					return joiner.toString();
				}	
			} else {
				return "능력이 없습니다.".toString();
			}
		}
	};

	@Override
	public boolean ActiveSkill(Material material, ClickType clickType) {
		if (!abilities.isEmpty()) {
			for (AbilityBase ability : abilities) {
				if (ability instanceof ActiveHandler) {
					ActiveHandler active = (ActiveHandler) ability;
					
					((ActiveHandler) ability).ActiveSkill(material, clickType);	
				}
			}
			return true;
		}
		return false;
	}

	@Override
	public void TargetSkill(Material material, LivingEntity entity) {
		if (!abilities.isEmpty()) {
			for (AbilityBase ability : abilities) {
				if (ability instanceof TargetHandler) {
					((TargetHandler) ability).TargetSkill(material, entity);
				}	
			}
		}
	}

	@Override
	public Set<GameTimer> getTimers() {
		if (!abilities.isEmpty()) {
			Set<GameTimer> timers = super.getTimers();
			for (AbilityBase ability : abilities) {
				if (ability != null) {
					timers = SetUnion.union(timers, ability.getTimers());
				}
			}
			return timers;
		}
		return super.getTimers();
	}

	@Override
	public Set<GameTimer> getRunningTimers() {
		if (!abilities.isEmpty()) {
			Set<GameTimer> timers = super.getRunningTimers();
			for (AbilityBase ability : abilities) {
				if (ability != null) {
					timers = SetUnion.union(timers, ability.getRunningTimers());
				}
			}
			return timers;
		}
		return super.getRunningTimers();
	}

	@Override
	public boolean usesMaterial(Material material) {
		if (!abilities.isEmpty()) {
			for (AbilityBase ability : abilities) {
				if (ability != null) {
					return ability.usesMaterial(material);
				}
			}
		}
		return super.usesMaterial(material);
	}

	@Override
	protected void onUpdate(Update update) {
		if (update == Update.RESTRICTION_CLEAR) {
			if (abilities != null) {
				if (!abilities.isEmpty()) {
					for (AbilityBase ability : abilities) {
						ability.setRestricted(false);		
					}
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
				abilities.clear();
			}
		}
	}
	
}
