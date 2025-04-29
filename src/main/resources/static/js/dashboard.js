const sidebar = document.getElementById('sidebar');
const menuButton = document.getElementById('menu-button');
const sidebarOverlay = document.getElementById('sidebar-overlay');
const sidebarLinks = document.querySelectorAll('.sidebar-link');
const contentSections = document.querySelectorAll('.content-section');
const employeeListSection = document.getElementById('manage-employees');
const employeeDetailSection = document.getElementById('employee-detail');
const employeeDetailTitle = document.getElementById('employee-detail-title');
const taskListSection = document.getElementById('manage-tasks');
const taskDetailSection = document.getElementById('task-detail');
const taskDetailTitle = document.getElementById('task-detail-title');
const logoutButton = document.getElementById('logout-button');
const loadingOverlay = document.getElementById('loading-overlay'); // Assuming this exists or will be added
const tabButtons = document.querySelectorAll('#employee-detail .tab-button'); // Moved selector here

let allSkills = [];
let rosterViewInitialized = false; // Flag to track if roster JS is loaded/initialized

function showLoadingOverlay() {
    if(loadingOverlay) loadingOverlay.style.display = 'flex';
}
function hideLoadingOverlay() {
    if(loadingOverlay) loadingOverlay.style.display = 'none';
}

// --- Dynamic Script Loader ---
function loadScript(url, callback) {
    const existingScript = document.querySelector(`script[src="${url}"]`);
    if (existingScript) {
        console.log(`Script ${url} already loaded.`);
        if (callback) callback();
        return;
    }

    const script = document.createElement('script');
    script.src = url;
    script.defer = true;
    script.onload = () => {
        console.log(`Script ${url} loaded successfully.`);
        if (callback) callback();
    };
    script.onerror = () => {
        console.error(`Error loading script: ${url}`);
    };
    document.body.appendChild(script);
}

// --- Navigation & Content Loading ---
async function navigateTo(targetId) {
    contentSections.forEach(section => {
        section.classList.add('hidden');
    });

    const targetSection = document.getElementById(targetId);
    if (targetSection) {
        targetSection.classList.remove('hidden');

        // Special handling for dynamic content sections
        switch (targetId) {
            case 'dashboard':
                loadDashboardData();
                break;
            case 'view-roster':
                await loadRosterView(targetSection);
                break;
            case 'manage-employees':
                loadEmployeeList();
                break;
            case 'manage-tasks':
                loadTaskList();
                break;
            case 'employee-detail':
                 // Handled by showEmployeeDetail, ensure list is hidden
                 if (employeeListSection) employeeListSection.classList.add('hidden');
                 if (taskListSection) taskListSection.classList.add('hidden');
                 break;
            case 'task-detail':
                 if (employeeListSection) employeeListSection.classList.add('hidden');
                 if (taskListSection) taskListSection.classList.add('hidden');
                 break;
            // Call placeholder loaders for other sections
            case 'roster-history':
                loadRosterHistory();
                break;
            case 'rules-constraints':
                loadRulesConstraints();
                break;
            case 'import-data':
                loadImportData();
                break;
            case 'business-settings':
                loadBusinessSettings();
                break;
            case 'account-settings':
                loadAccountSettings();
                break;
            case 'help-support':
                loadHelpSupport();
                break;
            case 'ga-visualization':
                loadGaVisualization();
                break;
            // Add cases for any other sections here
            case 'generate-roster': // Example: If this needs specific logic later
                 // loadGenerateRosterView(); // Add this function if needed
                 console.log(`Navigated to section: ${targetId} (no specific loader yet)`);
                 break;
            default:
                console.log(`Navigated to section: ${targetId} (no specific loader configured)`);
                // Optionally, display a generic 'not implemented' message in the target section
                loadPlaceholderContent(targetId, `Section: ${targetId}`);
                break;
        }

    } else {
        // Fallback to dashboard if targetId is invalid
        const dashboardSection = document.getElementById('dashboard');
        if (dashboardSection) {
             dashboardSection.classList.remove('hidden');
             targetId = 'dashboard';
             loadDashboardData();
        } else {
             console.error(`Target section '${targetId}' and fallback 'dashboard' not found.`);
             return; // Exit if no section found
        }
    }

    // Update sidebar active state
    sidebarLinks.forEach(link => {
        // Check if it's a navigation link with a data-target
        if (link.dataset.target) {
            link.classList.toggle('active', link.dataset.target === targetId);
        }
    });

    // Close sidebar on mobile after navigation
    if (window.innerWidth < 768 && sidebar && sidebarOverlay) {
        sidebar.classList.remove('open');
        sidebarOverlay.style.display = 'none';
    }
}

async function loadRosterView(targetSection) {
    if (!targetSection) return;

    // Check if content is already loaded (simple check)
    if (targetSection.querySelector('#schedule-grid-container')) {
        console.log('Roster HTML already loaded.');
        // Re-initialize if necessary, or assume state is preserved
        if (typeof initializeRosterView === 'function' && !rosterViewInitialized) {
             console.log('Re-initializing roster view...');
             try {
                await initializeRosterView(targetSection);
                rosterViewInitialized = true;
            } catch (err) { console.error("Error re-initializing roster view:", err); }
        }
        return;
    }

    try {
        targetSection.innerHTML = '<p class="text-center p-4">Loading roster view...</p>'; // Placeholder
        const response = await fetch('/roster.html');
        if (!response.ok) {
            throw new Error(`HTTP error! status: ${response.status}`);
        }
        const htmlContent = await response.text();
        targetSection.innerHTML = htmlContent;
        console.log('Roster HTML injected.');

        // Ensure roster.js is loaded and then initialize
        loadScript('/js/roster.js', async () => {
            if (typeof initializeRosterView === 'function') {
                 console.log('Initializing roster view...');
                 try {
                    // Pass the container element to the initializer
                    await initializeRosterView(targetSection); 
                    rosterViewInitialized = true;
                } catch (err) { 
                    console.error("Error initializing roster view:", err);
                    targetSection.innerHTML = `<p class="text-red-600 text-center p-4">Error initializing roster view: ${err.message}</p>`;
                }
            } else {
                console.error('initializeRosterView function not found after loading roster.js');
                targetSection.innerHTML = '<p class="text-red-600 text-center p-4">Error: Roster display script failed to load correctly.</p>';
            }
        });
    } catch (error) {
        console.error('Error loading or initializing roster view:', error);
        targetSection.innerHTML = `<p class="text-red-600 text-center p-4">Error loading roster view: ${error.message}</p>`;
    }
}

// --- Dashboard Specific Functions ---
async function loadDashboardData() {
    const placeholder = document.getElementById('dashboard-content-placeholder');
    const contentContainer = document.getElementById('dashboard'); // Assume a container exists

    if (!contentContainer && placeholder) {
         placeholder.innerHTML = '<p>Loading dashboard structure...</p>';
         // Example: Fetch basic dashboard structure if needed
         // For now, assume structure is in dashboard.html
         // We just need to load data into existing elements
         // If dashboard itself needs dynamic loading, adapt loadRosterView pattern
         console.log("Dashboard content container not found, attempting to load data into placeholders.");
         if(placeholder) placeholder.style.display = 'none'; // Hide placeholder once we start loading real data
     }
     if(placeholder && placeholder.style.display !== 'none') placeholder.style.display = 'none';


    // Example dashboard widgets (assuming elements exist in dashboard.html)
    displayDashboardStats();
    displayDashboardAlerts();
    // Add other dashboard loading functions here
}

