package RainStarSynergy;

import java.util.ArrayList;
import java.util.Set;

import javax.annotation.Nullable;

import org.bukkit.Bukkit;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByBlockEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;

import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.SubscribeEvent;
import daybreak.abilitywar.ability.AbilityBase;
import daybreak.abilitywar.ability.AbilityFactory.AbilityRegistration;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.config.Configuration;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.list.mix.Mix;
import daybreak.abilitywar.game.list.mix.synergy.Synergy;
import daybreak.abilitywar.game.list.mix.synergy.SynergyFactory;
import daybreak.abilitywar.game.manager.AbilityList;
import daybreak.abilitywar.game.module.DeathManager;
import daybreak.abilitywar.utils.base.random.Random;
import daybreak.abilitywar.utils.library.SoundLib;
import daybreak.google.common.base.Predicate;

@AbilityManifest(name = "기회", rank = Rank.L, species = Species.GOD, explain = {
		"치명적인 피해를 입었을 때 모든 대상의 능력을",
		"강제로 재추첨하고 절반의 체력으로 부활합니다.",
		"만일 대상의 능력이 시너지라면 다른 시너지 능력으로 교체됩니다."})

public class Chance extends Synergy {
	
	public Chance(Participant participant) {
		super(participant);
	}
	
	private final Predicate<Entity> predicate = new Predicate<Entity>() {
		@Override
		public boolean test(Entity entity) {
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
	
    public Class<? extends AbilityBase> getRandomAbility() {

        ArrayList<Class<? extends AbilityBase>> abilities = new ArrayList<>();
        for (String name : AbilityList.nameValues()) {
            if (!Configuration.Settings.isBlacklisted(name)) {
                abilities.add(AbilityList.getByString(name));
            }
        }
        for (Participant participant : getGame().getParticipants()) {
            if (participant.hasAbility() && participant.attributes().TARGETABLE.getValue()) {
                abilities.remove(participant.getAbility().getClass());
            }
        }

        Random r = new Random();
        return abilities.get(r.nextInt(abilities.size()));

    }

    public AbilityRegistration getRandomSynergy() {

		Set<AbilityRegistration> synergies = SynergyFactory.getSynergies();

        Random r = new Random();
        return synergies.toArray(new AbilityRegistration[]{})[r.nextInt(synergies.size())];
    }
    
	@SubscribeEvent
	public void onEntityDamageByEntity(EntityDamageByEntityEvent e) {
		onEntityDamage(e);
	}
	
	@SubscribeEvent
	public void onEntityDamageByBlock(EntityDamageByBlockEvent e) {
		onEntityDamage(e);
	}
    
	@SubscribeEvent
	public void onEntityDamage(EntityDamageEvent e) {
		if (e.getEntity().equals(getPlayer()) && getPlayer().getHealth() - e.getFinalDamage() <= 0) {
			getPlayer().setHealth(getPlayer().getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue() / 2);
			Bukkit.broadcastMessage("§e" + getPlayer().getName() + "§f로 인해 모든 플레이어에게 §b기회§f가 다시금 주어집니다...");
			SoundLib.UI_TOAST_CHALLENGE_COMPLETE.broadcastSound();
			e.setCancelled(true);
			getGame().getParticipants().forEach(participant -> {
					try {
						Mix mix = (Mix) participant.getAbility();
						if (predicate.test(participant.getPlayer())) {
							if (mix.hasSynergy()) {
								AbilityRegistration synergy = getRandomSynergy();
								mix.setAbility(SynergyFactory.getSynergyBase(synergy).getLeft().getAbilityClass(), SynergyFactory.getSynergyBase(synergy).getRight().getAbilityClass());
							} else {
								mix.setAbility(getRandomAbility(), getRandomAbility());
							}	
						}
					} catch (ReflectiveOperationException e1) {
						e1.printStackTrace();
					}
			});
		}
	}
	
}
