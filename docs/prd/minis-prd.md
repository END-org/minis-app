# MINIS Product Requirements Document

**Document Status:** Draft 1.0  
**Product:** MINIS  
**Repository:** `https://github.com/END-org/minis-app`    
**Primary Platform:** Mobile Application  
**Design Reference:** Figma MVP Mockups  
**Last Updated:** 2026-07-04  
**Document Owner:** Amr Bani Irshid  

---

## 1. Executive Summary

MINIS is a mobile-first **Mini Films Platform** for short, high-quality, valuable visual content. It is not a generic short-video entertainment app and it is not a traditional short-film platform where videos commonly run for 20–30 minutes. The intended content format is a compact mini film: usually around one minute, with some flexibility depending on the creative need.

The product gives filmmakers, creators, studios, brands, and organizations a focused place to publish cinematic, meaningful, and high-signal content. Viewers can open the app, browse a polished feed, watch videos or image-based posts, follow creators, interact through likes, saves, comments, and replies, and discover content by creators and categories.

The MVP should validate whether users engage with a feed of mini films that feel worth their time, while keeping the product simple enough to build, moderate, and operate. The first version prioritizes the core viewer experience, guest browsing, Firebase-based authentication, creator-controlled publishing for approved creators, basic profile and discovery features, comments and replies, reporting, and a lightweight admin dashboard.

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

---

## 3. Problem Statement

Short-form media products are extremely engaging, but they often reward volume, trends, and passive scrolling more than quality or meaning. Valuable short visual work can get buried inside entertainment-first feeds, while creators who care about cinematic storytelling, useful ideas, or compact narratives have limited product surfaces designed specifically for that type of content.

MINIS addresses this by creating a focused mobile platform where mini films, high-quality image posts, creator identity, and meaningful discovery are first-class product concepts.

The MVP will not attempt to solve large-scale recommendation, monetization, advanced video processing, or open creator onboarding. Instead, it will prove the core experience: a user can browse, watch, follow, interact, discover, and return because the content feels valuable.

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

### 4.2 MVP Success Metrics

| Metric | Why it matters |
|---|---|
| Watch Completion Rate | Measures whether mini films are strong enough to keep users watching. |
| Videos Watched Per Session | Measures feed momentum and session depth. |
| Daily Active Users | Measures whether users return to consume content. |
| Published Content Count | Measures whether approved creators are supplying enough content. |
| Report Rate / Quality Issues | Measures whether content quality and safety are under control. |

The MVP should not be judged by sign-up volume alone. The stronger signal is whether users watch, complete, interact with, and return to valuable content.

---

## 5. Non-Goals

The following are explicitly outside the MVP scope.

| ID | Non-Goal | Reason |
|---|---|---|
| NG1 | Live streaming | Adds real-time infrastructure, moderation, latency, and operational complexity. |
| NG2 | Advanced streaming architecture | HLS/DASH, adaptive bitrate ladders, and advanced media delivery are deferred. MVP supports basic video/image playback. |
| NG3 | Advanced recommendation engine | First version uses latest ordering, not ML ranking or personalization. |
| NG4 | Notifications | Important for the full product, but not required for MVP validation. |
| NG5 | Chat / direct messaging | Adds safety, moderation, abuse handling, and real-time complexity. |
| NG6 | Monetization | Ads, subscriptions, tips, creator payouts, and revenue sharing are future work. |
| NG7 | AI moderation | Reports and manual admin actions are sufficient for MVP. |
| NG8 | Video editing tools | Trimming, filters, effects, templates, and in-app editing are deferred. |
| NG9 | Public creator application system | Admin-controlled creator approval is enough for the first version. |
| NG10 | Advanced search engine | PostgreSQL search is sufficient for MVP. |
| NG11 | Download videos | Downloading content is not part of the first version. |
| NG12 | Playlists | Playlists are deferred until the content model matures. |
| NG13 | Full account deletion workflow | Delete Account is deferred because it requires policy decisions around related content and data. |

---

## 6. Target Users and Roles

### 6.1 Viewer

A viewer watches mini films, browses categories, opens creator profiles, reads comments, and may interact after signing in.

Viewer needs:

- fast access to the feed;
- content that feels worth watching;
- ability to browse as a guest;
- simple interactions after login;
- creator profiles and categories for discovery.

### 6.2 Creator

A creator is an admin-approved account that can publish videos or images. Creators may be individual filmmakers, studios, brands, organizations, or institutions.

Creator needs:

- a public identity page;
- ability to upload video or image content;
- title, description, category, optional thumbnail, and badges/metadata;
- ability to delete comments on their own content;
- basic visibility through feed, profile, search, and discover.

### 6.3 Admin

An admin manages users, creator access, reports, content removal, comment removal, and basic analytics through `web/admin`.

Admin needs:

- user and creator management;
- report review;
- content and comment removal;
- basic analytics;
- enough control to protect platform quality without building a complex moderation system.

