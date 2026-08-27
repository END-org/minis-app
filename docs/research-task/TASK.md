# MINIS Deployment and Mobile Release Research

## Objective

Recommend a practical deployment and mobile-release approach for the current MINIS application that can be implemented for the upcoming release and operated by a small team with limited budget.

The final recommendation must make clear:

* what to keep
* what to change
* what to defer
* expected cost
* what needs to start immediately
* what still needs technical validation after the research

Treat the engineering boundaries in `PROJECT-CONTEXT.md` as fixed for this release. If a release-blocking issue is discovered, document it for Backend Owner review rather than expanding this task into a framework, database or architecture rewrite.

## Timeline

This task lasts 5 working days.

| Day | Outcome |
|---|---|
| Day 1 | Understand the project, confirm key business inputs and record assumptions |
| Day 2 | Research credible options and eliminate clearly unsuitable ones |
| Day 3 | Review the shortlist, initial recommendation and unresolved questions |
| Day 4 | Deepen the strongest options and complete cost and mobile-release research |
| Day 5 | Complete the results and present the final recommendation |

## What To Research

### 1. Production Stack

Research the most practical production arrangement for the existing MINIS application.

Cover:

* Spring Boot hosting
* managed PostgreSQL hosting
* object storage
* identity/authentication

Do not make each item a separate market-research project. The goal is one practical production combination plus one credible fallback. A third option may be included if it represents a genuinely different approach worth considering.

Compare options by:

* fit with the current MINIS architecture
* setup and operational effort
* reliability and important service limits
* region/latency where relevant
* migration effort and lock-in
* pricing and overage behavior
* risk to the release schedule

For identity, answer this directly:

Is there a strong reason to replace the current identity service before this release?

If yes, explain:

* benefit
* migration impact
* user/client/backend impact
* why it is worth changing now

If no, recommend keeping it for this release and state when it should be reconsidered.

### 2. Upload and Video

Recommend an MVP upload and video strategy.

Answer:

* backend upload or direct-to-storage upload
* whether resumable/multipart upload is needed now
* accepted video profile versus transcoding
* whether transcoding is needed for the first public release
* whether thumbnails are needed now
* whether CDN is needed now
* high-level handling of failed/abandoned uploads
* what can be intentionally deferred

Do not design a future-scale video platform, complex worker system, recommendation infrastructure or speculative high-scale media architecture.

### 3. Minimum Production Operations

Determine the minimum required setup for the selected architecture:

* production environment
* minimal staging approach, if needed
* DNS/TLS
* secrets
* PostgreSQL backup/restore
* object-storage durability
* logs and error visibility
* simple uptime alerting
* rollback
* production access/account ownership

Prefer features already included in the selected platforms.

Do not automatically introduce separate paid services for monitoring, logging, error tracking, backup or CI/CD. Recommend an additional tool only when a real release requirement is otherwise missing.

### 4. Android and iOS Release

Research the practical release path for Android and iOS.

Cover:

* developer accounts, ownership and verification
* package name / Bundle ID and signing
* Mac/Xcode/build environment
* internal/beta testing path
* public-store submission/review path
* privacy, data declarations and account deletion
* user-generated-content/moderation requirements where relevant
* account/review/verification lead-time blockers
* actions that should begin immediately

Do not perform publication or turn this into a full store-submission checklist project.

## Inputs To Confirm

Confirm where possible:

* release scope
* monthly and one-time budget
* expected users
* expected video uploads and typical video size/duration
* expected viewing traffic
* launch countries
* billing/provider-account ownership
* DNS ownership
* Google Play account status/ownership
* Apple Developer account status/ownership
* Mac/Xcode access
* who will operate production

Do not create a separate stakeholder-interview file. Record important confirmed values and assumptions directly in the final report.

If exact values are unavailable:

* make a reasonable assumption
* clearly label it
* use Pilot, Expected Launch and Higher Growth scenarios where cost depends on usage
* identify decisions that are sensitive to the assumption

## How To Compare Options

Start broad enough to find credible alternatives, then eliminate unsuitable options early.

Do not compare every provider on the market.

The final report should show only:

* serious candidates that reached the shortlist
* important rejected candidates when the rejection is useful
* a short reason for each rejection
* one recommended architecture
* one fallback

Use official product documentation and official pricing for facts that can change.

Do not create weighted numeric scores.

Do not document every service found during searching.

## Day 3 Review

Review:

* confirmed constraints and important assumptions
* shortlist
* important rejected options
* initial recommended architecture
* fallback direction
* unresolved questions

## Final Deliverables

Complete:

1. `results/final-report.md`
2. `results/costs.csv`
3. `results/mobile-release.md`

On Day 5, return the completed result files and present the recommendation.

No separate slide deck is required.

## Out of Scope

This task does not include:

* production/staging implementation or cloud deployment
* PoCs or technical experiments
* Flutter/product-feature implementation
* Android/iOS publication
* provider account setup or billing obligations
* rewriting the backend, database technology or application architecture
* implementing upload, transcoding, CDN, monitoring or CI/CD
* penetration testing or a full security audit
* enterprise-scale architecture planning
* the detailed engineering roadmap that follows the research

## Completion Criteria

The task is complete when:

* one recommended architecture and one credible fallback are clearly justified
* important claims and prices use current official sources
* assumptions are visible
* recommendations fit the current MINIS architecture and release timing
* Pilot, Expected Launch and Higher Growth costs are estimated
* Android/iOS blockers and start-now actions are clear
* Keep / Change / Defer decisions are clear
* remaining technical uncertainties are identified for later validation
