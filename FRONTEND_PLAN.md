# Frontend Implementation Plan - Clean & Minimal

## Overview
Build a clean, minimal cinema booking UI using **vanilla JavaScript, semantic HTML, and modern CSS** - demonstrating solid web fundamentals without framework complexity. This approach showcases understanding of core web technologies and progressive enhancement.

## Tech Stack

### Core Technologies
- **HTML5**: Semantic markup (`<main>`, `<nav>`, `<article>`, `<section>`, `<form>`)
- **CSS3**: Modern layout (Grid, Flexbox), custom properties (CSS variables), no preprocessors
- **Vanilla JavaScript (ES6+)**: Modules, async/await, Fetch API, no build tools needed
- **Web Standards**: Progressive enhancement, accessibility (ARIA), responsive design

### Why This Stack?
✅ **Demonstrates Fundamentals**: Shows deep understanding of how the web works  
✅ **No Build Complexity**: Works directly in browser, easy to understand and maintain  
✅ **Production Ready**: Modern browsers support all features natively  
✅ **Performance**: Zero framework overhead, instant load times  
✅ **Learning Value**: Code is the documentation - no magic, no abstractions

## Project Structure

```
frontend/
├── index.html              # Landing page (public movie listings)
├── admin.html              # Admin dashboard
├── booking.html            # Booking flow
├── css/
│   ├── reset.css          # Minimal CSS reset
│   ├── variables.css      # Design tokens (colors, spacing, typography)
│   ├── layout.css         # Grid system and page structure
│   └── components.css     # Reusable UI components
├── js/
│   ├── config.js          # API base URL and configuration
│   ├── api.js             # Fetch wrapper with auth and error handling
│   ├── auth.js            # Login/logout, JWT management
│   ├── movies.js          # Movie listing and search
│   ├── booking.js         # Seat selection and booking flow
│   └── admin.js           # Admin movie import from TMDb
└── assets/
    └── icons/             # SVG icons (inline for simplicity)
```

## Design Principles

### 1. **Semantic HTML First**
```html
<!-- Good: Semantic structure -->
<main class="movie-listings">
  <section class="search-section">
    <form class="search-form" role="search">
      <label for="movie-search">Search Movies</label>
      <input type="search" id="movie-search" name="query" 
             placeholder="Search..." autocomplete="off">
      <button type="submit">Search</button>
    </form>
  </section>
  
  <section class="movies-grid">
    <article class="movie-card">
      <h2 class="movie-title">Inception</h2>
      <p class="movie-meta">
        <time datetime="2010-07-16">2010</time> • 
        <span class="genre">Sci-Fi</span> • 
        <span class="duration">148 min</span>
      </p>
    </article>
  </section>
</main>
```

### 2. **CSS Custom Properties for Theming**
```css
:root {
  /* Colors */
  --color-primary: #2563eb;
  --color-primary-hover: #1d4ed8;
  --color-text: #1f2937;
  --color-text-muted: #6b7280;
  --color-background: #ffffff;
  --color-surface: #f9fafb;
  --color-border: #e5e7eb;
  --color-error: #dc2626;
  --color-success: #16a34a;
  
  /* Spacing */
  --space-xs: 0.25rem;
  --space-sm: 0.5rem;
  --space-md: 1rem;
  --space-lg: 1.5rem;
  --space-xl: 2rem;
  
  /* Typography */
  --font-sans: system-ui, -apple-system, sans-serif;
  --font-mono: 'SF Mono', Monaco, monospace;
  --font-size-sm: 0.875rem;
  --font-size-base: 1rem;
  --font-size-lg: 1.125rem;
  --font-size-xl: 1.25rem;
  --font-size-2xl: 1.5rem;
  
  /* Layout */
  --container-width: 1200px;
  --border-radius: 0.5rem;
  --transition: 200ms ease;
}

/* Dark mode support */
@media (prefers-color-scheme: dark) {
  :root {
    --color-text: #f9fafb;
    --color-background: #111827;
    --color-surface: #1f2937;
    --color-border: #374151;
  }
}
```

