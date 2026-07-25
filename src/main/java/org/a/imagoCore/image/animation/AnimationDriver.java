package org.a.imagoCore.image.animation;

import org.a.imagoCore.image.display.ImageDisplay;

import java.awt.image.BufferedImage;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Drives animated playback of a sequence of image frames on a single
 * {@link ImageDisplay}.
 *
 * <p>Frame advancement is driven by the scheduler tick (or a custom
 * interval).  Supports loop, ping-pong, and single-play modes.
 */
public class AnimationDriver {

    public enum Mode {
        /** Play once then stop. */
        ONCE,
        /** Loop from beginning after reaching the end. */
        LOOP,
        /** Forward then reverse, repeatedly. */
        PING_PONG
    }

    private final ImageDisplay display;
    private final List<BufferedImage> frames;
    private final AtomicInteger index = new AtomicInteger(0);
    private final int intervalTicks;
    private final Mode mode;
    private volatile boolean running;

    public AnimationDriver(ImageDisplay display, List<BufferedImage> frames,
                           int intervalTicks, Mode mode) {
        this.display = display;
        this.frames = frames;
        this.intervalTicks = intervalTicks;
        this.mode = mode;
    }

    /** Advance to the next frame and update the display. */
    public void tick() {
        if (!running || frames.isEmpty()) return;
        int idx = index.getAndIncrement();

        if (idx >= frames.size()) {
            switch (mode) {
                case ONCE -> { stop(); return; }
                case LOOP  -> index.set(idx = 0);
                case PING_PONG -> {
                    // TODO: reverse direction on boundary
                    index.set(idx = 0);
                }
            }
        }

        display.show(frames.get(idx));
    }

    public void start() {
        this.running = true;
    }

    public void stop() {
        this.running = false;
    }

    public boolean isRunning() {
        return running;
    }

    public ImageDisplay getDisplay() {
        return display;
    }

    public int getIntervalTicks() {
        return intervalTicks;
    }
}
