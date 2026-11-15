package de.chunkbug;

import net.labymod.api.LabyModAddon;
import net.labymod.api.events.MessageSendEvent;
import net.minecraft.util.MovingObjectPosition;
import net.labymod.settings.elements.BooleanElement;
import net.labymod.settings.elements.ControlElement;
import net.labymod.settings.elements.HeaderElement;
import net.labymod.settings.elements.NumberElement;
import net.labymod.settings.elements.SettingsElement;
import net.labymod.settings.elements.StringElement;
import net.labymod.utils.Material;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.gui.inventory.GuiChest;
import net.minecraft.inventory.ContainerChest;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.Vec3;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import com.google.gson.JsonObject;

import java.util.List;
import java.util.function.IntConsumer;

/**
 * Chunkbug-Checker:
 * Bot steht auf seiner Position -> wirft ein signiertes Item aus der Hotbar ->
 * liegt es nach X Sekunden noch rum (Trichter hat es nicht eingesaugt) oder ist die Lampe aus,
 * ist der Chunk kaputt -> Bot geht für Y Minuten auf ein anderes GS und kommt zurück.
 */
public class ChunkBugAddon extends LabyModAddon {

    private enum State { IDLE, SELECTED, WAITING, AWAY, RETURNING, CHEST_LOOK, CHEST_OPEN, CHEST_TAKE, CHEST_CLOSE }

    private static final double SEARCH_RADIUS = 8; // Blöcke um die Bot-Position, in denen nach dem geworfenen Item gesucht wird

    private boolean enabled, checkLamp;
    private int posX, posY, posZ, lampX, lampY, lampZ, chestX, chestY, chestZ, waitSeconds, awayMinutes;
    private String signature, awayCommand, returnCommand;

    private State state = State.IDLE;
    private long stateSince, lastWarn;
    private boolean lampSeenOn;
    private int chestDelay;
    private float aimYaw, aimPitch; // Wurfrichtung (zum Trichter)
    private boolean hasAim;

    @Override
    public void onEnable() {
        getApi().registerForgeListener(this);
        getApi().getEventManager().register((MessageSendEvent) this::onChat);
    }

    /** #cb-Befehle im Chat. true = Nachricht nicht an den Server schicken. */
    private boolean onChat(String message) {
        if (!message.toLowerCase().startsWith("#cb")) return false;
        Minecraft mc = Minecraft.getMinecraft();
        EntityPlayerSP p = mc.thePlayer;
        if (p == null) return true;
        String cmd = message.length() > 3 ? message.substring(3).trim().toLowerCase() : "";
        BlockPos look = mc.objectMouseOver != null && mc.objectMouseOver.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK
                ? mc.objectMouseOver.getBlockPos() : null;

        switch (cmd) {
            case "start": setBool("enabled", enabled = true); set(State.IDLE); msg(p, "§aBot gestartet."); break;
            case "stop": setBool("enabled", enabled = false); set(State.IDLE); msg(p, "§cBot gestoppt."); break;
            case "pos":
                BlockPos me = new BlockPos(p);
                setNum("posX", posX = me.getX()); setNum("posY", posY = me.getY()); setNum("posZ", posZ = me.getZ());
                aimYaw = p.rotationYaw; aimPitch = p.rotationPitch; hasAim = true;
                getConfig().addProperty("yaw", aimYaw); getConfig().addProperty("pitch", aimPitch); saveConfig();
                msg(p, "Bot-Position = " + posX + " " + posY + " " + posZ + " + Wurfrichtung gespeichert (dahin, wo du schaust).");
                break;
            case "lampe":
                if (look == null) { msg(p, "§cSchau auf die Lampe."); break; }
                setNum("lampX", lampX = look.getX()); setNum("lampY", lampY = look.getY()); setNum("lampZ", lampZ = look.getZ());
                msg(p, "Lampe = " + lampX + " " + lampY + " " + lampZ);
                break;
            case "kiste":
                if (look == null) { msg(p, "§cSchau auf die Kiste."); break; }
                setNum("chestX", chestX = look.getX()); setNum("chestY", chestY = look.getY()); setNum("chestZ", chestZ = look.getZ());
                msg(p, "Kiste = " + chestX + " " + chestY + " " + chestZ);
                break;
            case "status":
                Block lamp = mc.theWorld.getBlockState(new BlockPos(lampX, lampY, lampZ)).getBlock();
                int slot = signedSlot(p);
                msg(p, "Aktiv: " + (enabled ? "§aja" : "§cnein") + "§f | Zustand: " + state
                        + " | Auf Position: " + (p.getDistanceSq(posX + 0.5, posY, posZ + 0.5) < 4 ? "§aja" : "§cnein"));
                msg(p, "Lampe: " + (lamp == Blocks.lit_redstone_lamp ? "§aAN" : lamp == Blocks.redstone_lamp ? "§cAUS" : "§ekeine Lampe/nicht geladen")
                        + "§f | Signiertes Item: " + (slot < 0 ? "§cnicht im Inventar" : "§aSlot " + (slot + 1)));
                break;
            default:
                msg(p, "#cb start | stop | status | pos (Standpunkt + Wurfrichtung) | lampe / kiste (Block, auf den du schaust)");
        }
        return true;
    }

