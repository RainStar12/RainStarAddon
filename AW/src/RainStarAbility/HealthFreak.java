package RainStarAbility;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.attribute.AttributeModifier.Operation;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent.RegainReason;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import RainStarEffect.SuperRegen;
import daybreak.abilitywar.ability.AbilityBase;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.SubscribeEvent;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.config.ability.AbilitySettings.SettingObject;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.base.minecraft.entity.health.Healths;
import daybreak.abilitywar.utils.base.minecraft.version.ServerVersion;
import daybreak.abilitywar.utils.library.ParticleLib;
import daybreak.abilitywar.utils.library.SoundLib;
import daybreak.abilitywar.utils.library.item.ItemLib;
import kotlin.ranges.RangesKt;

@AbilityManifest(name = "헬창", rank = Rank.S, species = Species.OTHERS, explain = {
		"웅크리고 있을 때 회복 속도가 빨라지고 공격력이 강해집니다.",
		"오랫동안 연속해 웅크리고 있을수록 효과들이 점점 더 강해집니다.",
		"웅크리지 않은 채 움직일 경우 근손실이 옵니다.",
		"적을 처치할 때마다 프로틴을 획득하고 소모 시 여러 가지 버프를 획득합니다.",
		"§b[§7아이디어 제공자§b] §6_Choco_pie"},
		summarize = {
		"웅크리고 있을 때 회복 속도가 빨라지고 공격력이 강해집니다.",
		"누군가를 죽이면 프로틴을 얻어 소모 시 여러 가지 버프를 획득합니다."
		})

public class HealthFreak extends AbilityBase {
	
	public HealthFreak(Participant participant) {
		super(participant);
	}
	
