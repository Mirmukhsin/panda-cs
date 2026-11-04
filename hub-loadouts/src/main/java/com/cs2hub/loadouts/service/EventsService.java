package com.cs2hub.loadouts.service;

import com.cs2hub.common.exception.ResourceNotFoundException;
import com.cs2hub.loadouts.dto.DiscountDealDto;
import com.cs2hub.loadouts.dto.EnterGiveawayResponseDto;
import com.cs2hub.loadouts.dto.EventsHubResponseDto;
import com.cs2hub.loadouts.dto.GiveawayDto;
import com.cs2hub.loadouts.dto.SpecialEventDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Service
public class EventsService {

    // Atomic tracking of giveaway entries
    private final Map<String, AtomicInteger> giveawayCounts = new ConcurrentHashMap<>();
    private final Map<String, Set<String>> giveawayParticipants = new ConcurrentHashMap<>();

    // Static templates for giveaways
    private final Map<String, GiveawayTemplate> giveawayTemplates = new ConcurrentHashMap<>();

    private record GiveawayTemplate(
            String id,
            String title,
            BigDecimal valueUsd,
            String rarity,
            String imageUrl,
            Instant endsAt,
            int initialEntries
    ) {}

    public EventsService() {
        initGiveaways();
    }

    private void initGiveaways() {
        Instant now = Instant.now();
        registerGiveaway("giveaway-ak47-bloodsport", "AK-47 | Bloodsport (Factory New)",
                BigDecimal.valueOf(145.00), "Covert",
                "https://raw.githubusercontent.com/ByMykel/CSGO-API/main/public/images/skins/skin-ak47-bloodsport.png",
                now.plus(Duration.ofDays(3)), 1240);

        registerGiveaway("giveaway-awp-asiimov", "AWP | Asiimov (Field-Tested)",
                BigDecimal.valueOf(135.00), "Covert",
                "https://raw.githubusercontent.com/ByMykel/CSGO-API/main/public/images/skins/skin-awp-asiimov.png",
                now.plus(Duration.ofDays(5)), 1890);

        registerGiveaway("giveaway-deagle-printstream", "Desert Eagle | Printstream (Minimal Wear)",
                BigDecimal.valueOf(75.00), "Covert",
                "https://raw.githubusercontent.com/ByMykel/CSGO-API/main/public/images/skins/skin-deagle-printstream.png",
                now.plus(Duration.ofDays(7)), 940);

        registerGiveaway("giveaway-m4a1s-player-two", "M4A1-S | Player Two (Field-Tested)",
                BigDecimal.valueOf(48.00), "Covert",
                "https://raw.githubusercontent.com/ByMykel/CSGO-API/main/public/images/skins/skin-m4a1s-hyper-beast.png",
                now.plus(Duration.ofDays(2)), 620);

        registerGiveaway("giveaway-steam-wallet-50", "$50 Steam Wallet Gift Card",
                BigDecimal.valueOf(50.00), "Extraordinary",
                "https://store.steampowered.com/public/images/giftcards/steamcard_50.png",
                now.plus(Duration.ofDays(10)), 3410);
    }

    private void registerGiveaway(String id, String title, BigDecimal valueUsd, String rarity,
                                  String imageUrl, Instant endsAt, int initialEntries) {
        giveawayTemplates.put(id, new GiveawayTemplate(id, title, valueUsd, rarity, imageUrl, endsAt, initialEntries));
        giveawayCounts.putIfAbsent(id, new AtomicInteger(initialEntries));
        giveawayParticipants.putIfAbsent(id, ConcurrentHashMap.newKeySet());
    }

    public EventsHubResponseDto getEventsHub(String userOrGuestKey) {
        String key = (userOrGuestKey != null && !userOrGuestKey.isBlank()) ? userOrGuestKey : "";

        List<GiveawayDto> giveaways = giveawayTemplates.values().stream()
                .map(t -> {
                    int count = giveawayCounts.getOrDefault(t.id, new AtomicInteger(t.initialEntries)).get();
                    boolean entered = !key.isEmpty() && giveawayParticipants.getOrDefault(t.id, Collections.emptySet()).contains(key);
                    return new GiveawayDto(t.id, t.title, t.valueUsd, t.rarity, t.imageUrl, t.endsAt, count, entered);
                })
                .toList();

        List<DiscountDealDto> discounts = getCuratedDiscounts();
        List<SpecialEventDto> specialEvents = getCuratedSpecialEvents();

        return new EventsHubResponseDto(giveaways, discounts, specialEvents);
    }