### 3. **Modern CSS Layout**
```css
/* Grid-based movie listings */
.movies-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(250px, 1fr));
  gap: var(--space-lg);
  padding: var(--space-lg);
}

/* Flexbox for components */
.movie-card {
  display: flex;
  flex-direction: column;
  gap: var(--space-sm);
  padding: var(--space-md);
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--border-radius);
  transition: transform var(--transition), box-shadow var(--transition);
}

.movie-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.1);
}
```

### 4. **Vanilla JavaScript Modules**
```javascript
// api.js - Centralized API client
const API_BASE = 'http://localhost:8080/api';

class ApiClient {
  constructor(baseUrl) {
    this.baseUrl = baseUrl;
  }

  getAuthHeaders() {
    const token = localStorage.getItem('jwt_token');
    return token ? { 'Authorization': `Bearer ${token}` } : {};
  }

  async request(endpoint, options = {}) {
    const url = `${this.baseUrl}${endpoint}`;
    const config = {
      headers: {
        'Content-Type': 'application/json',
        ...this.getAuthHeaders(),
        ...options.headers
      },
      ...options
    };

    try {
      const response = await fetch(url, config);
      
      if (!response.ok) {
        const error = await response.json();
        throw new Error(error.message || 'Request failed');
      }
      
      return await response.json();
    } catch (error) {
      console.error('API Error:', error);
      throw error;
    }
  }

  get(endpoint) {
    return this.request(endpoint);
  }

  post(endpoint, data) {
    return this.request(endpoint, {
      method: 'POST',
      body: JSON.stringify(data)
    });
  }
}

export const api = new ApiClient(API_BASE);
```

### 5. **Progressive Enhancement**
```javascript
// Check for required features
if (!('fetch' in window) || !('localStorage' in window)) {
  document.body.innerHTML = `
    <div class="error-message">
      Your browser doesn't support required features.
      Please use a modern browser (Chrome, Firefox, Safari, Edge).
    </div>
  `;
}

// Graceful degradation for JavaScript disabled
// Add <noscript> tags with alternative content
```

## Core Features Implementation

### 1. Public Movie Listings (index.html)
**Features:**
- View all available movies with shows
- Search/filter movies
- View show times
- Click to book → redirect to booking page

**Key Components:**
```html
<nav class="main-nav">
  <a href="/" class="logo">MyCinema</a>
  <div class="nav-links">
    <a href="/">Movies</a>
    <a href="/admin.html">Admin</a>
  </div>
</nav>

<main class="container">
  <section class="hero">
    <h1>Now Showing</h1>
    <form class="search-form" id="movie-search">
      <input type="search" placeholder="Search movies...">
      <button type="submit">Search</button>
    </form>
  </section>
  
  <section class="movies-grid" id="movies-container">
    <!-- Populated by JavaScript -->
  </section>
</main>
```

### 2. Booking Flow (booking.html)
**Features:**
- Visual seat map (grid layout)
- Real-time seat availability
- Select multiple seats
- Booking summary with total price
- Confirm and pay (mock payment)

**Seat Map Implementation:**
```css
.seat-map {
  display: grid;
  grid-template-columns: repeat(10, 40px);
  gap: 8px;
  justify-content: center;
  margin: var(--space-xl) 0;
}

.seat {
  width: 40px;
  height: 40px;
  border: 2px solid var(--color-border);
  border-radius: 6px;
  cursor: pointer;
  transition: all var(--transition);
  background: var(--color-surface);
}

.seat:hover:not(.unavailable) {
  transform: scale(1.1);
  border-color: var(--color-primary);
}

.seat.selected {
  background: var(--color-primary);
  color: white;
  border-color: var(--color-primary);
}

.seat.unavailable {
  background: var(--color-text-muted);
  cursor: not-allowed;
  opacity: 0.5;
}
```

