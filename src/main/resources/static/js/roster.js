// Global variables for roster view (might need adjustment if loaded dynamically)
let currentRosterContainer = null; // Store the container element
let scheduleGridContainer;
let employeeFilter;
let taskFilter;
let dayFilter;
let resetFiltersButton;
let contextMenu;
let closeContextMenuButton;
let filterContainer;
let scheduleMessageArea;
let generateButton;
let generateStartDateInput;
let generateEndDateInput;
let businessStartTimeInput;
let businessEndTimeInput;
let generationMessageArea;
let loadScheduleButton;
let deleteScheduleButton;

// --- Utility Functions (showMessage, hideMessage) ---
function showMessage(elementId, message, type = 'info', timeout = null) {
    const messageArea = document.getElementById(elementId);
    if (!messageArea) {
        console.warn(`Message area with ID ${elementId} not found.`);
        return;
    }
    let bgColor, textColor;
    switch (type) {
        case 'success': bgColor = 'bg-green-100'; textColor = 'text-green-700'; break;
        case 'error': bgColor = 'bg-red-100'; textColor = 'text-red-700'; break;
        default: bgColor = 'bg-blue-100'; textColor = 'text-blue-700'; break; // info
    }
    messageArea.className = `p-2 rounded-md ${bgColor} ${textColor} text-sm`;
    messageArea.textContent = message;
    messageArea.style.display = 'block';
    messageArea.style.visibility = 'visible';

    if (timeout) {
        setTimeout(() => hideMessage(elementId), timeout);
    }
}

function hideMessage(elementId) {
    const messageArea = document.getElementById(elementId);
    if (messageArea) {
        messageArea.style.visibility = 'hidden';
        messageArea.style.display = 'none';
        messageArea.textContent = '';
    }
}

// --- Date Formatting Helper ---
function formatDate(date) {
    // Basic YYYY-MM-DD format
    return date.toISOString().split('T')[0];
}
function formatTime(timeString) {
    // Assuming timeString is like "HH:mm:ss", return "HH:mm"
    if (!timeString) return '';
    return timeString.substring(0, 5);
}
function getDayAbbreviation(dateString) {
    const date = new Date(dateString + 'T00:00:00'); // Avoid timezone issues
    return date.toLocaleDateString('en-US', { weekday: 'short' }); // e.g., Mon, Tue
}

// --- Populate Filters (Updated for Tasks) ---
async function populateFilters() {
    if (!employeeFilter || !taskFilter) {
        console.error("Filter elements not found");
        return;
    }
    employeeFilter.innerHTML = '<option value="all">All Employees</option>';
    taskFilter.innerHTML = '<option value="all">All Tasks</option>';
    taskFilter.disabled = false; // Re-enable in case it was disabled

    try {
        const employees = await getEmployees(); // Assumes getEmployees is globally available or imported
        if (employees && Array.isArray(employees)) {
            employees.forEach(emp => {
                const option = document.createElement('option');
                option.value = emp.id;
                option.textContent = emp.name || `Employee ${emp.id}`;
                employeeFilter.appendChild(option);
            });
        } else {
            console.warn('No employees found or invalid format.');
        }

        const tasks = await getTasks(); // Assumes getTasks is globally available or imported
        if (tasks && Array.isArray(tasks)) {
            tasks.forEach(task => {
                const option = document.createElement('option');
                option.value = task.id;
                option.textContent = task.name || `Task ${task.id}`;
                taskFilter.appendChild(option);
            });
        } else {
            console.warn('No tasks found or invalid format.');
            taskFilter.disabled = true;
        }

    } catch (error) {
        console.error("Error populating filters:", error);
        showMessage('schedule-message', 'Error loading filter data.', 'error');
    }
}

