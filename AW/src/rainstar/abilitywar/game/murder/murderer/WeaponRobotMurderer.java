package rainstar.abilitywar.game.murder.murderer;

import java.text.DecimalFormat;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Predicate;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.block.Block;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerArmorStandManipulateEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.util.EulerAngle;
import org.bukkit.util.Vector;

import daybreak.abilitywar.AbilityWar;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.SubscribeEvent;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.AbstractGame.Participant.ActionbarNotification.ActionbarChannel;
import daybreak.abilitywar.game.list.murdermystery.Items;
import daybreak.abilitywar.game.list.murdermystery.MurderMystery;
import daybreak.abilitywar.game.list.murdermystery.MurderMystery.ArrowKillEvent;
import daybreak.abilitywar.game.list.murdermystery.ability.AbstractMurderer;
import daybreak.abilitywar.utils.base.color.Gradient;
import daybreak.abilitywar.utils.base.color.RGB;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.base.math.LocationUtil;
import daybreak.abilitywar.utils.base.math.geometry.Circle;
import daybreak.abilitywar.utils.base.minecraft.boundary.CenteredBoundingBox;
import daybreak.abilitywar.utils.base.minecraft.entity.decorator.Deflectable;
import daybreak.abilitywar.utils.base.minecraft.nms.NMS;
import daybreak.abilitywar.utils.base.minecraft.version.ServerVersion;
import daybreak.abilitywar.utils.library.MaterialX;
import daybreak.abilitywar.utils.library.ParticleLib;
import daybreak.abilitywar.utils.library.PotionEffects;
import daybreak.abilitywar.utils.library.SoundLib;

@AbilityManifest(name = "머더: 개조병기", rank = Rank.SPECIAL, species = Species.OTHERS, explain = {
		"모든 시민을 제거하세요!",
		"금 우클릭으로 금 8개를 소모해 활과 화살을 얻을 수 있습니다.",
		"검을 들었을 때 이동 속도 증가가 감소합니다.",
		"§8[§3반사 외갑§8]§f 금이 15개 이상 있다면 전부 소모해 중첩 불가능한 §3실드§f를 생성합니다.",
		"§3실드§f는 화살을 막아내고 5초간 모든 화살을 튕겨냅니다. §c쿨타임 §7: §f1분",
		"반사 외갑이 지속되는 도중 신속 4를 받지만, 15초간 근접 공격이 불가합니다.",
		"§8[§6기계팔§8]§f 금 좌클릭으로 금 2개를 소모해 로켓 손을 발사하여 적중한",
		"대상을 자신의 위치까지 끌어옵니다. §c쿨타임 §7: §f3초"
})
public class WeaponRobotMurderer extends AbstractMurderer {
	
	public WeaponRobotMurderer(Participant participant) {
		super(participant);
	}
	
	@Override
	protected void onUpdate(Update update) {
		super.onUpdate(update);
		if (update == Update.RESTRICTION_CLEAR) {
			NMS.sendTitle(getPlayer(), "§e직업§f: §6개조병기", "§f모든 §a시민§f의 공격을 막아내고 끌어당겨 제거하세요.", 10, 80, 10);
			new AbilityTimer(1) {
				@Override
				protected void onEnd() {
					NMS.clearTitle(getPlayer());
				}
			}.setInitialDelay(TimeUnit.SECONDS, 5).start();
			PASSIVE.stop(false);
			speedpassive.start();
			passive.start();
		} else if (update == Update.ABILITY_DESTROY) {
			NMS.setInvisible(getPlayer(), false);
		}
	}
	
	private final Cooldown pcooldown = new Cooldown(60), acooldown = new Cooldown(3);
	private boolean shield = false;
	private ActionbarChannel ac = newActionbarChannel(), ac2 = newActionbarChannel();
	private DecimalFormat df = new DecimalFormat("0.0");
	private final Circle circle = Circle.of(1, 25);
	private static final RGB cyan = RGB.of(1, 150, 126), darkblue = RGB.of(1, 3, 99);
	private List<RGB> gradation = Gradient.createGradient(30, cyan, darkblue);
	
