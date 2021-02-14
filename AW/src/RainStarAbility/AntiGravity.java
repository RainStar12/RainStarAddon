package RainStarAbility;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Projectile;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import daybreak.abilitywar.ability.AbilityBase;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.SubscribeEvent;
import daybreak.abilitywar.ability.Tips;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.ability.Tips.Description;
import daybreak.abilitywar.ability.Tips.Difficulty;
import daybreak.abilitywar.ability.Tips.Level;
import daybreak.abilitywar.ability.Tips.Stats;
import daybreak.abilitywar.ability.decorator.ActiveHandler;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.AbstractGame.Participant.ActionbarNotification.ActionbarChannel;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;

@AbilityManifest(name = "반중력", rank = Rank.C, species = Species.OTHERS, explain = {
		"자신이 발사하는 모든 발사체가 1틱 후 정지합니다.",
		"철괴 좌클릭 시, 정지된 모든 발사체를 정지 해제합니다.",
		"철괴 우클릭 시, 정지까지 걸리는 시간을 조절 가능합니다.",
		"이 능력은 게임 시작 때 §5반중력 실험 키트§f를 제공합니다.",
		"§5반중력 실험 키트 §7: §f화살 1세트, 투척 §2독§f/§4고통§f 1 포션 각각 2개"
		})

@Tips(tip = {
        "발사체를 멈춰둘 수 있다는 것을 잘 살리셔야 합니다. 이를테면 엔더 진주를",
        "활용하여 공중에 멈춰두고 원하는 때에 텔레포트가 가능하고,",
        "포션이나 화살 등을 멈춰두어 대상을 한번에 공격이 가능합니다.",
        "다만 화살이 제대로 나아가지 못하기 때문에 기본 화살보다 사용이 어렵습니다."
}, strong = {
        @Description(subject = "트랩", explain = {
                "능력을 모르는 상대를 미리 준비해 둔 화살 및 포션을",
                "멈춰둔 트랩을 이용하여 유인해 공격할 수 있습니다."
        })
}, weak = {
        @Description(subject = "원거리전", explain = {
                "화살이 멀리 나아가기 힘들다는 점 때문에 원거리 전에 취약합니다.",
                "되도록이면 상대를 내게 쫓아오는 쪽으로 유도하세요."
        })
}, stats = @Stats(offense = Level.ZERO, survival = Level.ZERO, crowdControl = Level.ZERO, mobility = Level.ZERO, utility = Level.TWO), difficulty = Difficulty.HARD)

public class AntiGravity extends AbilityBase implements ActiveHandler {
	
	public AntiGravity(Participant participant) {
		super(participant);
	}
	
	ActionbarChannel ac = newActionbarChannel();
	
	private final Map<Projectile, Vector> velocityMap = new HashMap<>();
	private static final Vector zerov = new Vector(0, 0, 0);
	private boolean arrows = true;
	private int timer = 1;
	
	@Override
	protected void onUpdate(Update update) {
		if (update == Update.RESTRICTION_CLEAR && arrows == true) {
			
			ItemStack poison = new ItemStack(Material.SPLASH_POTION, 2);
			PotionMeta pmeta = (PotionMeta) poison.getItemMeta();
			ItemStack instdmg = new ItemStack(Material.SPLASH_POTION, 2);
			PotionMeta imeta = (PotionMeta) instdmg.getItemMeta();
			ItemStack arrow = new ItemStack(Material.ARROW, 64);
			ItemMeta ameta = arrow.getItemMeta();
			
			pmeta.addCustomEffect(new PotionEffect(PotionEffectType.POISON, 200, 0), true);
			pmeta.setColor(PotionEffectType.POISON.getColor());
			pmeta.setDisplayName("§5반중력 실험 키트 §7- §2독 포션");
			imeta.addCustomEffect(new PotionEffect(PotionEffectType.HARM, 1, 0), true);
			imeta.setColor(PotionEffectType.HARM.getColor());
			imeta.setDisplayName("§5반중력 실험 키트 §7- §4고통 포션");
			ameta.setDisplayName("§5반중력 실험 키트 §7- §f화살");
			ameta.addEnchant(Enchantment.MENDING, 1, true);
			
			poison.setItemMeta(pmeta);
			instdmg.setItemMeta(imeta);
			arrow.setItemMeta(ameta);
			
			getPlayer().getInventory().addItem(poison);
			getPlayer().getInventory().addItem(instdmg);
			getPlayer().getInventory().addItem(arrow);
			arrows = false;
			
			ac.update("§b정지 시간 §f: " + timer + "틱 후");
		}
		
		if (update == Update.ABILITY_DESTROY || update == Update.RESTRICTION_SET) {
			for (Entry<Projectile, Vector> entry : velocityMap.entrySet()) {
				velocityMap.forEach(Projectile::setVelocity);
				entry.getKey().setGravity(true);
			}
			velocityMap.clear();
		}
	}
	
	@SubscribeEvent
	 public void onProjectileLaunch(ProjectileLaunchEvent e) {
		if (getPlayer().equals(e.getEntity().getShooter())) {
			
				new AbilityTimer(timer) {
					
					@Override
					protected void run(int count) {
					}
					
					@Override
					protected void onEnd() {
						velocityMap.put(e.getEntity(), e.getEntity().getVelocity());
						e.getEntity().setGravity(false);
						e.getEntity().setVelocity(zerov);
					}

					@Override
					protected void onSilentEnd() {
						velocityMap.put(e.getEntity(), e.getEntity().getVelocity());
						e.getEntity().setGravity(false);
						e.getEntity().setVelocity(zerov);
					}
					
				}.setPeriod(TimeUnit.TICKS, 1).start();
		}
	}
	
	public boolean ActiveSkill(Material material, AbilityBase.ClickType clicktype) {
	    if (material.equals(Material.IRON_INGOT) && clicktype.equals(AbilityBase.ClickType.LEFT_CLICK)) {
	    	
			for (Entry<Projectile, Vector> entry : velocityMap.entrySet()) {
					velocityMap.forEach(Projectile::setVelocity);
					entry.getKey().setGravity(true);
			}
			velocityMap.clear();
	    }
	    if (material.equals(Material.IRON_INGOT) && clicktype.equals(AbilityBase.ClickType.RIGHT_CLICK)) {
	    	if (timer == 1) {
	    		timer = 3;
	    	} else if (timer == 3) {
	    		timer = 5;
	    	} else if (timer == 5) {
	    		timer = 1;
	    	}
	    	ac.update("§b정지 시간 §f: " + timer + "틱 후");
	    }
	    return false;
	}
}