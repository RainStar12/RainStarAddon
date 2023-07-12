package rainstar.abilitywar.ability;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.Location;
import org.bukkit.FireworkEffect.Type;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Projectile;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitRunnable;

import daybreak.abilitywar.AbilityWar;
import daybreak.abilitywar.ability.AbilityBase;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.SubscribeEvent;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.config.ability.AbilitySettings.SettingObject;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.utils.base.random.Random;

@AbilityManifest(name = "명사수", rank = Rank.S, species = Species.HUMAN, explain = {
		"투사체 적중 시 자신과 대상의 §3거리§f 1칸당 공격력이 §c$[ADD_DAMAGE]%§f씩 증가합니다."
		},
		summarize = {
		"투사체 적중 시 자신과 대상의 §3거리§f 1칸당 공격력이 §c$[ADD_DAMAGE]%§f씩 증가합니다."
		})
public class BullsEye extends AbilityBase {
	
	public BullsEye(Participant participant) {
		super(participant);
	}
	
	public static final SettingObject<Double> ADD_DAMAGE = 
			abilitySettings.new SettingObject<Double>(BullsEye.class, "add-damage", 1.8,
			"# 거리 1칸당 대미지 증가량", "# 단위: %") {

		@Override
		public boolean condition(Double value) {
			return value >= 0;
		}

	};
	
	private final double adddamage = ADD_DAMAGE.getValue() * 0.01;
	private static final FixedMetadataValue NULL_VALUE = new FixedMetadataValue(AbilityWar.getPlugin(), null);
	private final Random random = new Random();
	
	public void firework(double value, Location location) {
		if (value >= 1.18) {
			final Firework firework = location.getWorld().spawn(location, Firework.class);
			final FireworkMeta meta = firework.getFireworkMeta();
			Type type = Type.BURST;
			List<Color> colors = new ArrayList<Color>();
			colors.add(Color.fromRGB(random.nextInt(256), random.nextInt(256), random.nextInt(256)));
			
			if (value > 1.234) {
				colors.add(Color.fromRGB(random.nextInt(256), random.nextInt(256), random.nextInt(256)));
			}
			if (value > 1.27) {
				colors.add(Color.fromRGB(random.nextInt(256), random.nextInt(256), random.nextInt(256)));
			}
			if (value > 1.306) {
				colors.add(Color.fromRGB(random.nextInt(256), random.nextInt(256), random.nextInt(256)));
			}
			if (value > 1.324) {
				colors.add(Color.fromRGB(random.nextInt(256), random.nextInt(256), random.nextInt(256)));
				type = Type.BALL;
			}
			if (value > 1.36) {
				colors.add(Color.fromRGB(random.nextInt(256), random.nextInt(256), random.nextInt(256)));
			}
			if (value > 1.414) {
				colors.add(Color.fromRGB(random.nextInt(256), random.nextInt(256), random.nextInt(256)));
				type = Type.CREEPER;
			}
			if (value > 1.45) {
				colors.add(Color.fromRGB(random.nextInt(256), random.nextInt(256), random.nextInt(256)));
				type = Type.STAR;
			}
			if (value > 1.54) {
				colors.add(Color.fromRGB(random.nextInt(256), random.nextInt(256), random.nextInt(256)));
			}
			if (value > 1.63) {
				colors.add(Color.fromRGB(random.nextInt(256), random.nextInt(256), random.nextInt(256)));
				type = Type.BALL_LARGE;
			}
			if (value >= 2.0) {
				colors.add(Color.fromRGB(random.nextInt(256), random.nextInt(256), random.nextInt(256)));
			}
			
			meta.addEffect(
					FireworkEffect.builder()
							.withColor(colors)
							.with(type)
							.withFlicker()
							.build()
			);
			meta.setPower(0);
			firework.setFireworkMeta(meta);
			firework.setMetadata("bullseye-firework", NULL_VALUE);
			new BukkitRunnable() {
				@Override
				public void run() {
					firework.detonate();
				}
			}.runTaskLater(AbilityWar.getPlugin(), 1L);	
		}
	}
	
	@SubscribeEvent
	public void onEntityDamageByEntity(EntityDamageByEntityEvent e) {
		if (e.getDamager().hasMetadata("bullseye-firework")) {
			e.setCancelled(true);
		}
		
		if (e.getDamager() instanceof Projectile) {
			Projectile projectile = (Projectile) e.getDamager();
			if (getPlayer().equals(projectile.getShooter())) {
				double multiply = 1 + (adddamage * Math.sqrt(e.getEntity().getLocation().distanceSquared(getPlayer().getLocation())));
				e.setDamage(e.getDamage() * multiply);
				firework(multiply, e.getEntity().getLocation());
			}
		}
	}

}
