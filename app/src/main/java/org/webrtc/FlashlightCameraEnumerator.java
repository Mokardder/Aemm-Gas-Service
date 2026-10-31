/*
 * Copyright 2015 The WebRTC project authors. All Rights Reserved.
 *
 * The BSD license for this adapted WebRTC camera implementation is in
 * WEBRTC-LICENSE.txt at the project root.
 */
package org.webrtc;

public final class FlashlightCameraEnumerator extends Camera1Enumerator {

    private final boolean captureToTexture;

    public FlashlightCameraEnumerator() {
        this(true);
    }

    public FlashlightCameraEnumerator(boolean captureToTexture) {
        super(captureToTexture);
        this.captureToTexture = captureToTexture;
    }

    @Override
    public FlashlightCameraCapturer createCapturer(
            String deviceName,
            CameraVideoCapturer.CameraEventsHandler eventsHandler
    ) {
        return new FlashlightCameraCapturer(deviceName, eventsHandler, captureToTexture);
    }
}
