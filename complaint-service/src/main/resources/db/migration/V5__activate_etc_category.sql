UPDATE complaint_categories
SET is_active = true,
    updated_at = NOW(6)
WHERE category_code = 'ETC'
  AND is_active = false;
