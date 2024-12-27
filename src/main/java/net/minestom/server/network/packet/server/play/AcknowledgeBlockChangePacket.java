package net.minestom.server.network.packet.server.play;

import net.minestom.server.network.NetworkBuffer;
import net.minestom.server.network.NetworkBufferTemplate;
import net.minestom.server.network.packet.server.ServerPacket;

import static net.minestom.server.network.NetworkBuffer.VAR_INT;

public record AcknowledgeBlockChangePacket(int sequence) implements ServerPacket.Play {
    public static final NetworkBuffer.Type<AcknowledgeBlockChangePacket> SERIALIZER = NetworkBufferTemplate.template(
            VAR_INT, AcknowledgeBlockChangePacket::sequence,
            AcknowledgeBlockChangePacket::new);
}