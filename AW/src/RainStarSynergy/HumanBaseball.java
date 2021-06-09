package RainStarSynergy;

import java.util.HashSet;
import java.util.Set;

import javax.annotation.Nullable;

import org.bukkit.Material;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Damageable;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.util.Vector;

import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.ability.SubscribeEvent;
import daybreak.abilitywar.ability.decorator.ActiveHandler;
import daybreak.abilitywar.config.ability.AbilitySettings.SettingObject;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.list.mix.synergy.Synergy;
import daybreak.abilitywar.game.module.DeathManager;
import daybreak.abilitywar.game.team.interfaces.Teamable;
import daybreak.abilitywar.utils.base.Formatter;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.base.math.LocationUtil;
import daybreak.abilitywar.utils.base.math.VectorUtil;
import daybreak.abilitywar.utils.base.minecraft.damage.Damages;
import daybreak.abilitywar.utils.library.SoundLib;
import daybreak.google.common.base.Predicate;

@AbilityManifest(
		name = "인간 야구", rank = Rank.S, species = Species.HUMAN, explain = {
		"철괴 우클릭 시 §c최근에 나를 때린 대상§f이 경직된 채 떠오릅니다. $[COOLDOWN]",
		"이때 대상에게 검을 §b휘두르면§f 해당 방향으로 멀리 날아갑니다. §3깡!§f",
		"이 능력은 기본 근접 공격으로도 약하게 발동합니다.",
		"대상이 착지하기 전까지 스쳐 지나가는 모든 생명체들은 피해를 입고,",
		"대상도 같이 피해를 입습니다."
		})

public class HumanBaseball extends Synergy implements ActiveHandler {

	public HumanBaseball(Participant participant) {
		super(participant);
	}
	
	public static final SettingObject<Integer> COOLDOWN 
	= synergySettings.new SettingObject<Integer>(HumanBaseball.class,
			"cooldown", 70, "# 쿨타임") {
		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}

		@Override
		public String toString() {
			return Formatter.formatCooldown(getValue());
		}
	};
	
	private Player target;
	private final Cooldown cooldown = new Cooldown(COOLDOWN.getValue());
	
	private final AbilityTimer stun = new AbilityTimer(100) {
		
		@Override
		public void onStart() {
			target.teleport(target.getLocation().add(0, 2, 0));
		}
		
    	@Override
		public void onEnd() {
    		onSilentEnd();
    	}
    	
    	@Override
    	public void onSilentEnd() {
    		cooldown.start();
    	}
		
	}.setPeriod(TimeUnit.TICKS, 1).register();
	
	@SubscribeEvent
	public void onPlayerMove(PlayerMoveEvent e) {
		if (stun.isRunning() && e.getPlayer().equals(target)) {
			e.setCancelled(true);
		}
	}
	
	@SubscribeEvent
	public void onEntityDamageByEntity(EntityDamageByEntityEvent e) {
		if (e.getEntity().equals(getPlayer()) && e.getDamager() instanceof Player) target = (Player) e.getDamager();
		if (stun.isRunning() && e.getEntity().equals(target)) {
			if (e.getDamager().equals(getPlayer())) {
				stun.stop(false);
				target.setVelocity(VectorUtil.validateVector(getPlayer().getLocation().getDirection().normalize().multiply(new Vector(8.5, 4, 8.5))));
				SoundLib.ENTITY_PLAYER_ATTACK_SWEEP.playSound(getPlayer().getLocation());
				SoundLib.BLOCK_ANVIL_LAND.playSound(getPlayer().getLocation(), 1, 1.25f);
				new Blow(target).start();
			} else e.setCancelled(true);
		}
		if (!stun.isRunning() && e.getDamager().equals(getPlayer()) && e.getEntity() instanceof Player) {
			Player player = (Player) e.getEntity();
			player.teleport(player.getLocation().add(0, 0.5, 0));
			player.setVelocity(VectorUtil.validateVector(getPlayer().getLocation().getDirection().normalize().multiply(new Vector(2.5, 1.5, 2.5))));
			SoundLib.ENTITY_PLAYER_ATTACK_SWEEP.playSound(getPlayer().getLocation());
			SoundLib.BLOCK_ANVIL_LAND.playSound(getPlayer().getLocation(), 1, 1.25f);
			new Blow(player).start();
		}
	}
	
	public boolean ActiveSkill(Material material, ClickType clicktype) {
		if (material.equals(Material.IRON_INGOT) && clicktype.equals(ClickType.RIGHT_CLICK) && !cooldown.isCooldown()) {
			if (target != null) {
				getPlayer().sendMessage("§c[§b!§c] §e" + target.getName() + "§f님을 5초간 멈춰둡니다.");
				return stun.start();
			} else {
				getPlayer().sendMessage("§c[§b!§c] §f아직 누군가에게 피해를 입은 적이 없습니다.");
			}
		}
		return false;
	}
	
	public class Blow extends AbilityTimer {
		
		private final Player player;
		private Set<Damageable> damageables = new HashSet<>();
		
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
				if (entity instanceof ArmorStand) {
					return false;
				}
				return true;
			}

			@Override
			public boolean apply(@Nullable Entity arg0) {
				return false;
			}
		};
		
		public Blow(Player player) {
			super();
			setPeriod(TimeUnit.TICKS, 1);
			this.player = player;
		}
		
		@SuppressWarnings("deprecation")
		@Override
		public void run(int count) {
			for (Damageable damageable : LocationUtil.getConflictingEntities(Damageable.class, player, predicate)) {
				if (!damageable.equals(getPlayer())) damageables.add(damageable);
			}
			if (damageables.size() > 0) {
				for (Damageable damageable : damageables) {
					if (!LocationUtil.isConflicting(getPlayer(), damageable)) {
						Damages.damageArrow(damageable, player, 7);
						Damages.damageArrow(player, (LivingEntity) damageable, 7);
						damageables.remove(damageable);
						break;
					}	
				}	
			}
			if (player.isOnGround()) {
				this.stop(false);
			}
		}
		
		@Override
		public void onEnd() {
			onSilentEnd();
		}
		
		@Override
		public void onSilentEnd() {
			
		}
		
	}
	
}
