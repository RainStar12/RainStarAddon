package rainstar.abilitywar.synergy;

import javax.annotation.Nullable;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Note;
import org.bukkit.Note.Tone;
import org.bukkit.attribute.Attribute;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Damageable;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Snowman;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.util.Vector;

import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.Materials;
import daybreak.abilitywar.ability.SubscribeEvent;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.ability.decorator.ActiveHandler;
import daybreak.abilitywar.config.ability.AbilitySettings.SettingObject;
import daybreak.abilitywar.config.enums.CooldownDecrease;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.AbstractGame.Participant.ActionbarNotification.ActionbarChannel;
import daybreak.abilitywar.game.list.mix.synergy.Synergy;
import daybreak.abilitywar.game.manager.effect.Frost;
import daybreak.abilitywar.game.manager.effect.event.ParticipantPreEffectApplyEvent;
import daybreak.abilitywar.game.module.DeathManager;
import daybreak.abilitywar.utils.base.Formatter;
import daybreak.abilitywar.utils.base.color.RGB;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.base.math.LocationUtil;
import daybreak.abilitywar.utils.base.math.VectorUtil.Vectors;
import daybreak.abilitywar.utils.base.math.geometry.Circle;
import daybreak.abilitywar.utils.base.math.geometry.Points;
import daybreak.abilitywar.utils.base.minecraft.damage.Damages;
import daybreak.abilitywar.utils.base.minecraft.nms.NMS;
import daybreak.abilitywar.utils.base.random.Random;
import daybreak.abilitywar.utils.library.MaterialX;
import daybreak.abilitywar.utils.library.ParticleLib;
import daybreak.abilitywar.utils.library.PotionEffects;
import daybreak.abilitywar.utils.library.SoundLib;
import daybreak.google.common.base.Predicate;
import daybreak.google.common.base.Strings;
import rainstar.abilitywar.effect.Chill;
import rainstar.abilitywar.effect.FrozenHeart;
import rainstar.abilitywar.effect.SnowflakeMark;

@AbilityManifest(name = "유키<눈꽃>", rank = Rank.L, species = Species.HUMAN, explain = {
		"냉기를 자유자재로 다루는 §b얼음 속성§f의 눈꽃 마법사, 유키.",
		"§7패시브 §8- §b콜드 마스터§f: 빙결, 냉기, 눈꽃 표식, 프로즌 하트 상태이상을",
		" 무효화시키고 효과를 받을 때마다 최대 10의 눈꽃 결정 스택을 쌓아",
		" 윈터 퍼레이드의 피해를 강화합니다.",
		"§7지팡이 우클릭 §8- §b윈터 퍼레이드§f: 영창을 시작합니다. 최대 단계 상태에서 다시",
		" 지팡이를 우클릭하면 7칸의 범위에 $[DURATION]초간 지속적으로 마법 피해를 입히고",
		" 적중 대상을 $[EFFECT_DURATION]초간 프로즌 하트 상태로 만듭니다. $[CAST_COOLDOWN]",
		" 눈과 얼음 위에서 대미지가 2 증가하며 신속 버프를 받습니다.",
		"§7상태이상 §8- §3프로즌 하트§f: 모든 회복 효과를 받을 수 없습니다.",
		" 슬롯을 변경할 때마다 2.5초를 대기해야 하고, 이동 속도와 공격 속도가 감소합니다.",
		" 효과가 해제될 때, 원래 받을 회복 효과를 절반으로 줄여서 한꺼번에 받습니다.",
		" 이 효과는 이동할 때마다 더 빨리 지속시간이 줄어듭니다."
		})

@Materials(materials = {
		Material.STICK
	})

public class YukiSnow extends Synergy implements ActiveHandler {
	
	public YukiSnow(Participant participant) {
		super(participant);
	}
	
	private boolean given = true;
	private Random random = new Random();
	
	private Vectors layer1vectors;
	private Vectors layer2vectors;
	private Location loc;
	
	private Vector zeroVec = new Vector(0, 0, 0);
	
	private static final Circle circle1 = Circle.of(5, 100);
	private static final Circle circle2 = Circle.of(6, 150);
	private static final Circle circle3 = Circle.of(7, 200);
	
