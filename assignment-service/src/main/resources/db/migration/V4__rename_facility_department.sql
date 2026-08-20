-- Keep the applied V2 migration immutable and align the facility category
-- with the agreed department name.

UPDATE departments
SET department_name = '시설관리과',
    updated_at = CURRENT_TIMESTAMP(6)
WHERE department_id = 20;
