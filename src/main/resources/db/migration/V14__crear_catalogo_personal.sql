CREATE TABLE personal (
    id BIGSERIAL PRIMARY KEY,
    tipo_documento VARCHAR(20) NOT NULL,
    numero_documento VARCHAR(20) NOT NULL,
    nombres VARCHAR(150) NOT NULL,
    apellidos VARCHAR(150),
    cargo VARCHAR(120),
    area VARCHAR(120),
    telefono VARCHAR(50),
    email VARCHAR(150),
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    creado_en TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    actualizado_en TIMESTAMP,
    CONSTRAINT uk_personal_numero_documento UNIQUE (numero_documento),
    CONSTRAINT chk_personal_tipo_documento_no_vacio CHECK (BTRIM(tipo_documento) <> ''),
    CONSTRAINT chk_personal_numero_documento_no_vacio CHECK (BTRIM(numero_documento) <> ''),
    CONSTRAINT chk_personal_nombres_no_vacio CHECK (BTRIM(nombres) <> '')
);

CREATE INDEX idx_personal_numero_documento ON personal (numero_documento);
CREATE INDEX idx_personal_nombres ON personal (nombres);
CREATE INDEX idx_personal_apellidos ON personal (apellidos);
CREATE INDEX idx_personal_activo ON personal (activo);
