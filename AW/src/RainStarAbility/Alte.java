package RainStarAbility;

import java.util.HashSet;
import java.util.Set;

import javax.annotation.Nullable;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Damageable;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;

import daybreak.abilitywar.ability.AbilityBase;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.SubscribeEvent;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.config.ability.AbilitySettings.SettingObject;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.AbstractGame.Participant.ActionbarNotification.ActionbarChannel;
import daybreak.abilitywar.game.list.mix.Mix;
import daybreak.abilitywar.game.module.DeathManager;
import daybreak.abilitywar.game.team.interfaces.Teamable;
import daybreak.abilitywar.utils.base.Formatter;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.base.math.LocationUtil;
import daybreak.abilitywar.utils.base.math.VectorUtil;
import daybreak.abilitywar.utils.base.minecraft.damage.Damages;
import daybreak.abilitywar.utils.base.minecraft.entity.health.Healths;
import daybreak.abilitywar.utils.base.minecraft.version.ServerVersion;
import daybreak.abilitywar.utils.base.random.Random;
import daybreak.abilitywar.utils.library.MaterialX;
import daybreak.abilitywar.utils.library.ParticleLib;
import daybreak.abilitywar.utils.library.SoundLib;
import daybreak.google.common.base.Predicate;
import daybreak.google.common.collect.ImmutableSet;

@AbilityManifest(name = "알테", rank = Rank.S, species = Species.HUMAN, explain = {
		"§7패시브 §8- §2적 조사§f: 대상에게 처음으로 피해를 받으면 대상의 §a능력 설명§f",
		" 한 줄을 알아낼 수 있습니다. §d믹스 능력자§f에서는 각각 한 줄씩 알아냅니다.",
		"§7패시브 §8- §c전쟁 병기§f: 모든 피해를 §c$[GET_DAMAGE_INCREASE]배§f로 받습니다.",
		" 그 대신, 피해를 $[EVADE]% 확률로 §b회피§f합니다.",
		"§7검 들고 F §8- §3절박한 시도§f: 3초간 받는 다음 피해를 §c$[GET_DAMAGE_INCREASE]배§f 대신 §4$[SKILL_GET_DAMAGE_INCREASE]배§f로 받습니다.",
		" 이 피해로 사망할 경우 피해량의 2배의 체력으로 부활하고, 폭발을 일으킵니다.",
		" 또한 영구 공격력 §c$[DAMAGE_INCREASE]%§f를 획득합니다. $[COOLDOWN]",
		" 다만 자폭에 실패할 경우 체력이 §c§l1§f이 됩니다."
		},
		summarize = {
		"대상 당 한 번 피해를 받으면 대상의 §a능력 설명 한 줄§f을 읽습니다.",
		"모든 피해를 더 많이 받지만 피해를 §b회피§f할 가능성이 있습니다.",
		"§7검 들고 F키§f로 다음 피해를 §c매우 강력하게§f 받지만 이 피해로 사망할 경우",
		"폭발과 함께 부활하며 §c영구 공격력§f을 얻습니다.",
		"사망에 실패한다면 체력이 §c§l1§f이 됩니다."
		})

public class Alte extends AbilityBase {
	
	public Alte(Participant participant) {
		super(participant);
	}
	
	public static final SettingObject<Double> GET_DAMAGE_INCREASE = 
			abilitySettings.new SettingObject<Double>(Alte.class, "get-damage-increase", 1.2,
			"# 받는 피해량 증가 배율") {

		@Override
		public boolean condition(Double value) {
			return value >= 0;
		}

	};
	
	public static final SettingObject<Double> SKILL_GET_DAMAGE_INCREASE = 
			abilitySettings.new SettingObject<Double>(Alte.class, "skill-get-damage-increase", 2.3,
			"# 절박한 시도 간 받는 피해량 증가 배율") {

		@Override
		public boolean condition(Double value) {
			return value >= 0;
		}

	};
	
