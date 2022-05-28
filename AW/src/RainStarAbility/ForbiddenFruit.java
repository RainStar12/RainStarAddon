package RainStarAbility;

import javax.annotation.Nullable;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent.RegainReason;

import daybreak.abilitywar.AbilityWar;
import daybreak.abilitywar.ability.AbilityBase;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.ability.decorator.ActiveHandler;
import daybreak.abilitywar.config.ability.AbilitySettings.SettingObject;
import daybreak.abilitywar.game.AbstractGame;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.module.DeathManager;
import daybreak.abilitywar.game.team.interfaces.Teamable;
import daybreak.abilitywar.utils.base.Formatter;
import daybreak.abilitywar.utils.base.math.LocationUtil;
import daybreak.abilitywar.utils.base.minecraft.entity.health.Healths;
import daybreak.abilitywar.utils.base.random.Random;
import daybreak.abilitywar.utils.library.ParticleLib;
import daybreak.abilitywar.utils.library.SoundLib;
import daybreak.google.common.base.Predicate;

@AbilityManifest(name = "선악과", rank = Rank.S, species = Species.OTHERS, explain = {
		"철괴를 이용해 §b선§c악§f 스킬을 사용할 수 있습니다.",
		"두 스킬은 §c쿨타임§f을 공유합니다. $[COOLDOWN]",
		"§7우클릭 §8- §b선§f: 바라보는 대상§8(§7없으면 자신§8)§f의 체력을 최대 체력까지 회복시킵니다.",
		" 이 스킬로 §6회복§f한 체력 반 칸당 다음 §c쿨타임§f이 $[DECREASE_COOLDOWN]초 감소합니다.",
		"§7좌클릭 §8- §c악§f: 자신이 포함된 무작위 대상의 체력을 반 칸으로 만듭니다.",
		" 이후 대상은 $[INV_DURATION]초간 무적 및 공격력이 $[DAMAGE_UP]% 증가합니다.",
		" 이 스킬로 §3없앤§f 체력 반 칸당 다음 §c쿨타임§f이 $[DECREASE_COOLDOWN]초 감소합니다."
		})

public class ForbiddenFruit extends AbilityBase implements ActiveHandler {
	
	public ForbiddenFruit(Participant participant) {
		super(participant);
	}
	
	public static final SettingObject<Integer> COOLDOWN = 
			abilitySettings.new SettingObject<Integer>(ForbiddenFruit.class, "cooldown", 200,
            "# 쿨타임", "# 단위: 초", "# §cW§6R§eE§aC§bK§7 최대 적용: 33%") {
        @Override
        public boolean condition(Integer value) {
            return value >= 0;
        }
        @Override
        public String toString() {
            return Formatter.formatCooldown(getValue());
        }
    };
    
	public static final SettingObject<Integer> DAMAGE_UP = 
			abilitySettings.new SettingObject<Integer>(ForbiddenFruit.class, "damage-up", 30,
            "# 공격력 증가 수치", "# 단위: %") {
        @Override
        public boolean condition(Integer value) {
            return value >= 0;
        }
    };
    
	public static final SettingObject<Double> DECREASE_COOLDOWN = 
			abilitySettings.new SettingObject<Double>(ForbiddenFruit.class, "decrease-cooldown", 3.5,
            "# 쿨타임 감소 수치", "# 단위: 초") {
        @Override
        public boolean condition(Double value) {
            return value >= 0;
        }
    };
    
	public static final SettingObject<Double> INV_DURATION = 
			abilitySettings.new SettingObject<Double>(ForbiddenFruit.class, "inv-duration", 10.0,
            "# 악 효과 무적 시간", "# 단위: 초") {
        @Override
        public boolean condition(Double value) {
            return value >= 0;
        }
    };
    
	private final Predicate<Entity> predicate = new Predicate<Entity>() {
		@Override
		public boolean test(Entity entity) {
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
				if (getPlayer().equals(entity)) return false;
			}
			return true;
		}

		@Override
		public boolean apply(@Nullable Entity arg0) {
			return false;
		}
	};
	
    private final double decreasevalue = DECREASE_COOLDOWN.getValue();
    private final int invduration = (int) (INV_DURATION.getValue() * 20);
    private final int incdamage = DAMAGE_UP.getValue();
	private final Cooldown cooldown = new Cooldown(COOLDOWN.getValue(), 33);
	
	public boolean ActiveSkill(Material material, ClickType clicktype) {
		if (material == Material.IRON_INGOT && !cooldown.isCooldown()) {
			int decreasestack = 0;
			if (clicktype.equals(ClickType.LEFT_CLICK)) {
				final Random random = new Random();
	            AbstractGame.Participant participant = random.pick(getGame().getParticipants().toArray(new AbstractGame.Participant[0]));
	            decreasestack = (int) (participant.getPlayer().getHealth() - 1);
	            Healths.setHealth(participant.getPlayer(), 1);
	            new InvTimer(participant.getPlayer(), invduration).start();
	            ParticleLib.SMOKE_LARGE.spawnParticle(participant.getPlayer().getLocation(), 0.25, 0, 0.25, 50, 1);
				SoundLib.ENTITY_VEX_CHARGE.playSound(participant.getPlayer().getLocation(), 1, 0.65f);
			} else if (clicktype.equals(ClickType.RIGHT_CLICK)) {
				Player p;
				if (LocationUtil.getEntityLookingAt(Player.class, getPlayer(), 30, predicate) != null) {
					p = LocationUtil.getEntityLookingAt(Player.class, getPlayer(), 30, predicate);
				} else p = getPlayer();
				double maxHP = p.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue();
				final EntityRegainHealthEvent event = new EntityRegainHealthEvent(p, maxHP, RegainReason.CUSTOM);
				Bukkit.getPluginManager().callEvent(event);
				if (!event.isCancelled()) {
					decreasestack = (int) (maxHP - p.getHealth());
					Healths.setHealth(p, maxHP);
		            ParticleLib.CLOUD.spawnParticle(p.getLocation(), 0.25, 0, 0.25, 50, 1);
		            ParticleLib.HEART.spawnParticle(p.getLocation(), 0.5, 1, 0.5, 10, 1);
					SoundLib.ENTITY_PLAYER_LEVELUP.playSound(p.getLocation(), 1, 1);
				}
			}
			cooldown.start();
			cooldown.setCount((int) Math.max(1, cooldown.getCount() - (decreasestack * decreasevalue)));
			return true;
		}
		return false;
	}
	
	public class InvTimer extends AbilityTimer implements Listener {
		
		private final Player player;
		
		public InvTimer(Player player, int duration) {
			super(TaskType.REVERSE, duration);
			this.player = player;
		}
		
		@Override
		public void onStart() {
			Bukkit.getPluginManager().registerEvents(this, AbilityWar.getPlugin());
		}
		
		@EventHandler()
		public void onEntityDamage(EntityDamageEvent e) {
			if (e.getEntity().equals(player)) e.setCancelled(true);
		}
		
		@EventHandler()
		public void onEntityDamageByEntity(EntityDamageByEntityEvent e) {
			onEntityDamage(e);
			Player damager = null;
			if (e.getDamager() instanceof Projectile) {
				Projectile projectile = (Projectile) e.getDamager();
				if (projectile.getShooter() instanceof Player) damager = (Player) projectile.getShooter();
			} else if (e.getDamager() instanceof Player) damager = (Player) e.getDamager();
			
			if (player.equals(damager)) {
				e.setDamage(e.getDamage() + (e.getDamage() * incdamage * 0.01));
			}	
		}
		
	}

}
