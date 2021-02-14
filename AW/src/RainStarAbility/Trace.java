package RainStarAbility;

import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;

import daybreak.abilitywar.ability.AbilityBase;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.ability.SubscribeEvent;
import daybreak.abilitywar.ability.AbilityBase.AbilityTimer;
import daybreak.abilitywar.ability.AbilityBase.ClickType;
import daybreak.abilitywar.ability.decorator.ActiveHandler;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.base.minecraft.nms.NMS;

@AbilityManifest(name = "트레이스", rank = Rank.A, species = Species.HUMAN, explain = {
		"§7철괴 타격 §8- §2특수 추적기§f: 철괴를 들고 다른 플레이어를 타격하여",
		" 특수 추적기를 대상에게 부착시킵니다. 특수 추적기는 단 하나만 한 대상에게",
		" 부착 가능하고, 다른 플레이어를 재타격 시 기본 추적기는 사라집니다.",
		"§7철괴 우클릭 §8- §a감시§f: 특수 추적기를 부착한 대상의 시야를 볼 수 있습니다.",
		" 다시 철괴 우클릭을 하면 감시 모드에서 나올 수 있습니다.",
		"§7철괴 좌클릭 §8- §2전자전§f: 특수 추적기의 EMP를 폭파시켜 주변 10칸 내",
		" 모든 플레이어를 전자 마비 상태로 만듭니다. $[COOLDOWN_CONFIG]",
		" 이후 자신은 특수 추적기가 있던 위치로 텔레포트합니다.",
		"§7패시브 §8- §a지피지기§f: 추적기를 부착한 적의 체력을 알 수 있습니다.",
		" 또한 대상이 능력을 사용할 때마다 감지할 수 있습니다."
		})

public class Trace extends AbilityBase implements ActiveHandler {
	
	public Trace(Participant participant) {
		super(participant);
	}
	
	protected void onUpdate(Update update) {
	    if (update == AbilityBase.Update.RESTRICTION_CLEAR) {
	    	if (target != null) {
				passive.start();
	    	}
	    }
	}
	
	private Player target;
	private boolean monitoring = false;
	
	private final AbilityTimer passive = new AbilityTimer() {
		
    	@Override
		public void run(int count) {
    		
    	}
    	
	}.setPeriod(TimeUnit.TICKS, 1).register();
	
	@SubscribeEvent
	public void onEntityDamageByEntity(EntityDamageByEntityEvent e) {
		if (e.getDamager().equals(getPlayer()) && e.getEntity() instanceof Player) {
			target = (Player) e.getEntity();
		}
	}
	
	@SubscribeEvent()
	public void onPlayerInteract(PlayerInteractEvent e) {
		if (e.getPlayer().equals(getPlayer()) && monitoring) {
			e.setCancelled(true);
		}
	}
	
	@SubscribeEvent
	public void onPlayerInteractEntity(PlayerInteractEntityEvent e) {
		if (e.getPlayer().equals(getPlayer()) && monitoring) {
			e.setCancelled(true);
		}
	}
	
	@SubscribeEvent
	public void onPlayerInteractAtEntity(PlayerInteractAtEntityEvent e) {
		if (e.getPlayer().equals(getPlayer()) && monitoring) {
			e.setCancelled(true);
		}
	}
	
	
	public boolean ActiveSkill(Material material, ClickType clicktype) {
	    if (material.equals(Material.IRON_INGOT) && clicktype.equals(ClickType.RIGHT_CLICK)) {
	    	if (target != null) {
	    		if (monitoring) {
			    	NMS.setCamera(getPlayer(), getPlayer());
			    	monitoring = false;	
	    		} else {
			    	NMS.setCamera(getPlayer(), target);
			    	monitoring = true;	
	    		}
	    	}
	    }
	    return false;
	}

}
