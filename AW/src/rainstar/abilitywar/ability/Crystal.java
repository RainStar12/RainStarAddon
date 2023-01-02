package rainstar.abilitywar.ability;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Note;
import org.bukkit.Note.Tone;
import org.bukkit.attribute.Attribute;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByBlockEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageModifier;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;

import daybreak.abilitywar.ability.AbilityBase;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.SubscribeEvent;
import daybreak.abilitywar.ability.SubscribeEvent.Priority;
import daybreak.abilitywar.ability.Tips;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.ability.Tips.Description;
import daybreak.abilitywar.ability.Tips.Difficulty;
import daybreak.abilitywar.ability.Tips.Level;
import daybreak.abilitywar.ability.Tips.Stats;
import daybreak.abilitywar.ability.decorator.ActiveHandler;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.list.mix.Mix;
import daybreak.abilitywar.utils.base.color.RGB;
import daybreak.abilitywar.utils.base.minecraft.entity.health.Healths;
import daybreak.abilitywar.utils.base.minecraft.nms.NMS;
import daybreak.abilitywar.utils.library.MaterialX;
import daybreak.abilitywar.utils.library.ParticleLib;
import daybreak.abilitywar.utils.library.PotionEffects;
import daybreak.abilitywar.utils.library.SoundLib;
import daybreak.abilitywar.utils.library.item.ItemLib;

@AbilityManifest(name = "크리스탈", rank = Rank.A, species = Species.OTHERS, explain = {
		"§7철괴 좌클릭 §8- §d생명 수정§f: 체력 1.5칸을 소모해 흡수 체력 1칸을 획득합니다.",
		" 흡수 체력량이 최대 체력의 절반 이하여야 사용 가능합니다.",
		"§7활 발사 §8- §b수정 화살§f: 화살 두 발을 소모해 수정 화살을 발사합니다.",
		" 수정 화살은 반드시 크리티컬이고, 나아가는 거리에 비례하여 거리 비례 대미지 및",
		" 타격한 적을 최대 5초간 구속시킵니다. 만약 다이아몬드가 있다면 하나를 소모해",
		" 수정 화살을 강화하여 더 빨리 날아가고 대상을 강하게 밀쳐냅니다.",
		"§7패시브 §8- §3수정 방벽§f: 흡수 체력을 가지고 있을 때 흡수 체력량을 넘는",
		" 피해량이 들어온다면, 오직 흡수 체력만이 소진됩니다.",
		"§8[§7HIDDEN§8] §5마탄의 사수§f: 정밀 조준 따위가 없어도 백발백중."
		},
		summarize = {
		"§7철괴 좌클릭§f으로 내 체력을 사용해 §e흡수 체력§f을 만들어",
		"§e흡수 체력§f이 있을 때 §e흡수 체력량§f보다도 많은 피해를 방어해줍니다.",
		"화살을 발사할 때 두 발을 소모하여 적중 대상을 느리게 만들고,",
		"거리 비례 피해를 추가로 주며 반드시 치명타가 발생합니다."
		})

@Tips(tip = {
        "활을 신중하게 잘 맞춰야 하는 원거리 딜러 및 탱커입니다. 20초마다 획득하는",
        "흡수 체력을 소지 중 대상을 활로 맞추면 대상에게 받는 피해량이 1칸을",
        "넘지 않게 되는 점, 도주를 저지하는 구속 효과, 100% 크리티컬의",
        "효과를 받을 수 있는 활이지만 화살을 2개씩 소모함을 주의하세요."
}, strong = {
        @Description(subject = "안정적인 화살 대미지", explain = {
                "기본적인 화살의 치명타는 확률이지만,",
                "치명타가 보장되기에 안정적으로 고대미지를 낼 수 있습니다."
        }),
        @Description(subject = "순간 높은 대미지를 내는 대상", explain = {
                "버서커나 넥스 등 한 방의 힘을 실린 공격을",
                "흡수 체력 한 칸의 피해로 무마시킬 수 있습니다."
        }),
        @Description(subject = "이동 속도가 높은 대상", explain = {
                "구속 디버프를 걸어 대상의 이동 속도를 늦춰보세요."
        })
}, weak = {
        @Description(subject = "다단히트", explain = {
                "수정 방벽은 최대 1칸까지의 피해로 줄여주는 것이지만,",
                "연속해서 대미지를 가하는 능력에게는 흡수 체력의 소모력이 빨라",
                "수정 방벽의 힘을 제대로 살리기 힘듭니다."
        })
}, stats = @Stats(offense = Level.ZERO, survival = Level.NINE, crowdControl = Level.THREE, mobility = Level.ZERO, utility = Level.ZERO), difficulty = Difficulty.NORMAL)

