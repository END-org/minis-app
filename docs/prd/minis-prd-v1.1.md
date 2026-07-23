# MINIS Product Requirements Document

**Document Status:** Draft 1.1  
**Product:** MINIS  
**Repository:** `https://github.com/END-org/minis-app`  
**Primary Platform:** Mobile Application  
**Design Reference:** Figma MVP Mockups  
**Last Updated:** 2026-07-10  
**Document Owner:** Amr Bani Irshid  

---

## 1. Executive Summary

MINIS is a mobile-first **Mini Films Platform** for short, high-quality, valuable visual content. It is not a generic short-video entertainment app and it is not a traditional short-film platform where videos commonly run for 20–30 minutes. The intended content format is a compact mini film: usually around one minute, with some flexibility depending on the creative need.

The product gives filmmakers, creators, studios, brands, and organizations a focused place to publish cinematic, meaningful, and high-signal content. Viewers can open the app, browse a polished feed, watch videos or image-based posts, follow creators, interact through likes, saves, comments, and replies, discover content by creators and categories, receive relevant notifications, and communicate with creators through a controlled one-to-one chat experience.

The MVP should validate whether users engage with a feed of mini films that feel worth their time, while keeping the product simple enough to build, moderate, and operate. The first version prioritizes the core viewer experience, guest browsing, Firebase-based authentication, creator-controlled publishing for approved creators, basic profile and discovery features, comments and replies, reporting, in-app and push notifications, creator-centered direct messaging, and a lightweight admin dashboard.

Notifications are included in the MVP to support re-engagement and timely interaction. Chat is included as an intentionally limited **MVP Lite** feature: authenticated viewers may start one-to-one text conversations with approved creators, and creators may respond. General viewer-to-viewer messaging, group chat, media attachments, voice messages, calls, and advanced presence features are outside the MVP.

---

## 2. Product Positioning

### 2.1 Product Definition

MINIS is a platform for **mini films with real value**.

A mini film in MINIS is a short visual work that is:

- concise, usually close to one minute;
- high-quality in presentation or intent;
- cinematic, creative, educational, documentary-style, narrative, or otherwise meaningful;
- valuable enough that the viewer should feel the watch time was justified.

MINIS should not position itself as a clone of TikTok, Reels, or YouTube Shorts. It may borrow mobile feed interaction patterns, but the product identity is different: the focus is value, taste, creator identity, and cinematic quality.

### 2.2 Product Tagline

> Mini films with real value.

### 2.3 Product Promise

Users should be able to open MINIS and quickly find short visual content that feels curated, intentional, and worth watching.

Creators should be able to publish compact visual work in an environment where quality and value are part of the product identity, not an exception hidden inside a noisy entertainment feed.

Notifications and messaging should strengthen the relationship between viewers and creators without turning MINIS into a generic social network or an unrestricted messaging platform.

---

## 3. Problem Statement

Short-form media products are extremely engaging, but they often reward volume, trends, and passive scrolling more than quality or meaning. Valuable short visual work can get buried inside entertainment-first feeds, while creators who care about cinematic storytelling, useful ideas, or compact narratives have limited product surfaces designed specifically for that type of content.

MINIS addresses this by creating a focused mobile platform where mini films, high-quality image posts, creator identity, meaningful discovery, timely interaction, and controlled creator communication are first-class product concepts.

The MVP will not attempt to solve large-scale recommendation, monetization, advanced video processing, open creator onboarding, or unrestricted social messaging. Instead, it will prove the core experience: a user can browse, watch, follow, interact, discover, receive relevant updates, communicate with a creator, and return because the content and creator relationships feel valuable.

---

## 4. Goals and Success Criteria

### 4.1 Product Goals

| ID | Goal | Description |
|---|---|---|
| G1 | Establish a clear mini-film experience | The product should feel like a platform for valuable mini films, not a generic short-video clone. |
| G2 | Deliver a polished mobile feed | Users can browse videos and image posts through a fast, simple, mobile-first feed. |
| G3 | Support guest discovery | Users can browse content before signing in, while interactions require authentication. |
| G4 | Support trusted creators | Only admin-approved creators can upload content in the MVP. |
| G5 | Preserve content quality | Publishing is limited to trusted creators, with reporting and admin controls available from the beginning. |
| G6 | Enable social engagement | Users can like, save, comment, reply, and follow creators. |
| G7 | Build measurable foundations | The MVP captures enough product events to evaluate engagement and inform future ranking. |
| G8 | Support timely re-engagement | Users receive useful in-app and push notifications for important interactions. |
| G9 | Enable controlled creator communication | Authenticated viewers can communicate privately with creators through a limited, moderated one-to-one text chat. |

### 4.2 MVP Success Metrics

| Metric | Why it matters |
|---|---|
| Watch Completion Rate | Measures whether mini films are strong enough to keep users watching. |
| Videos Watched Per Session | Measures feed momentum and session depth. |
| Daily Active Users | Measures whether users return to consume content. |
| Published Content Count | Measures whether approved creators are supplying enough content. |
| Report Rate / Quality Issues | Measures whether content quality and safety are under control. |
| Notification Open Rate | Measures whether notifications are relevant and useful rather than noisy. |
| Message Delivery Success Rate | Measures whether chat messages are stored and delivered reliably. |
| Creator Response Rate | Measures whether viewer-to-creator chat creates useful engagement. |

The MVP should not be judged by sign-up volume alone. The stronger signal is whether users watch, complete, interact with, communicate around, and return to valuable content.

Chat metrics are secondary to the core content metrics. The chat feature must not delay or weaken the primary mini-film experience.

---

## 5. Non-Goals

The following are explicitly outside the MVP scope.

