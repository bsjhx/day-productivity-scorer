// Check if user is logged in
function checkAuth() {
    const token = localStorage.getItem('token');
    if (!token) {
        window.location.href = '/login.html';
        return false;
    }
    return true;
}

// Show toast notification
function showToast(message, type = 'info') {
    const container = document.getElementById('toastContainer');
    const toast = document.createElement('div');
    toast.className = `toast toast-${type}`;
    toast.textContent = message;

    container.appendChild(toast);

    // Trigger animation
    setTimeout(() => toast.classList.add('show'), 10);

    // Remove after 4 seconds
    setTimeout(() => {
        toast.classList.remove('show');
        setTimeout(() => toast.remove(), 300);
    }, 4000);
}

// Add token to fetch requests
function authFetch(url, options = {}) {
    const token = localStorage.getItem('token');
    if (!token) {
        window.location.href = '/login.html';
        return Promise.reject('No token');
    }

    options.headers = {
        ...options.headers,
        'Authorization': `Bearer ${token}`
    };

    return fetch(url, options).then(response => {
        if (response.status === 401 || response.status === 403) {
            localStorage.removeItem('token');
            localStorage.removeItem('username');
            window.location.href = '/login.html';
            throw new Error('Unauthorized');
        }
        return response;
    });
}

// Get current week's Monday
function getMonday(date) {
    const d = new Date(date);
    const day = d.getDay();
    const diff = d.getDate() - day + (day === 0 ? -6 : 1);
    return new Date(d.setDate(diff));
}

// Get Sunday of the week
function getSunday(monday) {
    const sunday = new Date(monday);
    sunday.setDate(monday.getDate() + 6);
    return sunday;
}

// Format date as YYYY-MM-DD
function formatDate(date) {
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');
    return `${year}-${month}-${day}`;
}

// Format date for display (e.g., "Jul 21")
function formatDisplayDate(date) {
    const options = { month: 'short', day: 'numeric' };
    return date.toLocaleDateString('en-US', options);
}

// Format week range (e.g., "Jul 21 - Jul 27, 2026")
function formatWeekRange(monday, sunday) {
    const monthDay1 = formatDisplayDate(monday);
    const monthDay2 = formatDisplayDate(sunday);
    const year = sunday.getFullYear();
    return `${monthDay1} - ${monthDay2}, ${year}`;
}

// Get day name
function getDayName(date) {
    const days = ['Sunday', 'Monday', 'Tuesday', 'Wednesday', 'Thursday', 'Friday', 'Saturday'];
    return days[date.getDay()];
}

// Current week Monday
let currentMonday = getMonday(new Date());

// Fetch days from API
async function fetchDays(fromDate, toDate) {
    const from = formatDate(fromDate);
    const to = formatDate(toDate);

    const url = `/day/?from=${from}&to=${to}`;
    const response = await authFetch(url);

    if (!response.ok) {
        throw new Error(`HTTP ${response.status}`);
    }

    return await response.json();
}

// Rate a day
async function rateDay(date, score) {
    try {
        const url = '/day/';
        const response = await authFetch(url, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({
                day: formatDate(date),
                score: score
            })
        });

        if (!response.ok) {
            const errorData = await response.json();
            showToast(errorData.message || 'Failed to rate day', 'error');
            return;
        }

        showToast('Day rated successfully!', 'success');
        await renderWeek();
    } catch (error) {
        console.error('Error rating day:', error);
        showToast('Failed to rate day', 'error');
    }
}

// Render week view
async function renderWeek() {
    const monday = currentMonday;
    const sunday = getSunday(monday);

    // Update week range header
    document.getElementById('weekRange').textContent = formatWeekRange(monday, sunday);

    // Fetch data for the week
    const days = await fetchDays(monday, sunday);

    // Create a map of date -> day data
    const dayMap = {};
    days.forEach(day => {
        dayMap[day.day] = day;
    });

    // Render all 7 days
    const weekView = document.getElementById('weekView');
    weekView.innerHTML = '';

    for (let i = 0; i < 7; i++) {
        const date = new Date(monday);
        date.setDate(monday.getDate() + i);
        const dateStr = formatDate(date);
        const dayData = dayMap[dateStr];

        const dayElement = document.createElement('div');
        dayElement.className = 'day-card';

        const dayName = getDayName(date);
        const displayDate = formatDisplayDate(date);

        dayElement.innerHTML = `
            <div class="day-header">
                <div class="day-name">${dayName}</div>
                <div class="day-date">${displayDate}</div>
            </div>
            <div class="day-score">
                ${dayData ? `<div class="score-display">${dayData.score}</div>` : '<div class="score-display empty">-</div>'}
            </div>
            <div class="score-buttons">
                ${[0, 1, 2, 3, 4, 5].map(score =>
                    `<button class="score-btn ${dayData && dayData.score === score ? 'active' : ''}"
                             onclick="rateDay(new Date('${dateStr}'), ${score})">${score}</button>`
                ).join('')}
            </div>
        `;

        weekView.appendChild(dayElement);
    }
}

// Navigation handlers
document.getElementById('prevWeek').addEventListener('click', () => {
    currentMonday.setDate(currentMonday.getDate() - 7);
    renderWeek();
});

document.getElementById('nextWeek').addEventListener('click', () => {
    currentMonday.setDate(currentMonday.getDate() + 7);
    renderWeek();
});

// Theme switcher
document.querySelectorAll('.theme-btn').forEach(btn => {
    btn.addEventListener('click', () => {
        const theme = btn.dataset.theme;

        // Remove all theme classes
        document.body.classList.remove('theme-light', 'theme-image');

        // Add selected theme class
        if (theme === 'light') {
            document.body.classList.add('theme-light');
        } else if (theme === 'image') {
            document.body.classList.add('theme-image');
        }
        // 'dark' is the default, no class needed

        // Update active button
        document.querySelectorAll('.theme-btn').forEach(b => b.classList.remove('active'));
        btn.classList.add('active');

        // Save preference
        localStorage.setItem('theme', theme);
    });
});

// Load saved theme
const savedTheme = localStorage.getItem('theme') || 'dark';
const themeBtn = document.querySelector(`.theme-btn[data-theme="${savedTheme}"]`);
if (themeBtn) {
    themeBtn.click();
}

// Logout handler
document.getElementById('logoutBtn').addEventListener('click', () => {
    localStorage.removeItem('token');
    localStorage.removeItem('username');
    window.location.href = '/login.html';
});

// Check auth on page load
checkAuth();

// Initial render
renderWeek();