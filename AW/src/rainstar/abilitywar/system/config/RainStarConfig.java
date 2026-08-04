package rainstar.abilitywar.system.config;

import java.io.File;
import java.io.IOException;

import org.bukkit.configuration.file.YamlConfiguration;

public class RainStarConfig {
	
	private static final File FILE = new File("plugins/AbilityWar/rainstarConfig.yml");
	private static final YamlConfiguration config = new YamlConfiguration();
	
	static { load(); }
	
	private RainStarConfig() {}
	
	public enum ConfigType {
	    BOOLEAN,
	    INTEGER,
	    STRING;
	}
	
	public enum ConfigOption {

	    NO_DELAY(ConfigType.BOOLEAN, "no-delay", false, new String[] {
	            "§f공격 속도의 제한을 무시하고 공격할 수 있게 됩니다."
	        }),
	    
	    DOUBLE_MURDER(ConfigType.BOOLEAN, "double-murder", false, new String[] {
	    		"§c머더§f가 2배로 등장합니다. §3탐정§f은 그대로 등장합니다.",
	    		"§f그 대신 모든 §a시민팀§f은 §e금§f을 항상 2배로 얻습니다.",
	    		"§f이 옵션과 §a인원 수 밸런스§f를 동시 활성화하면 이 옵션만 적용됩니다."
	    	}),
	    GOLD_RUSH(ConfigType.BOOLEAN, "gold-rush", false, new String[] {
	    		"§e금§f이 생성되는 속도가 기존의 2배로 증가합니다.",
	    		"§b머더 타이머§f를 사용하지 않으면 적용되지 않습니다."
	    	}),
	    BLACK_COMPASS(ConfigType.BOOLEAN, "black-compass", true, new String[] {
	    		"§c머더: 블랙§f이 이제 능력을 사용할 때 나침반을 고장냅니다.",
	    		"§f또한 §a시민팀§f이 §c머더팀§f의 이름표를 보지 못하게 합니다."
	    	}),
	    HEAD_COUNT_BALANCE(ConfigType.BOOLEAN, "head-count-balance", true, new String[] {
	    		"§c머더 팀§7:§a시민 팀§f의 비율이 §c1§7:§a7~8§f이라면,",
	    		"§c머더 팀§f은 실드 1개를 갖고 시작하며 금을 25%로 1개 더 얻습니다.",
	    		"§c머더 팀§7:§a시민 팀§f의 비율이 §c1§7:§a1~3§f이라면,",
	    		"§a시민 팀§f은 화살을 1개 갖고 시작합니다.",
	    		"§f이 옵션과 §c더블 머더§f를 동시 활성화하면 §c더블 머더§f만 적용됩니다."
	    	}),

	    DETECTIVE_HOMING(ConfigType.BOOLEAN, "detective-homing", true, new String[] {
	    		"§3탐정§f은 일정 거리§8(§7√3§8)§f의 화살 유도력을 가집니다.",
	    		"§f이 옵션을 비활성화하면 §3탐정 <유도력+>§f이 무효과가 됩니다."
	    	}),
	    
	    NO_KILL(ConfigType.BOOLEAN, "no-kill", true, new String[] {
	    		"§f게임이 시작하고 지정 시간이 지나기 전까지 아무도 살인할 수 없습니다.",
	    		"§f설정 단위는 초단위입니다."
	    	}),
	    NO_KILL_TIME(ConfigType.INTEGER, "no-kill-time", 60, new String[] {
	    		"§f게임이 시작하고 지정 시간이 지나기 전까지 아무도 살인할 수 없습니다.",
	    		"§f설정 단위는 초단위입니다."
	    	}),

	    MURDER_TIMER(ConfigType.BOOLEAN, "murder-timer", true, new String[] {
	    		"§f게임이 시작하고 지정 시간이 지나면 §a시민팀§f이 승리합니다.",
	    		"§f설정 단위는 초단위입니다."
	    	}),
	    MURDER_TIMER_TIME(ConfigType.INTEGER, "murder-timer-time", 600, new String[] {
	    		"§f게임이 시작하고 지정 시간이 지나면 §a시민팀§f이 승리합니다.",
	    		"§f설정 단위는 초단위입니다."
	    	});

	    private final ConfigType type;
	    private final String path;
	    private final Object defaultValue;
	    private final String[] description;

	    ConfigOption(ConfigType type, String path, Object defaultValue, String[] description) {
	        this.type = type;
	        this.path = path;
	        this.defaultValue = defaultValue;
	        this.description = description;
	    }

	    public ConfigType getType() {
	        return type;
	    }

	    public String getPath() {
	        return path;
	    }

	    @SuppressWarnings("unchecked")
	    public <T> T getDefaultValue() {
	        return (T) defaultValue;
	    }
	    
	    public String[] getDescription() {
	        return description;
	    }
	    
	}
	
	public static Object get(ConfigOption option) {
	    switch (option.getType()) {
	        case BOOLEAN:
	            return config.getBoolean(option.getPath());
	        case INTEGER:
	            return config.getInt(option.getPath());
	        case STRING:
	            return config.getString(option.getPath());
	        default:
	            throw new IllegalArgumentException("Unknown config type: " + option.getType());
	    }
	}

	public static void set(ConfigOption option, Object value) {
	    config.set(option.getPath(), value);
	    save();
	}

	public static boolean getBoolean(ConfigOption option) {
	    if (option.getType() != ConfigType.BOOLEAN) {
	        throw new IllegalArgumentException(option.name() + " is not a BOOLEAN config.");
	    }
	    return (boolean) get(option);
	}

	public static int getInt(ConfigOption option) {
	    if (option.getType() != ConfigType.INTEGER) {
	        throw new IllegalArgumentException(option.name() + " is not an INTEGER config.");
	    }
	    return (int) get(option);
	}

	public static String getString(ConfigOption option) {
	    if (option.getType() != ConfigType.STRING) {
	        throw new IllegalArgumentException(option.name() + " is not a STRING config.");
	    }
	    return (String) get(option);
	}
	
	public static void load() {
	    try {
	        if (!FILE.exists()) {
	            FILE.getParentFile().mkdirs();
	            FILE.createNewFile();
	        }

	        config.load(FILE);

	        boolean changed = false;

	        for (ConfigOption option : ConfigOption.values()) {
	            if (!config.contains(option.getPath())) {
	                config.set(option.getPath(), option.getDefaultValue());
	                changed = true;
	            }
	        }

	        if (changed) {
	            config.save(FILE);
	        }
	    } catch (Exception e) {
	        e.printStackTrace();
	    }
	}
	
	public static void save() {
		try {
			config.save(FILE);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
}
