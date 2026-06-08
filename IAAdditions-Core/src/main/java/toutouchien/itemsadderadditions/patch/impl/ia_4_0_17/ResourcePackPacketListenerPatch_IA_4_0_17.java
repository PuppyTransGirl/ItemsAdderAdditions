package toutouchien.itemsadderadditions.patch.impl.ia_4_0_17;

import org.objectweb.asm.Type;
import org.objectweb.asm.commons.GeneratorAdapter;
import org.objectweb.asm.commons.Method;
import toutouchien.itemsadderadditions.patch.MethodCallRewritePatch;
import toutouchien.itemsadderadditions.patch.VersionConstraint;
import toutouchien.itemsadderadditions.patch.VersionSet;
import toutouchien.itemsadderadditions.patch.support.ProtocolLibPacketTypeFilter;

/**
 * ItemsAdder 4.0.17 registers both play and configuration resource-pack packet
 * listeners unconditionally. Some ProtocolLib/Minecraft combinations expose one
 * of those constants but leave it unregistered, producing noisy startup warnings.
 */
public class ResourcePackPacketListenerPatch_IA_4_0_17 extends MethodCallRewritePatch {
    private static final Type RESOURCE_PACK_SERVICE = Type.getObjectType("itemsadder/m/auy");
    private static final Type PACKET_TYPE_ARRAY = Type.getType("[Lcom/comphenix/protocol/PacketType;");

    @Override
    public VersionConstraint supportedVersions() {
        return VersionSet.ia("4.0.17");
    }

    @Override
    public String targetClass() {
        return "itemsadder/m/aut";
    }

    @Override
    protected String targetMethod() {
        return "<init>";
    }

    @Override
    protected String targetDescriptor() {
        return "(Litemsadder/m/auy;)V";
    }

    @Override
    protected String targetCallOwner() {
        return "itemsadder/m/auu";
    }

    @Override
    protected String targetCallName() {
        return "<init>";
    }

    @Override
    protected String targetCallDescriptor() {
        return "(Litemsadder/m/aut;Lorg/bukkit/plugin/Plugin;Lcom/comphenix/protocol/events/ListenerPriority;[Lcom/comphenix/protocol/PacketType;Litemsadder/m/auy;)V";
    }

    @Override
    protected void rewriteCall(
            GeneratorAdapter ga,
            int opcode,
            String owner,
            String name,
            String descriptor,
            boolean isInterface
    ) {
        int resourcePackService = ga.newLocal(RESOURCE_PACK_SERVICE);
        ga.storeLocal(resourcePackService);

        ga.invokeStatic(
                Type.getType(ProtocolLibPacketTypeFilter.class),
                Method.getMethod("Object[] supportedOnly(Object[])")
        );
        ga.checkCast(PACKET_TYPE_ARRAY);

        ga.loadLocal(resourcePackService);
        ga.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
    }
}
