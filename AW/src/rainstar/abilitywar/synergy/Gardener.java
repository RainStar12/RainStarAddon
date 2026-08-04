package rainstar.abilitywar.synergy;

import static rainstar.abilitywar.system.config.RainStarAbilitySetting.rsSynergySettings;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent.RegainReason;
import org.bukkit.event.player.PlayerVelocityEvent;
import org.bukkit.potion.PotionEffectType;

import daybreak.abilitywar.AbilityWar;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.ability.decorator.ActiveHandler;
import daybreak.abilitywar.config.ability.AbilitySettings.SettingObject;
import daybreak.abilitywar.game.AbstractGame.Effect;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.AbstractGame.Participant.ActionbarNotification.ActionbarChannel;
import daybreak.abilitywar.game.list.mix.synergy.Synergy;
import daybreak.abilitywar.game.manager.effect.registry.EffectType;
import daybreak.abilitywar.game.module.DeathManager;
import daybreak.abilitywar.game.team.interfaces.Teamable;
import daybreak.abilitywar.utils.base.Formatter;
import daybreak.abilitywar.utils.base.color.RGB;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.base.math.LocationUtil;
import daybreak.abilitywar.utils.base.math.geometry.Circle;
import daybreak.abilitywar.utils.base.minecraft.block.Blocks;
import daybreak.abilitywar.utils.base.minecraft.block.IBlockSnapshot;
import daybreak.abilitywar.utils.base.minecraft.nms.IHologram;
import daybreak.abilitywar.utils.base.minecraft.nms.NMS;
import daybreak.abilitywar.utils.base.random.Random;
import daybreak.abilitywar.utils.library.BlockX;
import daybreak.abilitywar.utils.library.MaterialX;
import daybreak.abilitywar.utils.library.ParticleLib;
import daybreak.abilitywar.utils.library.PotionEffects;
import daybreak.abilitywar.utils.library.SoundLib;
import daybreak.google.common.base.Predicate;
import daybreak.google.common.collect.ImmutableSet;
import kotlin.ranges.RangesKt;
import rainstar.abilitywar.effect.Charm;
import rainstar.abilitywar.effect.Dream;
import rainstar.abilitywar.effect.Mute;
import rainstar.abilitywar.effect.Paralysis;
import rainstar.abilitywar.effect.Poison;

@AbilityManifest(name = "정원사", rank = Rank.S, species = Species.HUMAN, explain = {
		"무작위 §d꽃§f의 효과를 $[FLOWER_RANGE_HOLDING]칸 내 대상에게 효과를 부여합니다.",
		"§a긍정 효과§f라면 §a아군§f에게, §c부정 효과§f라면 §c적§f에게 적용됩니다.",
        "§7철괴 좌클릭§f으로 제자리에 그 §d꽃§f을 심어 $[FLOWER_EFFECT_DURATION]초간 해당 위치에",
        "§d꽃§f의 효과를 $[FLOWER_RANGE_PLANTED]칸에 남기고, 자신은 새 무작위 §d꽃§f을 듭니다. $[COOLDOWN]",
		"§4양귀비 §c§n중독§c, §n마비§7 | §e민들레 §a회복속도, 회복력 증가§7 | §5파꽃 §c받는 피해량 $[ALLIUM_INCREASE]% 증가", 
		"§b난초 §a신속 $[ORCHID_SPEED], 저지 불가§7 | §f선애기별꽃 §a저항 $[AZUREBLUET_RESISTANCE], 상태이상 무시§7 | §d라일락 §c§n몽환§c, §n침묵", 
		"§6튤립 §a공격력 $[TULIP_INCREASE]% 증가§7 | §c장미 §a피해량 $[ROSE_REFLECT]% 반사§7 | §a모란 §c§n강력한 유혹",
		})
public class Gardener extends Synergy  implements ActiveHandler {
	
	public Gardener(Participant participant) {
		super(participant);
	}
	
	public static final SettingObject<Double> FLOWER_EFFECT_DURATION =
			rsSynergySettings.new SettingObject<Double>(Gardener.class, "flower-effect-duration", 12.0,
	        "# 효과 지속시간", "# 단위: 초") {
		
	    @Override
	    public boolean condition(Double value) {
	        return value >= 0;
	    }
	    
	};
	
