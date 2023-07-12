package rainstar.abilitywar.ability;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.StringJoiner;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.event.entity.EntityDamageByBlockEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.entity.EntityRegainHealthEvent.RegainReason;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;

import daybreak.abilitywar.ability.AbilityBase;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.SubscribeEvent;
import daybreak.abilitywar.ability.event.AbilityPreActiveSkillEvent;
import daybreak.abilitywar.ability.list.virus.VirusInfectionEvent;
import daybreak.abilitywar.config.ability.AbilitySettings.SettingObject;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.game.AbstractGame.Effect;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.manager.effect.event.ParticipantPreEffectApplyEvent;
import daybreak.abilitywar.game.manager.effect.registry.EffectRegistry.EffectRegistration;
import daybreak.abilitywar.utils.base.minecraft.entity.health.Healths;
import daybreak.abilitywar.utils.library.ParticleLib;
import rainstar.abilitywar.utils.Healing;

@AbilityManifest(name = "백신", rank = Rank.A, species = Species.HUMAN, explain = {
		"§7패시브 §8- §a면역§f: §e바이러스 능력§f에게 감염되지 않습니다.",
		" 공허, 압사, 생명체와 발사체에 의한 피해 외의 피해를 입지 않습니다.",
		"§7철괴 F키 §8- §b백신§f: 가진 §d상태이상§f과 체력을 전부 치료합니다. §8(§71회용§8)",
		" 이 효과로 제거한 §d상태이상§f에 영구적으로 면역 및 체력을 $[HEAL_AMOUNT] 회복합니다."
		},
		summarize = {
		"§e바이러스 능력§f에게 감염되지 않습니다.",
		"공허, 압사, 생명체와 발사체에 의한 피해 외의 피해를 입지 않습니다.",
		"§7철괴 F키§f로 단 한 번 가진 상태이상을 전부 제거하고 체력을 전부 회복합니다.",
		"스킬로 지운 상태이상을 영원히 받지 않고 체력을 $[HEAL_AMOUNT] 회복합니다."
		})
public class Vaccine extends AbilityBase {
	
	public Vaccine(Participant participant) {
		super(participant);
	}
	
	public static final SettingObject<Double> HEAL_AMOUNT = 
			abilitySettings.new SettingObject<Double>(Vaccine.class, "heal-amount", 1.5,
            "# 상태이상 무효화 시 회복하는 체력", "# 단위: 값(1 = 0.5칸)") {
        @Override
        public boolean condition(Double value) {
            return value >= 0;
        }
    };
	
    private final double healamount = HEAL_AMOUNT.getValue();
	private boolean onetime = true;
	private Set<EffectRegistration<?>> effects = new HashSet<>();
	
	@SubscribeEvent(onlyRelevant = true)
	public void onParticipantEffectApply(ParticipantPreEffectApplyEvent e) {
		if (effects.contains(e.getEffectType())) {
			getPlayer().sendMessage("§a[§d!§a] §f" + e.getEffectType().getManifest().displayName() + "§f 면역.");
			ParticleLib.VILLAGER_HAPPY.spawnParticle(getPlayer().getLocation(), 0.2, 1, 0.2, 10, 1);
			final EntityRegainHealthEvent event = new EntityRegainHealthEvent(getPlayer(), healamount, RegainReason.CUSTOM);
			Bukkit.getPluginManager().callEvent(event);
			if (!event.isCancelled()) {
				Healths.setHealth(getPlayer(), getPlayer().getHealth() + event.getAmount());	
			}
			e.setCancelled(true);
		}
	}
	
    @SubscribeEvent(onlyRelevant = true)
    public void onPlayerSwapHandItems(PlayerSwapHandItemsEvent e) {
    	if (e.getOffHandItem().getType().equals(Material.IRON_INGOT) && e.getPlayer().equals(getPlayer()) && onetime) {
    		final AbilityPreActiveSkillEvent event = new AbilityPreActiveSkillEvent(this, Material.IRON_INGOT, null);
    		Bukkit.getPluginManager().callEvent(event);
    		if (!event.isCancelled()) {
        		onetime = false;
        		Collection<Effect> nowEffects = getParticipant().getEffects();
        		if (nowEffects.size() >= 1) {
            		final StringJoiner joiner = new StringJoiner("§f, ");
            		for (Effect effect : nowEffects) {
            			effects.add(effect.getRegistration());
            			joiner.add(effect.getRegistration().getManifest().displayName());
            		}
            		getPlayer().sendMessage("§d[§a!§d] §f" + joiner.toString() + "§f의 효과를 치료하였습니다.");
        		}
    			double maxHealth = getPlayer().getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue();
    			Healing.heal(getPlayer(), maxHealth - getPlayer().getHealth(), RegainReason.CUSTOM);
        		getParticipant().removeEffects();
        	}	
    		e.setCancelled(true);
    	}
    }
    
    @SubscribeEvent
    public void onInfect(VirusInfectionEvent e) {
    	Bukkit.broadcastMessage("e.getParticipant() " + e.getParticipant().getPlayer().getName());
    	if (e.getParticipant().equals(getParticipant())) e.setCancelled(true);
    }

	@SubscribeEvent(onlyRelevant = true)
	public void onEntityDamage(EntityDamageEvent e) {
		if (!e.getCause().equals(DamageCause.PROJECTILE) && !e.getCause().equals(DamageCause.ENTITY_ATTACK) && !e.getCause().equals(DamageCause.ENTITY_EXPLOSION) && !e.getCause().equals(DamageCause.ENTITY_SWEEP_ATTACK) && !e.getCause().equals(DamageCause.VOID) && !e.getCause().equals(DamageCause.SUFFOCATION)) e.setCancelled(true);
	}
	
	@SubscribeEvent(onlyRelevant = true)
	public void onEntityDamageByBlock(EntityDamageByBlockEvent e) {
		onEntityDamage(e);
	}

}