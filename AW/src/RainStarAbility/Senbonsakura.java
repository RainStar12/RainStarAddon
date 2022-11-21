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
		"$[RANGE]칸 내에 $[PERIOD]초마다 칼날비가 떨어져 적에게 피해를 입힙니다.",
		"철괴 우클릭 시, ",
		"§b[§7아이디어 제공자§b] §bSlowrain"
		},
		summarize = {
		"일정 주기로 §d유도 화살§f을 §a충전§f합니다. §d유도 화살§f은 적 $[RANGE]칸에서 §5유도§f됩니다."
		})

public class Senbonsakura extends AbilityBase {
	
	public Senbonsakura(Participant participant) {
		super(participant);
	}
	
	private Random random = new Random();
	private static final EulerAngle DEFAULT_EULER_ANGLE = new EulerAngle(Math.toRadians(-10), 0, 0);
	private List<ArmorStand> armorstands = new ArrayList<>();
	private Player target;
	
	private AbilityTimer skill = new AbilityTimer() {
		
		@Override
		public void onStart() {
			for (int i = 0; i < (random.nextInt(20) + 1); i++) {
				armorstands.add(target.getWorld().spawn(target.getLocation().add(random.nextDouble() * 10 - 5, random.nextDouble() * 10 - 5, random.nextDouble() * 10 - 5), ArmorStand.class));
			}
			
			for (ArmorStand armorStand : armorstands) {
				armorStand.setFireTicks(Integer.MAX_VALUE);
				NMS.removeBoundingBox(armorStand);
				armorStand.setMetadata("SwordMaster", new FixedMetadataValue(AbilityWar.getPlugin(), null));
				armorStand.setVisible(false);
				armorStand.setInvulnerable(true);
				armorStand.getEquipment().setItemInMainHand(new ItemStack(Material.IRON_SWORD));
				armorStand.setGravity(false);
				armorStand.setRightArmPose(DEFAULT_EULER_ANGLE);	
			}
		}
		
		@Override
		public void run(int count) {
			
		}
		
	};
}