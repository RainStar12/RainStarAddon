package rainstar.abilitywar.ability;

import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nullable;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent.RegainReason;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerArmorStandManipulateEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.util.Vector;

import daybreak.abilitywar.AbilityWar;
import daybreak.abilitywar.ability.AbilityBase;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.ability.SubscribeEvent;
import daybreak.abilitywar.ability.decorator.ActiveHandler;
import daybreak.abilitywar.config.ability.AbilitySettings.SettingObject;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.AbstractGame.Participant.ActionbarNotification.ActionbarChannel;
import daybreak.abilitywar.game.module.DeathManager;
import daybreak.abilitywar.game.team.interfaces.Teamable;
import daybreak.abilitywar.utils.base.Formatter;
import daybreak.abilitywar.utils.base.color.RGB;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.base.concurrent.SimpleTimer.TaskType;
import daybreak.abilitywar.utils.base.math.LocationUtil;
import daybreak.abilitywar.utils.base.math.geometry.Line;
import daybreak.abilitywar.utils.base.minecraft.entity.health.Healths;
import daybreak.abilitywar.utils.base.minecraft.item.Skulls;
import daybreak.abilitywar.utils.base.minecraft.nms.NMS;
import daybreak.abilitywar.utils.library.ParticleLib;
import daybreak.abilitywar.utils.library.SoundLib;
import daybreak.google.common.base.Predicate;

@AbilityManifest(name = "데빌", rank = Rank.L, species = Species.OTHERS, explain = {
		"§7패시브 §8- §3비열한 수§f: 적에게 근접 피해를 입힐 때, 대상을 $[LOOK]초간 바라봅니다.",
		" 효과 지속 동안 대상에게 근접 피해를 입히면 대상은 넉백 효과가 감소합니다.",
		"§7패시브 §8- §c부당 계약§f: 적이 나를 공격할 때, $[CONTRACT_DURATION]초간 계약합니다.§8(§7재타격 시 갱신§8)§f",
		" §4(§c계약자 수 §e× §c$[INCREASE]§4)§f%만큼 공격력이 증가하고, 대상의 §d자연 회복 효과§f를 가져옵니다.",
		"§7철괴 좌클릭 §8- §4착취§f: 가장 가까운 계약자에게 최대 $[ENCROACH_DURATION]초간 잠식합니다.",
		" 잠식 간 능력의 지정 대상이 되지 않고 대상에게서 체력을 흡수해갑니다.",
		" 이때 웅크릴 시 대상을 변경하고 지속시간을 갱신시킬 수 있습니다.",
		" 사용 중 모든 계약 시간은 흐르지 않습니다. $[ENCROACH_COOLDOWN]",
		"§b[§7아이디어 제공자§b] §chorn1111"
		},
		summarize = {
		"근접 공격 시 잠시간 §3에임§f이 대상에게 §5유도§f됩니다. 나를 공격한 적과 계약하여",
		"계약자 수만큼 §c공격력 증가§f 및 대상의 §a자연 회복 효과§f를 강탈합니다.",
		"§7철괴 좌클릭으로§f 가장 가까운 계약자에게 잠식하여 대상의 체력을 빼앗습니다.",
		"웅크린다면 잠식 대상을 다른 계약자로 변경하고 지속시간을 리셋시킵니다."
		})
public class Devil extends AbilityBase implements ActiveHandler {

	public Devil(Participant participant) {
		super(participant);
	}
	
	public static final SettingObject<Double> CONTRACT_DURATION = 
			abilitySettings.new SettingObject<Double>(Devil.class, "contract-duration", 20.0,
            "# 계약 지속시간", "# 단위: 초") {
        @Override
        public boolean condition(Double value) {
            return value >= 0;
        }
    };
    
	public static final SettingObject<Double> LOOK = 
			abilitySettings.new SettingObject<Double>(Devil.class, "look", 0.5,
            "# 대상을 바라보는 지속 시간", "# 단위: 초") {
        @Override
        public boolean condition(Double value) {
            return value >= 0;
        }
    };
    
	
	public static final SettingObject<Integer> INCREASE = 
			abilitySettings.new SettingObject<Integer>(Devil.class, "increase", 15,
            "# 계약자 당 공격력 증가 수치", "# 단위: %") {
        @Override
        public boolean condition(Integer value) {
            return value >= 0;
        }
    };
    
	public static final SettingObject<Integer> HEALTH_STOLE = 
			abilitySettings.new SettingObject<Integer>(Devil.class, "health-stole", 2,
            "# 잠식 시 매 0.5초당 가져올 체력", "# 단위: 최대 체력의 (수치)%", "# 예) 20체력 기준 10으로 설정 시 20체력의 10%, 2HP") {
        @Override
        public boolean condition(Integer value) {
            return value >= 0;
        }
    };
	
