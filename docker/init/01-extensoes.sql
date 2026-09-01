-- pg_trgm sustenta o agrupamento por similaridade de descrição (RN-12).
-- O agrupamento fica no banco de propósito: trazer milhares de descrições para a JVM
-- e comparar em memória é o caminho fácil que não escala.
CREATE EXTENSION IF NOT EXISTS pg_trgm;
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
