package rainstar.abilitywar.ability;

import static rainstar.abilitywar.system.config.RainStarAbilitySetting.rsAbilitySettings;

import java.text.DecimalFormat;

import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

import daybreak.abilitywar.ability.AbilityBase;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.ability.SubscribeEvent;
import daybreak.abilitywar.config.ability.AbilitySettings.SettingObject;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.AbstractGame.Participant.ActionbarNotification.ActionbarChannel;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.base.minecraft.nms.NMS;

@AbilityManifest(name = "거인", rank = Rank.S, species = Species.HUMAN, explain = {
		"§e흡수 체력§f을 포함한 §d현재 체력§f에 비례해 §c공격력§f이 증가합니다.",
		"§8(§7초기 최대 체력의 $[HP_SCALE]%당 공격력 +$[DAMAGE_INCREASE]%§8)"
		})
public class Giant extends AbilityBase {
	
	public Giant(Participant participant) {
		super(participant);
	}
	
	public static final SettingObject<Double> HP_SCALE = rsAbilitySettings.new SettingObject<Double>(Giant.class,
			"hp-scale", 10.0, "# 최대 체력의 기준비율", "# 단위: %") {
		@Override
		public boolean condition(Double value) {
			return value >= 0;
		}
	};
	
	public static final SettingObject<Double> DAMAGE_INCREASE = rsAbilitySettings.new SettingObject<Double>(Giant.class,
			"damage-increase", 4.0, "# 증가하는 공격력", "# 단위: %") {
		@Override
		public boolean condition(Double value) {
			return value >= 0;
		}
	};
	
	private final double hpScale = HP_SCALE.getValue() * 0.01;
	private final double damageIncrease = DAMAGE_INCREASE.getValue() * 0.01;
	private double maxHealth = 0;
	private ActionbarChannel ac = newActionbarChannel();
	private DecimalFormat df = new DecimalFormat("0.0");
	
	@Override
	public void onUpdate(Update update) {
		if (update == Update.RESTRICTION_CLEAR) {
			if (maxHealth == 0) maxHealth = getPlayer().getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue();
			passive.start();
		}
	}
	
	private AbilityTimer passive = new AbilityTimer() {
		
		@Override
		public void run(int count) {
			ac.update("§c공격력 증가§7: §e" + df.format(getIncrease() * 100) + "%");
		}
		
	}.setPeriod(TimeUnit.TICKS, 1).register();
	
	@SubscribeEvent()
	private void onEntityDamageByEntity(EntityDamageByEntityEvent e) {
		Player damager = null;
		if (e.getDamager() instanceof Player) damager = (Player) e.getDamager();
		else if (e.getDamager() instanceof Projectile) {
			Projectile projectile = (Projectile) e.getDamager();
			if (projectile.getShooter() != null && projectile.getShooter() instanceof Player) damager = (Player) projectile.getShooter();
		}
		
		if (getPlayer().equals(damager)) {
			e.setDamage(e.getDamage() + (e.getDamage() * getIncrease()));
		}
	}
	
	public double getIncrease() {
		double nowHealth = getPlayer().getHealth() + NMS.getAbsorptionHearts(getPlayer());
		double scale = maxHealth * hpScale;
		return (nowHealth / scale) * damageIncrease;
	}

}
