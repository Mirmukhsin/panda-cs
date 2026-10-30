package com.cs2hub.stratboard.controller;

import com.cs2hub.stratboard.dto.StratBoardWsBroadcastDto;
import com.cs2hub.stratboard.dto.StratBoardWsEventDto;
import com.cs2hub.stratboard.service.StratRoomService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;

@Slf4j
@Controller
@RequiredArgsConstructor
public class StratRoomWsController {

    private final StratRoomService stratRoomService;

    @MessageMapping("/stratboard/rooms/{roomCode}/event")
    @SendTo("/topic/stratboard/rooms/{roomCode}")
    public StratBoardWsBroadcastDto handleRoomEvent(
            @DestinationVariable String roomCode,
            StratBoardWsEventDto event
    ) {
        log.debug("Processing WS event {} for room [{}] from participant {}",
                event.eventType(), roomCode, event.participantId());
        return stratRoomService.applyRoomEvent(roomCode, event);
    }
}
