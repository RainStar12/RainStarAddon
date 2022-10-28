package RainStarAbility;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.StringJoiner;

import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.entity.EntityDamageByBlockEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerMoveEvent;

import daybreak.abilitywar.ability.AbilityBase;
import daybreak.abilitywar.ability.AbilityFactory;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.ability.decorator.ActiveHandler;
import daybreak.abilitywar.ability.decorator.TargetHandler;
import daybreak.abilitywar.ability.SubscribeEvent;
import daybreak.abilitywar.ability.AbilityFactory.AbilityRegistration;
import daybreak.abilitywar.config.Configuration;
import daybreak.abilitywar.config.ability.AbilitySettings.SettingObject;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.list.mix.Mix;
import daybreak.abilitywar.game.list.mix.synergy.SynergyFactory;
import daybreak.abilitywar.utils.base.Formatter;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.base.minecraft.entity.health.event.PlayerSetHealthEvent;
import daybreak.abilitywar.utils.base.minecraft.nms.NMS;
import daybreak.abilitywar.utils.base.random.Random;
import daybreak.abilitywar.utils.library.PotionEffects;
import daybreak.abilitywar.utils.library.SoundLib;
import daybreak.google.common.collect.ImmutableMap;

@AbilityManifest(name = "수녀", rank = Rank.S, species = Species.HUMAN, explain = {
		"§7패시브 §8- §b십자가§f: §c언데드§f 종족에게 가하는 피해가 $[UNDEAD_DAMAGE]% 증가합니다.",
		" §b신§f 종족에게 받는 피해가 $[GOD_DAMAGE_DECREASE]% 감소합니다.",
		"§7철괴 우클릭 §8- §3신의 은총§f: $[CHANNELING_COUNT]초간 간절히 기도합니다. $[COOLDOWN]",
		" 기도가 끝나면 무작위 §b신§f이 $[GOD_DURATION]초간 강림해 그 §b신§f의 능력을 사용할 수 있습니다.",
		" 강신 도중엔 사망하지 않으며, §b신§f의 등급당 $[RANK_DAMAGE]%의 §c추가 피해§f를 입힙니다.",
		"§8[§7HIDDEN§8] §b11:25§f: 예수께서 이르시되 나는 부활이요 생명이니",
		"§e---------------------------------",
		"$(EXPLAIN)",
		"§e---------------------------------"
		},
		summarize = {
		"§c언데드§f에게 주는 피해가 증가하고, §b신§f에게 받는 피해가 감소합니다.",
		"§7철괴 우클릭 시§f 기도한 후 무작위 §b신§f이 강림해 §b신§f의 능력을 사용합니다.",
		"강신 도중엔 사망하지 않으며, 신의 등급에 비례해 추가 피해를 입힙니다.",
		"§e---------------------------------",
		"$(SUM_EXPLAIN)",
		"§e---------------------------------"
		})

public class Nun extends AbilityBase implements ActiveHandler, TargetHandler {
	
	public Nun(Participant participant) {
		super(participant);
	}
	
	public static final SettingObject<Integer> COOLDOWN = abilitySettings.new SettingObject<Integer>(
			Nun.class, "cooldown", 80, "# 기도 쿨타임") {

		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}

