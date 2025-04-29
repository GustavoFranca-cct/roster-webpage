// GA Visualization JavaScript
// This file handles the visualization of the genetic algorithm in real-time

// Chart.js for visualization
// Use window scope for the chart variable to ensure consistency
window.fitnessChart = null; 
let currentVisualization = null;

// Initialize the GA visualization page
function initGaVisualization() {
    console.log("Initializing GA Visualization...");
    
    // Load Chart.js if not already loaded
    if (typeof Chart === 'undefined') {
        loadScript('https://cdn.jsdelivr.net/npm/chart.js', () => {
            console.log("Chart.js loaded");
            setupGaVisualization();
        });
    } else {
        setupGaVisualization();
    }
}

// Setup event listeners and initialize forms
function setupGaVisualization() {
    console.log('Setting up GA visualization...');
    
    // Initialize chart
    initFitnessChart();
    
    // Load GA parameters
    loadGaParameters();
    
    // Setup form submission
    const form = document.getElementById('ga-visualization-form');
    if (form) {
        form.addEventListener('submit', runGaVisualization);
    } else {
        console.error('GA visualization form not found');
    }
    
    // Add stop button
    const stopButton = document.createElement('button');
    stopButton.id = 'stop-ga-button';
    stopButton.className = 'btn btn-danger';
    stopButton.textContent = 'Stop GA';
    stopButton.style.display = 'none';
    stopButton.addEventListener('click', stopGaVisualization);
    
    const formGroup = document.querySelector('.form-group');
    if (formGroup) {
        formGroup.appendChild(stopButton);
    } else {
        console.error('Form group not found for stop button');
    }
}

// Load current GA parameters and penalty weights
async function loadGaParameters() {
    hideMessage('ga-params-message');
    hideMessage('ga-penalties-message');
    showLoadingOverlay();
    
    try {
        // Fetch settings for relevant prefixes
        const gaSettings = await getSettingsByPrefix('ga');
        const penaltySettings = await getSettingsByPrefix('penalty');
        
        // Populate GA parameters form
        const gaParamsForm = document.getElementById('ga-params-form');
        if (gaParamsForm) {
            for (const key in gaSettings) {
                const input = gaParamsForm.elements[key];
                if (input) {
                    input.value = gaSettings[key];
                }
            }
        }
        
        // Populate penalty weights form
        const gaPenaltiesForm = document.getElementById('ga-penalties-form');
        if (gaPenaltiesForm) {
            for (const key in penaltySettings) {
                const input = gaPenaltiesForm.elements[key];
                if (input) {
                    input.value = penaltySettings[key];
                }
            }
        }
    } catch (error) {
        console.error("Error loading GA parameters:", error);
        showMessage('ga-params-message', `Error loading parameters: ${error.message}`, 'error');
    } finally {
        hideLoadingOverlay();
    }
}

// Save GA parameters
async function saveGaParameters(event) {
    event.preventDefault();
    const form = event.target;
    const messageDiv = document.getElementById('ga-params-message');
    if (!form) return;
    hideMessage('ga-params-message');
    
    const settingsToUpdate = {};
    const formData = new FormData(form);
    
    // Collect values from form inputs
    for (let [key, value] of formData.entries()) {
        if (value.trim() !== '') {
            settingsToUpdate[key] = value.trim();
        }
    }
    
    if (Object.keys(settingsToUpdate).length === 0) {
        showMessage('ga-params-message', 'No parameters were changed.', 'info');
        return;
    }
    
    try {
        showLoadingOverlay();
        await updateSettings(settingsToUpdate);
        hideLoadingOverlay();
        showMessage('ga-params-message', 'GA parameters saved successfully!', 'success', 3000);
    } catch (error) {
        console.error("Error saving GA parameters:", error);
        showMessage('ga-params-message', `Error saving parameters: ${error.message}`, 'error');
        hideLoadingOverlay();
    }
}

