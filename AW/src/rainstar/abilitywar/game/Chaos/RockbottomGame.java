package rainstar.abilitywar.game.Chaos;

import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.naming.OperationNotSupportedException;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

import com.google.common.base.Strings;

import daybreak.abilitywar.AbilityWar;
import daybreak.abilitywar.config.Configuration.Settings;
import daybreak.abilitywar.config.Configuration.Settings.DeathSettings;
import daybreak.abilitywar.game.Game;
import daybreak.abilitywar.game.GameAliases;
import daybreak.abilitywar.game.GameManifest;
import daybreak.abilitywar.game.AbstractGame.Observer;
import daybreak.abilitywar.game.event.GameCreditEvent;
import daybreak.abilitywar.game.manager.AbilityList;
import daybreak.abilitywar.game.manager.object.DefaultKitHandler;
import daybreak.abilitywar.game.module.DeathManager;
import daybreak.abilitywar.game.module.InfiniteDurability;
import daybreak.abilitywar.game.script.manager.ScriptManager;
import daybreak.abilitywar.utils.base.Messager;
import daybreak.abilitywar.utils.base.Seasons;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.base.minecraft.PlayerCollector;
import daybreak.abilitywar.utils.base.minecraft.nms.IHologram;
import daybreak.abilitywar.utils.base.minecraft.nms.NMS;
import daybreak.abilitywar.utils.base.random.Random;
import daybreak.abilitywar.utils.library.SoundLib;

@GameManifest(name = "카오스 - 최저점", description = {
		"§5§kaaa§2최저점§5§kaaa",
		"",
		"§f자신의 공격력이 갱신한 최대치 이하로 떨어지지 않습니다.",
		"§f기본적으로 공격력이 10%~50% 사이로 무작위 감소합니다."
		})
@GameAliases({"최저점", "락바텀"})
public class RockbottomGame extends Game implements DefaultKitHandler, Observer {
	
	public RockbottomGame() {
		super(PlayerCollector.EVERY_PLAYER_EXCLUDING_SPECTATORS());
		setRestricted(Settings.InvincibilitySettings.isEnabled());
		attachObserver(this);
		Bukkit.getPluginManager().registerEvents(this, AbilityWar.getPlugin());
	}

	private final Map<Participant, Double> damageMap = new HashMap<>();
	private final Random random = new Random();
	
