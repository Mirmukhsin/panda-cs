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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class CatalogSyncService {

    private final SkinRepository skinRepository;
    private final StickerRepository stickerRepository;
    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final String skinsApiUrl;
    private final String stickersApiUrl;

    public CatalogSyncService(
            SkinRepository skinRepository,
            StickerRepository stickerRepository,
            RestClient.Builder restClientBuilder,
            ObjectMapper objectMapper,
            @Value("${app.loadouts.skins-api-url:https://raw.githubusercontent.com/ByMykel/CSGO-API/main/public/api/en/skins.json}") String skinsApiUrl,
            @Value("${app.loadouts.stickers-api-url:https://raw.githubusercontent.com/ByMykel/CSGO-API/main/public/api/en/stickers.json}") String stickersApiUrl
    ) {
        this.skinRepository = skinRepository;
        this.stickerRepository = stickerRepository;
        this.restClient = restClientBuilder != null ? restClientBuilder.build() : RestClient.create();
        this.objectMapper = objectMapper;
        this.skinsApiUrl = skinsApiUrl;
        this.stickersApiUrl = stickersApiUrl;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onStartup() {
        if (skinRepository.count() == 0) {
            log.info("No weapon skins detected in database. Executing initial curated catalog seeding...");
            seedCuratedCatalog();
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
        String lower = (name != null ? name : "").toLowerCase();
        if (lower.contains("red") || lower.contains("crimson") || lower.contains("ruby") || lower.contains("howl") || lower.contains("kill confirmed") || lower.contains("cyrex") || lower.contains("autotronic")) return SkinColor.RED;
        if (lower.contains("blue") || lower.contains("cobalt") || lower.contains("sapphire") || lower.contains("vulcan") || lower.contains("ocean") || lower.contains("phosphor") || lower.contains("emperor")) return SkinColor.BLUE;
        if (lower.contains("purple") || lower.contains("violet") || lower.contains("neo-noir") || lower.contains("ultraviolet") || lower.contains("hyper beast") || lower.contains("nightwish")) return SkinColor.PURPLE;
        if (lower.contains("green") || lower.contains("emerald") || lower.contains("hydroponic") || lower.contains("atheris") || lower.contains("containment")) return SkinColor.GREEN;
        if (lower.contains("yellow") || lower.contains("gold") || lower.contains("dragon lore") || lower.contains("lore") || lower.contains("tiger") || lower.contains("anubis")) return SkinColor.YELLOW;
        if (lower.contains("orange") || lower.contains("asiimov") || lower.contains("fade") || lower.contains("blaze")) return SkinColor.ORANGE;
        if (lower.contains("white") || lower.contains("printstream") || lower.contains("snow") || lower.contains("mecha")) return SkinColor.WHITE;
        if (lower.contains("pink") || lower.contains("cortex") || lower.contains("bullet queen") || lower.contains("neon rider") || lower.contains("fever dream")) return SkinColor.PINK;
        return SkinColor.BLACK;
    }

    public List<SkinEntity> buildCuratedSkins() {
        return List.of(
                // AK-47
                createSkin("skin-ak47-redline", "AK-47 | Redline", "weapon_ak47", "AK-47", WeaponCategory.RIFLES, TeamSide.T, "Classified", SkinColor.RED, SkinColor.BLACK,
                        "https://community.cloudflare.steamstatic.com/economy/image/-9a81dlWLwJ2UUGcVs_nsVtzdOEdtWwKGZZLQHTxDZ7I56KU0Zwwo4NUX4oFJZEHLbXH5ApeO4YmlhxYQknCRvCo04DEVlxkKgpot7HxfDhjxszJemkV09-5lpKKqPrxN7LEmyUE6ZUp37-T89in2wXiqkBsYW-nco6ddwZtN1nQ-1W7kLq-g5C875zOm3VruSQm52GdwULjYQO3HA/360fx360f",
                        0.10, 0.70, null, bd(35.00), bd(18.50), bd(16.00), bd(14.00)),

                createSkin("skin-ak47-asiimov", "AK-47 | Asiimov", "weapon_ak47", "AK-47", WeaponCategory.RIFLES, TeamSide.T, "Covert", SkinColor.ORANGE, SkinColor.WHITE,
                        "https://community.cloudflare.steamstatic.com/economy/image/-9a81dlWLwJ2UUGcVs_nsVtzdOEdtWwKGZZLQHTxDZ7I56KU0Zwwo4NUX4oFJZEHLbXH5ApeO4YmlhxYQknCRvCo04DEVlxkKgpot7HxfDhjxszJemkV08y5nY6fqPP9ILrDhGpI18h0juDU-MKt0QLg_kY5NTj1cYDEcVRtZw3Y-gC4ku_ugZC_6pnLnCRi63R35X7cnAv330_5pYv-9Q/360fx360f",
                        0.05, 0.70, bd(240.00), bd(85.00), bd(45.00), bd(35.00), bd(28.00)),

                createSkin("skin-ak47-vulcan", "AK-47 | Vulcan", "weapon_ak47", "AK-47", WeaponCategory.RIFLES, TeamSide.T, "Covert", SkinColor.BLUE, SkinColor.WHITE,
                        "https://community.cloudflare.steamstatic.com/economy/image/-9a81dlWLwJ2UUGcVs_nsVtzdOEdtWwKGZZLQHTxDZ7I56KU0Zwwo4NUX4oFJZEHLbXH5ApeO4YmlhxYQknCRvCo04DEVlxkKgpot7HxfDhjxszJemkV09-5lpKKqPrxN7LEmyUF6cR33b6V84mg2Abk_Us5Nmn3cY_EdlU-YgqBqVO-yOvr1sW4vsuYziEyvXQn-z-DyPj5K94d/360fx360f",
                        0.00, 0.90, bd(950.00), bd(480.00), bd(280.00), bd(210.00), bd(150.00)),

                createSkin("skin-ak47-slate", "AK-47 | Slate", "weapon_ak47", "AK-47", WeaponCategory.RIFLES, TeamSide.T, "Restricted", SkinColor.BLACK, null,
                        "https://community.cloudflare.steamstatic.com/economy/image/-9a81dlWLwJ2UUGcVs_nsVtzdOEdtWwKGZZLQHTxDZ7I56KU0Zwwo4NUX4oFJZEHLbXH5ApeO4YmlhxYQknCRvCo04DEVlxkKgpot7HxfDhjxszJemkV0924l5OfmvLwOq7c2DwB65Zw2-rE89vx2AXnqUpqZWv0IdTAIwZsYVnW8gO4w-jpg8e_vZidnHMx7HQ8pGB8sjJzH8p6/360fx360f",
                        0.00, 1.00, bd(14.50), bd(7.80), bd(4.20), bd(3.50), bd(2.90)),

                createSkin("skin-ak47-legion-of-anubis", "AK-47 | Legion of Anubis", "weapon_ak47", "AK-47", WeaponCategory.RIFLES, TeamSide.T, "Covert", SkinColor.GOLD, SkinColor.YELLOW,
                        "https://community.cloudflare.steamstatic.com/economy/image/-9a81dlWLwJ2UUGcVs_nsVtzdOEdtWwKGZZLQHTxDZ7I56KU0Zwwo4NUX4oFJZEHLbXH5ApeO4YmlhxYQknCRvCo04DEVlxkKgpot7HxfDhnwMzJemkV09m_gZKKqOD4NrXXm35Q54kjiLqYrNuh0QHg-hA_MTihco7BclI5aV-F8gC6wunqhMO9vZjJzXMw6CRwt3iOm0GpwUYbLd-cOVo/360fx360f",
                        0.00, 0.70, bd(58.00), bd(24.00), bd(14.00), bd(12.00), bd(9.80)),

                createSkin("skin-ak47-neon-rider", "AK-47 | Neon Rider", "weapon_ak47", "AK-47", WeaponCategory.RIFLES, TeamSide.T, "Covert", SkinColor.PINK, SkinColor.PURPLE,
                        "https://community.cloudflare.steamstatic.com/economy/image/-9a81dlWLwJ2UUGcVs_nsVtzdOEdtWwKGZZLQHTxDZ7I56KU0Zwwo4NUX4oFJZEHLbXH5ApeO4YmlhxYQknCRvCo04DEVlxkKgpot7HxfDhjxszJemkV08-5lpKKqPrxN7LEm1Rd6dd2j6eT9NinjQTn_0JlZGD6coCRclVqYAvVqVO3w-7u08Dv6sjNynJivSUh53iOmhTkn1gSOSI-8J0f/360fx360f",
                        0.00, 0.80, bd(190.00), bd(75.00), bd(40.00), bd(32.00), bd(25.00)),

                createSkin("skin-ak47-nightwish", "AK-47 | Nightwish", "weapon_ak47", "AK-47", WeaponCategory.RIFLES, TeamSide.T, "Covert", SkinColor.PURPLE, SkinColor.BLUE,
                        "https://community.cloudflare.steamstatic.com/economy/image/-9a81dlWLwJ2UUGcVs_nsVtzdOEdtWwKGZZLQHTxDZ7I56KU0Zwwo4NUX4oFJZEHLbXH5ApeO4YmlhxYQknCRvCo04DEVlxkKgpot7HxfDhnwMzJemkV08-jh5KGqPrxN7LEmyUEu8cpi-vD8Ij2iwfn_kQ-NTr2cIGdJwE2ZQ7XqFS6kOzsgJ7u7snOwSQyvScmtyqPyUG0hBlMbvdvgeveFwsU74F41A/360fx360f",
                        0.00, 1.00, bd(120.00), bd(45.00), bd(22.00), bd(18.00), bd(14.50)),

                createSkin("skin-ak47-hydroponic", "AK-47 | Hydroponic", "weapon_ak47", "AK-47", WeaponCategory.RIFLES, TeamSide.T, "Classified", SkinColor.GREEN, SkinColor.WHITE,
                        "https://community.cloudflare.steamstatic.com/economy/image/-9a81dlWLwJ2UUGcVs_nsVtzdOEdtWwKGZZLQHTxDZ7I56KU0Zwwo4NUX4oFJZEHLbXH5ApeO4YmlhxYQknCRvCo04DEVlxkKgpot7HxfDhjxszJemkV09u5lpKKqPrxN7LEmyUJ6Zx0j7yVoNnw0Qey_0VramiiIYCVdwNqZwzU_FW5k-y71MPtv5nMziM3unN05XjcyUepwUYbY1sIka0/360fx360f",
                        0.00, 0.80, bd(1850.00), bd(1200.00), bd(850.00), bd(650.00), bd(550.00)),

                // M4A1-S
                createSkin("skin-m4a1s-printstream", "M4A1-S | Printstream", "weapon_m4a1_silencer", "M4A1-S", WeaponCategory.RIFLES, TeamSide.CT, "Covert", SkinColor.WHITE, SkinColor.BLACK,
                        "https://community.cloudflare.steamstatic.com/economy/image/-9a81dlWLwJ2UUGcVs_nsVtzdOEdtWwKGZZLQHTxDZ7I56KU0Zwwo4NUX4oFJZEHLbXH5ApeO4YmlhxYQknCRvCo04DEVlxkKgpou-6kejhz2v_Nfz5H_uO1gb-Gw_alIITBhGJf_NZlmOzA-LP5gVO8v11sYm2hLITDIAc9NV-G8wW-x-7u0JftvJzIzSRhvHEn7SzcmxG1gh1SLrs4-4b5314/360fx360f",
                        0.00, 0.80, bd(450.00), bd(220.00), bd(140.00), bd(110.00), bd(90.00)),

                createSkin("skin-m4a1s-hyper-beast", "M4A1-S | Hyper Beast", "weapon_m4a1_silencer", "M4A1-S", WeaponCategory.RIFLES, TeamSide.CT, "Covert", SkinColor.PURPLE, SkinColor.RED,
                        "https://community.cloudflare.steamstatic.com/economy/image/-9a81dlWLwJ2UUGcVs_nsVtzdOEdtWwKGZZLQHTxDZ7I56KU0Zwwo4NUX4oFJZEHLbXH5ApeO4YmlhxYQknCRvCo04DEVlxkKgpou-6kejhz2v_Nfz5H_uO1gb-Gw_alIITBhGJf_NZlmOzA-LP4jVC9vh5yZWGhcoCTJwdrZAnSqVm7ye-6hMS0vMidyCBgviUj4HqMnwv3309aLwW-8Q/360fx360f",
                        0.00, 1.00, bd(210.00), bd(75.00), bd(30.00), bd(25.00), bd(19.50)),

                createSkin("skin-m4a1s-cyrex", "M4A1-S | Cyrex", "weapon_m4a1_silencer", "M4A1-S", WeaponCategory.RIFLES, TeamSide.CT, "Covert", SkinColor.RED, SkinColor.WHITE,
                        "https://community.cloudflare.steamstatic.com/economy/image/-9a81dlWLwJ2UUGcVs_nsVtzdOEdtWwKGZZLQHTxDZ7I56KU0Zwwo4NUX4oFJZEHLbXH5ApeO4YmlhxYQknCRvCo04DEVlxkKgpou-6kejhz2v_Nfz5H_uO1gb-Gw_alIITBhGJf_NZlmOzA-LP5jVew_ERkYmD2cI-TdlU3aF3W_wC3wO_qg5W_u5icn3A27D5iuyj6u-9y7w/360fx360f",
                        0.00, 0.50, bd(55.00), bd(32.00), bd(24.00), bd(22.00), bd(20.00)),

                createSkin("skin-m4a1s-blue-phosphor", "M4A1-S | Blue Phosphor", "weapon_m4a1_silencer", "M4A1-S", WeaponCategory.RIFLES, TeamSide.CT, "Classified", SkinColor.BLUE, null,
                        "https://community.cloudflare.steamstatic.com/economy/image/-9a81dlWLwJ2UUGcVs_nsVtzdOEdtWwKGZZLQHTxDZ7I56KU0Zwwo4NUX4oFJZEHLbXH5ApeO4YmlhxYQknCRvCo04DEVlxkKgpou-6kejhz2v_Nfz5H_uO1gb-Gw_alIITBhGJf_NZlmOzA-LP4jVC9vh5yYm-hctDEdAM6YwuEqVK9wObu0MTq7cnOwSZhuil24i7cnR3hgElIO7M8haeeUAYL7tB5yL6S/360fx360f",
                        0.00, 0.08, bd(850.00), bd(780.00), null, null, null),

                // M4A4
                createSkin("skin-m4a4-the-emperor", "M4A4 | The Emperor", "weapon_m4a4", "M4A4", WeaponCategory.RIFLES, TeamSide.CT, "Covert", SkinColor.BLUE, SkinColor.GOLD,
                        "https://community.cloudflare.steamstatic.com/economy/image/-9a81dlWLwJ2UUGcVs_nsVtzdOEdtWwKGZZLQHTxDZ7I56KU0Zwwo4NUX4oFJZEHLbXH5ApeO4YmlhxYQknCRvCo04DEVlxkKgpou-6kejhnwMzFJTwW09--m5CbkuXLNLPehX9u5cB1g_zMu9qjiwayqEVuamnwJY7BcAc3Z1jX-AW2w7y8hpbou5-cwHcxuiV253jfnh3jhh8faLE6m7XAHgL3H4k3/360fx360f",
                        0.00, 0.80, bd(180.00), bd(70.00), bd(25.00), bd(20.00), bd(15.00)),

                createSkin("skin-m4a4-desolate-space", "M4A4 | Desolate Space", "weapon_m4a4", "M4A4", WeaponCategory.RIFLES, TeamSide.CT, "Classified", SkinColor.PURPLE, SkinColor.BLUE,
                        "https://community.cloudflare.steamstatic.com/economy/image/-9a81dlWLwJ2UUGcVs_nsVtzdOEdtWwKGZZLQHTxDZ7I56KU0Zwwo4NUX4oFJZEHLbXH5ApeO4YmlhxYQknCRvCo04DEVlxkKgpou-6kejhnwMzFJTwW08-zl5SEhcj5Nr_Yg2YfupQh2L6V892s0VXsr0NoZG2icNWUdANrN1nS_1m-xO7u1JXovsnJznRlsyUmsXmIlkS01RlIcKUx0lQ6q2bN/360fx360f",
                        0.00, 1.00, bd(38.00), bd(20.00), bd(12.00), bd(10.00), bd(8.20)),

                createSkin("skin-m4a4-howl", "M4A4 | Howl", "weapon_m4a4", "M4A4", WeaponCategory.RIFLES, TeamSide.CT, "Contraband", SkinColor.RED, SkinColor.ORANGE,
                        "https://community.cloudflare.steamstatic.com/economy/image/-9a81dlWLwJ2UUGcVs_nsVtzdOEdtWwKGZZLQHTxDZ7I56KU0Zwwo4NUX4oFJZEHLbXH5ApeO4YmlhxYQknCRvCo04DEVlxkKgpou-6kejhnwMzFJTwW09m7hIWZmOXLP7LWnn8f65pyjOrAotmg2Vfj_kdoYmmnco_EcFI_YQnU-gC5kO-5hMW0vpvMwSBn6CR2-z-DyG2z4rEa/360fx360f",
                        0.00, 0.40, bd(6500.00), bd(4800.00), bd(3800.00), bd(3200.00), null),

                // AWP
                createSkin("skin-awp-asiimov", "AWP | Asiimov", "weapon_awp", "AWP", WeaponCategory.RIFLES, TeamSide.BOTH, "Covert", SkinColor.ORANGE, SkinColor.WHITE,
                        "https://community.cloudflare.steamstatic.com/economy/image/-9a81dlWLwJ2UUGcVs_nsVtzdOEdtWwKGZZLQHTxDZ7I56KU0Zwwo4NUX4oFJZEHLbXH5ApeO4YmlhxYQknCRvCo04DEVlxkKgpot621FABz7PLfYQJG6d2inL-GkvP9Jrafw2lU6ccp0rqVpon0jVW2-EQ4amzzLI-QdwFvNVrZ_wK-kO_q1se_vsmbySRj6T5iuyil1_7d6Q/360fx360f",
                        0.18, 1.00, null, null, bd(135.00), bd(105.00), bd(85.00)),

                createSkin("skin-awp-neo-noir", "AWP | Neo-Noir", "weapon_awp", "AWP", WeaponCategory.RIFLES, TeamSide.BOTH, "Covert", SkinColor.PURPLE, SkinColor.PINK,
                        "https://community.cloudflare.steamstatic.com/economy/image/-9a81dlWLwJ2UUGcVs_nsVtzdOEdtWwKGZZLQHTxDZ7I56KU0Zwwo4NUX4oFJZEHLbXH5ApeO4YmlhxYQknCRvCo04DEVlxkKgpot621FABz7PLfYQJS5NO0m5O0m_7zO6-fzj9V7cAl2eyVpIrz2FKx8kZlam-icNSScVI8M12DqFS8k-fs0cW76sifz3Jk7yQi4HndzAv330-Wz700Lw/360fx360f",
                        0.00, 0.70, bd(45.00), bd(32.00), bd(24.00), bd(22.00), bd(20.00)),

                createSkin("skin-awp-atheris", "AWP | Atheris", "weapon_awp", "AWP", WeaponCategory.RIFLES, TeamSide.BOTH, "Restricted", SkinColor.GREEN, SkinColor.BLACK,
                        "https://community.cloudflare.steamstatic.com/economy/image/-9a81dlWLwJ2UUGcVs_nsVtzdOEdtWwKGZZLQHTxDZ7I56KU0Zwwo4NUX4oFJZEHLbXH5ApeO4YmlhxYQknCRvCo04DEVlxkKgpot621FABz7PLfYQJG-NNlhoaOmvPLPr7Vn35cpsB03-3D992n2wK3qBBuMG_ycdCRc1c2YF6E_wTvl-e-18W5tZuaynpi6yEr4CnemB3kgRhPbeM81qadSVicA6RLVq0/360fx360f",
                        0.00, 1.00, bd(18.00), bd(8.00), bd(4.50), bd(3.80), bd(3.20)),

                createSkin("skin-awp-dragon-lore", "AWP | Dragon Lore", "weapon_awp", "AWP", WeaponCategory.RIFLES, TeamSide.BOTH, "Covert", SkinColor.GOLD, SkinColor.YELLOW,
                        "https://community.cloudflare.steamstatic.com/economy/image/-9a81dlWLwJ2UUGcVs_nsVtzdOEdtWwKGZZLQHTxDZ7I56KU0Zwwo4NUX4oFJZEHLbXH5ApeO4YmlhxYQknCRvCo04DEVlxkKgpot621FABz7PLfYQJS5NO0m5O0m_7zO6-fk29T-sBzpezE9NWgiw23rkRtZ2zwcoTAJwE6ZFzZ_gLvkOi-gJW57svLzidluSRwtH3dy0DmgBhPLrs4dZt1m6o/360fx360f",
                        0.00, 0.70, bd(12000.00), bd(8500.00), bd(5500.00), bd(4000.00), bd(3200.00)),

                createSkin("skin-awp-wildfire", "AWP | Wildfire", "weapon_awp", "AWP", WeaponCategory.RIFLES, TeamSide.BOTH, "Covert", SkinColor.RED, SkinColor.YELLOW,
                        "https://community.cloudflare.steamstatic.com/economy/image/-9a81dlWLwJ2UUGcVs_nsVtzdOEdtWwKGZZLQHTxDZ7I56KU0Zwwo4NUX4oFJZEHLbXH5ApeO4YmlhxYQknCRvCo04DEVlxkKgpot621FABz7PLfYQJG6d2inL-GkvP9Jrafw2lU6ccp076UpY6k3AW3_0E-a2zwLIbBcwBoZAzV_AK_lO_vgpC7uM6YnCdj6T5iuyigXU9qCg/360fx360f",
                        0.01, 0.70, bd(180.00), bd(105.00), bd(60.00), bd(50.00), bd(42.00)),

                // USP-S
                createSkin("skin-usps-kill-confirmed", "USP-S | Kill Confirmed", "weapon_usp_silencer", "USP-S", WeaponCategory.PISTOLS, TeamSide.CT, "Covert", SkinColor.RED, null,
                        "https://community.cloudflare.steamstatic.com/economy/image/-9a81dlWLwJ2UUGcVs_nsVtzdOEdtWwKGZZLQHTxDZ7I56KU0Zwwo4NUX4oFJZEHLbXH5ApeO4YmlhxYQknCRvCo04DEVlxkKgpoo6m1FBRp3_bGcjhQ09-jq5WYh8j3KqnUjlRd4cJ5nqeWp97x3Qfk-Us_NWzwLNPHewVqY1uC-1ftwLzs05C8uprIyXZivCMr-z-DyG65W1jV/360fx360f",
                        0.00, 1.00, bd(240.00), bd(120.00), bd(65.00), bd(50.00), bd(42.00)),

                createSkin("skin-usps-cortex", "USP-S | Cortex", "weapon_usp_silencer", "USP-S", WeaponCategory.PISTOLS, TeamSide.CT, "Classified", SkinColor.PINK, SkinColor.WHITE,
                        "https://community.cloudflare.steamstatic.com/economy/image/-9a81dlWLwJ2UUGcVs_nsVtzdOEdtWwKGZZLQHTxDZ7I56KU0Zwwo4NUX4oFJZEHLbXH5ApeO4YmlhxYQknCRvCo04DEVlxkKgpoo6m1FBRp3_bGcjhQ09-jq5WYh8j3KqnUjlRc7cF4n-SPpdum2wXsr0A-ZWuhLYXAegY2YV2D-gC2xua90JS4u5qfzXs26SEh4yyMnAv3309a9_mrhQ/360fx360f",
                        0.00, 1.00, bd(16.00), bd(9.00), bd(5.50), bd(4.50), bd(4.00)),

                createSkin("skin-usps-ticket-to-hell", "USP-S | Ticket to Hell", "weapon_usp_silencer", "USP-S", WeaponCategory.PISTOLS, TeamSide.CT, "Restricted", SkinColor.WHITE, SkinColor.BLACK,
                        "https://community.cloudflare.steamstatic.com/economy/image/-9a81dlWLwJ2UUGcVs_nsVtzdOEdtWwKGZZLQHTxDZ7I56KU0Zwwo4NUX4oFJZEHLbXH5ApeO4YmlhxYQknCRvCo04DEVlxkKgpoo6m1FBRp3_bGcjhQ09-jq5WYh8j_OrfcZgBSu5Vz3-vDrN6t2wPl_kZsY2D3LITDJ1A2NAvTrFm8wrq7hMe6vZ2cnXFlsyI8pGB8ssiX2sR2/360fx360f",
                        0.00, 0.76, bd(6.00), bd(2.50), bd(1.20), bd(1.00), bd(0.90)),

                // Glock-18
                createSkin("skin-glock-water-elemental", "Glock-18 | Water Elemental", "weapon_glock", "Glock-18", WeaponCategory.PISTOLS, TeamSide.T, "Classified", SkinColor.RED, SkinColor.BLUE,
                        "https://community.cloudflare.steamstatic.com/economy/image/-9a81dlWLwJ2UUGcVs_nsVtzdOEdtWwKGZZLQHTxDZ7I56KU0Zwwo4NUX4oFJZEHLbXH5ApeO4YmlhxYQknCRvCo04DEVlxkKgposbaqKAxf0v73djxP4c-6wYGZnvnxDLbUkmJE5Ytzi77DrNmk2Ffn_BA6ZW2nJYPAclVvYFHW_gC6wevu1MW6tZvBznJm7yVw4CnamhTkhhhPbeM9m7XAHreX7QO4/360fx360f",
                        0.00, 0.70, bd(16.00), bd(11.00), bd(7.50), bd(7.00), bd(6.50)),

                createSkin("skin-glock-bullet-queen", "Glock-18 | Bullet Queen", "weapon_glock", "Glock-18", WeaponCategory.PISTOLS, TeamSide.T, "Covert", SkinColor.YELLOW, SkinColor.PINK,
                        "https://community.cloudflare.steamstatic.com/economy/image/-9a81dlWLwJ2UUGcVs_nsVtzdOEdtWwKGZZLQHTxDZ7I56KU0Zwwo4NUX4oFJZEHLbXH5ApeO4YmlhxYQknCRvCo04DEVlxkKgposbaqKAxf0v73dS9D6ti_l4GZnvnxDLbUkmJE5Ytwj-vD8Ij2iwfn_kRtZ2qmIdTGIVJtYV3V_1K6xejugpTouZucznsw7yMn5S6IzkXmhBgeO7ZqhaadTVicA6SIVq0/360fx360f",
                        0.00, 1.00, bd(70.00), bd(28.00), bd(12.00), bd(9.00), bd(7.00)),

                // Desert Eagle
                createSkin("skin-deagle-printstream", "Desert Eagle | Printstream", "weapon_deagle", "Desert Eagle", WeaponCategory.PISTOLS, TeamSide.BOTH, "Covert", SkinColor.WHITE, SkinColor.BLACK,
                        "https://community.cloudflare.steamstatic.com/economy/image/-9a81dlWLwJ2UUGcVs_nsVtzdOEdtWwKGZZLQHTxDZ7I56KU0Zwwo4NUX4oFJZEHLbXH5ApeO4YmlhxYQknCRvCo04DEVlxkKgposr-kLAtl7PDdTjlH_8-1n4OGluP3Jqjulk5Vv8Z0i-zA-LP4jVC9vh5yZG71d47BIQVsYAvZrwTtlOi9h5Du75zKyic1vXYg4X3bzRfingwXOx8F_uP7/360fx360f",
                        0.00, 0.80, bd(135.00), bd(75.00), bd(48.00), bd(42.00), bd(38.00)),

                createSkin("skin-deagle-code-red", "Desert Eagle | Code Red", "weapon_deagle", "Desert Eagle", WeaponCategory.PISTOLS, TeamSide.BOTH, "Covert", SkinColor.RED, SkinColor.WHITE,
                        "https://community.cloudflare.steamstatic.com/economy/image/-9a81dlWLwJ2UUGcVs_nsVtzdOEdtWwKGZZLQHTxDZ7I56KU0Zwwo4NUX4oFJZEHLbXH5ApeO4YmlhxYQknCRvCo04DEVlxkKgposr-kLAtl7PDdTjlH_9mkgL-OlvD4NoTSmXVO_Pp9i_vG8DW22Vfs-kA_NW_6cNDBIw84MVzW_VS3yL3o056_6pnNn3Njsydw5HzanUa-0AYMMLKbhx58wQ/360fx360f",
                        0.00, 1.00, bd(85.00), bd(45.00), bd(28.00), bd(24.00), bd(20.00)),

                createSkin("skin-deagle-ocean-drive", "Desert Eagle | Ocean Drive", "weapon_deagle", "Desert Eagle", WeaponCategory.PISTOLS, TeamSide.BOTH, "Covert", SkinColor.ORANGE, SkinColor.BLUE,
                        "https://community.cloudflare.steamstatic.com/economy/image/-9a81dlWLwJ2UUGcVs_nsVtzdOEdtWwKGZZLQHTxDZ7I56KU0Zwwo4NUX4oFJZEHLbXH5ApeO4YmlhxYQknCRvCo04DEVlxkKgposr-kLAtl7PDdTjlH_9mkgL-OlvD4NoTSmXVO_Pp9i_vG8DW22Ffk_0ZsYm2icNCRcANqZwvY_FO9kLi5hsW6756fyCRk7yQg42GdwUL31-yXKg/360fx360f",
                        0.00, 1.00, bd(110.00), bd(48.00), bd(24.00), bd(18.00), bd(15.00)),

                // SMGs
                createSkin("skin-mp9-starlight-protector", "MP9 | Starlight Protector", "weapon_mp9", "MP9", WeaponCategory.SMGS, TeamSide.CT, "Covert", SkinColor.WHITE, SkinColor.GOLD,
                        "https://community.cloudflare.steamstatic.com/economy/image/-9a81dlWLwJ2UUGcVs_nsVtzdOEdtWwKGZZLQHTxDZ7I56KU0Zwwo4NUX4oFJZEHLbXH5ApeO4YmlhxYQknCRvCo04DEVlxkKgpou6r8FABz7P7fYQJB49K_moWfqPv9NLPF2D5V68coiO-UotTxiwyxqUptZGGlLY_AdwVqZ12B8wW8k-q5hpS9v5yYnHMwsyVxsnePnwv3308kR3c3tA/360fx360f",
                        0.00, 0.80, bd(35.00), bd(16.00), bd(8.50), bd(7.00), bd(6.00)),

                createSkin("skin-mac10-disco-tech", "MAC-10 | Disco Tech", "weapon_mac10", "MAC-10", WeaponCategory.SMGS, TeamSide.T, "Classified", SkinColor.PURPLE, SkinColor.BLUE,
                        "https://community.cloudflare.steamstatic.com/economy/image/-9a81dlWLwJ2UUGcVs_nsVtzdOEdtWwKGZZLQHTxDZ7I56KU0Zwwo4NUX4oFJZEHLbXH5ApeO4YmlhxYQknCRvCo04DEVlxkKgpou7umeldf0v73dS9D6ti_l4GZnvnxDLbUkmJE5Ytzi77DrNuh2AXk_hdoMj3ycNSddlRtMV2B-AC_wuvv15W87pnPzXJivyh0snyPlhfk1B1PbeM817jJHwXIA7ZSVq0/360fx360f",
                        0.00, 1.00, bd(20.00), bd(8.50), bd(4.20), bd(3.50), bd(3.00)),

                // Knives & Gloves
                createSkin("skin-knife-karambit-doppler", "Karambit | Doppler", "weapon_knife_karambit", "Karambit", WeaponCategory.KNIVES, TeamSide.BOTH, "Covert", SkinColor.PURPLE, SkinColor.PINK,
                        "https://community.cloudflare.steamstatic.com/economy/image/-9a81dlWLwJ2UUGcVs_nsVtzdOEdtWwKGZZLQHTxDZ7I56KU0Zwwo4NUX4oFJZEHLbXH5ApeO4YmlhxYQknCRvCo04DEVlxkKgpovbSsLQJf2PLacDBA5ciJlY20hPbkI7PYhG5u5cB1g_zMu9mi3Vaw-0duNWv6I9SQelc8ZV-CrgK-l-bv0Me0vZmbz3U16XVz-z-DyPrtwE-t/360fx360f",
                        0.00, 0.08, bd(1600.00), bd(1400.00), null, null, null),

                createSkin("skin-knife-butterfly-fade", "Butterfly Knife | Fade", "weapon_knife_butterfly", "Butterfly Knife", WeaponCategory.KNIVES, TeamSide.BOTH, "Covert", SkinColor.YELLOW, SkinColor.PURPLE,
                        "https://community.cloudflare.steamstatic.com/economy/image/-9a81dlWLwJ2UUGcVs_nsVtzdOEdtWwKGZZLQHTxDZ7I56KU0Zwwo4NUX4oFJZEHLbXH5ApeO4YmlhxYQknCRvCo04DEVlxkKgpovbSsLQJf0ebcZThQ6tCvq4GGqPr1Ibndk1RX6cF0teXI8oThxlKwqkc4NTqldtSdewU9aVqF8lDoxu25gpS1vMydzXFmuScg43rVnhTh0h8fP_sv26KiV6n7Rw/360fx360f",
                        0.00, 0.08, bd(3200.00), bd(2800.00), null, null, null),

                createSkin("skin-gloves-sport-vice", "Sport Gloves | Vice", "weapon_gloves_sport", "Sport Gloves", WeaponCategory.GLOVES, TeamSide.BOTH, "Extraordinary", SkinColor.PINK, SkinColor.BLUE,
                        "https://community.cloudflare.steamstatic.com/economy/image/-9a81dlWLwJ2UUGcVs_nsVtzdOEdtWwKGZZLQHTxDZ7I56KU0Zwwo4NUX4oFJZEHLbXH5ApeO4YmlhxYQknCRvCo04DEVlxkKgpovbSsLQJf0ebcZThQ6tCvq4GGqPr1Ibndk1RX6cF0teXI8oThxlKwqkc4NTqldtSdewU9aVqF8lDoxu25gpS1vMydzXFmuScg43rVnhTh0h8fP_sv26KiV6n7Rw/360fx360f",
                        0.06, 0.80, bd(18000.00), bd(6500.00), bd(2200.00), bd(1400.00), bd(900.00))
        );
    }

    public List<StickerEntity> buildCuratedStickers() {
        return List.of(
                createSticker("sticker-titan-holo-katowice-2014", "Sticker | Titan (Holo) | Katowice 2014", "Exotic", SkinColor.BLUE,
                        "https://community.cloudflare.steamstatic.com/economy/image/-9a81dlWLwJ2UUGcVs_nsVtzdOEdtWwKGZZLQHTxDZ7I56KU0Zwwo4NUX4oFJZEHLbXQ9QVcJY8gulReQ0DFROWm3pnQRl1yIAhTtq2sLAZkw_r3eTRGuN23l4HYzqf1Zr3T2DsEupQmiO3E8N-j3FXl_kRtNzzyJIGUcQE4ZFiDqVntk-rvjZS1656fnXRhvCBw5WGdwUImL9p5YQ/360fx360f",
                        bd(55000.00)),

                createSticker("sticker-ibuypower-holo-katowice-2014", "Sticker | iBUYPOWER (Holo) | Katowice 2014", "Exotic", SkinColor.RED,
                        "https://community.cloudflare.steamstatic.com/economy/image/-9a81dlWLwJ2UUGcVs_nsVtzdOEdtWwKGZZLQHTxDZ7I56KU0Zwwo4NUX4oFJZEHLbXQ9QVcJY8gulReQ0DFROWm3pnQRl1yIAhTtq2sLAZkw_r3eTFGuN23l4HYzqf1Zr3T2DsEupQm3-_H8N_23FXl_kRtNzzyJoKVIQdsNFqE-QO_k-ntjcPt6Z2bn3MxuXFw5WGdwULyN49cbg/360fx360f",
                        bd(65000.00)),

                createSticker("sticker-reason-gaming-holo-katowice-2014", "Sticker | Reason Gaming (Holo) | Katowice 2014", "Exotic", SkinColor.ORANGE,
                        "https://community.cloudflare.steamstatic.com/economy/image/-9a81dlWLwJ2UUGcVs_nsVtzdOEdtWwKGZZLQHTxDZ7I56KU0Zwwo4NUX4oFJZEHLbXQ9QVcJY8gulReQ0DFROWm3pnQRl1yIAhTtq2sLAZkw_r3eTJGuN23l4HYzqf1Zr3T2DsEupQmiO3E8N-j3FXl_kRtNzzyJoGVcwQ_NFyC_gK_wOntjcC46pqcnXM1vXdw5WGdwUL7_p1_ZQ/360fx360f",
                        bd(48000.00)),

                createSticker("sticker-vox-eminor-holo-katowice-2014", "Sticker | Vox Eminor (Holo) | Katowice 2014", "Exotic", SkinColor.YELLOW,
                        "https://community.cloudflare.steamstatic.com/economy/image/-9a81dlWLwJ2UUGcVs_nsVtzdOEdtWwKGZZLQHTxDZ7I56KU0Zwwo4NUX4oFJZEHLbXQ9QVcJY8gulReQ0DFROWm3pnQRl1yIAhTtq2sLAZkw_r3eTJGuN23l4HYzqf1Zr3T2DsEupQm3-3E9N2s2QflqENramnyd9ecclU7ZlGDq1S_leztj8PvucubmHFnsid3-z-DyP2hP1xG/360fx360f",
                        bd(35000.00)),

                createSticker("sticker-team-dignitas-holo-katowice-2014", "Sticker | Team Dignitas (Holo) | Katowice 2014", "Exotic", SkinColor.YELLOW,
                        "https://community.cloudflare.steamstatic.com/economy/image/-9a81dlWLwJ2UUGcVs_nsVtzdOEdtWwKGZZLQHTxDZ7I56KU0Zwwo4NUX4oFJZEHLbXQ9QVcJY8gulReQ0DFROWm3pnQRl1yIAhTtq2sLAZkw_r3eTRGuN23l4HYzqf1Zr3T2DsEupQmiO3E8N-j3FXl_kRtNzzyI9OUdA43YAnX-QTvxezvgpK-tZrPzHY2vCFw-z-DyAGp1qM2/360fx360f",
                        bd(22000.00)),

                createSticker("sticker-cloud9-g2a-holo-katowice-2015", "Sticker | Cloud9 G2A (Holo) | Katowice 2015", "Remarkable", SkinColor.BLUE,
                        "https://community.cloudflare.steamstatic.com/economy/image/-9a81dlWLwJ2UUGcVs_nsVtzdOEdtWwKGZZLQHTxDZ7I56KU0Zwwo4NUX4oFJZEHLbXQ9QVcJY8gulReQ0DFROWm3pnQRl1yIAhTsb-kLRVw3_n3YzxB6N24lYTZk_P1au6FlD0JvcB32r7DoY-hjATn-ENkZjzzdoXDew85NVzVrFHtxr29jce0u5zMm3Rk7yNw-z-DyO4x63_0/360fx360f",
                        bd(950.00)),

                createSticker("sticker-crown-foil", "Sticker | Crown (Foil)", "Exotic", SkinColor.GOLD,
                        "https://community.cloudflare.steamstatic.com/economy/image/-9a81dlWLwJ2UUGcVs_nsVtzdOEdtWwKGZZLQHTxDZ7I56KU0Zwwo4NUX4oFJZEHLbXQ9QVcJY8gulReQ0DFROWm3pnQRl1yIAhTtb-iKgZn7PvBfS5D4Nm1kb-GkvP9JrbummpT38hnieb--InxgUG55RFtZWv7J9TAIQVvZlmF8wTrlbzp18Pt7smfmHFruyAisHjbnRCwgxpSLrs45xU07h8/360fx360f",
                        bd(850.00)),

                createSticker("sticker-howling-dawn", "Sticker | Howling Dawn", "Contraband", SkinColor.RED,
                        "https://community.cloudflare.steamstatic.com/economy/image/-9a81dlWLwJ2UUGcVs_nsVtzdOEdtWwKGZZLQHTxDZ7I56KU0Zwwo4NUX4oFJZEHLbXQ9QVcJY8gulReQ0DFROWm3pnQRl1yIAhTtLutKBNi7_nPezxH4-WzmL-Kk_fnDLfYkW5u5cB1g_zMu4r22wKy-kBqNW7xLIWXcgU9ZQ2G8wK5l-3n1pfuvJvMn3NhvXY8pGB8sD87D3Y/360fx360f",
                        bd(1450.00)),

                createSticker("sticker-flipsid3-holo-katowice-2015", "Sticker | FlipSid3 Tactics (Holo) | Katowice 2015", "Remarkable", SkinColor.GREEN,
                        "https://community.cloudflare.steamstatic.com/economy/image/-9a81dlWLwJ2UUGcVs_nsVtzdOEdtWwKGZZLQHTxDZ7I56KU0Zwwo4NUX4oFJZEHLbXQ9QVcJY8gulReQ0DFROWm3pnQRl1yIAhTsb-kLRVw3_n3eTRGuN23l4HYzqf1Zr3T2DsEupQm3u3E8N-j3FXl_kRtNzzyI4XEcAZsY13SqFPtkOruhpe16s7IzXM26XM8pGB8suw6y9M/360fx360f",
                        bd(420.00)),

                createSticker("sticker-team-liquid-foil-mlg-columbus-2016", "Sticker | Team Liquid (Foil) | MLG Columbus 2016", "Exotic", SkinColor.BLUE,
                        "https://community.cloudflare.steamstatic.com/economy/image/-9a81dlWLwJ2UUGcVs_nsVtzdOEdtWwKGZZLQHTxDZ7I56KU0Zwwo4NUX4oFJZEHLbXQ9QVcJY8gulReQ0DFROWm3pnQRl1yIAhTs7WkLAl03vvfcTBB7d2_k4TYxPWlZbqHzm5T7scpi-zA-LP4jVC9vh5yZmqid4_GdlVsMl7V_FK_x-ntg5TouJ6cmHEyvHUmtiqIykbh1R5IarM6m7XAHv524sB2/360fx360f",
                        bd(180.00))
        );
    }

    private SkinEntity createSkin(
            String id, String name, String weaponId, String weaponName,
            WeaponCategory category, TeamSide teamSide, String rarity,
            SkinColor primaryColor, SkinColor secondaryColor,
            String imageUrl, double minFloat, double maxFloat,
            BigDecimal fn, BigDecimal mw, BigDecimal ft, BigDecimal ww, BigDecimal bs
    ) {
        SkinEntity skin = SkinEntity.builder()
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
        skin.computeLowestPriceAndTimestamp();
        return skin;
    }

    private StickerEntity createSticker(
            String id, String name, String rarity, SkinColor primaryColor, String imageUrl, BigDecimal price
    ) {
        return StickerEntity.builder()
                .id(id)
                .name(name)
                .rarity(rarity)
                .primaryColor(primaryColor)
                .imageUrl(imageUrl)
                .priceUsd(price)
                .updatedAt(Instant.now())
                .build();
    }

    private BigDecimal bd(double val) {
        return BigDecimal.valueOf(val).setScale(2, java.math.RoundingMode.HALF_UP);
    }
}
