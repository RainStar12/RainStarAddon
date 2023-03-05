package rainstar.abilitywar.ability;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.attribute.Attribute;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffectType;

import daybreak.abilitywar.ability.AbilityBase;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.ability.SubscribeEvent;
import daybreak.abilitywar.config.ability.AbilitySettings.SettingObject;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.manager.effect.Fear;
import daybreak.abilitywar.utils.base.Formatter;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.base.minecraft.damage.Damages;
import daybreak.abilitywar.utils.base.minecraft.entity.health.Healths;
import daybreak.abilitywar.utils.base.minecraft.nms.NMS;
import daybreak.abilitywar.utils.base.minecraft.version.ServerVersion;
import daybreak.abilitywar.utils.base.random.Random;
import daybreak.abilitywar.utils.library.MaterialX;
import daybreak.abilitywar.utils.library.ParticleLib;
import daybreak.abilitywar.utils.library.SoundLib;

@AbilityManifest(name = "훌륭한 대화수단", rank = Rank.S, species = Species.OTHERS, explain = {
		"§6벽돌§f을 들고 있는 동안 게이지바가 나타나, 효과의 세기가 조절됩니다.",
		"§6벽돌§f로 타격 시 대상의 최대 체력 비례 고정 피해§8(§7최대 $[MAX_TRUEDMG]%§8)§f를 입히고,",
		"§b§n공포§8(§7최대 $[MAX_FEAR]초§8)§f에 빠뜨립니다. 위 효과는 §4치명타§f가 적용됩니다. $[COOLDOWN]",
		"§a[§e능력 제공자§a] §cDdun_kim"
		},
		summarize = {
		""
		})
public class GreatCommunicator extends AbilityBase {

    public GreatCommunicator(Participant participant) {
        super(participant);
    }
    
