package RainStarAbility;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.projectiles.ProjectileSource;
import org.bukkit.util.EulerAngle;
import org.bukkit.util.Vector;

import daybreak.abilitywar.AbilityWar;
import daybreak.abilitywar.ability.AbilityBase;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.game.AbstractGame.CustomEntity;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.module.DeathManager;
import daybreak.abilitywar.game.team.interfaces.Teamable;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.base.math.LocationUtil;
import daybreak.abilitywar.utils.base.math.VectorUtil;
import daybreak.abilitywar.utils.base.minecraft.entity.decorator.Deflectable;
import daybreak.abilitywar.utils.base.minecraft.nms.NMS;
import daybreak.abilitywar.utils.base.minecraft.raytrace.RayTrace;
import daybreak.abilitywar.utils.base.random.Random;
import daybreak.abilitywar.utils.library.SoundLib;
import daybreak.google.common.base.Predicate;

@AbilityManifest(name = "천본앵", rank = Rank.L, species = Species.HUMAN, explain = {
		"철괴 우클릭 시 $[DURATION]초간 $[RANGE]칸 내에 칼날§b비§f를 내리게 합니다. $[COOLDOWN]",
		"칼이 떨어지면서 파괴되고, 칼날이 §d벚꽃§f이 되어 §d벚꽃 스택§f이 최대 §d1000§f개까지 증가합니다.",
		"§d벚꽃 스택§f은 매번 10~50개를 획득할 수 있습니다. 1000개의 벚꽃을 전부 모으면,",
		"철괴 좌클릭으로 전부 소모해 $[SENBONSAKURA_DURATION]초간 주변에 벚꽃을 맴돌게 합니다.",
		"§b[§7아이디어 제공자§b] §bSlowRain"
		},
		summarize = {
		""
		})

public class Senbonsakura extends AbilityBase {
	
	public Senbonsakura(Participant participant) {
		super(participant);
	}
	
	private Random random = new Random();
	private static final EulerAngle DEFAULT_EULER_ANGLE = new EulerAngle(Math.toRadians(-10), 0, 0);
	private List<ArmorStand> armorstands = new ArrayList<>();
	private List<Location> locations = new ArrayList<>();
	private Player target;
	
	private AbilityTimer skill = new AbilityTimer() {
		
		@Override
		public void onStart() {
			for (int i = 0; i < 10; i++) {
				armorstands.add(getPlayer().getWorld().spawn(getPlayer().getLocation(), ArmorStand.class));
				locations.add(LocationUtil.getRandomLocation(target.getLocation(), 7.5));
			}
			
			for (ArmorStand armorStand : armorstands) {
				armorStand.setFireTicks(Integer.MAX_VALUE);
				NMS.removeBoundingBox(armorStand);
				armorStand.setMetadata("Senbonsakura", new FixedMetadataValue(AbilityWar.getPlugin(), null));
				armorStand.setVisible(false);
				armorStand.setInvulnerable(true);
				armorStand.getEquipment().setItemInMainHand(new ItemStack(Material.IRON_SWORD));
				armorStand.setGravity(false);
				armorStand.setRightArmPose(DEFAULT_EULER_ANGLE);	
			}
		}
		
		@Override
		public void run(int count) {
			for (ArmorStand armorstand : armorstands) {
				
			}
		}
		
	};
}