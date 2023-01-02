package rainstar.abilitywar.ability;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Note;
import org.bukkit.Note.Tone;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByBlockEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerVelocityEvent;
import org.bukkit.util.Vector;

import daybreak.abilitywar.ability.AbilityBase;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.ability.SubscribeEvent;
import daybreak.abilitywar.ability.decorator.ActiveHandler;
import daybreak.abilitywar.config.ability.AbilitySettings.SettingObject;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.utils.base.Formatter;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.base.concurrent.SimpleTimer.TaskType;
import daybreak.abilitywar.utils.library.ParticleLib;
import daybreak.abilitywar.utils.library.SoundLib;

@AbilityManifest(name = "철괴", rank = Rank.A, species = Species.OTHERS, explain = {
		"뒤쪽으로 밀려나지 않습니다.",
		"§3철괴§f 우클릭 시, $[DAMAGE_DECREASE_DURATION]초간 §b20%§f의 §b피해 경감 효과§f를 얻습니다.",
		"§c피격 시§f마다 지속시간이 갱신되고 §b경감 수치§f가 §b20%§f씩 증가합니다.",
		"§b경감 수치§f가 §b100%§f에 도달하면, $[INV_DURATION]초간 §a무적§f이 됩니다. $[COOLDOWN]"
		},
		summarize = {
		"뒤쪽으로 밀려나지 않습니다.",
		"§3철괴§f 우클릭 시, $[DAMAGE_DECREASE_DURATION]초간 §b20%§f의 §b피해 경감 효과§f를 얻습니다.",
		"§c피격 시§f마다 지속시간이 갱신되고 §b경감 수치§f가 §b20%§f씩 증가합니다.",
		"§b경감 수치§f가 §b100%§f에 도달하면, $[INV_DURATION]초간 §a무적§f이 됩니다. $[COOLDOWN]"
		})

public class Tekkai extends AbilityBase implements ActiveHandler {
	
	public Tekkai(Participant participant) {
		super(participant);
	}
	
	public static final SettingObject<Double> DAMAGE_DECREASE_DURATION = 
			abilitySettings.new SettingObject<Double>(Tekkai.class, "damage-decrease-duration", 2.0,
            "# 피해 경감 효과 지속시간") {
        @Override
        public boolean condition(Double value) {
            return value >= 0.0;
        }
    };
    
	public static final SettingObject<Double> INV_DURATION = 
			abilitySettings.new SettingObject<Double>(Tekkai.class, "inv-duration", 3.0,
            "# 무적 효과 지속시간") {
        @Override
        public boolean condition(Double value) {
            return value >= 0.0;
        }
    };
    
	public static final SettingObject<Integer> COOLDOWN = 
			abilitySettings.new SettingObject<Integer>(Tekkai.class, "cooldown", 75,
            "# 철괴 우클릭 쿨타임", "# 단위: 초") {
        @Override
        public boolean condition(Integer value) {
            return value >= 0;
        }
        @Override
        public String toString() {
            return Formatter.formatCooldown(getValue());
        }
    };
    
	private Cooldown cooldown = new Cooldown(COOLDOWN.getValue());
	private int decreaseduration = (int) (DAMAGE_DECREASE_DURATION.getValue() * 20);
	private int invduration = (int) (INV_DURATION.getValue() * 20);
	private int stack;
	private BossBar bossBar = null;
	private boolean block = false;
	
	private boolean isFront(Entity owner, Entity target) {
	    Vector eye = owner.getLocation().getDirection().setY(0).normalize();
	    Vector toEntity = target.getLocation().getDirection().setY(0).normalize();
	    double dot = toEntity.dot(eye);
	    return dot <= -0.75D;
	}
	
	public boolean ActiveSkill(Material material, ClickType clicktype) {
		if (material == Material.IRON_INGOT && clicktype.equals(ClickType.RIGHT_CLICK) && !blocking.isRunning() && !cooldown.isCooldown()) {
			return blocking.start();
		}
		return false;
	}