async function displayDashboardStats() {
    const statsSection = document.getElementById('dashboard-stats'); // Assume this ID exists
    if (!statsSection) {
        console.warn("Dashboard stats section not found.");
        return;
    }
    statsSection.innerHTML = '<p class="text-center col-span-full">Loading stats...</p>';

    try {
        // Assuming getDashboardStats is defined in api.js or globally
        const stats = await getDashboardStats();

        // Example: Find elements within statsSection and populate
        // This structure depends heavily on your actual dashboard.html
        statsSection.innerHTML = `
            <div class="stat-card">
                 <div class="stat-card-icon bg-blue-100 text-blue-600"><i class="lucide lucide-users"></i></div>
                 <div>
                     <p class="text-sm font-medium text-gray-500">Active Employees</p>
                     <p id="stat-active-employees" class="text-2xl font-semibold text-gray-900">${stats.activeEmployees ?? 'N/A'}</p>
                 </div>
             </div>
             <div class="stat-card">
                 <div class="stat-card-icon bg-green-100 text-green-600"><i class="lucide lucide-clock"></i></div>
                 <div>
                     <p class="text-sm font-medium text-gray-500">Hours This Week</p>
                     <p id="stat-hours-week" class="text-2xl font-semibold text-gray-900">${stats.hoursThisWeek?.toLocaleString() ?? 'N/A'}</p>
                 </div>
             </div>
             <div class="stat-card">
                 <div class="stat-card-icon bg-yellow-100 text-yellow-600"><i class="lucide lucide-calendar-plus"></i></div>
                 <div>
                     <p class="text-sm font-medium text-gray-500">Open Shifts</p>
                     <p id="stat-open-shifts" class="text-2xl font-semibold text-gray-900">${stats.openShifts ?? 'N/A'}</p>
                 </div>
             </div>
             <div class="stat-card">
                 <div class="stat-card-icon bg-red-100 text-red-600"><i class="lucide lucide-plane"></i></div>
                 <div>
                     <p class="text-sm font-medium text-gray-500">Pending Time Off</p>
                     <p id="stat-pending-timeoff" class="text-2xl font-semibold text-gray-900">${stats.pendingTimeOff ?? 'N/A'}</p>
                 </div>
             </div>
        `;

    } catch (error) {
        console.error("Error loading dashboard stats:", error);
        if(statsSection) statsSection.innerHTML = '<p class="text-red-600 text-center col-span-full">Error loading stats.</p>';
    }
}

async function displayDashboardAlerts() {
    const alertsList = document.getElementById('dashboard-alerts-list'); // Assume this ID exists
    if (!alertsList) {
        console.warn("Dashboard alerts list not found.");
        return;
    }
    alertsList.innerHTML = '<li>Loading alerts...</li>';

    try {
        // Assuming getDashboardAlerts is defined in api.js or globally
        const alerts = await getDashboardAlerts();
        alertsList.innerHTML = ''; // Clear loading message

        if (alerts && Array.isArray(alerts) && alerts.length > 0) {
            alerts.forEach(alert => {
                const li = document.createElement('li');
                let iconClass = 'lucide-info';
                let colorClass = 'text-blue-700';
                let iconColor = 'text-blue-500';
                if (alert.level === 'warning') {
                     iconClass = 'lucide-alert-triangle';
                     colorClass = 'text-yellow-700';
                     iconColor = 'text-yellow-500';
                 } else if (alert.level === 'error') {
                     iconClass = 'lucide-alert-circle';
                     colorClass = 'text-red-700';
                     iconColor = 'text-red-500';
                 }
                li.className = `flex items-center text-sm ${colorClass}`;
                li.innerHTML = `
                    <i class="lucide ${iconClass} mr-2 ${iconColor}"></i> ${alert.message || ''}
                `;
                alertsList.appendChild(li);
            });
        } else {
            alertsList.innerHTML = '<li>No current alerts.</li>';
        }
    } catch (error) {
        console.error("Error loading dashboard alerts:", error);
        alertsList.innerHTML = '<li class="text-red-600">Error loading alerts.</li>';
    }
}

// --- Employee Management Functions ---
async function loadEmployeeList() {
    if (!employeeListSection) return;
    const tableBody = employeeListSection.querySelector('tbody');
    if (!tableBody) return;

    tableBody.innerHTML = '<tr><td colspan="4" class="text-center p-4"><i class="lucide lucide-loader-2 animate-spin mr-2"></i> Loading employees...</td></tr>';
    hideMessage('employee-list-message');
    try {
        // Assuming getEmployees is defined in api.js or globally
        const employees = await getEmployees(true);
        tableBody.innerHTML = ''; // Clear loading state
        if (employees && Array.isArray(employees) && employees.length > 0) {
            employees.forEach(emp => {
                const row = document.createElement('tr');
                const statusClass = emp.isActive ? 'bg-green-100 text-green-800' : 'bg-red-100 text-red-800';
                const statusText = emp.isActive ? 'Active' : 'Inactive';
                const actionButtonText = emp.isActive ? 'Deactivate' : 'Activate';
                const actionButtonClass = emp.isActive ? 'btn-danger' : 'btn-primary';

                row.innerHTML = `
                    <td>${emp.name || 'N/A'}</td>
                    <td>${emp.contractHours !== undefined ? emp.contractHours : 'N/A'}</td>
                    <td><span class="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium ${statusClass}">${statusText}</span></td>
                    <td class="space-x-2">
                        <button class="btn btn-outline btn-sm" onclick="showEmployeeDetail('${emp.id}')">Edit</button>
                        <button class="btn ${actionButtonClass} btn-sm" onclick="toggleEmployeeStatus('${emp.id}', ${emp.isActive})">${actionButtonText}</button>
                    </td>
                `;
                tableBody.appendChild(row);
            });
        } else {
            tableBody.innerHTML = '<tr><td colspan="4" class="text-center p-4">No employees found. Add one using the button above.</td></tr>';
        }
    } catch (error) {
        console.error("Error loading employee list:", error);
        showMessage('employee-list-message', `Error loading employees: ${error.message}`, 'error');
        tableBody.innerHTML = '<tr><td colspan="4" class="text-center p-4 text-red-600">Could not load employees.</td></tr>';
    }
}

async function toggleEmployeeStatus(employeeId, isActive) {
    const action = isActive ? 'deactivate' : 'activate';
    const actionVerb = isActive ? 'Deactivating' : 'Activating';
    // Assuming deactivateEmployee and activateEmployee are defined in api.js or globally
    const apiFunction = isActive ? deactivateEmployee : activateEmployee;

    if (!confirm(`Are you sure you want to ${action} this employee?`)) {
        return;
    }

    try {
        showMessage('employee-list-message', `${actionVerb} employee...`, 'info');
        await apiFunction(employeeId);
        showMessage('employee-list-message', `Employee ${action}d successfully.`, 'success', 3000);
        loadEmployeeList(); // Refresh list
    } catch (error) {
        console.error(`Error ${actionVerb.toLowerCase()} employee:`, error);
        showMessage('employee-list-message', `Error ${actionVerb.toLowerCase()} employee: ${error.message}`, 'error');
    }
}