// Save penalty weights
async function saveGaPenalties(event) {
    event.preventDefault();
    const form = event.target;
    const messageDiv = document.getElementById('ga-penalties-message');
    if (!form) return;
    hideMessage('ga-penalties-message');
    
    const settingsToUpdate = {};
    const formData = new FormData(form);
    
    // Collect values from form inputs
    for (let [key, value] of formData.entries()) {
        if (value.trim() !== '') {
            settingsToUpdate[key] = value.trim();
        }
    }
    
    if (Object.keys(settingsToUpdate).length === 0) {
        showMessage('ga-penalties-message', 'No penalties were changed.', 'info');
        return;
    }
    
    try {
        showLoadingOverlay();
        await updateSettings(settingsToUpdate);
        hideLoadingOverlay();
        showMessage('ga-penalties-message', 'Penalty weights saved successfully!', 'success', 3000);
    } catch (error) {
        console.error("Error saving penalty weights:", error);
        showMessage('ga-penalties-message', `Error saving penalties: ${error.message}`, 'error');
        hideLoadingOverlay();
    }
}

// Set default dates (current week)
function setDefaultDates() {
    const startDateInput = document.getElementById('ga-start-date');
    const endDateInput = document.getElementById('ga-end-date');
    
    if (startDateInput && endDateInput) {
        const today = new Date();
        const monday = new Date(today);
        monday.setDate(today.getDate() - today.getDay() + 1); // Set to Monday of current week
        
        const sunday = new Date(monday);
        sunday.setDate(monday.getDate() + 6); // Set to Sunday of current week
        
        // Format dates as YYYY-MM-DD
        startDateInput.value = formatDateForInput(monday);
        endDateInput.value = formatDateForInput(sunday);
    }
}

// Format date as YYYY-MM-DD for input fields
function formatDateForInput(date) {
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');
    return `${year}-${month}-${day}`;
}

// Initialize the fitness chart
function initFitnessChart() {
    console.log('Initializing fitness chart...');
    
    // Show the chart container
    const chartContainer = document.getElementById('ga-chart-container');
    if (chartContainer) {
        chartContainer.style.display = 'block';
        // Ensure container has a defined height
        chartContainer.style.height = '400px';
        chartContainer.style.width = '100%';
    } else {
        console.error('Chart container not found');
        return;
    }
    
    // Hide the placeholder
    const placeholder = document.getElementById('ga-placeholder');
    if (placeholder) {
        placeholder.style.display = 'none';
    }
    
    // Get the chart canvas
    const ctx = document.getElementById('fitnessChart');
    if (!ctx) {
        console.error('Chart canvas not found');
        return;
    }
    
    // Set canvas size
    ctx.style.width = '100%';
    ctx.style.height = '100%';
    
    // Destroy existing chart if it exists
    if (window.fitnessChart) {
        window.fitnessChart.destroy();
        window.fitnessChart = null;
    }
    
    // Create new chart and assign directly to window.fitnessChart
    window.fitnessChart = new Chart(ctx, {
        type: 'line',
        data: {
            labels: [],
            datasets: [
                {
                    label: 'Best Fitness',
                    data: [],
                    borderColor: '#28a745',
                    backgroundColor: '#28a745',
                    fill: false,
                    tension: 0.1,
                    borderWidth: 2,
                    pointRadius: 3
                },
                {
                    label: 'Average Fitness',
                    data: [],
                    borderColor: '#007bff',
                    backgroundColor: '#007bff',
                    fill: false,
                    tension: 0.1,
                    borderWidth: 2,
                    pointRadius: 3
                },
                {
                    label: 'Worst Fitness',
                    data: [],
                    borderColor: '#dc3545',
                    backgroundColor: '#dc3545',
                    fill: false,
                    tension: 0.1,
                    borderWidth: 2,
                    pointRadius: 3
                }
            ]
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            animation: {
                duration: 0
            },
            scales: {
                y: {
                    beginAtZero: true,
                    title: {
                        display: true,
                        text: 'Fitness Value'
                    },
                    grid: {
                        display: true
                    }
                },
                x: {
                    title: {
                        display: true,
                        text: 'Generation'
                    },
                    grid: {
                        display: true
                    }
                }
            },
            plugins: {
                legend: {
                    display: true,
                    position: 'top'
                }
            }
        }
    });
    console.log('Fitness chart initialized successfully');
}

