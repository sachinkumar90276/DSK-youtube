# DSK YouTube

**Watch Everything. Pay Nothing.**

DSK YouTube is a 100% free native Android video & Shorts streaming platform built with Kotlin, Jetpack Compose, Material 3, Media3 ExoPlayer, Room SQLite, and DataStore.

## Key Features
- **100% Free Forever**: No subscriptions, no paywalls, no paid memberships, no payment gateways.
- **Home & Discovery**: Branded splash screen, category filtering (`All`, `Music`, `Gaming`, `Movies`, `News`, `Live`, `Comedy`, `Technology`, `Education`, `Sports`, `Travel`), Trending, Recommended, Latest Uploads, Most Viewed, Music Videos, Popular Shorts, and Live Streams.
- **Media3 ExoPlayer**: Play/pause, 10s seek forward/back, progress scrubbing, playback speed (`0.5x–2x`), quality selection (`Auto`, `1080p`, `720p`, `480p`, `360p`), volume control, captions (`CC`), autoplay next, fullscreen, and Picture-in-Picture (PiP).
- **Vertical Shorts**: 9:16 `VerticalPager` with active-page auto-play, pause when off-screen, likes, comments & replies, share, subscribe, and music info.
- **Creator Channels & Uploads**: Register/Sign In (PBKDF2-HMAC-SHA256 password hashing), auto-created creator channel, Photo Picker (`PickVisualMedia`) video/thumbnail uploads with real chunked progress tracking and 100 MB local free-tier cap, or direct MP4/WebM stream URLs.
- **Library & Playlists**: Watch History, Watch Later, Liked Videos, My Videos, My Shorts, Subscriptions, and full Playlists (create, edit, delete, reorder, public/private).
- **Role-Protected Admin Panel**: Dashboard metrics, URL publishing, edit/delete/publish/feature/trending controls, category management, user blocking, channel suspension, and report moderation.

## Storage & Free-Tier Limits
- Local device uploads are stored inside the app's private storage (`filesDir/uploads/`) with a 100 MB per-file cap to protect device storage.
- Direct HTTP/HTTPS MP4 & WebM URLs stream directly via ExoPlayer with zero server hosting fees.
