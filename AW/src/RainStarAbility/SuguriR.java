package RainStarAbility;

import java.text.DecimalFormat;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;

import daybreak.abilitywar.ability.AbilityBase;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.SubscribeEvent;
import daybreak.abilitywar.ability.AbilityBase.AbilityTimer;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.ability.decorator.ActiveHandler;
import daybreak.abilitywar.config.ability.AbilitySettings.SettingObject;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.manager.effect.Stun;
import daybreak.abilitywar.utils.base.Formatter;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.base.minecraft.version.ServerVersion;
import daybreak.abilitywar.utils.base.random.Random;
import daybreak.abilitywar.utils.library.MaterialX;
import daybreak.abilitywar.utils.library.ParticleLib;
import daybreak.abilitywar.utils.library.SoundLib;
import daybreak.google.common.collect.ImmutableSet;

@AbilityManifest(name = "스구리", rank = Rank.S, species = Species.HUMAN, explain = {
		"§7수치 §8- §c히트§f: §c히트§f가 §e100§c%§f 이상이면 받는 피해량이 비례하여 증가합니다.",
		" 최대 §e300§c%§f까지 증가하고, §e250§c%§f 이상이라면 §6잔열§f이 쌓입니다.",
		"§7수치 §8- §6잔열§f: 증가할 때 체력이 감소하고, 감소할 때 체력이 증가합니다.",
		" §6잔열§f은 최대 체력을 차지해, §6잔열§f만큼의 최대 체력은 채울 수 없습니다.",
		"§7검 우클릭 §8- §3대시§f: 바라보는 방향을 향해 지속 대시합니다. §c히트§f 수치가 지속 상승하며,",
		" 사용 직후엔 §c히트§f, 대시 거리가 추가 증가하고 $[EVADE_DURATION]초간 공격을 회피합니다.",
		" 회피한 피해량에 비례하여, §6잔열§f은 감소하고, §b액셀러레이터§f 스킬 게이지가 모여듭니다.",
		"§7철괴 우클릭 §8- §b액셀러레이터§f: 게이지를 모으면 사용할 수 있습니다.",
		" $[ACCELERATOR_DURATION]초간 §c히트§f가 §e0§c%§f가 되고, §6잔열§f이 빠르게 감소합니다.",
		" 또한 공격에 1~6의 추가 피해를 줍니다."
		},
		summarize = {
		""
		})

public class SuguriR extends AbilityBase {
	
	public SuguriR(Participant participant) {
		super(participant);
	}
	
	public static final SettingObject<Double> EVADE_DURATION = 
			abilitySettings.new SettingObject<Double>(SuguriR.class, "evade-duration", 0.5,
			"# 대시 간 회피 지속시간") {

		@Override
		public boolean condition(Double value) {
			return value >= 0;
		}

	};
	
	public static final SettingObject<Double> ACCELERATOR_DURATION = 
			abilitySettings.new SettingObject<Double>(SuguriR.class, "accelerator-duration", 7.5,
			"# 액셀러레이터 지속 시간") {

		@Override
		public boolean condition(Double value) {
			return value >= 0;
		}

	};
	
