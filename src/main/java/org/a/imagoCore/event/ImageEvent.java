package org.a.imagoCore.event;

import org.a.imagoCore.image.display.ImageDisplay;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * Base event for all ImagoCore image-related events.
 *
 * <p>Custom events that need image context should extend this class:
 * <pre>{@code
 * public class ImageLoadEvent extends ImageEvent {
 *     // ...
 * }
 * }</pre>
 */
public abstract class ImageEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();
    private final ImageDisplay display;
    private final String imageId;

    protected ImageEvent(ImageDisplay display, String imageId) {
        this.display = display;
        this.imageId = imageId;
    }

    protected ImageEvent(boolean isAsync, ImageDisplay display, String imageId) {
        super(isAsync);
        this.display = display;
        this.imageId = imageId;
    }

    /** Returns the {@link ImageDisplay} involved in this event. */
    public ImageDisplay getDisplay() {
        return display;
    }

    /** Returns the identifier of the image involved. */
    public String getImageId() {
        return imageId;
    }

    // ── Bukkit event boilerplate ─────────────────────────────────

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }

    @SuppressWarnings("unused")  // required by Bukkit API
    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
