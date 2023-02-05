package rainstar.abilitywar.ability;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;

import javax.annotation.Nullable;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent.RegainReason;

import daybreak.abilitywar.ability.AbilityBase;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.ability.decorator.ActiveHandler;
import daybreak.abilitywar.config.ability.AbilitySettings.SettingObject;
import daybreak.abilitywar.game.GameManager;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.AbstractGame.Participant.ActionbarNotification.ActionbarChannel;
import daybreak.abilitywar.game.module.DeathManager;
import daybreak.abilitywar.game.module.Wreck;
import daybreak.abilitywar.game.team.interfaces.Teamable;
import daybreak.abilitywar.utils.base.color.RGB;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.base.math.LocationUtil;
import daybreak.abilitywar.utils.base.random.Random;
import daybreak.abilitywar.utils.library.ParticleLib;
import daybreak.abilitywar.utils.library.PotionEffects;
import daybreak.abilitywar.utils.base.concurrent.SimpleTimer.TaskType;
import daybreak.abilitywar.utils.base.minecraft.block.Blocks;
import daybreak.abilitywar.utils.base.minecraft.block.IBlockSnapshot;
import daybreak.abilitywar.utils.base.minecraft.nms.NMS;
import daybreak.google.common.base.Predicate;
import daybreak.google.common.collect.ImmutableMap;
import kotlin.ranges.RangesKt;
import rainstar.abilitywar.effect.Charm;
import rainstar.abilitywar.effect.Dream;
import rainstar.abilitywar.effect.Poison;

@AbilityManifest(name = "그린 가든", rank = Rank.S, species = Species.OTHERS, explain = {
		"형형색색의 §d꽃§f의 §6씨앗§f을 최대 $[MAX_SEED]개까지 소지합니다.", 
		"§6씨앗§f을 전부 사용하면 $[RECHARGE]초 후 보급됩니다.", 
		"§7철괴 우클릭§f으로 제자리에 §6씨앗§f을 심어 색에 맞는 §d꽃§f을 $[BLOOMING_WAIT]초 후 피워냅니다.", 
		"§d꽃§f의 종류에 따라 $[FLOWER_EFFECT_DURATION]초간 $[RANGE]칸 내 생명체에게 효과를 부여합니다.", 
		"§a긍정 효과§f라면 아군에게, §c부정 효과§f라면 적에게 적용됩니다.", 
		"§4양귀비 §c§n중독§7 | §a민들레 §a회복속도 증가§7 | §5파꽃 §c받는 피해량 증가", 
		"§b난초 §a신속 2§7 | §f선애기별꽃 §a저항 및 넉백 제거§7 | §d라일락 §c§n몽환", 
		"§6튤립 §a공격력 증가§7 | §c장미 §a피해량 반사§7 | §e해바라기 §c§n유혹"
		})
public class GreenGarden extends AbilityBase implements ActiveHandler {
	
	public GreenGarden(Participant participant) {
		super(participant);
	}
	
	public static final SettingObject<Integer> MAX_SEED = 
			abilitySettings.new SettingObject<Integer>(GreenGarden.class, "max-seed", 3,
            "# 소지 가능한 최대 씨앗 개수") {
		
        @Override
        public boolean condition(Integer value) {
            return value >= 0;
        }
        
    };
    
	public static final SettingObject<Integer> RECHARGE = 
			abilitySettings.new SettingObject<Integer>(GreenGarden.class, "recharge-time", 45,
            "# 씨앗 보급까지 걸리는 시간", "# 단위: 초") {
		
        @Override
        public boolean condition(Integer value) {
            return value >= 0;
        }
        
    };
    
	public static final SettingObject<Double> BLOOMING_WAIT = 
			abilitySettings.new SettingObject<Double>(GreenGarden.class, "blooming-wait", 3.0,
            "# 개화까지 걸리는 시간", "# 단위: 초") {
		
        @Override
        public boolean condition(Double value) {
            return value >= 0;
        }
        
    };
    
	public static final SettingObject<Double> FLOWER_EFFECT_DURATION =
			abilitySettings.new SettingObject<Double>(GreenGarden.class, "flower-effect-duration", 6.3,
            "# 효과 지속시간", "# 단위: 초") {
		
        @Override
        public boolean condition(Double value) {
            return value >= 0;
        }
        
    };
    
	public static final SettingObject<Double> RANGE = 
			abilitySettings.new SettingObject<Double>(GreenGarden.class, "range", 4.0,
            "# 꽃 영향 범위", "# 단위: 칸") {
		
        @Override
        public boolean condition(Double value) {
            return value >= 0;
        }
        
    };
    
