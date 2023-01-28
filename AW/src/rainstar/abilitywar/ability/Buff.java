package rainstar.abilitywar.ability;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.annotation.Nullable;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.entity.EntityRegainHealthEvent.RegainReason;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import daybreak.abilitywar.ability.AbilityBase;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.SubscribeEvent;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.ability.decorator.ActiveHandler;
import daybreak.abilitywar.config.ability.AbilitySettings.SettingObject;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.AbstractGame.Participant.ActionbarNotification.ActionbarChannel;
import daybreak.abilitywar.game.manager.effect.Rooted;
import daybreak.abilitywar.game.manager.effect.Stun;
import daybreak.abilitywar.game.module.DeathManager;
import daybreak.abilitywar.game.team.interfaces.Teamable;
import daybreak.abilitywar.utils.base.Formatter;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.library.ParticleLib;
import daybreak.abilitywar.utils.library.SoundLib;
import daybreak.google.common.base.Predicate;
import daybreak.abilitywar.utils.base.concurrent.SimpleTimer.TaskType;
import daybreak.abilitywar.utils.base.math.LocationUtil;
import daybreak.abilitywar.utils.base.minecraft.FallingBlocks;
import daybreak.abilitywar.utils.base.minecraft.FallingBlocks.Behavior;
import daybreak.abilitywar.utils.base.minecraft.entity.health.Healths;
import daybreak.abilitywar.utils.base.minecraft.version.ServerVersion;

@AbilityManifest(name = "근육돼지", rank = Rank.L, species = Species.HUMAN, explain = {
		"§7패시브 §8- §c근육 강화§f: §6힘 §f또는 §3저항§f $[AMPLIFIER] 버프를 획득합니다. $[EFFECT_CHANGE]초마다 효과는 변경됩니다.",
		" §6힘§f일 땐 §e파운딩§f 스킬을, §3저항§f일 땐 §a스테로이드§f 스킬을 사용할 수 있습니다.",
		" 두 스킬은 §c쿨타임§f을 공유합니다. $[COOLDOWN]",
        "§7철괴 우클릭§8(§6힘§8) §8- §e파운딩§f: 높게 떠오른 뒤 바라보는 방향으로 찍어내립니다.",
        " $[RANGE]칸 내 대상에게 피해입히고 $[STUN]초간 §e§n기절§f시킵니다.",
        " 피해량은 인벤토리의 차지된 칸에 비례합니다. §8(§7칸당 $[INVENTORY_DAMAGE]§8)",
        "§7철괴 우클릭§8(§3저항§8) §8- §a스테로이드§f: $[CHANNELING]초간 §2§n이동 불가§f 후 체력을 $[HEAL_AMOUNT] §d회복§f합니다.",
		"§a[§e능력 제공자§a] §dhandony"
		},
		summarize = {
		"§6힘§f 또는 §7저항§f 버프를 바꿔가며 획득합니다.",
		"버프에 따라 사용할 수 있는 스킬이 달라집니다.",
		"§7철괴 우클릭§8(§6힘§8)§7 시§f 높게 떠오른 뒤 바라보는 방향으로 찍고",
		"범위 내 대상에게 인벤토리 차지칸에 비례한 피해를 입히고 기절시킵니다.",
		"§7철괴 우클릭§8(§3저항§8)§7 시§f 잠시간 §2§n이동 불가§f 후 체력을 회복합니다."
		})

public class Buff extends AbilityBase implements ActiveHandler {
	
	public Buff(Participant participant) {
		super(participant);
	}
	
	public static final SettingObject<Integer> COOLDOWN = abilitySettings.new SettingObject<Integer>(
			Buff.class, "cooldown", 75, "# 쿨타임") {

		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}