**JavaScript Logic:**
```javascript
// booking.js
class SeatMap {
  constructor(showId, containerId) {
    this.showId = showId;
    this.container = document.getElementById(containerId);
    this.selectedSeats = new Set();
    this.seatData = [];
  }

  async loadSeats() {
    try {
      this.seatData = await api.get(`/shows/${this.showId}/seats`);
      this.render();
    } catch (error) {
      this.showError('Failed to load seats');
    }
  }

  render() {
    this.container.innerHTML = this.seatData
      .map(seat => `
        <button 
          class="seat ${seat.status === 'AVAILABLE' ? '' : 'unavailable'}"
          data-seat-id="${seat.id}"
          data-row="${seat.rowLabel}"
          data-col="${seat.columnNumber}"
          ${seat.status !== 'AVAILABLE' ? 'disabled' : ''}
        >
          ${seat.rowLabel}${seat.columnNumber}
        </button>
      `)
      .join('');
    
    this.attachEventListeners();
  }

  attachEventListeners() {
    this.container.addEventListener('click', (e) => {
      const seat = e.target.closest('.seat');
      if (!seat || seat.disabled) return;
      
      const seatId = seat.dataset.seatId;
      if (this.selectedSeats.has(seatId)) {
        this.selectedSeats.delete(seatId);
        seat.classList.remove('selected');
      } else {
        this.selectedSeats.add(seatId);
        seat.classList.add('selected');
      }
      
      this.updateSummary();
    });
  }

  updateSummary() {
    const count = this.selectedSeats.size;
    const total = count * 15.00; // Price per seat
    document.getElementById('selected-count').textContent = count;
    document.getElementById('total-price').textContent = `$${total.toFixed(2)}`;
  }

  async confirmBooking() {
    if (this.selectedSeats.size === 0) {
      alert('Please select at least one seat');
      return;
    }

    try {
      const booking = await api.post('/bookings', {
        showId: this.showId,
        seatIds: Array.from(this.selectedSeats),
        paymentMethod: 'CREDIT_CARD'
      });
      
      window.location.href = `/confirmation.html?id=${booking.id}`;
    } catch (error) {
      alert(`Booking failed: ${error.message}`);
    }
  }
}

// Initialize
const urlParams = new URLSearchParams(window.location.search);
const showId = urlParams.get('showId');
const seatMap = new SeatMap(showId, 'seat-map');
seatMap.loadSeats();
```

### 3. Admin Dashboard (admin.html)
**Features:**
- Login with admin credentials
- Search TMDb movies
- Import selected movies
- View imported movies

**Authentication Flow:**
```javascript
// auth.js
class AuthService {
  async login(email, password) {
    try {
      const response = await api.post('/auth/login', { email, password });
      localStorage.setItem('jwt_token', response.token);
      localStorage.setItem('user_role', response.roles);
      return response;
    } catch (error) {
      throw new Error('Login failed: ' + error.message);
    }
  }

  logout() {
    localStorage.removeItem('jwt_token');
    localStorage.removeItem('user_role');
    window.location.href = '/';
  }

  isAuthenticated() {
    return !!localStorage.getItem('jwt_token');
  }

  isAdmin() {
    const roles = localStorage.getItem('user_role');
    return roles && roles.includes('ROLE_ADMIN');
  }

  requireAdmin() {
    if (!this.isAdmin()) {
      window.location.href = '/';
    }
  }
}

export const auth = new AuthService();
```

**TMDb Movie Import:**
```javascript
// admin.js
class AdminMovieManager {
  constructor() {
    auth.requireAdmin();
    this.searchInput = document.getElementById('tmdb-search');
    this.resultsContainer = document.getElementById('search-results');
    this.setupSearch();
  }

  setupSearch() {
    let timeout;
    this.searchInput.addEventListener('input', (e) => {
      clearTimeout(timeout);
      timeout = setTimeout(() => this.searchMovies(e.target.value), 300);
    });
  }

  async searchMovies(query) {
    if (query.length < 2) return;
    
    try {
      const results = await api.get(`/admin/movies/search?query=${encodeURIComponent(query)}`);
      this.displayResults(results);
    } catch (error) {
      this.showError('Search failed: ' + error.message);
    }
  }

  displayResults(movies) {
    this.resultsContainer.innerHTML = movies.map(movie => `
      <article class="movie-result">
        <div class="movie-info">
          <h3>${movie.title}</h3>
          <p class="meta">
            ${movie.releaseDate || 'N/A'} • 
            ${movie.genres?.join(', ') || 'Unknown'} • 
            ${movie.runtime ? movie.runtime + ' min' : 'N/A'}
          </p>
          <p class="overview">${movie.overview || 'No description available.'}</p>
        </div>
        <button 
          class="btn btn-primary" 
          onclick="adminManager.importMovie('${movie.externalId}', '${movie.genres[0] || 'Drama'}')"
        >
          Import
        </button>
      </article>
    `).join('');
  }

  async importMovie(externalId, genre) {
    try {
      await api.post('/admin/movies/import', { externalId, genre });
      alert('Movie imported successfully!');
    } catch (error) {
      alert('Import failed: ' + error.message);
    }
  }
}

// Initialize
window.adminManager = new AdminMovieManager();
```