	public static final SettingObject<Double> ENCROACH_DURATION = 
			abilitySettings.new SettingObject<Double>(Devil.class, "encroach-duration", 3.0,
            "# 계약자 잠식 최대 지속시간", "# 단위: 초") {
        @Override
        public boolean condition(Double value) {
            return value >= 0;
        }
    };
	
	public static final SettingObject<Integer> ENCROACH_COOLDOWN = 
			abilitySettings.new SettingObject<Integer>(Devil.class, "cooldown", 90,
            "# 철괴 좌클릭 쿨타임", "# 단위: 초") {
        @Override
        public boolean condition(Integer value) {
            return value >= 0;
        }
        @Override
        public String toString() {
            return Formatter.formatCooldown(getValue());
        }
    };
	
    private final int contract_duration = (int) (CONTRACT_DURATION.getValue() * 20);
    private final double increase = (INCREASE.getValue() / (double) 100);
    private double stole = (HEALTH_STOLE.getValue() / (double) 100);
    private final int encroach_duration = (int) (ENCROACH_DURATION.getValue() * 20);
    private final Cooldown cooldown = new Cooldown(ENCROACH_COOLDOWN.getValue());
    private final int looktime = (int) (LOOK.getValue() * 20);
    private final ActionbarChannel ac = newActionbarChannel(), ac2 = newActionbarChannel();
	private Player target = null;
	private int timerstack = looktime;
	private final Map<Player, Contract> contract = new HashMap<>();
	private final Set<Player> encroached = new HashSet<>();
	private Player encroachtarget;
	private final DecimalFormat df = new DecimalFormat("0.0");
	private RGB hornred = RGB.of(217, 113, 113), hornblack = RGB.of(67, 56, 50);
	private ArmorStand armorstand;
	
	private final Predicate<Entity> predicate = new Predicate<Entity>() {
		@Override
		public boolean test(Entity entity) {
			if (entity.equals(getPlayer()))
				return false;
			if (entity instanceof Player) {
				if (!getGame().isParticipating(entity.getUniqueId())
						|| (getGame() instanceof DeathManager.Handler && ((DeathManager.Handler) getGame())
								.getDeathManager().isExcluded(entity.getUniqueId()))
						|| !getGame().getParticipant(entity.getUniqueId()).attributes().TARGETABLE.getValue()) {
					return false;
				}
				if (getGame() instanceof Teamable) {
					final Teamable teamGame = (Teamable) getGame();
					final Participant entityParticipant = teamGame.getParticipant(entity.getUniqueId()),
							participant = getParticipant();
					return !teamGame.hasTeam(entityParticipant) || !teamGame.hasTeam(participant)
							|| (!teamGame.getTeam(entityParticipant).equals(teamGame.getTeam(participant)));
				}
				if (encroached.contains(entity)) return false;
				if (!contract.containsKey(entity)) return false;
			}
			return true;
		}

		@Override
		public boolean apply(@Nullable Entity arg0) {
			return false;
		}
	};
	
	protected void onUpdate(Update update) {
		if (update == Update.RESTRICTION_CLEAR) {
			look.start();
		}
	}
	
	private final AbilityTimer look = new AbilityTimer() {
		
		@Override
		public void run(int count) {
			if (target != null) {
				final Vector direction = target.getEyeLocation().toVector().subtract(getPlayer().getEyeLocation().toVector());
				final float yaw = LocationUtil.getYaw(direction), pitch = LocationUtil.getPitch(direction);
				NMS.rotateHead(getPlayer(), getPlayer(), yaw, pitch);
				timerstack--;
				if (timerstack <= 1) target = null;
			}
			
			ac.update("§c계약자 수§f: §e" + contract.size() + "§f명");
		}
		
	}.setPeriod(TimeUnit.TICKS, 1).register();
	