function updateChart(fitnessData) {
    console.log('Updating chart with data:', fitnessData);
    if (!window.fitnessChart) {
        console.error('Chart not initialized');
        return;
    }

    const { bestFitness, averageFitness, worstFitness } = fitnessData;
    
    // Update chart data
    window.fitnessChart.data.labels.push(window.fitnessChart.data.labels.length + 1);
    window.fitnessChart.data.datasets[0].data.push(bestFitness);
    window.fitnessChart.data.datasets[1].data.push(averageFitness);
    window.fitnessChart.data.datasets[2].data.push(worstFitness);
    
    // Update chart
    window.fitnessChart.update();
    console.log('Chart updated successfully');
}

let currentVisualizationId = null;

async function stopGaVisualization() {
    if (!currentVisualizationId) {
        console.log('No active visualization to stop');
        return;
    }
    
    try {
        console.log('Stopping GA visualization:', currentVisualizationId);
        const response = await fetch(`${API_BASE_URL}/ga/stop?visualizationId=${currentVisualizationId}`, {
            method: 'POST',
            headers: {
                'Authorization': `Bearer ${getToken()}`
            }
        });
        
        if (response.ok) {
            console.log('GA visualization stopped successfully');
            currentVisualizationId = null;
            document.getElementById('stop-ga-button').style.display = 'none';
            document.getElementById('run-ga-button').disabled = false;
        } else {
            console.error('Failed to stop GA visualization');
        }
    } catch (error) {
        console.error('Error stopping GA visualization:', error);
    }
}

let updateCounter = 0;
const UPDATE_BATCH_SIZE = 10; // Update every 10 generations
let pendingUpdates = [];

function updateVisualization(data) {
    console.log('Updating visualization with data:', data);
    
    // Add data to pending updates
    pendingUpdates.push(data);
    updateCounter++;
    
    // Only process updates in batches
    if (updateCounter % UPDATE_BATCH_SIZE === 0 || data.completed) {
        processPendingUpdates();
    }
}