// --- Display Schedule Grid ---
async function displaySchedule(scheduleStartDate, scheduleEndDate, explanations = null) {
    if (!scheduleGridContainer) {
        console.error("Schedule grid container not found");
        return;
    }

    // Clear existing rows and headers except the first employee name header
    const headers = scheduleGridContainer.querySelectorAll('.schedule-header:not(.employee-name)');
    headers.forEach(h => h.remove());
    const existingRows = scheduleGridContainer.querySelectorAll('.employee-row');
    existingRows.forEach(row => row.remove());
    // Ensure the static header exists
    if (!scheduleGridContainer.querySelector('.employee-name')) {
       const empHeader = document.createElement('div');
       empHeader.className = 'schedule-header employee-name';
       empHeader.style.position = 'sticky';
       empHeader.style.left = '0';
       empHeader.style.top = '0';
       empHeader.style.zIndex = '15';
       empHeader.textContent = 'Employee';
       scheduleGridContainer.insertBefore(empHeader, scheduleGridContainer.firstChild);
    }


    if (!scheduleStartDate || !scheduleEndDate) {
        showMessage('schedule-message', 'Please provide a valid date range to display the schedule.', 'error');
        return;
    }

    try {
        showMessage('schedule-message', 'Loading schedule...', 'info');
        const shiftsOrResult = await getScheduleApi(scheduleStartDate, scheduleEndDate); // Assumes getScheduleApi is globally available
        hideMessage('schedule-message');

        let shifts = shiftsOrResult;
        let backendExplanations = explanations;

        // If backend returns a wrapper (in case getScheduleApi is updated in future)
        if (shiftsOrResult && shiftsOrResult.shifts && Array.isArray(shiftsOrResult.shifts)) {
            shifts = shiftsOrResult.shifts;
            backendExplanations = shiftsOrResult.explanations || explanations;
        }

        if ((!shifts || !Array.isArray(shifts) || shifts.length === 0) && backendExplanations && backendExplanations.length > 0) {
            showMessage('schedule-message', backendExplanations.join('\n'), 'warning', 10000);
        } else if (!shifts || !Array.isArray(shifts)) {
            showMessage('schedule-message', 'Invalid schedule data received.', 'error');
            return;
        }

        // --- Process Data --- 
        const employeesData = {};
        const datesInRange = new Set();
        shifts.forEach(shift => {
            if (!shift.employeeId || !shift.shiftDate) return; // Skip invalid shifts

            datesInRange.add(shift.shiftDate);

            if (!employeesData[shift.employeeId]) {
                employeesData[shift.employeeId] = {
                    name: shift.employeeName || `Employee ${shift.employeeId}`,
                    shifts: {}
                };
            }
            if (!employeesData[shift.employeeId].shifts[shift.shiftDate]) {
                employeesData[shift.employeeId].shifts[shift.shiftDate] = [];
            }
            employeesData[shift.employeeId].shifts[shift.shiftDate].push(shift);
        });

        // --- Generate Date Headers ---
        let sortedDates = Array.from(datesInRange).sort();
        // If no shifts were found, create headers for the requested range
        if (sortedDates.length === 0) {
             let currentDate = new Date(scheduleStartDate + 'T00:00:00');
             const endDateObj = new Date(scheduleEndDate + 'T00:00:00');
             while (currentDate <= endDateObj) {
                 sortedDates.push(formatDate(currentDate));
                 currentDate.setDate(currentDate.getDate() + 1);
             }
         }

         scheduleGridContainer.style.gridTemplateColumns = `minmax(150px, 1fr) repeat(${sortedDates.length}, minmax(120px, 1fr))`;

         if (dayFilter) {
             dayFilter.innerHTML = '<option value="all">All Days</option>'; // Reset day filter
             sortedDates.forEach(dateStr => {
                 const dayOption = document.createElement('option');
                 dayOption.value = dateStr;
                 dayOption.textContent = `${getDayAbbreviation(dateStr)} - ${dateStr}`;
                 dayFilter.appendChild(dayOption);
             });
         }


         sortedDates.forEach(dateStr => {
             const header = document.createElement('div');
             header.classList.add('schedule-header');
             header.style.position = 'sticky';
             header.style.top = '0';
             header.style.zIndex = '5';
             header.textContent = `${getDayAbbreviation(dateStr)} (${dateStr.substring(5)})`; // e.g., Mon (01-15)
             header.dataset.date = dateStr; // Store full date
             scheduleGridContainer.appendChild(header);
         });


         // --- Build Employee Rows ---
        if (Object.keys(employeesData).length === 0) {
             const row = document.createElement('div');
             row.classList.add('employee-row'); // Use this class for identification
             row.style.gridColumn = `span ${sortedDates.length + 1}`; // Span all columns
             row.classList.add('text-center', 'p-4', 'text-gray-500', 'bg-white');
             if (backendExplanations && backendExplanations.length > 0) {
                row.textContent = backendExplanations.join('\n');
                row.classList.add('text-yellow-700', 'bg-yellow-50');
            } else {
                row.textContent = 'No employees scheduled for this period.';
            }
             scheduleGridContainer.appendChild(row);
         } else {
             Object.entries(employeesData).forEach(([employeeId, data]) => {
                 const employeeRow = document.createElement('div');
                 employeeRow.classList.add('employee-row');
                 employeeRow.dataset.employeeId = employeeId;
                 employeeRow.style.display = 'contents';

                 const nameCell = document.createElement('div');
                 nameCell.classList.add('employee-name');
                 nameCell.textContent = data.name;
                 employeeRow.appendChild(nameCell);

                 sortedDates.forEach(dateStr => {
                     const cell = document.createElement('div');
                     cell.classList.add('schedule-cell');
                     cell.dataset.date = dateStr;

                     const shiftsForDay = data.shifts[dateStr] || [];
                     if (shiftsForDay.length > 0) {
                         shiftsForDay.forEach(shift => {
                              const shiftBlock = document.createElement('div');
                              shiftBlock.classList.add('shift-block');
                              shiftBlock.innerHTML = `
                                 <span class="font-semibold">${formatTime(shift.startTime)} - ${formatTime(shift.endTime)}</span>
                                 <span class="text-xs italic">${shift.taskName || ''}</span>
                                 `;
                              // Add data for context menu
                              shiftBlock.dataset.employee = data.name;
                              shiftBlock.dataset.date = dateStr;
                              shiftBlock.dataset.startTime = formatTime(shift.startTime);
                              shiftBlock.dataset.endTime = formatTime(shift.endTime);
                              shiftBlock.dataset.shiftId = shift.id;
                              shiftBlock.dataset.taskId = shift.taskId || '';
                              shiftBlock.dataset.taskName = shift.taskName || 'No Task';

                              shiftBlock.addEventListener('contextmenu', showContextMenu);
                              cell.appendChild(shiftBlock);
                         });
                     } else {
                         cell.innerHTML = '&nbsp;';
                     }
                     employeeRow.appendChild(cell);
                 });
                 scheduleGridContainer.appendChild(employeeRow);
             });
         }

        applyFilters();

    } catch (error) {
        console.error("Error displaying schedule:", error);
        showMessage('schedule-message', `Error loading schedule: ${error.message}`, 'error');
    }
}

