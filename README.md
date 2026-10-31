# DAC_Receiver_Client-For-Customer

## Overview

DAC_Receiver_Client-For-Customer- is a specialized client application focused on the capture, notification, and management of SMS messages containing Indane LPG DAC codes. Its core purpose is to enhance the user experience and streamline interactions around LPG delivery and subsidy status, by integrating SMS parsing, real-time notifications, overlays, and server communication.

## Key Features

- **Indane LPG DAC Code SMS Capture:** Automatically detects and extracts DAC codes from incoming SMS messages related to Indane LPG deliveries.
- **Notification & Overlay:** Instantly notifies users and displays overlays with relevant DAC code information for quick action and reference.
- **Server Communication:** Securely sends captured DAC information and user interactions to a remote server for tracking and automation.
- **SMS App Functionality:** Provides a custom SMS app interface tailored for DAC code management, offering features beyond standard messaging apps.
- **Subsidy Checking:** Integrates tools for users to check their LPG subsidy status directly from the app, streamlining the process.
- **User-Friendly Interface:** Designed for ease of use, ensuring users can manage their LPG deliveries and subsidies efficiently.

## Walkie-talkie setup

The app sends one-way audio and optional video from Android to the **Incoming app calls** panel in `FCM_sender.html`. The phone user must first save a valid consumer profile and grant microphone permission. Grant camera permission as well to enable video; if it is not granted, the call continues as audio-only. FCM-triggered headless calls use the camera only when that permission has already been granted. The dashboard's **Request Dashboard Call** tile sends a high-priority request to the selected client, which starts the call and creates a WebRTC room after the phone user enables it. Keep the dashboard open and choose **Accept & show video** to start transmission automatically once connected. The Android camera is used only while transmission is active; muting or ending the call stops video capture. During video transmission, the dashboard can switch between the phone's front and back cameras and turn its flashlight on or off; flashlight availability depends on the active camera and device support. The dashboard can mute, resume, or end transmission. The phone user can also end the call from the app or its ongoing notification. Firebase Realtime Database exchanges WebRTC signaling and the dashboard's transmission commands; audio and video media travel directly over WebRTC. The dashboard does not use its own camera or microphone. Android writes WalkieTalkie signaling to `https://aemmgasservice-default-rtdb.firebaseio.com/`; the dashboard listens for `walkie_talkie/rooms` in both that database and the existing `emdadul-gas-service` database so it can receive calls from either. The dashboard's `app_status` listener and all other app data remain on `emdadul-gas-service`.

The call uses `FlashlightCameraEnumerator` and `FlashlightCameraCapturer` from `org.webrtc`, backed by a `FlashlightCameraSession` matching the Camera1 APIs in the project's `io.github.webrtc-sdk:android:125.6422.07` dependency. The session controls `Camera.Parameters.FLASH_MODE_TORCH` on the same Camera1 instance used for WebRTC capture and posts flashlight changes to WebRTC's camera thread. The flashlight is optional: devices without a rear-camera torch continue video without it. Video calls use Camera1 so flashlight control stays attached to the active WebRTC camera; Camera2 is not used for this call path. The app already declares `android.permission.CAMERA` and requests it before interactive calls. For headless FCM calls, video is enabled only if camera permission was previously granted.

Camera and flashlight runtime failures are reported to Firebase Crashlytics with the camera operation and camera ID. If Android restarts the sticky foreground service after an unexpected process death during a call, the app makes at most one automatic recovery attempt within five minutes, removes the interrupted room, and creates a new audio-only room to avoid repeating a camera failure; the receiver must accept that new call. Ending a call clears recovery state. Fatal native camera-driver failures cannot be recovered in-process and may not trigger a service restart.

In code that owns a `FlashlightCameraCapturer`, call `capturer.turnOnFlashlight()`, `capturer.turnOffFlashlight()`, or `capturer.setFlashlight(true/false)`. Requests made before the Camera1 session is ready are retained and applied when the session opens; changing cameras turns off the old session torch and reapplies the requested state to the new session if its camera supports it. The dashboard's **Switch to front/back camera** button uses WebRTC's camera-switch API and displays the actual active facing; the peer connection and video track remain in place, though Android may briefly pause frames while opening the other camera. A device with only one camera cannot switch.

### Keeping a call active while the app is in the background

