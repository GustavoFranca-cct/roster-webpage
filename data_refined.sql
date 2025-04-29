-- Sample Employees
-- Note: Availability format is Day_Start(HHmm)_End(HHmm), comma-separated
-- Note: Preferences format is Key:Value;Key:Value, semicolon-separated
CREATE TABLE employees (
    id INT PRIMARY KEY,
    availability VARCHAR(255),
    consecutive_day_penalty_weight INT,
    contract_hours INT,
    isactive BOOLEAN,
    max_consecutive_days INT,
    max_total_hours INT,
    max_weekends INT,
    min_consecutive_days INT,
    min_total_hours INT,
    name VARCHAR(255),
    preferences VARCHAR(255),
    total_hours_penalty_weight INT,
    weekend_penalty_weight INT
);
INSERT INTO employees (id, availability, consecutive_day_penalty_weight, contract_hours, isactive, max_consecutive_days, max_total_hours, max_weekends, min_consecutive_days, min_total_hours, name, preferences, total_hours_penalty_weight, weekend_penalty_weight) VALUES (1, '[0,1,1,1,1,1,0]', 3, 40, true, 50, 30, 5, 2, 2, 'John Smith', 'Morning shifts', 2, 1);
INSERT INTO employees (id, availability, consecutive_day_penalty_weight, contract_hours, isactive, max_consecutive_days, max_total_hours, max_weekends, min_consecutive_days, min_total_hours, name, preferences, total_hours_penalty_weight, weekend_penalty_weight) VALUES (2, '[1,1,1,1,1,0,0]', 2, 35, true, 40, 25, 4, 3, 1, 'Emily Johnson', 'Evening shifts', 1, 2);
INSERT INTO employees (id, availability, consecutive_day_penalty_weight, contract_hours, isactive, max_consecutive_days, max_total_hours, max_weekends, min_consecutive_days, min_total_hours, name, preferences, total_hours_penalty_weight, weekend_penalty_weight) VALUES (3, '[1,1,1,1,1,1,1]', 4, 45, true, 60, 35, 6, 2, 3, 'Michael Williams', 'No night shifts', 3, 1);
INSERT INTO employees (id, availability, consecutive_day_penalty_weight, contract_hours, isactive, max_consecutive_days, max_total_hours, max_weekends, min_consecutive_days, min_total_hours, name, preferences, total_hours_penalty_weight, weekend_penalty_weight) VALUES (4, '[0,1,1,1,1,0,0]', 1, 30, true, 35, 20, 4, 2, 2, 'Sarah Brown', 'Weekday shifts', 1, 2);
INSERT INTO employees (id, availability, consecutive_day_penalty_weight, contract_hours, isactive, max_consecutive_days, max_total_hours, max_weekends, min_consecutive_days, min_total_hours, name, preferences, total_hours_penalty_weight, weekend_penalty_weight) VALUES (5, '[1,0,1,0,1,0,1]', 2, 20, true, 25, 15, 3, 1, 1, 'David Jones', 'Flexible hours', 2, 3);
INSERT INTO employees (id, availability, consecutive_day_penalty_weight, contract_hours, isactive, max_consecutive_days, max_total_hours, max_weekends, min_consecutive_days, min_total_hours, name, preferences, total_hours_penalty_weight, weekend_penalty_weight) VALUES (6, '[1,1,1,1,1,1,0]', 1, 40, true, 50, 30, 5, 2, 2, 'Jessica Garcia', 'Prefer weekends', 3, 1);
INSERT INTO employees (id, availability, consecutive_day_penalty_weight, contract_hours, isactive, max_consecutive_days, max_total_hours, max_weekends, min_consecutive_days, min_total_hours, name, preferences, total_hours_penalty_weight, weekend_penalty_weight) VALUES (7, '[0,0,1,1,1,1,0]', 3, 35, true, 45, 25, 4, 3, 1, 'Robert Miller', 'Afternoon shifts', 2, 2);
INSERT INTO employees (id, availability, consecutive_day_penalty_weight, contract_hours, isactive, max_consecutive_days, max_total_hours, max_weekends, min_consecutive_days, min_total_hours, name, preferences, total_hours_penalty_weight, weekend_penalty_weight) VALUES (8, '[1,1,0,0,1,1,1]', 2, 25, true, 30, 20, 3, 1, 3, 'Jennifer Davis', 'Night shifts ok', 2, 3);
INSERT INTO employees (id, availability, consecutive_day_penalty_weight, contract_hours, isactive, max_consecutive_days, max_total_hours, max_weekends, min_consecutive_days, min_total_hours, name, preferences, total_hours_penalty_weight, weekend_penalty_weight) VALUES (9, '[1,1,1,1,0,0,0]', 3, 40, true, 50, 30, 5, 2, 2, 'Thomas Wilson', 'Early mornings', 1, 1);
INSERT INTO employees (id, availability, consecutive_day_penalty_weight, contract_hours, isactive, max_consecutive_days, max_total_hours, max_weekends, min_consecutive_days, min_total_hours, name, preferences, total_hours_penalty_weight, weekend_penalty_weight) VALUES (10, '[0,1,1,1,1,1,1]', 4, 45, true, 55, 35, 6, 2, 3, 'Lisa Martinez', 'Late evenings', 3, 1);
INSERT INTO employees (id, availability, consecutive_day_penalty_weight, contract_hours, isactive, max_consecutive_days, max_total_hours, max_weekends, min_consecutive_days, min_total_hours, name, preferences, total_hours_penalty_weight, weekend_penalty_weight) VALUES (11, '[1,0,1,0,1,0,1]', 2, 30, true, 35, 20, 3, 1, 1, 'Daniel Anderson', 'Split shifts', 2, 2);
INSERT INTO employees (id, availability, consecutive_day_penalty_weight, contract_hours, isactive, max_consecutive_days, max_total_hours, max_weekends, min_consecutive_days, min_total_hours, name, preferences, total_hours_penalty_weight, weekend_penalty_weight) VALUES (12, '[1,1,1,1,1,0,0]', 5, 40, true, 50, 30, 5, 2, 2, 'Nancy Taylor', 'No weekends', 0, 1);
INSERT INTO employees (id, availability, consecutive_day_penalty_weight, contract_hours, isactive, max_consecutive_days, max_total_hours, max_weekends, min_consecutive_days, min_total_hours, name, preferences, total_hours_penalty_weight, weekend_penalty_weight) VALUES (13, '[0,0,1,1,1,1,1]', 1, 35, true, 45, 25, 4, 3, 3, 'Kevin Lee', 'Weekend warrior', 3, 2);
INSERT INTO employees (id, availability, consecutive_day_penalty_weight, contract_hours, isactive, max_consecutive_days, max_total_hours, max_weekends, min_consecutive_days, min_total_hours, name, preferences, total_hours_penalty_weight, weekend_penalty_weight) VALUES (14, '[1,1,1,0,0,0,1]', 2, 20, true, 25, 15, 3, 1, 1, 'Amanda White', 'Friday/Sunday', 2, 3);
INSERT INTO employees (id, availability, consecutive_day_penalty_weight, contract_hours, isactive, max_consecutive_days, max_total_hours, max_weekends, min_consecutive_days, min_total_hours, name, preferences, total_hours_penalty_weight, weekend_penalty_weight) VALUES (15, '[1,1,1,1,1,1,1]', 1, 48, true, 60, 40, 7, 3, 4, 'Charles Harris', 'Any shift', 4, 1);
INSERT INTO employees (id, availability, consecutive_day_penalty_weight, contract_hours, isactive, max_consecutive_days, max_total_hours, max_weekends, min_consecutive_days, min_total_hours, name, preferences, total_hours_penalty_weight, weekend_penalty_weight) VALUES (16, '[0,1,1,1,0,0,0]', 3, 32, true, 40, 25, 4, 2, 2, 'Patricia Martin', 'Midweek only', 1, 2);
INSERT INTO employees (id, availability, consecutive_day_penalty_weight, contract_hours, isactive, max_consecutive_days, max_total_hours, max_weekends, min_consecutive_days, min_total_hours, name, preferences, total_hours_penalty_weight, weekend_penalty_weight) VALUES (17, '[1,0,0,1,0,0,1]', 2, 24, true, 30, 18, 2, 1, 1, 'Christopher Clark', 'Part-time', 1, 3);
INSERT INTO employees (id, availability, consecutive_day_penalty_weight, contract_hours, isactive, max_consecutive_days, max_total_hours, max_weekends, min_consecutive_days, min_total_hours, name, preferences, total_hours_penalty_weight, weekend_penalty_weight) VALUES (18, '[1,1,1,1,1,0,0]', 3, 40, true, 50, 30, 5, 2, 2, 'Linda Rodriguez', 'No nights', 1, 1);