async function showEmployeeDetail(employeeId = null) {
    navigateTo('employee-detail'); // Ensure the section is visible
    if (!employeeDetailSection) return;

    employeeDetailTitle.textContent = employeeId ? 'Edit Employee' : 'Add New Employee';
    const form = document.getElementById('employee-form');
    if(form) form.reset();
    const idInput = document.getElementById('emp-id');
    if(idInput) idInput.value = '';

    // Ensure skills checkboxes are present and populated *before* trying to check them
    await populateSkillsCheckboxes(); // Moved this call earlier

    if (employeeId) {
        try {
            showMessage('employee-detail-message', 'Loading employee details...', 'info');
            // Assuming getEmployee is defined in api.js or globally
            const employeeData = await getEmployee(employeeId);
            hideMessage('employee-detail-message');

            if(idInput) idInput.value = employeeData.id;
            const nameInput = document.getElementById('emp-name');
            if (nameInput) nameInput.value = employeeData.name || '';
            const hoursInput = document.getElementById('emp-contract-hours');
            if (hoursInput) hoursInput.value = employeeData.contractHours || '';
            const availInput = document.getElementById('emp-availability');
            if (availInput) availInput.value = employeeData.availability || '';
            const prefInput = document.getElementById('emp-preferences');
            if (prefInput) prefInput.value = employeeData.preferences || '';

            // Populate new constraint fields
            document.getElementById('emp-max-consecutive-days').value = employeeData.maxConsecutiveDays ?? '';
            document.getElementById('emp-min-consecutive-days').value = employeeData.minConsecutiveDays ?? '';
            document.getElementById('emp-max-weekends').value = employeeData.maxWeekends ?? '';
            document.getElementById('emp-max-total-hours').value = employeeData.maxTotalHours ?? '';
            document.getElementById('emp-min-total-hours').value = employeeData.minTotalHours ?? '';
            document.getElementById('emp-consecutive-day-penalty-weight').value = employeeData.consecutiveDayPenaltyWeight ?? '';
            document.getElementById('emp-weekend-penalty-weight').value = employeeData.weekendPenaltyWeight ?? '';
            document.getElementById('emp-total-hours-penalty-weight').value = employeeData.totalHoursPenaltyWeight ?? '';

            checkEmployeeSkills(employeeData.skills || []);

        } catch (error) {
            console.error(`Error fetching employee ${employeeId}:`, error);
            employeeDetailTitle.textContent = `Error loading employee ${employeeId}`;
            showMessage('employee-detail-message', `Error loading details: ${error.message}`, 'error');
        }
    } else {
        // Reset for new employee
        if(idInput) idInput.value = '';
        hideMessage('employee-detail-message');
        checkEmployeeSkills([]);
        // Clear constraint fields for new employee (rely on defaults or placeholders)
        document.getElementById('emp-max-consecutive-days').value = '';
        document.getElementById('emp-min-consecutive-days').value = '';
        document.getElementById('emp-max-weekends').value = '';
        document.getElementById('emp-max-total-hours').value = '';
        document.getElementById('emp-min-total-hours').value = '';
        document.getElementById('emp-consecutive-day-penalty-weight').value = '';
        document.getElementById('emp-weekend-penalty-weight').value = '';
        document.getElementById('emp-total-hours-penalty-weight').value = '';
    }
    activateTab('basic-info'); // Ensure the first tab is active
}

async function populateSkillsCheckboxes() {
    const container = document.getElementById('skills-checkbox-container');
    if (!container) return;

    container.innerHTML = '<p class="text-gray-500 italic">Loading skills...</p>';
    try {
        // Assuming getSkills is defined in api.js or globally
        allSkills = await getSkills();
        container.innerHTML = ''; // Clear loading
        if (allSkills && allSkills.length > 0) {
            allSkills.forEach(skill => {
                const div = document.createElement('div');
                div.className = 'flex items-center';
                div.innerHTML = `
                    <input id="skill-${skill.id}" name="skills" value="${skill.name}" type="checkbox" class="form-checkbox h-4 w-4 text-indigo-600 border-gray-300 rounded focus:ring-indigo-500">
                    <label for="skill-${skill.id}" class="ml-2 block text-sm text-gray-900">${skill.name}</label>
                `;
                container.appendChild(div);
            });
        } else {
            container.innerHTML = '<p class="text-gray-500">No skills defined. Add skills in settings.</p>';
        }
    } catch (error) {
        console.error("Error loading skills:", error);
        container.innerHTML = '<p class="text-red-600">Error loading skills.</p>';
        allSkills = [];
    }
}

function checkEmployeeSkills(employeeSkillNames) {
    const checkboxes = document.querySelectorAll('#skills-checkbox-container input[type="checkbox"]');
    checkboxes.forEach(checkbox => {
        checkbox.checked = employeeSkillNames.includes(checkbox.value);
    });
}

function getSelectedSkills() {
    const selectedSkills = [];
    const checkboxes = document.querySelectorAll('#skills-checkbox-container input[type="checkbox"]:checked');
    checkboxes.forEach(checkbox => {
        selectedSkills.push(checkbox.value);
    });
    return selectedSkills;
}

async function saveEmployeeDetails(event) {
    event.preventDefault();
    const form = event.target;
    const employeeId = document.getElementById('emp-id').value;
    const isUpdating = !!employeeId;

    const employeeData = {
        name: document.getElementById('emp-name').value.trim(),
        contractHours: parseInt(document.getElementById('emp-contract-hours').value) || 0,
        availability: document.getElementById('emp-availability').value.trim(),
        preferences: document.getElementById('emp-preferences').value.trim(),
        isActive: true, // Default to active, might need adjustment
        skills: getSelectedSkills(),

        // Add new constraint fields
        maxConsecutiveDays: parseInt(document.getElementById('emp-max-consecutive-days').value) || null,
        minConsecutiveDays: parseInt(document.getElementById('emp-min-consecutive-days').value) || null,
        maxWeekends: parseInt(document.getElementById('emp-max-weekends').value) || null,
        maxTotalHours: parseInt(document.getElementById('emp-max-total-hours').value) || null,
        minTotalHours: parseInt(document.getElementById('emp-min-total-hours').value) || null,
        consecutiveDayPenaltyWeight: parseInt(document.getElementById('emp-consecutive-day-penalty-weight').value) || null,
        weekendPenaltyWeight: parseInt(document.getElementById('emp-weekend-penalty-weight').value) || null,
        totalHoursPenaltyWeight: parseInt(document.getElementById('emp-total-hours-penalty-weight').value) || null
    };

    if (isUpdating) {
         try {
             // Preserve current active status when updating
             const currentData = await getEmployee(employeeId);
             employeeData.isActive = currentData.isActive;
         } catch (err) { console.error("Error fetching current employee status:", err); /* Keep default */ }
     }

    if (!employeeData.name || employeeData.contractHours < 0) {
         showMessage('employee-detail-message', 'Please provide a valid name and non-negative contract hours.', 'error');
         return;
    }

    try {
         showMessage('employee-detail-message', isUpdating ? 'Updating employee...' : 'Creating employee...', 'info');
         // Assuming updateEmployee and createEmployee are defined in api.js or globally
        if (isUpdating) {
            await updateEmployee(employeeId, employeeData);
            showMessage('employee-detail-message', 'Employee updated successfully!', 'success', 3000);
        } else {
            await createEmployee(employeeData);
            showMessage('employee-detail-message', 'Employee created successfully!', 'success', 3000);
        }
        // Short delay before navigating back to the list
        setTimeout(() => {
           hideEmployeeDetail();
        }, 1500);
    } catch (error) {
         console.error("Error saving employee:", error);
         showMessage('employee-detail-message', `Error saving employee: ${error.message}`, 'error');
    }
}