| ID | Non-Goal | Reason |
|---|---|---|
| NG1 | Live streaming | Adds real-time infrastructure, moderation, latency, and operational complexity. |
| NG2 | Advanced streaming architecture | HLS/DASH, adaptive bitrate ladders, and advanced media delivery are deferred. MVP supports basic video/image playback. |
| NG3 | Advanced recommendation engine | First version uses latest ordering, not ML ranking or personalization. |
| NG4 | Advanced notification automation | Marketing campaigns, recommendation-based notifications, scheduled digests, and complex preference rules are deferred. |
| NG5 | General-purpose social messaging | Viewer-to-viewer messaging, group chat, media attachments, voice messages, calls, and unrestricted messaging are outside MVP. |
| NG6 | Monetization | Ads, subscriptions, tips, creator payouts, and revenue sharing are future work. |
| NG7 | AI moderation | Reports, blocking, rate limits, and manual admin actions are sufficient for MVP. |
| NG8 | Video editing tools | Trimming, filters, effects, templates, and in-app editing are deferred. |
| NG9 | Public creator application system | Admin-controlled creator approval is enough for the first version. |
| NG10 | Advanced search engine | PostgreSQL search is sufficient for MVP. |
| NG11 | Download videos | Downloading content is not part of the first version. |
| NG12 | Playlists | Playlists are deferred until the content model matures. |
| NG13 | Full account deletion workflow | Delete Account is deferred because it requires policy decisions around related content and data. |
| NG14 | Advanced chat presence | Typing indicators, online status, last seen, reactions, message editing, and delete-for-everyone are deferred. |
| NG15 | Custom end-to-end encryption protocol | MVP relies on secure transport, authenticated access, authorization, and protected storage; custom E2EE is not included. |

---

## 6. Target Users and Roles

### 6.1 Viewer

A viewer watches mini films, browses categories, opens creator profiles, reads comments, interacts after signing in, receives notifications, and may communicate with creators.

Viewer needs:

- fast access to the feed;
- content that feels worth watching;
- ability to browse as a guest;
- simple interactions after login;
- creator profiles and categories for discovery;
- relevant notifications without excessive noise;
- a controlled way to contact creators;
- blocking and reporting controls for safety.

### 6.2 Creator

A creator is an admin-approved account that can publish videos or images. Creators may be individual filmmakers, studios, brands, organizations, or institutions.

Creator needs:

- a public identity page;
- ability to upload video or image content;
- title, description, category, optional thumbnail, and badges/metadata;
- ability to delete comments on their own content;
- basic visibility through feed, profile, search, and discover;
- notifications for meaningful audience interactions;
- ability to receive and respond to viewer messages;
- blocking and reporting controls for abusive conversations.

### 6.3 Admin

An admin manages users, creator access, reports, content removal, comment removal, reported messages, and basic analytics through `web/admin`.

Admin needs:

- user and creator management;
- report review;
- content and comment removal;
- reported-message review;
- basic analytics;
- enough control to protect platform quality without building a complex moderation system;
- privacy-conscious access that does not expose all private conversations by default.

### 6.4 MVP Role Model

| Role | Description | MVP Capabilities |
|---|---|---|
| Guest | Unauthenticated visitor | Browse feed, view creators, view comments, open discover/search. Cannot interact, receive account notifications, or use chat. |
| Viewer | Authenticated user | Like, save, comment, reply, follow, report, edit basic profile, receive notifications, and start one-to-one text conversations with approved creators. |
| Creator | Admin-approved user | Viewer capabilities plus upload video/image content, manage comments on own content, receive viewer messages, and respond in one-to-one conversations. |
| Admin | Internal operator | Manage users, creators, reports, videos, comments, reported messages, notification templates for system notices, and basic analytics. |

Moderator and Super Admin are not part of the MVP role model. They may be introduced later if operational needs justify them.

### 6.5 MVP Messaging Relationship

The MVP uses a **creator-centered chat model**:

- authenticated viewers may start a conversation with an approved creator;
- creators may reply to viewers who have contacted them;
- creators may continue an existing conversation;
- general viewer-to-viewer messaging is not supported;
- group conversations are not supported;
- blocked users cannot exchange new messages;
- either participant may report a message or conversation.

This limitation reduces spam, safety risk, and moderation cost while still validating whether direct creator communication adds value.

---

## 7. Design Source and MVP Surfaces

The Figma mock package is the primary design reference for the MVP. Product and engineering should follow it where possible, while allowing implementation-level adjustments when there is a clear technical or usability reason.

### 7.1 Required MVP Screens

All current Figma screens are considered part of the MVP scope.

| Screen | MVP Status | Product Purpose |
|---|---:|---|
| Splash / Launch | Required | Brand entry and authentication state check. |
| Gesture Intro | Required | First-use explanation of browsing gestures. |
| Sign In | Required | Returning-user authentication. |
| Sign Up / Create Account | Required | Account creation. |
| Feed Playing | Required | Primary content consumption state. |
| Feed Pause | Required | Playback pause and timeline control state. |
| Comments | Required | Comment and reply experience for content. |
| Create / Upload | Required | Creator upload entry point. |
| Creator Profile | Required | Public creator identity and content collection. |
| My Account | Required | User-owned profile/account area. |
| Search / Discover | Required | Browse by videos, creators, and categories. |
| Notification Center | Required | Display in-app notifications and unread state. |
| Conversations | Required | Display active creator conversations and unread message counts. |
| Chat Room | Required | One-to-one text messaging between a viewer and a creator. |

### 7.2 Additional MVP or Near-MVP Screens

| Screen / Flow | MVP Decision |
|---|---|
| Forgot Password | Required or near-MVP because email/password auth is supported. |
| Edit Profile | Required for name, avatar, and bio. |
| Basic Settings | Required at a simple level. |
| Notification Permission Prompt | Required when push notifications are supported by the device. |
| Notification Preferences | Near-MVP; simple global push toggle is enough. |
| Report Content | Required for videos, comments, accounts, and messages. |
| Block User | Required from chat and profile safety actions. |
| Admin Login | Required for admin dashboard. |
| Upload Status | Backend internal state required; full user-facing status screen is not required in MVP. |

---

## 8. Core Product Experience

### 8.1 First App Open

On app launch, MINIS shows a branded splash screen and checks authentication state.

Expected routing:

1. If this is the first time the app is opened on the device, show Gesture Intro.
2. If the user is authenticated, route to Feed.
3. If the user is not authenticated, route to Guest Feed.
4. When a guest attempts a restricted action, show Login / Sign Up entry.
5. After authentication, the app may request push-notification permission at a contextually appropriate time rather than immediately on first launch.

