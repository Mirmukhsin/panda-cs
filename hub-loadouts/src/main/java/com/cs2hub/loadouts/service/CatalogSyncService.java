package com.cs2hub.loadouts.service;

import com.cs2hub.loadouts.domain.SkinColor;
import com.cs2hub.loadouts.domain.SkinEntity;
import com.cs2hub.loadouts.domain.StickerEntity;
import com.cs2hub.loadouts.domain.TeamSide;
import com.cs2hub.loadouts.domain.WeaponCategory;
import com.cs2hub.loadouts.dto.SeedSummaryDto;
import com.cs2hub.loadouts.repository.SkinRepository;
import com.cs2hub.loadouts.repository.StickerRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class CatalogSyncService {

    private final SkinRepository skinRepository;
    private final StickerRepository stickerRepository;
    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final String skinsApiUrl;
    private final String stickersApiUrl;
    private final boolean syncImagesOnStartup;

    @Autowired
    public CatalogSyncService(
            SkinRepository skinRepository,
            StickerRepository stickerRepository,
            RestClient.Builder restClientBuilder,
            ObjectMapper objectMapper,
            @Value("${app.loadouts.skins-api-url:https://raw.githubusercontent.com/ByMykel/CSGO-API/main/public/api/en/skins.json}") String skinsApiUrl,
            @Value("${app.loadouts.stickers-api-url:https://raw.githubusercontent.com/ByMykel/CSGO-API/main/public/api/en/stickers.json}") String stickersApiUrl,
            @Value("${app.loadouts.sync-images-on-startup:false}") boolean syncImagesOnStartup
    ) {
        this.skinRepository = skinRepository;
        this.stickerRepository = stickerRepository;
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofMillis(5000));
        requestFactory.setReadTimeout(Duration.ofMillis(5000));
        RestClient.Builder builder = restClientBuilder != null ? restClientBuilder : RestClient.builder();
        this.restClient = builder.requestFactory(requestFactory).build();
        this.objectMapper = objectMapper;
        this.skinsApiUrl = skinsApiUrl;
        this.stickersApiUrl = stickersApiUrl;
        this.syncImagesOnStartup = syncImagesOnStartup;
    }

    public CatalogSyncService(
            SkinRepository skinRepository,
            StickerRepository stickerRepository,
            RestClient.Builder restClientBuilder,
            ObjectMapper objectMapper,
            String skinsApiUrl,
            String stickersApiUrl
    ) {
        this(skinRepository, stickerRepository, restClientBuilder, objectMapper, skinsApiUrl, stickersApiUrl, false);
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onStartup() {
        if (skinRepository.count() == 0) {
            log.info("No weapon skins detected in database. Executing initial curated catalog seeding...");
            seedCuratedCatalog();
        } else {
            seedMissingCuratedSkins();
        }
        if (syncImagesOnStartup) {
            updateImageUrlsFromApi();
        }
    }

    public boolean isSyncImagesOnStartup() {
        return syncImagesOnStartup;
    }

    @Transactional
    public void seedMissingCuratedSkins() {
        List<SkinEntity> curatedSkins = buildCuratedSkins();
        List<SkinEntity> missingSkins = curatedSkins.stream()
                .filter(skin -> !skinRepository.existsById(skin.getId()))
                .toList();

        if (!missingSkins.isEmpty()) {
            skinRepository.saveAll(missingSkins);
            log.info("Seeded {} new budget curated skins into database", missingSkins.size());
        }

        List<StickerEntity> curatedStickers = buildCuratedStickers();
        List<StickerEntity> missingStickers = curatedStickers.stream()
                .filter(st -> !stickerRepository.existsById(st.getId()))
                .toList();
        if (!missingStickers.isEmpty()) {
            stickerRepository.saveAll(missingStickers);
            log.info("Seeded {} new curated stickers into database", missingStickers.size());
        }
    }

    @Transactional
    public SeedSummaryDto seedCuratedCatalog() {
        List<SkinEntity> curatedSkins = buildCuratedSkins();
        List<StickerEntity> curatedStickers = buildCuratedStickers();

        skinRepository.saveAll(curatedSkins);
        stickerRepository.saveAll(curatedStickers);

        String msg = String.format("Successfully seeded %d curated skins and %d stickers",
                curatedSkins.size(), curatedStickers.size());
        log.info(msg);

        return new SeedSummaryDto((int) skinRepository.count(), (int) stickerRepository.count(), msg);
    }

    @Transactional
    public void updateImageUrlsFromApi() {
        try {
            updateSkinImagesFromApi();
            updateStickerImagesFromApi();
        } catch (Exception ex) {
            log.warn("Non-fatal: Could not fetch real image URLs from external API: {}", ex.getMessage());
        }
    }

    private void updateSkinImagesFromApi() {
        if (!StringUtils.hasText(skinsApiUrl)) return;
        try {
            String json = restClient.get()
                    .uri(skinsApiUrl)
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(String.class);

            if (!StringUtils.hasText(json)) return;
            JsonNode root = objectMapper.readTree(json);
            if (!root.isArray()) return;

            Map<String, String> imageByName = new HashMap<>();
            for (JsonNode node : root) {
                String name = node.path("name").asText(null);
                String image = node.path("image").asText(null);
                if (StringUtils.hasText(name) && StringUtils.hasText(image) && image.startsWith("http")) {
                    imageByName.put(name.trim(), image.trim());
                }
            }

            List<SkinEntity> allSkins = skinRepository.findAll();
            List<SkinEntity> toUpdate = new ArrayList<>();
            for (SkinEntity skin : allSkins) {
                String realImg = imageByName.get(skin.getName().trim());
                if (StringUtils.hasText(realImg) && !realImg.equals(skin.getImageUrl())) {
                    skin.setImageUrl(realImg);
                    toUpdate.add(skin);
                }
            }

            if (!toUpdate.isEmpty()) {
                skinRepository.saveAll(toUpdate);
                log.info("Updated real CDN image URLs for {} weapon skins", toUpdate.size());
            }
        } catch (Exception ex) {
            log.warn("Could not sync real skin image URLs from ByMykel API: {}", ex.getMessage());
        }
    }

    private void updateStickerImagesFromApi() {
        if (!StringUtils.hasText(stickersApiUrl)) return;
        try {
            String json = restClient.get()
                    .uri(stickersApiUrl)
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(String.class);

            if (!StringUtils.hasText(json)) return;
            JsonNode root = objectMapper.readTree(json);
            if (!root.isArray()) return;

            Map<String, String> imageByName = new HashMap<>();
            for (JsonNode node : root) {
                String name = node.path("name").asText(null);
                String image = node.path("image").asText(null);
                if (StringUtils.hasText(name) && StringUtils.hasText(image) && image.startsWith("http")) {
                    imageByName.put(name.trim(), image.trim());
                }
            }

            List<StickerEntity> allStickers = stickerRepository.findAll();
            List<StickerEntity> toUpdate = new ArrayList<>();
            for (StickerEntity sticker : allStickers) {
                String realImg = imageByName.get(sticker.getName().trim());
                if (StringUtils.hasText(realImg) && !realImg.equals(sticker.getImageUrl())) {
                    sticker.setImageUrl(realImg);
                    toUpdate.add(sticker);
                }
            }

            if (!toUpdate.isEmpty()) {
                stickerRepository.saveAll(toUpdate);
                log.info("Updated real CDN image URLs for {} stickers", toUpdate.size());
            }
        } catch (Exception ex) {
            log.warn("Could not sync real sticker image URLs from ByMykel API: {}", ex.getMessage());
        }
    }

    @Transactional
    public SeedSummaryDto syncExternalCatalog() {
        log.info("Starting external catalog synchronization from {} and {}", skinsApiUrl, stickersApiUrl);
        int syncedSkins = 0;
        int syncedStickers = 0;

        try {
            syncedSkins = syncExternalSkins();
        } catch (Exception ex) {
            log.error("Failed syncing external skins: {}", ex.getMessage());
        }

        try {
            syncedStickers = syncExternalStickers();
        } catch (Exception ex) {
            log.error("Failed syncing external stickers: {}", ex.getMessage());
        }

        String msg = String.format("Catalog sync complete. Synced %d skins and %d stickers.", syncedSkins, syncedStickers);
        log.info(msg);
        return new SeedSummaryDto((int) skinRepository.count(), (int) stickerRepository.count(), msg);
    }

    private int syncExternalSkins() {
        if (!StringUtils.hasText(skinsApiUrl)) return 0;

        String json = restClient.get()
                .uri(skinsApiUrl)
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .body(String.class);

        if (!StringUtils.hasText(json)) return 0;

        List<SkinEntity> batch = new ArrayList<>();
        try {
            JsonNode root = objectMapper.readTree(json);
            if (!root.isArray()) return 0;

            for (JsonNode node : root) {
                String id = node.path("id").asText(null);
                String name = node.path("name").asText(null);
                String image = node.path("image").asText("");

                if (!StringUtils.hasText(id) || !StringUtils.hasText(name)) continue;

                JsonNode weaponNode = node.path("weapon");
                String weaponId = weaponNode.path("id").asText("weapon_unknown");
                String weaponName = weaponNode.path("name").asText("Unknown");

                JsonNode categoryNode = node.path("category");
                String catName = categoryNode.path("name").asText("Rifles");
                WeaponCategory category = mapWeaponCategory(catName, weaponId);

                JsonNode teamNode = node.path("team");
                String teamStr = teamNode.path("id").asText("both");
                TeamSide teamSide = mapTeamSide(teamStr);

                String rarity = node.path("rarity").path("name").asText("Mil-Spec Grade");
                SkinColor primaryColor = inferColor(name, node);

                double minFloat = node.path("min_float").asDouble(0.0);
                double maxFloat = node.path("max_float").asDouble(1.0);

                SkinEntity skin = SkinEntity.builder()
                        .id(id)
                        .name(name)
                        .weaponId(weaponId)
                        .weaponName(weaponName)
                        .category(category)
                        .teamSide(teamSide)
                        .rarity(rarity)
                        .primaryColor(primaryColor)
                        .imageUrl(image)
                        .minFloat(minFloat)
                        .maxFloat(maxFloat)
                        .priceFn(BigDecimal.valueOf(50.0))
                        .priceMw(BigDecimal.valueOf(30.0))
                        .priceFt(BigDecimal.valueOf(18.0))
                        .priceWw(BigDecimal.valueOf(14.0))
                        .priceBs(BigDecimal.valueOf(10.0))
                        .build();

                batch.add(skin);
                if (batch.size() >= 100) {
                    skinRepository.saveAll(batch);
                    batch.clear();
                }
            }
            if (!batch.isEmpty()) {
                skinRepository.saveAll(batch);
            }
            return (int) skinRepository.count();
        } catch (Exception e) {
            log.error("Error parsing external skins JSON: {}", e.getMessage());
            return 0;
        }
    }

    private int syncExternalStickers() {
        if (!StringUtils.hasText(stickersApiUrl)) return 0;

        String json = restClient.get()
                .uri(stickersApiUrl)
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .body(String.class);

        if (!StringUtils.hasText(json)) return 0;

        List<StickerEntity> batch = new ArrayList<>();
        try {
            JsonNode root = objectMapper.readTree(json);
            if (!root.isArray()) return 0;

            for (JsonNode node : root) {
                String id = node.path("id").asText(null);
                String name = node.path("name").asText(null);
                String image = node.path("image").asText("");

                if (!StringUtils.hasText(id) || !StringUtils.hasText(name)) continue;

                String rarity = node.path("rarity").path("name").asText("Remarkable");
                SkinColor color = inferColor(name, node);

                StickerEntity sticker = StickerEntity.builder()
                        .id(id)
                        .name(name)
                        .rarity(rarity)
                        .primaryColor(color)
                        .imageUrl(image)
                        .priceUsd(BigDecimal.valueOf(5.0))
                        .build();

                batch.add(sticker);
                if (batch.size() >= 100) {
                    stickerRepository.saveAll(batch);
                    batch.clear();
                }
            }
            if (!batch.isEmpty()) {
                stickerRepository.saveAll(batch);
            }
            return (int) stickerRepository.count();
        } catch (Exception e) {
            log.error("Error parsing external stickers JSON: {}", e.getMessage());
            return 0;
        }
    }

    private WeaponCategory mapWeaponCategory(String categoryName, String weaponId) {
        if (categoryName != null) {
            String lower = categoryName.toLowerCase();
            if (lower.contains("pistol")) return WeaponCategory.PISTOLS;
            if (lower.contains("rifle") || lower.contains("sniper")) return WeaponCategory.RIFLES;
            if (lower.contains("smg")) return WeaponCategory.SMGS;
            if (lower.contains("heavy") || lower.contains("shotgun") || lower.contains("machinegun")) return WeaponCategory.HEAVY;
            if (lower.contains("knife")) return WeaponCategory.KNIVES;
            if (lower.contains("glove")) return WeaponCategory.GLOVES;
        }
        if (weaponId != null) {
            if (weaponId.contains("knife") || weaponId.contains("bayonet") || weaponId.contains("karambit")) return WeaponCategory.KNIVES;
            if (weaponId.contains("glove")) return WeaponCategory.GLOVES;
            if (weaponId.contains("ak47") || weaponId.contains("m4a") || weaponId.contains("awp")) return WeaponCategory.RIFLES;
        }
        return WeaponCategory.RIFLES;
    }

    private TeamSide mapTeamSide(String teamStr) {
        if (teamStr == null) return TeamSide.BOTH;
        String lower = teamStr.toLowerCase();
        if (lower.equals("t")) return TeamSide.T;
        if (lower.equals("ct")) return TeamSide.CT;
        return TeamSide.BOTH;
    }

    private SkinColor inferColor(String name, JsonNode node) {
        String n = name.toLowerCase();
        if (n.contains("red") || n.contains("blood") || n.contains("fire") || n.contains("ruby") || n.contains("crimson") || n.contains("cyrex") || n.contains("kill confirmed") || n.contains("water elemental")) {
            return SkinColor.RED;
        }
        if (n.contains("blue") || n.contains("water") || n.contains("cyan") || n.contains("ice") || n.contains("sapphire") || n.contains("cobalt") || n.contains("phosphor") || n.contains("emperor")) {
            return SkinColor.BLUE;
        }
        if (n.contains("purple") || n.contains("violet") || n.contains("amethyst") || n.contains("neon rider") || n.contains("desolate") || n.contains("ultraviolet")) {
            return SkinColor.PURPLE;
        }
        if (n.contains("green") || n.contains("emerald") || n.contains("toxic") || n.contains("hydroponic") || n.contains("atheris")) {
            return SkinColor.GREEN;
        }
        if (n.contains("yellow") || n.contains("gold") || n.contains("lore") || n.contains("anubis") || n.contains("bullet queen") || n.contains("tiger")) {
            return SkinColor.YELLOW;
        }
        if (n.contains("orange") || n.contains("blaze") || n.contains("asiimov")) {
            return SkinColor.ORANGE;
        }
        if (n.contains("pink") || n.contains("cortex") || n.contains("fade") || n.contains("vice") || n.contains("disco tech")) {
            return SkinColor.PINK;
        }
        if (n.contains("white") || n.contains("printstream") || n.contains("snow") || n.contains("mecha")) {
            return SkinColor.WHITE;
        }
        return SkinColor.BLACK;
    }

    public List<SkinEntity> buildCuratedSkins() {
        return List.of(
                // AK-47
                createSkin("skin-ak47-asiimov", "AK-47 | Asiimov", "weapon_ak47", "AK-47", WeaponCategory.RIFLES, TeamSide.T, "Covert", SkinColor.ORANGE, SkinColor.WHITE,
                        "https://raw.githubusercontent.com/ByMykel/CSGO-API/main/public/images/skins/skin-ak47-asiimov.png",
                        0.05, 0.70, bd(220.00), bd(75.00), bd(38.00), bd(28.00), bd(22.00)),

                createSkin("skin-ak47-bloodsport", "AK-47 | Bloodsport", "weapon_ak47", "AK-47", WeaponCategory.RIFLES, TeamSide.T, "Covert", SkinColor.RED, SkinColor.WHITE,
                        "https://raw.githubusercontent.com/ByMykel/CSGO-API/main/public/images/skins/skin-ak47-bloodsport.png",
                        0.00, 0.45, bd(145.00), bd(115.00), bd(95.00), bd(85.00), null),

                createSkin("skin-ak47-fire-serpent", "AK-47 | Fire Serpent", "weapon_ak47", "AK-47", WeaponCategory.RIFLES, TeamSide.T, "Covert", SkinColor.GREEN, SkinColor.BLUE,
                        "https://raw.githubusercontent.com/ByMykel/CSGO-API/main/public/images/skins/skin-ak47-fire-serpent.png",
                        0.06, 0.76, bd(2800.00), bd(1600.00), bd(950.00), bd(750.00), bd(550.00)),

                createSkin("skin-ak47-vulcan", "AK-47 | Vulcan", "weapon_ak47", "AK-47", WeaponCategory.RIFLES, TeamSide.T, "Covert", SkinColor.BLUE, SkinColor.WHITE,
                        "https://raw.githubusercontent.com/ByMykel/CSGO-API/main/public/images/skins/skin-ak47-vulcan.png",
                        0.00, 0.90, bd(950.00), bd(480.00), bd(280.00), bd(210.00), bd(150.00)),

                createSkin("skin-ak47-slate", "AK-47 | Slate", "weapon_ak47", "AK-47", WeaponCategory.RIFLES, TeamSide.T, "Restricted", SkinColor.BLACK, null,
                        "https://raw.githubusercontent.com/ByMykel/CSGO-API/main/public/images/skins/skin-ak47-slate.png",
                        0.00, 1.00, bd(14.50), bd(7.80), bd(4.20), bd(3.50), bd(2.90)),

                createSkin("skin-ak47-elite-build", "AK-47 | Elite Build", "weapon_ak47", "AK-47", WeaponCategory.RIFLES, TeamSide.T, "Mil-Spec Grade", SkinColor.BLACK, SkinColor.ORANGE,
                        "https://raw.githubusercontent.com/ByMykel/CSGO-API/main/public/images/skins/skin-ak47-elite-build.png",
                        0.00, 1.00, bd(8.50), bd(3.20), bd(1.80), bd(1.20), bd(0.80)),

                createSkin("skin-ak47-uncharted", "AK-47 | Uncharted", "weapon_ak47", "AK-47", WeaponCategory.RIFLES, TeamSide.T, "Mil-Spec Grade", SkinColor.YELLOW, SkinColor.ORANGE,
                        "https://raw.githubusercontent.com/ByMykel/CSGO-API/main/public/images/skins/skin-ak47-uncharted.png",
                        0.00, 0.75, bd(1.80), bd(0.90), bd(0.40), bd(0.35), bd(0.30)),

                createSkin("skin-ak47-legion-of-anubis", "AK-47 | Legion of Anubis", "weapon_ak47", "AK-47", WeaponCategory.RIFLES, TeamSide.T, "Covert", SkinColor.GOLD, SkinColor.YELLOW,
                        "https://raw.githubusercontent.com/ByMykel/CSGO-API/main/public/images/skins/skin-ak47-legion-of-anubis.png",
                        0.00, 0.70, bd(58.00), bd(24.00), bd(14.00), bd(12.00), bd(9.80)),

                createSkin("skin-ak47-neon-rider", "AK-47 | Neon Rider", "weapon_ak47", "AK-47", WeaponCategory.RIFLES, TeamSide.T, "Covert", SkinColor.PINK, SkinColor.PURPLE,
                        "https://raw.githubusercontent.com/ByMykel/CSGO-API/main/public/images/skins/skin-ak47-neon-rider.png",
                        0.00, 0.80, bd(190.00), bd(75.00), bd(40.00), bd(32.00), bd(25.00)),

                createSkin("skin-ak47-nightwish", "AK-47 | Nightwish", "weapon_ak47", "AK-47", WeaponCategory.RIFLES, TeamSide.T, "Covert", SkinColor.PURPLE, SkinColor.BLUE,
                        "https://raw.githubusercontent.com/ByMykel/CSGO-API/main/public/images/skins/skin-ak47-nightwish.png",
                        0.00, 1.00, bd(120.00), bd(45.00), bd(22.00), bd(18.00), bd(14.50)),

                createSkin("skin-ak47-hydroponic", "AK-47 | Hydroponic", "weapon_ak47", "AK-47", WeaponCategory.RIFLES, TeamSide.T, "Classified", SkinColor.GREEN, SkinColor.WHITE,
                        "https://raw.githubusercontent.com/ByMykel/CSGO-API/main/public/images/skins/skin-ak47-hydroponic.png",
                        0.00, 0.80, bd(1850.00), bd(1200.00), bd(850.00), bd(650.00), bd(550.00)),

                // M4A1-S
                createSkin("skin-m4a1s-printstream", "M4A1-S | Printstream", "weapon_m4a1_silencer", "M4A1-S", WeaponCategory.RIFLES, TeamSide.CT, "Covert", SkinColor.WHITE, SkinColor.BLACK,
                        "https://raw.githubusercontent.com/ByMykel/CSGO-API/main/public/images/skins/skin-m4a1s-printstream.png",
                        0.00, 0.80, bd(450.00), bd(220.00), bd(140.00), bd(110.00), bd(90.00)),

                createSkin("skin-m4a1s-hyper-beast", "M4A1-S | Hyper Beast", "weapon_m4a1_silencer", "M4A1-S", WeaponCategory.RIFLES, TeamSide.CT, "Covert", SkinColor.PURPLE, SkinColor.RED,
                        "https://raw.githubusercontent.com/ByMykel/CSGO-API/main/public/images/skins/skin-m4a1s-hyper-beast.png",
                        0.00, 1.00, bd(210.00), bd(75.00), bd(30.00), bd(25.00), bd(19.50)),

                createSkin("skin-m4a1s-cyrex", "M4A1-S | Cyrex", "weapon_m4a1_silencer", "M4A1-S", WeaponCategory.RIFLES, TeamSide.CT, "Covert", SkinColor.RED, SkinColor.WHITE,
                        "https://raw.githubusercontent.com/ByMykel/CSGO-API/main/public/images/skins/skin-m4a1s-cyrex.png",
                        0.00, 0.50, bd(55.00), bd(32.00), bd(24.00), bd(22.00), bd(20.00)),

                createSkin("skin-m4a1s-night-terror", "M4A1-S | Night Terror", "weapon_m4a1_silencer", "M4A1-S", WeaponCategory.RIFLES, TeamSide.CT, "Restricted", SkinColor.RED, SkinColor.BLACK,
                        "https://raw.githubusercontent.com/ByMykel/CSGO-API/main/public/images/skins/skin-m4a1s-night-terror.png",
                        0.00, 0.70, bd(2.40), bd(1.20), bd(0.70), bd(0.50), bd(0.40)),

                createSkin("skin-m4a1s-mud-spec", "M4A1-S | Mud-Spec", "weapon_m4a1_silencer", "M4A1-S", WeaponCategory.RIFLES, TeamSide.CT, "Industrial Grade", SkinColor.GREEN, null,
                        "https://raw.githubusercontent.com/ByMykel/CSGO-API/main/public/images/skins/skin-m4a1s-mud-spec.png",
                        0.00, 0.80, bd(1.50), bd(0.80), bd(0.40), bd(0.35), bd(0.30)),

                createSkin("skin-m4a1s-fizzy-pop", "M4A1-S | Fizzy POP", "weapon_m4a1_silencer", "M4A1-S", WeaponCategory.RIFLES, TeamSide.CT, "Mil-Spec Grade", SkinColor.BLUE, SkinColor.RED,
                        "https://raw.githubusercontent.com/ByMykel/CSGO-API/main/public/images/skins/skin-m4a1s-fizzy-pop.png",
                        0.00, 0.55, bd(2.50), bd(1.40), bd(0.80), bd(0.60), bd(0.50)),

                createSkin("skin-m4a1s-blue-phosphor", "M4A1-S | Blue Phosphor", "weapon_m4a1_silencer", "M4A1-S", WeaponCategory.RIFLES, TeamSide.CT, "Classified", SkinColor.BLUE, null,
                        "https://raw.githubusercontent.com/ByMykel/CSGO-API/main/public/images/skins/skin-m4a1s-blue-phosphor.png",
                        0.00, 0.08, bd(850.00), bd(780.00), null, null, null),

                // M4A4
                createSkin("skin-m4a4-the-emperor", "M4A4 | The Emperor", "weapon_m4a4", "M4A4", WeaponCategory.RIFLES, TeamSide.CT, "Covert", SkinColor.BLUE, SkinColor.GOLD,
                        "https://raw.githubusercontent.com/ByMykel/CSGO-API/main/public/images/skins/skin-m4a4-the-emperor.png",
                        0.00, 0.80, bd(180.00), bd(70.00), bd(25.00), bd(20.00), bd(15.00)),

                createSkin("skin-m4a4-evil-daimyo", "M4A4 | Evil Daimyo", "weapon_m4a4", "M4A4", WeaponCategory.RIFLES, TeamSide.CT, "Restricted", SkinColor.RED, SkinColor.BLACK,
                        "https://raw.githubusercontent.com/ByMykel/CSGO-API/main/public/images/skins/skin-m4a4-evil-daimyo.png",
                        0.00, 0.52, bd(2.80), bd(1.80), bd(1.10), bd(0.90), bd(0.70)),

                createSkin("skin-m4a4-magnesium", "M4A4 | Magnesium", "weapon_m4a4", "M4A4", WeaponCategory.RIFLES, TeamSide.CT, "Mil-Spec Grade", SkinColor.WHITE, SkinColor.BLACK,
                        "https://raw.githubusercontent.com/ByMykel/CSGO-API/main/public/images/skins/skin-m4a4-magnesium.png",
                        0.00, 1.00, bd(1.60), bd(0.70), bd(0.35), bd(0.30), bd(0.25)),

                createSkin("skin-m4a4-desolate-space", "M4A4 | Desolate Space", "weapon_m4a4", "M4A4", WeaponCategory.RIFLES, TeamSide.CT, "Classified", SkinColor.PURPLE, SkinColor.BLUE,
                        "https://raw.githubusercontent.com/ByMykel/CSGO-API/main/public/images/skins/skin-m4a4-desolate-space.png",
                        0.00, 1.00, bd(38.00), bd(20.00), bd(12.00), bd(10.00), bd(8.20)),

                createSkin("skin-m4a4-howl", "M4A4 | Howl", "weapon_m4a4", "M4A4", WeaponCategory.RIFLES, TeamSide.CT, "Contraband", SkinColor.RED, SkinColor.ORANGE,
                        "https://raw.githubusercontent.com/ByMykel/CSGO-API/main/public/images/skins/skin-m4a4-howl.png",
                        0.00, 0.40, bd(6500.00), bd(4800.00), bd(3800.00), bd(3200.00), null),

                // AWP
                createSkin("skin-awp-asiimov", "AWP | Asiimov", "weapon_awp", "AWP", WeaponCategory.RIFLES, TeamSide.BOTH, "Covert", SkinColor.ORANGE, SkinColor.WHITE,
                        "https://raw.githubusercontent.com/ByMykel/CSGO-API/main/public/images/skins/skin-awp-asiimov.png",
                        0.18, 1.00, null, null, bd(135.00), bd(105.00), bd(85.00)),

                createSkin("skin-awp-capillary", "AWP | Capillary", "weapon_awp", "AWP", WeaponCategory.RIFLES, TeamSide.BOTH, "Mil-Spec Grade", SkinColor.RED, SkinColor.BLACK,
                        "https://raw.githubusercontent.com/ByMykel/CSGO-API/main/public/images/skins/skin-awp-capillary.png",
                        0.05, 0.70, bd(2.20), bd(0.90), bd(0.50), bd(0.40), bd(0.35)),

                createSkin("skin-awp-worm-god", "AWP | Worm God", "weapon_awp", "AWP", WeaponCategory.RIFLES, TeamSide.BOTH, "Restricted", SkinColor.WHITE, SkinColor.GREEN,
                        "https://raw.githubusercontent.com/ByMykel/CSGO-API/main/public/images/skins/skin-awp-worm-god.png",
                        0.00, 0.45, bd(2.40), bd(1.40), bd(0.90), bd(0.80), bd(0.70)),

                createSkin("skin-awp-acheron", "AWP | Acheron", "weapon_awp", "AWP", WeaponCategory.RIFLES, TeamSide.BOTH, "Mil-Spec Grade", SkinColor.BLACK, SkinColor.BLUE,
                        "https://raw.githubusercontent.com/ByMykel/CSGO-API/main/public/images/skins/skin-awp-acheron.png",
                        0.00, 0.83, bd(2.50), bd(1.20), bd(0.60), bd(0.50), bd(0.40)),

                createSkin("skin-awp-neo-noir", "AWP | Neo-Noir", "weapon_awp", "AWP", WeaponCategory.RIFLES, TeamSide.BOTH, "Covert", SkinColor.PURPLE, SkinColor.PINK,
                        "https://raw.githubusercontent.com/ByMykel/CSGO-API/main/public/images/skins/skin-awp-neo-noir.png",
                        0.00, 0.70, bd(45.00), bd(32.00), bd(24.00), bd(22.00), bd(20.00)),

                createSkin("skin-awp-atheris", "AWP | Atheris", "weapon_awp", "AWP", WeaponCategory.RIFLES, TeamSide.BOTH, "Restricted", SkinColor.GREEN, SkinColor.BLACK,
                        "https://raw.githubusercontent.com/ByMykel/CSGO-API/main/public/images/skins/skin-awp-atheris.png",
                        0.00, 1.00, bd(18.00), bd(8.00), bd(4.50), bd(3.80), bd(3.20)),

                createSkin("skin-awp-dragon-lore", "AWP | Dragon Lore", "weapon_awp", "AWP", WeaponCategory.RIFLES, TeamSide.BOTH, "Covert", SkinColor.GOLD, SkinColor.YELLOW,
                        "https://raw.githubusercontent.com/ByMykel/CSGO-API/main/public/images/skins/skin-awp-dragon-lore.png",
                        0.00, 0.70, bd(12000.00), bd(8500.00), bd(5500.00), bd(4000.00), bd(3200.00)),

                createSkin("skin-awp-wildfire", "AWP | Wildfire", "weapon_awp", "AWP", WeaponCategory.RIFLES, TeamSide.BOTH, "Covert", SkinColor.RED, SkinColor.YELLOW,
                        "https://raw.githubusercontent.com/ByMykel/CSGO-API/main/public/images/skins/skin-awp-wildfire.png",
                        0.01, 0.70, bd(180.00), bd(105.00), bd(60.00), bd(50.00), bd(42.00)),

                // USP-S
                createSkin("skin-usps-kill-confirmed", "USP-S | Kill Confirmed", "weapon_usp_silencer", "USP-S", WeaponCategory.PISTOLS, TeamSide.CT, "Covert", SkinColor.RED, null,
                        "https://raw.githubusercontent.com/ByMykel/CSGO-API/main/public/images/skins/skin-usps-kill-confirmed.png",
                        0.00, 1.00, bd(240.00), bd(120.00), bd(65.00), bd(50.00), bd(42.00)),

                createSkin("skin-usps-check-engine", "USP-S | Check Engine", "weapon_usp_silencer", "USP-S", WeaponCategory.PISTOLS, TeamSide.CT, "Mil-Spec Grade", SkinColor.RED, SkinColor.BLACK,
                        "https://raw.githubusercontent.com/ByMykel/CSGO-API/main/public/images/skins/skin-usps-check-engine.png",
                        0.00, 0.60, bd(2.50), bd(1.50), bd(0.90), bd(0.70), bd(0.50)),

                createSkin("skin-usps-black-lotus", "USP-S | Black Lotus", "weapon_usp_silencer", "USP-S", WeaponCategory.PISTOLS, TeamSide.CT, "Mil-Spec Grade", SkinColor.PURPLE, SkinColor.BLUE,
                        "https://raw.githubusercontent.com/ByMykel/CSGO-API/main/public/images/skins/skin-usps-black-lotus.png",
                        0.00, 1.00, bd(3.20), bd(1.80), bd(1.10), bd(0.90), bd(0.70)),

                createSkin("skin-usps-blood-tiger", "USP-S | Blood Tiger", "weapon_usp_silencer", "USP-S", WeaponCategory.PISTOLS, TeamSide.CT, "Mil-Spec Grade", SkinColor.RED, SkinColor.GREEN,
                        "https://raw.githubusercontent.com/ByMykel/CSGO-API/main/public/images/skins/skin-usps-blood-tiger.png",
                        0.00, 0.30, bd(1.80), bd(0.90), bd(0.50), bd(0.40), bd(0.35)),

                createSkin("skin-usps-cortex", "USP-S | Cortex", "weapon_usp_silencer", "USP-S", WeaponCategory.PISTOLS, TeamSide.CT, "Classified", SkinColor.PINK, SkinColor.WHITE,
                        "https://raw.githubusercontent.com/ByMykel/CSGO-API/main/public/images/skins/skin-usps-cortex.png",
                        0.00, 1.00, bd(16.00), bd(9.00), bd(5.50), bd(4.50), bd(4.00)),

                createSkin("skin-usps-ticket-to-hell", "USP-S | Ticket to Hell", "weapon_usp_silencer", "USP-S", WeaponCategory.PISTOLS, TeamSide.CT, "Restricted", SkinColor.WHITE, SkinColor.BLACK,
                        "https://raw.githubusercontent.com/ByMykel/CSGO-API/main/public/images/skins/skin-usps-ticket-to-hell.png",
                        0.00, 0.76, bd(6.00), bd(2.50), bd(1.20), bd(1.00), bd(0.90)),

                // Glock-18
                createSkin("skin-glock-water-elemental", "Glock-18 | Water Elemental", "weapon_glock", "Glock-18", WeaponCategory.PISTOLS, TeamSide.T, "Classified", SkinColor.RED, SkinColor.BLUE,
                        "https://raw.githubusercontent.com/ByMykel/CSGO-API/main/public/images/skins/skin-glock-water-elemental.png",
                        0.00, 0.70, bd(16.00), bd(11.00), bd(7.50), bd(7.00), bd(6.50)),

                createSkin("skin-glock-candy-apple", "Glock-18 | Candy Apple", "weapon_glock", "Glock-18", WeaponCategory.PISTOLS, TeamSide.T, "Mil-Spec Grade", SkinColor.RED, null,
                        "https://raw.githubusercontent.com/ByMykel/CSGO-API/main/public/images/skins/skin-glock-candy-apple.png",
                        0.00, 0.30, bd(1.90), bd(0.90), bd(0.50), bd(0.40), bd(0.35)),

                createSkin("skin-glock-oxide-blaze", "Glock-18 | Oxide Blaze", "weapon_glock", "Glock-18", WeaponCategory.PISTOLS, TeamSide.T, "Mil-Spec Grade", SkinColor.ORANGE, SkinColor.BLACK,
                        "https://raw.githubusercontent.com/ByMykel/CSGO-API/main/public/images/skins/skin-glock-oxide-blaze.png",
                        0.00, 0.85, bd(1.20), bd(0.60), bd(0.35), bd(0.30), bd(0.25)),

                createSkin("skin-glock-high-beam", "Glock-18 | High Beam", "weapon_glock", "Glock-18", WeaponCategory.PISTOLS, TeamSide.T, "Industrial Grade", SkinColor.BLUE, null,
                        "https://raw.githubusercontent.com/ByMykel/CSGO-API/main/public/images/skins/skin-glock-high-beam.png",
                        0.00, 0.08, bd(1.50), bd(0.80), bd(0.40), bd(0.35), bd(0.30)),

                createSkin("skin-glock-winterized", "Glock-18 | Winterized", "weapon_glock", "Glock-18", WeaponCategory.PISTOLS, TeamSide.T, "Mil-Spec Grade", SkinColor.GREEN, SkinColor.WHITE,
                        "https://raw.githubusercontent.com/ByMykel/CSGO-API/main/public/images/skins/skin-glock-winterized.png",
                        0.00, 1.00, bd(1.20), bd(0.50), bd(0.30), bd(0.25), bd(0.20)),

                createSkin("skin-glock-bullet-queen", "Glock-18 | Bullet Queen", "weapon_glock", "Glock-18", WeaponCategory.PISTOLS, TeamSide.T, "Covert", SkinColor.YELLOW, SkinColor.PINK,
                        "https://raw.githubusercontent.com/ByMykel/CSGO-API/main/public/images/skins/skin-glock-bullet-queen.png",
                        0.00, 1.00, bd(70.00), bd(28.00), bd(12.00), bd(9.00), bd(7.00)),

                // Desert Eagle
                createSkin("skin-deagle-printstream", "Desert Eagle | Printstream", "weapon_deagle", "Desert Eagle", WeaponCategory.PISTOLS, TeamSide.BOTH, "Covert", SkinColor.WHITE, SkinColor.BLACK,
                        "https://raw.githubusercontent.com/ByMykel/CSGO-API/main/public/images/skins/skin-deagle-printstream.png",
                        0.00, 0.80, bd(135.00), bd(75.00), bd(48.00), bd(42.00), bd(38.00)),

                createSkin("skin-deagle-code-red", "Desert Eagle | Code Red", "weapon_deagle", "Desert Eagle", WeaponCategory.PISTOLS, TeamSide.BOTH, "Covert", SkinColor.RED, SkinColor.WHITE,
                        "https://raw.githubusercontent.com/ByMykel/CSGO-API/main/public/images/skins/skin-deagle-code-red.png",
                        0.00, 1.00, bd(85.00), bd(45.00), bd(28.00), bd(24.00), bd(20.00)),

                createSkin("skin-deagle-bronze-deco", "Desert Eagle | Bronze Deco", "weapon_deagle", "Desert Eagle", WeaponCategory.PISTOLS, TeamSide.BOTH, "Mil-Spec Grade", SkinColor.GOLD, SkinColor.ORANGE,
                        "https://raw.githubusercontent.com/ByMykel/CSGO-API/main/public/images/skins/skin-deagle-bronze-deco.png",
                        0.00, 0.46, bd(1.50), bd(0.70), bd(0.40), bd(0.35), bd(0.30)),

                createSkin("skin-deagle-blue-ply", "Desert Eagle | Blue Ply", "weapon_deagle", "Desert Eagle", WeaponCategory.PISTOLS, TeamSide.BOTH, "Mil-Spec Grade", SkinColor.BLUE, null,
                        "https://raw.githubusercontent.com/ByMykel/CSGO-API/main/public/images/skins/skin-deagle-blue-ply.png",
                        0.00, 1.00, bd(1.60), bd(0.80), bd(0.45), bd(0.35), bd(0.30)),

                createSkin("skin-deagle-corinthian", "Desert Eagle | Corinthian", "weapon_deagle", "Desert Eagle", WeaponCategory.PISTOLS, TeamSide.BOTH, "Mil-Spec Grade", SkinColor.GOLD, SkinColor.BLACK,
                        "https://raw.githubusercontent.com/ByMykel/CSGO-API/main/public/images/skins/skin-deagle-corinthian.png",
                        0.00, 0.44, bd(1.40), bd(0.70), bd(0.40), bd(0.35), bd(0.30)),

                createSkin("skin-deagle-trigger-discipline", "Desert Eagle | Trigger Discipline", "weapon_deagle", "Desert Eagle", WeaponCategory.PISTOLS, TeamSide.BOTH, "Restricted", SkinColor.PINK, SkinColor.BLACK,
                        "https://raw.githubusercontent.com/ByMykel/CSGO-API/main/public/images/skins/skin-deagle-trigger-discipline.png",
                        0.00, 0.97, bd(2.80), bd(1.40), bd(0.80), bd(0.60), bd(0.45)),

                createSkin("skin-deagle-ocean-drive", "Desert Eagle | Ocean Drive", "weapon_deagle", "Desert Eagle", WeaponCategory.PISTOLS, TeamSide.BOTH, "Covert", SkinColor.ORANGE, SkinColor.BLUE,
                        "https://raw.githubusercontent.com/ByMykel/CSGO-API/main/public/images/skins/skin-deagle-ocean-drive.png",
                        0.00, 1.00, bd(110.00), bd(48.00), bd(24.00), bd(18.00), bd(15.00)),

                // SMGs
                createSkin("skin-mp9-starlight-protector", "MP9 | Starlight Protector", "weapon_mp9", "MP9", WeaponCategory.SMGS, TeamSide.CT, "Covert", SkinColor.WHITE, SkinColor.GOLD,
                        "https://raw.githubusercontent.com/ByMykel/CSGO-API/main/public/images/skins/skin-mp9-starlight-protector.png",
                        0.00, 0.80, bd(35.00), bd(16.00), bd(8.50), bd(7.00), bd(6.00)),

                createSkin("skin-mp9-ruby-poison-dart", "MP9 | Ruby Poison Dart", "weapon_mp9", "MP9", WeaponCategory.SMGS, TeamSide.CT, "Restricted", SkinColor.RED, SkinColor.PURPLE,
                        "https://raw.githubusercontent.com/ByMykel/CSGO-API/main/public/images/skins/skin-mp9-ruby-poison-dart.png",
                        0.00, 0.50, bd(2.00), bd(1.10), bd(0.60), bd(0.40), bd(0.35)),

                createSkin("skin-mp9-featherweight", "MP9 | Featherweight", "weapon_mp9", "MP9", WeaponCategory.SMGS, TeamSide.CT, "Mil-Spec Grade", SkinColor.WHITE, null,
                        "https://raw.githubusercontent.com/ByMykel/CSGO-API/main/public/images/skins/skin-mp9-featherweight.png",
                        0.00, 1.00, bd(1.20), bd(0.50), bd(0.30), bd(0.25), bd(0.20)),

                createSkin("skin-mp9-deadly-poison", "MP9 | Deadly Poison", "weapon_mp9", "MP9", WeaponCategory.SMGS, TeamSide.CT, "Mil-Spec Grade", SkinColor.ORANGE, SkinColor.BLACK,
                        "https://raw.githubusercontent.com/ByMykel/CSGO-API/main/public/images/skins/skin-mp9-deadly-poison.png",
                        0.00, 1.00, bd(1.40), bd(0.60), bd(0.35), bd(0.30), bd(0.25)),

                createSkin("skin-mac10-disco-tech", "MAC-10 | Disco Tech", "weapon_mac10", "MAC-10", WeaponCategory.SMGS, TeamSide.T, "Classified", SkinColor.PURPLE, SkinColor.BLUE,
                        "https://raw.githubusercontent.com/ByMykel/CSGO-API/main/public/images/skins/skin-mac10-disco-tech.png",
                        0.00, 1.00, bd(20.00), bd(8.50), bd(4.20), bd(3.50), bd(3.00)),

                createSkin("skin-mac10-candy-apple", "MAC-10 | Candy Apple", "weapon_mac10", "MAC-10", WeaponCategory.SMGS, TeamSide.T, "Mil-Spec Grade", SkinColor.RED, null,
                        "https://raw.githubusercontent.com/ByMykel/CSGO-API/main/public/images/skins/skin-mac10-candy-apple.png",
                        0.00, 0.30, bd(1.20), bd(0.60), bd(0.35), bd(0.30), bd(0.25)),

                createSkin("skin-mac10-silver", "MAC-10 | Silver", "weapon_mac10", "MAC-10", WeaponCategory.SMGS, TeamSide.T, "Industrial Grade", SkinColor.WHITE, null,
                        "https://raw.githubusercontent.com/ByMykel/CSGO-API/main/public/images/skins/skin-mac10-silver.png",
                        0.00, 0.08, bd(1.40), bd(0.70), bd(0.40), bd(0.35), bd(0.30)),

                createSkin("skin-mac10-allure", "MAC-10 | Allure", "weapon_mac10", "MAC-10", WeaponCategory.SMGS, TeamSide.T, "Restricted", SkinColor.BLUE, SkinColor.WHITE,
                        "https://raw.githubusercontent.com/ByMykel/CSGO-API/main/public/images/skins/skin-mac10-allure.png",
                        0.00, 1.00, bd(1.90), bd(0.90), bd(0.50), bd(0.40), bd(0.30)),

                createSkin("skin-mac10-ensnared", "MAC-10 | Ensnared", "weapon_mac10", "MAC-10", WeaponCategory.SMGS, TeamSide.T, "Mil-Spec Grade", SkinColor.GREEN, SkinColor.BLACK,
                        "https://raw.githubusercontent.com/ByMykel/CSGO-API/main/public/images/skins/skin-mac10-ensnared.png",
                        0.00, 0.90, bd(1.10), bd(0.50), bd(0.30), bd(0.25), bd(0.20)),

                // Knives & Gloves
                createSkin("skin-knife-karambit-doppler", "Karambit | Doppler", "weapon_knife_karambit", "Karambit", WeaponCategory.KNIVES, TeamSide.BOTH, "Covert", SkinColor.PURPLE, SkinColor.PINK,
                        "https://raw.githubusercontent.com/ByMykel/CSGO-API/main/public/images/skins/skin-knife-karambit-doppler.png",
                        0.00, 0.08, bd(1600.00), bd(1400.00), null, null, null),

                createSkin("skin-knife-butterfly-fade", "Butterfly Knife | Fade", "weapon_knife_butterfly", "Butterfly Knife", WeaponCategory.KNIVES, TeamSide.BOTH, "Covert", SkinColor.YELLOW, SkinColor.PURPLE,
                        "https://raw.githubusercontent.com/ByMykel/CSGO-API/main/public/images/skins/skin-knife-butterfly-fade.png",
                        0.00, 0.08, bd(3200.00), bd(2800.00), null, null, null),

                createSkin("skin-gloves-sport-vice", "Sport Gloves | Vice", "weapon_gloves_sport", "Sport Gloves", WeaponCategory.GLOVES, TeamSide.BOTH, "Extraordinary", SkinColor.PINK, SkinColor.BLUE,
                        "https://raw.githubusercontent.com/ByMykel/CSGO-API/main/public/images/skins/skin-gloves-sport-vice.png",
                        0.06, 0.80, bd(18000.00), bd(6500.00), bd(2200.00), bd(1400.00), bd(900.00))
        );
    }

    public List<StickerEntity> buildCuratedStickers() {
        return List.of(
                createSticker("sticker-titan-holo-katowice-2014", "Sticker | Titan (Holo) | Katowice 2014", "Exotic", SkinColor.BLUE,
                        "https://raw.githubusercontent.com/ByMykel/CSGO-API/main/public/images/stickers/sticker-titan-holo-katowice-2014.png",
                        bd(55000.00)),

                createSticker("sticker-ibuypower-holo-katowice-2014", "Sticker | iBUYPOWER (Holo) | Katowice 2014", "Exotic", SkinColor.RED,
                        "https://raw.githubusercontent.com/ByMykel/CSGO-API/main/public/images/stickers/sticker-ibuypower-holo-katowice-2014.png",
                        bd(65000.00)),

                createSticker("sticker-reason-gaming-holo-katowice-2014", "Sticker | Reason Gaming (Holo) | Katowice 2014", "Exotic", SkinColor.ORANGE,
                        "https://raw.githubusercontent.com/ByMykel/CSGO-API/main/public/images/stickers/sticker-reason-gaming-holo-katowice-2014.png",
                        bd(48000.00)),

                createSticker("sticker-vox-eminor-holo-katowice-2014", "Sticker | Vox Eminor (Holo) | Katowice 2014", "Exotic", SkinColor.YELLOW,
                        "https://raw.githubusercontent.com/ByMykel/CSGO-API/main/public/images/stickers/sticker-vox-eminor-holo-katowice-2014.png",
                        bd(35000.00)),

                createSticker("sticker-team-dignitas-holo-katowice-2014", "Sticker | Team Dignitas (Holo) | Katowice 2014", "Exotic", SkinColor.YELLOW,
                        "https://raw.githubusercontent.com/ByMykel/CSGO-API/main/public/images/stickers/sticker-team-dignitas-holo-katowice-2014.png",
                        bd(22000.00)),

                createSticker("sticker-cloud9-g2a-holo-katowice-2015", "Sticker | Cloud9 G2A (Holo) | Katowice 2015", "Remarkable", SkinColor.BLUE,
                        "https://raw.githubusercontent.com/ByMykel/CSGO-API/main/public/images/stickers/sticker-cloud9-g2a-holo-katowice-2015.png",
                        bd(950.00)),

                createSticker("sticker-crown-foil", "Sticker | Crown (Foil)", "Exotic", SkinColor.GOLD,
                        "https://raw.githubusercontent.com/ByMykel/CSGO-API/main/public/images/stickers/sticker-crown-foil.png",
                        bd(850.00)),

                createSticker("sticker-howling-dawn", "Sticker | Howling Dawn", "Contraband", SkinColor.RED,
                        "https://raw.githubusercontent.com/ByMykel/CSGO-API/main/public/images/stickers/sticker-howling-dawn.png",
                        bd(1450.00)),

                createSticker("sticker-flipsid3-holo-katowice-2015", "Sticker | FlipSid3 Tactics (Holo) | Katowice 2015", "Remarkable", SkinColor.GREEN,
                        "https://raw.githubusercontent.com/ByMykel/CSGO-API/main/public/images/stickers/sticker-flipsid3-holo-katowice-2015.png",
                        bd(420.00)),

                createSticker("sticker-team-liquid-foil-mlg-columbus-2016", "Sticker | Team Liquid (Foil) | MLG Columbus 2016", "Exotic", SkinColor.BLUE,
                        "https://raw.githubusercontent.com/ByMykel/CSGO-API/main/public/images/stickers/sticker-team-liquid-foil-mlg-columbus-2016.png",
                        bd(380.00))
        );
    }

    private SkinEntity createSkin(String id, String name, String weaponId, String weaponName, WeaponCategory category,
                                  TeamSide teamSide, String rarity, SkinColor primaryColor, SkinColor secondaryColor,
                                  String imageUrl, double minFloat, double maxFloat,
                                  BigDecimal fn, BigDecimal mw, BigDecimal ft, BigDecimal ww, BigDecimal bs) {
        return SkinEntity.builder()
                .id(id)
                .name(name)
                .weaponId(weaponId)
                .weaponName(weaponName)
                .category(category)
                .teamSide(teamSide)
                .rarity(rarity)
                .primaryColor(primaryColor)
                .secondaryColor(secondaryColor)
                .imageUrl(imageUrl)
                .minFloat(minFloat)
                .maxFloat(maxFloat)
                .priceFn(fn)
                .priceMw(mw)
                .priceFt(ft)
                .priceWw(ww)
                .priceBs(bs)
                .build();
    }

    private StickerEntity createSticker(String id, String name, String rarity, SkinColor color, String imageUrl, BigDecimal price) {
        return StickerEntity.builder()
                .id(id)
                .name(name)
                .rarity(rarity)
                .primaryColor(color)
                .imageUrl(imageUrl)
                .priceUsd(price)
                .build();
    }

    private BigDecimal bd(double val) {
        return BigDecimal.valueOf(val);
    }
}