function processPendingUpdates() {
    if (pendingUpdates.length === 0) return;
    
    // Get the latest data point
    const latestData = pendingUpdates[pendingUpdates.length - 1];
    
    // Debug logging
    console.log('Processing updates:', pendingUpdates.length);
    console.log('Latest data:', latestData);
    
    // Show progress container
    const progressContainer = document.getElementById('ga-progress-container');
    if (progressContainer) {
        progressContainer.style.display = 'block';
    }

    // Update generation progress
    const generationProgress = document.getElementById('generation-progress');
    if (generationProgress) {
        const genProgress = (latestData.currentGeneration / latestData.totalGenerations) * 100;
        generationProgress.style.width = `${genProgress}%`;
    }

    // Update fitness progress
    const fitnessProgress = document.getElementById('fitness-progress');
    if (fitnessProgress) {
        const currentFitness = latestData.fitnessData?.currentFitness || 0;
        const bestFitness = latestData.fitnessData?.bestFitness || 1;
        const fitProgress = (currentFitness / bestFitness) * 100;
        fitnessProgress.style.width = `${fitProgress}%`;
    }

    // Update status message
    const statusMessage = document.getElementById('ga-status');
    if (statusMessage) {
        statusMessage.textContent = `Generation: ${latestData.currentGeneration}/${latestData.totalGenerations}`;
    }

    // Update stats container
    const statsContainer = document.getElementById('ga-stats-container');
    if (statsContainer) {
        statsContainer.style.display = 'grid';
    }

    // Update individual stat cards
    const bestFitnessElement = document.getElementById('ga-best-fitness');
    const avgFitnessElement = document.getElementById('ga-avg-fitness');
    const worstFitnessElement = document.getElementById('ga-worst-fitness');

    if (bestFitnessElement) {
        bestFitnessElement.textContent = (latestData.fitnessData?.bestFitness || 0).toFixed(2);
        bestFitnessElement.parentElement.style.display = 'block';
    }
    if (avgFitnessElement) {
        avgFitnessElement.textContent = (latestData.fitnessData?.averageFitness || 0).toFixed(2);
        avgFitnessElement.parentElement.style.display = 'block';
    }
    if (worstFitnessElement) {
        worstFitnessElement.textContent = (latestData.fitnessData?.worstFitness || 0).toFixed(2);
        worstFitnessElement.parentElement.style.display = 'block';
    }

    // Update chart with all pending data points
    const chartContainer = document.getElementById('ga-chart-container');
    if (chartContainer) {
        chartContainer.style.display = 'block';
    }

    if (window.fitnessChart && window.fitnessChart.data && latestData.fitnessData) {
        console.log('Updating chart with data');
        console.log('Current chart data:', window.fitnessChart.data);
        
        // Add all pending data points to the chart
        pendingUpdates.forEach(update => {
            // Check if fitnessData exists and has the expected properties
            if (update.fitnessData && update.fitnessData.hasOwnProperty('best') && update.fitnessData.hasOwnProperty('average') && update.fitnessData.hasOwnProperty('worst')) {
                window.fitnessChart.data.labels.push(update.currentGeneration);
                // Use correct property names from backend: best, average, worst
                window.fitnessChart.data.datasets[0].data.push(update.fitnessData.best);
                window.fitnessChart.data.datasets[1].data.push(update.fitnessData.average);
                window.fitnessChart.data.datasets[2].data.push(update.fitnessData.worst);
            } else {
                console.warn('Skipping update due to missing or incomplete fitnessData:', update);
            }
        });
        
        // Update chart once with all new data
        window.fitnessChart.update();
        console.log('Chart updated with new data:', window.fitnessChart.data);
        
        // Clear pending updates
        pendingUpdates = [];
    } else {
        console.error('Cannot update chart:', {
            chart: window.fitnessChart,
            chartData: window.fitnessChart?.data,
            latestData: latestData
        });
    }
}

async function runGaVisualization(event) {
    event.preventDefault();
    console.log('Running GA visualization...');
    
    // Use correct IDs for date inputs
    const startDate = document.getElementById('ga-start-date').value;
    const endDate = document.getElementById('ga-end-date').value;
    
    if (!startDate || !endDate) {
        alert('Please select both start and end dates');
        return;
    }
    
    const submitButton = document.getElementById('run-ga-button');
    const stopButton = document.getElementById('stop-ga-button');
    
    if (submitButton) {
        submitButton.disabled = true;
    }
    if (stopButton) {
        stopButton.style.display = 'inline-block';
    }
    
    // Ensure the chart is initialized before starting
    if (!window.fitnessChart) {
         console.log('Chart not ready, initializing...');
         initFitnessChart();
         await new Promise(resolve => setTimeout(resolve, 100)); 
         if (!window.fitnessChart) {
            console.error('Chart failed to initialize. Aborting visualization run.');
             if (submitButton) submitButton.disabled = false;
             if (stopButton) stopButton.style.display = 'none';
             showMessage('ga-run-message', 'Error: Chart failed to initialize.', 'error');
             return;
         }
    }

    // Reset chart before starting a new run
    if (window.fitnessChart && window.fitnessChart.data) { 
        window.fitnessChart.data.labels = [];
        window.fitnessChart.data.datasets.forEach((dataset) => {
            dataset.data = [];
        });
        window.fitnessChart.update();
    } else {
        console.warn('Could not reset chart before run. Chart:', window.fitnessChart, 'Chart Data:', window.fitnessChart?.data);
        // Attempt to re-initialize if reset failed
        initFitnessChart(); 
        await new Promise(resolve => setTimeout(resolve, 50)); // Short delay
    }
    
    // Show progress and stats containers
    const progressContainer = document.getElementById('ga-progress-container');
    const statsContainer = document.getElementById('ga-stats-container');
    if(progressContainer) progressContainer.style.display = 'block';
    if(statsContainer) statsContainer.style.display = 'grid'; // Or 'flex' depending on your CSS

    hideMessage('ga-run-message');

    try {
        console.log(`Starting GA visualization for dates: ${startDate} to ${endDate}`);
        // Pass the correct updateVisualization function reference
        const response = await startGaVisualization(startDate, endDate, updateVisualization); 
        currentVisualizationId = response.visualizationId;
        console.log('GA visualization started with ID:', currentVisualizationId);
        showMessage('ga-run-message', `GA Process Started (ID: ${currentVisualizationId})...`, 'info');

    } catch (error) {
        console.error('Error running GA visualization:', error);
        showMessage('ga-run-message', `Error starting GA: ${error.message}`, 'error');
        if (submitButton) {
            submitButton.disabled = false;
        }
        if (stopButton) {
            stopButton.style.display = 'none';
        }
         // Hide progress indicators on error
        if(progressContainer) progressContainer.style.display = 'none';
        if(statsContainer) statsContainer.style.display = 'none';
    }
}