		@Override
		public String toString() {
			return Formatter.formatCooldown(getValue());
		}

	};
	
	public static final SettingObject<Integer> AMPLIFIER = abilitySettings.new SettingObject<Integer>(
			Buff.class, "amplifier", 0, "# 포션 효과 계수", "# 주의! 0부터 시작합니다.", "# 0일 때 포션 효과 계수는 1레벨,", "# 1일 때 포션 효과 계수는 2레벨입니다.") {

		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}
		
		@Override
		public String toString() {
			return "" + (1 + getValue());
        }

	};
	
	public static final SettingObject<Double> RANGE = abilitySettings.new SettingObject<Double>(
			Buff.class, "range", 5.0, "# 파운딩 범위") {

		@Override
		public boolean condition(Double value) {
			return value >= 0;
		}

	};
	
	public static final SettingObject<Double> INVENTORY_DAMAGE = abilitySettings.new SettingObject<Double>(
			Buff.class, "item-damage", 0.4, "# 채워진 칸당 피해량") {

		@Override
		public boolean condition(Double value) {
			return value >= 0;
		}

	};
	
	public static final SettingObject<Double> EFFECT_CHANGE = abilitySettings.new SettingObject<Double>(
			Buff.class, "effect-change", 10.0, "# 효과 변경 주기") {

		@Override
		public boolean condition(Double value) {
			return value >= 0;
		}

	};

	public static final SettingObject<Double> STUN = abilitySettings.new SettingObject<Double>(
			Buff.class, "stun", 1.0, "# 기절 지속시간") {

		@Override
		public boolean condition(Double value) {
			return value >= 0;
		}

	};
	
	public static final SettingObject<Double> CHANNELING = abilitySettings.new SettingObject<Double>(
			Buff.class, "channeling", 3.0, "# 이동 불가 시간") {

		@Override
		public boolean condition(Double value) {
			return value >= 0;
		}

	};
	
	public static final SettingObject<Double> HEAL_AMOUNT = abilitySettings.new SettingObject<Double>(
			Buff.class, "heal-amount", 5.0, "# 체력 회복량") {

		@Override
		public boolean condition(Double value) {
			return value >= 0;
		}

	};
	
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
	
	private final Cooldown cool = new Cooldown(COOLDOWN.getValue());
	private final int stun = (int) (STUN.getValue() * 20);
	private final int channelingDur = (int) (CHANNELING.getValue() * 20);
	private final int amplifier = AMPLIFIER.getValue();
	private final int period = (int) (EFFECT_CHANGE.getValue() * 20);
	private final double healamount = HEAL_AMOUNT.getValue();
	private final double range = RANGE.getValue();
	private final double damage = INVENTORY_DAMAGE.getValue();
	private boolean nofall = false;
	private boolean str = false;
	private ActionbarChannel ac = newActionbarChannel();
	
	private final PotionEffect strength = new PotionEffect(PotionEffectType.INCREASE_DAMAGE, period, amplifier, true, false);
	private final PotionEffect resistance = new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, period, amplifier, true, false);
	
	@Override
	public void onUpdate(Update update) {
		if (update == Update.RESTRICTION_CLEAR) {
			buff.start();
		}
	}
	
	@Override
	public boolean ActiveSkill(Material material, ClickType clickType) {
		if (material == Material.IRON_INGOT && clickType == ClickType.RIGHT_CLICK && !cool.isCooldown() && !pounding.isRunning() && !steroid.isRunning()) {
			return (str ? pounding.start() : steroid.start());
		}
		return false;
	}
	
	private final AbilityTimer buff = new AbilityTimer() {
	
		@Override
		public void run(int count) {
			str = !str;
			ac.update("§c근육 강화§f: " + (str ? "§6힘" : "§3저항"));
			for (int i = 0; i < 5; i++) {
				SoundLib.BLOCK_CHORUS_FLOWER_GROW.playSound(getPlayer().getLocation(), 1, 0.75f);
				ParticleLib.TOTEM.spawnParticle(getPlayer().getLocation().clone().add(0, 1, 0), 0, 0, 0, 10, 0.33);
			}
			getPlayer().addPotionEffect(str ? strength : resistance);
		}
		
	}.setPeriod(TimeUnit.TICKS, period).register();
	
	@SuppressWarnings("deprecation")
	private final AbilityTimer pounding = new AbilityTimer() {

		@Override
		public void onStart() {
			this.setCount(0);
			nofall = true;
			getPlayer().setVelocity(new Vector(0, 1.7, 0));
		}

		@Override
		public void run(int count) {
			if (count >= 10) {
				if (count == 10) getPlayer().setVelocity(new Vector(0, 0, 0));
				getPlayer().setVelocity(getPlayer().getLocation().getDirection().normalize().multiply(4).setY(-4));
				if (getPlayer().isOnGround()) {
					getPlayer().setVelocity(new Vector(0, 0, 0));
					SoundLib.ENTITY_GENERIC_EXPLODE.playSound(getPlayer().getLocation(), 1, 1);
					if (ServerVersion.getVersion() >= 13) {
						for (Block block : LocationUtil.getBlocks2D(getPlayer().getLocation(), 3, true, true, true)) {
							if (block.getType() == Material.AIR) block = block.getRelative(BlockFace.DOWN);
							if (block.getType() == Material.AIR) continue;
							Location location = block.getLocation().add(0, 1, 0);
							FallingBlocks.spawnFallingBlock(location, block.getType(), false, getPlayer().getLocation().toVector().subtract(location.toVector()).multiply(-0.1).setY(Math.random()), Behavior.FALSE);
						}
					} else {
						for (Block block : LocationUtil.getBlocks2D(getPlayer().getLocation(), 3, true, true, true)) {
							if (block.getType() == Material.AIR) block = block.getRelative(BlockFace.DOWN);
							if (block.getType() == Material.AIR) continue;
							Location location = block.getLocation().add(0, 1, 0);
							FallingBlocks.spawnFallingBlock(location, block.getType(), block.getData(), false, getPlayer().getLocation().toVector().subtract(location.toVector()).multiply(-0.1).setY(Math.random()), Behavior.FALSE);
						}
					}
					List<ItemStack> list = new CopyOnWriteArrayList<>(getPlayer().getInventory().getContents());					
					for (ItemStack item : list) {
						if (item == null) {
							list.remove(item);
							continue;
						}
					}
					double nowdamage = list.size() * damage;
					for (LivingEntity livingEntity : LocationUtil.getNearbyEntities(LivingEntity.class, getPlayer().getLocation(), range, range, predicate)) {
						livingEntity.damage(nowdamage, getPlayer());
						if (livingEntity instanceof Player) Stun.apply(getGame().getParticipant((Player) livingEntity), TimeUnit.TICKS, stun);
					}
					stop(false);
				}
			}
		}

		@Override
		public void onEnd() {
			onSilentEnd();
		}
		
		@Override
		public void onSilentEnd() {
			cool.start();
		}

	}.setPeriod(TimeUnit.TICKS, 1).register();
	
	@SubscribeEvent
	private void onEntityDamage(EntityDamageEvent e) {
		if (e.getEntity().equals(getPlayer()) && nofall) {
			if (e.getCause().equals(DamageCause.FALL)) {
				e.setCancelled(true);
				nofall = false;
			}
		}
	}
	
	private final AbilityTimer steroid = new AbilityTimer(TaskType.REVERSE, channelingDur) {
		
		@Override
		public void onStart() {
			Rooted.apply(getParticipant(), TimeUnit.TICKS, channelingDur);
		}

		@Override
		public void onEnd() {
			onSilentEnd();
		}
		
		@Override
		public void onSilentEnd() {
			final EntityRegainHealthEvent event = new EntityRegainHealthEvent(getPlayer(), healamount, RegainReason.CUSTOM);
			Bukkit.getPluginManager().callEvent(event);
			if (!event.isCancelled()) {
				Healths.setHealth(getPlayer(), getPlayer().getHealth() + healamount);	
			}
			cool.start();
		}

	}.setPeriod(TimeUnit.TICKS, 1).register();
	
}