	public static final SettingObject<Integer> MAX_TRUEDMG = 
			abilitySettings.new SettingObject<Integer>(GreatCommunicator.class, "max-true-damage", 15,
			"# 최대 고정 피해량 배율", "# 단위: %") {

		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}

	};
	
	public static final SettingObject<Double> MAX_FEAR = 
			abilitySettings.new SettingObject<Double>(GreatCommunicator.class, "max-fear", 2.5,
			"# 최대 공포 시간") {

		@Override
		public boolean condition(Double value) {
			return value >= 0;
		}

	};
	
	public static final SettingObject<Integer> COOLDOWN = 
			abilitySettings.new SettingObject<Integer>(GreatCommunicator.class, "cooldown", 40,
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
	
	@SuppressWarnings("serial")
	private List<String> lores = new ArrayList<String>() {
		{
			add("§7");
			add("§7가끔은 이거로 하는 §4대화§7가 빠르죠...");
		}
	};
	
	@Override
	protected void onUpdate(AbilityBase.Update update) {
	    if (update == AbilityBase.Update.RESTRICTION_CLEAR) {
	    	attackCooldownChecker.start();
	    	handchecker.start();
	    	if (onetime) {
				ItemStack brick = new ItemStack(MaterialX.BRICK.getMaterial(), 1);
				ItemMeta brickmeta = brick.getItemMeta();
				brickmeta.setDisplayName("§c§l훌륭한 대화수단");
				brickmeta.addEnchant(Enchantment.DAMAGE_ALL, 5, true);
				brickmeta.addEnchant(Enchantment.MENDING, 1, true);
				brickmeta.setLore(lores);
				brickmeta.setUnbreakable(true);
				brick.setItemMeta(brickmeta);
				getPlayer().getInventory().addItem(brick);
	    		onetime = false;
	    	}
	    }
	}
	
	private final double maxtruedmg = MAX_TRUEDMG.getValue() * 0.01;
	private final int maxfear = (int) (MAX_FEAR.getValue() * 20);
	private final Cooldown cooldown = new Cooldown(COOLDOWN.getValue());
	private final Random random = new Random();
	private double speed = 0.01 + (random.nextInt(61) * 0.001);
	private BossBar bossBar = null;
	private double nowcharge = 0;
	private boolean reverse = false;
	private boolean onetime = true;
	
    private boolean attackCooldown = false;
	
	private final AbilityTimer attackCooldownChecker = new AbilityTimer() {
		
		@Override
		public void run(int count) {
			if (NMS.getAttackCooldown(getPlayer()) > 0.848 && attackCooldown) attackCooldown = false;
			else if (NMS.getAttackCooldown(getPlayer()) <= 0.848 && !attackCooldown) attackCooldown = true;
		}
		
	}.setPeriod(TimeUnit.TICKS, 1).register();
	
	@SuppressWarnings("deprecation")
	public static boolean isCriticalHit(Player p, boolean attackcool) {
		return (!p.isOnGround() && p.getFallDistance() > 0.0F && 
	      !p.getLocation().getBlock().isLiquid() &&
	      attackcool == false &&
	      !p.isInsideVehicle() && !p.isSprinting() && p
	      .getActivePotionEffects().stream().noneMatch(pe -> (pe.getType() == PotionEffectType.BLINDNESS)));
	}
	
    public AbilityTimer handchecker = new AbilityTimer() {

    	@Override
    	public void onStart() {
    		bossBar = Bukkit.createBossBar("§c게이지", BarColor.RED, BarStyle.SEGMENTED_10);
    		bossBar.setProgress(0);
    		if (ServerVersion.getVersion() >= 10) bossBar.setVisible(true);
    	}
    	
        @Override
        public void run(int count) {
            if (getPlayer().getInventory().getItemInMainHand().getType().equals(MaterialX.BRICK.getMaterial()) && !cooldown.isRunning()) {
        		bossBar.addPlayer(getPlayer());
        		nowcharge = reverse ? Math.max(0, nowcharge - speed) : Math.min(1, nowcharge + speed);
        		if (nowcharge == 1 || nowcharge == 0) reverse = !reverse;
        		bossBar.setProgress(nowcharge);
        		bossBar.setColor(nowcharge >= 0.9 ? BarColor.PURPLE : BarColor.RED);
            } else {
            	bossBar.removePlayer(getPlayer());
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
    
    @SubscribeEvent
    public void onEntityDamageByEntity(EntityDamageByEntityEvent e) {
    	if (!cooldown.isRunning() && getPlayer().equals(e.getDamager()) && e.getEntity() instanceof Player && e.getCause().equals(DamageCause.ENTITY_ATTACK) && getPlayer().getInventory().getItemInMainHand().getType().equals(MaterialX.BRICK.getMaterial())) {
    		Player player = (Player) e.getEntity();
    		double damage = player.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue() * maxtruedmg * nowcharge * (isCriticalHit(getPlayer(), attackCooldown) ? 1.5 : 1);
    		if (Damages.canDamage(player, DamageCause.ENTITY_ATTACK, damage)) {
    			Healths.setHealth(player, Math.max(1, player.getHealth() - damage));
    			Fear.apply(getGame().getParticipant(player), TimeUnit.TICKS, (int) (maxfear * nowcharge  * (isCriticalHit(getPlayer(), attackCooldown) ? 1.5 : 1)), getPlayer());
    			speed = 0.01 + (random.nextInt(61) * 0.001);
        		cooldown.start();
    			SoundLib.BLOCK_ANVIL_LAND.playSound(player.getLocation(), 1, 0.7f);
        		ParticleLib.CRIT.spawnParticle(player.getLocation().add(0, 1, 0), .3f, .3f, .3f, 100, 1);
        		if (isCriticalHit(getPlayer(), attackCooldown)) ParticleLib.CRIT_MAGIC.spawnParticle(player.getLocation().add(0, 1, 0), .3f, .3f, .3f, 100, 1);
    		}
    	}
    }

}