// --- Context Menu Logic (Updated for Task) ---
function showContextMenu(event) {
    event.preventDefault();
    if (!contextMenu) return;
    const shiftBlock = event.currentTarget;

    // Populate context menu with ShiftDTO info
    document.getElementById('context-menu-title').textContent = 'Shift Details';
    document.getElementById('context-menu-employee').textContent = shiftBlock.dataset.employee;
    document.getElementById('context-menu-day').textContent = shiftBlock.dataset.date;
    document.getElementById('context-menu-shift').textContent = `${shiftBlock.dataset.startTime} - ${shiftBlock.dataset.endTime}`;
    document.getElementById('context-menu-task').textContent = shiftBlock.dataset.taskName || 'N/A';
    document.getElementById('context-menu-details').textContent = `Shift ID: ${shiftBlock.dataset.shiftId || 'N/A'}, Task ID: ${shiftBlock.dataset.taskId || 'N/A'}`;

    // Positioning logic remains the same
    const menuWidth = contextMenu.offsetWidth;
    const menuHeight = contextMenu.offsetHeight;
    const windowWidth = window.innerWidth;
    const windowHeight = window.innerHeight;
    let top = event.clientY;
    let left = event.clientX;
    if (top + menuHeight > windowHeight) { top = windowHeight - menuHeight - 10; }
    if (left + menuWidth > windowWidth) { left = windowWidth - menuWidth - 10; }
    contextMenu.style.top = `${top}px`;
    contextMenu.style.left = `${left}px`;
    contextMenu.style.display = 'block';

    // Add event listener for delete button
    const deleteButton = contextMenu.querySelector('#context-menu-delete');
    if (deleteButton) {
        deleteButton.dataset.shiftId = shiftBlock.dataset.shiftId;
        deleteButton.onclick = handleDeleteShiftClick;
    }
}

function hideContextMenu() {
    if (contextMenu) {
        contextMenu.style.display = 'none';
    }
}

