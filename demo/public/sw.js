/**
 * Service Worker - Caching Strategy
 * - Static assets: Cache First (fast loading)
 * - API calls: Network First (fresh data)
 * - Images: Cache First with size limit
 */

const STATIC_CACHE = 'static-v1';
const DYNAMIC_CACHE = 'dynamic-v1';
const IMAGE_CACHE = 'images-v1';
const MAX_IMAGE_CACHE_SIZE = 50;

// Static assets to cache on install
const STATIC_ASSETS = [
  '/',
  '/index.html',
];

// Install event - cache static assets
self.addEventListener('install', (event) => {
  console.log('[SW] Installing...');
  event.waitUntil(
    caches.open(STATIC_CACHE)
      .then((cache) => {
        console.log('[SW] Caching static assets');
        return cache.addAll(STATIC_ASSETS);
      })
      .then(() => self.skipWaiting())
  );
});

// Activate event - clean up old caches
self.addEventListener('activate', (event) => {
  console.log('[SW] Activating...');
  event.waitUntil(
    caches.keys()
      .then((keys) => {
        return Promise.all(
          keys
            .filter((key) => key !== STATIC_CACHE && key !== DYNAMIC_CACHE && key !== IMAGE_CACHE)
            .map((key) => {
              console.log('[SW] Removing old cache:', key);
              return caches.delete(key);
            })
        );
      })
      .then(() => self.clients.claim())
  );
});

// Fetch event - apply caching strategies
self.addEventListener('fetch', (event) => {
  const { request } = event;
  const url = new URL(request.url);

  // Skip non-GET requests
  if (request.method !== 'GET') return;

  // Skip chrome-extension and other non-http requests
  if (!url.protocol.startsWith('http')) return;

  // API calls - Network First
  if (url.pathname.startsWith('/api/') || url.pathname.startsWith('/api')) {
    event.respondWith(networkFirst(request, DYNAMIC_CACHE));
    return;
  }

  // Images - Cache First with size limit
  if (
    request.destination === 'image' ||
    url.pathname.match(/\.(jpg|jpeg|png|gif|webp|svg|ico)$/i)
  ) {
    event.respondWith(cacheFirst(request, IMAGE_CACHE, MAX_IMAGE_CACHE_SIZE));
    return;
  }

  // Static assets (JS, CSS, fonts) - Cache First
  if (
    request.destination === 'script' ||
    request.destination === 'style' ||
    request.destination === 'font' ||
    url.pathname.match(/\.(js|css|woff2?|ttf|otf)$/i)
  ) {
    event.respondWith(cacheFirst(request, STATIC_CACHE));
    return;
  }

  // HTML pages - Network First with fallback
  if (request.mode === 'navigate' || request.destination === 'document') {
    event.respondWith(networkFirstWithFallback(request));
    return;
  }

  // Default - Stale While Revalidate
  event.respondWith(staleWhileRevalidate(request, DYNAMIC_CACHE));
});

// Cache First strategy
async function cacheFirst(request, cacheName, maxSize = null) {
  const cache = await caches.open(cacheName);
  const cachedResponse = await cache.match(request);

  if (cachedResponse) {
    return cachedResponse;
  }

  try {
    const networkResponse = await fetch(request);

    if (networkResponse.ok) {
      // Manage cache size
      if (maxSize) {
        await trimCache(cacheName, maxSize);
      }
      cache.put(request, networkResponse.clone());
    }

    return networkResponse;
  } catch (error) {
    console.log('[SW] Cache First fetch failed:', error);
    return new Response('Offline', { status: 503 });
  }
}

// Network First strategy
async function networkFirst(request, cacheName) {
  const cache = await caches.open(cacheName);

  try {
    const networkResponse = await fetch(request);

    if (networkResponse.ok) {
      cache.put(request, networkResponse.clone());
    }

    return networkResponse;
  } catch (error) {
    console.log('[SW] Network First fetch failed, trying cache:', error);
    const cachedResponse = await cache.match(request);

    if (cachedResponse) {
      return cachedResponse;
    }

    return new Response(JSON.stringify({ error: 'Offline' }), {
      status: 503,
      headers: { 'Content-Type': 'application/json' },
    });
  }
}

// Network First with HTML fallback
async function networkFirstWithFallback(request) {
  const cache = await caches.open(DYNAMIC_CACHE);

  try {
    const networkResponse = await fetch(request);

    if (networkResponse.ok) {
      cache.put(request, networkResponse.clone());
    }

    return networkResponse;
  } catch (error) {
    console.log('[SW] Network First fallback, trying cache:', error);
    const cachedResponse = await cache.match(request);

    if (cachedResponse) {
      return cachedResponse;
    }

    // Return cached index.html as last resort
    const indexResponse = await caches.match('/index.html');
    if (indexResponse) {
      return indexResponse;
    }

    return new Response('Offline', { status: 503 });
  }
}

// Stale While Revalidate strategy
async function staleWhileRevalidate(request, cacheName) {
  const cache = await caches.open(cacheName);
  const cachedResponse = await cache.match(request);

  // Fetch in background
  const fetchPromise = fetch(request)
    .then((networkResponse) => {
      if (networkResponse.ok) {
        cache.put(request, networkResponse.clone());
      }
      return networkResponse;
    })
    .catch(() => null);

  // Return cached response immediately if available
  return cachedResponse || fetchPromise;
}

// Trim cache to max size
async function trimCache(cacheName, maxSize) {
  const cache = await caches.open(cacheName);
  const keys = await cache.keys();

  if (keys.length > maxSize) {
    const deleteCount = keys.length - maxSize;
    for (let i = 0; i < deleteCount; i++) {
      await cache.delete(keys[i]);
    }
    console.log('[SW] Trimmed', deleteCount, 'items from', cacheName);
  }
}

// Message handling
self.addEventListener('message', (event) => {
  if (event.data && event.data.type === 'SKIP_WAITING') {
    self.skipWaiting();
  }

  if (event.data && event.data.type === 'CLEAR_CACHE') {
    caches.keys().then((keys) => {
      keys.forEach((key) => caches.delete(key));
    });
  }
});

console.log('[SW] Service worker loaded');
