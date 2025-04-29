const API_BASE_URL = 'http://localhost:8090/api'; // Your backend URL

/**
 * Shows a message to the user.
 * @param {string} elementId - The ID of the message paragraph element.
 * @param {string} text - The message text.
 * @param {'success' | 'error' | 'info'} type - The message type.
 */
function showMessage(elementId, text, type = 'info') {
    const msgElement = document.getElementById(elementId);
    if (!msgElement) return;
    msgElement.textContent = text;
    msgElement.className = `message ${type}`; // Reset classes and add type
}

/**
 * Hides a message element.
 * @param {string} elementId - The ID of the message paragraph element.
 */
function hideMessage(elementId) {
    const msgElement = document.getElementById(elementId);
    if (msgElement) {
        msgElement.textContent = '';
        msgElement.className = 'message'; // Remove type classes
    }
}


/**
 * Generic fetch wrapper
 * @param {string} endpoint - API endpoint (e.g., '/employees')
 * @param {object} options - Fetch options (method, headers, body)
 * @param {boolean} skipAuth - Whether to skip adding the Auth header
 * @returns {Promise<any>} - Promise resolving with JSON data or rejecting with error
 */
async function fetchApi(endpoint, options = {}) {
    const token = getToken();
    console.log('Making API request to:', `${API_BASE_URL}${endpoint}`);
    console.log('Auth token status:', token ? 'Present' : 'Missing');
    
    const headers = {
        'Accept': 'application/json',
        ...options.headers
    };

    if (!(options.body instanceof FormData)) {
        headers['Content-Type'] = 'application/json';
    }
    if (token) {
        headers['Authorization'] = `Bearer ${token}`;
        console.log('Authorization header added');
    }
    
    console.log('Request headers:', headers);
    
    try {
        const response = await fetch(`${API_BASE_URL}${endpoint}`, {
            ...options,
            headers,
            credentials: 'include'
        });
        
        console.log('Response status:', response.status);
        console.log('Response headers:', Object.fromEntries(response.headers.entries()));
        
        if (response.status === 401) {
            console.log('Authentication required, redirecting to login...');
            window.location.href = '/login.html';
            throw new Error('Not authenticated');
        }
        
        if (!response.ok) {
            const errorText = await response.text();
            console.log('API Error:', errorText);
            throw new Error(`API Error (${response.status}): ${errorText || response.statusText}`);
        }
        
        const data = await response.json();
        console.log('Response data:', data);
        return data;
    } catch (error) {
        console.error('Fetch API Error:', {
            endpoint,
            options,
            error
        });
        throw error;
    }
}

// --- Employee API Calls ---
async function getEmployees(includeInactive = false) {
    const endpoint = includeInactive ? '/employees?includeInactive=true' : '/employees';
    return await fetchApi(endpoint);
}

async function getEmployee(id) {
    return await fetchApi(`/employees/${id}`);
}

async function createEmployee(employeeData) {
    return await fetchApi('/employees', { method: 'POST', body: employeeData });
}

async function updateEmployee(id, employeeData) {
    return await fetchApi(`/employees/${id}`, { method: 'PUT', body: employeeData });
}

// Replaced deleteEmployee
async function deactivateEmployee(id) {
    return await fetchApi(`/employees/${id}/deactivate`, { method: 'PUT' });
}

async function activateEmployee(id) {
    return await fetchApi(`/employees/${id}/activate`, { method: 'PUT' });
}

// --- Schedule API Calls ---

// Schedule
const generateScheduleApi = (startDate, endDate, businessStartTime, businessEndTime) => fetchApi('/schedule/generate', {
    method: 'POST',
    body: JSON.stringify({
        startDate,
        endDate,
        businessStartTime, // Add business hours
        businessEndTime
    }),
}, false);

const getScheduleApi = (startDate, endDate) => {
    const params = new URLSearchParams({
        startDate: startDate, // Assumes YYYY-MM-DD format
        endDate: endDate,     // Assumes YYYY-MM-DD format
    });
    return fetchApi(`/schedule?${params.toString()}`, {}, false);
};