function hideEmployeeDetail() {
    if (employeeDetailSection) employeeDetailSection.classList.add('hidden');
    if (employeeListSection) employeeListSection.classList.remove('hidden');
    navigateTo('manage-employees'); // Navigate back and trigger list reload
}

// --- Utility: Messaging ---
/**
 * Shows a message element.
 * @param {string} elementId - The ID of the message paragraph element.
 * @param {string} text - The message text.
 * @param {'success' | 'error' | 'info' | 'warning'} type - The message type.
 * @param {number | null} timeout - Auto-hide timeout in ms (null for persistent).
 */
function showMessage(elementId, text, type = 'info', timeout = null) {
    const msgElement = document.getElementById(elementId);
    if (!msgElement) {
        console.warn(`showMessage: Element with ID ${elementId} not found.`);
        return;
    }

    let bgColor, textColor, iconClass;
    switch (type) {
        case 'success':
            bgColor = 'bg-green-100'; textColor = 'text-green-800'; iconClass = 'lucide-check-circle';
            break;
        case 'error':
            bgColor = 'bg-red-100'; textColor = 'text-red-800'; iconClass = 'lucide-alert-circle';
            break;
        case 'warning':
            bgColor = 'bg-yellow-100'; textColor = 'text-yellow-800'; iconClass = 'lucide-alert-triangle';
            break;
        default: // info
            bgColor = 'bg-blue-100'; textColor = 'text-blue-800'; iconClass = 'lucide-info';
            break;
    }

    msgElement.innerHTML = `<i class="lucide ${iconClass} mr-2 inline-block"></i> ${text}`;
    msgElement.className = `p-3 rounded-md ${bgColor} ${textColor} text-sm mb-4 flex items-center`; // Reset classes and add type + flex
    msgElement.style.display = 'flex';

    // Clear previous timeout if exists
    if (msgElement.dataset.timeoutId) {
        clearTimeout(parseInt(msgElement.dataset.timeoutId));
    }

    if (timeout && timeout > 0) {
        const timeoutId = setTimeout(() => hideMessage(elementId), timeout);
        msgElement.dataset.timeoutId = timeoutId.toString();
    } else {
        delete msgElement.dataset.timeoutId; // Remove attribute if no timeout
    }
}

/**
 * Hides a message element.
 * @param {string} elementId - The ID of the message paragraph element.
 */
function hideMessage(elementId) {
    const msgElement = document.getElementById(elementId);
    if (msgElement) {
        msgElement.style.display = 'none';
        msgElement.innerHTML = ''; // Clear content
        // Clear timeout if it exists
        if (msgElement.dataset.timeoutId) {
            clearTimeout(parseInt(msgElement.dataset.timeoutId));
            delete msgElement.dataset.timeoutId;
        }
    }
}

// --- Utility: Tabs ---
function activateTab(targetTabId) {
     const tabButtons = document.querySelectorAll('#employee-detail .tab-button');
     const tabContents = document.querySelectorAll('#employee-detail .tab-content');
     tabButtons.forEach(button => {
        button.classList.toggle('active', button.dataset.tab === targetTabId);
    });
    tabContents.forEach(content => {
        if (content) {
            const isActive = content.id === targetTabId;
            content.classList.toggle('active', isActive);
            content.classList.toggle('hidden', !isActive);
        }
    });
}

// --- Event Listeners Setup ---
function setupEventListeners() {
    // Sidebar links
    sidebarLinks.forEach(link => {
        // Handle only links with data-target for SPA navigation
        if (link.dataset.target) {
             link.addEventListener('click', (e) => {
                e.preventDefault();
                navigateTo(link.dataset.target);
            });
        }
        // Exclude logout and external links if any
    });

    // Mobile menu toggle
    if (menuButton && sidebar && sidebarOverlay) {
        menuButton.addEventListener('click', () => {
            sidebar.classList.toggle('open');
            sidebarOverlay.style.display = sidebar.classList.contains('open') ? 'block' : 'none';
        });
        sidebarOverlay.addEventListener('click', () => {
             sidebar.classList.remove('open');
             sidebarOverlay.style.display = 'none';
         });
    }

    // Logout button
    if(logoutButton) {
        logoutButton.addEventListener('click', (e) => {
            e.preventDefault();
            // Assuming logoutUser is defined in api.js or globally
            logoutUser();
            window.location.href = '/login.html'; // Redirect after logout
        });
    }

    // Employee Detail Tabs
    if (tabButtons) {
        tabButtons.forEach(button => {
            button.addEventListener('click', () => {
                activateTab(button.dataset.tab);
            });
        });
    }

    // Employee Form Submission
    const employeeForm = document.getElementById('employee-form');
    if (employeeForm) {
        employeeForm.addEventListener('submit', saveEmployeeDetails);
    }

    // Add other global or persistent listeners here

    // Add listener for Generate Roster form
    const generateRosterForm = document.getElementById('generate-roster-form'); // Make sure form has this ID
    if(generateRosterForm) {
        generateRosterForm.addEventListener('submit', handleGenerateRosterSubmit);
    }
}

// --- Initialization ---
document.addEventListener('DOMContentLoaded', () => {
    // Basic Auth Check (moved from inline script)
    if (typeof isLoggedIn !== 'function' || !isLoggedIn()) {
         console.log("User not logged in or auth check failed, redirecting to login page.");
         window.location.href = '/login.html';
         return; // Stop further initialization if not logged in
     }

    console.log("Dashboard Initializing...");
    setupEventListeners();

    // Navigate to the initial section (e.g., dashboard or based on URL hash)
    const initialTarget = window.location.hash ? window.location.hash.substring(1) : 'dashboard';
    // Check if initialTarget is a valid section ID before navigating
    const validTargets = Array.from(contentSections).map(s => s.id);
    if (validTargets.includes(initialTarget)) {
         navigateTo(initialTarget);
    } else {
         navigateTo('dashboard'); // Default to dashboard
    }

    // Initial setup for specific views if needed (e.g., ensure first tab is active)
    activateTab('basic-info');

    console.log("Dashboard Initialized.");
});

// Note: Assumes api.js (with isLoggedIn, API calls etc.) is loaded before this script
// or functions are globally available. 


// Add these functions somewhere in dashboard.js

