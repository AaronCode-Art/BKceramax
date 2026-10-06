package com.ceramax.api.observer.event;

import com.ceramax.api.dto.response.MensajeChatResponse;
import java.util.UUID;

public record MensajeChatEvent(String tipoChat, UUID chatId, MensajeChatResponse mensaje) {}