// --- Filtering Logic (Updated for Tasks) ---
function applyFilters() {
    if (!employeeFilter || !dayFilter || !taskFilter || !scheduleGridContainer) {
        console.warn("Filter elements or schedule grid not found, cannot apply filters.");
        return;
    }

    const selectedEmployee = employeeFilter.value;
    const selectedDate = dayFilter.value;
    const selectedTask = taskFilter.value;

    const rows = scheduleGridContainer.querySelectorAll('.employee-row');
    const headers = scheduleGridContainer.querySelectorAll('.schedule-header');
    const cells = scheduleGridContainer.querySelectorAll('.schedule-cell');

    // Reset visibility first
    rows.forEach(row => { if (row.dataset.employeeId) row.style.display = 'contents'; });
    headers.forEach(header => header.style.display = 'flex');
    cells.forEach(cell => cell.style.display = 'flex');
    scheduleGridContainer.querySelectorAll('.shift-block').forEach(block => block.style.display = 'flex');

    // Task Filtering (Shift block level - Now using taskId)
    if (selectedTask !== 'all') {
        scheduleGridContainer.querySelectorAll('.shift-block').forEach(block => {
            if (block.dataset.taskId !== selectedTask) {
                block.style.display = 'none';
            }
        });
    }

    // Employee Filtering (Row level) & Task Row Visibility
    rows.forEach(row => {
         if (!row.dataset.employeeId) return;
         let rowVisible = true;
         // Check Employee Match
         if (selectedEmployee !== 'all' && row.dataset.employeeId !== selectedEmployee) {
             rowVisible = false;
         }
         // Check if row has any visible shifts after task filtering
         if (rowVisible && selectedTask !== 'all') {
            const visibleBlocks = Array.from(row.querySelectorAll('.shift-block')).filter(b => b.style.display !== 'none');
            if (visibleBlocks.length === 0) {
                rowVisible = false;
            }
         }
         row.style.display = rowVisible ? 'contents' : 'none';
     });

    // Day Filtering (Column level)
    headers.forEach(header => {
         if (header.classList.contains('employee-name')) return;
         const matchesDate = (selectedDate === 'all' || header.dataset.date === selectedDate);
         header.style.display = matchesDate ? 'flex' : 'none';
     });
    cells.forEach(cell => {
         const matchesDate = (selectedDate === 'all' || cell.dataset.date === selectedDate);
         cell.style.display = matchesDate ? 'flex' : 'none';
     });

     // Adjust grid columns based on visible days
     const visibleDayHeaders = Array.from(scheduleGridContainer.querySelectorAll('.schedule-header:not(.employee-name)'))
                                .filter(h => h.style.display !== 'none');
     const visibleDaysCount = visibleDayHeaders.length;
     scheduleGridContainer.style.gridTemplateColumns = `minmax(150px, 1fr) repeat(${visibleDaysCount}, minmax(120px, 1fr))`;

    hideContextMenu();
}

function resetFilters() {
    if (employeeFilter) employeeFilter.value = 'all';
    if (taskFilter) taskFilter.value = 'all';
    if (dayFilter) dayFilter.value = 'all';
    applyFilters();
}

// --- Schedule Generation Logic ---
async function handleGenerateClick() {
    const startDate = generateStartDateInput.value;
    const endDate = generateEndDateInput.value;
    const businessStartTime = businessStartTimeInput.value; // HH:MM format
    const businessEndTime = businessEndTimeInput.value;   // HH:MM format

    // Validation
    if (!startDate || !endDate || !businessStartTime || !businessEndTime) {
        showMessage('generation-message', 'Please fill in all generation fields.', 'error');
        return;
    }
    if (endDate < startDate) {
         showMessage('generation-message', 'End date cannot be before start date.', 'error');
         return;
     }
     // Basic time validation (could be more robust)
     if (businessEndTime <= businessStartTime) {
         showMessage('generation-message', 'Business end time must be after start time.', 'error');
         return;
     }

    const requestData = {
        startDate: startDate,
        endDate: endDate,
        businessStartTime: businessStartTime + ":00", // Add seconds if backend expects it
        businessEndTime: businessEndTime + ":00"     // Add seconds if backend expects it
    };

    try {
        showMessage('generation-message', 'Generating schedule... Please wait.', 'info');
        generateButton.disabled = true;

        const result = await generateScheduleApi(
            requestData.startDate,
            requestData.endDate,
            requestData.businessStartTime,
            requestData.businessEndTime
        );

        // If explanations are present and no shifts, show explanations
        if (result && Array.isArray(result.explanations) && result.explanations.length > 0 && (!result.shifts || result.shifts.length === 0)) {
            showMessage('generation-message', result.explanations.join('\n'), 'warning', 10000);
        } else {
            showMessage('generation-message', 'Schedule generated successfully! Displaying results...', 'success', 3000);
        }

        // Display the newly generated schedule for the same period, pass explanations if present
        await displaySchedule(startDate, endDate, result && result.explanations);

    } catch (error) {
        console.error("Error generating schedule:", error);
        showMessage('generation-message', `Error generating schedule: ${error.message || 'Unknown error'}`, 'error');
    } finally {
         if (generateButton) generateButton.disabled = false;
    }
}

