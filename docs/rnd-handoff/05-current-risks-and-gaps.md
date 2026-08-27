# Current Risks And Gaps

## Confirmed Technical Gaps

| Item | Impact | Blocks | Likely owner |
|---|---|---|---|
| Live creator upload is not implemented. | Creators cannot publish through the product. | Production | Backend |
| Resumable/multipart mobile upload is undecided. | Large mobile uploads may fail or produce poor UX. | Production | R&D |
| Transcoding, thumbnails, and media worker are deferred. | MVP must either constrain inputs or add processing capacity. | Production | Backend |
| Content/media read path is uncommitted working-tree code. | Release baseline cannot assume it is merged. | Development | Backend |
| Notifications, chat, comments, likes, saves, follows, reports, and analytics are documentation-only or absent. | Broader PRD MVP is not close to complete. | Production | Product |
| Flutter app is absent except `.gitkeep`. | Mobile release work has no implemented application baseline. | Production | Mobile |
| `web/admin` is absent except `.gitkeep`. | Full admin dashboard from PRD is not implemented. | Production | Product |

## Operational Gaps

| Item | Impact | Blocks | Likely owner |
|---|---|---|---|
| No staging environment is implemented. | No realistic pre-production rehearsal path. | Staging | DevOps |
| No production deployment automation exists. | Release would depend on manual commands. | Production | DevOps |
| No TLS/domain/DNS configuration exists. | Public deployment is incomplete. | Production | R&D |
| No automated PostgreSQL or object-storage backups exist. | Data loss recovery is undefined. | Production | DevOps |
| No restore rehearsal exists. | Backups, if added, may not be usable. | Production | DevOps |
| No monitoring dashboards or alerts exist. | Incidents may not be detected quickly. | Production | DevOps |
| No rollback plan exists. | Failed deployment recovery is unclear. | Production | DevOps |

## Security Or Secret-Handling Findings

| Item | Impact | Blocks | Likely owner |
|---|---|---|---|
| `.env` exists and is ignored. | Local secrets may exist outside Git; keep it out of handoff copies and commits. | Staging | Backend |
| `secrets/minis-671fc-firebase-adminsdk-fbsvc-95e5d4a30e.json` exists and is ignored. | Firebase Admin credential-like file is contained by `.gitignore`/`.dockerignore`; do not copy to R&D repo; rotate if it was ever shared. | Production | Backend |
| `web/event-app/.env.local` exists and is ignored. | Public Firebase values may be local-only; confirm no private backend/admin credentials are placed in the frontend. | Staging | Frontend |
| Tracked `.env.example` files contain variable names and placeholders. | Acceptable template pattern, but review before publishing outside the company. | Staging | Backend |

Contents were not read.

## Mobile-Release Gaps

| Item | Impact | Blocks | Likely owner |
|---|---|---|---|
| Android app, package name, signing keys, and Play Console setup are not evidenced. | Android beta/public release cannot proceed. | Production | Mobile |
| iOS app, Bundle ID, certificates, profiles, Mac/Xcode access, TestFlight, and App Store setup are not evidenced. | iOS beta/public release cannot proceed. | Production | Mobile |
| Sign in with Apple is in PRD/ADR scope but not implemented in current React event client. | iOS auth expectations may require additional work. | Production | Mobile |
| Privacy policy, account deletion policy, and store data-safety answers are not evidenced. | Store review may block. | Production | Management |

## Business Information R&D Must Collect

| Item | Impact | Blocks | Likely owner |
|---|---|---|---|
| Budget and billing owner. | Determines viable hosting/storage/auth options. | Production | Management |
| Expected launch usage and media volume. | Needed for cost and performance assumptions. | Production | Product |
| Launch countries and network expectations. | Affects CDN/upload/mobile release decisions. | Production | Product |
| Domain and DNS ownership. | Needed for TLS, Firebase authorized domains, app links, and store review. | Production | Management |
| Operational owner and support expectations. | Needed for alerts, backups, incidents, and rollback. | Production | Management |
| Android and Apple account ownership. | Needed for store release readiness. | Production | Management |

## Documentation Conflicts

| Item | Impact | Blocks | Likely owner |
|---|---|---|---|
| PRD/system docs describe Flutter as primary MVP client; implementation only has a placeholder and React event app. | Scope and release target may be misunderstood. | Production | Product |
| PRD/system docs describe `web/admin`; implementation only has placeholder plus event-admin screens inside React event app. | Admin scope is unclear. | Production | Product |
| Architecture docs use package example `com.minis`; actual package root is `com.end.minisbackend`. | Low implementation risk, but stale docs can mislead R&D. | Development | Backend |
| Root README says placeholders under `mobile/flutter` and `web/admin` are not present, but `.gitkeep` placeholders are tracked. | Minor repository-state mismatch. | Development | Backend |
| `database/README.md` still emphasizes `V1` while current evidence includes `V2` committed and `V3` untracked. | Schema baseline can be misread. | Development | Backend |
| Decision backlog next-order text says create ADR-0004 before upload, while ADR-0004 is now event media read path; it also proposes duplicate ADR-0006 names. | Future ADR numbering needs cleanup. | Development | Backend |
| System overview says initial production direction is one VPS; repository only implements local/demo compose and prod profile configuration. | R&D must treat this as documented direction, not production readiness. | Production | R&D |

## Analysis Limitations

| Item | Impact | Blocks | Likely owner |
|---|---|---|---|
| Full backend Maven `verify` was not run. | Backend tests and static gates are not confirmed by this handoff. | Development | Backend |
| No manual Firebase Console or email-template verification was performed. | Auth environment readiness remains unknown. | Staging | R&D |
| No manual media import/playback was performed. | Event media read path remains implementation evidence, not end-to-end rehearsal. | Staging | Backend |
| No external provider research was performed. | R&D must collect official current sources separately. | Production | R&D |
