package RainStarAbility;

import java.util.HashSet;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.FireworkEffect.Type;
import org.bukkit.Note.Tone;
import org.bukkit.Material;
import org.bukkit.Note;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByBlockEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent.RegainReason;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import daybreak.abilitywar.AbilityWar;
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
import daybreak.abilitywar.config.ability.AbilitySettings.SettingObject;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.AbstractGame.Participant.ActionbarNotification.ActionbarChannel;
import daybreak.abilitywar.utils.annotations.Beta;
import daybreak.abilitywar.utils.base.Formatter;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.base.minecraft.FireworkUtil;
import daybreak.abilitywar.utils.base.random.Random;
import daybreak.abilitywar.utils.library.MaterialX;
import daybreak.abilitywar.utils.library.ParticleLib;
import daybreak.abilitywar.utils.library.SoundLib;
import daybreak.abilitywar.utils.library.item.ItemLib;

@Beta

@AbilityManifest(name = "플렉스", rank = Rank.S, species = Species.HUMAN, explain = {
		"§7웅크린 채 철괴 좌클릭 §8- §e자본주의§f: 가진 광물을 전부 소모하여",
		" 버프를 획득합니다.",
		"§8[§7HIDDEN§8] §e머니건§f: 금도 막 쏴버려!"})

@Tips(tip = {
        "인생은 원래 한 방입니다... 단 한 번의 기회를 놓치지 마세요.",
        "자신의 광물을 §e§lF§a§lL§e§lE§a§lX§f하고 돈의 맛을 보여주세요."
}, strong = {
        @Description(subject = "많은 광물 소유", explain = {
                "광물을 많이 소지하면 소지할수록 액티브 발동 시",
                "얻는 버프의 수치가 강력해집니다."
        }),
        @Description(subject = "어그로", explain = {
                "내 광물 보이지?",
                "광역 어그로를 끌어보세요!"
        })
}, weak = {
        @Description(subject = "많은 광물 소유", explain = {
                "반대로 말하면, 많은 광물을 다 잃어버릴 각오가 있어야 합니다..."
        }),
        @Description(subject = "뒤가 없음", explain = {
                "한 번 쓰면 광물을 다시 캐오기 전까진 아무 능력도",
                "존재하지 않습니다..."
        })
}, stats = @Stats(offense = Level.TWO, survival = Level.TWO, crowdControl = Level.ZERO, mobility = Level.ZERO, utility = Level.ZERO), difficulty = Difficulty.VERY_EASY)

public class Flex extends AbilityBase implements ActiveHandler {
	
	public Flex(Participant participant) {
		super(participant);
	}
	
	private int Iron = 0;
	private int Gold = 0;
	private int Diamond = 0;
	private int Emerald = 0;
	private Set<Item> items = new HashSet<>();
	private Set<Arrow> goldenarrow = new HashSet<>();
	private boolean onetime = true;
	long worldtime = 0;
	private Random random = new Random();
	private Player target;
	private final ActionbarChannel ac = newActionbarChannel();
	
	private static final Color[] colors = {
			Color.SILVER, Color.YELLOW, Color.AQUA, Color.LIME
	};
	
	private static final Color[] colors2 = {
			Color.YELLOW, Color.LIME, Color.GREEN, Color.WHITE
	};
	
	private static final Type[] types = {
			Type.BALL_LARGE, Type.STAR
	};
	
	private static final Type[] types2 = {
			Type.CREEPER, Type.BURST
	};
	
	private final Cooldown cool = new Cooldown(CooldownConfig.getValue());
	
	public static final SettingObject<Integer> CooldownConfig = abilitySettings.new SettingObject<Integer>(Flex.class,
			"Cooldown", 30, "# 쿨타임") {
		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}