    private void setNum(String key, int v) { getConfig().addProperty(key, v); saveConfig(); }
    private void setBool(String key, boolean v) { getConfig().addProperty(key, v); saveConfig(); }

    @Override
    public void loadConfig() {
        JsonObject c = getConfig();
        enabled = bool(c, "enabled", false);
        checkLamp = bool(c, "checkLamp", true);
        posX = num(c, "posX", 0);
        posY = num(c, "posY", 64);
        posZ = num(c, "posZ", 0);
        lampX = num(c, "lampX", 0);
        lampY = num(c, "lampY", 60);
        lampZ = num(c, "lampZ", 0);
        chestX = num(c, "chestX", 0);
        chestY = num(c, "chestY", 64);
        chestZ = num(c, "chestZ", 0);
        chestDelay = num(c, "chestDelay", 1000);
        waitSeconds = num(c, "waitSeconds", 30);
        awayMinutes = num(c, "awayMinutes", 10);
        signature = str(c, "signature", "");
        awayCommand = str(c, "awayCommand", "/p h AnderesGS");
        returnCommand = str(c, "returnCommand", "/p h MeinGS");
        hasAim = c.has("yaw");
        if (hasAim) { aimYaw = c.get("yaw").getAsFloat(); aimPitch = c.get("pitch").getAsFloat(); }
    }