// --- Initialization Function ---
async function initializeRosterView(containerElement) {
    console.log("Initializing Roster View...");
    if (!containerElement) {
        console.error("Roster container element not provided for initialization.");
        return;
    }
    currentRosterContainer = containerElement; // Store the container

    // Find elements within the container
    scheduleGridContainer = containerElement.querySelector('#schedule-grid-container');
    employeeFilter = containerElement.querySelector('#employee-filter');
    taskFilter = containerElement.querySelector('#task-filter');
    dayFilter = containerElement.querySelector('#day-filter');
    resetFiltersButton = containerElement.querySelector('#reset-filters');
    contextMenu = containerElement.querySelector('#context-menu');
    closeContextMenuButton = containerElement.querySelector('#close-context-menu');
    filterContainer = containerElement.querySelector('#filter-container');
    generateButton = containerElement.querySelector('#generate-schedule-button');
    generateStartDateInput = containerElement.querySelector('#generate-start-date');
    generateEndDateInput = containerElement.querySelector('#generate-end-date');
    businessStartTimeInput = containerElement.querySelector('#business-start-time');
    businessEndTimeInput = containerElement.querySelector('#business-end-time');
    generationMessageArea = containerElement.querySelector('#generation-message');
    scheduleMessageArea = containerElement.querySelector('#schedule-message');

    // Find buttons
    loadScheduleButton = containerElement.querySelector('#load-schedule-button');
    deleteScheduleButton = containerElement.querySelector('#delete-schedule-button');

    // Create message areas dynamically if they don't exist in the fetched HTML
    if (!scheduleMessageArea && filterContainer) {
        const msgDiv = document.createElement('div');
        msgDiv.id = 'schedule-message'; // Assign ID for targeting
        msgDiv.className = 'mb-4';
        filterContainer.parentNode.insertBefore(msgDiv, filterContainer);
        scheduleMessageArea = msgDiv; // Update reference
    }
     if (!generationMessageArea && containerElement.querySelector('#generation-container')) {
        const genMsgDiv = document.createElement('div');
        genMsgDiv.id = 'generation-message'; // Assign ID
        genMsgDiv.className = 'mt-3 text-sm';
        containerElement.querySelector('#generation-container').appendChild(genMsgDiv);
        generationMessageArea = genMsgDiv; // Update reference
    }

    // --- Add Event Listeners (Ensure elements exist before adding) ---
    if (employeeFilter) employeeFilter.addEventListener('change', applyFilters);
    if (taskFilter) taskFilter.addEventListener('change', applyFilters);
    if (dayFilter) dayFilter.addEventListener('change', applyFilters);
    if (resetFiltersButton) resetFiltersButton.addEventListener('click', resetFilters);
    if (generateButton) generateButton.addEventListener('click', handleGenerateClick);
    if (closeContextMenuButton) closeContextMenuButton.addEventListener('click', hideContextMenu);
    if (loadScheduleButton) loadScheduleButton.addEventListener('click', handleLoadScheduleClick);
    if (deleteScheduleButton) deleteScheduleButton.addEventListener('click', handleDeleteScheduleClick);

    // Global listeners for context menu
    document.addEventListener('click', (event) => {
        if (contextMenu && !contextMenu.contains(event.target) && !event.target.closest('.shift-block')) {
            hideContextMenu();
        }
    });
    document.addEventListener('keydown', (event) => {
        if (event.key === 'Escape') hideContextMenu();
    });

    // Populate dynamic filters
    await populateFilters();

    // Load schedule for default period (e.g., current week)
    const today = new Date();
    const startOfWeek = new Date(today.setDate(today.getDate() - today.getDay() + (today.getDay() === 0 ? -6 : 1))); // Start on Monday
    const endOfWeek = new Date(startOfWeek); // Clone start date
    endOfWeek.setDate(startOfWeek.getDate() + 6); // End on Sunday

    const defaultStartDate = formatDate(startOfWeek);
    const defaultEndDate = formatDate(endOfWeek);
    
    // Set date pickers to default values (if they exist in this view)
    const viewStartDateInput = containerElement.querySelector('#generate-start-date');
    const viewEndDateInput = containerElement.querySelector('#generate-end-date');
    if(viewStartDateInput) viewStartDateInput.value = defaultStartDate;
    if(viewEndDateInput) viewEndDateInput.value = defaultEndDate;

    await displaySchedule(defaultStartDate, defaultEndDate);

    console.log("Roster view initialized for:", defaultStartDate, "to", defaultEndDate);
}