	private final Cooldown castcool = new Cooldown(CAST_COOLDOWN.getValue(), "영창", CooldownDecrease._25);
	private boolean snowman = SNOWMAN_SPAWN.getValue();
	
	private final ActionbarChannel ac = newActionbarChannel();
	
	private int stack = 0;
	private int cast = 0;
	private int damage = 0;
	
	private static final RGB color1 = RGB.of(254, 254, 254), color2 = RGB.of(238, 254, 254), color3 = RGB.of(227, 253, 254),
			color4 = RGB.of(210, 253, 254), color5 = RGB.of(200, 253, 254), color6 = RGB.of(190, 252, 254),
			color7 = RGB.of(172, 252, 254), color8 = RGB.of(159, 252, 254), color9 = RGB.of(136, 252, 254),
			color10 = RGB.of(117, 252, 254), color11 = RGB.of(100, 250, 254), color12 = RGB.of(81, 250, 254),
			fieldcolor1 = RGB.of(195, 254, 254), fieldcolor2 = RGB.of(101, 196, 254), ringcolor1 = RGB.of(145, 247, 241),
			ringcolor2 = RGB.of(62, 240, 232);
	
	private static final Points LAYER = Points.of(0.12, new boolean[][]{
		{false, false, false, false, false, false, false, true, false, false, false, false, false, false, false},
		{false, false, false, false, false, true, false, false, false, true, false, false, false, false, false},
		{false, false, false, false, false, false, true, true, true, false, false, false, false, false, false},
		{true, false, true, false, false, false, false, false, false, false, false, false, true, false, true},
		{false, false, true, false, false, false, false, true, false, false, false, false, true, false, false},
		{true, true, true, false, false, true, false, true, false, true, false, false, true, true, true},
		{false, false, false, true, false, false, true, true, true, false, false, true, false, false, false},
		{false, false, false, false, true, true, false, false, false, true, true, false, false, false, false},
		{false, true, true, false, false, false, false, true, false, false, false, false, true, true, false},
		{false, false, false, false, true, true, false, false, false, true, true, false, false, false, false},
		{false, false, false, true, false, false, true, true, true, false, false, true, false, false, false},
		{true, true, true, false, false, true, false, true, false, true, false, false, true, true, true},
		{false, false, true, false, false, false, false, true, false, false, false, false, true, false, false},
		{true, false, true, false, false, false, false, false, false, false, false, false, true, false, true},
		{false, false, false, false, false, false, true, true, true, false, false, false, false, false, false},
		{false, false, false, false, false, true, false, false, false, true, false, false, false, false, false},
		{false, false, false, false, false, false, false, true, false, false, false, false, false, false, false}
	});
	