    @Override
    protected void fillSettings(List<SettingsElement> s) {
        s.add(new BooleanElement("Bot aktiv", this, icon(Material.LEVER), "enabled", enabled).addCallback(v -> { enabled = v; state = State.IDLE; }));

        s.add(new HeaderElement("Bot-Position (wo er wirft)"));
        s.add(coord("Bot X", "posX", posX, v -> posX = v));
        s.add(coord("Bot Y", "posY", posY, v -> posY = v));
        s.add(coord("Bot Z", "posZ", posZ, v -> posZ = v));

        s.add(new HeaderElement("Lampe (AN = Chunk ok, AUS = kaputt)"));
        s.add(new BooleanElement("Lampe prüfen", this, icon(Material.REDSTONE_LAMP_OFF), "checkLamp", checkLamp).addCallback(v -> checkLamp = v));
        s.add(coord("Lampe X", "lampX", lampX, v -> lampX = v));
        s.add(coord("Lampe Y", "lampY", lampY, v -> lampY = v));
        s.add(coord("Lampe Z", "lampZ", lampZ, v -> lampZ = v));

        s.add(new HeaderElement("Kiste mit signierten Items (max. 4 Blöcke vom Bot)"));
        s.add(coord("Kiste X", "chestX", chestX, v -> chestX = v));
        s.add(coord("Kiste Y", "chestY", chestY, v -> chestY = v));
        s.add(coord("Kiste Z", "chestZ", chestZ, v -> chestZ = v));

        s.add(new HeaderElement("Item & Zeiten"));
        s.add(new StringElement("Signatur-Text (leer = Bot-Name)", this, icon(Material.NAME_TAG), "signature", signature).addCallback(v -> signature = v));
        s.add(number("Wartezeit (Sek.)", Material.WATCH, "waitSeconds", waitSeconds, v -> waitSeconds = v).setRange(5, 600));
        s.add(number("Weg-Zeit (Min.)", Material.WATCH, "awayMinutes", awayMinutes, v -> awayMinutes = v).setRange(1, 120));
        s.add(number("Kisten-Delay (ms)", Material.WATCH, "chestDelay", chestDelay, v -> chestDelay = v).setRange(100, 10_000));

        s.add(new HeaderElement("Befehle"));
        s.add(new StringElement("Befehl: anderes GS", this, icon(Material.ENDER_PEARL), "awayCommand", awayCommand).addCallback(v -> awayCommand = v));
        s.add(new StringElement("Befehl: zurück", this, icon(Material.ENDER_PEARL), "returnCommand", returnCommand).addCallback(v -> returnCommand = v));
    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent e) {
        if (e.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getMinecraft();
        EntityPlayerSP p = mc.thePlayer;
        if (!enabled || p == null || mc.theWorld == null) return;

        long now = System.currentTimeMillis();
        long since = now - stateSince;
        boolean atPos = p.getDistanceSq(posX + 0.5, posY, posZ + 0.5) < 4; // max. 2 Blöcke daneben

        switch (state) {
            case IDLE:
                if (!atPos) return;
                int slot = signedSlot(p);
                if (slot < 0) {
                    if (now - lastWarn > 30_000) { // max. alle 30s zur Kiste
                        lastWarn = now;
                        if (!hasAim) { aimYaw = p.rotationYaw; aimPitch = p.rotationPitch; hasAim = true; } // ohne #cb pos: Blickrichtung vor der Kiste merken
                        set(State.CHEST_LOOK);
                    }
                    return;
                }
                if (slot >= 9) { mc.playerController.windowClick(0, slot, 8, 2, p); return; } // aus dem Inventar in Hotbar-Slot 9 tauschen
                p.inventory.currentItem = slot; // Slot-Wechsel wird im nächsten Tick an den Server geschickt
                if (hasAim) { p.rotationYaw = aimYaw; p.rotationPitch = aimPitch; } // nach Kiste/Teleport wieder zum Trichter drehen
                set(State.SELECTED);
                return;

            case CHEST_LOOK: // erst zur Kiste schauen, im nächsten Tick klicken
                lookAt(p, chestX + 0.5, chestY + 0.5, chestZ + 0.5);
                if (since < chestDelay) return;
                mc.playerController.onPlayerRightClick(p, mc.theWorld, p.getHeldItem(), new BlockPos(chestX, chestY, chestZ),
                        EnumFacing.UP, new Vec3(chestX + 0.5, chestY + 1, chestZ + 0.5));
                set(State.CHEST_OPEN);
                return;

            case CHEST_OPEN:
                if (mc.currentScreen instanceof GuiChest) set(State.CHEST_TAKE);
                else if (since > 5000 + chestDelay) { msg(p, "§cKiste ging nicht auf – Koordinaten/Abstand prüfen."); set(State.IDLE); }
                return;

            case CHEST_TAKE: // warten, bis der Server den Kisten-Inhalt geschickt hat
                if (since < chestDelay) return;
                if (!(mc.currentScreen instanceof GuiChest)) { set(State.IDLE); return; }
                ContainerChest chest = (ContainerChest) p.openContainer;
                int size = chest.getLowerChestInventory().getSizeInventory();
                boolean took = false;
                for (int i = 0; i < size && !took; i++) {
                    if (isSigned(chest.getSlot(i).getStack(), p)) {
                        mc.playerController.windowClick(chest.windowId, i, 0, 1, p); // Shift-Klick: ganzer Stack ins Inventar
                        took = true;
                    }
                }
                msg(p, took ? "Signierte Items aus der Kiste genommen." : "§cKeine signierten Items mehr in der Kiste! Nächster Versuch in 30s.");
                set(State.CHEST_CLOSE);
                return;

            case CHEST_CLOSE:
                if (since < chestDelay) return;
                if (mc.currentScreen instanceof GuiChest) p.closeScreen();
                set(State.IDLE);
                return;

            case SELECTED: // kurz warten, damit Slot und Blickrichtung beim Server sind (er wirft in Blickrichtung)
                if (since < 300) return;
                p.dropOneItem(false);
                lampSeenOn = false;
                set(State.WAITING);
                return;

            case WAITING: // erst nach dem Wurf: geht die Lampe irgendwann in der Wartezeit an?
                Block lamp = lampBlock(mc);
                if (lamp == Blocks.lit_redstone_lamp) lampSeenOn = true;
                if (since < waitSeconds * 1000L) return;
                if (signedItemLying(mc, p)) broken(p, "Item nach " + waitSeconds + "s nicht eingesaugt");
                else if (checkLamp && !lampSeenOn && lamp == Blocks.redstone_lamp) broken(p, "Lampe ist nach dem Wurf nicht angegangen");
                else { msg(p, "§aChunk OK – Item eingesaugt" + (lampSeenOn ? ", Lampe ging an." : ".")); set(State.IDLE); }
                return;

            case AWAY:
                if (since < awayMinutes * 60_000L) return;
                msg(p, "Gehe zurück zur Bot-Position.");
                p.sendChatMessage(returnCommand);
                set(State.RETURNING);
                return;

            case RETURNING:
                if (atPos) { msg(p, "Zurück – teste weiter."); set(State.IDLE); }
                else if (since > 60_000) { p.sendChatMessage(returnCommand); set(State.RETURNING); } // TP fehlgeschlagen -> nochmal
        }
    }

    private void broken(EntityPlayerSP p, String reason) {
        msg(p, "§cChunk KAPUTT (" + reason + ") – gehe " + awayMinutes + " Min. weg.");
        p.sendChatMessage(awayCommand);
        set(State.AWAY);
    }

    // keine Lampe an der Stelle (nicht geladen/falsche Koordinate) -> Lampenprüfung wird ignoriert
    private Block lampBlock(Minecraft mc) {
        return mc.theWorld.getBlockState(new BlockPos(lampX, lampY, lampZ)).getBlock();
    }

    /** 0-8 = Hotbar, 9-35 = Inventar (gleich der Slot-Nummer im Spieler-Container). */
    private int signedSlot(EntityPlayerSP p) {
        for (int i = 0; i < 36; i++) if (isSigned(p.inventory.mainInventory[i], p)) return i;
        return -1;
    }

    private static void lookAt(EntityPlayerSP p, double x, double y, double z) {
        double dx = x - p.posX, dy = y - (p.posY + p.getEyeHeight()), dz = z - p.posZ;
        p.rotationYaw = (float) (Math.toDegrees(Math.atan2(dz, dx)) - 90);
        p.rotationPitch = (float) -Math.toDegrees(Math.atan2(dy, Math.sqrt(dx * dx + dz * dz)));
    }

    private boolean signedItemLying(Minecraft mc, EntityPlayerSP p) {
        AxisAlignedBB box = new AxisAlignedBB(posX - SEARCH_RADIUS, posY - SEARCH_RADIUS, posZ - SEARCH_RADIUS,
                posX + SEARCH_RADIUS, posY + SEARCH_RADIUS, posZ + SEARCH_RADIUS);
        for (EntityItem item : mc.theWorld.getEntitiesWithinAABB(EntityItem.class, box))
            if (isSigned(item.getEntityItem(), p)) return true;
        return false;
    }

    /** Signiert = Name oder Lore enthält den Signatur-Text (leer = Name des Bots). */
    private boolean isSigned(ItemStack stack, EntityPlayerSP p) {
        if (stack == null) return false;
        StringBuilder text = new StringBuilder(stack.getDisplayName());
        if (stack.hasTagCompound()) {
            NBTTagList lore = stack.getTagCompound().getCompoundTag("display").getTagList("Lore", 8);
            for (int i = 0; i < lore.tagCount(); i++) text.append('\n').append(lore.getStringTagAt(i));
        }
        String needle = signature.trim().isEmpty() ? p.getName() : signature.trim();
        return EnumChatFormatting.getTextWithoutFormattingCodes(text.toString()).toLowerCase().contains(needle.toLowerCase());
    }

    private void set(State s) { state = s; stateSince = System.currentTimeMillis(); }

    private static void msg(EntityPlayerSP p, String s) { p.addChatMessage(new ChatComponentText("§6[ChunkBug] §f" + s)); }

    // NumberElement hat keinen Auto-Speichern-Konstruktor -> selbst in die Config schreiben
    private NumberElement number(String name, Material m, String key, int value, IntConsumer set) {
        return new NumberElement(name, icon(m), value).addCallback(v -> { set.accept(v); getConfig().addProperty(key, v); saveConfig(); });
    }

    private NumberElement coord(String name, String key, int value, IntConsumer set) {
        return number(name, Material.COMPASS, key, value, set).setRange(-30_000_000, 30_000_000);
    }

    private static ControlElement.IconData icon(Material m) { return new ControlElement.IconData(m); }

    private static boolean bool(JsonObject c, String k, boolean d) { return c.has(k) ? c.get(k).getAsBoolean() : d; }
    private static int num(JsonObject c, String k, int d) { return c.has(k) ? c.get(k).getAsInt() : d; }
    private static String str(JsonObject c, String k, String d) { return c.has(k) ? c.get(k).getAsString() : d; }
}