	public static final SettingObject<Integer> EVADE = 
			abilitySettings.new SettingObject<Integer>(Alte.class, "evade-chance", 15,
			"# 회피 확률", "# 단위: %") {

		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}

	};
	
	public static final SettingObject<Integer> DAMAGE_INCREASE = 
			abilitySettings.new SettingObject<Integer>(Alte.class, "damage-increase", 12,
			"# 영구 공격력", "# 단위: %") {

		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}

	};
	
	public static final SettingObject<Integer> COOLDOWN = 
			abilitySettings.new SettingObject<Integer>(Alte.class, "cooldown", 90,
            "# 쿨타임") {

        @Override
        public boolean condition(Integer value) {
            return value >= 0;
        }

        @Override
        public String toString() {
            return Formatter.formatCooldown(getValue());
        }

    };
	
	private final Set<Player> descriptionChecked = new HashSet<>();
	private static final Set<Material> swords;
	private final Random random = new Random();
	private final double getIncrease = GET_DAMAGE_INCREASE.getValue();
	private final double skillgetIncrease = SKILL_GET_DAMAGE_INCREASE.getValue();
	private final int evade = EVADE.getValue();
	private final int damageincrease = DAMAGE_INCREASE.getValue();
	private final Cooldown cooldown = new Cooldown(COOLDOWN.getValue());
	private int stack = 0;
	private ActionbarChannel ac = newActionbarChannel();
	private boolean success = false;
	private BossBar bossBar = null;
	
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
	
	static {
		if (MaterialX.NETHERITE_SWORD.isSupported()) {
			swords = ImmutableSet.of(MaterialX.WOODEN_SWORD.getMaterial(), Material.STONE_SWORD, Material.IRON_SWORD, MaterialX.GOLDEN_SWORD.getMaterial(), Material.DIAMOND_SWORD, MaterialX.NETHERITE_SWORD.getMaterial());
		} else {
			swords = ImmutableSet.of(MaterialX.WOODEN_SWORD.getMaterial(), Material.STONE_SWORD, Material.IRON_SWORD, MaterialX.GOLDEN_SWORD.getMaterial(), Material.DIAMOND_SWORD);
		}
	}
	
	@Override
	public void onUpdate(Update update) {
		if (update == Update.RESTRICTION_CLEAR) {
			ac.update("§c영구 공격력§f: §e" + (damageincrease * stack) + "§f%");
		}
	}
	
	private AbilityTimer skill = new AbilityTimer(60) {
		
		@Override
    	public void onStart() {
    		bossBar = Bukkit.createBossBar("§c절박한 시도", BarColor.RED, BarStyle.SOLID);
    		bossBar.setProgress(1);
    		bossBar.addPlayer(getPlayer());
    		if (ServerVersion.getVersion() >= 10) bossBar.setVisible(true);
    	}
    	
    	@Override
		public void run(int count) {
    		bossBar.setProgress(count / 60.0);
    	}
    	
		@Override
		public void onEnd() {
			onSilentEnd();
		}

		@Override
		public void onSilentEnd() {
			if (success) {
				success = false;
			} else {
				ParticleLib.DAMAGE_INDICATOR.spawnParticle(getPlayer().getLocation(), 0.5, 1, 0.5, 10, 1);
				Healths.setHealth(getPlayer(), 1);
			}
			cooldown.start();
			bossBar.removeAll();
		}
		
	}.setPeriod(TimeUnit.TICKS, 1).register();
	
    @SubscribeEvent(priority = 998)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent e) {
    	Player damager = null;
		if (e.getDamager() instanceof Projectile) {
			Projectile projectile = (Projectile) e.getDamager();
			if (projectile.getShooter() instanceof Player) damager = (Player) projectile.getShooter();
		} else if (e.getDamager() instanceof Player) damager = (Player) e.getDamager();
		
    	if (!descriptionChecked.contains(damager) && e.getEntity().equals(getPlayer())) {
    		descriptionChecked.add(damager);
    		Participant p = getGame().getParticipant(damager);
    		AbilityBase ab = p.getAbility();
    		if (ab.getClass().equals(Mix.class)) {
    			Mix mix = (Mix) ab;
    			if (mix.hasSynergy()) {
    				String[] description = mix.getSynergy().getManifest().explain();
        			getPlayer().sendMessage("§4[§c조사§4] §e" + damager.getName() + "§f - " + description[random.nextInt(description.length)]);
    			} else {
    				AbilityBase first = mix.getFirst(), second = mix.getSecond();
    				String[] description1 = first.getManifest().explain();
    				String[] description2 = second.getManifest().explain();
        			getPlayer().sendMessage("§4[§c조사§4] §e" + damager.getName() + "§f - " + description1[random.nextInt(description1.length)]);
        			getPlayer().sendMessage("§4[§c조사§4] §e" + damager.getName() + "§f - " + description2[random.nextInt(description2.length)]);
    			}
    		} else {
    			String[] description = ab.getManifest().explain();
    			getPlayer().sendMessage("§4[§c조사§4] §e" + damager.getName() + "§f - " + description[random.nextInt(description.length)]);
    		}
    	}
    	
    	if (!damager.equals(getPlayer()) && e.getEntity().equals(getPlayer())) {
    		if (random.nextInt(100) < evade) {
    			e.setCancelled(true);
    			SoundLib.ENTITY_PLAYER_ATTACK_SWEEP.playSound(getPlayer().getLocation(), 1, 1.7f);
    			if (skill.isRunning()) skill.stop(false);
    			getPlayer().setNoDamageTicks(20);
    		} else {
        		if (skill.isRunning()) {
        			e.setDamage(e.getDamage() * skillgetIncrease);
        			if (getPlayer().getHealth() - e.getFinalDamage() <= 0) {
        				e.setCancelled(true);
        				Healths.setHealth(getPlayer(), e.getFinalDamage() * 2);
        				ParticleLib.EXPLOSION_HUGE.spawnParticle(getPlayer().getLocation());
        				SoundLib.ENTITY_GENERIC_EXPLODE.playSound(getPlayer().getLocation());
        				for (Damageable damageable : LocationUtil.getNearbyEntities(Damageable.class, getPlayer().getLocation(), 5, 5, predicate)) {
        					Damages.damageExplosion(damageable, getPlayer(), (float) (e.getDamage()));
        					damageable.setVelocity(VectorUtil.validateVector(damageable.getLocation().toVector().subtract(getPlayer().getLocation().toVector()).normalize().setY(0.5).multiply(0.95)));
        				}
        				stack++;
        				ac.update("§c영구 공격력§f: §e" + (damageincrease * stack) + "§f%");
        				success = true;
        			}
        			skill.stop(false);
        		} else {
        			e.setDamage(e.getDamage() * getIncrease);
        		}	
    		}
    	}
    	
    	if (damager.equals(getPlayer()) && !e.getEntity().equals(getPlayer())) {
    		e.setDamage(e.getDamage() * (1 + (damageincrease * stack * 0.01)));
    	}
    }
	
    @SubscribeEvent(onlyRelevant = true)
    public void onPlayerSwapHandItems(PlayerSwapHandItemsEvent e) {
    	if (swords.contains(e.getOffHandItem().getType()) && e.getPlayer().equals(getPlayer())) {
    		if (!cooldown.isRunning() && !skill.isRunning()) {
    			skill.start();
    		}
    		e.setCancelled(true);
    	}
    }
    
}
