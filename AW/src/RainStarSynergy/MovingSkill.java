package RainStarSynergy;

import java.util.HashSet;
import java.util.Set;

import javax.annotation.Nullable;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Damageable;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByBlockEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import RainStarEffect.Confusion;
import daybreak.abilitywar.AbilityWar;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.SubscribeEvent;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.game.GameManager;
import daybreak.abilitywar.game.AbstractGame.Effect;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.list.mix.synergy.Synergy;
import daybreak.abilitywar.game.manager.effect.Bleed;
import daybreak.abilitywar.game.manager.effect.Rooted;
import daybreak.abilitywar.game.manager.effect.Stun;
import daybreak.abilitywar.game.manager.effect.registry.EffectRegistry;
import daybreak.abilitywar.game.manager.effect.registry.EffectRegistry.EffectRegistration;
import daybreak.abilitywar.game.manager.effect.registry.EffectType;
import daybreak.abilitywar.game.module.DeathManager;
import daybreak.abilitywar.game.module.Wreck;
import daybreak.abilitywar.game.team.interfaces.Teamable;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.base.math.LocationUtil;
import daybreak.abilitywar.utils.base.math.VectorUtil;
import daybreak.abilitywar.utils.base.math.geometry.Line;
import daybreak.abilitywar.utils.base.minecraft.damage.Damages;
import daybreak.abilitywar.utils.base.minecraft.nms.NMS;
import daybreak.abilitywar.utils.base.minecraft.version.ServerVersion;
import daybreak.abilitywar.utils.library.MaterialX;
import daybreak.abilitywar.utils.library.ParticleLib;
import daybreak.abilitywar.utils.library.SoundLib;
import daybreak.google.common.base.Predicate;
import daybreak.google.common.collect.ImmutableSet;

@AbilityManifest(name = "이동기", rank = Rank.L, species = Species.OTHERS, explain = {
		"§7패시브 §8- §b스태미나§f: 스태미나를 소모해 다른 능력을 사용할 수 있습니다.",
		" 스태미나가 3 이상일 때 어떠한 이동에 관한 제약이라도 받지 않으며,",
		" 능력을 사용한 것이 아닌 이동에 있어 스태미나는 더 빨리 차오릅니다.",
		" 스태미나가 1 이하가 될 때 3초간 회복하지 못하며 1초 후 1.5초간 기절합니다.",
		"§7검 들고 F §8- §3대시§f: 바라보는 방향으로 매우 빠르게 대시합니다. §c소모 §7: §b2",
		" 대시로 지나간 위치엔 대시 잔상이 남아 닿은 개체에게 피해를 입히고, §e기절§f시킵니다.",
		" 대시 시작부터 1초간 무적 및 타게팅 불능 상태가 됩니다.",
		" 공격 후 대시를 시전할 경우 대상을 §6혼란§f 및 §c출혈§f시킵니다.",
		"§7허공에서 웅크리기 §8- §a이단 점프§f: 공중에서 한 번 더 점프합니다. §c소모 §7: §b1",
		" 착지 전까지 적에게 피해를 입힐 때마다 대상은 2초간 §a이동 불능§f이 됩니다.",
		"§7패시브 §8- §b앞지르기§f: 이동 관련 상태 이상을 보유한 적에게 피해를 입히면",
		" 1.2배로 입히고, 스태미나를 소량 회복합니다."
		})

@SuppressWarnings("deprecation")
public class MovingSkill extends Synergy {
	
	public MovingSkill(Participant participant) {
		super(participant);
	}
	
	private static final Set<Material> swords;
	private double stamina = 0;
	private BossBar bossBar = null;
	private boolean fallcancel = false;
	
	private Location startLocation;
	private Participant target;
	private static final Vector zerov = new Vector(0, 0, 0);
	private int timer = (int) Math.ceil(Wreck.isEnabled(GameManager.getGame()) ? Wreck.calculateDecreasedAmount(75) * 5 : 5);
	private PotionEffect normalspeed = new PotionEffect(PotionEffectType.SPEED, 100, 1, true, false);
	private PotionEffect invisible = new PotionEffect(PotionEffectType.INVISIBILITY, 3, 0, true, false);
	private ItemStack[] armors;
	
	static {
		if (MaterialX.NETHERITE_SWORD.isSupported()) {
			swords = ImmutableSet.of(MaterialX.WOODEN_SWORD.getMaterial(), Material.STONE_SWORD, Material.IRON_SWORD, MaterialX.GOLDEN_SWORD.getMaterial(), Material.DIAMOND_SWORD, MaterialX.NETHERITE_SWORD.getMaterial());
		} else {
			swords = ImmutableSet.of(MaterialX.WOODEN_SWORD.getMaterial(), Material.STONE_SWORD, Material.IRON_SWORD, MaterialX.GOLDEN_SWORD.getMaterial(), Material.DIAMOND_SWORD);
		}
	}
	
