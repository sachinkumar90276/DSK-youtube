# DSK YouTube

**Watch Everything. Pay Nothing.**

DSK YouTube is a 100% free native Android video & Shorts streaming platform built with Kotlin, Jetpack Compose, Material 3, Retrofit/Moshi, Official YouTube Data API v3, Official YouTube Embedded Player, Media3 ExoPlayer (for creator direct uploads), Room SQLite, and DataStore.

## Key Features
- **100% Free & Zero DSK Ads**: No subscriptions, no paywalls, no paid memberships, no payment gateways, and zero app-inserted banner/popup/sponsored ads.
- **Live YouTube Data API v3**: Real trending videos, category feeds (`All`, `Music`, `Gaming`, `Movies`, `Entertainment`, `News`, `Live`, `Comedy`, `Technology`, `Education`, `Sports`, `Travel`), Popular Shorts, Live Streams, channel profiles, and live YouTube Search with smart caching and quota-aware refresh.
- **Official YouTube Embedded Player & Media3 ExoPlayer**: YouTube videos and Shorts play via YouTube's official IFrame Player API; creator-uploaded MP4/WebM/local files play via Media3 ExoPlayer with PiP, speed control, fullscreen, and autoplay next.
- **Vertical Shorts**: 9:16 `VerticalPager` with active-page playback, pause when off-screen, likes, comments & replies, share, subscribe, and creator info.
- **Creator Channels & Uploads**: Register/Sign In (PBKDF2-HMAC-SHA256 password hashing), creator channel management, Photo Picker (`PickVisualMedia`) video/thumbnail uploads with 100 MB local cap, or direct MP4/WebM URLs.
- **Library & Playlists**: Watch History, Watch Later, Liked Videos, My Videos, My Shorts, Subscriptions, and full Playlists.
- **Role-Protected Admin Panel**: Dashboard metrics, direct URL publishing, content moderation, category management, user blocking, channel suspension, and report resolution.

## Configuration
- Add `YOUTUBE_API_KEY` in the **Secrets panel in AI Studio** (mapped via `.env` / Secrets Gradle Plugin to `BuildConfig.YOUTUBE_API_KEY`).
