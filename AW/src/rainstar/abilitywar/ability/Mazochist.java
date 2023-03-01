package rainstar.abilitywar.ability;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent.RegainReason;
import org.bukkit.scheduler.BukkitRunnable;

import daybreak.abilitywar.AbilityWar;
import daybreak.abilitywar.ability.AbilityBase;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.SubscribeEvent;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.ability.decorator.ActiveHandler;
import daybreak.abilitywar.config.ability.AbilitySettings.SettingObject;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.AbstractGame.Participant.ActionbarNotification.ActionbarChannel;
import daybreak.abilitywar.utils.base.Formatter;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.base.minecraft.entity.health.Healths;

@AbilityManifest(name = "피학증", rank = Rank.A, species = Species.HUMAN, explain = {
		"§c피해를 입을 때§f마다 스택을 획득해 §d회복 속도§f가 증가합니다.",
		"스택은 체력이 가득 차면 초기화됩니다.",
		"철괴 우클릭 시, $[DURATION]초간 피해를 $[PERCENTAGE]%로 줄입니다. $[COOLDOWN]",
		"그 대신, 피해량만큼 한 번 더 자해합니다.",
		"§b[§7아이디어 제공자§b] §0personalmoder"
		},
		summarize = {
		"피해를 입을 때마다 최대 체력이 될 때까지 회복 속도가 증가합니다.",
		"§7철괴 우클릭 시§f $[DURATION]초간 감소된 피해를 2회 맞습니다."
		})

public class Mazochist extends AbilityBase implements ActiveHandler {
	
	public Mazochist(Participant participant) {
		super(participant);
	}
	
	public static final SettingObject<Integer> DURATION = 
			abilitySettings.new SettingObject<Integer>(Mazochist.class, "duration", 10,
            "# 스킬 지속시간", "# 단위: 초") {
        @Override
        public boolean condition(Integer value) {
            return value >= 0;
        }
    };
	
	public static final SettingObject<Integer> HEAL = 
			abilitySettings.new SettingObject<Integer>(Mazochist.class, "heal-amount", 40,
            "# 스택당 매 틱 회복량", "# 단위: 값 / 10000", "# 10 = 0.001", "# 초당 0.02 회복") {
        @Override
        public boolean condition(Integer value) {
            return value >= 0;
        }
    };
    
	public static final SettingObject<Integer> PERCENTAGE = 
			abilitySettings.new SettingObject<Integer>(Mazochist.class, "more-damage-percentage", 60,
            "# 스킬 발동 시 추가 피해량", "# 단위: %") {
        @Override
        public boolean condition(Integer value) {
            return value >= 0;
        }
    };
	
	public static final SettingObject<Integer> COOLDOWN = 
			abilitySettings.new SettingObject<Integer>(Mazochist.class, "cooldown", 60,
            "# 쿨타임", "# 단위: 초") {
        @Override
        public boolean condition(Integer value) {
            return value >= 0;
        }
        @Override
        public String toString() {
            return Formatter.formatCooldown(getValue());
        }
    };
    
    private List<Double> damages = new ArrayList<>();
    private final Cooldown cooldown = new Cooldown(COOLDOWN.getValue());
    private final double percentage = PERCENTAGE.getValue() * 0.01;
    private final int duration = DURATION.getValue() * 20;
    private final double heal = HEAL.getValue() / 10000.0;
	private ActionbarChannel ac = newActionbarChannel();
	private int stack = 0;
	
	@Override
	public void onUpdate(Update update) {
		if (update == Update.RESTRICTION_CLEAR) {
			moreheal.start();
		}
	}
	
	@SubscribeEvent(priority = 1000)
	public void onEntityDamageByEntity(EntityDamageByEntityEvent e) {
		if (e.getEntity().equals(getPlayer()) && !e.isCancelled()) {
			if (damages.contains(e.getDamage())) {
				damages.remove(e.getDamage());
			} else if (skill.isRunning() && !e.getDamager().equals(getPlayer())) {
				e.setDamage(e.getDamage() * percentage);
				damages.add(e.getDamage());
				new BukkitRunnable() {
					@Override
					public void run() {
						getPlayer().setNoDamageTicks(0);
						getPlayer().damage(e.getDamage(), getPlayer());
					}
				}.runTaskLater(AbilityWar.getPlugin(), 10L);
			}
			stack += 1;
		}
	}
	
	private AbilityTimer moreheal = new AbilityTimer() {
		
		@Override
		public void run(int count) {
			final EntityRegainHealthEvent event = new EntityRegainHealthEvent(getPlayer(), stack * heal, RegainReason.CUSTOM);
			Bukkit.getPluginManager().callEvent(event);
			if (!event.isCancelled()) {
				Healths.setHealth(getPlayer(), getPlayer().getHealth() + (stack * heal));
			}
			
			if (getPlayer().getHealth() == getPlayer().getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue()) stack = 0;
			ac.update("§c피격§f: §b" + stack + "§f회");
		}
	
	}.setPeriod(TimeUnit.TICKS, 1).register();
	
	private Duration skill = new Duration(duration) {
		
		@Override
		public void onDurationProcess(int count) {
			
		}
		
		@Override
		public void onDurationEnd() {
			onDurationSilentEnd();
		}
		
		@Override
		public void onDurationSilentEnd() {
			cooldown.start();
		}
	
	}.setPeriod(TimeUnit.TICKS, 1);
	
	public boolean ActiveSkill(Material material, AbilityBase.ClickType clicktype) {
		if (material.equals(Material.IRON_INGOT) && clicktype.equals(ClickType.RIGHT_CLICK) && !cooldown.isCooldown() && !skill.isRunning()) {
			return skill.start();
		}
		return false;
	}

}