// --- Handler for Load Schedule Button ---
async function handleLoadScheduleClick() {
    if (!currentRosterContainer) {
        console.error("Roster container not initialized.");
        showMessage('schedule-message', 'Error: Roster view not properly loaded.', 'error');
        return;
    }
    const startDate = generateStartDateInput.value;
    const endDate = generateEndDateInput.value;

    // const viewStartDateInput = currentRosterContainer.querySelector('#schedule-start-date');
    // const viewEndDateInput = currentRosterContainer.querySelector('#schedule-end-date');

    // if (!viewStartDateInput || !viewEndDateInput) {
    //      showMessage('schedule-message', 'Date range inputs not found in this view.', 'error');
    //      return;
    // }
    // const startDate = viewStartDateInput.value;   //using the generateStartDateInput.value instead of viewStartDateInput.value
    // const endDate = viewEndDateInput.value;      //using the generateEndDateInput.value instead of viewEndDateInput.value

    if (!startDate || !endDate) {
         showMessage('schedule-message', 'Please select both start and end dates.', 'warning');
         return;
    }
    await displaySchedule(startDate, endDate);
}

// --- New Handler for Delete Schedule Button ---
async function handleDeleteScheduleClick() {
    if (!currentRosterContainer) {
        console.error("Roster container not initialized.");
        showMessage('schedule-message', 'Error: Roster view not properly loaded.', 'error');
        return;
    }
    // const viewStartDateInput = currentRosterContainer.querySelector('#schedule-start-date');
    // const viewEndDateInput = currentRosterContainer.querySelector('#schedule-end-date');
    const startDate = generateStartDateInput.value;
    const endDate = generateEndDateInput.value;
    // if (!startDate || !endDate) {
    //      showMessage('schedule-message', 'Date range inputs not found in this view.', 'error');
    //      return;
    // }


    if (!startDate || !endDate) {
         showMessage('schedule-message', 'Please select the date range of the schedule to delete.', 'warning');
         return;
    }

    if (!confirm(`Are you sure you want to delete all shifts between ${startDate} and ${endDate}? This cannot be undone.`)) {
        return;
    }

    try {
        showMessage('schedule-message', `Deleting shifts from ${startDate} to ${endDate}...`, 'info');
        await deleteScheduleApi(startDate, endDate); // Assumes deleteScheduleApi exists globally
        showMessage('schedule-message', 'Schedule deleted successfully.', 'success', 3000);
        // Refresh the view to show it's empty
        await displaySchedule(startDate, endDate);
    } catch (error) {
        console.error("Error deleting schedule:", error);
        showMessage('schedule-message', `Error deleting schedule: ${error.message}`, 'error');
    }
}

// --- Function to handle Delete Shift click from context menu ---
async function handleDeleteShiftClick(event) {
    const shiftId = event.currentTarget.dataset.shiftId;
    if (!shiftId) {
        console.error("Shift ID not found on delete button.");
        return;
    }

    if (!confirm(`Are you sure you want to delete this specific shift (ID: ${shiftId})?`)) {
        hideContextMenu();
        return;
    }

    hideContextMenu(); // Close menu immediately
    showMessage('schedule-message', `Deleting shift ${shiftId}...`, 'info');
    showLoadingOverlay(); // Use global overlay if available

    try {
        await deleteShiftApi(shiftId); // Call the API function
        showMessage('schedule-message', `Shift ${shiftId} deleted successfully.`, 'success', 3000);
        // Refresh the schedule view to reflect the deletion
        // Need the current start/end dates from the view inputs
        const viewStartDateInput = currentRosterContainer.querySelector('#generate-start-date'); //using the element id from the generate-start-date input
        const viewEndDateInput = currentRosterContainer.querySelector('#generate-end-date'); //using the element id from the generate-end-date input
        if (viewStartDateInput && viewEndDateInput && viewStartDateInput.value && viewEndDateInput.value) {
            await displaySchedule(viewStartDateInput.value, viewEndDateInput.value);
        } else {
             console.warn("Could not refresh schedule view after delete: date inputs not found or empty.");
             // Or maybe call initializeRosterView again?
        }
    } catch (error) {
        console.error(`Error deleting shift ${shiftId}:`, error);
        showMessage('schedule-message', `Error deleting shift: ${error.message}`, 'error');
    } finally {
        hideLoadingOverlay();
    }
}