	protected void onUpdate(Update update) {
	    if (update == Update.RESTRICTION_CLEAR) {
	    	staminaupdater.start();
	    } 
	}
	
	private final Predicate<Effect> effectpredicate = new Predicate<Effect>() {
		@Override
		public boolean test(Effect effect) {
			final ImmutableSet<EffectType> effectType = effect.getRegistration().getEffectType();
			return effectType.contains(EffectType.MOVEMENT_RESTRICTION) || effectType.contains(EffectType.MOVEMENT_INTERRUPT);
		}

		@Override
		public boolean apply(@Nullable Effect arg0) {
			return false;
		}
	};
	
	private final Predicate<Entity> predicate = new Predicate<Entity>() {
		@Override
		public boolean test(Entity entity) {
			if (entity.equals(getPlayer())) return false;
			if (entity instanceof Player) {
				if (!getGame().isParticipating(entity.getUniqueId())
						|| (getGame() instanceof DeathManager.Handler && ((DeathManager.Handler) getGame()).getDeathManager().isExcluded(entity.getUniqueId()))
						|| !getGame().getParticipant(entity.getUniqueId()).attributes().TARGETABLE.getValue()) {
					return false;
				}
				if (getGame() instanceof Teamable) {
					final Teamable teamGame = (Teamable) getGame();
					final Participant entityParticipant = teamGame.getParticipant(entity.getUniqueId()), participant = getParticipant();
					return !teamGame.hasTeam(entityParticipant) || !teamGame.hasTeam(participant) || (!teamGame.getTeam(entityParticipant).equals(teamGame.getTeam(participant)));
				}
			}
			return true;
		}

		@Override
		public boolean apply(@Nullable Entity arg0) {
			return false;
		}
	};
	
	private final AbilityTimer attacked = new AbilityTimer(3) {
		
		@Override
		public void run(int count) {
		}
		
	}.setPeriod(TimeUnit.TICKS, 1).register();
	
	private final AbilityTimer jumptimer = new AbilityTimer() {
		
	}.setPeriod(TimeUnit.TICKS, 1).register();
	