// --- Business Settings Section ---
async function loadBusinessSettings() {
    const form = document.getElementById('business-settings-form');
    const messageDiv = document.getElementById('business-settings-message');
    if (!form) return;
    hideMessage('business-settings-message');
    showLoadingOverlay();

    try {
        // Fetch settings with a relevant prefix (e.g., "business.")
        const settings = await getSettingsByPrefix('business.');
        // Add other prefixes if needed (e.g., employee.default)
        // const empDefaults = await getSettingsByPrefix('employee.default.');
        // const allSettings = { ...settings, ...empDefaults };

        // Populate form fields
        for (const key in settings) {
            const input = form.elements[key];
            if (input) {
                input.value = settings[key];
            } else {
                console.warn(`Input field not found for business setting key: ${key}`);
            }
        }
    } catch (error) {
        console.error("Error loading business settings:", error);
        showMessage('business-settings-message', `Error loading settings: ${error.message}`, 'error');
    } finally {
        hideLoadingOverlay();
    }
}

async function saveBusinessSettings(event) {
    event.preventDefault();
    const form = document.getElementById('business-settings-form');
    const messageDiv = document.getElementById('business-settings-message');
    if (!form) return;
    hideMessage('business-settings-message');

    const settingsToUpdate = {};
    const formData = new FormData(form);

    for (let [key, value] of formData.entries()) {
        if (value.trim() !== '') { // Save non-empty values
            settingsToUpdate[key] = value.trim();
        }
        // Add validation if needed
    }

    if (Object.keys(settingsToUpdate).length === 0) {
        showMessage('business-settings-message', 'No settings were changed.', 'info');
        return;
    }

    try {
        showLoadingOverlay();
        await updateSettings(settingsToUpdate); // Use the general updateSettings API call
        hideLoadingOverlay();
        showMessage('business-settings-message', 'Business settings saved successfully!', 'success', 3000);
    } catch (error) {
        console.error("Error saving business settings:", error);
        showMessage('business-settings-message', `Error saving settings: ${error.message}`, 'error');
        hideLoadingOverlay();
    }
}


// Add this inside the setupEventListeners function or equivalent initialization block:

    // Add listener for Business Settings form
    const businessSettingsForm = document.getElementById('business-settings-form');
    if (businessSettingsForm) {
        businessSettingsForm.addEventListener('submit', saveBusinessSettings);
    }





// --- Placeholder Loaders for Other Sections ---


function loadPlaceholderContent(targetId, title) {
    const section = document.getElementById(targetId);
    if (section) {
        section.innerHTML = `
            <h1 class="text-2xl md:text-3xl font-bold text-gray-800 mb-6">${title}</h1>
            <div class="bg-white p-6 rounded-lg shadow-sm border border-gray-200">
                <p class="text-gray-500 italic">This section is not yet fully implemented.</p>
            </div>
        `;
    }
}

function loadRosterHistory() { loadPlaceholderContent('roster-history', 'Roster History'); }

async function loadRulesConstraints() {
    const form = document.getElementById('rules-form');
    const messageDiv = document.getElementById('rules-message');
    if (!form) return;
    hideMessage('rules-message');
    showLoadingOverlay();

    try {
        // Fetch settings for relevant prefixes
        const gaSettings = await getSettingsByPrefix('ga.');
        const penaltySettings = await getSettingsByPrefix('penalty.');
        const thresholdSettings = await getSettingsByPrefix('threshold.');
        const allSettings = { ...gaSettings, ...penaltySettings, ...thresholdSettings };

        // Populate form fields
        for (const key in allSettings) {
            const input = form.elements[key];
            if (input) {
                input.value = allSettings[key];
            } else {
                console.warn(`Input field not found for setting key: ${key}`);
            }
        }
    } catch (error) {
        console.error("Error loading rules settings:", error);
        showMessage('rules-message', `Error loading settings: ${error.message}`, 'error');
    } finally {
        hideLoadingOverlay();
    }
}

async function saveRulesSettings(event) {
    event.preventDefault();
    const form = document.getElementById('rules-form');
    const messageDiv = document.getElementById('rules-message');
    if (!form) return;
    hideMessage('rules-message');

    const settingsToUpdate = {};
    const formData = new FormData(form);

    // Collect values from form inputs that have a name
    for (let [key, value] of formData.entries()) {
        if (value.trim() !== '') { // Only save non-empty values
            settingsToUpdate[key] = value.trim();
        }
        // Optional: Add validation here (e.g., ensure rates are between 0 and 1)
    }

    if (Object.keys(settingsToUpdate).length === 0) {
        showMessage('rules-message', 'No settings were changed.', 'info');
        return;
    }

    try {
        showLoadingOverlay();
        await updateSettings(settingsToUpdate);
        hideLoadingOverlay();
        showMessage('rules-message', 'Rules and constraints saved successfully!', 'success', 3000);
        // Reload settings in ScheduleService on next generation?
        // Or trigger a backend endpoint to reload if service is singleton?
    } catch (error) {
        console.error("Error saving rules settings:", error);
        showMessage('rules-message', `Error saving settings: ${error.message}`, 'error');
        hideLoadingOverlay();
    }
}

function loadImportData() {
    // Clear any existing messages when the section is loaded
    hideMessage('import-message');
    
    // Set up event listeners for all three import forms
    const importEmployeesForm = document.getElementById('import-employees-form');
    const importSkillsForm = document.getElementById('import-skills-form');
    const importTasksForm = document.getElementById('import-tasks-form');
    
    if (importEmployeesForm) {
        importEmployeesForm.addEventListener('submit', (event) => handleImportSubmit(event, 'employees'));
    }
    
    if (importSkillsForm) {
        importSkillsForm.addEventListener('submit', (event) => handleImportSubmit(event, 'skills'));
    }
    
    if (importTasksForm) {
        importTasksForm.addEventListener('submit', (event) => handleImportSubmit(event, 'tasks'));
    }
}

async function handleImportSubmit(event, importType) {
    event.preventDefault();
    const fileInput = document.getElementById(`import-${importType}-file`);
    const file = fileInput.files[0];
    
    if (!file) {
        showMessage('import-message', `Please select a file to import ${importType}`, 'error');
        return;
    }
    
    if (!file.name.endsWith('.xlsx')) {
        showMessage('import-message', 'Please select an Excel (.xlsx) file', 'error');
        return;
    }
    
    const formData = new FormData();
    formData.append('file', file);
    formData.append('type', importType);
    
    try {
        showLoadingOverlay();
        const response = await fetchApi(`/${importType}/import`, {
            method: 'POST',
            body: formData
        });
        
        showMessage('import-message', `Successfully imported ${importType} data`, 'success');
        fileInput.value = ''; // Clear the file input
    } catch (error) {
        console.error(`Error importing ${importType}:`, error);
        showMessage('import-message', `Error: ${error.message}`, 'error');
    } finally {
        hideLoadingOverlay();
    }
}

function loadBusinessSettings() { loadPlaceholderContent('business-settings', 'Business Settings'); }

function loadAccountSettings() {
    loadPlaceholderContent('account-settings', 'Account Settings');
    // Attempt to load username if available (e.g., from localStorage)
    const username = localStorage.getItem('username');
    const usernameInput = document.getElementById('account-username');
    if (usernameInput && username) {
        usernameInput.value = username;
    }
}

