package rainstar.abilitywar.ability;

import static rainstar.abilitywar.system.config.RainStarAbilitySetting.rsAbilitySettings;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import org.bukkit.Location;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Entity;
import org.bukkit.entity.FishHook;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerFishEvent.State;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.util.Vector;

import daybreak.abilitywar.ability.AbilityBase;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.ability.SubscribeEvent;
import daybreak.abilitywar.config.ability.AbilitySettings.SettingObject;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.module.DeathManager;
import daybreak.abilitywar.game.team.interfaces.Teamable;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.base.math.LocationUtil;
import daybreak.abilitywar.utils.base.math.geometry.Circle;
import daybreak.abilitywar.utils.library.MaterialX;
import daybreak.abilitywar.utils.library.ParticleLib;
import daybreak.abilitywar.utils.library.SoundLib;
import daybreak.google.common.base.Predicate;
import rainstar.abilitywar.effect.Corrosion;
import rainstar.abilitywar.effect.Moisture;

@AbilityManifest(name = "낚시꾼", rank = Rank.A, species = Species.HUMAN, explain = {
		"기본적으로 내구도 무한의 낚싯대 하나를 지급받고, 낚싯찌 속도가 빨라집니다.",
		"낚싯찌가 지면에 닿으면 찌 주변 $[RANGE]칸 내의 대상에게 §3§n습기§f를 $[MOISTURE_DURATION]초 부여하고,",
		"다른 대상을 낚으면 끌려오는 힘이 강력해지며 §7§n부식§f을 $[CORROSION_DURATION]초 부여합니다."
		})
public class Fisherman extends AbilityBase {
	
	public Fisherman(Participant participant) {
		super(participant);
	}
	
	public static final SettingObject<Double> RANGE = rsAbilitySettings.new SettingObject<Double>(Fisherman.class, 
			"range", 3.0, "# 습기 범위", "# 단위: 칸") {
		@Override
		public boolean condition(Double value) {
			return value >= 0;
		}
	};

	public static final SettingObject<Double> MOISTURE_DURATION = rsAbilitySettings.new SettingObject<Double>(Fisherman.class, 
			"moisture-duration", 1.0, "# 습기 지속시간", "# 단위: 초") {
		@Override
		public boolean condition(Double value) {
			return value >= 0;
		}	
	};
	
	public static final SettingObject<Double> CORROSION_DURATION = rsAbilitySettings.new SettingObject<Double>(Fisherman.class, 
			"corrosion-duration", 2.0, "# 부식 지속시간", "# 단위: 초") {
		@Override
		public boolean condition(Double value) {
			return value >= 0;
		}	
	};
	
	private final double range = RANGE.getValue();
	private final int moistureDuration = (int) (MOISTURE_DURATION.getValue() * 20);
	private final int corrosionDuration = (int) (CORROSION_DURATION.getValue() * 20);
	private final Circle circle1 = Circle.of(range, (int) Math.min(200, range * 10));
	private final Circle circle2 = Circle.of(range/2, (int) Math.min(200, (range / 2) * 10));
	private final Circle circle3 = Circle.of(range/3, (int) Math.min(200, (range / 3) * 10));
	private boolean onetime = true;
	
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
	
	@SuppressWarnings("serial")
	private List<String> lores = new ArrayList<String>() {
		{
			add("§7");
			add("§7무엇이든 낚을 수 있을 것 같은 기분입니다.");
		}
	};
	
	@Override
	protected void onUpdate(AbilityBase.Update update) {
	    if (update == AbilityBase.Update.RESTRICTION_CLEAR) {
	    	if (onetime) {
				ItemStack fishingrod = new ItemStack(MaterialX.FISHING_ROD.getMaterial(), 1);
				ItemMeta fishingrodmeta = fishingrod.getItemMeta();
				fishingrodmeta.setDisplayName("§b영원의 낚싯대");
				fishingrodmeta.setLore(lores);
				fishingrodmeta.setUnbreakable(true);
				fishingrod.setItemMeta(fishingrodmeta);
				fishingrod.addUnsafeEnchantment(Enchantment.LUCK, 10);
				fishingrod.addUnsafeEnchantment(Enchantment.LURE, 10);
				fishingrod.addUnsafeEnchantment(Enchantment.ARROW_INFINITE, 10);
				getPlayer().getInventory().addItem(fishingrod);
	    		onetime = false;
	    	}
	    }
	}
	
	@SubscribeEvent
	private void onPlayerFish(PlayerFishEvent e) {
		if (getPlayer().equals(e.getPlayer()) && e.getState() == State.CAUGHT_ENTITY && e.getCaught() instanceof LivingEntity) {
	        LivingEntity target = (LivingEntity) e.getCaught();
	        Vector v = e.getPlayer().getLocation().toVector().subtract(target.getLocation().toVector()).normalize().multiply(2.5);
	        v.setY(v.getY() + 0.3);
	        target.setVelocity(v);
	        if (target instanceof Player && getGame().isParticipating((Player) target)) {
	        	Corrosion.apply(getGame().getParticipant((Player) target), TimeUnit.TICKS, corrosionDuration);
	        }
	    }
	}
	
	
	@SubscribeEvent
	private void onProjectileLaunch(ProjectileLaunchEvent e) {
		if (getPlayer().equals(e.getEntity().getShooter()) && e.getEntity() instanceof FishHook) {
			e.getEntity().setVelocity(e.getEntity().getVelocity().clone().multiply(2));
		}
	}
	
	@SubscribeEvent
	private void onProjectileHit(ProjectileHitEvent e) {
		if (getPlayer().equals(e.getEntity().getShooter()) && e.getHitEntity() == null && e.getEntity() instanceof FishHook) {
			new AbilityTimer(3) {
				
				Location location;
				
				@Override
				public void onStart() {
					location = e.getEntity().getLocation();
				}
				
				@Override
				public void run(int count) {
					Circle circle;
					switch(count) {
					case 3:
						circle = circle3;
						break;
					case 2:
						circle = circle2;
						break;
					case 1:
						circle = circle1;
						break;
					default:
						circle = circle1;
						break;
					}
	    			for (Location loc : circle.toLocations(location).floor(location.getY())) {
	    				ParticleLib.DRIP_WATER.spawnParticle(getPlayer(), loc, 0, 0, 0, 1, 0);
					}
				}
				
			}.setPeriod(TimeUnit.TICKS, 2).start();
			
			SoundLib.ENTITY_GENERIC_SPLASH.playSound(e.getEntity().getLocation(), 1, 2);
			
			for (Player player : LocationUtil.getNearbyEntities(Player.class, e.getEntity().getLocation(), range, range, predicate)) {
				Moisture.apply(getGame().getParticipant(player), TimeUnit.TICKS, moistureDuration);
			}
		}
	}
	
}
