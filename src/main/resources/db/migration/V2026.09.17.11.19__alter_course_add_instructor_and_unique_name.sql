ALTER TABLE course
    ADD COLUMN instructor_id UUID NOT NULL
        REFERENCES instructor(id);


ALTER TABLE course ADD CONSTRAINT course_name_uk UNIQUE (name);