//  --- Import document API Calls ---
// Removed redundant/incorrect 'upload' function.
// The main fetchApi function now handles FormData correctly.


// New function to delete schedule in range
const deleteScheduleApi = (startDate, endDate) => {
    const params = new URLSearchParams({
        startDate: startDate, // Assumes YYYY-MM-DD format
        endDate: endDate,     // Assumes YYYY-MM-DD format
    });
    return fetchApi(`/schedule?${params.toString()}`, { method: 'DELETE' });
};

// --- Task API Calls (New) ---
async function getAllTasks() {
    return await fetchApi('/tasks');
}

async function getTaskById(taskId) {
    return await fetchApi(`/tasks/${taskId}`);
}

async function createTask(taskData) {
    return await fetchApi('/tasks', { method: 'POST', body: taskData });
}

async function updateTask(taskId, taskData) {
    return await fetchApi(`/tasks/${taskId}`, { method: 'PUT', body: taskData });
}

async function deleteTaskApi(taskId) {
    return await fetchApi(`/tasks/${taskId}`, { method: 'DELETE' });
}

// --- Skill API Calls (New) ---
async function getSkills() {
    return await fetchApi('/skills');
}

// Add createSkill etc. later if needed from frontend

// --- Dashboard API Calls (New) ---
async function getDashboardStats() {
    return await fetchApi('/dashboard/stats');
}

async function getDashboardAlerts() {
    return await fetchApi('/dashboard/alerts');
}

// --- Settings API Calls ---
async function getAllSettings() {
    return await fetchApi('/settings');
}

async function getSettingsByPrefix(prefix) {
    try {
        console.log('Fetching settings with prefix:', prefix);
        const response = await fetch(`${API_BASE_URL}/settings/group/${prefix}`, {
            method: 'GET',
            headers: {
                'Accept': 'application/json',
                'Authorization': `Bearer ${getToken()}`
            },
            credentials: 'include'
        });

        console.log('Settings response status:', response.status);
        
        if (!response.ok) {
            let errorMessage = 'Failed to fetch settings';
            try {
                const errorData = await response.json();
                errorMessage = errorData.message || errorMessage;
            } catch (e) {
                console.error("Error parsing error response:", e);
            }
            throw new Error(errorMessage);
        }

        const settings = await response.json();
        console.log('Settings response:', settings);
        
        const result = {};
        
        // Handle both array and object responses
        if (Array.isArray(settings)) {
            settings.forEach(setting => {
                const key = setting.key.replace(prefix, '');
                result[key] = setting.value;
            });
        } else if (typeof settings === 'object' && settings !== null) {
            Object.entries(settings).forEach(([key, value]) => {
                result[key.replace(prefix, '')] = value;
            });
        }
        
        return result;
    } catch (error) {
        console.error('Error fetching settings:', error);
        throw error;
    }
}

async function updateSettings(settings) {
    try {
        const response = await fetchApi('/settings/batch', {
            method: 'POST',
            body: JSON.stringify(settings)
        });
        
        if (!response.ok) {
            throw new Error('Failed to update settings');
        }
        
        return response.json();
    } catch (error) {
        console.error('Error updating settings:', error);
        throw error;
    }
}

// --- Shift API Calls ---
async function deleteShiftApi(shiftId) {
    return await fetchApi(`/shifts/${shiftId}`, { method: 'DELETE' });
}

// --- Auth API Calls (New) ---
async function registerUser(credentials) { // credentials = { username, password }
    // Pass true to skip auth header
    // Ensure the body is stringified for JSON content type
    return await fetchApi('/auth/register', { method: 'POST', body: JSON.stringify(credentials) }, true);
}