	public static final SettingObject<Double> FLOWER_RANGE_HOLDING = 
			rsSynergySettings.new SettingObject<Double>(Gardener.class, "flower-range-holding", 6.0,
	        "# 꽃(들고 있을 때) 영향 범위", "# 단위: 칸") {
		
	    @Override
	    public boolean condition(Double value) {
	        return value >= 0;
	    }
	    
	};
	
	public static final SettingObject<Double> FLOWER_RANGE_PLANTED = 
			rsSynergySettings.new SettingObject<Double>(Gardener.class, "flower-range", 9.0,
	        "# 꽃(내려놓았을 때) 영향 범위", "# 단위: 칸") {
		
	    @Override
	    public boolean condition(Double value) {
	        return value >= 0;
	    }
	    
	};
	
	public static final SettingObject<Integer> ALLIUM_INCREASE = 
			rsSynergySettings.new SettingObject<Integer>(Gardener.class, "allium-increase", 30,
	        "# 파꽃 받는 피해량 증가량", "# 단위: %") {
		
	    @Override
	    public boolean condition(Integer value) {
	        return value >= 0;
	    }
	    
	};
	
	public static final SettingObject<Integer> ORCHID_SPEED = 
			rsSynergySettings.new SettingObject<Integer>(Gardener.class, "orchid-speed", 2,
		            "# 난초 신속 효과 계수", "# 주의! 0부터 시작합니다.", "# 0일 때 포션 효과 계수는 1레벨,", "# 1일 때 포션 효과 계수는 2레벨입니다.") {
		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}
		