function loadHelpSupport() { loadPlaceholderContent('help-support', 'Help / Support'); }

// GA Visualization loader
function loadGaVisualization() {
    const section = document.getElementById('ga-visualization');
    if (section) {
        // Set the inner HTML first
        section.innerHTML = `
            <h1 class="text-2xl md:text-3xl font-bold text-gray-800 mb-6">Genetic Algorithm Visualization</h1>
            <div class="bg-white p-6 rounded-lg shadow-sm border border-gray-200">
                <div class="mb-6">
                    <h2 class="text-xl font-semibold text-gray-700 mb-4">GA Parameters & Penalties</h2>
                    <!-- GA Parameters Form -->
                    <form id="ga-params-form" class="space-y-4 mb-6 pb-6 border-b border-gray-200">
                        <h3 class="text-lg font-medium text-gray-700 mb-2">Algorithm Settings</h3>
                        <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
                            <div class="form-group">
                                <label for="ga.population.size" class="block text-sm font-medium text-gray-700">Population Size</label>
                                <input type="number" id="ga.population.size" name="ga.population.size" min="10" max="500" 
                                    class="input-field" placeholder="e.g., 50">
                            </div>
                            <div class="form-group">
                                <label for="ga.max.generations" class="block text-sm font-medium text-gray-700">Max Generations</label>
                                <input type="number" id="ga.max.generations" name="ga.max.generations" min="10" max="1000" 
                                    class="input-field" placeholder="e.g., 100">
                            </div>
                            <div class="form-group">
                                <label for="ga.mutation.rate" class="block text-sm font-medium text-gray-700">Mutation Rate</label>
                                <input type="number" id="ga.mutation.rate" name="ga.mutation.rate" min="0" max="1" step="0.01" 
                                    class="input-field" placeholder="e.g., 0.1">
                            </div>
                            <div class="form-group">
                                <label for="ga.crossover.rate" class="block text-sm font-medium text-gray-700">Crossover Rate</label>
                                <input type="number" id="ga.crossover.rate" name="ga.crossover.rate" min="0" max="1" step="0.01" 
                                    class="input-field" placeholder="e.g., 0.8">
                            </div>
                            <div class="form-group">
                                <label for="ga.tournament.size" class="block text-sm font-medium text-gray-700">Tournament Size</label>
                                <input type="number" id="ga.tournament.size" name="ga.tournament.size" min="2" max="20" 
                                    class="input-field" placeholder="e.g., 5">
                            </div>
                        </div>
                        <div class="flex justify-end">
                             <button type="submit" class="btn-secondary">Save Parameters</button>
                        </div>
                         <div id="ga-params-message" class="message mt-4"></div>
                    </form>
                    
                     <!-- Penalty Weights Form -->
                    <form id="ga-penalties-form" class="space-y-4">
                         <h3 class="text-lg font-medium text-gray-700 mb-2">Penalty Weights</h3>
                         <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
                            <div class="form-group">
                                <label for="penalty.hard.constraint" class="block text-sm font-medium text-gray-700">Hard Constraint</label>
                                <input type="number" id="penalty.hard.constraint" name="penalty.hard.constraint" min="1" 
                                    class="input-field" placeholder="e.g., 1000">
                            </div>
                            <div class="form-group">
                                <label for="penalty.under.staffing" class="block text-sm font-medium text-gray-700">Under Staffing (Base)</label>
                                <input type="number" id="penalty.under.staffing" name="penalty.under.staffing" min="0" 
                                    class="input-field" placeholder="e.g., 500">
                            </div>
                            <div class="form-group">
                                <label for="penalty.over.staffing" class="block text-sm font-medium text-gray-700">Over Staffing (Base)</label>
                                <input type="number" id="penalty.over.staffing" name="penalty.over.staffing" min="0" 
                                    class="input-field" placeholder="e.g., 50">
                            </div>
                            <div class="form-group">
                                <label for="penalty.forbidden.succession" class="block text-sm font-medium text-gray-700">Forbidden Succession</label>
                                <input type="number" id="penalty.forbidden.succession" name="penalty.forbidden.succession" min="0" 
                                    class="input-field" placeholder="e.g., 200">
                            </div>
                        </div>
                        <div class="flex justify-end">
                             <button type="submit" class="btn-secondary">Save Penalties</button>
                        </div>
                        <div id="ga-penalties-message" class="message mt-4"></div>
                    </form>
                </div>

                <!-- Run Visualization Form -->
                 <div class="mb-6 pt-6 border-t border-gray-200">
                    <h2 class="text-xl font-semibold text-gray-700 mb-4">Run Visualization</h2>
                    <!-- Corrected Form ID here -->
                    <form id="ga-visualization-form" class="space-y-4">
                         <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
                            <div class="form-group">
                                <label for="ga-start-date" class="block text-sm font-medium text-gray-700">Start Date</label>
                                <input type="date" id="ga-start-date" name="startDate" required 
                                    class="input-field">
                            </div>
                             <div class="form-group">
                                <label for="ga-end-date" class="block text-sm font-medium text-gray-700">End Date</label>
                                <input type="date" id="ga-end-date" name="endDate" required 
                                    class="input-field">
                            </div>
                        </div>
                        <div class="flex justify-end">
                             <button type="submit" id="run-ga-button" class="btn-primary">Run GA Visualization</button>
                             <!-- Stop button will be added dynamically by JS -->
                        </div>
                        <div id="ga-run-message" class="message mt-4"></div>
                    </form>
                </div>
                
                <!-- Visualization Output Area -->
                <div class="mt-8">
                    <h2 class="text-xl font-semibold text-gray-700 mb-4">Visualization Output</h2>
                    <div class="bg-gray-50 p-4 rounded-lg border border-gray-200 min-h-[200px]">
                        <!-- Progress Bars -->
                        <div id="ga-progress-container" class="mb-4" style="display: none;">
                            <div class="flex justify-between mb-1">
                                <span id="ga-status" class="text-sm font-medium text-gray-700">Generation: 0/0</span>
                                <span class="text-sm font-medium text-gray-700">Progress</span>
                            </div>
                            <div class="w-full bg-gray-200 rounded-full h-2.5 mb-4">
                                <div id="generation-progress" class="bg-blue-600 h-2.5 rounded-full" style="width: 0%"></div>
                            </div>
                            <div class="w-full bg-gray-200 rounded-full h-2.5">
                                <div id="fitness-progress" class="bg-green-600 h-2.5 rounded-full" style="width: 0%"></div>
                            </div>
                        </div>
                        
                        <div id="ga-stats-container" class="grid grid-cols-1 md:grid-cols-3 gap-4 mb-6" style="display: none;">
                            <div class="stat-card-sm">
                                <div class="text-sm text-gray-500">Best Fitness</div>
                                <div id="ga-best-fitness" class="text-lg font-semibold text-green-600">N/A</div>
                            </div>
                            <div class="stat-card-sm">
                                <div class="text-sm text-gray-500">Average Fitness</div>
                                <div id="ga-avg-fitness" class="text-lg font-semibold text-blue-600">N/A</div>
                            </div>
                            <div class="stat-card-sm">
                                <div class="text-sm text-gray-500">Worst Fitness</div>
                                <div id="ga-worst-fitness" class="text-lg font-semibold text-red-600">N/A</div>
                            </div>
                        </div>
                        
                        <div id="ga-chart-container" class="h-80" style="display: none;">
                            <canvas id="fitnessChart"></canvas> <!-- Corrected ID -->
                        </div>
                        <div id="ga-placeholder" class="text-center text-gray-500 italic">
                            Run the visualization to see the chart and statistics.
                        </div>
                    </div>
                </div>
            </div>
        `;
        
        // Now load the GA visualization script and initialize it AFTER the HTML is in place
        loadScript('/js/ga-visualization.js', function() {
            console.log('GA visualization script loaded');
            if (typeof initGaVisualization === 'function') {
                try {
                    initGaVisualization(); // Initialize the GA visualization specific JS
                    console.log('GA visualization initialized.');
                    // Attach form submit listeners after initialization
                    const paramsForm = document.getElementById('ga-params-form');
                    if(paramsForm) paramsForm.addEventListener('submit', saveGaParameters);
                    const penaltiesForm = document.getElementById('ga-penalties-form');
                    if(penaltiesForm) penaltiesForm.addEventListener('submit', saveGaPenalties);
                    
                    // Set default dates after initialization
                    setDefaultDates(); 
                } catch (error) {
                    console.error("Error initializing GA visualization:", error);
                    const section = document.getElementById('ga-visualization');
                    if(section) {
                        const errorDiv = document.createElement('div');
                        errorDiv.className = 'text-red-600 p-4 bg-red-100 rounded border border-red-300 mt-4';
                        errorDiv.textContent = 'Error initializing visualization controls. Please check the console.';
                        section.appendChild(errorDiv);
                    }
                }
            } else {
                console.error("initGaVisualization function not found after loading script.");
                 const section = document.getElementById('ga-visualization');
                 if(section) {
                     const errorDiv = document.createElement('div');
                     errorDiv.className = 'text-red-600 p-4 bg-red-100 rounded border border-red-300 mt-4';
                     errorDiv.textContent = 'Could not load visualization controls correctly.';
                     section.appendChild(errorDiv);
                 }
            }
        });
    } else {
        console.error("Target section 'ga-visualization' not found.");
    }
}