@SuppressWarnings("deprecation")
public class Crystal extends AbilityBase implements ActiveHandler {

	public Crystal(Participant participant) {
		super(participant);
	}
	
	private Player target;
	private double dist = 0;
	private final Set<Player> checktarget = new HashSet<>();
	private final Map<Arrow, Location> arrowlocation = new HashMap<>();
	private int stack = 0;
    
	public boolean ActiveSkill(Material material, ClickType clicktype) {
	    if (material.equals(Material.IRON_INGOT) && clicktype.equals(ClickType.LEFT_CLICK)) {
	    	float yellowheart = NMS.getAbsorptionHearts(getPlayer());
	    	if (yellowheart <= (getPlayer().getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue() / 2)) {
	    		if (getPlayer().getHealth() > 3) {
		    		Healths.setHealth(getPlayer(), getPlayer().getHealth() - 3);
		    		NMS.setAbsorptionHearts(getPlayer(), yellowheart + 2);
		    		SoundLib.BLOCK_BREWING_STAND_BREW.playSound(getPlayer().getLocation(), 1, 1.5f);
		    		ParticleLib.SPELL_MOB.spawnParticle(getPlayer().getLocation(), RGB.YELLOW);
			    	return true;
	    		} else getPlayer().sendMessage("§c[§4!§c] §d체력§f이 모자랍니다.");
	    	} else getPlayer().sendMessage("§c[§4!§c] §e흡수 체력§f이 너무 많습니다.");
	    }
		return false;
	}
    
	@SubscribeEvent
	public void onEntityShootBow(EntityShootBowEvent e) {
		if (e.getEntity().equals(getPlayer())) {
			if (!getPlayer().getGameMode().equals(GameMode.CREATIVE) && (!e.getBow().hasItemMeta() || 
					!e.getBow().getItemMeta().hasEnchant(Enchantment.ARROW_INFINITE))) {
				ItemLib.removeItem(getPlayer().getInventory(), Material.ARROW, 2);
			}
		}
	}
	
	@SubscribeEvent(priority = Priority.HIGHEST)
	public void onEntityDamage(EntityDamageEvent e) {
    	if (e.getEntity().equals(getPlayer()) && !getPlayer().isDead()) {
    		float yellowheart = NMS.getAbsorptionHearts(getPlayer());
    		if (yellowheart != 0) {
    			float lostyellow = (float) e.getDamage(DamageModifier.ABSORPTION);
        		if (yellowheart + lostyellow == 0) {
            		if (e.getFinalDamage() > 0) {
            			e.setDamage(0);
                    	NMS.setAbsorptionHearts(getPlayer(), 0);
                	}
        		}
    		}
    	}
	}
	
	@SubscribeEvent
	public void onEntityDamageByBlock(EntityDamageByBlockEvent e) {
		onEntityDamage(e);
	}
    
