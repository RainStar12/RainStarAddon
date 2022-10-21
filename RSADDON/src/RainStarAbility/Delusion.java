package RainStarAbility;

import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;

import daybreak.abilitywar.ability.AbilityBase;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.config.ability.AbilitySettings.SettingObject;
import daybreak.abilitywar.ability.SubscribeEvent;
import daybreak.abilitywar.ability.event.AbilityActiveSkillEvent;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.utils.base.Formatter;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.base.concurrent.SimpleTimer.TaskType;
import daybreak.abilitywar.utils.library.MaterialX;
import daybreak.abilitywar.utils.library.ParticleLib;
import daybreak.abilitywar.utils.library.SoundLib;
import daybreak.google.common.collect.ImmutableSet;

@AbilityManifest(name = "나 방금 강해지는 상상함", rank = Rank.C, species = Species.OTHERS, explain = {
		"검 우클릭 시 공격력이 $[DAMAGE_UP]% 증가합니다.",
		"0.5초에 거쳐 공격력은 빠르게 줄어듭니다. $[COOLDOWN]"
		},
		summarize = {
		"§7검 우클릭 시§f 매우 짧은 시간동안 공격력이 폭증합니다."
		})

public class Delusion extends AbilityBase {

	public Delusion(Participant participant) {
		super(participant);
	}
	
	public static final SettingObject<Integer> DAMAGE_UP = 
			abilitySettings.new SettingObject<Integer>(Delusion.class, "damage-up", 100,
            "# 공격력 증가 수치", "# 단위: %") {
        @Override
        public boolean condition(Integer value) {
            return value >= 0;
        }
    };
	
	public static final SettingObject<Integer> COOLDOWN = 
			abilitySettings.new SettingObject<Integer>(Delusion.class, "cooldown", 40,
            "# 검 우클릭 쿨타임", "# 단위: 초") {
        @Override
        public boolean condition(Integer value) {
            return value >= 0;
        }
        @Override
        public String toString() {
            return Formatter.formatCooldown(getValue());
        }
    };
    
    private final Cooldown cooldown = new Cooldown(COOLDOWN.getValue());
	private final int fulldmg = DAMAGE_UP.getValue();
	private int incdamage = fulldmg;
	private static final Set<Material> swords;
	private BossBar bossBar = null;
	
	static {
		if (MaterialX.NETHERITE_SWORD.isSupported()) {
			swords = ImmutableSet.of(MaterialX.WOODEN_SWORD.getMaterial(), Material.STONE_SWORD, Material.IRON_SWORD, MaterialX.GOLDEN_SWORD.getMaterial(), Material.DIAMOND_SWORD, MaterialX.NETHERITE_SWORD.getMaterial());
		} else {
			swords = ImmutableSet.of(MaterialX.WOODEN_SWORD.getMaterial(), Material.STONE_SWORD, Material.IRON_SWORD, MaterialX.GOLDEN_SWORD.getMaterial(), Material.DIAMOND_SWORD);
		}
	}
	
	@SubscribeEvent
	public void onPlayerInteract(PlayerInteractEvent e) {
		if (e.getPlayer().equals(getPlayer()) && (e.getAction() == Action.RIGHT_CLICK_BLOCK || e.getAction() == Action.RIGHT_CLICK_AIR) && e.getItem() != null && swords.contains(e.getItem().getType()) && !cooldown.isCooldown()) {
			final AbilityActiveSkillEvent event = new AbilityActiveSkillEvent(this, e.getItem().getType(), ClickType.RIGHT_CLICK);
			Bukkit.getPluginManager().callEvent(event);
			getPlayer().sendMessage("§d능력을 사용하였습니다.");
			damageUp.start();
			cooldown.start();
		}
	}
	
	private AbilityTimer damageUp = new AbilityTimer(TaskType.REVERSE, 10) {
		
		@Override
		public void onStart() {
			ParticleLib.FLAME.spawnParticle(getPlayer().getLocation(), 0, 0, 0, 50, 1.5);
			SoundLib.ENTITY_ENDER_DRAGON_GROWL.playSound(getPlayer().getLocation(), 1, 1);
			bossBar = Bukkit.createBossBar("§c공격력 증가§7: §e" + incdamage + "§7%", BarColor.RED, BarStyle.SOLID);
    		bossBar.setProgress(1);
    		bossBar.addPlayer(getPlayer());
    		bossBar.setVisible(true);
		}
		
		@Override
		public void run(int count) {
			incdamage = (int) (fulldmg * (count / (double) 10));
			bossBar.setTitle("§c공격력 증가§7: §e" + incdamage + "§7%");
			bossBar.setProgress(count / (double) 10);
		}
		
		@Override
		public void onEnd() {
			onSilentEnd();
		}

		@Override
		public void onSilentEnd() {
			bossBar.removeAll();
			incdamage = fulldmg;
		}
		
	}.setPeriod(TimeUnit.TICKS, 1);
	
	@SubscribeEvent
	public void onEntityDamageByEntity(EntityDamageByEntityEvent e) {
		if (damageUp.isRunning()) {
			Player damager = null;
			if (e.getDamager() instanceof Projectile) {
				Projectile projectile = (Projectile) e.getDamager();
				if (projectile.getShooter() instanceof Player) damager = (Player) projectile.getShooter();
			} else if (e.getDamager() instanceof Player) damager = (Player) e.getDamager();
			
			if (getPlayer().equals(damager)) {
				e.setDamage(e.getDamage() + (e.getDamage() * incdamage * 0.01));
			}	
		}
	}
	
}