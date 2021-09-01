package RainStarAbility;

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

@AbilityManifest(name = "ũ����Ż", rank = Rank.A, species = Species.OTHERS, explain = {
		"��7ö�� ��Ŭ�� ��8- ��d���� ������f: ü�� 1.5ĭ�� �Ҹ��� ��� ü�� 1ĭ�� ȹ���մϴ�.",
		" ��� ü�·��� �ִ� ü���� ���� ���Ͽ��� ��� �����մϴ�.",
		"��7Ȱ �߻� ��8- ��b���� ȭ���f: ȭ�� �� ���� �Ҹ��� ���� ȭ���� �߻��մϴ�.",
		" ���� ȭ���� �ݵ�� ũ��Ƽ���̰�, ���ư��� �Ÿ��� ����Ͽ� �Ÿ� ��� ����� ��",
		" Ÿ���� ���� �ִ� 5�ʰ� ���ӽ�ŵ�ϴ�. ���� ���̾Ƹ�尡 �ִٸ� �ϳ��� �Ҹ���",
		" ���� ȭ���� ��ȭ�Ͽ� �� ���� ���ư��� ����� ���ϰ� ���ĳ��ϴ�.",
		"��7�нú� ��8- ��3���� �溮��f: ��� ü���� ������ ���� �� ��� ü�·��� �Ѵ�",
		" ���ط��� ���´ٸ�, ���� ��� ü�¸��� �����˴ϴ�.",
		"��8[��7HIDDEN��8] ��5��ź�� �����f: ���� ���� ������ ��� ��߹���."
		},
		summarize = {
		"��7ö�� ��Ŭ����f���� �� ü���� ����� ��e��� ü�¡�f�� �����",
		"��e��� ü�¡�f�� ���� �� ��e��� ü�·���f���ٵ� ���� ���ظ� ������ݴϴ�.",
		"ȭ���� �߻��� �� �� ���� �Ҹ��Ͽ� ���� ����� ������ �����,",
		"�Ÿ� ��� ���ظ� �߰��� �ָ� �ݵ�� ġ��Ÿ�� �߻��մϴ�."
		})

@Tips(tip = {
        "Ȱ�� �����ϰ� �� ����� �ϴ� ���Ÿ� ���� �� ��Ŀ�Դϴ�. 20�ʸ��� ȹ���ϴ�",
        "��� ü���� ���� �� ����� Ȱ�� ���߸� ��󿡰� �޴� ���ط��� 1ĭ��",
        "���� �ʰ� �Ǵ� ��, ���ָ� �����ϴ� ���� ȿ��, 100% ũ��Ƽ����",
        "ȿ���� ���� �� �ִ� Ȱ������ ȭ���� 2���� �Ҹ����� �����ϼ���."
}, strong = {
        @Description(subject = "�������� ȭ�� �����", explain = {
                "�⺻���� ȭ���� ġ��Ÿ�� Ȯ��������,",
                "ġ��Ÿ�� ����Ǳ⿡ ���������� �������� �� �� �ֽ��ϴ�."
        }),
        @Description(subject = "���� ���� ������� ���� ���", explain = {
                "����Ŀ�� �ؽ� �� �� ���� ���� �Ǹ� ������",
                "��� ü�� �� ĭ�� ���ط� ������ų �� �ֽ��ϴ�."
        }),
        @Description(subject = "�̵� �ӵ��� ���� ���", explain = {
                "���� ������� �ɾ� ����� �̵� �ӵ��� ���纸����."
        })
}, weak = {
        @Description(subject = "�ٴ���Ʈ", explain = {
                "���� �溮�� �ִ� 1ĭ������ ���ط� �ٿ��ִ� ��������,",
                "�����ؼ� ������� ���ϴ� �ɷ¿��Դ� ��� ü���� �Ҹ���� ����",
                "���� �溮�� ���� ����� �츮�� ����ϴ�."
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
	    		} else getPlayer().sendMessage("��c[��4!��c] ��dü�¡�f�� ���ڶ��ϴ�.");
	    	} else getPlayer().sendMessage("��c[��4!��c] ��e��� ü�¡�f�� �ʹ� �����ϴ�.");
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
					    			getPlayer().sendMessage("��8[��7HIDDEN��8] ��f100m ���� ���� 2�������� �����̽��ϴ�.");
					    			getPlayer().sendMessage("��8[��7HIDDEN��8] ��5��ź�� �����f�� �޼��Ͽ����ϴ�.");
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
				    			getPlayer().sendMessage("��8[��7HIDDEN��8] ��f100m ���� ���� 2�������� �����̽��ϴ�.");
				    			getPlayer().sendMessage("��8[��7HIDDEN��8] ��5��ź�� �����f�� �޼��Ͽ����ϴ�.");
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