    private final int maxseed = MAX_SEED.getValue();
    private final int recharge = (int) Math.ceil(Wreck.isEnabled(GameManager.getGame()) ? Wreck.calculateDecreasedAmount(100) * RECHARGE.getValue() : RECHARGE.getValue());
    private final int bloomingwait = (int) (BLOOMING_WAIT.getValue() * 20);
    private final int flowerduration = (int) (FLOWER_EFFECT_DURATION.getValue() * 20);
    private final double range = RANGE.getValue();
    private ActionbarChannel ac = newActionbarChannel();
    private List<Seed> seeds = new ArrayList<>();
    
	public boolean ActiveSkill(Material material, AbilityBase.ClickType clicktype) {
		if (material.equals(Material.IRON_INGOT) && clicktype.equals(AbilityBase.ClickType.RIGHT_CLICK)) {
			if (seeds.size() > 0) {
				new Plant(seeds.get(0), LocationUtil.floorY(getPlayer().getLocation()), flowerduration, bloomingwait).start();
				seeds.remove(0);
				ac.update(getSeedActionbars());
				return true;
			} else if (recharging.isRunning()) getPlayer().sendMessage("§2[§a!§2] §c보급까지§7: §f" + recharging.getCount() + "초");
		}
		return false;
	}
    
    public String getSeedActionbars() {
		final StringJoiner joiner = new StringJoiner(" ");
    	for (int a = 0; a < seeds.size(); a++) {
    		joiner.add(seeds.get(a).getSeedcolor());
    	}
    	
    	if (maxseed - seeds.size() > 0) {
        	for (int a = 0; a < maxseed - seeds.size(); a++) {
        		joiner.add("§7⧫");
        	}
    	}
    	return joiner.toString();
    }
    
    enum Seed {
    	POPPY("§4⧫", false, Material.POPPY, RGB.of(174, 1, 1)),
    	DANDELION("§a⧫", true, Material.DANDELION, RGB.of(254, 254, 38)),
    	ALLIUM("§5⧫", false, Material.ALLIUM, RGB.of(235, 97, 254)),
    	ORCHID("§b⧫", true, Material.BLUE_ORCHID, RGB.of(29, 189, 250)),
    	AZURE_BLUET("§f⧫", true, Material.AZURE_BLUET, RGB.of(254, 254, 225)),
    	LILAC("§d⧫", false, Material.LILAC, RGB.of(254, 208, 250)),
    	TULIP("§6⧫", true, Material.ORANGE_TULIP, RGB.of(254, 157, 91)),
    	ROSE("§c⧫", true, Material.ROSE_BUSH, RGB.of(254, 13, 19)),
    	SUNFLOWER("§e⧫", false, Material.SUNFLOWER, RGB.of(254, 185, 1));
    	
    	private final String seedcolor;
    	private final boolean positive;
    	private final Material flower;
    	private final RGB color;
    	
    	Seed(String seedcolor, boolean positive, Material flower, RGB color) {
    		this.seedcolor = seedcolor;
    		this.positive = positive;
    		this.flower = flower;
    		this.color = color;
    	}
    	
		public static Seed getRandomSeed() {
			final Random random = new Random();
			return random.pick(values());
		}
		
		public String getSeedcolor() {
			return seedcolor;
		}
		
		public boolean isPositive() {
			return positive;
		}
		
		public Material getFlower() {
			return flower;
		}
		
		public RGB getColor() {
			return color;
		}
		
    }
    
    public AbilityTimer recharging = new AbilityTimer(TaskType.REVERSE, Math.max(1, recharge)) {
    	
    	@Override
    	public void onStart() {
    		ac.update("§c보급까지§7: §f" + Math.max(1, recharge) + "초");
    	}

    	@Override
    	public void run(int count) {
    		ac.update("§c보급까지§7: §f" + count + "초");
    	}
    	
    	@Override
    	public void onEnd() {
    		onSilentEnd();
    	}
    	
    	@Override
    	public void onSilentEnd() {
    		seeds.clear();
    		for (int a = 0; a < maxseed; a++) {
        		seeds.add(Seed.getRandomSeed());	
    		}
    		ac.update(getSeedActionbars());
    	}
    	
	}.setPeriod(TimeUnit.SECONDS, 1).register();

	private class Plant extends AbilityTimer implements Listener {
		