-- Sample Tasks
CREATE TABLE tasks (id INT PRIMARY KEY, description VARCHAR(255), minimum_coverage INT, name VARCHAR(255), optimal_coverage INT, penalty_weight INT);
INSERT INTO tasks (id, description, minimum_coverage, name, optimal_coverage, penalty_weight) VALUES (10, 'Receive deliveries and restock shelves.', 1, 'Stocking', 2, 100);
INSERT INTO tasks (id, description, minimum_coverage, name, optimal_coverage, penalty_weight) VALUES (11, 'Onboarding or skill development session.', 1, 'Training', 2, 100);
INSERT INTO tasks (id, description, minimum_coverage, name, optimal_coverage, penalty_weight) VALUES (8, 'Customer service, restocking during peak hours.', 1, 'Afternoon Task', 2, 100);
INSERT INTO tasks (id, description, minimum_coverage, name, optimal_coverage, penalty_weight) VALUES (7, 'Prepare store for opening, initial setup.', 1, 'Morning Task', 2, 100);
INSERT INTO tasks (id, description, minimum_coverage, name, optimal_coverage, penalty_weight) VALUES (9, 'Clean up, closing procedures, cash out.', 1, 'Evening Shift', 2, 100);

-- Sample Skills
CREATE TABLE skills (id INT PRIMARY KEY, name VARCHAR(255));
INSERT INTO skills (id, name) VALUES (9, 'Cash Handling');
INSERT INTO skills (id, name) VALUES (10, 'Barista');
INSERT INTO skills (id, name) VALUES (11, 'Customer Service');
INSERT INTO skills (id, name) VALUES (12, 'Shift Supervisor');
INSERT INTO skills (id, name) VALUES (13, 'Inventory Management');
INSERT INTO skills (id, name) VALUES (14, 'First Aid Certified');
INSERT INTO skills (id, name) VALUES (15, 'Keyholder');


