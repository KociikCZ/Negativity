package com.elikill58.negativity.universal.adapter;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.elikill58.negativity.bungee.BungeeTranslationProvider;
import com.elikill58.negativity.universal.Cheat;
import com.elikill58.negativity.universal.DefaultConfigValue;
import com.elikill58.negativity.universal.NegativityAccount;
import com.elikill58.negativity.universal.NegativityPlayer;
import com.elikill58.negativity.universal.ReportType;
import com.elikill58.negativity.universal.TranslatedMessages;
import com.elikill58.negativity.universal.translation.CachingTranslationProvider;
import com.elikill58.negativity.universal.translation.TranslationProvider;
import com.elikill58.negativity.universal.translation.TranslationProviderFactory;
import com.elikill58.negativity.universal.utils.UniversalUtils;
import com.elikill58.negativity.velocity.VelocityNegativity;
import com.elikill58.negativity.velocity.VelocityNegativityPlayer;
import com.google.common.io.ByteStreams;
import com.google.gson.Gson;
import com.velocitypowered.api.proxy.Player;

import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;

public class VelocityAdapter extends Adapter implements TranslationProviderFactory {

	private Configuration config;
	private VelocityNegativity pl;

	public VelocityAdapter(VelocityNegativity pl, Configuration config) {
		this.pl = pl;
		this.config = config;
	}

	@Override
	public String getName() {
		return "velocity";
	}

	@Override
	public Object getConfig() {
		return config;
	}

	@Override
	public File getDataFolder() {
		return pl.getDataFolder();
	}

	@Override
	public String getStringInConfig(String dir) {
		if (config.contains(dir)) {
			return config.getString(dir);
		}
		return DefaultConfigValue.getDefaultValueString(dir);
	}

	@Override
	public boolean getBooleanInConfig(String dir) {
		if (config.contains(dir)) {
			return config.getBoolean(dir);
		}
		return DefaultConfigValue.getDefaultValueBoolean(dir);
	}

	@Override
	public void log(String msg) {
		pl.getLogger().info(msg);
	}

	@Override
	public void warn(String msg) {
		pl.getLogger().warn(msg);
	}

	@Override
	public void error(String msg) {
		pl.getLogger().error(msg);
	}

	@Override
	public HashMap<String, String> getKeysListInConfig(String dir) {
		HashMap<String, String> list = new HashMap<>();
		for (String s : config.getSection(dir).getKeys())
			list.put(s, config.getString(dir + "." + s));
		return list;
	}

	@Override
	public int getIntegerInConfig(String dir) {
		if (config.contains(dir)) {
			return config.getInt(dir);
		}
		return DefaultConfigValue.getDefaultValueInt(dir);
	}

	@Override
	public void set(String dir, Object value) {
		config.set(dir, value);
	}

	@Override
	public double getDoubleInConfig(String dir) {
		if (config.contains(dir)) {
			return config.getDouble(dir);
		}
		return DefaultConfigValue.getDefaultValueDouble(dir);
	}

	@Override
	public List<String> getStringListInConfig(String dir) {
		return config.getStringList(dir);
	}

	@Override
	public String getStringInOtherConfig(Path relativeFile, String key, String defaultValue) {
		Path configFile = getDataFolder().toPath().resolve(relativeFile);
		if (Files.notExists(configFile))
			return defaultValue;
		try {
			return ConfigurationProvider.getProvider(YamlConfiguration.class).load(configFile.toFile()).getString(key, defaultValue);
		} catch (IOException e) {
			e.printStackTrace();
			return defaultValue;
		}
	}

