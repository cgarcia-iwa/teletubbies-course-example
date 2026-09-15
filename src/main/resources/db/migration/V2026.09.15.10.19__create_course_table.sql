CREATE TABLE course (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(100) NOT NULL,
    description VARCHAR(200),
    duration SMALLINT NOT NULL CHECK (duration >= 1),
    level SMALLINT NOT NULL,
    category SMALLINT NOT NULL
);