	private final AbilityTimer encroaching = new AbilityTimer(TaskType.REVERSE, encroach_duration) {
		
		@Override
		public void onStart() {
			SoundLib.ENTITY_ZOMBIE_VILLAGER_CONVERTED.playSound(getPlayer().getLocation(), 1, 1.8f);
			getPlayer().setGameMode(GameMode.SPECTATOR);
			getParticipant().attributes().TARGETABLE.setValue(false);
			encroachtarget = LocationUtil.getNearestEntity(Player.class, getPlayer().getLocation(), predicate);
			getPlayer().setSpectatorTarget(encroachtarget);
			ParticleLib.SMOKE_LARGE.spawnParticle(encroachtarget.getLocation(), 0.25, 0, 0.25, 50, 0.4);
			SoundLib.ENTITY_VEX_CHARGE.playSound(encroachtarget.getLocation(), 1, 0.65f);
			encroached.add(encroachtarget);
			armorstand = getPlayer().getLocation().getWorld().spawn(getPlayer().getLocation().clone(), ArmorStand.class);
			armorstand.setMetadata("hornhead", new FixedMetadataValue(AbilityWar.getPlugin(), null));
			armorstand.setVisible(false);
			armorstand.getEquipment().setHelmet(Skulls.createSkull("horn1111"));
			armorstand.setGravity(false);
            armorstand.setInvulnerable(true);
		}
		
		@Override
		public void run(int count) {
			getPlayer().setSpectatorTarget(encroachtarget);
			for (Contract contracts : contract.values()) {
				contracts.pause();
			}
			ac2.update("§e남은 대상§f: §c" + (contract.size() - encroached.size()) + " §7| §3잠식 시간§f: " + df.format(count / (double) 20));
			if (count % 10 == 0) {
				double maxHP = encroachtarget.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue();
				double stealamount = (maxHP * stole);
				Healths.setHealth(encroachtarget, encroachtarget.getHealth() - stealamount);
				final EntityRegainHealthEvent event = new EntityRegainHealthEvent(getPlayer(), stealamount, RegainReason.CUSTOM);
				Bukkit.getPluginManager().callEvent(event);
				if (!event.isCancelled()) {
					Healths.setHealth(getPlayer(), getPlayer().getHealth() + stealamount);	
				}
				SoundLib.ENTITY_VEX_DEATH.playSound(encroachtarget.getLocation(), 1, 0.75f);
                
                if (encroachtarget.isDead()) {
                    if (LocationUtil.getNearestEntity(Player.class, getPlayer().getLocation(), predicate) != null) {
                     	SoundLib.ENTITY_ZOMBIE_VILLAGER_CONVERTED.playSound(getPlayer().getLocation(), 1, 1.8f);
			            encroachtarget = LocationUtil.getNearestEntity(Player.class, getPlayer().getLocation(), predicate);
			            getPlayer().setSpectatorTarget(encroachtarget);
                        ParticleLib.SMOKE_LARGE.spawnParticle(encroachtarget.getLocation(), 0.25, 0, 0.25, 50, 1);
                        SoundLib.ENTITY_VEX_CHARGE.playSound(encroachtarget.getLocation(), 1, 0.65f);
                        encroaching.setCount(encroach_duration);   
                    } else stop(false);
                }
			}
			
			double maxHP = getPlayer().getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue();
			double hp = getPlayer().getHealth();
			NMS.sendTitle(getPlayer(), "§dHP", "§a" + df.format(hp) + " §7/ §b" + df.format(maxHP), 0, 10, 0);
		}
		
		@Override
		public void onEnd() {
			onSilentEnd();
		}
		
		@Override
		public void onSilentEnd() {
			getPlayer().setSpectatorTarget(null);
			getPlayer().setGameMode(GameMode.SURVIVAL);
			getParticipant().attributes().TARGETABLE.setValue(true);
			encroachtarget = null;
			encroached.clear();
			cooldown.start();
			for (Contract contracts : contract.values()) {
				contracts.resume();
			}
			ac2.update(null);
			NMS.clearTitle(getPlayer());
			armorstand.remove();
		}
		
	}.setPeriod(TimeUnit.TICKS, 1).register();
	
	@SubscribeEvent
	private void onPlayerDeath(PlayerDeathEvent e) {
		final Contract con = contract.get(e.getEntity());
		if (con != null) {
			con.stop(false);
		}
	}
	
	@SubscribeEvent(onlyRelevant = true)
	private void onSneak(PlayerToggleSneakEvent e) {
		if (encroaching.isRunning() && LocationUtil.getNearestEntity(Player.class, getPlayer().getLocation(), predicate) != null) {
			SoundLib.ENTITY_ZOMBIE_VILLAGER_CONVERTED.playSound(getPlayer().getLocation(), 1, 1.8f);
			encroachtarget = LocationUtil.getNearestEntity(Player.class, getPlayer().getLocation(), predicate);
			getPlayer().setSpectatorTarget(encroachtarget);
			encroached.add(encroachtarget);
			ParticleLib.SMOKE_LARGE.spawnParticle(encroachtarget.getLocation(), 0.25, 0, 0.25, 50, 1);
			SoundLib.ENTITY_VEX_CHARGE.playSound(encroachtarget.getLocation(), 1, 0.65f);
			encroaching.setCount(encroach_duration);
		}
	}
	