		@Override
		public String toString() {
			return Formatter.formatCooldown(getValue());
		}

	};
	
	public static final SettingObject<Double> CHANNELING_COUNT = abilitySettings.new SettingObject<Double>(
			Nun.class, "channeling", 3.0, "# 기도하는 시간", "# 단위: 초") {
		@Override
		public boolean condition(Double value) {
			return value >= 0;
		}
	};
	
	
	public static final SettingObject<Double> GOD_DURATION = abilitySettings.new SettingObject<Double>(
			Nun.class, "being-god-duration", 30.0, "# 강신 지속시간", "# 단위: 초") {
		@Override
		public boolean condition(Double value) {
			return value >= 0;
		}
	};
	
	public static final SettingObject<Integer> RANK_DAMAGE = abilitySettings.new SettingObject<Integer>(
			Nun.class, "rank-damage", 7, "# 랭크당 공격력 증가 수치", "# 단위: %") {
		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}
	};
	
	public static final SettingObject<Integer> UNDEAD_DAMAGE = abilitySettings.new SettingObject<Integer>(
			Nun.class, "undead-damage", 25, "# 언데드 종족에게 가하는 피해량 증가 수치", "# 단위: %") {
		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}
	};
	
	public static final SettingObject<Integer> GOD_DAMAGE_DECREASE = abilitySettings.new SettingObject<Integer>(
			Nun.class, "god-damage-decrease", 20, "# 신 종족에게 받는 대미지 감소", "# 단위: %") {
		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}
	};
	
	public static final SettingObject<Integer> CHANCE = abilitySettings.new SettingObject<Integer>(
			Nun.class, "chance", 5, "# 시너지 등장 확률", "# 단위: %") {
		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}
	};
	
	private static final ImmutableMap<Rank, String> rankcolor = ImmutableMap.<Rank, String>builder()
			.put(Rank.C, "§e")
			.put(Rank.B, "§b")
			.put(Rank.A, "§a")
			.put(Rank.S, "§d")
			.put(Rank.L, "§6")
			.put(Rank.SPECIAL, "§c")
			.build();
	
	private boolean risky = false;
	private final Cooldown cooldown = new Cooldown(COOLDOWN.getValue(), "기도");
	private final int chance = CHANCE.getValue();
	private final int channelingDur = (int) (CHANNELING_COUNT.getValue() * 20);
	private final int duration = (int) (GOD_DURATION.getValue() * 20);
	private final double decreaseDMG = (GOD_DAMAGE_DECREASE.getValue() * 0.01);
	private final double undeadDMG = 1 + (UNDEAD_DAMAGE.getValue() * 0.01);
	private final double rankDMG = (RANK_DAMAGE.getValue() * 0.01);
	private AbilityBase godability;
	
	@SuppressWarnings("unused")
	private final Object EXPLAIN = new Object() {
		@Override
		public String toString() {
			if (godability == null) {
				return "아직 신께서 오지 않으셨습니다.".toString();
			} else {
				final StringJoiner joiner = new StringJoiner("\n");
				joiner.add("§3[§b" + godability.getDisplayName() + "§3] " + godability.getRank().getRankName() + " " + godability.getSpecies().getSpeciesName());
				for (final Iterator<String> iterator = godability.getExplanation(); iterator.hasNext();) {
					joiner.add("§f" + iterator.next());
				}
				return joiner.toString();
			}
		}
	};
	
	@SuppressWarnings("unused")
	private final Object SUM_EXPLAIN = new Object() {
		@Override
		public String toString() {
			if (godability == null) {
				return "아직 신께서 오지 않으셨습니다.".toString();
			} else {
				final StringJoiner joiner = new StringJoiner("\n");
				joiner.add("§3[§b" + godability.getDisplayName() + "§3] " + godability.getRank().getRankName() + " " + godability.getSpecies().getSpeciesName());
				for (final Iterator<String> iterator = godability.getSummarize(); iterator.hasNext();) {
					joiner.add("§f" + iterator.next());
				}
				return joiner.toString();
			}
		}
	};
	
	public AbilityRegistration getRandomGodAbility() {
		
		Set<AbilityRegistration> abilities = new HashSet<>();
		for (AbilityRegistration ability : AbilityFactory.getRegistrations()) {
			String name = ability.getManifest().name();
			if (!Configuration.Settings.isBlacklisted(name) && ability.getManifest().species().equals(Species.GOD)) {
				abilities.add(ability);
				abilities.add(ability);
			}
		}
		
		for (AbilityRegistration ability : AbilityFactory.getRegistrations()) {
			String name = ability.getManifest().name();
			if (!Configuration.Settings.isBlacklisted(name) && ability.getManifest().species().equals(Species.DEMIGOD)) {
				abilities.add(ability);
			}
		}
		
		Random r = new Random();
        return abilities.toArray(new AbilityRegistration[]{})[r.nextInt(abilities.size())];
	}
	
    public AbilityRegistration getRandomGodSynergy() {

		Set<AbilityRegistration> synergies = new HashSet<>();
		
        for (AbilityRegistration synergy : SynergyFactory.getSynergies()) {
        	String name = synergy.getManifest().name();
        	if (!Configuration.Settings.isBlacklisted(name) && synergy.getManifest().species().equals(Species.GOD)) {
        		synergies.add(synergy);
        		synergies.add(synergy);
        	}
        }
        
        for (AbilityRegistration synergy : SynergyFactory.getSynergies()) {
        	String name = synergy.getManifest().name();
        	if (!Configuration.Settings.isBlacklisted(name) && synergy.getManifest().species().equals(Species.DEMIGOD)) {
        		synergies.add(synergy);
        	}
        }
		
        Random r = new Random();
        return synergies.toArray(new AbilityRegistration[]{})[r.nextInt(synergies.size())];
    }

	
	private AbilityTimer channeling = new AbilityTimer(channelingDur) {
		
		@Override
		public void onStart() {
			getPlayer().sendMessage("§7§o기도합시다.");
			double maxHP = getPlayer().getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue();
			if (getPlayer().getHealth() <= maxHP * 0.15) risky = true;
		}
		
		@Override
		public void onEnd() {
			risky = false;
			beingGod.start();
		}
		
	}.setPeriod(TimeUnit.TICKS, 1).register();
	
	private AbilityTimer beingGod = new AbilityTimer(duration) {
		
		@Override
		public void onStart() {
			Random random = new Random();
			AbilityRegistration clazz = null;
			if (random.nextInt(100) < chance) clazz = getRandomGodSynergy();
			else clazz = getRandomGodAbility();
			try {
				getPlayer().getWorld().strikeLightningEffect(getPlayer().getLocation().add(random.nextInt(16) * 0.1, 0, random.nextInt(16) * 0.1));
				getPlayer().getWorld().strikeLightningEffect(getPlayer().getLocation().add(random.nextInt(16) * 0.1, 0, random.nextInt(16) * 0.1));
				getPlayer().getWorld().strikeLightningEffect(getPlayer().getLocation().add(random.nextInt(16) * 0.1, 0, random.nextInt(16) * 0.1));
				getPlayer().getWorld().strikeLightningEffect(getPlayer().getLocation());
				godability = AbilityBase.create(clazz, getParticipant());
				godability.setRestricted(false);
				SoundLib.ENTITY_EVOKER_PREPARE_SUMMON.playSound(getPlayer().getLocation(), 1, 2);
				NMS.sendTitle(getPlayer(), rankcolor.get(godability.getRank()) + "§l" + godability.getDisplayName(), "", 0, 100, 1);
				getPlayer().sendMessage("§3[§b!§3] " + rankcolor.get(godability.getRank()) + godability.getDisplayName() + "§f 신이 당신에게 §c강림§f하였습니다.");
				getPlayer().sendMessage("§c§o모든 것은 신의 뜻대로...");
			} catch (ReflectiveOperationException e) {
				e.printStackTrace();
			}
		}
		
		@Override
		public void run(int count) {
			if (count == duration - 30) NMS.clearTitle(getPlayer());
			getPlayer().setGlowing(true);
		}
		
		@Override
		public void onEnd() {
			onSilentEnd();
		}
		
		@Override
		public void onSilentEnd() {
			cooldown.start();
			NMS.clearTitle(getPlayer());
			getPlayer().setGlowing(false);
			if (Nun.this.godability != null) {
				Nun.this.godability.destroy();
			}
			Nun.this.godability = null;
		}
		
	}.setPeriod(TimeUnit.TICKS, 1).register();
	
	public boolean ActiveSkill(Material material, ClickType clickType) {
		if (godability != null) {
			return godability instanceof ActiveHandler && ((ActiveHandler) godability).ActiveSkill(material, clickType);	
		} else if (material == Material.IRON_INGOT && clickType == ClickType.RIGHT_CLICK && !cooldown.isCooldown() && !channeling.isRunning()) {
			return channeling.start();
		}
		return false;
	}
	
	public void TargetSkill(Material material, LivingEntity entity) {
		if (godability != null) {
			if (godability instanceof TargetHandler) {
			((TargetHandler) godability).TargetSkill(material, entity);
			}
		}
	}
	
	@SubscribeEvent(priority = 1000)
	private void onPlayerSetHealth(PlayerSetHealthEvent e) {
		if (e.getPlayer().equals(getPlayer()) && beingGod.isRunning()) {
			if (e.getHealth() <= 0.0) {
				e.setHealth(1);
				e.setCancelled(true);
			}	
		}
	}
	
	@SubscribeEvent(priority = 1000)
	private void onEntityDamage(EntityDamageEvent e) {
		if (getPlayer().equals(e.getEntity()) && getPlayer().getHealth() - e.getFinalDamage() <= 0) {
			if (beingGod.isRunning()) {
				e.setCancelled(true);
				getPlayer().setHealth(1);
				NMS.broadcastEntityEffect(getPlayer(), (byte) 2);
			}
			if (channeling.isRunning() && risky) {
				e.setCancelled(true);
				getPlayer().setHealth(1);
				PotionEffects.REGENERATION.addPotionEffect(getPlayer(), 60, 1, false);
				NMS.broadcastEntityEffect(getPlayer(), (byte) 2);
				getPlayer().sendMessage("§8[§7HIDDEN§8] §7§o예수께서 이르시되 나는 부활이요 생명이니 나를 믿는 자는 죽어도 살겠고");
				getPlayer().sendMessage("§8[§7HIDDEN§8] §f위험한 상황에서도 기도하였고, 믿음을 보답받았습니다.");
				getPlayer().sendMessage("§8[§7HIDDEN§8] §b믿음의 보답§f을 달성하였습니다.");
				SoundLib.UI_TOAST_CHALLENGE_COMPLETE.playSound(getPlayer());
			}
		}
	}
	
	@SubscribeEvent
	public void onPlayerMove(PlayerMoveEvent e) {
		if (channeling.isRunning() && e.getPlayer().equals(getPlayer())) e.setTo(e.getFrom());
	}
	
	@SubscribeEvent
	public void onEntityDamageByBlock(EntityDamageByBlockEvent e) {
		onEntityDamage(e);
	}
	
	@SubscribeEvent
	public void onEntityDamageByEntity(EntityDamageByEntityEvent e) {
		onEntityDamage(e);
		Player damager = null;
		if (e.getDamager() instanceof Projectile) {
			Projectile projectile = (Projectile) e.getDamager();
			if (projectile.getShooter() instanceof Player) damager = (Player) projectile.getShooter();
		} else if (e.getDamager() instanceof Player) damager = (Player) e.getDamager();
		
		if (e.getEntity() instanceof Player) {
			Player player = (Player) e.getEntity();
			if (!getPlayer().equals(player)) {
				boolean isUndead = false;
				Participant participant = getGame().getParticipant(player);
				if (participant.getAbility().getClass().equals(Mix.class)) {
					final Mix mix = (Mix) getParticipant().getAbility();
					final AbilityBase first = mix.getFirst(), second = mix.getSecond();
					if (first.getSpecies() == Species.UNDEAD || second.getSpecies() == Species.UNDEAD) isUndead = true;
				} else {
					if (participant.getAbility().getSpecies() == Species.UNDEAD) isUndead = true;
				}
				
				if (getPlayer().equals(damager) && isUndead) {
					e.setDamage(e.getDamage() * undeadDMG);
				}
				
				if (beingGod.isRunning()) {
					e.setDamage(e.getDamage() + (e.getDamage() * ((godability.getRank().ordinal() + 1) * rankDMG)));
				}	
			} else {
				boolean isGod = false;
				boolean isDemiGod = false;
				Participant participant = getGame().getParticipant(player);
				if (participant.getAbility().getClass().equals(Mix.class)) {
					final Mix mix = (Mix) getParticipant().getAbility();
					final AbilityBase first = mix.getFirst(), second = mix.getSecond();
					if (first.getSpecies() == Species.GOD || second.getSpecies() == Species.GOD) isGod = true;
					else if (first.getSpecies() == Species.DEMIGOD || second.getSpecies() == Species.DEMIGOD) isDemiGod = true;
				} else {
					if (participant.getAbility().getSpecies() == Species.GOD) isGod = true;
					else if (participant.getAbility().getSpecies() == Species.DEMIGOD) isDemiGod = true;
				}
				
				if (!getPlayer().equals(damager)) {
					if (isGod) e.setDamage(e.getDamage() - (e.getDamage() * decreaseDMG));
					else if (isDemiGod) e.setDamage(e.getDamage() - (e.getDamage() * decreaseDMG * 0.5));
				}
			}
		}
	}

}