		private final Seed seed;
		private final int flowerduration;
		private Location location;
		private final RGB seedcolor = RGB.of(18, 95, 41);
		private final ArmorStand hologram;
		private final DecimalFormat df = new DecimalFormat("0.0");
		private boolean onchange = true;
		private Block block;
		private IBlockSnapshot snapshot;
		private final Predicate<Entity> predicate;
		
		private Plant(Seed seed, Location location, int flowerduration, int bloomingduration) {
			super(TaskType.REVERSE, flowerduration + bloomingduration);
			setPeriod(TimeUnit.TICKS, 1);
			this.seed = seed;
			this.location = location;
			this.flowerduration = flowerduration;
			this.hologram = location.getWorld().spawn(location.clone().add(0, 0.75, 0), ArmorStand.class);
			hologram.setVisible(false);
			hologram.setGravity(false);
			hologram.setInvulnerable(true);
			NMS.removeBoundingBox(hologram);
			hologram.setCustomNameVisible(true);
			hologram.setCustomName("§a개화까지§7: §2" + df.format(getCount() / 20.0) + "§f초");
			this.predicate = new Predicate<Entity>() {
				@Override
				public boolean test(Entity entity) {
					if (seed.positive) {
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
			if (count > flowerduration) {
				//blooming
				hologram.setCustomName("§a개화까지§7: §2" + df.format((count - flowerduration) / 20.0) + "§f초");
				ParticleLib.REDSTONE.spawnParticle(location.clone().add(0, 0.15, 0), seedcolor);
			} else {
				//flower
				if (onchange) {
					this.block = location.clone().add(0, 1, 0).getBlock().getRelative(BlockFace.DOWN);
					location = block.getLocation();
					snapshot = Blocks.createSnapshot(block);
					block.setType(seed.getFlower());
					hologram.teleport(location.clone().add(0, 1.2, 0));
					onchange = false;
				}

				
				
				if (seed.getFlower().equals(Material.POPPY)) {
					for (Player player : LocationUtil.getEntitiesInCircle(Player.class, location, range, predicate)) {
						Poison.apply(getGame().getParticipant(player), TimeUnit.TICKS, count);
					}
				}
				
				if (seed.getFlower().equals(Material.DANDELION)) {
					for (Player player : LocationUtil.getEntitiesInCircle(Player.class, location, range, predicate)) {
						if (!player.isDead()) {
							final double maxHealth = player.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue();
							if (player.getHealth() < maxHealth) {
								final EntityRegainHealthEvent event = new EntityRegainHealthEvent(player, .3, RegainReason.CUSTOM);
								Bukkit.getPluginManager().callEvent(event);
								if (!event.isCancelled()) {
									player.setHealth(RangesKt.coerceIn(player.getHealth() + event.getAmount(), 0, maxHealth));
								}
							}
						}
					}
				}
				
				if (seed.getFlower().equals(Material.BLUE_ORCHID)) {
					for (Player player : LocationUtil.getEntitiesInCircle(Player.class, location, range, predicate)) {
						PotionEffects.SPEED.addPotionEffect(player, 3, 1, true);
					}
				}
				
				if (seed.getFlower().equals(Material.AZURE_BLUET)) {
					for (Player player : LocationUtil.getEntitiesInCircle(Player.class, location, range, predicate)) {
						PotionEffects.DAMAGE_RESISTANCE.addPotionEffect(player, 3, 0, true);
					}
				}
				
				if (seed.getFlower().equals(Material.LILAC)) {
					for (Player player : LocationUtil.getEntitiesInCircle(Player.class, location, range, predicate)) {
						Dream.apply(getGame().getParticipant(player), TimeUnit.TICKS, count);
					}
				}
				
				if (seed.getFlower().equals(Material.SUNFLOWER)) {
					for (Player player : LocationUtil.getEntitiesInCircle(Player.class, location, range, predicate)) {
						Charm.apply(getGame().getParticipant(player), TimeUnit.TICKS, count, getPlayer(), 30, 20);
					}
				}
			}
		}
		
		@EventHandler
		public void onBlockBreak(BlockBreakEvent e) {
			if (e.getBlock().equals(block)) e.setCancelled(true);
		}

		@EventHandler
		public void onExplode(BlockExplodeEvent e) {
			e.blockList().removeIf(blocks -> blocks.equals(block));
		}

		@EventHandler
		public void onExplode(EntityExplodeEvent e) {
			e.blockList().removeIf(blocks -> blocks.equals(block));
		}
		
	}
	
	
}