// --- Task Management Functions ---

async function loadTaskList() {
    if (!taskListSection) return;
    const tableBody = taskListSection.querySelector('tbody');
    const messageDiv = document.getElementById('task-list-message');
    if (!tableBody) return;
    hideMessage('task-list-message');

    tableBody.innerHTML = '<tr><td colspan="6" class="placeholder"><i class="lucide lucide-loader-2 animate-spin mr-2"></i> Loading tasks...</td></tr>';

    try {
        const tasks = await getAllTasks(); // Assumes function exists in api.js
        if (tasks && tasks.length > 0) {
            tableBody.innerHTML = ''; // Clear loading state
            tasks.forEach(task => {
                const row = document.createElement('tr');
                row.innerHTML = `
                    <td>${task.name || 'N/A'}</td>
                    <td>${task.description || ''}</td>
                    <td>${task.minimumCoverage ?? 'N/A'}</td>
                    <td>${task.optimalCoverage ?? 'N/A'}</td>
                    <td>${task.penaltyWeight ?? 'N/A'}</td>
                    <td class="actions">
                        <button class="btn-icon text-blue-600 hover:text-blue-800" onclick="showTaskDetail(${task.id})">
                            <i class="lucide lucide-edit-3"></i>
                        </button>
                        <button class="btn-icon text-red-600 hover:text-red-800" onclick="deleteTask(${task.id})">
                            <i class="lucide lucide-trash-2"></i>
                        </button>
                    </td>
                `;
                tableBody.appendChild(row);
            });
        } else {
            tableBody.innerHTML = '<tr><td colspan="6" class="text-center py-4">No tasks found.</td></tr>';
        }
    } catch (error) {
        console.error('Error loading tasks:', error);
        tableBody.innerHTML = '<tr><td colspan="6" class="error">Error loading tasks. Please try again.</td></tr>';
        showMessage('task-list-message', `Error loading tasks: ${error.message}`, 'error');
    }
}

async function showTaskDetail(taskId = null) {
    if (!taskListSection || !taskDetailSection || !taskDetailTitle) return;
    const form = document.getElementById('task-form');
    const messageDiv = document.getElementById('task-detail-message');
    if (!form) return;
    hideMessage('task-detail-message');
    form.reset();
    document.getElementById('task-id').value = '';
    // Ensure skills are populated for selection
    await populateTaskSkillsCheckboxes();

    if (taskId) {
        taskDetailTitle.textContent = 'Edit Task';
        try {
            showLoadingOverlay();
            const task = await getTaskById(taskId); // Assumes function exists in api.js
            if (task) {
                document.getElementById('task-id').value = task.id;
                document.getElementById('task-name').value = task.name || '';
                document.getElementById('task-description').value = task.description || '';
                document.getElementById('task-min-coverage').value = task.minimumCoverage ?? '';
                document.getElementById('task-opt-coverage').value = task.optimalCoverage ?? '';
                document.getElementById('task-penalty-weight').value = task.penaltyWeight ?? '';
                // Check the required skills for this task
                checkTaskSkills(task.requiredSkills || []); // Assumes DTO includes requiredSkills (names or IDs)
            } else {
                showMessage('task-detail-message', 'Task not found.', 'error');
                hideLoadingOverlay();
                return; // Don't show detail section if task not found
            }
        } catch (error) {
            console.error('Error fetching task details:', error);
            showMessage('task-detail-message', `Error loading task: ${error.message}`, 'error');
            hideLoadingOverlay();
            return;
        }
        hideLoadingOverlay();
    } else {
        taskDetailTitle.textContent = 'Add New Task';
        // Clear potential skill selections etc.
        checkTaskSkills([]); // Uncheck all skills for new task
    }

    taskListSection.classList.add('hidden');
    taskDetailSection.classList.remove('hidden');
}

function hideTaskDetail() {
    if (!taskListSection || !taskDetailSection) return;
    taskDetailSection.classList.add('hidden');
    taskListSection.classList.remove('hidden');
    loadTaskList(); // Refresh list when going back
}

async function saveTaskDetails(event) {
    event.preventDefault();
    const form = document.getElementById('task-form');
    const taskId = document.getElementById('task-id').value;
    const messageDiv = document.getElementById('task-detail-message');
    if (!form) return;
    hideMessage('task-detail-message');

    const taskData = {
        name: document.getElementById('task-name').value.trim(),
        description: document.getElementById('task-description').value.trim(),
        minimumCoverage: parseInt(document.getElementById('task-min-coverage').value) || 1,
        optimalCoverage: parseInt(document.getElementById('task-opt-coverage').value) || 1,
        penaltyWeight: parseInt(document.getElementById('task-penalty-weight').value) || 10,
        // Get selected skills for the task
        requiredSkills: getSelectedTaskSkills()
    };

    if (!taskData.name) {
        showMessage('task-detail-message', 'Task Name is required.', 'error');
        return;
    }

    try {
        showLoadingOverlay();
        let savedTask;
        if (taskId) {
            savedTask = await updateTask(taskId, taskData); // Assumes function exists in api.js
            showMessage('task-detail-message', 'Task updated successfully!', 'success', 3000);
        } else {
            savedTask = await createTask(taskData); // Assumes function exists in api.js
            showMessage('task-detail-message', 'Task created successfully!', 'success', 3000);
            // Optionally switch back to list after creation
            // hideTaskDetail();
            // Set ID in form for potential immediate edits?
             document.getElementById('task-id').value = savedTask.id;
             taskDetailTitle.textContent = 'Edit Task'; // Update title
        }
        hideLoadingOverlay();
    } catch (error) {
        console.error('Error saving task:', error);
        showMessage('task-detail-message', `Error saving task: ${error.message}`, 'error');
        hideLoadingOverlay();
    }
}

