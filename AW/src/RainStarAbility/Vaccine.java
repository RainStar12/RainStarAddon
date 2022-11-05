package RainStarAbility;

import java.util.HashSet;
import java.util.Set;
import java.util.StringJoiner;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.entity.EntityDamageByBlockEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;

import daybreak.abilitywar.ability.AbilityBase;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.SubscribeEvent;
import daybreak.abilitywar.ability.list.Virus;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.game.AbstractGame.Effect;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.list.mix.Mix;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;

@AbilityManifest(name = "백신", rank = Rank.A, species = Species.HUMAN, explain = {
		"§7패시브 §8- §a면역§f: §e바이러스 능력§f에게 감염되지 않습니다.",
		" 공허, 압사, 생명체와 발사체에 의한 피해 외의 피해를 입지 않습니다.",
		"§7철괴 F키 §8- §b백신§f: 가진 §d상태이상§f을 전부 제거합니다. §8(§71회용§8)",
		" 이 효과로 제거한 §d상태이상§f에 영구적으로 면역을 가집니다."
		},
		summarize = {
		"§e바이러스 능력§f에게 감염되지 않습니다.",
		"공허, 압사, 생명체와 발사체에 의한 피해 외의 피해를 입지 않습니다.",
		"§7철괴 F키§f로 단 한 번 가진 상태이상을 전부 제거합니다.",
		"스킬로 지운 상태이상을 영원히 받지 않습니다."
		})
public class Vaccine extends AbilityBase {
	
	public Vaccine(Participant participant) {
		super(participant);
	}
	
	private boolean onetime = true;
	private Set<Effect> effects;
	
    @SubscribeEvent(onlyRelevant = true)
    public void onPlayerSwapHandItems(PlayerSwapHandItemsEvent e) {
    	if (e.getOffHandItem().getType().equals(Material.IRON_INGOT) && e.getPlayer().equals(getPlayer()) && onetime) {
    		effects = new HashSet<>(getParticipant().getEffects());
    		final StringJoiner joiner = new StringJoiner("§f, ");
    		for (Effect effect : effects) {
    			
    		}
    		getPlayer().sendMessage("");
    		getParticipant().removeEffects();
    		e.setCancelled(true);
    	}
    }
	
	@SubscribeEvent(priority = 1001)
	public void onEntityDamageByEntity(EntityDamageByEntityEvent e) {
		Player damager = null;
		if (e.getDamager() instanceof Projectile) {
			Projectile projectile = (Projectile) e.getDamager();
			if (projectile.getShooter() instanceof Player) damager = (Player) projectile.getShooter();
		} else if (e.getDamager() instanceof Player) damager = (Player) e.getDamager();
		
		if (getPlayer().equals(damager) && e.getEntity() instanceof Player) {
			Player player = (Player) e.getEntity();
			if (getGame().isParticipating(player) && player.getHealth() - e.getFinalDamage() <= 0) {
				AbilityBase ab = getGame().getParticipant(player).getAbility();
				if (ab.getClass().equals(Mix.class)) {
					Mix mix = (Mix) ab;
					if (mix.getFirst().getClass().equals(Virus.class)) {
						new AbilityTimer(10) {
							
							@Override
							public void onStart() {
								mix.getFirst().setRestricted(true);
							}
							
							@Override
							public void onEnd() {
								onSilentEnd();
							}
							
							@Override
							public void onSilentEnd() {
								mix.getFirst().setRestricted(false);
							}
							
						}.setPeriod(TimeUnit.TICKS, 1).start();
					}
					if (mix.getSecond().getClass().equals(Virus.class)) {
						new AbilityTimer(10) {
							
							@Override
							public void onStart() {
								mix.getSecond().setRestricted(true);
							}
							
							@Override
							public void onEnd() {
								onSilentEnd();
							}
							
							@Override
							public void onSilentEnd() {
								mix.getSecond().setRestricted(false);
							}
							
						}.setPeriod(TimeUnit.TICKS, 1).start();
					}
				} else if (ab.getClass().equals(Virus.class)) {
					new AbilityTimer(10) {
						
						@Override
						public void onStart() {
							ab.setRestricted(true);
						}
						
						@Override
						public void onEnd() {
							onSilentEnd();
						}
						
						@Override
						public void onSilentEnd() {
							ab.setRestricted(false);
						}
						
					}.setPeriod(TimeUnit.TICKS, 1).start();
				}
			}
			
		}
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