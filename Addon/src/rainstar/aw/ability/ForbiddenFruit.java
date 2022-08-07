package rainstar.aw.ability;

import daybreak.abilitywar.AbilityWar;
import daybreak.abilitywar.ability.AbilityBase;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.ability.decorator.ActiveHandler;
import daybreak.abilitywar.config.ability.AbilitySettings.SettingObject;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.AbstractGame.Participant.ActionbarNotification.ActionbarChannel;
import daybreak.abilitywar.game.module.DeathManager;
import daybreak.abilitywar.utils.base.Formatter;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.base.math.LocationUtil;
import daybreak.abilitywar.utils.base.minecraft.entity.health.Healths;
import daybreak.abilitywar.utils.base.minecraft.nms.NMS;
import daybreak.abilitywar.utils.base.random.Random;
import daybreak.abilitywar.utils.library.ParticleLib;
import daybreak.abilitywar.utils.library.PotionEffects;
import daybreak.abilitywar.utils.library.SoundLib;
import daybreak.google.common.base.Predicate;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent.RegainReason;

import javax.annotation.Nullable;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

@AbilityManifest(name = "선악과", rank = Rank.L, species = Species.OTHERS, explain = {
		"철괴를 이용해 §b선§c악§f 스킬을 사용할 수 있습니다. 두 스킬은 §c쿨타임§f을 공유합니다.",
		"스킬로 §d회복한§7 / §3없앤§f 체력 반 칸당 다음 §c쿨타임§f이 $[DECREASE_COOLDOWN]초 감소합니다.",
		"감소 효과는 $[MAX_STACK]스택까지 적용됩니다. $[COOLDOWN]",
		"§7우클릭 §8- §b선§f: 바라보는 대상§8(§7없으면 자신§8)§f의 체력을 최대 체력까지 회복시킵니다.",
		" 추가로, 적에게 능력 사용 시 §d회복량§f의 절반만큼 §e흡수 체력§f을 획득합니다.",
		"§7좌클릭 §8- §c악§f: 자신 §c$[CHANCE]§f%, 타인 §b$(CHANCE_CALCULATE)§f%로 체력을 반 칸으로 만듭니다.",
		" 이후 대상은 $[INV_DURATION]초간 무적 및 공격력이 $[DAMAGE_UP]% 증가하고 나서 10초간 재생합니다."
		},
		summarize = {
		"§7철괴 우클릭으로§f §b선§f, §7좌클릭으로§f §c악§f 효과를 사용합니다.",
		"스킬로 영향을 미친 체력만큼 다음 §c쿨타임§f이 줄어듭니다. $[COOLDOWN]",
		"§3[§b선§3]§f 바라보는 대상 / 자신의 체력을 최대 체력까지 회복시킵니다.",
		" 대상이 자신이 아니라면 회복량의 절반만큼 §e흡수 체력§f을 획득합니다.",
		"§4[§c악§4]§f 모든 플레이어 중 한 명의 체력을 반 칸으로 만듭니다.",
		" 이후 대상은 일정 시간 §c무적 및 공격력 증가§f를 얻고 나서 잠시간 §d재생§f합니다."
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
    
	public static final SettingObject<Integer> MAX_STACK = 
			abilitySettings.new SettingObject<Integer>(ForbiddenFruit.class, "max-stack", 20,
            "# 감소 효과 최대치") {
        @Override
        public boolean condition(Integer value) {
            return value >= 0;
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
    
	public static final SettingObject<Integer> CHANCE = 
			abilitySettings.new SettingObject<Integer>(ForbiddenFruit.class, "chance", 33,
            "# 악 효과가 자신이 걸릴 확률", "# 단위: %") {
        @Override
        public boolean condition(Integer value) {
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
    private final int maxstack = MAX_STACK.getValue();
    private final int chance = CHANCE.getValue();
	private final Cooldown cooldown = new Cooldown(COOLDOWN.getValue(), 33);
	
	@SuppressWarnings("unused")
	private final static Object CHANCE_CALCULATE = new Object() {
		@Override
		public String toString() {
			return "" + (100 - CHANCE.getValue());
		}
	};
	
	public boolean ActiveSkill(Material material, ClickType clicktype) {
		if (material == Material.IRON_INGOT && !cooldown.isCooldown()) {
			int decreasestack = 0;
			if (clicktype.equals(ClickType.LEFT_CLICK)) {
				final Random random = new Random();
				Participant participant;
				if (random.nextInt(100) < chance) {
					participant = getParticipant();
				} else {
					List<Participant> participants = new ArrayList<>();
					for (Participant participantlist : getGame().getParticipants()) {
						if (predicate.test(participantlist.getPlayer())) participants.add(participantlist);
					}
					participant = random.pick(participants);
				}
	            decreasestack = (int) (participant.getPlayer().getHealth() - 1);
	            Healths.setHealth(participant.getPlayer(), 1);
	            new InvTimer(participant.getPlayer(), invduration).start();
	            ParticleLib.SMOKE_LARGE.spawnParticle(participant.getPlayer().getLocation(), 0.25, 0, 0.25, 50, 0.4);
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
		            ParticleLib.CLOUD.spawnParticle(p.getLocation(), 0.25, 0, 0.25, 50, 0.4);
		            ParticleLib.HEART.spawnParticle(p.getLocation(), 0.5, 1, 0.5, 10, 1);
					SoundLib.ENTITY_PLAYER_LEVELUP.playSound(p.getLocation(), 1, 1);
					if (!p.equals(getPlayer())) NMS.setAbsorptionHearts(getPlayer(), (float) (NMS.getAbsorptionHearts(getPlayer()) + (decreasestack * 0.5)));
				}
			}
			if (decreasestack > maxstack) decreasestack = maxstack;
			cooldown.start();
			cooldown.setCount((int) Math.max(1, cooldown.getCount() - (decreasestack * decreasevalue)));
			return true;
		}
		return false;
	}
	
	public class InvTimer extends AbilityTimer implements Listener {
		
		private final Player player;
		private final DecimalFormat df = new DecimalFormat("0.0");
		private final ActionbarChannel ac;
		
		public InvTimer(Player player, int duration) {
			super(TaskType.REVERSE, duration);
			setPeriod(TimeUnit.TICKS, 1);
			this.player = player;
			ac = getGame().getParticipant(player).actionbar().newChannel();
		}
		
		@Override
		public void onStart() {
			Bukkit.getPluginManager().registerEvents(this, AbilityWar.getPlugin());
		}
		
		@Override
		public void run(int count) {
			ac.update("§3무적§f: " + df.format(count / 20.0));
		}
		
		@Override
		public void onEnd() {
			onSilentEnd();
		}
		
		@Override
		public void onSilentEnd() {
			PotionEffects.REGENERATION.addPotionEffect(player, 200, 0, true);
			HandlerList.unregisterAll(this);
			ac.unregister();
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