-- Sample Employee-Skill Associations (using names for readability - adjust if using IDs)
-- Sample Task-Skill Requirements

CREATE TABLE task_required_skills (task_id INT PRIMARY KEY , skill_id INT);
INSERT INTO task_required_skills (task_id, skill_id) VALUES (7, 15);
INSERT INTO task_required_skills (task_id, skill_id) VALUES (8, 11);
INSERT INTO task_required_skills (task_id, skill_id) VALUES (9, 12);
INSERT INTO task_required_skills (task_id, skill_id) VALUES (10, 13);
INSERT INTO task_required_skills (task_id, skill_id) VALUES (8, 9);
INSERT INTO task_required_skills (task_id, skill_id) VALUES (11, 12);
INSERT INTO task_required_skills (task_id, skill_id) VALUES (7, 11);


-- Sample User (Password: 'password')
-- Use a BCrypt generator to get the hash for 'password'
CREATE TABLE users (id INT PRIMARY KEY, enabled BOOLEAN, password VARCHAR(255), username VARCHAR(255));
INSERT INTO users (id, enabled, password, username) VALUES (2, true, '$2a$12$XU5NM7TnSM8CiqmaBcD9NOp9/Pu7LBHUNH7CHvJhCin/i5XkhbuWi', 'firstUser');
INSERT INTO users (id, enabled, password, username) VALUES (1, true, '$2a$12$XU5NM7TnSM8CiqmaBcD9NOp9/Pu7LBHUNH7CHvJhCin/i5XkhbuWi', 'usertest');
