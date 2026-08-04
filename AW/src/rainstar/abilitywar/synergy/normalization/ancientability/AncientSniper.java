package rainstar.abilitywar.synergy.normalization.ancientability;

import static rainstar.abilitywar.system.config.RainStarAbilitySetting.rsAbilitySettings;

import org.bukkit.Material;
import org.bukkit.entity.Arrow;
import org.bukkit.event.entity.ProjectileLaunchEvent;

import daybreak.abilitywar.ability.AbilityBase;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.SubscribeEvent;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.config.ability.AbilitySettings.SettingObject;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.base.minecraft.nms.NMS;
import daybreak.abilitywar.utils.base.minecraft.version.ServerVersion;
import daybreak.abilitywar.utils.library.PotionEffects;

@AbilityManifest(name = "고대의 스나이퍼", rank = Rank.SPECIAL, species = Species.HUMAN, explain = {
        "스나이퍼가 쏘는 §b화살§f은 $[DURATION]초간 빠른 속도로 곧게 뻗어나가다 떨어집니다."
		})
public class AncientSniper extends AbilityBase {

	public AncientSniper(Participant participant) {
		super(participant);
	}
	
	public static final SettingObject<Double> DURATION = rsAbilitySettings.new SettingObject<Double>(AncientSniper.class, 
			"duration", 2.0, "# 화살 효과 적용 시간", "# 단위: 초") {

		@Override
		public boolean condition(Double value) {
			return value >= 0;
		}

	};
	
	private final int duration = (int) (DURATION.getValue() * 20);
	
	@Override
	public void onUpdate(Update update) {
		if (update == Update.RESTRICTION_CLEAR) {
			snipeMode.start();
		}
	}
	
	private final AbilityTimer snipeMode = new AbilityTimer() {
		@Override
		protected void run(int count) {
			final Material main = getPlayer().getInventory().getItemInMainHand().getType(), off = getPlayer().getInventory().getItemInOffHand().getType();
			if (main.equals(Material.BOW) || off.equals(Material.BOW) || (ServerVersion.getVersion() >= 14 && (main.equals(Material.CROSSBOW) || off.equals(Material.CROSSBOW)))) {
				PotionEffects.SLOW.addPotionEffect(getPlayer(), 5, 8, true);
				PotionEffects.JUMP.addPotionEffect(getPlayer(), 5, 200, true);
			}
		}
	}.setPeriod(TimeUnit.TICKS, 1).register();
	
	@SubscribeEvent
	public void onProjectileLaunch(ProjectileLaunchEvent e) {
		if (NMS.isArrow(e.getEntity())) {
			Arrow arrow = (Arrow) e.getEntity();
			if (getPlayer().equals(e.getEntity().getShooter())) {
				new AbilityTimer(duration) {

					@Override
					public void onStart() {
						arrow.setGlowing(true);
						arrow.setGravity(false);
						arrow.setVelocity(arrow.getVelocity().multiply(2.5));
					}

					@Override
					public void onEnd() {
						onSilentEnd();
					}

					@Override
					public void onSilentEnd() {
						arrow.setGlowing(false);
						arrow.setGravity(true);
					}

				}.setPeriod(TimeUnit.TICKS, 1).start();
			}
		}
	}
	
}