async function loginUser(credentials) {
    console.log("Attempting login with credentials:", { ...credentials, password: '[REDACTED]' });
    try {
        const response = await fetch(`${API_BASE_URL}/auth/login`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'Accept': 'application/json'
            },
            credentials: 'include',
            body: JSON.stringify(credentials)
        });

        console.log("Login response status:", response.status);
        
        if (!response.ok) {
            let errorMessage = 'Login failed';
            try {
                const errorData = await response.json();
                errorMessage = errorData.message || errorMessage;
            } catch (e) {
                console.error("Error parsing error response:", e);
            }
            throw new Error(errorMessage);
        }

        const data = await response.json();
        console.log("Login response:", { ...data, token: data?.token ? '[REDACTED]' : null });
        
        if (data && data.token) {
            localStorage.setItem('authToken', data.token);
            localStorage.setItem('username', data.username);
            console.log("Token stored in localStorage");
            return data;
        } else {
            console.error("Login failed: No token in response");
            throw new Error('Login failed: No token received.');
        }
    } catch (error) {
        console.error("Login error:", error);
        throw error;
    }
}

function logoutUser() {
    localStorage.removeItem('authToken');
    localStorage.removeItem('username');
    // No API call needed for stateless JWT logout typically,
    // but could add one if server-side session invalidation is used.
    console.log("User logged out.");
}

function getToken() {
    const token = localStorage.getItem('authToken');
    console.log('Auth token from localStorage:', token ? 'Present' : 'Missing');
    if (!token) {
        console.warn('No authentication token found in localStorage');
        // Check if we're on a protected page
        if (window.location.pathname.includes('/dashboard') || window.location.pathname.includes('/ga-visualization')) {
            console.warn('Attempting to access protected page without authentication');
            // Redirect to login if not already there
            if (!window.location.pathname.includes('/login')) {
                console.log('Redirecting to login page');
                window.location.href = '/login';
            }
        }
    }
    return token;
}

async function checkAuth() {
    try {
        console.log('Checking authentication status...');
        const token = getToken();
        if (!token) {
            console.warn('No authentication token available');
            return false;
        }
        
        console.log('Making authentication check request...');
        await fetchApi('/auth/check', { method: 'GET' });
        console.log('Authentication check successful');
        return true;
    } catch (error) {
        console.error('Authentication check failed:', error);
        // Clear invalid token
        localStorage.removeItem('authToken');
        return false;
    }
}

function getUsername() {
    return localStorage.getItem('username');
}

function isLoggedIn() {
    return !!getToken();
    // Could add token validation/expiry check here for more robustness
}

/**
 * Makes a simple authenticated request to check if the current token is valid.
 * Throws an error if the request fails (e.g., 401 Unauthorized).
 */
async function checkAuthStatus() {
    try {
        // Use fetchApi - it automatically adds the token.
        // We expect a 2xx status if the token is valid.
        // The endpoint doesn't need to return anything complex.
        await fetchApi('/auth/check', { method: 'GET' });
        // If fetchApi doesn't throw, the token is considered valid.
    } catch (error) {
        console.error("Auth status check failed:", error);
        // Re-throw the error so initializeApp can catch it
        throw error;
    }
}

// GA Visualization API endpoints
async function startGaVisualization(startDate, endDate, progressCallback) {
    try {
        console.log('Starting GA visualization with dates:', { startDate, endDate });
        console.log('Current auth token:', getToken() ? 'Present' : 'Missing');
        
        const response = await fetchApi('/ga/start', {
            method: 'POST',
            body: JSON.stringify({
                startDate,
                endDate
            })
        });

        console.log('GA start response:', response);

        if (!response) {
            throw new Error('Failed to start GA visualization');
        }

        // Start polling for status updates
        const visualizationId = response.visualizationId;
        let isActive = true;

        console.log('Starting status polling for visualization:', visualizationId);

        while (isActive) {
            try {
                const statusResponse = await fetchApi(`/ga/status?visualizationId=${visualizationId}`);
                console.log('GA status update:', statusResponse);
                
                if (statusResponse && statusResponse.active) {
                    // Update visualization with progress
                    if (progressCallback) {
                        progressCallback(statusResponse);
                    }

                    // Check if visualization is complete
                    if (statusResponse.completed) {
                        isActive = false;
                    }
                } else {
                    isActive = false;
                }

                // Wait before next status check
                await new Promise(resolve => setTimeout(resolve, 1000));
            } catch (error) {
                console.error('Error checking GA status:', error);
                isActive = false;
                throw error;
            }
        }

        return response;
    } catch (error) {
        console.error('Error in GA visualization:', error);
        throw error;
    }
}