	public static final SettingObject<Integer> DASH_START_HEAT = 
			abilitySettings.new SettingObject<Integer>(SuguriR.class, "dash-start-heat", 20,
			"# 대시 시작 시 추가 히트") {

		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}

	};
	
    
    @Override
    public void onUpdate(Update update) {
    	if (update == Update.RESTRICTION_CLEAR) {
    		
    	}
    }
    
	private static final Set<Material> swords;
	private final int evadeduration = (int) (EVADE_DURATION.getValue() * 20);
	private final int duration = (int) (ACCELERATOR_DURATION.getValue() * 20);
	private final int dashstartheat = DASH_START_HEAT.getValue();
	
	private final DecimalFormat df = new DecimalFormat("0");
	private int heat = 0;
	private BossBar bossBar = null;
	private long last;
	private double longHeat = 0;

	static {
		if (MaterialX.NETHERITE_SWORD.isSupported()) {
			swords = ImmutableSet.of(MaterialX.WOODEN_SWORD.getMaterial(), Material.STONE_SWORD, Material.IRON_SWORD, MaterialX.GOLDEN_SWORD.getMaterial(), Material.DIAMOND_SWORD, MaterialX.NETHERITE_SWORD.getMaterial());
		} else {
			swords = ImmutableSet.of(MaterialX.WOODEN_SWORD.getMaterial(), Material.STONE_SWORD, Material.IRON_SWORD, MaterialX.GOLDEN_SWORD.getMaterial(), Material.DIAMOND_SWORD);
		}
	}
	
	private AbilityTimer evade = new AbilityTimer(evadeduration) {
		
		@Override
		public void run(int count) {
			
		}
		
	}.setPeriod(TimeUnit.TICKS, 1).register();
	
	private AbilityTimer rightclickchecker = new AbilityTimer() {
		
		@Override
		public void onStart() {
			evade.start();
			heat += dashstartheat;
		}
		
		@Override
		public void run(int count) {
			if (System.currentTimeMillis() - last >= 220) {
				this.stop(false);
			}
			getPlayer().setVelocity(getPlayer().getLocation().getDirection().normalize().multiply(1.25));
		}
		
	}.setPeriod(TimeUnit.TICKS, 1).register();
	
	private final AbilityTimer heatupdater = new AbilityTimer() {
		
    	@Override
    	public void onStart() {
    		bossBar = Bukkit.createBossBar("§c히트 §e" + df.format(heat) + "§c%", BarColor.PINK, BarStyle.SOLID);
    		bossBar.setProgress(Math.min(1, heat * 0.01));
    		bossBar.addPlayer(getPlayer());
    		if (ServerVersion.getVersion() >= 10) bossBar.setVisible(true);
    	}
    	
    	@Override 
		public void run(int count) {
    		if (!rightclickchecker.isRunning()) heat = Math.max(0, heat - 1);
			
			if (accelerator.isRunning()) {
				bossBar.setColor(BarColor.BLUE);
				bossBar.setTitle("§b액셀러레이터");
				bossBar.setProgress(accelerator.getCount() / (double) duration);
			} else {
				if (heat <= 100) bossBar.setColor(BarColor.PINK);
				else if (heat <= 250) bossBar.setColor(BarColor.RED);
				else bossBar.setColor(BarColor.PURPLE);
				bossBar.setTitle("§c히트 §e" + df.format(heat) + "§c%");
				bossBar.setProgress(Math.min(1, (heat - (heat/100) * 100) * 0.01));
			}
    	}
    	
		@Override
		public void onEnd() {
			bossBar.removeAll();
		}

		@Override
		public void onSilentEnd() {
			bossBar.removeAll();
		}
		
	}.setPeriod(TimeUnit.TICKS, 1).register();
	
	private final AbilityTimer accelerator = new AbilityTimer(duration) {
		
    	@Override
    	public void onStart() {
    		ParticleLib.END_ROD.spawnParticle(getPlayer().getLocation(), 1, 2, 1, 35, 1);
    		SoundLib.ENTITY_ILLUSIONER_CAST_SPELL.playSound(getPlayer().getLocation(), 1, 1.3f);
    	}
    	
    	@Override
    	public void run(int count) {
    		heat = 0;
    	}
    	
    	@Override
    	public void onEnd() {
    	}
		
	}.setPeriod(TimeUnit.TICKS, 1).register();
	
	public void heatGain(int value) {
		heat = Math.min(300, heat + value);
		longHeatGain(heat * 0.01);
	}
	
	public void heatLose(int value) {
		heat = Math.max(0, heat - value);		
	}
	
	public void longHeatGain(double value) {
		longHeat = longHeat + heat;
	}
	
	public void longHeatLose(double value) {
		
	}
	
	@SubscribeEvent
	public void onPlayerInteract(PlayerInteractEvent e) {
		if ((e.getAction().equals(Action.RIGHT_CLICK_AIR) || e.getAction().equals(Action.RIGHT_CLICK_BLOCK)) && e.getPlayer().equals(getPlayer()) && swords.contains(getPlayer().getInventory().getItemInMainHand().getType())) {
			last = System.currentTimeMillis();
			if (!rightclickchecker.isRunning()) {
				rightclickchecker.start();
				getPlayer().setVelocity(getPlayer().getLocation().getDirection().normalize().multiply(1.4));
			}
		}
	}
	
	@SubscribeEvent
	public void onPlayerInteract(PlayerInteractEntityEvent e) {
		if (e.getPlayer().equals(getPlayer()) && swords.contains(getPlayer().getInventory().getItemInMainHand().getType())) {
			last = System.currentTimeMillis();
			if (!rightclickchecker.isRunning()) {
				rightclickchecker.start();
				getPlayer().setVelocity(getPlayer().getLocation().getDirection().normalize().multiply(1.4));
			}
		}
	}
	
}