Gesture Intro appears only once per device/app install and can be skipped.

### 8.2 Guest Browsing

Guests can browse content before creating an account. This reduces friction and lets the product demonstrate value immediately.

Guests can:

- view the feed;
- watch videos;
- view image posts;
- view creator profiles;
- view comments;
- use search/discover.

Guests cannot:

- like;
- save;
- comment;
- reply;
- follow;
- upload;
- report;
- use chat;
- access account-specific notifications.

Restricted actions should prompt authentication without breaking the browsing session.

### 8.3 Authenticated Viewer Experience

Authenticated viewers can interact with content and creators.

Viewer capabilities:

- like content;
- save content;
- comment;
- reply to comments;
- follow creators;
- report videos, comments, accounts, and messages;
- edit name, avatar, and bio;
- view saved videos and liked videos;
- receive in-app and push notifications;
- start a one-to-one text conversation with an approved creator;
- read conversation history;
- mark messages as read;
- block a user;
- manage a simple global push-notification preference.

### 8.4 Creator Experience

Creators are not self-selected. In the MVP, Admins manually grant Creator access.

Creators can:

- upload video or image content;
- provide title, description, category, and optional thumbnail;
- publish content after basic processing succeeds;
- manage comments on their own content by deleting inappropriate comments;
- maintain a creator profile with avatar/logo, name, bio, follower count, videos count, verified badge, videos, and external links;
- receive notifications for new followers, comments, replies, new messages, and relevant admin/system actions;
- receive and respond to one-to-one viewer conversations;
- block and report abusive users or messages.

### 8.5 Admin Experience

Admins operate through `web/admin`, a separate web frontend that consumes the same backend APIs.

Admin capabilities in MVP:

- manage users;
- grant/revoke creator access;
- remove videos;
- remove comments;
- review reports;
- manage creators;
- review reported message content and surrounding context;
- issue limited system/admin notifications when necessary;
- view basic analytics.

Admins must not have unrestricted default access to browse every private conversation. Message content should become visible to admins only when required for a report, safety investigation, legal obligation, or explicitly authorized support workflow.

The MVP does not require video approval/rejection workflow because only trusted admin-approved creators can publish. This may change later if the platform opens creator access more broadly.

---

## 9. Feed Requirements

### 9.1 Feed Behavior

| ID | Requirement | Priority |
|---|---|---:|
| FEED-001 | The app shall provide an infinite-scrolling feed. | P0 |
| FEED-002 | The backend shall serve feed items using pagination. | P0 |
| FEED-003 | The feed shall support video posts and image posts in MVP. | P0 |
| FEED-004 | Videos shall auto-play when they become the active feed item. | P0 |
| FEED-005 | Audio behavior may respect app/device state or last user preference to avoid disruptive playback. | P1 |
| FEED-006 | The first MVP feed ordering shall be Latest. | P0 |
| FEED-007 | Removed or hidden content shall not appear in the normal feed. | P0 |
| FEED-008 | A creator profile or feed action may provide an entry point to message the creator when the viewer is authenticated. | P1 |

### 9.2 Feed Item UI

Feed items should follow the Figma layout as closely as possible.

Required visible or accessible elements:

- creator avatar;
- creator name;
- video/image title;
- short description;
- like button;
- comment button;
- share button;
- save button;
- follow button;
- category;
- quality badge such as `4K` where applicable;
- badges such as `AI` or other metadata badges where applicable;
- subtitle indicator or displayed subtitles when available.

Duration and view count should not be permanently forced into the primary viewing overlay. Duration may appear when the user pauses or interacts with playback controls. View count may appear in secondary locations such as profile grids, detail areas, or analytics-like surfaces rather than always on the feed overlay.

### 9.3 Pause State

Pause state is a playback control state, not a separate detail page.

When a user pauses a video:

- playback stops;
- the same feed UI remains visible;
- a playback timeline/progress control may appear;
- the user may seek to another point in the video;
- no large detail panel should replace the viewing experience in MVP.

---

## 10. Media and Content Requirements

### 10.1 Supported Media Types

| Media Type | MVP Support | Notes |
|---|---:|---|
| Video | Yes | Primary content format. |
| Image | Yes | Supported in first version. |
| Live Stream | No | Deferred. |
| Advanced streaming formats | No | HLS/DASH and adaptive streaming are deferred. |
| Chat media attachment | No | Chat supports text only in MVP. |

### 10.2 Video Duration

The preferred video duration is up to **90 seconds**.

This should be treated as a product guideline for MVP rather than an aggressively strict rule unless engineering, moderation, or product quality requires enforcement.

### 10.3 Content Quality Direction

MINIS accepts content that provides value, even when it is not directly educational.

Accepted directions include:

- Education;
- Technology;
- Business;
- Health;
- Science;
- History;
- Skills;
- Mini Documentaries;
- Storytelling;
- Career Advice;
- Life Tips;
- Cinematic Stories;
- Creative Filmmaking.

The content policy should be broad enough to include creativity and storytelling, but narrow enough to reject spammy, misleading, harmful, abusive, or low-value posts.

### 10.4 Upload Metadata

Creators must provide:

- title;
- description;
- category;
- media file: video or image.

Optional:

- thumbnail.

If a thumbnail is not provided, the system may use a generated frame from the video or a placeholder, depending on implementation capability.

### 10.5 Badges and Subtitles

The product should support content-level metadata badges such as:

- `4K`;
- `AI`;
- other future badges defined by product/admin rules.

Subtitles should be supported when available. Automatic subtitle generation is not required for MVP.

---

## 11. Upload and Publishing Lifecycle

### 11.1 MVP Publishing Model

In the MVP, all creators are trusted accounts approved manually by Admins. Therefore, submitted content is published after upload and processing succeed. Manual pre-publication review is not required in MVP.

Future versions may introduce pending review, rejection states, creator applications, and trust tiers when creator access expands.

### 11.2 Internal Upload States

Upload state tracking is required in the backend even if the full status UI is not shown to users.

MVP internal states:

