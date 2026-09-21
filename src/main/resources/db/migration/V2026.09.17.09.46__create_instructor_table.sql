
CREATE TABLE instructor (
                            id UUID DEFAULT gen_random_uuid(),
                            full_name VARCHAR(150) NOT NULL,
                            email VARCHAR(150) NOT NULL,
                            PRIMARY KEY (id),
                            CONSTRAINT instructor_full_name_uk UNIQUE (full_name),
                            CONSTRAINT instructor_email_uk UNIQUE (email)
);