    @SubscribeEvent
    public void onEntityDamageByEntity(EntityDamageByEntityEvent e) {
    	onEntityDamage(e);
    	if (NMS.isArrow(e.getDamager()) && e.getEntity() instanceof Player) {
    		target = (Player) e.getEntity();
    		Arrow arrow = (Arrow) e.getDamager();
    		if (arrow != null) {    		
    			if (getPlayer().equals(arrow.getShooter()) && !e.getEntity().equals(getPlayer())) {
    				if (arrowlocation.get(arrow) != null) dist = target.getLocation().distanceSquared(arrowlocation.get(arrow));
    				else dist = target.getLocation().distanceSquared(getPlayer().getLocation());
	    			if (dist < 25) {
	    				PotionEffects.SLOW.addPotionEffect(target.getPlayer(), 20, 0, true);
	    			} else if (dist < 100) {
	    				PotionEffects.SLOW.addPotionEffect(target.getPlayer(), 40, 0, true);
	    				e.setDamage(e.getDamage() * 1.1);
	    			} else if (dist < 625) {
	    				PotionEffects.SLOW.addPotionEffect(target.getPlayer(), 60, 0, true);
	    				e.setDamage(e.getDamage() * 1.2);
	    			} else if (dist < 1600) {
	    				PotionEffects.SLOW.addPotionEffect(target.getPlayer(), 80, 0, true);
	    				e.setDamage(e.getDamage() * 1.3);
	    			} else if (dist < 10000) {
	    				PotionEffects.SLOW.addPotionEffect(target.getPlayer(), 100, 0, true);
	    				e.setDamage(e.getDamage() * 1.4);
	    			} else if (dist >= 10000) {
	    				AbilityBase ab = getParticipant().getAbility();
	    				if (ab.getClass().equals(Mix.class)) {
	    					Mix mix = (Mix) ab;
	    					if (!mix.getFirst().getClass().equals(PrecisionAiming.class) && !mix.getSecond().getClass().equals(PrecisionAiming.class)) {
			    				if (checktarget.contains(target)) {
				    				PotionEffects.SLOW.addPotionEffect(target.getPlayer(), 200, 1, true);
				    				e.setDamage(e.getDamage() * 2);
					    			getPlayer().sendMessage("§8[§7HIDDEN§8] §f100m 밖의 적을 2연속으로 맞히셨습니다.");
					    			getPlayer().sendMessage("§8[§7HIDDEN§8] §5마탄의 사수§f를 달성하였습니다.");
					    			SoundLib.UI_TOAST_CHALLENGE_COMPLETE.playSound(getPlayer());	
					    			checktarget.clear();
			    				} else {
			    					checktarget.clear();
			    					checktarget.add(target);
			    				}
	    					}
	    				} else {
		    				if (checktarget.contains(target)) {
			    				PotionEffects.SLOW.addPotionEffect(target.getPlayer(), 200, 1, true);
			    				e.setDamage(e.getDamage() * 2);
				    			getPlayer().sendMessage("§8[§7HIDDEN§8] §f100m 밖의 적을 2연속으로 맞히셨습니다.");
				    			getPlayer().sendMessage("§8[§7HIDDEN§8] §5마탄의 사수§f를 달성하였습니다.");
				    			SoundLib.UI_TOAST_CHALLENGE_COMPLETE.playSound(getPlayer());
				    			checktarget.clear();
		    				} else {
		    					checktarget.clear();
		    					checktarget.add(target);
		    				}	
	    				}
	    			}
    			}	
    		} else {
				PotionEffects.SLOW.addPotionEffect(target.getPlayer(), 20, 0, true);
    		}
    	}
    }
    
	@SubscribeEvent
	public void onProjectileHit(ProjectileHitEvent e) {
		if (NMS.isArrow(e.getEntity())) {
			Arrow arrow = (Arrow) e.getEntity();
			if (getPlayer().equals(e.getEntity().getShooter())) {
	    		ParticleLib.ITEM_CRACK.spawnParticle(arrow.getLocation(), 0, 0, 0, 35, 0.2, MaterialX.PRISMARINE_CRYSTALS);
	    		ParticleLib.ITEM_CRACK.spawnParticle(arrow.getLocation(), 0, 0, 0, 15, 0.2, MaterialX.DIAMOND);
			}	
		}
		if (e.getHitEntity() == null && !checktarget.isEmpty()) {
			checktarget.clear();
		}
	}
    
