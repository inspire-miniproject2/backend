CREATE TABLE departments (
    department_id BIGINT NOT NULL AUTO_INCREMENT,
    department_name VARCHAR(100) NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (department_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE complaint_categories (
    category_id BIGINT NOT NULL AUTO_INCREMENT,
    category_name VARCHAR(100) NOT NULL,
    category_code VARCHAR(50) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (category_id),
    UNIQUE KEY uq_complaint_categories_category_code (category_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE department_categories (
    department_category_id BIGINT NOT NULL AUTO_INCREMENT,
    department_id BIGINT NOT NULL,
    category_id BIGINT NOT NULL,
    officer_user_id BIGINT NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (department_category_id),
    UNIQUE KEY uq_department_categories_category_id (category_id),
    KEY idx_department_categories_department_id (department_id),
    KEY idx_department_categories_officer_user_id (officer_user_id),
    CONSTRAINT fk_department_categories_department
        FOREIGN KEY (department_id) REFERENCES departments (department_id),
    CONSTRAINT fk_department_categories_category
        FOREIGN KEY (category_id) REFERENCES complaint_categories (category_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- officer_user_id belongs to user-service and is intentionally a logical reference.
