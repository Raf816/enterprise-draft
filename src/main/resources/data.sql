-- ============================================================
-- STAFF MANAGEMENT CONTEXT — Seed Staff
-- ============================================================

INSERT INTO staff_member (id, first_name, surname, email, department, line_manager_id, hire_date, "current_role", start_date_current_role, job_level, employment_type, employment_status)
VALUES
    ('mgr-00000-00000-00000-000000000001', 'Sarah', 'Thompson', 'sarah.thompson@company.com', 'Engineering', NULL, '2020-03-15', 'Engineering Manager', '2022-01-01', 'L6', 'FULL_TIME', 'ACTIVE'),
    ('stf-00000-00000-00000-000000000001', 'James', 'Wilson', 'james.wilson@company.com', 'Engineering', 'mgr-00000-00000-00000-000000000001', '2022-06-01', 'Software Engineer', '2022-06-01', 'L4', 'FULL_TIME', 'ACTIVE'),
    ('stf-00000-00000-00000-000000000002', 'Emily', 'Chen', 'emily.chen@company.com', 'Engineering', 'mgr-00000-00000-00000-000000000001', '2023-01-10', 'Software Engineer', '2023-01-10', 'L4', 'FULL_TIME', 'ACTIVE'),
    ('stf-00000-00000-00000-000000000003', 'David', 'Patel', 'david.patel@company.com', 'Engineering', 'mgr-00000-00000-00000-000000000001', '2021-09-01', 'Senior Engineer', '2024-04-01', 'L5', 'FULL_TIME', 'ACTIVE'),
    ('adm-00000-00000-00000-000000000001', 'Rachel', 'Morgan', 'rachel.morgan@company.com', 'HR', NULL, '2019-01-05', 'HR Administrator', '2019-01-05', 'L5', 'FULL_TIME', 'ACTIVE');

-- ============================================================
-- LEAVE MANAGEMENT CONTEXT — Seed Allowances (business year 2026-2027)
-- ============================================================

INSERT INTO leave_allowance (id, staff_member_id, manager_id, first_name, surname, department, business_year_start, business_year_end, total_entitlement, days_used, days_pending)
VALUES
    ('allow-0000-0000-0000-000000000001', 'stf-00000-00000-00000-000000000001', 'mgr-00000-00000-00000-000000000001', 'James', 'Wilson', 'Engineering', 2026, 2027, 25, 5, 3),
    ('allow-0000-0000-0000-000000000002', 'stf-00000-00000-00000-000000000002', 'mgr-00000-00000-00000-000000000001', 'Emily', 'Chen', 'Engineering', 2026, 2027, 25, 2, 0),
    ('allow-0000-0000-0000-000000000003', 'stf-00000-00000-00000-000000000003', 'mgr-00000-00000-00000-000000000001', 'David', 'Patel', 'Engineering', 2026, 2027, 28, 10, 5),
    ('allow-0000-0000-0000-000000000004', 'mgr-00000-00000-00000-000000000001', 'adm-00000-00000-00000-000000000001', 'Sarah', 'Thompson', 'Engineering', 2026, 2027, 30, 8, 0);

-- ============================================================
-- LEAVE MANAGEMENT CONTEXT — Seed Leave Requests
-- ============================================================

INSERT INTO leave_request (id, staff_member_id, manager_id, leave_type, start_date, end_date, number_of_days, reason, status, submitted_on, decided_on, decided_by, cancellation_reason)
VALUES
    ('lreq-0000-0000-0000-000000000001', 'stf-00000-00000-00000-000000000001', 'mgr-00000-00000-00000-000000000001', 'ANNUAL', '2026-07-14', '2026-07-18', 5, 'Summer holiday', 'APPROVED', '2026-06-01', '2026-06-03', 'mgr-00000-00000-00000-000000000001', NULL),
    ('lreq-0000-0000-0000-000000000002', 'stf-00000-00000-00000-000000000001', 'mgr-00000-00000-00000-000000000001', 'ANNUAL', '2026-09-01', '2026-09-03', 3, 'Long weekend', 'PENDING', '2026-08-15', NULL, NULL, NULL),
    ('lreq-0000-0000-0000-000000000003', 'stf-00000-00000-00000-000000000002', 'mgr-00000-00000-00000-000000000001', 'ANNUAL', '2026-08-05', '2026-08-06', 2, 'Personal appointment', 'APPROVED', '2026-07-20', '2026-07-21', 'mgr-00000-00000-00000-000000000001', NULL),
    ('lreq-0000-0000-0000-000000000004', 'stf-00000-00000-00000-000000000003', 'mgr-00000-00000-00000-000000000001', 'ANNUAL', '2026-12-23', '2026-12-31', 5, 'Christmas break', 'PENDING', '2026-08-20', NULL, NULL, NULL),
    ('lreq-0000-0000-0000-000000000005', 'stf-00000-00000-00000-000000000003', 'mgr-00000-00000-00000-000000000001', 'ANNUAL', '2026-10-14', '2026-10-25', 10, 'Annual trip', 'APPROVED', '2026-08-01', '2026-08-02', 'mgr-00000-00000-00000-000000000001', NULL),
    ('lreq-0000-0000-0000-000000000006', 'stf-00000-00000-00000-000000000001', 'mgr-00000-00000-00000-000000000001', 'ANNUAL', '2026-04-07', '2026-04-11', 5, 'Easter break', 'CANCELLED', '2026-03-01', '2026-03-02', 'mgr-00000-00000-00000-000000000001', 'Plans changed');