### 6.4 MVP Role Model

| Role | Description | MVP Capabilities |
|---|---|---|
| Guest | Unauthenticated visitor | Browse feed, view creators, view comments, open discover/search. Cannot interact. |
| Viewer | Authenticated user | Like, save, comment, reply, follow, report, edit basic profile. |
| Creator | Admin-approved user | Viewer capabilities plus upload video/image content and manage comments on own content. |
| Admin | Internal operator | Manage users, creators, reports, videos, comments, and basic analytics. |

Moderator and Super Admin are not part of the MVP role model. They may be introduced later if operational needs justify them.

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

### 7.2 Additional MVP or Near-MVP Screens

| Screen / Flow | MVP Decision |
|---|---|
| Forgot Password | Required or near-MVP because email/password auth is supported. |
| Edit Profile | Required for name, avatar, and bio. |
| Basic Settings | Required at a simple level. |
| Report Content | Required for videos, comments, and accounts. |
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
- report.

Restricted actions should prompt authentication without breaking the browsing session.

### 8.3 Authenticated Viewer Experience

Authenticated viewers can interact with content and creators.

Viewer capabilities:

- like content;
- save content;
- comment;
- reply to comments;
- follow creators;
- report videos, comments, and accounts;
- edit name, avatar, and bio;
- view saved videos and liked videos.

### 8.4 Creator Experience

Creators are not self-selected. In the MVP, Admins manually grant Creator access.

Creators can:

- upload video or image content;
- provide title, description, category, and optional thumbnail;
- publish content after basic processing succeeds;
- manage comments on their own content by deleting inappropriate comments;
- maintain a creator profile with avatar/logo, name, bio, follower count, videos count, verified badge, videos, and external links.

### 8.5 Admin Experience

Admins operate through `web/admin`, a separate web frontend that consumes the same backend APIs.

Admin capabilities in MVP:

- manage users;
- grant/revoke creator access;
- remove videos;
- remove comments;
- review reports;
- manage creators;
- view basic analytics.

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

Firebase is responsible for authentication only. The Spring Boot backend remains responsible for product identity, user profile, roles, creator access, permissions, videos, comments, reports, feed behavior, admin capabilities, and business logic.

### 12.2 Account Requirements

| ID | Requirement | Priority |
|---|---|---:|
| AUTH-001 | Users shall be able to sign up using supported Firebase methods. | P0 |
| AUTH-002 | Users shall be able to sign in using supported Firebase methods. | P0 |
| AUTH-003 | The app shall support password recovery for email/password accounts. | P1 |
| AUTH-004 | The backend shall create or maintain an application user profile after authentication. | P0 |
| AUTH-005 | Newly registered users shall start as Viewer accounts. | P0 |
| AUTH-006 | Creator access shall be granted only by Admins in MVP. | P0 |

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

### 13.2 Follow

Follow is included in MVP.

| ID | Requirement | Priority |
|---|---|---:|
| SOCIAL-010 | Authenticated users shall be able to follow creators. | P0 |
| SOCIAL-011 | Creator profiles shall show follower count. | P1 |
| SOCIAL-012 | Follow shall not trigger notifications in MVP. | P0 |

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

### 14.2 Comment Moderation

| ID | Requirement | Priority |
|---|---|---:|
| COMM-010 | Creators shall be able to delete comments on their own content. | P0 |
| COMM-011 | Admins shall be able to delete any comment. | P0 |
| COMM-012 | Reported comments shall appear in admin report review. | P0 |
| COMM-013 | Automatic offensive-content filtering is not required in MVP. | P2 |

---

## 15. Creator Profile Requirements

Creator profiles are first-class discovery surfaces.

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

Creators may represent individuals, filmmakers, studios, brands, companies, or institutions.

---

## 16. My Account Requirements

My Account is the user-owned profile and settings area.

MVP content:

- name;
- email;
- avatar;
- bio;
- saved videos;
- liked videos;
- my uploads, if the user is a Creator;
- settings;
- logout.

Delete Account is deferred until product and engineering define data-retention behavior for uploaded content, comments, likes, saves, reports, and related audit records.

---

## 17. Search and Discover Requirements

### 17.1 Search Scope

Search includes:

- videos/posts;
- creators;
- categories.

### 17.2 Discover

Discover includes categories to help users find valuable content without relying on a recommendation engine.

### 17.3 MVP Search Implementation Direction

PostgreSQL search is sufficient for the MVP. A separate search engine such as Elasticsearch or Meilisearch is not required for the first version.

Search can be revisited when content volume, latency, ranking quality, typo tolerance, or semantic discovery becomes a real constraint.

---

## 18. Reporting and Moderation Requirements

### 18.1 Reportable Entities

Authenticated users can report:

- videos/posts;
- comments;
- accounts.

Guests must sign in to report.

### 18.2 Report Reasons

Reports should support predefined reasons plus an optional free-text explanation.

Initial reasons:

