-- ============================================================
-- LEAVE MANAGEMENT CONTEXT (CORE)
-- ============================================================

CREATE TABLE leave_allowance (
    id                   VARCHAR(36)  PRIMARY KEY,
    staff_member_id      VARCHAR(36)  NOT NULL,
    manager_id           VARCHAR(36)  NOT NULL,
    first_name           VARCHAR(50)  NOT NULL,
    surname              VARCHAR(50)  NOT NULL,
    department           VARCHAR(100),
    business_year_start  INT          NOT NULL,
    business_year_end    INT          NOT NULL,
    total_entitlement    INT          NOT NULL CHECK (total_entitlement > 0),
    days_used            INT          NOT NULL DEFAULT 0 CHECK (days_used >= 0),
    days_pending         INT          NOT NULL DEFAULT 0 CHECK (days_pending >= 0),
    CONSTRAINT uq_allowance_staff_year UNIQUE (staff_member_id, business_year_start)
);

CREATE INDEX idx_leave_allowance_staff ON leave_allowance(staff_member_id);
CREATE INDEX idx_leave_allowance_manager ON leave_allowance(manager_id);

CREATE TABLE leave_request (
    id                   VARCHAR(36)  PRIMARY KEY,
    staff_member_id      VARCHAR(36)  NOT NULL,
    manager_id           VARCHAR(36)  NOT NULL,
    leave_type           VARCHAR(20)  NOT NULL,
    start_date           DATE         NOT NULL,
    end_date             DATE         NOT NULL,
    number_of_days       INT          NOT NULL CHECK (number_of_days > 0),
    reason               VARCHAR(500),
    status               VARCHAR(20)  NOT NULL,
    submitted_on         DATE         NOT NULL,
    decided_on           DATE,
    decided_by           VARCHAR(36),
    decision_reason      VARCHAR(500),
    cancellation_reason  VARCHAR(500)
);

CREATE INDEX idx_leave_request_staff ON leave_request(staff_member_id);
CREATE INDEX idx_leave_request_manager ON leave_request(manager_id);
CREATE INDEX idx_leave_request_status ON leave_request(status);

-- ============================================================
-- STAFF MANAGEMENT CONTEXT (SUPPORTING)
-- ============================================================

CREATE TABLE staff_member (
    id                        VARCHAR(36)  PRIMARY KEY,
    first_name                VARCHAR(50)  NOT NULL,
    surname                   VARCHAR(50)  NOT NULL,
    email                     VARCHAR(150) NOT NULL UNIQUE,
    department                VARCHAR(100) NOT NULL,
    line_manager_id           VARCHAR(36),
    hire_date                 DATE         NOT NULL,
    "current_role"            VARCHAR(100) NOT NULL,
    start_date_current_role   DATE         NOT NULL,
    job_level                 VARCHAR(20),
    employment_type           VARCHAR(20)  NOT NULL,
    employment_status         VARCHAR(20)  NOT NULL
);

CREATE INDEX idx_staff_member_manager ON staff_member(line_manager_id);

-- ============================================================
-- COMMON (EVENT INFRASTRUCTURE)
-- ============================================================

CREATE TABLE event_store (
    id              INT AUTO_INCREMENT PRIMARY KEY,
    occurred_on     DATE          NOT NULL,
    event_body      VARCHAR(65000) NOT NULL,
    event_type      VARCHAR(100)  NOT NULL,
    status          VARCHAR(20)   NOT NULL,
    retry_count     INT           NOT NULL DEFAULT 0,
    source_context  VARCHAR(100)
);

CREATE INDEX idx_event_store_type ON event_store(event_type);
CREATE INDEX idx_event_store_status ON event_store(status);

-- Spring Modulith event publication registry (required for @TransactionalEventListener)
CREATE TABLE IF NOT EXISTS event_publication (
    id                      UUID         NOT NULL PRIMARY KEY,
    listener_id             VARCHAR(512) NOT NULL,
    event_type              VARCHAR(512) NOT NULL,
    serialized_event        VARCHAR(4000) NOT NULL,
    publication_date        TIMESTAMP WITH TIME ZONE NOT NULL,
    completion_date         TIMESTAMP WITH TIME ZONE,
    status                  VARCHAR(20)  DEFAULT 'PUBLISHED' NOT NULL,
    completion_attempts     INT          DEFAULT 0 NOT NULL,
    last_resubmission_date  TIMESTAMP WITH TIME ZONE
);