	private static final Points LAYER1 = Points.of(0.35, new boolean[][]{
		{false, false, false, false, false, false, false, false, false, false, true, true, true, false, false, false, false, false, false, false, false, false, false},
		{false, false, false, false, false, false, false, false, true, true, true, false, true, true, true, false, false, false, false, false, false, false, false},
		{false, false, false, false, false, false, false, false, true, false, true, true, true, false, true, false, false, false, false, false, false, false, false},
		{false, false, false, false, false, false, false, true, true, true, false, false, false, true, true, true, false, false, false, false, false, false, false},
		{false, false, false, false, false, false, false, true, false, true, true, false, true, true, false, true, false, false, false, false, false, false, false},
		{true, true, true, true, true, false, false, true, true, false, true, false, true, false, true, true, false, false, true, true, true, true, true},
		{true, false, true, false, true, true, true, false, true, true, false, false, false, true, true, false, true, true, true, false, true, false, true},
		{true, true, true, false, true, false, true, false, false, true, true, false, true, true, false, false, true, false, true, false, true, true, true},
		{true, false, false, false, true, false, true, false, false, true, true, false, true, true, false, false, true, false, true, false, false, false, true},
		{true, true, true, true, false, false, true, false, false, true, false, true, false, true, false, false, true, false, false, true, true, true, true},
		{false, true, false, false, false, false, true, true, true, true, false, true, false, true, true, true, true, false, false, false, false, true, false},
		{false, true, true, true, true, true, false, true, false, false, true, true, true, false, false, true, false, true, true, true, true, true, false},
		{false, false, false, false, false, true, true, false, true, true, true, false, true, true, true, false, true, true, false, false, false, false, false},
		{false, true, true, true, true, true, false, true, false, false, true, true, true, false, false, true, false, true, true, true, true, true, false},
		{false, true, false, false, false, false, true, true, true, true, false, true, false, true, true, true, true, false, false, false, false, true, false},
		{true, true, true, true, false, false, true, false, false, true, false, true, false, true, false, false, true, false, false, true, true, true, true},
		{true, false, false, false, true, false, true, false, false, true, true, false, true, true, false, false, true, false, true, false, false, false, true},
		{true, true, true, false, true, false, true, false, false, true, true, false, true, true, false, false, true, false, true, false, true, true, true},
		{true, false, true, false, true, true, true, false, true, true, false, false, false, true, true, false, true, true, true, false, true, false, true},
		{true, true, true, true, true, false, false, true, true, false, true, false, true, false, true, true, false, false, true, true, true, true, true},
		{false, false, false, false, false, false, false, true, false, true, true, false, true, true, false, true, false, false, false, false, false, false, false},
		{false, false, false, false, false, false, false, true, true, true, false, false, false, true, true, true, false, false, false, false, false, false, false},
		{false, false, false, false, false, false, false, false, true, false, true, true, true, false, true, false, false, false, false, false, false, false, false},
		{false, false, false, false, false, false, false, false, true, true, true, false, true, true, true, false, false, false, false, false, false, false, false},
		{false, false, false, false, false, false, false, false, false, false, true, true, true, false, false, false, false, false, false, false, false, false, false},
	});
	