## Accessibility Features

### 1. Semantic HTML
- Use `<nav>`, `<main>`, `<article>`, `<section>`, `<form>` appropriately
- Proper heading hierarchy (`<h1>` → `<h2>` → `<h3>`)
- `<button>` for actions, `<a>` for navigation

### 2. ARIA Labels
```html
<button 
  class="seat" 
  aria-label="Row A, Seat 5, Available"
  aria-pressed="false"
  data-seat-id="123"
>
  A5
</button>

<form role="search" aria-label="Search movies">
  <input 
    type="search" 
    id="movie-search" 
    aria-label="Movie title"
    placeholder="Search movies..."
  >
</form>
```

### 3. Keyboard Navigation
```javascript
// Ensure all interactive elements are keyboard accessible
document.addEventListener('keydown', (e) => {
  if (e.key === 'Escape') {
    closeModal();
  }
  if (e.key === 'Enter' && e.target.classList.contains('seat')) {
    e.target.click();
  }
});

// Focus management for modals
function openModal() {
  const modal = document.getElementById('modal');
  modal.classList.add('open');
  modal.querySelector('button').focus();
}
```

### 4. Color Contrast
- Ensure WCAG AA compliance (4.5:1 contrast ratio)
- Don't rely solely on color for information
- Provide text labels alongside color indicators

## Responsive Design

### Mobile-First Approach
```css
/* Base styles (mobile) */
.movies-grid {
  grid-template-columns: 1fr;
  padding: var(--space-md);
}

.seat-map {
  grid-template-columns: repeat(5, 50px);
  gap: 4px;
}

/* Tablet (≥768px) */
@media (min-width: 768px) {
  .movies-grid {
    grid-template-columns: repeat(2, 1fr);
  }
  
  .seat-map {
    grid-template-columns: repeat(10, 40px);
  }
}

/* Desktop (≥1024px) */
@media (min-width: 1024px) {
  .movies-grid {
    grid-template-columns: repeat(3, 1fr);
  }
}

/* Large Desktop (≥1280px) */
@media (min-width: 1280px) {
  .movies-grid {
    grid-template-columns: repeat(4, 1fr);
  }
}
```

## Performance Optimizations

### 1. Lazy Loading
```javascript
// Load images only when visible
const imageObserver = new IntersectionObserver((entries) => {
  entries.forEach(entry => {
    if (entry.isIntersecting) {
      const img = entry.target;
      img.src = img.dataset.src;
      imageObserver.unobserve(img);
    }
  });
});

document.querySelectorAll('img[data-src]').forEach(img => {
  imageObserver.observe(img);
});
```

### 2. Debouncing Search
```javascript
function debounce(func, delay) {
  let timeout;
  return function(...args) {
    clearTimeout(timeout);
    timeout = setTimeout(() => func.apply(this, args), delay);
  };
}

const searchMovies = debounce(async (query) => {
  // API call
}, 300);
```

### 3. CSS Optimization
- Use `will-change` sparingly for animations
- Leverage CSS containment: `contain: layout style paint;`
- Minimize reflows with `transform` and `opacity`

## Error Handling

### User-Friendly Error Messages
```javascript
class ErrorHandler {
  static show(message, type = 'error') {
    const toast = document.createElement('div');
    toast.className = `toast toast-${type}`;
    toast.textContent = message;
    toast.setAttribute('role', 'alert');
    document.body.appendChild(toast);
    
    setTimeout(() => toast.classList.add('show'), 10);
    setTimeout(() => {
      toast.classList.remove('show');
      setTimeout(() => toast.remove(), 300);
    }, 3000);
  }

  static handleApiError(error) {
    if (error.message.includes('401')) {
      this.show('Session expired. Please login again.');
      auth.logout();
    } else if (error.message.includes('403')) {
      this.show('You don\'t have permission to perform this action.');
    } else if (error.message.includes('Network')) {
      this.show('Network error. Please check your connection.');
    } else {
      this.show(error.message || 'An unexpected error occurred.');
    }
  }
}
```