    public EnterGiveawayResponseDto enterGiveaway(String giveawayId, String userOrGuestKey) {
        GiveawayTemplate template = giveawayTemplates.get(giveawayId);
        if (template == null) {
            throw new ResourceNotFoundException("Giveaway not found with ID: " + giveawayId);
        }

        String key = (userOrGuestKey != null && !userOrGuestKey.isBlank())
                ? userOrGuestKey
                : "guest-" + System.currentTimeMillis();

        Set<String> participants = giveawayParticipants.computeIfAbsent(giveawayId, k -> ConcurrentHashMap.newKeySet());
        AtomicInteger counter = giveawayCounts.computeIfAbsent(giveawayId, k -> new AtomicInteger(template.initialEntries));

        boolean added = participants.add(key);
        int currentCount;
        if (added) {
            currentCount = counter.incrementAndGet();
            log.info("Participant '{}' entered giveaway '{}'. Total entries: {}", key, giveawayId, currentCount);
            return new EnterGiveawayResponseDto(giveawayId, currentCount, true, "Entry recorded successfully!");
        } else {
            currentCount = counter.get();
            return new EnterGiveawayResponseDto(giveawayId, currentCount, true, "You have already entered this giveaway!");
        }
    }

    private List<DiscountDealDto> getCuratedDiscounts() {
        return List.of(
                new DiscountDealDto("deal-1", "M4A4 | The Emperor (Field-Tested)",
                        BigDecimal.valueOf(25.00), BigDecimal.valueOf(18.75), 25, "Skinport",
                        "https://raw.githubusercontent.com/ByMykel/CSGO-API/main/public/images/skins/skin-m4a4-the-emperor.png",
                        "https://skinport.com/item/cs2/m4a4-the-emperor"),

                new DiscountDealDto("deal-2", "AK-47 | Vulcan (Field-Tested)",
                        BigDecimal.valueOf(280.00), BigDecimal.valueOf(224.00), 20, "Buff163",
                        "https://raw.githubusercontent.com/ByMykel/CSGO-API/main/public/images/skins/skin-ak47-vulcan.png",
                        "https://buff.163.com/goods/ak47-vulcan"),

                new DiscountDealDto("deal-3", "USP-S | Kill Confirmed (Well-Worn)",
                        BigDecimal.valueOf(50.00), BigDecimal.valueOf(37.50), 25, "CS.MONEY",
                        "https://raw.githubusercontent.com/ByMykel/CSGO-API/main/public/images/skins/skin-usps-kill-confirmed.png",
                        "https://cs.money/market/buy/usps-kill-confirmed"),

                new DiscountDealDto("deal-4", "Karambit | Doppler Phase 2 (Factory New)",
                        BigDecimal.valueOf(1600.00), BigDecimal.valueOf(1360.00), 15, "DMarket",
                        "https://raw.githubusercontent.com/ByMykel/CSGO-API/main/public/images/skins/skin-knife-karambit-doppler.png",
                        "https://dmarket.com/ingame-items/item-list/csgo-skins/knife-karambit-doppler"),

                new DiscountDealDto("deal-5", "Sport Gloves | Vice (Field-Tested)",
                        BigDecimal.valueOf(2200.00), BigDecimal.valueOf(1760.00), 20, "Skinport",
                        "https://raw.githubusercontent.com/ByMykel/CSGO-API/main/public/images/skins/skin-gloves-sport-vice.png",
                        "https://skinport.com/item/cs2/sport-gloves-vice"),

                new DiscountDealDto("deal-6", "Butterfly Knife | Fade (Factory New)",
                        BigDecimal.valueOf(3200.00), BigDecimal.valueOf(2720.00), 15, "Buff163",
                        "https://raw.githubusercontent.com/ByMykel/CSGO-API/main/public/images/skins/skin-knife-butterfly-fade.png",
                        "https://buff.163.com/goods/butterfly-knife-fade"),

                new DiscountDealDto("deal-7", "AWP | Neo-Noir (Minimal Wear)",
                        BigDecimal.valueOf(32.00), BigDecimal.valueOf(22.40), 30, "CS.MONEY",
                        "https://raw.githubusercontent.com/ByMykel/CSGO-API/main/public/images/skins/skin-awp-neo-noir.png",
                        "https://cs.money/market/buy/awp-neo-noir"),

                new DiscountDealDto("deal-8", "Desert Eagle | Code Red (Field-Tested)",
                        BigDecimal.valueOf(28.00), BigDecimal.valueOf(19.60), 30, "Skinport",
                        "https://raw.githubusercontent.com/ByMykel/CSGO-API/main/public/images/skins/skin-deagle-code-red.png",
                        "https://skinport.com/item/cs2/desert-eagle-code-red"),

                new DiscountDealDto("deal-9", "Glock-18 | Water Elemental (Factory New)",
                        BigDecimal.valueOf(16.00), BigDecimal.valueOf(11.20), 30, "DMarket",
                        "https://raw.githubusercontent.com/ByMykel/CSGO-API/main/public/images/skins/skin-glock-water-elemental.png",
                        "https://dmarket.com/ingame-items/item-list/csgo-skins/glock-water-elemental")
        );
    }