// Display the GA schedule
function displayGaSchedule(schedule) {
    const scheduleTable = document.getElementById('ga-schedule-table');
    if (!scheduleTable || !schedule || !schedule.shifts || schedule.shifts.length === 0) {
        return;
    }
    
    // Create table
    const table = document.createElement('table');
    table.className = 'min-w-full divide-y divide-gray-200';
    
    // Create table header
    const thead = document.createElement('thead');
    thead.className = 'bg-gray-50';
    
    const headerRow = document.createElement('tr');
    
    // Add header cells
    const headers = ['Date', 'Day', 'Start Time', 'End Time', 'Task', 'Employee', 'Fitness Impact'];
    headers.forEach(headerText => {
        const th = document.createElement('th');
        th.className = 'px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider';
        th.textContent = headerText;
        headerRow.appendChild(th);
    });
    
    thead.appendChild(headerRow);
    table.appendChild(thead);
    
    // Create table body
    const tbody = document.createElement('tbody');
    tbody.className = 'bg-white divide-y divide-gray-200';
    
    // Group shifts by date
    const shiftsByDate = {};
    schedule.shifts.forEach(shift => {
        const date = shift.date;
        if (!shiftsByDate[date]) {
            shiftsByDate[date] = [];
        }
        shiftsByDate[date].push(shift);
    });
    
    // Sort dates
    const sortedDates = Object.keys(shiftsByDate).sort();
    
    // Add rows for each shift
    sortedDates.forEach(date => {
        const shifts = shiftsByDate[date];
        shifts.sort((a, b) => a.startTime.localeCompare(b.startTime));
        
        shifts.forEach(shift => {
            const row = document.createElement('tr');
            
            // Format date
            const dateObj = new Date(date);
            const dayOfWeek = ['Sun', 'Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat'][dateObj.getDay()];
            
            // Add cells
            const cells = [
                date,
                dayOfWeek,
                shift.startTime,
                shift.endTime,
                shift.taskName || 'Unassigned',
                shift.employeeName || 'Unassigned',
                shift.fitnessImpact ? shift.fitnessImpact.toFixed(2) : 'N/A'
            ];
            
            cells.forEach(cellText => {
                const td = document.createElement('td');
                td.className = 'px-6 py-4 whitespace-nowrap text-sm text-gray-500';
                td.textContent = cellText;
                row.appendChild(td);
            });
            
            tbody.appendChild(row);
        });
    });
    
    table.appendChild(tbody);
    
    // Clear and append table
    scheduleTable.innerHTML = '';
    scheduleTable.appendChild(table);
}

// Load script dynamically
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

// Initialize when the page loads - REMOVED as initialization is now triggered by dashboard.js
// document.addEventListener('DOMContentLoaded', () => {
//     // Check if we're on the GA visualization page
//     const gaVisualizationSection = document.getElementById('ga-visualization');
//     if (gaVisualizationSection && !gaVisualizationSection.classList.contains('hidden')) {
//         initGaVisualization();
//     }
// }); 