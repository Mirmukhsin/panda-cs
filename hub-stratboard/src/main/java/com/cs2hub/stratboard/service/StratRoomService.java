package com.cs2hub.stratboard.service;

import com.cs2hub.common.exception.BadRequestException;
import com.cs2hub.common.exception.ResourceNotFoundException;
import com.cs2hub.common.exception.UnauthorizedException;
import com.cs2hub.common.security.AuthenticatedUser;
import com.cs2hub.stratboard.domain.Cs2Map;
import com.cs2hub.stratboard.domain.StratBoardEventType;
import com.cs2hub.stratboard.domain.StratRoomState;
import com.cs2hub.stratboard.dto.CanvasElementDto;
import com.cs2hub.stratboard.dto.CreateStratRoomRequestDto;
import com.cs2hub.stratboard.dto.RoomParticipantDto;
import com.cs2hub.stratboard.dto.SaveRoomTacticRequestDto;
import com.cs2hub.stratboard.dto.SaveTacticRequestDto;
import com.cs2hub.stratboard.dto.StratBoardWsBroadcastDto;
import com.cs2hub.stratboard.dto.StratBoardWsEventDto;
import com.cs2hub.stratboard.dto.StratRoomSnapshotDto;
import com.cs2hub.stratboard.dto.TacticDetailDto;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class StratRoomService {

    private final TacticService tacticService;
    private final SimpMessagingTemplate messagingTemplate;
    private final ObjectMapper objectMapper;

    private static final String ROOM_CODE_CHARS = "23456789ABCDEFGHJKLMNPQRSTUVWXYZ";
    private final SecureRandom secureRandom = new SecureRandom();

    private final Map<String, StratRoomState> rooms = new ConcurrentHashMap<>();

    public StratRoomSnapshotDto createRoom(
            CreateStratRoomRequestDto request,
            Optional<AuthenticatedUser> currentUser
    ) {
        if (!Cs2Map.isValid(request.mapName())) {
            throw new BadRequestException("Unsupported CS2 map: " + request.mapName());
        }

        String roomCode = generateUniqueRoomCode();
        String hostParticipantId = "host-" + UUID.randomUUID().toString().substring(0, 8);
        UUID hostUserId = currentUser.map(AuthenticatedUser::userId).orElse(null);

        String colorHex = StringUtils.hasText(request.hostColorHex())
                ? request.hostColorHex().trim()
                : "#3B82F6";

        RoomParticipantDto hostParticipant = new RoomParticipantDto(
                hostParticipantId,
                request.hostDisplayName().trim(),
                colorHex,
                hostUserId,
                true
        );

        StratRoomState roomState = new StratRoomState(
                roomCode,
                request.mapName().trim().toLowerCase(),
                request.teamSide(),
                hostUserId,
                hostParticipantId,
                hostParticipant
        );

        rooms.put(roomCode, roomState);
        log.info("Created collaborative room [{}] for map {} ({}) by host '{}'",
                roomCode, roomState.getMapName(), roomState.getTeamSide(), request.hostDisplayName());

        // Pre-load initial tactic if specified
        if (StringUtils.hasText(request.initialTacticIdOrShareCode())) {
            try {
                loadTacticIntoRoomState(roomState, request.initialTacticIdOrShareCode().trim(), currentUser);
            } catch (Exception e) {
                log.warn("Failed to pre-load initial tactic '{}' into room [{}]: {}",
                        request.initialTacticIdOrShareCode(), roomCode, e.getMessage());
            }
        }

        return roomState.toSnapshotDto();
    }

    public StratRoomSnapshotDto getRoomSnapshot(String roomCode) {
        StratRoomState roomState = getRoomOrThrow(roomCode);
        roomState.touch();
        return roomState.toSnapshotDto();
    }

    public StratBoardWsBroadcastDto applyRoomEvent(String roomCode, StratBoardWsEventDto event) {
        StratRoomState roomState = getRoomOrThrow(roomCode);
        roomState.touch();

        String participantId = event.participantId();
        StratBoardEventType type = event.eventType();

        StratBoardWsBroadcastDto broadcast;

        switch (type) {
            case PARTICIPANT_JOIN -> {
                RoomParticipantDto p = event.participant();
                if (p != null) {
                    roomState.getParticipants().put(p.participantId(), p);
                }
                broadcast = new StratBoardWsBroadcastDto(
                        roomCode, type, participantId, p, null, null, null, null, null, null, Instant.now()
                );
            }
            case PARTICIPANT_LEAVE -> {
                RoomParticipantDto removed = roomState.getParticipants().remove(participantId);
                broadcast = new StratBoardWsBroadcastDto(
                        roomCode, type, participantId, removed, null, null, null, null, null, null, Instant.now()
                );
            }
            case CURSOR_MOVE -> broadcast = new StratBoardWsBroadcastDto(
                    roomCode, type, participantId, null, event.cursor(), null, null, null, null, null, Instant.now()
            );
            case ELEMENT_UPSERT -> {
                CanvasElementDto elem = event.element();
                if (elem != null) {
                    roomState.getCanvasElements().put(elem.elementId(), elem);
                }
                broadcast = new StratBoardWsBroadcastDto(
                        roomCode, type, participantId, null, null, elem, null, null, null, null, Instant.now()
                );
            }
            case ELEMENT_DELETE -> {
                String elemId = event.elementId();
                if (elemId != null) {
                    roomState.getCanvasElements().remove(elemId);
                }
                broadcast = new StratBoardWsBroadcastDto(
                        roomCode, type, participantId, null, null, null, elemId, null, null, null, Instant.now()
                );
            }
            case BOARD_CLEAR -> {
                roomState.getCanvasElements().clear();
                broadcast = new StratBoardWsBroadcastDto(
                        roomCode, type, participantId, null, null, null, null, null, null, Collections.emptyList(), Instant.now()
                );
            }
            case MAP_CHANGE -> {
                if (StringUtils.hasText(event.mapName()) && Cs2Map.isValid(event.mapName())) {
                    roomState.setMapName(event.mapName().trim().toLowerCase());
                }
                if (event.teamSide() != null) {
                    roomState.setTeamSide(event.teamSide());
                }
                broadcast = new StratBoardWsBroadcastDto(
                        roomCode, type, participantId, null, null, null, null,
                        roomState.getMapName(), roomState.getTeamSide(), null, Instant.now()
                );
            }
            case LOAD_TACTIC -> {
                if (StringUtils.hasText(event.tacticIdOrShareCode())) {
                    loadTacticIntoRoomState(roomState, event.tacticIdOrShareCode(), Optional.empty());
                } else if (StringUtils.hasText(event.canvasStateJson())) {
                    List<CanvasElementDto> parsed = parseCanvasElements(event.canvasStateJson());
                    roomState.getCanvasElements().clear();
                    parsed.forEach(el -> roomState.getCanvasElements().put(el.elementId(), el));
                }
                broadcast = new StratBoardWsBroadcastDto(
                        roomCode, type, participantId, null, null, null, null,
                        roomState.getMapName(), roomState.getTeamSide(),
                        new ArrayList<>(roomState.getCanvasElements().values()),
                        Instant.now()
                );
            }
            default -> broadcast = StratBoardWsBroadcastDto.of(roomCode, type, participantId);
        }

        return broadcast;
    }

    public StratRoomSnapshotDto loadTacticIntoRoom(
            String roomCode,
            String tacticIdOrShareCode,
            Optional<AuthenticatedUser> currentUser
    ) {
        StratRoomState roomState = getRoomOrThrow(roomCode);
        loadTacticIntoRoomState(roomState, tacticIdOrShareCode, currentUser);

        // Broadcast to all connected room subscribers
        StratBoardWsBroadcastDto broadcast = new StratBoardWsBroadcastDto(
                roomState.getRoomCode(),
                StratBoardEventType.LOAD_TACTIC,
                "system",
                null,
                null,
                null,
                null,
                roomState.getMapName(),
                roomState.getTeamSide(),
                new ArrayList<>(roomState.getCanvasElements().values()),
                Instant.now()
        );

        messagingTemplate.convertAndSend("/topic/stratboard/rooms/" + roomState.getRoomCode(), broadcast);
        return roomState.toSnapshotDto();
    }

    public TacticDetailDto saveRoomAsTactic(
            String roomCode,
            AuthenticatedUser currentUser,
            SaveRoomTacticRequestDto request
    ) {
        if (currentUser == null) {
            throw new UnauthorizedException("Authentication is required to save tactics");
        }

        StratRoomState roomState = getRoomOrThrow(roomCode);

        // Serialize current canvas elements into JSON
        Map<String, Object> stateMap = new LinkedHashMap<>();
        stateMap.put("version", 1);
        stateMap.put("mapName", roomState.getMapName());
        stateMap.put("teamSide", roomState.getTeamSide());
        stateMap.put("elements", new ArrayList<>(roomState.getCanvasElements().values()));

        String canvasStateJson;
        try {
            canvasStateJson = objectMapper.writeValueAsString(stateMap);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize room {} canvas elements: {}", roomCode, e.getMessage());
            canvasStateJson = "{\"elements\":[]}";
        }

        SaveTacticRequestDto saveRequest = new SaveTacticRequestDto(
                request.title(),
                roomState.getMapName(),
                roomState.getTeamSide(),
                request.description(),
                canvasStateJson,
                request.isPublic() == null || request.isPublic()
        );

        TacticDetailDto savedTactic = tacticService.createTactic(currentUser.userId(), saveRequest);
        log.info("Saved room [{}] state as new tactic '{}' ({}) by user {}",
                roomCode, savedTactic.title(), savedTactic.shareCode(), currentUser.userId());

        return savedTactic;
    }

    @Scheduled(fixedDelay = 1800000) // Every 30 minutes
    public void cleanupInactiveRooms() {
        Instant cutoff = Instant.now().minus(Duration.ofHours(6));
        int beforeCount = rooms.size();

        rooms.entrySet().removeIf(entry -> {
            boolean expired = entry.getValue().getLastActivityAt().isBefore(cutoff);
            if (expired) {
                log.info("Evicting inactive stratboard room [{}] (last active: {})",
                        entry.getKey(), entry.getValue().getLastActivityAt());
            }
            return expired;
        });

        int evicted = beforeCount - rooms.size();
        if (evicted > 0) {
            log.info("Cleaned up {} inactive stratboard rooms. Active remaining: {}", evicted, rooms.size());
        }
    }

    public int getActiveRoomsCount() {
        return rooms.size();
    }

    public void clearAllRooms() {
        rooms.clear();
    }

    private void loadTacticIntoRoomState(
            StratRoomState roomState,
            String tacticIdOrShareCode,
            Optional<AuthenticatedUser> currentUser
    ) {
        Optional<UUID> userId = currentUser.map(AuthenticatedUser::userId);
        TacticDetailDto tactic = tacticService.getTacticByIdOrShareCode(tacticIdOrShareCode, userId);

        roomState.setMapName(tactic.mapName());
        roomState.setTeamSide(tactic.teamSide());

        List<CanvasElementDto> elements = parseCanvasElements(tactic.canvasStateJson());
        roomState.getCanvasElements().clear();
        for (CanvasElementDto elem : elements) {
            roomState.getCanvasElements().put(elem.elementId(), elem);
        }
        roomState.touch();
    }

    private List<CanvasElementDto> parseCanvasElements(String json) {
        if (!StringUtils.hasText(json)) {
            return Collections.emptyList();
        }
        try {
            JsonNode root = objectMapper.readTree(json);
            if (root.has("elements") && root.get("elements").isArray()) {
                return objectMapper.readerForListOf(CanvasElementDto.class).readValue(root.get("elements"));
            } else if (root.isArray()) {
                return objectMapper.readerForListOf(CanvasElementDto.class).readValue(root);
            }
        } catch (Exception e) {
            log.warn("Failed to parse canvasStateJson into elements list: {}", e.getMessage());
        }
        return Collections.emptyList();
    }

    private StratRoomState getRoomOrThrow(String roomCode) {
        if (!StringUtils.hasText(roomCode)) {
            throw new ResourceNotFoundException("Room code is required");
        }
        String normalized = roomCode.trim().toUpperCase();
        StratRoomState state = rooms.get(normalized);
        if (state == null) {
            throw new ResourceNotFoundException("Stratboard room not found with code: " + normalized);
        }
        return state;
    }

    private String generateUniqueRoomCode() {
        for (int i = 0; i < 20; i++) {
            StringBuilder sb = new StringBuilder(8);
            for (int j = 0; j < 8; j++) {
                sb.append(ROOM_CODE_CHARS.charAt(secureRandom.nextInt(ROOM_CODE_CHARS.length())));
            }
            String code = sb.toString();
            if (!rooms.containsKey(code)) {
                return code;
            }
        }
        return UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
}