    private List<SpecialEventDto> getCuratedSpecialEvents() {
        Instant now = Instant.now();
        return List.of(
                new SpecialEventDto("event-shanghai-major",
                        "Perfect World Shanghai Major 2024", "MAJOR",
                        "The pinnacle CS2 championship featuring 24 top global squads battling for Major glory and Pick'Em trophies.",
                        now.minus(Duration.ofDays(10)), now.plus(Duration.ofDays(20)),
                        "$1,250,000 USD", "Shanghai, China",
                        "https://images.unsplash.com/photo-1542751371-adc38448a05e?auto=format&fit=crop&w=1200&q=80",
                        "https://store.steampowered.com/app/730/CounterStrike_2/"),

                new SpecialEventDto("event-the-armory",
                        "The Armory Update & Season Pass", "VALVE_DROP",
                        "Activate your Armory Pass in CS2 to earn Armory Credits, trade for exclusive Charm attachments and Limited Edition skins.",
                        now.minus(Duration.ofDays(30)), now.plus(Duration.ofDays(60)),
                        "Exclusive Charms & Heat Treated Skins", "Global In-Game Event",
                        "https://images.unsplash.com/photo-1511512578047-dfb367046420?auto=format&fit=crop&w=1200&q=80",
                        "https://store.steampowered.com/app/730/CounterStrike_2/"),

                new SpecialEventDto("event-iem-katowice-2025",
                        "IEM Katowice 2025", "TOURNAMENT",
                        "The legendary cathedral of Counter-Strike in the Spodek arena returns with fierce world-class competition.",
                        now.plus(Duration.ofDays(45)), now.plus(Duration.ofDays(60)),
                        "$1,000,000 USD", "Spodek Arena, Katowice, Poland",
                        "https://images.unsplash.com/photo-1511882150382-421056c89033?auto=format&fit=crop&w=1200&q=80",
                        "https://pro.eslgaming.com/tour/cs/katowice/"),

                new SpecialEventDto("event-blast-world-final",
                        "BLAST Premier World Final", "TOURNAMENT",
                        "The crowning season climax bringing together world ranking champions for elite tier trophy contention.",
                        now.plus(Duration.ofDays(25)), now.plus(Duration.ofDays(32)),
                        "$1,000,000 USD", "Singapore Indoor Stadium",
                        "https://images.unsplash.com/photo-1579373903781-fd5c0c30c4cd?auto=format&fit=crop&w=1200&q=80",
                        "https://blastpremier.com/")
        );
    }
}
