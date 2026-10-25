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

The app includes one-way audio from Android to the **Incoming app calls** panel in `FCM_sender.html`. The phone user must first save a valid consumer profile and grant microphone permission. The dashboard's **Request Dashboard Call** tile sends a high-priority request to the selected client, which automatically starts the call and creates a WebRTC room. If Android blocks a background microphone foreground service, the client instead shows a notification that opens the call screen for manual enablement. Keep the dashboard open and choose **Accept & transmit** to start sending audio automatically once connected. The dashboard can mute, resume, or end transmission. The phone user can also end the call from the app or its ongoing notification. Firebase Realtime Database exchanges WebRTC signaling and the dashboard's transmission commands; audio media travels directly over WebRTC.

### Keeping a call active while the app is in the background

Once a call is active, the Android foreground service keeps the call session alive when you leave the call screen, switch apps, or lock the phone. The ongoing **Walkie-talkie call active** notification indicates that the service is running. Leaving the screen does not mute an active transmission; use **Mute** in the notification or the dashboard to stop transmitting. Use **Talk** or **Accept & transmit** to resume, and **End call** to stop the call and its foreground service.

To use this behavior:

1. Set up a consumer profile, allow microphone access, and enable anonymous Firebase Authentication as described below.
2. Open `FCM_sender.html`, select the client, and choose **Request Dashboard Call**.
3. If Android shows a notification asking you to open the app, tap it and enable the call from the app.
4. In the dashboard's **Incoming app calls** panel, choose **Accept & transmit** after the phone connects.
5. You can now leave the call screen or lock the phone. Confirm the ongoing call notification remains visible; use its controls or the dashboard to mute, resume, or end the call.

Android may still restrict microphone access or stop background work based on device settings and manufacturer-specific battery management. The foreground notification must remain present for the call to stay active; dismissing it by ending the call, force-stopping the app, or revoking microphone permission stops or prevents the session.

Before using the feature:

1. In Firebase Authentication, enable the **Anonymous** sign-in provider for the existing Firebase project. Both Android and the dashboard sign in anonymously.
2. In Realtime Database Rules, merge the `walkie_talkie` subtree from [`database.rules.json`](./database.rules.json) into the existing rules. Do not replace existing application rules with this subtree.
3. Serve `FCM_sender.html` over HTTPS and leave it open to receive calls. Grant microphone permission to Android when prompted; the browser receiver only needs audio playback. Starting a call on the phone is required before the dashboard can accept and control transmission; the dashboard cannot activate the microphone while the app has not been locally enabled.

WebRTC currently uses a public STUN server for peer-to-peer connection discovery. STUN-only connections may fail on some carrier, corporate, or restrictive networks. For reliable connectivity across those networks, deploy a TURN service and configure its short-lived credentials in both ICE server lists; Firebase carries signaling, not audio media. The receiver rules allow an authenticated, bounded query of the 20 most recent room records so the dashboard can detect calls. Anonymous Authentication is not a private staff identity system; use stronger Firebase Authentication and tighten the rules if signaling data must be restricted to specific operators.

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