    @SubscribeEvent
	public void onProjectileLaunch(ProjectileLaunchEvent e) {
    	if (getPlayer().equals(e.getEntity().getShooter()) && NMS.isArrow(e.getEntity())) {
    		Arrow arrow = (Arrow) e.getEntity();
    		arrow.setCritical(true);
    		arrowlocation.put(arrow, getPlayer().getLocation());
    		if (getPlayer().getInventory().contains(Material.DIAMOND)) {
    			ItemLib.removeItem(getPlayer().getInventory(), Material.DIAMOND, 1);
    			arrow.setVelocity(arrow.getVelocity().multiply(1.2));
    			arrow.setKnockbackStrength(arrow.getKnockbackStrength() + 1);
        		stack++;
    			double temp = (double) stack;
    			int soundnumber = (int) (temp - (Math.ceil(temp / SOUND_RUNNABLES.size()) - 1) * SOUND_RUNNABLES.size()) - 1;
	    		SOUND_RUNNABLES.get(soundnumber).run();	
    		} else {
        		SoundLib.ENTITY_ENDER_EYE_DEATH.playSound(arrow.getLocation(), 1, 1.6f);
    		}
    	}
    }
    
	private final List<Runnable> SOUND_RUNNABLES = Arrays.asList(
		() -> {
			SoundLib.CHIME.playInstrument(getPlayer().getLocation(), Note.natural(1, Tone.D));
		},
		() -> {
			SoundLib.CHIME.playInstrument(getPlayer().getLocation(), Note.natural(1, Tone.B));
		},
		() -> {
			SoundLib.CHIME.playInstrument(getPlayer().getLocation(), Note.natural(1, Tone.A));
		},
		() -> {
			SoundLib.CHIME.playInstrument(getPlayer().getLocation(), Note.natural(1, Tone.G));
		},
		() -> {
			SoundLib.CHIME.playInstrument(getPlayer().getLocation(), Note.natural(1, Tone.A));
		},
		() -> {
			SoundLib.CHIME.playInstrument(getPlayer().getLocation(), Note.natural(1, Tone.G));
		},
		() -> {
			SoundLib.CHIME.playInstrument(getPlayer().getLocation(), Note.natural(1, Tone.A));
		},
		() -> {
			SoundLib.CHIME.playInstrument(getPlayer().getLocation(), Note.natural(1, Tone.B));
		},
		() -> {
			SoundLib.CHIME.playInstrument(getPlayer().getLocation(), Note.natural(0, Tone.D));
		},
		() -> {
			SoundLib.CHIME.playInstrument(getPlayer().getLocation(), Note.natural(0, Tone.D));
		},
		() -> {
			SoundLib.CHIME.playInstrument(getPlayer().getLocation(), Note.natural(1, Tone.G));
		},
		() -> {
			SoundLib.CHIME.playInstrument(getPlayer().getLocation(), Note.natural(1, Tone.A));
		},
		() -> {
			SoundLib.CHIME.playInstrument(getPlayer().getLocation(), Note.natural(1, Tone.G));
		},
		() -> {
			SoundLib.CHIME.playInstrument(getPlayer().getLocation(), Note.natural(1, Tone.A));
		},
		() -> {
			SoundLib.CHIME.playInstrument(getPlayer().getLocation(), Note.natural(1, Tone.B));
		},
		() -> {
			SoundLib.CHIME.playInstrument(getPlayer().getLocation(), Note.natural(1, Tone.B));
		},
		() -> {
			SoundLib.CHIME.playInstrument(getPlayer().getLocation(), Note.natural(1, Tone.D));
		},
		() -> {
			SoundLib.CHIME.playInstrument(getPlayer().getLocation(), Note.natural(1, Tone.G));
		},
		() -> {
			SoundLib.CHIME.playInstrument(getPlayer().getLocation(), Note.natural(1, Tone.B));
		},
		() -> {
			SoundLib.CHIME.playInstrument(getPlayer().getLocation(), Note.natural(1, Tone.C));
		},
		() -> {
			SoundLib.CHIME.playInstrument(getPlayer().getLocation(), Note.natural(1, Tone.B));
		},
		() -> {
			SoundLib.CHIME.playInstrument(getPlayer().getLocation(), Note.natural(1, Tone.G));
		},
		() -> {
			SoundLib.CHIME.playInstrument(getPlayer().getLocation(), Note.natural(0, Tone.E));
		},
		() -> {
			SoundLib.CHIME.playInstrument(getPlayer().getLocation(), Note.natural(1, Tone.G));
		},
		() -> {
			SoundLib.CHIME.playInstrument(getPlayer().getLocation(), Note.natural(1, Tone.B));
		}
	);

}