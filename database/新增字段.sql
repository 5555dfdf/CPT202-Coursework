ALTER TABLE specialists ADD COLUMN name VARCHAR(100);

UPDATE specialists SET name = 'Dr.Smith' WHERE user_id = 'u3';
UPDATE specialists SET name = 'Dr.Lee' WHERE user_id = 'u4';