| State | Meaning |
|---|---|
| `uploading` | Media is being uploaded or received. |
| `processing` | Backend is preparing or validating media. |
| `published` | Content is available in feed, profile, search, and discover. |
| `failed` | Upload or processing failed. |
| `removed` | Content was removed by admin. |

Not required in MVP:

- `pending_review`;
- `rejected`.

These may be introduced when untrusted creators or public creator applications are supported.

---

## 12. Authentication and Account Requirements

### 12.1 Authentication Provider

The MVP uses Firebase Authentication for identity and sign-in flows.

Supported methods:

- Email / Password;
- Google Sign-In;
- Apple Sign-In.

Firebase is responsible for authentication only. The Spring Boot backend remains responsible for product identity, user profile, roles, creator access, permissions, videos, comments, reports, feed behavior, notifications, chat authorization, admin capabilities, and business logic.

Firebase Cloud Messaging may be used for push delivery, but the backend remains the source of truth for notification creation, persistence, authorization, unread state, and delivery attempts.

### 12.2 Account Requirements

| ID | Requirement | Priority |
|---|---|---:|
| AUTH-001 | Users shall be able to sign up using supported Firebase methods. | P0 |
| AUTH-002 | Users shall be able to sign in using supported Firebase methods. | P0 |
| AUTH-003 | The app shall support password recovery for email/password accounts. | P1 |
| AUTH-004 | The backend shall create or maintain an application user profile after authentication. | P0 |
| AUTH-005 | Newly registered users shall start as Viewer accounts. | P0 |
| AUTH-006 | Creator access shall be granted only by Admins in MVP. | P0 |
| AUTH-007 | All notification and chat operations shall require backend authorization based on the authenticated application user. | P0 |
| AUTH-008 | Device push tokens shall be associated with authenticated users and revocable on logout or invalidation. | P0 |

### 12.3 Profile Editing

In MVP, users can edit:

- name;
- avatar;
- bio.

Additional profile fields are deferred.

---

## 13. Social Interaction Requirements

### 13.1 Likes and Saves

Authenticated users can like and save content. Guests can view content but must sign in before liking or saving.

| ID | Requirement | Priority |
|---|---|---:|
| SOCIAL-001 | Authenticated users shall be able to like content. | P0 |
| SOCIAL-002 | Authenticated users shall be able to save content. | P0 |
| SOCIAL-003 | Users shall be able to view saved content from My Account. | P1 |
| SOCIAL-004 | Users shall be able to view liked content from My Account. | P1 |

Likes do not generate notifications in the MVP to reduce noise and notification fan-out.

### 13.2 Follow

Follow is included in MVP.

| ID | Requirement | Priority |
|---|---|---:|
| SOCIAL-010 | Authenticated users shall be able to follow creators. | P0 |
| SOCIAL-011 | Creator profiles shall show follower count. | P1 |
| SOCIAL-012 | A new follow shall create an in-app notification for the followed creator. | P0 |
| SOCIAL-013 | A new follow may create a push notification if the creator has push enabled. | P1 |
| SOCIAL-014 | The system should avoid creating duplicate follow notifications for the same active follow relationship. | P1 |

### 13.3 Share

Share button exists in the feed UI. The MVP should support a basic share action if feasible. Advanced share analytics and deep-linking behavior can be defined separately.

---

## 14. Comments and Replies Requirements

### 14.1 Comment Access

Guests can view comments. Only authenticated users can create comments or replies.

| ID | Requirement | Priority |
|---|---|---:|
| COMM-001 | Users shall be able to open comments for a feed item. | P0 |
| COMM-002 | Guests shall be able to read comments. | P0 |
| COMM-003 | Authenticated users shall be able to add comments. | P0 |
| COMM-004 | Authenticated users shall be able to reply to comments. | P0 |
| COMM-005 | Users shall be able to report comments. | P0 |
| COMM-006 | A new comment shall notify the content creator unless the creator created the comment. | P0 |
| COMM-007 | A reply shall notify the parent comment author unless the author created the reply. | P0 |

### 14.2 Comment Moderation

| ID | Requirement | Priority |
|---|---|---:|
| COMM-010 | Creators shall be able to delete comments on their own content. | P0 |
| COMM-011 | Admins shall be able to delete any comment. | P0 |
| COMM-012 | Reported comments shall appear in admin report review. | P0 |
| COMM-013 | Automatic offensive-content filtering is not required in MVP. | P2 |
| COMM-014 | Notifications pointing to removed comments or content shall fail gracefully and avoid exposing removed content. | P0 |

---

## 15. Notification Requirements

### 15.1 Notification Channels

The MVP supports:

- **In-app notifications:** stored by the backend and displayed in Notification Center;
- **Push notifications:** delivered through Firebase Cloud Messaging and the relevant mobile platform integration.

In-app notification state is authoritative. Push delivery is best-effort and must not be treated as the only record of a notification.

### 15.2 MVP Notification Types

| Type | Recipient | Trigger | MVP |
|---|---|---|---:|
| `NEW_FOLLOWER` | Creator | A viewer follows the creator. | P0 |
| `NEW_COMMENT` | Content creator | A user comments on the creator's content. | P0 |
| `COMMENT_REPLY` | Comment author | A user replies to the author's comment. | P0 |
| `NEW_MESSAGE` | Conversation participant | A new chat message is stored for the recipient. | P0 |
| `CONTENT_REMOVED` | Creator | Admin removes the creator's content. | P1 |
| `ACCOUNT_NOTICE` | User | Admin/system sends an important account notice. | P1 |

The MVP does not send notifications for every like, video view, profile view, or low-value activity.

### 15.3 Notification Functional Requirements

