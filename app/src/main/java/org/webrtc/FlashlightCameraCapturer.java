/*
 * Copyright 2016 The WebRTC project authors. All Rights Reserved.
 *
 * The BSD license for this adapted WebRTC camera implementation is in
 * WEBRTC-LICENSE.txt at the project root.
 */
package org.webrtc;

import android.content.Context;

public final class FlashlightCameraCapturer extends Camera1Capturer {

    public interface FlashlightStateListener {
        void onFlashlightStateChanged(boolean enabled, String status);
    }

    private final Object flashlightLock = new Object();
    private final boolean captureToTexture;
    private FlashlightCameraSession cameraSession;
    private boolean flashlightRequested;
    private long flashlightRequestVersion;
    private FlashlightStateListener flashlightStateListener;

    public FlashlightCameraCapturer(
            String cameraName,
            CameraEventsHandler eventsHandler,
            boolean captureToTexture
    ) {
        super(cameraName, eventsHandler, captureToTexture);
        this.captureToTexture = captureToTexture;
    }

    @Override
    protected void createCameraSession(
            CameraSession.CreateSessionCallback createSessionCallback,
            CameraSession.Events events,
            Context applicationContext,
            SurfaceTextureHelper surfaceTextureHelper,
            String cameraName,
            int width,
            int height,
            int framerate
    ) {
        FlashlightCameraSession.create(
                new CameraSession.CreateSessionCallback() {
                    @Override
                    public void onDone(CameraSession session) {
                        if (!(session instanceof FlashlightCameraSession)) {
                            createSessionCallback.onFailure(
                                    CameraSession.FailureType.ERROR,
                                    "Unexpected camera session type."
                            );
                            return;
                        }
                        FlashlightCameraSession flashlightSession =
                                (FlashlightCameraSession) session;
                        boolean requested;
                        long requestVersion;
                        synchronized (flashlightLock) {
                            cameraSession = flashlightSession;
                            requested = flashlightRequested;
                            requestVersion = flashlightRequestVersion;
                        }
                        flashlightSession.setFlashlightActive(
                                requested,
                                (enabled, status) -> notifyFlashlightState(
                                        requestVersion,
                                        enabled,
                                        status
                                )
                        );
                        createSessionCallback.onDone(session);
                    }

                    @Override
                    public void onFailure(
                            CameraSession.FailureType failureType,
                            String error
                    ) {
                        createSessionCallback.onFailure(failureType, error);
                    }
                },
                events,
                captureToTexture,
                applicationContext,
                surfaceTextureHelper,
                cameraName,
                width,
                height,
                framerate
        );
    }

    public void turnOnFlashlight() {
        setFlashlight(true);
    }

    public void turnOffFlashlight() {
        setFlashlight(false);
    }

    public void setFlashlight(boolean enabled) {
        setFlashlight(enabled, null);
    }

    public void setFlashlight(
            boolean enabled,
            FlashlightStateListener stateListener
    ) {
        FlashlightCameraSession session;
        long requestVersion;
        synchronized (flashlightLock) {
            flashlightRequested = enabled;
            flashlightStateListener = stateListener;
            requestVersion = ++flashlightRequestVersion;
            session = cameraSession;
        }

        if (session == null) {
            notifyFlashlightState(
                    requestVersion,
                    false,
                    enabled ? "Waiting for camera session." : "off"
            );
            return;
        }
        session.setFlashlightActive(
                enabled,
                (actualEnabled, status) -> notifyFlashlightState(
                        requestVersion,
                        actualEnabled,
                        status
                )
        );
    }

    @Override
    public void dispose() {
        setFlashlight(false);
        synchronized (flashlightLock) {
            flashlightRequested = false;
            flashlightStateListener = null;
            cameraSession = null;
        }
        super.dispose();
    }

    private void notifyFlashlightState(
            long requestVersion,
            boolean enabled,
            String status
    ) {
        FlashlightStateListener stateListener;
        synchronized (flashlightLock) {
            if (requestVersion != flashlightRequestVersion) {
                return;
            }
            stateListener = flashlightStateListener;
        }
        if (stateListener != null) {
            stateListener.onFlashlightStateChanged(enabled, status);
        }
    }
}
