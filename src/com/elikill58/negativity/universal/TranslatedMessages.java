package com.elikill58.negativity.universal;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.annotation.Nullable;

import com.elikill58.negativity.universal.adapter.Adapter;
import com.elikill58.negativity.universal.translation.TranslationProvider;
import com.elikill58.negativity.universal.translation.TranslationProviderFactory;

public class TranslatedMessages {

	public static final String PLATFORM_PROVIDER_ID = "platform";

	public static String DEFAULT_LANG = Adapter.getAdapter().getStringInConfig("Translation.default");
	public static List<String> LANGS = Adapter.getAdapter().getStringListInConfig("Translation.lang_available");
	public static String column = Adapter.getAdapter().getStringInConfig("Database.column_lang");
	public static boolean activeTranslation = Adapter.getAdapter().getBooleanInConfig("Translation.active"),
			useDb = Adapter.getAdapter().getBooleanInConfig("Translation.use_db");
	private static String providerFactoryId = PLATFORM_PROVIDER_ID;
	private static TranslationProviderFactory platformFactory = null;
	private static final Map<String, TranslationProviderFactory> registeredFactories = new HashMap<>();

	private static final Map<String, TranslationProvider> translationProviders = new HashMap<>();
	@Nullable
	private static TranslationProvider fallbackTranslationProvider = null;

	public static void init() {
		DEFAULT_LANG = Adapter.getAdapter().getStringInConfig("Translation.default");
		LANGS = Adapter.getAdapter().getStringListInConfig("Translation.lang_available");
		column = Adapter.getAdapter().getStringInConfig("Database.column_lang");
		activeTranslation = Adapter.getAdapter().getBooleanInConfig("Translation.active");
		useDb = Adapter.getAdapter().getBooleanInConfig("Translation.use_db");

		platformFactory = Adapter.getAdapter().getPlatformTranslationProviderFactory();
		registerTranslationProviderFactory(PLATFORM_PROVIDER_ID, platformFactory);

		providerFactoryId = Adapter.getAdapter().getStringInConfig("Translation.provider");
		loadMessages();
	}

	public static void loadMessages() {
		translationProviders.clear();
		TranslationProviderFactory factory = registeredFactories.getOrDefault(providerFactoryId, platformFactory);
		if (activeTranslation) {
			for (String lang : LANGS) {
				TranslationProvider provider = factory.createTranslationProvider(lang);
				if (provider != null) {
					translationProviders.put(lang, provider);
				}
			}
		} else {
			TranslationProvider provider = factory.createTranslationProvider(DEFAULT_LANG);
			if (provider == null) {
				Adapter.getAdapter().warn("Could not load the default translation provider");
				return;
			}
			translationProviders.put(DEFAULT_LANG, provider);
		}

		fallbackTranslationProvider = factory.createFallbackTranslationProvider();
	}

	public static void registerTranslationProviderFactory(String id, TranslationProviderFactory factory) {
		if (id == null || id.isEmpty()) {
			Adapter.getAdapter().warn("Could not register TranslationProviderFactory " + factory.getClass().getName() + " because of invalid id " + id);
			return;
		}
		registeredFactories.put(id, factory);
	}

	public static String loadLang(UUID playerId) {
		try {
			String idString = playerId.toString();
			if (useDb) {
				try (PreparedStatement stm = Database.getConnection()
						.prepareStatement("SELECT * FROM " + Database.table_lang + " WHERE uuid = ?")) {
					stm.setString(1, idString);
					ResultSet result = stm.executeQuery();
					if (result.next()) {
						String gettedLang = result.getString(column);
						for(String tempLang : LANGS)
							if(gettedLang.equalsIgnoreCase(tempLang) || gettedLang.contains(tempLang))
								return gettedLang;
						Adapter.getAdapter().warn("Unknow lang for player with UUID " + idString + ": " + gettedLang);
					}
				}
			}

			Path file = Paths .get("user" , idString + ".yml");
			return Adapter.getAdapter().getStringInOtherConfig(file, "lang", DEFAULT_LANG);
		} catch (Exception e) {
			e.printStackTrace();
			return DEFAULT_LANG;
		}
	}

	public static String getDefaultLang() {
		return DEFAULT_LANG;
	}

	public static String getLang(UUID playerId) {
		if (activeTranslation) {
			return loadLang(playerId);
		}
		return DEFAULT_LANG;
	}

	public static List<String> getStringListFromLang(String lang, String key, Object... placeholders) {
		TranslationProvider provider = getProviderFor(lang);
		if (provider != null) {
			List<String> messageList = provider.getList(key, placeholders);
			if (messageList != null && !messageList.isEmpty()) {
				return messageList;
			}
		}

		if (!lang.equals(DEFAULT_LANG)) {
			return getStringListFromLang(DEFAULT_LANG, key, placeholders);
		}

		if (fallbackTranslationProvider != null) {
			List<String> fallbackMessageList = fallbackTranslationProvider.getList(key, placeholders);
			if (fallbackMessageList != null && !fallbackMessageList.isEmpty()) {
				return fallbackMessageList;
			}
		}

		return Collections.singletonList(key);
	}

	public static String getStringFromLang(String lang, String key, Object... placeholders) {
		TranslationProvider provider = getProviderFor(lang);
		if (provider != null) {
			String message = provider.get(key, placeholders);
			if (message != null) {
				return message;
			}
		}

		if (!lang.equals(DEFAULT_LANG)) {
			return getStringFromLang(DEFAULT_LANG, key, placeholders);
		}

		if (fallbackTranslationProvider != null) {
			String fallbackMessage = fallbackTranslationProvider.get(key, placeholders);
			if (fallbackMessage != null) {
				return fallbackMessage;
			}
		}

		return key;
	}

	@Nullable
	private static TranslationProvider getProviderFor(String lang) {
		if (activeTranslation) {
			TranslationProvider provider = TranslatedMessages.translationProviders.get(lang);
			if (provider != null) {
				return provider;
			}
		}
		return TranslatedMessages.translationProviders.get(DEFAULT_LANG);
	}
}