	private AbilityTimer blocking = new AbilityTimer(TaskType.REVERSE, decreaseduration) {
		
		@Override
		public void onStart() {
			SoundLib.BLOCK_ANVIL_PLACE.playSound(getPlayer().getLocation(), 1, 0.75f);
			bossBar = Bukkit.createBossBar("§b피해 경감", BarColor.BLUE, BarStyle.SEGMENTED_6);
    		bossBar.setProgress(stack / (double) 6);
    		bossBar.addPlayer(getPlayer());
    		bossBar.setVisible(true);
		}
		
		@Override
		public void run(int count) {
			if (inv.isRunning()) {
				bossBar.setColor(BarColor.WHITE);
				bossBar.setTitle("§3무적");
				bossBar.setProgress(inv.getCount() / (double) invduration);
			} else bossBar.setProgress(stack / (double) 6);
		}
		
		@Override
		public void onEnd() {
			onSilentEnd();
		}

		@Override
		public void onSilentEnd() {
			cooldown.start();
			bossBar.removeAll();
			stack = 0;
		}
		
	}.setPeriod(TimeUnit.TICKS, 1);
	
	private AbilityTimer inv = new AbilityTimer(TaskType.REVERSE, invduration) {
		
	}.setPeriod(TimeUnit.TICKS, 1);
	
	@SubscribeEvent
	public void onEntityDamage(EntityDamageEvent e) {
		if (e.getEntity().equals(getPlayer())) {
			if (inv.isRunning()) {
				e.setCancelled(true);
				ParticleLib.CRIT_MAGIC.spawnParticle(getPlayer().getLocation(), 0.2, 1.5, 0.2, 30, 1);
				SoundLib.BLOCK_ANVIL_LAND.playSound(getPlayer().getLocation(), 1, 0.5f);
				SoundLib.ITEM_SHIELD_BLOCK.playSound(getPlayer().getLocation(), 1, 0.8f);
			}
			if (blocking.isRunning() && !e.isCancelled()) {
				blocking.setCount(decreaseduration);
				if (stack < 6) stack += 1;
				e.setDamage(e.getDamage() * (1 - (stack * 0.2)));
				if (stack >= 1) SoundLib.BASS_GUITAR.playInstrument(getPlayer().getLocation(), Note.natural(0, Tone.C));
				if (stack >= 2) SoundLib.BASS_GUITAR.playInstrument(getPlayer().getLocation(), Note.natural(1, Tone.G));
				if (stack >= 3) SoundLib.BASS_GUITAR.playInstrument(getPlayer().getLocation(), Note.sharp(1, Tone.G));
				if (stack >= 4) SoundLib.BASS_GUITAR.playInstrument(getPlayer().getLocation(), Note.natural(1, Tone.A));
				if (stack >= 5) SoundLib.BASS_GUITAR.playInstrument(getPlayer().getLocation(), Note.natural(0, Tone.D));
				if (stack == 6) {
					inv.start();
					blocking.setCount(invduration);
				}
			}
		}
	}
	
	@SubscribeEvent
	public void onEntityDamageByBlock(EntityDamageByBlockEvent e) {
		onEntityDamage(e);
	}
	
	@SubscribeEvent
	public void onEntityDamageByEntity(EntityDamageByEntityEvent e) {
		onEntityDamage(e);
		
		if (e.getEntity().equals(getPlayer()) && e.getDamager() instanceof Player && isFront(getPlayer(), e.getDamager())) {
			block = true;
		}
	}
	
	@SubscribeEvent(onlyRelevant = true)
	private void onVelocity(PlayerVelocityEvent e) {
		Vector applyVec = e.getVelocity().clone().setY(0).normalize();
		Vector backVec = getPlayer().getLocation().getDirection().clone().setY(0).normalize().multiply(-1);
		if (applyVec.dot(backVec) >= 0.5D || block) {
			block = false;
			SoundLib.BLOCK_STONE_PLACE.playSound(getPlayer().getLocation(), 1, 0.5f);
			e.setCancelled(true);
		}
	}
	
}