		@Override
		public String toString() {
			return Formatter.formatCooldown(getValue());
		}
	};	
	
	public static final SettingObject<Integer> DurationConfig = abilitySettings.new SettingObject<Integer>(Flex.class,
			"Duration", 10, "# 지속 시간") {
		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}
	};
	
	private void updateTime(World world) {
		world.setTime(13000);
	}
	
	private final ItemStack[] istacks = {
            MaterialX.GOLD_INGOT.createItem(),
            MaterialX.EMERALD.createItem(),
            MaterialX.DIAMOND.createItem(),
            MaterialX.IRON_INGOT.createItem()
    };
	
	
	@Override
	public boolean ActiveSkill(Material material, ClickType clicktype) {
		 if (material.equals(Material.IRON_INGOT) && clicktype.equals(ClickType.LEFT_CLICK) && !duration.isDuration() && !cool.isCooldown() && getPlayer().isSneaking()) {
			Iron = ItemLib.removeItem(getPlayer().getInventory(), Material.IRON_INGOT);
			Gold = ItemLib.removeItem(getPlayer().getInventory(), Material.GOLD_INGOT);
			Diamond = ItemLib.removeItem(getPlayer().getInventory(), Material.DIAMOND);
			Emerald = ItemLib.removeItem(getPlayer().getInventory(), Material.EMERALD);
			duration.start();
			return true;
		 }
		 return false;
	}
	
	@SubscribeEvent
	public void onEntityPickupItem(EntityPickupItemEvent e) {
		if (items.contains(e.getItem())) {
			e.setCancelled(true);
		}
	}
	
	PotionEffect glowing = new PotionEffect(PotionEffectType.GLOWING, 100, 0, true, false);
	
	private final Duration duration = new Duration((DurationConfig.getValue() * 20), cool) {
		
		@Override
		protected void onDurationStart() {
			if (Emerald != 0) {
				duration.setCount(duration.getCount() + (Emerald * 10));
			}
    		updateTime(getPlayer().getWorld());
    		FireworkUtil.spawnRandomFirework(getPlayer().getLocation(), colors, colors, types, 1);
			new BukkitRunnable() {
				@Override
				public void run() {
		    		FireworkUtil.spawnRandomFirework(getPlayer().getLocation(), colors, colors, types, 1);
				}	
			}.runTaskLater(AbilityWar.getPlugin(), 10L);
			new BukkitRunnable() {
				@Override
				public void run() {
		    		FireworkUtil.spawnRandomFirework(getPlayer().getLocation(), colors, colors, types, 1);
				}	
			}.runTaskLater(AbilityWar.getPlugin(), 20L);
			new BukkitRunnable() {
				@Override
				public void run() {
		    		FireworkUtil.spawnRandomFirework(getPlayer().getLocation(), colors, colors, types, 1);
				}	
			}.runTaskLater(AbilityWar.getPlugin(), 30L);
			new BukkitRunnable() {
				@Override
				public void run() {
		    		FireworkUtil.spawnRandomFirework(getPlayer().getLocation(), colors, colors, types, 1);
				}	
			}.runTaskLater(AbilityWar.getPlugin(), 40L);
    		ac.update("§7" + (0.05 * Iron) + " §f|§e " + (0.03 * Gold) + " §f|§b " + (0.005 * Diamond) + " §f|§a " + (0.5 * Emerald));
			new AbilityTimer(100) {
				 	@Override
	                protected void run(int count) {
	                	SoundLib.ENTITY_ITEM_PICKUP.playSound(getPlayer().getLocation(), 0.5f, 1.3f);
	                    final Item item = getPlayer().getWorld().dropItem(getPlayer().getEyeLocation(), random.pick(istacks));
	                    items.add(item);
	                    new AbilityTimer(12) {
	                        @Override
	                        protected void run(int count) {
	                            if (item.isOnGround()) {
	                                stop(true);
	                            }
	                        }
	                        @Override
	                        protected void onEnd() {
	                            item.remove();
	                        }
	                        @Override
	                        protected void onSilentEnd() {
	                            item.remove();
	                        }
	                    }.setPeriod(TimeUnit.TICKS, 5).start();
	                    item.setPickupDelay(Integer.MAX_VALUE);
	                    item.setVelocity(new Vector(((random.nextDouble() * 2) - 1) / 2, .7, ((random.nextDouble() * 2) - 1) / 2));
	                }
			 }.setPeriod(TimeUnit.TICKS, 1).start();
		}
		
		@Override
		protected void onDurationProcess(int count) {
			final double maxHP = getPlayer().getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue();
			if (getPlayer().getHealth() < maxHP) {
				final EntityRegainHealthEvent event = new EntityRegainHealthEvent(getPlayer(), (0.005 * Diamond), RegainReason.CUSTOM);
				Bukkit.getPluginManager().callEvent(event);
				if (!event.isCancelled()) {
					getPlayer().setHealth(Math.min(getPlayer().getHealth() + (0.005 * Diamond), maxHP));
				}

			}
		}
		
		@Override
		protected void onDurationEnd() {
			onDurationSilentEnd();
		}
		
		@Override
		protected void onDurationSilentEnd() {
			ac.update(null);
		}
		
	}.setPeriod(TimeUnit.TICKS, 1);
	
	@SubscribeEvent
	public void onEntityDamage(EntityDamageEvent e) {
		if (e.getEntity().equals(getPlayer()) && duration.isRunning()) {
			e.setDamage(e.getDamage() - (Iron * 0.05));
		}
	}
	
	@SubscribeEvent
	public void onEntityDamageByBlock(EntityDamageByBlockEvent e) {
		onEntityDamage(e);
	}
	
	@SubscribeEvent
	public void onEntityDamageByEntity(EntityDamageByEntityEvent e) {
		if (e.getDamager().equals(getPlayer()) && duration.isRunning()) {
			e.setDamage(e.getDamage() + (Gold * 0.03));
		}
		onEntityDamage(e);
	}
	
	@SubscribeEvent
	public void onEntityShootBow(EntityShootBowEvent e) {
		if (e.getEntity().equals(getPlayer())) {
			Material off = getPlayer().getInventory().getItemInOffHand().getType();
			if (off.equals(Material.GOLD_INGOT)) {
				ItemLib.removeItem(getPlayer().getInventory(), Material.GOLD_INGOT, 1);	
				ItemLib.addItem(getPlayer().getInventory(), Material.ARROW, 1);
			}
		}
	}
	
    @SubscribeEvent
	public void onProjectileLaunch(ProjectileLaunchEvent e) {
    	if (e.getEntity().getShooter().equals(getPlayer()) && e.getEntity() instanceof Arrow) {
    		Material off = getPlayer().getInventory().getItemInOffHand().getType();
			if (off.equals(Material.GOLD_INGOT)) {
				if (onetime) {
	    			getPlayer().sendMessage("§8[§7HIDDEN§8] §e머니건§f을 달성하였습니다.");
	    			SoundLib.UI_TOAST_CHALLENGE_COMPLETE.playSound(getPlayer());
					onetime = false;
				}
				Arrow arrow = (Arrow) e.getEntity();
	    		goldenarrow.add(arrow);
	    		SoundLib.ENTITY_FIREWORK_ROCKET_LAUNCH.playSound(arrow.getLocation());
				arrow.setGlowing(true);
	    		
	    		new AbilityTimer(8) {
	    			
	    			@Override
	                protected void run(int count) {
	    				switch(count) {
	    				case 7: 				
	    					SoundLib.CHIME.playInstrument(arrow.getLocation(), Note.natural(0, Tone.E));
	    					break;
	    				case 6: 				
	    					SoundLib.CHIME.playInstrument(arrow.getLocation(), Note.natural(1, Tone.A));
	    					break;
	    				case 5: 				
	    					SoundLib.CHIME.playInstrument(arrow.getLocation(), Note.natural(1, Tone.B));
	    					break;
	    				case 4: 				
	    					SoundLib.CHIME.playInstrument(arrow.getLocation(), Note.sharp(1, Tone.C));
	    					break;
	    				case 3: 				
	    					SoundLib.CHIME.playInstrument(arrow.getLocation(), Note.natural(1, Tone.B));
	    					break;
	    				case 2: 				
	    					SoundLib.CHIME.playInstrument(arrow.getLocation(), Note.sharp(2, Tone.F));
	    					break;
	    				case 1: 				
	    					SoundLib.CHIME.playInstrument(arrow.getLocation(), Note.sharp(1, Tone.C));
	    					SoundLib.CHIME.playInstrument(arrow.getLocation(), Note.natural(1, Tone.E));
	    					break;
	    				}
	    				if (goldenarrow.contains(arrow)) {
		    				ParticleLib.TOTEM.spawnParticle(arrow.getLocation(), 0, 0, 0, 5, 0.5);	
	    				}
	    			}
	    			
	    		}.setPeriod(TimeUnit.TICKS, 4).start();
			}
    	}
    }
    
    @SubscribeEvent
    public void onProjectileHit(ProjectileHitEvent e) {
    	if (goldenarrow.contains(e.getEntity()) && !e.getHitEntity().equals(getPlayer()) && e.getHitEntity() != null) {
    		FireworkUtil.spawnRandomFirework(e.getHitEntity().getLocation(), colors2, colors2, types2, 1);
    		target = (Player) e.getHitEntity();
    		ItemLib.addItem(target.getInventory(), Material.GOLD_INGOT, 1);
    		target.addPotionEffect(glowing);
    		e.getEntity().setGlowing(false);
    		goldenarrow.remove(e.getEntity());
    	}
    }
  
}