		@Override
		public String toString() {
			return "" + (1 + getValue());
		}
		
	};
	
	public static final SettingObject<Integer> AZUREBLUET_RESISTANCE = 
			rsSynergySettings.new SettingObject<Integer>(Gardener.class, "azurebluet-resistance", 1,
		            "# 선애기별꽃 저항 효과 계수", "# 주의! 0부터 시작합니다.", "# 0일 때 포션 효과 계수는 1레벨,", "# 1일 때 포션 효과 계수는 2레벨입니다.") {
		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}
		
		@Override
		public String toString() {
			return "" + (1 + getValue());
		}
		
	};
	
	public static final SettingObject<Integer> TULIP_INCREASE = 
			rsSynergySettings.new SettingObject<Integer>(Gardener.class, "tulip-increase", 30,
	        "# 튤립 공격력 증가량", "# 단위: %") {
		
	    @Override
	    public boolean condition(Integer value) {
	        return value >= 0;
	    }
	    
	};
	
	public static final SettingObject<Integer> ROSE_REFLECT = 
			rsSynergySettings.new SettingObject<Integer>(Gardener.class, "rose-reflect", 100,
	        "# 반사 피해량", "# 단위: %") {
		
	    @Override
	    public boolean condition(Integer value) {
	        return value >= 0;
	    }
	    
	};
	
	public static final SettingObject<Integer> COOLDOWN = 
			rsSynergySettings.new SettingObject<Integer>(Gardener.class, "cooldown", 45,
			"# 꽃 설치 쿨타임") {

		@Override
		public boolean condition(Integer value) {
			return value >= 1;
		}

		@Override
		public String toString() {
			return Formatter.formatCooldown(getValue());
		}
	};
	

	private final int flowerDuration = (int) (FLOWER_EFFECT_DURATION.getValue() * 20);
	private final double rangeHolding = FLOWER_RANGE_HOLDING.getValue(), rangePlanted = FLOWER_RANGE_PLANTED.getValue();
	private final double alliumIncrease = 1 + (ALLIUM_INCREASE.getValue() * 0.01);
	private final int orchidSpeed = ORCHID_SPEED.getValue();
	private final int azurebluetResistance = AZUREBLUET_RESISTANCE.getValue();
	private final double tulipIncrease = 1 + (TULIP_INCREASE.getValue() * 0.01);
	private final double roseReflect = (ROSE_REFLECT.getValue() * 0.01);
	
	private ActionbarChannel ac = newActionbarChannel();
	private List<Location> flowerlocations = new ArrayList<>();
	private Random random = new Random();
	private Flower nowFlower = random.pick(Flower.values());
	private Plant nowPlant = null;
	private Cooldown cooldown = new Cooldown(COOLDOWN.getValue());
	
    enum Flower {  	
    	POPPY("§4양귀비", false, MaterialX.POPPY, RGB.of(174, 1, 1)),
    	DANDELION("§e민들레", true, MaterialX.DANDELION, RGB.of(254, 254, 38)),
    	ALLIUM("§5파꽃", false, MaterialX.ALLIUM, RGB.of(235, 97, 254)),
    	ORCHID("§b난초", true, MaterialX.BLUE_ORCHID, RGB.of(29, 189, 250)),
    	AZURE_BLUET("§f선애기별꽃", true, MaterialX.AZURE_BLUET, RGB.of(254, 254, 225)),
    	LILAC("§d라일락", false, MaterialX.LILAC, RGB.of(254, 208, 250)),
    	TULIP("§6튤립", true, MaterialX.ORANGE_TULIP, RGB.of(254, 157, 91)),
    	ROSE("§c장미", true, MaterialX.ROSE_BUSH, RGB.of(254, 13, 19)),
    	PEONY("§a모란", false, MaterialX.PEONY, RGB.of(254, 159, 207));
    	
    	private final String flowerName;
    	private final boolean positive;
    	private final MaterialX flower;
    	private final RGB color;
    	
    	Flower(String flowerName, boolean positive, MaterialX flower, RGB color) {
    		this.flowerName = flowerName;
    		this.positive = positive;
    		this.flower = flower;
    		this.color = color;
    	}
    	
		public static Flower getRandomFlower() {
			final Random random = new Random();
			return random.pick(values());
		}
		
		public String getName() {
			return name();
		}
		
		public String getFlowerName() {
			return flowerName;
		}
		
		public boolean isPositive() {
			return positive;
		}
		
		public MaterialX getFlower() {
			return flower;
		}
		
		public RGB getColor() {
			return color;
		}
		
    }
    
    @Override
    public void onUpdate(Update update) {
    	if (update == Update.RESTRICTION_CLEAR) {
    		passive.start();
    		if (nowPlant == null) {
        		nowPlant = new Plant(nowFlower, getPlayer().getLocation(), flowerDuration, true);
        		nowPlant.start();
    		}
    	}
    }
    
	private AbilityTimer passive = new AbilityTimer() {
		
		@Override
		public void run(int count) {
			ac.update(nowFlower.getFlowerName());
		}
		
	}.setPeriod(TimeUnit.TICKS, 1).register();
	
	public boolean ActiveSkill(Material material, ClickType clicktype) {
		if (material.equals(Material.IRON_INGOT) && clicktype.equals(ClickType.LEFT_CLICK)) {
			if (nowFlower != null && !cooldown.isCooldown()) {
				nowPlant.stop(false);
				new Plant(nowFlower, getPlayer().getLocation(), flowerDuration, false).start();
				nowFlower = random.pick(Flower.class);
				nowPlant = new Plant(nowFlower, getPlayer().getLocation(), flowerDuration, true);
        		nowPlant.start();
				return cooldown.start();
			}
		}
		return false;
	}
	
	private class Plant extends AbilityTimer implements Listener {
		
		private final Flower flower;
		private Location location = getPlayer().getLocation();
		private IHologram hologram = null;
		private final DecimalFormat df = new DecimalFormat("0.0");
		private Block block;
		private IBlockSnapshot snapshot;
		private double range;
		private Circle circle;
		private boolean holding;
		private final Predicate<Entity> predicate;		
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
		
		private Plant(Flower flower, Location location, int flowerduration, boolean holding) {
			super(TaskType.REVERSE, flowerduration);
			setPeriod(TimeUnit.TICKS, 1);
			Bukkit.getPluginManager().registerEvents(this, AbilityWar.getPlugin());
			this.flower = flower;
			this.holding = holding;
			this.range = holding ? rangeHolding : rangePlanted;
			this.circle = Circle.of(range, (int) (range * 12));
			if (!holding) {
				this.location = location;
				this.hologram = NMS.newHologram(location.getWorld(), location.getX(), location.getY(), location.getZ(), "§a개화까지§7: §2" + df.format(getCount() / 20.0) + "§f초");
				hologram.display(getPlayer());
				this.block = location.clone().add(0, 1, 0).getBlock().getRelative(BlockFace.DOWN);
				location = block.getLocation().clone().add(0.5, 0, 0.5);
				flowerlocations.add(location);
				snapshot = Blocks.createSnapshot(block);
				BlockX.setType(block, flower.getFlower());					
				hologram.teleport(location.clone().add(0, 1.2, 0));
				SoundLib.ENTITY_PLAYER_LEVELUP.playSound(location, 1, 2);
			} else setCount(flowerduration / 2);
			this.predicate = new Predicate<Entity>() {
				@Override
				public boolean test(Entity entity) {
					if (flower.isPositive()) {
						if (entity.equals(getPlayer())) return true;
						if (entity instanceof Player) {
							if (!getGame().isParticipating(entity.getUniqueId())
									|| (getGame() instanceof DeathManager.Handler &&
											((DeathManager.Handler) getGame()).getDeathManager().isExcluded(entity.getUniqueId()))) {
								return false;
							}
							if (getGame() instanceof Teamable) {
								final Teamable teamGame = (Teamable) getGame();
								final Participant entityParticipant = teamGame.getParticipant(
										entity.getUniqueId()), participant = getParticipant();
								return teamGame.hasTeam(entityParticipant) && teamGame.hasTeam(participant)
										&& (teamGame.getTeam(entityParticipant).equals(teamGame.getTeam(participant)));
							}
							Player player = (Player) entity;
							if (player.getGameMode().equals(GameMode.SPECTATOR)) {
								return false;
							}
						}
						return false;	
					} else {
						if (entity.equals(getPlayer())) return false;
						if (entity instanceof Player) {
							if (!getGame().isParticipating(entity.getUniqueId())
									|| (getGame() instanceof DeathManager.Handler &&
											((DeathManager.Handler) getGame()).getDeathManager().isExcluded(entity.getUniqueId()))) {
								return false;
							}
							if (getGame() instanceof Teamable) {
								final Teamable teamGame = (Teamable) getGame();
								final Participant entityParticipant = teamGame.getParticipant(
										entity.getUniqueId()), participant = getParticipant();
								return !teamGame.hasTeam(entityParticipant) || !teamGame.hasTeam(participant)
										|| (!teamGame.getTeam(entityParticipant).equals(teamGame.getTeam(participant)));
							}
							Player player = (Player) entity;
							if (player.getGameMode().equals(GameMode.SPECTATOR)) {
								return false;
							}
						}
						return true;
					}
				}

				@Override
				public boolean apply(@Nullable Entity arg0) {
					return false;
				}
			};
		}
		
		@Override
		protected void run(int count) {
			if (!holding) hologram.setText("§2남은 수명§7: §c" + df.format(count / 20.0) + "§f초");	
			else {
				location = getPlayer().getLocation();
				if (count <= (flowerDuration / 2)) this.setCount((flowerDuration / 2) + 10);
			}
			
			if (count % 2 == 0) {
				for (Location loc : circle.toLocations(location).floor(location.getY())) {
					ParticleLib.REDSTONE.spawnParticle(loc, flower.getColor());
				}
			}
			
			if (flower.getName().equals("POPPY")) {
				for (Player player : LocationUtil.getEntitiesInCircle(Player.class, location, range, predicate)) {
					Poison.apply(getGame().getParticipant(player), TimeUnit.TICKS, count);
					Paralysis.apply(getGame().getParticipant(player), TimeUnit.TICKS, count);
				}
			}
			
			if (flower.getName().equals("DANDELION")) {
				for (Player player : LocationUtil.getEntitiesInCircle(Player.class, location, range, predicate)) {
					if (!player.isDead()) {
						final double maxHealth = player.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue();
						if (player.getHealth() < maxHealth) {
							final EntityRegainHealthEvent event = new EntityRegainHealthEvent(player, .025, RegainReason.CUSTOM);
							Bukkit.getPluginManager().callEvent(event);
							if (!event.isCancelled()) {
								player.setHealth(RangesKt.coerceIn(player.getHealth() + event.getAmount(), 0, maxHealth));
							}
						}
					}
				}
			}
			
			if (flower.getName().equals("ORCHID")) {
				for (Player player : LocationUtil.getEntitiesInCircle(Player.class, location, range, predicate)) {
					PotionEffects.SPEED.addPotionEffect(player, 3, orchidSpeed, true);
					if (player.hasPotionEffect(PotionEffectType.SLOW)) player.removePotionEffect(PotionEffectType.SLOW);
					if (player.hasPotionEffect(PotionEffectType.BLINDNESS)) player.removePotionEffect(PotionEffectType.BLINDNESS);
					if (player.hasPotionEffect(PotionEffectType.LEVITATION)) player.removePotionEffect(PotionEffectType.LEVITATION);
					getGame().getParticipant(player).removeEffects(effectpredicate);
				}
			}
			
			if (flower.getName().equals("AZURE_BLUET")) {
				for (Player player : LocationUtil.getEntitiesInCircle(Player.class, location, range, predicate)) {
					PotionEffects.DAMAGE_RESISTANCE.addPotionEffect(player, 3, azurebluetResistance, true);
					getGame().getParticipant(player).removeEffects();
				}
			}
			
			if (flower.getName().equals("LILAC")) {
				for (Player player : LocationUtil.getEntitiesInCircle(Player.class, location, range, predicate)) {
					Dream.apply(getGame().getParticipant(player), TimeUnit.TICKS, count);
					if (!getGame().getParticipant(player).hasEffect(Mute.registration)) Mute.apply(getGame().getParticipant(player), TimeUnit.TICKS, count);
				}
			}
			
			if (flower.getName().equals("PEONY")) {
				for (Player player : LocationUtil.getEntitiesInCircle(Player.class, location, range, predicate)) {
					if (!getGame().getParticipant(player).hasEffect(Charm.registration)) Charm.apply(getGame().getParticipant(player), TimeUnit.TICKS, count, getPlayer(), 50, 70);
				}
			}
		}
		
		@Override
		protected void onEnd() {
			onSilentEnd();
		}
		
		@Override
		protected void onSilentEnd() {
			if (!holding) {
				flowerlocations.remove(location);
				hologram.unregister();	
				if (snapshot != null) snapshot.apply();
			}
			HandlerList.unregisterAll(this);
		}
		
		@EventHandler
		public void onEntityDamage(EntityDamageEvent e) {
			if (flower.getName().equals("ALLIUM")) {
				if (LocationUtil.getEntitiesInCircle(Player.class, location, range, predicate).contains(e.getEntity())) {
					e.setDamage(e.getDamage() * alliumIncrease);
				}
			}
		}
		
		@EventHandler
		public void onEntityDamageByEntity(EntityDamageByEntityEvent e) {
	    	Player damager = null;
			if (e.getDamager() instanceof Projectile) {
				Projectile projectile = (Projectile) e.getDamager();
				if (projectile.getShooter() instanceof Player) damager = (Player) projectile.getShooter();
			} else if (e.getDamager() instanceof Player) damager = (Player) e.getDamager();
			
			if (flower.getName().equals("ALLIUM")) {
				onEntityDamage(e);
			}
			
			if (damager != null) {
				if (flower.getName().equals("TULIP")) {
					if (LocationUtil.getEntitiesInCircle(Player.class, location, range, predicate).contains(damager)) {
						e.setDamage(e.getDamage() * tulipIncrease);
					}
				}
				
				if (flower.getName().equals("ROSE")) {
					if (LocationUtil.getEntitiesInCircle(Player.class, location, range, predicate).contains(e.getEntity())) {
						damager.damage(e.getDamage() * roseReflect, e.getEntity());
					}
				}	
			}
		}
		
		@EventHandler
		private void onVelocity(PlayerVelocityEvent e) {
			if (flower.getName().equals("AZURE_BLUET") && LocationUtil.getEntitiesInCircle(Player.class, location, range, predicate).contains(e.getPlayer())) e.setCancelled(true);
		}
		
		@EventHandler
		private void onEntityRegainHealth(EntityRegainHealthEvent e) {
			if (flower.getName().equals("DANDELION") && LocationUtil.getEntitiesInCircle(Player.class, location, range, predicate).contains(e.getEntity())) e.setAmount(e.getAmount() * 1.5);
		}
		
		@EventHandler
		public void onBlockBreak(BlockBreakEvent e) {
			if (!holding && e.getBlock().equals(block)) e.setCancelled(true);
		}

		@EventHandler
		public void onExplode(BlockExplodeEvent e) {
			if (!holding) e.blockList().removeIf(blocks -> blocks.equals(block));
		}

		@EventHandler
		public void onExplode(EntityExplodeEvent e) {
			if (!holding) e.blockList().removeIf(blocks -> blocks.equals(block));
		}
		
	}
	
}