	private final AbilityTimer staminaupdater = new AbilityTimer() {
		
    	@Override
    	public void onStart() {
    		bossBar = Bukkit.createBossBar("스태미나", BarColor.BLUE, BarStyle.SEGMENTED_10);
    		bossBar.setProgress(stamina * 0.1);
    		bossBar.addPlayer(getPlayer());
    		if (ServerVersion.getVersion() >= 10) bossBar.setVisible(true);
    	}
    	
    	@Override
		public void run(int count) {
    		if (stamina >= 3) {
    			if (getPlayer().hasPotionEffect(PotionEffectType.SLOW)) getPlayer().removePotionEffect(PotionEffectType.SLOW);
    			if (getPlayer().hasPotionEffect(PotionEffectType.BLINDNESS)) getPlayer().removePotionEffect(PotionEffectType.BLINDNESS);
    			if (getPlayer().hasPotionEffect(PotionEffectType.LEVITATION)) getPlayer().removePotionEffect(PotionEffectType.LEVITATION);
    			getParticipant().removeEffects(effectpredicate);
    		}
    		if (timer == 0) {
    			staminaGain(0.5);
    			bossBar.setProgress(stamina * 0.1);
    		} else {
    			staminaGain((double) 1 / (timer * 20));
    			bossBar.setProgress(stamina * 0.1);	
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
	
	private final AbilityTimer stunmaker = new AbilityTimer(20) {
		
		@Override
		public void onEnd() {
			onSilentEnd();
		}

		@Override
		public void onSilentEnd() {
			Stun.apply(getParticipant(), TimeUnit.TICKS, 30);
		}
		
	}.setPeriod(TimeUnit.TICKS, 1).register();
	
	private final AbilityTimer staminaOut = new AbilityTimer(60) {
		
	}.setPeriod(TimeUnit.TICKS, 1).register();
	
	public void staminaUse(double value) {
		if (stamina - value < 1) {
			stunmaker.start();
			staminaOut.start();
		}
		stamina = Math.max(0, stamina - value);
	}
	
	public void staminaGain(double value) {
		if (!staminaOut.isRunning()) {
			stamina = Math.min(10, stamina + value);
		}
	}
	
	private final AbilityTimer dashinv = new AbilityTimer(20) {
		
		@Override
		public void onStart() {
			getParticipant().attributes().TARGETABLE.setValue(false);
		}
		
	   	@Override
	   	public void onEnd() {
	   		onSilentEnd();
	   	}
	    	
	   	@Override
	    public void onSilentEnd() {
			getParticipant().attributes().TARGETABLE.setValue(true);
	   	}
		
	}.setPeriod(TimeUnit.TICKS, 1).register();
	
	private final AbilityTimer dashing = new AbilityTimer(1) {
		
		@Override
		public void onStart() {
	    	getPlayer().addPotionEffect(invisible);
	    	getPlayer().setVelocity(VectorUtil.validateVector(getPlayer().getLocation().getDirection().normalize().multiply(10).setY(0)));
	    	getPlayer().getInventory().setArmorContents(null);
			if (target != null) {
		    	if (attacked.isRunning()) {
					Bleed.apply(getGame(), target.getPlayer(), TimeUnit.TICKS, 70);
		    		Confusion.apply(target, TimeUnit.SECONDS, 3, 20);
		   		}	
			}
	   	}
	 
	   	@Override
		public void run(int count) {
			SoundLib.ENTITY_PLAYER_ATTACK_SWEEP.playSound(getPlayer().getLocation(), 1, 1.5f);
	   	}
	   	
	   	@Override
	   	public void onEnd() {
	   		onSilentEnd();
	   	}
	    	
	   	@Override
	    public void onSilentEnd() {
			getPlayer().setVelocity(zerov);
			getPlayer().getInventory().setArmorContents(armors);
	   		SoundLib.ENTITY_FIREWORK_ROCKET_BLAST.playSound(getPlayer().getLocation());
			new BukkitRunnable() {
				
				@Override
				public void run() {
					new AfterImage().start();
				}
				
			}.runTaskLater(AbilityWar.getPlugin(), 1L);
	   	}
	    	
	}.setPeriod(TimeUnit.TICKS, 1).register();
	
    @SubscribeEvent(onlyRelevant = true)
    public void onPlayerSwapHandItems(PlayerSwapHandItemsEvent e) {
    	if (swords.contains(e.getOffHandItem().getType()) && e.getPlayer().equals(getPlayer())) {
    		if (!dashing.isRunning()) {
    			if (stamina >= 2) {
        	    	startLocation = getPlayer().getLocation();
        	    	armors = getPlayer().getInventory().getArmorContents();
            		staminaUse(2);
                	dashing.start();
                	jumptimer.pause();
                	if (dashinv.isRunning()) {
                		dashinv.setCount(20);
                	} else {
                		dashinv.start();
                	}
        		} else {
        			getPlayer().sendMessage("§f[§c!§f] §c스태미나가 부족합니다.");
        		}	
    		}
    		e.setCancelled(true);
    	}
    }
    
	@SubscribeEvent(onlyRelevant = true)
    public void onPlayerMove(PlayerMoveEvent e) {
		if (e.getPlayer().equals(getPlayer())) {
			if (!e.getPlayer().isOnGround() && !jumptimer.isRunning()) {
				if (stamina >= 1) {
					if (e.getPlayer().isSneaking()) {
						staminaUse(1);
						e.getPlayer().setVelocity(VectorUtil.validateVector(new Vector(e.getPlayer().getVelocity().getX() * 1.25, 1, e.getPlayer().getVelocity().getZ() * 1.25)));
						ParticleLib.CLOUD.spawnParticle(getPlayer().getLocation(), 0.5, 0.1, 0.5, 100, 0);
						SoundLib.BLOCK_SNOW_FALL.playSound(getPlayer().getLocation(), 1, 0.7f);
						jumptimer.resume();
						jumptimer.start();
						fallcancel = true;
					}
				}
			}
			if (e.getPlayer().isOnGround() && jumptimer.isRunning()) {
				jumptimer.stop(false);
			}
		}
    	if (!dashing.isRunning() && !jumptimer.isRunning() && e.getPlayer().equals(getPlayer())) {
    		if (stamina < 10) {
    			final Location from = e.getFrom(), to = e.getTo();
    			if (to == null || (from.getX() == to.getX() && from.getY() == to.getY() && from.getZ() == to.getZ())) return;
    			staminaGain(0.02);
    		}
    	}
    }
    
    @SubscribeEvent(onlyRelevant = true)
    public void onEntityDamage(EntityDamageEvent e) {
    	if (dashinv.isRunning() && e.getEntity().equals(getPlayer())) {
    		e.setCancelled(true);
    	}
		if (e.getEntity().equals(getPlayer()) && e.getCause() == DamageCause.FALL && fallcancel) {
			fallcancel = false;
			e.setCancelled(true);
		}
    }
    
    @SubscribeEvent
    public void onEntityDamageByEntity(EntityDamageByEntityEvent e) {
    	onEntityDamage(e);
    	if (e.getDamager().equals(getPlayer()) && e.getEntity() instanceof Player) {
    		Player player = (Player) e.getEntity();
			target = getGame().getParticipant(player);
    		for (EffectRegistration<?> effect : EffectRegistry.values()) {
    			if (effect.getEffectType().contains(EffectType.MOVEMENT_INTERRUPT) || effect.getEffectType().contains(EffectType.MOVEMENT_RESTRICTION)) {
    				if (target.hasEffect(effect)) {
    					e.setDamage(e.getDamage() * 1.2);
    					staminaGain(0.5);
    				}
    			}
    		}
    		if (jumptimer.isRunning() && predicate.test(player)) {
    			Rooted.apply(getGame().getParticipant(player), TimeUnit.TICKS, 40);
    		}
    		if (attacked.isRunning()) attacked.setCount(3);
    		else attacked.start();
    	}
    	if (NMS.isArrow(e.getDamager()) && e.getEntity() instanceof Player) {
    		Arrow arrow = (Arrow) e.getDamager();
    		if (getPlayer().equals(arrow.getShooter())) {
        		target = getGame().getParticipant((Player) e.getEntity());
        		for (EffectRegistration<?> effect : EffectRegistry.values()) {
        			if (effect.getEffectType().contains(EffectType.MOVEMENT_INTERRUPT) || effect.getEffectType().contains(EffectType.MOVEMENT_RESTRICTION)) {
        				if (target.hasEffect(effect)) {
        					e.setDamage(e.getDamage() * 1.2);
        					staminaGain(0.5);
        				}
        			}
        		}	
    		}
    	}
    }
    
	@SubscribeEvent
	private void onPlayerJoin(final PlayerJoinEvent e) {
		if (getPlayer().getUniqueId().equals(e.getPlayer().getUniqueId()) && staminaupdater.isRunning()) {
			if (bossBar != null) bossBar.addPlayer(e.getPlayer());
		}
	}

	@SubscribeEvent
	private void onPlayerQuit(final PlayerQuitEvent e) {
		if (getPlayer().getUniqueId().equals(e.getPlayer().getUniqueId())) {
			if (bossBar != null) bossBar.removePlayer(e.getPlayer());
		}
	}
    
    @SubscribeEvent
    public void onEntityDamageByBlock(EntityDamageByBlockEvent e) {
    	onEntityDamage(e);
    }
    
    public class AfterImage extends AbilityTimer {
    	
    	private Set<Damageable> damagedcheck = new HashSet<>();
    	private Location saveloc1;
    	private Location saveloc2;
    	
    	private AfterImage() {
    		super(TaskType.REVERSE, 60);
    		setPeriod(TimeUnit.TICKS, 1);
    	}
    	
    	@Override
    	protected void onStart() {
	   		for (Location loc : Line.between(startLocation, getPlayer().getLocation(), (int) Math.min(300, (25 * Math.sqrt(startLocation.distance(getPlayer().getLocation()))))).toLocations(startLocation)) {
	   			ParticleLib.END_ROD.spawnParticle(loc.add(0, 1, 0), 0, 0, 0, 1, 0);
	   			saveloc1 = startLocation;
	   			saveloc2 = getPlayer().getLocation();
	   		}
    	}
    	
    	@Override
    	protected void run(int count) {
    		for (Damageable p : LocationUtil.rayTraceEntities(Damageable.class, saveloc1, saveloc2, 0.75, predicate)) {
    			if (!p.equals(getPlayer()) && !damagedcheck.contains(p)) {
    				if (p instanceof Player) {
    					Stun.apply(getGame().getParticipant((Player) p), TimeUnit.TICKS, 4);
            			if (getGame().getParticipant((Player) p).hasEffect(Confusion.registration)) {
            				if (count < 50) {
            					getPlayer().addPotionEffect(normalspeed);
                				Damages.damageMagic(p, getPlayer(), true, 2);
                				damagedcheck.add(p);
            				}
            			} else {
            				Damages.damageMagic(p, getPlayer(), true, 2);
            				damagedcheck.add(p);
            			}
    				} else {
        				Damages.damageMagic(p, getPlayer(), true, 2);
        				damagedcheck.add(p);
    				}
    			}
    		}
    	}
    	
    	@Override
    	protected void onEnd() {
    		onSilentEnd();
    	}
    	
    	@Override
    	protected void onSilentEnd() {
    		damagedcheck.clear();
    	}
    	
    }

}