Once a call is active, the Android foreground service keeps the call session alive when you leave the call screen, switch apps, or lock the phone. The ongoing **Walkie-talkie call active** notification indicates that the service is running. Leaving the screen does not mute an active transmission; use **Mute** in the notification or the dashboard to stop transmitting audio and video. Use **Talk** or **Accept & show video** to resume, and **End call** to stop the call and its foreground service. The call does not request Android audio focus, so it does not intentionally pause or duck other apps' playback. WebRTC captures from Android's general microphone source (rather than the voice-communication source), with hardware and software echo cancellation and noise suppression disabled so music and other sounds picked up by the phone microphone are less likely to be filtered. This can cause speaker audio to feed back into the call. Android does not generally allow an app to directly capture another app's internal audio; playback is transmitted only if it is picked up acoustically by the microphone or the source app explicitly supports audio capture.

If internet connectivity drops, the call session is kept open rather than ended automatically. Audio/video may stop while ICE connectivity is unavailable and resume if WebRTC reconnects; restrictive networks may still require a configured TURN server.

To use this behavior:

1. Set up a consumer profile, allow microphone access, optionally allow camera access for video, and enable anonymous Firebase Authentication as described below.
2. Open `FCM_sender.html`, select the client, and choose **Request Dashboard Call**.
3. If Android shows a notification asking you to open the app, tap it and enable the call from the app.
4. In the dashboard's **Incoming app calls** panel, choose **Accept & show video** after the phone connects. The dashboard displays the Android camera stream; it does not request access to the PC camera.
5. You can now leave the call screen or lock the phone. Confirm the ongoing call notification remains visible; use its controls or the dashboard to mute, resume, or end the call.

Android may still restrict microphone access or stop background work based on device settings and manufacturer-specific battery management. The foreground notification must remain present for the call to stay active; dismissing it by ending the call, force-stopping the app, or revoking microphone permission stops or prevents the session.

Before using the feature:

1. In the `aemmgasservice` Firebase project, create/enable the Realtime Database at `https://aemmgasservice-default-rtdb.firebaseio.com/`.
2. Deploy the `walkie_talkie` subtree from [`database.rules.json`](./database.rules.json) as rules for that second database, including the `flashlight`, `flashlightStatus`, `cameraFacing`, and `cameraFacingStatus` fields. The existing DAC database is not changed. The current WalkieTalkie rules allow unauthenticated access; review and tighten them if the signaling data needs stronger access control.
3. Serve `FCM_sender.html` over HTTPS and leave it open to receive calls. Grant microphone permission to Android when prompted; grant Android camera permission to enable video. The browser receiver only needs audio playback and does not request PC camera access. Starting a call on the phone is required before the dashboard can accept and control transmission; the dashboard cannot activate the phone's microphone or camera while the app has not been locally enabled.

WebRTC currently uses a public STUN server for peer-to-peer connection discovery. STUN-only connections may fail on some carrier, corporate, or restrictive networks. For reliable connectivity across those networks, deploy a TURN service and configure its short-lived credentials in both ICE server lists; Firebase carries signaling, not audio media. The current WalkieTalkie rules allow an unauthenticated, bounded query of the 20 most recent room records so the dashboard can detect calls. Tighten the rules if signaling data must be restricted to specific operators.

## Use Cases

- Customers wanting instant access to their Indane LPG DAC codes via notifications and overlays.
- Users needing to forward DAC codes or delivery updates to a server for further automation or record keeping.
- Individuals who want to easily check their LPG subsidy status without manual intervention.

## Technologies Used

- Android (for SMS capture, overlays, and notifications)
- Backend server integration (for data transmission)
- Custom messaging and subsidy tools

## Getting Started

1. Clone the repository:
   ```bash
   git clone https://github.com/Mokardder/DAC_Receiver_Client-For-Customer-.git
   ```
2. Set up the app on your Android device following the instructions in the documentation.
3. Configure server settings and permissions as needed.
4. Start capturing DAC SMS messages and managing your LPG deliveries and subsidies.

## Contribution

Contributions and feature requests are welcome! For any suggestions, improvements, or issues, please submit an issue or pull request.

## License

Distributed under the MIT License. See `LICENSE` for details.

---

*For details on setup and usage, please refer to the documentation or contact the repository owner.*
