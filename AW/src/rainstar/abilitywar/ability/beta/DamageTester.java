package rainstar.abilitywar.ability.beta;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Damageable;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByBlockEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;

import daybreak.abilitywar.ability.AbilityBase;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.SubscribeEvent;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.ability.decorator.ActiveHandler;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.AbstractGame.Participant.ActionbarNotification.ActionbarChannel;
import daybreak.abilitywar.game.module.DeathManager;
import daybreak.abilitywar.game.team.interfaces.Teamable;
import daybreak.abilitywar.utils.annotations.Beta;
import daybreak.abilitywar.utils.base.math.LocationUtil;
import daybreak.abilitywar.utils.base.minecraft.damage.Damages;
import daybreak.google.common.base.Predicate;
import rainstar.abilitywar.system.killreward.KillRewardGUI.Type;

@Beta

@AbilityManifest(name = "대미지테스터", rank = Rank.A, species = Species.OTHERS, explain = {
		"철괴 좌클릭 시 대미지의 종류를 정할 수 있음",
		"아무거나 F키시 해당 종류의 피해 5를 가장 가까운 대상에게 입힘",
		"이때 실제 피해량이 어땠는지 출력됨"
		})
public class DamageTester extends AbilityBase implements ActiveHandler {

	public DamageTester(Participant participant) {
		super(participant);
	}

	enum DamageType {
		DAMAGES_ARROW("§bDamages.damageArrow()") {
			protected void onAttack(Player attacker, Damageable damageable) {
				Damages.damageArrow(damageable, attacker, 5);
			}
    		@Override
    		public DamageType next() {
    			return DAMAGES_EXPLOSION;
    		}
		},
		DAMAGES_EXPLOSION("§cDamages.damageExplosion()") {
			protected void onAttack(Player attacker, Damageable damageable) {
				Damages.damageExplosion(damageable, attacker, 5);
			}
    		@Override
    		public DamageType next() {
    			return DAMAGES_FIXED;
    		}
		},
		DAMAGES_FIXED("§3Damages.damageFixed()") {
			protected void onAttack(Player attacker, Damageable damageable) {
				Damages.damageFixed(damageable, attacker, 5);
			}
    		@Override
    		public DamageType next() {
    			return DAMAGES_MAGIC_IGNOREARMOR;
    		}
		},
		DAMAGES_MAGIC_IGNOREARMOR("§5Damages.damageMagic() <방관>") {
			protected void onAttack(Player attacker, Damageable damageable) {
				Damages.damageMagic(damageable, attacker, true, 5);
			}
    		@Override
    		public DamageType next() {
    			return DAMAGES_MAGIC;
    		}
		},
		DAMAGES_MAGIC("§dDamages.damageMagic() <비방관>") {
			protected void onAttack(Player attacker, Damageable damageable) {
				Damages.damageMagic(damageable, attacker, false, 5);
			}
    		@Override
    		public DamageType next() {
    			return DAMAGES_THORN;
    		}
		},
		DAMAGES_THORN("§aDamages.damageThorn()") {
			protected void onAttack(Player attacker, Damageable damageable) {
				Damages.damageThorn(damageable, attacker, 5);
			}
    		@Override
    		public DamageType next() {
    			return TARGET_DAMAGE_NOATTACKER;
    		}
		},
		TARGET_DAMAGE_NOATTACKER("§6객체.damage() <공격자 없음>") {
			protected void onAttack(Player attacker, Damageable damageable) {
				damageable.damage(5);
			}
    		@Override
    		public DamageType next() {
    			return TARGET_DAMAGE;
    		}
		},
		TARGET_DAMAGE("§e객체.damage() <공격자 있음>") {
			protected void onAttack(Player attacker, Damageable damageable) {
				damageable.damage(5, attacker);
			}
    		@Override
    		public DamageType next() {
    			return DAMAGES_ARROW;
    		}
		};
		
		private final String actionbar;
		
		DamageType(String actionbar) {
			this.actionbar = actionbar;
		}
		
		protected abstract void onAttack(Player attacker, Damageable damageable);
		
		public String actionbar() {
			return actionbar;
		}
		
		public abstract DamageType next();
		
	}
	
	private DamageType damagetype = DamageType.DAMAGES_ARROW;
	private ActionbarChannel ac = newActionbarChannel();
	private List<Damageable> damageables = new ArrayList<>();
	
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
			return true;
		}

		@Override
		public boolean apply(@Nullable Entity arg0) {
			return false;
		}
	};

	@SubscribeEvent(priority = 999999999)
	public void onEntityDamage(EntityDamageEvent e) {
		if (damageables.contains(e.getEntity())) {
			Bukkit.broadcastMessage("§b피해 타입: " + e.getCause());
			Bukkit.broadcastMessage("§c피해량 결과: " + e.getFinalDamage());
			damageables.remove(e.getEntity());
		}
	}
	
	@SubscribeEvent(priority = 999999999)
	public void onEntityDamageByBlock(EntityDamageByBlockEvent e) {
		if (damageables.contains(e.getEntity())) {
			Bukkit.broadcastMessage("§b피해 타입: " + e.getCause());
			Bukkit.broadcastMessage("§c피해량 결과: " + e.getFinalDamage());
			damageables.remove(e.getEntity());
		}
	}
	
	@SubscribeEvent(priority = 999999999)
	public void onEntityDamageByEntity(EntityDamageByEntityEvent e) {
		if (damageables.contains(e.getEntity())) {
			Bukkit.broadcastMessage("§b피해 타입: " + e.getCause());
			Bukkit.broadcastMessage("§c피해량 결과: " + e.getFinalDamage());
			damageables.remove(e.getEntity());
		}
	}
	
	@SubscribeEvent(onlyRelevant = true)
	private void onPlayerSwapHandItems(PlayerSwapHandItemsEvent e) {
		if (LocationUtil.getNearestEntity(Damageable.class, getPlayer().getLocation(), predicate) != null) {
			Damageable damageable = LocationUtil.getNearestEntity(Damageable.class, getPlayer().getLocation(), predicate);
			damageables.add(damageable);
			damagetype.onAttack(getPlayer(), damageable);
			e.setCancelled(true);
		}
	}
	
	public boolean ActiveSkill(Material material, ClickType clicktype) {
		if (material == Material.IRON_INGOT && clicktype == ClickType.LEFT_CLICK) {
			damagetype = damagetype.next();
			ac.update(damagetype.actionbar());
			return true;
		}
		return false;
	}
	
}