	@Override
	public File copy(String lang, File f) {
		if (f.exists())
			return f;

		File parentDir = f.getParentFile();
		if (!parentDir.exists())
			parentDir.mkdirs();

		String fileName = "bungee_en_US.yml";
		if (lang.toLowerCase().contains("fr") || lang.toLowerCase().contains("be"))
			fileName = "bungee_fr_FR.yml";
		if (lang.toLowerCase().contains("pt") || lang.toLowerCase().contains("br"))
			fileName = "bungee_pt_BR.yml";
		if (lang.toLowerCase().contains("no"))
			fileName = "bungee_no_NO.yml";
		else if (lang.toLowerCase().contains("ru"))
			fileName = "bungee_ru_RU.yml";
		else if (lang.toLowerCase().contains("zh") || lang.toLowerCase().contains("cn"))
			fileName = "bungee_zh_CN.yml";
		else if (lang.toLowerCase().contains("de"))
			fileName = "bungee_de_DE.yml";
		else if (lang.toLowerCase().contains("nl"))
			fileName = "bungee_nl_NL.yml";
		else if (lang.toLowerCase().contains("sv"))
			fileName = "bungee_sv_SV.yml";
		try (InputStream in = pl.getResourceAsStream(fileName); OutputStream out = new FileOutputStream(f)) {
			ByteStreams.copy(in, out);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return f;
	}

	@Override
	public TranslationProviderFactory getPlatformTranslationProviderFactory() {
		return this;
	}

	@Nullable
	@Override
	public TranslationProvider createTranslationProvider(String language) {
		String languageFileName = language + ".yml";
		File translationFile = new File(pl.getDataFolder(), "lang" + File.separator + languageFileName);
		try (InputStreamReader reader = new InputStreamReader(new FileInputStream(copy(language, translationFile)), StandardCharsets.UTF_8)) {
			Configuration msgConfig = ConfigurationProvider.getProvider(YamlConfiguration.class).load(reader);
			return new CachingTranslationProvider(new BungeeTranslationProvider(msgConfig));
		} catch (Exception e) {
			pl.getLogger().error("Could not load translation file {}", languageFileName, e);
			return null;
		}
	}

	@Nullable
	@Override
	public TranslationProvider createFallbackTranslationProvider() {
		try (InputStream inputStream = VelocityNegativity.getInstance().getResourceAsStream("bungee_en_US.yml")) {
			if (inputStream == null) {
				VelocityNegativity.getInstance().getLogger().warn("Could not find the fallback messages resource.");
				return null;
			}
			Configuration msgConfig = ConfigurationProvider.getProvider(YamlConfiguration.class).load(inputStream);
			return new CachingTranslationProvider(new BungeeTranslationProvider(msgConfig));
		} catch (Exception e) {
			pl.getLogger().error("Could not load the fallback translation resource ", e);
			return null;
		}
	}

	@Override
	public List<Cheat> getAbstractCheats() {
		return new ArrayList<>();
	}

	@Override
	public void reload() {

	}

	@Override
	public String getVersion() {
		return pl.getServer().getVersion().getVersion();
	}

	@Override
	public void reloadConfig() {

	}

	@Nonnull
	@Override
	public NegativityAccount getNegativityAccount(UUID playerId) {
		return new NegativityAccount(playerId, TranslatedMessages.getLang(playerId), false, new ArrayList<>());
	}

	@Nullable
	@Override
	public NegativityPlayer getNegativityPlayer(UUID playerId) {
		Optional<Player> player = pl.getServer().getPlayer(playerId);
		return player.isPresent() ? VelocityNegativityPlayer.getNegativityPlayer(player.get()) : null;
	}

	@Override
	public void invalidateAccount(UUID playerId) {}

	@Override
	public void alertMod(ReportType type, Object p, Cheat c, int reliability, String proof, String hover_proof) {}

	@Override
	public void runConsoleCommand(String cmd) {
		pl.getServer().getCommandManager().execute(pl.getServer().getConsoleCommandSource(), cmd);
	}
	
	@Override
	public CompletableFuture<Boolean> isUsingMcLeaks(UUID playerId) {
		return UniversalUtils.requestMcleaksData(playerId.toString()).thenApply(response -> {
			if (response == null) {
				return false;
			}
			try {
				Gson gson = new Gson();
				Map<?, ?> data = gson.fromJson(response, Map.class);
				Object isMcleaks = data.get("isMcleaks");
				if (isMcleaks != null) {
					return Boolean.parseBoolean(isMcleaks.toString());
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
			return false;
		});
	}
}