| ID | Requirement | Priority |
|---|---|---:|
| NOTIF-001 | The backend shall persist every in-app notification before attempting push delivery. | P0 |
| NOTIF-002 | Authenticated users shall be able to retrieve notifications using pagination. | P0 |
| NOTIF-003 | Notifications shall display read/unread state. | P0 |
| NOTIF-004 | Users shall be able to mark one notification as read. | P0 |
| NOTIF-005 | Users shall be able to mark all notifications as read. | P1 |
| NOTIF-006 | The app shall display an unread notification count or badge. | P0 |
| NOTIF-007 | Tapping a notification shall deep-link to the relevant supported screen when the target still exists. | P0 |
| NOTIF-008 | The backend shall avoid notifying users about their own actions. | P0 |
| NOTIF-009 | Invalid or expired push tokens shall be disabled or removed. | P0 |
| NOTIF-010 | Push delivery failure shall not roll back the product action that caused the notification. | P0 |
| NOTIF-011 | Users shall have a simple global control to enable or disable push notifications where platform rules allow. | P1 |
| NOTIF-012 | In-app notifications may remain available even when push notifications are disabled. | P0 |
| NOTIF-013 | The system shall support idempotent notification creation for retried domain events. | P0 |
| NOTIF-014 | Notification payloads shall not expose sensitive private message text on lock screens by default. | P0 |

### 15.4 Notification Delivery Behavior

- Notification creation must be asynchronous relative to the main user action when practical.
- Failure to send a push notification must not fail a successful comment, follow, reply, or message operation.
- Repeated retries must not create duplicate in-app notifications.
- The backend should track delivery attempt status for observability.
- Push content should be concise and privacy-aware.
- The app should fetch authoritative data after opening a notification rather than trusting stale push payload data.

### 15.5 Deferred Notification Capabilities

Not required in MVP:

- marketing campaigns;
- recommendation-based notifications;
- scheduled digest notifications;
- per-notification-type preference matrix;
- email notifications;
- SMS notifications;
- advanced aggregation such as “20 people followed you”;
- notification experimentation platform;
- complex localization management.

---

## 16. Chat and Direct Messaging Requirements

### 16.1 MVP Chat Definition

Chat is included as **MVP Lite**.

The MVP supports:

- one-to-one conversations only;
- authenticated viewer-to-creator messaging;
- text messages only;
- conversation history;
- unread counts;
- sent and read state;
- blocking;
- reporting;
- new-message notifications.

The MVP does not support:

- viewer-to-viewer conversations;
- group chat;
- images, videos, files, GIFs, or stickers;
- voice messages;
- audio/video calls;
- typing indicators;
- online status or last seen;
- reactions;
- message editing;
- delete-for-everyone;
- message forwarding;
- disappearing messages.

### 16.2 Conversation Rules

| ID | Requirement | Priority |
|---|---|---:|
| CHAT-001 | An authenticated viewer shall be able to start a one-to-one conversation with an approved creator. | P0 |
| CHAT-002 | A creator shall be able to respond to viewers in existing conversations. | P0 |
| CHAT-003 | Only one active conversation shall exist for the same viewer-creator pair. | P0 |
| CHAT-004 | General viewer-to-viewer messaging shall not be supported in MVP. | P0 |
| CHAT-005 | Users shall not be able to send new messages when either participant has blocked the other. | P0 |
| CHAT-006 | Removed, suspended, or deleted accounts shall not be able to send new messages. | P0 |
| CHAT-007 | Revoking Creator access shall prevent new creator-centered conversations unless an admin policy explicitly preserves existing support conversations. | P1 |

### 16.3 Message Requirements

| ID | Requirement | Priority |
|---|---|---:|
| CHAT-010 | Messages shall support plain text only. | P0 |
| CHAT-011 | Empty or whitespace-only messages shall be rejected. | P0 |
| CHAT-012 | Message length shall be limited to a configurable maximum, initially 2,000 characters. | P0 |
| CHAT-013 | The backend shall assign a stable unique identifier and server timestamp to every accepted message. | P0 |
| CHAT-014 | Message history shall be returned using cursor-based pagination. | P0 |
| CHAT-015 | The sender shall receive confirmation when the backend has accepted and stored the message. | P0 |
| CHAT-016 | The recipient shall be able to mark messages as read. | P0 |
| CHAT-017 | The system shall store read timestamps or equivalent read state. | P0 |
| CHAT-018 | Sending a message shall create a `NEW_MESSAGE` notification for the recipient. | P0 |
| CHAT-019 | Message retries from the client shall support idempotency to avoid duplicate stored messages. | P0 |
| CHAT-020 | Messages shall be displayed in deterministic order using server-controlled ordering fields. | P0 |
| CHAT-021 | Users shall be able to report an individual message or the conversation. | P0 |
| CHAT-022 | Users shall be able to block the other participant from the conversation screen. | P0 |
| CHAT-023 | The app shall display unread message counts in the conversation list. | P0 |
| CHAT-024 | The MVP shall support near-real-time message delivery when the app is active. | P0 |
| CHAT-025 | If real-time delivery is temporarily unavailable, stored messages shall remain retrievable through refresh or reconnect. | P0 |

### 16.4 Real-Time Delivery Direction

The product requires near-real-time behavior but does not lock the implementation to a specific transport in the PRD.

Acceptable implementation options include:

- WebSocket;
- Server-Sent Events combined with HTTP writes;
- a managed real-time delivery service;
- short polling as a temporary fallback, not the preferred steady-state design.

The backend database remains the source of truth for accepted messages. Real-time transport is a delivery optimization, not the durable message store.

### 16.5 Chat Safety and Abuse Controls

| ID | Requirement | Priority |
|---|---|---:|
| CHAT-SAFE-001 | The backend shall enforce per-user and per-conversation message rate limits. | P0 |
| CHAT-SAFE-002 | Users shall be able to block another participant. | P0 |
| CHAT-SAFE-003 | Blocking shall prevent new messages in both directions for the blocked relationship. | P0 |
| CHAT-SAFE-004 | Users shall be able to report messages with a predefined reason and optional explanation. | P0 |
| CHAT-SAFE-005 | Admins shall review only reported message content and sufficient surrounding context. | P0 |
| CHAT-SAFE-006 | The platform shall not expose private conversations publicly. | P0 |
| CHAT-SAFE-007 | Message APIs shall verify that the authenticated user belongs to the conversation. | P0 |
| CHAT-SAFE-008 | Push notifications for chat shall avoid exposing full sensitive message text by default. | P0 |
| CHAT-SAFE-009 | The system shall record moderation and admin actions in an audit trail. | P0 |
| CHAT-SAFE-010 | Automatic AI-based message moderation is not required in MVP. | P2 |

