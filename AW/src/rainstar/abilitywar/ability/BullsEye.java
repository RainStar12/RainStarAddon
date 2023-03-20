package rainstar.abilitywar.ability;

import org.bukkit.Location;
import org.bukkit.entity.Projectile;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.metadata.FixedMetadataValue;

import daybreak.abilitywar.AbilityWar;
import daybreak.abilitywar.ability.AbilityBase;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.SubscribeEvent;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.config.ability.AbilitySettings.SettingObject;
import daybreak.abilitywar.game.AbstractGame.Participant;

@AbilityManifest(name = "명사수", rank = Rank.S, species = Species.HUMAN, explain = {
		"투사체 적중 시 자신과 대상의 §3거리§f 1칸당 §c$[ADD_DAMAGE]%§f씩 증가합니다."
		},
		summarize = {
		"투사체 적중 시 자신과 대상의 §3거리§f 1칸당 §c$[ADD_DAMAGE]%§f씩 증가합니다."
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
	
	public void firework(double value, Location location) {
		
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
