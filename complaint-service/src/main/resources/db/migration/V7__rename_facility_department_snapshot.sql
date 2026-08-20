-- Complaint stores the department name captured at assignment time. Keep
-- existing facility complaints consistent with assignment-service.

UPDATE complaints
SET assigned_department_name = '시설관리과',
    updated_at = CURRENT_TIMESTAMP(6)
WHERE assigned_department_id = 20;
