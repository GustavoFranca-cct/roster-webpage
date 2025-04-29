-- Sample Employees
-- Note: Availability format is Day_Start(HHmm)_End(HHmm), comma-separated
-- Note: Preferences format is Key:Value;Key:Value, semicolon-separated
INSERT INTO employees (name, contract_hours, availability, preferences, is_active)
VALUES
    ('Alice Manager', 40, 'Mon_0800_1600,Tue_0800_1600,Wed_0800_1600,Thu_0800_1600,Fri_0800_1600', 'Preferred:Weekdays', true),
    ('Bob Barista', 30, 'Mon_0700_1500,Wed_0700_1500,Fri_0700_1500,Sat_0900_1700', 'Unpreferred:Tue_Any;Preferred:Sat_Morning', true),
    ('Charlie Cashier', 20, 'Tue_1200_2000,Thu_1200_2000,Sat_1200_2000', 'Preferred:Afternoon', true),
    ('Diana Intern', 15, 'Mon_1000_1400,Wed_1000_1400', '', true),
    ('Evan ExEmployee', 40, 'Mon_0900_1700', '', false); -- Inactive employee

-- Sample Tasks
INSERT INTO tasks (name, description)
VALUES
    ('Opening Shift', 'Prepare store for opening, initial setup.'),
    ('Mid Shift', 'Customer service, restocking during peak hours.'),
    ('Closing Shift', 'Clean up, closing procedures, cash out.'),
    ('Stocking', 'Receive deliveries and restock shelves.'),
    ('Training', 'Onboarding or skill development session.');

-- Sample Skills
INSERT INTO skills (name) VALUES
    ('Cash Handling'),
    ('Barista'),
    ('Customer Service'),
    ('Shift Supervisor'),
    ('Inventory Management'),
    ('First Aid Certified'),
    ('Keyholder');

-- Sample Employee-Skill Associations (using names for readability - adjust if using IDs)
-- Assuming Alice is Supervisor, Keyholder, etc.
INSERT INTO employee_skills (employee_id, skill_id)
SELECT e.id, s.id FROM employees e, skills s WHERE e.name = 'Alice Manager' AND s.name IN ('Shift Supervisor', 'Keyholder', 'Customer Service');

-- Assuming Bob is Barista, Customer Service
INSERT INTO employee_skills (employee_id, skill_id)
SELECT e.id, s.id FROM employees e, skills s WHERE e.name = 'Bob Barista' AND s.name IN ('Barista', 'Customer Service');

-- Assuming Charlie is Cash Handling, Customer Service
INSERT INTO employee_skills (employee_id, skill_id)
SELECT e.id, s.id FROM employees e, skills s WHERE e.name = 'Charlie Cashier' AND s.name IN ('Cash Handling', 'Customer Service');

-- Sample Task-Skill Requirements
-- Assuming Opening Shift requires Keyholder
INSERT INTO task_required_skills (task_id, skill_id)
SELECT t.id, s.id FROM tasks t, skills s WHERE t.name = 'Opening Shift' AND s.name = 'Keyholder';

-- Assuming Closing Shift requires Keyholder and Shift Supervisor
INSERT INTO task_required_skills (task_id, skill_id)
SELECT t.id, s.id FROM tasks t, skills s WHERE t.name = 'Closing Shift' AND s.name IN ('Keyholder', 'Shift Supervisor');

-- Assuming Mid Shift requires Customer Service
INSERT INTO task_required_skills (task_id, skill_id)
SELECT t.id, s.id FROM tasks t, skills s WHERE t.name = 'Mid Shift' AND s.name = 'Customer Service';

-- Sample User (Password: 'password')
-- Use a BCrypt generator to get the hash for 'password'
-- Example Hash (yours might differ slightly): $2a$10$dXJ3SWdSgpRq8a./IsT4.OgdsjNcsQpXlf7jNeCvJRTf.6DckdBdq
INSERT INTO users (username, password, enabled) VALUES
('firstUser', '$2a$10$dXJ3SWdSgpRq8a./IsT4.OgdsjNcsQpXlf7jNeCvJRTf.6DckdBdq', true);