- Spam;
- Misleading;
- Harmful;
- Low Quality;
- Harassment;
- Copyright;
- Other.

### 18.3 Admin Moderation Actions

Admins can:

- review reports;
- remove videos/posts;
- remove comments;
- manage users;
- manage creators.

The MVP does not require a full moderation workflow with escalations, queues by severity, reviewer assignment, or automated risk scoring. Those are future improvements.

---

## 19. Admin Dashboard Requirements

The admin dashboard is a separate web frontend under `web/admin`. It is not part of the Flutter mobile app.

### 19.1 MVP Admin Capabilities

| Capability | MVP Status |
|---|---:|
| Admin Login | Required |
| Manage Users | Required |
| Manage Creators | Required |
| Remove Videos / Posts | Required |
| Review Reports | Required |
| Remove Comments | Required |
| Basic Analytics | Required |
| Approve / Reject Videos | Not required in MVP |

### 19.2 Admin Analytics

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
- removed videos/comments.

Advanced analytics dashboards are not required.

---

## 20. Analytics Events

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
| `report_created` | User reports video, comment, or account. |
| `follow_created` | User follows a creator. |
| `save_created` | User saves content. |

The MVP does not need advanced analytics, ML feature stores, recommendation pipelines, or event warehouses. Event collection should be designed cleanly enough that those systems can be added later.

---

## 21. Functional Requirements Summary

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
| Creator Profile | Identity, bio, content, followers, verified badge, links. |
| My Account | Profile basics, saved, liked, creator uploads, settings, logout. |
| Search/Discover | Search videos, creators, categories; browse categories. |
| Moderation | Report videos/comments/accounts; admin removal of content/comments. |
| Admin | Manage users, creators, reports, content, comments, basic analytics. |

---

## 22. Non-Functional Product Requirements

| ID | Category | Requirement |
|---|---|---|
| NFR-001 | Performance | Feed transitions and playback startup should feel responsive on normal mobile networks. |
| NFR-002 | Reliability | Feed should degrade gracefully if secondary counts or analytics events fail. |
| NFR-003 | Security | Authenticated actions must require valid identity and backend authorization. |
| NFR-004 | Authorization | Creator and Admin capabilities must be controlled by backend roles, not by client state. |
| NFR-005 | Privacy | Guest browsing should not expose private user information. |
| NFR-006 | Moderation | Removed content and comments must not appear in normal user-facing surfaces. |
| NFR-007 | Maintainability | Product modules should remain separable: feed, users, creators, uploads, comments, reports, admin. |
| NFR-008 | Observability | Upload failures, playback issues, report creation, and admin actions should be traceable. |
| NFR-009 | Accessibility | Text, controls, captions/subtitles, and contrast should be considered during UI implementation. |
| NFR-010 | Scalability | MVP design should not block future recommendation, CDN/media optimization, or search improvements. |

---

## 23. Release Scope

### 23.1 MVP Release

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
- simple admin dashboard;
- basic analytics events.

### 23.2 Post-MVP Roadmap

| Phase | Candidate Features |
|---|---|
| V1.1 | Notifications, improved upload status UI, better content analytics, improved profile settings. |
| V1.2 | Creator application system, approval/rejection workflow, trust tiers, richer moderation tooling. |
| V1.3 | Better ranking using watch completion, likes, saves, follows, and reports. |
| V1.4 | Advanced media delivery, CDN optimization, adaptive streaming if needed. |
| V1.5 | Captions workflow, improved subtitles, optional automated subtitle generation. |
| Future | Monetization, creator payouts, chat, playlists, advanced search, AI moderation, advanced recommendation. |

---

## 24. Risks and Mitigations

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
| Scope creep | MVP delays | Keep notifications, live, monetization, advanced search, and recommendation out of MVP. |

---

## 25. Product Decisions Locked for MVP

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
| Notifications | Deferred. |
| Search | PostgreSQL search for MVP. |
| Admin dashboard | Included, simple version. |
| Video review | Not required in MVP because creators are trusted. |
| Recommendation engine | Deferred. |
| Live streaming | Deferred. |

---

## 26. Deferred Decisions

These are intentionally not finalized in the MVP PRD and should be decided only when the related feature becomes active.

| Decision | Deferred Until |
|---|---|
| Exact long-term aspect-ratio policy | After testing Figma-based layout with real content. |
| Advanced audio autoplay rules | During mobile implementation and QA. |
| Delete Account behavior | Before adding account deletion. |
| Public creator application workflow | When opening the platform beyond admin-approved creators. |
| Full moderation workflow | When report volume requires operational workflow. |
| HLS/DASH/adaptive streaming | When media delivery becomes a real bottleneck. |
| Advanced recommendation | After enough engagement events are collected. |
| Advanced search engine | When PostgreSQL search becomes insufficient. |
| Monetization | After product value and retention are validated. |

---

## 27. Acceptance Criteria for MVP Readiness

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
- the product experience matches the Figma direction closely enough for MVP validation.