### 16.6 Message Removal and Retention

For MVP:

- users cannot edit sent messages;
- users cannot delete a message for everyone;
- the product may allow local hiding later, but it is not required;
- admins may remove reported messages from user-facing access when required;
- exact long-term retention and account-deletion behavior remain a deferred policy decision;
- moderation and audit requirements may require preserving limited records even after user-facing removal.

---

## 17. Creator Profile Requirements

Creator profiles are first-class discovery and communication surfaces.

| ID | Requirement | Priority |
|---|---|---:|
| PROF-001 | Creator profile shall show avatar or logo. | P0 |
| PROF-002 | Creator profile shall show creator name. | P0 |
| PROF-003 | Creator profile shall show bio. | P0 |
| PROF-004 | Creator profile shall show follower count. | P1 |
| PROF-005 | Creator profile shall show videos count. | P1 |
| PROF-006 | Creator profile shall show creator content. | P0 |
| PROF-007 | Creator profile shall support verified badge. | P1 |
| PROF-008 | Creator profile shall support external links. | P1 |
| PROF-009 | Authenticated viewers shall have a visible action to start or open a conversation with the creator. | P0 |
| PROF-010 | Guests who attempt to message a creator shall be prompted to authenticate. | P0 |
| PROF-011 | The message action shall be unavailable when the relationship is blocked or the creator cannot receive messages. | P0 |

Creators may represent individuals, filmmakers, studios, brands, companies, or institutions.

---

## 18. My Account Requirements

My Account is the user-owned profile and settings area.

MVP content:

- name;
- email;
- avatar;
- bio;
- saved videos;
- liked videos;
- my uploads, if the user is a Creator;
- notifications entry;
- conversations entry;
- simple push-notification preference;
- blocked users management or access point;
- settings;
- logout.

Delete Account is deferred until product and engineering define data-retention behavior for uploaded content, comments, likes, saves, reports, notifications, conversations, messages, and related audit records.

---

## 19. Search and Discover Requirements

### 19.1 Search Scope

Search includes:

- videos/posts;
- creators;
- categories.

Private conversations and messages are never part of public search.

### 19.2 Discover

Discover includes categories to help users find valuable content without relying on a recommendation engine.

### 19.3 MVP Search Implementation Direction

PostgreSQL search is sufficient for the MVP. A separate search engine such as Elasticsearch or Meilisearch is not required for the first version.

Search can be revisited when content volume, latency, ranking quality, typo tolerance, or semantic discovery becomes a real constraint.

---

## 20. Reporting and Moderation Requirements

### 20.1 Reportable Entities

Authenticated users can report:

- videos/posts;
- comments;
- accounts;
- individual chat messages;
- conversations.

Guests must sign in to report.

### 20.2 Report Reasons

Reports should support predefined reasons plus an optional free-text explanation.

Initial reasons:

- Spam;
- Misleading;
- Harmful;
- Low Quality;
- Harassment;
- Copyright;
- Impersonation;
- Unwanted Messages;
- Other.

### 20.3 Admin Moderation Actions

Admins can:

- review reports;
- remove videos/posts;
- remove comments;
- manage users;
- manage creators;
- review reported messages and limited surrounding context;
- remove reported messages from user-facing access when justified;
- suspend messaging capability for abusive accounts;
- suspend or remove accounts;
- record moderation decisions and actions.

The MVP does not require a full moderation workflow with escalations, queues by severity, reviewer assignment, automated risk scoring, or AI moderation. Those are future improvements.

### 20.4 Privacy Rules for Message Moderation

- Admins do not receive unrestricted access to all private conversations.
- A message report may expose the reported message and a limited number of nearby messages required to understand context.
- Access to reported message data must be authorized and auditable.
- Message content must not appear in general analytics dashboards.
- Moderation exports and support access are outside MVP unless explicitly approved.

---

## 21. Admin Dashboard Requirements

The admin dashboard is a separate web frontend under `web/admin`. It is not part of the Flutter mobile app.

### 21.1 MVP Admin Capabilities

| Capability | MVP Status |
|---|---:|
| Admin Login | Required |
| Manage Users | Required |
| Manage Creators | Required |
| Remove Videos / Posts | Required |
| Review Reports | Required |
| Remove Comments | Required |
| Review Reported Messages | Required |
| Suspend User Messaging | Required |
| Send Important Account/System Notice | Near-MVP |
| Basic Analytics | Required |
| Approve / Reject Videos | Not required in MVP |
| Browse All Private Conversations | Not allowed by default |

### 21.2 Admin Analytics

Basic analytics should help operators understand whether the product is working and whether content quality is acceptable.

Suggested MVP analytics:

- total users;
- active users;
- total creators;
- content uploaded;
- videos watched;
- likes;
- comments;
- reports;
- removed videos/comments;
- notifications created;
- notification open rate;
- active conversations;
- messages sent;
- message delivery failures;
- reported messages;
- messaging suspensions.

Advanced analytics dashboards are not required.

---

## 22. Analytics Events

The MVP should capture only essential events needed to evaluate the experience and support future ranking improvements.

| Event | Trigger |
|---|---|
| `video_played` | Active video starts playback. |
| `video_paused` | User pauses video. |
| `video_completed` | User reaches the end of a video. |
| `video_skipped` | User scrolls away before completion. |
| `video_liked` | User likes content. |
| `comment_created` | User creates a comment. |
| `profile_opened` | User opens a creator/user profile. |
| `upload_submitted` | Creator submits content upload. |
| `report_created` | User reports video, comment, account, message, or conversation. |
| `follow_created` | User follows a creator. |
| `save_created` | User saves content. |
| `notification_created` | Backend creates an in-app notification. |
| `notification_opened` | User opens a notification. |
| `push_delivery_failed` | Push provider reports a terminal delivery failure or invalid token. |
| `conversation_started` | Viewer starts a creator conversation. |
| `message_sent` | Backend accepts and stores a message. |
| `message_read` | Recipient marks one or more messages as read. |
| `user_blocked` | User blocks another participant. |
| `message_reported` | User reports a message or conversation. |