	@SubscribeEvent
	public void onRegainHealth(EntityRegainHealthEvent e) {
		if (contract.containsKey(e.getEntity()) && (e.getRegainReason().equals(RegainReason.SATIATED) || e.getRegainReason().equals(RegainReason.REGEN))) {
			final EntityRegainHealthEvent event = new EntityRegainHealthEvent(getPlayer(), e.getAmount(), RegainReason.CUSTOM);
			Bukkit.getPluginManager().callEvent(event);
			if (!event.isCancelled()) {
				Healths.setHealth(getPlayer(), getPlayer().getHealth() + e.getAmount());
			}
			e.setCancelled(true);
		}
	}
	
	@SubscribeEvent
	public void onPlayerMove(PlayerMoveEvent e) {
		if (e.getPlayer().equals(encroachtarget) && encroaching.isRunning()) {
			ParticleLib.REDSTONE.spawnParticle(encroachtarget.getLocation().clone().add(0, 1, 0), hornred);
			armorstand.teleport(encroachtarget.getLocation().clone().add(0, 1, 0));
		}
	}
	
	@SubscribeEvent
	private void onArmorStandManipulate(PlayerArmorStandManipulateEvent e) {
		if (e.getRightClicked().hasMetadata("hornhead")) e.setCancelled(true);
	}
	
	@SubscribeEvent
	public void onEntityDamageByEntity(EntityDamageByEntityEvent e) {
		Player damager = null;
		if (e.getDamager() instanceof Projectile) {
			Projectile projectile = (Projectile) e.getDamager();
			if (projectile.getShooter() instanceof Player) damager = (Player) projectile.getShooter();
		} else if (e.getDamager() instanceof Player) damager = (Player) e.getDamager();
		
		if (e.getDamager().equals(getPlayer()) && e.getEntity() instanceof Player) {
			if (target != null && e.getEntity().equals(target)) e.getEntity().setVelocity(getPlayer().getLocation().toVector().subtract(e.getEntity().getLocation().toVector()).multiply(0.2).setY(0));
			timerstack = looktime;
			target = (Player) e.getEntity();
		}
		
		if (getPlayer().equals(damager)) {
			e.setDamage(e.getDamage() * (1 + (increase * contract.size())));
		}
		
		if (e.getEntity().equals(getPlayer()) && !getPlayer().equals(damager) && damager != null) {
			if (contract.containsKey(damager)) contract.get(damager).setCount(contract_duration);
			else new Contract(damager).start();
		}
	}
	
	public boolean ActiveSkill(Material material, AbilityBase.ClickType clicktype) {
		if (material.equals(Material.IRON_INGOT) && clicktype.equals(ClickType.LEFT_CLICK)) {
			if (contract.size() >= 1) {
				if (!cooldown.isCooldown() && !encroaching.isRunning()) {
					return encroaching.start();
				}
			} else getPlayer().sendMessage("§4[§c!§4] §f계약자가 없어 능력을 사용할 수 없습니다.");
		}
		return false;
	}
	
	public class Contract extends AbilityTimer {
		
		private final Player player;
		private final Participant participant;
		private final ActionbarChannel ac;
		private final DecimalFormat df = new DecimalFormat("0.0");
		
		private Contract(Player player) {
			super(contract_duration);
			setPeriod(TimeUnit.TICKS, 1);
			contract.put(player, this);
			this.player = player;
			this.participant = getGame().getParticipant(player);
			this.ac = participant.actionbar().newChannel();
		}
		
		@Override
		protected void onStart() {
			SoundLib.ENTITY_EVOKER_FANGS_ATTACK.playSound(player, 1, 0.55f);
			SoundLib.ENTITY_EVOKER_FANGS_ATTACK.playSound(getPlayer(), 1, 0.55f);
		}
		
		@Override
		protected void run(int count) {
			if (count % 10 == 0) {
				for (Location loc : Line.between(getPlayer().getLocation().clone().add(0, 1, 0), player.getLocation().clone().add(0, 1, 0), 100).toLocations(getPlayer().getLocation().clone().add(0, 1, 0))) {
					ParticleLib.REDSTONE.spawnParticle(loc, hornblack);
				}
			}
			ac.update("§e" + getPlayer().getName() + "§7와 계약§f: §c" + df.format(count / (double) 20) + "§f초");
		}
		
		@Override
		protected void onEnd() {
			onSilentEnd();
		}

		@Override
		protected void onSilentEnd() {
			contract.remove(player);
			ac.unregister();
		}
		
	}
	
}