	@Override
	protected void progressGame(int seconds) {
		switch (seconds) {
			case 1:
				List<String> lines = Messager.asList("§8==== §a게임 참여자 목록 §8====");
				int count = 0;
				for (Participant p : getParticipants()) {
					count++;
					lines.add("§b" + count + ". §f" + p.getPlayer().getName());
				}
				lines.add("§2총 인원수 : " + count + "명");
				lines.add("§8===========================");

				for (String line : lines) {
					Bukkit.broadcastMessage(line);
				}

				if (getParticipants().size() < 2) {
					stop();
					Bukkit.broadcastMessage("§c최소 참가자 수를 충족하지 못하여 게임을 중지합니다. §8(§72명§8)");
				}
				break;
			case 3:
				lines = Messager.asList(
						"§5ChaosWar §f- §d혼돈의 전쟁",
						"§e버전 §7: §f4.0.1",
						"§b개발자 §7: §aRainStar_ 레인스타",
						"§9디스코드 §7: §a레인스타§7#0846"
				);

				GameCreditEvent event = new GameCreditEvent(this);
				Bukkit.getPluginManager().callEvent(event);
				lines.addAll(event.getCredits());

				for (String line : lines) {
					Bukkit.broadcastMessage(line);
				}
				break;
			case 5:
				if (Settings.getDrawAbility()) {
					for (String line : Messager.asList(
							"§f플러그인에 총 §b" + AbilityList.nameValues().size() + "개§f의 능력이 등록되어 있습니다.",
							"§7능력을 무작위로 할당합니다...")) {
						Bukkit.broadcastMessage(line);
					}
					try {
						startAbilitySelect();
					} catch (OperationNotSupportedException ignored) {
					}
				}
				break;
			case 6:
				if (Settings.getDrawAbility()) {
					Bukkit.broadcastMessage("§f모든 참가자가 능력을 §b확정§f했습니다.");
				} else {
					Bukkit.broadcastMessage("§f능력자 게임 설정에 따라 §b능력§f을 추첨하지 않습니다.");
				}
				break;
			case 8:
				Bukkit.broadcastMessage("§a게임의 특정 난수를 조작하였습니다.");
				Bukkit.broadcastMessage("§2잠시 후 게임이 시작됩니다.");
				break;
			case 10:
				Bukkit.broadcastMessage("§2게임이 §c5§2초 후에 시작됩니다.");
				SoundLib.BLOCK_NOTE_BLOCK_HARP.broadcastSound();
				break;
			case 11:
				Bukkit.broadcastMessage("§2게임이 §c4§2초 후에 시작됩니다.");
				SoundLib.BLOCK_NOTE_BLOCK_HARP.broadcastSound();
				break;
			case 12:
				Bukkit.broadcastMessage("§2게임이 §c3§2초 후에 시작됩니다.");
				SoundLib.BLOCK_NOTE_BLOCK_HARP.broadcastSound();
				break;
			case 13:
				Bukkit.broadcastMessage("§2게임이 §c2§2초 후에 시작됩니다.");
				SoundLib.BLOCK_NOTE_BLOCK_HARP.broadcastSound();
				break;
			case 14:
				Bukkit.broadcastMessage("§2게임이 §c1§2초 후에 시작됩니다.");
				SoundLib.BLOCK_NOTE_BLOCK_HARP.broadcastSound();
				break;
			case 15:
				if (Seasons.isChristmas()) {
					final String blocks = Strings.repeat("§c■§2■", 22);
					Bukkit.broadcastMessage(blocks);
					Bukkit.broadcastMessage("§f            §2Rock Bottom §f- §3최저점  ");
					Bukkit.broadcastMessage("§f                   게임 시작                ");
					Bukkit.broadcastMessage(blocks);
				} else {
					for (String line : Messager.asList(
							"§8■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■",
							"§f             §2Rock Bottom §f- §3최저점  ",
							"§f                    게임 시작                ",
							"§8■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■")) {
						Bukkit.broadcastMessage(line);
					}
				}

				giveDefaultKit(getParticipants());

				if (Settings.getSpawnEnable()) {
					Location spawn = Settings.getSpawnLocation().toBukkitLocation();
					for (Participant participant : getParticipants()) {
						participant.getPlayer().teleport(spawn);
					}
				}

				if (Settings.getNoHunger()) {
					Bukkit.broadcastMessage("§2배고픔 무제한§a이 적용됩니다.");
				} else {
					Bukkit.broadcastMessage("§4배고픔 무제한§c이 적용되지 않습니다.");
				}

				if (Settings.getInfiniteDurability()) {
					addModule(new InfiniteDurability());
				} else {
					Bukkit.broadcastMessage("§4내구도 무제한§c이 적용되지 않습니다.");
				}

				if (Settings.getClearWeather()) {
					for (World w : Bukkit.getWorlds()) {
						w.setStorm(false);
					}
				}
				
				if (isRestricted()) {
					getInvincibility().start(false);
				} else {
					Bukkit.broadcastMessage("§4초반 무적§c이 적용되지 않습니다.");
					setRestricted(false);
				}

				ScriptManager.runAll(this);
				
				startGame();
				break;
		}
	}
	
