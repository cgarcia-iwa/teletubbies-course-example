ALTER TABLE course
    ADD COLUMN instructor_id UUID NOT NULL;

ALTER TABLE course
    ADD CONSTRAINT courses_instructor_id_fk
        FOREIGN KEY (instructor_id)
            REFERENCES instructor(id);


ALTER TABLE course ADD CONSTRAINT course_name_uk UNIQUE (name);