Private message text must not be copied into general analytics events.

The MVP does not need advanced analytics, ML feature stores, recommendation pipelines, or event warehouses. Event collection should be designed cleanly enough that those systems can be added later.

---

## 23. Functional Requirements Summary

| Area | P0 Requirements |
|---|---|
| Launch | Splash screen, auth-state check, first-use gesture intro. |
| Auth | Firebase auth with Email/Password, Google, Apple. |
| Guest Mode | Browse feed, comments, creators, search/discover. |
| Feed | Infinite scroll, paginated backend, video/image posts, autoplay, Latest ordering. |
| Playback | Play, pause, seek/progress controls. |
| Content | Title, description, category, optional thumbnail, badges, subtitles when available. |
| Interaction | Like, save, comment, reply, follow, report. |
| Upload | Creator-only upload of video/image with required metadata. |
| Creator Profile | Identity, bio, content, followers, verified badge, links, message action. |
| My Account | Profile basics, saved, liked, creator uploads, notifications, conversations, settings, logout. |
| Search/Discover | Search videos, creators, categories; browse categories. |
| Notifications | In-app notifications, unread state, push delivery, deep links, notification badge. |
| Chat | Viewer-to-creator one-to-one text chat, history, unread counts, read state, blocking, reporting. |
| Moderation | Report videos/comments/accounts/messages; admin removal and messaging suspension. |
| Admin | Manage users, creators, reports, content, comments, reported messages, and basic analytics. |

---

## 24. Non-Functional Product Requirements

| ID | Category | Requirement |
|---|---|---|
| NFR-001 | Performance | Feed transitions and playback startup should feel responsive on normal mobile networks. |
| NFR-002 | Reliability | Feed should degrade gracefully if secondary counts, notifications, chat delivery, or analytics events fail. |
| NFR-003 | Security | Authenticated actions must require valid identity and backend authorization. |
| NFR-004 | Authorization | Creator and Admin capabilities must be controlled by backend roles, not by client state. |
| NFR-005 | Privacy | Guest browsing should not expose private user information, and private messages must be accessible only to authorized participants and approved moderation workflows. |
| NFR-006 | Moderation | Removed content, comments, and messages must not appear in normal user-facing surfaces. |
| NFR-007 | Maintainability | Product modules should remain separable: feed, users, creators, uploads, comments, notifications, chat, reports, and admin. |
| NFR-008 | Observability | Upload failures, playback issues, notification failures, message delivery failures, report creation, and admin actions should be traceable. |
| NFR-009 | Accessibility | Text, controls, captions/subtitles, contrast, notification labels, and chat controls should be considered during UI implementation. |
| NFR-010 | Scalability | MVP design should not block future recommendation, CDN/media optimization, notification fan-out improvements, chat scaling, or search improvements. |
| NFR-011 | Notification Reliability | Push is best-effort; in-app notification persistence must remain reliable and idempotent. |
| NFR-012 | Chat Durability | A message acknowledged as sent must be durably stored before success is returned. |
| NFR-013 | Chat Ordering | Message order must be deterministic under retries, reconnects, and concurrent sends. |
| NFR-014 | Abuse Prevention | Chat and notification-triggering actions must be rate-limited and protected from spam. |
| NFR-015 | Delivery Recovery | Reconnect or refresh must recover messages missed during temporary real-time transport failure. |
| NFR-016 | Data Minimization | Push payloads, logs, analytics, and admin surfaces must avoid exposing unnecessary private message content. |
| NFR-017 | Auditability | Admin moderation, messaging suspension, and reported-message access must be recorded. |
| NFR-018 | Mobile Token Lifecycle | Push tokens must support registration, replacement, logout cleanup, and invalid-token removal. |

---

## 25. Release Scope

### 25.1 MVP Release

The MVP release includes:

- mobile-first Flutter app;
- Firebase authentication;
- guest feed browsing;
- video and image content;
- latest-ordered feed;
- creator profiles;
- my account;
- basic profile editing;
- creator-only upload;
- comments and replies;
- likes, saves, follows;
- search/discover with categories;
- reporting;
- in-app notifications;
- push notifications for supported MVP events;
- notification center and unread badge;
- creator-centered one-to-one text chat;
- conversations list and unread message counts;
- message read state;
- blocking and message reporting;
- simple admin dashboard;
- reported-message review;
- basic analytics events.

### 25.2 Post-MVP Roadmap

| Phase | Candidate Features |
|---|---|
| V1.1 | Better notification preferences, notification aggregation, improved upload status UI, better content analytics, improved profile settings, chat quality improvements. |
| V1.2 | Creator application system, approval/rejection workflow, trust tiers, richer moderation tooling, message requests or creator inbox controls. |
| V1.3 | Better ranking using watch completion, likes, saves, follows, and reports. |
| V1.4 | Advanced media delivery, CDN optimization, adaptive streaming if needed, improved notification fan-out infrastructure. |
| V1.5 | Captions workflow, improved subtitles, optional automated subtitle generation. |
| Future | Monetization, creator payouts, viewer-to-viewer chat, group chat, chat attachments, voice messages, calls, playlists, advanced search, AI moderation, advanced recommendation. |

---

## 26. Risks and Mitigations

