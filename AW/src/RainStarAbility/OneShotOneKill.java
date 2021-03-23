package RainStarAbility;

import java.util.HashMap;
import java.util.Map;

import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;

import daybreak.abilitywar.ability.AbilityBase;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.SubscribeEvent;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.base.minecraft.entity.health.Healths;
import daybreak.abilitywar.utils.library.MaterialX;
import daybreak.abilitywar.utils.library.ParticleLib;
import daybreak.abilitywar.utils.library.SoundLib;

@AbilityManifest(name = "일격필살", rank = Rank.A, species = Species.HUMAN, explain = {
		"§7패시브 §8- §c한 방에 간다§f: 단 한 번도 근접 타격한 적 없는 적을 처음으로",
		" 근접 타격할 때 2.5배의 피해로 입힙니다.",
		" 이후 기본 피해량으로만 공격하고, 대상을 타격하면 타격할수록 대상에게 주는",
		" 기본 피해량이 계속해 감소합니다.",
		"§7패시브 §8- §c원샷원킬§f: 단 한 번도 타격한 적 없는 적을 한 번에 처치할 경우",
		" 모든 적의 타격 카운팅이 초기화되며, 체력을 입힌 피해량만큼 회복합니다."
		},
		summarize = {
		"단 한 번도 근접 타격한 적 없는 적을 근접 타격시 2.5배의 피해를 입힙니다.",
		"이 효과로 적을 처치시 모든 적의 타격 카운팅이 리셋, 체력을 회복합니다.",
		"타격한 적이 있는 적에겐 기본 피해량이 계속해 감소합니다."
		})

public class OneShotOneKill extends AbilityBase {

	public OneShotOneKill(Participant participant) {
		super(participant);
	}
	
	private Map<Player, Integer> attackCounter = new HashMap<>();
	private double lastDamage = 0;
	private Player dead;
	
	@SubscribeEvent
	public void onEntityDamageByEntity(EntityDamageByEntityEvent e) {
		if (e.getDamager().equals(getPlayer()) && !e.isCancelled()) {
			if (e.getEntity() instanceof Player) {
				Player player = (Player) e.getEntity();
				if (!attackCounter.containsKey(player)) {
					new AbilityTimer(5) {
						
						@Override
						public void run(int count) {
							SoundLib.ENTITY_PLAYER_ATTACK_STRONG.playSound(getPlayer(), 1, 1.15f);
						}
						
					}.setPeriod(TimeUnit.TICKS, 1).start();
					SoundLib.ENTITY_POLAR_BEAR_WARNING.playSound(getPlayer().getLocation());
					ParticleLib.CRIT.spawnParticle(player.getLocation().add(0, 1, 0), .3f, .3f, .3f, 100, 1);
					e.setDamage(e.getDamage() * 2.5);
					attackCounter.put(player, 1);
					if (player.getHealth() - e.getFinalDamage() <= 0) {
						new AbilityTimer(2) {
							
							@Override
							public void onStart() {
								dead = player;
								lastDamage = e.getFinalDamage();
							}
							
							@Override
							public void onEnd() {
								onSilentEnd();
							}
							
							@Override
							public void onSilentEnd() {
								dead = null;
								lastDamage = 0;
							}
							
						}.setPeriod(TimeUnit.TICKS, 1).start();
					}
				} else {
					e.setDamage(Math.max(e.getDamage() / 3, e.getDamage() - (attackCounter.get(player) * 0.3)));
					attackCounter.put(player, attackCounter.get(player) + 1);
				}
			}
		}
	}
	
	@SubscribeEvent
	public void onPlayerDeath(PlayerDeathEvent e) {
		if (dead != null) {
			if (e.getEntity().equals(dead)) {
				attackCounter.clear();
				Healths.setHealth(getPlayer(), getPlayer().getHealth() + lastDamage);
				SoundLib.ENTITY_ZOMBIE_VILLAGER_CURE.playSound(getPlayer().getLocation(), 1, 0.75f);
				ParticleLib.ITEM_CRACK.spawnParticle(dead.getEyeLocation(), .3f, .3f, .3f, 100, 0.5, MaterialX.REDSTONE);
			}
		}
	}
	
}
