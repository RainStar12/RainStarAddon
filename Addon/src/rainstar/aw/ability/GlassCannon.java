package rainstar.aw.ability;

import daybreak.abilitywar.ability.AbilityBase;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.ability.SubscribeEvent;
import daybreak.abilitywar.ability.Tips;
import daybreak.abilitywar.ability.Tips.Description;
import daybreak.abilitywar.ability.Tips.Difficulty;
import daybreak.abilitywar.ability.Tips.Level;
import daybreak.abilitywar.ability.Tips.Stats;
import daybreak.abilitywar.config.ability.AbilitySettings.SettingObject;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.AbstractGame.Participant.ActionbarNotification.ActionbarChannel;
import daybreak.abilitywar.utils.library.MaterialX;
import daybreak.abilitywar.utils.library.ParticleLib;
import daybreak.abilitywar.utils.library.SoundLib;
import daybreak.google.common.collect.ImmutableSet;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;

import java.util.Set;

@AbilityManifest(
		name = "유리 대포",
		rank = Rank.B, 
		species = Species.OTHERS, 
		explain = {
		"검 우클릭으로 유리 대포 모드를 켜거나 끌 수 있습니다.",
		"§c[§b유리 대포§c] §f주는 피해, 받는 피해 모두 §c§l$[DAMAGE]§f 증가합니다."
		},
		summarize = {
		"검 우클릭으로 유리 대포 모드를 켜거나 끌 수 있습니다.",
		"§c[§b유리 대포§c] §f주는 피해, 받는 피해 모두 §c§l$[DAMAGE]§f 증가합니다."
		})

@Tips(tip = {
        "강한 공격력을 가지는 대신 그만큼의 추가 피해를 받을 각오를 해야하는",
        "능력으로, 추가 대미지와 추가 피해량이 동일하기 때문에",
        "본인의 PVP 실력에 큰 영향을 받습니다."
}, strong = {
        @Description(subject = "높은 공격력", explain = {
                "최대 7까지의 높은 공격력을 상대에게 가할 수 있습니다.",
                "일반 다이아몬드 칼 기준, 2배의 대미지를 가하는 셈입니다."
        }),
        @Description(subject = "급습", explain = {
                "급습으로 대상보다 1회의 타격이라도 이득을 보면",
                "높은 대미지 덕에 상대와의 격차가 커집니다."
        })
}, weak = {
        @Description(subject = "낮은 방어력", explain = {
                "대미지를 높이는 대신 그만큼의 대미지를 받을 수 있기에,",
                "그 점을 감안하고 대미지를 잘 조절해서 싸워야 합니다."
        })
}, stats = @Stats(offense = Level.NINE, survival = Level.ZERO, crowdControl = Level.ZERO, mobility = Level.ZERO, utility = Level.ZERO), difficulty = Difficulty.HARD)

public class GlassCannon extends AbilityBase {
	
	public GlassCannon(Participant participant) {
		super (participant);
	}
	
	public static final SettingObject<Double> DAMAGE = 
			abilitySettings.new SettingObject<Double>(GlassCannon.class, "damage", 5.0,
            "# 유리 대포 대미지") {
        @Override
        public boolean condition(Double value) {
            return value >= 0;
        }
    };
    
	private static final Set<Material> swords;
	private final double damage = DAMAGE.getValue();
	private boolean glasscannon = false;
	private final ActionbarChannel ac = newActionbarChannel();
	
	static {
		if (MaterialX.NETHERITE_SWORD.isSupported()) {
			swords = ImmutableSet.of(MaterialX.WOODEN_SWORD.getMaterial(), Material.STONE_SWORD, Material.IRON_SWORD, MaterialX.GOLDEN_SWORD.getMaterial(), Material.DIAMOND_SWORD, MaterialX.NETHERITE_SWORD.getMaterial());
		} else {
			swords = ImmutableSet.of(MaterialX.WOODEN_SWORD.getMaterial(), Material.STONE_SWORD, Material.IRON_SWORD, MaterialX.GOLDEN_SWORD.getMaterial(), Material.DIAMOND_SWORD);
		}
	}
	
	@SubscribeEvent
	public void onPlayerInteract(PlayerInteractEvent e) {
		if (e.getPlayer().equals(getPlayer()) && (e.getAction() == Action.RIGHT_CLICK_BLOCK || e.getAction() == Action.RIGHT_CLICK_AIR) && e.getItem() != null && swords.contains(e.getItem().getType())) {
			glasscannon = !glasscannon;
			ac.update(glasscannon ? "§c[§b유리 대포§c]" : "§8[§7유리 대포§8]");
		}
	}
	
	@SubscribeEvent
	public void onEntityDamageByEntity(EntityDamageByEntityEvent e) {		
		Player damager = null;
		if (e.getDamager() instanceof Projectile) {
			Projectile projectile = (Projectile) e.getDamager();
			if (projectile.getShooter() instanceof Player) damager = (Player) projectile.getShooter();
		} else if (e.getDamager() instanceof Player) damager = (Player) e.getDamager();
		
		if (glasscannon && (getPlayer().equals(damager) || e.getEntity().equals(getPlayer()))) {
			e.setDamage(e.getDamage() + damage);
			SoundLib.BLOCK_GLASS_BREAK.playSound(getPlayer(), 1, 1.5f);
			ParticleLib.BLOCK_CRACK.spawnParticle(e.getEntity().getLocation(), 0, 2, 0, 30, 1, MaterialX.GLASS);
		}
	}
}