	public static final SettingObject<Integer> MAX_DAMAGE = 
			abilitySettings.new SettingObject<Integer>(HealthFreak.class, "max-damage", 5,
			"# 최대 대미지",
			"# 최대 대미지가 늘어나는 만큼",
			"# 회복 속도도 같이 늘어납니다.") {

		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}

	};
	
	public static final SettingObject<Integer> PROTEIN_DURATION = 
			abilitySettings.new SettingObject<Integer>(HealthFreak.class, "protein-duration", 30,
			"# 프로틴 효과의 지속 시간") {

		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}

	};
	
	protected void onUpdate(AbilityBase.Update update) {
		if (update == Update.RESTRICTION_CLEAR) {
			addcounter.start();
		}
		if (update == Update.RESTRICTION_SET || update == Update.ABILITY_DESTROY) {
			getPlayer().getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).removeModifier(movespeed);
		}
	}
	
	private BossBar bossBar = null;
	private double addDamage = 0;
	private double longsneak = 0;
	private int maxDamage = MAX_DAMAGE.getValue();
	private int duration = PROTEIN_DURATION.getValue();
	private final DecimalFormat df = new DecimalFormat("0.00");
	private boolean onetime = true;
	private AttributeModifier movespeed = new AttributeModifier(UUID.randomUUID(), "movespeed", 0.2, Operation.ADD_NUMBER);
	
	private Set<Integer> proteins = new HashSet<>();
	
	@SuppressWarnings("serial")
	private List<String> lores = new ArrayList<String>() {
		{
			add("§7");
			add("§6지속 시간§7: §c" + duration + "§7초");
			add("§7최대 체력이 2칸 늘어납니다.");
			add("§7다른 회복 효과를 무시하고 매 초마다 체력을");
			add("§70.75만큼 회복하는 초회복 효과를 받습니다.");
			add("§7또한 근육이 빨리 붙고 근손실이 느려집니다.");
			add("§c프로틴의 주인이 사망하면, 효과를 얻을 수 없습니다.");
			add("§7원 주인: §e" + getPlayer().getName());
		}
	};
	
    private final AbilityTimer addcounter = new AbilityTimer() {
    	
    	@Override
    	public void onStart() {
    		bossBar = Bukkit.createBossBar("추가 피해량", BarColor.RED, BarStyle.SOLID);
    		bossBar.setProgress(0);
    		bossBar.addPlayer(getPlayer());
    		if (ServerVersion.getVersion() >= 10) bossBar.setVisible(true);
    	}
    	
    	@Override
		public void run(int count) {
    		bossBar.setTitle("§c추가 피해량 §7: §e" + df.format(addDamage));
    		bossBar.setProgress(RangesKt.coerceIn(addDamage / maxDamage, 0, 1));
    		if (getPlayer().isSneaking()) {
    			if (getParticipant().hasEffect(SuperRegen.registration)) addDamage = Math.min(maxDamage, addDamage + (0.04 + ((longsneak / maxDamage) * 0.06)));
    			else addDamage = Math.min(maxDamage, addDamage + (0.015 + ((longsneak / maxDamage) * 0.045)));
				final EntityRegainHealthEvent event = new EntityRegainHealthEvent(getPlayer(), (addDamage * 0.004), RegainReason.CUSTOM);
				Bukkit.getPluginManager().callEvent(event);
				if (!event.isCancelled()) {
	        		Healths.setHealth(getPlayer(), (getPlayer().getHealth() + (addDamage * 0.004)));	
				}
				longsneak = Math.min(maxDamage, longsneak + 0.032);
				if (count % 10 == 0 && addDamage < maxDamage) {
					SoundLib.BLOCK_CHORUS_FLOWER_GROW.playSound(getPlayer().getLocation(), 1, 0.7f);
					ParticleLib.TOTEM.spawnParticle(getPlayer().getLocation().clone().add(0, 1, 0), 0.5, 0.25, 0.5, 10, 0);
				}
    		} else {
    			longsneak = 0;
    			addDamage = Math.max(0, addDamage - 0.04);
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
    
    @SubscribeEvent(onlyRelevant = true)
    public void onPlayerMove(PlayerMoveEvent e) {
    	if (!getPlayer().isSneaking()) {
    		if (getParticipant().hasEffect(SuperRegen.registration)) addDamage = Math.max(0, addDamage - 0.005);
    		else addDamage = Math.max(0, addDamage - 0.03);
			getPlayer().getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).removeModifier(movespeed);
			onetime = true;
    	} else {
    		if (longsneak >= (maxDamage * 0.75) && onetime) {
    			getPlayer().getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).addModifier(movespeed);
    			onetime = false;
    		}
    	}
    }
    
    @SubscribeEvent
    public void onPlayerKill(PlayerDeathEvent e) {
    	if (e.getEntity().getKiller() != null) {
        	if (e.getEntity().getKiller().equals(getPlayer())) {
    			ItemStack protein = new ItemStack(Material.POTION, 1);
    			PotionMeta proteinmeta = (PotionMeta) protein.getItemMeta();
    			
    			proteinmeta.setDisplayName("§f§l프로틴");
    			proteinmeta.setColor(Color.WHITE);
    			
    			proteinmeta.addCustomEffect(new PotionEffect(PotionEffectType.SPEED, (duration * 20), 1), false);
    			
    			proteinmeta.setLore(lores);
    			
    			protein.setItemMeta(proteinmeta);
    			proteins.add(ItemLib.hashCode(protein));
    			
    			getPlayer().getInventory().addItem(protein);
        	}	
    	}
    }
    
	@SubscribeEvent
	public void onPlayerItemConsume(PlayerItemConsumeEvent e) {
		if (proteins.contains(ItemLib.hashCode(e.getItem()))) {
			new HealthUp(e.getPlayer()).start();
			SuperRegen.apply(getGame().getParticipant(e.getPlayer()), TimeUnit.SECONDS, duration);
			SoundLib.BLOCK_CHORUS_FLOWER_GROW.playSound(e.getPlayer().getLocation(), 1, 0.75f);
			SoundLib.BLOCK_CHORUS_FLOWER_GROW.playSound(e.getPlayer().getLocation(), 1, 0.75f);
			SoundLib.BLOCK_CHORUS_FLOWER_GROW.playSound(e.getPlayer().getLocation(), 1, 0.75f);
			ParticleLib.TOTEM.spawnParticle(e.getPlayer().getLocation().clone().add(0, 1, 0), 1, 0.5, 0.1, 30, 0.35);
		}
	}
    
    @SubscribeEvent
    public void onEntityDamageByEntity(EntityDamageByEntityEvent e) {
    	if (e.getDamager().equals(getPlayer())) {
    		e.setDamage(e.getDamage() + addDamage);
    	}
    }

    private class HealthUp extends AbilityTimer {
    	
    	private Player player;
    	private AttributeModifier health;
    	
    	private HealthUp(Player player) {
    		super(TaskType.REVERSE, duration * 20);
    		setPeriod(TimeUnit.TICKS, 1);
    		this.player = player;
    	}
    	
    	@Override
    	public void onStart() {
    		health = new AttributeModifier(UUID.randomUUID(), "health", 4, Operation.ADD_NUMBER);
    		player.getAttribute(Attribute.GENERIC_MAX_HEALTH).addModifier(health);
    	}
    	
    	@Override
    	public void onEnd() {
    		onSilentEnd();
    	}
    	
    	@Override
    	public void onSilentEnd() {
    		player.getAttribute(Attribute.GENERIC_MAX_HEALTH).removeModifier(health);
    	}
    	
    }
    
}