	private static final Points LAYER2 = Points.of(0.35, new boolean[][]{
		{false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false},
		{false, false, false, false, false, false, false, false, false, false, false, true, false, false, false, false, false, false, false, false, false, false, false},
		{false, false, false, false, false, false, false, false, false, true, false, false, false, true, false, false, false, false, false, false, false, false, false},
		{false, false, false, false, false, false, false, false, false, false, true, true, true, false, false, false, false, false, false, false, false, false, false},
		{false, false, false, false, false, false, false, false, true, false, false, true, false, false, true, false, false, false, false, false, false, false, false},
		{false, false, false, false, false, false, false, false, false, true, false, true, false, true, false, false, false, false, false, false, false, false, false},
		{false, true, false, true, false, false, false, false, false, false, true, true, true, false, false, false, false, false, false, true, false, true, false},
		{false, false, false, true, false, true, false, false, false, false, false, true, false, false, false, false, false, true, false, true, false, false, false},
		{false, true, true, true, false, true, false, false, false, false, false, true, false, false, false, false, false, true, false, true, true, true, false},
		{false, false, false, false, true, true, false, false, false, false, true, false, true, false, false, false, false, true, true, false, false, false, false},
		{false, false, true, true, true, true, false, false, false, false, true, false, true, false, false, false, false, true, true, true, true, false, false},
		{false, false, false, false, false, false, true, false, true, true, false, false, false, true, true, false, true, false, false, false, false, false, false},
		{false, false, false, false, false, false, false, true, false, false, false, true, false, false, false, true, false, false, false, false, false, false, false},
		{false, false, false, false, false, false, true, false, true, true, false, false, false, true, true, false, true, false, false, false, false, false, false},
		{false, false, true, true, true, true, false, false, false, false, true, false, true, false, false, false, false, true, true, true, true, false, false},
		{false, false, false, false, true, true, false, false, false, false, true, false, true, false, false, false, false, true, true, false, false, false, false},
		{false, true, true, true, false, true, false, false, false, false, false, true, false, false, false, false, false, true, false, true, true, true, false},
		{false, false, false, true, false, true, false, false, false, false, false, true, false, false, false, false, false, true, false, true, false, false, false},
		{false, true, false, true, false, false, false, false, false, false, true, true, true, false, false, false, false, false, false, true, false, true, false},
		{false, false, false, false, false, false, false, false, false, true, false, true, false, true, false, false, false, false, false, false, false, false, false},
		{false, false, false, false, false, false, false, false, true, false, false, true, false, false, true, false, false, false, false, false, false, false, false},
		{false, false, false, false, false, false, false, false, false, false, true, true, true, false, false, false, false, false, false, false, false, false, false},
		{false, false, false, false, false, false, false, false, false, true, false, false, false, true, false, false, false, false, false, false, false, false, false},
		{false, false, false, false, false, false, false, false, false, false, false, true, false, false, false, false, false, false, false, false, false, false, false},
		{false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false}
	});
	
	
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
			}
			return true;
		}

		@Override
		public boolean apply(@Nullable Entity arg0) {
			return false;
		}
	};
	
	public static final SettingObject<Integer> CAST_COOLDOWN 
	= synergySettings.new SettingObject<Integer>(YukiSnow.class,
			"cast-cooldown", 40, "# 영창 쿨타임") {
		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}

		@Override
		public String toString() {
			return Formatter.formatCooldown(getValue());
		}
	};
	
	public static final SettingObject<Integer> DURATION 
	= synergySettings.new SettingObject<Integer>(YukiSnow.class,
			"duration", 5, "# 필드 지속시간") {
		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}
	};
	
	public static final SettingObject<Integer> EFFECT_DURATION 
	= synergySettings.new SettingObject<Integer>(YukiSnow.class,
			"effect-duration", 15, "# 프로즌 하트 지속시간") {
		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}
	};
	
	public static final SettingObject<Boolean> SNOWMAN_SPAWN 
	= synergySettings.new SettingObject<Boolean>(YukiSnow.class,
			"snowman-spawn", true, "# 유키가 플레이어를 죽일 때 눈사람을",
			"# 소환할 지 여부를 정합니다.") {
	};
	
	@Override
	protected void onUpdate(Update update) {
		if (update == Update.RESTRICTION_CLEAR && given) {			
			ItemStack stick = new ItemStack(Material.STICK, 1);
			ItemMeta stickmeta = stick.getItemMeta();	
			
			if (random.nextInt(100) == 0) {
				stickmeta.setDisplayName("§6딱총나무 지팡이");
			} else {
				stickmeta.setDisplayName("§b설한의 지팡이");
			}		
			stickmeta.addEnchant(Enchantment.SILK_TOUCH, 1, true);
			stickmeta.addEnchant(Enchantment.KNOCKBACK, 2, true);
			stickmeta.setUnbreakable(true);		
			stick.setItemMeta(stickmeta);
			
			getPlayer().getInventory().addItem(stick);
			given = false;
		}
		if (update == Update.RESTRICTION_CLEAR) {
			buff.start();
		}
	}
	
	@Override
	public boolean usesMaterial(Material material) {
		return (material == MaterialX.STICK.getMaterial());
	}
	
	@SuppressWarnings("deprecation")
	@Override
	public boolean ActiveSkill(Material material, ClickType clicktype) {
		if (material == Material.STICK && clicktype == ClickType.RIGHT_CLICK && !castcool.isCooldown()) {
			if (casting.isRunning() && cast >= 12) {
				casting.stop(false);
				music.start();
				if (field.isRunning()) {
					field.stop(false);
				}
				field.start();
				cast = 0;
				return castcool.start();
			} else if (!casting.isRunning() && getPlayer().isOnGround()) {
				casting.start();
			}
		}
		return false;
	}
	
	@SubscribeEvent
	public void onPlayerDeath(PlayerDeathEvent e) {
		if (snowman) {
			if (e.getEntity() instanceof Player && getPlayer().equals(e.getEntity().getKiller())) {
				Snowman snowman = e.getEntity().getWorld().spawn(e.getEntity().getLocation(), Snowman.class);
				snowman.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(50);
				snowman.setHealth(50);
			}	
		}
	}
	
	@SubscribeEvent(onlyRelevant = true)
	public void onParticipantEffectApply(ParticipantPreEffectApplyEvent e) {
		if (e.getEffectType().equals(Frost.registration) || e.getEffectType().equals(Chill.registration) || 
				e.getEffectType().equals(FrozenHeart.registration) || e.getEffectType().equals(SnowflakeMark.registration)) {
			e.setCancelled(true);
			if (stack < 10) {
				stack++;
				ac.update("§b눈꽃 결정 스택§f: " + stack);
			}
		}
	}
	
	private final AbilityTimer buff = new AbilityTimer() {

		@Override
		public void run(int count) {
			Material m = getPlayer().getLocation().getBlock().getType();
			Material bm = getPlayer().getLocation().clone().subtract(0, 1, 0).getBlock().getType();
			if (m.equals(Material.SNOW) || bm.equals(Material.SNOW) || bm.equals(Material.SNOW_BLOCK) || bm.equals(Material.ICE) || bm.equals(Material.PACKED_ICE)) {
				PotionEffects.SPEED.addPotionEffect(getPlayer(), 5, 1, true);
			}
		}

	}.setPeriod(TimeUnit.TICKS, 1).register();
	
	private final AbilityTimer field = new AbilityTimer(100) {
		
		@Override
		public void onStart() {
			loc = getPlayer().getLocation().clone();
			layer1vectors = LAYER1.clone().rotateAroundAxisX(90).clone();
			layer2vectors = LAYER2.clone().rotateAroundAxisX(90).clone();
			Material m = getPlayer().getLocation().getBlock().getType();
			Material bm = getPlayer().getLocation().subtract(0, 1, 0).getBlock().getType();
			if (m.equals(Material.SNOW) || bm.equals(Material.SNOW) || bm.equals(Material.SNOW_BLOCK) || bm.equals(Material.ICE) || bm.equals(Material.PACKED_ICE)) {
				damage = 8;
			} else {
				damage = 6;
			}
		}
		
    	@Override
		public void run(int count) {
    		if (count % 20 == 0) {
    			for (Damageable d : LocationUtil.getNearbyEntities(Damageable.class, loc, 7, 7, predicate)) {
    				Damages.damageMagic(d, getPlayer(), false, (float) (damage + (stack * 0.25)));
    				if (d instanceof Player) {
    					Player p = (Player) d;
    					FrozenHeart.apply(getGame().getParticipant(p), TimeUnit.SECONDS, EFFECT_DURATION.getValue());
    				}
    			}
    		}
    		if (count % 10 == 0) {
    			ParticleLib.SNOW_SHOVEL.spawnParticle(loc.clone().add(0, 4, 0), 4, 1, 4, 200, 0);
    		}
			if (count % 4 == 0) {
				for (Location location : layer1vectors.toLocations(loc).floor(loc.getY())) {
					ParticleLib.REDSTONE.spawnParticle(location.clone().add(-0.19, 0, -4.41), fieldcolor1);
				}
				for (Location location : layer2vectors.toLocations(loc).floor(loc.getY())) {
					ParticleLib.REDSTONE.spawnParticle(location.clone().add(-0.19, 0, -4.41), fieldcolor2);
				}
				for (Location location : circle1.toLocations(loc).floor(loc.getY())) {
					ParticleLib.REDSTONE.spawnParticle(location, ringcolor2);
				}
				for (Location location : circle2.toLocations(loc).floor(loc.getY())) {
					ParticleLib.REDSTONE.spawnParticle(location, ringcolor1);
				}
				for (Location location : circle3.toLocations(loc).floor(loc.getY())) {
					ParticleLib.REDSTONE.spawnParticle(location, ringcolor2);
				}
			}
			
    	}
		
	}.setPeriod(TimeUnit.TICKS, 1).register();
	
	private final AbilityTimer casting = new AbilityTimer() {
		
    	@Override
		public void run(int count) {
    		if (count % 5 == 0) {
        		if (cast < 12) {
        			cast++;
        		}
        		final Location eyeLocation = getPlayer().getLocation().clone().add(getPlayer().getLocation().getDirection().add(new Vector(0, 2.5, 0)));
    			final float yaw = getPlayer().getLocation().getYaw();
    			for (Location loc : LAYER.rotateAroundAxisY(-yaw).toLocations(eyeLocation)) {
    				switch(cast) {
    				case 1: ParticleLib.REDSTONE.spawnParticle(loc, color1);
    				break;
    				case 2: ParticleLib.REDSTONE.spawnParticle(loc, color2);
    				break;
    				case 3: ParticleLib.REDSTONE.spawnParticle(loc, color3);
    				break;
    				case 4: ParticleLib.REDSTONE.spawnParticle(loc, color4);
    				break;
    				case 5: ParticleLib.REDSTONE.spawnParticle(loc, color5);
    				break;
    				case 6: ParticleLib.REDSTONE.spawnParticle(loc, color6);
    				break;
    				case 7: ParticleLib.REDSTONE.spawnParticle(loc, color7);
    				break;
    				case 8: ParticleLib.REDSTONE.spawnParticle(loc, color8);
    				break;
    				case 9: ParticleLib.REDSTONE.spawnParticle(loc, color9);
    				break;
    				case 10: ParticleLib.REDSTONE.spawnParticle(loc, color10);
    				break;
    				case 11: ParticleLib.REDSTONE.spawnParticle(loc, color11);
    				break;
    				case 12: ParticleLib.REDSTONE.spawnParticle(loc, color12);
    				break;
    				}
    			}
    			LAYER.rotateAroundAxisY(yaw);
			}
    		NMS.sendTitle(getPlayer(), Strings.repeat("§b§k|", cast).concat(Strings.repeat("§7§k|", 12 - cast)), "", 0, 100, 1);	
    	}
    	
    	@Override
    	public void onEnd() {
    		onSilentEnd();
    	}
    	
    	@Override
    	public void onSilentEnd() {
    		NMS.clearTitle(getPlayer());
    	}
		
	}.setPeriod(TimeUnit.TICKS, 1).register();
	
	private final AbilityTimer music = new AbilityTimer() {

    	@Override
		public void run(int count) {
    		switch(count) {
    		case 1:
    		case 13:
    			SoundLib.CHIME.playInstrument(getPlayer().getLocation(), Note.natural(1, Tone.G));
				break;
    		case 3:
    		case 15:
    			SoundLib.CHIME.playInstrument(getPlayer().getLocation(), Note.natural(1, Tone.A));
				break;
    		case 4:
    		case 16:
    			SoundLib.CHIME.playInstrument(getPlayer().getLocation(), Note.sharp(1, Tone.A));
				break;
    		case 6:
    		case 18:
    			SoundLib.CHIME.playInstrument(getPlayer().getLocation(), Note.natural(1, Tone.F));
    			break;
    		case 22: 
    			SoundLib.CHIME.playInstrument(getPlayer().getLocation(), Note.natural(1, Tone.D));
    			break;
    		case 25:
    		case 28: 
    			SoundLib.CHIME.playInstrument(getPlayer().getLocation(), Note.natural(1, Tone.C));
				break;
    		case 27: 
    			SoundLib.CHIME.playInstrument(getPlayer().getLocation(), Note.natural(0, Tone.D));
				break;
    		case 31: 
    			SoundLib.CHIME.playInstrument(getPlayer().getLocation(), Note.sharp(1, Tone.A));
				break;
    		case 33: 
    			SoundLib.CHIME.playInstrument(getPlayer().getLocation(), Note.sharp(0, Tone.D));
				break;
    		case 34: 
    			SoundLib.CHIME.playInstrument(getPlayer().getLocation(), Note.natural(1, Tone.C));
				break;
    		case 37:
    			SoundLib.CHIME.playInstrument(getPlayer().getLocation(), Note.natural(1, Tone.D));
				break;
    		case 40:
    			SoundLib.CHIME.playInstrument(getPlayer().getLocation(), Note.natural(1, Tone.C));
				break;
    		case 43:
    			SoundLib.CHIME.playInstrument(getPlayer().getLocation(), Note.sharp(1, Tone.A));
				break;
    		case 46:
    			SoundLib.CHIME.playInstrument(getPlayer().getLocation(), Note.natural(1, Tone.A));
    			stop(false);
    			break;
    		}
    	}
		
	}.setPeriod(TimeUnit.TICKS, 2).register();
	
	@SubscribeEvent
	public void onPlayerMove(PlayerMoveEvent e) {
		if (casting.isRunning() && e.getPlayer().equals(getPlayer()) && cast < 12) getPlayer().setVelocity(zeroVec);
	}
	
}