async function deleteTask(taskId) {
    if (!confirm('Are you sure you want to delete this task? This cannot be undone.')) {
        return;
    }

    try {
        showLoadingOverlay();
        await deleteTaskApi(taskId); // Assumes function exists in api.js
        hideLoadingOverlay();
        showMessage('task-list-message', 'Task deleted successfully!', 'success', 3000);
        loadTaskList(); // Refresh the list
    } catch (error) {
        console.error('Error deleting task:', error);
        hideLoadingOverlay();
        showMessage('task-list-message', `Error deleting task: ${error.message}`, 'error');
    }
}

// --- Helper Functions for Task Skills ---
async function populateTaskSkillsCheckboxes() {
    const container = document.getElementById('task-skills-checkbox-container');
    if (!container) return;

    // Avoid re-fetching if allSkills is already populated
    if (allSkills.length === 0) {
        container.innerHTML = '<p class="text-gray-500 italic">Loading skills...</p>';
        try {
            allSkills = await getSkills(); // Fetch from API if not already loaded
        } catch (error) {
            console.error("Error loading skills for task form:", error);
            container.innerHTML = '<p class="text-red-600">Error loading skills.</p>';
            allSkills = [];
            return;
        }
    }

    container.innerHTML = ''; // Clear loading/previous checkboxes
    if (allSkills && allSkills.length > 0) {
        allSkills.forEach(skill => {
            const div = document.createElement('div');
            div.className = 'flex items-center';
            div.innerHTML = `
                <input id="task-skill-${skill.id}" name="task-skills" value="${skill.name}" type="checkbox" class="form-checkbox h-4 w-4 text-indigo-600 border-gray-300 rounded focus:ring-indigo-500">
                <label for="task-skill-${skill.id}" class="ml-2 block text-sm text-gray-900">${skill.name}</label>
            `;
            container.appendChild(div);
        });
    } else {
        container.innerHTML = '<p class="text-gray-500">No skills defined.</p>';
    }
}

function checkTaskSkills(taskSkillNames) {
    const checkboxes = document.querySelectorAll('#task-skills-checkbox-container input[type="checkbox"]');
    checkboxes.forEach(checkbox => {
        checkbox.checked = taskSkillNames.includes(checkbox.value);
    });
}

function getSelectedTaskSkills() {
    const selectedSkills = [];
    const checkboxes = document.querySelectorAll('#task-skills-checkbox-container input[type="checkbox"]:checked');
    checkboxes.forEach(checkbox => {
        selectedSkills.push(checkbox.value);
    });
    return selectedSkills;
}

// --- Utility Functions ---

// Function to save ONLY the constraints from the new tab
// This assumes the main employee details are saved separately
async function saveEmployeeConstraints() {
    const employeeId = document.getElementById('emp-id').value;
    const messageDiv = document.getElementById('employee-detail-message');
    if (!employeeId) {
        showMessage(messageDiv, "Cannot save constraints: No employee loaded.", 'error');
        return;
    }
    hideMessage('employee-detail-message');

    const employeeDataUpdate = {
        maxConsecutiveDays: parseInt(document.getElementById('emp-max-consecutive-days').value) || null,
        minConsecutiveDays: parseInt(document.getElementById('emp-min-consecutive-days').value) || null,
        maxWeekends: parseInt(document.getElementById('emp-max-weekends').value) || null,
        maxTotalHours: parseInt(document.getElementById('emp-max-total-hours').value) || null,
        minTotalHours: parseInt(document.getElementById('emp-min-total-hours').value) || null,
        consecutiveDayPenaltyWeight: parseInt(document.getElementById('emp-consecutive-day-penalty-weight').value) || null,
        weekendPenaltyWeight: parseInt(document.getElementById('emp-weekend-penalty-weight').value) || null,
        totalHoursPenaltyWeight: parseInt(document.getElementById('emp-total-hours-penalty-weight').value) || null
    };

    try {
        showLoadingOverlay();
        // Fetch existing employee data first to merge
        const existingEmployee = await getEmployeeById(employeeId);
        if (!existingEmployee) {
            showMessage(messageDiv, "Error: Could not find existing employee to update constraints.", 'error');
            hideLoadingOverlay();
            return;
        }

        // Merge constraint updates into existing data
        const fullEmployeeData = { ...existingEmployee, ...employeeDataUpdate };

        await updateEmployee(employeeId, fullEmployeeData);
        hideLoadingOverlay();
        showMessage('employee-detail-message', 'Employee constraints updated successfully!', 'success', 3000);
    } catch (error) {
        console.error('Error saving employee constraints:', error);
        showMessage('employee-detail-message', `Error saving constraints: ${error.message}`, 'error');
        hideLoadingOverlay();
    }
}

// --- Generate Roster Handling ---
async function handleGenerateRosterSubmit(event) {
    event.preventDefault();
    const form = event.target;
    const startDateInput = document.getElementById('roster-start-date');
    const endDateInput = document.getElementById('roster-end-date');
    const messageDiv = document.getElementById('generate-roster-message'); // Add message div to form
    const submitButton = form.querySelector('button[type="submit"]');

    hideMessage('generate-roster-message');

    const startDate = startDateInput.value;
    const endDate = endDateInput.value;

    if (!startDate || !endDate) {
        showMessage('generate-roster-message', 'Please select both a start and end date.', 'warning');
        return;
    }
    if (new Date(endDate) < new Date(startDate)) {
         showMessage('generate-roster-message', 'End date cannot be before start date.', 'warning');
         return;
    }

    // Disable button and show loading
    if (submitButton) submitButton.disabled = true;
    showMessage('generate-roster-message', 'Generating roster... This may take some time depending on the period and constraints.', 'info');
    showLoadingOverlay();

    try {
        // Call the API (assuming generateScheduleApi exists in api.js)
        // Pass null for business hours for now, as they were removed from DTO in backend
        const generatedSchedule = await generateScheduleApi(startDate, endDate, null, null);
        hideLoadingOverlay();
        showMessage('generate-roster-message', `Successfully generated roster with ${generatedSchedule.length} shifts.`, 'success', 5000);
        
        // Optional: Navigate to view roster after generation
        // navigateTo('view-roster'); 
        // Need to ensure view-roster automatically fetches the newly generated range

    } catch (error) {
        hideLoadingOverlay();
        console.error("Error generating roster:", error);
        showMessage('generate-roster-message', `Error generating roster: ${error.message || 'Unknown error'}`, 'error');
    } finally {
        if (submitButton) submitButton.disabled = false;
    }
}