	@EventHandler(priority = EventPriority.HIGHEST)
	public void onEntityDamageByEntity(EntityDamageByEntityEvent e) {
		if (!e.isCancelled()) {
			Player damager = null;
			if (e.getDamager() instanceof Projectile) {
				Projectile projectile = (Projectile) e.getDamager();
				if (projectile.getShooter() instanceof Player) damager = (Player) projectile.getShooter();
			} else if (e.getDamager() instanceof Player) damager = (Player) e.getDamager();
			
			if (damager != null && getParticipants().contains(getParticipant(damager)) && !e.getEntity().equals(damager)) {
				e.setDamage(e.getDamage() * (1 - ((random.nextInt(41) + 10) * 0.01)));
				if (damageMap.containsKey(getParticipant(damager))) {
					if (e.getDamage() > damageMap.get(getParticipant(damager))) {
						damageMap.put(getParticipant(damager), e.getDamage());
						new Holograms(getParticipant(damager), e.getEntity().getLocation(), e.getDamage(), e.getDamage(), true, false, true).start();
					} else {
						new Holograms(getParticipant(damager), e.getEntity().getLocation(), e.getDamage(), damageMap.get(getParticipant(damager)), false, true, true).start();
						e.setDamage(damageMap.get(getParticipant(damager)));
					}
				} else {
					damageMap.put(getParticipant(damager), e.getDamage());
					new Holograms(getParticipant(damager), e.getEntity().getLocation(), e.getDamage(), e.getDamage(), false, false, true).start();
				}
			}
		}
	}

	@Override
	protected @Nonnull DeathManager newDeathManager() {
		return new DeathManager(this) {
			public void Operation(Participant victim) {
				switch (DeathSettings.getOperation()) {
					case 탈락:
						eliminate(victim);
						excludedPlayers.add(victim.getPlayer().getUniqueId());
						break;
					case 관전모드:
					case 없음:
						victim.getPlayer().setGameMode(GameMode.SPECTATOR);
						excludedPlayers.add(victim.getPlayer().getUniqueId());
						break;
				}
			}
		};
	}

	@Override
	public void update(GameUpdate update) {
		if (update == GameUpdate.END) {
			HandlerList.unregisterAll(this);
		}
	}
	
	private class Holograms extends GameTimer {
		
		private final IHologram hologram;
		private Random random = new Random();
		private final DecimalFormat damageDF = new DecimalFormat("0.00");
		private boolean control;
		private double damages;
		private double damage;
		private double controldamage;
		
		private Holograms(Participant participant, Location hitLoc, double damage, double controldamage, boolean over, boolean control, boolean showWho) {
			super(TaskType.REVERSE, 30);
			setPeriod(TimeUnit.TICKS, 1);
			this.hologram = NMS.newHologram(hitLoc.getWorld(), 
					hitLoc.getX() + (((random.nextDouble() * 2) - 1) * 0.5),
					hitLoc.getY() + 1.25 + (((random.nextDouble() * 2) - 1) * 0.25), 
					hitLoc.getZ() + (((random.nextDouble() * 2) - 1) * 0.5), 
					over ? "§b§l" + damageDF.format(damage) : "§c§l" + damageDF.format(damage));
			this.damage = damage;
			this.control = control;
			this.controldamage = controldamage;
			if (showWho) {
				hologram.display(participant.getPlayer());
			} else {
				for (Player player : participant.getPlayer().getWorld().getPlayers()) {
					hologram.display(player);
				}	
			}
		}
		
		@Override
		protected void run(int count) {
			if (control) {
				if (count > 20) {
					if (damage > controldamage) {
						damages = damage - (((damage - controldamage) * (31 - count)) / 10);	
					} else if (damage < controldamage) {
						damages = damage + (((controldamage - damage) * (31 - count)) / 10);
					} else if (damage == controldamage) {
						damages = damage;
					}
					hologram.setText("§a§l" + damageDF.format(damages));
				}	
			}
			hologram.teleport(hologram.getLocation().add(0, 0.03, 0));
		}
		
		@Override
		protected void onEnd() {
			onSilentEnd();
		}
		
		@Override
		protected void onSilentEnd() {
			hologram.unregister();
		}
		
	}

}