	private final Predicate<Entity> predicate = new Predicate<Entity>() {
		@Override
		public boolean test(Entity entity) {
			if (entity.equals(getPlayer())) return false;
			if (entity.hasMetadata("Rockethand")) return false;
			return !(entity instanceof Player) || (getGame().isParticipating(entity.getUniqueId())
					&& !((MurderMystery) getGame()).isDead(entity.getUniqueId())
					&& getGame().getParticipant(entity.getUniqueId()).attributes().TARGETABLE.getValue());
		}
	};
	
	protected final AbilityTimer speedpassive = new AbilityTimer() {
		@Override
		protected void run(int count) {
			if (Items.isMurdererSword(getPlayer().getInventory().getItemInMainHand())) {
				getPlayer().getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).setBaseValue(0.12);
			}
		}
	}.setPeriod(TimeUnit.TICKS, 1).register();
	
	@SubscribeEvent(onlyRelevant = true)
	private void onInteract(PlayerInteractEvent e) {
		if (Items.isGold(e.getItem())) {
			if (e.getAction() == Action.RIGHT_CLICK_AIR || e.getAction() == Action.RIGHT_CLICK_BLOCK) {
				final MurderMystery murderMystery = (MurderMystery) getGame();
				if (murderMystery.consumeGold(getParticipant(), 8)) {
					if (!addArrow()) {
						murderMystery.addGold(getParticipant());
					} else {
						if (!hasBow()) {
							getPlayer().getInventory().setItem(2, Items.NORMAL_BOW.getStack());
						}
					}
				}
			} else if (e.getAction() == Action.LEFT_CLICK_AIR || e.getAction() == Action.LEFT_CLICK_BLOCK) {
				final MurderMystery murderMystery = (MurderMystery) getGame();
				if (!acooldown.isCooldown() && !skill.isRunning()) {
					if (murderMystery.consumeGold(getParticipant(), 2)) {
						skill.start();
						getPlayer().sendMessage("§4[§c!§4] §6로켓 손§f을 발사하였습니다.");
					}	
				}
			}
		}
	}
	
	@SubscribeEvent(priority = 5)
	private void onArrowKill(ArrowKillEvent e) {
		if (!e.isCancelled()) {
			if (!(e.getShooter().getAbility() instanceof AbstractMurderer) && e.getTarget().equals(getParticipant()) && shield) {
				e.setCancelled(true);
				SoundLib.ENTITY_ZOMBIE_ATTACK_IRON_DOOR.playSound(getPlayer().getLocation(), 1, (float) ((random.nextInt(16) * 0.1) + 0.5));
				ParticleLib.CRIT_MAGIC.spawnParticle(getPlayer().getLocation(), 0.5, 1, 0.5, 10, 0);
				if (!reflecting.isRunning()) reflecting.start();
			}	
		}
	}
	
	@SubscribeEvent
	private void onMurderer(MurderEvent e) {
		if (cantkill.isRunning() && e.getMurderer().getParticipant().equals(getParticipant())) e.setCancelled(true);
	}
	
	public AbilityTimer passive = new AbilityTimer() {
		
		@Override
		public void run(int count) {
			if (!shield && !pcooldown.isRunning() && !reflecting.isRunning()) {
				final MurderMystery murderMystery = (MurderMystery) getGame();
				if (murderMystery.consumeGold(getParticipant(), 15)) {
					murderMystery.consumeGold(getParticipant(), murderMystery.getGold(getParticipant()));
					shield = true;
					ac.update("§3⃣");
				}
			}
		}
		
	}.setPeriod(TimeUnit.TICKS, 1).register();
	
	private final AbilityTimer cantkill = new AbilityTimer(300) {
		
		@Override
		protected void run(int count) {
			ac2.update("§c근접 공격 불가§f: §3" + df.format(count / 20.0) + "§f초");
		}
		
		@Override
		protected void onEnd() {
			onSilentEnd();
		}

		@Override
		protected void onSilentEnd() {
			ac2.update(null);
		}
		
	}.setPeriod(TimeUnit.TICKS, 1).register();
	
	public AbilityTimer reflecting = new AbilityTimer(100) {
		
		private double y;
		private boolean add;
		private int particlecount = 1;
		private boolean reverse = false;
		
		@Override
		public void onStart() {
			cantkill.start();
		}
		
		@Override
		public void run(int count) {
			ac.update("§3반사 외갑§7: §b" + df.format(count / 20.0) + "§f초");
			
			if (add && y >= 2.0) add = false;
			else if (!add && y <= 0) add = true;

			y = add ? y + 0.1 : y - 0.1;
			
			if (particlecount == gradation.size()-1 || particlecount == 0) reverse = !reverse;
			particlecount = Math.min(gradation.size()-1, particlecount + (reverse ? -1 : +1));
			
			for (Location location : circle.toLocations(getPlayer().getLocation().add(0, y, 0))) {
				ParticleLib.REDSTONE.spawnParticle(location, gradation.get(particlecount));
			}
			for (Location location : circle.toLocations(getPlayer().getLocation().add(0, 2.0 - y, 0))) {
				ParticleLib.REDSTONE.spawnParticle(location, gradation.get(particlecount));
			}
			
			for (Entity entity : LocationUtil.getNearbyEntities(Entity.class, getPlayer().getLocation(), 8, 8, null)) {
				deflect(entity, false);
			}
			
			PotionEffects.SPEED.addPotionEffect(getPlayer(), 20, 3, false, false, true);
		}
		
		@Override
		public void onEnd() {
			pcooldown.start();
			onSilentEnd();
		}
		
		@Override
		public void onSilentEnd() {
			shield = false;
			ac.update(null);
		}
		
	}.setPeriod(TimeUnit.TICKS, 1).register();
	
	private final Duration skill = new Duration(20, acooldown) {
		
		private LinkedList<ArmorStand> handParts;
		private LivingEntity target;
		private Vector direction;
		private EulerAngle headPose;
		private Location current;
		private final ItemStack head = MaterialX.ORANGE_WOOL.createItem();
		private CenteredBoundingBox boundingBox;
		
		protected void onDurationStart() {
			this.handParts = new LinkedList<>();
			this.target = null;
			this.direction = getPlayer().getLocation().getDirection().normalize().multiply(0.83);
			this.headPose = new EulerAngle(this.direction.getX(), this.direction.getY(), this.direction.getZ());
			this.current = getPlayer().getLocation().clone().add(this.direction);
			this.boundingBox = CenteredBoundingBox.of(this.current, -0.75D, -0.75D, -0.75D, 0.75D, 0.75D, 0.75D);
		}
		
		protected void onDurationProcess(int i) {
			for (int a = 0; a < 2; a++) {
				ArmorStand handPart = (ArmorStand) getPlayer().getWorld().spawn(this.current, ArmorStand.class);
				handPart.setHeadPose(this.headPose);
				handPart.setVisible(false);
				handPart.getEquipment().setHelmet(this.head);
				handPart.setGravity(false);
				if (ServerVersion.getVersion() >= 10) handPart.setInvulnerable(true); 
				handPart.setMetadata("Rockethand", (MetadataValue) new FixedMetadataValue(AbilityWar.getPlugin(), null));
				this.handParts.add(handPart);
				this.current.add(this.direction);
				this.boundingBox.setCenter(handPart.getEyeLocation());
				final Block block = handPart.getLocation().clone().add(0, 1.5, 0).getBlock();
				final Material type = block.getType();
				if (type.isSolid()) {
					stop(false);
					return;
				}
				List<LivingEntity> entities = LocationUtil.getConflictingEntities(LivingEntity.class, getPlayer().getWorld(), boundingBox, predicate);
				if (entities.size() > 0) {
					this.target = entities.get(0);
					stop(false);
					return;
				} 
			}
		}
		
		protected void onDurationEnd() {
			if (this.target == null) {
				for (ArmorStand armorStand : this.handParts)
				armorStand.remove(); 
			} else {
				new AbilityTimer(this.handParts.size()) {
					private final LinkedList<ArmorStand> partList = handParts;
					private final LivingEntity targetEntity = target;
					
					protected void run(int i) {
						if (ServerVersion.getVersion() >= 10) this.targetEntity.setGravity(false); 
						ArmorStand armorStand = this.partList.pollLast();
						this.targetEntity.teleport(armorStand.getEyeLocation());
						armorStand.remove();
					}
	                
					protected void onEnd() {
						if (ServerVersion.getVersion() >= 10) this.targetEntity.setGravity(true); 
						this.targetEntity.setFallDistance(0.0F); 
					}
				}.setPeriod(TimeUnit.TICKS, 1).start();
			} 
		}
	        
		protected void onDurationSilentEnd() {
			for (ArmorStand armorStand : this.handParts) {
				armorStand.remove(); 
			}
		}
		
	}.setPeriod(TimeUnit.TICKS, 1);
	
	@SubscribeEvent
	private void onPlayerArmorStandManipulate(PlayerArmorStandManipulateEvent e) {
		if (e.getRightClicked().hasMetadata("Rockethand")) e.setCancelled(true); 
	}
	
	private boolean deflect(Entity entity, boolean playerDirection) {
		if (entity == null) return false;
		if (entity instanceof Projectile) {
			return deflect((Projectile) entity, playerDirection);
		} else {
			final List<MetadataValue> metadatas = entity.getMetadata("deflectable");
			if (!metadatas.isEmpty()) {
				final Object value = metadatas.get(0).value();
				if (value instanceof Deflectable) {
					return deflect((Deflectable) value, playerDirection);
				}
			}
		}
		return false;
	}

	private boolean deflect(Projectile projectile, boolean playerDirection) {
		if (projectile != null && !projectile.isOnGround() && projectile.isValid() && !getPlayer().equals(projectile.getShooter())) {
			if (projectile.hasMetadata("flector")) {
				if (getPlayer().getUniqueId().equals(projectile.getMetadata("flector").get(0).value())) {
					return false;
				} else {
					projectile.removeMetadata("flector", AbilityWar.getPlugin());
				}
			}
			projectile.setVelocity(playerDirection ? getPlayer().getLocation().getDirection().multiply(2.2) : projectile.getLocation().toVector().subtract(getPlayer().getLocation().toVector()).normalize().add(projectile.getVelocity().multiply(-1)));
			projectile.setMetadata("flector", new FixedMetadataValue(AbilityWar.getPlugin(), getPlayer().getUniqueId()));
			SoundLib.ENTITY_PLAYER_ATTACK_SWEEP.playSound(getPlayer());
			SoundLib.ENTITY_ZOMBIE_ATTACK_IRON_DOOR.playSound(getPlayer().getLocation(), 1, (float) ((random.nextInt(16) * 0.1) + 0.5));
			ParticleLib.SWEEP_ATTACK.spawnParticle(projectile.getLocation());
			return true;
		}
		return false;
	}

	private boolean deflect(Deflectable deflectable, boolean playerDirection) {
		if (deflectable != null && !getPlayer().equals(deflectable.getShooter())) {
			deflectable.onDeflect(getParticipant(), playerDirection ? getPlayer().getLocation().getDirection().multiply(2.2) : deflectable.getLocation().toVector().subtract(getPlayer().getLocation().toVector()).normalize().add(deflectable.getDirection().multiply(-1)));
			SoundLib.ENTITY_PLAYER_ATTACK_SWEEP.playSound(getPlayer());
			SoundLib.ENTITY_ZOMBIE_ATTACK_IRON_DOOR.playSound(getPlayer().getLocation(), 1, (float) ((random.nextInt(16) * 0.1) + 0.5));
			ParticleLib.SWEEP_ATTACK.spawnParticle(deflectable.getLocation());
			return true;
		}
		return false;
	}

}