## Testing Strategy

### 1. Manual Testing Checklist
- [ ] All pages load without errors
- [ ] Login/logout works correctly
- [ ] Movie search returns results
- [ ] Seat selection works (click, multi-select)
- [ ] Booking creates reservation
- [ ] Admin can import movies from TMDb
- [ ] Responsive on mobile, tablet, desktop
- [ ] Keyboard navigation works
- [ ] Error messages display correctly

### 2. Browser Compatibility
- Chrome (latest)
- Firefox (latest)
- Safari (latest)
- Edge (latest)
- Mobile Safari (iOS)
- Chrome Mobile (Android)

### 3. Accessibility Testing
- [ ] Screen reader compatible (VoiceOver, NVDA)
- [ ] Keyboard navigation works throughout
- [ ] Color contrast meets WCAG AA
- [ ] Focus indicators visible
- [ ] Alt text for images

## Deployment

### Static File Hosting
Since this is pure HTML/CSS/JS, deploy to:
- **Netlify** (free, auto-deploy from Git)
- **Vercel** (free, serverless functions if needed)
- **GitHub Pages** (free, simple)
- **AWS S3 + CloudFront** (professional setup)

### Configuration
Create `js/config.js` with environment-specific settings:
```javascript
const CONFIG = {
  API_BASE_URL: window.location.hostname === 'localhost' 
    ? 'http://localhost:8080/api'
    : 'https://api.mycinema.com/api',
  ENABLE_LOGGING: window.location.hostname === 'localhost'
};

export default CONFIG;
```

## Project Timeline

### Phase 1: Core Structure (2-3 hours)
- HTML structure for all pages
- CSS reset, variables, and layout system
- Basic API client and auth module

### Phase 2: Public Features (3-4 hours)
- Movie listings with grid layout
- Search functionality
- Show times display
- Responsive design implementation

### Phase 3: Booking Flow (4-5 hours)
- Seat map visualization
- Seat selection logic
- Booking summary
- Payment confirmation
- Booking confirmation page

### Phase 4: Admin Dashboard (3-4 hours)
- Login form and authentication
- TMDb search interface
- Movie import functionality
- Admin movie management

### Phase 5: Polish & Testing (2-3 hours)
- Accessibility improvements
- Error handling refinement
- Cross-browser testing
- Performance optimization
- Documentation

**Total Estimated Time: 14-19 hours**

## Why This Approach Works

### 1. **Demonstrates Core Skills**
- Deep understanding of HTML/CSS/JS fundamentals
- No framework magic to hide behind
- Shows ability to build from scratch

### 2. **Production Ready**
- Works in all modern browsers
- Fast load times (no framework overhead)
- Easy to debug (no build tools)
- Simple deployment

### 3. **Maintainable**
- Clear separation of concerns
- Self-documenting code
- No dependencies to update
- Easy for others to understand

### 4. **Scalable**
- Modular architecture
- Easy to add features
- Can migrate to framework later if needed

### 5. **Professional Quality**
- Clean, semantic code
- Proper error handling
- Accessible and responsive
- Follows web standards

## Next Steps

1. **Create Basic HTML Structure** - Start with `index.html` and core layout
2. **Build CSS System** - Variables, reset, layout, components
3. **Implement API Client** - Fetch wrapper with auth and error handling
4. **Build Movie Listings** - Public movie display with search
5. **Create Booking Flow** - Seat map and reservation logic
6. **Add Admin Dashboard** - TMDb integration and movie import
7. **Test & Polish** - Accessibility, responsiveness, error handling

---

**This approach demonstrates:**
✅ Solid understanding of web fundamentals  
✅ Ability to build production-ready applications  
✅ Clean, maintainable code  
✅ Professional development practices  
✅ No over-engineering - just good engineering
