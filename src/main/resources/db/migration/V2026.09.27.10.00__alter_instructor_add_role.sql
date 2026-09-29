-- 1 = ADMINISTRATOR, 2 = TEACHER (ver InstructorRoleType)
ALTER TABLE instructor
    ADD COLUMN role SMALLINT NOT NULL DEFAULT 2;

ALTER TABLE instructor
    ADD CONSTRAINT instructor_role_ck CHECK (role IN (1, 2));