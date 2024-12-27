package net.minestom.server.network.packet.server.play;

import net.minestom.server.network.NetworkBuffer;
import net.minestom.server.network.NetworkBufferTemplate;
import net.minestom.server.network.packet.server.ServerPacket;

import static net.minestom.server.network.NetworkBuffer.*;

public record OpenHorseWindowPacket(byte windowId, int slotCount, int entityId) implements ServerPacket.Play {
    public static final NetworkBuffer.Type<OpenHorseWindowPacket> SERIALIZER = NetworkBufferTemplate.template(
            BYTE, OpenHorseWindowPacket::windowId,
            VAR_INT, OpenHorseWindowPacket::slotCount,
            INT, OpenHorseWindowPacket::entityId,
            OpenHorseWindowPacket::new);
}