| Risk | Impact | Mitigation |
|---|---|---|
| Product is perceived as another short-video clone | Weak differentiation | Position clearly around mini films, value, creator identity, and cinematic quality. |
| Content supply is too low | Feed feels empty | Start with trusted creators and curated content. |
| Quality bar is unclear | Feed becomes inconsistent | Define content quality guidelines and use admin-approved creators. |
| Guest mode reduces signups | Users browse without converting | Require login for interaction while keeping browsing friction low. |
| Direct publishing causes bad content | Quality/safety issues | Limit upload to trusted creators and keep report/admin removal tools ready. |
| Basic video delivery is not enough | Playback issues as usage grows | Keep media architecture replaceable; defer HLS/CDN optimization until needed. |
| Latest feed becomes weak over time | Poor relevance | Use MVP analytics to inform later ranking. |
| Comment abuse | Safety and moderation burden | Allow reports, creator deletion on own content, and admin deletion globally. |
| Notification fatigue | Users disable notifications or uninstall | Notify only on high-value events, avoid like notifications, and provide a simple push toggle. |
| Push delivery is unreliable | Users miss updates | Persist in-app notifications first and treat push as best-effort. |
| Chat increases abuse and spam | Safety and support burden | Restrict chat to viewer-to-creator, use blocking, reporting, rate limits, and admin messaging suspension. |
| Chat expands into a full messaging product | Scope and delivery risk | Lock MVP to one-to-one text, no attachments, no groups, no calls, no advanced presence. |
| Real-time transport fails | Delayed or missing live updates | Persist before acknowledgment, reconnect, and recover through history refresh. |
| Private messages are overexposed internally | Privacy and trust damage | Restrict admin access to reported content and audit moderation access. |
| Duplicate messages or notifications appear | Poor user experience | Require client idempotency keys and backend deduplication. |
| Scope creep | MVP delays | Keep advanced notifications, general messaging, live, monetization, advanced search, and recommendation out of MVP. |

---

## 27. Product Decisions Locked for MVP

| Decision | Final MVP Direction |
|---|---|
| Product type | Mini Films Platform. |
| Tagline | Mini films with real value. |
| Content philosophy | Valuable, high-quality, cinematic, creative, educational, documentary, or meaningful content. |
| Design source | Figma is the primary reference, with technical adjustment allowed. |
| Auth | Firebase Authentication. |
| Guest access | Guests can browse; interactions require login. |
| Roles | Guest, Viewer, Creator, Admin. |
| Creator access | Admin manually grants Creator role. |
| Upload access | Creator-only. |
| Media types | Video and image in MVP. |
| Feed ordering | Latest. |
| Feed style | Infinite scroll with backend pagination. |
| Comments | Included with replies. |
| Follow | Included. |
| Notifications | Included: in-app plus push for selected high-value events. |
| Notification source of truth | Backend-persisted in-app notification. |
| Chat | Included as MVP Lite. |
| Chat relationship | Authenticated viewers may message approved creators; creators may respond. |
| Chat format | One-to-one, text only. |
| General viewer-to-viewer chat | Deferred. |
| Chat attachments, groups, voice, calls | Deferred. |
| Message moderation | Blocking, reporting, rate limits, and admin review of reported content. |
| Search | PostgreSQL search for MVP. |
| Admin dashboard | Included, simple version. |
| Video review | Not required in MVP because creators are trusted. |
| Recommendation engine | Deferred. |
| Live streaming | Deferred. |

---

## 28. Deferred Decisions

These are intentionally not finalized in the MVP PRD and should be decided only when the related feature becomes active.

| Decision | Deferred Until |
|---|---|
| Exact long-term aspect-ratio policy | After testing Figma-based layout with real content. |
| Advanced audio autoplay rules | During mobile implementation and QA. |
| Delete Account behavior | Before adding account deletion, including notification and message retention rules. |
| Public creator application workflow | When opening the platform beyond admin-approved creators. |
| Full moderation workflow | When report volume requires operational workflow. |
| HLS/DASH/adaptive streaming | When media delivery becomes a real bottleneck. |
| Advanced recommendation | After enough engagement events are collected. |
| Advanced search engine | When PostgreSQL search becomes insufficient. |
| Monetization | After product value and retention are validated. |
| Per-notification-type preferences | After notification volume and user feedback justify them. |
| Notification aggregation and digests | After usage volume creates notification noise. |
| Viewer-to-viewer messaging | After creator-centered messaging is validated and moderation capacity increases. |
| Group chat and media attachments | After one-to-one text chat is stable and product need is proven. |
| Message editing and delete-for-everyone | After product, moderation, and audit behavior are defined. |
| Exact message retention period | Before scale, account deletion, legal, or compliance requirements demand a formal policy. |
| Custom end-to-end encryption | Only if the product and threat model require it. |
| Final real-time transport | During architecture design; the PRD requires behavior, not a specific protocol. |

---

## 29. Acceptance Criteria for MVP Readiness

The MVP is considered product-ready when:

- a guest can open the app and browse the feed;
- an authenticated viewer can like, save, comment, reply, follow, and report;
- a creator approved by an admin can upload video or image content;
- uploaded creator content can appear in feed, profile, search, and discover after processing succeeds;
- a user can view creator profiles and account pages;
- search/discover works for videos, creators, and categories;
- admin can manage users, creators, reports, videos, and comments;
- removed videos/comments are hidden from normal user-facing surfaces;
- essential analytics events are captured;
- the product experience matches the Figma direction closely enough for MVP validation;
- a supported interaction creates a persisted in-app notification;
- unread notification count is displayed correctly;
- a push notification is attempted without making the original interaction fail if push delivery fails;
- opening a valid notification routes the user to the relevant screen;
- an authenticated viewer can start a one-to-one text conversation with an approved creator;
- the creator can receive, read, and respond to the message;
- accepted messages remain available after reconnecting or reopening the app;
- duplicate client retries do not create duplicate messages;
- blocked users cannot exchange new messages;
- users can report messages or conversations;
- admins can review reported message content with limited context and record a moderation action;
- private messages are not exposed in public search, general analytics, or unrestricted admin browsing;
- chat and notification failures are observable without breaking the core feed and interaction flows.

---

## 30. MVP Scope Control Notes

Notifications and chat are now part of the MVP, but they must remain bounded.

The following scope rules are mandatory unless the PRD is revised again:

1. Notifications are limited to high-value product events.
2. Push delivery is best-effort; in-app persistence is authoritative.
3. Chat is creator-centered, one-to-one, and text-only.
4. Viewer-to-viewer messaging is not part of MVP.
5. Media attachments, voice notes, calls, groups, presence, reactions, editing, and delete-for-everyone are not part of MVP.
6. Blocking, reporting, rate limiting, authorization, privacy, and auditability are not optional extras; they are part of the minimum safe chat scope.
7. Chat must not delay the core feed, upload, content interaction, and moderation experience beyond the agreed release plan.
