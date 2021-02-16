package RainStarSynergy;

import org.bukkit.Material;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.SubscribeEvent;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.ability.decorator.ActiveHandler;
import daybreak.abilitywar.config.ability.AbilitySettings.SettingObject;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.list.mix.synergy.Synergy;
import daybreak.abilitywar.utils.base.Formatter;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.library.SoundLib;

@AbilityManifest(name = "무중력", rank = Rank.A, species = Species.OTHERS, explain = {
		"자신이 발사하는 모든 발사체가 5초간 중력의 영향을 무시하고 나아갑니다.",
		"철괴 우클릭 시, $[DURATION]초간 자신의 몸이 중력의 영향을 받지 않습니다.",
		"능력 지속 도중 다시 우클릭 시 즉시 끝낼 수 있습니다. $[COOLDOWN]",
		"또한 화살로 맞힌 개체를 3초간 중력을 역행해 띄웁니다.",
		"낙하 대미지를 무시합니다."
})

public class GravityZero extends Synergy implements ActiveHandler {
	
	public GravityZero(Participant participant) {
		super(participant);
	}
	
	@Override
	protected void onUpdate(Update update) {
		if (update == Update.ABILITY_DESTROY || update == Update.RESTRICTION_SET) {
			getPlayer().setGravity(true);
		}
	}
	
	private final Cooldown gravityc = new Cooldown(COOLDOWN.getValue());
	
	LivingEntity target;
	
	public static final SettingObject<Integer> COOLDOWN = synergySettings.new SettingObject<Integer>(GravityZero.class,
			"cooldown", 30, "# 무중력 쿨타임") {
		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}

		@Override
		public String toString() {
			return Formatter.formatCooldown(getValue());
		}
	};
	
	public static final SettingObject<Integer> DURATION = synergySettings.new SettingObject<Integer>(GravityZero.class,
			"duration", 10, "# 지속시간") {

		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}

	};
	
	@Override
	public boolean ActiveSkill(Material material, ClickType clickType) {
		if (material == Material.IRON_INGOT && clickType.equals(ClickType.RIGHT_CLICK) && !gravityc.isCooldown()) {
			if (nogravity.isRunning()) {
				nogravity.stop(false);
			} else {
				nogravity.start();
				return true;
			}
		}
		return false;
	}		
	
	private PotionEffect levitation = new PotionEffect(PotionEffectType.LEVITATION, 60, 3, true, false);
	
	@SubscribeEvent
	 public void onProjectileLaunch(ProjectileLaunchEvent e) {
		if (getPlayer().equals(e.getEntity().getShooter())) {
			
				new AbilityTimer(100) {
					
					@Override
					protected void run(int count) {
					}
					
					@Override
					protected void onStart() {
						e.getEntity().setGravity(false);
					}
					
					@Override
					protected void onEnd() {
						e.getEntity().setGravity(true);
					}

					@Override
					protected void onSilentEnd() {
						e.getEntity().setGravity(true);
					}
					
				}.setPeriod(TimeUnit.TICKS, 1).start();
		}
	}
	
	@SubscribeEvent
	private void onEntityDamage(EntityDamageEvent e) {
		if (!e.isCancelled() && getPlayer().equals(e.getEntity()) && e.getCause().equals(DamageCause.FALL)) {
			e.setCancelled(true);
			getPlayer().sendMessage("§a낙하 대미지를 받지 않습니다.");
			SoundLib.ENTITY_EXPERIENCE_ORB_PICKUP.playSound(getPlayer());
		}
	}
	
	@SubscribeEvent
	public void onEntityDamageByEntity(EntityDamageByEntityEvent e) {
		if (e.getDamager() instanceof Arrow) {
			Arrow ar = (Arrow) e.getDamager();
			if (getPlayer().equals(ar.getShooter()) && !e.getEntity().equals(getPlayer()) && e.getEntity() instanceof LivingEntity) {
				target = (LivingEntity) e.getEntity();
				target.addPotionEffect(levitation);
			}
		}
	}
	
	private final Duration nogravity = (Duration) new Duration(DURATION.getValue() * 20, gravityc) {
		
		@Override
		protected void onDurationProcess(int ticks) {
			getPlayer().setGravity(false);
		}
		
		@Override
		protected void onDurationStart() {
			getPlayer().sendMessage("§b무중력§f이 적용됩니다. 당신은 이제 §5중력§f의 영향을 무시합니다.");
		}
		
		@Override
		protected void onDurationEnd() {
			getPlayer().setGravity(true);
			getPlayer().sendMessage("다시 §5중력§f의 영향을 받습니다.");
		}
		
		@Override
		protected void onDurationSilentEnd() {
			getPlayer().setGravity(true);
			getPlayer().sendMessage("다시 §5중력§f의 영향을 받습니다.");
		}
		
	}.setPeriod(TimeUnit.TICKS, 1);
}